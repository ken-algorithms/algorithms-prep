# 04 — Kafka, event-driven, Saga: bịt gap lớn nhất

> **Vì sao file này tồn tại:** JD ghi *"distributed, event-driven systems... **Apache Kafka
> preferred**"*, và vòng Engineering Manager hỏi đích danh **microservices, Saga pattern,
> event-driven, commit log**. Đã grep toàn bộ `all-in-one-v2`: **không có Kafka, không có SQS**.
> Đây là gap thật, không phải gap tưởng tượng.
>
> **Tin tốt:** bạn đã có nền mà chưa nhận ra — sổ cái append-only và bút toán đảo trong dự án ngân
> hàng của bạn **chính là** hai khái niệm cốt lõi của mục này. Xem [§4.3](#43-bạn-đã-làm-saga-mà-chưa-gọi-tên).

---

## 1. Kafka là **commit log**, không phải hàng đợi

Từ khoá `commit log` xuất hiện trong danh sách câu hỏi vòng EM. Đó không phải từ ngẫu nhiên — nó là
cách phân biệt người **đã dùng** Kafka với người **đã đọc về** Kafka.

```text
HÀNG ĐỢI (SQS, RabbitMQ):
    [msg1][msg2][msg3]  →  consumer đọc msg1  →  msg1 BIẾN MẤT
    - Đọc là mất. Không đọc lại được.
    - Một message tới một consumer.

COMMIT LOG (Kafka):
    offset:  0     1     2     3     4
            [m0]  [m1]  [m2]  [m3]  [m4]  ← ghi thêm vào CUỐI, không bao giờ sửa
                         ▲                 consumer-group-A đang ở offset 2
                   ▲                       consumer-group-B đang ở offset 1
    - Đọc KHÔNG làm mất. Message ở lại tới hết retention.
    - Consumer tự giữ VỊ TRÍ ĐỌC (offset) của mình.
    - Nhiều group đọc CÙNG dữ liệu, độc lập, ở tốc độ khác nhau.
    - Tua lại được: reset offset về 0 là replay toàn bộ lịch sử.
```

**Câu nói ra khi được hỏi:**

> 🇬🇧 *"Kafka isn't really a queue — it's a distributed, append-only commit log. Consumers don't
> remove messages; they just advance their own offset. That's what makes replay possible, and it's
> why several independent consumer groups can read the same stream at different speeds."*

**Vì sao điều này quan trọng với ngân hàng** — và đây là chỗ nối vào thế mạnh của bạn:

```text
Sổ cái kế toán:  append-only, không sửa, sai thì ghi bút toán ngược
Kafka:           append-only, không sửa, sai thì ghi event bù

→ CÙNG MỘT TRIẾT LÝ. Kafka hợp với ngân hàng vì sổ cái vốn đã là một commit log.
```

> Nói được câu đó ở vòng EM đáng giá hơn liệt kê mười tính năng của Kafka.

---

## 2. Bốn khái niệm phải nói trôi

| Khái niệm | Là gì | Bẫy hay bị hỏi ngược |
|---|---|---|
| **Partition** | Một topic chia thành N partition. **Thứ tự chỉ được đảm bảo TRONG một partition**, không phải trong cả topic | *"Kafka có đảm bảo thứ tự không?"* → **"Trong một partition thì có, giữa các partition thì không."** Trả lời "có" là sai |
| **Partition key** | Quyết định message vào partition nào: `hash(key) % numPartitions` | Cùng key ⇒ cùng partition ⇒ **giữ được thứ tự cho key đó**. Đây là cơ chế duy nhất để có ordering |
| **Consumer group** | Trong một group, **mỗi partition chỉ có đúng một consumer** đọc | ⇒ số consumer hữu ích **không vượt quá** số partition. Thêm consumer nữa là ngồi không |
| **Offset** | Vị trí đọc, do consumer giữ và commit | **Commit trước khi xử lý** = at-most-once (mất dữ liệu). **Commit sau khi xử lý** = at-least-once (trùng) |

### Ordering — chỗ hay trả lời hớ nhất

```text
topic "transactions", 3 partition:

  key = accountId=42  →  hash(42) % 3 = 0  →  LUÔN vào partition 0
  key = accountId=99  →  hash(99) % 3 = 1  →  LUÔN vào partition 1

⇒ Mọi giao dịch của account 42 đi qua partition 0, ĐÚNG THỨ TỰ.
⇒ Giao dịch của account 42 và 99 KHÔNG có thứ tự với nhau — và không cần có.

Chọn key = accountId là chọn ĐÚNG mức granularity: đủ để có thứ tự nơi cần,
đủ phân tán để scale. Chọn key = "constant" thì 1 partition gánh hết (hot partition).
Không chọn key thì round-robin, MẤT thứ tự hoàn toàn.
```

Đây **chính là** cơ chế single-writer ở
[06 §12.4](../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md#124-đường-ghi-single-writer-theo-account) —
bạn đã viết ra rồi, chỉ là dùng Kinesis thay vì Kafka. **Khái niệm giống hệt nhau**: partition key,
một consumer cho mỗi partition, thứ tự trong key.

---

## 3. Exactly-once: câu trả lời đúng là "không cần"

Câu hỏi kinh điển: *"Làm sao đảm bảo exactly-once?"*

**Trả lời hạng xoàng:** *"Bật `enable.idempotence` và dùng transaction của Kafka."*

**Trả lời hạng Lead:**

> 🇬🇧 *"In practice I'd choose **at-least-once delivery plus an idempotent sink**, rather than
> end-to-end exactly-once. Kafka transactions only give you exactly-once **within** Kafka — the
> moment you write to an external database, that guarantee stops at the boundary. So instead of
> paying the throughput and operational cost for a guarantee that doesn't reach where I need it, I
> make the **write** idempotent: a natural idempotency key with a unique constraint, and
> `ON CONFLICT DO NOTHING`. Then a duplicate delivery is simply a no-op."*

```text
at-least-once  +  sink idempotent   =   hiệu quả tương đương exactly-once
                                        rẻ hơn, đơn giản hơn, biên rõ ràng hơn
```

**Và bạn có ví dụ thật để kể ngay:** khoá idempotency `accrual:{accountId}:{date}` — một account,
một ngày, đúng một bút toán lãi, chạy lại bao nhiêu lần cũng vậy
([06 §6 bước 1](../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md#bước-1--idempotency-trước-tiên-làm-trước-cả-việc-sửa-race)).

---

## 4. Saga pattern

### 4.1 Vấn đề Saga giải quyết

```text
Một transaction ACID chỉ sống trong MỘT database.
Microservices = nhiều database. Không có transaction chung.

Chuyển tiền A → B, hai service khác nhau:
    trừ tiền A  ✅ thành công
    cộng tiền B  ❌ thất bại
    → tiền BỐC HƠI. Không có ROLLBACK nào cứu được vì đã commit ở DB khác.
```

**Saga = chuỗi transaction cục bộ, mỗi bước có một "bước bù" (compensating transaction) để đảo lại
nếu bước sau hỏng.** Không rollback — mà **ghi thêm một hành động ngược lại**.

### 4.2 Hai kiểu — phải phân biệt được

```text
CHOREOGRAPHY (không nhạc trưởng)
   service A ──event──► service B ──event──► service C
   Mỗi service tự nghe event và tự quyết định làm gì.
   ✅ Không có điểm chết tập trung, dễ thêm bước
   ❌ Luồng nghiệp vụ KHÔNG NẰM Ở ĐÂU CẢ — muốn hiểu phải đọc hết mọi service
   ❌ Dễ thành vòng lặp event ngoài ý muốn

ORCHESTRATION (có nhạc trưởng)
                 ┌──────────────┐
                 │ Orchestrator │  giữ state machine của cả saga
                 └──┬───┬───┬───┘
                    ▼   ▼   ▼
                    A   B   C     mỗi service chỉ làm việc của nó rồi báo lại
   ✅ Luồng nghiệp vụ nằm ở MỘT chỗ, đọc được, test được
   ✅ Xử lý lỗi và bù trừ tập trung
   ❌ Thêm một thành phần phải vận hành; orchestrator có thể phình thành god service
```

**Chọn cái nào:** ≤3 bước và luồng đơn giản → choreography. Nhiều bước, nhiều nhánh lỗi, cần audit
được → **orchestration**. Với ngân hàng, gần như luôn là **orchestration**, vì phải trả lời được câu
*"giao dịch này đang ở bước nào"* khi kiểm toán hỏi.

### 4.3 Bạn **đã làm** Saga mà chưa gọi tên

Đây là chỗ biến gap thành điểm mạnh:

```java
// all-in-one-v2 — JournalEntry.java (code THẬT của bạn)
public List<Posting> buildRevertedPostings(JournalEntry newJournalEntry) {
    for (Posting p : postings) {
        revertedPostings.add(
            new Posting(p.getAmount().negate(), newJournalEntry, p.getLedgerAccount()));
        //                  └── ĐẢO DẤU, không sửa, không xoá bút toán cũ
    }
}
```

**Đó chính xác là một compensating transaction.** Bạn đã áp dụng đúng nguyên tắc cốt lõi của Saga —
*sửa sai bằng cách ghi thêm hành động ngược, không phải bằng cách xoá lịch sử* — chỉ là trong phạm
vi một service.

**Cách kể ở vòng EM:**

> 🇬🇧 *"I haven't run a distributed Saga across services in production, but I've implemented the
> core primitive it relies on. Our ledger never mutates or deletes an entry — a reversal creates a
> new entry with the negated amount. That's a compensating transaction, and it's the same reasoning
> a Saga needs: you can't roll back a committed local transaction, so you move forward with an
> action that cancels it out. Scaling that to multiple services means adding an orchestrator that
> owns the state machine and knows which compensations to fire, plus idempotency on every step so
> retries are safe."*

> **Vì sao câu này mạnh:** nó vừa **trung thực** (chưa làm distributed saga) vừa chứng minh **hiểu
> bản chất**, lại **neo vào code thật**. Mạnh hơn hẳn việc thuộc lòng định nghĩa Saga.

### 4.4 Ba thứ phải nói kèm khi bàn Saga

| Thứ | Vì sao bắt buộc |
|---|---|
| **Idempotency ở mọi bước** | Saga có retry. Retry không idempotent trên hệ thống tiền = ghi trùng |
| **Không phải hành động nào cũng bù được** | Gửi email, đẩy notification — **không rút lại được**. Phải đặt các bước không thể bù **ở cuối** saga |
| **Trạng thái trung gian nhìn thấy được** | Saga **không có isolation**: giữa chừng, người khác đọc thấy trạng thái nửa vời. Phải có `PENDING` tường minh, đừng giả vờ là atomic |

Điểm thứ ba là thứ hay bị bỏ sót nhất — và là câu hỏi ngược mà một EM giỏi sẽ hỏi.

---

## 5. Bài luyện: thiết kế chuyển tiền liên ngân hàng bằng Saga

Đây là bài design ngân hàng kinh điển và **rất khớp** với hồ sơ của bạn. Tự làm 45 phút, quay màn
hình, **nói tiếng Anh**.

```text
ĐỀ: Chuyển tiền từ tài khoản ngân hàng A sang ngân hàng B.
    Ba service: Account Service, Payment Gateway, Notification Service.
    Không có transaction phân tán. Thiết kế sao cho không bao giờ mất tiền
    và không bao giờ tạo tiền.
```

**Khung trả lời — 6 bước:**

```text
1. BẤT BIẾN trước tiên:  tổng tiền toàn hệ thống KHÔNG ĐỔI ở mọi thời điểm.
                         Mọi thiết kế phải bảo toàn nó.

2. Các bước saga:        reserve tiền ở A  →  gọi payment gateway  →  ghi có cho B
                                              →  gửi thông báo (KHÔNG BÙ ĐƯỢC → để CUỐI)

3. Bước bù:              release reservation ở A;  gateway → yêu cầu hoàn tiền
                         (lưu ý: hoàn tiền là bút toán MỚI, không phải xoá bút toán cũ)

4. Orchestration:        state machine giữ trạng thái saga; mỗi bước có timeout;
                         trạng thái tường minh: PENDING → RESERVED → SETTLED / COMPENSATED

5. Idempotency:          mọi bước có khoá tự nhiên: transfer:{transferId}:{step}
                         → gateway trả lời hai lần cũng chỉ ghi sổ một lần

6. Điều KHÔNG chắc:      gateway timeout — KHÔNG BIẾT nó đã thực hiện hay chưa.
                         → PHẢI có API tra cứu trạng thái + đối soát cuối ngày.
                         → Đây là câu trả lời phân biệt Senior với Lead.
```

**Bước 6 là điểm ăn tiền.** Người thường thiết kế cho luồng thành công và luồng thất bại. Người làm
ngân hàng thật biết trạng thái nguy hiểm nhất là **không biết** — và thiết kế cho nó.

---

## 6. Demo cần dựng (tuần 2)

Đừng chỉ đọc. Kết thúc tuần 2 phải có **code chạy được để kể**.

```text
docker-compose:  kafka + zookeeper (hoặc KRaft) + postgres

producer:        Spring Boot, POST /transactions
                 → KafkaTemplate.send("transactions", accountId, payload)
                                                       └── partition key

consumer:        @KafkaListener(topics="transactions", groupId="ledger-writer")
                 AckMode.MANUAL — commit offset SAU KHI ghi DB thành công
                 INSERT ... ON CONFLICT (idempotency_key) DO NOTHING

chứng minh:      1. gửi 2 lần cùng idempotency key  → DB chỉ có 1 dòng
                 2. kill consumer giữa chừng, bật lại → không mất, không trùng
                 3. gửi 100 giao dịch cùng accountId → thứ tự trong DB đúng y hệt thứ tự gửi
```

Ba phép chứng minh đó, mỗi cái là một câu trả lời phỏng vấn có bằng chứng.

> ⚠️ **Chưa dựng.** Đây là việc cần làm trong tuần 2 của
> [kế hoạch](03-ke-hoach-on-tap.md#tuần-2--kafka-và-event-driven-gap-lớn-nhất), không phải việc đã
> xong. Đừng nói *"tôi đã làm"* trước khi chạy thật.

---

## 7. Ranh giới trung thực

| Điều | Trạng thái | Nói gì |
|---|---|---|
| Khái niệm Kafka §1-3 | Kiến thức chuẩn, **chưa vận hành production** | ✅ Nói được tự tin, nhưng **đừng nói "tôi đã chạy Kafka ở production"** |
| Demo §6 | **Chưa dựng** | 🔴 Sau khi dựng thì được nói "tôi đã dựng một demo và đo được X" |
| `buildRevertedPostings` = compensating transaction | ✅ **Code thật, đã đọc trực tiếp** | ✅ Dẫn chứng được |
| Distributed Saga nhiều service | **Chưa làm bao giờ** | 🔴 **Nói thẳng.** Kèm lập luận ở [§4.3](#43-bạn-đã-làm-saga-mà-chưa-gọi-tên) — trung thực + hiểu bản chất mạnh hơn giả vờ |
| Bài design §5 | Bài tự luyện, **chưa làm** | 🟡 Là bài tập, không phải kinh nghiệm |

> **Câu an toàn khi bị hỏi "anh có kinh nghiệm Kafka không":**
> 🇬🇧 *"Not in production — the system I work on uses AWS SNS and a database-driven approach rather
> than Kafka. But I've built the equivalent design on Kinesis, where the reasoning is identical:
> partition by account id to get ordering where it matters, one consumer per partition, at-least-once
> with an idempotent sink. And I've built a local Kafka setup to make sure I understand the
> mechanics rather than just the vocabulary."*
>
> Trung thực về ranh giới rồi **kéo ngay về chỗ mình mạnh** — đó là cách trả lời gap đúng.
