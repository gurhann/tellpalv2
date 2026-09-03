package com.tellpal.v2.content.web.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.support.AdminApiIntegrationTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
class ContributorAdminIntegrationTest extends AdminApiIntegrationTestSupport {

    @Autowired
    private ContentManagementService contentManagementService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    admin_refresh_tokens,
                    admin_user_roles,
                    admin_users,
                    content_contributors,
                    contributor_roles,
                    contributors,
                    content_localizations,
                    story_page_localizations,
                    story_pages,
                    contents,
                    media_assets
                restart identity cascade
                """);
    }

    @Test
    void contributorCreateListRenameAndAssignWorkWithAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "forest-story", 5, true));

        MvcResult createResult = mockMvc.perform(post("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "Elif Yilmaz",
                                  "roles": ["AUTHOR", "MUSICIAN"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Long contributorId = readPayload(createResult).get("contributorId").asLong();

        mockMvc.perform(get("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contributorId").value(contributorId));

        mockMvc.perform(put("/api/admin/contributors/{contributorId}", contributorId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "Elif Kaya",
                                  "roles": ["AUTHOR"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Elif Kaya"));

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "languageCode": "tr",
                                  "creditName": "E. Kaya",
                                  "sortOrder": 0
                                }
                                """.formatted(contributorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contributorId").value(contributorId))
                .andExpect(jsonPath("$.role").value("AUTHOR"));
    }

    @Test
    void contributorRegistryFiltersPaginatesAndReportsUsage() throws Exception {
        String accessToken = authenticateAdmin();
        Long adaId = createContributor(accessToken, "Ada Lovelace", "AUTHOR", "MUSICIAN");
        Long graceId = createContributor(accessToken, "Grace Hopper", "AUTHOR");
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "registry-story", 1, true));
        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken).contentType("application/json")
                        .content("{\"contributorId\":%d,\"role\":\"AUTHOR\",\"sortOrder\":0}".formatted(adaId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/contributor-registry")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("q", " ada ").param("role", "MUSICIAN").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].contributorId").value(adaId))
                .andExpect(jsonPath("$.items[0].totalUsageCount").value(1))
                .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(get("/api/admin/contributor-registry")
                        .header("Authorization", "Bearer " + accessToken).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation_error"));
    }

    @Test
    void contributorRegistryUsesDefaultMetadataEmptyTotalsAndIdTieBreak() throws Exception {
        String accessToken = authenticateAdmin();
        Long firstId = createContributor(accessToken, "Tie First");
        Long secondId = createContributor(accessToken, "Tie Second");
        jdbcTemplate.update("update contributors set updated_at = ? where id in (?, ?)",
                java.sql.Timestamp.valueOf("2026-09-03 12:00:00"), firstId, secondId);

        mockMvc.perform(get("/api/admin/contributor-registry")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].contributorId").value(secondId))
                .andExpect(jsonPath("$.items[1].contributorId").value(firstId));

        mockMvc.perform(get("/api/admin/contributor-registry")
                        .header("Authorization", "Bearer " + accessToken).param("q", "does-not-exist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void contributorRegistryPagesAllMatchesAndAppliesRoleFilter() throws Exception {
        String token = authenticateAdmin();
        Long one = createContributor(token, "Page Ada One", "AUTHOR");
        Long two = createContributor(token, "Page Ada Two", "AUTHOR");
        Long three = createContributor(token, "Page Ada Three", "MUSICIAN");
        Long other = createContributor(token, "Page Ada Other", "NARRATOR");

        mockMvc.perform(get("/api/admin/contributor-registry").header("Authorization", "Bearer " + token)
                        .param("q", "page ada").param("size", "2").param("page", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalItems").value(4)).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[0].contributorId").value(other))
                .andExpect(jsonPath("$.items[1].contributorId").value(three));
        mockMvc.perform(get("/api/admin/contributor-registry").header("Authorization", "Bearer " + token)
                        .param("q", "page ada").param("size", "2").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalItems").value(4))
                .andExpect(jsonPath("$.items[0].contributorId").value(two))
                .andExpect(jsonPath("$.items[1].contributorId").value(one));
        mockMvc.perform(get("/api/admin/contributor-registry").header("Authorization", "Bearer " + token)
                        .param("role", "MUSICIAN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].contributorId").value(three));
    }

    @Test
    void contributorRegistryReportsUsageForEveryProfileRole() throws Exception {
        String token = authenticateAdmin();
        Long contributorId = createContributor(token, "Usage Both Roles", "AUTHOR", "MUSICIAN");
        ContentReference first = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "usage-author", 1, true));
        ContentReference second = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "usage-musician", 1, true));
        assign(token, first.contentId(), contributorId, "AUTHOR");
        assign(token, second.contentId(), contributorId, "MUSICIAN");

        mockMvc.perform(get("/api/admin/contributor-registry").header("Authorization", "Bearer " + token)
                        .param("q", "Usage Both Roles"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].totalUsageCount").value(2))
                .andExpect(jsonPath("$.items[0].usageByRole.AUTHOR").value(1))
                .andExpect(jsonPath("$.items[0].usageByRole.MUSICIAN").value(1));
    }

    private void assign(String token, Long contentId, Long contributorId, String role) throws Exception {
        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", contentId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"contributorId\":%d,\"role\":\"%s\",\"sortOrder\":0}"
                                .formatted(contributorId, role)))
                .andExpect(status().isCreated());
    }

    @Test
    void contributorListCanSearchDisplayNamesWithAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();
        createContributor(accessToken, "Aylin Demir");
        Long matchingContributorId = createContributor(accessToken, "Elif Kaya");
        createContributor(accessToken, "Baris Kaya");

        mockMvc.perform(get("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("q", "elif")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contributorId").value(matchingContributorId));
    }

    @Test
    void contributorValidationReturnsProblemDetails() throws Exception {
        String accessToken = authenticateAdmin();

        mockMvc.perform(post("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "",
                                  "roles": ["AUTHOR"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation_error"))
                .andExpect(jsonPath("$.fieldErrors.displayName").value("displayName is required"));
    }

    @Test
    void contributorRolesAreRequiredNonEmptyAndUniqueWithoutWritingAProfile() throws Exception {
        String accessToken = authenticateAdmin();

        mockMvc.perform(post("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"displayName\":\"Missing roles\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation_error"));
        mockMvc.perform(post("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"displayName\":\"Empty roles\",\"roles\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation_error"));
        mockMvc.perform(post("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"displayName\":\"Duplicate roles\",\"roles\":[\"AUTHOR\",\"AUTHOR\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation_error"));

        mockMvc.perform(get("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void normalizedDuplicateNameReturnsExistingIdAndOwnRenameIsAllowed() throws Exception {
        String accessToken = authenticateAdmin();
        Long contributorId = createContributor(accessToken, "Ada Lovelace");
        Long secondContributorId = createContributor(accessToken, "Grace Hopper", "MUSICIAN");

        mockMvc.perform(post("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"displayName\":\"  ada lovelace  \",\"roles\":[\"MUSICIAN\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("duplicate_contributor_display_name"))
                .andExpect(jsonPath("$.existingContributorId").value(contributorId));

        mockMvc.perform(put("/api/admin/contributors/{contributorId}", contributorId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"displayName\":\"  ADA LOVELACE  \",\"roles\":[\"AUTHOR\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("ADA LOVELACE"));

        mockMvc.perform(put("/api/admin/contributors/{contributorId}", secondContributorId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"displayName\":\"ada lovelace\",\"roles\":[\"AUTHOR\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.existingContributorId").value(contributorId));

        mockMvc.perform(get("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("q", "grace hopper")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contributorId").value(secondContributorId))
                .andExpect(jsonPath("$[0].displayName").value("Grace Hopper"))
                .andExpect(jsonPath("$[0].roles", Matchers.contains("MUSICIAN")));
    }

    @Test
    void usedRoleCannotBeRemovedAndUnusedRoleCanBeRemoved() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "role-use-story", 4, true));
        Long contributorId = createContributor(accessToken, "Role Protected", "AUTHOR", "MUSICIAN");

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {"contributorId": %d, "role": "MUSICIAN", "sortOrder": 0}
                                """.formatted(contributorId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {"contributorId": %d, "role": "MUSICIAN", "languageCode": "tr", "sortOrder": 0}
                                """.formatted(contributorId)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/admin/contributors/{contributorId}", contributorId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"displayName\":\"Changed\",\"roles\":[\"AUTHOR\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("contributor_role_in_use"))
                .andExpect(jsonPath("$.role").value("MUSICIAN"))
                .andExpect(jsonPath("$.usageCount").value(2))
                .andExpect(jsonPath("$.affectedContents.length()").value(1))
                .andExpect(jsonPath("$.affectedContents[0].contentId").value(content.contentId()))
                .andExpect(jsonPath("$.affectedContents[0].externalKey").value("role-use-story"));

        mockMvc.perform(get("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("limit", "10"))
                .andExpect(jsonPath("$[0].displayName").value("Role Protected"))
                .andExpect(jsonPath("$[0].roles", Matchers.containsInAnyOrder("AUTHOR", "MUSICIAN")));

        mockMvc.perform(put("/api/admin/contributors/{contributorId}", contributorId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"displayName\":\"Role Protected\",\"roles\":[\"MUSICIAN\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", Matchers.contains("MUSICIAN")));
    }

    @Test
    void globalContributorAssignmentWorksWithoutContentLocalizations() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "global-credit-story", 4, true));
        Long contributorId = createContributor(accessToken, "Global Author");

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "creditName": "Global Author",
                                  "sortOrder": 0
                                }
                                """.formatted(contributorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contributorId").value(contributorId))
                .andExpect(jsonPath("$.role").value("AUTHOR"))
                .andExpect(jsonPath("$.languageCode").value(Matchers.nullValue()));
    }

    @Test
    void sameContributorCanHaveGlobalAndLocalizedCreditsForTheSameRole() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "mixed-credit-story", 4, true));
        Long contributorId = createContributor(accessToken, "Mixed Scope Author");

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "creditName": "Global Author",
                                  "sortOrder": 0
                                }
                                """.formatted(contributorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.languageCode").value(Matchers.nullValue()));

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "languageCode": "en",
                                  "creditName": "English Author",
                                  "sortOrder": 0
                                }
                                """.formatted(contributorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.languageCode").value("en"));
    }

    @Test
    void blankLanguageCodeIsRejectedWhenGlobalScopeIsRequestedIncorrectly() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "blank-language-story", 4, true));
        Long contributorId = createContributor(accessToken, "Blank Language Author");

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "languageCode": "   ",
                                  "creditName": null,
                                  "sortOrder": 0
                                }
                                """.formatted(contributorId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("invalid_request"))
                .andExpect(jsonPath("$.detail").value("languageCode must not be blank when provided"));
    }

    @Test
    void duplicateGlobalScopeAssignmentAndSortOrderStillFail() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "duplicate-global-story", 4, true));
        Long authorId = createContributor(accessToken, "Duplicate Global Author");
        Long illustratorId = createContributor(accessToken, "Duplicate Global Illustrator");

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "creditName": null,
                                  "sortOrder": 0
                                }
                                """.formatted(authorId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "creditName": null,
                                  "sortOrder": 0
                                }
                                """.formatted(authorId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("invalid_request"));

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "creditName": null,
                                  "sortOrder": 0
                                }
                                """.formatted(illustratorId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("invalid_request"));
    }

    @Test
    void listAndUnassignContentContributorsRoundTripWithAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "list-contributor-story", 4, true));
        Long contributorId = createContributor(accessToken, "List Author");

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "languageCode": "tr",
                                  "creditName": "List Author",
                                  "sortOrder": 0
                                }
                                """.formatted(contributorId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contributorId").value(contributorId))
                .andExpect(jsonPath("$[0].languageCode").value("tr"));

        mockMvc.perform(delete("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .param("contributorId", contributorId.toString())
                        .param("role", "AUTHOR")
                        .param("languageCode", "tr"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unassignMissingContributorAssignmentReturnsNotFoundProblemDetail() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "missing-assignment-story", 4, true));
        Long contributorId = createContributor(accessToken, "Missing Assignment");

        mockMvc.perform(delete("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .param("contributorId", contributorId.toString())
                        .param("role", "AUTHOR")
                        .param("languageCode", "tr"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("content_contributor_not_found"));
    }

    @Test
    void deleteContributorRequiresAssignmentsToBeRemovedFirst() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "delete-contributor-story", 4, true));
        Long contributorId = createContributor(accessToken, "Delete Protected");

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "creditName": null,
                                  "sortOrder": 0
                                }
                                """.formatted(contributorId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/admin/contributors/{contributorId}", contributorId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("contributor_in_use"));

        mockMvc.perform(delete("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .param("contributorId", contributorId.toString())
                        .param("role", "AUTHOR"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/admin/contributors/{contributorId}", contributorId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].contributorId", Matchers.not(Matchers.hasItem(contributorId.intValue()))));
    }

    private Long createContributor(String accessToken, String displayName) throws Exception {
        return createContributor(accessToken, displayName, "AUTHOR");
    }

    private Long createContributor(String accessToken, String displayName, String... roles) throws Exception {
        String rolesJson = java.util.Arrays.stream(roles)
                .map(role -> "\"" + role + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        MvcResult createResult = mockMvc.perform(post("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {"displayName": "%s", "roles": [%s]}
                                """.formatted(displayName, rolesJson)))
                .andExpect(status().isCreated())
                .andReturn();

        return readPayload(createResult).get("contributorId").asLong();
    }
}
