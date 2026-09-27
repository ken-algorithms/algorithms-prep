# 01 · Nền tảng toán — chỉ phần thật sự dùng

> **Nguyên tắc của file này:** không dạy toán để thi. Mỗi khái niệm đều phải trả lời được một câu
> hỏi có thật trong `motivesidp-ai-service`. Cái nào không trả lời được câu hỏi nào thì bị cắt.
>
> **Thời lượng:** 8–12 giờ. **Yêu cầu trước:** biết Python + numpy.

---

## 0. Bản đồ: 5 khối toán, mỗi khối một câu hỏi thật

```
┌─────────────────────┐   trả lời   ┌──────────────────────────────────────────┐
│ 1. Vector & dot     │────────────▶│ Cosine similarity trong Qdrant tính gì?  │
│    product          │             │ Vì sao phải L2-normalize 2 lần?          │
└─────────────────────┘             └──────────────────────────────────────────┘
┌─────────────────────┐             ┌──────────────────────────────────────────┐
│ 2. Ma trận & phép   │────────────▶│ Linear layer là gì? Vì sao 512-dim →     │
│    nhân             │             │ 128-dim lại "mất thông tin"?             │
└─────────────────────┘             └──────────────────────────────────────────┘
┌─────────────────────┐             ┌──────────────────────────────────────────┐
│ 3. Đạo hàm & chain  │────────────▶│ "Model học" nghĩa là gì? Gradient là gì? │
│    rule             │             │ Vì sao learning rate quá lớn thì nổ?     │
└─────────────────────┘             └──────────────────────────────────────────┘
┌─────────────────────┐             ┌──────────────────────────────────────────┐
│ 4. Xác suất &       │────────────▶│ temperature trong LLM làm gì về mặt toán?│
│    softmax          │             │ logprob dùng để lọc output thế nào?      │
└─────────────────────┘             └──────────────────────────────────────────┘
┌─────────────────────┐             ┌──────────────────────────────────────────┐
│ 5. Thống kê suy     │────────────▶│ 75.8% trên 19 mẫu có ý nghĩa không?      │
│    diễn             │             │ Bao nhiêu mẫu thì đủ?                    │
└─────────────────────┘             └──────────────────────────────────────────┘
```

---

## 1. Vector, dot product, cosine — nền của toàn bộ retrieval

### 1.1 Ba phép toán, ba ý nghĩa hình học

Cho hai vector `a`, `b` trong không gian `d` chiều:

| Phép | Công thức | Ý nghĩa hình học | Dùng ở đâu trong repo |
|---|---|---|---|
| **Norm (độ dài)** | `‖a‖ = √(Σ aᵢ²)` | Độ dài mũi tên | `FashionCLIPEmbeddingService` L2-normalize trước khi trả |
| **Dot product** | `a·b = Σ aᵢbᵢ` | Hình chiếu của `a` lên `b`, nhân với `‖b‖` | Qdrant `Distance.DOT` |
| **Cosine** | `cos(a,b) = (a·b)/(‖a‖‖b‖)` | **Góc** giữa 2 mũi tên, bỏ qua độ dài | Qdrant `Distance.COSINE`, collection `techpack_sketches*` |

### 1.2 Điều quan trọng nhất bạn cần rút ra

> **Nếu đã L2-normalize (‖a‖ = ‖b‖ = 1) thì cosine = dot product.**

Điều này không phải mẹo vặt. Nó là lý do:

1. **Qdrant `Distance.COSINE` tự normalize khi insert.** Nếu bạn cũng đã normalize ở client
   (FashionCLIP service đang normalize 2 lần), việc đó **vô hại nhưng thừa** — normalize một
   vector đã normalize là no-op.
2. **`Distance.DOT` nhanh hơn `Distance.COSINE`** (bớt 2 phép chia). Nếu vector đã normalize từ
   phía client thì dùng `DOT` là đủ và nhanh hơn. → một tối ưu nhỏ, miễn phí.
3. **Cosine bỏ qua độ lớn.** Với embedding ảnh, "độ lớn" thường mang tín hiệu về độ tự tin/độ
   đậm nét của ảnh. Cosine vứt tín hiệu đó đi. Đôi khi đó là điều bạn muốn, đôi khi không.

### 1.3 Chạy thử — 10 phút

```python
import numpy as np

def l2_normalize(v):
    return v / np.linalg.norm(v)

a = np.array([3.0, 4.0])          # ‖a‖ = 5
b = np.array([6.0, 8.0])          # cùng hướng, dài gấp đôi
c = np.array([-4.0, 3.0])         # vuông góc với a

print("‖a‖ =", np.linalg.norm(a))                        # 5.0
print("a·b =", a @ b)                                     # 50.0  ← phụ thuộc độ dài
print("cos(a,b) =", l2_normalize(a) @ l2_normalize(b))    # 1.0   ← cùng hướng
print("cos(a,c) =", l2_normalize(a) @ l2_normalize(c))    # 0.0   ← vuông góc
print("normalize 2 lần == 1 lần:",
      np.allclose(l2_normalize(l2_normalize(a)), l2_normalize(a)))   # True
```

### 1.4 Hiện tượng "lời nguyền số chiều" — vì sao cosine sketch của bạn hay rơi vào 0.7–0.9

Trong không gian nhiều chiều, hai vector **ngẫu nhiên** gần như luôn vuông góc (cosine ≈ 0). Nhưng
embedding **không ngẫu nhiên** — chúng bị dồn vào một nón hẹp (anisotropy). Hệ quả: mọi cặp ảnh,
kể cả không liên quan, vẫn ra cosine cao.

```python
import numpy as np
rng = np.random.default_rng(0)

for d in [2, 8, 64, 512]:
    v = rng.normal(size=(2000, d))
    v /= np.linalg.norm(v, axis=1, keepdims=True)
    sims = v[:1000] @ v[1000:].T
    print(f"d={d:4d}  cosine ngẫu nhiên: mean={sims.mean():+.3f}  std={sims.std():.3f}")
```

Kết quả (chạy thật):

```
d=   2  cosine ngẫu nhiên: mean=+0.000  std=0.707
d=   8  cosine ngẫu nhiên: mean=+0.000  std=0.354
d=  64  cosine ngẫu nhiên: mean=-0.000  std=0.125
d= 512  cosine ngẫu nhiên: mean=+0.000  std=0.044
```

**Đọc bảng này:** ở d=512, hai vector ngẫu nhiên có cosine lệch khỏi 0 chưa tới 0.044. Vậy mà
sketch của bạn ra 0.7–0.9 với **mọi cặp** — nghĩa là embedding thật **không** phân tán như ngẫu
nhiên, nó bị dồn cụm. Đó chính là dấu hiệu của domain gap.

> **Kết luận hành động:** con số cosine tuyệt đối (0.85) **không có ý nghĩa gì**. Chỉ có **thứ
> hạng tương đối** và **khoảng cách giữa phân bố positive vs negative** mới có ý nghĩa.
> Đừng bao giờ đặt ngưỡng cứng kiểu `if cosine > 0.8`. → [07](07-embedding-retrieval-rag.md) §4

---

## 2. Ma trận — linear layer thật ra là gì

### 2.1 Nhân ma trận = một lô phép dot product

```
   X (batch=2, in=3)      W (in=3, out=2)        Y = X @ W  (2, 2)
   ┌           ┐          ┌         ┐            ┌                    ┐
   │ 1  2  3   │          │ 1   0   │            │ 1·1+2·0+3·1   ...  │   ┌  4   -2 ┐
   │ 4  5  6   │    @     │ 0  -1   │     =      │                    │ = │ 10   -5 │
   └           ┘          │ 1   0   │            └                    ┘   └         ┘
                          └         ┘
   2 mẫu, mỗi mẫu 3 feature × 3 feature → 2 feature = 2 mẫu, mỗi mẫu 2 feature
```

Một `nn.Linear(3, 2)` trong PyTorch chính là `Y = X @ W + b`, với `W` là tham số **học được**.
Không hơn.

### 2.2 Ba quy tắc shape phải thuộc lòng

| Tình huống | Shape | Ghi nhớ |
|---|---|---|
| Linear layer | `(B, in) @ (in, out) → (B, out)` | Chiều trong phải khớp và **triệt tiêu** |
| Batch matmul (attention) | `(B, H, L, d) @ (B, H, d, L) → (B, H, L, L)` | 2 chiều cuối là ma trận, các chiều trước là batch |
| Broadcasting | `(B, L, d) + (d,) → (B, L, d)` | Chiều thiếu được thêm vào **bên trái**, chiều =1 được nhân bản |

> **99% lỗi lúc mới train model là lỗi shape.** Mẹo: in `tensor.shape` sau mỗi dòng cho tới khi
> quen. Không xấu hổ gì cả.

### 2.3 Giảm chiều 512 → 128 mất gì

Câu hỏi thật: repo của bạn dùng `EMBEDDING_DIM = 128` cho bge-m3 (vốn là 1024-dim) và 512 cho
FashionCLIP. Giảm chiều mất gì?

Một phép chiếu tuyến tính `R^512 → R^128` **luôn làm mất thông tin** — nói chính xác, nó chiếu
không gian 512 chiều xuống một không gian con 128 chiều, mọi thứ vuông góc với không gian con đó
bị xóa sạch.

**Nhưng mất bao nhiêu thì phụ thuộc dữ liệu.** Cách đo: PCA và nhìn tỉ lệ phương sai giữ lại.

```python
import numpy as np
# emb: (N, 512) — embedding thật của bạn từ Qdrant
emb = emb - emb.mean(axis=0)
_, s, _ = np.linalg.svd(emb, full_matrices=False)
var = (s ** 2) / (s ** 2).sum()
for k in [16, 32, 64, 128, 256]:
    print(f"giữ {k:3d} chiều → giữ lại {var[:k].sum()*100:.1f}% phương sai")
```

> **Bài tập thật, 30 phút:** chạy đoạn trên với 1000 sketch embedding từ Qdrant của bạn. Nếu
> 128 chiều giữ >95% phương sai → giảm chiều gần như miễn phí, và bạn tiết kiệm 4× bộ nhớ +
> 4× tốc độ search. Nếu chỉ giữ 60% → bạn đang mất thông tin thật.

---

## 3. Đạo hàm & chain rule — "model học" nghĩa là gì

### 3.1 Toàn bộ deep learning gói trong một vòng lặp

```
        ┌──────────────────────────────────────────────────────┐
        │                                                      │
        │   1. forward:  ŷ = f(x; θ)          (dự đoán)        │
        │   2. loss:     L = loss(ŷ, y)       (sai bao nhiêu)  │
        │   3. backward: g = ∂L/∂θ            (sai theo hướng nào) │
        │   4. update:   θ ← θ − η·g          (đi ngược hướng sai) │
        │                                                      │
        └───────────────────────┬──────────────────────────────┘
                                │ lặp vài nghìn → vài triệu lần
                                ▼
                            θ tốt hơn
```

`η` là **learning rate**. Chỉ có vậy. Mọi thứ khác (Adam, scheduler, momentum) là biến thể của
bước 4.

### 3.2 Gradient = hướng dốc lên. Ta đi **ngược** nó.

Với hàm 1 biến `L(θ) = θ²`, gradient `∂L/∂θ = 2θ`.

```
  L(θ)
   │        ╲                    ╱
   │         ╲                  ╱
   │          ╲                ╱
   │           ╲___     ______╱
   │               ╲___╱
   └────────────────┬──────────────── θ
        θ=-2        0        θ=+2
     g=-4           ↑        g=+4
     đi phải        min      đi trái
     (θ -= η·(-4))           (θ -= η·(+4))
```

**Learning rate quá lớn thì sao?** `θ=2, g=4, η=0.6` → `θ ← 2 − 2.4 = −0.4`... rồi vọt ngược lại.
Với `η > 1/L_smooth` nó **phân kỳ**, loss tăng dần tới `NaN`. Đó chính xác là triệu chứng
"loss = nan" mà ai train model cũng gặp lần đầu.

```python
for lr in [0.1, 0.5, 0.9, 1.1]:
    theta, hist = 2.0, []
    for _ in range(10):
        theta -= lr * 2 * theta
        hist.append(theta)
    print(f"lr={lr}: {[round(h,3) for h in hist[:5]]} ... cuối={hist[-1]:.3g}")
```

```
lr=0.1: [1.6, 1.28, 1.024, 0.819, 0.655] ... cuối=0.215     hội tụ mượt
lr=0.5: [0.0, 0.0, 0.0, 0.0, 0.0] ... cuối=0                nhảy thẳng vào min (may mắn)
lr=0.9: [-1.6, 1.28, -1.024, 0.819, -0.655] ... cuối=0.215  dao động quanh min, vẫn hội tụ
lr=1.1: [-2.4, 2.88, -3.456, 4.147, -4.977] ... cuối=12.4   PHÂN KỲ — biên độ tăng dần
```

### 3.3 Chain rule — lý do backprop tồn tại

Nếu `L = f(g(h(x)))` thì:

```
   ∂L     ∂L    ∂f    ∂g
   ──  =  ── · ── · ──
   ∂x     ∂f    ∂g    ∂h
```

Nhân dồn từ **cuối về đầu**. Đó là "back-propagation": không phải thuật toán thần kỳ, chỉ là
chain rule áp cho đồ thị tính toán.

**Hệ quả thực tế quan trọng:** nếu mỗi nhân tử < 1, tích của 50 lớp → ~0 (**vanishing gradient**,
lớp đầu không học được). Nếu mỗi nhân tử > 1 → nổ (**exploding gradient**). Đây là lý do tồn tại
của residual connection (ResNet), LayerNorm, và gradient clipping. → [03](03-deep-learning-co-ban.md) §5

---

## 4. Xác suất & softmax — cái điều khiển LLM

### 4.1 Softmax: biến điểm số tùy ý thành phân bố xác suất

```
                exp(zᵢ / T)
   p(i)  =  ─────────────────
             Σⱼ exp(zⱼ / T)
```

`z` là **logits** (điểm thô model xuất ra), `T` là **temperature** — đúng cái bạn set
`temperature=0.1` trong `ModelConfig`.

### 4.2 Temperature làm gì — bằng số cụ thể

```python
import numpy as np
logits = np.array([4.0, 3.0, 1.0, 0.5])      # 4 token ứng viên

def softmax(z, T=1.0):
    e = np.exp((z - z.max()) / T)             # trừ max để tránh tràn số
    return e / e.sum()

for T in [0.1, 0.5, 1.0, 2.0]:
    print(f"T={T:4.1f} → {np.round(softmax(logits, T), 4)}")
```

```
T= 0.1 → [1.     0.     0.     0.    ]    gần như tất định — luôn chọn token mạnh nhất
T= 0.5 → [0.8782 0.1188 0.0022 0.0008]
T= 1.0 → [0.6907 0.2541 0.0344 0.0209]    phân bố "gốc" của model
T= 2.0 → [0.4991 0.3027 0.1114 0.0867]    phẳng hơn — sáng tạo/hỗn loạn hơn
```

**Đọc bảng này để hiểu code của bạn:** `temperature=0.1` khiến model gần như tất định →
tốt cho extraction JSON (bạn muốn cùng input ra cùng output), nhưng **không phải bằng 0** —
vẫn còn 0.01% khả năng đi lệch. Nếu muốn tái lập tuyệt đối phải set `T=0` (greedy) **và** cố định
seed — mà nhiều backend self-host vẫn không đảm bảo do batching thay đổi thứ tự phép cộng
dấu phẩy động.

> **Đây là lý do thật khiến cùng một techpack chạy 2 lần ra 2 kết quả khác nhau.** Không phải bug.
> → [09](09-eval-va-do-luong.md) §7

### 4.3 Cross-entropy loss — cái mọi LLM đang tối ưu

```
   L  =  − log p(token_đúng)
```

Chỉ vậy. Nếu model gán xác suất 0.9 cho token đúng → `L = 0.105`. Nếu gán 0.01 → `L = 4.6`.
Phạt rất nặng khi model **tự tin mà sai**.

| p(đúng) | Loss | Diễn giải |
|---|---|---|
| 0.99 | 0.01 | gần hoàn hảo |
| 0.90 | 0.105 | tốt |
| 0.50 | 0.693 | đoán bừa 2 lựa chọn |
| 0.10 | 2.30 | tệ |
| 0.01 | 4.61 | rất tệ |
| →0 | →∞ | vì sao có `eps` trong mọi implementation |

**Perplexity** = `exp(loss)` — "model đang phân vân giữa bao nhiêu lựa chọn". Loss 2.30 →
perplexity 10 → như thể model đang chọn ngẫu nhiên giữa 10 token.

### 4.4 Logprob — công cụ lọc output bạn chưa dùng

API OpenAI-compatible (cả vLLM lẫn llama.cpp server) trả được `logprobs`. Với mỗi token sinh ra
bạn biết model **tự tin bao nhiêu**.

```python
resp = client.chat.completions.create(..., logprobs=True, top_logprobs=3)
for tok in resp.choices[0].logprobs.content:
    print(tok.token, round(np.exp(tok.logprob), 3))
```

> **Ứng dụng ngay cho motivesidp:** với extraction JSON, tính trung bình logprob của các token
> thuộc **giá trị** field (không tính token của dấu ngoặc/tên field). Field nào có confidence thấp
> → đẩy sang human review thay vì tin. Đây là một **confidence signal miễn phí**, không tốn thêm
> token nào, và hiện `provenance` của bạn chưa dùng. → [09](09-eval-va-do-luong.md) §6

---

## 5. Thống kê suy diễn — vì sao 75.8% trên 19 mẫu không nói lên điều gì

### 5.1 Khoảng tin cậy Wilson cho tỉ lệ

Đừng dùng công thức `p ± 1.96√(p(1−p)/n)` (Wald) — nó sai nặng khi `n` nhỏ hoặc `p` gần 0/1.
Dùng **Wilson**:

```python
import math

def wilson(k, n, z=1.96):
    if n == 0: return (0.0, 1.0)
    p = k / n
    denom = 1 + z*z/n
    center = (p + z*z/(2*n)) / denom
    half = z * math.sqrt(p*(1-p)/n + z*z/(4*n*n)) / denom
    return (center - half, center + half)

for n, p in [(19, 0.758), (30, 0.758), (100, 0.758), (280, 0.758), (256, 0.891)]:
    lo, hi = wilson(round(p*n), n)
    print(f"n={n:4d}  p̂={p:.3f}  →  95% CI = [{lo:.3f}, {hi:.3f}]  rộng ±{(hi-lo)/2*100:.1f}đ")
```

```
n=  19  p̂=0.758  →  95% CI = [0.512, 0.882]  rộng ±18.5đ
n=  30  p̂=0.758  →  95% CI = [0.591, 0.882]  rộng ±14.6đ
n= 100  p̂=0.758  →  95% CI = [0.668, 0.833]  rộng ±8.3đ
n= 280  p̂=0.758  →  95% CI = [0.704, 0.804]  rộng ±5.0đ
n= 256  p̂=0.891  →  95% CI = [0.846, 0.923]  rộng ±3.8đ
```

### 5.2 Đọc bảng này thế nào

- **PTU 75.8% trên 19 style:** khoảng thật là **51.2%–88.2%**. Nếu lần sau ra 82%, bạn **không
  chứng minh được** là đã cải thiện. Hai khoảng chồng lấn hoàn toàn.
- **Hard accuracy 89.1% trên 256 ô:** khoảng **84.6%–92.3%** — hẹp hơn nhiều, con số này **dùng
  được**. Đây là lý do bạn nên báo cáo số 256-ô, không phải số 19-style, khi muốn thuyết phục.
- **Muốn phát hiện cải thiện 5 điểm:** cần n ≈ 280 với bài toán binary. Thực tế cần hơn vì còn
  phải so sánh 2 nhóm.

### 5.3 Quy tắc ngón tay cái

| n | Dùng để làm gì | KHÔNG dùng để |
|---|---|---|
| 5–20 | Smoke test, phát hiện lỗi thô, debug | So sánh 2 phương án |
| 30–50 | Phát hiện thay đổi **lớn** (>15 điểm) | Tuyên bố cải thiện nhỏ |
| 100–300 | So sánh 2 phương án, chênh 5–10 điểm | Chênh <3 điểm |
| 500+ | Chênh nhỏ, phân tích theo phân khúc | — |

> **Hành động cho bạn:** golden set PTU 19 style là **quá nhỏ để ra quyết định**. Ưu tiên số 1 của
> quý này nên là mở rộng nó lên ~100 style. Việc đó có giá trị hơn mọi tinh chỉnh prompt.
> → [09](09-eval-va-do-luong.md) §2

---

## 6. Bài tập kiểm tra — đạt hết thì bạn xong file này

| # | Bài | Tiêu chí đạt |
|---|---|---|
| 1 | Viết `cosine(a,b)` bằng numpy thuần, verify với `scipy.spatial.distance.cosine` | khớp tới 1e-9 (nhớ: scipy trả **distance** = 1 − cosine) |
| 2 | Lấy 1000 sketch embedding từ Qdrant, vẽ histogram cosine của cặp cùng-style vs khác-style | 2 histogram trên cùng 1 figure, có ghi độ chồng lấn |
| 3 | Chạy PCA trên embedding đó, trả lời: 128 chiều giữ bao nhiêu % phương sai | một con số % |
| 4 | Cài `θ² ` gradient descent với `lr ∈ {0.1, 0.5, 0.9, 1.1}`, giải thích vì sao 1.1 phân kỳ | chỉ ra điều kiện `lr < 2/L` |
| 5 | Tính softmax tay (máy tính bỏ túi) cho `z=[2,1,0]`, `T=1` rồi `T=0.5` | khớp numpy tới 3 chữ số |
| 6 | Tính Wilson CI cho kết quả eval mới nhất của bạn, đưa vào report | có dòng `p̂ = x.xx [lo, hi], n=N` |

---

## 7. Ranh giới trung thực

- **Đã chạy thật:** toàn bộ đoạn code trong §1.4, §3.2, §4.2, §5.1 — output in trong file là output thật.
- **Chưa chạy trên data của bạn:** §2.3 (PCA) và bài tập 2, 3 — cần embedding thật từ Qdrant của bạn.
  Tôi không có quyền truy cập.
- **Là đơn giản hóa có chủ đích:** §3.3 trình bày chain rule cho hàm 1 biến. Thực tế là Jacobian
  cho hàm nhiều biến; đơn giản hóa này **đúng về trực giác, thiếu về hình thức**. Đủ cho tầng
  engineer, không đủ nếu bạn muốn đọc paper optimization.
- **Không bàn tới:** eigenvalue/eigenvector, SVD chi tiết, phân phối nhiều chiều, kiểm định giả
  thuyết. Chúng cần thiết nhưng không phải bây giờ — quay lại ở [09](09-eval-va-do-luong.md) khi
  cần so sánh 2 hệ thống một cách chặt chẽ.
