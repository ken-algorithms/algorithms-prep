namespace OrderFlow.Core.Domain;

public enum OrderStatus
{
    Pending,
    Confirmed,
    Rejected
}

/// <summary>
/// Don hang. Setter de private: muon doi trang thai phai goi method co ten nghiep vu (Create, Process),
/// khong ai gan thang order.Status = ... tu ngoai duoc.
/// </summary>
public class Order
{
    /// <summary>Quy tac demo cho buoc "xu ly": tong tien vuot han muc thi tu choi.</summary>
    public const decimal AutoConfirmLimit = 50_000_000m;

    private readonly List<OrderLine> _lines = new();

    private Order() { } // EF Core can constructor rong khi doc tu DB

    public Guid Id { get; private set; }
    public string CustomerEmail { get; private set; } = null!;
    public OrderStatus Status { get; private set; }
    public decimal Total { get; private set; }
    public string? RejectReason { get; private set; }
    public DateTime CreatedAtUtc { get; private set; }
    public DateTime? ProcessedAtUtc { get; private set; }
    public IReadOnlyList<OrderLine> Lines => _lines;

    public static Order Create(string customerEmail, IEnumerable<OrderLine> lines, DateTime nowUtc)
    {
        if (string.IsNullOrWhiteSpace(customerEmail))
            throw new DomainException("customerEmail is required");
        if (nowUtc.Kind != DateTimeKind.Utc)
            throw new ArgumentException("nowUtc must be UTC", nameof(nowUtc)); // Npgsql chi nhan UTC cho timestamptz

        var order = new Order
        {
            Id = Guid.NewGuid(),
            CustomerEmail = customerEmail.Trim().ToLowerInvariant(),
            Status = OrderStatus.Pending,
            CreatedAtUtc = nowUtc
        };
        order._lines.AddRange(lines);
        if (order._lines.Count == 0)
            throw new DomainException("an order needs at least one line");

        // quy tac attribute [Required]/[Range] khong dien ta duoc: phai nhin ca danh sach
        var duplicate = order._lines.GroupBy(l => l.Sku).FirstOrDefault(g => g.Count() > 1);
        if (duplicate is not null)
            throw new DomainException($"sku {duplicate.Key} appears more than once, merge the quantities into one line");

        order.Total = order._lines.Sum(l => l.LineTotal);
        return order;
    }

    /// <summary>
    /// Buoc xu ly chay o Worker sau khi nhan message OrderCreated. Tra ve false neu don da xu ly roi
    /// (goi lan hai khong doi gi ca: idempotent).
    /// </summary>
    public bool Process(DateTime nowUtc)
    {
        if (Status != OrderStatus.Pending)
            return false;

        if (Total > AutoConfirmLimit)
        {
            Status = OrderStatus.Rejected;
            // Invariant: may Mac dat vung Viet Nam in N0 thanh 60.000.000, server Linux in 60,000,000
            RejectReason = FormattableString.Invariant($"total {Total:N0} is over the auto-confirm limit {AutoConfirmLimit:N0}");
        }
        else
        {
            Status = OrderStatus.Confirmed;
        }
        ProcessedAtUtc = nowUtc;
        return true;
    }
}

public class OrderLine
{
    private OrderLine() { }

    public OrderLine(string sku, int quantity, decimal unitPrice)
    {
        if (string.IsNullOrWhiteSpace(sku)) throw new DomainException("sku is required");
        if (quantity <= 0) throw new DomainException($"quantity must be > 0 (sku {sku})");
        if (unitPrice <= 0) throw new DomainException($"unitPrice must be > 0 (sku {sku})");
        Sku = sku.Trim().ToUpperInvariant();
        Quantity = quantity;
        UnitPrice = unitPrice;
    }

    public long Id { get; private set; }
    public Guid OrderId { get; private set; }
    public string Sku { get; private set; } = null!;
    public int Quantity { get; private set; }
    public decimal UnitPrice { get; private set; }
    public decimal LineTotal => Quantity * UnitPrice;
}

/// <summary>Loi nghiep vu: API tra 400 thay vi 500.</summary>
public class DomainException(string message) : Exception(message);
