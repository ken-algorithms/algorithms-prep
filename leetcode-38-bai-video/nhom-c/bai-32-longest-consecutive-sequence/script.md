# Bài 32 — Longest Consecutive Sequence — kịch bản lời đọc (sinh tự động từ narration.py)

Giọng: `vi-VN-NamMinhNeural` · kiểu lời đọc: `phienam`

| Đoạn | Giây | Phụ đề | Lời đọc TTS |
|---|---|---|---|
| `intro` | 6.816 | Bài 32 · Longest Consecutive Sequence (LeetCode #128) | Bài ba mươi hai. Long ghét con sếch cu típ si quần, lít cốt số một trăm hai mươi tám. |
| `problem` | 10.536 | Đề bài: độ dài dãy số nguyên liên tiếp dài nhất — liên tiếp về giá trị, không cần kề nhau trong mảng. Yêu cầu O(n) | Đề bài: tìm độ dài dãy số nguyên liên tiếp dài nhất. Liên tiếp về giá trị, không cần nằm cạnh nhau trong mảng. Và phải chạy trong ô en. |
| `example` | 10.44 | [100, 4, 200, 1, 3, 2] → 4, vì có dãy 1, 2, 3, 4 | Ví dụ: một trăm, bốn, hai trăm, một, ba, hai. Kết quả là bốn, vì có dãy một, hai, ba, bốn. |
| `sort` | 7.896 | Cách 1: sort rồi đếm đoạn liền nhau → đúng, nhưng O(n log n) — chưa đạt yêu cầu | Cách thứ nhất: sắp xếp rồi đếm các đoạn liền nhau. Đúng, nhưng tốn ô en lốc en, chưa đạt yêu cầu. |
| `idea` | 6.696 | Cách 2: bỏ hết vào HashSet — hỏi "x có không?" chỉ O(1) | Cách thứ hai: bỏ hết vào một hát sét. Hỏi một số có trong sét hay không chỉ tốn ô một. |
| `key` | 5.928 | Mấu chốt: chỉ bắt đầu đếm ở ĐẦU dãy — khi num − 1 không có trong set | Mấu chốt: chỉ bắt đầu đếm ở đầu dãy, tức là khi số liền trước nó không có trong sét. |
| `w1` | 11.352 | num = 1: 0 ∉ set → đầu dãy. 2, 3, 4 có; 5 không → length = 4, best = 4 | Số một: số không không có trong sét, nên một là đầu dãy. Đếm tiếp: hai, ba, bốn đều có, năm thì không. Độ dài bốn. |
| `w2` | 6.096 | num = 2: 1 ∈ set → không phải đầu dãy → bỏ qua | Số hai: số một có trong sét, nên hai không phải đầu dãy. Bỏ qua. |
| `w3` | 3.36 | num = 3: 2 ∈ set → bỏ qua | Số ba: có số hai, bỏ qua. |
| `w100` | 8.208 | num = 100: 99 ∉ set → đầu dãy, nhưng 101 không có → length = 1 | Số một trăm: không có chín mươi chín, nên là đầu dãy. Nhưng một trăm lẻ một không có, độ dài chỉ là một. |
| `w4` | 3.456 | num = 4: 3 ∈ set → bỏ qua | Số bốn: có số ba, bỏ qua. |
| `w200` | 7.152 | num = 200: đầu dãy, length = 1 · hết set → return best = 4 | Số hai trăm: đầu dãy, độ dài một. Hết sét, trả về độ dài lớn nhất là bốn. |
| `trap` | 6.816 | Vì sao phải kiểm tra num − 1? Bỏ đi thì số nào cũng đếm tới cuối dãy | Vì sao phải kiểm tra số liền trước? Nếu bỏ đi, số nào cũng đếm tới tận cuối dãy. |
| `trap_count` | 9.336 | nums = 1…6: 6 + 5 + 4 + 3 + 2 + 1 = 21 bước = n(n+1)/2 → O(n²) | Với mảng từ một tới sáu: sáu cộng năm cộng bốn, cộng ba cộng hai cộng một, là hai mươi mốt bước. Tức là ô en bình phương. |
| `trap_fix` | 8.232 | Có kiểm tra: chỉ số 1 đi hết dãy → 6 bước. Mỗi số được "đi qua" đúng 1 lần → O(n) | Có kiểm tra, chỉ số một đi hết dãy, sáu bước. Mỗi số chỉ được đi qua đúng một lần, nên là ô en. |
| `py_intro` | 2.328 | Code Python — leetcode-38-bai/lc128-longest-consecutive-sequence.py | Giờ xem code Pai thon. |
| `py_set` | 4.752 | set(nums): bỏ số trùng + tra cứu O(1) | Dòng đầu tạo sét: vừa bỏ số trùng, vừa tra cứu ô một. |
| `py_start` | 3.456 | if num − 1 not in num_set: chỉ đầu dãy mới được đếm | Điều kiện này đảm bảo chỉ đầu dãy mới được đếm. |
| `py_while` | 4.584 | while num + length in num_set: đếm tới hết dãy → cập nhật best | Vòng oai đếm tới khi hết dãy, rồi cập nhật độ dài lớn nhất. |
| `java_intro` | 4.68 | Java 21 — groupc/LongestConsecutiveSequence.java: cùng thuật toán, tách hàm sequenceLengthFrom | Bản gia va dùng cùng thuật toán, và tách phần đếm ra một hàm riêng. |
| `java_start` | 6.6 | boolean isSequenceStart = !values.contains(value − 1): đặt tên cho điều kiện → dễ đọc, dễ giải thích | Điều kiện đầu dãy được đặt tên thành một biến. Dễ đọc, và dễ giải thích khi phỏng vấn. |
| `java_trap` | 9.816 | Bẫy: lặp trên values (set), đừng lặp trên nums — 1000 số 1 trùng: 999 → 999.000 bước | Cẩn thận: phải lặp trên sét, không lặp trên mảng gốc. Nếu mảng có một nghìn số một trùng nhau, số bước tăng từ chín trăm chín mươi chín lên gần một triệu. |
| `complexity` | 8.688 | Time O(n): mỗi số 1 lần kiểm tra num − 1 + tối đa 1 lần được đi qua · Space O(n) cho set | Độ phức tạp: thời gian ô en, vì mỗi số chỉ được kiểm tra một lần và đi qua tối đa một lần. Bộ nhớ ô en cho sét. |
| `tradeoff` | 14.112 | Sort: O(n log n) · Set không kiểm tra đầu dãy: O(n²) · Set + đầu dãy: O(n) · Union-Find: gần O(n), phức tạp hơn | Tóm lại: sắp xếp thì ô en lốc en. Dùng sét mà không kiểm tra đầu dãy thì ô en bình phương. Có kiểm tra thì ô en. Diu ni ơn phai cũng gần ô en, nhưng phức tạp hơn nhiều. |
| `outro` | 3.648 | Tiếp theo: Bài 33 · Subarray Sum Equals K (#560) | Bài tiếp theo: sấp a rây xăm i quần kê. |

Tổng lời đọc: 175.0s
