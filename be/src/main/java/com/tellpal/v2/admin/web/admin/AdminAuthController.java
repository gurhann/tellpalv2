package com.tellpal.v2.admin.web.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.tellpal.v2.admin.api.AdminAuthenticationApi;
import com.tellpal.v2.admin.api.AdminAuthenticationResult;
import com.tellpal.v2.admin.api.AdminLoginCommand;
import com.tellpal.v2.admin.api.AdminLogoutCommand;
import com.tellpal.v2.admin.api.AdminRefreshCommand;
import com.tellpal.v2.shared.web.admin.AdminApiController;
import com.tellpal.v2.shared.web.admin.AdminWebRequestSupport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@AdminApiController
@RequestMapping("/api/admin/auth")
@Tag(name = "Admin Auth", description = "Authentication endpoints for admin sessions.")
public class AdminAuthController {

    private final AdminAuthenticationApi adminAuthenticationApi;

    public AdminAuthController(AdminAuthenticationApi adminAuthenticationApi) {
        this.adminAuthenticationApi = adminAuthenticationApi;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in an admin user", description = "Authenticates the admin user and issues new access and refresh tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin user authenticated"),
            @ApiResponse(responseCode = "400", description = "Login request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Credentials are invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user is disabled", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminAuthenticationResponse login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest httpServletRequest) {
        return AdminAuthenticationResponse.from(adminAuthenticationApi.login(new AdminLoginCommand(
                request.username(),
                request.password(),
                httpServletRequest.getHeader("User-Agent"),
                AdminWebRequestSupport.resolveClientIp(httpServletRequest))));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh admin tokens", description = "Rotates the refresh token and returns a new access and refresh token pair.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens rotated"),
            @ApiResponse(responseCode = "400", description = "Refresh request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Refresh token is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user is disabled", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Refresh token reuse was detected", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminAuthenticationResponse refresh(
            @Valid @RequestBody AdminRefreshRequest request,
            HttpServletRequest httpServletRequest) {
        return AdminAuthenticationResponse.from(adminAuthenticationApi.refresh(new AdminRefreshCommand(
                request.refreshToken(),
                httpServletRequest.getHeader("User-Agent"),
                AdminWebRequestSupport.resolveClientIp(httpServletRequest))));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Log out an admin session", description = "Invalidates the supplied refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Refresh token revoked"),
            @ApiResponse(responseCode = "400", description = "Logout request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public void logout(@Valid @RequestBody AdminLogoutRequest request) {
        adminAuthenticationApi.logout(new AdminLogoutCommand(request.refreshToken()));
    }
}
