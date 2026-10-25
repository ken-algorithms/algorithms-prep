// Bai 04: chuong trinh nay CO 5 LOI cai san. Khong doc code de tim: dung debugger cua Rider.
// Ket qua dung (sau khi sua het) ghi trong bai 04, muc "Dap an".
using System.Globalization;
using DebugDrill;

CultureInfo.DefaultThreadCurrentCulture = CultureInfo.InvariantCulture; // in so giong nhau tren moi may

var catalog = new Dictionary<string, decimal>
{
    ["BOOK-001"] = 150_000m,
    ["PEN-002"] = 12_000m,
    ["MUG-003"] = 99_000m
};

List<Order> orders = OrderGenerator.Create(count: 1000);
Console.WriteLine($"Loaded {orders.Count} orders");

decimal revenue = 0;
for (var i = 1; i < orders.Count; i++)
{
    revenue += InvoiceCalculator.Total(orders[i], catalog);
}
Console.WriteLine($"Revenue: {revenue:N0}");

var withCoupon = orders.Count(o => o.CouponCode is not null);
var percent = withCoupon / orders.Count * 100;
Console.WriteLine($"Orders with a coupon: {withCoupon} ({percent}%)");

ReportWriter.WriteAsync("report.txt", $"revenue={revenue}");
Console.WriteLine("Report written to report.txt");
