# 05 — Vòng tiếng Anh: rủi ro lớn nhất

> **Vì sao đây là file quan trọng nhất trong `nab-prep`:** gap Kafka bịt được trong 2 tuần. Gap
> tiếng Anh thì không. Và vòng 100% English là **vòng cuối** — trượt ở đó là mất toàn bộ công sức
> của bốn vòng trước.
>
> Mọi nguồn đều xác nhận: **ít nhất một vòng 100% tiếng Anh**, thường là Engineering Manager, bàn
> **system design sâu** + behavioral. Xem [01 §4](01-nab-research.md#4-quy-trình-phỏng-vấn--hai-biến-thể-được-báo-cáo).

> ### 📌 File này **không thay thế** lộ trình tiếng Anh của bạn
>
> Bạn đã có lộ trình riêng, đầy đủ hơn nhiều, ở **`../../ielts-target-5-5/`** — 40 tuần, 4 giai
> đoạn, giáo án từng ngày, kèm [bản web đồng hành](https://claude.ai/code/artifact/9569184e-cc2d-43ce-b0e7-c4c0183af03b)
> có đồng hồ luyện, chấm Writing và theo dõi tiến độ.
>
> **Phân vai rõ ràng:**
>
> | Việc | Ở đâu |
> |---|---|
> | Xây **nền** tiếng Anh: nghe, đọc, viết, phát âm, từ vựng học thuật | **`ielts-target-5-5/`** — đó là lộ trình chính, chạy dài hạn |
> | Luyện **đúng kịch bản phỏng vấn kỹ thuật**: kể câu chuyện ledger, tranh luận trade-off, thừa nhận gap, từ vựng `idempotent`/`partition`/`compensating transaction` | **File này** — mỏng, chuyên biệt, ăn theo cái nền kia |
>
> Nói cách khác: lộ trình IELTS lo **năng lực**, file này lo **kịch bản**. Đừng lấy file này thay
> cho việc luyện nền — 12 mẫu câu ở [§3](#12-mẫu-câu-phải-thuộc-như-phản-xạ) chỉ bật ra được khi
> nền đã đủ.

---

## 1. Tiếng Anh ở NAB dùng vào việc gì — không phải "trả lời câu hỏi"

```text
Cái nhiều người tưởng:   nghe câu hỏi → dịch trong đầu → trả lời câu đã học thuộc
Cái thật sự xảy ra:      TRANH LUẬN kiến trúc, bảo vệ một lựa chọn, thừa nhận đánh đổi,
                         hỏi lại khi đề chưa rõ, đổi ý giữa chừng khi có lập luận tốt hơn
```

Đó là lý do học thuộc câu trả lời **không cứu được**. Cái cần luyện là **nói ra suy nghĩ khi đang
suy nghĩ** bằng tiếng Anh.

**Ba tình huống khó nhất, phải luyện riêng:**

| Tình huống | Vì sao khó | Mẫu câu |
|---|---|---|
| **Nêu đánh đổi** | Cần cấu trúc câu phức, không phải từ vựng khó | *"The trade-off here is X — I gain A, but I pay for it with B."* |
| **Thừa nhận không biết** | Bản năng là nói vòng vo, nghe rất tệ | *"I haven't done that in production. What I have done is... and the reasoning carries over because..."* |
| **Hỏi lại khi đề mơ hồ** | Im lặng làm rồi sẽ bị đánh giá là không clarify | *"Before I sketch anything — could you tell me whether X or Y? Because it changes the design."* |

---

## 2. Vòng HR (100% English) — dễ nhất, chuẩn bị được hết

Câu hỏi được nêu đích danh trong các nguồn: giới thiệu bản thân, **điểm mạnh/điểm yếu**, mục tiêu,
kinh nghiệm trước đây, hoạt động cuối tuần, **mục tiêu 2 năm tới**.

### Elevator pitch — 90 giây

> *"I'm a backend engineer with about N years of experience, mostly in Java and Spring Boot. For the
> past few years I've been working on a **commercial banking platform** — loan accounts, a
> double-entry ledger, interest accrual, billing cycles. The system runs on **AWS** — ECS Fargate
> behind ALBs, Aurora PostgreSQL, all provisioned with Terraform.*
>
> *What I enjoy most is the part where correctness actually matters. For example, I recently traced
> a bug where our nightly interest job and customer payments were racing each other — the interest
> was being calculated on a balance that had already changed by the time it was written. It wasn't a
> lost update, it was **write skew**, so the database never raised an error; it just wrote the wrong
> number silently. Finding that changed how I think about batch jobs.*
>
> *I'm looking at NAB because it's a **product company inside a real bank** — the domain I already
> work in, at a much larger scale, with an engineering culture I'd learn from."*

**Vì sao pitch này hiệu quả:** nó gài sẵn **ba móc câu** để người phỏng vấn kéo — banking domain,
AWS, và câu chuyện race condition. Bạn đang **điều khiển** phần tiếp theo của cuộc phỏng vấn.

### Điểm yếu — trả lời thế nào cho không sáo

Đừng dùng "I'm a perfectionist". Dùng gap thật, kèm hành động thật:

> *"Two honest ones. First, **Kubernetes** — we had an EKS cluster but we moved to ECS Fargate back
> in 2023 and I haven't operated Kubernetes since. I can read the manifests, but I wouldn't claim
> production experience.*
>
> *Second, **Kafka**. Our system uses SNS and a database-driven approach, so I've built the
> equivalent design on Kinesis rather than Kafka itself. I've been working through it deliberately —
> partition keys, consumer groups, offset semantics, at-least-once with an idempotent sink — because
> I'd rather understand the mechanics than just the vocabulary."*

Điểm mạnh của cách trả lời này: **cụ thể, kiểm chứng được, kèm hành động đang làm.** Và nó chủ động
xử lý gap trước khi bị đào.

### Vì sao đổi việc

Đừng chê công ty cũ. Kéo về **hướng đi**, không phải **chạy trốn**:

> *"I've learned a lot building the banking platform I'm on, but it's a relatively small team and
> the system is mature — the interesting architectural decisions were made before I joined. I want
> to work somewhere those decisions are still being made, at a scale where they matter more."*

---

## 3. Vòng Engineering Manager — khó nhất

**Chủ đề đã biết:** microservices, **Saga pattern**, **event-driven**, **commit log**, ~20% behavioral.

### Khung nói cho một bài system design — bốn giai đoạn

```text
CLARIFY   "Before I draw anything, three questions..."
          → Đặt câu hỏi TRƯỚC khi vẽ. Bằng tiếng Anh, đây cũng là cách MUA THỜI GIAN
            để đầu sắp xếp lại. Tận dụng.

ESTIMATE  "Let me put some numbers on the board first."
          → Con số là chỗ trú an toàn: nói số dễ hơn nói lập luận trừu tượng.

DESIGN    "I'll walk through the data path, then come back to failure modes."
          → Tuyên bố cấu trúc trước. Người nghe biết bạn đang ở đâu,
            và bạn cũng biết mình đang ở đâu.

TRADE-OFF "The trade-off is... and here's when I'd choose differently."
          → BẮT BUỘC. Không có câu này thì nghe như Senior, không phải Lead.
```

### 12 mẫu câu phải thuộc như phản xạ

| Tình huống | Câu |
|---|---|
| Mở đầu clarify | *"Before I sketch anything, can I check a couple of assumptions?"* |
| Chốt giả định | *"I'll assume X for now — let me know if that's wrong and I'll adjust."* |
| Nêu đánh đổi | *"The trade-off is that I gain A, but I pay for it with B."* |
| Nêu ngưỡng | *"This works up to roughly X. Past that, I'd switch to Y."* |
| Right-sizing | *"I wouldn't reach for Kafka at this load — here's the threshold where I would."* |
| Thừa nhận gap | *"I haven't done that in production. What I have done is..."* |
| Sửa mình | *"Actually, let me revise that — I think B is the better call because..."* |
| Đổi ý sau phản biện | *"That's a fair point. I'd change my answer: ..."* |
| Câu giờ hợp lệ | *"Let me think about that for a second."* |
| Kiểm tra hướng | *"Is that the level of detail you're after, or should I go deeper?"* |
| Nêu failure mode | *"The dangerous state here isn't failure — it's **not knowing**."* |
| Chốt lại | *"So to summarise: X for now, Y when we cross this threshold."* |

> Ba câu quan trọng nhất: **"the trade-off is..."**, **"I haven't done that in production, but..."**,
> **"let me revise that"**. Ba câu này phải bật ra không cần nghĩ.

### Kể câu chuyện ledger bằng tiếng Anh — 90 giây

Đây là **vũ khí chính**, luyện tới mức không cần nhìn giấy:

> *"We had a nightly job that accrued interest on loan accounts. It read the balance for a whole
> batch of accounts, computed the interest, and wrote the postings — but the read and the write were
> in **separate transactions**, so the gap between them was as long as the batch took to process.*
>
> *If a customer paid during that window, the interest was calculated on the old balance and written
> after the payment had landed. The subtle part is that this **isn't a lost update** — the two sides
> insert different rows, so the database sees no conflict at all. No exception, no error log. It
> writes successfully and is silently wrong. That's why it reached production.*
>
> *When I dug in, I found there was no optimistic or pessimistic locking anywhere in the codebase —
> and, more interestingly, there **was** a `synchronized` block in that exact file, so it looked like
> concurrency had been handled. But it was guarding a `SimpleDateFormat` that was already
> thread-local, and it was a JVM-local lock while the service runs on multiple ECS instances.*
>
> *The fix I proposed goes in order of risk: idempotency keys first — because every option needs
> retry, and retry without idempotency in a financial system is worse than the bug. Then narrow the
> scope from batch to per-account. Then a pessimistic row lock with a consistent lock ordering. And
> finally remove the cached balance column, which was a second source of truth.*
>
> *But the deepest fix wasn't locking at all. The race existed because interest was defined as a
> function of **whenever the job ran**. If you define it as a function of the ledger at a **business
> date**, it becomes a pure function — same answer no matter when it runs — and most of the need for
> locking disappears."*

**Vì sao câu chuyện này mạnh:** banking domain + concurrency + debugging + kiến trúc + **một insight
thật** (đổi định nghĩa thay vì thêm khoá) — đúng bốn thứ NAB đang tìm, trong một câu chuyện.

---

## 4. Luyện thế nào — 20 phút mỗi ngày

**Nguyên tắc: phải NÓI RA TIẾNG.** Đọc thầm không có tác dụng. Tiếng Anh nói và tiếng Anh đọc là hai
kỹ năng khác nhau, và bạn đang thiếu cái thứ nhất.

| Tuần | Bài tập hằng ngày | Cách tự chấm |
|:---:|---|---|
| **1** | Đọc to một mục trong [06](../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md), gấp lại, **giải thích lại bằng lời của mình** | Ghi âm. Nghe lại: có ngập ngừng quá 3 giây ở đâu? |
| **2** | Giải thích **Kafka** bằng tiếng Anh: commit log, partition, offset, consumer group | Có nói được *"it's not really a queue"* trôi không? |
| **3** | Kể **câu chuyện ledger** — 90 giây, rồi 5 phút, rồi bản đào sâu 15 phút | Bản 90 giây có nói liền mạch không nhìn giấy chưa? |
| **4** | **Mock design 45 phút**, 100% English, quay màn hình | Đếm số lần dùng "the trade-off is" — dưới 4 lần là chưa đạt |
| **5** | **6 câu STAR** bằng tiếng Anh, mỗi câu có số liệu | Có câu nào dài quá 2 phút không? Cắt |

### Ba mẹo thực dụng

1. **Ghi âm rồi nghe lại.** Khó chịu, nhưng đây là cách duy nhất nghe được chỗ mình ngập ngừng. Tự
   đánh giá lúc đang nói thì không chính xác.
2. **Luyện đúng chủ đề sẽ bị hỏi**, đừng luyện tiếng Anh chung chung. Từ vựng bạn cần là
   *idempotent, throughput, partition, compensating transaction, eventual consistency, back-pressure* —
   không phải từ vựng du lịch.
3. **Chuẩn bị sẵn câu chữa cháy** cho lúc mất từ: *"Sorry, let me rephrase that."* Có sẵn một câu để
   thoát thì sẽ không hoảng — và hoảng mới là thứ làm hỏng cả vòng.

---

## 5. Câu hỏi hỏi lại họ — bằng tiếng Anh

Hỏi hay là tín hiệu, và nó cũng chuyển thế trận về phía bạn. Chọn 3–4 câu:

| Câu hỏi | Vì sao đáng hỏi |
|---|---|
| *"Which parts of the platform does this team own end-to-end?"* | Biết mình sẽ làm gì thật |
| *"Where does Kafka sit in your architecture today — is it core to the transaction path, or more for analytics?"* | JD ghi "preferred". Hỏi thẳng, và nó cho thấy bạn hiểu sự khác biệt |
| *"How much of the Java estate is moving to Golang?"* | Họ tuyển Golang nhiều — hỏi thẳng thay vì đoán |
| *"What does on-call look like for this team?"* | Câu hỏi của người từng vận hành production |
| *"How do decisions get recorded — do you use ADRs or RFCs?"* | Nối thẳng vào thế mạnh của bạn |
| *"What separates someone doing well at this level from someone doing exceptionally?"* | Câu hỏi của người định gắn bó |

---

## 6. Ranh giới trung thực

| Điều | Trạng thái |
|---|---|
| "Ít nhất một vòng 100% English" | 🟢 Nhiều nguồn độc lập xác nhận |
| Chủ đề vòng EM (Saga, commit log, event-driven) | 🟡 Từ **một** bài kể chi tiết trên Viblo — đáng tin nhưng chỉ một nguồn |
| Câu hỏi HR (điểm mạnh/yếu, mục tiêu 2 năm) | 🟡 Từ voz, luồng **StarCamp (graduate)** — vòng senior có thể khác |
| Các mẫu câu ở [§3](#12-mẫu-câu-phải-thuộc-như-phản-xạ) | Tôi soạn dựa trên khung phỏng vấn chuẩn, **không phải trích từ NAB** |
| Trình độ tiếng Anh hiện tại của bạn | ❓ **Không có dữ liệu.** Bài tập ngày 1 (ghi âm 90 giây) tồn tại chính là để đo cái này |
| "5 tuần là đủ" | 🔴 **Phụ thuộc hoàn toàn vào điểm xuất phát.** Nếu bản ghi ngày 1 nghe khó khăn thì cần nhiều hơn 5 tuần — và tốt hơn là **hoãn nộp** chứ không phải nộp rồi hy vọng |
