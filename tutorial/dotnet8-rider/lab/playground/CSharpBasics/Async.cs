using System.Diagnostics;

namespace CSharpBasics;

public static class Async
{
    public static async Task Run()
    {
        var sw = Stopwatch.StartNew();
        string[] skus = ["BOOK-001", "PEN-002", "MUG-03"];

        // goi lan luot: tong ~ 3 x 300 ms
        foreach (var sku in skus)
            await GetStockAsync(sku, CancellationToken.None);
        Console.WriteLine($"sequential: ~{Round(sw.ElapsedMilliseconds)} ms");

        // goi song song: tao ca 3 Task truoc roi moi await -> tong ~ 300 ms
        sw.Restart();
        int[] stocks = await Task.WhenAll(skus.Select(sku => GetStockAsync(sku, CancellationToken.None)));
        Console.WriteLine($"parallel:   ~{Round(sw.ElapsedMilliseconds)} ms, stocks = {string.Join(", ", stocks)}");

        // huy: CancellationToken di xuyen moi tang (controller -> service -> EF Core -> Npgsql)
        using var cts = new CancellationTokenSource(TimeSpan.FromMilliseconds(100));
        try
        {
            await GetStockAsync("SLOW-SKU", cts.Token);
        }
        catch (OperationCanceledException)
        {
            Console.WriteLine("cancelled after ~100 ms instead of waiting 300 ms");
        }

        static long Round(long ms) => (ms + 50) / 100 * 100;
    }

    private static async Task<int> GetStockAsync(string sku, CancellationToken ct)
    {
        await Task.Delay(300, ct); // gia lap goi DB/HTTP; trong luc cho, thread duoc tra lai thread pool
        return sku.Length;
    }

    public static Task Generics()
    {
        // generic method + rang buoc: T phai so sanh duoc
        Console.WriteLine($"Max(3, 9, 4) = {Max(3, 9, 4)} | Max(\"kiwi\", \"apple\") = {Max("kiwi", "apple")}");

        // delegate co san: Func<vao, ra> va Action<vao> ~ Function/Consumer cua Java
        Func<decimal, decimal> withVat = amount => amount * 1.08m;
        Action<string> log = message => Console.WriteLine($"[log] {message}");
        log($"price with 8% VAT: {withVat(100_000m)}");

        var cache = new Cache<string, decimal>();
        Console.WriteLine($"first call: {cache.GetOrAdd("BOOK-001", _ => 150_000m)}, cached: {cache.Count}");
        return Task.CompletedTask;

        static T Max<T>(params T[] items) where T : IComparable<T>
        {
            var best = items[0];
            foreach (var item in items)
                if (item.CompareTo(best) > 0)
                    best = item;
            return best;
        }
    }

    private sealed class Cache<TKey, TValue> where TKey : notnull
    {
        private readonly Dictionary<TKey, TValue> _items = new();

        public int Count => _items.Count;

        public TValue GetOrAdd(TKey key, Func<TKey, TValue> factory)
        {
            if (!_items.TryGetValue(key, out var value))
                _items[key] = value = factory(key);
            return value;
        }
    }
}
