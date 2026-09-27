# 08 · Fine-tuning

> **Câu hỏi file này trả lời:** khi nào fine-tune là câu trả lời đúng (hiếm hơn bạn nghĩ), cần bao
> nhiêu dữ liệu và GPU, và **fine-tune cái gì trước** trong hệ của bạn.
>
> **Thời lượng:** 20–25 giờ. **Yêu cầu trước:** [03](03-deep-learning-co-ban.md), [06](06-multimodal-vlm.md), [07](07-embedding-retrieval-rag.md).

---

## 1. Thang leo — fine-tune là bậc CUỐI, không phải bậc đầu

```
  Chi phí                                                            Hiệu quả
    │                                                      ┌──────────────────┐
    │                                                      │ ⑥ FINE-TUNE      │
    │                                              ┌───────┤   full / LoRA    │
    │                                              │ ⑤ RAG │   tuần–tháng     │
    │                                      ┌───────┤ vài   │   cần GPU + data │
    │                              ┌───────┤ ④ Tool│ ngày  └──────────────────┘
    │                      ┌───────┤ ③ Few-│ calling
    │              ┌───────┤ ② CoT/│ shot  │ 1-2 ngày
    │      ┌───────┤ ① Sửa │ decomp│ vài giờ
    │      │ ⓪ Đo  │ prompt│ 1 ngày
    │      │ trước │ vài h │
    └──────┴───────┴───────┴───────┴───────┴───────┴────────────────────────▶
                                                                       Thời gian

  LUẬT: chỉ leo lên bậc n+1 khi đã ĐO được bậc n không đủ.
        Đa số dự án nhảy thẳng từ ① lên ⑥ và lãng phí hàng tháng.
```

### 1.1 Bảng quyết định — fine-tune giải quyết gì và KHÔNG giải quyết gì

| Vấn đề | Fine-tune có giải quyết? | Giải pháp đúng |
|---|---|---|
| Model không biết **sự kiện/dữ liệu** của bạn | ❌ **Không** (và nhồi kiến thức vào weight rất kém hiệu quả) | **RAG** |
| Model không theo **format** bạn muốn | 🟡 Có, nhưng thừa | Constrained decoding ([05](05-nlp.md)§8.2) |
| Model không hiểu **thuật ngữ ngành** của bạn | ✅ **Có** | Fine-tune, hoặc few-shot nếu ít |
| Model **quá chậm / quá đắt** | ✅ **Có** — distill xuống model nhỏ | Fine-tune model nhỏ ⭐ |
| Embedding không phân tách domain của bạn | ✅ **Có** — đây là ca kinh điển | Contrastive fine-tune ⭐⭐⭐ |
| Model bịa (hallucinate) | 🟡 Giảm được | Grounding + verification quan trọng hơn |
| Cần suy luận nhiều bước tốt hơn | ❌ Khó với data nhỏ | Model lớn hơn, hoặc phân rã bài toán |

> **Hai hàng ⭐ là hai trường hợp fine-tune thật sự đúng cho bạn.** Mọi hàng khác, có đường rẻ hơn.

---

## 2. Bốn cách fine-tune — chi phí và yêu cầu

```
  ① FULL FINE-TUNE          ② LoRA                    ③ QLoRA                ④ PROMPT TUNING
  ┌──────────────┐          ┌──────────────┐          ┌──────────────┐       ┌──────────────┐
  │ 🔥🔥🔥🔥🔥🔥 │          │ ❄❄❄❄❄❄❄❄ │          │ ❄❄ 4-bit ❄❄ │       │ ❄❄❄❄❄❄❄❄ │
  │ mọi tham số  │          │ +🔥 adapter  │          │ +🔥 adapter  │       │ +🔥 vài token│
  └──────────────┘          └──────────────┘          └──────────────┘       └──────────────┘
   VRAM: ~16×P               VRAM: ~2.5×P              VRAM: ~0.75×P          VRAM: ~0.6×P
   (P = tỉ tham số, GB)
   7B → ~112 GB              7B → ~18 GB               7B → ~6 GB             7B → ~5 GB
   ❌ ngoài tầm              ✅ 1×A100/4090            ✅ 1×3090/4090         ít dùng
   Chất lượng: 100%          ~97-99%                   ~95-98%                ~80%
```

### 2.1 Toán VRAM cho full fine-tune — vì sao con số 16× xuất hiện

```
  Với model P tỉ tham số, train bf16 + optimizer AdamW:

    weights            2 byte/tham số   →  2P GB
    gradients          2 byte/tham số   →  2P GB
    Adam moment 1      4 byte/tham số   →  4P GB
    Adam moment 2      4 byte/tham số   →  4P GB
    master weights fp32 4 byte/tham số  →  4P GB
                                          ──────
                                          16P GB   + activation (phụ thuộc batch/seq)

  7B  → ~112 GB  → cần ≥2×A100 80GB
  27B → ~432 GB  → cần ≥6×A100 80GB
```

→ Đây là lý do LoRA tồn tại, và là lý do **bạn sẽ không bao giờ full fine-tune Qwen 27B**.

---

## 3. LoRA — cơ chế, không phải phép màu

### 3.1 Ý tưởng

```
   Thay vì cập nhật W (d×k, hàng triệu tham số),
   ta ĐÓNG BĂNG W và học thêm hai ma trận gầy:

        h = W·x  +  (B·A)·x · (α/r)
            │         │
            │         └── B: (d×r),  A: (r×k),  với r ≪ min(d,k)
            └── đóng băng

   Ví dụ d=k=4096, r=16:
     W       = 4096 × 4096 = 16,777,216 tham số   (đóng băng)
     A + B   = 16×4096 + 4096×16 = 131,072        (học)
                                    ────────────
     → chỉ học 0.78% số tham số
```

Lúc suy luận có thể **merge** `W' = W + BA·(α/r)` → **không thêm độ trễ nào**.

### 3.2 Ba hyperparameter và cách chọn

| Tham số | Mặc định tốt | Tăng lên thì | Ghi chú |
|---|---|---|---|
| `r` (rank) | **16** | Dung lượng học ↑, dễ overfit ↑ | 8 cho task hẹp, 64 cho task rộng/nhiều data |
| `alpha` | **2×r** (32) | Ảnh hưởng của adapter ↑ | Tỉ lệ `α/r` mới là thứ quan trọng |
| `target_modules` | **mọi lớp linear** | Chất lượng ↑, VRAM ↑ | Chỉ `q_proj,v_proj` là rẻ nhưng yếu hơn rõ rệt |

```python
from peft import LoraConfig, get_peft_model

config = LoraConfig(
    r=16, lora_alpha=32, lora_dropout=0.05,
    target_modules=["q_proj","k_proj","v_proj","o_proj",
                    "gate_proj","up_proj","down_proj"],   # đủ cả, không chỉ q/v
    task_type="CAUSAL_LM",
)
model = get_peft_model(base_model, config)
model.print_trainable_parameters()
# → trainable: 40,370,176 || all: 6,778,318,848 || trainable%: 0.5956
```

### 3.3 QLoRA thêm gì

QLoRA = LoRA + **base model nén 4-bit (NF4)** + double quantization + paged optimizer.
Kết quả: fine-tune model 7B trên **một GPU 24GB** (3090/4090).

**Đánh đổi:** chậm hơn LoRA thường ~30%, chất lượng thấp hơn chút ít. Với người học và với dự án
cỡ vừa, đánh đổi này gần như luôn đáng.

---

## 4. Dữ liệu — thứ quyết định, không phải thuật toán

### 4.1 Bao nhiêu là đủ

| Task | Số mẫu tối thiểu | Điểm ngọt | Ghi chú |
|---|---|---|---|
| Học **format/style** output | 50–100 | 500 | Ít nhất trong mọi loại |
| Học **thuật ngữ ngành** | 500 | 2,000–5,000 | |
| **Embedding** contrastive | 1,000 cặp | 10,000+ cặp | Cặp rẻ hơn nhiều so với mẫu SFT ⭐ |
| **Reranker** cross-encoder | 2,000 bộ ba | 20,000+ | |
| **Distill** model lớn → nhỏ | 5,000 | 50,000+ | Nhãn sinh tự động từ model lớn ⭐ |
| Dạy **năng lực suy luận mới** | 50,000+ | 500,000+ | Gần như ngoài tầm dự án thường |

> **1,000 mẫu sạch thắng 10,000 mẫu bẩn.** Đây không phải khẩu hiệu — nhãn sai dạy model học sai
> một cách rất hiệu quả. Thời gian nên dồn vào làm sạch, không phải vào tăng số lượng.

### 4.2 Bốn nguồn dữ liệu của bạn — xếp theo chi phí

| Nguồn | Có sẵn không | Cỡ | Dùng cho |
|---|---|---|---|
| **Golden BOM Excel** (`data/team_a_bom/golden/`, 20 case) | ✅ có | ~1000 dòng | Cặp (query → vật liệu đúng) cho embedding/reranker |
| **Techpack lịch sử đã index** | ✅ có | vài nghìn sketch | Cặp positive cho contrastive (cùng style = positive) |
| **Log production** | ✅ có (Langfuse) | tăng dần | Distill: input → output của model lớn |
| **Gán nhãn tay** | ❌ phải làm | tùy ngân sách | Attribute (lapel/pocket/vent) |

### 4.3 Chống rò rỉ — nhắc lại vì nó giết nhiều dự án fine-tune

Chia **theo style**, không theo file. Xem [02](02-machine-learning-co-ban.md)§2.2. Với fine-tune,
rò rỉ đặc biệt nguy hiểm vì model có đủ dung lượng để **nhớ thuộc lòng** — bạn sẽ thấy metric
tuyệt vời trên test và thảm họa trên production.

---

## 5. SFT một LLM — quy trình

### 5.1 Định dạng dữ liệu

```json
{"messages": [
  {"role": "system", "content": "Bạn trích BOM từ mô tả techpack."},
  {"role": "user", "content": "..."},
  {"role": "assistant", "content": "{\"items\": [...]}"}
]}
```

**Quan trọng:** loss chỉ tính trên phần `assistant` (masking). Nếu tính cả prompt, model sẽ học
sinh ra cả prompt — lỗi kinh điển khi tự viết training loop. Thư viện (TRL `SFTTrainer`) xử lý
việc này qua `DataCollatorForCompletionOnlyLM`.

### 5.2 Hyperparameter khởi điểm

| Tham số | Giá trị | Ghi chú |
|---|---|---|
| epochs | **1–3** | >3 gần như chắc chắn overfit với data nhỏ |
| learning rate | **1e-4 – 2e-4** (LoRA) | Full FT thì 1e-5 |
| batch size hiệu dụng | 16–64 | Dùng gradient accumulation nếu VRAM ít |
| max_seq_len | vừa đủ | Dài gấp đôi → VRAM gấp đôi |
| warmup | 3–5% tổng step | |
| lr scheduler | cosine | |

### 5.3 Catastrophic forgetting

Fine-tune quá mạnh vào một task hẹp → model **quên** năng lực chung.

```
  epoch 0 ──────▶ epoch 1 ──────▶ epoch 2 ──────▶ epoch 5
  task riêng: 60%   82%             88%             91%
  năng lực chung: 100%  98%          92%             71%  ← đã hỏng
                                      ▲
                              dừng quanh đây
```

**Cách phát hiện:** giữ một **tập eval "năng lực chung"** (vài chục prompt không liên quan task)
và chạy nó ở mỗi checkpoint. Nếu nó tụt, dừng lại. Hầu như không ai làm bước này, và đó là lý do
nhiều model fine-tune "giỏi hơn trên benchmark riêng, tệ hơn khi dùng thật".

---

## 6. ⭐ Fine-tune embedding — project ROI cao nhất của bạn

Đây là chỗ fine-tune thật sự là câu trả lời đúng, vì [06](06-multimodal-vlm.md)§1.3 đã chỉ ra:
thông tin bạn cần **không tồn tại** trong embedding hiện tại.

### 6.1 Thiết kế

```
   DỮ LIỆU: cặp positive (2 sketch cùng style, khác view/khác biến thể)
            negative: lấy trong batch (in-batch) + HARD negative

   ┌──────────────────────────────────────────────────────────────┐
   │ anchor: sketch FRONT của CAMP1_144                           │
   │ positive: sketch FRONT của CAMP1_497  (cùng style CAMP1)     │
   │ hard negative: sketch FRONT của CNRA1 (CÙNG subgroup jacket) │  ← chìa khóa
   │ easy negative: sketch của một chiếc váy                      │  ← gần vô dụng
   └──────────────────────────────────────────────────────────────┘

   MODEL: FashionCLIP image tower, ĐÓNG BĂNG hầu hết
          🔥 mở khóa: 2 block ViT cuối + projection head
          hoặc dùng LoRA trên ViT (peft hỗ trợ)

   LOSS: InfoNCE (xem 06 §1.2), temperature 0.05–0.07
```

### 6.2 Vì sao hard negative là chìa khóa

Nhắc lại [04](04-computer-vision.md)§7.2 nhưng cụ thể hơn:

```
  Batch chỉ có easy negative              Batch có hard negative
  ┌────────────────────────┐              ┌────────────────────────┐
  │ loss về 0 sau 50 step  │              │ loss giảm chậm, đều    │
  │ recall@5 KHÔNG tăng    │              │ recall@5 tăng thật     │
  │ → model đã "giỏi" việc │              │ → model buộc phải học  │
  │   phân biệt jacket vs  │              │   chi tiết phân biệt   │
  │   váy — việc nó ĐÃ giỏi│              │   jacket với jacket    │
  └────────────────────────┘              └────────────────────────┘
```

**Cách lấy hard negative gần như miễn phí:** dùng chính model hiện tại. Với mỗi anchor, retrieve
top-20, loại bỏ các doc thật sự là positive, **những cái còn lại chính là hard negative** — chúng
là thứ model đang nhầm. Đây là "hard negative mining" và nó nên được làm lại sau mỗi vài epoch.

### 6.3 Ngân sách batch — hạn chế thật cần biết trước

InfoNCE cần batch lớn. Với VRAM hạn chế, hai kỹ thuật cứu:

| Kỹ thuật | Làm gì | Chi phí |
|---|---|---|
| **GradCache** | Chia batch làm nhiều mảnh, tích lũy gradient mà vẫn có negative của cả batch | chậm ~2× |
| **Memory bank / MoCo queue** | Giữ hàng đợi embedding từ các batch trước làm negative | embedding hơi cũ |

Với vài nghìn sketch và batch 128–256, một GPU 24GB là đủ cho ViT-B/16 khi chỉ mở 2 block cuối.

### 6.4 Kế hoạch 6 tuần — có cổng kiểm tra

| Tuần | Việc | Cổng để đi tiếp |
|---|---|---|
| **0** | **Đo separation hiện tại** ([06](06-multimodal-vlm.md)§2.2) | Cohen's d < 1.0 → đáng làm tiếp. **d > 2.0 → DỪNG, embedding không phải vấn đề** |
| 1 | Dựng tập eval: 50+ query có ground truth, đo recall@1/@5 + CI | Có baseline có CI |
| 2 | Dựng tập train: cặp positive từ style, hard negative từ top-20 | ≥1000 cặp, đã chia theo style |
| 3 | Linear probe trước (chỉ train projection head) | Nếu linear probe đã đủ → **dừng, xong sớm** |
| 4 | Mở 2 block cuối + InfoNCE, train, theo dõi recall trên val mỗi epoch | recall val tăng |
| 5 | Hard negative mining vòng 2, train lại | recall tăng tiếp |
| 6 | **Shadow mode** trên production 2 tuần | Mức tăng **vượt CI** của baseline → mới đổi |

> **Kỳ vọng trung thực:** tôi **không biết** con số cải thiện. Với domain gap rõ rệt như của bạn,
> fine-tune contrastive thường cho mức tăng lớn — nhưng "thường" không phải "chắc chắn", và tôi
> chưa chạy trên dữ liệu của bạn. **Tuần 0 và tuần 1 tồn tại chính để bạn biết trước khi đầu tư
> 5 tuần còn lại.** Đừng bỏ chúng.

---

## 7. Fine-tune reranker — dễ hơn, rẻ hơn, thường bị bỏ qua

Cross-encoder dễ fine-tune hơn bi-encoder rất nhiều: nó là bài binary classification thuần.

```python
# Dữ liệu: (query, doc, label)  label ∈ {0, 1}
# Positive: từ golden BOM  ·  Negative: top-k của retrieval hiện tại mà KHÔNG đúng
from sentence_transformers import CrossEncoder, InputExample
model = CrossEncoder("BAAI/bge-reranker-base", num_labels=1)
model.fit(train_dataloader, epochs=2, warmup_steps=100,
          optimizer_params={"lr": 2e-5})
```

| | Fine-tune embedding | Fine-tune reranker |
|---|---|---|
| Dữ liệu cần | cặp positive + hard negative | (query, doc, nhãn nhị phân) |
| Khó | trung bình (cần batch lớn, InfoNCE) | **dễ** (binary classification) |
| Ảnh hưởng | recall — **trần** của hệ thống | precision/MRR — thứ hạng |
| Phải reindex không | ✅ **có** — mọi vector phải tính lại | ❌ **không** |
| Thời gian | tuần | **ngày** |

> **Khuyến nghị thứ tự:** nếu bảng chẩn đoán ở [07](07-embedding-retrieval-rag.md)§7.3 cho ra
> *"recall cao, MRR thấp"* → **fine-tune reranker trước**. Rẻ hơn nhiều, không phải reindex,
> ra kết quả trong vài ngày. Chỉ fine-tune embedding khi recall tầng 1 mới là nút thắt.

---

## 8. Distillation — con đường cắt chi phí lớn nhất

```
   TEACHER                              STUDENT
   Qwen3.6-VL 27B          ────────▶    Model nhỏ (0.5B–3B) hoặc classifier
   đắt, chậm, chất lượng cao            rẻ 30-100×, nhanh 10-50×

   Dữ liệu train: CHÍNH LÀ LOG PRODUCTION CỦA BẠN
   (input, output của teacher) — bạn đã có sẵn trong Langfuse
```

### 8.1 Vì sao đây là hướng thực tế nhất để cắt chi phí

- **Nhãn miễn phí** — teacher đã sinh ra chúng trong lúc chạy production.
- **Phân bố khớp hoàn hảo** — data train chính là data production, không có lệch phân bố.
- **Đo được rõ ràng** — so student với teacher trên cùng golden set.
- **Triển khai an toàn** — định tuyến: student xử lý ca dễ, ca khó escalate lên teacher (§8.2).

### 8.2 Kiến trúc định tuyến — cách triển khai không rủi ro

```
                      ┌──────────────┐
   request ──────────▶│  STUDENT     │
                      │  (nhỏ, rẻ)   │
                      └──────┬───────┘
                             │ confidence
              ┌──────────────┴──────────────┐
         cao  │                             │  thấp
              ▼                             ▼
      ┌───────────────┐            ┌─────────────────┐
      │ Trả kết quả   │            │ TEACHER 27B     │
      │ ~70-80% lưu   │            │ ~20-30% lưu     │
      │ lượng         │            │ lượng           │
      └───────────────┘            └─────────────────┘

   Chi phí trung bình giảm mạnh, chất lượng ca khó GIỮ NGUYÊN.
   Cần: một confidence signal đáng tin → logprob ([01] §4.4) + calibration ([02] §7.3)
```

> **Đây là mẫu kiến trúc mà một AI Engineer ở tier-1 được kỳ vọng biết thiết kế.** Nó cũng là
> câu chuyện phỏng vấn rất mạnh vì có số đo hai chiều: chi phí giảm bao nhiêu, chất lượng giữ
> được bao nhiêu. → [13](13-lo-trinh-ai-engineer-vsf-fpt.md)§5

### 8.3 Ca dễ nhất để bắt đầu

Phân loại **FRONT/BACK** (hiện dùng VLM 27B):
- Teacher đã chạy hàng nghìn lần → có sẵn hàng nghìn nhãn.
- Student = `nn.Linear(512, 2)` trên embedding đã có trong Qdrant.
- Train trong 1 phút trên CPU.
- Nếu đạt ~99% so với teacher → cắt được một lời gọi VLM trên mỗi sketch.

Đây cũng chính là bài tập ở [03](03-deep-learning-co-ban.md)§9. **Một bài tập, một cải tiến
production thật.**

---

## 9. Bảng tổng hợp — fine-tune gì trước

| Thứ tự | Fine-tune cái gì | Điều kiện kích hoạt | Nỗ lực | Rủi ro |
|---|---|---|---|---|
| **1** | Distill FRONT/BACK classifier | Luôn đáng — có sẵn data và embedding | 1 ngày | Rất thấp |
| **2** | Reranker (cross-encoder) | recall@50 cao nhưng MRR thấp | 1 tuần | Thấp — không reindex |
| **3** | Embedding sketch (InfoNCE) | Cohen's d < 1.0 | 6 tuần | Trung bình — phải reindex |
| **4** | Classifier lọc trang có BOM | Muốn cắt token ảnh ở thượng nguồn | 1 tuần | Thấp — nhưng FN làm mất dữ liệu, cần ngưỡng thận trọng |
| **5** | Distill VLM extraction → model nhỏ | Sau khi 1–4 đã xong, có ≥5000 log sạch | 2 tháng | Cao |
| ❌ | Fine-tune Qwen 27B | — | — | **Không làm.** Không đủ GPU, không đủ data, và không phải nút thắt |

---

## 10. Ranh giới trung thực

- **Đã verify:** toán VRAM §2.1 là phép tính chuẩn cho AdamW + mixed precision (2+2+4+4+4 byte).
  Con số LoRA `trainable%: 0.5956` ở §3.2 là output điển hình cho Llama-7B với 7 target module —
  tôi **không chạy lại**, coi đó là minh họa.
- **Là ước lượng, không phải đo:** mọi con số ở §4.1 (bao nhiêu mẫu là đủ) là quy tắc ngón tay
  phổ biến trong ngành, **không phải kết quả thí nghiệm trên dữ liệu của bạn**. Chúng có thể
  lệch 2–3 lần tùy độ khó task.
- **KHÔNG hứa hẹn con số cải thiện:** tôi cố tình không viết "fine-tune sẽ tăng X%". Tôi không
  biết, và bất kỳ ai nói con số đó mà chưa nhìn dữ liệu của bạn thì đang đoán. Tuần 0/tuần 1
  trong §6.4 là cách duy nhất để biết.
- **Chưa kiểm chứng:** số lượng cặp lấy được từ golden BOM (kế thừa giả định từ
  [07](07-embedding-retrieval-rag.md)§7.1 — hãy kiểm nó trước).
- **Đơn giản hóa:** §8 bỏ qua các kỹ thuật distillation nâng cao (KL trên phân bố logit,
  intermediate-layer matching). Với bài toán của bạn, distill "input → output text" là đủ và
  đơn giản hơn nhiều.
- **Không bàn tới:** RLHF/DPO/GRPO (alignment) — chúng cần dữ liệu preference và không giải quyết
  vấn đề nào bạn đang có. Quay lại khi cần dạy model "phong cách trả lời", không phải "độ chính xác".
