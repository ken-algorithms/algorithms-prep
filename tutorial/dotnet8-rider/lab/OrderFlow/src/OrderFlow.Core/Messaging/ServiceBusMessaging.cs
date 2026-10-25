using System.ComponentModel.DataAnnotations;
using Azure.Messaging.ServiceBus;
using OrderFlow.Core.Data;

namespace OrderFlow.Core.Messaging;

public sealed class ServiceBusOptions
{
    public const string Section = "ServiceBus";

    [Required] public string OrdersQueue { get; set; } = "orders";

    [Range(1, 64)] public int MaxConcurrentCalls { get; set; } = 4;
}

public sealed class ServiceBusMessagePublisher(ServiceBusSender sender) : IMessagePublisher
{
    public Task PublishAsync(OutboxMessage message, CancellationToken ct)
    {
        var sbMessage = new ServiceBusMessage(BinaryData.FromString(message.Payload))
        {
            MessageId = message.Id.ToString(), // cung outbox id -> cung MessageId -> duplicate detection cua queue loc
            Subject = message.Type,
            ContentType = "application/json"
        };
        return sender.SendMessageAsync(sbMessage, ct);
    }
}
