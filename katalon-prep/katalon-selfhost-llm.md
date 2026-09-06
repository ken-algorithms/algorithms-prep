# Cloud LLM → self-host: bài toán Katalon đang tuyển, và bạn đã làm rồi

> **Vì sao file này tồn tại:** hướng tuyển của Katalon đã dịch sang **AI Engineer**, trọng tâm là
> chuyển từ tích hợp **OpenAI / AWS Bedrock / Azure OpenAI** sang **self-host model** để kéo chi phí
> suy luận xuống.
>
> ⚠️ **Nguồn của thông tin pivot này là bạn, không phải tôi tra được.** Tra công khai ngày
> 06/09/2026 chỉ thấy Katalon đang mở **Product Security Engineer** trên ITviec — không có JD AI
> Engineer công khai. Xem [§10](#10-ranh-giới-trung-thực) trước khi dùng bất kỳ dòng nào ở đây làm
> căn cứ chắc chắn.
>
> 🔴 **Đọc [katalon-llm-migration-va-scale.md](katalon-llm-migration-va-scale.md) TRƯỚC nếu bạn
> từng trượt ở nhóm câu này.** File đó lo bốn thứ khó hơn: migrate không ảnh hưởng hệ đang chạy,
> **chứng minh** kết quả được bảo toàn, test tự động trên đầu ra không tất định, và scale không mất
> request. File này chỉ lo *câu chuyện* và *kinh tế học*.
>
> > **Tin rất tốt:** đây gần như là bài toán *"kể lại đúng việc mình đã làm"*. Cả hai hệ thống AI của
> bạn **đã chạy 100% self-host** — có `.env`, có `docker-compose`, có lịch sử migrate, có số đo
> chất lượng. Rất ít ứng viên có thứ này.

---

## Mục lục

- [1. Bằng chứng: bạn đã làm đúng việc họ đang tuyển](#1-bằng-chứng-bạn-đã-làm-đúng-việc-họ-đang-tuyển)
- [2. Cái seam khiến mọi thứ swap được](#2-cái-seam-khiến-mọi-thứ-swap-được)
- [3. Kinh tế học: khi nào self-host thật sự rẻ hơn](#3-kinh-tế-học-khi-nào-self-host-thật-sự-rẻ-hơn)
- [4. Serving stack: llama.cpp vs vLLM — bạn đã migrate thật](#4-serving-stack-llamacpp-vs-vllm--bạn-đã-migrate-thật)
- [5. Cái giá thật của self-host](#5-cái-giá-thật-của-self-host)
- [6. Bảy bậc giảm chi phí, xếp theo tỉ lệ hoàn vốn](#6-bảy-bậc-giảm-chi-phí-xếp-theo-tỉ-lệ-hoàn-vốn)
- [7. Khi nào **không** nên self-host](#7-khi-nào-không-nên-self-host)
- [8. Câu trả lời phỏng vấn + 10 follow-up](#8-câu-trả-lời-phỏng-vấn--10-follow-up)
- [9. Kế hoạch 2 tuần](#9-kế-hoạch-2-tuần)
- [10. Ranh giới trung thực](#10-ranh-giới-trung-thực)

---

## 1. Bằng chứng: bạn đã làm đúng việc họ đang tuyển

Tất cả đã đọc trực tiếp từ hai repo, không suy đoán.

| # | Bằng chứng | Ở đâu |
|:---:|---|---|
| 1 | `OPENAI_API_ENDPOINT=http://192.168.2.71:8611/v1` — **IP LAN, không phải api.openai.com** | `motivesidp-ai-service/.env` |
| 2 | `OPENAI_MODEL="qwen36-35b-q5-vlm"` — model OSS, lượng tử hoá q5 | `motivesidp-ai-service/.env` |
| 3 | **`OPENAI_SELFHOSTED_MAX_TOKENS=10240`** — biến tự nó nói ra kiến trúc | `motivesidp-ai-service/.env` |
| 4 | `OPENAI_BASE_URL=http://192.168.2.152:8611/v1` — host suy luận **thứ hai**, tách khỏi host của ai-service | `motivesidp-bom-agentic/.env` |
| 5 | Qdrant cũng self-host: `QDRANT_HOST=http://192.168.2.69` | `motivesidp-ai-service/.env` |
| 6 | **Hai file compose cho hai backend suy luận**: `docker-compose.llm.yml` (llama.cpp GGUF + mmproj) và `docker-compose.llm.vllm.yml` (vLLM v0.19.0-cu130, FP8, ctx 65536) | `motivesidp-bom-agentic/deploy/` |
| 7 | Lịch sử nâng model có thật: `3574f77 upgrade vlm to qwen 3.6 27b q6 gguf` → `7bb03f5 update qwen 3.6 27b to qwen 3.6 35b` | git log |
| 8 | `4c8558f Demo v2: ... **Anthropic opt-in**` — đường cloud tồn tại nhưng bị hạ xuống thành tuỳ chọn | git log |
| 9 | `3ffed19 chore: **remove provider traces** from customer-facing branches` | git log |
| 10 | Comment trong code: *"Inference always uses the self-hosted Qwen server. **NEVER set `LLM_PROVIDER=anthropic` here.**"* | [AI-STACK §7.3](AI-STACK-INTERVIEW-ANSWERS.md) |
| 11 | Đã **đo** chất lượng OSS vs cloud, không chỉ đoán: Qwen khớp Claude **100% action (27/27)** trên rulebook v2 | [AI-STACK §7.3](AI-STACK-INTERVIEW-ANSWERS.md) |
| 12 | Có tài liệu **quyết định prompt vs RAG vs fine-tune (LoRA)** | `motivesidp-bom-agentic/docs/TUTORIAL_RFC_LORA.md` |

> **Điểm mạnh nhất không phải là "tôi đã self-host".** Nhiều người chạy được Ollama trên máy. Điểm
> mạnh là **#11**: bạn *đo* chất lượng model tự host so với model cloud trên chính bài toán của
> mình, rồi mới quyết định — và có con số công bố lại được.

---

## 2. Cái seam khiến mọi thứ swap được

Đây là ý kiến trúc quan trọng nhất của cả file, và cũng là câu mở đầu nên nói khi được hỏi.

```text
KHÔNG phải:  code gọi thẳng SDK của từng nhà cung cấp
             openai.ChatCompletion.create(...)      ─┐
             bedrock_runtime.invoke_model(...)       ├─ đổi provider = SỬA CODE
             azure_client.chat.completions(...)     ─┘

MÀ LÀ:       code chỉ biết MỘT giao thức — OpenAI-compatible HTTP
             POST {BASE_URL}/v1/chat/completions
                     │
                     ├── api.openai.com          (cloud)
                     ├── Azure OpenAI            (cloud, cùng shape)
                     ├── AWS Bedrock (qua gateway proxy)
                     └── vLLM / llama.cpp / SGLang / TGI   ◄── self-host
                                                              ĐỔI PROVIDER = ĐỔI MỘT BIẾN MÔI TRƯỜNG
```

**Bằng chứng seam này hoạt động trong hệ của bạn:** biến vẫn tên `OPENAI_*` nhưng trỏ vào
`192.168.2.71:8611`. Code không biết và không cần biết. Đó là lý do bạn đổi từ llama.cpp sang vLLM
mà **không sửa một dòng application code nào** — file compose vLLM còn giữ nguyên
`--served-model-name qwen3.6-27b-q6-vlm` để app cũ chạy tiếp.

**Và đây là chỗ nối thẳng vào Katalon:** TrueTest cho phép dùng **AI service đóng gói sẵn hoặc một
OpenAI-compatible API key**. Nghĩa là **sản phẩm của họ đã nói đúng giao thức này rồi** — bài toán
self-host của họ không phải viết lại tích hợp, mà là **dựng và vận hành đầu bên kia của seam**.

> **Câu nên nói:** *"Việc đầu tiên tôi làm không phải chọn model, mà là đảm bảo tầng ứng dụng chỉ
> biết một giao thức. Khi seam đó đúng rồi thì self-host, cloud, hay chạy song song A/B đều chỉ là
> cấu hình — và quan trọng hơn, mình **quay lại cloud được trong một phút** nếu self-host có sự cố.
> Không có đường lui đó thì không ai dám chuyển."*

---

## 3. Kinh tế học: khi nào self-host thật sự rẻ hơn

Đây là phần dễ nói sai nhất. Self-host **không** tự động rẻ hơn.

### Cấu trúc chi phí

```text
CLOUD API:      chi phí = tokens × đơn giá
                → tuyến tính, bắt đầu từ ~0, không có chi phí cố định
                → dùng ít thì gần như miễn phí

SELF-HOST:      chi phí = GPU (thuê hoặc khấu hao)  +  NGƯỜI VẬN HÀNH  +  điện/chỗ đặt
                → gần như CỐ ĐỊNH, không phụ thuộc bạn gọi 1 hay 100 triệu token
                → dùng ít thì cực đắt trên mỗi token
```

Hai đường thẳng cắt nhau ở một điểm. **Toàn bộ quyết định nằm ở chỗ: bạn đang ở bên nào của điểm đó.**

### Số tham khảo 2026 *(nguồn ngoài, không phải bạn đo — xem [§10](#10-ranh-giới-trung-thực))*

| Đại lượng | Giá trị |
|---|---|
| Điểm hoà vốn thường gặp | **~600 triệu – 1,2 tỉ token/tháng** |
| Model 7B trên A100 80G | **~$0,15 / triệu token** |
| API hạng mini tương đương | **~$1,50 / triệu token** (≈ 10×) |
| GPU chiếm bao nhiêu TCO | **60–70%** |
| Người vận hành chiếm bao nhiêu | **25–30%** |
| Cụm 8×H100 on-demand | **$22–28K / tháng** |
| Một inference engineer senior (loaded) | **$20–30K / tháng** |
| Độ trễ self-host H100 vs API cloud | **~18ms** vs **~350ms** |

**Ba điều rút ra, và điều thứ ba mới là điều đáng nói:**

1. Chênh **~10×** trên mỗi token là thật — nhưng chỉ hiện thực hoá **sau** điểm hoà vốn.
2. **Người tốn gần bằng GPU.** Mọi bài phân tích chỉ đếm tiền GPU đều sai. Một cụm 8×H100 và một
   inference engineer có bậc chi phí ngang nhau.
3. **Độ trễ mới là lý do thuyết phục nhất, không phải tiền.** 18ms so với 350ms là khác biệt gần
   **20×**, và nó không phụ thuộc bạn dùng nhiều hay ít. Với sản phẩm cần phản hồi tức thì trong
   IDE hoặc trong lúc chạy test, đó là khác biệt về **trải nghiệm**, không phải về hoá đơn.

> **Câu phân biệt Senior với Lead:** *"Tôi sẽ không mở đầu bằng lý do chi phí. Chi phí chỉ thắng
> sau điểm hoà vốn, mà điểm đó phải đo mới biết. Ba lý do vững hơn và đúng ngay từ token đầu tiên
> là: **độ trễ**, **dữ liệu không rời hạ tầng của mình**, và **không bị nhà cung cấp đổi model dưới
> chân**. Nếu ba cái đó không quan trọng với sản phẩm thì tôi sẽ khuyên đừng self-host."*

### Ba lý do đúng ngay cả khi chưa hoà vốn

| Lý do | Vì sao mạnh hơn lý do chi phí |
|---|---|
| **Độ trễ** | ~20× và ổn định. Không phụ thuộc hàng đợi của nhà cung cấp |
| **Dữ liệu ở lại nhà** | Với Katalon: TrueTest **ghi lại phiên của người dùng thật của khách hàng**. Gửi DOM đó ra API bên thứ ba là bài toán compliance, không phải bài toán kỹ thuật. Đây có thể là lý do **thật sự** đằng sau pivot |
| **Model không đổi dưới chân** | API deprecate model, đổi trọng số, đổi hành vi. Eval harness đang xanh bỗng đỏ mà bạn không đổi gì. Self-host ghim được checkpoint |

---

## 4. Serving stack: llama.cpp vs vLLM — bạn đã migrate thật

Không cần đọc lý thuyết ở đâu: file `deploy/docker-compose.llm.vllm.yml` của bạn **tự ghi lại lý do
migrate** ngay trong comment đầu file.

| | **llama.cpp** | **vLLM** |
|---|---|---|
| Định dạng trọng số | **GGUF** lượng tử hoá | Checkpoint **HF/Transformers**, FP8/AWQ/GPTQ |
| Điểm mạnh | Chạy được trên VRAM nhỏ, CPU cũng chạy, dựng nhanh | **Throughput cao**: PagedAttention, continuous batching |
| Hợp với | Một người dùng, thử nghiệm, máy yếu | **Nhiều request đồng thời — tức production** |
| VLM (ảnh) | Cần `--mmproj` riêng | Dùng thẳng checkpoint HF của model |
| Bạn đã dùng | Giai đoạn đầu, Qwen 3.6 27B **q6 GGUF** | Hiện tại, **FP8, ctx 65536** |

**Ba dòng comment trong file compose của bạn, mỗi dòng là một câu trả lời phỏng vấn sẵn:**

1. *"vLLM không map 1:1 với `--mmproj` của llama.cpp. Với VLM, hướng đúng là dùng checkpoint
   Hugging Face của model tương ứng."* → bạn biết **VLM khác LLM ở tầng serving**, không chỉ ở tầng
   prompt.
2. *"Giữ `--served-model-name qwen3.6-27b-q6-vlm` để app có thể tiếp tục dùng `MODEL_NAME` cũ."*
   → **migrate không downtime, không sửa app**. Đây chính là seam ở [§2](#2-cái-seam-khiến-mọi-thứ-swap-được) phát huy.
3. *"Nếu VRAM không đủ cho FP8 + ctx 65536, hạ `--max-model-len` xuống 32768 trước."* → bạn biết
   **KV cache tỉ lệ với context length**, và biết nút nào vặn trước khi đổi model.

> **Điểm thứ ba đáng đào sâu nếu bị hỏi:** VRAM = trọng số + **KV cache**. Trọng số cố định; KV
> cache = `2 × n_layers × n_kv_heads × head_dim × ctx_len × batch × dtype`. Nên khi hết VRAM, hạ
> `max-model-len` rẻ hơn nhiều so với đổi sang model nhỏ hơn — vì nó không đụng tới chất lượng, chỉ
> đụng tới độ dài đầu vào tối đa.

---

## 5. Cái giá thật của self-host

Phần này quan trọng vì nó chứng minh bạn đã **chạy thật**, không phải đọc blog. Và bạn có commit
làm bằng chứng.

### 5.1 Model yếu hơn làm vỡ structured output

```text
f97a3f9  MIDP-584: fix parsing json not work with Qwen 3.6 35B
```

Đây là **cái giá điển hình nhất** của self-host, và gần như không blog nào nói: model OSS tuân thủ
JSON schema kém hơn model cloud hàng đầu. Không phải "kém thông minh hơn" — mà là **kém kỷ luật định
dạng hơn**. Chưa kể Qwen còn sinh khối `<think>` phải bóc trước khi parse.

**Bốn lớp phòng thủ, xếp theo thứ tự nên áp dụng:**

| Lớp | Làm gì | Chi phí |
|---|---|---|
| 1. **Constrained decoding** | vLLM hỗ trợ guided JSON / grammar — model **không thể** sinh token phá schema | Gần như miễn phí. **Làm trước tiên** |
| 2. **Bóc `<think>` + tìm khối JSON** | Chuẩn hoá đầu ra trước khi parse | Rẻ |
| 3. **Retry có phản hồi lỗi** | Đưa chính thông báo lỗi parse vào lượt sau làm observation | Tốn token |
| 4. **Escalate** | Quá số lần thì đánh dấu `NEEDS_REVIEW`, không đoán bừa | Tốn người |

Bạn đã có lớp 2, 3, 4. **Lớp 1 là thứ nên bổ sung và nên nói ra** — nó là câu trả lời "đúng bài" cho
người phỏng vấn AI engineer.

### 5.2 Vận hành mà cloud API lo hộ bạn

| Việc | Cloud | Self-host |
|---|:---:|---|
| Autoscale theo tải | Tự động | **Bạn lo.** GPU không scale-to-zero nhanh như container |
| Model chết / OOM | Tự động | **Bạn lo.** OOM giữa lúc suy luận là chuyện thường |
| Cập nhật model | Nhà cung cấp | **Bạn lo** — nhưng đây cũng là *ưu điểm*: bạn chọn lúc |
| Nhiều model song song | Không cần nghĩ | **Bạn lo.** Mỗi model chiếm VRAM riêng |
| Đo p50/p99, hàng đợi | Có dashboard | **Bạn lo.** Cần Prometheus + metric của vLLM |

**Sáu metric phải có khi tự vận hành** — nói được danh sách này là tín hiệu đã từng vận hành thật:

```text
tokens/s (prefill và decode TÁCH RIÊNG — hai pha có đặc tính khác hẳn)
GPU utilization  +  VRAM đã cấp
độ dài hàng đợi  +  thời gian chờ trước khi được xếp lịch
TTFT (time to first token)  — cái người dùng cảm nhận
p99 end-to-end theo từng loại request
tỉ lệ lỗi parse schema   ◄── metric riêng của bài toán self-host, đo đúng §5.1
```

### 5.3 Ba GPU rảnh vẫn tốn tiền như ba GPU bận

Đây là khác biệt tư duy lớn nhất so với cloud API. Với API, không gọi thì không trả tiền. Với GPU,
**đơn vị lãng phí là thời gian rảnh**, nên tối ưu không phải "gọi ít đi" mà là **nhồi đầy batch**.

→ Đó là lý do `continuous batching` của vLLM đáng giá hơn mọi mẹo prompt: nó tăng throughput trên
cùng phần cứng, tức **giảm chi phí trên mỗi token mà không đụng gì tới chất lượng**.

---

## 6. Bảy bậc giảm chi phí, xếp theo tỉ lệ hoàn vốn

Thứ tự này là nội dung chính khi họ hỏi *"làm sao tối ưu chi phí"*. Nói theo đúng thứ tự — nó cho
thấy bạn ưu tiên theo hiệu quả, không theo độ "ngầu".

| # | Bậc | Được | Mất | Bạn đã có? |
|:---:|---|---|---|:---:|
| 1 | **Đừng gọi LLM** — cache theo hash đầu vào, rule tất định, tra bảng | Giảm nhiều nhất, không mất gì | Phải nhận ra chỗ nào không cần LLM | ✅ **Rulebook cache** — [AI-STACK §7.1](AI-STACK-INTERVIEW-ANSWERS.md) |
| 2 | **Cắt đầu vào** — chỉ đưa DOM subtree liên quan, không đưa cả trang | Token vào giảm mạnh; prefill là pha tốn nhất | Cần logic chọn lọc | ✅ Cap `max_rule_rows_for_llm` |
| 3 | **Continuous batching** (vLLM) | Throughput tăng nhiều lần, **không đụng chất lượng** | Không có nhược điểm thật | ✅ Đã chạy vLLM |
| 4 | **Model tiering** — model nhỏ phân loại/trích xuất, model lớn chỉ để sinh | Phần lớn lượt gọi rơi vào model rẻ | Phải định tuyến, và phải **đo** chứ đừng đoán | ✅ Claude offline build rulebook, Qwen chạy runtime |
| 5 | **Lượng tử hoá** — FP8 / AWQ / q5-q6 | VRAM giảm ~2-4×, throughput tăng | Mất chất lượng — **phải đo**, đừng tin bảng benchmark chung | ✅ q5, FP8 |
| 6 | **Fine-tune LoRA** thay vì prompt dài | Prompt ngắn lại vĩnh viễn; model nhỏ đạt chất lượng model lớn | Chi phí train + pipeline dữ liệu + nguy cơ quá khớp | 🟡 Có tài liệu quyết định (`motivesidp-bom-agentic/docs/TUTORIAL_RFC_LORA.md`), **chưa train** |
| 7 | **Speculative decoding** | Giảm độ trễ decode | Phức tạp, cần model nháp | ❌ Chưa |

> **Bậc 1 là bậc bị bỏ qua nhiều nhất và cũng lời nhất.** Kiến trúc rulebook của bạn —
> *"LLM không bao giờ trả về giá trị, nó chỉ ghi lại **cách tìm** giá trị"* — khiến **lần chạy sau
> tốn 0 lượt gọi LLM để đọc rule**. Đó là tối ưu chi phí ở tầng **kiến trúc**, không phải tầng hạ
> tầng, và nó là câu chuyện mạnh nhất bạn có cho vòng AI engineer.

---

## 7. Khi nào **không** nên self-host

Nói được phần này mới đủ tín hiệu Lead. Người mê công nghệ chỉ nói lúc nào nên.

| Tình huống | Vì sao nên ở lại cloud |
|---|---|
| **Dưới điểm hoà vốn** | GPU rảnh vẫn tốn tiền. Dưới ~vài trăm triệu token/tháng, API gần như chắc chắn rẻ hơn |
| **Không có người vận hành chuyên trách** | Người chiếm 25–30% TCO. Không có người thì không có self-host, chỉ có nợ kỹ thuật |
| **Cần đúng năng lực của model hàng đầu** | Với reasoning khó hoặc tuân thủ schema ngặt, khoảng cách vẫn còn thật — bạn **đã đo** thấy điều đó |
| **Tải bùng nổ, không đều** | GPU phải cấp cho đỉnh; API co giãn tự nhiên |
| **Chưa có eval harness** | Không đo được thì không biết chuyển xong chất lượng còn hay mất. **Dựng eval trước, chuyển sau** |

> **Câu trả lời hoàn chỉnh nhất cho "nên self-host không":** *"Câu trả lời của tôi là **hybrid**,
> và ranh giới không nằm ở model — nằm ở **loại việc**. Việc chạy thường xuyên, đầu ra có schema
> chặt, dữ liệu nhạy cảm thì tự host: đó là phần chiếm hầu hết lưu lượng và hưởng trọn lợi ích độ
> trễ. Việc chạy hiếm, reasoning khó, chấp nhận trễ hơn thì gọi cloud. Trong hệ của tôi ranh giới
> đó rơi đúng vào chỗ: Claude chạy **offline một lần** để build rulebook, Qwen tự host chạy
> **runtime cho mọi request**."*

Đó không phải câu trả lời trung dung cho an toàn — đó là **kiến trúc thật đang chạy trong hệ của
bạn**, và bạn có số đo để bảo vệ nó.

---

## 8. Câu trả lời phỏng vấn + 10 follow-up

### 8.1 Kể theo STAR — 90 giây

> **S —** *"Hệ AI của tôi ban đầu gọi model qua API bên thứ ba. Bài toán là chi phí suy luận tăng
> theo lưu lượng, và quan trọng hơn: dữ liệu nghiệp vụ — techpack, BOM của khách — phải rời hạ tầng
> của mình."*
>
> **T —** *"Chuyển sang tự host mà không được để chất lượng tụt, và phải quay lại cloud được ngay
> nếu hỏng."*
>
> **A —** *"Ba bước. Một, tầng ứng dụng chỉ nói **một giao thức OpenAI-compatible** — nên đổi
> provider chỉ là đổi một biến môi trường, và đường lui luôn còn. Hai, **đo trước khi chuyển**: tôi
> chạy Qwen tự host đối chiếu Claude trên chính golden set của mình. Trên tác vụ có schema ràng
> buộc, Qwen khớp Claude **100% action, 27/27**. Trên tác vụ trích rule tự do thì Claude vẫn hơn —
> nên tôi **không** chuyển phần đó. Ba, chọn serving stack theo tải: bắt đầu bằng llama.cpp cho
> nhanh, rồi migrate sang **vLLM** khi cần throughput, giữ nguyên `served-model-name` nên app không
> phải sửa dòng nào."*
>
> **R —** *"Hiện toàn bộ suy luận runtime chạy trên hạ tầng nội bộ. Cái giá tôi trả và đã ghi lại:
> model OSS tuân thủ JSON kém hơn — có một commit sửa đúng lỗi đó với Qwen 3.6 35B. Hướng xử lý
> đúng là **constrained decoding ở tầng serving**, chứ không phải retry thêm ở tầng ứng dụng."*

### 8.2 Follow-up

<details>
<summary><b>Nhóm A — kiến trúc và kinh tế (4 câu)</b></summary>

**A1. "Self-host rẻ hơn bao nhiêu?"**
> Tuỳ bạn đứng ở đâu so với điểm hoà vốn. Cloud là chi phí tuyến tính từ 0; self-host gần như cố
> định. Số tham khảo 2026 là hoà vốn quanh **600 triệu – 1,2 tỉ token/tháng**, và mỗi triệu token
> trên phần cứng tự host có thể rẻ hơn khoảng 10×. Nhưng tôi sẽ không mở đầu bằng chi phí — GPU rảnh
> vẫn tốn tiền, và **người vận hành chiếm 25–30% TCO**, thứ mà phần lớn phân tích bỏ quên. Lý do
> vững hơn và đúng ngay từ token đầu tiên là độ trễ, quyền kiểm soát dữ liệu, và không bị đổi model
> dưới chân.

**A2. "Làm sao chuyển mà không khoá mình vào một hướng khác?"**
> Giữ tầng ứng dụng chỉ biết một giao thức OpenAI-compatible. Cloud, self-host, hay chạy song song
> A/B đều thành cấu hình. Trong hệ của tôi biến vẫn tên `OPENAI_*` nhưng trỏ vào một IP nội bộ —
> code không biết và không cần biết. Quan trọng nhất là **đường lui vẫn còn**: không có nó thì không
> ai dám chuyển.

**A3. "Chọn model nào?"**
> Tôi không chọn theo bảng xếp hạng chung — tôi chọn theo **golden set của chính bài toán**. Tôi đã
> đo Qwen tự host với Claude và kết quả không đồng đều: trên tác vụ schema ràng buộc thì khớp 100%,
> trên trích rule tự do thì Claude ra nhiều rule hơn. Nên tôi tự host phần thứ nhất và giữ cloud cho
> phần thứ hai. Không có eval harness thì mọi lựa chọn model đều là cảm tính.

**A4. "Bao nhiêu GPU thì đủ?"**
> Không trả lời được nếu chưa biết ba số: token/tháng, tỉ lệ prefill/decode, và p99 mục tiêu. VRAM =
> trọng số + KV cache, mà KV cache tỉ lệ với context length nhân batch — nên `max-model-len` thường
> là nút vặn trước khi tính tới thêm GPU. Tôi sẽ benchmark trên chính traffic thật rồi mới cấp phần
> cứng.
</details>

<details>
<summary><b>Nhóm B — vận hành và chất lượng (4 câu)</b></summary>

**B1. "Model tự host trả JSON sai thì sao?"**
> Đó là cái giá điển hình nhất của self-host, và tôi có commit sửa đúng lỗi đó. Bốn lớp, theo thứ
> tự: **constrained decoding** ở tầng serving để model không thể sinh token phá schema — đây là lớp
> đúng nhất và rẻ nhất; rồi chuẩn hoá đầu ra, bóc khối `<think>` của Qwen; rồi retry có đưa lỗi
> parse vào làm observation; cuối cùng escalate `NEEDS_REVIEW` chứ không đoán bừa. Sai lầm phổ biến
> là chỉ làm lớp 3 và tưởng đã xử lý.

**B2. "Đo gì để biết hệ đang khoẻ?"**
> Tokens/s **tách riêng prefill và decode** vì hai pha khác đặc tính hẳn; GPU util và VRAM đã cấp;
> độ dài hàng đợi và thời gian chờ xếp lịch; **TTFT** vì đó là cái người dùng cảm nhận; p99
> end-to-end theo từng loại request; và một metric riêng của self-host là **tỉ lệ lỗi parse schema**.

**B3. "llama.cpp hay vLLM?"**
> llama.cpp để dựng nhanh và chạy trên VRAM nhỏ; **vLLM cho production** vì PagedAttention và
> continuous batching cho throughput cao hơn hẳn khi nhiều request đồng thời. Tôi đã đi đúng đường
> đó — bắt đầu GGUF, rồi migrate sang vLLM FP8. Một lưu ý với VLM: vLLM không map 1:1 với `--mmproj`
> của llama.cpp, phải dùng checkpoint HF của model.

**B4. "Fine-tune có đáng không?"**
> Chỉ sau khi ba bậc rẻ hơn đã cạn: bỏ bớt lượt gọi LLM, cắt đầu vào, và batching. LoRA đáng làm khi
> prompt đã dài mà vẫn phải lặp lại cùng một hướng dẫn, hoặc khi muốn model nhỏ đạt chất lượng model
> lớn trên **một** tác vụ hẹp. Tôi có tài liệu quyết định prompt vs RAG vs fine-tune, nhưng **chưa
> train** — và tôi nghĩ biết khi nào *chưa* nên fine-tune cũng quan trọng ngang việc biết cách làm.
</details>

<details>
<summary><b>Nhóm C — hợp với Katalon (2 câu)</b></summary>

**C1. "Áp vào TrueTest thì làm thế nào?"**
> Thuận lợi là TrueTest đã cho phép dùng **OpenAI-compatible API key** — nghĩa là seam đã có sẵn,
> việc còn lại là dựng và vận hành đầu bên kia. Tôi sẽ chia theo loại việc: phần **chạy thường
> xuyên và có schema chặt** — trích locator, phân loại bước journey, sinh assertion — tự host, vì đó
> là phần chiếm hầu hết lưu lượng và hưởng trọn lợi ích độ trễ. Phần reasoning khó, chạy hiếm thì
> giữ cloud. Và với TrueTest có một lý do mạnh hơn chi phí: **nó ghi lại phiên của người dùng thật
> của khách hàng**. Giữ DOM đó trong hạ tầng của mình là bài toán compliance, không phải bài toán
> hoá đơn.

**C2. "Rủi ro lớn nhất khi chuyển là gì?"**
> Không phải hạ tầng — là **chuyển xong mới phát hiện chất lượng tụt**. Nên thứ tự bắt buộc là: dựng
> eval harness trước, đo baseline trên cloud, rồi mới chuyển và so lại trên **cùng** golden set.
> Chuyển trước rồi đo sau thì lúc phát hiện lệch đã không còn biết mất từ đâu. Và luôn giữ đường lui
> về cloud bằng một biến môi trường.
</details>

---

## 9. Kế hoạch 2 tuần

Chỉ phần **chưa có**. Những gì đã chạy thật thì luyện kể, không làm lại.

### Tuần 1 — bịt hai lỗ hổng kỹ thuật

| Ngày | Việc | Xong là gì |
|:---:|---|---|
| 1 | Đọc lại [§1](#1-bằng-chứng-bạn-đã-làm-đúng-việc-họ-đang-tuyển), mở đúng file `.env` và `docker-compose.llm.vllm.yml` | Trích được **file:line** thay vì kể chung chung |
| 2–3 | **Bật constrained decoding trên vLLM** (guided JSON) cho một endpoint thật, đo tỉ lệ lỗi parse trước/sau | Con số **của chính bạn** cho lớp 1 ở [§5.1](#51-model-yếu-hơn-làm-vỡ-structured-output) |
| 4 | Dựng **Prometheus + metric vLLM**, đọc được 6 metric ở [§5.2](#52-vận-hành-mà-cloud-api-lo-hộ-bạn) | Nói được TTFT và độ dài hàng đợi thật của hệ mình |
| 5 | Benchmark throughput: batch 1 vs batch 8 vs 32, cùng phần cứng | Chứng minh continuous batching bằng số, không bằng lời |
| 6–7 | **Bảng TCO của chính bạn**: token/tháng thật × đơn giá API vs chi phí GPU đang dùng | Biết mình đang ở **bên nào** của điểm hoà vốn |

### Tuần 2 — biến thành câu chuyện

| Ngày | Việc | Xong là gì |
|:---:|---|---|
| 1 | Luyện STAR ở [§8.1](#81-kể-theo-star--90-giây) — 90 giây, không nhìn giấy | Câu chuyện chính đã sẵn |
| 2 | Luyện 10 follow-up ở [§8.2](#82-follow-up) | Chịu được đào sâu |
| 3 | Bản **tiếng Anh** của STAR (Katalon là công ty Mỹ, HQ Atlanta) | Dùng chung khung với [nab-prep/05](../nab-prep/05-english-interview.md) |
| 4 | Vẽ **sơ đồ migration** lên giấy: seam, hai backend, đường lui, eval gate | Vẽ được trong 3 phút lên bảng |
| 5 | Đọc `motivesidp-bom-agentic/docs/TUTORIAL_RFC_LORA.md` — bảng prompt vs RAG vs fine-tune | Trả lời B4 mà không phải đoán |
| 6 | Chuẩn bị **câu hỏi hỏi lại**: TrueTest tự host tới đâu rồi, ai đang vận hành, eval harness thế nào | Hỏi đúng chỗ là tín hiệu |
| 7 | Mock 45 phút: *"thiết kế lộ trình chuyển TrueTest sang self-host"* | Diễn tập |

**Nếu chỉ còn 3 ngày:** làm ngày 1 và ngày 6–7 của tuần 1 (dẫn chứng + bảng TCO), rồi ngày 1–2 của
tuần 2 (STAR + follow-up). Bỏ phần còn lại.

---

## 10. Ranh giới trung thực

Đọc kỹ mục này. File này có **một điểm yếu về nguồn** cần biết rõ.

| Điều | Trạng thái | Được nói gì |
|---|---|---|
| **Katalon pivot sang AI Engineer + self-host** | 🔴 **Nguồn là bạn, tôi KHÔNG tra được** | Tra ITviec ngày 06/09/2026 chỉ thấy **Product Security Engineer**. Không có JD AI Engineer công khai. **Lấy JD nguyên văn từ nguồn của bạn trước khi tin toàn bộ file này** |
| 12 bằng chứng ở [§1](#1-bằng-chứng-bạn-đã-làm-đúng-việc-họ-đang-tuyển) | ✅ **Đọc trực tiếp** từ `.env`, `docker-compose`, git log | ✅ Dẫn chứng được, có file cụ thể |
| Số đo Qwen vs Claude (100% action, 27/27) | ✅ Đã đo, có trong [AI-STACK §7.3](AI-STACK-INTERVIEW-ANSWERS.md) | ✅ Con số thật của bạn |
| **Số kinh tế 2026** ở [§3](#3-kinh-tế-học-khi-nào-self-host-thật-sự-rẻ-hơn) | 🟠 **Nguồn ngoài, bạn chưa đo** | 🟠 Nói *"theo các phân tích 2026"*, **không** nói *"tôi đo được"*. Bảng TCO của chính bạn là việc của tuần 1 |
| TrueTest chấp nhận OpenAI-compatible API key | 🟡 Từ tài liệu bên thứ ba về Katalon Studio | 🟡 Nên xác nhận lại với người phỏng vấn — và **hỏi thẳng là một câu hỏi hay** |
| Constrained decoding chưa bật | 🔴 **Chưa làm** | 🔴 Nói là *"hướng xử lý đúng"* và *"việc tiếp theo tôi làm"*, **không** nói đã làm |
| Prometheus / 6 metric | 🔴 **Chưa dựng** | 🔴 Là danh sách nên đo, chưa phải số đang có |
| LoRA | 🟡 Có tài liệu quyết định, **chưa train** | 🟡 Nói thẳng — và nêu lý do *chưa* nên fine-tune |
| Speculative decoding | 🔴 Chưa dùng | 🔴 Biết khái niệm, không có kinh nghiệm |

> **Câu an toàn khi bị hỏi sâu về chi phí:**
> *"Tôi có kiến trúc và có số đo chất lượng, nhưng bảng TCO đầy đủ thì tôi chưa dựng — hệ của tôi
> chạy trên phần cứng nội bộ nên chi phí trên mỗi token chưa được quy đổi ra tiền. Nếu vào đây, việc
> đầu tiên tôi làm là đo cái đó, vì không có nó thì mọi quyết định self-host đều là niềm tin."*

---

## Liên quan

| Tài liệu | Liên quan chỗ nào |
|---|---|
| [AI-STACK-INTERVIEW-ANSWERS.md §7.3](AI-STACK-INTERVIEW-ANSWERS.md) | **Số đo Qwen vs Claude** — bằng chứng chất lượng cho toàn bộ file này |
| [AI-STACK-INTERVIEW-ANSWERS.md §7.1](AI-STACK-INTERVIEW-ANSWERS.md) | Rulebook cache — bậc 1 của [§6](#6-bảy-bậc-giảm-chi-phí-xếp-theo-tỉ-lệ-hoàn-vốn), tối ưu chi phí ở tầng kiến trúc |
| [katalon-senior-lead-phong-van.md §1.3b](katalon-senior-lead-phong-van.md) | Hướng tuyển AI Engineer và bản đồ năng lực đã cập nhật |
| [katalon-system-design/01](katalon-system-design/01-truetest-journey-mining.md) | TrueTest — nơi mô hình tự host sẽ chạy |
| [nab-prep/05](../nab-prep/05-english-interview.md) | Khung luyện nói tiếng Anh, dùng chung cho bản EN của STAR |
