package com.tellpal.v2.user.application;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import com.tellpal.v2.user.api.*;
import com.tellpal.v2.category.api.*;
class LibraryHomeTestQueryServiceTest {
    private final UserResolutionApi users=mock(UserResolutionApi.class);
    private final LibraryHomeQueryApi library=mock(LibraryHomeQueryApi.class);
    private final LibraryHomeTestQueryService service=new LibraryHomeTestQueryService(users,library,org.mockito.Mockito.mock(com.tellpal.v2.category.api.LibraryStoryDetailQueryApi.class),"allowed,subscriber","subscriber");
    @Test void rejectsMissingInvalidAndUnlistedIdentityBeforeAnyCatalogCall() {
        assertThatThrownBy(()->service.query(null,"tr")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("401");
        when(users.resolveOrCreateByIdToken("bad")).thenThrow(new UserAuthenticationException("Invalid token"));
        assertThatThrownBy(()->service.query("Bearer bad","tr")).hasMessageContaining("401");
        when(users.resolveOrCreateByIdToken("other")).thenReturn(new AuthenticatedAppUser(1L,1L,"unlisted",false));
        assertThatThrownBy(()->service.query("Bearer other","tr")).hasMessageContaining("403");
        verifyNoInteractions(library);
    }
    @Test void selectsAudienceOnlyAfterVerifiedIdentityAndRejectsInvalidLanguage() {
        when(users.resolveOrCreateByIdToken("valid")).thenReturn(new AuthenticatedAppUser(1L,1L,"subscriber",false));
        when(library.query("tr",LibraryHomeQueryApi.Audience.SUBSCRIBER)).thenReturn(new LibraryHomeCatalog(1,"tr",LibraryHomeQueryApi.Audience.SUBSCRIBER,5,List.of()));
        assertThat(service.query("Bearer valid","tr").audience()).isEqualTo(LibraryHomeQueryApi.Audience.SUBSCRIBER);
        when(library.query(null,LibraryHomeQueryApi.Audience.SUBSCRIBER)).thenThrow(new IllegalArgumentException());
        assertThatThrownBy(()->service.query("Bearer valid",null)).hasMessageContaining("400");
    }
    @Test void subscriberFixturesMustBeAnAllowlistSubset() {
        assertThatThrownBy(()->new LibraryHomeTestQueryService(users,library,org.mockito.Mockito.mock(com.tellpal.v2.category.api.LibraryStoryDetailQueryApi.class),"","subscriber")).isInstanceOf(IllegalArgumentException.class);
    }
}
