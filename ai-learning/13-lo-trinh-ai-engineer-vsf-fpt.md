# 13 · Lộ trình lên AI Engineer (FPT / VSF / tier-1 VN)

> ⚠ **Đọc trước:** tôi **không xác định được "VSF" trong yêu cầu của bạn là công ty nào.**
> Tìm kiếm cho ra "VinSmart Future (VSF)" và một công ty tên VSF tuyển Software Engineer ở Hà Nội,
> nhưng tôi không có bằng chứng đó là nơi bạn nhắm tới. File này viết theo **JD AI Engineer điển
> hình ở tier-1 Việt Nam** (FPT Software AI Center, FPT AI, VNPT AI, VinAI, Viettel AI).
> **Gửi tôi JD thật thì tôi viết lại chính xác phần gap và câu hỏi phỏng vấn.**

---

## 1. Ba nhánh "AI Engineer" ở VN — bạn đang ứng tuyển nhánh nào

Tên gọi giống nhau, nội dung công việc khác hẳn. Nhầm nhánh là nguyên nhân trượt phổ biến nhất.

```
  ┌────────────────────────┬────────────────────────┬────────────────────────┐
  │ A · GenAI / LLM        │ B · ML/DL ENGINEER     │ C · RESEARCH           │
  │    ENGINEER            │                        │    ENGINEER/SCIENTIST  │
  ├────────────────────────┼────────────────────────┼────────────────────────┤
  │ RAG, agent, prompt,    │ Train/fine-tune model, │ Paper, kiến trúc mới,  │
  │ tích hợp, serving      │ CV/NLP chuyên sâu,     │ SOTA benchmark         │
  │                        │ data pipeline          │                        │
  │                        │                        │                        │
  │ Toán: trung bình       │ Toán: cao              │ Toán: rất cao          │
  │ Kỹ sư: RẤT cao         │ Kỹ sư: cao             │ Kỹ sư: trung bình      │
  │ Bằng cấp: không cần    │ Bằng: ưu tiên Thạc sĩ  │ Bằng: TS/Thạc sĩ mạnh  │
  ├────────────────────────┼────────────────────────┼────────────────────────┤
  │ ✅ BẠN ĐÃ Ở ĐÂY        │ ← MỤC TIÊU 12 THÁNG    │ không khuyến nghị      │
  │    và khá mạnh         │                        │ (đổi ngành, 3-5 năm)   │
  └────────────────────────┴────────────────────────┴────────────────────────┘
```

**Lời khuyên thẳng:** đừng cố nhảy sang C. Vị thế mạnh nhất của bạn là **A với năng lực B** —
người vừa ship được hệ thống vừa train được model. Kiểu người này hiếm hơn cả hai đầu và được
trả cao ở công ty product.

---

## 2. Giải phẫu JD điển hình — và bạn đứng ở đâu

Dưới đây là các cụm yêu cầu lặp đi lặp lại trong JD AI Engineer ở tier-1 VN:

| Cụm yêu cầu trong JD | Mức của bạn | Bằng chứng bạn có sẵn | Gap |
|---|---|---|---|
| Python, clean code, API | **Mạnh** | FastAPI, 267 file, kiến trúc phân tầng | — |
| Docker, CI/CD, deploy | **Mạnh** | 15 compose file, GitLab CI, Jetson | — |
| LLM integration, prompt engineering | **Mạnh** | 12 thư mục prompt có version, structured output | — |
| RAG, vector DB | **Khá** | Qdrant, multivector, rerank 2 tầng | Chưa đo recall@k |
| "ML/DL fundamentals" | **YẾU** ⚠ | — | **Chưa train model nào** |
| "PyTorch / TensorFlow" | **YẾU** ⚠ | — | **Không có code train trong repo** |
| "CV: detection, classification" | Trung bình | Dùng Docling, FashionCLIP | Dùng hộp đen, không train |
| "NLP: NER, classification, embedding" | Trung bình | bge-m3, reranker | Dùng, không train |
| Fine-tuning, LoRA | **YẾU** ⚠ | — | **Chưa có** |
| Model serving, optimization | **Mạnh** | vLLM, TEI, GGUF, AWQ, benchmark Jetson | — |
| Eval, metrics | Trung bình | Golden set, 18 run eval có kỷ luật | Chưa có CI, chưa breakdown |
| MLOps (tracking, registry) | Yếu | Langfuse | Chưa có MLflow/DVC |
| Toán: đại số, xác suất, tối ưu | Trung bình | — | Chưa chứng minh được |
| Giao tiếp, tài liệu | **Rất mạnh** | Tài liệu nội bộ của bạn tốt hơn đa số | — |

```
   HỒ SƠ CỦA BẠN — nhìn theo hình dạng

   Kỹ sư hệ thống  ████████████████████  ← rất mạnh, hiếm ở ứng viên AI
   LLM/GenAI       ████████████████      ← mạnh
   Serving/MLOps   ██████████████        ← mạnh
   Eval            ██████████            ← khá
   ML/DL nền       ████                  ← ĐÂY LÀ CHỖ BỊ LOẠI ⚠
   Fine-tune       ██                    ← ĐÂY LÀ CHỖ BỊ LOẠI ⚠
   Toán            ██████                ← đủ để không bị hỏi khó

   → Bạn KHÔNG cần trở thành researcher. Bạn cần NÂNG hai thanh ngắn
     lên mức "đã làm thật, có số" — đúng những gì file 12 tạo ra.
```

---

## 3. Kế hoạch 12 tháng

```
  THÁNG 1-3 · NỀN + THẮNG NHANH
  ├─ [01][02][03] toán → ML → DL          (đọc + bài tập)
  ├─ Project ①②③ của file 12              (có số thật)
  └─ Mốc: train được model đầu tiên, có 1 cải tiến production có số
                                │
  THÁNG 4-6 · CHUYÊN SÂU MIỀN
  ├─ [06][07] VLM + retrieval
  ├─ Project ④⑤⑥⑦
  └─ Mốc: 3 cải tiến production có số + có CI eval
                                │
  THÁNG 7-9 · FINE-TUNE — CHỨNG MINH NĂNG LỰC
  ├─ [08] + [04] hoặc [05] tùy nhánh chọn
  ├─ Project ⑧ hoặc ⑩ (tùy cổng ②)
  └─ Mốc: một model do CHÍNH BẠN train đang chạy production ⭐
                                │
  THÁNG 10-12 · ĐÓNG GÓI & RA THỊ TRƯỜNG
  ├─ Viết lại CV theo §4
  ├─ Portfolio công khai (§6)
  ├─ Ôn phỏng vấn (§5)
  └─ Mốc: nộp hồ sơ
```

**Mốc tháng 7–9 là mốc quyết định.** "Tôi đã fine-tune một embedding model trên dữ liệu nội bộ,
recall@5 tăng từ X lên Y với khoảng tin cậy không chồng lấn, và nó đang chạy production" —
câu đó đưa bạn qua vòng sàng lọc của **mọi** JD ở §2.

---

## 4. Viết lại CV — ba nguyên tắc

### 4.1 Số, không phải tính từ

| ❌ Viết thế này | ✅ Viết thế này |
|---|---|
| "Phát triển hệ thống AI xử lý tài liệu" | "Xây IDP pipeline sinh BOM tự động, 89.1% field accuracy [84.6–92.3] trên 256 ô golden" |
| "Tối ưu hiệu năng hệ thống" | "Giảm 62% token ảnh bằng cách hiệu chỉnh resolution theo đường cong accuracy, giữ accuracy trong khoảng nhiễu" |
| "Có kinh nghiệm fine-tuning" | "Fine-tune FashionCLIP bằng InfoNCE + hard negative mining trên 3.2k cặp sketch, recall@5 0.61→0.79" |
| "Làm việc với vector database" | "Thiết kế retrieval 2 tầng (bge-m3 + cross-encoder), recall@50 96%, MRR 0.71, p95 latency 180ms" |

### 4.2 Nêu cả đánh đổi

Câu *"tôi chọn A thay vì B vì …, đánh đổi là …"* là dấu hiệu của **senior**. Không có nó, dù bạn
có bao nhiêu năm kinh nghiệm cũng nghe như mid-level.

### 4.3 Ba câu chuyện mạnh nhất bạn đã có

| # | Câu chuyện | Chứng minh năng lực gì |
|---|---|---|
| 1 | **"Tôi tự gỡ LLM ra khỏi đường mặc định"** (`enable_llm=False`) | Kỷ luật kỹ thuật, hiểu ranh giới tin tưởng, không sùng bái công nghệ |
| 2 | **"Đừng để LLM trả lời — để nó viết ra cách tìm câu trả lời"** (rulebook) | Tư duy kiến trúc gốc, đo được |
| 3 | **"Nghi phạm đầu tiên là verifier, không phải model"** | Kỹ năng chẩn đoán hệ thống — rất hiếm |

Ba câu chuyện này **đã mạnh sẵn**. Thứ chúng thiếu là một câu chuyện thứ tư: *"và tôi tự train
được model khi cần."* Đó là toàn bộ mục đích của 12 tháng ở §3.

---

## 5. Ôn phỏng vấn — bốn vòng điển hình

### 5.1 Vòng ML fundamentals

| Câu hỏi hay gặp | Ở đâu trong bộ tài liệu |
|---|---|
| Bias-variance tradeoff là gì? | [02](02-machine-learning-co-ban.md)§7 |
| Overfit — nhận biết và xử lý thế nào? | [02](02-machine-learning-co-ban.md)§7.1-7.2 |
| Precision vs recall — khi nào ưu tiên cái nào? | [02](02-machine-learning-co-ban.md)§6.2 |
| Giải thích backprop | [03](03-deep-learning-co-ban.md)§3 |
| Vanishing gradient — vì sao và sửa sao? | [03](03-deep-learning-co-ban.md)§5 |
| BatchNorm vs LayerNorm | [03](03-deep-learning-co-ban.md)§6 |
| Vì sao attention chia √d? | [05](05-nlp.md)§3.3 |
| Xử lý dữ liệu lệch lớp | [02](02-machine-learning-co-ban.md)§6.3 |
| Cross-validation — khi nào GroupKFold? | [02](02-machine-learning-co-ban.md)§2.2 |

### 5.2 Vòng coding

- **DSA:** bạn đã có [leetcode-38-bai](../leetcode-38-bai/) trong workspace này. Dùng lại.
- **ML coding:** thường là "cài logistic regression / k-means / attention bằng numpy", hoặc
  "viết training loop PyTorch". → [03](03-deep-learning-co-ban.md)§3, §4.
- **Data:** pandas/numpy thao tác dữ liệu, xử lý missing value.

### 5.3 Vòng system design — vũ khí mạnh nhất của bạn

Đề bài thường gặp: *"Thiết kế hệ thống trích xuất thông tin từ tài liệu quy mô lớn"* — chính là
hệ bạn đang làm.

```
  Khung trả lời (12 phút):

  ① Làm rõ yêu cầu (2p)   quy mô? độ trễ? chi phí lỗi? ai là người dùng cuối?
  ② Kiến trúc cao (3p)    ingest → layout → extract → retrieve → map → verify → export
  ③ Đi sâu 1-2 chỗ (4p)   ⭐ CHỖ BẠN TỎA SÁNG: retrieval 2 tầng, ranh giới tin tưởng
  ④ Đo & vận hành (2p)    golden set, CI gate, shadow mode, drift
  ⑤ Đánh đổi (1p)         VLM vs OCR+rule · self-host vs API · precision vs tự động hóa
```

**Ba chi tiết sẽ khiến bạn nổi bật:**
- Nói về **khoảng tin cậy**, không chỉ accuracy → rất ít ứng viên làm.
- Nói về **chi phí token có số cụ thể** → chứng tỏ đã vận hành thật.
- Nói **"tôi để LLM ra khỏi đường mặc định"** → chứng tỏ có kỷ luật kỹ thuật.

### 5.4 Vòng behavioral

Chuẩn bị theo STAR cho 4 tình huống: một lần bạn **tự bác bỏ** giả thuyết của chính mình, một lần
bạn **chọn giải pháp đơn giản hơn**, một lần bạn **sai và sửa**, một lần bạn **thuyết phục được
team** bằng số liệu.

> Câu chuyện "vòng cung RFC/bakeoff — tự bác bỏ 3 lần rồi mở lại và thắng" trong tài liệu Katalon
> của bạn là câu chuyện behavioral **rất mạnh**. Dùng lại nó.

---

## 6. Portfolio công khai

Không ai thuê bạn vì repo nội bộ họ không xem được. Cần **2–3 thứ công khai**:

| Loại | Gợi ý cụ thể | Công sức |
|---|---|---|
| **Blog kỹ thuật** ⭐ | 3 bài từ file 12: "Đo separation của embedding trước khi fine-tune" · "Token ảnh của VLM: cần gạt bị bỏ quên" · "Vì sao accuracy không CI là con số vô nghĩa" | 2 ngày/bài |
| Repo demo | Phiên bản đã ẩn danh của một project trong file 12 | 1 tuần |
| Đóng góp open source | Issue/PR nhỏ cho Qdrant client, docling, sentence-transformers | tùy |
| Nói ở meetup | GDG/AI meetup Đà Nẵng/HCM | 1 tuần chuẩn bị |

**Blog có ROI cao nhất** — nó chứng minh cả năng lực kỹ thuật lẫn khả năng diễn đạt, và tài liệu
bạn viết trong repo cho thấy bạn viết rất tốt. Ba bài trên đều có thể viết **mà không tiết lộ gì
của khách hàng** (dùng số đã ẩn danh hoặc dữ liệu tự sinh).

---

## 7. Bốn sai lầm cần tránh

| Sai lầm | Vì sao hỏng | Thay bằng |
|---|---|---|
| Học lý thuyết 6 tháng rồi mới làm | Quên hết, không có bằng chứng | Học **cùng lúc** với project ở file 12 |
| Nói "có kinh nghiệm fine-tuning" khi mới chạy 1 notebook | Bị bóc trần trong 2 câu hỏi | Chỉ nói cái đã có **số và có CI** |
| Nhắm vị trí research | Cạnh tranh với TS, sở trường bạn bị bỏ phí | Nhắm **A với năng lực B** |
| Bỏ qua đo lường vì "không hào nhoáng" | Đây chính là thứ phân biệt senior | [09](09-eval-va-do-luong.md) là file ROI cao nhất |

---

## 8. Ranh giới trung thực

- **KHÔNG verify được:** "VSF" là công ty nào. Cảnh báo ở đầu file là thật, không phải hình thức.
  **Gửi JD thì tôi viết lại §2 và §5 chính xác cho vị trí đó.**
- **Đọc từ nguồn công khai, mức chi tiết thấp:** JD FPT Software/FPT AI yêu cầu ML/DL,
  TensorFlow/PyTorch, NLP framework; FPT có AI Center và AI Factory quy mô lớn. Nguồn:
  [fpt.com](https://fpt.com/en/news/fpt-news/fpt-software-ai-center),
  [ai.fpt-software.com](https://ai.fpt-software.com/our-team/),
  [itviec.com](https://itviec.com/it-jobs/ai-engineer),
  [vnptai.io](https://vnptai.io/vi/recruitment/job1).
- **Là tổng hợp/suy luận của tôi:** bảng §2 (các cụm yêu cầu JD) là mẫu hình chung tôi tổng hợp,
  **không phải trích nguyên văn** một JD cụ thể. Mức đánh giá của bạn trong bảng đó là nhận định
  của tôi từ việc đọc code — bạn có quyền không đồng ý.
- **Chưa kiểm chứng:** mức lương, quy trình phỏng vấn cụ thể, và các con số CV mẫu ở §4.1
  (chúng là **template để bạn điền**, không phải thành tích của bạn — đừng copy nguyên).
- **Là ý kiến:** khuyến nghị không nhắm nhánh research (§1). Hợp lý với hồ sơ hiện tại và mục
  tiêu 12 tháng, nhưng đó là quyết định nghề nghiệp của bạn, không phải của tôi.
