"""Kịch bản bài 31 — mỗi đoạn: (phụ đề hiển thị, câu đọc phiên âm).

Hai kiểu lời đọc, chọn bằng TEXT=phienam|tienganh khi build:
- SEGMENTS[key][1]: phiên âm thuật ngữ ("máp", "tru"), đọc tên chữ cái kiểu Việt ("bê", "đê").
- TTS_EN[key]: giữ nguyên từ tiếng Anh ("map", "true").
"""

LESSON = "Bài 31 — Isomorphic Strings"

SEGMENTS = {
    "intro": (
        "Bài 31 · Isomorphic Strings (LeetCode #205)",
        "Bài ba mươi mốt. Ai xô mo phích, sờ trinh, lít cốt số hai trăm linh năm.",
    ),
    "problem": (
        "Đề bài: có phép thay ký tự 1–1 biến s thành t không? Mỗi ký tự s ↔ đúng 1 ký tự t",
        "Đề bài: có cách thay từng ký tự của s để được t hay không. Mỗi ký tự của s phải ghép với đúng một ký tự của t, và ngược lại.",
    ),
    "example": (
        's = "egg", t = "add": e → a, g → d → true',
        "Ví dụ: e giê giê và a đê đê. e thành a, giê thành đê, nhất quán, nên là tru.",
    ),
    "ex_false": (
        's = "foo", t = "bar": o → a rồi o → r — một ký tự ghép 2 nơi → false',
        "Còn ép o o và bê a e rờ: chữ o lúc thì thành a, lúc thành e rờ. Mâu thuẫn, nên là phon.",
    ),
    "idea": (
        "Ý tưởng: ghi lại các cặp đã ghép bằng dictionary, gặp lại thì kiểm tra có khớp không",
        "Ý tưởng: dùng từ điển ghi lại các cặp đã ghép. Gặp lại một ký tự, kiểm tra nó có ghép đúng như cũ không.",
    ),
    "p0": (
        "p → t: cả 2 map đều chưa có → ghi p→t và t→p",
        "Chữ pê ghép với tê. Cả hai máp đều chưa có, nên ghi vào cả hai.",
    ),
    "p1": (
        "a → i: mới → ghi vào",
        "a ghép với i. Mới, ghi vào.",
    ),
    "p2": (
        "p → t: map_st có p → t, map_ts có t → p → khớp ✓",
        "Lại là pê với tê. Cả hai máp đều đã có, và đều khớp. Đi tiếp.",
    ),
    "p3": (
        "e → l: mới → ghi vào",
        "e ghép với e lờ. Ghi vào.",
    ),
    "p4": (
        "r → e: mới → ghi vào · hết chuỗi, không mâu thuẫn → return True",
        "e rờ ghép với e. Ghi vào. Hết chuỗi mà không có mâu thuẫn, trả về tru.",
    ),
    "trap": (
        'Vì sao cần 2 map? s = "badc", t = "baba": chỉ map s → t thì không thấy mâu thuẫn nào',
        "Vì sao cần hai máp? Thử bê a đê xê và bê a bê a. Nếu chỉ có máp từ s sang t, ta không thấy mâu thuẫn nào.",
    ),
    "trap_why": (
        "Nhưng b và d cùng ghép vào b — hai ký tự s chung một ký tự t → không phải 1–1",
        "Nhưng cả bê và đê đều ghép vào bê. Hai ký tự của s dùng chung một ký tự của t, nên không phải một đổi một.",
    ),
    "two_maps": (
        "Nên cần thêm map_ts (t → s) để kiểm tra chiều ngược lại — song ánh",
        "Nên phải có thêm máp từ t ngược về s, để kiểm tra cả chiều ngược lại. Đó là song ánh.",
    ),
    "b0": (
        "b → b: chưa có → ghi vào cả 2 map",
        "bê ghép với bê. Chưa có, ghi vào cả hai máp.",
    ),
    "b1": (
        "a → a: chưa có → ghi vào",
        "a ghép với a. Ghi vào.",
    ),
    "b2": (
        "d → b: map_st chưa có d, nhưng map_ts nói b đã thuộc về b ≠ d → return False",
        "đê ghép với bê. Máp s sang t chưa có đê. Nhưng máp t sang s nói bê đã thuộc về bê, không phải đê. Mâu thuẫn, trả về phon.",
    ),
    "py_intro": (
        "Code Python — leetcode-38-bai/lc205-isomorphic-strings.py",
        "Giờ xem code Pai thon.",
    ),
    "py_maps": (
        "2 dict: map_st (s → t) và map_ts (t → s)",
        "Hai từ điển: một cho chiều s sang t, một cho chiều t sang s.",
    ),
    "py_checks": (
        "2 điều kiện False: cs đã ghép với ký tự khác, hoặc ct đã bị ký tự khác chiếm",
        "Hai điều kiện trả về phon: ký tự của s đã ghép với ký tự khác, hoặc ký tự của t đã bị ký tự khác chiếm.",
    ),
    "py_put": (
        "Hợp lệ → ghi cả 2 chiều · duyệt hết → True",
        "Hợp lệ thì ghi cả hai chiều. Duyệt hết mà không mâu thuẫn, trả về tru.",
    ),
    "java_intro": (
        "Java 21 — groupc/IsomorphicStrings.java: cùng thuật toán, 2 HashMap<Character, Character>",
        "Bản gia va dùng cùng thuật toán, với hai hát mép.",
    ),
    "java_get": (
        "get() trả null nếu chưa có — thay cho phép kiểm tra 'in' của Python",
        "Hàm ghét trả về nâu nếu chưa có khóa. Nó thay cho phép kiểm tra in của Pai thon.",
    ),
    "java_box": (
        "Bẫy: Character != char → so giá trị (unboxing) ✓. Hai Character với nhau thì != so địa chỉ → phải dùng equals()",
        "Cẩn thận: ở đây ta so một đối tượng ca rác tơ với một ký tự thường, nên gia va so theo giá trị, đúng. Nhưng nếu cả hai đều là ca rác tơ, dấu khác sẽ so địa chỉ, phải dùng hàm i quồ.",
    ),
    "complexity": (
        "Time O(n) · Space O(1) — tối đa số ký tự của bảng mã (ASCII: 256)",
        "Độ phức tạp: thời gian ô en. Bộ nhớ ô một, vì mỗi máp tối đa bằng số ký tự của bảng mã.",
    ),
    "tradeoff": (
        "2 HashMap: mọi ký tự · 1 HashMap + HashSet đã dùng: tương đương · 2 mảng int[256]: nhanh nhất, chỉ ASCII",
        "Các cách khác: một hát mép cộng một hát sét các ký tự đã dùng, tương đương. Hoặc hai mảng hai trăm năm mươi sáu ô, nhanh nhất, nhưng chỉ cho a xơ ki.",
    ),
    "outro": (
        "Tiếp theo: Bài 32 · Longest Consecutive Sequence (#128)",
        "Bài tiếp theo: long ghét con sếch cu típ si quần.",
    ),
}

TTS_EN = {
    "intro": "Bài ba mươi mốt. Isomorphic Strings, LeetCode số 205.",
    "example": "Ví dụ: egg và add. e thành a, g thành d, nhất quán, nên là true.",
    "ex_false": "Còn foo và bar: chữ o lúc thì thành a, lúc thành r. Mâu thuẫn, nên là false.",
    "idea": "Ý tưởng: dùng dictionary ghi lại các cặp đã ghép. Gặp lại một ký tự, kiểm tra nó có ghép đúng như cũ không.",
    "p0": "Chữ p ghép với t. Cả hai map đều chưa có, nên ghi vào cả hai.",
    "p2": "Lại là p với t. Cả hai map đều đã có, và đều khớp. Đi tiếp.",
    "p3": "e ghép với l. Ghi vào.",
    "p4": "r ghép với e. Ghi vào. Hết chuỗi mà không có mâu thuẫn, trả về true.",
    "trap": "Vì sao cần hai map? Thử badc và baba. Nếu chỉ có map từ s sang t, ta không thấy mâu thuẫn nào.",
    "trap_why": "Nhưng cả b và d đều ghép vào b. Hai ký tự của s dùng chung một ký tự của t, nên không phải một đổi một.",
    "two_maps": "Nên phải có thêm map từ t ngược về s, để kiểm tra cả chiều ngược lại. Đó là song ánh, bijection.",
    "b0": "b ghép với b. Chưa có, ghi vào cả hai map.",
    "b2": "d ghép với b. Map s sang t chưa có d. Nhưng map t sang s nói b đã thuộc về b, không phải d. Mâu thuẫn, trả về false.",
    "py_intro": "Giờ xem code Python.",
    "py_maps": "Hai dict: map s t cho chiều s sang t, map t s cho chiều ngược lại.",
    "py_checks": "Hai điều kiện trả về false: ký tự của s đã ghép với ký tự khác, hoặc ký tự của t đã bị ký tự khác chiếm.",
    "py_put": "Hợp lệ thì ghi cả hai chiều. Duyệt hết mà không mâu thuẫn, trả về true.",
    "java_intro": "Bản Java dùng cùng thuật toán, với hai HashMap Character, Character.",
    "java_get": "Hàm get trả về null nếu chưa có key. Nó thay cho phép kiểm tra in của Python.",
    "java_box": "Cẩn thận: ở đây ta so một Character với một char, nên Java unbox và so theo giá trị, đúng. Nhưng nếu cả hai đều là Character, dấu khác sẽ so reference, phải dùng equals.",
    "complexity": "Độ phức tạp: thời gian O n. Bộ nhớ O một, vì mỗi map tối đa bằng số ký tự của bảng mã.",
    "tradeoff": "Các cách khác: một HashMap cộng một HashSet các ký tự đã dùng, tương đương. Hoặc hai mảng int hai trăm năm mươi sáu ô, nhanh nhất, nhưng chỉ cho ASCII.",
    "outro": "Bài tiếp theo: Longest Consecutive Sequence.",
}
