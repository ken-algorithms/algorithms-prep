# 14 · Tài nguyên — theo đúng thứ tự đọc

> **Nguyên tắc:** danh sách này **có thứ tự**. Đọc sai thứ tự là lý do phổ biến khiến người ta bỏ
> cuộc — bắt đầu bằng một cuốn quá nặng rồi kết luận "mình không hợp với AI".
>
> Mỗi mục có: **vì sao đọc**, **đọc phần nào**, **bỏ qua phần nào**. Bỏ qua cũng quan trọng như đọc.

---

## 0. Lộ trình tài nguyên

```
  GIAI ĐOẠN 1 · NỀN (tháng 1-3)
  ┌────────────────────────────────────────────────────────────────┐
  │ ① 3Blue1Brown — Neural Networks (4 video, 1h)   ← BẮT ĐẦU ĐÂY  │
  │ ② Andrej Karpathy — "Neural Networks: Zero to Hero"            │
  │ ③ fast.ai Practical Deep Learning (7 bài đầu)                  │
  │ ④ Dive into Deep Learning (d2l.ai) — tra cứu                   │
  └────────────────────────────────────────────────────────────────┘
                              │
  GIAI ĐOẠN 2 · MIỀN (tháng 4-8)
  ┌────────────────────────────────────────────────────────────────┐
  │ CV: CS231n  ·  NLP: CS224n  ·  LLM: Karpathy GPT + LLM101n     │
  │ RAG: Pinecone/Qdrant docs  ·  sentence-transformers docs       │
  └────────────────────────────────────────────────────────────────┘
                              │
  GIAI ĐOẠN 3 · NÂNG CAO (tháng 9-12)
  ┌────────────────────────────────────────────────────────────────┐
  │ Paper gốc (§4)  ·  HF course  ·  Designing ML Systems          │
  └────────────────────────────────────────────────────────────────┘
```

---

## 1. Giai đoạn 1 — nền

### ① 3Blue1Brown — "Neural Networks" ⭐ BẮT ĐẦU Ở ĐÂY

- **Là gì:** 4 video hoạt hình, tổng ~70 phút. Miễn phí trên YouTube.
- **Vì sao đầu tiên:** nó cho bạn **trực giác hình học** về gradient descent và backprop mà không
  một cuốn sách nào làm tốt bằng. Xem xong, mọi công thức sau đó trở nên dễ chịu.
- **Xem:** cả 4, đặc biệt video 3 và 4 (backpropagation).
- Cũng nên xem: series *Essence of Linear Algebra* (15 video) nếu bạn thấy [01](01-nen-tang-toan.md) khó.

### ② Andrej Karpathy — "Neural Networks: Zero to Hero" ⭐⭐ QUAN TRỌNG NHẤT

- **Là gì:** series YouTube, xây từ một neuron tới một GPT nhỏ, **gõ từng dòng code**.
- **Vì sao:** đây là tài nguyên **hiệu quả nhất trên đời** cho người đã biết lập trình mà muốn
  hiểu deep learning thật. Không có toán thừa, không có slide, chỉ có code chạy được.
- **Thứ tự:** `micrograd` → `makemore` (5 phần) → `GPT from scratch` → `tokenizer`.
- **Cách xem đúng:** **gõ lại toàn bộ code**, đừng chỉ xem. Dừng video, tự đoán dòng tiếp theo.
  Một tập 2 tiếng sẽ tốn bạn 5 tiếng — đó là dấu hiệu bạn đang làm đúng.
- **Tập `tokenizer`** giải thích chính xác nội dung [05](05-nlp.md)§1 và liên quan trực tiếp tới
  bài toán chi phí token của bạn.

### ③ fast.ai — "Practical Deep Learning for Coders"

- **Vì sao:** triết lý top-down — làm chạy trước, hiểu sau. Rất hợp với người đã là kỹ sư.
- **Đọc:** 7 bài đầu. **Bỏ qua:** phần 2 (from foundations) — trùng với ② và ② tốt hơn.
- Bài về **fine-tuning và transfer learning** áp thẳng vào [04](04-computer-vision.md)§4.

### ④ Dive into Deep Learning (d2l.ai)

- **Vì sao:** sách miễn phí, có code PyTorch chạy được cho **mọi** khái niệm. Dùng để **tra cứu**,
  không phải đọc tuần tự.
- **Đọc khi cần:** chương 3–5 (linear/MLP), 7 (CNN), 10–11 (attention/Transformer).
- **Bỏ qua:** chương optimization nâng cao, RNN chi tiết.

---

## 2. Giai đoạn 2 — theo nhánh

### Computer Vision — CS231n (Stanford)

- **Đọc:** lecture notes (miễn phí, chất lượng rất cao) về backprop, CNN, training tricks.
- **Bỏ qua:** phần lịch sử, phần hardware đã lỗi thời.
- **Bổ sung hiện đại (CS231n hơi cũ ở mảng này):** blog *"An Image is Worth 16x16 Words"* (ViT),
  tài liệu OpenCLIP, và tài liệu Ultralytics YOLO.

### NLP — CS224n (Stanford)

- **Đọc:** lecture về word vectors, attention, Transformer, pretraining.
- **Bỏ qua:** phần syntactic parsing, machine translation cổ điển.

### LLM — Karpathy (lại) + tài liệu nhà cung cấp

- `Let's build GPT` và `Let's build the GPT Tokenizer` — bắt buộc.
- `LLM101n` (nếu đã ra đủ nội dung) — tổng quan build LLM từ đầu.
- **Tài liệu Anthropic về prompt engineering** — ngắn, thực dụng, tốt hơn hầu hết khóa học trả phí.

### RAG & retrieval

| Nguồn | Vì sao |
|---|---|
| **Qdrant docs** (concepts, filtering, quantization) | Bạn đang dùng — đọc kỹ phần payload index và hybrid |
| **sentence-transformers docs** ⭐ | Tài liệu tốt nhất về training embedding & cross-encoder. Đọc mục *Training Overview* và *Losses* |
| **BEIR benchmark paper** | Hiểu cách đo retrieval cho đúng |
| **ColBERT paper** | Vì bạn đang dùng MaxSim multivector mà có thể chưa đọc |

### Fine-tuning

| Nguồn | Vì sao |
|---|---|
| **HuggingFace PEFT docs** | LoRA/QLoRA thực hành |
| **TRL docs** (`SFTTrainer`) | SFT đúng cách, có masking prompt |
| **Unsloth** | Fine-tune nhanh trên 1 GPU, notebook sẵn dùng |
| **sentence-transformers > Training** ⭐ | Đúng thứ bạn cần cho project ⑧ ở [12](12-thuc-hanh-tren-motivesidp.md) |

---

## 3. Sách — chỉ ba cuốn

| Sách | Vì sao | Đọc phần nào |
|---|---|---|
| **Hands-On Machine Learning** (Géron) | Cuốn thực hành tốt nhất cho người mới. Code sklearn + Keras | Phần I (ML cổ điển) là chính. Phần II đọc sau ② |
| **Designing Machine Learning Systems** (Chip Huyen) ⭐ | **Cuốn hợp với bạn nhất.** Nói về hệ thống ML thật: data, eval, deploy, drift — đúng thế mạnh của bạn | Chương về evaluation, data distribution shift, model deployment |
| **AI Engineering** (Chip Huyen) | Viết cho đúng vị trí bạn đang ở: xây ứng dụng trên foundation model | Cả cuốn |

**Không khuyến nghị lúc này:** *Deep Learning* (Goodfellow) — nặng toán, lỗi thời ở nhiều chỗ;
*Pattern Recognition and ML* (Bishop) — hay nhưng dành cho nhánh research. Cả hai sẽ làm bạn
nản trong tuần đầu.

---

## 4. Paper — đọc khi đã có nền, theo thứ tự này

| # | Paper | Vì sao liên quan TRỰC TIẾP tới bạn |
|---|---|---|
| 1 | **Attention Is All You Need** (2017) | Nền của mọi model trong stack của bạn |
| 2 | **BERT** (2018) | Hiểu encoder → bge-m3 |
| 3 | **ViT** — *An Image is Worth 16x16 Words* (2020) | FashionCLIP dùng ViT-B/16; giải thích token ảnh |
| 4 | **CLIP** (2021) | Chính là model bạn đang dùng. Đọc kỹ phần contrastive loss |
| 5 | **LoRA** (2021) | Phương pháp fine-tune bạn sẽ dùng |
| 6 | **ColBERT** (2020) | MaxSim multivector trong Qdrant của bạn |
| 7 | **Sentence-BERT** (2019) | Vì sao mean-pool BERT không đủ |
| 8 | **QLoRA** (2023) | Fine-tune trên 1 GPU |
| 9 | **Lost in the Middle** (2023) | Vì sao nạp nhiều context không tốt hơn ([07](07-embedding-retrieval-rag.md)§8) |
| 10 | **Qwen-VL / Qwen2-VL tech report** | Model bạn đang chạy. Tìm phần image token & dynamic resolution ⭐ |

> **Cách đọc paper (3 lượt):** ① abstract + hình + kết luận (10 phút, quyết định có đọc tiếp không)
> → ② đọc hết, bỏ qua chứng minh (1 giờ) → ③ đọc kỹ phần liên quan việc của bạn + thử tái lập
> (nhiều giờ). Đa số paper dừng ở lượt ①, và đó là điều bình thường.
>
> **Paper #10 là paper bạn nên đọc sớm nhất** — nó chứa câu trả lời chính xác cho công thức token
> ảnh mà [06](06-multimodal-vlm.md)§9 đang cảnh báo là chưa verify được.

---

## 5. Công cụ — cài đặt tối thiểu

| Việc | Công cụ | Ghi chú |
|---|---|---|
| Môi trường Python | `uv` | Bạn đang dùng rồi, nhanh hơn poetry/pip nhiều |
| Notebook | Jupyter / VSCode notebook | Để thí nghiệm nhanh |
| Train | PyTorch (không cần TensorFlow) | Ngành đã hội tụ về PyTorch |
| Tracking | MLflow self-host | Hợp thói quen self-host của bạn ([11](11-mlops-serving.md)§5) |
| Fine-tune LLM | HF `transformers` + `peft` + `trl`, hoặc Unsloth | |
| Fine-tune embedding | `sentence-transformers` | Đúng công cụ cho project ⑧ |
| GPU khi không có máy | Colab (free/Pro), Kaggle (30h GPU/tuần miễn phí) | Kaggle đủ cho project ⑧ |
| Đếm token | `tiktoken`, hoặc tokenizer HF của đúng model | Không dùng `len/4` nữa |

---

## 6. Kênh theo dõi — đừng theo quá nhiều

| Nguồn | Tần suất | Vì sao |
|---|---|---|
| **HuggingFace Daily Papers** | hàng ngày, lướt 5 phút | Biết cái gì đang nổi |
| **Blog Qdrant / vLLM / HF** | khi có bản mới | Trực tiếp ảnh hưởng stack của bạn |
| **Chip Huyen blog** | hiếm nhưng chất | Góc nhìn hệ thống |
| **Sebastian Raschka** (Ahead of AI) | hàng tháng | Giải thích kỹ thuật rõ ràng nhất |
| **Karpathy** (X/YouTube) | hiếm | Mỗi bài đều đáng |

> **Lời khuyên ngược dòng:** hãy theo dõi **ít** đi. Lĩnh vực này tạo ra nhiều tiếng ồn hơn tín
> hiệu, và cảm giác "phải cập nhật liên tục" là kẻ thù của việc học sâu. Một giờ làm project ở
> [12](12-thuc-hanh-tren-motivesidp.md) đáng giá hơn mười giờ đọc tin.

---

## 7. Ngân sách thời gian — thực tế

```
  8-10 giờ/tuần, phân bổ:

  ████████████████░░░░░░░░  60%  LÀM (project ở file 12, gõ code theo Karpathy)
  ██████░░░░░░░░░░░░░░░░░░  25%  ĐỌC (video, sách, tài liệu)
  ███░░░░░░░░░░░░░░░░░░░░░  15%  VIẾT (báo cáo thí nghiệm, blog)

  ⚠ Tỉ lệ sai phổ biến: 90% đọc / 10% làm.
    Nó tạo ra cảm giác tiến bộ mà không tạo ra năng lực.
    Nếu tuần này bạn không chạy một dòng code nào mới, tuần đó bằng không.
```

**Phần VIẾT 15% đừng bỏ.** Viết lại thứ vừa học là cách kiểm tra rẻ nhất xem bạn có thật sự hiểu
không — và nó vừa hay tạo ra portfolio ở [13](13-lo-trinh-ai-engineer-vsf-fpt.md)§6.

---

## 8. Ranh giới trung thực

- **Là khuyến nghị dựa trên hiểu biết của tôi tính tới thời điểm huấn luyện.** Tài nguyên online
  thay đổi: khóa học bị gỡ, được cập nhật, đổi URL. **Kiểm tra còn tồn tại trước khi lên kế hoạch**
  dựa vào một mục cụ thể.
- **Chưa verify ở thời điểm viết:** tôi không mở từng link để kiểm tình trạng hiện tại. Đặc biệt
  `LLM101n` — tôi không chắc trạng thái hoàn thành của nó.
- **Là ý kiến chủ quan:** thứ tự ưu tiên, và khuyến nghị **không** đọc Goodfellow/Bishop lúc này.
  Nhiều người sẽ không đồng ý. Lý lẽ của tôi: với hồ sơ và mục tiêu 12 tháng của bạn, chúng có
  tỉ lệ bỏ cuộc cao và lợi ích thấp.
- **Không có ở đây:** khóa học trả phí (Coursera, Udacity, DeepLearning.AI). Không phải vì chúng
  tệ — mà vì mọi thứ trong danh sách trên đều **miễn phí và chất lượng ngang hoặc hơn**. Nếu bạn
  cần cấu trúc và deadline bên ngoài để có động lực thì khóa trả phí có giá trị riêng của nó.
