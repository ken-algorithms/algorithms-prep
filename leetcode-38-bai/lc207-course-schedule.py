"""
LeetCode #207 - Course Schedule
Nhom B - 10 bai bo sung
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

from collections import defaultdict, deque


class Solution:
    def can_finish(self, numCourses: int, prerequisites: list[list[int]]) -> bool:
        graph = defaultdict(list)
        indegree = [0] * numCourses
        for course, pre in prerequisites:
            graph[pre].append(course)
            indegree[course] += 1

        queue = deque([c for c in range(numCourses) if indegree[c] == 0])
        visited = 0
        while queue:
            node = queue.popleft()
            visited += 1
            for nxt in graph[node]:
                indegree[nxt] -= 1
                if indegree[nxt] == 0:
                    queue.append(nxt)
        return visited == numCourses


if __name__ == "__main__":
    sol = Solution()
    print(sol.can_finish(2, [[1, 0]]))  # Expect: True
    print(sol.can_finish(2, [[1, 0], [0, 1]]))  # Expect: False
