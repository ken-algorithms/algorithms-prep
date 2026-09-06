# katalon-prep — workspace thực hành cho phỏng vấn Katalon

> Đi kèm [katalon-senior-lead-phong-van.md](katalon-senior-lead-phong-van.md) (chiến lược) và
> [leetcode-38-bai-phong-van-vietnam.md](../leetcode-38-bai-phong-van-vietnam.md) (DSA).
> Workspace này là phần **code thật, chạy được, có test**.

```bash
cd katalon-prep-java   && mvn test        # 191 test, 0 failure   (JDK 21 qua toolchains)
cd katalon-prep-python && uv sync && uv run pytest -q   # 122 test, 0 failure   (offline)
```

**Đã verify trên máy bạn: 313 test, 0 failure.** Không phần nào cần Docker. Phần Python không gọi mạng.

> **Muốn luyện code tay 100% trong terminal, không mở IDE?**
> → [**TERMINAL-DICTIONARY.md**](TERMINAL-DICTIONARY.md) — từ điển lệnh theo nhóm cho vim, SDKMAN,
> Maven, uv, pytest, debug, cùng 12 bẫy macOS/zsh đã gặp thật. Mọi lệnh trong đó đã chạy thử trên máy bạn.

## Thứ tự ưu tiên tổng thể — làm gì trước

**Giai đoạn 0 — ngay bây giờ, trước buổi chat với Talent Acquisition** (~2-3 giờ)

1. [**CV-BASED-ANSWERS.md**](CV-BASED-ANSWERS.md) ⭐ — thứ tự dự án thật, câu chuyện Credit
   Strong/Motives IDP
2. [SCREENING-CALL-ANSWERS.md](SCREENING-CALL-ANSWERS.md) — logistics (lương/notice), why
   Katalon, câu hỏi hỏi lại

   Xong khi: nói được elevator pitch 60-90s không cần nhìn giấy.

**Giai đoạn 1 — 3 tuần kỹ thuật** (chi tiết ở bảng "Lộ trình 3 tuần" dưới)

`common/04` (agent) ⭐ → `common/01`+`02` (clean code/pattern) → `common/03` (system design) →
Java `05`(postgres)→`02`(quarkus)→`03`(spring)→`06`(distributed) → drill song song suốt.

**Giai đoạn 2 — riêng trước vòng technical/system design** (1 tài liệu + 1 folder, đọc AI trước)

- [**AI-STACK-INTERVIEW-ANSWERS.md**](AI-STACK-INTERVIEW-ANSWERS.md) ⭐ — stack AI thật của bạn
  (`motivesidp-ai-service` + `motivesidp-bom-agentic`) → câu trả lời phỏng vấn.
  Agent/memory/tool calling/flow control, **vòng cung RFC-bakeoff 43 run** (tự bác bỏ 3 lần rồi mở
  lại và thắng), **số đo thật** (89.1% hard trên 256 ô golden, token 4.45× vs 1.56×), **nền tảng
  vector/dot-product/cosine + 4 loại distance Qdrant + thực tiễn 2026** (MRL, quantization,
  reranker), 61 câu hỏi luyện, và **mục ranh giới trung thực** phải đọc trước khi vào phòng.
- [**katalon-selfhost-llm.md**](katalon-selfhost-llm.md) ⭐⭐ — **hướng tuyển mới (06/09/2026): AI
  Engineer, chuyển OpenAI/Bedrock/Azure sang self-host để giảm chi phí.** Đây gần như là bài
  *"kể lại đúng việc mình đã làm"*: 12 bằng chứng từ `.env`/`docker-compose`/git log, seam
  OpenAI-compatible, kinh tế học điểm hoà vốn, llama.cpp→vLLM, cái giá thật của self-host
  (constrained decoding, 6 metric vận hành), 7 bậc giảm chi phí, **khi nào KHÔNG nên self-host**,
  STAR 90 giây + 10 follow-up, kế hoạch 2 tuần.
- [**katalon-llm-migration-va-scale.md**](katalon-llm-migration-va-scale.md) 🔴 — **nhóm kỹ năng đã
  làm bạn trượt.** Vì sao *"accuracy không đổi"* **không chứng minh gì** (nghiên cứu 2026: +7,3 điểm
  tổng mà vẫn 6% item tụt hạng); vì sao **không diff được đầu ra kể cả ở temperature 0**; công thức
  cỡ mẫu `n ≈ 16p(1−p)/δ²`; 5 giai đoạn shadow→canary; CI gate cho hệ không tất định; **capacity
  tính từ KV cache** (FP8 gấp đôi số user); admission control và **vì sao hệ "không bao giờ từ chối"
  lại mất nhiều request hơn**; **lộ trình 4 tuần, mỗi tuần ra một con số của chính bạn**.
- [**katalon-system-design/**](katalon-system-design/) ⭐ — **mọi bài system design gom về một
  folder, phân loại theo họ.** Bắt đầu bằng [README](katalon-system-design/README.md): **5 trục
  nhận diện** (ghi/đọc · điểm/khoảng · tài nguyên tranh nhau · tuyệt đối/xấp xỉ · đọc-tính-ghi) và
  **7 họ bài** — thứ dùng được cho cả đề **chưa từng thấy**. Sáu file:
  [01 TrueTest journey mining](katalon-system-design/01-truetest-journey-mining.md) (bài on-domain
  quan trọng nhất) · [02 Distributed test execution](katalon-system-design/02-distributed-test-execution.md)
  (fairness, WFQ/DRR, LPT bin-packing, *"10.000 test cùng lúc"*) ·
  [03 Real-time analytics](katalon-system-design/03-realtime-analytics-dashboard.md) (có sơ đồ
  mermaid) · [**04 Event counting 10k/phút**](katalon-system-design/04-event-counting-10k.md) ⭐⭐
  (**bài đã hỏi thật ở vòng Principal** — 4 lỗ hổng của câu trả lời tại chỗ, thiết kế đúng từ cơ bản
  tới dòng code, 3 tech stack kèm ngưỡng chuyển, demo từng bước, 16 câu follow-up) ·
  [05 Hệ thống thật all-in-one trên AWS](katalon-system-design/05-he-thong-that-allinone-aws.md)
  (kho bằng chứng ECS/ALB/Terraform thật để dẫn chứng) ·
  [**06 Race condition khi tính balance**](katalon-system-design/06-race-condition-balance-ledger.md) ⭐⭐
  (**lỗi thật trong `all-in-one-v2`** — batch tính lãi đua với thanh toán. *Phần I:* 7 phát hiện có
  file:line, thang 5 bậc giải pháp, balance real-time đúng, STAR 90 giây. *Phần II — thiết kế lại
  từ đầu:* đổi định nghĩa để race biến mất, batch chỉ phát lệnh chứ không tính, scale khi posting
  tăng vô hạn, kiến trúc AWS 11 bước có Terraform).

**Giai đoạn 3 — trước vòng cuối/mock** (chưa có ngày riêng trong lộ trình — tự thêm)

Ghép lại thành 1 buổi mock full-loop, tự quay màn hình, kể STAR không cần đọc giấy.

Xuyên suốt cả 3 giai đoạn: [TERMINAL-DICTIONARY.md](TERMINAL-DICTIONARY.md) khi luyện tay,
`07-dsa-drill` song song.

## Thiếu gì — ưu tiên bổ sung theo thứ tự

| # | Gap | Mức độ | Vì sao | Làm gì |
|---|---|:---:|---|---|
| 1 | **Playwright / TypeScript / CDP** | 🔴 Cao nhất | JD ghi **hard requirement**: *"web instrumentation and browser automation including JavaScript/TypeScript and Playwright"*. **Không có dòng code nào** ở bất kỳ đâu trong workspace này bịt gap này | Project TS riêng: 3 test + trace viewer + `newCDPSession()` bắt DOM mutation + port `SelfHealingLocator` ([PATTERNS.md](katalon-prep-java/01-clean-code-solid/PATTERNS.md)) sang TS. Chưa làm — xem "Chưa làm" dưới |
| 2 | **Mock full-loop chưa có buổi riêng** | 🟡 Vừa | Lộ trình 3 tuần dạy đủ nội dung nhưng không có ngày nào ghép lại thành một buổi phỏng vấn giả đầy đủ (giới thiệu → design → code → hỏi lại) | Tự thêm 1 buổi cuối lộ trình, tự quay màn hình, không đọc giấy |
| 3 | **Spark / PySpark** | 🟢 Thấp — chủ động, không phải sai sót | Bạn đã quyết định để sau, ưu tiên agent trước. Đã xác minh kỹ thuật sẵn (PySpark 3.5.9 chạy được, cần ghim JDK 21) | Quay lại sau khi xong Giai đoạn 1, xem [python README](katalon-prep-python/README.md#phần-spark-chưa-làm) |
| 4 | **Capstone mini-TrueTest** | 🟢 Thấp — optional | Bài tập ghép toàn bộ pipeline, không phải kiến thức mới | Chỉ làm nếu còn dư thời gian sau Giai đoạn 1–2 |

**Tự kiểm tra nhanh trước khi coi Giai đoạn 1 là "xong":** bạn đã thực sự **gõ tay** hai bài
`two_sum`/`level_order` ở [07-dsa-drill](katalon-prep-java/07-dsa-drill/) (cả Java và Python) hay
mới đọc code mẫu? Đọc ≠ luyện — module đó thiết kế để bạn tự viết, không phải để xem đáp án.

## Năm folder

| Folder | Là gì | Test | Chạy bằng |
|---|---|:---:|---|
| **[katalon-system-design/](katalon-system-design/)** | **Đề bài + lời giải** system design, phân loại theo 7 họ | — | đọc |
| **[katalon-self-questions/](katalon-self-questions/)** | **Câu hỏi tự ghi** — khái niệm đơn lẻ, có code chạy được | — | đọc + chạy |
| **[katalon-prep-common/](katalon-prep-common/)** | **Tài liệu** — khái niệm, so sánh 2 ngôn ngữ, quy trình, câu trả lời phỏng vấn | — | đọc |
| **[katalon-prep-java/](katalon-prep-java/)** | 9 module Maven | **191** | `mvn test` (JDK 21) |
| **[katalon-prep-python/](katalon-prep-python/)** | 6 package | **122** | `uv run pytest` (3.12+) |

**Vì sao `katalon-system-design` tách khỏi `katalon-prep-common`:** `common` là mạch **học khái
niệm** (`01→04`, gắn với package code chạy được). `katalon-system-design` là **đề bài để luyện trả
lời**. Code chạy được của system design vẫn ở lại
[katalon-prep-java/08-system-design/](katalon-prep-java/08-system-design/) — nó là module Maven khai
báo trong `pom.xml`, chuyển đi là vỡ build.

**Vì sao `common` chỉ có tài liệu, không có code:** design pattern viết bằng Java và bằng Python
nhìn *khác nhau* (`interface` vs `Protocol`, Decorator class vs `@decorator`, `sealed` vs
`match`). Nhét code Java vào folder tên "common" là sai sự thật. Nên: **khái niệm + bảng dịch hai
ngôn ngữ ở `common`, code chạy được ở hai folder ngôn ngữ.**

## Bắt đầu từ đâu

```text
1. common/04-ai-agent-system-design.md  ⭐  TrueTest là sản phẩm lõi của Katalon
   └→ katalon-prep-python/src/prep/agent/   55 test, có demo chạy được

2. common/01-clean-code-solid.md + 02-design-patterns.md
   └→ katalon-prep-java/01-clean-code-solid/      61 test
   └→ katalon-prep-python/src/prep/clean_code/    14 test

3. common/03-system-design.md
   └→ katalon-prep-java/08-system-design/         24 test
   └→ katalon-prep-python/src/prep/distributed/   19 test

4. katalon-prep-java/05-postgres-depth → 02-quarkus-service → 03 → 06 → 00/04
5. 07-dsa-drill  (cả hai bên, làm song song suốt, tắt Copilot)
```

## Trả lời 2 câu hỏi của bạn

### Nên code Java, Python, hay cả hai?

**Java là chính, Python là phụ** — nhưng chia việc rõ ràng. JD ghi *"**Strong expertise in Java**"*
là requirement **#1**, Python là **#4**.

| Tình huống | Ngôn ngữ | Lý do |
|---|---|---|
| **Take-home** | **Java** (Quarkus/Spring Boot) | Bằng chứng duy nhất họ có về "strong Java" |
| **Live coding / DSA** | **Java** | Bạn đã có cả 38 bài bằng Java 21. Nếu họ nói "ngôn ngữ nào cũng được" → vẫn chọn Java **và nói lý do**: *"vì đó là ngôn ngữ chính của vị trí này"*. Câu đó tự nó là signal |
| **System design** | không code | Nhắc Quarkus/Kafka/Postgres, không nhắc FastAPI |
| **AI / agent / LLM** ⭐ | **Python** | TrueTest. Chỗ background LangGraph/RAG của bạn phát huy |
| **Data pipeline / Spark** | **Python (PySpark)** | JD yêu cầu Python đúng ở mảng này |
| **Browser automation** | **TypeScript** | Playwright |

> **Ngoại lệ:** nếu vòng coding quá ngặt (≤20 phút) và tay Java còn chậm — chọn Python để **giải
> xong** vẫn tốt hơn viết Java nửa vời rồi hết giờ. Đó là phương án B. Module `00` ở cả hai bên
> tồn tại để bạn không cần nó.

### Bạn design rồi để AI gen code — có ảnh hưởng gì không?

**Với Katalon: không xấu, là điểm cộng.** Bằng chứng từ chính JD:

- JD Lead nice-to-have: *"Experience with **GitHub Copilot, Cursor, or Claude Code**"*
- JD TrueTest: *"**Leverage AI Coding Assistants** to boost development efficiency"*
- Chính sản phẩm của họ là AI sinh test case tự động

Đừng che. Che còn tệ hơn.

**Nhưng có 3 rủi ro thật:**

| # | Rủi ro | Mức độ |
|---|---|---|
| 1 | **Live coding không có AI.** Sau 1 năm nghỉ Java + quen gen code, tay viết `Stream`/`Optional`/`CompletableFuture` sẽ chậm | 🔴 Cao — bịt bằng module `00` |
| 2 | **Bị đào tới tầng implementation.** Level Lead sẽ hỏi *"cái này vỡ ở đâu"*. Design ở tầng plan + AI lo phần còn lại = không biết trade-off tầng dòng code | 🔴 Cao — **có bằng chứng trong code RCI của bạn** |
| 3 | **Take-home bị hỏi lại live.** "Giải thích dòng này", "extend feature Y ngay" | 🟡 Vừa |

**Bằng chứng cho #2 — từ chính code RCI của bạn.** `HealthCheckContextStrategy.java` có 14 strategy
`@Autowired` nhưng `switch` chỉ xử lý **2**. Tôi dựng lại rồi **chạy thật**. Kết quả khác hẳn những
gì đọc code sẽ đoán (tôi cũng đoán sai): dòng "biến chết" gọi `join()` **trước** `getSafe()`, mà
`join()` trên future failed thì **ném** → một service phụ lỗi làm **cả endpoint health trả 500**,
và `getSafe()` — đoạn duy nhất viết *để* xử lý lỗi — là **dead code**. Nếu bạn **xoá dòng biến
chết** (SonarLint đang đòi), hành vi **đổi hẳn** → response thành **rỗng im lặng**, dashboard vẫn xanh.

**Hai bug che lấp nhau.** "Dọn dẹp vô hại" biến sự cố *dễ thấy* thành sự cố *không ai thấy*.

Đây **không phải** lỗi của AI. Là lỗi của **design đúng ở tầng cao nhưng không review ở tầng dòng
code** — AI chỉ làm nó xảy ra nhanh hơn.

### Trong buổi này tôi đoán sai 7 lần, và chỉ CHẠY mới lộ

Đó là bằng chứng sống cho quy tắc *"luôn chạy, không chỉ đọc"* — chi tiết ở
[common/01 §5](katalon-prep-common/01-clean-code-solid.md#5-bảy-lần-tôi-đoán-sai-trong-khi-xây-workspace-này).
Bốn trong bảy là kết luận **sai hẳn**, không phải chỉ số liệu yếu:

- `getSafe()` → tưởng nuốt lỗi, thật ra **ném 500** và là dead code
- `asyncio.gather` → tưởng huỷ anh em, thật ra **bỏ rơi** chúng (`TaskGroup` mới huỷ)
- salting hot key → tưởng thêm salt là xong, thật ra salt 1× partition gần **vô dụng** và 2× còn **tệ hơn**
- `grounding_score` cộng dồn → một test **bịa hoàn toàn** được **50/100**, lọt qua ngưỡng review

**Cách trả lời khi được hỏi** (họ sẽ hỏi, vì JD có nhắc):

> *"Tôi design trước, viết implementation plan, rồi dùng Claude Code để implement. Nhưng tôi review
> từng dòng và không để AI quyết định kiến trúc — AI không biết trade-off của hệ thống mình. Tôi có
> gate rõ: test phải xanh, và tôi phải giải thích được từng quyết định. Ví dụ cụ thể: gần đây tôi
> dựng lại một class health-check aggregator của mình để review, và khi chạy test mới phát hiện hai
> bug đang che lấp nhau — xoá dead code sẽ biến lỗi 500 ồn ào thành response rỗng im lặng. Đọc code
> không thấy được, phải chạy mới thấy. Từ đó tôi đặt quy tắc: refactor thì viết test trước."*

## Lộ trình 3 tuần

| Ngày | Việc | Xong là gì |
|---|---|---|
| 1 | Đọc [common/04](katalon-prep-common/04-ai-agent-system-design.md), chạy `uv run python -m prep.agent.demo` | Nói được vòng discover→model→generate→maintain |
| 2–3 | `agent/` — đọc `loop.py` + `guardrails.py`, tự viết lại vòng lặp từ đầu | Kể được 7 cách agent chết + cách chặn |
| 4 | `agent/rag.py` + `evals.py` — chạy test, đọc số đo | Trả lời được "test hệ không tất định thế nào" |
| 5–6 | `01` Java — [REVIEW.md](katalon-prep-java/01-clean-code-solid/REVIEW.md), tự tìm 14 vấn đề **trước khi** xem `after/` | Tìm được ≥10/14 |
| 7 | `01` Java — [PATTERNS.md](katalon-prep-java/01-clean-code-solid/PATTERNS.md), viết lại `SelfHealingLocator` không nhìn | Giải thích được confidence + khi nào không auto-update |
| 8 | **Mở `HealthCheckContextStrategy.java` thật trong RCI và refactor nó** | PR thật — story phỏng vấn |
| 9–10 | `08` Java — chạy test, tự viết lại circuit breaker + token bucket từ đầu | Vẽ được máy trạng thái 3 pha |
| 11–12 | `08` — trình bày 1 bài design 45 phút, tự quay màn hình | ≥4 trade-off/bài, có ghi số lên bảng |
| 13–14 | `05` — chạy từng lab, **cố tình phá** (bỏ `VACUUM` ở test 5 xem còn Index Only Scan không) | Đọc `EXPLAIN` thành thạo |
| 15–16 | `02` — đọc code, `quarkus:dev`, thêm 1 endpoint mới + test | Quarkus không còn lạ |
| 17 | `03` — chạy `./bench.sh`, điền [COMPARISON.md](katalon-prep-java/03-spring-boot-compare/COMPARISON.md) | **Bảng số của chính bạn** |
| 18–19 | `06` Java + `distributed/` Python — tự viết lại jitter + DLQ từ đầu | Kể được chuỗi at-least-once → idempotent |
| 20–21 | `04` + `00` (cả hai bên) ôn lại; `07` drill 6 bài **tắt Copilot** | Tay Java và Python đều nhanh lại |

`07` nên làm **song song suốt** 3 tuần (3 bài/tuần), không dồn vào cuối.

## Chưa làm

| Gap | Trạng thái | Ghi chú |
|---|---|---|
| **Spark / PySpark** | Bạn đã quyết định **để sau** | Đã xác minh: PySpark 3.5.9 chạy được, nhưng **vỡ trên JDK 25** (`getSubject is not supported` — JEP 486). Phải ghim `JAVA_HOME` sang 21. Chi tiết ở [python README](katalon-prep-python/README.md#phần-spark-chưa-làm) |
| **Playwright / CDP** | Chưa | Là TypeScript, cần browser. Project TS riêng: 3 test + trace viewer + `newCDPSession()` bắt DOM mutation + port `SelfHealingLocator` sang TS |
| **Capstone mini-TrueTest** | Chưa | Ghép tất cả: instrumentation SDK → collector → Kafka → Spark mining → pgvector → LLM agent. Xem [tài liệu chiến lược §9.7](katalon-senior-lead-phong-van.md) |

Phần `agent/` đã bịt được khúc `generate` của capstone — đó là khúc đứng giữa, nên nói về nó thì
tự nhiên chạm được cả khâu trước lẫn khâu sau.
