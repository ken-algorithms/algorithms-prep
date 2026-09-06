# Mẫu trả lời cho buổi chat với Talent Acquisition (Wendy) — Katalon

> Dành riêng cho **vòng 1 — HR/Talent Acquisition screening call** (~30 phút), KHÔNG phải vòng kỹ
> thuật. Vị trí: **Lead Software Engineer (Java, Python, JavaScript)**.
>
> Mỗi mục có **tiếng Việt để hiểu và nhớ ý**, **tiếng Anh để nói thật trong buổi call** (vì buổi
> chat này rất có thể diễn ra bằng tiếng Anh). Không học thuộc — đọc kỹ, hiểu logic, rồi **nói
> bằng lời của bạn**. Trả lời nghe như đọc thuộc là dấu hiệu đáng ngờ với TA đã nói chuyện với
> hàng trăm ứng viên.
>
> Toàn bộ ví dụ kỹ thuật ở đây lấy từ dấu vết **thật** trong repo của bạn — không bịa số liệu.

---

## 0. Trước khi vào call — 5 điều cần chốt sẵn

Đây là những chỗ tôi **không thể điền thay bạn** vì đó là quyết định cá nhân. Chốt trước khi
vào call, viết ra giấy đặt cạnh màn hình:

| Câu hỏi Wendy chắc chắn hỏi | Bạn cần chốt trước |
|---|---|
| *"What's your current role / notice period?"* | Công ty hiện tại, chức danh, **thời gian báo trước** (thường 30–60 ngày ở VN) |
| *"Why are you looking for a new opportunity?"* | Lý do thật, diễn đạt tích cực (xem §3) |
| *"What's your salary expectation?"* | **Một khoảng số cụ thể**, không nói "tuỳ công ty" |
| *"Are you open to remote / are you based in Vietnam?"* | JD ghi *"Vietnam (remote)"* — xác nhận vị trí hiện tại của bạn |
| *"When could you start?"* | Ngày cụ thể sau khi trừ notice period |

> **Về lương:** tôi không có dữ liệu để gợi ý số — nó phụ thuộc thị trường hiện tại, level thật
> bạn deal được, và mức bạn đang nhận. Nguyên tắc chung: nói một **khoảng** (không phải một số),
> và khoảng đó nên có đáy ở mức bạn *thực sự* rời việc hiện tại, không phải mức tối thiểu chịu được.

---

## 1. "Tell me about yourself" / "Walk me through your background"

Đây là câu **chắc chắn có**, thường mở đầu buổi call. Giữ trong **60–90 giây**, đi theo cấu trúc:
hiện tại → kinh nghiệm nền → điểm khác biệt → vì sao đang tìm hiểu Katalon.

### Tiếng Việt (để hiểu logic)

> "Tôi hiện đang là [chức danh] tại [công ty], làm chủ yếu với Java và Python. Nền tảng của tôi
> là Java — tôi có kinh nghiệm build hệ thống backend production thật với Spring Boot, kiến trúc
> hexagonal, OAuth2, GraphQL, và làm việc với message queue. Song song đó, khoảng một năm gần đây
> tôi tập trung nhiều vào mảng AI/agent — tôi đã xây các hệ thống agent dùng LangGraph, làm RAG
> với vector search, và có kinh nghiệm thật về việc đánh giá và ra quyết định dựa trên số liệu khi
> so sánh các thiết kế khác nhau, không chỉ code theo cảm tính. Điều làm tôi thấy Katalon thú vị là
> vị trí này cần đúng combo Java cho hệ thống + Python cho AI — và sản phẩm TrueTest của Katalon
> chính là bài toán AI-cho-testing mà tôi đã có kinh nghiệm thực chiến gần nhất."

### English (nói trong buổi call)

> *"I'm currently a [your title] at [current company], working primarily across Java and Python.
> My core background is Java — I've built production backend systems with Spring Boot, hexagonal
> architecture, OAuth2, GraphQL, and message-queue-based integrations. Alongside that, over the
> past year I've been focused heavily on AI and agentic systems — I've built agent pipelines using
> LangGraph, done RAG work with vector search, and have real experience making data-driven design
> decisions rather than just shipping on gut feel. What makes this role interesting to me is that
> it needs exactly that combination — Java for the core systems, Python for the AI side — and
> Katalon's TrueTest product is essentially the AI-for-testing problem I've most recently worked
> on hands-on."*

> **Chỗ điền tay:** `[chức danh]`, `[công ty]` — tôi không có thông tin này, bạn tự điền theo
> tình huống thật hiện tại của mình.

---

## 2. "Why Katalon?" / "Why this role?"

Câu này kiểm tra bạn có **research thật** hay chỉ apply đại trà. Trả lời hay nhất là nhắc được
**TrueTest** cụ thể — vì gần như không ứng viên nào đọc kỹ tới mức đó.

### Tiếng Việt

> "Katalon không chỉ là một công ty làm test automation — cái làm tôi chú ý là **TrueTest**: sản
> phẩm tự động discover, model, generate và maintain test case bằng AI, dựa trên hành vi người
> dùng thật trên trình duyệt. Đó đúng là điểm giao giữa hai thứ tôi đang làm: hệ thống backend
> Java ở quy mô lớn, và agentic AI/LLM. Job description cũng ghi rất rõ là công nghệ Java, Python,
> JavaScript song song — điều đó khớp chính xác với cách tôi đang làm việc, không phải kiểu công
> ty chỉ cần một ngôn ngữ."

### English

> *"Katalon isn't just a test automation company to me — what really caught my attention is
> **TrueTest**, the product that automatically discovers, models, generates, and maintains test
> cases using AI based on real user behavior in the browser. That's exactly the intersection of
> what I've been doing: large-scale Java backend systems, and agentic AI/LLM work. The job
> description is also explicit about needing Java, Python, and JavaScript together, which matches
> how I actually work — not a role that only needs one language."*

---

## 3. "Why are you looking for a new opportunity?" / lý do rời công việc hiện tại

**Nguyên tắc:** luôn nói **hướng về phía trước** (đi tới cái gì), không nói **hướng về phía sau**
(chạy khỏi cái gì). Không nói xấu công ty/sếp cũ dù thật đến đâu.

### Tiếng Việt — khung câu trả lời (điền theo tình huống thật của bạn)

> "[Lý do thật của bạn, ví dụ: Tôi đã đạt được nhiều ở vai trò hiện tại và đang tìm một môi trường
> có quy mô lớn hơn / nhiều thử thách kỹ thuật hơn / cho phép tôi kết hợp sâu cả Java và AI]. Vị
> trí ở Katalon phù hợp vì [lý do cụ thể liên kết với JD]."

**Ví dụ cụ thể nếu áp dụng được với bạn** (dựa trên bằng chứng: bạn có kinh nghiệm production
Java thật + đã tự học/xây AI agent 1 năm gần đây):

> "Tôi đã dành phần lớn thời gian gần đây để tự học sâu về AI/agentic systems bên cạnh công việc
> Java chính, và tôi đang tìm một vị trí nơi cả hai mảng đó đều được dùng thật, không phải tách
> biệt. Đó là lý do vị trí Lead ở Katalon — nơi Java xây hệ thống lõi và Python/AI là một phần
> chính thức của sản phẩm — rất khớp với hướng tôi muốn đi."

### English

> *"I've spent a lot of the past period going deep on AI and agentic systems alongside my core
> Java work, and I'm looking for a role where both are actually used together, not siloed. That's
> why this Lead role at Katalon stood out — Java for the core systems and Python/AI as a first-class
> part of the product is exactly the direction I want to grow in."*

> **Quan trọng:** nếu bạn có lý do khác (công ty giảm quy mô, muốn remote, muốn lương tốt hơn,
> hết dự án...) hãy dùng lý do **thật của bạn**, diễn đạt tích cực theo khung trên. Đừng dùng câu
> mẫu này nếu nó không đúng với hoàn cảnh — TA rất dễ nhận ra câu trả lời không ăn khớp khi hỏi sâu thêm.

---

## 4. "Tell me about a project you're proud of" — câu chuyện mạnh nhất bạn có

Đây là câu chuyện **thật, có số liệu, và thể hiện tư duy Lead** — tự bác bỏ chính đề xuất của
mình dựa trên dữ liệu. Rất hiếm ứng viên có câu chuyện dạng này. Dùng khung STAR.

### Tiếng Việt

> "Một ví dụ tôi tự hào nhất không phải là một feature tôi ship thành công, mà là một lần tôi
> **tự bác bỏ đề xuất kỹ thuật của chính mình**. Tôi có viết một RFC đề xuất một hướng thiết kế
> cho hệ thống agent, và để chắc chắn, tôi làm luôn một bakeoff — thử nghiệm so sánh cách đó với
> phương án khác dựa trên số liệu thật, không phải suy đoán. Kết quả benchmark cho thấy rõ đề xuất
> ban đầu của tôi **không tốt bằng** phương án còn lại. Tôi viết một RFC thứ hai, trình bày số
> liệu, và chủ động rút lại đề xuất đầu — dù tôi là người viết ra nó. Với tôi đó chính là tư duy
> Lead: quyết định dựa trên dữ liệu quan trọng hơn việc bảo vệ ý tưởng của mình."

### English

> *"One example I'm actually most proud of isn't a feature I successfully shipped — it's a time I
> **rejected my own technical proposal**. I wrote an RFC proposing a particular design direction
> for an agent system, and to validate it properly, I ran a bakeoff — a data-driven comparison
> against an alternative approach, instead of just going with intuition. The benchmark results
> clearly showed my original proposal was actually **worse** than the alternative. So I wrote a
> second RFC, presented the data, and proactively withdrew my own proposal — even though I was the
> one who wrote it. To me, that's what lead-level thinking looks like: data-driven decisions matter
> more than defending your own idea."*

**Nếu được hỏi thêm chi tiết:** chuẩn bị sẵn 2–3 câu về *cái gì* được so sánh và *số liệu cụ thể*
là gì — Wendy khó hỏi sâu kỹ thuật, nhưng hiring manager ở vòng sau **sẽ hỏi**. Viết trước ra giấy
để không bị lúng túng khi kể lại lần hai.

---

## 5. Nếu bị hỏi thẳng: "How comfortable are you with Java right now?"

Đây là câu **rủi ro cao nhất** nếu bạn thật đã tạm ít động tay Java gần đây. Nguyên tắc: **thành
thật + có bằng chứng + có hành động cụ thể đang làm**, không né tránh và không phóng đại.

### Tiếng Việt

> "Nền tảng Java của tôi là thật và có production experience — tôi đã build hệ thống Spring Boot
> với kiến trúc hexagonal, OAuth2, GraphQL với DataLoader để giải quyết vấn đề N+1, và Spring Cloud
> Stream với RabbitMQ. Gần đây tôi dành nhiều thời gian hơn cho mảng Python/AI, nên tôi đang chủ
> động ôn lại phần Java sâu — cú pháp Java 21, Quarkus, concurrency — để đảm bảo mình sẵn sàng ở
> mức production ngay khi bắt đầu, không chỉ ở mức 'còn nhớ cách viết'."

### English

> *"My Java foundation is real and production-grade — I've built Spring Boot systems with hexagonal
> architecture, OAuth2, GraphQL with DataLoader to solve N+1 query issues, and Spring Cloud Stream
> with RabbitMQ. I've spent more recent time on the Python/AI side, so I'm actively refreshing the
> deeper Java areas — Java 21 features, Quarkus, concurrency — to make sure I'm production-ready
> from day one, not just 'remembering the syntax'."*

> **Vì sao câu này AN TOÀN để nói thật:** JD Lead của Katalon liệt kê *"Experience with GitHub
> Copilot, Cursor, or Claude Code"* là nice-to-have, và bản thân TrueTest dùng AI coding assistant
> để tăng tốc phát triển. Chủ động ôn lại có kỷ luật (có test, có số liệu) là **điểm cộng**, không
> phải điểm trừ, với đúng công ty này.

---

## 6. "What are your strengths?" / "What's an area you're working on improving?"

### Điểm mạnh — Tiếng Việt

> "Điểm mạnh rõ nhất của tôi là khả năng làm việc thật ở cả hai phía: hệ thống backend Java quy mô
> production, và AI/agentic systems bằng Python. Tôi cũng có kỷ luật kỹ thuật rõ — viết test đầy
> đủ, và trước khi quyết định thiết kế tôi thường làm benchmark thật thay vì đoán."

### Điểm mạnh — English

> *"My clearest strength is being genuinely hands-on on both sides — production-grade Java backend
> systems, and AI/agentic systems in Python. I also have a strong engineering discipline — writing
> thorough tests, and validating design decisions with real benchmarks rather than assumptions
> before committing to them."*

### Điểm cần cải thiện — Tiếng Việt (thành thật, có kế hoạch cụ thể)

> "Tôi dành nhiều thời gian gần đây cho mảng AI nên có phần hơi rời Java hàng ngày một thời gian
> — tôi đang chủ động bù lại bằng cách ôn tập có cấu trúc, viết lại các bài toán Java 21 và pattern
> concurrency từ đầu để chắc chắn phản xạ vẫn nhanh."

### Điểm cần cải thiện — English

> *"I've been spending a lot of recent time on the AI side, so I stepped back a bit from daily
> Java work — I'm actively closing that by doing structured review, rewriting Java 21 exercises
> and concurrency patterns from scratch to make sure my reflexes are still sharp."*

> **Vì sao trả lời này an toàn:** nó thành thật (khớp với thực tế), có hành động cụ thể (không mơ
> hồ "tôi đang cải thiện"), và không phải điểm yếu chí mạng với JD này (Python cũng là yêu cầu cứng).

---

## 7. Câu hỏi bạn nên hỏi lại Wendy — kết thúc buổi call chủ động

Hỏi lại luôn tạo ấn tượng tốt, và câu hỏi đúng còn cho bạn thông tin thật để chuẩn bị vòng sau.

### Tiếng Việt

1. "Sau buổi hôm nay, quy trình các vòng tiếp theo sẽ như thế nào, và mất khoảng bao lâu?"
2. "Team tôi sẽ join nếu trúng tuyển làm việc trực tiếp với sản phẩm nào — TrueTest, hay
   Platform/TestOps?"
3. "Có vòng take-home assignment không? Nếu có, thường tập trung vào phần nào?"

### English

1. *"What does the process look like after today, and roughly how long does it typically take?"*
2. *"Which product would the team I'd be joining be working on most closely — TrueTest, or the
   Platform/TestOps side?"*
3. *"Is there a take-home assignment as part of the process? If so, what does it typically focus on?"*

> Câu hỏi #2 rất đáng hỏi: câu trả lời cho biết bạn nên chuẩn bị sâu phần nào hơn cho vòng kỹ
> thuật — nếu là TrueTest thì dồn thêm vào [common/04-ai-agent-system-design.md](katalon-prep-common/04-ai-agent-system-design.md).

---

## 8. Checklist 10 phút trước khi vào call

- [ ] Đã chốt: chức danh hiện tại, notice period, khoảng lương mong muốn (§0)
- [ ] Đã đọc lại §1 và §2, **không đọc thuộc** — chỉ nắm ý để nói tự nhiên
- [ ] Đã chuẩn bị 2–3 câu chi tiết cho câu chuyện bakeoff RFC (§4) — phòng khi hỏi sâu thêm
- [ ] Đã quyết định cách trả lời §5 (Java gần đây) theo đúng tình huống thật của mình
- [ ] Có CV bản mới nhất mở sẵn, tên file rõ ràng (`Duc-Nguyen-Minh-CV.pdf`)
- [ ] Kiểm tra camera/mic Google Meet trước 5 phút — ứng viên trễ hoặc kỹ thuật lỗi ở vòng đầu là mất điểm oan
- [ ] Ghi sẵn 3 câu hỏi ở §7 để hỏi lại cuối buổi
