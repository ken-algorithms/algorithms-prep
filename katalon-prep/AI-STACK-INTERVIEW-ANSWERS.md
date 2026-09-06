# AI stack của bạn → câu trả lời phỏng vấn Katalon

> **Nguồn:** đọc trực tiếp code **hai** repo ngày **26/08/2026** —
> `motivesidp-ai-service` (267 file Python, 960 commit) và `motivesidp-bom-agentic` (613 file,
> LangGraph). Mọi con số/đoạn code dưới đây đều có đường dẫn `file:line` để bạn tự verify lại
> trước khi mang vào phòng phỏng vấn.
>
> Đi kèm: [katalon-prep-common/04-ai-agent-system-design.md](katalon-prep-common/04-ai-agent-system-design.md)
> (lý thuyết, không phụ thuộc dự án) — file này là **bằng chứng thật của riêng bạn** để trả lời
> lý thuyết đó. [CV-BASED-ANSWERS.md](CV-BASED-ANSWERS.md) là phần câu chuyện sự nghiệp.

---

## 0. Đọc file này trong 2 phút

**Vị thế của bạn:** JD Katalon xếp *"LLM API integration, prompt engineering, agent-based design
patterns"* vào mục **nice-to-have**. Nhưng sản phẩm mũi nhọn của họ — TrueTest — **chính là** bài
toán đó. Nghĩa là: đa số ứng viên Java sẽ trống mảng này, còn bạn có **hệ thống AI đang chạy thật,
có người dùng thật, có tiền thật đi kèm** (BOM sai → đặt mua nguyên phụ liệu sai).

**Năm thứ mạnh nhất, xếp theo thứ tự nên dùng:**

| # | Vũ khí | Vì sao mạnh |
|---|---|---|
| 1 | **Vòng cung RFC/bakeoff — 43 run, 7 vòng, tự bác bỏ 3 lần rồi mở lại và thắng** ([§1B](#1b--hai-hệ-thống-một-vòng-cung--đây-mới-là-câu-chuyện-đầy-đủ)) | Chứng minh 4 thứ cùng lúc: xây được agentic đầy đủ, thiết kế được thí nghiệm có kiểm soát, **tự bác bỏ** khi số liệu không ủng hộ, và **quay lại** khi điều kiện đổi. Cực hiếm |
| 2 | **"Đừng để LLM trả lời — để nó viết ra cách tìm câu trả lời"** ([§7.1](#71-rulebook--ý-tưởng-kiến-trúc-mạnh-nhất-và-cũng-dễ-kể-nhất)) | Một nguyên tắc kiến trúc gọn, dễ hiểu, và **đo được**: đưa accuracy từ <70% lên 89% |
| 3 | **"Nghi phạm đầu tiên là VERIFIER, không phải model"** ([§1B](#1b--hai-hệ-thống-một-vòng-cung--đây-mới-là-câu-chuyện-đầy-đủ)) | Sửa verifier giảm escalated **44% với 0 token thêm**, trong khi 3 vòng sửa agent đều bị reject. Insight rất on-domain với testing |
| 4 | **Bạn chủ động GỠ LLM ra khỏi đường mặc định** ([§4](#4-câu-chuyện-mạnh-nhất--bạn-tự-gỡ-llm-của-chính-mình)) | Trùng đúng triết lý Katalon công bố cho TrueTest: *"AI decisions must be explainable, humans are accountable"* |
| 5 | **Có SỐ, không phải tính từ** ([§7](#7-số-đo-thật--thứ-khiến-bạn-khác-mọi-ứng-viên-ai-khác)) | 89.1% hard trên 256 ô golden · 75.8% PTU trên 19 style · token 4.45× vs 1.56×. Đa số ứng viên AI **không có** số nào |
| 6 | **Grounding gate + self-consistency voting** ([§6](#6-làm-sao-tin-được-output-của-llm--3-cơ-chế-thật-trong-code)) | Đúng bài toán TrueTest phải giải khi LLM sinh locator/test case |

**Rủi ro lớn nhất phải xử lý trước:** doc nội bộ của bạn mô tả tool-calling agent như đang chạy
production, nhưng code hiện tại **không còn gọi đường đó**. Đọc [§10](#10-ranh-giới-trung-thực--đừng-nói-quá) trước khi phỏng vấn.

---

## 1. Hệ thống bạn đang làm — mô tả 90 giây

### Tiếng Việt

> "Tôi đang làm tech lead cho Motives IDP — hệ thống tự động sinh BOM (bill of materials, định mức
> nguyên phụ liệu) cho ngành may mặc. Đầu vào là techpack dạng PDF/Excel của khách, đầu ra là file
> BOM Excel mà bộ phận mua hàng dùng để đặt nguyên liệu thật.
>
> Về kỹ thuật: FastAPI trên Python 3.12, Postgres + Qdrant, và 5 model AI chạy sau HTTP endpoint
> riêng — một VLM Qwen3-VL để đọc hiểu trang techpack, bge-m3 để embedding text, bge-reranker-base
> làm cross-encoder rerank, FashionCLIP để embedding ảnh sketch, và Docling để cắt vùng ảnh khỏi
> trang PDF. Bản thân service là CPU-only, toàn bộ GPU nằm ngoài process.
>
> Phần tôi thấy đáng nói nhất không phải là dùng model gì, mà là **ranh giới tin tưởng**: BOM sai
> dẫn tới đơn mua hàng sai, nên hệ thống được thiết kế để LLM **không bao giờ** là người quyết định
> cuối. Mọi field đều có provenance ghi rõ nguồn và mức tin cậy, và khi không đủ chắc thì hệ thống
> để trống rồi đẩy cho người review, chứ không đoán."

### English

> "I'm the tech lead on Motives IDP — a system that automatically generates BOMs (bills of
> materials) for garment manufacturing. Input is the customer's techpack as PDF or Excel; output is
> an Excel BOM that the purchasing team uses to actually order materials.
>
> Stack-wise: FastAPI on Python 3.12, Postgres plus Qdrant, and five AI models each behind its own
> HTTP endpoint — a Qwen3-VL vision-language model to read techpack pages, bge-m3 for text
> embeddings, bge-reranker-base as a cross-encoder reranker, FashionCLIP for sketch image
> embeddings, and Docling for extracting picture regions from PDF pages. The service itself is
> CPU-only; all GPU work sits out of process.
>
> The part I find most worth talking about isn't which models we use — it's **where we draw the
> trust boundary**. A wrong BOM becomes a wrong purchase order, so the system is designed so the
> LLM is never the final decision-maker. Every field carries provenance recording its source and
> confidence, and when confidence is insufficient the system leaves it blank and escalates to a
> human rather than guessing."

**Số liệu bạn được phép nói (đã verify từ git):** repo 960 commit, bạn là contributor lớn nhất với
**248 commit tự viết** và **254/360 merge vào `develop`** trong team ~8 người — tức bạn là người
review và gác cổng nhánh chính.

---

## 1B. ⭐ Hai hệ thống, một vòng cung — đây mới là câu chuyện đầy đủ

Bạn **không** chỉ có một hệ thống AI. Bạn có hai, và chúng kể một câu chuyện hoàn chỉnh mà rất ít
ứng viên có được.

| | `motivesidp-bom-agentic` | `motivesidp-ai-service` |
|---|---|---|
| Vai trò | **Bản agentic đầy đủ** — thử nghiệm có kiểm soát | **Production** |
| Orchestration | **LangGraph thật** — `StateGraph`, 12 node, 4 conditional edge, có chu trình | Vòng `for` tuần tự cứng |
| Agent loop | **ReAct tự viết tay** (không dùng `create_react_agent`) | Không có |
| Memory ngắn hạn | **`AsyncSqliteSaver` checkpointer** → resume + time-travel debug | `state.json` write-only, **không resume được** |
| Memory dài hạn | **`Lesson` store cross-run** | Không có |
| Tool | **12 tool, scope 3 tầng** | 3 `StructuredTool` |
| Self-correction | **replan → retry, có chu trình thật** | Không |
| Fallback tất định | *"không fallback deterministic"* — cố ý | Tất định là mặc định |
| Kết cục | react bị **từ chối 3 vòng → đóng track → mở lại → ACCEPTED** | Đang chạy thật |

### Vòng cung thật — 43 run đo trong 3 ngày, và nó KHÔNG kết thúc ở "agentic thua"

> ⚠️ **Đây là chỗ dễ kể sai nhất.** Câu chuyện **không** phải "tôi thử agentic, thất bại, quay về
> tất định". Nó là một chuỗi 7 vòng đo có kỷ luật, trong đó hướng agentic bị **từ chối 3 lần, đóng
> track kèm điều kiện mở lại ghi rõ, rồi mở lại và thắng** khi điều kiện đó được đáp ứng.

| Vòng | Arm | Kết quả đo | Phán quyết |
|---|---|---|---|
| 1 | react v1 (9 tool, full transcript) | overall **+2.0pp** nhưng token **4.45×** | ❌ REJECTED |
| 2 | react v2 (cắt chi phí) | token còn 1.37× nhưng overall **−4.1pp** | ❌ REJECTED |
| 3 | hybrid (gate 0 LLM call) | FAIL cả 3, **server suy giảm giữa thí nghiệm** | ❌ REJECTED → **đóng track** |
| — | *meta-finding sau 15 run* | *phương sai nuốt tín hiệu; n=3 không đo nổi ±2-3 dòng* | → nâng n lên 5 |
| 4-6 | **sửa VERIFIER, không sửa agent** | escalated **10.0 → 5.6 (−44%)**, token **+0** | ✅ PASS cả 4 tiêu chí |
| 7 | react-full (mở lại, hạ tầng đã ổn) | escalated **3.0 (−46%)**, item_code **93.9% vs 88.6%**, token 1.56× | ✅ **ACCEPTED** |

Toàn bộ từ lần từ chối đầu tới lần chấp nhận cuối: **19/07 → 21/07/2026, dưới 48 giờ, 43 run đo.**
`ACT_MODE=react` hiện là default thật trong `.env`.

#### Tiếng Việt

> "Tôi xây bản agentic đầy đủ — LangGraph 12 node, chu trình verify→replan→worker, ReAct loop tự
> viết, checkpointer SQLite để resume và time-travel debug, long-term store lưu 'lesson' từ các lần
> retry thành công cho run sau đọc lại.
>
> Rồi tôi đo nó, và nó **trượt ba lần liên tiếp**. Lần một: chất lượng tăng 2 điểm nhưng token gấp
> **4.45 lần** — tôi truy ra là structural chứ không phải nhiễu, vì mỗi vòng lặp gửi lại toàn bộ
> transcript nên token tăng theo bình phương số bước. Lần hai tôi cắt chi phí xuống 1.37 lần thì
> chất lượng tụt 4 điểm. Lần ba, hướng hybrid, trượt cả ba tiêu chí — nhưng giữa thí nghiệm thì
> LLM server suy giảm, nên tôi ghi rõ là kết quả bị nhiễu confound chứ không kết luận hybrid tệ.
>
> Chỗ tôi thấy mình học được nhiều nhất là **meta-finding sau 15 run**: phương sai nuốt chửng tín
> hiệu. Mọi nhánh đều có run tốt và run tệ — với n=3 thì hiệu ứng ±2-3 dòng đơn giản là **không đo
> được**. Nên tôi đóng track react lại, nhưng đóng **có điều kiện** — ghi rõ 3 tiền đề để mở lại.
>
> Rồi tôi đổi hướng hoàn toàn: thay vì sửa agent, tôi sửa **verifier**. Nhận ra rằng khi một item bị
> escalate ở *mọi* cấu hình, kể cả trên golden do người làm, thì nghi phạm đầu tiên là hard-check của
> mình chứ không phải model. Ba vòng sửa verifier đưa escalated từ 10 xuống 5.6, giảm 44%, **không
> tốn thêm một token nào** — trong khi ba vòng sửa agent trước đó đều bị reject.
>
> Và phần kết: khi hạ tầng serve đã ổn định — đúng tiền đề số một tôi ghi lúc đóng track — tôi mở
> lại react và lần này nó **thắng**: escalated còn 3.0, và quan trọng hơn, item_code accuracy 93.9%
> so với 88.6% của static. Tôi kiểm riêng chỉ số đó vì bài học đau ở vòng 2: khi ấy escalated giảm
> nhưng thực chất là model **'confidently wrong'** — nó khai [MISSING] ít đi nhưng sai nhiều hơn.
> Với BOM thì sai-mà-tự-tin nguy hiểm hơn thiếu-có-đánh-dấu, vì người duyệt không biết chỗ nào cần soi."

#### English (bản rút gọn để nói)

> "I built the full agentic version — LangGraph, twelve nodes, a real verify→replan→worker cycle, a
> hand-written ReAct loop, SQLite checkpointing for resume and time-travel debugging, and a
> long-term store of lessons from successful retries.
>
> Then I measured it, and it failed three times running. First: quality up two points, but tokens
> **4.45×** — and I traced that to something structural, not noise, because each iteration resends
> the whole transcript so tokens grow quadratically in step count. Second: I cut cost to 1.37× and
> quality dropped four points. Third, a hybrid gate, failed all three criteria — but the LLM server
> degraded mid-experiment, so I recorded it as confounded rather than concluding hybrid was bad.
>
> The thing I learned most from was the **meta-finding after fifteen runs**: variance was swallowing
> the signal. Every arm had a good run and a bad run — at n=3, a ±2-3 line effect simply **isn't
> measurable**. So I closed the react track, but closed it **conditionally**, writing down three
> preconditions for reopening it.
>
> Then I changed direction entirely: instead of fixing the agent, I fixed the **verifier**. The
> insight was that when an item escalates under *every* configuration — including on the
> human-made golden file — the first suspect is your own hard check, not the model. Three rounds of
> verifier fixes took escalations from 10 down to 5.6, a 44% reduction, at **zero additional token
> cost** — while three rounds of agent changes had all been rejected.
>
> And the ending: once the serving infrastructure was stable — precondition one from when I closed
> the track — I reopened react and this time it **won**: escalations down to 3.0, and more
> importantly item_code accuracy at 93.9% versus static's 88.6%. I checked that metric specifically
> because of the painful lesson from round two, where escalations dropped but the model was actually
> **'confidently wrong'** — flagging fewer items as missing while being wrong more often. For a BOM,
> wrong-but-confident is more dangerous than missing-but-flagged, because the reviewer doesn't know
> where to look."

#### Bốn câu trích thẳng từ tài liệu của bạn — đáng thuộc lòng

> *"Khi item escalate ở MỌI cấu hình kể cả golden, nghi phạm đầu tiên là VERIFIER, không phải model."*
> — `BAKEOFF_RFC001.md:240-243`

> *"Model ít khai [MISSING] hơn nhưng sai nhiều hơn = **confidently wrong** — với BOM, sai-mà-tự-tin
> nguy hiểm hơn thiếu-có-đánh-dấu."* — `BAKEOFF_RFC001.md:186-190`

> *"Không có eval harness thì +2pp overall đã đủ thuyết phục để merge — và ta sẽ âm thầm gánh chi phí
> gấp 4.5 lần trong production."* — commit `1f129f4`

> *"Phương sai nuốt chửng tín hiệu... với n=3, hiệu ứng ±2-3 dòng KHÔNG đo được."*
> — `BAKEOFF_RFC001.md:133-136`

> 💡 **Vì sao vòng cung này ăn điểm hơn hẳn "tôi biết dùng LangGraph":** nó chứng minh bốn thứ cùng
> lúc — biết xây agentic đầy đủ; biết thiết kế thí nghiệm có kiểm soát; biết **tự bác bỏ** khi số
> liệu không ủng hộ; và biết **quay lại** khi điều kiện đổi. Riêng chuyện đóng track *có điều kiện
> ghi sẵn* rồi mở lại đúng điều kiện đó là thứ rất hiếm gặp.

> ⚠️ **Đừng nói "agentic không hiệu quả"** — sai với chính dữ liệu của bạn, vì cuối cùng react đã
> được chấp nhận. Kết luận đúng và hẹp: *"react chỉ trả đủ giá trị khi hạ tầng serve ổn định và khi
> tôi đã sửa xong verifier — trước đó thì không."*

---

## 2. Sự thật kiến trúc — hai tầng rất khác nhau

Đây là phần **quan trọng nhất** để trả lời đúng, vì nếu nói sai bạn sẽ bị bắt bài ngay ở câu hỏi
đào sâu đầu tiên.

### Tầng 1 — pipeline (Agent 3 → 10): tuần tự cứng, KHÔNG agentic

`orchestrator_team_a.py:89-99` — toàn bộ logic "agent nào chạy tiếp theo" chỉ là một vòng `for`:

```python
def _run_sequential(self, state: BOMPipelineState) -> BOMPipelineState:
    for fn in [
        self._agent3_trimlist_parser,
        self._agent4_product_context_and_base_bom,
        self._agent5_item_planner,
        self._agent6_material_resolver,
        self._agent7_costing,
        self._agent8_place_to_use,
        self._agent9_qa_traceability,
    ]:
        state = fn(state)
```

Docstring dòng 1 của file ghi thẳng: `"""Sequential BOM orchestrator (Team A) without LangGraph."""`

**Ở repo production này không có LangGraph.** `langgraph` không phải dependency khai báo — nó chỉ tồn
tại gián tiếp vì `langchain` kéo theo. Có 2 tàn dư chết cần biết để không bị hớ: flag
`require_langgraph: bool = False` (`core/config.py:39`, không có chỗ nào đọc) và giá trị `'langgraph'`
trong enum `graph_mode` (`models/domain.py:382`, không bao giờ được gán — orchestrator hardcode
`graph_mode="langchain"`).

> ⚠️ **Quan trọng — đừng nói "tôi không dùng LangGraph" một cách chung chung.** Bạn **có** dùng
> LangGraph thật, chỉ là ở repo khác (`motivesidp-bom-agentic`: `StateGraph`, 12 node, 4 conditional
> edge, chu trình, checkpointer). Cách nói đúng: *"Hệ production không dùng LangGraph — plan tất định
> nên graph không mang lại gì. Nhưng tôi có một hệ song song dùng LangGraph đầy đủ, và tôi chọn khác
> nhau giữa hai nơi là có lý do."* Xem [§1B](#1b--hai-hệ-thống-một-vòng-cung--đây-mới-là-câu-chuyện-đầy-đủ).

### Tầng 2 — `BaseAgent`: plan tất định, LLM nằm TRONG tool

`services/base_agent.py:63-68` — docstring do chính bạn viết, và đây là câu trả lời hoàn hảo cho
câu hỏi *"làm sao đảm bảo agent chạy đúng flow"*:

```python
"""Minimal agent base.

Memory is a plain dict shared across all tool calls within a single ``run()``.
Tools are zero-arg callables (bound methods) that read/write ``self.memory``.
The plan is built deterministically by ``_make_plan()`` — no LLM planning step.
LLMs are used inside tools, never to decide the plan itself.
"""
```

> **Câu chốt để nói ra miệng:** *"LLM được dùng bên trong tool, không bao giờ dùng để quyết định
> plan."* — Một câu này trả lời gọn cả câu hỏi flow control lẫn câu hỏi reliability.

### Bảng đối chiếu — nói thế nào cho đúng

| Nếu bạn nói... | Rủi ro | Nói thế này thay vào |
|---|---|---|
| "Hệ thống của tôi là agentic AI" | 🔴 Sai. Plan là code Python cứng | "Đây là **staged pipeline**, một số bước có gọi LLM bên trong. Tôi cố ý **không** để LLM lập kế hoạch" |
| "Hệ production của tôi dùng LangGraph" | 🔴 Sai với repo đó | "Hệ production dùng LangChain làm lớp gọi LLM + structured output, không cần graph. **Hệ agentic song song thì dùng LangGraph đầy đủ** — 12 node, có chu trình, có checkpointer" |
| "Agent tự quyết bước tiếp theo" | 🔴 Sai | "Thứ tự bước là tất định. Đây là quyết định thiết kế có chủ đích, không phải hạn chế" |
| "Plan tất định, LLM nằm trong tool" | ✅ Đúng, và là điểm mạnh | Giữ nguyên |

---

## 3. Bốn câu hỏi bạn muốn chuẩn bị — trả lời đầy đủ

### 3.1 "AI agent được ứng dụng thế nào trong hệ thống của bạn?"

#### Tiếng Việt

> "Tôi muốn tách rõ 2 tầng, vì từ 'agent' bị dùng lẫn lộn nhiều.
>
> **Tầng ngoài** là một pipeline 8 bước — parse trimlist, resolve product context, plan item, resolve
> material, costing, place-to-use, QA, export. Chúng tôi gọi là 'Agent 3' đến 'Agent 10' nhưng nói
> chính xác thì đây **không** phải agent tự trị: thứ tự bước là một list Python cứng, không có LLM
> nào lập kế hoạch. Đó là lựa chọn có chủ đích — đầu ra là BOM đi vào đơn mua hàng thật, nên tôi
> không muốn thứ tự xử lý thay đổi giữa 2 lần chạy cùng input.
>
> **Tầng trong** mới là chỗ AI làm việc thật: VLM đọc trang techpack và trả JSON có cấu trúc, LLM
> phân loại subgroup/chest-type, LLM chọn giữa mã light/dark, và một pass QA phát hiện bất thường.
> Mỗi chỗ là một lời gọi có input/output rõ ràng, có schema, có thể tắt bằng flag.
>
> Chỗ duy nhất từng có agent tự trị đúng nghĩa — LLM tự chọn tool, tự lặp — là Agent 6 Material
> Resolver. Tôi sẽ nói riêng về nó vì câu chuyện thú vị nằm ở việc tôi **gỡ nó ra**."

#### English

> "Let me separate two layers, because 'agent' gets used loosely.
>
> **The outer layer** is an 8-step pipeline — parse trimlist, resolve product context, plan items,
> resolve materials, costing, place-to-use, QA, export. We call them 'Agent 3' through 'Agent 10',
> but to be precise these are **not** autonomous agents: the step order is a hard-coded Python list,
> with no LLM planning. That was deliberate — the output is a BOM that drives real purchase orders,
> so I don't want the processing order to vary between two runs on the same input.
>
> **The inner layer** is where AI actually works: a VLM reads techpack pages and returns structured
> JSON, an LLM classifies subgroup and chest type, an LLM picks between light and dark item codes,
> and a QA pass flags anomalies. Each is a bounded call with a defined schema, and each can be
> switched off by a flag.
>
> The one place that had a genuinely autonomous agent — LLM choosing tools, looping on its own — was
> Agent 6, the material resolver. I'll talk about that separately, because the interesting part is
> that I **took it out**."

---

### 3.2 "Memory khai báo và quản lý thế nào?"

Bạn có bằng chứng ở **cả hai đầu phổ**, và nói được cả hai + lý do chọn khác nhau chính là câu trả
lời cấp Lead.

| Tầng | `motivesidp-ai-service` (production) | `motivesidp-bom-agentic` (agentic) |
|---|---|---|
| **State máy** | `BOMPipelineState` — Pydantic ~30 field (`models/domain.py:360-390`) | `AgentState` — **TypedDict + reducer** (`state.py:18`) |
| **Ghi song song** | Không cần (tuần tự) | **`merge_tasks` reducer** — bắt buộc, vì nhiều `Send` cùng ghi `rule_tasks` |
| **Scratchpad trong agent** | `self.memory: dict` (`base_agent.py:75`) | — |
| **Short-term / resume** | ❌ `state.json` write-only, **không resume** | ✅ **`AsyncSqliteSaver`** — resume + **time-travel debug** |
| **Long-term / cross-run** | ❌ Không có | ✅ **`Lesson` store** — học từ retry thành công |
| **Conversation memory** | ❌ Không (cố ý — không phải chatbot) | ❌ Không |

**Hai chi tiết đáng nói nhất — cả hai đều là bug/bẫy thật đã nghĩ qua:**

**(a) Reducer cho ghi song song** (`state.py:8-15`) — không có nó là mất dữ liệu:

```python
def merge_tasks(left, right) -> list[RuleTask]:
    """Merge by rule_id; the newer entry (higher attempt / later verdict) wins."""
```

Vì nhiều nhánh `Send` chạy song song đều ghi `rule_tasks`, last-write-wins sẽ nuốt kết quả. Hệ quả
kéo theo: vì `merge_tasks` **không bao giờ xoá**, node `plan` phải chủ động đánh dấu task bị hấp thụ
thành `Verdict.SKIPPED` để tránh chạy hai lần (`plan/node.py:31-38`).

**(b) Định danh cross-run — bẫy tinh vi** (`memory/store.py`):

```python
def lesson_identity(task: RuleTask) -> str:
    """Định danh ổn định CROSS-RUN của một task: item name chuẩn hoá (upper, gộp space).
    KHÔNG dùng rule_id — LINE task có rule_id "L{i}:{item}" chứa index dòng base,
    đổi file BOM base là index lệch -> lesson của run trước không bao giờ match."""
```

→ Đây là loại chi tiết chứng minh bạn đã **thực sự vận hành** memory cross-run, không chỉ bật một
tính năng lên.

#### Tiếng Việt

> "Chúng tôi không dùng conversation memory, và đó là chủ đích — hệ thống không phải chatbot, mỗi
> lần chạy là một job độc lập trên một techpack.
>
> State chính là một Pydantic model tên `BOMPipelineState`, khoảng 30 field, được truyền xuyên suốt
> 8 bước và mỗi bước mutate rồi trả lại. Nó chứa cả dữ liệu nghiệp vụ — `bom_rows`, `trimlist_parsed`,
> `qa_result` — lẫn phần cross-cutting: `llm_calls_total`, `llm_tokens_total`, `llm_calls_by_agent`,
> `human_review_required`, và `agent_history`.
>
> Lý do dùng Pydantic model thay vì dict: mỗi bước đọc field của bước trước, nên tôi cần lỗi nổ ra
> lúc gán sai kiểu chứ không phải lúc export Excel ở bước cuối.
>
> Ở trong một agent thì memory là một dict phẳng, chia sẻ giữa các tool trong cùng một `run()`. Tool
> là bound method không tham số, đọc/ghi thẳng vào dict đó. Có một quy ước nhỏ tôi thích: key bắt đầu
> bằng dấu gạch dưới được coi là private và bị lọc khỏi output cuối — `base_agent.py:130`.
>
> Về persistence: sau mỗi bước hệ thống ghi snapshot `state.json` xuống đĩa theo `pipeline_run_id`.
> Nhưng phải nói thẳng — ở hệ production này đó là **write-only**, dùng để debug và audit, không có
> đường đọc lại, nên chưa resume được một run đang dở.
>
> Điều thú vị là ở **hệ agentic tôi làm song song**, tôi đã giải đúng bài đó: dùng checkpointer
> SQLite của LangGraph, state được checkpoint sau mỗi superstep, nên resume được và **time-travel
> debug** được — quay lại đúng trạng thái trước khi một node chạy sai. Cộng thêm một tầng long-term
> memory riêng lưu 'lesson' từ những lần retry thành công, để planner của run sau đọc lại trước khi
> lập kế hoạch.
>
> Nên nói chính xác hơn: tôi đã build cả hai mức. Hệ production chọn mức nhẹ vì mỗi run chỉ vài phút
> và chạy lại từ đầu rẻ hơn chi phí vận hành một checkpointer. Nếu run kéo dài hàng chục phút hoặc
> tốn nhiều token thì phép tính đó đảo chiều — và lúc đó tôi đã có sẵn cách làm."

#### English

> "We don't use conversation memory, and that's deliberate — this isn't a chatbot; each run is an
> independent job over one techpack.
>
> The main state is a Pydantic model, `BOMPipelineState`, about 30 fields, threaded through all
> eight steps with each step mutating and returning it. It carries both business data — `bom_rows`,
> `trimlist_parsed`, `qa_result` — and cross-cutting concerns: `llm_calls_total`,
> `llm_tokens_total`, `llm_calls_by_agent`, `human_review_required`, and `agent_history`.
>
> Why a Pydantic model instead of a dict: each step reads fields the previous step wrote, so I want
> the failure at the point of a bad assignment, not at Excel export in the last step.
>
> Inside a single agent, memory is a flat dict shared across tool calls within one `run()`. Tools are
> zero-argument bound methods that read and write that dict directly. There's a small convention I
> like: keys starting with an underscore are treated as private and filtered out of the final output.
>
> On persistence: after each step we snapshot `state.json` to disk under the `pipeline_run_id`. But I
> should be straight about this — in the production system that's **write-only**, for debugging and
> audit, with no read path, so we can't resume a partially-completed run.
>
> What's interesting is that in the **agentic system I built alongside it**, I did solve exactly
> that: LangGraph's SQLite checkpointer, state checkpointed after every superstep, so runs resume
> and I get **time-travel debugging** — rewind to the state just before a node went wrong. Plus a
> separate long-term layer storing 'lessons' from successful retries, which the next run's planner
> reads before planning.
>
> So more precisely: I've built both tiers. Production picked the light one because a run takes a few
> minutes and re-running from scratch is cheaper than operating a checkpointer. If runs took tens of
> minutes or burned serious tokens, that calculation flips — and I'd already know how to do it."

> 💡 **Vì sao đoạn "hạn chế" lại làm câu trả lời MẠNH hơn:** interviewer level Lead luôn hỏi tiếp
> *"vậy nếu chạy dở thì sao?"*. Nói trước điểm yếu + hướng sửa = bạn kiểm soát câu chuyện. Giấu đi
> rồi bị hỏi trúng = mất điểm gấp đôi.

---

### 3.3 "Tool calling hoạt động thế nào?"

Trong codebase có **hai cơ chế tool hoàn toàn khác nhau**. Giải thích được *vì sao* lại có hai
chính là câu trả lời phân biệt Senior với Lead.

#### Cơ chế A — tool nội bộ, zero-arg, mutate memory

```python
# base_agent.py — tool là bound method không tham số
def register_tool(self, name: str, fn: Callable[[], dict[str, Any]]) -> None:
    """Register a bound method as a callable tool."""
    self._tools[name] = fn
```

LLM **không nhìn thấy** những tool này. Chúng chỉ là đơn vị thực thi có tên để trace và retry.

#### Cơ chế B — LangChain `StructuredTool` cho LLM tự chọn

`tools/material_search.py:169-200` — 3 tool, mỗi tool có `args_schema` là Pydantic model:

```python
StructuredTool.from_function(
    func=self._tool_lookup_by_code,
    name="lookup_material_by_code",
    description=(
        "Look up Product Master candidates by an exact material item code. "
        "Use this when a BOM item_code is available."
    ),
    args_schema=LookupByCodeInput,
),
```

Với schema chặn cứng biên (`material_search.py:88-107`):

```python
class LookupByCodeInput(BaseModel):
    item_code: str = Field(min_length=1, max_length=128, description="Exact material item code")
    limit: int = Field(default=5, ge=1, le=5)   # LLM không thể xin 1000 kết quả
```

#### Tiếng Việt

> "Có hai loại tool trong hệ thống và tôi cố ý không gộp chúng làm một.
>
> Loại thứ nhất là tool nội bộ của pipeline — bound method không tham số, đọc/ghi vào memory dict
> chung. LLM không hề nhìn thấy chúng. Chúng tồn tại để tôi có đơn vị nhỏ nhất để trace, retry và
> đo token riêng từng bước. Gọi là 'tool' hơi dễ gây nhầm, thực chất là step có tên.
>
> Loại thứ hai mới là tool calling đúng nghĩa: LangChain `StructuredTool` với `args_schema` là
> Pydantic model, LLM nhận schema qua function-calling contract của OpenAI-compatible API và tự sinh
> JSON args. Có 3 tool: tra cứu theo mã chính xác, tra theo mã nhà cung cấp, và semantic search trên
> vector index.
>
> Ba chi tiết tôi thấy đáng nói:
>
> Thứ nhất, **biên được chặn ngay trong schema** chứ không phải trong prompt — `limit` khai báo
> `ge=1, le=5`, nên kể cả LLM có xin 1000 kết quả thì Pydantic từ chối trước khi chạm database.
> Ràng buộc bằng type an toàn hơn ràng buộc bằng lời văn.
>
> Thứ hai, **thứ tự ưu tiên tool được mã hóa vào chính description**: 'dùng cái này khi có item_code',
> 'chỉ dùng semantic search khi hai cách kia không giải quyết được'. Đây là điều hướng bằng prompt,
> và tôi biết rõ nó là gợi ý chứ không phải ràng buộc cứng — chúng tôi không dùng `tool_choice` để ép.
>
> Thứ ba, và tôi nghĩ đây là phần quan trọng nhất: **LLM chỉ được chọn candidate mà tool thực sự trả
> về**. Mỗi kết quả tool được cache lại theo `candidate_id`, và lựa chọn cuối của LLM bị validate
> ngược lại cache đó — nếu nó bịa ra một mã vật liệu không có trong danh sách thì code raise lỗi chứ
> không im lặng chấp nhận. System prompt cũng ghi thẳng: *'Never invent a material code'*. Nhưng tôi
> không tin vào prompt, nên có thêm lớp kiểm tra bằng code."

#### English

> "There are two kinds of tools in the system, and I deliberately kept them separate.
>
> The first kind are internal pipeline tools — zero-argument bound methods that read and write a
> shared memory dict. The LLM never sees them. They exist so I have a smallest traceable unit for
> retry and per-step token accounting. Calling them 'tools' is a bit of a misnomer; they're really
> named steps.
>
> The second kind is real tool calling: LangChain `StructuredTool` with a Pydantic `args_schema`. The
> LLM receives that schema through the OpenAI-compatible function-calling contract and generates JSON
> arguments itself. There are three: exact item-code lookup, supplier-article lookup, and semantic
> search over the vector index.
>
> Three details I think are worth calling out:
>
> First, **the bounds live in the schema, not the prompt** — `limit` is declared `ge=1, le=5`, so even
> if the model asks for a thousand results, Pydantic rejects it before it reaches the database.
> Constraining by type is safer than constraining by prose.
>
> Second, **the tool priority order is encoded in the descriptions themselves**: 'use this when an
> item_code is available', 'use semantic search only after exact and supplier-article lookup are
> insufficient'. That's prompt-level steering, and I'm clear-eyed that it's a hint, not a hard
> constraint — we don't force it with `tool_choice`.
>
> Third, and I think this is the important one: **the LLM may only select a candidate a tool actually
> returned**. Every tool result is cached by `candidate_id`, and the model's final selection is
> validated against that cache — if it invents a material code that wasn't in the list, the code
> raises rather than silently accepting it. The system prompt does say *'Never invent a material
> code'*, but I don't trust prompts, so there's a code-level check behind it."

#### Phần nâng cao — tool scoping 3 tầng ở hệ agentic (dùng khi bị đào sâu)

Ở `motivesidp-bom-agentic` có 12 tool và **ba tầng thu hẹp**, mỗi tầng có lý do khác nhau:

| Tầng | Cơ chế | Lý do |
|---|---|---|
| 1. Theo node | `registry.tools_for(node)` — `rule_worker` 9 tool, `plan` 3, `classify` 2 | Docstring: *"less tool-choice noise for a 27B OSS model"* |
| 2. Theo task | `react_tools(task)` — scope `core` chỉ bind 2 tool material, thêm sheet tool **chỉ khi** rule có `cross_refs` | Docstring ghi số đo: *"bakeoff v1: 9 tools = **4.45x token**"* |
| 3. Runtime | `_ALLOWED` set, tool ngoài danh sách trả `"TOOL_NOT_ALLOWED"` | Phòng khi model gọi tool không được bind |

> **Câu trả lời khi bị hỏi "tool nhiều thì sao":** *"Tôi đo được là bind cả 9 tool làm token gấp
> **4.45 lần** — vì mỗi vòng ReAct gửi lại toàn bộ schema tool cộng lịch sử, nên chi phí tăng theo
> bình phương số bước. Nên tôi thu hẹp theo ba tầng: theo node, rồi theo từng task cụ thể dựa vào
> việc rule đó có cross-reference hay không, và cuối cùng là một allow-list ở runtime để model không
> gọi được tool chưa bind. Bài học: **với model nhỏ, tool schema là chi phí context, không phải tính
> năng miễn phí** — mỗi tool thêm vào là tiền và là thêm cơ hội chọn sai."*

**Và một chi tiết về error handling** (`actor.py:87-98`): lỗi tool **không** raise, mà được đưa trở
lại làm observation (`TOOL_ERROR: {exc}`) để model tự sửa ở vòng sau. Đây đúng ranh giới *"lỗi nào
trả về cho model, lỗi nào ném lên trên"* mà [04-ai-agent-system-design.md](katalon-prep-common/04-ai-agent-system-design.md) bàn.

---

### 3.4 ⭐ "Làm sao đảm bảo AI agent thực thi đúng flow bạn mong muốn?"

Đây là câu **mạnh nhất** của bạn. Trả lời theo 5 tầng, từ ngoài vào trong.

#### Tiếng Việt

> "Tôi trả lời bằng 5 tầng, vì không có một cơ chế đơn lẻ nào đủ.
>
> **Tầng 1 — không cho LLM quyết định flow.** Đây là quyết định gốc. Plan được build tất định trong
> code Python; LLM được gọi bên trong từng bước, không bao giờ được hỏi 'bước tiếp theo là gì'. Docstring
> trong `BaseAgent` ghi đúng câu đó. Đánh đổi rất rõ: hệ thống mất khả năng tự thích nghi với input lạ
> — nhưng đổi lại, cùng một input luôn đi qua cùng một đường, và khi có sự cố tôi biết chính xác phải
> đọc bước nào. Với domain mà output thành đơn mua hàng, tôi chọn đánh đổi đó không do dự.
>
> **Tầng 2 — ràng buộc bằng schema, không bằng lời văn.** Mọi output LLM đều bị ép về Pydantic model.
> Tool args cũng vậy, có min/max length và biên số. Nếu LLM trả sai hình dạng, nó fail ở tầng
> validation chứ không lan xuống dưới.
>
> **Tầng 3 — grounding: chỉ chấp nhận cái verify được ngược lại nguồn.** Với extraction, mã vật liệu
> do VLM đọc ra bị đối chiếu ngược vào text gốc của trang; không tìm thấy thì set null chứ không giữ.
> Với tool calling, LLM chỉ được chọn candidate mà tool đã trả về, và lựa chọn bị validate lại. Nguyên
> tắc chung: **LLM được phép chọn, không được phép sáng tác.**
>
> **Tầng 4 — khi không chắc thì dừng, không đoán.** Mỗi field mang provenance: nguồn nào, giá trị gốc
> nào, mức tin cậy nào. Confidence không phải do LLM tự chấm — với material nó suy ra từ số candidate
> khớp: đúng 1 thì HIGH, từ 2-4 là MEDIUM, trên 4 là LOW. Khi không resolve được, row bị đánh TBA,
> `confidence_score = 0.0`, `needs_human_review = True`.
>
> **Tầng 5 — quan sát được.** Mỗi bước ghi trace entry có step_id, tool, số token, thời gian, số lần
> retry và lỗi. Pipeline state đếm `llm_calls_total`, `llm_tokens_total`, và `llm_calls_by_agent`.
> Có Langfuse cho trace LLM. Retry là 3 lần với backoff tuyến tính, và nếu một bước fail hết 3 lần
> thì pipeline dừng luôn — fail fast, không chạy tiếp với dữ liệu thiếu."

#### English

> "I'd answer in five layers, because no single mechanism is enough.
>
> **Layer 1 — don't let the LLM decide the flow.** That's the root decision. The plan is built
> deterministically in Python; the LLM is called inside individual steps and is never asked 'what's
> next'. The `BaseAgent` docstring says exactly that. The trade-off is explicit: the system loses the
> ability to adapt to unusual inputs — but in exchange, identical input always takes the identical
> path, and when something breaks I know precisely which step to read. For a domain where the output
> becomes a purchase order, I take that trade without hesitation.
>
> **Layer 2 — constrain with schemas, not prose.** Every LLM output is coerced into a Pydantic model.
> Tool arguments too, with min/max lengths and numeric bounds. If the model returns the wrong shape,
> it fails at validation rather than propagating downstream.
>
> **Layer 3 — grounding: only accept what can be verified against the source.** On extraction, item
> codes the VLM reads are checked back against the page's source text; if not found, the field is
> nulled rather than kept. On tool calling, the model may only pick a candidate a tool returned, and
> that pick is re-validated. The general rule: **the LLM may select, it may not invent.**
>
> **Layer 4 — when uncertain, stop rather than guess.** Every field carries provenance: which source,
> which raw value, what confidence. Confidence isn't self-reported by the model — for materials it's
> derived from how many candidates matched: exactly one is HIGH, two to four is MEDIUM, more than four
> is LOW. When nothing resolves, the row is marked TBA with `confidence_score = 0.0` and
> `needs_human_review = True`.
>
> **Layer 5 — make it observable.** Every step writes a trace entry with step id, tool, tokens,
> duration, retry count and error. The pipeline state tracks `llm_calls_total`, `llm_tokens_total`, and
> `llm_calls_by_agent`. Langfuse handles LLM tracing. Retries are three attempts with linear backoff,
> and if a step exhausts them the pipeline stops — fail fast rather than continuing on partial data."

#### Bằng chứng code cho từng tầng

| Tầng | File:line | Nội dung |
|---|---|---|
| 1. Flow tất định | `base_agent.py:67-68` | *"The plan is built deterministically... LLMs are used inside tools, never to decide the plan itself"* |
| 1. Flow tất định | `orchestrator_team_a.py:89-99` | vòng `for` cứng qua 7 agent |
| 2. Schema | `material_search.py:88-107` | `Field(min_length=1, max_length=128)`, `Field(ge=1, le=5)` |
| 2. Schema | `chains/product_context_chain.py:11-18` | `llm.with_structured_output(SubgroupDetectionResult, method="json_mode")` |
| 3. Grounding | `vlm/services/document_analysis.py:124-145` | `_ground_item_codes()` — null hóa mã không có trong text nguồn |
| 3. Grounding | `material_resolver.py:766-787` | validate lựa chọn LLM ngược lại candidate cache |
| 3. Grounding | `prompts/agent06.../tool_search.yml:8-9` | *"You may select only a candidate returned by a tool call. Never invent a material code"* |
| 4. Confidence | `services/provenance.py:150-155` | `material_confidence_from_candidate_count()` — 1→HIGH, ≤4→MEDIUM, >4→LOW |
| 4. Escalation | `material_resolver.py:143-165` | TBA + `confidence_score=0.0` + `needs_human_review=True` |
| 5. Trace | `base_agent.py:37-46` | `TraceEntry(step_id, tool_name, tokens_used, duration_ms, status, attempts, error)` |
| 5. Retry | `base_agent.py:15-16, 158-185` | `MAX_STEP_RETRIES=3`, `RETRY_BACKOFF_S=0.5 * attempt` |

> ⚠️ **Bẫy hay bị hỏi tiếp:** *"Backoff của anh có jitter không?"* — **Không có.** `RETRY_BACKOFF_S * attempt`
> là backoff tuyến tính thuần. Trả lời thẳng: *"Không, và với workload hiện tại thì chưa thành vấn đề vì
> retry là per-request chứ không phải nhiều client cùng retry vào một endpoint. Nếu scale ra nhiều
> worker gọi chung một VLM endpoint thì cần thêm jitter để tránh thundering herd."* — Biết mình thiếu gì
> mạnh hơn là giả vờ đủ.

---

## 4. Câu chuyện mạnh nhất — bạn tự gỡ LLM của chính mình

Đây là story nên để dành cho vòng **hiring manager** hoặc **stakeholder**. Nó cùng thể loại với
story bakeoff RFC trong [CV-BASED-ANSWERS.md](CV-BASED-ANSWERS.md): *dữ liệu thắng cái tôi*.

### Sự thật đã verify

Commit `b15d009` — tác giả `ducnm`, ngày **23/07/2026**, message *"MIDP-456 update confidence score
and provenance"*. Diff trên `material_resolver.py` **xóa** đường fallback LLM:

```diff
-            # ── Fallback: LangChain tool-assisted search (LLM) ─────────────────────
-            try:
-                mat = self._tool_assisted_search(row, trim, context)
-                if mat:
-                    row.resolution_source = 'MATERIAL_TOOL_LLM_SELECTED'
-                    row.confidence_score  = 0.85
+            else:
+                set_field(row, "item_code", row.item_code,
+                  make_provenance(
+                    SourceType.TRIMLIST,
+                    trim.source_file,
+                    confidence_type=ConfidenceType.LOW,
+                ))
+                row.resolution_source  = 'TBA'
+                row.confidence_score   = 0.0
+                row.needs_human_review = True
```

Tức là: **thay vì để LLM đoán khi tra cứu tất định thất bại, hệ thống để trống và đẩy cho người review.**

### Kể thế nào

#### Tiếng Việt

> "Agent 6 giải bài toán map một dòng BOM sang mã vật liệu thật trong Product Master. Thiết kế ban
> đầu có 2 tầng: một bộ lọc tất định 5 bước trên database, và nếu nó không ra kết quả thì fallback
> sang một tool-calling agent — LLM tự chọn tool, tự tra, tự chọn candidate, gắn confidence 0.85.
>
> Tôi đã build xong tầng đó, có test đầy đủ. Rồi tôi gỡ nó ra khỏi đường chạy mặc định.
>
> Lý do: hỏi lại đúng câu 'hỏng thì hỏng kiểu gì'. Output của hệ thống này là file BOM mà bộ phận mua
> hàng dùng để đặt nguyên liệu thật. Khi bộ lọc tất định không tìm ra, nghĩa là dữ liệu thực sự mơ hồ.
> Cho LLM đoán trong tình huống mơ hồ tạo ra thứ nguy hiểm nhất: **một mã vật liệu trông hoàn toàn hợp
> lý nhưng sai**. Nó có định dạng đúng, nhà cung cấp đúng, và mang confidence 0.85 — nên không ai
> review, và nó đi thẳng vào đơn hàng.
>
> Một ô trống thì ngược lại: nó xấu, nó gây khó chịu, nhưng nó **bắt buộc** phải có người xử lý. Trong
> domain này, một lỗi ồn ào rẻ hơn nhiều so với một lỗi im lặng.
>
> Nên tôi đổi fallback thành: đánh dấu TBA, confidence 0.0, `needs_human_review=True`, và ghi provenance
> nói rõ giá trị này đến từ TrimList ở mức tin cậy thấp. Người review nhìn vào biết ngay tại sao nó ở
> đó và cần kiểm tra gì.
>
> Code agent và test tôi vẫn giữ. Nó chưa chết vĩnh viễn — nó chờ một validation gate. Khi nào tôi có
> cách verify một lựa chọn của LLM **trước khi** tin nó, tôi sẽ bật lại. Vấn đề chưa bao giờ là LLM
> không chọn được; vấn đề là tôi chưa có cách chứng minh lựa chọn đó đúng."

#### English

> "Agent 6 solves the problem of mapping a BOM line to a real material code in the Product Master.
> The original design had two tiers: a deterministic five-step database filter, and if that returned
> nothing, a fallback to a tool-calling agent — the LLM picks tools, searches, selects a candidate,
> and we stamped it confidence 0.85.
>
> I built that tier, with full tests. Then I took it out of the default path.
>
> The reason was asking the right question: not 'does it work' but 'how does it fail'. The output of
> this system is a BOM the purchasing team uses to order real materials. When the deterministic
> filter finds nothing, that means the data genuinely is ambiguous. Letting an LLM guess under
> ambiguity produces the most dangerous possible artifact: **a material code that looks entirely
> plausible and is wrong**. Right format, right supplier, and carrying 0.85 confidence — so nobody
> reviews it, and it flows straight into a purchase order.
>
> A blank does the opposite. It's ugly, it's annoying, and it **forces** someone to deal with it. In
> this domain, a loud failure is far cheaper than a silent one.
>
> So I changed the fallback to: mark the row TBA, confidence 0.0, `needs_human_review=True`, and write
> provenance recording that the value came from the TrimList at low confidence. A reviewer can see
> immediately why it's there and what to check.
>
> I kept the agent code and its tests. It isn't dead — it's waiting on a validation gate. The day I
> can verify an LLM's selection **before** trusting it, I'll switch it back on. The problem was never
> that the LLM couldn't choose; it's that I had no way to prove the choice was right."

### Vì sao story này ăn điểm ở Katalon

Katalon công bố chính thức rằng AI của họ là **"trust and accountability layer"** — *"AI decisions
must be explainable"*, *"humans are accountable for every release"*. Câu chuyện của bạn là **cùng
một nguyên tắc, đã thực thi bằng code, trong một domain có hậu quả tài chính**. Bạn có thể nói thẳng:

> *"Tôi đọc phần Katalon nói AI phải explainable và con người chịu trách nhiệm cuối. Đó chính là kết
> luận tôi rút ra trong dự án của mình, theo cách khá đau — tôi đã phải gỡ đường LLM mà chính tôi
> viết ra. Nên khi TrueTest nói không auto-merge test case sinh tự động vào suite của khách, tôi hiểu
> vì sao ràng buộc đó tồn tại."*

---

## 4B. ⭐ Bug hay nhất bạn có — và cách bạn chặn nó tái diễn

Đây là câu trả lời cho *"kể về một bug khó"* và *"anh nâng chất lượng code của team thế nào"* — cùng
một câu chuyện, và nó có một artifact rất ít người có.

### Bug

Commit `86bc5e0` (12/07/2026). Flag `ENABLE_REPLAN=false` **trông như** hoạt động, nhưng router chỉ
nhìn `verdict == Verdict.RETRY`:

```python
def route_after_verify(state):
    retry = [t for t in state["rule_tasks"] if t.wave == wave and t.verdict == Verdict.RETRY]
    if retry:
        return [Send("replan", {**state, "task": t}) for t in retry]
```

Và có **hai chỗ khác** tự set `verdict = RETRY` mà không hề biết đến flag: `rule_worker` khi worker
gặp exception, và `fixed_batch` khi batch sót `rule_key`. Tệ hơn: hai đường đó **không check
`max_attempts`** → vòng lặp `replan → worker → lỗi → replan …` chạy tới khi đụng
`recursion_limit: 200`. Thực tế đã ghi nhận chạy tới **attempt 31**.

### Fix — hợp nhất quyền quyết định về đúng một chỗ

```diff
--- a/app/agent/nodes/rule_worker/node.py
-            task.verdict = Verdict.RETRY
+            # verdict để PENDING, verify là NƠI DUY NHẤT quyết RETRY/ESCALATED
+            # (tôn trọng ENABLE_REPLAN + max_attempts)
```

Còn lại đúng một nơi có quyền (`verify/node.py:82-87`):

```python
can_retry = task.attempt < task.max_attempts and settings.enable_replan
task.verdict = Verdict.RETRY if can_retry else Verdict.ESCALATED
```

### Artifact đáng giá nhất — test bất biến kiến trúc

`tests/unit/test_retry_flags.py:94-105` — không test hành vi, mà **test chính cấu trúc source code**:

```python
def test_no_retry_setter_outside_verify():
    """Chốt kiến trúc: verify là nơi DUY NHẤT set Verdict.RETRY."""
    root = pathlib.Path(__file__).resolve().parents[2] / "app"
    offenders = []
    for f in root.rglob("*.py"):
        if f.name == "node.py" and f.parent.name == "verify":
            continue
        for i, line in enumerate(f.read_text().splitlines(), 1):
            if re.search(r"verdict\s*=\s*Verdict\.RETRY", line):
                offenders.append(f"{f}:{i}")
    assert not offenders, offenders
```

**Tôi đã tự verify: bất biến này hiện vẫn đúng** — grep toàn `app/` chỉ ra đúng 1 kết quả,
`verify/node.py:85`.

### Kể thế nào

> **VI:** *"Bug này dạy tôi một nguyên tắc tôi mang theo từ đó: **một feature flag phải có đúng một
> điểm thực thi.** Flag `ENABLE_REPLAN` của tôi được check ở node verify, và nhìn thì hoạt động đúng.
> Nhưng có hai đường code khác tự set verdict thành RETRY, và router thì chỉ nhìn verdict — nó không
> biết flag tồn tại. Nên flag tắt mà replan vẫn chạy, và vì hai đường đó cũng không check
> max_attempts nên nó lặp vô hạn tới recursion limit. Có run đã đi tới attempt 31.
>
> Fix thì đơn giản: bỏ việc set verdict ở hai chỗ kia, để chúng chỉ ghi `failure_analysis` và giữ
> verdict PENDING, còn verify là nơi duy nhất quyết RETRY hay ESCALATED — nên nó cũng là nơi duy nhất
> cần biết về flag và về max_attempts.
>
> Nhưng phần tôi tâm đắc là cách chặn tái diễn. Test hành vi thông thường không đủ, vì một người sau
> này thêm một đường code thứ ba thì test cũ vẫn xanh. Nên tôi viết một test **quét toàn bộ source
> tree** tìm mọi chỗ gán `verdict = Verdict.RETRY`, loại trừ đúng file verify, và fail nếu tìm thấy
> bất kỳ chỗ nào khác. Nó biến một quy ước kiến trúc thành một ràng buộc được máy kiểm tra. Nếu ai đó
> — kể cả tôi sáu tháng sau — tái lập lối tắt đó, suite đỏ ngay kèm đường dẫn và số dòng."*

> **EN (short):** *"The lesson I carried out of this: **a feature flag needs exactly one enforcement
> point.** `ENABLE_REPLAN` was checked in the verify node and looked fine — but two other code paths
> set the verdict to RETRY directly, and the router only looks at the verdict; it doesn't know the flag
> exists. So the flag was off and replan still ran, and since those paths didn't check `max_attempts`
> either, it looped to the recursion limit — one run reached attempt 31.
>
> The fix was to make verify the single authority: the other paths now only record
> `failure_analysis` and leave the verdict PENDING. But the part I'm proud of is how I stopped it
> recurring. A behavioural test isn't enough, because someone adding a third path later still leaves
> it green. So I wrote a test that **scans the whole source tree** for any assignment of
> `verdict = Verdict.RETRY`, excludes the one legitimate file, and fails on anything else. It turns an
> architectural convention into a machine-checked constraint. If anyone — including me in six
> months — reintroduces that shortcut, the suite goes red with the file and line number."*

> 💡 **Vì sao đây là câu trả lời Lead:** nó không dừng ở "tôi sửa bug". Nó là *"tôi tìm ra class lỗi,
> hợp nhất quyền quyết định về một chỗ, rồi biến quy ước đó thành ràng buộc máy kiểm tra được để
> người sau không phá được"*. Đó đúng nghĩa scale một quyết định qua tài liệu/tooling thay vì qua
> lời nhắc.

---

## 5. Bản đồ: stack của bạn → từng dòng JD Katalon

| Yêu cầu JD | Bạn có gì (đã verify) | Độ mạnh |
|---|---|---|
| *"Strong proficiency in **Python**"* | 267 file Python 3.12, FastAPI, Pydantic v2, SQLModel/Alembic. 248 commit tự viết | 🟢 Mạnh |
| *"**LLM API integration**"* (nice-to-have) | OpenAI-compatible client, JSON mode, structured output 4 kiểu khác nhau, xử lý `<think>` block của Qwen | 🟢 Mạnh |
| *"**prompt engineering**"* (nice-to-have) | 26 prompt YAML có `prompt_id`/`version`/`owner`; two-stage vs unified prompting theo số trang | 🟢 Mạnh |
| *"**agent-based design patterns**"* (nice-to-have) | **LangGraph 12 node có chu trình, ReAct loop tự viết, replan/self-correction, checkpointer, cross-run lesson store, tool scoping 3 tầng** + bakeoff RFC 43 run | 🟢 **Rất mạnh** — đây là mảng bạn hơn đa số ứng viên Java |
| *"**large-scale data processing**, batch and real-time"* | ⚠️ Đây là **gap**. Pipeline là sync request/response, không queue, không Spark | 🔴 Yếu — xem [§9](#9-điểm-yếu-thật--chuẩn-bị-trước-khi-bị-hỏi) |
| *"**highly available, distributed systems**"* | 🟡 Docker Compose, không K8s. GPU model tách process qua HTTP là điểm cộng nhỏ | 🟡 Trung bình |
| *"**observable** systems"* | structlog JSON, TraceID middleware, Langfuse, `llm_calls_by_agent`, `vote_share` logging | 🟢 Mạnh |
| *"Docker and **Kubernetes**"* | ⚠️ Docker/Compose có; **K8s không có trong repo này** (`chart/` chỉ là diagram) | 🔴 Gap |
| *"**Java**"* | Không thuộc dự án này — dùng [CV-BASED-ANSWERS.md](CV-BASED-ANSWERS.md) §3 (Credit Strong) và [hệ thống thật all-in-one trên AWS](katalon-system-design/05-he-thong-that-allinone-aws.md) | — |
| *"JavaScript/TypeScript + **Playwright**"* | ⚠️ Không có. Vẫn là gap #1 toàn workspace | 🔴 Gap |
| **Multimodal / vision** | VLM đọc techpack, FashionCLIP, Docling layout, self-consistency voting | 🟢 Mạnh — TrueTest cần đúng thứ này |
| **Vector search / RAG** | Qdrant 4 collection, cross-encoder rerank, multivector MAX_SIM | 🟢 Mạnh |

---

## 6. "Làm sao tin được output của LLM?" — 3 cơ chế thật trong code

Ba cơ chế này **map trực tiếp** sang bài toán TrueTest (LLM sinh locator/test case có thể trông đúng
mà chạy sai). Nói được cả ba = bạn đang nói ngôn ngữ của team TrueTest.

### 6.1 Grounding gate — chỉ giữ cái verify được ngược lại nguồn

`vlm/services/document_analysis.py:100-145`:

```python
def _is_grounded_in_text(value: str | None, source_text: str) -> bool:
    normalized_value = _normalize_for_grounding(value)      # bỏ hết ký tự không phải a-z0-9
    if not normalized_value:
        return False
    normalized_source = _normalize_for_grounding(source_text)
    return normalized_value in normalized_source
```

Mã vật liệu VLM đọc ra mà **không xuất hiện trong text gốc của trang** thì bị set `None`. Có
allowlist cứng 2 mã cho trường hợp đặc biệt (`bm22002`, `nxsbb02nologo`).

> **Liên hệ TrueTest:** đây đúng là cách chặn LLM bịa ra một CSS selector nghe hợp lý nhưng không tồn
> tại trong DOM. Nói ra liên hệ này khi trình bày.

### 6.2 Self-consistency voting — chống "sai một cách tự tin"

`vlm/services/document_analysis.py:560-662`. Đây là đoạn code tôi khuyên bạn **in ra mang theo**,
vì docstring của nó thể hiện đúng tư duy Lead:

```python
"""...
The model is deterministic at temperature=0 and can be confidently wrong on tightly-clustered
icons — sampling `num_samples` times at `sample_temperature` and taking the plurality vote gives
it a chance to land on the correct count instead of repeating the same wrong guess. A low
temperature keeps samples close to the model's real read of the image (still varied enough to
catch a one-off misread) instead of injecting enough randomness to flip a narrow plurality.

The plurality winner is trusted even when narrow (e.g. 4 vs 3 votes): the Stage-1 full-page
fallback this recount corrects for has been observed to be *systematically* wrong on tightly
clustered icons (deterministically so, since it runs at temperature 0) — a weak-but-correct
plurality from a zoomed crop still beats that fallback, so no vote-share threshold is applied.
"""
```

Cấu hình: `num_samples=7`, `sample_temperature=0.3`, lấy plurality qua `Counter(votes).most_common(1)`.
`vote_share` được log lại để quan sát mức đồng thuận.

**Ba quyết định thiết kế trong đó, mỗi cái đều có lý do viết ra:**

1. **Vì sao không dùng temperature 0?** Vì temperature 0 khiến model *lặp lại đúng cái sai* — sai một
   cách tất định thì không có cách nào phát hiện bằng cách chạy lại.
2. **Vì sao 0.3 mà không cao hơn?** Đủ để bắt lỗi đọc nhầm một lần, không đủ để lật ngược một plurality
   sát nút.
3. **Vì sao không đặt ngưỡng vote_share?** Vì phương án thay thế (đếm cả trang ở stage 1) đã được quan
   sát là sai *có hệ thống* — thắng sát nút từ ảnh zoom vẫn tốt hơn.

### 6.3 Confidence suy ra từ dữ liệu, không phải LLM tự chấm

`services/provenance.py:150-155`:

```python
def material_confidence_from_candidate_count(count: int) -> ConfidenceType:
    if count <= 1:
        return ConfidenceType.HIGH
    if count <= 4:
        return ConfidenceType.MEDIUM
    return ConfidenceType.LOW
```

#### Câu trả lời gọn khi bị hỏi

> **VI:** *"Tôi không hỏi LLM 'anh tự tin bao nhiêu' — model có xu hướng tự tin đều đặn kể cả khi sai.
> Confidence của tôi suy ra từ tính chất của dữ liệu: nếu tra ra đúng 1 candidate thì không có gì mơ
> hồ, đó là HIGH. Ra 5 candidate nghĩa là bản thân dữ liệu mơ hồ, đó là LOW — bất kể model nói gì."*
>
> **EN:** *"I don't ask the LLM how confident it is — models are uniformly confident, including when
> they're wrong. My confidence is derived from a property of the data: if the lookup returns exactly
> one candidate there's no ambiguity, that's HIGH. Five candidates means the data itself is ambiguous,
> that's LOW — regardless of what the model claims."*

---

## 7. Số đo thật — thứ khiến bạn khác mọi ứng viên AI khác

> Phần lớn ứng viên nói về AI bằng tính từ ("khá chính xác", "hoạt động tốt"). Bạn có **số**. Đây
> là mục đáng giá nhất cả file — nhưng chỉ khi bạn nhớ được vài con số và **bối cảnh** của chúng.

### 7.1 Rulebook — ý tưởng kiến trúc mạnh nhất, và cũng dễ kể nhất

Đây là thứ tôi khuyên bạn đưa lên **sớm** trong vòng technical, vì nó là một quyết định kiến trúc
gọn gàng mà interviewer nào cũng hiểu ngay.

System prompt `agent_build_rulebook/prompt.yml:4-6` nói thẳng mục tiêu:

> *"Your goal is to produce a validated, version-controlled JSON rulebook from a given SRS Excel
> file. **The rulebook drives all downstream BOM resolution agents without requiring further LLM
> calls per style.**"*

**Ý tưởng:** thay vì mỗi lần chạy lại bắt LLM đọc tài liệu nghiệp vụ (SRS) rồi suy luận, bạn dùng
LLM **một lần offline** để dịch tài liệu đó thành **JSON rule có version**, lưu vào Postgres. Runtime
sau đó chỉ đọc rule và thực thi bằng Python thuần — **zero LLM call per style**.

**Và đây là chi tiết tinh tế nhất** (`rule_builder_v2.py:1-5`) — khác biệt giữa v1 và v2:

> *"Unlike BuildRulebookAgent (v1) which extracts reference values and treats them as final answers,
> **V2 reads the QUY TAC (procedure) columns to produce search_strategy entries. `new_item_code` is
> always null — resolved at runtime against actual trimlist.**"*

Rule thật (`item_rules_v2_STANDAR.json`, rule `IR2-STANDAR-002`):

```json
"action": "REPLACE_CODE",
"new_item_code": null,
"search_strategy": {
  "item_code": { "source": "TRIMLIST",
    "procedure": "Find row labeled 'ZROH FRONT' in TrimList and extract General Code (Column B).",
    "search_field": "ZROH FRONT", "extract_field": "general_code" } }
```

#### Cách kể (đây là câu đắt nhất trong file này)

> **VI:** *"Bài học lớn nhất tôi rút ra: **đừng để LLM trả lời, hãy để nó viết ra cách tìm câu trả
> lời.** Phiên bản đầu, tôi cho LLM đọc tài liệu nghiệp vụ rồi trích thẳng ra giá trị — kết quả là
> mỗi lần tài liệu đổi hoặc model đổi thì output đổi, và không ai truy được vì sao ra con số đó.
> Phiên bản hai, LLM không được trả về giá trị nữa: `new_item_code` **luôn** là null. Nó chỉ được trả
> về **chiến lược tìm kiếm** — tìm dòng nào, cột nào, trích field nào. Việc tra cứu thật do code
> Python tất định làm, tại runtime, trên dữ liệu thật.
>
> Đổi lại được ba thứ: kết quả tái lập được, audit được về tận ô Excel gốc, và runtime không tốn
> một token LLM nào cho mỗi style. LLM chuyển từ vai 'người trả lời' sang vai 'người biên dịch tài
> liệu thành code' — và đó là vai nó làm tốt hơn nhiều."*

> **EN:** *"The biggest lesson I took from this: **don't ask the LLM for the answer — ask it to
> write down how to find the answer.** In v1 the model read the business spec and extracted values
> directly, so every time the spec or the model changed, the output changed, and nobody could trace
> why a given number appeared. In v2 the model isn't allowed to return a value at all —
> `new_item_code` is always null. It returns only a **search strategy**: which row, which column,
> which field to extract. The actual lookup is done by deterministic Python at runtime against real
> data.
>
> That bought three things: reproducible results, auditability down to the source spreadsheet cell,
> and zero LLM tokens per style at runtime. The LLM moved from being the answerer to being a
> compiler that turns a spec into rules — which it's much better at."*

> 💡 **Liên hệ TrueTest:** đây chính là lý do TrueTest sinh ra **test case** (artifact bền, review
> được, chạy lại được) chứ không phải để LLM "kiểm thử hộ" theo thời gian thực. Nói được liên hệ
> này cho thấy bạn hiểu vì sao sản phẩm của họ được thiết kế như vậy.

### 7.2 Chất lượng end-to-end — số thật, và trung thực về điểm chưa tốt

**VLTT1 — 32 dòng golden do người kiểm tay, 8 field = 256 ô** (`docs/test-data/vltt1/README.md`):

| Field | Chính xác |
|---|---|
| Item Name, BOM Group | 100.0% |
| SWL, Place To Use | 90.6% |
| Content | 87.5% |
| Supplier | 84.4% |
| Item Code | 81.2% soft / **68.8% hard** |
| C/O | 78.1% |
| **Tổng** | **90.6% soft · 89.1% hard** (232/256 · 228/256) |

**Baseline trước khi có rulebook: dưới 70%** (`BOM_A_Architecture.md:14`). Tức kiến trúc rulebook ở
§7.1 mang lại **~20 điểm phần trăm**.

**Place-To-Use trên 19 style** (`RE-MIDP-PTU-Review-Roadmap.md`, 25/08/2026): exact match **75.8%**
(482/636); nếu tính cả sai lệch chỉ do khoảng trắng/thứ tự thì 78.1%; **sai nội dung thật 21.9%**.

**23 lần chạy thật** (tổng hợp từ `output-bom/*/qa_report.json`): 10 `BLOCKED`, 13
`APPROVED_WITH_WARNINGS`, **0 lần sạch hoàn toàn**. `provenance_coverage_pct` 98.95–100%. Nhưng
**TBA cells 22–27 mỗi run trên 33–37 dòng — tức ~65-70% số dòng vẫn cần người xử lý.**

> ⚠️ **Con số 65-70% TBA này bạn PHẢI chủ động nói ra**, đừng để bị hỏi. Cách nói:
>
> *"Tôi muốn trung thực về mức độ trưởng thành: hiện khoảng 2/3 số dòng vẫn rơi vào TBA và cần người
> xử lý. Nhưng đó là **do thiết kế ở giai đoạn này** — tôi chọn ngưỡng chặt, thà để trống còn hơn
> điền sai, vì output đi vào đơn mua hàng. Giá trị hệ thống mang lại lúc này là ở 1/3 tự động được
> **cộng với** việc mọi ô đều có provenance nên người review biết chính xác cần kiểm gì, thay vì đọc
> lại từ đầu. Lộ trình nới ngưỡng nằm ở việc tăng độ phủ rulebook, không phải ở việc nới lỏng cho
> LLM đoán nhiều hơn."*

Con đường cải thiện đã được lượng hoá sẵn trong roadmap: P0 (chỉ sửa config/format) 75.8% → 82.2%;
thêm 14 rule ở P1 → ~88-90%. **Nói được lộ trình có số là tín hiệu Lead.**

### 7.3 Qwen vs Claude — model tiering thật, và một kết quả bất ngờ

> ⭐ **Mục này giờ là bằng chứng chất lượng cho một câu chuyện lớn hơn.** Hướng tuyển của Katalon đã
> dịch sang **AI Engineer, trọng tâm chuyển cloud LLM sang self-host để giảm chi phí** — tức đúng
> việc bạn đã làm. Phần kinh tế học, serving stack, cái giá thật và cách kể:
> **[katalon-selfhost-llm.md](katalon-selfhost-llm.md)**.

Model tiering là **thật và có chủ đích** (`.env-example:89-103`):

```bash
# ── BOM Agentic Pipeline — Inference LLM (agents 4-9, online) ────────────
# Inference always uses the self-hosted Qwen server. NEVER set LLM_PROVIDER=anthropic here.
AI_MODEL=Qwen3-VL-8B-Instruct
# ── Rule Builder LLM (offline only — import-from-srs, import-fixed-rules) ─
ANTHROPIC_MODEL=claude-sonnet-4-6
```

→ **Model đắt (Claude) chỉ chạy offline một lần để build rulebook; model rẻ tự host (Qwen) chạy
online.** Đây chính là "model tiering" mà câu hỏi cost control hay nhắm tới.

**Kết quả đo được** (từ `run_meta.json` các snapshot, và chạy lại chính harness `eval_rulebook.py`):

| So sánh | Kết quả |
|---|---|
| Số rule trích được từ cùng 1 SRS | Claude **67** PTU / **160** item mapping · Qwen **58** / **105** |
| Qwen đối chiếu golden = Claude (PTU exact) | **90.9%** exact · 93.3% fuzzy |
| Qwen vs Claude trên rulebook **v2** | **100% khớp action (27/27)**, phân bố action giống hệt |
| Biến thể "capped" (giới hạn rule/chunk cho model yếu) | **Làm tệ đi**: 90.0% → 86.2% PTU, 35.1% → 27.0% item |
| Tỉ lệ tự đánh dấu NEEDS_REVIEW | Claude **29.9%** > mọi bản Qwen (8.6–15.5%) |

#### Ba nhận định đáng nói

> **VI (nói gọn):** *"Ba thứ tôi học được khi đo Qwen tự host với Claude:
>
> Thứ nhất, **khoảng cách phụ thuộc vào việc bài toán có cấu trúc tới đâu.** Ở bài trích rule tự do
> từ tài liệu, Claude hơn rõ — nhiều hơn 15% PTU rule và 50% item mapping. Nhưng ở rulebook v2, nơi
> tôi đã ép output vào schema chặt và bài toán chỉ còn là phân loại action, hai model **khớp 100%**
> trên 27 item. Kết luận thực dụng: **đầu tư vào cấu trúc hoá bài toán có ROI cao hơn đầu tư vào
> model mạnh hơn** — và nó còn giúp tôi dùng được model rẻ tự host cho đường online.
>
> Thứ hai, một kết quả phản trực giác: tôi có một biến thể giới hạn số rule mỗi chunk cho model yếu,
> nghĩ là sẽ giúp nó tập trung. Đo ra thì **tệ hơn** — 90% xuống 86%. Cắt nhỏ context làm mất ngữ
> cảnh giữa các rule liên quan. Nếu không đo thì tôi đã giữ tối ưu hoá sai đó.
>
> Thứ ba, Claude tự đánh dấu NEEDS_REVIEW nhiều gấp đôi Qwen. Tôi không đọc đó là điểm trừ — model
> nhận biết được giới hạn của chính nó có giá trị hơn model tự tin đều."*

> **EN (short version):** *"Three things I learned measuring self-hosted Qwen against Claude. First,
> the gap depends on how structured the task is — on free-form rule extraction Claude was clearly
> ahead, but on our v2 rulebook, where I'd forced the output into a tight schema and reduced the task
> to action classification, the two agreed **100%** across 27 items. The practical conclusion:
> **structuring the problem has better ROI than upgrading the model** — and it let me run a cheap
> self-hosted model on the online path. Second, a counter-intuitive result: a variant that capped
> rules per chunk for weaker models actually made things **worse**, 90% down to 86% — chunking too
> small destroyed context between related rules. I'd have kept that wrong optimisation if I hadn't
> measured. Third, Claude flagged NEEDS_REVIEW twice as often as Qwen; I read that as a feature, not
> a defect — a model that knows its own limits is worth more than one that's uniformly confident."*

### 7.4 Bằng chứng tái lập được (reproducibility)

- `llm_temperature = 0.0` ghim trong `core/config.py:73`.
- **17 lần chạy** trong `v2_llm_eval/` cho phân bố action **giống hệt nhau**: 13 run ở n=70 đều ra
  `{KEEP:45, REPLACE_CODE:10, CONDITIONAL:8, REMOVE:7}`.
- Rulebook có version + snapshot theo timestamp + `run_meta.json` đánh dấu bản `active`.
- `validate_rulebook.py` là **cổng chặn trước khi deploy**, `sys.exit(1)` nếu fail: kiểm tra schema,
  độ phủ 7 item bắt buộc, trùng `rule_id`, `0.0 ≤ confidence ≤ 1.0`, và **từ chối rulebook thiếu
  provenance**.

> **Câu trả lời cho "làm sao anh biết thay đổi prompt không làm hỏng thứ khác?"**
>
> *"Có golden snapshot theo version và một harness đo lại: exact match, fuzzy similarity, tỉ lệ
> NEEDS_REVIEW, và số chunk fail — kèm ngưỡng pass/fail. Cộng thêm một validator chạy trước deploy,
> exit code khác 0 nếu rulebook sai schema hoặc thiếu provenance. Nên đổi prompt là chạy lại được
> và so được, không phải đổi rồi hy vọng."*

### 7.5 Kiểm soát chi phí — có, bằng cap chứ không bằng cache

Bảng cap thật trong `core/config.py`:

| Setting | Mặc định | Ý nghĩa |
|---|---|---|
| `max_rule_rows_for_llm` | 120 | trần tuyệt đối số dòng SRS gửi cho LLM |
| `srs_llm_chunk_char_budget` | 3000 | ngân sách ký tự mỗi lời gọi |
| `material_tool_max_iterations` | 2 (`le=4`) | ngân sách số lần gọi tool mỗi dòng |
| `llm_qa_chunk_size` / `llm_qa_max_anomalies` | 30 / 50 | trần dòng mỗi call QA / tổng anomaly |
| `llm_qa_execution_timeout` | 600s | trần wall-clock cả pha |
| `enable_qa_llm` | **False** | QA LLM tắt mặc định → chi phí bằng 0 |

**Trung thực về điểm thiếu:** không có prompt caching, không có semantic cache, không tính chi phí
theo đô-la trong repo (giao cho Langfuse), và `llm_tokens_total` **được khai báo và trả về API nhưng
không có chỗ nào tăng nó** — tức luôn bằng 0. Nếu bị hỏi sâu về cost, nói thẳng:

> *"Kiểm soát chi phí hiện nay là bằng cap cứng — trần số dòng, ngân sách ký tự mỗi chunk, trần số
> lần gọi tool, và tắt hẳn pha QA LLM theo mặc định. Cái tôi chưa làm là caching và tính cost theo
> đô-la trong hệ thống; hiện đang dựa vào Langfuse. Có một bug tôi biết: `llm_tokens_total` được trả
> về API nhưng chưa bao giờ được cộng, nên nó luôn là 0 — số call thì đúng, số token thì không."*

> 💡 Thừa nhận một bug cụ thể mình biết mà chưa sửa **tăng** độ tin cậy — nó chứng minh bạn thật sự
> đọc code chứ không mô tả theo trí nhớ.

---

## 8. RAG & retrieval — nói đúng, đừng nói quá

### Sự thật

| Câu hỏi | Sự thật đã verify |
|---|---|
| Có hybrid search không? | **Không.** Dense-only. Không BM25, không sparse vector, không Postgres full-text |
| Reranker là gì? | **Cross-encoder `BAAI/bge-reranker-base`** qua TEI (không phải LLM judge) |
| Vector DB | Qdrant `v1.17.1`, 4 collection, **COSINE toàn bộ** |
| pgvector? | **Không có.** Chỉ Qdrant |
| Có phải RAG không? | **Một phần** — chỉ Team A (retrieval nạp candidate cho LLM chọn). Team B/C là retrieval + so sánh, không phải RAG |

**Collection và chiều vector** (`qdrant/qdrant_setup.py:47-67`):

| Collection | Kiểu | Dim | Model |
|---|---|---|---|
| `master_materials` | vector đơn | 128 | bge-m3 |
| `techpack_sketches` | vector đơn | 512 | Marqo/marqo-fashionCLIP |
| `techpack_history`, `bom_history` | **multivector MAX_SIM** (kiểu ColBERT late-interaction) | 128 | bge-m3 |

### 8.1 Nền tảng: vector → dot product → cosine

Ba khái niệm này xếp thành một chuỗi: **vector** là thứ được so, **dot product** là phép toán, **cosine**
là dot product đã chuẩn hoá. Hiểu theo thứ tự đó thì không cần học thuộc công thức nào.

#### (a) Vector là gì — trong ngữ cảnh embedding

Về mặt toán, vector chỉ là **một danh sách số có thứ tự**: `[0.12, −0.44, 0.08, …]`. Nhưng ý nghĩa
nằm ở cách hiểu nó:

- **Là một điểm** trong không gian `d` chiều. Vector 128 chiều = một điểm trong không gian 128 chiều.
- **Là một mũi tên** từ gốc toạ độ tới điểm đó — có **hướng** và có **độ dài** (norm).

**Embedding là một hàm** biến dữ liệu phi cấu trúc thành vector, sao cho **khoảng cách hình học phản
ánh sự tương đồng ngữ nghĩa**:

```text
"LINING"       → [0.21, -0.08, 0.44, ...]  ┐ gần nhau (cùng là vải lót)
"LINING FABRIC"→ [0.19, -0.11, 0.41, ...]  ┘
"BUTTON"       → [-0.52, 0.33, -0.07, ...]   xa hẳn
```

Điểm cốt lõi để nói trong phỏng vấn: **từng chiều riêng lẻ không có ý nghĩa người đọc được.** Chiều
thứ 47 không phải "độ dài áo". Ý nghĩa nằm ở **quan hệ giữa các vector**, không nằm ở giá trị từng
chiều. Đó là lý do bạn không thể debug embedding bằng cách đọc số — chỉ đo được bằng cách so cặp.

Hai đại lượng của một vector, và chỉ một trong hai thường mang ngữ nghĩa:

| | Là gì | Có mang ngữ nghĩa? |
|---|---|---|
| **Hướng** | Vector chỉ về phía nào | ✅ **Có** — đây là phần chứa "nghĩa" |
| **Độ dài** (`‖v‖ = √Σvᵢ²`) | Mũi tên dài bao nhiêu | ⚠️ Thường **không** — hay bị chi phối bởi độ dài text, độ đậm của ảnh… |

→ Và chính điều này dẫn tới việc chọn cosine thay vì Euclidean.

#### (b) Dot product (tích vô hướng) là gì

**Định nghĩa đại số** — nhân từng cặp phần tử rồi cộng lại:

```text
A · B = a₁b₁ + a₂b₂ + … + a_d b_d = Σ aᵢbᵢ
```

Ví dụ cụ thể: `A = [1, 2, 3]`, `B = [4, 5, 6]` → `A·B = 1×4 + 2×5 + 3×6 = 4 + 10 + 18 = 32`.

**Định nghĩa hình học** — và đây mới là chỗ hiểu được nó *nghĩa là gì*:

```text
A · B = ‖A‖ × ‖B‖ × cos(θ)
                              θ = góc giữa A và B
```

Đọc câu này ngược lại: dot product = **độ dài A × độ dài B × mức độ cùng hướng**. Nó trộn **ba** thứ
vào một số duy nhất — hai độ dài và một góc. Trực giác: dot product đo *"B đi được bao xa theo hướng
của A"*.

```text
A·B > 0  → góc nhọn, cùng chiều nói chung
A·B = 0  → vuông góc (trực giao — trong embedding nghĩa là "không liên quan")
A·B < 0  → góc tù, ngược chiều
```

**Vấn đề của dot product khi dùng làm similarity:** vì nó nhân cả độ dài vào, một vector **dài** sẽ ăn
điểm cao với mọi thứ, chỉ vì nó dài. Nếu độ dài không mang ngữ nghĩa (thường là vậy), đó là nhiễu.

#### (c) Cosine similarity = dot product đã bỏ độ dài đi

Muốn giữ **góc** mà bỏ **độ dài**, chỉ cần chia lại cho hai norm — đúng theo công thức hình học ở trên:

```text
              A · B            Σ aᵢbᵢ
cos(A,B) = ───────────── = ─────────────────────
            ‖A‖ · ‖B‖      √Σaᵢ² · √Σbᵢ²

  = +1  → cùng hướng hoàn toàn (nghĩa giống nhau)
  =  0  → vuông góc (không liên quan)
  = −1  → ngược hướng
```

Nên câu trả lời gọn nhất cho *"cosine khác dot product ở đâu"* là: **cosine chính là dot product của
hai vector sau khi đã chuẩn hoá về độ dài 1.** Không phải hai phép toán khác nhau — cùng một phép,
khác ở chỗ có bỏ độ dài hay không.

**Tính chất quyết định: bất biến với độ dài (scale-invariant).** `cos(A, B) = cos(A, 2B) = cos(A, 100B)`.
Đây là lý do dùng nó cho embedding: độ dài vector thường phản ánh những thứ bạn **không** muốn so —
text dài hơn, ảnh nhiều chi tiết hơn — còn *nội dung ngữ nghĩa* nằm ở hướng.

> ⚠️ **Bẫy thuật ngữ hay bị hỏi:** *cosine **similarity*** (càng cao càng giống, khoảng `[−1, 1]`)
> khác *cosine **distance*** (`1 − cosine similarity`, càng thấp càng giống, khoảng `[0, 2]`).
> Qdrant dùng chữ `Distance.COSINE` nhưng **trả về score kiểu similarity** — càng cao càng giống. Nếu
> bạn sort sai chiều thì lấy ra đúng những candidate tệ nhất. Biết chi tiết này cho thấy bạn đã thật
> sự dùng, không phải chỉ đọc.

> 💡 **Điểm đáng nói nhất:** nếu vector đã được **L2-normalize** (‖v‖ = 1) thì **cosine ≡ dot
> product**, và cosine cũng **đơn điệu ngược** với Euclidean (`‖A−B‖² = 2 − 2·cos(A,B)` khi cả hai đã
> chuẩn hoá). Nghĩa là với vector đã chuẩn hoá, **cả ba metric xếp hạng giống hệt nhau** — chọn cái
> nào chỉ còn là chuyện tốc độ. Qdrant tận dụng đúng điều này: với `Distance.COSINE` nó **tự normalize
> lúc upsert**, rồi query chỉ cần tính dot product — phép rẻ hơn và tối ưu SIMD được.

**Trong hệ của bạn: `Distance.COSINE` ở toàn bộ 4 collection** (`qdrant/qdrant_setup.py:14-28`) —
đúng và nhất quán, không có gì phải bào chữa.

#### Câu trả lời gọn

> **VI:** *"Cosine đo góc giữa hai vector, không đo khoảng cách. Điểm quan trọng là nó bất biến với
> độ dài — nên hai đoạn text cùng nghĩa mà một dài một ngắn vẫn ra điểm cao, vì ngữ nghĩa nằm ở
> hướng chứ không ở độ lớn. Toàn bộ collection của tôi dùng cosine. Và một chi tiết hay: khi vector
> đã L2-normalize thì cosine và dot product là một — Qdrant chuẩn hoá lúc upsert đúng vì vậy, để
> runtime chỉ phải tính dot product."*

> **EN:** *"Cosine measures the angle between two vectors, not the distance. The key property is
> scale-invariance — two texts with the same meaning but different lengths still score high, because
> meaning lives in the direction, not the magnitude. All our collections use cosine. And a nice
> detail: once vectors are L2-normalized, cosine and dot product are equivalent — which is exactly
> why Qdrant normalizes at upsert time, so at query time it only needs a dot product."*

---

### 8.2 Qdrant có đúng 4 loại distance — và **không có** loại nào là default

> ⚠️ **Sửa một ngộ nhận trước đã:** `Distance.COSINE` **không phải default của Qdrant**. Qdrant
> **không có default** — `distance` là **tham số bắt buộc**. Xác minh trong chính client bạn đang
> dùng (`qdrant_client/http/models/models.py:3699`):
>
> ```python
> class VectorParams(BaseModel, extra="forbid"):
>     size: int = Field(..., description="Size of a vectors used")
>     distance: "Distance" = Field(..., description="...")   # ← "..." = REQUIRED
> ```
>
> Nên COSINE là **lựa chọn của bạn** trong `qdrant_setup.py`, không phải thứ Qdrant tự áp. Trong
> phỏng vấn, nói *"tôi chọn cosine vì…"* mạnh hơn nhiều so với *"đó là default"* — câu sau nghe như
> bạn chưa từng cân nhắc.

**Toàn bộ enum** (`qdrant_client/http/models/models.py:774-785` — đúng 4 giá trị, không hơn):

```python
class Distance(str, Enum):
    COSINE    = "Cosine"
    EUCLID    = "Euclid"
    DOT       = "Dot"
    MANHATTAN = "Manhattan"
```

| Distance | Công thức | Bị ảnh hưởng bởi độ dài? | Cao = giống hay thấp = giống? | Dùng khi |
|---|---|:---:|---|---|
| **`COSINE`** | `A·B / (‖A‖‖B‖)` | ❌ Không | **Cao = giống** | Embedding ngữ nghĩa — **mặc định đúng** cho hầu hết trường hợp |
| **`DOT`** | `Σ aᵢbᵢ` | ✅ Có | **Cao = giống** | Vector **đã tự normalize sẵn** (tiết kiệm 1 bước), hoặc recommender nơi norm mã hoá độ phổ biến |
| **`EUCLID`** (L2) | `√Σ(aᵢ−bᵢ)²` | ✅ Có | **Thấp = giống** | Toạ độ vật lý, cluster trong không gian thật |
| **`MANHATTAN`** (L1) | `Σ\|aᵢ−bᵢ\|` | ✅ Có | **Thấp = giống** | Dữ liệu thưa/nhiều chiều; ít nhạy với outlier hơn L2 |

**Ngoài `Distance`, Qdrant còn một trục cấu hình thứ hai** — `MultiVectorComparator`, và nó chỉ có
**một** giá trị (`models.py:1813-1815`):

```python
class MultiVectorComparator(str, Enum):
    MAX_SIM = "max_sim"
```

Đây là thứ bạn đang dùng cho `techpack_history`/`bom_history` — late interaction kiểu ColBERT, xem
[§8.3](#83-embedding-strategy--ba-cấu-hình-khác-nhau-trong-cùng-một-hệ).

#### Chọn thế nào — cây quyết định gọn

```text
Vector đến từ embedding model ngữ nghĩa (text/ảnh)?
├─ Có → độ dài vector có mang thông tin bạn MUỐN so không?
│        ├─ Không (thường là vậy) → COSINE          ← 95% trường hợp
│        └─ Có (norm = độ phổ biến/tin cậy) → DOT
└─ Không, là toạ độ/đo lường vật lý thật → EUCLID (hoặc MANHATTAN nếu nhiều outlier)

Đã tự L2-normalize vector trước khi upsert rồi? → DOT (bỏ được phép chia dư thừa)
```

> **Câu trả lời khi bị hỏi "vì sao cosine mà không phải 3 cái kia":**
>
> **VI:** *"Vì trong embedding ngữ nghĩa, độ dài vector là nhiễu chứ không phải tín hiệu — nó bị chi
> phối bởi độ dài text hay độ chi tiết của ảnh, những thứ tôi không muốn so. Cosine bỏ độ dài đi, chỉ
> giữ hướng. Euclid và Manhattan thì có tính độ dài, nên hai text cùng nghĩa mà khác độ dài sẽ bị
> phạt oan. Dot product thì tôi sẽ chọn nếu tôi đã tự chuẩn hoá vector trước khi upsert — lúc đó dot
> và cosine tương đương về xếp hạng, và dot rẻ hơn một phép chia. Nhưng vì tôi để Qdrant tự chuẩn hoá
> qua COSINE nên khác biệt đó không đáng kể, và COSINE thì tường minh hơn cho người đọc code sau này."*

---

### 8.3 Embedding strategy — ba cấu hình khác nhau trong cùng một hệ

Đây là chỗ bạn có thể nói sâu, vì hệ của bạn dùng **ba chiến lược khác nhau cho ba bài toán khác
nhau** (`qdrant_setup.py:47-67`):

| Collection | Vector | Dim | Model | Vì sao chiến lược đó |
|---|---|---|---|---|
| `master_materials` | đơn | 128 | `BAAI/bge-m3` | Match tên vật liệu — 1 vector/record là đủ |
| `techpack_sketches` | đơn | **512** | `Marqo/marqo-fashionCLIP` | 1 ảnh sketch = 1 vector |
| `techpack_history`, `bom_history` | **multivector `MAX_SIM`** | 128 | `BAAI/bge-m3` | Late interaction kiểu ColBERT |

**Multivector `MAX_SIM` là gì và vì sao khác hẳn:** thay vì nén cả tài liệu thành **một** vector,
ColBERT-style giữ **một vector cho mỗi token/patch**, rồi điểm của cặp (query, doc) =
`Σ_over_query_tokens max_over_doc_tokens cos(qᵢ, dⱼ)` — mỗi token query tìm token doc khớp nhất với
nó, rồi cộng lại.

| | Single vector (dense pooling) | Multivector MAX_SIM (late interaction) |
|---|---|---|
| Lưu trữ | 1 vector/doc | N vector/doc → **nặng hơn nhiều** |
| Chi tiết cục bộ | Bị nén mất | **Giữ được** — một cụm từ khớp vẫn ăn điểm |
| Tốc độ | Nhanh | Chậm hơn, tốn RAM |
| Phù hợp | Match ngắn, rõ | Tài liệu dài, cần khớp một phần |

> **Câu trả lời khi bị hỏi "vì sao `techpack_history` dùng multivector mà `master_materials` thì
> không":** *"Bài toán khác nhau. Match tên vật liệu là so hai chuỗi ngắn — pooling thành một vector
> không mất gì đáng kể, và tôi được tốc độ với bộ nhớ. Còn techpack lịch sử là tài liệu dài: cái tôi
> cần là 'có một đoạn trong tài liệu này khớp với truy vấn của tôi', mà pooling cả tài liệu thành một
> vector thì đúng cái tín hiệu đó bị pha loãng. Late interaction giữ được vector từng token nên một
> đoạn khớp mạnh vẫn nổi lên. Đánh đổi là lưu trữ và RAM tăng theo số token — nên tôi chỉ dùng ở
> collection thật sự cần."*

---

### 8.4 Thực tiễn 2026 — cái gì đang phổ biến, và bạn đang đứng ở đâu

> **Nguồn:** tra web ngày **26/08/2026** (vượt mốc kiến thức nội tại của tôi nên phải tra, không đoán).
> Link ở cuối mục. Số liệu benchmark là của bên thứ ba, **không** phải đo trên dữ liệu của bạn.

#### (a) Metric — cosine vẫn là mặc định của ngành

Không có chuyển dịch nào rời khỏi cosine cho embedding ngữ nghĩa. Điều **đã** thay đổi từ 2024-2025 là
mọi thứ **quanh** metric: chiều vector, quantization, và kiến trúc hai tầng. Nên lựa chọn COSINE của
bạn không phải nợ kỹ thuật.

#### (b) Chiều vector — 1024 là "sweet spot", và có số cụ thể

| Chiều | Thực tiễn 2026 |
|---|---|
| **1024** | **Sweet spot** cho phần lớn corpus. Từ 3072 → 1024 mất khoảng **0.03 Recall@10** nhưng giảm ~3× lưu trữ |
| 512 | Recall bắt đầu tụt rõ, nhất là với query nhập nhằng |
| **256 + scalar int8** | Được khuyến nghị là **cấu hình cân bằng nhất** cho hệ quy mô lớn |
| 128 | Mất mát lớn — xem bảng dưới |

**Và đây là con số quan trọng nhất cho hoàn cảnh của bạn** (benchmark HotpotQA 100k doc):

| Cấu hình | Recall@10 mất | MRR@10 mất |
|---|---|---|
| 384d + scalar int8 | 1.46% | 2.72% |
| 256d + scalar int8 | 4.6% | 4.4% |
| **128d + scalar int8** | **14.8%** | **17.5%** |
| Binary quant @ 384d | 18.7% | 20.7% |

> ⚠️ **Đọc kỹ dòng 128d:** mất **~15% Recall@10** — và đó là **với model có MRL**. Hệ của bạn cắt một
> model **không** có MRL xuống 128 chiều, nên về lý thuyết còn **tệ hơn** con số này, vì không có gì
> đảm bảo 128 chiều đầu bảo toàn thứ tự khoảng cách. Đây là bằng chứng bên ngoài cho phần nợ kỹ thuật
> ở [§8.5](#85--128-dim-vs-512-dim--và-nợ-kỹ-thuật-thật-cần-thừa-nhận).

#### (c) Matryoshka (MRL) — cách ĐÚNG để giảm chiều

**MRL (Matryoshka Representation Learning)** là kỹ thuật train sao cho **thông tin quan trọng nhất được
dồn về các chiều đầu**. Nhờ đó cắt `3072 → 1024 → 512 → 256` vẫn giữ được phần lớn recall — giống búp
bê Matryoshka lồng nhau, mỗi lớp là một embedding hoàn chỉnh.

**Ràng buộc then chốt, và nó xác nhận đúng điều tôi viết ở §8.5:**

> *"Model **phải** được train với MRL — bạn không thể cắt một embedding model thường và mong nó suy
> giảm êm ái."*

Model hỗ trợ MRL 2026: OpenAI `text-embedding-3`, Voyage (2048/1024/512/256 kèm fp32/int8/binary),
Nomic. **`bge-m3` không nằm trong nhóm này.**

#### (d) Quantization — trục tiết kiệm mà bạn chưa dùng

| Kỹ thuật | Tiết kiệm | Đánh đổi |
|---|---|---|
| **Scalar (int8)** | **4×** | Rất nhỏ — được coi là "gần như miễn phí" |
| **Binary (1-bit)** | **32×** | Mất mát lớn (~18.7% recall @384d) — **chỉ dùng khi có rerank phía sau** |
| MRL 1024→256 + int8 | ~70.8% giảm | Recall −4.6% |
| MRL 1024→128 + int8 | ~77.9% giảm | Recall −14.8% |

**Kiến trúc production được khuyến nghị 2026:** index trong RAM dùng **MRL + binary/int8** (nhanh,
nhỏ), rồi **rerank bằng embedding fp32 đầy đủ** đọc từ disk hoặc tầng chậm hơn. Tức là quantization
và rerank là **hai mặt của cùng một thiết kế** — bạn dám nén mạnh ở tầng 1 *chính vì* có tầng 2 sửa lại.

> 💡 **Đây là câu trả lời rất mạnh nếu được hỏi "anh sẽ tối ưu chi phí vector search thế nào":**
> *"Tôi sẽ không giảm chiều bằng cách cắt như hiện tại. Hướng đúng là hai trục riêng biệt: đổi sang
> model có MRL rồi cắt theo đúng thiết kế của nó, và bật scalar int8 quantization — cái này cho 4×
> tiết kiệm với mất mát gần như không đáng kể, rẻ hơn nhiều so với việc giảm chiều. Binary
> quantization thì chỉ hợp lý khi đã có tầng rerank chắc chắn phía sau, vì nó mất gần 19% recall."*

#### (e) Model đang dẫn đầu 2026

- **Text embedding:** `Qwen3-Embedding-8B` dẫn MTEB Multilingual (~70.58). Bảng xếp hạng **dịch chuyển
  mỗi 3-4 tháng**, mỗi thế hệ hơn thế hệ trước 3-5% về retrieval → **đừng cưới một model**, hãy giữ
  eval harness để đổi được.
- **Reranker:** `bge-reranker-v2-m3` là **default self-hosted** phổ biến nhất (Apache 2.0);
  họ `Qwen3-Reranker` (0.6B/4B/8B) chấm điểm bằng xác suất sinh "yes" vs "no"; Cohere Rerank 3.5 là
  đường nhanh nhất nếu chấp nhận SaaS.
- **Multimodal:** có `Qwen3-VL-Embedding` và `Qwen3-VL-Reranker` — framework hợp nhất cho retrieval +
  ranking đa phương thức. **Rất đáng lưu ý với bạn**, vì bạn đã chạy Qwen VLM sẵn và đang phải tự ghép
  FashionCLIP + VLM rerank bằng tay.

#### (f) Bạn đang đứng ở đâu — bảng tự chấm

| Trục | Hệ của bạn | Thực tiễn 2026 | Đánh giá |
|---|---|---|---|
| Metric | `COSINE` toàn bộ | Cosine vẫn là mặc định | ✅ Đúng |
| Kiến trúc 2 tầng | bi-encoder → cross-encoder / VLM rerank | Chuẩn công nghiệp | ✅ Đúng |
| Reranker | `bge-reranker-base` | `bge-reranker-v2-m3` là default mới | 🟡 Nên nâng — cùng họ, đổi rẻ |
| Late interaction | Có (`MAX_SIM`) ở 2 collection | Được coi là điểm giữa tốt | ✅ Đi trước nhiều nơi |
| Chiều text | **128, cắt từ 1024, model không MRL** | 1024 sweet spot; cắt chỉ khi có MRL | 🔴 **Nợ kỹ thuật** |
| Quantization | **Không dùng** | int8 gần như miễn phí (4×) | 🟡 Cơ hội bỏ ngỏ |
| Hybrid (dense + sparse) | Không có | Vẫn phổ biến cho keyword-heavy | 🟡 Tuỳ bài toán |

> **Câu trả lời tổng khi được hỏi "anh theo dõi lĩnh vực này thế nào":**
>
> **VI:** *"Kiến trúc của tôi khớp với thực tiễn hiện tại ở hai điểm quan trọng nhất: cosine cho
> embedding ngữ nghĩa, và hai tầng bi-encoder → cross-encoder. Chỗ tôi biết mình lệch là chiều vector:
> chuẩn hiện tại là quanh 1024, và nếu muốn nhỏ hơn thì phải dùng model có Matryoshka chứ không phải
> cắt tuỳ ý như tôi đang làm — benchmark công khai cho thấy 128 chiều mất khoảng 15% Recall@10 ngay cả
> khi model CÓ hỗ trợ MRL. Và trục tôi chưa khai thác là quantization: scalar int8 cho 4× tiết kiệm với
> mất mát gần như bằng không, đó là thứ đáng làm trước cả việc giảm chiều. Tôi cũng để ý là bảng xếp
> hạng embedding đổi mỗi 3-4 tháng, nên tôi coi việc giữ eval harness quan trọng hơn việc chọn đúng
> model hôm nay."*

**Nguồn:** [Scaling Vector Search — Quantization vs Matryoshka](https://towardsdatascience.com/649627-2/) ·
[Matryoshka Embeddings (MongoDB/Voyage)](https://www.mongodb.com/company/blog/technical/matryoshka-embeddings-smarter-embeddings-with-voyage-ai) ·
[Best Embedding Models for RAG 2026 (MTEB)](https://www.premai.io/blog/best-embedding-models-for-rag-2026-ranked-by-mteb-score-cost-and-self-hosting/) ·
[Best Rerankers for RAG 2026](https://futureagi.com/blog/best-rerankers-for-rag-2026/) ·
[Top Reranking Models (Redis)](https://redis.io/blog/top-reranking-models-rag-accuracy/) ·
[Qwen3-VL-Embedding & Reranker](https://arxiv.org/pdf/2601.04720)

---

### 8.5 ⭐ 128 dim vs 512 dim — và nợ kỹ thuật thật cần thừa nhận

#### Chiều vector đánh đổi cái gì

| | Ít chiều (128) | Nhiều chiều (512, 1024) |
|---|---|---|
| Sức biểu đạt | Thấp — dễ **nhồi** hai khái niệm khác nhau vào vùng gần nhau | Cao — phân biệt được sắc thái |
| Bộ nhớ / tốc độ | Rẻ. 128 float32 = 512 B/vector | 512-dim = 2 KB, **4×** |
| Nguy cơ | Under-fitting biểu diễn | Curse of dimensionality; cần nhiều data hơn để index tốt |

**512 chiều cho sketch là hợp lý** — đó là **chiều gốc** của `Marqo/marqo-fashionCLIP` (OpenCLIP
ViT-B-16). Không cắt, không pad, dùng đúng như model xuất ra. Không có gì phải bàn.

#### Còn 128 chiều thì có vấn đề thật ⚠️

`BAAI/bge-m3` xuất **1024 chiều**. Nhưng `EMBEDDING_DIM=128`, và code làm thế này
(`embedding_service.py:30-38`):

```python
def _normalize_vector_dim(self, vector: list[float]) -> list[float]:
    expected_dim = int(self._settings.effective_embedding_dim)
    ...
    if len(vector) > expected_dim:
        return vector[:expected_dim]        # ← cắt lấy 128 chiều ĐẦU của 1024
    return vector + [0.0] * (expected_dim - len(vector))
```

→ **Bỏ đi 87.5% vector.** Và đây là điểm kỹ thuật quan trọng nhất trong cả mục này:

> **Cắt chiều chỉ hợp lệ nếu model được train theo Matryoshka (MRL).** Các model như
> `text-embedding-3` của OpenAI hay Nomic được train sao cho **k chiều đầu tự thân là một embedding
> hợp lệ** — cắt xuống là giảm chiều có kiểm soát. `bge-m3` **không** thuộc loại đó: không có gì đảm
> bảo 128 chiều đầu bảo toàn được thứ tự khoảng cách. Nên đây **không phải "giảm chiều"** — đây là
> **bỏ phần lớn vector và hy vọng**.

**Vì sao con số 128 tồn tại:** nó được chọn từ thời định dùng ColQwen multivector (default model name
trong `vector_config.py` vẫn còn là `tomoro-ai-colqwen3-embed-4b-awq`), và **không được chỉnh lại**
khi deployment chuyển sang `bge-m3`. Đây là nợ kỹ thuật do quán tính cấu hình, không phải quyết định
thiết kế.

> ⚠️ **Đừng hợp lý hoá chỗ này.** Cách trả lời đúng:
>
> **VI:** *"Đây là nợ kỹ thuật thật và tôi biết. bge-m3 xuất 1024 chiều nhưng config của chúng tôi là
> 128, và code cắt lấy 128 chiều đầu — tức bỏ gần 90% vector. Cắt chiều như vậy chỉ hợp lệ với model
> train theo Matryoshka, nơi k chiều đầu được thiết kế để tự thân là embedding hợp lệ; bge-m3 không
> phải loại đó, nên không có gì đảm bảo thứ tự khoảng cách được bảo toàn. Nguồn gốc là con số 128
> được chọn từ thời tính dùng một model multivector khác và không ai chỉnh lại sau khi đổi sang
> bge-m3 — quán tính cấu hình. Cách sửa đúng là để nguyên 1024, hoặc nếu thật sự cần nhỏ hơn thì
> dùng model có hỗ trợ MRL, chứ không phải cắt tuỳ ý."*
>
> **EN:** *"That's real technical debt and I know about it. bge-m3 emits 1024 dimensions but our
> config is 128, and the code truncates to the first 128 — discarding nearly 90% of the vector.
> Truncating like that is only valid for Matryoshka-trained models, where the first k dimensions are
> designed to be a valid standalone embedding; bge-m3 isn't one, so there's no guarantee the distance
> ordering survives. The origin is that 128 was picked when we planned to use a different multivector
> model, and nobody revisited it after switching to bge-m3 — config inertia. The right fix is to keep
> 1024, or if we genuinely need smaller, use a model that supports MRL rather than truncating
> arbitrarily."*

> 💡 Một chi tiết phụ bạn có thể nói thêm nếu muốn ghi điểm: cắt vector **phá vỡ L2-normalization**
> (norm của 128 chiều đầu < 1). Nhưng với `Distance.COSINE` thì Qdrant tự normalize lại lúc upsert,
> nên **cosine không bị sai** — vấn đề nằm ở **mất thông tin**, không nằm ở metric. Phân biệt được
> hai chuyện đó cho thấy bạn hiểu cả tầng toán lẫn tầng vận hành.

---

### 8.6 ⭐ Vì sao vẫn cần rerank — và bằng chứng thật từ chính hệ của bạn

#### Lý do gốc: bi-encoder vs cross-encoder

Đây là câu trả lời cốt lõi, và nó không phải chuyện "để chính xác hơn" mà là **giới hạn cấu trúc**:

| | **Bi-encoder** (tầng embedding) | **Cross-encoder** (tầng rerank) |
|---|---|---|
| Cách chạy | Encode query và doc **độc lập**, so cosine | Đưa **cặp (query, doc) vào cùng một lần forward** |
| Doc được encode khi nào | **Trước, lúc ingest** — không biết query | **Lúc query** — thấy cả hai cùng lúc |
| Có attention chéo query↔doc? | ❌ Không bao giờ | ✅ Có — đây là toàn bộ điểm khác biệt |
| Chi phí | Rẻ, index được ANN | Đắt: **N lần forward cho N candidate** |
| Scale được không | Hàng triệu doc | Chỉ vài chục candidate |

**Điểm mấu chốt:** khi bi-encoder nén một document thành một vector, nó buộc phải nén **mà không biết
sẽ bị hỏi gì**. Một vector phải "trả lời được mọi câu hỏi tương lai" → nó tối ưu cho cái *chung*, và
làm mờ cái *cụ thể*. Cross-encoder không có ràng buộc đó: nó đọc query và doc cùng lúc nên token
trong query có thể attend trực tiếp vào token trong doc.

→ Vì thế kiến trúc chuẩn là **hai tầng**: bi-encoder lọc từ hàng triệu xuống top-K (recall cao, giá
rẻ), cross-encoder xếp lại top-K đó (precision cao, giá đắt nhưng chỉ trên K phần tử).

**Trong hệ của bạn có đủ cả hai loại rerank:**

| Tầng | Cơ chế | Model |
|---|---|---|
| Text | Cross-encoder qua TEI | **`BAAI/bge-reranker-base`** (port 8613) |
| Ảnh | **VLM so 2 ảnh cạnh nhau** | `qwen3.6-27b-q6-vlm` — chấm silhouette / construction / detail |

#### Bằng chứng thật: rerank cứu đúng candidate mà embedding loại oan

Đây là ví dụ đắt nhất — nó **chứng minh** lý thuyết trên bằng số của chính bạn. Với 4 style cùng
nhóm `jacket/kd`, xếp hạng **embedding-only**:

```text
g60362  0.907   ← embedding cho là giống nhất
v85654  0.903
v32843  0.876   ← nhưng ĐÂY mới là style đúng về cấu trúc
v77556  0.857
```

Sau khi qua **VLM rerank**, `v32843` **thắng với `final_score = 0.912`**, nhờ
`construction_similarity` 0.90–0.95 so với 0.3–0.5 của hai style kia — **dù embedding của nó thấp
nhất trong nhóm được xét**.

**Vì sao embedding sai:** `marqo-fashionCLIP` được train trên **ảnh chụp sản phẩm thương mại**, không
phải **sketch kỹ thuật line-art**. Nó nhạy với silhouette, màu, pattern tổng thể — chứ không phân
biệt tốt chi tiết cấu trúc cục bộ như kiểu ve áo hay kiểu túi. Mà cấu trúc mới là thứ quyết định BOM
giống nhau.

Công thức fusion cuối (`similar_techpack_scorer.py`) cho thấy bạn **không** bỏ embedding, chỉ hạ nó
xuống một phiếu trong bốn:

```text
final_score = 0.40 × embedding
            + 0.25 × silhouette
            + 0.25 × construction
            + 0.10 × detail
```

#### Bài học đắt nhất — câu này nên thuộc lòng

> **VI:** *"Bài học là: **embedding chỉ quyết định ai được vào vòng xét, reranker mới quyết định ai
> đúng.** Và nó có một hệ quả vận hành rất cụ thể mà tôi học được theo cách đau: nếu cắt `top_k` quá
> sớm thì reranker giỏi đến mấy cũng vô nghĩa, vì candidate đúng đã bị loại **trước khi** nó được
> nhìn. Chúng tôi từng hardcode `top_k=2` thay vì đọc từ config, và style đúng — chỉ khác độ rộng ve
> áo — nằm ở hạng 3 theo embedding nên không bao giờ tới được tầng rerank. Sửa để đọc config thành 3
> thì nó vào được và thắng. Cái sai không nằm ở model nào cả, nằm ở một hằng số — nhưng nó chỉ lộ ra
> khi tôi truy ngược tại sao một style rõ ràng đúng lại không bao giờ được chọn."*

> **EN:** *"The lesson: **embedding decides who gets considered; the reranker decides who's right.**
> And that has a very concrete operational consequence I learned the hard way: if you cut `top_k` too
> early, it doesn't matter how good your reranker is, because the correct candidate was eliminated
> **before** it was ever looked at. We had `top_k=2` hardcoded instead of read from config, and the
> structurally-correct style — differing only in lapel width — sat at rank 3 by embedding, so it never
> reached the rerank stage. Fixing it to read config (3) let it through, and it won. The bug wasn't in
> any model; it was one constant — but it only surfaced when I traced back why an obviously-correct
> style was never being picked."*

> ⚠️ **Câu hỏi ngược rất dễ bị hỏi tiếp: "vậy sao không tăng `top_k` lên thật lớn?"**
>
> Trả lời: *"Vì cross-encoder tốn **N lần forward cho N candidate** — chi phí tăng tuyến tính theo
> `top_k`, còn với VLM rerank thì mỗi candidate là một lời gọi model 27B trên ảnh, rất đắt. Nên
> `top_k` chính là núm điều chỉnh recall-vs-cost, và nó phải **lớn hơn số style thật trong mỗi
> `product_group/product_subgroup`** — chứ không phải một hằng số chọn bừa. Tăng `top_k` chỉ là vá
> tạm; hướng triệt để là làm tầng 1 bớt sai, ví dụ trích attribute có cấu trúc (kiểu ve áo, kiểu túi,
> xẻ tà) một lần lúc ingest rồi so exact-match — deterministic, rẻ hơn, và audit được khi sai."*

#### Bẫy trung thực về reranker ⚠️

Nếu bị đào sâu, có hai điều phải nói thẳng:

1. **Đường vector search + rerank của Team C hiện chưa được gọi** — xem
   [§10](#10-ranh-giới-trung-thực--đừng-nói-quá) Lệch #2. Nói *"đã tích hợp và test, đang chờ bật cho
   nhánh chưa có supplier_art"*, đừng nói *"pipeline của tôi có rerank"* trống không.
2. **Fallback khi TEI không truy cập được là đếm từ trùng thuần** (`tei_reranker_client.py:149-173`):
   `overlap = len(query_terms & candidate_terms)`. Đây là degradation có chủ đích để không sập, nhưng
   nó **không phải** rerank ngữ nghĩa — nếu reranker chết mà không ai để ý thì chất lượng tụt âm thầm.
   Nói ra điều này thể hiện bạn nghĩ về failure mode, không chỉ happy path.

---


### 8.7 Nếu bị hỏi tiếp: "sửa gốc thì làm thế nào?"

Câu chuyện `top_k` ở [§8.4](#86--vì-sao-vẫn-cần-rerank--và-bằng-chứng-thật-từ-chính-hệ-của-bạn) mới là
**vá tạm**. Bạn có sẵn hai hướng sửa gốc đã viết ra trong `README.md` của repo — nói được cả hai kèm
ROI là điểm cộng lớn.

**Hướng 1 — attribute-level scoring lúc ingest (ROI cao hơn).** Dùng VLM trích 6 attribute có cấu
trúc thành JSON enum — lapel type (notch/shawl/peak), breasted, pocket style, vent, silhouette, fabric
surface — chạy **một lần cho mỗi sketch lúc ingest**, lưu vào payload Qdrant. Lúc query chỉ còn
exact/enum match:

```text
attribute_score = (số attribute khớp) / (tổng số attribute)
composite_score = w1 × image_cosine + w2 × attribute_score   # tính cho TOÀN BỘ pool, TRƯỚC khi cắt top_k
```

Điểm mấu chốt: chấm trên **toàn bộ pool trước khi cắt** → loại bỏ hẳn cái bug ở §8.4. Và chi phí là
**O(1) lần gọi VLM mỗi ảnh** thay vì O(N) lần gọi mỗi query như rerank hiện tại — rẻ hơn nhiều,
deterministic, và audit được khi sai.

**Hướng 2 — fine-tune embedding bằng triplet loss.** Sửa tại nguồn: dạy model ưu tiên chi tiết cấu
trúc thay vì màu/silhouette, bằng các bộ ba `(anchor, positive, hard negative)` — ví dụ
`(Y44440, V32843, G60362)`. Điểm quan trọng: negative phải là **hard negative** (trông giống nhưng
sai cấu trúc), không phải negative ngẫu nhiên như áo-vs-quần, vì đó mới đúng loại lỗi cần dạy.

> ⚠️ **Và đây là chỗ ghi điểm mạnh nhất — nói ra giới hạn trước khi bị hỏi:** collection hiện chỉ có
> **8 điểm = 4 style**. Fine-tune trên 4 style là học vẹt, không tổng quát hoá được.
>
> **VI:** *"Tôi đã đánh giá hướng fine-tune và kết luận là **chưa làm được**, không phải vì kỹ thuật
> mà vì dữ liệu: collection hiện có đúng 4 style. Mức đầu tư nhẹ nhất — freeze backbone, chỉ train một
> projection head — cũng cần vài trăm cặp mới thấy hiệu quả. Với 4 style thì bất kỳ fine-tune nào cũng
> chỉ overfit đúng 4 cái đó. Nên tôi ưu tiên hướng attribute-level trước: nó rẻ hơn, deterministic,
> audit được, và không cần đợi data. Fine-tune để dành đến khi historical library đủ lớn — tính bằng
> hàng chục đến hàng trăm style mỗi subgroup, không phải hàng chục style tổng."*
>
> **EN:** *"I evaluated fine-tuning and concluded it's **not feasible yet** — not for technical
> reasons but data ones: the collection currently holds four styles. Even the lightest tier — freeze
> the backbone, train only a projection head — needs a few hundred pairs to show an effect. With four
> styles, any fine-tune just overfits those four. So I prioritised the attribute-level approach
> instead: cheaper, deterministic, auditable, and it doesn't wait on data. Fine-tuning is parked until
> the historical library is genuinely large — tens to hundreds of styles per subgroup, not tens
> overall."*

> 💡 **Biết khi nào CHƯA nên dùng ML là tín hiệu Lead mạnh hơn là biết cách dùng ML.** Rất nhiều ứng
> viên sẽ đề xuất fine-tune ngay; rất ít người dừng lại đếm xem có đủ data hay không.

**Vì sao không dùng Docling để khoanh vùng bộ phận trang phục** (câu này hay bị hỏi vì nghe hợp lý):
`docling-layout-heron` là model **layout tài liệu** — nhãn của nó là `picture, table, text, caption,
title, section_header…` (xác minh qua `docling_core.types.doc.DocItemLabel`). Nó trả lời *"đây là một
vùng ảnh trên trang PDF"*, chứ không phải object detector cho nội dung **bên trong** bức sketch. Nó
không có và không thể có nhãn `lapel`/`pocket`/`vent` — đó là bài toán khác hẳn (page layout vs
garment-part detection).

---

---

### 8.8 Nối sang Katalon — vì sao ba câu trên đều on-domain

| Khái niệm của bạn | Bài toán tương ứng ở Katalon/TrueTest |
|---|---|
| Cosine trên embedding sketch | So một DOM/screenshot với snapshot lịch sử để phát hiện UI đã đổi |
| Bi-encoder lọc → cross-encoder xếp lại | Lọc nhanh hàng nghìn element ứng viên → xếp lại chính xác để chọn locator |
| Embedding sai vì train sai domain (ảnh sản phẩm vs sketch) | Model train trên web thường có thể sai trên app nội bộ/enterprise UI |
| `top_k` cắt sớm loại oan candidate đúng | Self-healing locator loại sớm element đúng → "chữa" ra locator sai |
| Attribute có cấu trúc thay embedding pooled | Dùng `data-testid` / `role` / text có cấu trúc thay vì chỉ so ảnh |

> **Câu nối mạnh:** *"Bài toán 'tìm techpack lịch sử giống nhất' của tôi và bài toán 'tìm lại element
> sau khi UI đổi' của TrueTest giống nhau về cấu trúc: một tầng embedding rẻ để lọc, một tầng đắt
> hơn để xác nhận, và **rủi ro lớn nhất nằm ở tầng lọc** — vì cái bị loại ở đó thì tầng sau không bao
> giờ có cơ hội sửa. Tôi đã trả giá cho bài học đó bằng một hằng số `top_k` hardcode."*

---

## 9. Điểm yếu thật — chuẩn bị trước khi bị hỏi

Đừng để interviewer tìm ra trước bạn. Với mỗi cái: thừa nhận → nói vì sao chấp nhận được ở bối
cảnh hiện tại → nói sẽ sửa thế nào.

| Điểm yếu | Sự thật | Cách trả lời |
|---|---|---|
| **Xử lý đồng bộ, không queue** | `POST /idp/pipeline/run` await thẳng orchestrator. VLM timeout **1200s**/call, uvicorn 4 worker. Một techpack nhiều trang giữ connection nhiều phút | *"Đây là hạn chế thật và là thứ tôi sửa đầu tiên nếu tăng tải. Hiện chấp nhận được vì volume nội bộ và người dùng chờ kết quả đồng bộ. Hướng đúng là job queue + polling/webhook — cũng chính là mảng 'batch and real-time' JD nhắc, tôi biết mình cần bổ sung"* |
| **Không có K8s** | Deploy bằng Docker Compose qua GitLab CI trên shell runner. `chart/` chỉ chứa diagram, không có `Chart.yaml` | *"Ở quy mô hiện tại Compose là đủ và tôi không muốn thêm chi phí vận hành chưa cần. Tôi hiểu K8s ở mức khái niệm nhưng chưa vận hành production — đây là gap tôi đang chủ động bịt"* |
| **State write-only, không resume** | Snapshot `state.json` mỗi bước nhưng không có đường đọc lại | Xem [§3.2](#32-memory-khai-báo-và-quản-lý-thế-nào) — đã có sẵn cách nói |
| **bge-m3 bị cắt còn 128 chiều** | bge-m3 sinh 1024-d, `embedding_service.py:30-38` truncate còn 128 | *"Đây là nợ kỹ thuật thật: 128 được chọn từ thời định dùng ColQwen multivector, không chỉnh lại khi chuyển sang bge-m3. Cắt cụt vector làm mất chất lượng retrieval. Tôi biết và nó nằm trong backlog"* — **đừng bịa lý do hợp lý hóa** |
| **Hai lockfile** | `poetry.lock` (14/08) và `uv.lock` (24/08) resolve khác nhau; Dockerfile còn chạy `poetry lock` lúc build → phá pinning | *"Reproducibility hazard thật, phải chốt một tool"* |
| **Secret commit vào repo** | Qdrant API key plaintext trong compose, `.env` được track | Nếu bị hỏi: thừa nhận thẳng, nói hướng đúng là secret manager / CI variable. **Đừng bào chữa** |
| **Không có Spark/batch** | Không có dấu vết | Xem [README.md](README.md) — đã chủ động hoãn, không phải sai sót |

> 💡 **Nguyên tắc chung:** với **mọi** điểm trong bảng này, câu trả lời tệ nhất là hợp lý hóa nó thành
> ưu điểm. Câu trả lời tốt nhất là: *"Đúng, đây là nợ kỹ thuật. Lý do nó tồn tại là [bối cảnh]. Nếu
> ưu tiên nó thì tôi sẽ [hành động cụ thể]."*

---

## 10. Ranh giới trung thực — đừng nói quá

**Đọc mục này trước khi vào phòng phỏng vấn.** Có một khoảng lệch giữa tài liệu nội bộ của bạn và
code hiện tại. Nếu interviewer đào sâu mà bạn mô tả sai, thiệt hại lớn hơn nhiều so với việc nói đúng
từ đầu.

### Lệch #1 ⚠️ — tool-calling agent không còn trên đường mặc định

- **Tài liệu `docs/ai_technologies_overview.md` §4.2, §5 của bạn** mô tả Agent 6 có tool-calling agent
  đang hoạt động.
- **Code hiện tại:** `_tool_assisted_search` (`material_resolver.py:730`) chỉ được gọi từ **test**, không
  từ `run()`. Chính bạn gỡ nó trong commit `b15d009` (23/07/2026).
- **Hệ quả kèm theo:** `orchestrator_team_a.py:645` vẫn đếm `resolution_source == "MATERIAL_TOOL_LLM_SELECTED"`
  — giá trị không thể sinh ra được nữa. Dòng trace `'Agent6: material not resolved via LangChain tools'`
  (`material_resolver.py:165`) cũng đã lạc hậu vì không có LangChain tool nào chạy ở đường đó.

| ❌ Đừng nói | ✅ Nói thế này |
|---|---|
| "Hệ thống của tôi dùng tool-calling agent trong production" | "Tôi đã build tool-calling agent với LangChain StructuredTool, có test đầy đủ. Rồi tôi **chủ động đưa nó ra khỏi đường mặc định** vì lý do rủi ro — và đó là quyết định tôi thấy đúng nhất trong dự án này" |

→ Dùng nguyên story ở [§4](#4-câu-chuyện-mạnh-nhất--bạn-tự-gỡ-llm-của-chính-mình). Nói đúng sự thật **mạnh hơn** nói quá.

### Lệch #2 — đường vector search + rerank của Team C cũng chưa được gọi

`retrieval_pipeline_team_c.py`: `_retrieve_material_list_without_sub_art` (chứa vector search + rerank)
**không được gọi** trong `run_retrieve`. Luồng thật: có `sub_art` → lookup chính xác qua REST API; không
có → set TBA. Có TODO ghi rõ `# TODO if not found by sub-art=supplier_art will collect top 3 items
( implement in next sprint )`.

→ Nói: *"Cross-encoder rerank đã tích hợp và test, hiện đang chờ bật cho nhánh chưa có supplier_art."*
Đừng nói *"pipeline retrieval của tôi có rerank"* trống không.

### Lệch #3 ⚠️ — "LangGraph 14 node" thực ra là 12 node

Commit `61824da` tên *"update diagram langgraph 14 nodes"* — nhưng nó chỉ sửa **file PlantUML**, không
đổi một dòng nào trong `graph.py`. Code có **12 lần `add_node`**; 14 = 12 + `START` + `END`. Chính
diagram ghi rõ: `box "14 node (10 business + 2 điều phối + START/END)"`.

→ Nói **"12 node"** (10 business + 2 điều phối). Nếu nói "14" mà interviewer đếm trong code thì bạn
mất uy tín ở một chi tiết vô nghĩa — không đáng.

Tương tự, **ReAct loop là tự viết tay**, không phải `create_react_agent`. Không có
`langgraph.prebuilt` nào được dùng. Nói *"tôi tự viết loop trong một node"* — và đó là **điểm mạnh**,
vì bạn kiểm soát được sliding window (`turns[-window_n:]`), truncate observation, và budget check
từng vòng.

### Lệch #4 — dự án agentic có nợ kỹ thuật riêng

- `pyproject.toml` khai `langgraph>=0.2.60` / `langchain>=0.3`, nhưng lock thực tế resolve
  **langgraph 1.2.8 / langchain 1.3.12** — sàn version đã lạc hậu rất xa. Ai resolve lại mà không có
  lock có thể rơi vào API 0.2.x khác hành vi.
- Key `task` được truyền qua `Send` payload nhưng **không khai trong `AgentState`** — key ad-hoc.
- Bakeoff chạy trên model `qwen3.6-27b-q6-vlm`, nhưng `.env` hiện đã đổi sang `qwen36-35b-q5-vlm`
  → **số liệu có trước lần đổi model đó**. Nếu bị hỏi "số này còn đúng không", trả lời thẳng: chưa
  đo lại sau khi đổi model.
- Chỉ có **một golden style** (VLTT1, 34 dòng) cho toàn bộ 43 run. n nhỏ, và tài liệu tự ghi nhận.
- RFC-003 tiêu chí (a) viết `>= 87.9%` nhưng đo được **87.8%** — được phán là "HOÀ trong nhiễu".
  Nghiêm ngặt thì đó là **fail 0.1pp được diễn giải thành hoà**. Nếu interviewer bắt điểm này, đừng
  chống: *"Đúng, nghiêm ngặt thì trượt 0.1 điểm. Tôi phán hoà dựa vào một chỉ số độc lập khác —
  item_code accuracy 93.9% vs 88.6% — nhưng bạn nói đúng là tôi đã nới tiêu chí do chính mình đặt ra,
  và lẽ ra nên ghi rõ là 'chấp nhận có điều kiện' thay vì 'hoà'."*

### Lệch #5 — config trap khiến biến môi trường bị bỏ qua âm thầm

`core/config.py:61-72` bind field LLM vào **biến của VLM** qua `validation_alias`:

```python
llm_base_url: str = Field(default='', validation_alias='OPENAI_API_ENDPOINT')
ai_model: str = Field(default='', validation_alias='OPENAI_MODEL')
```

Vì đặt `validation_alias` mà không có `populate_by_name`, các biến `LLM_BASE_URL`/`AI_MODEL` ghi trong
`.env-example` bị **bỏ qua im lặng** — mọi thứ đều rơi về `OPENAI_MODEL`.

→ Đây thực ra là **material tốt**: nếu được hỏi *"kể về một bug khó tìm"*, đây là ví dụ hay về lỗi
config im lặng — không crash, không log, chỉ là model chạy không phải model bạn nghĩ.

### Checklist trước phỏng vấn

- [ ] Đọc lại `material_resolver.py:100-165` — hiểu rõ đường chạy **hiện tại**, không phải đường trong doc
- [ ] Đọc lại docstring `zoom_recount_button_icons` (`document_analysis.py:565-590`) — đây là artifact mạnh nhất
- [ ] Nhớ ranh giới hai repo: **ai-service không LangGraph · bom-agentic CÓ LangGraph (12 node, không phải 14)**
- [ ] Nhớ ở ai-service: không hybrid search, không pgvector, không K8s, không queue, không caching
- [ ] Nhớ vòng cung **không** kết thúc ở "agentic thua" — RFC-003 ACCEPTED, `ACT_MODE=react` là default hiện tại
- [ ] Thuộc 6 con số: **89.1%** hard VLTT1 (256 ô) · **<70%** baseline trước rulebook · **75.8%** PTU
      trên 19 style · **65-70%** dòng còn TBA · **100%** Qwen≡Claude trên rulebook v2 · 248 commit + 254/360 merge
- [ ] Thuộc 1 câu: *"Đừng để LLM trả lời — để nó viết ra cách tìm câu trả lời"* ([§7.1](#71-rulebook--ý-tưởng-kiến-trúc-mạnh-nhất-và-cũng-dễ-kể-nhất))
- [ ] Chuẩn bị 1 câu cho mỗi dòng trong [§9](#9-điểm-yếu-thật--chuẩn-bị-trước-khi-bị-hỏi)
- [ ] Tự chạy `poetry run python motives/scripts/eval_rulebook.py` (không tham số) để nhìn lại bảng
      số của chính mình — đây là thứ bạn sẽ mô tả trong phòng phỏng vấn

---

## 11. Câu hỏi luyện tập — tự trả lời không nhìn giấy

### Nhóm A — agent & flow control (khả năng bị hỏi: cao)

1. Hệ thống của anh có phải agentic không? Định nghĩa "agentic" của anh là gì?
2. Vì sao anh không để LLM tự lập kế hoạch? Trong trường hợp nào anh **sẽ** để?
3. Anh đảm bảo agent chạy đúng flow bằng cách nào? *(→ [§3.4](#34--làm-sao-đảm-bảo-ai-agent-thực-thi-đúng-flow-bạn-mong-muốn))*
4. Nếu một bước fail giữa chừng thì sao? Resume được không?
5. Retry của anh có jitter không? Vì sao có/không?
6. Vì sao dùng LangChain mà không dùng LangGraph? Khi nào anh sẽ chuyển?

### Nhóm B — memory & state

7. State giữa các agent được truyền thế nào? Vì sao Pydantic model chứ không phải dict?
8. Có conversation memory không? Vì sao không cần?
9. Memory trong một agent khác gì memory giữa các agent?
10. Nếu muốn chạy 2 pipeline song song trên cùng một style thì có vấn đề gì không?

### Nhóm C — tool calling

11. Tool được khai báo thế nào? Ai định nghĩa schema?
12. Làm sao ngăn LLM gọi tool với tham số vô lý?
13. Anh có ép `tool_choice` không? Vì sao?
14. Làm sao ngăn LLM bịa ra kết quả không đến từ tool?
15. Nếu LLM gọi tool lặp vô hạn thì sao? *(⚠️ trung thực: `MATERIAL_TOOL_MAX_ITERATIONS` chỉ được nhét vào prompt như hint, **không** enforce bằng recursion limit — đây là điểm yếu thật, nói thẳng)*

### Nhóm D — độ tin cậy & eval

16. Làm sao anh biết output của LLM đúng?
17. Confidence score tính thế nào? Vì sao không hỏi LLM?
18. Chống hallucination bằng cách nào? *(→ grounding gate [§6.1](#61-grounding-gate--chỉ-giữ-cái-verify-được-ngược-lại-nguồn))*
19. Vì sao lấy 7 mẫu ở temperature 0.3 mà không phải 1 mẫu ở temperature 0?
20. Khi nào hệ thống dừng lại và gọi người?

### Nhóm E — eval, chi phí & vận hành

21. Anh biết hệ thống chính xác bao nhiêu %? Đo trên gì, bao nhiêu mẫu? *(→ [§7.2](#72-chất-lượng-end-to-end--số-thật-và-trung-thực-về-điểm-chưa-tốt))*
22. Đổi prompt xong làm sao biết không làm hỏng chỗ khác? *(→ golden snapshot + harness + validator, [§7.4](#74-bằng-chứng-tái-lập-được-reproducibility))*
23. Vì sao dùng model tự host thay vì gọi API frontier? *(→ [§7.3](#73-qwen-vs-claude--model-tiering-thật-và-một-kết-quả-bất-ngờ))*
24. Anh kiểm soát chi phí LLM thế nào? *(→ [§7.5](#75-kiểm-soát-chi-phí--có-bằng-cap-chứ-không-bằng-cache); nhớ thừa nhận thiếu caching + bug `llm_tokens_total`)*
25. Kể một tối ưu hoá anh làm mà đo ra là **sai**. *(→ biến thể "capped" làm accuracy tệ đi, [§7.3](#73-qwen-vs-claude--model-tiering-thật-và-một-kết-quả-bất-ngờ))*
26. 65-70% dòng vẫn là TBA — anh gọi đó là thành công à? *(⚠️ câu khó, có sẵn cách trả lời ở [§7.2](#72-chất-lượng-end-to-end--số-thật-và-trung-thực-về-điểm-chưa-tốt))*

### Nhóm F — retrieval, embedding & rerank

27. Hybrid search hay dense-only? Vì sao?
28. **Vector trong embedding là gì? Từng chiều có ý nghĩa người đọc được không?** *(→ không — ý nghĩa nằm ở quan hệ giữa các vector, [§8.1](#81-nền-tảng-vector--dot-product--cosine))*
29. **Dot product là gì? Ý nghĩa hình học của nó?** *(→ `‖A‖·‖B‖·cos θ` — trộn 2 độ dài + 1 góc vào một số)*
30. **Cosine similarity là gì? Vì sao cosine mà không phải Euclidean?** *(→ [§8.1](#81-nền-tảng-vector--dot-product--cosine))*
31. Cosine và dot product khác nhau chỗ nào? *(→ cosine = dot product sau khi chuẩn hoá độ dài về 1 — cùng một phép toán)*
32. Khi nào cosine và dot product cho cùng xếp hạng? *(→ khi vector đã L2-normalize; Qdrant normalize lúc upsert)*
33. Cosine **similarity** khác cosine **distance** ở đâu? *(⚠️ bẫy: Qdrant đặt tên `Distance.COSINE` nhưng score càng CAO càng giống)*
34. **Qdrant có mấy loại distance? Cái nào là default?** *(⚠️ bẫy: đúng **4** loại, và **không có default** — `distance` là required, [§8.2](#82-qdrant-có-đúng-4-loại-distance--và-không-có-loại-nào-là-default))*
35. Khi nào anh chọn `DOT` thay vì `COSINE`? *(→ khi đã tự normalize trước upsert, hoặc norm mã hoá độ phổ biến)*
36. `MANHATTAN` (L1) dùng khi nào? *(→ dữ liệu thưa/nhiều chiều, ít nhạy outlier hơn L2)*
37. **Vì sao 512 chiều cho ảnh mà 128 cho text? Con số đó chọn thế nào?** *(⚠️ nợ kỹ thuật — [§8.5](#85--128-dim-vs-512-dim--và-nợ-kỹ-thuật-thật-cần-thừa-nhận))*
38. Cắt vector 1024 → 128 chiều có hợp lệ không? *(→ chỉ hợp lệ với model Matryoshka/MRL; `bge-m3` không phải)*
39. Nhiều chiều hơn có luôn tốt hơn không? *(→ [§8.4](#84-thực-tiễn-2026--cái-gì-đang-phổ-biến-và-bạn-đang-đứng-ở-đâu): 1024 là sweet spot, 3072→1024 chỉ mất ~0.03 Recall@10)*
40. **Anh sẽ giảm chi phí vector search thế nào?** *(→ quantization int8 cho 4× gần như miễn phí, TRƯỚC khi nghĩ tới giảm chiều — [§8.4](#84-thực-tiễn-2026--cái-gì-đang-phổ-biến-và-bạn-đang-đứng-ở-đâu))*
41. Binary quantization nên dùng khi nào? *(→ 32× tiết kiệm nhưng mất ~19% recall → **chỉ khi có rerank phía sau**)*
42. **Đã có cosine similarity rồi thì vì sao còn cần rerank?** *(→ bi-encoder vs cross-encoder, [§8.6](#86--vì-sao-vẫn-cần-rerank--và-bằng-chứng-thật-từ-chính-hệ-của-bạn))*
43. Reranker là model gì? Vì sao cross-encoder chứ không phải LLM judge?
44. Vì sao không tăng `top_k` lên thật lớn cho chắc? *(→ chi phí cross-encoder tuyến tính theo N)*
45. Vì sao `techpack_history` dùng multivector MAX_SIM còn `master_materials` thì không? *(→ [§8.3](#83-embedding-strategy--ba-cấu-hình-khác-nhau-trong-cùng-một-hệ))*
46. Kể một lần retrieval trả sai và anh tìm ra root cause thế nào? *(→ story top_k [§8.6](#86--vì-sao-vẫn-cần-rerank--và-bằng-chứng-thật-từ-chính-hệ-của-bạn))*
47. Nếu reranker chết thì hệ thống hành xử thế nào? *(⚠️ fallback là đếm từ trùng thuần — degradation âm thầm)*
48. Anh sẽ fine-tune embedding không? Vì sao chưa? *(→ [§8.7](#87-nếu-bị-hỏi-tiếp-sửa-gốc-thì-làm-thế-nào), chỉ có 4 style)*
49. Anh theo dõi lĩnh vực embedding/retrieval thế nào? *(→ MTEB đổi mỗi 3-4 tháng → giữ eval harness quan trọng hơn chọn đúng model hôm nay)*

### Nhóm G — LangGraph & vòng cung RFC (từ `motivesidp-bom-agentic`)

50. Vì sao dùng graph mà không phải pipeline tuần tự? Graph mua được gì? *(→ fan-out `Send` động, chu trình verify→replan, wave ordering, join node)*
51. Kể về `retry_dispatch` — vì sao cần một node chỉ `return {}`? *(→ router chạy per-branch nhân bản Send → retry storm tới attempt 31)*
52. Vì sao `rule_tasks` cần reducer riêng mà `trace` chỉ cần `operator.add`?
53. LLM có được quyết định routing không? *(→ **"LLM quyết pass/fail, code tất định quyết topology"**)*
54. Kể về một RFC bạn tự bác bỏ. *(→ [§1B](#1b--hai-hệ-thống-một-vòng-cung--đây-mới-là-câu-chuyện-đầy-đủ), nhớ nói đủ cả phần mở lại và thắng)*
55. Vì sao token react gấp 4.45× mà không phải ~2×? *(→ mỗi vòng gửi lại toàn bộ transcript → tăng theo bình phương số bước; đây là **structural, không phải nhiễu**)*
56. Bạn làm gì khi phương sai lớn hơn hiệu ứng muốn đo? *(→ meta-finding sau 15 run, nâng n=3→5, và pivot sang sửa verifier)*
57. "Escalated giảm" mà bạn lại coi là tín hiệu xấu — vì sao? *(→ **confidently wrong**; kiểm chéo bằng item_code accuracy)*
58. Kể về một bug do feature flag. *(→ [§4B](#4b--bug-hay-nhất-bạn-có--và-cách-bạn-chặn-nó-tái-diễn), chốt bằng test bất biến)*

### Nhóm H — nối sang Katalon/TrueTest

59. Kinh nghiệm này áp dụng vào TrueTest thế nào?
60. Nếu anh phải thiết kế phần "LLM sinh test case" của TrueTest, anh chặn hallucination ra sao?
61. Anh sẽ đo "chất lượng test case sinh tự động" bằng metric gì?

**Gợi ý trả lời nhóm H — bảng dịch domain:**

| Bài toán của bạn | Bài toán tương ứng ở TrueTest |
|---|---|
| Mã vật liệu bịa ra không có trong techpack | Locator/selector bịa ra không có trong DOM |
| Grounding gate: đối chiếu ngược vào text nguồn | Đối chiếu selector ngược vào DOM snapshot |
| Validation gate: chỉ tin cái verify được | Chạy thật test sinh ra trong sandbox, chỉ đề xuất cái PASS |
| Self-consistency voting 7 mẫu | Chạy test 3 lần để lọc flaky |
| TBA + `needs_human_review` thay vì đoán | Không auto-merge vào suite của khách |
| Confidence từ số candidate khớp | Confidence từ số chiến lược locator cùng trỏ một element |
| Provenance từng field | Trace test case về journey/DOM sinh ra nó |

> **Câu kết mạnh khi được hỏi "vì sao Katalon":**
>
> *"Hệ thống tôi đang làm và TrueTest giải hai bài toán khác nhau về domain nhưng **giống hệt nhau về
> cấu trúc rủi ro**: một model đọc tài liệu phi cấu trúc, sinh ra artifact có cấu trúc, và artifact
> đó đi vào một quy trình có hậu quả thật — đơn mua hàng ở phía tôi, test suite của khách ở phía các
> anh. Trong cả hai, thất bại nguy hiểm không phải là model từ chối trả lời, mà là model trả lời một
> cách hợp lý và sai. Tôi đã dành phần lớn năm nay xây các cơ chế để bắt đúng loại thất bại đó."*

---

## 12. Nguồn — tự verify trước khi dùng

Tất cả đường dẫn dưới đây tương đối từ `motivesidp-ai-service/`:

### Repo `motivesidp-bom-agentic` (hệ agentic)

| Chủ đề | File |
|---|---|
| **LangGraph build — 12 node, 4 conditional edge, join node** | `app/agent/graph.py` (đọc cả docstring `retry_dispatch` dòng 48-49) |
| State TypedDict + **reducer cho ghi song song** | `app/agent/state.py:8-15, 18` |
| **Checkpointer** (resume + time-travel) | `app/agent/memory/checkpointer.py` |
| **Cross-run lesson store** + bẫy định danh | `app/agent/memory/store.py` |
| **ReAct loop tự viết** + tool scoping + error-as-observation | `app/agent/nodes/rule_worker/actor.py:70-148` |
| Tool registry, scope theo node | `app/agent/tools/registry.py` |
| **Bug `ENABLE_REPLAN` + test bất biến** ⭐ | `tests/unit/test_retry_flags.py:72-105` · commit `86bc5e0` |
| Nơi duy nhất quyết verdict | `app/agent/nodes/verify/node.py:82-87` |
| **Bakeoff — 7 vòng, 43 run, mọi con số** ⭐ | `docs/BAKEOFF_RFC001.md` (336 dòng) |
| RFC-001 / 002 / 003 (tiêu chí + phán quyết) | `docs/rfc/RFC-00{1,2,3}-*.md` |
| Eval harness (`overall = coverage × field_micro`) | `scripts/eval_bom.py` |
| Script chấm ĐẠT/FAIL tự động | `scripts/bakeoff_summary.py:54-58` |
| **Bài học vận hành đo lường** | `docs/RUNBOOK.md:107-162` |

### Repo `motivesidp-ai-service` (production)

| Chủ đề | File |
|---|---|
| Flow tất định, memory, retry, trace | `motives/src/application_platform/bom_agent/services/base_agent.py` |
| Pipeline tuần tự, state | `motives/src/application_platform/orchestrator_team_a.py` |
| Tool calling, args schema | `motives/src/application_platform/bom_agent/tools/material_search.py` |
| Agent 6, câu chuyện §4 | `motives/src/application_platform/bom_agent/services/material_resolver.py` |
| Grounding gate, voting | `motives/src/vlm/services/document_analysis.py` |
| Confidence, provenance | `motives/src/application_platform/bom_agent/services/provenance.py` |
| Prompt versioning, path guard | `motives/src/application_platform/bom_agent/prompts/loader.py` |
| Qdrant collection, dim | `motives/src/data_platform/qdrant/qdrant_setup.py` |
| **Rulebook — "LLM viết strategy, không viết đáp án"** | `motives/src/application_platform/bom_agent/services/rule_builder_v2.py` (đọc docstring dòng 1-5) |
| Rulebook prompt (mục tiêu zero-LLM-per-style) | `.../bom_agent/prompts/agent_build_rulebook/prompt.yml` |
| Eval harness (exact/fuzzy/NEEDS_REVIEW + ngưỡng pass) | `motives/scripts/eval_rulebook.py` |
| Validator chặn deploy (T1–T4, `sys.exit(1)`) | `motives/scripts/validate_rulebook.py` |
| Số đo end-to-end VLTT1 | `docs/test-data/vltt1/README.md` · `docs/team-a-data-samples/VLTT1_TEST_GUIDE.md` |
| Số đo PTU 19 style + lộ trình cải thiện | `docs/requirements/RE-MIDP-PTU-Review-Roadmap.md` |
| Cap chi phí | `.../bom_agent/core/config.py` (`max_rule_rows_for_llm`, `srs_llm_chunk_char_budget`…) |
| Model tiering Qwen online / Claude offline | `.env-example:89-103` |
| **Doc tổng quan AI bạn tự viết** | `docs/ai_technologies_overview.md` ⚠️ có 1 chỗ lệch — xem [§10](#10-ranh-giới-trung-thực--đừng-nói-quá) |
| Story top_k + hướng cải thiện | `README.md` mục *"Cách tính similarity techpack"* |

**Lệnh verify nhanh câu chuyện §4:**

```bash
cd /Users/duc.nguyen/data/projects/success/motives/motivesidp-ai-service
git show b15d009 -- motives/src/application_platform/bom_agent/services/material_resolver.py
grep -rn "_tool_assisted_search" --include="*.py" . | grep -v .venv
```
