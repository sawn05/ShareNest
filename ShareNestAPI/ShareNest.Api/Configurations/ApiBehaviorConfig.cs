using Microsoft.AspNetCore.Mvc;
using ShareNest.Api.Models.DTOs.Common;

namespace ShareNest.Api.Configurations;

public static class ApiBehaviorConfig
{
    public static void Configure(ApiBehaviorOptions options)
    {
        options.InvalidModelStateResponseFactory = context =>
        {
            var errors = context.ModelState
                .Where(e => e.Value is { Errors.Count: > 0 })
                .ToDictionary(
                    e => char.ToLowerInvariant(e.Key[0]) + e.Key[1..],
                    e => e.Value!.Errors.Select(x => x.ErrorMessage).ToArray());

            return new BadRequestObjectResult(new ApiErrorResponse
            {
                Status = StatusCodes.Status400BadRequest,
                Message = "Dữ liệu không hợp lệ.",
                Errors = errors
            });
        };
    }
}