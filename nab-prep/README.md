# nab-prep — chuẩn bị phỏng vấn NAB Innovation Centre Vietnam

> **Vị trí nhắm tới:** Senior / Lead Java Engineer · **Tra cứu ngày 06/09/2026**
>
> Song song với [../katalon-prep/](../katalon-prep/). **~70% nội dung dùng lại được** — folder này
> chỉ chứa phần riêng của NAB.

---

## Đọc theo thứ tự

| # | File | Trả lời câu gì |
|:---:|---|---|
| **01** | [**NAB research**](01-nab-research.md) | Công ty là ai, JD yêu cầu gì, phỏng vấn mấy vòng, lương bao nhiêu — **kèm bảng độ tin cậy từng nguồn** |
| **02** | [**Gap analysis**](02-gap-analysis.md) ⭐ | JD đối chiếu hồ sơ thật của bạn: mạnh chỗ nào (có bằng chứng file:line), thiếu chỗ nào |
| **03** | [**Kế hoạch 5 tuần**](03-ke-hoach-on-tap.md) | Làm gì mỗi ngày, theo đúng thứ tự các vòng |
| **04** | [**Kafka / event-driven / Saga**](04-kafka-event-driven-saga.md) | Gap kỹ thuật lớn nhất — và cách biến nó thành điểm mạnh |
| **05** | [**Vòng tiếng Anh**](05-english-interview.md) ⭐ | **Rủi ro số một.** Script kể chuyện, 12 mẫu câu tranh luận trade-off. Phần xây nền nằm ở lộ trình riêng `ielts-target-5-5/` ([bản web](https://claude.ai/code/artifact/9569184e-cc2d-43ce-b0e7-c4c0183af03b)) |

---

## Tóm tắt 60 giây

**Công ty:** NAB Innovation Centre Vietnam — product company của ngân hàng lớn nhất Úc, lập tại VN
2019, 1.000+ người, HCM + Hà Nội, hybrid, **No OT**, ITviec 4.6/5 (97% recommend).

**Stack:** AWS · Azure · Kubernetes · Terraform · **Java + Spring Boot** · Golang · React ·
Jenkins · Docker.

**Phỏng vấn — hợp nhất từ các nguồn:**

```text
1. Sàng lọc tự động   trắc nghiệm Java/Spring HOẶC 3 bài thuật toán online
2. Coding thật        LeetCode (medium → có nguồn nói hard), có thể trên Codility
3. Technical + Lead   Java core, Spring (Beans/IoC/Security/JPA), dự án, git, sort/search, Docker
4. System design      microservices, Saga, event-driven, commit log
5. Ít nhất 1 vòng     100% TIẾNG ANH — thường là Engineering Manager
```

**Vị thế của bạn:**

| | |
|---|---|
| 🟢🟢 **Rất mạnh** | **Banking domain** (sổ cái kế toán kép thật), **AWS** (Terraform/ECS/ALB thật, có file:line) |
| 🟢 **Mạnh** | Microservices, containers, CI/CD, mentor/ADR-RFC |
| 🟡 **Cần làm nóng** | Java tay nghề (~1 năm không viết hằng ngày), LeetCode gõ tay |
| 🔴 **Gap thật** | **Kafka / event-driven**, **Saga** — bịt được trong ~2 tuần |
| ❓ **Ẩn số lớn nhất** | **Tiếng Anh nói** — không bịt được trong 2 tuần. Nền đã có lộ trình riêng 40 tuần ở `ielts-target-5-5/` ([bản web](https://claude.ai/code/artifact/9569184e-cc2d-43ce-b0e7-c4c0183af03b)); file 05 chỉ lo phần kịch bản phỏng vấn |

---

## Ba việc làm ngay hôm nay

1. **Ghi âm elevator pitch 90 giây bằng tiếng Anh, nghe lại.** Đây là phép đo duy nhất cho biết bạn
   cần 5 tuần hay 10 tuần. ([05 §4](05-english-interview.md#4-luyện-thế-nào--20-phút-mỗi-ngày))
2. **Lấy JD nguyên văn** của đúng vị trí đang mở trên
   [ITviec](https://itviec.com/companies/nab-innovation-centre-vietnam) — JD đổi theo từng đợt, tài
   liệu này là bản đồ chứ không phải bản sao công chứng.
3. **Tìm referral.** Nhiều nguồn nói vị trí ở NAB *"thường có network rủ nhau vào"*, và quy trình đã
   tăng từ 3 lên 5 vòng vì quá đông ứng viên.

---

## Vũ khí mạnh nhất của bạn

Câu chuyện **race condition trong sổ cái ngân hàng**
([../katalon-prep/katalon-system-design/06](../katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md)) —
gói gọn **banking domain + concurrency + debugging + thiết kế lại + AWS** trong một câu chuyện, và
mọi chi tiết đều verify được bằng code thật.

Bản tiếng Anh 90 giây đã soạn sẵn ở
[05 §3](05-english-interview.md#kể-câu-chuyện-ledger-bằng-tiếng-anh--90-giây). **Luyện tới mức không
cần nhìn giấy.**

---

## Cảnh báo cần biết trước

| | |
|---|---|
| **Đàm phán lương neo theo payslip** | Nhiều nguồn nhắc. Mức tăng bị neo vào lương cũ, không neo vào giá thị trường. Chuẩn bị lập luận trước vòng 5 |
| **Quy trình dài** | Nhiều review phàn nàn tốn thời gian; số vòng đã tăng từ 3 lên 5 |
| **Số liệu lương mâu thuẫn tới 3 lần** | Trang việc làm nói 29-44tr/tháng, voz nói 3-4k USD. **Đừng dùng con số nào để chốt kỳ vọng** — xem [01 §5](01-nab-research.md#5-lương--các-nguồn-mâu-thuẫn-nhau-đọc-kỹ) |

---

## Liên quan

| | |
|---|---|
| [../katalon-prep/](../katalon-prep/) | Hướng song song. Phần dùng chung: DSA, Java, system design, clean code |
| [../katalon-prep/katalon-system-design/](../katalon-prep/katalon-system-design/) | 6 bài design + bản đồ 7 họ bài — dùng trực tiếp cho vòng 4 và 5 |
| [../leetcode-38-bai-java/](../leetcode-38-bai-java/) | Vòng 1 và vòng 3 |
| [../katalon-prep/katalon-self-questions/](../katalon-prep/katalon-self-questions/) | Deadlock — đã có sẵn bản song ngữ |
