# 38 bài LeetCode hay được dùng để phỏng vấn tại Việt Nam

Tài liệu tổng hợp 38 bài toán LeetCode phổ biến trong các vòng phỏng vấn kỹ thuật tại Việt Nam, chia thành 3 nhóm:

- **Nhóm A (18 bài):** Nhóm bài nền tảng, tần suất xuất hiện cao nhất, gần như "must-know".
- **Nhóm B (10 bài):** Nhóm bài bổ sung, phủ thêm các pattern còn thiếu (Backtracking, Graph, Sliding Window nâng cao...).
- **Nhóm C (10 bài):** Nhóm bài tập trung riêng vào kỹ thuật **HashSet / Dictionary (HashMap)**.

Mỗi bài gồm: đề bài tóm tắt, **hướng giải quyết bằng lời**, lời giải **Python 3** và **Java 21** chi tiết, và độ phức tạp.

Source code đầy đủ (có test) nằm ở [leetcode-38-bai/](leetcode-38-bai/) (Python) và [leetcode-38-bai-java/](leetcode-38-bai-java/) (Java, Maven). Các lời giải Java cho bài về cây/linked list dùng chung 2 model đặt tại `com.motives.leetcode.common`, thay vì định nghĩa lại `TreeNode`/`ListNode` trong từng file như bản Python:

```java
// com.motives.leetcode.common.TreeNode — record bất biến (immutable), dùng cho mọi bài về cây
public record TreeNode(int val, TreeNode left, TreeNode right) {
    public TreeNode(int val) {
        this(val, null, null);
    }
}
```

```java
// com.motives.leetcode.common.ListNode — class thường, next có thể mutate (Remove Nth Node From End of List cần relink in-place)
public final class ListNode {
    private int val;
    private ListNode next;

    public ListNode(int val) { this.val = val; }
    public ListNode(int val, ListNode next) { this.val = val; this.next = next; }

    public int getVal() { return val; }
    public void setVal(int val) { this.val = val; }
    public ListNode getNext() { return next; }
    public void setNext(ListNode next) { this.next = next; }
}
```

<a id="muc-luc"></a>

## Mục lục tổng quan

| Nhóm | # | Bài toán | LeetCode | Độ khó | Pattern |
|---|---|----------|----------|--------|---------|
| A | 1 | [Two Sum](#bai-1) | #1 | Easy | HashMap |
| A | 2 | [Merge Intervals](#bai-2) | #56 | Medium | Sorting |
| A | 3 | [LRU Cache](#bai-3) | #146 | Medium | HashMap + Doubly Linked List |
| A | 4 | [Binary Tree Level Order Traversal](#bai-4) | #102 | Medium | BFS |
| A | 5 | [Valid Parentheses](#bai-5) | #20 | Easy | Stack |
| A | 6 | [Number of Islands](#bai-6) | #200 | Medium | DFS/BFS Grid |
| A | 7 | [Longest Substring Without Repeating Characters](#bai-7) | #3 | Medium | Sliding Window |
| A | 8 | [Best Time to Buy and Sell Stock](#bai-8) | #121 | Easy | Greedy / DP |
| A | 9 | [Product of Array Except Self](#bai-9) | #238 | Medium | Prefix/Suffix Product |
| A | 10 | [Serialize and Deserialize Binary Tree](#bai-10) | #297 | Hard | DFS Preorder |
| A | 11 | [Group Anagrams](#bai-11) | #49 | Medium | HashMap |
| A | 12 | [Maximum Subarray](#bai-12) | #53 | Medium | Kadane's Algorithm |
| A | 13 | [Longest Palindromic Substring](#bai-13) | #5 | Medium | Expand Around Center |
| A | 14 | [Meeting Rooms II](#bai-14) | #253 | Medium | Heap / Sorting |
| A | 15 | [Remove Nth Node From End of List](#bai-15) | #19 | Medium | Two Pointers (Linked List) |
| A | 16 | [Construct Binary Tree from Preorder and Inorder Traversal](#bai-16) | #105 | Medium | HashMap + Recursion |
| A | 17 | [Kth Largest Element in an Array](#bai-17) | #215 | Medium | Heap |
| A | 18 | [Search in Rotated Sorted Array](#bai-18) | #33 | Medium | Binary Search |
| B | 19 | [3Sum](#bai-19) | #15 | Medium | Two Pointers + Sorting |
| B | 20 | [Trapping Rain Water](#bai-20) | #42 | Hard | Two Pointers |
| B | 21 | [Course Schedule](#bai-21) | #207 | Medium | Topological Sort |
| B | 22 | [Word Break](#bai-22) | #139 | Medium | DP on String |
| B | 23 | [Coin Change](#bai-23) | #322 | Medium | DP (Unbounded Knapsack) |
| B | 24 | [Minimum Window Substring](#bai-24) | #76 | Hard | Sliding Window |
| B | 25 | [Top K Frequent Elements](#bai-25) | #347 | Medium | HashMap + Heap |
| B | 26 | [Lowest Common Ancestor of a Binary Tree](#bai-26) | #236 | Medium | Tree DFS |
| B | 27 | [Word Search](#bai-27) | #79 | Medium | Backtracking |
| B | 28 | [Validate Binary Search Tree](#bai-28) | #98 | Medium | Tree DFS + Range |
| C | 29 | [Contains Duplicate](#bai-29) | #217 | Easy | HashSet |
| C | 30 | [Valid Anagram](#bai-30) | #242 | Easy | HashMap Counting |
| C | 31 | [Isomorphic Strings](#bai-31) | #205 | Easy | HashMap Bijection |
| C | 32 | [Longest Consecutive Sequence](#bai-32) | #128 | Medium | HashSet |
| C | 33 | [Subarray Sum Equals K](#bai-33) | #560 | Medium | Prefix Sum + HashMap |
| C | 34 | [Intersection of Two Arrays II](#bai-34) | #350 | Easy | HashMap Counting |
| C | 35 | [Happy Number](#bai-35) | #202 | Easy | HashSet Cycle Detection |
| C | 36 | [4Sum II](#bai-36) | #454 | Medium | HashMap |
| C | 37 | [Continuous Subarray Sum](#bai-37) | #523 | Medium | Prefix Sum % k + HashMap |
| C | 38 | [Design HashMap](#bai-38) | #706 | Medium | Tự implement HashMap |

---

# NHÓM A — 18 bài nền tảng

<a id="bai-1"></a>

## 1. Two Sum (#1) — Easy

**Đề bài:** Cho mảng `nums` và số `target`, tìm 2 chỉ số sao cho `nums[i] + nums[j] == target`.

**Ví dụ:**
```
Input: nums = [2, 7, 11, 15], target = 9
Output: [0, 1]
Giải thích: nums[0] + nums[1] = 2 + 7 = 9
```

**Hướng giải quyết:** Dùng một hash map để lưu `giá trị -> chỉ số` khi duyệt qua mảng. Với mỗi số `num`, kiểm tra xem `target - num` (số bù) đã tồn tại trong map chưa — nếu có, trả về ngay. Nếu chưa, lưu `num` vào map và tiếp tục. Nhờ hash map, việc tra cứu số bù chỉ tốn O(1), giúp cả bài toán chạy trong một lượt duyệt duy nhất O(n) thay vì lồng hai vòng lặp O(n²).

**Độ phức tạp:** Time O(n), Space O(n).

```python
"""
LeetCode #1 - Two Sum
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def two_sum(self, nums: list[int], target: int) -> list[int]:
        seen = {}  # value -> index
        for i, num in enumerate(nums):
            complement = target - num
            if complement in seen:
                return [seen[complement], i]
            seen[num] = i
        return []


if __name__ == "__main__":
    sol = Solution()
    print(sol.two_sum([2, 7, 11, 15], 9))  # Expect: [0, 1]
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #1 - Two Sum (Nhom A, bai 1).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class TwoSum {

    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> valueToIndex = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            Integer complementIndex = valueToIndex.get(complement);
            if (complementIndex != null) {
                return new int[] {complementIndex, i};
            }
            valueToIndex.put(nums[i], i);
        }
        throw new IllegalArgumentException("No two sum solution exists for the given input");
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-2"></a>

## 2. Merge Intervals (#56) — Medium

**Đề bài:** Cho danh sách các khoảng `[start, end]`, hợp nhất các khoảng bị chồng lấn.

**Ví dụ:**
```
Input: intervals = [[1,3],[2,6],[8,10],[15,18]]
Output: [[1,6],[8,10],[15,18]]
Giải thích: [1,3] va [2,6] chong lan nen gop thanh [1,6]
```

**Hướng giải quyết:** Sắp xếp các khoảng theo `start`. Sau đó duyệt tuần tự: nếu khoảng hiện tại có `start` nhỏ hơn hoặc bằng `end` của khoảng cuối cùng trong kết quả, nghĩa là chúng chồng lấn — mở rộng `end` của khoảng cuối bằng `max` của hai `end`. Nếu không chồng lấn, thêm khoảng hiện tại vào kết quả như một khoảng mới. Việc sort trước giúp ta chỉ cần so sánh với khoảng liền trước, không cần so sánh chéo toàn bộ.

**Độ phức tạp:** Time O(n log n) (do sort), Space O(n).

```python
"""
LeetCode #56 - Merge Intervals
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def merge(self, intervals: list[list[int]]) -> list[list[int]]:
        intervals.sort(key=lambda x: x[0])
        merged = []
        for interval in intervals:
            if merged and interval[0] <= merged[-1][1]:
                merged[-1][1] = max(merged[-1][1], interval[1])
            else:
                merged.append(interval)
        return merged


if __name__ == "__main__":
    sol = Solution()
    print(
        sol.merge([[1, 3], [2, 6], [8, 10], [15, 18]])
    )  # Expect: [[1, 6], [8, 10], [15, 18]]
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Comparator;

/**
 * LeetCode #56 - Merge Intervals (Nhom A, bai 2).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class MergeIntervals {

    public int[][] merge(int[][] intervals) {
        int[][] sorted = intervals.clone();
        Arrays.sort(sorted, Comparator.comparingInt(interval -> interval[0]));

        Deque<int[]> merged = new ArrayDeque<>();
        for (int[] interval : sorted) {
            int[] last = merged.peekLast();
            if (last != null && interval[0] <= last[1]) {
                last[1] = Math.max(last[1], interval[1]);
            } else {
                merged.addLast(interval);
            }
        }
        return merged.toArray(new int[0][]);
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-3"></a>

## 3. LRU Cache (#146) — Medium

**Đề bài:** Thiết kế cache có giới hạn dung lượng, khi đầy thì loại phần tử **ít dùng gần đây nhất (Least Recently Used)**.

**Ví dụ:**
```
cache = LRUCache(2)
cache.put(1, 1)
cache.put(2, 2)
cache.get(1)      -> 1
cache.put(3, 3)   -> loai key 2 (LRU)
cache.get(2)      -> -1 (da bi loai)
cache.put(4, 4)   -> loai key 1 (LRU)
cache.get(1)      -> -1
cache.get(3)      -> 3
cache.get(4)      -> 4
```

**Hướng giải quyết:** Kết hợp **HashMap** (tra cứu O(1)) với **Doubly Linked List** (duy trì thứ tự sử dụng, xóa/chèn O(1)). Trong Python, `collections.OrderedDict` đã tích hợp sẵn cả hai đặc tính này: mỗi lần `get`/`put` một key, ta gọi `move_to_end` để đưa key đó lên "mới nhất". Khi vượt capacity, `popitem(last=False)` sẽ loại bỏ phần tử đầu tiên (cũ nhất). Trong phỏng vấn, nếu được yêu cầu không dùng `OrderedDict`, cần tự implement một Doubly Linked List thủ công + dict lưu con trỏ node.

**Độ phức tạp:** Time O(1) cho `get`/`put`, Space O(capacity).

```python
"""
LeetCode #146 - LRU Cache
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

from collections import OrderedDict


class LRUCache:
    def __init__(self, capacity: int):
        self.capacity = capacity
        self.cache = OrderedDict()

    def get(self, key: int) -> int:
        if key not in self.cache:
            return -1
        self.cache.move_to_end(key)
        return self.cache[key]

    def put(self, key: int, value: int) -> None:
        if key in self.cache:
            self.cache.move_to_end(key)
        self.cache[key] = value
        if len(self.cache) > self.capacity:
            self.cache.popitem(last=False)


if __name__ == "__main__":
    cache = LRUCache(2)
    cache.put(1, 1)
    cache.put(2, 2)
    print(cache.get(1))  # Expect: 1
    cache.put(3, 3)  # evict key 2
    print(cache.get(2))  # Expect: -1
    cache.put(4, 4)  # evict key 1
    print(cache.get(1))  # Expect: -1
    print(cache.get(3))  # Expect: 3
    print(cache.get(4))  # Expect: 4
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #146 - LRU Cache (Nhom A, bai 3).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 *
 * <p>HashMap cho tra cuu O(1) + doubly linked list cho thu tu su dung O(1).
 * Node moi duoc dua vao cuoi (most-recently-used); khi vuot capacity,
 * node dau (least-recently-used) bi loai.
 */
public class LruCache {

    private final int capacity;
    private final Map<Integer, Node> cache;
    private final Node head;
    private final Node tail;

    public LruCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.head = new Node(0, 0);
        this.tail = new Node(0, 0);
        head.next = tail;
        tail.prev = head;
    }

    public int get(int key) {
        Node node = cache.get(key);
        if (node == null) {
            return -1;
        }
        moveToMostRecentlyUsed(node);
        return node.value;
    }

    public void put(int key, int value) {
        Node existing = cache.get(key);
        if (existing != null) {
            existing.value = value;
            moveToMostRecentlyUsed(existing);
            return;
        }

        Node created = new Node(key, value);
        cache.put(key, created);
        addToMostRecentlyUsed(created);

        if (cache.size() > capacity) {
            Node leastRecentlyUsed = head.next;
            removeNode(leastRecentlyUsed);
            cache.remove(leastRecentlyUsed.key);
        }
    }

    private void moveToMostRecentlyUsed(Node node) {
        removeNode(node);
        addToMostRecentlyUsed(node);
    }

    private void addToMostRecentlyUsed(Node node) {
        Node previousTail = tail.prev;
        previousTail.next = node;
        node.prev = previousTail;
        node.next = tail;
        tail.prev = node;
    }

    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private static final class Node {
        private final int key;
        private int value;
        private Node prev;
        private Node next;

        private Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-4"></a>

## 4. Binary Tree Level Order Traversal (#102) — Medium

**Đề bài:** Trả về giá trị các node của cây nhị phân theo từng tầng (level), từ trên xuống.

**Ví dụ:**
```
Input: root = [3,9,20,null,null,15,7]
Output: [[3],[9,20],[15,7]]
```

**Hướng giải quyết:** Dùng BFS với một `queue`. Ở mỗi vòng lặp, ghi nhận `level_size = len(queue)` hiện tại (số node ở tầng đang xét), sau đó pop đúng `level_size` node đó ra, thu thập giá trị và đẩy các con của chúng vào queue cho tầng kế tiếp. Việc "chốt" số lượng node của tầng trước khi xử lý là mẹo quan trọng để tách đúng ranh giới giữa các tầng.

**Độ phức tạp:** Time O(n), Space O(n).

```python
"""
LeetCode #102 - Binary Tree Level Order Traversal
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

from collections import deque


class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right


class Solution:
    def level_order(self, root: TreeNode | None) -> list[list[int]]:
        if not root:
            return []
        result = []
        queue = deque([root])
        while queue:
            level_size = len(queue)
            level = []
            for _ in range(level_size):
                node = queue.popleft()
                level.append(node.val)
                if node.left:
                    queue.append(node.left)
                if node.right:
                    queue.append(node.right)
            result.append(level)
        return result


if __name__ == "__main__":
    root = TreeNode(3, TreeNode(9), TreeNode(20, TreeNode(15), TreeNode(7)))
    sol = Solution()
    print(sol.level_order(root))  # Expect: [[3], [9, 20], [15, 7]]
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

import com.motives.leetcode.common.TreeNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * LeetCode #102 - Binary Tree Level Order Traversal (Nhom A, bai 4).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class BinaryTreeLevelOrderTraversal {

    public List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) {
            return result;
        }

        Deque<TreeNode> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<Integer> level = new ArrayList<>(levelSize);
            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                level.add(node.val());
                if (node.left() != null) {
                    queue.add(node.left());
                }
                if (node.right() != null) {
                    queue.add(node.right());
                }
            }
            result.add(level);
        }
        return result;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-5"></a>

## 5. Valid Parentheses (#20) — Easy

**Đề bài:** Kiểm tra một chuỗi chỉ gồm `(){}[]` có hợp lệ (đóng/mở đúng thứ tự) hay không.

**Ví dụ:**
```
Ví dụ 1:
Input: s = "()[]{}"
Output: true

Ví dụ 2:
Input: s = "(]"
Output: false
```

**Hướng giải quyết:** Dùng **stack**. Khi gặp dấu mở, push vào stack. Khi gặp dấu đóng, so sánh với phần tử trên cùng của stack: nếu khớp cặp thì pop ra, nếu không khớp (hoặc stack trống) thì chuỗi không hợp lệ. Sau khi duyệt hết chuỗi, nếu stack rỗng thì hợp lệ hoàn toàn.

**Độ phức tạp:** Time O(n), Space O(n).

```python
"""
LeetCode #20 - Valid Parentheses
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def is_valid(self, s: str) -> bool:
        stack = []
        pairs = {")": "(", "]": "[", "}": "{"}
        for ch in s:
            if ch in pairs:
                if not stack or stack.pop() != pairs[ch]:
                    return False
            else:
                stack.append(ch)
        return not stack


if __name__ == "__main__":
    sol = Solution()
    print(sol.is_valid("()[]{}"))  # Expect: True
    print(sol.is_valid("(]"))  # Expect: False
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * LeetCode #20 - Valid Parentheses (Nhom A, bai 5).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class ValidParentheses {

    private static final Map<Character, Character> CLOSING_TO_OPENING =
            Map.of(')', '(', ']', '[', '}', '{');

    public boolean isValid(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            Character expectedOpening = CLOSING_TO_OPENING.get(c);
            if (expectedOpening == null) {
                stack.push(c);
            } else if (stack.isEmpty() || !stack.pop().equals(expectedOpening)) {
                return false;
            }
        }
        return stack.isEmpty();
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-6"></a>

## 6. Number of Islands (#200) — Medium

**Đề bài:** Cho lưới 2D gồm `'1'` (đất) và `'0'` (nước), đếm số "hòn đảo" (các cụm đất liền kề theo 4 hướng).

**Ví dụ:**
```
Input: grid = [
  ["1","1","0","0","0"],
  ["1","1","0","0","0"],
  ["0","0","1","0","0"],
  ["0","0","0","1","1"]
]
Output: 3
```

**Hướng giải quyết:** Duyệt qua từng ô của lưới. Khi gặp ô đất (`'1'`) chưa được thăm, tăng số đảo lên 1, sau đó dùng **DFS (hoặc BFS)** để "nhấn chìm" (đánh dấu thành `'0'`) toàn bộ cụm đất liền kề với ô đó — nhờ vậy các ô cùng đảo sẽ không bị đếm lại lần thứ hai.

**Độ phức tạp:** Time O(rows × cols), Space O(rows × cols) (trong trường hợp xấu nhất do độ sâu đệ quy).

```python
"""
LeetCode #200 - Number of Islands
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def num_islands(self, grid: list[list[str]]) -> int:
        if not grid:
            return 0
        rows, cols = len(grid), len(grid[0])
        count = 0

        def dfs(r: int, c: int) -> None:
            if r < 0 or r >= rows or c < 0 or c >= cols or grid[r][c] != "1":
                return
            grid[r][c] = "0"
            dfs(r + 1, c)
            dfs(r - 1, c)
            dfs(r, c + 1)
            dfs(r, c - 1)

        for r in range(rows):
            for c in range(cols):
                if grid[r][c] == "1":
                    count += 1
                    dfs(r, c)
        return count


if __name__ == "__main__":
    grid = [
        ["1", "1", "0", "0", "0"],
        ["1", "1", "0", "0", "0"],
        ["0", "0", "1", "0", "0"],
        ["0", "0", "0", "1", "1"],
    ]
    sol = Solution()
    print(sol.num_islands(grid))  # Expect: 3
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

/**
 * LeetCode #200 - Number of Islands (Nhom A, bai 6).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class NumberOfIslands {

    private static final char LAND = '1';
    private static final char WATER = '0';
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public int numIslands(char[][] grid) {
        int islandCount = 0;
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                if (grid[row][col] == LAND) {
                    islandCount++;
                    sinkIsland(grid, row, col);
                }
            }
        }
        return islandCount;
    }

    private void sinkIsland(char[][] grid, int row, int col) {
        if (!isLand(grid, row, col)) {
            return;
        }
        grid[row][col] = WATER;
        for (int[] direction : DIRECTIONS) {
            sinkIsland(grid, row + direction[0], col + direction[1]);
        }
    }

    private boolean isLand(char[][] grid, int row, int col) {
        return row >= 0 && row < grid.length
                && col >= 0 && col < grid[row].length
                && grid[row][col] == LAND;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-7"></a>

## 7. Longest Substring Without Repeating Characters (#3) — Medium

**Đề bài:** Tìm độ dài chuỗi con dài nhất không có ký tự lặp lại.

**Ví dụ:**
```
Input: s = "abcabcbb"
Output: 3
Giải thích: chuoi con dai nhat khong lap la "abc", do dai 3
```

**Hướng giải quyết:** Dùng **sliding window** với hai con trỏ `left`, `right` và một hash map lưu **vị trí xuất hiện gần nhất** của mỗi ký tự. Khi mở rộng `right` gặp ký tự đã xuất hiện trong cửa sổ hiện tại (`last_index[ch] >= left`), co `left` nhảy tới ngay sau vị trí lặp đó. Luôn cập nhật độ dài lớn nhất sau mỗi bước mở rộng.

**Độ phức tạp:** Time O(n), Space O(min(n, alphabet size)).

```python
"""
LeetCode #3 - Longest Substring Without Repeating Characters
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def length_of_longest_substring(self, s: str) -> int:
        last_index = {}
        left = 0
        best = 0
        for right, ch in enumerate(s):
            if ch in last_index and last_index[ch] >= left:
                left = last_index[ch] + 1
            last_index[ch] = right
            best = max(best, right - left + 1)
        return best


if __name__ == "__main__":
    sol = Solution()
    print(sol.length_of_longest_substring("abcabcbb"))  # Expect: 3
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #3 - Longest Substring Without Repeating Characters (Nhom A, bai 7).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class LongestSubstringWithoutRepeatingCharacters {

    public int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> lastSeenIndex = new HashMap<>();
        int windowStart = 0;
        int longest = 0;

        for (int windowEnd = 0; windowEnd < s.length(); windowEnd++) {
            char current = s.charAt(windowEnd);
            Integer previousIndex = lastSeenIndex.get(current);
            if (previousIndex != null && previousIndex >= windowStart) {
                windowStart = previousIndex + 1;
            }
            lastSeenIndex.put(current, windowEnd);
            longest = Math.max(longest, windowEnd - windowStart + 1);
        }
        return longest;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-8"></a>

## 8. Best Time to Buy and Sell Stock (#121) — Easy

**Đề bài:** Cho mảng giá cổ phiếu theo ngày, tìm lợi nhuận tối đa từ đúng 1 lần mua và 1 lần bán (bán sau mua).

**Ví dụ:**
```
Input: prices = [7,1,5,3,6,4]
Output: 5
Giải thích: mua tai gia 1 (ngay 2), ban tai gia 6 (ngay 5), loi nhuan = 5
```

**Hướng giải quyết:** Duyệt một lượt, luôn ghi nhận **giá thấp nhất đã gặp** (`min_price`) tính đến thời điểm hiện tại — đây chính là điểm mua tốt nhất có thể nếu bán tại ngày hiện tại. Tại mỗi ngày, tính lợi nhuận nếu bán hôm nay (`price - min_price`) và so sánh để cập nhật lợi nhuận lớn nhất.

**Độ phức tạp:** Time O(n), Space O(1).

```python
"""
LeetCode #121 - Best Time to Buy and Sell Stock
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def max_profit(self, prices: list[int]) -> int:
        min_price = float("inf")
        max_profit = 0
        for price in prices:
            min_price = min(min_price, price)
            max_profit = max(max_profit, price - min_price)
        return max_profit


if __name__ == "__main__":
    sol = Solution()
    print(sol.max_profit([7, 1, 5, 3, 6, 4]))  # Expect: 5
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

/**
 * LeetCode #121 - Best Time to Buy and Sell Stock (Nhom A, bai 8).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class BestTimeToBuySellStock {

    public int maxProfit(int[] prices) {
        int minPriceSoFar = Integer.MAX_VALUE;
        int maxProfitSoFar = 0;

        for (int price : prices) {
            minPriceSoFar = Math.min(minPriceSoFar, price);
            maxProfitSoFar = Math.max(maxProfitSoFar, price - minPriceSoFar);
        }
        return maxProfitSoFar;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-9"></a>

## 9. Product of Array Except Self (#238) — Medium

**Đề bài:** Trả về mảng `result` trong đó `result[i]` là tích của tất cả phần tử trừ `nums[i]`, **không dùng phép chia**.

**Ví dụ:**
```
Input: nums = [1,2,3,4]
Output: [24,12,8,6]
```

**Hướng giải quyết:** Tách bài toán thành 2 lượt duyệt: lượt 1 tính **tích tiền tố (prefix product)** — tích của mọi phần tử bên trái `i`; lượt 2 (duyệt ngược) tính **tích hậu tố (suffix product)** — tích mọi phần tử bên phải `i`, đồng thời nhân dồn vào kết quả đã có từ lượt 1. Kết quả tại `i` chính là `prefix[i] * suffix[i]`, hoàn toàn không cần chia.

**Độ phức tạp:** Time O(n), Space O(1) ngoài mảng kết quả.

```python
"""
LeetCode #238 - Product of Array Except Self
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def product_except_self(self, nums: list[int]) -> list[int]:
        n = len(nums)
        result = [1] * n
        prefix = 1
        for i in range(n):
            result[i] = prefix
            prefix *= nums[i]
        suffix = 1
        for i in range(n - 1, -1, -1):
            result[i] *= suffix
            suffix *= nums[i]
        return result


if __name__ == "__main__":
    sol = Solution()
    print(sol.product_except_self([1, 2, 3, 4]))  # Expect: [24, 12, 8, 6]
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

/**
 * LeetCode #238 - Product of Array Except Self (Nhom A, bai 9).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class ProductOfArrayExceptSelf {

    public int[] productExceptSelf(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];

        int prefixProduct = 1;
        for (int i = 0; i < n; i++) {
            result[i] = prefixProduct;
            prefixProduct *= nums[i];
        }

        int suffixProduct = 1;
        for (int i = n - 1; i >= 0; i--) {
            result[i] *= suffixProduct;
            suffixProduct *= nums[i];
        }
        return result;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-10"></a>

## 10. Serialize and Deserialize Binary Tree (#297) — Hard

**Đề bài:** Thiết kế thuật toán chuyển cây nhị phân thành chuỗi (`serialize`) và khôi phục lại chính xác cây từ chuỗi đó (`deserialize`).

**Ví dụ:**
```
Input: root = [1,2,3,null,null,4,5]
serialize(root)      -> "1,2,#,#,3,4,#,#,5,#,#"
deserialize(...)     -> cay giong het root ban dau
```

**Hướng giải quyết:** Dùng **DFS Preorder** (gốc → trái → phải), với node `None` được đánh dấu bằng một ký hiệu đặc biệt (ví dụ `'#'`) để giữ nguyên hình dạng cây. Khi deserialize, dùng một iterator để "ăn" từng token theo đúng thứ tự preorder đã ghi — tại mỗi bước, nếu token là `'#'` thì trả `None`, ngược lại tạo node mới rồi đệ quy xây nhánh trái/phải theo cùng logic.

**Độ phức tạp:** Time O(n), Space O(n).

```python
"""
LeetCode #297 - Serialize and Deserialize Binary Tree
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right


class Codec:
    def serialize(self, root) -> str:
        vals = []

        def dfs(node):
            if not node:
                vals.append("#")
                return
            vals.append(str(node.val))
            dfs(node.left)
            dfs(node.right)

        dfs(root)
        return ",".join(vals)

    def deserialize(self, data: str):
        vals = iter(data.split(","))

        def build():
            val = next(vals)
            if val == "#":
                return None
            node = TreeNode(int(val))
            node.left = build()
            node.right = build()
            return node

        return build()


if __name__ == "__main__":
    root = TreeNode(1, TreeNode(2), TreeNode(3, TreeNode(4), TreeNode(5)))
    codec = Codec()
    data = codec.serialize(root)
    print(data)
    restored = codec.deserialize(data)
    print(codec.serialize(restored) == data)  # Expect: True
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

import com.motives.leetcode.common.TreeNode;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.StringJoiner;

/**
 * LeetCode #297 - Serialize and Deserialize Binary Tree (Nhom A, bai 10).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class BinaryTreeCodec {

    private static final String NULL_MARKER = "#";
    private static final String DELIMITER = ",";

    public String serialize(TreeNode root) {
        StringJoiner joiner = new StringJoiner(DELIMITER);
        appendPreorder(root, joiner);
        return joiner.toString();
    }

    public TreeNode deserialize(String data) {
        Deque<String> tokens = new ArrayDeque<>(Arrays.asList(data.split(DELIMITER)));
        return buildFromPreorder(tokens);
    }

    private void appendPreorder(TreeNode node, StringJoiner joiner) {
        if (node == null) {
            joiner.add(NULL_MARKER);
            return;
        }
        joiner.add(String.valueOf(node.val()));
        appendPreorder(node.left(), joiner);
        appendPreorder(node.right(), joiner);
    }

    private TreeNode buildFromPreorder(Deque<String> tokens) {
        String token = tokens.poll();
        if (token == null || NULL_MARKER.equals(token)) {
            return null;
        }
        TreeNode left = buildFromPreorder(tokens);
        TreeNode right = buildFromPreorder(tokens);
        return new TreeNode(Integer.parseInt(token), left, right);
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-11"></a>

## 11. Group Anagrams (#49) — Medium

**Đề bài:** Nhóm các chuỗi là anagram của nhau (cùng tập ký tự, khác thứ tự) vào từng nhóm.

**Ví dụ:**
```
Input: strs = ["eat","tea","tan","ate","nat","bat"]
Output: [["eat","tea","ate"],["tan","nat"],["bat"]]
```

**Hướng giải quyết:** Với mỗi chuỗi, tạo một **key chuẩn hóa** bằng cách sort các ký tự trong chuỗi đó (`''.join(sorted(word))`) — hai chuỗi là anagram của nhau thì sẽ luôn có key giống nhau. Dùng `defaultdict(list)` để nhóm các chuỗi có cùng key vào chung một danh sách.

**Độ phức tạp:** Time O(n · k log k) với `k` là độ dài chuỗi trung bình, Space O(n · k).

```python
"""
LeetCode #49 - Group Anagrams
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

from collections import defaultdict


class Solution:
    def group_anagrams(self, strs: list[str]) -> list[list[str]]:
        groups = defaultdict(list)
        for s in strs:
            key = "".join(sorted(s))
            groups[key].append(s)
        return list(groups.values())


if __name__ == "__main__":
    sol = Solution()
    print(sol.group_anagrams(["eat", "tea", "tan", "ate", "nat", "bat"]))
    # Expect nhom: ['eat','tea','ate'], ['tan','nat'], ['bat']
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LeetCode #49 - Group Anagrams (Nhom A, bai 11).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class GroupAnagrams {

    public List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> anagramsByKey = new LinkedHashMap<>();
        for (String word : strs) {
            String key = sortedKey(word);
            anagramsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(word);
        }
        return new ArrayList<>(anagramsByKey.values());
    }

    private String sortedKey(String word) {
        char[] letters = word.toCharArray();
        Arrays.sort(letters);
        return new String(letters);
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-12"></a>

## 12. Maximum Subarray (#53) — Medium

**Đề bài:** Tìm tổng lớn nhất của một dãy con liên tiếp trong mảng.

**Ví dụ:**
```
Input: nums = [-2,1,-3,4,-1,2,1,-5,4]
Output: 6
Giải thích: day con [4,-1,2,1] co tong lon nhat = 6
```

**Hướng giải quyết:** Áp dụng **Kadane's Algorithm**: duy trì `current` là tổng lớn nhất của dãy con liên tiếp **kết thúc tại vị trí hiện tại**. Tại mỗi phần tử, quyết định "bắt đầu lại từ đây" hay "nối tiếp dãy trước" bằng `current = max(num, current + num)` — nếu cộng thêm làm tổng nhỏ hơn chính `num`, thì bắt đầu lại tốt hơn. Đồng thời cập nhật `best` là giá trị lớn nhất từng đạt được của `current`.

**Độ phức tạp:** Time O(n), Space O(1).

```python
"""
LeetCode #53 - Maximum Subarray
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def max_sub_array(self, nums: list[int]) -> int:
        best = nums[0]
        current = nums[0]
        for num in nums[1:]:
            current = max(num, current + num)
            best = max(best, current)
        return best


if __name__ == "__main__":
    sol = Solution()
    print(sol.max_sub_array([-2, 1, -3, 4, -1, 2, 1, -5, 4]))  # Expect: 6
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

/**
 * LeetCode #53 - Maximum Subarray (Nhom A, bai 12).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class MaximumSubarray {

    public int maxSubArray(int[] nums) {
        int best = nums[0];
        int currentSum = nums[0];

        for (int i = 1; i < nums.length; i++) {
            currentSum = Math.max(nums[i], currentSum + nums[i]);
            best = Math.max(best, currentSum);
        }
        return best;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-13"></a>

## 13. Longest Palindromic Substring (#5) — Medium

**Đề bài:** Tìm chuỗi con là palindrome (đối xứng) dài nhất trong chuỗi cho trước.

**Ví dụ:**
```
Input: s = "babad"
Output: "bab" (hoac "aba" deu hop le)
```

**Hướng giải quyết:** Dùng kỹ thuật **"expand around center"**: mỗi palindrome đều có một tâm (1 ký tự nếu độ dài lẻ, 2 ký tự nếu độ dài chẵn). Với mỗi vị trí `i` trong chuỗi, thử mở rộng ra hai hướng từ tâm là `(i, i)` (lẻ) và `(i, i+1)` (chẵn), miễn là hai đầu còn bằng nhau. So sánh độ dài palindrome tìm được ở mỗi tâm để giữ lại kết quả dài nhất.

**Độ phức tạp:** Time O(n²), Space O(1).

```python
"""
LeetCode #5 - Longest Palindromic Substring
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def longest_palindrome(self, s: str) -> str:
        if not s:
            return ""
        start, end = 0, 0

        def expand(l: int, r: int):
            while l >= 0 and r < len(s) and s[l] == s[r]:
                l -= 1
                r += 1
            return l + 1, r - 1

        for i in range(len(s)):
            l1, r1 = expand(i, i)
            if r1 - l1 > end - start:
                start, end = l1, r1
            l2, r2 = expand(i, i + 1)
            if r2 - l2 > end - start:
                start, end = l2, r2
        return s[start : end + 1]


if __name__ == "__main__":
    sol = Solution()
    print(sol.longest_palindrome("babad"))  # Expect: "bab" hoac "aba"
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

/**
 * LeetCode #5 - Longest Palindromic Substring (Nhom A, bai 13).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class LongestPalindromicSubstring {

    public String longestPalindrome(String s) {
        if (s.isEmpty()) {
            return "";
        }

        int bestStart = 0;
        int bestEnd = 0;
        for (int center = 0; center < s.length(); center++) {
            int[] oddBounds = expandAroundCenter(s, center, center);
            if (oddBounds[1] - oddBounds[0] > bestEnd - bestStart) {
                bestStart = oddBounds[0];
                bestEnd = oddBounds[1];
            }

            int[] evenBounds = expandAroundCenter(s, center, center + 1);
            if (evenBounds[1] - evenBounds[0] > bestEnd - bestStart) {
                bestStart = evenBounds[0];
                bestEnd = evenBounds[1];
            }
        }
        return s.substring(bestStart, bestEnd + 1);
    }

    private int[] expandAroundCenter(String s, int left, int right) {
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            left--;
            right++;
        }
        return new int[] {left + 1, right - 1};
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-14"></a>

## 14. Meeting Rooms II (#253) — Medium

**Đề bài:** Cho danh sách các cuộc họp `[start, end]`, tìm số phòng họp tối thiểu cần để tổ chức hết mà không trùng giờ.

**Ví dụ:**
```
Input: intervals = [[0,30],[5,10],[15,20]]
Output: 2
Giải thích: can 2 phong vi [0,30] chong lan voi ca [5,10] va [15,20]
```

**Hướng giải quyết:** Sắp xếp các cuộc họp theo `start`. Dùng một **min-heap** lưu thời điểm kết thúc của các cuộc họp đang "chiếm phòng". Với mỗi cuộc họp mới: nếu cuộc họp kết thúc sớm nhất trong heap (`heap[0]`) đã xong trước khi cuộc họp mới bắt đầu, ta **tái sử dụng phòng đó** (thay `end` cũ bằng `end` mới trong heap); nếu chưa, phải mở **thêm phòng mới** (push thêm vào heap). Số phòng tối thiểu chính là kích thước heap lớn nhất từng đạt được — ở cách viết dưới, là kích thước heap cuối cùng.

**Độ phức tạp:** Time O(n log n), Space O(n).

```python
"""
LeetCode #253 - Meeting Rooms II
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

import heapq


class Solution:
    def min_meeting_rooms(self, intervals: list[list[int]]) -> int:
        if not intervals:
            return 0
        intervals.sort(key=lambda x: x[0])
        heap: list[int] = []  # end times của các cuộc họp đang diễn ra
        for start, end in intervals:
            if heap and heap[0] <= start:
                heapq.heapreplace(heap, end)
            else:
                heapq.heappush(heap, end)
        return len(heap)


if __name__ == "__main__":
    sol = Solution()
    print(sol.min_meeting_rooms([[0, 30], [5, 10], [15, 20]]))  # Expect: 2
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * LeetCode #253 - Meeting Rooms II (Nhom A, bai 14).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class MeetingRoomsII {

    public int minMeetingRooms(int[][] intervals) {
        if (intervals.length == 0) {
            return 0;
        }

        int[][] sorted = intervals.clone();
        Arrays.sort(sorted, Comparator.comparingInt(interval -> interval[0]));

        PriorityQueue<Integer> endTimesInUse = new PriorityQueue<>();
        for (int[] meeting : sorted) {
            int start = meeting[0];
            int end = meeting[1];
            if (!endTimesInUse.isEmpty() && endTimesInUse.peek() <= start) {
                endTimesInUse.poll();
            }
            endTimesInUse.add(end);
        }
        return endTimesInUse.size();
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-15"></a>

## 15. Remove Nth Node From End of List (#19) — Medium

**Đề bài:** Xóa node thứ `n` tính từ cuối danh sách liên kết (linked list).

**Ví dụ:**
```
Input: head = [1,2,3,4,5], n = 2
Output: [1,2,3,5]
```

**Hướng giải quyết:** Dùng **hai con trỏ (fast/slow)** với một `dummy node` đứng trước `head` để xử lý gọn trường hợp xóa chính node đầu. Cho `fast` đi trước `n` bước. Sau đó di chuyển `fast` và `slow` cùng lúc cho tới khi `fast` chạm cuối danh sách — lúc này `slow` đang đứng ngay trước node cần xóa (vì khoảng cách giữa `fast` và `slow` luôn là `n`). Cuối cùng, nối `slow.next` bỏ qua node cần xóa.

**Độ phức tạp:** Time O(L) với `L` là độ dài danh sách, Space O(1).

```python
"""
LeetCode #19 - Remove Nth Node From End of List
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class ListNode:
    def __init__(self, val=0, next=None):
        self.val = val
        self.next = next


class Solution:
    def remove_nth_from_end(self, head, n: int):
        dummy = ListNode(0, head)
        fast = slow = dummy
        for _ in range(n):
            fast = fast.next
        while fast.next:
            fast = fast.next
            slow = slow.next
        slow.next = slow.next.next
        return dummy.next


if __name__ == "__main__":

    def build_list(values):
        dummy = ListNode(0)
        cur = dummy
        for v in values:
            cur.next = ListNode(v)
            cur = cur.next
        return dummy.next

    def to_list(node):
        result = []
        while node:
            result.append(node.val)
            node = node.next
        return result

    head = build_list([1, 2, 3, 4, 5])
    sol = Solution()
    result = sol.remove_nth_from_end(head, 2)
    print(to_list(result))  # Expect: [1, 2, 3, 5]
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

import com.motives.leetcode.common.ListNode;

/**
 * LeetCode #19 - Remove Nth Node From End of List (Nhom A, bai 15).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class RemoveNthNodeFromEndOfList {

    public ListNode removeNthFromEnd(ListNode head, int n) {
        ListNode dummy = new ListNode(0, head);
        ListNode fast = dummy;
        ListNode slow = dummy;

        for (int i = 0; i < n; i++) {
            fast = fast.getNext();
        }
        while (fast.getNext() != null) {
            fast = fast.getNext();
            slow = slow.getNext();
        }
        slow.setNext(slow.getNext().getNext());
        return dummy.getNext();
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-16"></a>

## 16. Construct Binary Tree from Preorder and Inorder Traversal (#105) — Medium

**Đề bài:** Cho mảng duyệt Preorder và Inorder của một cây nhị phân, xây dựng lại chính xác cây đó.

**Ví dụ:**
```
Input: preorder = [3,9,20,15,7], inorder = [9,3,15,20,7]
Output: [3,9,20,null,null,15,7]
```

**Hướng giải quyết:** Phần tử đầu tiên còn lại trong `preorder` luôn là **gốc** của cây/nhánh con hiện tại. Dùng một hash map `giá trị -> chỉ số trong inorder` để tra nhanh **vị trí của gốc trong inorder** — từ đó biết được phần bên trái của gốc trong inorder chính là nhánh con trái, phần bên phải là nhánh con phải. Dùng một con trỏ (`pre_idx`) để "lấy" từng gốc lần lượt theo đúng thứ tự preorder (gốc → trái → phải) khi đệ quy dựng nhánh trái trước, nhánh phải sau.

**Độ phức tạp:** Time O(n), Space O(n).

```python
"""
LeetCode #105 - Construct Binary Tree from Preorder and Inorder Traversal
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right


class Solution:
    def build_tree(self, preorder: list[int], inorder: list[int]) -> TreeNode | None:
        index_map = {val: i for i, val in enumerate(inorder)}
        self.pre_idx = 0

        def build(left: int, right: int) -> TreeNode | None:
            if left > right:
                return None
            root_val = preorder[self.pre_idx]
            self.pre_idx += 1
            root = TreeNode(root_val)
            mid = index_map[root_val]
            root.left = build(left, mid - 1)
            root.right = build(mid + 1, right)
            return root

        return build(0, len(inorder) - 1)


if __name__ == "__main__":

    def level_order(root):
        if not root:
            return []
        result, queue = [], [root]
        while queue:
            node = queue.pop(0)
            result.append(node.val)
            if node.left:
                queue.append(node.left)
            if node.right:
                queue.append(node.right)
        return result

    sol = Solution()
    tree = sol.build_tree([3, 9, 20, 15, 7], [9, 3, 15, 20, 7])
    print(level_order(tree))  # Expect: [3, 9, 20, 15, 7]
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

import com.motives.leetcode.common.TreeNode;
import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #105 - Construct Binary Tree from Preorder and Inorder Traversal (Nhom A, bai 16).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class BuildTreeFromPreorderInorder {

    public TreeNode buildTree(int[] preorder, int[] inorder) {
        return new TreeBuilder(preorder, inorder).build();
    }

    /** Encapsulates the recursion state so the recursive helper needs only two parameters. */
    private static final class TreeBuilder {
        private final int[] preorder;
        private final Map<Integer, Integer> inorderIndexByValue;
        private int preorderCursor;

        private TreeBuilder(int[] preorder, int[] inorder) {
            this.preorder = preorder;
            this.inorderIndexByValue = new HashMap<>();
            for (int i = 0; i < inorder.length; i++) {
                inorderIndexByValue.put(inorder[i], i);
            }
        }

        private TreeNode build() {
            return build(0, preorder.length - 1);
        }

        private TreeNode build(int inorderLeft, int inorderRight) {
            if (inorderLeft > inorderRight) {
                return null;
            }

            int rootValue = preorder[preorderCursor++];
            int rootInorderIndex = inorderIndexByValue.get(rootValue);

            TreeNode left = build(inorderLeft, rootInorderIndex - 1);
            TreeNode right = build(rootInorderIndex + 1, inorderRight);
            return new TreeNode(rootValue, left, right);
        }
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-17"></a>

## 17. Kth Largest Element in an Array (#215) — Medium

**Đề bài:** Tìm phần tử lớn thứ `k` trong mảng (không cần sort toàn bộ).

**Ví dụ:**
```
Input: nums = [3,2,1,5,6,4], k = 2
Output: 5
Giải thích: sort giam dan la [6,5,4,3,2,1], phan tu lon thu 2 la 5
```

**Hướng giải quyết:** Duy trì một **min-heap kích thước cố định `k`**, chứa `k` phần tử lớn nhất đã gặp tính đến hiện tại. Khởi tạo heap với `k` phần tử đầu. Với mỗi phần tử còn lại, nếu nó lớn hơn phần tử nhỏ nhất trong heap (`heap[0]`), thay thế phần tử nhỏ nhất đó bằng phần tử mới (`heapreplace`). Sau khi duyệt hết mảng, phần tử nhỏ nhất còn lại trong heap (`heap[0]`) chính là phần tử lớn thứ `k`. (Cách khác: **Quickselect** cho average O(n), thường được hỏi thêm khi phỏng vấn muốn tối ưu hơn heap O(n log k)).

**Độ phức tạp:** Time O(n log k), Space O(k).

```python
"""
LeetCode #215 - Kth Largest Element in an Array
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

import heapq


class Solution:
    def find_kth_largest(self, nums: list[int], k: int) -> int:
        heap = nums[:k]
        heapq.heapify(heap)
        for num in nums[k:]:
            if num > heap[0]:
                heapq.heapreplace(heap, num)
        return heap[0]


if __name__ == "__main__":
    sol = Solution()
    print(sol.find_kth_largest([3, 2, 1, 5, 6, 4], 2))  # Expect: 5
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

import java.util.PriorityQueue;

/**
 * LeetCode #215 - Kth Largest Element in an Array (Nhom A, bai 17).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class KthLargestElement {

    public int findKthLargest(int[] nums, int k) {
        PriorityQueue<Integer> smallestOfTopK = new PriorityQueue<>(k);
        for (int num : nums) {
            smallestOfTopK.add(num);
            if (smallestOfTopK.size() > k) {
                smallestOfTopK.poll();
            }
        }
        return smallestOfTopK.peek();
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-18"></a>

## 18. Search in Rotated Sorted Array (#33) — Medium

**Đề bài:** Tìm `target` trong một mảng đã sort nhưng bị xoay (rotate) tại một điểm không xác định, yêu cầu O(log n).

**Ví dụ:**
```
Input: nums = [4,5,6,7,0,1,2], target = 0
Output: 4
```

**Hướng giải quyết:** Vẫn dùng **binary search**, nhưng tại mỗi bước cần xác định **nửa nào (trái hoặc phải của `mid`) đang được sort đúng thứ tự**. So sánh `nums[left]` với `nums[mid]`: nếu `nums[left] <= nums[mid]`, nửa trái đang sort bình thường — kiểm tra `target` có nằm trong khoảng `[nums[left], nums[mid])` để quyết định thu hẹp về bên trái hay bên phải. Ngược lại, nửa phải đang sort bình thường — áp dụng logic tương tự cho nửa đó.

**Độ phức tạp:** Time O(log n), Space O(1).

```python
"""
LeetCode #33 - Search in Rotated Sorted Array
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def search(self, nums: list[int], target: int) -> int:
        left, right = 0, len(nums) - 1
        while left <= right:
            mid = (left + right) // 2
            if nums[mid] == target:
                return mid
            if nums[left] <= nums[mid]:
                if nums[left] <= target < nums[mid]:
                    right = mid - 1
                else:
                    left = mid + 1
            else:
                if nums[mid] < target <= nums[right]:
                    left = mid + 1
                else:
                    right = mid - 1
        return -1


if __name__ == "__main__":
    sol = Solution()
    print(sol.search([4, 5, 6, 7, 0, 1, 2], 0))  # Expect: 4
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupa;

/**
 * LeetCode #33 - Search in Rotated Sorted Array (Nhom A, bai 18).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class SearchInRotatedSortedArray {

    public int search(int[] nums, int target) {
        int left = 0;
        int right = nums.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                return mid;
            }

            boolean leftHalfIsSorted = nums[left] <= nums[mid];
            if (leftHalfIsSorted) {
                if (nums[left] <= target && target < nums[mid]) {
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            } else {
                if (nums[mid] < target && target <= nums[right]) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }
        return -1;
    }
}
```

---

# NHÓM B — 10 bài bổ sung (phủ thêm pattern)

[⬆ Về mục lục](#muc-luc)

<a id="bai-19"></a>

## 19. 3Sum (#15) — Medium

**Đề bài:** Tìm tất cả bộ ba số phân biệt trong mảng có tổng bằng 0.

**Ví dụ:**
```
Input: nums = [-1,0,1,2,-1,-4]
Output: [[-1,-1,2],[-1,0,1]]
```

**Hướng giải quyết:** Sort mảng trước. Cố định lần lượt từng phần tử `nums[i]` làm số đầu tiên, sau đó dùng **two pointers** (`left`, `right`) quét trên phần còn lại để tìm cặp có tổng bằng `-nums[i]`. Nếu tổng ba số nhỏ hơn 0, tăng `left`; nếu lớn hơn 0, giảm `right`; nếu bằng 0, ghi nhận kết quả rồi di chuyển cả hai con trỏ, đồng thời **bỏ qua các giá trị trùng lặp** (skip duplicate) để tránh bộ ba lặp lại trong kết quả.

**Độ phức tạp:** Time O(n²), Space O(1) (ngoài mảng kết quả).

```python
"""
LeetCode #15 - 3Sum
Nhom B - 10 bai bo sung
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def three_sum(self, nums: list[int]) -> list[list[int]]:
        nums.sort()
        n = len(nums)
        result = []
        for i in range(n - 2):
            if i > 0 and nums[i] == nums[i - 1]:
                continue
            left, right = i + 1, n - 1
            while left < right:
                total = nums[i] + nums[left] + nums[right]
                if total < 0:
                    left += 1
                elif total > 0:
                    right -= 1
                else:
                    result.append([nums[i], nums[left], nums[right]])
                    left += 1
                    right -= 1
                    while left < right and nums[left] == nums[left - 1]:
                        left += 1
                    while left < right and nums[right] == nums[right + 1]:
                        right -= 1
        return result


if __name__ == "__main__":
    sol = Solution()
    print(sol.three_sum([-1, 0, 1, 2, -1, -4]))  # Expect: [[-1, -1, 2], [-1, 0, 1]]
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * LeetCode #15 - 3Sum (Nhom B, bai 19).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class ThreeSum {

    public List<List<Integer>> threeSum(int[] nums) {
        int[] sorted = nums.clone();
        Arrays.sort(sorted);

        List<List<Integer>> triplets = new ArrayList<>();
        for (int i = 0; i < sorted.length - 2; i++) {
            if (i > 0 && sorted[i] == sorted[i - 1]) {
                continue;
            }
            findPairsWithTargetSum(sorted, i, triplets);
        }
        return triplets;
    }

    private void findPairsWithTargetSum(int[] sorted, int fixedIndex, List<List<Integer>> triplets) {
        int left = fixedIndex + 1;
        int right = sorted.length - 1;

        while (left < right) {
            int sum = sorted[fixedIndex] + sorted[left] + sorted[right];
            if (sum < 0) {
                left++;
            } else if (sum > 0) {
                right--;
            } else {
                triplets.add(List.of(sorted[fixedIndex], sorted[left], sorted[right]));
                left++;
                right--;
                while (left < right && sorted[left] == sorted[left - 1]) {
                    left++;
                }
                while (left < right && sorted[right] == sorted[right + 1]) {
                    right--;
                }
            }
        }
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-20"></a>

## 20. Trapping Rain Water (#42) — Hard

**Đề bài:** Cho mảng độ cao các cột, tính lượng nước tối đa có thể chứa được giữa các cột sau khi mưa.

**Ví dụ:**
```
Input: height = [0,1,0,2,1,0,1,3,2,1,2,1]
Output: 6
```

**Hướng giải quyết:** Dùng **two pointers** từ hai đầu (`left`, `right`), duy trì `left_max`/`right_max` là chiều cao lớn nhất đã gặp từ mỗi phía. Ở mỗi bước, di chuyển con trỏ có `max` nhỏ hơn vào trong — vì lượng nước tại vị trí đó chỉ bị giới hạn bởi cột thấp hơn trong hai phía (nước không thể cao hơn "bức tường" thấp nhất). Cộng dồn `max_hiện_tại - height[con_trỏ]` vào tổng nước.

**Độ phức tạp:** Time O(n), Space O(1).

```python
"""
LeetCode #42 - Trapping Rain Water
Nhom B - 10 bai bo sung
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def trap(self, height: list[int]) -> int:
        if not height:
            return 0
        left, right = 0, len(height) - 1
        left_max, right_max = height[left], height[right]
        water = 0
        while left < right:
            if left_max <= right_max:
                left += 1
                left_max = max(left_max, height[left])
                water += left_max - height[left]
            else:
                right -= 1
                right_max = max(right_max, height[right])
                water += right_max - height[right]
        return water


if __name__ == "__main__":
    sol = Solution()
    print(sol.trap([0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1]))  # Expect: 6
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupb;

/**
 * LeetCode #42 - Trapping Rain Water (Nhom B, bai 20).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class TrappingRainWater {

    public int trap(int[] height) {
        if (height.length == 0) {
            return 0;
        }

        int left = 0;
        int right = height.length - 1;
        int leftMax = height[left];
        int rightMax = height[right];
        int totalWater = 0;

        while (left < right) {
            if (leftMax <= rightMax) {
                left++;
                leftMax = Math.max(leftMax, height[left]);
                totalWater += leftMax - height[left];
            } else {
                right--;
                rightMax = Math.max(rightMax, height[right]);
                totalWater += rightMax - height[right];
            }
        }
        return totalWater;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-21"></a>

## 21. Course Schedule (#207) — Medium

**Đề bài:** Cho `numCourses` môn học và danh sách cặp `[course, prerequisite]`, xác định có thể học hết tất cả môn hay không (không có chu trình phụ thuộc vòng).

**Ví dụ:**
```
Ví dụ 1:
Input: numCourses = 2, prerequisites = [[1,0]]
Output: true
Giải thích: hoc mon 0 truoc, roi hoc mon 1 - khong co chu trinh

Ví dụ 2:
Input: numCourses = 2, prerequisites = [[1,0],[0,1]]
Output: false
Giải thích: mon 0 can mon 1, mon 1 lai can mon 0 - chu trinh vo han
```

**Hướng giải quyết:** Mô hình hóa thành đồ thị có hướng, mỗi cạnh là `prerequisite -> course`. Áp dụng **Kahn's Algorithm (BFS + in-degree)**: tính in-degree (số môn tiên quyết) của mỗi môn, đưa các môn có in-degree = 0 vào queue (học được ngay). Mỗi khi "học" một môn, giảm in-degree của các môn phụ thuộc nó; môn nào về 0 thì thêm vào queue. Nếu số môn học được (`visited`) bằng `numCourses` thì không có chu trình — có thể học hết.

**Độ phức tạp:** Time O(V + E), Space O(V + E).

```python
"""
LeetCode #207 - Course Schedule
Nhom B - 10 bai bo sung
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

from collections import defaultdict, deque


class Solution:
    def can_finish(self, numCourses: int, prerequisites: list[list[int]]) -> bool:
        graph = defaultdict(list)
        indegree = [0] * numCourses
        for course, pre in prerequisites:
            graph[pre].append(course)
            indegree[course] += 1

        queue = deque([c for c in range(numCourses) if indegree[c] == 0])
        visited = 0
        while queue:
            node = queue.popleft()
            visited += 1
            for nxt in graph[node]:
                indegree[nxt] -= 1
                if indegree[nxt] == 0:
                    queue.append(nxt)
        return visited == numCourses


if __name__ == "__main__":
    sol = Solution()
    print(sol.can_finish(2, [[1, 0]]))  # Expect: True
    print(sol.can_finish(2, [[1, 0], [0, 1]]))  # Expect: False
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupb;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * LeetCode #207 - Course Schedule (Nhom B, bai 21).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class CourseSchedule {

    public boolean canFinish(int numCourses, int[][] prerequisites) {
        List<List<Integer>> dependentCourses = buildAdjacencyList(numCourses, prerequisites);
        int[] inDegree = computeInDegrees(numCourses, prerequisites);

        Deque<Integer> readyToTake = new ArrayDeque<>();
        for (int course = 0; course < numCourses; course++) {
            if (inDegree[course] == 0) {
                readyToTake.add(course);
            }
        }

        int coursesTaken = 0;
        while (!readyToTake.isEmpty()) {
            int course = readyToTake.poll();
            coursesTaken++;
            for (int dependent : dependentCourses.get(course)) {
                if (--inDegree[dependent] == 0) {
                    readyToTake.add(dependent);
                }
            }
        }
        return coursesTaken == numCourses;
    }

    private List<List<Integer>> buildAdjacencyList(int numCourses, int[][] prerequisites) {
        List<List<Integer>> adjacency = new ArrayList<>(numCourses);
        for (int i = 0; i < numCourses; i++) {
            adjacency.add(new ArrayList<>());
        }
        for (int[] prerequisite : prerequisites) {
            int course = prerequisite[0];
            int mustTakeFirst = prerequisite[1];
            adjacency.get(mustTakeFirst).add(course);
        }
        return adjacency;
    }

    private int[] computeInDegrees(int numCourses, int[][] prerequisites) {
        int[] inDegree = new int[numCourses];
        for (int[] prerequisite : prerequisites) {
            inDegree[prerequisite[0]]++;
        }
        return inDegree;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-22"></a>

## 22. Word Break (#139) — Medium

**Đề bài:** Cho chuỗi `s` và từ điển `wordDict`, xác định `s` có thể được tách thành các từ trong từ điển hay không.

**Ví dụ:**
```
Input: s = "leetcode", wordDict = ["leet","code"]
Output: true
Giải thích: "leetcode" = "leet" + "code"
```

**Hướng giải quyết:** Dùng **DP trên chuỗi**: `dp[i] = True` nếu chuỗi con `s[0:i]` có thể tách được hoàn toàn từ từ điển. `dp[0] = True` (chuỗi rỗng luôn tách được). Với mỗi `i`, thử mọi điểm cắt `j < i`: nếu `dp[j]` đã đúng và phần còn lại `s[j:i]` là một từ hợp lệ trong từ điển, thì `dp[i] = True`. Kết quả cuối là `dp[n]`.

**Độ phức tạp:** Time O(n² ) (n là độ dài chuỗi, giả sử tra từ điển O(1) nhờ set), Space O(n).

```python
"""
LeetCode #139 - Word Break
Nhom B - 10 bai bo sung
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def word_break(self, s: str, wordDict: list[str]) -> bool:
        word_set = set(wordDict)
        n = len(s)
        dp = [False] * (n + 1)
        dp[0] = True
        for i in range(1, n + 1):
            for j in range(i):
                if dp[j] and s[j:i] in word_set:
                    dp[i] = True
                    break
        return dp[n]


if __name__ == "__main__":
    sol = Solution()
    print(sol.word_break("leetcode", ["leet", "code"]))  # Expect: True
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupb;

import java.util.List;
import java.util.Set;

/**
 * LeetCode #139 - Word Break (Nhom B, bai 22).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class WordBreak {

    public boolean wordBreak(String s, List<String> wordDict) {
        Set<String> dictionary = Set.copyOf(wordDict);
        int n = s.length();

        boolean[] breakable = new boolean[n + 1];
        breakable[0] = true;

        for (int end = 1; end <= n; end++) {
            for (int start = 0; start < end; start++) {
                if (breakable[start] && dictionary.contains(s.substring(start, end))) {
                    breakable[end] = true;
                    break;
                }
            }
        }
        return breakable[n];
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-23"></a>

## 23. Coin Change (#322) — Medium

**Đề bài:** Cho các loại tiền `coins` và số tiền `amount`, tìm số lượng tiền tối thiểu để đủ `amount` (hoặc -1 nếu không thể).

**Ví dụ:**
```
Input: coins = [1,2,5], amount = 11
Output: 3
Giải thích: 11 = 5 + 5 + 1 (3 dong)
```

**Hướng giải quyết:** DP dạng **unbounded knapsack**: `dp[a]` là số coin tối thiểu để đạt đúng tổng `a`. Khởi tạo `dp[0] = 0`, còn lại là vô hạn (`inf`). Với mỗi số tiền `a` từ 1 đến `amount`, thử dùng từng loại `coin`: nếu `coin <= a`, thì `dp[a] = min(dp[a], dp[a-coin] + 1)` — nghĩa là dùng thêm 1 đồng `coin` sau khi đã giải quyết xong phần `a - coin`.

**Độ phức tạp:** Time O(amount × số loại coin), Space O(amount).

```python
"""
LeetCode #322 - Coin Change
Nhom B - 10 bai bo sung
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def coin_change(self, coins: list[int], amount: int) -> int:
        dp = [float("inf")] * (amount + 1)
        dp[0] = 0
        for a in range(1, amount + 1):
            for coin in coins:
                if coin <= a:
                    dp[a] = min(dp[a], dp[a - coin] + 1)
        return dp[amount] if dp[amount] != float("inf") else -1


if __name__ == "__main__":
    sol = Solution()
    print(sol.coin_change([1, 2, 5], 11))  # Expect: 3
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupb;

import java.util.Arrays;

/**
 * LeetCode #322 - Coin Change (Nhom B, bai 23).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class CoinChange {

    private static final int UNREACHABLE = Integer.MAX_VALUE;

    public int coinChange(int[] coins, int amount) {
        int[] minCoinsForAmount = new int[amount + 1];
        Arrays.fill(minCoinsForAmount, UNREACHABLE);
        minCoinsForAmount[0] = 0;

        for (int currentAmount = 1; currentAmount <= amount; currentAmount++) {
            for (int coin : coins) {
                if (coin <= currentAmount && minCoinsForAmount[currentAmount - coin] != UNREACHABLE) {
                    minCoinsForAmount[currentAmount] =
                            Math.min(minCoinsForAmount[currentAmount], minCoinsForAmount[currentAmount - coin] + 1);
                }
            }
        }
        return minCoinsForAmount[amount] == UNREACHABLE ? -1 : minCoinsForAmount[amount];
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-24"></a>

## 24. Minimum Window Substring (#76) — Hard

**Đề bài:** Tìm cửa sổ (substring) ngắn nhất trong `s` chứa đủ mọi ký tự (kể cả trùng) có trong `t`.

**Ví dụ:**
```
Input: s = "ADOBECODEBANC", t = "ABC"
Output: "BANC"
```

**Hướng giải quyết:** **Sliding window** với một `Counter` (`need`) đếm số ký tự còn thiếu để đủ `t`, và biến `missing` là tổng số ký tự còn thiếu. Mở rộng `right` sang phải, mỗi khi "nhặt" được một ký tự đang cần (`need[ch] > 0`), giảm `missing`. Khi `missing == 0` (cửa sổ hiện tại đã đủ `t`), thử **co `left` vào** để thu nhỏ cửa sổ tối đa có thể (bỏ các ký tự dư), ghi nhận kết quả nếu ngắn hơn kết quả tốt nhất hiện có, rồi tiếp tục nhích `left` lên 1 (làm cửa sổ lại "thiếu" 1 ký tự) để mở rộng `right` tiếp ở vòng sau.

**Độ phức tạp:** Time O(|s| + |t|), Space O(|t|).

```python
"""
LeetCode #76 - Minimum Window Substring
Nhom B - 10 bai bo sung
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

from collections import Counter


class Solution:
    def min_window(self, s: str, t: str) -> str:
        if not s or not t:
            return ""
        need = Counter(t)
        missing = len(t)
        left = 0
        best_left, best_right = 0, 0
        for right, ch in enumerate(s, 1):
            if need[ch] > 0:
                missing -= 1
            need[ch] -= 1
            if missing == 0:
                while need[s[left]] < 0:
                    need[s[left]] += 1
                    left += 1
                if best_right == 0 or right - left < best_right - best_left:
                    best_left, best_right = left, right
                need[s[left]] += 1
                missing += 1
                left += 1
        return s[best_left:best_right]


if __name__ == "__main__":
    sol = Solution()
    print(sol.min_window("ADOBECODEBANC", "ABC"))  # Expect: "BANC"
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupb;

/**
 * LeetCode #76 - Minimum Window Substring (Nhom B, bai 24).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class MinimumWindowSubstring {

    private static final int ASCII_SIZE = 128;

    public String minWindow(String s, String t) {
        if (s.isEmpty() || t.isEmpty()) {
            return "";
        }

        int[] remainingNeeded = new int[ASCII_SIZE];
        for (char c : t.toCharArray()) {
            remainingNeeded[c]++;
        }

        int missingCount = t.length();
        int windowStart = 0;
        int bestStart = 0;
        int bestEnd = 0;

        for (int windowEnd = 1; windowEnd <= s.length(); windowEnd++) {
            char enteringChar = s.charAt(windowEnd - 1);
            if (remainingNeeded[enteringChar] > 0) {
                missingCount--;
            }
            remainingNeeded[enteringChar]--;

            if (missingCount == 0) {
                windowStart = shrinkWindow(s, remainingNeeded, windowStart);
                if (bestEnd == 0 || windowEnd - windowStart < bestEnd - bestStart) {
                    bestStart = windowStart;
                    bestEnd = windowEnd;
                }
                remainingNeeded[s.charAt(windowStart)]++;
                missingCount++;
                windowStart++;
            }
        }
        return s.substring(bestStart, bestEnd);
    }

    private int shrinkWindow(String s, int[] remainingNeeded, int windowStart) {
        while (remainingNeeded[s.charAt(windowStart)] < 0) {
            remainingNeeded[s.charAt(windowStart)]++;
            windowStart++;
        }
        return windowStart;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-25"></a>

## 25. Top K Frequent Elements (#347) — Medium

**Đề bài:** Trả về `k` phần tử xuất hiện nhiều nhất trong mảng.

**Ví dụ:**
```
Input: nums = [1,1,1,2,2,3], k = 2
Output: [1, 2]
```

**Hướng giải quyết:** Đếm tần suất mỗi phần tử bằng `Counter` (hash map). Sau đó dùng `heapq.nlargest(k, ...)` — về bản chất là một **max-heap** rút ra `k` phần tử có tần suất lớn nhất — để lấy đúng `k` phần tử theo tần suất mà không cần sort toàn bộ danh sách tần suất.

**Độ phức tạp:** Time O(n log k), Space O(n).

```python
"""
LeetCode #347 - Top K Frequent Elements
Nhom B - 10 bai bo sung
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

import heapq
from collections import Counter


class Solution:
    def top_k_frequent(self, nums: list[int], k: int) -> list[int]:
        counts = Counter(nums)
        return [num for num, _ in heapq.nlargest(k, counts.items(), key=lambda x: x[1])]


if __name__ == "__main__":
    sol = Solution()
    print(sol.top_k_frequent([1, 1, 1, 2, 2, 3], 2))  # Expect: [1, 2]
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupb;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * LeetCode #347 - Top K Frequent Elements (Nhom B, bai 25).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class TopKFrequentElements {

    public int[] topKFrequent(int[] nums, int k) {
        Map<Integer, Integer> frequencyByValue = new HashMap<>();
        for (int num : nums) {
            frequencyByValue.merge(num, 1, Integer::sum);
        }

        PriorityQueue<Integer> leastFrequentOfTopK =
                new PriorityQueue<>((a, b) -> frequencyByValue.get(a) - frequencyByValue.get(b));
        for (int value : frequencyByValue.keySet()) {
            leastFrequentOfTopK.add(value);
            if (leastFrequentOfTopK.size() > k) {
                leastFrequentOfTopK.poll();
            }
        }

        int[] result = new int[k];
        for (int i = k - 1; i >= 0; i--) {
            result[i] = leastFrequentOfTopK.poll();
        }
        return result;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-26"></a>

## 26. Lowest Common Ancestor of a Binary Tree (#236) — Medium

**Đề bài:** Tìm node tổ tiên chung gần nhất (LCA) của hai node `p` và `q` trong cây nhị phân (không phải BST).

**Ví dụ:**
```
Input: root = [3,5,1,6,2,0,8,null,null,7,4], p = 5, q = 1
Output: 3
Giải thích: node 5 va node 1 co to tien chung gan nhat la node 3 (goc)
```

**Hướng giải quyết:** Đệ quy DFS "bottom-up": tại mỗi node, nếu node hiện tại là `None`, hoặc chính là `p`/`q`, trả về node đó ngay. Ngược lại, đệ quy tìm trong nhánh trái và nhánh phải. Nếu **cả hai nhánh đều tìm thấy kết quả không rỗng**, nghĩa là `p` và `q` nằm ở hai nhánh khác nhau — node hiện tại chính là LCA. Nếu chỉ một nhánh có kết quả, LCA nằm ở nhánh đó — trả kết quả của nhánh đó lên.

**Độ phức tạp:** Time O(n), Space O(h) với `h` là chiều cao cây (đệ quy).

```python
"""
LeetCode #236 - Lowest Common Ancestor of a Binary Tree
Nhom B - 10 bai bo sung
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right


class Solution:
    def lowest_common_ancestor(self, root, p, q):
        if not root or root == p or root == q:
            return root
        left = self.lowest_common_ancestor(root.left, p, q)
        right = self.lowest_common_ancestor(root.right, p, q)
        if left and right:
            return root
        return left or right


if __name__ == "__main__":

    def find_node(root, val):
        if not root:
            return None
        if root.val == val:
            return root
        return find_node(root.left, val) or find_node(root.right, val)

    root = TreeNode(
        3,
        TreeNode(5, TreeNode(6), TreeNode(2, TreeNode(7), TreeNode(4))),
        TreeNode(1, TreeNode(0), TreeNode(8)),
    )
    p = find_node(root, 5)
    q = find_node(root, 1)
    sol = Solution()
    print(sol.lowest_common_ancestor(root, p, q).val)  # Expect: 3
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupb;

import com.motives.leetcode.common.TreeNode;

/**
 * LeetCode #236 - Lowest Common Ancestor of a Binary Tree (Nhom B, bai 26).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class LowestCommonAncestor {

    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) {
            return root;
        }

        TreeNode fromLeft = lowestCommonAncestor(root.left(), p, q);
        TreeNode fromRight = lowestCommonAncestor(root.right(), p, q);

        if (fromLeft != null && fromRight != null) {
            return root;
        }
        return fromLeft != null ? fromLeft : fromRight;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-27"></a>

## 27. Word Search (#79) — Medium

**Đề bài:** Cho lưới ký tự 2D và một từ `word`, xác định `word` có thể được tạo thành bằng cách đi liên tiếp qua các ô kề nhau (4 hướng, không lặp ô) hay không.

**Ví dụ:**
```
Input: board = [["A","B","C","E"],["S","F","C","S"],["A","D","E","E"]], word = "ABCCED"
Output: true
```

**Hướng giải quyết:** Dùng **Backtracking + DFS**: thử bắt đầu từ mọi ô trong lưới. Tại mỗi bước DFS `(r, c, i)`, nếu ký tự tại ô hiện tại khớp với `word[i]`, tạm đánh dấu ô đó là "đã dùng" (ví dụ đổi thành `'#'`) rồi đệ quy thử 4 hướng cho `word[i+1]`. Nếu nhánh đó thất bại, **backtrack** — khôi phục lại ký tự gốc của ô để tiếp tục thử hướng khác/vị trí khác.

**Độ phức tạp:** Time O(rows × cols × 4^L) với `L` là độ dài `word`, Space O(L) cho đệ quy.

```python
"""
LeetCode #79 - Word Search
Nhom B - 10 bai bo sung
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def exist(self, board: list[list[str]], word: str) -> bool:
        rows, cols = len(board), len(board[0])

        def dfs(r: int, c: int, i: int) -> bool:
            if i == len(word):
                return True
            if r < 0 or r >= rows or c < 0 or c >= cols or board[r][c] != word[i]:
                return False
            temp, board[r][c] = board[r][c], "#"
            found = (
                dfs(r + 1, c, i + 1)
                or dfs(r - 1, c, i + 1)
                or dfs(r, c + 1, i + 1)
                or dfs(r, c - 1, i + 1)
            )
            board[r][c] = temp
            return found

        for r in range(rows):
            for c in range(cols):
                if dfs(r, c, 0):
                    return True
        return False


if __name__ == "__main__":
    board = [["A", "B", "C", "E"], ["S", "F", "C", "S"], ["A", "D", "E", "E"]]
    sol = Solution()
    print(sol.exist(board, "ABCCED"))  # Expect: True
    print(sol.exist(board, "SEE"))  # Expect: True
    print(sol.exist(board, "ABCB"))  # Expect: False
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupb;

/**
 * LeetCode #79 - Word Search (Nhom B, bai 27).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class WordSearch {

    private static final char VISITED_MARKER = '#';
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public boolean exist(char[][] board, String word) {
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                if (searchFrom(board, word, row, col, 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean searchFrom(char[][] board, String word, int row, int col, int wordIndex) {
        if (wordIndex == word.length()) {
            return true;
        }
        if (!isMatchingCell(board, word, row, col, wordIndex)) {
            return false;
        }

        char original = board[row][col];
        board[row][col] = VISITED_MARKER;

        boolean found = false;
        for (int[] direction : DIRECTIONS) {
            if (searchFrom(board, word, row + direction[0], col + direction[1], wordIndex + 1)) {
                found = true;
                break;
            }
        }

        board[row][col] = original;
        return found;
    }

    private boolean isMatchingCell(char[][] board, String word, int row, int col, int wordIndex) {
        return row >= 0 && row < board.length
                && col >= 0 && col < board[row].length
                && board[row][col] == word.charAt(wordIndex);
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-28"></a>

## 28. Validate Binary Search Tree (#98) — Medium

**Đề bài:** Kiểm tra một cây nhị phân có phải là Binary Search Tree (BST) hợp lệ hay không.

**Ví dụ:**
```
Ví dụ 1:
Input: root = [2,1,3]
Output: true

Ví dụ 2:
Input: root = [5,1,4,null,null,3,6]
Output: false
Giải thích: node 4 nam o nhanh phai cua goc 5, nhung 4 < 5 nen vi pham
dieu kien BST (toan bo nhanh phai phai lon hon goc)
```

**Hướng giải quyết:** **Bẫy phổ biến** là chỉ so sánh mỗi node với node cha trực tiếp — điều này sai vì BST yêu cầu **toàn bộ** subtree trái phải nhỏ/lớn hơn node, không chỉ node cha gần nhất. Giải đúng bằng DFS truyền theo một khoảng `(low, high)` hợp lệ cho mỗi node: node phải thỏa `low < node.val < high`. Khi đi vào nhánh trái, `high` được siết lại thành `node.val`; khi đi vào nhánh phải, `low` được siết lại thành `node.val`.

**Độ phức tạp:** Time O(n), Space O(h).

```python
"""
LeetCode #98 - Validate Binary Search Tree
Nhom B - 10 bai bo sung
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right


class Solution:
    def is_valid_bst(self, root) -> bool:
        def validate(node, low: float, high: float) -> bool:
            if not node:
                return True
            if not (low < node.val < high):
                return False
            return validate(node.left, low, node.val) and validate(
                node.right, node.val, high
            )

        return validate(root, float("-inf"), float("inf"))


if __name__ == "__main__":
    root_valid = TreeNode(2, TreeNode(1), TreeNode(3))
    sol = Solution()
    print(sol.is_valid_bst(root_valid))  # Expect: True

    root_invalid = TreeNode(5, TreeNode(1), TreeNode(4, TreeNode(3), TreeNode(6)))
    print(sol.is_valid_bst(root_invalid))  # Expect: False
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupb;

import com.motives.leetcode.common.TreeNode;

/**
 * LeetCode #98 - Validate Binary Search Tree (Nhom B, bai 28).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class ValidateBinarySearchTree {

    public boolean isValidBST(TreeNode root) {
        return isWithinBounds(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private boolean isWithinBounds(TreeNode node, long lowerBound, long upperBound) {
        if (node == null) {
            return true;
        }
        if (node.val() <= lowerBound || node.val() >= upperBound) {
            return false;
        }
        return isWithinBounds(node.left(), lowerBound, node.val())
                && isWithinBounds(node.right(), node.val(), upperBound);
    }
}
```

---

# NHÓM C — 10 bài tập trung HashSet / Dictionary

[⬆ Về mục lục](#muc-luc)

<a id="bai-29"></a>

## 29. Contains Duplicate (#217) — Easy

**Đề bài:** Kiểm tra mảng có phần tử trùng lặp hay không.

**Ví dụ:**
```
Input: nums = [1,2,3,1]
Output: true
```

**Hướng giải quyết:** Duyệt qua mảng, dùng một **HashSet** để lưu các giá trị đã gặp. Với mỗi phần tử, nếu nó đã có trong set thì trả về `True` ngay (đã tìm thấy trùng lặp). Nếu chưa, thêm vào set và tiếp tục. Đây là ví dụ cơ bản nhất cho việc dùng HashSet để tra cứu tồn tại trong O(1) thay vì so sánh từng cặp O(n²).

**Độ phức tạp:** Time O(n), Space O(n).

```python
"""
LeetCode #217 - Contains Duplicate
Nhom C - 10 bai HashSet/Dictionary
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def contains_duplicate(self, nums: list[int]) -> bool:
        seen = set()
        for num in nums:
            if num in seen:
                return True
            seen.add(num)
        return False


if __name__ == "__main__":
    sol = Solution()
    print(sol.contains_duplicate([1, 2, 3, 1]))  # Expect: True
    print(sol.contains_duplicate([1, 2, 3, 4]))  # Expect: False
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupc;

import java.util.HashSet;
import java.util.Set;

/**
 * LeetCode #217 - Contains Duplicate (Nhom C, bai 29).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class ContainsDuplicate {

    public boolean containsDuplicate(int[] nums) {
        Set<Integer> seen = new HashSet<>();
        for (int num : nums) {
            if (!seen.add(num)) {
                return true;
            }
        }
        return false;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-30"></a>

## 30. Valid Anagram (#242) — Easy

**Đề bài:** Kiểm tra hai chuỗi `s` và `t` có phải là anagram của nhau hay không (cùng tần suất ký tự).

**Ví dụ:**
```
Input: s = "anagram", t = "nagaram"
Output: true
```

**Hướng giải quyết:** Nếu độ dài khác nhau, chắc chắn không phải anagram. Ngược lại, dùng `Counter` (một dạng **dictionary đếm tần suất**) để đếm số lần xuất hiện của mỗi ký tự trong `s` và trong `t`. Hai chuỗi là anagram khi và chỉ khi hai `Counter` này **bằng nhau hoàn toàn** (so sánh dict trực tiếp bằng `==`).

**Độ phức tạp:** Time O(n), Space O(1) (giới hạn bởi bảng chữ cái) hoặc O(n) tổng quát.

```python
"""
LeetCode #242 - Valid Anagram
Nhom C - 10 bai HashSet/Dictionary
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

from collections import Counter


class Solution:
    def is_anagram(self, s: str, t: str) -> bool:
        if len(s) != len(t):
            return False
        return Counter(s) == Counter(t)


if __name__ == "__main__":
    sol = Solution()
    print(sol.is_anagram("anagram", "nagaram"))  # Expect: True
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupc;

/**
 * LeetCode #242 - Valid Anagram (Nhom C, bai 30).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class ValidAnagram {

    private static final int LOWERCASE_LETTER_COUNT = 26;

    public boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        int[] letterCounts = new int[LOWERCASE_LETTER_COUNT];
        for (int i = 0; i < s.length(); i++) {
            letterCounts[s.charAt(i) - 'a']++;
            letterCounts[t.charAt(i) - 'a']--;
        }

        for (int count : letterCounts) {
            if (count != 0) {
                return false;
            }
        }
        return true;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-31"></a>

## 31. Isomorphic Strings (#205) — Easy

**Đề bài:** Kiểm tra `s` và `t` có "đẳng cấu" (isomorphic) hay không — tồn tại một phép ánh xạ 1-1 giữa ký tự của `s` và ký tự của `t`.

**Ví dụ:**
```
Input: s = "egg", t = "add"
Output: true
Giải thích: e->a, g->d (anh xa nhat quan)
```

**Hướng giải quyết:** Cần đảm bảo ánh xạ là **song ánh (bijection)**, nên phải dùng **hai dictionary** theo hai chiều: `map_st` (ký tự `s -> t`) và `map_ts` (ký tự `t -> s`). Duyệt song song hai chuỗi; tại mỗi cặp ký tự `(cs, ct)`, nếu `cs` đã được map tới một ký tự khác `ct` trước đó, hoặc `ct` đã được map từ một ký tự khác `cs` trước đó, thì vi phạm tính song ánh — trả về `False`. Nếu hợp lệ, cập nhật cả hai map.

**Độ phức tạp:** Time O(n), Space O(1) (giới hạn alphabet).

```python
"""
LeetCode #205 - Isomorphic Strings
Nhom C - 10 bai HashSet/Dictionary
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def is_isomorphic(self, s: str, t: str) -> bool:
        map_st = {}
        map_ts = {}
        for cs, ct in zip(s, t):
            if cs in map_st and map_st[cs] != ct:
                return False
            if ct in map_ts and map_ts[ct] != cs:
                return False
            map_st[cs] = ct
            map_ts[ct] = cs
        return True


if __name__ == "__main__":
    sol = Solution()
    print(sol.is_isomorphic("egg", "add"))  # Expect: True
    print(sol.is_isomorphic("foo", "bar"))  # Expect: False
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupc;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #205 - Isomorphic Strings (Nhom C, bai 31).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class IsomorphicStrings {

    public boolean isIsomorphic(String s, String t) {
        Map<Character, Character> sourceToTarget = new HashMap<>();
        Map<Character, Character> targetToSource = new HashMap<>();

        for (int i = 0; i < s.length(); i++) {
            char sourceChar = s.charAt(i);
            char targetChar = t.charAt(i);

            Character mappedTarget = sourceToTarget.get(sourceChar);
            Character mappedSource = targetToSource.get(targetChar);

            if (mappedTarget != null && mappedTarget != targetChar) {
                return false;
            }
            if (mappedSource != null && mappedSource != sourceChar) {
                return false;
            }

            sourceToTarget.put(sourceChar, targetChar);
            targetToSource.put(targetChar, sourceChar);
        }
        return true;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-32"></a>

## 32. Longest Consecutive Sequence (#128) — Medium

**Đề bài:** Tìm độ dài dãy số nguyên liên tiếp dài nhất trong mảng (không cần liên tiếp về vị trí, chỉ cần liên tiếp về giá trị), yêu cầu O(n).

**Ví dụ:**
```
Input: nums = [100,4,200,1,3,2]
Output: 4
Giải thích: day lien tiep dai nhat la [1,2,3,4]
```

**Hướng giải quyết:** Đưa toàn bộ phần tử vào một **HashSet** để tra cứu tồn tại O(1). Với mỗi số `num`, chỉ bắt đầu đếm dãy nếu `num - 1` **không** có trong set — nghĩa là `num` là **điểm bắt đầu** của một dãy liên tiếp (nếu không kiểm tra điều kiện này, ta sẽ đếm lại một dãy nhiều lần từ các điểm giữa, gây tốn thời gian). Từ điểm bắt đầu, mở rộng dần `num + 1`, `num + 2`, ... miễn là còn trong set, đếm độ dài dãy đó.

**Độ phức tạp:** Time O(n) (mỗi số chỉ được "mở rộng" từ đúng 1 điểm bắt đầu), Space O(n).

```python
"""
LeetCode #128 - Longest Consecutive Sequence
Nhom C - 10 bai HashSet/Dictionary
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def longest_consecutive(self, nums: list[int]) -> int:
        num_set = set(nums)
        best = 0
        for num in num_set:
            if num - 1 not in num_set:
                length = 1
                while num + length in num_set:
                    length += 1
                best = max(best, length)
        return best


if __name__ == "__main__":
    sol = Solution()
    print(sol.longest_consecutive([100, 4, 200, 1, 3, 2]))  # Expect: 4
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupc;

import java.util.HashSet;
import java.util.Set;

/**
 * LeetCode #128 - Longest Consecutive Sequence (Nhom C, bai 32).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class LongestConsecutiveSequence {

    public int longestConsecutive(int[] nums) {
        Set<Integer> values = new HashSet<>();
        for (int num : nums) {
            values.add(num);
        }

        int longest = 0;
        for (int value : values) {
            boolean isSequenceStart = !values.contains(value - 1);
            if (isSequenceStart) {
                longest = Math.max(longest, sequenceLengthFrom(values, value));
            }
        }
        return longest;
    }

    private int sequenceLengthFrom(Set<Integer> values, int start) {
        int length = 1;
        while (values.contains(start + length)) {
            length++;
        }
        return length;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-33"></a>

## 33. Subarray Sum Equals K (#560) — Medium

**Đề bài:** Đếm số dãy con liên tiếp trong mảng có tổng bằng đúng `k`.

**Ví dụ:**
```
Input: nums = [1,1,1], k = 2
Output: 2
Giải thích: co 2 day con co tong = 2: [1,1] (vi tri 0-1) va [1,1] (vi tri 1-2)
```

**Hướng giải quyết:** Dùng kỹ thuật **prefix sum + hash map**. Gọi `prefix_sum` là tổng từ đầu mảng tới vị trí hiện tại. Một dãy con `[i+1..j]` có tổng bằng `k` khi và chỉ khi `prefix_sum[j] - prefix_sum[i] == k`, tức `prefix_sum[i] == prefix_sum[j] - k`. Vì vậy, khi duyệt tới vị trí `j`, ta chỉ cần tra map xem **đã có bao nhiêu vị trí trước đó** có `prefix_sum == prefix_sum[j] - k`, rồi cộng số đó vào kết quả. Map khởi tạo với `{0: 1}` để xử lý đúng trường hợp dãy con bắt đầu từ đầu mảng.

**Độ phức tạp:** Time O(n), Space O(n).

```python
"""
LeetCode #560 - Subarray Sum Equals K
Nhom C - 10 bai HashSet/Dictionary
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

from collections import defaultdict


class Solution:
    def subarray_sum(self, nums: list[int], k: int) -> int:
        prefix_count = defaultdict(int)
        prefix_count[0] = 1
        prefix_sum = 0
        result = 0
        for num in nums:
            prefix_sum += num
            result += prefix_count[prefix_sum - k]
            prefix_count[prefix_sum] += 1
        return result


if __name__ == "__main__":
    sol = Solution()
    print(sol.subarray_sum([1, 1, 1], 2))  # Expect: 2
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupc;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #560 - Subarray Sum Equals K (Nhom C, bai 33).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class SubarraySumEqualsK {

    public int subarraySum(int[] nums, int k) {
        Map<Integer, Integer> countByPrefixSum = new HashMap<>();
        countByPrefixSum.put(0, 1);

        int prefixSum = 0;
        int matchingSubarrays = 0;
        for (int num : nums) {
            prefixSum += num;
            matchingSubarrays += countByPrefixSum.getOrDefault(prefixSum - k, 0);
            countByPrefixSum.merge(prefixSum, 1, Integer::sum);
        }
        return matchingSubarrays;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-34"></a>

## 34. Intersection of Two Arrays II (#350) — Easy

**Đề bài:** Tìm giao của hai mảng, giữ đúng số lần lặp lại (khác với "Intersection of Two Arrays" #349 chỉ lấy giá trị duy nhất).

**Ví dụ:**
```
Input: nums1 = [1,2,2,1], nums2 = [2,2]
Output: [2, 2]
```

**Hướng giải quyết:** Đếm tần suất các phần tử của mảng thứ nhất bằng `Counter` (dictionary đếm). Duyệt qua mảng thứ hai; với mỗi phần tử, nếu tần suất còn lại trong `Counter` của nó lớn hơn 0, thêm vào kết quả và **giảm tần suất đi 1** (để không dùng lại quá số lượng đã đếm được ở mảng 1).

**Độ phức tạp:** Time O(n + m), Space O(min(n, m)).

```python
"""
LeetCode #350 - Intersection of Two Arrays II
Nhom C - 10 bai HashSet/Dictionary
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

from collections import Counter


class Solution:
    def intersect(self, nums1: list[int], nums2: list[int]) -> list[int]:
        counts = Counter(nums1)
        result = []
        for num in nums2:
            if counts[num] > 0:
                result.append(num)
                counts[num] -= 1
        return result


if __name__ == "__main__":
    sol = Solution()
    print(sol.intersect([1, 2, 2, 1], [2, 2]))  # Expect: [2, 2]
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LeetCode #350 - Intersection of Two Arrays II (Nhom C, bai 34).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class IntersectionOfTwoArrays {

    public int[] intersect(int[] nums1, int[] nums2) {
        Map<Integer, Integer> remainingCount = new HashMap<>();
        for (int num : nums1) {
            remainingCount.merge(num, 1, Integer::sum);
        }

        List<Integer> intersection = new ArrayList<>();
        for (int num : nums2) {
            int available = remainingCount.getOrDefault(num, 0);
            if (available > 0) {
                intersection.add(num);
                remainingCount.put(num, available - 1);
            }
        }
        return intersection.stream().mapToInt(Integer::intValue).toArray();
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-35"></a>

## 35. Happy Number (#202) — Easy

**Đề bài:** Một số là "happy number" nếu lặp lại việc thay số đó bằng tổng bình phương các chữ số của nó, cuối cùng sẽ về 1. Kiểm tra `n` có phải happy number.

**Ví dụ:**
```
Input: n = 19
Output: true
Giải thích: 19 -> 1^2+9^2=82 -> 8^2+2^2=68 -> 6^2+8^2=100 -> 1^2+0^2+0^2=1
```

**Hướng giải quyết:** Đây là bài toán **phát hiện chu trình (cycle detection)** trên một dãy số được sinh ra bởi hàm biến đổi. Dùng một **HashSet** để lưu lại tất cả các giá trị đã từng gặp trong quá trình biến đổi. Nếu tại một bước nào đó giá trị mới lại **trùng với một giá trị đã gặp trước đó** (mà chưa về 1), nghĩa là đã rơi vào chu trình lặp vô hạn → không phải happy number. Nếu giá trị về đúng 1 trước khi lặp, thì là happy number.

**Độ phức tạp:** Time O(log n) mỗi lần biến đổi × số bước tới khi phát hiện chu trình, Space O(số giá trị trung gian).

```python
"""
LeetCode #202 - Happy Number
Nhom C - 10 bai HashSet/Dictionary
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def is_happy(self, n: int) -> bool:
        seen = set()
        while n != 1 and n not in seen:
            seen.add(n)
            n = sum(int(d) ** 2 for d in str(n))
        return n == 1


if __name__ == "__main__":
    sol = Solution()
    print(sol.is_happy(19))  # Expect: True
    print(sol.is_happy(2))  # Expect: False
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupc;

import java.util.HashSet;
import java.util.Set;

/**
 * LeetCode #202 - Happy Number (Nhom C, bai 35).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class HappyNumber {

    public boolean isHappy(int n) {
        Set<Integer> seen = new HashSet<>();
        int current = n;
        while (current != 1 && seen.add(current)) {
            current = sumOfSquaredDigits(current);
        }
        return current == 1;
    }

    private int sumOfSquaredDigits(int number) {
        int sum = 0;
        while (number > 0) {
            int digit = number % 10;
            sum += digit * digit;
            number /= 10;
        }
        return sum;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-36"></a>

## 36. 4Sum II (#454) — Medium

**Đề bài:** Cho 4 mảng số `nums1, nums2, nums3, nums4` cùng độ dài `n`, đếm số bộ `(i, j, k, l)` sao cho `nums1[i] + nums2[j] + nums3[k] + nums4[l] == 0`.

**Ví dụ:**
```
Input: nums1 = [1,2], nums2 = [-2,-1], nums3 = [-1,2], nums4 = [0,2]
Output: 2
```

**Hướng giải quyết:** Nếu duyệt vét cạn 4 vòng lặp sẽ tốn O(n⁴). Thay vào đó, **chia đôi bài toán**: gộp `nums1` và `nums2` trước, tính mọi tổng `a + b` có thể và lưu **số lần xuất hiện** của mỗi tổng vào một hash map (`sum_ab`) — tốn O(n²). Sau đó, với mỗi cặp `(c, d)` từ `nums3` và `nums4`, ta cần tìm số cặp `(a, b)` sao cho `a + b == -(c + d)` — chính là `sum_ab[-(c+d)]`, tra cứu O(1). Cộng dồn tất cả để ra kết quả.

**Độ phức tạp:** Time O(n²), Space O(n²).

```python
"""
LeetCode #454 - 4Sum II
Nhom C - 10 bai HashSet/Dictionary
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

from collections import defaultdict


class Solution:
    def four_sum_count(
        self, nums1: list[int], nums2: list[int], nums3: list[int], nums4: list[int]
    ) -> int:
        sum_ab = defaultdict(int)
        for a in nums1:
            for b in nums2:
                sum_ab[a + b] += 1

        result = 0
        for c in nums3:
            for d in nums4:
                result += sum_ab[-(c + d)]
        return result


if __name__ == "__main__":
    sol = Solution()
    print(sol.four_sum_count([1, 2], [-2, -1], [-1, 2], [0, 2]))  # Expect: 2
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupc;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #454 - 4Sum II (Nhom C, bai 36).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class FourSumII {

    public int fourSumCount(int[] nums1, int[] nums2, int[] nums3, int[] nums4) {
        Map<Integer, Integer> countByPairSum = new HashMap<>();
        for (int a : nums1) {
            for (int b : nums2) {
                countByPairSum.merge(a + b, 1, Integer::sum);
            }
        }

        int totalQuadruplets = 0;
        for (int c : nums3) {
            for (int d : nums4) {
                totalQuadruplets += countByPairSum.getOrDefault(-(c + d), 0);
            }
        }
        return totalQuadruplets;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-37"></a>

## 37. Continuous Subarray Sum (#523) — Medium

**Đề bài:** Kiểm tra mảng có tồn tại một dãy con liên tiếp với **độ dài ít nhất 2** mà tổng của nó là bội số của `k` hay không.

**Ví dụ:**
```
Input: nums = [23,2,4,6,7], k = 6
Output: true
Giải thích: day con [2,4] co tong = 6, la boi so cua 6
```

**Hướng giải quyết:** Vẫn dựa trên **prefix sum**, nhưng lần này quan tâm tới **số dư khi chia cho `k`** thay vì giá trị tuyệt đối. Nếu hai vị trí `i < j` có `prefix_sum[i] % k == prefix_sum[j] % k`, thì tổng đoạn `[i+1..j]` chia hết cho `k`. Dùng một hash map lưu **chỉ số đầu tiên** ứng với mỗi số dư đã gặp (`remainder_index`, khởi tạo `{0: -1}` để xử lý đoạn từ đầu mảng). Nếu số dư hiện tại đã tồn tại trong map và khoảng cách chỉ số `> 1` (đảm bảo độ dài đoạn ≥ 2), trả về `True` ngay.

**Độ phức tạp:** Time O(n), Space O(min(n, k)).

```python
"""
LeetCode #523 - Continuous Subarray Sum
Nhom C - 10 bai HashSet/Dictionary
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def check_subarray_sum(self, nums: list[int], k: int) -> bool:
        remainder_index = {0: -1}
        prefix_sum = 0
        for i, num in enumerate(nums):
            prefix_sum += num
            remainder = prefix_sum % k
            if remainder in remainder_index:
                if i - remainder_index[remainder] > 1:
                    return True
            else:
                remainder_index[remainder] = i
        return False


if __name__ == "__main__":
    sol = Solution()
    print(sol.check_subarray_sum([23, 2, 4, 6, 7], 6))  # Expect: True
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupc;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #523 - Continuous Subarray Sum (Nhom C, bai 37).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class ContinuousSubarraySum {

    public boolean checkSubarraySum(int[] nums, int k) {
        Map<Integer, Integer> firstIndexByRemainder = new HashMap<>();
        firstIndexByRemainder.put(0, -1);

        int prefixSum = 0;
        for (int i = 0; i < nums.length; i++) {
            prefixSum += nums[i];
            int remainder = prefixSum % k;

            Integer firstIndex = firstIndexByRemainder.get(remainder);
            if (firstIndex != null) {
                if (i - firstIndex > 1) {
                    return true;
                }
            } else {
                firstIndexByRemainder.put(remainder, i);
            }
        }
        return false;
    }
}
```

[⬆ Về mục lục](#muc-luc)

<a id="bai-38"></a>

## 38. Design HashMap (#706) — Medium

**Đề bài:** Tự thiết kế và implement một HashMap từ đầu (không dùng `dict` có sẵn của Python), hỗ trợ `put`, `get`, `remove`.

**Ví dụ:**
```
hm = MyHashMap()
hm.put(1, 1)
hm.put(2, 2)
hm.get(1)      -> 1
hm.get(3)      -> -1 (chua ton tai)
hm.put(2, 1)   (cap nhat value cua key 2)
hm.get(2)      -> 1
hm.remove(2)
hm.get(2)      -> -1
```

**Hướng giải quyết:** Đây là bài kiểm tra **hiểu bản chất bên trong của HashMap**. Dùng kỹ thuật **separate chaining**: tạo một mảng cố định gồm `size` "bucket" (mỗi bucket là một list rỗng ban đầu). Một **hash function** đơn giản (`key % size`) sẽ quyết định key thuộc bucket nào. Trong mỗi bucket, lưu các cặp `(key, value)` dưới dạng list — khi có nhiều key rơi vào cùng 1 bucket (collision), ta duyệt tuyến tính trong list đó để tìm/cập nhật/xóa đúng key. Với `size` đủ lớn (ví dụ 1000) và dữ liệu phân bố đều, mỗi bucket chỉ chứa rất ít phần tử, giúp các phép toán gần O(1) trung bình.

**Độ phức tạp:** Time O(1) trung bình (O(n/size) trong trường hợp xấu do collision), Space O(n).

```python
"""
LeetCode #706 - Design HashMap
Nhom C - 10 bai HashSet/Dictionary
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class MyHashMap:
    def __init__(self):
        self.size = 1000
        self.buckets = [[] for _ in range(self.size)]

    def _hash(self, key: int) -> int:
        return key % self.size

    def put(self, key: int, value: int) -> None:
        bucket = self.buckets[self._hash(key)]
        for i, (k, v) in enumerate(bucket):
            if k == key:
                bucket[i] = (key, value)
                return
        bucket.append((key, value))

    def get(self, key: int) -> int:
        bucket = self.buckets[self._hash(key)]
        for k, v in bucket:
            if k == key:
                return v
        return -1

    def remove(self, key: int) -> None:
        bucket = self.buckets[self._hash(key)]
        for i, (k, v) in enumerate(bucket):
            if k == key:
                bucket.pop(i)
                return


if __name__ == "__main__":
    hm = MyHashMap()
    hm.put(1, 1)
    hm.put(2, 2)
    print(hm.get(1))  # Expect: 1
    print(hm.get(3))  # Expect: -1
    hm.put(2, 1)
    print(hm.get(2))  # Expect: 1
    hm.remove(2)
    print(hm.get(2))  # Expect: -1
```

**Lời giải Java 21:**

```java
package com.motives.leetcode.groupc;

import java.util.ArrayList;
import java.util.List;

/**
 * LeetCode #706 - Design HashMap (Nhom C, bai 38).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 *
 * <p>Separate chaining: moi bucket la mot list cac Entry (key, value).
 */
public class MyHashMap {

    private static final int BUCKET_COUNT = 1000;

    private final List<List<Entry>> buckets;

    public MyHashMap() {
        buckets = new ArrayList<>(BUCKET_COUNT);
        for (int i = 0; i < BUCKET_COUNT; i++) {
            buckets.add(new ArrayList<>());
        }
    }

    public void put(int key, int value) {
        List<Entry> bucket = bucketFor(key);
        for (int i = 0; i < bucket.size(); i++) {
            if (bucket.get(i).key() == key) {
                bucket.set(i, new Entry(key, value));
                return;
            }
        }
        bucket.add(new Entry(key, value));
    }

    public int get(int key) {
        for (Entry entry : bucketFor(key)) {
            if (entry.key() == key) {
                return entry.value();
            }
        }
        return -1;
    }

    public void remove(int key) {
        List<Entry> bucket = bucketFor(key);
        bucket.removeIf(entry -> entry.key() == key);
    }

    private List<Entry> bucketFor(int key) {
        int index = Math.floorMod(key, BUCKET_COUNT);
        return buckets.get(index);
    }

    private record Entry(int key, int value) {
    }
}
```

---

[⬆ Về mục lục](#muc-luc)

## Tổng kết pattern theo nhóm kỹ thuật

| Kỹ thuật | Bài toán áp dụng |
|---|---|
| HashMap / HashSet cơ bản | #1, #217, #242, #205, #350 |
| Prefix Sum + HashMap | #560, #523, #238 |
| Sliding Window | #3, #76 |
| Two Pointers | #15, #42, #19, #33 |
| Stack | #20 |
| Heap (Priority Queue) | #215, #253, #347 |
| DFS/BFS trên Grid | #200, #79 |
| DFS/BFS trên Tree | #102, #236, #98, #297, #105 |
| Backtracking | #79 |
| Dynamic Programming | #53, #139, #322 |
| Graph – Topological Sort | #207 |
| Binary Search | #33 |
| Design (cấu trúc dữ liệu) | #146, #706 |
| Cycle Detection bằng HashSet | #202 |
| Kết hợp HashMap cho bài toán tổ hợp | #454 |

**Gợi ý ôn tập:** Nên luyện theo đúng thứ tự nhóm A → B → C, vì nhóm A là các bài "phải biết", nhóm B mở rộng thêm pattern khó hơn (Graph, Backtracking, Sliding Window nâng cao), còn nhóm C giúp phản xạ nhanh với việc "khi nào nên nghĩ tới HashSet/Dictionary" — một trong những kỹ năng quan trọng nhất để giải nhanh các bài toán Easy/Medium trong vòng phỏng vấn 45 phút.
