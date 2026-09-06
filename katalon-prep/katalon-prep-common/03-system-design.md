# System design — quy trình + primitive

> **Ba bài design đầy đủ nằm ở [tài liệu chiến lược §5](../katalon-senior-lead-phong-van.md#5-ba-bài-system-design-on-domain).**
> File này là phần *quy trình* và *primitive* — thứ dùng lại cho mọi bài, không phụ thuộc ngôn ngữ.
>
> Code chạy được: [Java `08-system-design/`](../katalon-prep-java/08-system-design/) (24 test)
> · [Python `distributed/resilience.py`](../katalon-prep-python/src/prep/distributed/resilience.py) (19 test)
> · [AI agent → file riêng](04-ai-agent-system-design.md) ⭐

## 1. Quy trình 45 phút

```text
 5'  Clarify      HỎI, đừng đoán: scale? multi-tenant? real-time hay batch? ai đọc kết quả?
 5'  Estimate     QPS, storage/tháng, cost. GHI SỐ LÊN BẢNG
 5'  API + model  contract trước, schema sau
15'  High-level   vẽ hộp, đi theo ĐƯỜNG DỮ LIỆU từ đầu đến cuối
10'  Deep dive    interviewer sẽ chọn 1 chỗ — chuẩn bị sẵn 2 chỗ bạn muốn bị chọn
 5'  Failure      cái gì vỡ, phát hiện thế nào, chữa thế nào
```

**Ba lỗi làm mất điểm nhiều nhất:**

1. **Nhảy vào vẽ hộp ngay** — bỏ qua clarify. Interviewer đang chờ xem bạn có hỏi hay không;
   đó là một phần của bài thi, không phải phần mở đầu lịch sự.
2. **Không ghi số.** "Nhiều dữ liệu" là vô nghĩa. "50k test/phút × 200 byte = 10 MB/phút =
   14 GB/ngày = 5 TB/năm" là một câu trả lời.
3. **Chỉ nói đường đi đẹp.** Phần failure mode là chỗ phân biệt Senior với Lead.

## 2. Bốn con số phải ước lượng được không cần giấy

| | Cách nhẩm |
|---|---|
| **QPS** | `sự_kiện/ngày ÷ 86400`. Nhân 3–5 cho giờ cao điểm |
| **Storage/năm** | `QPS × byte/bản_ghi × 3.15×10⁷` |
| **Đủ 1 máy?** | 1 máy làm được ~10k QPS đọc đơn giản, ~1k QPS ghi có index |
| **Cost** | Aurora ~$0.10/GB-tháng; egress đắt hơn storage nhiều |

## 3. Primitive — cùng thuật toán, hai ngôn ngữ

| Primitive | Điểm interviewer đào | Java | Python |
|---|---|---|---|
| **Token bucket** | vì sao không fixed window (bug biên cửa sổ); nạp **lười** để 10k tenant không cần 10k timer | `Resilience.java` | `TokenBucket` |
| **Circuit breaker** | HALF_OPEN chỉ cho **MỘT** request thử qua; một lần hỏng ở HALF_OPEN → mở lại **ngay** | `Resilience.java` | `CircuitBreaker` |
| **Retry + jitter** | không jitter → **thundering herd**; `cap` để lần 20 không đợi 12 ngày; **lỗi vĩnh viễn không retry** | `Retries.java` | `RetryPolicy` |
| **Idempotency store** | **bẫy TTL**: ngắn hơn độ trễ giao lại tối đa → mất tính idempotent, im lặng | `Resilience.java` | `IdempotencyStore` |
| **Bin-packing LPT** | NP-hard, LPT là xấp xỉ 4/3. Đo được **1200s → 315s = 3.81×** | `Scheduling.java` | — |
| **Weighted fair queue** | work-conserving: hàng đợi rỗng thì nhường slot, không để trống | `Scheduling.java` | — |
| **Partition / hot key** | skew ratio; salting; **phải salt ≥ 16× số partition** | `KafkaSimulator.java` | `salted_partition` |

### Ba con số đo thật, mang vào phỏng vấn được

```text
bin-packing LPT      makespan 1200s → 315s        = 3.81×
fixed window bug     200 request trong 1.1 giây mà vẫn "đúng luật"
salting hot key      1× partition: skew 2.80×  ·  2×: 3.70× (TỆ HƠN)  ·  16×: 1.22×
```

Con số cuối là thứ hầu hết hướng dẫn không nói. "Thêm salt là xong" là sai: bạn chỉ đổi một key
nóng thành N key vừa, mà N key đó **lại** hash không đều vào partition (bài toán sinh nhật). Nói
được con số này là khác biệt giữa *có đọc về salting* và *đã làm thật*.

## 4. Retry và circuit breaker giải hai bài toán khác nhau

Đây là câu hỏi bẫy hay gặp: *"đã có retry rồi cần circuit breaker làm gì?"*

```text
retry           bảo vệ MỘT request      → làm sự cố NẶNG THÊM khi backend quá tải
circuit breaker bảo vệ CẢ HỆ THỐNG      → ngắt hẳn luồng đi khi đã rõ là hỏng
```

Retry một mình nhân lưu lượng lên gấp 3 đúng lúc backend đang chết. Cần **cả hai**, và cần
**jitter** — nếu không thì mọi client thức dậy cùng một lúc và đập chết service vừa ngồi dậy.

## 5. Testability — quyết định quan trọng nhất khi có yếu tố thời gian

**Mọi primitive nhận đồng hồ và bộ sinh ngẫu nhiên qua tham số**, không gọi
`System.currentTimeMillis()` / `time.monotonic()` / `random.random()` trực tiếp.

Nhờ vậy test bơm thời gian giả (`clock.advance(30)`) — không `sleep`, không flaky, chạy trong
micro giây. Đây chính là câu trả lời cho *"làm sao bạn test được retry/timeout/circuit breaker?"*

Và một bẫy thật kèm theo: **đồng hồ lùi được.** NTP điều chỉnh ngược không được làm vỡ bộ đếm.
Test `clockGoingBackwardsIsSafe` — bug này thật và rất khó tìm.

## 6. Ba bài design on-domain

Chi tiết ở [tài liệu chiến lược §5](../katalon-senior-lead-phong-van.md#5-ba-bài-system-design-on-domain).
Mỗi bài đều dùng lại primitive ở §3:

| Bài | Primitive dùng lại | Chỗ nên chủ động dẫn vào deep dive |
|---|---|---|
| **TrueTest — AI journey mining** ⭐ | agent + guardrail + RAG | prompt injection từ DOM ([file riêng](04-ai-agent-system-design.md) §2) |
| **Distributed Test Execution** | bin-packing, WFQ, token bucket, idempotency | vì sao LPT chứ không round-robin (3.81×) |
| **Real-time Test Analytics** | partitioning + rollup + BRIN | vì sao `percentile_disc` tính trong DB |

**Chuẩn bị sẵn hai chỗ bạn *muốn* bị đào.** Interviewer sẽ chọn một chỗ để deep dive — nếu bạn
đã cài sẵn hai chỗ có số đo thật thì bạn chọn thay họ.

## 7. Không cần Docker cho bất kỳ phần nào

Bạn đã xoá Docker để tiết kiệm disk. Cả `08-system-design` (Java) và `distributed/` (Python) đều
là primitive thuần — không hạ tầng, không container. Postgres dùng embedded binary, Kafka mô phỏng
ngữ nghĩa bằng code. Xem [README gốc](../README.md).
