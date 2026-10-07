package com.tellpal.v2.user.web.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import com.tellpal.v2.category.api.LibraryHomeCatalog;
import com.tellpal.v2.category.api.LibraryHomeQueryApi;
import com.tellpal.v2.user.api.AuthenticatedAppUser;
import com.tellpal.v2.user.api.UserResolutionApi;
import com.tellpal.v2.user.application.LibraryHomeTestQueryService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Exercises actual Spring profile conditions and MVC registration without external adapters. */
class LibraryHomeProfileContextTest {
    @ParameterizedTest
    @ValueSource(strings = {"production", "prod", "default"})
    void productionAndDefaultExcludeBothLibraryBeansAndTheMapping(String profile) throws Exception {
        try (var context = context(profile)) {
            assertThat(context.getBeansOfType(LibraryHomeTestController.class)).isEmpty();
            assertThat(context.getBeansOfType(LibraryHomeTestQueryService.class)).isEmpty();
            assertThat(hasLibraryMapping(context)).isFalse();
            MockMvcBuilders.webAppContextSetup(context).build()
                    .perform(get("/api/test/library").param("lang", "tr").header("Authorization", "Bearer fixture"))
                    .andExpect(status().isNotFound());
            verifyNoInteractions(context.getBean(UserResolutionApi.class), context.getBean(LibraryHomeQueryApi.class));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "test"})
    void localAndTestRegisterTheActualGuardedServiceAndController(String profile) throws Exception {
        try (var context = context(profile)) {
            assertThat(context.getBeansOfType(LibraryHomeTestController.class)).hasSize(1);
            assertThat(context.getBeansOfType(LibraryHomeTestQueryService.class)).hasSize(1);
            assertThat(hasLibraryMapping(context)).isTrue();
            var users = context.getBean(UserResolutionApi.class);
            var library = context.getBean(LibraryHomeQueryApi.class);
            var mvc = MockMvcBuilders.webAppContextSetup(context).build();
            mvc.perform(get("/api/test/library").param("lang", "tr"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
            verifyNoInteractions(users, library);
            when(users.resolveOrCreateByIdToken("unlisted-fixture"))
                    .thenReturn(new AuthenticatedAppUser(1L, 1L, "unlisted", false));
            mvc.perform(get("/api/test/library").param("lang", "tr")
                            .header("Authorization", "Bearer unlisted-fixture"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
            verifyNoInteractions(library);
            when(users.resolveOrCreateByIdToken("allowed-fixture"))
                    .thenReturn(new AuthenticatedAppUser(2L, 2L, "allowed", false));
            when(library.query("tr", LibraryHomeQueryApi.Audience.NON_SUBSCRIBER))
                    .thenReturn(new LibraryHomeCatalog(1, "tr", LibraryHomeQueryApi.Audience.NON_SUBSCRIBER, 5, List.of()));
            mvc.perform(get("/api/test/library").param("lang", "tr")
                            .header("Authorization", "Bearer allowed-fixture"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.requestedLanguage").value("tr"))
                    .andExpect(jsonPath("$.rows").isEmpty());
            verify(library).query("tr", LibraryHomeQueryApi.Audience.NON_SUBSCRIBER);
        }
    }

    private boolean hasLibraryMapping(AnnotationConfigWebApplicationContext context) {
        return context.getBean(RequestMappingHandlerMapping.class).getHandlerMethods().keySet().stream()
                .anyMatch(mapping -> mapping.getPatternValues().contains("/api/test/library"));
    }

    private AnnotationConfigWebApplicationContext context(String profile) {
        var context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().setActiveProfiles(profile);
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                "tellpal.library.test.allowed-firebase-uids=allowed",
                "tellpal.library.test.subscriber-firebase-uids=");
        context.register(LibraryMvc.class);
        context.refresh();
        return context;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({LibraryHomeTestController.class, LibraryHomeTestQueryService.class, LibraryHomeTestExceptionHandler.class})
    static class LibraryMvc {
        @Bean UserResolutionApi users() { return mock(UserResolutionApi.class); }
        @Bean com.tellpal.v2.category.api.LibraryStoryDetailQueryApi details() { return mock(com.tellpal.v2.category.api.LibraryStoryDetailQueryApi.class); }
        @Bean LibraryHomeQueryApi library() { return mock(LibraryHomeQueryApi.class); }
    }
}
