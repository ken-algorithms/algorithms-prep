# 04 · Computer Vision

> **Câu hỏi file này trả lời:** trong pipeline của bạn, chỗ nào **thật sự cần CV** và chỗ nào
> đang dùng VLM để làm thay việc mà một model CV nhỏ làm tốt hơn, rẻ hơn, nhanh hơn.
>
> **Thời lượng:** 15–18 giờ. **Yêu cầu trước:** [03](03-deep-learning-co-ban.md).

---

## 0. CV trong motivesidp — bản đồ hiện trạng

```
  TECHPACK PDF
       │
       ▼
  ┌─────────────────────────┐
  │ ① Docling layout-heron  │  ◀── CV THẬT: object detection trên layout tài liệu
  │    → bbox của "picture" │      (họ DocLayNet: picture/table/text/title...)
  └────────────┬────────────┘      ⚠ KHÔNG biết gì về quần áo
               │
               ▼
  ┌─────────────────────────┐
  │ ② Cắt ảnh + chia đôi    │  ◀── KHÔNG phải AI: OpenCV/PIL, chia theo tỉ lệ trái/phải
  │    theo tỉ lệ cố định   │      ⚠ giả định FRONT bên trái, BACK bên phải
  └────────────┬────────────┘
               │
       ┌───────┴────────┐
       ▼                ▼
  ┌─────────┐    ┌──────────────────┐
  │③ VLM    │    │④ FashionCLIP     │  ◀── CV THẬT: image encoder (ViT-B-16)
  │ FRONT/  │    │  → vector 512-dim│      ⚠ train trên ảnh SẢN PHẨM, không phải sketch
  │ BACK?   │    └──────────────────┘
  └─────────┘             │
   ⚠ dùng VLM 27B         ▼
     cho bài toán       Qdrant cosine search
     nhị phân           → top-k candidate
                               │
                               ▼
                        ┌──────────────┐
                        │⑤ VLM so 2 ảnh│ ◀── dùng VLM THAY cho garment-part detector
                        │  chấm 3 điểm │     ⚠ đắt, chậm, không tái lập được
                        └──────────────┘
```

**Bốn dấu ⚠ trên là bốn cơ hội CV thật sự.** File này cho bạn kiến thức để tự đánh giá từng cái.

---

## 1. Ảnh là tensor — và ba cái bẫy

```
   Ảnh RGB 224×224     →    tensor shape (3, 224, 224)
                             │   │    │
                             │   │    └── W (chiều rộng)
                             │   └────── H (chiều cao)
                             └────────── C (kênh màu)

   Một batch 32 ảnh    →    (32, 3, 224, 224)     ← PyTorch: NCHW
                            (32, 224, 224, 3)     ← TensorFlow/PIL/OpenCV: NHWC
```

| Bẫy | Triệu chứng | Sửa |
|---|---|---|
| **NCHW vs NHWC** | Shape error hoặc ảnh ra nhiễu | `img.permute(2,0,1)` khi từ PIL sang torch |
| **BGR vs RGB** | Ảnh xanh lè, accuracy tụt 5–10% mà không hiểu vì sao | OpenCV đọc **BGR**. `cv2.cvtColor(img, cv2.COLOR_BGR2RGB)` |
| **Chưa normalize** | Model pretrained cho kết quả rác | Phải dùng **đúng mean/std của model** (ImageNet ≠ CLIP) |

```python
# Normalize của ImageNet (ResNet, ViT ImageNet)
mean=[0.485, 0.456, 0.406]; std=[0.229, 0.224, 0.225]
# Normalize của CLIP/OpenCLIP — KHÁC, và FashionCLIP dùng cái này
mean=[0.48145466, 0.4578275, 0.40821073]; std=[0.26862954, 0.26130258, 0.27577711]
```

> **Kiểm tra ngay hôm nay:** service FashionCLIP của bạn có dùng đúng normalize của OpenCLIP
> không? Dùng nhầm ImageNet stats là lỗi **im lặng** — vẫn ra vector 512-dim, cosine vẫn tính
> được, chỉ là chất lượng tụt mà không ai biết. Đây là 15 phút kiểm tra có khả năng trả lời được
> một phần câu hỏi "vì sao similarity của mình không tốt".

---

## 2. Convolution — cơ chế

### 2.1 Một phép tích chập làm gì

```
   Input 5×5              Kernel 3×3           Output 3×3
   ┌─────────────┐        ┌───────┐            ┌─────────┐
   │ 1 1 1 0 0   │        │ 1 0 1 │            │ 4 3 4   │
   │ 0 1 1 1 0   │   ⊛    │ 0 1 0 │     =      │ 2 4 3   │
   │ 0 0 1 1 1   │        │ 1 0 1 │            │ 2 3 4   │
   │ 0 0 1 1 0   │        └───────┘            └─────────┘
   │ 0 1 1 0 0   │
   └─────────────┘        Kernel trượt qua từng vị trí,
                          nhân từng phần tử rồi cộng lại.
                          Ô [0,0] = 1·1+1·0+1·1 + 0·0+1·1+1·0 + 0·1+0·0+1·1 = 4
```

**Ba tính chất khiến conv thắng fully-connected cho ảnh:**

| Tính chất | Nghĩa là | Hệ quả |
|---|---|---|
| **Chia sẻ tham số** | Cùng kernel dùng cho mọi vị trí | Ít tham số hơn FC hàng nghìn lần |
| **Kết nối cục bộ** | Mỗi output chỉ nhìn 1 vùng nhỏ | Khớp với cấu trúc ảnh (pixel gần nhau liên quan nhau) |
| **Bất biến tịnh tiến** | Vật thể dịch chuyển vẫn nhận ra | Không cần thấy con mèo ở mọi vị trí lúc train |

### 2.2 Công thức kích thước — phải thuộc

```
        H_out = ⌊(H_in + 2·padding − kernel) / stride⌋ + 1
```

| Input | kernel | stride | padding | Output | Ghi nhớ |
|---|---|---|---|---|---|
| 224 | 3 | 1 | 1 | **224** | k=3,s=1,p=1 → **giữ nguyên kích thước** (mẫu phổ biến nhất) |
| 224 | 3 | 2 | 1 | **112** | s=2 → **giảm một nửa** |
| 224 | 7 | 2 | 3 | **112** | lớp đầu ResNet |
| 224 | 1 | 1 | 0 | **224** | conv 1×1 = trộn kênh, không đụng không gian |

```python
import torch.nn as nn, torch
x = torch.randn(1, 3, 224, 224)
for k, s, p in [(3,1,1), (3,2,1), (7,2,3), (1,1,0)]:
    print(f"k={k} s={s} p={p} →", tuple(nn.Conv2d(3, 16, k, s, p)(x).shape))
```

Chạy thật:

```
k=3 s=1 p=1 → (1, 16, 224, 224)
k=3 s=2 p=1 → (1, 16, 112, 112)
k=7 s=2 p=3 → (1, 16, 112, 112)
k=1 s=1 p=0 → (1, 16, 224, 224)
```

### 2.3 Receptive field — vì sao cần mạng sâu

```
  Lớp 1 (3×3):  mỗi neuron "nhìn" 3×3 pixel gốc
  Lớp 2 (3×3):  nhìn 5×5
  Lớp 3 (3×3):  nhìn 7×7
  ...
  Lớp 10:       nhìn 21×21

  → Muốn nhận diện "cả cái áo" (chiếm 200×200 pixel) cần RẤT nhiều lớp,
    hoặc stride/pooling để thu nhỏ nhanh, hoặc... attention (ViT nhìn toàn cục từ lớp 1).
```

Đây chính là lý do ViT xuất hiện: **attention có receptive field toàn cục ngay từ lớp đầu tiên.**

---

## 3. Kiến trúc CNN — 4 mốc cần biết

| Năm | Model | Đóng góp | Còn dùng không |
|---|---|---|---|
| 2012 | AlexNet | Chứng minh DL + GPU thắng feature thủ công | Lịch sử |
| 2014 | VGG | Xếp chồng conv 3×3 | Lịch sử (quá nặng) |
| **2015** | **ResNet** | **Residual `y=f(x)+x` → train được 100+ lớp** | ⭐ Vẫn là backbone mặc định cho nhiều task |
| 2019 | EfficientNet | Scale cân bằng depth/width/resolution | Khi cần nhẹ |
| **2020** | **ViT** | **Bỏ conv, dùng Transformer trên patch** | ⭐ Nền của CLIP, SigLIP, mọi VLM |

### 3.1 ViT — chính là thứ FashionCLIP đang dùng

```
   Ảnh 224×224×3
        │
        ▼  chia thành patch 16×16 (không chồng lấn)
   ┌──┬──┬──┬──┐          14 × 14 = 196 patch
   │  │  │  │  │          mỗi patch: 16·16·3 = 768 số
   ├──┼──┼──┼──┤              │
   │  │  │  │  │              ▼  Linear(768 → 768)
   ├──┼──┼──┼──┤          196 "token", mỗi token 768 chiều
   │  │  │  │  │              │
   └──┴──┴──┴──┘              ▼  + positional embedding, + [CLS] token
                          197 token
                              │
                              ▼  12 Transformer block (self-attention + FFN)
                              │
                              ▼  lấy vector của [CLS]
                          embedding ảnh (768 → project xuống 512 với ViT-B-16 của CLIP)
```

> **Đọc sơ đồ này là hiểu vì sao "token ảnh" tồn tại và vì sao độ phân giải quyết định chi phí:**
> ảnh 448×448 → 784 patch → **4× token** so với 224×224. Đây là gốc của [10](10-toi-uu-token-chi-phi.md)§4.
> ViT-**B/16** nghĩa là **B**ase size, patch **16**.

| Ký hiệu | Patch | Token cho ảnh 224 | Chi phí attention (O(n²)) |
|---|---|---|---|
| ViT-B/32 | 32×32 | 49 | 1× |
| **ViT-B/16** (FashionCLIP) | 16×16 | 196 | ~16× |
| ViT-L/14 | 14×14 | 256 | ~27× |

---

## 4. Transfer learning — chiến lược bạn sẽ dùng 95% thời gian

**Bạn sẽ gần như không bao giờ train CNN từ đầu.** Ba chiến lược, xếp theo chi phí:

```
                  Backbone pretrained (ViT/ResNet)      Head mới
                  ┌─────────────────────────────┐      ┌──────┐
  ① Linear probe  │ ❄❄❄❄❄ ĐÓNG BĂNG HẾT ❄❄❄❄❄ │─────▶│ 🔥   │   ~1K params
                  └─────────────────────────────┘      └──────┘   phút, CPU được
                                                                   cần ~50-500 mẫu

                  ┌─────────────────────────────┐      ┌──────┐
  ② Partial FT    │ ❄❄❄❄❄❄❄❄❄❄❄❄❄❄ │ 🔥🔥 │─────▶│ 🔥   │   ~10M params
                  └─────────────────────────────┘      └──────┘   chục phút, 1 GPU
                                  2 block cuối                     cần ~1K-10K mẫu

                  ┌─────────────────────────────┐      ┌──────┐
  ③ Full FT       │ 🔥🔥🔥🔥🔥 TRAIN HẾT 🔥🔥🔥🔥 │─────▶│ 🔥   │   ~86M params
                  └─────────────────────────────┘      └──────┘   giờ, GPU khỏe
                                                                   cần ~10K+ mẫu
                                                                   ⚠ dễ quên kiến thức pretrain
```

**Quy tắc chọn:**

| Dữ liệu bạn có | Domain giống pretrain? | Chọn |
|---|---|---|
| < 500 mẫu | Giống | ① Linear probe |
| < 500 mẫu | **Khác** (← sketch của bạn) | ① trước, nếu tệ thì ② với lr rất nhỏ |
| 1K–10K | Giống | ② |
| 1K–10K | Khác | ② hoặc ③ với lr nhỏ + warmup dài |
| > 10K | Bất kỳ | ③ |

> **Áp cho bạn:** sketch line-art **rất khác** ảnh sản phẩm thời trang. Với vài nghìn sketch,
> ② là điểm ngọt. Nhưng chỉ làm ② sau khi ① đã cho bạn con số baseline — nếu không bạn không
> biết ② có đáng không. → [08](08-finetuning.md)§6

---

## 5. Augmentation — cách nhân dữ liệu miễn phí

**Nguyên tắc:** chỉ dùng biến đổi mà nhãn **không đổi** trong domain của bạn.

| Augmentation | Với ảnh sản phẩm | Với **sketch techpack** | Lý do |
|---|---|---|---|
| Random crop | ✅ | ⚠ cẩn thận | Cắt mất cổ áo → mất chính thông tin cần |
| **Horizontal flip** | ✅ | ❌ **KHÔNG** | Lật ngang biến FRONT thành ảnh gương → nhầm với BACK, và túi ngực trái/phải đổi bên |
| Color jitter | ✅ | ❌ vô nghĩa | Sketch là đen trắng |
| **Rotation nhỏ (±5°)** | ✅ | ✅ | Scan lệch là có thật |
| **Thay đổi độ dày nét** | — | ✅ ⭐ | Máy scan/độ nén khác nhau → nét dày mỏng khác nhau. Rất hợp domain |
| **Thêm nhiễu scan / JPEG artifact** | ✅ | ✅ ⭐ | Khớp với ảnh thật từ PDF nén |
| Random erasing | ✅ | ✅ | Che 1 vùng → model không phụ thuộc 1 chi tiết |
| Mixup/CutMix | ✅ | ⚠ | Trộn 2 sketch tạo ra thứ không tồn tại |

```python
import torchvision.transforms as T
sketch_aug = T.Compose([
    T.RandomRotation(5, fill=255),                       # nền trắng, không đen
    T.RandomApply([T.GaussianBlur(3, sigma=(0.1, 0.8))], p=0.3),   # giả lập nét mờ
    T.RandomResizedCrop(224, scale=(0.85, 1.0)),         # chỉ crop nhẹ
    T.ToTensor(),
    T.Normalize(mean=[0.48145466, 0.4578275, 0.40821073],
                std=[0.26862954, 0.26130258, 0.27577711]),   # CLIP stats
])
```

> **Điểm hay bị bỏ sót:** `fill=255` khi xoay. Mặc định `fill=0` = đen, tạo ra góc đen mà ảnh thật
> không bao giờ có → model học "góc đen = đã augment" thay vì học hình dáng. Lỗi kinh điển.

---

## 6. Detection & segmentation — khi nào bạn cần

### 6.1 Bốn bài toán, phân biệt bằng output

```
  CLASSIFICATION      DETECTION           SEMANTIC SEG        INSTANCE SEG
  ┌──────────┐        ┌──────────┐        ┌──────────┐        ┌──────────┐
  │          │        │ ┌────┐   │        │ ░░░░     │        │ ░░░░     │
  │  (áo)    │        │ │túi │   │        │ ░░░░░    │        │ ░░░░░    │
  │          │        │ └────┘   │        │  ▓▓▓     │        │  ▒▒▒     │
  └──────────┘        └──────────┘        └──────────┘        └──────────┘
  "đây là áo"         "túi ở đây          "pixel này thuộc    "túi #1 và túi #2
                       (x,y,w,h)"          lớp túi"            là 2 vật khác nhau"
```

### 6.2 Model nào cho việc gì

| Bài | Model nên thử | Ghi chú |
|---|---|---|
| Detection real-time | **YOLO (v8/v11)** | Nhanh, dễ train, hệ sinh thái Ultralytics tốt |
| Detection chính xác | **DETR / DINO-DETR** | Không cần NMS, tốt với vật chồng lấn |
| Detection **không cần train** | **Grounding DINO** ⭐ | Nhận **text prompt**: "pocket", "collar", "welt pocket" |
| Segmentation không cần train | **SAM / SAM 2** | Cắt vùng theo điểm/box, zero-shot |
| Layout tài liệu | **Docling layout-heron** (bạn đang dùng), LayoutLMv3, DocLayNet models | Đúng công cụ cho đúng việc |

### 6.3 Cơ hội cụ thể nhất trong repo của bạn

Tài liệu nội bộ ghi rõ: *"Garment-attribute CV (lapel/pocket/vent detection): ❌ Chưa có"* và
hiện đang dùng VLM so sánh 2 ảnh để "giả lập".

```
   HIỆN TẠI                                 ĐỀ XUẤT (2 giai đoạn)
   ┌──────────────────────┐                 ┌──────────────────────────────────┐
   │ VLM 27B so 2 ảnh     │   GĐ 1 →        │ Grounding DINO, ZERO-SHOT        │
   │ → 3 điểm + lý giải   │   (1 tuần)      │ prompt: "patch pocket", "lapel", │
   │                      │                 │ "welt pocket", "vent"            │
   │ • đắt (27B, có ảnh)  │                 │ → bbox + confidence, KHÔNG train │
   │ • chậm               │                 │ → so attribute thay vì so ảnh    │
   │ • không tái lập được │                 └──────────────┬───────────────────┘
   │ • không giải thích   │                                │ nếu zero-shot đủ tốt
   │   được vì sao 0.7    │   GĐ 2 →                       ▼
   └──────────────────────┘   (1 tháng)     ┌──────────────────────────────────┐
                                            │ Gán nhãn 300-500 sketch từ output│
                                            │ GĐ1 (sửa tay, nhanh hơn nhãn từ  │
                                            │ đầu nhiều) → fine-tune YOLO nhỏ  │
                                            │ → chạy CPU, mili-giây, tái lập   │
                                            └──────────────────────────────────┘
```

**Đánh đổi phải nói rõ:** hướng này đổi *"một model vạn năng, cấu hình bằng prompt"* lấy
*"một model chuyên biệt, phải gán nhãn và maintain"*. Nó chỉ đáng khi (a) taxonomy attribute ổn
định, và (b) bạn có người gán nhãn. Nếu taxonomy còn thay đổi hàng tháng, **giữ VLM**.

**Và quan trọng nhất:** trước khi làm gì cả, hãy chạy Grounding DINO zero-shot trên 30 sketch và
**nhìn bằng mắt**. Một buổi chiều. Nếu nó không bắt được túi trên sketch line-art (rất có thể,
vì nó cũng train trên ảnh thật) thì cả hướng này chết sớm — và bạn tiết kiệm được một tháng.

---

## 7. Metric learning — nền lý thuyết cho bài similarity của bạn

Đây là nhánh CV **liên quan trực tiếp nhất** tới Team B. Ý tưởng: thay vì học phân loại, học một
**không gian nhúng** sao cho khoảng cách phản ánh đúng "giống nhau".

```
   TRƯỚC khi học metric              SAU khi học metric
   (không gian FashionCLIP gốc)      (đã fine-tune trên sketch của bạn)

        ●a  ○b                            ●a ●c ●d
      ○c      ●d                                        ○b ○e
         ●e ○f                                        ○f

   ● = cùng style, ○ = style khác     cùng style dồn cụm,
   trộn lẫn → cosine vô dụng          khác style tách xa → cosine có nghĩa
```

### 7.1 Ba họ loss

| Loss | Công thức ý tưởng | Cần gì | Ghi chú |
|---|---|---|---|
| **Contrastive** | Kéo cặp giống lại, đẩy cặp khác ra quá margin | cặp (a,b,label) | Cổ điển |
| **Triplet** | `max(0, d(a,p) − d(a,n) + margin)` | bộ ba (anchor, positive, negative) | Chất lượng phụ thuộc **cách chọn negative** |
| **InfoNCE / NT-Xent** ⭐ | Softmax trên similarity trong batch: positive là 1, các mẫu còn lại là negative | chỉ cần cặp positive | Dùng trong CLIP. **Batch lớn = nhiều negative miễn phí** |

### 7.2 Hard negative mining — chỗ quyết định thành bại

```
  Chọn negative NGẪU NHIÊN             Chọn HARD negative
  anchor: jacket 2 nút                 anchor: jacket 2 nút
  negative: váy dạ hội                 negative: jacket 3 nút  ← gần, khó
       │                                    │
       ▼                                    ▼
  loss về 0 rất nhanh,                 model buộc phải học
  model không học được gì              chi tiết phân biệt thật
  → "collapse": mọi embedding          → embedding có ích
    giống nhau
```

> **Trong bài của bạn, hard negative sẵn có và miễn phí:** các style **cùng product subgroup**
> chính là hard negative tự nhiên. Random negative (lấy style thuộc nhóm khác) sẽ khiến loss về
> 0 mà chẳng học được gì. → [08](08-finetuning.md)§6

---

## 8. OCR & document AI — nhánh bạn đang chạm mà chưa gọi tên

| Lớp công cụ | Ví dụ | Bạn đang dùng | Khi nào cần |
|---|---|---|---|
| OCR thuần | Tesseract, PaddleOCR, **RapidOCR** ✅ | `rapidocr-onnxruntime` trong `pyproject.toml` | Text thuần, có sẵn bbox |
| Layout analysis | **Docling layout-heron** ✅, LayoutLMv3 | Đang dùng | Biết block nào là bảng/hình/tiêu đề |
| Table structure | TableFormer, PubTabNet models | ⚠ chưa rõ | **Bảng techpack có merged cell** — đây là chỗ khó nhất |
| Document VQA / end-to-end | **Qwen-VL** ✅, Donut, Nougat | Đang dùng | Hỏi thẳng "cho tôi BOM dạng JSON" |

### 8.1 Đánh đổi: OCR+rule vs VLM end-to-end

| | OCR + layout + rule | VLM end-to-end |
|---|---|---|
| Chi phí/trang | ~0 (CPU) | **cao** (token ảnh + output) |
| Tốc độ | ms | giây |
| Tái lập | 100% | không (xem [01](01-nen-tang-toan.md)§4.2) |
| Bảng lạ, layout mới | ❌ gãy | ✅ vẫn xử được |
| Debug khi sai | dễ, biết rule nào sai | khó, "prompt lại xem sao" |

> **Chiến lược lai đáng thử — và nó cắt token thật:** dùng Docling + OCR để **lọc trang**.
> Trang nào chắc chắn không chứa BOM (trang bìa, trang size chart, trang care label) thì
> **không gửi vào VLM**. Nếu cắt được 40% số trang, bạn cắt ~40% token ảnh — mà không đụng gì
> tới chất lượng. → [10](10-toi-uu-token-chi-phi.md)§5

---

## 9. Bài tập

| # | Bài | Tiêu chí đạt | Ước lượng |
|---|---|---|---|
| 1 | Cài convolution 2D bằng numpy thuần (vòng lặp), so với `torch.nn.functional.conv2d` | khớp 1e-5 | 2h |
| 2 | Tính tay output shape cho 5 cấu hình conv, verify bằng code | 5/5 đúng | 30m |
| 3 | Linear probe FashionCLIP → phân loại FRONT/BACK trên sketch của bạn | accuracy + CI, so với VLM | 4h |
| 4 | Chạy Grounding DINO zero-shot trên 30 sketch với 6 prompt attribute, **nhìn bằng mắt** | 1 trang kết luận: dùng được / không | 4h |
| 5 | Vẽ 2 histogram cosine (cùng style vs khác style) trên embedding hiện tại | 1 hình + số đo độ chồng lấn | 3h |
| 6 | Kiểm tra service FashionCLIP có dùng đúng CLIP normalize stats không | câu trả lời có/không + bằng chứng code | 30m |

**Bài 6 làm trước tiên** — rẻ nhất, và nếu sai thì nó giải thích được rất nhiều thứ.

---

## 10. Ranh giới trung thực

- **Đã chạy thật:** §2.2 (output shape conv) — đã verify bằng PyTorch.
- **Đọc từ code, đã verify:** sơ đồ §0 dựng từ `docs/ai_technologies_overview.md` và
  `sketches_extraction.py`; `rapidocr-onnxruntime` có thật trong `pyproject.toml`.
- **Là suy luận của tôi, chưa đo:** toàn bộ §6.3 (đề xuất Grounding DINO). Tôi **không biết**
  Grounding DINO có hoạt động trên sketch line-art hay không — nó cũng train trên ảnh thật nên
  có thể gặp đúng vấn đề domain gap như FashionCLIP. Đó là lý do bài tập 4 là *"nhìn bằng mắt
  trong 1 buổi chiều"* chứ không phải *"triển khai"*.
- **Là suy luận:** ước lượng "cắt 40% trang → cắt 40% token" ở §8.1 giả định phân bố trang đồng
  đều. Số thật phải đo trên techpack của bạn.
- **Chưa kiểm chứng:** tôi không đọc được code của service FashionCLIP (nó chạy ngoài repo này),
  nên nhận định về normalize stats ở §1 là **câu hỏi cần bạn kiểm**, không phải phát hiện.
- **Không bàn tới:** video, 3D, pose estimation, GAN/diffusion — không liên quan bài toán hiện tại.
