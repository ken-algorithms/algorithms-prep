"""
LeetCode #3 - Longest Substring Without Repeating Characters
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def length_of_longest_substring(self, s: str) -> int:
        last_index = {}
        left = 0
        best = 0
        for right, ch in enumerate(s):
            if ch in last_index and last_index[ch] >= left:
                left = last_index[ch] + 1
            last_index[ch] = right
            best = max(best, right - left + 1)
        return best


if __name__ == "__main__":
    sol = Solution()
    print(sol.length_of_longest_substring("abcabcbb"))  # Expect: 3
