using OrderFlow.Core;
using OrderFlow.Worker;

var builder = Host.CreateApplicationBuilder(args);

builder.Services.AddOrderFlowData(builder.Configuration);      // DbContext + TimeProvider
builder.Services.AddOrderFlowMessaging(builder.Configuration); // ServiceBusClient, sender, OutboxProcessor, handler

builder.Services.AddHostedService<OutboxDispatcherWorker>();     // outbox -> Service Bus
builder.Services.AddHostedService<OrderCreatedConsumerWorker>(); // Service Bus -> xu ly don

var host = builder.Build();
host.Run();
