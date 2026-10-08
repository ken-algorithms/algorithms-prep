# 07 — Event counting khi tải lên 10M và 100M request/phút

> **Câu follow-up phải chuẩn bị** sau bài [04 — Event Counting 10k/phút](04-event-counting-10k.md): *"Nếu là 10
> triệu request/phút thì sao? 100 triệu thì sao?"*. Đây là câu "tải tăng 10 lần thì cái gì vỡ trước" của
> vòng Principal, chỉ khác là tăng 1.000 và 10.000 lần.
>
> Họ bài: **A** (đếm & tổng hợp theo thời gian) **+ C** (thu thập luồng sự kiện), xem
> [README §2](README.md#2-bảy-họ-bài--chữ-ký-nhận-dạng-lõi-giải-pháp-bẫy-kinh-điển). Giữ nguyên mọi giả định của
> 04: **20 cặp key-value mỗi request**, 4 outcome (`true`, `false`, `fake_key`, `malformed`), hỏi theo khoảng
> thời gian bất kỳ, cần số **chính xác**.
>
> Video chen ngang dạy file này: [java-system-design/video-katalon](../../java-system-design/video-katalon/00-ke-hoach-va-lich-su.md)
> (Tom hỏi, Emma trả lời). Nối với lộ trình: [§9](#9-bản-đồ-nối-với-giai-đoạn-1-và-giai-đoạn-2).

---

## Mục lục

- [0. Đọc trong 60 giây](#0-đọc-trong-60-giây)
- [1. Đề mở rộng — hỏi lại gì trước khi trả lời](#1-đề-mở-rộng--hỏi-lại-gì-trước-khi-trả-lời)
- [2. Thang ước lượng: 10k → 100M request/phút](#2-thang-ước-lượng-10k--100m-requestphút)
- [3. Điều không đổi: đường đọc](#3-điều-không-đổi-đường-đọc)
- [4. Cái gì vỡ trước — đi từng bước × 10](#4-cái-gì-vỡ-trước--đi-từng-bước--10)
- [5. Kiến trúc ở 10M request/phút](#5-kiến-trúc-ở-10m-requestphút)
- [6. Kiến trúc ở 100M request/phút](#6-kiến-trúc-ở-100m-requestphút)
- [7. Họ C khi tải lớn — thu thập từ nguồn không kiểm soát](#7-họ-c-khi-tải-lớn--thu-thập-từ-nguồn-không-kiểm-soát)
- [8. Kịch bản trả lời](#8-kịch-bản-trả-lời)
- [9. Bản đồ nối với giai đoạn 1 và giai đoạn 2](#9-bản-đồ-nối-với-giai-đoạn-1-và-giai-đoạn-2)
- [10. Ranh giới trung thực](#10-ranh-giới-trung-thực)

---

## 0. Đọc trong 60 giây

1. **Số trước.** 10M request/phút = **166.667 request/s = 3,3 triệu event/s**. 100M request/phút = **1,67 triệu
   request/s = 33 triệu event/s**. So với bài gốc: × 1.000 và × 10.000.
2. **Đường đọc gần như không đổi.** Bảng rollup lớn theo *số nhóm × số bucket*, không theo số event. Truy vấn
   một năm vẫn đọc khoảng 500 dòng như ở 04. Mọi thay đổi nằm ở **đường ghi**.
3. **10M: không còn lưu mỗi event một dòng trong database nào.** Collector gộp mỗi request thành một bản ghi;
   Kafka chia partition theo `batch_id`; stream processor khử trùng trong một cửa sổ rồi gộp theo event time;
   kết quả được **ghi đè**, không cộng dồn; raw nén vào object storage; một job batch đếm lại để đối soát.
4. **100M: bài toán đổi từ "chọn database" sang "vật lý và tiền".** Mỗi µs CPU trên một request tốn
   **1,7 core**; byte đi qua mạng; state khử trùng; một region là một bán kính sự cố. Lời giải: chia
   **cell theo region** rồi gộp toàn cục (số đếm cộng được, tiền thì không); khử trùng theo
   `(client_id, seq)`; sketch chỉ dùng cho *distinct* và *top-K*, **số tổng vẫn đếm chính xác**.
5. **Trả lời bằng đúng khung × 10 năm câu** của giai đoạn 1 (số mới → vỡ trước → sửa và đánh đổi → vỡ tiếp →
   chưa làm gì), xem [§8](#8-kịch-bản-trả-lời).

---

## 1. Đề mở rộng — hỏi lại gì trước khi trả lời

Interviewer nói *"10M thì sao?"*. Đừng vẽ ngay. Chốt cách hiểu trong một câu: *"Tôi hiểu là 10 triệu
request mỗi phút, vẫn 20 cặp key-value mỗi request, vẫn cần số chính xác theo khoảng bất kỳ"*. Rồi hỏi
những câu **đổi kiến trúc** ở quy mô này (khác bộ 6 câu của [04 §2](04-event-counting-10k.md#2-trước-khi-vẽ-6-câu-hỏi-phải-hỏi-interviewer)):

| # | Câu hỏi | Vì sao đổi kiến trúc ở 10M–100M |
|---|---|---|
| 1 | Vẫn 20 item mỗi request? | Đơn vị tải. 20 item → 3,3M event/s; 200 item → 33M event/s ở cùng 10M request/phút |
| 2 | Bao nhiêu client gửi, mỗi client gửi bao nhiêu request/phút? | Vài nghìn agent gửi dày → gộp ở client rất lợi, khử trùng theo `(client_id, seq)` rẻ. Hàng chục triệu thiết bị gửi thưa → không gộp được ở client |
| 3 | Client ở một region hay khắp thế giới? | Khắp thế giới → **cell theo region**, luật dữ liệu theo vùng (khách EU ở EU) |
| 4 | Số liệu để làm dashboard hay để tính tiền? | Dashboard: số tạm từ stream là đủ. Tính tiền: chỉ dùng số **đã đối soát** từ batch |
| 5 | Có cần breakdown theo từng key không, bao nhiêu key? | Có → cần OLAP cột (ClickHouse); xem [§3.2](#32-cardinality-nổ-hay-không-là-tương-đối) |
| 6 | Giữ raw bao lâu, có phải audit từng event không? | Ở 100M, lưu và chép raw là một trong ba khoản tiền lớn nhất (cùng CPU mỗi request, [§6.3](#63-ba-thứ-phải-tính-tiền-ra-được)); định dạng và thời gian giữ quyết định chi phí |

> **Câu nên nói:** *"Ở 10 triệu request/phút, tôi vẫn hỏi đơn vị tải trước, vì 20 item hay 200 item mỗi
> request là chênh nhau một bậc độ lớn. Và tôi hỏi số này để làm dashboard hay để tính tiền, vì hai mục
> đích đó cho hai mức chính xác khác nhau."*

---

## 2. Thang ước lượng: 10k → 100M request/phút

Giả định (nói to khi phỏng vấn): **20 item/request**; request JSON khoảng **1 KB**; raw **60 B/event** như
[04 §3](04-event-counting-10k.md#3-ước-lượng-tải--con-số-quyết-định-kiến-trúc).

| | 10k/phút (04) | 100k/phút | 1M/phút | **10M/phút** | **100M/phút** |
|---|---:|---:|---:|---:|---:|
| So với 04 | × 1 | × 10 | × 100 | **× 1.000** | **× 10.000** |
| Request/s | 167 | 1.667 | 16.667 | **166.667** | **1.666.667** |
| Event/s | 3.333 | 33.333 | 333.333 | **3,3 triệu** | **33 triệu** |
| Event/ngày | 288 triệu | 2,88 tỷ | 28,8 tỷ | **288 tỷ** | **2.880 tỷ** |
| Raw/ngày ở 60 B/event | 17 GB | 173 GB | 1,7 TB | **17 TB** | **173 TB** |
| Băng thông vào (~1 KB/request) | 0,17 MB/s | 1,7 MB/s | 17 MB/s | **167 MB/s ≈ 1,3 Gbit/s** | **1,67 GB/s ≈ 13 Gbit/s** |
| Dòng rollup 1 phút/ngày (1 tenant, 4 outcome) | 5.760 | 5.760 | 5.760 | **5.760** | **5.760** |

```text
request/s  = request/phút ÷ 60                 10.000.000 ÷ 60      = 166.667
event/s    = request/s × 20                    166.667 × 20         = 3.333.333
event/ngày = request/phút × 20 × 1.440         10M × 20 × 1.440     = 288 tỷ
raw/ngày   = event/ngày × 60 B                 288 tỷ × 60 B        = 17,3 TB
```

**Đỉnh × 3** (giống hệ số ngày đặc biệt của giai đoạn 1): 10M/phút có lúc lên ~500.000 request/s, 10M event/s,
~4 Gbit/s; 100M/phút lên ~5M request/s, 100M event/s, ~40 Gbit/s.

**Dòng cuối của bảng là luận điểm của cả file:** tải tăng 10.000 lần nhưng bảng rollup tổng không lớn thêm một
dòng nào.

---

## 3. Điều không đổi: đường đọc

### 3.1 Rollup lớn theo số nhóm, không theo số event

```text
số dòng rollup = số tenant × số outcome × số bucket        (không có "event/s" trong công thức)

1 tenant, tầng phút:     1 × 4 × 1.440      =      5.760 dòng/ngày   ở MỌI mức tải
10.000 tenant, tầng phút: 10.000 × 4 × 1.440 = 57,6 triệu dòng/ngày  ← vẫn không phụ thuộc tải
```

Vì vậy thuật toán phân rã khoảng ([04 §6.3](04-event-counting-10k.md#63-thuật-toán-phân-rã-khoảng-thời-gian-bất-kỳ)),
cache version-bump và single-flight ([04 §6.9](04-event-counting-10k.md#69-cache-và-thundering-herd)) giữ
nguyên ở 10M và 100M. Truy vấn một năm bất kỳ vẫn đọc tối đa khoảng 530 dòng mỗi tenant.

**Thứ thay đổi ở phía đọc** chỉ là số người xem dashboard (QPS đọc) — chữa bằng cache, không phải bằng kiến
trúc ghi mới. Và khi gộp toàn cục ở 100M, câu trả lời phải nói rõ nó **tạm** hay **đã chốt**
(`as_of`, `watermark`, `is_final` như [03](03-realtime-analytics-dashboard.md)).

### 3.2 Cardinality nổ hay không là tương đối

[04 §6.7](04-event-counting-10k.md#67-cardinality--cái-bẫy-chính-của-bài-này) kết luận: breakdown theo 100.000
key thì rollup **không giảm** được gì. Kết luận đó đúng **ở 10k/phút**, và đổi ngược ở 10M/phút:

```text
hệ số gộp = số event mỗi bucket ÷ số nhóm khác nhau mỗi bucket
số nhóm   = 100.000 key × 4 outcome = 400.000 nhóm mỗi phút

10k/phút:  200.000 event/phút    ÷ 400.000 → ≤ 0,5   → gần như mỗi event một dòng: rollup vô ích
10M/phút:  200 triệu event/phút  ÷ 400.000 → 500     → rollup theo key giảm 500 lần: đáng làm
```

**Bài học:** "cardinality explosion" là tỉ lệ giữa số nhóm và số event trong một bucket, không phải một con
số cố định. Ở 10M/phút, rollup theo key lại có lợi, nhưng 400.000 nhóm × 1.440 phút = **576 triệu dòng/ngày
mỗi tenant** → đó là việc của kho cột (ClickHouse), không phải Postgres.

---

## 4. Cái gì vỡ trước — đi từng bước × 10

Mỗi bước trả lời đúng năm câu của [khung × 10 giai đoạn 1](../../java-system-design/02-ve-he-thong-100k-1m-10m.md#5-câu-hỏi-tải-tăng-10-lần-thì-sao--cách-trả-lời).
Các ngưỡng là **bậc độ lớn** (Postgres primary ~30–50k dòng/s, Redis một node ~100k thao tác/s, như
[04 §7](04-event-counting-10k.md#7-chọn-tech-stack--3-phương-án-và-ngưỡng-chuyển) và
[giai đoạn 1 §1.4](../../java-system-design/02-ve-he-thong-100k-1m-10m.md#14-từ-rps-ra-số-máy--ba-con-số-nhẩm)), phải đo lại.

### 4.1 × 10 — 100k request/phút

```text
1. Số mới:    1.667 request/s, 33.333 event/s, 2,88 tỷ event/ngày, 173 GB raw/ngày.
2. Vỡ trước:  đường ghi của MỘT Postgres primary — mỗi event một dòng + một mục index unique + WAL,
              cùng lúc continuous aggregate đọc lại raw. 33k dòng/s chạm ngưỡng ~30–50k của một primary.
3. Sửa:       gộp ngay trong API: mỗi request 20 item → MỘT dòng (batch_id, ts, tenant, 4 bộ đếm, mảng item
              đóng gói). Ghi giảm 20 lần, còn ~1.700 dòng/s. Rollup = sum(4 bộ đếm) thay vì count(*).
              Đánh đổi là: khử trùng theo request (batch_id) thay vì theo từng event; muốn breakdown theo key
              phải giải nén mảng item.
4. Vỡ tiếp:   dung lượng raw nằm trong database giao dịch; mỗi lần failover database là API không ghi được.
5. Chưa làm:  Kafka, stream processor — sau khi ghi giảm 20 lần, một node còn dư gấp nhiều lần.
```

### 4.2 × 100 — 1M request/phút

```text
1. Số mới:    16.667 request/s, 333.333 event/s, 28,8 tỷ event/ngày, 1,7 TB raw/ngày (60 B/event).
2. Vỡ trước:  API ghi ĐỒNG BỘ vào database. Database chậm hay failover vài chục giây là API phải giữ
              request hoặc trả lỗi; client retry dồn lại thành một cơn bão sau đó.
3. Sửa:       đặt Kafka giữa API và phần tính (acks=all, replication factor 3, min.insync.replicas 2);
              API trả 202 sau khi Kafka xác nhận; stream processor gộp theo (tenant, phút, outcome) rồi
              ghi rollup; raw đi tiếp sang object storage (Parquet) để đối soát. Đây là câu C1 của 04.
              Đánh đổi là: thêm 2–3 hệ thống phải vận hành, số liệu trở thành eventual (trễ vài giây).
4. Vỡ tiếp:   partition nóng nếu key = tenant_id; consumer chậm thì rebalance storm (P20).
5. Chưa làm:  ClickHouse — rollup tổng vẫn vừa Postgres; chỉ cần kho cột khi có breakdown theo key.
```

### 4.3 × 1.000 — 10M request/phút

```text
1. Số mới:    166.667 request/s (đỉnh ~500k), 3,3 triệu event/s, 288 tỷ event/ngày, ~1,3 Gbit/s vào.
              Raw 17 TB/ngày nếu 60 B/event; ~1,8 TB/ngày nếu đóng gói theo request (§5.3).
2. Vỡ trước:  (a) key Kafka = tenant_id: tenant lớn nhất, giả sử 20% tải = 667k event/s, dồn vào MỘT
              partition; một consumer không theo kịp, lag tăng mãi.
              (b) khử trùng bằng unique index trên từng event hết chỗ chạy: không còn bảng raw dạng dòng.
3. Sửa:       key = batch_id; collector gộp mỗi request thành một bản ghi; stream processor khử trùng
              batch_id trong cửa sổ 15 phút rồi gộp tumbling 1 phút theo event time; kết quả ghi đè
              (§5.4). Đánh đổi là: mất thứ tự theo tenant (đếm không cần thứ tự), state khử trùng vài GB.
4. Vỡ tiếp:   CPU của collector (166.667 request/s × chi phí mỗi request), state khử trùng, tiền lưu raw.
5. Chưa làm:  cell đa region, gộp ở client, sketch cho số tổng.
```

### 4.4 × 10.000 — 100M request/phút

```text
1. Số mới:    1,67 triệu request/s (đỉnh ~5M), 33 triệu event/s, 2.880 tỷ event/ngày, ~13 Gbit/s vào
              (đỉnh ~40). Raw 173 TB/ngày nếu 60 B/event; ~18 TB/ngày nếu đóng gói.
2. Vỡ trước:  không còn là một thành phần, mà là tiền và vật lý:
              (a) CPU mỗi request: 1 µs = 1,7 core. P01 (new ObjectMapper() mỗi request, +14 µs) tốn ~24 core
                  và hơn 30 GB/s rác cho GC.
              (b) byte đi qua mạng: chép raw qua AZ, qua region.
              (c) state khử trùng: 1,5 tỷ batch_id trong 15 phút.
              (d) một region chết là mọi khách mất số liệu cùng lúc.
3. Sửa:       cell theo region, mỗi cell chạy nguyên pipeline 10M; chỉ số đếm đã gộp mới đi toàn cục;
              khử trùng theo (client_id, seq); định dạng nhị phân + nén trên dây; raw đóng gói, ở lại region.
              Đánh đổi là: truy vấn toàn cục chỉ "chốt" khi region chậm nhất qua watermark.
4. Vỡ tiếp:   chi phí lưu raw và chi phí đối soát (đếm lại hàng chục TB mỗi ngày).
5. Chưa làm:  exactly-once xuyên region, một cụm Kafka toàn cầu, sketch hay sampling cho số tổng.
```

> **Nhận ra quy luật của bốn bước:** mỗi × 10 đẩy **ranh giới gộp** về gần nguồn hơn một nấc: database
> (10k) → API (100k) → stream processor (1M) → collector + khử trùng theo partition (10M) → region, thậm chí
> client (100M). Nói được câu này là nói được cả thang.

---

## 5. Kiến trúc ở 10M request/phút

### 5.1 Sơ đồ

```text
 Client SDK  ──HTTPS keep-alive──►  Edge LB + gateway  ──►  Collector (stateless, ~28 pod)
 {batch_id, ts, items[20]}          quota theo tenant         1. validate schema, đồng hồ client
 retry: backoff + jitter            429 + Retry-After         2. phân loại 20 item (registry trong RAM)
                                                              3. gộp: 1 request → 1 bản ghi
                                                                 {batch_id, tenant, ts, đếm[4], item đóng gói}
                                                              4. produce acks=all ──► trả 202
                                                                         │
                                       Kafka topic "requests"   key = batch_id, 64–128 partition, RF 3
                                         │                                              │
                                         ▼                                              ▼
              Stream processor (Flink / Kafka Streams)                       Sink connector
               a. khử trùng batch_id: state RocksDB, giữ 15 phút event time   → object storage, Parquet
               b. gộp cục bộ theo (tenant, phút, outcome) trong mỗi task        tenant / ngày / giờ
               c. gộp toàn cục theo (tenant, phút): tumbling 1 phút,                     │
                  watermark 2 phút, cho phép trễ 30 phút                                 ▼
               d. trễ hơn nữa → topic "late" + bộ đếm, không im lặng             Batch đối soát mỗi giờ
               e. ghi ĐÈ: cnt = GREATEST(cnt, mới)                                đếm lại, khử trùng toàn cục
                                         │                                       → drift, sửa, is_final
                                         ▼                                              │
              Rollup store: Postgres/Timescale (chỉ tổng) hoặc ClickHouse (theo key) ◄──┘
                                         ▼
              Query API: phân rã khoảng (04 §6.3) + cache version-bump + single-flight (04 §6.9)
```

### 5.2 Quyết định và đánh đổi

| Quyết định | Chọn | Vì sao (số) | Đánh đổi là… |
|---|---|---|---|
| Đơn vị đi qua Kafka | **1 bản ghi mỗi request**: bộ đếm 4 outcome + item đóng gói | 166.667 bản ghi/s thay vì 3,3M | Muốn soi một event thì vào raw |
| Partition key | **`batch_id`** | Tenant lớn nhất không dồn vào một partition; retry của cùng request rơi vào cùng partition nên khử trùng chỉ cần state cục bộ | Mất thứ tự theo tenant — đếm không cần thứ tự |
| Số partition | **Chọn dư từ đầu** (64–128) | Tăng partition làm `hash(key) mod P` đổi: retry sau lúc đổi rơi sang partition khác, khử trùng hụt | Nhiều partition hơn mức cần, rebalance lâu hơn |
| Khử trùng | State theo `batch_id`, giữ **15 phút theo event time** | 166.667/s × 900 s ≈ **150 triệu khoá ≈ 7–8 GB** (Little's Law) | Retry muộn hơn 15 phút lọt qua → batch đối soát bắt |
| Gộp | **Hai tầng**: cục bộ trong task, rồi toàn cục theo `(tenant, phút)` | Tầng toàn cục chỉ nhận vài số mỗi task mỗi phút → tenant lớn không làm nóng một task | Thêm một lần shuffle trong job |
| Thời gian | Tumbling 1 phút theo **event time**, watermark 2 phút, cho phép trễ 30 phút | Như `start_offset 30 minutes` của 04 | Số "đã chốt" trễ ~2 phút |
| Ghi kết quả | Upsert **ghi đè** `(tenant, phút, outcome)`, `cnt = GREATEST(cnt, mới)` | Replay sau crash và consumer "xác sống" không làm số lùi hay cộng trùng (§5.4) | Sửa số xuống phải đi cột `final_cnt` riêng |
| Raw | Đóng gói theo request, **Parquet nén**, giữ 30 ngày rồi chuyển lớp lạnh | ~1,8 TB/ngày trước khi nén (§5.3) | Đối soát chạy batch, không real-time |
| Đối soát | Batch mỗi giờ đếm lại từ raw, khử trùng `batch_id` toàn cục | `reconcile_drift` phải bằng 0 | Hai đường tính một con số → phải dùng chung định nghĩa và code phân loại |
| Rollup store | Postgres/Timescale nếu chỉ tổng; **ClickHouse** nếu breakdown theo key | Tổng: 5.760 dòng/ngày/tenant ở tầng phút | Thêm một hệ thống khi có breakdown |

### 5.3 Raw: đừng cho mỗi event một UUID

60 B/event của 04 phần lớn là `event_id` 16 byte ngẫu nhiên, không nén được. Ở 10M, cho **mỗi request** một
`batch_id`, còn event là `(batch_id, vị trí)`. Đúng như demo của 04 sinh `uuid5(prefix:i)`: không cần lưu id
từng event vì tính lại được.

```text
một request đóng gói: batch_id 16 B + ts 8 B + tenant 4 B + 20 × (key_ref 4 B + outcome 1 B) = 128 B
                    = 6,4 B/event, trước khi nén cột

10M/phút:   14,4 tỷ request/ngày × 128 B = 1,8 TB/ngày    (so với 17 TB ở 60 B/event)
100M/phút:  144 tỷ request/ngày  × 128 B = 18 TB/ngày     (so với 173 TB)
```

Parquet + zstd còn giảm thêm vì `key_ref` lặp nhiều và `outcome` chỉ có 4 giá trị — **phải đo trên dữ liệu
thật** trước khi nói con số.

### 5.4 Exactly-once về hiệu quả từ các mảnh at-least-once

Không có tầng nào exactly-once. Ghép ba thứ lại thì kết quả đếm đúng một lần:

1. **Client** gửi lại **cùng `batch_id`** khi retry (quy tắc idempotency key của giai đoạn 1). Thời gian giữ
   khoá khử trùng (15 phút) phải **dài hơn** cửa sổ retry của SDK (ví dụ 10 phút), nếu không thì mất tính
   idempotent một cách im lặng.
2. **Stream processor** khử trùng `batch_id` trước khi gộp. Vì key Kafka là `batch_id`, bản gốc và bản retry
   nằm cùng partition, state khử trùng là state cục bộ của task. Hết hạn theo **event time**, không theo đồng
   hồ máy, để replay cho cùng kết quả.
3. **Sink** ghi đè bằng giá trị lớn hơn:

```sql
INSERT INTO agg_1m (tenant_id, bucket, outcome, cnt)
VALUES ($1, $2, $3, $4)
ON CONFLICT (tenant_id, bucket, outcome)
DO UPDATE SET cnt = GREATEST(agg_1m.cnt, EXCLUDED.cnt);
```

**Vì sao `GREATEST` đúng:** sau khi khử trùng, số đếm của một nhóm chỉ **tăng dần** theo tiến độ đọc log. Job
khôi phục từ checkpoint thì phát lại số **nhỏ hơn** rồi tăng lên lại; consumer "xác sống" (bị GC pause, đã mất
partition nhưng chưa biết) chỉ cầm số **cũ hơn**. `GREATEST` bỏ qua cả hai. Đây chính là **fencing token** của
lab 10A giai đoạn 2, với token là con số đếm — nơi lưu trữ từ chối giá trị cũ, không cần client tự biết mình
đã cũ.

**Giới hạn phải nói:** `GREATEST` không sửa được số **quá cao** (ví dụ retry đến sau khi khoá khử trùng hết
hạn). Việc đó là của batch đối soát: nó ghi `final_cnt` riêng, đọc dùng `COALESCE(final_cnt, cnt)`.

**Event trễ:** trong 30 phút thì cửa sổ được tính lại và phát lại số lớn hơn (`GREATEST` nhận). Trễ hơn thì đi
topic `late` và tăng bộ đếm `events_late`; raw vẫn có nên batch đối soát đếm đủ. Không bỏ im lặng
([04 §6.5](04-event-counting-10k.md#65-late-arriving-data-và-watermark)).

### 5.5 Collector: chi phí mỗi request nhân với 166.667

Collector không chạm database nên rẻ hơn nhiều so với pod của giai đoạn 1 (4–5 ms CPU mỗi request vì có JPA và
query). Giả định **0,2 ms CPU mỗi request** (TLS, HTTP, parse JSON 1 KB, phân loại 20 item, produce) — phải đo:

```text
10M/phút:   166.667 × 0,2 ms = 33 core → ở mức 60% CPU: ~56 vCPU ≈ 28 pod 2 vCPU
100M/phút:  1.666.667 × 0,2 ms = 333 core → ~556 vCPU ≈ 280 pod
mỗi 1 µs thêm vào một request: 0,17 core ở 10M, 1,7 core ở 100M, 0,0002 core ở 10k
```

**Ba lỗi của Track P trở thành tiền thật ở đây** (số đo ở [01 §2](../../java-system-design/01-java-code-cham-duoi-tai-cao.md#2-nhóm-1--lãng-phí-cpu-và-rác-mỗi-request-p01p07)):

| Lỗi | Số đo (01) | Ở 10M/phút | Ở 100M/phút |
|---|---|---|---|
| P01 `new ObjectMapper()` mỗi request | 14,6 µs → 0,48 µs; 19,6 KB → 688 B rác | ~2,4 core, ~3 GB/s rác | ~24 core, hơn 30 GB/s rác |
| P04 dùng exception cho key sai | 942 ns → 8,8 ns mỗi lần kiểm (50% input sai) | ~3 core | ~31 core |
| P05 đếm bằng `Map<Outcome, Integer>` | boxing + một node mỗi lần `merge` | Đếm 4 outcome thì dùng `int[4]` theo `ordinal()` | như trái |

Ở 10k/phút, cả ba lỗi cộng lại chưa tới 0,01 core — không ai thấy. **Cùng một dòng code, khác nhau ở số request
nhân vào.**

---

## 6. Kiến trúc ở 100M request/phút

### 6.1 Sơ đồ: cell theo region

```text
        GeoDNS / anycast: client → region gần nhất;  client_id → cell nhà (cố định, như user → cell ở L4)
 ┌──────────── Cell EU ─────────────┐ ┌──────────── Cell US ─────────────┐ ┌───── Cell APAC ─────┐
 │ edge → collector → Kafka →       │ │ (nguyên pipeline 10M của §5)      │ │ (như hai cell kia)   │
 │ stream → rollup của cell         │ │                                   │ │                      │
 │ raw → object storage tại EU      │ │ raw ở lại US                      │ │ raw ở lại APAC       │
 └───────────────┬──────────────────┘ └───────────────┬───────────────────┘ └──────────┬───────────┘
                 │  chỉ số đếm đã gộp: vài dòng mỗi tenant mỗi phút              │
                 └─────────────────────────────┬───────────────────────────────────────┘
                                               ▼
                    Rollup toàn cục = SUM theo cell;  "đã chốt" khi mọi cell đã qua watermark
                                               ▼
                    Query API toàn cục (phân rã khoảng như 04)
```

### 6.2 Năm thay đổi so với 10M

| # | Thay đổi | Vì sao (số) | Đánh đổi là… |
|---|---|---|---|
| 1 | **Cell theo region**, gộp toàn cục bằng cộng | Một region không còn là điểm chết; raw không đi qua region; số đếm cộng được (giao hoán, kết hợp) — mỗi cell giữ phần của mình như CRDT G-counter | Số toàn cục "đã chốt" phải chờ cell chậm nhất; client đổi cell khi sự cố có thể gửi trùng qua hai cell → đối soát toàn cục bắt |
| 2 | Khử trùng theo **`(client_id, seq)`** thay vì `batch_id` | State theo **số client đang hoạt động**, không theo số request trong cửa sổ (1,5 tỷ khoá). Cùng cách idempotent producer của Kafka chống trùng bằng (producer id, sequence) | Client phải giữ số thứ tự; client khởi động lại thì đổi epoch; một client quá lớn phải chia `client_id#stream` |
| 3 | **Dây nhị phân + nén**; gộp ở client nếu client ít và gửi dày | JSON 1 KB × 1,67M/s = 13 Gbit/s; Protobuf + nén giảm vài lần. Client gộp 10 giây rồi gửi `{client_id, seq, đếm[4]}` thì số request giảm đúng bằng số request gộp | Đổi hợp đồng API; muốn audit từng event thì client phải gửi raw riêng, chậm và rẻ |
| 4 | **Raw đóng gói, nén cột, ở lại region**, giữ nóng ngắn | 173 TB/ngày (60 B/event) → ~18 TB/ngày (128 B/request) trước khi nén | Đối soát chỉ chạy trong region; câu hỏi mới về raw cũ phải đọc từ lớp lạnh, chậm |
| 5 | **Sketch** cho *distinct* và *top-K*; số tổng giữ chính xác | HyperLogLog ~12 KB cho sai số chuẩn ~0,81% và cộng được qua bucket, qua cell; Count-Min Sketch cho key nóng nhất | Sketch là xấp xỉ: không dùng để tính tiền |

### 6.3 Ba thứ phải tính tiền ra được

**Mạng qua AZ.** Kafka tự chạy trên máy ảo chép mỗi byte sang 2 AZ khác. Với raw chưa đóng gói (173 TB/ngày)
và giá niêm yết khoảng 0,01 USD/GB mỗi chiều giữa hai AZ của AWS: `173.000 GB × 2 bản chép × 0,02 USD ≈ 6.900
USD/ngày` chỉ riêng phí mạng nội bộ. Đóng gói và nén **trước khi** gửi đi là bắt buộc, không phải tối ưu.

**Lưu raw.** 30 ngày raw ở 173 TB/ngày là ~5,2 PB; ở 18 TB/ngày là ~540 TB. Với giá niêm yết S3 Standard
khoảng 0,021–0,023 USD/GB-tháng, chênh nhau cỡ **110 nghìn** và **12 nghìn** USD mỗi tháng.

**CPU mỗi request.** 1 µs = 1,7 core ([§5.5](#55-collector-chi-phí-mỗi-request-nhân-với-166667)). Đây là chỗ
Track P của giai đoạn 1 trả tiền.

> Giá thay đổi theo thời gian và theo dịch vụ (Kafka được quản lý có thể không tính phí chép giữa broker). Đây
> là phép nhân để ra **bậc độ lớn** trên bảng trắng, không phải báo giá.

### 6.4 Cái KHÔNG làm ở 100M, và vì sao

| Không làm | Vì sao |
|---|---|
| Exactly-once xuyên region (giao dịch phân tán) | Số đếm cộng được nên mỗi cell tự đúng là đủ; phần trùng ở ranh giới cell để đối soát bắt. 2PC xuyên region là đúng chỗ giai đoạn 2 dạy *không* dùng |
| Một cụm Kafka toàn cầu | Mọi byte phải bay qua region; một cụm chết là mọi khách chết |
| Sketch hay sampling cho số tổng | Tổng là 4 số nguyên mỗi bucket: đếm chính xác **rẻ** ở mọi quy mô. Sketch dành cho câu hỏi có cardinality cao |
| Mỗi event một dòng trong bất kỳ database nào | 2.880 tỷ dòng/ngày |

> **Đối chiếu họ G:** ví tiền **không** làm active-active được như thế này, vì số dư có bất biến (không âm)
> nên cần một nơi ghi duy nhất hoặc đồng thuận. Số đếm thì không có bất biến nào giữa các cell — cộng là xong.
> Đây là lý do "đếm" và "tiền" nằm ở hai họ khác nhau ([README](README.md#2-bảy-họ-bài--chữ-ký-nhận-dạng-lõi-giải-pháp-bẫy-kinh-điển), [06](06-race-condition-balance-ledger.md)).

---

## 7. Họ C khi tải lớn — thu thập từ nguồn không kiểm soát

Lõi họ C trong [README](README.md#2-bảy-họ-bài--chữ-ký-nhận-dạng-lõi-giải-pháp-bẫy-kinh-điển): *backpressure +
at-least-once + sink idempotent + partition key giữ thứ tự đúng phạm vi*. Bẫy: *hàng đợi vô hạn; partition
nóng; PII rời client trước khi redact*. Áp vào bài này:

### 7.1 Bảng ba mức tải

| Yếu tố họ C | 10k/phút (04) | 10M/phút | 100M/phút |
|---|---|---|---|
| **Backpressure** | Rate limit ở gateway, hàng đợi có giới hạn, Kafka lag | + quota theo tenant ở edge; buffer producer có giới hạn → `503` thay vì chờ | + token bucket **cục bộ** mỗi collector (chia quota), SDK lùi theo `Retry-After` có jitter, buffer phía client có giới hạn và đếm số bỏ |
| **At-least-once + sink idempotent** | `event_id` + `ON CONFLICT DO NOTHING` | Khử trùng `batch_id` trong state + `GREATEST` | Khử trùng `(client_id, seq)`, state theo số client |
| **Partition key** | `tenant_id` | `batch_id`: đếm không cần thứ tự; retry cùng partition | `client_id` trong từng cell |
| **PII** | — | Key chứa dữ liệu người dùng thì hash/redact **ở client** | + raw ở lại region (luật dữ liệu); chỉ số đếm đi toàn cục |

### 7.2 Backpressure: bốn lớp, lớp nào cũng phải có giới hạn

```text
Client SDK   buffer có giới hạn; đầy thì bỏ cái cũ nhất VÀ tăng bộ đếm client_dropped (gửi kèm lần sau)
             retry: backoff mũ + jitter, tôn trọng Retry-After, bỏ cuộc sau 10 phút (< 15 phút khử trùng)
Edge         quota theo tenant → 429 + Retry-After
Collector    buffer.memory + max.block.ms của Kafka producer có giới hạn → 503, không xếp hàng vô hạn
Kafka        là bộ giảm chấn: consumer chậm thì lag tăng, không mất dữ liệu (retention ≥ thời gian sự cố)
Consumer     max.poll.records theo p99 xử lý, xử lý theo lô, executor có giới hạn (P13, P20)
```

**Rate limit ở 100M:** token bucket Redis + Lua của lab 2 giai đoạn 1 cần một round trip mỗi request →
1,67 triệu thao tác/s ≈ 17 node Redis (~100k thao tác/s mỗi node) chỉ để đếm quota, và mỗi request chờ thêm một
RTT. Thay bằng bucket **trong RAM của từng collector**, mỗi collector giữ một phần quota của tenant, vài giây
cân lại một lần từ bộ đếm trung tâm. Đánh đổi: giới hạn toàn cục thành xấp xỉ, tenant có thể vượt chút ít lúc
cân lại.

### 7.3 Registry key hợp lệ ở 33 triệu lần tra mỗi giây

[04 §6.6](04-event-counting-10k.md#66-phân-loại-fake-key) dùng Bloom filter để loại nhanh key giả. Bloom chỉ
tiết kiệm lượt tra cho key **giả**; mọi key "có thể có" vẫn phải hỏi store. Ở 100M, phần đó là hàng chục
triệu lượt tra mỗi giây — store khi đó lớn hơn cả hệ thống đếm. Nên:

- Giữ **toàn bộ registry trong RAM của collector**, dạng fingerprint 64 bit: 10 triệu key ≈ **80 MB**. Xác
  suất một key giả trùng fingerprint ≈ 10⁷ ÷ 2⁶⁴ ≈ 5 × 10⁻¹³ mỗi lần tra — nhỏ hơn mọi nguồn sai khác.
- Cập nhật bằng một **compacted topic** của Kafka (key = tên key, value = hợp lệ/không + version); collector
  đọc topic đó để giữ bản sao mới.
- Ghi **version registry** vào raw, để batch đối soát phân loại lại đúng theo version khi cần.

### 7.4 Đồng hồ của client

Event time do client gửi là **wall clock** của máy client: có thể lệch, có thể lùi (giai đoạn 2, tuần 10).
Collector ghi thêm thời điểm nhận; event có `ts` ở tương lai quá vài phút hoặc cũ hơn cửa sổ giữ raw thì gắn
cờ và đếm riêng (`events_clock_skew`), không lặng lẽ đếm vào bucket sai.

### 7.5 Metric thêm vào sáu metric của 04

| Metric | Cảnh báo điều gì |
|---|---|
| `consumer_lag_seconds` theo partition | Stream processor không theo kịp; một partition lag riêng = partition nóng |
| `dedup_hits_per_sec` | Tăng đột ngột = client đang retry hàng loạt (thường do edge chậm) |
| `events_late`, `events_clock_skew` | Event bị đẩy sang đường phụ — không được im lặng |
| `reconcile_drift` theo cell | Như 04, nhưng tách theo cell để biết cell nào sai |
| `collector_cpu_us_per_request` | Bắt lỗi kiểu Track P ngay khi deploy: +10 µs ở 100M là +17 core |
| `bytes_per_request` trên dây | Client đổi định dạng hay quên nén |

---

## 8. Kịch bản trả lời

### 8.1 "Nếu là 10M request/phút?" — 2 phút

```text
0:00  Chốt hiểu đề: "10 triệu request/phút, vẫn 20 item/request, vẫn cần số chính xác theo khoảng bất kỳ."
0:10  Số mới: "166 nghìn request/s, 3,3 triệu event/s, 288 tỷ event/ngày, khoảng 1,3 Gbit/s vào."
0:25  Không đổi: "Đường đọc giữ nguyên — rollup lớn theo số nhóm × số bucket, không theo số event.
      Truy vấn một năm vẫn đọc ~500 dòng."
0:40  Vỡ trước: "Postgres không còn giữ mỗi event một dòng được, và nếu key Kafka là tenant thì tenant
      lớn nhất làm nóng một partition."
1:00  Sửa: "Collector gộp mỗi request thành một bản ghi. Kafka key theo batch_id. Stream processor khử
      trùng batch_id trong 15 phút, gộp hai tầng theo event time, cửa sổ 1 phút, watermark 2 phút.
      Sink ghi đè bằng GREATEST nên replay hay consumer cũ không làm sai số. Raw đóng gói vào object
      storage; mỗi giờ một job đếm lại để đối soát."
1:40  Đánh đổi + chưa làm: "Mất thứ tự theo tenant, state khử trùng vài GB, số chốt trễ ~2 phút.
      Tôi chưa chia region, chưa gộp ở client, chưa dùng sketch — chưa cái nào cần ở mức này."
```

### 8.2 "Còn 100M?" — 2 phút

```text
0:00  Số mới: "1,67 triệu request/s, 33 triệu event/s, 13 Gbit/s vào, đỉnh gấp ba."
0:15  Đổi bản chất: "Ở mức này không còn là chọn database. Mỗi micro giây CPU trên một request là
      1,7 core; mỗi byte raw chép qua AZ là tiền; state khử trùng theo batch_id là 1,5 tỷ khoá."
0:40  Sửa:
      "Một — chia cell theo region, mỗi cell là pipeline 10M; chỉ số đếm đã gộp đi toàn cục, vì số đếm
            cộng được.
       Hai — khử trùng theo client_id và số thứ tự, state theo số client chứ không theo số request.
       Ba  — dây nhị phân và nén; nếu client ít mà gửi dày thì gộp ở client.
       Bốn — raw đóng gói, ở lại region: từ 173 xuống khoảng 18 TB mỗi ngày trước khi nén.
       Năm — sketch chỉ cho distinct và top-K; tổng vẫn đếm chính xác vì rẻ."
1:40  Chưa làm: "Không exactly-once xuyên region, không một cụm Kafka toàn cầu. Số toàn cục chỉ chốt khi
      cell chậm nhất qua watermark, và API nói rõ số nào là tạm."
```

### 8.3 Follow-up hay gặp

**"Sao không dùng exactly-once của Kafka/Flink từ đầu đến cuối?"**
> Transaction của Kafka chỉ bao phần nằm trong Kafka. Sink là database thì vẫn cần sink idempotent. Tôi chọn
> at-least-once + khử trùng + ghi đè `GREATEST`: rẻ hơn, và đúng cả khi có consumer "xác sống".

**"Consumer chết giữa một cửa sổ?"**
> Job khôi phục từ checkpoint, đọc lại từ offset của checkpoint, phát lại số nhỏ hơn rồi tăng dần. `GREATEST`
> bỏ qua số nhỏ hơn nên bảng không bao giờ lùi.

**"Tenant lớn nhất chiếm 30% tải?"**
> Key là `batch_id` nên tải của tenant đó trải đều mọi partition. Tầng gộp toàn cục chỉ nhận vài số đã gộp mỗi
> task mỗi phút nên không có task nóng.

**"Retry đến sau khi khoá khử trùng đã hết hạn?"**
> SDK bỏ cuộc sau 10 phút, khoá giữ 15 phút. Phần lọt qua thì đối soát mỗi giờ bắt được, vì batch khử trùng
> trên toàn bộ raw.

**"Cần breakdown theo 100.000 key?"**
> Ở 10M/phút, rollup theo key giảm 500 lần, nên đáng làm — nhưng 576 triệu dòng/ngày mỗi tenant thì để
> ClickHouse giữ. Top-K key nóng nhất thì dùng Count-Min Sketch.

**"Một region chết?"**
> Client chuyển sang cell khác với epoch mới. Số của cell chết đứng ở watermark cuối và được đánh dấu chưa chốt;
> raw của nó còn trong object storage nên đối soát lại được khi region quay về.

**"Chi phí lớn nhất ở 100M là gì?"**
> Không phải database. Là CPU mỗi request ở collector, byte chép qua AZ, và lưu raw. Cả ba đều tính được bằng
> phép nhân trên bảng.

---

## 9. Bản đồ nối với giai đoạn 1 và giai đoạn 2

**Ý chính:** bài này **không cần kiến thức mới**. Giai đoạn 1 cho cách ước lượng, khung × 10 và tư duy chi phí
mỗi request. Giai đoạn 2 cho Kafka, partition, khử trùng, stream processing và fencing. Câu hỏi 10M/100M chỉ
bắt ghép chúng lại dưới áp lực thời gian.

Ký hiệu: `gd1 Ep02` là video Ep02 của giai đoạn 1 (giọng Tom), `gd2 Ep17` là video Ep17 của giai đoạn 2 (giọng
Emma). Video giai đoạn 1 từ Ep07 trở đi **chưa dựng** (đánh dấu *kế hoạch*).

| Trong bài này | Giai đoạn 1 | Giai đoạn 2 |
|---|---|---|
| Thang ước lượng §2: request/s, event/s, TB/ngày, Gbit/s | [10 §1.1 quy đổi](../../java-system-design/10-implement-gd1-nen-tang.md#11-quy-đổi-phải-nhẩm-được), [02 §1](../../java-system-design/02-ve-he-thong-100k-1m-10m.md#1-từ-n-users-ra-ccu-và-rps--công-thức-4-bước) · gd1 Ep01, Ep02, Ep03 | — |
| State khử trùng = tốc độ × thời gian giữ | [Little's Law, 10 §1.3](../../java-system-design/10-implement-gd1-nen-tang.md#13-littles-law--công-thức-dùng-nhiều-nhất-cả-lộ-trình) · gd1 Ep01 | — |
| Năm câu × 10 ở §4 và §8 | [02 §5](../../java-system-design/02-ve-he-thong-100k-1m-10m.md#5-câu-hỏi-tải-tăng-10-lần-thì-sao--cách-trả-lời), thang L0–L4 · gd1 Ep06, Ep25 (*kế hoạch*) | Áp lại cho V6 ở [tuần 5–6](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-56--v6-mini-core-transfer-ở-l2-chủ-nhật-tuần-6-30-phút) · gd2 Ep06 |
| "Chưa làm X vì…": right-sizing | Luật của thang: chỉ lên bậc khi có thứ đang vỡ ([02 §2](../../java-system-design/02-ve-he-thong-100k-1m-10m.md#2-thang-5-bậc--tóm-tắt-trên-một-trang)) · gd1 Ep06 | — |
| Chi phí mỗi request × 1,67M (§5.5) | [Track P nhóm 1, P01–P07](../../java-system-design/01-java-code-cham-duoi-tai-cao.md#2-nhóm-1--lãng-phí-cpu-và-rác-mỗi-request-p01p07) · gd1 Ep04, Ep05 | — |
| Hàng đợi có giới hạn, `503` thay vì chờ | [P13](../../java-system-design/01-java-code-cham-duoi-tai-cao.md#p13--hàng-đợi-không-giới-hạn-khi-quá-tải) | [8.5 backpressure](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#85-retry-dlt-backpressure) · gd2 Ep13 |
| Quota theo tenant, `429` + `Retry-After`, token bucket cục bộ | [Lab 2 rate limiter](../../java-system-design/10-implement-gd1-nen-tang.md#lab-2--rate-limiter-phân-tán-trong-spring-boot-thứ-bảy-34-giờ), [V3](../../java-system-design/10-implement-gd1-nen-tang.md#track-s-tuần-2--v3-chủ-nhật-20-phút) · gd1 Ep08, Ep11 (*kế hoạch*) | — |
| Keep-alive, chi phí bắt tay TLS ở edge | [10 §2.1](../../java-system-design/10-implement-gd1-nen-tang.md#21-đường-đi-của-một-request-https-mới), [§2.2 L4/L7](../../java-system-design/10-implement-gd1-nen-tang.md#22-l4-hay-l7) · gd1 Ep07, Ep08 (*kế hoạch*) | — |
| `batch_id`, khoá giữ lâu hơn cửa sổ retry | [Idempotency key, 10 §2.4](../../java-system-design/10-implement-gd1-nen-tang.md#24-idempotency-key--4-quy-tắc) · gd1 Ep10 (*kế hoạch*) | [7.5 idempotency ở ba tầng](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#75-idempotency-ở-ba-tầng) · gd2 Ep08 |
| RocksDB, ClickHouse: kho ghi nhiều | [10 §3.1 B-tree hay LSM-tree](../../java-system-design/10-implement-gd1-nen-tang.md#31-b-tree-hay-lsm-tree) · gd1 Ep13 (*kế hoạch*) | — |
| Cache version-bump, single-flight, key nóng nhân bản | [10 §4.2 ba bệnh của cache](../../java-system-design/10-implement-gd1-nen-tang.md#42-ba-bệnh-của-cache) · gd1 Ep21 (*kế hoạch*) | — |
| Số tạm hay số chốt (`is_final`) | [10 §4.4 CAP, PACELC](../../java-system-design/10-implement-gd1-nen-tang.md#44-cap-pacelc--nói-trong-30-giây) · gd1 Ep19 (*kế hoạch*) | — |
| Partition key, partition nóng, đổi số partition | — | [5.3 partitioning](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#53-partitioning-và-rebalancing), [8.1 thứ tự chỉ trong một partition](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#81-thứ-tự-chỉ-tồn-tại-trong-một-partition) · gd2 Ep03, Ep12 |
| `acks=all`, RF 3, `min.insync.replicas` 2 | — | [8.2 độ bền ghi](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#82-độ-bền-ghi) · gd2 Ep12 |
| At-least-once + sink idempotent; transaction Kafka không bao database | — | [8.3 ba mức giao nhận](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#83-ba-mức-giao-nhận), [lab 8](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#lab-8--spring-boot--kafka--postgres-thứ-bảy-tuần-8-và-9) · gd2 Ep13, Ep15 |
| Consumer chậm, `max.poll.records`, xử lý theo lô | [P20](../../java-system-design/01-java-code-cham-duoi-tai-cao.md#p20--kafka-consumer-xử-lý-đồng-bộ-từng-message-) | [8.4 consumer group](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#84-consumer-group-rebalance-maxpollintervalms) · gd2 Ep13, Ep16 |
| Tumbling window, event time, watermark, event trễ | — | [8.6 stream processing](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#86-stream-processing--đủ-để-nói-chuyện), đề **ad click aggregation** (cùng họ A) · gd2 Ep17 |
| `GREATEST` như fencing token | — | [10.3 khoá phân tán](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#103-khoá-phân-tán-cho-hiệu-năng-hay-cho-đúng-đắn), [lab 10A](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#lab-10a--fencing-token-thứ-bảy-15-giờ) · gd2 Ep20 |
| Đồng hồ client lệch, event time không đáng tin | — | [10.1 đồng hồ nói dối](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#101-đồng-hồ-nói-dối) · gd2 Ep19 |
| Cell theo region, gộp bằng cộng (CRDT counter) | [L4 cell-based](../../java-system-design/02-ve-he-thong-100k-1m-10m.md#35-l4--vượt-10m-chỉ-cần-nói-được-không-cần-vẽ-chi-tiết) · gd1 Ep25 (*kế hoạch*) | [5.1 multi-leader, CRDT](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#51-ba-mô-hình-replication) · gd2 Ep01 |
| Không 2PC xuyên region | — | [7.1 vì sao không 2PC](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#71-vì-sao-không-2pc-giữa-microservices) · gd2 Ep07 |
| Đếm khác tiền (họ A khác họ G) | [10 §3.3 isolation](../../java-system-design/10-implement-gd1-nen-tang.md#33-isolation-và-các-dị-thường--bảng-phải-thuộc) · gd1 Ep14 (*kế hoạch*) | Sổ cái kép, ví tiền ([7.6](../../java-system-design/20-implement-gd2-du-lieu-phan-tan.md#76-sổ-cái-kép-double-entry)) · gd2 Ep08, Ep11 |

**Thứ tự xem đề xuất cho video chen ngang:** K00–K02 sau giai đoạn 1 (chỉ cần ước lượng, thang × 10, Postgres);
K03–K07 sau giai đoạn 2 (cần Kafka, stream processing, fencing). Chi tiết ở
[kế hoạch bộ video](../../java-system-design/video-katalon/00-ke-hoach-va-lich-su.md).

---

## 10. Ranh giới trung thực

| Điều | Trạng thái | Được nói gì |
|---|---|---|
| Mọi con số §2 | **Tính từ giả định** (20 item/request, ~1 KB/request, 60 B/event) | ✅ Nói, kèm "giả định" và câu hỏi lại đơn vị tải |
| Ngưỡng Postgres 30–50k dòng/s, Redis ~100k thao tác/s | Bậc độ lớn, lấy từ 04 và giai đoạn 1 §1.4, **chưa đo** trong repo này | 🟡 "Bậc độ lớn, tôi sẽ đo trước khi quyết" |
| 0,2 ms CPU mỗi request của collector | **Giả định**, chưa có collector nào được đo | 🟡 Nói phép nhân, không nói như số đo |
| P01, P04 ở §5.5 | Số JMH thật của [01 §2](../../java-system-design/01-java-code-cham-duoi-tai-cao.md#2-nhóm-1--lãng-phí-cpu-và-rác-mỗi-request-p01p07); nhân với request/s là **suy luận** | ✅ Số đo là thật; phép nhân là ước lượng |
| 128 B/request đóng gói | Phép cộng kích thước trường, **chưa** đo nén thật | 🟡 Nói "trước khi nén" |
| Giá mạng qua AZ, giá S3 | Giá niêm yết của AWS tại lúc viết, có thể đã đổi; dịch vụ được quản lý tính khác | 🟡 Chỉ dùng để ra bậc độ lớn |
| Lập luận `GREATEST` | Đúng khi khử trùng **trước** khi gộp và số đếm chỉ tăng; không sửa được số quá cao | ✅ Nói kèm giới hạn và cột `final_cnt` |
| HyperLogLog ~12 KB, sai số ~0,81% | Thông số của bản cài đặt trong Redis | ✅ |
| Kiến trúc 10M, 100M | Thiết kế trên giấy; **không có demo** ở mức tải này trong repo | ⚠️ Không nói "tôi đã chạy" — nói "tôi sẽ dựng và đo thế này" |

---

## Liên quan

| Tài liệu | Liên quan chỗ nào |
|---|---|
| [04 — Event Counting 10k/phút](04-event-counting-10k.md) | Bài gốc: thiết kế tầng 1, phân rã khoảng, idempotency ở raw, ngưỡng chuyển stack |
| [03 — Real-time Analytics Dashboard](03-realtime-analytics-dashboard.md) | `as_of`, `watermark`, `is_final`; Kafka Streams state store; `tenantId#shard` |
| [01 — TrueTest journey mining](01-truetest-journey-mining.md) | Họ C on-domain: SDK, edge collector, key `(tenantId, sessionId)`, PII ở client |
| [06 — Race condition balance ledger](06-race-condition-balance-ledger.md) | Họ G: vì sao tiền không gộp như số đếm |
| [Video chen ngang](../../java-system-design/video-katalon/00-ke-hoach-va-lich-su.md) | 8 video, Tom hỏi, Emma trả lời |
