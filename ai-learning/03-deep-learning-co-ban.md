# 03 · Deep Learning cơ bản

> **Mục tiêu cụ thể:** hết file này bạn **tự tay train được model đầu tiên** và hiểu từng dòng
> trong vòng lặp train — không copy tutorial.
>
> **Thời lượng:** 15–20 giờ. **Yêu cầu trước:** [01](01-nen-tang-toan.md), [02](02-machine-learning-co-ban.md).

---

## 0. Bản đồ

```
  §1 Neuron → §2 MLP → §3 Backprop TÍNH TAY → §4 PyTorch loop → §5 Vấn đề độ sâu
                              ▲                                         │
                              │                                         ▼
                     ĐÂY LÀ MỤC QUAN TRỌNG NHẤT               §6 Normalization
                     Làm được mục này = vượt ngưỡng            §7 Optimizer & LR
                     L2→L3 của toàn bộ deep learning           §8 Công thức debug
                                                               §9 Project đầu tiên
```

---

## 1. Neuron — đơn vị nhỏ nhất

```
   x₁ ──w₁──┐
   x₂ ──w₂──┼──▶ Σ ──▶ z = w·x + b ──▶ φ(z) ──▶ a
   x₃ ──w₃──┘                          ▲
             b (bias)              activation
```

Không có `φ` thì mọi lớp chồng lên nhau vẫn chỉ là **một** phép biến đổi tuyến tính:
`W₂(W₁x) = (W₂W₁)x`. **Activation phi tuyến chính là lý do "sâu" có ý nghĩa.**

| Activation | Công thức | Đạo hàm | Dùng khi |
|---|---|---|---|
| **ReLU** | `max(0,z)` | `1 nếu z>0, ngược lại 0` | Mặc định cho lớp ẩn. Rẻ, không vanishing ở nhánh dương |
| **GELU** | `z·Φ(z)` | mượt | Mặc định trong Transformer (BERT, GPT, Qwen) |
| **Sigmoid** | `1/(1+e⁻ᶻ)` | `σ(1−σ)` ≤ 0.25 | **Chỉ ở lớp output** binary. Ở lớp ẩn → vanishing gradient |
| **Tanh** | `(eᶻ−e⁻ᶻ)/(eᶻ+e⁻ᶻ)` | `1−tanh²` | Hiếm, trong RNN cũ |
| **Softmax** | §[01](01-nen-tang-toan.md)§4 | — | Chỉ ở lớp output multi-class |

> **Vì sao sigmoid ở lớp ẩn là sai lầm lịch sử:** đạo hàm tối đa 0.25. Qua 10 lớp,
> gradient nhân dồn tối đa `0.25¹⁰ ≈ 1e-6` → lớp đầu gần như không học. ReLU (đạo hàm = 1)
> giải quyết chính xác vấn đề này, và đó là một trong hai lý do deep learning cất cánh sau 2012
> (lý do kia là GPU).

---

## 2. MLP — xếp neuron thành lớp

```
  input          hidden (4)        output
  (3)             ReLU              (2)
   ●──────┐    ┌───●───┐         ┌───●
   ●──────┼───▶│   ●   │────────▶│
   ●──────┘    │   ●   │         └───●
               └───●───┘
        W₁: (3,4)      W₂: (4,2)
        b₁: (4,)       b₂: (2,)

  Tổng tham số = 3·4 + 4 + 4·2 + 2 = 26
```

```python
import torch.nn as nn
model = nn.Sequential(
    nn.Linear(3, 4), nn.ReLU(),
    nn.Linear(4, 2),
)
print(sum(p.numel() for p in model.parameters()))   # 26
```

**Bài tập nhỏ nhưng quan trọng:** tính tay số tham số của `nn.Linear(768, 3072)` →
`768×3072 + 3072 = 2,362,368`. Một block Transformer có 2 lớp như vậy trong FFN. Nhân với số
layer → bạn hiểu vì sao model 7B "nặng 7 tỉ tham số". → [11](11-mlops-serving.md) §2

---

## 3. Backprop tính tay — mục quan trọng nhất của toàn bộ file

Đây là bài kiểm tra mốc **M1** ở [README](README.md). Làm được nó, bạn không còn sợ deep learning.

### 3.1 Mạng ví dụ

```
  x = [1.0, 2.0]      W1 = [[0.1, 0.2],     b1 = [0.0, 0.0]
                            [0.3, 0.4]]
  y = 1.0 (nhãn)      W2 = [[0.5], [0.6]]   b2 = [0.0]

  forward:  z1 = x @ W1 + b1  →  a1 = ReLU(z1)  →  z2 = a1 @ W2 + b2  →  L = (z2 − y)²
```

### 3.2 Forward — tính từng bước

```
  z1 = [1·0.1 + 2·0.3,  1·0.2 + 2·0.4] = [0.7, 1.0]
  a1 = ReLU([0.7, 1.0])                 = [0.7, 1.0]     (cả hai dương → giữ nguyên)
  z2 = 0.7·0.5 + 1.0·0.6                = 0.95
  L  = (0.95 − 1.0)²                    = 0.0025
```

### 3.3 Backward — chain rule, đi ngược

```
  ∂L/∂z2  = 2(z2 − y)              = 2(0.95 − 1.0)        = −0.10

  ∂L/∂W2  = a1ᵀ · ∂L/∂z2           = [0.7, 1.0] · (−0.10) = [−0.070, −0.100]
  ∂L/∂b2  = ∂L/∂z2                                        = −0.10

  ∂L/∂a1  = ∂L/∂z2 · W2ᵀ           = −0.10 · [0.5, 0.6]   = [−0.050, −0.060]
  ∂L/∂z1  = ∂L/∂a1 ⊙ ReLU'(z1)     = [−0.05, −0.06] ⊙ [1,1] = [−0.050, −0.060]

  ∂L/∂W1  = xᵀ · ∂L/∂z1            = [[1],[2]] · [−0.05, −0.06]
                                    = [[−0.05, −0.06],
                                       [−0.10, −0.12]]
  ∂L/∂b1  = ∂L/∂z1                                        = [−0.050, −0.060]
```

### 3.4 Verify bằng PyTorch

```python
import torch

x  = torch.tensor([[1.0, 2.0]])
y  = torch.tensor([[1.0]])
W1 = torch.tensor([[0.1, 0.2], [0.3, 0.4]], requires_grad=True)
b1 = torch.zeros(2, requires_grad=True)
W2 = torch.tensor([[0.5], [0.6]], requires_grad=True)
b2 = torch.zeros(1, requires_grad=True)

z1 = x @ W1 + b1
a1 = torch.relu(z1)
z2 = a1 @ W2 + b2
L  = ((z2 - y) ** 2).sum()
L.backward()

print("z1 =", z1.detach().numpy(), " z2 =", z2.item(), " L =", L.item())
print("dW2 =", W2.grad.numpy().ravel())
print("db2 =", b2.grad.numpy())
print("dW1 =\n", W1.grad.numpy())
print("db1 =", b1.grad.numpy())
```

Output thật:

```
z1 = [[0.70000005 1.        ]]  z2 = 0.9500000476837158  L = 0.0024999952875077724
dW2 = [-0.06999994 -0.0999999 ]
db2 = [-0.0999999]
dW1 =
 [[-0.04999995 -0.05999995]
 [-0.0999999  -0.11999989]]
db1 = [-0.04999995 -0.05999995]
```

Khớp với tính tay ở §3.3 tới sai số float32 (~5e-8). ✅ Đó cũng là bài học phụ: **float32 không
bao giờ khớp tuyệt đối** — luôn so bằng `np.allclose(..., atol=1e-6)`, đừng so bằng `==`.

> **Bài tập bắt buộc:** làm lại toàn bộ §3.2–§3.3 **với `x = [2.0, −3.0]`** (chú ý ReLU sẽ chặn
> một nhánh về 0 — đó chính là chỗ backprop qua ReLU khác biệt), rồi verify. Không làm bài này
> thì đừng đọc tiếp — phần còn lại của deep learning chỉ là mở rộng của chính nó.

---

## 4. Vòng lặp train trong PyTorch — thuộc lòng 6 dòng

```python
for epoch in range(n_epochs):
    model.train()
    for xb, yb in train_loader:
        optimizer.zero_grad()        # ① xóa gradient cũ — QUÊN DÒNG NÀY = LỖI IM LẶNG PHỔ BIẾN NHẤT
        pred = model(xb)             # ② forward
        loss = criterion(pred, yb)   # ③ tính loss
        loss.backward()              # ④ backprop, điền .grad cho mọi tham số
        optimizer.step()             # ⑤ cập nhật tham số
    model.eval()                     # ⑥ tắt dropout/batchnorm-update
    with torch.no_grad():
        evaluate(model, val_loader)
```

### 4.1 Năm lỗi im lặng — không báo lỗi nhưng model không học

| Lỗi | Triệu chứng | Cách phát hiện |
|---|---|---|
| Quên `zero_grad()` | Loss giảm rồi bật lên bất thường | Gradient tích lũy → norm tăng dần. In `p.grad.norm()` |
| Quên `model.eval()` | Val loss cao hơn hẳn train một cách khó hiểu | Dropout vẫn bật lúc eval |
| Quên `torch.no_grad()` lúc eval | OOM, chậm | Memory tăng theo số batch val |
| Shuffle data **cùng với** nhãn không đồng bộ | Loss đứng yên ở mức random | Kiểm tra bằng cách overfit 1 batch (§8) |
| Learning rate sai bậc | Loss = NaN, hoặc loss không nhúc nhích | LR range test (§7.3) |

### 4.2 Dataset & DataLoader — mẫu chuẩn

```python
from torch.utils.data import Dataset, DataLoader

class SketchPairDataset(Dataset):
    def __init__(self, records, transform=None):
        self.records, self.transform = records, transform
    def __len__(self):
        return len(self.records)
    def __getitem__(self, i):
        r = self.records[i]
        img = load_image(r["path"])
        if self.transform: img = self.transform(img)
        return img, r["label"]

loader = DataLoader(ds, batch_size=32, shuffle=True,
                    num_workers=4, pin_memory=True, drop_last=True)
```

`num_workers` > 0 gần như luôn cần — nếu không, GPU sẽ ngồi chờ CPU đọc ảnh. Triệu chứng:
`nvidia-smi` báo GPU util dao động 10–40% thay vì >90%.

---

## 5. Vì sao mạng sâu khó train — và ba phát minh giải quyết nó

```
  Mạng 5 lớp:  gradient lớp 1 = g₅ · g₄ · g₃ · g₂ · g₁

  mỗi |gᵢ| ≈ 0.5  →  0.5⁵ = 0.031      → VANISHING, lớp đầu đứng yên
  mỗi |gᵢ| ≈ 1.5  →  1.5⁵ = 7.6        → EXPLODING, loss = NaN
  mỗi |gᵢ| ≈ 1.0  →  1.0⁵ = 1.0        → ổn định ← mục tiêu
```

| Phát minh | Giải quyết gì | Ý tưởng một câu |
|---|---|---|
| **Residual connection** (ResNet, 2015) | Vanishing | `y = f(x) + x` → gradient có "đường cao tốc" đi thẳng về, `∂y/∂x = ∂f/∂x + 1` |
| **Normalization** (BatchNorm/LayerNorm) | Cả hai | Giữ phân bố activation ổn định qua các lớp |
| **Khởi tạo tốt** (He/Xavier) | Cả hai | Chọn scale khởi tạo sao cho variance không co/nở qua từng lớp |

Cộng thêm **gradient clipping** (`torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)`) —
chặn cứng exploding. Gần như bắt buộc khi fine-tune LLM.

> **Liên hệ trực tiếp:** mọi model bạn đang dùng (Qwen3-VL, bge-m3, ViT-B-16 của FashionCLIP)
> đều là stack Transformer block có residual + LayerNorm. Hiểu §5 là hiểu vì sao chúng train được.

---

## 6. BatchNorm vs LayerNorm — phân biệt cho đúng

```
   Tensor (batch=4, features=3)

        f1   f2   f3
   s1 [  •    •    •  ]  ─┐
   s2 [  •    •    •  ]   │ LayerNorm: chuẩn hóa theo HÀNG
   s3 [  •    •    •  ]   │ (mỗi mẫu độc lập)
   s4 [  •    •    •  ]  ─┘
        │    │    │
        └────┴────┴──── BatchNorm: chuẩn hóa theo CỘT (qua cả batch)
```

| | BatchNorm | LayerNorm |
|---|---|---|
| Chuẩn hóa theo | Cả batch, mỗi feature | Mỗi mẫu, qua các feature |
| Phụ thuộc batch size | **Có** — batch nhỏ thì thống kê nhiễu | Không |
| Train ≠ eval | **Có** — eval dùng running mean/var | Không, giống nhau |
| Dùng ở | CNN (ResNet) | **Transformer, NLP, mọi LLM** |

> **Vì sao Transformer dùng LayerNorm:** câu có độ dài khác nhau, batch có padding → thống kê
> theo batch bị nhiễu bởi padding. LayerNorm miễn nhiễm. Ngoài ra LayerNorm cho phép batch=1
> lúc inference mà không cần chế độ eval riêng.
>
> `RMSNorm` (Llama, Qwen dùng) là LayerNorm bỏ bước trừ mean — rẻ hơn ~10%, chất lượng tương đương.

---

## 7. Optimizer & learning rate

### 7.1 Ba optimizer cần biết

| Optimizer | Cập nhật | Khi nào |
|---|---|---|
| SGD + momentum | `v ← βv + g;  θ ← θ − ηv` | CNN vision, đôi khi generalize tốt hơn Adam |
| **Adam** | Thích nghi lr theo từng tham số (dùng moment 1 và 2) | Mặc định |
| **AdamW** ⭐ | Adam + weight decay **tách rời** khỏi gradient | Chuẩn cho mọi Transformer/LLM. Dùng cái này |

### 7.2 Learning rate schedule — hình chuẩn

```
   lr
    │      ╱‾‾‾╲___
    │     ╱        ╲_____
    │    ╱               ╲_____
    │   ╱                      ╲____
    │  ╱                            ╲___
    └─┴────────────────────────────────────── step
      └warmup┘└──── cosine decay ────┘
       3-10%
       tổng step

   Warmup: tránh gradient lớn bất thường ở bước đầu phá tham số đã pretrain
   Decay:  cuối quá trình cần bước nhỏ để hội tụ vào đáy
```

```python
from torch.optim.lr_scheduler import OneCycleLR
opt = torch.optim.AdamW(model.parameters(), lr=3e-4, weight_decay=0.01)
sched = OneCycleLR(opt, max_lr=3e-4, total_steps=n_steps, pct_start=0.05)
# trong loop: opt.step(); sched.step()
```

### 7.3 LR range test — cách chọn lr trong 5 phút thay vì đoán

Tăng lr theo cấp số nhân qua ~100 batch, vẽ `loss` theo `log(lr)`:

```
   loss
    │╲                              ╱
    │ ╲                           ╱
    │  ╲___                     ╱
    │      ╲__________________╱
    └──────────┬─────┬──────────────── log(lr)
              1e-5  1e-3
               │     │
               │     └── lr quá lớn, loss bật lên
               └──────── chọn lr ở đây: ~1/10 điểm thấp nhất
```

### 7.4 Bảng learning rate mặc định — dùng được ngay

| Tình huống | lr | Ghi chú |
|---|---|---|
| Train MLP/CNN nhỏ từ đầu | `1e-3` | AdamW |
| Train Transformer từ đầu | `3e-4` | + warmup |
| **Fine-tune full** model pretrained | `1e-5` – `5e-5` | Lớn hơn → phá kiến thức pretrain |
| **LoRA** fine-tune | `1e-4` – `3e-4` | Cao hơn full FT vì chỉ update adapter |
| Linear probe (đóng băng backbone) | `1e-3` | Chỉ có lớp cuối học |

---

## 8. Công thức debug — thứ tự bắt buộc khi model không học

Đây là checklist tôi khuyên bạn **dán lên màn hình**. Nó tiết kiệm hàng chục giờ.

```
  ① OVERFIT 1 BATCH
     Lấy 8 mẫu, tắt shuffle/augment/regularization, train 500 step.
     ┌─ Loss KHÔNG về ~0  →  CÓ BUG. Dừng lại. Đừng train tiếp.
     │   Nghi phạm: shape sai · nhãn lệch · loss sai · lr sai bậc · quên zero_grad
     └─ Loss về ~0        →  Pipeline đúng. Đi tiếp ②

  ② KIỂM TRA DỮ LIỆU BẰNG MẮT
     In/vẽ 20 mẫu KÈM NHÃN, ngay trước khi vào model (sau mọi transform).
     Bạn sẽ ngạc nhiên số lần lỗi nằm ở đây.

  ③ SO VỚI BASELINE NGỚ NGẨN
     Model của bạn có thắng "luôn đoán lớp phổ biến nhất" không?
     Không thắng → chưa học được gì.

  ④ LR RANGE TEST (§7.3)

  ⑤ THEO DÕI GRADIENT NORM
     for n, p in model.named_parameters():
         if p.grad is not None: print(n, p.grad.norm().item())
     Toàn 0 → mất kết nối đồ thị (detach nhầm?). Toàn NaN/inf → exploding, clip lại.

  ⑥ TĂNG ĐỘ PHỨC TẠP TỪ TỪ
     1 lớp → 2 lớp → thêm augment → thêm regularization. Mỗi lần đổi MỘT thứ.
```

> **Nguyên tắc vàng:** nếu ① thất bại thì mọi thứ sau đó vô nghĩa. Người mới hay nhảy thẳng
> sang tune hyperparameter khi thật ra có bug shape. Luôn làm ① trước.

---

## 9. Project đầu tiên — chạy trên data thật của bạn

**Bài toán:** phân loại sketch FRONT vs BACK — việc mà hiện tại VLM đang làm bằng prompt
(`sketches_extraction.py`).

**Vì sao chọn bài này:**
- Có sẵn dữ liệu (`techpack_sketches*` trong Qdrant, và ảnh gốc trên disk).
- Nhãn rẻ — người nhìn 1 giây biết ngay front hay back.
- **Có baseline để so:** chính VLM hiện tại. Nếu classifier nhỏ đạt tương đương → bạn cắt được
  một lời gọi VLM trên **mỗi sketch**, tiết kiệm token thật.
- Đủ nhỏ để train trên CPU hoặc 1 GPU trong vài phút.

**Kế hoạch 3 bước, tăng dần:**

| Bước | Cách làm | Tham số train | Thời gian | Kỳ vọng |
|---|---|---|---|---|
| **A. Linear probe** | Lấy embedding FashionCLIP 512-dim **đã có sẵn trong Qdrant**, train `nn.Linear(512, 2)` | 1,026 | < 1 phút, CPU | Baseline. Nếu đã >95% thì **dừng luôn**, không cần bước B/C |
| **B. Fine-tune nhẹ** | Mở khóa 2 block cuối của ViT, lr `1e-5` | ~14M | ~10 phút, 1 GPU | +1–3 điểm |
| **C. Train từ đầu** | ResNet-18 từ scratch | 11M | ~30 phút | Thường **tệ hơn** A và B với data nhỏ — làm để tự chứng minh điều này |

```python
# Bước A — đủ ngắn để viết trong 20 phút
import torch, torch.nn as nn
from torch.utils.data import TensorDataset, DataLoader

X = torch.tensor(embeddings, dtype=torch.float32)     # (N, 512) lấy từ Qdrant
y = torch.tensor(labels,     dtype=torch.long)        # 0=FRONT, 1=BACK

# CHIA THEO STYLE, không phải theo ảnh — xem 02 §2.2
tr, va = split_by_style(X, y, groups=styles, test_size=0.2)

model = nn.Linear(512, 2)
opt   = torch.optim.AdamW(model.parameters(), lr=1e-3, weight_decay=0.01)
crit  = nn.CrossEntropyLoss()

for epoch in range(30):
    model.train()
    for xb, yb in DataLoader(TensorDataset(*tr), batch_size=32, shuffle=True):
        opt.zero_grad(); loss = crit(model(xb), yb); loss.backward(); opt.step()
    model.eval()
    with torch.no_grad():
        acc = (model(va[0]).argmax(1) == va[1]).float().mean()
    print(f"epoch {epoch:2d}  val acc {acc:.3f}")
```

> **Kết quả cần báo cáo:** accuracy **kèm khoảng tin cậy** (xem [01](01-nen-tang-toan.md)§5), so với
> accuracy của VLM hiện tại trên **cùng tập test**, và ước lượng token tiết kiệm nếu thay.
> Đó là format báo cáo của một AI Engineer, không phải của người chạy tutorial.

---

## 10. Ranh giới trung thực

- **Đã chạy thật:** §3.4 — output PyTorch trong file là output thật, khớp với tính tay §3.3.
  §2 (đếm 26 tham số) cũng đã chạy.
- **Chưa chạy:** §9 — code đúng khung nhưng `split_by_style`, `embeddings`, `labels` là của bạn.
  Tôi **không biết** classifier bước A sẽ đạt bao nhiêu; con số ">95%" là kỳ vọng dựa trên độ dễ
  của bài toán, **không phải kết quả đo**.
- **Là đơn giản hóa:** §5 trình bày vanishing/exploding bằng tích các đại lượng vô hướng. Thực tế
  là tích các Jacobian và phải nói về singular value. Đủ cho trực giác, thiếu cho lý thuyết.
- **Không bàn tới:** RNN/LSTM chi tiết (xem [00](00-chan-doan-nang-luc.md)§6 — cố ý bỏ), mixed
  precision training, distributed training (DDP/FSDP — quay lại ở [11](11-mlops-serving.md)),
  và các optimizer mới hơn (Lion, Sophia, Muon) — chúng tồn tại, nhưng AdamW vẫn là mặc định an toàn.
