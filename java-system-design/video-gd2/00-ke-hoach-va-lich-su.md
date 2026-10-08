# Video giai đoạn 2 — Dữ liệu và hệ phân tán (tuần 5–10): kế hoạch và nhật ký

> Bộ video tiếng Anh cho [giai đoạn 2](../20-implement-gd2-du-lieu-phan-tan.md) của
> [lộ trình 6 tháng](../00-lo-trinh-6-thang.md). Giọng đọc là **Kokoro, giọng nữ Emma (`af_heart`)**,
> cùng công cụ [`tools/lesson_video/`](../../tools/lesson_video/README.md) và cùng quy ước với
> [bộ video giai đoạn 1](../video-gd1/00-ke-hoach-va-lich-su.md) (giọng Tom).
>
> File này vừa là kế hoạch, vừa là chỗ theo dõi việc đã làm. Mỗi đợt làm xong phải cập nhật
> [bảng trạng thái (mục 8)](#8-bảng-trạng-thái) và [nhật ký (mục 9)](#9-nhật-ký--lịch-sử-đã-làm).
> Chữ viết tắt: [01 — Bảng chữ viết tắt giai đoạn 2](01-bang-chu-viet-tat.md) (chỉ chữ mới) cộng
> [bảng của giai đoạn 1](../video-gd1/01-bang-chu-viet-tat.md). Độ phủ: [02 — Độ phủ nội dung](02-do-phu.md).

---

## 0. Trả lời ngắn: bao nhiêu video

**23 video (Ep00–Ep22), dự kiến khoảng 3 giờ 40 phút**, mỗi video 8–11 phút như tuần 1 của giai đoạn 1:

| Nhóm | Số video | Mã | Dài dự kiến |
|---|---:|---|---:|
| Định hướng giai đoạn 2 | 1 | Ep00 | 8 phút |
| Tuần 5–6 — replication, quorum, partitioning, consistent hashing, V6 | 6 | Ep01–Ep06 | 58 phút |
| Tuần 7 — 2PC, saga, outbox, idempotency, sổ cái kép | 5 | Ep07–Ep11 | 50 phút |
| Tuần 8–9 — Kafka sâu, lab 8, P20, stream processing, V7 | 7 | Ep12–Ep18 | 68 phút |
| Tuần 10 — đồng hồ, Raft, khoá phân tán, capstone, mốc tuần 10 | 4 | Ep19–Ep22 | 40 phút |
| **Tổng** | **23** | | **≈ 224 phút** |

**Thực tế (đợt 1, 08/10/2026): 23 video, tổng 2:03:02 (≈ 123 phút), mỗi video dài từ 3:02 (Ep18) đến 7:11 (Ep01),
106 MB.** Ngắn hơn dự kiến khoảng 45%: Emma đọc nhanh hơn Tom chừng 10% ở cùng tốc độ 0,9, và
kịch bản gọn hơn mức 8–11 phút đã ước tính. Số phút ở mục 3 là dự kiến ban đầu; số thật ở [mục 8](#8-bảng-trạng-thái).

Vì sao là con số này:

1. **Nguồn dài ngang giai đoạn 1.** File 20 có 1.179 dòng (file 10 có 1.211), cộng P20 của file 01,
   §3.3 và §5 của file 02, phần giai đoạn 2 và capstone của file 00.
2. **Tuần 1 đã cho số thật:** 7 video, 69 phút cho khoảng 300 dòng nguồn và 91 ý chính. Giai đoạn 2
   gấp khoảng 3 lần lượng đó, nên khoảng 20–24 video là vừa.
3. **Mỗi video một ý lớn kèm bài tập của chính ý đó**, như giai đoạn 1. Lab dài (lab 8) tách làm hai
   video; mỗi khối đề bấm giờ (KV store + cache, payment + wallet, hotel + scheduler) là một video.
4. **Không làm video ôn chữ viết tắt riêng.** Mỗi video tự giải thích chữ viết tắt nó dùng (thẻ
   *Acronyms in this video*), giống quy tắc ở mục 2.

---

## 1. Phạm vi nguồn

| File | Phần dùng cho video | Ghi chú |
|---|---|---|
| [20 — Implement giai đoạn 2](../20-implement-gd2-du-lieu-phan-tan.md) | **Toàn bộ** | Nguồn chính: khái niệm, lab, bài tập, đề bấm giờ, capstone, mốc tuần 10, ranh giới trung thực |
| [00 — Lộ trình 6 tháng](../00-lo-trinh-6-thang.md) | Giai đoạn 2, capstone | Bảng tuần 5–10 và hai đề capstone A/B |
| [01 — Track P](../01-java-code-cham-duoi-tai-cao.md) | P20, §7 (metric của P20) | Các P khác thuộc giai đoạn 1 và 3 |
| [02 — Track S](../02-ve-he-thong-100k-1m-10m.md) | §3.3 (L2), §5 (câu hỏi × 10) | Dùng cho V6 |
| [dist-lab](../dist-lab/README.md), [perf-lab results](../perf-lab/results/) | Số đo đã chạy | ConsistentHash, Saga, OutboxLab (Postgres 16.4 + Kafka 4.1.2 thật), Fencing, D20 |

Số liệu trong video **chỉ lấy từ các file trên**. Số đo thật nói rõ là "measured" (kèm môi trường đo);
giả định của đề bấm giờ nói rõ là "assumption".

---

## 2. Quy ước chung

Giữ nguyên [quy ước của giai đoạn 1](../video-gd1/00-ke-hoach-va-lich-su.md#2-quy-ước-chung-cho-mọi-video),
chỉ khác:

| Hạng mục | Giai đoạn 2 |
|---|---|
| Giọng | Kokoro-82M q8, giọng **Emma `af_heart`** (nữ, Mỹ), **một người đọc** cho cả bộ, tốc độ **0,9** |
| Màn hình | Cùng bố cục slide; góc phải ghi *Java System Design · Phase 2*; tag `EPxx · WEEKS 5–6` cho khối hai tuần |
| Cấu hình chung | [`lessons/series.yaml`](lessons/series.yaml): tên bộ, người đọc, màu của Emma. Kịch bản không phải lặp lại |
| Chữ viết tắt | Kiểm với **hai** bảng: bảng giai đoạn 1 và [bảng chữ mới của giai đoạn 2](01-bang-chu-viet-tat.md) |
| Ý chính | [`lessons/points.yaml`](lessons/points.yaml): mỗi ý có chữ bắt buộc (`expect`), phạm vi theo khối tuần (0, 5, 7, 8, 10) |

Khung một video giữ như cũ: title → thẻ *Acronyms in this video* → nội dung → bài tập có đếm ngược
"Pause the video and try it yourself" → interview line hoặc bài nói mẫu 2 phút → recap.

---

## 3. Danh sách 23 video

Cột **Nguồn**: `20 §5.1` là mục 5.1 của file 20; `BT 5.4` là bài tập 5.4; `01 P20` là P20 của file 01.

### Định hướng

| Mã | Tên (tiếng Anh) | Nội dung chính | Nguồn | Phút |
|---|---|---|---|---:|
| **Ep00** | Phase 2 orientation | Vì sao giai đoạn này phân biệt senior; điều kiện vào (mốc tuần 4, lab 3A, lab 2); nhịp tuần và hai khối hai tuần; bản đồ 6 tuần; thư mục `my-work/`; chạy Kafka 4.x không Docker (KRaft) và Postgres nhúng; capstone tuần 9–16; mốc tuần 10 | 20 §0; 00 giai đoạn 2 | 8 |

### Tuần 5–6 — Replication, partitioning, consistent hashing

| Mã | Tên (tiếng Anh) | Nội dung chính | Nguồn | Phút |
|---|---|---|---|---:|
| **Ep01** | Replication: leaders, followers, and lag | Đầu ra, tài liệu đọc; ba mô hình replication; đồng bộ hay bất đồng bộ (+1–2 ms, ~90 ms); ba dị thường do lag; BT 5.1 (RPO), BT 5.3 (failover cấp lại ID) | 20 tuần 5–6 (đầu ra, đọc), §5.1; BT 5.1, 5.3 | 10 |
| **Ep02** | Quorums: N, W, R and what they don't promise | `R + W > N`; quorum không cho linearizability; LWW làm mất ghi; sloppy quorum + hinted handoff; lab 5B; BT 5.2 | 20 §5.2, lab 5B; BT 5.2 | 9 |
| **Ep03** | Partitioning: ranges, hashes, hot spots, secondary indexes | Bốn cách chia; index local (scatter-gather) và global (cập nhật bất đồng bộ); BT 5.4 tài khoản nóng (sổ cái append-only, salting ≥ 16 lần, gom lô); BT 5.5 tìm theo mã tham chiếu | 20 §5.3; BT 5.4, 5.5 | 10 |
| **Ep04** | Consistent hashing: lab 5A and the 2-minute talk | Vòng băm, `TreeMap` + `ceilingEntry`; bảng đo 1/10/100/200 vnode (80% so với 18,9%); hệ số nhân bản 3; BT 5.6 (256 partition cố định); bài nói 2 phút | 20 lab 5A, §5.3; BT 5.6; bài nói tuần 5–6 | 10 |
| **Ep05** | Timed designs: a key-value store and a distributed cache | Khung lời giải 8 dòng của KV store (vnode, N=3, quorum chỉnh được, vector clock, hinted handoff, Merkle tree, gossip, LSM); Redis Cluster 16.384 slot, `MOVED`, hot key, stampede, failover mất ghi, hash tag | 20 BT 5.7, 5.8 | 10 |
| **Ep06** | V6: Mini Core Transfer at 1M users | Track S: V2 vẽ lại cho 1M (~12.000 CCU, ~2.000 RPS, ~150 ghi/s); replica + read-your-writes, cache-aside, outbox → broker, tách Notification; đoạn "× 10" 5 câu (800 connection, PgBouncer, CQRS qua CDC, partition theo tháng, chưa shard); Track P tuần 5–6 (rà checklist 01 §6); tuần 10 cập nhật V6 bằng số đo | 20 Track S, Track P tuần 5–6, Track S tuần 10; 02 §3.3, §5 | 9 |

### Tuần 7 — Transaction phân tán

| Mã | Tên (tiếng Anh) | Nội dung chính | Nguồn | Phút |
|---|---|---|---|---:|
| **Ep07** | Why not two-phase commit: sagas instead | Đầu ra, tài liệu đọc; vì sao không 2PC (khoá qua mạng, coordinator chết, 5 × 99,9% → 99,5%, Kafka/REST không tham gia được); orchestration hay choreography; ba loại bước và thứ tự; biện pháp bù thiếu isolation; bài nói 2 phút | 20 tuần 7 (đầu ra, đọc), §7.1–7.3; bài nói tuần 7 | 11 |
| **Ep08** | Outbox, idempotency at three layers, and the double-entry ledger | Dual write; outbox polling hay CDC (Debezium đọc WAL); at-least-once nên consumer phải idempotent; idempotency ở API, consumer, gọi ra ngoài; sổ cái kép; BT 7.6 (hai query bất biến) | 20 §7.4–7.6; BT 7.6 | 10 |
| **Ep09** | Lab 7: an interbank transfer saga that survives crashes | 5 bước (compensatable, pivot, retriable); `saga_instance`; recovery job 10 s / 30 s; pivot timeout không bù mù; 6 test; khung `Saga.java` và output đã chạy; Track P tuần 7 (P09 qua ranh giới saga); Track S tuần 7 (phác V7) | 20 lab 7, Track P/S tuần 7 | 10 |
| **Ep10** | Saga drills: step order, isolation, and the pivot timeout | BT 7.1 (nạp ví: thứ tự và pivot), 7.2 (số dư sổ cái và số dư khả dụng, semantic lock), 7.3 (`UNKNOWN`, hỏi lại theo idempotency key, đối soát), 7.4 (chọn orchestration hay choreography) | 20 BT 7.1–7.4 | 10 |
| **Ep11** | Timed designs: a payment system and a digital wallet | Payment system: luồng, PSP, exactly-once = at-least-once + idempotency hai đầu, kết quả không rõ, đối soát, PCI DSS; digital wallet: khi nào cần event sourcing + Raft (1 triệu TPS so với 25–250 TPS) | 20 BT 7.5, 7.7 | 9 |

### Tuần 8–9 — Kafka sâu

| Mã | Tên (tiếng Anh) | Nội dung chính | Nguồn | Phút |
|---|---|---|---|---:|
| **Ep12** | Kafka: ordering, keys, and durable writes | Đầu ra, tài liệu đọc; thứ tự chỉ trong một partition; chọn key; thêm partition làm vỡ thứ tự; `acks`, ISR, `min.insync.replicas`, unclean leader election; idempotent producer; BT 8.1, 8.2 | 20 tuần 8–9 (đầu ra, đọc), §8.1–8.2; BT 8.1, 8.2 | 10 |
| **Ep13** | Kafka: delivery guarantees, consumer groups, and dead letters | Ba mức giao nhận; Kafka transaction chỉ bao phần trong Kafka; consumer group, rebalance, `max.poll.interval.ms`, KIP-848; poison message → DLT; lỗi tạm thời → `pause()`; backpressure | 20 §8.3–8.5 | 10 |
| **Ep14** | Lab 8, part 1: the outbox and its relay on real Kafka | Schema 6 bảng; chuyển tiền + outbox trong một transaction; relay `FOR UPDATE SKIP LOCKED`, gửi rồi mới đánh dấu; vì sao relay giữ connection vẫn chấp nhận được | 20 lab 8 bước 1–3 | 9 |
| **Ep15** | Lab 8, part 2: the idempotent consumer, and seven tests that prove it | Consumer với `processed_event`; retry + DLT; 7 test và số đã chạy (1.000 → 1.100 message, áp dụng 1.000, bỏ 100, 800/800, sổ cái = 0); bước 7 phá thứ tự và ba cách giữ; BT 8.3; bài nói 2 phút | 20 lab 8 bước 4–7; BT 8.3; bài nói tuần 8–9 | 11 |
| **Ep16** | Track P: P20, the slow consumer and the rebalance storm | Code xấu và sửa; cơ chế storm; số đo D20 (chưa xong sau 45 s, 1.847–2.245 lần trùng; 24,6 s; 1,3 s); thứ tự sửa; bài tập P20 (600 ms, chọn 100, ba metric) | 01 P20, §7; 20 Track P tuần 8–9 | 9 |
| **Ep17** | Stream processing, and the ad click aggregation design | Window tumbling, hopping, session; event time và processing time; watermark; late event; đề Ad click aggregation (11.600/s, key `ad_id#0..7`, sink idempotent, raw để đối soát) | 20 §8.6; BT 8.4 | 9 |
| **Ep18** | V7: an interbank transfer with Kafka and a saga | Sơ đồ V7 (orchestrator, ledger, fraud, gateway, notification, đối soát); 5 điểm phải nói; key của topic | 20 Track S tuần 8–9 | 9 |

### Tuần 10 — Đồng thuận, khoá phân tán, đồng hồ; capstone; mốc

| Mã | Tên (tiếng Anh) | Nội dung chính | Nguồn | Phút |
|---|---|---|---|---:|
| **Ep19** | Clocks lie, and Raft in five lines | Đầu ra, tài liệu đọc; wall clock và monotonic; LWW để lệnh cũ thắng; nguồn thứ tự đáng tin; Raft: term, bầu đa số, chép log, commit; BT 10.1, 10.2 | 20 tuần 10 (đầu ra, đọc), §10.1–10.2; BT 10.1, 10.2 | 10 |
| **Ep20** | Distributed locks and fencing tokens | Khoá cho hiệu năng hay cho đúng đắn; kẽ hở TTL + pause; fencing token do nơi lưu trữ kiểm; lab 10A (output mô phỏng, bản Postgres thật); lab 10B (lease, `lease_version`); BT 10.3; bài nói 2 phút | 20 §10.3, lab 10A, 10B; BT 10.3; bài nói tuần 10 | 11 |
| **Ep21** | Timed designs: hotel reservation and a distributed job scheduler | Bảng tồn kho theo ngày, update có điều kiện nhiều đêm, overbooking 10%, giữ `PENDING` 15 phút; scheduler: cron sinh một nơi, công bằng giữa tenant, lease, "exactly-once?" | 20 BT 10.4, 10.5 | 9 |
| **Ep22** | Capstone kickoff and the week-10 milestone | Capstone A/B, design doc 11 mục, ADR; phạm vi MVP; 10 tiêu chí mốc tuần 10; trễ thì cắt gì; ranh giới trung thực | 20 capstone, mốc tuần 10, ranh giới; 00 capstone | 10 |

---

## 4. Bảng phủ nội dung

Mỗi mục của nguồn phải có ít nhất một video. Bảng chi tiết (ý chính nào dạy ở phút nào) do lệnh
`coverage` sinh ra ở [02 — Độ phủ nội dung](02-do-phu.md); bảng dưới chỉ là phân công.

| Mục của file 20 | Video |
|---|---|
| §0 Trước khi bắt đầu | Ep00 |
| Tuần 5–6: đầu ra, đọc, §5.1 | Ep01 |
| §5.2, lab 5B | Ep02 |
| §5.3 (bảng chia, index phụ) | Ep03 (đầy đủ), Ep04 (phần vòng băm) |
| Lab 5A, bài nói tuần 5–6 | Ep04 |
| Track P tuần 5–6, Track S tuần 5–6 (V6) | Ep06 |
| BT 5.1 · 5.2 · 5.3 · 5.4 · 5.5 · 5.6 · 5.7 · 5.8 | Ep01 · Ep02 · Ep01 · Ep03 · Ep03 · Ep04 · Ep05 · Ep05 |
| Tuần 7: đầu ra, đọc, §7.1–7.3, bài nói | Ep07 |
| §7.4–7.6, BT 7.6 | Ep08 |
| Lab 7, Track P tuần 7, Track S tuần 7 | Ep09 |
| BT 7.1–7.4 | Ep10 |
| BT 7.5, 7.7 | Ep11 |
| Tuần 8–9: đầu ra, đọc, §8.1–8.2, BT 8.1–8.2 | Ep12 |
| §8.3–8.5 | Ep13 |
| Lab 8 bước 1–3 | Ep14 |
| Lab 8 bước 4–7, BT 8.3, bài nói | Ep15 |
| Track P tuần 8–9 (P20) | Ep16 |
| §8.6, BT 8.4 | Ep17 |
| Track S tuần 8–9 (V7) | Ep18 |
| Tuần 10: đầu ra, đọc, §10.1–10.2, BT 10.1–10.2 | Ep19 |
| §10.3, lab 10A, 10B, BT 10.3, bài nói | Ep20 |
| BT 10.4, 10.5 | Ep21 |
| Track S tuần 10 | Ep06 |
| Capstone khởi động, mốc tuần 10, ranh giới trung thực | Ep22 |
| File 00: giai đoạn 2 · capstone | Ep00 · Ep22 |
| File 01: P20, §7 | Ep16 |
| File 02: §3.3, §5 | Ep06 |

---

## 5. Phát âm

Quy tắc và các chỗ đã kiểm ở [giai đoạn 1, mục 5](../video-gd1/00-ke-hoach-va-lich-su.md#5-phát-âm--các-chỗ-phải-viết-say)
dùng lại nguyên vẹn (Emma và Tom dùng cùng bộ tách âm `en-us`). Kiểm thêm ngày 08/10/2026 bằng
`kokoro_onnx.tokenizer.Tokenizer().phonemize` cho khoảng 90 thuật ngữ của giai đoạn 2:

| Trên màn hình | Kokoro đọc nếu để nguyên | Cách xử lý |
|---|---|---|
| ISR | *isser* | `speak.py` đổi thành `I S R` |
| eKYC | *ee-kick* | `speak.py`: `e K Y C` |
| etcd | *etkd* | `speak.py`: `et-see-dee` |
| retriable | *re-tri-a-BLE* | `speak.py`: `retry able` |
| Tết | đánh vần từng ký tự | `speak.py`: `Tet` |
| `R + W > N` | bỏ mất dấu `>` | `speak.py`: `greater than`, `less than`, `not equal to` |
| `max.poll.records`, `min.insync.replicas` | ngắt câu ở dấu chấm | `speak.py` đổi dấu chấm trong tên cấu hình thành dấu cách |
| GA (general availability) | *gah* | Lời đọc nói `generally available`, không dùng chữ tắt |
| `1/(N+1)` | *one slash N plus one* | `say`: `one over N plus one` |
| 5tr, 10:00:00.010 (kiểu Việt, giờ có mili giây) | sai | Viết kiểu Anh trên slide: `5 million`; giờ dùng `say` |
| DDIA | *dee dee eye eye* (cách viết cũ `dee dee eye ay` của giai đoạn 1 bị đọc sai) | `speak.py`: `D D I eigh` — tìm ra khi dựng, dựng lại Ep00, Ep01, Ep03 |
| Chữ A đứng riêng giữa câu: relay A, topic A, fix A, client A, JPA, SHA-512 | mạo từ *a* | `speak.py`: chữ A viết hoa giữa câu thành `eigh` (đầu câu giữ nguyên vì đó là mạo từ); dựng lại Ep13 và Ep04 của giai đoạn 1 |
| SLO | *slow* | `speak.py`: `S L O` |
| raft.github.io, draw.io | *… ee-oh* | `say`: `raft dot github dot I O`; `speak.py`: `draw dot I O` (dựng lại Ep06 của giai đoạn 1) |
| retryable | *re-tri-AY-ble* | `speak.py`: `retry able` như `retriable`; dựng lại Ep15 |
| Redis | *rih-deez* (`ɹᵻdiz`): âm đầu không nhấn | **Chưa sửa.** Viết `Reddis` thì gần đúng (`ɹˈɛdiz`), nhưng phải dựng lại 10 video của cả hai giai đoạn; để lúc duyệt quyết |

Đọc đúng sẵn: 2PC (*two P C*), LWW, CRDT, DLT, PSP, PCI DSS, OLAP, WAL, KRaft (*K raft*), KIP-848,
CDC, CQRS, Debezium, ZooKeeper, DynamoDB, Cassandra, ClickHouse, Lamport, Raft, ShedLock, MinIO,
PgBouncer, `acks=all` (*acks equals all*), `read_committed`, `SKIP LOCKED`, `SET NX PX`, vnode,
Merkle tree, vector clock, choreography, orchestration, compensatable, Sydney, Melbourne, Singapore.

---

## 6. Công cụ và quy trình

Dùng [`tools/lesson_video/`](../../tools/lesson_video/README.md) như giai đoạn 1
([quy trình 9 bước](../video-gd1/00-ke-hoach-va-lich-su.md#62-quy-trình-một-video)). Thay đổi cho
giai đoạn 2:

| Thay đổi | Lý do |
|---|---|
| `series.yaml` cạnh kịch bản: giá trị chung (tên bộ, người đọc) được gộp vào mọi kịch bản | Không lặp lại khai báo Emma ở 23 file |
| `check --glossary` nhận nhiều bảng | Chữ cũ ở bảng giai đoạn 1, chữ mới ở bảng giai đoạn 2 |
| Bộ kiểm chữ viết tắt bắt cả chữ bắt đầu bằng số (`2PC`); bỏ qua trạng thái viết hoa trong code (`PENDING`, `COMPLETED`…) và từ khoá SQL | `2PC` trước đây lọt qua lần kiểm |
| `speak.py`: các chữ ở mục 5 | Kiểm phiên âm |
| `coverage --strict` báo lỗi cả khi còn mục nguồn chưa có video | Trước chỉ xét ý chính |
| Web: tab Video có nhiều bộ (nút chọn giai đoạn), nhãn khối tuần `Tuần 5–6`, `Tuần 8–9` | Hai bộ video, hai giọng |
| Pages: copy từng bộ vào `media/jsd/<bộ>/` | Tên file của hai bộ có thể trùng (`ep01-…`) |
| Bảng tự thu nhỏ chữ khi một từ dài (tên test, tên hàm) không vừa cột | Tên test của lab 8 tràn sang cột bên cạnh (Ep15) |
| `speak.py`: chữ A giữa câu, DDIA, SLO, draw.io (mục 5) | Lỗi đọc tìm ra lúc dựng; có test |
| Test: mọi ý chính và mọi mục nguồn của giai đoạn 2 đều có video | Giữ độ phủ 100% khi sửa kịch bản về sau |

Lệnh (chạy trong `tools/`, `L=../java-system-design/video-gd2/lessons`):

```bash
R="uv run --with-requirements lesson_video/requirements.txt python3 -m lesson_video"
$R check $L/ep*.yaml --glossary ../java-system-design/video-gd1/01-bang-chu-viet-tat.md \
                     --glossary ../java-system-design/video-gd2/01-bang-chu-viet-tat.md
$R build $L/ep05-kv-store-cache.yaml                       # Kokoro af_heart, theo series.yaml
$R coverage $L --points $L/points.yaml --title "Độ phủ nội dung — video giai đoạn 2" \
   --link-prefix ../ --out ../java-system-design/video-gd2/02-do-phu.md --strict
```

---

## 7. Các đợt làm

| Đợt | Phạm vi | Đầu ra | Điều kiện xong |
|---|---|---|---|
| **0** | Kế hoạch | File này, [bảng chữ viết tắt giai đoạn 2](01-bang-chu-viet-tat.md), kiểm phát âm, thay đổi công cụ và web | **Xong 08/10/2026** |
| **1** | Cả giai đoạn 2 | Ep00–Ep22, [độ phủ](02-do-phu.md), tab Video có giai đoạn 2 | **Xong 08/10/2026**, chờ duyệt (người dùng yêu cầu làm hết một lần rồi tạo PR) |

---

## 8. Bảng trạng thái

Trạng thái: **—** chưa làm · **Kịch bản** đã viết YAML · **Chờ duyệt** đã dựng Kokoro và qua các lệnh
kiểm · **Xong** người dùng đã duyệt.

| Mã | Tuần | Trạng thái | Dài thật | MB | Chương | Đợt | Ghi chú |
|---|:---:|:---:|---:|---:|---:|:---:|---|
| Ep00 | — | **Chờ duyệt** | 6:02 | 5,5 | 9 | 1 | 9 ý chính · 13 chữ viết tắt |
| Ep01 | 5–6 | **Chờ duyệt** | 7:11 | 6,1 | 7 | 1 | 7 ý chính · 10 chữ viết tắt |
| Ep02 | 5–6 | **Chờ duyệt** | 6:16 | 5,3 | 7 | 1 | 6 ý chính · 3 chữ viết tắt |
| Ep03 | 5–6 | **Chờ duyệt** | 5:50 | 5,0 | 7 | 1 | 5 ý chính · 4 chữ viết tắt |
| Ep04 | 5–6 | **Chờ duyệt** | 5:26 | 5,0 | 8 | 1 | 6 ý chính · 3 chữ viết tắt |
| Ep05 | 5–6 | **Chờ duyệt** | 6:14 | 5,3 | 6 | 1 | 6 ý chính · 11 chữ viết tắt |
| Ep06 | 5–6 | **Chờ duyệt** | 5:57 | 5,3 | 6 | 1 | 7 ý chính · 17 chữ viết tắt |
| Ep07 | 7 | **Chờ duyệt** | 6:12 | 5,6 | 7 | 1 | 8 ý chính · 8 chữ viết tắt |
| Ep08 | 7 | **Chờ duyệt** | 5:38 | 4,8 | 7 | 1 | 8 ý chính · 7 chữ viết tắt |
| Ep09 | 7 | **Chờ duyệt** | 4:57 | 4,4 | 7 | 1 | 6 ý chính · 6 chữ viết tắt |
| Ep10 | 7 | **Chờ duyệt** | 4:50 | 3,9 | 7 | 1 | 4 ý chính · 3 chữ viết tắt |
| Ep11 | 7 | **Chờ duyệt** | 4:42 | 3,8 | 5 | 1 | 4 ý chính · 5 chữ viết tắt |
| Ep12 | 8–9 | **Chờ duyệt** | 4:56 | 4,1 | 7 | 1 | 7 ý chính · 4 chữ viết tắt |
| Ep13 | 8–9 | **Chờ duyệt** | 3:40 | 3,2 | 6 | 1 | 4 ý chính · 6 chữ viết tắt |
| Ep14 | 8–9 | **Chờ duyệt** | 4:57 | 4,7 | 8 | 1 | 5 ý chính · 7 chữ viết tắt |
| Ep15 | 8–9 | **Chờ duyệt** | 6:08 | 5,6 | 8 | 1 | 9 ý chính · 4 chữ viết tắt |
| Ep16 | 8–9 | **Chờ duyệt** | 4:57 | 4,2 | 8 | 1 | 7 ý chính · 2 chữ viết tắt |
| Ep17 | 8–9 | **Chờ duyệt** | 4:27 | 3,7 | 6 | 1 | 7 ý chính · 3 chữ viết tắt |
| Ep18 | 8–9 | **Chờ duyệt** | 3:02 | 2,6 | 6 | 1 | 2 ý chính · 2 chữ viết tắt |
| Ep19 | 10 | **Chờ duyệt** | 5:37 | 4,7 | 7 | 1 | 8 ý chính · 7 chữ viết tắt |
| Ep20 | 10 | **Chờ duyệt** | 5:19 | 4,5 | 8 | 1 | 8 ý chính · 7 chữ viết tắt |
| Ep21 | 10 | **Chờ duyệt** | 4:25 | 3,6 | 5 | 1 | 5 ý chính · 1 chữ viết tắt |
| Ep22 | 10 | **Chờ duyệt** | 6:24 | 5,9 | 8 | 1 | 9 ý chính · 10 chữ viết tắt |

**Đã dựng 23/23 video, 2:03:02 (≈ 123 phút), 106 MB — chờ duyệt. Đã duyệt 0/23.**

---

## 9. Nhật ký / lịch sử đã làm

Mỗi đợt thêm một mục ở **cuối** danh sách: ngày, làm gì, số liệu, lỗi gặp và cách xử lý, commit.

### Đợt 0 — 08/10/2026: lập kế hoạch

- **Yêu cầu:** làm video giai đoạn 2 với giọng Emma `af_heart`, làm hết rồi tạo PR. "Giai đoạn 2" hiểu
  theo lộ trình: tuần 5–10, file 20. Tuần 2–4 của giai đoạn 1 vẫn còn trong
  [kế hoạch giai đoạn 1](../video-gd1/00-ke-hoach-va-lich-su.md#7-các-đợt-làm).
- **Đọc nguồn**: toàn bộ file 20; phần giai đoạn 2 và capstone của file 00; P20 và §7 của file 01;
  §3.3 và §5 của file 02; kết quả đo của dist-lab và perf-lab D20.
- **Chốt 23 video** (mục 0, 3) và phân công mục nguồn (mục 4).
- **Kiểm phát âm** khoảng 90 thuật ngữ mới (mục 5); thêm quy tắc vào `speak.py`, có test.
- **Công cụ và web** đổi theo mục 6.

### Đợt 1 — 08/10/2026: dựng cả 23 video

Yêu cầu: làm hết video giai đoạn 2 bằng giọng Emma `af_heart`, xong thì tạo PR.

- **Kịch bản**: 23 file YAML ở [`lessons/`](lessons/), 616 câu, 16.820 từ. Emma khai báo một lần ở
  [`series.yaml`](lessons/series.yaml).
- **Ý chính**: [`lessons/points.yaml`](lessons/points.yaml) có **147 ý chính** lấy từ nguồn, mỗi ý có chữ
  bắt buộc (`expect`) và mục nguồn. Phạm vi là **67 mục** (heading) của file 20, 00, 01 và 02.
- **Kết quả kiểm** (`check` với hai bảng chữ viết tắt, `coverage --strict`): 147/147 ý chính và 67/67 mục
  có video; mọi chữ viết tắt trên slide và trong lời đọc đều có trong bảng và trên thẻ của video. Chi
  tiết ở [02 — Độ phủ nội dung](02-do-phu.md). Test `test_phase2_points_and_headings_all_covered` giữ
  mức 100% này khi sửa kịch bản về sau.
- **Dựng**: Kokoro `af_heart`, tốc độ 0,9, hai tiến trình song song. Tổng 2:03:02 (≈ 123 phút),
  106 MB, mỗi file khoảng −16 LUFS.
- **Soát bố cục**: xem ảnh cuối mỗi cảnh (ghép 4 ảnh một tấm) trước khi dựng. Các lỗi đã sửa:
  - Code: dòng quá dài (Ep00); callout che dòng cuối (Ep04).
  - Bảng: tên định danh dài tràn sang cột bên (Ep09, Ep15). Công cụ nay tự thu nhỏ chữ khi một từ không
    vừa cột; ở Ep15 tên test xuống dòng tại dấu `_`.
  - Bảng 8 dòng chữ quá nhỏ: tách hai slide (Ep05).
  - Sơ đồ: mũi tên chồng lên hộp (Ep07); nhãn mũi tên bị cắt (Ep08); hộp tràn khung (Ep12). Sơ đồ V7
    (Ep18) và sơ đồ scheduler (Ep21) xếp lại để đường nối không cắt qua hộp.
  - Ô sơ đồ luồng chứa tên cấu hình dài: rút từ 5 ô xuống 4 ô (Ep13).
  - "Raft in five lines": 5 ô chữ nhỏ đổi thành danh sách đánh số (Ep19).
  - Khung design doc bị đánh số hai lần (Ep22).
- **Phát âm** ([mục 5](#5-phát-âm)): kiểm lời đọc của mọi câu có số hoặc chữ viết tắt sau khi qua
  `speak.py`, rồi phiên âm bằng bộ tách âm của Kokoro. Các lỗi tìm ra:
  - "lab 5B" đọc thành *five billion*.
  - DDIA đọc thành *dee dee eye eye*.
  - Chữ A đứng riêng giữa câu (relay A, topic A, fix A, JPA, SHA-512) đọc thành mạo từ.
  - SLO đọc thành *slow*; đuôi `.io` đọc thành *ee-oh*; "retryable" đọc thành *re-tri-AY-ble*.
  - Còn lại chưa sửa: Redis đọc *rih-deez* (xem mục 5).

  Đã sửa `speak.py` và thêm test. Dựng lại các video đã dựng trước khi sửa: Ep00, Ep01, Ep03, Ep13, Ep15, cùng
  Ep04 và Ep06 của giai đoạn 1. Một script so lời đọc hiện tại của từng câu với tiếng mà file mp4 đã dùng.
  Kết quả: cả 23 video và 7 video giai đoạn 1 đều dùng lời đọc mới nhất.
- **Âm thanh**: đo từng file bằng `ffmpeg ebur128`. Ep12 và Ep14 có 2–3 mẫu vượt 0 dBFS ở âm bật *P*
  sau khi mã hoá AAC 64 kbit/s; mỗi chỗ dưới 0,1 ms, không nghe thấy, giữ nguyên.
- **Sửa nguồn**: ở lab 10A của file 20, điều kiện ghi đổi từ `last_token < ?` thành `last_token <= ?`.
  Bản cũ chặn cả lần ghi thứ hai của chính người đang giữ token mới nhất.
- **Sự cố**: container khởi động lại hai lần giữa lúc dựng (lúc Ep04–Ep05, rồi lúc Ep19–Ep20). Tiến
  trình dựng mất nhưng file vẫn còn. Đưa hai video đó vào lại hàng đợi; tiếng đã đọc nằm trong bộ nhớ
  đệm nên chỉ phải đọc những câu còn thiếu.
- **App**: tab **Video** có nút chọn giai đoạn (lựa chọn được lưu) và nhãn khối tuần `Tuần 5–6`,
  `Tuần 8–9`. Pages copy từng bộ vào `media/jsd/<bộ>/`.
- **Kiểm app bằng Playwright**:
  - Tab hiện đủ 30 video (23 + 7). Lựa chọn giai đoạn được giữ sau khi tải lại trang.
  - Với từng video: tiêu đề, số chương, số ý chính, link mục nguồn, ảnh bìa và đường dẫn mp4 đều khớp
    `lessons.json`.
  - Tua theo chương và theo ý chính đúng giây. Ba nút tài liệu của giai đoạn 2 mở đúng file.
  - Màn hình 390 px không tràn ngang. Không có lỗi JavaScript; chỉ có các yêu cầu ra ngoài bị proxy chặn.
  - Chromium của Playwright không có H.264 nên test thay mọi mp4 bằng một file WebM.
- **Chưa làm**: chưa có người nghe lại toàn bộ, đó là bước duyệt. Góp ý về giọng, tốc độ, độ dài hay bố
  cục dựng lại nhanh vì tiếng đã nằm trong bộ nhớ đệm.
- Commit trên nhánh `claude/wizardly-pascal-9yhum5`.

---

## 10. Quyết định đã chọn mặc định

| # | Câu hỏi | Mặc định | Phương án khác |
|---|---|---|---|
| 1 | Giai đoạn 2 là gì | Tuần 5–10 của lộ trình (file 20) | Tuần 2 của giai đoạn 1 (Ep07–Ep12 ở kế hoạch giai đoạn 1) |
| 2 | Giọng | Emma `af_heart` cho cả bộ giai đoạn 2; giai đoạn 1 giữ Tom | Đọc lại giai đoạn 1 bằng Emma (khoảng 25 phút Kokoro mỗi video) |
| 3 | Tốc độ | 0,9 như giai đoạn 1; khi xem có nút 1,25× và 1,5× | 1,0 |
| 4 | Lưu video | Commit mp4 vào git như giai đoạn 1 | Git LFS |

---

## 11. Ranh giới trung thực

| Nội dung | Trạng thái |
|---|---|
| Số video, thời lượng | Mục 0 và 3 là **ước tính** ban đầu; mục 8 là **số thật** đo từ file đã dựng (`lessons.json`) |
| Phát âm | **Đã kiểm** bằng bộ tách âm của Kokoro cho các thuật ngữ ở mục 5 và mọi câu có số hoặc chữ viết tắt; chưa nghe lại toàn bộ bằng tai |
| Âm thanh | Mọi video chuẩn hoá về −16 LUFS, đỉnh khoảng −1 dBFS; riêng Ep12 có 3 mẫu vượt 0 dBFS ở âm bật *P* của “P20” sau khi mã hoá AAC (0,07 ms, không nghe thấy) |
| Số liệu trong video | Lấy từ file 20, 01, 02 và output đã chạy của dist-lab, perf-lab; lab Spring Boot (7, 8), lab 5B, 10A bản Postgres, 10B **chưa chạy** trong workspace này, video nói rõ điều đó |
