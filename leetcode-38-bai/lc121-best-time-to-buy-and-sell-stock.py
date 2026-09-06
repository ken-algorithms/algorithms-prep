"""
LeetCode #121 - Best Time to Buy and Sell Stock
Nhom A - 18 bai nen tang
Xem mo ta huong giai chi tiet bang loi tai: ../leetcode-38-bai-phong-van-vietnam.md
"""


class Solution:
    def max_profit(self, prices: list[int]) -> int:
        min_price = float("inf")
        max_profit = 0
        for price in prices:
            min_price = min(min_price, price)
            max_profit = max(max_profit, price - min_price)
        return max_profit


if __name__ == "__main__":
    sol = Solution()
    print(sol.max_profit([7, 1, 5, 3, 6, 4]))  # Expect: 5
