"""Kịch bản bài 29 — mỗi đoạn: (phụ đề hiển thị, câu đọc phiên âm).

Hai kiểu lời đọc, chọn bằng TEXT=phienam|tienganh khi chạy make_voice.py:
- SEGMENTS[key][1]: phiên âm thuật ngữ ("hát sét", "tru") — an toàn với mọi giọng.
- TTS_EN[key]: giữ nguyên từ tiếng Anh ("HashSet", "true") — hợp với giọng neural.
  Đoạn không có trong TTS_EN thì dùng lại bản phiên âm (không chứa từ tiếng Anh).
"""

SEGMENTS = {
    "intro": (
        "Bài 29 · Contains Duplicate (LeetCode #217)",
        "Bài hai mươi chín. Con tên đu pli kết, lít cốt số hai trăm mười bảy.",
    ),
    "problem": (
        "Đề bài: mảng có phần tử nào xuất hiện từ 2 lần trở lên không?",
        "Đề bài: cho một mảng số nguyên, kiểm tra xem có phần tử nào xuất hiện từ hai lần trở lên hay không.",
    ),
    "example": (
        "nums = [1, 2, 3, 1] → true, vì số 1 xuất hiện 2 lần",
        "Ví dụ, mảng một, hai, ba, một. Kết quả là tru, vì số một xuất hiện hai lần.",
    ),
    "brute": (
        "Cách ngây thơ: so từng cặp → n(n−1)/2 phép so sánh = O(n²)",
        "Cách ngây thơ là so sánh từng cặp phần tử. Với n phần tử, ta cần n nhân n trừ một chia hai phép so sánh, tức là ô en bình phương.",
    ),
    "brute_bad": (
        "n = 100.000 → gần 5 tỷ phép so sánh",
        "Với mảng một trăm nghìn phần tử, đó là gần năm tỷ phép so sánh. Quá chậm.",
    ),
    "idea": (
        "Ý tưởng: HashSet seen lưu các số đã gặp — kiểm tra tồn tại chỉ O(1)",
        "Ý tưởng: dùng một hát sét tên là sín, để lưu các số đã gặp. Kiểm tra một số có trong sét hay không chỉ tốn ô một.",
    ),
    "step0": (
        "num = 1: chưa có trong seen → thêm vào",
        "Số một, chưa có trong sét. Thêm vào.",
    ),
    "step1": (
        "num = 2: chưa có → thêm vào",
        "Số hai, chưa có. Thêm vào.",
    ),
    "step2": (
        "num = 3: chưa có → thêm vào",
        "Số ba, cũng chưa có. Thêm vào.",
    ),
    "step3": (
        "num = 1: ĐÃ có trong seen → return True ngay",
        "Số một. Lần này, số một đã có trong sét. Trả về tru ngay lập tức, không cần duyệt tiếp.",
    ),
    "ex2": (
        "nums = [1, 2, 3, 4]: duyệt hết không gặp số trùng → return False",
        "Nếu mảng là một, hai, ba, bốn, ta duyệt hết mà không gặp số nào trùng, nên trả về phon.",
    ),
    "py_intro": (
        "Code Python — leetcode-38-bai/lc217-contains-duplicate.py",
        "Giờ xem code Pai thon.",
    ),
    "py_seen": (
        "seen = set(): tập rỗng lưu các số đã gặp",
        "Dòng này tạo một sét rỗng, để lưu các số đã gặp.",
    ),
    "py_loop": (
        "Duyệt từng num — đã có trong seen → return True",
        "Duyệt từng số. Nếu số đó đã có trong sét, trả về tru.",
    ),
    "py_add": (
        "Chưa có → seen.add(num)",
        "Nếu chưa có, thêm nó vào sét.",
    ),
    "py_false": (
        "Duyệt hết không trùng → return False",
        "Duyệt hết mà không trùng, trả về phon.",
    ),
    "java_intro": (
        "Code Java 21 — groupc/ContainsDuplicate.java",
        "Còn đây là bản gia va.",
    ),
    "java_seen": (
        "set()  ↔  new HashSet<>()",
        "Sét rỗng bên Pai thon, tương ứng với niu hát sét bên gia va.",
    ),
    "java_add": (
        "Set.add() trả về false nếu đã tồn tại → gộp 'kiểm tra + thêm' làm 1 dòng",
        "Điểm khác: hàm át của gia va trả về phon nếu phần tử đã có. Nên ta gộp bước kiểm tra và bước thêm vào, làm một dòng.",
    ),
    "complexity": (
        "Time O(n) · Space O(n) — cả Python và Java",
        "Độ phức tạp: thời gian ô en, bộ nhớ ô en, ở cả hai ngôn ngữ.",
    ),
    "tradeoff": (
        "Đánh đổi: +O(n) bộ nhớ để O(n²) → O(n). Thiếu bộ nhớ? Sort rồi so kề nhau: O(n log n)",
        "Đánh đổi là: tốn thêm bộ nhớ ô en, để giảm thời gian từ ô en bình phương xuống ô en. Nếu bị giới hạn bộ nhớ, có thể sắp xếp mảng rồi so hai phần tử kề nhau, mất ô en lốc en.",
    ),
    "outro": (
        "Tiếp theo: Bài 30 · Valid Anagram (#242)",
        "Bài tiếp theo: va lít a na gram.",
    ),
}

TTS_EN = {
    "intro": "Bài hai mươi chín. Contains Duplicate, LeetCode số 217.",
    "example": "Ví dụ, mảng một, hai, ba, một. Kết quả là true, vì số một xuất hiện hai lần.",
    "brute": "Cách ngây thơ là so sánh từng cặp phần tử. Với n phần tử, ta cần n nhân n trừ một chia hai phép so sánh, tức là O n bình phương.",
    "idea": "Ý tưởng: dùng một HashSet tên là seen, để lưu các số đã gặp. Kiểm tra một số có trong set hay không chỉ tốn O một.",
    "step0": "Số một, chưa có trong set. Thêm vào.",
    "step3": "Số một. Lần này, số một đã có trong set. Trả về true ngay lập tức, không cần duyệt tiếp.",
    "ex2": "Nếu mảng là một, hai, ba, bốn, ta duyệt hết mà không gặp số nào trùng, nên trả về false.",
    "py_intro": "Giờ xem code Python.",
    "py_seen": "Dòng này tạo một set rỗng, để lưu các số đã gặp.",
    "py_loop": "Duyệt từng số. Nếu số đó đã có trong set, trả về true.",
    "py_add": "Nếu chưa có, thêm nó vào set.",
    "py_false": "Duyệt hết mà không trùng, trả về false.",
    "java_intro": "Còn đây là bản Java.",
    "java_seen": "Set rỗng bên Python, tương ứng với new HashSet bên Java.",
    "java_add": "Điểm khác: hàm add của Java trả về false nếu phần tử đã có. Nên ta gộp bước kiểm tra và bước thêm vào, làm một dòng.",
    "complexity": "Độ phức tạp: thời gian O n, bộ nhớ O n, ở cả hai ngôn ngữ.",
    "tradeoff": "Đánh đổi là: tốn thêm bộ nhớ O n, để giảm thời gian từ O n bình phương xuống O n. Nếu bị giới hạn bộ nhớ, có thể sắp xếp mảng rồi so hai phần tử kề nhau, mất O n log n.",
    "outro": "Bài tiếp theo: Valid Anagram.",
}
