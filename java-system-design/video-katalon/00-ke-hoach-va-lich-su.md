# Video chen ngang Katalon — giải bài đếm sự kiện từng bước, dựng hệ thống, đọc code: kế hoạch và nhật ký

> Bộ video tiếng Anh **chen giữa các giai đoạn** của [lộ trình 6 tháng](../00-lo-trinh-6-thang.md), để luyện phỏng vấn
> Katalon. Đề là bài đã hỏi thật ở vòng Principal và câu trả lời tại chỗ đã bị bác:
> [04 — Event Counting 10k/phút](../../katalon-prep/katalon-system-design/04-event-counting-10k.md).
>
> **Từ đợt 3 (14/10/2026) bộ được làm lại toàn bộ**: giải đề **từng bước** — bốn cách, ba cách bị bác và bị bác vì tiêu
> chí nào — tới cách tốt nhất; **dựng chi tiết** hệ thống theo cách đó; **đọc code** Java chạy thật trong
> [module 10](../../katalon-prep/katalon-prep-java/10-event-counting/README.md) (20 test trên PostgreSQL 16 thật); cuối
> cùng mới tới câu nối tiếp 10M và 100M. **Một giọng: Emma** (`af_heart`). Cùng công cụ
> [`tools/lesson_video/`](../../tools/lesson_video/README.md).
>
> File này vừa là kế hoạch, vừa là chỗ theo dõi việc đã làm: [bảng trạng thái (mục 8)](#8-bảng-trạng-thái),
> [nhật ký (mục 9)](#9-nhật-ký--lịch-sử-đã-làm). Chữ viết tắt: [01 — Bảng chữ viết tắt](01-bang-chu-viet-tat.md) (chỉ chữ
> mới) cộng bảng của [giai đoạn 1](../video-gd1/01-bang-chu-viet-tat.md) và [giai đoạn 2](../video-gd2/01-bang-chu-viet-tat.md).
> Độ phủ: [02 — Độ phủ nội dung](02-do-phu.md).

---

## 0. Trả lời ngắn: bao nhiêu video, xem khi nào

**14 video (K00–K13), 64 phút**, bốn phần:

| Phần | Video | Nội dung | Xem sau | Dài |
|---|---|---|---|---:|
| **A — Giải đề** | K00–K04 | Đề, sáu tiêu chí T1–T6, bốn cách: lock + tổng chạy, raw đếm lúc hỏi, counter phút `+n` (bị bác), raw + rollup ghi đè (chọn) | Ước lượng (tuần 1) | 20:59 |
| **B — Dựng hệ thống** | K05–K07 | Bảng và partition, đường ghi, rollup job, đường đọc, đối soát, retention | Postgres (tuần 3) | 14:32 |
| **C — Đọc code** | K08–K10 | `IngestService`, `RollupJob`, `RangeDecomposer`, `StatsService`, các test và số đo | Java/Spring (tuần 4) | 11:26 |
| **D — 10M và 100M** | K11–K13 | Leo × 10 tới Kafka (bác từng lựa chọn), cell theo region và tiền, bài trả lời 3 phút + 5 follow-up | Giai đoạn 2 (tuần 10) | 16:42 |

---

## 1. Vì sao làm lại

Bộ cũ (đợt 1–2) **nói nhiều lý thuyết** về 10M/100M và về sự cố Kafka, nhưng không giải chính đề đã hỏi theo cách cần
trình bày trong phòng: *vì sao câu trả lời cũ sai, các cách khác sai ở đâu, cách đúng dựng thế nào, code ra sao*. Bộ mới:

1. **Một khung chấm duy nhất** (T1–T6, [module 10 §1](../../katalon-prep/katalon-prep-java/10-event-counting/README.md#1-đề-bài-và-sáu-tiêu-chí-chấm)) cho mọi cách giải.
2. **Mỗi cách bị bác đều có code và test** cho thấy nó trượt đúng tiêu chí nào: cách 1 retry ra 60 cho 40 event thật;
   cách 3 ra 2.420 cho 2.400; cách 2 đếm 32.460 dòng so với 337 + 60.
3. **Cách được chọn có module chạy thật**, video đọc code từ chính file nguồn (rút gọn, không bịa).
4. **Số đo thật, nói rõ giới hạn**: số tranh chấp là 7 lần chạy trên một máy, nói tỉ lệ chứ không nói số tuyệt đối.
5. **Một giọng** theo yêu cầu: Emma vừa giảng vừa trả lời như ứng viên.

---

## 2. Phạm vi nguồn

| Nguồn | Dùng cho |
|---|---|
| [Module 10 README](../../katalon-prep/katalon-prep-java/10-event-counting/README.md) (mới, viết cùng bộ video) | Sáu tiêu chí, bốn cách, dựng hệ thống, đọc code, kết quả test — nguồn chính của K00–K10 |
| [04 — Event Counting 10k/phút](../../katalon-prep/katalon-system-design/04-event-counting-10k.md) | Đề, 4 lỗ hổng, ước lượng, data model, rollup, phân rã, late data, follow-up, kịch bản 3 phút |
| [07 — 10M và 100M/phút](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md) | Thang × 10, kiến trúc 10M, 100M, ba hoá đơn — K11, K12 |
| [Module 09 README](../../katalon-prep/katalon-prep-java/09-kafka-pipeline/README.md) | Số đo lệch partition, sửa "đánh dấu + cộng trong một transaction", DLT, rate limit — K11 |

Phạm vi kiểm: 56 heading (`scope` trong [`points.yaml`](lessons/points.yaml)); 92 ý chính.

---

## 3. Quy ước

- Một người nói khai báo ở [`series.yaml`](lessons/series.yaml): `emma` (`af_heart`), `prefix: K`.
- Cảnh `exercise` có nhãn *Question* hoặc *Follow-up*, đếm ngược 6–8 giây; ở K13 mỗi follow-up có một slide *Model answer*.
- Code trên slide trích từ file thật của module 10 hoặc 09, được rút gọn (`...`); tên lớp, tên hàm và SQL giữ nguyên.
- Số đo chỉ lấy từ output test đã chạy (ghi tên test ở chú thích slide).
- Lệnh:

```bash
cd tools
R="uv run --with-requirements lesson_video/requirements.txt python3 -m lesson_video"
G="--glossary ../java-system-design/video-gd1/01-bang-chu-viet-tat.md --glossary ../java-system-design/video-gd2/01-bang-chu-viet-tat.md"
$R check ../java-system-design/video-katalon/lessons/k*.yaml $G --glossary ../java-system-design/video-katalon/01-bang-chu-viet-tat.md
$R build ../java-system-design/video-katalon/lessons/k05-build-tables-and-write-path.yaml
$R coverage ../java-system-design/video-katalon/lessons --points ../java-system-design/video-katalon/lessons/points.yaml \
   --strict --link-prefix ../ --title "Độ phủ nội dung — video chen ngang Katalon" --out ../java-system-design/video-katalon/02-do-phu.md
```

---

## 4. Danh sách 14 video

### Phần A — Giải đề: bốn cách, ba cách bị bác

| Mã | Video | Kết luận |
|---|---|---|
| K00 | The Katalon counting question, and six tests every answer must pass | Ba cụm từ quyết định; hỏi 6 câu; 3.333 event/s; hợp đồng API; T1–T6 |
| K01 | Approach 1: lock, add to a running total, read the last record | Câu trả lời cũ; **bác vì T1** (mất thời gian); retry 60/40; lock chậm 2,4–4,4 lần nhưng là lý do thứ hai |
| K02 | Approach 2: store every event, count when asked | Qua 5/6; **bác vì T2** (288 triệu dòng mỗi lần hỏi); giữ raw làm sự thật và oracle |
| K03 | Approach 3: per-minute counters, bumped as you write | Sửa được T1; **bác vì T3, T5**: atomic ≠ idempotent (2.420/2.400), không raw; Redis chỉ làm cache |
| K04 | Approach 4: raw truth, rollups that recompute, ranges that decompose | **Chọn**: qua cả 6; bảng chấm bốn cách; chưa cần Kafka + ngưỡng |

### Phần B — Dựng hệ thống theo cách 4

| Mã | Video | Nội dung |
|---|---|---|
| K05 | Build 1: the tables, the partitions and the write path | Sáu bảng; DDL raw partition theo ngày UTC; event id tất định; 5 bước ghi; 4 outcome; registry |
| K06 | Build 2: the rollup job — recompute, replace, seal | DELETE + INSERT … SELECT; cửa sổ 30 phút; seal; event trễ (test 09:58); 4 mức trễ; đánh đổi |
| K07 | Build 3: the read path, the reconcile, and retention | Phân rã khoảng (ví dụ của 04, lỗi đã sửa); chặn trên 529; tỉ lệ lúc đọc; đối soát; drop partition; metric; cache |

### Phần C — Đọc code và test (module 10)

| Mã | Video | Nội dung |
|---|---|---|
| K08 | Code 1: the module, the request, and IngestService | Thứ tự đọc; `EventClassifier`; `ingest()`; `eventId()`; controller; test idempotency |
| K09 | Code 2: RollupJob, RangeDecomposer and StatsService | `refreshRecent`, `replace`, `decompose`, `query`, `Jobs` |
| K10 | Code 3: the tests that prove it, and the numbers they printed | Postgres thật + đồng hồ giả + oracle; 500/500 khoảng; drift −5; tranh chấp 7 lần; cách chạy; giới hạn |

### Phần D — Câu nối tiếp 10M và 100M

| Mã | Video | Nội dung |
|---|---|---|
| K11 | “What about 10 million a minute?” — step by step to a Kafka pipeline | Leo × 10; 5 lựa chọn bị bác; lệch partition đo được 53,2% vs 25,4%; pipeline; sửa module 09; DLT, 429 |
| K12 | “And 100 million?” — when the problem becomes physics and money | 5 lựa chọn bị bác; 5 thay đổi; ba hoá đơn; vì sao số đếm active-active được còn ví tiền thì không |
| K13 | The whole answer in three minutes, and a follow-up drill | Khung 3 phút; bài nói mẫu kể như câu chuyện; 5 follow-up có đáp án mẫu; giới hạn phải nói trước |

---

## 5. Bản đồ: bộ chen ngang ↔ bài học giai đoạn 1 và 2

Bộ này **không dạy lại** nền tảng; nó dùng chúng. Video giai đoạn 1 từ Ep07 trở đi chưa dựng: dẫn tới tuần của lộ trình.

| Video | Dựa trên giai đoạn 1 | Dựa trên giai đoạn 2 |
|---|---|---|
| K00 | Ep01–Ep03 (ước lượng), Ep06 (luật của thang) | — |
| K01–K03 | Tuần 2: idempotency key; tuần 3: lab 3A lost update (lock, MVCC); Track P (đo trước khi sửa) | Ep08 (idempotency ba tầng) |
| K04 | Ep06 (right-sizing, câu hỏi "× 10") | — |
| K05–K07 | Tuần 3: database sâu (index, partition, vacuum); tuần 4: lab 4 cache hai tầng và stampede (single-flight) | Ep17 (window, event time, watermark) |
| K08–K10 | Tuần 2: lab 2 Spring Boot; tuần 3: lab 3A, 3C (test chạy trên database thật) | — |
| K11 | Ep04, Ep05 (Track P: µs × request/s = core) | Ep03 (partition nóng), Ep12–Ep13 (key, acks, DLT), Ep15 (consumer idempotent), Ep16 (P20), Ep20 (fencing) |
| K12 | L4 cell-based (tuần 4) | Ep01 (multi-leader, CRDT), Ep07 (không 2PC), Ep12–Ep13 (idempotent producer) |
| K13 | Toàn bộ | Toàn bộ |

---

## 6. Phát âm — các chỗ đã kiểm

Kiểm bằng bộ tách âm của Kokoro (`phon` và tokenizer): UTC (`jˌuːtˌiːsˈiː`), Postgres, upsert, idempotent
(`idem-potent`), reconciler, Parquet, jitter, `acks all`, Retry-After, MockMvc, JDK, `K03` → "K three", dedup →
"dee-doop" (từ đợt 1). **Sửa mới:** `IDs` trước đây đọc `aɪdˈiː` (mất âm s) → `speak.py` đổi thành "eye-dees"
(`ˈaɪdˈiːz`), có test. Số như `1,080–1,367/s` và `2.4–4.4×` để `speak.py` đọc; 2PC viết bằng `say` ("two P C").

---

## 7. Công cụ: thay đổi cho đợt 3

| Thay đổi | Vì sao |
|---|---|
| Slide code chừa chỗ dưới cùng bằng chiều cao nhãn mục cao nhất | Nhãn mục (`t` của item) che dòng code cuối ở 4 slide; chừa cố định nên code không nhảy giữa các bước |
| `speak.py`: `IDs` → "eye-dees" | Kokoro bỏ âm s số nhiều |
| `series.yaml` chỉ còn `emma` | Theo yêu cầu một giọng; thẻ mở đầu tự hiện một người nói |

---

## 8. Bảng trạng thái

| Mã | Phần | Trạng thái | Dài | MB | Chương | Kiểm |
|---|---|---|---|---|---|---|
| K00 | A | **Chờ duyệt** | 5:05 | 4,4 | 10 | 6 ý chính · 4 chữ viết tắt |
| K01 | A | **Chờ duyệt** | 4:21 | 3,8 | 11 | 7 ý chính · 1 chữ viết tắt |
| K02 | A | **Chờ duyệt** | 3:03 | 2,5 | 9 | 5 ý chính · 3 chữ viết tắt |
| K03 | A | **Chờ duyệt** | 4:14 | 3,6 | 11 | 7 ý chính · 3 chữ viết tắt |
| K04 | A | **Chờ duyệt** | 4:16 | 3,6 | 10 | 6 ý chính · 3 chữ viết tắt |
| K05 | B | **Chờ duyệt** | 5:06 | 4,3 | 10 | 6 ý chính · 5 chữ viết tắt |
| K06 | B | **Chờ duyệt** | 4:17 | 3,6 | 9 | 5 ý chính · 2 chữ viết tắt |
| K07 | B | **Chờ duyệt** | 5:09 | 4,5 | 12 | 9 ý chính · 2 chữ viết tắt |
| K08 | C | **Chờ duyệt** | 4:01 | 3,7 | 9 | 6 ý chính · 8 chữ viết tắt |
| K09 | C | **Chờ duyệt** | 3:08 | 3,1 | 8 | 5 ý chính · 2 chữ viết tắt |
| K10 | C | **Chờ duyệt** | 4:18 | 3,9 | 11 | 8 ý chính · 3 chữ viết tắt |
| K11 | D | **Chờ duyệt** | 6:04 | 5,4 | 11 | 8 ý chính · 6 chữ viết tắt |
| K12 | D | **Chờ duyệt** | 4:55 | 4,3 | 9 | 6 ý chính · 9 chữ viết tắt |
| K13 | D | **Chờ duyệt** | 5:43 | 4,6 | 11 | 8 ý chính · 3 chữ viết tắt |

**Đã dựng 14/14 video, 63:39 (≈ 64 phút), 55 MB — chờ duyệt. Đã duyệt 0/14.**

---

## 9. Nhật ký / lịch sử đã làm

### Đợt 0 — 08/10/2026: nguồn và kế hoạch

- Viết [07 — Event Counting 10M và 100M/phút](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md)
  làm nguồn; thêm vào README của folder system design; sửa số họ bài (7) và số trục (5) ở README, 04, 06 và file chiến lược.
- Chốt 8 video, hai nhóm xem, dạng phỏng vấn hai giọng (mục 0, 3, 4).

### Đợt 1 — 08/10/2026: dựng cả 8 video — đã thay ở đợt 3

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

### Đợt 2 — 10/10/2026: mở rộng thực chiến Kafka & bộ code runnable (K08–K13) — đã thay ở đợt 3

Yêu cầu: 8 video đầu tiên thuần lý thuyết phỏng vấn ("trên giấy"). Cần hướng dẫn chuyên sâu tích hợp Kafka,
build code chạy thật và tái hiện/fix trực tiếp 5 sự cố production kinh điển.

- **Mã nguồn thực chiến**: tạo mới module [`katalon-prep-java/09-kafka-pipeline`](../../katalon-prep/katalon-prep-java/09-kafka-pipeline/README.md)
  chạy trên Spring Boot 3.3.13, Java 21/25, Kafka KRaft. Đầy đủ:
  - Ingestion REST API (`POST /events`), `EventProducer` gom lô (`linger.ms=20`, `batch.size=65536`, `acks=all`, Snappy).
  - Lỗi 1 (Hotspot Skew): `TenantKeyPartitioner` (40% lệch) vs `BatchIdPartitioner` (chia đều 25%).
  - Lỗi 2 (P20 Rebalance Storm): single-message 10s vs `BatchEventConsumer` 2ms (nhanh gấp ~5.000x).
  - Lỗi 3 (Poison Pill): `ErrorHandlingDeserializer` + `DeadLetterPublishingRecoverer` đẩy tự động sang `.DLT`.
  - Lỗi 4 (Duplicate Retries): `RollupRepository` với `processed_batch` và `GREATEST` upsert (`reconcile_drift = 0`).
  - Lỗi 5 (Producer Buffer Exhaustion): `ProducerRateLimiter` Token Bucket chặn burst quá tải (HTTP 429).
  - **8/8 unit/integration tests pass 100%** qua `mvn -pl 09-kafka-pipeline test`.
- **Kịch bản**: 6 file YAML mới (`k08` đến `k13`) trong `lessons/`. Chữ viết tắt bổ sung: **JMX**.
  Lệnh `check` và `coverage` đều qua sạch: **74/74 ý chính, 50/50 mục tài liệu**.
- **Web App**: `web/build.py` cập nhật `total: 14`, chia 3 nhóm tuần (Phần 1A, Phần 1B, Phần 2).
  `lessons.json` cập nhật đủ 14 bài với chapters và points điều hướng chi tiết.

### Đợt 3 — 14/10/2026: làm lại toàn bộ — giải từng bước, dựng hệ thống, đọc code, một giọng Emma

Yêu cầu: review lại cả bộ chen ngang; bộ hiện tại nhiều lý thuyết, chưa thực tiễn giảng giải bài toán Katalon đang
cần. Muốn giải từng bước, từng cách bị bác cho tới cách tốt nhất, chi tiết cách dựng hệ thống theo cách tốt nhất, có
hướng dẫn code; chỉ dùng giọng Emma (`af_heart`); dựng lại hết, commit, push.

- **Review bộ cũ (K00–K13)**: K00–K07 chủ yếu thiết kế 10M/100M trên giấy, không giải lại chính đề 10k theo từng cách;
  K08–K13 có số không nguồn (ví dụ "12,000 req/s stalled", "65,000+ req/s") và giải thích code module 09 có lỗi (xem dưới).
  Xoá cả 14 kịch bản và media cũ; viết mới 14 kịch bản (mục 4).
- **Code mới: [module 10 `10-event-counting`](../../katalon-prep/katalon-prep-java/10-event-counting/README.md)**:
  Spring Boot 3.3 + Java 21 + PostgreSQL 16 thuần. Ba cách bị bác viết thành code thật (`rejected/`) để test chứng
  minh chúng trượt; cách được chọn đủ đường ghi, rollup ghi đè, phân rã khoảng, đối soát, retention. **20 test** trên
  Postgres 16 thật (zonky embedded, không Docker), gồm 500 khoảng ngẫu nhiên so với `COUNT(*)` trên raw.
- **Lỗi tìm thấy và đã sửa**:
  - [04 §6.3](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#63-thuật-toán-phân-rã-khoảng-thời-gian-bất-kỳ):
    hàm Python `decompose` đi tiếp cùng tầng tới hết khoảng — ví dụ của chính tài liệu đọc 4.824 bucket phút thay vì
    84 + 31 + 2. Đã sửa, chạy lại ví dụ; test Java khoá đúng ví dụ này.
  - Module 09: đánh dấu `processed_batch` và cộng số đếm ở **hai transaction** — consumer chết ở giữa là batch bị đánh
    dấu nhưng không được đếm, lần gửi lại bị bỏ qua. Nay `applyBatchOnce` làm cả hai trong một transaction. Partitioner
    dùng `murmur2` + `toPositive` thay cho `Math.abs(hashCode())` (ra số âm khi `hashCode() == Integer.MIN_VALUE`).
    8/8 test của module 09 vẫn qua. README của module 09 ghi đúng cơ chế (trước ghi `GREATEST` nhưng code cộng dồn).
- **Số đo tranh chấp**: chạy `ContentionDemoTest` 7 lần; số dao động (cách 1: 1.080–1.367, cách 3: 4.094–5.986, cách 4:
  3.334–5.488 batch/s). Video và README ghi khoảng, không ghi số của một lần chạy.
- **Kịch bản**: 14 file, 352 câu, 9.026 từ, một người nói. `points.yaml` viết lại: **92 ý chính, 56 heading**;
  `check` với ba bảng chữ viết tắt và `coverage --strict` sạch ([02 — Độ phủ](02-do-phu.md)). Bảng chữ viết tắt của bộ
  nay chỉ còn chữ mới thật sự dùng: UTC, SDK.
- **Soát bố cục** (ảnh cuối mọi cảnh, ghép 4 ảnh một tấm), đã sửa: nhãn mục che dòng code cuối (sửa trong `slides.py`,
  mục 7); nhãn mũi tên bị cắt ở sơ đồ K04, K11 (dãn hộp); hộp flow K05, K06 chật chữ (rút tiêu đề); code của slide lỗi
  Python (K07) và `refreshRecent` (K09) chữ nhỏ (rút dòng); follow-up K13 có đáp án dài trong phụ đề → tách slide
  *Model answer*.
- **Phát âm** (mục 6): thêm `IDs` → "eye-dees" vào `speak.py`. Thay đổi này làm câu của
  [Ep01 giai đoạn 2](../video-gd2/00-ke-hoach-va-lich-su.md) có chữ IDs đọc khác, nên dựng lại Ep01 (Đợt 3 của kế hoạch
  giai đoạn 2).
- **Dựng**: Kokoro `af_heart`, tốc độ 0,9, hai tiến trình song song. Tổng 63:39 (≈ 64 phút), 55 MB;
  độ to từng file từ −15,8 đến −16,7 LUFS. Đỉnh mẫu sau mã hoá AAC: K02 +0,4 dBFS, K06 +0,1 dBFS (tiếng gốc đã được `loudnorm` chặn ở −1,5 dBTP; như K01 cũ và Ep12, Ep14 giai đoạn 2, giữ nguyên); 12 video còn lại dưới 0 dBFS. Một script so lời đọc hiện tại của từng câu với tiếng file mp4 đã dùng: cả 44 video của ba bộ dùng lời đọc mới nhất.
- **App, Pages, test**: `web/build.py` đổi giọng thành Emma, `total: 14`, bốn nhóm (Phần A–D);
  [`tests/test_playwright_katalon_videos.py`](../../tests/test_playwright_katalon_videos.py) viết lại theo 14 video mới.
  Playwright (Chromium): 14/14 video khớp `lessons.json` (mã, tiêu đề, độ dài, số chương, số ý chính, ảnh bìa, file mp4); bốn nhóm A–D đúng thứ tự; chỉ giọng Emma; **146 liên kết nguồn** mở đúng heading, kể cả README module 10; bài test cũ cho cả ba bộ (44 video) vẫn qua: tua theo chương và ý chính đúng giây, giữ lựa chọn bộ sau khi tải lại, 390 px không tràn ngang, không lỗi JavaScript.
- **Chưa làm**: chưa có người nghe lại toàn bộ — đó là bước duyệt. Sửa một slide hay một câu dựng lại 1–3 phút vì
  tiếng đã nằm trong bộ nhớ đệm.

---

## 10. Quyết định đã chọn mặc định (đổi được khi duyệt)

| Quyết định | Chọn | Lý do |
|---|---|---|
| Giọng | Một giọng: Emma (`af_heart`) | Theo yêu cầu đợt 3 |
| Thứ tự | Bác từng cách trước, rồi mới dựng | Đúng cách trình bày trong phòng: lý do của thiết kế nằm ở các cách bị bác |
| Code | Module 10 riêng, Postgres thuần, không TimescaleDB | Thấy được cơ chế (ghi đè, seal); test chạy không cần Docker; Timescale là bản dựng sẵn cùng ý tưởng |
| 10M/100M | Gói vào phần D (3 video) thay vì 8 video | Câu nối tiếp, không phải đề chính; giữ số và lập luận của 07 |
| Nhóm trên web | Phần A (tuần 2), B (tuần 3), C (tuần 4), D (tuần 10) | Mỗi phần cần nền tảng của tuần đó |
| Đếm ngược | 6–8 giây | Đủ để bấm dừng; luyện thật thì dừng lâu hơn và nói thành tiếng |

---

## 11. Ranh giới trung thực

| Điều | Trạng thái |
|---|---|
| Đúng đắn của cách 4 (retry, khoảng bất kỳ, event trễ, đối soát, retention) | ✅ Test trên PostgreSQL 16 thật, 20/20 |
| Cách 1–3 trượt đúng tiêu chí nói trong video | ✅ `RejectedApproachesTest`, `ContentionDemoTest` |
| Số tranh chấp | 🟡 7 lần chạy, một máy, Postgres embedded mặc định — video nói "quote the ratio" |
| Tải 10k request/phút liên tục nhiều giờ | ⚠️ Chưa load test kéo dài |
| Cache "24 giờ qua", advisory lock khi nhiều instance | ⚠️ Thiết kế, chưa code — video nói rõ |
| 10M, 100M (K11, K12) | Thiết kế trên giấy từ [07](../../katalon-prep/katalon-system-design/07-event-counting-10m-100m.md); số lệch partition là số đo thật của module 09; giá AWS là giá niêm yết để ra bậc độ lớn |
| Phát âm | Kiểm bằng bộ tách âm của Kokoro, chưa có người nghe lại toàn bộ |
