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
