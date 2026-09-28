"""Kịch bản bài 38 — mỗi đoạn: (phụ đề hiển thị, câu đọc phiên âm).

Đã kiểm chứng bằng code thật (Python + Java): put(1,1) put(2,2) put(1001,7) → bucket 1 = [(1,1), (1001,7)]
· get(1001) = 7, get(3) = −1 · put(2,1) → bucket 2 = [(2,1)] · remove(1) → bucket 1 = [(1001,7)], get(1) = −1
· 20.000 thao tác ngẫu nhiên khớp dict · Java Math.floorMod(−1, 1000) = 999, −1 % 1000 = −1
· java.util.HashMap (JDK 25, đọc bằng reflection): capacity 16, load factor 0.75, TREEIFY_THRESHOLD 8,
MIN_TREEIFY_CAPACITY 64; đo thực tế: resize ở phần tử thứ 13 (> 12),
bucket thành TreeNode ở phần tử thứ 9 khi bảng 64, bảng 16 thì resize 16→32→64 rồi mới treeify. Ràng buộc đề (LeetCode 706): 0 ≤ key, value ≤ 10⁶, tối đa 10⁴ lời gọi.

TEXT=phienam|tienganh khi build: SEGMENTS[key][1] phiên âm, TTS_EN[key] giữ từ tiếng Anh.
"""

LESSON = "Bài 38 — Design HashMap"

SEGMENTS = {
    "intro": (
        "Bài 38 · Design HashMap (LeetCode #706) — bài cuối nhóm C",
        "Bài ba mươi tám. Đi zai hát mép, lít cốt số bảy trăm linh sáu. Bài cuối của nhóm xê.",
    ),
    "problem": (
        "Đề bài: tự cài HashMap — put(key, value), get(key) (không có → −1), remove(key) · không dùng dict / HashMap có sẵn",
        "Đề bài: tự cài một hát mép với ba hàm: pút, ghét, và ri mu. Ghét trả về trừ một nếu không có. Không được dùng bảng băm có sẵn.",
    ),
    "naive": (
        "Cách ngây thơ: đề cho 0 ≤ key ≤ 10⁶ → mảng 10⁶ + 1 ô, key là chỉ số → O(1) nhưng tốn bộ nhớ cho cả triệu ô",
        "Cách đơn giản nhất: đề cho khóa từ không tới một triệu, nên có thể tạo một mảng một triệu lẻ một ô, dùng khóa làm chỉ số. Chạy ô một, nhưng tốn bộ nhớ cho cả triệu ô, trong khi chỉ có tối đa mười nghìn lời gọi.",
    ),
    "idea": (
        "Ý tưởng: 1000 bucket · hash(key) = key % 1000 quyết định key vào bucket nào",
        "Ý tưởng: chỉ dùng một nghìn cái xô, gọi là bắc kịt. Hàm băm lấy khóa chia lấy dư cho một nghìn, để biết khóa thuộc xô nào.",
    ),
    "chain": (
        "Collision: nhiều key cùng bucket → mỗi bucket là 1 list các cặp (key, value) — separate chaining",
        "Nếu nhiều khóa rơi vào cùng một xô, gọi là va chạm, thì mỗi xô là một danh sách các cặp khóa và giá trị. Kỹ thuật này gọi là xích riêng.",
    ),
    "w_put": (
        "put(1, 1) → 1 % 1000 = 1 → bucket 1 · put(2, 2) → bucket 2",
        "Pút một, một: một chia lấy dư cho một nghìn là một, vào xô số một. Pút hai, hai: vào xô số hai.",
    ),
    "w_coll": (
        "put(1001, 7) → 1001 % 1000 = 1 → VA CHẠM với key 1 → nối thêm vào list của bucket 1",
        "Pút một nghìn lẻ một, bảy: chia lấy dư cũng ra một. Va chạm với khóa một, nên nối thêm vào cuối danh sách của xô số một.",
    ),
    "w_get": (
        "get(1001) → bucket 1 → duyệt: key 1 ≠ 1001, key 1001 = 1001 → trả 7",
        "Ghét một nghìn lẻ một: vào xô số một, duyệt từng cặp. Khóa một không khớp, khóa một nghìn lẻ một khớp, trả về bảy.",
    ),
    "w_miss": (
        "get(3) → bucket 3 rỗng → trả −1",
        "Ghét ba: xô số ba rỗng, trả về trừ một.",
    ),
    "w_update": (
        "put(2, 1) → key 2 ĐÃ có → cập nhật tại chỗ, không thêm cặp mới",
        "Pút hai, một: khóa hai đã có, nên cập nhật giá trị tại chỗ, không thêm cặp mới.",
    ),
    "w_remove": (
        "remove(1) → xoá cặp (1, 1) khỏi bucket 1 · get(1) = −1, nhưng get(1001) vẫn = 7",
        "Ri mu một: xóa cặp khóa một khỏi xô số một. Giờ ghét một trả về trừ một, nhưng khóa một nghìn lẻ một vẫn còn.",
    ),
    "worst": (
        "Trường hợp xấu: key 5, 1005, 2005, … cùng vào bucket 5 → list dài n → mỗi thao tác O(n)",
        "Trường hợp xấu nhất: các khóa năm, một nghìn lẻ năm, hai nghìn lẻ năm, đều vào xô số năm. Danh sách dài ra, mỗi thao tác tốn ô en.",
    ),
    "py_intro": (
        "Code Python — leetcode-38-bai/lc706-design-hashmap.py",
        "Giờ xem code Pai thon.",
    ),
    "py_hash": (
        "__init__: 1000 list rỗng · _hash: key % self.size",
        "Hàm khởi tạo tạo một nghìn danh sách rỗng. Hàm băm chỉ là chia lấy dư.",
    ),
    "py_put": (
        "put: duyệt bucket — gặp key thì thay tuple rồi return · hết vòng mới append",
        "Hàm pút duyệt xô. Gặp khóa thì thay cặp mới rồi thoát. Duyệt hết mà không thấy mới nối thêm vào cuối.",
    ),
    "py_remove": (
        "remove: bucket.pop(i) rồi return — key không có thì không làm gì",
        "Hàm ri mu tìm thấy thì bóp vị trí đó rồi thoát. Khóa không có thì không làm gì.",
    ),
    "java_intro": (
        "Java 21 — groupc/MyHashMap.java: List<List<Entry>> · Entry là record bất biến → bucket.set(i, new Entry(...))",
        "Bản gia va: một danh sách các danh sách en try. En try là ri cọt, không sửa được, nên cập nhật bằng cách thay một en try mới.",
    ),
    "java_floor": (
        "Math.floorMod(key, 1000) thay cho %: key âm vẫn ra 0…999 (floorMod(−1, 1000) = 999, còn −1 % 1000 = −1) · đề cho key ≥ 0 nên đây là phòng thủ",
        "Gia va dùng flo mót thay cho phép chia lấy dư thường. Với khóa âm, phép thường ra số âm, ví dụ trừ một, làm lỗi chỉ số. Flo mót ra chín trăm chín mươi chín. Đề cho khóa không âm, nên đây là phòng thủ, giống bẫy ở bài ba mươi bảy.",
    ),
    "java_remove": (
        "remove: removeIf(entry -> entry.key() == key) — gọn, nhưng luôn duyệt hết bucket",
        "Hàm ri mu dùng ri mu íp, gọn một dòng. Đổi lại, nó luôn duyệt hết cả xô.",
    ),
    "real": (
        "HashMap thật của Java: 16 bucket, size > 0.75 × capacity (vd > 12) → gấp đôi + rehash · 1 bucket vượt 8 phần tử → cây đỏ-đen (nếu bảng < 64 bucket thì gấp đôi bảng trước)",
        "Hát mép thật của gia va làm thêm hai việc. Bắt đầu mười sáu xô, khi số phần tử vượt bảy mươi lăm phần trăm số xô, tức là quá mười hai, thì gấp đôi số xô và băm lại. Và khi một xô dài quá tám phần tử, thì đổi danh sách thành cây đỏ đen. Nếu bảng còn dưới sáu mươi tư xô, gia va gấp đôi bảng trước.",
    ),
    "complexity": (
        "Time O(1) trung bình, O(n) xấu nhất (mọi key cùng bucket) · Space O(1000 + n)",
        "Độ phức tạp: thời gian trung bình ô một, xấu nhất ô en khi mọi khóa cùng một xô. Bộ nhớ là một nghìn xô cộng n cặp.",
    ),
    "outro": (
        "Hết nhóm C — 10 bài HashSet / Dictionary · bài 29 → 38",
        "Vậy là xong nhóm xê, mười bài về hát sét và từ điển. Cảm ơn bạn đã theo dõi.",
    ),
}

TTS_EN = {
    "intro": "Bài ba mươi tám. Design HashMap, LeetCode số 706. Bài cuối của nhóm C.",
    "problem": "Đề bài: tự cài một HashMap với ba hàm: put, get, và remove. Get trả về trừ một nếu không có. Không được dùng dict hay HashMap có sẵn.",
    "naive": "Cách đơn giản nhất: đề cho key từ không tới một triệu, nên có thể tạo một array một triệu lẻ một ô, dùng key làm index. Chạy O một, nhưng tốn bộ nhớ cho cả triệu ô, trong khi chỉ có tối đa mười nghìn lời gọi.",
    "idea": "Ý tưởng: chỉ dùng một nghìn bucket. Hash function lấy key modulo một nghìn, để biết key thuộc bucket nào.",
    "chain": "Nếu nhiều key rơi vào cùng một bucket, gọi là collision, thì mỗi bucket là một list các cặp key value. Kỹ thuật này gọi là separate chaining.",
    "w_put": "Put một, một: một modulo một nghìn là một, vào bucket một. Put hai, hai: vào bucket hai.",
    "w_coll": "Put một nghìn lẻ một, bảy: modulo cũng ra một. Collision với key một, nên append vào list của bucket một.",
    "w_get": "Get một nghìn lẻ một: vào bucket một, duyệt từng cặp. Key một không khớp, key một nghìn lẻ một khớp, trả về bảy.",
    "w_miss": "Get ba: bucket ba rỗng, trả về trừ một.",
    "w_update": "Put hai, một: key hai đã có, nên cập nhật value tại chỗ, không thêm cặp mới.",
    "w_remove": "Remove một: xóa cặp key một khỏi bucket một. Giờ get một trả về trừ một, nhưng key một nghìn lẻ một vẫn còn.",
    "worst": "Trường hợp xấu nhất: các key năm, một nghìn lẻ năm, hai nghìn lẻ năm, đều vào bucket năm. List dài ra, mỗi thao tác tốn O n.",
    "py_intro": "Giờ xem code Python.",
    "py_hash": "Hàm init tạo một nghìn list rỗng. Hàm hash chỉ là key modulo size.",
    "py_put": "Hàm put duyệt bucket. Gặp key thì thay tuple mới rồi return. Duyệt hết mà không thấy mới append.",
    "py_remove": "Hàm remove tìm thấy thì pop vị trí đó rồi return. Key không có thì không làm gì.",
    "java_intro": "Bản Java: một List of List of Entry. Entry là record, immutable, nên cập nhật bằng bucket set với một Entry mới.",
    "java_floor": "Java dùng Math floorMod thay cho toán tử phần trăm. Với key âm, phép thường ra số âm, ví dụ trừ một, làm lỗi index. floorMod ra chín trăm chín mươi chín. Đề cho key không âm, nên đây là phòng thủ, giống bẫy ở bài 37.",
    "java_remove": "Hàm remove dùng removeIf, gọn một dòng. Đổi lại, nó luôn duyệt hết cả bucket.",
    "real": "HashMap thật của Java làm thêm hai việc. Bắt đầu mười sáu bucket, khi size vượt load factor không phẩy bảy lăm nhân capacity, tức là quá mười hai, thì gấp đôi và rehash. Và khi một bucket dài quá tám phần tử, thì đổi list thành red black tree. Nếu table còn dưới sáu mươi tư bucket, Java resize trước.",
    "complexity": "Độ phức tạp: thời gian trung bình O một, xấu nhất O n khi mọi key cùng một bucket. Bộ nhớ O một nghìn cộng n.",
    "outro": "Vậy là xong nhóm C, mười bài về HashSet và Dictionary. Cảm ơn bạn đã theo dõi.",
}
