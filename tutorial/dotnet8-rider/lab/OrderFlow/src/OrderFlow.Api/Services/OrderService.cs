using Microsoft.EntityFrameworkCore;
using OrderFlow.Api.Contracts;
using OrderFlow.Core.Data;
using OrderFlow.Core.Domain;
using OrderFlow.Core.Messaging;

namespace OrderFlow.Api.Services;

public sealed class OrderService(OrderDbContext db, TimeProvider time, ILogger<OrderService> logger)
{
    public async Task<OrderResponse> CreateAsync(CreateOrderRequest request, CancellationToken ct)
    {
        var now = time.GetUtcNow().UtcDateTime;
        var order = Order.Create(
            request.CustomerEmail,
            request.Lines.Select(l => new OrderLine(l.Sku, l.Quantity, l.UnitPrice)),
            now);

        db.Orders.Add(order);
        db.OutboxMessages.Add(OutboxMessage.For(
            MessageTypes.OrderCreated,
            new OrderCreated(order.Id, order.CustomerEmail, order.Total, now),
            now));

        // INSERT orders + order_lines + outbox_messages trong MOT transaction: co don thi chac chan co message
        await db.SaveChangesAsync(ct);

        logger.LogInformation("Order {OrderId} created for {Email}, total {Total}", order.Id, order.CustomerEmail, order.Total);
        return OrderResponse.From(order);
    }

    public async Task<OrderResponse?> GetAsync(Guid id, CancellationToken ct)
    {
        var order = await db.Orders
            .AsNoTracking() // chi doc: khong can change tracking, nhanh hon va it bo nho hon
            .Include(o => o.Lines)
            .SingleOrDefaultAsync(o => o.Id == id, ct);
        return order is null ? null : OrderResponse.From(order);
    }

    public async Task<IReadOnlyList<OrderResponse>> ListAsync(OrderStatus? status, int take, CancellationToken ct)
    {
        IQueryable<Order> query = db.Orders.AsNoTracking().Include(o => o.Lines);
        if (status is not null)
            query = query.Where(o => o.Status == status);

        var orders = await query
            .OrderByDescending(o => o.CreatedAtUtc)
            .Take(take)
            .ToListAsync(ct);
        return orders.Select(OrderResponse.From).ToList();
    }
}
