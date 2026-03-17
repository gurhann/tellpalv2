package com.tellpal.v2.admin.web.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tellpal.v2.admin.api.AdminAuthenticationApi;
import com.tellpal.v2.admin.api.AdminAuthenticationResult;
import com.tellpal.v2.admin.api.AdminLoginCommand;
import com.tellpal.v2.admin.api.AdminLogoutCommand;
import com.tellpal.v2.admin.api.AdminRefreshCommand;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthenticationApi adminAuthenticationApi;

    public AdminAuthController(AdminAuthenticationApi adminAuthenticationApi) {
        this.adminAuthenticationApi = adminAuthenticationApi;
    }

    @PostMapping("/login")
    public AdminAuthenticationResponse login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest httpServletRequest) {
        return AdminAuthenticationResponse.from(adminAuthenticationApi.login(new AdminLoginCommand(
                request.username(),
                request.password(),
                httpServletRequest.getHeader("User-Agent"),
                resolveClientIp(httpServletRequest))));
    }

    @PostMapping("/refresh")
    public AdminAuthenticationResponse refresh(
            @Valid @RequestBody AdminRefreshRequest request,
            HttpServletRequest httpServletRequest) {
        return AdminAuthenticationResponse.from(adminAuthenticationApi.refresh(new AdminRefreshCommand(
                request.refreshToken(),
                httpServletRequest.getHeader("User-Agent"),
                resolveClientIp(httpServletRequest))));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody AdminLogoutRequest request) {
        adminAuthenticationApi.logout(new AdminLogoutCommand(request.refreshToken()));
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }
        return forwardedFor.split(",", 2)[0].trim();
    }
}
