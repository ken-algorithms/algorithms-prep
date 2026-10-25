using OrderFlow.Core;
using OrderFlow.Core.Messaging;
using OrderFlow.Core.Outbox;
using OrderFlow.Worker;

var builder = Host.CreateApplicationBuilder(args);

builder.Services.AddOrderFlowData(builder.Configuration); // DbContext + TimeProvider
builder.Services.AddOptions<OutboxOptions>()
    .Bind(builder.Configuration.GetSection(OutboxOptions.Section))
    .ValidateDataAnnotations()
    .ValidateOnStart(); // cau hinh sai thi chet ngay luc khoi dong
builder.Services.AddSingleton<IMessagePublisher, LoggingMessagePublisher>(); // bai 09 doi sang Service Bus
builder.Services.AddScoped<OutboxProcessor>();

builder.Services.AddHostedService<OutboxDispatcherWorker>(); // outbox -> (tam thoi) log

var host = builder.Build();
host.Run();
