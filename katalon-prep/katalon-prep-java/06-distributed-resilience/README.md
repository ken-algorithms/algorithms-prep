# Module 06 — Distributed & resilience (KHÔNG cần Docker)

```bash
mvn -pl 06-distributed-resilience test    # 19 test pass
```

> **Đính chính:** ROADMAP trước ghi module này cần Docker. Đúng một phần — nhưng thứ **phỏng vấn
> hỏi** về Kafka là **ngữ nghĩa**, không phải thao tác: ordering nằm ở đâu, key quyết định gì,
> rebalance làm mất gì, DLQ để làm gì, exactly-once có thật không. Toàn bộ những cái đó mô phỏng
> được trung thực bằng code thuần.

## Hai phần

### 1. `Retries.java` — retry + backoff + jitter

**Thundering herd**: 1.000 worker cùng gọi một service. Service restart. Cả 1.000 nhận lỗi **cùng
lúc**, rồi cùng retry sau đúng 1s, 2s, 4s... Chúng **đồng bộ hoá vĩnh viễn** và đập vào service đang
hồi phục theo từng đợt 1.000 request. Service không bao giờ đứng dậy được.

Test chứng minh bằng số:

| Chiến lược | 1000 client retry lần 3 |
|---|---|
| `EXPONENTIAL` (không jitter) | **1 giá trị duy nhất** — tất cả chờ đúng 200ms |
| `EXPONENTIAL_FULL_JITTER` | **>100 giá trị khác nhau**, rải đều `[0, 200ms]` |

Ba điểm khác đáng nói:

- **`cap`** chặn thời gian chờ. Thiếu nó thì lần thử 20 chờ 12 ngày — nghe buồn cười nhưng là bug
  thật trong các thư viện retry tự viết.

- **Không retry lỗi của chính mình** (400/validation). Retry một request sai sẽ sai y hệt lần nữa.
  Test `nonRetryableErrorsStopImmediately`.

- Tách **quyết định** (chờ bao lâu) khỏi **tác động** (sleep) → logic retry test được trong vài
  micro-giây thay vì phải chờ thật.

### 2. `KafkaSimulator.java` — partition, ordering, consumer group, DLQ

**Tái tạo trung thực:** partition theo hash của key, ordering trong partition, consumer group +
assignment, offset commit, rebalance, at-least-once.

**KHÔNG tái tạo** (ghi rõ để không ảo tưởng): độ trễ mạng và backpressure thật, replication/ISR,
log compaction, Kafka transaction (exactly-once), rebalance protocol thực tế (cooperative sticky).

| Test | Bài học |
|---|---|
| `sameKeyGoesToSamePartitionInOrder` | Kafka **chỉ** đảm bảo thứ tự **trong một partition** |
| `negativeHashCodeIsHandled` | `hashCode()` có thể **âm** → `-7 % 3 = -1` → IndexOutOfBounds. Phải dùng `Math.floorMod`. Bug thật khi tự viết partitioner |
| `withoutKeyOrderingIsLost` | Không key → round-robin → **mất** đảm bảo thứ tự |
| `lowCardinalityKeyCausesHotPartition` | Key = `tenantId` → 1 tenant lớn ôm hết 1 partition. Sửa: key = `(tenantId, sessionId)` → rải đều **mà vẫn** giữ thứ tự trong session. **Đúng thiết kế trong bài design TrueTest** |
| `partitionsAreExclusivelyAssigned` | 1 partition ↔ 1 consumer. Consumer nhiều hơn partition = ngồi không |
| `uncommittedWorkIsRedelivered` | Xử lý xong mà **chưa commit** rồi chết → giao **lại** → mỗi message xử lý **2 lần**. Đây là at-least-once |
| `idempotencyMakesRedeliveryHarmless` | Cùng dữ liệu, nhưng side effect chỉ **1 lần**. Đây là lý do bắt buộc phải có idempotent consumer |
| `rebalanceReassignsPartitions...` | Đọc tiếp từ **offset đã commit**, không phải từ chỗ đã xử lý |
| `poisonMessageGoesToDlq...` | Message sau poison **vẫn** được xử lý |
| `withoutDlqTheConsumerLoopsForever` | Không DLQ → offset **không bao giờ** tiến → cả partition đứng. Sự cố **im lặng**: không crash, chỉ là lag tăng dần |

## Nối với các module khác

- **Idempotency store + TTL** (và bẫy TTL < retry window): [module 08](../08-system-design/)
- **Circuit breaker** 3 pha: [module 08](../08-system-design/)
- **`INSERT ... ON CONFLICT DO NOTHING`** — idempotent consumer trong DB thật: [module 05](../05-postgres-depth/)
- **`SKIP LOCKED`** queue giao việc cho worker: [module 05](../05-postgres-depth/)

## Khi nào cần broker thật

Phần **bắt buộc** cần Kafka thật là quan sát rebalance protocol thực tế và backpressure. Lúc đó bật
Docker Desktop tạm rồi xoá lại, hoặc dùng `KafkaClusterTestKit` (embedded broker trong JVM).

## Checklist

- [ ] Giải thích thundering herd và vì sao jitter chữa được nó
- [ ] Nói được `cap` để làm gì và điều gì xảy ra nếu thiếu
- [ ] Phân biệt lỗi nên retry và lỗi không nên retry
- [ ] Giải thích ordering của Kafka nằm ở đâu, và key ảnh hưởng thế nào
- [ ] Giải thích hot partition + cách chọn key để rải đều mà vẫn giữ thứ tự
- [ ] Kể được chuỗi: at-least-once → duplicate → idempotent consumer
- [ ] Giải thích poison message làm kẹt partition, và DLQ chữa thế nào
