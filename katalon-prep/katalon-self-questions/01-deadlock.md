# 01 — Deadlock là gì? Reproduce thế nào? Vì sao xảy ra?

> **Vì sao ghi câu này:** deadlock là câu kinh điển ở vòng Java/concurrency, và nó nối thẳng vào
> khuyến nghị dùng `SELECT ... FOR UPDATE` ở
> [../katalon-system-design/06 §5 bậc 2](../katalon-system-design/06-race-condition-balance-ledger.md#bậc-2--pessimistic-lock-theo-từng-account)
> — nơi tôi đã cảnh báo *"nguy cơ deadlock nếu hai đường lấy khoá theo thứ tự khác nhau"*. File này
> giải thích chính xác vì sao.
>
> **Ghi song ngữ 🇻🇳 + 🇬🇧** — vòng phỏng vấn có thể bằng tiếng Anh. Xem [§8](#8-trả-lời-bằng-tiếng-anh).
>
> ✅ **Mọi output trong file này là output thật, đã chạy trên máy bạn** (JDK 25.0.2 Temurin,
> Python 3.12.13) — trừ mục SQL, xem [§10](#10-ranh-giới-trung-thực).

---

## Mục lục

- [1. Trả lời ngắn (30 giây)](#1-trả-lời-ngắn-30-giây)
- [2. Định nghĩa chính xác — 4 điều kiện Coffman](#2-định-nghĩa-chính-xác--4-điều-kiện-coffman)
- [3. Reproduce bằng Java](#3-reproduce-bằng-java)
- [4. Reproduce bằng Python](#4-reproduce-bằng-python)
- [5. Deadlock ở tầng database](#5-deadlock-ở-tầng-database)
- [6. Cách phát hiện — công cụ cụ thể](#6-cách-phát-hiện--công-cụ-cụ-thể)
- [7. Cách chữa + đánh đổi](#7-cách-chữa--đánh-đổi)
- [8. Trả lời bằng tiếng Anh](#8-trả-lời-bằng-tiếng-anh)
- [9. Follow-up hay bị hỏi tiếp](#9-follow-up-hay-bị-hỏi-tiếp)
- [10. Ranh giới trung thực](#10-ranh-giới-trung-thực)

---

## 1. Trả lời ngắn (30 giây)

**🇻🇳** *"Deadlock là khi hai hay nhiều luồng cùng chờ nhau và không bên nào nhả tài nguyên đang
giữ, nên không ai đi tiếp được — vĩnh viễn. Nguyên nhân gốc gần như luôn là **thứ tự lấy khoá không
nhất quán**: luồng 1 giữ A rồi xin B, luồng 2 giữ B rồi xin A. Cách chữa chính là **áp một thứ tự
khoá toàn cục**; nếu không làm được thì dùng `tryLock` có timeout để ít nhất còn thoát ra được."*

**🇬🇧** *"A deadlock is when two or more threads each hold a resource the other needs and neither
will release it, so none of them can ever make progress. The root cause is almost always
**inconsistent lock ordering** — thread 1 takes A then asks for B, thread 2 takes B then asks for A.
The primary fix is to impose a **global lock ordering**; if that isn't possible, use a `tryLock`
with a timeout so at least you can back out."*

> **Câu ăn điểm nếu nói thêm được:** *"Điểm đáng nói là JVM và Python **phát hiện được nhưng không
> tự chữa** — luồng kẹt vĩnh viễn. Còn PostgreSQL thì **tự phát hiện và huỷ một transaction** để
> phá vòng. Nên deadlock ở tầng ứng dụng nguy hiểm hơn ở tầng database."*

---

## 2. Định nghĩa chính xác — 4 điều kiện Coffman

Deadlock chỉ xảy ra khi **cả bốn** điều kiện cùng đúng. Phá **một** điều kiện là đủ để diệt deadlock
— đây chính là bản đồ của mọi cách chữa ở [§7](#7-cách-chữa--đánh-đổi).

| # | Điều kiện 🇻🇳 | Condition 🇬🇧 | Nghĩa là gì | Phá bằng cách nào |
|---|---|---|---|---|
| 1 | **Loại trừ lẫn nhau** | *Mutual exclusion* | Tài nguyên chỉ một bên giữ được tại một thời điểm | Dùng cấu trúc bất biến / lock-free / bản sao riêng |
| 2 | **Giữ và chờ** | *Hold and wait* | Đang giữ khoá này mà vẫn xin khoá khác | Lấy **tất cả** khoá cùng lúc, hoặc nhả hết rồi thử lại |
| 3 | **Không tước đoạt** | *No preemption* | Không ai giật được khoá từ tay người đang giữ | `tryLock(timeout)` — tự nhả sau khi chờ đủ lâu |
| 4 | **Chờ vòng tròn** | *Circular wait* | A chờ B, B chờ A (hoặc vòng dài hơn) | **Thứ tự khoá toàn cục** ← cách thực dụng nhất |

```text
                        worker-1                    worker-2
                           │                            │
              giữ  ────►  LOCK_A                     LOCK_B  ◄──── giữ
                           │                            │
              xin  ────►  LOCK_B  ◄───────────┐         │
                                              └────►  LOCK_A  ◄──── xin

              ┌──────────────────────────────────────────────┐
              │  worker-1 chờ worker-2 nhả B                 │
              │  worker-2 chờ worker-1 nhả A     → VÒNG TRÒN │
              └──────────────────────────────────────────────┘
```

> **Điều kiện 4 là điều kiện dễ phá nhất trong thực tế** — và cũng là lý do "quy ước thứ tự khoá"
> là câu trả lời chuẩn. Ba điều kiện kia thường là bản chất của bài toán, không bỏ được.

---

## 3. Reproduce bằng Java

### 3.1 Code — reproduce **100%**, không phải "chạy nhiều lần may ra dính"

```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;

public class DeadlockDemo {
    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public static void main(String[] args) throws Exception {
        // Latch makes the deadlock DETERMINISTIC: both threads must hold their
        // first lock before either is allowed to reach for the second one.
        CountDownLatch bothHoldFirst = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            synchronized (LOCK_A) {                 // 1st: A
                sync(bothHoldFirst);
                synchronized (LOCK_B) {             // 2nd: B  -- never reached
                    System.out.println("t1 finished");
                }
            }
        }, "worker-1");

        Thread t2 = new Thread(() -> {
            synchronized (LOCK_B) {                 // 1st: B   <-- REVERSED ORDER
                sync(bothHoldFirst);
                synchronized (LOCK_A) {             // 2nd: A  -- never reached
                    System.out.println("t2 finished");
                }
            }
        }, "worker-2");

        t1.setDaemon(true); t2.setDaemon(true);     // daemon so the JVM can still exit
        t1.start(); t2.start();

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] ids = null;
        for (int i = 0; i < 30 && ids == null; i++) {
            Thread.sleep(100);
            ids = bean.findDeadlockedThreads();     // the JVM can DETECT it, but never FIX it
        }

        if (ids == null) {
            System.out.println("NO deadlock detected");
        } else {
            System.out.println("DEADLOCK detected, threads = " + ids.length);
            for (ThreadInfo info : bean.getThreadInfo(ids)) {
                System.out.printf("  %s  blocked on <%s>  held by %s%n",
                        info.getThreadName(), info.getLockName(), info.getLockOwnerName());
            }
        }
        System.out.println("main exits; the two workers are stuck forever");
    }

    private static void sync(CountDownLatch latch) {
        try { latch.countDown(); latch.await(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

```bash
java DeadlockDemo.java     # JDK 11+ chạy thẳng file .java, không cần javac
```

**Output thật (JDK 25.0.2 Temurin, đã chạy):**

```text
DEADLOCK detected, threads = 2
  worker-1  blocked on <java.lang.Object@41fbdac4>  held by worker-2
  worker-2  blocked on <java.lang.Object@c33b74f>  held by worker-1
main exits; the two workers are stuck forever
```

### 3.2 Ba chi tiết trong code này, mỗi cái là một điểm ăn nói

| Chi tiết | Vì sao quan trọng |
|---|---|
| **`CountDownLatch` thay vì `Thread.sleep()`** | `sleep` chỉ làm deadlock *có khả năng* xảy ra. Latch **bắt buộc** cả hai luồng phải giữ xong khoá thứ nhất rồi mới có luồng nào được xin khoá thứ hai → deadlock **chắc chắn**, chạy lần nào cũng dính. Đây là khác biệt giữa "test flaky" và "test tin được" |
| **`findDeadlockedThreads()`** | JVM **biết** đang deadlock. Nhưng nó chỉ báo cáo — **không tự phá**. Đây là câu trả lời cho *"vì sao service treo mà không có exception nào"* |
| **`setDaemon(true)`** | Không có nó thì JVM không thoát được, phải `kill -9`. Chi tiết nhỏ nhưng nó cho thấy bạn từng chạy thật chứ không chép code |

### 3.3 Bản sửa — cùng logic, thêm một quy ước

```java
// FIX: impose a GLOBAL lock ordering. Both threads take A first, then B.
// Nothing else changes -- same locks, same work, same threads.
Thread t2Fixed = new Thread(() -> {
    synchronized (LOCK_A) {            // was LOCK_B
        synchronized (LOCK_B) {        // was LOCK_A
            System.out.println("t2 finished");
        }
    }
}, "worker-2");
```

Khi khoá là **đối tượng động** (ví dụ hai tài khoản ngân hàng), sắp thứ tự theo một khoá ổn định:

```java
// Classic bank-transfer fix: always lock the lower id first.
void transfer(Account from, Account to, BigDecimal amount) {
    Account first  = from.getId() < to.getId() ? from : to;
    Account second = from.getId() < to.getId() ? to   : from;
    synchronized (first) {
        synchronized (second) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}
```

> ⚠️ **Bẫy phải biết:** nếu `from.getId() == to.getId()` (chuyển cho chính mình) thì `synchronized`
> lồng nhau trên **cùng** object — Java monitor là **reentrant** nên không sao. Nhưng nếu dùng
> `ReentrantLock` mà quên gọi `unlock()` đủ số lần, hoặc dùng khoá **không** reentrant, thì sẽ
> tự khoá chính mình. Xem [§4.2](#42-self-deadlock--lock-không-reentrant).

### 3.4 Phương án hai — `tryLock` có timeout

Dùng khi **không** áp được thứ tự toàn cục (ví dụ khoá đến từ thư viện bên ngoài):

```java
private final ReentrantLock lockA = new ReentrantLock();
private final ReentrantLock lockB = new ReentrantLock();

boolean doWork() throws InterruptedException {
    if (!lockA.tryLock(200, TimeUnit.MILLISECONDS)) return false;
    try {
        if (!lockB.tryLock(200, TimeUnit.MILLISECONDS)) return false;  // back out instead of waiting forever
        try {
            // ... critical section
            return true;
        } finally { lockB.unlock(); }
    } finally { lockA.unlock(); }
}
```

**Đánh đổi:** deadlock biến thành **livelock** nếu hai bên cứ thử-thất bại-thử lại đồng bộ với nhau.
Chữa bằng **backoff ngẫu nhiên** trước khi thử lại — đúng cùng lý do phải có jitter trong retry ở
[../katalon-prep-java/06-distributed-resilience/](../katalon-prep-java/06-distributed-resilience/).

---

## 4. Reproduce bằng Python

### 4.1 Deadlock kinh điển — **GIL không cứu bạn**

Đây là hiểu nhầm phổ biến nhất: *"Python có GIL nên không bị race/deadlock."* **Sai.** GIL tuần tự
hoá việc thực thi bytecode, nhưng `threading.Lock` là khoá ở tầng người dùng — hoàn toàn deadlock được.

```python
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
```

**Output thật (Python 3.12.13, đã chạy):**

```text
worker-1 alive after 2s: True
worker-2 alive after 2s: True
both still alive == deadlock (Python will NOT tell you)
```

> **Khác biệt quan trọng với Java, nên nói ra:** Java có `findDeadlockedThreads()` báo thẳng
> *"deadlock, hai luồng, ai giữ gì"*. **Python không có thứ tương đương** — bạn chỉ suy ra được từ
> việc luồng vẫn còn sống. Nghĩa là ở Python, deadlock **khó chẩn đoán hơn**, và
> [§6.2](#62-python) là cách duy nhất để nhìn thấy nó.
>
> `threading.Barrier(2)` ở đây đóng đúng vai trò của `CountDownLatch` bên Java: ép hai luồng trùng
> thời điểm để deadlock xảy ra **chắc chắn**.

### 4.2 Self-deadlock — `Lock` không reentrant

Biến thể này chỉ cần **một** luồng và **một** khoá. Hay gặp khi refactor: hàm A đã giữ khoá rồi gọi
hàm B mà hàm B cũng lấy đúng khoá đó.

```python
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
```

**Output thật (đã chạy):**

```text
Lock  -> self-deadlock: True
RLock -> fine, same thread may re-enter
RLock -> self-deadlock: False
```

**Bảng đối chiếu hai ngôn ngữ — đây là chỗ hay bị hỏi khi CV ghi cả Java lẫn Python:**

| | Java | Python |
|---|---|---|
| Khoá mặc định | `synchronized` / `ReentrantLock` — **reentrant sẵn** | `threading.Lock` — **KHÔNG reentrant** |
| Bản reentrant | mặc định đã là | phải chủ động dùng `threading.RLock` |
| Hệ quả | Tự gọi lại chính mình vẫn chạy | Tự gọi lại chính mình → **treo ngay lập tức** |
| Có detector không | ✅ `ThreadMXBean.findDeadlockedThreads()` | ❌ không có |
| Lấy khoá có timeout | `tryLock(t, unit)` | `lock.acquire(timeout=t)` |

### 4.3 Biến thể `asyncio` — một luồng vẫn deadlock được

Người ta hay nghĩ async single-threaded thì miễn nhiễm. Không phải:

```python
import asyncio

async def main():
    lock = asyncio.Lock()
    async with lock:
        async with lock:          # asyncio.Lock is NOT reentrant either -> hangs forever
            print("never printed")

asyncio.run(main())               # hangs; needs Ctrl-C or a timeout wrapper
```

Deadlock ở đây không phải do nhiều luồng, mà do **coroutine chờ một thứ chỉ chính nó mới nhả được**.
Cùng bản chất: chờ vòng tròn, vòng chỉ có một đỉnh.

---

## 5. Deadlock ở tầng database

**Đây là biến thể liên quan trực tiếp nhất tới công việc thật của bạn** — và tới khuyến nghị
`SELECT ... FOR UPDATE` ở
[../katalon-system-design/06](../katalon-system-design/06-race-condition-balance-ledger.md#bậc-2--pessimistic-lock-theo-từng-account).

```sql
-- Session 1                              -- Session 2
BEGIN;                                    BEGIN;
UPDATE loan_account SET ... WHERE id = 1; UPDATE loan_account SET ... WHERE id = 2;
-- giữ khoá dòng id=1                     -- giữ khoá dòng id=2

UPDATE loan_account SET ... WHERE id = 2; UPDATE loan_account SET ... WHERE id = 1;
-- CHỜ session 2 nhả id=2                 -- CHỜ session 1 nhả id=1
--                        └──── VÒNG TRÒN ────┘
```

PostgreSQL phát hiện sau `deadlock_timeout` (mặc định **1 giây**) và **huỷ một transaction**:

```text
ERROR:  deadlock detected
DETAIL: Process 123 waits for ShareLock on transaction 456; blocked by process 789.
        Process 789 waits for ShareLock on transaction 455; blocked by process 123.
SQLSTATE: 40P01
```

### Khác biệt cốt lõi giữa deadlock ứng dụng và deadlock database

| | Java / Python | PostgreSQL |
|---|---|---|
| Có phát hiện không | Java: có (chỉ báo cáo) · Python: không | ✅ Có |
| **Có tự phá không** | ❌ **Không bao giờ** — treo vĩnh viễn | ✅ **Có** — huỷ một transaction |
| Bạn thấy gì | Service treo, **không có exception nào** | `SQLSTATE 40P01`, một bên nhận lỗi |
| Xử lý thế nào | Phải restart / phải thiết kế để không xảy ra | **Bắt `40P01` và retry** |

> **Câu ăn điểm:** *"Deadlock ở database thực ra 'dễ sống chung' hơn — nó tự phá vòng và ném lỗi
> `40P01`, mình bắt rồi retry. Nhưng retry chỉ an toàn nếu transaction **idempotent** — nên với hệ
> thống tiền, idempotency key là điều kiện tiên quyết của retry, không phải tính năng thêm sau."*
> (Đây đúng là lập luận ở [06 §6 bước 1](../katalon-system-design/06-race-condition-balance-ledger.md#bước-1--idempotency-trước-tiên-làm-trước-cả-việc-sửa-race).)

### Biến thể ác nhất: deadlock ở connection pool

Không cần hai khoá nào cả — chỉ cần pool cạn:

```text
Pool có 10 connection.
10 request cùng lúc, mỗi request:
    1. lấy connection #1  ──► mở transaction
    2. gọi một service khác, service đó lại XIN THÊM một connection từ CÙNG pool
    3. pool đã hết sạch (cả 10 đang bị giữ ở bước 1)
    4. cả 10 cùng chờ một connection mà chỉ chính chúng mới nhả được
    → TREO TOÀN BỘ SERVICE, và log chỉ báo "connection timeout" chứ không nói "deadlock"
```

Cách chữa: **một request không bao giờ được giữ hai connection cùng lúc**; nếu buộc phải, dùng pool
riêng cho luồng phụ. HikariCP có công thức tối thiểu `pool_size ≥ threads × (connections_per_thread − 1) + 1`.

> Đáng nêu vì nó **không giống deadlock**: không có `synchronized` nào, không có `FOR UPDATE` nào,
> log chỉ nói timeout. Nhưng bản chất vẫn là đủ 4 điều kiện Coffman với "tài nguyên" là connection.

---

## 6. Cách phát hiện — công cụ cụ thể

### 6.1 Java

```bash
jcmd -l                        # tìm pid
jstack <pid>                   # hoặc: jcmd <pid> Thread.print
```

**Output thật (đã chạy trên máy bạn, JDK 25):**

```text
Found one Java-level deadlock:
=============================
"worker-1":
  waiting to lock monitor 0x000000012f36b3b0 (object 0x000000070e0dd768, a java.lang.Object),
  which is held by "worker-2"

"worker-2":
  waiting to lock monitor 0x000000012f36b2e0 (object 0x000000070e0dd758, a java.lang.Object),
  which is held by "worker-1"

Java stack information for the threads listed above:
===================================================
"worker-1":
	at Hang.lambda$main$0(Hang.java:6)
	- waiting to lock <0x000000070e0dd768> (a java.lang.Object)
```

`jstack` nói thẳng **"Found one Java-level deadlock"** kèm cả dòng code. Đây là công cụ đầu tiên
phải gọi khi một service Java treo mà CPU 0%.

Trong code, dùng `ThreadMXBean` để tự phát hiện (xem [§3.1](#31-code--reproduce-100-không-phải-chạy-nhiều-lần-may-ra-dính))
— hữu ích cho health check hoặc cho test.

> ⚠️ `jstack` **chỉ** phát hiện deadlock trên `synchronized` và `java.util.concurrent.Lock`. Nó
> **không** thấy deadlock ở tầng logic (chờ nhau qua queue, qua DB, qua connection pool).

### 6.2 Python

Không có detector sẵn. Hai cách thực dụng:

```python
import faulthandler
faulthandler.dump_traceback_later(10, exit=True)   # sau 10s: dump stack MỌI thread rồi thoát
```

**Output thật (đã chạy):**

```text
Timeout (0:00:02)!
Thread 0x000000016f477000 (most recent call first):
  File "detect.py", line 11 in w2
  ...
Thread 0x000000016e46b000 (most recent call first):
  File "detect.py", line 7 in w1
```

→ Thấy `w1` kẹt ở dòng 7 (`with lock_b`) và `w2` kẹt ở dòng 11 (`with lock_a`) — đủ để suy ra vòng.

> ⚠️ **Khác Java ở chỗ quan trọng:** nó **không nói chữ "deadlock"**. Nó chỉ in stack của mọi luồng;
> bạn phải tự đọc và tự nhận ra vòng chờ. Đây là lý do deadlock trong Python khó chẩn đoán hơn.

Hoặc từ ngoài process, không cần sửa code:

```bash
py-spy dump --pid <pid>        # in stack mọi thread của process đang chạy
```

### 6.3 PostgreSQL

```sql
-- ai đang chờ ai, ngay lúc này
SELECT a.pid, a.state, a.wait_event_type, a.wait_event,
       a.query, cardinality(pg_blocking_pids(a.pid)) AS blockers,
       pg_blocking_pids(a.pid) AS blocked_by
FROM pg_stat_activity a
WHERE cardinality(pg_blocking_pids(a.pid)) > 0;
```

Và bật ghi log để có bằng chứng sau sự cố:

```ini
log_lock_waits = on          # ghi log khi một câu lệnh chờ khoá quá deadlock_timeout
deadlock_timeout = 1s        # cũng là ngưỡng bắt đầu chạy thuật toán dò deadlock
```

---

## 7. Cách chữa + đánh đổi

Xếp theo thứ tự nên thử. Mỗi cách phá **một** điều kiện Coffman ở [§2](#2-định-nghĩa-chính-xác--4-điều-kiện-coffman).

| # | Cách | Phá điều kiện | Được | **Mất** |
|---|---|:---:|---|---|
| 1 | **Thứ tự khoá toàn cục** — luôn lấy khoá theo một thứ tự cố định (id tăng dần) | 4 — chờ vòng tròn | Diệt tận gốc; không tốn hiệu năng; suy luận được | Phải **kỷ luật toàn team**; một người quên là vỡ → nên có test kiến trúc chặn |
| 2 | **`tryLock` + timeout + backoff ngẫu nhiên** | 3 — không tước đoạt | Không cần biết trước thứ tự; hợp khi khoá đến từ thư viện ngoài | Thành **livelock** nếu thiếu backoff; phải viết đường xử lý thất bại |
| 3 | **Gom thành một khoá duy nhất** (khoá thô hơn) | 4 | Đơn giản nhất, không thể sai | Giảm song song — có thể thành nghẽn cổ chai |
| 4 | **Không lồng khoá** — tách critical section ra | 2 — giữ và chờ | Deadlock không có đất sống | Phải thiết kế lại luồng dữ liệu; đôi khi không tách được |
| 5 | **Single-writer** — mỗi thực thể một luồng/partition xử lý tuần tự | 1 — loại trừ lẫn nhau | **Loại bỏ** đồng thời thay vì quản lý nó; không khoá, không retry | Đổi mô hình sang bất đồng bộ ([06 §5 bậc 4](../katalon-system-design/06-race-condition-balance-ledger.md#bậc-4--single-writer-theo-account)) |
| 6 | **Bất biến / lock-free** (`AtomicLong`, CAS, cấu trúc immutable) | 1 | Không có khoá thì không có deadlock | Chỉ dùng được cho thao tác đơn giản; CAS retry có thể sống trâu ở tranh chấp cao |
| 7 | **Giữ transaction DB thật ngắn** | 2 | Giảm mạnh xác suất; dễ làm | Chỉ **giảm xác suất**, không diệt hẳn |

### Biến quy ước #1 thành thứ máy kiểm tra được

Cách #1 là tốt nhất nhưng phụ thuộc con người — nên biến nó thành một cái test, giống hệt cách bạn
đã làm với `test_no_retry_setter_outside_verify`
([../AI-STACK-INTERVIEW-ANSWERS.md §4B](../AI-STACK-INTERVIEW-ANSWERS.md)):

```java
@Test
void locksMustAlwaysBeAcquiredInIdOrder() throws IOException {
    // mọi chỗ khoá hai account phải đi qua helper đã sắp xếp sẵn thứ tự
    List<Path> offenders = Files.walk(Path.of("src/main/java"))
        .filter(p -> p.toString().endsWith(".java"))
        .filter(p -> countOccurrences(p, "findByIdForUpdate") > 1)
        .filter(p -> !contains(p, "lockInIdOrder("))
        .toList();
    assertThat(offenders)
        .as("Khoá nhiều account mà không qua lockInIdOrder() — xem self-questions/01 §7")
        .isEmpty();
}
```

> **Nguyên tắc:** *quy ước mà chỉ nằm trong wiki thì sáu tháng sau sẽ có người vi phạm.* Quy ước có
> test đi kèm thì không.

---

## 8. Trả lời bằng tiếng Anh

### 8.1 Thuật ngữ 🇻🇳 ↔ 🇬🇧

| Tiếng Việt | English | Ghi chú phát âm / dùng từ |
|---|---|---|
| Bế tắc / khoá chết | **deadlock** | dùng thẳng "deadlock", không dịch |
| Bốn điều kiện Coffman | the four **Coffman conditions** | /ˈkɒfmən/ |
| Loại trừ lẫn nhau | **mutual exclusion** | |
| Giữ và chờ | **hold and wait** | |
| Không tước đoạt | **no preemption** | /priˈempʃən/ |
| Chờ vòng tròn | **circular wait** | |
| Thứ tự khoá | **lock ordering** | "impose a consistent lock ordering" |
| Vùng găng | **critical section** | |
| Khoá tái nhập | **reentrant lock** | /riːˈentrənt/ |
| Độ mịn của khoá | **lock granularity** | "coarse-grained" ↔ "fine-grained" |
| Tranh chấp khoá | **lock contention** | |
| Đói tài nguyên | **starvation** | |
| Sống trâu | **livelock** | |
| Điều kiện tranh đua | **race condition** | |
| Bế tắc tự thân | **self-deadlock** | |

### 8.2 Câu trả lời 30 giây

> *"A deadlock happens when two or more threads each hold a lock the other one needs, and neither
> will release it — so nobody can make progress, permanently. Formally it needs all four Coffman
> conditions to hold at once: mutual exclusion, hold-and-wait, no preemption, and circular wait.
> Breaking any single one of them is enough to prevent it, and in practice the easiest one to break
> is circular wait — you impose a consistent global lock ordering."*

### 8.3 Câu trả lời 90 giây (khi họ nói "tell me more")

> *"The classic reproduction is two threads and two locks taken in opposite order. Thread one takes
> A then asks for B; thread two takes B then asks for A. One thing I'd point out is that a
> `Thread.sleep` between the two acquisitions only makes it **likely** — if you want it to be
> deterministic you use a `CountDownLatch` so both threads are guaranteed to hold their first lock
> before either reaches for the second. That's the difference between a flaky test and a reliable one.*
>
> *For detection: on the JVM, `jstack` prints 'Found one Java-level deadlock' and tells you exactly
> which thread holds what, and programmatically `ThreadMXBean.findDeadlockedThreads()` gives you the
> same thing. The important nuance is that the JVM **detects** it but never **resolves** it — the
> threads stay stuck forever, and there's no exception, which is why the symptom is a hung service
> at zero CPU.*
>
> *Python is worse in that respect: there's no built-in detector at all, so you fall back to
> `faulthandler.dump_traceback_later` or `py-spy dump` and read the stacks yourself. And a common
> misconception is that the GIL protects you — it doesn't. The GIL serialises bytecode execution,
> but `threading.Lock` is a user-level lock and deadlocks just fine.*
>
> *Databases behave differently and it's worth calling out: PostgreSQL actively detects the cycle
> after `deadlock_timeout` and **aborts one transaction** with SQLSTATE `40P01`. So a database
> deadlock is recoverable — you catch it and retry. But retry is only safe if the transaction is
> idempotent, which is why in a financial system I'd treat an idempotency key as a prerequisite for
> retry rather than something you add later."*

### 8.4 Ba câu ngắn nên thuộc

| Tình huống | Câu |
|---|---|
| Chốt nguyên nhân | *"The root cause is almost always inconsistent lock ordering."* |
| Chốt cách chữa | *"Impose a global lock ordering; if you can't, use `tryLock` with a timeout and randomised backoff."* |
| Chốt sự khác biệt | *"The JVM detects deadlocks but never resolves them; PostgreSQL detects **and** breaks them."* |

---

## 9. Follow-up hay bị hỏi tiếp

<details>
<summary><b>Nhóm A — khái niệm (5 câu)</b></summary>

**A1. "Deadlock khác livelock và starvation thế nào?"**

| | Trạng thái luồng | Có tiến triển không | Ví dụ |
|---|---|---|---|
| **Deadlock** | **Bị chặn**, không chạy | Không, và **không bao giờ** thoát ra được | Hai luồng chờ khoá của nhau |
| **Livelock** | **Đang chạy**, tốn CPU | Không, nhưng vẫn "bận rộn" | Hai bên cùng `tryLock` thất bại rồi thử lại đồng bộ, mãi không ai thắng |
| **Starvation** | Đang chạy hoặc chờ | Có tiến triển, nhưng **một số luồng không bao giờ tới lượt** | Priority queue mà luồng ưu tiên cao submit liên tục |

> Câu chốt: *"Deadlock thì CPU 0%, livelock thì CPU 100%. Nhìn CPU là phân biệt được ngay."*

**A2. "Nếu chỉ phá được một điều kiện Coffman thì chọn cái nào?"**
> Circular wait — vì ba cái kia thường là bản chất của bài toán, không bỏ được. Mutual exclusion là
> lý do có khoá; hold-and-wait khó tránh khi cần nhiều tài nguyên; no preemption thì giật khoá giữa
> chừng dễ để lại trạng thái dở dang. Còn thứ tự khoá chỉ là một quy ước, không tốn hiệu năng gì.

**A3. "Deadlock có xảy ra với một luồng không?"**
> Có — self-deadlock, khi khoá không reentrant. Trong Python `threading.Lock` không reentrant nên
> `with lock: with lock:` treo ngay. Java `synchronized` reentrant nên không sao, nhưng
> `asyncio.Lock` bên Python cũng không reentrant, tức single-threaded async vẫn deadlock được.

**A4. "Hai luồng dùng đúng một khoá thì có deadlock được không?"**
> Với khoá reentrant chuẩn thì không — chỉ tranh chấp và chờ, rồi lần lượt qua. Deadlock cần ít nhất
> hai tài nguyên để tạo được vòng. Trừ trường hợp self-deadlock ở A3, nơi "vòng" chỉ có một đỉnh.

**A5. "GIL trong Python có ngăn được deadlock không?"**
> Không. GIL tuần tự hoá việc thực thi bytecode, còn `threading.Lock` là khoá tầng người dùng — hai
> chuyện khác nhau. Tôi đã chạy thử: hai luồng lấy khoá ngược thứ tự vẫn treo vĩnh viễn. GIL chỉ
> làm một số thao tác đơn lẻ trở nên atomic, nó không tạo ra thứ tự khoá nào cả.
</details>

<details>
<summary><b>Nhóm B — thực chiến (4 câu)</b></summary>

**B1. "Production đang treo, anh làm gì đầu tiên?"**
> Nhìn CPU trước: 0% thì nghiêng về deadlock hoặc chờ I/O, 100% thì nghiêng về livelock hoặc vòng
> lặp vô hạn. Rồi `jstack <pid>` — nếu là deadlock trên monitor thì nó nói thẳng "Found one
> Java-level deadlock" kèm dòng code. Nếu `jstack` không thấy gì mà service vẫn treo, tôi nghi
> deadlock ở tầng logic: connection pool cạn, chờ nhau qua queue, hoặc chờ một service ngoài không
> có timeout. Và tôi lấy **hai** bản dump cách nhau vài giây — nếu stack không đổi thì thật sự kẹt,
> đổi thì là chậm chứ không phải deadlock.

**B2. "Vì sao dùng `FOR UPDATE` lại sinh nguy cơ deadlock?"**
> Vì mỗi `SELECT ... FOR UPDATE` là một khoá dòng giữ tới hết transaction. Hai transaction khoá hai
> account theo thứ tự ngược nhau là tạo ra đúng vòng chờ. Đó là lý do khi tôi đề xuất pessimistic
> lock cho bài balance, tôi kèm luôn một quy ước bắt buộc: **luôn khoá theo `loan_account_id` tăng
> dần**. Và may là ở database, Postgres tự phá vòng và ném `40P01` chứ không treo vĩnh viễn.

**B3. "Làm sao viết test cho deadlock mà không bị flaky?"**
> Không dùng `sleep`. Dùng `CountDownLatch` (Java) hoặc `Barrier` (Python) để **ép** cả hai luồng
> phải giữ xong khoá thứ nhất rồi mới có ai được xin khoá thứ hai — như vậy deadlock xảy ra chắc
> chắn 100%. Rồi assert bằng `ThreadMXBean.findDeadlockedThreads()` chứ không assert bằng timeout.
> Và cho thread là daemon để JVM còn thoát được. Với **bản đã sửa** thì test ngược lại: assert cả
> hai luồng kết thúc trong một thời hạn.

**B4. "Đã có thứ tự khoá rồi, làm sao chắc team không phá vỡ nó?"**
> Bọc vào một helper duy nhất — `lockInIdOrder(a, b, action)` — rồi thêm một test kiến trúc quét
> source: chỗ nào khoá từ hai account trở lên mà không đi qua helper đó thì test đỏ. Quy ước nằm
> trong wiki thì sáu tháng sau sẽ có người vi phạm; quy ước có test thì không.
</details>

---

## 10. Ranh giới trung thực

| Điều | Trạng thái | Được nói gì |
|---|---|---|
| Code Java §3.1 + output | ✅ **Đã chạy**, JDK 25.0.2 Temurin | ✅ Nói *"tôi đã chạy"* |
| Output `jstack` §6.1 | ✅ **Đã chạy thật** trên máy bạn | ✅ Trích được nguyên văn |
| Code Python §4.1, §4.2 + output | ✅ **Đã chạy**, Python 3.12.13 | ✅ |
| `faulthandler` §6.2 + output | ✅ **Đã chạy** | ✅ |
| `asyncio.Lock` §4.3 | 🟡 **Chưa chạy** — treo vĩnh viễn nên không tiện chạy tự động | 🟡 Nói *"về nguyên tắc"*, hoặc chạy thử với `asyncio.wait_for` trước khi khẳng định |
| Deadlock SQL §5 + `40P01` | 🔴 **Chưa chạy trên máy này** (không có psql, Docker không chạy) | 🔴 Đây là kiến thức chuẩn về Postgres, **không phải bạn tự đo**. Muốn chắc thì `docker run postgres` rồi mở hai `psql` |
| Công thức pool HikariCP §5 | 🟡 Công thức tài liệu, chưa tự kiểm | 🟡 Nói ý tưởng, đừng chắc nịch con số |
| `py-spy dump` §6.2 | 🔴 **Chưa cài, chưa chạy** | 🔴 Nói *"công cụ thường dùng"*, không nói *"tôi đã dùng"* |
| Test kiến trúc §7 | 🔴 **Chưa viết, chưa chạy** | 🔴 Nói là đề xuất |

> **Cách nói an toàn:** *"Phần Java và Python tôi đã chạy thật và có output. Phần deadlock ở
> PostgreSQL tôi hiểu cơ chế nhưng chưa dựng lại trên máy — nếu anh/chị muốn tôi có thể mô tả chính
> xác các bước để reproduce."* Trung thực về ranh giới **mạnh hơn** là nói như đã làm hết.

---

## Liên quan

| Tài liệu | Liên quan chỗ nào |
|---|---|
| [code/](code/) | **Bốn file chạy được** của chính file này — Java + Python, không cần cài gì |
| [../katalon-system-design/06 — Race condition balance](../katalon-system-design/06-race-condition-balance-ledger.md) | Nơi đề xuất `FOR UPDATE` và cảnh báo deadlock nếu sai thứ tự khoá — file này là phần giải thích |
| [../katalon-prep-java/04-concurrency/](../katalon-prep-java/04-concurrency/) | Code concurrency Java chạy được |
| [../katalon-prep-java/06-distributed-resilience/](../katalon-prep-java/06-distributed-resilience/) | Retry + jitter — lý do `tryLock` phải có backoff ngẫu nhiên |
| [../katalon-prep-common/03-system-design.md](../katalon-prep-common/03-system-design.md) | Khái niệm nền, so sánh Java ↔ Python |
