using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using OrderFlow.Api.Contracts;
using OrderFlow.Core.Messaging;

namespace OrderFlow.IntegrationTests;

/// <summary>Chay API that trong bo nho (WebApplicationFactory), goi HTTP that, DB la PostgreSQL that.</summary>
[Collection(nameof(PostgresCollection))]
public class OrdersApiTests(PostgresFixture pg) : IAsyncLifetime
{
    private WebApplicationFactory<Program> _factory = null!;
    private HttpClient _client = null!;

    public async Task InitializeAsync()
    {
        await pg.ResetAsync();
        _factory = new WebApplicationFactory<Program>().WithWebHostBuilder(host =>
            host.UseSetting("ConnectionStrings:Postgres", pg.ConnectionString)); // tro API vao container test
        _client = _factory.CreateClient();
    }

    public async Task DisposeAsync()
    {
        _client.Dispose();
        await _factory.DisposeAsync();
    }

    private static object ValidOrder(string email = "An.Nguyen@Example.com") => new
    {
        customerEmail = email,
        lines = new[] { new { sku = "book-001", quantity = 2, unitPrice = 150_000 } }
    };

    [Fact]
    public async Task Post_saves_the_order_and_its_outbox_message_together()
    {
        var response = await _client.PostAsJsonAsync("/api/orders", ValidOrder());

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        var order = await response.Content.ReadFromJsonAsync<OrderResponse>();
        Assert.NotNull(order);
        Assert.Equal("Pending", order.Status);
        Assert.Equal(300_000m, order.Total);
        Assert.Equal("an.nguyen@example.com", order.CustomerEmail);
        Assert.Equal($"/api/orders/{order.Id}", response.Headers.Location?.AbsolutePath);

        await using var db = pg.CreateDbContext();
        var outbox = await db.OutboxMessages.SingleAsync();
        Assert.Equal(MessageTypes.OrderCreated, outbox.Type);
        Assert.Null(outbox.ProcessedAtUtc);
        Assert.Equal(order.Id, JsonSerializer.Deserialize<OrderCreated>(outbox.Payload, Json.Options)!.OrderId);
    }

    [Fact]
    public async Task Get_returns_what_post_created()
    {
        var created = await (await _client.PostAsJsonAsync("/api/orders", ValidOrder())).Content.ReadFromJsonAsync<OrderResponse>();

        var order = await _client.GetFromJsonAsync<OrderResponse>($"/api/orders/{created!.Id}");

        Assert.Equal(created.Id, order!.Id);
        Assert.Equal("BOOK-001", Assert.Single(order.Lines).Sku);
    }

    [Fact]
    public async Task Invalid_body_returns_400_validation_problem_and_writes_nothing()
    {
        var response = await _client.PostAsJsonAsync("/api/orders", new { customerEmail = "not-an-email", lines = Array.Empty<object>() });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        var problem = await response.Content.ReadFromJsonAsync<ValidationProblemDetails>();
        Assert.Contains("CustomerEmail", problem!.Errors.Keys);
        Assert.Contains("Lines", problem.Errors.Keys);

        await using var db = pg.CreateDbContext();
        Assert.Equal(0, await db.Orders.CountAsync());
        Assert.Equal(0, await db.OutboxMessages.CountAsync());
    }

    [Fact]
    public async Task Domain_rule_violation_returns_400_problem_from_the_exception_handler()
    {
        var response = await _client.PostAsJsonAsync("/api/orders", new
        {
            customerEmail = "a@b.com",
            lines = new[] { new { sku = "book-001", quantity = 1, unitPrice = 10 }, new { sku = "BOOK-001", quantity = 2, unitPrice = 10 } }
        });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        Assert.Equal("application/problem+json", response.Content.Headers.ContentType?.MediaType);
        var problem = await response.Content.ReadFromJsonAsync<ProblemDetails>();
        Assert.Equal("The order is not valid", problem!.Title);
        Assert.Contains("BOOK-001 appears more than once", problem.Detail);
    }

    [Fact]
    public async Task Unknown_id_returns_404_problem()
    {
        var response = await _client.GetAsync($"/api/orders/{Guid.NewGuid()}");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        Assert.Equal("application/problem+json", response.Content.Headers.ContentType?.MediaType);
    }

    [Fact]
    public async Task Health_check_reaches_postgres()
    {
        var response = await _client.GetAsync("/health");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal("Healthy", await response.Content.ReadAsStringAsync());
    }
}
