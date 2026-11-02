using System.ComponentModel.DataAnnotations;

namespace ShareNest.Api.Models.DTOs.Users;

public class UpdateProfileRequestDto
{
    [Required, StringLength(100)]
    public string FullName { get; set; } = string.Empty;

    [Required, RegularExpression(@"^0\d{9,10}$", ErrorMessage = "Số điện thoại không hợp lệ.")]
    public string PhoneNumber { get; set; } = string.Empty;

    [StringLength(255)]
    public string? Address { get; set; }

    [StringLength(255), Url]
    public string? AvatarUrl { get; set; }
}

public class ChangePasswordRequestDto
{
    [Required]
    public string CurrentPassword { get; set; } = string.Empty;

    [Required, StringLength(100, MinimumLength = 8)]
    public string NewPassword { get; set; } = string.Empty;
}