using System.Text.Json;
using OrderFlow.Core.Data;
using OrderFlow.Core.Messaging;

namespace OrderFlow.UnitTests;

public class OutboxMessageTests
{
    [Fact]
    public void For_serializes_the_message_as_camelCase_json_that_round_trips()
    {
        var now = new DateTime(2026, 10, 21, 9, 0, 0, DateTimeKind.Utc);
        var evt = new OrderCreated(Guid.Parse("11111111-2222-3333-4444-555555555555"), "a@b.com", 360_000m, now);

        var outbox = OutboxMessage.For(MessageTypes.OrderCreated, evt, now);

        Assert.Equal("order-created", outbox.Type);
        Assert.Equal(
            """{"orderId":"11111111-2222-3333-4444-555555555555","customerEmail":"a@b.com","total":360000,"createdAtUtc":"2026-10-21T09:00:00Z"}""",
            outbox.Payload);
        Assert.Equal(evt, JsonSerializer.Deserialize<OrderCreated>(outbox.Payload, Json.Options)); // record: so sanh theo gia tri
        Assert.Null(outbox.ProcessedAtUtc);
    }
}
