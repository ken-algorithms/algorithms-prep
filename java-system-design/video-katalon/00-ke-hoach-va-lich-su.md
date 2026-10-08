# Video chen ngang Katalon — họ A + C: đếm sự kiện 10k → 10M → 100M request/phút: kế hoạch và nhật ký

> Bộ video tiếng Anh **chen giữa các giai đoạn** của [lộ trình 6 tháng](../00-lo-trinh-6-thang.md), để luyện phỏng
> vấn Katalon. Câu hỏi gốc là bài đã hỏi thật ở vòng Principal:
> [04 — Event Counting 10k/phút](../../katalon-prep/katalon-system-design/04-event-counting-10k.md). Bộ video trả lời câu
> follow-up *"nếu là 10 triệu request/phút thì sao? 100 triệu thì sao?"*, viết ở
> [07 — Event Counting 10M và 100M/phút](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md).
>
> **Hai giọng, dạng phỏng vấn thử:** Tom (`am_michael`, giọng của [giai đoạn 1](../video-gd1/00-ke-hoach-va-lich-su.md))
> đóng vai interviewer; Emma (`af_heart`, giọng của [giai đoạn 2](../video-gd2/00-ke-hoach-va-lich-su.md)) là ứng viên.
> Cùng công cụ [`tools/lesson_video/`](../../tools/lesson_video/README.md).
>
> File này vừa là kế hoạch, vừa là chỗ theo dõi việc đã làm: [bảng trạng thái (mục 8)](#8-bảng-trạng-thái),
> [nhật ký (mục 9)](#9-nhật-ký--lịch-sử-đã-làm). Chữ viết tắt: [01 — Bảng chữ viết tắt](01-bang-chu-viet-tat.md) (chỉ chữ
> mới) cộng bảng của [giai đoạn 1](../video-gd1/01-bang-chu-viet-tat.md) và [giai đoạn 2](../video-gd2/01-bang-chu-viet-tat.md).
> Độ phủ: [02 — Độ phủ nội dung](02-do-phu.md).

---

## 0. Trả lời ngắn: bao nhiêu video, xem khi nào

**8 video (K00–K07), dự kiến khoảng 54 phút**, chia hai nhóm theo thời điểm nên xem:

| Nhóm | Video | Cần học trước | Dài dự kiến |
|---|---|---|---:|
| Xem sau giai đoạn 1 (hết tuần 4) | K00–K02 | Ước lượng, thang L0–L4 và câu hỏi "× 10", Postgres, cache | 19 phút |
| Xem sau giai đoạn 2 (hết tuần 10) | K03–K07 | Kafka (key, độ bền, giao nhận), stream window, fencing token | 35 phút |
| **Tổng** | **8** | | **≈ 54 phút** |

**Thực tế (đợt 1, 08/10/2026): 8 video, tổng 48:04 (≈ 48 phút), 42 MB;** nhóm sau giai đoạn 1 16:53, nhóm sau giai đoạn 2 31:11; ngắn nhất K00 4:44, dài nhất K05 7:21. Số từng video ở [mục 8](#8-bảng-trạng-thái).

Vì sao là con số này:

1. **Bốn câu hỏi, mỗi câu một cụm video:** mốc 10k (K01), leo thang × 10 và × 100 (K02), 10M (K03 thiết kế, K04 tính
   đúng đắn), 100M (K05), họ C khi tải lớn (K06), rồi trả lời có bấm giờ và bản đồ nối với lộ trình (K07).
2. **Mỗi video 5–8 phút**, đúng một câu interviewer có thể hỏi tiếp. Dài hơn thì khó luyện lại từng câu.
3. **Không lặp giai đoạn 1 và 2.** Bộ này không dạy lại Kafka hay Little's Law; nó dùng chúng, rồi chỉ ra bài học gốc ở
   slide *Built on* cuối mỗi video ([mục 5](#5-bản-đồ-video-chen-ngang--bài-học-giai-đoạn-1-và-2)).

---

## 1. Vì sao có bộ chen ngang này

Yêu cầu: luyện phỏng vấn Katalon với **họ bài thu thập và xử lý dữ liệu** trong bảy họ bài
([README](../../katalon-prep/katalon-system-design/README.md#2-bảy-họ-bài--chữ-ký-nhận-dạng-lõi-giải-pháp-bẫy-kinh-điển)):
họ **A** (đếm và tổng hợp theo thời gian — *xử lý*) và họ **C** (thu thập luồng sự kiện — *thu thập*). Bài event counting
có sẵn chỉ ở mức cơ bản: 10k request/phút. Bộ video hỏi tiếp: 10M thì sao, 100M thì sao — và mỗi ý trong câu trả lời
lấy từ bài học nào của giai đoạn 1 và 2.

"Chen ngang" nghĩa là bộ này **không** thuộc lịch 30 video của giai đoạn 1 hay 23 video của giai đoạn 2. Xem lúc nào
cũng được, nhưng K03–K07 dễ hiểu nhất sau tuần 10.

---

## 2. Phạm vi nguồn

| File | Phần dùng | Ghi chú |
|---|---|---|
| [07 — Event Counting 10M và 100M/phút](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md) | **Toàn bộ** | Viết cùng đợt với bộ video: thang ước lượng, cái gì vỡ trước, kiến trúc 10M và 100M, họ C, kịch bản trả lời, bản đồ nối với lộ trình, ranh giới trung thực |
| [04 — Event Counting 10k/phút](../../katalon-prep/katalon-system-design/04-event-counting-10k.md) | §0–4, §6.3–6.5, §7, §11 | Mốc 10k cho K01 |
| [README họ bài](../../katalon-prep/katalon-system-design/README.md) | §1 (5 trục), §2 (7 họ) | K00 |
| [02 — Track S](../02-ve-he-thong-100k-1m-10m.md) | §5 (năm câu trả lời "× 10") | K02 |
| [01 — Track P](../01-java-code-cham-duoi-tai-cao.md) | §2 (số đo P01, P04, P05) | K05: chi phí mỗi request nhân với 1,67 triệu request/s |

Mọi số trong video lấy từ các file trên. Số đo thật (P01, P04) nói rõ là "measured"; còn lại là phép nhân từ giả định
(20 cặp/request, ~1 KB/request, 60 B/event, 0,2 ms CPU mỗi request của collector), nói rõ là "assumption".

---

## 3. Quy ước

| Hạng mục | Bộ chen ngang |
|---|---|
| Giọng | Kokoro-82M q8, tốc độ 0,9. **Tom** `am_michael`, vai interviewer; **Emma** `af_heart`, vai ứng viên |
| Mã video | `K00`–`K07` (khoá `prefix: K` ở [`lessons/series.yaml`](lessons/series.yaml)); file `k0N-<slug>.yaml` |
| Màn hình | Góc phải ghi *Java System Design · Katalon interlude*; tag `K03 · AFTER PHASE 2` nói luôn nên xem sau giai đoạn nào; thẻ mở đầu hiện cả hai người nói |
| Dạng | Tom hỏi → đếm ngược "Pause the video and try it yourself" (nhãn *Question*, *Follow-up*, *Drill*) → Emma trả lời → slide *Built on* → recap |
| Chữ viết tắt | Kiểm với **ba** bảng: giai đoạn 1, giai đoạn 2, và [bảng chữ mới của bộ này](01-bang-chu-viet-tat.md) |
| Ý chính | [`lessons/points.yaml`](lessons/points.yaml): mỗi ý có chữ bắt buộc (`expect`); phạm vi theo nhóm xem (4 = sau giai đoạn 1, 10 = sau giai đoạn 2) |

```bash
cd tools
R="uv run --with-requirements lesson_video/requirements.txt python3 -m lesson_video"
G="--glossary ../java-system-design/video-gd1/01-bang-chu-viet-tat.md --glossary ../java-system-design/video-gd2/01-bang-chu-viet-tat.md"
$R check ../java-system-design/video-katalon/lessons/k0*.yaml $G --glossary ../java-system-design/video-katalon/01-bang-chu-viet-tat.md
$R build ../java-system-design/video-katalon/lessons/k03-10m-pipeline.yaml
$R coverage ../java-system-design/video-katalon/lessons --points ../java-system-design/video-katalon/lessons/points.yaml \
   --strict --link-prefix ../ --title "Độ phủ nội dung — video chen ngang Katalon" --out ../java-system-design/video-katalon/02-do-phu.md
```

---

## 4. Danh sách 8 video

Cột **Nguồn**: `07 §5.4` là mục 5.4 của file 07 (`katalon-prep/katalon-system-design/`).

### Xem sau giai đoạn 1

| Mã | Tên (tiếng Anh) | Nội dung chính | Nguồn | Phút |
|---|---|---|---|---:|
| **K00** | Katalon interlude: the question after 10k a minute | Đề đã hỏi thật và câu trả lời bị bác; 5 trục nhận họ bài; họ A (xử lý) và họ C (thu thập); ba mức tải 3.333 / 3,3 triệu / 33 triệu event/s; 8 video và khi nào xem; cách luyện (dừng và trả lời thành tiếng, nói giả định, năm câu "× 10", slide *Built on*) | README §1–2; 04 §0; 07 §0, §9 | 5 |
| **K01** | The 10k baseline in three minutes | Ba câu hỏi trước khi vẽ; 4 lỗ hổng của câu trả lời cũ; 3.333 event/s, 288 triệu/ngày, 96 dòng cho 24 giờ; thiết kế tầng 1; ba nguyên tắc và bốn chi tiết ghi điểm; phân rã khoảng (~120 dòng thay vì 864 triệu); right-sizing và ngưỡng chuyển; bốn câu cần nhớ | 04 §0–4, §6.3–6.5, §7, §11 | 6 |
| **K02** | Climbing the ladder: ten and a hundred times | Hỏi lại gì ở 10M–100M; thang 5 bậc (request/s, event/s, event/ngày, raw/ngày, băng thông); đường đọc không đổi (5.760 dòng/ngày ở mọi mức); cardinality là tương đối (≤ 0,5 so với 500); năm câu "× 10"; × 10 (gộp trong API) và × 100 (Kafka làm bộ đệm bền); quy luật: chỗ gộp tiến về gần nguồn | 07 §1–4.2; 02 §5 | 8 |

### Xem sau giai đoạn 2

| Mã | Tên (tiếng Anh) | Nội dung chính | Nguồn | Phút |
|---|---|---|---|---:|
| **K03** | Ten million requests a minute: the pipeline | Số mới (166.667 request/s, 3,3 triệu event/s, 1,3 Gbit/s); vỡ trước: key theo tenant làm nóng một partition, không còn bảng một dòng mỗi event; sơ đồ pipeline; một bản ghi mỗi request, key `batch_id`, số partition chọn dư, gộp hai tầng, cửa sổ theo event time; raw đóng gói 128 B/request; câu trả lời 2 phút | 07 §4.3, §5.1–5.3, §8.1 | 7 |
| **K04** | Counting exactly once with at-least-once parts | Ba mảnh: client gửi lại cùng `batch_id`, khử trùng 15 phút (state = tốc độ × thời gian), sink ghi đè `GREATEST`; vì sao đúng khi replay và khi có consumer "xác sống"; giống fencing token của lab 10A; event trễ, đồng hồ client; đối soát mỗi giờ (`final_cnt`, `is_final`); follow-up về transaction của Kafka | 07 §5.4, §7.4, §8.3 | 7 |
| **K05** | A hundred million a minute: physics and money | Số mới (1,67 triệu request/s, 33 triệu event/s, 13 Gbit/s); 1 µs mỗi request = 1,7 core, P01 và P04 thành ~24 và ~31 core; ba hoá đơn (mạng qua AZ, lưu raw, CPU); cell theo region cộng toàn cục; năm thay đổi so với 10M; vì sao số đếm active-active được còn tiền thì không; cái không làm; câu trả lời 2 phút | 07 §4.4, §5.5, §6, §8.2 | 8 |
| **K06** | Collecting from sources you don't control | Bốn quy tắc họ C ở ba mức tải; backpressure năm lớp, lớp nào cũng có giới hạn; rate limit ở 1,67 triệu request/s (Redis + Lua so với token bucket cục bộ); registry key trong RAM (fingerprint 64 bit); metric thêm; drill "tenant lớn nhất gửi gấp 10" | 07 §7 | 6 |
| **K07** | The timed answer, and where each idea comes from | Hai câu hỏi có đếm ngược 10 giây và câu trả lời mẫu; năm câu hỏi nhanh; bản đồ về giai đoạn 1 (video đã dựng và tuần 2–4 chưa dựng) và giai đoạn 2 (Kafka; stream và hệ phân tán); ranh giới trung thực | 07 §8–10 | 7 |

---

## 5. Bản đồ: video chen ngang ↔ bài học giai đoạn 1 và 2

**Trả lời ngắn cho câu "bộ này liên quan gì tới giai đoạn 1 và 2":** bộ chen ngang **không có kiến thức mới**. Giai đoạn 1
cho cách ước lượng, khung "× 10" năm câu và tư duy chi phí mỗi request (Track P). Giai đoạn 2 cho Kafka (key, thứ tự, độ
bền, giao nhận, consumer group), stream processing (window, event time, watermark), khử trùng, fencing token và CRDT.
Câu hỏi 10M/100M chỉ bắt ghép chúng lại, dưới áp lực thời gian. Bảng đầy đủ theo từng ý ở
[07 §9](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#9-bản-đồ-nối-với-giai-đoạn-1-và-giai-đoạn-2).

Ký hiệu: video giai đoạn 1 từ Ep07 trở đi **chưa dựng** (*kế hoạch*); khi đó dẫn tới tuần và mục tài liệu.

| Video | Dựa trên giai đoạn 1 | Dựa trên giai đoạn 2 |
|---|---|---|
| K00 | Ep00 (cách xem video, "Pause and try it yourself") · Ep06 (luật của thang) | — |
| K01 | Ep01–Ep03 (ước lượng) · Ep06 (chỉ lên bậc khi có thứ vỡ) · tuần 2: [idempotency key](../10-implement-gd1-nen-tang.md#24-idempotency-key--4-quy-tắc) (Ep10 *kế hoạch*) · tuần 4: [ba bệnh của cache](../10-implement-gd1-nen-tang.md#42-ba-bệnh-của-cache) (Ep21 *kế hoạch*) | — |
| K02 | Ep01 (quy đổi) · Ep06 và [02 §5](../02-ve-he-thong-100k-1m-10m.md#5-câu-hỏi-tải-tăng-10-lần-thì-sao--cách-trả-lời) (năm câu "× 10") · tuần 3: [B-tree hay LSM-tree](../10-implement-gd1-nen-tang.md#31-b-tree-hay-lsm-tree) (Ep13 *kế hoạch*) | Ep12 (acks, bản sao, bộ đệm bền) |
| K03 | Ep06 ("chưa làm" ở mọi bậc) | Ep03 (partition nóng) · Ep12 (thứ tự chỉ trong partition, đổi số partition) · Ep17 (window, event time, watermark; đề ad click cùng họ A) |
| K04 | Ep01 (Little's Law: state = tốc độ × thời gian) · tuần 2: idempotency key giữ lâu hơn cửa sổ retry | Ep08 (idempotency ở ba tầng) · Ep13, Ep15 (at-least-once + sink idempotent; transaction chỉ bao Kafka) · Ep19 (đồng hồ nói dối) · Ep20 (fencing token) |
| K05 | Ep04, Ep05 (Track P: µs × request/s ra core) · [L4 cell-based](../02-ve-he-thong-100k-1m-10m.md#35-l4--vượt-10m-chỉ-cần-nói-được-không-cần-vẽ-chi-tiết) (Ep25 *kế hoạch*) | Ep01 (multi-leader, CRDT) · Ep07 (không 2PC) · Ep12–Ep13 (idempotent producer: producer id + sequence) |
| K06 | [P13](../01-java-code-cham-duoi-tai-cao.md#p13--hàng-đợi-không-giới-hạn-khi-quá-tải) (hàng đợi có giới hạn) · tuần 2: [lab 2 rate limiter](../10-implement-gd1-nen-tang.md#lab-2--rate-limiter-phân-tán-trong-spring-boot-thứ-bảy-34-giờ) và V3 (Ep08, Ep11 *kế hoạch*) | Ep12 (key, compacted topic) · Ep13 (backpressure) · Ep16 (P20) · đề Katalon [01](../../katalon-prep/katalon-system-design/01-truetest-journey-mining.md) (PII ở client) |
| K07 | Toàn bộ cột trái, ghép lại | Toàn bộ cột trái, ghép lại |

**Ngược lại — bài nào của lộ trình được "dùng thật" ở bộ này:** giai đoạn 1 cả 7 video đã dựng (Ep00–Ep06); giai đoạn 2
11/23 video: Ep01, Ep03, Ep07, Ep08, Ep12, Ep13, Ep15, Ep16, Ep17, Ep19, Ep20.

### Ghi chú thiết kế nội dung

- **Câu "đường đọc không đổi" là trục của cả bộ.** Rollup lớn theo *nhóm × bucket*, không theo event/s, nên K01 giữ
  nguyên ở 10M và 100M; mọi video sau chỉ nói về đường ghi. Nói được câu này là tránh được lỗi vẽ lại cả hệ thống.
- **Không dùng sketch cho số tổng.** Lỗi hay gặp ở 100M là nhảy sang Count-Min Sketch cho mọi thứ; tổng 4 outcome mỗi
  bucket là 4 số nguyên — đếm chính xác rẻ ở mọi quy mô (K05, K07).
- **`GREATEST` thay cho mọi cơ chế exactly-once.** Chọn vì nó nối thẳng về fencing token của lab 10A giai đoạn 2, và
  vì giới hạn của nó (không hạ được số quá cao) dẫn tự nhiên tới batch đối soát (K04).

---

## 6. Phát âm — các chỗ đã kiểm

Mọi câu có số, ký hiệu hay chữ viết tắt đã qua `speak.py` rồi phiên âm bằng bộ tách âm của Kokoro (`phon`).

| Chữ | Kokoro đọc nếu để nguyên | Cách xử lý |
|---|---|---|
| `K01`…`K07` | *K zero one* | Quy tắc mới trong `speak.py`: `K01` → *K 1* (như `P01` → *P 1*) |
| `13 Gbit/s` | *13 G-bit per second* | Đơn vị mới: *gigabits per second* |
| `1 µs` | *1 microseconds* | Số ít cho `1` + đơn vị (trừ phần lẻ thập phân: *0 point 1 milliseconds*) |
| PII | *pee-eye* | `speak.py`: *P I I* |
| dedup | *đờ-đắp* (`dᵻdˈʌp`) | `speak.py`: *dee-doop*; từ ghép `dedups`, `deduplicate` giữ nguyên |
| `202`, `429`, `503` | đọc như số thường | `say`: *two oh two*, *four twenty-nine*, *five oh three* |
| `p99` | không rõ | `say`: *p ninety-nine* |
| problem `04`, lab `10A` | *zero four*, *ten eigh* | `say`: *problem four*, *lab ten A* |
| `5 × 10⁻¹³` | bỏ dấu mũ âm | `say`: *five times ten to the minus thirteen* |
| `$6,900`, `$110k` | ký hiệu tiền | chỉ trên slide; lời đọc viết *dollars* |

Đã kiểm đúng sẵn: `batch_id` (*batch eye-dee*), GREATEST, HyperLogLog, RocksDB, TimescaleDB, Parquet, OLAP, SDK, CRDT,
LSM, anycast, Count-Min Sketch, `P13`, `P20`, `L4`.

**Ảnh hưởng tới hai bộ cũ:** quy tắc `dedup` làm đổi đúng một câu của
[Ep15 giai đoạn 2](../video-gd2/lessons/ep15-lab8-consumer-tests.yaml) ("a dedup table you must clean up") — đã dựng lại.
Các quy tắc khác không đổi câu nào trong 969 câu của 30 video cũ (đã so lời đọc cũ và mới từng câu).

---

## 7. Công cụ: thay đổi cho bộ này

| Thay đổi | Vì sao |
|---|---|
| Khoá `prefix` (series.yaml) và `Lesson.code`: mã `K03` trên màn hình, trong `lessons.json` (`code`), bảng độ phủ và web | Hai bộ trước dùng `Ep`; bộ chen ngang cần mã riêng để không lẫn "Ep03 của bộ nào" |
| `lesson_paths()`: mọi `.yaml` trong `lessons/` trừ `points.yaml`, `series.yaml` | Trước đây chỉ đọc `ep*.yaml` |
| Thẻ mở đầu hiện hai, ba người nói; chữ trong thẻ không vẽ tràn khỏi khung | Bộ này có Tom và Emma; bản đầu tràn chữ ở 7/8 video |
| Cảnh `exercise` có `label` (*Question*, *Follow-up*, *Drill*, *Problem*) | "Exercise K03" đọc như bài tập của lab |
| Kiểm chữ viết tắt cả trong các dòng của cảnh `compare` | Trước đây bỏ sót. Lộ ra 3 chỗ ở bộ cũ: **NAB**, **MVP** ở Ep00 giai đoạn 1 và **WAL** ở Ep14 giai đoạn 2 hiện trên slide mà thẻ không giải thích — đã thêm vào thẻ và dựng lại hai video |
| Bỏ qua từ khoá SQL `GREATEST`, `COALESCE`, `EXCLUDED`… và động từ HTTP `GET`, `POST`… khi kiểm chữ viết tắt | Không phải chữ viết tắt |
| Test: mỗi bộ kiểm với đúng các bảng chữ viết tắt nó dựa vào; độ phủ 100% của bộ chen ngang | Giữ mức đã đạt khi sửa kịch bản về sau |
| Web: chuẩn hoá đường dẫn `../katalon-prep/…` khi mở mục nguồn; hiện mã `K03`; workflow Pages copy mọi bộ `video-*` | Nguồn của bộ này nằm ngoài `java-system-design/` |

---

## 8. Bảng trạng thái

| Mã | Nhóm | Trạng thái | Dài | MB | Chương | Kiểm | Ghi chú |
|---|---|---|---|---|---|---|---|
| K00 | sau GĐ1 | **Chờ duyệt** | 4:44 | 4,1 | 9 | 7 ý chính · 1 chữ viết tắt | |
| K01 | sau GĐ1 | **Chờ duyệt** | 5:32 | 4,7 | 12 | 10 ý chính · 2 chữ viết tắt | 2 mẫu +2,2 dBFS lúc 2:51 (nhật ký đợt 1) |
| K02 | sau GĐ1 | **Chờ duyệt** | 6:37 | 5,7 | 12 | 9 ý chính · 4 chữ viết tắt | |
| K03 | sau GĐ2 | **Chờ duyệt** | 6:19 | 5,6 | 11 | 8 ý chính · 9 chữ viết tắt | |
| K04 | sau GĐ2 | **Chờ duyệt** | 5:47 | 4,9 | 12 | 8 ý chính · 5 chữ viết tắt | |
| K05 | sau GĐ2 | **Chờ duyệt** | 7:21 | 6,8 | 13 | 12 ý chính · 10 chữ viết tắt | |
| K06 | sau GĐ2 | **Chờ duyệt** | 5:34 | 4,8 | 10 | 6 ý chính · 6 chữ viết tắt | |
| K07 | sau GĐ2 | **Chờ duyệt** | 6:09 | 5,7 | 9 | 8 ý chính · 8 chữ viết tắt | |

**Đã dựng 8/8 video, 48:04 (≈ 48 phút), 42 MB — chờ duyệt. Đã duyệt 0/8.**

---

## 9. Nhật ký / lịch sử đã làm

### Đợt 0 — 08/10/2026: nguồn và kế hoạch

- Viết [07 — Event Counting 10M và 100M/phút](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md)
  làm nguồn; thêm vào README của folder system design; sửa số họ bài (7) và số trục (5) ở README, 04, 06 và file chiến lược.
- Chốt 8 video, hai nhóm xem, dạng phỏng vấn hai giọng (mục 0, 3, 4).

### Đợt 1 — 08/10/2026: dựng cả 8 video

Yêu cầu: làm video chen ngang để luyện phỏng vấn Katalon với họ bài thu thập và xử lý dữ liệu. Bài event counting có
sẵn mới ở mức 10k request/phút; câu hỏi là 10M thì sao, 100M thì sao, và các video liên quan gì tới giai đoạn 1 và 2.

- **Nguồn**: viết mới [07](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md) (xem Đợt 0).
- **Kịch bản**: 8 file YAML ở [`lessons/`](lessons/), 250 câu, 6.478 từ. Tom hỏi, Emma trả lời; khai báo một lần ở
  [`series.yaml`](lessons/series.yaml) (`prefix: K`, hai người nói).
- **Ý chính**: [`lessons/points.yaml`](lessons/points.yaml) có **68 ý chính**, phạm vi **46 mục** của 07, 04, README họ
  bài và §5 của file 02. `check` với ba bảng chữ viết tắt và `coverage --strict` đều sạch: 68/68 ý, 46/46 mục
  ([02 — Độ phủ](02-do-phu.md)). Test `test_interlude_points_and_headings_all_covered` giữ mức này.
- **Dựng**: Kokoro, tốc độ 0,9, hai rồi ba tiến trình song song (cùng 3 video dựng lại của hai bộ cũ: khoảng 50 phút).
  Tổng 48:04 (≈ 48 phút), 42 MB; mỗi file từ −15,8 đến −16,4 LUFS. K01 có 2 mẫu liền nhau vượt 0 dBFS (+2,2 dBFS, dài
  0,05 ms) ở một âm bật lúc 2:51, sinh ra khi mã hoá AAC 64 kbit/s: tiếng gốc của Kokoro chỉ tới 0,62 và `loudnorm` đã
  chặn ở −1,5 dBTP. Giữ nguyên như Ep12, Ep14 của giai đoạn 2; nếu khi duyệt nghe thấy tiếng tách ở 2:51 thì dựng lại
  câu đó.
- **Soát bố cục** (ảnh cuối mỗi cảnh, ghép 4 ảnh một tấm), lỗi đã sửa:
  - Thẻ mở đầu hai người nói tràn chữ ở 7/8 video → thẻ gọn hơn, chữ không vẽ quá khung; rút ngắn dòng nguồn.
  - Sơ đồ: nhãn mũi tên bị cắt (K01); pipeline 10M chật (K03) → xếp lại ba hàng; ô cell dạng "zone" không hiện dòng
    phụ (K05) → đổi thành hộp thường.
  - Bảng dày chữ nhỏ: "năm thay đổi" (K05) tách hai slide; bản đồ giai đoạn 1 và 2 (K07) tách bốn slide.
  - Khung luồng backpressure (K06) rút chữ; nhãn "Exercise K03" đổi thành *Question*, *Follow-up*, *Drill*, *Problem*.
- **Phát âm** ([mục 6](#6-phát-âm--các-chỗ-đã-kiểm)): `K01`, `Gbit/s`, `1 µs`, PII, dedup sửa trong `speak.py`; số mã
  HTTP, `p99`, problem `04`, lab `10A`, `10⁻¹³` và số tiền viết bằng `say`.
- **Bộ cũ**: dựng lại Ep00 giai đoạn 1 (thẻ thêm NAB, MVP), Ep14 giai đoạn 2 (thẻ thêm WAL) và Ep15 giai đoạn 2 (đọc
  *dedup*); ghi ở Đợt 1c của [kế hoạch giai đoạn 1](../video-gd1/00-ke-hoach-va-lich-su.md) và Đợt 1b của
  [kế hoạch giai đoạn 2](../video-gd2/00-ke-hoach-va-lich-su.md). Một script so lời đọc hiện tại của từng câu với tiếng
  file mp4 đã dùng: cả 38 video của ba bộ đều dùng lời đọc mới nhất.
- **App**: tab **Video** có nút thứ ba *Chen ngang Katalon*, mã `K00`–`K07`, hai nhóm *Xem sau giai đoạn 1* và *Xem sau
  giai đoạn 2*; mục nguồn ở `katalon-prep/` mở đúng heading. Pages copy cả ba bộ vào `media/jsd/<bộ>/`.
- **Kiểm app bằng Playwright**: tab có 38 video (7 + 23 + 8) và ba nút chọn bộ; bộ chen ngang có đúng hai nhóm. Với
  từng video: mã (`K01`), tiêu đề, số chương, số ý chính, số link nguồn, ảnh bìa và đường dẫn mp4 khớp `lessons.json`.
  Tua theo chương và theo ý chính đúng giây; lựa chọn bộ được giữ sau khi tải lại. **110 liên kết nguồn** (ý chính và
  `covers`) của bộ chen ngang đều mở được tài liệu tại đúng heading, kể cả tài liệu ở `katalon-prep/`. Ba nút tài liệu
  mở đúng file; màn hình 390 px không tràn ngang; không có lỗi JavaScript. Bản mô phỏng Pages: 76/76 file media có ở
  `media/jsd/`, ba bộ lấy ảnh bìa và mp4 từ `media/jsd/<bộ>/`. Hai bộ cũ vẫn qua bài test cũ trên template mới.
- **Chưa làm**: chưa có người nghe lại toàn bộ — đó là bước duyệt. Góp ý về giọng, nhịp hỏi–đáp hay độ dài dựng lại
  nhanh vì tiếng đã nằm trong bộ nhớ đệm.
- Commit trên nhánh `claude/wizardly-pascal-9yhum5`.

---

## 10. Quyết định đã chọn mặc định (đổi được khi duyệt)

| Quyết định | Chọn | Lý do |
|---|---|---|
| Giọng | Tom hỏi, Emma trả lời | Dùng lại hai giọng đã quen của giai đoạn 1 và 2; hỏi–đáp giống phòng phỏng vấn hơn một người đọc |
| Ngôn ngữ | Tiếng Anh, tài liệu tiếng Việt | Như hai bộ trước; phỏng vấn Katalon vòng system design bằng tiếng Anh |
| Đếm ngược | 5–10 giây | Đủ để bấm dừng; muốn luyện thật thì dừng 2 phút và nói thành tiếng |
| Nhóm trên web | "Xem sau giai đoạn 1" (K00–K02), "Xem sau giai đoạn 2" (K03–K07) | Nói luôn điều kiện để hiểu |
| Video giai đoạn 1 chưa dựng | Dẫn tới tuần và mục tài liệu, ghi *kế hoạch* | Không hứa một video chưa có |

---

## 11. Ranh giới trung thực

| Điều | Trạng thái |
|---|---|
| Số trong video | Phép nhân từ giả định (20 cặp/request, ~1 KB/request, 60 B/event, 0,2 ms CPU mỗi request), như [07 §10](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md#10-ranh-giới-trung-thực). Số đo thật duy nhất là P01, P04 của Track P |
| Giá AWS | Giá niêm yết tại lúc viết, chỉ để ra bậc độ lớn; dịch vụ được quản lý tính khác |
| Kiến trúc 10M, 100M | Thiết kế trên giấy, **không có demo** ở mức tải này; video nói rõ "I would build and measure it like this" |
| Phát âm | Kiểm bằng bộ tách âm của Kokoro, chưa có người nghe lại toàn bộ — đó là bước duyệt |
