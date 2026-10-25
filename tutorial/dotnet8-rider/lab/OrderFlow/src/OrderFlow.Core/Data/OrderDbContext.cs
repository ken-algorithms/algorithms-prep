using Microsoft.EntityFrameworkCore;
using OrderFlow.Core.Domain;

namespace OrderFlow.Core.Data;

public class OrderDbContext(DbContextOptions<OrderDbContext> options) : DbContext(options)
{
    public DbSet<Order> Orders => Set<Order>();
    public DbSet<OutboxMessage> OutboxMessages => Set<OutboxMessage>();
    public DbSet<ProcessedMessage> ProcessedMessages => Set<ProcessedMessage>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Order>(e =>
        {
            e.HasKey(o => o.Id);
            e.Property(o => o.CustomerEmail).HasMaxLength(256);
            e.Property(o => o.Status).HasConversion<string>().HasMaxLength(16); // luu "Pending", doc DB de hieu
            e.Property(o => o.Total).HasPrecision(18, 2);
            e.Property(o => o.RejectReason).HasMaxLength(512);
            e.HasIndex(o => o.CreatedAtUtc);

            e.HasMany(o => o.Lines).WithOne().HasForeignKey(l => l.OrderId).OnDelete(DeleteBehavior.Cascade);
            e.Navigation(o => o.Lines).HasField("_lines").UsePropertyAccessMode(PropertyAccessMode.Field);
        });

        modelBuilder.Entity<OrderLine>(e =>
        {
            e.ToTable("order_lines"); // khong co DbSet<OrderLine> nen EF dat ten theo class: order_line
            e.Property(l => l.Sku).HasMaxLength(32);
            e.Property(l => l.UnitPrice).HasPrecision(18, 2);
            e.Ignore(l => l.LineTotal);
        });

        modelBuilder.Entity<OutboxMessage>(e =>
        {
            e.Property(m => m.Type).HasMaxLength(128);
            e.Property(m => m.Payload).HasColumnType("jsonb");
            // index mot phan: chi chua dong CHUA gui, nen nho mai du bang outbox lon dan
            e.HasIndex(m => m.OccurredAtUtc).HasFilter("processed_at_utc IS NULL");
        });

        modelBuilder.Entity<ProcessedMessage>(e =>
        {
            e.HasKey(m => m.MessageId);
            e.Property(m => m.MessageId).HasMaxLength(128);
        });
    }
}
