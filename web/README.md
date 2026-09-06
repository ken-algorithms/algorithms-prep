# web — bản một trang "Algorithms Learning"

**Artifact:** https://claude.ai/code/artifact/51b40ad7-c7ed-434f-9de6-56ddf4079a77

Một trang duy nhất, chạy hoàn toàn ngoại tuyến. Toàn bộ 45 tài liệu markdown được nhúng
thẳng vào HTML nên đọc được cả kho mà không cần mạng.

```bash
cd /Users/duc.nguyen/data/projects/success/motives/motivesidp-ai-learning/algorithms-prep
uv run --with markdown --with pymdown-extensions python3 web/build.py
```

Chạy lại build sau **mỗi lần sửa `app.template.html` hoặc bất kỳ file `.md` nào**.

## Năm tab

| Tab | Nội dung |
|---|---|
| **Tổng quan** | Số bài đã gõ tay, số yêu cầu JD đã sẵn sàng, dải độ phủ 38 bài (một vạch = một bài), hai thẻ hướng ứng tuyển với đủ các vòng, và danh sách gap đỏ cần làm trước |
| **Công ty** | NAB và Katalon tách riêng: stack, các vòng phỏng vấn đánh số, và **checklist yêu cầu JD tick được** — mỗi dòng ghi rõ mạnh / cần ôn / gap kèm lý do. Bấm **Mở** để nhảy thẳng tới tài liệu chứng minh |
| **Thuật toán** | 38 bài, lọc theo nhóm A/B/C, độ khó, hoặc chỉ hiện bài chưa làm. Tick là **đã gõ tay xong, tắt Copilot** — không phải đã đọc lời giải. Bấm tên bài mở thẳng phần phân tích |
| **System design** | 5 trục nhận diện họ bài, bảng 7 họ (chữ ký · lõi · bẫy), và 6 bài design đầy đủ |
| **Tài liệu** | Toàn bộ 45 file, nhóm theo folder, tìm kiếm **không dấu** trên toàn văn, link giữa các tài liệu bấm được |

Bấm `/` ở bất kỳ đâu để nhảy vào ô tìm kiếm.

## Ba file, ba vai trò

| File | Vai trò |
|---|---|
| `app.template.html` | **Bản nguồn giao diện và mã — sửa ở đây.** Có 5 chỗ đánh dấu cho build chèn dữ liệu: `/*__DOCS__*/`, `/*__ALGOS__*/`, `/*__CO__*/`, `/*__FAMS__*/`, `/*__AXES__*/` |
| `build.py` | Render mọi `.md` sang HTML, đọc bảng 38 bài từ `leetcode-38-bai-phong-van-vietnam.md`, giữ dữ liệu công ty và 7 họ bài, rồi nhúng tất cả vào bản nguồn |
| `algorithms-learning.html` | Bản dựng để publish làm Artifact — không có `<!doctype>`/`<html>`/`<head>`/`<body>` vì nền tảng tự bọc. **Sinh tự động, đừng sửa tay** |
| `index.html` | Bản dựng standalone, mở trực tiếp bằng trình duyệt. **Sinh tự động, đừng sửa tay** |

## Nguồn dữ liệu

| Dữ liệu | Lấy từ đâu |
|---|---|
| 38 bài thuật toán | **Parse từ bảng mục lục** trong `leetcode-38-bai-phong-van-vietnam.md` — một nguồn duy nhất, sửa bảng đó là web đổi theo |
| Nội dung tài liệu | Mọi file `.md` trong repo (trừ `site/`, `web/`, `.git/`) |
| Yêu cầu JD, các vòng, gap | Khai trong `build.py`, soạn từ `nab-prep/02-gap-analysis.md` và `katalon-prep/` |
| 7 họ bài, 5 trục | Khai trong `build.py`, soạn từ `katalon-prep/katalon-system-design/README.md` |

Sửa gap hoặc thêm vòng phỏng vấn thì sửa `COMPANIES` trong `build.py`, không sửa HTML.

## Tiến độ lưu ở đâu

`localStorage`, khoá `algo-learning-v1` — nằm trong **đúng trình duyệt đó**, không đồng bộ
giữa máy và điện thoại. Xoá dữ liệu duyệt web là mất. Nút **Xoá tiến độ** ở cột trái đặt
lại về rỗng.

Chủ đề sáng/tối lưu riêng ở `algo-learning-v1:theme`, ba trạng thái: theo máy → sáng → tối.

## Khác gì `../site/`

| | `web/` (bản này) | `../site/` |
|---|---|---|
| Số trang | 1 | 165 |
| Theo dõi tiến độ | **Có** | Không |
| Tổng quan công ty, checklist JD | **Có** | Không |
| Xem được file code `.java`/`.py` | Không | **Có** |
| Dùng khi | Học hằng ngày, trên điện thoại | Tra cứu sâu, cần đọc source |
