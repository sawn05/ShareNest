using ShareNest.Api.Models.DTOs.Auth;

namespace ShareNest.Api.Services.Interfaces;

public interface IAuthService
{
    Task<AuthResponseDto> RegisterAsync(RegisterRequestDto dto);
    Task<AuthResponseDto> LoginAsync(LoginRequestDto dto);
    Task<UserDto> GetCurrentUserAsync(int userId);
}