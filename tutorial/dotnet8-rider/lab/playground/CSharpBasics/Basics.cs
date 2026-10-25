using System.Globalization;

namespace CSharpBasics;

public static class Basics
{
    public static Task Types()
    {
        int count = 3;                 // System.Int32, giong int cua Java
        long big = 3_000_000_000;      // dau _ chi de doc cho de
        decimal price = 150_000m;      // tien LUON dung decimal (nhu BigDecimal), hau to m
        double ratio = 0.1 + 0.2;      // double: nhanh nhung sai so nhi phan
        var name = "OrderFlow";        // var: compiler suy ra string, van la kieu tinh
        Console.WriteLine($"count={count}, big={big}, price={price}, name={name}");
        Console.WriteLine($"0.1 + 0.2 as double  = {ratio:R}");
        Console.WriteLine($"0.1 + 0.2 as decimal = {0.1m + 0.2m}");

        string a = "hello";
        string b = string.Concat("hel", "lo"); // tao chuoi moi luc chay
        Console.WriteLine($"a == b -> {a == b}  (C# so sanh NOI DUNG chuoi bang ==, Java phai dung equals)");

        var json = """{"sku": "BOOK-001", "qty": 2}"""; // raw string (C# 11): khong can escape dau "
        Console.WriteLine(json);

        var vi = CultureInfo.GetCultureInfo("vi-VN");
        Console.WriteLine($"vi-VN N0: {360_000m.ToString("N0", vi)} | C0: {360_000m.ToString("C0", vi)} | invariant: {360_000m.ToString("N0", CultureInfo.InvariantCulture)}");
        return Task.CompletedTask;
    }

    public static Task Nulls()
    {
        string? maybe = FindEmail(42);                  // string? = co the null; string = compiler hieu la khong null
        Console.WriteLine($"maybe?.Length -> {maybe?.Length.ToString() ?? "null"}"); // ?. : null thi ca bieu thuc la null
        Console.WriteLine(maybe ?? "(no email)");      // ?? : gia tri thay the khi null
        maybe ??= "default@example.com";               // ??= : chi gan khi dang null
        Console.WriteLine(maybe);

        int? quantity = null;                          // kieu gia tri co the null: Nullable<int>
        Console.WriteLine($"HasValue={quantity.HasValue}, GetValueOrDefault(1)={quantity.GetValueOrDefault(1)}");

        if (FindEmail(1) is { } email)                 // khong null thi gan vao bien email
            Console.WriteLine($"found {email}");
        return Task.CompletedTask;

        static string? FindEmail(int id) => id == 1 ? "an@example.com" : null;
    }

    public static Task Collections()
    {
        int[] numbers = [5, 3, 8, 1];                  // collection expression (C# 12)
        List<string> skus = ["BOOK-001", "PEN-002"];
        skus.Add("MUG-003");
        Console.WriteLine($"numbers: {string.Join(", ", numbers)} | skus: {string.Join(", ", skus)} (Count={skus.Count})");

        var stock = new Dictionary<string, int>(StringComparer.OrdinalIgnoreCase) // khoa khong phan biet hoa thuong
        {
            ["BOOK-001"] = 10,
            ["PEN-002"] = 0
        };
        Console.WriteLine($"stock[\"book-001\"] = {stock["book-001"]}");
        if (!stock.TryGetValue("MUG-003", out var mugs))   // TryGetValue: khong nem KeyNotFoundException
            Console.WriteLine($"MUG-003 not in stock (mugs={mugs})");

        foreach (var (sku, qty) in stock)              // tach KeyValuePair thanh hai bien
            Console.WriteLine($"  {sku}: {qty}");

        var unique = new HashSet<string> { "a", "b", "a" };
        int[] sorted = [.. numbers.Order()];           // .. trai cac phan tu vao mang moi
        Console.WriteLine($"HashSet count={unique.Count} | sorted: {string.Join(", ", sorted)}");
        return Task.CompletedTask;
    }
}
