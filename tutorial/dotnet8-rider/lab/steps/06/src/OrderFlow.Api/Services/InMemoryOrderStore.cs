using System.Collections.Concurrent;
using OrderFlow.Core.Domain;

namespace OrderFlow.Api.Services;

/// <summary>Kho tam cua bai 06: don nam trong RAM, tat app la mat. Bai 07 thay bang PostgreSQL.</summary>
public sealed class InMemoryOrderStore
{
    // nhieu request chay cung luc tren nhieu thread: dung ConcurrentDictionary, khong dung Dictionary thuong
    private readonly ConcurrentDictionary<Guid, Order> _orders = new();

    public void Add(Order order) => _orders[order.Id] = order;

    public Order? Find(Guid id) => _orders.GetValueOrDefault(id);

    public IEnumerable<Order> All => _orders.Values;
}
