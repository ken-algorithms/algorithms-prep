# katalon-self-questions — câu hỏi tự ghi

> Chỗ để **bạn** tự thêm câu hỏi trong quá trình ôn: câu tự nghĩ ra, câu nghe được, câu bị hỏi mà
> trả lời chưa tốt, hay đơn giản là khái niệm muốn ghi lại cho chắc.
>
> Khác với [../katalon-system-design/](../katalon-system-design/) — bên kia là **đề bài lớn** có
> kiến trúc và đánh đổi. Bên này là **câu hỏi đơn lẻ**, ngắn hơn, tra nhanh hơn.
>
> Cha: [../README.md](../README.md)

---

## Danh sách câu hỏi

| # | Câu hỏi | Ngôn ngữ ghi chú | Có code chạy được |
|---|---|:---:|:---:|
| [01](01-deadlock.md) | **Deadlock là gì? Reproduce thế nào? Vì sao xảy ra?** | 🇻🇳 + 🇬🇧 | ✅ [code/](code/) — Java · Python (đã chạy) |

---

## Quy ước cho câu hỏi mới

Mỗi câu một file `NN-ten-ngan.md`. Không nhồi nhiều câu vào một file — tra nhanh quan trọng hơn
gọn file.

**Khung khuyến nghị** (bỏ bớt mục nào không cần, đừng bịa cho đủ):

```markdown
# NN — <Câu hỏi>

> Vì sao ghi câu này · Ngày ghi · Mức ưu tiên

## 1. Trả lời ngắn (30 giây)      ← cái nói ra trong phòng
## 2. Định nghĩa chính xác        ← cái phải đúng, không được mơ hồ
## 3. Reproduce / chứng minh      ← CODE CHẠY ĐƯỢC, không phải mô tả
## 4. Vì sao xảy ra               ← cơ chế, không phải hiện tượng
## 5. Cách phát hiện              ← công cụ cụ thể, có lệnh
## 6. Cách chữa + đánh đổi        ← mỗi cách kèm một câu "mất gì"
## 7. Phân biệt với khái niệm gần ← chỗ hay bị lẫn
## 8. Trả lời bằng tiếng Anh      ← nếu vòng phỏng vấn dùng tiếng Anh
## 9. Follow-up hay bị hỏi tiếp
```

**Ba nguyên tắc** (giữ đúng tinh thần cả workspace này):

1. **Chạy được thì phải chạy.** Đừng viết "code này sẽ deadlock" — chạy thử rồi ghi kết quả thật.
2. **Nói rõ cái gì đã kiểm chứng, cái gì mới là suy luận.** Có một mục ranh giới trung thực nếu cần.
3. **Mỗi giải pháp kèm một đánh đổi.** Không có câu "đánh đổi là..." thì chưa xong.

---

## Khi nào viết ở đây, khi nào viết ở chỗ khác

| Loại nội dung | Viết ở |
|---|---|
| Câu hỏi đơn lẻ, khái niệm, "cái này là gì / vì sao vỡ" | **Folder này** |
| Đề bài system design có kiến trúc + đánh đổi | [../katalon-system-design/](../katalon-system-design/) |
| Khái niệm nền có code chạy được ở cả Java và Python | [../katalon-prep-common/](../katalon-prep-common/) |
| Câu hỏi về dự án AI của bạn | [../AI-STACK-INTERVIEW-ANSWERS.md](../AI-STACK-INTERVIEW-ANSWERS.md) |
| Câu hỏi về CV / hành vi / lương | [../CV-BASED-ANSWERS.md](../CV-BASED-ANSWERS.md) · [../SCREENING-CALL-ANSWERS.md](../SCREENING-CALL-ANSWERS.md) |
