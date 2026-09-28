"""Kịch bản bài 30 — mỗi đoạn: (phụ đề hiển thị, câu đọc phiên âm).

Hai kiểu lời đọc, chọn bằng TEXT=phienam|tienganh khi build:
- SEGMENTS[key][1]: phiên âm thuật ngữ ("cao tơ", "tru") — an toàn với mọi giọng.
- TTS_EN[key]: giữ nguyên từ tiếng Anh ("Counter", "true") — hợp với giọng neural / người đọc.
  Đoạn không có trong TTS_EN thì dùng lại bản phiên âm.

Lưu ý: Python và Java của bài này giải KHÁC cách — Python so 2 Counter, Java dùng 1 mảng int[26]
cộng/trừ. Video minh hoạ đúng cả hai.
"""

LESSON = "Bài 30 — Valid Anagram"

SEGMENTS = {
    "intro": (
        "Bài 30 · Valid Anagram (LeetCode #242)",
        "Bài ba mươi. Va lít a na gram, lít cốt số hai trăm bốn mươi hai.",
    ),
    "problem": (
        "Đề bài: t có phải anagram của s — cùng ký tự, cùng số lần xuất hiện, chỉ khác thứ tự?",
        "Đề bài: cho hai chuỗi s và t. Kiểm tra t có phải là a na gram của s hay không, tức là cùng các ký tự, cùng số lần xuất hiện, chỉ khác thứ tự.",
    ),
    "example": (
        's = "anagram", t = "nagaram" → true (chỉ đổi chỗ các chữ cái)',
        "Ví dụ: a na gram và na ga ram. Hai chuỗi chỉ đổi chỗ các chữ cái, nên kết quả là tru.",
    ),
    "naive": (
        "Cách 1: sort cả hai chuỗi rồi so → đúng, nhưng tốn O(n log n)",
        "Cách thứ nhất: sắp xếp cả hai chuỗi rồi so sánh. Giống nhau thì là a na gram. Nhưng sắp xếp tốn ô en lốc en.",
    ),
    "idea": (
        "Cách 2: đếm tần suất từng ký tự bằng dictionary (Python: Counter) → O(n)",
        "Cách tốt hơn: đếm mỗi ký tự xuất hiện bao nhiêu lần, bằng một bảng đếm, trong Pai thon là cao tơ. Chỉ cần duyệt mỗi chuỗi một lần, ô en.",
    ),
    "len": (
        "Bước 1: len(s) = len(t) = 7 → đi tiếp (khác độ dài thì False ngay)",
        "Bước một: so độ dài. Cả hai đều bảy ký tự, nên đi tiếp. Nếu độ dài khác nhau, trả về phon ngay.",
    ),
    "count_s": (
        "Counter(s): a → 3, g → 1, m → 1, n → 1, r → 1",
        "Đếm chuỗi s: chữ a ba lần. Các chữ g, m, n, r mỗi chữ một lần.",
    ),
    "count_t": (
        "Counter(t): đếm \"nagaram\" ra đúng bảng như vậy",
        "Đếm chuỗi t, cũng ra đúng bảng như vậy.",
    ),
    "compare": (
        "Counter(s) == Counter(t): so từng ký tự → khớp hết → True",
        "So hai bảng đếm theo từng ký tự. Khớp hết, nên trả về tru.",
    ),
    "ex2": (
        's = "rat", t = "car": chữ t chỉ có ở s, chữ c chỉ có ở t → False',
        "Với rát và ca: chữ tê chỉ có ở s, chữ xê chỉ có ở t. Hai bảng đếm khác nhau, nên trả về phon.",
    ),
    "py_intro": (
        "Code Python — leetcode-38-bai/lc242-valid-anagram.py",
        "Giờ xem code Pai thon.",
    ),
    "py_len": (
        "len(s) != len(t) → False: chặn sớm, khỏi phải đếm",
        "Khác độ dài thì trả về phon luôn, khỏi phải đếm.",
    ),
    "py_counter": (
        "Counter(s) == Counter(t): tạo 2 dict đếm, Python so theo từng key",
        "Dòng cuối tạo hai bảng đếm rồi so sánh. Pai thon so hai bảng theo từng khóa.",
    ),
    "java_intro": (
        "Java 21 — groupc/ValidAnagram.java: 1 mảng int[26] thay cho 2 Counter",
        "Bản gia va làm khác: chỉ dùng một mảng hai mươi sáu số nguyên, thay cho hai bảng đếm.",
    ),
    "java_loop": (
        "Cùng một vòng lặp: ký tự của s → +1, ký tự của t → −1",
        "Trong cùng một vòng lặp, gặp ký tự của s thì cộng một, gặp ký tự của t thì trừ một.",
    ),
    "java_zero": (
        "Anagram ⇔ cộng/trừ triệt tiêu, mọi ô về 0 → return true",
        "Nếu là a na gram, cộng và trừ triệt tiêu nhau, mọi ô đều về không. Có ô nào khác không thì trả về phon.",
    ),
    "java_unicode": (
        "Đánh đổi: int[26] chỉ đúng với chữ a–z. Có Unicode (vd. tiếng Việt có dấu) → dùng HashMap như Counter",
        "Đánh đổi là: mảng hai mươi sáu ô chỉ đúng khi chuỗi chỉ có chữ thường a đến z. Nếu có ký tự u ni cốt, như tiếng Việt có dấu, phải dùng hát mép, giống cao tơ.",
    ),
    "complexity": (
        "Time O(n) · Space O(1) — bảng chữ cái cố định 26 ký tự",
        "Độ phức tạp: thời gian ô en. Bộ nhớ ô một, vì bảng chữ cái cố định hai mươi sáu ký tự.",
    ),
    "tradeoff": (
        "Sort: O(n log n) · Counter/HashMap: O(n), mọi ký tự · int[26]: O(n), nhanh nhất nhưng chỉ a–z",
        "Tóm lại: sắp xếp thì ô en lốc en. Bảng đếm thì ô en, dùng được cho mọi ký tự. Mảng hai mươi sáu ô cũng ô en, nhanh nhất, nhưng chỉ cho chữ a đến z.",
    ),
    "outro": (
        "Tiếp theo: Bài 31 · Isomorphic Strings (#205)",
        "Bài tiếp theo: ai xô mo phích, sờ trinh.",
    ),
}

TTS_EN = {
    "intro": "Bài ba mươi. Valid Anagram, LeetCode số 242.",
    "problem": "Đề bài: cho hai chuỗi s và t. Kiểm tra t có phải là anagram của s hay không, tức là cùng các ký tự, cùng số lần xuất hiện, chỉ khác thứ tự.",
    "example": "Ví dụ: anagram và nagaram. Hai chuỗi chỉ đổi chỗ các chữ cái, nên kết quả là true.",
    "naive": "Cách thứ nhất: sort cả hai chuỗi rồi so sánh. Giống nhau thì là anagram. Nhưng sort tốn O n log n.",
    "idea": "Cách tốt hơn: đếm mỗi ký tự xuất hiện bao nhiêu lần bằng một dictionary, trong Python là Counter. Chỉ cần duyệt mỗi chuỗi một lần, O n.",
    "len": "Bước một: so độ dài. Cả hai đều bảy ký tự, nên đi tiếp. Nếu độ dài khác nhau, trả về false ngay.",
    "compare": "So hai Counter theo từng ký tự. Khớp hết, nên trả về true.",
    "ex2": "Với rat và car: chữ t chỉ có ở s, chữ c chỉ có ở t. Hai Counter khác nhau, nên trả về false.",
    "py_intro": "Giờ xem code Python.",
    "py_len": "Khác độ dài thì trả về false luôn, khỏi phải đếm.",
    "py_counter": "Dòng cuối tạo hai Counter rồi so sánh. Python so hai dict theo từng key.",
    "java_intro": "Bản Java làm khác: chỉ dùng một mảng int hai mươi sáu phần tử, thay cho hai Counter.",
    "java_zero": "Nếu là anagram, cộng và trừ triệt tiêu nhau, mọi ô đều về không. Có ô nào khác không thì trả về false.",
    "java_unicode": "Đánh đổi là: mảng int hai mươi sáu ô chỉ đúng khi chuỗi chỉ có chữ thường a đến z. Nếu có ký tự Unicode, như tiếng Việt có dấu, phải dùng HashMap, giống Counter.",
    "complexity": "Độ phức tạp: thời gian O n. Bộ nhớ O một, vì bảng chữ cái cố định hai mươi sáu ký tự.",
    "tradeoff": "Tóm lại: sort thì O n log n. Counter hoặc HashMap thì O n, dùng được cho mọi ký tự. Mảng int hai mươi sáu ô cũng O n, nhanh nhất, nhưng chỉ cho chữ a đến z.",
    "outro": "Bài tiếp theo: Isomorphic Strings.",
}
