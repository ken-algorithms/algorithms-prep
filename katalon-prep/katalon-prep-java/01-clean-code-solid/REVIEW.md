# Module 01 — Clean code / SOLID: case study từ code RCI thật

> Catalog design pattern nằm ở [PATTERNS.md](PATTERNS.md).

> Đây là module **quan trọng nhất** trong workspace, vì nó xử lý đúng cái lỗi lặp lại trong code
> production của bạn, và vì "clean code + design pattern" là vòng Katalon chắc chắn đào.

## Cách làm (đừng đọc `after/` trước)

```bash
mvn -pl 01-clean-code-solid test     # 61 test pass (27 clean-code + 34 patterns)
```

1. Mở [`before/ServiceHealthAggregatorBefore.java`](src/main/java/com/prep/cleancode/before/ServiceHealthAggregatorBefore.java).
   Tự tìm vấn đề, ghi ra giấy. Mục tiêu ≥10/14.

2. Đọc bảng dưới, đối chiếu.
3. Chạy [`BeforeFailureModesTest`](src/test/java/com/prep/cleancode/before/BeforeFailureModesTest.java)
   — 7 test chứng minh từng failure mode.

4. Giờ mới mở [`after/`](src/main/java/com/prep/cleancode/after/).
5. Cuối cùng: **mở file thật trong RCI và refactor nó**. Đó là PR thật và là story phỏng vấn.

`before/` là bản distilled (đã ẩn danh, bỏ tên vendor) của
`rci-backend/rci-gateway/src/main/java/com/rci/gateway/app/strategy/HealthCheckContextStrategy.java`.

---

## 14 vấn đề, kèm rule Sonar thật

| # | Vấn đề | Sonar rule | Vì sao là vấn đề thật |
|---|---|---|---|
| 1 | **Field injection** (`@Autowired` field, không final) | `java:S3306` | Không tạo được object hợp lệ bằng constructor → test phải dùng reflection hoặc `@SpringBootTest`. Object tồn tại được ở trạng thái nửa vời |
| 2 | **6/8 dependency khai báo nhưng không dùng** (code thật: 12/14) | `java:S1068` | Spring vẫn tạo bean, vẫn tốn memory, vẫn có thể fail khi khởi tạo — để rồi không ai gọi. Người đọc sau không biết cái nào thật cần |
| 3 | **`switch` bên trong Strategy pattern** | — (lỗi design) | **Vấn đề trung tâm.** Strategy tồn tại chính xác để xóa switch. Còn switch = mỗi lần thêm service phải sửa class này → vi phạm **Open/Closed** |
| 4 | **`default:` ném lúc runtime** | — | Thiếu wiring lẽ ra phải phát hiện lúc **khởi động**, không phải lúc request đầu tiên chạm vào lúc 3 giờ sáng |
| 5 | **`Collectors.toMap` không có merge function** | — | Trùng key → `IllegalStateException` lúc runtime. Xem test bẫy này trong module `00` |
| 6 | **Biến chết `doneJobs`** | `java:S1481`, `java:S1854` | Không chỉ vô dụng — nó **gây** lỗi 500. Xem mục "Phát hiện quan trọng" dưới |
| 7 | **Nuốt exception, trả `null`** | `java:S2221`, `java:S1181` | Bắt `Exception` chung chung, không log, dùng `null` làm giá trị báo lỗi → mọi caller phải null-check, quên là NPE ở chỗ khác |
| 8 | **Không có timeout** (`future.get()` không tham số) | — | 1 downstream treo → cả health check treo vô hạn. Health check tự biến thành sự cố |
| 9 | **Varargs theo vị trí** `Health... health` rồi `health[0]`, `[1]`, `[2]` | `java:S3346` | Caller phải nhớ đúng thứ tự, compiler không giúp. Thiếu phần tử → `ArrayIndexOutOfBounds` |
| 10 | **Log message sai và copy-paste** (`"Can not update properties."`) | `java:S106` (dùng `System.err`) | Đọc log production không biết service nào, vì sao. Message còn không liên quan tới việc đang làm |
| 11 | **Code comment-out để lại trong repo** | `java:S125` | Không ai dám xóa vì không biết còn cần không → nằm đó mãi mãi |
| 12 | **Unchecked cast ẩn trong helper `browse()`** | `java:S1905` | `ClassCastException` nổ ở chỗ không liên quan tới nguyên nhân |
| 13 | **Trả `HashMap`/`ArrayList` mutable ra ngoài** | `java:S1319` | Caller sửa được trạng thái bên trong |
| 14 | **Wildcard static import** `import static ...ServiceName.*` | `java:S2208` | Không biết symbol đến từ đâu, dễ va tên |

**SonarLint trong IDE bạn đã tự flag** `java:S1481`, `java:S1854`, `java:S106`, `java:S1192` khi
tôi viết file này — tức là bảng trên không phải lý thuyết.

---

## Phát hiện quan trọng nhất: hai bug đang che lấp nhau

Đây là thứ chỉ lộ ra khi **chạy thật**, không phải khi đọc code. Bản thân tôi cũng đoán sai lúc đầu.

Trong `doHealthCheck()`:

```java
// dòng "biến chết" — trông như vô hại
List<Health> doneJobs = jobMap.values().stream().map(CompletableFuture::join).toList();
...
result.put(AUTH.getId(), buildResponse(getSafe(jobMap.get(AUTH))));  // getSafe nuốt lỗi
```

**Hành vi thật khi một downstream lỗi** (test `[3a]`):

`join()` trên future **failed** thì **ném** `CompletionException`. Dòng biến chết chạy **trước**
`getSafe()`. Nên:

- cả method nổ ra ngoài → endpoint health trả **500**
- `getSafe()` — đoạn duy nhất được viết ra **để** xử lý lỗi — **không bao giờ chạy tới**. Nó là dead code.

**Hành vi sau khi "dọn dẹp vô hại"** (test `[3b]`):

SonarLint đòi bạn xóa dòng biến chết (`S1481` + `S1854`). Mọi reviewer cũng sẽ đề nghị vậy. Xóa xong:

- không ném nữa
- `getSafe()` giờ mới thật sự chạy → nuốt lỗi → trả `null`
- `buildResponse(null)` in ra `"Can not update properties."` rồi trả **map rỗng**
- service khỏe vẫn báo `status: UP` → dashboard trông **gần như ổn**

Bạn vừa biến một sự cố **dễ thấy** (500, alert ngay) thành một sự cố **không ai thấy** (dashboard
xanh, 3 tháng sau mới có người hỏi). Về mặt Sonar bạn đã "fix" 2 issue. Về mặt vận hành bạn vừa làm
mọi thứ tệ hơn.

> **Đây là câu trả lời hoàn hảo cho câu hỏi phỏng vấn "vì sao phải viết test trước khi refactor?"**
> Không phải vì lý thuyết. Vì "xóa dead code" nghe như zero-risk nhưng ở đây nó đổi hành vi từ
> fail-loud sang fail-silent. Không có test, không ai phát hiện.
>
> Và nó cũng là câu trả lời cho *"dùng AI gen code có sao không?"* — cái bẫy này không đến từ AI,
> nó đến từ việc **không chạy code sau khi review**.

---

## `after/` — thay đổi gì và vì sao

### Thay đổi mang tính quyết định: strategy tự khai báo `service()`

```java
// TRƯỚC: strategy không biết nó phục vụ service nào
public interface HealthCheckStrategy {
    CompletableFuture<Health> doCheck();
}
// → người GỌI phải biết → sinh ra switch + 14 field @Qualifier

// SAU: strategy tự trả lời "tôi lo service nào"
public interface HealthCheck {
    ServiceName service();
    CompletableFuture<Health> check();
}
// → registry tự build map → thêm service KHÔNG sửa file nào đang có
```

Trong Spring, điều này cho phép:

```java
@Component
class HealthCheckRegistry {
    HealthCheckRegistry(List<HealthCheck> checks) { ... }  // Spring inject MỌI bean HealthCheck
}
```

Thêm 1 `@Component` mới là xong. **Không switch, không `@Qualifier`, không sửa registry.**

### Bảng đối chiếu

| Khía cạnh | `before` | `after` |
|---|---|---|
| Wiring | `switch` + 14 `@Qualifier` field | `List<HealthCheck>` → `EnumMap`, tự đăng ký |
| Thêm service mới | sửa switch + thêm field + thêm qualifier | thêm 1 class, **không sửa gì cả** |
| Sai cấu hình (2 bean trùng service) | không phát hiện | **không khởi động được**, message chỉ rõ cả 2 class |
| Service chưa có check | ném `UnsupportedOperationException` | `Optional.empty()` |
| Downstream lỗi | 500 cho cả endpoint | `Health.down(reason)` — cô lập trong 1 entry |
| Downstream treo | treo vô hạn | `orTimeout` → `Health.unknown` |
| Timeout vs lỗi thật | không phân biệt | **`UNKNOWN` vs `DOWN`** |
| Model | class mutable + `Map<String,Object>` | `record` + `Map<String,String>` + `Map.copyOf` |
| Trả về | `HashMap` mutable | `Map.copyOf` — ném nếu sửa |
| Test | cần Spring/reflection | **plain JUnit, không framework, không network** |

### Vì sao tách `UNKNOWN` khỏi `DOWN`

Điểm này interviewer sẽ đào, và nó phân biệt Senior với Lead:

- **`DOWN`** = đã gọi, service trả lời thất bại → có hành động rõ ràng
- **`UNKNOWN`** = **ta không biết** (timeout, mất mạng) → service có thể vẫn sống

Gộp chung lại sẽ gây **báo động giả** và làm **sai SLO/error budget**. Timeout không chứng minh
service đã chết, nó chỉ chứng minh ta không kịp biết.

---

## SOLID — map vào code cụ thể, không nói lý thuyết

| Nguyên lý | `before` vi phạm ở đâu | `after` giải quyết thế nào |
|---|---|---|
| **S**RP | 1 class làm 4 việc: tra cứu strategy, gọi song song, xử lý lỗi, format response | `HealthCheckRegistry` (tra cứu) · `HealthAggregator` (orchestration) · từng `HealthCheck` (1 service) · `Report` (kết quả) |
| **O**CP | thêm service → **phải sửa** switch | thêm service → **thêm 1 class**, không sửa file cũ |
| **L**SP | không có hợp đồng — impl muốn ném gì thì ném, trả null cũng được | `HealthCheck` ghi rõ 3 điều khoản; `guarded()` **cưỡng chế** cả khi impl vi phạm (test `misbehavingCheckThatThrows`, `nullFutureIsContained`) |
| **I**SP | — (interface đã nhỏ) | giữ 2 method, không phình |
| **D**IP | phụ thuộc vào 14 class cụ thể qua `@Qualifier` | chỉ phụ thuộc `HealthCheck` + `Duration` timeout → test bằng fake |

## Pattern dùng ở `after/`

- **Strategy** (làm đúng): `HealthCheck` + self-registration, không switch
- **Registry**: `Map<ServiceName, HealthCheck>` build 1 lần, fail-fast
- **Value Object**: `Health`, `Report` — record immutable
- **Null Object / errors-as-values**: `Health.down` / `Health.unknown` thay cho `null` + exception
- **Guard/Decorator**: `guarded()` bọc mọi check để cưỡng chế hợp đồng

> Katalon on-domain: `HealthCheck` ↔ nhiều test engine (Selenium / Appium / Playwright) là **cùng
> một hình dạng Strategy**. Khi được hỏi "cho ví dụ Strategy pattern", dùng đúng ví dụ này rồi nói:
> *"và đây là cách tôi làm SAI nó lần đầu, cùng cách tôi sửa"*. Câu đó mạnh hơn mọi định nghĩa sách.

---

## Checklist tự kiểm tra

- [ ] Giải thích được vì sao `switch` trong Strategy là vi phạm Open/Closed
- [ ] Giải thích được vì sao field injection làm code khó test
- [ ] Nói được `join()` khác `get()` ở đâu, và cái nào ném gì
- [ ] Giải thích được vì sao timeout → `UNKNOWN` chứ không phải `DOWN`
- [ ] Kể được câu chuyện "hai bug che lấp nhau" mà không cần mở file
- [ ] Chỉ ra được trong `HealthAggregator.check()` chỗ nào bắt buộc phải khởi tạo hết future
      trước khi join, và vì sao (nếu không sẽ chạy tuần tự)

- [ ] Đã refactor file thật trong RCI
