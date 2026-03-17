package com.tellpal.v2.user.web.mobile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tellpal.v2.user.api.AuthenticatedAppUser;
import com.tellpal.v2.user.api.UserAuthenticationException;
import com.tellpal.v2.user.api.UserResolutionApi;
import com.tellpal.v2.user.application.UserApplicationExceptions.UserProfileNotFoundException;
import com.tellpal.v2.user.application.UserProfileResults.UserProfileRecord;
import com.tellpal.v2.user.application.UserProfileService;
import com.tellpal.v2.shared.web.admin.AdminApiExceptionHandler;
import com.tellpal.v2.shared.web.admin.AdminAuthenticationFacade;
import com.tellpal.v2.shared.web.admin.AdminProblemDetailsFactory;

@WebMvcTest(ProfileMobileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        AuthenticatedMobileUserResolver.class,
        UserMobileExceptionHandler.class,
        AdminApiExceptionHandler.class,
        AdminProblemDetailsFactory.class
})
class ProfileMobileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private UserResolutionApi userResolutionApi;

    @MockitoBean
    private AdminAuthenticationFacade adminAuthenticationFacade;

    @Test
    void listProfilesReturnsProfilesForCurrentUser() throws Exception {
        when(userResolutionApi.resolveOrCreateByIdToken("stub:mobile-user"))
                .thenReturn(new AuthenticatedAppUser(41L, 410L, "firebase-41", false));
        when(userProfileService.listProfiles(41L)).thenReturn(List.of(
                new UserProfileRecord(
                        41L,
                        410L,
                        "Moon",
                        "6-8",
                        22L,
                        List.of("story"),
                        List.of("sleep"),
                        true)));

        mockMvc.perform(get("/api/profiles")
                        .header("Authorization", "Bearer stub:mobile-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(41))
                .andExpect(jsonPath("$[0].profileId").value(410))
                .andExpect(jsonPath("$[0].displayName").value("Moon"))
                .andExpect(jsonPath("$[0].avatarMediaId").value(22))
                .andExpect(jsonPath("$[0].primary").value(true));
    }

    @Test
    void getProfileReturnsNotFoundProblemDetailsWhenProfileIsMissing() throws Exception {
        when(userResolutionApi.resolveOrCreateByIdToken("stub:mobile-user"))
                .thenReturn(new AuthenticatedAppUser(41L, 410L, "firebase-41", false));
        when(userProfileService.getProfile(41L, 999L))
                .thenThrow(new UserProfileNotFoundException(41L, 999L));

        mockMvc.perform(get("/api/profiles/999")
                        .header("Authorization", "Bearer stub:mobile-user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User profile not found"))
                .andExpect(jsonPath("$.errorCode").value("user_profile_not_found"))
                .andExpect(jsonPath("$.path").value("/api/profiles/999"));
    }

    @Test
    void updateProfileDelegatesToApplicationService() throws Exception {
        when(userResolutionApi.resolveOrCreateByIdToken("stub:mobile-user"))
                .thenReturn(new AuthenticatedAppUser(41L, 410L, "firebase-41", false));
        when(userProfileService.updateProfile(any())).thenReturn(new UserProfileRecord(
                41L,
                410L,
                "Luna",
                "7-9",
                35L,
                List.of("fantasy", "nature"),
                List.of("focus"),
                true));

        mockMvc.perform(put("/api/profiles/410")
                        .header("Authorization", "Bearer stub:mobile-user")
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "Luna",
                                  "ageRange": "7-9",
                                  "avatarMediaId": 35,
                                  "favoriteGenres": ["fantasy", "nature"],
                                  "mainPurposes": ["focus"],
                                  "primary": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Luna"))
                .andExpect(jsonPath("$.favoriteGenres[0]").value("fantasy"))
                .andExpect(jsonPath("$.mainPurposes[0]").value("focus"))
                .andExpect(jsonPath("$.primary").value(true));

        verify(userProfileService).updateProfile(org.mockito.ArgumentMatchers.argThat(command ->
                command.userId().equals(41L)
                        && command.profileId().equals(410L)
                        && "Luna".equals(command.displayName())
                        && "7-9".equals(command.ageRange())
                        && command.avatarMediaId().equals(35L)
                        && command.favoriteGenres().equals(List.of("fantasy", "nature"))
                        && command.mainPurposes().equals(List.of("focus"))
                        && command.primary()));
    }

    @Test
    void missingAuthorizationHeaderReturnsBadRequestProblemDetails() throws Exception {
        mockMvc.perform(get("/api/profiles"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errorCode").value("invalid_request"))
                .andExpect(jsonPath("$.path").value("/api/profiles"));
    }

    @Test
    void invalidFirebaseTokenReturnsUnauthorizedProblemDetails() throws Exception {
        when(userResolutionApi.resolveOrCreateByIdToken(eq("stub:expired-token")))
                .thenThrow(new UserAuthenticationException("Token expired"));

        mockMvc.perform(get("/api/profiles")
                        .header("Authorization", "Bearer stub:expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Firebase token"))
                .andExpect(jsonPath("$.detail").value("Token expired"))
                .andExpect(jsonPath("$.errorCode").value("firebase_auth_error"));
    }

    @Test
    void updateProfileRejectsInvalidBody() throws Exception {
        when(userResolutionApi.resolveOrCreateByIdToken("stub:mobile-user"))
                .thenReturn(new AuthenticatedAppUser(41L, 410L, "firebase-41", false));

        mockMvc.perform(put("/api/profiles/410")
                        .header("Authorization", "Bearer stub:mobile-user")
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "Luna",
                                  "ageRange": "",
                                  "favoriteGenres": ["fantasy"],
                                  "mainPurposes": ["focus"],
                                  "primary": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errorCode").value("validation_error"))
                .andExpect(jsonPath("$.fieldErrors.ageRange").value("ageRange is required"));
    }
}
