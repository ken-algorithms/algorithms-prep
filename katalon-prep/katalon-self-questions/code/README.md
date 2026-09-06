# code — ví dụ chạy được cho [01-deadlock.md](../01-deadlock.md)

Không cần Docker, không cần cài gì thêm.

```bash
java DeadlockDemo.java     # JDK 11+  → in "DEADLOCK detected, threads = 2"
python3 deadlock_demo.py   # 3.8+     → hai worker vẫn alive sau 2s = deadlock
python3 self_deadlock.py   #          → Lock treo, RLock không
python3 detect.py          #          → faulthandler dump stack rồi thoát sau 2s
```

Đã chạy thật: JDK 25.0.2 Temurin · Python 3.12.13 (macOS arm64). Output ghi trong
[01-deadlock.md](../01-deadlock.md).

Xem `jstack` trên deadlock đang chạy:

```bash
java DeadlockDemo.java &            # sửa vòng lặp detect thành Thread.sleep(60000) để nó treo lâu
jcmd -l | grep -i deadlock          # lấy pid
jstack <pid> | grep -A 14 "Found one Java-level deadlock"
```
