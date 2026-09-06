"""
LeetCode #20 - Valid Parentheses
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def is_valid(self, s: str) -> bool:
        stack = []
        pairs = {")": "(", "]": "[", "}": "{"}
        for ch in s:
            if ch in pairs:
                if not stack or stack.pop() != pairs[ch]:
                    return False
            else:
                stack.append(ch)
        return not stack


if __name__ == "__main__":
    sol = Solution()
    print(sol.is_valid("()[]{}"))  # Expect: True
    print(sol.is_valid("(]"))  # Expect: False
