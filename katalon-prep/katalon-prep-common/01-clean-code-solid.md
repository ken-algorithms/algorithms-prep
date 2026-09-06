# Clean code & SOLID — chung cho cả hai ngôn ngữ

> Code chạy được: [Java `01-clean-code-solid/`](../katalon-prep-java/01-clean-code-solid/) (61 test)
> · [Python `clean_code/`](../katalon-prep-python/src/prep/clean_code/) (14 test)
> Cả hai dùng **cùng một bài**: health aggregator, distilled từ code RCI thật của bạn.

## 1. SOLID — chỗ hai ngôn ngữ khác nhau

| | Nguyên tắc | Java 21 | Python 3.12 |
|---|---|---|---|
| **S** | Một class một lý do để đổi | như nhau | như nhau |
| **O** | Mở để mở rộng, đóng để sửa | strategy tự khai báo `service()`, registry hỏi | y hệt, nhưng không cần `implements` |
| **L** | Thay thế được cho nhau | ép bởi compiler | **không ép** — chỉ mypy bắt được |
| **I** | Interface hẹp | phải tạo interface + sửa class gốc | **`Protocol` đặt ngay tại package của người dùng** — người cung cấp không cần biết |
| **D** | Phụ thuộc vào abstraction | interface + constructor injection | Protocol + constructor injection |

**Điểm đáng nói khi phỏng vấn:** ISP và DIP ở Python gần như **miễn phí** nhờ structural typing.
Bạn định nghĩa Protocol *hẹp* ngay tại chỗ dùng nó, không phải sửa class gốc. Đó là lý do module
Python không cần một interface `HealthCheck` khổng lồ.

Nhưng phải nói được **cái mất**: không còn danh sách "ai implement cái này" để IDE truy ngược,
và `isinstance` với `@runtime_checkable` **chỉ kiểm tra tên method có tồn tại** — không kiểm tra
chữ ký hàm. Test `test_runtime_checkable_CHI_kiem_tra_ten_method_khong_kiem_tra_chu_ky` chứng minh:
một class có `duration(self, unit, scale) -> str` vẫn được `isinstance` trả `True`.

## 2. Bốn ranh giới thiết kế đáng nói nhất

Đây là những chỗ interviewer mức Lead sẽ đào, và cả bốn đều xuất hiện trong cùng một class nhỏ.

### 2.1 UNKNOWN ≠ DOWN

Ba trạng thái, không phải hai. Gộp UNKNOWN vào DOWN nghĩa là một health check bị timeout báo
service đã chết → hệ thống điều phối xoay vòng lại một service **đang khoẻ** → **sự cố lan rộng
ra từ chính công cụ giám sát**.

Và thứ tự ưu tiên khi gộp là một **quyết định sản phẩm**: bất kỳ DOWN → DOWN; còn lại có UNKNOWN
→ UNKNOWN. DOWN thắng UNKNOWN vì một sự cố *đã chắc chắn* quan trọng hơn một điều *chưa rõ*.

### 2.2 Fail-fast lúc khởi tạo, không lúc chạy

Hai check cùng khai báo `service == "db"` là **lỗi cấu hình**. Phát hiện lúc start ≫ phát hiện
lúc một trong hai bị ghi đè âm thầm ở production.

Cùng triết lý ở `ToolRegistry` bên agent: tool trùng tên → ném ngay lúc `register`.

### 2.3 Chia 0 → `0.0`, không phải `NaN`, không phải ném

`NaN` **lan truyền im lặng** qua mọi phép tính phía sau và làm vỡ mọi dashboard — mà rất khó
truy nguyên vì nó không để lại dấu vết. Ném thì buộc mọi caller phải `try/except` cho một trường
hợp hoàn toàn bình thường (chưa có test nào chạy).

### 2.4 Không có dữ liệu ≠ dữ liệu bằng 0

`Skipped` không có duration → trả `None`, **không** trả `Duration.ZERO` / `timedelta(0)`. Số 0 là
một lời nói dối: nó kéo trung bình xuống và làm p95 sai.

> Bẫy Java kèm theo: `Duration::ZERO` **không** phải method reference hợp lệ — `ZERO` là *field*.
> Phải viết `.orElse(Duration.ZERO)`.

## 3. Mười bốn vấn đề trong `before/` — và số hiệu rule tương ứng

Cùng danh sách cho cả hai ngôn ngữ. **Tự tìm trước khi mở `after/`.** Mục tiêu ≥ 10/14.

| # | Vấn đề | SonarQube (Java) | ruff (Python) |
|---|---|---|---|
| 1 | Trạng thái toàn cục / field injection | — | — |
| 2 | Strategy inject N cái, `switch` xử lý 2 | — | — |
| 3 | Quá nhiều tham số, có cờ boolean | `S107` | `PLR0913` |
| 4 | Dispatch bằng `if/elif` trên tên | — | — |
| 5 | Nhánh `default` báo DOWN sai | — | — |
| 6 | Không có timeout | — | — |
| 7 | Song song không giữ kết quả cái khoẻ | — | — |
| 8 | **Biến chết** | `S1481` + `S1854` | `F841` |
| 9 | Bắt hết lỗi rồi nuốt | `S2221` | `E722` / `BLE001` |
| 10 | `print` thay vì logging | `S106` | `T201` |
| 11 | Chuỗi lặp lại thay vì hằng số | `S1192` | — |
| 12 | Trả về collection nội bộ (không copy) | `S2384` | — |
| 13 | Chia 0 / magic number | — | `PLR2004` |
| 14 | `random`/clock không tiêm được → test không tất định | — | — |

> **SonarLint trong IDE của bạn đang chạy live** — nó đã tự flag `S1481`, `S1854`, `S106`,
> `S1192` trong `before/` Java, và `S5778`/`S5863`/`S5958` trong test Python (tôi đã sửa hai
> cái sau, xem §5). **Đèn đỏ ở `before/` là đúng mục đích**; phải sạch ở `after/`.
>
> Python thì `ruff` được cấu hình `per-file-ignores` cho `before/` — trừ `E722`, vì nó là vấn
> đề [9] và phải có `noqa` **ngay tại dòng** để người đọc thấy đó là cố ý.

## 4. Hai bug che lấp nhau — bằng chứng vì sao phải CHẠY chứ không chỉ ĐỌC

Từ chính `HealthCheckContextStrategy.java` trong RCI. Tôi dựng lại rồi chạy thật, và **kết quả
khác hẳn những gì đọc code sẽ đoán — tôi cũng đoán sai**:

```text
Dòng "biến chết" doneJobs gọi join() TRƯỚC getSafe().
join() trên future đã failed thì NÉM
   → một service phụ lỗi làm CẢ endpoint health trả 500.
   → getSafe() — đoạn duy nhất viết RA ĐỂ xử lý lỗi — KHÔNG BAO GIỜ chạy tới. Dead code.

Xoá dòng biến chết (SonarLint đang đòi: S1481 + S1854):
   → không ném nữa, getSafe nuốt lỗi
   → response thành RỖNG IM LẶNG, dashboard vẫn xanh.
```

**"Dọn dẹp vô hại" biến sự cố *dễ thấy* thành sự cố *không ai thấy*.** Cả hai hành vi đều có test
chứng minh: `BeforeFailureModesTest` case `[3a]` và `[3b]`.

Đây **không phải** lỗi của AI gen code. Là lỗi của **design đúng ở tầng cao nhưng không review ở
tầng dòng code** — AI chỉ làm nó xảy ra nhanh hơn.

## 5. Bảy lần tôi đoán sai trong khi xây workspace này

Đây là dữ liệu thật, và là bằng chứng cho nguyên tắc *"luôn CHẠY, không chỉ ĐỌC"*.

| Chỗ | Tôi tưởng | Sự thật |
|---|---|---|
| `getSafe()` | lỗi bị nuốt → response rỗng | `join()` ném trước → **500**, `getSafe` là dead code |
| `Set.of(a, b)` trùng | dedupe | **ném** `IllegalArgumentException` |
| dataset bin-packing | chứng minh được | chỉ 1.02× → phải thiết kế lại → **3.81×** |
| `asyncio.gather` | huỷ anh em khi một task ném | **bỏ rơi** chúng; `TaskGroup` mới huỷ |
| `sum([0.1]*10)` | `!= 1.0` | **đúng bằng** 1.0 — sai số triệt tiêu nhau |
| salting hot key | thêm salt là xong | salt 1× partition gần **vô dụng**; 2× còn **tệ hơn** 1×; phải ≥ 16× |
| `grounding_score` cộng dồn | hợp lý | test **bịa hoàn toàn** được **50/100**, lọt qua ngưỡng review |

Ba lần trong số đó chỉ là số liệu yếu, **bốn lần là kết luận SAI HẲN** mà đọc code không thấy được.

**Cách trả lời khi được hỏi về việc dùng AI gen code** (họ sẽ hỏi — JD Lead ghi rõ *"Experience
with GitHub Copilot, Cursor, or Claude Code"*):

> *"Tôi design trước, viết implementation plan, rồi dùng Claude Code để implement. Nhưng tôi
> review từng dòng và không để AI quyết định kiến trúc — AI không biết trade-off của hệ thống
> mình. Tôi có gate rõ: test phải xanh, và tôi phải giải thích được từng quyết định. Ví dụ cụ
> thể: gần đây tôi dựng lại một class health-check aggregator của mình để review, và khi chạy
> test mới phát hiện hai bug đang che lấp nhau — xoá dead code sẽ biến lỗi 500 ồn ào thành
> response rỗng im lặng. Đọc code không thấy được, phải chạy mới thấy. Từ đó tôi đặt quy tắc:
> refactor thì viết test trước."*

## 6. Bốn quy tắc từ giờ tới lúc phỏng vấn

1. **Viết tay đúng những gì sẽ bị bắt viết tay.** DSA, idiom core → **tắt Copilot**. Module `00`, `07`.
2. **Dùng AI đúng những gì ở công ty cũng sẽ dùng AI.** Scaffolding, test, refactor — nhưng **đọc 100% output**.
3. **Luôn CHẠY code, không chỉ đọc.** Bảng §5 là bằng chứng.
4. **Với mỗi PR, tự hỏi "cái này vỡ ở đâu?"** Không trả lời được thì chưa review xong.

## 7. Bài tập

1. Mở `before/` (Java rồi Python), tự tìm 14 vấn đề. Ghi ra giấy trước khi mở `after/`.
2. Viết lại `HealthAggregator` từ đầu ở ngôn ngữ **còn lại** (bạn đọc Java thì viết Python).
   Chỗ nào phải quyết định khác đi? Đó là chỗ hai ngôn ngữ thật sự khác nhau.
3. **Mở `HealthCheckContextStrategy.java` thật trong RCI và refactor nó.** Viết test trước.
   Đây là PR thật → thành story mang đi phỏng vấn.
4. Chạy rồi phá: bỏ `List.copyOf` → test nào đỏ? bỏ `orTimeout` → test nào treo?
   bỏ hệ số nhân trong `grounding_score` → test bịa được bao nhiêu điểm?
