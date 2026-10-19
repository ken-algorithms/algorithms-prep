# Lab ôn bài giai đoạn 1: một project, 79 test, Postgres và Redis thật

Code cho các bài [01–09](../README.md). Mỗi package là một bài; mỗi test in một dòng `[LAB]` là con số được trích
trong bài.

```bash
mvn -q test                                    # 79 test, ~30 giây
mvn -q test -Dtest='Ledger*Test,BalanceReadTest,AccrualRaceTest'   # chỉ phần sổ cái + tính lãi
mvn test | grep '\[LAB\]'                      # chỉ xem số liệu
```

**Cần gì:** JDK 21+, Maven 3.9+. **Không cần Docker.**

- **PostgreSQL 16.4** chạy embedded (zonky `embedded-postgres`, binary tải qua Maven, gồm cả bản Apple Silicon). Mỗi
  test class xin một database riêng.
- **Redis**: test tự bật `redis-server` từ PATH (`/usr/bin`, `/usr/local/bin`, `/opt/homebrew/bin`) trên cổng ngẫu
  nhiên. Không có thì 4 test Redis tự **skip**, phần còn lại vẫn chạy. macOS: `brew install redis`.

## Bản đồ

| Bài | Package (`src/main/java/com/prep/onbai/…`) | Schema | Test |
|---|---|---|---|
| 01 | `pool/`: `LittlesLaw`, `QueueSimulator`, `NestedTransactions` | — | `PoolSizingTest` |
| 02 | `idempotency/`: Spring Boot app, `IdempotencyStore`, `PaymentService`, `FakePsp` | `idempotency.sql` | `IdempotencyApiTest` (MockMvc) |
| 03 | `ratelimit/`: `TokenBucket`, `WindowCounters`, `RedisRateLimiters` + `token_bucket.lua` | — | `RateLimiterTest` |
| 04 | `pgconcurrency/`: `Tx`; `jpa/` (Spring Data JPA, Hibernate 6.5) | `pgconcurrency.sql` | `IsolationAnomaliesTest`, `JpaPitfallsTest` |
| 05 | `ledger/`: `LedgerService`, `BalanceQueries`, `ReadYourWrites`, `SnapshotBalances`, `HotAccountBalances`, `LedgerReconciler` | `ledger.sql` | `LedgerTest`, `LedgerConcurrencyTest`, `BalanceReadTest` |
| 06 | `accrual/`: `LegacyAccrualJob`, `RcTransactionAccrual`, `LockedAccrualService`, `SkipLockedAccrualBatch`, `EffectiveDateAccrual`, `AccrualAudit` | `ledger.sql` + `accrual.sql` | `AccrualRaceTest` |
| 07 | `shortener/`: `Snowflake`, `Base62`, `UrlShortener` | `shortener.sql` | `IdAndShortenerTest` |
| 08 | `cache/`: `SingleFlight`, `CacheAside`, `ProductStore` + `put_if_newer.lua` | `cache.sql` | `CacheTest` |
| 09 | `outbox/`: `OrderService`, `OutboxRelay`, `IdCursorRelay`, `NotificationConsumer`, `Backoff` | `outbox.sql` | `OutboxTest` |

Hỗ trợ test: `testsupport/Pg` (Postgres embedded, database mới cho mỗi lớp), `testsupport/Locks` (chờ tới khi thấy
session đang chờ khoá trong `pg_stat_activity`), `testsupport/RedisServer`.

## Cách test race ở đây (dùng lại được cho dự án thật)

1. **Seam tất định**: code nhận một `Runnable` chạy đúng giữa "đọc" và "ghi" (`gapAfterRead`, `afterDbRead`,
   `afterPublish`). Test chèn giao dịch thứ hai vào đó, nên interleaving lặp lại được 100%, không phải mong trúng.
2. **Hai connection, hai luồng, chọn thứ tự từng câu lệnh** (`Tx`). Lệnh có thể bị chặn thì chạy `async`.
3. **Bằng chứng đang chờ khoá**: `Locks.awaitWaiters` hỏi `pg_stat_activity` trên connection riêng, không `sleep` đoán.
4. **Tranh chấp thật**: `CyclicBarrier` + N vòng, assert bất biến (tổng tiền, không âm, đối soát sạch), không assert
   thời gian.

## Kết quả

Lần chạy gần nhất: [results/test-output.txt](results/test-output.txt). JDK 21.0.11, PostgreSQL 16.4 embedded, Redis
7.0.15, container Linux 4 vCPU. Các dòng có tranh chấp thật (ví dụ "bản READ COMMITTED tính trên số dư cũ 19/20")
dao động nhẹ giữa các lần chạy; test không assert vào chúng.
