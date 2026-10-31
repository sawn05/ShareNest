using Microsoft.EntityFrameworkCore;
using ShareNest.Api.Data;
using ShareNest.Api.Helpers;
using ShareNest.Api.Models.Entities;
using ShareNest.Api.Models.Enums;

namespace ShareNest.Api.Data;

public static class DbSeeder
{
    public static async Task SeedAsync(AppDbContext db, IConfiguration config)
    {
        var email = config["SeedAdmin:Email"];
        var password = config["SeedAdmin:Password"];
        if (string.IsNullOrWhiteSpace(email) || string.IsNullOrWhiteSpace(password)) return;

        if (await db.Users.AnyAsync(u => u.Email == email)) return;

        db.Users.Add(new User
        {
            FullName = config["SeedAdmin:FullName"] ?? "Admin Phạm Quang Sáng",
            Email = email.ToLowerInvariant(),
            PhoneNumber = config["SeedAdmin:PhoneNumber"] ?? "0900000000",
            PasswordHash = PasswordHelper.Hash(password),
            Role = UserRole.Admin
        });
        await db.SaveChangesAsync();
    }
}