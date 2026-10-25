using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Mvc;
using OrderFlow.Api.Contracts;
using OrderFlow.Api.Services;
using OrderFlow.Core.Domain;

namespace OrderFlow.Api.Controllers;

[ApiController] // tu validate model -> 400 ValidationProblemDetails, tu bind body JSON
[Route("api/orders")]
// Khong dat [Produces("application/json")] o day: no ep ca loi 400/404 thanh application/json,
// mat content type chuan application/problem+json (test Unknown_id_returns_404_problem bat duoc)
public class OrdersController(OrderService orders) : ControllerBase
{
    [HttpPost]
    [ProducesResponseType(typeof(OrderResponse), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    public async Task<ActionResult<OrderResponse>> Create(CreateOrderRequest request, CancellationToken ct)
    {
        var order = await orders.CreateAsync(request, ct);
        return CreatedAtAction(nameof(GetById), new { id = order.Id }, order); // 201 + header Location
    }

    [HttpGet("{id:guid}")]
    [ProducesResponseType(typeof(OrderResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<ActionResult<OrderResponse>> GetById(Guid id, CancellationToken ct)
    {
        var order = await orders.GetAsync(id, ct);
        return order is null ? NotFound() : order;
    }

    [HttpGet]
    [ProducesResponseType(typeof(IReadOnlyList<OrderResponse>), StatusCodes.Status200OK)]
    public Task<IReadOnlyList<OrderResponse>> List(
        [FromQuery] OrderStatus? status,
        [FromQuery, Range(1, 100)] int take = 20,
        CancellationToken ct = default)
        => orders.ListAsync(status, take, ct);
}
