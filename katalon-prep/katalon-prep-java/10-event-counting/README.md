# Module 10 — Event counting: từ cách bị bác tới cách tốt nhất, có code chạy thật

```bash
mvn -pl 10-event-counting test    # 20 test, PostgreSQL 16 thật (zonky embedded, không cần Docker), khoảng một phút
```

> **Đề:** bài phỏng vấn thật ở Katalon (Principal), xem [04 — Event Counting 10k/phút](../../katalon-system-design/04-event-counting-10k.md).
> Module này giải lại đề **theo từng bước**: bốn cách, ba cách bị bác và vì sao, rồi dựng cách tốt nhất thành một
> service Spring Boot 3 + PostgreSQL 16 có test chứng minh từng tính chất. Video dạy file này:
> [bộ chen ngang Katalon](../../../java-system-design/video-katalon/00-ke-hoach-va-lich-su.md) (giọng Emma).
>
> Câu nối tiếp *"10M thì sao, 100M thì sao"* ở [07](../../katalon-system-design/07-event-counting-10m-100m.md);
> pipeline Kafka cho mức 10M ở [module 09](../09-kafka-pipeline/README.md).

---

## Mục lục

- [1. Đề bài và sáu tiêu chí chấm](#1-đề-bài-và-sáu-tiêu-chí-chấm)
- [2. Bốn cách giải, ba cách bị bác](#2-bốn-cách-giải-ba-cách-bị-bác)
- [3. Dựng hệ thống theo cách 4](#3-dựng-hệ-thống-theo-cách-4)
- [4. Đọc code theo thứ tự](#4-đọc-code-theo-thứ-tự)
- [5. Test chứng minh gì — kết quả chạy thật](#5-test-chứng-minh-gì--kết-quả-chạy-thật)
- [6. Chạy như một service](#6-chạy-như-một-service)
- [7. Khi tải lên 10M và 100M request/phút](#7-khi-tải-lên-10m-và-100m-requestphút)
- [8. Ranh giới trung thực](#8-ranh-giới-trung-thực)

---

## 1. Đề bài và sáu tiêu chí chấm

> ~**10.000 request/phút**. Mỗi request mang một **list key-value**, value là `true`/`false`, key có thể là
> **key giả**. Bất kỳ lúc nào cũng phải trả lời được: bao nhiêu `true`, `false`, `fake key` — **trong 24 giờ qua**
> hoặc **theo một khoảng thời gian bất kỳ**.

**Hợp đồng vào/ra** (giả định đã hỏi lại interviewer, [04 §2](../../katalon-system-design/04-event-counting-10k.md#2-trước-khi-vẽ-6-câu-hỏi-phải-hỏi-interviewer)):

```text
POST /events  {"tenantId":"t1","batchId":"b-42","ts":"2026-08-29T09:58:10Z",
               "items":[{"key":"checkout_v2","value":true}, {"key":"nope","value":true}, ...]}   → 202
GET  /stats?tenant=t1&from=2026-08-26T03:17:30Z&to=2026-08-29T11:42:10Z
     → {"trueCount":…, "falseCount":…, "fakeKey":…, "malformed":…, "successRate":…}
```

Ước lượng: 20 cặp/request → **3.333 event/s**, **288 triệu event/ngày**, ~17 GB raw/ngày.

**Sáu tiêu chí** — mỗi cách giải phải qua cả sáu; cách nào trượt một tiêu chí "phải có" là bị bác:

| # | Tiêu chí | Vì sao bắt buộc |
|---|---|---|
| T1 | Trả lời **khoảng thời gian bất kỳ** `[from, to)` | Vế khó nhất của đề |
| T2 | Đọc **rẻ và ổn định**, không phụ thuộc lượng dữ liệu | "Bất kỳ lúc nào" = dashboard bấm liên tục |
| T3 | Client **retry không đếm trùng** | Mọi hệ thống có retry là at-least-once |
| T4 | Event **đến trễ** vào đúng khung giờ của nó | Client gom batch rồi gửi, mạng chậm |
| T5 | Số sai **sửa được, chứng minh được** | Không có raw thì không đối soát được |
| T6 | Ghi **không xếp hàng** sau một lock chung | Tải tăng thì hot row là trần |

---

## 2. Bốn cách giải, ba cách bị bác

### 2.1 Cách 1 — khoá và một tổng chạy ("đọc bản ghi cuối")

Đây là câu trả lời tại chỗ trong buổi phỏng vấn: *"nhận request, lock, cộng vào bảng tổng; khi hỏi thì đọc bản ghi
cuối"*. Code: [`LockedTotalCounter`](src/main/java/com/prep/counting/rejected/LockedTotalCounter.java).

```sql
SELECT 1 FROM naive_total WHERE tenant_id = ? FOR UPDATE;          -- mọi request xếp hàng ở đây
UPDATE naive_total SET true_cnt = true_cnt + ?, false_cnt = false_cnt + ?, ... WHERE tenant_id = ?;
```

| Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|
| T1 khoảng bất kỳ | ❌ **chí mạng** | Một tổng chạy không còn thời gian trong nó: chỉ trả lời "từ đầu tới giờ" |
| T3 retry | ❌ | Test: hai batch rồi retry một batch → bản ghi cuối là **60**, đúng là 40 |
| T4 trễ | ❌ | Không có khung giờ nào để đặt event vào |
| T5 sửa được | ❌ | Không có raw |
| T6 lock | ❌ | Đo 7 lần: **1.080–1.367 batch/s** với 16 luồng, chậm 2,4–4,4 lần cách 4 (§5) |

**Biến thể "lưu snapshot rồi trừ"** (`tổng(t2) − tổng(t1)`): chỉ đúng khi tổng không bao giờ reset — mà "24 giờ qua"
lại cần reset; độ phân giải bằng chu kỳ snapshot; retry vẫn cộng trùng. Không cứu được.

**Bác vì T1.** Lock (T6) là lý do thứ hai, không phải thứ nhất: hơn 1.000 batch/s vẫn gấp ~6 lần 167 request/s của đề.

### 2.2 Cách 2 — lưu mọi event, đếm lúc hỏi

Ghi mỗi cặp key-value một dòng (idempotent bằng khoá chính), hỏi thì `COUNT(*) ... GROUP BY outcome`. Code:
[`RawScanStats`](src/main/java/com/prep/counting/rejected/RawScanStats.java).

| Tiêu chí | Kết quả |
|---|---|
| T1, T3, T4, T5, T6 | ✅ cả năm — đây là **nửa đúng** của lời giải |
| T2 đọc rẻ | ❌ "24 giờ qua" = đếm **288 triệu dòng** mỗi lần dashboard tải lại; "một năm" = 105 tỷ |

Test `approach2CountsEveryRowTheBestReadsBuckets`: một khoảng 5 giờ 25 phút, cách 2 đếm **32.460** dòng raw; cách 4
đọc **337** dòng bucket + 60 dòng raw ở mép, cùng con số.

**Bác vì T2, nhưng giữ lại raw**: nó là nguồn sự thật của cách 4. Vì luôn đúng, test dùng nó làm *oracle*.

### 2.3 Cách 3 — counter theo phút, cộng ngay lúc ghi

Sửa lỗi chí mạng của cách 1 bằng cách đếm **theo bucket thời gian**: mỗi request cộng vào 4 dòng của phút đó. Redis
`HINCRBY` theo giờ là cùng thiết kế, chỉ đổi chỗ lưu. Code: [`SyncMinuteCounter`](src/main/java/com/prep/counting/rejected/SyncMinuteCounter.java).

```sql
INSERT INTO sync_agg_1m (tenant_id, bucket, outcome, cnt) VALUES (?, date_bin('1 minute', ?, origin), ?, ?)
ON CONFLICT (tenant_id, bucket, outcome) DO UPDATE SET cnt = sync_agg_1m.cnt + EXCLUDED.cnt;
```

| Tiêu chí | Kết quả |
|---|---|
| T1 khoảng bất kỳ | ✅ (theo phút) — test: khoảng 09:30–10:30 khớp oracle |
| T2 đọc rẻ | ✅ |
| T4 trễ | ✅ nếu bucket theo event time |
| T3 retry | ❌ `cnt + n` **atomic nhưng không idempotent**: test retry một batch → 2.420, đúng là 2.400 |
| T5 sửa được | ❌ không có raw: không biết đã sai, không tính lại được |
| T6 lock | 🟡 đo 7 lần: **4.094–5.986 batch/s**, không lần nào chậm hơn cách 4 — **tốc độ không phải lý do bác** |

**Bác vì T3 + T5.** Bài học: *atomic* đảm bảo không mất cập nhật, không đảm bảo không cộng hai lần.

### 2.4 Cách 4 — raw + rollup tính lại + phân rã khoảng (cách được chọn)

Lấy nửa đúng của cách 2 (raw là sự thật, idempotent) và nửa đúng của cách 3 (đếm sẵn theo bucket), nối bằng một quy
tắc: **bucket được TÍNH LẠI từ nguồn rồi GHI ĐÈ, không bao giờ cộng dồn.**

```text
POST /events ─► IngestService: kiểm tra, phân loại 4 outcome, INSERT raw … ON CONFLICT DO NOTHING ─► 202
                                   │ raw_events (partition theo ngày, event time) ← NGUỒN SỰ THẬT
                                   ▼
             RollupJob mỗi phút: tính lại 30 phút gần nhất, DELETE + INSERT trong 1 transaction
                    agg_1m (từ raw) → agg_1h (từ agg_1m) → agg_1d (từ agg_1h);  sealed_until = đầu phút hiện tại
                                   ▼
GET /stats ─► RangeDecomposer: [from, to) → mép < 1 phút + phút + giờ + ngày + đuôi chưa seal ─► cộng lại
             Reconciler mỗi giờ: COUNT(raw) = SUM(agg_1m) từng giờ? lệch → báo + tính lại giờ đó
```

### 2.5 Bảng chấm cả bốn cách

| | T1 khoảng | T2 đọc rẻ | T3 retry | T4 trễ | T5 sửa được | T6 ghi | Kết luận |
|---|---|---|---|---|---|---|---|
| 1. Lock + tổng chạy | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | Bác (T1) |
| 2. Raw, đếm lúc hỏi | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | Bác (T2) — giữ raw |
| 3. Counter phút `+n` | ✅ | ✅ | ❌ | ✅ | ❌ | ✅ | Bác (T3, T5) |
| **4. Raw + rollup ghi đè** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | **Chọn** |

**Right-sizing:** ở 3.333 event/s, cách 4 chạy trên **một** Postgres. Chưa cần Kafka. Ngưỡng chuyển: >50k event/s bền
vững, cần breakdown theo key với cardinality cao, cần replay quá 7 ngày, hoặc nhiều consumer độc lập cùng cần luồng
event ([04 §7](../../katalon-system-design/04-event-counting-10k.md#7-chọn-tech-stack--3-phương-án-và-ngưỡng-chuyển)).

---

## 3. Dựng hệ thống theo cách 4

### 3.1 Bảng và partition

[`schema.sql`](src/main/resources/schema.sql) — Postgres 16 thuần, không cần TimescaleDB:

| Bảng | Khoá | Vai trò |
|---|---|---|
| `raw_events` | `(event_id, ts)`, partition theo **ngày UTC** của `ts` | Nguồn sự thật, giữ 7 ngày |
| `agg_1m`, `agg_1h`, `agg_1d` | `(tenant_id, bucket, outcome)` | Dẫn xuất, tính lại được; giữ 30 ngày / 1 năm / mãi |
| `rollup_state` | 1 dòng | `sealed_until`: trước mốc này đã rollup xong |
| `valid_keys` | `key_name` | Registry key hợp lệ; app giữ bản copy trong RAM, nạp lại mỗi phút |

`event_id` là UUID **tất định** từ `(tenant, batchId, vị trí)`: retry cùng batch → cùng id → `ON CONFLICT DO NOTHING`.
Khoá chính phải chứa `ts` vì bảng partition theo `ts` — nên `ts` phải là event time do client gửi, không được là `now()`.

### 3.2 Đường ghi

[`IngestService`](src/main/java/com/prep/counting/ingest/IngestService.java), theo thứ tự:

1. Kiểm tra: có `tenantId`, `batchId`, `ts`; 1–1.000 item (chặn payload khổng lồ trước khi chạm DB).
2. `ts` cũ hơn 7 ngày (raw đã xoá) → **422** và tăng `rejectedTooLate` — không bao giờ bỏ im lặng. `ts` ở tương lai quá
   5 phút → 400. Cũ hơn 30 phút → vẫn nhận, tăng `lateBeyondWindow` (đối soát sẽ sửa bucket của nó).
3. Phân loại từng item ([`EventClassifier`](src/main/java/com/prep/counting/domain/EventClassifier.java)): key trước,
   value sau; `null`, `1`, `"yes"` là `MALFORMED`, không gộp vào `false`.
4. **Một** câu `INSERT … VALUES (…),(…) … ON CONFLICT DO NOTHING` cho cả batch: một round trip, trả số dòng mới.
5. Trả **202** `{received, inserted, duplicates}`: đã lưu, chưa đếm.

Không lock, không counter, không aggregate ở đường ghi.

### 3.3 Rollup job: tính lại rồi ghi đè

[`RollupJob`](src/main/java/com/prep/counting/rollup/RollupJob.java), chạy mỗi phút:

```sql
-- trong MỘT transaction, cho cửa sổ [now − 30 phút, đầu phút hiện tại)
DELETE FROM agg_1m WHERE bucket >= :from AND bucket < :to;
INSERT INTO agg_1m (tenant_id, bucket, outcome, cnt)
SELECT tenant_id, date_bin('1 minute', ts, TIMESTAMPTZ '2000-01-01 00:00:00+00'), outcome, count(*)
FROM raw_events WHERE ts >= :from AND ts < :to GROUP BY 1, 2, 3;
-- giờ từ phút, ngày từ giờ: cùng mẫu; rồi
UPDATE rollup_state SET sealed_until = GREATEST(sealed_until, :to);
```

- **Ghi đè, không cộng dồn** → chạy lại bao nhiêu lần cũng ra cùng số (T3 ở tầng rollup).
- **Cửa sổ 30 phút** → event trễ ≤ 30 phút tự vào đúng bucket ở lần chạy sau (T4). Test: event 09:58 tới lúc 10:05:30
  → bucket 09:58 là 0 trước lần chạy kế, 20 sau lần chạy kế.
- **Không seal phút đang chạy** (`to` = đầu phút hiện tại); phần sau `sealed_until` đọc thẳng raw.
- `date_bin` với gốc UTC cố định: không phụ thuộc time zone của session (`date_trunc` trên `timestamptz` thì có).

### 3.4 Đường đọc: phân rã khoảng bất kỳ

[`RangeDecomposer`](src/main/java/com/prep/counting/query/RangeDecomposer.java): đi từ `from`, **mỗi bước** lấy bucket thô
nhất bắt đầu đúng ở đây và còn vừa, gộp với mảnh trước nếu cùng tầng. Mép lẻ < 1 phút và phần sau `sealed_until` đọc raw.

```text
2026-08-26 03:17:30 → 2026-08-29 11:42:10
RAW 30 s │ MINUTE 42 │ HOUR 20 │ DAY 2 │ HOUR 11 │ MINUTE 42 │ RAW 10 s   = 84 phút + 31 giờ + 2 ngày
```

Chặn trên: ≤ 2 mảnh raw, ≤ 118 bucket phút, ≤ 46 bucket giờ, + 1 bucket mỗi ngày trọn → khoảng một năm đọc tối đa
**529 bucket** (nhân 4 outcome). Test thử 20.000 khoảng ngẫu nhiên tới một năm: không hở, không chồng, nhiều nhất 507
bucket. [`StatsService`](src/main/java/com/prep/counting/query/StatsService.java) cộng các mảnh và tính `successRate` lúc
đọc — **không lưu tỉ lệ**, vì tỉ lệ không cộng được qua bucket.

> **Lỗi đã sửa ở 04 §6.3.** Bản Python cũ có vòng lặp trong đi tiếp *cùng tầng* tới hết khoảng: ví dụ trên đọc 4.824
> bucket phút. Kết quả vẫn đúng nên khó thấy. Test `theWorkedExampleFromDoc04` khoá đúng ví dụ này.

### 3.5 Đối soát và event trễ

[`Reconciler`](src/main/java/com/prep/counting/rollup/Reconciler.java), mỗi giờ, cho từng giờ đã seal trong 24 giờ qua:
`COUNT(*)` trên raw so với `SUM(cnt)` trên `agg_1m`. Lệch → ghi `reconcile_drift`, tính lại đúng giờ đó, kiểm lại.

| Event trễ | Ai sửa | Test |
|---|---|---|
| ≤ 30 phút | Lần rollup kế tiếp | `aLateEventInsideTheWindowMovesToItsBucketOnTheNextRun` |
| 30 phút – 24 giờ | Đối soát hằng giờ | `aBatchLaterThanTheWindowIsRepairedByTheHourlyReconcile`: 1.000 → 1.020 |
| 24 giờ – 7 ngày | `RollupJob.refresh(from, to)` gọi tay (backfill) | — |
| > 7 ngày | Từ chối 422, đếm `rejectedTooLate` | `tooOldIsRefusedAndCountedNeverDroppedSilently` |

### 3.6 Retention

`PartitionManager.dropBefore(ngày)` = `DROP TABLE raw_events_YYYYMMDD`: O(1), không dead tuple, không vacuum. `DELETE`
288 triệu dòng mỗi ngày thì autovacuum ngộp. Test: xoá 2 ngày raw cũ, truy vấn ngày cũ vẫn trả 20 từ `agg_1h`/`agg_1d`.

### 3.7 Bốn job nền

[`Jobs`](src/main/java/com/prep/counting/Jobs.java): nạp lại registry (1 phút), rollup (1 phút), đối soát (hằng giờ,
phút 5), partition (tạo trước 7 ngày qua tới ngày mai, xoá ngày cũ hơn 7 ngày). Nhiều instance thì chỉ một instance chạy
job (hoặc bọc bằng `pg_try_advisory_lock`).

---

## 4. Đọc code theo thứ tự

| # | File | Đọc để thấy |
|---|---|---|
| 1 | [`schema.sql`](src/main/resources/schema.sql) | Raw là nguồn sự thật; ba tầng dẫn xuất; hai bảng của cách bị bác |
| 2 | [`EventBatch`](src/main/java/com/prep/counting/domain/EventBatch.java), [`EventClassifier`](src/main/java/com/prep/counting/domain/EventClassifier.java) | Hợp đồng vào; bốn outcome |
| 3 | [`IngestService`](src/main/java/com/prep/counting/ingest/IngestService.java) | `eventId` tất định; một INSERT nhiều dòng; 202 |
| 4 | [`RollupJob`](src/main/java/com/prep/counting/rollup/RollupJob.java) | `replace`: DELETE + INSERT … SELECT; `refreshRecent` và seal |
| 5 | [`RangeDecomposer`](src/main/java/com/prep/counting/query/RangeDecomposer.java), [`StatsService`](src/main/java/com/prep/counting/query/StatsService.java) | Phân rã và cộng; tỉ lệ tính lúc đọc |
| 6 | [`Reconciler`](src/main/java/com/prep/counting/rollup/Reconciler.java) | `FULL OUTER JOIN` raw với rollup |
| 7 | [`rejected/`](src/main/java/com/prep/counting/rejected/) | Ba cách bị bác, viết thật để test được |
| 8 | [`EventController`](src/main/java/com/prep/counting/api/EventController.java), [`CountingConfig`](src/main/java/com/prep/counting/CountingConfig.java), [`Jobs`](src/main/java/com/prep/counting/Jobs.java) | Lớp Spring mỏng: lớp lõi không phụ thuộc Spring nên test dựng thẳng được |

---

## 5. Test chứng minh gì — kết quả chạy thật

Chạy ngày 14/10/2026 trong sandbox Linux, JDK 21, PostgreSQL 16.4 embedded. **20/20 test qua.**

| Test | Chứng minh | Số in ra |
|---|---|---|
| `EventClassifierTest` (3) | 4 outcome; key kiểm trước value | — |
| `IngestIdempotencyTest` (4) | Retry cùng batch: `inserted=20` rồi `inserted=0, duplicates=20`; quá 7 ngày → 422 có đếm; tương lai, quá 1.000 item → 400 | |
| `RangeDecomposerTest` (3) | Ví dụ của 04: 84 + 31 + 2; 20.000 khoảng ngẫu nhiên đúng và bị chặn | nhiều nhất 507 bucket |
| `RollupAndQueryTest` (3) | 3 giờ tải (3.600 batch, 72.000 event, 10% trễ tới 5 phút, rollup mỗi phút): **500 khoảng ngẫu nhiên khớp `COUNT(*)` trên raw**; trễ trong cửa sổ; trễ 3 giờ được đối soát sửa | nhiều nhất 436 dòng bucket mỗi truy vấn; cả 3 giờ: 12 dòng, 0 dòng raw |
| `ReconcilerTest` (1) | Cố tình cộng thừa 5 vào một bucket → phát hiện `drift = −5` → sửa → 0 | |
| `RetentionTest` (1) | `DROP` 2 partition cũ; rollup ngày cũ vẫn trả lời | |
| `RejectedApproachesTest` (3) | Cách 1: retry → 60/40; cách 3: khoảng đúng nhưng retry → 2.420/2.400; cách 2: 32.460 dòng raw so với 337 + 60 | |
| `ContentionDemoTest` (1) | 4.000 batch, 16 luồng ghi, cùng một tenant và một phút | 7 lần chạy: cách 1 **1.080–1.367**, cách 3 **4.094–5.986**, cách 4 **3.334–5.488** batch/s |
| `ApiTest` (1) | Qua Spring thật: 202, retry → `duplicates`, `/stats` đủ 4 số và `successRate` 0,5, 422 khi quá cũ | |

**Đọc số đo tranh chấp cho đúng:** bảy lần chạy trên một máy, Postgres embedded cấu hình mặc định; số dao động giữa các
lần. Kết luận dùng được là **thứ tự và tỉ lệ** (cách 1 chậm 2,4–4,4 lần vì mọi request xếp hàng sau một row lock),
không phải con số tuyệt đối.
Cách 3 không chậm hơn cách 4 ở đây — nó bị bác vì đúng đắn, không vì tốc độ.

---

## 6. Chạy như một service

```bash
docker run -d --name pg -e POSTGRES_PASSWORD=dev -e POSTGRES_DB=stats -p 5432:5432 postgres:16
mvn -pl 10-event-counting spring-boot:run          # schema.sql tự chạy; job rollup mỗi phút
psql -h localhost -U postgres stats -c "INSERT INTO valid_keys VALUES ('checkout_v2'), ('dark_mode')"
curl -X POST localhost:8080/events -H 'Content-Type: application/json' -d '{"tenantId":"t1","batchId":"b-1",
  "ts":"'$(date -u +%Y-%m-%dT%H:%M:%SZ)'","items":[{"key":"checkout_v2","value":true},{"key":"nope","value":true}]}'
curl "localhost:8080/stats?tenant=t1&from=2026-01-01T00:00:00Z&to=2030-01-01T00:00:00Z"
```

Registry nạp lại mỗi phút: key thêm vào `valid_keys` có hiệu lực sau tối đa 60 giây.

---

## 7. Khi tải lên 10M và 100M request/phút

Đường đọc **không đổi**: rollup lớn theo số tenant × outcome × bucket, không theo số event. Mọi thay đổi ở đường ghi
([07 §4](../../katalon-system-design/07-event-counting-10m-100m.md#4-cái-gì-vỡ-trước--đi-từng-bước--10)):

| Tải | Cái vỡ trước | Thay bằng | Code |
|---|---|---|---|
| 10k/phút (3,3k event/s) | — | Module này, một Postgres | `10-event-counting` |
| 10M/phút (3,3M event/s) | Một Postgres không ghi nổi mỗi event một dòng | Kafka key theo `batch_id`; consumer theo lô; đánh dấu batch + cộng số trong **một** transaction; raw đóng gói ra object storage | [`09-kafka-pipeline`](../09-kafka-pipeline/README.md) |
| 100M/phút (33M event/s) | Một region, mạng, tiền | Cell theo region rồi cộng toàn cục; khử trùng theo `(client_id, seq)`; sketch chỉ cho distinct/top-K | [07 §6](../../katalon-system-design/07-event-counting-10m-100m.md#6-kiến-trúc-ở-100m-requestphút) |

Ba ý của module này đi theo lên mọi mức tải: **raw (hoặc log) là sự thật**, **đếm theo event time vào bucket**, **ghi
đè hoặc khử trùng, không cộng mù**.

---

## 8. Ranh giới trung thực

| Điều | Trạng thái |
|---|---|
| Đúng đắn (idempotent, khoảng bất kỳ, trễ, đối soát, retention) | ✅ Test trên Postgres 16 thật |
| Số đo tranh chấp §5 | 🟡 Bảy lần chạy, một máy, embedded, số dao động. Nói *"cách 1 chậm vài lần vì xếp hàng sau row lock"*, đừng đọc thuộc số |
| Tải 10k request/phút liên tục | ⚠️ Chưa chạy load test kéo dài; test ghi 72.000 event theo thời gian giả |
| Nhiều instance chạy job cùng lúc | ⚠️ Chưa có khoá; tài liệu ghi cách làm (`pg_try_advisory_lock`) |
| Cache "24 giờ qua" (Redis, single-flight) | ⚠️ Chưa code; thiết kế ở [04 §6.9](../../katalon-system-design/04-event-counting-10k.md#69-cache-và-thundering-herd) |
| Breakdown theo từng key | ❌ Ngoài phạm vi; xem [04 §6.7](../../katalon-system-design/04-event-counting-10k.md#67-cardinality--cái-bẫy-chính-của-bài-này) |
