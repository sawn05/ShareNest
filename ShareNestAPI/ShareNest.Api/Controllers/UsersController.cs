using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using ShareNest.Api.Helpers;
using ShareNest.Api.Models.DTOs.Auth;
using ShareNest.Api.Models.DTOs.Users;
using ShareNest.Api.Services.Interfaces;

namespace ShareNest.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/users")]
public class UsersController : ControllerBase
{
    private readonly IUserService _users;
    public UsersController(IUserService users) => _users = users;

    [HttpGet("me")]
    [ProducesResponseType(typeof(UserDto), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetMe() =>
        Ok(await _users.GetProfileAsync(User.GetUserId()));

    [HttpPut("me")]
    [ProducesResponseType(typeof(UserDto), StatusCodes.Status200OK)]
    public async Task<IActionResult> UpdateMe(UpdateProfileRequestDto dto) =>
        Ok(await _users.UpdateProfileAsync(User.GetUserId(), dto));

    [HttpPut("me/password")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> ChangePassword(ChangePasswordRequestDto dto)
    {
        await _users.ChangePasswordAsync(User.GetUserId(), dto);
        return NoContent();
    }
}