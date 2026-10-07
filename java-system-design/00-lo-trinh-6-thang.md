# Lộ trình System Design 6 tháng cho Senior Java Developer

> **Bản v2 · cập nhật 04/10/2026.** Bản v1 là file PDF *Lộ trình System Design 6 tháng* (bản gốc trực
> tuyến: [Claude Doc](https://claude.ai/code/artifact/9a31764a-5f8b-4910-b449-33d0363de816)). File này
> giữ nguyên toàn bộ nội dung v1 và thêm hai track chạy xuyên suốt 24 tuần.
>
> Folder: [README](README.md) · Track P: [01 — Code Java chậm dưới tải cao](01-java-code-cham-duoi-tai-cao.md)
> · Track S: [02 — Vẽ hệ thống 100k → 1M → 10M users](02-ve-he-thong-100k-1m-10m.md)
> · Bài tập chi tiết: [10 — Implement giai đoạn 1](10-implement-gd1-nen-tang.md)

---

## Thay đổi so với v1

| | v1 (PDF) | v2 (file này) |
|---|---|---|
| **Track P — Code Java chậm dưới tải cao** | Chỉ có một dòng "virtual threads; GC ảnh hưởng tới p99" ở tuần 12 | 20 anti-pattern chia 4 nhóm, **có code chạy được và số đo thật**, rải vào tuần 1, 2, 3, 4, 8–9, 12, 13, 15–16. Xem [01](01-java-code-cham-duoi-tai-cao.md) |
| **Track S — Thang vẽ hệ thống** | Có đề bài, chưa có thang quy mô | Công thức **users → DAU → CCU → RPS**, thang 5 bậc L0→L4, **10 bản vẽ** từ một máy tới microservices 10M users, rải vào cả 24 tuần. Xem [02](02-ve-he-thong-100k-1m-10m.md) |
| Bảng từng giai đoạn | 3 cột | Thêm cột **Track P · Track S** để biết tuần đó làm gì của hai track |
| Tự đánh giá, mốc kiểm tra | 11 câu, 6 mốc | Thêm 3 câu tự đánh giá; thêm tiêu chí P/S vào các mốc |
| Bài tập chi tiết | Không có | Mỗi giai đoạn một file implement. **Đã có: [giai đoạn 1](10-implement-gd1-nen-tang.md), [giai đoạn 2](20-implement-gd2-du-lieu-phan-tan.md)** |

---

## Mục tiêu và giả định

Sau 24 tuần, bạn cần vượt được vòng system design level senior và tự dẫn dắt thiết kế ở công việc
thật. Cụ thể là ba kết quả:

- **Phỏng vấn:** trình bày trọn một thiết kế trong 45–60 phút bằng tiếng Anh, có số liệu ước lượng và
  trade-off rõ ràng.
- **Công việc:** viết design doc, review kiến trúc, chỉ ra điểm nghẽn và tình huống lỗi trước khi code.
  **(v2)** Review được cả **code**: chỉ ra đoạn Java nào sẽ thành điểm nghẽn khi tải lên.
- **Bằng chứng:** một dự án capstone có số liệu load test, cùng 2–3 câu chuyện thiết kế thật từ dự án
  hiện tại.

Lộ trình giả định bạn vẫn đi làm full-time và học 10–12 giờ/tuần. Nếu chỉ có 5–6 giờ/tuần, hãy giãn
thành 9–10 tháng. Nếu cần phỏng vấn trong 6–8 tuần, làm nhanh giai đoạn 1, đọc lướt giai đoạn 2–3 và
dồn sức cho giai đoạn 4.

Mỗi chủ đề đi đủ bốn bước: đọc, vẽ sơ đồ, chạy demo nhỏ bằng Java/Spring, rồi giải thích trade-off
trong 2 phút. Bước cuối chính là thứ người phỏng vấn chấm. Các công ty có HQ nước ngoài như NAB,
Katalon thường làm việc với team quốc tế, nên hãy luyện trình bày bằng tiếng Anh từ tháng đầu.

---

## Tự đánh giá điểm xuất phát

Tick những câu bạn trả lời trôi chảy trong 2 phút; câu nào chưa tick là lỗ hổng cần ưu tiên.

- [ ] Ước lượng QPS, dung lượng lưu trữ 5 năm và băng thông cho hệ thống 10 triệu user.
- [ ] **(v2)** Từ "1 triệu user đăng ký", tính ra DAU, **CCU đỉnh** và **RPS đỉnh**, nói rõ giả định nào làm con số đổi nhiều nhất.
- [ ] Giải thích replication đồng bộ và bất đồng bộ, và vì sao user có thể không thấy dữ liệu mình vừa ghi.
- [ ] Chọn shard key cho bảng giao dịch và xử lý hot partition.
- [ ] Phân biệt các isolation level, lost update, write skew; biết khi nào dùng `@Version`, khi nào dùng `SELECT ... FOR UPDATE`.
- [ ] Giải thích Kafka partition, consumer group, thứ tự message, at-least-once và exactly-once.
- [ ] So sánh cache-aside với write-through và cách chống cache stampede.
- [ ] Giải thích Saga, outbox pattern và idempotency key cho API thanh toán.
- [ ] Đặt timeout, retry có backoff + jitter và circuit breaker mà không gây retry storm.
- [ ] **(v2)** Kể được 5 đoạn code Java làm hệ thống chậm hoặc sập dù kiến trúc đúng, và **metric nào** phát hiện từng cái.
- [ ] **(v2)** Giải thích vì sao gọi HTTP bên trong `@Transactional` giới hạn throughput của cả service, bằng Little's Law.
- [ ] Định nghĩa SLI/SLO cho một API và đặt cảnh báo hợp lý.
- [ ] Vẽ kiến trúc multi-AZ trên AWS cho một service Spring Boot có database.
- [ ] Tự dẫn dắt một buổi thiết kế 45 phút mà không cần gợi ý.

Dưới 5 ô: đi đủ từ giai đoạn 1. Từ 5 đến 9 ô: lướt giai đoạn 1, dồn thời gian cho giai đoạn 2. Từ
10 ô trở lên: vào thẳng giai đoạn 3–4 và tập trung luyện đề. **Hai track P và S thì không bỏ**,
kể cả khi nhảy giai đoạn: chúng là phần nhiều người senior vẫn thiếu.

---

## Tổng quan lộ trình

Lộ trình gồm 4 giai đoạn trong 24 tuần; phần thực hành chạy song song để lý thuyết luôn có sản phẩm
đi kèm. **(v2)** Hai track mới chạy dọc cả 24 tuần, mỗi tuần 1–2 giờ, lấy từ giờ ôn tập và giờ lab.

```text
 LÝ THUYẾT VÀ LUYỆN ĐỀ                         THỰC HÀNH SONG SONG
 ┌───────────────────────────────────────┐     ┌──────────────────────────────────┐
 │ 1. Nền tảng · tuần 1–4                │ ──▶ │ Lab nhỏ bằng Spring Boot         │
 │    Ước lượng tải, networking và API   │     │ Rate limiter Redis/Bucket4j      │
 │    Database sâu, caching, nhất quán   │     │ Test tái hiện lost update        │
 └───────────────────┬───────────────────┘     └──────────────────────────────────┘
                     ◇ Mốc tuần 4: URL shortener + Rate limiter trong 45 phút
 ┌───────────────────▼───────────────────┐     ┌──────────────────────────────────┐
 │ 2. Dữ liệu và hệ phân tán · tuần 5–10 │ ──▶ │ Lab Kafka + outbox · tuần 8–9    │
 │    Replication, partitioning, consensus│     ├──────────────────────────────────┤
 │    Saga, outbox, idempotency, Kafka sâu│     │ CAPSTONE · tuần 9–16             │
 └───────────────────┬───────────────────┘     │ Bản A: ngân hàng · Bản B: SaaS   │
                     ◇ Mốc tuần 10             │ Design doc → MVP → load test     │
 ┌───────────────────▼───────────────────┐     │ Thử phá hệ thống, deploy cloud   │
 │ 3. Microservices và cloud · tuần 11–16│ ──▶ │ README có sơ đồ và số liệu       │
 │    Resilience, observability, security│     └──────────────────────────────────┘
 │    Kiến trúc trên AWS, DR, deployment │
 └───────────────────┬───────────────────┘
                     ◇ Mốc tuần 16: capstone chạy trên cloud, có số liệu load test
 ┌───────────────────▼───────────────────┐     ┌──────────────────────────────────┐
 │ 4. Phỏng vấn và ứng tuyển · tuần 17–24│ ──▶ │ Mock interview mỗi tuần          │
 │    3–4 đề mỗi tuần, có bấm giờ        │     │ Ít nhất 1 buổi bằng tiếng Anh    │
 │    Chuẩn bị theo công ty, CV có số liệu│     │ 6–8 câu chuyện STAR, nộp theo đợt│
 └───────────────────────────────────────┘     └──────────────────────────────────┘
                     ◇ Mốc tuần 24: CV mới, 6–8 câu chuyện STAR, nộp đơn đợt đầu

 ═══ (v2) CHẠY DỌC 24 TUẦN ═════════════════════════════════════════════════════════
  Track P  code Java chậm dưới tải: nhóm 1 (t1) → I/O & pool (t2–3) → bộ nhớ (t4)
           → Kafka consumer (t8–9) → concurrency, profiling (t12) → metric (t13) → review capstone (t15–16)
  Track S  vẽ hệ thống: L0/L1 100k (t1–4) → L2 1M (t6–10) → tách services (t11) → L3 10M (t15–16)
           → drill "một hệ thống, ba quy mô" (t17–24)
  Suốt 24 tuần ở công việc: design doc, ADR, đo p95/p99, SLO, on-call
```

Chỉ chuyển giai đoạn khi đạt mốc; mỗi mũi tên ngang nối phần lý thuyết với bài thực hành cùng thời
điểm.

### Lịch tuần mẫu

Một tuần điển hình có 5 buổi, tổng 10–11 giờ, dồn phần nặng vào cuối tuần.

| Ngày | Thời lượng | Việc |
|---|---|---|
| Thứ Hai | 1,5 giờ | Đọc sách theo giai đoạn, ghi chú bằng sơ đồ |
| Thứ Tư | 1,5 giờ | Đọc 1 bài engineering blog, tóm tắt vấn đề, giải pháp, trade-off |
| Thứ Sáu | 1 giờ | Ôn khái niệm, xem lại sổ lỗi. **(v2)** 30 phút track P: chạy một anti-pattern, ghi số vào sổ |
| Thứ Bảy | 3–4 giờ | Lab hoặc capstone: code, đo, thử phá hệ thống |
| Chủ nhật | 3 giờ | 1 đề bấm giờ 45 phút, rồi so với lời giải mẫu và ghi lỗi. **(v2)** Bản vẽ track S của tuần nằm trong buổi này |

Từ tuần 17, các buổi đọc chuyển sang luyện đề, còn Chủ nhật dành cho mock interview.

---

## Giai đoạn 1 — Nền tảng (tuần 1–4)

Tháng đầu biến kinh nghiệm Java/Spring thành tư duy hệ thống: ước lượng tải, hiểu đường đi của
request, chọn đúng database. **Hướng dẫn và bài tập từng ngày: [10 — Implement giai đoạn 1](10-implement-gd1-nen-tang.md).**

| Tuần | Học gì | Thực hành | **(v2) Track P · Track S** |
|---|---|---|---|
| 1 | Khung tư duy và ước lượng: scale từ 1 đến hàng triệu user, latency numbers, quy đổi tải (1 triệu request/ngày ≈ 12 QPS). Đọc Alex Xu Vol 1, chương 1–3. | Ước lượng QPS, storage, băng thông cho 3 hệ thống: app ngân hàng 5 triệu user, nền tảng chạy 1 triệu test/ngày, app chat. | **P:** nhóm 1 — chi phí CPU/rác mỗi request (P01–P07), học JMH, quy ra số core ở 10k RPS. **S:** công thức users → CCU → RPS; vẽ V1 (L0 + SPOF) và V2 (L1, 100k users) |
| 2 | Networking và API: DNS, TCP, HTTP/2, TLS, load balancer L4/L7, CDN, API gateway; REST, gRPC, WebSocket/SSE; phân trang bằng cursor, idempotency key. | Đề Rate limiter; cài token bucket bằng Redis + Lua hoặc Bucket4j trong Spring Boot. | **P:** P08 HttpClient mới mỗi request, P10 không timeout → nghẽn lan sang endpoint khác. **S:** V3 — rate limiter đặt ở đâu |
| 3 | Database sâu: B-tree và LSM-tree, index và query plan, isolation level, optimistic và pessimistic locking, read replica và replication lag, SQL hay NoSQL. Đọc DDIA các chương Storage and Retrieval, Transactions. | Đề URL shortener và Unique ID generator. Viết test tái hiện lost update rồi sửa bằng `@Version`. | **P:** P09 gọi HTTP trong `@Transactional`, P15 N+1, P16 `findAll()` + OFFSET sâu. **S:** V4 — URL shortener + ID generator |
| 4 | Caching và nhất quán: cache-aside, write-through, TTL, invalidation, cache stampede, hot key; Caffeine hay Redis; CAP, PACELC, read-your-writes. | Đề Notification system. Cuối tuần tự làm 1 đề trong 45 phút, ghi âm, nghe lại và ghi lỗi. | **P:** P18 cache không giới hạn → OOM, P19 nạp hết vào bộ nhớ. **S:** V5 — Notification system; hoàn thiện V2 với số liệu |

---

## Giai đoạn 2 — Dữ liệu và hệ phân tán (tuần 5–10)

Phần này phân biệt senior với mid-level: hiểu dữ liệu chạy ra sao khi có nhiều node, nhiều service
và mạng chập chờn. **Hướng dẫn và bài tập từng tuần: [20 — Implement giai đoạn 2](20-implement-gd2-du-lieu-phan-tan.md).**

| Tuần | Học gì | Thực hành | **(v2) Track P · Track S** |
|---|---|---|---|
| 5–6 | Replication (leader-follower, multi-leader, leaderless, quorum), partitioning, consistent hashing, rebalancing. Đọc DDIA các chương Replication, Partitioning/Sharding; xem loạt bài giảng Distributed Systems của Martin Kleppmann. | Đề Distributed key-value store và Distributed cache. | **S:** V6 — Mini Core Transfer ở **L2 (1M users)**: read replica + read-your-writes, cache-aside. Viết đoạn "×10 thì cái gì vỡ trước" |
| 7 | Transaction phân tán: vì sao tránh 2PC giữa microservices, Saga (orchestration và choreography), outbox + CDC (Debezium), idempotency, sổ cái kép (double-entry ledger). | Đề Payment system và Digital wallet (Alex Xu Vol 2). | **P:** đọc lại P09 dưới góc saga: bước gọi ngoài phải nằm **giữa** hai transaction ngắn |
| 8–9 | Kafka sâu: partition key và thứ tự, consumer group, rebalance, acks và ISR, idempotent producer, transaction, log compaction, DLQ, backpressure; stream processing ở mức khái niệm (window, late event). | Lab Spring Boot + Kafka + PostgreSQL: outbox, consumer idempotent, DLQ, test bằng Testcontainers. Đề Ad click aggregation. | **P:** P20 consumer gọi HTTP đồng bộ từng message → lag và rebalance storm; xử lý theo lô. **S:** V7 — Payment/wallet có Kafka + Saga |
| 10 | Đồng thuận và phối hợp: Raft, leader election, distributed lock và fencing token, đồng hồ và thứ tự sự kiện. Đọc DDIA các chương The Trouble with Distributed Systems, Consistency and Consensus. | Đề Hotel reservation (chống overbooking) và Distributed job scheduler. | **S:** cập nhật V6 với số đo từ lab Kafka |

---

## Giai đoạn 3 — Microservices, cloud và vận hành (tuần 11–16)

Sáu tuần này biến lý thuyết thành kiến trúc chạy thật trên cloud, đúng loại việc ngân hàng số và
công ty SaaS làm hằng ngày.

| Tuần | Học gì | Thực hành | **(v2) Track P · Track S** |
|---|---|---|---|
| 11 | Microservices và DDD: khi nào chưa nên tách (modular monolith), bounded context, API gateway, BFF, sync và async, CQRS, event sourcing. Đọc Microservices Patterns (Chris Richardson). | Vẽ lại hệ thống đang làm theo C4 model; chỉ ra 3 chỗ coupling chặt. | **S:** V8 — tách monolith V6 thành services, C4 container. Mỗi ranh giới tách phải có một câu "vì cái gì đang vỡ" |
| 12 | Resilience và hiệu năng: timeout budget, retry + backoff + jitter, circuit breaker, bulkhead, load shedding (Resilience4j); virtual threads; GC ảnh hưởng tới p99. Đọc Release It! và Amazon Builders' Library. | Load test bằng Gatling hoặc k6; tìm điểm nghẽn, sửa, đo lại. | **P (tuần chính của track):** P11 khoá toàn cục, P12 virtual thread pinning, P13 hàng đợi vô hạn, P14 `parallelStream` với I/O; profiling bằng JFR + async-profiler. Load test phải tìm ra **ít nhất một** anti-pattern trong code capstone |
| 13 | Observability: log, metric, trace với OpenTelemetry, Micrometer, Prometheus, Grafana; SLI, SLO, error budget; postmortem. Đọc Google SRE Book. | Đề Metrics monitoring and alerting; gắn tracing vào capstone. | **P:** dashboard "phát hiện anti-pattern": Hikari pending, executor queue size, GC pause, allocation rate. **S:** V9 — phủ observability + security lên V8 |
| 14 | Security, trọng tâm với ngân hàng: OAuth2/OIDC, rủi ro của JWT, mTLS, quản lý secret, mã hóa at rest và in transit, KMS, audit log, che PII, least privilege. | Đề xác thực và phân quyền cho mobile banking; dựng Keycloak cho capstone. | |
| 15–16 | Cloud (ưu tiên AWS, đổi sang Azure/GCP nếu công ty mục tiêu dùng): VPC, multi-AZ và multi-region, ALB, ECS/EKS, Lambda, RDS/Aurora, DynamoDB, SQS/SNS, MSK, S3; Well-Architected Framework; DR theo RTO/RPO; Terraform; blue/green, canary, feature flag. | Deploy capstone lên cloud, nhớ đặt budget alert. Tùy chọn: thi AWS Solutions Architect – Associate. | **P:** review toàn bộ code capstone theo checklist ở [01 §6](01-java-code-cham-duoi-tai-cao.md#6-checklist-review-code-trước-khi-lên-tải). **S:** V10 — Mini Core Transfer ở **L3 (10M)** trên AWS, multi-AZ + DR |

---

## Giai đoạn 4 — Luyện phỏng vấn và ứng tuyển (tuần 17–24)

Hai tháng cuối chuyển từ hiểu sang trình bày: luyện đề có bấm giờ, mock interview, rồi ứng tuyển
theo đợt.

| Tuần | Việc chính |
|---|---|
| 17–20 | Làm 3–4 đề/tuần, mỗi đề 45–60 phút có bấm giờ, viết lại lời giải rồi so với lời giải mẫu. Mỗi tuần ít nhất 1 mock interview bằng tiếng Anh với bạn bè, đồng nghiệp hoặc nền tảng mock. **(v2)** Mỗi tuần 1 lần drill **"một hệ thống, ba quy mô"** (vẽ V2 → V6 → V10 trong 45 phút) |
| 21–22 | Chuẩn bị theo từng công ty: domain, stack, giá trị văn hóa, sản phẩm mới nhất. Viết 6–8 câu chuyện STAR: sự cố production, quyết định kiến trúc có trade-off, cải thiện hiệu năng, bất đồng kỹ thuật, mentoring. **(v2)** Ít nhất 1 câu chuyện "cải thiện hiệu năng" lấy từ track P, có số trước/sau |
| 23–24 | Ứng tuyển theo đợt: 3–5 công ty để luyện tay trước, công ty mục tiêu sau. Xin referral qua LinkedIn và giữ nhịp 2–3 đề/tuần. |

Sửa CV từ mô tả công việc sang quyết định thiết kế có số liệu. Ví dụ, thay "Phát triển REST API bằng
Spring Boot" bằng "Thiết kế lại luồng đối soát bằng Kafka + outbox, rút thời gian xử lý từ 4 giờ
xuống 25 phút" (số liệu minh họa).

---

## (v2) Track P — Code Java chậm dưới tải cao

> Nội dung đầy đủ, code và số đo: **[01 — Code Java chậm dưới tải cao](01-java-code-cham-duoi-tai-cao.md)**

**Vì sao cần track này.** Một kiến trúc đúng (LB, autoscale, cache, replica) chỉ nhân năng lực của
**một request**. Nếu một request lãng phí, kiến trúc nhân luôn sự lãng phí đó. Tệ hơn, có loại code
không tốn CPU mà **giữ tài nguyên khan hiếm** (connection, thread, carrier thread) lâu hơn cần thiết.
Loại này đặt một trần cứng mà thêm máy không phá được.

| Nhóm | Bản chất | Anti-pattern | Tuần |
|---|---|---|:---:|
| **1. Lãng phí mỗi request** | CPU và rác × QPS = số core và áp lực GC | P01 object đắt tạo lại · P02 regex compile · P03 nối chuỗi trong vòng lặp · P04 exception làm luồng điều khiển · P05 autoboxing · P06 dựng chuỗi log khi level tắt · P07 O(n²) ẩn | 1 |
| **2. Giữ tài nguyên khan hiếm** | Little's Law: `đang bận = throughput × thời gian giữ` | P08 HttpClient mới mỗi request · P09 gọi ngoài trong `@Transactional` · P10 không timeout · P11 khoá toàn cục · P12 virtual thread pinning · P13 hàng đợi vô hạn · P14 `parallelStream` với I/O | 2, 3, 12 |
| **3. Round trip thừa** | Mỗi lần đi qua mạng tốn 0,5–1 ms dù chỉ lấy 1 dòng | P15 N+1 · P16 `findAll()` lọc trong Java, OFFSET sâu · P17 gọi remote trong vòng lặp | 3 |
| **4. Bộ nhớ** | Heap sống to → GC dài → p99 xấu → OOM | P18 cache không giới hạn · P19 nạp hết vào bộ nhớ · P20 Kafka consumer xử lý từng message đồng bộ | 4, 8–9 |

**Số đo thật trên máy lab** (chạy `perf-lab`, chi tiết ở file 01):

| Demo | Bản xấu | Bản sửa |
|---|---|---|
| P09 pool 10 connection, gọi ngoài 50 ms bên trong transaction | **~163 req/s**, p99 1,2 s | **~930 req/s**, p99 ~0,26 s |
| P10 đối tác chậm 2 s, không timeout | `/balance` (không liên quan) p99 **3,1 s** | p99 **5 ms** |
| P12 1.000 virtual thread, `synchronized` quanh I/O 20 ms, JDK 21 | **5,0 s** | **23 ms** |
| P15 trang 100 đơn hàng | **101 query**, 116 ms | **2 query**, 9 ms |
| P18 cache không giới hạn, heap 128 MB | **OOM** sau ~114k request | 1 triệu request, heap ổn định |
| P20 Kafka consumer gọi downstream từng message (Kafka thật) | Sau 45 s chưa xong, ~2.000 lần gửi trùng | Gọi theo lô: 1,3 s, 0 trùng |

Mốc của track: **tuần 4** chạy xong nhóm 1 bằng JMH và ghi số của máy mình; **tuần 12** load test
capstone tìm ra ít nhất một anti-pattern thật; **tuần 16** capstone qua checklist review.

---

## (v2) Track S — Vẽ hệ thống từ 100k tới 10M users

> Nội dung đầy đủ, sơ đồ và bảng tính: **[02 — Vẽ hệ thống 100k → 1M → 10M users](02-ve-he-thong-100k-1m-10m.md)**

**Trả lời thẳng câu "100k users thì CCU là bao nhiêu":** với app ngân hàng (30% hoạt động mỗi ngày,
8 phút dùng mỗi ngày, giờ cao điểm 10% lưu lượng):

| Đăng ký | CCU đỉnh thường | CCU ngày lương (× 3) | RPS đỉnh ngày lương | Bậc kiến trúc |
|---:|---:|---:|---:|---|
| **100k** | ~400 | ~1.200 | **~200** | **L1** — modular monolith × 2, Postgres Multi-AZ, Redis |
| **1M** | ~4.000 | ~12.000 | **~2.000** | **L2** — autoscale, read replica, cache-aside, outbox → Kafka, tách 2–3 service có lý do |
| **10M** | ~40.000 | ~120.000 | **~20.000** | **L3** — microservices theo bounded context, CQRS read model, CDC, PgBouncer, multi-AZ + DR |

Với app chat (phiên 60 phút), cùng số user đăng ký thì CCU **gấp 12 lần**. Vì vậy câu hỏi đầu tiên
luôn là *"một phiên dùng bao lâu?"*.

**Lộ trình 10 bản vẽ** (chi tiết ở file 02 §4): V1 L0 + SPOF (t1) → V2 L1 100k (t1) → V3 rate
limiter (t2) → V4 URL shortener (t3) → V5 notification (t4) → V6 Mini Core Transfer 1M (t6) → V7
payment + saga (t8–9) → V8 tách services C4 (t11) → V9 observability + security (t13–14) → V10 10M
trên AWS (t15–16) → drill "một hệ thống, ba quy mô" (t17–24).

---

## Dự án thực hành (capstone)

Chọn một trong hai dự án theo công ty mục tiêu, làm từ tuần 9 đến tuần 16; đây là câu chuyện chính
để kể khi phỏng vấn.

| Tiêu chí | A. Mini Core Transfer (ngân hàng) | B. Distributed Test Runner (SaaS) |
|---|---|---|
| Hợp với | NAB, ngân hàng số, ví điện tử | Katalon, công ty SaaS và dev tools |
| Chức năng | Mở tài khoản, chuyển tiền nội bộ, lịch sử giao dịch, thông báo | Nhận yêu cầu chạy test từ API hoặc CI webhook, chia job cho worker, stream log realtime, lưu screenshot/video, dashboard kết quả |
| Điểm thiết kế phải có | Sổ cái kép, idempotency key, Saga + outbox, chống chi trùng, đối soát cuối ngày, audit log | Queue + worker autoscale, chia tài nguyên công bằng giữa tenant, timeout/retry cho job treo, log realtime qua WebSocket/SSE, pre-signed URL lên S3/MinIO, webhook có retry |
| Mở rộng nếu còn thời gian | Phát hiện gian lận đơn giản bằng Kafka Streams | Bước sinh test bằng LLM: hàng đợi riêng, rate limit, audit trail ghi rõ người hay AI tạo mỗi phiên bản |
| Thí nghiệm chứng minh | Gửi lại một request 100 lần chỉ ra 1 giao dịch; kill consumer giữa chừng không mất hay trùng tiền; load test 500 TPS | Một tenant đẩy 10.000 job không làm nghẽn tenant khác; kill worker giữa chừng job vẫn chạy lại đúng; autoscale theo độ dài queue (KEDA) |
| **(v2) Track S** | Vẽ capstone ở 3 bậc: 100k (L1), 1M (L2), 10M (L3) | Cùng cách, với "users" thay bằng "test chạy mỗi ngày" |
| **(v2) Track P** | Load test tìm và sửa ít nhất 1 anti-pattern; ghi số trước/sau vào README | Như bên trái |

Stack chung: Java 21 trở lên, Spring Boot bản mới, PostgreSQL, Kafka, Redis, Testcontainers,
OpenTelemetry, Docker/Kubernetes, Gatling hoặc k6. **(v2)** Thêm JMH, JFR, async-profiler cho track P.

Quy trình: viết design doc trước khi code (tuần 9), làm MVP (tuần 10–14), load test và thử phá hệ
thống (tuần 15–16). Cuối cùng viết README có sơ đồ, số liệu và các trade-off đã chọn.

---

## Áp dụng vào công việc hiện tại

Câu chuyện thiết kế thật thuyết phục hơn mọi lời giải học thuộc, nên hãy tạo ra chúng ngay ở dự án
đang làm.

- Viết design doc cho feature tiếp theo bạn nhận: bối cảnh, mục tiêu, ước lượng tải, phương án chọn,
  phương án loại và lý do, tình huống lỗi, kế hoạch rollout. Nhờ tech lead review.
- Ghi mỗi quyết định kiến trúc thành một ADR dài khoảng một trang, lưu ngay trong repo.
- Chọn một API chậm, đo p95/p99, tìm nguyên nhân (query, connection pool, gọi tuần tự), sửa rồi đo
  lại; giữ số liệu trước và sau. **(v2)** Dùng checklist ở [01 §6](01-java-code-cham-duoi-tai-cao.md#6-checklist-review-code-trước-khi-lên-tải) để tìm nhanh.
- Rà các luồng có rủi ro mất hoặc trùng dữ liệu, rồi đề xuất idempotency, outbox hoặc retry có backoff.
- **(v2)** Lấy access log một ngày của hệ thống đang làm, tính CCU và RPS đỉnh thật, so với công thức
  ở [02 §1](02-ve-he-thong-100k-1m-10m.md#1-từ-n-users-ra-ccu-và-rps--công-thức-4-bước). Chênh lệch
  giữa giả định và thực tế là một câu chuyện phỏng vấn tốt.
- Định nghĩa SLO cho 1–2 service quan trọng; xin tham gia on-call và viết postmortem.
- Trình bày lại mỗi chủ đề đã học trong một buổi tech talk nội bộ 20 phút.

Mỗi việc trên là một câu chuyện STAR sẵn sàng cho giai đoạn 4.

---

## Khung trả lời phỏng vấn (45–60 phút)

Người phỏng vấn level senior chấm cách bạn dẫn dắt và cân nhắc trade-off, không chấm việc nhớ đúng
một sơ đồ mẫu.

1. **Làm rõ yêu cầu (5–7 phút):** chốt 3–5 tính năng chính; số user, QPS, độ trễ, availability, mức
   nhất quán cần có; những gì nằm ngoài phạm vi. **(v2)** Hỏi độ dài phiên và có giữ kết nối hay không.
2. **Ước lượng (3–5 phút):** QPS đọc/ghi lúc cao điểm, dung lượng 5 năm, băng thông. Chỉ tính những
   con số làm thay đổi thiết kế. **(v2)** Đi theo 4 bước users → DAU → CCU → RPS.
3. **API và data model (5–8 phút):** các endpoint chính, schema, chọn database và lý do.
4. **Thiết kế tổng thể (khoảng 10 phút):** sơ đồ end-to-end chạy được cho luồng chính, chưa vội tối ưu.
5. **Đào sâu (15–20 phút):** 2–3 điểm khó nhất như scale, nhất quán, hot spot, lỗi giữa chừng; mỗi điểm
   nêu ít nhất 2 phương án và trade-off. **(v2)** Một điểm đào sâu tốt: *"đoạn code nào trong service này
   sẽ thành nút thắt trước cả DB"*.
6. **Tổng kết (3–5 phút):** monitoring, bảo mật, rủi ro còn lại, hướng mở rộng khi tải tăng 10 lần.
   **(v2)** Trả lời theo khung 5 câu ở [02 §5](02-ve-he-thong-100k-1m-10m.md#5-câu-hỏi-tải-tăng-10-lần-thì-sao--cách-trả-lời).

Dấu hiệu level senior: tự dẫn dắt, nói rõ đổi cái gì lấy cái gì, có số liệu, nghĩ tới vận hành
(deploy, migrate dữ liệu, rollback) và không over-engineer.

---

## Bộ đề luyện tập

Làm hết nhóm nền tảng trước, sau đó ưu tiên nhóm khớp công ty mục tiêu; tổng cộng khoảng 30 đề trong
6 tháng.

| Nhóm | Đề | Hợp với |
|---|---|---|
| Nền tảng (bắt buộc) | URL shortener, Rate limiter, Unique ID generator, Notification system, Key-value store, News feed, Chat, Search autocomplete, Web crawler, Google Drive | Mọi công ty |
| Fintech, ngân hàng | Payment system, Digital wallet, chuyển tiền nội bộ và liên ngân hàng, phát hiện gian lận realtime, sao kê cho hàng triệu tài khoản, Open Banking API, Stock exchange | NAB, TymeX, Money Forward, ngân hàng số, ví điện tử |
| SaaS, dev tools | Nền tảng chạy test phân tán, CI/CD pipeline, Distributed job scheduler, thu thập và tìm kiếm log, analytics multi-tenant, Webhook delivery, Feature flag service | Katalon, Employment Hero, Parcel Perform, KiotViet |
| Super-app, e-commerce, du lịch | Ride-hailing và tìm quanh đây, giao đồ ăn, flash sale, Hotel reservation, bán vé sự kiện, Top-K trending | Grab, Shopee, Agoda, Be, Tiki |

Bài đã có lời giải đầy đủ trong workspace này: [katalon-system-design](../katalon-prep/katalon-system-design/README.md)
(6 bài, 7 họ bài, 5 trục nhận diện).

---

## Tài liệu chọn lọc

Đọc theo thứ tự Alex Xu Vol 1 → DDIA → Microservices Patterns → Release It!; các tài liệu còn lại
chỉ dùng khi cần đào sâu.

| Tài liệu | Loại | Dùng ở |
|---|---|---|
| System Design Interview Vol 1 và 2 (Alex Xu, Sahn Lam) | Sách | Giai đoạn 1 và 4: khung trả lời, lời giải mẫu |
| Designing Data-Intensive Applications (Martin Kleppmann) | Sách | Giai đoạn 2: nền tảng dữ liệu, quan trọng nhất |
| Microservices Patterns (Chris Richardson) | Sách | Giai đoạn 3: ví dụ Java/Spring, Saga, outbox |
| Release It! (Michael Nygard) | Sách | Giai đoạn 3: thiết kế cho production |
| Understanding Distributed Systems (Roberto Vitillo) | Sách | Đọc trước DDIA nếu thấy DDIA nặng |
| Fundamentals of Software Architecture (Mark Richards, Neal Ford) | Sách | Tư duy kiến trúc và trade-off |
| Learning Domain-Driven Design (Vlad Khononov) | Sách | Tuần 11: bounded context |
| **(v2)** Java Performance, 2nd ed. (Scott Oaks) | Sách | Track P: JIT, GC, heap, đo lường |
| **(v2)** [JMH](https://github.com/openjdk/jmh), [async-profiler](https://github.com/async-profiler/async-profiler), JFR + JDK Mission Control | Công cụ | Track P: đo đúng, tìm điểm nóng |
| **(v2)** [HikariCP — About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing) | Bài viết | Track P nhóm 2: vì sao pool nhỏ lại nhanh hơn |
| [Hello Interview](https://www.hellointerview.com/), [ByteByteGo](https://bytebytego.com/) | Web, khóa học | Lời giải mẫu có phân tích sâu |
| [system-design-primer](https://github.com/donnemartin/system-design-primer) | Repo miễn phí | Ôn nhanh khái niệm |
| Distributed Systems lectures (Martin Kleppmann, Cambridge) | Video miễn phí trên YouTube | Giai đoạn 2 |
| [Amazon Builders' Library](https://aws.amazon.com/builders-library/), AWS Well-Architected Framework | Bài viết miễn phí | Resilience, cloud, **(v2)** cell-based cho L4 |
| [Google SRE Book](https://sre.google/books/) | Sách miễn phí | SLO, monitoring, postmortem |
| Engineering blog của Netflix, Uber, Discord, Stripe, Grab, Agoda, Canva | Blog | Mỗi tuần 1 bài case study |

---

## Công ty mục tiêu

Ngoài NAB và Katalon, nhóm sát nhất với hồ sơ của bạn là trung tâm công nghệ của ngân hàng, fintech
và SaaS nước ngoài đặt tại Việt Nam.

### Ở Việt Nam

| Công ty | Họ xây gì | Nên luyện thêm |
|---|---|---|
| [NAB Innovation Centre Vietnam](https://www.secondtalent.com/resources/global-capability-centers-vietnam/) | Hơn 2.000 người ở TP.HCM và Hà Nội, xây digital banking cho khách hàng NAB tại Úc và New Zealand: AI, software engineering, security, data | Payment, ledger, bảo mật, cloud |
| [Katalon](https://itbrief.asia/story/katalon-launches-ai-testing-platform-for-software-quality) | HQ Atlanta; True Platform (ra mắt 4/2026) gộp automation, manual test, execution, analytics, test management, production monitoring; AI agent có audit trail và bước người duyệt | Chạy job phân tán, multi-tenant SaaS, audit trail, tích hợp DevOps |
| [GoTymeX (trước là TymeX)](https://www.secondtalent.com/resources/global-capability-centers-vietnam/) | Đội TP.HCM xây nền tảng cho hai ngân hàng số TymeBank (Nam Phi) và GoTyme Bank (Philippines) | Core banking, ledger, onboarding số |
| [Money Forward Vietnam](https://www.secondtalent.com/resources/global-capability-centers-vietnam/) | Fintech SaaS cho thị trường Nhật: ERP, payroll, công cụ ngân hàng; ngôn ngữ làm việc là tiếng Anh | Multi-tenant SaaS, tích hợp ngân hàng, batch |
| [Axon Vietnam](https://www.secondtalent.com/resources/global-capability-centers-vietnam/) | Khoảng 400 người; nền tảng bằng chứng số, livestream từ body camera, AI viết báo cáo | Lưu trữ và stream video, xử lý sự kiện, bảo mật dữ liệu |
| [Employment Hero, Parcel Perform, KiotViet](https://itviec.com/blog/30-vietnam-best-it-companies-2026/) | SaaS nhân sự và lương, theo dõi vận chuyển, quản lý bán lẻ | Multi-tenant, job scheduler, webhook |
| [Rakuten Fintech Vietnam, GFT, Thoughtworks, EPAM](https://itviec.com/blog/30-vietnam-best-it-companies-2026/) | Fintech và tư vấn kỹ thuật cho khách hàng toàn cầu | Microservices, cloud, tùy dự án |
| MoMo, ZaloPay (VNG), Techcombank, MB Bank, VPBank | Ví điện tử và ngân hàng nội địa có lượng giao dịch rất lớn | Payment, chịu tải đỉnh, chống gian lận |

Dòng cuối là gợi ý theo hiểu biết chung, chưa đối chiếu nguồn. Thị trường 2026 khắt khe hơn: theo
[ITviec](https://itviec.com/blog/30-vietnam-best-it-companies-2026/), số đơn mỗi ứng viên nộp tăng từ
6,1 lên 7,5 nhưng số offer vẫn giữ 2,7.

### Quốc tế

| Khu vực | Công ty gợi ý | Điểm hợp |
|---|---|---|
| Úc | NAB (Melbourne), Atlassian, Canva | Ngân hàng lớn và SaaS quy mô lớn |
| Singapore | Grab, Shopee/Sea, DBS, Airwallex | Super-app, thanh toán, ngân hàng số |
| Thái Lan | Agoda, LINE MAN Wongnai | Đặt phòng và marketplace quy mô lớn |
| Nhật | Rakuten, PayPay, Money Forward | Fintech, e-commerce; nhiều nhóm làm việc bằng tiếng Anh |
| Châu Âu | Booking.com, Adyen, Zalando, Wise | Thanh toán, e-commerce; backend chủ yếu trên JVM |

Bảng quốc tế là gợi ý theo hiểu biết chung. Visa, relocation và vị trí tuyển thay đổi liên tục, nên
kiểm tra trang careers trước khi nộp.

---

## Mốc kiểm tra và sai lầm thường gặp

Tick từng mốc khi đạt; trễ quá 2 tuần ở mốc nào thì cắt bớt phạm vi thay vì kéo dài cả lộ trình.

- [ ] **Tuần 4:** làm trọn URL shortener và Rate limiter trong 45 phút, có ước lượng số liệu. **(v2)** Tính được CCU/RPS cho 100k, 1M, 10M users; chạy xong nhóm 1 của track P bằng JMH trên máy mình; có bản vẽ V2 (L1).
- [ ] **Tuần 10:** giải thích replication, partitioning, Saga/outbox, Kafka delivery semantics mà không cần tài liệu; lab Kafka + outbox chạy được. **(v2)** Có bản vẽ V6 (L2, 1M) với đoạn "×10 thì vỡ gì trước".
- [ ] **Tuần 12:** có design doc đầu tiên được review ở công việc thật. **(v2)** Load test capstone tìm và sửa được ít nhất 1 anti-pattern, có số trước/sau.
- [ ] **Tuần 16:** capstone chạy trên cloud, có số liệu load test và README. **(v2)** Có bản vẽ V10 (L3, 10M); capstone qua checklist review của track P.
- [ ] **Tuần 20:** đã luyện 20+ đề và 4+ buổi mock interview. **(v2)** Đã làm drill "một hệ thống, ba quy mô" ít nhất 3 lần.
- [ ] **Tuần 24:** CV mới, 6–8 câu chuyện STAR, đã nộp đơn đợt đầu.

Sai lầm thường gặp:

- Học thuộc lời giải mẫu; người phỏng vấn đổi một yêu cầu nhỏ là lộ ngay.
- Vẽ sơ đồ trước khi hỏi yêu cầu và ước lượng số liệu.
- Chỉ đọc mà không xây, nên không có số liệu thật để kể.
- Over-engineer: microservices, Kafka, sharding cho hệ thống chỉ 50 QPS.
- Bỏ qua tình huống lỗi: mạng chập chờn, service chết giữa chừng, retry gây trùng dữ liệu.
- Im lặng suy nghĩ; trong phỏng vấn hãy nói to quá trình cân nhắc.
- **(v2)** Nhầm "N users" với "N request đồng thời"; quên hỏi độ dài phiên.
- **(v2)** Tin rằng thêm pod sẽ cứu được mọi thứ. Pool connection, khoá và hàng đợi không scale theo số pod.
- **(v2)** Đo hiệu năng bằng `System.currentTimeMillis()` quanh một vòng lặp; không warm-up, không nhìn p99.

---

## Nguồn

- [Global Capability Centers in Vietnam: List of 15 GCCs](https://www.secondtalent.com/resources/global-capability-centers-vietnam/) — Second Talent, 23/09/2026
- [Top 30 Best IT Companies in Vietnam 2026](https://itviec.com/blog/30-vietnam-best-it-companies-2026/) — ITviec, 11/03/2026
- [Katalon launches AI testing platform for software quality](https://itbrief.asia/story/katalon-launches-ai-testing-platform-for-software-quality) — IT Brief Asia, 09/04/2026
- **(v2)** [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491) — OpenJDK, dùng cho P12
- **(v2)** [JEP 444: Virtual Threads](https://openjdk.org/jeps/444) — OpenJDK
