package com.tellpal.v2.presentation.api.admin;

import com.tellpal.v2.admin.application.AdminAuthApplicationService;
import com.tellpal.v2.admin.application.LoginResult;
import com.tellpal.v2.admin.application.RefreshResult;
import com.tellpal.v2.presentation.dto.admin.LoginRequest;
import com.tellpal.v2.presentation.dto.admin.LoginResponse;
import com.tellpal.v2.presentation.dto.admin.LogoutRequest;
import com.tellpal.v2.presentation.dto.admin.RefreshRequest;
import com.tellpal.v2.presentation.dto.admin.RefreshResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthApplicationService adminAuthApplicationService;

    public AdminAuthController(AdminAuthApplicationService adminAuthApplicationService) {
        this.adminAuthApplicationService = adminAuthApplicationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResult result = adminAuthApplicationService.login(request.username(), request.password());
        return ResponseEntity.ok(new LoginResponse(result.accessToken(), result.refreshToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest request) {
        RefreshResult result = adminAuthApplicationService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(new RefreshResponse(result.accessToken(), result.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        adminAuthApplicationService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
