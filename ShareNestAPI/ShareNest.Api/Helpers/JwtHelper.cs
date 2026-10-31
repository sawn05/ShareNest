using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using ShareNest.Api.Configurations;
using ShareNest.Api.Models.Entities;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;

namespace ShareNest.Api.Helpers;

public interface IJwtHelper
{
    (string Token, DateTime ExpiresAt) Generate(User user);
}

public class JwtHelper : IJwtHelper
{
    private readonly JwtSettings _settings;

    public JwtHelper(IOptions<JwtSettings> options) => _settings = options.Value;

    public (string Token, DateTime ExpiresAt) Generate(User user)
    {
        var claims = new[]
        {
            new Claim(ClaimTypes.NameIdentifier, user.UserId.ToString()),
            new Claim(ClaimTypes.Email, user.Email),
            new Claim(ClaimTypes.Role, user.Role.ToString()) // "User" hoặc "Admin"
        };

        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_settings.Key));
        var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);
        var expires = DateTime.UtcNow.AddMinutes(_settings.ExpiryMinutes);

        var token = new JwtSecurityToken(
            issuer: _settings.Issuer,
            audience: _settings.Audience,
            claims: claims,
            expires: expires,
            signingCredentials: creds);

        return (new JwtSecurityTokenHandler().WriteToken(token), expires);
    }
}