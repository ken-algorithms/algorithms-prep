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
