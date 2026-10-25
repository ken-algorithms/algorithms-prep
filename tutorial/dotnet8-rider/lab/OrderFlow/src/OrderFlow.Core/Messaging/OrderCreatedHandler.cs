using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Npgsql;
using OrderFlow.Core.Data;

namespace OrderFlow.Core.Messaging;

public enum HandleResult
{
    Processed,
    Duplicate,
    OrderNotFound
}

/// <summary>
/// Xu ly OrderCreated mot cach idempotent. Service Bus giao "it nhat mot lan": cung mot message co the toi hai lan
/// (lock het han, consumer chet truoc khi Complete, outbox gui lai). Khoa chinh processed_messages.message_id
/// la thu bao dam xu ly dung mot lan; cau AnyAsync phia tren chi la duong tat.
/// </summary>
public sealed class OrderCreatedHandler(OrderDbContext db, TimeProvider time, ILogger<OrderCreatedHandler> logger)
{
    public async Task<HandleResult> HandleAsync(string messageId, OrderCreated message, CancellationToken ct)
    {
        if (await db.ProcessedMessages.AnyAsync(p => p.MessageId == messageId, ct))
            return HandleResult.Duplicate;

        var order = await db.Orders.SingleOrDefaultAsync(o => o.Id == message.OrderId, ct);
        if (order is null)
            return HandleResult.OrderNotFound;

        var now = time.GetUtcNow().UtcDateTime;
        order.Process(now);
        db.ProcessedMessages.Add(new ProcessedMessage { MessageId = messageId, ProcessedAtUtc = now });

        try
        {
            await db.SaveChangesAsync(ct); // UPDATE orders + INSERT processed_messages trong MOT transaction
        }
        catch (DbUpdateException ex) when (ex.InnerException is PostgresException { SqlState: PostgresErrorCodes.UniqueViolation })
        {
            // consumer khac vua xu ly cung message: transaction nay rollback ca phan UPDATE, coi nhu trung
            logger.LogInformation("Message {MessageId} was processed concurrently, skipping", messageId);
            return HandleResult.Duplicate;
        }

        logger.LogInformation("Order {OrderId} processed: {Status}", order.Id, order.Status);
        return HandleResult.Processed;
    }
}
