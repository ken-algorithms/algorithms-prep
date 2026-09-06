# Bài phỏng vấn thật — Katalon, Principal Software Engineer: thống kê 10k request/phút

> **Nguồn:** buổi phỏng vấn thật của bạn (Katalon, level Principal). Không phải bài giả định.
> **Trạng thái:** câu trả lời tại chỗ chưa được chấp nhận — tài liệu này mổ xẻ *vì sao*, rồi dựng
> lại lời giải đúng từ tầng khái niệm xuống tầng dòng code, kèm demo chạy được.
>
> Liên quan: [03 — Real-time Analytics Dashboard](03-realtime-analytics-dashboard.md) — cùng **họ
> bài toán**, phát biểu ở mức tổng quát; file này là bản đào sâu của đúng câu đã bị hỏi.
> [05 — Hệ thống thật all-in-one trên AWS](05-he-thong-that-allinone-aws.md) — hạ tầng thật của bạn.
> [README của folder](README.md) — bản đồ 6 họ bài system design.

---

## Mục lục

- [0. Đề bài và những gì đề bài KHÔNG nói](#0-đề-bài-và-những-gì-đề-bài-không-nói)
- [1. Vì sao câu trả lời cũ bị bác — 4 lỗ hổng](#1-vì-sao-câu-trả-lời-cũ-bị-bác--4-lỗ-hổng)
- [2. Trước khi vẽ: 6 câu hỏi phải hỏi interviewer](#2-trước-khi-vẽ-6-câu-hỏi-phải-hỏi-interviewer)
- [3. Ước lượng tải — con số quyết định kiến trúc](#3-ước-lượng-tải--con-số-quyết-định-kiến-trúc)
- [4. Design tầng 1 — bản tối giản đúng](#4-design-tầng-1--bản-tối-giản-đúng)
- [5. Design tầng 2 — bản production](#5-design-tầng-2--bản-production)
- [6. Chi tiết từng phần](#6-chi-tiết-từng-phần)
  - [6.1 Data model](#61-data-model)
  - [6.2 Time bucket và rollup nhiều tầng](#62-time-bucket-và-rollup-nhiều-tầng)
  - [6.3 Thuật toán phân rã khoảng thời gian bất kỳ](#63-thuật-toán-phân-rã-khoảng-thời-gian-bất-kỳ)
  - [6.4 Idempotency — vì sao counter KHÔNG phải nguồn sự thật](#64-idempotency--vì-sao-counter-không-phải-nguồn-sự-thật)
  - [6.5 Late-arriving data và watermark](#65-late-arriving-data-và-watermark)
  - [6.6 Phân loại "fake key"](#66-phân-loại-fake-key)
  - [6.7 Cardinality — cái bẫy chính của bài này](#67-cardinality--cái-bẫy-chính-của-bài-này)
  - [6.8 Tỉ lệ 50% — đừng bao giờ lưu tỉ lệ](#68-tỉ-lệ-50--đừng-bao-giờ-lưu-tỉ-lệ)
  - [6.9 Cache và thundering herd](#69-cache-và-thundering-herd)
  - [6.10 Backpressure khi burst](#610-backpressure-khi-burst)
  - [6.11 Retention và chi phí lưu trữ](#611-retention-và-chi-phí-lưu-trữ)
  - [6.12 Observability — 6 metric phải có](#612-observability--6-metric-phải-có)
- [7. Chọn tech stack — 3 phương án và ngưỡng chuyển](#7-chọn-tech-stack--3-phương-án-và-ngưỡng-chuyển)
- [8. Demo chạy được — từng bước](#8-demo-chạy-được--từng-bước)
- [9. Nếu demo bằng Java/Spring (stack của Katalon)](#9-nếu-demo-bằng-javaspring-stack-của-katalon)
- [10. 16 câu follow-up interviewer hay hỏi](#10-16-câu-follow-up-interviewer-hay-hỏi)
- [11. Kịch bản trình bày 3 phút](#11-kịch-bản-trình-bày-3-phút)
- [12. Ranh giới trung thực](#12-ranh-giới-trung-thực)

---

## 0. Đề bài và những gì đề bài KHÔNG nói

**Đề bài (diễn đạt lại cho gọn):**

> Có khoảng **10.000 request/phút**. Mỗi request mang về một **list key-value**, value là
> `true`/`false`, và key có thể là **key không hợp lệ ("fake key")**. Tỉ lệ thành công khoảng 50%.
> Thiết kế hệ thống sao cho **bất kỳ lúc nào** cũng hỏi được: có bao nhiêu `true`, bao nhiêu
> `false`, bao nhiêu `fake key` — **trong 24 giờ qua**, hoặc **theo một khoảng thời gian bất kỳ**.

**Ba từ khoá quyết định toàn bộ design — và đều dễ đọc lướt qua:**

| Từ khoá trong đề | Ràng buộc thật nó đặt ra |
|---|---|
| *"bất kỳ lúc nào"* | Read path phải **rẻ và ổn định**, không được là một job chạy nền rồi mới có kết quả. Không được scan raw. |
| *"hoặc truy vấn theo time nhất định"* | Phải trả lời được **range query tuỳ ý** `[t1, t2)` — không chỉ "tổng từ đầu tới giờ". **Đây là chỗ câu trả lời cũ vỡ.** |
| *"một list key-value"* | Đơn vị đếm **không phải** request. 1 request = N event. Không hỏi N là bao nhiêu thì không ước lượng được tải. |

**Điều đề bài không nói — và bạn phải chủ động hỏi:** xem [§2](#2-trước-khi-vẽ-6-câu-hỏi-phải-hỏi-interviewer).

> **Đây là bài "counting at scale"**, một họ bài kinh điển (Twitter impression counts, ad
> click counting, rate-limit analytics). Không phải bài lưu trữ. Sự khác biệt: bài lưu trữ tối ưu
> **ghi**; bài đếm tối ưu **đọc theo khoảng thời gian**, và chấp nhận ghi nhiều lần cùng một dữ liệu
> ở nhiều độ phân giải khác nhau.

---

## 1. Vì sao câu trả lời cũ bị bác — 4 lỗ hổng

**Câu trả lời tại chỗ:** *"Nhận request → chunk insert. Lúc insert thì thống kê luôn, có cơ chế
locking khi insert để tổng kết ngay tại thời điểm đó trong vòng 24h. Khi cần hỏi thì lấy last record
ra là biết."*

Ý tưởng nền (đếm sẵn thay vì đếm lúc hỏi) **đúng**. Bốn chỗ hỏng nằm ở cách hiện thực.

### Lỗ hổng #1 — một counter lũy kế không trả lời được range query (chí mạng)

Nếu bạn giữ **một** bản ghi thống kê và cập nhật nó mỗi lần insert, thì "last record" chỉ cho biết
**trạng thái tại thời điểm hiện tại**. Thông tin thời gian đã bị *nướng* vào một con số duy nhất.

```text
Cái bạn có:      total_true = 7.204.115   ← tổng lũy kế, không tách được
Cái đề bài hỏi:  từ 03:00 đến 05:00 hôm qua có bao nhiêu true?   ← KHÔNG trả lời được
```

Có một biến thể *tưởng là cứu được*: lưu **snapshot theo thời gian** rồi lấy hiệu hai snapshot
(`counter(t2) − counter(t1)`). Nhưng cách này chỉ đúng khi counter **đơn điệu tăng và không bao giờ
reset** — mà bạn lại nói *"trong vòng 24h"*, nghĩa là có reset/cửa sổ trượt. Reset một phát là mọi
phép trừ qua ranh giới đó sai. Và nếu không reset thì "24h qua" lại không tính được.

→ **Cùng một cấu trúc không thể vừa là cửa sổ trượt 24h vừa là chuỗi lũy kế.** Đây gần như chắc chắn
là chỗ interviewer thấy hụt.

### Lỗ hổng #2 — lock ở write path là anti-pattern về scale

"Có cơ chế locking khi insert thì tổng kết luôn" nghĩa là mọi request phải **giành cùng một lock** để
cộng vào cùng một dòng.

- Toàn bộ write path bị **serialize** — không scale ngang được. Thêm instance app cũng vô ích vì
  nghẽn nằm ở dòng dữ liệu bị tranh chấp, không nằm ở CPU app.
- Trong Postgres, đây là **row-level lock contention** trên một hot row: tất cả transaction xếp
  hàng, và ở tải cao sẽ thấy `LWLock` / `tuple` wait event cùng lock queue dài.
- Đáng nói hơn: bạn đang bắt **write** (cần nhanh, cần chịu burst) **chờ** **compute** (aggregation)
  xong mới trả response. Hai việc này khác bản chất và nên tách rời.

> Nếu bị hỏi *"vậy dùng `UPDATE ... SET c = c + 1` có atomic không"* — **có**, atomic không phải vấn
> đề. Vấn đề là **contention**: atomic đảm bảo đúng, không đảm bảo nhanh.

### Lỗ hổng #3 — trộn nguồn sự thật với cache

Khi counter là thứ **duy nhất** được cập nhật, nó vừa là kết quả vừa là nguồn sự thật. Hệ quả:

- Retry của client (at-least-once) → **đếm trùng**, và không có cách nào phát hiện.
- Aggregation logic sai/ đổi định nghĩa → **không backfill lại được**, vì dữ liệu gốc đã mất chi tiết.
- Không có cách **đối soát** (reconcile) để chứng minh con số đúng.

### Lỗ hổng #4 — bucket theo thời điểm insert, không phải thời điểm sự kiện

Client gom (batch) rồi gửi. Một request đến lúc `10:00:03` có thể chứa event xảy ra lúc `09:58`.
Đếm theo lúc insert → số liệu bị lệch khung giờ và **không bao giờ sửa được**.

---

### Bảng đối chiếu gọn

| Yêu cầu của đề | Giải pháp cũ | Vì sao hụt |
|---|---|---|
| Hỏi bất kỳ lúc nào | ✅ Đọc 1 record — nhanh | Nhưng chỉ đúng cho **1 loại câu hỏi** |
| 24 giờ qua | 🟡 Có, nếu reset đúng | Reset làm hỏng mọi truy vấn khác |
| **Khoảng thời gian bất kỳ** | ❌ Không | **Lỗ hổng #1** |
| 10k req/phút và còn tăng | ❌ Lock serialize write | **Lỗ hổng #2** |
| Sửa sai / backfill / đối soát | ❌ Không có raw | **Lỗ hổng #3** |
| Client gửi theo batch trễ | ❌ Lệch khung giờ | **Lỗ hổng #4** |

---

## 2. Trước khi vẽ: 6 câu hỏi phải hỏi interviewer

Ở level Principal, **hỏi đúng câu trước khi vẽ** được chấm điểm ngang với bản vẽ. Sáu câu này mỗi
câu đều **đổi kiến trúc**, không phải hỏi cho có:

| # | Câu hỏi | Nếu trả lời A → | Nếu trả lời B → |
|---|---|---|---|
| 1 | Mỗi request có bao nhiêu cặp key-value? | ~10 → 1.7k event/s, Postgres thuần đủ | ~1000 → 167k event/s, phải có stream layer |
| 2 | Cần thống kê **tổng** hay **breakdown theo từng key**? | Tổng → cardinality ~3 dòng/phút, cực nhẹ | Theo key → **cardinality explosion**, xem [§6.7](#67-cardinality--cái-bẫy-chính-của-bài-này) |
| 3 | Tập key hợp lệ có bao nhiêu, đổi thường không? | Vài nghìn, tĩnh → HashSet in-memory | Hàng triệu, động → Bloom filter + store |
| 4 | Độ trễ chấp nhận được của số liệu: real-time tuyệt đối hay trễ ~1 phút? | Trễ 1 phút OK → async aggregation | Tuyệt đối → phải đọc-gộp cả tầng chưa seal |
| 5 | Khoảng thời gian truy vấn xa nhất là bao lâu? | 30 ngày → 2 tầng rollup | 2 năm → cần tầng ngày + retention policy |
| 6 | Số liệu này dùng để làm gì — dashboard hay billing? | Dashboard → chấp nhận xấp xỉ | Billing → **exactly-once bắt buộc**, phải có raw + đối soát |

> **Câu nên nói ra:** *"Trước khi vẽ tôi cần chốt một con số: mỗi request bao nhiêu cặp key-value?
> Vì 10k request/phút không phải đơn vị tải thật — đơn vị tải thật là số event/giây, và hai con số
> đó có thể lệch nhau 100 lần."*
>
> Câu này một mình đã tách bạn khỏi ứng viên vẽ ngay Kafka lên bảng.

---

## 3. Ước lượng tải — con số quyết định kiến trúc

Giả định để có số cụ thể (**nói rõ là giả định**): trung bình **20 cặp key-value/request**.

```text
Request:   10.000 / phút          =    167 req/s
Event:     10.000 × 20            =    200.000 event/phút  =  3.333 event/s
Mỗi ngày:  200.000 × 60 × 24      =    288 triệu event/ngày
```

**Raw storage** (mỗi event ~60 byte sau khi nén: uuid 16B + ts 8B + key ref + outcome 1B):

```text
288M × 60B  ≈  17 GB/ngày raw  →  ~120 GB cho 7 ngày retention
```

→ **Đây là lý do không được scan raw để trả lời truy vấn.** 17 GB/ngày mà quét cho mỗi lần hỏi
dashboard thì chết ngay.

**Rollup storage** (chỉ đếm tổng, 4 outcome type):

```text
1 phút:  1.440 bucket/ngày × 4 outcome  =    5.760 dòng/ngày   ← không đáng kể
1 giờ:      24 bucket/ngày × 4          =       96 dòng/ngày
1 ngày:      1 bucket     × 4           =        4 dòng/ngày
```

**Đây là toàn bộ luận điểm của bài:** dữ liệu đọc nhỏ hơn dữ liệu ghi **~3000 lần**. Truy vấn "24h
qua" chỉ chạm 96 dòng. Truy vấn "cả năm ngoái" chạm 1.460 dòng. Đó là lý do read path luôn dưới
10ms bất kể tải ghi bao nhiêu.

**Con số cần nhớ để nói tự tin:**

| Đại lượng | Giá trị | Ý nghĩa khi tranh luận |
|---|---|---|
| 3.3k event/s | Tải ghi thật | Postgres/Timescale 1 node **thừa sức** (~50-100k insert/s với COPY/batch) |
| 17 GB/ngày | Raw | Cần partition + compression + retention |
| 96 dòng | Đọc cho "24h qua" | Read path là **hằng số nhỏ**, không phụ thuộc tải |
| ~100k event/s | Ngưỡng cần Kafka+Flink | Nói ra ngưỡng này = biết right-sizing |

> **Điểm cộng lớn:** nói thẳng *"10k/phút chưa cần Kafka"*. Rất nhiều ứng viên vẽ Kafka + Flink +
> ClickHouse cho tải 167 req/s. Biết **khi nào chưa cần** là tín hiệu Principal — đúng cùng logic
> bạn đã dùng khi kết luận *chưa nên fine-tune embedding vì collection mới có 4 style*
> ([AI-STACK-INTERVIEW-ANSWERS.md §8.7](../AI-STACK-INTERVIEW-ANSWERS.md)).

---

## 4. Design tầng 1 — bản tối giản đúng

Vẽ cái này lên bảng trước. Nó **đã trả lời đủ đề bài**. Rồi mới nói phần scale.

```text
                      ┌──────────────────────────────────────┐
  POST /events        │  API (stateless, N instance)         │
  {ts, items:[{k,v}]} │  1. validate schema                  │
 ───────────────────► │  2. phân loại: true/false/fake/malformed
                      │  3. append RAW (batch COPY, KHÔNG lock)
                      │  4. trả 202 ngay ────────────────────┼──►  202 Accepted
                      └───────────────┬──────────────────────┘
                                      │  (append-only, PK = event_id)
                                      ▼
                      ┌──────────────────────────────────────┐
                      │  raw_events   (hypertable, 7 ngày)   │  ← NGUỒN SỰ THẬT
                      └───────────────┬──────────────────────┘
                                      │  rollup ASYNC theo EVENT TIME
                                      ▼
             ┌────────────────┬────────────────┬────────────────┐
             │  agg_1m (30d)  │  agg_1h  (1y)  │  agg_1d (∞)    │  ← DẪN XUẤT, tính lại được
             └────────┬───────┴────────┬───────┴────────┬───────┘
                      └────────────────┼────────────────┘
                                       ▼
  GET /stats?from&to   ┌──────────────────────────────────────┐
 ◄──────────────────── │  Query: phân rã [t1,t2) thành các    │
   {true, false,       │  bucket đã tính sẵn rồi CỘNG lại      │
    fake_key, rate}    │  → không bao giờ chạm raw            │
                       └──────────────────────────────────────┘
```

**Ba nguyên tắc, nói thành lời:**

1. **Ghi tách khỏi tính.** API chỉ append rồi trả `202`. Không lock, không aggregate đồng bộ.
2. **Đếm theo bucket thời gian, không phải một counter tổng.** Mỗi bucket giữ riêng
   `true/false/fake_key`. Range query = cộng các bucket rơi vào khoảng đó.
3. **Raw là sự thật, rollup là dẫn xuất.** Rollup luôn **tính lại được** từ raw → sửa được sai,
   backfill được, đối soát được.

**Đối chiếu trực tiếp với giải pháp cũ:**

| | Giải pháp cũ | Bản sửa |
|---|---|---|
| Đơn vị đếm | 1 counter tổng | 1 counter **cho mỗi bucket thời gian** |
| Thời điểm tính | Đồng bộ, trong lock, lúc insert | **Async**, sau khi ghi, atomic không cần lock tường minh |
| Nguồn sự thật | Chính counter | **Raw events**; counter là dẫn xuất |
| Trả lời range query | Không | Cộng bucket |
| Đếm theo | Thời điểm insert | **Thời điểm sự kiện** (event time) |

---

## 5. Design tầng 2 — bản production

Thêm 4 thứ khi tải tăng hoặc yêu cầu chặt hơn. **Chỉ vẽ khi được hỏi tiếp** — vẽ hết ngay từ đầu là
over-engineering.

```text
                    ┌─────────────┐
  Client ──────────►│ API Gateway │  rate limit / auth / per-tenant quota
                    └──────┬──────┘
                           ▼
                    ┌─────────────┐   pipeline HINCRBY   ┌──────────────────┐
                    │ Ingest svc  │─────────────────────►│ Redis            │
                    │ (stateless) │   (đường NÓNG)       │ hash theo giờ    │──► "24h qua" <2ms
                    └──────┬──────┘                      │ TTL 48h          │
                           │                             └──────────────────┘
                           │ produce (đường BỀN)
                           ▼
                    ┌─────────────┐
                    │ Kafka       │  partition key = tenant_id  (giữ thứ tự trong tenant)
                    │ retention 7d│  → replay được khi rollup sai
                    └──────┬──────┘
                           ▼
                    ┌─────────────┐         ┌───────────────────────────────┐
                    │ Consumer    │────────►│ raw_events (hypertable)       │
                    │ batch 5k    │         └───────────┬───────────────────┘
                    └─────────────┘                     ▼
                                              agg_1m → agg_1h → agg_1d
                                                        ▲
                    ┌───────────────────────────────────┴──────┐
                    │ Reconcile job (mỗi giờ):                 │
                    │  SUM(agg_1m) vs COUNT(raw) trong 2h qua  │
                    │  lệch > 0 → alert + refresh lại bucket   │
                    └──────────────────────────────────────────┘
```

**Vì sao có hai đường (Redis + Kafka) chứ không một:**

| Đường | Trả lời câu gì | Đặc tính |
|---|---|---|
| **Nóng** — Redis hash theo giờ | *"24h qua"* — câu hỏi hay gặp nhất, cần nhanh nhất | 25 `HGETALL` trong 1 pipeline ≈ **1 RTT, <2ms**. Mất cũng không sao — dựng lại được từ raw |
| **Bền** — Kafka → Postgres | Range query bất kỳ, audit, backfill | Chậm hơn (10-50ms) nhưng **chính xác và tính lại được** |

> **Trade-off phải nói ra:** hai đường = **hai nguồn số liệu có thể lệch nhau**. Chấp nhận được vì
> Redis chỉ phục vụ dashboard (xấp xỉ trong ~vài giây là đủ), còn mọi câu trả lời "chính thức" đi
> đường Postgres. Và có **reconcile job** đo chính xác độ lệch đó — không đoán.

---

## 6. Chi tiết từng phần

### 6.1 Data model

```sql
-- ══ NGUỒN SỰ THẬT ══════════════════════════════════════════════════════
CREATE TABLE raw_events (
    event_id   uuid        NOT NULL,   -- do CLIENT sinh → idempotency key
    ts         timestamptz NOT NULL,   -- EVENT TIME, không phải now()
    tenant_id  text        NOT NULL,
    key_name   text        NOT NULL,
    outcome    smallint    NOT NULL,   -- 0=false 1=true 2=fake_key 3=malformed
    PRIMARY KEY (event_id, ts)         -- ts phải nằm trong PK: ràng buộc của hypertable
);

SELECT create_hypertable('raw_events', 'ts', chunk_time_interval => INTERVAL '1 hour');

-- nén chunk cũ hơn 1 ngày: ~10-20× tiết kiệm trên dữ liệu dạng này
ALTER TABLE raw_events SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'tenant_id, outcome',
    timescaledb.compress_orderby   = 'ts DESC'
);
SELECT add_compression_policy('raw_events', INTERVAL '1 day');
SELECT add_retention_policy  ('raw_events', INTERVAL '7 days');
```

**Bốn quyết định trong 8 dòng này, mỗi cái nên giải thích được:**

| Quyết định | Lý do |
|---|---|
| `event_id` do **client** sinh | Là idempotency key. Server sinh thì retry sẽ ra id mới → đếm trùng |
| `PRIMARY KEY (event_id, ts)` | TimescaleDB bắt buộc cột partition có mặt trong mọi unique index. **Hệ quả cần biết:** cùng `event_id` nhưng khác `ts` vẫn insert được 2 lần → `ts` **phải** là event time deterministic từ client, không được là `now()` |
| `outcome smallint` không phải text | 288M dòng/ngày — 1 byte vs ~10 byte là 2.5 GB/ngày |
| `chunk_time_interval = 1 hour` | ~12M dòng/chunk. Timescale khuyến nghị chunk vừa RAM; 1 giờ hợp với tải này. Ngày thì chunk quá to, phút thì quá nhiều chunk |

**Insert idempotent:**

```sql
INSERT INTO raw_events (event_id, ts, tenant_id, key_name, outcome)
VALUES (...), (...), ...             -- batch, không lock
ON CONFLICT (event_id, ts) DO NOTHING;   -- retry lần 2 là no-op
```

---

### 6.2 Time bucket và rollup nhiều tầng

**TimescaleDB continuous aggregate** làm đúng việc này native — rollup tự chạy, tự tính lại phần
gần đây:

```sql
-- ══ TẦNG 1 PHÚT ════════════════════════════════════════════════════════
CREATE MATERIALIZED VIEW agg_1m
WITH (timescaledb.continuous) AS
SELECT time_bucket('1 minute', ts) AS bucket,
       tenant_id,
       outcome,
       count(*) AS cnt
FROM raw_events
GROUP BY 1, 2, 3;

SELECT add_continuous_aggregate_policy('agg_1m',
    start_offset      => INTERVAL '30 minutes',  -- ◄── CỬA SỔ TÍNH LẠI (watermark)
    end_offset        => INTERVAL '1 minute',    -- ◄── không đụng bucket đang chạy dở
    schedule_interval => INTERVAL '1 minute');

-- ══ TẦNG GIỜ — cagg trên cagg (Timescale ≥ 2.9) ════════════════════════
CREATE MATERIALIZED VIEW agg_1h
WITH (timescaledb.continuous) AS
SELECT time_bucket('1 hour', bucket) AS bucket, tenant_id, outcome, sum(cnt) AS cnt
FROM agg_1m GROUP BY 1, 2, 3;

SELECT add_continuous_aggregate_policy('agg_1h',
    start_offset => INTERVAL '3 hours', end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '10 minutes');

-- ══ TẦNG NGÀY ══════════════════════════════════════════════════════════
CREATE MATERIALIZED VIEW agg_1d
WITH (timescaledb.continuous) AS
SELECT time_bucket('1 day', bucket) AS bucket, tenant_id, outcome, sum(cnt) AS cnt
FROM agg_1h GROUP BY 1, 2, 3;

SELECT add_retention_policy('agg_1m', INTERVAL '30 days');
SELECT add_retention_policy('agg_1h', INTERVAL '1 year');
-- agg_1d giữ vĩnh viễn: 4 dòng/ngày/tenant, 100 năm cũng chỉ ~146k dòng
```

**Hai tham số đáng nói nhất — vì chúng là câu trả lời cho hai câu hỏi khó:**

- `start_offset => '30 minutes'` → **đây chính là cách xử lý dữ liệu đến trễ**. Mỗi phút, Timescale
  **tính lại từ đầu** toàn bộ bucket trong 30 phút gần nhất. Event đến trễ 20 phút vẫn được đếm
  đúng vào bucket của nó. Không cần code gì thêm. *"Cơ chế late-data của tôi là một tham số config,
  và tôi biết giá trị của nó"* — câu đó nghe rất khác *"tôi sẽ xử lý late data"*.
- `end_offset => '1 minute'` → không materialize bucket đang chạy dở (sẽ sai). Nhưng **real-time
  aggregate** của Timescale (mặc định bật) tự động UNION phần đã materialize với phần raw mới nhất
  khi query → người dùng vẫn thấy số liệu tới giây hiện tại.

**Phân bổ tầng theo độ dài truy vấn:**

| Khoảng hỏi | Tầng dùng | Số dòng đọc |
|---|---|---|
| < 2 giờ | `agg_1m` | ≤ 120 × 4 |
| 2 giờ – 30 ngày | `agg_1h` (+ mép bằng `agg_1m`) | ≤ 720 × 4 |
| > 30 ngày | `agg_1d` (+ mép bằng `agg_1h`) | ≤ 365 × 4 |

---

### 6.3 Thuật toán phân rã khoảng thời gian bất kỳ

Đây là **phần lõi trí tuệ** của bài — và là chỗ chứng minh bạn thật sự trả lời được *"khoảng thời
gian bất kỳ"*, không chỉ nói suông.

**Ý tưởng:** greedy — từ `t1` đi tới `t2`, mỗi bước lấy **khối thô nhất còn vừa**.

```text
Hỏi: 2026-08-26 03:17:30  →  2026-08-29 11:42:10

 03:17:30─┐                                                    ┌─11:42:10
          │  30s   │ 42 phút │  20 giờ │ 2 NGÀY │ 11 giờ │ 42 phút │ 10s
          └─ raw ──┴─ agg_1m ┴─ agg_1h ┴─ agg_1d┴─ agg_1h ┴ agg_1m ┴ raw ─┘

Tổng: 2 truy vấn raw + 84 dòng phút + 31 dòng giờ + 2 dòng ngày  ≈ 120 dòng
So với: quét raw 3 ngày = 864 TRIỆU dòng.       → nhanh hơn ~7 triệu lần
```

```python
from datetime import datetime, timedelta, timezone

TIERS = [                                  # thô → mịn
    ("agg_1d", timedelta(days=1)),
    ("agg_1h", timedelta(hours=1)),
    ("agg_1m", timedelta(minutes=1)),
]

def floor_to(t: datetime, step: timedelta) -> datetime:
    epoch = datetime(1970, 1, 1, tzinfo=timezone.utc)
    n = (t - epoch) // step
    return epoch + n * step

def decompose(t1: datetime, t2: datetime) -> list[tuple[str, datetime, datetime]]:
    """Phân rã [t1, t2) thành ít mảnh nhất, mỗi mảnh nằm trọn trong một tầng rollup."""
    parts, cur = [], t1
    while cur < t2:
        for tier, step in TIERS:
            if floor_to(cur, step) == cur and cur + step <= t2:
                start, n = cur, 0
                while floor_to(cur, step) == cur and cur + step <= t2:
                    cur += step
                    n += 1
                parts.append((tier, start, cur))
                break
        else:
            # chưa thẳng biên phút (hoặc còn < 1 phút ở đuôi) → đọc raw đoạn ngắn
            nxt = min(t2, floor_to(cur, timedelta(minutes=1)) + timedelta(minutes=1))
            parts.append(("raw_events", cur, nxt))
            cur = nxt
    return parts
```

**Chặn trên số mảnh** — nói được con số này là ghi điểm:

```text
≤ 2 mảnh raw  +  ≤ 118 phút  +  ≤ 46 giờ  +  N ngày
→ truy vấn 1 NĂM bất kỳ đọc tối đa ~530 dòng.  Bounded, không phụ thuộc tải ghi.
```

**Ba cái bẫy trong phân rã, phải nêu:**

1. **Timezone.** `time_bucket('1 day', ts)` cắt theo **UTC**. Khách hỏi "hôm nay" theo giờ VN
   (UTC+7) thì bucket ngày UTC lệch 7 tiếng. Cách xử lý: `time_bucket('1 day', ts, 'Asia/Ho_Chi_Minh')`
   (Timescale hỗ trợ timezone-aware bucket), **hoặc** chỉ rollup tới tầng giờ rồi ghép ngày ở query
   layer. Cách 2 an toàn hơn nếu có nhiều timezone.
2. **Nửa mở `[t1, t2)`.** Phải nhất quán, nếu không sẽ đếm trùng ở mép giữa hai mảnh liền nhau.
3. **Mảnh raw ở hai đầu.** Nếu raw đã bị retention xoá (>7 ngày) thì không đọc được — khi đó
   **làm tròn** `t1`/`t2` về biên phút và **nói rõ độ chính xác** ra ngoài response
   (`"resolution": "1m"`), không im lặng trả số sai.

---

### 6.4 Idempotency — vì sao counter KHÔNG phải nguồn sự thật

Đây là chỗ giải pháp cũ hỏng ngầm, và interviewer giỏi sẽ đào tới.

**Vấn đề:** mọi hệ thống có retry đều là **at-least-once**. Client timeout rồi gửi lại, consumer
Kafka rebalance rồi đọc lại offset cũ. Nếu bạn `INCR` counter, thì:

```text
INCR là atomic  ✅        INCR là idempotent  ❌
→ chạy lại lần 2 = cộng thêm lần nữa = SAI, và không có cách nào biết là đã sai.
```

**Lời giải — đẩy idempotency xuống tầng raw, không đặt ở tầng counter:**

```text
raw_events:  PK (event_id, ts) + ON CONFLICT DO NOTHING
             → insert lại lần thứ 2, 3, 10 đều cho cùng một kết quả  ✅ idempotent thật

agg_*:       KHÔNG phải increment. Là kết quả của một câu COUNT(*) chạy lại trên raw.
             → chạy lại rollup bao nhiêu lần cũng ra cùng con số  ✅ idempotent theo dẫn xuất
```

**Đây chính là điều continuous aggregate làm:** nó không `+= 1`, nó **tính lại `count(*)`** cho
bucket rồi **ghi đè**. Vì vậy toàn bộ chuỗi là idempotent mà bạn không viết một dòng dedupe nào.

**Reconcile job — thứ chứng minh con số đúng, không phải tin là đúng:**

```sql
-- chạy mỗi giờ, đối soát 2 giờ gần nhất
WITH r AS (
    SELECT outcome, count(*) AS n FROM raw_events
    WHERE ts >= now() - INTERVAL '2 hours' AND ts < date_trunc('hour', now())
    GROUP BY outcome
), a AS (
    SELECT outcome, sum(cnt) AS n FROM agg_1m
    WHERE bucket >= now() - INTERVAL '2 hours' AND bucket < date_trunc('hour', now())
    GROUP BY outcome
)
SELECT COALESCE(r.outcome, a.outcome) AS outcome,
       COALESCE(r.n, 0) - COALESCE(a.n, 0) AS drift
FROM r FULL OUTER JOIN a USING (outcome)
WHERE COALESCE(r.n, 0) <> COALESCE(a.n, 0);
```

`drift <> 0` → alert + `CALL refresh_continuous_aggregate('agg_1m', t1, t2)`.

> **Câu nên nói:** *"Tôi có một job mỗi giờ so tổng rollup với `COUNT(*)` trên raw. Nếu lệch, nó
> alert và tự refresh lại bucket đó. Nghĩa là tôi không **tin** số liệu đúng — tôi **đo** nó."*
> Đây là kiểu lập luận đã có sẵn trong hồ sơ của bạn: cùng tinh thần với gate
> `test_no_retry_setter_outside_verify` ([AI-STACK-INTERVIEW-ANSWERS.md §4B](../AI-STACK-INTERVIEW-ANSWERS.md))
> — biến một bất biến kiến trúc thành một cái test tự động, thay vì một dòng ghi chú trong doc.

---

### 6.5 Late-arriving data và watermark

```text
Trục thời gian sự kiện:
    09:58  ─── event xảy ra ở client
             │
             │  client gom batch, mạng chậm, retry
             ▼
    10:03  ─── server nhận

Đếm theo INGEST time  → vào bucket 10:03   ❌ sai khung, không sửa được
Đếm theo EVENT time   → vào bucket 09:58   ✅ đúng, nhưng bucket 09:58 đã "seal" chưa?
```

**Cơ chế watermark, ba mức:**

| Độ trễ của event | Xử lý | Cấu hình |
|---|---|---|
| ≤ 30 phút | Tự động đúng — rollup tính lại bucket đó | `start_offset => '30 minutes'` |
| 30 phút – 7 ngày | Không tự sửa, nhưng raw còn → **backfill thủ công** | `CALL refresh_continuous_aggregate('agg_1m', t1, t2)` |
| > 7 ngày (raw đã xoá) | **Từ chối, đếm riêng** | `events_rejected_too_late` counter + trả `422` |

**Nguyên tắc:** không bao giờ **âm thầm** bỏ event trễ. Luôn có một counter đếm số event bị từ chối
— nếu counter đó tăng bất thường thì đó là tín hiệu client đang hỏng, và bạn biết được.

> **Đánh đổi phải nói:** `start_offset` càng lớn thì càng chịu được trễ, nhưng mỗi lần refresh phải
> tính lại càng nhiều dữ liệu → tốn CPU. 30 phút là điểm tôi chọn cho tải này; nếu client là mobile
> (offline lâu) thì phải nâng lên, và khi đó nên tách một job backfill riêng chạy thưa hơn thay vì
> nới `start_offset`.

---

### 6.6 Phân loại "fake key"

Đề bài nói *"có `true`/`false` hay không"* — nghĩa là có ít nhất **3 nhóm**, và thực tế nên là **4**:

```python
from enum import IntEnum

class Outcome(IntEnum):
    FALSE     = 0   # key hợp lệ, value = false
    TRUE      = 1   # key hợp lệ, value = true
    FAKE_KEY  = 2   # key KHÔNG có trong registry
    MALFORMED = 3   # key hợp lệ nhưng value không phải boolean  ← thường bị bỏ sót

def classify(key: str, value, registry) -> Outcome:
    if key not in registry:                    # kiểm tra key TRƯỚC
        return Outcome.FAKE_KEY
    if isinstance(value, bool):
        return Outcome.TRUE if value else Outcome.FALSE
    if isinstance(value, str) and value.lower() in ("true", "false"):
        return Outcome.TRUE if value.lower() == "true" else Outcome.FALSE
    return Outcome.MALFORMED
```

**Nêu `MALFORMED` ra là điểm cộng:** đề bài chỉ nói true/false/fake key, nhưng thực tế `value` có thể
là `null`, `"1"`, `"yes"`, số. Gom chúng vào `false` là **giấu lỗi**; đếm riêng thì client hỏng lộ ra
ngay. *"Tôi tách nhóm thứ tư vì im lặng quy về false sẽ che mất một class lỗi của client"* — câu này
cho thấy bạn nghĩ về vận hành, không chỉ về đề bài.

**Registry lưu ở đâu — theo kích thước:**

| Số key hợp lệ | Cấu trúc | Ghi chú |
|---|---|---|
| < 100k | `HashSet` in-memory, refresh mỗi 60s | Nhanh nhất, đơn giản nhất. Chấp nhận 60s stale |
| 100k – 10M | Redis `SET` + `SISMEMBER` (pipeline) | 1 RTT cho cả batch |
| > 10M | **Bloom filter** in-memory + fallback store | Xem dưới |

**Bloom filter — hướng dùng phải đúng chiều, đây là chỗ dễ nói sai:**

```text
Bloom trả "KHÔNG CÓ"  → chắc chắn không có     → FAKE_KEY, khỏi hỏi store   ✅ nhanh
Bloom trả "CÓ THỂ CÓ" → có thể false positive  → PHẢI hỏi store để xác nhận
```

Bloom **không có false negative** → dùng nó để **loại nhanh key giả** thì an toàn tuyệt đối. Nếu tỉ
lệ fake key cao (đề bài gợi ý ~50% "không thành công"), Bloom cắt được phần lớn lượt tra store.
Với 10M key và `p = 1%`: **~12 MB RAM**, 7 hàm hash.

> ⚠️ **Không được** dùng Bloom theo chiều ngược lại ("Bloom nói có → coi là hợp lệ") — false positive
> sẽ đếm key giả thành key thật. Nói rõ chiều đúng chứng minh bạn hiểu cấu trúc, không chỉ nhớ tên nó.

---

### 6.7 Cardinality — cái bẫy chính của bài này

**Đây là chỗ bài toán có thể vỡ, và là câu hỏi #2 ở [§2](#2-trước-khi-vẽ-6-câu-hỏi-phải-hỏi-interviewer).**

```text
Câu hỏi A: "tổng true/false/fake trong khoảng T"
  → cardinality = 4 outcome × số bucket
  → 1 ngày = 4 × 1440 = 5.760 dòng           ✅ không đáng kể

Câu hỏi B: "breakdown theo TỪNG key"
  → cardinality = 4 × số key × số bucket
  → 100k key: 4 × 100.000 × 1440 = 576 TRIỆU dòng/ngày
  → NHIỀU HƠN cả số event thô (288M).        ❌ rollup phản tác dụng hoàn toàn
```

**Cardinality explosion:** khi số tổ hợp nhóm vượt số dòng gốc, pre-aggregation **làm hệ thống tệ
đi**. Nhận ra ranh giới này là tín hiệu senior rõ nhất trong cả bài.

**Ba lối thoát, chọn theo yêu cầu thật:**

| Lối | Cách làm | Được / Mất |
|---|---|---|
| **Top-K thay vì toàn bộ** | Space-Saving / Count-Min Sketch giữ ~1000 key nóng nhất mỗi bucket | Bộ nhớ hằng số. Mất: key đuôi dài không chính xác. **Thường là đủ** — không ai đọc bảng 100k dòng |
| **Rollup thô hơn cho chiều key** | Tầng phút chỉ giữ tổng; breakdown theo key **chỉ có ở tầng giờ/ngày** | Giảm 60×. Mất: không xem được key-level ở độ phân giải phút |
| **Đổi sang columnar** | ClickHouse: lưu raw dạng cột, `GROUP BY key` on-the-fly | Không cần rollup theo key. Mất: thêm một hệ thống phải vận hành |

> **Câu nên nói ra ngay ở phút thứ 5:** *"Trước khi tôi vẽ tiếp — thống kê này là tổng hay breakdown
> theo từng key? Vì nếu theo từng key thì pre-aggregation có thể sinh ra nhiều dòng hơn cả dữ liệu
> gốc, và tôi sẽ phải chọn kiến trúc khác."*

---

### 6.8 Tỉ lệ 50% — đừng bao giờ lưu tỉ lệ

Đề bài nhắc *"thành công bao nhiêu, 50%"*. Hai điều rút ra:

**(a) Lưu tử số và mẫu số, tính tỉ lệ ở query time. Không lưu tỉ lệ.**

Vì **tỉ lệ không cộng được**:

```text
Phút 1:  90 true / 100  → 90%
Phút 2:  10 true / 1000 → 1%

Trung bình hai tỉ lệ:  (90% + 1%) / 2 = 45.5%     ❌ SAI
Tỉ lệ thật:            (90 + 10) / 1100 = 9.1%    ✅ ĐÚNG
```

Đây là lỗi kinh điển "average of averages". Lưu `count` rồi chia lúc đọc thì không bao giờ mắc.
Cùng lý do đó: **không lưu `avg`, `p95`, `percentile` ở tầng rollup** — chúng cũng không cộng được.
Muốn có percentile phải lưu sketch cộng được (t-digest/HDR histogram), không lưu con số kết quả.

**(b) 50% nghĩa là không nhóm nào hiếm.**

Nếu `true` chỉ chiếm 0.01%, có thể chỉ đếm `true` và suy ra `false = total − true`. Với 50/50 thì
mẹo đó vô nghĩa — phải đếm đủ mọi nhóm. Nói ra điều này cho thấy bạn **đọc kỹ đề**, không lướt qua
con số 50%.

---

### 6.9 Cache và thundering herd

Câu *"24h qua"* là câu hỏi nóng nhất → cache nó. Nhưng cache có hai bẫy.

**Bẫy 1 — TTL đoán mò.** Đặt `TTL = 60s` là đang đoán. Thay bằng **version-bump**: nhét id của
bucket đã seal gần nhất vào cache key.

```python
# bucket phút đã seal gần nhất → key tự đổi khi có dữ liệu mới, không cần TTL
sealed = floor_to(now, timedelta(minutes=1)) - timedelta(minutes=1)
cache_key = f"stats:{tenant}:24h:{int(sealed.timestamp())}"
```

Bucket mới seal → key mới → cache miss tự nhiên → luôn tươi, không bao giờ stale, không phải chỉnh TTL.

**Bẫy 2 — thundering herd.** Key đổi cùng lúc cho **mọi** client → tất cả cùng miss → tất cả cùng
đánh DB.

```python
async def get_stats_24h(tenant: str):
    key = cache_key_for(tenant)
    if (v := await redis.get(key)) is not None:
        return json.loads(v)
    # single-flight: chỉ MỘT request được đi tính, số còn lại chờ rồi đọc cache
    if await redis.set(key + ":lock", "1", nx=True, ex=10):
        try:
            v = await compute_from_rollups(tenant)
            await redis.set(key, json.dumps(v), ex=300)
            return v
        finally:
            await redis.delete(key + ":lock")
    await asyncio.sleep(0.05)
    return await get_stats_24h(tenant)      # thực tế: thêm giới hạn số lần thử
```

> **Chú ý sự khác biệt với giải pháp cũ:** ở đây cũng có lock — nhưng lock nằm ở **read path, cho
> một truy vấn tốn kém, và không chặn ghi**. Lock trong giải pháp cũ nằm ở **write path và chặn mọi
> request**. Cùng một từ "lock", vị trí khác nhau hoàn toàn. Đây là một câu đáng nói ra vì nó cho
> thấy bạn không bị dị ứng với lock, mà biết **đặt lock đúng chỗ**.

---

### 6.10 Backpressure khi burst

10k/phút là trung bình. Peak có thể 5-10×. Ba lớp phòng thủ:

```text
Lớp 1 — Rate limit ở gateway:      token bucket theo tenant  → 429 + Retry-After
Lớp 2 — Hàng đợi CÓ GIỚI HẠN:      bounded queue trong app   → đầy thì 503, KHÔNG chờ vô hạn
Lớp 3 — Batch + backpressure DB:   gom 5000 dòng/COPY; consumer chậm → Kafka lag tăng (an toàn)
```

**Nguyên tắc:** *hàng đợi không giới hạn là bug, không phải tính năng*. Queue vô hạn chỉ đổi lỗi
"từ chối nhanh" thành lỗi "OOM sau 20 phút" — tệ hơn nhiều vì mất luôn dữ liệu đã nhận.

**Vì sao `202 Accepted` chứ không `200 OK`:** `202` nói đúng sự thật — "đã nhận, chưa xử lý xong".
Client biết là số liệu chưa lên ngay. Trả `200` là nói dối về một hệ thống eventual consistency.

---

### 6.11 Retention và chi phí lưu trữ

| Tầng | Giữ | Dung lượng (1 tenant, tải giả định) | Xoá bằng |
|---|---|---|---|
| `raw_events` | 7 ngày | ~120 GB (nén còn ~10-20 GB) | `DROP CHUNK` |
| `agg_1m` | 30 ngày | ~173k dòng — vài MB | `DROP CHUNK` |
| `agg_1h` | 1 năm | ~35k dòng | `DROP CHUNK` |
| `agg_1d` | vĩnh viễn | 1.460 dòng/năm | không xoá |

**Điểm phải nhấn:** xoá bằng **`DROP CHUNK`, không phải `DELETE`**.

```text
DELETE FROM raw_events WHERE ts < now() - '7 days'
  → 288M dòng × 7 = 2 tỉ dead tuple → autovacuum ngộp → bloat → hệ thống chậm dần rồi chết

DROP CHUNK
  → unlink file. O(1). Không dead tuple. Không vacuum.
```

Đây là lý do **partition theo thời gian** không chỉ để query nhanh — nó còn để **xoá được**. Nhiều
hệ thống chết vì retention chứ không phải vì tải.

---

### 6.12 Observability — 6 metric phải có

| Metric | Cảnh báo điều gì |
|---|---|
| `ingest_events_per_sec` | Tải thật (theo event, không theo request) |
| `event_time_lag_seconds` (p50/p99) | Khoảng cách event time → ingest time. Tăng đột ngột = client hỏng hoặc mạng nghẽn |
| `rollup_seal_lag_seconds` | Bucket gần nhất đã materialize cách hiện tại bao lâu. Tăng = rollup không theo kịp |
| `events_rejected_too_late` | Event trễ quá watermark bị từ chối — **không được im lặng** |
| `reconcile_drift` | Lệch giữa `SUM(agg)` và `COUNT(raw)`. **Phải bằng 0.** Khác 0 = mất hoặc đếm trùng |
| `query_latency_p99{range_bucket}` | Tách theo độ dài khoảng hỏi — truy vấn 1 năm chậm là bình thường, truy vấn 1 giờ chậm là bug |

> `reconcile_drift` là metric đắt giá nhất trong bảng: nó là thứ duy nhất **chứng minh** con số bạn
> trả cho khách là đúng. Năm metric kia đo sức khoẻ; metric này đo **tính đúng đắn**.

---

## 7. Chọn tech stack — 3 phương án và ngưỡng chuyển

| | **A. Postgres + TimescaleDB** | **B. A + Redis (2 đường)** | **C. Kafka + Flink + ClickHouse** |
|---|---|---|---|
| **Tải phù hợp** | ≤ ~30k event/s | ≤ ~50k event/s | ≥ 100k event/s |
| **Đọc "24h qua"** | 10-30 ms | **< 2 ms** | 10-50 ms |
| **Range query bất kỳ** | ✅ SQL thuần | ✅ (qua Postgres) | ✅ rất nhanh, columnar |
| **Breakdown theo key cardinality cao** | ❌ nổ dòng | ❌ | ✅ điểm mạnh nhất |
| **Rollup** | Tự động (continuous aggregate) | Như A | Tự viết (Flink window) hoặc `AggregatingMergeTree` |
| **Late data** | ✅ 1 tham số config | ✅ | ✅ Flink watermark (mạnh nhất, phức tạp nhất) |
| **Replay khi rollup sai** | Từ raw (7 ngày) | Từ raw | Từ Kafka (mạnh nhất) |
| **Số hệ thống phải vận hành** | **1** | 2 | **4+** |
| **Người vận hành cần** | 1 DBA quen Postgres | +Redis | +Kafka +Flink +ClickHouse, thường cần team riêng |

### Khuyến nghị cho đúng bài này

> **Phương án A đủ.** 3.3k event/s là **1/10 khả năng** của một node Timescale cấu hình khá. Thêm
> Kafka + Flink cho tải này là **tăng gấp 4 bề mặt vận hành để đổi lấy công suất không dùng tới**.

**Nêu rõ ngưỡng chuyển — đây là phần được chấm điểm cao nhất:**

```text
A → B  khi:  p99 đọc "24h qua" > 50ms, HOẶC QPS đọc > 1000/s
             (nghĩa là: khi ĐỌC thành nút thắt, không phải khi ghi)

B → C  khi:  BẤT KỲ điều nào sau đây:
             • > 50k event/s bền vững (1 node Postgres bắt đầu đuối)
             • cần breakdown theo key với cardinality > ~10k  ← lý do phổ biến NHẤT
             • cần replay > 7 ngày để tính lại theo định nghĩa metric mới
             • có nhiều consumer độc lập cùng cần luồng event này (alerting, ML, billing...)
```

Chú ý: lý do phổ biến nhất để chuyển sang C **không phải là tải** — mà là **cardinality** và **số
consumer**. Nói được điều này chứng tỏ bạn đã đứng ở phía vận hành, không chỉ đọc blog kiến trúc.

**Ánh xạ sang stack Katalon** (Java/Spring, JD nhắc Kafka): phương án B nói bằng ngôn ngữ của họ là
*Spring Boot + Kafka + TimescaleDB + Redis* — xem [§9](#9-nếu-demo-bằng-javaspring-stack-của-katalon).

---

## 8. Demo chạy được — từng bước

**Mục tiêu demo:** chứng minh 3 điều, mỗi điều bằng một con số đo được.

1. Ghi 10k request/phút mà API không chậm đi.
2. Truy vấn khoảng thời gian **bất kỳ** trả về đúng — đối chiếu với `COUNT(*)` trên raw.
3. Truy vấn nhanh **không phụ thuộc** độ dài khoảng hỏi.

**Stack:** FastAPI + TimescaleDB + Redis, chạy bằng docker-compose. Chọn Python vì bạn viết nhanh
nhất và demo không phải bài chấm Java. (Bản Java ở [§9](#9-nếu-demo-bằng-javaspring-stack-của-katalon).)

### Bước 0 — `docker-compose.yml`

```yaml
services:
  db:
    image: timescale/timescaledb:latest-pg16
    environment: { POSTGRES_PASSWORD: dev, POSTGRES_DB: stats }
    ports: ["5432:5432"]
    command: -c shared_preload_libraries=timescaledb -c max_connections=200
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    command: redis-server --appendonly yes      # AOF: đừng mất counter khi restart
```

```bash
docker compose up -d
```

### Bước 1 — schema

Chạy toàn bộ SQL ở [§6.1](#61-data-model) + [§6.2](#62-time-bucket-và-rollup-nhiều-tầng):

```bash
docker compose exec -T db psql -U postgres stats < schema.sql
```

**Kiểm tra ngay** (đừng tin, hãy chạy):

```sql
SELECT view_name, materialization_hypertable_name
  FROM timescaledb_information.continuous_aggregates;
SELECT job_id, application_name, schedule_interval
  FROM timescaledb_information.jobs WHERE job_id >= 1000;
```

### Bước 2 — ingest endpoint

```python
# app.py
import asyncpg, uuid
from datetime import datetime, timezone
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

app = FastAPI()
REGISTRY: set[str] = set()          # nạp lúc startup, refresh nền

class Item(BaseModel):
    key:   str
    value: object

class Batch(BaseModel):
    event_id_prefix: str            # client sinh → idempotency
    ts:    datetime                 # EVENT TIME, không phải now()
    items: list[Item] = Field(max_length=1000)

@app.post("/events", status_code=202)
async def ingest(b: Batch):
    if (datetime.now(timezone.utc) - b.ts).total_seconds() > 7 * 86400:
        METRICS["events_rejected_too_late"] += len(b.items)
        raise HTTPException(422, "event too old to be counted")

    rows = [
        (uuid.uuid5(uuid.NAMESPACE_OID, f"{b.event_id_prefix}:{i}"),
         b.ts, "t1", it.key, int(classify(it.key, it.value, REGISTRY)))
        for i, it in enumerate(b.items)
    ]
    async with POOL.acquire() as con:
        await con.executemany(
            "INSERT INTO raw_events (event_id, ts, tenant_id, key_name, outcome) "
            "VALUES ($1,$2,$3,$4,$5) ON CONFLICT DO NOTHING", rows)

    # đường nóng: hash theo GIỜ, pipeline 1 RTT
    hkey = f"stats:t1:{b.ts:%Y%m%d%H}"
    pipe = REDIS.pipeline()
    for _, _, _, _, oc in rows:
        pipe.hincrby(hkey, str(oc), 1)
    pipe.expire(hkey, 48 * 3600)
    await pipe.execute()

    return {"accepted": len(rows)}
```

**Ba điểm cố ý trong 25 dòng này, chuẩn bị để giải thích khi bị hỏi:**

- `uuid5(namespace, prefix:i)` → **deterministic**. Client gửi lại đúng batch đó → cùng uuid →
  `ON CONFLICT DO NOTHING` nuốt → không đếm trùng. Đây là idempotency thật, không phải dedupe bằng
  cache.
- Redis `expire` **48h** cho hash theo giờ → tự dọn, không cần cron. Bucket giờ chứ không phải phút
  → "24h qua" đọc 25 hash thay vì 1440.
- `max_length=1000` trên `items` → chặn payload khổng lồ ngay ở tầng schema, trước khi chạm DB.

### Bước 3 — query endpoint

```python
TIER_TABLE = {"agg_1d": "agg_1d", "agg_1h": "agg_1h",
              "agg_1m": "agg_1m", "raw_events": "raw_events"}

@app.get("/stats")
async def stats(frm: datetime, to: datetime, tenant: str = "t1"):
    totals = {0: 0, 1: 0, 2: 0, 3: 0}
    async with POOL.acquire() as con:
        for tier, a, b in decompose(frm, to):           # §6.3
            col = "count(*)" if tier == "raw_events" else "sum(cnt)"
            tcol = "ts"      if tier == "raw_events" else "bucket"
            rows = await con.fetch(
                f"SELECT outcome, {col} AS n FROM {TIER_TABLE[tier]} "
                f"WHERE tenant_id=$1 AND {tcol} >= $2 AND {tcol} < $3 GROUP BY outcome",
                tenant, a, b)
            for r in rows:
                totals[r["outcome"]] += r["n"]

    valid = totals[0] + totals[1]
    return {
        "false": totals[0], "true": totals[1],
        "fake_key": totals[2], "malformed": totals[3],
        # tỉ lệ tính LÚC ĐỌC, không lưu — §6.8
        "success_rate": round(totals[1] / valid, 4) if valid else None,
        "parts_scanned": len(decompose(frm, to)),       # để chứng minh chi phí bounded
    }
```

### Bước 4 — sinh tải

```python
# loadgen.py — 10.000 request/phút, 20 item/request
import asyncio, httpx, random, uuid
from datetime import datetime, timedelta, timezone

KEYS = [f"feature_{i}" for i in range(500)]

async def one(client, i):
    now = datetime.now(timezone.utc)
    ts = now - timedelta(seconds=random.randint(0, 120))   # cố tình có event TRỄ
    items = [{"key": random.choice(KEYS) if random.random() > 0.15
                     else f"fake_{uuid.uuid4().hex[:8]}",   # ~15% fake key
              "value": random.random() < 0.5}              # ~50% true — đúng đề
             for _ in range(20)]
    await client.post("http://localhost:8000/events",
        json={"event_id_prefix": uuid.uuid4().hex, "ts": ts.isoformat(), "items": items})

async def main():
    async with httpx.AsyncClient(timeout=10) as c:
        for minute in range(10):
            await asyncio.gather(*(one(c, i) for i in range(10_000)))
            print(f"minute {minute}: 200.000 event đã gửi")

asyncio.run(main())
```

Chú ý loadgen **cố tình** sinh event trễ tới 120 giây và 15% fake key — để demo chứng minh được
[§6.5](#65-late-arriving-data-và-watermark) và [§6.6](#66-phân-loại-fake-key), không phải chỉ chạy
cho đẹp.

### Bước 5 — kiểm chứng đúng đắn (bước quan trọng nhất)

```sql
-- rollup có khớp raw không? Phải ra RỖNG.
WITH r AS (SELECT outcome, count(*) n FROM raw_events
           WHERE ts >= now()-'1 hour'::interval AND ts < date_trunc('minute', now())
           GROUP BY 1),
     a AS (SELECT outcome, sum(cnt) n FROM agg_1m
           WHERE bucket >= now()-'1 hour'::interval AND bucket < date_trunc('minute', now())
           GROUP BY 1)
SELECT * FROM r FULL JOIN a USING (outcome) WHERE r.n IS DISTINCT FROM a.n;
```

```bash
# range query bất kỳ, không thẳng biên — chứng minh §6.3
curl "http://localhost:8000/stats?frm=2026-08-29T03:17:30Z&to=2026-08-29T11:42:10Z"
```

### Bước 6 — đo, ghi số vào bảng

```bash
# so p99 giữa khoảng ngắn và khoảng dài — điểm mấu chốt: KHÔNG chênh nhiều
for r in "1 hour" "24 hours" "30 days" "365 days"; do
  echo -n "$r: "; curl -s -o /dev/null -w "%{time_total}s\n" "http://localhost:8000/stats?..."
done
```

| Đo | Kỳ vọng | Số thật của bạn |
|---|---|---|
| Ingest p99 (10k req/phút) | < 50 ms | *điền* |
| `/stats` 1 giờ | < 15 ms | *điền* |
| `/stats` 24 giờ | < 20 ms | *điền* |
| `/stats` 365 ngày | < 60 ms | *điền* |
| `parts_scanned` cho 365 ngày | ~380 | *điền* |
| `reconcile_drift` | **0** | *điền* |

> **Bảng số của chính bạn** quan trọng hơn cả kiến trúc. Cùng lý do bạn đã điền
> [COMPARISON.md](../katalon-prep-java/03-spring-boot-compare/COMPARISON.md) thay vì chép benchmark
> người khác — trong phòng phỏng vấn, *"tôi đo được 18ms cho 24h và 54ms cho 1 năm"* mạnh hơn hẳn
> *"nó sẽ nhanh vì đã pre-aggregate"*.

**⚠️ Chưa chạy.** Toàn bộ §8 là thiết kế demo, **chưa thực thi trên máy bạn** tại thời điểm viết.
Đừng nói "tôi đã đo được X" cho tới khi thật sự chạy — xem [§12](#12-ranh-giới-trung-thực).

---

## 9. Nếu demo bằng Java/Spring (stack của Katalon)

JD Katalon là **Java first** ([README §"Nên code Java, Python, hay cả hai?"](../README.md)). Nếu đây
thành bài take-home thì viết Java. Kiến trúc **không đổi**, chỉ đổi công cụ:

| Thành phần | Python (§8) | Java/Spring |
|---|---|---|
| API | FastAPI | **Spring Boot WebFlux** (`@PostMapping`, trả `Mono<ResponseEntity>`) |
| Ingest bất đồng bộ | `asyncio` + pool | `spring-kafka` `KafkaTemplate.send()` |
| Batch insert | `executemany` | `JdbcTemplate.batchUpdate()` + `rewriteBatchedStatements=true` |
| Consumer | worker riêng | `@KafkaListener(batch=true)` + `AckMode.MANUAL` |
| Redis pipeline | `redis.pipeline()` | `RedisTemplate.executePipelined()` |
| Rollup | continuous aggregate | **giữ nguyên** — nằm ở DB, không ở app |
| Rate limit | gateway | Bucket4j hoặc Resilience4j `RateLimiter` |
| Metric | dict | Micrometer → Prometheus |
| Test tích hợp | pytest | **Testcontainers** (`TimescaleDB` + `Kafka` container thật) |

**Ba chi tiết Java đáng nêu vì chúng chứng minh chiều sâu, không chỉ biết tên framework:**

1. **`rewriteBatchedStatements=true`** trong JDBC URL — không bật thì `batchUpdate` vẫn gửi từng
   statement một, và bạn mất ~10× throughput mà không có lỗi nào báo. Đây là loại chi tiết chỉ người
   từng đo mới biết.
2. **`AckMode.MANUAL` + commit sau khi insert xong** — commit trước khi ghi là **at-most-once** (mất
   dữ liệu khi crash); commit sau là **at-least-once** (đếm trùng) — và đếm trùng đã được `ON
   CONFLICT DO NOTHING` khử ở [§6.4](#64-idempotency--vì-sao-counter-không-phải-nguồn-sự-thật).
   Chuỗi lập luận này chính là thứ bạn đã luyện ở
   [`06-distributed`](../katalon-prep-java/06-distributed-resilience/).
3. **Testcontainers, không phải H2.** H2 không có TimescaleDB, nên test trên H2 sẽ **không chạm** vào
   phần quan trọng nhất của hệ thống. Nói được điều này = biết test cái gì mới có giá trị.

---

## 10. 16 câu follow-up interviewer hay hỏi

<details>
<summary><b>Nhóm A — đào vào chỗ giải pháp cũ hỏng (4 câu)</b></summary>

**A1. "Nếu tôi hỏi 'từ 3h đến 5h chiều thứ Ba tuần trước' thì hệ thống trả lời thế nào?"**
> Phân rã khoảng đó thành các bucket đã tính sẵn: mép đầu và mép cuối lấy từ tầng phút, phần giữa
> lấy từ tầng giờ, rồi cộng lại. Khoảng 2 tiếng thì đọc khoảng 8 dòng. Tôi không bao giờ chạm raw
> data cho truy vấn này — và đây chính là chỗ mà một counter lũy kế duy nhất không làm được.

**A2. "Vì sao không cập nhật thống kê ngay lúc insert cho đơn giản?"**
> Hai lý do. Một là scale: mọi request phải tranh cùng một dòng, write path bị serialize, thêm
> instance cũng không giúp. Hai là quan trọng hơn — một counter cập nhật tại chỗ chỉ giữ **trạng
> thái hiện tại**, thông tin thời gian bị mất, nên không trả lời được truy vấn theo khoảng. Tôi vẫn
> đếm sẵn, nhưng đếm sẵn **theo từng bucket thời gian**, và làm bất đồng bộ.

**A3. "Lock có gì sai? `UPDATE SET c = c + 1` là atomic mà."**
> Atomic thì đúng, nhưng atomic đảm bảo *đúng*, không đảm bảo *nhanh*. Vấn đề là contention: 3000
> transaction/giây tranh một hot row sẽ xếp hàng, và đó là nghẽn không mở rộng ngang được. Tôi vẫn
> dùng lock, nhưng ở read path cho single-flight cache — nơi nó chặn một truy vấn tốn kém chứ không
> chặn mọi lượt ghi.

**A4. "Nếu chỉ cần 24h qua thôi thì giải pháp cũ có đủ không?"**
> Đủ cho đúng một câu hỏi đó. Nhưng đề bài có vế "hoặc theo khoảng thời gian bất kỳ", và hai vế này
> cần hai cấu trúc dữ liệu khác nhau nếu làm theo cách cũ — một cửa sổ trượt có reset và một chuỗi
> lũy kế không reset. Bucket theo thời gian trả lời được **cả hai** bằng một cấu trúc.
</details>

<details>
<summary><b>Nhóm B — đúng đắn và độ tin cậy (5 câu)</b></summary>

**B1. "Client retry, làm sao không đếm trùng?"**
> `event_id` do client sinh deterministic, là primary key của bảng raw cùng với timestamp, insert
> bằng `ON CONFLICT DO NOTHING`. Rollup thì không phải increment mà là `count(*)` chạy lại rồi ghi
> đè, nên chạy lại bao nhiêu lần cũng ra cùng kết quả. Idempotency nằm ở tầng raw, không ở tầng
> counter.

**B2. "Consumer chết giữa chừng thì sao?"**
> Kafka offset chưa commit → đọc lại từ offset cũ → một số event insert lần hai → `ON CONFLICT` nuốt
> → không lệch. Nếu rollup đã chạy cho bucket đó rồi thì lần refresh kế tiếp trong `start_offset`
> tính lại và ghi đè con số đúng.

**B3. "Làm sao anh biết số liệu đang trả cho khách là đúng?"**
> Có job đối soát mỗi giờ: `SUM` trên rollup so với `COUNT(*)` trên raw cho 2 giờ gần nhất. Lệch
> khác 0 thì alert và tự refresh lại bucket. Tôi không tin nó đúng, tôi đo.

**B4. "Redis chết mất counter thì sao?"**
> Redis chỉ là đường nóng phục vụ câu "24h qua", không phải nguồn sự thật. Mất thì dựng lại từ
> Postgres trong vài giây. Trong lúc đó `/stats` vẫn trả lời được, chỉ chậm hơn — degradation, không
> phải outage.

**B5. "Event đến trễ 3 tiếng thì sao?"**
> Cửa sổ tự động tính lại của tôi là 30 phút nên nó không tự đúng. Nhưng raw vẫn giữ 7 ngày, nên tôi
> gọi refresh thủ công cho khoảng đó là số liệu đúng lại. Quá 7 ngày thì raw đã bị retention xoá →
> từ chối và **tăng một counter riêng**, để nếu client hỏng thì tôi thấy chứ không mất im lặng.
</details>

<details>
<summary><b>Nhóm C — scale và đánh đổi (4 câu)</b></summary>

**C1. "Nếu tải tăng 100× thì đổi gì?"**
> 330k event/s thì một node Postgres không đủ. Chuyển sang Kafka + stream processor + ClickHouse.
> Nhưng kiến trúc logic **không đổi** — vẫn là ghi tách khỏi tính, vẫn bucket theo event time, vẫn
> rollup nhiều tầng. Chỉ đổi công cụ hiện thực. Và tôi sẽ không làm việc đó ở tải 3.3k event/s.

**C2. "Vì sao không dùng Kafka ngay từ đầu?"**
> Vì tải hiện tại là 167 request/giây. Kafka thêm ba thứ phải vận hành để đổi lấy công suất không
> dùng tới. Tôi có ngưỡng cụ thể để chuyển: 50k event/s bền vững, hoặc cần breakdown theo key với
> cardinality trên 10k, hoặc cần replay quá 7 ngày, hoặc có nhiều consumer độc lập cần cùng luồng
> này.

**C3. "Cần breakdown theo từng key thì sao?"**
> Đó là câu hỏi tôi hỏi ngay từ đầu, vì nó đổi kiến trúc. Với 100k key thì rollup theo phút sinh 576
> triệu dòng/ngày — nhiều hơn cả dữ liệu gốc, tức pre-aggregation phản tác dụng. Ba lối: giữ top-K
> bằng Count-Min Sketch, hoặc chỉ breakdown ở tầng giờ trở lên, hoặc chuyển sang columnar store.
> Tôi sẽ hỏi thực tế người dùng có đọc quá 50 key một lúc không — thường là không.

**C4. "Multi-tenant, một tenant gửi 10× thì tenant khác có bị ảnh hưởng?"**
> `tenant_id` là partition key ở Kafka và là cột nhóm trong rollup, nên dữ liệu tách sẵn. Chống ảnh
> hưởng chéo cần thêm: quota cứng theo tenant ở gateway, hàng đợi có trọng số (fair queueing) thay
> vì FIFO chung, và giới hạn tài nguyên cho consumer pool.
</details>

<details>
<summary><b>Nhóm D — chi tiết dễ bị bỏ sót (3 câu)</b></summary>

**D1. "Tính tỉ lệ thành công thế nào?"**
> Lưu tử số và mẫu số riêng, chia lúc đọc. **Không bao giờ lưu tỉ lệ**, vì tỉ lệ không cộng được —
> trung bình của hai tỉ lệ không phải tỉ lệ của tổng. Cùng lý do đó tôi không lưu `avg` hay `p95` ở
> tầng rollup.

**D2. "Truy vấn 'hôm nay' theo giờ Việt Nam thì sao?"**
> Bucket ngày cắt theo UTC nên lệch 7 tiếng. Hoặc dùng bucket có timezone của Timescale, hoặc chỉ
> rollup tới tầng giờ rồi ghép ngày ở tầng query. Tôi chọn cách hai nếu có nhiều timezone, vì một
> tầng giờ phục vụ được mọi timezone thay vì phải nhân bản tầng ngày cho từng cái.

**D3. "Xoá dữ liệu cũ thế nào?"**
> `DROP CHUNK`, không phải `DELETE`. `DELETE` 2 tỉ dòng sinh dead tuple làm autovacuum ngộp và bảng
> bloat. `DROP CHUNK` là unlink file, O(1). Partition theo thời gian không chỉ để query nhanh — nó
> để **xoá được**.
</details>

---

## 11. Kịch bản trình bày 3 phút

Nếu gặp lại bài này (hoặc biến thể: đếm click, đếm test run, đếm API call), nói theo thứ tự này.
**Đừng vẽ ngay** — 40 giây đầu là hỏi và ước lượng.

```text
0:00-0:25   HỎI (không vẽ gì cả)
            "Mỗi request bao nhiêu cặp key-value? Và cần tổng hay breakdown theo từng key?"
            → giả định 20 cặp, chỉ cần tổng. Nếu breakdown thì tôi sẽ nói chỗ khác.

0:25-0:45   ƯỚC LƯỢNG (viết 3 số lên bảng)
            "10k req/phút × 20 = 200k event/phút = 3.3k event/s.
             288 triệu event/ngày, ~17 GB raw/ngày.
             Nên: không bao giờ được quét raw để trả lời truy vấn."

0:45-1:30   Ý CHÍNH — vẽ 3 hộp
            "Ba nguyên tắc.
             Một: ghi tách khỏi tính. API chỉ append raw rồi trả 202, không lock, không aggregate
                  đồng bộ.
             Hai: đếm sẵn theo BUCKET THỜI GIAN, không phải một counter tổng. Vì đề bài hỏi
                  khoảng thời gian bất kỳ, mà một counter lũy kế thì thông tin thời gian đã mất.
             Ba: raw là nguồn sự thật, rollup là dẫn xuất — nên luôn tính lại được, sửa được,
                 và đối soát được."

1:30-2:10   TRẢ LỜI TRUY VẤN
            "Rollup ba tầng: phút, giờ, ngày. Hỏi khoảng bất kỳ thì phân rã: mép lấy tầng mịn,
             giữa lấy tầng thô. Truy vấn một năm đọc tối đa khoảng 500 dòng — chi phí bị chặn
             trên, không phụ thuộc tải ghi.
             Riêng câu '24h qua' là câu nóng nhất nên tôi cache bằng Redis hash theo giờ:
             25 lần đọc trong một pipeline, dưới 2ms."

2:10-2:40   HAI CHI TIẾT GHI ĐIỂM (chọn 2 trong 4, đừng nói hết)
            • "Tôi bucket theo EVENT time chứ không phải ingest time, và cửa sổ tính lại là 30
               phút — nên event đến trễ trong 30 phút tự đúng, không cần code thêm."
            • "Tôi lưu count chứ không lưu tỉ lệ, vì tỉ lệ không cộng được."
            • "Idempotency đặt ở tầng raw bằng event_id + ON CONFLICT DO NOTHING, không đặt ở
               counter — vì INCR atomic nhưng không idempotent."
            • "Có job đối soát mỗi giờ so rollup với COUNT(*) trên raw. Lệch thì alert và tự sửa."

2:40-3:00   RIGHT-SIZING (câu kết mạnh nhất)
            "Với 3.3k event/s tôi sẽ KHÔNG dùng Kafka — một node TimescaleDB thừa sức và ít hơn
             ba hệ thống phải vận hành. Ngưỡng tôi sẽ chuyển: 50k event/s bền vững, hoặc cần
             breakdown theo key cardinality cao, hoặc có nhiều consumer độc lập cùng cần luồng này."
```

**Bốn câu, nếu chỉ nhớ được bốn câu:**

1. *"Ghi tách khỏi tính — API append rồi trả 202, aggregation chạy async."*
2. *"Đếm theo bucket thời gian, không phải một counter tổng — vì phải trả lời khoảng bất kỳ."*
3. *"Raw là sự thật, rollup là dẫn xuất — nên tính lại được và đối soát được."*
4. *"Tải này chưa cần Kafka, và đây là ngưỡng cụ thể để chuyển."*

---

## 12. Ranh giới trung thực

Đọc mục này trước khi vào phòng. Nói quá một chi tiết ở đây là mất nhiều hơn được.

| Điều | Trạng thái | Được nói gì |
|---|---|---|
| Kiến trúc §4-§7 | Chuẩn ngành, lập luận vững | ✅ Nói tự tin |
| Con số §3 (3.3k event/s, 17 GB/ngày) | **Tính từ giả định 20 cặp/request** | ✅ Nói — nhưng **nói rõ là giả định**, và nói luôn là bạn sẽ hỏi con số thật |
| Demo §8 | **Thiết kế, CHƯA CHẠY** | ⚠️ Nói *"tôi sẽ dựng thế này"*, **không** nói *"tôi đã đo được"* cho tới khi chạy thật |
| Bảng đo ở §8 bước 6 | Ô trống | ⚠️ Chạy rồi mới điền. Số bịa là rủi ro lớn nhất trong cả tài liệu này |
| Ngưỡng "50k event/s" (A→C) | **Ước lượng bậc độ lớn**, không phải benchmark của bạn | 🟡 Nói kèm *"bậc độ lớn, tôi sẽ đo trước khi quyết"* |
| Bloom filter 12 MB cho 10M key, p=1% | Công thức chuẩn, chưa đo thực tế | 🟡 Là toán, không phải đo — nói vậy nếu bị đào |
| `rewriteBatchedStatements` ~10× | Con số hay gặp trong tài liệu MySQL/JDBC, **bạn chưa tự đo** | 🟡 Nói *"nếu không bật thì batch không thật sự batch"* — an toàn hơn nói con số |
| TimescaleDB cagg-trên-cagg | Có từ Timescale 2.9 | ✅ Đúng, nhưng kiểm phiên bản trước khi demo |
| Tỉ lệ fake key 15% ở loadgen | Do bạn đặt cho demo, **không phải số của Katalon** | ✅ Nói rõ là tham số demo |

**Điều đáng giá nhất khi kể lại buổi phỏng vấn cũ** — nếu có vòng sau và họ nhắc lại bài này:

> *"Lần trước tôi trả lời là aggregate ngay lúc insert trong lock rồi đọc bản ghi cuối. Về sau tôi
> nhận ra chỗ hỏng: một counter lũy kế chỉ giữ trạng thái hiện tại, nên không trả lời được vế
> 'khoảng thời gian bất kỳ' của đề — mà đó lại là yêu cầu khó nhất. Ý đúng là đếm sẵn, nhưng phải
> đếm sẵn theo từng bucket thời gian và làm bất đồng bộ, với raw là nguồn sự thật để rollup luôn
> tính lại được."*

Thừa nhận thẳng rồi trình bày bản sửa **mạnh hơn** là trả lời đúng ngay từ đầu — nó cho thấy bạn
học được từ phản hồi, đúng thứ họ đang tuyển ở level Principal.

---

## Liên quan trong workspace

| Tài liệu | Liên quan chỗ nào |
|---|---|
| [03 — Real-time Analytics Dashboard](03-realtime-analytics-dashboard.md) | Cùng bài toán, phát biểu dưới dạng "Real-time Test Analytics Dashboard" |
| [05 — Hệ thống thật all-in-one trên AWS](05-he-thong-that-allinone-aws.md) | Hạ tầng AWS thật của bạn — ECS/ALB/autoscaling để đối chiếu khi nói về scale |
| [katalon-prep-common/03-system-design.md](../katalon-prep-common/03-system-design.md) | Quy trình 45 phút chuẩn cho mọi bài system design |
| [katalon-prep-java/06-distributed-resilience/](../katalon-prep-java/06-distributed-resilience/) | at-least-once → idempotent, DLQ, retry+jitter — code chạy được |
| [katalon-prep-java/05-postgres-depth/](../katalon-prep-java/05-postgres-depth/) | `EXPLAIN`, index, partition — nền cho §6.1 và §6.11 |
| [AI-STACK-INTERVIEW-ANSWERS.md](../AI-STACK-INTERVIEW-ANSWERS.md) | Cùng một kiểu lập luận: đo thay vì tin, và biết khi nào **chưa** cần công cụ mạnh |
