# 15 · Nền tảng cần nắm — bản đồ 8 nhóm

> **Câu hỏi file này trả lời:** sau khi đọc [`token_gpu_optimization_research.md`](../../../motivesidp-ai-service/docs/token_gpu_optimization_research.md)
> (1.700+ dòng, 15 mục), **thật sự cần nắm những gì** để làm được nó — và nhóm nào trong 16 file
> trước đã có, nhóm nào còn thiếu.
>
> **Thời lượng:** 45 phút đọc (đây là file bản đồ, không phải file học). **Yêu cầu trước:** không.

---

## 0. Kết luận trước, lý lẽ sau

Tài liệu research đó đề xuất khoảng 25 việc. Đếm lại xem mỗi việc cần kiến thức nền nào:

```
                   Số việc cần nhóm kiến thức đó
   Đo lường/thực nghiệm  ████████████████████  ~20/25
   Inference & hệ thống  ██████████████        ~14/25
   Information Retrieval ████████              ~8/25
   CV cổ điển            ██████                ~6/25
   Data engineering      ██████                ~6/25
   Kiến trúc LLM system  █████                 ~5/25
   Observability         █████                 ~5/25
   ─────────────────────────────────────────────────
   ML/DL thật sự         ████                  ~4/25   ← chỉ 1 trong 8 nhóm
```

> **Điều này không có nghĩa ML/DL không quan trọng.** Nó có nghĩa: **ML/DL là tầng 4, không phải
> tầng 1.** Học nó trước khi nắm 7 nhóm kia thì sẽ train được model mà không biết nó có tốt hơn
> không, chạy ở đâu, và lấy data từ đâu — đúng tình trạng mà tài liệu research mô tả.

Đối chiếu với [`00-chan-doan-nang-luc.md`](00-chan-doan-nang-luc.md): chẩn đoán ở đó là "bạn ở tầng
orchestration, thiếu tầng bên trong model". **File này bổ sung một đính chính quan trọng:** phần lớn
thứ đang chặn công việc thật **không nằm bên trong model** — nó nằm ở đo lường, retrieval, CV cổ
điển và kỹ thuật hệ thống. Tầng bên trong model vẫn cần, nhưng nó là đích cuối, không phải bước kế.

---

## 1. Bảng 8 nhóm — học ở đâu

| # | Nhóm | File | Trạng thái |
|---|---|---|---|
| **1** | Phương pháp thực nghiệm & thống kê | [09 · Eval và đo lường](09-eval-va-do-luong.md) · [01 · Nền tảng toán](01-nen-tang-toan.md) | ✅ **Đã đủ** — 09 có golden set, CI, calibration, shadow mode |
| **2** | Kỹ thuật inference & hệ thống | [16 · Inference internals](16-inference-internals.md) 🆕 · [10 · Tối ưu token](10-toi-uu-token-chi-phi.md) | ⚠️ 10 có prefix caching, **thiếu prefill/decode, MoE, active params** → file 16 |
| **3** | Observability / tracing | [11 · MLOps & serving](11-mlops-serving.md) | ⚠️ Có nhắc Langfuse, **thiếu khái niệm tracing & gate chuẩn hoá** → xem [16 §6](16-inference-internals.md) |
| **4** | Information Retrieval | [07 · Embedding, retrieval, RAG](07-embedding-retrieval-rag.md) | ✅ Đã có nDCG/MRR/hard negative — **thiếu Matryoshka** → xem [07](07-embedding-retrieval-rag.md) + [16 §7](16-inference-internals.md) |
| **5** | Computer Vision **cổ điển** | [17 · CV cổ điển](17-cv-co-dien.md) 🆕 | ❌ **Thiếu hoàn toàn** — 04 chỉ có CV deep learning (conv, CNN, transfer learning) |
| **6** | Data engineering cho ML | [18 · Data lineage & thu nhãn](18-data-lineage-va-thu-nhan.md) 🆕 | ❌ **Thiếu** — 02/08 có nhắc rò rỉ dữ liệu, chưa có lineage/versioning/thu nhãn |
| **7** | Thiết kế hệ thống có LLM | [19 · Hệ thống có LLM](19-he-thong-co-llm.md) 🆕 · [05 · NLP](05-nlp.md) | ⚠️ 05/10 có constrained decoding, **thiếu tool contract, agent loop, fallback, injection** |
| **8** | ML/DL | [02](02-machine-learning-co-ban.md) · [03](03-deep-learning-co-ban.md) · [08 · Fine-tuning](08-finetuning.md) | ✅ Đã đủ — và **chỉ cần 5 kỹ thuật**, xem §3 |

---

## 2. Thứ tự học đề xuất — và vì sao

```
   2 ──► 1 ──► 4 ──► 5 ──► 6 ──► 7 ──► 3 ──► 8
   │     │     │     │     │     │     │     │
   │     │     │     │     │     │     │     └─ ML/DL: tới đây mới biết cần train gì
   │     │     │     │     │     │     └─ Observability: cần khi hệ đã chạy nhiều luồng
   │     │     │     │     │     └─ LLM system design: cần khi xây chat agent (research §15)
   │     │     │     │     └─ Data engineering: cần khi bắt đầu thu nhãn
   │     │     │     └─ CV cổ điển: gỡ được 2 nút "sai công cụ" lớn nhất
   │     │     └─ IR: chỗ có bug thật + ROI fine-tune cao nhất
   │     └─ Thực nghiệm: học tự nhiên khi phải đo kết quả của bước 2
   └─ Inference: cho kết quả đo được NGAY TUẦN ĐẦU
```

**Vì sao bắt đầu ở (2) chứ không phải (1)?** Về logic thì (1) đo lường là nền của mọi thứ. Nhưng về
động lực học thì (2) thắng: bật prefix caching và đo lại prefill là việc làm được trong một buổi,
cho con số thay đổi ngay, và **bắt buộc** phải biết đo — nên nó tự kéo (1) theo. Học (1) trước theo
kiểu sách vở thì khô và dễ bỏ.

**Vì sao (8) ML/DL ở cuối?** Vì tới lúc đó bạn đã biết **chính xác** cần train cái gì (5 kỹ thuật ở
§3), đo bằng gì (nhóm 1), lấy data ở đâu (nhóm 6), và chạy nó ở đâu (nhóm 2). Train mà thiếu bốn thứ
đó là train mù.

---

## 3. Nhóm 8 — chỉ cần 5 kỹ thuật, không cần hơn

Đây là phần dễ gây lo lắng nhất ("phải học cả deep learning à?"). Thực tế, toàn bộ 6 hạng mục
fine-tune trong tài liệu research chỉ dùng **5 kỹ thuật**:

| Kỹ thuật | Dùng ở đâu | Học ở |
|---|---|---|
| **Transfer learning / frozen backbone + linear probe** | B1 sketch classifier — train trên **CPU**, vài phút | [04 §4](04-computer-vision.md) |
| **LoRA / PEFT** | B5 reranker, B6 distill | [08 §3](08-finetuning.md) |
| **Knowledge distillation** | B6 — dùng output của model 35B làm nhãn cho model 4B | [08](08-finetuning.md) |
| **Contrastive / metric learning** | B4 projection head, fine-tune FashionCLIP | [04 §7](04-computer-vision.md) · [07](07-embedding-retrieval-rag.md) |
| **Calibration** | **Mọi ngưỡng fallback** trong research đều phụ thuộc cái này | [09 §6](09-eval-va-do-luong.md) |

Kỹ thuật thứ 5 hay bị bỏ qua nhất nhưng lại quan trọng nhất về mặt vận hành: mọi model nhỏ trong
research đều có dạng *"nếu confidence < ngưỡng thì rơi về VLM 35B"*. Ngưỡng đó **vô nghĩa nếu
confidence chưa được calibrate** — một model có thể trả 0.95 cho thứ nó đoán bừa.

### Không cần đào sâu

Train transformer từ đầu · nghiên cứu kiến trúc mới · chạy theo model vừa ra · lý thuyết tối ưu hoá.
Không có việc nào trong tài liệu research cần tới chúng.

---

## 4. Tự kiểm — 8 câu, mỗi câu một nhóm

Trả lời được không cần tra cứu thì coi như nắm nhóm đó. Trả lời sai → đọc file tương ứng.

| # | Câu hỏi | Nhóm |
|---|---|---|
| 1 | Vì sao tune 5 trọng số scoring trên ~150 query rồi báo cáo trên chính tập đó là sai? Sai kiểu gì, hậu quả ra sao? | 1 |
| 2 | Model `Qwen3.6-35B-A3B` có 35B tham số. Vì sao đổi sang một model **dense 8B** lại **chậm hơn**? | 2 |
| 3 | Đoạn `RunnableConfig(run_name="vlm_x")` mà không truyền `callbacks` — nó trace được gì? | 3 |
| 4 | `vector[:128]` trên `bge-m3` là bug. Trên model nào thì **không** phải bug, và vì sao? | 4 |
| 5 | Đếm nút áo trên bản vẽ line-art: kể 2 cách làm **không dùng** model ngôn ngữ nào | 5 |
| 6 | Bạn có 20 techpack + BOM đúng. Chia train/test thế nào để **không** rò rỉ? Nêu 2 kiểu rò rỉ có thể xảy ra | 6 |
| 7 | Vì sao "để LLM sinh lại cả file BOM" là sai **về kiến trúc**, chứ không phải vì model chưa đủ giỏi? | 7 |
| 8 | Model classifier trả `confidence=0.95`. Bạn đặt ngưỡng fallback ở 0.85. Cần kiểm tra gì trước khi tin con số 0.95? | 8 |

**Đáp án:** nằm rải trong các file tương ứng. Câu 2 ở [16 §3](16-inference-internals.md#3-moe-vs-dense--vì-sao-35b-có-thể-rẻ-hơn-8b),
câu 4 ở [16 §7](16-inference-internals.md#7-matryoshka--vì-sao-cắt-vector-đôi-khi-được-đôi-khi-hỏng),
câu 5 ở [17](17-cv-co-dien.md), câu 6 ở [18](18-data-lineage-va-thu-nhan.md),
câu 7 ở [19](19-he-thong-co-llm.md).

---

## 5. Bài tập

| # | Bài | Tiêu chí đạt | Ước lượng |
|---|---|---|---|
| 1 | Tự chấm 8 câu ở §4, ghi lại câu nào không trả lời được | 1 danh sách trung thực | 30m |
| 2 | Đọc `token_gpu_optimization_research.md` §2, tự tính lại ngân sách token cho **một** style bằng số của riêng bạn | Ra được con số + nêu rõ giả định | 2h |
| 3 | Với mỗi việc trong research §4 (A1–A7), ghi xem nó cần nhóm kiến thức nào | Bảng 7 dòng | 1h |
| 4 | Chọn **một** nhóm bạn yếu nhất, đọc file của nó, làm bài tập đầu tiên trong đó | Xong 1 bài tập thật | 4-6h |

**Bài 2 làm trước** — nó cho biết bạn đang đứng ở đâu, và nó là số cần có để làm mọi việc còn lại.

---

## 6. Ranh giới trung thực

- **Đọc từ code, đã verify:** bảng §1 về "file nào đã có gì" dựng bằng cách grep từ khoá
  (`Hough`, `prefill`, `MoE`, `Matryoshka`, `lineage`, `tool calling`…) trên toàn bộ 16 file ngày
  17/09/2026. Các ô ghi ❌ là **thật sự không xuất hiện**, không phải tôi đoán.
- **Là ước lượng của tôi, chưa đo:** biểu đồ đếm việc ở §0 (~20/25, ~14/25…) là tôi tự phân loại
  25 đề xuất trong research theo nhóm kiến thức. Cách phân loại khác có thể ra tỉ lệ khác — nhưng
  thứ hạng (đo lường > inference > IR > … > ML/DL) thì khó đảo.
- **Là quan điểm, không phải sự thật:** thứ tự học `2 → 1 → 4 → 5 → 6 → 7 → 3 → 8` là đề xuất dựa
  trên động lực học và tình trạng cụ thể của `motivesidp-ai-service`. Ở dự án khác, thứ tự khác có
  thể đúng hơn.
- **Không bàn tới:** kỹ năng mềm, phỏng vấn, portfolio — xem [13](13-lo-trinh-ai-engineer-vsf-fpt.md).
