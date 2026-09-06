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
