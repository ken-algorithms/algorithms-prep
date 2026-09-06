# Module 08 — System design (KHÔNG cần Docker)

```bash
mvn -pl 08-system-design test    # 24 test pass
```

> **Đính chính:** ở ROADMAP lần trước tôi ghi module system design cần Docker — **sai**. System
> design là docs + diagram + primitive thuần Java. Không cần hạ tầng gì.

## Ý tưởng của module này

Đa số người ôn system design bằng cách đọc rồi vẽ hộp. Vấn đề: khi bị đào sâu, họ trả lời bằng **danh
từ** ("dùng token bucket", "dùng bin packing") mà không nói được **cơ chế** và **trade-off**.

Module này biến 5 primitive hay bị hỏi nhất thành **code chạy được, có test, có số đo**. Bạn tự viết,
tự đo, nên khi bị hỏi *"cụ thể thì rate limit thế nào?"* bạn trả lời bằng cơ chế.

| Primitive | File | Xuất hiện trong bài design nào |
|---|---|---|
| **Bin-packing (LPT)** | [`Scheduling.java`](src/main/java/com/prep/sysdesign/Scheduling.java) | Distributed Test Execution — chia test cho worker |
| **Weighted fair queueing** | cùng file | Multi-tenant SaaS — chống noisy neighbour |
| **Token bucket rate limiter** | [`Resilience.java`](src/main/java/com/prep/sysdesign/Resilience.java) | Quota theo tenant, bảo vệ ingest |
| **Circuit breaker** | cùng file | Resilience — JD ghi *"highly available"* |
| **Idempotency store** | cùng file | Kafka at-least-once + idempotent consumer |

## Số đo thật từ test

```text
=== makespan: chia-theo-so-luong=1200s   LPT=315s   (nhanh hon 3.81x)
=== 12 slot dau: [big-0..big-4, small-0, big-5..big-9, small-1]   (weight 5:1, free KHÔNG bị starvation)
```

**Bin-packing 3.81x** — con số này đáng mang vào phỏng vấn. 16 test (4×300s + 12×5s), 4 shard:

- Chia theo **số lượng**: 4 test nặng rơi hết vào shard 0 → makespan **1200s**
- **LPT**: mỗi shard một test nặng → makespan **315s**

Điểm sâu hơn: cách chia theo số lượng **phụ thuộc thứ tự đầu vào** → không dự đoán được. LPT thì
deterministic. "Không dự đoán được" là thứ tệ hơn "chậm" ở hệ thống production.

## Những điểm được cài sẵn để trả lời khi bị đào

| Câu hỏi | Có sẵn trong code/test |
|---|---|
| *"LPT có tối ưu không?"* | Không. Xấp xỉ `4/3 − 1/(3K)` của bài NP-hard, đổi lại `O(n log n)`. Ghi trong javadoc |
| *"Fairness có làm giảm throughput?"* | Có. Nhưng **work-conserving**: tenant hết việc thì tenant khác dùng hết slot — test `unusedQuotaIsNotWasted` |
| *"Vì sao token bucket, không fixed window?"* | Fixed window có bug **biên cửa sổ**: 100 req lúc 00:59 + 100 req lúc 01:00 = 200 req/2 giây |
| *"Vì sao token bucket, không leaky bucket?"* | Token bucket cho **burst** — đúng hành vi CI: im cả đêm rồi 500 test lúc 9h sáng |
| *"Rate limiter cần background thread?"* | Không — nạp **lazy** lúc đọc. 1 triệu tenant = 1 triệu timer là không scale được |
| *"Vì sao cần HALF_OPEN?"* | Nếu chỉ CLOSED/OPEN thì hết cool-down toàn bộ traffic đập vào downstream cùng lúc → **thundering herd** |
| *"Exactly-once có thật không?"* | Rất đắt. Cách rẻ và đúng: at-least-once + **idempotent consumer** |
| *"TTL của idempotency store đặt bao nhiêu?"* | **Phải lớn hơn** thời gian retry tối đa của producer. Test `ttlShorterThanRetryWindowCausesDoubleProcessing` chứng minh bug khi đặt sai |

## Chi tiết kỹ thuật đáng chú ý

**Mọi primitive nhận `LongSupplier` làm nguồn thời gian** thay vì gọi `System.currentTimeMillis()`
trực tiếp. Nhờ vậy test bơm thời gian giả (`FakeClock.advance(Duration.ofSeconds(30))`) — không
`Thread.sleep`, không flaky, chạy trong 0.006s. Đây là một trong những quyết định **testability**
quan trọng nhất khi viết code có yếu tố thời gian.

**Đồng hồ lùi được xử lý**: test `clockGoingBackwardsIsSafe` — NTP điều chỉnh ngược không được làm vỡ
bộ đếm. Bug này thật và rất khó tìm.

**Circuit breaker giữ nguyên exception gốc**, không bọc lại — test `originalExceptionIsPropagated`.

## Ba bài design đầy đủ

Nằm ở [tài liệu chiến lược](../../katalon-senior-lead-phong-van.md#5-ba-bài-system-design-on-domain):

1. **TrueTest — AI journey mining** ⭐ (quan trọng nhất)
2. **Distributed Test Execution Platform** → dùng `Scheduling.java` của module này
3. **Real-time Test Analytics Dashboard** → dùng partitioning + rollup của [module 05](../05-postgres-depth/)

## Quy trình 45 phút

```text
 5' Clarify      HỎI, đừng đoán: scale? tenant? real-time hay batch? ai review kết quả?
 5' Estimate     QPS, storage/tháng, cost. GHI SỐ LÊN BẢNG
 5' API + model  contract trước, schema sau
15' High-level   vẽ hộp, đi theo ĐƯỜNG DỮ LIỆU
10' Deep-dive    interviewer chọn 1 component
 5' Trade-off    + failure mode + "scale 10x thì gì vỡ trước"
```

> **Luật vàng cho Lead:** mỗi lựa chọn kiến trúc phải kèm một câu *"đánh đổi là..."*. Không có câu đó
> = nói như Senior, không phải Lead.

## Checklist

- [ ] Giải thích LPT + nói được nó không tối ưu và vì sao không cần tối ưu
- [ ] Vẽ được máy trạng thái circuit breaker 3 pha + vì sao cần HALF_OPEN
- [ ] Giải thích token bucket vs fixed window vs leaky bucket
- [ ] Giải thích at-least-once + idempotent thay cho exactly-once, và bẫy TTL
- [ ] Trình bày 1 bài design trong 35 phút với ≥4 trade-off
