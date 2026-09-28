"""Kịch bản bài 33 — mỗi đoạn: (phụ đề hiển thị, câu đọc phiên âm).

Mọi con số đã kiểm chứng bằng code thật (lc560 + SubarraySumEqualsK.java + brute force):
[1, 2, 1, -1, 2, 1], k = 3 → 5 · thiếu {0: 1} → 3 · ghi trước rồi mới tra với [1, -1, 0], k = 0 → 6 (đúng: 3)
· defaultdict tạo thêm key rác -2: 0 và 2: 0, Java getOrDefault thì không.

TEXT=phienam|tienganh khi build: SEGMENTS[key][1] phiên âm, TTS_EN[key] giữ từ tiếng Anh.
"""

LESSON = "Bài 33 — Subarray Sum Equals K"

SEGMENTS = {
    "intro": (
        "Bài 33 · Subarray Sum Equals K (LeetCode #560)",
        "Bài ba mươi ba. Sấp a rây xăm i quần kê, lít cốt số năm trăm sáu mươi.",
    ),
    "problem": (
        "Đề bài: đếm số mảng con LIÊN TIẾP có tổng đúng bằng k (có thể có số âm)",
        "Đề bài: đếm xem có bao nhiêu mảng con liên tiếp có tổng đúng bằng kê. Mảng có thể có số âm.",
    ),
    "example": (
        "nums = [1, 1, 1], k = 2 → 2: [1, 1] ở vị trí 0–1 và 1–2",
        "Ví dụ: một, một, một, với kê bằng hai. Có hai mảng con: vị trí không tới một, và một tới hai.",
    ),
    "brute": (
        "Cách 1: thử mọi mảng con — n(n+1)/2 mảng → O(n²)",
        "Cách thứ nhất: thử mọi mảng con. Có khoảng n bình phương chia hai mảng con, nên tốn ô en bình phương.",
    ),
    "window": (
        "Sliding window? KHÔNG được — có số âm thì tổng lúc tăng lúc giảm, không biết nên nới hay thu",
        "Dùng cửa sổ trượt được không? Không. Khi có số âm, tổng lúc tăng lúc giảm, ta không biết nên nới rộng hay thu hẹp cửa sổ.",
    ),
    "prefix_idea": (
        "Tổng tiền tố P: tổng(i … j) = P[j + 1] − P[i] · ví dụ [2, 1] = 4 − 1 = 3",
        "Dùng tổng tiền tố pê. Tổng của một đoạn bằng hiệu của hai tổng tiền tố. Ví dụ đoạn hai, một bằng bốn trừ một, là ba.",
    ),
    "prefix_eq": (
        "Tổng đoạn = k ⇔ P[i] = P − k → đếm xem P − k đã xuất hiện bao nhiêu lần: HashMap",
        "Đoạn có tổng bằng kê, khi và chỉ khi có một tổng tiền tố trước đó bằng pê trừ kê. Vậy chỉ cần đếm xem pê trừ kê đã xuất hiện bao nhiêu lần, bằng một hát mép.",
    ),
    "init": (
        "Khởi tạo {0: 1} = tổng rỗng trước phần tử đầu → đếm được mảng con bắt đầu từ vị trí 0",
        "Khởi tạo bảng với tổng không, xuất hiện một lần. Đó là tổng rỗng trước phần tử đầu tiên, để đếm được các mảng con bắt đầu từ vị trí không.",
    ),
    "s0": (
        "num = 1: P = 1, cần P − k = −2 → chưa gặp, +0 · ghi P = 1",
        "Số một: pê bằng một. Cần tìm trừ hai, chưa gặp. Ghi pê bằng một vào bảng.",
    ),
    "s1": (
        "num = 2: P = 3, cần 0 → map[0] = 1 → +1: [1, 2]",
        "Số hai: pê bằng ba. Cần tìm không, có một lần. Cộng một, đó là đoạn một, hai.",
    ),
    "s2": (
        "num = 1: P = 4, cần 1 → map[1] = 1 → +1: [2, 1]",
        "Số một: pê bằng bốn. Cần tìm một, có một lần. Cộng một, đoạn hai, một.",
    ),
    "s3": (
        "num = −1: P = 3, cần 0 → +1: [1, 2, 1, −1] · map[3] thành 2",
        "Số trừ một: pê quay về ba. Cần tìm không, cộng một, đoạn một, hai, một, trừ một. Tổng ba giờ đã gặp hai lần.",
    ),
    "s4": (
        "num = 2: P = 5, cần 2 → chưa gặp, +0",
        "Số hai: pê bằng năm. Cần tìm hai, chưa gặp.",
    ),
    "s5": (
        "num = 1: P = 6, cần 3 → map[3] = 2 → +2: [1, −1, 2, 1] và [2, 1] · result = 5",
        "Số một: pê bằng sáu. Cần tìm ba, đã gặp hai lần, nên cộng hai cùng lúc. Tổng cộng là năm.",
    ),
    "py_intro": (
        "Code Python — leetcode-38-bai/lc560-subarray-sum-equals-k.py",
        "Giờ xem code Pai thon.",
    ),
    "py_init": (
        "prefix_count[0] = 1 — thiếu dòng này, ví dụ vừa rồi ra 3 thay vì 5",
        "Dòng khởi tạo tổng không bằng một. Thiếu dòng này, ví dụ vừa rồi ra ba thay vì năm.",
    ),
    "py_order": (
        "Thứ tự: tra map[P − k] TRƯỚC, ghi P SAU. Ngược lại: [1, −1, 0], k = 0 → 6 (đúng là 3)",
        "Thứ tự rất quan trọng: tra bảng trước, rồi mới ghi pê. Làm ngược lại, với kê bằng không, sẽ đếm cả đoạn rỗng: ra sáu thay vì ba.",
    ),
    "py_default": (
        "Bẫy Python: đọc defaultdict bằng [x] sẽ TẠO key x = 0 → map có key rác −2: 0, 2: 0 · dùng .get(x, 0) nếu muốn gọn",
        "Một điểm tinh tế của Pai thon: đọc đi phôn đích bằng ngoặc vuông sẽ tự tạo khóa mới bằng không. Kết quả vẫn đúng, nhưng bảng bị phình. Dùng hàm ghét nếu muốn gọn.",
    ),
    "java_intro": (
        "Java 21 — groupc/SubarraySumEqualsK.java: cùng thuật toán, HashMap<Integer, Integer>",
        "Bản gia va dùng cùng thuật toán, với một hát mép.",
    ),
    "java_get": (
        "getOrDefault(P − k, 0): chỉ đọc, KHÔNG tạo key mới — map chỉ có 6 key thật",
        "Hàm ghét o đi phôn chỉ đọc, không tạo khóa mới. Bảng chỉ chứa đúng sáu tổng đã gặp.",
    ),
    "java_merge": (
        "merge(P, 1, Integer::sum): chưa có → 1, có rồi → cộng thêm 1 — một dòng thay cho if/else",
        "Hàm mơ giơ: chưa có khóa thì đặt bằng một, có rồi thì cộng thêm một. Một dòng thay cho cả if else.",
    ),
    "complexity": (
        "Time O(n) — duyệt 1 lần, mỗi bước O(1) · Space O(n) — tối đa n + 1 tổng tiền tố",
        "Độ phức tạp: thời gian ô en, duyệt một lần. Bộ nhớ ô en, cho tối đa n cộng một tổng tiền tố.",
    ),
    "tradeoff": (
        "Brute force: O(n²) · Sliding window: O(n) nhưng chỉ đúng khi mọi số > 0 · Prefix + HashMap: O(n), mọi trường hợp",
        "Tóm lại: thử mọi đoạn thì ô en bình phương. Cửa sổ trượt thì ô en, nhưng chỉ đúng khi mọi số dương. Tổng tiền tố cộng hát mép thì ô en, đúng trong mọi trường hợp.",
    ),
    "outro": (
        "Tiếp theo: Bài 34 · Intersection of Two Arrays II (#350)",
        "Bài tiếp theo: in tơ séc sần ốp tu ơ rây tu.",
    ),
}

TTS_EN = {
    "intro": "Bài ba mươi ba. Subarray Sum Equals K, LeetCode số 560.",
    "problem": "Đề bài: đếm xem có bao nhiêu subarray liên tiếp có tổng đúng bằng k. Mảng có thể có số âm.",
    "example": "Ví dụ: một, một, một, với k bằng hai. Có hai subarray: vị trí không tới một, và một tới hai.",
    "brute": "Cách thứ nhất: brute force, thử mọi subarray. Có khoảng n bình phương chia hai subarray, nên tốn O n bình phương.",
    "window": "Dùng sliding window được không? Không. Khi có số âm, tổng lúc tăng lúc giảm, ta không biết nên nới rộng hay thu hẹp window.",
    "prefix_idea": "Dùng prefix sum P. Tổng của một đoạn bằng hiệu của hai prefix sum. Ví dụ đoạn hai, một bằng bốn trừ một, là ba.",
    "prefix_eq": "Đoạn có tổng bằng k, khi và chỉ khi có một prefix sum trước đó bằng P trừ k. Vậy chỉ cần đếm xem P trừ k đã xuất hiện bao nhiêu lần, bằng một HashMap.",
    "init": "Khởi tạo map với key không, count một. Đó là prefix rỗng trước phần tử đầu tiên, để đếm được các subarray bắt đầu từ vị trí không.",
    "s0": "Số một: P bằng một. Cần tìm trừ hai, chưa gặp. Ghi P bằng một vào map.",
    "s1": "Số hai: P bằng ba. Cần tìm không, có một lần. Cộng một, đó là đoạn một, hai.",
    "s2": "Số một: P bằng bốn. Cần tìm một, có một lần. Cộng một, đoạn hai, một.",
    "s3": "Số trừ một: P quay về ba. Cần tìm không, cộng một, đoạn một, hai, một, trừ một. Key ba giờ có count hai.",
    "s4": "Số hai: P bằng năm. Cần tìm hai, chưa gặp.",
    "s5": "Số một: P bằng sáu. Cần tìm ba, count bằng hai, nên cộng hai cùng lúc. Result bằng năm.",
    "py_intro": "Giờ xem code Python.",
    "py_init": "Dòng prefix count của không bằng một. Thiếu dòng này, ví dụ vừa rồi ra ba thay vì năm.",
    "py_order": "Thứ tự rất quan trọng: tra map trước, rồi mới ghi P. Làm ngược lại, với k bằng không, sẽ đếm cả subarray rỗng: ra sáu thay vì ba.",
    "py_default": "Một điểm tinh tế của Python: đọc defaultdict bằng ngoặc vuông sẽ tự tạo key mới bằng không. Kết quả vẫn đúng, nhưng map bị phình. Dùng hàm get nếu muốn gọn.",
    "java_intro": "Bản Java dùng cùng thuật toán, với HashMap Integer, Integer.",
    "java_get": "Hàm getOrDefault chỉ đọc, không tạo key mới. Map chỉ chứa đúng sáu prefix sum đã gặp.",
    "java_merge": "Hàm merge: chưa có key thì đặt bằng một, có rồi thì cộng thêm một. Một dòng thay cho cả if else.",
    "complexity": "Độ phức tạp: thời gian O n, duyệt một lần. Bộ nhớ O n, cho tối đa n cộng một prefix sum.",
    "tradeoff": "Tóm lại: brute force thì O n bình phương. Sliding window thì O n, nhưng chỉ đúng khi mọi số dương. Prefix sum cộng HashMap thì O n, đúng trong mọi trường hợp.",
    "outro": "Bài tiếp theo: Intersection of Two Arrays Two.",
}
