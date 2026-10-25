// Thi nghiem Azure Service Bus tren emulator (bai 09). Moi lenh mot hien tuong:
//   dotnet run -- basics | retry | processor-throws | duplicate | topic | poison | dlq <queue>
using Azure.Messaging.ServiceBus;

var connectionString = Environment.GetEnvironmentVariable("SB_CONNECTION")
    ?? "Endpoint=sb://localhost;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";

await using var client = new ServiceBusClient(connectionString);
var command = args.FirstOrDefault() ?? "basics";

switch (command)
{
    case "basics": await Basics(); break;
    case "retry": await Retry(); break;
    case "processor-throws": await ProcessorThrows(); break;
    case "duplicate": await Duplicate(); break;
    case "topic": await Topic(); break;
    case "poison": await Poison(); break;
    case "dlq": await PeekDeadLetters(args.ElementAtOrDefault(1) ?? "demo"); break;
    default: Console.WriteLine($"unknown command '{command}'"); break;
}

// 1. Gui -> nhan kieu peek-lock -> Complete. Message bi "khoa" chu khong bi xoa cho toi khi Complete.
async Task Basics()
{
    await using var sender = client.CreateSender("demo");
    await sender.SendMessageAsync(new ServiceBusMessage("hello from .NET 8") { MessageId = $"basics-{Guid.NewGuid():N}" });
    Console.WriteLine("sent 1 message to queue 'demo'");

    await using var receiver = client.CreateReceiver("demo"); // mac dinh ReceiveMode.PeekLock
    var msg = await receiver.ReceiveMessageAsync(TimeSpan.FromSeconds(5));
    Console.WriteLine($"received: body='{msg.Body}' messageId={msg.MessageId} seq={msg.SequenceNumber} " +
                      $"deliveryCount={msg.DeliveryCount} lockedUntil={msg.LockedUntil:HH:mm:ss}");
    await receiver.CompleteMessageAsync(msg); // xac nhan xong -> Service Bus xoa message
    Console.WriteLine("completed");

    var again = await receiver.ReceiveMessageAsync(TimeSpan.FromSeconds(2));
    Console.WriteLine($"receive again: {(again is null ? "nothing (queue empty)" : again.MessageId)}");
}

// 2. Abandon (= xu ly loi) lap lai: DeliveryCount tang; qua MaxDeliveryCount (3) -> tu vao dead-letter queue
async Task Retry()
{
    await using var sender = client.CreateSender("demo");
    var id = $"retry-{Guid.NewGuid():N}";
    await sender.SendMessageAsync(new ServiceBusMessage("this one always fails") { MessageId = id });

    await using var receiver = client.CreateReceiver("demo");
    for (var attempt = 1; attempt <= 4; attempt++)
    {
        var msg = await receiver.ReceiveMessageAsync(TimeSpan.FromSeconds(3));
        if (msg is null)
        {
            Console.WriteLine($"attempt {attempt}: nothing left in 'demo' -> the broker moved it to the dead-letter queue");
            break;
        }
        Console.WriteLine($"attempt {attempt}: got {msg.MessageId} deliveryCount={msg.DeliveryCount} -> abandon");
        await receiver.AbandonMessageAsync(msg);
    }
    await PeekDeadLetters("demo", onlyId: id);
}

// 3. Handler cua ServiceBusProcessor nem exception: processor tu Abandon -> giao lai -> het luot -> dead-letter
async Task ProcessorThrows()
{
    await using var sender = client.CreateSender("demo");
    var id = $"throws-{Guid.NewGuid():N}";
    await sender.SendMessageAsync(new ServiceBusMessage("handler will throw") { MessageId = id });

    var deliveries = 0;
    await using var processor = client.CreateProcessor("demo", new ServiceBusProcessorOptions { AutoCompleteMessages = false });
    processor.ProcessMessageAsync += args =>
    {
        deliveries++;
        Console.WriteLine($"handler: {args.Message.MessageId} deliveryCount={args.Message.DeliveryCount} -> throw");
        throw new InvalidOperationException("database is down");
    };
    processor.ProcessErrorAsync += args =>
    {
        Console.WriteLine($"ProcessErrorAsync: {args.ErrorSource}: {args.Exception.Message}");
        return Task.CompletedTask;
    };
    await processor.StartProcessingAsync();
    await Task.Delay(TimeSpan.FromSeconds(6));
    await processor.StopProcessingAsync();
    Console.WriteLine($"handler ran {deliveries} times");
    await PeekDeadLetters("demo", onlyId: id);
}

// 4. Queue bat RequiresDuplicateDetection: hai message CUNG MessageId trong cua so -> broker giu mot
async Task Duplicate()
{
    await using var sender = client.CreateSender("demo");
    var id = $"dup-{Guid.NewGuid():N}";
    await sender.SendMessageAsync(new ServiceBusMessage("first copy") { MessageId = id });
    await sender.SendMessageAsync(new ServiceBusMessage("second copy, same MessageId") { MessageId = id });
    Console.WriteLine($"sent 2 messages with MessageId={id}");

    await using var receiver = client.CreateReceiver("demo");
    var got = 0;
    while (await receiver.ReceiveMessageAsync(TimeSpan.FromSeconds(3)) is { } msg)
    {
        got++;
        Console.WriteLine($"received '{msg.Body}'");
        await receiver.CompleteMessageAsync(msg);
    }
    Console.WriteLine($"received {got} message(s)");
}

// 5. Topic: MOT message gui vao topic, MOI subscription nhan mot ban sao rieng (fan-out)
async Task Topic()
{
    await using var sender = client.CreateSender("order-events");
    await sender.SendMessageAsync(new ServiceBusMessage("""{"orderId":"demo-1","status":"Confirmed"}""")
    {
        MessageId = $"evt-{Guid.NewGuid():N}",
        Subject = "order-confirmed",
        ContentType = "application/json"
    });
    Console.WriteLine("published 1 event to topic 'order-events'");

    foreach (var subscription in new[] { "email", "analytics" })
    {
        await using var receiver = client.CreateReceiver("order-events", subscription);
        var msg = await receiver.ReceiveMessageAsync(TimeSpan.FromSeconds(5));
        Console.WriteLine($"subscription '{subscription}' got: subject={msg?.Subject} body={msg?.Body}");
        if (msg is not null) await receiver.CompleteMessageAsync(msg);
    }
}

// 6. Gui message hong vao queue 'orders' cua OrderFlow: Worker phai dead-letter ngay, khong retry
async Task Poison()
{
    await using var sender = client.CreateSender("orders");
    var id = $"poison-{Guid.NewGuid():N}";
    await sender.SendMessageAsync(new ServiceBusMessage("this is not json") { MessageId = id, Subject = "order-created" });
    Console.WriteLine($"sent poison message {id} to 'orders'; check the Worker log, then: dotnet run -- dlq orders");
}

async Task PeekDeadLetters(string queue, string? onlyId = null)
{
    await using var dlq = client.CreateReceiver(queue, new ServiceBusReceiverOptions { SubQueue = SubQueue.DeadLetter });
    var messages = await dlq.PeekMessagesAsync(maxMessages: 50); // Peek: chi xem, khong khoa, khong xoa
    var shown = messages.Where(m => onlyId is null || m.MessageId == onlyId).ToList();
    Console.WriteLine($"dead-letter queue of '{queue}': {shown.Count} message(s)");
    foreach (var m in shown)
        Console.WriteLine($"  {m.MessageId} reason={m.DeadLetterReason} description={m.DeadLetterErrorDescription} deliveryCount={m.DeliveryCount}");
}
