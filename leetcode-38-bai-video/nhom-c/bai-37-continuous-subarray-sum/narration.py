"""Kịch bản bài 37 — mỗi đoạn: (phụ đề hiển thị, câu đọc phiên âm).

Đã kiểm chứng bằng code thật + vét cạn: [23,2,4,6,7] k=6 → True · [5,6,1,3] k=6 → True (bước i=1 bị loại vì
đoạn dài 1) · [6,6] k=6 → True, nhưng nếu ghi đè chỉ số → False · [0] k=1 → False, [0,0] → True
· Java −1 % 6 = −1, Python −1 % 6 = 5. Ràng buộc đề (LeetCode 523): 0 ≤ nums[i], tổng ≤ 2³¹ − 1, k ≥ 1.

TEXT=phienam|tienganh khi build: SEGMENTS[key][1] phiên âm, TTS_EN[key] giữ từ tiếng Anh.
"""

LESSON = "Bài 37 — Continuous Subarray Sum"

SEGMENTS = {
    "intro": (
        "Bài 37 · Continuous Subarray Sum (LeetCode #523)",
        "Bài ba mươi bảy. Con ti nhu ợt sấp a rây xăm, lít cốt số năm trăm hai mươi ba.",
    ),
    "problem": (
        "Đề bài: có đoạn con liên tiếp DÀI ≥ 2 mà tổng là bội số của k không?",
        "Đề bài: kiểm tra xem có đoạn con liên tiếp nào dài ít nhất hai phần tử, mà tổng chia hết cho kê hay không.",
    ),
    "example": (
        "[23, 2, 4, 6, 7], k = 6 → true: [2, 4] có tổng 6",
        "Ví dụ: hai mươi ba, hai, bốn, sáu, bảy, với kê bằng sáu. Có đoạn hai, bốn, tổng bằng sáu, nên là tru.",
    ),
    "brute": (
        "Cách 1: thử mọi đoạn dài ≥ 2 → O(n²)",
        "Cách thứ nhất: thử mọi đoạn có độ dài từ hai trở lên. Ô en bình phương.",
    ),
    "idea": (
        "Hai tiền tố có CÙNG số dư khi chia k → đoạn giữa chúng chia hết cho k · 23 % 6 = 5, 29 % 6 = 5 → 29 − 23 = 6",
        "Ý tưởng: nếu hai tổng tiền tố có cùng số dư khi chia cho kê, thì đoạn nằm giữa chúng chia hết cho kê. Ví dụ hai mươi ba và hai mươi chín cùng dư năm, hiệu của chúng là sáu.",
    ),
    "idea2": (
        "HashMap: số dư → chỉ số ĐẦU TIÊN gặp · khởi tạo {0: −1} cho đoạn bắt đầu từ đầu mảng",
        "Dùng một hát mép lưu mỗi số dư gặp lần đầu ở vị trí nào. Khởi tạo số dư không ở vị trí trừ một, để bắt được các đoạn bắt đầu từ đầu mảng.",
    ),
    "w0": (
        "i = 0: P = 5, 5 % 6 = 5 → chưa có → ghi {5: 0}",
        "Vị trí không: tổng tiền tố năm, dư năm. Chưa có, ghi số dư năm ở vị trí không.",
    ),
    "w1": (
        "i = 1: P = 11, dư 5 → đã có ở 0 · khoảng cách 1 → đoạn [6] chỉ dài 1 → CHƯA được, và KHÔNG ghi đè",
        "Vị trí một: tổng mười một, lại dư năm, đã gặp ở vị trí không. Nhưng khoảng cách chỉ là một, đoạn sáu chỉ dài một phần tử. Chưa được. Và không ghi đè vị trí cũ.",
    ),
    "w2": (
        "i = 2: P = 12, dư 0 → có ở −1 · khoảng cách 3 > 1 → [5, 6, 1] tổng 12 → return True",
        "Vị trí hai: tổng mười hai, dư không. Số dư không đã có ở vị trí trừ một. Khoảng cách ba, đoạn năm, sáu, một có tổng mười hai. Trả về tru.",
    ),
    "overwrite": (
        "Vì sao chỉ giữ chỉ số ĐẦU TIÊN? [6, 6], k = 6: ghi đè mỗi lần → khoảng cách luôn 1 → False — đúng là True",
        "Vì sao chỉ giữ vị trí đầu tiên? Thử sáu, sáu với kê bằng sáu. Nếu ghi đè mỗi lần, khoảng cách luôn là một, ra phon. Nhưng đáp án đúng là tru, vì sáu cộng sáu bằng mười hai.",
    ),
    "py_intro": (
        "Code Python — leetcode-38-bai/lc523-continuous-subarray-sum.py",
        "Giờ xem code Pai thon.",
    ),
    "py_rem": (
        "remainder_index = {0: −1} · remainder = prefix_sum % k",
        "Khởi tạo số dư không ở vị trí trừ một, rồi mỗi bước tính số dư của tổng tiền tố.",
    ),
    "py_len": (
        "i − remainder_index[remainder] > 1 ⇔ đoạn dài ≥ 2",
        "Khoảng cách lớn hơn một, nghĩa là đoạn dài ít nhất hai phần tử.",
    ),
    "py_else": (
        "else: chỉ ghi khi số dư CHƯA có → giữ chỉ số đầu tiên",
        "Nhánh else chỉ ghi khi số dư chưa có. Nhờ vậy luôn giữ vị trí đầu tiên.",
    ),
    "java_intro": (
        "Java 21 — groupc/ContinuousSubarraySum.java: cùng thuật toán, get() trả null khi chưa có",
        "Bản gia va dùng cùng thuật toán. Hàm ghét trả về nâu khi số dư chưa có.",
    ),
    "java_mod": (
        "Bẫy: Java −1 % 6 = −1 (Python ra 5). An toàn ở đây vì đề cho nums[i] ≥ 0 · có số âm → ((x % k) + k) % k",
        "Cẩn thận: trong gia va, trừ một chia lấy dư cho sáu ra trừ một, còn Pai thon ra năm. Ở đây an toàn vì đề cho mọi số không âm. Nếu có số âm, phải cộng thêm kê rồi chia lấy dư lần nữa.",
    ),
    "java_int": (
        "Tràn số? Đề cho tổng ≤ 2³¹ − 1 và k ≥ 1 → int không tràn, không chia cho 0",
        "Có tràn số không? Đề cho tổng không quá hai mũ ba mốt trừ một, và kê ít nhất bằng một. Nên kiểu in không tràn, và không chia cho không.",
    ),
    "compare33": (
        "So với bài 33: bài 33 ĐẾM → map lưu số lần · bài này hỏi CÓ/KHÔNG + cần độ dài → map lưu chỉ số đầu tiên",
        "So với bài ba mươi ba: bài đó đếm số đoạn, nên bảng lưu số lần. Bài này chỉ hỏi có hay không, và cần độ dài, nên bảng lưu vị trí đầu tiên.",
    ),
    "complexity": (
        "Time O(n) · Space O(min(n, k)) — tối đa k số dư khác nhau",
        "Độ phức tạp: thời gian ô en. Bộ nhớ không quá n, và không quá kê, vì chỉ có kê số dư khác nhau.",
    ),
    "outro": (
        "Tiếp theo: Bài 38 · Design HashMap (#706) — bài cuối nhóm C",
        "Bài tiếp theo, cũng là bài cuối của nhóm xê: tự thiết kế một hát mép.",
    ),
}

TTS_EN = {
    "intro": "Bài ba mươi bảy. Continuous Subarray Sum, LeetCode số 523.",
    "problem": "Đề bài: kiểm tra xem có subarray liên tiếp nào dài ít nhất hai phần tử, mà tổng là bội số của k hay không.",
    "example": "Ví dụ: hai mươi ba, hai, bốn, sáu, bảy, với k bằng sáu. Có đoạn hai, bốn, tổng bằng sáu, nên là true.",
    "brute": "Cách thứ nhất: brute force, thử mọi subarray dài từ hai trở lên. O n bình phương.",
    "idea": "Ý tưởng: nếu hai prefix sum có cùng số dư khi chia cho k, thì đoạn nằm giữa chúng chia hết cho k. Ví dụ hai mươi ba và hai mươi chín cùng dư năm, hiệu của chúng là sáu.",
    "idea2": "Dùng một HashMap lưu mỗi remainder gặp lần đầu ở index nào. Khởi tạo remainder không ở index trừ một, để bắt được các đoạn bắt đầu từ đầu mảng.",
    "w0": "Index không: prefix sum năm, dư năm. Chưa có, ghi remainder năm ở index không.",
    "w1": "Index một: prefix sum mười một, lại dư năm, đã gặp ở index không. Nhưng khoảng cách chỉ là một, đoạn sáu chỉ dài một phần tử. Chưa được. Và không ghi đè index cũ.",
    "w2": "Index hai: prefix sum mười hai, dư không. Remainder không đã có ở index trừ một. Khoảng cách ba, đoạn năm, sáu, một có tổng mười hai. Return true.",
    "overwrite": "Vì sao chỉ giữ index đầu tiên? Thử sáu, sáu với k bằng sáu. Nếu ghi đè mỗi lần, khoảng cách luôn là một, ra false. Nhưng đáp án đúng là true, vì sáu cộng sáu bằng mười hai.",
    "py_intro": "Giờ xem code Python.",
    "py_rem": "Khởi tạo remainder index với không bằng trừ một, rồi mỗi bước tính prefix sum modulo k.",
    "py_len": "Khoảng cách lớn hơn một, nghĩa là đoạn dài ít nhất hai phần tử.",
    "py_else": "Nhánh else chỉ ghi khi remainder chưa có. Nhờ vậy luôn giữ index đầu tiên.",
    "java_intro": "Bản Java dùng cùng thuật toán. Hàm get trả về null khi remainder chưa có.",
    "java_mod": "Cẩn thận: trong Java, trừ một modulo sáu ra trừ một, còn Python ra năm. Ở đây an toàn vì đề cho mọi số không âm. Nếu có số âm, phải cộng thêm k rồi modulo lần nữa.",
    "java_int": "Có overflow không? Đề cho tổng không quá hai mũ ba mốt trừ một, và k ít nhất bằng một. Nên kiểu int không tràn, và không chia cho không.",
    "compare33": "So với bài 33: bài đó đếm số subarray, nên map lưu count. Bài này chỉ hỏi có hay không, và cần độ dài, nên map lưu index đầu tiên.",
    "complexity": "Độ phức tạp: thời gian O n. Bộ nhớ O min n k, vì chỉ có k remainder khác nhau.",
    "outro": "Bài tiếp theo, cũng là bài cuối của nhóm C: Design HashMap.",
}
