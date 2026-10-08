# lesson_video — dựng video bài giảng từ kịch bản YAML

Công cụ dựng bộ video tiếng Anh cho lộ trình Java System Design: slide 1280×720 vẽ bằng Pillow, giọng
**Kokoro-82M** (tốc độ 0,9), ghép bằng ffmpeg thành MP4 H.264 + AAC có chương. Lấy từ
công cụ `agents/lesson_video` của repo
[superken-ielts/ielts-target-5-5](https://github.com/superken-ielts/ielts-target-5-5) (phần đọc giọng, ghép
tiếng, mã hoá), viết mới phần slide cho nội dung system design.

Hai bộ video, mỗi bộ một thư mục `java-system-design/<bộ>/lessons/` có kế hoạch, bảng trạng thái và nhật ký riêng:

| Bộ | Giọng | Kế hoạch |
|---|---|---|
| Giai đoạn 1 (tuần 1–4) | Tom, `am_michael` | [video-gd1/00-ke-hoach-va-lich-su.md](../../java-system-design/video-gd1/00-ke-hoach-va-lich-su.md) |
| Giai đoạn 2 (tuần 5–10) | Emma, `af_heart` | [video-gd2/00-ke-hoach-va-lich-su.md](../../java-system-design/video-gd2/00-ke-hoach-va-lich-su.md) |

File `series.yaml` cạnh kịch bản (nếu có) chứa giá trị chung của cả bộ: `series` (dòng chữ góc phải slide),
`speakers` (người đọc, màu, giọng), `sources`, `gap`, `scene_gap`. Kịch bản ghi đè được từng khoá.

## Chạy

```bash
cd tools
R="uv run --with-requirements lesson_video/requirements.txt python3 -m lesson_video"
L=../java-system-design/video-gd1/lessons

$R check $L/ep01-*.yaml --glossary ../java-system-design/video-gd1/01-bang-chu-viet-tat.md
# bộ giai đoạn 2: kiểm với cả hai bảng (--glossary lặp lại được)
$R check ../java-system-design/video-gd2/lessons/ep*.yaml --glossary ../java-system-design/video-gd1/01-bang-chu-viet-tat.md \
                                                          --glossary ../java-system-design/video-gd2/01-bang-chu-viet-tat.md
$R frames $L/ep01-estimation-toolkit.yaml --out /tmp/frames     # ảnh cuối mỗi cảnh, không cần model
$R phon $L/ep01-estimation-toolkit.yaml                         # phiên âm các câu có số, ký hiệu
$R build $L/ep01-estimation-toolkit.yaml                        # Kokoro → ep01-….mp4 + .jpg + lessons.json
$R coverage $L --points $L/points.yaml --week 1 --out ../java-system-design/video-gd1/02-do-phu-tuan-1.md
uv run --with-requirements lesson_video/requirements.txt --with pytest python3 -m pytest lesson_video/tests -q
```

`check` kiểm: cú pháp, anchor trong `covers` có thật trong file nguồn, mỗi ý chính khai ở `points:` có đủ chữ
bắt buộc (`expect` trong `points.yaml`) ngay trong cảnh đó, và (khi có `--glossary`) mọi chữ viết tắt trên
màn hình hay trong lời đọc đều có trong bảng chữ viết tắt **và** trong thẻ *Acronyms in this video* của video.

## Model Kokoro

Không commit model. Đặt ở `~/.cache/lesson_video/kokoro/` (hoặc `--model`, `--voices`, biến `KOKORO_MODEL`,
`KOKORO_VOICES`). Hugging Face bị chặn trong môi trường cloud nên lấy từ npm:

```bash
npm pack kokoro-q8-shards@1.0.0 kokoro-js@1.2.1
tar xzf kokoro-q8-shards-1.0.0.tgz && tar xzf kokoro-js-1.2.1.tgz --one-top-level=kjs
mkdir -p ~/.cache/lesson_video/kokoro/voices
cat package/kokoro-q8.part{0,1,2,3,4,5}.bin > ~/.cache/lesson_video/kokoro/model_quantized.onnx
sha256sum ~/.cache/lesson_video/kokoro/model_quantized.onnx   # fbae9257e1e05ffc727e951ef9b9c98418e6d79f1c9b6b13bd59f5c9028a1478
cp kjs/package/voices/am_michael.bin kjs/package/voices/af_heart.bin ~/.cache/lesson_video/kokoro/voices/
```

Tiếng từng câu lưu ở `~/.cache/lesson_video/tts/` theo (bộ đọc, giọng, chữ): dựng lại chỉ đọc câu đã đổi.
Trên CPU 4 nhân, Kokoro đọc chậm hơn thời gian thật khoảng 1,6 lần; hai lượt dựng song song dùng hết CPU.

## Kịch bản

Một file `epNN-<slug>.yaml` cho mỗi video, cạnh file `points.yaml`. Khung:

```yaml
id: ep01-estimation-toolkit
ep: 1
week: 1
title: "The estimation toolkit"
subtitle: "Conversions, availability nines, latency numbers, and Little's Law"
covers: [10-implement-gd1-nen-tang.md#11-quy-đổi-phải-nhẩm-được]   # tính từ java-system-design/
scenes:
  - kind: table
    chapter: "Conversions"          # thành một chương trong MP4 và trên web
    heading: "Conversions to do in your head"
    columns: ["Quantity", "Remember as"]
    rows: [["1 day", "86,400 s ≈ 10⁵ s"]]
    points: [conv-day]              # ý chính dạy trong cảnh này (points.yaml)
    lines:
      - tom: "One day is 86,400 seconds. Round it to 10^5."    # phụ đề
        say: "One day is eighty-six thousand four hundred seconds. Round it to ten to the fifth."
        focus: 0                    # hàng / mục đang nói (giữ tới dòng sau)
      - wait: 5                     # đếm ngược "Pause the video and try it yourself"
        note: "Write your assumptions first."
```

| Kiểu cảnh | Dùng cho | Khoá riêng |
|---|---|---|
| `title` | Mở đầu: tên, chương của video, người đọc, nguồn | `items: [{t}]` |
| `acronyms` | Thẻ chữ viết tắt đầu video | `items: [{abbr, full, vi}]` |
| `bullets` | Danh sách, checklist | `items: [{t, s}]`, `style: num\|dot\|check`, `build` |
| `table` | Bảng, phép tính từng bước | `columns`, `rows`, `widths`, `mono`, `accent`, `build`, `note` |
| `code` | Code có tô dòng và chú thích | `code`, `items: [{lines: [a, b], t}]` |
| `pattern` | Cặp code xấu / sửa + số đo | `bad`, `good`, `stats`, `stats_columns` |
| `compare` | 2–3 cột so sánh | `items: [{title, lines, tone}]` |
| `flow` | Các bước nối mũi tên | `items: [{t, s, f}]`, `build` |
| `diagram` | Sơ đồ hộp–mũi tên hiện dần | `nodes: [{id, label, sub, x, y, w, h, shape, at, tone}]`, `edges: [{a, b, label, dashed, at}]` |
| `stats` | Con số lớn | `items: [{v, l, s, tone}]` |
| `exercise` | Đề bài + dữ kiện, đi với `wait` | `n`, `q`, `items: [{t}]` |
| `speech` | Bài nói mẫu, tô câu đang đọc | `items: [{t, say}]`, dòng `read: tom` |

Câu không có `say` vẫn được đổi cho dễ đọc (`speak.py`): số thập phân, dải `P01–P07`, đơn vị `ms`/`KB`/`MB/s`,
`~`, `≈`, `→`, `>`, `<`, `≠`, lũy thừa `10⁵`, tên cấu hình có dấu chấm (`max.poll.records`), mã lab (`lab 5B`)
và vài chữ Kokoro đọc sai (PACELC, ReDoS, SaaS, OIDC, DDIA, SLO, ISR, eKYC, etcd, draw.io, retryable…).
Chữ "A" viết hoa đứng riêng **giữa câu** (`relay A`, `topic A`, `J P A`) đọc là chữ cái (`eigh`); đầu câu thì
giữ nguyên vì thường là mạo từ ("A poll returns…"), nên đừng mở câu bằng nhãn A. Viết tắt đánh vần cũng
đọc đúng (`D A U` → `D eigh U`).

Bảng tự chọn cỡ chữ theo chỗ trống, và thu nhỏ thêm khi một từ không xuống dòng được (tên test, tên hàm)
dài hơn cột; muốn chữ to thì cho từ dài xuống dòng bằng `\n` (ví dụ sau dấu `_`).
