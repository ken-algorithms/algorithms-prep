using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using OrderFlow.Core.Data;

namespace OrderFlow.Core;

/// <summary>Dang ky phan du lieu, dung chung cho Api va Worker.</summary>
public static class DataServiceCollectionExtensions
{
    public static IServiceCollection AddOrderFlowData(this IServiceCollection services, IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString("Postgres")
            ?? throw new InvalidOperationException("Missing connection string 'ConnectionStrings:Postgres'");

        services.AddDbContext<OrderDbContext>(options => options
            .UseNpgsql(connectionString)
            .UseSnakeCaseNamingConvention()); // bang orders, cot customer_email thay vi "Orders"."CustomerEmail"

        services.TryAddSingleton(TimeProvider.System);
        return services;
    }
}
