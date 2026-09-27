# 17 · Computer Vision cổ điển — biết khi nào KHÔNG cần model

> **Câu hỏi file này trả lời:** hai chỗ tốn GPU nhất trong pipeline của bạn — đếm nút và dò khung
> bảng — đang dùng VLM 35B. Cả hai là bài toán CV cổ điển đã giải xong từ những năm 1970–1990.
> File này dạy đủ để bạn **nhận ra** những chỗ như vậy.
>
> **Thời lượng:** 10–12 giờ. **Yêu cầu trước:** không.
> **Bổ trợ:** [04 · Computer Vision](04-computer-vision.md) nói về CV *deep learning*
> (conv, CNN, transfer learning). File này nói về phần **trước** đó — và phần đó vẫn chưa lỗi thời.

---

## 0. Hai chỗ trong code đang dùng nhầm công cụ

### 0.1 Đếm nút — 16 call VLM cho một phép đếm

[`document_analysis.py:560-640`](../../../motivesidp-ai-service/motives/src/vlm/services/document_analysis.py):

```
1 call "locate"  +  7 call sample ở temperature 0.3  →  lấy plurality vote
× 2 vị trí (center_front, sleeve_cuff)  =  16 call VLM / style
≈ 32.000 token  ≈  2 phút GPU
```

Docstring của chính hàm đó thừa nhận vấn đề: *"model có thể tự tin mà sai trên các icon xếp sát
nhau"*. Đó là **dấu hiệu kinh điển** của việc dùng model ngôn ngữ để làm việc đếm.

**Nút áo trên bản vẽ kỹ thuật là:** hình tròn, cùng bán kính, nét đen trên nền trắng, xếp thẳng hàng
dọc. Đây gần như là bài tập mẫu của Hough Circle Transform.

### 0.2 Dò khung bảng DESCRIPTION — 15 call VLM cho một hình chữ nhật

[`pocket_description_service.py:582-616`](../../../motivesidp-ai-service/motives/src/application_platform/bom_agent/services/read_techpack_ptu/pocket_description_service.py):

```
5 nhiệt độ (0.3 → 0.5) × tối đa 3 vòng = 15 call VLM full-page
→ rồi lấy hợp các bounding box (_consensus_outer_union)
```

Trong khi đó **đã có sẵn trong stack**:
- `docling-layout-heron` chạy ở `:8640`, trả nhãn `table` kèm bbox — đúng đối tượng cần tìm
- `rapidocr-onnxruntime` đã là dependency, và **đã được dùng ngay trong chính file đó**
  (`_ocr_text`, `_ocr_anchor_keywords_present`)

> **Bài học chung:** khi thấy code gọi model nhiều lần rồi "vote" hoặc "lấy hợp", đó thường là dấu
> hiệu công cụ sai — không phải model yếu.

---

## 1. Ảnh nhị phân & morphology — nền của mọi thứ

Bản vẽ line-art là trường hợp dễ nhất của CV: chỉ có đen và trắng.

```python
import cv2
img  = cv2.imread(path, cv2.IMREAD_GRAYSCALE)
# Ngưỡng thích ứng — bền hơn ngưỡng cố định khi PDF render đậm nhạt khác nhau
bw = cv2.adaptiveThreshold(img, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                           cv2.THRESH_BINARY_INV, 31, 10)
```

Bốn phép morphology, hiểu bằng trực giác:

| Phép | Làm gì | Dùng khi |
|---|---|---|
| **Erode** | Bào mỏng vùng trắng | Xoá nhiễu hạt, tách 2 vật dính nhau |
| **Dilate** | Phình vùng trắng | Nối nét đứt |
| **Open** = erode → dilate | Xoá vật nhỏ, **giữ nguyên kích thước** vật lớn | Lọc nhiễu |
| **Close** = dilate → erode | Lấp lỗ bên trong vật | Vá nét đứt của đường kẻ bảng |

```python
kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3))
clean  = cv2.morphologyEx(bw, cv2.MORPH_OPEN, kernel)
```

**Mẹo quan trọng cho bảng:** dùng kernel **rất dài và mỏng** để tách riêng đường kẻ ngang/dọc:

```python
horizontal = cv2.morphologyEx(bw, cv2.MORPH_OPEN,
                cv2.getStructuringElement(cv2.MORPH_RECT, (40, 1)))
vertical   = cv2.morphologyEx(bw, cv2.MORPH_OPEN,
                cv2.getStructuringElement(cv2.MORPH_RECT, (1, 40)))
grid = cv2.add(horizontal, vertical)      # chỉ còn khung bảng
```

Đây là cách dò bảng cổ điển — và nó **tất định**, chạy trong ~20 ms trên CPU.

---

## 2. Connected components — đếm vật thể

Sau khi nhị phân hoá, "đếm có bao nhiêu đốm riêng biệt" là một hàm:

```python
n, labels, stats, centroids = cv2.connectedComponentsWithStats(bw, connectivity=8)
for i in range(1, n):                       # 0 là nền
    x, y, w, h, area = stats[i]
    if 50 < area < 500 and 0.8 < w/h < 1.25:   # lọc theo diện tích & độ vuông
        ...                                     # ứng viên "nút"
```

**Bộ lọc là phần quan trọng nhất**, không phải thuật toán. Nút áo có ràng buộc rất mạnh:
diện tích trong một khoảng, gần tròn (w≈h), và **các nút cùng nhóm có bán kính gần bằng nhau**.

---

## 3. Hough Transform — tìm đường thẳng và đường tròn

Ý tưởng: mỗi điểm ảnh "bỏ phiếu" cho tất cả hình có thể đi qua nó. Hình nào được nhiều phiếu nhất
là hình thật.

```python
circles = cv2.HoughCircles(
    blurred, cv2.HOUGH_GRADIENT, dp=1,
    minDist=20,        # 2 nút gần nhau nhất cách bao nhiêu px
    param1=100,        # ngưỡng Canny trên
    param2=15,         # ngưỡng tích luỹ — THẤP hơn = nhạy hơn, nhiều false positive hơn
    minRadius=4, maxRadius=15,
)
```

**Bốn tham số phải chỉnh theo DPI của bạn.** Code đang render crop ở `dpi=400, min_side=2000`
([`techpack_parser.py:497`](../../../motivesidp-ai-service/motives/src/application_platform/bom_agent/services/techpack_parser.py)) —
nút ở DPI đó to hơn nhiều so với ở 180 DPI.

### 3.1 Ràng buộc hình học — thứ làm nó chính xác hơn VLM

VLM nhìn ảnh và "cảm nhận" có mấy nút. CV cho phép bạn **áp luật cứng**:

```python
# Nút ở nẹp giữa: PHẢI thẳng hàng dọc → x gần bằng nhau
xs = [c[0] for c in circles]
if np.std(xs) < 8:                        # thẳng hàng
    buttons = sorted(circles, key=lambda c: c[1])   # sắp theo y
# Khoảng cách giữa các nút liên tiếp phải đều
gaps = np.diff([b[1] for b in buttons])
if np.std(gaps) / np.mean(gaps) < 0.25:   # đều → tin được
    qty = len(buttons)
```

Nếu ràng buộc không thoả → **trả `None` và để VLM xử lý**. Đây chính là mô hình fallback: CV lo
95% trường hợp dễ trong 30 ms, VLM lo 5% khó.

> **Ưu thế quyết định so với VLM: bạn có bbox để audit.** Khi QC hỏi "sao lại 6 nút?", bạn vẽ được
> 6 vòng tròn lên ảnh. VLM chỉ trả một con số kèm lời giải thích có thể bịa.

---

## 4. Template matching — khi vật thể luôn giống hệt nhau

Techpack của cùng một khách hàng dùng chung bộ icon. Nếu icon nút luôn y hệt:

```python
res = cv2.matchTemplate(img, template, cv2.TM_CCOEFF_NORMED)
ys, xs = np.where(res >= 0.8)
boxes  = [(x, y, tw, th) for x, y in zip(xs, ys)]
boxes  = nms(boxes, iou_thresh=0.3)        # gộp các match chồng nhau
```

**Hạn chế phải biết:** không bền với xoay và đổi tỉ lệ. Techpack là PDF vector render ra ảnh nên
thường ổn định — nhưng nếu khách đổi template thì hỏng. Dùng khi có **ít biến thể và kiểm soát được**.

---

## 5. IoU & NMS — hai công thức phải thuộc

Dùng ở mọi bài toán detection, kể cả khi bạn không train model nào.

```python
def iou(a, b):                      # a, b = (x1, y1, x2, y2)
    xi1, yi1 = max(a[0], b[0]), max(a[1], b[1])
    xi2, yi2 = min(a[2], b[2]), min(a[3], b[3])
    inter = max(0, xi2 - xi1) * max(0, yi2 - yi1)
    ua = (a[2]-a[0])*(a[3]-a[1]) + (b[2]-b[0])*(b[3]-b[1]) - inter
    return inter / ua if ua > 0 else 0.0
```

**NMS (Non-Maximum Suppression):** sắp các box theo điểm giảm dần, giữ box cao nhất, xoá mọi box có
IoU với nó > ngưỡng, lặp lại.

Code của bạn **đã có** `_iou` và `_consensus_outer_union`
([`pocket_description_service.py:428-480`](../../../motivesidp-ai-service/motives/src/application_platform/bom_agent/services/read_techpack_ptu/pocket_description_service.py)) —
nhưng đang dùng để gộp kết quả của **15 lần gọi VLM**, thay vì để gộp kết quả của một detector.

---

## 6. OCR như một công cụ hình học, không chỉ để đọc chữ

Đây là góc nhìn hay bị bỏ qua: OCR không chỉ trả *chữ*, nó trả **toạ độ của chữ**.

```python
from rapidocr_onnxruntime import RapidOCR
ocr = RapidOCR()
result, _ = ocr(image)      # [[box, text, score], ...]  ← box là 4 điểm
```

⇒ Muốn tìm bảng DESCRIPTION? Tìm toạ độ các từ neo, rồi lấy bao lồi:

```python
anchors = [b for b, t, s in result
           if t.upper() in ("DESCRIPTION", "SLEEVES", "SIZE", "LAPEL", "VENT")]
x1 = min(p[0] for box in anchors for p in box)
y1 = min(p[1] for box in anchors for p in box)
x2 = max(p[0] for box in anchors for p in box)
y2 = max(p[1] for box in anchors for p in box)
```

Vài chục ms trên CPU, thay cho 15 lần prefill full-page ≈ 140 giây GPU.

Và code của bạn **đã làm đúng hướng này rồi** — `_ocr_anchor_keywords_present()` kiểm tra đúng ba
từ khoá đó, chỉ là để *verify* thay vì để *định vị*.

---

## 7. Cây quyết định: CV cổ điển hay model?

```
Vật thể cần tìm có hình học ổn định không?   (tròn/chữ nhật/đường thẳng)
├── CÓ ──► Nền có sạch không? (line-art, scan rõ)
│          ├── CÓ ──► ✅ CV CỔ ĐIỂN. Bắt đầu ở đây, luôn luôn.
│          └── KHÔNG ► thử CV + tiền xử lý; fail thì lên detector nhỏ
└── KHÔNG ► Có nhiều biến thể nhưng cùng "loại" không?
           ├── CÓ ──► 🎯 Detector nhỏ (YOLOv8n) — 300-500 ảnh nhãn
           └── KHÔNG ► Cần hiểu ngữ nghĩa / đọc chữ trong ngữ cảnh?
                      └── CÓ ──► 🤖 VLM. Và CHỈ khi tới được đây.
```

Áp vào pipeline của bạn:

| Việc | Hình học ổn định? | Kết luận |
|---|---|---|
| Đếm nút | Tròn, cùng bán kính, thẳng hàng | ✅ **CV cổ điển** |
| Dò khung bảng | Chữ nhật, có đường kẻ | ✅ **CV cổ điển** (hoặc heron sẵn có) |
| FRONT/BACK/BOTH/OTHER | Không — hình dáng áo đa dạng | 🎯 **Classifier nhỏ** (linear probe, [04 §4](04-computer-vision.md)) |
| Đọc bảng BOM thành JSON | Không — cần hiểu ngữ nghĩa | 🤖 **VLM** — đúng chỗ |
| So 2 sketch giống nhau bao nhiêu | Không — cần hiểu cấu trúc | 🤖 **VLM / embedding** — đúng chỗ |

---

## 8. Bài tập

| # | Bài | Tiêu chí đạt | Ước lượng |
|---|---|---|---|
| 1 | Nhị phân hoá 10 crop sketch, thử 3 kiểu threshold, nhìn bằng mắt | 1 hình so sánh + chọn 1 kiểu | 2h |
| 2 | Tách đường kẻ ngang/dọc của bảng DESCRIPTION bằng morphology kernel dài | Ảnh chỉ còn khung bảng | 3h |
| 3 | **Đếm nút bằng HoughCircles** trên 20 style golden, so với đáp án đúng | Accuracy + thời gian/ảnh | 6h |
| 4 | Thêm ràng buộc thẳng hàng + đều khoảng (§3.1), đo lại | Accuracy tăng bao nhiêu | 3h |
| 5 | Dò bbox bảng bằng **OCR anchor**, so IoU với kết quả 15-call VLM hiện tại | IoU trung bình + thời gian | 4h |
| 6 | Cài `iou()` và `nms()` từ đầu, test với 5 case tự vẽ | 5/5 đúng | 2h |
| 7 | Với mỗi bước VLM trong pipeline, chạy cây quyết định §7 và ghi kết luận | Bảng ~8 dòng | 2h |

**Bài 3 là bài quan trọng nhất cả file.** Nếu Hough đạt ≥95% trên 20 style golden thì bạn vừa xoá
32.000 token/style và 2 phút GPU/style — bằng khoảng 150 dòng code không cần train gì.

**Bài 7 làm cuối** — nó biến kiến thức file này thành danh sách việc.

---

## 9. Ranh giới trung thực

- **Đọc từ code, đã verify:** mọi mô tả ở §0 (16 call đếm nút, 15 call dò bbox, `_iou` đã tồn tại,
  `rapidocr` đã là dependency và đã được gọi) đọc trực tiếp từ source ngày 17/09/2026.
- **Là suy luận của tôi, CHƯA ĐO:** **toàn bộ §3 và §6 là giả thuyết.** Tôi **không biết** Hough
  Circle có đạt 95% trên sketch techpack thật hay không — tôi chưa chạy. Nút có thể bị nét khác che,
  có thể là hình vuông ở vài khách hàng, có thể quá nhỏ ở DPI thấp. Đó chính là lý do bài tập 3 tồn
  tại, và là lý do nó phải chạy **trước** khi viết production code.
- **Là suy luận:** con số "vài chục ms trên CPU" ở §6 là mức thông thường của RapidOCR, chưa đo trên
  máy bạn.
- **Cảnh báo phương pháp:** đừng thay VLM bằng CV rồi mới đo. **Đo song song trước** — chạy cả hai
  trên 20 style golden, so từng case. CV thắng thì chuyển, thua thì bỏ, và **cả hai kết quả đều
  đáng ghi lại**.
- **Không bàn tới:** SIFT/SURF/ORB (feature matching), optical flow, camera calibration, stereo —
  không liên quan tới ảnh tài liệu.
