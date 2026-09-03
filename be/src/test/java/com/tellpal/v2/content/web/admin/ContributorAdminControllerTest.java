package com.tellpal.v2.content.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.time.Instant;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentContributorNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContributorInUseException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContributorNotFoundException;
import com.tellpal.v2.content.application.ContributorManagementResults.ContentContributorRecord;
import com.tellpal.v2.content.application.ContributorManagementResults.ContributorRecord;
import com.tellpal.v2.content.application.ContributorRegistryReadResults;
import com.tellpal.v2.content.application.ContributorManagementService;
import com.tellpal.v2.content.domain.ContributorRole;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiExceptionHandler;
import com.tellpal.v2.shared.web.admin.AdminAuthenticationFacade;
import com.tellpal.v2.shared.web.admin.AdminProblemDetailsFactory;

@WebMvcTest(ContributorAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ContentAdminExceptionHandler.class, AdminApiExceptionHandler.class, AdminProblemDetailsFactory.class})
class ContributorAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContributorManagementService contributorManagementService;

    @MockitoBean
    private AdminAuthenticationFacade adminAuthenticationFacade;

    @Test
    void createContributorReturnsCreatedResponse() throws Exception {
        when(contributorManagementService.createContributor(any())).thenReturn(
                new ContributorRecord(11L, "Elif Yilmaz", Set.of(ContributorRole.AUTHOR, ContributorRole.MUSICIAN)));

        mockMvc.perform(post("/api/admin/contributors")
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "Elif Yilmaz",
                                  "roles": ["AUTHOR", "MUSICIAN"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/admin/contributors/11"))
                .andExpect(jsonPath("$.contributorId").value(11))
                .andExpect(jsonPath("$.displayName").value("Elif Yilmaz"))
                .andExpect(jsonPath("$.roles", Matchers.containsInAnyOrder("AUTHOR", "MUSICIAN")));
    }

    @Test
    void listContributorsReturnsArray() throws Exception {
        when(contributorManagementService.listContributors(2, null)).thenReturn(List.of(
                new ContributorRecord(12L, "Baris Kaya"),
                new ContributorRecord(11L, "Elif Yilmaz")));

        mockMvc.perform(get("/api/admin/contributors").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].displayName").value("Baris Kaya"))
                .andExpect(jsonPath("$[1].displayName").value("Elif Yilmaz"));
    }

    @Test
    void contributorRegistryReturnsEnvelopeAndDelegatesFilters() throws Exception {
        when(contributorManagementService.listContributorRegistry("Ada", ContributorRole.AUTHOR, 1, 10))
                .thenReturn(new ContributorRegistryReadResults.Page(List.of(
                        new ContributorRegistryReadResults.Item(8L, "Ada Lovelace",
                                Set.of(ContributorRole.AUTHOR), 3,
                                Map.of(ContributorRole.AUTHOR, 3L), Instant.parse("2026-09-03T10:00:00Z"))),
                        1, 10, 11));

        mockMvc.perform(get("/api/admin/contributor-registry")
                        .param("q", "  Ada ").param("role", "AUTHOR").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(11))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[0].totalUsageCount").value(3))
                .andExpect(jsonPath("$.items[0].usageByRole.AUTHOR").value(3));
    }

    @Test
    void contributorRegistryRejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/admin/contributor-registry").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation_error"));
        mockMvc.perform(get("/api/admin/contributor-registry").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation_error"));
        mockMvc.perform(get("/api/admin/contributor-registry").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation_error"));
        mockMvc.perform(get("/api/admin/contributor-registry").param("role", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation_error"));
    }

    @Test
    void listContributorsTrimsSearchQueryBeforeDelegating() throws Exception {
        when(contributorManagementService.listContributors(2, "eli")).thenReturn(List.of(
                new ContributorRecord(11L, "Elif Yilmaz")));

        mockMvc.perform(get("/api/admin/contributors")
                        .param("limit", "2")
                        .param("q", "  eli  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("Elif Yilmaz"));

        verify(contributorManagementService).listContributors(2, "eli");
    }

    @Test
    void listContributorsRejectsNonPositiveLimit() throws Exception {
        mockMvc.perform(get("/api/admin/contributors").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation_error"));
    }

    @Test
    void renameContributorReturnsUpdatedResponse() throws Exception {
        when(contributorManagementService.renameContributor(any())).thenReturn(new ContributorRecord(11L, "Elif Kaya"));

        mockMvc.perform(put("/api/admin/contributors/11")
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "Elif Kaya",
                                  "roles": ["AUTHOR"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contributorId").value(11))
                .andExpect(jsonPath("$.displayName").value("Elif Kaya"));
    }

    @Test
    void deleteContributorReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/contributors/11"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listContentContributorsReturnsAssignments() throws Exception {
        when(contributorManagementService.listContentContributors(51L)).thenReturn(List.of(
                new ContentContributorRecord(
                        51L,
                        11L,
                        "Elif Yilmaz",
                        ContributorRole.AUTHOR,
                        LanguageCode.TR,
                        "E. Yilmaz",
                        0)));

        mockMvc.perform(get("/api/admin/contents/51/contributors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contributorId").value(11))
                .andExpect(jsonPath("$[0].languageCode").value("tr"));
    }

    @Test
    void assignContributorReturnsCreatedResponse() throws Exception {
        when(contributorManagementService.assignContentContributor(any())).thenReturn(new ContentContributorRecord(
                51L,
                11L,
                "Elif Yilmaz",
                ContributorRole.AUTHOR,
                LanguageCode.TR,
                "E. Yilmaz",
                0));

        mockMvc.perform(post("/api/admin/contents/51/contributors")
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": 11,
                                  "role": "AUTHOR",
                                  "languageCode": "tr",
                                  "creditName": "E. Yilmaz",
                                  "sortOrder": 0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentId").value(51))
                .andExpect(jsonPath("$.contributorId").value(11))
                .andExpect(jsonPath("$.role").value("AUTHOR"))
                .andExpect(jsonPath("$.languageCode").value("tr"));
    }

    @Test
    void assignContributorAllowsGlobalScopeWhenLanguageCodeIsNull() throws Exception {
        when(contributorManagementService.assignContentContributor(any())).thenReturn(new ContentContributorRecord(
                51L,
                11L,
                "Elif Yilmaz",
                ContributorRole.ILLUSTRATOR,
                null,
                null,
                0));

        mockMvc.perform(post("/api/admin/contents/51/contributors")
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": 11,
                                  "role": "ILLUSTRATOR",
                                  "languageCode": null,
                                  "creditName": null,
                                  "sortOrder": 0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentId").value(51))
                .andExpect(jsonPath("$.contributorId").value(11))
                .andExpect(jsonPath("$.role").value("ILLUSTRATOR"))
                .andExpect(jsonPath("$.languageCode").value(Matchers.nullValue()));
    }

    @Test
    void unassignContributorReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/contents/51/contributors")
                        .param("contributorId", "11")
                        .param("role", "AUTHOR")
                        .param("languageCode", "tr"))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingContributorBecomesNotFoundProblemDetails() throws Exception {
        when(contributorManagementService.renameContributor(any())).thenThrow(new ContributorNotFoundException(77L));

        mockMvc.perform(put("/api/admin/contributors/77")
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "Missing",
                                  "roles": ["AUTHOR"]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Contributor not found"))
                .andExpect(jsonPath("$.errorCode").value("contributor_not_found"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void contributorInUseBecomesConflictProblemDetails() throws Exception {
        doThrow(new ContributorInUseException(11L))
                .when(contributorManagementService)
                .deleteContributor(any());

        mockMvc.perform(delete("/api/admin/contributors/11"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("contributor_in_use"));
    }

    @Test
    void duplicateContributorNameReturnsExistingContributorId() throws Exception {
        doThrow(new com.tellpal.v2.content.application.ContentApplicationExceptions.DuplicateContributorDisplayNameException(12L))
                .when(contributorManagementService).createContributor(any());

        mockMvc.perform(post("/api/admin/contributors").contentType("application/json")
                        .content("{\"displayName\":\"Elif\",\"roles\":[\"AUTHOR\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.existingContributorId").value(12));
    }

    @Test
    void missingContentContributorBecomesNotFoundProblemDetails() throws Exception {
        doThrow(new ContentContributorNotFoundException(51L, 11L, ContributorRole.AUTHOR, LanguageCode.TR))
                .when(contributorManagementService)
                .unassignContentContributor(any());

        mockMvc.perform(delete("/api/admin/contents/51/contributors")
                        .param("contributorId", "11")
                        .param("role", "AUTHOR")
                        .param("languageCode", "tr"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("content_contributor_not_found"));
    }
}
