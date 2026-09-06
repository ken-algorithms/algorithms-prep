# Module 03 — Spring Boot 3.3.13 (cùng API, để so sánh)

```bash
mvn -pl 03-spring-boot-compare test    # 10 test pass
./bench.sh                             # benchmark Quarkus vs Spring Boot
```

Dùng **đúng version của project RCI** (Spring Boot 3.3.13) để mọi bài học port thẳng sang project
thật của bạn. Cùng API contract với [module 02](../02-quarkus-service/) → so sánh 1:1.

Giá trị của module này **không** phải học Spring Boot (bạn đã giỏi) — mà là:

1. **Sửa đúng lỗi #1 của RCI**: constructor injection thay `@Autowired` field, cho thành phản xạ.
2. Có **số đo của chính mình** để nói về Quarkus vs Spring Boot.

## Ba bug thật gặp khi chạy — đều do "gom file cho gọn"

### 1 + 2. Nested type chống lại framework

Tôi gom repository vào `Repositories` và entity vào `Entities` cho gọn file. Hỏng **hai lần**:

| Lỗi | Nguyên nhân |
|---|---|
| `No qualifying bean of type 'Repositories$TestRunRepository'` | Spring Data **không** tạo proxy cho repository là nested interface |
| `UnknownEntityException: Could not resolve root entity 'TestResultRow'` | Hibernate đăng ký entity nested với tên `Entities$TestResultRow`, nên JPQL `from TestResultRow` không resolve |

**Bài học:** entity và repository phải là kiểu **top-level**, mỗi cái một file. Gom kiểu vào holder
class để tiết kiệm file là đi ngược quy ước framework. Cả hai lỗi **chỉ lộ khi CHẠY**, không lộ khi compile.

### 3. Thiếu `-parameters` khi không dùng `spring-boot-starter-parent`

```text
Name for argument of type [java.lang.Long] not specified, and parameter name
information not available via reflection
```

`spring-boot-starter-parent` bật sẵn `-parameters`. Parent của workspace này là `katalon-prep`, nên
phải tự bật trong [`pom.xml` gốc](../pom.xml). Đây là bẫy rất phổ biến ở project multi-module có
parent riêng — **và RCI của bạn đúng là dạng đó**, nên đáng nhớ.

## Đối chiếu Quarkus ↔ Spring Boot (từ code thật của 2 module)

| Khía cạnh | Quarkus (02) | Spring Boot (03) |
|---|---|---|
| DI | build-time, không reflection lúc runtime | runtime, reflection + proxy |
| Repository | `PanacheRepository` + query string | `JpaRepository` + **derived query từ tên method** |
| Kiểm tra query | lúc chạy | **lúc khởi động** (fail fast) — điểm cộng cho Spring |
| RFC 7807 | tự định nghĩa record `Problem` | **`ProblemDetail` built-in** — điểm cộng cho Spring |
| Test HTTP | `@QuarkusTest` + RestAssured | `@SpringBootTest` + MockMvc |
| Postgres trong test | `QuarkusTestResourceLifecycleManager` | `@DynamicPropertySource` |
| Virtual threads | mặc định reactive/worker pool | **một dòng**: `spring.threads.virtual.enabled=true` |
| Catch-all exception | tự viết → **dễ nuốt nhầm status** (đã gặp) | `ResponseEntityExceptionHandler` lo sẵn 404/405/415 |

## Benchmark: `bench.sh`

Không cần cài `hey`/`k6`/`wrk` — load generator viết bằng Java + **virtual threads**
([`LoadGenerator.java`](src/main/java/com/prep/spring/bench/LoadGenerator.java)). 200 request đồng
thời chỉ cần vài OS thread; nó vừa là công cụ đo vừa là ví dụ chạy được cho [module 04](../04-concurrency/).

```bash
CONC=200 SECS=30 ./bench.sh
```

Điền kết quả vào [COMPARISON.md](COMPARISON.md).

> **Ba điều phải nói khi trình bày số đo** (nếu không, số đo mất giá trị):
> 1. Client và server chạy **cùng máy** → tranh CPU. Số tuyệt đối không đúng với production; chỉ
>    dùng để **so sánh** hai framework trong cùng điều kiện.
> 2. Có **warm-up 5 giây** bị loại khỏi thống kê — JIT chưa compile và connection pool chưa đầy thì
>    vài giây đầu chậm gấp nhiều lần.
> 3. Đo **p50/p95/p99**, không đo trung bình. Trung bình giấu đuôi — mà đuôi mới là thứ người dùng phàn nàn.

## Checklist

- [ ] Viết service Spring Boot **không dùng `@Autowired` field** một cách tự nhiên
- [ ] Giải thích derived query fail-fast lúc khởi động vs query string kiểm tra lúc chạy
- [ ] Chạy `bench.sh` và điền `COMPARISON.md` bằng số của chính mình
- [ ] Nói được 3 cảnh báo về tính trung thực của benchmark
