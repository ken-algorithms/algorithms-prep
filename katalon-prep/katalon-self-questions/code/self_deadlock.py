import threading
lock = threading.Lock()          # NOT reentrant
def f():
    with lock:
        with lock:               # same thread, same lock -> blocks on ITSELF
            print("never printed")
t = threading.Thread(target=f, daemon=True); t.start(); t.join(timeout=1)
print(f"Lock  -> self-deadlock: {t.is_alive()}")

rlock = threading.RLock()        # reentrant
def g():
    with rlock:
        with rlock:
            print("RLock -> fine, same thread may re-enter")
t2 = threading.Thread(target=g, daemon=True); t2.start(); t2.join(timeout=1)
print(f"RLock -> self-deadlock: {t2.is_alive()}")
