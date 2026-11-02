using System.Text.Json.Serialization;

namespace ShareNest.Api.Models.DTOs.Common;

public class ApiErrorResponse
{
    public int Status { get; set; }
    public string Message { get; set; } = string.Empty;

    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public Dictionary<string, string[]>? Errors { get; set; }
}