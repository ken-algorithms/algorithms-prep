using Microsoft.Extensions.Options;
using OrderFlow.Core.Outbox;

namespace OrderFlow.Worker;

/// <summary>
/// Background worker kieu "hen gio": moi PollIntervalMs chay mot vong outbox. Phan logic nam o OutboxProcessor
/// (Core) de test duoc ma khong can dung host; lop nay chi lo vong lap, scope DI va xu ly loi.
/// </summary>
public sealed class OutboxDispatcherWorker(
    IServiceScopeFactory scopeFactory,
    IOptions<OutboxOptions> options,
    ILogger<OutboxDispatcherWorker> logger) : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        var interval = TimeSpan.FromMilliseconds(options.Value.PollIntervalMs);
        logger.LogInformation("Outbox dispatcher started, polling every {Interval}", interval);

        using var timer = new PeriodicTimer(interval);
        try
        {
            do
            {
                await DrainAsync(stoppingToken);
            }
            while (await timer.WaitForNextTickAsync(stoppingToken));
        }
        catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
        {
            // host dang tat (Ctrl+C, docker stop): thoat vong lap binh thuong
        }
    }

    private async Task DrainAsync(CancellationToken ct)
    {
        try
        {
            int sent;
            do
            {
                // Worker nay la singleton, DbContext la scoped: moi vong tao mot scope moi, xong thi Dispose
                await using var scope = scopeFactory.CreateAsyncScope();
                var processor = scope.ServiceProvider.GetRequiredService<OutboxProcessor>();
                sent = await processor.DispatchBatchAsync(ct);
            }
            while (sent == options.Value.BatchSize); // con hang thi lam tiep ngay, khong doi tick sau
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            // KHONG de exception bay ra khoi ExecuteAsync: tu .NET 6 mac dinh la dung ca host
            logger.LogError(ex, "Outbox dispatch failed, will retry on the next tick");
        }
    }
}
