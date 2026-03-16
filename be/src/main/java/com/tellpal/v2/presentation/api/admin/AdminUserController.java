package com.tellpal.v2.presentation.api.admin;

import com.tellpal.v2.admin.application.AdminAuthApplicationService;
import com.tellpal.v2.admin.domain.AdminUser;
import com.tellpal.v2.presentation.dto.admin.AdminUserResponse;
import com.tellpal.v2.presentation.dto.admin.AssignRoleRequest;
import com.tellpal.v2.presentation.dto.admin.CreateAdminUserRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminAuthApplicationService adminAuthApplicationService;

    public AdminUserController(AdminAuthApplicationService adminAuthApplicationService) {
        this.adminAuthApplicationService = adminAuthApplicationService;
    }

    @PostMapping
    public ResponseEntity<AdminUserResponse> createAdminUser(@RequestBody CreateAdminUserRequest request) {
        AdminUser user = adminAuthApplicationService.createAdminUser(request.username(), request.password());
        return ResponseEntity.status(201).body(new AdminUserResponse(user.getId(), user.getUsername(), user.isEnabled()));
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<Void> assignRole(@PathVariable Long id, @RequestBody AssignRoleRequest request) {
        adminAuthApplicationService.assignRole(id, request.roleCode());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/roles/{roleCode}")
    public ResponseEntity<Void> removeRole(@PathVariable Long id, @PathVariable String roleCode) {
        adminAuthApplicationService.removeRole(id, roleCode);
        return ResponseEntity.noContent().build();
    }
}
