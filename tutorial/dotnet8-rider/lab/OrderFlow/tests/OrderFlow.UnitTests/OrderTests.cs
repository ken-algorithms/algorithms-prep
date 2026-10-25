using OrderFlow.Core.Domain;

namespace OrderFlow.UnitTests;

public class OrderTests
{
    private static readonly DateTime Now = new(2026, 10, 21, 9, 0, 0, DateTimeKind.Utc);

    private static Order NewOrder(params OrderLine[] lines) => Order.Create("An.Nguyen@Example.com ", lines, Now);

    [Fact]
    public void Create_computes_the_total_and_normalizes_input()
    {
        var order = NewOrder(new OrderLine("book-001", 2, 150_000m), new OrderLine("pen-002", 5, 12_000m));

        Assert.Equal(360_000m, order.Total);
        Assert.Equal(OrderStatus.Pending, order.Status);
        Assert.Equal("an.nguyen@example.com", order.CustomerEmail);
        Assert.Equal(new[] { "BOOK-001", "PEN-002" }, order.Lines.Select(l => l.Sku));
        Assert.NotEqual(Guid.Empty, order.Id);
    }

    [Fact]
    public void Create_rejects_an_order_without_lines()
    {
        var ex = Assert.Throws<DomainException>(() => NewOrder());
        Assert.Contains("at least one line", ex.Message);
    }

    [Theory]
    [InlineData(0)]
    [InlineData(-3)]
    public void A_line_needs_a_positive_quantity(int quantity)
    {
        Assert.Throws<DomainException>(() => new OrderLine("BOOK-001", quantity, 10m));
    }

    [Fact]
    public void Create_rejects_the_same_sku_twice_even_with_different_case()
    {
        var ex = Assert.Throws<DomainException>(() =>
            NewOrder(new OrderLine("book-001", 1, 10m), new OrderLine("BOOK-001", 2, 10m)));
        Assert.Contains("BOOK-001 appears more than once", ex.Message);
    }

    [Fact]
    public void Create_requires_utc_time()
    {
        var local = new DateTime(2026, 10, 21, 16, 0, 0, DateTimeKind.Local);
        Assert.Throws<ArgumentException>(() => Order.Create("a@b.com", new[] { new OrderLine("X", 1, 1m) }, local));
    }

    [Fact]
    public void Process_confirms_an_order_under_the_limit()
    {
        var order = NewOrder(new OrderLine("BOOK-001", 1, 150_000m));

        Assert.True(order.Process(Now.AddSeconds(1)));
        Assert.Equal(OrderStatus.Confirmed, order.Status);
        Assert.Equal(Now.AddSeconds(1), order.ProcessedAtUtc);
        Assert.Null(order.RejectReason);
    }

    [Fact]
    public void Process_rejects_an_order_over_the_limit()
    {
        var order = NewOrder(new OrderLine("LAPTOP-PRO", 2, 30_000_000m));

        order.Process(Now);

        Assert.Equal(OrderStatus.Rejected, order.Status);
        Assert.Equal("total 60,000,000 is over the auto-confirm limit 50,000,000", order.RejectReason);
    }

    [Fact]
    public void Processing_twice_changes_nothing_the_second_time()
    {
        var order = NewOrder(new OrderLine("BOOK-001", 1, 150_000m));
        order.Process(Now);

        Assert.False(order.Process(Now.AddMinutes(5)));
        Assert.Equal(Now, order.ProcessedAtUtc);
    }
}
