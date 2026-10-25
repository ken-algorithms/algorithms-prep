using OrderFlow.Api.Contracts;
using OrderFlow.Core.Domain;

namespace OrderFlow.Api.Services;

// Ban cua bai 06: luu trong InMemoryOrderStore. Bai 07 doi sang EF Core nhung giu nguyen 3 method,
// nen OrdersController khong phai sua dong nao.
public sealed class OrderService(InMemoryOrderStore store, TimeProvider time, ILogger<OrderService> logger)
{
    public Task<OrderResponse> CreateAsync(CreateOrderRequest request, CancellationToken ct)
    {
        var order = Order.Create(
            request.CustomerEmail,
            request.Lines.Select(l => new OrderLine(l.Sku, l.Quantity, l.UnitPrice)),
            time.GetUtcNow().UtcDateTime);
        store.Add(order);

        logger.LogInformation("Order {OrderId} created for {Email}, total {Total}", order.Id, order.CustomerEmail, order.Total);
        return Task.FromResult(OrderResponse.From(order));
    }

    public Task<OrderResponse?> GetAsync(Guid id, CancellationToken ct)
    {
        var order = store.Find(id);
        return Task.FromResult<OrderResponse?>(order is null ? null : OrderResponse.From(order));
    }

    public Task<IReadOnlyList<OrderResponse>> ListAsync(OrderStatus? status, int take, CancellationToken ct)
    {
        IReadOnlyList<OrderResponse> result = store.All
            .Where(o => status is null || o.Status == status)
            .OrderByDescending(o => o.CreatedAtUtc)
            .Take(take)
            .Select(OrderResponse.From)
            .ToList();
        return Task.FromResult(result);
    }
}
