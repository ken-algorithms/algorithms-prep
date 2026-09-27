# 09 · Đánh giá & đo lường

> **File có ROI cao nhất trong bộ này.** Không phải vì nội dung khó, mà vì nó là điều kiện cần của
> mọi thứ khác: **không đo được thì không cải tiến được, chỉ có thể tin tưởng.**
>
> **Thời lượng:** 10–12 giờ. **Yêu cầu trước:** [01](01-nen-tang-toan.md)§5, [02](02-machine-learning-co-ban.md)§6.

---

## 0. Chẩn đoán hệ đo hiện tại của bạn

| Bạn đã có | Bạn đang thiếu |
|---|---|
| ✅ Golden set 20 case BOM có file Excel đúng | ❌ Khoảng tin cậy cho mọi con số |
| ✅ Quy trình eval có kỷ luật (18 run `v2_llm_eval/` có timestamp) | ❌ Breakdown theo field — biết sai ở đâu |
| ✅ Số cụ thể: 89.1% hard / 75.8% PTU | ❌ Phân loại lỗi (error taxonomy) |
| ✅ So sánh nhiều model (`llm_comparison/`) | ❌ Regression suite chạy tự động trên CI |
| ✅ Langfuse trace | ❌ Shadow mode chuẩn hóa |
| | ❌ Đo tính không tái lập (chạy 2 lần ra 2 kết quả) |

**Sáu dấu ❌ đó là nội dung của file này.**

---

## 1. Ba tầng đo — đừng trộn lẫn

```
   ┌────────────────────────────────────────────────────────────┐
   │ TẦNG 3 · BUSINESS           % BOM được duyệt không sửa     │
   │   chậm (tuần), đắt,          thời gian người review/techpack│
   │   nhưng là THẬT              chi phí/techpack               │
   └────────────────────────────────────────────────────────────┘
                      ▲ tương quan? PHẢI KIỂM
   ┌────────────────────────────────────────────────────────────┐
   │ TẦNG 2 · END-TO-END         accuracy per-field trên golden │
   │   giờ, chạy được hằng ngày   recall@k của retrieval        │
   └────────────────────────────────────────────────────────────┘
                      ▲
   ┌────────────────────────────────────────────────────────────┐
   │ TẦNG 1 · COMPONENT          separation embedding           │
   │   giây, chạy mỗi commit      tỉ lệ JSON parse lỗi          │
   │                              tỉ lệ retry                    │
   └────────────────────────────────────────────────────────────┘

   ⚠ BẪY LỚN NHẤT: tối ưu tầng 1 mà tầng 3 không nhúc nhích.
     Ít nhất một lần, hãy KIỂM tương quan giữa tầng 2 và tầng 3 —
     nếu accuracy tăng 5 điểm mà thời gian review không giảm,
     bạn đang đo sai thứ.
```

---

## 2. Xây golden set — công việc quan trọng nhất, ít hào nhoáng nhất

### 2.1 Bốn thuộc tính của một golden set tốt

| Thuộc tính | Nghĩa là | Kiểm thế nào |
|---|---|---|
| **Đại diện** | Phân bố giống production | So histogram product group golden vs production |
| **Đủ lớn** | CI đủ hẹp để ra quyết định | Xem §5, thường cần n ≥ 100 |
| **Sạch** | Nhãn đúng, đã được người thứ hai kiểm | Đo inter-annotator agreement (§2.3) |
| **Bao phủ ca khó** | Có cả ca biên, không chỉ ca dễ | Phân tầng theo độ khó (§2.2) |

### 2.2 Phân tầng — quan trọng hơn tổng số

```
  ❌ 100 case ngẫu nhiên          ✅ 100 case PHÂN TẦNG
     → 85 ca dễ, 15 ca khó           40 dễ  (layout chuẩn, đủ thông tin)
     → accuracy bị chi phối           40 vừa (thiếu vài field)
       bởi ca dễ, che mất             20 khó (layout lạ, scan mờ, viết tay)
       thất bại ở ca khó
                                      → Báo cáo accuracy THEO TỪNG TẦNG:
                                        "dễ 96% · vừa 84% · khó 51%"
                                        Con số này CHỈ ĐƯỜNG, con số gộp thì không.
```

> Một con số gộp `89.1%` không bảo bạn nên làm gì tiếp. `dễ 96% / vừa 84% / khó 51%` thì có:
> hãy đi xem 49% ca khó thất bại vì cái gì.

### 2.3 Inter-annotator agreement — bước ai cũng bỏ

Trước khi tin golden set, hãy kiểm: **hai người gán nhãn có ra cùng kết quả không?**

```python
from sklearn.metrics import cohen_kappa_score
kappa = cohen_kappa_score(annotator_A, annotator_B)
```

| Cohen's κ | Diễn giải | Hệ quả |
|---|---|---|
| < 0.4 | Kém | **Bài toán chưa được định nghĩa rõ.** Model không thể vượt qua sự mơ hồ của chính định nghĩa |
| 0.4 – 0.6 | Trung bình | Cần làm rõ hướng dẫn gán nhãn |
| 0.6 – 0.8 | Tốt | Dùng được |
| > 0.8 | Rất tốt | |

> **Điều này thiết lập TRẦN cho model của bạn.** Nếu hai chuyên gia chỉ đồng ý 70% về "style nào
> tương tự nhất", thì model đạt 70% đã là chạm trần — mọi nỗ lực vượt qua đó là đuổi theo nhiễu.
> Với bài similarity của bạn (vốn chủ quan), **tôi ngờ rằng đây chính là điều đang xảy ra** và
> bạn chưa bao giờ kiểm. Một buổi sáng với 2 người và 30 cặp là đủ để biết.

---

## 3. Đo từng thành phần

### 3.1 Embedding

```python
# Chỉ số chính: separation — xem 06 §2.2
d = (same.mean() - diff.mean()) / np.sqrt((same.var() + diff.var()) / 2)
```

### 3.2 Retrieval

```python
# recall@k, MRR, nDCG — xem 02 §6.4 và 07 §7
```

### 3.3 Extraction (VLM)

Không dùng một con số. Dùng **bảng theo field**:

| Field | Exact match | Sai định dạng | Để trống | Bịa (không có trong nguồn) |
|---|---|---|---|---|
| `material_code` | 94% | 1% | 4% | **1%** ← nguy hiểm nhất |
| `consumption` | 78% | 8% | 12% | 2% |
| `place_to_use` | 61% | 3% | 30% | 6% |

> **Cột cuối là cột quan trọng nhất** và gần như không ai đo. "Để trống" là hành vi an toàn;
> "bịa" là hành vi gây thiệt hại tiền thật. Chúng **không được** gộp chung vào một con số
> "không chính xác".

### 3.4 So sánh chuỗi — chọn đúng thước

| Thước | Khi nào | Lưu ý |
|---|---|---|
| Exact match | Mã, ID, enum | Chuẩn hóa trước (case, khoảng trắng, unicode NFC) |
| Normalized edit distance | Mô tả tự do | `1 - lev(a,b)/max(len)` |
| Numeric tolerance | Số lượng, định mức | `abs(a-b)/b < 0.01` — sai 0.001 không phải lỗi |
| Set F1 | Danh sách (nhiều dòng BOM) | Xử đúng trường hợp thiếu/thừa dòng |

---

## 4. Phân loại lỗi — biến 11% sai thành việc cần làm

```
  ❌ "accuracy 89%"  →  không biết làm gì tiếp

  ✅ Mở 30 ca sai, đọc từng ca, phân loại:

     ┌─────────────────────────────────────────────────────────┐
     │ Nguyên nhân                        │ Số ca │ Sửa bằng   │
     ├────────────────────────────────────┼───────┼────────────┤
     │ Retrieval không đưa đúng vào top-k │  12   │ [07] §7    │
     │ VLM đọc sai số trong bảng          │   8   │ resolution │
     │ Rule mapping thiếu case            │   5   │ sửa rule   │
     │ Nhãn golden SAI                    │   3   │ sửa golden │← luôn có!
     │ Ambiguous, 2 người cũng cãi nhau   │   2   │ chấp nhận  │
     └────────────────────────────────────┴───────┴────────────┘

     → Bây giờ bạn biết: ưu tiên #1 là retrieval, không phải prompt.
```

> **Ba giờ đọc 30 ca sai bằng mắt có giá trị hơn ba tuần tinh chỉnh prompt.** Đây là lời khuyên
> tôi tự tin nhất trong cả bộ tài liệu. Và luôn luôn, khoảng 10% "lỗi" hóa ra là **golden sai** —
> việc này tự nó đã cải thiện chất lượng phép đo.

---

## 5. Khoảng tin cậy — bắt buộc, không phải tùy chọn

### 5.1 Bootstrap — dùng được cho MỌI metric

Wilson (xem [01](01-nen-tang-toan.md)§5.1) chỉ dùng được cho tỉ lệ. Bootstrap dùng được cho
recall@k, MRR, nDCG, F1, bất cứ thứ gì.

```python
import numpy as np

def bootstrap_ci(values, metric_fn, n_boot=10000, alpha=0.05, seed=0):
    rng = np.random.default_rng(seed)
    values = np.asarray(values)
    n = len(values)
    stats = [metric_fn(values[rng.integers(0, n, n)]) for _ in range(n_boot)]
    lo, hi = np.percentile(stats, [100*alpha/2, 100*(1-alpha/2)])
    return metric_fn(values), lo, hi

hits = np.array([1,1,0,1,1,1,0,1,1,1,0,1,1,1,1,0,1,1,1,0])   # 20 query, 1 = đúng trong top-5
m, lo, hi = bootstrap_ci(hits, np.mean)
print(f"recall@5 = {m:.3f}  95% CI = [{lo:.3f}, {hi:.3f}]")
```

Chạy thật:

```
recall@5 = 0.750  95% CI = [0.550, 0.950]
```

→ Với 20 query, recall@5 = 0.75 thật ra nằm đâu đó giữa **55% và 95%**.

### 5.2 So sánh hai hệ thống — bootstrap trên HIỆU

Sai lầm phổ biến: tính CI riêng cho A và B rồi xem chúng có chồng lấn không. **Cách đó quá bảo
thủ.** Đúng hơn là bootstrap trên **hiệu theo cặp**, vì cùng một tập query.

```python
def paired_bootstrap(hits_a, hits_b, n_boot=10000, seed=0):
    rng = np.random.default_rng(seed)
    a, b = np.asarray(hits_a), np.asarray(hits_b)
    n = len(a)
    idx   = rng.integers(0, n, (n_boot, n))       # lấy mẫu lại CÙNG chỉ số cho cả hai
    diffs = (a[idx] - b[idx]).mean(axis=1)
    lo, hi = np.percentile(diffs, [2.5, 97.5])
    return (a - b).mean(), lo, hi, (diffs > 0).mean()

a = np.array([1,1,0,1,1,1,0,1,1,1,0,1,1,1,1,0,1,1,1,0])       # hệ mới
b = np.array([1,0,0,1,1,1,0,1,0,1,0,1,1,0,1,0,1,1,1,0])       # baseline
d, lo, hi, p = paired_bootstrap(a, b)
print(f"chênh lệch = {d:+.3f}  95% CI = [{lo:+.3f}, {hi:+.3f}]  P(mới > cũ) = {p:.3f}")
```

Chạy thật:

```
chênh lệch = +0.150  95% CI = [+0.000, +0.300]  P(mới > cũ) = 0.964
```

**Đọc:** hệ mới tốt hơn 15 điểm, CI chạm 0 — bằng chứng khá nhưng **chưa chắc chắn** ở mức 95%.
Với n=20 thì đây là điều bình thường. Muốn kết luận chắc → tăng n.

### 5.3 Quy tắc báo cáo

> **Từ nay, mọi con số eval trong repo của bạn phải có dạng:**
> `recall@5 = 0.750 [0.550, 0.900], n=20`
>
> Không có `n` và CI thì con số đó không dùng để ra quyết định được. Đưa quy tắc này vào
> template báo cáo, nó sẽ tự thay đổi cách cả team nói chuyện về kết quả.

---

## 6. Confidence & calibration

### 6.1 Ba nguồn confidence — bạn mới dùng một

| Nguồn | Cách lấy | Chi phí | Bạn đang dùng? |
|---|---|---|---|
| **Model tự khai** ("confidence": 0.9) | Hỏi trong prompt | tốn output token | ✅ có |
| **Logprob** của token giá trị | `logprobs=True` | **miễn phí** | ❌ chưa |
| **Self-consistency** — chạy k lần, đo mức đồng thuận | k lần gọi | đắt k× | 🟡 một phần |
| **Margin retrieval** — top1 − top2 | có sẵn | miễn phí | ❌ chưa |

> **Model tự khai confidence là nguồn kém tin cậy nhất trong bốn cái** — LLM nổi tiếng là quá tự
> tin và con số nó khai không tương quan chặt với độ đúng. Logprob và margin đều **miễn phí** và
> thường tốt hơn. Đây là cải tiến rẻ nhất trong toàn bộ file này.

### 6.2 Quy trình calibration

```
  ① Thu thập (confidence, đúng/sai) trên ≥200 mẫu val
  ② Vẽ reliability diagram (xem 02 §7.3)
  ③ Fit Platt scaling:   p_calib = σ(a·logit(p_raw) + b)
  ④ Chọn ngưỡng theo CHI PHÍ, không theo con số đẹp:
       ngưỡng cao → ít FP, nhiều ca phải review tay
       ngưỡng thấp → ít review, nhiều FP (tiền thật)
  ⑤ Vẽ đường cong đánh đổi và ĐỂ BUSINESS CHỌN ĐIỂM
```

```
   % tự động hóa
    100│●
       │  ●
     80│    ●
       │      ●───────── business chọn ở đây:
     60│        ●        "tự động 60%, FP ≤ 2%"
       │          ●
     40│            ●
       └──────────────────────── tỉ lệ FP
        0%   2%   5%   10%

   Đây là một quyết định KINH DOANH, không phải kỹ thuật.
   Việc của bạn là VẼ đường cong, không phải chọn điểm.
```

---

## 7. Tính không tái lập — đo nó, đừng phớt lờ

Cùng input, chạy 2 lần, ra 2 kết quả khác nhau. Nguyên nhân: `temperature > 0`, batching không
tất định ở tầng backend, thứ tự cộng dấu phẩy động trên GPU.

```python
# Chạy cùng 1 techpack N lần, đo độ ổn định
outs = [run_pipeline(techpack) for _ in range(10)]
fields = outs[0].keys()
for f in fields:
    vals = [o[f] for o in outs]
    n_unique = len(set(map(str, vals)))
    if n_unique > 1:
        print(f"⚠ {f}: {n_unique} giá trị khác nhau trong 10 lần chạy")
```

> **Vì sao phải làm:** nếu độ dao động nội tại là ±4 điểm, thì mọi "cải tiến +3 điểm" bạn từng
> báo cáo đều là nhiễu. **Bạn cần biết con số dao động này trước khi tin bất kỳ so sánh nào.**
> Đây là 2 giờ làm việc và nó hiệu chỉnh lại toàn bộ lịch sử đo của bạn.
>
> Cách giảm: `temperature=0`, cố định seed, và với các bước tất định thì **cache theo hash input**.

---

## 8. LLM-as-judge — dùng được, nhưng phải biết cái bẫy

### 8.1 Khi nào dùng

| Dùng được | Không nên dùng |
|---|---|
| Chấm chất lượng mô tả tự do | Chấm đúng/sai có ground truth (dùng exact match) |
| So sánh cặp A/B | Chấm điểm tuyệt đối 1–10 (rất nhiễu) |
| Sàng lọc sơ bộ trước khi người xem | Là **bằng chứng cuối cùng** để ra quyết định |

### 8.2 Năm thiên lệch đã được ghi nhận

| Thiên lệch | Là gì | Khắc phục |
|---|---|---|
| **Vị trí** | Thích đáp án đứng trước | Chạy cả 2 thứ tự, lấy trung bình |
| **Độ dài** | Thích câu trả lời dài hơn | Kiểm soát độ dài, hoặc đo tương quan length-score |
| **Tự ưu ái** | Model thích output của chính họ model đó | Dùng judge khác họ với model sinh |
| **Nới điểm** | Ngại cho điểm thấp | Dùng rubric nhị phân thay vì thang 1–10 |
| **Định dạng** | Thích markdown đẹp | Chuẩn hóa format trước khi chấm |

### 8.3 Luật bắt buộc

> **Luôn calibrate judge với người.** Lấy 50 mẫu, cho người chấm, cho judge chấm, đo tương quan
> (Spearman) hoặc κ. Nếu κ < 0.6, **judge không dùng được cho bài toán này** — và bạn phải biết
> điều đó trước khi dựa vào nó. Judge không được calibrate là cách rất hiệu quả để tự lừa mình
> một cách có hệ thống.

---

## 9. Regression suite — đưa eval vào CI

```yaml
# .gitlab-ci.yml — ý tưởng
eval:golden:
  script:
    - python -m motives.eval.run --suite golden-fast --out report.json
    - python -m motives.eval.gate --report report.json --baseline baselines/main.json
  rules:
    - changes: ["motives/src/**/*.py", "motives/src/**/prompts/**"]
```

| Suite | Số case | Thời gian | Chạy khi nào | Cổng |
|---|---|---|---|---|
| `smoke` | 3 | < 1 phút | mỗi commit | pipeline không crash |
| `golden-fast` | 20 | ~10 phút | mỗi MR | **không tụt quá CI dưới của baseline** |
| `golden-full` | 100+ | ~2 giờ | hằng đêm | báo cáo xu hướng |
| `production-shadow` | liên tục | — | luôn | cảnh báo drift |

> **Thiết kế cổng cho đúng:** đừng chặn MR khi metric giảm 1 điểm (đó là nhiễu, xem §5). Chặn khi
> metric **tụt xuống dưới cận dưới CI của baseline**. Đây là cách dùng thống kê đúng chỗ, và nó
> giữ cho CI không trở thành thứ mọi người học cách bỏ qua.

---

## 10. Shadow mode — cách đổi model mà không mạo hiểm

```
   production request
         │
         ├──────────────▶ HỆ CŨ ──────▶ trả cho user ✅
         │                  │
         └──────────────▶ HỆ MỚI       │
                            │          │
                            ▼          ▼
                     ┌──────────────────────┐
                     │  LOG CẢ HAI, so sánh │
                     │  KHÔNG ảnh hưởng user│
                     └──────────────────────┘
                                │
                     2-4 tuần, đủ mẫu thật
                                │
                                ▼
                     ┌──────────────────────┐
                     │ Mới > Cũ vượt CI?    │
                     │ CÓ → canary 5% → 50% │
                     │      → 100%          │
                     │ KHÔNG → giữ cũ       │
                     └──────────────────────┘
```

**Ba thứ phải log trong shadow mode:** output của cả hai, latency của cả hai, và **chi phí token
của cả hai**. Một hệ tốt hơn 2 điểm nhưng đắt gấp 3 lần có thể không đáng đổi — và bạn chỉ biết
điều đó nếu đã log.

---

## 11. Bài tập

| # | Bài | Tiêu chí đạt | Ước lượng | Ưu tiên |
|---|---|---|---|---|
| 1 | Thêm bootstrap CI vào **mọi** số eval hiện có | Báo cáo dạng `x [lo,hi], n=N` | 3h | ⭐⭐⭐ |
| 2 | **Đo dao động nội tại** (§7): chạy 10 lần 1 techpack | 1 con số ±đ | 2h | ⭐⭐⭐ |
| 3 | **Đọc 30 ca sai bằng mắt**, lập bảng phân loại lỗi (§4) | bảng ≥5 loại + ưu tiên | 3h | ⭐⭐⭐ |
| 4 | Mở rộng golden set 20 → 100 case, phân tầng dễ/vừa/khó | 100 case có tag độ khó | 3 tuần | ⭐⭐⭐ |
| 5 | Đo Cohen's κ giữa 2 người trên 30 cặp similarity (§2.3) | 1 con số κ + kết luận về trần | 4h | ⭐⭐ |
| 6 | Bật logprob, vẽ reliability diagram, so với confidence model tự khai | 2 hình + kết luận | 6h | ⭐⭐ |
| 7 | Dựng `golden-fast` vào CI với cổng theo CI dưới | pipeline xanh/đỏ đúng | 8h | ⭐⭐ |

**Bài 1, 2, 3 làm trong tuần này.** Tổng 8 giờ, và chúng thay đổi cách bạn nhìn mọi con số đang có.

---

## 12. Ranh giới trung thực

- **Đã chạy thật:** §5.1 và §5.2 — output là thật, với chính các mảng `hits` viết trong code.
- **Là suy đoán của tôi, cần bạn kiểm:** nhận định ở §2.3 rằng bài similarity của bạn có κ thấp.
  Tôi **không có bằng chứng** — đó là giả thuyết dựa trên tính chủ quan của bài toán. Bài tập 5
  tồn tại để bác bỏ hoặc xác nhận nó.
- **Là ước lượng:** bảng §3.3 (94%/78%/61%...) là **số minh họa tôi bịa ra để làm ví dụ về định
  dạng bảng**, không phải số đo của hệ bạn. Đừng trích dẫn nó.
- **Chưa kiểm chứng trên hệ của bạn:** §9 (cấu hình GitLab CI) — tôi thấy repo có `.gitlab-ci.yml`
  nhưng không đọc nội dung, nên đoạn YAML là mẫu ý tưởng, không phải patch áp được ngay.
- **Đơn giản hóa:** §5.2 dùng paired bootstrap. Có các kiểm định chặt hơn (permutation test,
  McNemar cho dữ liệu nhị phân theo cặp). Với mục đích ra quyết định kỹ thuật, bootstrap là đủ
  và dễ giải thích cho người không làm thống kê.
