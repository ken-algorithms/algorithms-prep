"""
LeetCode #76 - Minimum Window Substring
Nhom B - 10 bai bo sung
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

from collections import Counter


class Solution:
    def min_window(self, s: str, t: str) -> str:
        if not s or not t:
            return ""
        need = Counter(t)
        missing = len(t)
        left = 0
        best_left, best_right = 0, 0
        for right, ch in enumerate(s, 1):
            if need[ch] > 0:
                missing -= 1
            need[ch] -= 1
            if missing == 0:
                while need[s[left]] < 0:
                    need[s[left]] += 1
                    left += 1
                if best_right == 0 or right - left < best_right - best_left:
                    best_left, best_right = left, right
                need[s[left]] += 1
                missing += 1
                left += 1
        return s[best_left:best_right]


if __name__ == "__main__":
    sol = Solution()
    print(sol.min_window("ADOBECODEBANC", "ABC"))  # Expect: "BANC"
