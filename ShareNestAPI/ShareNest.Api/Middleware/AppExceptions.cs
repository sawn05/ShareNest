namespace ShareNest.Api.Middleware;

public class AppException : Exception
{
    public int StatusCode { get; }
    public AppException(string message, int statusCode) : base(message) => StatusCode = statusCode;
}