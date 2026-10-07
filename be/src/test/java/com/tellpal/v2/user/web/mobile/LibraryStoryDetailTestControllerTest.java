package com.tellpal.v2.user.web.mobile;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.tellpal.v2.user.application.LibraryHomeTestQueryService;
import com.tellpal.v2.user.api.*;
import com.tellpal.v2.category.api.*;

class LibraryStoryDetailTestControllerTest {
    final UserResolutionApi users=mock(UserResolutionApi.class);
    final LibraryStoryDetailQueryApi details=mock(LibraryStoryDetailQueryApi.class);
    MockMvc mvc;
    @BeforeEach void setup() {
        mvc=MockMvcBuilders.standaloneSetup(new LibraryHomeTestController(new LibraryHomeTestQueryService(users,mock(LibraryHomeQueryApi.class),details,"allowed,subscriber","subscriber")))
            .setControllerAdvice(new LibraryHomeTestExceptionHandler()).build();
        when(users.resolveOrCreateByIdToken("valid")).thenReturn(new AuthenticatedAppUser(1L,1L,"allowed",false));
    }
    @Test void missingInvalidAndDeniedAdmissionNeverQueriesDetail() throws Exception {
        mvc.perform(get("/api/test/library/stories/1")).andExpect(status().isUnauthorized());
        when(users.resolveOrCreateByIdToken("bad")).thenThrow(new UserAuthenticationException("private"));
        mvc.perform(get("/api/test/library/stories/1").header("Authorization","Bearer bad")).andExpect(status().isUnauthorized());
        when(users.resolveOrCreateByIdToken("denied")).thenReturn(new AuthenticatedAppUser(1L,1L,"denied",false));
        mvc.perform(get("/api/test/library/stories/1").header("Authorization","Bearer denied")).andExpect(status().isForbidden());
        verifyNoInteractions(details);
    }
    @Test void unavailableAndInvalidRequestsHaveStableRedactedErrors() throws Exception {
        when(details.query(2,1,"tr","en","READING",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER)).thenReturn(Optional.empty());
        mvc.perform(get("/api/test/library/stories/1").header("Authorization","Bearer valid").param("categoryId","2").param("lang","tr").param("contentLang","en").param("experience","READING"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("content_not_found"));
        when(details.query(0,1,null,null,null,LibraryHomeQueryApi.Audience.NON_SUBSCRIBER)).thenThrow(new IllegalArgumentException("private"));
        mvc.perform(get("/api/test/library/stories/1").header("Authorization","Bearer valid"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("invalid_request"));
        mvc.perform(get("/api/test/library/stories/oops").header("Authorization","Bearer valid"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("invalid_request"));
    }
}
