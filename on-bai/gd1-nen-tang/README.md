# Ôn bài · Giai đoạn 1 — Nền tảng: lý thuyết đi cùng lab chạy thật

> Bộ bài ôn cho vòng **system design + code tại chỗ** của **Katalon** và **NAB**, ở mức senior Java. Mỗi bài đi
> theo một đường: trọng tâm phỏng vấn → lý thuyết vừa đủ → **real case dựng từ đầu** (code thật, giải thích từng
> đoạn) → lab có số liệu → vẽ design → drill code 10–20 phút → follow-up → checklist.
>
> Toàn bộ code nằm trong **một** project Maven: [`lab/`](lab/README.md). **79 test, 0 lỗi**, Postgres 16 thật
> (embedded, không cần Docker), Redis thật nếu có `redis-server`, chạy hết trong khoảng 30 giây. Output thật ở
> [`lab/results/test-output.txt`](lab/results/test-output.txt).

```bash
cd on-bai/gd1-nen-tang/lab && mvn -q test
```

---

## Danh sách bài

| # | Bài | Trọng tâm phải nói được | Katalon | NAB | Lab (test) |
|:-:|---|---|:-:|:-:|---|
| 00 | [Dẫn 45 phút: khung, ước lượng, vẽ, code tại chỗ](00-cach-dan-45-phut.md) | Ước lượng để ra **một** quyết định; ba đường ghi/đọc/đối soát; 9 đoạn code lõi | ★★★ | ★★★ | — |
| 01 | [Little's Law, pool, timeout, backpressure](01-littles-law-pool-timeout.md) | Trần = pool / thời gian giữ; p99 nổ khi ρ → 1; `REQUIRES_NEW` tự deadlock pool | ★★ | ★★ | `PoolSizingTest` (5) |
| 02 | [Idempotency key cho API có tiền](02-idempotency-key.md) | Hai pattern; ba loại lỗi; lease + fencing token | ★★★ | ★★★ | `IdempotencyApiTest` (7) |
| 03 | [Rate limiter: token bucket, cửa sổ, Redis Lua](03-rate-limiter.md) | Lỗ hổng biên; race `GET`/`SET`; Lua nguyên tử; fail-open/closed | ★★★ | ★★ | `RateLimiterTest` (6) |
| 04 | [Postgres concurrency + bẫy Spring/JPA](04-postgres-concurrency.md) | Bốn cách chống lost update; write skew; SSI cần **mọi** bên; `FOR NO KEY UPDATE` | ★★★ | ★★★ | `IsolationAnomaliesTest` (10), `JpaPitfallsTest` (5) |
| 05 | [Sổ cái kép và số dư real-time: ví từ đầu](05-ledger-va-balance.md) ⭐ | Một câu `UPDATE` khoá + kiểm + cấp seq; snapshot theo seq; account nóng theo mốc xmin | ★★★ | ★★★ | `LedgerTest` (11), `LedgerConcurrencyTest` (3), `BalanceReadTest` (5) |
| 06 | [Race tính lãi với thanh toán (bài thật)](06-race-tinh-lai-vs-thanh-toan.md) ⭐ | Bốn cách trực giác sai; khoá theo id + `SKIP LOCKED`; đổi định nghĩa theo ngày nghiệp vụ | ★★★ | ★★★ | `AccrualRaceTest` (10) |
| 07 | [ID generator và URL shortener](07-id-generator-url-shortener.md) | Snowflake và ba điều kiện; hoán vị thay vì hash; alias chiếm mã | ★★ | ★★ | `IdAndShortenerTest` (7) |
| 08 | [Cache nhất quán: stampede, race xoá, tombstone](08-cache-consistency.md) | Single-flight; reader chậm ghi giá cũ; version + tombstone | ★★ | ★★ | `CacheTest` (3) |
| 09 | [Outbox và notification](09-outbox-notification.md) | Dual write; `SKIP LOCKED`; inbox; con trỏ id sót commit muộn | ★★ | ★★★ | `OutboxTest` (7) |
| 10 | [Tổng ôn: ba đề mock, đào sâu, tự chấm](10-tong-on-mock-interview.md) | Bấm giờ 45 phút; 20 câu đào sâu ledger; 25 câu nhanh | ★★★ | ★★★ | — |

★★★ gần như chắc chắn bị hỏi · ★★ hay gặp. Bài 05 và 06 dùng chung cho cả hai công ty: chúng là câu chuyện mạnh
nhất của bạn (sổ cái ngân hàng thật, [doc 06](../../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md)).

---

## Thứ tự học

```text
00 (đọc nhanh) → 04 → 05 → 06 → 02 → 01 → 03 → 09 → 08 → 07 → 10 → 00 (đọc lại)
```

- **04 trước 05, 06:** mọi lập luận về khoá và isolation ở bài sổ cái dựa vào bài 04.
- **05, 06 sớm:** vũ khí chính cho NAB, và câu chuyện họ G cho Katalon.
- Theo **tuần** của [lộ trình giai đoạn 1](../../java-system-design/10-implement-gd1-nen-tang.md): xem bảng ở
  [bài 00 §7](00-cach-dan-45-phut.md#7-lịch-ôn-đề-xuất-khớp-tuần-14-của-lộ-trình).

---

## Bộ bài này thêm gì so với tài liệu đã có

Phần lớn chủ đề đã có ở lộ trình, ở `katalon-prep/` và `nab-prep/`. Bộ ôn bài **không chép lại**; nó thêm ba thứ:

1. **Code chạy được và số đo cho từng khẳng định.** Ví dụ "khoá theo id thì không deadlock" có test `[commit, 40P01]`
   so với ~1.900 chuyển khoản không deadlock; "single-flight" có 100 so với 1 lần đọc DB.
2. **Những điều chỉ chạy thật mới thấy**, đáng nói trong phòng phỏng vấn:
   - SSI của Postgres chỉ bắt write skew khi **cả hai** bên `SERIALIZABLE` ([04](04-postgres-concurrency.md#bước-6--write-skew-quy-tắc-nằm-trên-nhiều-dòng)).
   - Hibernate 6 sinh `FOR NO KEY UPDATE` cho `PESSIMISTIC_WRITE` ([04](04-postgres-concurrency.md#bước-9--lockpessimistic_write-thật-ra-sinh-ra-gì)).
   - Test `isIn(5,00; 3,50)` của doc 06 **cũng pass với bản lỗi**; cần kiểm theo định nghĩa ([06](06-race-tinh-lai-vs-thanh-toan.md#bước-4--bản-vá-cho-hệ-thống-đang-chạy-idempotency--từng-khoản-vay--khoá-theo-id)).
   - Mốc snapshot theo `posting.id` **sót** posting của transaction commit muộn khi không có khoá; đính chính doc 06
     §7.2, kèm cách đúng bằng mốc xmin ([05](05-ledger-va-balance.md#bước-10--tài-khoản-nóng-không-khoá-chốt-theo-mốc-xmin)).
   - `pg_stat_activity` bị chụp một lần cho cả transaction; poll phải dùng connection riêng.
3. **Drill code tại chỗ** có tiêu chí chấm, cho đúng phần interviewer hay bắt viết.

---

## Liên quan

- Lộ trình: [giai đoạn 1](../../java-system-design/10-implement-gd1-nen-tang.md) · [perf-lab](../../java-system-design/perf-lab/README.md)
  (đo thread thật) · [dist-lab](../../java-system-design/dist-lab/README.md) (Kafka thật, giai đoạn 2)
- Katalon: [7 họ bài + khung 45 phút](../../katalon-prep/katalon-system-design/README.md) · [đếm sự kiện 10k](../../katalon-prep/katalon-system-design/04-event-counting-10k.md)
- NAB: [README](../../nab-prep/README.md) · [gap analysis](../../nab-prep/02-gap-analysis.md) · [vòng tiếng Anh](../../nab-prep/05-english-interview.md)
