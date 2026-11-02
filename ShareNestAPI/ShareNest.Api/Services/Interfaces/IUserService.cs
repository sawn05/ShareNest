using ShareNest.Api.Models.DTOs.Auth;
using ShareNest.Api.Models.DTOs.Users;

namespace ShareNest.Api.Services.Interfaces;

public interface IUserService
{
    Task<UserDto> GetProfileAsync(int userId);
    Task<UserDto> UpdateProfileAsync(int userId, UpdateProfileRequestDto dto);
    Task ChangePasswordAsync(int userId, ChangePasswordRequestDto dto);
}