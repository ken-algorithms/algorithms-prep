"""
LeetCode #56 - Merge Intervals
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def merge(self, intervals: list[list[int]]) -> list[list[int]]:
        intervals.sort(key=lambda x: x[0])
        merged = []
        for interval in intervals:
            if merged and interval[0] <= merged[-1][1]:
                merged[-1][1] = max(merged[-1][1], interval[1])
            else:
                merged.append(interval)
        return merged


if __name__ == "__main__":
    sol = Solution()
    print(
        sol.merge([[1, 3], [2, 6], [8, 10], [15, 18]])
    )  # Expect: [[1, 6], [8, 10], [15, 18]]
