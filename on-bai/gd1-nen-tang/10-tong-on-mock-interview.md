# Bài 10 — Tổng ôn: ba đề mock 45 phút, một buổi đào sâu, bảng tự chấm

> **Dùng cho:** 3 ngày cuối trước phỏng vấn **Katalon** hoặc **NAB**. Bấm giờ thật, nói thành tiếng, ghi âm.
> Mỗi đề ghép 4–6 bài trước đó; đáp án ở đây là **khung**, chi tiết nằm trong bài được dẫn.
>
> **Thời lượng:** mock 135 phút (ba đề, mỗi đề 45′) · đào sâu 30 phút · câu hỏi nhanh 20 phút

---

## 0. Cách tự mock cho có ích

1. Đọc đề, **bấm 45 phút**, nói to như có người nghe. Vẽ trên giấy hoặc excalidraw.
2. Hết giờ thì mở khung đáp án, chấm theo bảng §5. **Ghi ra ba chỗ hổng**, mỗi chỗ trỏ về một mục cụ thể của một
   bài.
3. Ngày hôm sau làm lại đúng ba chỗ đó (drill của bài tương ứng), không làm lại cả đề.
4. Nghe lại ghi âm: đếm số lần nói "đánh đổi là…". Dưới 5 lần trong 45 phút thì đang nói như mid-level.

---

## 1. Mock A (NAB): ví điện tử P2P có số dư real-time

**Đề:** *"Thiết kế ví điện tử cho 2 triệu khách: nạp tiền từ ngân hàng liên kết, chuyển P2P, xem số dư và lịch
sử, nhận thông báo khi có tiền vào. Không được mất tiền, không trừ hai lần."*

<details>
<summary><b>Khung đáp án (mở sau khi làm xong)</b></summary>

| Phút | Nội dung cần có | Bài |
|---|---|---|
| 0–5 | Hỏi: số dư được âm không (không); hạn mức; có hold/pending không; tiền tệ; ai là nguồn sự thật cho tiền nạp (ngân hàng, qua webhook) | [00](00-cach-dan-45-phut.md) |
| 5–10 | ~8 TPS trung bình, đỉnh ~40; posting ~6 GB/tháng. **Kết luận: bài đúng đắn, không phải bài scale** | [00 §3](00-cach-dan-45-phut.md#3-ước-lượng-con-số-để-ra-quyết-định-không-để-khoe) |
| 10–15 | `POST /transfers` + `Idempotency-Key`, mã lỗi 201/409/422/402; bảng `account`/`journal`/`posting` | [02](02-idempotency-key.md), [05 bước 1](05-ledger-va-balance.md#bước-1--schema-ba-bảng-mỗi-cột-có-lý-do) |
| 15–30 | Đường ghi: một transaction, `UPDATE … WHERE balance + ? >= 0` theo id tăng dần. Đường đọc: O(1) từ dòng account, replica + token seq. Nạp tiền: webhook ngân hàng có key tự nhiên (`bank_txn_id`). Thông báo: outbox → Kafka → notification service | [05](05-ledger-va-balance.md), [09](09-outbox-notification.md) |
| 30–40 | Deep dive interviewer hay chọn: **hai lệnh chuyển đồng thời** (EvalPlanQual, deadlock), hoặc **webhook đến hai lần** (idempotency) | [05 bước 4](05-ledger-va-balance.md#bước-4--post-giành-key-rồi-khoá--kiểm-tra--cấp-seq-trong-một-câu), [04](04-postgres-concurrency.md) |
| 40–45 | Ví merchant nóng (account nóng, tách ví con); partition `posting` theo tháng; chuyển liên ngân hàng là saga (giai đoạn 2); đối soát hằng đêm | [05 bước 10–11](05-ledger-va-balance.md#bước-10--tài-khoản-nóng-không-khoá-chốt-theo-mốc-xmin) |

**Ba câu bắt buộc phải nói:** "số dư là projection, cột `balance` cập nhật cùng transaction";
"kiểm tra và trừ trong một câu `UPDATE`"; "đối soát năm truy vấn phải ra rỗng".

**Bẫy hay mắc:** cache số dư rồi quyết định trên cache; dùng `double`; `SELECT` số dư rồi `if` rồi `UPDATE`; gọi
API ngân hàng trong transaction.
</details>

---

## 2. Mock B (Katalon): nhận kết quả test từ CI, dashboard, thông báo

**Đề:** *"Hàng nghìn tenant chạy test trên CI gửi kết quả về nền tảng (mỗi lần chạy vài trăm tới vài nghìn test
case). Cần: API nhận kết quả không mất, dashboard tỉ lệ pass/fail theo khoảng thời gian bất kỳ, thông báo Slack
khi một run thất bại. Một tenant chạy CI lỗi bắn 50.000 request/phút không được làm chậm tenant khác."*

<details>
<summary><b>Khung đáp án</b></summary>

| Phút | Nội dung | Bài |
|---|---|---|
| 0–5 | Đơn vị tải thật: **test case**, không phải request (một request mang 500 case); chính xác tuyệt đối hay xấp xỉ (đếm: tuyệt đối); multi-tenant isolation | [Katalon README §6](../../katalon-prep/katalon-system-design/README.md#6-khung-45-phút) |
| 5–10 | Ví dụ 10k request/phút × 500 case = 83k case/s đỉnh; lưu raw theo ngày | [doc 04](../../katalon-prep/katalon-system-design/04-event-counting-10k.md) |
| 10–15 | `POST /runs/{runId}/results` theo lô, trả `202`; `event_id` tất định = khoá idempotency; `429` + `Retry-After` theo tenant | [02](02-idempotency-key.md), [03](03-rate-limiter.md) |
| 15–30 | Ingest → raw partition theo ngày (`ON CONFLICT DO NOTHING`) → rollup phút/giờ/ngày → query tách khoảng. Rate limit tenant bằng Lua token bucket. Run kết thúc thì ghi outbox → notifier Slack (consumer idempotent) | [doc 04](../../katalon-prep/katalon-system-design/04-event-counting-10k.md), [03](03-rate-limiter.md), [09](09-outbox-notification.md) |
| 30–40 | Deep dive: **CI retry gửi lại cả lô** (dedupe bằng `event_id`, đếm không trùng); **tenant ồn ào** (quota + hàng đợi có trọng số, không phải chỉ rate limit) | [03 Q4](03-rate-limiter.md#6-follow-up), doc 02 họ B |
| 40–45 | 10× thì gì vỡ trước: ghi raw (partition, COPY theo lô), rollup trễ; 100× thì đổi kiến trúc (Kafka + stream processing) | [doc 07](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md) |

**Câu ăn điểm:** *"Rate limit giải bài 'quá nhanh', không giải bài 'chiếm phần của người khác'. Với tenant ồn
ào tôi cần thêm quota đồng thời và hàng đợi có trọng số."*

**Bẫy:** đếm bằng `UPDATE counter = counter + n` đồng bộ (hot row, lost update nếu sai cách); để dashboard đọc
raw mỗi lần; gửi Slack ngay trong request ingest.
</details>

---

## 3. Mock C (cả hai): URL shortener có thống kê, đề "khởi động"

**Đề:** *"100 triệu link mới/tháng, đọc/ghi 100:1, giữ 5 năm, thống kê click theo ngày cho chủ link."*

<details>
<summary><b>Khung đáp án</b></summary>

- Ước lượng: ghi 40/s, đọc 4.000/s (đỉnh 10.000), 3 TB/5 năm, 7 ký tự base62 dư ~600 lần.
  ([07 §1.3](07-id-generator-url-shortener.md#13-ước-lượng-cho-đề-url-shortener-lộ-trình-v4))
- Mã: sequence → hoán vị `(A·n + B) mod 62^7` → base62. Không trùng, không đọc kiểm tra, không đoán được bằng +1.
  ([07 bước 4](07-id-generator-url-shortener.md#bước-4--mã-7-ký-tự-sequence-qua-một-hoán-vị))
- Alias: không gian tên riêng. Redirect 302. Cache-aside + single-flight + tombstone khi xoá link.
  ([08](08-cache-consistency.md))
- Click: sự kiện vào hàng đợi, gộp theo phút. **Không** update đồng bộ.
- 10×: cache thiếu RAM cho tập nóng; ghi click nếu đồng bộ. 100×: shard bảng link theo hash của mã.

**Dùng đề này để luyện tốc độ:** làm trong 30 phút thay vì 45. Phần thừa dành cho câu "nếu ×10".
</details>

---

## 4. Buổi đào sâu (30 phút): câu chuyện ledger, interviewer hỏi dồn

Kịch bản dưới đây là cách một interviewer giỏi đào câu chuyện [bài 06](06-race-tinh-lai-vs-thanh-toan.md). Che
cột phải, trả lời từng câu trong **≤ 45 giây**, rồi so.

| # | Interviewer hỏi | Ý chính phải có |
|:-:|---|---|
| 1 | "Kể tôi nghe một bug concurrency khó nhất anh từng sửa." | STAR 90 giây có ba con số ([bài 06 §5](06-race-tinh-lai-vs-thanh-toan.md#5-kể-chuyện-star-90-giây-bản-có-số)) |
| 2 | "Vì sao gọi là write skew, không phải lost update?" | Hai bên ghi hai dòng khác nhau; bất biến nghiệp vụ, DB không biết |
| 3 | "Code có `synchronized` mà?" | JVM-local, ≥ 2 task ECS, hai service; khoá nhầm thứ đã an toàn |
| 4 | "Thêm `@Transactional` không đủ à?" | RC: snapshot theo câu lệnh, không khoá khi đọc. Lab: seq 1 lúc tính, seq 2 lúc commit |
| 5 | "Vậy `SERIALIZABLE`?" | Bắt được ghi đè (`40001`); với lỗi A thì thứ tự "accrual trước" vẫn hợp lệ; mọi bên phải SER; cần retry + idempotency |
| 6 | "Sửa theo thứ tự nào, vì sao idempotency trước?" | Mọi cách đều cần retry; retry không idempotent thì tính lãi hai lần (lab: lãi 10 thay vì 5) |
| 7 | "Khoá cái gì, theo thứ tự nào?" | Dòng account gốc + lãi, `ORDER BY id FOR UPDATE`, khớp quy ước của đường ghi |
| 8 | "Batch khoá thì khách phải chờ à?" | `SKIP LOCKED` theo chunk; vòng sau bắt kịp; đếm số lần skip, cảnh báo |
| 9 | "Anh test race thế nào?" | Seam chèn interleaving tất định + `CyclicBarrier` 20 vòng + bằng chứng `pg_stat_activity`; Postgres thật, không H2 |
| 10 | "Test `isIn(5,00; 3,50)` có đủ không?" | Không: bản lỗi cũng pass. Cần `basis_seq` hoặc kiểm toán theo định nghĩa |
| 11 | "Thiết kế lại thì sao?" | Đổi định nghĩa: lãi ngày D = f(posting effective ≤ D) + giờ chốt sổ; ghi muộn thành điều chỉnh |
| 12 | "Hai bản điều chỉnh chạy song song?" | Khoá + key `accrual-adj:{loan}:{day}:{basis_seq}`; lab: 8 bản cho đúng 1 điều chỉnh |
| 13 | "Đi xa hơn nữa?" | Single-writer theo account (Kafka/Kinesis key = loanId), API trả 202; điều kiện kích hoạt |
| 14 | "Cột `current_balance` xử lý sao?" | Bỏ, hoặc chỉ cập nhật tương đối trong cùng transaction + đối soát drift |
| 15 | "Làm sao biết đã sửa xong trên production?" | Baseline drift + mismatch trước deploy; theo dõi về 0 |
| 16 | "Batch 1 triệu khoản vay chạy bao lâu?" | ~1,5 ms/khoản vay, chunk 500, 8 luồng → 3–4 phút; giới hạn ở WAL và pool |
| 17 | "Account hệ thống như `cash` có khoá không?" | Không (nóng); số dư tổng hợp theo mốc xmin; mốc theo id thì sót ([bài 05 bước 10](05-ledger-va-balance.md#bước-10--tài-khoản-nóng-không-khoá-chốt-theo-mốc-xmin)) |
| 18 | "Khách vừa trả tiền, refresh thấy số cũ?" | Read-your-writes: token seq, replica chưa bắt kịp thì đọc primary |
| 19 | "Làm tròn lãi kiểu gì?" | `HALF_EVEN`, scale 2 khi ghi sổ, tính trên `numeric`; hỏi kế toán quy tắc chính thức |
| 20 | "Nếu được làm lại, anh làm khác gì từ đầu?" | Append-only + định nghĩa theo ngày nghiệp vụ + idempotency key tự nhiên ngay từ ngày đầu; test kiến trúc chặn ghi sổ không khoá |

---

## 5. Bảng tự chấm (0–2 điểm mỗi dòng)

| Tiêu chí | 0 | 1 | 2 |
|---|---|---|---|
| Làm rõ yêu cầu | Lao vào vẽ | Hỏi chức năng | Hỏi bất biến, ai ghi cùng lúc, độ chính xác |
| Ước lượng | Không có | Có số | Số dẫn tới **một quyết định** |
| API + mô hình | Thiếu | Đủ endpoint/bảng | Có mã lỗi, idempotency, nguồn sự thật vs dẫn xuất |
| Kiến trúc | Hộp rời rạc | Đủ luồng | Ba đường ghi/đọc/đối soát, số trên mũi tên |
| Đúng đắn khi đồng thời | Không nhắc | Nhắc "dùng lock" | Chỉ đúng chỗ race, cơ chế cụ thể, thứ tự khoá |
| Thất bại | Không nhắc | Retry | Phân loại lỗi, timeout theo tầng, DLQ, đối soát |
| Scale | "Thêm server" | Cache/replica | Biết cái gì vỡ trước khi ×10, kèm điều kiện kích hoạt |
| Trade-off | Không có | Có vài câu | Mỗi lựa chọn một câu "đánh đổi là…" |
| Code tại chỗ | Không viết được | Viết, thiếu điểm then chốt | Viết đúng + nói test + nói giới hạn |
| Giao tiếp | Độc thoại | Có hỏi lại | Dẫn dắt, kiểm tra interviewer muốn đào đâu |

**≥ 16/20:** sẵn sàng level senior. **12–15:** ôn lại ba dòng thấp nhất. **< 12:** làm lại bài 00 và các drill.

---

## 6. Câu hỏi nhanh (trả lời ≤ 15 giây mỗi câu)

1. Isolation mặc định của Postgres? Nó chặn gì, không chặn gì?
2. Một câu SQL chống rút quá số dư?
3. Vì sao khoá theo id tăng dần?
4. `40001` và `40P01` khác nhau thế nào, xử lý ra sao?
5. Hibernate 6 sinh gì cho `PESSIMISTIC_WRITE` trên Postgres?
6. Vì sao `@Transactional` gọi qua `this` không chạy?
7. `@Modifying` cần hai cờ nào, vì sao phải tự tăng version?
8. Ba loại lỗi khi gọi PSP và cách xử lý idempotency key cho từng loại?
9. Fencing token giải bài gì?
10. Token bucket: hai tham số, nghĩa là gì?
11. Vì sao fixed window cho qua gấp đôi?
12. Vì sao `INCR` rồi `EXPIRE` nguy hiểm?
13. `L = λW`: pool 10, giữ 50 ms, trần bao nhiêu?
14. Vì sao p99 nổ khi utilization > 90%?
15. Công thức pool tránh deadlock khi có `REQUIRES_NEW`?
16. Virtual thread có tăng sức chứa DB không?
17. Snowflake: ba điều kiện để không trùng?
18. Vì sao hoán vị `(A·n+B) mod N` không cần kiểm tra trùng?
19. Race "reader chậm" của cache là gì, sửa thế nào?
20. Dual write hỏng theo hai kiểu nào?
21. Vì sao relay outbox không dùng `id > lastId`?
22. `SKIP LOCKED` làm mất gì?
23. Số dư chạy (`balance_after`) để làm gì?
24. Vì sao snapshot chốt theo seq không cần khoá?
25. Mốc xmin là gì, cái giá của nó?

<details>
<summary><b>Đáp án gọn</b></summary>

1. RC. Chặn dirty read; không chặn lost update đọc-sửa-ghi, write skew, non-repeatable read. ([04 §1](04-postgres-concurrency.md#1-lý-thuyết-cốt-lõi-mvcc-đủ-để-suy-luận))
2. `UPDATE account SET balance = balance - ? WHERE id = ? AND balance >= ?`, kiểm rowcount.
3. Mọi transaction khoá cùng thứ tự thì không có vòng chờ, nên không deadlock.
4. `40001`: serialization failure, retry cả transaction (cần idempotent). `40P01`: deadlock, cũng retry, nhưng chữa gốc bằng thứ tự khoá.
5. `FOR NO KEY UPDATE`.
6. Proxy không bị đi qua; annotation không có tác dụng.
7. `flushAutomatically`, `clearAutomatically`; bulk update bỏ qua `@Version` và persistence context.
8. Chắc chắn chưa xảy ra thì trả key; không biết (timeout) thì giữ, retry sau lease cùng key; kết quả nghiệp vụ thì lưu, replay.
9. Chủ cũ hết lease mà vẫn chạy: chỉ chủ hiện tại (token khớp) mới chốt được.
10. Dung lượng = burst tối đa; tốc độ nạp = trung bình dài hạn.
11. Limit cuối cửa sổ này + limit đầu cửa sổ sau rơi trong vài ms.
12. Chết giữa hai lệnh thì key không TTL, bị chặn vĩnh viễn. Dùng `SET NX PX` hoặc Lua.
13. 10 / 0,05 s = 200 req/s.
14. Chờ tỉ lệ ρ/(1−ρ); lab: chờ p99 2,6 ms ở 0,5 thành 241 ms ở 0,99.
15. Tn × (Cm − 1) + 1.
16. Không. Chờ rẻ hơn, nhưng 10 connection vẫn là 10.
17. Worker id duy nhất; đồng hồ không lùi xa; xử lý tràn seq (chờ ms sau).
18. Song ánh trên [0, N) khi gcd(A, N) = 1.
19. Reader đọc DB bản cũ, writer cập nhật + xoá, reader ghi bản cũ vào cache. Sửa bằng version + tombstone (hoặc lease).
20. Publish rồi rollback thì sự kiện ma; commit rồi publish lỗi thì mất sự kiện.
21. Id cấp theo thứ tự insert, dòng hiện ra theo thứ tự commit; commit muộn bị sót.
22. Thứ tự giữa các dòng (các aggregate).
23. Sao kê, số dư lịch sử theo seq O(1), replay idempotent đúng response cũ, đối soát chuỗi.
24. Seq cấp dưới khoá dòng account tới commit, nên các seq đã commit luôn là dãy liên tục.
25. Xid nhỏ nhất còn chạy; dưới mốc không ai commit muộn. Giá: transaction dài ghìm mốc, phần đuôi phình.
</details>
