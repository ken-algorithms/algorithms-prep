"""
LeetCode #98 - Validate Binary Search Tree
Nhom B - 10 bai bo sung
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right


class Solution:
    def is_valid_bst(self, root) -> bool:
        def validate(node, low: float, high: float) -> bool:
            if not node:
                return True
            if not (low < node.val < high):
                return False
            return validate(node.left, low, node.val) and validate(
                node.right, node.val, high
            )

        return validate(root, float("-inf"), float("inf"))


if __name__ == "__main__":
    root_valid = TreeNode(2, TreeNode(1), TreeNode(3))
    sol = Solution()
    print(sol.is_valid_bst(root_valid))  # Expect: True

    root_invalid = TreeNode(5, TreeNode(1), TreeNode(4, TreeNode(3), TreeNode(6)))
    print(sol.is_valid_bst(root_invalid))  # Expect: False
