using System.Text.Json;
using OrderFlow.Core.Messaging;

namespace OrderFlow.Core.Data;

/// <summary>
/// Mot dong outbox = mot message CHUA gui. Ghi cung transaction voi don hang, Worker doc ra gui len Service Bus sau.
/// Id cua dong nay dung lam MessageId tren Service Bus: gui lai bao nhieu lan cung mot Id.
/// </summary>
public class OutboxMessage
{
    public Guid Id { get; set; }
    public string Type { get; set; } = null!;
    public string Payload { get; set; } = null!;
    public DateTime OccurredAtUtc { get; set; }
    public DateTime? ProcessedAtUtc { get; set; }
    public int Attempts { get; set; }
    public string? LastError { get; set; }

    public static OutboxMessage For<T>(string type, T message, DateTime nowUtc) => new()
    {
        Id = Guid.NewGuid(),
        Type = type,
        Payload = JsonSerializer.Serialize(message, Json.Options),
        OccurredAtUtc = nowUtc
    };
}

/// <summary>Inbox: message nao da xu ly thi co mot dong o day. Khoa chinh MessageId chan xu ly hai lan.</summary>
public class ProcessedMessage
{
    public string MessageId { get; set; } = null!;
    public DateTime ProcessedAtUtc { get; set; }
}
