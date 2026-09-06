# Module 07 — DSA drill (TẮT COPILOT)

```bash
mvn -pl 07-dsa-drill test    # in lịch 5 tuần ra màn hình
```

Module này **không chứa lời giải** — lời giải đã có đủ ở
[leetcode-38-bai-phong-van-vietnam.md](../../../leetcode-38-bai-phong-van-vietnam.md) (Python + Java 21).

Mục đích khác hẳn: một chỗ để bạn **viết lại từ đầu, không nhìn, không AI**, có test chạy ngay.
Đây chính là cách bịt **rủi ro #1** của việc quen dùng AI gen code: *vòng live coding không có Copilot*.

## Cách dùng

1. **Tắt Copilot/Cursor** (`Cmd+Shift+P` → "Disable Copilot completions").
2. Mở [`DrillWorkspaceTest.java`](src/test/java/com/prep/drill/DrillWorkspaceTest.java), **xoá dòng
   `@Disabled`** của nhóm bạn đang luyện.

3. Bấm giờ, viết trong [`DrillWorkspace.java`](src/main/java/com/prep/drill/DrillWorkspace.java).
4. `mvn -pl 07-dsa-drill test`
5. **Chỉ khi xong (hoặc hết giờ)** mới mở file `.md` đối chiếu, rồi ghi lại chỗ mình viết kém hơn.

Mặc định tất cả `@Disabled` để `mvn test` ở thư mục gốc vẫn xanh (18 test skipped).

## 20 bài on-domain, không phải 38

Vòng coding của Katalon **không** phải LeetCode-hard: 3–4 vòng, có take-home, nội dung là case study

+ discussion. DSA Medium chỉ là **điều kiện để không bị loại**. Đừng grind thêm 200 bài — dồn thời

gian sang take-home.

Mỗi bài đều kèm **liên hệ với domain Katalon** để nhớ lâu hơn và để nói ra được khi phỏng vấn:

| Bài | Liên hệ |
|---|---|
| **#207** Course Schedule | **thứ tự phụ thuộc giữa test** — bài on-domain nhất |
| **#253** Meeting Rooms II | **xếp test song song vào executor pool** — nói được liên hệ này là điểm cộng lớn |
| #236 Lowest Common Ancestor | tìm container chung gần nhất của 2 element → self-healing locator |
| #102 / #105 / #297 | duyệt / dựng lại / serialize **DOM tree** |
| #146 LRU Cache | cache DOM snapshot, kết quả locator |
| #3 / #76 Sliding Window | phân tích log theo cửa sổ thời gian |
| #560 / #523 Prefix Sum | đếm cửa sổ, phát hiện chu kỳ trong chuỗi event |

Lịch 5 tuần × 4 bài, ~7 giờ tổng. Chạy `mvn -pl 07-dsa-drill test` để in ra.

## Format bắt buộc — đây mới là thứ được chấm điểm

Interviewer đánh giá **quá trình** nhiều hơn **kết quả**. Ứng viên clarify tốt, nêu naive rồi tối ưu,
nói to suốt quá trình — sẽ qua vòng **kể cả khi không kịp xong**. Ngược lại, ứng viên im lặng 15 phút
rồi đưa đáp án đúng thường bị đánh giá thấp hơn.

```text
1. CLARIFY (2')     input rỗng? âm? trùng? kết quả có cần ổn định thứ tự?
2. VÍ DỤ (1')       tự viết 1 ví dụ nhỏ + 1 edge case, chạy tay
3. NAIVE (2')       nói ra O(n²) trước — ĐỪNG bỏ qua bước này
4. BOTTLENECK (1')  chỉ rõ chỗ nào chậm và VÌ SAO
5. TỐI ƯU (10')     vừa code vừa NÓI TO
6. COMPLEXITY (1')  time + space, giải thích chứ không đọc thuộc
7. TEST (3')        tự nghĩ edge case: rỗng, 1 phần tử, tất cả giống nhau, giá trị âm
```

Test trong module đã kèm sẵn **edge case** cho từng bài — đọc trước cũng được, mục tiêu là tạo phản
xạ chứ không phải đánh đố.

## Checklist

- [ ] Giải #1 Two Sum dưới 10 phút, viết tay, không tra cứu
- [ ] Giải #253 và **tự nói ra** liên hệ với executor pool
- [ ] Giải #207 và **tự nói ra** liên hệ với test dependency
- [ ] Tự quay màn hình 2 bài, xem lại xem mình có nói to đủ không
