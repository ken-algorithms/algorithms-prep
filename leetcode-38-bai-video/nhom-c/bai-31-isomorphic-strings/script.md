# Bài 31 — Isomorphic Strings — kịch bản lời đọc (sinh tự động từ narration.py)

Giọng: `vi-VN-NamMinhNeural` · kiểu lời đọc: `phienam`

| Đoạn | Giây | Phụ đề | Lời đọc TTS |
|---|---|---|---|
| `intro` | 6.504 | Bài 31 · Isomorphic Strings (LeetCode #205) | Bài ba mươi mốt. Ai xô mo phích, sờ trinh, lít cốt số hai trăm linh năm. |
| `problem` | 9.192 | Đề bài: có phép thay ký tự 1–1 biến s thành t không? Mỗi ký tự s ↔ đúng 1 ký tự t | Đề bài: có cách thay từng ký tự của s để được t hay không. Mỗi ký tự của s phải ghép với đúng một ký tự của t, và ngược lại. |
| `example` | 7.944 | s = "egg", t = "add": e → a, g → d → true | Ví dụ: e giê giê và a đê đê. e thành a, giê thành đê, nhất quán, nên là tru. |
| `ex_false` | 8.568 | s = "foo", t = "bar": o → a rồi o → r — một ký tự ghép 2 nơi → false | Còn ép o o và bê a e rờ: chữ o lúc thì thành a, lúc thành e rờ. Mâu thuẫn, nên là phon. |
| `idea` | 7.92 | Ý tưởng: ghi lại các cặp đã ghép bằng dictionary, gặp lại thì kiểm tra có khớp không | Ý tưởng: dùng từ điển ghi lại các cặp đã ghép. Gặp lại một ký tự, kiểm tra nó có ghép đúng như cũ không. |
| `p0` | 6.072 | p → t: cả 2 map đều chưa có → ghi p→t và t→p | Chữ pê ghép với tê. Cả hai máp đều chưa có, nên ghi vào cả hai. |
| `p1` | 3.192 | a → i: mới → ghi vào | a ghép với i. Mới, ghi vào. |
| `p2` | 7.128 | p → t: map_st có p → t, map_ts có t → p → khớp ✓ | Lại là pê với tê. Cả hai máp đều đã có, và đều khớp. Đi tiếp. |
| `p3` | 3.72 | e → l: mới → ghi vào | e ghép với e lờ. Ghi vào. |
| `p4` | 6.288 | r → e: mới → ghi vào · hết chuỗi, không mâu thuẫn → return True | e rờ ghép với e. Ghi vào. Hết chuỗi mà không có mâu thuẫn, trả về tru. |
| `trap` | 8.424 | Vì sao cần 2 map? s = "badc", t = "baba": chỉ map s → t thì không thấy mâu thuẫn nào | Vì sao cần hai máp? Thử bê a đê xê và bê a bê a. Nếu chỉ có máp từ s sang t, ta không thấy mâu thuẫn nào. |
| `trap_why` | 8.064 | Nhưng b và d cùng ghép vào b — hai ký tự s chung một ký tự t → không phải 1–1 | Nhưng cả bê và đê đều ghép vào bê. Hai ký tự của s dùng chung một ký tự của t, nên không phải một đổi một. |
| `two_maps` | 7.08 | Nên cần thêm map_ts (t → s) để kiểm tra chiều ngược lại — song ánh | Nên phải có thêm máp từ t ngược về s, để kiểm tra cả chiều ngược lại. Đó là song ánh. |
| `b0` | 4.92 | b → b: chưa có → ghi vào cả 2 map | bê ghép với bê. Chưa có, ghi vào cả hai máp. |
| `b1` | 2.496 | a → a: chưa có → ghi vào | a ghép với a. Ghi vào. |
| `b2` | 11.592 | d → b: map_st chưa có d, nhưng map_ts nói b đã thuộc về b ≠ d → return False | đê ghép với bê. Máp s sang t chưa có đê. Nhưng máp t sang s nói bê đã thuộc về bê, không phải đê. Mâu thuẫn, trả về phon. |
| `py_intro` | 2.328 | Code Python — leetcode-38-bai/lc205-isomorphic-strings.py | Giờ xem code Pai thon. |
| `py_maps` | 4.824 | 2 dict: map_st (s → t) và map_ts (t → s) | Hai từ điển: một cho chiều s sang t, một cho chiều t sang s. |
| `py_checks` | 7.104 | 2 điều kiện False: cs đã ghép với ký tự khác, hoặc ct đã bị ký tự khác chiếm | Hai điều kiện trả về phon: ký tự của s đã ghép với ký tự khác, hoặc ký tự của t đã bị ký tự khác chiếm. |
| `py_put` | 6.048 | Hợp lệ → ghi cả 2 chiều · duyệt hết → True | Hợp lệ thì ghi cả hai chiều. Duyệt hết mà không mâu thuẫn, trả về tru. |
| `java_intro` | 3.72 | Java 21 — groupc/IsomorphicStrings.java: cùng thuật toán, 2 HashMap<Character, Character> | Bản gia va dùng cùng thuật toán, với hai hát mép. |
| `java_get` | 6.432 | get() trả null nếu chưa có — thay cho phép kiểm tra 'in' của Python | Hàm ghét trả về nâu nếu chưa có khóa. Nó thay cho phép kiểm tra in của Pai thon. |
| `java_box` | 12.576 | Bẫy: Character != char → so giá trị (unboxing) ✓. Hai Character với nhau thì != so địa chỉ → phải dùng equals() | Cẩn thận: ở đây ta so một đối tượng ca rác tơ với một ký tự thường, nên gia va so theo giá trị, đúng. Nhưng nếu cả hai đều là ca rác tơ, dấu khác sẽ so địa chỉ, phải dùng hàm i quồ. |
| `complexity` | 7.872 | Time O(n) · Space O(1) — tối đa số ký tự của bảng mã (ASCII: 256) | Độ phức tạp: thời gian ô en. Bộ nhớ ô một, vì mỗi máp tối đa bằng số ký tự của bảng mã. |
| `tradeoff` | 10.344 | 2 HashMap: mọi ký tự · 1 HashMap + HashSet đã dùng: tương đương · 2 mảng int[256]: nhanh nhất, chỉ ASCII | Các cách khác: một hát mép cộng một hát sét các ký tự đã dùng, tương đương. Hoặc hai mảng hai trăm năm mươi sáu ô, nhanh nhất, nhưng chỉ cho a xơ ki. |
| `outro` | 3.816 | Tiếp theo: Bài 32 · Longest Consecutive Sequence (#128) | Bài tiếp theo: long ghét con sếch cu típ si quần. |

Tổng lời đọc: 174.2s
