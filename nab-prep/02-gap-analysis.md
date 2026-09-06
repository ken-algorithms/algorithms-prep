# 02 — Gap analysis: JD của NAB đối chiếu hồ sơ thật của bạn

> Không đoán. Mỗi ô "mạnh" dưới đây đều trỏ tới **bằng chứng đã verify** trong workspace này —
> file, dòng, số đo. Mỗi ô "gap" đều nói rõ **gap thật ở đâu** và mất bao lâu để bịt.
>
> Nguồn JD: [01-nab-research §3](01-nab-research.md#3-jd-seniorlead-java-engineer).

---

## 1. Bảng đối chiếu tổng

| # | Yêu cầu JD | Mức của bạn | Bằng chứng |
|:---:|---|:---:|---|
| 1 | **Banking / financial services** *(nice-to-have)* | 🟢🟢 **Rất mạnh** | Bạn đang làm **hệ thống ngân hàng thương mại thật** (Austin Capital Bank): loan account, **sổ cái kế toán kép** Transaction→JournalEntry→Posting→LedgerAccount, tính lãi, billing cycle, charge, past-due. Xem [06](../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md) |
| 2 | **Cloud computing (AWS, Azure)** | 🟢🟢 **Rất mạnh** | ECS Fargate, ALB, StepScaling, Route53, Secrets Manager, Aurora, Redshift, Lambda, SNS, WAF, EKS — **Terraform thật, có file:line**. Xem [05](../katalon-prep/katalon-system-design/05-he-thong-that-allinone-aws.md) |
| 3 | **Microservices + RESTful API** | 🟢 **Mạnh** | 6+ service qua Spring Cloud Gateway; đã phân tích được cả **điểm yếu** của thiết kế đó (route tĩnh, không service discovery) |
| 4 | **Containers (ECS, Docker)** | 🟢 **Mạnh** | ECS Fargate production thật, task definition, `awsvpc`, target_type=ip |
| 5 | **CI/CD (Jenkins, Git, Gradle)** | 🟢 **Mạnh** | CodeBuild `buildspec.yml`, **Jenkins chạy trên ECS Fargate + EFS** trong repo infra, S3 backend + DynamoDB state locking |
| 6 | **AWS Lambda / FaaS** | 🟡 **Vừa** | Lambda thật nhưng ở **tầng vận hành** (log parser, endpoint checker, SNS→Teams), **không phải business logic** |
| 7 | **Java 8+, Spring/Spring Boot** | 🟡 **Vừa — cần làm nóng tay** | Nền vững, nhưng **~1 năm không viết Java hằng ngày**. Đã tự nhận ở [katalon-prep README](../katalon-prep/README.md) |
| 8 | **Unit / integration test** | 🟡 **Vừa** | Có 313 test trong workspace ôn; nhưng ở dự án thật thì mảng test là điểm cần dẫn chứng cụ thể hơn |
| 9 | **Kubernetes** | 🟠 **Yếu** | EKS `csc-devops` **có thật nhưng chết từ 2023-08-10**, đã chuyển sang ECS. Nói được "vì sao bỏ K8s" là điểm cộng, nhưng vận hành K8s thì không |
| 10 | ⭐ **Event-driven + Kafka** *(preferred)* | 🔴 **GAP THẬT** | Đã grep toàn repo: **không có Kafka, không có SQS**. Chỉ có SNS ở tầng alerting. Đây là gap lớn nhất về kỹ thuật |
| 11 | ⭐ **Saga pattern / commit log** *(vòng EM)* | 🔴 **GAP THẬT** | Chưa có dự án nào dùng. Đã viết lý thuyết ở [06 §12-13](../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md) nhưng **chưa triển khai** |
| 12 | ⭐ **English (có vòng 100% tiếng Anh)** | ❓ **Ẩn số lớn nhất** | Không có dữ liệu. **Rủi ro cao nhất trong toàn bộ hồ sơ** — xem [§4](#4-rủi-ro-lớn-nhất-không-phải-kỹ-thuật) |
| 13 | **LeetCode live coding (tới hard)** | 🟡 **Vừa** | Có [leetcode-38-bai](../leetcode-38-bai/) cả Java lẫn Python, nhưng **đọc ≠ gõ tay**, và bar ở đây có thể là **hard** |
| 14 | **Mentor / định hướng kỹ thuật** *(Lead)* | 🟢 **Mạnh** | ADR/RFC thật, bakeoff 43 run tự bác bỏ RFC của mình, convention 1-file-1-prompt có version |
| 15 | **Agile, code review team quốc tế** | 🟢 **Mạnh** | Có, và có story thật về review phát hiện bug che lấp nhau |

---

## 2. Ba thứ khiến bạn **khác biệt** so với ứng viên Java thường

Đây là phần nên chủ động đưa ra, đừng chờ được hỏi.

### 2.1 Bạn đã làm sổ cái ngân hàng thật — và tìm ra bug trong đó

JD ghi *"banking/financial services"* là nice-to-have. Phần lớn ứng viên Java không có. Bạn không
chỉ **có**, mà còn:

- Hiểu **double-entry ledger** ở mức thiết kế: `Transaction → JournalEntry → Posting → LedgerAccount`
- Tìm ra một lỗi **write skew** thật giữa batch tính lãi và giao dịch khách — và gọi đúng tên nó,
  phân biệt được với lost update
- Đề xuất được cả bản vá lẫn bản thiết kế lại
  ([06 Phần II](../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md#12-nếu-được-thiết-kế-lại-từ-đầu--bản-triệt-để))

> Với một ngân hàng, câu chuyện này **đắt hơn** mọi thứ khác trong CV. Nó chứng minh bạn hiểu tiền,
> không chỉ hiểu code.

### 2.2 Bạn đọc được Terraform hạ tầng thật, không chỉ "dùng AWS"

Khác biệt giữa *"tôi có dùng AWS"* và *"tôi đã audit `aws_appautoscaling_policy` với
`step_scaling_policy_configuration` và biết vì sao chọn StepScaling thay vì TargetTracking"* là
khác biệt giữa Senior và Lead.

### 2.3 Bạn có thói quen **đo thay vì tin**

Bakeoff 43 run, tự bác bỏ RFC của chính mình 3 lần rồi mở lại; job đối soát để **chứng minh** số
liệu đúng thay vì tin nó đúng; test kiến trúc grep cả cây source để chặn tái phạm. Đây là tín hiệu
Lead rõ nhất, và nó **áp dụng được cho ngân hàng** — nơi "chắc là đúng" không đủ.

---

## 3. Ba gap kỹ thuật — và thời gian thật để bịt

| Gap | Vì sao quan trọng với NAB | Bịt thế nào | Thời gian |
|---|---|---|:---:|
| **Kafka / event-driven** | JD ghi "preferred"; vòng EM hỏi **commit log** — đó là ngôn ngữ của người dùng Kafka thật | Dựng một demo chạy được: producer/consumer, partition key, consumer group, offset, idempotent sink. Xem [04-kafka-event-driven-saga](04-kafka-event-driven-saga.md) | **1–2 tuần** |
| **Saga pattern** | Được hỏi đích danh ở vòng EM | Hiểu choreography vs orchestration, compensating transaction, và **vì sao ngân hàng cần nó**. Bạn đã có nền: ledger + bút toán ngược = compensating transaction | **3–5 ngày** |
| **Java tay nghề** | Live coding không có AI hỗ trợ | Gõ tay [leetcode-38-bai-java](../leetcode-38-bai-java/), **tắt Copilot**, tính giờ | **Liên tục, 3 tuần** |

> **Tin tốt về Saga:** bạn đã có sẵn nền tảng mà không nhận ra. `JournalEntry.buildRevertedPostings()`
> tạo posting âm để đảo bút toán — **đó chính xác là compensating transaction**. Bạn chỉ cần gọi
> đúng tên và mở rộng ra phạm vi nhiều service.

---

## 4. Rủi ro lớn nhất **không phải kỹ thuật**

```text
Kỹ thuật:  gap Kafka/Saga  →  bịt được trong 2 tuần, có lộ trình rõ

Tiếng Anh: vòng Engineering Manager 100% English, bàn SYSTEM DESIGN SÂU
           (microservices, Saga, event-driven, commit log) + behavioral
           →  KHÔNG bịt được trong 2 tuần nếu chưa quen nói
           →  và đây là vòng CUỐI, tức trượt ở đây là mất toàn bộ công sức trước đó
```

**Vì sao đây là rủi ro số một:**

- Bạn phải **tranh luận kiến trúc** bằng tiếng Anh, không chỉ trả lời câu hỏi. Nói được
  *"the trade-off is..."* trôi chảy khó hơn nhiều so với đọc tài liệu tiếng Anh.
- Đã có **ít nhất một vòng 100% English** ở mọi biến thể quy trình được báo cáo.
- JD viết rõ: *"especially in the global software development environment"* — họ làm việc trực tiếp
  với team Úc, không qua trung gian.

**Hành động:** bắt đầu luyện nói **ngay từ tuần 1**, song song với kỹ thuật, không để cuối.
Xem [05-english-interview](05-english-interview.md).

---

## 5. Điều gì **dùng lại được** từ katalon-prep

Đây là tin tốt lớn: bạn không bắt đầu từ đầu.

| Đã có | Dùng cho NAB thế nào |
|---|---|
| [leetcode-38-bai](../leetcode-38-bai/) + [-java](../leetcode-38-bai-java/) | **Trực tiếp** — vòng online assessment + live coding |
| [katalon-prep-java/](../katalon-prep/katalon-prep-java/) 9 module, 191 test | **Trực tiếp** — Spring Boot, Postgres, concurrency, distributed resilience |
| [05 — Hệ thống thật AWS](../katalon-prep/katalon-system-design/05-he-thong-that-allinone-aws.md) | **Trực tiếp** — kho dẫn chứng cho mọi câu hỏi về cloud |
| [06 — Race condition ledger](../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md) | **Vũ khí mạnh nhất** — banking + concurrency + thiết kế lại + AWS trong một câu chuyện |
| [04 — Event counting 10k](../katalon-prep/katalon-system-design/04-event-counting-10k.md) | System design; và phần Kinesis/stream là **cầu nối sang Kafka** |
| [02 — Distributed test execution](../katalon-prep/katalon-system-design/02-distributed-test-execution.md) | Fairness, queue, backpressure — dùng cho design |
| [katalon-self-questions/01 — Deadlock](../katalon-prep/katalon-self-questions/01-deadlock.md) | **Trực tiếp** — câu kinh điển vòng technical, đã có sẵn bản tiếng Anh |
| [katalon-prep-common/](../katalon-prep/katalon-prep-common/) | Clean code, design pattern, quy trình design 45 phút |

**Không dùng được cho NAB** (đặc thù Katalon): AI agent/LangGraph/RAG/embedding, Playwright/CDP,
test automation domain. Giữ nguyên đó cho hướng Katalon.

```text
Ước lượng:  ~70% công sức đã bỏ ra cho katalon-prep dùng lại được cho NAB
            ~30% còn lại là phần riêng của NAB: Kafka, Saga, tiếng Anh
```

---

## 6. Kết luận thẳng

**Bạn hợp với vị trí này hơn mức bạn nghĩ.** Ba yêu cầu khó nhất của JD — banking domain, AWS thật,
microservices ở quy mô production — bạn đều có **bằng chứng kiểm chứng được**, không phải kể suông.
Phần lớn ứng viên Java sẽ mạnh Java hơn bạn nhưng **không có ngân hàng thật trong tay**.

**Ba việc phải làm, theo đúng thứ tự ưu tiên:**

1. **Tiếng Anh** — bắt đầu ngay hôm nay, không để cuối. Rủi ro lớn nhất.
2. **Kafka + Saga** — gap kỹ thuật thật, nhưng có lộ trình 2 tuần rõ ràng.
3. **Tay nghề Java** — gõ tay mỗi ngày, tắt Copilot.

Chi tiết theo tuần: [03-ke-hoach-on-tap](03-ke-hoach-on-tap.md).
