# 01 — NAB Innovation Centre Vietnam: công ty, JD, quy trình phỏng vấn

> **Nguồn:** tra web ngày **06/09/2026**. Mọi mục đều ghi rõ **độ tin cậy** — JD từ trang tuyển dụng
> là một chuyện, lời kể ẩn danh trên voz là chuyện khác. Xem [§7](#7-độ-tin-cậy-của-từng-nguồn)
> trước khi dùng bất kỳ con số nào để đàm phán.

---

## 1. Công ty

| | |
|---|---|
| **Tên đầy đủ** | NAB Innovation Centre Vietnam (NICV) |
| **Thuộc** | National Australia Bank — **Technology & Enterprise Operations division** |
| **NAB là ai** | Ngân hàng doanh nghiệp lớn nhất Úc, một trong "Big Four" |
| **Thành lập tại VN** | 2019 |
| **Địa điểm** | TP.HCM **và** Hà Nội |
| **Quy mô** | 1.000+ nhân sự |
| **Loại hình** | IT Product (banking) — **không phải outsourcing** |
| **Mô hình làm việc** | Hybrid, thứ 2–6, chính sách **"No OT"** |
| **Đánh giá ITviec** | 4.6/5.0 từ 75 review, **97% recommend** |

> **Điểm quan trọng cho bạn:** đây là **product company của một ngân hàng thật**, không phải công ty
> gia công. Nghĩa là domain nghiệp vụ ngân hàng được coi trọng — và đó **đúng là thế mạnh lớn nhất**
> của bạn (xem [02-gap-analysis](02-gap-analysis.md)).

---

## 2. Tech stack công ty tự công bố

| Nhóm | Công nghệ |
|---|---|
| **Cloud & hạ tầng** | **AWS**, Azure, **Kubernetes**, **Terraform** |
| **Ngôn ngữ** | **Java**, JavaScript/TypeScript, Node.js, .NET, **Golang** |
| **Framework** | **Spring Boot**, React-Redux, Serverless |
| **DevOps** | **Jenkins**, GitHub, CloudFormation, CHEF, **Docker** |
| **Công cụ** | JIRA, Confluence, Microsoft Teams, ServiceNow |

**Ba quan sát đáng chú ý:**

1. **AWS + Terraform + ECS/K8s + Jenkins** — trùng gần như hoàn toàn với hạ tầng thật bạn đã review
   ở [../katalon-prep/katalon-system-design/05](../katalon-prep/katalon-system-design/05-he-thong-that-allinone-aws.md).
   Đây là điểm mạnh **có bằng chứng file:line**, không phải nói suông.
2. **Golang xuất hiện nhiều** trong tin tuyển dụng gần đây (Senior Golang Engineer, Full-stack
   ReactJS+Golang) — dấu hiệu họ đang dịch chuyển một phần khỏi Java. Không phải rào cản, nhưng nên
   biết để không ngạc nhiên nếu bị hỏi.
3. **Không thấy nhắc Kafka trong stack công bố**, nhưng **JD lại ghi "Kafka preferred"** — xem
   [§3](#3-jd-seniorlead-java-engineer).

---

## 3. JD Senior/Lead Java Engineer

### Yêu cầu kinh nghiệm

| Cấp | Số năm |
|---|---|
| **Senior** | **5+ năm** software engineer trong môi trường phát triển phức tạp |
| **Lead** | **7+ năm** (một nguồn khác ghi **8+** — xem [§7](#7-độ-tin-cậy-của-từng-nguồn)) |

### Must-have

- **Java 8+**, **Spring / Spring Boot**
- Thiết kế & phát triển **RESTful API** và hệ thống **microservices**
- **Cloud computing (AWS, Azure...)**
- **Containers (ECS, Kubernetes, Docker)** và **FaaS (AWS Lambda)**
- **Unit test và integration test**
- Môi trường **Agile**, clean code, khả năng giải quyết vấn đề
- ⭐ *"Experience with **distributed, event-driven systems** and using messaging protocols, with
  **Apache Kafka preferred**"*
- ⭐ *"**Good English communication skills** (both verbal & written), especially in the global
  software development environment"*

### Nice-to-have

- **CI/CD** hiện đại: Git, Ansible, Jenkins, NPM, Gradle
- ⭐ **Kinh nghiệm banking / financial services**

### Trách nhiệm (trích)

- *"Design, develop, review, implement, and manage Java applications and services"*
- *"Design, develop, review, implement, and manage continuous integration, build management and
  deployment scripts"*
- Tham gia Agile, prototyping, code review **với team quốc tế**, xây microservices và API trên
  **AWS Cloud architecture**
- Làm cùng delivery team qua **CI/CD và DevOps practices**
- Coding vững, **unit/component test coverage tốt**, debug
- **Mentor engineer và định hướng kỹ thuật** (cấp Lead)

> **Ba dòng có ⭐ là ba dòng quyết định.** Hai trong ba bạn đang **rất mạnh** (banking, AWS), một
> đang là **gap thật** (Kafka/event-driven) — và tiếng Anh là ẩn số lớn nhất.

### Phúc lợi (theo tin tuyển dụng)

Lương cạnh tranh + **thưởng tháng 13** · **20 ngày phép + 7 ngày nghỉ ốm** · bảo hiểm sức khoẻ cao
cấp · tài khoản học (Udemy, Coursera, LinkedIn Learning, **A Cloud Guru**) · khuyến khích thi
**chứng chỉ AWS/Azure** · hybrid · sự kiện công ty thường niên.

---

## 4. Quy trình phỏng vấn — **hai biến thể được báo cáo**

Đây là phần quan trọng nhất, và các nguồn **không thống nhất**. Tôi trình bày cả hai thay vì chọn
một, vì cả hai đều từ người đã phỏng vấn thật.

### Biến thể A — 5 vòng *(voz, vị trí Java Engineer)*

| Vòng | Nội dung |
|:---:|---|
| 1 | **Trắc nghiệm Java/Spring online** — *"làm trên web, đủ số câu là pass"* |
| 2 | **HR call — 100% tiếng Anh** |
| 3 | **Live coding — một bài LeetCode HARD** |
| 4 | **Technical interview với tech lead** |
| 5 | **Đàm phán lương** |

### Biến thể B — 3 vòng *(bài kể chi tiết trên Viblo, vị trí Senior Backend)*

| Vòng | Nội dung | Thời lượng / ngôn ngữ |
|:---:|---|---|
| 1 | **Online assessment — 3 bài thuật toán**, *"độ khó khoảng tầm medium trở xuống, không đến mức quá khó"* | — |
| 2 | **Technical với 2 Tech Lead** — Java, Spring Boot, backend; **có phần system design phân tích tình huống thật** | **~1 tiếng 45 phút**, ~50% Anh / 50% Việt |
| 3 | **Engineering Manager** — system design **sâu**: microservices, **Saga pattern**, **event-driven**, **commit log**; ~20% behavioral | **100% tiếng Anh** |

### Biến thể C — StarCamp *(chương trình graduate, không áp dụng cho bạn)*

Phỏng vấn tiếng Anh → **Codility test** → technical interview về DSA/OOP/database/Java core.
*"sẽ không hỏi mấy cái quá cao siêu"*. Ghi lại để bạn **không nhầm** thông tin junior với luồng senior.

### Hợp nhất — điều gì chắc chắn xuất hiện

Bỏ qua khác biệt về số vòng, **năm thứ này xuất hiện ở mọi nguồn**:

```text
1. SÀNG LỌC TỰ ĐỘNG      → trắc nghiệm Java/Spring HOẶC 3 bài thuật toán online
2. CODING THẬT           → LeetCode (medium→hard, có nguồn nói HARD), có thể trên Codility
3. TECHNICAL VỚI LEAD    → Java core, Spring (Beans, IoC, Security, JPA), dự án của bạn,
                           git, thuật toán (sort/search), Docker
4. SYSTEM DESIGN         → microservices, Saga, event-driven, commit log
5. TIẾNG ANH             → ít nhất một vòng 100% English, thường là vòng Engineering Manager
```

### Câu hỏi kỹ thuật được nêu đích danh

| Chủ đề | Cụ thể được nhắc |
|---|---|
| **Java core** | Nền tảng ngôn ngữ, OOP |
| **Spring** | **Beans, IoC, Security, JPA** |
| **Dự án** | *"trình bày dự án mà bạn đã làm"* — đào sâu |
| **Git** | Kinh nghiệm sử dụng |
| **Thuật toán** | **sort, search** |
| **Docker** | Container cơ bản |
| **System design** | **Microservices, Saga pattern, event-driven, commit log** |
| **Behavioral** | Teamwork, quản lý công việc (~20% vòng EM) |

> **`commit log` là từ khoá đáng chú ý** — đó là ngôn ngữ của người dùng Kafka thật (Kafka *là* một
> distributed commit log). Nói được điều này ở vòng EM sẽ khác hẳn việc chỉ liệt kê "tôi biết Kafka".

---

## 5. Lương — **các nguồn mâu thuẫn nhau, đọc kỹ**

| Nguồn | Con số | Loại nguồn |
|---|---|---|
| Trang tổng hợp việc làm | **29–44 triệu/tháng** trung bình | Trung bình **mọi vị trí**, gồm cả non-tech |
| voz | Dev ~29-30 tuổi: **"3-4k nhiều lắm"** (USD/tháng ≈ 75–100tr) | Ẩn danh, tự chọn mẫu |
| voz | Senior iOS: **"min là 3k net"** | Ẩn danh |
| voz | Một người pass với offer **3k, 4+ năm kinh nghiệm** | Ẩn danh |
| voz | *"60 triệu/tháng vẫn còn thấp"*; senior thu nhập **800tr – 1 tỷ/năm** | Ẩn danh, dễ thổi phồng |
| Review site | Senior DevOps từ **80tr gross** | Ẩn danh |

**Vì sao lệch tới 3 lần:** trang việc làm lấy trung bình toàn công ty (gồm hành chính, support);
voz là dân IT senior tự khoe — mẫu lệch lên. **Con số thực tế cho Senior/Lead Java nhiều khả năng
nằm trong khoảng 2.500–4.000 USD/tháng**, nhưng đây là **suy luận của tôi từ các nguồn trên**, không
phải dữ liệu xác nhận.

> ⚠️ **Cảnh báo từ nhiều nguồn:** NAB **đàm phán lương dựa trên payslip** — họ đòi bảng lương hiện
> tại. Nghĩa là mức tăng bị neo vào lương cũ chứ không neo vào giá thị trường. **Chuẩn bị lập luận
> cho việc này trước khi vào vòng 5**, đừng để bị bất ngờ.

---

## 6. Văn hoá & điều kiện làm việc

**Điểm cộng được nhắc lặp lại:**

- **No OT**, giờ giấc đúng, ổn định — *"điểm đến lý tưởng cho dân IT thích môi trường chuyên nghiệp
  nhưng không quá áp lực"*
- Được xếp **tier 1 ở Hà Nội** trong cộng đồng
- Đào tạo thật: **Cloud Guild**, **Tech Academy**, tài khoản A Cloud Guru, khuyến khích thi chứng chỉ
- Lương ổn định, minh bạch

**Điểm trừ / cần biết trước:**

- **Quy trình tuyển rất dài và tốn thời gian** — nhiều review phàn nàn
- *"Quy trình tuyển dụng cực khó"*
- Đàm phán lương neo theo payslip
- **Referral có trọng lượng** — voz nói vị trí *"thường có network rủ nhau vào"*
- Số vòng đã **tăng từ 3 lên 5** do lượng ứng viên đông

> **Hành động cụ thể:** trước khi nộp cổng chính thức, tìm xem có ai quen đang làm NAB không.
> Referral ở đây không phải "cho có" — nó rút ngắn được phần sàng lọc.

---

## 7. Độ tin cậy của từng nguồn

Đọc bảng này **trước khi** dùng bất kỳ con số nào ở trên.

| Thông tin | Nguồn | Độ tin cậy | Lưu ý |
|---|---|:---:|---|
| Công ty, quy mô, địa điểm, tech stack | Trang công ty trên ITviec | 🟢 Cao | Công ty tự công bố |
| Yêu cầu JD (Java 8+, Spring, AWS, Kafka preferred, English) | Tin tuyển dụng ITviec / ITjobs | 🟢 Cao | Trích từ JD đăng công khai |
| Senior 5+ năm / Lead 7+ năm | Tin tuyển dụng | 🟡 Vừa | **Một nguồn khác ghi Lead 8+** — số năm thay đổi theo từng tin |
| Phúc lợi (13th, 20 phép, healthcare) | Tin tuyển dụng | 🟢 Cao | |
| **Quy trình 5 vòng** | voz (ẩn danh) | 🟡 Vừa | Một người kể; không phải quy trình chính thức |
| **Quy trình 3 vòng + chi tiết vòng EM** | Bài Viblo (có tên, kể chi tiết) | 🟡 Vừa–Cao | Nhất quán, có chi tiết cụ thể → đáng tin hơn |
| **LeetCode HARD ở vòng live coding** | voz | 🟠 Thấp–Vừa | **Chỉ một nguồn.** Nguồn khác nói *"medium trở xuống"*. **Cứ ôn tới hard cho chắc** |
| Chủ đề kỹ thuật (Spring Beans/IoC/JPA, Saga, commit log) | Nhiều nguồn trùng khớp | 🟢 Cao | Trùng lặp giữa các nguồn độc lập |
| **Mọi con số lương** | voz + review site | 🟠 Thấp | Mâu thuẫn nhau tới 3 lần. **Không dùng để chốt kỳ vọng** |
| "Đàm phán theo payslip" | Nhiều review | 🟡 Vừa | Lặp lại đủ nhiều để đáng chuẩn bị |
| Văn hoá No OT, tier 1 | Nhiều nguồn | 🟡 Vừa | Ý kiến chủ quan, nhưng nhất quán |

**Ba trang không truy cập được** (HTTP 410 / DNS lỗi) nên một số JD chỉ lấy được qua bản tóm tắt
tìm kiếm chứ không phải nguyên văn: hai tin ITviec đã gỡ, và `1900.com.vn` không phân giải được
tên miền từ môi trường này.

> **Việc bạn nên tự làm:** mở trực tiếp
> [trang công ty trên ITviec](https://itviec.com/companies/nab-innovation-centre-vietnam) và
> [trang tuyển dụng NAB](https://www.nab.com.au/about-us/global-innovation-centre-careers) để lấy
> **JD nguyên văn của đúng vị trí đang mở**, vì JD thay đổi theo từng đợt. Tài liệu này là bản đồ,
> không phải bản sao công chứng.

---

## Nguồn

- [NAB Innovation Centre Vietnam — trang công ty, ITviec](https://itviec.com/companies/nab-innovation-centre-vietnam)
- [Senior/Lead Java Engineer — ITjobs.com.vn](https://www.itjobs.com.vn/en/job/85199/senior-lead-java-engineer)
- [Senior/Lead Java Engineer — ITviec](https://itviec.com/it-jobs/senior-lead-java-engineer-nab-innovation-centre-vietnam-2901)
- [Java Engineer (All levels) — ITviec](https://itviec.com/it-jobs/java-engineer-all-levels-nab-innovation-centre-vietnam-0402)
- [NAB Global Innovation Centres — careers chính thức](https://www.nab.com.au/about-us/global-innovation-centre-careers)
- [voz — Hội anh em NAB](https://voz.vn/t/hoi-anh-em-nab.693795/)
- [voz — Xin review dev tại NAB](https://voz.vn/t/xin-review-dev-tai-nab.543106/)
- [voz — Java Technical Interview NAB StarCamp](https://voz.vn/t/java-technial-interview-nab-starcamp.1034064/)
- [Viblo — Phía sau offer Senior tại NAB là cả quá trình bền bỉ](https://viblo.asia/p/phia-sau-offer-senior-tai-nab-la-ca-qua-trinh-ben-bi-WR5JR1Y04Gv)
- [NgonCareer — Review lương tại NAB](https://ngoncareer.com/muc-luong/review-luong-nab/)
