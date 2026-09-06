"""
LeetCode #202 - Happy Number
Nhom C - 10 bai HashSet/Dictionary
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def is_happy(self, n: int) -> bool:
        seen = set()
        while n != 1 and n not in seen:
            seen.add(n)
            n = sum(int(d) ** 2 for d in str(n))
        return n == 1


if __name__ == "__main__":
    sol = Solution()
    print(sol.is_happy(19))  # Expect: True
    print(sol.is_happy(2))  # Expect: False
