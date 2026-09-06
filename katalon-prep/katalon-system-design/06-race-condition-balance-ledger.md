# 06 — Race condition khi tính balance trên ledger (bài từ hệ thống thật)

> **Nguồn:** lỗi thật, lặp lại, trong `all-in-one-v2` (`commercial-svc-account` +
> `commercial-svc-batch`). Khách thanh toán nhiều lần trong một phút, đồng thời batch job tính lãi
> chạy trên chính account đó → số dư dùng để tính lãi khác số dư tại thời điểm ghi sổ → tính sai.
>
> Họ bài: **G — quản lý đồng thời trên trạng thái chia sẻ** *(consistency & concurrency control)*.
> Chữ ký nhận dạng: *đọc → tính → ghi, và giữa "đọc" với "ghi" có người khác chen vào.*
> Bản đồ 7 họ: [README](README.md#2-bảy-họ-bài--chữ-ký-nhận-dạng-lõi-giải-pháp-bẫy-kinh-điển) ·
> Bằng chứng hạ tầng: [05](05-he-thong-that-allinone-aws.md)
>
> **Mọi trích dẫn code trong file này đều đã đọc trực tiếp từ repo, có file:line.** Phần suy luận
> được đánh dấu riêng ở [§11](#11-ranh-giới-trung-thực).

---

## Mục lục

- [1. Triệu chứng và tên gọi đúng của lỗi](#1-triệu-chứng-và-tên-gọi-đúng-của-lỗi)
- [2. Bằng chứng trong code thật — 7 phát hiện](#2-bằng-chứng-trong-code-thật--7-phát-hiện)
- [3. Dựng lại đúng interleaving gây sai](#3-dựng-lại-đúng-interleaving-gây-sai)
- [4. Bốn "giải pháp trực giác" đều không đủ](#4-bốn-giải-pháp-trực-giác-đều-không-đủ)
- [5. Thang giải pháp — 5 bậc](#5-thang-giải-pháp--5-bậc)
- [6. Khuyến nghị cụ thể cho all-in-one-v2](#6-khuyến-nghị-cụ-thể-cho-all-in-one-v2)
- [7. Balance real-time đúng khi event dồn dập](#7-balance-real-time-đúng-khi-event-dồn-dập)
- [8. Batch và online không đánh nhau](#8-batch-và-online-không-đánh-nhau)
- [9. Chứng minh đã sửa — 3 loại test](#9-chứng-minh-đã-sửa--3-loại-test)
- [10. Câu trả lời phỏng vấn + 12 follow-up](#10-câu-trả-lời-phỏng-vấn--12-follow-up)

**Phần II — thiết kế lại từ đầu** *(đọc sau khi đã hiểu §1-§7)*

- [12. Nếu được thiết kế lại từ đầu — bản triệt để](#12-nếu-được-thiết-kế-lại-từ-đầu--bản-triệt-để)
  - [12.1 Một thay đổi ĐỊNH NGHĨA xoá được phần lớn bài toán](#121-một-thay-đổi-định-nghĩa-xoá-được-phần-lớn-bài-toán) ⭐
  - [12.2 Năm quyết định nền](#122-năm-quyết-định-nền)
  - [12.3 Mô hình dữ liệu](#123-mô-hình-dữ-liệu)
  - [12.4 Đường ghi: single-writer theo account](#124-đường-ghi-single-writer-theo-account)
  - [12.5 Batch tính lãi thiết kế thế nào](#125-batch-tính-lãi-thiết-kế-thế-nào) ⭐
  - [12.6 Scale khi transaction và posting tăng không giới hạn](#126-scale-khi-transaction-và-posting-tăng-không-giới-hạn)
- [13. Kiến trúc AWS — khai báo từng bước](#13-kiến-trúc-aws--khai-báo-từng-bước)
- [14. Lộ trình chuyển đổi — không big-bang](#14-lộ-trình-chuyển-đổi--không-big-bang)
- [15. Khi nào **không** nên làm bản này](#15-khi-nào-không-nên-làm-bản-này)
- [11. Ranh giới trung thực](#11-ranh-giới-trung-thực) *(bao trùm cả hai phần)*

---

## 1. Triệu chứng và tên gọi đúng của lỗi

**Triệu chứng:** khách thanh toán lúc 10:00:05. Job tính lãi đọc số dư lúc 10:00:00, ghi bút toán
lãi lúc 10:00:12. Lãi được tính trên số dư **trước khi trả tiền** nhưng lại được ghi sổ **sau khi
trả tiền** — sổ sách nhìn vào thì bút toán lãi và khoản thanh toán cùng nằm trong một ngày, mà con
số lại không khớp nhau.

**Gọi tên cho đúng — vì gọi sai thì chữa sai:**

| Tên | Là gì | Có phải lỗi này không |
|---|---|---|
| **Lost update** | Hai transaction cùng `UPDATE` một dòng, cái sau đè cái trước | ❌ Không — ở đây hai bên ghi **hai dòng khác nhau** |
| **Dirty read** | Đọc dữ liệu chưa commit | ❌ Không — Postgres `READ COMMITTED` đã chặn |
| **Non-repeatable read** | Đọc hai lần trong cùng transaction, ra hai kết quả | ❌ Không — không có transaction bao ngoài để mà "non-repeatable" |
| **TOCTOU** *(time-of-check to time-of-use)* | Kiểm tra/đọc ở thời điểm A, hành động dựa trên kết quả đó ở thời điểm B, giữa A và B trạng thái đã đổi | ✅ **Đúng cái này** |
| **Write skew** | Hai transaction đọc chồng lấn, mỗi bên ghi chỗ khác nhau, kết quả vi phạm một bất biến chung | ✅ Đúng luôn — bất biến bị vi phạm là *"lãi phải tính trên số dư tại thời điểm ghi sổ"* |

**Vì sao lỗi này khó chịu hơn lost update:**

```text
Lost update  → có XUNG ĐỘT rõ ràng trên một dòng
               → DB phát hiện được, optimistic lock ném exception, ta biết mà retry

TOCTOU/write skew ở đây
               → KHÔNG có xung đột nào. Hai bên INSERT hai posting khác nhau.
               → DB thấy hoàn toàn hợp lệ. Không exception. Không log lỗi.
               → Ghi "thành công", và SAI TRONG IM LẶNG.
```

Đó là lý do nó lọt tới production và chỉ lộ ra khi đối soát sổ sách. **Không có lỗi nào để bắt** —
phải thiết kế để nó không xảy ra được, chứ không thể try/catch.

---

## 2. Bằng chứng trong code thật — 7 phát hiện

Tất cả đã grep/đọc trực tiếp trên `/Users/duc.nguyen/data/projects/zaitenllc/all-in-one-v2`
(7.615 file `.java`, không tính `target/`).

| # | Phát hiện | Bằng chứng | Mức |
|---|---|---|:---:|
| 1 | **Không có bất kỳ cơ chế khoá nào ở tầng JPA** | `@Version`: **0** kết quả · `LockModeType` / `PESSIMISTIC` / `OPTIMISTIC`: **0** · `SELECT ... FOR UPDATE` / `SKIP LOCKED`: **0** (chỉ khớp comment tiếng Anh, không phải SQL) | 🔴 |
| 2 | **562 `@Transactional`, 0 chỗ khai báo isolation** | `grep 'Isolation\.'` → **0**. Tất cả chạy mặc định = `READ COMMITTED` trên Postgres | 🔴 |
| 3 | **Đường tính lãi không nằm trong transaction nào** | `RunLoanAccountDailyAccrualConsumerService.execute()` (dòng 67) **không có** `@Transactional`; caller `AccrualDailyLoanConsumerStrategy.execute()` cũng không | 🔴 |
| 4 | **Khoảng hở đọc→ghi trải dài cả batch** | Đọc số dư cho **cả lô** ở dòng 171-173 (`getCurrentBalanceByListIds`), ghi transaction ở dòng 113 (`saveAll`), cập nhật tiếp ở dòng 119-121 — ba bước, ba transaction khác nhau | 🔴 |
| 5 | **Có `synchronized` — nhưng khoá nhầm thứ và nhầm phạm vi** | Cùng file: `private final Object lock = new Object();` và 3 khối `synchronized (lock)` ở dòng 137/143/149 — chỉ bọc `SimpleDateFormat.parse()` và `new Date()`. Mà formatter **đã** là `ThreadLocal` nên vốn đã an toàn. Và lock là **JVM-local**, vô nghĩa khi chạy ≥2 instance ECS ([05](05-he-thong-that-allinone-aws.md)) | 🔴 |
| 6 | **Hai nguồn sự thật cho cùng một con số** | `LoanAccount.currentBalance` là **cột persist** (`LoanAccount.java:104`, nằm ngoài khối `@Transient` bắt đầu ở dòng 161) — trong khi số dư cũng derive được bằng `COALESCE(sum(p.amount), 0)` (`LoanAccountRepository.java:553-569`) | 🔴 |
| 7 | **Posting có setter cộng dồn** | `Posting.addToAmount(BigDecimal)` — sửa tại chỗ số tiền của một bút toán đã ghi. Vi phạm tính bất biến của sổ cái | 🟡 |

**Một điểm đang làm ĐÚNG, phải ghi nhận** (và là nền để sửa phần còn lại):

- Mô hình đã là **double-entry ledger** chuẩn: `Transaction → JournalEntry → Posting[] → LedgerAccount`.
- `JournalEntry.buildRevertedPostings()` đảo bút toán bằng cách **tạo posting âm**
  (`p.getAmount().negate()`), **không** sửa/xoá bút toán cũ. Đây đúng là cách sổ cái phải làm.
- Số dư **derive được** từ `SUM(posting.amount)` — nghĩa là đã có sẵn nguồn sự thật đáng tin.

→ **Kiến trúc nền đúng. Lỗi nằm ở tầng thực thi: thiếu ranh giới transaction và thiếu khoá.** Đây là
tin tốt: không phải viết lại hệ thống.

---

### Trích nguyên văn hai chỗ quan trọng nhất

**(a) Truy vấn số dư** — `LoanAccountRepository.java:553-569`:

```sql
select
  COALESCE(sum(p.amount), 0) as balance,
  loan.id as loanAccountId
  from transaction t
  left join journal_entry j on j.transaction_id = t.id
  left join posting p       on p.journal_entry_id = j.id
  left join ledger_account l on l.id = p.ledger_account_id
  left join loan_account loan on t.loan_account_id = loan.id
  where t.loan_account_id in (:loanAccountIds)
    and j.entry_date < :balanceDate          -- <<< số dư TẠI MỘT THỜI ĐIỂM
    and t.type <> 0
    and l.type = 'PRINCIPAL' and l.sub_type = 'LOAN_PRINCIPAL'
  group by loan.id
```

Truy vấn này **tự nó đúng** — nó trả số dư tính tới `balanceDate`. Vấn đề là cái xảy ra **sau khi**
nó trả về.

**(b) Đường đi của job tính lãi** — `RunLoanAccountDailyAccrualConsumerService.java`:

```java
public void execute(final String referenceDate, final List<Long> loanAccountIds) {   // dòng 67
    // ...  KHÔNG có @Transactional ở đây, cũng không ở caller

    createTransactionAccrualDailyLoan(...);        // dòng 106 →
        //   dòng 171-173:  ĐỌC số dư cho CẢ LÔ
        //   final List<LoanAccountIdBalance> listLoanIdCurrentBalance =
        //       loanAccountMixService.getCurrentBalanceByListIds(keySet, endOfBusinessReferenceDate);
        //   dòng 175-189:  TÍNH lãi từ item.getBalance()

    transactionWriteService.saveAll(listTransactionResults);          // dòng 113  GHI (transaction khác)

    calculateAndUpdateInterestAccrualDailyLoan(...);                  // dòng 119-121  GHI tiếp (transaction khác nữa)
}
```

**Ba bước, ba transaction, không có gì giữ account đứng yên giữa chúng.** Và vì đọc theo **lô**,
khoảng hở giữa "đọc số dư của account X" và "ghi bút toán lãi cho account X" **kéo dài bằng thời
gian xử lý cả lô** — không phải vài mili-giây, mà có thể là giây tới phút tuỳ kích thước lô.

---

## 3. Dựng lại đúng interleaving gây sai

Số liệu ví dụ: dư nợ gốc 10.000, lãi suất ngày 0,05%.

```text
 THỜI GIAN   BATCH JOB (tính lãi)                 API THANH TOÁN (khách trả 3.000)
 ─────────────────────────────────────────────────────────────────────────────────
 10:00:00    BEGIN tx#1
             SELECT SUM(p.amount) → 10.000
             COMMIT tx#1                          ← số dư đã "rời khỏi" transaction
                     │
                     │  ← KHOẢNG HỞ: không ai giữ account này
                     │
 10:00:05            │                            BEGIN tx#2
                     │                            INSERT journal_entry + posting(-3.000)
                     │                            COMMIT tx#2      ← số dư thật giờ là 7.000
                     │
 10:00:12    lãi = 10.000 × 0,05% = 5,00          (đã xong)
             BEGIN tx#3
             INSERT posting(lãi = 5,00)           ← ghi bằng số dư CŨ
             UPDATE loan_account SET current_balance = 10.000   ← ĐÈ NGƯỢC số dư đã đúng!
             COMMIT tx#3
 ─────────────────────────────────────────────────────────────────────────────────
 KẾT QUẢ:  lãi đúng phải là 7.000 × 0,05% = 3,50   →  sai 1,50 (+42,9%)
           và current_balance = 10.000  trong khi SUM(posting) = 7.000 + lãi
           → HAI nguồn sự thật lệch nhau, không có gì báo lỗi.
```

**Hai lỗi riêng biệt, chồng lên nhau** — cần thấy rõ vì cách chữa khác nhau:

| | Lỗi | Bản chất | Chữa bằng |
|---|---|---|---|
| **A** | Lãi tính trên số dư cũ | TOCTOU: đọc ở T1, dùng ở T2 | Ranh giới transaction + khoá ([§5](#5-thang-giải-pháp--5-bậc)) |
| **B** | `current_balance` bị đè ngược | Lost update kinh điển trên một cột | **Bỏ hẳn cột đó** hoặc coi nó là cache có version ([§6](#6-khuyến-nghị-cụ-thể-cho-all-in-one-v2)) |

Lỗi B tệ hơn lỗi A: lỗi A làm sai một con số của một ngày; lỗi B làm **hỏng trạng thái**, và mọi
thứ đọc từ `current_balance` sau đó đều sai cho tới khi có ai đó đối soát.

---

## 4. Bốn "giải pháp trực giác" đều không đủ

Đây là phần nên nói ra trong phỏng vấn — nó cho thấy bạn đã loại trừ, không phải đoán trúng.

### 4.1 `synchronized` trong Java — **sai phạm vi**

```java
private final Object lock = new Object();      // đang có thật trong code, dòng 61
synchronized (lock) { ... }
```

Lock này nằm trong **một JVM**. Hệ thống chạy ECS Fargate với `desired_count ≥ 2`
([05](05-he-thong-that-allinone-aws.md)) — instance A khoá không ngăn được instance B. Và ngay cả
một instance, batch job với online API có thể là **hai service khác nhau**
(`commercial-svc-batch` vs `commercial-svc-account`), tức hai process khác nhau.

> **Bẫy phụ đáng nêu:** khối `synchronized` hiện có bọc `SimpleDateFormat.parse()`, mà formatter đó
> đã được bọc `ThreadLocal` ở dòng 51-52. Tức là **khoá thừa cho thứ đã an toàn, trong khi thứ thật
> sự cần khoá thì không có khoá nào**. Nhìn vào code sẽ tưởng "đã xử lý concurrency rồi".

### 4.2 Chỉ thêm `@Transactional` — **cần nhưng chưa đủ**

Gói cả `execute()` vào một transaction thì lỗi B (đè `current_balance`) đỡ hơn, nhưng lỗi A **vẫn
còn**, vì Postgres mặc định `READ COMMITTED`:

```text
READ COMMITTED: mỗi câu lệnh nhìn thấy snapshot MỚI NHẤT tại lúc câu lệnh đó chạy.
→ Đọc số dư ở đầu transaction, người khác commit, ta ghi ở cuối transaction
→ KHÔNG có xung đột nào bị phát hiện. Vẫn sai y như cũ.
```

`READ COMMITTED` chỉ chống dirty read. Nó **không** chống write skew.

### 4.3 Nâng lên `SERIALIZABLE` — **đúng nhưng đắt và chưa đủ một mình**

Postgres SSI *sẽ* phát hiện được write skew này và ném
`could not serialize access due to read/write dependencies` (SQLState `40001`).

Nhưng:

- Phải **retry** mọi transaction bị huỷ — mà retry một giao dịch tiền **bắt buộc phải idempotent**,
  nếu không thì ghi sổ hai lần. Hiện tại chưa có idempotency key.
- Batch chạy hàng nghìn account sẽ sinh rất nhiều xung đột giả với luồng online → tỉ lệ abort cao,
  batch chạy mãi không xong.
- Đổi isolation cho **562 chỗ** `@Transactional` là thay đổi rủi ro rất rộng.

→ Là công cụ đúng cho **một vài đường quan trọng**, không phải giải pháp toàn hệ thống.

### 4.4 Retry mù — **biến lỗi tính sai thành lỗi ghi trùng**

Thêm `@Retryable` mà không có idempotency thì mỗi lần retry lại `INSERT` thêm một bút toán lãi nữa.
Đổi một lỗi *khó thấy* lấy một lỗi *tệ hơn*: khách bị tính lãi nhiều lần.

> **Nguyên tắc:** trong hệ thống tiền, **idempotency là điều kiện tiên quyết của retry**, không phải
> tính năng thêm sau. Đây đúng là chuỗi lập luận at-least-once → idempotent ở
> [../katalon-prep-java/06-distributed-resilience/](../katalon-prep-java/06-distributed-resilience/).

---

## 5. Thang giải pháp — 5 bậc

Từ rẻ tới triệt để. Không phải chọn một — thực tế là **chồng nhiều bậc**.

### Bậc 0 — Gộp đọc và ghi vào **một** transaction *(điều kiện cần)*

```java
@Transactional                       // ← hiện đang KHÔNG có
public void accrueForAccount(Long loanAccountId, Date asOf) {
    BigDecimal balance = repo.getCurrentBalance(loanAccountId, asOf);
    BigDecimal interest = balance.multiply(dailyRate);
    postingRepo.save(new Posting(interest, journalEntry, incomeAccount));
}
```

Chưa đủ (xem [§4.2](#42-chỉ-thêm-transactional--cần-nhưng-chưa-đủ)) nhưng là **tiền đề bắt buộc**
cho mọi bậc sau. Kèm theo: **đổi từ đọc theo lô sang đọc theo từng account** — khoảng hở đọc→ghi co
từ "cả batch" xuống "một account".

### Bậc 1 — Optimistic locking

```java
@Entity
public class LoanAccount {
    @Version
    private Long version;            // Hibernate tự tăng, tự so khi UPDATE
}
```

Khi hai bên cùng chạm account, bên chậm hơn nhận `OptimisticLockException` → retry.

| Được | Mất |
|---|---|
| Rẻ nhất, không giữ khoá DB, không có nguy cơ deadlock | Chỉ bảo vệ khi **cả hai bên đều UPDATE cùng dòng `loan_account`** — nếu batch chỉ INSERT posting thì `version` không đổi và không bắt được gì |
| Phù hợp khi tranh chấp **thấp** | Tranh chấp cao → retry storm |

> ⚠️ **Đây là chỗ dễ trả lời sai trong phỏng vấn.** `@Version` **không** tự động cứu được write skew
> giữa "đọc SUM(posting)" và "INSERT posting mới". Nó chỉ hoạt động nếu ta **cố ý** chạm vào
> `loan_account` trong mọi đường ghi để version bump (một dạng "khoá vật chỉ điểm" — *materialized
> conflict*). Nói được điều này là hiểu bản chất, không phải thuộc tên annotation.

### Bậc 2 — Pessimistic lock theo từng account

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select la from LoanAccount la where la.id = :id")
Optional<LoanAccount> findByIdForUpdate(@Param("id") Long id);
```

sinh ra `SELECT ... FOR UPDATE`. Mọi đường ghi (thanh toán, tính lãi, phí trễ) **đều phải** lấy khoá
này trước → các thao tác trên **cùng một account** bị tuần tự hoá, còn account khác nhau vẫn song song.

Cho batch, thêm `SKIP LOCKED` để không phải xếp hàng chờ khách:

```sql
SELECT id FROM loan_account
 WHERE id = ANY(:ids)
   FOR UPDATE SKIP LOCKED      -- account nào đang có giao dịch thì BỎ QUA, vòng sau xử lý
```

| Được | Mất |
|---|---|
| Đúng chắc chắn; dễ suy luận; **không cần retry** | Giữ khoá tới hết transaction → transaction phải ngắn |
| `SKIP LOCKED` cho batch không bao giờ chặn khách | Nguy cơ **deadlock** nếu hai đường lấy khoá theo **thứ tự khác nhau** → bắt buộc quy ước: luôn khoá theo `id` tăng dần |

**Đây là bậc tôi khuyến nghị làm trước cho hệ thống hiện tại** — lý do ở [§6](#6-khuyến-nghị-cụ-thể-cho-all-in-one-v2).

### Bậc 3 — Sổ cái append-only thuần: **bỏ hẳn `current_balance`**

Không có trạng thái khả biến thì không có gì để race.

```text
TRƯỚC:  loan_account.current_balance   (UPDATE)  ─┐
        SUM(posting.amount)            (derive)  ─┴─→ hai nguồn, lệch nhau được

SAU:    CHỈ posting (INSERT-only)                ──→ một nguồn duy nhất
        balance = f(postings)                        không UPDATE = không lost update
```

| Được | Mất |
|---|---|
| Diệt tận gốc lỗi B; audit đầy đủ; tính lại được quá khứ bất kỳ lúc nào | `SUM()` chậm dần theo số posting → cần snapshot ([§7](#7-balance-real-time-đúng-khi-event-dồn-dập)) |
| Khớp đúng nguyên tắc kế toán kép **đã có sẵn** trong hệ thống | Phải sửa mọi chỗ đang đọc `current_balance` |

Vẫn cần bậc 2 cho lỗi A: append-only chống được *đè trạng thái*, nhưng không tự chống được *tính
toán trên số dư cũ*.

### Bậc 4 — Single-writer theo account

Đưa mọi lệnh ghi của một account qua **một hàng đợi có thứ tự** — Kafka với `key = loanAccountId`,
hoặc mô hình actor một account một actor.

```text
                       partition = hash(loanAccountId)
  payment  ──┐
  accrual  ──┼──►  Kafka  ──►  consumer duy nhất cho partition đó  ──►  DB
  late fee ──┘                 (xử lý TUẦN TỰ, không cần khoá)
```

| Được | Mất |
|---|---|
| **Loại bỏ** concurrency thay vì quản lý nó — không khoá, không retry, không deadlock | Đổi mô hình ghi từ đồng bộ sang bất đồng bộ: API không trả kết quả cuối ngay được |
| Thứ tự sự kiện của một account là xác định → replay được | Account nóng = partition nóng |

Đúng về mặt kiến trúc, nhưng là **thay đổi lớn**. Chỉ nên đề xuất như hướng dài hạn, kèm điều kiện
kích hoạt cụ thể.

> 📐 **Bậc này được triển khai đầy đủ ở [Phần II — §12](#12-nếu-được-thiết-kế-lại-từ-đầu--bản-triệt-để)**:
> mô hình dữ liệu, cách thiết kế batch tính lãi để không bao giờ đua với khách, chiến lược scale khi
> posting tăng không giới hạn, và [kiến trúc AWS khai báo từng bước](#13-kiến-trúc-aws--khai-báo-từng-bước).

### Bậc 5 — `SERIALIZABLE` cho vài đường quan trọng

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
```

Dùng cho những đường mà tính đúng đắn quan trọng hơn thông lượng (chốt sổ cuối kỳ, tất toán khoản
vay). **Bắt buộc** kèm retry `SQLState 40001` **và** idempotency key.

### Bảng chọn nhanh

| Bậc | Chống được lỗi A (tính trên số dư cũ) | Chống được lỗi B (đè trạng thái) | Chi phí sửa | Rủi ro |
|---|:---:|:---:|:---:|:---:|
| 0 — một transaction | 🟡 một phần | ✅ | Thấp | Thấp |
| 1 — optimistic `@Version` | 🟡 chỉ khi materialize xung đột | ✅ | Thấp | Retry storm |
| 2 — pessimistic `FOR UPDATE` | ✅ | ✅ | Trung bình | Deadlock nếu sai thứ tự khoá |
| 3 — append-only, bỏ `current_balance` | ❌ (cần kèm bậc 2) | ✅ **triệt để** | Trung bình–cao | Phải sửa nhiều điểm đọc |
| 4 — single-writer qua Kafka | ✅ | ✅ | Cao | Đổi mô hình API |
| 5 — `SERIALIZABLE` | ✅ | ✅ | Thấp/điểm | Abort nhiều, cần idempotency |

---

## 6. Khuyến nghị cụ thể cho all-in-one-v2

**Thứ tự này chọn theo tiêu chí: sửa được lỗi thật sớm nhất, với rủi ro hồi quy thấp nhất.**

### Bước 1 — Idempotency trước tiên *(làm trước cả việc sửa race)*

Nghe ngược đời, nhưng: mọi bậc còn lại đều có thể cần retry, và retry không idempotent trên hệ thống
tiền còn nguy hiểm hơn chính lỗi đang có.

```sql
ALTER TABLE transaction ADD COLUMN idempotency_key text;
CREATE UNIQUE INDEX ux_transaction_idem ON transaction (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
```

Với accrual, khoá tự nhiên đã có sẵn ngữ nghĩa — **không cần sinh UUID**:
`accrual:{loanAccountId}:{referenceDate}`. Một account, một ngày, đúng một bút toán lãi. Chạy lại
job bao nhiêu lần cũng vậy.

> Đây cũng là câu trả lời cho *"chạy lại batch có an toàn không?"* — hiện tại: **không**.

### Bước 2 — Thu hẹp ranh giới: từ lô xuống từng account

```java
// TRƯỚC (dòng 171-173): đọc số dư CẢ LÔ, ghi sau đó rất lâu
final List<LoanAccountIdBalance> balances =
        loanAccountMixService.getCurrentBalanceByListIds(ids, eobDate);

// SAU: mỗi account một transaction ngắn, đọc và ghi sát nhau
for (Long id : ids) {
    accrualService.accrueOne(id, eobDate);      // @Transactional bên trong
}
```

Đổi này **một mình đã thu hẹp cửa sổ race từ "cả batch" xuống "một account"** — giảm phần lớn tần
suất lỗi, và không đụng vào schema.

**Đánh đổi phải nói ra:** N transaction nhỏ chậm hơn 1 transaction lớn. Với batch chạy ngoài giờ thì
đổi thông lượng lấy tính đúng đắn là đáng. Nếu chậm quá thì chunk theo nhóm 100-500 account, **không**
quay lại một transaction cho toàn bộ.

### Bước 3 — Pessimistic lock theo account, thứ tự khoá cố định

```java
@Transactional
public void accrueOne(Long loanAccountId, Date asOf) {
    // 1. Khoá account. Mọi đường ghi khác (payment, late fee) PHẢI khoá cùng cách này.
    loanAccountRepository.findByIdForUpdate(loanAccountId)
            .orElseThrow(() -> new NotFoundException(loanAccountId));

    // 2. Đọc số dư — giờ đã được bảo vệ, không ai chen vào được
    BigDecimal balance = loanAccountRepository.getCurrentBalance(loanAccountId, asOf);

    // 3. Tính và ghi — vẫn trong cùng transaction, cùng khoá
    postingRepository.saveAll(buildAccrualPostings(loanAccountId, balance, asOf));
}   // khoá nhả khi commit
```

Cho batch dùng `SKIP LOCKED` để không bao giờ chặn khách đang giao dịch; account bị bỏ qua sẽ được
xử lý ở vòng quét sau.

**Quy ước chống deadlock — phải viết thành convention, không để tự phát:**
*mọi transaction chạm nhiều account phải khoá theo `loan_account_id` tăng dần.*

### Bước 4 — Xử lý `current_balance`

Hai lựa chọn, nên chọn (a):

| | Cách làm | Khi nào chọn |
|---|---|---|
| **(a) Bỏ hẳn** | Xoá cột, mọi nơi đọc chuyển sang `SUM(posting)` + snapshot ở [§7](#7-balance-real-time-đúng-khi-event-dồn-dập) | Nếu chịu được đợt sửa rộng. **Triệt để nhất** |
| **(b) Hạ cấp thành cache** | Giữ cột nhưng **chỉ** cập nhật trong transaction đã giữ khoá, thêm `balance_as_of_posting_id` để biết nó tươi tới đâu, và job đối soát định kỳ | Nếu quá nhiều nơi đang đọc |

**Tuyệt đối không giữ nguyên hiện trạng**: một cột persist được cập nhật từ nhiều đường mà không có
khoá, song song tồn tại với một nguồn derive khác — đó chính là định nghĩa của lỗi B.

### Bước 5 — Job đối soát *(thứ chứng minh đã sửa)*

```sql
-- chạy hằng ngày sau EOB; phải trả về RỖNG
SELECT la.id,
       la.current_balance                                  AS stored,
       COALESCE(sum(p.amount), 0)                          AS derived,
       la.current_balance - COALESCE(sum(p.amount), 0)     AS drift
FROM loan_account la
LEFT JOIN transaction   t ON t.loan_account_id = la.id
LEFT JOIN journal_entry j ON j.transaction_id  = t.id
LEFT JOIN posting       p ON p.journal_entry_id = j.id
LEFT JOIN ledger_account l ON l.id = p.ledger_account_id
WHERE l.type = 'PRINCIPAL' AND l.sub_type = 'LOAN_PRINCIPAL'
GROUP BY la.id, la.current_balance
HAVING la.current_balance <> COALESCE(sum(p.amount), 0);
```

`drift <> 0` → alert. Cùng tinh thần với reconcile job ở
[04 §6.4](04-event-counting-10k.md#64-idempotency--vì-sao-counter-không-phải-nguồn-sự-thật): **không
tin là đúng, đo để biết đúng.** Chạy query này trên production **trước khi sửa** cũng chính là cách
định lượng lỗi hiện tại đang lớn tới đâu — và là con số bạn mang vào phòng phỏng vấn.

---

## 7. Balance real-time đúng khi event dồn dập

Đây là phần bạn hỏi cụ thể: *"event nhiều cùng lúc thì làm sao show ra balance real-time đúng mà
không bị race condition."*

### 7.1 Hai yêu cầu xung đột nhau

```text
ĐÚNG    → balance phải = SUM(mọi posting)          → càng nhiều posting càng chậm
NHANH   → phải trả về trong vài chục ms            → không thể SUM lại từ đầu mỗi lần
```

Cache TTL mù **không dùng được ở đây** — khác hẳn bài đếm ở [04](04-event-counting-10k.md), nơi
dashboard trễ vài giây là chấp nhận được. Số dư tài khoản mà hiển thị cũ 5 giây thì khách vừa trả
tiền xong vẫn thấy số cũ → mất niềm tin, và tệ hơn: họ trả tiếp lần nữa.

### 7.2 Lời giải: snapshot + phần đuôi, chốt theo `posting_id`

Ý tưởng: **đóng băng phần quá khứ, chỉ cộng phần mới.**

```sql
CREATE TABLE balance_snapshot (
    loan_account_id  bigint      NOT NULL,
    ledger_type      text        NOT NULL,
    balance          numeric(19,4) NOT NULL,
    up_to_posting_id bigint      NOT NULL,   -- ◄── chốt tới posting nào
    updated_at       timestamptz NOT NULL,
    PRIMARY KEY (loan_account_id, ledger_type)
);
```

```sql
-- Đọc balance: snapshot + đúng những posting sinh ra SAU snapshot
SELECT s.balance + COALESCE(sum(p.amount), 0) AS balance
FROM balance_snapshot s
LEFT JOIN posting p ON p.id > s.up_to_posting_id
     AND p.journal_entry_id IN (SELECT j.id FROM journal_entry j
                                JOIN transaction t ON t.id = j.transaction_id
                                WHERE t.loan_account_id = :id)
WHERE s.loan_account_id = :id AND s.ledger_type = 'LOAN_PRINCIPAL'
GROUP BY s.balance;
```

**Vì sao chốt theo `posting_id` chứ không theo thời gian:**

| Chốt theo | Vấn đề |
|---|---|
| `updated_at < now()` | Đồng hồ lệch giữa các instance; posting ghi sau nhưng timestamp trước → **cộng thiếu hoặc cộng trùng** |
| **`id > up_to_posting_id`** | `id` là `IDENTITY` đơn điệu tăng, do DB cấp. Không phụ thuộc đồng hồ. **Không thể trùng, không thể sót** |

> Đây là điểm kỹ thuật đáng nói nhất trong cả mục này: **dùng số thứ tự do DB cấp làm ranh giới,
> không dùng thời gian.** Cùng một bài học với `event_id` ở
> [04 §6.4](04-event-counting-10k.md#64-idempotency--vì-sao-counter-không-phải-nguồn-sự-thật).

**Chi phí đọc bị chặn trên:** snapshot cập nhật mỗi khi có ~100 posting mới (hoặc mỗi phút) → mỗi
lần đọc chỉ cộng tối đa ~100 dòng, **bất kể account đó có 10 hay 10 triệu posting**.

### 7.3 Read-your-writes: khách vừa trả tiền phải thấy ngay

Đây là yêu cầu riêng của "real-time", và là chỗ nhiều thiết kế bỏ sót.

```text
Khách POST /payment  →  commit, DB cấp posting.id = 90.123
                     →  API trả về:  { "balance": 7000, "as_of_posting_id": 90123 }
                                                          └── client GIỮ số này

Client GET /balance?min_posting_id=90123
   → server đọc; nếu snapshot + đuôi chưa tới 90.123 (đang đọc read replica bị trễ)
       → hoặc đọc lại từ primary, hoặc chờ ngắn rồi thử lại
   → KHÔNG BAO GIỜ trả về một số dư cũ hơn cái khách vừa thấy
```

Không có cơ chế này thì với read replica, khách trả tiền xong bấm refresh **có thể thấy số dư cũ** —
một lỗi trông y hệt race condition dù DB hoàn toàn nhất quán. **Số dư phải đơn điệu theo góc nhìn của
một khách hàng**, kể cả khi hệ thống là eventual consistency ở tầng dưới.

### 7.4 Đẩy thay đổi thay vì để client hỏi liên tục

```text
posting mới được ghi (trong cùng transaction)
        │
        └─► outbox table  ──►  publisher  ──►  WebSocket/SSE  ──►  UI cập nhật
                                                                   kèm as_of_posting_id
```

Dùng **transactional outbox**: ghi sự kiện vào bảng `outbox` **trong cùng transaction** với posting,
rồi một publisher riêng đọc và phát. Nếu bắn sự kiện trực tiếp trong code service, sẽ có lúc
transaction rollback mà sự kiện đã bay đi → UI hiện số dư của một giao dịch **không tồn tại**.

### 7.5 Tóm tắt kiến trúc đọc

```text
        ┌──────────────────────────────────────────────────────────┐
GHI     │ 1 transaction:  FOR UPDATE account                       │
        │                 INSERT posting(s)   (append-only)        │
        │                 INSERT outbox                            │
        │                 [tuỳ chọn] refresh balance_snapshot      │
        └───────────────────────────┬──────────────────────────────┘
                                    ▼
ĐỌC     balance = snapshot.balance + SUM(posting WHERE id > up_to_posting_id)
        ├─ chi phí bị chặn trên (~100 dòng), không phụ thuộc lịch sử account
        ├─ trả kèm as_of_posting_id → client dùng cho read-your-writes
        └─ KHÔNG cache TTL mù; nếu cache thì key chứa max(posting_id)
```

---

## 8. Batch và online không đánh nhau

Ngay cả khi đã khoá đúng, batch vẫn có thể làm khách phải chờ. Bốn cách, dùng kết hợp:

| Cách | Làm gì | Đánh đổi |
|---|---|---|
| **`SKIP LOCKED`** | Batch bỏ qua account đang có giao dịch, gom lại xử lý ở vòng sau | Cần vòng quét lần hai; phải theo dõi account bị bỏ qua nhiều lần bất thường |
| **Chunk nhỏ, commit từng chunk** | 100-500 account/transaction thay vì cả lô | Nhiều transaction hơn; nhưng khoá được giữ ngắn hơn nhiều |
| **Cửa sổ EOB** | Chạy accrual sau giờ chốt sổ (code đã có khái niệm EOB — `DateUtils.setHours(..., 19)`) | Không giúp được với khách giao dịch 24/7 |
| **Accrual thành event trên cùng luồng** | Batch không ghi DB trực tiếp mà **publish** lệnh accrual vào cùng hàng đợi với payment (bậc 4) | Thay đổi lớn nhất, nhưng khiến tranh chấp **biến mất** thay vì được quản lý |

**Nói được điều này là ghi điểm:** thứ tự xử lý giữa payment và accrual trong cùng một ngày là một
**quyết định nghiệp vụ**, không phải chi tiết kỹ thuật. Phải hỏi kế toán: *nếu khách trả tiền lúc
10:00 và lãi tính cho ngày hôm đó, lãi tính trên số dư trước hay sau khoản trả?* Câu trả lời quyết
định thiết kế — và nếu không hỏi, ta chỉ đang làm cho code chạy không lỗi chứ chưa chắc **đúng**.

---

## 9. Chứng minh đã sửa — 3 loại test

Lỗi race không tái hiện được bằng unit test thường. Cần đúng ba loại:

### 9.1 Test tranh chấp thật, hai luồng, DB thật

```java
@Testcontainers
class AccrualPaymentRaceTest {
    @Container static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:16");

    @RepeatedTest(50)                                  // race hiếm → phải lặp
    void interestMustUseBalanceAtWriteTime() throws Exception {
        Long accountId = seedLoan(new BigDecimal("10000"));
        var barrier = new CyclicBarrier(2);             // ép hai luồng chạy CÙNG LÚC

        var pool = Executors.newFixedThreadPool(2);
        var payment = pool.submit(() -> { barrier.await(); paymentService.pay(accountId, new BigDecimal("3000")); return null; });
        var accrual = pool.submit(() -> { barrier.await(); accrualService.accrueOne(accountId, today());  return null; });
        payment.get(); accrual.get();

        // BẤT BIẾN: lãi phải khớp với số dư ngay trước bút toán lãi — dù thứ tự nào thắng
        assertThat(actualInterest(accountId))
            .isIn(new BigDecimal("5.00"),      // accrual thắng trước: 10.000 × 0,05%
                  new BigDecimal("3.50"));     // payment thắng trước:  7.000 × 0,05%
        // giá trị nào KHÁC hai số này = race vẫn còn
    }
}
```

Điểm mấu chốt: **`CyclicBarrier` để ép trùng thời điểm**, và **`@RepeatedTest`** vì race không xảy
ra mỗi lần. Chạy trên Postgres thật qua Testcontainers — H2 có ngữ nghĩa khoá khác, test trên H2 sẽ
xanh mà production vẫn vỡ.

### 9.2 Test bất biến kế toán

```java
@Test
void everyJournalEntryMustBalance() {
    // kế toán kép: tổng mọi posting trong một journal_entry phải bằng 0
    assertThat(jdbc.queryForList(
        "SELECT journal_entry_id FROM posting " +
        "GROUP BY journal_entry_id HAVING sum(amount) <> 0"))
      .isEmpty();
}
```

Bất biến này **độc lập với mọi logic nghiệp vụ**. Nếu nó vỡ thì sổ cái sai, bất kể vì lý do gì.

### 9.3 Test kiến trúc — chặn tái phạm

Cùng dạng với gate `test_no_retry_setter_outside_verify` trong repo BOM của bạn
([../AI-STACK-INTERVIEW-ANSWERS.md §4B](../AI-STACK-INTERVIEW-ANSWERS.md)): biến một quy ước thành
một cái test, thay vì một dòng ghi chú trong wiki.

```java
@Test
void noWriteToLedgerWithoutAccountLock() throws IOException {
    // mọi phương thức lưu Posting phải nằm trong class đã gọi findByIdForUpdate
    List<Path> offenders = Files.walk(Path.of("src/main/java"))
        .filter(p -> p.toString().endsWith(".java"))
        .filter(p -> contains(p, "postingRepository.save"))
        .filter(p -> !contains(p, "findByIdForUpdate"))
        .toList();
    assertThat(offenders)
        .as("Ghi posting mà không khoá account trước — xem 06 §6 bước 3")
        .isEmpty();
}
```

> Test này thô (grep văn bản) nhưng bắt được đúng thứ cần bắt: **người thứ ba, sáu tháng sau, thêm
> một đường ghi mới và quên khoá.** Đó mới là cách lỗi này quay lại.

---

## 10. Câu trả lời phỏng vấn + 12 follow-up

### 10.1 Kể theo STAR (90 giây)

> **S —** *"Hệ thống cho vay tôi làm dùng sổ cái kế toán kép: transaction → journal entry →
> posting. Số dư derive bằng tổng các posting. Khách có thể giao dịch nhiều lần trong một phút,
> đồng thời có batch job tính lãi hằng ngày chạy trên cùng account đó."*
>
> **T —** *"Chúng tôi phát hiện lãi bị tính sai: lãi được tính trên số dư trước khi khách thanh
> toán, nhưng lại ghi sổ sau khi thanh toán đã vào."*
>
> **A —** *"Tôi truy ra ba điều. Một, đường tính lãi đọc số dư cho cả lô rồi ghi sổ ở một transaction
> khác — khoảng hở giữa đọc và ghi dài bằng thời gian xử lý cả lô. Hai, không có transaction nào bao
> quanh, và toàn hệ thống không có một chỗ nào dùng optimistic hay pessimistic lock. Ba — chỗ này
> đáng nói nhất — trong file đó **có** một khối `synchronized`, nên nhìn vào tưởng đã xử lý
> concurrency; nhưng nó bọc `SimpleDateFormat` vốn đã là `ThreadLocal`, và là khoá JVM-local trong
> khi service chạy từ hai instance ECS.*
>
> *Tôi gọi đúng tên lỗi trước khi sửa: đây là write skew, không phải lost update — hai bên ghi hai
> dòng khác nhau nên database không thấy xung đột nào, không có exception, nó ghi 'thành công' và
> sai trong im lặng. Đó là lý do nó lọt tới production.*
>
> *Cách sửa tôi làm theo thứ tự: idempotency key trước — vì mọi phương án đều cần retry, mà retry
> không idempotent trên hệ thống tiền còn tệ hơn lỗi đang có. Rồi thu hẹp phạm vi từ cả lô xuống
> từng account. Rồi pessimistic lock theo account với `SKIP LOCKED` cho batch để nó không bao giờ
> chặn khách đang giao dịch. Cuối cùng bỏ cột `current_balance` — cột đó là nguồn sự thật thứ hai và
> là chỗ duy nhất bị đè ngược."*
>
> **R —** *"Và tôi thêm một job đối soát so `current_balance` với `SUM(posting)`, cộng một test chạy
> hai luồng thật trên Postgres qua Testcontainers có `CyclicBarrier` ép trùng thời điểm. Chạy query
> đối soát trên production **trước khi** sửa cũng chính là cách tôi định lượng lỗi đang lớn tới đâu —
> tôi không muốn nói 'đã sửa' mà không có số."*

### 10.2 Follow-up

<details>
<summary><b>Nhóm A — chẩn đoán (4 câu)</b></summary>

**A1. "Vì sao `@Transactional` không đủ?"**
> Vì Postgres mặc định `READ COMMITTED`, và mức đó chỉ chống dirty read. Mỗi câu lệnh trong
> transaction vẫn nhìn snapshot mới nhất tại lúc nó chạy, nên đọc số dư ở đầu, người khác commit,
> mình ghi ở cuối — không có xung đột nào bị phát hiện. `@Transactional` là điều kiện cần, không
> phải điều kiện đủ.

**A2. "Sao anh biết đây là write skew chứ không phải lost update?"**
> Lost update là hai bên `UPDATE` **cùng một dòng**. Ở đây hai bên `INSERT` **hai posting khác
> nhau** — về mặt database là hoàn toàn hợp lệ, không đụng nhau. Cái bị vi phạm là một **bất biến
> nghiệp vụ**: lãi phải tính trên số dư tại thời điểm ghi sổ. Database không biết bất biến đó tồn
> tại. Phân biệt được hai cái này quan trọng vì lost update thì optimistic lock bắt được, còn write
> skew thì không — trừ khi mình cố ý tạo ra một điểm xung đột vật chất.

**A3. "Nếu chỉ có một instance thì `synchronized` có đủ không?"**
> Vẫn không, vì batch và API là hai service khác nhau, hai process khác nhau. Và kể cả cùng process
> thì nó cũng khoá nhầm: khối `synchronized` hiện có bọc `SimpleDateFormat`, mà formatter đó đã
> `ThreadLocal` rồi. Nó khoá thứ đã an toàn, còn thứ cần khoá thì bỏ trống.

**A4. "Làm sao anh phát hiện ra lỗi này?"**
> Từ đối soát sổ sách, không phải từ log — vì **không có log lỗi nào để mà thấy**. Đó chính là đặc
> điểm nguy hiểm nhất của lớp lỗi này: nó ghi "thành công". Sau đó tôi biến chính phép đối soát ấy
> thành một job chạy hằng ngày, để lần sau nó tự lộ ra chứ không phải chờ người phát hiện.
</details>

<details>
<summary><b>Nhóm B — lựa chọn giải pháp (5 câu)</b></summary>

**B1. "Vì sao chọn pessimistic thay vì optimistic?"**
> Vì optimistic chỉ bắt được xung đột khi **cả hai bên cùng UPDATE một dòng**. Ở đây batch chỉ INSERT
> posting, không chạm `loan_account`, nên `@Version` sẽ không bao giờ bump và không phát hiện được
> gì — trừ khi tôi cố ý bắt mọi đường ghi phải chạm `loan_account` để materialize xung đột. Cách đó
> hoạt động, nhưng phức tạp hơn mà không rõ ràng hơn `FOR UPDATE`. Với lại tranh chấp trên **cùng
> một account** là thấp, khoá được giữ trong một transaction rất ngắn — đúng điều kiện dùng
> pessimistic.

**B2. "`SERIALIZABLE` có giải quyết được không?"**
> Có, Postgres SSI sẽ phát hiện write skew này và ném `40001`. Nhưng phải retry mọi transaction bị
> huỷ, mà retry giao dịch tiền thì bắt buộc idempotent — hiện chưa có. Và batch chạy hàng nghìn
> account sẽ abort liên tục vì đụng luồng online. Tôi để dành `SERIALIZABLE` cho vài đường quan
> trọng như chốt sổ hay tất toán, không dùng làm giải pháp toàn hệ thống — 562 chỗ `@Transactional`
> là quá rộng để đổi isolation một lượt.

**B3. "`SKIP LOCKED` khiến batch bỏ sót account thì sao?"**
> Không bỏ sót, chỉ hoãn — account bị bỏ qua được xử lý ở vòng quét sau. Quan trọng là phải **đếm**
> số lần bỏ qua: một account bị skip nhiều vòng liên tiếp là tín hiệu bất thường, phải alert chứ
> không được im lặng. Và vì có idempotency key `accrual:{id}:{date}`, chạy lại vòng sau không tạo
> bút toán trùng.

**B4. "Vì sao làm idempotency trước khi sửa race?"**
> Vì mọi phương án sửa race đều có thể cần retry, và retry không idempotent trên hệ thống tiền sẽ
> đổi một lỗi khó thấy lấy một lỗi tệ hơn: khách bị tính lãi hai lần. Idempotency là điều kiện tiên
> quyết của retry, không phải tính năng thêm sau. Và nó rẻ — với accrual thì khoá tự nhiên đã có sẵn
> ngữ nghĩa, một account một ngày một bút toán, không cần sinh UUID.

**B5. "Đưa hết qua Kafka một partition mỗi account thì sao?"**
> Đó là hướng đúng nhất về kiến trúc — nó **loại bỏ** concurrency thay vì quản lý nó, không khoá,
> không retry, không deadlock, và thứ tự sự kiện của một account trở thành xác định nên replay được.
> Nhưng nó đổi mô hình ghi từ đồng bộ sang bất đồng bộ, API không trả kết quả cuối ngay được nữa.
> Tôi để đó là hướng dài hạn với điều kiện kích hoạt rõ ràng, không đề xuất làm ngay để chữa một lỗi
> mà `FOR UPDATE` chữa được.
</details>

<details>
<summary><b>Nhóm C — real-time và vận hành (3 câu)</b></summary>

**C1. "Balance real-time mà không SUM lại từ đầu thì làm sao?"**
> Snapshot cộng phần đuôi: lưu số dư đã chốt tới một `posting_id` nhất định, đọc thì lấy snapshot
> cộng các posting có id lớn hơn mốc đó. Chi phí đọc bị chặn trên bởi số posting mới kể từ snapshot,
> không phụ thuộc account có bao nhiêu lịch sử. Điểm quan trọng: chốt theo **`posting_id`** chứ
> không theo thời gian — id do DB cấp, đơn điệu tăng, không phụ thuộc đồng hồ; chốt theo timestamp
> thì đồng hồ lệch giữa các instance sẽ làm cộng sót hoặc cộng trùng.

**C2. "Khách vừa trả tiền, refresh thấy số cũ thì sao?"**
> Đó là vấn đề read-your-writes, và nó **trông y hệt race condition** dù database hoàn toàn nhất
> quán — nguyên nhân thật là đọc từ read replica bị trễ. Cách xử lý: API ghi trả về
> `as_of_posting_id`, client giữ và gửi kèm khi đọc; nếu replica chưa bắt kịp mốc đó thì đọc lại từ
> primary. Nguyên tắc là số dư phải **đơn điệu theo góc nhìn của một khách hàng**.

**C3. "Bắn WebSocket báo số dư mới thì đặt ở đâu?"**
> Transactional outbox: ghi sự kiện vào bảng `outbox` **trong cùng transaction** với posting, rồi
> publisher riêng đọc và phát. Nếu bắn thẳng trong code service thì sẽ có lúc transaction rollback
> mà sự kiện đã bay đi — UI hiện số dư của một giao dịch không tồn tại. Đó là lỗi tệ hơn hẳn việc
> hiển thị chậm vài trăm mili-giây.
</details>

---

## 12. Nếu được thiết kế lại từ đầu — bản triệt để

> [§5](#5-thang-giải-pháp--5-bậc) và [§6](#6-khuyến-nghị-cụ-thể-cho-all-in-one-v2) là **chữa hệ
> thống đang chạy**. Mục này trả lời câu khác hẳn: *nếu viết lại từ trang giấy trắng thì làm thế nào
> để bài toán này **không tồn tại**, và chịu được khi transaction/posting tăng không giới hạn.*

### 12.1 Một thay đổi ĐỊNH NGHĨA xoá được phần lớn bài toán

Đây là ý quan trọng nhất của cả mục. Trước khi bàn khoá, stream hay AWS, hãy nhìn lại **lãi được
định nghĩa là gì**:

```text
ĐỊNH NGHĨA CŨ (ngầm, nằm trong code):
    lãi = f( số dư ĐỌC ĐƯỢC tại thời điểm job chạy )
    → phụ thuộc đồng hồ treo tường → không xác định → PHẢI khoá để ghim nó lại

ĐỊNH NGHĨA MỚI (tường minh, nằm trong hợp đồng nghiệp vụ):
    lãi cho ngày D = f( trạng thái sổ cái với mọi bút toán có effective_date <= D )
    → hàm THUẦN TUÝ của dữ liệu, không phụ thuộc lúc nào chạy
    → chạy 19:00 hay 23:00 hay chạy lại ngày mai: RA CÙNG MỘT SỐ
```

**Hệ quả — ba vấn đề tan biến, không cần cơ chế nào:**

| Vấn đề cũ | Với định nghĩa mới |
|---|---|
| Khách thanh toán **lúc job đang chạy** | Nếu khoản đó có `effective_date > D` → **không ảnh hưởng** lãi ngày D. Không có race |
| Job chạy lại (retry, rerun) | Hàm thuần tuý → cùng input, cùng output. Idempotent **theo bản chất**, không cần cơ chế |
| Khách thanh toán **trễ**, `effective_date <= D` | Không phải race — là **tính lại**. Sinh bút toán điều chỉnh, không khoá gì cả |

> **Câu ăn điểm nhất cả file:** *"Race condition ở đây không sinh ra từ thiếu khoá. Nó sinh ra từ
> việc định nghĩa lãi theo **thời điểm chạy job** thay vì theo **ngày nghiệp vụ**. Sửa định nghĩa
> thì phần lớn nhu cầu khoá biến mất — và cái còn lại đổi từ bài toán đồng thời sang bài toán tính
> lại, vốn dễ hơn nhiều."*

**Phân biệt hai trục thời gian** — thiếu cái này thì mọi thiết kế ledger đều sai:

| Trục | Cột | Ai quyết định | Dùng để |
|---|---|---|---|
| **Thời gian nghiệp vụ** | `effective_date` | Nghiệp vụ / khách | **Tính toán**. Bút toán *thuộc về* ngày nào |
| **Thời gian hệ thống** | `recorded_at` | Database | **Audit**. Ta *biết* điều đó lúc nào |

Sổ cái có đủ hai trục là **bi-temporal** — trả lời được cả *"số dư ngày 15 là bao nhiêu"* lẫn
*"ngày 20 chúng ta **tưởng** số dư ngày 15 là bao nhiêu"*. Câu thứ hai là câu kiểm toán viên sẽ hỏi.

### 12.2 Năm quyết định nền

| # | Quyết định | Vì sao | Đánh đổi |
|---|---|---|---|
| 1 | **Append-only tuyệt đối** — không `UPDATE`, không `DELETE` trên ledger | Không có trạng thái khả biến thì không có lost update. Sửa sai = bút toán ngược | Dữ liệu chỉ tăng; phải có tầng lưu trữ lạnh |
| 2 | **Single-writer theo account** | Loại bỏ đồng thời thay vì quản lý nó: không khoá, không retry, không deadlock | Ghi thành bất đồng bộ; API trả `202` chứ không trả kết quả cuối |
| 3 | **Balance là projection, không phải state** | Một nguồn sự thật duy nhất. Dựng lại được từ ledger bất cứ lúc nào | Cần snapshot để đọc nhanh |
| 4 | **Mọi phép tính tiền là hàm thuần tuý của ledger tại một `effective_date`** | [§12.1](#121-một-thay-đổi-định-nghĩa-xoá-được-phần-lớn-bài-toán) | Phải chốt định nghĩa với kế toán trước khi code |
| 5 | **Idempotency key là khoá tự nhiên, không phải UUID ngẫu nhiên** | `accrual:{account}:{date}` tự nó nói lên *"một account, một ngày, một bút toán"*. UUID ngẫu nhiên không chống được double-submit từ hai nguồn khác nhau | Phải nghĩ ra khoá tự nhiên cho từng loại nghiệp vụ |

### 12.3 Mô hình dữ liệu

```sql
-- ══ SỔ CÁI: append-only, bất biến, KHÔNG BAO GIỜ update ══════════════════
CREATE TABLE ledger_entry (
    account_id       bigint       NOT NULL,
    account_seq      bigint       NOT NULL,   -- ◄ số thứ tự RIÊNG của account, liên tục, do writer cấp
    entry_id         uuid         NOT NULL,
    journal_id       uuid         NOT NULL,   -- gom các dòng của cùng một bút toán kép
    ledger_account   text         NOT NULL,   -- PRINCIPAL / INTEREST_RECEIVABLE / ...
    amount           numeric(19,4) NOT NULL,  -- dương = ghi nợ, âm = ghi có
    effective_date   date         NOT NULL,   -- ◄ THỜI GIAN NGHIỆP VỤ — dùng để TÍNH
    recorded_at      timestamptz  NOT NULL DEFAULT now(),  -- ◄ THỜI GIAN HỆ THỐNG — dùng để AUDIT
    idempotency_key  text         NOT NULL,
    PRIMARY KEY (account_id, account_seq)
) PARTITION BY RANGE (effective_date);

CREATE UNIQUE INDEX ux_ledger_idem ON ledger_entry (idempotency_key);
CREATE INDEX ix_ledger_acct_eff   ON ledger_entry (account_id, effective_date, account_seq);

-- ══ SNAPSHOT: cache có thể vứt đi và dựng lại ════════════════════════════
CREATE TABLE balance_snapshot (
    account_id      bigint        NOT NULL,
    ledger_account  text          NOT NULL,
    balance         numeric(19,4) NOT NULL,
    up_to_seq       bigint        NOT NULL,   -- ◄ chốt theo account_seq, KHÔNG theo thời gian
    PRIMARY KEY (account_id, ledger_account)
);

-- ══ OUTBOX: phát sự kiện trong CÙNG transaction với ledger ═══════════════
CREATE TABLE outbox (
    id           bigserial PRIMARY KEY,
    account_id   bigint      NOT NULL,
    payload      jsonb       NOT NULL,
    published_at timestamptz              -- NULL = chưa phát
);
```

**Vì sao `account_seq` là cột quan trọng nhất trong toàn bộ thiết kế:**

| Nó cho ta | Cụ thể |
|---|---|
| **Thứ tự xác định trong một account** | Do single-writer cấp → liên tục, không hở, không trùng |
| **Ranh giới snapshot đáng tin** | `up_to_seq` không phụ thuộc đồng hồ — xem [§7.2](#72-lời-giải-snapshot--phần-đuôi-chốt-theo-posting_id) |
| **Khoá lạc quan miễn phí** | Client gửi `expected_seq`; writer từ chối nếu account đã tiến xa hơn |
| **Read-your-writes** | Trả `account_seq` về cho client, client dùng nó khi đọc — [§7.3](#73-read-your-writes-khách-vừa-trả-tiền-phải-thấy-ngay) |
| **Phát hiện mất mát** | Seq nhảy cóc = có bút toán biến mất. Bất biến kiểm tra được bằng SQL |

> **Vì sao seq theo từng account chứ không phải một sequence toàn cục:** sequence toàn cục là một
> điểm tranh chấp duy nhất cho toàn hệ thống — chính thứ ta đang tìm cách loại bỏ. Seq theo account
> do writer của partition đó cấp trong bộ nhớ, không cần phối hợp với ai.

### 12.4 Đường ghi: single-writer theo account

```text
   payment ──┐
   accrual  ─┼──►  STREAM  ──► partition = hash(account_id)  ──►  WRITER (1 cho mỗi partition)
   late fee ─┘     (có thứ tự)                                      │
                                                                    │  Mỗi lệnh, TUẦN TỰ:
                                                                    │   1. đọc snapshot + đuôi
                                                                    │   2. TÍNH (hàm thuần tuý)
                                                                    │   3. cấp account_seq kế tiếp
                                                                    │   4. INSERT ledger + outbox
                                                                    │      (1 transaction)
                                                                    ▼
                                                          Aurora PostgreSQL
```

**Điểm mấu chốt phải nói ra:** trong writer, bước "đọc" và bước "ghi" **không cần khoá** — vì
**không có ai khác** có thể ghi vào account đó. Tính duy nhất của writer là do phân hoạch stream bảo
đảm, không phải do khoá bảo đảm.

```text
Bậc 2 ([§5]):  nhiều writer  +  khoá  →  đúng, nhưng phải quản lý tranh chấp, deadlock, retry
Bậc 4 (mục này): MỘT writer   +  không khoá  →  không có gì để tranh chấp
```

### 12.5 Batch tính lãi thiết kế thế nào

Đây là câu bạn hỏi cụ thể. **Ba thiết kế, khác nhau ở chỗ *ai* tính, không phải ở chỗ *khoá thế nào*.**

| | Batch làm gì | Race? | Nhận xét |
|---|---|:---:|---|
| **A. Hiện tại** | Đọc balance → **tự tính lãi** → ghi DB | ❌ Có | Khoảng hở đọc→ghi. Đây là bug đang có |
| **B. Bản vá [§6](#6-khuyến-nghị-cụ-thể-cho-all-in-one-v2)** | Khoá account → đọc → tự tính → ghi | ✅ Không | Đúng, nhưng batch **tranh chấp** với khách |
| **C. Bản triệt để** | **Chỉ phát lệnh** `AccrueInterest(account, date)`. **KHÔNG tính, KHÔNG đọc balance** | ✅ Không | Batch không bao giờ chạm DB. Không có gì để tranh chấp |

**Thiết kế C — tách "quyết định tính lãi" khỏi "tính ra bao nhiêu":**

```text
┌── EventBridge cron 19:00 ────────────────────────────────────────────────┐
│                                                                          │
│  Commander (Lambda):                                                     │
│    - liệt kê account đủ điều kiện (query CHỈ ĐỌC, không cần chính xác    │
│      tuyệt đối — thừa một account thì writer tự bỏ qua)                  │
│    - phát lệnh vào stream:                                               │
│         { type: "ACCRUE_INTEREST", accountId: 42, businessDate: "..." ,  │
│           idempotencyKey: "accrual:42:2026-08-29" }                      │
│    - KHÔNG đọc balance. KHÔNG tính tiền. KHÔNG ghi DB.                   │
└──────────────────────────────────┬───────────────────────────────────────┘
                                   ▼
        Lệnh vào ĐÚNG partition của account 42, xếp hàng SAU mọi
        giao dịch của khách đã tới trước nó, TRƯỚC mọi giao dịch tới sau
                                   ▼
┌── Writer của partition đó ───────────────────────────────────────────────┐
│  xử lý tuần tự, khi tới lượt lệnh này:                                   │
│    balance = f(ledger, effective_date <= businessDate)   ◄ hàm thuần tuý │
│    interest = balance × dailyRate                                        │
│    INSERT ledger_entry(...) ON CONFLICT (idempotency_key) DO NOTHING     │
└──────────────────────────────────────────────────────────────────────────┘
```

**Vì sao thiết kế này diệt được race, nói bằng ba câu:**

1. Batch **không đọc balance** → không có "đọc lúc T1".
2. Lệnh accrual **đi cùng đường** với giao dịch khách, cùng partition → thứ tự **xác định**, không
   phải do đồng hồ quyết định.
3. Balance được tính **bởi chính writer, tại đúng vị trí của lệnh trong chuỗi** → không có khoảng
   hở nào để chen vào.

**Câu hỏi bạn đặt ra — "nếu khách thêm transaction cùng lúc thì sao":**

```text
Khách gửi payment lúc 19:00:03, commander phát lệnh accrual lúc 19:00:00.

Cả hai vào cùng partition của account đó. Chỉ có hai khả năng, và CẢ HAI đều đúng:

  (a) accrual xếp trước → tính trên số dư chưa trừ payment
      → nhưng nếu payment có effective_date <= businessDate thì
        job đối soát phát hiện và sinh BÚT TOÁN ĐIỀU CHỈNH. Không sai sổ.

  (b) payment xếp trước → accrual tính trên số dư đã trừ. Đúng ngay.

KHÔNG CÓ khả năng thứ ba — không có trạng thái lai nửa cũ nửa mới,
vì writer xử lý TỪNG lệnh MỘT, và balance luôn là f(ledger tại đúng điểm đó).
```

> **Đây là câu trả lời hoàn chỉnh nhất cho câu hỏi của bạn:** vấn đề không phải "làm sao chặn khách
> giao dịch trong lúc batch chạy" — mà là **làm sao để thứ tự nào cũng cho kết quả đúng và giải
> thích được**. Chặn khách là sai hướng: khách có quyền giao dịch bất cứ lúc nào.

**Tối ưu sâu hơn — batch tốt nhất là batch không chạy:**

Nếu lãi là hàm thuần tuý của ledger, thì **không nhất thiết phải materialize nó mỗi ngày**:

```text
EAGER (đang làm):  mỗi account, mỗi ngày → 1 journal entry + 2 posting
                   100k account × 365 ngày × 2  =  73 TRIỆU posting/năm
                   → phần lớn tăng trưởng ledger đến từ CHÍNH BATCH, không phải từ khách

LAZY:              chỉ materialize khi CHỐT KỲ (sao kê, tất toán, đóng sổ tháng)
                   accrued(account, D) tính on-demand từ ledger
                   → giảm ~95% số posting sinh ra
```

| | Eager (mỗi ngày mỗi account) | Lazy (chốt kỳ) |
|---|---|---|
| Số posting | 73M/năm (100k account) | ~3M/năm |
| Đọc "lãi tích luỹ hôm nay" | Đọc sẵn, nhanh | Tính on-demand, tốn hơn |
| Bút toán sổ cái tổng (GL) hằng ngày | Có sẵn | **Vẫn có** — nhưng ghi **gộp** ở mức ledger account, không phải mỗi khách một dòng |
| Rủi ro | Ledger phình theo thời gian | **Phải được kế toán duyệt** |

> ⚠️ **Không tự quyết cái này.** Yêu cầu ghi nhận doanh thu hằng ngày ở mức GL là **quy định kế
> toán**, không phải lựa chọn kỹ thuật. Cách dung hoà thường dùng: **GL ghi gộp hằng ngày** (một
> bút toán cho toàn danh mục, không phải mỗi khách một bút toán) + **chi tiết theo khách tính
> on-demand**. Nêu được phương án dung hoà này là tín hiệu hiểu domain, không chỉ hiểu kỹ thuật.

### 12.6 Scale: khi transaction và posting tăng không giới hạn

**Ledger chỉ tăng, không bao giờ xoá** (quy định lưu trữ). Nên bài toán scale ở đây **không phải**
"xoá cho nhẹ" mà là "làm sao chi phí **đọc** không tăng theo lịch sử".

| Áp lực | Cách xử lý | Kết quả |
|---|---|---|
| **Đọc balance chậm dần theo số posting** | Snapshot + phần đuôi ([§7.2](#72-lời-giải-snapshot--phần-đuôi-chốt-theo-posting_id)), refresh mỗi ~100 entry | Chi phí đọc **bị chặn trên**, không phụ thuộc account có 10 hay 10 triệu entry |
| **Bảng ledger quá lớn để index hiệu quả** | `PARTITION BY RANGE (effective_date)` theo tháng | Query một khoảng chỉ chạm vài partition; index nhỏ, nằm gọn trong RAM |
| **Dữ liệu cũ chiếm chỗ đắt tiền** | Partition > 2 năm: `DETACH` → export Parquet lên **S3**, truy vấn bằng **Athena** | Ledger nóng giữ kích thước ổn định; dữ liệu lạnh rẻ ~10× mà vẫn truy vấn được |
| **Một writer/DB không đủ throughput ghi** | Shard theo dải `account_id` sang nhiều cluster Aurora | Ghi scale tuyến tính. **Mất:** không còn transaction xuyên shard → chuyển tiền giữa hai shard phải dùng saga |
| **Account nóng (nhiều giao dịch bất thường)** | Partition nóng — không tách được vì thứ tự trong account là bắt buộc | **Giới hạn cứng phải nói ra:** một account không thể ghi song song. Nếu một account cần hơn ~1000 ghi/giây thì mô hình này không hợp |

**Ước lượng để có số mà nói** (giả định, **chưa đo** — xem [§11](#11-ranh-giới-trung-thực)):

```text
100.000 account hoạt động
  Accrual eager:  100k × 365 × 2 posting        =  73M posting/năm
  Giao dịch khách: 100k × 5/tháng × 2 posting   =  12M posting/năm
                                          TỔNG  ≈  85M posting/năm
  ~120 byte/dòng  →  ~10 GB/năm  →  sau 7 năm ≈ 70 GB nóng nếu không tách tầng
  Với LAZY accrual:                             ≈  15M posting/năm  (giảm ~82%)
```

→ Con số này nói lên điều đáng chú ý: **chính batch accrual là nguồn tăng trưởng chính của ledger,
không phải khách hàng.** Đó là lý do câu hỏi "thiết kế batch thế nào" quan trọng hơn nó thoạt nghe.

---

## 13. Kiến trúc AWS — khai báo từng bước

> Bám sát hạ tầng **đã có thật** của bạn (ECS Fargate, Aurora PostgreSQL, ALB, Terraform với S3
> backend + DynamoDB state locking — đã xác nhận ở [05](05-he-thong-that-allinone-aws.md)), nên
> phần lớn là **thêm vào**, không phải thay nền.

### 13.1 Sơ đồ tổng

```text
        (1)                    (2)                      (3)
   ┌──────────┐         ┌───────────────┐      ┌─────────────────────┐
   │   ALB    │────────►│ ECS Fargate   │─────►│ Kinesis Data Stream │
   │ (đã có)  │  HTTPS  │ command-api   │ put  │ partitionKey =      │
   └──────────┘         │ trả 202 +     │      │   accountId         │
                        │ account_seq   │      └──────────┬──────────┘
                        └───────────────┘                 │
   ┌──────────────┐                                       │ (4)
   │ EventBridge  │  cron 19:00                           ▼
   │ Scheduler    │──────────┐              ┌──────────────────────────┐
   └──────────────┘          ▼              │ ECS Fargate  ledger-     │
                    ┌─────────────────┐     │ writer (KCL)             │
              (7)   │ Lambda          │     │ 1 consumer / 1 shard     │
                    │ accrual-        │────►│ → SINGLE WRITER/account  │
                    │ commander       │ put └────────┬─────────────────┘
                    │ (chỉ PHÁT LỆNH) │              │ (5)
                    └─────────────────┘              ▼
                                          ┌──────────────────────────┐
                             (6)          │ Aurora PostgreSQL        │
                    ┌──────────────┐      │ ledger_entry (partition) │
                    │ DynamoDB     │◄─────┤ balance_snapshot, outbox │
                    │ KCL leases   │ KCL  └────────┬─────────────────┘
                    └──────────────┘               │
                                                   │ (8) outbox poller
        (9)                                        ▼
   ┌──────────────┐   ┌───────────────┐   ┌──────────────────┐
   │ ElastiCache  │◄──┤ ECS query-api │   │ Lambda publisher │
   │ Redis        │   │ + Aurora      │   │ → API GW         │
   │ (snapshot)   │   │   read replica│   │   WebSocket      │
   └──────────────┘   └───────────────┘   └──────────────────┘

        (10)  EventBridge cron → ECS task reconcile → CloudWatch alarm khi drift ≠ 0
        (11)  Partition cũ → S3 Parquet → Glue Catalog → Athena
```

### 13.2 Từng bước

#### Bước 1 — Stream có thứ tự (thành phần MỚI quan trọng nhất)

```hcl
resource "aws_kinesis_stream" "ledger_commands" {
  name             = "ledger-commands"
  retention_period = 168                      # 7 ngày, đủ để replay khi writer lỗi
  encryption_type  = "KMS"
  kms_key_id       = "alias/aws/kinesis"

  stream_mode_details {
    stream_mode = "ON_DEMAND"                 # tự co giãn shard; KHÔNG khai shard_count ở mode này
  }
}
```

**Producer phải đặt `partitionKey = accountId`** — đây là dòng code quyết định toàn bộ tính đúng
đắn của thiết kế:

```java
kinesis.putRecord(PutRecordRequest.builder()
    .streamName("ledger-commands")
    .partitionKey(String.valueOf(command.accountId()))   // ◄ MỌI lệnh của 1 account vào CÙNG shard
    .data(SdkBytes.fromUtf8String(json))
    .build());
```

> ⚠️ **Bẫy khi resharding:** ở chế độ `ON_DEMAND`, Kinesis tự tách/gộp shard khi tải đổi. Trong lúc
> đó, key có thể chuyển sang shard con. KCL bảo đảm **xử lý hết shard cha trước khi mở shard con**
> nên thứ tự **trong một key** vẫn giữ — nhưng phải dùng KCL, **không** tự viết `GetRecords`.

#### Bước 2 — Bảng lease cho KCL *(thiếu bước này là writer không đơn nhất)*

```hcl
resource "aws_dynamodb_table" "ledger_writer_leases" {
  name         = "ledger-writer-leases"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "leaseKey"
  attribute { name = "leaseKey"  type = "S" }
}
```

**Đây là bước hay bị quên nhất.** KCL dùng bảng này để giữ *lease*: mỗi shard chỉ **một** worker
được giữ lease tại một thời điểm. **Chính bảng DynamoDB này — chứ không phải Kinesis — là thứ thực
thi tính chất single-writer.** Không có nó thì hai task ECS cùng đọc một shard và mọi lập luận ở
[§12.4](#124-đường-ghi-single-writer-theo-account) sụp đổ.

#### Bước 3 — Command API (ECS Fargate, tái dùng khuôn đã có)

```hcl
resource "aws_ecs_service" "command_api" {
  name            = "ledger-command-api"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.command_api.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = var.private_subnet_ids
    security_groups = [aws_security_group.command_api.id]
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.command_api.arn
    container_name   = "command-api"
    container_port   = 8080
  }
}
```

API **chỉ** validate + `putRecord` + trả `202 Accepted` kèm `command_id`. **Không chạm DB.** Nhờ vậy
nó stateless hoàn toàn và scale ngang thoải mái.

#### Bước 4 — Ledger writer (ECS Fargate + KCL)

```hcl
resource "aws_ecs_service" "ledger_writer" {
  name            = "ledger-writer"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.ledger_writer.arn
  desired_count   = 4                       # <= số shard; KCL tự chia lease
  launch_type     = "FARGATE"
  network_configuration {
    subnets         = var.private_subnet_ids
    security_groups = [aws_security_group.ledger_writer.id]
  }
  # KHÔNG gắn load_balancer: đây là consumer, không nhận HTTP
}
```

> **Chi tiết đáng nói:** `desired_count` **không được vượt số shard** — task thừa sẽ không giành
> được lease nào và chỉ ngồi không tốn tiền. Đây là điểm khác biệt so với autoscaling theo CPU của
> service HTTP: writer phải scale theo **số shard**, không theo CPU.

#### Bước 5 — Aurora PostgreSQL (đã có, chỉ thêm schema + partition)

Tái dùng module `terraform-aws-modules/rds-aurora/aws` đang dùng. Thêm job tạo partition trước:

```sql
-- chạy hằng tháng qua ECS scheduled task; tạo TRƯỚC 3 tháng để không bao giờ hụt
CREATE TABLE IF NOT EXISTS ledger_entry_2026_09
  PARTITION OF ledger_entry
  FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
```

> Nếu quên tạo partition trước thì `INSERT` sẽ lỗi `no partition of relation found for row` — và
> đây là sự cố production kinh điển vào đúng ngày 1 hằng tháng. Luôn tạo trước ít nhất 3 tháng và
> **có alarm** khi partition xa nhất còn dưới 30 ngày.

#### Bước 6 — Lịch chạy batch

```hcl
resource "aws_scheduler_schedule" "daily_accrual" {
  name                         = "daily-accrual"
  schedule_expression          = "cron(0 19 * * ? *)"
  schedule_expression_timezone = "America/Chicago"     # ◄ theo giờ NGHIỆP VỤ, không phải UTC
  flexible_time_window { mode = "OFF" }                # đúng giờ, không xê dịch

  target {
    arn      = aws_lambda_function.accrual_commander.arn
    role_arn = aws_iam_role.scheduler_invoke.arn
    retry_policy {
      maximum_retry_attempts       = 3
      maximum_event_age_in_seconds = 3600
    }
  }
}
```

**Retry ở đây an toàn** vì commander chỉ phát lệnh, và lệnh mang `idempotencyKey` — chạy lại cả job
cũng không sinh bút toán trùng. Đây là lợi ích trực tiếp của quyết định nền #5 ở
[§12.2](#122-năm-quyết-định-nền).

#### Bước 7 — Accrual commander (Lambda — **chỉ phát lệnh**)

```python
def handler(event, _ctx):
    business_date = event.get("businessDate") or yesterday_in_business_tz()
    total = 0
    for batch in eligible_account_ids(business_date):        # đọc replica, KHÔNG cần chính xác tuyệt đối
        kinesis.put_records(
            StreamName="ledger-commands",
            Records=[{
                "PartitionKey": str(aid),                     # ◄ giữ thứ tự theo account
                "Data": json.dumps({
                    "type": "ACCRUE_INTEREST",
                    "accountId": aid,
                    "businessDate": business_date,
                    "idempotencyKey": f"accrual:{aid}:{business_date}",
                }).encode(),
            } for aid in batch],
        )
        total += len(batch)
    return {"commandsEmitted": total}    # KHÔNG đọc balance. KHÔNG tính tiền. KHÔNG ghi DB.
```

Ba điều Lambda này **cố ý không làm** — và đó là toàn bộ lý do thiết kế C hết race:

| Không làm | Vì sao |
|---|---|
| Không đọc balance | Không có "đọc lúc T1" thì không có TOCTOU |
| Không tính lãi | Writer tính, tại đúng vị trí của lệnh trong chuỗi |
| Không ghi DB | Không tranh chấp với giao dịch khách |

> Danh sách account "đủ điều kiện" **không cần chính xác tuyệt đối** — thừa một account thì writer
> tự bỏ qua khi thấy điều kiện không thoả. Đây là chỗ được phép đọc replica bị trễ, và nói ra được
> điều đó cho thấy bạn phân biệt được chỗ nào cần chặt, chỗ nào được lỏng.

#### Bước 8 — Outbox → WebSocket

```text
ledger_entry INSERT ─┐
                     ├── CÙNG 1 transaction ──► commit
outbox      INSERT ──┘
                     ▼
        Lambda poller (EventBridge mỗi 10s, hoặc DB trigger → SQS)
                     ▼
        API Gateway WebSocket ──► UI cập nhật kèm account_seq
```

Ghi outbox **trong cùng transaction** với ledger. Bắn thẳng từ code service sẽ có lúc transaction
rollback mà sự kiện đã bay đi → UI hiện số dư của một giao dịch **không tồn tại**.

#### Bước 9 — Đường đọc

```hcl
resource "aws_elasticache_replication_group" "balance_cache" {
  replication_group_id = "balance-snapshot-cache"
  engine               = "redis"
  node_type            = "cache.t4g.small"
  num_cache_clusters   = 2
  automatic_failover_enabled = true
  transit_encryption_enabled = true
}
```

Cache key **phải chứa `account_seq`**, không dùng TTL mù:

```text
balance:{accountId}:{ledgerAccount}:{account_seq}
                                     └── seq đổi ⇒ key đổi ⇒ miss tự nhiên ⇒ không bao giờ stale
```

Query API đọc Aurora **read replica**; nếu client gửi `min_seq` mà replica chưa bắt kịp thì đọc lại
từ primary — [§7.3](#73-read-your-writes-khách-vừa-trả-tiền-phải-thấy-ngay).

#### Bước 10 — Đối soát tự động

```hcl
resource "aws_cloudwatch_metric_alarm" "ledger_drift" {
  alarm_name          = "ledger-snapshot-drift"
  namespace           = "Ledger"
  metric_name         = "SnapshotDrift"
  statistic           = "Maximum"
  period              = 3600
  evaluation_periods  = 1
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"       # drift PHẢI bằng 0
  alarm_actions       = [aws_sns_topic.oncall.arn]
  treat_missing_data  = "breaching"                  # ◄ job không chạy = cũng là sự cố
}
```

`treat_missing_data = "breaching"` là chi tiết nhỏ nhưng quan trọng: nếu job đối soát **không chạy**,
alarm vẫn kêu. Mặc định (`missing`) sẽ im lặng — và một job giám sát chết âm thầm còn tệ hơn không
có job nào.

#### Bước 11 — Tầng lưu trữ lạnh

```text
partition > 2 năm  ──► DETACH PARTITION ──► export Parquet ──► S3 (Glacier IR)
                                                                  │
                                              Glue Catalog ◄──────┘
                                                    ▼
                                              Athena (SQL cho audit/kiểm toán)
```

Ledger nóng giữ kích thước ổn định; dữ liệu lạnh vẫn truy vấn được bằng SQL khi kiểm toán hỏi.

### 13.3 Vì sao Kinesis chứ không phải SQS hay MSK

| | **SQS Standard** | **SQS FIFO** | **Kinesis** ✅ | **MSK (Kafka)** |
|---|---|---|---|---|
| Giữ thứ tự theo account | ❌ không | ✅ theo `MessageGroupId` | ✅ theo partition key | ✅ theo key |
| Nhiều consumer độc lập cùng luồng | ❌ (đọc là mất) | ❌ | ✅ | ✅ |
| Replay lịch sử | ❌ | ❌ | ✅ tới 365 ngày | ✅ tuỳ cấu hình |
| Đảm bảo **một** consumer/partition | ❌ | 🟡 theo group | ✅ **qua KCL lease** | ✅ consumer group |
| Gánh nặng vận hành | Thấp nhất | Thấp | **Thấp** | Cao (broker, ZK/KRaft, patch) |
| Throughput/giới hạn | Rất cao | 300 msg/s/group | 1 MB/s ghi mỗi shard | Rất cao |

**Chọn Kinesis** vì nó là điểm cân bằng duy nhất cho bài này: có thứ tự theo key, có replay, có
single-consumer-per-shard qua KCL, mà **không phải vận hành cụm Kafka**.

> **SQS FIFO là ứng viên gần nhất** và rẻ hơn — nhưng giới hạn 300 message/giây cho mỗi
> `MessageGroupId`, và **không replay được**. Với sổ cái tiền, khả năng replay để dựng lại khi
> writer có bug là thứ tôi không muốn bỏ.
>
> **Chuyển sang MSK khi:** cần giữ > 365 ngày, hoặc đã có hệ sinh thái Kafka (Connect, Streams),
> hoặc có nhiều team consumer độc lập. Không phải vì throughput.

### 13.4 Ước lượng chi phí

⚠️ **Bậc độ lớn, chưa dùng AWS Pricing Calculator** — xem [§11](#11-ranh-giới-trung-thực).

| Thành phần | Cấu hình | Ghi chú chi phí |
|---|---|---|
| Kinesis on-demand | ~85M record/năm | Tính theo record ghi + GB; ở tải này là **hạng mục nhỏ** |
| DynamoDB leases | PAY_PER_REQUEST, vài chục item | **Gần như miễn phí** |
| ECS Fargate writer | 4 task nhỏ chạy 24/7 | **Hạng mục lớn nhất** trong phần thêm mới |
| Lambda commander | 1 lần/ngày | Không đáng kể |
| ElastiCache | 2 × cache.t4g.small | Trung bình |
| Aurora | **đã có** | Tăng thêm do ledger phình — giảm bằng partition + tầng lạnh |
| S3 Glacier IR + Athena | Lưu lạnh + query theo lượt | Rẻ nhất tính trên GB |

**Đòn bẩy chi phí lớn nhất không nằm ở hạ tầng** mà ở quyết định eager/lazy accrual
([§12.5](#125-batch-tính-lãi-thiết-kế-thế-nào)): giảm ~82% số posting sinh ra sẽ giảm chi phí Aurora
và Kinesis nhiều hơn mọi việc tinh chỉnh instance.

---

## 14. Lộ trình chuyển đổi — không big-bang

Không ai được phép "viết lại hệ thống sổ cái" trong một lần triển khai. Bốn giai đoạn:

| GĐ | Làm gì | Rủi ro | Bỏ được khi |
|---|---|:---:|---|
| **1. Vá trước** | Toàn bộ [§6](#6-khuyến-nghị-cụ-thể-cho-all-in-one-v2): idempotency → thu hẹp phạm vi → `FOR UPDATE` → job đối soát | Thấp | Ngay — sửa được lỗi thật đang chảy máu |
| **2. Ghi song song** | Đường ghi mới (stream + writer) chạy **song song**, ghi vào bảng ledger **mới**. Hệ thống cũ vẫn là nguồn sự thật | Thấp | Job so sánh hai bên **khớp 100% trong 30 ngày liên tục** |
| **3. Đổi đường đọc** | Query API đọc từ ledger mới; ghi vẫn song song để còn quay lui | Trung bình | Không có lệch, p99 đọc đạt mục tiêu |
| **4. Dừng đường cũ** | Tắt ghi cũ, xoá `current_balance` | Cao | Sau giai đoạn 3 ổn định ≥ 1 kỳ chốt sổ |

**Cổng kiểm soát của giai đoạn 2 — đây là phần đáng nói nhất:**

```sql
-- chạy hằng giờ trong suốt giai đoạn 2; PHẢI trả về rỗng trước khi được sang GĐ 3
SELECT account_id, old_balance, new_balance, old_balance - new_balance AS drift
FROM   v_old_balance  o
FULL JOIN v_new_ledger_balance n USING (account_id)
WHERE  o.old_balance IS DISTINCT FROM n.new_balance;
```

> Đây là **shadow comparison**: hệ thống mới chạy thật, chịu tải thật, nhưng **chưa ai tin nó** cho
> tới khi nó tự chứng minh trong 30 ngày. Không có bước này thì "viết lại" chỉ là đổi một tập lỗi
> đã biết lấy một tập lỗi chưa biết.

---

## 15. Khi nào **không** nên làm bản này

Phần quan trọng nhất khi trình bày — nó cho thấy bạn cân được chi phí, không phải mê kiến trúc.

| Tình huống | Nên làm gì thay thế |
|---|---|
| Tải hiện tại thấp, lỗi hiếm | **Chỉ làm [§6](#6-khuyến-nghị-cụ-thể-cho-all-in-one-v2).** `FOR UPDATE` + idempotency + đối soát đã diệt lỗi thật, với một phần nhỏ chi phí |
| Team chưa từng vận hành hệ bất đồng bộ | Bản triệt để thêm Kinesis, KCL, lease, replay, thứ tự — **đổi một lớp lỗi lấy một lớp lỗi khác**, khó hơn |
| Nghiệp vụ cần biết kết quả **ngay** trong response | Ghi bất đồng bộ trả `202` là thay đổi hợp đồng API, phải có sự đồng ý của phía nghiệp vụ |
| Không có ngân sách cho 30 ngày chạy song song | Không có shadow comparison thì đừng chuyển. Chuyển mù nguy hiểm hơn ở nguyên |

> **Câu nên nói:** *"Tôi sẽ không đề xuất viết lại để chữa lỗi này — bản vá ở §6 đủ diệt nó. Bản
> thiết kế lại chỉ đáng làm nếu có thêm lý do độc lập: cần audit trail đầy đủ, cần replay để dựng
> lại khi có bug, hoặc ghi đã chạm trần một node Aurora. Nếu chỉ để hết race thì đó là dùng dao mổ
> trâu."*

---

## 11. Ranh giới trung thực

| Điều | Trạng thái | Được nói gì |
|---|---|---|
| 7 phát hiện ở [§2](#2-bằng-chứng-trong-code-thật--7-phát-hiện) | **Đã grep/đọc trực tiếp**, có file:line | ✅ Nói tự tin, dẫn được file và dòng |
| `0 @Version / 0 LockModeType / 0 FOR UPDATE` | Đã đếm trên 7.615 file `.java`, loại `target/` | ✅ Con số thật |
| Interleaving ở [§3](#3-dựng-lại-đúng-interleaving-gây-sai) | **Dựng lại từ cấu trúc code**, không phải trace log production | 🟡 Nói *"đây là interleaving giải thích được triệu chứng"*, đừng nói *"tôi bắt được log đúng chuỗi này"* |
| Số 10.000 / 3.000 / 0,05% | **Số minh hoạ**, không phải dữ liệu thật | 🟡 Nói rõ là ví dụ |
| Mức độ lỗi trên production (bao nhiêu account, sai bao nhiêu tiền) | **Chưa đo** | 🔴 Chạy query đối soát ở [§6 bước 5](#bước-5--job-đối-soát-thứ-chứng-minh-đã-sửa) rồi mới có số. Đừng ước lượng |
| Các bản sửa ở [§6](#6-khuyến-nghị-cụ-thể-cho-all-in-one-v2) | **Đề xuất, chưa triển khai** | 🔴 Nói *"đây là kế hoạch sửa của tôi"*, **không** nói *"tôi đã sửa và hết lỗi"* |
| Test ở [§9](#9-chứng-minh-đã-sửa--3-loại-test) | **Chưa viết, chưa chạy** | 🔴 Cùng nguyên tắc |
| `desired_count ≥ 2` (lý do `synchronized` vô dụng) | ✅ Đã xác nhận bằng Terraform thật | ✅ Xem [05](05-he-thong-that-allinone-aws.md) |
| Thứ tự nghiệp vụ payment-trước-hay-accrual-trước | **Chưa hỏi kế toán** | 🟡 Nêu như câu **cần hỏi**, không tự quyết — xem [§8](#8-batch-và-online-không-đánh-nhau) |
| **— Phần II (§12-§15) —** | | |
| Lập luận "đổi định nghĩa xoá được race" ([§12.1](#121-một-thay-đổi-định-nghĩa-xoá-được-phần-lớn-bài-toán)) | Suy luận kiến trúc, **chưa hiện thực** | ✅ Lập luận vững, nói được tự tin — nhưng nói rõ là **đề xuất thiết kế**, không phải thứ đã chạy |
| Định nghĩa lãi theo `effective_date` | **Phải được kế toán duyệt** — đây là quy định, không phải lựa chọn kỹ thuật | 🔴 Không được tự quyết. Nêu như phương án cần xác nhận |
| Eager vs lazy accrual, "giảm ~82% posting" | **Tính từ giả định 100k account**, chưa đo số thật | 🟡 Nói rõ là ước lượng từ giả định, và nói luôn sẽ hỏi con số thật |
| Ước lượng 85M posting/năm, ~10 GB/năm | Cùng giả định trên | 🟡 Bậc độ lớn |
| Kiến trúc AWS §13, Terraform | **Chưa `terraform plan`, chưa deploy** | 🔴 Nói *"tôi sẽ dựng thế này"*, **không** nói *"tôi đã dựng"* |
| KCL lease qua DynamoDB đảm bảo single-writer | Cơ chế chuẩn của KCL, **chưa tự kiểm chứng** | 🟡 Là hiểu biết về công cụ, không phải đo đạc của bạn |
| Kinesis giữ thứ tự per-key khi resharding | Hành vi tài liệu hoá của KCL, **chưa tự thử** | 🟡 Nếu bị đào sâu, nói thẳng là chưa tự kiểm |
| Bảng chi phí §13.4 | **Chưa dùng AWS Pricing Calculator** | 🔴 Chỉ nói thứ tự ưu tiên tương đối, **đừng nói con số tiền** |
| Ngưỡng "~1000 ghi/giây cho một account" | **Ước lượng bậc độ lớn**, không phải benchmark | 🟡 Nói kèm *"tôi sẽ đo trước khi chốt"* |

> **Câu an toàn nhất khi được hỏi "anh sửa xong chưa":**
> *"Tôi đã truy ra nguyên nhân và có kế hoạch sửa theo thứ tự rủi ro. Việc đầu tiên tôi làm không
> phải là sửa mà là viết query đối soát để đo lỗi đang lớn tới đâu — vì tôi không muốn nói 'đã sửa'
> mà không có con số trước và sau."*

---

## Liên quan

| Tài liệu | Liên quan chỗ nào |
|---|---|
| [README — bản đồ họ bài](README.md) | Họ G và 4 trục nhận diện |
| [04 — Event counting 10k/phút](04-event-counting-10k.md) | Cùng nguyên tắc *"đừng scan raw"* và *"reconcile để đo, không để tin"*; **khác** ở chỗ bài này cần chính xác tuyệt đối nên không dùng được cache TTL mù |
| [05 — Hệ thống thật all-in-one trên AWS](05-he-thong-that-allinone-aws.md) | Xác nhận ECS chạy nhiều instance → lý do `synchronized` vô dụng |
| [../katalon-prep-java/06-distributed-resilience/](../katalon-prep-java/06-distributed-resilience/) | Chuỗi at-least-once → idempotent, retry + jitter, DLQ — code chạy được |
| [../katalon-prep-java/05-postgres-depth/](../katalon-prep-java/05-postgres-depth/) | Isolation level, index, `EXPLAIN` — nền cho §4 và §7 |
| [../AI-STACK-INTERVIEW-ANSWERS.md](../AI-STACK-INTERVIEW-ANSWERS.md) | Mẫu *"biến bất biến kiến trúc thành một cái test"* dùng lại ở §9.3 |
