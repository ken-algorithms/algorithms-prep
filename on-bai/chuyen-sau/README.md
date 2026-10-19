# Ôn bài · Chuyên sâu — Kafka từ bên trong, đếm sự kiện 1M–10M request/s, sổ cái tính số dư

> Bộ bài đi **sâu hơn** giai đoạn 1. Không dừng ở "dùng Kafka", "đếm bằng bucket", "khoá dòng": bộ bài mở từng lớp
> bên trong và **viết code từng bước** cho ba chủ đề hay bị đào nhất ở Katalon và NAB. Mọi con số đều đo trên hệ thống
> chạy thật: Kafka 4.1.2 chế độ KRaft (3 controller + 3–4 broker) và PostgreSQL 16, dựng bằng Docker Compose.
>
> Toàn bộ code nằm trong **một** project Maven Java 21, không Spring: [`lab/`](lab/README.md). **29 test JUnit, 0 lỗi**,
> cộng 7 script thí nghiệm Kafka, 3 script đo và demo cho bài đếm, và JMH. Output thật ở [`lab/results/`](lab/results/).

```bash
cd on-bai/chuyen-sau/lab
docker compose -f docker/cluster.yml up -d --wait     # 3 controller + 3 broker Kafka + Postgres 16
mvn -q test                                           # 29 test trên Kafka + Postgres thật, ~5 phút
```

---

## Danh sách bài

| # | Bài | Trọng tâm phải nói được | Katalon | NAB | Lab (test) |
|:-:|---|---|:-:|:-:|---|
| 01 | [Kafka chạy bằng Docker và kết nối](01-kafka-chay-docker-va-ket-noi.md) | KRaft: controller và broker; `listeners` khác `advertised.listeners`, 4 kiểu kết nối; bẫy `min.insync.replicas` của 4.x | ★★ | ★★★ | `k01-chay-va-ket-noi.sh`, `k01b-min-isr-elr.sh` |
| 02 | [Bên trong broker: segment, index, offset, compaction](02-kafka-ben-trong-broker.md) | Segment + index thưa; record batch v2; `__consumer_offsets`; metadata log; retention xoá cả segment; compaction | ★★ | ★★★ | `k02-ben-trong-broker.sh` |
| 03 | [Replication và sự cố: tái hiện mất dữ liệu đã xác nhận](03-kafka-replication-va-su-co.md) | LEO, HW, ISR; `acks=1` mất 1.566 bản ghi đã xác nhận, `acks=all` mất 0; controlled shutdown so với `kill -9`; mất quorum | ★★ | ★★★ | `k03-replication-va-su-co.sh` |
| 04 | [Producer và consumer từ code](04-kafka-producer-consumer-tu-code.md) | `send()` không gửi gì; batch + nén ×6; rebalance eager, cooperative, KIP-848; ba kiểu commit; transaction và LSO | ★★★ | ★★★ | `k04-producer-consumer.sh` |
| 05 | [Scale và vận hành Kafka](05-kafka-scale-va-van-hanh.md) | Broker mới rỗng; reassign có throttle (12,8 s đúng như tính); rút broker đúng thứ tự; key đổi partition 85,7%; tính cỡ cụm bằng byte | ★★★ | ★★ | `k05-scale.sh`, `k05b-thu-nho-cum.sh` |
| 06 | [Đếm sự kiện 1M–10M request/s: đề, con số, kiến trúc](06-dem-su-kien-1m-10m-de-va-kien-truc.md) ⭐ | Đổi ra byte và CPU; 4 cách trực giác chết; phân loại sớm, bản ghi 32 B, khử trùng theo client, commit cùng offset | ★★★ | ★★ | `e-curl-demo.sh` |
| 07 | [Code phía ingest: từ JSON tới bản ghi 32 byte](07-dem-su-kien-code-ingest.md) | Parser streaming 10 µs; registry 128 bit trên hai mảng `long`; block theo partition; 202 sau ack; 80 µs CPU/request | ★★★ | ★ | `KeyRegistryTest` (2), `RequestDecoderTest` (4) |
| 08 | [Code phía aggregator: khử trùng, commit cùng offset, đọc khoảng](08-dem-su-kien-code-aggregator.md) ⭐ | Cửa sổ 64 bit 2,4 ns; delta theo giây 28,6 ns; offset là fencing token, cả hai thứ tự xác sống; ≤ 282 + số ngày bucket | ★★★ | ★★ | `ClientWindowTest` (2), `LongObjectMapTest` (1), `CountStoreTest` (4), `RangeDecomposerTest` (3) |
| 09 | [Chạy thật với sự cố, đo fan-out, scale 1M → 10M](09-dem-su-kien-chay-that-va-scale.md) | Test toàn tuyến khớp oracle qua 7 sự cố; fan-out: 48 partition chậm hơn 12 khoảng 25%; kế hoạch cỡ hệ thống | ★★★ | ★★ | `PipelineTest` (1) |
| 10 | [Sổ cái và số dư từ code](10-ledger-tinh-so-du-tu-code.md) ⭐ | V1 tạo 470 đồng; V2 deadlock 145/200; V3 một câu `UPDATE` + idempotency + khoá theo id; 4 cách đọc số dư; tài khoản nóng | ★★ | ★★★ | `MemWalletTest` (4), `LedgerDbTest` (4), `BalanceReadTest` (2), `HoldsAndHotAccountTest` (2) |

★★★ gần như chắc chắn bị hỏi · ★★ hay gặp · ★ để hiểu sâu. ⭐ là ba bài nên ôn kỹ nhất: kiến trúc đếm sự kiện (06),
phần khó nhất của nó (08), và sổ cái (10).

---

## Thứ tự học

```text
Kafka:      01 → 02 → 03 → 04 → 05          (bài 05 §10 là cầu nối sang bài đếm)
Đếm:        06 → 07 → 08 → 09               (06 đọc trước để biết mình đang code cái gì)
Sổ cái:     10                              (đọc gd1 bài 04 trước nếu chưa chắc về khoá và isolation)
```

- **Thời gian có hạn:** 06 → 08 → 10 → 04 → 03. Đủ để trả lời bài fake key ở 10M request/s và bài chuyển tiền.
- **Phỏng vấn NAB trước:** 10 → 03 → 04 → 01 → 02, rồi đọc lướt 06.
- **Phỏng vấn Katalon trước:** 06 → 07 → 08 → 09, rồi 05 §10 và 04.

---

## Bộ bài này thêm gì so với giai đoạn 1 và tài liệu đã có

Giai đoạn 1 ([`gd1-nen-tang/`](../gd1-nen-tang/README.md)) và các tài liệu
[Katalon 04](../../katalon-prep/katalon-system-design/04-event-counting-10k.md),
[Katalon 07](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md),
[NAB 04](../../nab-prep/04-kafka-event-driven-saga.md) đã có lời giải ở mức thiết kế. Bộ chuyên sâu thêm ba thứ:

1. **Kafka nhìn từ bên trong, bằng chính file và log của broker:** `kafka-dump-log` từng segment, index thưa, snapshot
   producer, metadata log của KRaft; tái hiện **mất 1.566 bản ghi đã xác nhận** với `acks=1`; đo chuyển partition
   khớp lý thuyết tới từng giây.
2. **Bài đếm ở 1M–10M request/s, viết đủ code và chứng minh bằng sự cố.** Lời giải ở mức 10M/**phút** khử trùng bằng
   tập `batch_id`. Ở 10M/**giây**, tập đó là 144 GB, nên bộ bài đổi sang khử trùng theo client (64 bit/client) và
   commit offset cùng số đếm. Một test toàn tuyến giết aggregator, giết ingest, retry, khởi động lại client, và số cuối
   **khớp chính xác** oracle.
3. **Những điều chỉ chạy thật mới thấy**, đáng kể trong phòng phỏng vấn:
   - Kafka 4.x: đặt `min.insync.replicas` ở broker **không đủ**; controller ghi giá trị của nó thành mặc định cấp cụm
     ([01](01-kafka-chay-docker-va-ket-noi.md)).
   - Sau khi đổi leader quorum, broker mất ~10 giây mới chuyển kênh forward sang controller mới
     ([03](03-kafka-replication-va-su-co.md)).
   - Fan-out của ingest: bộ đệm 64 KB cấp mới cho **mỗi** block làm rác tăng theo số partition; dùng lại bộ đệm thì rác
     ở 12 partition giảm từ 3,2–3,4 KB xuống 1,8 KB mỗi request. Phần còn lại là bộ đệm lz4 mà Kafka cấp cho mỗi batch
     ([09](09-dem-su-kien-chay-that-va-scale.md)).
   - Aggregator xác sống commit **trước** chủ mới thì bản đầu làm chủ thật buông partition; sửa bằng nạp lại từ Postgres
     ([08](08-dem-su-kien-code-aggregator.md)).
   - Tài khoản nóng: `synchronous_commit = off` không giúp; bớt round trip mới giúp, vì thời gian giữ khoá gồm cả mạng
     ([10](10-ledger-tinh-so-du-tu-code.md)).

---

## Đọc số liệu cho đúng

Lab chạy trên **một máy 4 vCPU** dùng chung cho 6–7 JVM Kafka, Postgres và chính chương trình đo. Hệ quả:

- Số tuyệt đối thấp hơn máy thật và **dao động giữa các lần chạy**, có khi gấp đôi. Mỗi bài ghi rõ khi nào nên đọc tỷ lệ
  thay vì số tuyệt đối, và nhiều thí nghiệm chạy hai vòng để thấy độ nhiễu.
- Trạng thái cụm ảnh hưởng số đo: cùng code, cụm còn 765 partition rác thì HTTP ingest chỉ đạt 6.364 request/s; dọn
  còn 136 partition thì đạt 14.329 request/s.
- Mọi khẳng định về **đúng sai** (mất dữ liệu, đếm trùng, deadlock, khớp oracle) thì không phụ thuộc máy: chúng là
  assert trong test.

---

## Liên quan

- Giai đoạn 1: [README](../gd1-nen-tang/README.md) · [sổ cái và số dư](../gd1-nen-tang/05-ledger-va-balance.md) ·
  [Postgres concurrency](../gd1-nen-tang/04-postgres-concurrency.md) · [outbox](../gd1-nen-tang/09-outbox-notification.md)
- Katalon: [7 họ bài](../../katalon-prep/katalon-system-design/README.md) · [đếm sự kiện 10k](../../katalon-prep/katalon-system-design/04-event-counting-10k.md) ·
  [10M–100M/phút](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md) ·
  [race trên số dư](../../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md)
- NAB: [Kafka, event-driven, saga](../../nab-prep/04-kafka-event-driven-saga.md)
- Code Java chậm dưới tải cao: [track P](../../java-system-design/01-java-code-cham-duoi-tai-cao.md)
