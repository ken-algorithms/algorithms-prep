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
