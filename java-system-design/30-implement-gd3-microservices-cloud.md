# Implement giai đoạn 3 — Microservices, cloud và vận hành (tuần 11–16): hướng dẫn học và bài tập

> **Technical details cho giai đoạn 3** của [lộ trình 6 tháng](00-lo-trinh-6-thang.md#giai-đoạn-3--microservices-cloud-và-vận-hành-tuần-1116).
> Sáu tuần này biến lý thuyết thành kiến trúc chạy thật: tách service có lý do, load test tìm điểm
> nghẽn, đo được, bảo mật, rồi đưa capstone lên AWS.
>
> Giai đoạn trước: [20 — Implement giai đoạn 2](20-implement-gd2-du-lieu-phan-tan.md) · Lời giải tham
> khảo đã chạy: [ops-lab/](ops-lab/) · Track P: [01](01-java-code-cham-duoi-tai-cao.md) · Track S: [02](02-ve-he-thong-100k-1m-10m.md)
>
> Hai mốc: [tuần 12](#mốc-tuần-12) và [tuần 16](#mốc-tuần-16--tiêu-chí-qua-giai-đoạn).

---

## 0. Trước khi bắt đầu

**Điều kiện vào:** đã qua [mốc tuần 10](20-implement-gd2-du-lieu-phan-tan.md#mốc-tuần-10--tiêu-chí-qua-giai-đoạn):
có design doc capstone v1 và khung MVP chạy được. Giai đoạn này **làm trên capstone**, không làm bài
rời: mỗi tuần thêm một lớp (ranh giới module, khả năng chịu lỗi, quan sát, bảo mật, cloud).

**Bản đồ 6 tuần:**

| Tuần | Chủ đề | Lab (trên capstone) | Track P | Track S | Đề bấm giờ |
|---|---|---|---|---|---|
| 11 | Microservices và DDD | Event storming, context map, ranh giới module bằng test | — | **V8**: tách V6 thành services (C4) | Tách monolith ngân hàng |
| 12 | Resilience và hiệu năng | **Load test bằng Gatling**, tìm và sửa điểm nghẽn; Resilience4j | **Tuần chính**: P11–P14, JFR, async-profiler | — | Flash sale |
| 13 | Observability, SLO | OpenTelemetry, dashboard, cảnh báo theo burn rate, postmortem | Dashboard phát hiện anti-pattern | **V9**: phủ observability + security lên V8 | Metrics monitoring and alerting |
| 14 | Security cho ngân hàng | Keycloak + Spring Security, test phân quyền theo đối tượng, audit log | — | | Auth cho mobile banking |
| 15–16 | AWS, DR, deploy | Terraform, deploy capstone, canary, diễn tập DR | Review capstone theo checklist 01 §6 | **V10**: Mini Core Transfer ở L3 (10M) trên AWS | DR đa region cho core banking |

**Thư mục bài làm** (tiếp tục `my-work/`):

```text
java-system-design/my-work/
├── capstone/
│   ├── design-doc.md            cập nhật mỗi tuần
│   ├── adr/                     mỗi quyết định một file
│   ├── load-test/               simulation Gatling + bảng số trước/sau (tuần 12)
│   ├── observability/           dashboard JSON, rule cảnh báo, postmortem (tuần 13)
│   └── infra/                   Terraform (tuần 15–16)
└── drawings/V8, V9, V10
```

**Lời giải tham khảo:** [ops-lab](ops-lab/) chạy đúng quy trình tuần 12 trên Spring Boot 3.3 + HikariCP
+ PostgreSQL 16 thật + Gatling, kèm demo retry storm và khung Terraform cho tuần 15–16. Tự làm trên
capstone trước, đối chiếu sau.

---

## Tuần 11 — Microservices và DDD: tách khi có lý do

### Đầu ra phải có cuối tuần

- [ ] Event storming cho capstone, ra danh sách bounded context và **context map** (quan hệ giữa chúng).
- [ ] Capstone là **modular monolith** có test chặn import chéo giữa module.
- [ ] C4 container diagram của **hệ thống đang làm ở công ty**, chỉ ra 3 chỗ coupling chặt.
- [ ] Bản vẽ **V8**.
- [ ] Bài nói 2 phút: *"When would you NOT split a system into microservices?"*

### Đọc

| Buổi | Tài liệu | Phần |
|---|---|---|
| Thứ Hai | Microservices Patterns (Richardson) chương 1–2 | Khi nào microservices; decompose theo business capability và subdomain |
| Thứ Tư | Learning Domain-Driven Design (Khononov) phần I (strategic design) | Subdomain, bounded context, context map |
| Thứ Sáu | Fundamentals of Software Architecture (Richards, Ford) chương về modular monolith và microservices | Đặc tính kiến trúc, đánh đổi |
| Chủ nhật | [02 §3.3–3.4](02-ve-he-thong-100k-1m-10m.md#33-l2--1m-users-tách-đọc-tách-việc-chậm) | Vì sao L3 mới tách, và tách theo gì |

### Ghi chú khái niệm

#### 11.1 Khi nào **chưa** nên tách

Microservices đổi **độ phức tạp trong code** lấy **độ phức tạp phân tán**: lời gọi mạng thay lời gọi
hàm, saga thay `@Transactional`, trace thay stack trace, nhiều pipeline deploy. Đáng đổi khi có ít nhất
một lý do cụ thể:

| Lý do tách | Ví dụ ngân hàng | Không phải lý do |
|---|---|---|
| Team cần deploy độc lập | 6–10 team giẫm nhau trong một codebase | "Code to quá" |
| SLO hoặc kiểu tải khác hẳn | Ledger p99 200 ms, 99,99%; báo cáo chạy batch nặng | "Để scale" khi cả hệ thống mới 200 RPS |
| Miền lỗi phải cách ly | Notification phụ thuộc bên thứ ba hay chết | "Microservices là best practice" |
| Ranh giới tuân thủ, dữ liệu | Dữ liệu thẻ (PCI DSS) tách khỏi phần còn lại | |

**Modular monolith** là mặc định: một artifact, nhiều module có ranh giới rõ, mỗi module sở hữu bảng
của mình, gọi nhau qua interface công khai. Tách ra service sau này chỉ là đổi lời gọi hàm thành lời
gọi mạng ở chỗ đã có ranh giới.

#### 11.2 DDD chiến lược trong 10 dòng

- **Subdomain**: *core* (thứ tạo khác biệt: sổ cái, chuyển tiền), *supporting* (onboarding, báo cáo),
  *generic* (auth, gửi email: mua hoặc dùng sẵn).
- **Bounded context**: ranh giới trong đó một từ có **một** nghĩa. "Account" ở Ledger là tài khoản kế
  toán; ở Onboarding là hồ sơ khách hàng. Hai mô hình, hai context.
- **Context map**: quan hệ giữa các context. *Customer/Supplier* (bên dưới phục vụ nhu cầu bên trên),
  *Conformist* (bên dưới chấp nhận mô hình của bên trên), *Anti-Corruption Layer* (dịch mô hình bên
  ngoài, ví dụ API ngân hàng đối tác, sang mô hình của mình), *Open Host Service / Published Language*
  (API và schema sự kiện công khai, có version).
- **Database per service**: không service nào đọc bảng của service khác. Cần dữ liệu thì gọi API hoặc
  giữ **read model** được nạp từ sự kiện.

#### 11.3 Bốn kiểu coupling — thứ cần tìm ở hệ thống thật

| Kiểu | Dấu hiệu | Hậu quả |
|---|---|---|
| **Thời gian** (temporal) | Chuỗi gọi đồng bộ A → B → C → D | Availability nhân lên: 4 × 99,9% = 99,6%; latency cộng dồn |
| **Dữ liệu** | Hai service cùng đọc/ghi một bảng | Đổi schema phải deploy cùng lúc; không ai sở hữu dữ liệu |
| **Triển khai** | "Service A và B phải release cùng nhau" | Thực chất là một service bị cắt đôi |
| **Ngữ nghĩa** | Đổi ý nghĩa một trường làm hỏng bên tiêu thụ | Sự kiện không có schema, không có version |

#### 11.4 Đồng bộ hay bất đồng bộ giữa services

| Dùng **đồng bộ** (REST/gRPC) khi | Dùng **bất đồng bộ** (sự kiện, lệnh qua Kafka) khi |
|---|---|
| Người gọi cần kết quả **ngay** để trả lời user (kiểm tra số dư khả dụng) | Người gọi không cần chờ (gửi thông báo, cập nhật read model) |
| Thao tác đọc | Một sự kiện có nhiều bên quan tâm |
| Chuỗi gọi ngắn (≤ 2 tầng) | Bên kia có thể chết hoặc chậm mà mình vẫn phải chạy |

**CQRS** tách mô hình ghi và mô hình đọc; đáng làm khi đọc và ghi khác nhau nhiều về hình dạng hoặc
tải (lịch sử giao dịch có tìm kiếm). **Event sourcing** lưu chuỗi sự kiện thay cho trạng thái; sổ cái
append-only vốn đã gần với nó. Áp event sourcing cho **mọi** thứ thường là over-engineer.

### Lab 11 — Ranh giới module (Thứ Bảy, 3 giờ)

1. **Event storming một mình** (60 phút, Excalidraw): viết mọi *domain event* của capstone theo thời
   gian (`AccountOpened`, `TransferRequested`, `FundsHeld`, `TransferCompleted`, …), rồi command và
   actor gây ra chúng, rồi gom thành cụm. Mỗi cụm là một ứng viên bounded context.
2. **Context map**: vẽ quan hệ giữa các context, ghi kiểu quan hệ (11.2) trên mỗi mũi tên.
3. **Ép ranh giới trong code**: mỗi context một package gốc (`…account`, `…transfer`, `…ledger`,
   `…notification`), bên trong có `api` (công khai) và `internal`. Viết test chặn import chéo vào
   `internal`, bằng ArchUnit hoặc Spring Modulith:

   ```java
   // ArchUnit
   @ArchTest
   static final ArchRule noReachingIntoOtherModulesInternals =
       noClasses().that().resideInAPackage("..transfer..")
           .should().dependOnClassesThat().resideInAnyPackage("..ledger.internal..", "..account.internal..");

   // hoặc Spring Modulith: mỗi package con trực tiếp của package ứng dụng là một module
   @Test void verifyModules() { ApplicationModules.of(CapstoneApplication.class).verify(); }
   ```

4. **Mỗi module sở hữu bảng của mình**: tiền tố bảng theo module, và module khác không viết SQL vào
   bảng đó. Ghi các vi phạm hiện có vào sổ lỗi.

**Tiêu chí đạt:** test ranh giới xanh; context map có ít nhất một ACL (cổng ngân hàng đối tác) và một
Open Host Service (sự kiện `transfer.completed` có schema, có version).

### Track S tuần 11 — V8: tách V6 thành services (Chủ nhật, 40 phút)

Vẽ C4 container cho Mini Core Transfer khi đã tách. Mỗi service phải có một dòng **"tách vì …"** lấy
từ bảng 11.1. Service nào không viết được dòng đó thì gộp lại.

<details markdown="1">
<summary><b>Đáp án V8 (rút gọn)</b></summary>

| Service | Tách vì | Dữ liệu sở hữu | Giao tiếp |
|---|---|---|---|
| **Ledger** | Core subdomain; SLO cao nhất; bất biến tiền phải nằm ở một chỗ | `ledger_entry`, số dư | Nhận lệnh qua Kafka (saga), phát `ledger.posted` |
| **Transfer** | Orchestrator saga, đổi nhiều hơn mọi phần khác (luật chuyển tiền) | `transfer`, `saga_instance`, outbox | REST từ gateway; lệnh và sự kiện qua Kafka |
| **Notification** | Phụ thuộc bên thứ ba hay chết; tải burst theo chiến dịch | `notification_log` | Chỉ nghe sự kiện |
| **Interbank gateway** | ACL với ngân hàng đối tác; miền lỗi riêng; chứng chỉ mTLS riêng | trạng thái lệnh gửi đối tác | Nhận lệnh, trả `reply / unknown` |
| Account + Onboarding | **Chưa tách** khỏi nhau: một team, cùng nhịp đổi | `customer`, `account` | REST |
| Reporting | **Chưa là service**: read model nạp qua CDC, chạy như job | read model | Đọc |

Câu đánh đổi: tách Ledger khỏi Transfer biến một transaction thành saga (giai đoạn 2, tuần 7), nên chỉ
làm khi lý do SLO và đội ngũ là thật.

</details>

### Bài tập tuần 11

**11.1 — Có nên tách?** Team 6 người, capstone đang 300 RPS. Module báo cáo chạy query nặng mỗi sáng
làm API chuyển tiền chậm. Một người đề xuất tách báo cáo thành microservice. Bạn trả lời gì?

<details markdown="1">
<summary><b>Đáp án 11.1</b></summary>

Vấn đề là **tranh tài nguyên DB**, không phải ranh giới code. Sửa rẻ hơn theo thứ tự: báo cáo đọc từ
**read replica**; tách **connection pool** riêng cho báo cáo (bulkhead) để nó không ăn hết pool của
chuyển tiền; chạy báo cáo như một **process riêng từ cùng codebase** (cùng artifact, profile khác). Tách
thành service riêng chỉ khi có team riêng hoặc vòng đời dữ liệu riêng. Đánh đổi của cách rẻ: báo cáo
đọc dữ liệu trễ vài giây.

</details>

**11.2 — Chuỗi gọi đồng bộ.** Request đi gateway → Transfer → Account → Limit → Ledger, mỗi service
availability 99,9%, p99 100 ms, gọi nối tiếp. Availability và p99 tối thiểu của cả chuỗi? Ba cách cải
thiện?

<details markdown="1">
<summary><b>Đáp án 11.2</b></summary>

Availability ≈ 0,999⁵ ≈ **99,5%** (từ 43 phút xuống ~3,6 giờ downtime/tháng). p99 tối thiểu **≥ 400
ms** cho 4 chặng sau gateway, thực tế còn cao hơn vì p99 của chuỗi xấu hơn tổng p99 từng chặng khi các
chặng độc lập. Cải thiện: gọi **song song** những chặng không phụ thuộc nhau (Account và Limit); **cache**
hoặc read model cho dữ liệu ít đổi (hạn mức); chuyển phần không cần chờ sang **bất đồng bộ** (ghi sổ
qua saga, trả `202` cho client).

</details>

**11.3 — Chung database.** Hai service Transfer và Reporting cùng đọc bảng `transfer`. Team Transfer
muốn đổi `amount` (số thực) thành `amount_minor` (số nguyên). Chuyện gì xảy ra, và lộ trình gỡ coupling?

<details markdown="1">
<summary><b>Đáp án 11.3</b></summary>

Đổi cột làm hỏng Reporting mà Transfer không biết; hai team phải phối hợp deploy. Gỡ theo từng bước
(strangler): (1) cho Reporting đọc qua một **view** do Transfer sở hữu, giữ tên cột cũ trong view; (2)
Transfer phát sự kiện `transfer.completed` có schema; (3) Reporting nạp **read model riêng** từ sự kiện
(hoặc CDC); (4) bỏ quyền đọc bảng gốc của Reporting. Sau bước 4 Transfer đổi schema tự do.

</details>

**11.4 — Đề bấm giờ: tách monolith ngân hàng** (45 phút). Monolith 8 năm tuổi, 1 database 600 bảng, 12
team. Lãnh đạo muốn microservices trong 1 năm. Trình bày cách tiếp cận.

<details markdown="1">
<summary><b>Khung lời giải</b></summary>

1. **Không viết lại.** Strangler fig: đặt gateway phía trước, cắt dần từng luồng sang service mới.
2. **Chọn thứ tự theo giá trị và rủi ro:** bắt đầu từ phần ít ràng buộc dữ liệu, đổi thường xuyên,
   có team sở hữu rõ (notification, tra cứu sản phẩm); để sổ cái **sau cùng** hoặc giữ trong monolith
   lâu nhất.
3. **Tách dữ liệu trước khi tách code** cho mỗi phần: xác định bảng thuộc context nào, chặn ghi chéo,
   rồi mới chuyển.
4. **Đồng bộ hai bên trong giai đoạn chuyển:** CDC từ DB cũ sang service mới; dual-write là cấm.
5. **Nền tảng trước:** CI/CD, observability, service template, quy ước sự kiện; thiếu thì 12 team tự
   làm 12 kiểu.
6. Đo tiến độ bằng **số luồng đã chuyển và số bảng đã có chủ**, không phải số service.

</details>

### Bài nói 2 phút

*"When would you NOT split a system into microservices?"* Phải có: microservices đổi độ phức tạp code
lấy độ phức tạp phân tán; bốn lý do đáng tách (bảng 11.1); modular monolith với ranh giới được test ép;
và một ví dụ: tranh tài nguyên DB thì sửa bằng replica và bulkhead, không bằng tách service.

---

## Tuần 12 — Resilience và hiệu năng: load test, tìm nút thắt, sửa, đo lại

### Đầu ra phải có cuối tuần (mốc tuần 12)

- [ ] Chạy lại [ops-lab](ops-lab/) và tái hiện được bảng P09 trên máy mình.
- [ ] Load test **capstone** bằng Gatling, tìm "đầu gối" (tải mà p99 bắt đầu tăng vọt), tìm và sửa
      **ít nhất một** anti-pattern, có bảng số trước/sau.
- [ ] Client gọi đối tác trong capstone có timeout + retry (backoff + jitter) + circuit breaker +
      bulkhead, có test chaos (đối tác chậm 2 s, đối tác chết).
- [ ] Đề bấm giờ: **Flash sale**.
- [ ] Bài nói 2 phút: *"How would you load test a payment API before a big campaign?"*

### Đọc

| Buổi | Tài liệu | Phần |
|---|---|---|
| Thứ Hai | Release It! (Nygard), phần *Stability Patterns* và *Stability Antipatterns* | Timeout, circuit breaker, bulkhead, chain reactions |
| Thứ Tư | Amazon Builders' Library: *Timeouts, retries, and backoff with jitter*; *Using load shedding to avoid overload* | |
| Thứ Sáu | [01 nhóm 2](01-java-code-cham-duoi-tai-cao.md#3-nhóm-2--giữ-tài-nguyên-khan-hiếm-quá-lâu-littles-law) (P08–P14) và [ops-lab](ops-lab/README.md) | Kèm chạy lại ops-lab |
| Tùy chọn | Gil Tene, bài nói *How NOT to Measure Latency* | Coordinated omission |

### Ghi chú khái niệm

#### 12.1 Năm lớp phòng thủ khi gọi một phụ thuộc

| Lớp | Bảo vệ ai | Cấu hình bắt đầu |
|---|---|---|
| **Timeout** | Thread/connection của mình | Connect 300 ms, read ≈ 2–3 × p99 của phụ thuộc, nhỏ hơn ngân sách còn lại của request |
| **Retry** | Một request riêng lẻ | Chỉ thao tác idempotent; tối đa 2–3 lần; backoff mũ + **jitter**; **chỉ retry ở một tầng** |
| **Circuit breaker** | **Phụ thuộc** đang yếu (và thread của mình) | Cửa sổ 50–100 lời gọi, ngưỡng lỗi 50%, mở 5–30 s, half-open vài lời gọi; **luôn kèm fallback** |
| **Bulkhead** | Các phần khác của chính service | Semaphore/pool riêng cho từng phụ thuộc, kích thước theo Little's Law |
| **Load shedding** | Cả service khi quá tải | Hàng đợi có giới hạn, trả 429/503 + `Retry-After`, ưu tiên luồng tiền hơn luồng phụ |

**Số đo từ [ops-lab RetryStorm](ops-lab/README.md#kết-quả-07102026-output-đầy-đủ-results2026-10-07txt)**
(downstream sự cố 2 giây): retry ngay lập tức đưa **7.934** lần gọi vào downstream trong lúc sự cố, so
với 2.000 khi không retry, mà không cứu thêm request nào. Backoff + jitter cứu được nhiều request nhất
(83,7% thành công) nhưng p99 lên ~945 ms. Thêm circuit breaker thì chỉ còn **75** lần gọi vào downstream
đang chết, nhưng 500 ms đầu sau hồi phục chỉ 23% thành công vì breaker còn đang mở. **Retry giúp client,
hại downstream; breaker giúp downstream, hại client trong lúc mở.** Cần cả hai, cùng fallback.

#### 12.2 Retry nhân theo tầng

Gateway retry 2 lần, service A retry 3 lần, SDK gọi B retry 2 lần: một request của user thành tối đa
`3 × 4 × 3 = 36` lời gọi vào B. Quy tắc: **retry ở một tầng** (thường là tầng gần chỗ lỗi nhất), các tầng
khác không retry; có **retry budget** (ví dụ retry không vượt 10% lưu lượng); và **truyền deadline**
xuống dưới để tầng dưới không làm việc cho một request mà tầng trên đã bỏ.

#### 12.3 Virtual threads: sửa gì, không sửa gì

Virtual threads bỏ giới hạn số **thread**. Chúng không bỏ giới hạn **connection pool**, **đối tác**, hay
**khoá**. Đo trong ops-lab với code P09: platform threads ở 250 rps cho p99 11 s, **0 lỗi** (Tomcat 200
thread vô tình làm bộ giới hạn); bật virtual threads thì hơn 4.000 request vào xếp hàng ở Hikari,
p50 22 s, **19% lỗi** vì chờ quá `connectionTimeout`. Bật virtual threads mà không có bulkhead/giới hạn
đồng thời trước tài nguyên khan hiếm là bỏ mất bộ giới hạn duy nhất đang có.

#### 12.4 GC và p99

Heap sống to và tốc độ cấp phát cao làm GC dày và dài, rơi vào p99 (nhóm 4 của track P). Trên JDK 21:
G1 là mặc định, hợp đa số service; **ZGC thế hệ** (`-XX:+UseZGC -XX:+ZGenerational`) giữ pause dưới
mili giây với heap lớn, đổi lại tốn thêm CPU và bộ nhớ. Đừng chỉnh GC trước khi đo: bật
`-Xlog:gc*:file=gc.log` và nhìn pause thực tế trong giờ cao điểm.

#### 12.5 Load test đúng cách

| Sai | Đúng |
|---|---|
| 100 user lặp lại liên tục (mô hình **đóng**) | Request đến theo **tốc độ** cố định (mô hình **mở**: `constantUsersPerSec`), không chờ request trước |
| Báo cáo latency **trung bình** | p50, p95, **p99**, max, và số lỗi |
| Một mức tải duy nhất | **Bậc thang** tải để tìm đầu gối: 50% → 75% → 100% → 125% trần dự kiến |
| Đo ngay khi khởi động | Warm-up (JIT, pool, cache), rồi mới đo ở trạng thái ổn định |
| Chỉ nhìn kết quả của tool tải | Nhìn **cùng lúc** metric server: Hikari pending, thread busy, GC, CPU, lag (phương pháp USE: utilization, saturation, errors) |
| Đổi nhiều thứ một lúc | Mỗi lần đổi một thứ, đo lại |

Mô hình đóng che mất đuôi dài: server chậm thì user ảo cũng gửi chậm lại, tool "tự nhường", và p99 trông
đẹp. Đó là *coordinated omission*.

#### 12.6 Profiling trên production-like

```bash
# JFR: ghi 60 s, overhead thấp, dùng được trên production
jcmd <pid> JFR.start duration=60s filename=/tmp/rec.jfr settings=profile
# mở bằng JDK Mission Control: Threads (ai đang chờ ở đâu), Lock Instances, Allocation, GC

# async-profiler: flame graph
asprof -d 30 -e cpu   -f cpu.html   <pid>     # CPU: nhóm 1 của track P
asprof -d 30 -e alloc -f alloc.html <pid>     # cấp phát: P01, P03, P19
asprof -d 30 -e lock  -f lock.html  <pid>     # tranh khoá: P11
asprof -d 30 -e wall  -f wall.html  <pid>     # thời gian thực kể cả chờ I/O: P09, P10, P15

jcmd <pid> Thread.print > threads.txt         # thread dump: hàng loạt thread ở HikariPool.getConnection = P09
```

### Lab 12 — Load test, tìm nút thắt, sửa, đo lại (Thứ Bảy, 4 giờ)

**Bước 1 — Tái hiện ops-lab** (45 phút):

```bash
cd java-system-design/ops-lab
mvn -q package -DskipTests
java -jar target/ops-lab.jar                                                    # terminal 1
mvn -q test-compile gatling:test -Dpath=/transfers/bad -Drps=150 -Dseconds=30   # terminal 2
mvn -q gatling:test -Dpath=/transfers/bad -Drps=250 -Dseconds=30
mvn -q gatling:test -Dpath=/transfers/good -Drps=250 -Dseconds=30
```

**Trước khi chạy**, tính trần của `/transfers/bad` bằng Little's Law và ghi vào sổ. Số tham khảo đã chạy:

| Lượt | p99 | Lỗi | Thông lượng thực | Hikari chờ max |
|---|---:|---:|---:|---:|
| bad @ 150 rps | 105 ms | 0 | 145 req/s | 0 (active 9/10) |
| bad @ 250 rps | **11,1 s** | 0 | **183 req/s** (lý thuyết 188) | **189** |
| good @ 250 rps | 103 ms | 0 | 242 req/s | 0 |
| bad @ 250 rps, `VT=true` | **30,1 s** | **18,6–19,9%** | — | **4.260–4.483** |

**Bước 2 — Tìm đầu gối của capstone** (60 phút): viết simulation Gatling cho 2–3 endpoint chính của
capstone, chạy bậc thang 50 → 100 → 200 → 400 rps (hoặc `incrementUsersPerSec`). Vẽ p99 theo tải. Đầu
gối là chỗ p99 đổi độ dốc.

**Bước 3 — Chẩn đoán** (45 phút): ở mức tải ngay trên đầu gối, chụp cùng lúc: `hikaricp.connections.pending`,
`tomcat.threads.busy` (hoặc số virtual thread), CPU, `jvm.gc.pause`, thread dump, một bản JFR 60 s, một
flame graph `wall`. Ghi một câu: *"Nút thắt là ___, bằng chứng là ___"*.

**Bước 4 — Sửa một thứ, đo lại** (45 phút): đúng một thay đổi, chạy lại bậc thang. Bảng trước/sau vào
`capstone/load-test/README.md`. Đây là **câu chuyện STAR về hiệu năng** cho giai đoạn 4.

**Bước 5 — Resilience4j cho client gọi đối tác** (45 phút): `resilience4j-spring-boot3`, cấu hình timeout,
retry (backoff + jitter, chỉ cho lỗi tạm thời), circuit breaker có fallback, bulkhead. Test chaos:

| Test | Assert |
|---|---|
| Đối tác chậm 2 s | Request kết thúc dưới timeout + fallback; endpoint **không** gọi đối tác vẫn giữ p99 cũ (P10) |
| Đối tác trả 500 liên tục | Breaker mở sau N lời gọi; lời gọi sau đó trả fallback ngay, không chạm đối tác |
| Đối tác sống lại | Breaker half-open rồi đóng; số lời gọi vào đối tác trở về bình thường |
| 100 request đồng thời vào đối tác | Bulkhead giới hạn ở kích thước đã tính; phần vượt fail nhanh |

**Bước 6 — Chạy demo retry storm** (15 phút): `java -jar target/ops-lab.jar retry-storm`. Giải thích bằng
lời vì sao dòng circuit breaker có tỉ lệ thành công **thấp hơn** không retry.

### Track P tuần 12 — tuần chính của track

Ôn P11–P14 ([01 nhóm 2](01-java-code-cham-duoi-tai-cao.md#3-nhóm-2--giữ-tài-nguyên-khan-hiếm-quá-lâu-littles-law)),
rồi dùng công cụ thật để **thấy** chúng:

1. Chạy `D12Pinning` với `-Djdk.tracePinnedThreads=full` trên JDK 21: log chỉ thẳng dòng `synchronized`.
2. Chạy `ContentionBench` (P11) kèm async-profiler `-e lock` (JMH có sẵn `-prof async`): khoá của `SyncCounter` đứng đầu.
3. Chạy `D09PoolExhaustion` và lấy thread dump giữa chừng: đếm số thread ở `Semaphore.acquire`.
4. Trên capstone: flame graph `wall` ở tải đầu gối. Gạch tên từng anti-pattern tìm thấy theo mã P.

### Bài tập tuần 12

**12.1 — Kích thước bulkhead.** Đối tác fraud p99 400 ms, đỉnh 50 lời gọi/giây. Kích thước semaphore?
Vượt thì làm gì?

<details markdown="1">
<summary><b>Đáp án 12.1</b></summary>

Little's Law theo p99: `50 × 0,4 = 20` lời gọi đồng thời; chừa dư ~25%: **25**. Vượt thì fail nhanh và đi
đường fallback, không xếp hàng: xếp hàng ở đây chính là P13. Khi đối tác chậm lên 2 s, `50 × 2 = 100` lời
gọi muốn chạy cùng lúc, bulkhead chặn ở 25 và giữ phần còn lại của service sống.

</details>

**12.2 — Fallback cho fraud check.** Breaker của dịch vụ fraud đang mở. Chuyển tiền nên làm gì? Trả lời
riêng cho lệnh 200.000đ và lệnh 200 triệu.

<details markdown="1">
<summary><b>Đáp án 12.2</b></summary>

Không có một câu trả lời chung: đó là **quyết định nghiệp vụ** phải ghi vào ADR. Cách hay gặp: lệnh nhỏ
dưới ngưỡng **fail-open có giới hạn** (cho qua, gắn cờ kiểm tra lại sau, giới hạn tổng số tiền cho qua
trong lúc breaker mở); lệnh lớn **fail-closed** thành trạng thái `PENDING_REVIEW` và báo khách "đang xử
lý". Không bao giờ fail-open không giới hạn: kẻ gian sẽ học được cách làm dịch vụ fraud chậm đi.

</details>

**12.3 — Đọc kết quả ops-lab.** Đồng nghiệp chạy load test `/transfers/bad` ở 150 rps, thấy p99 105 ms và
kết luận "đạt yêu cầu, lên production". Bạn nói gì?

<details markdown="1">
<summary><b>Đáp án 12.3</b></summary>

150 rps nằm dưới trần ~188 rps nên mọi thứ trông bình thường; active connection đã 9/10, tức hệ thống
đang ở **mép vực**. Chỉ cần tải tăng 25% (đỉnh ngày lương thường gấp 3) là p99 lên 11 giây. Load test phải
đẩy **vượt** tải dự kiến để thấy đầu gối, và phải đọc **metric bão hoà** (active/pending), không chỉ
latency. Yêu cầu: chạy bậc thang tới 2–3 lần tải dự kiến trước khi kết luận.

</details>

**12.4 — Đề bấm giờ: Flash sale** (45 phút). 10.000 sản phẩm giá sốc, 2 triệu người chờ lúc 12:00.

<details markdown="1">
<summary><b>Khung lời giải</b></summary>

| Lớp | Quyết định | Đánh đổi là… |
|---|---|---|
| Trước giờ mở | Trang sản phẩm tĩnh qua CDN, đếm ngược ở client | Giá/tồn kho trên trang có thể cũ |
| Cổng vào | **Waiting room**: hàng đợi ảo phát token theo tốc độ hệ thống chịu được | Người dùng chờ, cần UI tốt |
| Chống bot | Rate limit theo user + thiết bị, CAPTCHA khi nghi ngờ | Gây khó cho người thật |
| Giữ hàng | Tồn kho trong **Redis**, trừ bằng Lua nguyên tử (giống token bucket ở giai đoạn 1) | Redis là nguồn sự thật tạm thời: cần đối soát với DB |
| Tạo đơn | Trừ Redis thành công → đẩy lệnh tạo đơn vào Kafka → ghi DB bất đồng bộ, **idempotent** theo `orderId` | Đơn hiện ra trễ vài giây |
| Thanh toán | Giữ hàng 10 phút; hết hạn thì trả lại tồn kho | Hàng bị giữ ảo |
| Quá tải | Load shedding ở gateway: ưu tiên luồng thanh toán của người đã có hàng | Người đến sau bị từ chối nhanh |

Con số: 2 triệu người trong 1 phút ≈ 33.000 req/s chỉ để vào cửa, nhưng chỉ **10.000** người mua được.
Bài này là **tranh tài nguyên hữu hạn** + **hấp thụ burst**, không phải bài throughput: đừng scale DB lên
33.000 ghi/giây.

</details>

### Bài nói 2 phút

*"How would you load test a payment API before a big campaign?"* Phải có: ước lượng tải đỉnh (track S),
mô hình mở, bậc thang vượt đỉnh 2–3 lần, đo p99 và metric bão hoà cùng lúc, môi trường giống production,
test chaos phụ thuộc, và một con số thật từ lab của bạn.

---

## Tuần 13 — Observability, SLO, postmortem

### Đầu ra phải có cuối tuần

- [ ] Capstone có trace OpenTelemetry xuyên HTTP và Kafka; thấy được span gọi đối tác nằm trong hay
      ngoài transaction.
- [ ] SLO cho 2 luồng chính, rule cảnh báo theo **burn rate**, dashboard golden signals.
- [ ] Dashboard "phát hiện anti-pattern" (Track P).
- [ ] Một postmortem viết theo mẫu cho sự cố bạn tạo ra ở lab 12.
- [ ] Bản vẽ **V9**; đề bấm giờ **Metrics monitoring and alerting**.
- [ ] Bài nói 2 phút: *"How do you define an SLO for a transfer API and alert on it?"*

### Đọc

| Buổi | Tài liệu | Phần |
|---|---|---|
| Thứ Hai | Google SRE Book, chương *Service Level Objectives*, *Monitoring Distributed Systems* | SLI/SLO, golden signals |
| Thứ Tư | Google SRE Workbook, chương *Alerting on SLOs* | Burn rate, cảnh báo nhiều cửa sổ |
| Thứ Sáu | Tài liệu OpenTelemetry Java (agent, context propagation) | |
| Chủ nhật | Alex Xu Vol 2 chương 5 *Metrics monitoring and alerting system*; SRE Book chương *Postmortem Culture* | |

### Ghi chú khái niệm

#### 13.1 Ba trụ, một sợi chỉ

**Metric** trả lời "có vấn đề không, bao nhiêu" (rẻ, tổng hợp, cardinality thấp). **Trace** trả lời "chậm ở
đâu trong chuỗi gọi". **Log** trả lời "chính xác chuyện gì xảy ra với request này". Sợi chỉ nối ba thứ là
**trace id**: có trong log (MDC), có trong exemplar của metric, và đi theo header `traceparent` qua HTTP và
Kafka.

#### 13.2 SLI, SLO, error budget

```text
SLI    tỉ lệ request /transfers trả 2xx trong ≤ 500 ms
SLO    99,9% trong cửa sổ 30 ngày
Budget 0,1% request được phép "xấu" = 50.000 request nếu tháng có 50 triệu request
```

Cảnh báo theo **burn rate** (tốc độ đốt budget so với tốc độ "đều"), hai cửa sổ để vừa nhanh vừa ít báo
nhầm (theo SRE Workbook):

| Mức | Burn rate | Cửa sổ dài / ngắn | Ý nghĩa |
|---|---:|---|---|
| Page | 14,4 | 1 giờ / 5 phút | Đốt 2% budget trong 1 giờ: hết budget trong ~2 ngày |
| Page | 6 | 6 giờ / 30 phút | Đốt 5% budget trong 6 giờ |
| Ticket | 1 | 3 ngày / 6 giờ | Đốt đúng tốc độ hết budget cuối tháng |

**Cảnh báo theo triệu chứng** (user bị ảnh hưởng), không theo nguyên nhân (CPU 80% lúc 3 giờ sáng không
ai bị ảnh hưởng thì không đánh thức ai).

#### 13.3 Cardinality

Mỗi tổ hợp giá trị label là một time series. Gắn `userId` hay `accountId` vào metric thì số series bằng số
user: hệ thống metric sập. Giá trị cardinality cao đi vào **log và trace**, không vào metric.

### Lab 13 — Quan sát được capstone (Thứ Bảy, 4 giờ)

1. **Trace:** chạy capstone với OpenTelemetry Java agent:

   ```bash
   java -javaagent:opentelemetry-javaagent.jar \
        -Dotel.service.name=transfer \
        -Dotel.exporter.otlp.endpoint=http://localhost:4318 \
        -jar capstone.jar
   ```

   Agent tự instrument Spring MVC, JDBC, HTTP client, Kafka. Gửi về Jaeger hoặc Grafana Tempo. Mở trace
   của `/transfers/bad` trong ops-lab: span HTTP tới đối tác nằm **giữa** các span JDBC của cùng một
   transaction. Đó là cách P09 hiện ra trên production.
2. **Metric:** `micrometer-registry-prometheus`, mở `/actuator/prometheus`. Bật histogram cho SLO:

   ```properties
   management.metrics.distribution.percentiles-histogram.http.server.requests=true
   management.metrics.distribution.slo.http.server.requests=100ms,500ms,1s
   ```

3. **Rule cảnh báo** (mẫu, sửa tên metric theo capstone):

   ```yaml
   groups:
   - name: transfer-slo
     rules:
     - record: sli:transfer_bad_ratio:rate5m
       expr: |
         1 - (
           sum(rate(http_server_requests_seconds_bucket{uri="/transfers",outcome="SUCCESS",le="0.5"}[5m]))
           /
           sum(rate(http_server_requests_seconds_count{uri="/transfers"}[5m]))
         )
     # tương tự cho [1h], [30m], [6h] ...
     - alert: TransferSloFastBurn
       expr: sli:transfer_bad_ratio:rate1h > (14.4 * 0.001) and sli:transfer_bad_ratio:rate5m > (14.4 * 0.001)
       labels: {severity: page}
       annotations: {summary: "Transfer SLO đang đốt budget nhanh gấp 14,4 lần"}
   ```

4. **Dashboard golden signals** (latency, traffic, errors, saturation) cho mỗi service, và **dashboard
   phát hiện anti-pattern** (Track P bên dưới).
5. **Postmortem** cho "sự cố" lab 12 (bad @ 250 rps) theo mẫu: tóm tắt · ảnh hưởng (số liệu) · dòng thời
   gian · nguyên nhân và yếu tố góp phần · điều làm tốt / chưa tốt · hành động có người phụ trách và hạn.
   Không đổ lỗi cho người; tìm lỗ hổng trong hệ thống và quy trình.

### Track P tuần 13 — dashboard phát hiện anti-pattern

Một panel cho mỗi dòng của [01 §7](01-java-code-cham-duoi-tai-cao.md#7-bắt-chúng-trên-production--metric-nào-công-cụ-nào):

| Panel | Metric | Báo hiệu |
|---|---|---|
| Pool DB | `hikaricp_connections_pending`, `hikaricp_connections_usage_seconds` (p99) | P09, P15, P16 |
| Thread server | `tomcat_threads_busy_threads` / max | P10 |
| Hàng đợi executor | `executor_queued_tasks` | P13 |
| Gọi ra ngoài | `http_client_requests_seconds` p99 theo `uri`/client | P08, P10 |
| GC | `jvm_gc_pause_seconds` max, `jvm_gc_memory_allocated_bytes_total` rate | P01–P07, P18, P19 |
| Bộ nhớ | `jvm_memory_used_bytes{area="heap"}` (đáy răng cưa) | P18 |
| Kafka | lag tối đa, số lần rebalance | P20 |

Bài tập: chạy lại ops-lab bad @ 250 rps, chụp dashboard. Panel nào đỏ trước?

### Track S tuần 13–14 — V9 (Chủ nhật tuần 14, 30 phút)

Phủ lên V8: trace đi qua đâu (gateway → Transfer → Kafka → Ledger), collector ở đâu, metric/log đi về
đâu; token được kiểm ở đâu (gateway và **từng service**), mTLS giữa service nào, secret lấy từ đâu, audit
log ghi ở đâu và ai đọc được.

### Bài tập tuần 13

**13.1 — Tính budget.** SLO 99,9% theo tháng, 50 triệu request/tháng. (a) Budget bao nhiêu request? (b)
Burn rate 14,4 kéo dài thì hết budget sau bao lâu? (c) Một deploy lỗi làm 5% request hỏng trong 20 phút,
lưu lượng đều: tốn bao nhiêu % budget?

<details markdown="1">
<summary><b>Đáp án 13.1</b></summary>

(a) 50.000. (b) 30 ngày ÷ 14,4 ≈ **2,1 ngày**. (c) 20 phút ≈ 1/2.160 tháng → ~23.150 request trong 20 phút
× 5% ≈ **1.160 request xấu ≈ 2,3% budget**. Con số này giúp quyết định: còn budget thì tiếp tục ra tính
năng; budget gần cạn thì ưu tiên độ tin cậy.

</details>

**13.2 — Đọc trace.** Một trace `/accounts/42/summary` có 1 span HTTP server 480 ms, bên trong 51 span JDBC
`select … from transaction_item where transaction_id = ?` mỗi span ~8 ms. Bệnh gì, sửa gì?

<details markdown="1">
<summary><b>Đáp án 13.2</b></summary>

N+1 (P15): 1 query lấy danh sách + 50 query con, ~400 ms chỉ cho round trip. Sửa bằng `IN (…)` hoặc
`@EntityGraph`, rồi thêm test đếm query (giai đoạn 1, lab 3C) để nó không quay lại.

</details>

**13.3 — Đề bấm giờ: Metrics monitoring and alerting** (45 phút).

<details markdown="1">
<summary><b>Khung lời giải</b></summary>

| Thành phần | Chọn | Đánh đổi là… |
|---|---|---|
| Thu | **Pull** (Prometheus scrape) cho service lâu dài; **push** qua gateway cho job ngắn | Pull cần service discovery |
| Lưu | Time-series DB, nén theo khối thời gian; giữ thô 15 ngày, **downsample** 5 phút cho 1 năm | Mất chi tiết khi xem dữ liệu cũ |
| Mở rộng | Shard theo series (hash label), nhiều bản sao; dùng hệ lưu dài hạn phía sau | Truy vấn xuyên shard |
| Truy vấn | Recording rule tính sẵn các biểu thức nặng | Thêm cấu hình phải giữ |
| Cảnh báo | Đánh giá rule định kỳ → alert manager: gộp, khử trùng, im lặng theo lịch, định tuyến | Cảnh báo trễ bằng chu kỳ đánh giá |
| Bẫy | **Cardinality**: giới hạn số series mỗi metric, từ chối label động | |

</details>

### Bài nói 2 phút

*"How do you define an SLO for a transfer API and alert on it?"* Phải có: SLI đo từ phía user (tỉ lệ
request thành công và đủ nhanh), SLO và error budget bằng số, cảnh báo burn rate nhiều cửa sổ, cảnh báo
theo triệu chứng, và dùng budget để quyết định giữa tính năng và độ tin cậy.

---

## Tuần 14 — Security cho ngân hàng

### Đầu ra phải có cuối tuần

- [ ] Keycloak chạy cho capstone: realm, client mobile (PKCE), client service-to-service, roles.
- [ ] Spring Security resource server với 6 test phân quyền (bảng dưới), trong đó có test **BOLA**.
- [ ] Log đã che PII; audit log append-only có chuỗi hash.
- [ ] Đề bấm giờ: **Auth cho mobile banking**.
- [ ] Bài nói 2 phút: *"How do you secure a transfer API in a mobile banking app?"*

### Đọc

| Buổi | Tài liệu | Phần |
|---|---|---|
| Thứ Hai | OAuth 2.0 Security Best Current Practice (IETF, RFC 9700) | PKCE, không dùng implicit/password grant |
| Thứ Tư | OWASP API Security Top 10 (bản 2023) | Đặc biệt API1: Broken Object Level Authorization |
| Thứ Sáu | Spring Security reference: OAuth2 Resource Server (JWT) | |
| Chủ nhật | AWS: KMS envelope encryption, Secrets Manager rotation | |

### Ghi chú khái niệm

#### 14.1 Chọn luồng OAuth

| Ai gọi | Luồng | Ghi chú |
|---|---|---|
| App mobile, web của khách | **Authorization Code + PKCE** | Không implicit, không password grant |
| Service gọi service | **Client credentials** (hoặc danh tính từ mTLS/mesh) | Token ngắn hạn, scope hẹp |
| Ngân hàng đối tác gọi API của mình | **mTLS** + client credentials, có thể ký request | Chứng chỉ riêng cho từng đối tác |

#### 14.2 Kiểm JWT — danh sách tối thiểu

- Chữ ký với **thuật toán mong đợi** (cấu hình cứng, không tin header `alg`; không bao giờ chấp nhận
  `none`; chặn nhầm RS256/HS256).
- `iss` đúng, `aud` có service này, `exp`/`nbf` với độ lệch đồng hồ nhỏ (≤ 60 s).
- Khoá lấy từ JWKS theo `kid`, có cache và làm mới khi gặp `kid` lạ.
- Access token **ngắn** (5–15 phút) + refresh token **xoay vòng**. JWT không thu hồi được trước hạn: thao
  tác rủi ro cao thì kiểm thêm (introspection hoặc danh sách `jti` bị chặn trong Redis tới khi hết hạn).
- **Không để PII trong JWT**: payload chỉ là base64, ai cầm token cũng đọc được.
- **Step-up**: chuyển tiền trên ngưỡng đòi mức xác thực cao hơn (OTP/sinh trắc), kiểm qua claim `acr`
  hoặc thời điểm xác thực gần nhất.

#### 14.3 Phân quyền theo đối tượng (BOLA)

Đã đăng nhập **không** có nghĩa được xem mọi tài khoản. `GET /accounts/{id}/transactions` phải kiểm `id`
thuộc về người đang gọi, và tốt nhất là kiểm **ngay trong query**:
`… where account_id = :id and customer_id = :subjectFromToken`. Đây là lỗi số 1 trong OWASP API Top 10.

#### 14.4 Dữ liệu và bí mật

- **Bí mật**: không trong git, không trong biến môi trường in ra log; dùng Secrets Manager/SSM có xoay
  vòng; quyền theo **IAM role** của task/pod, không dùng access key dài hạn.
- **Mã hoá**: TLS mọi nơi; at rest bằng KMS; trường nhạy cảm (số CCCD, số thẻ) mã hoá ở mức trường hoặc
  **tokenization**; envelope encryption để xoay khoá không phải mã hoá lại toàn bộ.
- **Log**: cho phép theo danh sách trường (allowlist) thay vì cố che theo danh sách cấm; mask số tài khoản
  (`101****56`) như ví dụ P06.
- **Audit log**: append-only, ghi ai/làm gì/lúc nào/từ đâu/kết quả; chống sửa bằng chuỗi hash (mỗi dòng
  chứa hash của dòng trước) hoặc lưu WORM (S3 Object Lock); tách quyền đọc khỏi đội vận hành ứng dụng.
- **Least privilege**: mỗi service một DB role chỉ có quyền trên bảng của mình; không service nào dùng
  superuser.

### Lab 14 — Keycloak + Spring Security (Thứ Bảy, 4 giờ)

1. Keycloak (Docker `quay.io/keycloak/keycloak` chế độ `start-dev`, hoặc bản zip): realm `bank`, client
   `mobile` (public, PKCE), client `ledger-batch` (confidential, client credentials), role `customer`,
   `ops`.
2. Capstone thành resource server:

   ```properties
   spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/bank
   spring.security.oauth2.resourceserver.jwt.audiences=transfer-api
   ```

3. Test bằng `spring-security-test` (không cần Keycloak chạy lúc test):

   | Test | Assert |
   |---|---|
   | Không token | 401 |
   | Token sai `aud` | 401 |
   | Token hết hạn | 401 |
   | Khách A xem giao dịch tài khoản của khách B | **403 hoặc 404**, không lộ dữ liệu (BOLA) |
   | Chuyển 200 triệu, token không có step-up | 403 kèm lý do cần xác thực thêm |
   | `ops` gọi endpoint của khách | 403 (vai trò vận hành không thay khách giao dịch) |

   ```java
   mvc.perform(get("/accounts/{id}/transactions", accountOfB)
           .with(jwt().jwt(j -> j.subject("customer-A").claim("aud", "transfer-api"))))
      .andExpect(status().isForbidden());
   ```

4. **Che PII trong log**: test ghi log một `Transfer` và assert log không chứa số tài khoản đầy đủ.
5. **Audit log**: bảng `audit_event(id, at, actor, action, target, result, prev_hash, hash)`, `hash =
   sha256(prev_hash || nội dung)`. Test: sửa tay một dòng giữa chuỗi → job kiểm tra chuỗi báo lỗi đúng dòng đó.

### Bài tập tuần 14

**14.1 — Tìm lỗi.** Cấu hình kiểm JWT viết tay sau có ít nhất 4 lỗi:

```java
var claims = Jwts.parser().setSigningKeyResolver(resolver).parseClaimsJws(token).getBody();
// resolver chọn khoá theo header "alg" của token
// không kiểm "aud"; access token sống 24 giờ; token chứa "nationalId" và "phone"
```

<details markdown="1">
<summary><b>Đáp án 14.1</b></summary>

(1) Chọn khoá/thuật toán theo `alg` trong token → mở đường nhầm thuật toán; phải cố định thuật toán
mong đợi. (2) Không kiểm `aud` → token cấp cho service khác dùng được ở đây. (3) Sống 24 giờ → token bị
lộ dùng được cả ngày; nên 5–15 phút + refresh xoay vòng. (4) PII trong payload (đọc được, nằm trong log
proxy, cache). Thêm: không thấy kiểm `iss`; nên dùng resource server của Spring thay vì tự viết.

</details>

**14.2 — Thu hồi.** Điện thoại của khách bị mất. Làm sao chặn ngay mọi giao dịch từ thiết bị đó, khi
access token là JWT còn hạn 10 phút?

<details markdown="1">
<summary><b>Đáp án 14.2</b></summary>

Thu hồi refresh token của thiết bị (Keycloak: đăng xuất session); access token còn tối đa 10 phút thì:
với thao tác **rủi ro cao** (chuyển tiền, đổi hạn mức) kiểm thêm trạng thái session/thiết bị (introspection
hoặc danh sách `sid`/`jti` bị chặn trong Redis, TTL bằng thời hạn token); thao tác đọc chấp nhận tối đa 10
phút. Đánh đổi: thêm một lần tra cứu cho luồng rủi ro cao.

</details>

**14.3 — Đề bấm giờ: Auth cho mobile banking** (45 phút). Đăng nhập, đăng ký thiết bị, sinh trắc, step-up
khi chuyển tiền, phiên trên nhiều thiết bị, chống dò OTP.

<details markdown="1">
<summary><b>Khung lời giải</b></summary>

- **Đăng ký thiết bị**: lần đầu đăng nhập bằng mật khẩu + OTP SMS; app sinh cặp khoá trong secure enclave
  /keystore, đăng ký khoá công khai với server. Từ đó sinh trắc học mở khoá **khoá riêng trên máy** để
  ký challenge; server không bao giờ nhận dữ liệu sinh trắc.
- **Đăng nhập**: OIDC Authorization Code + PKCE; access token 5–10 phút, refresh token gắn với thiết bị,
  xoay vòng mỗi lần dùng.
- **Step-up**: chuyển tiền trên ngưỡng hoặc tới người nhận mới → ký challenge chứa nội dung giao dịch
  (số tiền, người nhận) bằng khoá thiết bị, để chữ ký không dùng lại được cho giao dịch khác.
- **Chống dò OTP**: giới hạn số lần theo tài khoản và thiết bị, khoá tạm có backoff, OTP hết hạn nhanh,
  so sánh thời gian hằng số.
- **Quan sát**: audit mọi lần đăng nhập, step-up, đổi thiết bị; cảnh báo đăng nhập bất thường.

</details>

### Bài nói 2 phút

*"How do you secure a transfer API in a mobile banking app?"* Phải có: Authorization Code + PKCE, token
ngắn + refresh xoay vòng, kiểm JWT đúng (alg cố định, iss, aud), **phân quyền theo đối tượng**, step-up có
ký nội dung giao dịch, idempotency key, audit log, và không có PII trong token hay log.

---

## Tuần 15–16 — AWS, DR, deploy an toàn

### Đầu ra phải có cuối tuần 16

- [ ] **Budget alert tạo trước tiên.**
- [ ] Capstone chạy trên AWS bằng Terraform: VPC 2–3 AZ, ALB, ECS Fargate (hoặc EKS), RDS/Aurora
      PostgreSQL, Kafka (MSK Serverless hoặc một broker KRaft trên EC2 cho lab), secret trong Secrets Manager.
- [ ] Deploy canary hoặc blue/green, có tự rollback khi lỗi.
- [ ] Diễn tập DR: khôi phục DB từ snapshot, đo RTO thật, viết runbook.
- [ ] Load test trên cloud (capstone A: 500 TPS) và README capstone có số.
- [ ] Review toàn bộ code capstone theo [checklist 01 §6](01-java-code-cham-duoi-tai-cao.md#6-checklist-review-code-trước-khi-lên-tải).
- [ ] Bản vẽ **V10**; đề bấm giờ **DR đa region cho core banking**.
- [ ] Bài nói 2 phút: *"Walk me through your capstone on AWS and how it survives losing an availability zone."*

### Đọc

| Buổi | Tài liệu | Phần |
|---|---|---|
| T2 tuần 15 | AWS Well-Architected Framework, trụ *Reliability* và *Cost Optimization* | |
| T4 tuần 15 | AWS whitepaper *Disaster Recovery of Workloads on AWS* | 4 chiến lược DR, RTO/RPO |
| T2 tuần 16 | Tài liệu ECS: deployment circuit breaker, blue/green với CodeDeploy | |
| T4 tuần 16 | [05 — hệ thống thật all-in-one trên AWS](../katalon-prep/katalon-system-design/05-he-thong-that-allinone-aws.md) | Đối chiếu với hệ thống bạn đang vận hành |

### Ghi chú khái niệm

#### 15.1 Bố cục mạng chuẩn

```text
VPC 10.x.0.0/16, 3 AZ
├── public subnet   (mỗi AZ)  ALB, NAT gateway
├── private subnet  (mỗi AZ)  task ECS / pod EKS — không IP public
└── data subnet     (mỗi AZ)  RDS/Aurora, ElastiCache, MSK — chỉ nhận kết nối từ private subnet
Security group nối thành chuỗi: Internet → ALB → app → DB. VPC endpoint cho S3, Secrets Manager, ECR
để lưu lượng không đi qua NAT (rẻ hơn và an toàn hơn).
```

#### 15.2 Bốn chiến lược DR

| Chiến lược | RPO | RTO | Chi phí | Hợp với |
|---|---|---|---|---|
| Backup & restore | Giờ | Giờ | Thấp nhất | File sao kê, dữ liệu báo cáo |
| Pilot light | Phút | Hàng chục phút | Thấp | Dữ liệu replicate liên tục, hạ tầng compute tắt sẵn |
| Warm standby | Giây – phút | Phút | Trung bình | **Core banking** thường chọn mức này |
| Multi-site active/active | ~0 | ~0 | Cao nhất | Chỉ khi nghiệp vụ chia được để không hai region cùng ghi một tài khoản |

Aurora Global Database (theo tài liệu AWS) replicate sang region khác với độ trễ thường dưới 1 giây; vẫn là
**bất đồng bộ**, nên RPO không bằng 0. Con số RTO thật chỉ có sau khi **diễn tập**.

#### 15.3 Deploy an toàn

- **Canary**: 5% lưu lượng sang bản mới, gác bằng alarm (5xx, p99, lỗi nghiệp vụ như tỉ lệ chuyển tiền
  thất bại), tự rollback; sau đó 25%, 100%.
- **Blue/green**: hai môi trường, chuyển lưu lượng ở ALB, rollback bằng chuyển lại.
- **Feature flag**: tách *deploy* khỏi *release*; tắt tính năng không cần deploy lại.
- **Migration DB kiểu expand/contract**: thêm cột mới (expand) → code ghi cả hai, đọc cột mới → backfill →
  code chỉ dùng cột mới → bỏ cột cũ (contract). Mỗi bước tương thích ngược để rollback code luôn được.

#### 15.4 Bẫy chi phí hay gặp

NAT gateway tính tiền theo **lượng dữ liệu đi qua** (dùng VPC endpoint); dữ liệu **giữa các AZ** có phí (app ở
AZ-a gọi DB/Kafka ở AZ-b liên tục); MSK và RDS chạy 24/7 kể cả khi lab không dùng (tắt hoặc `terraform
destroy` sau buổi lab); log CloudWatch không đặt thời hạn giữ.

### Lab 15–16 — Đưa capstone lên AWS (Thứ Bảy tuần 15 và 16)

Khung Terraform tham khảo, **đã `terraform validate`, chưa apply**: [ops-lab/terraform/](ops-lab/terraform/)
(VPC 2 AZ, ALB, ECS Fargate có deployment circuit breaker tự rollback, RDS PostgreSQL 16 mã hoá với mật
khẩu do RDS quản lý trong Secrets Manager, budget alert). Mỗi chỗ cố ý rẻ cho lab có chú thích
"Production: …".

<details markdown="1">
<summary><b>Budget alert — tạo đầu tiên (trích từ khung đã validate)</b></summary>

```hcl
resource "aws_budgets_budget" "monthly" {
  name         = "${var.name}-monthly"
  budget_type  = "COST"
  limit_amount = var.budget_usd
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.budget_email]
  }
}
```

</details>

| Bước | Làm | Bằng chứng nộp |
|---|---|---|
| 1 | Budget alert, rồi state Terraform trên S3 có khoá | `terraform plan` sạch |
| 2 | Mạng + DB + ECS + ALB; secret từ Secrets Manager, quyền bằng task role | URL ALB trả `/actuator/health` UP |
| 3 | Kafka cho capstone | Lab 8 của giai đoạn 2 chạy được trên cloud |
| 4 | Deploy bản lỗi cố ý (health check fail) | ECS tự rollback, ghi thời gian |
| 5 | Canary/blue-green với alarm 5xx | Ảnh chụp lần rollback tự động |
| 6 | Diễn tập DR: khôi phục snapshot RDS sang instance mới, trỏ app sang | **RTO đo được** + runbook từng bước |
| 7 | Load test từ máy EC2 cùng region (không từ laptop) | Bảng số trong README |
| 8 | `terraform destroy` sau mỗi buổi | Hoá đơn tháng dưới budget |

### Track P tuần 15–16

Review toàn bộ code capstone theo [checklist 01 §6](01-java-code-cham-duoi-tai-cao.md#6-checklist-review-code-trước-khi-lên-tải),
13 câu hỏi. Mỗi "có" thành một issue có mã P và cách sửa; sửa hết trước load test bước 7. Ghi số issue tìm
được vào README capstone: đó là bằng chứng của kỷ luật review, không phải điểm trừ.

### Track S tuần 15–16 — V10: Mini Core Transfer ở L3 (10M) trên AWS (Chủ nhật tuần 16, 45 phút)

Vẽ lại [02 §3.4](02-ve-he-thong-100k-1m-10m.md#34-l3--10m-users-microservices-có-lý-do) bằng dịch vụ
AWS, AZ là hộp bao, ghi RPO/RTO và đường failover.

<details markdown="1">
<summary><b>Ánh xạ tham khảo</b></summary>

| Thành phần ở 02 §3.4 | AWS | Ghi chú |
|---|---|---|
| CDN + WAF + global LB | CloudFront + AWS WAF + Route 53 | Route 53 health check cho failover region |
| API gateway / BFF | ALB + service gateway riêng, hoặc API Gateway | |
| Services | ECS Fargate (hoặc EKS) trải 3 AZ | Deployment circuit breaker, canary |
| Ledger DB | Aurora PostgreSQL, partition theo tháng, **RDS Proxy** thay PgBouncer | Aurora Global Database sang region DR |
| Kafka | Amazon MSK, 3 broker / 3 AZ, RF 3, `min.insync.replicas=2` | |
| CDC | Debezium trên MSK Connect | |
| Read model | OpenSearch (tìm kiếm), ElastiCache Redis (số dư hiển thị) | |
| Realtime gateway | ECS service riêng sau NLB | Kết nối dài |
| Observability | OpenTelemetry Collector → Amazon Managed Prometheus/Grafana, X-Ray hoặc Tempo | |
| Secret, khoá | Secrets Manager, KMS | |
| Region DR | Warm standby: Aurora secondary + ECS thu nhỏ, scale lên khi failover | RPO giây, RTO phút sau diễn tập |

</details>

### Bài tập tuần 15–16

**15.1 — Chọn chiến lược DR.** (a) Sổ cái: RPO ≤ 1 phút, RTO ≤ 15 phút. (b) Kho file sao kê PDF: RPO 24 giờ,
RTO 1 ngày. (c) Trang giới thiệu sản phẩm.

<details markdown="1">
<summary><b>Đáp án 15.1</b></summary>

(a) **Warm standby**: Aurora Global Database + ECS ở region DR chạy tối thiểu, runbook failover đã diễn tập.
(b) **Backup & restore**: S3 Cross-Region Replication hoặc chỉ versioning + backup; sinh lại được từ sổ cái.
(c) Tĩnh trên S3 + CloudFront, origin dự phòng ở region khác; gần như không tốn gì.

</details>

**15.2 — Đổi tên cột không downtime.** Đổi `amount numeric` thành `amount_minor bigint` trên bảng 500 triệu
dòng, app đang chạy 20 pod, phải rollback được ở mọi bước.

<details markdown="1">
<summary><b>Đáp án 15.2</b></summary>

1. **Expand**: thêm cột `amount_minor` cho phép null (không khoá bảng lâu).
2. Deploy code **ghi cả hai cột**, vẫn đọc cột cũ.
3. **Backfill** theo lô nhỏ (vài nghìn dòng mỗi lô, có nghỉ) để không làm nghẽn replica.
4. Kiểm tra hai cột khớp (query đối soát); deploy code **đọc cột mới**, vẫn ghi cả hai.
5. Deploy code chỉ ghi cột mới; thêm `NOT NULL` (Postgres: thêm constraint `NOT VALID` rồi `VALIDATE`).
6. **Contract**: xoá cột cũ ở một release sau.

Rollback ở bước nào cũng được vì code của bước trước vẫn chạy được với schema của bước sau.

</details>

**15.3 — ECS Fargate hay EKS?** Team 8 người, 6 service, chưa ai vận hành Kubernetes.

<details markdown="1">
<summary><b>Đáp án 15.3</b></summary>

**ECS Fargate**: không quản node, không quản control plane, đủ cho autoscale, deploy có rollback, task
role. EKS đáng khi cần hệ sinh thái Kubernetes (operator, service mesh, KEDA, chạy đa cloud) hoặc đã có
platform team. Đánh đổi: ECS khoá vào AWS và ít công cụ cộng đồng hơn. Đây cũng là điều hệ thống thật của
bạn đã làm (chuyển EKS sang ECS), nên kể bằng chính câu chuyện đó.

</details>

**15.4 — Đề bấm giờ: DR đa region cho core banking** (45 phút). Region chính Sydney, yêu cầu RPO ≤ 1 phút,
RTO ≤ 30 phút, chi phí DR ≤ 30% chi phí chính.

<details markdown="1">
<summary><b>Khung lời giải</b></summary>

Warm standby ở Melbourne. Dữ liệu: Aurora Global Database (bất đồng bộ, RPO giây), MSK replicate bằng
MirrorMaker 2 (offset của consumer phải được dịch), S3 CRR. Compute: ECS ở region DR chạy 10–20% công suất,
autoscale khi failover. Quyết định failover **do người bấm** theo runbook, không tự động hoàn toàn: tránh
hai region cùng nhận ghi. Sau failover: đối soát những giao dịch trong khoảng RPO (đã commit ở Sydney nhưng
chưa sang Melbourne) bằng log của đối tác và client retry có idempotency key. Diễn tập mỗi quý, đo RTO thật.
Đánh đổi chính: RPO > 0 nghĩa là có thể phải xử lý tay một số giao dịch, và đó là chi phí chấp nhận được
so với active/active.

</details>

### Bài nói 2 phút

*"Walk me through your capstone on AWS and how it survives losing an availability zone."* Phải có: bố cục
3 tầng subnet, mỗi tầng trải nhiều AZ, ALB chỉ gửi vào target khoẻ, DB Multi-AZ failover, Kafka RF 3 với
`min.insync.replicas=2`, task ECS phân bố theo AZ, và con số từ lần diễn tập (RTO bạn đo được).

---

## Capstone tuần 11–16

| Tuần | Capstone A — Mini Core Transfer | Capstone B — Distributed Test Runner |
|---|---|---|
| 11 | Module ranh giới rõ, test ArchUnit/Modulith; context map | Như bên trái; tenant là khái niệm xuyên mọi module |
| 12 | Load test chuyển tiền, tìm và sửa 1 anti-pattern; Resilience4j cho cổng đối tác | Load test nhận job; một tenant 10.000 job không làm nghẽn tenant khác |
| 13 | Trace xuyên saga và Kafka; SLO chuyển tiền | Trace từ API tới worker; SLO thời gian chờ trong hàng đợi |
| 14 | Keycloak, BOLA, step-up, audit log | Keycloak đa tenant, cách ly dữ liệu theo tenant |
| 15–16 | AWS, canary, DR, load test 500 TPS | AWS, autoscale worker theo độ dài hàng đợi (KEDA nếu EKS) |

**README capstone** (nộp tuần 16): hệ thống là gì (3 dòng) · sơ đồ (V10 thu nhỏ theo thứ đã làm) · bảng
số: tải, p99, lỗi, trước/sau tối ưu · thí nghiệm thử phá và kết quả · các quyết định chính và đánh đổi
(link ADR) · cách chạy · chi phí AWS thật của tháng.

---

## Mốc tuần 12

| | Tiêu chí | Bắt buộc |
|---|---|:---:|
| 1 | Có design doc đầu tiên được review **ở công việc thật** (theo lộ trình gốc) | ✅ |
| 2 | Load test capstone tìm và sửa ít nhất một anti-pattern, **bảng số trước/sau** | ✅ |
| 3 | Tái hiện được bảng ops-lab trên máy mình, giải thích bằng Little's Law | ✅ |
| 4 | Client gọi đối tác có đủ 4 lớp (timeout, retry có jitter, breaker có fallback, bulkhead) + test chaos | ✅ |

## Mốc tuần 16 — tiêu chí qua giai đoạn

| | Tiêu chí | Bắt buộc |
|---|---|:---:|
| 1 | Capstone chạy trên AWS bằng Terraform, có budget alert | ✅ |
| 2 | Có số load test trên cloud và README capstone theo mẫu | ✅ |
| 3 | Diễn tập DR có RTO đo được và runbook | ✅ |
| 4 | Một lần rollback tự động đã xảy ra thật (canary hoặc deployment circuit breaker) | ✅ |
| 5 | Review capstone theo checklist track P, mọi issue đã xử lý hoặc có lý do để lại | ✅ |
| 6 | Bản vẽ V8, V9, V10 | ✅ |
| 7 | 5 đề bấm giờ của giai đoạn, tự chấm ≥ 7/10 | ✅ |
| 8 | 5 bài nói 2 phút đã ghi âm | ✅ |
| 9 | Thi AWS Solutions Architect – Associate | Tùy chọn |

Trễ quá 2 tuần: bỏ DR đa region (giữ diễn tập khôi phục snapshot), dùng một broker Kafka thay MSK, giữ
nguyên load test và README: đó là thứ được hỏi ở giai đoạn 4.

---

## Ranh giới trung thực

| Nội dung | Trạng thái |
|---|---|
| Bảng P09 ở lab 12 | **Đã chạy** ngày 07/10/2026: Spring Boot 3.3.13, HikariCP pool 10, PostgreSQL 16.4 (embedded), đối tác giả 50 ms, Gatling 3.11.5 mô hình mở. Lượt `VT=true` chạy 2 lần. App, Postgres và Gatling **cùng một máy 4 vCPU**, nên số tuyệt đối (đặc biệt good @ 800 rps có p99 511 ms) chịu ảnh hưởng tranh CPU. Output: [ops-lab/results](ops-lab/results/2026-10-07.txt) |
| Retry storm | **Đã chạy 2 lần**, kết quả gần như trùng nhau. Mô hình downstream **từ chối ngay** khi đầy, không chậm dần, nên không tái hiện vòng xoáy chậm → timeout → retry của sự cố thật |
| Khung Terraform | **Đã `terraform validate` và `terraform fmt -check`** (Terraform 1.16.5, provider AWS 6.67.0, checksum SHA-256 khớp). **Chưa `plan`/`apply`**: không có tài khoản AWS ở đây |
| Lab 11, 13, 14, 15–16 trên capstone | **Chưa làm trong workspace này.** Đoạn code ArchUnit/Spring Modulith, cấu hình OpenTelemetry, rule Prometheus, cấu hình Spring Security là mẫu theo tài liệu, **chưa chạy** |
| Ngưỡng burn rate (14,4 / 6 / 1) | Theo Google SRE Workbook |
| Độ trễ replicate của Aurora Global Database | Theo tài liệu AWS, chưa đo |
| Đáp án đề bấm giờ | Khung lời giải hợp lý, không phải đáp án duy nhất |
| Tên chương sách, tên tài liệu | Theo bản hiện hành tại thời điểm viết; tìm theo tên nếu link đổi |
