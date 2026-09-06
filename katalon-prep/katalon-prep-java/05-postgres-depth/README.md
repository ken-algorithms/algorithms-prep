# Module 05 — PostgreSQL in-depth (KHÔNG cần Docker)

```bash
mvn -pl 05-postgres-depth test    # 19 test pass, ~6 giây
```

## Không Docker, không brew install

Module này dùng **[zonky embedded-postgres](https://github.com/zonkyio/embedded-postgres)**: binary
**PostgreSQL 16.4 thật** được đóng gói trong artifact Maven, giải nén vào temp dir, chạy như
subprocess, và bị dừng khi JVM thoát. Sau khi test xong **không còn gì trên máy**.

Đã verify trên máy bạn:

```text
>>> PostgreSQL 16.4 on x86_64-apple-darwin20.6.0
```

PostgreSQL **16** vì Katalon dùng **AWS Aurora PostgreSQL** (JD Senior Database Engineer ghi rõ:
*"Architect, provision, and manage PostgreSQL databases on AWS Aurora"*, kèm extension
`pgvector`, `TimescaleDB`, `PostGIS`).

Postgres khởi động **một lần** cho cả suite (~10s) qua [`Pg.java`](src/test/java/com/prep/postgres/Pg.java);
mỗi lab dùng schema riêng nên không dính nhau.

---

## Điều làm module này khác các tutorial Postgres

**Test assert trực tiếp trên query plan.** Không phải "tôi biết đọc `EXPLAIN`" mà là test **chứng
minh** index đã đổi plan từ `Seq Scan` sang `Index Scan`:

```java
String plan = Pg.explain("SELECT * FROM lab01.test_results WHERE run_id = 42");
assertTrue(Pg.usesSeqScan(plan));          // trước khi có index
// ... CREATE INDEX ...
assertTrue(Pg.usesIndexOnlyScan(plan));    // sau khi có covering index
```

`Pg.java` có helper: `usesSeqScan`, `usesIndex`, `usesIndexOnlyScan`, `indexesUsed`, `actualRows`,
`tableSizeBytes`, `indexSizeBytes`, `humanSize`.

---

## Lab 01 — Index ([Lab01IndexesTest](src/test/java/com/prep/postgres/Lab01IndexesTest.java))

300k dòng `test_results`, phân bố lệch giống production (94% PASSED / 5% FAILED / 1% SKIPPED).

### Số đo thật

```text
[1] không index:  Parallel Seq Scan, Buffers: shared hit=4286, actual time=11.934 ms
                  Rows Removed by Filter: 99980
[2] có B-tree:    Index Scan using idx_results_run, Buffers: shared hit=63 read=2, actual time=0.117 ms
```

→ **nhanh 100x, ít buffer 68x**. Đây là con số nên mang vào phỏng vấn.

```text
[4] index size:  full=2056 kB   partial=120 kB   (nhỏ hơn 17.1x)
[6] created_at:  btree=2168 kB  brin=24 kB       (BRIN nhỏ hơn 90x)
```

### Nội dung

| Test | Bài học |
|---|---|
| 1–2 | Seq Scan → Index Scan; đọc `Buffers` và `Rows Removed by Filter` |
| 3 | **Leftmost prefix rule**: index `(run_id, status)` phục vụ được query theo `run_id`, **không** phục vụ query chỉ có `status`. Lỗi thiết kế index phổ biến nhất |
| 4 | **Partial index** `WHERE status='FAILED'` → nhỏ hơn **17x**, vẫn được dùng |
| 5 | **Covering index** (`INCLUDE`) → `Index Only Scan`. **Phải `VACUUM`** trước, không thì visibility map chưa cập nhật và Postgres vẫn phải chạm heap |
| 6 | **BRIN** cho cột thời gian append-only → nhỏ hơn **90x**. Trade-off: chỉ hiệu quả khi dữ liệu **sắp xếp vật lý** theo cột đó |
| 7 | **GIN** cho `jsonb` |
| 8 | `rows=` là **ước lượng**. Thêm 100k dòng mà không `ANALYZE` → statistics lạc hậu → planner chọn sai. **Việc đầu tiên khi tuning không phải thêm index, mà là kiểm tra statistics** |

---

## Lab 02 — Partitioning ([Lab02PartitioningTest](src/test/java/com/prep/postgres/Lab02PartitioningTest.java))

4 partition theo tháng, 50k dòng mỗi cái.

### Số đo thật

```text
DELETE 50k dòng:      12 ms, dead_tup=50000, size 2944 kB -> 2944 kB (KHÔNG giảm)
DROP PARTITION 50k:   14 ms, không dead tuple, disk trả lại NGAY
```

Đây là **lý do thật sự** để partition: retention policy trở thành một lệnh DDL thay vì một job
`DELETE` chạy hàng giờ, sinh bloat, và làm autovacuum bão.

### Nội dung

| Test | Bài học |
|---|---|
| 1 | `PRIMARY KEY` của bảng partitioned **phải chứa** partition key — giới hạn thật, dễ quên khi migrate |
| 2 | **Partition pruning**: filter theo tháng → chỉ quét 1 partition |
| 3 | **Bẫy**: query **không** có partition key → quét **cả 4** partition. Partition key phải chọn theo **cách query**, không theo cách dữ liệu đến |
| 4 | `DELETE` vs `DROP PARTITION` — bảng số ở trên |
| 5 | **Bẫy default partition**: nó "sửa" lỗi insert ngoài range, nhưng sau đó **không thể** tạo partition mới cho range đã có dữ liệu trong default mà không khóa bảng. Cách đúng: job tự tạo partition trước hạn (pg_partman), default chỉ để bắt lỗi + cảnh báo |

---

## Lab 03 — MVCC / bloat / locking ([Lab03MvccLockingTest](src/test/java/com/prep/postgres/Lab03MvccLockingTest.java))

### Số đo thật — đây là câu hỏi lọc rất tốt

```text
[1] UPDATE 100k dòng:  size 24 MB -> 49 MB (+100%), dead_tup=100000
[2] VACUUM:            size 49 MB -> 49 MB (KHÔNG giảm), dead_tup=0
[3] VACUUM FULL:       size 49 MB -> 24 MB
```

**`UPDATE` không sửa dòng tại chỗ.** Postgres ghi một phiên bản **mới** và đánh dấu phiên bản cũ là
dead (để transaction đang chạy vẫn đọc được bản cũ — đó là MVCC). Kết quả: bảng **gần gấp đôi** dù
số dòng logic không đổi.

**`VACUUM` không trả disk lại cho OS** — nó chỉ đánh dấu không gian để **tái sử dụng** bên trong bảng.
Đây chính là câu trả lời cho *"tôi DELETE cả triệu dòng mà disk không giảm"*.

**`VACUUM FULL` trả disk** nhưng lấy `ACCESS EXCLUSIVE LOCK` → mọi query bị chặn. Với bảng 500GB là
nhiều giờ downtime. Nên câu trả lời **đúng** ở production không phải `VACUUM FULL`, mà là **pg_repack**
(online) hoặc **thiết kế partition từ đầu** (Lab 02).

### `FOR UPDATE SKIP LOCKED` — queue giao test cho worker

Cực kỳ on-domain với Katalon (distributed test execution).

```text
=== w1 claimed: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
=== w2 claimed: [11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
=== [không SKIP LOCKED] worker2 bị chặn: ERROR: canceling statement due to lock timeout
```

Hai worker chạy **đồng thời** nhận job **không trùng nhau**, không ai phải chờ. Không có `SKIP LOCKED`
thì worker thứ hai **bị chặn** (chứng minh bằng `lock_timeout = 500ms`).

> **Bài học:** một queue viết bằng `FOR UPDATE` thường sẽ biến N worker thành **1 worker** — tất cả
> xếp hàng trên cùng những dòng đầu tiên. Throughput không tăng dù bạn scale worker lên 50.
> `SKIP LOCKED` là thứ biến nó thành queue thật sự song song.

---

## Câu hỏi phỏng vấn module này chuẩn bị cho bạn

- *"Query này chậm, bạn làm gì?"* → kiểm tra statistics trước (Lab 01 test 8), rồi `EXPLAIN (ANALYZE, BUFFERS)`, so `rows=` ước lượng với actual, xem `Rows Removed by Filter`
- *"Khi nào partition?"* → khi có retention policy, hoặc bảng quá lớn để `VACUUM`/reindex. Và partition key theo **cách query**
- *"Vì sao `DELETE` nhiều không giải phóng disk?"* → MVCC + `VACUUM` chỉ tái sử dụng (Lab 03)
- *"Index nào cho cột thời gian?"* → BRIN nếu append-only (nhỏ hơn 90x), B-tree nếu cần range query chính xác
- *"`SKIP LOCKED` dùng làm gì?"* → queue worker song song
- *"Composite index `(a,b)` có phục vụ query theo `b` không?"* → **không** (leftmost prefix)

## Checklist

- [ ] Đọc được `EXPLAIN (ANALYZE, BUFFERS)`: phân biệt `rows=` ước lượng vs `actual rows`
- [ ] Giải thích leftmost prefix rule
- [ ] Biết khi nào dùng partial / covering / BRIN / GIN, và trade-off từng cái
- [ ] Giải thích MVCC → vì sao `UPDATE` làm bảng phình
- [ ] Nói được `VACUUM` vs `VACUUM FULL` vs `pg_repack` vs partition
- [ ] Giải thích `SKIP LOCKED` và vì sao `FOR UPDATE` thuần làm queue mất tính song song
