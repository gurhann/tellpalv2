package com.tellpal.v2.user.web.mobile;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tellpal.v2.category.api.LibraryHomeQueryApi;
import com.tellpal.v2.user.api.AuthenticatedAppUser;
import com.tellpal.v2.user.api.UserAuthenticationException;
import com.tellpal.v2.user.api.UserResolutionApi;
import com.tellpal.v2.user.application.LibraryHomeTestQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LibraryHomeTestControllerTest {
    private final UserResolutionApi users = mock(UserResolutionApi.class);
    private final LibraryHomeQueryApi library = mock(LibraryHomeQueryApi.class);
    private MockMvc mvc;

    @BeforeEach void configure() {
        var service = new LibraryHomeTestQueryService(users, library,org.mockito.Mockito.mock(com.tellpal.v2.category.api.LibraryStoryDetailQueryApi.class), "allowed", "");
        mvc = MockMvcBuilders.standaloneSetup(new LibraryHomeTestController(service))
                .setControllerAdvice(new LibraryHomeTestExceptionHandler()).build();
    }

    @Test void missingTokenIsProblemDetailBeforeAnyCatalogQuery() throws Exception {
        mvc.perform(get("/api/test/library").param("lang", "tr"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Firebase bearer token is required"))
                .andExpect(jsonPath("$.errorCode").value("firebase_auth_error"))
                .andExpect(jsonPath("$.path").value("/api/test/library"));
        verifyNoInteractions(users, library);
    }

    @Test void invalidTokenIsProblemDetailAndNeverQueriesCatalog() throws Exception {
        when(users.resolveOrCreateByIdToken("invalid"))
                .thenThrow(new UserAuthenticationException("private verifier detail"));
        mvc.perform(get("/api/test/library").param("lang", "tr")
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Firebase authentication is invalid"));
        verifyNoInteractions(library);
    }

    @Test void verifiedUnlistedIdentityIsProblemDetailAndNeverQueriesCatalog() throws Exception {
        when(users.resolveOrCreateByIdToken("valid"))
                .thenReturn(new AuthenticatedAppUser(1L, 1L, "unlisted", false));
        mvc.perform(get("/api/test/library").param("lang", "tr")
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("library_test_access_denied"));
        verifyNoInteractions(library);
    }

    @Test void unavailableLanguageIsProblemDetailAfterAuthorizedResolution() throws Exception {
        when(users.resolveOrCreateByIdToken("valid"))
                .thenReturn(new AuthenticatedAppUser(1L, 1L, "allowed", false));
        when(library.query("xx", LibraryHomeQueryApi.Audience.NON_SUBSCRIBER))
                .thenThrow(new IllegalArgumentException("invalid language"));
        mvc.perform(get("/api/test/library").param("lang", "xx")
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("library_language_invalid"));
    }
}
