using ShareNest.Api.Models.Enums;

namespace ShareNest.Api.Models.Entities;

public class ReceiveRequest
{
    public int RequestId { get; set; }
    public int PostId { get; set; }
    public int RequesterId { get; set; }
    public RequestStatus Status { get; set; } = RequestStatus.Pending;
    public DateTime CreatedAt { get; set; }

    public Post Post { get; set; } = null!;
    public User Requester { get; set; } = null!;
}