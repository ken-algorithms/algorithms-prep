import faulthandler, threading, sys
lock_a, lock_b = threading.Lock(), threading.Lock()
b = threading.Barrier(2)
def w1():
    with lock_a:
        b.wait()
        with lock_b: pass
def w2():
    with lock_b:
        b.wait()
        with lock_a: pass
threading.Thread(target=w1, name="w1", daemon=True).start()
threading.Thread(target=w2, name="w2", daemon=True).start()
faulthandler.dump_traceback_later(2, exit=True)   # dump ALL thread stacks after 2s, then exit
threading.Event().wait()
