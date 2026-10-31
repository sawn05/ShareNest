using ShareNest.Api.Helpers;
using ShareNest.Api.Middleware;
using ShareNest.Api.Models.DTOs.Auth;
using ShareNest.Api.Models.Entities;
using ShareNest.Api.Models.Enums;
using ShareNest.Api.Repositories.Interfaces;
using ShareNest.Api.Services.Interfaces;

namespace ShareNest.Api.Services;

public class AuthService : IAuthService
{
    private readonly IUserRepository _users;
    private readonly IJwtHelper _jwt;

    public AuthService(IUserRepository users, IJwtHelper jwt)
    {
        _users = users;
        _jwt = jwt;
    }

    public async Task<AuthResponseDto> RegisterAsync(RegisterRequestDto dto)
    {
        var email = dto.Email.Trim().ToLowerInvariant();

        if (await _users.EmailExistsAsync(email))
            throw new AppException("Email đã được sử dụng.", StatusCodes.Status409Conflict);
        if (await _users.PhoneExistsAsync(dto.PhoneNumber))
            throw new AppException("Số điện thoại đã được sử dụng.", StatusCodes.Status409Conflict);

        var user = new User
        {
            FullName = dto.FullName.Trim(),
            Email = email,
            PhoneNumber = dto.PhoneNumber,
            PasswordHash = PasswordHelper.Hash(dto.Password),
            Address = dto.Address,
            Role = UserRole.User // luôn là User
        };
        await _users.AddAsync(user);

        return BuildResponse(user);
    }

    public async Task<AuthResponseDto> LoginAsync(LoginRequestDto dto)
    {
        var user = await _users.GetByEmailAsync(dto.Email.Trim().ToLowerInvariant());

        // Tránh lộ thông tin đăng nhập
        if (user is null || !PasswordHelper.Verify(dto.Password, user.PasswordHash))
            throw new AppException("Email hoặc mật khẩu không đúng.", StatusCodes.Status401Unauthorized);

        return BuildResponse(user);
    }

    public async Task<UserDto> GetCurrentUserAsync(int userId)
    {
        var user = await _users.GetByIdAsync(userId)
            ?? throw new AppException("Không tìm thấy người dùng.", StatusCodes.Status404NotFound);
        return ToDto(user);
    }

    private AuthResponseDto BuildResponse(User user)
    {
        var (token, expiresAt) = _jwt.Generate(user);
        return new AuthResponseDto { Token = token, ExpiresAt = expiresAt, User = ToDto(user) };
    }

    private static UserDto ToDto(User u) => new()
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