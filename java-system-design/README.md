# java-system-design — lộ trình System Design 6 tháng cho Senior Java

> Bản markdown, có bổ sung, của file PDF *Lộ trình System Design 6 tháng cho Senior Java Developer*
> (v1, 04/10/2026). Mục tiêu: qua vòng system design level senior **và** dẫn dắt được thiết kế ở công
> việc thật.
>
> Dùng chung cho cả hai hướng ứng tuyển: [NAB](../nab-prep/README.md) (capstone A, ngân hàng) và
> [Katalon](../katalon-prep/README.md) (capstone B, SaaS). Đề có lời giải sẵn ở
> [katalon-system-design](../katalon-prep/katalon-system-design/README.md).

---

## Đọc theo thứ tự

| # | File | Là gì | Trạng thái |
|:---:|---|---|---|
| **00** | [**Lộ trình 6 tháng (v2)**](00-lo-trinh-6-thang.md) ⭐ | Toàn bộ lộ trình: 4 giai đoạn, capstone, công ty mục tiêu, mốc kiểm tra. Có thêm cột Track P và Track S cho từng tuần | Xong |
| **01** | [**Track P — Code Java chậm dưới tải cao**](01-java-code-cham-duoi-tai-cao.md) ⭐ | 20 anti-pattern chia 4 nhóm; code xấu, code sửa, **số đo thật**, metric phát hiện, checklist review | Xong, 16/20 có code chạy và số đo |
| **02** | [**Track S — Vẽ hệ thống 100k → 1M → 10M users**](02-ve-he-thong-100k-1m-10m.md) ⭐ | Công thức users → DAU → CCU → RPS, thang 5 bậc L0–L4 có sơ đồ, lộ trình 10 bản vẽ | Xong |
| **10** | [**Implement giai đoạn 1 — Nền tảng (tuần 1–4)**](10-implement-gd1-nen-tang.md) | Hướng dẫn học từng buổi, lab, bài tập có đáp án, tiêu chí đạt mốc tuần 4 | Xong |
| **20** | [**Implement giai đoạn 2 — Dữ liệu và hệ phân tán (tuần 5–10)**](20-implement-gd2-du-lieu-phan-tan.md) | Replication, saga, Kafka sâu, đồng thuận; lab outbox + consumer idempotent, P20, bản vẽ V6–V7, khởi động capstone, mốc tuần 10 | Xong |
| **30** | [**Implement giai đoạn 3 — Microservices, cloud, vận hành (tuần 11–16)**](30-implement-gd3-microservices-cloud.md) | DDD và ranh giới module, load test Gatling tìm nút thắt, Resilience4j, OpenTelemetry + SLO burn rate, Keycloak + BOLA, AWS + DR, V8–V10, mốc tuần 12 và 16 | Xong |
| 40 | Implement giai đoạn 4 — Phỏng vấn (tuần 17–24) | Lịch đề, drill "một hệ thống, ba quy mô", STAR | Chưa viết |
| V1 | [**Video giai đoạn 1 — kế hoạch và nhật ký**](video-gd1/00-ke-hoach-va-lich-su.md) | 30 video tiếng Anh giọng Kokoro Tom (`am_michael`) cho tuần 1–4: danh sách, bảng phủ nội dung, phát âm, bảng trạng thái, nhật ký từng đợt. Kèm [bảng chữ viết tắt](video-gd1/01-bang-chu-viet-tat.md) (148 dòng) và [độ phủ nội dung tuần 1](video-gd1/02-do-phu-tuan-1.md). Xem trong app: tab **Video** | Tuần 1 đã dựng (Ep00–Ep06), chờ duyệt; tuần 2–4 chưa dựng |
| V2 | [**Video giai đoạn 2 — kế hoạch và nhật ký**](video-gd2/00-ke-hoach-va-lich-su.md) | 23 video tiếng Anh giọng Kokoro Emma (`af_heart`) cho tuần 5–10: replication, quorum, partitioning, saga, outbox, Kafka sâu, P20, stream processing, đồng hồ, Raft, khoá phân tán, capstone, mốc tuần 10. Kèm [bảng chữ viết tắt mới](video-gd2/01-bang-chu-viet-tat.md) (14 dòng) và [độ phủ nội dung](video-gd2/02-do-phu.md) (147/147 ý chính). Xem trong app: tab **Video** → **Giai đoạn 2** | Đã dựng đủ 23 video (≈ 123 phút), chờ duyệt |
| — | [perf-lab/](perf-lab/) | Module Maven: JMH + demo cho Track P. 33 test | Đã chạy |
| — | [ops-lab/](ops-lab/) | Lời giải tham khảo giai đoạn 3: Spring Boot + HikariCP + PostgreSQL 16 thật, load test Gatling, retry storm Resilience4j, khung Terraform đã `validate` | Đã chạy |
| — | [dist-lab/](dist-lab/) | Lời giải tham khảo giai đoạn 2: consistent hashing, saga, fencing, outbox trên Postgres 16 + Kafka 4.1.2 thật | Đã chạy |

---

## Hai track mới — vì sao thêm

**Track P (code).** Lộ trình v1 dạy kiến trúc: LB, cache, replica, Kafka. Nhưng kiến trúc chỉ nhân
năng lực của **một request**. Một đoạn `@Transactional` gọi HTTP 50 ms bên trong làm cả service chạm
trần **163 req/s** với pool 10 connection, thêm bao nhiêu pod cũng vậy. Senior phải nhìn ra điều đó
**trong code review**, trước khi nó lên production.

**Track S (vẽ).** Đề phỏng vấn nói "10 triệu user" nhưng không nói CCU hay RPS. Track này dạy đổi
con số đó ra tải thật, chọn đúng bậc kiến trúc, và trả lời câu "tải tăng 10 lần thì sao" theo một
khung cố định. Trả lời ngắn cho câu hay hỏi nhất:

| Đăng ký (app ngân hàng) | CCU đỉnh ngày lương | RPS đỉnh | Bậc |
|---:|---:|---:|---|
| 100k | ~1.200 | ~200 | L1 — modular monolith × 2 |
| 1M | ~12.000 | ~2.000 | L2 — read replica, cache, outbox → Kafka |
| 10M | ~120.000 | ~20.000 | L3 — microservices theo bounded context |

---

## Liên kết với phần còn lại của workspace

| Ở đâu | Dùng cho tuần nào |
|---|---|
| [katalon-system-design/](../katalon-prep/katalon-system-design/README.md) — 7 họ bài, 6 bài đầy đủ | Luyện đề giai đoạn 1 và 4; bài [06 race condition ledger](../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md) cho tuần 3 và 7 |
| [katalon-prep-java/05-postgres-depth](../katalon-prep/katalon-prep-java/05-postgres-depth/) — index, MVCC, `SKIP LOCKED` trên Postgres thật | Tuần 3 |
| [katalon-prep-java/06-distributed-resilience](../katalon-prep/katalon-prep-java/06-distributed-resilience/) — retry, jitter, Kafka simulator | Tuần 2, 8–9, 12 |
| [katalon-prep-java/08-system-design](../katalon-prep/katalon-prep-java/08-system-design/) — token bucket, circuit breaker | Tuần 2 (rate limiter) |
| [nab-prep/04 Kafka, Saga](../nab-prep/04-kafka-event-driven-saga.md) | Tuần 7–9 |
| [nab-prep/05 tiếng Anh phỏng vấn](../nab-prep/05-english-interview.md) | Mỗi tuần, phần trình bày 2 phút |
