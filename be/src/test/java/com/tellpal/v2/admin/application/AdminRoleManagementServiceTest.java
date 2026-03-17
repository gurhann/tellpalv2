package com.tellpal.v2.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.tellpal.v2.admin.api.AdminAssignRoleCommand;
import com.tellpal.v2.admin.domain.AdminRole;
import com.tellpal.v2.admin.domain.AdminRoleRepository;
import com.tellpal.v2.admin.domain.AdminUser;
import com.tellpal.v2.admin.domain.AdminUserRepository;

@ExtendWith(MockitoExtension.class)
class AdminRoleManagementServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private AdminRoleRepository adminRoleRepository;

    private AdminRoleManagementService adminRoleManagementService;

    @BeforeEach
    void setUp() {
        adminRoleManagementService = new AdminRoleManagementService(
                Clock.fixed(Instant.parse("2026-03-17T10:00:00Z"), ZoneOffset.UTC),
                adminUserRepository,
                adminRoleRepository);
    }

    @Test
    void assignsRequestedRoleToUser() {
        AdminUser adminUser = AdminUser.create("content-admin", "stored-password-hash");
        ReflectionTestUtils.setField(adminUser, "id", 99L, Long.class);
        AdminRole adminRole = AdminRole.create("CONTENT_MANAGER", "Content manager role");

        when(adminUserRepository.findById(99L)).thenReturn(Optional.of(adminUser));
        when(adminRoleRepository.findByCode("CONTENT_MANAGER")).thenReturn(Optional.of(adminRole));

        adminRoleManagementService.assignRole(new AdminAssignRoleCommand(99L, "CONTENT_MANAGER"));

        assertThat(adminUser.getRoleAssignments())
                .extracting(assignment -> assignment.getAdminRole().getCode())
                .containsExactly("CONTENT_MANAGER");
        verify(adminUserRepository).save(adminUser);
    }

    @Test
    void rejectsUnknownUsers() {
        when(adminUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminRoleManagementService.assignRole(new AdminAssignRoleCommand(99L, "ADMIN")))
                .isInstanceOf(AdminUserNotFoundException.class);
    }
}
