# katalon-prep-java — phần Java (9 module Maven)

```bash
cd katalon-prep-java
mvn test                             # toàn bộ — 191 test, 0 failure (18 skipped: drill cố ý)
mvn -pl 01-clean-code-solid test     # chạy 1 module
```

Đã verify trên máy bạn: **191 test, 0 failure, 0 error, BUILD SUCCESS cả 9 module.**

`00/01/04/05/06/07/08` compile bằng JDK 25 với `release=21`; `02/03` chạy **JDK 21** qua
maven-toolchains (Java 21.0.12 cài bằng SDKMAN, JDK 25 vẫn là default của máy).
`maven.compiler.release=21` có chủ ý: Katalon dùng Java 21, nên compiler **chặn** mọi API sau 21 —
bạn không vô tình học API mà Katalon không có.

## Không cần Docker — cả 9 module

- **PostgreSQL** dùng [embedded-postgres](https://github.com/zonkyio/embedded-postgres): binary
  **PostgreSQL 16.4 thật** đóng gói trong artifact Maven, chạy như subprocess, tự dừng khi JVM
  thoát. Dùng ở [05](05-postgres-depth/), **và cả [02](02-quarkus-service/) + [03](03-spring-boot-compare/)**
  (thay Quarkus Dev Services / Testcontainers). Sau khi test **không còn gì trên máy**.
  Đã verify: `>>> PostgreSQL 16.4 on x86_64-apple-darwin20.6.0`
- **Kafka** → [06](06-distributed-resilience/) mô phỏng ngữ nghĩa (partition, ordering, consumer
  group, rebalance, DLQ) bằng code thuần. Phần *bắt buộc* cần broker thật chỉ là rebalance
  protocol thực tế và backpressure — lúc đó bật Docker tạm rồi xoá.
- **System design** → [08](08-system-design/) là primitive thuần Java. (Ở ROADMAP đầu tôi ghi là
  cần Docker — **sai, đã sửa**.)
- **Benchmark** không cần `hey`/`k6`/`wrk` → load generator viết bằng Java + virtual threads.

## Level Java thật của bạn (đo từ 2 project cũ)

Bạn nói "bỏ Java 1 năm". Nhưng dữ liệu cho thấy khoảng cách **hẹp hơn** bạn nghĩ nhiều.

| Project | Quy mô | Stack thật |
|---|---|---|
| **`success/rci`** | 470 file Java, 10 module Maven | **Java 21**, Spring Boot **3.3.13**, Spring Cloud 2023.0.6, OAuth2 Authorization Server, GraphQL + DataLoader, JPA, Redis, Spring Cloud Stream (RabbitMQ), OpenFeign, Undertow, Actuator, MapStruct, Lombok, AWS + Azure |
| **`zaitenllc/all-in-one-v2`** | 858 file Java | Java 8/11, domain ngân hàng (credit, KYC) |

**Điểm mạnh đã có bằng chứng:** Java 21 production thật · kiến trúc hexagonal (`app`/`domain`/`infra`) ·
multi-module Maven với `${revision}` · Strategy + Facade · `CompletableFuture` · GraphQL DataLoader
(nghĩa là đã đụng N+1 thật) · Spring Cloud Stream → chuyển Kafka rất nhanh.

**Gap thật, theo thứ tự nguy hiểm:**

| Gap | Bằng chứng | Bịt ở |
|---|---|---|
| 🔴 **Test gần như không có** | **4 file test / 470 file Java**, không Testcontainers. JD ghi *"strong unit testing"* | toàn workspace: **191 test** |
| 🔴 **Strategy pattern implement sai** | `switch` + `@Qualifier` ×14 bên trong chính cái Strategy | **`01`** ← lý do nó là module đầu |
| 🔴 **Field injection `@Autowired`** ×15 | Không test được không có Spring | `01` |
| ✅ ~~Không có Quarkus~~ | đã bịt | `02` (11 test) |
| 🔴 Không có Spark/PySpark | Không dấu vết | [phần Python](../katalon-prep-python/) — chưa làm, để sau |
| 🔴 Không có Playwright/CDP | Không dấu vết | chưa làm |

> **Kết luận thẳng:** vấn đề **không phải** "quên cú pháp Java". Là **kỷ luật test** và **một lỗi
> design pattern lặp lại**. Cả hai sửa được trong 2 tuần, và cả hai là thứ interviewer Lead đào đúng vào.

## Chín module

| # | Module | Test | Nội dung |
|---|---|:---:|---|
| **00** | [`00-refresher-java21/`](00-refresher-java21/) | **15** | Ôn Java 21 dạng test tự kiểm tra: pattern matching for switch, record pattern, guarded pattern, defensive copy, bẫy `Collectors.toMap`, bẫy `Set.of` **ném** chứ không dedupe, `groupingBy` không tạo key rỗng |
| **01** | [`01-clean-code-solid/`](01-clean-code-solid/) | **61** | **Flagship.** [REVIEW.md](01-clean-code-solid/REVIEW.md): `before/` = code RCI thật (14 vấn đề kèm rule Sonar) vs `after/`. [PATTERNS.md](01-clean-code-solid/PATTERNS.md): **6 pattern on-domain** — CoR (self-healing locator), Decorator (thứ tự đổi ý nghĩa metric), Strategy+Adapter, Observer (3 bug), Template Method, Builder |
| **02** | [`02-quarkus-service/`](02-quarkus-service/) | **11** | **Quarkus 3.28 + Panache + Flyway + Postgres thật.** REST multi-tenant, bulk insert (SEQUENCE không IDENTITY), RFC 7807. **3 bug thật gặp khi chạy** |
| **03** | [`03-spring-boot-compare/`](03-spring-boot-compare/) | **10** | **Spring Boot 3.3.13** (đúng version RCI), cùng API → so sánh 1:1. Constructor injection (sửa lỗi #1 của RCI). `bench.sh` + LoadGenerator (virtual threads) |
| **04** | [`04-concurrency/`](04-concurrency/) | **11** | Race condition, `AtomicLong` vs `LongAdder`, **deadlock + phát hiện bằng `ThreadMXBean`**, lock ordering, `tryLock`, **virtual threads** |
| **05** | [`05-postgres-depth/`](05-postgres-depth/) | **19** | **PostgreSQL 16 thật.** Test assert trực tiếp trên query plan. Index (**100×**), partial (**17×** nhỏ hơn), BRIN (**90×**), partitioning, MVCC/bloat, **`SKIP LOCKED`** |
| **06** | [`06-distributed-resilience/`](06-distributed-resilience/) | **19** | Retry + backoff + **jitter** (thundering herd), Kafka simulator: partition/ordering/hot partition, consumer group + **rebalance**, at-least-once → idempotent, **DLQ** |
| **07** | [`07-dsa-drill/`](07-dsa-drill/) | 3 + 18 ⏸ | Harness luyện **20 bài on-domain**, tắt Copilot, bấm giờ. Test `@Disabled` sẵn — bỏ annotation khi bắt đầu |
| **08** | [`08-system-design/`](08-system-design/) | **24** | **System design chạy được.** Bin-packing LPT (**3.81×**), weighted fair queueing, token bucket, circuit breaker 3 pha, idempotency + bẫy TTL |

## Sáu bug thật, tất cả chỉ lộ khi CHẠY

Đây là phần đáng giá nhất của workspace — không đọc code nào phát hiện được.

**Quarkus (2):**
1. Catch-all `ExceptionMapper<RuntimeException>` **nuốt** `WebApplicationException` → lỗi 415 của
   client thành **500 của server**. Alert 5xx giả, client không biết mình sai gì.
2. `@Consumes` ở mức class làm endpoint **không có body** vẫn đòi `Content-Type`.

**Spring Boot (3) — cả 3 cùng một nguyên nhân gốc: tôi gom file cho gọn:**
3. Repository là **nested interface** → Spring Data không tạo proxy.
4. Entity là **nested class** → Hibernate đăng ký tên `Entities$TestResultRow`, JPQL không resolve.
5. Thiếu `-parameters` vì parent không phải `spring-boot-starter-parent` — **RCI của bạn đúng là dạng đó**.

**Maven (1):**
6. Maven không recompile khi chỉ đổi pom → phải `clean` mới thấy `-parameters` vào bytecode.
   Tôi đã tưởng fix không hiệu lực.

## Thứ tự học

`01` (clean code/SOLID/patterns) → `08` (system design) → `05` (Postgres) → `02` (Quarkus — gap
lớn nhất) → `03` (so sánh + benchmark) → `06` → `00`/`04` (bổ trợ) → `07` (drill giữ nhiệt, làm
song song suốt).

Lộ trình theo ngày: xem [README gốc](../README.md#lộ-trình-3-tuần).

## Cách dùng cho hiệu quả

1. **Đừng đọc `after/` trước.** Giá trị nằm ở việc tự tìm ra vấn đề trong `before/`.
2. **Chạy test rồi cố tình làm nó fail.** Xoá `List.copyOf` trong `Health` → test nào đỏ? Bỏ
   `orTimeout` → test nào treo? Bỏ `VACUUM` ở Lab 01 test 5 → còn `Index Only Scan` không?
   Hiểu code bằng cách phá nó.
3. **SonarLint trong IDE bạn đang chạy live** — nó đã tự flag `java:S1481`, `S1854`, `S106`,
   `S1192` trong `before/`. Đèn đỏ ở `before/` là **đúng mục đích**; phải sạch ở `after/`.
4. **Nói to khi đọc.** Mục tiêu không phải hiểu, mà là **giải thích được cho người khác**.
5. **Số đo trong README các module là số thật từ máy bạn** — mang vào phỏng vấn được.

## Một việc bạn phải tự làm

Chạy `./bench.sh` rồi điền [COMPARISON.md](03-spring-boot-compare/COMPARISON.md). Tôi để trống có
chủ ý — số đo phải là của chính bạn thì mới dùng được trong phỏng vấn.
