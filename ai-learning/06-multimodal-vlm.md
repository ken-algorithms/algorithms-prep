# 06 · Multimodal & VLM

> **File quan trọng nhất cho công việc hiện tại của bạn.** Nó trả lời hai câu:
> (1) vì sao FashionCLIP cho similarity kém trên sketch, và (2) **token ảnh của bạn đang đi đâu** —
> với con số cụ thể cho trang techpack A4.
>
> **Thời lượng:** 12–15 giờ. **Yêu cầu trước:** [04](04-computer-vision.md), [05](05-nlp.md).

---

## 0. Hai họ model multimodal — đừng nhầm

```
  ┌──────────────────────────────────────┐   ┌──────────────────────────────────────┐
  │  A · CONTRASTIVE ENCODER             │   │  B · VISION-LANGUAGE MODEL (VLM)     │
  │     CLIP, SigLIP, FashionCLIP        │   │     Qwen-VL, LLaVA, GPT-4V           │
  ├──────────────────────────────────────┤   ├──────────────────────────────────────┤
  │  ảnh ──▶ vector                      │   │  ảnh + text ──▶ TEXT                 │
  │  text ─▶ vector (cùng không gian)    │   │                                      │
  │                                      │   │  ViT encoder → projector → LLM       │
  │  Nhiệm vụ: ĐO GIỐNG NHAU            │   │  Nhiệm vụ: ĐỌC HIỂU & SINH           │
  │  Kích thước: 100–400M                │   │  Kích thước: 2B–100B+                │
  │  Chi phí: rẻ, ms                     │   │  Chi phí: đắt, giây                  │
  │                                      │   │                                      │
  │  Bạn dùng: FashionCLIP 512-dim       │   │  Bạn dùng: Qwen3.6-VL 27B Q6         │
  │            (chỉ image tower)         │   │            (extraction + so sánh)    │
  └──────────────────────────────────────┘   └──────────────────────────────────────┘
```

**Cả hai đều "hiểu ảnh", nhưng theo hai nghĩa hoàn toàn khác nhau.** Nhầm lẫn hai họ này là nguồn
của rất nhiều quyết định kiến trúc sai.

---

## 1. CLIP — contrastive learning giữa ảnh và text

### 1.1 Ý tưởng train

```
  Batch N=4 cặp (ảnh, caption)

              caption₁  caption₂  caption₃  caption₄
    ảnh₁    [   ✓        ✗        ✗        ✗    ]     ✓ = positive (đường chéo)
    ảnh₂    [   ✗        ✓        ✗        ✗    ]     ✗ = negative
    ảnh₃    [   ✗        ✗        ✓        ✗    ]
    ảnh₄    [   ✗        ✗        ✗        ✓    ]

  Loss = cross-entropy theo HÀNG + cross-entropy theo CỘT
  → kéo đường chéo lên, đẩy mọi ô khác xuống
```

Đây là **InfoNCE**. Batch càng lớn → càng nhiều negative miễn phí → chất lượng càng tốt.
CLIP gốc train với batch **32,768**. Đây là lý do bạn không thể "fine-tune CLIP trên laptop" một
cách nghiêm túc — batch nhỏ thì loss gần như không có tín hiệu. → [08](08-finetuning.md)§6

### 1.2 Code InfoNCE — ngắn đến bất ngờ

```python
import torch, torch.nn.functional as F

def clip_loss(img_emb, txt_emb, temperature=0.07):
    img_emb = F.normalize(img_emb, dim=-1)
    txt_emb = F.normalize(txt_emb, dim=-1)
    logits  = img_emb @ txt_emb.T / temperature       # (N, N)
    labels  = torch.arange(len(logits), device=logits.device)
    return (F.cross_entropy(logits, labels) +         # theo hàng
            F.cross_entropy(logits.T, labels)) / 2    # theo cột
```

`temperature=0.07` (học được trong CLIP thật) làm sắc phân bố — xem [01](01-nen-tang-toan.md)§4.2.
Toàn bộ "phép màu" của CLIP nằm trong 6 dòng này cộng với **400 triệu cặp ảnh-caption**.

### 1.3 Hệ quả: CLIP học được gì và KHÔNG học được gì

| Học tốt | Học kém |
|---|---|
| Khái niệm ngữ nghĩa xuất hiện trong caption ("váy đỏ", "áo len") | **Chi tiết không ai viết trong caption** ("túi welt vs túi patch", "xẻ tà đơn vs đôi") |
| Phong cách, bố cục tổng thể | Đếm số lượng ("3 nút" vs "2 nút") |
| Loại vật thể | Quan hệ không gian chính xác |

> **Đây là lý do gốc rễ** vì sao FashionCLIP không phân biệt được các style jacket gần giống nhau
> của bạn: **không có caption nào trên internet mô tả "jacket 2 nút notch lapel có xẻ tà đôi"**.
> Model chưa bao giờ được dạy chiều thông tin đó. Không phải lỗi cấu hình, không phải lỗi
> threshold — **thông tin đó không tồn tại trong embedding**.
>
> Fine-tune không "sửa" model; nó **dạy thêm một chiều mới** vào không gian. Đó là lý do fine-tune
> là hướng đi đúng, chứ không phải tinh chỉnh trọng số. → [08](08-finetuning.md)

---

## 2. Domain gap — đo nó, đừng đoán

### 2.1 Ba tầng lệch trong trường hợp của bạn

```
  Tầng 1: LỆCH PHONG CÁCH ẢNH
     train: ảnh chụp, có màu, texture vải, ánh sáng, nền, đôi khi có người mẫu
     dùng:  line-art đen trắng, nét vector, nền trắng tinh
     → Kích hoạt của lớp đầu ViT (vốn dò cạnh/texture) khác hẳn phân bố lúc train

  Tầng 2: LỆCH NGỮ NGHĨA
     train: "áo khoác nam màu xanh navy"        ← mức loại sản phẩm
     cần:   "notch lapel, 2 nút, túi welt"      ← mức chi tiết cấu tạo
     → Chiều thông tin cần thiết chưa từng được giám sát

  Tầng 3: LỆCH PHÂN BỐ HẸP
     train: toàn bộ thời trang (váy, giày, túi, áo...)
     dùng:  gần như chỉ jacket cùng một hãng
     → Mọi ảnh của bạn dồn vào một vùng RẤT nhỏ của không gian
       → cosine giữa mọi cặp đều cao (0.8-0.95) và gần như vô dụng để xếp hạng
```

### 2.2 Thí nghiệm 3 giờ để đo — làm trước mọi thứ khác

```python
import numpy as np, matplotlib.pyplot as plt

# emb: (N, 512) từ Qdrant · style_id: (N,) nhãn style thật
E = emb / np.linalg.norm(emb, axis=1, keepdims=True)
S = E @ E.T

iu = np.triu_indices(len(E), k=1)
same = S[iu][style_id[iu[0]] == style_id[iu[1]]]
diff = S[iu][style_id[iu[0]] != style_id[iu[1]]]

print(f"cùng style : mean={same.mean():.3f}  std={same.std():.3f}  n={len(same)}")
print(f"khác style : mean={diff.mean():.3f}  std={diff.std():.3f}  n={len(diff)}")

# Chỉ số quan trọng nhất: separation (dạng Cohen's d)
d = (same.mean() - diff.mean()) / np.sqrt((same.var() + diff.var()) / 2)
print(f"separation (Cohen's d) = {d:.2f}")

plt.hist(diff, bins=50, alpha=.5, density=True, label="khác style")
plt.hist(same, bins=50, alpha=.5, density=True, label="cùng style")
plt.legend(); plt.xlabel("cosine"); plt.savefig("separation.png", dpi=120)
```

**Bảng đọc kết quả:**

| Cohen's d | Nghĩa là | Việc cần làm |
|---|---|---|
| **< 0.5** | Embedding gần như vô dụng cho bài này | **Fine-tune là bắt buộc.** Mọi tinh chỉnh trọng số/threshold đều lãng phí |
| **0.5 – 1.0** | Có tín hiệu nhưng yếu | Fine-tune cho lợi ích lớn. Rerank giúp được phần nào |
| **1.0 – 2.0** | Khá | Tối ưu retrieval + rerank trước, fine-tune sau |
| **> 2.0** | Tốt | Embedding không phải nút thắt. Tìm vấn đề ở chỗ khác |

> **Đây là thí nghiệm rẻ nhất, quyết định nhất, và bạn chưa làm.** Nó tốn 3 giờ và nó bảo bạn
> có nên tốn 3 tháng cho fine-tune hay không. Nếu chỉ làm **một** việc sau khi đọc bộ tài liệu
> này, hãy làm việc này.

---

## 3. Kiến trúc VLM — bên trong Qwen-VL

```
   ẢNH                                    TEXT
    │                                      │
    ▼                                      ▼
  ┌─────────────────┐              ┌──────────────┐
  │ ViT encoder     │              │  Tokenizer   │
  │ patch 14×14     │              │  (BPE)       │
  └────────┬────────┘              └──────┬───────┘
           │ N_patch vector               │ M token id
           ▼                              │
  ┌─────────────────┐                     │
  │ Merger / MLP    │  gộp 2×2 patch      │
  │ projector       │  → 1 token          │
  └────────┬────────┘  (giảm 4× token)    │
           │ N_patch/4 "image token"      │
           │  đã ở CÙNG không gian        │
           │  với text embedding          │
           └──────────┬───────────────────┘
                      ▼
           ┌──────────────────────────────┐
           │   LLM decoder (Transformer)  │  ← image token và text token
           │   [img][img]...[img] [text]  │    nằm chung một chuỗi,
           └──────────────┬───────────────┘    LLM không phân biệt
                          ▼
                      TEXT output
```

**Điểm mấu chốt cần hiểu:** với LLM, ảnh **chỉ là một dãy token** giống hệt text. Nó nằm trong
cùng context window, tính cùng một attention O(n²), và **tốn tiền y hệt**.

---

## 4. Token ảnh — phép tính quyết định hóa đơn của bạn

### 4.1 Công thức của họ Qwen-VL

```
   ① smart_resize: làm tròn H, W về bội của 28  (= patch 14 × merge 2)
   ② token_ảnh = (H_resize / 28) × (W_resize / 28)
```

(Ảnh còn bị kẹp lại nếu vượt `max_pixels`, mặc định ~12.8M pixel.)

### 4.2 Bảng tính — đã chạy thật

```
         gốc     sau smart_resize   token ảnh
     224x224            224x224          64
     448x448            448x448         256
    1024x768           1036x756         999
   1024x1024          1036x1036       1,369
   1568x1568          1568x1568       3,136
   1536x2048          1540x2044       4,015
   2480x3508          2492x3500      11,125   ◀── TRANG A4 QUÉT 300 DPI
```

### 4.3 Con số bạn cần nhìn thẳng

```
   MỘT TRANG TECHPACK A4 QUÉT 300 DPI = 11,125 TOKEN ẢNH

   So sánh với prompt text lớn nhất của bạn:
   agent07_costing     ≈ 4,136 token
   agent08_place_to_use≈ 4,076 token
                         ─────────
   Tổng 12 thư mục prompt ≈ 14,100 token

   → MỘT trang ảnh ≈ 79% toàn bộ prompt text của TẤT CẢ 12 agent cộng lại.
     Techpack 10 trang → 111,250 token ảnh, gấp ~8× toàn bộ prompt text.
```

### 4.4 Và đây là cần gạt

```
   Trang A4 300 dpi, giảm độ phân giải:

   ×1.00  (3508×2480)  → 11,125 token   ████████████████████████████████  100%
   ×0.75  (2631×1860)  →  6,204 token   ██████████████████                 56%
   ×0.50  (1754×1240)  →  2,772 token   ████████                           25%
   ×0.35  (1227×868)   →  1,364 token   ████                               12%

   Giảm một nửa chiều → token giảm BỐN lần (vì token ∝ diện tích).
```

> **Nhưng đánh đổi là thật và phải đo:** dưới một ngưỡng nào đó, chữ nhỏ trong bảng techpack sẽ
> không đọc được nữa và accuracy sụp. Bạn **không thể biết ngưỡng đó ở đâu nếu không đo**.
>
> **Thí nghiệm bắt buộc, 1 ngày:** lấy 20 trang techpack có ground truth, chạy extraction ở
> 5 mức resolution, vẽ đường cong:
>
> ```
>   accuracy
>     │  ────────●────●────●
>     │                     ╲●         ← điểm gãy
>     │                       ╲
>     │                        ╲●
>     └────────────────────────────── token ảnh
>       1.4k  2.8k  6.2k  11.1k
>              ↑
>          chọn ở đây: ngay TRƯỚC điểm gãy
> ```
>
> Kết quả của thí nghiệm này là một con số duy nhất — `max_pixels` nên đặt bằng bao nhiêu —
> và nó có thể cắt 50–75% chi phí ảnh mà không mất accuracy. Đây là ROI cao nhất trong toàn bộ
> bộ tài liệu này. → [10](10-toi-uu-token-chi-phi.md)§4

### 4.5 Ba tối ưu token ảnh khác, ít rủi ro hơn

| Tối ưu | Cách | Rủi ro |
|---|---|---|
| **Crop trước khi gửi** | Docling đã cho bbox vùng `picture`/`table` — gửi **vùng cắt**, không gửi cả trang | Thấp. Bạn đã có bbox rồi |
| **Lọc trang** | Bỏ trang bìa/size chart/care label trước khi vào VLM | Thấp–trung bình. Cần classifier trang (rẻ, xem [05](05-nlp.md)§5) |
| **Grayscale + giảm bit depth** | ❌ **KHÔNG tiết kiệm token** | Token phụ thuộc **độ phân giải**, không phụ thuộc kênh màu hay dung lượng file |

> Hàng thứ ba là một hiểu lầm rất phổ biến — nén PNG, đổi sang JPEG chất lượng thấp, chuyển
> grayscale đều **không giảm một token nào**. Chỉ resize mới giảm.

---

## 5. Đọc đúng một điểm trong code của bạn

```python
# vlm_clients.py
image_tokens = sum(max(1, int(len(base64.b64encode(img)) / 4)) for img in images)
```

Dòng này tính token ảnh từ **độ dài chuỗi base64**, tức là từ **dung lượng file**. Theo §4.4,
đó không phải đại lượng quyết định token.

Hệ quả thực tế: hai ảnh cùng 1568×1568 (đều đúng 3,136 token) nhưng một ảnh PNG 3MB, một ảnh
JPEG 150KB → hàm này báo chênh nhau **20 lần**. Mọi cảnh báo/ngưỡng/quyết định dựa trên số đó
đều lệch.

**Sửa:**

```python
import math

def qwen_image_tokens(h: int, w: int, factor: int = 28,
                      max_pixels: int = 16384 * 28 * 28) -> int:
    """Số token ảnh của họ Qwen-VL. Bội số 28 = patch 14 × merge 2×2."""
    hb = max(factor, round(h / factor) * factor)
    wb = max(factor, round(w / factor) * factor)
    if hb * wb > max_pixels:                       # kẹp lại nếu vượt trần
        beta = math.sqrt(h * w / max_pixels)
        hb = math.floor(h / beta / factor) * factor
        wb = math.floor(w / beta / factor) * factor
    return (hb // factor) * (wb // factor)
```

Và quan trọng hơn: **đối chiếu với `usage.prompt_tokens` thật** mà `_extract_usage_tokens()` của
bạn đã đọc về. Nếu hàm ước lượng lệch quá 10% so với `usage` thật, hằng số `factor`/`max_pixels`
của backend bạn đang chạy khác mặc định — chỉnh lại theo số thật.

---

## 6. Prompt cho VLM — khác prompt cho LLM text

| Nguyên tắc | Vì sao | Áp dụng |
|---|---|---|
| **Đặt ảnh TRƯỚC câu hỏi** | Với causal attention, text sau ảnh "nhìn thấy" ảnh; text trước thì không | Kiểm lại thứ tự trong `user_content` — code của bạn đang đặt text trước ảnh |
| **Một câu hỏi cụ thể > nhiều câu hỏi mơ hồ** | VLM dễ bỏ sót khi phải trả lời nhiều thứ | Tách thành nhiều call nhỏ nếu accuracy quan trọng hơn chi phí |
| **Yêu cầu tọa độ thay vì mô tả** | Tọa độ verify được, mô tả thì không | Bạn đã làm ở `coordinates_crop` — mở rộng |
| **Nói rõ "nếu không thấy thì trả null"** | Không nói → model bịa | Grounding gate |
| **Ảnh độ phân giải vừa đủ** | Quá nhỏ → không đọc được chữ; quá lớn → đốt token, và **có thể giảm accuracy** vì loãng attention | §4.4 |

> **Điểm thứ nhất đáng kiểm ngay.** Trong `run_vlm()`:
> ```python
> user_content = [{"type": "text", "text": user_prompt}]
> if image_bytes: ...user_content.append({"type": "image_url", ...})
> ```
> Text đứng trước ảnh. Với attention nhân quả, các token của `user_prompt` **không** attend được
> tới image token phía sau. Model vẫn trả lời (vì token sinh ra ở cuối nhìn thấy tất cả), nhưng
> đây là thứ tự kém tối ưu. Thử đảo lại và đo — một thay đổi 2 dòng, có thể đo được bằng golden set.

---

## 7. Khi nào dùng VLM, khi nào dùng model chuyên

```
                    ┌──────────────────────────────┐
                    │ Task có taxonomy CỐ ĐỊNH và  │
                    │ chạy > 10K lần/tháng?        │
                    └──────┬───────────────┬───────┘
                        CÓ │               │ KHÔNG
                           ▼               ▼
              ┌────────────────────┐   ┌─────────────────────┐
              │ Có ≥ 500 mẫu       │   │ DÙNG VLM            │
              │ gán nhãn được?     │   │ (linh hoạt thắng    │
              └───┬────────────┬───┘   │  chi phí)           │
                CÓ│            │KHÔNG  └─────────────────────┘
                  ▼            ▼
       ┌──────────────┐   ┌──────────────────────────┐
       │ MODEL CHUYÊN │   │ VLM để BOOTSTRAP nhãn,   │
       │ (classifier/ │   │ người sửa, rồi train     │
       │  detector)   │   │ model chuyên             │
       │ rẻ 100×,     │   │ ← đường đi thực tế nhất  │
       │ nhanh 100×,  │   └──────────────────────────┘
       │ tái lập 100% │
       └──────────────┘
```

**Áp vào pipeline của bạn:**

| Task hiện dùng VLM | Taxonomy cố định? | Tần suất | Đề xuất |
|---|---|---|---|
| Phân loại sketch FRONT/BACK | ✅ 2 lớp | cao | → **Linear probe trên embedding có sẵn.** Rẻ nhất, làm ngay |
| Chấm 3 điểm so sánh 2 sketch | ⚠ chủ quan | cao | Giữ VLM, nhưng **bootstrap nhãn** để train reranker riêng |
| Extraction bảng BOM từ trang | ❌ layout đa dạng | cao | **Giữ VLM.** Đây là chỗ VLM thật sự không thay được |
| Lọc trang có/không có BOM | ✅ nhị phân | cao | → classifier nhẹ, **cắt token ảnh ở thượng nguồn** ⭐ |

---

## 8. Bài tập

| # | Bài | Tiêu chí đạt | Ước lượng | Ưu tiên |
|---|---|---|---|---|
| 1 | **§2.2 đầy đủ** — histogram separation + Cohen's d trên sketch của bạn | 1 hình + 1 con số d + kết luận | 3h | ⭐⭐⭐ |
| 2 | **§4.4** — đường cong accuracy theo resolution trên 20 trang golden | 1 đường cong + `max_pixels` đề xuất | 8h | ⭐⭐⭐ |
| 3 | Thay `_estimate_tokens` bằng §5, đối chiếu với `usage` thật trên 50 call | sai số < 10% | 3h | ⭐⭐ |
| 4 | Cài `clip_loss` §1.2, train trên 100 cặp đồ chơi, xem loss giảm | loss → gần 0 | 3h | ⭐ |
| 5 | Đảo thứ tự ảnh/text trong `run_vlm`, đo accuracy trước/sau trên golden | 2 con số + CI | 4h | ⭐⭐ |
| 6 | Crop theo bbox Docling thay vì gửi cả trang, đo token + accuracy | bảng so sánh | 6h | ⭐⭐⭐ |

---

## 9. Ranh giới trung thực

- **Đã chạy thật:** bảng §4.2 và §4.4 — tính bằng thuật toán `smart_resize` của họ Qwen-VL
  (factor 28 = patch 14 × merge 2×2, `max_pixels` mặc định 16384·28·28). Con số là kết quả tính,
  không phải ước lượng.
- **⚠ CẢNH BÁO QUAN TRỌNG:** tôi áp công thức của **Qwen2-VL/Qwen2.5-VL**. Bạn đang chạy
  **Qwen3.6-VL 27B Q6** — tôi **không verify được** model đó có giữ nguyên `patch=14`,
  `merge=2×2`, `factor=28` hay không, và backend GGUF/llama.cpp của bạn có thể đặt `max_pixels`
  khác. **Cách kiểm chắc chắn, 10 phút:** gửi 3 ảnh ở 3 độ phân giải khác nhau, đọc
  `usage.prompt_tokens` trả về, trừ đi phần text. Con số thật đó mới là con số của bạn. Kết luận
  định tính (**token ∝ diện tích**, resize là cần gạt lớn nhất) đúng với **mọi** VLM họ ViT.
- **Đọc từ code, đã verify:** dòng `image_tokens = ... base64 ... / 4` trong `vlm_clients.py`;
  thứ tự text-trước-ảnh trong `user_content`; kích thước prompt trong bảng §4.3 (đo bằng
  `wc -c` trên các file `.yml` trong `prompts/`, chia 4 — nên **bản thân nó cũng là ước lượng**,
  xem [05](05-nlp.md)§1.3).
- **Là suy luận, chưa đo:** toàn bộ §2.1 (ba tầng lệch) là giải thích cơ chế hợp lý, **không phải
  kết quả đo**. Bài tập 1 tồn tại để biến nó thành số.
- **Là khuyến nghị chưa kiểm chứng:** §6 điểm thứ nhất (đảo thứ tự ảnh/text). Lý lẽ về causal
  attention đúng, nhưng mức ảnh hưởng thực tế **có thể rất nhỏ** — nhiều VLM được train với cả
  hai thứ tự. Phải đo, đừng đổi rồi tin.
