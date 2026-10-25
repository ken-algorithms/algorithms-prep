using System.ComponentModel.DataAnnotations;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using OrderFlow.Core.Data;
using OrderFlow.Core.Messaging;

namespace OrderFlow.Core.Outbox;

public sealed class OutboxOptions
{
    public const string Section = "Outbox";

    [Range(1, 1000)] public int BatchSize { get; set; } = 50;

    [Range(100, 60_000)] public int PollIntervalMs { get; set; } = 1000;
}

/// <summary>
/// Mot vong outbox: khoa mot lo dong chua gui (FOR UPDATE SKIP LOCKED), gui tung dong, danh dau da gui, commit.
/// Chay nhieu Worker song song cung duoc: SKIP LOCKED chia viec, khong ban nao lay trung dong cua ban khac.
/// Chet giua "gui" va "commit" thi lan sau gui lai: consumer phai idempotent (xem OrderCreatedHandler).
/// </summary>
public sealed class OutboxProcessor(
    OrderDbContext db,
    IMessagePublisher publisher,
    TimeProvider time,
    IOptions<OutboxOptions> options,
    ILogger<OutboxProcessor> logger)
{
    /// <returns>So message da gui thanh cong trong vong nay.</returns>
    public async Task<int> DispatchBatchAsync(CancellationToken ct)
    {
        var batchSize = options.Value.BatchSize;
        await using var tx = await db.Database.BeginTransactionAsync(ct);

        // FromSql + chuoi noi suy: {batchSize} thanh tham so @p0, khong phai noi chuoi -> khong SQL injection
        var batch = await db.OutboxMessages
            .FromSql($"""
                SELECT * FROM outbox_messages
                WHERE processed_at_utc IS NULL
                ORDER BY occurred_at_utc
                LIMIT {batchSize}
                FOR UPDATE SKIP LOCKED
                """)
            .ToListAsync(ct);

        var sent = 0;
        foreach (var message in batch)
        {
            try
            {
                await publisher.PublishAsync(message, ct);
                message.ProcessedAtUtc = time.GetUtcNow().UtcDateTime;
                sent++;
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                message.Attempts++;
                message.LastError = ex.Message.Length > 2000 ? ex.Message[..2000] : ex.Message;
                logger.LogWarning(ex, "Publish failed for outbox message {MessageId} (attempt {Attempts})",
                    message.Id, message.Attempts);
                break; // broker dang loi: dung lo nay, vong sau thu lai, khong dap tiep vao broker
            }
        }

        await db.SaveChangesAsync(ct);
        await tx.CommitAsync(ct);

        if (sent > 0)
            logger.LogInformation("Outbox: sent {Sent}/{Batch} message(s)", sent, batch.Count);
        return sent;
    }
}
