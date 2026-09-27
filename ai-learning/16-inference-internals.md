# 16 · Inference internals — prefill, KV cache, MoE, quantization

> **Câu hỏi file này trả lời:** GPU của bạn đang thật sự tốn vào việc gì, và vì sao đổi sang "model
> nhỏ hơn" đôi khi làm **chậm đi**.
>
> **Thời lượng:** 10–12 giờ. **Yêu cầu trước:** không (file này không cần deep learning).
> **Bổ trợ:** [10 · Tối ưu token](10-toi-uu-token-chi-phi.md) nói về *cắt token*; file này nói về
> *cái gì xảy ra với token sau khi gửi đi*.

---

## 0. Hạ tầng của bạn — số đo thật

Đọc từ [`.env`](../../../motivesidp-ai-service/.env) và
[`dockers/JETSON-ORIN-LLAMA-PERF.md`](../../../motivesidp-ai-service/dockers/JETSON-ORIN-LLAMA-PERF.md)
(đo 31/07/2026):

```
Toàn bộ LLM + VLM của cả 3 team  →  MỘT con Jetson AGX Orin 64GB
                                     Qwen3.6-35B-A3B UD-Q5_K_S (GGUF, llama.cpp)

   Prefill (nạp prompt + ảnh) ......  248 token/s
   Decode  (sinh chữ) .............  28,8 token/s
   RAM đang dùng .................  41,9 / 61,4 GB
```

Quy đổi ra thứ sờ được:

| Thứ | Token | Thời gian |
|---|---|---|
| 1 ảnh trang A4 ở `max_image_size=1600` | ~2.300 | **~9,3 giây** |
| System prompt `unified_bom_extraction_combined.yml` (34 KB) | ~8.500 | **~34 giây** |
| Một style Team A (~140k vào, ~10k ra) | 150k | **~15 phút** |

> Ghi nhớ con số này: **1.000 token prompt ≈ 4 giây GPU.** Mọi quyết định tối ưu đều quy về đây.

---

## 1. Prefill vs decode — hai pha, hai nút thắt khác nhau

Đây là khái niệm quan trọng nhất trong cả file.

| | **Prefill** | **Decode** |
|---|---|---|
| Làm gì | Đọc toàn bộ prompt, tính KV cache | Sinh **từng** token một |
| Song song hoá | **Được** — cả prompt xử lý một lượt | **Không** — token n+1 cần token n |
| Nút thắt | **Compute (FLOPs)** | **Băng thông bộ nhớ** |
| Tốc độ trên máy bạn | 248 tok/s | 28,8 tok/s |

**Vì sao decode chậm hơn ~9 lần?** Vì mỗi token sinh ra phải **đọc lại toàn bộ trọng số model** từ
bộ nhớ. Không phải vì phép tính nặng — mà vì phải chuyển ~2 GB dữ liệu qua bus cho **mỗi chữ**.

**Vì sao điều này quan trọng:** workload của bạn là **prefill-dominated**.

```
Một style: 140.000 token vào / 10.000 token ra   →  tỉ lệ 14:1

   prefill  140.000 / 248   = 565s   ████████████████████████  62%
   decode    10.000 / 28,8  = 347s   ██████████████            38%
```

⇒ Cắt prompt có tác dụng gấp rưỡi cắt output. Và ngược lại: mọi thứ làm prompt dài ra (ảnh độ phân
giải cao, prompt hệ thống lặp, few-shot examples) đắt hơn cảm giác.

### 1.1 Bẫy: token ảnh tỉ lệ với **độ phân giải**, không phải kích thước file

Qwen3-VL cắt ảnh thành patch 28×28 pixel, mỗi patch ≈ 1 token:

```
tokens ≈ (W × H) / 784
```

Ảnh 1600×2000 = 3,2M pixel → **~4.080 token** → 16 giây prefill. Nén JPEG xuống 200KB **không** giúp
gì — vẫn 3,2M pixel. Muốn giảm token phải **giảm độ phân giải**, hoặc crop vùng cần thiết.

Chính codebase cũng dùng xấp xỉ này:
[`document_image.py:21`](../../../motivesidp-ai-service/motives/src/vlm/core/document_image.py) —
`estimate_image_tokens = W*H//768`.

---

## 2. KV cache — vì sao decode không phải tính lại từ đầu

Khi sinh token thứ n, model cần "nhìn lại" toàn bộ token 1..n-1. Nếu tính lại mỗi lần thì độ phức
tạp là O(n²). KV cache lưu sẵn K (key) và V (value) của các token trước → mỗi token mới chỉ tính
phần của chính nó.

**Cái giá:** KV cache tốn RAM, và tốn **tuyến tính theo độ dài context**.

```
KV_bytes ≈ 2 × n_layers × n_kv_heads × head_dim × bytes_per_value × n_tokens
           ↑                                        ↑
       (K và V)                            f16 = 2 bytes, q8_0 ≈ 1 byte
```

### 2.1 Quantize KV cache: khi nào nên, khi nào không

`--cache-type-k q8_0 --cache-type-v q8_0` giảm một nửa RAM của KV cache, nhưng phải **giải nén
on-the-fly** mỗi lần đọc ⇒ **tốn thêm compute**.

> **Quy tắc:** quantize KV khi **thiếu RAM**. Giữ f16 khi **thiếu compute**.

Máy của bạn: 41,9/61,4 GB — còn trống ~19,5 GB, và phân tích trong perf doc §3.1 kết luận nút thắt
là **GPU clock / hiệu quả kernel**, không phải băng thông. ⇒ **Giữ f16 là đúng.** Đây là lý do
compose Jetson không có 2 cờ đó, còn compose máy `.152` thì có.

### 2.2 Prefix caching — đòn bẩy lớn nhất, gần như miễn phí

Nếu 20 request cùng bắt đầu bằng một system prompt 8.500 token giống hệt nhau, KV cache của phần
prefix đó **có thể dùng lại**. Không cache: 20 × 34s = 11 phút chỉ để nạp cùng một đoạn chữ.

Điều kiện: prefix phải **giống nhau đến từng byte**. Nhét timestamp hay số trang vào system prompt
là phá cache.

- llama.cpp: `cache_prompt` (mặc định bật ở bản mới) + `--cache-reuse N`
- vLLM: `--enable-prefix-caching`

**Cách verify** (quan trọng — đừng tin là nó đang bật): xem log llama.cpp
`prompt eval time = ... / N tokens`. Nếu N nhỏ hơn hẳn tổng prompt ⇒ cache đang ăn.

---

## 3. MoE vs dense — vì sao "35B" có thể rẻ hơn "8B"

Đây là chỗ trực giác sai nhiều nhất.

`Qwen3.6-35B-A3B`: **35B tổng tham số**, nhưng **A3B = 3B active** mỗi token. Mixture-of-Experts
chỉ kích hoạt một nhóm nhỏ "expert" cho mỗi token, thay vì chạy toàn bộ mạng.

| Model | Byte đọc/token (decode) | FLOP/token (prefill) |
|---|---|---|
| **35B-A3B @ Q5_K_S** | **2,06 GB** ← số đo thật trong perf doc | ~2 × 3B = 6 GFLOP |
| Dense **8B** @ Q4_K_M | ~4,5 GB → **chậm hơn ~2×** | ~2 × 8B = 16 GFLOP |
| Dense **4B** @ Q4_K_M | ~2,25 GB → xấp xỉ bằng | ~2 × 4B = 8 GFLOP |
| Dense **2B** @ Q4_K_M | ~1,1 GB → nhanh hơn ~2× | ~2 × 2B = 4 GFLOP |

**Công thức nhẩm:**

```
decode_speed  ∝  1 / (active_params × bits_per_weight)
prefill_cost  ∝  active_params
```

⇒ **Về mặt tính toán, model "35B" này đã hành xử như một dense 3B.** Cái nó tốn là **RAM**
(24 GB trọng số), không phải tốc độ. Mà RAM đang là thứ **dư**.

> **Bài học tổng quát:** khi so model, đừng nhìn con số trong tên. Nhìn **active params** và
> **bits/weight**. "70B" MoE với 2B active rẻ hơn "13B" dense.

---

## 4. Quantization — đọc tên cho đúng

| Ký hiệu | Nghĩa | Bit/weight | Ghi chú |
|---|---|---|---|
| `f16` / `bf16` | không nén | 16 | Baseline chất lượng |
| `Q8_0` | 8-bit | 8 | Gần như không mất chất lượng |
| `Q5_K_S` | 5-bit, K-quant, Small | ~5,5 | Đang dùng |
| `Q4_K_M` | 4-bit, K-quant, Medium | ~4,5 | **Điểm ngọt phổ biến nhất** |
| `AWQ` / `GPTQ` W4A16 | 4-bit weight, 16-bit activation | 4 | Cho vLLM, có kernel riêng |
| `FP8` | 8-bit float | 8 | Cần GPU Hopper/Ada trở lên |

Từ Q5_K_S → Q4_K_M: đọc ít hơn ~18% byte mỗi token ⇒ **decode nhanh hơn ~20%**, model nhẹ đi ~5 GB.
Chất lượng giảm nhỏ (bản `UD-Q4_K_XL` của Unsloth thường chênh không đáng kể).

Đây là đòn bẩy perf doc §4.3 đã chỉ ra và **chưa áp dụng**.

---

## 5. Batching — vì sao chạy 7 lần tuần tự là lãng phí

Code hiện tại đếm nút bằng cách gọi VLM **7 lần liên tiếp trên cùng một ảnh**
([`document_analysis.py:616-638`](../../../motivesidp-ai-service/motives/src/vlm/services/document_analysis.py)):

```python
for sample_index in range(num_samples):     # 7 lần
    response_text, ... = run_vlm(..., image_bytes=image_bytes, ...)
```

⇒ ảnh bị **prefill 7 lần** = 7 × ~1.800 token = 12.600 token = ~50 giây.

Cách đúng: một request với `n=7`. Server prefill ảnh **một lần**, rồi sinh 7 nhánh output từ cùng
KV cache đó ⇒ còn ~1.800 token prefill + 7 lần decode ngắn. **Cắt ~85% token của bước này mà không
đổi thuật toán.**

### 5.1 Ba khái niệm batching cần phân biệt

| | Nghĩa |
|---|---|
| **`n=k`** | Một prompt, k kết quả — chia sẻ prefill |
| **Continuous batching** | Server gộp request của nhiều người dùng vào cùng batch GPU |
| **`--parallel N`** (llama.cpp) | N slot chạy song song — **nhưng chia đôi context**: `--ctx-size 65536 --parallel 4` ⇒ mỗi slot chỉ có **16.384 token**, không phải 64k |

Bẫy của dòng cuối: prompt 8.5k + ảnh 2.3k = 10.8k đã gần chạm trần 16k. Vượt trần thì bị cắt **âm
thầm** — không báo lỗi, chỉ ra kết quả sai.

---

## 6. Observability — nhóm 3, gộp vào đây vì liên quan trực tiếp

Bạn không tối ưu được thứ không đo được. Ba khái niệm:

| | Nghĩa |
|---|---|
| **Trace** | Một luồng nghiệp vụ hoàn chỉnh (một lần chạy pipeline) |
| **Span** | Một bước trong trace (một agent, một call) |
| **Generation** | Loại span đặc biệt cho LLM — có prompt, completion, **token in/out**, model |

### 6.1 Nguyên tắc: một gate, bật một lần

Cách **sai** (đang làm trong repo): gắn callback thủ công ở từng chỗ gọi.

```python
llm.invoke(messages, config={"callbacks": callbacks})   # chỗ nào quên là chỗ đó mù
```

Hậu quả thật: audit trong research §10.3 cho thấy **~55–65% token không được đếm** — và đó lại
đúng là những chỗ tốn nhiều nhất.

Cách **đúng**: bật bằng biến môi trường, tự động phủ mọi LangChain Runnable.

```bash
LANGSMITH_TRACING=true          # alias cũ: LANGCHAIN_TRACING_V2
LANGSMITH_API_KEY=...
```

Giới hạn: chỉ thấy cái gì **đi qua LangChain**. Call dùng `httpx` trần hoặc OpenAI SDK trần vẫn mù →
cần `@traceable` hoặc `wrap_openai()`.

### 6.2 Bẫy đã xảy ra trong repo

```python
# techpack_parser.py:529 — đặt TÊN cho một trace KHÔNG TỒN TẠI
response = llm.invoke(messages, config=RunnableConfig(run_name=f"vlm_{key}"))
```

`run_name` chỉ đặt nhãn. Không có `callbacks` thì không có gì được gửi đi đâu cả.

---

## 7. Matryoshka — vì sao cắt vector đôi khi được, đôi khi hỏng

Xếp ở đây vì nó là bài toán *biểu diễn*, liên quan trực tiếp tới bộ nhớ và tốc độ retrieval.

Code hiện tại ([`embedding_service.py:36-37`](../../../motivesidp-ai-service/motives/src/data_platform/embedding_service/embedding_service.py)):

```python
if len(vector) > expected_dim:
    return vector[:expected_dim]        # 1024 → 128
```

**Với `bge-m3` đây là bug.** Một embedding thường **không** có thứ tự ưu tiên giữa các chiều — thông
tin trải đều cả 1024 chiều. Lấy 128 chiều đầu ≈ lấy ngẫu nhiên 12,5% thông tin.

**Matryoshka Representation Learning (MRL)** là kỹ thuật train để điều đó *trở thành* đúng: loss
được tính đồng thời ở nhiều mức cắt (768, 512, 256, 128…), ép model dồn thông tin quan trọng nhất
vào các chiều đầu — như búp bê Nga. Model train kiểu này (`nomic-embed-text-v1.5`,
`text-embedding-3-*`…) **cắt được an toàn**. `bge-m3` thì không.

**Ba cách sửa**, theo công sức tăng dần:

1. Dùng đủ 1024 chiều, reindex. Miễn phí, đúng ngay.
2. Giữ 1024 chiều + bật **quantization vector** của Qdrant (scalar/binary) — giảm RAM mà giữ tín hiệu.
3. Train một lớp `Linear(1024 → 128)` bằng MRL loss trên 46.919 dòng material đã có. **Chạy trên
   CPU, vài phút.** Đây mới là "fine-tune đúng chỗ".

---

## 8. Bài tập

| # | Bài | Tiêu chí đạt | Ước lượng |
|---|---|---|---|
| 1 | Tính tay token của 1 trang techpack ở 3 mức `max_image_size` (1024/1600/2048), quy ra giây prefill | Bảng 3 dòng + công thức | 1h |
| 2 | Kiểm tra prefix caching **có đang bật thật không** trên Jetson — đọc log `prompt eval time = ... / N tokens` | Câu trả lời có/không + bằng chứng log | 1h |
| 3 | Đổi bước đếm nút từ `for` 7 lần → 1 request `n=7`. Đo token trước/sau | Số đo, và kết quả đếm **không đổi** | 4h |
| 4 | Ước tính KV cache cho `--ctx-size 65536` bằng công thức §2, so với RAM `jtop` báo | Sai số < 30% | 2h |
| 5 | Chạy thử `Q4_K_M` song song `Q5_K_S`, đo decode tok/s và accuracy trên 20 style golden | 2 con số cho mỗi bản | 6h |
| 6 | Grep toàn repo tìm mọi chỗ gọi LLM, đánh dấu chỗ nào có `callbacks` chỗ nào không | Bảng audit của riêng bạn | 3h |
| 7 | Train `Linear(1024→128)` bằng MRL loss trên material CSV, so Recall@5 với `vector[:128]` | 2 con số + CI | 6h |

**Bài 2 làm trước** — rẻ nhất, và nếu cache chưa bật thì nó là việc có ROI cao nhất trong cả dự án.
**Bài 6 làm thứ hai** — nó cho biết bạn đang mù ở đâu.

---

## 9. Ranh giới trung thực

- **Đọc từ tài liệu nội bộ, đã verify:** mọi con số ở §0 lấy từ
  `dockers/JETSON-ORIN-LLAMA-PERF.md` (đo 31/07/2026) và `.env`. Con số `2,06 GB/token` ở §3 là số
  **đo thật** trong perf doc, không phải tôi tính.
- **Là tính toán của tôi, chưa đo:** các dòng "dense 8B / 4B / 2B" ở §3 tính bằng công thức
  `params × bits / 8`, **chưa chạy thử**. Thứ hạng thì chắc, con số cụ thể có thể lệch do hiệu quả
  kernel — MoE trên llama.cpp có overhead routing mà dense không có, nên thực tế có thể MoE kém hơn
  lý thuyết. **Phải đo mới biết** (bài tập 5).
- **Là suy luận:** ước tính "cắt ~85% token bước đếm nút nhờ `n=7`" giả định server hỗ trợ `n` và
  chia sẻ prefill đúng cách. Cần verify trên chính llama.cpp build đang chạy.
- **Chưa kiểm chứng:** tôi không chạy được lệnh trên Jetson để xem prefix caching có bật hay không —
  đó là lý do bài tập 2 tồn tại.
- **Không bàn tới:** speculative decoding, tensor parallelism, FlashAttention chi tiết, PagedAttention —
  đều thú vị nhưng chưa phải nút thắt của bạn.
