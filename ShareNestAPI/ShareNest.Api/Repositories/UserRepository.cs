using Microsoft.EntityFrameworkCore;
using ShareNest.Api.Data;
using ShareNest.Api.Models.Entities;
using ShareNest.Api.Repositories.Interfaces;

namespace ShareNest.Api.Repositories;

public class UserRepository : IUserRepository
{
    private readonly AppDbContext _db;
    public UserRepository(AppDbContext db) => _db = db;

    public Task<User?> GetByIdAsync(int id) => _db.Users.FirstOrDefaultAsync(u => u.UserId == id);

    public Task<User?> GetByEmailAsync(string email) =>
        _db.Users.FirstOrDefaultAsync(u => u.Email == email);

    public Task<bool> EmailExistsAsync(string email) => _db.Users.AnyAsync(u => u.Email == email);

    public Task<bool> PhoneExistsAsync(string phone) => _db.Users.AnyAsync(u => u.PhoneNumber == phone);

    public async Task AddAsync(User user)
    {
        _db.Users.Add(user);
        await _db.SaveChangesAsync();
    }
}