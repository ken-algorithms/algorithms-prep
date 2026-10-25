namespace DebugDrill;

public record OrderLine(string Sku, int Quantity);

public record Order(int Id, string? CouponCode, List<OrderLine> Lines);

public static class OrderGenerator
{
    private static readonly string[] Skus = ["BOOK-001", "PEN-002", "MUG-003"];

    /// <summary>Du lieu co dinh (seed 2026): moi lan chay ra cung mot bo don.</summary>
    public static List<Order> Create(int count)
    {
        var random = new Random(2026);
        var orders = new List<Order>(count);
        for (var id = 1; id <= count; id++)
        {
            var lines = Enumerable.Range(0, random.Next(1, 4))
                .Select(_ => new OrderLine(Skus[random.Next(Skus.Length)], random.Next(1, 6)))
                .ToList();
            if (id == 42) lines[0] = lines[0] with { Sku = "book-001" }; // du lieu nhap tay tu he thong cu
            if (id == 737) lines[0] = lines[0] with { Quantity = -5 };   // loi nhap lieu
            orders.Add(new Order(id, id % 4 == 0 ? "SALE10" : null, lines));
        }
        return orders;
    }
}

public static class InvoiceCalculator
{
    public static decimal Total(Order order, IReadOnlyDictionary<string, decimal> catalog)
    {
        decimal total = 0;
        foreach (var line in order.Lines)
        {
            var unitPrice = catalog[line.Sku];
            total += unitPrice * line.Quantity;
        }
        return order.CouponCode == "SALE10" ? total * 0.9m : total;
    }
}

public static class ReportWriter
{
    public static async Task WriteAsync(string path, string content)
    {
        await Task.Delay(200); // gia lap ghi len o dia / storage cham
        await File.WriteAllTextAsync(path, content);
    }
}
