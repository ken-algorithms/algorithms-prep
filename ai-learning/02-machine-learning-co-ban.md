# 02 · Machine Learning cơ bản

> **Câu hỏi file này trả lời:** bốn trọng số `0.40/0.25/0.25/0.10` trong `similar_techpack_scorer.py`
> đáng lẽ phải được **học** như thế nào, và làm sao biết mình đã học đúng chứ không phải học vẹt
> 20 case golden.
>
> **Thời lượng:** 12–15 giờ. **Yêu cầu trước:** [01](01-nen-tang-toan.md).

---

## 0. Bản đồ

```
 DỮ LIỆU ──▶ TÁCH TẬP ──▶ MÔ HÌNH ──▶ LOSS ──▶ TỐI ƯU ──▶ ĐO ──▶ QUYẾT ĐỊNH
    │           │            │          │         │         │         │
   §1          §2           §3         §4        §5        §6        §7
 loại bài   train/val/    tuyến tính  chọn hàm  gradient  metric   ngưỡng,
 toán       test, rò rỉ   → cây → GBDT phạt gì  descent   nào đúng calibration
                                                                      │
                                                                      ▼
                                                            §8 learning-to-rank
                                                         (chính là bài toán của bạn)
```

---

## 1. Ba loại bài toán — và bài toán của bạn thuộc loại nào

| Loại | Đầu ra | Ví dụ chung | Trong motivesidp |
|---|---|---|---|
| **Regression** | số thực | dự đoán giá | dự đoán consumption/định mức vải |
| **Classification** | nhãn rời rạc | spam/không spam | phân loại sketch FRONT vs BACK (hiện VLM làm) |
| **Ranking** | thứ tự | kết quả tìm kiếm | **Team B: xếp hạng style tương tự** ⭐ |
| *(Clustering)* | nhóm, không nhãn | phân khúc khách | gom nhóm sketch để phát hiện trùng lặp |

> **Điểm mấu chốt bị bỏ sót trong repo hiện tại:** Team B là bài toán **ranking**, không phải
> classification. Metric đúng là `recall@k` / `MRR` / `nDCG`, **không phải accuracy**. Nếu bạn
> đang báo cáo "accuracy 75.8%" cho một bài ranking thì con số đó đang trả lời sai câu hỏi.
> → §6.4

---

## 2. Tách tập dữ liệu — và bốn kiểu rò rỉ giết chết mọi kết quả

### 2.1 Ba tập, ba vai trò

```
   Toàn bộ data (100 style)
   ┌──────────────────────────┬──────────────┬──────────────┐
   │        TRAIN  60%        │   VAL  20%   │  TEST  20%   │
   └──────────────────────────┴──────────────┴──────────────┘
         │                          │                │
    fit tham số              chọn hyperparameter   CHẠM ĐÚNG 1 LẦN
    (trọng số w)             (ngưỡng, số cây,       khi đã chốt mọi thứ
                              learning rate)
```

**Luật sắt:** mỗi lần bạn nhìn vào TEST rồi sửa gì đó, TEST trở thành VAL. Sau 5 lần nhìn, con số
TEST của bạn đã lạc quan một cách có hệ thống.

### 2.2 Bốn kiểu rò rỉ — với ví dụ trực tiếp từ dữ liệu của bạn

| Kiểu rò rỉ | Là gì | Rủi ro cụ thể trong motivesidp |
|---|---|---|
| **Rò rỉ nhóm** | Cùng một thực thể xuất hiện ở cả train và test | `soft_light_CAMP1_144`, `_497`, `_578` là **3 biến thể của cùng 1 style CAMP1**. Chia ngẫu nhiên → 144 vào train, 497 vào test → điểm cao giả |
| **Rò rỉ thời gian** | Train bằng dữ liệu tương lai, test quá khứ | Techpack mùa 2026 dự đoán mùa 2024 — vô nghĩa với production |
| **Rò rỉ tiền xử lý** | Tính mean/std hoặc fit PCA trên **toàn bộ** rồi mới chia | Normalize embedding bằng thống kê của cả tập → test đã "biết" train |
| **Rò rỉ nhãn** | Feature chứa thông tin của nhãn | Dùng `base_bom_id` làm feature để dự đoán... chính base BOM |

> **Đây là lỗi tôi cá là sẽ xảy ra đầu tiên khi bạn train model đầu tiên.** Dataset của bạn
> (`data/team_a_bom/golden/`) đặt tên theo pattern `{rulebook}_{style}_{variant}` — **phải chia
> theo `style`, không phải theo file.** Dùng `GroupKFold`, không dùng `KFold`.

```python
from sklearn.model_selection import GroupKFold
import re

files  = ["soft_light_CAMP1_144", "soft_light_CAMP1_497", "soft_light_CAMP1_578",
          "soft_light_CNRA1", "soft_light_TOEN1_656"]
groups = [re.sub(r"_\d+$", "", f) for f in files]      # style, bỏ variant
# → ['soft_light_CAMP1', 'soft_light_CAMP1', 'soft_light_CAMP1', 'soft_light_CNRA1', ...]
# GroupKFold đảm bảo 3 biến thể CAMP1 luôn nằm cùng một phía
```

### 2.3 Với n nhỏ (20–30 case) thì sao?

Bạn không đủ dữ liệu để chia 60/20/20. Dùng **nested cross-validation**:

```
   Vòng ngoài (đánh giá):    [test][─────── train+val ───────]
                                        │
   Vòng trong (chọn tham số):  [val][──── train ────]  × k lần
```

Chi phí: chạy `k_ngoài × k_trong` lần. Với 20 case, `5 × 4 = 20` lần fit — vẫn rất rẻ cho một
mô hình 4 tham số. **Không có cớ để bỏ qua bước này.**

---

## 3. Mô hình — đi từ đơn giản nhất

### 3.1 Thứ tự bắt buộc phải thử

```
  ┌──────────────┐   không đủ tốt   ┌──────────────┐   không đủ tốt   ┌──────────────┐
  │  BASELINE    │─────────────────▶│  TUYẾN TÍNH  │─────────────────▶│  GBDT        │
  │ hằng số /    │                  │ linear/      │                  │ XGBoost      │
  │ luật đơn     │                  │ logistic reg │                  │ LightGBM     │
  └──────────────┘                  └──────────────┘                  └──────────────┘
       ↑                                                                      │
       │                                                                      │ vẫn không đủ
       │  ⚠ NẾU BASELINE ĐÃ ĐỦ TỐT → DỪNG.                                   ▼
       │    Rất nhiều dự án AI thất bại vì bỏ qua bước này.            ┌──────────────┐
       │                                                              │  DEEP LEARNING│
       └──────────────────────────────────────────────────────────────│  chỉ khi có   │
                                                                      │  data lớn/    │
                                                                      │  unstructured │
                                                                      └──────────────┘
```

**Với dữ liệu dạng bảng (tabular), GBDT vẫn thắng deep learning trong đa số trường hợp thực tế.**
Đây không phải ý kiến cũ — nó vẫn đúng năm 2026 cho tabular cỡ vừa. Đừng nhảy thẳng vào neural
network vì nó "hiện đại hơn".

### 3.2 Logistic regression — mô hình bạn cần cho bài scoring

```
   p = σ(w₁·x₁ + w₂·x₂ + w₃·x₃ + w₄·x₄ + b),     σ(z) = 1/(1+e^(−z))
       └──────────────┬──────────────┘
        chính là "composite score" của bạn,
        nhưng w được HỌC thay vì gõ tay
```

Ưu điểm cho đúng bài toán của bạn:
- **4 tham số** — fit được với 20–30 case (quy tắc ngón tay: ≥10 mẫu/tham số).
- **Giải thích được** — `w` chính là "tầm quan trọng của từng tín hiệu", business đọc hiểu.
- **Ra xác suất, không phải điểm tùy ý** — và xác suất thì **ngưỡng được** một cách có nghĩa (§7).
- **Thay thế trực tiếp** `SimilarTechpackScoringConfig` mà không đổi kiến trúc.

### 3.3 Cây quyết định & GBDT — 60 giây

| | Cây đơn | Random Forest | GBDT (XGBoost/LightGBM) |
|---|---|---|---|
| Ý tưởng | Chia không gian bằng các câu hỏi if/else | Nhiều cây độc lập, lấy trung bình | Cây sau **sửa lỗi** của cây trước |
| Overfit | Rất dễ | Ít | Trung bình (cần early stopping) |
| Khi nào dùng | Giải thích cho người ngoài | Baseline nhanh, ít tune | Khi cần điểm cao nhất trên tabular |

> **Liên hệ:** `rule_engine.py` của bạn thật ra là một **cây quyết định viết tay**. Nếu có nhãn,
> bạn có thể train một cây thật rồi **so sánh** với rule tay — chỗ nào cây khác rule tay chính là
> chỗ business rule có thể sai hoặc thiếu. Đây là cách dùng ML rất mạnh mà ít người nghĩ tới:
> **dùng model để audit rule, không phải để thay rule.**

---

## 4. Loss — bạn đang phạt cái gì

| Bài toán | Loss chuẩn | Đặc tính | Khi nào đổi |
|---|---|---|---|
| Regression | MSE `(ŷ−y)²` | Phạt nặng outlier | Có outlier → dùng **Huber** |
| Regression | MAE `\|ŷ−y\|` | Bền với outlier | Cần gradient mượt → Huber |
| Binary classification | BCE `−[y log p + (1−y) log(1−p)]` | Chuẩn | Lệch lớp nặng → **Focal loss** |
| Multi-class | Cross-entropy | Chuẩn | — |
| Ranking (cặp) | **Pairwise logistic** | Học "A xếp trên B" | ⭐ bài toán Team B |
| Metric learning | **InfoNCE / Triplet** | Kéo gần positive, đẩy xa negative | ⭐ fine-tune FashionCLIP → [08](08-finetuning.md) |

### 4.1 Chọn loss = chọn định nghĩa "sai"

Điểm hay bị hiểu nhầm: loss **không phải** metric. Loss là thứ **tối ưu được bằng gradient**;
metric là thứ **business quan tâm**. Chúng thường khác nhau.

```
   Bạn muốn:   recall@5 cao          ← metric, không khả vi, không tối ưu trực tiếp được
   Bạn tối ưu: pairwise logistic loss ← loss, khả vi, là proxy của metric trên
                     │
                     └── nếu proxy lệch quá thì loss giảm mà metric không tăng.
                         ĐÂY LÀ LỖI THƯỜNG GẶP NHẤT khi train lần đầu.
```

**Kỷ luật bắt buộc:** mỗi epoch log **cả hai** — train loss và metric trên val. Loss giảm mà
metric đứng yên → proxy sai, đổi loss chứ đừng train lâu hơn.

---

## 5. Gradient descent — ba biến thể phải phân biệt

| Biến thể | Cập nhật sau | Ưu | Nhược |
|---|---|---|---|
| Batch GD | toàn bộ dataset | ổn định | chậm, không scale |
| **SGD** | 1 mẫu | thoát được local min nhờ nhiễu | dao động mạnh |
| **Mini-batch** ⭐ | 32–256 mẫu | cân bằng, tận dụng GPU | cần chọn batch size |

**Batch size ảnh hưởng gì:**

```
  batch nhỏ (8-32)                       batch lớn (256-1024)
  ├─ nhiễu nhiều → regularize tự nhiên   ├─ gradient chính xác hơn
  ├─ generalize thường tốt hơn           ├─ tận dụng GPU tốt hơn
  └─ chậm trên GPU                       └─ dễ kẹt ở minimum "nhọn", cần lr lớn hơn

  Quy tắc: tăng batch ×k thì tăng lr ×√k (hoặc ×k với Adam, thử cả hai)
```

**Optimizer nên dùng:** `AdamW` với `lr=3e-4` cho model train từ đầu, `lr=1e-5..5e-5` cho
fine-tune. Đây là mặc định tốt trong 90% trường hợp. Đừng tune optimizer trước khi tune dữ liệu.

---

## 6. Metric — chỗ hầu hết người mới làm sai

### 6.1 Confusion matrix, bốn ô

```
                        DỰ ĐOÁN
                  Positive    Negative
              ┌────────────┬────────────┐
    T  Pos    │    TP      │    FN      │   ← bỏ sót
    H         │  (đúng)    │ (bỏ lọt)   │
    Ự  ───────┼────────────┼────────────┤
    C  Neg    │    FP      │    TN      │
    Ế         │ (báo động  │  (đúng)    │
              │   nhầm)    │            │
              └────────────┴────────────┘

    Precision = TP/(TP+FP)   "trong số tôi báo là đúng, bao nhiêu % đúng thật"
    Recall    = TP/(TP+FN)   "trong số đúng thật, tôi bắt được bao nhiêu %"
    F1        = 2PR/(P+R)    trung bình điều hòa
```

### 6.2 Precision hay Recall — quyết định bởi chi phí của lỗi

Trong motivesidp, chi phí **không đối xứng**:

| Loại lỗi | Hệ quả thật | Chi phí |
|---|---|---|
| **FP** — hệ thống điền sai vật liệu mà tự tin | Purchasing đặt mua nguyên liệu sai → **tiền thật** | RẤT CAO |
| **FN** — hệ thống để trống, đẩy cho người review | Tốn thời gian người | Thấp |

> → Hệ thống của bạn **phải ưu tiên precision**. Và thực tế kiến trúc bạn đã làm đúng điều đó
> (để trống khi không chắc thay vì đoán). Nhưng bạn **chưa đo nó bằng precision/recall** —
> bạn đang đo bằng accuracy, thứ trộn lẫn hai loại lỗi có chi phí khác nhau. → §6.5

### 6.3 Accuracy — cái bẫy lệch lớp

```
  Dataset: 95 mẫu OK, 5 mẫu lỗi
  Model "luôn trả lời OK":  accuracy = 95%   ← nghe rất giỏi
                            recall lỗi = 0%  ← hoàn toàn vô dụng
```

**Luật:** nếu lớp lệch hơn 70/30, accuracy là số vô nghĩa. Dùng PR-AUC hoặc F1 theo lớp thiểu số.

### 6.4 Metric cho bài ranking — cái Team B cần

| Metric | Định nghĩa | Trả lời câu hỏi |
|---|---|---|
| **Recall@k** | % query có đáp án đúng trong top-k | "Top-5 có chứa style đúng không?" ⭐ |
| **MRR** | trung bình của `1/vị_trí_đúng_đầu_tiên` | "Đáp án đúng nằm ở hạng mấy?" |
| **nDCG@k** | recall có trọng số theo vị trí + mức liên quan | Khi có **nhiều mức** liên quan, không chỉ đúng/sai |
| **Precision@1** | % query có hạng 1 đúng | "Nếu tôi chỉ lấy top-1 thì đúng bao nhiêu %?" ⭐ |

```python
def recall_at_k(rankings, k):
    """rankings[i] = vị trí (1-based) của đáp án đúng cho query i, hoặc None nếu không có trong list."""
    hit = sum(1 for r in rankings if r is not None and r <= k)
    return hit / len(rankings)

def mrr(rankings):
    return sum(1/r if r else 0 for r in rankings) / len(rankings)

ranks = [1, 3, 2, None, 1, 7, 2]
for k in (1, 3, 5, 10):
    print(f"recall@{k:2d} = {recall_at_k(ranks, k):.3f}")
print(f"MRR      = {mrr(ranks):.3f}")
```

Chạy thật:

```
recall@ 1 = 0.286
recall@ 3 = 0.714
recall@ 5 = 0.714
recall@10 = 0.857
MRR      = 0.497
```

> **Đọc bảng này:** recall@1 chỉ 0.286 nhưng recall@10 tới 0.857 — nghĩa là **retrieval tìm được
> đáp án, nhưng xếp hạng sai**. Đây là chẩn đoán rất khác với "retrieval không tìm thấy". Cách
> sửa cũng khác hẳn: cần **rerank tốt hơn**, không cần embedding tốt hơn.
> Bạn không thể có chẩn đoán này nếu chỉ đo một con số accuracy.

### 6.5 Bảng đề xuất cụ thể cho motivesidp

| Thành phần | Metric hiện tại | Metric **nên dùng** | Vì sao |
|---|---|---|---|
| Team B similar techpack | "accuracy" | **recall@1, recall@5, MRR** + breakdown theo product group | Đây là ranking, không phải classification |
| Team A BOM cell | accuracy 89.1% | **precision/recall theo từng field**, + % ô để trống | Chi phí FP ≫ FN, và "để trống" là hành vi hợp lệ, không phải sai |
| PTU | accuracy 75.8% | per-style accuracy + **CI** | n=19 quá nhỏ, cần CI |
| Extraction VLM | — | **field-level exact match** + edit distance cho field text | Sai 1 ký tự khác sai hoàn toàn |

---

## 7. Overfit, regularization, và calibration

### 7.1 Nhận diện overfit bằng learning curve

```
  loss                                    loss
   │╲                                      │╲
   │ ╲___ val                              │ ╲___ val  ─── ─── ─── (đi ngang cao)
   │     ╲___                              │     ╲__╱‾‾‾‾‾‾  ← val TĂNG trở lại
   │         ╲______ train                 │        ╲_______ train → 0
   └──────────────────── epoch             └──────────────────── epoch
      LÀNH MẠNH                               OVERFIT
      cả 2 cùng giảm, gap nhỏ                 train→0, val bật lên
                                              → dừng ở điểm val thấp nhất (early stopping)
```

Còn một trường hợp thứ ba ít người nói: **cả hai cùng cao và phẳng** → *underfit*, model quá yếu
hoặc learning rate quá nhỏ hoặc dữ liệu không chứa tín hiệu. Với 20 case golden, khả năng cao bạn
gặp trường hợp này trước khi gặp overfit.

### 7.2 Ba công cụ chống overfit, theo thứ tự nên thử

| Thứ tự | Công cụ | Chi phí | Hiệu quả |
|---|---|---|---|
| 1 | **Thêm dữ liệu** | cao (gán nhãn) | Cao nhất, luôn luôn |
| 2 | **Giảm số tham số** (4 trọng số thay vì 40 feature) | miễn phí | Cao khi n nhỏ |
| 3 | L2 regularization (`weight_decay`) | miễn phí | Trung bình |
| 4 | Early stopping | miễn phí | Trung bình |
| 5 | Dropout / augmentation | rẻ | Với deep learning |

> Với n=20, **công cụ số 2 quan trọng nhất**. Đừng đưa 30 feature vào model khi chỉ có 20 mẫu —
> nó sẽ nhớ thuộc lòng cả 20 mẫu và vô dụng trên mẫu thứ 21.

### 7.3 Calibration — vì sao `confidence` của bạn có thể đang nói dối

Model xuất ra `p = 0.9`. Trong 100 lần model nói 0.9, có đúng 90 lần đúng không? Nếu có →
**well-calibrated**. Nếu chỉ đúng 60 lần → model **quá tự tin**.

```
   Reliability diagram
   1.0 │                              ╱  ← đường lý tưởng (calibrated)
       │                         ╱  ●
   tỉ  │                    ╱  ●
   lệ  │               ╱ ●
   đúng│          ╱ ●
   thật│     ╱  ●              ● = model của bạn, nằm DƯỚI đường
       │╱  ●                       → quá tự tin, nói 0.8 nhưng chỉ đúng 0.55
   0.0 └──────────────────────────
       0.0    confidence dự đoán   1.0
```

**Vì sao điều này quan trọng sống còn với bạn:** toàn bộ kiến trúc "tự tin thì điền, không tự tin
thì để trống cho người review" **dựa vào giả định confidence đáng tin**. Nếu confidence chưa được
calibrate, ngưỡng của bạn đang đặt ở chỗ vô nghĩa.

```python
from sklearn.calibration import calibration_curve
prob_true, prob_pred = calibration_curve(y_true, y_prob, n_bins=10, strategy="quantile")
for pp, pt in zip(prob_pred, prob_true):
    print(f"model nói {pp:.2f} → thực tế đúng {pt:.2f}  {'⚠ quá tự tin' if pt < pp - 0.1 else ''}")
```

**Cách sửa:** Platt scaling (fit 1 logistic regression trên output) hoặc isotonic regression.
Rẻ, nhanh, cần khoảng 100+ mẫu val. → [09](09-eval-va-do-luong.md) §6

---

## 8. Learning-to-rank — thay 4 con số gõ tay bằng 4 con số học được

Đây là **project ML đầu tiên tôi khuyên bạn làm**, vì nó dùng đúng data bạn đang có.

### 8.1 Bài toán

```
  Hiện tại:
    score = 0.40·emb + 0.25·silhouette + 0.25·construction + 0.10·detail
            └─── 4 con số do người gõ tay ───┘

  Mục tiêu:
    score = w₁·emb + w₂·silhouette + w₃·construction + w₄·detail + b
            └─── học từ golden set, có cross-validation ───┘
```

### 8.2 Chuẩn bị dữ liệu — dạng cặp (pairwise)

Với mỗi query techpack `q`, bạn có: 1 candidate **đúng** (`c⁺`) và N candidate **sai** (`c⁻`).
Tạo các cặp:

```
  (q, c⁺, c⁻)  →  nhãn: "c⁺ phải xếp trên c⁻"
```

Từ 20 query × 10 candidate → khoảng **180 cặp huấn luyện** từ chỉ 20 case gán nhãn. Đây là lý do
pairwise ranking hiệu quả với dữ liệu ít.

### 8.3 Code chạy được

```python
import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import GroupKFold

# X_pos, X_neg: (n_pairs, 4) — 4 điểm thành phần cho candidate đúng / sai
# groups: (n_pairs,) — id của query, để không rò rỉ giữa các fold

def build_pairwise(X_pos, X_neg):
    """Trick kinh điển: học w sao cho w·(x⁺ − x⁻) > 0."""
    d = X_pos - X_neg
    X = np.vstack([d, -d])                       # đối xứng hóa để không lệch
    y = np.hstack([np.ones(len(d)), np.zeros(len(d))])
    return X, y

X, y = build_pairwise(X_pos, X_neg)
g    = np.hstack([groups, groups])

accs = []
for tr, te in GroupKFold(n_splits=5).split(X, y, groups=g):
    clf = LogisticRegression(C=1.0, fit_intercept=False).fit(X[tr], y[tr])
    accs.append(clf.score(X[te], y[te]))
print(f"pairwise accuracy CV = {np.mean(accs):.3f} ± {np.std(accs):.3f}")

clf = LogisticRegression(C=1.0, fit_intercept=False).fit(X, y)
w = clf.coef_[0]
w = np.clip(w, 0, None)                          # ép không âm — giữ tính diễn giải
w = w / w.sum()                                  # chuẩn hóa về tổng = 1, khớp contract hiện tại
print("trọng số học được:", dict(zip(
    ["embedding", "silhouette", "construction", "detail"], np.round(w, 3))))
```

### 8.4 Quy trình triển khai an toàn — bốn bước, không được nhảy cóc

```
  ①  Đóng băng baseline           ②  Học trọng số          ③  Shadow mode
  ┌────────────────────┐          ┌──────────────────┐     ┌──────────────────┐
  │ Chạy trọng số hiện │          │ GroupKFold trên  │     │ Chạy SONG SONG   │
  │ tại trên golden,   │─────────▶│ golden, lấy w    │────▶│ cả 2, chỉ LOG,   │
  │ ghi recall@1/@5    │          │ Báo cáo CV ± std │     │ không đổi output │
  │ + CI bootstrap     │          │                  │     │ 2-4 tuần         │
  └────────────────────┘          └──────────────────┘     └──────────────────┘
                                                                    │
                                           ④  Đổi — chỉ khi mức tăng ▼
                                              VƯỢT khoảng tin cậy của baseline
                                           ┌──────────────────────────────┐
                                           │ Nếu CI chồng lấn → KHÔNG đổi │
                                           │ Giữ config cũ làm rollback   │
                                           └──────────────────────────────┘
```

> **Đánh đổi phải nói rõ:** trọng số học được **khớp với 20 case golden**. Nếu 20 case đó không
> đại diện cho phân bố thật (ví dụ toàn jacket, production có cả shirt), trọng số học được có thể
> **tệ hơn** trọng số gõ tay trên dữ liệu thật. Đó chính là lý do bước ③ shadow mode tồn tại và
> không được bỏ.

---

## 9. Bài tập — làm hết là xong file này

| # | Bài | Tiêu chí đạt | Ước lượng |
|---|---|---|---|
| 1 | Cài logistic regression **từ đầu** bằng numpy (forward + gradient + update), so với sklearn | trọng số khớp tới 2 chữ số | 2h |
| 2 | Viết `GroupKFold` split cho `data/team_a_bom/golden/`, in ra để tự kiểm không rò rỉ variant | không style nào ở cả 2 phía | 1h |
| 3 | Tính recall@1/@5/MRR cho Team B trên golden set hiện tại | 3 con số + CI | 3h |
| 4 | Vẽ learning curve cho bài 1 trên data giả lập, chỉ ra điểm overfit | 1 hình, có mũi tên điểm early stop | 1h |
| 5 | **Project chính:** §8 đầy đủ — học 4 trọng số, báo cáo CV, so với baseline | bảng so sánh có CI | 8h |
| 6 | Vẽ reliability diagram cho confidence hiện tại của extraction | 1 hình + kết luận có/không calibrated | 3h |

---

## 10. Ranh giới trung thực

- **Đã chạy thật:** đoạn `recall_at_k`/`mrr` ở §6.4 — output in trong file là thật.
- **Chưa chạy:** §8.3 — code đúng về mặt thuật toán nhưng tôi không có `X_pos`/`X_neg` của bạn.
  **Phải tự chạy rồi mới tin.** Đặc biệt: tôi giả định 4 điểm thành phần đã cùng thang đo [0,1];
  nếu không thì phải standardize trước, nếu không trọng số sẽ vô nghĩa.
- **Là suy luận:** con số "180 cặp từ 20 query" ở §8.2 giả định mỗi query có ~10 candidate. Số
  thật phụ thuộc `top_k` bạn cấu hình.
- **Đơn giản hóa:** §8 dùng pairwise logistic (RankNet đơn giản hóa). Có họ listwise
  (LambdaMART, ListNet) tối ưu trực tiếp nDCG và thường tốt hơn — nhưng cần nhiều dữ liệu hơn.
  Với n=20, pairwise là lựa chọn đúng.
- **Chưa kiểm chứng:** nhận định "GBDT vẫn thắng DL trên tabular" (§3.1) là đồng thuận chung trong
  ngành và khớp với các benchmark tôi biết tính tới cutoff, nhưng tôi không benchmark lại.
