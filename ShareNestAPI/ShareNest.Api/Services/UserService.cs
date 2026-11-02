using ShareNest.Api.Helpers;
using ShareNest.Api.Middleware;
using ShareNest.Api.Models.DTOs.Auth;
using ShareNest.Api.Models.DTOs.Users;
using ShareNest.Api.Models.Entities;
using ShareNest.Api.Repositories.Interfaces;
using ShareNest.Api.Services.Interfaces;

namespace ShareNest.Api.Services;

public class UserService : IUserService
{
    private readonly IUserRepository _users;
    public UserService(IUserRepository users) => _users = users;

    public async Task<UserDto> GetProfileAsync(int userId) =>
        UserMapper.ToDto(await GetUserOrThrowAsync(userId));

    public async Task<UserDto> UpdateProfileAsync(int userId, UpdateProfileRequestDto dto)
    {
        var user = await GetUserOrThrowAsync(userId);
        var phone = dto.PhoneNumber.Trim();

        if (await _users.PhoneExistsAsync(phone, userId))
            throw new AppException("Số điện thoại đã được sử dụng.", StatusCodes.Status409Conflict);

        var avatar = string.IsNullOrWhiteSpace(dto.AvatarUrl) ? null : dto.AvatarUrl.Trim();
        if (avatar is not null &&
            !(Uri.TryCreate(avatar, UriKind.Absolute, out var uri) && uri.Scheme == Uri.UriSchemeHttps))
            throw new AppException("Ảnh đại diện phải là đường dẫn https hợp lệ.",
                StatusCodes.Status400BadRequest);

        user.FullName = dto.FullName.Trim();
        user.PhoneNumber = phone;
        user.Address = string.IsNullOrWhiteSpace(dto.Address) ? null : dto.Address.Trim();
        user.AvatarUrl = avatar;

        await _users.UpdateAsync(user);
        return UserMapper.ToDto(user);
    }

    public async Task ChangePasswordAsync(int userId, ChangePasswordRequestDto dto)
    {
        var user = await GetUserOrThrowAsync(userId);

        if (!PasswordHelper.Verify(dto.CurrentPassword, user.PasswordHash))
            throw new AppException("Mật khẩu hiện tại không đúng.", StatusCodes.Status400BadRequest);

        if (dto.CurrentPassword == dto.NewPassword)
            throw new AppException("Mật khẩu mới phải khác mật khẩu hiện tại.",
                StatusCodes.Status400BadRequest);

        user.PasswordHash = PasswordHelper.Hash(dto.NewPassword);
        await _users.UpdateAsync(user);
    }

    private async Task<User> GetUserOrThrowAsync(int userId) =>
        await _users.GetByIdAsync(userId)
        ?? throw new AppException("Không tìm thấy người dùng.", StatusCodes.Status404NotFound);
}