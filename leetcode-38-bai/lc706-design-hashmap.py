"""
LeetCode #706 - Design HashMap
Nhom C - 10 bai HashSet/Dictionary
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class MyHashMap:
    def __init__(self):
        self.size = 1000
        self.buckets = [[] for _ in range(self.size)]

    def _hash(self, key: int) -> int:
        return key % self.size

    def put(self, key: int, value: int) -> None:
        bucket = self.buckets[self._hash(key)]
        for i, (k, v) in enumerate(bucket):
            if k == key:
                bucket[i] = (key, value)
                return
        bucket.append((key, value))

    def get(self, key: int) -> int:
        bucket = self.buckets[self._hash(key)]
        for k, v in bucket:
            if k == key:
                return v
        return -1

    def remove(self, key: int) -> None:
        bucket = self.buckets[self._hash(key)]
        for i, (k, v) in enumerate(bucket):
            if k == key:
                bucket.pop(i)
                return


if __name__ == "__main__":
    hm = MyHashMap()
    hm.put(1, 1)
    hm.put(2, 2)
    print(hm.get(1))  # Expect: 1
    print(hm.get(3))  # Expect: -1
    hm.put(2, 1)
    print(hm.get(2))  # Expect: 1
    hm.remove(2)
    print(hm.get(2))  # Expect: -1
