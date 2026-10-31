using ShareNest.Api.Models.Enums;

namespace ShareNest.Api.Models.Entities;

public class User
{
    public int UserId { get; set; }
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string PhoneNumber { get; set; } = string.Empty;
    public string PasswordHash { get; set; } = string.Empty;
    public string? Address { get; set; }
    public string? AvatarUrl { get; set; }
    public UserRole Role { get; set; } = UserRole.User;
    public DateTime CreatedAt { get; set; }

    public ICollection<Post> Posts { get; set; } = new List<Post>();
    public ICollection<ReceiveRequest> Requests { get; set; } = new List<ReceiveRequest>();
    public ICollection<SearchHistory> SearchHistories { get; set; } = new List<SearchHistory>();
}