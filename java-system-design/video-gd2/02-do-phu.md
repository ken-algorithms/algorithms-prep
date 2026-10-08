# Độ phủ nội dung — video giai đoạn 2

**147/147 ý chính đã có video.** Bảng sinh bằng `python -m lesson_video coverage` từ `points.yaml` (ý chính lấy từ tài liệu nguồn) và kịch bản `ep*.yaml`; mỗi ý có chữ bắt buộc (`expect`) mà lệnh `check` đã kiểm là có mặt trong cảnh dạy ý đó. Thời điểm lấy từ `lessons.json` sau khi dựng.

## Theo video

| Video | Dài | Chương | Ý chính |
|---|---:|---:|---:|
| Ep00 · Phase 2 orientation | 6:02 | 9 | 9 |
| Ep01 · Replication: leaders, followers, and lag | 7:11 | 7 | 7 |
| Ep02 · Quorums: N, W, R and what they don't promise | 6:16 | 7 | 6 |
| Ep03 · Partitioning: ranges, hashes, hot spots, secondary indexes | 5:50 | 7 | 5 |
| Ep04 · Consistent hashing: lab 5A and the 2-minute talk | 5:26 | 8 | 6 |
| Ep05 · Timed designs: a key-value store and a distributed cache | 6:14 | 6 | 6 |
| Ep06 · V6: Mini Core Transfer at 1M users | 5:57 | 6 | 7 |
| Ep07 · Why not two-phase commit: sagas instead | 6:12 | 7 | 8 |
| Ep08 · Outbox, idempotency at three layers, and the double-entry ledger | 5:38 | 7 | 8 |
| Ep09 · Lab 7: an interbank transfer saga that survives crashes | 4:57 | 7 | 6 |
| Ep10 · Saga drills: step order, isolation, and the pivot timeout | 4:50 | 7 | 4 |
| Ep11 · Timed designs: a payment system and a digital wallet | 4:42 | 5 | 4 |
| Ep12 · Kafka: ordering, keys, and durable writes | 4:56 | 7 | 7 |
| Ep13 · Kafka: delivery guarantees, consumer groups, and dead letters | 3:40 | 6 | 4 |
| Ep14 · Lab 8, part 1: the outbox and its relay on real Kafka | 4:57 | 8 | 5 |
| Ep15 · Lab 8, part 2: the idempotent consumer, and seven tests that prove it | 6:08 | 8 | 9 |
| Ep16 · Track P: P20, the slow consumer and the rebalance storm | 4:57 | 8 | 7 |
| Ep17 · Stream processing, and the ad click aggregation design | 4:27 | 6 | 7 |
| Ep18 · V7: an interbank transfer with Kafka and a saga | 3:02 | 6 | 2 |
| Ep19 · Clocks lie, and Raft in five lines | 5:37 | 7 | 8 |
| Ep20 · Distributed locks and fencing tokens | 5:19 | 8 | 8 |
| Ep21 · Timed designs: hotel reservation and a distributed job scheduler | 4:25 | 5 | 5 |
| Ep22 · Capstone kickoff and the week-10 milestone | 6:24 | 8 | 9 |

## Theo mục của tài liệu nguồn

**67/67 mục có video.** Mỗi mục là một heading trong phạm vi của tuần; mục có video khi nằm trong `covers` của video hoặc là nguồn của một ý chính đã dạy.

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
| ✅ | [20 · Tuần 7 — Transaction phân tán: Saga, outbox, idempotency, sổ cái kép](../20-implement-gd2-du-lieu-phan-tan.md#tuần-7--transaction-phân-tán-saga-outbox-idempotency-sổ-cái-kép) | Ep07 |
| ✅ | [20 · Đầu ra phải có cuối tuần](../20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần) | Ep07 |
| ✅ | [20 · Đọc](../20-implement-gd2-du-lieu-phan-tan.md#đọc-1) | Ep07 |
| ✅ | [20 · Ghi chú khái niệm](../20-implement-gd2-du-lieu-phan-tan.md#ghi-chú-khái-niệm-1) | Ep07 |
| ✅ | [20 · 7.1 Vì sao không 2PC giữa microservices](../20-implement-gd2-du-lieu-phan-tan.md#71-vì-sao-không-2pc-giữa-microservices) | Ep07 |
| ✅ | [20 · 7.2 Orchestration hay choreography](../20-implement-gd2-du-lieu-phan-tan.md#72-orchestration-hay-choreography) | Ep07 |
| ✅ | [20 · 7.3 Ba loại bước và thứ tự](../20-implement-gd2-du-lieu-phan-tan.md#73-ba-loại-bước-và-thứ-tự) | Ep07 |
| ✅ | [20 · 7.4 Dual write và outbox](../20-implement-gd2-du-lieu-phan-tan.md#74-dual-write-và-outbox) | Ep08 |
| ✅ | [20 · 7.5 Idempotency ở ba tầng](../20-implement-gd2-du-lieu-phan-tan.md#75-idempotency-ở-ba-tầng) | Ep08 |
| ✅ | [20 · 7.6 Sổ cái kép (double-entry)](../20-implement-gd2-du-lieu-phan-tan.md#76-sổ-cái-kép-double-entry) | Ep08 |
| ✅ | [20 · Lab 7 — Saga chuyển tiền liên ngân hàng (Thứ Bảy, 3–4 giờ)](../20-implement-gd2-du-lieu-phan-tan.md#lab-7--saga-chuyển-tiền-liên-ngân-hàng-thứ-bảy-34-giờ) | Ep09 |
| ✅ | [20 · Track P tuần 7 — P09 nhìn qua ranh giới saga](../20-implement-gd2-du-lieu-phan-tan.md#track-p-tuần-7--p09-nhìn-qua-ranh-giới-saga) | Ep09 |
| ✅ | [20 · Track S tuần 7](../20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-7) | Ep09 |
| ✅ | [20 · Bài tập tuần 7](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-7) | Ep08, Ep10, Ep11 |
| ✅ | [20 · Bài nói 2 phút](../20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút-1) | Ep07 |
| ✅ | [20 · Tuần 8–9 — Kafka sâu, lab outbox + consumer idempotent, P20](../20-implement-gd2-du-lieu-phan-tan.md#tuần-89--kafka-sâu-lab-outbox--consumer-idempotent-p20) | Ep12 |
| ✅ | [20 · Đầu ra phải có cuối tuần 9](../20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần-9) | Ep12 |
| ✅ | [20 · Đọc](../20-implement-gd2-du-lieu-phan-tan.md#đọc-2) | Ep12 |
| ✅ | [20 · Ghi chú khái niệm](../20-implement-gd2-du-lieu-phan-tan.md#ghi-chú-khái-niệm-2) | Ep12 |
| ✅ | [20 · 8.1 Thứ tự chỉ tồn tại trong một partition](../20-implement-gd2-du-lieu-phan-tan.md#81-thứ-tự-chỉ-tồn-tại-trong-một-partition) | Ep12 |
| ✅ | [20 · 8.2 Độ bền ghi](../20-implement-gd2-du-lieu-phan-tan.md#82-độ-bền-ghi) | Ep12 |
| ✅ | [20 · 8.3 Ba mức giao nhận](../20-implement-gd2-du-lieu-phan-tan.md#83-ba-mức-giao-nhận) | Ep13 |
| ✅ | [20 · 8.4 Consumer group, rebalance, `max.poll.interval.ms`](../20-implement-gd2-du-lieu-phan-tan.md#84-consumer-group-rebalance-maxpollintervalms) | Ep13 |
| ✅ | [20 · 8.5 Retry, DLT, backpressure](../20-implement-gd2-du-lieu-phan-tan.md#85-retry-dlt-backpressure) | Ep13 |
| ✅ | [20 · 8.6 Stream processing — đủ để nói chuyện](../20-implement-gd2-du-lieu-phan-tan.md#86-stream-processing--đủ-để-nói-chuyện) | Ep17 |
| ✅ | [20 · Lab 8 — Spring Boot + Kafka + Postgres (Thứ Bảy tuần 8 và 9)](../20-implement-gd2-du-lieu-phan-tan.md#lab-8--spring-boot--kafka--postgres-thứ-bảy-tuần-8-và-9) | Ep14, Ep15 |
| ✅ | [20 · Track P tuần 8–9 — P20: consumer chậm và rebalance storm](../20-implement-gd2-du-lieu-phan-tan.md#track-p-tuần-89--p20-consumer-chậm-và-rebalance-storm) | Ep16 |
| ✅ | [20 · Track S tuần 8–9 — V7: payment/wallet có Kafka + Saga (Chủ nhật tuần 9, 40 phút)](../20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-89--v7-paymentwallet-có-kafka--saga-chủ-nhật-tuần-9-40-phút) | Ep18 |
| ✅ | [20 · Bài tập tuần 8–9](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-89) | Ep12, Ep15, Ep17 |
| ✅ | [20 · Bài nói 2 phút](../20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút-2) | Ep15 |
| ✅ | [01 · P20 — Kafka consumer xử lý đồng bộ từng message ⭐](../01-java-code-cham-duoi-tai-cao.md#p20--kafka-consumer-xử-lý-đồng-bộ-từng-message-) | Ep16 |
| ✅ | [01 · 7. Bắt chúng trên production — metric nào, công cụ nào](../01-java-code-cham-duoi-tai-cao.md#7-bắt-chúng-trên-production--metric-nào-công-cụ-nào) | Ep16 |
| ✅ | [20 · Tuần 10 — Đồng thuận, khoá phân tán, đồng hồ](../20-implement-gd2-du-lieu-phan-tan.md#tuần-10--đồng-thuận-khoá-phân-tán-đồng-hồ) | Ep19 |
| ✅ | [20 · Đầu ra phải có cuối tuần](../20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần-1) | Ep19 |
| ✅ | [20 · Đọc](../20-implement-gd2-du-lieu-phan-tan.md#đọc-3) | Ep19 |
| ✅ | [20 · Ghi chú khái niệm](../20-implement-gd2-du-lieu-phan-tan.md#ghi-chú-khái-niệm-3) | Ep19 |
| ✅ | [20 · 10.1 Đồng hồ nói dối](../20-implement-gd2-du-lieu-phan-tan.md#101-đồng-hồ-nói-dối) | Ep19 |
| ✅ | [20 · 10.2 Raft trong năm dòng](../20-implement-gd2-du-lieu-phan-tan.md#102-raft-trong-năm-dòng) | Ep19 |
| ✅ | [20 · 10.3 Khoá phân tán: cho hiệu năng hay cho đúng đắn](../20-implement-gd2-du-lieu-phan-tan.md#103-khoá-phân-tán-cho-hiệu-năng-hay-cho-đúng-đắn) | Ep20 |
| ✅ | [20 · Lab 10A — Fencing token (Thứ Bảy, 1,5 giờ)](../20-implement-gd2-du-lieu-phan-tan.md#lab-10a--fencing-token-thứ-bảy-15-giờ) | Ep20 |
| ✅ | [20 · Lab 10B — Job scheduler phân tán (tùy chọn, Thứ Bảy, 2 giờ)](../20-implement-gd2-du-lieu-phan-tan.md#lab-10b--job-scheduler-phân-tán-tùy-chọn-thứ-bảy-2-giờ) | Ep20 |
| ✅ | [20 · Track S tuần 10](../20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-10) | Ep06 |
| ✅ | [20 · Bài tập tuần 10](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-10) | Ep19, Ep20, Ep21 |
| ✅ | [20 · Bài nói 2 phút](../20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút-3) | Ep20 |
| ✅ | [20 · Capstone khởi động — tuần 9–10](../20-implement-gd2-du-lieu-phan-tan.md#capstone-khởi-động--tuần-910) | Ep22 |
| ✅ | [20 · Mẫu design doc (`my-work/capstone/design-doc.md`)](../20-implement-gd2-du-lieu-phan-tan.md#mẫu-design-doc-my-workcapstonedesign-docmd) | Ep22 |
| ✅ | [20 · Phạm vi MVP gợi ý](../20-implement-gd2-du-lieu-phan-tan.md#phạm-vi-mvp-gợi-ý) | Ep22 |
| ✅ | [20 · Mốc tuần 10 — tiêu chí qua giai đoạn](../20-implement-gd2-du-lieu-phan-tan.md#mốc-tuần-10--tiêu-chí-qua-giai-đoạn) | Ep22 |
| ✅ | [20 · Ranh giới trung thực](../20-implement-gd2-du-lieu-phan-tan.md#ranh-giới-trung-thực) | Ep22 |
| ✅ | [00 · Dự án thực hành (capstone)](../00-lo-trinh-6-thang.md#dự-án-thực-hành-capstone) | Ep22 |

## Theo mục nguồn

### 20 §0 · Trước khi bắt đầu

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#0-trước-khi-bắt-đầu](../20-implement-gd2-du-lieu-phan-tan.md#0-trước-khi-bắt-đầu)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Giai đoạn 2 phân biệt senior với mid-level: dữ liệu chạy thế nào khi có nhiều node, nhiều service và mạng chập chờn | Ep00 · 1:13 (Why phase 2) |
| ✅ | Điều kiện vào: đã qua mốc tuần 4; lab 3A (lost update) và lab 2 (Lua nguyên tử) là nền bắt buộc vì giai đoạn 2 lặp lại hai ý đó ở quy mô nhiều service | Ep00 · 1:13 (Why phase 2) |
| ✅ | Nhịp tuần giữ như giai đoạn 1; hai khối hai tuần (5–6 và 8–9): tuần đầu dùng Thứ Bảy để dựng, tuần sau để đo và thử phá | Ep00 · 3:00 (One week) |
| ✅ | Bản đồ 6 tuần: 5–6 replication, partitioning, consistent hashing (lab 5A, 5B, V6, đề KV + cache); 7 saga, outbox, idempotency, sổ cái kép (lab 7, payment + wallet); 8–9 Kafka (lab 8, P20, V7, ad click); 10 đồng thuận, khoá, đồng hồ (lab 10A, 10B, hotel + scheduler); capstone tuần 9–10 | Ep00 · 1:51 (The six weeks) |
| ✅ | Thư mục bài làm my-work/: w5-6-hashing, w7-saga, w8-9-kafka-lab, w10-coordination, capstone/design-doc.md và adr/, drawings V6, V7 | Ep00 · 3:30 (Your folders) |
| ✅ | Chạy Kafka không Docker: tải bản 4.1.2, kiểm sha512, format storage một lần; Kafka 4.x chỉ còn KRaft, không ZooKeeper; Postgres nhúng (embedded-postgres); có Docker thì Testcontainers | Ep00 · 3:50 (Kafka without Docker) |
| ✅ | Xem trước mốc tuần 10: giải thích 6 chủ đề không cần tài liệu, lab 8 bảy test xanh, lab 7 có recovery, D20 trước/sau, V6 V7, 6 đề bấm giờ ≥ 7/10, design doc v1 + khung MVP | Ep00 · 4:25 (Capstone and milestone) |
| ✅ | Cách dùng bộ video: mỗi video một ý lớn kèm bài tập theo thứ tự file 20; dừng ở đếm ngược; thẻ chữ viết tắt; 'measured' là số đã chạy (Postgres 16.4, Kafka 4.1.2 thật), 'assumption' là đoán; tab Video có thời điểm và link mục nguồn | Ep00 · 5:17 (How to use these videos) |

### 00 · Giai đoạn 2

Nguồn: [00-lo-trinh-6-thang.md#giai-đoạn-2--dữ-liệu-và-hệ-phân-tán-tuần-510](../00-lo-trinh-6-thang.md#giai-đoạn-2--dữ-liệu-và-hệ-phân-tán-tuần-510)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Lộ trình 00: tuần 5–10 cùng chủ đề, đọc DDIA các chương replication, partitioning, the trouble with distributed systems, consistency and consensus; xem bài giảng Distributed Systems của Martin Kleppmann | Ep00 · 1:51 (The six weeks) |

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
| ✅ | Ba dị thường khi đọc từ follower trễ: read-your-writes, monotonic reads (dữ liệu lùi về cũ), consistent prefix (câu trả lời trước câu hỏi); chữa read-your-writes như bài tập 3.4 giai đoạn 1 | Ep01 · 3:38 (Three models) |

### 20 · Bài tập tuần 5–6

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-56](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-56)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | BT 5.1: (a) đồng bộ Melbourne +12 ms, RPO = 0; (b) Singapore +90 ms, mất 1/3 ngân sách p99 300 ms; (c) đồng bộ khác AZ +1–2 ms + bất đồng bộ sang Melbourne: mất region thì mất vài giây ghi cuối, cần đối soát; ngân hàng chọn (c), (a) cho dữ liệu không được mất dòng nào | Ep01 · 4:44 (Exercise 5.1) |
| ✅ | BT 5.3: follower trễ 2 s được đẩy lên, sequence bigserial cấp lại ID đã dùng → hai giao dịch cùng ID, có thể lộ dữ liệu khách này cho khách khác (sự cố GitHub ở DDIA ch. 5); chống: ID không phụ thuộc DB (Snowflake, UUID), không tự failover sang replica trễ cho dữ liệu tiền, đối soát sau failover | Ep01 · 5:53 (Exercise 5.3) |
| ✅ | BT 5.2: (a) N=3,W=2,R=2 chịu 1 node chết cho ghi và đọc; (b) N=5,W=3,R=3 chịu 2; (c) W=1,R=1: R + W = 2 ≤ 3 có thể đọc cũ — chấp nhận cho lượt xem, trạng thái online, giỏ hàng; không chấp nhận cho số dư, tồn kho, hạn mức | Ep02 · 3:14 (Exercise 5.2) |
| ✅ | BT 5.4: merchant chiếm 30% giao dịch → shard của nó gánh 30% tải ghi và tranh khoá một dòng số dư; sửa theo thứ tự: sổ cái append-only (số dư tính từ bút toán), salting thành N tài khoản con (phải ≥ 16 lần số partition mới đều), gom bút toán theo lô vài trăm ms; đánh đổi: số dư không còn là một con số đọc trực tiếp | Ep03 · 3:26 (Exercise 5.4: a hot merchant) |
| ✅ | BT 5.5: tìm theo mã tham chiếu trên mọi tài khoản: index local + scatter-gather (latency bằng shard chậm nhất, tải nhân số shard, chấp nhận nếu hiếm) hoặc index global (bảng reference → account, txn shard theo reference, hoặc OpenSearch qua CDC; cập nhật bất đồng bộ); tổng đài ngân hàng: global qua CDC + thông báo giao dịch dưới 1 phút có thể chưa hiện | Ep03 · 4:39 (Exercise 5.5: lookup by reference) |
| ✅ | BT 5.6: mod N ~80% (đo 80,0%); vòng 100 vnode ~20% (đo 18,9%); 256 partition cố định: node mới nhận ~51 partition (256 ÷ 5) ≈ 20% key nhưng chuyển nguyên partition nên dễ quản lý, theo dõi, giới hạn băng thông; Kafka, Redis Cluster, Elasticsearch chọn cách này; đánh đổi: số partition phải đủ lớn từ đầu | Ep04 · 3:24 (Exercise 5.6) |

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
| ✅ | Lab 5A bước 1: ConsistentHash với add, remove, nodeFor, tham số số vnode; TreeMap<Long, String> và tailMap (hoặc ceilingEntry); hash = MD5 nên chạy lại ra đúng số | Ep04 · 0:39 (Build the ring) |
| ✅ | Lab 5A đo 1 triệu key, 4 → 5 node: mod N 80%; vòng 1 vnode lệch 6,77 lần, node bận nhất 40,7%, chỉ 9,1% key chuyển; 10 vnode 2,20 / 16,4%; 100 vnode 1,11 / 18,9%; 200 vnode 1,19 / 19,8% | Ep04 · 1:17 (Measure it) |
| ✅ | Hai điều đọc ra: vnode tồn tại để làm đều (1 vnode: 40,7%, gấp 1,6 lần lý tưởng, node mới chỉ nhận 9,1%); 200 vnode lệch hơn 100 — với hàm băm cố định, nhiều vnode hơn chỉ đều hơn về kỳ vọng | Ep04 · 2:04 (Measure it) |
| ✅ | Lab 5A bước 3: hệ số nhân bản 3, nodesFor(key, 3) đi theo chiều kim đồng hồ lấy 3 node vật lý khác nhau (bỏ vnode trùng node); test: xoá một node chỉ đổi tập replica của các key node đó đang giữ | Ep04 · 2:35 (Three replicas) |

### 20 · Bài nói 2 phút tuần 5–6

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút](../20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Bài nói 'How does consistent hashing reduce data movement when you add a node?': mod N chuyển ~80% (số đo), vòng băm chỉ chuyển phần của node mới (~1/(N+1)), vnode để chia đều (1 vnode lệch 6,8 lần), đánh đổi: mất truy vấn theo khoảng, phải quản lý bản đồ vòng | Ep04 · 4:03 (Your 2-minute talk) |

### 20 · Bài tập tuần 5–6 (đề bấm giờ)

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-56](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-56)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Khung 45 phút (file 00): làm rõ yêu cầu 5–7 phút, ước lượng 3–5, API + data 5–8, tổng thể ~10, đào sâu 15–20 (2–3 điểm, mỗi điểm 2 phương án), tổng kết 3–5 kèm × 10 | Ep05 · 1:02 (45 minutes) |
| ✅ | BT 5.7 KV store (1/2): vòng băm + vnode (mất truy vấn theo khoảng); N=3 mỗi bản một AZ, gấp 3 dung lượng; quorum W/R chỉnh theo request (mặc định 2/2 chậm hơn W=1); xung đột bằng vector clock trả mọi bản cho client hoặc LWW nếu chấp nhận mất | Ep05 · 2:03 (5.7 Key-value store) |
| ✅ | BT 5.7 KV store (2/2): sloppy quorum + hinted handoff (phá R + W > N lúc sự cố); read repair + anti-entropy bằng Merkle tree; gossip + heartbeat hội tụ vài giây; LSM commit log → memtable → SSTable + bloom filter, compaction tốn I/O | Ep05 · 2:35 (5.7 Key-value store) |
| ✅ | BT 5.8 distributed cache: Redis Cluster 16.384 hash slot chia cho các master, mỗi master một replica khác AZ; client biết bản đồ slot, gọi thẳng node, redirect MOVED khi bản đồ đổi; resharding = chuyển slot | Ep05 · 3:44 (5.8 Distributed cache) |
| ✅ | Ước lượng cache trước khi vẽ (giả định nói to): 300k GET/s, một node Redis ~100k thao tác/s (con số nhẩm giai đoạn 1, một core) → 3 master ở 100%, chạy ≤ 50% → 6 master + replica = 12 node; 10 triệu key × 1 KB ≈ 10 GB | Ep05 · 4:15 (5.8 Distributed cache) |
| ✅ | BT 5.8 ba câu đào sâu: hot key (L1 Caffeine trước Redis hoặc nhân bản key), stampede (lab 4 giai đoạn 1), failover mất ghi (Redis nhân bản bất đồng bộ nên cache không bao giờ là nơi lưu chính dữ liệu tiền); thao tác nhiều khoá cần cùng slot: hash tag {account:42} | Ep05 · 4:49 (5.8 Distributed cache) |

### 20 · Track S tuần 5–6 — V6

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-56--v6-mini-core-transfer-ở-l2-chủ-nhật-tuần-6-30-phút](../20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-56--v6-mini-core-transfer-ở-l2-chủ-nhật-tuần-6-30-phút)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | V6 = vẽ lại V2 cho 1M users: ~12.000 CCU, ~2.000 RPS ngày lương, ghi ~150/giây | Ep06 · 1:21 (The numbers) |
| ✅ | Đáp án × 10 của V6: số mới 20.000 RPS, ghi 1.500/s, 120k CCU; vỡ trước: connection tới primary 80 pod × 10 = 800, rồi CPU primary; sửa: PgBouncer transaction pooling, lịch sử sang read model CQRS qua CDC (trễ vài trăm ms, thêm hệ thống); vỡ tiếp: sổ cái 650 GB/năm → partition theo tháng; chưa shard theo account_id vì ghi còn dư, shard làm chuyển tiền thành saga | Ep06 · 3:49 (The × 10 answer) |

### 02 §3.3 · L2 — 1M users

Nguồn: [02-ve-he-thong-100k-1m-10m.md#33-l2--1m-users-tách-đọc-tách-việc-chậm](../02-ve-he-thong-100k-1m-10m.md#33-l2--1m-users-tách-đọc-tách-việc-chậm)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Sơ đồ L2: CDN, WAF + ALB, API gateway/BFF (auth, rate limit), Identity (Keycloak/OIDC), core app monolith tách module 3–10 pod autoscale, Redis cluster cache-aside, Postgres primary + 2 read replica bất đồng bộ, outbox relay → Kafka/SQS → Notification service → FCM, APNs, SMS | Ep06 · 1:46 (The drawing) |
| ✅ | Mỗi hộp một con số: read replica vì đọc ~90% của 2.000 RPS (đánh đổi lag → read-your-writes); cache-aside hit 90% → DB thấy 1/10 (số dư dùng để quyết định vẫn đọc DB có khoá); outbox vì SMS 200–2.000 ms so với 300 ms p99; tách Notification trước (tải, lỗi khác, ít ràng buộc); API gateway thêm 1–3 ms, cần HA | Ep06 · 2:40 (The drawing) |

### 02 §5 · Câu hỏi × 10

Nguồn: [02-ve-he-thong-100k-1m-10m.md#5-câu-hỏi-tải-tăng-10-lần-thì-sao--cách-trả-lời](../02-ve-he-thong-100k-1m-10m.md#5-câu-hỏi-tải-tăng-10-lần-thì-sao--cách-trả-lời)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Khung 5 câu trả lời × 10: số mới, vỡ trước (vì con số nào), sửa và đánh đổi, vỡ tiếp, chưa làm gì và vì sao (chỗ ghi điểm senior) | Ep06 · 3:49 (The × 10 answer) |

### 20 · Track P tuần 5–6

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#track-p-tuần-56](../20-implement-gd2-du-lieu-phan-tan.md#track-p-tuần-56)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Track P tuần 5–6: không có anti-pattern mới; 30 phút Thứ Sáu chạy checklist 01 §6 trên code lab giai đoạn 1 của chính mình (rate limiter, db-lab, cache-lab), mỗi dòng 'có' ghi vào sổ lỗi kèm cách sửa; tập dượt cho review capstone tuần 15–16 | Ep06 · 5:05 (Track P and week 10) |

### 20 · Track S tuần 10

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-10](../20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-10)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Tuần 10 mở lại V6, thay số giả định bằng số đo lab 8: độ trễ commit → consumer (chu kỳ poll của relay chiếm phần lớn), số message trùng mỗi lần relay restart; ghi 'đã đo / còn giả định' cạnh mỗi con số | Ep06 · 5:05 (Track P and week 10) |

### 20 · Tuần 7: đầu ra

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần](../20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Cuối tuần 7: lab 7 saga orchestrator lưu trạng thái trong Postgres, bù đúng thứ tự, tiếp tục sau crash, mỗi bước idempotent; đề Payment system và Digital wallet; query bất biến sổ cái (BT 7.6) chạy trên dữ liệu lab; bài nói 2 phút về 2PC | Ep07 · 1:00 (Week 7) |

### 20 · Tuần 7: đọc

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#đọc-1](../20-implement-gd2-du-lieu-phan-tan.md#đọc-1)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Đọc tuần 7: Microservices Patterns (Chris Richardson) ch. 4 saga và phần transactional outbox ch. 3; Alex Xu Vol 2 ch. 11 Payment system, ch. 12 Digital wallet; nab-prep/04 (câu hỏi đích danh vòng EM ở NAB); doc 06 phần II thiết kế lại sổ cái | Ep07 · 1:30 (Week 7) |

### 20 §7.1 · Vì sao không 2PC giữa microservices

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#71-vì-sao-không-2pc-giữa-microservices](../20-implement-gd2-du-lieu-phan-tan.md#71-vì-sao-không-2pc-giữa-microservices)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | 2PC giữ khoá trên mọi service suốt hai pha qua mạng; coordinator chết giữa hai pha thì các bên treo với khoá đang giữ; availability là tích (5 service 99,9% → 99,5%); Kafka, phần lớn NoSQL, API REST của đối tác không tham gia 2PC được | Ep07 · 2:39 (What 2PC costs) |
| ✅ | Saga đổi nhất quán tức thời lấy nhất quán cuối cùng + bù trừ, và phải tự xử lý phần thiếu isolation | Ep07 · 2:39 (What 2PC costs) |

### 20 §7.2 · Orchestration hay choreography

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#72-orchestration-hay-choreography](../20-implement-gd2-du-lieu-phan-tan.md#72-orchestration-hay-choreography)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Orchestration: một orchestrator gửi lệnh, nhận phản hồi, lưu trạng thái; nhìn luồng ở một chỗ; hợp luồng tiền nhiều bước cần bù chính xác; rủi ro 'god service'. Choreography: mỗi service nghe sự kiện tự làm bước của mình; luồng rải khắp nơi; hợp fan-out đơn giản ít bù; rủi ro vòng lặp sự kiện | Ep07 · 3:25 (Orchestration or choreography) |

### 20 §7.3 · Ba loại bước và thứ tự

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#73-ba-loại-bước-và-thứ-tự](../20-implement-gd2-du-lieu-phan-tan.md#73-ba-loại-bước-và-thứ-tự)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Ba loại bước: compensatable (có bù: giữ tiền ↔ nhả tiền), pivot (điểm không quay lại: gửi lệnh sang ngân hàng đối tác, trừ thẻ), retriable (sau pivot, retry đủ là thành công: ghi sổ cái cuối, gửi thông báo); thứ tự đúng compensatable → pivot → retriable; bước có thể thất bại đặt sau pivot là tự tạo trạng thái không bù được | Ep07 · 4:14 (Three kinds of steps) |
| ✅ | Saga thiếu isolation, cần biện pháp: semantic lock (trạng thái PENDING/giữ tiền), cập nhật giao hoán (cộng/trừ thay vì ghi đè), đọc lại giá trị trước khi ghi | Ep07 · 4:55 (Three kinds of steps) |

### 20 · Bài nói 2 phút tuần 7

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút-1](../20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút-1)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Bài nói 'Why wouldn't you use two-phase commit between microservices?': khoá giữ qua mạng, coordinator chết thì treo, availability nhân lên, nhiều hệ thống không tham gia được; thay bằng saga + outbox + idempotency; đánh đổi: mất isolation, cần trạng thái PENDING và đối soát | Ep07 · 5:21 (Your 2-minute talk) |

### 20 §7.4 · Dual write và outbox

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#74-dual-write-và-outbox](../20-implement-gd2-du-lieu-phan-tan.md#74-dual-write-và-outbox)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Dual write: db.save(transfer); kafka.send(event) là hai hệ thống không commit cùng nhau, hỏng được ở giữa: DB commit mà event mất, hoặc event đi mà DB rollback | Ep08 · 0:44 (Dual write) |
| ✅ | Outbox: ghi event vào bảng outbox trong cùng transaction với dữ liệu nghiệp vụ (một lần commit), rồi một relay đọc bảng đó, gửi và đánh dấu đã gửi | Ep08 · 1:15 (Dual write) |
| ✅ | Polling relay (không thêm hạ tầng, trễ bằng chu kỳ poll 100–500 ms, query poll liên tục, giữ thứ tự chỉ khi một relay) so với CDC Debezium đọc WAL (Kafka Connect + Debezium, quyền replication slot, trễ vài chục ms, gần như không thêm tải, theo thứ tự commit) | Ep08 · 1:36 (Dual write) |
| ✅ | Cả hai đều at-least-once: relay gửi xong rồi chết trước khi đánh dấu thì gửi lại; vì vậy consumer luôn phải idempotent | Ep08 · 2:16 (Dual write) |

### 20 §7.5 · Idempotency ở ba tầng

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#75-idempotency-ở-ba-tầng](../20-implement-gd2-du-lieu-phan-tan.md#75-idempotency-ở-ba-tầng)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Idempotency ở ba tầng: API (Idempotency-Key, bài 2.4 giai đoạn 1); consumer (bảng processed_event(event_id primary key) insert trong cùng transaction với tác dụng phụ, lab 8); gọi ra ngoài (gửi khoá idempotency sang ngân hàng, PSP để khi timeout còn hỏi lại) | Ep08 · 2:33 (Idempotency, three layers) |

### 20 §7.6 · Sổ cái kép

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#76-sổ-cái-kép-double-entry](../20-implement-gd2-du-lieu-phan-tan.md#76-sổ-cái-kép-double-entry)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Sổ cái kép: mỗi giao dịch ít nhất hai bút toán, tổng bằng 0; bút toán chỉ thêm, không sửa, không xoá; sai thì ghi bút toán đảo; số dư là tổng bút toán (hoặc cột duy trì cùng transaction và đối soát với tổng) | Ep08 · 3:11 (Double-entry ledger) |
| ✅ | Sửa sai không xoá gì: chuyển nhầm thì ghi bút toán đảo (cùng bút toán, đổi dấu) rồi ghi giao dịch đúng; số dư vẫn là tổng bút toán, mọi giao dịch tổng bằng 0 | Ep08 · 3:42 (Double-entry ledger) |

### 20 · Bài tập tuần 7

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-7](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-7)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | BT 7.6: query (a) group by transfer_id having sum(amount) <> 0 phải rỗng; (b) balance khác opening_balance + coalesce(sum(amount), 0) phải rỗng; chạy như job đối soát mỗi giờ, alert khi khác rỗng; lab 8 in đúng hai con số này | Ep08 · 4:35 (Exercise 7.6) |

### 20 · Lab 7 — Saga chuyển tiền liên ngân hàng

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#lab-7--saga-chuyển-tiền-liên-ngân-hàng-thứ-bảy-34-giờ](../20-implement-gd2-du-lieu-phan-tan.md#lab-7--saga-chuyển-tiền-liên-ngân-hàng-thứ-bảy-34-giờ)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Năm bước: (1) giữ tiền available −= x, held += x — compensatable, bù = nhả tiền; (2) kiểm tra gian lận/hạn mức — compensatable chỉ đọc; (3) gửi lệnh sang ngân hàng đối tác kèm idempotency key — pivot; (4) ghi sổ cái held −= x, hai bút toán — retriable; (5) gửi thông báo — retriable | Ep09 · 0:46 (Five steps) |
| ✅ | Bắt buộc: bảng saga_instance(id, type, state, current_step, payload jsonb, version, updated_at); mỗi bước một transaction, cập nhật current_step cùng transaction với tác dụng phụ; không gọi đối tác trong transaction (P09); recovery job mỗi 10 s chạy tiếp saga đứng quá 30 s từ current_step nên mỗi bước phải chạy lại an toàn; timeout ở bước 3 không bù mù, hỏi lại bằng idempotency key | Ep09 · 1:19 (The rules) |
| ✅ | Sáu test: happyPath (COMPLETED, 2 bút toán tổng 0, held = 0), fraudRejects_releasesHold, partnerRejects_compensatesInReverse (bù 2 rồi 1), partnerTimeout_queriesBeforeCompensating (đối tác đã nhận → đi tiếp, không bù), crashAfterStep3_recoveryResumes (không gửi bước 3 lần hai), duplicateCommand_singleSaga | Ep09 · 2:42 (Six tests) |
| ✅ | Khung Saga.java đã chạy (trong bộ nhớ): chạy từng bước, lỗi thì bù theo thứ tự ngược; output COMPLETED (ví 700, merchant 300) và COMPENSATED khi merchant bank timeout (UNDO fraud check, UNDO reserve → ví 1.000, merchant 0); phần lưu trạng thái, recovery, pivot timeout là việc của lab | Ep09 · 3:27 (The core, already run) |

### 20 · Track P tuần 7 — P09 qua ranh giới saga

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#track-p-tuần-7--p09-nhìn-qua-ranh-giới-saga](../20-implement-gd2-du-lieu-phan-tan.md#track-p-tuần-7--p09-nhìn-qua-ranh-giới-saga)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | P09 nhìn qua saga: mỗi bước một transaction ngắn, lời gọi ra ngoài nằm giữa hai transaction, trạng thái saga thay cho một transaction dài; bài tập: viết lại transfer() của P09 thành 3 bước saga, chỉ rõ transaction nào giữ khoá dòng nào, bao lâu | Ep09 · 4:07 (Track P and Track S) |

### 20 · Track S tuần 7

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-7](../20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-7)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Phác V7: luồng lệnh và sự kiện giữa orchestrator, ledger, fraud, cổng ngân hàng đối tác, notification; chỉ cần đúng các mũi tên và chỗ nào là pivot | Ep09 · 4:07 (Track P and Track S) |

### 20 · Bài tập tuần 7 (7.1–7.4)

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-7](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-7)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | BT 7.1 nạp ví bằng thẻ: thứ tự (a) tạo bản ghi PENDING → (e) kiểm tra hạn mức → (b) trừ thẻ qua PSP là pivot → (c) cộng ví retriable, idempotent theo id lần nạp → (d) email; đặt (e) sau (b) là sai: vượt hạn mức thì phải hoàn tiền thẻ, chậm, có phí, khách thấy hai dòng sao kê | Ep10 · 0:52 (7.1 Order the steps) |
| ✅ | BT 7.2: tách hai con số: số dư sổ cái 10 triệu (chưa có bút toán), số dư khả dụng 5 triệu (đã trừ phần giữ); app hiện cả hai; lệnh 8 triệu kiểm trên số dư khả dụng nên bị từ chối; đây là semantic lock | Ep10 · 1:53 (7.2 Missing isolation) |
| ✅ | BT 7.3 pivot timeout: bù ngay là sai (đối tác có thể đã chuyển tiền → mất tiền); gửi lại chỉ an toàn với cùng idempotency key và đối tác khử trùng; đúng: chuyển saga sang UNKNOWN, hỏi trạng thái theo idempotency key có backoff, vẫn không rõ thì vào hàng đợi đối soát (file đối soát cuối ngày), báo khách 'đang xử lý' | Ep10 · 2:46 (7.3 Pivot timeout) |
| ✅ | BT 7.4: chuyển tiền liên ngân hàng → orchestration; cập nhật điểm thưởng, thông báo, read model sau giao dịch → choreography (publish transfer.completed); mở tài khoản → orchestration cho eKYC → tạo tài khoản core → cấp thẻ, rồi choreography cho email chào mừng | Ep10 · 3:50 (7.4 Pick a style) |

### 20 · Bài tập tuần 7 (7.5, 7.7)

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-7](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-7)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | BT 7.5 Payment system: client → Payment API (Idempotency-Key) → payment service tạo payment CREATED → payment executor → PSP (kèm idempotency key, trang thanh toán do PSP host, không chạm số thẻ) → webhook/poll → SUCCEEDED/FAILED → ledger (double-entry) → wallet → outbox → thông báo; đối soát file settlement của PSP mỗi ngày với sổ cái, lệch vào hàng đợi xử lý tay | Ep11 · 1:09 (7.5 Payment system) |
| ✅ | BT 7.5: một dòng payment với trạng thái rõ: CREATED (lưu kèm idempotency key trước khi gọi ra ngoài) → gửi PSP cùng key mỗi lần retry → SUCCEEDED/FAILED qua webhook hoặc poll → chỉ SUCCEEDED mới ghi sổ cái; timeout thì UNKNOWN, hỏi lại theo key, rồi file settlement quyết định | Ep11 · 1:51 (7.5 Payment system) |
| ✅ | BT 7.5 ba điểm đào sâu: exactly-once = retry at-least-once + idempotency key ở cả hai đầu; kết quả không rõ (như 7.3); đối soát là thứ chứng minh hệ thống đúng, không phải tin code; không lưu số thẻ để khỏi gánh phạm vi PCI DSS | Ep11 · 2:25 (7.5 Payment system) |
| ✅ | BT 7.7 Digital wallet: Postgres + sổ cái append-only + outbox; 250 TPS × 6 ghi = 1.500 ghi/giây vừa một primary; event sourcing + Raft (sách: 1 triệu TPS) giải bài toán vượt trần một primary; ngưỡng chuyển: ghi chạm ~50–70% năng lực đo được, hoặc khi audit cần replay lịch sử | Ep11 · 3:32 (7.7 Digital wallet) |

### 20 · Tuần 8–9: đầu ra

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần-9](../20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần-9)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Cuối tuần 9: lab 8 Spring Boot + Kafka + Postgres, 7 test có test tái hiện gửi trùng do relay crash và chứng minh consumer chỉ áp dụng một lần; chạy D20 (P20) và sửa một consumer; V7; đề Ad click aggregation; design doc capstone v1; bài nói 2 phút về exactly-once | Ep12 · 0:25 (Weeks 8–9) |

### 20 · Tuần 8–9: đọc

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#đọc-2](../20-implement-gd2-du-lieu-phan-tan.md#đọc-2)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Đọc: Kafka: The Definitive Guide bản 2 (ch. 3–4 producer, consumer; ch. 6–8 internals, reliable delivery, exactly-once); Alex Xu Vol 2 ch. 4 message queue, ch. 6 ad click aggregation; DDIA ch. 11 stream processing; 01 P20 kèm chạy D20 | Ep12 · 1:00 (Weeks 8–9) |

### 20 §8.1 · Thứ tự chỉ tồn tại trong một partition

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#81-thứ-tự-chỉ-tồn-tại-trong-một-partition](../20-implement-gd2-du-lieu-phan-tan.md#81-thứ-tự-chỉ-tồn-tại-trong-một-partition)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Cùng key → cùng partition → đúng thứ tự; chọn key = thực thể cần thứ tự (account_id cho sự kiện tài khoản); tăng số partition làm hash(key) mod partitions đổi, sự kiện mới sang partition khác trong khi cũ còn ở partition cũ → thứ tự vỡ; chọn số partition đủ lớn từ đầu theo số consumer tối đa | Ep12 · 1:30 (Ordering) |

### 20 §8.2 · Độ bền ghi

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#82-độ-bền-ghi](../20-implement-gd2-du-lieu-phan-tan.md#82-độ-bền-ghi)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | acks=1 mất khi leader ghi xong rồi chết trước khi follower chép; acks=all + replication.factor=3 + min.insync.replicas=2 phải mất 2/3 broker cùng lúc — cấu hình chuẩn cho tiền; unclean.leader.election.enable=false (mặc định): thà ngừng ghi còn hơn mất | Ep12 · 2:10 (Durable writes) |
| ✅ | Idempotent producer bật mặc định từ Kafka 3.0: broker khử trùng khi producer retry; chỉ chống trùng do retry của producer, không chống trùng do relay outbox chết rồi chạy lại | Ep12 · 2:46 (Durable writes) |

### 20 · Bài tập tuần 8–9

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-89](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-89)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | BT 8.1: key = accountId vì thứ tự cần giữ trong một tài khoản (khoá phải đến trước giao dịch bị từ chối); eventId đều nhưng mất thứ tự; customerId giữ thứ tự rộng hơn cần và tạo partition nóng với khách doanh nghiệp nhiều tài khoản | Ep12 · 3:32 (Exercise 8.1) |
| ✅ | BT 8.2: topic replication.factor=3, min.insync.replicas=2; producer acks=all, idempotence bật, delivery.timeout.ms hữu hạn kèm xử lý lỗi; broker unclean.leader.election.enable=false; acks=all một mình là 'mọi replica trong ISR': ISR co còn leader thì bằng acks=1 | Ep12 · 4:12 (Exercise 8.2) |

### 20 §8.3 · Ba mức giao nhận

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#83-ba-mức-giao-nhận](../20-implement-gd2-du-lieu-phan-tan.md#83-ba-mức-giao-nhận)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | At-most-once: commit offset trước khi xử lý, crash giữa chừng → mất message; at-least-once: commit sau khi xử lý, crash → xử lý lại; 'exactly-once' có DB: at-least-once + consumer idempotent (dedup cùng transaction với tác dụng phụ) → đúng một lần về hiệu quả | Ep13 · 0:34 (Three delivery levels) |
| ✅ | Kafka transaction (isolation.level=read_committed) cho exactly-once trong Kafka (đọc topic A, ghi topic B, commit offset cùng transaction); có ghi Postgres thì vẫn cần idempotency ở DB | Ep13 · 1:09 (Three delivery levels) |

### 20 §8.4 · Consumer group, rebalance

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#84-consumer-group-rebalance-maxpollintervalms](../20-implement-gd2-du-lieu-phan-tan.md#84-consumer-group-rebalance-maxpollintervalms)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Consumer phải poll() lại trước max.poll.interval.ms (mặc định 300 s), nếu không broker coi là chết, chia partition cho consumer khác và commit của nó thất bại; lô max.poll.records (mặc định 500) xử lý càng lâu càng dễ vượt — chính là P20; Kafka 4.0 đưa KIP-848 lên GA (rebalance do broker điều phối, không dừng cả group) nhưng giới hạn giữa hai lần poll vẫn còn | Ep13 · 1:34 (Consumer groups) |

### 20 §8.5 · Retry, DLT, backpressure

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#85-retry-dlt-backpressure](../20-implement-gd2-du-lieu-phan-tan.md#85-retry-dlt-backpressure)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Poison message: retry vài lần có backoff rồi sang dead-letter topic kèm header lỗi để partition không kẹt; lỗi tạm thời (downstream chết): đừng đẩy vào DLT ngay, pause() partition, chờ, resume() hoặc retry topic có độ trễ; backpressure: giới hạn bằng max.poll.records và executor có giới hạn (P13), không phải BlockingQueue vô hạn | Ep13 · 2:25 (Retry, DLT, backpressure) |

### 20 · Lab 8: Spring Boot + Kafka + Postgres

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#lab-8--spring-boot--kafka--postgres-thứ-bảy-tuần-8-và-9](../20-implement-gd2-du-lieu-phan-tan.md#lab-8--spring-boot--kafka--postgres-thứ-bảy-tuần-8-và-9)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Project my-work/w8-9-kafka-lab: spring-boot-starter-web, spring-kafka, spring-boot-starter-jdbc, Postgres; bản lõi không Spring OutboxLab.java đã chạy với Postgres 16.4 và Kafka 4.1.2 thật, dùng để đối chiếu không để chép; test bằng Testcontainers nếu có Docker, không thì Kafka từ tarball như §0 | Ep14 · 0:52 (The project) |
| ✅ | Bước 1 schema 6 bảng: account (check balance >= 0), transfer, ledger_entry, outbox (event_id unique, topic, msg_key, payload, created_at, published_at null = chưa gửi), partial index outbox_pending where published_at is null, processed_event (event_id khoá chính), notification | Ep14 · 1:25 (Step 1 · Schema) |
| ✅ | Bước 2: TransferService.transfer() @Transactional: trừ có điều kiện (where id = ? and balance >= ?), 0 dòng thì từ chối; cộng bên nhận; insert transfer; hai ledger_entry; một dòng outbox key = acct-{from}; một commit cho tiền, sổ cái và sự kiện | Ep14 · 2:05 (Step 2 · One transaction) |
| ✅ | Bước 3 relay @Scheduled(fixedDelay = 200): trong một transaction ngắn select … where published_at is null order by id limit 100 for update skip locked (relay khác bỏ qua, không chờ); producer.send(record).get() với header event-id, chỉ đánh dấu published_at khi broker đã ack; relay crash sau khi gửi trước khi đánh dấu → gửi lại (at-least-once) | Ep14 · 2:46 (Step 3 · The relay) |
| ✅ | Relay giữ connection trong lúc gửi lô 100 message: P09 ở mức nhỏ, chấp nhận vì Kafka cùng AZ ack trong vài ms, relay 1–2 instance với pool riêng, delivery.timeout.ms chặn trên thời gian giữ; không chấp nhận được thì chuyển sang CDC | Ep14 · 3:52 (P09, on purpose) |
| ✅ | Bước 4 consumer idempotent: @KafkaListener, trong MỘT transaction insert into processed_event(event_id) on conflict do nothing; chèn được 1 dòng thì tạo notification, 0 dòng thì bỏ qua (trùng); offset commit sau khi DB commit (mặc định Spring Kafka commit sau khi listener trả về) | Ep15 · 0:25 (Step 4 · Consumer) |
| ✅ | Bước 5: DefaultErrorHandler + DeadLetterPublishingRecoverer + FixedBackOff(1000, 2): lỗi 3 lần sang <topic>.DLT; JSON hỏng đăng ký not-retryable → DLT ngay; phân biệt lỗi không bao giờ thành công với lỗi tạm thời | Ep15 · 1:05 (Step 5 · Retry and DLT) |
| ✅ | Bước 6, bốn test có số: relayCrashAfterSend_producesDuplicates (1.000 dòng → 1.100 message), idempotentConsumer_appliesEachEventOnce (áp dụng 1.000, bỏ 100), twoRelaysWithSkipLocked_doNotDoublePublish (800/800), ledgerInvariantsHold (sum(ledger) = 0, tổng 100.000.000) | Ep15 · 1:42 (Step 6 · Seven tests) |
| ✅ | Bước 6, ba test chỉ có trong lab Spring: poisonMessage_goesToDlt (payload hỏng vào DLT kèm header lỗi, message sau vẫn xử lý), consumerCrashBeforeOffsetCommit_noDoubleEffect (lỗi sau DB commit trước offset commit → message tới lại, bị bỏ qua), eventsForOneAccountArriveInOrder (một relay: cùng account_id đến đúng thứ tự) | Ep15 · 2:32 (Step 6 · Seven tests) |
| ✅ | Output thật của OutboxLab (07/10/2026, chạy 2 lần giống hệt): 1.000 giao dịch; relay A crash sau khi gửi lô 3; relay A đánh dấu 200, B+C 800; 1.100 message cho 1.000 dòng outbox; consumer applied=1000 skipped=100; sum(balance) không đổi, sum(ledger) = 0, outbox pending = 0, 1.000 notification; bản lõi chưa kiểm thứ tự và DLT | Ep15 · 3:01 (Step 6 · Seven tests) |
| ✅ | Bước 7: hai relay song song, chạy lại test thứ tự: relay B lấy 101–200, C lấy 201–300, C gửi xong trước → có thể đỏ; SKIP LOCKED chống gửi trùng, không giữ thứ tự | Ep15 · 3:38 (Step 7 · Break the order) |
| ✅ | Bước 7, ghi vào sổ lỗi ba cách giữ thứ tự: một relay duy nhất (khoá leader bằng advisory lock hoặc ShedLock); chia outbox theo hash(key) mod K, mỗi phần một relay; consumer chấp nhận đến lệch thứ tự bằng version theo từng tài khoản | Ep15 · 4:08 (Step 7 · Break the order) |

### 20 · Bài tập tuần 8–9 (8.3)

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-89](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-89)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | BT 8.3: tối đa 100 × 3 = 300 message trùng mỗi ngày (lab đo đúng 100 cho một lần crash); consumer không idempotent: 300 thông báo gửi hai lần, nếu consumer là ledger thì ghi sổ hai lần; giảm lô chỉ giảm số lượng, idempotency mới bỏ được hậu quả | Ep15 · 4:58 (Exercise 8.3) |

### 20 · Bài nói 2 phút tuần 8–9

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút-2](../20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút-2)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Bài nói 'How do you get exactly-once processing with Kafka and a database?': Kafka transaction chỉ bao phần trong Kafka; với DB at-least-once + dedup bằng event_id trong cùng transaction với tác dụng phụ, commit offset sau; outbox phía gửi; số của lab (1.100 message, 1.000 lần áp dụng); đánh đổi: thêm một lần ghi DB mỗi message, bảng dedup phải dọn theo thời gian | Ep15 · 5:25 (2-minute talk) |

### 01 · P20: Kafka consumer xử lý đồng bộ từng message

Nguồn: [01-java-code-cham-duoi-tai-cao.md#p20--kafka-consumer-xử-lý-đồng-bộ-từng-message-](../01-java-code-cham-duoi-tai-cao.md#p20--kafka-consumer-xử-lý-đồng-bộ-từng-message-)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Bản xấu: @KafkaListener gọi HTTP 200 ms cho mỗi message, poll 500 record = 100 s mới poll lại; sửa: batch listener + API gửi theo lô (cộng consumer idempotent như lab 8) | Ep16 · 0:32 (The bug) |
| ✅ | Đối tác chậm lên 1 s: 500 × 1 s = 500 s vượt max.poll.interval.ms 300 s → broker coi consumer chết → rebalance → partition sang consumer khác, xử lý lại từ offset chưa commit → cũng chậm → lại rebalance: rebalance storm, gửi trùng thông báo | Ep16 · 0:58 (The storm) |
| ✅ | Bản xấu không bao giờ xong vì mỗi lô bị đuổi khỏi group trước khi commit nên quay lại từ đầu; gửi trùng hơn 2.000 thông báo; sửa A (giảm max.poll.records) hết storm nhưng chậm, sửa B (gọi theo lô) nhanh gấp 18 lần | Ep16 · 2:25 (D20 numbers) |
| ✅ | Sửa theo thứ tự: gọi theo lô; giảm max.poll.records theo p99 của downstream chứ không theo trung bình; xử lý song song có giới hạn nhưng giữ thứ tự theo key; pause() partition khi downstream chậm; consumer idempotent vì trùng chắc chắn xảy ra | Ep16 · 2:47 (Fix it, in order) |

### 20 · Track P tuần 8–9: chạy D20, bài tập P20

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#track-p-tuần-89--p20-consumer-chậm-và-rebalance-storm](../20-implement-gd2-du-lieu-phan-tan.md#track-p-tuần-89--p20-consumer-chậm-và-rebalance-storm)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Số đo D20 (Kafka 4.1.2 thật, 2.000 message, 4 partition, 2 consumer, downstream 20 ms, max.poll.interval.ms hạ xuống 6 s, 2 lần chạy): PER_MESSAGE_500 chưa xong sau 45 s (1.267–1.619/2.000, 1.847–2.245 lần gửi trùng, 6 partition bị thu hồi, 6–7 commit lỗi); PER_MESSAGE_50 xong 24,6 s; BATCH_CALL_500 xong 1,3 s, 0 trùng | Ep16 · 1:35 (D20 numbers) |
| ✅ | Bài tập P20: (a) 300.000 ÷ 500 = 600 ms mỗi message; sự cố làm downstream chậm lên 700 ms là đủ storm; (b) theo p99 với hệ số an toàn 2: 300 s ÷ (1,2 s × 2) ≈ 125, chọn 100; tốt hơn gọi theo lô hoặc executor có giới hạn + pause(); (c) consumer lag tăng (records-lag-max), số lần rebalance / join group tăng, số commit thất bại | Ep16 · 3:40 (Exercise P20) |

### 01 §7 · Bắt P20 trên production

Nguồn: [01-java-code-cham-duoi-tai-cao.md#7-bắt-chúng-trên-production--metric-nào-công-cụ-nào](../01-java-code-cham-duoi-tai-cao.md#7-bắt-chúng-trên-production--metric-nào-công-cụ-nào)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | P20 trên production: metric kafka.consumer.fetch.manager.records.lag.max tăng, số lần rebalance; xác nhận bằng log 'Member ... has left the group' lặp lại; đo p99 không đo trung bình | Ep16 · 4:31 (In production) |

### 20 §8.6 · Stream processing

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#86-stream-processing--đủ-để-nói-chuyện](../20-implement-gd2-du-lieu-phan-tan.md#86-stream-processing--đủ-để-nói-chuyện)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Window tumbling (1 phút, không chồng), hopping (5 phút trượt mỗi 1 phút), session (đóng theo khoảng lặng) | Ep17 · 0:33 (Windows) |
| ✅ | Event time (lúc click xảy ra) khác processing time (lúc hệ thống thấy); gom theo event time, watermark quyết khi nào đóng window, có chính sách cho late event (cập nhật lại window đã đóng hoặc đưa vào bảng đối soát) | Ep17 · 1:01 (Event time) |

### 20 · Bài tập 8.4: Ad click aggregation

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-89](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-89)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | BT 8.4: 1 tỷ click/ngày ≈ 11.600/s trung bình, ~50.000/s đỉnh; truy vấn số click theo ad trong N phút gần nhất và top 100 ad | Ep17 · 2:09 (Exercise 8.4) |
| ✅ | BT 8.4 luồng: click → Kafka (key ad_id#0..7) → stream processor (tumbling 1 phút theo event time, watermark 15 s) → upsert (ad_id, minute) → OLAP; raw click vào object storage; batch hằng ngày tính lại và so với stream | Ep17 · 2:32 (Exercise 8.4) |
| ✅ | Quyết định: thu click → Kafka key ad_id, ad nóng → partition nóng nên tách key ad_id#0..7; gộp bằng stream processor tumbling 1 phút theo event time, watermark 15 s, click sau watermark xử lý riêng; ghi upsert (ad_id, minute) → sink idempotent, không cần exactly-once của framework | Ep17 · 3:01 (Exercise 8.4) |
| ✅ | Lưu raw click vào object storage để đối soát và tính lại khi có bug; batch hằng ngày tính lại từ raw so với kết quả stream (hai đường tính cùng một con số); phục vụ bằng OLAP (ClickHouse/Druid) hoặc Postgres partition theo ngày | Ep17 · 3:28 (Exercise 8.4) |
| ✅ | Cùng họ bài với 04 — đếm sự kiện 10k/phút: bucket theo thời gian, rollup, giữ raw để đối soát | Ep17 · 3:50 (Interview line) |

### 20 · Track S tuần 8–9: V7

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-89--v7-paymentwallet-có-kafka--saga-chủ-nhật-tuần-9-40-phút](../20-implement-gd2-du-lieu-phan-tan.md#track-s-tuần-89--v7-paymentwallet-có-kafka--saga-chủ-nhật-tuần-9-40-phút)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Sơ đồ V7: app → Transfer API (Idempotency-Key) → orchestrator (saga_instance + outbox) → lệnh hold/check/post qua Kafka (transfer.commands / transfer.events) → ledger append-only, fraud/limit, interbank gateway (idempotency key sang đối tác) trả lời qua Kafka; gateway → ngân hàng đối tác; transfer.completed → notification idempotent; job đối soát file đối tác với sổ cái | Ep18 · 0:29 (The drawing) |
| ✅ | 5 điểm phải nói: orchestrator lưu trạng thái + outbox cùng transaction; key topic = transferId cho lệnh, accountId cho sự kiện tài khoản; gateway là pivot, timeout trả unknown, không bù mù (7.3); mọi consumer idempotent vì outbox và Kafka at-least-once; job đối soát là bằng chứng cuối cùng tiền đúng | Ep18 · 1:17 (Five points) |

### 20 · Tuần 10: đầu ra

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần-1](../20-implement-gd2-du-lieu-phan-tan.md#đầu-ra-phải-có-cuối-tuần-1)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Cuối tuần 10: lab 10A fencing token trên Postgres thật có test client 'sống lại' sau pause; lab 10B job scheduler SKIP LOCKED + lease (tùy chọn); hai đề bấm giờ Hotel reservation, Distributed job scheduler; cập nhật V6 bằng số đo lab 8; qua mốc tuần 10; bài nói 2 phút về khoá phân tán và fencing token | Ep19 · 0:41 (Week 10) |

### 20 · Tuần 10: đọc

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#đọc-3](../20-implement-gd2-du-lieu-phan-tan.md#đọc-3)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Đọc: DDIA ch. 8 The Trouble with Distributed Systems, ch. 9 Consistency and Consensus; Kleppmann How to do distributed locking (2016); bài báo Raft + raft.github.io; Alex Xu Vol 2 ch. 7 Hotel reservation | Ep19 · 1:16 (Week 10) |

### 20 §10.1 · Đồng hồ nói dối

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#101-đồng-hồ-nói-dối](../20-implement-gd2-du-lieu-phan-tan.md#101-đồng-hồ-nói-dối)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | System.currentTimeMillis() là wall clock, NTP chỉnh được kể cả lùi; đo khoảng thời gian dùng System.nanoTime() (monotonic) | Ep19 · 1:50 (Clocks lie) |
| ✅ | Hai máy lệch vài ms tới vài chục ms là bình thường; LWW theo timestamp có thể để lệnh ghi cũ hơn thắng; thứ tự đáng tin: một log (một partition Kafka, một sequence DB) hoặc đồng hồ logic (Lamport, version vector) | Ep19 · 2:11 (Clocks lie) |

### 20 §10.2 · Raft trong năm dòng

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#102-raft-trong-năm-dòng](../20-implement-gd2-du-lieu-phan-tan.md#102-raft-trong-năm-dòng)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Node là follower/candidate/leader; thời gian chia term; hết hạn không nghe leader → candidate xin phiếu; đa số → leader term mới; leader nhận ghi, chép log, commit khi đa số đã chép; chỉ bầu cho candidate có log ít nhất mới bằng mình nên leader mới có mọi entry đã commit | Ep19 · 3:35 (Raft) |
| ✅ | 5 node chịu được 2 node chết; mạng chia 2/3 thì phía 3 node vẫn chạy, phía 2 node không ghi được; dùng ở etcd, Consul, Kafka KRaft (từ Kafka 4.0 không còn ZooKeeper) | Ep19 · 4:11 (Raft) |

### 20 · Bài tập tuần 10 (10.1, 10.2)

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-10](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-10)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | BT 10.1: X ghi timestamp 10:00:00.020, Y ghi 10:00:00.010; LWW chọn lệnh 1 (5tr) dù lệnh 2 xảy ra sau: sai và không có lỗi nào được báo; với hạn mức tiền dùng một nguồn thứ tự (một leader, một sequence) hoặc update có điều kiện theo version | Ep19 · 3:04 (Exercise 10.1) |
| ✅ | BT 10.2: (a) 2; (b) leader cũ không chép tới đa số nên không commit, client timeout; phía 3 node bầu leader term lớn hơn; nối lại thì leader cũ xuống follower, bỏ entry chưa commit; (c) 4 node chịu 1 như 3 node nhưng tốn thêm máy, 6 chịu 2 như 5 | Ep19 · 4:59 (Exercise 10.2) |

### 20 §10.3 · Khoá phân tán

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#103-khoá-phân-tán-cho-hiệu-năng-hay-cho-đúng-đắn](../20-implement-gd2-du-lieu-phan-tan.md#103-khoá-phân-tán-cho-hiệu-năng-hay-cho-đúng-đắn)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Khoá cho hiệu năng (tránh hai worker làm trùng việc tốn kém): hỏng thì thỉnh thoảng làm trùng, tốn tiền không sai, Redis SET key value NX PX 30000 đủ; khoá cho đúng đắn (hai bên cùng ghi là sai dữ liệu): khoá + fencing token do chính nơi lưu trữ kiểm | Ep20 · 0:44 (Two kinds of lock) |
| ✅ | Khoá có TTL luôn có kẽ hở: client giữ khoá bị GC pause hoặc mạng treo quá TTL, khoá hết hạn, client khác lấy khoá, client cũ tỉnh dậy vẫn tin mình giữ khoá và ghi; client không tự biết mình bị pause, chỉ nơi lưu trữ chặn được bằng cách từ chối token cũ; tài nguyên chính là Postgres thì dùng khoá dòng hoặc update có điều kiện | Ep20 · 1:13 (The gap) |

### 20 · Lab 10A: fencing token

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#lab-10a--fencing-token-thứ-bảy-15-giờ](../20-implement-gd2-du-lieu-phan-tan.md#lab-10a--fencing-token-thứ-bảy-15-giờ)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Bản mô phỏng Fencing.java đã chạy: không fencing thì A (token 1) và B (token 2) đều ghi, giá trị cuối là giá trị cũ của A; có fencing thì A bị từ chối (ghi=false), giá trị cuối là của B | Ep20 · 1:53 (Lab 10A) |
| ✅ | Bản thật trên Postgres: khoá = một dòng lock(name, owner, token, expires_at), cấp bằng update có điều kiện where expires_at < now() và token = token + 1 returning token; config(name, value, last_token), ghi where last_token <= token (0 dòng = bị từ chối); test A lấy khoá, pause quá TTL, B lấy khoá và ghi, A ghi → 0 dòng, giá trị cuối của B | Ep20 · 2:16 (Lab 10A) |

### 20 · Lab 10B: job scheduler (tùy chọn)

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#lab-10b--job-scheduler-phân-tán-tùy-chọn-thứ-bảy-2-giờ](../20-implement-gd2-du-lieu-phan-tan.md#lab-10b--job-scheduler-phân-tán-tùy-chọn-thứ-bảy-2-giờ)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Lab 10B: worker claim job READY bằng update … for update skip locked, lease 30 s, lease_version + 1; gia hạn mỗi 10 s; reaper trả lease_until < now() về READY; ghi kết quả kèm where lease_version = :v: worker bị coi là chết mà vẫn chạy ghi 0 dòng; lease_version chính là fencing token | Ep20 · 2:53 (Lab 10B) |
| ✅ | Test lab 10B: 4 worker, 1.000 job, kill một worker giữa chừng: mọi job chạy ít nhất một lần, kết quả ghi đúng một lần | Ep20 · 3:25 (Lab 10B) |

### 20 · Bài tập 10.3

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-10](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-10)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | BT 10.3: (a) khoá cho hiệu năng (Redis hoặc ShedLock) + email idempotent theo (khách, ngày); (b) không cần khoá phân tán, tài nguyên là Postgres: update có điều kiện hoặc khoá dòng (giai đoạn 1 lab 3A); (c) gọi trùng chỉ tốn tiền: Redis lock đủ + idempotency key gửi nhà cung cấp nếu họ hỗ trợ | Ep20 · 4:08 (Exercise 10.3) |

### 20 · Bài nói 2 phút tuần 10

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút-3](../20-implement-gd2-du-lieu-phan-tan.md#bài-nói-2-phút-3)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Bài nói 'How can a distributed lock fail, and what is a fencing token?': TTL + process pause là kẽ hở không đóng được từ phía client; token tăng dần cấp cùng khoá; nơi lưu trữ từ chối token cũ; số của lab (A ghi bị từ chối); khi nào không cần khoá phân tán (tài nguyên chính là DB) | Ep20 · 4:41 (2-minute talk) |

### 20 · Bài tập 10.4, 10.5

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-10](../20-implement-gd2-du-lieu-phan-tan.md#bài-tập-tuần-10)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | BT 10.4 Hotel reservation: room_type_inventory một dòng mỗi (khách sạn, loại phòng, ngày) với total, reserved; đặt nhiều đêm là MỘT transaction update có điều kiện date between :in and :out - 1 and reserved + :rooms <= total * 1.1; số dòng < số đêm → rollback | Ep21 · 0:49 (10.4 Hotel reservation) |
| ✅ | BT 10.4 quyết định: chống đặt trùng bằng reservation_id client sinh (idempotency key, unique); chống overbooking bằng update có điều kiện (ngày cao điểm tranh một dòng, vẫn ổn); giữ phòng PENDING 15 phút → saga thanh toán → xác nhận hoặc nhả; tìm phòng đọc cache/replica, kiểm lại lúc đặt | Ep21 · 1:18 (10.4 Hotel reservation) |
| ✅ | Hotel reservation thuộc họ G (đọc → tính → ghi trên trạng thái chia sẻ), như bài 06 | Ep21 · 2:04 (10.4 Hotel reservation) |
| ✅ | BT 10.5 Distributed job scheduler: lõi lab 10B; lịch cron chỉ một nơi sinh lần chạy, unique (schedule_id, fire_time) để sinh trùng vô hại; ưu tiên và công bằng giữa tenant (họ B, bài 02); job treo = lease hết hạn, reaper trả về READY; kết quả ghi kèm lease_version | Ep21 · 2:51 (10.5 Job scheduler) |
| ✅ | 'Exactly-once?' của scheduler: không; at-least-once + job idempotent + fencing khi ghi kết quả | Ep21 · 3:32 (10.5 Job scheduler) |

### 00 · Capstone A hoặc B

Nguồn: [00-lo-trinh-6-thang.md#dự-án-thực-hành-capstone](../00-lo-trinh-6-thang.md#dự-án-thực-hành-capstone)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Chọn capstone theo công ty: A Mini Core Transfer (ngân hàng, ví) cần sổ cái kép, idempotency key, saga + outbox, chống chi trùng, đối soát cuối ngày, audit log, chứng minh bằng gửi lại 100 lần ra 1 giao dịch, kill consumer không mất/trùng tiền, load test 500 TPS; B Distributed Test Runner (SaaS) cần queue + worker autoscale, chia công bằng giữa tenant, retry job treo, log realtime, webhook retry, chứng minh 10.000 job một tenant không nghẽn tenant khác, kill worker job chạy lại đúng | Ep22 · 0:55 (The capstone) |

### 20 · Capstone khởi động

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#capstone-khởi-động--tuần-910](../20-implement-gd2-du-lieu-phan-tan.md#capstone-khởi-động--tuần-910)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Capstone chạy tuần 9–16; giai đoạn 2 chỉ làm design doc v1 (tuần 9) và khung MVP chạy được (tuần 10) | Ep22 · 0:00 (Intro) |

### 20 · Mẫu design doc

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#mẫu-design-doc-my-workcapstonedesign-docmd](../20-implement-gd2-du-lieu-phan-tan.md#mẫu-design-doc-my-workcapstonedesign-docmd)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Design doc 11 mục: bối cảnh (5 dòng), mục tiêu/không phải mục tiêu, yêu cầu phi chức năng có số (SLO p99, availability, RPO/RTO, nhất quán theo luồng), tải ở ba bậc 100k/1M/10M, kiến trúc C4, dữ liệu và API, luồng chính + 3 luồng lỗi, phương án đã cân nhắc, vận hành, rủi ro, kế hoạch | Ep22 · 1:46 (Design doc) |
| ✅ | Mỗi quyết định lớn ở mục 8 thành một ADR một trang (adr/0001-outbox-polling-thay-cdc.md): Bối cảnh · Quyết định · Hệ quả · Phương án đã loại | Ep22 · 2:44 (Design doc) |

### 20 · Phạm vi MVP

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#phạm-vi-mvp-gợi-ý](../20-implement-gd2-du-lieu-phan-tan.md#phạm-vi-mvp-gợi-ý)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Tuần 10 khung: A API mở tài khoản + chuyển tiền nội bộ có idempotency key, sổ cái kép, outbox → Kafka, consumer thông báo idempotent (lab 8); B API nhận job + bảng job, worker SKIP LOCKED + lease (lab 10B); tuần 11–14 saga liên ngân hàng, đối soát, Keycloak / hàng đợi theo tenant, log, MinIO; tuần 15–16 load test 500 TPS, thử phá / 10.000 job một tenant, kill worker, autoscale | Ep22 · 3:14 (MVP scope) |

### 20 · Mốc tuần 10

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#mốc-tuần-10--tiêu-chí-qua-giai-đoạn](../20-implement-gd2-du-lieu-phan-tan.md#mốc-tuần-10--tiêu-chí-qua-giai-đoạn)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Mốc tuần 10 tiêu chí 1–5: trả lời 6 chủ đề không tài liệu ≤ 2 phút; lab 8 bảy test xanh có test trùng do relay crash; lab 7 recovery + pivot timeout; lab 5A, 10A bắt buộc; lab 5B, 10B tùy chọn | Ep22 · 4:00 (The milestone) |
| ✅ | Tiêu chí 6–10: D20 sửa một consumer có số trước/sau; V6 (× 10, số thật lab 8) và V7; 6 đề bấm giờ tự chấm ≥ 7/10; design doc v1 đủ 11 mục + khung MVP chạy được; 4 bài nói 2 phút đã ghi âm | Ep22 · 4:33 (The milestone) |
| ✅ | Trễ quá 2 tuần: bỏ lab tùy chọn và bớt một đề bấm giờ, không bỏ lab 8 và design doc vì capstone phụ thuộc cả hai | Ep22 · 5:00 (The milestone) |

### 20 · Ranh giới trung thực

Nguồn: [20-implement-gd2-du-lieu-phan-tan.md#ranh-giới-trung-thực](../20-implement-gd2-du-lieu-phan-tan.md#ranh-giới-trung-thực)

| | Ý chính | Video · thời điểm |
|:---:|---|---|
| ✅ | Đã chạy: ConsistentHash, Fencing, Saga (JDK 21); OutboxLab 2 lần với PostgreSQL 16.4 và Kafka 4.1.2 thật (KRaft), kết quả giống hệt; D20 2 lần, max.poll.interval.ms hạ còn 6 s. Chưa làm: lab Spring 7, 8, 5B, 10A bản Postgres, 10B; bước 5 (DLT) và 7 (thứ tự) của lab 8 là suy luận; đáp án đề bấm giờ là khung hợp lý, số liệu là giả định | Ep22 · 5:11 (Honest boundaries) |

