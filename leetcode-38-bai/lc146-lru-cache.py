"""
LeetCode #146 - LRU Cache
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""

from collections import OrderedDict


class LRUCache:
    def __init__(self, capacity: int):
        self.capacity = capacity
        self.cache = OrderedDict()

    def get(self, key: int) -> int:
        if key not in self.cache:
            return -1
        self.cache.move_to_end(key)
        return self.cache[key]

    def put(self, key: int, value: int) -> None:
        if key in self.cache:
            self.cache.move_to_end(key)
        self.cache[key] = value
        if len(self.cache) > self.capacity:
            self.cache.popitem(last=False)


if __name__ == "__main__":
    cache = LRUCache(2)
    cache.put(1, 1)
    cache.put(2, 2)
    print(cache.get(1))  # Expect: 1
    cache.put(3, 3)  # evict key 2
    print(cache.get(2))  # Expect: -1
    cache.put(4, 4)  # evict key 1
    print(cache.get(1))  # Expect: -1
    print(cache.get(3))  # Expect: 3
    print(cache.get(4))  # Expect: 4
