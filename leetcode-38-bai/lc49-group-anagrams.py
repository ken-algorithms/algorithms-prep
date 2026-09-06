"""
LeetCode #49 - Group Anagrams
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

from collections import defaultdict


class Solution:
    def group_anagrams(self, strs: list[str]) -> list[list[str]]:
        groups = defaultdict(list)
        for s in strs:
            key = "".join(sorted(s))
            groups[key].append(s)
        return list(groups.values())


if __name__ == "__main__":
    sol = Solution()
    print(sol.group_anagrams(["eat", "tea", "tan", "ate", "nat", "bat"]))
    # Expect nhom: ['eat','tea','ate'], ['tan','nat'], ['bat']
