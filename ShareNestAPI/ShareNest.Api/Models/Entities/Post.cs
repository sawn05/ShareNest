using ShareNest.Api.Models.Enums;

namespace ShareNest.Api.Models.Entities;

public class Post
{
    public int PostId { get; set; }
    public int UserId { get; set; }
    public int CategoryId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public int Quantity { get; set; }
    public string Condition { get; set; } = string.Empty;
    public PostStatus Status { get; set; } = PostStatus.Available;
    public string LocationName { get; set; } = string.Empty;
    public decimal Latitude { get; set; }
    public decimal Longitude { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    public User User { get; set; } = null!;
    public Category Category { get; set; } = null!;
    public ICollection<PostImage> Images { get; set; } = new List<PostImage>();
    public ICollection<ReceiveRequest> Requests { get; set; } = new List<ReceiveRequest>();
}