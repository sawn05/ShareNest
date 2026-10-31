namespace ShareNest.Api.Models.Entities;

public class SearchHistory
{
    public int HistoryId { get; set; }
    public int UserId { get; set; }
    public string Keyword { get; set; } = string.Empty;
    public DateTime SearchedAt { get; set; }

    public User User { get; set; } = null!;
}