# 03 — Kế hoạch ôn tập NAB: 5 tuần

> Bám đúng [5 vòng đã hợp nhất](01-nab-research.md#hợp-nhất--điều-gì-chắc-chắn-xuất-hiện) và
> [gap thật](02-gap-analysis.md#3-ba-gap-kỹ-thuật--và-thời-gian-thật-để-bịt). **~70% nội dung dùng
> lại từ katalon-prep** — kế hoạch này chỉ tập trung vào phần chưa có.

---

## 0. Nguyên tắc của kế hoạch này

1. **Tiếng Anh chạy song song từ ngày 1**, không dồn cuối. Đây là rủi ro số một và là thứ duy nhất
   không thể nhồi.
2. **Mỗi tuần phải có một thứ chạy được**, không chỉ đọc. Đọc ≠ biết.
3. **Ưu tiên theo vòng phỏng vấn**, không theo sở thích: vòng nào tới trước thì ôn trước.
4. **Câu chuyện ledger là vũ khí chính** — mọi tuần đều luyện kể lại nó ở một góc khác nhau.

---

## 1. Bản đồ: mỗi vòng cần gì

| Vòng | Cần gì | Ôn ở đâu | Tuần |
|:---:|---|---|:---:|
| **1. Trắc nghiệm / 3 bài thuật toán** | Java core, Spring cơ bản, DSA medium | [leetcode-38-bai-java](../leetcode-38-bai-java/) + [katalon-prep-java/00](../katalon-prep/katalon-prep-java/) | 1–2 |
| **2. HR 100% tiếng Anh** | Giới thiệu, điểm mạnh/yếu, mục tiêu, lý do đổi việc | [05-english-interview](05-english-interview.md) | 1 |
| **3. Live coding (tới hard)** | Gõ tay, tắt Copilot, nói trong lúc code | [leetcode-38-bai-java](../leetcode-38-bai-java/) | 2–4 |
| **4. Technical với Tech Lead** | Java core, Spring (Beans/IoC/Security/JPA), dự án, git, sort/search, Docker | [katalon-prep-java](../katalon-prep/katalon-prep-java/) + câu chuyện ledger | 3 |
| **5. Engineering Manager — 100% English** | **System design sâu**: microservices, **Saga**, **event-driven**, **commit log** + behavioral | [04-kafka-event-driven-saga](04-kafka-event-driven-saga.md) + [katalon-system-design](../katalon-prep/katalon-system-design/) | 4–5 |

---

## 2. Kế hoạch theo tuần

### Tuần 1 — Làm nóng Java + khởi động tiếng Anh

| Ngày | Việc | Xong là gì |
|:---:|---|---|
| 1 | Đọc [01-nab-research](01-nab-research.md) + [02-gap-analysis](02-gap-analysis.md). Lấy **JD nguyên văn** của đúng vị trí đang mở trên ITviec | Biết chính xác mình nộp vào đâu, thiếu gì |
| 1 | **Ghi âm elevator pitch 90 giây bằng tiếng Anh**, nghe lại | Biết trình độ nói thật của mình đang ở đâu |
| 2–3 | [katalon-prep-java/00-refresher-java21](../katalon-prep/katalon-prep-java/) — Stream, Optional, CompletableFuture, record, sealed | Tay Java hết ì |
| 4–5 | Spring: **Bean lifecycle, IoC/DI, `@Transactional` proxy, JPA N+1, Spring Security filter chain** | Trả lời được 4 chủ đề Spring bị hỏi đích danh |
| 6–7 | **6 bài LeetCode medium, gõ tay, tắt Copilot, tính giờ 25 phút/bài** | Biết tốc độ thật của mình |
| Mỗi ngày | **20 phút nói tiếng Anh** — đọc to một mục trong [06](../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md) rồi tự giải thích lại không nhìn | |

### Tuần 2 — Kafka và event-driven *(gap lớn nhất)*

| Ngày | Việc | Xong là gì |
|:---:|---|---|
| 1–2 | Đọc [04-kafka-event-driven-saga §1-3](04-kafka-event-driven-saga.md) — commit log, partition, offset, consumer group | Giải thích được **vì sao Kafka là commit log**, không chỉ "hàng đợi" |
| 3–5 | **Dựng demo chạy được**: docker-compose Kafka + producer/consumer Spring Boot, partition key = accountId, consumer group, idempotent sink | **Có code để kể**, không phải lý thuyết |
| 6 | Đọc [04 §4](04-kafka-event-driven-saga.md) — **Saga**: choreography vs orchestration, compensating transaction | Nối được với `buildRevertedPostings()` trong dự án thật của bạn |
| 7 | 6 bài LeetCode medium/hard | |
| Mỗi ngày | 20 phút nói tiếng Anh — chủ đề tuần này: **giải thích Kafka bằng tiếng Anh** | |

### Tuần 3 — Technical round: dự án + Java sâu

| Ngày | Việc | Xong là gì |
|:---:|---|---|
| 1–2 | Luyện kể **câu chuyện ledger** ([06](../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md)) trong 3 độ dài: 90 giây, 5 phút, 15 phút đào sâu | Vũ khí chính đã sẵn sàng |
| 3 | [katalon-self-questions/01 — Deadlock](../katalon-prep/katalon-self-questions/01-deadlock.md), **chạy lại code**, học phần tiếng Anh | Trả lời được cả VI và EN |
| 4 | [katalon-prep-java/05-postgres-depth](../katalon-prep/katalon-prep-java/) — index, `EXPLAIN`, isolation level | Nối được với câu chuyện race condition |
| 5 | [katalon-prep-java/06-distributed-resilience](../katalon-prep/katalon-prep-java/) — retry+jitter, circuit breaker, DLQ, idempotent | Chuỗi at-least-once → idempotent nói trôi |
| 6 | Docker + git: multi-stage build, layer cache; rebase vs merge, cherry-pick, bisect | Hai chủ đề bị hỏi đích danh |
| 7 | 6 bài LeetCode, **có bài hard** | |
| Mỗi ngày | 20 phút tiếng Anh — chủ đề: **kể dự án bằng tiếng Anh** | |

### Tuần 4 — System design + vòng EM

| Ngày | Việc | Xong là gì |
|:---:|---|---|
| 1 | [katalon-system-design/README](../katalon-prep/katalon-system-design/README.md) — **5 trục nhận diện, 7 họ bài** | Xử lý được đề chưa từng thấy |
| 2 | [06 Phần II](../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md#12-nếu-được-thiết-kế-lại-từ-đầu--bản-triệt-để) — single-writer, event-driven, AWS 11 bước | **Đúng chủ đề vòng EM**: microservices + event-driven + commit log trong một bài |
| 3 | [04 — Event counting](../katalon-prep/katalon-system-design/04-event-counting-10k.md) | Bài counting-at-scale, và cầu nối stream |
| 4 | [04-kafka-event-driven-saga §5](04-kafka-event-driven-saga.md) — **luyện bài Saga: chuyển tiền liên ngân hàng** | Bài design ngân hàng kinh điển |
| 5 | **Mock design 45 phút, quay màn hình, nói 100% tiếng Anh** | Biết mình vỡ ở đâu |
| 6 | Xem lại bản quay, sửa 3 điểm yếu nhất | |
| 7 | LeetCode hard × 3 | |
| Mỗi ngày | 30 phút tiếng Anh — **tăng liều**, chủ đề: tranh luận trade-off | |

### Tuần 5 — Ghép lại + behavioral + đàm phán

| Ngày | Việc | Xong là gì |
|:---:|---|---|
| 1–2 | **6 câu chuyện STAR**, mỗi câu có số liệu, **bằng tiếng Anh** | ~20% vòng EM là behavioral |
| 3 | Chuẩn bị **câu hỏi hỏi lại** họ: team làm sản phẩm gì, on-call ra sao, Kafka dùng ở đâu, lộ trình Java→Golang | Vòng nào cũng hỏi, và hỏi hay là tín hiệu |
| 4 | **Chiến lược đàm phán lương** — họ neo theo payslip ([§5](01-nab-research.md#5-lương--các-nguồn-mâu-thuẫn-nhau-đọc-kỹ)) | Không bị bất ngờ ở vòng 5 |
| 5 | **Mock full-loop**: HR (EN) → coding → technical → design (EN) | Diễn tập toàn bộ |
| 6 | Vá 3 lỗ hổng lộ ra từ mock | |
| 7 | Nghỉ. Đọc lại các mục **ranh giới trung thực** | Biết rõ chỗ nào không được nói quá |

---

## 3. Lịch hằng ngày (mọi tuần)

```text
 20-30 phút   TIẾNG ANH nói ra tiếng   ← không bao giờ bỏ, kể cả ngày bận
 60-90 phút   Việc chính của ngày hôm đó
 30-45 phút   LeetCode gõ tay, tắt Copilot, tính giờ
 10 phút      Ghi lại: hôm nay cái gì CHẠY được, cái gì mới chỉ ĐỌC
```

> Cột cuối quan trọng nhất. Cuối tuần nhìn lại, nếu toàn "đọc" thì tuần đó **chưa tính là xong**.

---

## 4. Ba thứ đo được để biết đã sẵn sàng

| Kiểm tra | Đạt khi |
|---|---|
| **Coding** | Giải xong 1 bài LeetCode **hard** trong 45 phút, **vừa code vừa giải thích thành tiếng**, không dùng AI |
| **System design** | Trình bày 45 phút bài "thiết kế lại hệ thống ledger" **bằng tiếng Anh**, có ≥4 trade-off nói rõ, không nhìn giấy |
| **Tiếng Anh** | Nghe lại bản ghi của chính mình mà **không thấy ngập ngừng ở đoạn tranh luận trade-off** |

Chưa đạt cả ba thì **đừng nộp vội** — quy trình NAB dài và họ có cooldown giữa các lần apply.

---

## 5. Nếu chỉ còn 1 tuần

Bỏ hết, làm đúng bốn thứ này:

1. **Câu chuyện ledger bằng tiếng Anh** — 90 giây + phiên bản đào sâu 10 phút.
   ([06](../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md))
2. **Kafka ở mức khái niệm cho vững**: commit log, partition, offset, consumer group, ordering theo
   key, at-least-once + idempotent. ([04 §1-3](04-kafka-event-driven-saga.md))
3. **Saga**: choreography vs orchestration + compensating transaction, nối với bút toán ngược trong
   dự án của bạn. ([04 §4](04-kafka-event-driven-saga.md))
4. **10 bài LeetCode medium gõ tay**, tắt Copilot.

Bỏ qua: Kubernetes (đằng nào cũng yếu, thành thật là hơn), Golang, chi tiết Azure.

---

## 6. Trung thực về kế hoạch này

| Điều | Trạng thái |
|---|---|
| 5 tuần | **Ước lượng của tôi**, giả định bạn học ~2-3 giờ/ngày. Chưa tính lịch làm việc thật của bạn |
| "70% dùng lại được" | Ước lượng từ đối chiếu nội dung, **không phải đo** |
| Quy trình 5 vòng | Từ nguồn ẩn danh — **có thể khác** khi bạn phỏng vấn thật. Xem [§7 độ tin cậy](01-nab-research.md#7-độ-tin-cậy-của-từng-nguồn) |
| "LeetCode hard" | Chỉ một nguồn nói. Nguồn khác nói medium. Kế hoạch này **ôn tới hard cho chắc** |
| Demo Kafka tuần 2 | **Chưa dựng.** Là việc cần làm, không phải việc đã xong |
