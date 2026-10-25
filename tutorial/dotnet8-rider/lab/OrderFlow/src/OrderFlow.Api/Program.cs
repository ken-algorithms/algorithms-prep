using OrderFlow.Api.Infrastructure;
using OrderFlow.Api.Services;
using OrderFlow.Core;
using OrderFlow.Core.Data;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

// ---- 1. Dang ky dich vu (DI container) ----
builder.Services.AddOrderFlowData(builder.Configuration); // DbContext (scoped) + TimeProvider (singleton)
builder.Services.AddScoped<OrderService>();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

builder.Services.AddProblemDetails(); // moi loi tra ve dang application/problem+json (RFC 7807)
builder.Services.AddExceptionHandler<DomainExceptionHandler>();
builder.Services.AddHealthChecks().AddDbContextCheck<OrderDbContext>("postgres");

var app = builder.Build();

// docker compose chay "dotnet OrderFlow.Api.dll --migrate" MOT lan truoc khi api va worker len
if (args.Contains("--migrate"))
{
    await using var scope = app.Services.CreateAsyncScope();
    await scope.ServiceProvider.GetRequiredService<OrderDbContext>().Database.MigrateAsync();
    app.Logger.LogInformation("Database migrated");
    return;
}

// ---- 2. Pipeline middleware: request di tu tren xuong ----
app.UseExceptionHandler();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI(); // http://localhost:5080/swagger
}

app.MapControllers();
app.MapHealthChecks("/health");

app.Run();

public partial class Program { } // de project test dung duoc WebApplicationFactory<Program>
