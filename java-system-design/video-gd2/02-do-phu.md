# Độ phủ nội dung — video giai đoạn 2

**46/46 ý chính đã có video.** Bảng sinh bằng `python -m lesson_video coverage` từ `points.yaml` (ý chính lấy từ tài liệu nguồn) và kịch bản `ep*.yaml`; mỗi ý có chữ bắt buộc (`expect`) mà lệnh `check` đã kiểm là có mặt trong cảnh dạy ý đó. Thời điểm lấy từ `lessons.json` sau khi dựng.

## Theo video

| Video | Dài | Chương | Ý chính |
|---|---:|---:|---:|
| Ep00 · Phase 2 orientation | 6:02 | 9 | 9 |
| Ep01 · Replication: leaders, followers, and lag | 7:11 | 7 | 7 |
| Ep02 · Quorums: N, W, R and what they don't promise | 6:16 | 7 | 6 |
| Ep03 · Partitioning: ranges, hashes, hot spots, secondary indexes | 5:50 | 7 | 5 |
| Ep04 · Consistent hashing: lab 5A and the 2-minute talk | chưa dựng | 8 | 6 |
| Ep05 · Timed designs: a key-value store and a distributed cache | chưa dựng | 6 | 6 |
| Ep06 · V6: Mini Core Transfer at 1M users | chưa dựng | 6 | 7 |

## Theo mục của tài liệu nguồn

**18/67 mục có video.** Mỗi mục là một heading trong phạm vi của tuần; mục có video khi nằm trong `covers` của video hoặc là nguồn của một ý chính đã dạy.

| | Mục | Video |
|:---:|---|---|
| ✅ | [20 · 0. Trước khi bắt đầu](../20-implement-gd2-du-lieu-phan-tan.md#0-trước-khi-bắt-đầu) | Ep00 |
| ✅ | [00 · Giai đoạn 2 — Dữ liệu và hệ phân tán (tuần 5–10)](../00-lo-trinh-6-thang.md#giai-đoạn-2--dữ-liệu-và-hệ-phân-tán-tuần-510) | Ep00 |
| ✅ | [20 · Tuần 5–6 — Replication, partitioning, consistent hashing](../20-implement-gd2-du-lieu-phan-tan.md#tuần-56--replication-partitioning-consistent-hashing) | Ep01 |
| ✅ | [20 · Đầu ra phải có cuối tuần 6](../20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần-6) | Ep01 |
| ✅ | [20 · Đọc](../20-implement-gd2-du-lieu-phan-tan.md#đọc) | Ep01 |
| ✅ | [20 · Ghi chú khái niệm](../20-implement-gd2-du-lieu-phan-tan.md#ghi-chú-khái-niệm) | Ep01 |
| ✅ | [20 · 5.1 Ba mô hình replication](../20-implement-gd2-du-lieu-phan-tan.md#51-ba-mô-hình-replication) | Ep01 |
| ✅ | [20 · 5.2 Quorum `N, W, R`](../20-implement-gd2-du-lieu-phan-tan.md#52-quorum-n-w-r) | Ep02 |
| ✅ | [20 · 5.3 Partitioning và rebalancing](../20-implement-gd2-du-lieu-phan-tan.md#53-partitioning-và-rebalancing) | Ep03 |
| ✅ | [20 · Lab 5A — Vòng băm có virtual node (Thứ Bảy tuần 5)](../20-implement-gd2-du-lieu-phan-tan.md#lab-5a--vòng-băm-có-virtual-node-thứ-bảy-tuần-5) | Ep04 |
| ✅ | [20 · Lab 5B — Mô phỏng quorum (Thứ Bảy tuần 6)](../20-implement-gd2-du-lieu-phan-tan.md#lab-5b--mô-phỏng-quorum-thứ-bảy-tuần-6) | Ep02 |
| ✅ | [20 · Track P tuần 5–6](../20-implement-gd2-du-lieu-phan-tan.md#track-p-tuần-56) | Ep06 |
| ✅ | [20 · Track S tuần 5–6 — V6: Mini Core Transfer ở L2 (Chủ nhật tuần 6, 30 phút)](../20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-56--v6-mini-core-transfer-ở-l2-chủ-nhật-tuần-6-30-phút) | Ep06 |
| ✅ | [20 · Bài tập tuần 5–6](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-56) | Ep01, Ep02, Ep03, Ep04, Ep05 |
| ✅ | [20 · Bài nói 2 phút](../20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút) | Ep04 |
| ✅ | [02 · 3.3 L2 — 1M users: tách đọc, tách việc chậm](../02-ve-he-thong-100k-1m-10m.md#33-l2--1m-users-tách-đọc-tách-việc-chậm) | Ep06 |
| ✅ | [02 · 5. Câu hỏi "tải tăng 10 lần thì sao?" — cách trả lời](../02-ve-he-thong-100k-1m-10m.md#5-câu-hỏi-tải-tăng-10-lần-thì-sao--cách-trả-lời) | Ep06 |
| ❌ | [20 · Tuần 7 — Transaction phân tán: Saga, outbox, idempotency, sổ cái kép](../20-implement-gd2-du-lieu-phan-tan.md#tuần-7--transaction-phân-tán-saga-outbox-idempotency-sổ-cái-kép) | **chưa có** |
| ❌ | [20 · Đầu ra phải có cuối tuần](../20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần) | **chưa có** |
| ❌ | [20 · Đọc](../20-implement-gd2-du-lieu-phan-tan.md#đọc-1) | **chưa có** |
| ❌ | [20 · Ghi chú khái niệm](../20-implement-gd2-du-lieu-phan-tan.md#ghi-chú-khái-niệm-1) | **chưa có** |
| ❌ | [20 · 7.1 Vì sao không 2PC giữa microservices](../20-implement-gd2-du-lieu-phan-tan.md#71-vì-sao-không-2pc-giữa-microservices) | **chưa có** |
| ❌ | [20 · 7.2 Orchestration hay choreography](../20-implement-gd2-du-lieu-phan-tan.md#72-orchestration-hay-choreography) | **chưa có** |
| ❌ | [20 · 7.3 Ba loại bước và thứ tự](../20-implement-gd2-du-lieu-phan-tan.md#73-ba-loại-bước-và-thứ-tự) | **chưa có** |
| ❌ | [20 · 7.4 Dual write và outbox](../20-implement-gd2-du-lieu-phan-tan.md#74-dual-write-và-outbox) | **chưa có** |
| ❌ | [20 · 7.5 Idempotency ở ba tầng](../20-implement-gd2-du-lieu-phan-tan.md#75-idempotency-ở-ba-tầng) | **chưa có** |
| ❌ | [20 · 7.6 Sổ cái kép (double-entry)](../20-implement-gd2-du-lieu-phan-tan.md#76-sổ-cái-kép-double-entry) | **chưa có** |
| ❌ | [20 · Lab 7 — Saga chuyển tiền liên ngân hàng (Thứ Bảy, 3–4 giờ)](../20-implement-gd2-du-lieu-phan-tan.md#lab-7--saga-chuyển-tiền-liên-ngân-hàng-thứ-bảy-34-giờ) | **chưa có** |
| ❌ | [20 · Track P tuần 7 — P09 nhìn qua ranh giới saga](../20-implement-gd2-du-lieu-phan-tan.md#track-p-tuần-7--p09-nhìn-qua-ranh-giới-saga) | **chưa có** |
| ❌ | [20 · Track S tuần 7](../20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-7) | **chưa có** |
| ❌ | [20 · Bài tập tuần 7](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-7) | **chưa có** |
| ❌ | [20 · Bài nói 2 phút](../20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút-1) | **chưa có** |
| ❌ | [20 · Tuần 8–9 — Kafka sâu, lab outbox + consumer idempotent, P20](../20-implement-gd2-du-lieu-phan-tan.md#tuần-89--kafka-sâu-lab-outbox--consumer-idempotent-p20) | **chưa có** |
| ❌ | [20 · Đầu ra phải có cuối tuần 9](../20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần-9) | **chưa có** |
| ❌ | [20 · Đọc](../20-implement-gd2-du-lieu-phan-tan.md#đọc-2) | **chưa có** |
| ❌ | [20 · Ghi chú khái niệm](../20-implement-gd2-du-lieu-phan-tan.md#ghi-chú-khái-niệm-2) | **chưa có** |
| ❌ | [20 · 8.1 Thứ tự chỉ tồn tại trong một partition](../20-implement-gd2-du-lieu-phan-tan.md#81-thứ-tự-chỉ-tồn-tại-trong-một-partition) | **chưa có** |
| ❌ | [20 · 8.2 Độ bền ghi](../20-implement-gd2-du-lieu-phan-tan.md#82-độ-bền-ghi) | **chưa có** |
| ❌ | [20 · 8.3 Ba mức giao nhận](../20-implement-gd2-du-lieu-phan-tan.md#83-ba-mức-giao-nhận) | **chưa có** |
| ❌ | [20 · 8.4 Consumer group, rebalance, `max.poll.interval.ms`](../20-implement-gd2-du-lieu-phan-tan.md#84-consumer-group-rebalance-maxpollintervalms) | **chưa có** |
| ❌ | [20 · 8.5 Retry, DLT, backpressure](../20-implement-gd2-du-lieu-phan-tan.md#85-retry-dlt-backpressure) | **chưa có** |
| ❌ | [20 · 8.6 Stream processing — đủ để nói chuyện](../20-implement-gd2-du-lieu-phan-tan.md#86-stream-processing--đủ-để-nói-chuyện) | **chưa có** |
| ❌ | [20 · Lab 8 — Spring Boot + Kafka + Postgres (Thứ Bảy tuần 8 và 9)](../20-implement-gd2-du-lieu-phan-tan.md#lab-8--spring-boot--kafka--postgres-thứ-bảy-tuần-8-và-9) | **chưa có** |
| ❌ | [20 · Track P tuần 8–9 — P20: consumer chậm và rebalance storm](../20-implement-gd2-du-lieu-phan-tan.md#track-p-tuần-89--p20-consumer-chậm-và-rebalance-storm) | **chưa có** |
| ❌ | [20 · Track S tuần 8–9 — V7: payment/wallet có Kafka + Saga (Chủ nhật tuần 9, 40 phút)](../20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-89--v7-paymentwallet-có-kafka--saga-chủ-nhật-tuần-9-40-phút) | **chưa có** |
| ❌ | [20 · Bài tập tuần 8–9](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-89) | **chưa có** |
| ❌ | [20 · Bài nói 2 phút](../20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút-2) | **chưa có** |
| ❌ | [01 · P20 — Kafka consumer xử lý đồng bộ từng message ⭐](../01-java-code-cham-duoi-tai-cao.md#p20--kafka-consumer-xử-lý-đồng-bộ-từng-message-) | **chưa có** |
| ❌ | [01 · 7. Bắt chúng trên production — metric nào, công cụ nào](../01-java-code-cham-duoi-tai-cao.md#7-bắt-chúng-trên-production--metric-nào-công-cụ-nào) | **chưa có** |
| ❌ | [20 · Tuần 10 — Đồng thuận, khoá phân tán, đồng hồ](../20-implement-gd2-du-lieu-phan-tan.md#tuần-10--đồng-thuận-khoá-phân-tán-đồng-hồ) | **chưa có** |
| ❌ | [20 · Đầu ra phải có cuối tuần](../20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần-1) | **chưa có** |
| ❌ | [20 · Đọc](../20-implement-gd2-du-lieu-phan-tan.md#đọc-3) | **chưa có** |
| ❌ | [20 · Ghi chú khái niệm](../20-implement-gd2-du-lieu-phan-tan.md#ghi-chú-khái-niệm-3) | **chưa có** |
| ❌ | [20 · 10.1 Đồng hồ nói dối](../20-implement-gd2-du-lieu-phan-tan.md#101-đồng-hồ-nói-dối) | **chưa có** |
| ❌ | [20 · 10.2 Raft trong năm dòng](../20-implement-gd2-du-lieu-phan-tan.md#102-raft-trong-năm-dòng) | **chưa có** |
| ❌ | [20 · 10.3 Khoá phân tán: cho hiệu năng hay cho đúng đắn](../20-implement-gd2-du-lieu-phan-tan.md#103-khoá-phân-tán-cho-hiệu-năng-hay-cho-đúng-đắn) | **chưa có** |
| ❌ | [20 · Lab 10A — Fencing token (Thứ Bảy, 1,5 giờ)](../20-implement-gd2-du-lieu-phan-tan.md#lab-10a--fencing-token-thứ-bảy-15-giờ) | **chưa có** |
| ❌ | [20 · Lab 10B — Job scheduler phân tán (tùy chọn, Thứ Bảy, 2 giờ)](../20-implement-gd2-du-lieu-phan-tan.md#lab-10b--job-scheduler-phân-tán-tùy-chọn-thứ-bảy-2-giờ) | **chưa có** |
| ✅ | [20 · Track S tuần 10](../20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-10) | Ep06 |
| ❌ | [20 · Bài tập tuần 10](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-10) | **chưa có** |
| ❌ | [20 · Bài nói 2 phút](../20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút-3) | **chưa có** |
| ❌ | [20 · Capstone khởi động — tuần 9–10](../20-implement-gd2-du-lieu-phan-tan.md#capstone-khởi-động--tuần-910) | **chưa có** |
| ❌ | [20 · Mẫu design doc (`my-work/capstone/design-doc.md`)](../20-implement-gd2-du-lieu-phan-tan.md#mẫu-design-doc-my-workcapstonedesign-docmd) | **chưa có** |
| ❌ | [20 · Phạm vi MVP gợi ý](../20-implement-gd2-du-lieu-phan-tan.md#phạm-vi-mvp-gợi-ý) | **chưa có** |
| ❌ | [20 · Mốc tuần 10 — tiêu chí qua giai đoạn](../20-implement-gd2-du-lieu-phan-tan.md#mốc-tuần-10--tiêu-chí-qua-giai-đoạn) | **chưa có** |
| ❌ | [20 · Ranh giới trung thực](../20-implement-gd2-du-lieu-phan-tan.md#ranh-giới-trung-thực) | **chưa có** |
| ❌ | [00 · Dự án thực hành (capstone)](../00-lo-trinh-6-thang.md#dự-án-thực-hành-capstone) | **chưa có** |

## Theo mục nguồn

### 20 §0 · Trước khi bắt đầu

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#0-trước-khi-bắt-đầu](../20-implement-gd2-du-lieu-phan-tan.md#0-trước-khi-bắt-đầu)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Giai đoạn 2 phân biệt senior với mid-level: dữ liệu chạy thế nào khi có nhiều node, nhiều service và mạng chập chờn | Ep00 · 1:13 (Why phase 2) |
| ✅ | Điều kiện vào: đã qua mốc tuần 4; lab 3A (lost update) và lab 2 (Lua nguyên tử) là nền bắt buộc vì giai đoạn 2 lặp lại hai ý đó ở quy mô nhiều service | Ep00 · 1:13 (Why phase 2) |
| ✅ | Nhịp tuần giữ như giai đoạn 1; hai khối hai tuần (5–6 và 8–9): tuần đầu dùng Thứ Bảy để dựng, tuần sau để đo và thử phá | Ep00 · 3:01 (One week) |
| ✅ | Bản đồ 6 tuần: 5–6 replication, partitioning, consistent hashing (lab 5A, 5B, V6, đề KV + cache); 7 saga, outbox, idempotency, sổ cái kép (lab 7, payment + wallet); 8–9 Kafka (lab 8, P20, V7, ad click); 10 đồng thuận, khoá, đồng hồ (lab 10A, 10B, hotel + scheduler); capstone tuần 9–10 | Ep00 · 1:52 (The six weeks) |
| ✅ | Thư mục bài làm my-work/: w5-6-hashing, w7-saga, w8-9-kafka-lab, w10-coordination, capstone/design-doc.md và adr/, drawings V6, V7 | Ep00 · 3:30 (Your folders) |
| ✅ | Chạy Kafka không Docker: tải bản 4.1.2, kiểm sha512, format storage một lần; Kafka 4.x chỉ còn KRaft, không ZooKeeper; Postgres nhúng (embedded-postgres); có Docker thì Testcontainers | Ep00 · 3:51 (Kafka without Docker) |
| ✅ | Xem trước mốc tuần 10: giải thích 6 chủ đề không cần tài liệu, lab 8 bảy test xanh, lab 7 có recovery, D20 trước/sau, V6 V7, 6 đề bấm giờ ≥ 7/10, design doc v1 + khung MVP | Ep00 · 4:25 (Capstone and milestone) |
| ✅ | Cách dùng bộ video: mỗi video một ý lớn kèm bài tập theo thứ tự file 20; dừng ở đếm ngược; thẻ chữ viết tắt; 'measured' là số đã chạy (Postgres 16.4, Kafka 4.1.2 thật), 'assumption' là đoán; tab Video có thời điểm và link mục nguồn | Ep00 · 5:17 (How to use these videos) |

### 00 · Giai đoạn 2

Nguồn: [00-lo-trinh-6-thang.md#giai-đoạn-2--dữ-liệu-và-hệ-phân-tán-tuần-510](../00-lo-trinh-6-thang.md#giai-đoạn-2--dữ-liệu-và-hệ-phân-tán-tuần-510)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Lộ trình 00: tuần 5–10 cùng chủ đề, đọc DDIA các chương replication, partitioning, the trouble with distributed systems, consistency and consensus; xem bài giảng Distributed Systems của Martin Kleppmann | Ep00 · 1:52 (The six weeks) |

### 20 · Tuần 5–6: đầu ra

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần-6](../20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần-6)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Cuối tuần 6: lab 5A vòng băm có vnode và hệ số nhân bản 3; lab 5B mô phỏng quorum, tái hiện đọc cũ khi R + W ≤ N; đề KV store và distributed cache; V6 ở L2 có đoạn × 10; bài nói 2 phút về consistent hashing | Ep01 · 1:04 (Weeks 5–6) |

### 20 · Tuần 5–6: đọc

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#đọc](../20-implement-gd2-du-lieu-phan-tan.md#đọc)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Đọc: DDIA chương 5 (replication) và 6 (partitioning); Alex Xu Vol 1 chương 5, 6; bài báo Dynamo (2007) phần 4 — nguồn gốc sloppy quorum, hinted handoff; tuỳ chọn bài giảng của Kleppmann | Ep01 · 1:36 (Weeks 5–6) |

### 20 §5.1 · Ba mô hình replication

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#51-ba-mô-hình-replication](../20-implement-gd2-du-lieu-phan-tan.md#51-ba-mô-hình-replication)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Ba mô hình: single-leader (không xung đột; Postgres, MySQL, Kafka partition; hợp sổ cái), multi-leader (xung đột, giải bằng LWW, merge, CRDT; offline-first, active-active), leaderless (version + read repair; Cassandra, DynamoDB; LWW làm mất một lệnh ghi) | Ep01 · 2:04 (Three models) |
| ✅ | Đồng bộ: commit chờ replica xác nhận, +1–2 ms cùng region, ~90 ms nếu replica ở Singapore còn leader ở Sydney, không mất dữ liệu đã commit; bất đồng bộ: nhanh nhưng failover có thể mất vài giây ghi cuối | Ep01 · 3:07 (Three models) |
| ✅ | Ba dị thường khi đọc từ follower trễ: read-your-writes, monotonic reads (dữ liệu lùi về cũ), consistent prefix (câu trả lời trước câu hỏi); chữa read-your-writes như bài tập 3.4 giai đoạn 1 | Ep01 · 3:39 (Three models) |

### 20 · Bài tập tuần 5–6

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-56](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-56)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | BT 5.1: (a) đồng bộ Melbourne +12 ms, RPO = 0; (b) Singapore +90 ms, mất 1/3 ngân sách p99 300 ms; (c) đồng bộ khác AZ +1–2 ms + bất đồng bộ sang Melbourne: mất region thì mất vài giây ghi cuối, cần đối soát; ngân hàng chọn (c), (a) cho dữ liệu không được mất dòng nào | Ep01 · 4:44 (Exercise 5.1) |
| ✅ | BT 5.3: follower trễ 2 s được đẩy lên, sequence bigserial cấp lại ID đã dùng → hai giao dịch cùng ID, có thể lộ dữ liệu khách này cho khách khác (sự cố GitHub ở DDIA ch. 5); chống: ID không phụ thuộc DB (Snowflake, UUID), không tự failover sang replica trễ cho dữ liệu tiền, đối soát sau failover | Ep01 · 5:53 (Exercise 5.3) |
| ✅ | BT 5.2: (a) N=3,W=2,R=2 chịu 1 node chết cho ghi và đọc; (b) N=5,W=3,R=3 chịu 2; (c) W=1,R=1: R + W = 2 ≤ 3 có thể đọc cũ — chấp nhận cho lượt xem, trạng thái online, giỏ hàng; không chấp nhận cho số dư, tồn kho, hạn mức | Ep02 · 3:14 (Exercise 5.2) |
| ✅ | BT 5.4: merchant chiếm 30% giao dịch → shard của nó gánh 30% tải ghi và tranh khoá một dòng số dư; sửa theo thứ tự: sổ cái append-only (số dư tính từ bút toán), salting thành N tài khoản con (phải ≥ 16 lần số partition mới đều), gom bút toán theo lô vài trăm ms; đánh đổi: số dư không còn là một con số đọc trực tiếp | Ep03 · 3:26 (Exercise 5.4: a hot merchant) |
| ✅ | BT 5.5: tìm theo mã tham chiếu trên mọi tài khoản: index local + scatter-gather (latency bằng shard chậm nhất, tải nhân số shard, chấp nhận nếu hiếm) hoặc index global (bảng reference → account, txn shard theo reference, hoặc OpenSearch qua CDC; cập nhật bất đồng bộ); tổng đài ngân hàng: global qua CDC + thông báo giao dịch dưới 1 phút có thể chưa hiện | Ep03 · 4:39 (Exercise 5.5: lookup by reference) |
| ✅ | BT 5.6: mod N ~80% (đo 80,0%); vòng 100 vnode ~20% (đo 18,9%); 256 partition cố định: node mới nhận ~51 partition (256 ÷ 5) ≈ 20% key nhưng chuyển nguyên partition nên dễ quản lý, theo dõi, giới hạn băng thông; Kafka, Redis Cluster, Elasticsearch chọn cách này; đánh đổi: số partition phải đủ lớn từ đầu | Ep04 (Exercise 5.6) |

### 20 §5.2 · Quorum N, W, R

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#52-quorum-n-w-r](../20-implement-gd2-du-lieu-phan-tan.md#52-quorum-n-w-r)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | N bản sao, W bản xác nhận ghi, R bản trả lời đọc; R + W > N thì tập đọc và tập ghi luôn giao nhau nên đọc thấy ít nhất một bản mới nhất; N=3, W=2, R=2 chịu được 1 node chết cho cả đọc lẫn ghi | Ep02 · 0:44 (The rule) |
| ✅ | Chọn W, R theo từng request (khung đề KV store): mặc định W=2, R=2, chậm hơn W=1; công thức R + W > N cho biết đánh đổi giữa độ trễ, độ mới và số node được chết | Ep02 · 2:16 (What quorums don't promise) |
| ✅ | Quorum không cho linearizability: hai lệnh ghi đồng thời giải bằng LWW theo đồng hồ thì một lệnh mất im lặng; ghi thất bại ở bước W vẫn để lại bản ghi trên 1 replica, không rollback; sloppy quorum + hinted handoff (Dynamo) tăng availability nhưng phá đảm bảo R + W > N | Ep02 · 1:24 (What quorums don't promise) |

### 20 · Lab 5B — Mô phỏng quorum

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#lab-5b--mô-phỏng-quorum-thứ-bảy-tuần-6](../20-implement-gd2-du-lieu-phan-tan.md#lab-5b--mô-phỏng-quorum-thứ-bảy-tuần-6)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Lab 5B tự viết, không có lời giải: 3 replica (value, version); ghi tăng version, đủ W xác nhận; đọc R bản lấy version lớn nhất; bơm lỗi một replica chết, một replica chậm; test R=2,W=2 luôn mới nhất, R=1,W=1 đọc cũ, LWW đồng hồ lệch 20 ms mất một lệnh; read repair đo sau 1.000 lần đọc | Ep02 · 3:54 (Lab 5B) |
| ✅ | Lab 5B: hai lệnh ghi đồng thời, LWW theo đồng hồ lệch 20 ms → lệnh xảy ra sau bị mất, không báo lỗi; test phải chỉ ra lệnh nào | Ep02 · 4:35 (Lab 5B) |

### 20 §5.3 · Partitioning và rebalancing

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#53-partitioning-và-rebalancing](../20-implement-gd2-du-lieu-phan-tan.md#53-partitioning-và-rebalancing)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Bốn cách chia: theo khoảng khoá (đọc theo khoảng tốt, dễ lệch khi khoá theo thời gian), hash mod N (đều, rebalance thảm hoạ: đo được 80% key chuyển khi 4 → 5 node), vòng băm + vnode (chỉ ~1/(N+1) key chuyển: 18,9% với 100 vnode), số partition cố định (Kafka, Redis Cluster 16.384 slot, Elasticsearch; chuyển nguyên partition, số partition chọn trước) | Ep03 · 0:44 (Four ways to split) |
| ✅ | Vì sao hash mod N chuyển ~80% key khi 4 → 5 node: key chỉ ở yên khi hash mod 4 = hash mod 5, xảy ra với 1/5 số key; vòng băm chỉ chuyển phần của node mới | Ep03 · 1:49 (Four ways to split) |
| ✅ | Index phụ khi đã shard: local (mỗi shard index dữ liệu của mình, truy vấn phải hỏi mọi shard rồi gộp: scatter-gather) hoặc global (index tự shard theo giá trị; đọc nhanh nhưng cập nhật thường bất đồng bộ) | Ep03 · 2:28 (Secondary indexes) |

### 20 · Lab 5A — Vòng băm có virtual node

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#lab-5a--vòng-băm-có-virtual-node-thứ-bảy-tuần-5](../20-implement-gd2-du-lieu-phan-tan.md#lab-5a--vòng-băm-có-virtual-node-thứ-bảy-tuần-5)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Lab 5A bước 1: ConsistentHash với add, remove, nodeFor, tham số số vnode; TreeMap<Long, String> và tailMap (hoặc ceilingEntry); hash = MD5 nên chạy lại ra đúng số | Ep04 (Build the ring) |
| ✅ | Lab 5A đo 1 triệu key, 4 → 5 node: mod N 80%; vòng 1 vnode lệch 6,77 lần, node bận nhất 40,7%, chỉ 9,1% key chuyển; 10 vnode 2,20 / 16,4%; 100 vnode 1,11 / 18,9%; 200 vnode 1,19 / 19,8% | Ep04 (Measure it) |
| ✅ | Hai điều đọc ra: vnode tồn tại để làm đều (1 vnode: 40,7%, gấp 1,6 lần lý tưởng, node mới chỉ nhận 9,1%); 200 vnode lệch hơn 100 — với hàm băm cố định, nhiều vnode hơn chỉ đều hơn về kỳ vọng | Ep04 (Measure it) |
| ✅ | Lab 5A bước 3: hệ số nhân bản 3, nodesFor(key, 3) đi theo chiều kim đồng hồ lấy 3 node vật lý khác nhau (bỏ vnode trùng node); test: xoá một node chỉ đổi tập replica của các key node đó đang giữ | Ep04 (Three replicas) |

### 20 · Bài nói 2 phút tuần 5–6

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút](../20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Bài nói 'How does consistent hashing reduce data movement when you add a node?': mod N chuyển ~80% (số đo), vòng băm chỉ chuyển phần của node mới (~1/(N+1)), vnode để chia đều (1 vnode lệch 6,8 lần), đánh đổi: mất truy vấn theo khoảng, phải quản lý bản đồ vòng | Ep04 (Your 2-minute talk) |

### 20 · Bài tập tuần 5–6 (đề bấm giờ)

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-56](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-56)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Khung 45 phút (file 00): làm rõ yêu cầu 5–7 phút, ước lượng 3–5, API + data 5–8, tổng thể ~10, đào sâu 15–20 (2–3 điểm, mỗi điểm 2 phương án), tổng kết 3–5 kèm × 10 | Ep05 (45 minutes) |
| ✅ | BT 5.7 KV store (1/2): vòng băm + vnode (mất truy vấn theo khoảng); N=3 mỗi bản một AZ, gấp 3 dung lượng; quorum W/R chỉnh theo request (mặc định 2/2 chậm hơn W=1); xung đột bằng vector clock trả mọi bản cho client hoặc LWW nếu chấp nhận mất | Ep05 (5.7 Key-value store) |
| ✅ | BT 5.7 KV store (2/2): sloppy quorum + hinted handoff (phá R + W > N lúc sự cố); read repair + anti-entropy bằng Merkle tree; gossip + heartbeat hội tụ vài giây; LSM commit log → memtable → SSTable + bloom filter, compaction tốn I/O | Ep05 (5.7 Key-value store) |
| ✅ | BT 5.8 distributed cache: Redis Cluster 16.384 hash slot chia cho các master, mỗi master một replica khác AZ; client biết bản đồ slot, gọi thẳng node, redirect MOVED khi bản đồ đổi; resharding = chuyển slot | Ep05 (5.8 Distributed cache) |
| ✅ | Ước lượng cache trước khi vẽ (giả định nói to): 300k GET/s, một node Redis ~100k thao tác/s (con số nhẩm giai đoạn 1, một core) → 3 master ở 100%, chạy ≤ 50% → 6 master + replica = 12 node; 10 triệu key × 1 KB ≈ 10 GB | Ep05 (5.8 Distributed cache) |
| ✅ | BT 5.8 ba câu đào sâu: hot key (L1 Caffeine trước Redis hoặc nhân bản key), stampede (lab 4 giai đoạn 1), failover mất ghi (Redis nhân bản bất đồng bộ nên cache không bao giờ là nơi lưu chính dữ liệu tiền); thao tác nhiều khoá cần cùng slot: hash tag {account:42} | Ep05 (5.8 Distributed cache) |

### 20 · Track S tuần 5–6 — V6

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-56--v6-mini-core-transfer-ở-l2-chủ-nhật-tuần-6-30-phút](../20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-56--v6-mini-core-transfer-ở-l2-chủ-nhật-tuần-6-30-phút)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | V6 = vẽ lại V2 cho 1M users: ~12.000 CCU, ~2.000 RPS ngày lương, ghi ~150/giây | Ep06 (The numbers) |
| ✅ | Đáp án × 10 của V6: số mới 20.000 RPS, ghi 1.500/s, 120k CCU; vỡ trước: connection tới primary 80 pod × 10 = 800, rồi CPU primary; sửa: PgBouncer transaction pooling, lịch sử sang read model CQRS qua CDC (trễ vài trăm ms, thêm hệ thống); vỡ tiếp: sổ cái 650 GB/năm → partition theo tháng; chưa shard theo account_id vì ghi còn dư, shard làm chuyển tiền thành saga | Ep06 (The × 10 answer) |

### 02 §3.3 · L2 — 1M users

Nguồn: [02-ve-he-thong-100k-1m-10m.md#33-l2--1m-users-tách-đọc-tách-việc-chậm](../02-ve-he-thong-100k-1m-10m.md#33-l2--1m-users-tách-đọc-tách-việc-chậm)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Sơ đồ L2: CDN, WAF + ALB, API gateway/BFF (auth, rate limit), Identity (Keycloak/OIDC), core app monolith tách module 3–10 pod autoscale, Redis cluster cache-aside, Postgres primary + 2 read replica bất đồng bộ, outbox relay → Kafka/SQS → Notification service → FCM, APNs, SMS | Ep06 (The drawing) |
| ✅ | Mỗi hộp một con số: read replica vì đọc ~90% của 2.000 RPS (đánh đổi lag → read-your-writes); cache-aside hit 90% → DB thấy 1/10 (số dư dùng để quyết định vẫn đọc DB có khoá); outbox vì SMS 200–2.000 ms so với 300 ms p99; tách Notification trước (tải, lỗi khác, ít ràng buộc); API gateway thêm 1–3 ms, cần HA | Ep06 (The drawing) |

### 02 §5 · Câu hỏi × 10

Nguồn: [02-ve-he-thong-100k-1m-10m.md#5-câu-hỏi-tải-tăng-10-lần-thì-sao--cách-trả-lời](../02-ve-he-thong-100k-1m-10m.md#5-câu-hỏi-tải-tăng-10-lần-thì-sao--cách-trả-lời)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Khung 5 câu trả lời × 10: số mới, vỡ trước (vì con số nào), sửa và đánh đổi, vỡ tiếp, chưa làm gì và vì sao (chỗ ghi điểm senior) | Ep06 (The × 10 answer) |

### 20 · Track P tuần 5–6

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#track-p-tuần-56](../20-implement-gd2-du-lieu-phan-tan.md#track-p-tuần-56)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Track P tuần 5–6: không có anti-pattern mới; 30 phút Thứ Sáu chạy checklist 01 §6 trên code lab giai đoạn 1 của chính mình (rate limiter, db-lab, cache-lab), mỗi dòng 'có' ghi vào sổ lỗi kèm cách sửa; tập dượt cho review capstone tuần 15–16 | Ep06 (Track P and week 10) |

### 20 · Track S tuần 10

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-10](../20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-10)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Tuần 10 mở lại V6, thay số giả định bằng số đo lab 8: độ trễ commit → consumer (chu kỳ poll của relay chiếm phần lớn), số message trùng mỗi lần relay restart; ghi 'đã đo / còn giả định' cạnh mỗi con số | Ep06 (Track P and week 10) |

