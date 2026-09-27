# 18 · Data lineage & thu nhãn — nơi dữ liệu train đến từ

> **Câu hỏi file này trả lời:** bạn cần 10.000 mẫu có nhãn để fine-tune, hiện có 12. Lấy ở đâu ra
> mà không phải thuê người ngồi label?
>
> **Thời lượng:** 8–10 giờ. **Yêu cầu trước:** không.
> **Bổ trợ:** [09 · Eval](09-eval-va-do-luong.md) nói cách *dùng* golden set; file này nói cách
> *tạo ra* nó một cách bền vững.

---

## 0. Bài toán thật của bạn

| Hạng mục | Data đang có | Cần tối thiểu | Tỉ lệ |
|---|---|---|---|
| Distill call LLM → model 4B | **12 run** trong `storage/history/` | 10k–100k mẫu | **~1/1000** |
| Trích xuất BOM toàn techpack | ~30 tài liệu có nhãn | ~300 | ~1/10 |
| Sketch similarity | 15 bundle, 2 subgroup | ≥500 style | ~1/30 |

Khoảng cách này **không đóng được bằng một sprint**. Nó chỉ đóng được bằng thời gian — và **chỉ khi
hôm nay đã có chỗ chứa**. Mỗi tháng chạy production mà không ghi nhãn là một tháng mất trắng, không
mua lại được.

> **Nguyên tắc trung tâm của file này:** dữ liệu train tốt nhất **không phải đi mua hay đi thuê
> label** — nó là **phó sản của công việc hàng ngày**, nếu bạn thiết kế hệ thống để giữ lại.

---

## 1. Lineage & provenance — biết một giá trị từ đâu ra

**Provenance** = với mỗi giá trị, ghi lại nó đến từ đâu, bằng cách nào, độ tin cậy bao nhiêu.
**Lineage** = chuỗi biến đổi từ dữ liệu gốc tới đầu ra cuối.

Dự án của bạn **đã làm tốt chuyện này** — và đây là tài sản quý nhất, quý hơn cả code:

```python
# provenance.py:158-174
make_provenance(
    source_type=SourceType.BASE_BOM,      # từ nguồn nào
    source_value="4900153745-VLTT1.xlsx", # cụ thể file/dòng nào
    confidence_type=ConfidenceType.HIGH,  # tin tới mức nào
    resolution_method="base_bom_pairing", # bằng cơ chế nào
)
```

Ba công dụng, theo thứ tự ít người nghĩ tới dần:

1. **Audit** — QC hỏi "sao dòng này ra mã này?" → trả lời được. (Ai cũng nghĩ tới.)
2. **Debug** — accuracy tụt, lọc theo `resolution_method` để biết bước nào hỏng. (Ít người dùng.)
3. **🔑 Training data** — biết giá trị nào do AI quyết, giá trị nào do người sửa ⇒ **cặp
   (AI sai, người đúng)** chính là mẫu train. (Gần như không ai thiết kế cho mục đích này từ đầu.)

### 1.1 Lỗ hổng cụ thể trong code hiện tại

[`constants.py:30-42`](../../../motivesidp-ai-service/motives/src/application_platform/constants/constants.py)
có 10 `SourceType`: `base_bom`, `trimlist`, `techpack`, `pnp`, `scs_trimlist`…

**Không có `USER_FEEDBACK`.**

Nghĩa là khi người dùng sửa một giá trị, hệ thống **không phân biệt được** với giá trị do AI sinh.
Thêm một enum là vài dòng code — nhưng nó tách được vĩnh viễn "AI quyết" khỏi "người quyết", và đó
đúng là nhãn cần cho mọi việc train sau này.

---

## 2. Nhãn ẩn — tìm dữ liệu train trong luồng nghiệp vụ sẵn có

Đây là kỹ năng quan trọng nhất của file. Công thức:

> **Tìm chỗ mà một người có chuyên môn đang ra quyết định, và quyết định đó đang bị vứt đi.**

### 2.1 Ví dụ thật trong repo của bạn

[`idp_pipeline.py:349`](../../../motivesidp-ai-service/motives/src/api/routes/idp_pipeline.py):

```python
@router.post("/idp/pipeline/re-export")
async def re_exporting_bom_excel(payload: IDPReExportingData):
    """Re-export BOM Excel using edited bom_lines from backend service."""
```

Đọc kỹ: endpoint này nhận **`bom_lines` đã được người sửa trên UI**, render ra Excel mới, rồi **quên**.

Mỗi lần merchandiser sửa một dòng BOM là service nhận đúng cặp *(AI sinh gì → người sửa thành gì)* —
nhãn đắt nhất có thể có, do người có chuyên môn tạo ra, trả bằng lương thật — và không lưu gì.

Hơn nữa, `BomLineSchema` còn mang theo hai trường vàng:

```python
field_sources: dict[str, Any]                  # giá trị này vốn từ đâu
alternative_materials: list[dict[str, Any]]    # các ứng viên ĐÃ ĐƯA RA cho người chọn
```

⇒ Nếu người dùng chọn một `alternative` thay vì ứng viên top-1 của AI, đó là **preference pair hoàn
chỉnh** `(query, positive, negative)` — đúng định dạng train cross-encoder reranker, không cần chế
biến gì thêm.

**Vấn đề duy nhất:** `IDPReExportingData` **không có `session_id`/`run_id`**, nên bản đã sửa không
nối ngược được về bản AI sinh. Thêm một trường + lưu lại cặp.

### 2.2 Tự tìm nhãn ẩn — checklist

| Câu hỏi | Nếu "có" thì đó là mỏ nhãn |
|---|---|
| Có chỗ nào người dùng **sửa** output của AI không? | → cặp sửa lỗi |
| Có chỗ nào người dùng **chọn** trong danh sách AI đưa ra không? | → preference pair (quý nhất) |
| Có chỗ nào người dùng **duyệt / từ chối** không? | → **tín hiệu phủ định** (hiếm nhất) |
| Có quyết định nghiệp vụ nào trong quá khứ **ngụ ý** đáp án đúng không? | → nhãn lịch sử, miễn phí |

Dòng cuối áp cho Team B: *"merchandiser đã thật sự tái dùng BOM của style nào?"* là một relevance
judgement do người thật đưa ra, đã trả bằng tiền. Mine cặp style có BOM overlap cao rồi đưa BA
confirm theo lô — rẻ hơn nhiều so với bắt họ chấm điểm tương đồng từ ảnh.

> **Tín hiệu phủ định là thứ đắt nhất.** "Người dùng từ chối đề xuất này" quý hơn "người dùng chấp
> nhận", vì nó hiếm và vì nó dạy model biên giới. Hầu hết hệ thống chỉ log cái được chấp nhận.

---

## 3. Event log — cách lưu cho đúng

Nguyên tắc: **append-only, không sửa quá khứ, một dòng một sự kiện.**

```json
{
  "event_type": "bom_line_correction",
  "team": "C",
  "session_id": "...",
  "techpack_sha256": "...",
  "row_id": "...",
  "field": "item_code",
  "ai_value": "RP-1019-3",
  "human_value": "RP-1019-7",
  "alternatives_shown": ["RP-1019-3", "RP-1019-7", "..."],
  "corrected_by": "...", "corrected_at": "2026-09-17T10:00:00Z",
  "prompt_version": "unified_bom_extraction_jacket@1.4",
  "model_id": "qwen36-35b-q5-vlm",
  "code_sha": "a1b2c3d"
}
```

### 3.1 Ba trường hay bị quên và sau này tiếc nhất

`prompt_version`, `model_id`, `code_sha`.

Không có chúng thì một năm sau nhìn vào dataset bạn **không tách được** "model dở" với "prompt đã
đổi" với "code đã sửa". Và khi thấy accuracy tụt ở tháng 7, bạn không truy được vì sao.

### 3.2 Vì sao append-only

Nếu sửa bản ghi cũ khi có thông tin mới, bạn mất khả năng tái lập trạng thái tại thời điểm T. Mà
tái lập là điều kiện bắt buộc để trả lời "model tháng 3 và model tháng 9 khác nhau ở đâu".

---

## 4. Dataset versioning

```
dsv-2026-Q3/
  ├── manifest.json      # checksum, số mẫu, khoảng thời gian, cách lọc
  ├── train.jsonl
  ├── val.jsonl
  └── test.jsonl         # ← KHÓA. Không ai được nhìn trong lúc phát triển.
```

**Quy tắc:**

1. Snapshot **bất biến**. Có data mới → tạo `dsv-2026-Q4`, không sửa Q3.
2. `manifest.json` ghi rõ **cách lọc** ("chỉ mẫu có `qa_status=APPROVED`") — nếu không, 6 tháng sau
   không ai tái lập được.
3. Model ghi lại nó train trên `dsv` nào. Không có thì không so sánh được hai model.

---

## 5. Rò rỉ dữ liệu (leakage) — bốn kiểu, kiểu 2 và 4 dễ dính nhất

Rò rỉ = thông tin từ tập test lọt vào tập train ⇒ metric đẹp, production thì hỏng.

| Kiểu | Ở dự án của bạn nghĩa là gì |
|---|---|
| **1. Chia theo dòng thay vì theo đơn vị** | Chia 650 dòng BOM ngẫu nhiên ⇒ dòng của cùng một style nằm cả hai bên. Phải chia **theo style**. |
| **2. 🔑 Rò rỉ phiên bản** | `VLTT1 v1` ở train, `VLTT1 v2` ở test — cùng style, khác revision. Gần như trùng khớp. **Phải chia theo style code, không phải theo file.** |
| **3. Rò rỉ thời gian** | Train trên data 2026, test trên 2025. Production luôn là "train quá khứ, dự đoán tương lai" — split phải theo thời gian. |
| **4. 🔑 Rò rỉ qua tiền xử lý** | Fit normalizer / mine hard negative / chọn ngưỡng **trên toàn bộ dữ liệu** rồi mới chia. Mọi thống kê phải tính **chỉ trên train**. |

Kiểu 4 đặc biệt nguy hiểm với công việc của bạn: mine hard negative từ 46.919 dòng material —
nếu mine trên cả tập rồi mới split thì negative của test đã "biết" về test.

**Kiểm nhanh:** nếu accuracy cao bất thường (>95% ở bài khó), giả định là rò rỉ cho tới khi chứng
minh ngược lại.

---

## 6. Distillation data — chỉ giữ mẫu đã được kiểm

Distill = dùng output của model lớn làm nhãn cho model nhỏ. Cái bẫy:

> **Distill từ output chưa QC là dạy model 4B sai y hệt model 35B, chỉ nhanh hơn.**

Quy tắc lọc, theo độ tin cậy giảm dần:

| Mức | Nguồn nhãn | Dùng được không |
|---|---|---|
| A | Người sửa và duyệt | ✅ Tốt nhất |
| B | AI sinh + qua QA gate `APPROVED` + không ai sửa | ✅ Dùng được |
| C | AI sinh + `APPROVED_WITH_WARNINGS` | ⚠️ Chỉ khi thiếu, đánh dấu riêng |
| D | AI sinh, chưa ai xem | ❌ **Không** |

Repo của bạn đã có sẵn tín hiệu này: `QAResult.status`, `human_review_required`, `confidence_score`,
`needs_human_review`. Chỉ cần dùng chúng làm bộ lọc.

---

## 7. Bài tập

| # | Bài | Tiêu chí đạt | Ước lượng |
|---|---|---|---|
| 1 | Chạy checklist §2.2 trên toàn bộ API của service, liệt kê mọi "mỏ nhãn ẩn" | Danh sách ≥3 chỗ | 3h |
| 2 | Thiết kế schema event log cho H1 (correction capture), viết 5 ví dụ JSON tay | Schema + 5 mẫu | 3h |
| 3 | Thêm `SourceType.USER_FEEDBACK` và ghi provenance khi re-export | Test chứng minh phân biệt được | 4h |
| 4 | Chia 20 style golden thành train/test **không rò rỉ**, ghi rõ chống 4 kiểu thế nào | 1 script + giải thích | 3h |
| 5 | Từ `storage/history/`, trích mọi `llm_decision` thành JSONL, lọc theo mức A/B ở §6 | Đếm được bao nhiêu mẫu mức A, B | 4h |
| 6 | Tính tốc độ tích luỹ: mỗi tháng có bao nhiêu run, bao nhiêu lượt sửa tay → bao lâu đạt 10k mẫu | 1 con số + giả định | 2h |

**Bài 6 làm trước.** Nó trả lời câu hỏi quyết định cả lộ trình: *"bao lâu nữa mới đủ data?"* — và nếu
câu trả lời là "3 năm" thì bạn biết phải thay đổi cách thu, không phải chờ.

**Bài 1 làm thứ hai** — nó có thể tìm ra mỏ nhãn mà tôi chưa thấy.

---

## 8. Ranh giới trung thực

- **Đọc từ code, đã verify:** `SourceType` không có `USER_FEEDBACK`; `IDPReExportingData` không có
  `session_id`; `BomLineSchema` có `field_sources` và `alternative_materials`; `storage/history/`
  có 12 run. Tất cả đọc trực tiếp ngày 17/09/2026.
- **Là suy luận, CHƯA XÁC MINH:** tôi kết luận `/idp/pipeline/re-export` nhận BOM người sửa dựa
  trên **docstring và schema**, **không phải traffic thật**. Có thể backend đang gọi nó cho mục đích
  khác (đổi template), hoặc chưa gọi. **Kiểm log endpoint trước khi xây gì trên giả định đó** — đây
  là điều quan trọng nhất cần verify trong cả file.
- **Là con số từ nguồn ngoài:** "10k–100k mẫu" lấy từ paper *Small Language Models are the Future of
  Agentic AI* (NVIDIA + Georgia Tech, arXiv 2506.02153), cho tác vụ agentic tổng quát. Tác vụ hẹp
  schema cố định có thể ít hơn — nhưng nên lấy con số của paper làm mốc an toàn.
- **Chưa kiểm chứng:** tôi không biết mỗi tháng service chạy bao nhiêu lượt thật và bao nhiêu lượt
  bị sửa tay. Đó là lý do bài tập 6 tồn tại, và là số quan trọng nhất còn thiếu.
- **Không bàn tới:** data mesh, feature store, streaming — quá nặng so với quy mô hiện tại.
