package com.tellpal.v2.admin.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.tellpal.v2.admin.domain.AdminRoleRepository;
import com.tellpal.v2.admin.domain.AdminUser;
import com.tellpal.v2.admin.domain.AdminUserRepository;
import com.tellpal.v2.admin.infrastructure.security.AdminPasswordHasher;
import com.tellpal.v2.shared.web.admin.AdminWebRequestSupport;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAuthIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private AdminRoleRepository adminRoleRepository;

    @Autowired
    private AdminPasswordHasher adminPasswordHasher;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("truncate table admin_refresh_tokens, admin_user_roles, admin_users restart identity cascade");
    }

    @Test
    void loginReturnsTokensAndPersistsRefreshTokenHash() throws Exception {
        AdminUser adminUser = seedAdmin("admin-root", "secret", true, "ADMIN");

        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType("application/json")
                        .header(AdminWebRequestSupport.REQUEST_ID_HEADER, "req-login-001")
                        .content("""
                                {
                                  "username": "admin-root",
                                  "password": "secret"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(AdminWebRequestSupport.REQUEST_ID_HEADER, "req-login-001"))
                .andReturn();

        JsonNode payload = readPayload(result);
        String refreshToken = payload.get("refreshToken").asText();

        assertThat(payload.get("accessToken").asText()).isNotBlank();
        assertThat(refreshToken).isNotBlank();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from admin_refresh_tokens where admin_user_id = ?",
                Integer.class,
                adminUser.getId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select refresh_token_hash from admin_refresh_tokens where admin_user_id = ?",
                String.class,
                adminUser.getId())).isNotEqualTo(refreshToken);
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        seedAdmin("admin-root", "secret", true, "ADMIN");

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "admin-root",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsDisabledAdmin() throws Exception {
        seedAdmin("disabled-admin", "secret", false, "ADMIN");

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "disabled-admin",
                                  "password": "secret"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshRotatesTokensAndRejectsReusedToken() throws Exception {
        seedAdmin("admin-root", "secret", true, "ADMIN");

        String originalRefreshToken = loginAndReadRefreshToken("admin-root", "secret");
        String rotatedRefreshToken = refreshAndReadRefreshToken(originalRefreshToken);

        assertThat(rotatedRefreshToken).isNotEqualTo(originalRefreshToken);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from admin_refresh_tokens where revoked_at is null",
                Integer.class)).isEqualTo(1);

        mockMvc.perform(post("/api/admin/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(originalRefreshToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        seedAdmin("admin-root", "secret", true, "ADMIN");

        String refreshToken = loginAndReadRefreshToken("admin-root", "secret");

        mockMvc.perform(post("/api/admin/auth/logout")
                        .contentType("application/json")
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/admin/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndReadRefreshToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return readPayload(result).get("refreshToken").asText();
    }

    private String refreshAndReadRefreshToken(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andReturn();
        return readPayload(result).get("refreshToken").asText();
    }

    private JsonNode readPayload(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private AdminUser seedAdmin(String username, String rawPassword, boolean active, String... roleCodes) {
        AdminUser adminUser = AdminUser.create(username, adminPasswordHasher.hash(rawPassword));
        if (!active) {
            adminUser.deactivate();
        }
        Arrays.stream(roleCodes)
                .map(roleCode -> adminRoleRepository.findByCode(roleCode).orElseThrow())
                .forEach(role -> adminUser.assignRole(role, java.time.Instant.now()));
        return adminUserRepository.save(adminUser);
    }
}
