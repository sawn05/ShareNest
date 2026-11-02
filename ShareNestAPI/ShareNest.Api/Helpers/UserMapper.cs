using ShareNest.Api.Models.DTOs.Auth;
using ShareNest.Api.Models.Entities;

namespace ShareNest.Api.Helpers;

public static class UserMapper
{
    public static UserDto ToDto(User u) => new()
    {
        UserId = u.UserId,
        FullName = u.FullName,
        Email = u.Email,
        PhoneNumber = u.PhoneNumber,
        Address = u.Address,
        AvatarUrl = u.AvatarUrl,
        Role = u.Role.ToString()
    };
}