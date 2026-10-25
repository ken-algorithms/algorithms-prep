using System.Text.Json;

namespace OrderFlow.Core.Messaging;

/// <summary>Su kien API phat ra khi tao don. Record: bat bien, so sanh theo gia tri, serialize gon.</summary>
public sealed record OrderCreated(Guid OrderId, string CustomerEmail, decimal Total, DateTime CreatedAtUtc);

public static class MessageTypes
{
    public const string OrderCreated = "order-created";
}

public static class Json
{
    /// <summary>camelCase giong ASP.NET Core, de payload trong outbox, Service Bus va API cung mot kieu.</summary>
    public static readonly JsonSerializerOptions Options = new(JsonSerializerDefaults.Web);
}
