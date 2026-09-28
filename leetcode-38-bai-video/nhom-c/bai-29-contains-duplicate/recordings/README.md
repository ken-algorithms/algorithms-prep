# Bài 29 — danh sách câu cần ghi âm

Mỗi đoạn một file, **tên file = cột Đoạn** (ví dụ `intro.m4a`, `step0.mp3`), bỏ vào thư mục này.
Định dạng nào cũng được: m4a (Voice Memos iPhone/Mac), mp3, wav, aiff, flac, ogg, webm.
Không cần cắt khoảng lặng đầu/cuối — script tự cắt và cân âm lượng.
Thiếu đoạn nào thì đoạn đó tạm dùng giọng Linh. Build: `VOICE=file ./build.sh`.

Đọc tự nhiên, thuật ngữ tiếng Anh đọc như bình thường (HashSet, true, false). Nên đọc ở phòng
yên tĩnh, micro cách miệng ~15–20 cm, giữ cùng một chỗ ngồi cho mọi đoạn để giọng đồng đều.

| # | Đoạn (tên file) | Câu đọc | Phụ đề trên màn hình |
|---|---|---|---|
| 1 | `intro` | Bài hai mươi chín. Contains Duplicate, LeetCode số 217. | Bài 29 · Contains Duplicate (LeetCode #217) |
| 2 | `problem` | Đề bài: cho một mảng số nguyên, kiểm tra xem có phần tử nào xuất hiện từ hai lần trở lên hay không. | Đề bài: mảng có phần tử nào xuất hiện từ 2 lần trở lên không? |
| 3 | `example` | Ví dụ, mảng một, hai, ba, một. Kết quả là true, vì số một xuất hiện hai lần. | nums = [1, 2, 3, 1] → true, vì số 1 xuất hiện 2 lần |
| 4 | `brute` | Cách ngây thơ là so sánh từng cặp phần tử. Với n phần tử, ta cần n nhân n trừ một chia hai phép so sánh, tức là O n bình phương. | Cách ngây thơ: so từng cặp → n(n−1)/2 phép so sánh = O(n²) |
| 5 | `brute_bad` | Với mảng một trăm nghìn phần tử, đó là gần năm tỷ phép so sánh. Quá chậm. | n = 100.000 → gần 5 tỷ phép so sánh |
| 6 | `idea` | Ý tưởng: dùng một HashSet tên là seen, để lưu các số đã gặp. Kiểm tra một số có trong set hay không chỉ tốn O một. | Ý tưởng: HashSet seen lưu các số đã gặp — kiểm tra tồn tại chỉ O(1) |
| 7 | `step0` | Số một, chưa có trong set. Thêm vào. | num = 1: chưa có trong seen → thêm vào |
| 8 | `step1` | Số hai, chưa có. Thêm vào. | num = 2: chưa có → thêm vào |
| 9 | `step2` | Số ba, cũng chưa có. Thêm vào. | num = 3: chưa có → thêm vào |
| 10 | `step3` | Số một. Lần này, số một đã có trong set. Trả về true ngay lập tức, không cần duyệt tiếp. | num = 1: ĐÃ có trong seen → return True ngay |
| 11 | `ex2` | Nếu mảng là một, hai, ba, bốn, ta duyệt hết mà không gặp số nào trùng, nên trả về false. | nums = [1, 2, 3, 4]: duyệt hết không gặp số trùng → return False |
| 12 | `py_intro` | Giờ xem code Python. | Code Python — leetcode-38-bai/lc217-contains-duplicate.py |
| 13 | `py_seen` | Dòng này tạo một set rỗng, để lưu các số đã gặp. | seen = set(): tập rỗng lưu các số đã gặp |
| 14 | `py_loop` | Duyệt từng số. Nếu số đó đã có trong set, trả về true. | Duyệt từng num — đã có trong seen → return True |
| 15 | `py_add` | Nếu chưa có, thêm nó vào set. | Chưa có → seen.add(num) |
| 16 | `py_false` | Duyệt hết mà không trùng, trả về false. | Duyệt hết không trùng → return False |
| 17 | `java_intro` | Còn đây là bản Java. | Code Java 21 — groupc/ContainsDuplicate.java |
| 18 | `java_seen` | Set rỗng bên Python, tương ứng với new HashSet bên Java. | set()  ↔  new HashSet<>() |
| 19 | `java_add` | Điểm khác: hàm add của Java trả về false nếu phần tử đã có. Nên ta gộp bước kiểm tra và bước thêm vào, làm một dòng. | Set.add() trả về false nếu đã tồn tại → gộp 'kiểm tra + thêm' làm 1 dòng |
| 20 | `complexity` | Độ phức tạp: thời gian O n, bộ nhớ O n, ở cả hai ngôn ngữ. | Time O(n) · Space O(n) — cả Python và Java |
| 21 | `tradeoff` | Đánh đổi là: tốn thêm bộ nhớ O n, để giảm thời gian từ O n bình phương xuống O n. Nếu bị giới hạn bộ nhớ, có thể sắp xếp mảng rồi so hai phần tử kề nhau, mất O n log n. | Đánh đổi: +O(n) bộ nhớ để O(n²) → O(n). Thiếu bộ nhớ? Sort rồi so kề nhau: O(n log n) |
| 22 | `outro` | Bài tiếp theo: Valid Anagram. | Tiếp theo: Bài 30 · Valid Anagram (#242) |
