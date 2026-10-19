# Lab chuyên sâu: Kafka 4.1.2 KRaft + PostgreSQL 16 trên Docker, 29 test, 10 script thí nghiệm

Code cho các bài [01–10](../README.md) của bộ chuyên sâu. Một project Maven, Java 21, **không Spring**: muốn thấy từng
lớp bên dưới (client Kafka, JDBC) chứ không qua framework. Mỗi test in dòng `[LAB]`, mỗi script in output thô; đó
chính là các con số được trích trong bài.

```bash
docker compose -f docker/cluster.yml up -d --wait   # 3 controller + 3 broker Kafka 4.1.2 + Postgres 16 (~1 phút)
mvn -q test                                         # 29 test trên Kafka + Postgres thật, ~5 phút
mvn test | grep '\[LAB\]'                           # chỉ xem số liệu
scripts/chay-tat-ca.sh                              # chạy lại TOÀN BỘ lab từ cụm trắng, ghi results/, ~50 phút
```

**Cần gì:** Docker + Compose v2, JDK 21+, Maven 3.9+, khoảng 6 GB RAM trống (3 controller heap 256 MB, 3–4 broker heap
512 MB, Postgres, cộng JVM của bài đo). Test cần hạ tầng mà hạ tầng chưa chạy thì tự **skip** kèm lời nhắc lệnh
`docker compose`, không báo lỗi.

---

## Hạ tầng

| File | Dựng gì | Dùng cho |
|---|---|---|
| [`docker/kafka-1node.yml`](docker/kafka-1node.yml) | 1 node Kafka (broker + controller chung tiến trình) | Bài 01: chạy, đọc `server.properties` sinh ra, 4 kiểu kết nối |
| [`docker/cluster.yml`](docker/cluster.yml) | `controller-1..3` (quorum KRaft), `kafka-1..3`, `postgres`; `kafka-4` nằm trong profile `scale` | Bài 01–10 |

| Kết nối | Từ máy host | Từ container cùng network |
|---|---|---|
| Kafka bootstrap | `localhost:19092,localhost:29092,localhost:39092` (kafka-4: `49092`) | `kafka-1:9092,kafka-2:9092,kafka-3:9092` |
| Postgres | `jdbc:postgresql://localhost:55432/lab`, user/pass `lab`/`lab` | `postgres:5432` |

Cụm lab cố ý khác production ở vài chỗ cho thí nghiệm nhanh (đều ghi trong `cluster.yml`): `replica.lag.time.max.ms`
10 s, quét retention mỗi 5 s, heap nhỏ. Bảng khác biệt đầy đủ ở [bài 05 §11](../05-kafka-scale-va-van-hanh.md).

```bash
docker compose -f docker/cluster.yml --profile scale up -d --wait kafka-4    # thêm broker 4 (bài 05)
docker compose -f docker/cluster.yml --profile scale down -v                 # xoá sạch, kể cả dữ liệu
docker exec -it kafka-1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka-1:9092 --list
docker exec -it postgres psql -U lab -d lab
```

---

## Script thí nghiệm

Mỗi script ghi output vào `results/` khi chạy bằng `scripts/<tên>.sh 2>&1 | tee results/<tên>.txt`.

| Script | Bài | Làm gì | Thời gian |
|---|---|---|---|
| [`k01-chay-va-ket-noi.sh`](scripts/k01-chay-va-ket-noi.sh) | 01 | Dựng 1 node, đọc cấu hình sinh ra, vòng đời broker, 4 kiểu kết nối | ~3 phút |
| [`k01b-min-isr-elr.sh`](scripts/k01b-min-isr-elr.sh) | 01 | Bẫy `min.insync.replicas` của Kafka 4.x (ELR) trên cụm 1+1 riêng | ~1 phút |
| [`k02-ben-trong-broker.sh`](scripts/k02-ben-trong-broker.sh) | 02 | File segment, `kafka-dump-log`, index, offset, metadata log, retention, compaction | ~4 phút |
| [`k03-replication-va-su-co.sh`](scripts/k03-replication-va-su-co.sh) | 03 | Giết broker khi đang ghi: `acks=1` mất, `acks=all` không; mất quorum | ~6 phút |
| [`k04-producer-consumer.sh`](scripts/k04-producer-consumer.sh) | 04 | Tinh chỉnh producer, ba kiểu rebalance, ba kiểu commit, transaction | ~5 phút |
| [`k05-scale.sh`](scripts/k05-scale.sh) | 05 | Thêm broker 4, reassign có throttle, bầu leader, đổi số partition, scale consumer | ~4 phút |
| [`k05b-thu-nho-cum.sh`](scripts/k05b-thu-nho-cum.sh) | 05 | Rút broker 4: tắt thẳng (sai), rồi chuyển ra có throttle + gỡ đăng ký (đúng) | ~2 phút |
| [`e-curl-demo.sh`](scripts/e-curl-demo.sh) | 07 | API thật bằng curl: 202/400, gửi lại cùng body, epoch mới, `GET /stats` | ~1 phút |
| [`e-http-bench.sh`](scripts/e-http-bench.sh) | 07 | CPU trên request: JDK HttpServer, Netty, ingest đầy đủ | ~3 phút |
| [`e-ingest-fanout.sh`](scripts/e-ingest-fanout.sh) | 09 | Số partition và flush → cỡ block, record/s, rác mỗi request | ~4 phút |
| [`run.sh`](scripts/run.sh) | — | Chạy một class có `main()`: `scripts/run.sh com.prep.cs.kafka.ConnectCheck localhost:19092` | — |
| [`chay-tat-ca.sh`](scripts/chay-tat-ca.sh) | — | Chạy lại mọi thứ theo đúng thứ tự để số liệu các bài khớp nhau | ~50 phút |

`results/e-ingest-fanout-truoc.txt` đo bằng bản `IngestNode` **trước** khi sửa bộ đệm (bài 09 §4.1), nên code hiện
tại không tái tạo được nó. Muốn chạy lại: tạm đổi `Buf.cut()` về cấp phát bộ đệm 64 KB mới cho mỗi block như bản cũ mô
tả ở bài 09 §4.1, rồi chạy `e-ingest-fanout.sh`.

---

## Bản đồ code

| Bài | Package (`src/main/java/com/prep/cs/…`) | Test |
|---|---|---|
| 01 | `kafka/`: `ConnectCheck`, `ClientDefaults` | — (script) |
| 03 | `kafka/`: `SteadyProducer`, `AckLossCheck`, `QuorumProbe` | — (script) |
| 04 | `kafka/`: `ProducerTuning`, `GroupRebalanceDemo`, `CommitSemantics`, `TxnDemo`, `RobustConsumerLoop` | — (script) |
| 05 | `kafka/`: `PartitionMapping`, `ScaleConsumers`, `Decommission` | — (script) |
| 06–09 | `counting/`: `Hash128`, `KeyRegistry`, `RequestDecoder`, `RequestRecord`, `IngestNode`, `RegistryTopic`, `ClientWindow`, `LongObjectMap`, `PartitionAggregator`, `CountStore`, `Aggregator`, `RangeDecomposer`, `NettyIngestServer`, `HttpIngestServer`, `LoadGen` | `KeyRegistryTest`, `RequestDecoderTest`, `ClientWindowTest`, `LongObjectMapTest`, `CountStoreTest`, `RangeDecomposerTest`, `PipelineTest` |
| 07–09 | `counting/bench/`: `CountingBench` (JMH), `IngestBench`, `AggregatorBench`, `RawHttpLoad` | — |
| 10 | `ledger/mem/`: `Wallets`, `WalletStress` · `ledger/db/`: `LedgerDb`, `Ledgers`, `BalanceReads`, `Holds` | `MemWalletTest`, `LedgerDbTest`, `BalanceReadTest`, `HoldsAndHotAccountTest` |

Schema: [`counting.sql`](src/main/resources/counting.sql), [`ledger_cs.sql`](src/main/resources/ledger_cs.sql). Hỗ trợ
test: `Infra` (địa chỉ hạ tầng; skip nếu chưa chạy).

## Đo hiệu năng

```bash
# JMH: tắt cụm cho máy rảnh rồi đo (~4 phút)
docker compose -f docker/cluster.yml stop
mvn -q compile && java -cp "target/classes:$(cat target/cp.txt)" org.openjdk.jmh.Main CountingBench -prof gc
docker compose -f docker/cluster.yml up -d --wait

BS=localhost:19092,localhost:29092,localhost:39092
scripts/run.sh com.prep.cs.counting.bench.IngestBench "$BS" 3 10            # 1 ingest node, 3 luồng, 10 giây
scripts/run.sh com.prep.cs.counting.bench.IngestBench "$BS" 3 10 48 20      # ... 48 partition, flush 20 ms
scripts/run.sh com.prep.cs.counting.bench.AggregatorBench localhost:19092 20000 500   # 10,5 triệu request
```

`target/cp.txt` (classpath) do `scripts/run.sh` tạo lần đầu.

---

## Kết quả

Lần chạy gần nhất: [`results/`](results/). JDK 21, Kafka 4.1.2, PostgreSQL 16, Docker trên container Linux **4 vCPU**
dùng chung cho mọi thứ. Đọc số theo hướng dẫn ở [README bộ bài](../README.md#đọc-số-liệu-cho-đúng): tỷ lệ và xu hướng
tin được, số tuyệt đối thì không. Mọi khẳng định đúng/sai (mất dữ liệu, đếm trùng, deadlock, khớp oracle) là assert
trong test.

| File | Bài |
|---|---|
| `k01-chay-va-ket-noi.txt`, `k01b-min-isr-elr.txt`, `k01c-quorum.txt` | 01 |
| `k02-ben-trong-broker.txt` | 02 |
| `k03-replication-va-su-co.txt` | 03 |
| `k04-producer-consumer.txt` | 04 |
| `k05-scale.txt`, `k05b-thu-nho-cum.txt` | 05 |
| `e-curl-demo.txt`, `e-http-bench.txt`, `e-ingest-bench.txt`, `e-countingbench.txt` | 07 |
| `e-aggregator-bench.txt` | 08 |
| `e-ingest-fanout-truoc.txt`, `e-ingest-fanout.txt` | 09 |
| `test-output.txt` (các dòng `[LAB]` và tổng kết của `mvn test`) | 07–10 |

Không commit: `acked-*.txt`, `k03-*-producer.txt` (file trung gian của bài 03), `test-output-full.txt`.

---

## Gặp lỗi

| Triệu chứng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `toomanyrequests` khi kéo `postgres:16` | Docker Hub giới hạn lượt kéo | `docker pull mirror.gcr.io/library/postgres:16 && docker tag mirror.gcr.io/library/postgres:16 postgres:16` |
| Client treo ở `localhost:9092` hoặc nối được bootstrap rồi lỗi | Dùng nhầm listener (bài 01 §6) | Từ host dùng `localhost:19092,...`; từ container dùng `kafka-1:9092,...` |
| Cổng đã bị chiếm | Kafka hay Postgres khác đang chạy | Đổi cổng trong `cluster.yml` (19092/29092/39092/49092/55432) |
| Script k02–k05 lệch số so với bài | Cụm còn topic từ lần chạy trước | `docker compose -f docker/cluster.yml --profile scale down -v` rồi dựng lại; hoặc chạy `chay-tat-ca.sh` |
| Số đo thấp bất thường | Cụm còn hàng trăm partition rác, hoặc máy đang chạy việc khác | Xoá topic rác (`kafka-topics --delete --topic 'ingest-bench-.*'`), chạy lại |
| `dockerd` không chạy sau khi máy ngủ | Docker daemon dừng | Khởi động lại Docker rồi `docker compose ... up -d --wait` |
