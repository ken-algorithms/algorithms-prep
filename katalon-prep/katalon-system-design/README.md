# katalon-system-design — bản đồ họ bài System Design

> Gom mọi bài system design trong workspace về một chỗ, **phân loại theo họ** thay vì theo thứ tự
> tình cờ. Mục đích: khi nghe một đề lạ trong phòng phỏng vấn, bạn nhận ra **nó thuộc họ nào trong
> 30 giây**, rồi lấy đúng bộ khung ra dùng — thay vì thiết kế lại từ đầu.
>
> Cha: [../README.md](../README.md) · Chiến lược: [../../katalon-senior-lead-phong-van.md](../katalon-senior-lead-phong-van.md)

---

## 1. Nhận họ bài trong 30 giây — 4 trục phân loại

Đừng học thuộc danh sách. Hỏi 4 câu này về đề bài, câu trả lời sẽ chỉ thẳng vào họ:

```text
Trục 1 — Ghi nặng hay đọc nặng?
         ghi >> đọc  → nghĩ ngay tới hấp thụ burst, ghi bất đồng bộ, 202 Accepted
         đọc >> ghi  → nghĩ ngay tới cache, read replica, denormalize, materialized view

Trục 2 — Đọc theo ĐIỂM hay theo KHOẢNG?
         theo điểm (lookup 1 id)  → index/hash, KV store, cache là đủ
         theo KHOẢNG thời gian    → BẮT BUỘC pre-aggregation + rollup phân tầng   ★ họ A
                                    (đây là chỗ đa số ứng viên trượt)

Trục 3 — Có tài nguyên HỮU HẠN mà nhiều bên tranh nhau không?
         có   → bài FAIRNESS, không phải bài throughput                          ★ họ B
                quota + hàng đợi có trọng số + priority/preemption
         không → bỏ qua

Trục 4 — Kết quả cần CHÍNH XÁC TUYỆT ĐỐI hay xấp xỉ được?
         tuyệt đối (billing, audit) → phải giữ raw + idempotent + job đối soát
         xấp xỉ được (dashboard)    → mở khoá được sketch/sampling/top-K, rẻ hơn nhiều bậc

Trục 5 — Có ĐỌC rồi TÍNH rồi GHI dựa trên cái vừa đọc không?
         có   → bài CONCURRENCY, không phải bài performance                      ★ họ G
                ranh giới transaction + khoá + idempotency + append-only
         không → bỏ qua
```

> **Trục 5 là trục dễ bỏ sót nhất.** Nó không hỏi về quy mô — một hệ thống 10 request/giây vẫn dính.
> Dấu hiệu: trong đề có hai bên cùng chạm một thực thể, và ít nhất một bên **ra quyết định dựa trên
> giá trị vừa đọc**. Xem [06](06-race-condition-balance-ledger.md).

**Ví dụ chạy thử 4 trục trên bài đã hỏi thật** (10k request/phút, đếm true/false/fake-key):

| Trục | Trả lời | Suy ra |
|---|---|---|
| 1 | Ghi >> đọc | Ghi bất đồng bộ, trả `202`, không aggregate đồng bộ |
| 2 | **Theo khoảng bất kỳ** | **Bắt buộc bucket thời gian + rollup phân tầng** ← chỗ giải pháp cũ vỡ |
| 3 | Không (nếu single-tenant) | Bỏ qua fairness |
| 4 | Tuyệt đối (đếm số) | Giữ raw + `ON CONFLICT DO NOTHING` + reconcile job |

→ Ra đúng kiến trúc ở [04](04-event-counting-10k.md), không cần "nhớ bài".

---

## 2. Bảy họ bài — chữ ký nhận dạng, lõi giải pháp, bẫy kinh điển

| # | Họ bài | Chữ ký nhận dạng | Lõi giải pháp | Bẫy kinh điển | File |
|---|---|---|---|---|---|
| **A** | **Đếm & tổng hợp theo thời gian**<br>*(counting/aggregation at scale)* | Ghi nặng, đọc nhẹ, **nhưng đọc theo khoảng thời gian bất kỳ** | Pre-aggregation vào **bucket thời gian** + rollup phân tầng (phút→giờ→ngày) + phân rã khoảng lúc query | **Cardinality explosion**; lưu tỉ lệ thay vì lưu count; bucket theo ingest-time thay vì event-time | [03](03-realtime-analytics-dashboard.md) · [**04** ⭐](04-event-counting-10k.md) |
| **B** | **Điều phối tác vụ & chia tài nguyên hữu hạn**<br>*(scheduling / fair queueing)* | Nhiều bên **tranh nhau một pool hữu hạn** | Quota cứng + **hàng đợi có trọng số** (WFQ/DRR) + priority & preemption + bin-packing (LPT) | Đọc thành bài throughput; FIFO chung gây **head-of-line blocking**; priority tuyệt đối gây **starvation** | [**02** ⭐](02-distributed-test-execution.md) |
| **C** | **Thu thập & xử lý luồng sự kiện**<br>*(ingestion pipeline)* | Dữ liệu đến liên tục từ nguồn **mình không kiểm soát** | Backpressure + at-least-once **+ sink idempotent** + partition key giữ ordering đúng phạm vi | Hàng đợi **vô hạn**; hot partition; PII rời client trước khi redact | [**01** ⭐](01-truetest-journey-mining.md) |
| **D** | **Hệ thống có LLM trong vòng lặp**<br>*(agentic / AI system design)* | Có bước **không tất định** nằm giữa pipeline | Plan tất định, LLM **chỉ nằm trong tool**; schema ràng buộc; validation gate; escalate theo confidence | Để LLM quyết flow; tin self-reported confidence; không có eval harness | [../katalon-prep-common/04-ai-agent-system-design.md](../katalon-prep-common/04-ai-agent-system-design.md) · [../AI-STACK-INTERVIEW-ANSWERS.md](../AI-STACK-INTERVIEW-ANSWERS.md) |
| **E** | **Tìm kiếm ngữ nghĩa / RAG**<br>*(retrieval)* | Truy vấn theo **ý nghĩa**, không theo khoá chính xác | Embedding + vector index + **rerank** (bi-encoder lọc, cross-encoder xếp hạng) | Cắt `top_k` quá sớm; truncate embedding của model không phải MRL; nhầm cosine similarity với cosine distance | [../AI-STACK-INTERVIEW-ANSWERS.md §8](../AI-STACK-INTERVIEW-ANSWERS.md) |
| **F** | **Độ tin cậy & chống lỗi lan**<br>*(resilience)* | Có **phụ thuộc ngoài** có thể chậm hoặc chết | Timeout theo tầng + retry có jitter + circuit breaker + bulkhead + DLQ | Retry không jitter gây **thundering herd**; retry và circuit breaker giải **hai** bài khác nhau; nuốt lỗi thành dead code | [../katalon-prep-java/06-distributed-resilience/](../katalon-prep-java/06-distributed-resilience/) · [../katalon-prep-python/](../katalon-prep-python/) |
| **G** | **Quản lý đồng thời trên trạng thái chia sẻ**<br>*(consistency & concurrency control)* | **Đọc → tính → ghi**, và giữa "đọc" với "ghi" có người khác chen vào | Ranh giới transaction đúng + khoá (optimistic/pessimistic) + **idempotency là tiền đề của retry** + append-only để không có gì mà race | Nhầm **write skew** với lost update (optimistic lock **không** bắt được write skew); `synchronized` JVM-local trong khi chạy nhiều instance; retry mà không idempotent; hai nguồn sự thật cho cùng một con số | [**06** ⭐](06-race-condition-balance-ledger.md) |

**Cắt ngang cả bảy họ — không phải một họ riêng:** *multi-tenant & cô lập*. Gần như mọi đề của
Katalon đều là multi-tenant, nên với **bất kỳ** họ nào cũng phải hỏi thêm: dữ liệu tách thế nào,
quota ra sao, một tenant hỏng thì tenant khác có sao không.

> **Phân biệt họ A và họ G — hay bị lẫn vì cả hai đều "đếm số":** họ A hỏi *"tổng hợp nhiều sự kiện
> thế nào cho nhanh"*, sai lệch vài giây chấp nhận được. Họ G hỏi *"hai bên cùng chạm một con số thì
> ai đúng"*, sai một xu là sai. Vì vậy cache TTL mù dùng được ở họ A nhưng **không** dùng được ở họ
> G — xem [06 §7.1](06-race-condition-balance-ledger.md#71-hai-yêu-cầu-xung-đột-nhau).

---

## 3. Bài toán "10k request/phút" thuộc họ nào?

Câu hỏi bạn gặp ở vòng Principal thuộc **họ A — đếm & tổng hợp theo thời gian**. Tên gọi khác của
cùng một họ, tuỳ nơi:

| Tên gọi | Dùng ở đâu |
|---|---|
| **Counting at scale** / **counter service** | Phổ biến nhất trong tài liệu phỏng vấn |
| **Time-series aggregation** | Khi nhấn vào trục thời gian |
| **Real-time analytics / OLAP over event streams** | Khi nhấn vào phía dashboard |
| **Top-K / Heavy hitters** | Biến thể khi cần breakdown theo key với cardinality cao |
| **Metrics ingestion** | Khi số liệu là metric hệ thống (Prometheus, Datadog) |

**Cùng họ, khác vỏ — nhận ra là trả lời được ngay:**

- Đếm lượt xem video YouTube / lượt impression trên X
- Đếm click quảng cáo và tính CTR theo chiến dịch trong khoảng thời gian
- Google Analytics real-time
- Đếm số lần gọi API theo API key để tính tiền (rate-limit analytics)
- **Katalon:** đếm test pass/fail/skip theo suite, theo tenant, theo khoảng thời gian ← đúng đề của họ

Chữ ký chung của cả họ: **ghi nặng, đọc nhẹ, nhưng đọc phải theo khoảng thời gian bất kỳ.** Chính vế
cuối làm mọi giải pháp "một counter duy nhất" sụp đổ, và đó là lý do câu trả lời tại chỗ bị bác
([04 §1](04-event-counting-10k.md#1-vì-sao-câu-trả-lời-cũ-bị-bác--4-lỗ-hổng)).

---

## 4. Các file trong folder

| File | Họ | Nguồn | Trạng thái |
|---|:---:|---|---|
| [**01** — TrueTest: AI journey mining](01-truetest-journey-mining.md) | C + D | Tách từ chiến lược §5.1 | Bài **on-domain quan trọng nhất** — sản phẩm lõi của Katalon |
| [**02** — Distributed Test Execution Platform](02-distributed-test-execution.md) | B | §5.2 + phần fairness bổ sung | Có bản đào sâu *"10.000 test cùng lúc, multi-tenant"* |
| [**03** — Real-time Test Analytics Dashboard](03-realtime-analytics-dashboard.md) | A | Tách từ chiến lược §5.3 | Bản tổng quát, có **sơ đồ mermaid** vẽ được lên bảng |
| [**04** — Event Counting 10k/phút](04-event-counting-10k.md) ⭐ | A | **Đã hỏi thật, vòng Principal** | Bản đào sâu nhất: 4 lỗ hổng của câu trả lời cũ → thiết kế đúng → demo từng bước |
| [**05** — Hệ thống thật all-in-one trên AWS](05-he-thong-that-allinone-aws.md) | — | Review repo thật của bạn | **Không phải đề bài** — là kho **bằng chứng thật** (ECS/ALB/autoscaling/Terraform) để dẫn chứng khi trả lời bất kỳ bài nào |
| [**06** — Race condition khi tính balance](06-race-condition-balance-ledger.md) ⭐ | G | **Lỗi thật, lặp lại, trong `all-in-one-v2`** | **Hai phần.** *Phần I — chữa hệ đang chạy:* 7 phát hiện có file:line (0 `@Version`, 0 `FOR UPDATE`, `synchronized` khoá nhầm chỗ), thang 5 bậc giải pháp, balance real-time đúng. *Phần II — thiết kế lại từ đầu:* [đổi định nghĩa để race biến mất](06-race-condition-balance-ledger.md#121-một-thay-đổi-định-nghĩa-xoá-được-phần-lớn-bài-toán), batch chỉ **phát lệnh** chứ không tính, scale khi posting tăng vô hạn, và [kiến trúc AWS 11 bước](06-race-condition-balance-ledger.md#13-kiến-trúc-aws--khai-báo-từng-bước) có Terraform |

**Vì sao 05 nằm ở đây dù không phải đề bài:** khi trả lời design, câu mạnh nhất không phải *"tôi sẽ
dùng ALB"* mà là *"hệ thống tôi đang vận hành dùng ALB + StepScaling, và đây là file Terraform"*.
File 05 là chỗ lấy dẫn chứng đó.

---

## 5. Cái gì **không** chuyển vào đây, và vì sao

| Nằm ở đâu | Là gì | Vì sao không chuyển |
|---|---|---|
| [../katalon-prep-java/08-system-design/](../katalon-prep-java/08-system-design/) | Code Java, 24 test | Là **module Maven** khai báo trong `pom.xml`. Chuyển là vỡ build |
| [../katalon-prep-python/](../katalon-prep-python/) `src/prep/distributed/` | Code Python, 19 test | Là **package Python** trong `src/`. Chuyển là vỡ import |
| [../katalon-prep-common/03-system-design.md](../katalon-prep-common/03-system-design.md) | Ghi chú khái niệm + cầu nối sang code | Thuộc mạch tài liệu học `common/01→04`, không phải một đề bài |
| [../katalon-prep-common/04-ai-agent-system-design.md](../katalon-prep-common/04-ai-agent-system-design.md) | Họ D | Cùng lý do trên; và nó gắn với package `prep/agent/` |

**Nguyên tắc phân chia:** folder này chứa **đề bài + lời giải** (đọc để luyện trả lời). Code chạy
được và ghi chú khái niệm ở lại chỗ cũ (chạy để hiểu). Trộn hai loại vào một chỗ sẽ vỡ build mà
không được gì.

---

## 6. Khung 45 phút

Áp cho cả 5 file. Không đổi theo đề.

```text
 5' Clarify      → functional / non-functional / scale / constraint. HỎI, đừng đoán
 5' Estimate     → QPS, storage/tháng, bandwidth, cost. Ghi số lên bảng
 5' API + model  → contract trước, schema sau
15' High-level   → vẽ box & arrow, đi theo đường dữ liệu
10' Deep-dive    → interviewer chọn 1 component, đào sâu
 5' Trade-off + failure mode + "nếu scale 10x thì gì vỡ trước"
```

> **Luật vàng cho level Lead/Principal:** mỗi lựa chọn kiến trúc phải kèm một câu *"đánh đổi là..."*.
> Không có câu đó = nói như Senior.

**Ba câu mở đầu dùng được cho mọi đề** — nói ra trong 60 giây đầu, trước khi vẽ bất cứ thứ gì:

1. *"Đơn vị tải thật ở đây là gì?"* — 10k request/phút không phải đơn vị tải nếu mỗi request mang
   20 item. Hai con số lệch nhau 20 lần.
2. *"Cái này cần chính xác tuyệt đối hay xấp xỉ được?"* — mở hoặc khoá cả một nhánh giải pháp
   (sketch, sampling, top-K).
3. *"Multi-tenant thì một tenant lớn ảnh hưởng tenant khác thế nào?"* — với Katalon, câu trả lời
   luôn là "có", và hỏi trước thì bạn dẫn dắt được phần sau.

---

## 7. Thứ tự đọc

| Còn bao nhiêu thời gian | Đọc gì |
|---|---|
| **30 phút** | [04](04-event-counting-10k.md) §1 (4 lỗ hổng) + §11 (kịch bản 3 phút), rồi mục 1–2 của file này |
| **2 giờ** | Trên + [01](01-truetest-journey-mining.md) trọn vẹn (bài on-domain quan trọng nhất) + [06 §10.1](06-race-condition-balance-ledger.md#101-kể-theo-star-90-giây) (STAR có bằng chứng code thật) |
| **Nửa ngày** | Cả 6 file, và **chạy demo** ở [04 §8](04-event-counting-10k.md#8-demo-chạy-được--từng-bước) để có bảng số của chính mình |
| **Trước mỗi vòng** | Mục 1 (4 trục) của file này — đó là thứ dùng được cho đề **chưa từng thấy** |

---

## 8. Điều còn thiếu trong folder này

| Gap | Mức | Ghi chú |
|---|:---:|---|
| **Demo ở [04 §8](04-event-counting-10k.md#8-demo-chạy-được--từng-bước) chưa chạy** | 🔴 | Bảng đo còn ô trống. Đừng nói "tôi đã đo được" trước khi chạy thật |
| Họ B chưa có code chạy được | 🟡 | WFQ/DRR mới ở mức mô tả. [../katalon-prep-java/08-system-design/](../katalon-prep-java/08-system-design/) có token bucket + circuit breaker nhưng chưa có fair queueing |
| **Bản sửa ở [06](06-race-condition-balance-ledger.md) chưa triển khai** | 🔴 | Chẩn đoán đã có bằng chứng file:line, nhưng chưa sửa và chưa đo mức lỗi trên production. Việc đầu tiên nên làm là chạy query đối soát ở [06 §6 bước 5](06-race-condition-balance-ledger.md#bước-5--job-đối-soát-thứ-chứng-minh-đã-sửa) để có con số trước/sau |
| Họ E chưa có file riêng ở đây | 🟢 | Nội dung đầy đủ đã có ở [../AI-STACK-INTERVIEW-ANSWERS.md §8](../AI-STACK-INTERVIEW-ANSWERS.md); tách ra chỉ để cho đều, không thêm giá trị |
| Chưa có bài **storage/indexing** thuần | 🟢 | Chưa gặp trong JD Katalon. [../katalon-prep-java/05-postgres-depth/](../katalon-prep-java/05-postgres-depth/) đã phủ phần index/EXPLAIN |
