# ai-learning — từ "người gọi GenAI API" thành AI Engineer

> **Viết cho:** bạn — tech lead `motivesidp-ai-service`, đã build được hệ thống AI chạy production
> (5 model sau HTTP endpoint, Qdrant, VLM, agentic pipeline), nhưng **chưa từng train một model nào**.
>
> **Bằng chứng nền tảng:** toàn bộ ví dụ trong bộ tài liệu này lấy từ code thật của
> `motivesidp-ai-service` (đọc ngày 11/09/2026) — không phải ví dụ sách giáo khoa. Mọi con số có
> đường dẫn file để bạn tự verify.

---

## 0. Đọc file này trong 90 giây

**Chẩn đoán ngắn gọn:** bạn đang ở **tầng orchestration** — biết ghép model thành hệ thống, biết
prompt, biết đo bằng golden set. Thứ bạn thiếu là **tầng bên trong model**: không biết loss nào
đang được tối ưu, không biết vì sao embedding của sketch line-art lại tệ, không biết đổi từ
`temperature=0.1` sang fine-tune thì cần bao nhiêu dữ liệu.

**Hệ quả cụ thể trong dự án của bạn** — 4 chỗ đang "kẹt" vì thiếu tầng đó:

| Chỗ kẹt | Hiện tại | Thiếu kiến thức gì | File giải |
|---|---|---|---|
| Trọng số scoring `0.40/0.25/0.25/0.10` do người gõ tay | [`similar_techpack_scorer.py:29-33`](../../../motivesidp-ai-service/motives/src/application_platform/scoring/similar_techpack_scorer.py) | Learning-to-rank — trọng số phải **học** từ golden set, không phải đoán | [02](02-machine-learning-co-ban.md) · [07](07-embedding-retrieval-rag.md) |
| FashionCLIP train trên **ảnh sản phẩm**, dùng để so **sketch line-art** | `docs/ai_technologies_overview.md §6.2` | Domain gap + contrastive fine-tune (InfoNCE/triplet) | [06](06-multimodal-vlm.md) · [08](08-finetuning.md) |
| Token đắt vì mỗi call VLM nhét cả ảnh base64 | [`vlm_clients.py:22-38`](../../../motivesidp-ai-service/motives/src/vlm/core/vlm_clients.py) | Image token math — token ảnh tỉ lệ với **độ phân giải**, không phải kích thước file | [10](10-toi-uu-token-chi-phi.md) |
| `late_fusion.py` vẫn là stub trả số cứng `0.25/0.62` | [`late_fusion.py:26-34`](../../../motivesidp-ai-service/motives/src/application_platform/scoring/late_fusion.py) | Calibration + fusion có học | [02](02-machine-learning-co-ban.md) · [09](09-eval-va-do-luong.md) |

**Cách dùng bộ tài liệu này:** đừng đọc tuần tự từ đầu tới cuối. Đọc [00](00-chan-doan-nang-luc.md)
để tự chấm điểm, rồi nhảy vào đúng nhánh đang chặn bạn. Mỗi file đều tự đứng được.

> **Bổ sung 17/09/2026 — một đính chính quan trọng cho chẩn đoán ở trên.** Sau khi rà toàn bộ code
> path gọi LLM/VLM để viết
> [`token_gpu_optimization_research.md`](../../../motivesidp-ai-service/docs/token_gpu_optimization_research.md),
> hoá ra **phần lớn thứ đang chặn công việc thật KHÔNG nằm bên trong model** — nó nằm ở đo lường,
> information retrieval, **CV cổ điển** và kỹ thuật hệ thống. Trong 8 nhóm kiến thức cần thiết,
> **chỉ 1 nhóm là ML/DL**. Bốn nhóm mà 16 file đầu chưa phủ đã được thêm ở
> [**15**](15-nen-tang-can-nam.md) → [16](16-inference-internals.md) · [17](17-cv-co-dien.md) ·
> [18](18-data-lineage-va-thu-nhan.md) · [19](19-he-thong-co-llm.md). **Đọc [15](15-nen-tang-can-nam.md)
> ngay sau [00](00-chan-doan-nang-luc.md).**

---

## 1. Bản đồ 20 file

```
                          ┌──────────────────────────────┐
                          │ 00 · CHẨN ĐOÁN NĂNG LỰC      │  ← bắt đầu ở đây
                          │ tự chấm 9 trục, L0→L5        │
                          └──────────────┬───────────────┘
                                         │
        ┌────────────────────────────────┼────────────────────────────────┐
        │                                │                                │
   ═══ TẦNG NỀN ═══               ═══ TẦNG MIỀN ═══             ═══ TẦNG KỸ SƯ ═══
        │                                │                                │
  ┌─────▼─────┐                    ┌─────▼─────┐                    ┌─────▼─────┐
  │ 01 Toán   │                    │ 04 CV     │                    │ 09 Eval   │
  │ 02 ML     │───────────────────▶│ 05 NLP    │───────────────────▶│ 10 Token  │
  │ 03 DL     │                    │ 06 VLM    │                    │ 11 Serving│
  └───────────┘                    │ 07 RAG    │                    └───────────┘
   3-5 tuần                        │ 08 Finetune│                     3-4 tuần
   KHÔNG BỎ QUA                    └───────────┘                    ROI cao nhất
                                     6-8 tuần                       cho công việc
                                                                    hiện tại
                                         │
                          ┌──────────────▼───────────────┐
                          │ 12 · 12 PROJECT TRÊN CHÍNH   │
                          │      motivesidp — có data    │
                          │      thật, có metric thật    │
                          └──────────────┬───────────────┘
                                         │
                          ┌──────────────▼───────────────┐
                          │ 13 · Lộ trình AI Engineer    │
                          │ 14 · Tài nguyên theo thứ tự  │
                          └──────────────────────────────┘

   ═══ TẦNG NỀN KHÔNG-PHẢI-ML (bổ sung 17/09/2026) ═══
   ┌────────────────────────────────────────────────────────────────┐
   │ 15 · NỀN TẢNG CẦN NẮM — bản đồ 8 nhóm   ← đọc trước 16-19      │
   ├──────────────┬──────────────┬──────────────┬──────────────────┤
   │ 16 Inference │ 17 CV cổ     │ 18 Data      │ 19 Hệ thống      │
   │    internals │    điển      │    lineage   │    có LLM        │
   │ prefill/MoE  │ Hough/OCR    │ thu nhãn     │ tool/injection   │
   └──────────────┴──────────────┴──────────────┴──────────────────┘
      Sinh ra từ docs/token_gpu_optimization_research.md — 4 nhóm
      kiến thức mà 16 file đầu chưa phủ, và đang chặn công việc thật
```

| File | Nội dung | Đọc khi nào |
|---|---|---|
| [**00-chan-doan-nang-luc.md**](00-chan-doan-nang-luc.md) ⭐ | Thang L0–L5 cho 9 trục · tự chấm · chẩn đoán dựa trên code thật của bạn | **Đầu tiên, luôn luôn** |
| [01-nen-tang-toan.md](01-nen-tang-toan.md) | Chỉ phần toán **thật sự dùng**: dot product ↔ cosine similarity, ma trận ↔ linear layer, gradient, softmax/cross-entropy | Trước 02–03 |
| [02-machine-learning-co-ban.md](02-machine-learning-co-ban.md) | Loss · gradient descent · overfit · metrics · calibration · learning-to-rank | Nền bắt buộc |
| [03-deep-learning-co-ban.md](03-deep-learning-co-ban.md) | Backprop tính tay · PyTorch training loop · optimizer · normalization · công thức debug | Sau 02 |
| [04-computer-vision.md](04-computer-vision.md) | Convolution · CNN · ResNet · ViT · detection · segmentation · OCR/layout · metric learning | Nhánh CV |
| [05-nlp.md](05-nlp.md) | BPE tokenizer (gốc của chi phí token) · attention tính tay · Transformer · BERT vs GPT · cross-encoder | Nhánh NLP |
| [06-multimodal-vlm.md](06-multimodal-vlm.md) | CLIP/InfoNCE · kiến trúc VLM · **image token math** · vì sao FashionCLIP lệch domain | Nhánh multimodal |
| [07-embedding-retrieval-rag.md](07-embedding-retrieval-rag.md) | Không gian vector · HNSW · ColBERT MaxSim · hybrid search · RRF · recall@k/nDCG | Nhánh RAG |
| [08-finetuning.md](08-finetuning.md) ⭐ | Khi nào fine-tune · SFT · **LoRA/QLoRA** · contrastive fine-tune embedding · distillation | Câu hỏi lớn nhất của bạn |
| [09-eval-va-do-luong.md](09-eval-va-do-luong.md) ⭐ | Golden set · per-field accuracy · bootstrap CI · shadow mode · LLM-as-judge và cái bẫy của nó | ROI cao nhất |
| [10-toi-uu-token-chi-phi.md](10-toi-uu-token-chi-phi.md) ⭐ | Token đi đâu · nén prompt · prefix cache · routing · **cắt token ảnh** | ROI cao nhất |
| [11-mlops-serving.md](11-mlops-serving.md) | Toán VRAM · quantization Q4/Q6/AWQ · vLLM vs llama.cpp vs TEI · throughput | Bạn đã làm 60% rồi |
| [12-thuc-hanh-tren-motivesidp.md](12-thuc-hanh-tren-motivesidp.md) ⭐ | 12 project xếp theo ROI, trên **chính data bạn đang có** | Song song mọi file |
| [13-lo-trinh-ai-engineer-vsf-fpt.md](13-lo-trinh-ai-engineer-vsf-fpt.md) | Giải phẫu JD · gap · kế hoạch 6 tháng · bộ câu hỏi phỏng vấn | Khi tính chuyện nhảy việc |
| [14-tai-nguyen.md](14-tai-nguyen.md) | Khóa học/sách/paper **theo đúng thứ tự đọc**, kèm lý do | Tra cứu |
| [**15-nen-tang-can-nam.md**](15-nen-tang-can-nam.md) ⭐ | **Bản đồ 8 nhóm kiến thức** thật sự cần · chỉ 1/8 là ML/DL · thứ tự học · 8 câu tự kiểm | **Đọc trước 16–19** |
| [16-inference-internals.md](16-inference-internals.md) ⭐ | **prefill vs decode** · KV cache · prefix caching · **MoE vs dense & active params** · quantization · batching · tracing · Matryoshka | Khi muốn hệ chạy nhanh hơn |
| [17-cv-co-dien.md](17-cv-co-dien.md) ⭐ | Hough · morphology · connected components · template matching · IoU/NMS · **OCR như công cụ hình học** · cây quyết định "CV hay model" | Khi thấy VLM làm việc của CV |
| [18-data-lineage-va-thu-nhan.md](18-data-lineage-va-thu-nhan.md) | Provenance & lineage · **tìm nhãn ẩn trong luồng nghiệp vụ** · event log · dataset versioning · **4 kiểu rò rỉ** | Trước khi tính chuyện fine-tune |
| [19-he-thong-co-llm.md](19-he-thong-co-llm.md) | Xương sống tất định + lớp xác suất · **4 mức structured output** · tool contract · vòng an toàn · **prompt injection** · khi nào KHÔNG cần agent | Khi xây chat agent |

---

## 2. Lộ trình 24 tuần — bản rút gọn

Giả định **8–10 giờ/tuần** (2 buổi tối + 1 buổi cuối tuần). Đây là nhịp bền, không phải nhịp sprint.

```
Tuần   1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24
      ─────────────────────────────────────────────────────────────────────────
01-03 ████████████                                                              Toán+ML+DL
04    ................████████                                                  CV
05    ................████████                                                  NLP
06    ........................██████                                            VLM
07    ..............................██████                                      RAG sâu
08    ....................................████████                              Fine-tune
09    ......██................................██████                            Eval
10    ..........██................................████                          Token
11    ................................................████                      Serving
12    ......▲......▲......▲......▲......▲......▲......▲......▲......▲            Project
13-14 ..................................................................██████  Career
      ─────────────────────────────────────────────────────────────────────────
       ↑ TUẦN 6: đã đủ để tự train                ↑ TUẦN 16: đã đủ để
         1 classifier trên sketch của bạn           fine-tune FashionCLIP thật
```

**Ba mốc kiểm chứng — không đạt thì đừng đi tiếp:**

| Mốc | Tuần | Bài kiểm tra tự chấm |
|---|---|---|
| **M1 — Biết model học thế nào** | 6 | Viết backprop cho 1 MLP 2 lớp **bằng numpy thuần**, khớp với `torch.autograd` tới 1e-6. Không nhìn tài liệu |
| **M2 — Biết đo** | 12 | Xây golden set 30 style trên sketch thật, báo cáo recall@5 **kèm khoảng tin cậy bootstrap 95%** |
| **M3 — Biết dạy model** | 18 | Fine-tune FashionCLIP bằng InfoNCE trên sketch nội bộ, chứng minh recall@5 tăng **vượt khoảng tin cậy** của baseline |

M3 mà đạt thì bạn không còn là "người gọi API" nữa — đó là một dòng CV thật, có số.

---

## 3. Ba nguyên tắc của bộ tài liệu này

Kế thừa nguyên tắc của workspace `algorithms-prep`:

1. **Chạy được thì phải chạy.** Mọi đoạn code trong bộ này đều là code chạy được, không phải
   pseudo-code. Chỗ nào tôi chưa chạy thì có ghi rõ `[chưa chạy]`.
2. **Phân biệt rõ đã kiểm chứng / suy luận / chưa đo.** Mỗi file có mục *"ranh giới trung thực"*
   ở cuối. Con số ước lượng luôn ghi là ước lượng.
3. **Mỗi kỹ thuật kèm một câu "đánh đổi là…"** — fine-tune không miễn phí, quantization không
   miễn phí, rerank không miễn phí.

---

## 4. Ranh giới trung thực của chính file này

- **Đã verify bằng cách đọc code:** kiến trúc `motivesidp-ai-service`, tên model, trọng số scoring,
  kích thước prompt, cấu hình Qdrant, cách gọi VLM. Đường dẫn file trong bảng §0 là thật.
- **Là suy luận của tôi, bạn cần tự kiểm:** ROI của từng project ở file 12, ước lượng thời gian
  24 tuần, mức tăng accuracy kỳ vọng khi fine-tune.
- **Tôi KHÔNG verify được:** "VSF" trong yêu cầu của bạn là công ty nào (VinSmart Future? một
  công ty khác?). File 13 viết theo JD AI Engineer **điển hình ở tier-1 VN** (FPT Software AI
  Center, FPT AI, VNPT AI, VinAI, Viettel AI). **Gửi tôi JD thật thì tôi viết lại chính xác phần đó.**
- **Chưa đo:** tôi chưa chạy pipeline của bạn, nên mọi con số về token/accuracy hiện tại là đọc
  từ code + tài liệu nội bộ, không phải tôi tự benchmark.
