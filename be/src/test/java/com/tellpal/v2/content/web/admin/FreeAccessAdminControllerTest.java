package com.tellpal.v2.content.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentFreeAccessAlreadyExistsException;
import com.tellpal.v2.content.application.ContentFreeAccessResults.ContentFreeAccessRecord;
import com.tellpal.v2.content.application.ContentFreeAccessService;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiExceptionHandler;
import com.tellpal.v2.shared.web.admin.AdminAuthenticationFacade;
import com.tellpal.v2.shared.web.admin.AdminProblemDetailsFactory;

@WebMvcTest(FreeAccessAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ContentAdminExceptionHandler.class, AdminApiExceptionHandler.class, AdminProblemDetailsFactory.class})
class FreeAccessAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentFreeAccessService contentFreeAccessService;

    @MockitoBean
    private AdminAuthenticationFacade adminAuthenticationFacade;

    @Test
    void createFreeAccessReturnsCreatedResponse() throws Exception {
        when(contentFreeAccessService.grantFreeAccess(any())).thenReturn(
                new ContentFreeAccessRecord(12L, "default", 51L, LanguageCode.TR));

        mockMvc.perform(post("/api/admin/free-access")
                        .contentType("application/json")
                        .content("""
                                {
                                  "accessKey": "default",
                                  "contentId": 51,
                                  "languageCode": "tr"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/api/admin/free-access/default/languages/tr/contents/51"))
                .andExpect(jsonPath("$.freeAccessId").value(12))
                .andExpect(jsonPath("$.accessKey").value("default"))
                .andExpect(jsonPath("$.languageCode").value("tr"));
    }

    @Test
    void listFreeAccessEntriesReturnsArray() throws Exception {
        when(contentFreeAccessService.listFreeAccessEntries("default")).thenReturn(List.of(
                new ContentFreeAccessRecord(12L, "default", 51L, LanguageCode.TR),
                new ContentFreeAccessRecord(13L, "default", 52L, LanguageCode.EN)));

        mockMvc.perform(get("/api/admin/free-access").param("accessKey", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].contentId").value(51))
                .andExpect(jsonPath("$[1].languageCode").value("en"));
    }

    @Test
    void deleteFreeAccessReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/free-access/default/languages/tr/contents/51"))
                .andExpect(status().isNoContent());
    }

    @Test
    void duplicateFreeAccessReturnsConflictProblemDetails() throws Exception {
        when(contentFreeAccessService.grantFreeAccess(any()))
                .thenThrow(new ContentFreeAccessAlreadyExistsException("default", 51L, LanguageCode.TR));

        mockMvc.perform(post("/api/admin/free-access")
                        .contentType("application/json")
                        .content("""
                                {
                                  "accessKey": "default",
                                  "contentId": 51,
                                  "languageCode": "tr"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Content free access already exists"))
                .andExpect(jsonPath("$.errorCode").value("content_free_access_exists"));
    }
}
