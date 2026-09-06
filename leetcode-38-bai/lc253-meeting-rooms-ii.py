"""
LeetCode #253 - Meeting Rooms II
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

import heapq


class Solution:
    def min_meeting_rooms(self, intervals: list[list[int]]) -> int:
        if not intervals:
            return 0
        intervals.sort(key=lambda x: x[0])
        heap: list[int] = []  # end times của các cuộc họp đang diễn ra
        for start, end in intervals:
            if heap and heap[0] <= start:
                heapq.heapreplace(heap, end)
            else:
                heapq.heappush(heap, end)
        return len(heap)


if __name__ == "__main__":
    sol = Solution()
    print(sol.min_meeting_rooms([[0, 30], [5, 10], [15, 20]]))  # Expect: 2
