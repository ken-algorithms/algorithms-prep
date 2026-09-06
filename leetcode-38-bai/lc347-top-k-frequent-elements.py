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
