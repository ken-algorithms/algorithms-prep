# Bài 30 — Valid Anagram — kịch bản lời đọc (sinh tự động từ narration.py)

Giọng: `vi-VN-NamMinhNeural` · kiểu lời đọc: `phienam`

| Đoạn | Giây | Phụ đề | Lời đọc TTS |
|---|---|---|---|
| `intro` | 5.952 | Bài 30 · Valid Anagram (LeetCode #242) | Bài ba mươi. Va lít a na gram, lít cốt số hai trăm bốn mươi hai. |
| `problem` | 9.384 | Đề bài: t có phải anagram của s — cùng ký tự, cùng số lần xuất hiện, chỉ khác thứ tự? | Đề bài: cho hai chuỗi s và t. Kiểm tra t có phải là a na gram của s hay không, tức là cùng các ký tự, cùng số lần xuất hiện, chỉ khác thứ tự. |
| `example` | 7.584 | s = "anagram", t = "nagaram" → true (chỉ đổi chỗ các chữ cái) | Ví dụ: a na gram và na ga ram. Hai chuỗi chỉ đổi chỗ các chữ cái, nên kết quả là tru. |
| `naive` | 9.336 | Cách 1: sort cả hai chuỗi rồi so → đúng, nhưng tốn O(n log n) | Cách thứ nhất: sắp xếp cả hai chuỗi rồi so sánh. Giống nhau thì là a na gram. Nhưng sắp xếp tốn ô en lốc en. |
| `idea` | 9.84 | Cách 2: đếm tần suất từng ký tự bằng dictionary (Python: Counter) → O(n) | Cách tốt hơn: đếm mỗi ký tự xuất hiện bao nhiêu lần, bằng một bảng đếm, trong Pai thon là cao tơ. Chỉ cần duyệt mỗi chuỗi một lần, ô en. |
| `len` | 9.12 | Bước 1: len(s) = len(t) = 7 → đi tiếp (khác độ dài thì False ngay) | Bước một: so độ dài. Cả hai đều bảy ký tự, nên đi tiếp. Nếu độ dài khác nhau, trả về phon ngay. |
| `count_s` | 7.896 | Counter(s): a → 3, g → 1, m → 1, n → 1, r → 1 | Đếm chuỗi s: chữ a ba lần. Các chữ g, m, n, r mỗi chữ một lần. |
| `count_t` | 3.384 | Counter(t): đếm "nagaram" ra đúng bảng như vậy | Đếm chuỗi t, cũng ra đúng bảng như vậy. |
| `compare` | 5.64 | Counter(s) == Counter(t): so từng ký tự → khớp hết → True | So hai bảng đếm theo từng ký tự. Khớp hết, nên trả về tru. |
| `ex2` | 7.344 | s = "rat", t = "car": chữ t chỉ có ở s, chữ c chỉ có ở t → False | Với rát và ca: chữ tê chỉ có ở s, chữ xê chỉ có ở t. Hai bảng đếm khác nhau, nên trả về phon. |
| `py_intro` | 2.328 | Code Python — leetcode-38-bai/lc242-valid-anagram.py | Giờ xem code Pai thon. |
| `py_len` | 3.84 | len(s) != len(t) → False: chặn sớm, khỏi phải đếm | Khác độ dài thì trả về phon luôn, khỏi phải đếm. |
| `py_counter` | 5.952 | Counter(s) == Counter(t): tạo 2 dict đếm, Python so theo từng key | Dòng cuối tạo hai bảng đếm rồi so sánh. Pai thon so hai bảng theo từng khóa. |
| `java_intro` | 5.76 | Java 21 — groupc/ValidAnagram.java: 1 mảng int[26] thay cho 2 Counter | Bản gia va làm khác: chỉ dùng một mảng hai mươi sáu số nguyên, thay cho hai bảng đếm. |
| `java_loop` | 5.88 | Cùng một vòng lặp: ký tự của s → +1, ký tự của t → −1 | Trong cùng một vòng lặp, gặp ký tự của s thì cộng một, gặp ký tự của t thì trừ một. |
| `java_zero` | 8.304 | Anagram ⇔ cộng/trừ triệt tiêu, mọi ô về 0 → return true | Nếu là a na gram, cộng và trừ triệt tiêu nhau, mọi ô đều về không. Có ô nào khác không thì trả về phon. |
| `java_unicode` | 10.296 | Đánh đổi: int[26] chỉ đúng với chữ a–z. Có Unicode (vd. tiếng Việt có dấu) → dùng HashMap như Counter | Đánh đổi là: mảng hai mươi sáu ô chỉ đúng khi chuỗi chỉ có chữ thường a đến z. Nếu có ký tự u ni cốt, như tiếng Việt có dấu, phải dùng hát mép, giống cao tơ. |
| `complexity` | 7.68 | Time O(n) · Space O(1) — bảng chữ cái cố định 26 ký tự | Độ phức tạp: thời gian ô en. Bộ nhớ ô một, vì bảng chữ cái cố định hai mươi sáu ký tự. |
| `tradeoff` | 12.768 | Sort: O(n log n) · Counter/HashMap: O(n), mọi ký tự · int[26]: O(n), nhanh nhất nhưng chỉ a–z | Tóm lại: sắp xếp thì ô en lốc en. Bảng đếm thì ô en, dùng được cho mọi ký tự. Mảng hai mươi sáu ô cũng ô en, nhanh nhất, nhưng chỉ cho chữ a đến z. |
| `outro` | 3.768 | Tiếp theo: Bài 31 · Isomorphic Strings (#205) | Bài tiếp theo: ai xô mo phích, sờ trinh. |

Tổng lời đọc: 142.1s
