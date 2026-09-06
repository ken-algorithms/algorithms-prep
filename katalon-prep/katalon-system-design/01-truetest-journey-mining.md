# 01 — Design TrueTest: AI journey mining

> **Bài quan trọng nhất** — đây là sản phẩm lõi của Katalon, nên nhiều khả năng đây là bài họ đưa ra.
> Họ bài: **thu thập & xử lý luồng sự kiện** + **hệ thống có LLM trong vòng lặp**.
> Tách ra từ `katalon-senior-lead-phong-van.md` §5.1. Khung 45 phút: [README](README.md#6-khung-45-phút).

**Đề:** Thiết kế hệ thống tự động **discover → model → generate → maintain** user-journey test case từ hành vi người dùng thật trên web app của khách hàng.

## Clarify (phải hỏi)

- Thu thập từ **production traffic của khách** hay staging? → ảnh hưởng cực lớn tới **PII/compliance**
- Bao nhiêu tenant, bao nhiêu session/tháng/tenant?
- Journey được sinh **real-time** hay batch hằng đêm chấp nhận được?
- Test case sinh ra dùng framework gì (Playwright? Katalon Studio script?)
- Ai review test case sinh ra — người hay auto-merge?

## Estimate (ví dụ để có số mà nói)

```text
2.000 tenant × 500K session/tháng × 40 event/session  ≈ 40B event/tháng
                                                       ≈ 15K event/s (avg), ~50K/s (peak)
Event ~500 B  →  20 TB/tháng raw  →  Parquet + zstd (~5x)  →  ~4 TB/tháng
Giữ raw 30 ngày (hot) + 1 năm (Glacier); giữ journey model vô thời hạn (nhỏ)
```

## Kiến trúc

```text
[Browser SDK / CDP instrumentation]
   ├─ MutationObserver + event listener (click, input, nav, XHR)
   ├─ PII REDACTION NGAY TẠI CLIENT  ⚠️ điểm ăn tiền
   ├─ Sampling theo tenant + batch + gzip (giảm chi phí & tải trang)
   ▼
[Edge Collector]  Quarkus reactive, stateless, sau ALB
   ├─ auth tenant, validate schema, rate limit / quota
   ▼
[Kafka]  key = (tenantId, sessionId)   → đảm bảo THỨ TỰ trong 1 session
   ├──────────────► HOT PATH: Spark Structured Streaming
   │                 ├─ sessionization + watermark cho late event
   │                 └─ rollup → TimescaleDB (live dashboard)
   └──────────────► COLD PATH: S3 raw Parquet (partition tenant/date)
                     ▼
              [Spark Batch — nightly]  ⬅ đây là "large-scale data processing" trong JD
                     ├─ dựng PAGE-TRANSITION GRAPH mỗi tenant
                     ├─ SEQUENCE MINING (PrefixSpan) → journey phổ biến
                     ├─ chấm điểm journey: tần suất × giá trị business × coverage gap
                     └─ dedupe journey gần giống (embedding similarity)
                     ▼
              [Journey Store]  PostgreSQL (Aurora) + pgvector
                     ▼
              [LLM Generation Layer]
                     ├─ RAG: DOM snapshot + test corpus hiện có + convention của tenant
                     ├─ Agent: plan → sinh Playwright/Katalon script → self-critique
                     ├─ VALIDATION GATE: chạy thật script sinh ra trong sandbox
                     │    → chỉ script PASS mới được đề xuất cho user  ⚠️ chống hallucination
                     └─ Eval harness: pass-rate, flakiness, độ giống test người viết
                     ▼
              [MAINTAIN loop]  UI đổi → locator vỡ
                     ├─ so DOM cũ/mới, match element bằng embedding + heuristic
                     └─ đề xuất locator mới kèm confidence; thấp thì escalate cho người
```

## Deep-dive hay bị hỏi

**a) Vì sao key Kafka là `(tenantId, sessionId)` mà không phải `tenantId`?**
Ordering trong Kafka chỉ đảm bảo **trong 1 partition**. Journey mining cần đúng thứ tự event trong 1 session. Key theo `tenantId` thôi thì 1 tenant lớn dồn hết vào 1 partition → **hot partition**. Key `(tenantId, sessionId)` phân tán đều và vẫn giữ thứ tự trong session.
*Đánh đổi:* mất ordering **giữa các session** — nhưng journey mining không cần.

**b) Exactly-once hay at-least-once?**
Chọn **at-least-once + idempotent write**: mỗi event có `eventId` (UUID client-gen), sink dùng `INSERT ... ON CONFLICT DO NOTHING` / Delta merge.
*Đánh đổi:* rẻ và đơn giản hơn Kafka transaction (exactly-once làm throughput giảm, vận hành phức tạp); trả giá là phải thiết kế mọi sink idempotent.

**c) Data skew trong Spark**
Tenant lớn nhất có thể chiếm 40% dữ liệu → 1 task chạy mãi không xong. Fix: **salting** (`tenantId + "#" + random(0..N)`) cho stage aggregate, rồi gộp lại; broadcast join cho dimension table nhỏ; AQE (`spark.sql.adaptive.skewJoin.enabled`).

**d) PII — câu hỏi phân biệt Lead** ⚠️
Đang record **người dùng thật của khách hàng**. Bắt buộc: redaction **ở client trước khi rời browser** (không phải ở server), mask `input[type=password]`, allowlist thay vì denylist cho attribute, không chụp full screenshot mặc định, data residency theo region (EU stay in EU), retention policy + right-to-delete (GDPR), audit log ai xem được session nào.
*Nói câu này = ghi điểm ngay:* "Đây không chỉ là bài big data, nó là bài **compliance**. Nếu redaction sai một lần, khách enterprise sẽ rời đi và đó là rủi ro lớn hơn mọi vấn đề hiệu năng."

**e) Chống hallucination của LLM**
LLM sinh locator/test có thể trông đúng mà chạy sai. Giải: **validation gate bắt buộc** — chạy script trong sandbox headless, chỉ đề xuất cái PASS; chạy 3 lần để lọc flaky; confidence score; luôn có **human-in-the-loop** cho lần đầu. Không bao giờ auto-merge vào test suite của khách.

**f) Cost control cho LLM**
Cache theo hash của (journey + DOM signature); model tiering (model nhỏ để classify/extract, model lớn chỉ để generate); batch; giới hạn token bằng cách chỉ đưa DOM subtree liên quan thay vì cả trang (đây là lúc **Trie/tree pruning** có ích thật).

## Trade-off tổng

| Lựa chọn | Được | Mất |
|---|---|---|
| Batch nightly cho journey mining | Rẻ, dễ debug, dùng được thuật toán global | Journey mới trễ tới 24h |
| Streaming cho dashboard, batch cho mining (lambda-ish) | Cân bằng đúng nhu cầu | 2 code path phải giữ đồng nhất |
| PostgreSQL + pgvector thay vector DB riêng | 1 hệ thống ít vận hành, transaction chung | Kém vector DB chuyên dụng ở quy mô >100M vector |
| Sampling event | Giảm 80% cost | Mất journey rare — mà journey rare đôi khi là bug quan trọng nhất |
| Validation gate bắt buộc | Tin cậy được, không phá test suite khách | Chậm hơn, tốn compute chạy sandbox |
