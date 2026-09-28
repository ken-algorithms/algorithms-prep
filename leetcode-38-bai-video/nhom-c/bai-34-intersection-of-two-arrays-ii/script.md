# Bài 34 — Intersection of Two Arrays II — kịch bản lời đọc (sinh tự động từ narration.py)

Giọng: `vi-VN-NamMinhNeural` · kiểu lời đọc: `phienam`

| Đoạn | Giây | Phụ đề | Lời đọc TTS |
|---|---|---|---|
| `intro` | 6.888 | Bài 34 · Intersection of Two Arrays II (LeetCode #350) | Bài ba mươi tư. In tơ séc sần ốp tu ơ rây tu, lít cốt số ba trăm năm mươi. |
| `problem` | 9.288 | Đề bài: giao của 2 mảng, GIỮ số lần lặp — số nào xuất hiện ở cả hai bao nhiêu lần thì lấy bấy nhiêu | Đề bài: tìm phần giao của hai mảng, và giữ đúng số lần lặp. Một số xuất hiện ở cả hai mảng bao nhiêu lần, thì lấy bấy nhiêu lần. |
| `example` | 12.384 | [1, 2, 2, 1] ∩ [2, 2] → [2, 2] · bài #349 (chỉ lấy giá trị duy nhất) sẽ ra [2] | Ví dụ: một, hai, hai, một, giao với hai, hai, ra hai, hai. Khác bài ba trăm bốn mươi chín, chỉ lấy giá trị duy nhất, sẽ ra một số hai. |
| `naive` | 7.944 | Cách 1: mỗi số của nums2 đi tìm rồi xoá trong nums1 → O(n · m) | Cách thứ nhất: với mỗi số của mảng hai, đi tìm nó trong mảng một, thấy thì xóa đi. Tốn ô en nhân em. |
| `idea` | 8.808 | Cách 2: đếm nums1 bằng Counter = số "suất" của mỗi giá trị · lấy một lần → trừ 1 suất | Cách tốt hơn: đếm mảng một bằng cao tơ. Mỗi giá trị có bấy nhiêu suất. Mỗi lần lấy thì trừ đi một suất. |
| `c0` | 5.256 | counts = Counter(nums1): 4 → 2 suất, 9 → 1, 5 → 1 | Đếm mảng một: số bốn có hai suất, số chín một suất, số năm một suất. |
| `w0` | 4.752 | num = 9: còn 1 suất → lấy · 9 còn 0 | Số chín: còn một suất, lấy. Chín hết suất. |
| `w1` | 4.704 | num = 4: còn 2 suất → lấy · 4 còn 1 | Số bốn: còn hai suất, lấy. Còn một. |
| `w2` | 6.288 | num = 9: hết suất (0) → bỏ qua — nums1 chỉ có một số 9 | Lại số chín: hết suất rồi, bỏ qua. Vì mảng một chỉ có một số chín. |
| `w3` | 5.472 | num = 8: không có trong nums1 → counts[8] = 0 → bỏ qua | Số tám: không có trong mảng một, đọc ra không. Bỏ qua. |
| `w4` | 6.384 | num = 4: còn 1 suất → lấy · kết quả [9, 4, 4] | Số bốn: còn một suất, lấy. Kết quả là chín, bốn, bốn. |
| `py_intro` | 2.328 | Code Python — leetcode-38-bai/lc350-intersection-of-two-arrays-ii.py | Giờ xem code Pai thon. |
| `py_counter` | 3.84 | Counter(nums1): đếm một lần, O(n) | Dòng đầu đếm mảng một bằng cao tơ, một lần duyệt. |
| `py_check` | 8.712 | counts[num] > 0: còn suất mới lấy · đọc Counter[x] KHÔNG tạo key — khác defaultdict ở bài 33 | Chỉ lấy khi còn suất. Và đọc cao tơ bằng ngoặc vuông không tạo khóa mới. Khác với đi phôn đích ở bài ba mươi ba. |
| `py_order` | 5.88 | Kết quả theo thứ tự của nums2 — đề cho phép trả về thứ tự bất kỳ | Kết quả đi theo thứ tự của mảng hai. Đề cho phép trả về thứ tự bất kỳ. |
| `java_intro` | 5.592 | Java 21 — groupc/IntersectionOfTwoArrays.java: merge để đếm, getOrDefault để đọc | Bản gia va: dùng mơ giơ để đếm, và ghét o đi phôn để đọc số suất còn lại. |
| `java_put` | 7.056 | put(num, available − 1): trừ suất · cuối cùng stream() đổi List<Integer> → int[] | Lấy xong thì ghi lại số suất trừ một. Cuối cùng, đổi danh sách sang mảng số nguyên bằng sờ trim. |
| `fu_sorted` | 6.84 | Follow-up 1: cả 2 mảng đã sort → hai con trỏ, bộ nhớ thêm O(1) | Câu hỏi mở rộng thứ nhất: nếu cả hai mảng đã được sắp xếp, dùng hai con trỏ, không cần bộ nhớ thêm. |
| `fu_small` | 8.52 | Follow-up 2: nums1 nhỏ hơn nhiều → đếm mảng NHỎ → bộ nhớ O(min(n, m)) — code này luôn đếm nums1 | Thứ hai: nếu một mảng nhỏ hơn nhiều, hãy đếm mảng nhỏ, bộ nhớ chỉ bằng mảng nhỏ. Code hiện tại luôn đếm mảng một. |
| `fu_disk` | 7.656 | Follow-up 3: nums2 quá lớn, nằm trên đĩa → giữ Counter mảng nhỏ trong RAM, đọc nums2 từng khúc | Thứ ba: nếu mảng hai quá lớn, nằm trên đĩa, giữ bảng đếm của mảng nhỏ trong bộ nhớ, và đọc mảng hai từng khúc. |
| `complexity` | 9.912 | Time O(n + m) · Space O(n) theo nums1 — đếm mảng nhỏ hơn thì O(min(n, m)) | Độ phức tạp: thời gian ô en cộng em. Bộ nhớ ô en theo mảng một. Nếu đếm mảng nhỏ hơn, thì chỉ bằng mảng nhỏ. |
| `outro` | 3.0 | Tiếp theo: Bài 35 · Happy Number (#202) | Bài tiếp theo: háp pi năm bơ. |

Tổng lời đọc: 147.5s
