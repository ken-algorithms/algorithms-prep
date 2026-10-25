using System.Diagnostics;
using OrderFlow.Api.Infrastructure;
using OrderFlow.Api.Services;

var builder = WebApplication.CreateBuilder(args);

// ---- 1. Dang ky dich vu (DI container) ----
builder.Services.AddSingleton(TimeProvider.System);  // dong ho: mot instance cho ca app
builder.Services.AddSingleton<InMemoryOrderStore>(); // kho tam: song suot doi app, moi request dung chung
builder.Services.AddScoped<OrderService>();          // moi request mot instance moi

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

builder.Services.AddProblemDetails(); // moi loi tra ve dang application/problem+json (RFC 7807)
builder.Services.AddExceptionHandler<DomainExceptionHandler>();
builder.Services.AddHealthChecks();   // bai 07 them kiem tra ket noi PostgreSQL

var app = builder.Build();

// ---- 2. Pipeline middleware: request di tu tren xuong, response di nguoc len ----
app.UseExceptionHandler();

// middleware tu viet de thay pipeline: do thoi gian moi request (bai 07 bo di, log SQL cua EF da du)
app.Use(async (context, next) =>
{
    var started = Stopwatch.GetTimestamp();
    await next(context); // goi cac middleware phia sau, cuoi cung la controller
    app.Logger.LogInformation("{Method} {Path} -> {Status} in {Elapsed:F1} ms", context.Request.Method,
        context.Request.Path, context.Response.StatusCode, Stopwatch.GetElapsedTime(started).TotalMilliseconds);
});

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI(); // http://localhost:5080/swagger
}

app.MapControllers();
app.MapHealthChecks("/health");

app.Run();
