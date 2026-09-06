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
