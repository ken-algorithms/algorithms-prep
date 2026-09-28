"""Kịch bản bài 35 — mỗi đoạn: (phụ đề hiển thị, câu đọc phiên âm).

Đã kiểm chứng bằng code thật: 19 → 82 → 68 → 100 → 1 · 2 → 4 → 16 → 37 → 58 → 89 → 145 → 42 → 20 → 4
· max tổng bình phương chữ số với x ≤ 999 là 243 · Floyd khớp HashSet với n = 1…100.000
· mọi số không vui ≤ 100.000 đều đi qua 4.

TEXT=phienam|tienganh khi build: SEGMENTS[key][1] phiên âm, TTS_EN[key] giữ từ tiếng Anh.
"""

LESSON = "Bài 35 — Happy Number"

SEGMENTS = {
    "intro": (
        "Bài 35 · Happy Number (LeetCode #202)",
        "Bài ba mươi lăm. Háp pi năm bơ, lít cốt số hai trăm linh hai.",
    ),
    "problem": (
        "Đề bài: thay n bằng tổng bình phương các chữ số, lặp lại. Về được 1 → happy number",
        "Đề bài: thay số n bằng tổng bình phương các chữ số của nó, rồi lặp lại. Nếu cuối cùng về được một, đó là số vui.",
    ),
    "example": (
        "19 → 1² + 9² = 82 → 68 → 100 → 1 → true",
        "Ví dụ mười chín: một bình phương cộng chín bình phương là tám mươi hai. Rồi sáu mươi tám, một trăm, và về một. Là số vui.",
    ),
    "unhappy": (
        "2 → 4 → 16 → 37 → 58 → 89 → 145 → 42 → 20 → 4 … quay lại 4: vòng lặp vô tận → false",
        "Còn số hai: bốn, mười sáu, ba mươi bảy, năm mươi tám, tám mươi chín, một trăm bốn lăm, bốn mươi hai, hai mươi, rồi lại bốn. Lặp mãi, không bao giờ về một.",
    ),
    "bound": (
        "Sao chắc không tăng mãi? Số ≥ 4 chữ số luôn giảm; số ≤ 3 chữ số cho tổng ≤ 243 → dãy chỉ có hữu hạn giá trị",
        "Vì sao dãy không tăng mãi? Số có từ bốn chữ số trở lên thì tổng luôn nhỏ hơn chính nó. Số có ba chữ số thì tổng không quá hai trăm bốn mươi ba. Nên dãy chỉ đi qua hữu hạn giá trị: hoặc về một, hoặc lặp lại.",
    ),
    "idea": (
        "Ý tưởng: lưu mọi số đã gặp vào HashSet seen — gặp lại một số cũ = đang lặp → false",
        "Ý tưởng: lưu mọi số đã gặp vào một hát sét. Gặp lại một số cũ nghĩa là đang lặp, trả về phon.",
    ),
    "h0": (
        "n = 2: chưa gặp → thêm vào seen · 2² = 4",
        "Bắt đầu với hai: chưa gặp, thêm vào sét. Hai bình phương là bốn.",
    ),
    "h_mid": (
        "4 → 16 → 37 → 58 → 89 → 145 → 42 → 20: mỗi số mới đều được thêm vào seen",
        "Bốn, mười sáu, ba mươi bảy, năm mươi tám, tám mươi chín, một trăm bốn lăm, bốn mươi hai, hai mươi. Mỗi số mới đều được thêm vào sét.",
    ),
    "h_end": (
        "20 → 2² + 0² = 4 · 4 ĐÃ có trong seen → chu trình → return False",
        "Hai mươi cho ra bốn. Nhưng bốn đã có trong sét. Đó là chu trình, trả về phon.",
    ),
    "floyd": (
        "Follow-up: không dùng set? Rùa–thỏ (Floyd): rùa đi 1 bước, thỏ 2 bước — có vòng thì sẽ gặp nhau → bộ nhớ O(1)",
        "Câu hỏi mở rộng: không dùng sét được không? Dùng rùa và thỏ. Rùa đi một bước, thỏ đi hai bước. Nếu có vòng lặp, thỏ sẽ đuổi kịp rùa. Bộ nhớ chỉ ô một.",
    ),
    "trick4": (
        "Mẹo: mọi số không vui đều rơi vào vòng chứa 4 (đã kiểm tra với n ≤ 100.000)",
        "Một mẹo thú vị: mọi số không vui đều rơi vào vòng lặp có số bốn. Tôi đã kiểm tra với mọi n tới một trăm nghìn.",
    ),
    "py_intro": (
        "Code Python — leetcode-38-bai/lc202-happy-number.py",
        "Giờ xem code Pai thon.",
    ),
    "py_while": (
        "while n != 1 and n not in seen: dừng khi về 1 HOẶC gặp lại số cũ",
        "Vòng lặp dừng khi về một, hoặc khi gặp lại một số cũ.",
    ),
    "py_digits": (
        "sum(int(d) ** 2 for d in str(n)): tách chữ số qua chuỗi — ngắn, dễ đọc",
        "Tách chữ số bằng cách đổi sang chuỗi. Ngắn và dễ đọc.",
    ),
    "java_intro": (
        "Java 21 — groupc/HappyNumber.java: cùng thuật toán, tách hàm sumOfSquaredDigits",
        "Bản gia va dùng cùng thuật toán, và tách phần tính tổng ra một hàm riêng.",
    ),
    "java_add": (
        "seen.add(current) trả về false nếu đã có → gộp kiểm tra + thêm vào điều kiện while (như bài 29)",
        "Hàm át trả về phon nếu số đã có. Nên kiểm tra và thêm được gộp ngay trong điều kiện vòng lặp, giống bài hai mươi chín.",
    ),
    "java_digits": (
        "number % 10 lấy chữ số cuối, number /= 10 bỏ chữ số cuối — không tạo chuỗi",
        "Chia lấy dư cho mười để lấy chữ số cuối, chia nguyên cho mười để bỏ nó đi. Không cần tạo chuỗi.",
    ),
    "complexity": (
        "Time O(log n): mỗi bước tốn O(số chữ số), số bước bị chặn · Space O(log n) cho seen (Floyd: O(1))",
        "Độ phức tạp: thời gian ô lốc en, vì mỗi bước tốn theo số chữ số, và số bước bị chặn. Bộ nhớ ô lốc en cho sét, hoặc ô một nếu dùng rùa thỏ.",
    ),
    "outro": (
        "Tiếp theo: Bài 36 · 4Sum II (#454)",
        "Bài tiếp theo: pho xăm tu.",
    ),
}

TTS_EN = {
    "intro": "Bài ba mươi lăm. Happy Number, LeetCode số 202.",
    "problem": "Đề bài: thay số n bằng tổng bình phương các chữ số của nó, rồi lặp lại. Nếu cuối cùng về được một, đó là happy number.",
    "example": "Ví dụ mười chín: một bình phương cộng chín bình phương là tám mươi hai. Rồi sáu mươi tám, một trăm, và về một. Là happy number, true.",
    "idea": "Ý tưởng: lưu mọi số đã gặp vào một HashSet tên là seen. Gặp lại một số cũ nghĩa là đang lặp, trả về false.",
    "h0": "Bắt đầu với hai: chưa gặp, thêm vào seen. Hai bình phương là bốn.",
    "h_mid": "Bốn, mười sáu, ba mươi bảy, năm mươi tám, tám mươi chín, một trăm bốn lăm, bốn mươi hai, hai mươi. Mỗi số mới đều được thêm vào seen.",
    "h_end": "Hai mươi cho ra bốn. Nhưng bốn đã có trong seen. Đó là cycle, trả về false.",
    "py_intro": "Giờ xem code Python.",
    "py_while": "Vòng while dừng khi n bằng một, hoặc khi n đã có trong seen.",
    "py_digits": "Tách chữ số bằng cách đổi sang string. Ngắn và dễ đọc.",
    "java_intro": "Bản Java dùng cùng thuật toán, và tách phần tính tổng ra hàm sum of squared digits.",
    "java_add": "Hàm add trả về false nếu số đã có. Nên kiểm tra và thêm được gộp ngay trong điều kiện while, giống bài 29.",
    "java_digits": "Modulo mười để lấy chữ số cuối, chia nguyên cho mười để bỏ nó đi. Không cần tạo string.",
    "floyd": "Follow up: không dùng set được không? Dùng Floyd, rùa và thỏ. Rùa đi một bước, thỏ đi hai bước. Nếu có cycle, thỏ sẽ đuổi kịp rùa. Bộ nhớ O một.",
    "complexity": "Độ phức tạp: thời gian O log n, vì mỗi bước tốn theo số chữ số, và số bước bị chặn. Bộ nhớ O log n cho seen, hoặc O một nếu dùng Floyd.",
    "outro": "Bài tiếp theo: 4Sum Two.",
}
