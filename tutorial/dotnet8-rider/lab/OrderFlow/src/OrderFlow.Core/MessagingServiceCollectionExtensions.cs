using Azure.Messaging.ServiceBus;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using OrderFlow.Core.Messaging;
using OrderFlow.Core.Outbox;

namespace OrderFlow.Core;

/// <summary>Dang ky phan messaging cho Worker: Service Bus, outbox, handler.</summary>
public static class MessagingServiceCollectionExtensions
{
    public static IServiceCollection AddOrderFlowMessaging(this IServiceCollection services, IConfiguration configuration)
    {
        services.AddOptions<ServiceBusOptions>()
            .Bind(configuration.GetSection(ServiceBusOptions.Section))
            .ValidateDataAnnotations()
            .ValidateOnStart(); // cau hinh sai thi chet ngay luc khoi dong, khong doi toi message dau tien
        services.AddOptions<OutboxOptions>()
            .Bind(configuration.GetSection(OutboxOptions.Section))
            .ValidateDataAnnotations()
            .ValidateOnStart();

        var connectionString = configuration.GetConnectionString("ServiceBus")
            ?? throw new InvalidOperationException("Missing connection string 'ConnectionStrings:ServiceBus'");

        // ServiceBusClient giu ket noi AMQP, thread-safe: MOT instance cho ca app (giong HttpClient dung chung)
        services.AddSingleton(_ => new ServiceBusClient(connectionString, new ServiceBusClientOptions
        {
            RetryOptions = new ServiceBusRetryOptions { MaxRetries = 2, TryTimeout = TimeSpan.FromSeconds(10) }
        }));
        services.AddSingleton(sp => sp.GetRequiredService<ServiceBusClient>()
            .CreateSender(sp.GetRequiredService<IOptions<ServiceBusOptions>>().Value.OrdersQueue));
        services.AddSingleton<IMessagePublisher, ServiceBusMessagePublisher>();

        services.AddScoped<OutboxProcessor>();
        services.AddScoped<OrderCreatedHandler>();
        return services;
    }
}
