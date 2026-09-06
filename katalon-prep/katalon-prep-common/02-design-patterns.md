# Design pattern — Java ↔ Python

> **Đây là tài liệu, không phải code.** Pattern viết bằng Java và bằng Python nhìn *khác nhau*
> — đó chính là lý do tài liệu này tồn tại. Code chạy được nằm ở:
> [Java: `01-clean-code-solid/PATTERNS.md`](../katalon-prep-java/01-clean-code-solid/PATTERNS.md) (34 test)
> · [Python: `clean_code/` + `agent/`](../katalon-prep-python/src/prep/)

## 1. Bảng dịch — chỗ Python làm gọn hơn, chỗ Python không bảo vệ được

| Khái niệm | Java 21 | Python 3.12 | Hệ quả thật |
|---|---|---|---|
| **Interface** | `interface X` + `implements X` (nominal) | `Protocol` (structural) | Python: người cung cấp **không cần biết** Protocol tồn tại → ISP/DIP gần như miễn phí. Mất: không có danh sách "ai implement cái này" để IDE truy ngược |
| **Sum type** | `sealed interface` + `record` | frozen dataclass + `match` | **Java compiler BẮT LỖI khi thiếu nhánh. Python KHÔNG.** Phải tự viết `case _: raise` — nếu không, quên một loại là bug im lặng |
| **Immutable** | `record` + `Map.copyOf` | `@dataclass(frozen=True)` | `frozen` chỉ **nóng một lớp**: chặn gán lại thuộc tính, không làm dict bên trong bất biến. Vẫn phải bọc `MappingProxyType` |
| **Decorator** | class bọc class | class bọc, **hoặc** `@decorator` | Cú pháp `@` chỉ dùng được lúc *định nghĩa*; bọc *lúc chạy* (chọn theo config) thì vẫn phải viết class |
| **Enum** | `enum` có method, dùng trong `switch` | `Enum` | Java `EnumMap` nhanh hơn `HashMap` đáng kể; Python không có tương đương |
| **DI** | constructor + `final` | constructor + type hint | Cùng nguyên tắc. Java có `final` để compiler ép; Python chỉ có quy ước |
| **Async** | virtual thread (viết như blocking) | `async`/`await` (phải màu hoá hàm) | Java 21 **không cần** viết lại hàm; Python thì `async` lan ra toàn bộ call stack |
| **Timeout** | `orTimeout` — **KHÔNG huỷ** việc bên dưới | `asyncio.timeout` — **HUỶ thật** | Đoạn "1 phút sẽ hết hạn" ở Java có thể vẫn đang giữ kết nối DB sau 10 phút |
| **Song song, một cái lỗi** | `allOf` — anh em chạy tiếp | `gather` — chạy tiếp; `TaskGroup` — **huỷ hết** | Xem §6 |
| **Generic** | `<T>` | `def f[T](...)` (PEP 695) | Tương đương |
| **Bắt hết lỗi** | `catch (Exception e)` | `except Exception` — **KHÔNG** phải `except:` | `except:` trần nuốt cả `CancelledError` → task không huỷ được nữa. Java không có bẫy tương ứng |

## 2. Sáu pattern on-domain

Nguyên tắc chọn ví dụ: dùng đúng domain test automation. Khi interviewer hỏi "cho ví dụ
Observer", trả lời *"event bus cho test run, và đây là 3 bug tôi phải xử lý"* mạnh hơn hẳn
*"ví dụ như nút bấm và listener"*.

| Pattern | Bài toán Katalon | Điểm interviewer sẽ đào | Java | Python |
|---|---|---|---|---|
| **Chain of Responsibility** ⭐ | self-healing locator | vì sao không if-else; confidence; **khi nào KHÔNG được auto-update** | [`Locators.java`](../katalon-prep-java/01-clean-code-solid/src/main/java/com/prep/cleancode/patterns/locator/Locators.java) | `guardrails.decide` (cùng triết lý 3 mức) |
| **Decorator** | retry/timing/screenshot quanh 1 step | **thứ tự đổi ý nghĩa metric** | [`Executors.java`](../katalon-prep-java/01-clean-code-solid/src/main/java/com/prep/cleancode/patterns/execution/Executors.java) | `BudgetedLLM(CachingLLM(...))` |
| **Strategy + Adapter** | chọn Selenium/Appium/Playwright | vì sao `switch` vi phạm OCP | [`EngineSelection.java`](../katalon-prep-java/01-clean-code-solid/src/main/java/com/prep/cleancode/patterns/engine/EngineSelection.java) | `HealthCheck` Protocol · `LLMClient` + `AnthropicLLM` |
| **Observer** | event bus báo kết quả | 3 bug: listener ném, listener chậm, `ConcurrentModificationException` | [`TestEventBus.java`](../katalon-prep-java/01-clean-code-solid/src/main/java/com/prep/cleancode/patterns/events/TestEventBus.java) | — |
| **Template Method** | setUp → execute → tearDown | **teardown phải luôn chạy**; exception masking | [`TestLifecycle.java`](../katalon-prep-java/01-clean-code-solid/src/main/java/com/prep/cleancode/patterns/lifecycle/TestLifecycle.java) | `try/finally`, `contextmanager` |
| **Registry** | tập hợp strategy, chặn trùng | fail-fast lúc khởi tạo, không lúc chạy | `HealthCheckRegistry.java` | `HealthCheckRegistry` · `ToolRegistry` |

## 3. Case study — Strategy làm sai → làm đúng

Đây là bug **thật trong code RCI của bạn**, và là lý do module `01` đứng đầu ở cả hai ngôn ngữ.

```text
SAI: 14 strategy được inject, nhưng `switch` bên trong chỉ xử lý 2.
     12 cái còn lại rơi vào `default` và bị báo "DOWN" — trong khi sự thật là "KHÔNG BIẾT".
     Thêm strategy mới = phải sửa `switch` = vi phạm Open/Closed.
```

```text
ĐÚNG: strategy TỰ KHAI BÁO nó phục vụ cái gì (`service()` / thuộc tính `service`).
      Registry chỉ việc hỏi. Không còn `switch` nào để quên một nhánh.
      Thêm strategy mới = thêm một class, KHÔNG sửa code cũ.
```

> **Open/Closed nói bằng code, không nói bằng lời.** Test
> `test_them_service_moi_KHONG_phai_sua_dong_code_nao_cu` chứng minh: thêm một class mới,
> không sửa dòng nào, registry nhận ngay.

**UNKNOWN ≠ DOWN** là ranh giới thiết kế quan trọng nhất của case study này. Gộp UNKNOWN vào
DOWN nghĩa là một health check bị timeout sẽ báo service đã chết, và hệ thống điều phối sẽ xoay
vòng lại một service **đang khoẻ**. Sự cố lan rộng ra từ chính công cụ giám sát.

## 4. Ngưỡng tin cậy — pattern xuất hiện ở cả hai bên

| | self-healing locator (Java) | agent sinh test case (Python) |
|---|---|---|
| Tín hiệu | `stability - attemptIndex × 15` | grounding score (selector × chất lượng) |
| Ngưỡng auto | 70 | 80 |
| Dưới ngưỡng | vẫn trả element, **không** tự sửa test suite | **đề xuất** cho người duyệt |
| Vì sao | một lần tự động sửa sai là mất niềm tin vĩnh viễn | y hệt |

Nhận ra hai chỗ này là **cùng một pattern** là thứ đáng nói trong phỏng vấn: nó cho thấy bạn
nghĩ ở tầng nguyên tắc sản phẩm, không phải tầng thư viện.

## 5. Bẫy `frozen` chỉ nóng một lớp

```python
@dataclass(frozen=True)
class Health:
    details: Mapping[str, str]

h = Health({"db": "UP"})
h.details = {}          # ✅ bị chặn (FrozenInstanceError)
h.details["db"] = "X"   # ❌ KHÔNG bị chặn nếu không bọc MappingProxyType
```

Java `record` cũng vậy: `record Health(Map<String,String> details)` không copy gì cả — phải
`Map.copyOf` trong compact constructor. **Hai ngôn ngữ, cùng một bẫy, cùng một cách chữa.**

## 6. `gather` vs `TaskGroup` — và tôi đã đoán sai chỗ này

Tôi tưởng `asyncio.gather` huỷ anh em khi một task ném. **Không phải.** Chạy mới biết:

| | một task ném thì... | dùng khi nào |
|---|---|---|
| `gather(...)` | ném lỗi đầu tiên ra ngoài, **anh em VẪN CHẠY tiếp** nhưng thành **task mồ côi** — vẫn ăn CPU/kết nối, và nếu sau đó chúng cũng ném thì Python in `"Task exception was never retrieved"` | health check: ta **muốn** biết kết quả mọi service |
| `gather(..., return_exceptions=True)` | không ném, trả cả lỗi lẫn kết quả | **đúng cho health check** |
| `TaskGroup` (3.11+) | **HUỶ** tất cả anh em, gom lỗi thành `ExceptionGroup` | giao dịch nhiều bước: một bước hỏng thì làm tiếp là vô nghĩa |

`CompletableFuture.allOf` của Java gần `gather` hơn: không tự huỷ anh em.

Test: `test_gather_KHONG_huy_anh_em_ma_bo_ROI_chung___TaskGroup_thi_huy`.

## 7. Cách luyện

1. **Đọc `before/` trước, tự tìm vấn đề, rồi mới mở `after/`.** Giá trị nằm ở việc tự tìm ra.
   Java 14 vấn đề, Python 14 vấn đề — cùng lớp vấn đề, biểu hiện khác nhau.
2. **Viết lại `SelfHealingLocator` từ đầu không nhìn.** Nếu giải thích được confidence score và
   *khi nào không được auto-update* thì bạn đã nắm pattern, không phải nhớ code.
3. **Cùng một pattern, viết cả hai ngôn ngữ.** Chỗ nào Python gọn hơn, chỗ nào Python mất an
   toàn — bảng §1 là câu trả lời, nhưng phải tự viết mới thấy.
4. **Chạy rồi cố tình phá.** Bỏ `List.copyOf` → test nào đỏ? Đổi `except Exception` thành
   `except:` → test nào treo? Bỏ `return_exceptions=True` → mất thông tin gì?
