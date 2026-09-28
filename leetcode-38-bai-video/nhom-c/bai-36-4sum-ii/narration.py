"""Kịch bản bài 36 — mỗi đoạn: (phụ đề hiển thị, câu đọc phiên âm).

Đã kiểm chứng bằng code thật + vét cạn: ví dụ đề [1,2] [-2,-1] [-1,2] [0,2] → 2 · ví dụ minh hoạ
[1,2] [-2,-1] [-1,1] [0,-1] → 4 (có 1 lần tra trúng tổng 0 đã gặp 2 lần) · defaultdict tạo key rác 2: 0
· n = 200: n⁴ = 1,6 tỷ, n³ = 8 triệu, n² = 40.000.

TEXT=phienam|tienganh khi build: SEGMENTS[key][1] phiên âm, TTS_EN[key] giữ từ tiếng Anh.
"""

LESSON = "Bài 36 — 4Sum II"

SEGMENTS = {
    "intro": (
        "Bài 36 · 4Sum II (LeetCode #454)",
        "Bài ba mươi sáu. Pho xăm tu, lít cốt số bốn trăm năm mươi tư.",
    ),
    "problem": (
        "Đề bài: 4 mảng cùng độ dài n — đếm số bộ (i, j, k, l) có nums1[i] + nums2[j] + nums3[k] + nums4[l] = 0",
        "Đề bài: có bốn mảng cùng độ dài n. Đếm xem có bao nhiêu bộ bốn chỉ số, mỗi mảng lấy một số, mà tổng bằng không.",
    ),
    "example": (
        "[1, 2] [−2, −1] [−1, 2] [0, 2] → 2 bộ: 1 − 2 − 1 + 2 = 0 và 2 − 1 − 1 + 0 = 0",
        "Ví dụ trong đề có hai bộ: một trừ hai trừ một cộng hai, và hai trừ một trừ một cộng không.",
    ),
    "brute": (
        "Cách 1: 4 vòng lặp lồng nhau → O(n⁴) · n = 200 → 1,6 tỷ bộ",
        "Cách thứ nhất: bốn vòng lặp lồng nhau, ô en mũ bốn. Với n bằng hai trăm, là một tỷ sáu trăm triệu bộ.",
    ),
    "three": (
        "Cách 2: 3 vòng + HashSet cho nums4 → O(n³) · n = 200 → vẫn 8 triệu",
        "Cách thứ hai: ba vòng lặp, và tra mảng thứ tư bằng hát mép. Ô en mũ ba, vẫn tám triệu phép tính.",
    ),
    "idea": (
        "Cách 3 — chia đôi: a + b = −(c + d) · đếm mọi tổng a + b vào HashMap, rồi tra −(c + d) → O(n²)",
        "Cách tốt nhất là chia đôi. A cộng bê phải bằng trừ của xê cộng đê. Đếm mọi tổng a cộng bê vào một hát mép, rồi với mỗi cặp xê đê, tra xem có bao nhiêu cặp khớp. Chỉ ô en bình phương.",
    ),
    "p1": (
        "Nửa đầu: mọi tổng a + b của nums1 × nums2 → sum_ab = {−1: 1, 0: 2, 1: 1}",
        "Nửa đầu: tính mọi tổng a cộng bê. Trừ một xuất hiện một lần, không xuất hiện hai lần, một xuất hiện một lần.",
    ),
    "q0": (
        "c = −1, d = 0 → cần a + b = 1 → sum_ab[1] = 1 → +1",
        "Nửa sau. Xê trừ một, đê bằng không: cần a cộng bê bằng một. Có một cặp, cộng một.",
    ),
    "q1": (
        "c = −1, d = −1 → cần 2 → chưa có → +0",
        "Xê trừ một, đê trừ một: cần hai. Không có cặp nào.",
    ),
    "q2": (
        "c = 1, d = 0 → cần −1 → sum_ab[−1] = 1 → +1",
        "Xê bằng một, đê bằng không: cần trừ một. Có một cặp, cộng một.",
    ),
    "q3": (
        "c = 1, d = −1 → cần 0 → sum_ab[0] = 2 → +2 cùng lúc · tổng = 4",
        "Xê bằng một, đê trừ một: cần không. Tổng không có hai cặp, nên cộng hai cùng lúc. Tổng cộng là bốn.",
    ),
    "why_count": (
        "Vì sao lưu SỐ LẦN, không chỉ có/không? Tổng 0 đến từ 2 cặp (1, −1) và (2, −2) → phải cộng 2",
        "Vì sao phải lưu số lần, không chỉ có hay không? Vì tổng không đến từ hai cặp khác nhau, và mỗi cặp là một bộ riêng.",
    ),
    "py_intro": (
        "Code Python — leetcode-38-bai/lc454-4sum-ii.py",
        "Giờ xem code Pai thon.",
    ),
    "py_phase1": (
        "2 vòng đầu: đếm mọi tổng a + b — n² cặp",
        "Hai vòng đầu đếm mọi tổng a cộng bê, n bình phương cặp.",
    ),
    "py_phase2": (
        "2 vòng sau: cộng số cặp (a, b) có tổng −(c + d) — mỗi lần tra O(1)",
        "Hai vòng sau cộng dồn số cặp khớp. Mỗi lần tra chỉ ô một.",
    ),
    "py_default": (
        "Lại bẫy defaultdict (bài 33): sum_ab[−(c + d)] tạo key rác khi không tìm thấy — ví dụ này thêm 2: 0",
        "Lại là điểm tinh tế của đi phôn đích như bài ba mươi ba: tra một tổng không có sẽ tạo khóa rác. Ví dụ này thêm khóa hai bằng không.",
    ),
    "java_intro": (
        "Java 21 — groupc/FourSumII.java: merge để đếm, getOrDefault để tra — không tạo key rác",
        "Bản gia va: mơ giơ để đếm, ghét o đi phôn để tra. Không tạo khóa rác.",
    ),
    "java_int": (
        "Tràn số? Đề giới hạn |nums[i]| ≤ 2²⁸ → |a + b| ≤ 2²⁹ < 2³¹ → int vẫn an toàn",
        "Có sợ tràn số không? Đề giới hạn mỗi số không quá hai mũ hai tám, nên tổng hai số không quá hai mũ hai chín, vẫn vừa kiểu in.",
    ),
    "ksum": (
        "Tổng quát: k mảng → chia 2 nửa, mỗi nửa k/2 mảng → O(n^(k/2)) — kỹ thuật meet in the middle",
        "Tổng quát lên: với ca mảng, chia thành hai nửa, mỗi nửa ca chia hai mảng. Độ phức tạp ô en mũ ca chia hai. Kỹ thuật này gọi là gặp nhau ở giữa.",
    ),
    "complexity": (
        "Time O(n²) · Space O(n²) — tối đa n² tổng a + b khác nhau",
        "Độ phức tạp: thời gian ô en bình phương. Bộ nhớ cũng ô en bình phương, cho tối đa n bình phương tổng khác nhau.",
    ),
    "outro": (
        "Tiếp theo: Bài 37 · Continuous Subarray Sum (#523)",
        "Bài tiếp theo: con ti nhu ợt sấp a rây xăm.",
    ),
}

TTS_EN = {
    "intro": "Bài ba mươi sáu. 4Sum Two, LeetCode số 454.",
    "brute": "Cách thứ nhất: brute force bốn vòng lặp lồng nhau, O n mũ bốn. Với n bằng hai trăm, là một tỷ sáu trăm triệu bộ.",
    "three": "Cách thứ hai: ba vòng lặp, và tra mảng thứ tư bằng HashMap. O n mũ ba, vẫn tám triệu phép tính.",
    "idea": "Cách tốt nhất là chia đôi. a cộng b phải bằng trừ của c cộng d. Đếm mọi tổng a cộng b vào một HashMap, rồi với mỗi cặp c d, tra xem có bao nhiêu cặp khớp. Chỉ O n bình phương.",
    "p1": "Nửa đầu: tính mọi tổng a cộng b vào sum ab. Trừ một xuất hiện một lần, không xuất hiện hai lần, một xuất hiện một lần.",
    "q0": "Nửa sau. c trừ một, d bằng không: cần a cộng b bằng một. Có một cặp, cộng một.",
    "q1": "c trừ một, d trừ một: cần hai. Không có cặp nào.",
    "q2": "c bằng một, d bằng không: cần trừ một. Có một cặp, cộng một.",
    "q3": "c bằng một, d trừ một: cần không. Tổng không có count bằng hai, nên cộng hai cùng lúc. Tổng cộng là bốn.",
    "why_count": "Vì sao phải lưu count, không chỉ có hay không? Vì tổng không đến từ hai cặp khác nhau, và mỗi cặp là một bộ riêng.",
    "py_intro": "Giờ xem code Python.",
    "py_phase1": "Hai vòng đầu đếm mọi tổng a cộng b vào sum ab, n bình phương cặp.",
    "py_phase2": "Hai vòng sau cộng dồn sum ab của trừ c cộng d. Mỗi lần tra chỉ O một.",
    "py_default": "Lại là điểm tinh tế của defaultdict như bài 33: tra một tổng không có sẽ tạo key rác. Ví dụ này thêm key hai bằng không.",
    "java_intro": "Bản Java: merge để đếm, getOrDefault để tra. Không tạo key rác.",
    "java_int": "Có sợ overflow không? Đề giới hạn mỗi số không quá hai mũ hai tám, nên tổng hai số không quá hai mũ hai chín, vẫn vừa kiểu int.",
    "ksum": "Tổng quát lên: với k mảng, chia thành hai nửa, mỗi nửa k chia hai mảng. Độ phức tạp O n mũ k chia hai. Kỹ thuật này gọi là meet in the middle.",
    "complexity": "Độ phức tạp: thời gian O n bình phương. Bộ nhớ cũng O n bình phương, cho tối đa n bình phương tổng khác nhau.",
    "outro": "Bài tiếp theo: Continuous Subarray Sum.",
}
