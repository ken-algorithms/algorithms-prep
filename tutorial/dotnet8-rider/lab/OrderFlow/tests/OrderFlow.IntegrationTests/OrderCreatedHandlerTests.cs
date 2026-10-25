using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Time.Testing;
using OrderFlow.Core.Data;
using OrderFlow.Core.Domain;
using OrderFlow.Core.Messaging;

namespace OrderFlow.IntegrationTests;

[Collection(nameof(PostgresCollection))]
public class OrderCreatedHandlerTests(PostgresFixture pg) : IAsyncLifetime
{
    private static readonly DateTimeOffset T0 = new(2026, 10, 21, 9, 0, 0, TimeSpan.Zero);
    private readonly FakeTimeProvider _time = new(T0); // dong ho gia: thoi gian trong test co dinh, assert duoc

    public Task InitializeAsync() => pg.ResetAsync();

    public Task DisposeAsync() => Task.CompletedTask;

    private async Task<OrderCreated> SeedOrderAsync(decimal unitPrice)
    {
        await using var db = pg.CreateDbContext();
        var order = Order.Create("a@b.com", new[] { new OrderLine("BOOK-001", 1, unitPrice) }, T0.UtcDateTime);
        db.Orders.Add(order);
        await db.SaveChangesAsync();
        return new OrderCreated(order.Id, order.CustomerEmail, order.Total, order.CreatedAtUtc);
    }

    private async Task<HandleResult> HandleAsync(string messageId, OrderCreated message)
    {
        await using var db = pg.CreateDbContext(); // moi lan mot DbContext, nhu moi message mot scope trong Worker
        return await new OrderCreatedHandler(db, _time, NullLogger<OrderCreatedHandler>.Instance)
            .HandleAsync(messageId, message, default);
    }

    [Fact]
    public async Task Processes_the_order_once_and_ignores_a_redelivery()
    {
        var message = await SeedOrderAsync(150_000m);
        _time.Advance(TimeSpan.FromSeconds(2));

        Assert.Equal(HandleResult.Processed, await HandleAsync("msg-1", message));
        _time.Advance(TimeSpan.FromMinutes(1));
        Assert.Equal(HandleResult.Duplicate, await HandleAsync("msg-1", message)); // giao lai cung message

        await using var db = pg.CreateDbContext();
        var order = await db.Orders.SingleAsync();
        Assert.Equal(OrderStatus.Confirmed, order.Status);
        Assert.Equal(T0.UtcDateTime.AddSeconds(2), order.ProcessedAtUtc); // lan hai khong ghi de
        Assert.Equal(1, await db.ProcessedMessages.CountAsync());
    }

    [Fact]
    public async Task Rejects_an_order_over_the_limit()
    {
        var message = await SeedOrderAsync(60_000_000m);

        Assert.Equal(HandleResult.Processed, await HandleAsync("msg-big", message));

        await using var db = pg.CreateDbContext();
        Assert.Equal(OrderStatus.Rejected, (await db.Orders.SingleAsync()).Status);
    }

    [Fact]
    public async Task Unknown_order_is_reported_not_retried()
    {
        var message = new OrderCreated(Guid.NewGuid(), "a@b.com", 1m, T0.UtcDateTime);

        Assert.Equal(HandleResult.OrderNotFound, await HandleAsync("msg-x", message));
    }

    [Fact]
    public async Task Concurrent_redeliveries_are_processed_exactly_once()
    {
        var message = await SeedOrderAsync(150_000m);

        // stress test: tuy thoi diem, cac ban trung bi chan o AnyAsync hoac o khoa chinh; khong bao dam cham nhanh catch
        var results = await Task.WhenAll(Enumerable.Range(0, 6).Select(_ => Task.Run(() => HandleAsync("msg-race", message))));

        Assert.Equal(1, results.Count(r => r == HandleResult.Processed));
        Assert.Equal(5, results.Count(r => r == HandleResult.Duplicate));
        await using var db = pg.CreateDbContext();
        Assert.Equal(1, await db.ProcessedMessages.CountAsync());
    }

    /// <summary>Gia lap consumer khac commit cung MessageId ngay truoc SaveChanges cua handler (sau AnyAsync).</summary>
    private sealed class OtherConsumerCommitsFirst(PostgresFixture pg, string messageId) : SaveChangesInterceptor
    {
        public override async ValueTask<InterceptionResult<int>> SavingChangesAsync(
            DbContextEventData eventData, InterceptionResult<int> result, CancellationToken ct = default)
        {
            await using var other = pg.CreateDbContext(); // ket noi rieng, transaction rieng, commit ngay
            other.ProcessedMessages.Add(new ProcessedMessage { MessageId = messageId, ProcessedAtUtc = T0.UtcDateTime });
            await other.SaveChangesAsync(ct);
            return result;
        }
    }

    [Fact]
    public async Task Losing_the_race_to_another_consumer_is_a_duplicate_and_rolls_back_the_update()
    {
        var message = await SeedOrderAsync(150_000m);

        await using (var db = pg.CreateDbContext(new OtherConsumerCommitsFirst(pg, "msg-race")))
        {
            var result = await new OrderCreatedHandler(db, _time, NullLogger<OrderCreatedHandler>.Instance)
                .HandleAsync("msg-race", message, default);
            Assert.Equal(HandleResult.Duplicate, result); // nhanh catch unique violation, lan nao cung vao
        }

        await using var check = pg.CreateDbContext();
        Assert.Equal(OrderStatus.Pending, (await check.Orders.SingleAsync()).Status); // UPDATE cua handler da rollback
        Assert.Equal(1, await check.ProcessedMessages.CountAsync());                  // chi con dong cua consumer kia
    }
}
