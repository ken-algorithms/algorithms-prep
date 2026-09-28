"""Kịch bản bài 34 — mỗi đoạn: (phụ đề hiển thị, câu đọc phiên âm).

Đã kiểm chứng bằng code thật: [1,2,2,1] ∩ [2,2] → [2, 2] · [4,9,5,4] ∩ [9,4,9,8,4] → [9, 4, 4]
(#349 bằng set → [9, 4]) · Counter[8] KHÔNG tạo key (defaultdict thì có) · two pointers trên mảng đã sort → [4, 4, 9].
Lưu ý: code luôn đếm nums1 → Space O(n) theo nums1; O(min(n, m)) chỉ khi đếm mảng nhỏ hơn.

TEXT=phienam|tienganh khi build: SEGMENTS[key][1] phiên âm, TTS_EN[key] giữ từ tiếng Anh.
"""

LESSON = "Bài 34 — Intersection of Two Arrays II"

SEGMENTS = {
    "intro": (
        "Bài 34 · Intersection of Two Arrays II (LeetCode #350)",
        "Bài ba mươi tư. In tơ séc sần ốp tu ơ rây tu, lít cốt số ba trăm năm mươi.",
    ),
    "problem": (
        "Đề bài: giao của 2 mảng, GIỮ số lần lặp — số nào xuất hiện ở cả hai bao nhiêu lần thì lấy bấy nhiêu",
        "Đề bài: tìm phần giao của hai mảng, và giữ đúng số lần lặp. Một số xuất hiện ở cả hai mảng bao nhiêu lần, thì lấy bấy nhiêu lần.",
    ),
    "example": (
        "[1, 2, 2, 1] ∩ [2, 2] → [2, 2] · bài #349 (chỉ lấy giá trị duy nhất) sẽ ra [2]",
        "Ví dụ: một, hai, hai, một, giao với hai, hai, ra hai, hai. Khác bài ba trăm bốn mươi chín, chỉ lấy giá trị duy nhất, sẽ ra một số hai.",
    ),
    "naive": (
        "Cách 1: mỗi số của nums2 đi tìm rồi xoá trong nums1 → O(n · m)",
        "Cách thứ nhất: với mỗi số của mảng hai, đi tìm nó trong mảng một, thấy thì xóa đi. Tốn ô en nhân em.",
    ),
    "idea": (
        "Cách 2: đếm nums1 bằng Counter = số \"suất\" của mỗi giá trị · lấy một lần → trừ 1 suất",
        "Cách tốt hơn: đếm mảng một bằng cao tơ. Mỗi giá trị có bấy nhiêu suất. Mỗi lần lấy thì trừ đi một suất.",
    ),
    "c0": (
        "counts = Counter(nums1): 4 → 2 suất, 9 → 1, 5 → 1",
        "Đếm mảng một: số bốn có hai suất, số chín một suất, số năm một suất.",
    ),
    "w0": (
        "num = 9: còn 1 suất → lấy · 9 còn 0",
        "Số chín: còn một suất, lấy. Chín hết suất.",
    ),
    "w1": (
        "num = 4: còn 2 suất → lấy · 4 còn 1",
        "Số bốn: còn hai suất, lấy. Còn một.",
    ),
    "w2": (
        "num = 9: hết suất (0) → bỏ qua — nums1 chỉ có một số 9",
        "Lại số chín: hết suất rồi, bỏ qua. Vì mảng một chỉ có một số chín.",
    ),
    "w3": (
        "num = 8: không có trong nums1 → counts[8] = 0 → bỏ qua",
        "Số tám: không có trong mảng một, đọc ra không. Bỏ qua.",
    ),
    "w4": (
        "num = 4: còn 1 suất → lấy · kết quả [9, 4, 4]",
        "Số bốn: còn một suất, lấy. Kết quả là chín, bốn, bốn.",
    ),
    "py_intro": (
        "Code Python — leetcode-38-bai/lc350-intersection-of-two-arrays-ii.py",
        "Giờ xem code Pai thon.",
    ),
    "py_counter": (
        "Counter(nums1): đếm một lần, O(n)",
        "Dòng đầu đếm mảng một bằng cao tơ, một lần duyệt.",
    ),
    "py_check": (
        "counts[num] > 0: còn suất mới lấy · đọc Counter[x] KHÔNG tạo key — khác defaultdict ở bài 33",
        "Chỉ lấy khi còn suất. Và đọc cao tơ bằng ngoặc vuông không tạo khóa mới. Khác với đi phôn đích ở bài ba mươi ba.",
    ),
    "py_order": (
        "Kết quả theo thứ tự của nums2 — đề cho phép trả về thứ tự bất kỳ",
        "Kết quả đi theo thứ tự của mảng hai. Đề cho phép trả về thứ tự bất kỳ.",
    ),
    "java_intro": (
        "Java 21 — groupc/IntersectionOfTwoArrays.java: merge để đếm, getOrDefault để đọc",
        "Bản gia va: dùng mơ giơ để đếm, và ghét o đi phôn để đọc số suất còn lại.",
    ),
    "java_put": (
        "put(num, available − 1): trừ suất · cuối cùng stream() đổi List<Integer> → int[]",
        "Lấy xong thì ghi lại số suất trừ một. Cuối cùng, đổi danh sách sang mảng số nguyên bằng sờ trim.",
    ),
    "fu_sorted": (
        "Follow-up 1: cả 2 mảng đã sort → hai con trỏ, bộ nhớ thêm O(1)",
        "Câu hỏi mở rộng thứ nhất: nếu cả hai mảng đã được sắp xếp, dùng hai con trỏ, không cần bộ nhớ thêm.",
    ),
    "fu_small": (
        "Follow-up 2: nums1 nhỏ hơn nhiều → đếm mảng NHỎ → bộ nhớ O(min(n, m)) — code này luôn đếm nums1",
        "Thứ hai: nếu một mảng nhỏ hơn nhiều, hãy đếm mảng nhỏ, bộ nhớ chỉ bằng mảng nhỏ. Code hiện tại luôn đếm mảng một.",
    ),
    "fu_disk": (
        "Follow-up 3: nums2 quá lớn, nằm trên đĩa → giữ Counter mảng nhỏ trong RAM, đọc nums2 từng khúc",
        "Thứ ba: nếu mảng hai quá lớn, nằm trên đĩa, giữ bảng đếm của mảng nhỏ trong bộ nhớ, và đọc mảng hai từng khúc.",
    ),
    "complexity": (
        "Time O(n + m) · Space O(n) theo nums1 — đếm mảng nhỏ hơn thì O(min(n, m))",
        "Độ phức tạp: thời gian ô en cộng em. Bộ nhớ ô en theo mảng một. Nếu đếm mảng nhỏ hơn, thì chỉ bằng mảng nhỏ.",
    ),
    "outro": (
        "Tiếp theo: Bài 35 · Happy Number (#202)",
        "Bài tiếp theo: háp pi năm bơ.",
    ),
}

TTS_EN = {
    "intro": "Bài ba mươi tư. Intersection of Two Arrays Two, LeetCode số 350.",
    "example": "Ví dụ: một, hai, hai, một, giao với hai, hai, ra hai, hai. Khác bài 349, chỉ lấy giá trị duy nhất, sẽ ra một số hai.",
    "naive": "Cách thứ nhất: với mỗi số của nums2, đi tìm nó trong nums1, thấy thì xóa đi. Tốn O n nhân m.",
    "idea": "Cách tốt hơn: đếm nums1 bằng Counter. Mỗi giá trị có bấy nhiêu suất. Mỗi lần lấy thì trừ đi một suất.",
    "c0": "Đếm nums1: số bốn có hai suất, số chín một suất, số năm một suất.",
    "w2": "Lại số chín: hết suất rồi, bỏ qua. Vì nums1 chỉ có một số chín.",
    "w3": "Số tám: không có trong nums1, counts của tám bằng không. Bỏ qua.",
    "py_intro": "Giờ xem code Python.",
    "py_counter": "Dòng đầu đếm nums1 bằng Counter, một lần duyệt.",
    "py_check": "Chỉ lấy khi counts lớn hơn không. Và đọc Counter bằng ngoặc vuông không tạo key mới. Khác với defaultdict ở bài 33.",
    "py_order": "Kết quả đi theo thứ tự của nums2. Đề cho phép trả về thứ tự bất kỳ.",
    "java_intro": "Bản Java: dùng merge để đếm, và getOrDefault để đọc số suất còn lại.",
    "java_put": "Lấy xong thì put lại available trừ một. Cuối cùng, đổi List Integer sang mảng int bằng stream.",
    "fu_sorted": "Follow up thứ nhất: nếu cả hai mảng đã sort, dùng two pointers, bộ nhớ thêm O một.",
    "fu_small": "Thứ hai: nếu một mảng nhỏ hơn nhiều, hãy đếm mảng nhỏ, bộ nhớ O min n m. Code hiện tại luôn đếm nums1.",
    "fu_disk": "Thứ ba: nếu nums2 quá lớn, nằm trên đĩa, giữ Counter của mảng nhỏ trong RAM, và đọc nums2 từng khúc.",
    "complexity": "Độ phức tạp: thời gian O n cộng m. Bộ nhớ O n theo nums1. Nếu đếm mảng nhỏ hơn, thì O min n m.",
    "outro": "Bài tiếp theo: Happy Number.",
}
