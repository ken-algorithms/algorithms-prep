# Module 09 — Kafka High-Throughput Event Pipeline & Troubleshooting

```bash
mvn -pl 09-kafka-pipeline test    # Chạy 7 unit/integration tests kiểm chứng 5 bài toán
```

> **Mục tiêu của module:** Biến các thiết kế lý thuyết "trên giấy" của bài phỏng vấn Katalon Principal 
> ([04 — Event Counting 10k/phút](../../katalon-system-design/04-event-counting-10k.md) và 
> [07 — Event Counting 10M/phút](../../katalon-system-design/07-event-counting-10m-100m.md)) thành 
> **mã nguồn Spring Boot 3 + Java 21 chạy thật**, tái hiện trực tiếp 5 sự cố production kinh điển và 
> code cách giải quyết triệt để.

---

## 1. Kiến trúc luồng dữ liệu (Data Flow)

```text
[Client / SDK]
      │
      ▼  (POST /events · batch ~20 items, batch_id)
[Ingestion API] ─── TokenBucketLimiter (chặn burst vượt ngưỡng -> HTTP 429)
      │
      ▼  (KafkaTemplate · key = batch_id · acks=all · snappy · linger.ms=20)
[Kafka Topic: events.ingest] (4 partitions · RF 3)
      │
      ├──────────────────────────────┐
      │ (Batch Listener)             │ (Lỗi Deserializer / Poison Pill)
      ▼                              ▼
[Batch Consumer Worker]       [Dead Letter Topic: .DLT]
      │                              │
      ├─ Dedup qua processed_batch   └─ Lưu audit / cảnh báo PagerDuty
      ├─ Phân loại: TRUE, FALSE, FAKE_KEY, MALFORMED
      ▼
[Database: PostgreSQL / H2]
      └── Bảng agg_1m (tenant_id, bucket_minute, outcome, cnt)
```

---

## 2. 5 Sự cố Production kinh điển và cách Code để Fix

| # | Sự cố / Lỗi Production | Nguyên nhân | Code tái hiện & Khắc phục | File kiểm chứng |
|---|---|---|---|---|
| **1** | **Partition Hotspot (Skew)** | Key theo `tenant_id` khiến Big Tenant (40% tải) dồn hết vào 1 partition | Đổi partition key sang **`batch_id`** qua [`BatchIdPartitioner`](src/main/java/com/prep/kafka/producer/BatchIdPartitioner.java) $\rightarrow$ Tải chia đều 25% mỗi partition | [`PartitionSkewTest`](src/test/java/com/prep/kafka/PartitionSkewTest.java) |
| **2** | **Consumer Lag & Rebalance Storm (P20)** | Consumer gọi downstream đồng bộ từng message ($500 \times 20\text{ms} = 10\text{s} > \text{max.poll.interval.ms}$) | Chuyển sang **Batch Listener** [`BatchEventConsumer`](src/main/java/com/prep/kafka/consumer/BatchEventConsumer.java) gộp DB update $\rightarrow$ xử lý 500 records trong <50ms | [`BatchProcessingVsSlowConsumerTest`](src/test/java/com/prep/kafka/BatchProcessingVsSlowConsumerTest.java) |
| **3** | **Poison Pill Message làm sập Consumer** | Client gửi JSON sai format làm Deserializer crash lặp vô tận (Head-of-Line Blocking) | Cấu hình [`PoisonPillConfig`](src/main/java/com/prep/kafka/resilience/PoisonPillConfig.java) với `ErrorHandlingDeserializer` + `DeadLetterPublishingRecoverer` đẩy tự động sang `.DLT` | [`PoisonPillDltTest`](src/test/java/com/prep/kafka/PoisonPillDltTest.java) |
| **4** | **Duplicate Delivery (Đếm trùng)** | Client timeout hoặc Kafka retry làm gửi lại batch cũ $\rightarrow$ counter bị tăng khống | [`RollupRepository`](src/main/java/com/prep/kafka/repository/RollupRepository.java) lưu bảng `processed_batch` và cập nhật lũy kế `GREATEST` $\rightarrow$ `reconcile_drift = 0` | [`IdempotentDeduplicationTest`](src/test/java/com/prep/kafka/IdempotentDeduplicationTest.java) |
| **5** | **Producer Buffer Exhaustion** | Tải đột biến làm tràn RAM buffer của Kafka Producer (`BufferExhaustedException`) | Tích hợp [`ProducerRateLimiter`](src/main/java/com/prep/kafka/resilience/ProducerRateLimiter.java) theo thuật toán Token Bucket ở tầng REST Ingest API | [`ProducerRateLimitingTest`](src/test/java/com/prep/kafka/ProducerRateLimitingTest.java) |

---

## 3. Cách chạy Kafka thật (KRaft - Không cần ZooKeeper)

Nếu muốn chạy toàn bộ pipeline với Kafka broker thật ngoài localhost:

```bash
# Cách 1: Tải Kafka 4.x và chạy KRaft độc lập
curl -O https://downloads.apache.org/kafka/4.1.2/kafka_2.13-4.1.2.tgz
tar xzf kafka_2.13-4.1.2.tgz && cd kafka_2.13-4.1.2
bin/kafka-storage.sh format --standalone -t "$(bin/kafka-storage.sh random-uuid)" -c config/server.properties
bin/kafka-server-start.sh config/server.properties    # cổng 9092

# Cách 2: Docker Compose
docker run -d --name kafka -p 9092:9092 apache/kafka:4.1.2
```

Sau khi Kafka chạy, start Spring Boot application:
```bash
mvn -pl 09-kafka-pipeline spring-boot:run
```
Gửi request thử nghiệm:
```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{"batchId":"b1","tenantId":"tenant_1","items":[{"key":"feature_1","value":true},{"key":"fake","value":false}]}'
```
Tra cứu thống kê:
```bash
curl "http://localhost:8080/events/stats?tenantId=tenant_1&bucketMinute=29800000"
```
