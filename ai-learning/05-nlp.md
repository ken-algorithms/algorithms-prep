# 05 · NLP — từ tokenizer tới Transformer

> **Câu hỏi file này trả lời:** token là gì **về mặt cơ học** (và vì sao `len(text)/4` trong
> `vlm_clients.py` là ước lượng, không phải phép đo), attention thật sự tính cái gì, và vì sao
> cross-encoder rerank tốt hơn bi-encoder mà lại không thay thế được nó.
>
> **Thời lượng:** 15–18 giờ. **Yêu cầu trước:** [03](03-deep-learning-co-ban.md).

---

## 0. Bản đồ — và chỗ bạn đang đứng

```
  §1 Tokenizer ──────▶ §2 Embedding ──────▶ §3 Attention ──────▶ §4 Transformer
   GỐC CỦA CHI PHÍ      word2vec→ngữ cảnh    TÍNH TAY được       block đầy đủ
        │                                          │
        │                                          ▼
        │                              §5 BERT vs GPT — encoder vs decoder
        │                                          │
        │                       ┌──────────────────┼──────────────────┐
        ▼                       ▼                  ▼                  ▼
  §9 Tiếng Việt          §6 Bi-encoder       §7 Cross-encoder   §8 Decoding &
   & token đắt gấp đôi     (bge-m3 của bạn)    (reranker của bạn)  structured output
```

---

## 1. Tokenizer — nơi mọi chi phí bắt đầu

### 1.1 Vì sao không dùng từ, không dùng ký tự

| Cách | Vocab | Vấn đề |
|---|---|---|
| Theo **từ** | ~500K+ | Từ mới/lỗi chính tả → `<UNK>`, mất sạch thông tin |
| Theo **ký tự** | ~200 | Chuỗi quá dài (chi phí attention O(n²)), khó học ngữ nghĩa |
| **Subword (BPE)** ⭐ | 32K–256K | Cân bằng: từ phổ biến = 1 token, từ hiếm = nhiều mảnh |

### 1.2 BPE hoạt động thế nào — thuật toán trong 6 dòng

```
  1. Bắt đầu: vocab = tất cả ký tự đơn
  2. Đếm mọi cặp ký tự liền kề trong corpus
  3. Cặp xuất hiện nhiều nhất → gộp thành 1 token mới
  4. Lặp lại cho tới khi đủ vocab_size
  5. Kết quả: một BẢNG MERGE có thứ tự
  6. Lúc tokenize: áp lại bảng merge đó theo đúng thứ tự
```

```
  Ví dụ cực nhỏ, corpus = "low low lower lowest"

  Bước 0: l o w   l o w   l o w e r   l o w e s t
  Bước 1: cặp "l o" xuất hiện 4 lần → gộp "lo"
          lo w   lo w   lo w e r   lo w e s t
  Bước 2: cặp "lo w" xuất hiện 4 lần → gộp "low"
          low   low   low e r   low e s t
  Bước 3: cặp "e s" → "es" ... v.v.
```

### 1.3 Hệ quả trực tiếp lên hóa đơn của bạn

```python
# uv run --with tiktoken python3
import tiktoken
enc = tiktoken.get_encoding("cl100k_base")

for s in ["hello world",
          "Techpack",
          "polyester",
          "SOFT_LIGHT_CAMP1",
          "Áo khoác dạ nam",
          "định mức nguyên phụ liệu"]:
    ids = enc.encode(s)
    print(f"{len(s):3d} ký tự → {len(ids):2d} token  ({len(s)/len(ids):.2f} ký tự/token)  {s!r}")
```

Chạy thật:

```
 11 ký tự →  2 token  (5.50 ký tự/token)  'hello world'
  8 ký tự →  2 token  (4.00 ký tự/token)  'Techpack'
  9 ký tự →  2 token  (4.50 ký tự/token)  'polyester'
 16 ký tự →  6 token  (2.67 ký tự/token)  'SOFT_LIGHT_CAMP1'
 15 ký tự →  7 token  (2.14 ký tự/token)  'Áo khoác dạ nam'
 24 ký tự → 11 token  (2.18 ký tự/token)  'định mức nguyên phụ liệu'
```

**Ba điều đọc ra từ bảng này:**

1. **Giả định `len/4` của bạn chỉ đúng cho tiếng Anh thường** (4.0–5.5 ký tự/token ở 3 dòng đầu).
   Với tiếng Việt có dấu, tỉ lệ thật là **~2.1–2.2 ký tự/token** — nghĩa là công thức `len/4` đang
   **ước lượng thấp hơn thực tế khoảng 2 lần** cho mọi prompt tiếng Việt.
2. **Mã style như `SOFT_LIGHT_CAMP1` tốn 6 token cho 16 ký tự** (2.67 ký tự/token) — đắt gần gấp
   đôi văn bản tiếng Anh. Prompt của bạn có rất nhiều mã như vậy.
3. **Đây là lý do phải đo, không được ước lượng.** Dùng đúng tokenizer của model đang chạy.

> **Sửa ngay trong repo — 20 dòng code:** thay `_estimate_text_tokens` bằng tokenizer thật.
> Mọi endpoint OpenAI-compatible đều trả `usage.prompt_tokens` — dùng **con số thật đó** để
> đối chiếu và hiệu chỉnh. Code của bạn đã đọc `usage` (`_extract_usage_tokens`) rồi, chỉ là
> đang song song duy trì một ước lượng sai. → [10](10-toi-uu-token-chi-phi.md)§2

### 1.4 Ba loại tokenizer, biết để không nhầm

| Thuật toán | Model dùng | Đặc điểm |
|---|---|---|
| **BPE** | GPT, Llama, Qwen | Gộp cặp theo tần suất |
| **WordPiece** | BERT, bge-m3 (họ XLM-R) | Giống BPE, chọn cặp theo likelihood |
| **Unigram / SentencePiece** | T5, XLM-R, mBERT | Bắt đầu vocab lớn rồi tỉa dần. Xử lý được ngôn ngữ không có dấu cách |

> Hai model của bạn dùng **hai tokenizer khác nhau**: Qwen (BPE) và bge-m3 (SentencePiece/XLM-R).
> Không thể dùng chung bộ đếm token.

---

## 2. Embedding — từ word2vec tới ngữ cảnh

### 2.1 Ba thế hệ

```
  ① STATIC (word2vec, GloVe, 2013)
     "bank" luôn ra CÙNG MỘT vector, dù là bờ sông hay ngân hàng
     ┌──────┐
     │"bank"│──▶ [0.2, -0.5, ...]     ← 1 từ = 1 vector, tra bảng
     └──────┘
     ✅ nổi tiếng với: vua − đàn ông + đàn bà ≈ nữ hoàng
     ❌ không xử lý được đa nghĩa

  ② CONTEXTUAL (ELMo→BERT, 2018)
     "tôi ra BANK rút tiền"  ──▶ [0.8, 0.1, ...]
     "tôi ngồi bên BANK sông"──▶ [-0.3, 0.7, ...]    ← khác nhau!
     ✅ cùng từ, khác ngữ cảnh → khác vector

  ③ SENTENCE/PASSAGE EMBEDDING (SBERT→bge-m3, 2019→nay)
     cả câu/đoạn ──▶ 1 vector, TỐI ƯU RIÊNG cho so sánh cosine
     ✅ đây là cái bạn đang dùng cho material search
```

> **Điểm quan trọng dễ sai:** lấy BERT gốc rồi mean-pool để làm sentence embedding cho kết quả
> **tệ**. Cần model được train riêng bằng contrastive learning (SBERT, bge, E5, GTE). bge-m3 của
> bạn đúng loại này. Đây cũng là lý do không thể "tự chế" embedding từ một LLM bất kỳ.

### 2.2 bge-m3 có gì đặc biệt — và liên hệ với cấu hình Qdrant của bạn

bge-m3 xuất **ba loại biểu diễn cùng lúc**:

| Loại | Là gì | Dùng để |
|---|---|---|
| **Dense** | 1 vector cho cả đoạn | Semantic search thường |
| **Sparse (lexical)** | trọng số theo token, như BM25 học được | Khớp từ khóa chính xác |
| **ColBERT (multi-vector)** | 1 vector **cho mỗi token** | Khớp chi tiết, chấm điểm MaxSim |

Repo của bạn cấu hình `MultiVectorConfig(comparator=MAX_SIM)` cho `techpack_history` và
`bom_history` → **đang dùng nhánh ColBERT**. Cơ chế MaxSim:

```
   query tokens:  q1  q2  q3
   doc tokens:    d1  d2  d3  d4  d5

   score = Σ  max  sim(qᵢ, dⱼ)
          i    j

   Với mỗi token query, tìm token doc GIỐNG NHẤT, lấy điểm đó, rồi cộng lại.
```

| | Dense đơn vector | ColBERT MaxSim |
|---|---|---|
| Bộ nhớ | 1 vector/doc | **N vector/doc** (N = số token) — đắt hơn hàng chục lần |
| Chất lượng | tốt | tốt hơn, nhất là khi cần khớp chi tiết |
| Tốc độ | nhanh | chậm hơn |
| Khi nào đáng | mặc định | khi doc dài và cần khớp cụm từ cụ thể |

> **Câu hỏi bạn nên tự trả lời:** `master_materials` dùng vector đơn, `techpack_history` dùng
> multivector. Quyết định đó có được **đo** không, hay là mặc định copy từ đâu đó? Nếu chưa đo,
> đây là một thí nghiệm 1 ngày: chạy cả hai trên cùng tập query, so recall@5 và so dung lượng.

---

## 3. Attention — tính tay được

### 3.1 Công thức

```
                       ⎛  Q Kᵀ  ⎞
   Attention(Q,K,V) = σ⎜ ────── ⎟ V           σ = softmax theo hàng
                       ⎝   √d   ⎠
```

Ba vai trò, dùng phép ẩn dụ tra cứu:

| | Tên | Vai |
|---|---|---|
| **Q** | Query | "Tôi đang tìm cái gì" |
| **K** | Key | "Tôi có cái gì để chào" |
| **V** | Value | "Nội dung thật tôi đưa cho bạn" |

### 3.2 Ví dụ tính tay — seq_len=3, d=2

```python
import numpy as np
Q = np.array([[1.0, 0.0], [0.0, 1.0], [1.0, 1.0]])   # 3 token, d=2
K = Q.copy()
V = np.array([[10.0, 0.0], [0.0, 10.0], [5.0, 5.0]])
d = Q.shape[1]

scores = Q @ K.T / np.sqrt(d)
w = np.exp(scores - scores.max(axis=1, keepdims=True))
w = w / w.sum(axis=1, keepdims=True)
out = w @ V

print("scores =\n", np.round(scores, 3))
print("attention weights =\n", np.round(w, 3))
print("output =\n", np.round(out, 3))
```

Chạy thật:

```
scores =
 [[0.707 0.    0.707]
 [0.    0.707 0.707]
 [0.707 0.707 1.414]]
attention weights =
 [[0.401 0.198 0.401]  ← token 1 chú ý ngang nhau tới token 1 và token 3
 [0.198 0.401 0.401]
 [0.248 0.248 0.503]]  ← token 3 chú ý mạnh nhất tới chính nó
output =
 [[6.017 3.983]
 [3.983 6.017]
 [5.    5.   ]]
```

### 3.3 Vì sao chia √d — có lý do toán học cụ thể

Nếu `Q`, `K` có phần tử ~N(0,1) độc lập, thì `q·k` có **phương sai bằng d**. Với d=768, độ lệch
chuẩn của score đo được là ~27.7 (≈√768). Đưa số cỡ đó vào softmax → phân bố gần như one-hot → **gradient ≈ 0**,
model không học được.

```python
rng = np.random.default_rng(0)
for d in [4, 64, 768]:
    q, k = rng.normal(size=(1000, d)), rng.normal(size=(1000, d))
    s = (q * k).sum(1)
    print(f"d={d:4d}  std(q·k) = {s.std():6.2f}   std sau khi chia √d = {(s/np.sqrt(d)).std():.2f}")
```

```
d=   4  std(q·k) =   1.99   std sau khi chia √d = 1.00
d=  64  std(q·k) =   8.05   std sau khi chia √d = 1.01
d= 768  std(q·k) =  27.71   std sau khi chia √d = 1.00
```

Chia `√d` giữ phương sai ≈ 1 bất kể độ sâu. Một dòng chia, nhưng thiếu nó thì Transformer không
train được.

### 3.4 Multi-head — vì sao nhiều đầu

Một head học **một kiểu quan hệ**. 12 head → 12 kiểu quan hệ song song (cú pháp, đồng tham chiếu,
vị trí...). Chi phí gần như không đổi vì `d` được chia đều: `d_head = d_model / n_heads`.

### 3.5 Ba biến thể attention phải phân biệt

| Loại | Ai nhìn được ai | Dùng ở |
|---|---|---|
| **Bi-directional self-attention** | mọi token nhìn mọi token | BERT, bge-m3, **encoder** |
| **Causal (masked) self-attention** | token i chỉ nhìn ≤ i | GPT, Qwen, **decoder** — bắt buộc để sinh text |
| **Cross-attention** | query từ chuỗi A, key/value từ chuỗi B | VLM: text query nhìn vào image token |

---

## 4. Transformer block — lắp lại đầy đủ

```
            ┌─────────────────────────────────┐
   input ──▶│         LayerNorm               │
            └────────────┬────────────────────┘
                         ▼
            ┌─────────────────────────────────┐
            │   Multi-Head Self-Attention     │
            └────────────┬────────────────────┘
                         ▼
              ⊕ ◀────────── residual (từ input)
              │
              ▼
            ┌─────────────────────────────────┐
            │         LayerNorm               │
            └────────────┬────────────────────┘
                         ▼
            ┌─────────────────────────────────┐
            │  FFN: Linear(d→4d) → GELU →     │   ← chứa ~2/3 số tham số của block
            │       Linear(4d→d)              │
            └────────────┬────────────────────┘
                         ▼
              ⊕ ◀────────── residual
              │
              ▼  output
```

Xếp chồng 12 (BERT-base) / 32 (7B) / 64 (70B) block như vậy. **Đó là toàn bộ kiến trúc.**

### 4.1 Positional encoding — vì sao cần

Self-attention **không biết thứ tự**: hoán vị token thì output cũng chỉ hoán vị tương ứng.
Phải bơm thông tin vị trí vào.

| Cách | Model | Ưu |
|---|---|---|
| Sinusoidal | Transformer gốc | Không tham số |
| Learned absolute | BERT | Đơn giản |
| **RoPE** (rotary) ⭐ | Llama, Qwen, hầu hết LLM hiện đại | Mã hóa **vị trí tương đối**, ngoại suy được context dài hơn lúc train |

> **Liên hệ thực tế:** khi bạn thấy model "chất lượng tụt khi context dài", một phần lý do nằm ở
> đây — RoPE ngoại suy được nhưng không miễn phí. Các kỹ thuật mở rộng context (NTK scaling,
> YaRN) đều là can thiệp vào RoPE.

---

## 5. BERT vs GPT — encoder vs decoder

```
  ENCODER (BERT, bge-m3)                DECODER (GPT, Qwen)
  ┌────────────────────────┐            ┌────────────────────────┐
  │ Nhìn được HAI CHIỀU    │            │ Chỉ nhìn về TRÁI       │
  │ [CLS] tôi [MASK] cơm   │            │ tôi ăn ___             │
  │        ↑ đoán "ăn" từ  │            │        ↑ đoán token kế  │
  │          cả 2 phía     │            │          tiếp           │
  └────────────────────────┘            └────────────────────────┘
  Train: Masked LM                      Train: Next-token prediction
  Giỏi: HIỂU (classify, embed, NER)     Giỏi: SINH (generate, chat)
  KHÔNG sinh text được                  Embed được nhưng kém hơn encoder
```

| | Encoder | Decoder |
|---|---|---|
| Trong stack của bạn | `bge-m3`, `bge-reranker` | `Qwen3-VL` |
| Kích thước điển hình | 100M–500M | 7B–700B |
| Chi phí inference | rẻ, chạy CPU được | đắt, cần GPU |
| Output | vector / nhãn | chuỗi text |

> **Bài học kiến trúc:** rất nhiều việc bạn đang giao cho decoder 27B thật ra là việc của encoder
> 300M — phân loại FRONT/BACK, chấm điểm liên quan, phát hiện trang có BOM. Encoder rẻ hơn
> **hai bậc độ lớn**. Đây là cần gạt lớn thứ hai sau token ảnh. → [10](10-toi-uu-token-chi-phi.md)§6

---

## 6. Bi-encoder — cách retrieval của bạn hoạt động

```
   query ──▶ [Encoder] ──▶ vector_q ─┐
                                     ├──▶ cosine ──▶ điểm
   doc   ──▶ [Encoder] ──▶ vector_d ─┘
             (tính TRƯỚC, lưu vào Qdrant)
```

| Ưu | Nhược |
|---|---|
| Doc embed **một lần**, lưu sẵn | query và doc **không bao giờ nhìn thấy nhau** |
| Search hàng triệu doc trong ms | mọi tương tác bị nén vào 1 phép dot product |
| Scale tốt | chất lượng kém hơn cross-encoder |

---

## 7. Cross-encoder rerank — vì sao tốt hơn mà không thay được bi-encoder

```
   [query] [SEP] [doc]  ──▶ [Encoder] ──▶ điểm liên quan (1 số)
       └────┬────┘
   query và doc ở CHUNG một chuỗi
   → attention cho phép từng token query "nhìn thẳng" từng token doc
```

```
  ĐỘ CHÍNH XÁC        │ ■■■■■■■■■■  cross-encoder
                      │ ■■■■■■      bi-encoder
                      └──────────────────────────

  CHI PHÍ             │ ■■■■■■■■■■  cross-encoder: O(N) forward pass, KHÔNG precompute được
                      │ ■           bi-encoder: 1 forward + ANN search
                      └──────────────────────────
```

**Vì vậy kiến trúc chuẩn là hai tầng** — đúng cái bạn đã làm với `RerankService` + TEI reranker:

```
  1 triệu doc ──[bi-encoder + ANN]──▶ top 100 ──[cross-encoder]──▶ top 3
                rẻ, recall cao                    đắt, precision cao
                ~10ms                             ~100ms cho 100 doc
```

### 7.1 Ba tham số cần đo, không được đoán

| Tham số | Câu hỏi | Cách đo |
|---|---|---|
| `top_k` trước rerank | Lấy 20, 50 hay 100 candidate? | Vẽ **recall@k của tầng 1** theo k. Chọn k nhỏ nhất mà recall ≥ 95% |
| `top_k` sau rerank | Trả 3 hay 5? | Theo nhu cầu UI + đo precision@k |
| Có nên rerank không | Rerank có thật sự cải thiện? | So MRR trước/sau. **Có trường hợp rerank làm tệ đi** khi domain lệch |

> Code hiện tại `rerank(query, candidates, top_k=3)` — con số 3 và số candidate đầu vào **đến từ
> đâu**? Nếu tầng 1 chỉ đạt recall@50 = 70%, thì cross-encoder giỏi mấy cũng không cứu được 30%
> đã mất. **Luôn đo recall của tầng 1 trước khi tối ưu tầng 2.** Đây là lỗi thứ tự phổ biến nhất
> trong hệ thống RAG. → [07](07-embedding-retrieval-rag.md)§7

---

## 8. Decoding & structured output

### 8.1 Các chiến lược sinh token

| Chiến lược | Cách | Khi nào |
|---|---|---|
| **Greedy** (`T=0`) | luôn chọn token xác suất cao nhất | ⭐ Extraction, JSON, mọi thứ cần tái lập |
| **Temperature sampling** | lấy mẫu từ phân bố đã scale | Sáng tạo |
| **Top-k** | chỉ lấy mẫu trong k token đầu | Chặn đuôi rác |
| **Top-p (nucleus)** | lấy mẫu trong tập nhỏ nhất có tổng xác suất ≥ p | Mặc định phổ biến |
| **Beam search** | giữ b nhánh tốt nhất | Dịch máy. **Hiếm khi tốt cho chat** |

> Code của bạn: `temperature` từ config, `top_p=1`. Với extraction nên để `temperature=0` thẳng.
> `temperature=0.1` không cho bạn thêm gì so với 0, chỉ thêm một nguồn không tái lập.

### 8.2 Constrained decoding — kỹ thuật bạn nên dùng mà chưa dùng

Thay vì cầu xin model ("hãy trả về JSON hợp lệ"), **ép ở tầng sampler**: ở mỗi bước, chỉ cho phép
những token khiến chuỗi vẫn hợp grammar.

```
  Đang ở:  {"item_code": "
  Grammar cho phép tiếp theo: bất kỳ ký tự chuỗi nào, hoặc "
  → Mọi token khác bị đặt logit = −∞ TRƯỚC softmax
  → JSON hợp lệ 100%, không cần retry
```

| Cách | Đảm bảo | Hỗ trợ ở đâu |
|---|---|---|
| Prompt "trả JSON" | ❌ không | mọi nơi |
| `response_format={"type":"json_object"}` ✅ bạn đang dùng | JSON hợp lệ, **không** đảm bảo đúng schema | OpenAI-compatible |
| **JSON Schema / grammar (GBNF, outlines, xgrammar)** ⭐ | đúng schema luôn | vLLM (`guided_json`), llama.cpp (`grammar`) |

> **Giá trị cụ thể với bạn:** bạn đang tự host qua vLLM/llama.cpp — **cả hai đều hỗ trợ grammar**.
> Chuyển sang `guided_json` xóa sạch một lớp retry + parse-fallback trong `output_parser.py`,
> và **tiết kiệm token thật** (không còn retry). Đây là thay đổi vài chục dòng, ROI rất cao.

### 8.3 Cắt output token bằng thiết kế schema

Đây là cần gạt ít người nghĩ tới:

```json
// Tốn nhiều token — tên field dài, lặp lại ở MỌI dòng
{"material_code": "...", "material_description": "...", "unit_of_measure": "...",
 "consumption_quantity": ..., "place_to_use_description": "..."}

// Tốn ít hơn ~40% — tên ngắn, có bảng ánh xạ ở phía code
{"mc": "...", "md": "...", "uom": "...", "qty": ..., "ptu": "..."}

// Ít nhất — mảng theo vị trí, kèm header một lần
{"cols": ["mc","md","uom","qty","ptu"],
 "rows": [["...","...","...",1.2,"..."], ["...","...","...",0.8,"..."]]}
```

Với BOM 50 dòng, dạng thứ ba tiết kiệm hàng nghìn output token mỗi lần gọi.
**Đánh đổi:** prompt khó đọc hơn, cần lớp map ở code, và model nhỏ đôi khi nhầm thứ tự cột —
phải đo lại accuracy sau khi đổi, không được giả định. → [10](10-toi-uu-token-chi-phi.md)§7

---

## 9. Tiếng Việt — ba điều cần biết

1. **Token đắt hơn ~2×.** Xem §1.3. Prompt tiếng Việt tốn gấp đôi prompt tiếng Anh cùng nội dung.
   → Với system prompt dùng lại hàng nghìn lần, **viết bằng tiếng Anh** rẻ hơn đáng kể. Đánh đổi:
   khó review bởi BA người Việt. Cân nhắc: prompt tiếng Anh + comment tiếng Việt trong file YAML.
2. **Tiếng Việt có dấu cách giữa âm tiết, không phải giữa từ.** "học sinh" = 2 âm tiết, 1 từ.
   Với model hiện đại (Qwen, bge-m3 đều đa ngữ) điều này được xử lý ngầm, **không cần** word
   segmentation như thời VnCoreNLP.
3. **Mã nội bộ (style code, mã vật liệu) là kẻ ăn token.** `SOFT_LIGHT_CAMP1` = 6 token. Nếu
   prompt của bạn liệt kê 200 mã vật liệu ứng viên, chỉ riêng danh sách đó đã hơn 1200 token.
   → Cân nhắc đánh số lại candidate (`1`, `2`, `3`) trong prompt và map ngược ở code.
   Bạn đã làm điều tương tự với `candidate_id` cache trong `material_resolver.py` — mở rộng ý đó.

---

## 10. Bài tập

| # | Bài | Tiêu chí đạt | Ước lượng |
|---|---|---|---|
| 1 | Cài BPE train + encode từ đầu trên 1 corpus nhỏ | encode/decode khứ hồi đúng | 4h |
| 2 | Đo tỉ lệ ký tự/token thật cho **prompt thật của bạn** bằng tokenizer Qwen | 1 bảng cho 12 thư mục prompt | 2h |
| 3 | Cài multi-head attention bằng numpy, so với `nn.MultiheadAttention` | khớp 1e-5 | 4h |
| 4 | So dense vs ColBERT multivector trên `techpack_history`: recall@5 và dung lượng | bảng 2×2 | 4h |
| 5 | Chuyển 1 agent sang `guided_json` của vLLM, đo tỉ lệ retry trước/sau | 2 con số | 3h |
| 6 | Thử schema nén (§8.3) cho Agent 10, đo token + accuracy trước/sau | bảng so sánh | 4h |

---

## 11. Ranh giới trung thực

- **Đã chạy thật:** §1.3 (tiktoken), §3.2 (attention), §3.3 (variance) — output là thật.
  ⚠ §1.3 dùng `cl100k_base` (tokenizer OpenAI), **không phải** tokenizer của Qwen3-VL. Tỉ lệ
  ký tự/token của Qwen sẽ khác — bài tập 2 tồn tại chính vì lý do đó. Kết luận định tính
  ("tiếng Việt đắt gấp đôi", "mã in hoa có gạch dưới đắt") đúng với cả hai họ tokenizer, nhưng
  **con số cụ thể thì phải đo lại bằng tokenizer thật của bạn**.
- **Đọc từ code, đã verify:** `MultiVectorConfig(MAX_SIM)`, `response_format={"type":"json_object"}`,
  `rerank(..., top_k=3)`, `temperature` từ `ModelConfig`, `_estimate_text_tokens`.
- **Là suy luận, chưa đo:** ước lượng "tiết kiệm 40%" ở §8.3 và "1600 token cho 200 mã" ở §9 —
  tính theo tỉ lệ token quan sát được, chưa chạy trên prompt thật của bạn.
- **Là khuyến nghị, chưa kiểm chứng trên hệ của bạn:** §8.2 (`guided_json`). Tôi biết vLLM và
  llama.cpp có tính năng này, nhưng **không biết phiên bản bạn đang deploy có bật không** —
  kiểm bằng cách gọi thử một request.
- **Không bàn tới:** RNN/LSTM/GRU chi tiết, seq2seq cổ điển, parsing cú pháp, topic modeling.
  Chúng là lịch sử quan trọng nhưng không thay đổi quyết định kỹ thuật nào của bạn hôm nay.
