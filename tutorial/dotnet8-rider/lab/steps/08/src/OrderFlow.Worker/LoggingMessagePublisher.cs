using OrderFlow.Core.Data;
using OrderFlow.Core.Messaging;

namespace OrderFlow.Worker;

/// <summary>Publisher tam cua bai 08: chi ghi log, chua gui di dau. Bai 09 xoa file nay, dung ServiceBusMessagePublisher.</summary>
public sealed class LoggingMessagePublisher(ILogger<LoggingMessagePublisher> logger) : IMessagePublisher
{
    public Task PublishAsync(OutboxMessage message, CancellationToken ct)
    {
        logger.LogInformation("PUBLISH {Type} {MessageId}: {Payload}", message.Type, message.Id, message.Payload);
        return Task.CompletedTask;
    }
}
