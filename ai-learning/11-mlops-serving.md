# 11 · MLOps & Serving

> **Đây là trục mạnh nhất của bạn** (L3 theo [00](00-chan-doan-nang-luc.md)§2) — bạn đã deploy
> vLLM, llama.cpp, TEI, GGUF Q6, AWQ, cả trên Jetson Orin và đã benchmark. File này lấp ba lỗ hổng
> còn lại: **toán VRAM**, **đo throughput đúng cách**, và **vòng đời model**.
>
> **Thời lượng:** 8–10 giờ. Đọc nhanh, bỏ qua mục nào bạn đã biết.

---

## 1. Bản đồ runtime — cái nào cho việc gì

| Runtime | Cho | Ưu | Nhược | Bạn dùng |
|---|---|---|---|---|
| **vLLM** | LLM/VLM, GPU, nhiều request | PagedAttention, continuous batching, throughput cao nhất | Chỉ GPU, cần VRAM đủ chứa model | ✅ |
| **llama.cpp / GGUF** | LLM, CPU hoặc GPU yếu, quantize mạnh | Chạy được nơi vLLM không chạy, Q4/Q6 | Throughput thấp hơn khi batch lớn | ✅ (Jetson) |
| **TEI** | Embedding & reranker | Tối ưu riêng cho encoder, rất nhanh | Chỉ encoder | ✅ |
| **TensorRT-LLM** | Tối đa hiệu năng NVIDIA | Nhanh nhất | Build phức tạp, khóa vào NVIDIA | ❌ |
| **Ollama** | Dev local | Dễ nhất | Không dành cho production nhiều request | ❌ |

---

## 2. ⭐ Toán VRAM — phải tính được trước khi deploy

### 2.1 Hai thành phần

```
   VRAM = TRỌNG SỐ + KV CACHE + (activation, overhead ~1-2GB)
           │             │
           │             └── tỉ lệ với: batch × context × layer
           └── tỉ lệ với: số tham số × byte/tham số
```

### 2.2 Trọng số

```
   VRAM_weights (GB) ≈ P (tỉ tham số) × byte_mỗi_tham_số

   fp16/bf16 : 2.0 byte    27B → ~54 GB
   fp8       : 1.0 byte    27B → ~27 GB
   Q8 / int8 : 1.0 byte    27B → ~27 GB
   Q6_K      : ~0.82 byte  27B → ~22 GB   ◀── cấu hình của bạn
   AWQ/GPTQ 4-bit : ~0.6 byte  27B → ~16 GB
   Q4_K_M    : ~0.55 byte  27B → ~15 GB
```

### 2.3 KV cache — thành phần hay bị quên, và là thủ phạm OOM

```
   KV_cache (byte) = 2 × n_layer × n_kv_head × d_head × seq_len × batch × byte_per_elem
                     │
                     └── 2 = Key và Value
```

Ví dụ một model ~27B điển hình (n_layer=64, n_kv_head=8 với GQA, d_head=128, fp16):

```python
def kv_cache_gb(n_layer, n_kv_head, d_head, seq_len, batch, bytes_per=2):
    return 2 * n_layer * n_kv_head * d_head * seq_len * batch * bytes_per / 1024**3

for ctx in [4096, 16384, 32768, 131072]:
    for b in [1, 8, 32]:
        print(f"ctx={ctx:>6}  batch={b:>2}  KV = {kv_cache_gb(64, 8, 128, ctx, b):6.2f} GB")
```

Chạy thật:

```
ctx=  4096  batch= 1  KV =   1.00 GB
ctx=  4096  batch= 8  KV =   8.00 GB
ctx=  4096  batch=32  KV =  32.00 GB
ctx= 16384  batch= 1  KV =   4.00 GB
ctx= 16384  batch= 8  KV =  32.00 GB
ctx= 16384  batch=32  KV = 128.00 GB
ctx= 32768  batch= 1  KV =   8.00 GB
ctx= 32768  batch= 8  KV =  64.00 GB
ctx= 32768  batch=32  KV = 256.00 GB
ctx=131072  batch= 1  KV =  32.00 GB
ctx=131072  batch= 8  KV = 256.00 GB
ctx=131072  batch=32  KV = 1024.00 GB
```

> **Đọc bảng này và bạn hiểu ngay một chuyện quan trọng cho pipeline của mình:** một trang techpack
> 300 dpi ngốn ~11k token ảnh ([06](06-multimodal-vlm.md)§4). Ba trang trong một request là ~33k
> context. Ở ctx=32768 với batch=8, chỉ riêng KV cache đã **64 GB** — gấp gần ba lần trọng số Q6 (22 GB).
> Ngay cả batch=1 ở context đó cũng đã tốn 8 GB chỉ cho KV.
>
> **Nghĩa là: giảm độ phân giải ảnh không chỉ tiết kiệm tiền token — nó còn cho bạn tăng batch
> size, tức tăng throughput.** Hai lợi ích từ một thay đổi.

### 2.4 Ba đòn bẩy giảm KV cache

| Đòn bẩy | Cách | Đánh đổi |
|---|---|---|
| **GQA/MQA** | Model được thiết kế sẵn (n_kv_head < n_head) | Không chọn được — thuộc về kiến trúc model |
| **KV cache quantization** | vLLM `--kv-cache-dtype fp8` | ~2× tiết kiệm, chất lượng giảm nhẹ |
| **Giảm `max_model_len`** | Đặt đúng nhu cầu thật, không để mặc định | Request dài hơn sẽ bị từ chối |

> `--max-model-len` để mặc định (thường là max của model, có khi 128k) là lỗi cấu hình phổ biến
> nhất — nó buộc vLLM giữ chỗ KV khổng lồ và giảm số request chạy song song. Đặt bằng **p99 context
> thật của bạn + biên an toàn**.

---

## 3. Quantization — đánh đổi cụ thể

```
   Chất lượng
     │  ● fp16 (chuẩn 100%)
     │    ● Q8 / fp8      (~99.5%)
     │      ● Q6_K        (~99%)     ◀── bạn đang ở đây, lựa chọn tốt
     │        ● AWQ 4-bit (~98%)
     │          ● Q4_K_M  (~97%)
     │             ● Q3    (~92%)    ◀── bắt đầu thấy rõ
     │                 ● Q2 (~80%)   ◀── thường không dùng được
     └────────────────────────────────▶ VRAM tiết kiệm
```

| Loại | Cơ chế | Runtime | Ghi chú |
|---|---|---|---|
| **GGUF Q*_K** | Quantize theo block, k-quant | llama.cpp | Linh hoạt, chạy CPU được |
| **AWQ** | Bảo vệ kênh trọng số "quan trọng" | vLLM | Chất lượng tốt ở 4-bit, cần calibration set |
| **GPTQ** | Quantize từng lớp, bù lỗi | vLLM | Tương tự AWQ |
| **fp8** | Native trên Hopper/Ada | vLLM | Nhanh, gần như không mất chất lượng |

> **Điều ít người đo:** quantization ảnh hưởng **không đều** giữa các task. Nó thường giữ tốt
> năng lực sinh văn bản chung nhưng có thể làm hỏng **trích xuất số chính xác** — đúng thứ bạn
> cần. **Đừng tin bảng benchmark chung; đo trên golden set của bạn.** Bạn đã có cơ sở hạ tầng
> `llm_comparison/` để làm việc này.

---

## 4. Đo throughput cho đúng

### 4.1 Bốn chỉ số, đừng lẫn lộn

| Chỉ số | Là gì | Ai quan tâm |
|---|---|---|
| **TTFT** (time to first token) | Độ trễ tới token đầu | UX streaming |
| **TPOT** (time per output token) | Tốc độ sinh sau token đầu | UX streaming |
| **Latency end-to-end** | Tổng thời gian một request | ⭐ Pipeline batch như của bạn |
| **Throughput** (req/s hoặc token/s) | Năng lực hệ thống | ⭐ Chi phí hạ tầng |

> Pipeline của bạn là **batch, không phải chat** — nên TTFT gần như không quan trọng, còn
> **throughput và latency end-to-end** mới là thứ cần tối ưu. Đây là lý do continuous batching
> của vLLM đáng giá với bạn hơn so với một hệ chat.

### 4.2 Đường cong phải vẽ

```
  throughput (req/s)              latency p95 (s)
    │        ╱‾‾‾‾‾‾‾‾‾            │                    ╱
    │      ╱                       │                  ╱
    │    ╱                         │           ____╱
    │  ╱                           │    ______╱
    └──────────────────── batch    └──────────────────── batch
         ↑                                    ↑
     bão hòa ở đây                  latency bắt đầu nổ ở đây
                                    → chọn batch NGAY TRƯỚC điểm này
```

```bash
# vLLM có sẵn công cụ benchmark
python -m vllm.entrypoints.openai.api_server --model ... &
python benchmarks/benchmark_serving.py \
  --backend openai --model <model> \
  --dataset-name random --random-input-len 8000 --random-output-len 500 \
  --request-rate 1,2,4,8,16
```

> **Dùng `--random-input-len` gần với thực tế của bạn** (nhiều nghìn token vì có ảnh), không phải
> mặc định vài trăm. Benchmark với input ngắn sẽ cho con số throughput đẹp và vô dụng.

---

## 5. Vòng đời model — chỗ còn thiếu trong hệ của bạn

```
   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
   │ DATA     │──▶│ TRAIN    │──▶│ EVAL     │──▶│ DEPLOY   │──▶│ MONITOR  │
   │ version  │   │ track    │   │ gate     │   │ canary   │   │ drift    │
   └──────────┘   └──────────┘   └──────────┘   └──────────┘   └────┬─────┘
        ▲                                                            │
        └────────────────────── retrain khi drift ───────────────────┘

   Bạn đang có:  ✅ eval (tốt)  ✅ deploy (tốt)  🟡 monitor (Langfuse)
   Bạn đang thiếu: ❌ data version  ❌ experiment tracking  ❌ drift detection
```

| Thiếu | Công cụ | Vì sao cần khi bắt đầu train model |
|---|---|---|
| **Data versioning** | DVC, hoặc chỉ cần hash + manifest JSON | Không biết model v3 train trên data nào → không tái lập được |
| **Experiment tracking** | MLflow (self-host), W&B | Chạy 40 thí nghiệm rồi quên cái nào tốt là chuyện chắc chắn xảy ra |
| **Model registry** | MLflow Registry, hoặc thư mục + metadata | Rollback nhanh khi model mới tệ |
| **Drift detection** | So phân bố input/output theo tuần | Phát hiện "techpack mùa mới khác hẳn" trước khi khách phàn nàn |

> **Gợi ý thực tế:** bạn **chưa cần** cả bộ MLOps stack. Khi bắt đầu train model đầu tiên, hãy bắt
> đầu bằng **MLflow self-host** — nó lo cả tracking lẫn registry, chạy bằng một docker-compose,
> và hợp với thói quen self-host sẵn có của bạn. Thêm DVC sau, khi dataset bắt đầu có nhiều phiên bản.

### 5.1 Drift — ba loại

| Loại | Là gì | Dấu hiệu trong hệ của bạn |
|---|---|---|
| **Data drift** | Phân bố input đổi | Techpack đổi template, thêm product group mới |
| **Concept drift** | Quan hệ input→output đổi | Rule nghiệp vụ đổi, nhà cung cấp mới |
| **Model drift** | Model đổi dưới chân bạn | Nâng cấp Qwen, đổi quantization |

**Cách phát hiện rẻ nhất, không cần ground truth:** theo dõi **phân bố của chính output**.
Tỉ lệ ô để trống tăng đột ngột, confidence trung bình tụt, số dòng BOM trung bình đổi — đó là
tín hiệu cảnh báo sớm mà không cần ai gán nhãn.

---

## 6. Chi phí hạ tầng — khung tính

```
   Chi phí mỗi techpack  =  (token × giá/token)                  ← nếu dùng API
                         =  (thời gian GPU × giá GPU/giờ)        ← nếu self-host  ◀ bạn

   Self-host:
     chi phí/giờ GPU  ÷  (throughput req/giờ)  =  chi phí/request

   Ví dụ khung (điền số thật của bạn vào):
     GPU ___ $/giờ ÷ ___ techpack/giờ = ___ $/techpack
```

**Điểm hòa vốn self-host vs API** phụ thuộc **tỉ lệ sử dụng**. GPU chạy 10% thời gian thì gần như
chắc chắn API rẻ hơn; chạy >60% thì self-host thường thắng. Bạn đã chọn self-host — nên đòn bẩy
chi phí của bạn là **tăng tỉ lệ sử dụng và throughput**, không phải giảm đơn giá.

> Và nhắc lại §2.3: giảm token ảnh → giảm KV cache → tăng batch → **tăng throughput** → giảm
> chi phí mỗi techpack. Cùng một hành động, ba tầng lợi ích.

---

## 7. Bài tập

| # | Bài | Tiêu chí đạt | Ước lượng |
|---|---|---|---|
| 1 | Tính VRAM lý thuyết cho cấu hình Qwen hiện tại, so với `nvidia-smi` thật | sai số < 15% | 2h |
| 2 | Vẽ đường cong throughput/latency theo batch, chọn batch tối ưu | 1 hình + 1 con số | 4h |
| 3 | Đo `max_model_len` thật cần (p99 context), chỉnh lại config | trước/sau + throughput | 3h |
| 4 | So Q6 vs AWQ 4-bit **trên golden set của bạn**, không phải benchmark chung | bảng accuracy + VRAM + tốc độ | 6h |
| 5 | Dựng MLflow self-host, log lại 1 thí nghiệm từ [03](03-deep-learning-co-ban.md)§9 | UI thấy được run | 3h |
| 6 | Dựng dashboard drift: tỉ lệ ô trống + confidence TB theo tuần | 1 dashboard | 6h |

---

## 8. Ranh giới trung thực

- **Đã chạy thật:** bảng KV cache §2.3 — tính bằng công thức chuẩn.
- **⚠ Là giả định về kiến trúc:** tôi dùng `n_layer=64, n_kv_head=8, d_head=128` như **cấu hình
  điển hình của một model ~27B có GQA**. Tôi **không verify** Qwen3.6-VL 27B có đúng các con số
  này. Lấy từ `config.json` của model bạn đang chạy rồi tính lại — công thức đúng, tham số phải
  là của bạn.
- **Là ước lượng ngành, không phải đo:** bảng byte/tham số §2.2 và thang chất lượng §3. Chúng
  đúng về bậc độ lớn; con số chính xác phụ thuộc tỉ lệ lớp được quantize và cấu hình cụ thể.
- **Chưa đo trên hệ bạn:** mọi thứ về throughput, latency, chi phí. Bạn có
  `JETSON-ORIN-LLAMA-PERF.md` nên có thể đã có một phần — tôi không đọc file đó.
- **Là khuyến nghị:** MLflow (§5). Lựa chọn hợp lý với môi trường self-host của bạn, nhưng không
  phải lựa chọn duy nhất đúng.
