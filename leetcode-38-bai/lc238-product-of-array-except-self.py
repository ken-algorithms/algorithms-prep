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
