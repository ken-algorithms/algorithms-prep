using System.Collections.Concurrent;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;
using OrderFlow.Core.Data;
using OrderFlow.Core.Messaging;
using OrderFlow.Core.Outbox;

namespace OrderFlow.IntegrationTests;

[Collection(nameof(PostgresCollection))]
public class OutboxProcessorTests(PostgresFixture pg) : IAsyncLifetime
{
    public Task InitializeAsync() => pg.ResetAsync();

    public Task DisposeAsync() => Task.CompletedTask;

    /// <summary>Publisher gia: ghi lai message da "gui"; tuy chon cham hoac loi.</summary>
    private sealed class RecordingPublisher(TimeSpan delay = default, int failFirst = 0) : IMessagePublisher
    {
        private int _calls;
        public ConcurrentQueue<Guid> Sent { get; } = new();

        public async Task PublishAsync(OutboxMessage message, CancellationToken ct)
        {
            if (Interlocked.Increment(ref _calls) <= failFirst)
                throw new InvalidOperationException("broker unavailable");
            if (delay > TimeSpan.Zero)
                await Task.Delay(delay, ct);
            Sent.Enqueue(message.Id);
        }
    }

    private OutboxProcessor NewProcessor(OrderDbContext db, IMessagePublisher publisher, int batchSize = 50) =>
        new(db, publisher, TimeProvider.System, Options.Create(new OutboxOptions { BatchSize = batchSize }),
            NullLogger<OutboxProcessor>.Instance);

    private async Task<List<Guid>> SeedAsync(int count)
    {
        await using var db = pg.CreateDbContext();
        var start = new DateTime(2026, 10, 21, 9, 0, 0, DateTimeKind.Utc);
        var rows = Enumerable.Range(0, count)
            .Select(i => OutboxMessage.For("test", new { n = i }, start.AddSeconds(i)))
            .ToList();
        db.OutboxMessages.AddRange(rows);
        await db.SaveChangesAsync();
        return rows.Select(r => r.Id).ToList();
    }

    [Fact]
    public async Task Sends_pending_messages_in_order_and_only_once()
    {
        var ids = await SeedAsync(3);
        var publisher = new RecordingPublisher();

        await using (var db = pg.CreateDbContext())
            Assert.Equal(3, await NewProcessor(db, publisher).DispatchBatchAsync(default));
        await using (var db = pg.CreateDbContext())
            Assert.Equal(0, await NewProcessor(db, publisher).DispatchBatchAsync(default)); // vong sau: khong con gi

        Assert.Equal(ids, publisher.Sent.ToList());
        await using var check = pg.CreateDbContext();
        Assert.True(await check.OutboxMessages.AllAsync(m => m.ProcessedAtUtc != null));
    }

    [Fact]
    public async Task A_failed_publish_keeps_the_message_and_records_the_error()
    {
        await SeedAsync(1);

        await using (var db = pg.CreateDbContext())
            Assert.Equal(0, await NewProcessor(db, new RecordingPublisher(failFirst: 1)).DispatchBatchAsync(default));

        await using (var db = pg.CreateDbContext())
        {
            var row = await db.OutboxMessages.SingleAsync();
            Assert.Null(row.ProcessedAtUtc);
            Assert.Equal(1, row.Attempts);
            Assert.Equal("broker unavailable", row.LastError);
        }

        await using (var db = pg.CreateDbContext())
            Assert.Equal(1, await NewProcessor(db, new RecordingPublisher()).DispatchBatchAsync(default)); // broker song lai
    }

    [Fact]
    public async Task Two_dispatchers_in_parallel_never_send_the_same_message()
    {
        var ids = await SeedAsync(40);
        var publisher = new RecordingPublisher(delay: TimeSpan.FromMilliseconds(15)); // giu khoa lau de hai ben chong lan

        async Task<int> RunOne()
        {
            var total = 0;
            int sent;
            do
            {
                await using var db = pg.CreateDbContext();
                sent = await NewProcessor(db, publisher, batchSize: 5).DispatchBatchAsync(default);
                total += sent;
            } while (sent > 0);
            return total;
        }

        var results = await Task.WhenAll(RunOne(), RunOne());

        Assert.Equal(40, results.Sum());
        Assert.Equal(40, publisher.Sent.Count);                     // tong so lan gui
        Assert.Equal(ids.Order(), publisher.Sent.Distinct().Order()); // khong message nao gui hai lan
        Assert.All(results, r => Assert.True(r > 0, "both dispatchers should get work"));
    }
}
