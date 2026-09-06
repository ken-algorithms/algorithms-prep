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
