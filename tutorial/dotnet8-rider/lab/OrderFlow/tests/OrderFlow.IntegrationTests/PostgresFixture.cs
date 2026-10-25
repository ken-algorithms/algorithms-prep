using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using OrderFlow.Core.Data;
using Testcontainers.PostgreSql;

namespace OrderFlow.IntegrationTests;

/// <summary>
/// Mot container PostgreSQL 16 THAT cho ca bo test (Testcontainers tu chay va tu xoa, chi can Docker dang chay).
/// Ap dung dung migration cua app: test ca migration, khong dung EnsureCreated.
/// </summary>
public sealed class PostgresFixture : IAsyncLifetime
{
    private readonly PostgreSqlContainer _container = new PostgreSqlBuilder("postgres:16-alpine").Build();

    public string ConnectionString => _container.GetConnectionString();

    public async Task InitializeAsync()
    {
        await _container.StartAsync();
        await using var db = CreateDbContext();
        await db.Database.MigrateAsync();
    }

    public Task DisposeAsync() => _container.DisposeAsync().AsTask();

    public OrderDbContext CreateDbContext(params IInterceptor[] interceptors) => new(new DbContextOptionsBuilder<OrderDbContext>()
        .UseNpgsql(ConnectionString)
        .UseSnakeCaseNamingConvention()
        .AddInterceptors(interceptors) // test chen hanh vi vao giua SaveChanges (OrderCreatedHandlerTests)
        .Options);

    /// <summary>Moi test bat dau tu bang rong.</summary>
    public async Task ResetAsync()
    {
        await using var db = CreateDbContext();
        await db.Database.ExecuteSqlRawAsync("TRUNCATE orders, order_lines, outbox_messages, processed_messages");
    }
}

[CollectionDefinition(nameof(PostgresCollection))]
public class PostgresCollection : ICollectionFixture<PostgresFixture>;
