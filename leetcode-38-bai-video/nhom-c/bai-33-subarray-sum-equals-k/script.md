# Bài 33 — Subarray Sum Equals K — kịch bản lời đọc (sinh tự động từ narration.py)

Giọng: `vi-VN-NamMinhNeural` · kiểu lời đọc: `phienam`

| Đoạn | Giây | Phụ đề | Lời đọc TTS |
|---|---|---|---|
| `intro` | 6.48 | Bài 33 · Subarray Sum Equals K (LeetCode #560) | Bài ba mươi ba. Sấp a rây xăm i quần kê, lít cốt số năm trăm sáu mươi. |
| `problem` | 7.176 | Đề bài: đếm số mảng con LIÊN TIẾP có tổng đúng bằng k (có thể có số âm) | Đề bài: đếm xem có bao nhiêu mảng con liên tiếp có tổng đúng bằng kê. Mảng có thể có số âm. |
| `example` | 9.024 | nums = [1, 1, 1], k = 2 → 2: [1, 1] ở vị trí 0–1 và 1–2 | Ví dụ: một, một, một, với kê bằng hai. Có hai mảng con: vị trí không tới một, và một tới hai. |
| `brute` | 7.824 | Cách 1: thử mọi mảng con — n(n+1)/2 mảng → O(n²) | Cách thứ nhất: thử mọi mảng con. Có khoảng n bình phương chia hai mảng con, nên tốn ô en bình phương. |
| `window` | 9.696 | Sliding window? KHÔNG được — có số âm thì tổng lúc tăng lúc giảm, không biết nên nới hay thu | Dùng cửa sổ trượt được không? Không. Khi có số âm, tổng lúc tăng lúc giảm, ta không biết nên nới rộng hay thu hẹp cửa sổ. |
| `prefix_idea` | 9.84 | Tổng tiền tố P: tổng(i … j) = P[j + 1] − P[i] · ví dụ [2, 1] = 4 − 1 = 3 | Dùng tổng tiền tố pê. Tổng của một đoạn bằng hiệu của hai tổng tiền tố. Ví dụ đoạn hai, một bằng bốn trừ một, là ba. |
| `prefix_eq` | 10.752 | Tổng đoạn = k ⇔ P[i] = P − k → đếm xem P − k đã xuất hiện bao nhiêu lần: HashMap | Đoạn có tổng bằng kê, khi và chỉ khi có một tổng tiền tố trước đó bằng pê trừ kê. Vậy chỉ cần đếm xem pê trừ kê đã xuất hiện bao nhiêu lần, bằng một hát mép. |
| `init` | 9.24 | Khởi tạo {0: 1} = tổng rỗng trước phần tử đầu → đếm được mảng con bắt đầu từ vị trí 0 | Khởi tạo bảng với tổng không, xuất hiện một lần. Đó là tổng rỗng trước phần tử đầu tiên, để đếm được các mảng con bắt đầu từ vị trí không. |
| `s0` | 7.608 | num = 1: P = 1, cần P − k = −2 → chưa gặp, +0 · ghi P = 1 | Số một: pê bằng một. Cần tìm trừ hai, chưa gặp. Ghi pê bằng một vào bảng. |
| `s1` | 8.352 | num = 2: P = 3, cần 0 → map[0] = 1 → +1: [1, 2] | Số hai: pê bằng ba. Cần tìm không, có một lần. Cộng một, đó là đoạn một, hai. |
| `s2` | 8.04 | num = 1: P = 4, cần 1 → map[1] = 1 → +1: [2, 1] | Số một: pê bằng bốn. Cần tìm một, có một lần. Cộng một, đoạn hai, một. |
| `s3` | 10.632 | num = −1: P = 3, cần 0 → +1: [1, 2, 1, −1] · map[3] thành 2 | Số trừ một: pê quay về ba. Cần tìm không, cộng một, đoạn một, hai, một, trừ một. Tổng ba giờ đã gặp hai lần. |
| `s4` | 5.112 | num = 2: P = 5, cần 2 → chưa gặp, +0 | Số hai: pê bằng năm. Cần tìm hai, chưa gặp. |
| `s5` | 8.928 | num = 1: P = 6, cần 3 → map[3] = 2 → +2: [1, −1, 2, 1] và [2, 1] · result = 5 | Số một: pê bằng sáu. Cần tìm ba, đã gặp hai lần, nên cộng hai cùng lúc. Tổng cộng là năm. |
| `py_intro` | 2.328 | Code Python — leetcode-38-bai/lc560-subarray-sum-equals-k.py | Giờ xem code Pai thon. |
| `py_init` | 6.552 | prefix_count[0] = 1 — thiếu dòng này, ví dụ vừa rồi ra 3 thay vì 5 | Dòng khởi tạo tổng không bằng một. Thiếu dòng này, ví dụ vừa rồi ra ba thay vì năm. |
| `py_order` | 10.008 | Thứ tự: tra map[P − k] TRƯỚC, ghi P SAU. Ngược lại: [1, −1, 0], k = 0 → 6 (đúng là 3) | Thứ tự rất quan trọng: tra bảng trước, rồi mới ghi pê. Làm ngược lại, với kê bằng không, sẽ đếm cả đoạn rỗng: ra sáu thay vì ba. |
| `py_default` | 11.64 | Bẫy Python: đọc defaultdict bằng [x] sẽ TẠO key x = 0 → map có key rác −2: 0, 2: 0 · dùng .get(x, 0) nếu muốn gọn | Một điểm tinh tế của Pai thon: đọc đi phôn đích bằng ngoặc vuông sẽ tự tạo khóa mới bằng không. Kết quả vẫn đúng, nhưng bảng bị phình. Dùng hàm ghét nếu muốn gọn. |
| `java_intro` | 3.72 | Java 21 — groupc/SubarraySumEqualsK.java: cùng thuật toán, HashMap<Integer, Integer> | Bản gia va dùng cùng thuật toán, với một hát mép. |
| `java_get` | 6.648 | getOrDefault(P − k, 0): chỉ đọc, KHÔNG tạo key mới — map chỉ có 6 key thật | Hàm ghét o đi phôn chỉ đọc, không tạo khóa mới. Bảng chỉ chứa đúng sáu tổng đã gặp. |
| `java_merge` | 7.32 | merge(P, 1, Integer::sum): chưa có → 1, có rồi → cộng thêm 1 — một dòng thay cho if/else | Hàm mơ giơ: chưa có khóa thì đặt bằng một, có rồi thì cộng thêm một. Một dòng thay cho cả if else. |
| `complexity` | 8.52 | Time O(n) — duyệt 1 lần, mỗi bước O(1) · Space O(n) — tối đa n + 1 tổng tiền tố | Độ phức tạp: thời gian ô en, duyệt một lần. Bộ nhớ ô en, cho tối đa n cộng một tổng tiền tố. |
| `tradeoff` | 12.024 | Brute force: O(n²) · Sliding window: O(n) nhưng chỉ đúng khi mọi số > 0 · Prefix + HashMap: O(n), mọi trường hợp | Tóm lại: thử mọi đoạn thì ô en bình phương. Cửa sổ trượt thì ô en, nhưng chỉ đúng khi mọi số dương. Tổng tiền tố cộng hát mép thì ô en, đúng trong mọi trường hợp. |
| `outro` | 4.08 | Tiếp theo: Bài 34 · Intersection of Two Arrays II (#350) | Bài tiếp theo: in tơ séc sần ốp tu ơ rây tu. |

Tổng lời đọc: 191.5s
