# algorithms-prep — workspace ôn phỏng vấn

Hai hướng ứng tuyển chạy song song, dùng chung phần lớn nội dung kỹ thuật.

```bash
# Bản một trang — dùng hằng ngày, có tick tiến độ  (đây là bản publish làm Artifact)
uv run --with markdown --with pymdown-extensions python3 web/build.py
open web/index.html

# Bản nhiều trang — tra cứu sâu, xem được cả file code
uv run --with markdown --with pymdown-extensions python3 build_site.py
open site/index.html
```

---

## Có gì ở đây

| Folder | Là gì |
|---|---|
| [**nab-prep/**](nab-prep/) ⭐ | **NAB Innovation Centre Vietnam** — Senior/Lead Java Engineer. Nghiên cứu công ty, JD, quy trình 5 vòng, gap analysis, kế hoạch 5 tuần |
| [**katalon-prep/**](katalon-prep/) | **Katalon** — Senior/Lead. Câu trả lời theo CV, stack AI, 6 bài system design, workspace code 313 test. **Mới:** [self-host LLM](katalon-prep/katalon-selfhost-llm.md) — hướng tuyển đã dịch sang AI Engineer |
| [katalon-prep/katalon-system-design/](katalon-prep/katalon-system-design/) | **Dùng chung cho cả hai hướng** — 7 họ bài, 5 trục nhận diện, 6 bài design đầy đủ |
| [leetcode-38-bai/](leetcode-38-bai/) · [leetcode-38-bai-java/](leetcode-38-bai-java/) | 38 bài DSA, cả Python và Java |
| [leetcode-38-bai-phong-van-vietnam.md](leetcode-38-bai-phong-van-vietnam.md) | Tài liệu DSA gốc |
| [web/](web/) | **Bản một trang** — tổng quan, công ty, 38 bài có tick, system design, đọc tài liệu |

Tiếng Anh có lộ trình riêng ở `../ielts-target-5-5/` ([bản web](https://claude.ai/code/artifact/9569184e-cc2d-43ce-b0e7-c4c0183af03b)) — 40 tuần, giáo án
từng ngày, có bản web đồng hành. [`nab-prep/05`](nab-prep/05-english-interview.md) chỉ lo phần
**kịch bản phỏng vấn kỹ thuật**, không thay thế việc xây nền.

---

## Trang web tổng hợp

`build_site.py` dựng một **site tĩnh, chạy offline hoàn toàn** từ mọi file `.md` trong repo:

- Sidebar gom nhóm theo folder, **tìm kiếm toàn văn** (bấm `/`)
- Mục lục trong trang, chế độ **sáng/tối**
- Link `.md` được viết lại thành `.html`; **link tới file code cũng xem được ngay trên web**
- Không CDN, không script ngoài — mở bằng `file://` là chạy

Chạy lại lệnh build mỗi khi thêm hoặc sửa tài liệu.

```bash
uv run --with markdown --with pymdown-extensions python3 build_site.py
```

> `site/` là **thư mục sinh ra**, có thể xoá và dựng lại bất cứ lúc nào.

---

## Nguyên tắc chung của workspace này

Ba nguyên tắc áp cho mọi tài liệu ở đây — chúng là lý do bộ tài liệu này dùng được trong phòng
phỏng vấn:

1. **Chạy được thì phải chạy.** Không viết "code này sẽ deadlock" — chạy rồi ghi output thật.
2. **Phân biệt rõ đã kiểm chứng / suy luận / chưa đo.** Gần như mọi file đều có mục
   *"ranh giới trung thực"* ở cuối. **Đọc mục đó trước khi vào phòng.**
3. **Mỗi lựa chọn kiến trúc kèm một câu "đánh đổi là…"** — không có câu đó thì nghe như Senior,
   không phải Lead.
