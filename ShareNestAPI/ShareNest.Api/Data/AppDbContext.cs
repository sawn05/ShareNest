using Microsoft.EntityFrameworkCore;
using ShareNest.Api.Models.Entities;

namespace ShareNest.Api.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<User> Users => Set<User>();
    public DbSet<Category> Categories => Set<Category>();
    public DbSet<Post> Posts => Set<Post>();
    public DbSet<PostImage> PostImages => Set<PostImage>();
    public DbSet<ReceiveRequest> Requests => Set<ReceiveRequest>();
    public DbSet<SearchHistory> SearchHistories => Set<SearchHistory>();

    protected override void OnModelCreating(ModelBuilder b)
    {
        b.Entity<User>(e =>
        {
            e.ToTable("Users");
            e.Property(x => x.FullName).HasMaxLength(100).IsRequired();
            e.Property(x => x.Email).HasMaxLength(150).IsRequired();
            e.Property(x => x.PhoneNumber).HasMaxLength(20).IsRequired();
            e.Property(x => x.PasswordHash).HasMaxLength(255).IsRequired();
            e.Property(x => x.Address).HasMaxLength(255);
            e.Property(x => x.AvatarUrl).HasMaxLength(255);
            e.Property(x => x.Role).HasConversion<string>().HasMaxLength(10);
            e.Property(x => x.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
            e.HasIndex(x => x.Email).IsUnique();
            e.HasIndex(x => x.PhoneNumber).IsUnique();
        });

        b.Entity<Category>(e =>
        {
            e.ToTable("Categories");
            e.Property(x => x.Name).HasMaxLength(100).IsRequired();
            e.HasIndex(x => x.Name).IsUnique();
        });

        b.Entity<Post>(e =>
        {
            e.ToTable("Posts", t => t.HasCheckConstraint("CK_Posts_Quantity", "quantity > 0"));
            e.Property(x => x.Title).HasMaxLength(200).IsRequired();
            e.Property(x => x.Condition).HasMaxLength(50).IsRequired();
            e.Property(x => x.LocationName).HasMaxLength(255).IsRequired();
            e.Property(x => x.Status).HasConversion<string>().HasMaxLength(20);
            e.Property(x => x.Latitude).HasPrecision(10, 8);
            e.Property(x => x.Longitude).HasPrecision(11, 8);
            e.Property(x => x.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
            e.Property(x => x.UpdatedAt).HasDefaultValueSql("GETUTCDATE()");

            e.HasOne(x => x.User).WithMany(u => u.Posts)
                .HasForeignKey(x => x.UserId).OnDelete(DeleteBehavior.Restrict);
            e.HasOne(x => x.Category).WithMany(c => c.Posts)
                .HasForeignKey(x => x.CategoryId).OnDelete(DeleteBehavior.Restrict);

            e.HasIndex(x => x.Status);      // lọc bài theo trạng thái
            e.HasIndex(x => x.CategoryId);  // lọc theo danh mục
            e.HasIndex(x => x.Title);       // tìm kiếm theo tiêu đề
        });

        b.Entity<PostImage>(e =>
        {
            e.ToTable("Post_Image");
            e.HasKey(x => x.ImageId);
            e.Property(x => x.ImageUrl).HasMaxLength(255).IsRequired();
            e.HasOne(x => x.Post).WithMany(p => p.Images)
                .HasForeignKey(x => x.PostId).OnDelete(DeleteBehavior.Cascade);
        });

        b.Entity<ReceiveRequest>(e =>
        {
            e.ToTable("Request");
            e.HasKey(x => x.RequestId);
            e.Property(x => x.Status).HasConversion<string>().HasMaxLength(20);
            e.Property(x => x.CreatedAt).HasDefaultValueSql("GETUTCDATE()");

            e.HasOne(x => x.Post).WithMany(p => p.Requests)
                .HasForeignKey(x => x.PostId).OnDelete(DeleteBehavior.Cascade);
            e.HasOne(x => x.Requester).WithMany(u => u.Requests)
                .HasForeignKey(x => x.RequesterId).OnDelete(DeleteBehavior.Restrict);
        });

        b.Entity<SearchHistory>(e =>
        {
            e.ToTable("Search_History");
            e.HasKey(x => x.HistoryId);
            e.Property(x => x.Keyword).HasMaxLength(150).IsRequired();
            e.Property(x => x.SearchedAt).HasDefaultValueSql("GETUTCDATE()");
            e.HasOne(x => x.User).WithMany(u => u.SearchHistories)
                .HasForeignKey(x => x.UserId).OnDelete(DeleteBehavior.Cascade);
        });
    }

    // updated_at "tự động cập nhật"
    public override Task<int> SaveChangesAsync(CancellationToken ct = default)
    {
        foreach (var entry in ChangeTracker.Entries<Post>()
                     .Where(e => e.State == EntityState.Modified))
        {
            entry.Entity.UpdatedAt = DateTime.UtcNow;
        }
        return base.SaveChangesAsync(ct);
    }
}