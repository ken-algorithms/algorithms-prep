# Module 02 — Quarkus service (P0, gap lớn nhất của bạn)

```bash
mvn -pl 02-quarkus-service test     # 11 test pass
./mvnw quarkus:dev                  # live reload (cần Postgres ở localhost:5432)
```

**Quarkus 3.28.5 + Panache + Flyway + PostgreSQL 16.4 thật — KHÔNG cần Docker.**

Đây là gap #1: Quarkus xuất hiện ở **mọi** JD backend của Katalon, còn RCI của bạn thuần Spring Boot.

## Chạy trên JDK 21, không phải JDK 25

Máy bạn mặc định JDK 25, nhưng Katalon dùng **Java 21**. Module này (và `03`) dùng
**maven-toolchains-plugin** để ép JDK 21:

```text
~/.m2/toolchains.xml  →  /Users/duc.nguyen/.sdkman/candidates/java/21.0.12-tem
```

Java 21.0.12 đã được cài qua SDKMAN trong buổi này; JDK 25 vẫn là default của máy. Vừa loại rủi ro
tương thích framework, vừa đúng môi trường Katalon.

## Không Docker: embedded-postgres thay Dev Services

Quarkus **Dev Services** tự bật Postgres container khi test — nhưng cần Docker.
[`EmbeddedPostgresResource`](src/test/java/com/prep/quarkus/EmbeddedPostgresResource.java) implement
`QuarkusTestResourceLifecycleManager`, khởi động PostgreSQL 16.4 thật như subprocess và ghi đè
`quarkus.datasource.jdbc.url`.

> **Vì sao không dùng H2 cho nhanh:** H2 không có `percentile_disc`, không có partial index, không
> có `jsonb`, `EXPLAIN` hoàn toàn khác. Test trên H2 rồi deploy lên Postgres là cách chắc chắn nhất
> để bug lọt lên production.

## API (on-domain Katalon)

```text
POST /api/runs                    tạo test run
POST /api/runs/{id}/results       nộp batch kết quả (tối đa 10.000)
POST /api/runs/{id}/complete      đóng run
GET  /api/runs/{id}/summary       pass/fail/flaky + p50/p95
GET  /api/runs?status=&page=&size= list có phân trang
GET  /q/health                    Quarkus health (có check datasource)
```

## Ba bug thật gặp khi chạy — và đây là phần đáng giá nhất

### 1. Catch-all `ExceptionMapper` biến lỗi 4xx của client thành 500

Ban đầu tôi viết `ExceptionMapper<RuntimeException>` trả 500 cho mọi trường hợp. Nhưng
`jakarta.ws.rs.WebApplicationException` (và `NotSupportedException`=415, `NotAllowedException`=405)
**cũng là** `RuntimeException` — và chúng **đã mang sẵn status đúng**.

Kết quả: request thiếu `Content-Type` lẽ ra nhận **415** thì nhận **500**. Client không biết mình
sai gì, còn on-call thấy alert 5xx giả. **Một catch-all quá rộng biến lỗi CỦA CLIENT thành lỗi CỦA
SERVER.**

Sửa: nếu là `WebApplicationException` thì tôn trọng status của nó, và 4xx không log ở mức `error`.
Test `catchAllMapperMustNotSwallowWebApplicationException` chứng minh.

### 2. Endpoint không có body vẫn đòi `Content-Type`

`@Consumes(APPLICATION_JSON)` đặt ở mức class cho tiện → `POST /{id}/complete` (không có body) cũng
đòi header. Sửa bằng `@Consumes(MediaType.WILDCARD)` cho endpoint đó.

### 3. `IDENTITY` làm vỡ batch insert

`test_result` dùng **`SEQUENCE`**, không dùng `IDENTITY`. Lý do: `IDENTITY` **buộc** Hibernate insert
từng dòng một để lấy id sinh ra → **batch bị vô hiệu hoàn toàn**. Đây là bẫy rất phổ biến khi tối ưu
bulk insert. Xem `V1__init.sql` và `TestResultRepository.persistBatch`.

## Điểm thiết kế đáng nói khi phỏng vấn

| Quyết định | Lý do |
|---|---|
| **Panache Repository**, không Active Record | Active record trộn truy vấn vào entity → entity vừa là model vừa là DAO, khó test, vi phạm SRP |
| **Flyway là nguồn sự thật duy nhất**, `schema-management.strategy=none` | `ddl-auto=update` không versioned, không review được, không rollback được, và có thể drop cột khi đổi tên field |
| **Không `@ManyToOne`** giữa `TestResultRow` và `TestRun` | Ở bảng hàng tỷ dòng, quan hệ JPA khiến Hibernate load + quản lý entity trong persistence context khi bulk insert. `run_id` dạng `bigint` đơn giản và nhanh hơn nhiều |
| **`flush()` + `clear()` mỗi batch** | Không thì 10.000 entity nằm trong persistence context → OOM ở quy mô thật |
| **Một query `group by`** thay cho 4 query `count` | 1 lần quét index thay vì 4. Ở bảng lớn là khác biệt về bậc |
| **`percentile_disc` tính trong DB** | Không kéo toàn bộ duration về app rồi sort |
| **Batch giới hạn 10.000** | Không giới hạn = một client gửi 10 triệu dòng làm OOM cả service. Phải trả **400 rõ ràng**, không phải 500 |
| **Lọc `tenantId` ở MỌI query** | Quên một chỗ là rò rỉ dữ liệu giữa khách hàng — bug nghiêm trọng nhất của SaaS |
| **`PASSED` + `attempts>1` → `FLAKY`** | Che con số này là cách test suite mục dần mà không ai biết |
| **`flakinessRate` chia 0 → `0.0`** | `NaN` làm vỡ mọi dashboard phía sau |

## Chuẩn bị cho câu hỏi

- *"Vì sao Quarkus thay vì Spring Boot?"* → build-time DI (không reflection lúc runtime) → startup
  nhanh hơn nhiều lần, RSS nhỏ hơn → **density cao hơn trên K8s → cost/pod thấp hơn**. Với SaaS
  nhiều service nhỏ thì đây là tiền thật. Đánh đổi: ecosystem nhỏ hơn, native image có bẫy reflection.
  → **Đo bằng số ở [module 03](../03-spring-boot-compare/COMPARISON.md).**

- *"Panache active record hay repository?"*
- *"Insert 10.000 dòng thế nào cho nhanh?"* → batch_size + **SEQUENCE không IDENTITY** + flush/clear
- *"Xử lý lỗi trong REST API thế nào?"* → RFC 7807 + **đừng để catch-all nuốt status có sẵn**

## Checklist

- [ ] Giải thích build-time DI của Quarkus và hệ quả về cost trên K8s
- [ ] Nói được vì sao `IDENTITY` làm vỡ batch insert
- [ ] Giải thích vì sao catch-all mapper là nguy hiểm, và ranh giới đúng của nó
- [ ] Chỉ ra được mọi chỗ lọc `tenantId` và điều gì xảy ra nếu quên một chỗ
- [ ] Chạy được `quarkus:dev` và sửa code thấy live reload
