# 00 · Chẩn đoán năng lực — bạn đang ở đâu

> Mục tiêu: sau 20 phút, bạn có **một bảng điểm 9 trục** và biết chính xác nên đọc file nào trước.
> Chẩn đoán dưới đây dựa trên bằng chứng đọc từ code `motivesidp-ai-service`, không phải phỏng đoán.

---

## 1. Thang L0–L5 — định nghĩa chung

Thang này áp cho mọi trục. Điểm mấu chốt: **ranh giới L2→L3 là ranh giới "dùng" → "hiểu"**, và
đó đúng là chỗ bạn đang đứng.

| Mức | Tên | Bạn làm được gì | Câu test |
|---|---|---|---|
| **L0** | Chưa biết | Nghe tên, không biết để làm gì | — |
| **L1** | Biết khái niệm | Giải thích được bằng lời cho người ngoài | "Embedding là gì?" |
| **L2** | **Dùng được API** | Gọi thư viện/endpoint, ráp vào hệ thống, debug lỗi tích hợp | "Gọi bge-m3 để embed 1000 dòng vật liệu" |
| **L3** | **Hiểu cơ chế** | Biết bên trong tính gì, đọc được paper, **chẩn đoán được khi kết quả sai** | "Vì sao cosine 2 sketch giống nhau chỉ đạt 0.6?" |
| **L4** | **Sửa được model** | Train/fine-tune, thiết kế loss, chọn hyperparameter có lý do | "Fine-tune FashionCLIP trên 2000 sketch nội bộ" |
| **L5** | Cải tiến phương pháp | Đề xuất kiến trúc/loss mới, kết quả tái lập được, viết được paper | Nghiên cứu |

**Với vị trí AI Engineer ở công ty product/tier-1 VN, ngưỡng tuyển thường là L3 đều tay + L4 ở
1–2 trục.** Không ai đòi L5 ở vị trí engineer (đó là research scientist).

---

## 2. Bảng điểm hiện tại của bạn — đọc từ code

```
                         L0   L1   L2   L3   L4   L5
                         │    │    │    │    │    │
Toán nền (đại số/xác suất)████████░░░░░░░░░░░░░░░      L2  ← suy ra từ việc code có
                                  ▲                        cosine/normalize nhưng
                                  bạn ở đây                không có phân tích không gian vector

Machine Learning cơ bản  ██████░░░░░░░░░░░░░░░░░░      L1–L2 ← trọng số scoring gõ tay,
                              ▲                              không có train/val split

Deep Learning            ████░░░░░░░░░░░░░░░░░░░░      L1  ← không có file .py nào
                            ▲                               import torch để train

Computer Vision          ████████░░░░░░░░░░░░░░░░      L2  ← dùng Docling + FashionCLIP
                                  ▲                         như hộp đen, có OpenCV crop

NLP / LLM                ████████████░░░░░░░░░░░░      L2–L3 ← prompt rất tốt, cấu trúc JSON
                                      ▲                       chặt, nhưng không đụng tokenizer

Multimodal / VLM         ████████████░░░░░░░░░░░░      L2–L3 ← dùng Qwen-VL rất sâu ở tầng
                                      ▲                       prompt, chưa động tầng image token

Embedding / Retrieval    ████████████░░░░░░░░░░░░      L2–L3 ← có Qdrant, multivector, rerank
                                      ▲                       nhưng chưa đo recall@k bao giờ

Fine-tuning              ██░░░░░░░░░░░░░░░░░░░░░░      L0–L1 ← chưa có dấu vết nào trong repo
                          ▲

Eval / đo lường          ██████████░░░░░░░░░░░░░░      L2  ← có golden set 20 style, có số
                                ▲                           89.1%/75.8%, nhưng chưa có CI,
                                                            chưa có per-field breakdown

Serving / MLOps          ████████████████░░░░░░░░      L3  ← MẠNH NHẤT. vLLM, TEI, llama.cpp,
                                          ▲                 GGUF Q6, AWQ, Jetson — đã đo perf
```

### Bằng chứng cho từng chấm điểm

| Trục | Bằng chứng **có** (đẩy điểm lên) | Bằng chứng **thiếu** (giữ điểm xuống) |
|---|---|---|
| Serving | `dockers/` có 15 compose file: vLLM, llama.cpp server, TEI, Jetson Orin; có cả `JETSON-ORIN-LLAMA-PERF.md` — tức là **đã benchmark thật** | Không thấy load test/throughput curve theo batch size |
| NLP | 12 thư mục prompt có version, có `complete_json()`, có structured output ép schema Pydantic | Không có chỗ nào đụng tokenizer, đếm token bằng `len(text)/4` — [`vlm_clients.py:27-30`](../../../motivesidp-ai-service/motives/src/vlm/core/vlm_clients.py) |
| Retrieval | Qdrant `MultiVectorConfig(MAX_SIM)` — hiểu ColBERT-style; có rerank service riêng | Chưa có payload index → sketch retrieval là **exhaustive scan**, không phải ANN (tài liệu nội bộ tự nhận) |
| ML | Có `bom_scorer.py`, `rule_engine.py`, composite score | Trọng số `0.40/0.25/0.25/0.10` hard-code; `late_fusion.py` docstring ghi thẳng *"Late fusion stub"*, trả `0.25`/`0.62` cứng |
| Fine-tuning | — | Không có `train.py`, không có `datasets` HF, không có checkpoint, không có W&B |
| Eval | `data/team_a_bom/golden/` 20 case; `v2_llm_eval/` 18 run có timestamp — **quy trình eval có kỷ luật** | Không có khoảng tin cậy, không có per-field confusion, không có inter-annotator agreement |

**Đọc bảng này thế nào:** bạn không yếu. Bạn **lệch**. Cột phải (serving, prompt, kiến trúc) đã
L3; cột trái (toán → ML → DL → fine-tune) còn L0–L2. Lệch kiểu này rất phổ biến với người đi từ
software engineering sang AI, và nó có một hệ quả cụ thể: **bạn sửa được mọi thứ quanh model,
nhưng không sửa được model.**

---

## 3. Bốn triệu chứng của việc thiếu tầng dưới — lấy từ chính repo

### Triệu chứng 1 — trọng số do người gõ tay

```python
# similar_techpack_scorer.py:29-33
embedding_weight: float = 0.40
silhouette_weight: float = 0.25
construction_weight: float = 0.25
detail_weight: float = 0.10
```

Bốn con số này quyết định thứ hạng style tương tự — tức là quyết định cả output của Team B.
Chúng đến từ đâu? Từ trực giác. Không có ai chứng minh `0.40` tốt hơn `0.35`.

> **Đây là bài toán learning-to-rank sơ cấp.** Có 20 case golden → đủ để fit 4 tham số bằng
> logistic regression hoặc grid search có cross-validation. Việc này **không cần GPU, không cần
> deep learning**, chỉ cần L2 machine learning. → [02](02-machine-learning-co-ban.md) §7, [07](07-embedding-retrieval-rag.md) §8

### Triệu chứng 2 — dùng model lệch domain mà không biết cách đo độ lệch

FashionCLIP (`Marqo/marqo-fashionCLIP`, ViT-B-16) được train bằng **ảnh chụp sản phẩm thời trang**
kèm caption. Bạn đưa vào **sketch line-art kỹ thuật** — đen trắng, nét mảnh, không có texture,
không có màu, không có người mẫu.

```
  Ảnh model được train           Ảnh bạn đưa vào
  ┌──────────────────┐          ┌──────────────────┐
  │  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓  │          │       ╱╲         │
  │  ▓▓ ảnh chụp ▓▓  │          │      ╱  ╲        │
  │  ▓▓  có màu  ▓▓  │   vs     │     │    │       │
  │  ▓▓ có texture▓▓ │          │     │ ── │       │
  │  ▓▓ có nền   ▓▓  │          │     └────┘       │
  └──────────────────┘          └──────────────────┘
   phân bố train                 phân bố inference
          └──────── domain gap ────────┘
```

Hệ quả: embedding vẫn ra vector 512 chiều, cosine vẫn tính được, **nhưng không gian đó không còn
phân tách tốt theo tiêu chí bạn cần** (silhouette, cổ áo, túi). Bạn không đo được điều này vì
chưa bao giờ vẽ phân bố cosine của cặp giống nhau vs cặp khác nhau.

> **Bài test 1 giờ, làm được ngay tuần này:** lấy 30 sketch, gán nhãn tay theo cặp
> "cùng style / khác style", vẽ 2 histogram cosine chồng lên nhau. Nếu 2 histogram chồng lấn
> nhiều → embedding gần như vô dụng cho bài toán này, và **mọi tinh chỉnh trọng số đều vô ích**.
> → [06](06-multimodal-vlm.md) §5, [09](09-eval-va-do-luong.md) §3

### Triệu chứng 3 — đếm token bằng `len(text)/4`

```python
# vlm_clients.py:27-30
def _estimate_text_tokens(text: str) -> int:
    return max(1, int(len(text) / 4))

# và với ảnh:
image_tokens = sum(max(1, int(len(base64.b64encode(img)) / 4)) for img in images)
```

Dòng thứ hai **sai về bản chất**, không phải sai số. Token ảnh của một VLM **không liên quan gì
tới độ dài chuỗi base64**. Nó được tính từ độ phân giải ảnh sau khi resize, chia thành patch:

```
      token_ảnh ≈ (H_sau_resize / patch) × (W_sau_resize / patch) / (merge_factor²)
```

Một ảnh PNG 2MB và một ảnh PNG 200KB **cùng độ phân giải** tốn **y hệt** số token. Ngược lại,
resize ảnh từ 1568px xuống 784px cắt **~75%** token ảnh — và token ảnh thường chiếm phần lớn chi
phí trong pipeline đọc techpack.

> Đây là cần gạt tiết kiệm token lớn nhất bạn đang có mà chưa kéo. → [06](06-multimodal-vlm.md) §6, [10](10-toi-uu-token-chi-phi.md) §4

### Triệu chứng 4 — có số accuracy nhưng không có khoảng tin cậy

Bạn có `89.1%` hard accuracy trên 256 ô golden và `75.8%` PTU trên 19 style. Câu hỏi: nếu lần sau
chạy ra `91.5%`, đó là **cải thiện thật** hay là **nhiễu**?

Với n=19 style, khoảng tin cậy 95% của tỉ lệ 75.8% rộng khoảng **±18.5 điểm phần trăm**
(51.2%–88.2%, theo khoảng Wilson — đã tính, xem [01](01-nen-tang-toan.md) §5.1). Nghĩa là: 75.8% → 85% trên 19 mẫu **không chứng minh được gì cả**.

```
  n=19,  p̂=0.758        khoảng tin cậy Wilson 95%
  ├─────────────────────●──────────────┤
 51.2%                                88.2%
  └──── mọi giá trị trong khoảng này đều "có thể là sự thật" ────┘

  n=100 → ±8.3 điểm      n=280 → ±5.0 điểm      n=19 → ±18.5 điểm
```

> Không biết điều này thì bạn sẽ tiêu hàng tuần để "tối ưu" những thứ chỉ là nhiễu.
> → [09](09-eval-va-do-luong.md) §5

---

## 4. Tự chấm — bảng để bạn điền

In ra hoặc copy vào file riêng. Chấm **trung thực**, tiêu chí là "làm được mà không tra Google".

| # | Trục | Câu hỏi tự test | L hiện tại | L mục tiêu 6 tháng | File |
|---|---|---|---|---|---|
| 1 | Toán nền | Giải thích vì sao normalize vector rồi thì dot product = cosine, và vì sao điều đó quan trọng với Qdrant | ___ | L3 | [01](01-nen-tang-toan.md) |
| 2 | ML | Cho 1 tập 20 mẫu, thiết kế quy trình chọn 4 trọng số sao cho không overfit | ___ | L3 | [02](02-machine-learning-co-ban.md) |
| 3 | DL | Viết backprop 1 MLP 2 lớp bằng numpy, khớp autograd | ___ | L3 | [03](03-deep-learning-co-ban.md) |
| 4 | CV | Giải thích convolution 3×3 stride 2 padding 1 làm gì với tensor (1,3,224,224) | ___ | L3 | [04](04-computer-vision.md) |
| 5 | NLP | Tính tay attention cho seq_len=3, d=4; giải thích vì sao chia √d | ___ | L3 | [05](05-nlp.md) |
| 6 | VLM | Tính số token ảnh cho input 1024×768 với patch 14, merge 2×2 | ___ | L3 | [06](06-multimodal-vlm.md) |
| 7 | Retrieval | Giải thích HNSW; vì sao multivector MAX_SIM khác single vector cosine | ___ | L4 | [07](07-embedding-retrieval-rag.md) |
| 8 | Fine-tune | Tính VRAM cần để LoRA một model 7B ở bf16, rank 16 | ___ | L4 | [08](08-finetuning.md) |
| 9 | Eval | Tính khoảng tin cậy bootstrap cho recall@5 trên 30 query | ___ | L4 | [09](09-eval-va-do-luong.md) |
| 10 | Token | Chỉ ra 3 chỗ trong pipeline của bạn tốn token nhất, kèm số | ___ | L4 | [10](10-toi-uu-token-chi-phi.md) |
| 11 | Serving | Tính VRAM cho Qwen3-VL 27B Q6 + KV cache ở context 32k | ___ | L4 | [11](11-mlops-serving.md) |

**Quy tắc đọc kết quả:**

- Trục nào **L0–L1** → bắt buộc đọc file tương ứng từ đầu, không nhảy cóc.
- Trục nào **L2** → đọc mục "cơ chế bên trong" của file đó, bỏ qua mục nhập môn.
- Trục nào **L3+** → chỉ đọc mục "ranh giới trung thực" và phần ứng dụng vào motivesidp.

---

## 5. Thứ tự tôi khuyên cho riêng bạn

Không phải thứ tự số file. Đây là thứ tự **theo ROI cho công việc hiện tại**:

```
  TUẦN 1-2   ┌─ 09 Eval §3,§5 ──────────┐  Làm trước tiên. Không đo được thì
             │  + 10 Token §4           │  mọi cải tiến sau đều mù. Và §4
             └──────────────────────────┘  của file 10 là tiền mặt ngay lập tức.
                          │
  TUẦN 3-7   ┌────────────▼─────────────┐  Nền. Chán nhưng không bỏ được.
             │  01 Toán → 02 ML → 03 DL │  Hết giai đoạn này bạn train được
             └──────────────────────────┘  model đầu tiên của đời mình.
                          │
  TUẦN 8-14  ┌────────────▼─────────────┐  Nhánh miền. Ưu tiên 06+07 vì đó
             │  06 VLM → 07 RAG →       │  là chỗ dự án đang chảy máu.
             │  04 CV → 05 NLP          │  04/05 đọc sau, bổ nền.
             └──────────────────────────┘
                          │
  TUẦN 15-20 ┌────────────▼─────────────┐  Đỉnh. Fine-tune FashionCLIP là
             │  08 Fine-tuning          │  project chứng minh năng lực rõ nhất.
             └──────────────────────────┘
                          │
  TUẦN 21-24 ┌────────────▼─────────────┐
             │  11 Serving + 13 Career  │
             └──────────────────────────┘

  XUYÊN SUỐT: 12 Project — mỗi 3 tuần đóng 1 project có số đo
```

---

## 6. Ba thứ KHÔNG nên học (với bạn, lúc này)

Tiết kiệm thời gian cũng quan trọng như học đúng.

| Thứ | Vì sao bỏ qua | Học lại khi nào |
|---|---|---|
| **Train LLM từ đầu (pretraining)** | Tốn hàng trăm nghìn USD GPU, không có công ty VN nào giao việc này cho AI Engineer. Bạn cần *hiểu* nó, không cần *làm* nó | Không bao giờ, trừ khi vào research lab |
| **RNN/LSTM chi tiết** | Đã bị Transformer thay thế gần hết trong NLP. Chỉ cần biết khái niệm để hiểu vì sao attention thắng | Đọc 30 phút ở [05](05-nlp.md) §4 là đủ |
| **Toán chứng minh (đo lường, giải tích hàm)** | Không dùng tới ở tầng engineer. Bạn cần tính được gradient, không cần chứng minh hội tụ | Khi làm research |

Ngược lại, **ba thứ dễ bị bỏ qua nhưng tuyệt đối đừng bỏ**: (1) thống kê cơ bản và khoảng tin cậy,
(2) cách xây dataset sạch, (3) cách debug một model học không xuống loss. Đây là 3 thứ phân biệt
AI Engineer thật với người đọc tutorial.

---

## 7. Ranh giới trung thực

- **Đã verify:** mọi trích dẫn code trong §3 lấy trực tiếp từ file trong `motivesidp-ai-service`
  ngày 11/09/2026. Kích thước prompt, tên model, cấu hình Qdrant đọc từ code và `docs/ai_technologies_overview.md`.
- **Là đánh giá chủ quan của tôi:** bảng điểm L ở §2. Tôi chấm từ dấu vết code, không phỏng vấn bạn.
  Có thể tôi chấm thấp ở trục bạn biết nhưng chưa viết vào repo này. **Bạn có quyền tự sửa điểm.**
- **Là ước lượng, chưa đo:** khoảng tin cậy ±19 điểm ở §3 tính theo xấp xỉ Wilson cho n=19, p=0.758 —
  đúng về bậc độ lớn, nhưng tôi chưa chạy bootstrap trên số thật của bạn.
- **Chưa kiểm chứng:** con số `89.1%` và `75.8%` tôi lấy từ tài liệu `katalon-prep/AI-STACK-INTERVIEW-ANSWERS.md`
  do bạn viết, không tự chạy lại được.
