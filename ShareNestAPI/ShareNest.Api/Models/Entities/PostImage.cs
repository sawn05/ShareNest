namespace ShareNest.Api.Models.Entities;

public class PostImage
{
    public int ImageId { get; set; }
    public int PostId { get; set; }
    public string ImageUrl { get; set; } = string.Empty;
    public bool IsPrimary { get; set; }

    public Post Post { get; set; } = null!;
}