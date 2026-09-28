"""Kịch bản bài 32 — mỗi đoạn: (phụ đề hiển thị, câu đọc phiên âm).

Thứ tự duyệt set [1, 2, 3, 100, 4, 200] đã kiểm tra thật trên CPython 3.12 và Java HashSet — các đoạn
w1…w200 đọc theo đúng thứ tự đó. Scene lấy thứ tự bằng list(set(nums)) nên luôn khớp code chạy thật.

TEXT=phienam|tienganh khi build: SEGMENTS[key][1] phiên âm, TTS_EN[key] giữ từ tiếng Anh.
"""

LESSON = "Bài 32 — Longest Consecutive Sequence"

SEGMENTS = {
    "intro": (
        "Bài 32 · Longest Consecutive Sequence (LeetCode #128)",
        "Bài ba mươi hai. Long ghét con sếch cu típ si quần, lít cốt số một trăm hai mươi tám.",
    ),
    "problem": (
        "Đề bài: độ dài dãy số nguyên liên tiếp dài nhất — liên tiếp về giá trị, không cần kề nhau trong mảng. Yêu cầu O(n)",
        "Đề bài: tìm độ dài dãy số nguyên liên tiếp dài nhất. Liên tiếp về giá trị, không cần nằm cạnh nhau trong mảng. Và phải chạy trong ô en.",
    ),
    "example": (
        "[100, 4, 200, 1, 3, 2] → 4, vì có dãy 1, 2, 3, 4",
        "Ví dụ: một trăm, bốn, hai trăm, một, ba, hai. Kết quả là bốn, vì có dãy một, hai, ba, bốn.",
    ),
    "sort": (
        "Cách 1: sort rồi đếm đoạn liền nhau → đúng, nhưng O(n log n) — chưa đạt yêu cầu",
        "Cách thứ nhất: sắp xếp rồi đếm các đoạn liền nhau. Đúng, nhưng tốn ô en lốc en, chưa đạt yêu cầu.",
    ),
    "idea": (
        "Cách 2: bỏ hết vào HashSet — hỏi \"x có không?\" chỉ O(1)",
        "Cách thứ hai: bỏ hết vào một hát sét. Hỏi một số có trong sét hay không chỉ tốn ô một.",
    ),
    "key": (
        "Mấu chốt: chỉ bắt đầu đếm ở ĐẦU dãy — khi num − 1 không có trong set",
        "Mấu chốt: chỉ bắt đầu đếm ở đầu dãy, tức là khi số liền trước nó không có trong sét.",
    ),
    "w1": (
        "num = 1: 0 ∉ set → đầu dãy. 2, 3, 4 có; 5 không → length = 4, best = 4",
        "Số một: số không không có trong sét, nên một là đầu dãy. Đếm tiếp: hai, ba, bốn đều có, năm thì không. Độ dài bốn.",
    ),
    "w2": (
        "num = 2: 1 ∈ set → không phải đầu dãy → bỏ qua",
        "Số hai: số một có trong sét, nên hai không phải đầu dãy. Bỏ qua.",
    ),
    "w3": (
        "num = 3: 2 ∈ set → bỏ qua",
        "Số ba: có số hai, bỏ qua.",
    ),
    "w100": (
        "num = 100: 99 ∉ set → đầu dãy, nhưng 101 không có → length = 1",
        "Số một trăm: không có chín mươi chín, nên là đầu dãy. Nhưng một trăm lẻ một không có, độ dài chỉ là một.",
    ),
    "w4": (
        "num = 4: 3 ∈ set → bỏ qua",
        "Số bốn: có số ba, bỏ qua.",
    ),
    "w200": (
        "num = 200: đầu dãy, length = 1 · hết set → return best = 4",
        "Số hai trăm: đầu dãy, độ dài một. Hết sét, trả về độ dài lớn nhất là bốn.",
    ),
    "trap": (
        "Vì sao phải kiểm tra num − 1? Bỏ đi thì số nào cũng đếm tới cuối dãy",
        "Vì sao phải kiểm tra số liền trước? Nếu bỏ đi, số nào cũng đếm tới tận cuối dãy.",
    ),
    "trap_count": (
        "nums = 1…6: 6 + 5 + 4 + 3 + 2 + 1 = 21 bước = n(n+1)/2 → O(n²)",
        "Với mảng từ một tới sáu: sáu cộng năm cộng bốn, cộng ba cộng hai cộng một, là hai mươi mốt bước. Tức là ô en bình phương.",
    ),
    "trap_fix": (
        "Có kiểm tra: chỉ số 1 đi hết dãy → 6 bước. Mỗi số được \"đi qua\" đúng 1 lần → O(n)",
        "Có kiểm tra, chỉ số một đi hết dãy, sáu bước. Mỗi số chỉ được đi qua đúng một lần, nên là ô en.",
    ),
    "py_intro": (
        "Code Python — leetcode-38-bai/lc128-longest-consecutive-sequence.py",
        "Giờ xem code Pai thon.",
    ),
    "py_set": (
        "set(nums): bỏ số trùng + tra cứu O(1)",
        "Dòng đầu tạo sét: vừa bỏ số trùng, vừa tra cứu ô một.",
    ),
    "py_start": (
        "if num − 1 not in num_set: chỉ đầu dãy mới được đếm",
        "Điều kiện này đảm bảo chỉ đầu dãy mới được đếm.",
    ),
    "py_while": (
        "while num + length in num_set: đếm tới hết dãy → cập nhật best",
        "Vòng oai đếm tới khi hết dãy, rồi cập nhật độ dài lớn nhất.",
    ),
    "java_intro": (
        "Java 21 — groupc/LongestConsecutiveSequence.java: cùng thuật toán, tách hàm sequenceLengthFrom",
        "Bản gia va dùng cùng thuật toán, và tách phần đếm ra một hàm riêng.",
    ),
    "java_start": (
        "boolean isSequenceStart = !values.contains(value − 1): đặt tên cho điều kiện → dễ đọc, dễ giải thích",
        "Điều kiện đầu dãy được đặt tên thành một biến. Dễ đọc, và dễ giải thích khi phỏng vấn.",
    ),
    "java_trap": (
        "Bẫy: lặp trên values (set), đừng lặp trên nums — 1000 số 1 trùng: 999 → 999.000 bước",
        "Cẩn thận: phải lặp trên sét, không lặp trên mảng gốc. Nếu mảng có một nghìn số một trùng nhau, số bước tăng từ chín trăm chín mươi chín lên gần một triệu.",
    ),
    "complexity": (
        "Time O(n): mỗi số 1 lần kiểm tra num − 1 + tối đa 1 lần được đi qua · Space O(n) cho set",
        "Độ phức tạp: thời gian ô en, vì mỗi số chỉ được kiểm tra một lần và đi qua tối đa một lần. Bộ nhớ ô en cho sét.",
    ),
    "tradeoff": (
        "Sort: O(n log n) · Set không kiểm tra đầu dãy: O(n²) · Set + đầu dãy: O(n) · Union-Find: gần O(n), phức tạp hơn",
        "Tóm lại: sắp xếp thì ô en lốc en. Dùng sét mà không kiểm tra đầu dãy thì ô en bình phương. Có kiểm tra thì ô en. Diu ni ơn phai cũng gần ô en, nhưng phức tạp hơn nhiều.",
    ),
    "outro": (
        "Tiếp theo: Bài 33 · Subarray Sum Equals K (#560)",
        "Bài tiếp theo: sấp a rây xăm i quần kê.",
    ),
}

TTS_EN = {
    "intro": "Bài ba mươi hai. Longest Consecutive Sequence, LeetCode số 128.",
    "problem": "Đề bài: tìm độ dài dãy số nguyên liên tiếp dài nhất. Liên tiếp về giá trị, không cần nằm cạnh nhau trong mảng. Và phải chạy trong O n.",
    "sort": "Cách thứ nhất: sort rồi đếm các đoạn liền nhau. Đúng, nhưng tốn O n log n, chưa đạt yêu cầu.",
    "idea": "Cách thứ hai: bỏ hết vào một HashSet. Hỏi một số có trong set hay không chỉ tốn O một.",
    "key": "Mấu chốt: chỉ bắt đầu đếm ở đầu dãy, tức là khi num trừ một không có trong set.",
    "w1": "Số một: số không không có trong set, nên một là đầu dãy. Đếm tiếp: hai, ba, bốn đều có, năm thì không. Length bằng bốn.",
    "w2": "Số hai: số một có trong set, nên hai không phải đầu dãy. Bỏ qua.",
    "w100": "Số một trăm: không có chín mươi chín, nên là đầu dãy. Nhưng một trăm lẻ một không có, length chỉ là một.",
    "w200": "Số hai trăm: đầu dãy, length một. Hết set, return best bằng bốn.",
    "trap": "Vì sao phải kiểm tra num trừ một? Nếu bỏ đi, số nào cũng đếm tới tận cuối dãy.",
    "trap_count": "Với mảng từ một tới sáu: sáu cộng năm cộng bốn, cộng ba cộng hai cộng một, là hai mươi mốt bước. Tức là O n bình phương.",
    "trap_fix": "Có kiểm tra, chỉ số một đi hết dãy, sáu bước. Mỗi số chỉ được đi qua đúng một lần, nên là O n.",
    "py_intro": "Giờ xem code Python.",
    "py_set": "Dòng đầu tạo set: vừa bỏ số trùng, vừa tra cứu O một.",
    "py_start": "Điều kiện num trừ một not in num set đảm bảo chỉ đầu dãy mới được đếm.",
    "py_while": "Vòng while đếm tới khi hết dãy, rồi cập nhật best.",
    "java_intro": "Bản Java dùng cùng thuật toán, và tách phần đếm ra hàm sequence length from.",
    "java_start": "Điều kiện đầu dãy được đặt tên thành biến is sequence start. Dễ đọc, và dễ giải thích khi phỏng vấn.",
    "java_trap": "Cẩn thận: phải lặp trên values, là set, không lặp trên nums. Nếu mảng có một nghìn số một trùng nhau, số bước tăng từ chín trăm chín mươi chín lên gần một triệu.",
    "complexity": "Độ phức tạp: thời gian O n, vì mỗi số chỉ được kiểm tra một lần và đi qua tối đa một lần. Bộ nhớ O n cho set.",
    "tradeoff": "Tóm lại: sort thì O n log n. Dùng set mà không kiểm tra đầu dãy thì O n bình phương. Có kiểm tra thì O n. Union Find cũng gần O n, nhưng phức tạp hơn nhiều.",
    "outro": "Bài tiếp theo: Subarray Sum Equals K.",
}
