package com.tellpal.v2.admin.infrastructure.security;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.admin.domain.AdminRole;
import com.tellpal.v2.admin.domain.AdminRoleRepository;
import com.tellpal.v2.admin.domain.AdminUser;
import com.tellpal.v2.admin.domain.AdminUserRepository;

@Component
class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final String DEFAULT_ROLE_CODE = "ADMIN";

    private final AdminUserRepository adminUserRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminPasswordHasher adminPasswordHasher;
    private final String username;
    private final String password;
    private final String roleCode;

    AdminBootstrapRunner(
            AdminUserRepository adminUserRepository,
            AdminRoleRepository adminRoleRepository,
            AdminPasswordHasher adminPasswordHasher,
            @Value("${tellpal.security.admin.bootstrap.username:}") String username,
            @Value("${tellpal.security.admin.bootstrap.password:}") String password,
            @Value("${tellpal.security.admin.bootstrap.role-code:" + DEFAULT_ROLE_CODE + "}") String roleCode) {
        this.adminUserRepository = adminUserRepository;
        this.adminRoleRepository = adminRoleRepository;
        this.adminPasswordHasher = adminPasswordHasher;
        this.username = normalize(username);
        this.password = password == null ? "" : password;
        this.roleCode = normalize(roleCode);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() && password.isBlank()) {
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Both admin bootstrap username and password must be configured");
        }

        AdminRole role = adminRoleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Admin bootstrap role does not exist: " + roleCode));
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .map(existingUser -> updateExistingUser(existingUser, role))
                .orElseGet(() -> createUser(role));
        adminUserRepository.save(adminUser);
        log.info("admin_bootstrap_completed username={} roleCode={}", username, roleCode);
    }

    private AdminUser createUser(AdminRole role) {
        AdminUser adminUser = AdminUser.create(username, adminPasswordHasher.hash(password));
        adminUser.assignRole(role, Instant.now());
        return adminUser;
    }

    private AdminUser updateExistingUser(AdminUser adminUser, AdminRole role) {
        adminUser.updatePasswordHash(adminPasswordHasher.hash(password));
        adminUser.activate();
        adminUser.assignRole(role, Instant.now());
        return adminUser;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
