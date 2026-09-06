# Module 04 — Concurrency (P0)

```bash
mvn -pl 04-concurrency test    # 11 test pass
```

Checklist gốc bạn đưa nói đúng: *"Thread Pool, Race Condition, Deadlock, Virtual Threads"* là thứ
phải nắm. Module này biến chúng thành code chạy được.

## Nội dung

| Chủ đề | File | Điểm đáng giá |
|---|---|---|
| **Race condition** | `ResultCounter.Unsafe` | `value = v + 1` là 3 lệnh read-modify-write → mất update. Test chạy lại 5 lần để **không flaky**, và ghi rõ: test xanh **KHÔNG** phải bằng chứng không có race |
| **4 cách đếm** | `ResultCounter` | `Unsafe` / `synchronized` / `AtomicLong` (CAS) / **`LongAdder`** (striping, tránh false sharing). Trade-off `AtomicLong` vs `LongAdder` là câu hỏi phỏng vấn thật |
| **Deadlock + phát hiện** | `Deadlocks`, test `oppositeLockOrderDeadlocks` | Dùng **`ThreadMXBean.findDeadlockedThreads()`** — chính là cách chẩn đoán request treo ở production. Test này deterministic nhờ `CountDownLatch` |
| **Lock ordering** | `Deadlocks.lockOrdered` | Cách sửa deadlock đáng tin cậy nhất: sắp xếp theo order toàn cục → phá **circular wait** (điều kiện 4 của Coffman) |
| **`tryLock` timeout** | `Deadlocks.tryBothOrGiveUp` | Phá điều kiện *no preemption*. Đánh đổi: có thể thất bại → caller phải xử lý, và retry ngay lập tức thì sinh **livelock** |
| **Virtual threads** | `VirtualThreads` | 2000 task blocking xong trong <10s. So sánh với pool 8 platform thread |
| **Exception biến mất** | `runAllCollectingFailures` | `executor.submit()` nuốt exception vào `Future` — không ai gọi `get()` là **mất tích hoàn toàn**. Nguyên nhân thật của nhiều "job chạy mà không ra kết quả" |

## Ba quy tắc virtual thread phải nói đúng

1. **Đừng pool virtual thread** — chúng rẻ như object. Dùng `newVirtualThreadPerTaskExecutor()`.
2. **Chỉ giúp IO-bound, không giúp CPU-bound** — task tính toán không unmount.
3. **Vẫn cần giới hạn concurrency xuống downstream** — virtual thread bỏ giới hạn về *thread*,
   không bỏ giới hạn về *connection pool*. 10.000 virtual thread đâm vào pool 20 slot = 9.980 xếp
   hàng. Vẫn phải có `Semaphore`/rate limiter. **Đây là bẫy nhiều người bỏ qua.**

## Về PINNING — lưu ý phiên bản (quan trọng)

- **Java 21** (Katalon dùng): khối `synchronized` chứa điểm blocking sẽ **ghim** virtual thread vào
  carrier → mất hết lợi ích. Cách tránh: dùng `ReentrantLock` thay `synchronized` trong đoạn có IO.

- **Java 24+** (JEP 491): đã sửa, `synchronized` không còn ghim.
- **Máy bạn chạy JDK 25** → bạn sẽ **không** quan sát được pinning bằng thực nghiệm.

Vì vậy module này **có ý không viết test cho pinning**: một test "chứng minh pinning" sẽ pass trên
JDK 21 và fail trên JDK 25 — tức là một test dối trá. Nhưng câu trả lời đúng khi phỏng vấn là câu
trả lời của **Java 21**.

## Checklist

- [ ] Kể được 4 điều kiện Coffman và cách phá từng cái
- [ ] Giải thích `AtomicLong` vs `LongAdder` bằng false sharing
- [ ] Biết dùng `ThreadMXBean` / `jstack` để chẩn đoán treo
- [ ] Nói đúng 3 quy tắc virtual thread, đặc biệt quy tắc #3
- [ ] Nói đúng pinning theo Java 21, và biết nó đã thay đổi từ Java 24
