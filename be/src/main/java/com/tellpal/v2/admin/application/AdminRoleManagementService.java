package com.tellpal.v2.admin.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.admin.api.AdminAssignRoleCommand;
import com.tellpal.v2.admin.api.AdminRoleManagementApi;
import com.tellpal.v2.admin.domain.AdminRole;
import com.tellpal.v2.admin.domain.AdminRoleRepository;
import com.tellpal.v2.admin.domain.AdminUser;
import com.tellpal.v2.admin.domain.AdminUserRepository;

/**
 * Application service for mutating admin role assignments.
 *
 * <p>The service loads the admin aggregate and role reference, then applies the assignment within
 * a single transaction so role changes are persisted atomically.
 */
@Service
public class AdminRoleManagementService implements AdminRoleManagementApi {

    private final Clock clock;
    private final AdminUserRepository adminUserRepository;
    private final AdminRoleRepository adminRoleRepository;

    public AdminRoleManagementService(
            Clock clock,
            AdminUserRepository adminUserRepository,
            AdminRoleRepository adminRoleRepository) {
        this.clock = clock;
        this.adminUserRepository = adminUserRepository;
        this.adminRoleRepository = adminRoleRepository;
    }

    /**
     * Assigns a role to an existing admin user.
     *
     * <p>The command fails when the admin user or role does not exist. Repeating the same
     * assignment leaves the aggregate unchanged.
     */
    @Override
    @Transactional
    public void assignRole(AdminAssignRoleCommand command) {
        AdminUser adminUser = adminUserRepository.findById(command.adminUserId())
                .orElseThrow(() -> new AdminUserNotFoundException(command.adminUserId()));
        AdminRole adminRole = adminRoleRepository.findByCode(command.roleCode())
                .orElseThrow(() -> new AdminRoleNotFoundException(command.roleCode()));

        Instant assignedAt = Instant.now(clock);
        adminUser.assignRole(adminRole, assignedAt);
        adminUserRepository.save(adminUser);
    }
}
