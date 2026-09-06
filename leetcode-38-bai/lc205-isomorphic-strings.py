"""
LeetCode #205 - Isomorphic Strings
Nhom C - 10 bai HashSet/Dictionary
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def is_isomorphic(self, s: str, t: str) -> bool:
        map_st = {}
        map_ts = {}
        for cs, ct in zip(s, t):
            if cs in map_st and map_st[cs] != ct:
                return False
            if ct in map_ts and map_ts[ct] != cs:
                return False
            map_st[cs] = ct
            map_ts[ct] = cs
        return True


if __name__ == "__main__":
    sol = Solution()
    print(sol.is_isomorphic("egg", "add"))  # Expect: True
    print(sol.is_isomorphic("foo", "bar"))  # Expect: False
