using Microsoft.AspNetCore.Diagnostics;
using Microsoft.AspNetCore.Mvc;
using OrderFlow.Core.Domain;

namespace OrderFlow.Api.Infrastructure;

/// <summary>
/// IExceptionHandler (moi trong .NET 8): bat DomainException o mot cho, tra 400 ProblemDetails.
/// Exception khac -> tra false -> UseExceptionHandler tra 500 ProblemDetails, khong lo stack trace ra ngoai.
/// </summary>
public sealed class DomainExceptionHandler(IProblemDetailsService problemDetails) : IExceptionHandler
{
    public async ValueTask<bool> TryHandleAsync(HttpContext httpContext, Exception exception, CancellationToken ct)
    {
        if (exception is not DomainException)
            return false;

        httpContext.Response.StatusCode = StatusCodes.Status400BadRequest;
        return await problemDetails.TryWriteAsync(new ProblemDetailsContext
        {
            HttpContext = httpContext,
            Exception = exception,
            ProblemDetails = new ProblemDetails
            {
                Status = StatusCodes.Status400BadRequest,
                Title = "The order is not valid",
                Detail = exception.Message
            }
        });
    }
}
