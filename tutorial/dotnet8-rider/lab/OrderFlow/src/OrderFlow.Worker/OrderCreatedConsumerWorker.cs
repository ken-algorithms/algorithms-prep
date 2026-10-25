using System.Text.Json;
using Azure.Messaging.ServiceBus;
using Microsoft.Extensions.Options;
using OrderFlow.Core.Messaging;

namespace OrderFlow.Worker;

/// <summary>
/// Background worker kieu "nghe su kien": ServiceBusProcessor tu keo message ve va goi HandleMessageAsync,
/// toi da MaxConcurrentCalls message cung luc. Moi message: Complete (xong), DeadLetter (hong, retry vo ich),
/// hoac nem exception (loi tam thoi) -> processor Abandon -> Service Bus giao lai, DeliveryCount tang.
/// </summary>
public sealed class OrderCreatedConsumerWorker(
    ServiceBusClient client,
    IServiceScopeFactory scopeFactory,
    IOptions<ServiceBusOptions> options,
    ILogger<OrderCreatedConsumerWorker> logger) : BackgroundService
{
    private ServiceBusProcessor? _processor;

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _processor = client.CreateProcessor(options.Value.OrdersQueue, new ServiceBusProcessorOptions
        {
            AutoCompleteMessages = false, // chi Complete sau khi DB commit: crash giua chung thi message duoc giao lai
            MaxConcurrentCalls = options.Value.MaxConcurrentCalls
        });
        _processor.ProcessMessageAsync += HandleMessageAsync;
        _processor.ProcessErrorAsync += HandleErrorAsync;

        await _processor.StartProcessingAsync(stoppingToken);
        logger.LogInformation("Consuming queue '{Queue}' with up to {Max} concurrent calls",
            options.Value.OrdersQueue, options.Value.MaxConcurrentCalls);

        try
        {
            await Task.Delay(Timeout.Infinite, stoppingToken); // processor tu chay tren thread rieng; o day chi doi tat
        }
        catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
        {
        }
    }

    public override async Task StopAsync(CancellationToken cancellationToken)
    {
        if (_processor is not null)
        {
            await _processor.StopProcessingAsync(cancellationToken); // cho cac message dang xu ly chay xong
            await _processor.DisposeAsync();
        }
        await base.StopAsync(cancellationToken);
    }

    private async Task HandleMessageAsync(ProcessMessageEventArgs args)
    {
        var message = args.Message;

        OrderCreated? evt = null;
        try
        {
            evt = message.Body.ToObjectFromJson<OrderCreated>(Json.Options);
        }
        catch (JsonException)
        {
        }

        if (evt is null || evt.OrderId == Guid.Empty)
        {
            // message hong: retry bao nhieu lan cung hong -> dua thang vao dead-letter queue cho nguoi xem
            await args.DeadLetterMessageAsync(message, "invalid-payload", "Body is not a valid OrderCreated", args.CancellationToken);
            logger.LogWarning("Dead-lettered message {MessageId}: invalid payload", message.MessageId);
            return;
        }

        await using var scope = scopeFactory.CreateAsyncScope(); // moi message mot scope -> mot DbContext rieng
        var handler = scope.ServiceProvider.GetRequiredService<OrderCreatedHandler>();
        var result = await handler.HandleAsync(message.MessageId, evt, args.CancellationToken);

        if (result == HandleResult.OrderNotFound)
        {
            await args.DeadLetterMessageAsync(message, "order-not-found", $"Order {evt.OrderId} does not exist", args.CancellationToken);
            logger.LogWarning("Dead-lettered message {MessageId}: order {OrderId} not found", message.MessageId, evt.OrderId);
            return;
        }

        await args.CompleteMessageAsync(message, args.CancellationToken);
        logger.LogInformation("Message {MessageId} {Result} (delivery #{Delivery})", message.MessageId, result, message.DeliveryCount);
    }

    private Task HandleErrorAsync(ProcessErrorEventArgs args)
    {
        logger.LogError(args.Exception, "Service Bus error from {Source} on {Entity}", args.ErrorSource, args.EntityPath);
        return Task.CompletedTask;
    }
}
