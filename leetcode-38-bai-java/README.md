# 38 bài LeetCode phỏng vấn - Java 21

Bản Java 21 của 38 bài trong [leetcode-38-bai-phong-van-vietnam.md](../leetcode-38-bai-phong-van-vietnam.md) (bản Python 3 ở [leetcode-38-bai/](../leetcode-38-bai/)). Ưu tiên clean code, SOLID, và tránh các issue phổ biến của SonarQube cho Java.

## Cấu trúc project (Maven)

```
leetcode-38-bai-java/
├── pom.xml                              Java 21 (maven.compiler.release=21), JUnit 5
└── src/
    ├── main/java/com/motives/leetcode/
    │   ├── common/     TreeNode, ListNode - model dung chung
    │   ├── groupa/     18 bai nen tang
    │   ├── groupb/     10 bai bo sung
    │   └── groupc/     10 bai HashSet / Dictionary
    └── test/java/com/motives/leetcode/
        ├── support/    TreeNodes, ListNodes - helper dung cay/list cho test
        ├── groupa/      18 test class
        ├── groupb/      10 test class
        └── groupc/      10 test class
```

## Build & chạy test

```bash
mvn test        # compile + chay toan bo 48 test case (38 bai, mot so bai co 2-3 case)
mvn compile      # chi compile main source
```

Đã verify trong môi trường phát triển:
- `mvn test` → **48/48 test pass**, 0 failure, 0 error.
- `javac -Xlint:all` trên toàn bộ `src/main` → **0 warning**.
- Không có SonarQube server để chạy `mvn sonar:sonar` thật, nhưng code được viết và tự rà soát theo các rule phổ biến nhất của "Sonar way" Java profile (chi tiết bên dưới).

## Quyết định thiết kế (clean code / SOLID / tránh Sonar issue)

- **`TreeNode` là Java record (immutable).** Cả 6 bài liên quan tới cây (Level Order, Serialize/Deserialize, Build Tree, LCA, Validate BST) chỉ đọc cây có sẵn hoặc build bottom-up (con dựng trước, cha dựng sau), nên không bài nào cần mutate `left`/`right` sau khi tạo node. Dùng record vừa idiomatic Java 21, vừa tự động tránh lỗi Sonar **S1104** (field không nên public) mà không cần getter/setter thủ công.
- **`ListNode` là class thường, field `private` + getter/setter.** Khác với TreeNode, bài Remove Nth Node From End of List bắt buộc phải relink `next` in-place (đặc trưng của thuật toán O(1) space trên linked list), nên không thể dùng record immutable ở đây — nhưng field vẫn được đóng gói đúng chuẩn, không public.
- **Dùng `Deque<Character>` (`ArrayDeque`) thay `java.util.Stack`** trong Valid Parentheses. `Stack` kế thừa `Vector`, là class legacy/synchronized và bị Sonar flag ở rule **S1149**.
- **Không raw type, không `System.out`/`printStackTrace` trong main code**, không magic string lặp lại (đưa thành `private static final` constant), không method nào vượt quá 4-5 tham số (dưới ngưỡng mặc định 7 của rule **S107**).
- **LRU Cache và Design HashMap tự cài đặt cấu trúc dữ liệu** (doubly linked list thủ công, separate chaining thủ công) thay vì "ăn gian" bằng `LinkedHashMap`/`HashMap` có sẵn của Java — đúng tinh thần của một bài phỏng vấn thiết kế hệ thống dữ liệu.
- **SOLID áp dụng ở mức hợp lý, không ép abstraction thừa:** mỗi class chỉ giải đúng 1 bài (Single Responsibility). Các bài "design" (LruCache, MyHashMap, BinaryTreeCodec) tách riêng class node/entry nội bộ (`private static final class`/`record`), đóng gói kỹ. Không tạo một `interface Solution<I, O>` chung cho cả 38 bài vì chữ ký phương thức khác nhau quá nhiều (int[], List<List<Integer>>, boolean, TreeNode...) — ép dùng interface chung ở đây sẽ là over-engineering, không phải SOLID thật.
- **`BuildTreeFromPreorderInorder` dùng nested `TreeBuilder`** để gói trạng thái đệ quy (con trỏ preorder + map tra inorder) thay vì truyền 5 tham số qua từng lời gọi đệ quy — giảm số tham số và cognitive complexity.
- **`ValidateBinarySearchTree` dùng `long` cho biên trên/dưới** (thay vì `int`) để tránh lỗi overflow kinh điển khi giá trị node chạm `Integer.MIN_VALUE`/`MAX_VALUE` — một lỗi thật rất dễ gặp nếu chỉ dịch máy móc từ bản Python (Python không có giới hạn kiểu int).

## Test

Mỗi bài có 1 file test JUnit 5 riêng (`<TenClass>Test.java`), dùng lại đúng input/output đã verify ở bản Python/markdown. Hai helper test-only (`TreeNodes.fromLevelOrder(...)`, `ListNodes.of(...)`) giúp dựng cây/list ngay từ mảng kiểu LeetCode (ví dụ `fromLevelOrder(3, 9, 20, null, null, 15, 7)`), giữ test ngắn và dễ đọc. Các helper này chỉ nằm ở `src/test` — không lẫn vào main code.

## Bảng tra cứu 38 bài

| Nhóm | LeetCode | Bài toán | Java class |
|---|---|---|---|
| A | #1 | Two Sum | `groupa.TwoSum` |
| A | #56 | Merge Intervals | `groupa.MergeIntervals` |
| A | #146 | LRU Cache | `groupa.LruCache` |
| A | #102 | Binary Tree Level Order Traversal | `groupa.BinaryTreeLevelOrderTraversal` |
| A | #20 | Valid Parentheses | `groupa.ValidParentheses` |
| A | #200 | Number of Islands | `groupa.NumberOfIslands` |
| A | #3 | Longest Substring Without Repeating Characters | `groupa.LongestSubstringWithoutRepeatingCharacters` |
| A | #121 | Best Time to Buy and Sell Stock | `groupa.BestTimeToBuySellStock` |
| A | #238 | Product of Array Except Self | `groupa.ProductOfArrayExceptSelf` |
| A | #297 | Serialize and Deserialize Binary Tree | `groupa.BinaryTreeCodec` |
| A | #49 | Group Anagrams | `groupa.GroupAnagrams` |
| A | #53 | Maximum Subarray | `groupa.MaximumSubarray` |
| A | #5 | Longest Palindromic Substring | `groupa.LongestPalindromicSubstring` |
| A | #253 | Meeting Rooms II | `groupa.MeetingRoomsII` |
| A | #19 | Remove Nth Node From End of List | `groupa.RemoveNthNodeFromEndOfList` |
| A | #105 | Construct Binary Tree from Preorder and Inorder Traversal | `groupa.BuildTreeFromPreorderInorder` |
| A | #215 | Kth Largest Element in an Array | `groupa.KthLargestElement` |
| A | #33 | Search in Rotated Sorted Array | `groupa.SearchInRotatedSortedArray` |
| B | #15 | 3Sum | `groupb.ThreeSum` |
| B | #42 | Trapping Rain Water | `groupb.TrappingRainWater` |
| B | #207 | Course Schedule | `groupb.CourseSchedule` |
| B | #139 | Word Break | `groupb.WordBreak` |
| B | #322 | Coin Change | `groupb.CoinChange` |
| B | #76 | Minimum Window Substring | `groupb.MinimumWindowSubstring` |
| B | #347 | Top K Frequent Elements | `groupb.TopKFrequentElements` |
| B | #236 | Lowest Common Ancestor of a Binary Tree | `groupb.LowestCommonAncestor` |
| B | #79 | Word Search | `groupb.WordSearch` |
| B | #98 | Validate Binary Search Tree | `groupb.ValidateBinarySearchTree` |
| C | #217 | Contains Duplicate | `groupc.ContainsDuplicate` |
| C | #242 | Valid Anagram | `groupc.ValidAnagram` |
| C | #205 | Isomorphic Strings | `groupc.IsomorphicStrings` |
| C | #128 | Longest Consecutive Sequence | `groupc.LongestConsecutiveSequence` |
| C | #560 | Subarray Sum Equals K | `groupc.SubarraySumEqualsK` |
| C | #350 | Intersection of Two Arrays II | `groupc.IntersectionOfTwoArrays` |
| C | #202 | Happy Number | `groupc.HappyNumber` |
| C | #454 | 4Sum II | `groupc.FourSumII` |
| C | #523 | Continuous Subarray Sum | `groupc.ContinuousSubarraySum` |
| C | #706 | Design HashMap | `groupc.MyHashMap` |
