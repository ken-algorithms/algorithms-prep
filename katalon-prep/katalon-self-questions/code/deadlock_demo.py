"""Deadlock in Python. The GIL does NOT prevent this."""
import threading

lock_a, lock_b = threading.Lock(), threading.Lock()
both_hold_first = threading.Barrier(2)      # deterministic, not "sleep and hope"

def worker_1():
    with lock_a:                 # 1st: A
        both_hold_first.wait()
        with lock_b:             # 2nd: B  -- never reached
            print("t1 finished")

def worker_2():
    with lock_b:                 # 1st: B   <-- REVERSED ORDER
        both_hold_first.wait()
        with lock_a:             # 2nd: A  -- never reached
            print("t2 finished")

t1 = threading.Thread(target=worker_1, name="worker-1", daemon=True)
t2 = threading.Thread(target=worker_2, name="worker-2", daemon=True)
t1.start(); t2.start()
t1.join(timeout=2); t2.join(timeout=2)

# Python has NO built-in deadlock detector -> we can only observe "still alive"
print(f"worker-1 alive after 2s: {t1.is_alive()}")
print(f"worker-2 alive after 2s: {t2.is_alive()}")
print("both still alive == deadlock (Python will NOT tell you)")
