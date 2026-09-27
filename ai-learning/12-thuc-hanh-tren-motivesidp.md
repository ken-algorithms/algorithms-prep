# 12 · 12 project thực hành trên chính motivesidp

> **Nguyên tắc:** không làm project đồ chơi. Mỗi project dưới đây dùng **dữ liệu thật bạn đang có**,
> tạo ra **một con số thật**, và **cải thiện được hệ thống thật**. Học và làm việc là cùng một hành động.
>
> **Cách dùng:** làm theo thứ tự. Mỗi project đóng gói thành một file
> `docs/experiments/NN-ten-project.md` với đúng bố cục ở §0.

---

## 0. Bố cục báo cáo chuẩn — dùng cho cả 12 project

```markdown
# Thí nghiệm NN — <tên>

## Câu hỏi
Một câu. Trả lời được bằng số.

## Giả thuyết
Tôi kỳ vọng <X> vì <lý do cơ chế>.

## Thiết lập
data: <nguồn, n, cách chia>  |  model: <tên, version>  |  metric: <tên>
baseline: <số hiện tại, kèm CI>

## Kết quả
| Cấu hình | Metric [CI] | n | Ghi chú |

## Kết luận
Xác nhận / bác bỏ giả thuyết. Việc cần làm tiếp.

## Ranh giới trung thực
Cái gì đã đo · cái gì suy luận · cái gì chưa kiểm.
```

> Bố cục này **chính là bố cục người phỏng vấn AI Engineer muốn nghe**. Làm quen với nó từ
> project đầu tiên.

---

## 1. Bản đồ 12 project

```
  ĐỘ KHÓ
    ▲
 L4 │                                          ⑪ Distill VLM    ⑫ Attribute CV
    │                                             extraction        end-to-end
    │                          ⑧ Fine-tune
 L3 │                             embedding   ⑨ Router    ⑩ Fine-tune
    │              ⑤ Learning-      (InfoNCE)    nhỏ/lớn      reranker
    │                 to-rank
 L2 │      ③ Đường cong  ④ Tập eval
    │         resolution     retrieval    ⑥ Hybrid    ⑦ Lọc trang
    │                                        search
 L1 │ ① Đo lại   ② Separation
    │   mọi số      embedding
    └────────────────────────────────────────────────────────────────▶
      Tuần 1-2     Tuần 3-6      Tuần 7-12    Tuần 13-18   Tuần 19-24

  ══ ĐƯỜNG GĂNG ══  ① → ② → ③ là điều kiện cần của gần như mọi project sau.
                    Đừng nhảy cóc.
```

---

## 2. Bảng tổng hợp

| # | Project | Học gì | Dữ liệu | Metric | Công sức | Giá trị production |
|---|---|---|---|---|---|---|
| **①** | Đo lại mọi số kèm CI | [09](09-eval-va-do-luong.md) | golden hiện có | CI, dao động | 1 tuần | ⭐⭐⭐ nền cho mọi thứ |
| **②** | Separation của embedding | [06](06-multimodal-vlm.md) | sketch trong Qdrant | Cohen's d | 3 ngày | ⭐⭐⭐ quyết định có fine-tune |
| **③** | Đường cong accuracy↔resolution | [06](06-multimodal-vlm.md),[10](10-toi-uu-token-chi-phi.md) | 20 trang golden | acc vs token | 1 tuần | ⭐⭐⭐ cắt 50-75% token ảnh |
| **④** | Tập eval retrieval từ golden BOM | [07](07-embedding-retrieval-rag.md) | golden Excel | recall@k, MRR | 1 tuần | ⭐⭐⭐ mở khóa ⑥⑧⑩ |
| **⑤** | Learning-to-rank 4 trọng số | [02](02-machine-learning-co-ban.md) | golden Team B | recall@1/@5 | 1 tuần | ⭐⭐ |
| **⑥** | Hybrid search (dense + sparse) | [07](07-embedding-retrieval-rag.md) | master_materials | recall@10 | 1 tuần | ⭐⭐⭐ |
| **⑦** | Classifier lọc trang có BOM | [03](03-deep-learning-co-ban.md),[04](04-computer-vision.md) | trang PDF + nhãn | recall, token | 2 tuần | ⭐⭐⭐ |
| **⑧** | Fine-tune embedding sketch | [08](08-finetuning.md) | sketch + style | recall@5 | 6 tuần | ⭐⭐ (nếu ② cho d<1) |
| **⑨** | Router model nhỏ/lớn | [08](08-finetuning.md),[10](10-toi-uu-token-chi-phi.md) | log production | chi phí, acc | 3 tuần | ⭐⭐⭐ |
| **⑩** | Fine-tune reranker | [08](08-finetuning.md) | từ ④ | MRR | 2 tuần | ⭐⭐ |
| **⑪** | Distill VLM extraction | [08](08-finetuning.md) | log Langfuse | acc vs teacher | 8 tuần | ⭐⭐ |
| **⑫** | Garment attribute detection | [04](04-computer-vision.md) | sketch + nhãn tay | mAP | 8 tuần | ⭐ rủi ro cao |

---

## 3. Ba project đầu — chi tiết đầy đủ

### ① Đo lại mọi số, kèm khoảng tin cậy

**Câu hỏi:** những con số eval hiện tại có đủ chắc để ra quyết định không?

**Việc làm:**
1. Thêm `bootstrap_ci()` ([09](09-eval-va-do-luong.md)§5.1) vào mọi script eval.
2. Chạy lại toàn bộ golden set, báo cáo dạng `x [lo, hi], n=N`.
3. **Đo dao động nội tại**: chạy cùng một techpack 10 lần, đếm field khác nhau ([09](09-eval-va-do-luong.md)§7).
4. Đọc **30 ca sai bằng mắt**, lập bảng phân loại lỗi ([09](09-eval-va-do-luong.md)§4).

**Đầu ra:**
```
  BÁO CÁO ①
  ├─ hard accuracy  = 0.891 [0.846, 0.923]  n=256
  ├─ PTU accuracy   = 0.758 [0.512, 0.882]  n=19    ⚠ n quá nhỏ để so sánh
  ├─ dao động nội tại = ±___ điểm  (10 lần chạy)
  └─ phân loại lỗi: retrieval __ · VLM đọc sai __ · rule thiếu __ · golden sai __
```

**Vì sao đây là project số 1:** nó hiệu chỉnh lại toàn bộ lịch sử đo của bạn. Rất có thể một vài
"cải tiến" trong quá khứ nằm trong khoảng nhiễu — biết điều đó ngay bây giờ tốt hơn là xây tiếp
lên trên.

---

### ② Separation của embedding

**Câu hỏi:** embedding sketch hiện tại có phân tách được style không?

**Việc làm:** [06](06-multimodal-vlm.md)§2.2 — histogram + Cohen's d.

**Cổng quyết định:**
```
  d < 0.5   → embedding gần vô dụng. Fine-tune (⑧) là BẮT BUỘC.
              Mọi tinh chỉnh trọng số (⑤) đều lãng phí.
  0.5–1.0   → fine-tune (⑧) đáng làm. Làm ⑤ và ⑩ trước vì rẻ hơn.
  1.0–2.0   → ⑧ chưa cần. Ưu tiên ⑤, ⑥, ⑩.
  d > 2.0   → embedding KHÔNG phải vấn đề. Đi tìm ở mapping/scoring.
```

**Vì sao quan trọng:** project ⑧ tốn 6 tuần. Project ② tốn 3 ngày và nói cho bạn biết ⑧ có đáng
hay không. Đây là tỉ lệ đòn bẩy tốt nhất trong danh sách.

---

### ③ Đường cong accuracy ↔ resolution

**Câu hỏi:** `max_pixels` nên đặt bằng bao nhiêu?

**Việc làm:** [10](10-toi-uu-token-chi-phi.md)§4.

**Đầu ra mong đợi:**
```
  ×1.00  11,125 token  acc=0.89 [0.85, 0.92]   ← baseline
  ×0.75   6,204 token  acc=0.__ [____, ____]
  ×0.50   2,772 token  acc=0.__ [____, ____]   ← nếu CI còn chồng lấn baseline
  ×0.35   1,364 token  acc=0.__ [____, ____]      thì đây là chiến thắng lớn
  ×0.25     ___ token  acc=0.__ [____, ____]

  → max_pixels đề xuất: _____ (mức rẻ nhất còn chồng lấn CI của ×1.00)
  → tiết kiệm token ảnh: ___%
```

**Tinh chỉnh đáng làm thêm:** tách riêng ngưỡng cho **trang bảng** (chữ nhỏ, cần cao) và
**trang sketch** (chỉ cần nét, chịu được thấp). Docling đã phân loại được loại trang.

---

## 4. Sáu project giữa — tóm tắt

### ④ Tập eval retrieval từ golden BOM

Mỗi dòng BOM đúng trong file Excel golden **là một cặp (query → vật liệu đúng)**. Dựng qrels từ
đó. → [07](07-embedding-retrieval-rag.md)§7.1

**Kiểm giả định trước khi lập kế hoạch:** mở 2 file golden, đếm xem thật sự truy ngược được bao
nhiêu cặp. Nếu chỉ được 150 thay vì 1000, kế hoạch phải đổi.

### ⑤ Learning-to-rank

Thay `0.40/0.25/0.25/0.10` bằng trọng số học được. → [02](02-machine-learning-co-ban.md)§8

**Chỉ làm nếu ② cho d ≥ 1.0.** Nếu embedding không phân tách được thì tối ưu trọng số của nó
là tối ưu nhiễu.

### ⑥ Hybrid search

bge-m3 **đã sinh sẵn sparse vector** — bật nó, gộp bằng RRF, đo recall trước/sau.
→ [07](07-embedding-retrieval-rag.md)§5

Dữ liệu của bạn đầy mã (`190T`, mã NCC, mã màu) — đây là trường hợp hybrid có lợi rõ nhất.

### ⑦ Classifier lọc trang

```
  Nhãn: trang này có chứa dòng BOM không? (nhị phân)
  Nguồn nhãn: từ output VLM hiện tại — trang nào từng sinh ra dòng BOM = positive
  Model: embedding trang (hoặc feature từ Docling) → nn.Linear(d, 2)
  ⚠ Ngưỡng THẬN TRỌNG: ưu tiên recall ~100%, chấp nhận precision thấp
     (thà gửi thừa còn hơn mất dòng BOM)
  Đo: % trang cắt được · token tiết kiệm · SỐ DÒNG BOM BỊ MẤT (phải = 0)
```

### ⑧ Fine-tune embedding

Kế hoạch 6 tuần đầy đủ ở [08](08-finetuning.md)§6.4. **Cổng vào: ② cho d < 1.0.**

### ⑨ Router model nhỏ/lớn

Kiến trúc ở [08](08-finetuning.md)§8.2. Bắt đầu bằng task dễ nhất: FRONT/BACK classifier
([03](03-deep-learning-co-ban.md)§9).

### ⑩ Fine-tune reranker

Rẻ hơn ⑧, không phải reindex, ra kết quả trong ngày. → [08](08-finetuning.md)§7

**Cổng vào:** bảng chẩn đoán [07](07-embedding-retrieval-rag.md)§7.3 cho ra *"recall cao, MRR thấp"*.

---

## 5. Hai project lớn — chỉ làm khi đã xong 1-10

### ⑪ Distill VLM extraction

Teacher Qwen 27B → student 2-3B trên log production. → [08](08-finetuning.md)§8

**Điều kiện:** ≥5,000 log sạch, đã có router (⑨) để escalate ca khó.

### ⑫ Garment attribute detection

Grounding DINO zero-shot → gán nhãn → YOLO. → [04](04-computer-vision.md)§6.3

**Rủi ro cao nhất trong danh sách.** Làm bài kiểm tra 1 buổi chiều trước
([04](04-computer-vision.md)§9 bài 4): nếu Grounding DINO không bắt được gì trên sketch line-art,
**dừng luôn** và tiết kiệm 8 tuần.

---

## 6. Lịch 24 tuần

```
  Tuần  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24
       ──────────────────────────────────────────────────────────────────────
  ①    ████
  ②        ██
  ③          ████
  ④              ████
  ⑤                  ████
  ⑥                      ████
  ⑦                          ██████
  ⑩                                ████
  ⑨                                    ██████
  ⑧                                          ████████████
  ⑪/⑫                                                     ████████████
       ──────────────────────────────────────────────────────────────────────
         ▲ tuần 6: biết nên           ▲ tuần 14: đã có 2     ▲ tuần 24: đã
           fine-tune hay không          cải tiến production     train & deploy
                                        có số                   model của mình
```

---

## 7. Bảng tổng kết để điền — "portfolio" của bạn sau 24 tuần

| # | Project | Trạng thái | Kết quả (số + CI) | Đã lên production? |
|---|---|---|---|---|
| ① | Đo lại + CI | ☐ | | |
| ② | Separation | ☐ | `d = ___` | n/a |
| ③ | Resolution | ☐ | `−___% token, acc giữ nguyên` | ☐ |
| ④ | Tập eval retrieval | ☐ | `n = ___ cặp` | n/a |
| ⑤ | Learning-to-rank | ☐ | `recall@1: ___ → ___` | ☐ |
| ⑥ | Hybrid search | ☐ | `recall@10: ___ → ___` | ☐ |
| ⑦ | Lọc trang | ☐ | `−___% lời gọi, 0 dòng mất` | ☐ |
| ⑧ | Fine-tune embedding | ☐ | `recall@5: ___ → ___` | ☐ |
| ⑨ | Router | ☐ | `−___% chi phí, acc giữ` | ☐ |
| ⑩ | Fine-tune reranker | ☐ | `MRR: ___ → ___` | ☐ |

> **Bảng này điền xong chính là phần "Dự án" trong CV AI Engineer của bạn.** Mỗi dòng là một câu
> chuyện có số, có phương pháp, có kiểm chứng thống kê. Đó là thứ phân biệt ứng viên thật với
> ứng viên kể chuyện. → [13](13-lo-trinh-ai-engineer-vsf-fpt.md)

---

## 8. Ranh giới trung thực

- **Là ước lượng của tôi:** toàn bộ cột "công sức" và "giá trị production". Tôi ước lượng từ độ
  phức tạp kỹ thuật, **không biết** tốc độ làm việc của bạn hay ràng buộc lịch dự án.
- **Là giả định quan trọng nhất, cần kiểm trước:** project ④ dựa trên giả định golden BOM Excel
  truy ngược được thành cặp (query, doc đúng). **Nếu giả định này sai thì ④, ⑥, ⑩ đều phải thiết
  kế lại.** Dành 30 phút mở 2 file golden và kiểm trước khi lập kế hoạch.
- **Không hứa hẹn kết quả:** tôi không viết con số kỳ vọng cho ⑤, ⑧, ⑩. Tôi không biết, và các
  cổng quyết định (②, và bảng chẩn đoán ở [07](07-embedding-retrieval-rag.md)§7.3) tồn tại chính
  vì lý do đó.
- **Rủi ro cao nhất:** ⑫. Có khả năng thật là Grounding DINO không hoạt động trên line-art.
  Bài kiểm tra một buổi chiều là bắt buộc trước khi cam kết.
