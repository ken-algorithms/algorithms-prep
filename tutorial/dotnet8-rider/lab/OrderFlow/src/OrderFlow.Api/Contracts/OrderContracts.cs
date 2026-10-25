using System.ComponentModel.DataAnnotations;
using OrderFlow.Core.Domain;

namespace OrderFlow.Api.Contracts;

// Hop dong HTTP tach rieng khoi entity: doi bang DB khong lam vo API, va client khong gui duoc Status/Id.

public sealed record CreateOrderRequest(
    [Required, EmailAddress, StringLength(256)] string CustomerEmail,
    [Required, MinLength(1), MaxLength(50)] List<CreateOrderLineRequest> Lines);

public sealed record CreateOrderLineRequest(
    [Required, StringLength(32, MinimumLength = 1)] string Sku,
    [Range(1, 1000)] int Quantity,
    [Range(1.0, 1_000_000_000.0)] decimal UnitPrice);

public sealed record OrderLineResponse(string Sku, int Quantity, decimal UnitPrice, decimal LineTotal);

public sealed record OrderResponse(
    Guid Id,
    string CustomerEmail,
    string Status,
    decimal Total,
    string? RejectReason,
    DateTime CreatedAtUtc,
    DateTime? ProcessedAtUtc,
    IReadOnlyList<OrderLineResponse> Lines)
{
    public static OrderResponse From(Order order) => new(
        order.Id,
        order.CustomerEmail,
        order.Status.ToString(),
        order.Total,
        order.RejectReason,
        order.CreatedAtUtc,
        order.ProcessedAtUtc,
        order.Lines.Select(l => new OrderLineResponse(l.Sku, l.Quantity, l.UnitPrice, l.LineTotal)).ToList());
}
