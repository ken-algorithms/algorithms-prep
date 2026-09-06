# Lộ trình phỏng vấn Katalon — Senior Developer / Technical Lead

> Cập nhật: **13/08/2026**. Tài liệu này được viết lại dựa trên **JD thật đang mở tại Katalon** (fetch trực tiếp từ `katalon.com/careers` + Built In + Glassdoor), không phải checklist chung.
> Bài tập DSA đi kèm: [leetcode-38-bai-phong-van-vietnam.md](../leetcode-38-bai-phong-van-vietnam.md) (Python + Java 21).

---

## 0. TL;DR — 6 điều cần sửa trong checklist tham khảo

Checklist bạn đưa đúng khoảng **70%** phần "kỹ năng chung của Senior/Lead", nhưng **lệch khá xa** so với JD thật của Katalon 2026. Bảng đối chiếu:

| # | Checklist tham khảo nói | Thực tế JD Katalon (verified) | Mức độ |
|---|---|---|---|
| 1 | Ngầm định stack là **Java** | Vị trí Lead đang mở là **"Lead Software Engineer (Java, Python, JavaScript)"** — Python là **yêu cầu cứng**, không phải nice-to-have. JS/TS + Playwright cũng là yêu cầu cứng | 🔴 **Sai trọng số** |
| 2 | Không nhắc **Quarkus** | Quarkus xuất hiện ở **mọi** JD backend của Katalon, đứng ngang hoặc **trước** Spring Boot. Cũng có Micronaut | 🔴 **Thiếu hẳn** |
| 3 | "NoSQL: **MongoDB, Cassandra** hoặc TimescaleDB" cho log/test result | Katalon là **PostgreSQL-first**: AWS **Aurora PostgreSQL** + RDS, extension **pgvector / TimescaleDB / PostGIS**, **Amazon Redshift** cho warehouse. **Không có dấu hiệu nào** dùng MongoDB/Cassandra | 🔴 **Sai công nghệ** |
| 4 | Chỉ nhắc **Kafka** cho xử lý bất đồng bộ | JD Lead yêu cầu rõ **"large-scale data processing, batch and real-time"** với **Apache Spark / PySpark**. Kafka có, nhưng Spark là phần bị bỏ sót | 🔴 **Thiếu mảng lớn** |
| 5 | **Không nhắc gì về AI/LLM** | Katalon tự định vị là **"AI-augmented software testing platform"**. Sản phẩm mũi nhọn **TrueTest** = AI tự động discover/model/generate/maintain test case. JD ghi rõ: **prompt engineering, LLM API integration, agent-based design patterns**, dùng **Claude Code / Cursor / Copilot** | 🔴 **Bỏ sót điểm mạnh nhất của bạn** |
| 6 | Ngầm định vòng phỏng vấn nặng **LeetCode** | Glassdoor: **3–4 vòng**, gồm HR screen → technical/hiring manager → **take-home assignment** → stakeholder. Ứng viên mô tả là **"case study + discussion"**, không phải grind thuật toán. Tổng **2–3 tuần** | 🟡 **Sai phân bổ thời gian** |

**Phần checklist tham khảo nói ĐÚNG và đáng giữ nguyên:**

- ✅ Tư duy trade-off là thứ phân biệt Senior với Lead — chính xác 100%.
- ✅ Clarification trước khi code, naive-first-then-optimize — đúng quy trình.
- ✅ SOLID + Factory/Strategy/Observer với ví dụ thực tế.
- ✅ Concurrency: Thread Pool, Race Condition, Deadlock, **Virtual Threads (Java 21)** — rất đúng, Katalon dùng Java 21.
- ✅ Tree/Graph ứng dụng vào **DOM tree** và **test workflow** — đúng domain một cách đáng ngạc nhiên.
- 🟡 **Trie**: hợp lý (auto-complete trong Studio, matching selector) nhưng **ROI thấp hơn** Hash/Tree/Graph. Đừng dành quá 1 buổi.

---

## 1. Sự thật về Katalon (đã verify)

### 1.1 Sản phẩm — bạn phải nói được cái này trong 60 giây

| Sản phẩm | Bản chất | Tech signal |
|---|---|---|
| **Katalon Studio** | IDE desktop, xây trên **Eclipse RCP**, script bằng **Groovy/Java**, wrap Selenium/Appium | Java core: Swing/JavaFX, JDBC, RMI, Concurrency |
| **Katalon Runtime Engine** | CLI/headless runner để chạy test trong CI | Container, K8s |
| **Katalon Platform / TestOps** | **SaaS** — orchestration, phân tích, báo cáo test ở quy mô lớn | Java + Quarkus + React + Kafka + PostgreSQL |
| **Katalon TrueTest** ⭐ | **"AI-augmented test automation solution that automatically discovers, models, generates, and maintains user-journey test cases"** | LLM, agent patterns, Spark, web instrumentation |
| **Katalon Recorder** | Browser extension record/playback | JS/TS, CDP |

> **TrueTest là chìa khóa.** Đọc kỹ định nghĩa của nó: *discover → model → generate → maintain* user-journey test case một cách tự động. Nghĩa là: thu thập hành vi người dùng thật từ browser → mô hình hóa thành journey → LLM sinh ra test case → tự bảo trì khi UI đổi. **Toàn bộ system design + AI phần dưới đều xoay quanh bài toán này.**

#### 1.1.1 Katalon Studio — chi tiết (verified: [Wikipedia](https://en.wikipedia.org/wiki/Katalon_Studio), [StackShare](https://stackshare.io/katalon-llc/studio))

**Đây là sản phẩm gốc, ra đời trước** (nội bộ 01/2015, public 09/2016; bản quyền chia free/paid từ v7, 10/2019) — khác hẳn TrueTest về bản chất: Studio là **desktop IDE** cài trên máy, TrueTest là **SaaS**.

- **Kiến trúc:** UI dựng trên **Eclipse RCP** (Rich Client Platform) — cùng nền tảng với IntelliJ/Eclipse IDE thật (SWT, không phải web app), không phải Electron/web-wrapper.
- **Ngôn ngữ chính:** **Groovy** (dynamic language chạy trên JVM, cú pháp gần Java nhưng cho phép gọi trực tiếp bytecode/thư viện Java) + Java thuần khi cần. Cho phép **import jar Java bên ngoài** làm custom keyword — đây là lý do Studio "wrap" được cả ngôn ngữ script động và ecosystem Java.
- **Nền automation:** không tự viết driver — **wrap Selenium WebDriver** (chuẩn W3C WebDriver) cho web, **Appium 2.x** cho mobile (iOS/Android). Studio là lớp trừu tượng phía trên, không phải competitor của Selenium.
- **2 mode song song, chuyển đổi được:** (1) **Manual/Recorder** — record & playback dạng bảng, low-code; (2) **Script view** — Groovy IDE thật, có autocomplete + debug. Đây là điểm khác biệt cốt lõi so với Selenium thuần (chỉ có script).
- **Keyword-driven testing:** thư viện keyword built-in (`WebUI.click`, `WebUI.verifyElementPresent`...) + custom keyword tự viết bằng Groovy, kết hợp **data-driven** (bind Excel/CSV/DB vào test case).
- **Object Repository:** tách locator ra khỏi test script thành 1 layer riêng (Test Object) — giảm chi phí sửa khi UI đổi. **Đây chính là tiền thân ý tưởng "self-healing locator"** mà TrueTest làm tự động hơn bằng AI.
- **Katalon Runtime Engine (KRE):** bản headless/CLI của Studio để chạy test suite trong pipeline CI (Jenkins/GitHub Actions/Azure Pipelines) — chỗ nối giữa việc author test (Studio) và JD yêu cầu *"CI/CD, GitHub Actions"*.
- **Vì sao liên quan tới vị trí Lead bạn ứng tuyển:** Studio là lớp Java/Groovy **client-side cũ**, phần lớn logic nằm trong desktop app, không phải nơi có backend service Java/Quarkus mà JD nhắc. Backend service thật (Quarkus/Spring Boot, distributed system, Kafka, Spark) nằm ở **TestOps/TrueTest** — 2 sản phẩm SaaS mới hơn. Nói cách khác: JD "Lead Software Engineer" đang tuyển cho **tầng platform/AI phía sau**, không phải cho Studio.

#### 1.1.2 Katalon TrueTest — chi tiết (verified: [Katalon blog](https://katalon.com/resources-center/blog/katalon-truetest-improve-qa), [Katalon AI Features](https://katalon.com/ai-powered-testing-platform), [Product Roundup 05-06/2026](https://katalon.com/resources-center/blog/katalon-product-roundup-june-2026))

TrueTest **không đứng riêng** — nó là 1 trong 5 sản phẩm dưới cùng **"Katalon Platform"**: Studio, TestOps, TestCloud, TrueTest, Production Insights/Recorder — chia sẻ chung tài khoản/workspace.

**Luồng hoạt động (mô tả chính thức, không phải suy diễn):**

1. **Capture** — bắt hành vi **người dùng thật trên production** (không phải test giả).
2. **Model** — tổng hợp các hành vi phổ biến thành **user journey map**.
3. **Generate** — tự sinh test case + test data cover các luồng phổ biến nhất trong journey map.
4. **Measure coverage theo hành vi thật** — đo coverage dựa trên journey map thật của user, không dựa trên coverage code như tool truyền thống. Đây là khác biệt định vị lớn nhất của TrueTest.

> ⚠️ **Giới hạn thông tin quan trọng:** các trang marketing của Katalon **không công khai** cơ chế capture kỹ thuật (không xác nhận rõ SDK/browser extension/CDP cụ thể) và **không công khai** kiến trúc backend (Kafka/Spark/K8s...). Toàn bộ phần kiến trúc chi tiết ở [mục 5.1](katalon-system-design/01-truetest-journey-mining.md) là **bài tập thiết kế giả định của bạn**, suy ra từ JD + domain, **không phải sự thật đã verify từ Katalon**. Nếu bị hỏi ngược *"sao anh biết chi tiết pipeline vậy?"* — trả lời thẳng: *"Đây là thiết kế tôi tự đề xuất dựa trên JD và cách sản phẩm mô tả hành vi, tôi muốn được anh/chị confirm hoặc phản biện."* Đừng nói như thể đó là sự thật đã biết.

**Cập nhật tính năng 2026 (từ Product Roundup — cho thấy hướng phát triển thật):**

- **Regenerate**: sửa 1 test case cũ khi flow đổi, giữ lại phần logic cũ thay vì viết lại từ đầu.
- **Preview split**: xem trước cách 1 test case quá lớn sẽ bị tách thành sub-case trước khi lưu.
- **Object Verification** + **Custom Code Injection**: cho phép chèn check/code tùy biến sâu hơn việc chỉ so sánh UI state.
- **"Run with AI"**: dispatch một **autonomous agent** thực thi test trên app thật — tự click, tự điền field, tự verify state, và **tự "recover" khi UI đổi bất ngờ**. Đây chính là ví dụ thật của *"agent-based design pattern"* mà JD nhắc, không phải khái niệm trừu tượng.

**Nền AI — xác nhận chính thức (khác với marketing chung):**

- Katalon **công khai nói dùng OpenAI GPT models** để xử lý AI trong platform, kèm cam kết *"data không bị lưu lại hoặc dùng để train"* (privacy theo workspace). Đây là chi tiết cụ thể hiếm khi vendor công khai — dùng được khi nói về "AI vendor strategy" trong phỏng vấn.
- **AI Self-Healing** (platform-wide, không riêng TrueTest): tự phát hiện + tự sửa locator vỡ khi chạy test.
- Các tính năng AI khác cùng hệ sinh thái (áp dụng cho cả Studio/TestOps, không riêng TrueTest, nhưng cho thấy bức tranh tổng "AI hóa toàn platform"): **Test Case Generator** (sinh test từ requirement doc/OpenAPI spec), **Autonomous Test Runner** (chạy test viết bằng ngôn ngữ tự nhiên, không cần code), **Root Cause Analyzer** + **Flakiness Detector** (phân biệt lỗi thật vs flaky), **Requirement Analyzer**, **Report & Insight Generator**, **Bug Reporter** (tự tạo ticket vào Jira/Azure DevOps), **Visual Tester**.
- Katalon tự định vị AI là **"trust and accountability layer"**: *"AI decisions must be explainable"*, *"humans are accountable for every release"* — nghĩa là **không auto-merge mù**. Điểm này **khớp trực tiếp** với "validation gate bắt buộc" mà bạn tự thiết kế ở [mục 5.1e](katalon-system-design/01-truetest-journey-mining.md#deep-dive-hay-bị-hỏi) — tức là hướng thiết kế của bạn **đi đúng triết lý sản phẩm thật** của Katalon, có thể tự tin nói ra điều này khi trình bày.

### 1.2 Vị trí Technology đang mở (fetch 13/08/2026)

| Vị trí | Location | Ghi chú |
|---|---|---|
| **Lead Software Engineer (Java, Python, JavaScript)** ⭐ | Vietnam (remote) | **Đây là target chính của bạn** |
| Senior/Lead Software Engineer (Agentic AI, Fullstack, Java) ⭐ | HCMC | Target phụ — khớp background LangGraph/RAG |
| Senior Platform Engineer (DevOps, Kubernetes, AWS) | Vietnam | |
| Senior Database Engineer | Vietnam | Nguồn thông tin về stack DB |
| Product Security Engineer (Cloud, DevSecOps) | Vietnam | |
| Head of Data | Vietnam | |

### 1.3 JD "Lead Software Engineer (Java, Python, JavaScript)" — trích nguyên văn

**Team/Product:** *"Technology team building Katalon's next-generation AI-augmented software testing platform"*

**Responsibilities:**

- *"Design, develop, and maintain scalable backend services and modern web applications using Java, Python, and JavaScript technologies"*
- *"Lead technical design discussions, architecture reviews, and implementation planning"*
- *"Review code and mentor engineers to improve code quality, maintainability, and system reliability"*
- *"Build highly available, secure, and observable distributed systems"*
- *"Investigate and resolve production issues while continuously improving system stability"*

**Required qualifications:**

- *"Strong expertise in **Java**"* — backend với **Quarkus, Spring Boot, hoặc Micronaut**
- *"Proven experience designing and building **highly available, distributed systems**"* — performance & reliability
- *"Hands-on experience with **large-scale data processing** including batch and real-time workloads"*
- *"Strong proficiency in **Python**, with understanding of data structures and algorithms"*
- *"Experience with **web instrumentation and browser automation** including JavaScript/TypeScript and **Playwright**"*
- *"Hands-on experience deploying, operating, and scaling **cloud-native services** using Docker and Kubernetes"*
- CI/CD, automated testing, version control (**GitHub Actions**)
- *"Strong problem-solving and communication skills"* + **mentoring**

**Tech stack tổng hợp:** Java, Python, JS/TS, **Quarkus**, Spring Boot, **Apache Spark / PySpark**, Docker, Kubernetes, GitHub Actions, **Playwright**, PostgreSQL (Aurora), Kafka, React, AWS.

**Nice-to-have:** GitHub Copilot / Cursor / **Claude Code**, **LLM API integration**, **prompt engineering**, SaaS enterprise background, open-source contributions.

### 1.3b Cập nhật 06/09/2026 — hướng tuyển đã dịch sang **AI Engineer** ⭐

> ⚠️ **Nguồn của cập nhật này là bạn, không phải tra cứu.** Tra ITviec ngày 06/09/2026 chỉ thấy
> Katalon đang mở **Product Security Engineer** — không có JD AI Engineer công khai. Hãy lấy JD
> nguyên văn từ nguồn của bạn (recruiter, careers page) rồi đối chiếu lại mục này.

**Trọng tâm mới:** chuyển từ tích hợp **OpenAI / AWS Bedrock / Azure OpenAI** sang **self-host
model** để kéo chi phí suy luận xuống.

**Vì sao điều này đổi hẳn thứ tự ưu tiên ôn tập của bạn:**

| | Trước | Sau |
|---|---|---|
| Trọng số của mảng AI | P1 — điểm cộng | **P0 — trục chính** |
| Playwright/CDP là gap 🔴 | Gap lớn nhất | **Bớt nghiêm trọng** nếu vị trí là AI Engineer thuần |
| Thứ mạnh nhất của bạn | Java + banking | **Self-host LLM đã chạy production** |

**Ba việc bạn đã làm đúng bài họ đang tuyển** — có `.env`, `docker-compose`, git log làm bằng chứng:

- Toàn bộ suy luận runtime chạy trên hạ tầng nội bộ (`OPENAI_API_ENDPOINT` trỏ IP LAN, model Qwen OSS)
- **Migrate serving stack thật**: llama.cpp GGUF → vLLM FP8, không sửa một dòng application code
- **Đo trước khi chuyển**: Qwen tự host khớp Claude **100% action (27/27)** trên tác vụ schema ràng buộc

→ Chi tiết đầy đủ, kèm kinh tế học self-host và 10 câu follow-up:
**[katalon-selfhost-llm.md](katalon-selfhost-llm.md)** ⭐

**Một điểm nối đáng nói:** TrueTest cho phép dùng **AI service đóng gói sẵn hoặc một
OpenAI-compatible API key** — nghĩa là sản phẩm của họ **đã nói đúng giao thức** mà kiến trúc của
bạn dựa vào. Bài toán self-host của họ không phải viết lại tích hợp, mà là dựng và vận hành đầu bên
kia của seam.

### 1.4 Vòng phỏng vấn (Glassdoor, HCMC 2025–2026)

```text
Vòng 1  HR screen (~30 phút)          → background, motivation, expectation lương
Vòng 2  Technical / Hiring Manager    → deep-dive project, case study, design
Vòng 3  TAKE-HOME ASSIGNMENT ⚠️        → điểm quyết định, ít người chuẩn bị kỹ
Vòng 4  Stakeholder / panel           → cross-functional, culture fit, lead signal
Tổng thời gian: 2–3 tuần
```

**Ứng viên nhận xét:** interviewer *friendly & transparent*, quy trình *well-structured*, nội dung là *"questions about background, past experience, key projects, as well as case studies to check your rationale and problem-solving skills"*.

> ⚠️ **Hệ quả quan trọng cho việc phân bổ thời gian:** Không có bằng chứng nào cho thấy Katalon có vòng LeetCode-hard nghiêm ngặt. Nghĩa là:
> - DSA Medium là **điều kiện đủ để không bị loại** (bạn đã có 38 bài) → đừng grind thêm 200 bài.
> - **Take-home + deep-dive project + design** là nơi phân định level và dải lương → dồn 70% thời gian vào đây.
>
> ⚠️ Có 1 report Glassdoor mô tả vòng *"Senior PM & VP of Product"* — nhưng report đó **có thể là cho vị trí Product**, không phải engineering. Đừng coi là chắc chắn.

---

## 2. Bản đồ năng lực: JD → ôn cái gì, ưu tiên nào

Ký hiệu: **P0** = trượt nếu không có · **P1** = quyết định level/lương · **P2** = điểm cộng

| # | Năng lực | Ưu tiên | Nội dung cụ thể phải nắm |
|---|---|:---:|---|
| 1 | **Java 21 + Quarkus/Spring Boot** | **P0** | DI build-time vs runtime, Mutiny `Uni`/`Multi`, native image (GraalVM), record/sealed/pattern matching, JPA/Hibernate + N+1 |
| 2 | **Distributed systems** | **P0** | Idempotency, at-least-once + dedupe, retry/backoff/jitter, circuit breaker, timeout budget, graceful degradation, CAP trade-off cụ thể |
| 3 | **PostgreSQL depth** | **P0** | `EXPLAIN ANALYZE`, index (B-tree/GIN/BRIN/partial/covering), **partitioning**, MVCC + bloat + autovacuum, replication & read replica, connection pooling (PgBouncer), lock contention |
| 4 | **Concurrency Java** | **P0** | **Virtual Threads** (pinning bởi `synchronized`, đừng pool virtual thread), structured concurrency, `CompletableFuture`, `ConcurrentHashMap`, race/deadlock, thread pool sizing |
| 5 | **DSA Medium ứng dụng** | **P0** | Hash, Two Pointers, Sliding Window, **Tree/Graph (BFS/DFS/topo sort)**, DP cơ bản, heap. → **dùng luôn 38 bài đã có** |
| 6 | **Kafka + event-driven** | **P1** | Partition key & ordering guarantee, consumer group rebalance, offset commit strategy, exactly-once vs idempotent consumer, DLQ, compaction, backpressure |
| 7 | **Spark / PySpark** | **P1** | DataFrame vs RDD, **shuffle & data skew** (salting), broadcast join, partitioning, Parquet + predicate pushdown, **Structured Streaming** (watermark, late data), checkpointing |
| 8 | **Playwright + web instrumentation** | **P1** | **CDP (Chrome DevTools Protocol)**, MutationObserver, locator strategy & **self-healing locator**, Shadow DOM/iframe, auto-wait, trace viewer, chống flaky |
| 9 | **System design SaaS multi-tenant** | **P1** | Tenant isolation (row-level vs schema vs DB), noisy neighbor, rate limit/quota, fair scheduling, cost per tenant |
| 10 | **AI/LLM cho testing domain** ⭐ | **P0** ⬆ | Agent pattern (ReAct/plan-execute), tool calling, RAG trên DOM/test corpus, **pgvector**, eval harness cho LLM output, cost/latency/token control, chống hallucination locator. **Nâng từ P1 lên P0** sau cập nhật [§1.3b](#13b-cập-nhật-06092026--hướng-tuyển-đã-dịch-sang-ai-engineer-) |
| 10b | **Self-host LLM & kinh tế suy luận** ⭐ | **P0** 🆕 | Seam OpenAI-compatible; **vLLM vs llama.cpp** (PagedAttention, continuous batching, GGUF vs FP8); VRAM = trọng số + **KV cache** và vì sao `max-model-len` là nút vặn đầu tiên; **constrained decoding** để model yếu không phá JSON schema; điểm hoà vốn cloud-vs-self-host và **người chiếm 25–30% TCO**; model tiering; LoRA — biết khi nào **chưa** nên. → [katalon-selfhost-llm.md](katalon-selfhost-llm.md) |
| 10c | **Migrate an toàn + eval + scale** 🔴 | **P0** 🆕 | **Nhóm đã làm bạn trượt.** Item-level regression thay vì accuracy tổng; bất định do batch ở temperature 0; cỡ mẫu `n≈16p(1−p)/δ²`; Fisher exact + FDR + permutation null; shadow bất đồng bộ → canary theo tenant; CI gate 3 tầng; VRAM = trọng số + **KV cache**; Little's Law; **admission control** và nghịch lý "không từ chối ai"; prefix-aware routing. → [katalon-llm-migration-va-scale.md](katalon-llm-migration-va-scale.md) |
| 11 | **K8s + observability** | **P1** | Job/CronJob, HPA/KEDA, resource request vs limit, OOMKilled, readiness vs liveness, OpenTelemetry trace/metric/log, SLO/error budget |
| 12 | **Clean code + SOLID + patterns** | **P1** | Strategy (multi-engine runner), Factory, Observer, Adapter; code review standard; **SonarQube** rule thật (S1149, S1104, S107, cognitive complexity) |
| 13 | **Lead/mentoring** | **P1** | Cách bạn review code, dẫn design review, xử lý disagreement, onboard người mới, chia task, viết RFC/ADR |
| 14 | **Profiling & perf** | **P2** | Heap dump + MAT, **async-profiler**, JFR, GC (G1 vs ZGC), `-XX:MaxRAMPercentage` trong container, benchmark bằng JMH |
| 15 | **Security** | **P2** | OWASP Top 10, secret management, **PII redaction khi record session người dùng thật** (rất on-domain!) |
| 16 | Trie | **P2** | Auto-complete, prefix matching selector. 1 buổi là đủ |

---

## 3. Đánh giá điểm mạnh / gap của bạn

> Suy ra từ dấu vết repo `motivesidp-ai-learning` (LangChain/LangGraph, FastAPI+Qdrant, RAG, agent bakeoff RFC) và bộ 38 bài Python+Java. **Bạn tự xác thực lại**.

**Điểm mạnh — dùng làm vũ khí chính:**

| Bằng chứng trong repo | Map vào JD Katalon |
|---|---|
| LangGraph agent 14 nodes, ReAct agent hoàn chỉnh | *"agent-based design patterns"*, *"LLM integration"* — **đúng mũi nhọn TrueTest** |
| **Bakeoff RFC-001/RFC-002 → tự REJECT RFC của mình dựa trên số liệu** | Đây là **story lead-level xuất sắc**: data-driven decision + intellectual honesty. Kể story này ở vòng stakeholder |
| Refactor hardcode → config/`.env`, phát hiện "config đóng đinh theo tên sheet" | *"code quality, maintainability"* + tư duy extensibility |
| 53/53 test pass, ruff clean, docs cập nhật cùng code | *"strong unit testing"*, engineering discipline |
| 38 bài Java 21 dùng `record`, `ArrayDeque` thay `Stack`, tự rà SonarQube rule | Chứng minh **clean code Java** có chủ đích, không phải dịch máy từ Python |
| FastAPI + Qdrant (vector search) | Chuyển sang **pgvector** rất nhanh — Katalon đã có pgvector trên Aurora |
| Python + Java song song | Khớp đúng *"Java, Python, JavaScript"* |

**Gap phải bịt — theo thứ tự nguy hiểm:**

| Gap | Vì sao chết | Bịt thế nào |
|---|---|---|
| 🔴 **Java production-grade** (Quarkus/Spring Boot thật, không phải file thuật toán) | JD ghi *"**Strong expertise in Java**"* là yêu cầu **số 1**. Viết được `TwoSum.java` ≠ build được service | Sprint 1 của lộ trình 2 tháng: build 1 service Quarkus thật |
| 🔴 **Apache Spark / PySpark** | Yêu cầu cứng, và trong repo **không có dấu vết nào** | Sprint 3 + ngày 6 của sprint 1 tuần |
| 🔴 **Playwright + web instrumentation / CDP** | Yêu cầu cứng, và là **domain cốt lõi** của Katalon | Sprint 4 + ngày 6 |
| 🟡 **Distributed systems ở quy mô SaaS** | Repo là agent app, không thấy multi-tenant/HA | Học qua 3 bài design ở mục 5 |
| 🟡 **JS/TS frontend (React)** | JD Lead cần *"modern web applications"* | Đủ để đọc/review + build dashboard nhỏ |

> **Chiến lược định vị:** Đừng cố giả vờ là Java veteran 10 năm. Định vị bạn là **"Lead có chiều sâu AI/agentic thật, đang vận hành trên cả Python và Java, đến để làm TrueTest"**. Đó chính xác là người mà JD *"next-generation AI-augmented testing platform"* đang tìm — và là thứ đa số ứng viên Java thuần **không có**.

---

## 4. Lộ trình ngắn — 7 ngày (13/08 → 20/08/2026)

**Giả định:** ~4h/ngày T2–T6, ~8h T7–CN → **tổng ~36h**. Mỗi ngày có **deliverable ghi ra file** — không đọc suông.

Tạo trước folder làm việc:

```bash
mkdir -p katalon-prep/{design,java-notes,stories,spark,playwright}
```

### Ngày 1 — Recon + trung thực với chính mình (4h)

| Thời lượng | Việc | Deliverable |
|---|---|---|
| 60' | Đọc lại JD Lead ở mục 1.3, **tự chấm điểm 1–5** cho từng dòng requirement | `katalon-prep/self-assessment.md` |
| 60' | Dùng thử **TrueTest** (trial) + đọc doc TestOps. Ghi lại: nó làm gì, chỗ nào bạn thấy còn yếu | `katalon-prep/product-notes.md` |
| 60' | Viết **narrative 90 giây**: "Tôi là ai, vì sao Katalon, vì sao là TrueTest" | `katalon-prep/pitch.md` |
| 60' | Rà repo, chọn **3 project** để deep-dive. Với mỗi cái: bài toán → kiến trúc → trade-off → số liệu → điều học được | `katalon-prep/stories/deep-dive-{1,2,3}.md` |

**Checkpoint:** Nói được pitch 90s không cần đọc.

### Ngày 2 — Java 21 + Quarkus/Spring Boot (4h)

| Thời lượng | Việc |
|---|---|
| 90' | **Quarkus vs Spring Boot vs Micronaut**: build-time DI vs reflection, startup/RSS, GraalVM native, Mutiny `Uni`/`Multi`. Trả lời được: *"Vì sao Katalon chọn Quarkus cho SaaS trên K8s?"* (gợi ý: density → cost/pod) |
| 90' | **Concurrency**: Virtual Threads — pinning bởi `synchronized` (dùng `ReentrantLock`), **không pool virtual thread**, structured concurrency; `CompletableFuture`; `ConcurrentHashMap`; deadlock 101 |
| 60' | **Hibernate/JPA**: N+1 và cách phát hiện, `@EntityGraph`, lazy vs eager, batch insert, transaction boundary + isolation level |

**Deliverable:** `java-notes/cheatsheet.md` — 2 trang, viết bằng chữ của bạn.

### Ngày 3 — DSA tập trung, không grind (4h)

Không học bài mới. **Dùng lại [38 bài](../leetcode-38-bai-phong-van-vietnam.md)**, chỉ chọn subset on-domain và luyện **nói to trong lúc code**:

| Nhóm | Bài (theo doc 38 bài) | Lý do on-domain |
|---|---|---|
| Tree/Graph | #102, #105, #236, #98, #297, #207 | DOM tree, test suite tree, dependency graph, **topo sort cho test dependency** |
| Hash/Prefix | #1, #560, #523, #49, #128 | dedupe event, session aggregation |
| Sliding Window | #3, #76 | phân tích log theo cửa sổ thời gian |
| Heap | #215, #253, #347 | **#253 Meeting Rooms II ≈ scheduling test song song vào executor pool** — nói được liên hệ này là điểm cộng lớn |
| Design | #146 LRU, #706 HashMap | cache layer, hiểu bản chất |

**Cách luyện (quan trọng hơn số bài):** 25 phút/bài, bấm giờ, **nói to**: clarify input/edge case → naive O(n²) → chỉ ra bottleneck → optimize → complexity → test case. Tự quay màn hình 2 bài, xem lại.

**Deliverable:** 8–10 bài đã luyện theo format trên, 2 video tự review.

### Ngày 4 — System Design #1: TrueTest (4h)

Làm **trọn vẹn** bài "Design TrueTest — AI journey mining" ở [mục 5.1](katalon-system-design/01-truetest-journey-mining.md). Tự vẽ trên giấy/Excalidraw trước, rồi đối chiếu.

**Deliverable:** `design/truetest.md` + 1 diagram. Trình bày được trong **35 phút**.

### Ngày 5 — System Design #2 + #3 (4h)

- 2h: [Distributed Test Execution Platform](katalon-system-design/02-distributed-test-execution.md)
- 2h: [Real-time Test Analytics](katalon-system-design/03-realtime-analytics-dashboard.md)

**Deliverable:** `design/execution.md`, `design/analytics.md`. Mỗi bài **phải có mục "Trade-offs" riêng với ít nhất 4 cặp đánh đổi**.

### Ngày 6 — Bịt 2 gap cứng: Spark + Playwright (8h)

| Thời lượng | Việc | Deliverable |
|---|---|---|
| 3h | **PySpark hands-on**: đọc 1 file Parquet event log giả (tự sinh 5–10M dòng), làm sessionization + đếm page transition. Cố tình tạo **data skew** rồi fix bằng salting. Đọc `explain()` | `spark/sessionize.py` + notes về shuffle/skew |
| 3h | **Playwright hands-on** (TS): viết 3 test trên 1 site demo. Bật **trace viewer**. Thử `page.context().newCDPSession()` để bắt DOM mutation / network. Viết 1 hàm **self-healing locator** đơn giản (thử `data-testid` → `role` → `text` → fallback) | `playwright/` + notes về locator strategy |
| 2h | **Take-home dry run**: tự đặt 1 đề giống Katalon (xem [mục 7](#7-playbook-cho-vòng-take-home-vòng-quyết-định)), làm trong 2h có tính giờ, viết README | `katalon-prep/takehome-dryrun/` |

**Checkpoint:** Nói được *"vì sao locator hay flaky và cách hệ thống tự chữa"* — đây là câu hỏi domain rất dễ bị hỏi.

### Ngày 7 — STAR + mock full loop (8h)

| Thời lượng | Việc |
|---|---|
| 2h | Viết **6 STAR story** (xem [mục 6](#6-vòng-behavioral--lead-signal)): conflict, mentoring, sai lầm production, quyết định data-driven (dùng **bakeoff RFC** của bạn), trade-off dưới deadline, dẫn design review |
| 2h | **Mock vòng technical**: nhờ người khác (hoặc AI) hỏi 45' — 1 coding + 1 design. Ghi âm |
| 2h | **Mock vòng stakeholder**: câu hỏi behavioral + *"bạn sẽ làm gì trong 90 ngày đầu"* |
| 1h | Soạn **8 câu hỏi hỏi lại interviewer** ([mục 8](#8-câu-hỏi-nên-hỏi-lại-interviewer)) |
| 1h | Rà logistics: CV có nhắc Quarkus/Spark/Playwright chưa? GitHub public sạch chưa? Setup phòng/mic/mạng |

**Checkpoint cuối tuần — pass hết mới gọi là sẵn sàng:**

- [ ] Pitch 90s trôi chảy, có nhắc TrueTest
- [ ] Vẽ + trình bày 1 bài design 35' có ≥4 trade-off
- [ ] Giải 1 bài Medium 25' vừa code vừa nói
- [ ] Giải thích Quarkus vs Spring Boot theo góc **cost trên K8s**
- [ ] Giải thích shuffle/skew trong Spark bằng ví dụ mình tự chạy
- [ ] Giải thích self-healing locator + vì sao test flaky
- [ ] 6 STAR story, mỗi story có **số liệu**
- [ ] Nêu được 1 điểm bạn thấy sản phẩm Katalon có thể tốt hơn (thể hiện đã dùng thật)

---

## 5. Ba bài System Design on-domain

> **Đã tách ra folder riêng:** [katalon-prep/katalon-system-design/](katalon-system-design/)
> — mỗi bài một file, có bản đồ **6 họ bài** và **4 trục nhận diện** để xử lý cả đề chưa từng thấy.
> Mục này giữ lại khung 45 phút và bảng dẫn đường; nội dung chi tiết nằm ở các file con.

### Quy trình chuẩn 45 phút (áp cho cả 3 bài)

```text
 5' Clarify      → functional / non-functional / scale / constraint. HỎI, đừng đoán
 5' Estimate     → QPS, storage/tháng, bandwidth, cost. Ghi số lên bảng
 5' API + model  → contract trước, schema sau
15' High-level   → vẽ box & arrow, đi theo đường dữ liệu
10' Deep-dive    → interviewer chọn 1 component, đào sâu
 5' Trade-off + failure mode + "nếu scale 10x thì gì vỡ trước"
```

> **Luật vàng cho level Lead:** mỗi lựa chọn kiến trúc phải kèm 1 câu *"đánh đổi là..."*. Không có câu đó = nói như Senior, không phải Lead.

### Ba bài — đọc ở đâu

| Bài | Họ bài | Chi tiết |
|---|---|---|
| **5.1 TrueTest — AI journey mining** ⭐ bài quan trọng nhất | Thu thập luồng sự kiện + LLM trong vòng lặp | [01-truetest-journey-mining.md](katalon-system-design/01-truetest-journey-mining.md) |
| **5.2 Distributed Test Execution Platform** | Điều phối tác vụ & chia tài nguyên hữu hạn (fairness) | [02-distributed-test-execution.md](katalon-system-design/02-distributed-test-execution.md) — có thêm bản đào sâu *"10.000 test cùng lúc, multi-tenant"* |
| **5.3 Real-time Test Analytics Dashboard** | Đếm & tổng hợp theo thời gian | [03-realtime-analytics-dashboard.md](katalon-system-design/03-realtime-analytics-dashboard.md) |

**Thêm một bài không có trong danh sách gốc — vì nó đã được hỏi thật:**

| Bài | Vì sao quan trọng |
|---|---|
| [**04 — Event Counting 10k request/phút**](katalon-system-design/04-event-counting-10k.md) ⭐ | **Câu đã bị hỏi ở vòng Principal** và câu trả lời tại chỗ chưa được chấp nhận. File này mổ xẻ 4 lỗ hổng, dựng lại lời giải đúng từ design cơ bản tới dòng code, so 3 tech stack kèm ngưỡng chuyển, và có demo chạy được từng bước |

**Và một kho bằng chứng, không phải đề bài:**
[05 — Hệ thống thật all-in-one trên AWS](katalon-system-design/05-he-thong-that-allinone-aws.md)
— ECS/ALB/autoscaling/Terraform thật đã verify, để dẫn chứng khi trả lời bất kỳ bài design nào.


## 6. Vòng behavioral / lead signal

Chuẩn bị **6 story theo STAR**, mỗi story **phải có số liệu**. Gợi ý map từ repo của bạn:

| Câu hỏi thường gặp | Story nên dùng | Điểm nhấn |
|---|---|---|
| "Kể về một quyết định kỹ thuật bạn đưa ra dựa trên dữ liệu" | **Bakeoff RFC-001/002: bạn tự reject RFC của mình** sau khi đo | Data > ego. Nêu rõ tiêu chí đo, kết quả, và việc bạn đóng track |
| "Kể về lần bạn sai" | Bug retry loop: flag `ENABLE_REPLAN` tưởng hoạt động nhưng 2 chỗ khác set `verdict=RETRY` vượt flag | Cách bạn tìm root cause, và bài học: flag phải có **một** điểm thực thi |
| "Bạn nâng chất lượng code của team thế nào" | Refactor hardcode → config; đặt convention 1-file-1-prompt có version trên git | Nêu được **nguyên tắc** bạn thiết lập, không chỉ việc bạn làm |
| "Bạn mentor thế nào" | Review code + viết ADR/RFC để người sau hiểu **vì sao** | Lead = scale được quyết định qua tài liệu |
| "Bất đồng với đồng nghiệp/PM" | Chọn story bạn **đổi ý** sau khi nghe lập luận đối phương | Disagree & commit |
| "Trade-off dưới deadline" | Chọn giải pháp static thay vì agentic đầy đủ vì đo thấy chưa đủ tin cậy | Chọn cái đúng, không chọn cái ngầu |

**Câu hỏi riêng cho Lead — chuẩn bị trước:**

- *"90 ngày đầu ở Katalon bạn làm gì?"* → 30 ngày đọc code + ship 1 thay đổi nhỏ có ý nghĩa + map ownership; 60 ngày dẫn 1 feature; 90 ngày đề xuất 1 cải tiến kiến trúc **có số đo**.
- *"Team bạn phải chọn giữa ship nhanh và làm đúng?"* → nói về **reversibility**: quyết định dễ đảo thì ship nhanh, quyết định khó đảo (schema, API public, data model) thì làm đúng.
- *"Bạn dùng AI coding assistant thế nào?"* → JD ghi rõ đây là kỳ vọng. Nói cụ thể: dùng Claude Code cho refactor lớn/viết test, **nhưng** review từng dòng, không để AI quyết định kiến trúc, và có test gate. Bạn có bằng chứng thật trong repo.

---

## 7. Playbook cho vòng Take-home (vòng quyết định)

Đây là chỗ **dễ vượt người khác nhất** vì nhiều ứng viên chỉ làm cho chạy được.

**Đề có khả năng cao (đoán từ domain Katalon):**

- Viết service nhận webhook kết quả test → lưu → expose API thống kê (Java/Quarkus + Postgres)
- Parse file log/JUnit XML lớn → sinh report, có yêu cầu về hiệu năng/bộ nhớ
- Viết crawler/instrumentation nhỏ bằng Playwright, xuất ra structured data
- Cho 1 đoạn code tệ → refactor + giải thích

**Checklist bắt buộc có (đừng thiếu cái nào):**

- [ ] **README dẫn dắt được người đọc**: cách chạy (1 lệnh: `docker compose up`), quyết định thiết kế, **mục "Trade-offs & What I'd do with more time"** ⭐
- [ ] **Test thật**: unit + 1 integration (Testcontainers cho Postgres). Ghi rõ coverage phần logic quan trọng
- [ ] **Xử lý lỗi & edge case** rõ ràng, không `catch (Exception e) {}`
- [ ] **Observability tối thiểu**: structured log có correlation id, 1 health endpoint
- [ ] **Docker + docker-compose** chạy được từ máy sạch
- [ ] **CI**: 1 file GitHub Actions chạy build + test (JD nhắc GitHub Actions)
- [ ] **Git history sạch**: commit nhỏ, message có nghĩa — họ sẽ đọc, đây là tín hiệu Lead
- [ ] **Không over-engineer**: đừng nhét Kafka + K8s vào bài 4h. Thay vào đó **viết trong README** là ở production sẽ dùng gì và vì sao — thể hiện tư duy mà không phình code
- [ ] Nếu dùng AI assistant: **nói ra** trong README, kèm cách bạn verify. Trung thực > giả vờ

> **Mẹo:** mục *"Trade-offs & What I'd do with more time"* trong README là thứ khiến reviewer nhớ bạn. Viết 5–8 gạch đầu dòng thật, có suy nghĩ.

---

## 8. Câu hỏi nên hỏi lại interviewer

Chọn 3–4 câu phù hợp vòng. Câu hỏi tốt = tín hiệu Lead.

1. TrueTest hiện lấy dữ liệu hành vi từ production của khách hay staging? Bài toán PII/compliance được xử lý ở đâu trong pipeline?
2. Tỉ lệ test case do TrueTest sinh ra được khách **giữ lại** sau 30 ngày là bao nhiêu — và team đo "chất lượng test sinh tự động" bằng metric gì?
3. Katalon dùng cả Quarkus và Spring Boot — tiêu chí chọn cho service mới là gì? Có đang hợp nhất về một hướng?
4. Với vai trò Lead này, ranh giới ownership so với Engineering Manager và Architect nằm ở đâu?
5. Team đang đau nhất ở đâu: throughput ingest, chi phí LLM, độ tin cậy của test sinh ra, hay tốc độ ship?
6. Quy trình quyết định kiến trúc: có RFC/ADR không? Ai là người ký cuối?
7. Team dùng AI coding assistant tới mức nào, và có guardrail gì cho code do AI sinh vào production?
8. 6–12 tháng tới, thành công của người vào vị trí này được đo bằng gì cụ thể?

---

## 9. Lộ trình dài — 2–3 tháng (13/08 → giữa 11/2026)

6 sprint × 2 tuần. Mỗi sprint có **artifact đưa lên GitHub được**. Toàn bộ hội tụ về **1 capstone** ở mục 9.7.

### 9.1 Sprint 1 (13/08 – 27/08) — Java production-grade

**Mục tiêu:** từ "viết được Java" → "build được service Java đưa lên production".

- Build 1 REST service **Quarkus**: Panache/Hibernate + PostgreSQL, Flyway migration, validation, exception mapper, OpenAPI, `@Transactional` đúng chỗ.
- Làm lại **cùng service** bằng Spring Boot → viết so sánh: startup time, RSS, DX, khi nào chọn cái nào.
- Concurrency lab: dựng endpoint chậm, so sánh platform thread pool vs **virtual thread**; **tự tạo** deadlock + race condition rồi fix; đo bằng JMH.
- GraalVM native image: build, đo, ghi lại chỗ vỡ (reflection).
- **Artifact:** `quarkus-vs-spring-lab/` + benchmark có số thật.

### 9.2 Sprint 2 (27/08 – 10/09) — Distributed + Kafka + Postgres depth

- Kafka: producer/consumer với Quarkus (SmallRye Reactive Messaging); tự gây **consumer rebalance**, ordering violation, duplicate → fix bằng idempotent consumer + DLQ.
- Postgres: sinh 50M dòng; luyện `EXPLAIN (ANALYZE, BUFFERS)`; so sánh index type; **partition theo thời gian**; gây lock contention rồi đọc `pg_stat_activity`; test replica lag.
- Resilience: timeout, retry + jitter, circuit breaker (SmallRye Fault Tolerance), bulkhead.
- Observability: OpenTelemetry trace xuyên service → Jaeger; định nghĩa 3 SLO.
- **Artifact:** `distributed-lab/` + `postgres-tuning-notes.md`.

### 9.3 Sprint 3 (10/09 – 24/09) — Data at scale: Spark/PySpark

- PySpark trên dataset 100M+ dòng event: sessionization, window function, page-transition graph.
- **Shuffle & skew**: đo, salting, broadcast join, AQE. Đọc Spark UI thành thạo (stage/task/spill).
- Parquet: partition pruning, predicate pushdown, file-size tuning (bài toán small-file).
- **Structured Streaming**: Kafka → aggregate có watermark → sink idempotent. Xử lý late data.
- Sequence mining (PrefixSpan) để tìm journey phổ biến.
- **Artifact:** `spark-journey-mining/` + notes về skew có số trước/sau.

### 9.4 Sprint 4 (24/09 – 08/10) — Browser automation & instrumentation

- Playwright (TS) sâu: locator strategy, auto-wait, fixture, parallel + sharding, trace viewer, network intercept.
- **CDP trực tiếp**: `newCDPSession()`, `DOM`/`Network`/`Runtime` domain; MutationObserver để bắt DOM change.
- Xây **instrumentation SDK nhỏ**: bắt click/input/nav → event stream có schema, kèm **PII redaction tại client**.
- Xây **self-healing locator**: nhiều chiến lược + scoring; đo tỉ lệ chữa được khi bạn cố tình đổi DOM.
- Nghiên cứu flakiness: nguyên nhân (race, animation, network, thứ tự test) → cách phát hiện thống kê.
- **Artifact:** `web-instrumentation-sdk/` + báo cáo tỉ lệ self-heal.

### 9.5 Sprint 5 (08/10 – 22/10) — Agentic AI cho testing domain

Đây là sprint bạn có lợi thế — đẩy nó thành **thứ khác biệt**, không chỉ đủ dùng.

- Agent sinh Playwright test từ journey: plan → generate → **self-critique** → chạy sandbox → sửa nếu fail.
- RAG với **pgvector** trên Aurora-compatible Postgres: index DOM snapshot + test corpus; so sánh với Qdrant (bạn đã biết) và **viết ra trade-off**.
- **Eval harness** — phần quan trọng nhất: dataset journey → đo pass-rate, flakiness (chạy 3 lần), độ giống test người viết, cost/token, latency. Có bảng số.
- Cost/latency control: cache theo DOM signature, model tiering, DOM pruning trước khi vào prompt.
- Guardrail: không auto-merge, confidence threshold, human-in-the-loop.
- **Artifact:** `ai-test-generator/` + `EVAL.md` có bảng số thật.

### 9.6 Sprint 6 (22/10 – 05/11) — Lead skill + đánh bóng

- Viết **3 ADR** cho các quyết định trong capstone (format: context → options → decision → consequences).
- Viết **1 RFC** đề xuất kiến trúc, tự phản biện phần "Alternatives considered" và "Risks".
- Tự code-review capstone bằng checklist bạn tự soạn; chạy SonarQube/SonarLint thật, ghi lại issue và cách xử lý.
- Luyện DSA giữ nhiệt: 3 bài/tuần từ [38 bài](../leetcode-38-bai-phong-van-vietnam.md), vừa code vừa nói.
- 4 mock interview: 2 design, 1 coding, 1 behavioral. Ghi âm, xem lại.
- Cập nhật CV/LinkedIn: **Quarkus, Spark/PySpark, Playwright, pgvector, agentic AI** — dùng đúng từ khóa JD, mỗi bullet có số.
- Viết 1 bài blog/README về capstone → dùng làm "open-source contribution" signal mà JD nhắc.

### 9.7 Capstone xuyên suốt: **mini-TrueTest** ⭐

Xây một hệ thống nhỏ nhưng **đúng hình dạng sản phẩm Katalon**. Đây là thứ bạn mang vào phòng phỏng vấn.

```text
[Demo web app]  (1 app e-commerce nhỏ tự viết)
      │  instrumentation SDK (Sprint 4) — TS + CDP, có PII redaction
      ▼
[Quarkus collector]  (Sprint 1) ──► [Kafka] (Sprint 2)
                                       ├──► Spark Streaming → TimescaleDB → dashboard React
                                       └──► S3/MinIO Parquet
                                              ▼
                                    [Spark batch journey mining] (Sprint 3)
                                              ▼
                                    [Postgres + pgvector] (Sprint 5)
                                              ▼
                                    [LLM agent sinh Playwright test] (Sprint 5)
                                              ▼
                                    [Sandbox validation + self-healing locator] (Sprint 4)
                                              ▼
                                    [React dashboard: journey, test sinh ra, flakiness]
```

Toàn bộ chạy bằng `docker compose up`, có CI GitHub Actions, có `EVAL.md` với số đo, có 3 ADR.

**Vì sao capstone này thắng:** nó cover **đúng từng dòng** requirement của JD — Java/Quarkus, Python/Spark, JS/TS/Playwright, distributed, Docker/K8s, CI/CD, LLM integration, agent pattern — và chứng minh bạn hiểu **domain testing**, không chỉ hiểu công nghệ.

---

## 10. Bảng tự đánh giá độ sẵn sàng

Chấm 1–5. **Senior cần ≥3 ở tất cả P0. Lead cần ≥4 ở P0 và ≥3 ở P1.**

| Năng lực | P | Điểm | Bằng chứng cụ thể của bạn |
|---|:---:|:---:|---|
| Java 21 + Quarkus/Spring Boot production | P0 | ⬜ | |
| Distributed systems & resilience | P0 | ⬜ | |
| PostgreSQL depth (index/partition/tuning) | P0 | ⬜ | |
| Concurrency (virtual thread, race, deadlock) | P0 | ⬜ | |
| DSA Medium + nói to khi giải | P0 | ⬜ | |
| System design 45' có trade-off | P0 | ⬜ | |
| Kafka & event-driven | P1 | ⬜ | |
| Spark / PySpark | P1 | ⬜ | |
| Playwright + instrumentation + CDP | P1 | ⬜ | |
| Multi-tenant SaaS design | P1 | ⬜ | |
| AI/LLM + agent cho testing | P1 | ⬜ | |
| K8s + observability | P1 | ⬜ | |
| Clean code / SOLID / patterns | P1 | ⬜ | |
| Lead: mentoring, review, ADR/RFC | P1 | ⬜ | |
| Profiling & performance | P2 | ⬜ | |
| Security & PII/compliance | P2 | ⬜ | |

---

## 11. Chiến thuật trong phòng phỏng vấn (giữ nguyên từ checklist gốc — phần này đúng)

1. **Clarify trước, code sau.** 3–5 phút hỏi về scale (request/s? TB?), user, ràng buộc. Đừng đoán.
2. **Naive trước, tối ưu sau.** Trình bày giải pháp chạy được → chỉ ra bottleneck → cải tiến từng bước. Interviewer cần thấy **quá trình**, không chỉ đáp án.
3. **Luôn nói trade-off.** *"Chọn A thì consistency cao nhưng latency tăng; chọn B thì ngược lại — với use case này tôi chọn A vì..."* Câu **"vì..."** là phần quan trọng nhất.
4. **Nói to (Think Out Loud).** Im lặng 2 phút = interviewer không biết bạn đang nghĩ hay đang bí.
5. **Dùng số.** "Khoảng 15K event/s, ~4TB/tháng sau nén" mạnh hơn "rất nhiều dữ liệu".
6. **Không biết thì nói không biết** — rồi nói cách bạn sẽ tìm ra. Bịa ở level Lead là chí tử.
7. **Neo về domain Katalon.** Mỗi khi có cơ hội, liên hệ về test automation / TrueTest. Nó cho thấy bạn đến vì công ty này, không phải đi rải CV.

---

## 12. Nguồn tham khảo

- [Katalon Careers — trang tuyển dụng chính thức](https://katalon.com/careers)
- [Katalon — Lead Software Engineer (Java, Python, JavaScript)](https://katalon.com/careers/job/8111218)
- [Katalon — Senior Database Engineer](https://katalon.com/careers/job/8051235)
- [Katalon — tất cả vị trí Technology](https://katalon.com/careers/all-jobs?d=technology)
- [Built In — Senior Software Engineer, Katalon TestOps (Java, React, Microservice)](https://builtin.com/job/senior-software-engineer-katalon-testops-java-react-microservice/4513115)
- [Built In — Senior Software Engineer, Katalon Studio](https://builtin.com/job/senior-software-engineer-katalon-studio/3007611)
- [Built In — Lead Software Engineer, TrueTest (Java, Python)](https://builtin.com/job/lead-software-engineer-truetest-java-spring-boot-python/7463903)
- [Glassdoor — Katalon Interview Experience & Questions](https://www.glassdoor.com/Interview/Katalon-Interview-Questions-E4670298.htm)
- [ITviec — Senior Software Engineer (Java, C#) tại Katalon](https://itviec.com/it-jobs/senior-software-engineer-java-c-katalon-5711)
- [Katalon Studio — Wikipedia](https://en.wikipedia.org/wiki/Katalon_Studio) — lịch sử, kiến trúc Eclipse RCP, ngôn ngữ Groovy/Java
- [Katalon Studio — StackShare](https://stackshare.io/katalon-llc/studio) — tech stack chi tiết
- [Katalon TrueTest: Improve QA with Test Automation](https://katalon.com/resources-center/blog/katalon-truetest-improve-qa) — cách TrueTest capture/model/generate test từ hành vi thật
- [Katalon AI Features | Purpose-Built for Software Quality](https://katalon.com/ai-powered-testing-platform) — xác nhận dùng OpenAI GPT models, danh sách đầy đủ tính năng AI toàn platform
- [Katalon Product Roundup June 2026](https://katalon.com/resources-center/blog/katalon-product-roundup-june-2026) — tính năng TrueTest mới nhất (Regenerate, Object Verification, Run with AI)
- Bộ bài tập DSA kèm theo: [leetcode-38-bai-phong-van-vietnam.md](../leetcode-38-bai-phong-van-vietnam.md)

> **Lưu ý về độ tin cậy:** JD ở mục 1.3 và 1.2 fetch trực tiếp từ `katalon.com` ngày 13/08/2026 — tin cậy cao. Hai posting Built In đã bị đóng (06/2025) nhưng vẫn giá trị để đọc **tín hiệu stack**. Thông tin vòng phỏng vấn từ Glassdoor là **báo cáo tự nguyện của ứng viên, số lượng nhỏ** — coi là chỉ dấu, không phải quy trình chính thức. Phần đánh giá điểm mạnh/gap ở mục 3 là **suy luận từ repo của bạn** — hãy tự xác thực lại. Phần chi tiết Katalon Studio/TrueTest ở mục 1.1.1–1.1.2 fetch 25/08/2026 từ trang chính thức Katalon + Wikipedia/StackShare — **tin cậy cao cho phần "làm gì"**, nhưng **kiến trúc backend cụ thể (capture mechanism, Kafka/Spark/K8s...) không được Katalon công khai** — phần đó trong mục 5.1 vẫn là thiết kế giả định của bạn, không phải sự thật đã verify.
