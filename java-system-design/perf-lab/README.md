# perf-lab — code Java chậm dưới tải cao, đo được

Code chạy được cho [Track P](../01-java-code-cham-duoi-tai-cao.md). Một module Maven, không cần
Docker hay DB thật; riêng D20 (P20) cần một broker Kafka.

```bash
mvn -q test                                              # 33 test, ~6 giây
mvn -q package -DskipTests                               # target/benchmarks.jar
java -cp target/benchmarks.jar com.prep.perf.demo.RunAll # nhóm 2-4, ~40 giây
java -jar target/benchmarks.jar -prof gc                 # nhóm 1 + P11, ~6 phút
java -cp target/benchmarks.jar com.prep.perf.demo.D20SlowKafkaConsumer   # P20, cần Kafka localhost:9092
```

Đã chạy ngày 04/10/2026 trên container Linux 4 vCPU, JDK 21.0.11: **33 test, 0 failure**. D20 chạy
ngày 07/10/2026 với Apache Kafka 4.1.2. Output thô ở [results/](results/).

## Hai loại đo, hai loại kiểm tra

| | Đo cái gì | Công cụ | Kiểm tra bằng test |
|---|---|---|---|
| **Nhóm 1** (P01–P07) và P11 | CPU và byte rác **mỗi lần gọi** | JMH `-prof gc` | `SameResultTest`: bản sửa trả **cùng kết quả** với bản xấu |
| **Nhóm 2–4** (P09, P10, P12, P13, P15, P18, P19) | Hệ quả **hàng đợi** ở mức hệ thống: throughput, p99, OOM | Chương trình `main`, gom p50/p95/p99 | `CountedEffectsTest`: chỉ assert thứ **đếm được** (số query, số entry, số request bị từ chối) |

Không có test nào assert thời gian, trừ P12 (chênh ~200 lần nên đủ chắc), và test đó tự tắt trên
JDK 24+ vì pinning không còn tồn tại ở đó.

## Bản đồ file

| Anti-pattern | Code | Đo bằng |
|---|---|---|
| P01 object đắt tạo lại | `code/P01ExpensiveObjects` | `bench/PerRequestBench` |
| P02 regex compile | `code/P02Regex` | `bench/PerRequestBench` |
| P03 nối chuỗi trong vòng lặp | `code/P03StringConcat` | `bench/BatchBench` |
| P04 exception làm luồng điều khiển | `code/P04ExceptionFlow` | `bench/PerRequestBench$ExceptionBench` |
| P05 autoboxing | `code/P05Boxing` | `bench/BatchBench` |
| P06 dựng chuỗi log khi tắt | `code/P06Logging` | `bench/PerRequestBench` |
| P07 O(n²) ẩn | `code/P07HiddenQuadratic` | `bench/BatchBench` |
| P09 gọi ngoài trong transaction | `demo/D09PoolExhaustion` | `main` |
| P10 không timeout | `demo/D10NoTimeout` | `main` |
| P11 khoá toàn cục | `code/P11Contention` | `bench/ContentionBench` |
| P12 virtual thread pinning | `demo/D12Pinning` | `main` (JDK 21–23) |
| P13 hàng đợi vô hạn | `demo/D13UnboundedQueue` | `main` |
| P15 N+1 | `demo/D15NPlusOne` | `main` |
| P18 cache không giới hạn | `demo/D18UnboundedCache` | `main`, JVM con `-Xmx128m` |
| P19 nạp hết vào bộ nhớ | `demo/D19MaterializeAll` | `main`, JVM con `-Xmx512m` |
| P20 Kafka consumer chậm | `demo/D20SlowKafkaConsumer` | `main`, **cần Kafka thật** ở `localhost:9092`; không nằm trong `RunAll` |

P08, P14, P16, P17 chưa có code; lý do ghi ở [file 01 §10](../01-java-code-cham-duoi-tai-cao.md#10-ranh-giới-trung-thực).

## Bài tập kèm theo

Hướng dẫn từng bước và bài tập tự làm nằm ở [10 — Implement giai đoạn 1](../10-implement-gd1-nen-tang.md)
(tuần 1: nhóm 1 bằng JMH; tuần 2–4: P08–P10, P15–P19) và [20 — Implement giai đoạn 2](../20-implement-gd2-du-lieu-phan-tan.md)
(tuần 8–9: P20).
