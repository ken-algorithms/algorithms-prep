"""
LeetCode #5 - Longest Palindromic Substring
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def longest_palindrome(self, s: str) -> str:
        if not s:
            return ""
        start, end = 0, 0

        def expand(l: int, r: int):
            while l >= 0 and r < len(s) and s[l] == s[r]:
                l -= 1
                r += 1
            return l + 1, r - 1

        for i in range(len(s)):
            l1, r1 = expand(i, i)
            if r1 - l1 > end - start:
                start, end = l1, r1
            l2, r2 = expand(i, i + 1)
            if r2 - l2 > end - start:
                start, end = l2, r2
        return s[start : end + 1]


if __name__ == "__main__":
    sol = Solution()
    print(sol.longest_palindrome("babad"))  # Expect: "bab" hoac "aba"
