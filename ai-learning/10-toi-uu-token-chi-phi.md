# 10 · Tối ưu token & chi phí

> **Câu hỏi:** "làm sao tiết kiệm token đáng kể". Câu trả lời ngắn: **token ảnh, không phải prompt
> text.** File này chỉ ra bảy cần gạt, xếp theo ROI, với con số tính từ chính repo của bạn.
>
> **Thời lượng:** 8–10 giờ. **Yêu cầu trước:** [05](05-nlp.md)§1, [06](06-multimodal-vlm.md)§4.

---

## 1. Bức tranh chi phí — nhìn thẳng vào con số

```
   MỘT TRANG TECHPACK A4 QUÉT 300 DPI, GỬI NGUYÊN VÀO QWEN-VL

   token ảnh   ████████████████████████████████████████████  11,125
   prompt text ██████                                          ~1,500   (agent08 ≈ 4,076 cho cả thư mục)
   output      ██                                                ~500
                                                              ────────
                                                               ~13,125

   → TOKEN ẢNH CHIẾM ~85% CHI PHÍ CỦA MỘT LẦN GỌI.
```

Và một techpack thường có nhiều trang:

```
   Techpack 10 trang, gửi nguyên:  ~111,250 token ảnh
   So sánh: TOÀN BỘ prompt text của cả 12 thư mục agent ≈ 14,100 token

   → 1 techpack ≈ 8× toàn bộ prompt text của hệ thống.
```

> **Kết luận định hướng:** mọi nỗ lực rút gọn prompt text đang tranh giành **15%** chi phí.
> Cần gạt thật nằm ở 85% còn lại. Đây là lý do file này bắt đầu bằng ảnh, không phải bằng prompt.

---

## 2. Trước tiên: đo cho đúng

Bạn không thể tối ưu thứ đo sai. Hai việc, làm trong một buổi:

### 2.1 Thay ước lượng bằng số thật

```python
# vlm_clients.py đã đọc usage rồi — hãy DÙNG nó làm nguồn sự thật
input_tokens, output_tokens, total = _extract_usage_tokens(data)
```

```python
# và thay hàm ước lượng ảnh bằng công thức đúng (xem 06 §5)
def qwen_image_tokens(h, w, factor=28, max_pixels=16384*28*28): ...
```

### 2.2 Ghi sổ token — schema tối thiểu

Mỗi lời gọi LLM log đủ 8 trường. Langfuse của bạn đã bắt phần lớn, nhưng thiếu phần ảnh:

| Trường | Vì sao cần |
|---|---|
| `agent_name` | Biết agent nào ăn token |
| `model_id` | So model |
| `input_tokens` (từ `usage`) | Sự thật |
| `output_tokens` (từ `usage`) | Sự thật |
| `n_images`, `image_hw[]` | **Đang thiếu** — không có thì không truy được nguồn |
| `est_image_tokens` | Để đối chiếu với `usage` |
| `cache_hit` | Đo hiệu quả prefix cache (§6) |
| `retry_count` | Retry là token bị đốt vô ích |

**Báo cáo cần dựng:**

```
   TOKEN THEO AGENT (1 tuần)
   agent08_place_to_use  ████████████████████████  42%   ← có ảnh
   agent06_material      ██████████                18%   ← tool loop, nhiều vòng
   agent07_costing       ███████                   13%
   agent05_item_planner  ████                       8%
   ...
   RETRY (lãng phí)      ███                        6%   ← xóa được hoàn toàn
```

> **Không có biểu đồ này thì mọi tối ưu là đoán.** Dựng nó trước, mất nửa ngày, và nó sẽ nói cho
> bạn biết 3 cần gạt nào đáng kéo thay vì bảy.

---

## 3. Bảy cần gạt, xếp theo ROI

| # | Cần gạt | Mức tiết kiệm ước tính | Rủi ro | Công sức |
|---|---|---|---|---|
| **1** | **Giảm độ phân giải ảnh về ngưỡng vừa đủ** | **50–75% token ảnh** | Trung bình — phải đo điểm gãy | 1 ngày |
| **2** | **Crop theo bbox thay vì gửi cả trang** | 30–70% token ảnh | Thấp — đã có bbox từ Docling | 2 ngày |
| **3** | **Lọc trang không chứa BOM** | 20–50% số lần gọi | Trung bình — FN làm mất dữ liệu | 1 tuần |
| **4** | **Constrained decoding, xóa retry** | 100% token retry (~5–10%) | Rất thấp | 1 ngày |
| **5** | **Prefix cache cho system prompt** | 20–40% token *input* | Rất thấp | 1 ngày |
| **6** | **Định tuyến model nhỏ/lớn** | 40–70% chi phí tổng | Trung bình | 3 tuần |
| **7** | Nén schema output | 20–40% token *output* | Thấp–trung bình | 2 ngày |

**Thứ tự tôi khuyên:** 4 → 5 → 1 → 2 → 3 → 7 → 6. Bốn và năm gần như không rủi ro, làm trước để
lấy đà và lấy số.

---

## 4. ⭐ Cần gạt 1 — độ phân giải

Toàn bộ cơ sở ở [06](06-multimodal-vlm.md)§4. Ở đây là quy trình thực hiện.

### 4.1 Đường cong phải vẽ

```
   accuracy trên 20 trang golden
     │
  95%│ ●────────●────────●
     │                    ╲
  90%│                     ●         ← điểm gãy ở đâu đó quanh đây
     │                      ╲
  80%│                       ╲
     │                        ●
  70%│
     └──────┴────────┴────────┴────────┴──── token ảnh / trang
          1,364    2,772    6,204   11,125
          (×0.35)  (×0.50)  (×0.75)  (×1.0)

   CHỌN: mức thấp nhất mà accuracy CHƯA tụt khỏi CI của mức cao nhất.
```

### 4.2 Script thí nghiệm

```python
import math
from PIL import Image

SCALES = [1.0, 0.75, 0.5, 0.35, 0.25]

def qwen_image_tokens(h, w, factor=28, max_pixels=16384*28*28):
    hb = max(factor, round(h/factor)*factor)
    wb = max(factor, round(w/factor)*factor)
    if hb*wb > max_pixels:
        beta = math.sqrt(h*w/max_pixels)
        hb = math.floor(h/beta/factor)*factor
        wb = math.floor(w/beta/factor)*factor
    return (hb//factor)*(wb//factor)

results = []
for scale in SCALES:
    hits, tokens = [], 0
    for page, truth in golden_pages:               # 20 trang có ground truth
        img = Image.open(page)
        w, h = int(img.width*scale), int(img.height*scale)
        small = img.resize((w, h), Image.LANCZOS)
        tokens += qwen_image_tokens(h, w)
        out = call_vlm(small, prompt)
        hits.append(field_exact_match(out, truth))
    lo, hi = bootstrap_ci(hits, np.mean)[1:]       # xem 09 §5.1
    results.append((scale, tokens, np.mean(hits), lo, hi))

for s, t, m, lo, hi in results:
    print(f"×{s:<5} {t:>7,} token  acc={m:.3f} [{lo:.3f},{hi:.3f}]")
```

> **Cách đọc kết quả cho đúng:** đừng chọn mức cao nhất có accuracy cao nhất. Chọn mức **rẻ nhất
> mà CI của nó còn chồng lấn với CI của mức ×1.0**. Đó là định nghĩa thống kê của "không tệ hơn".

### 4.3 Lưu ý khi resize

| Việc | Đúng | Sai |
|---|---|---|
| Thuật toán | `Image.LANCZOS` | `NEAREST` (răng cưa, phá chữ nhỏ) |
| Thứ tự | Resize **rồi** encode base64 | Encode rồi nén file (không giảm token) |
| Nội dung khác nhau | **Ngưỡng khác nhau** cho trang bảng vs trang sketch | Một ngưỡng cho tất cả |
| Ảnh nhỏ sẵn | Không upscale | Upscale để "rõ hơn" — chỉ đốt token |

> Hàng thứ ba đáng làm riêng: trang **bảng đo/BOM** có chữ nhỏ, cần độ phân giải cao. Trang
> **sketch** chỉ cần đường nét, chịu được resize mạnh hơn nhiều. Đặt hai `max_pixels` khác nhau
> theo loại trang (Docling đã phân loại được) là tối ưu vừa an toàn vừa hiệu quả.

---

## 5. Cần gạt 2 & 3 — gửi ít hơn, ngay từ đầu

```
   HIỆN TẠI                              ĐỀ XUẤT
   ┌──────────────────┐                  ┌──────────────────┐
   │  TRANG A4        │                  │  TRANG A4        │
   │  ┌────────────┐  │                  │  ┌────────────┐  │
   │  │  header    │  │                  │  │  header    │  │ ← bỏ
   │  ├────────────┤  │  ─── gửi ───▶    │  ├────────────┤  │
   │  │   BẢNG     │  │    cả trang      │  │   BẢNG     │  │ ← CHỈ gửi cái này
   │  ├────────────┤  │    11,125 tok    │  ├────────────┤  │   ~3,000 tok
   │  │  sketch    │  │                  │  │  sketch    │  │ ← gửi riêng, resize khác
   │  ├────────────┤  │                  │  ├────────────┤  │
   │  │  footer    │  │                  │  │  footer    │  │ ← bỏ
   │  └────────────┘  │                  │  └────────────┘  │
   └──────────────────┘                  └──────────────────┘
```

**Bạn đã có mọi thứ cần thiết:** Docling layout-heron trả bbox cho `table`, `picture`,
`page_header`, `page_footer`. Code `_bbox_from_picture_item` đã cắt vùng `picture` rồi — chỉ cần
mở rộng ý tưởng đó sang `table` và bỏ header/footer.

**Cần gạt 3 (lọc trang)** đi xa hơn: một classifier nhẹ quyết định trang nào **không cần gọi VLM**
chút nào.

```
   10 trang ──▶ [classifier trang] ──▶ 4 trang có BOM ──▶ VLM
                    rẻ, ms                 6 trang bỏ qua
                                           → cắt 60% lời gọi
```

> **Cảnh báo quan trọng:** false negative ở đây làm **mất dữ liệu vĩnh viễn** — trang bị bỏ qua
> thì không ai biết nó chứa gì. Nên đặt ngưỡng **rất thận trọng** (ưu tiên recall gần 100%, chấp
> nhận precision thấp) và log mọi trang bị bỏ để audit được. Thà gửi thừa còn hơn mất dòng BOM.

---

## 6. Cần gạt 4 & 5 — hai thứ gần như miễn phí, làm tuần này

### 6.1 Constrained decoding — xóa sạch token retry

Chi tiết ở [05](05-nlp.md)§8.2. Tóm tắt: chuyển từ `response_format={"type":"json_object"}` sang
`guided_json` (vLLM) hoặc grammar GBNF (llama.cpp). JSON đúng schema **được đảm bảo ở tầng
sampler**, không cần retry.

```
   Hiện tại:  gọi → parse lỗi → retry → parse lỗi → retry → OK
              └──── 3× token cho 1 kết quả ────┘

   Sau:       gọi → OK (đảm bảo)
              └── 1× token ──┘
```

Nếu tỉ lệ retry hiện tại là 8%, đây là 8% token biến mất với rủi ro gần bằng không.
**Đo tỉ lệ retry hiện tại trước** — nếu nó là 0.5% thì cần gạt này không đáng ưu tiên.

### 6.2 Prefix cache — tính lại phần đầu prompt một lần

vLLM có **automatic prefix caching**; llama.cpp có KV cache reuse. Cơ chế: nếu nhiều request chia
sẻ cùng phần **đầu** prompt, phần đó chỉ tính KV một lần.

```
   ✅ ĐÚNG THỨ TỰ (cache được)           ❌ SAI THỨ TỰ (cache vô hiệu)
   ┌──────────────────────┐              ┌──────────────────────┐
   │ system prompt (cố định)│ ← cache    │ timestamp / run_id   │ ← đổi mỗi lần
   │ few-shot (cố định)     │ ← cache    │ system prompt        │   → HỎNG hết cache
   │ rulebook (cố định)     │ ← cache    │ few-shot             │     phía sau
   ├──────────────────────┤              │ input riêng          │
   │ input riêng của request│            └──────────────────────┘
   └──────────────────────┘
```

**Ba luật để cache hoạt động:**

1. **Phần cố định luôn đứng trước**, phần thay đổi đứng sau. Đúng một lần, đúng mãi.
2. **Không nhét timestamp/uuid/run_id vào đầu prompt.** Một ký tự đổi là cache chết.
3. **Bật prefix caching ở server** (`--enable-prefix-caching` với vLLM) và **đo `cache_hit`**.

> Với 12 thư mục prompt mà system prompt gần như bất biến, đây là khoản tiết kiệm input token
> đáng kể, tốn một ngày, rủi ro gần bằng không. Điều duy nhất cần kiểm: các agent của bạn có
> đang vô tình chèn gì đó thay đổi vào đầu prompt không (grep `datetime`, `uuid`, `run_id` trong
> code build prompt).

---

## 7. Cần gạt 7 — nén output

Chi tiết ở [05](05-nlp.md)§8.3. Ba mức, tăng dần cả tiết kiệm lẫn rủi ro:

| Mức | Cách | Tiết kiệm | Rủi ro |
|---|---|---|---|
| A | Rút gọn tên field (`material_code` → `mc`) | ~20% | Thấp |
| B | Bỏ field dư thừa khỏi schema (suy được ở code thì đừng bắt model trả) | ~15% | Rất thấp ⭐ |
| C | Mảng theo vị trí + header một lần | ~40% | Trung bình — model nhỏ dễ lệch cột |
| D | Chỉ trả **delta** so với base BOM, không trả cả bảng | ~60% | Trung bình — cần logic merge |

> **Mức B nên làm trước và gần như luôn đúng:** rà schema output, bất kỳ field nào code có thể tự
> tính (`row_index`, `unit` suy từ material master, `created_at`) thì **đừng bắt model sinh ra**.
> Model sinh ra chậm hơn, đắt hơn, và **sai được** — trong khi code thì không.
>
> **Mức D rất hợp với Team A** vì bạn đã có base BOM làm điểm xuất phát. Bắt model trả lại 50 dòng
> gần giống base BOM là lãng phí; bắt nó trả "dòng 12 đổi vật liệu thành X, thêm dòng Y" thì rẻ
> hơn nhiều lần.

---

## 8. Cần gạt 6 — định tuyến model

ROI cao nhất nhưng cũng công phu nhất. Kiến trúc ở [08](08-finetuning.md)§8.2.

```
   ┌──────────────────────────────────────────────────────────┐
   │ BẬC 1 · Rule / regex / lookup           chi phí ~0        │
   │   ✅ Bạn đã làm tốt (enable_llm=False mặc định)           │
   ├──────────────────────────────────────────────────────────┤
   │ BẬC 2 · Model nhỏ chuyên biệt           rẻ ~100×          │
   │   classifier, embedding + linear probe                    │
   │   ❌ Bạn chưa có bậc này ← CƠ HỘI LỚN NHẤT               │
   ├──────────────────────────────────────────────────────────┤
   │ BẬC 3 · LLM/VLM nhỏ (2B-9B)             rẻ ~5×            │
   │   🟡 Bạn có compose cho Qwen 9B nhưng mặc định dùng 27B   │
   ├──────────────────────────────────────────────────────────┤
   │ BẬC 4 · VLM lớn (27B/35B)               đắt nhất          │
   │   ✅ Đang dùng cho gần như mọi thứ ← vấn đề nằm ở đây     │
   └──────────────────────────────────────────────────────────┘
```

**Cách triển khai an toàn:**

```
   ① Chọn MỘT task, đo baseline bằng model lớn
   ② Chạy model nhỏ SONG SONG (shadow), log cả hai
   ③ Tìm ngưỡng confidence mà model nhỏ đạt chất lượng ngang model lớn
   ④ Định tuyến: dưới ngưỡng → escalate lên model lớn
   ⑤ Đo: % lưu lượng xử lý bởi model nhỏ, chi phí trung bình, accuracy tổng
```

**Task nên bắt đầu:** phân loại FRONT/BACK. Nó ở bậc 4 mà lẽ ra thuộc bậc 2 — chênh nhau khoảng
hai bậc độ lớn về chi phí. Xem [03](03-deep-learning-co-ban.md)§9.

---

## 9. Ba thứ KHÔNG tiết kiệm token — để khỏi mất thời gian

| Việc | Vì sao vô ích |
|---|---|
| Nén ảnh PNG → JPEG chất lượng thấp | Token phụ thuộc **độ phân giải**, không phụ thuộc dung lượng file. 0 token tiết kiệm |
| Chuyển ảnh sang grayscale | Y hệt — patch vẫn thế. 0 token tiết kiệm |
| Viết prompt "hãy trả lời ngắn gọn" | Ảnh hưởng nhỏ và không đảm bảo. Dùng `max_tokens` + schema chặt |
| Bỏ khoảng trắng/xuống dòng trong prompt | Tiết kiệm vài chục token. Đổi lấy prompt không đọc được. Không đáng |

---

## 10. Bảng theo dõi — dựng một lần, dùng mãi

| Chỉ số | Giá trị hiện tại | Mục tiêu | Đo ở đâu |
|---|---|---|---|
| Token/techpack (trung bình) | ___ | −60% | Langfuse, tổng theo `pipeline_run_id` |
| Token ảnh / tổng token | ___ | < 60% | Log mới ở §2.2 |
| Tỉ lệ retry | ___ | < 1% | `retry_count` |
| Prefix cache hit rate | ___ | > 70% | vLLM metrics |
| % lưu lượng xử lý bởi model nhỏ | 0% | > 50% | Router |
| Chi phí/techpack | ___ | −60% | Suy từ token × đơn giá |
| **Accuracy (canh gác)** | 89.1% | **không tụt** | Golden set + CI |

> **Hàng cuối là hàng quan trọng nhất.** Mọi tối ưu chi phí phải được kiểm chứng là **không làm
> tụt accuracy ngoài khoảng nhiễu**. Không có hàng đó, tối ưu token trở thành cách hạ chất lượng
> một cách có tổ chức.

---

## 11. Kế hoạch 30 ngày

```
  TUẦN 1  ┌─ Dựng log token đầy đủ (§2)  ──────────────┐  không tối ưu gì
          └─ Đo: token đi đâu, retry bao nhiêu         ┘  chỉ ĐO
  TUẦN 2  ┌─ Cần gạt 4: constrained decoding           ┐  rủi ro thấp
          └─ Cần gạt 5: prefix cache                   ┘  lấy số sớm
  TUẦN 3  ┌─ Cần gạt 1: đường cong resolution          ┐  ROI lớn nhất
          └─ Chọn max_pixels theo CI                   ┘
  TUẦN 4  ┌─ Cần gạt 2: crop theo bbox Docling         ┐
          └─ Báo cáo tổng: trước/sau, kèm accuracy CI  ┘

  Sau 30 ngày bạn có: một con số tiết kiệm THẬT, có bằng chứng không mất chất lượng.
  Đó vừa là kết quả công việc, vừa là một dòng CV.
```

---

## 12. Ranh giới trung thực

- **Đã tính thật:** mọi con số token ảnh (§1, §4.1) tính bằng `smart_resize` của họ Qwen-VL.
  Kích thước prompt trong §1 đo bằng `wc -c` trên thư mục `prompts/` rồi chia 4 — **bản thân
  phép chia 4 là ước lượng** và theo [05](05-nlp.md)§1.3 nó ước lượng **thấp** cho tiếng Việt.
  Nghĩa là phần "prompt text" trong biểu đồ §1 có thể lớn hơn tôi vẽ, nhưng **kết luận ảnh chiếm
  đa số vẫn đứng vững** vì chênh lệch là gần một bậc độ lớn.
- **⚠ Cảnh báo kế thừa từ [06](06-multimodal-vlm.md)§9:** công thức token ảnh là của
  Qwen2/2.5-VL. Bạn chạy Qwen3.6-VL trên llama.cpp/vLLM — **phải verify bằng `usage.prompt_tokens`
  thật** trước khi tin con số tuyệt đối. Kết luận tương đối (token ∝ diện tích, resize là cần gạt
  lớn nhất) đúng với mọi VLM họ ViT.
- **Là ước lượng, chưa đo trên hệ bạn:** toàn bộ cột "Mức tiết kiệm" ở §3. Chúng là khoảng hợp lý
  dựa trên cơ chế, **không phải kết quả đo**. Tuần 1 của §11 tồn tại để thay chúng bằng số thật.
- **Chưa kiểm chứng:** tỉ lệ retry hiện tại của bạn (tôi viết "nếu 8%" như giả định), tỉ lệ trang
  không chứa BOM, và việc backend của bạn đã bật prefix caching hay chưa.
- **Đã verify từ code:** `_extract_usage_tokens` tồn tại và đọc `usage`; `response_format` JSON
  object; 12 thư mục prompt và kích thước của chúng; có compose cho Qwen 9B lẫn 27B/35B.
