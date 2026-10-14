# Độ phủ nội dung — video chen ngang Katalon

**92/92 ý chính đã có video.** Bảng sinh bằng `python -m lesson_video coverage` từ `points.yaml` (ý chính lấy từ tài liệu nguồn) và các kịch bản `.yaml`; mỗi ý có chữ bắt buộc (`expect`) mà lệnh `check` đã kiểm là có mặt trong cảnh dạy ý đó. Thời điểm lấy từ `lessons.json` sau khi dựng.

## Theo video

| Video | Dài | Chương | Ý chính |
|---|---:|---:|---:|
| K00 · The Katalon counting question, and six tests every answer must pass | 5:05 | 10 | 6 |
| K01 · Approach 1: lock, add to a running total, read the last record | 4:21 | 11 | 7 |
| K02 · Approach 2: store every event, count when asked | 3:03 | 9 | 5 |
| K03 · Approach 3: per-minute counters, bumped as you write | 4:14 | 11 | 7 |
| K04 · Approach 4: raw truth, rollups that recompute, ranges that decompose | 4:16 | 10 | 6 |
| K05 · Build 1: the tables, the partitions and the write path | 5:06 | 10 | 6 |
| K06 · Build 2: the rollup job — recompute, replace, seal | 4:17 | 9 | 5 |
| K07 · Build 3: the read path, the reconcile, and retention | 5:09 | 12 | 9 |
| K08 · Code 1: the module, the request, and IngestService | 4:01 | 9 | 6 |
| K09 · Code 2: RollupJob, RangeDecomposer and StatsService | 3:08 | 8 | 5 |
| K10 · Code 3: the tests that prove it, and the numbers they printed | 4:18 | 11 | 8 |
| K11 · “What about 10 million a minute?” — step by step to a Kafka pipeline | 6:04 | 11 | 8 |
| K12 · “And 100 million?” — when the problem becomes physics and money | 4:55 | 9 | 6 |
| K13 · The whole answer in three minutes, and a follow-up drill | 5:43 | 11 | 8 |

## Theo mục của tài liệu nguồn

**56/56 mục có video.** Mỗi mục là một heading trong phạm vi của tuần; mục có video khi nằm trong `covers` của video hoặc là nguồn của một ý chính đã dạy.

| | Mục | Video |
|:---:|---|---|
| ✅ | [README · 1. Đề bài và sáu tiêu chí chấm](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#1-đề-bài-và-sáu-tiêu-chí-chấm) | K00 |
| ✅ | [04 · 0. Đề bài và những gì đề bài KHÔNG nói](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#0-đề-bài-và-những-gì-đề-bài-không-nói) | K00 |
| ✅ | [04 · 2. Trước khi vẽ: 6 câu hỏi phải hỏi interviewer](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#2-trước-khi-vẽ-6-câu-hỏi-phải-hỏi-interviewer) | K00 |
| ✅ | [04 · 3. Ước lượng tải — con số quyết định kiến trúc](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#3-ước-lượng-tải--con-số-quyết-định-kiến-trúc) | K00, K02 |
| ✅ | [README · 2.1 Cách 1 — khoá và một tổng chạy ("đọc bản ghi cuối")](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#21-cách-1--khoá-và-một-tổng-chạy-đọc-bản-ghi-cuối) | K01 |
| ✅ | [04 · 1. Vì sao câu trả lời cũ bị bác — 4 lỗ hổng](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#1-vì-sao-câu-trả-lời-cũ-bị-bác--4-lỗ-hổng) | K01 |
| ✅ | [04 · Lỗ hổng #1 — một counter lũy kế không trả lời được range query (chí mạng)](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#lỗ-hổng-1--một-counter-lũy-kế-không-trả-lời-được-range-query-chí-mạng) | K01 |
| ✅ | [04 · Lỗ hổng #2 — lock ở write path là anti-pattern về scale](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#lỗ-hổng-2--lock-ở-write-path-là-anti-pattern-về-scale) | K01 |
| ✅ | [04 · 12. Ranh giới trung thực](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#12-ranh-giới-trung-thực) | K01, K13 |
| ✅ | [README · 2.2 Cách 2 — lưu mọi event, đếm lúc hỏi](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#22-cách-2--lưu-mọi-event-đếm-lúc-hỏi) | K02 |
| ✅ | [04 · 6.1 Data model](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#61-data-model) | K02, K05 |
| ✅ | [README · 2.3 Cách 3 — counter theo phút, cộng ngay lúc ghi](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#23-cách-3--counter-theo-phút-cộng-ngay-lúc-ghi) | K03 |
| ✅ | [04 · 6.4 Idempotency — vì sao counter KHÔNG phải nguồn sự thật](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#64-idempotency--vì-sao-counter-không-phải-nguồn-sự-thật) | K03 |
| ✅ | [04 · Lỗ hổng #3 — trộn nguồn sự thật với cache](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#lỗ-hổng-3--trộn-nguồn-sự-thật-với-cache) | K03 |
| ✅ | [04 · 5. Design tầng 2 — bản production](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#5-design-tầng-2--bản-production) | K03 |
| ✅ | [README · 5. Test chứng minh gì — kết quả chạy thật](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#5-test-chứng-minh-gì--kết-quả-chạy-thật) | K03, K08, K10 |
| ✅ | [README · 2.4 Cách 4 — raw + rollup tính lại + phân rã khoảng (cách được chọn)](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#24-cách-4--raw--rollup-tính-lại--phân-rã-khoảng-cách-được-chọn) | K04 |
| ✅ | [README · 2.5 Bảng chấm cả bốn cách](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#25-bảng-chấm-cả-bốn-cách) | K04 |
| ✅ | [04 · 4. Design tầng 1 — bản tối giản đúng](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#4-design-tầng-1--bản-tối-giản-đúng) | K04 |
| ✅ | [04 · 7. Chọn tech stack — 3 phương án và ngưỡng chuyển](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#7-chọn-tech-stack--3-phương-án-và-ngưỡng-chuyển) | K04 |
| ✅ | [04 · 11. Kịch bản trình bày 3 phút](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#11-kịch-bản-trình-bày-3-phút) | K04, K13 |
| ✅ | [README · 3. Dựng hệ thống theo cách 4](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#3-dựng-hệ-thống-theo-cách-4) | K05 |
| ✅ | [README · 3.1 Bảng và partition](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#31-bảng-và-partition) | K05, K08 |
| ✅ | [README · 3.2 Đường ghi](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#32-đường-ghi) | K05, K08 |
| ✅ | [04 · 6.6 Phân loại "fake key"](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#66-phân-loại-fake-key) | K05 |
| ✅ | [04 · 6.10 Backpressure khi burst](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#610-backpressure-khi-burst) | K05 |
| ✅ | [README · 3.3 Rollup job: tính lại rồi ghi đè](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#33-rollup-job-tính-lại-rồi-ghi-đè) | K06, K09 |
| ✅ | [04 · 6.2 Time bucket và rollup nhiều tầng](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#62-time-bucket-và-rollup-nhiều-tầng) | K06 |
| ✅ | [04 · 6.5 Late-arriving data và watermark](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#65-late-arriving-data-và-watermark) | K06 |
| ✅ | [README · 3.5 Đối soát và event trễ](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#35-đối-soát-và-event-trễ) | K06, K07, K10 |
| ✅ | [README · 3.4 Đường đọc: phân rã khoảng bất kỳ](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#34-đường-đọc-phân-rã-khoảng-bất-kỳ) | K07, K09 |
| ✅ | [README · 3.6 Retention](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#36-retention) | K07 |
| ✅ | [README · 3.7 Bốn job nền](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#37-bốn-job-nền) | K07, K09 |
| ✅ | [04 · 6.3 Thuật toán phân rã khoảng thời gian bất kỳ](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#63-thuật-toán-phân-rã-khoảng-thời-gian-bất-kỳ) | K07 |
| ✅ | [04 · 6.8 Tỉ lệ 50% — đừng bao giờ lưu tỉ lệ](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#68-tỉ-lệ-50--đừng-bao-giờ-lưu-tỉ-lệ) | K07 |
| ✅ | [04 · 6.9 Cache và thundering herd](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#69-cache-và-thundering-herd) | K07 |
| ✅ | [04 · 6.11 Retention và chi phí lưu trữ](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#611-retention-và-chi-phí-lưu-trữ) | K07 |
| ✅ | [04 · 6.12 Observability — 6 metric phải có](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#612-observability--6-metric-phải-có) | K07 |
| ✅ | [README · 4. Đọc code theo thứ tự](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#4-đọc-code-theo-thứ-tự) | K08 |
| ✅ | [04 · 9. Nếu demo bằng Java/Spring (stack của Katalon)](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#9-nếu-demo-bằng-javaspring-stack-của-katalon) | K08 |
| ✅ | [README · 6. Chạy như một service](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#6-chạy-như-một-service) | K10 |
| ✅ | [README · 8. Ranh giới trung thực](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#8-ranh-giới-trung-thực) | K10 |
| ✅ | [README · 7. Khi tải lên 10M và 100M request/phút](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#7-khi-tải-lên-10m-và-100m-requestphút) | K11 |
| ✅ | [07 · 4. Cái gì vỡ trước — đi từng bước × 10](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#4-cái-gì-vỡ-trước--đi-từng-bước--10) | K11 |
| ✅ | [07 · 4.3 × 1.000 — 10M request/phút](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#43--1000--10m-requestphút) | K11 |
| ✅ | [07 · 5.1 Sơ đồ](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#51-sơ-đồ) | K11 |
| ✅ | [07 · 5.2 Quyết định và đánh đổi](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#52-quyết-định-và-đánh-đổi) | K11 |
| ✅ | [07 · 5.4 Exactly-once về hiệu quả từ các mảnh at-least-once](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#54-exactly-once-về-hiệu-quả-từ-các-mảnh-at-least-once) | K11 |
| ✅ | [README · 2. 5 Sự cố Production kinh điển và cách Code để Fix](../../katalon-prep/katalon-prep-java/09-kafka-pipeline/README.md#2-5-sự-cố-production-kinh-điển-và-cách-code-để-fix) | K11 |
| ✅ | [07 · 4.4 × 10.000 — 100M request/phút](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#44--10000--100m-requestphút) | K12 |
| ✅ | [07 · 6. Kiến trúc ở 100M request/phút](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#6-kiến-trúc-ở-100m-requestphút) | K12 |
| ✅ | [07 · 6.2 Năm thay đổi so với 10M](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#62-năm-thay-đổi-so-với-10m) | K12 |
| ✅ | [07 · 6.3 Ba thứ phải tính tiền ra được](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#63-ba-thứ-phải-tính-tiền-ra-được) | K12 |
| ✅ | [07 · 6.4 Cái KHÔNG làm ở 100M, và vì sao](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#64-cái-không-làm-ở-100m-và-vì-sao) | K12 |
| ✅ | [07 · 8. Kịch bản trả lời](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#8-kịch-bản-trả-lời) | K12, K13 |
| ✅ | [04 · 10. 16 câu follow-up interviewer hay hỏi](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#10-16-câu-follow-up-interviewer-hay-hỏi) | K13 |

## Theo mục nguồn

### README · 1-đề-bài-và-sáu-tiêu-chí-chấm

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#1-đề-bài-và-sáu-tiêu-chí-chấm](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#1-đề-bài-và-sáu-tiêu-chí-chấm)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Đề Katalon: ~10.000 request/phút, mỗi request một list key-value, value true/false, key có thể giả; bất kỳ lúc nào trả lời số true/false/fake key cho 24 giờ qua hoặc khoảng bất kỳ | K00 · 0:46 (The question) |
| ✅ | Hợp đồng: POST /events {tenantId, batchId do client sinh và dùng lại khi retry, ts = event time, items} → 202; GET /stats?tenant&from&to | K00 · 3:08 (The contract) |
| ✅ | Sáu tiêu chí T1–T6: khoảng bất kỳ, đọc rẻ, retry không đếm trùng, event trễ đúng phút, sai sửa được, ghi không xếp hàng sau lock | K00 · 3:41 (Six tests) |

### 04-event-counting-10k · 0-đề-bài-và-những-gì-đề-bài-không-nói

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#0-đề-bài-và-những-gì-đề-bài-không-nói](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#0-đề-bài-và-những-gì-đề-bài-không-nói)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Ba cụm từ quyết định thiết kế: 'bất kỳ lúc nào' (đọc rẻ, không job nền), 'khoảng bất kỳ' (phần khó), 'list key-value' (đơn vị là item) | K00 · 1:19 (Three phrases) |

### 04-event-counting-10k · 2-trước-khi-vẽ-6-câu-hỏi-phải-hỏi-interviewer

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#2-trước-khi-vẽ-6-câu-hỏi-phải-hỏi-interviewer](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#2-trước-khi-vẽ-6-câu-hỏi-phải-hỏi-interviewer)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Hỏi trước khi vẽ: số cặp mỗi request, tổng hay theo key, số key hợp lệ, độ tươi, khoảng xa nhất, dashboard hay billing — mỗi câu đổi thiết kế | K00 · 1:52 (Ask first) |

### 04-event-counting-10k · 3-ước-lượng-tải--con-số-quyết-định-kiến-trúc

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#3-ước-lượng-tải--con-số-quyết-định-kiến-trúc](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#3-ước-lượng-tải--con-số-quyết-định-kiến-trúc)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Ước lượng: 167 request/s × 20 = 3.333 event/s, 288 triệu/ngày, ~17 GB raw/ngày; '24 giờ qua' chỉ 96 dòng → không bao giờ quét raw | K00 · 2:32 (The numbers) |
| ✅ | '24 giờ qua' = 288 triệu dòng mỗi lần refresh; một năm 105 tỷ; index không đổi bản chất | K02 · 1:34 (The cost) |

### 04-event-counting-10k · 1-vì-sao-câu-trả-lời-cũ-bị-bác--4-lỗ-hổng

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#1-vì-sao-câu-trả-lời-cũ-bị-bác--4-lỗ-hổng](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#1-vì-sao-câu-trả-lời-cũ-bị-bác--4-lỗ-hổng)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Câu trả lời cũ: insert theo chunk, lock và cộng vào tổng 24 giờ, đọc bản ghi cuối; ý đúng: đếm trước thay vì đếm lúc hỏi | K01 · 0:20 (The answer) |

### README · 21-cách-1--khoá-và-một-tổng-chạy-đọc-bản-ghi-cuối

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#21-cách-1--khoá-và-một-tổng-chạy-đọc-bản-ghi-cuối](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#21-cách-1--khoá-và-một-tổng-chạy-đọc-bản-ghi-cuối)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Cách 1 thành code: một dòng mỗi tenant, SELECT … FOR UPDATE rồi UPDATE cộng dồn | K01 · 0:46 (As code) |
| ✅ | Bảng chấm cách 1: chỉ qua T2; trượt T1 (chí mạng), T3, T4, T5, T6 | K01 · 3:10 (Scorecard) |

### 04-event-counting-10k · lỗ-hổng-1--một-counter-lũy-kế-không-trả-lời-được-range-query

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#lỗ-hổng-1--một-counter-lũy-kế-không-trả-lời-được-range-query-chí-mạng](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#lỗ-hổng-1--một-counter-lũy-kế-không-trả-lời-được-range-query-chí-mạng)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Tổng chạy mất thời gian: 09:00 → 20, 10:00 → 40, retry → 60 (đúng là 40); không trả lời được 09:00–10:00 | K01 · 1:21 (Time is gone) |
| ✅ | Snapshot rồi trừ chỉ đúng khi tổng không reset; độ phân giải bằng chu kỳ snapshot; retry vẫn cộng trùng | K01 · 1:58 (Snapshots?) |

### 04-event-counting-10k · lỗ-hổng-2--lock-ở-write-path-là-anti-pattern-về-scale

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#lỗ-hổng-2--lock-ở-write-path-là-anti-pattern-về-scale](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#lỗ-hổng-2--lock-ở-write-path-là-anti-pattern-về-scale)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Đo 7 lần: lock + tổng chạy 1.080–1.367 batch/s, append raw 3.334–5.488 (chậm 2,4–4,4 lần, 16 luồng); đề cần 167/s nên lock là lý do thứ hai | K01 · 2:31 (The lock) |

### 04-event-counting-10k · 12-ranh-giới-trung-thực

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#12-ranh-giới-trung-thực](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#12-ranh-giới-trung-thực)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Kể lại câu trả lời cũ: tổng chạy chỉ giữ trạng thái hiện tại nên không trả lời khoảng; bản sửa đếm theo bucket, bất đồng bộ, raw là sự thật | K01 · 3:30 (In the room) |
| ✅ | Nói giới hạn trước: 20 cặp là giả định; đúng đắn đã test; tốc độ là một lần đo; ngưỡng Postgres là bậc độ lớn; 10M/100M trên giấy | K13 · 4:55 (Limits) |

### README · 22-cách-2--lưu-mọi-event-đếm-lúc-hỏi

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#22-cách-2--lưu-mọi-event-đếm-lúc-hỏi](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#22-cách-2--lưu-mọi-event-đếm-lúc-hỏi)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Cách 2: mỗi item một dòng, event_id tất định + ON CONFLICT DO NOTHING, COUNT(*) GROUP BY outcome cho khoảng bất kỳ | K02 · 0:25 (The idea) |
| ✅ | Cách 2 qua 5/6 tiêu chí; trượt T2 vì chi phí đọc tăng theo dữ liệu | K02 · 0:52 (Five passes) |
| ✅ | Đo: khoảng 5 giờ 25 phút — cách 2 đếm 32.460 dòng raw, cách 4 đọc 337 dòng bucket + 60 dòng raw | K02 · 1:52 (Measured) |

### 04-event-counting-10k · 61-data-model

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#61-data-model](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#61-data-model)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Giữ bảng raw: nguồn sự thật, idempotency ở tầng dưới cùng, oracle cho test, giữ 7 ngày | K02 · 2:17 (Keep half) |
| ✅ | DDL raw: outcome smallint; partition theo ngày UTC của ts; PK (event_id, ts) phải chứa cột partition; ts là event time; bound có +00 | K05 · 1:02 (Raw table) |

### README · 23-cách-3--counter-theo-phút-cộng-ngay-lúc-ghi

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#23-cách-3--counter-theo-phút-cộng-ngay-lúc-ghi](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#23-cách-3--counter-theo-phút-cộng-ngay-lúc-ghi)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Cách 3: bảng (tenant, phút, outcome), upsert cnt = cnt + EXCLUDED.cnt theo event time; khoảng = tổng các phút | K03 · 0:30 (The idea) |
| ✅ | Cách 3 sửa được lỗi chí mạng: 09:30–10:30 khớp COUNT(*) trên raw; đọc rẻ; trễ đúng phút | K03 · 0:56 (It works) |
| ✅ | Bảng chấm cách 3: qua 4/6; trượt T3 (retry) và T5 (sửa được) | K03 · 3:45 (Scorecard) |

### 04-event-counting-10k · 64-idempotency--vì-sao-counter-không-phải-nguồn-sự-thật

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#64-idempotency--vì-sao-counter-không-phải-nguồn-sự-thật](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#64-idempotency--vì-sao-counter-không-phải-nguồn-sự-thật)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Atomic không phải idempotent: retry cộng 20 hai lần; đo 2.420 so với 2.400; sửa bằng raw + đếm lại | K03 · 1:43 (Retry) |

### 04-event-counting-10k · lỗ-hổng-3--trộn-nguồn-sự-thật-với-cache

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#lỗ-hổng-3--trộn-nguồn-sự-thật-với-cache](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#lỗ-hổng-3--trộn-nguồn-sự-thật-với-cache)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Không có raw: không phát hiện được sai, không tính lại được; bảng batch đã thấy = raw đổi tên + hai lệnh ghi phải commit cùng nhau | K03 · 2:23 (No raw) |

### 04-event-counting-10k · 5-design-tầng-2--bản-production

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#5-design-tầng-2--bản-production](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#5-design-tầng-2--bản-production)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Redis hash theo giờ cùng thiết kế: 25 lần đọc trong pipeline; cùng lỗi đếm trùng, mất khi restart; chỉ làm cache | K03 · 2:53 (Redis) |

### README · 5-test-chứng-minh-gì--kết-quả-chạy-thật

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#5-test-chứng-minh-gì--kết-quả-chạy-thật](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#5-test-chứng-minh-gì--kết-quả-chạy-thật)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Đo 7 lần: cách 3 4.094–5.986 batch/s, không chậm hơn cách 4 (3.334–5.488) — bác vì đúng đắn, không vì tốc độ | K03 · 3:23 (Not speed) |
| ✅ | IngestIdempotencyTest: cùng batch hai lần → 20 rồi 0; batchId khác → 40 dòng; 8 ngày → 422; tương lai, 1.001 item → 400 | K08 · 3:08 (Proof) |
| ✅ | Test trung thực: Postgres 16 thật chạy subprocess (không Docker, không H2), database mới mỗi test, đồng hồ do test điều khiển, COUNT(*) trên raw là oracle | K10 · 0:26 (Test setup) |
| ✅ | anyRangeEqualsCountOnRaw: 3 giờ tải, 10% batch trễ tới 5 phút, rollup mỗi phút, 500 khoảng ngẫu nhiên phải bằng COUNT(*) trên raw | K10 · 1:05 (The big one) |
| ✅ | Kết quả: 72.000 event, 500/500 khoảng khớp, mỗi truy vấn ≤ 436 dòng bucket; cả 3 giờ đọc 12 dòng, 0 dòng raw | K10 · 1:33 (It printed) |
| ✅ | Mỗi lời hứa một test: retry, trễ trong cửa sổ, trễ 3 giờ (1.000 → 1.020), drift, đọc bị chặn, retention, cách bị bác, hợp đồng HTTP qua Spring | K10 · 1:54 (Every promise) |
| ✅ | Đo tranh chấp 7 lần: 1.080–1.367 / 4.094–5.986 / 3.334–5.488 batch/s; nói tỉ lệ, không đọc số tuyệt đối | K10 · 2:51 (Contention) |

### README · 24-cách-4--raw--rollup-tính-lại--phân-rã-khoảng-cách-được-ch

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#24-cách-4--raw--rollup-tính-lại--phân-rã-khoảng-cách-được-chọn](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#24-cách-4--raw--rollup-tính-lại--phân-rã-khoảng-cách-được-chọn)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Cách 4: raw là sự thật (từ cách 2) + đếm trước theo bucket (từ cách 3), nối bằng quy tắc tính lại và ghi đè, không cộng dồn; ghi tách khỏi đếm | K04 · 0:25 (The rule) |

### 04-event-counting-10k · 4-design-tầng-1--bản-tối-giản-đúng

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#4-design-tầng-1--bản-tối-giản-đúng](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#4-design-tầng-1--bản-tối-giản-đúng)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Sơ đồ: client → ingest (202) → raw_events → RollupJob mỗi phút → agg_1m/1h/1d → Stats phân rã; Reconciler hằng giờ | K04 · 1:00 (The design) |

### README · 25-bảng-chấm-cả-bốn-cách

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#25-bảng-chấm-cả-bốn-cách](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#25-bảng-chấm-cả-bốn-cách)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Bảng chấm bốn cách: 1 bác (T1), 2 bác (T2) giữ raw, 3 bác (T3, T5), 4 được chọn | K04 · 1:45 (Scorecard) |
| ✅ | Cơ chế từng tiêu chí: bucket + raw mép; ≤ 529 bucket/năm; id tất định + đếm lại; cửa sổ 30 phút; raw 7 ngày + đối soát; chỉ job ghi bucket | K04 · 2:06 (Why each passes) |

### 04-event-counting-10k · 7-chọn-tech-stack--3-phương-án-và-ngưỡng-chuyển

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#7-chọn-tech-stack--3-phương-án-và-ngưỡng-chuyển](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#7-chọn-tech-stack--3-phương-án-và-ngưỡng-chuyển)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Chưa cần Kafka ở 10k/phút; ngưỡng: > ~50k event/s, breakdown theo key > 10k key, replay > 7 ngày, nhiều consumer độc lập | K04 · 2:55 (Right-size) |

### 04-event-counting-10k · 11-kịch-bản-trình-bày-3-phút

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#11-kịch-bản-trình-bày-3-phút](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#11-kịch-bản-trình-bày-3-phút)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Bốn câu phải nhớ: ghi tách khỏi đếm; đếm theo bucket; raw là sự thật, bucket tính lại; chưa cần Kafka + ngưỡng | K04 · 3:33 (Four sentences) |
| ✅ | Khung 3 phút: hỏi, ước lượng, bác lần lượt rồi sửa, dựng, chứng minh, right-size | K13 · 0:23 (Three minutes) |
| ✅ | Bài nói mẫu kể như câu chuyện: hỏi, số, cách hiển nhiên bị bác, raw + bucket tính lại, phân rã + đối soát, chưa cần Kafka | K13 · 0:45 (The answer) |

### README · 31-bảng-và-partition

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#31-bảng-và-partition](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#31-bảng-và-partition)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Sáu bảng: raw_events (7 ngày), agg_1m (30 ngày), agg_1h (1 năm), agg_1d (mãi), rollup_state (sealed_until), valid_keys | K05 · 0:34 (Tables) |
| ✅ | event_id = UUID theo tên của tenant/batchId/index; retry cùng batchId → cùng id → insert 0 dòng; id ngẫu nhiên phía server sẽ đếm mọi retry | K05 · 1:54 (Event id) |
| ✅ | eventId() một dòng: UUID.nameUUIDFromBytes; controller trả 202 ACCEPTED; GET /stats nhận Instant | K08 · 2:45 (The id) |

### README · 32-đường-ghi

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#32-đường-ghi](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#32-đường-ghi)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Đường ghi năm bước: validate (1–1.000 item), kiểm event time (7 ngày → 422, tương lai → 400), phân loại, một INSERT nhiều dòng, 202 | K05 · 2:34 (Write path) |
| ✅ | ingest(): validate, quá 7 ngày → rejectedTooLate + 422, tương lai > 5 phút → 400, trễ > 30 phút → lateBeyondWindow, ensureFor partition | K08 · 1:42 (Ingest) |

### 04-event-counting-10k · 66-phân-loại-fake-key

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#66-phân-loại-fake-key](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#66-phân-loại-fake-key)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Phân loại: key trước, value sau; key giả → fake_key; 'yes', 1, null → malformed, không gộp vào false | K05 · 3:21 (Four outcomes) |
| ✅ | Registry: < 100k key → HashSet trong RAM, thay cả bộ mỗi 60 s; 100k–10M → Redis set; > 10M → Bloom filter, 'không' là chắc chắn giả | K05 · 3:47 (Registry) |

### README · 33-rollup-job-tính-lại-rồi-ghi-đè

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#33-rollup-job-tính-lại-rồi-ghi-đè](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#33-rollup-job-tính-lại-rồi-ghi-đè)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Một cửa sổ: DELETE rồi INSERT … SELECT count(*) theo date_bin gốc UTC trong một transaction; giờ từ phút, ngày từ giờ; rồi dời seal | K06 · 0:23 (Replace) |
| ✅ | refreshRecent: to = đầu phút hiện tại (không seal phút đang chạy), from = to − 30 phút; sealed_until = to | K06 · 1:09 (Every minute) |
| ✅ | refreshRecent(): cửa sổ 30 phút tới đầu phút hiện tại, replace + seal trong một transaction; replaceAll ba tầng | K09 · 0:16 (refreshRecent) |
| ✅ | replace(): DELETE + INSERT … SELECT date_bin; refresh(from, to) dùng lại cho backfill và sửa, không dời seal | K09 · 0:54 (replace) |

### README · 35-đối-soát-và-event-trễ

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#35-đối-soát-và-event-trễ](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#35-đối-soát-và-event-trễ)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Test: batch event time 09:58:10 tới lúc 10:05:30 → bucket 09:58 là 0 trước lần chạy kế, 20 sau lần chạy lúc 10:06:30 | K06 · 2:07 (Measured) |
| ✅ | Reconciler: mỗi giờ đã đóng, COUNT(*) raw vs SUM(cnt) agg_1m bằng FULL OUTER JOIN; lệch → reconcile_drift + tính lại giờ đó | K07 · 2:59 (Reconcile) |
| ✅ | ReconcilerTest: cố tình cộng 5 vào một bucket → drift −5 → sửa → sạch | K10 · 2:28 (Plant a bug) |

### 04-event-counting-10k · 65-late-arriving-data-và-watermark

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#65-late-arriving-data-và-watermark](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#65-late-arriving-data-và-watermark)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Trễ ≤ 30 phút: lần rollup kế; ≤ 24 giờ: đối soát hằng giờ; ≤ 7 ngày: backfill; > 7 ngày: 422 + metric | K06 · 2:41 (Later still) |

### 04-event-counting-10k · 62-time-bucket-và-rollup-nhiều-tầng

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#62-time-bucket-và-rollup-nhiều-tầng](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#62-time-bucket-và-rollup-nhiều-tầng)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Lựa chọn: vì sao 30 phút; vì sao delete + insert thay vì upsert; hai bản job chạy cùng lúc → advisory lock; TimescaleDB continuous aggregate là cùng ý tưởng | K06 · 3:06 (Trade-offs) |

### 04-event-counting-10k · 63-thuật-toán-phân-rã-khoảng-thời-gian-bất-kỳ

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#63-thuật-toán-phân-rã-khoảng-thời-gian-bất-kỳ](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#63-thuật-toán-phân-rã-khoảng-thời-gian-bất-kỳ)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Ví dụ 03:17:30 → 3 ngày sau 11:42:10: raw 30 s, 42 phút, 20 giờ, 2 ngày, 11 giờ, 42 phút, raw 10 s = 84 + 31 + 2 bucket | K07 · 0:21 (Decompose) |

### README · 34-đường-đọc-phân-rã-khoảng-bất-kỳ

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#34-đường-đọc-phân-rã-khoảng-bất-kỳ](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#34-đường-đọc-phân-rã-khoảng-bất-kỳ)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Thuật toán: mỗi bước lấy bucket thô nhất bắt đầu đúng ở đây và còn vừa; mép < 1 phút đọc raw; gộp mảnh cùng tầng; sau seal đọc raw | K07 · 0:53 (Algorithm) |
| ✅ | Lỗi đã sửa: bản Python cũ đi tiếp cùng tầng tới hết khoảng → 4.824 bucket phút thay vì 84 + 31 + 2; kết quả vẫn đúng nên khó thấy | K07 · 1:23 (A real bug) |
| ✅ | Chặn trên: ≤ 2 mảnh raw, ≤ 118 phút + 46 giờ, ≤ 529 bucket cho một năm; 20.000 khoảng ngẫu nhiên nhiều nhất 507 | K07 · 1:58 (Bounded) |
| ✅ | decompose(): clamp theo seal, chọn lại tầng ở mỗi bước, add() gộp tầng giống nhau, đuôi sau seal đọc raw | K09 · 1:25 (Decompose) |
| ✅ | StatsService.query(): mảng 4 số theo outcome, raw đếm, bucket cộng và đếm số dòng đọc; Stats.of chia tỉ lệ lúc đọc | K09 · 1:57 (Stats) |

### 04-event-counting-10k · 68-tỉ-lệ-50--đừng-bao-giờ-lưu-tỉ-lệ

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#68-tỉ-lệ-50--đừng-bao-giờ-lưu-tỉ-lệ](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#68-tỉ-lệ-50--đừng-bao-giờ-lưu-tỉ-lệ)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Không lưu tỉ lệ: 90/100 và 10/1.000 → trung bình 45,5% sai, đúng là 9,1%; StatsService chia lúc đọc | K07 · 2:28 (Rates) |

### 04-event-counting-10k · 611-retention-và-chi-phí-lưu-trữ

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#611-retention-và-chi-phí-lưu-trữ](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#611-retention-và-chi-phí-lưu-trữ)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Retention: DELETE 288 triệu dòng/ngày gây vacuum storm; drop partition là O(1); rollup giữ câu trả lời của ngày đã xoá | K07 · 3:31 (Retention) |

### 04-event-counting-10k · 612-observability--6-metric-phải-có

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#612-observability--6-metric-phải-có](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#612-observability--6-metric-phải-có)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Sáu metric (reconcile_drift phải bằng 0) và bốn job nền: reload key, rollup, reconcile, partition | K07 · 3:57 (Watch it) |

### 04-event-counting-10k · 69-cache-và-thundering-herd

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#69-cache-và-thundering-herd](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#69-cache-và-thundering-herd)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Cache '24 giờ qua': key chứa phút đã seal (không đoán TTL), single-flight khi miss; lock ở đường đọc; chưa code | K07 · 4:21 (Cache) |

### README · 4-đọc-code-theo-thứ-tự

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#4-đọc-code-theo-thứ-tự](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#4-đọc-code-theo-thứ-tự)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Thứ tự đọc code: schema.sql, EventBatch/EventClassifier, IngestService, RollupJob, RangeDecomposer/StatsService, Reconciler, rejected/, lớp Spring mỏng | K08 · 0:43 (The map) |
| ✅ | EventBatch là record, value giữ kiểu Object; classify: key trước, pattern matching theo kiểu value, còn lại là MALFORMED | K08 · 1:14 (The request) |

### 04-event-counting-10k · 9-nếu-demo-bằng-javaspring-stack-của-katalon

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#9-nếu-demo-bằng-javaspring-stack-của-katalon](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#9-nếu-demo-bằng-javaspring-stack-của-katalon)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Một INSERT nhiều dòng cho cả batch, OffsetDateTime UTC thay vì Timestamp, ON CONFLICT DO NOTHING, số dòng trả về = số dòng mới | K08 · 2:16 (One insert) |

### README · 37-bốn-job-nền

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#37-bốn-job-nền](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#37-bốn-job-nền)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Jobs: reloadKeys mỗi phút, rollup mỗi phút, reconcile phút 5 mỗi giờ, partitions tạo trước và xoá quá hạn | K09 · 2:22 (Wiring) |

### README · 6-chạy-như-một-service

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#6-chạy-như-một-service](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#6-chạy-như-một-service)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Chạy: mvn -pl 10-event-counting test (20 test, không Docker); chạy service với Postgres 16 và curl | K10 · 3:19 (Run it) |

### README · 8-ranh-giới-trung-thực

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#8-ranh-giới-trung-thực](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#8-ranh-giới-trung-thực)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Giới hạn: đúng đắn đã test; số tranh chấp là một lần chạy; chưa load test kéo dài; advisory lock và cache chưa code | K10 · 3:35 (Honest limits) |

### 07-event-counting-10m-100m · 43--1000--10m-requestphút

Nguồn: [../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#43--1000--10m-requestphút](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#43--1000--10m-requestphút)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | 10M/phút: 166.667 request/s, 3,3 triệu event/s, 288 tỷ/ngày, ~1,3 Gbit/s; đường đọc không đổi | K11 · 0:38 (Numbers) |
| ✅ | Bị bác ở 10M: Postgres to hơn mỗi event một dòng, Kafka key theo tenant, consumer ghi đồng bộ từng record (rebalance storm), đánh dấu rồi cộng trong hai commit, bảng dedup Postgres | K11 · 1:58 (Rejected) |

### 07-event-counting-10m-100m · 4-cái-gì-vỡ-trước--đi-từng-bước--10

Nguồn: [../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#4-cái-gì-vỡ-trước--đi-từng-bước--10](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#4-cái-gì-vỡ-trước--đi-từng-bước--10)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Leo × 10: 100k → gộp mỗi request thành một dòng; 1M → Kafka giữa API và phần đếm, 202 sau acks=all; 10M → key batch_id, khử trùng trong stream state | K11 · 1:06 (×10, ×100) |

### README · 2-5-sự-cố-production-kinh-điển-và-cách-code-để-fix

Nguồn: [../katalon-prep/katalon-prep-java/09-kafka-pipeline/README.md#2-5-sự-cố-production-kinh-điển-và-cách-code-để-fix](../../katalon-prep/katalon-prep-java/09-kafka-pipeline/README.md#2-5-sự-cố-production-kinh-điển-và-cách-code-để-fix)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Đo (module 09): key theo tenant → partition nóng nhất 53,2%; key theo batch_id → ~25% | K11 · 2:53 (Measured skew) |
| ✅ | Poison pill → DLT để partition tiếp tục; burst → buffer có giới hạn + token bucket → 429 Retry-After | K11 · 4:47 (Failure modes) |

### 07-event-counting-10m-100m · 51-sơ-đồ

Nguồn: [../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#51-sơ-đồ](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#51-sơ-đồ)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Pipeline 10M: SDK batch_id + retry, ~28 pod collector, Kafka key batch_id 64–128 partition, stream processor khử trùng 15 phút + cửa sổ 1 phút, GREATEST, raw đóng gói ra object storage, đối soát hằng giờ | K11 · 3:24 (The pipeline) |

### 07-event-counting-10m-100m · 54-exactly-once-về-hiệu-quả-từ-các-mảnh-at-least-once

Nguồn: [../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#54-exactly-once-về-hiệu-quả-từ-các-mảnh-at-least-once](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#54-exactly-once-về-hiệu-quả-từ-các-mảnh-at-least-once)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Sửa ở module 09: đánh dấu processed_batch và cộng số đếm trong một transaction; race lúc rebalance → transaction thua rollback | K11 · 4:11 (One transaction) |

### README · 7-khi-tải-lên-10m-và-100m-requestphút

Nguồn: [../katalon-prep/katalon-prep-java/10-event-counting/README.md#7-khi-tải-lên-10m-và-100m-requestphút](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#7-khi-tải-lên-10m-và-100m-requestphút)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Ba ý không đổi: log sự thật, đếm theo event time theo bucket, không cộng mù — chỉ công cụ đổi | K11 · 5:20 (What stayed) |

### 07-event-counting-10m-100m · 44--10000--100m-requestphút

Nguồn: [../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#44--10000--100m-requestphút](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#44--10000--100m-requestphút)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | 100M/phút: 1,67 triệu request/s, 33 triệu event/s, ~13 Gbit/s; 1 µs mỗi request = 1,7 core | K12 · 0:45 (Numbers) |

### 07-event-counting-10m-100m · 64-cái-không-làm-ở-100m-và-vì-sao

Nguồn: [../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#64-cái-không-làm-ở-100m-và-vì-sao](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#64-cái-không-làm-ở-100m-và-vì-sao)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Bị bác ở 100M: một cụm Kafka toàn cầu, exactly-once xuyên region (2PC), sketch cho số tổng, khử trùng theo batch_id (1,5 tỷ khoá), mỗi event một dòng | K12 · 1:10 (Rejected) |
| ✅ | Số đếm active-active được vì không có bất biến giữa các cell; số dư thì có (không âm) nên cần một nơi ghi hoặc đồng thuận | K12 · 3:37 (Counts vs money) |

### 07-event-counting-10m-100m · 62-năm-thay-đổi-so-với-10m

Nguồn: [../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#62-năm-thay-đổi-so-với-10m](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#62-năm-thay-đổi-so-với-10m)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Năm thay đổi: cell theo region cộng toàn cục (G-counter), khử trùng (client_id, seq), dây nhị phân + nén, raw đóng gói ở lại region, sketch chỉ cho distinct/top-K | K12 · 1:58 (Five changes) |

### 07-event-counting-10m-100m · 63-ba-thứ-phải-tính-tiền-ra-được

Nguồn: [../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#63-ba-thứ-phải-tính-tiền-ra-được](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#63-ba-thứ-phải-tính-tiền-ra-được)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Ba hoá đơn: mạng qua AZ ≈ 6.900 USD/ngày nếu chưa đóng gói, lưu raw 110k vs 12k USD/tháng, CPU 1,7 core mỗi µs | K12 · 2:49 (Three bills) |

### 07-event-counting-10m-100m · 8-kịch-bản-trả-lời

Nguồn: [../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#8-kịch-bản-trả-lời](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#8-kịch-bản-trả-lời)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Câu trả lời 100M dưới một phút: vật lý và tiền; cell theo region; khử trùng (client_id, seq); số tổng chính xác | K12 · 4:03 (One minute) |
| ✅ | Follow-up: 10M trong một phút — chỉ đường ghi đổi: gộp request, Kafka key batch_id, khử trùng 15 phút, cửa sổ 1 phút, GREATEST, raw đóng gói | K13 · 4:27 (Follow-up 5) |

### 04-event-counting-10k · 10-16-câu-follow-up-interviewer-hay-hỏi

Nguồn: [../katalon-prep/katalon-system-design/04-event-counting-10k.md#10-16-câu-follow-up-interviewer-hay-hỏi](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#10-16-câu-follow-up-interviewer-hay-hỏi)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Follow-up: 3–5 giờ chiều thứ Ba → hai bucket giờ × 4 outcome = 8 dòng, không chạm raw | K13 · 2:08 (Follow-up 1) |
| ✅ | Follow-up: crash giữa chừng → retry chèn 0 dòng; rollup thay trong một transaction nên crash rollback; không có phép cộng nào chạy hai lần | K13 · 2:42 (Follow-up 2) |
| ✅ | Follow-up: biết số đúng bằng cách đo — đối soát hằng giờ, lệch thì cảnh báo và tính lại | K13 · 3:13 (Follow-up 3) |
| ✅ | Follow-up: 'hôm nay' giờ Việt Nam = 17:00 UTC hôm qua tới 17:00 UTC hôm nay; tầng giờ phục vụ mọi múi giờ tròn giờ | K13 · 3:48 (Follow-up 4) |

