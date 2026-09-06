# AI agent — thiết kế hệ thống (không phụ thuộc ngôn ngữ)

> **Đây là tài liệu quan trọng nhất trong `common/`.** TrueTest là sản phẩm lõi và là khác biệt
> duy nhất của Katalon so với các hãng test automation khác. Mọi hãng đều có test runner;
> chỉ Katalon bán "AI tự khám phá user journey rồi sinh test case".
>
> Code chạy được: [`katalon-prep-python/src/prep/agent/`](../katalon-prep-python/src/prep/agent/) — **55 test**.
> Chạy thử: `cd katalon-prep-python && uv run python -m prep.agent.demo`

## 0. TrueTest thực sự làm gì

```text
discover  →  model   →  generate  →  maintain
   │           │           │            │
   │           │           │            └─ DOM đổi → tự chữa locator → đề xuất cập nhật
   │           │           └─ LLM sinh test case, có ràng buộc bằng schema + DOM thật
   │           └─ gom event thành session → khai phá journey phổ biến (Spark)
   └─ SDK nhúng vào web khách, gom telemetry hành vi người dùng thật
```

Bạn không cần làm được cả bốn khâu. Nhưng phải **nói được cả vòng** và **làm sâu một khâu**.
Khâu `generate` là khâu bạn đã làm, và cũng là khâu đứng giữa — nói về nó thì tự nhiên chạm
được cả khâu trước lẫn khâu sau.

## 1. Bảy cách agent chết trong sản xuất

Vòng lặp agent chỉ có 20 dòng. Thứ phân biệt đồ chơi với sản phẩm là **các điều kiện dừng**.

| Cách chết | Triệu chứng | Chặn bằng | Cái giá phải trả |
|---|---|---|---|
| Lặp vô hạn | hoá đơn tăng, không bao giờ xong | `max_steps` | cắt cả những ca thật sự cần nhiều bước |
| **Lặp một hành động** | gọi y hệt tool + tham số mãi | **phát hiện chu trình** | có thể cắt nhầm vòng lặp hợp lệ |
| Phình context | lượt sau đắt gấp bội lượt đầu | cắt kết quả tool | mất thông tin — **phải báo cho model biết đã cắt** |
| Chạy tràn hạn mức | hết tiền giữa chừng | trần token **cứng** | trả kết quả từng phần |
| Lỗi hạ tầng bị retry | đốt hạn mức vào việc vô vọng | tách `Fatal` khỏi `Retryable` | phân loại sai thì chặn nhầm lỗi tạm thời |
| Dừng khi chưa xong | trả kết quả thiếu, im lặng | kiểm tra điều kiện hoàn thành | |
| Không truy nguyên được | không biết nó đã làm gì | `trace` đầy đủ | log phình, có thể lộ dữ liệu → phải che bí mật |

**Điểm phân biệt Lead:** `max_steps` là thứ ai cũng nói. **Phát hiện chu trình** mới là thứ
cắt sớm. Đo được: một agent kẹt ở bước 2 với `max_steps=30` sẽ đốt đủ 30 lượt; phát hiện chu
trình dừng ở **bước 3** — tiết kiệm 27 lượt, mà mỗi lượt sau lại đắt hơn lượt trước vì context
dài thêm. Test chứng minh: `test_phat_hien_CHU_TRINH_cat_som_hon_max_steps_nhieu`.

### Ranh giới quan trọng nhất: lỗi nào trả về cho model, lỗi nào ném lên trên

```text
model TỰ SỬA ĐƯỢC  →  trả về dạng văn bản cho model  →  nó gọi lại đúng ở lượt sau
  · gọi tool không tồn tại (kèm gợi ý tên gần đúng)
  · thiếu tham số bắt buộc
  · tham số sai kiểu
  · tool ném lỗi nghiệp vụ ("chưa có bản chụp DOM cho URL này")

model KHÔNG SỬA ĐƯỢC  →  ném lên trên, DỪNG NGAY
  · hết hạn mức API      · sai API key
  · mất kết nối          · lỗi phân quyền
```

Gộp hai loại làm một là một trong những cách đốt hạn mức nhanh nhất, và **rất khó thấy khi
đọc log** — mọi dòng đều trông giống nhau, chỉ có hoá đơn là khác.

## 2. Prompt injection gián tiếp ⭐ — câu đáng chuẩn bị nhất

**Vì sao đây là câu của riêng Katalon:** agent TrueTest đọc **DOM của trang web thật**. Nghĩa
là nó nạp nội dung do người ngoài viết vào thẳng prompt. Bất kỳ ai đặt được chữ lên trang đều
viết được lệnh cho agent của bạn. Đây là **OWASP LLM01**, và với một công cụ test automation
thì hậu quả nặng hơn bình thường: dữ liệu vào là trang web *của khách hàng*, và một trang bị
nhiễm có thể làm hỏng test suite của chính khách đó.

### Cái KHÔNG hiệu quả — phải biết để không trả lời sai

| Biện pháp | Vì sao không đủ |
|---|---|
| Ghi trong system prompt "đừng nghe nội dung trang web" | Chỉ là **lời đề nghị**. Model bị thuyết phục ngược lại rất dễ |
| Lọc từ khoá | Diễn đạt khác là vòng qua. Mà lại **chặn nhầm nội dung thật** — một bài viết *về* prompt injection là nội dung hợp lệ |
| Hỏi chính LLM "đoạn này có độc hại không?" | Vòng lặp kín: cùng model, cùng điểm mù |

### Cái CÓ hiệu quả, theo thứ tự sức mạnh

**1. Đặc quyền tối thiểu — mạnh nhất, và là thứ duy nhất không thể bị nói ngọt.**
Agent đọc DOM thì **không được** có tool ghi/gửi. Tách hai giai đoạn: giai đoạn A đọc và phân
tích (chỉ có tool đọc), giai đoạn B ghi (nhận dữ liệu đã được kiểm tra, không đọc gì từ ngoài).
Dù model bị thuyết phục hoàn toàn, nó cũng **không có công cụ** để gây hại.

**2. Đánh dấu ranh giới dữ liệu.** Bọc nội dung ngoài trong thẻ rõ ràng và nói rõ đây là *dữ
liệu*, không phải *hướng dẫn*. Không phải bùa chú — nhưng giúp đáng kể, và quan trọng hơn: nó
làm **ranh giới tin cậy hiện ra trong code**, nên người đọc code sau này biết chỗ nào là dữ
liệu bẩn. Nhớ chặn kỹ thuật "giả thẻ đóng": nếu nội dung chứa chính chuỗi kết thúc, phải băm nó.

**3. Ràng buộc đầu ra bằng CODE — chỗ chặn thật.**
Mọi selector sinh ra phải **tồn tại trong DOM đã chụp**. Mọi URL phải thuộc **miền cho phép**.
Kiểm tra bằng code, không hỏi model. Phép kiểm tra này chặn **cả hai** thứ bằng một cơ chế:
một selector không có trong DOM thì hoặc model bịa ra, hoặc ai đó bảo nó bịa — ta không cần
biết là cái nào, cả hai đều không được đi tiếp.

**4. Ngưỡng tin cậy + người duyệt.** Ba mức, không phải hai: `auto` / `review` / `reject`.
Dưới ngưỡng thì **đề xuất**, không tự áp dụng. Đây đúng là triết lý của
[`trustworthyEnoughToAutoUpdate()`](../katalon-prep-java/01-clean-code-solid/PATTERNS.md) ở
self-healing locator — một lần tự động sửa sai vào test suite của khách là mất niềm tin vĩnh viễn.

**5. Phát hiện mẫu khả nghi → đánh dấu, KHÔNG dùng làm cổng chặn chính.**
Nhưng có một tín hiệu mạnh: **văn bản bị GIẤU** (`display:none`, `font-size:0`, `aria-hidden`).
Chữ mà người dùng không đọc được nhưng model vẫn đọc được thì gần như chắc chắn là cố ý —
nội dung thật hiếm khi phải giấu. Nên: ẩn → từ chối; không ẩn → chỉ đánh dấu.

### Câu trả lời gọn khi được hỏi

> *"Sản phẩm đọc DOM của khách, nên nội dung do người ngoài viết đi thẳng vào prompt — đó là
> prompt injection gián tiếp. Tôi không chặn bằng system prompt hay lọc từ khoá, vì cả hai đều
> vòng qua được. Tôi chặn bằng bốn lớp: đặc quyền tối thiểu — agent đọc DOM không có tool ghi;
> đánh dấu ranh giới dữ liệu; ràng buộc đầu ra bằng code — mọi selector phải có thật trong DOM,
> mọi URL phải thuộc miền cho phép; và ngưỡng tin cậy ba mức, dưới ngưỡng thì đề xuất chứ không
> tự áp dụng. Tôi có demo chạy được: model **đã nghe theo** lệnh ẩn, nhưng không bước nào tới
> được test suite."*

## 3. Test hệ thống không tất định — bốn tầng

Câu hỏi gần như chắc chắn sẽ có. Cái làm nên mức Lead là biết **tầng nào dùng cho việc gì**.

| Tầng | Dùng cho | Tần suất | Cái giá |
|---|---|---|---|
| **1. Unit test tất định** (LLM giả) | code quanh model: vòng lặp, dispatch, validate, guardrail | mỗi commit | không đo được chất lượng model |
| **2. Test tính chất** | bất biến đúng với **mọi** đầu ra: "selector phải có thật" | mỗi commit + với model thật | không bắt được lỗi ngữ nghĩa |
| **3. Bộ vàng** | N ca kèm đầu ra mong đợi, chấm có phân bậc | hàng ngày / trước release | tốn công duy trì, dễ lỗi thời |
| **4. LLM làm giám khảo** | thứ không chấm máy móc được | thưa, có lấy mẫu | yếu nhất, đắt nhất, **thiên vị** |

**Tầng 1 là tầng lớn nhất và là tầng hầu hết bị bỏ qua** — người ta tưởng "có LLM thì không
test được". Sai. 122 test ở `katalon-prep-python` chạy **offline trong 4 giây**, không cần API key.

Ba điều ít người nói:

- **Bộ vàng phải có ca ÂM.** `must_not_contain`. Toàn ca đẹp thì điểm cao mà hệ thống vẫn vỡ ở
  sản xuất. Một agent đúng 95% nhưng 5% còn lại gửi dữ liệu ra ngoài thì vẫn không dùng được.
- **Cổng CI đúng là "không được TỤT so với lần trước"**, không phải "pass ≥ 90%". Tỉ lệ tổng có
  thể **tăng** trong khi đúng những ca quan trọng nhất đã vỡ. So sánh từng ca mới thấy.
- **Giám khảo phải được kiểm định trước khi tin.** Đo độ khớp với nhãn của *người*. Dưới ~0.8
  thì điểm của nó không dùng để ra quyết định được. Và trong hai loại sai, **dương giả nguy hiểm
  hơn**: cho qua một đầu ra hỏng nghĩa là hỏng lọt tới khách.
- **`pass@1` khác `pass@k`.** Chạy một lần rồi kết luận là sai lầm đo lường phổ biến nhất. Một
  hệ thống đạt 1/5 lần **không phải** là hệ thống đạt.

## 4. Đầu ra có cấu trúc — ba tầng

*"Làm sao đảm bảo LLM trả về đúng định dạng?"* Trả lời "tôi ghi trong prompt" là trả lời sai.

1. **Tool/schema** — ràng buộc nằm ở tầng giải mã, không phải ở lời đề nghị. Mạnh nhất.
2. **Validate** — schema ép được **kiểu**, không ép được **ý nghĩa**. "Selector phải tồn tại
   trong DOM" thì không schema nào bắt được.
3. **Sửa-và-thử-lại có trần** — đưa **chính thông báo lỗi** cho model. Tỉ lệ thành công ở lần 2
   rất cao **nếu thông báo lỗi đủ cụ thể**.

Tầng thứ tư ít người nói: **ghi nhận tỉ lệ hỏng**. Nếu 30% phản hồi phải sửa thì vấn đề ở
**prompt/schema**, không ở model — và đổi model sẽ không cứu được.

> **Thông báo lỗi là một phần của giao diện với model.** Viết cho *model* đọc, không phải cho
> log. `"không hợp lệ"` → model đoán mò. `"selector '#foo' không có trong DOM, hãy dùng một
> trong: #bar, #baz"` → model sửa được ngay.

## 5. RAG — vì sao domain này cần hybrid

Katalon dùng **pgvector trên Aurora PostgreSQL** (có trong JD).

Trả lời "embedding rồi cosine" là câu trả lời của người đọc tutorial. Đây là câu trả lời của
người đã làm — **đo được**, số thật từ `test_09_rag.py`:

| truy vấn | tìm bằng từ khoá | tìm bằng vector |
|---|---|---|
| `#cancel-btn` | ✅ đúng | ✅ đúng — **nhưng kéo theo `#submit-btn` ở 72% điểm** |
| `#submit_btn` (biến thể `_`) | ❌ **rỗng hoàn toàn** | ✅ đúng |
| `#submitBtn` (camelCase) | ❌ **rỗng hoàn toàn** | ✅ đúng |

**Hai phương pháp hỏng ngược chiều nhau.** Từ khoá chính xác tuyệt đối khi khớp, nhưng một biến
thể chính tả làm nó trả về *không có gì* — không phải "kém hơn", mà là **rỗng**. Vector chịu
được biến thể nhưng xếp `#submit-btn` sát `#cancel-btn` — hai thứ **ngược nghĩa** (một cái gửi
form, một cái huỷ). Trong corpus thật hàng chục ngàn đoạn thì rác đó lấp kín top-k.

Cơ chế: tokenizer thật cắt `#submit-btn` → `[sub, mit, btn]` và `#cancel-btn` → `[can, cel, btn]`.
Chúng dùng chung mảnh `btn` nên **không bao giờ trực giao**. Đo được: cosine = **0.333**.

Mà domain của Katalon thì đầy định danh: selector, tên test, mã lỗi, tên API.

**Cần hybrid không phải vì "hybrid tốt hơn" mà vì hai điểm mù không chồng lên nhau.**

Ba chi tiết nữa:
- **Gộp bằng RRF (thứ hạng), không cộng thẳng điểm.** Cosine ∈ [-1,1], TF-IDF không có trần.
  Cộng thẳng thì thành phần có thang đo lớn hơn nuốt thành phần kia — và nó đổi theo dữ liệu,
  nên hôm nay đúng mai lại sai.
- **Cắt chunk theo ranh giới thẻ, không theo số ký tự.** Cắt mù xẻ đôi `<button id="sub`|`mit-btn">`
  → selector biến mất khỏi *cả hai* mảnh.
- **Đo `recall@k` tách riêng khỏi khâu sinh.** `recall@k = 0.4` nghĩa là 60% câu hỏi không bao
  giờ có cơ hội trả lời đúng — tối ưu prompt lúc đó là vô ích.

Bẫy pgvector cần biết: **index HNSW/IVFFlat là GẦN ĐÚNG.** Nó có thể bỏ sót bản ghi đúng nhất
để đổi lấy tốc độ. `EXPLAIN` vẫn là công cụ kiểm tra — y hệt
[module 05](../katalon-prep-java/05-postgres-depth/).

## 6. Chi phí — thứ quyết định agent có lên được sản xuất không

Chi phí = **số vòng lặp × kích thước context**. Cả hai đều tăng âm thầm.

| Đòn bẩy | Hiệu quả | Bẫy |
|---|---|---|
| **Prompt caching** | lớn nhất | Dán timestamp/uuid vào system prompt "cho dễ debug" → **tỉ lệ trúng cache về 0**, hoá đơn tăng nhiều lần, hoàn toàn vô hình trong code |
| Cắt kết quả tool | ngăn phình theo cấp số | Cắt **im lặng** còn tệ hơn không cắt — model tưởng đã đủ dữ liệu rồi kết luận sai |
| Định tuyến model | lớn | Thêm một điểm hỏng; phải eval riêng từng nhánh |
| Phát hiện chu trình | lớn khi có sự cố | Có thể cắt nhầm vòng lặp hợp lệ |
| Giảm số tool | vừa | Mỗi schema tool nằm trong **mọi** lượt gọi |

Bọc theo lớp, và **thứ tự có ý nghĩa**:

```text
BudgetedLLM( CachingLLM( AnthropicLLM() ) )   → lần trúng cache KHÔNG tính vào trần
CachingLLM( BudgetedLLM( AnthropicLLM() ) )   → có tính
```

Đúng pattern Decorator ở [module 01](../katalon-prep-java/01-clean-code-solid/PATTERNS.md), và
cũng đúng bài học đó: **thứ tự decorator đổi ý nghĩa của phép đo**. Không có đáp án đúng tuyệt
đối — nhưng phải biết mình đang chọn cái nào và vì sao.

## 7. Grounding là cổng nhân, không phải điểm cộng

Đây là lỗi tôi thật sự mắc khi xây `grounding_score`, và chỉ **chạy** mới lộ ra.

Thang điểm đầu tiên cộng dồn: 50 điểm selector + 30 điểm journey + 20 điểm assertion. Kết quả:

> một test case có selector **bịa hoàn toàn** vẫn được **50/100** — vì nó dùng tên hành động
> hợp lệ và có một bước assert. 50 điểm cho một thứ không thể chạy được, và nó **lọt qua ngưỡng
> review (40)** để tới mặt người duyệt.

Công thức đúng — tỉ lệ selector có thật là **hệ số nhân**:

```text
điểm = 100 × tỉ_lệ_selector_thật × (0.5 + 0.3×bám_journey + 0.2×có_assert)
```

**Bài học chung, dùng cho mọi hệ thống chấm điểm:** điều kiện **CẦN** phải nhân, điều kiện
**TỐT** mới được cộng. Trộn hai loại vào một phép cộng là cách dễ nhất để tạo ra một con điểm
trông hợp lý mà hoàn toàn vô dụng.

Và một quyết định thiết kế nữa: **chấm điểm bằng CODE, không hỏi LLM.** Lấy chính model tự chấm
điểm mình là một vòng lặp kín — nó tự tin nhất đúng lúc nó sai nhất.

## 8. Bốn câu hỏi và câu trả lời gọn

**"Vì sao dùng interface cho LLM client?"**
> Không phải để tuân thủ SOLID. Đo được bằng giây: 122 test chạy không cần API key, xong trong
> 4 giây; đổi nhà cung cấp = viết một adapter mới, không sửa một dòng logic nào; người khác
> clone repo về là chạy được ngay.

**"Agent của bạn khác một script gọi LLM ở chỗ nào?"**
> Ở các điều kiện dừng và ở guardrail. Vòng lặp thì 20 dòng — ai viết cũng được. Phần còn lại
> là bảy cách nó chết trong sản xuất và cách chặn từng cái.

**"Bạn tin đầu ra của LLM đến mức nào?"**
> Không tin. Tôi kiểm tra bằng code: selector phải có trong DOM, URL phải thuộc miền cho phép,
> và có ngưỡng ba mức — dưới ngưỡng thì đề xuất chứ không tự áp dụng.

**"Nếu tôi cho bạn xây TrueTest từ đầu?"**
> Bốn khâu: discover (SDK telemetry) → model (sessionization bằng Spark) → generate (agent có
> guardrail) → maintain (self-healing locator). Tôi sẽ bắt đầu từ `maintain` chứ không phải
> `generate` — vì locator vỡ là nguyên nhân số 1 làm test automation bị bỏ hoang, nên nó là chỗ
> tạo giá trị nhanh nhất, và nó không cần LLM nên rủi ro thấp hơn hẳn.

## Đối chiếu code

| Chủ đề | Python | Java |
|---|---|---|
| Vòng lặp + điều kiện dừng | [`agent/loop.py`](../katalon-prep-python/src/prep/agent/loop.py) | — |
| Guardrail / injection | [`agent/guardrails.py`](../katalon-prep-python/src/prep/agent/guardrails.py) | — |
| RAG hybrid | [`agent/rag.py`](../katalon-prep-python/src/prep/agent/rag.py) | — |
| Eval | [`agent/evals.py`](../katalon-prep-python/src/prep/agent/evals.py) | — |
| Ngưỡng tin cậy 3 mức | `guardrails.decide` | [`Locators.java`](../katalon-prep-java/01-clean-code-solid/src/main/java/com/prep/cleancode/patterns/locator/Locators.java) |
| Decorator xếp lớp | `BudgetedLLM(CachingLLM(...))` | [`Executors.java`](../katalon-prep-java/01-clean-code-solid/src/main/java/com/prep/cleancode/patterns/execution/Executors.java) |
| Retry / circuit breaker | [`distributed/resilience.py`](../katalon-prep-python/src/prep/distributed/resilience.py) | [`06-distributed-resilience`](../katalon-prep-java/06-distributed-resilience/) |
