# 07 · Embedding, Retrieval & RAG

> **Câu hỏi file này trả lời:** hệ retrieval của bạn (Qdrant + bge-m3 + FashionCLIP + TEI reranker)
> đang mất chất lượng ở tầng nào, và làm sao **đo** được điều đó thay vì đoán.
>
> **Thời lượng:** 12–15 giờ. **Yêu cầu trước:** [05](05-nlp.md)§6–7.

---

## 0. Giải phẫu một hệ retrieval — và chỗ nào của bạn chưa được đo

```
   ┌──────────────┐
   │ ① CHUẨN BỊ   │  chunk / trích field / chuẩn hóa text
   │    DỮ LIỆU   │  ⚠ Chưa đo: "một record vật liệu" gồm những field nào ghép lại?
   └──────┬───────┘
          ▼
   ┌──────────────┐
   │ ② EMBED      │  bge-m3 (text) · FashionCLIP (ảnh)
   │              │  ⚠ Chưa đo: separation (xem 06 §2.2)
   └──────┬───────┘
          ▼
   ┌──────────────┐
   │ ③ INDEX      │  Qdrant: HNSW · payload filter · multivector
   │              │  ⚠ ĐÃ BIẾT LÀ VẤN ĐỀ: không có payload index
   └──────┬───────┘                          → exhaustive scan
          ▼
   ┌──────────────┐
   │ ④ SEARCH     │  top-k ANN, có/không filter, có/không hybrid
   │    tầng 1    │  ⚠ Chưa đo: RECALL@k CỦA TẦNG NÀY ← quan trọng nhất
   └──────┬───────┘
          ▼
   ┌──────────────┐
   │ ⑤ RERANK     │  bge-reranker qua TEI, top_k=3
   │    tầng 2    │  ⚠ Chưa đo: rerank có cải thiện MRR không?
   └──────┬───────┘
          ▼
   ┌──────────────┐
   │ ⑥ DÙNG       │  nạp vào LLM (Team A) hoặc map rule (Team C)
   └──────────────┘
```

> **Luật thứ tự tối ưu:** không bao giờ tối ưu tầng ⑤ trước khi đo tầng ④. Nếu tầng 1 chỉ đưa
> được đáp án đúng vào top-50 với 70% số query, thì trần của cả hệ thống là 70% — reranker hoàn
> hảo cũng không vượt qua. **Đây là sai lầm thứ tự phổ biến nhất trong hệ RAG.**

---

## 1. Chuẩn bị dữ liệu — bước bị coi thường nhất

### 1.1 Embed cái gì, không phải embed thế nào

Với một record vật liệu, bạn có nhiều lựa chọn:

```
  A) chỉ tên:            "Polyester Lining 190T"
  B) tên + mô tả:        "Polyester Lining 190T | lót thân, dệt thoi, 100% PES"
  C) tên + mô tả + thuộc tính có cấu trúc:
                         "name: Polyester Lining 190T | type: lining |
                          composition: 100% polyester | weave: taffeta |
                          supplier: XYZ | color: navy"
  D) câu tự nhiên sinh từ template:
                         "Đây là vải lót polyester 190T dệt taffeta, dùng cho thân áo..."
```

**Không có đáp án đúng phổ quát.** Nhưng có quy tắc:

| Quy tắc | Lý do |
|---|---|
| Query và document phải **cùng dạng** | Query là "vải lót" mà doc là bảng field → lệch phân bố |
| Field số/mã ít giá trị trong dense embedding | `190T`, `XYZ-4432` → **dùng sparse/BM25 cho chúng** (§5) |
| Field quá dài pha loãng tín hiệu | Mô tả 500 từ nén thành 1 vector → mất chi tiết |
| Thêm field chỉ đáng nếu **đo thấy** recall tăng | Trực giác thường sai ở đây |

> **Thí nghiệm 1 ngày, ROI rất cao:** dựng 4 collection với 4 cách A/B/C/D, chạy cùng 50 query
> golden, so recall@10. Bạn sẽ có bằng chứng thay vì tranh luận. Kinh nghiệm chung: C thường
> thắng, nhưng **thứ tự field cũng ảnh hưởng** (field quan trọng nên để đầu).

### 1.2 Chunking — chỉ áp dụng khi doc dài

| Chiến lược | Khi nào |
|---|---|
| Không chunk (1 record = 1 vector) | ⭐ Đúng cho vật liệu/BOM row — record đã ngắn |
| Fixed-size + overlap (512 token, overlap 50) | Văn bản dài không cấu trúc |
| Theo cấu trúc (mỗi section/bảng = 1 chunk) | ⭐ Đúng cho tài liệu SRS/rulebook |
| Semantic chunking | Hiếm khi đáng công |

> Trong stack của bạn, phần lớn dữ liệu đã có cấu trúc (row vật liệu, row BOM), nên chunking
> **không phải vấn đề của bạn** — đừng tốn thời gian vào đó.

---

## 2. Không gian vector — ba thứ hay bị hiểu sai

### 2.1 Chọn metric

| Metric | Công thức | Dùng khi |
|---|---|---|
| **Cosine** | góc | Mặc định cho embedding ngữ nghĩa |
| **Dot product** | `a·b` | Khi vector đã normalize → **bằng cosine nhưng nhanh hơn** |
| **Euclidean (L2)** | `‖a−b‖` | Hiếm với embedding hiện đại |

> Vì bge-m3 và FashionCLIP đều trả vector đã normalize, `Distance.DOT` cho **kết quả giống hệt**
> `Distance.COSINE` mà rẻ hơn. Một tối ưu miễn phí — nhưng phải chắc chắn normalize đã xảy ra
> ở **mọi** đường ghi vào, kể cả đường import cũ.

### 2.2 Ngưỡng cosine tuyệt đối là cái bẫy

```
  ❌ SAI                                ✅ ĐÚNG
  if score > 0.80: accept               scores = [s1, s2, s3, ...]   (đã sắp xếp)
                                        if s1 - s2 > margin: accept  ← khoảng cách top1-top2
  Vì sao sai:                           elif s1 > percentile_95_của_phân_bố_nền: accept
  • ngưỡng đúng thay đổi theo           else: đẩy cho người review
    model, theo domain, theo
    độ dài text                         Vì sao đúng:
  • đổi model → mọi ngưỡng              • so tương đối, không phụ thuộc thang tuyệt đối
    phải hiệu chỉnh lại                 • margin giữa top1 và top2 là tín hiệu ĐỘ TỰ TIN
  • không ai nhớ hiệu chỉnh               tốt hơn nhiều so với điểm tuyệt đối
```

> **Liên hệ:** `similar_techpack_scorer` và `late_fusion` của bạn đều đang làm việc với điểm tuyệt
> đối. Chuyển sang tín hiệu margin là một cải tiến rẻ và thường hiệu quả ngay.

### 2.3 Hai không gian vector không so chéo được

`bge-m3` (text, 128-dim cấu hình hiện tại) và `FashionCLIP` (ảnh, 512-dim) là **hai không gian
hoàn toàn độc lập**. Không có phép cosine nào giữa chúng có ý nghĩa.

Muốn "so text với ảnh" thì phải dùng **text tower của chính FashionCLIP** — model đó có text tower
và tài liệu nội bộ của bạn ghi rõ nó **được load nhưng chưa bao giờ gọi**.

> **Đây là một tài sản đang nằm không.** Nếu bạn muốn query "jacket 2 nút notch lapel" bằng chữ
> và tìm sketch, đường đi là text tower của FashionCLIP — **không phải** bge-m3. Thí nghiệm nửa
> ngày: gọi text tower với 10 mô tả attribute, xem cosine với sketch tương ứng có cao hơn sketch
> khác không. Nếu có → mở ra cả một hướng attribute-based search mà không cần train gì.

---

## 3. ANN & HNSW — vì sao search nhanh được

### 3.1 Vấn đề

Brute-force: so query với **mọi** vector → O(N·d). Với 1 triệu vector 512-dim = 512 triệu phép
nhân mỗi query. Quá chậm.

### 3.2 HNSW — đồ thị nhiều tầng

```
   Tầng 2 (thưa)     ●─────────────────────●            bước nhảy dài
                     │                     │
   Tầng 1            ●────●────────●───────●            bước vừa
                     │    │        │       │
   Tầng 0 (đầy đủ)   ●─●─●─●─●─●─●─●─●─●─●─●            bước ngắn, chính xác

   Tìm kiếm: bắt đầu tầng trên cùng, nhảy xa về phía query,
             tụt xuống tầng dưới, tinh chỉnh dần.
             → O(log N) thay vì O(N)
```

### 3.3 Ba tham số Qdrant cần biết

| Tham số | Ý nghĩa | Tăng lên thì |
|---|---|---|
| `m` | số cạnh mỗi node | recall ↑, RAM ↑, build chậm hơn |
| `ef_construct` | độ rộng tìm kiếm lúc **build** | chất lượng index ↑, build chậm hơn |
| `ef` (search) | độ rộng tìm kiếm lúc **query** | recall ↑, latency ↑ — **chỉnh được lúc chạy** |

> **ANN là xấp xỉ.** `recall@k` của ANN so với brute-force thường 95–99%, **không phải 100%**.
> Nếu bạn đang debug "vì sao doc này không ra dù cosine cao", ANN miss là một nghi phạm.
> Cách kiểm: chạy lại với `exact=True` trong Qdrant, so kết quả.

### 3.4 Vấn đề đã biết trong hệ của bạn: thiếu payload index

Tài liệu nội bộ ghi: *"không có payload index trên `branch`/`product_group`/`product_subgroup`/
`view_type`; retrieval sketch là exhaustive scan trong slice đã filter"*.

```
  KHÔNG có payload index                  CÓ payload index
  ┌────────────────────────┐              ┌────────────────────────┐
  │ Qdrant duyệt TẤT CẢ    │              │ Dùng index để lấy      │
  │ điểm, kiểm filter từng │              │ nhanh tập con khớp     │
  │ cái, rồi tính cosine   │              │ filter, rồi ANN trong  │
  │                        │              │ tập đó                 │
  │ → O(N) theo TỔNG số    │              │ → O(log n) theo số     │
  │   style, tuyến tính    │              │   style trong subgroup │
  └────────────────────────┘              └────────────────────────┘
```

```python
from qdrant_client import models
client.create_payload_index(
    collection_name="techpack_sketches",
    field_name="product_subgroup",
    field_schema=models.PayloadSchemaType.KEYWORD,
)
```

Vài dòng, không đổi kiến trúc, không đổi kết quả — chỉ nhanh hơn. **Nhưng đo trước và sau**,
vì với vài nghìn điểm thì exhaustive scan có khi vẫn nhanh hơn ANN, và bạn sẽ tối ưu một thứ
không phải nút thắt.

---

## 4. Filter — và cái bẫy pre/post filtering

```
  PRE-FILTER (Qdrant làm đúng cách)       POST-FILTER (cái bẫy)
  ┌──────────────────────────┐            ┌──────────────────────────┐
  │ 1. Lọc theo subgroup      │           │ 1. ANN top-10 toàn bộ    │
  │ 2. ANN trong tập đã lọc   │           │ 2. Lọc theo subgroup     │
  │ → luôn đủ k kết quả       │           │ → có thể còn 0 kết quả!  │
  └──────────────────────────┘            └──────────────────────────┘
```

Qdrant `query_points(..., query_filter=...)` là **pre-filter** — đúng. Nhưng nếu ở đâu đó trong
code bạn lấy top-k rồi lọc trong Python, đó là post-filter và nó **âm thầm làm rỗng kết quả**
với các subgroup hiếm. Đáng grep một lượt.

---

## 5. Hybrid search — dense + sparse

### 5.1 Vì sao cần cả hai

| | Dense (bge-m3) | Sparse (BM25) |
|---|---|---|
| Giỏi | ngữ nghĩa: "vải lót" ≈ "lining" | khớp chính xác: mã `XYZ-4432`, `190T` |
| Dở | mã, số, từ hiếm, tên riêng | từ đồng nghĩa, diễn đạt khác |

```
  Query: "lining 190T supplier XYZ"
         └─ngữ nghĩa─┘ └──khớp chính xác──┘
              dense giỏi      BM25 giỏi
   → Cần CẢ HAI. Đây là lý do hybrid gần như luôn thắng một mình dense.
```

> Dữ liệu của bạn **đầy mã**: mã vật liệu, mã style, mã nhà cung cấp, mã màu. Đây là trường hợp
> hybrid có lợi rõ nhất. Và tiện lợi: **bge-m3 đã sinh sẵn sparse vector** — bạn đang có tính
> năng đó mà chưa bật.

### 5.2 Gộp kết quả — RRF

```
                          1
   RRF(d) = Σ  ───────────────────────        k thường = 60
            i   k + rank_i(d)
```

```python
def rrf(rank_lists, k=60):
    """rank_lists: list các danh sách doc_id đã sắp xếp, mỗi list từ 1 retriever."""
    scores = {}
    for lst in rank_lists:
        for rank, doc_id in enumerate(lst, start=1):
            scores[doc_id] = scores.get(doc_id, 0.0) + 1.0 / (k + rank)
    return sorted(scores, key=scores.get, reverse=True)

dense  = ["A", "B", "C", "D"]
sparse = ["C", "A", "E", "F"]
print(rrf([dense, sparse]))
```

Chạy thật: `['A', 'C', 'B', 'E', 'D', 'F']` — chú ý `E` (hạng 3 của sparse) vượt `D` (hạng 4 của dense).

**Vì sao RRF tốt hơn cộng điểm có trọng số:** nó chỉ dùng **thứ hạng**, nên không cần chuẩn hóa
điểm giữa hai hệ có thang hoàn toàn khác nhau (cosine 0–1 vs BM25 0–30). Không có hyperparameter
phải tune ngoài `k`, và `k=60` hoạt động tốt trong hầu hết trường hợp.

---

## 6. Rerank — tầng 2

Cơ chế đã nói ở [05](05-nlp.md)§7. Ở đây là phần vận hành:

| Câu hỏi | Cách trả lời bằng số |
|---|---|
| Lấy bao nhiêu candidate vào rerank? | Vẽ recall@k của tầng 1 theo k ∈ {10,20,50,100}. Chọn k nhỏ nhất đạt recall ≥ 95% |
| Rerank có cải thiện thật không? | So MRR trước/sau trên **cùng** tập query, kèm CI |
| Reranker có lệch domain không? | `bge-reranker-base` train trên text web. Text vật liệu ngành may là domain hẹp → **có thể lệch**. Đo, đừng giả định |
| Chi phí có đáng không? | Đo latency p50/p95 thêm vào, so với mức tăng MRR |

> **Trường hợp rerank làm tệ đi là có thật** và hay gặp khi domain lệch mạnh. Nếu MRR sau rerank
> thấp hơn trước, đừng cố tune — hãy fine-tune reranker trên dữ liệu của bạn, hoặc bỏ tầng đó.
> → [08](08-finetuning.md)§7

---

## 7. Đo hệ retrieval — quy trình đầy đủ

### 7.1 Xây tập đánh giá

```
  Cần:  50–200 query  +  với mỗi query, danh sách doc ĐÚNG (qrels)

  Ba nguồn lấy nhãn, xếp theo chi phí:
  ① Từ log production   — user click/chọn gì  (rẻ nhất, có bias vị trí)
  ② Từ golden set sẵn có — 20 case BOM của bạn đã ngầm chứa cặp (query → vật liệu đúng)
  ③ Gán nhãn tay        — đắt nhưng sạch nhất

  → Với bạn, ② là mỏ vàng chưa khai thác: mỗi dòng BOM đúng trong file Excel golden
    CHÍNH LÀ một cặp (query, doc đúng). 20 case × ~50 dòng = ~1000 cặp MIỄN PHÍ.
```

> Đây có lẽ là phát hiện đáng giá nhất trong file này: **bạn đã có ~1000 cặp đánh giá retrieval
> mà chưa dùng**, nằm ngay trong golden BOM Excel. Không cần gán nhãn thêm một dòng nào.

### 7.2 Bảng metric phải báo cáo

| Metric | Tầng | Ý nghĩa |
|---|---|---|
| **recall@10 / @50 / @100** | tầng 1 | Trần của cả hệ thống |
| **MRR** | sau rerank | Đáp án đúng nằm hạng mấy |
| **nDCG@5** | sau rerank | Có tính mức độ liên quan |
| **precision@1** | cuối | Tỉ lệ dùng được nếu chỉ lấy top-1 |
| **latency p50 / p95** | mỗi tầng | Ngân sách hiệu năng |

### 7.3 Bảng chẩn đoán — đọc số để biết sửa gì

| recall@50 tầng 1 | MRR sau rerank | Chẩn đoán | Sửa |
|---|---|---|---|
| Thấp (<70%) | bất kỳ | **Embedding/index yếu** | Fine-tune embedding · hybrid search · xem lại §1.1 |
| Cao (>95%) | Thấp (<0.5) | Retrieval ổn, **xếp hạng dở** | Fine-tune reranker · learning-to-rank |
| Cao | Cao (>0.8) | Retrieval không phải nút thắt | Tìm vấn đề ở mapping/generation |
| Thấp | Cao | Hiếm — có thể tập eval quá dễ | Kiểm lại chất lượng qrels |

> **Đây là bảng bạn nên in ra.** Nó biến "similarity không tốt" từ một câu than phiền thành một
> chẩn đoán có hành động cụ thể.

---

## 8. RAG — và vì sao hệ của bạn chỉ "một phần" RAG

```
  RAG CỔ ĐIỂN                             HỆ CỦA BẠN
  query ─▶ retrieve ─▶ nạp context        Team A: retrieve ─▶ nạp candidate ─▶ LLM CHỌN
        ─▶ LLM SINH câu trả lời                  → RAG dạng hẹp (chọn, không sinh) ✅
                                          Team B: retrieve ─▶ VLM SO SÁNH 2 ảnh
                                                  → không phải RAG ❌
                                          Team C: retrieve ─▶ RULE map
                                                  → không phải RAG ❌
```

**Điều này hoàn toàn ổn** — và thực ra là thiết kế tốt cho bài toán của bạn. "RAG chọn từ candidate"
kiểm soát được hơn "RAG sinh văn bản", vì output bị ràng buộc vào tập candidate có thật. Đừng
chuyển sang RAG sinh chỉ vì tên nghe hiện đại hơn.

### 8.1 Bảy lỗi RAG phổ biến — tự kiểm

| # | Lỗi | Triệu chứng | Kiểm thế nào |
|---|---|---|---|
| 1 | Đo tầng 2 mà không đo tầng 1 | Tune mãi không lên | §7.2 |
| 2 | Chunk sai kích thước | Context thiếu ngữ cảnh hoặc loãng | Đọc bằng mắt 20 chunk |
| 3 | Không hybrid, dữ liệu đầy mã | Query có mã thì trượt | §5 |
| 4 | Ngưỡng cosine tuyệt đối | Đổi model là hỏng | §2.2 |
| 5 | Post-filter thay vì pre-filter | Kết quả rỗng với nhóm hiếm | §4 |
| 6 | Nạp quá nhiều context | Token đắt + **"lost in the middle"** | Đo accuracy theo số candidate |
| 7 | Không có "tôi không biết" | Model bịa khi retrieval trượt | Grounding gate — ✅ bạn đã có |

> **Lỗi 6 đáng nói thêm:** nạp 20 candidate không tốt hơn nạp 5. Nhiều nghiên cứu cho thấy LLM
> chú ý kém với thông tin nằm **ở giữa** context dài. Đây vừa là vấn đề chất lượng vừa là vấn đề
> token — nên nó là một thí nghiệm có ROI kép: đo accuracy theo `n_candidates ∈ {3,5,10,20}`,
> rất có thể 5 vừa rẻ hơn vừa tốt hơn 20.

---

## 9. Bài tập

| # | Bài | Tiêu chí đạt | Ước lượng | Ưu tiên |
|---|---|---|---|---|
| 1 | **Dựng tập eval từ golden BOM Excel** (§7.1②) | ≥500 cặp (query, doc đúng) dạng qrels | 6h | ⭐⭐⭐ |
| 2 | Đo recall@10/@50/@100 tầng 1 + MRR sau rerank, điền vào bảng §7.3 | 1 bảng + 1 chẩn đoán | 4h | ⭐⭐⭐ |
| 3 | Bật sparse của bge-m3, gộp bằng RRF, so recall trước/sau | 2 con số + CI | 6h | ⭐⭐ |
| 4 | Thêm payload index, đo latency p50/p95 trước/sau | bảng 2×2 | 2h | ⭐⭐ |
| 5 | Thử 4 cách build text vật liệu (§1.1 A/B/C/D), so recall@10 | bảng 4 dòng | 8h | ⭐⭐ |
| 6 | Gọi **text tower** của FashionCLIP, thử attribute search (§2.3) | kết luận có/không khả thi | 4h | ⭐⭐ |
| 7 | Đo accuracy theo `n_candidates ∈ {3,5,10,20}` (§8 lỗi 6) | 1 đường cong | 4h | ⭐⭐ |

---

## 10. Ranh giới trung thực

- **Đã chạy thật:** hàm `rrf` §5.2 — output `['A','C','B','D','E','F']` là thật.
- **Đọc từ tài liệu nội bộ của bạn, chưa tự verify trên hệ đang chạy:** thiếu payload index
  (§3.4), `MultiVectorConfig(MAX_SIM)`, text tower FashionCLIP được load nhưng không gọi (§2.3),
  `EMBEDDING_DIM=128`. Nguồn: `docs/ai_technologies_overview.md`, `docs/similar_techpack_embedding_model.md`.
- **Là suy luận của tôi, cần bạn kiểm:** ước lượng "~1000 cặp miễn phí từ golden BOM" (§7.1)
  giả định mỗi case có ~50 dòng BOM và mỗi dòng có một vật liệu đúng xác định được. **Con số
  thật có thể thấp hơn nhiều** nếu nhiều dòng dùng chung vật liệu hoặc không truy ngược được về
  query. Đây là giả định quan trọng nhất trong file — kiểm nó trước khi lập kế hoạch dựa vào nó.
- **Chưa đo:** mọi con số hiệu năng (recall, MRR, latency) của hệ bạn. File này cho bạn **công cụ
  đo**, không phải kết quả đo.
- **Cảnh báo:** khuyến nghị `Distance.DOT` thay `COSINE` (§2.1) chỉ an toàn nếu **mọi** đường ghi
  vào đều normalize. Nếu có một đường import cũ không normalize, đổi metric sẽ làm hỏng kết quả
  một cách âm thầm. Kiểm trước khi đổi.
