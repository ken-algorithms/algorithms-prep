"""
LeetCode #105 - Construct Binary Tree from Preorder and Inorder Traversal
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right


class Solution:
    def build_tree(self, preorder: list[int], inorder: list[int]) -> TreeNode | None:
        index_map = {val: i for i, val in enumerate(inorder)}
        self.pre_idx = 0

        def build(left: int, right: int) -> TreeNode | None:
            if left > right:
                return None
            root_val = preorder[self.pre_idx]
            self.pre_idx += 1
            root = TreeNode(root_val)
            mid = index_map[root_val]
            root.left = build(left, mid - 1)
            root.right = build(mid + 1, right)
            return root

        return build(0, len(inorder) - 1)


if __name__ == "__main__":

    def level_order(root):
        if not root:
            return []
        result, queue = [], [root]
        while queue:
            node = queue.pop(0)
            result.append(node.val)
            if node.left:
                queue.append(node.left)
            if node.right:
                queue.append(node.right)
        return result

    sol = Solution()
    tree = sol.build_tree([3, 9, 20, 15, 7], [9, 3, 15, 20, 7])
    print(level_order(tree))  # Expect: [3, 9, 20, 15, 7]
