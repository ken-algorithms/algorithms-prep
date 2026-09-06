# Design patterns — catalog on-domain cho Katalon

> Đi kèm [REVIEW.md](REVIEW.md) (case study Strategy làm sai → làm đúng, từ code RCI thật).
> Toàn bộ code ở [`patterns/`](src/main/java/com/prep/cleancode/patterns/), test ở
> [`PatternsTest.java`](src/test/java/com/prep/cleancode/patterns/PatternsTest.java) — **34 test pass**.

```bash
mvn -pl 01-clean-code-solid test          # 61 test (27 clean-code + 34 patterns)
```

**Nguyên tắc chọn ví dụ:** mọi pattern ở đây dùng đúng domain test automation của Katalon. Khi
interviewer hỏi "cho ví dụ Observer", trả lời bằng *"event bus cho test run, và đây là 3 bug tôi phải
xử lý"* mạnh hơn hẳn *"ví dụ như nút bấm và listener"*.

---

## Bảng tra cứu nhanh

| Pattern | File | Bài toán Katalon | Điểm interviewer sẽ đào |
|---|---|---|---|
| **Chain of Responsibility** | [`locator/Locators.java`](src/main/java/com/prep/cleancode/patterns/locator/Locators.java) | **Self-healing locator** — DOM đổi, locator vỡ | Vì sao không dùng if-else; confidence score; khi nào **không** được auto-update |
| **Decorator** | [`execution/Executors.java`](src/main/java/com/prep/cleancode/patterns/execution/Executors.java) | retry / timing / screenshot quanh 1 step | **Thứ tự decorator đổi ý nghĩa metric** |
| **Strategy + Adapter** | [`engine/EngineSelection.java`](src/main/java/com/prep/cleancode/patterns/engine/EngineSelection.java) | chọn Selenium / Appium / Playwright | Vì sao switch là vi phạm OCP; Adapter bảo vệ khỏi breaking change của lib |
| **Observer** | [`events/TestEventBus.java`](src/main/java/com/prep/cleancode/patterns/events/TestEventBus.java) | event bus báo kết quả test | 3 bug: listener ném, listener chậm, `ConcurrentModificationException` |
| **Template Method** | [`lifecycle/TestLifecycle.java`](src/main/java/com/prep/cleancode/patterns/lifecycle/TestLifecycle.java) | setUp → execute → tearDown | **teardown phải luôn chạy**; exception masking |
| **Builder** | cùng file trên | dựng `TestCase` có validate | validate ở `build()`, không ở setter; ràng buộc liên-trường |
| **Strategy (case study sai→đúng)** | [`before/`](src/main/java/com/prep/cleancode/before/) → [`after/`](src/main/java/com/prep/cleancode/after/) | health check aggregation | Xem [REVIEW.md](REVIEW.md) |

---

## 1. Chain of Responsibility — self-healing locator ⭐

**Đây là pattern đáng chuẩn bị nhất.** Nó chính là thứ JD gọi là *"web instrumentation and browser
automation"*, và là bài toán đặc trưng nhất của Katalon.

**Bài toán:** test tìm element bằng `#submit-btn`. Dev đổi UI, id biến mất. Test fail — nhưng không
phải vì sản phẩm lỗi, mà vì **locator giòn**. Đây là nguyên nhân số 1 làm test automation bị bỏ hoang.

**Giải pháp:** không dùng một locator, dùng một **chuỗi** xếp theo độ bền:

| Locator | `stability()` | Vì sao |
|---|---|---|
| `data-testid` | 100 | QA/dev chủ động đặt → gần như không bao giờ đổi |
| ARIA `role` + name | 80 | Gắn với ý nghĩa, khá bền |
| `text` | 60 | Đổi khi i18n |
| CSS class | 40 | Đổi mỗi lần refactor style |
| XPath theo vị trí | 10 | Vỡ ngay khi thêm một `<div>` |

Chuỗi **tự sắp xếp** theo `stability` — người viết test không phải nhớ thứ tự.

**Ba điểm phân biệt Lead:**

1. **`confidence = stability - attemptIndex × 15`.** Phải xuống sâu trong chuỗi nghĩa là các locator
   ưu tiên đã vỡ → DOM đổi nhiều hơn dự kiến → kết quả đáng tin **ít hơn** dù locator hiện tại trông ổn.

2. **`trustworthyEnoughToAutoUpdate()` — ngưỡng 70.** Dưới ngưỡng thì vẫn trả element (test chạy tiếp
   được), nhưng **không bao giờ tự sửa test suite của khách**. Sai một lần là mất niềm tin vĩnh viễn.

3. **Telemetry ghi "từ locator nào sang locator nào"** → dữ liệu để đề xuất *"test của bạn nên đổi sang X"*.
   Đó là sản phẩm, không chỉ là log.

**Vì sao CoR chứ không phải if-else:** chuỗi phải **cấu hình được** (mỗi tenant có convention khác),
**mở rộng được** (thêm chiến lược mới không sửa code cũ), và **báo cáo được** (mắt xích nào đã xử lý).
if-else không cho cả ba.

**Chịu lỗi:** `Page.query()` có thể ném (CDP mất kết nối) hoặc trả `null`. Một locator lỗi **không**
được làm chết cả chuỗi → `safeQuery()` cưỡng chế hợp đồng ở biên. Cùng bài học với
`HealthAggregator.guarded()`.

---

## 2. Decorator — và bài học về THỨ TỰ

Chạy một step test cần nhiều thứ phụ: đo thời gian, retry, screenshot, log, metric. Nhét hết vào một
class thì thành 300 dòng làm 6 việc — **chính xác hình dạng của `ServiceHealthAggregatorBefore`**.

**Điểm interviewer đào — thứ tự decorator là quyết định thiết kế, không phải chi tiết:**

```text
retry(timing(step))   → mỗi LẦN THỬ được đo riêng.  Metric = latency của 1 attempt
timing(retry(step))   → đo TỔNG cả quá trình retry. Metric = latency người dùng cảm nhận
```

Cả hai đều "đúng", nhưng trả lời hai câu hỏi khác nhau. Đặt sai thì **dashboard p95 nói dối**:
`retry(timing(...))` làm p95 trông đẹp giả tạo vì mỗi attempt ngắn, trong khi người dùng thực sự chờ
gấp 3 lần. Test `decoratorOrderChangesMeaning` chứng minh bằng số (~60ms vs ~180ms).

Tương tự với screenshot: `retry(screenshot(x))` chụp **mọi** lần fail (đầy đủ bằng chứng, tốn
storage); `screenshot(retry(x))` chỉ chụp khi hết retry (rẻ hơn, mất dấu vết các lần giữa).

**Điểm vận hành:** step pass sau khi retry **không phải** "pass" — nó là **flaky**, và `attempts > 1
&& passed` là tín hiệu đó. Che con số này đi là cách test suite mục dần mà không ai biết — đúng cái
bệnh mà TrueTest sinh ra để chữa.

**Chi tiết dễ bỏ qua:** screenshot **lỗi** không được che mất lỗi gốc. Test
`screenshotOnlyOnFailureAndNeverMasksTheRealError` assert `failureReason` vẫn là `"assertion failed"`.

---

## 3. Strategy + Adapter — chọn engine

Checklist gốc của bạn ghi đúng: *"Strategy (thay đổi linh hoạt giữa Selenium, Appium, Playwright)"*.
Đây là bản implement đúng cách.

**Mẹo làm Strategy tự đăng ký được:** tách **metadata để chọn** (`supportedPlatforms()`,
`capabilities()`, `preference()`) khỏi **hành động** (`run()`). Selector chỉ việc filter + sort. Thêm
engine mới = thêm một class, **không sửa gì** — test `addingEngineRequiresNoChangeToSelector` chứng minh.

Cùng ý tưởng với `HealthCheck.service()` ở `after/`: **strategy tự khai báo nó phục vụ gì**.

**Vì sao cần Adapter:** Selenium/Appium/Playwright có API hoàn toàn khác (`findElement` vs
`locator()` vs `$()`). Nếu code test gọi trực tiếp API từng lib thì đổi engine = viết lại toàn bộ test.
Adapter khoanh sự khác biệt vào một chỗ, và là lớp bảo vệ khi lib có breaking change.

---

## 4. Observer — event bus, và 3 bug kinh điển

| # | Bug | Hậu quả thật | Cách xử lý ở đây |
|---|---|---|---|
| 1 | **Listener ném** làm chết cả test run | Webhook của khách 500 → test run báo fail dù test pass. Từ "lỗi tích hợp" thành "mất dữ liệu kết quả" | `publish()` bắt `RuntimeException`, thu vào `failures()` kèm **tên listener** — cô lập nhưng **không nuốt** |
| 2 | **Listener chậm** làm nghẽn tất cả | HTTP đồng bộ 2s × 10.000 event | `BatchingWebhookListener` gom batch. **`RunFinished` buộc phải flush**, không thì batch cuối mất vĩnh viễn |
| 3 | **`ConcurrentModificationException`** khi listener tự unregister trong lúc đang được gọi | Chỉ xảy ra ở một số run → rất khó debug | `CopyOnWriteArrayList` — duyệt trên snapshot |

**Trade-off `CopyOnWriteArrayList`:** mỗi lần register/unregister copy cả array. Chỉ đúng khi
"đọc rất nhiều, ghi rất ít" — và đăng ký listener đúng là vậy. Nói được trade-off này là điểm cộng.

**Ranh giới trách nhiệm — điểm Lead:** test pass thì phải báo pass, **bất kể webhook của khách có
sống hay không**. Nhìn ra ranh giới đó là việc của Lead, không phải của người viết code.

---

## 5. Template Method — teardown LUÔN chạy

Bug thật: **teardown không chạy khi setUp hoặc execute ném**. Hậu quả: browser không đóng, container
không xóa, record trong DB không dọn → rò rỉ tài nguyên dần, đến lúc CI hết disk hoặc hết license Appium.

**Bạn không thể tin subclass sẽ nhớ viết `try/finally`.** Nên lớp cha giữ quyền điều phối:
`run()` là **`final`**, subclass chỉ điền vào hook. Thứ tự bước là **bất biến của hệ thống**, không
phải lựa chọn của người viết test.

Ba quyết định trong `run()`:

1. `finally` đảm bảo teardown chạy kể cả khi setUp ném.
2. setUp ném → **không** chạy execute (chạy tiếp trên trạng thái nửa vời chỉ sinh lỗi thứ hai che mất lỗi gốc).
3. Lỗi teardown ghi **riêng**, không ghi đè lỗi gốc — bẫy **exception masking**. Nếu chỉ `throw` trong
   `finally` thì lỗi gốc biến mất và bạn debug sai chỗ hàng giờ. Test
   `teardownFailureDoesNotMaskTheOriginalError` assert cả hai lỗi đều được giữ.

**Một quyết định có thể tranh luận** (nên chủ động nêu ra khi trình bày): teardown lỗi mà test pass
thì vẫn tính **PASS**. Lý do: teardown lỗi là vấn đề **hạ tầng**, không phải thất bại của sản phẩm
được test — báo fail sẽ gây báo động giả. Điểm quan trọng là lỗi **vẫn được ghi lại**.

---

## 6. Builder — validate một lần, báo cả cụm lỗi

Hai vấn đề Builder giải quyết:

1. **Constructor nhiều tham số cùng kiểu:** `new TestCase(name, suite, owner, tag, env)` — 5 `String`,
   đổi chỗ hai cái là compiler không báo gì, bug im lặng. Sonar có `java:S107` cho số tham số, nhưng
   vấn đề thật là **không đọc được**.

2. **Đối tượng nửa vời:** dùng setter thì object tồn tại ở trạng thái chưa hợp lệ giữa các lần set.

**Validate phải nằm trong `build()`, KHÔNG trong từng setter** — vì ràng buộc **liên-trường** chỉ
kiểm tra được khi đã có đủ thông tin:

```java
// Riêng lẻ đều hợp lệ: timeout 400s ok, retries 2 ok.
// Kết hợp thì vô lý: 400s × 3 attempt = 20 phút cho 1 test.
TestCase.builder("slow").timeout(Duration.ofSeconds(400)).maxRetries(2).build();
// → IllegalStateException: ... can exceed the run budget
```

Và **báo cả cụm lỗi một lần**, không bắt người dùng sửa 5 vòng — test `allProblemsReportedAtOnce`.

---

## SOLID — map vào code, không nói lý thuyết

| Nguyên lý | Ví dụ vi phạm | Ví dụ tuân thủ |
|---|---|---|
| **S**RP | `ServiceHealthAggregatorBefore` làm 4 việc | `HealthCheckRegistry` (tra cứu) / `HealthAggregator` (điều phối) / `HealthCheck` (1 service) |
| **O**CP | `switch` trong Strategy → thêm service phải sửa | `EngineSelector`, `HealthCheckRegistry` — thêm class là xong. **2 test chứng minh** |
| **L**SP | impl muốn ném gì thì ném, trả `null` cũng được | `HealthCheck` & `Page` ghi rõ hợp đồng; `guarded()` / `safeQuery()` **cưỡng chế** cả khi impl vi phạm |
| **I**SP | interface phình | `HealthCheck` 2 method; `Page` 1 method |
| **D**IP | phụ thuộc 14 class cụ thể qua `@Qualifier` | `SelfHealingLocator` chỉ biết `Page` → **test được không cần browser** |

> **Bằng chứng mạnh nhất của DIP:** toàn bộ 61 test của module này chạy **không Spring, không mock
> framework, không browser, không network**. Khi được hỏi *"làm sao biết code dễ test?"*, câu trả lời
> không phải "tôi viết test" mà là **"tôi không cần framework để test"**.

---

## Checklist tự kiểm tra

- [ ] Giải thích self-healing locator + vì sao có confidence score + khi nào **không** auto-update
- [ ] Nói được `retry(timing(x))` khác `timing(retry(x))` ở đâu và ảnh hưởng dashboard thế nào
- [ ] Giải thích vì sao `switch` trong Strategy là vi phạm OCP, và cách bỏ nó (self-declaring strategy)
- [ ] Kể 3 bug của Observer viết nải ngay + cách xử lý từng cái
- [ ] Giải thích exception masking và vì sao teardown lỗi không được che lỗi gốc
- [ ] Nói được vì sao validate phải ở `build()` chứ không ở setter
- [ ] Chỉ ra được `List.copyOf` / `Map.copyOf` ở đâu và nó chặn bug gì
