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
