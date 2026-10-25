using OrderFlow.Core.Data;

namespace OrderFlow.Core.Messaging;

/// <summary>Cho OutboxProcessor khong biet gi ve Service Bus: test thay bang ban gia, doi broker chi doi class cai dat.</summary>
public interface IMessagePublisher
{
    Task PublishAsync(OutboxMessage message, CancellationToken ct);
}
