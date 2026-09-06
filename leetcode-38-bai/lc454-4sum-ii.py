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
