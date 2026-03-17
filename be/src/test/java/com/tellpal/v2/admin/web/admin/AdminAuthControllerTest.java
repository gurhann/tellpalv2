package com.tellpal.v2.admin.web.admin;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.tellpal.v2.admin.api.AdminAuthenticationApi;
import com.tellpal.v2.admin.api.AdminAuthenticationResult;
import com.tellpal.v2.admin.application.AdminAuthenticationFailedException;
import com.tellpal.v2.shared.web.admin.AdminApiExceptionHandler;
import com.tellpal.v2.shared.web.admin.AdminAuthenticationFacade;
import com.tellpal.v2.shared.web.admin.AdminProblemDetailsFactory;

@WebMvcTest(AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AdminAuthExceptionHandler.class, AdminApiExceptionHandler.class, AdminProblemDetailsFactory.class})
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAuthenticationApi adminAuthenticationApi;

    @MockitoBean
    private AdminAuthenticationFacade adminAuthenticationFacade;

    @Test
    void loginReturnsAuthenticationPayload() throws Exception {
        when(adminAuthenticationApi.login(org.mockito.ArgumentMatchers.any())).thenReturn(new AdminAuthenticationResult(
                42L,
                "admin-root",
                Set.of("ADMIN"),
                "access-token",
                Instant.parse("2026-03-17T11:00:00Z"),
                "refresh-token",
                Instant.parse("2026-04-16T10:00:00Z")));

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "MockMvc")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .content("""
                                {
                                  "username": "admin-root",
                                  "password": "secret"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminUserId").value(42))
                .andExpect(jsonPath("$.username").value("admin-root"))
                .andExpect(jsonPath("$.roleCodes[0]").value("ADMIN"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void refreshReturnsAuthenticationPayload() throws Exception {
        when(adminAuthenticationApi.refresh(org.mockito.ArgumentMatchers.any())).thenReturn(new AdminAuthenticationResult(
                42L,
                "admin-root",
                Set.of("ADMIN", "VIEWER"),
                "replacement-access-token",
                Instant.parse("2026-03-17T11:00:00Z"),
                "replacement-refresh-token",
                Instant.parse("2026-04-16T10:00:00Z")));

        mockMvc.perform(post("/api/admin/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "MockMvc")
                        .content("""
                                {
                                  "refreshToken": "raw-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("replacement-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("replacement-refresh-token"));
    }

    @Test
    void logoutReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/admin/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "raw-refresh-token"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(adminAuthenticationApi).logout(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void invalidCredentialsBecomeUnauthorizedProblemDetails() throws Exception {
        when(adminAuthenticationApi.login(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new AdminAuthenticationFailedException("Invalid admin credentials"));

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin-root",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication failed"))
                .andExpect(jsonPath("$.detail").value("Invalid admin credentials"))
                .andExpect(jsonPath("$.errorCode").value("auth_failed"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/admin/auth/login"));
    }
}
