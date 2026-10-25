using System.Net;

namespace CSharpBasics;

public record OrderRow(int Id, string Customer, decimal Total, string Status);

public static class Queries
{
    private static readonly List<OrderRow> Orders =
    [
        new(1, "an", 360_000m, "Confirmed"),
        new(2, "binh", 60_000_000m, "Rejected"),
        new(3, "an", 120_000m, "Confirmed"),
        new(4, "chi", 45_000m, "Pending"),
        new(5, "binh", 900_000m, "Confirmed")
    ];

    public static Task Linq()
    {
        // LINQ ~ Java Stream: Where = filter, Select = map, Sum/Count/Any = reduce
        var confirmedTotal = Orders.Where(o => o.Status == "Confirmed").Sum(o => o.Total);
        Console.WriteLine($"confirmed total: {confirmedTotal.ToVnd()}");

        var byCustomer = Orders
            .GroupBy(o => o.Customer)
            .Select(g => new { Customer = g.Key, Count = g.Count(), Total = g.Sum(o => o.Total) }) // kieu vo danh
            .OrderByDescending(x => x.Total);
        foreach (var row in byCustomer)
            Console.WriteLine($"  {row.Customer,-5} {row.Count} order(s) {row.Total.ToVnd(),16}");

        var biggest = Orders.MaxBy(o => o.Total)!;
        Console.WriteLine($"biggest: #{biggest.Id} | any rejected: {Orders.Any(o => o.Status == "Rejected")} | " +
                          $"first pending: #{Orders.First(o => o.Status == "Pending").Id}");

        var byId = Orders.ToDictionary(o => o.Id);
        Console.WriteLine($"byId[3].Customer = {byId[3].Customer}");

        // deferred execution: dinh nghia query KHONG chay gi ca; chi chay khi duyet (First, ToList, foreach)
        var query = Orders.Where(o =>
        {
            Console.WriteLine($"  checking #{o.Id}");
            return o.Total > 500_000m;
        });
        Console.WriteLine("query defined, nothing ran yet");
        Console.WriteLine($"first big order: #{query.First().Id}");
        return Task.CompletedTask;
    }

    public static Task Errors()
    {
        try
        {
            int.Parse("abc");
        }
        catch (FormatException ex)
        {
            Console.WriteLine($"FormatException: {ex.Message}");
        }

        try
        {
            throw new HttpRequestException("gateway timeout", null, HttpStatusCode.GatewayTimeout);
        }
        catch (HttpRequestException ex) when (ex.StatusCode == HttpStatusCode.GatewayTimeout) // exception filter
        {
            Console.WriteLine("caught only because the filter matched: 504 is retryable");
        }

        Console.WriteLine(int.TryParse("42", out var n) ? $"TryParse ok: {n}" : "not a number"); // input nguoi dung: TryParse

        using (var first = new DemoResource("connection-1")) // using = try-with-resources cua Java
        {
            Console.WriteLine("  working with connection-1");
        }
        using var second = new DemoResource("connection-2");  // using declaration: Dispose o cuoi method
        Console.WriteLine("  end of the method body");
        return Task.CompletedTask;
    }

    private sealed class DemoResource : IDisposable
    {
        private readonly string _name;

        public DemoResource(string name)
        {
            _name = name;
            Console.WriteLine($"  open {name}");
        }

        public void Dispose() => Console.WriteLine($"  dispose {_name}");
    }
}
