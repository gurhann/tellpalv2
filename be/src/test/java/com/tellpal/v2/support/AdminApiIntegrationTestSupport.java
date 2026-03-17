package com.tellpal.v2.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AdminApiIntegrationTestSupport extends PostgresIntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private AdminRoleRepository adminRoleRepository;

    @Autowired
    private AdminPasswordHasher adminPasswordHasher;

    protected String authenticateAdmin() throws Exception {
        seedAdmin("admin-root", "secret", true, "ADMIN");
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "admin-root",
                                  "password": "secret"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return readPayload(result).get("accessToken").asText();
    }

    protected JsonNode readPayload(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    protected AdminUser seedAdmin(String username, String rawPassword, boolean active, String... roleCodes) {
        AdminUser adminUser = AdminUser.create(username, adminPasswordHasher.hash(rawPassword));
        if (!active) {
            adminUser.deactivate();
        }
        Arrays.stream(roleCodes)
                .map(roleCode -> adminRoleRepository.findByCode(roleCode).orElseThrow())
                .forEach(role -> adminUser.assignRole(role, Instant.now()));
        return adminUserRepository.save(adminUser);
    }
}
