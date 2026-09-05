package com.tellpal.v2.content.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.application.ContributorManagementCommands.AssignContentContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.ContributorRole;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.support.AdminApiIntegrationTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
class ContributorAdminIntegrationTest extends AdminApiIntegrationTestSupport {

    @Autowired
    private ContentManagementService contentManagementService;

    @Autowired
    private ContributorManagementService contributorManagementService;

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
        addLocalization(content, LanguageCode.TR);

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
                .andExpect(jsonPath("$.role").value("AUTHOR"))
                .andExpect(jsonPath("$.contributorRoles", Matchers.contains("AUTHOR")));
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
        addLocalization(content, LanguageCode.TR);
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
        addLocalization(content, LanguageCode.EN);
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
    void duplicateAssignmentsFailButLegacyClientSortOrderIsIgnored() throws Exception {
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("content_contributor_assignment_exists"));

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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sortOrder").value(1))
                .andExpect(jsonPath("$.contributorRoles", Matchers.contains("AUTHOR")))
                .andExpect(jsonPath("$.assignmentId").isNumber());
    }

    @Test
    void reorderRequiresAnExactGroupPermutationAndLeavesTheStoredOrderUntouchedWhenInvalid() throws Exception {
        String token = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "reorder-contributors", 1, true));
        Long first = createContributor(token, "Reorder First");
        Long second = createContributor(token, "Reorder Second");
        Long narrator = createContributor(token, "Reorder Narrator", "NARRATOR");
        assign(token, content.contentId(), first, "AUTHOR");
        assign(token, content.contentId(), second, "AUTHOR");
        assign(token, content.contentId(), narrator, "NARRATOR");
        java.util.List<Long> assignmentIds = jdbcTemplate.queryForList(
                "select id from content_contributors where content_id = ? and role = 'AUTHOR' order by sort_order",
                Long.class, content.contentId());
        Long foreignAssignmentId = jdbcTemplate.queryForObject(
                "select id from content_contributors where content_id = ? and role = 'NARRATOR'",
                Long.class, content.contentId());

        mockMvc.perform(put("/api/admin/contents/{contentId}/contributors/reorder", content.contentId())
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"role\":\"AUTHOR\",\"assignmentIds\":[%d,%d]}"
                                .formatted(assignmentIds.get(1), assignmentIds.get(0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignmentId").value(assignmentIds.get(1)))
                .andExpect(jsonPath("$[0].sortOrder").value(0));

        mockMvc.perform(put("/api/admin/contents/{contentId}/contributors/reorder", content.contentId())
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"role\":\"AUTHOR\",\"assignmentIds\":[%d,%d]}"
                                .formatted(assignmentIds.get(1), assignmentIds.get(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("invalid_request"));

        mockMvc.perform(put("/api/admin/contents/{contentId}/contributors/reorder", content.contentId())
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"role\":\"AUTHOR\",\"assignmentIds\":[%d]}"
                                .formatted(assignmentIds.get(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("invalid_request"));

        mockMvc.perform(put("/api/admin/contents/{contentId}/contributors/reorder", content.contentId())
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"role\":\"AUTHOR\",\"assignmentIds\":[%d,%d]}"
                                .formatted(assignmentIds.get(1), foreignAssignmentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("invalid_request"));

        assertThat(jdbcTemplate.queryForList(
                "select id from content_contributors where content_id = ? and role = 'AUTHOR' order by sort_order",
                Long.class, content.contentId()))
                .containsExactly(assignmentIds.get(1), assignmentIds.get(0));
    }

    @Test
    void assignmentRejectsAProfileRoleMismatchWithoutWritingAnAssignment() throws Exception {
        String token = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "role-mismatch", 1, true));
        Long contributorId = createContributor(token, "Author Only", "AUTHOR");

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"contributorId\":%d,\"role\":\"NARRATOR\"}".formatted(contributorId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("contributor_role_not_supported"));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from content_contributors where content_id = ?", Integer.class, content.contentId()))
                .isZero();
    }

    @Test
    void concurrentAssignmentsSerializeAtTheContentAggregateAndProduceConsecutiveOrders() throws Exception {
        String token = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "concurrent-contributors", 1, true));
        Long first = createContributor(token, "Concurrent First");
        Long second = createContributor(token, "Concurrent Second");
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.concurrent.Future<?> firstWrite = executor.submit(() -> assignAfterConcurrentStart(
                    ready, start, content.contentId(), first));
            java.util.concurrent.Future<?> secondWrite = executor.submit(() -> assignAfterConcurrentStart(
                    ready, start, content.contentId(), second));
            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            firstWrite.get();
            secondWrite.get();
        } finally {
            executor.shutdownNow();
        }
        assertThat(jdbcTemplate.queryForList(
                "select sort_order from content_contributors where content_id = ? order by sort_order",
                Integer.class, content.contentId())).containsExactly(0, 1);
    }

    private void assignAfterConcurrentStart(
            java.util.concurrent.CountDownLatch ready,
            java.util.concurrent.CountDownLatch start,
            Long contentId,
            Long contributorId) {
        ready.countDown();
        try {
            if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent assignment start timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent assignment was interrupted", exception);
        }
        contributorManagementService.assignContentContributor(
                new AssignContentContributorCommand(contentId, contributorId, ContributorRole.AUTHOR, null, null, 0));
    }

    private void addLocalization(ContentReference content, LanguageCode languageCode) {
        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(), languageCode, "Test title", null, null, null, null, null,
                LocalizationStatus.DRAFT, ProcessingStatus.PENDING, null));
    }

    @Test
    void contributorSortOrderMigrationNormalizesExistingGapsPerRoleAndLanguageGroup() throws Exception {
        String token = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "migration-contributors", 1, true));
        Long first = createContributor(token, "Migration First");
        Long second = createContributor(token, "Migration Second");
        assign(token, content.contentId(), first, "AUTHOR");
        assign(token, content.contentId(), second, "AUTHOR");
        jdbcTemplate.update("update content_contributors set sort_order = 2147483647 where content_id = ? and contributor_id = ?",
                content.contentId(), first);
        jdbcTemplate.update("update content_contributors set sort_order = 14 where content_id = ? and contributor_id = ?",
                content.contentId(), second);

        String migration = new org.springframework.core.io.ClassPathResource(
                "db/migration/V21__normalize_content_contributor_sort_orders.sql").getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        jdbcTemplate.execute(migration);

        assertThat(jdbcTemplate.queryForList(
                "select sort_order from content_contributors where content_id = ? and role = 'AUTHOR' order by sort_order",
                Integer.class, content.contentId())).containsExactly(0, 1);
    }

    @Test
    void listAndUnassignContentContributorsRoundTripWithAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "list-contributor-story", 4, true));
        addLocalization(content, LanguageCode.TR);
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
                .andExpect(jsonPath("$[0].contributorRoles", Matchers.contains("AUTHOR")))
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
    void updateContentContributorPreservesAssignmentIdAndAppendsToTheTargetRoleGroup() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "edit-contributor-story", 4, true));
        Long firstContributor = createContributor(accessToken, "Edit First", "AUTHOR", "MUSICIAN");
        Long secondContributor = createContributor(accessToken, "Edit Second", "MUSICIAN");

        MvcResult firstAssignmentResult = mockMvc.perform(
                post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"contributorId\":%d,\"role\":\"AUTHOR\"}"
                                .formatted(firstContributor)))
                .andExpect(status().isCreated())
                .andReturn();
        Long assignmentId = readPayload(firstAssignmentResult).get("assignmentId").asLong();

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"contributorId\":%d,\"role\":\"MUSICIAN\"}"
                                .formatted(secondContributor)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/admin/contents/{contentId}/contributors/{assignmentId}",
                        content.contentId(), assignmentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"role\":\"MUSICIAN\",\"creditName\":\"Edited First\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.contributorId").value(firstContributor))
                .andExpect(jsonPath("$.role").value("MUSICIAN"))
                .andExpect(jsonPath("$.creditName").value("Edited First"))
                .andExpect(jsonPath("$.sortOrder").value(1));

        assertThat(jdbcTemplate.queryForList(
                "select contributor_id from content_contributors where content_id = ? and role = 'MUSICIAN' order by sort_order",
                Long.class, content.contentId()))
                .containsExactly(secondContributor, firstContributor);
    }

    @Test
    void updateContentContributorPersistsExistingLocalizedScopeAndRejectsUnknownLanguage() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "edit-localized-contributor", 4, true));
        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(), LanguageCode.TR, "Masal", null, null, null, null, null,
                LocalizationStatus.DRAFT, ProcessingStatus.PENDING, null));
        Long contributorId = createContributor(accessToken, "Localized Author", "AUTHOR");

        MvcResult assignmentResult = mockMvc.perform(
                post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"contributorId\":%d,\"role\":\"AUTHOR\"}"
                                .formatted(contributorId)))
                .andExpect(status().isCreated())
                .andReturn();
        Long assignmentId = readPayload(assignmentResult).get("assignmentId").asLong();

        mockMvc.perform(put("/api/admin/contents/{contentId}/contributors/{assignmentId}",
                        content.contentId(), assignmentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"role\":\"AUTHOR\",\"languageCode\":\"tr\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.languageCode").value("tr"));

        assertThat(jdbcTemplate.queryForObject(
                "select language_code from content_contributors where id = ?",
                String.class, assignmentId)).isEqualTo("tr");

        mockMvc.perform(put("/api/admin/contents/{contentId}/contributors/{assignmentId}",
                        content.contentId(), assignmentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"role\":\"AUTHOR\",\"languageCode\":\"de\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("content_contributor_language_not_found"));

        assertThat(jdbcTemplate.queryForObject(
                "select language_code from content_contributors where id = ?",
                String.class, assignmentId)).isEqualTo("tr");
    }

    @Test
    void updateMissingContentContributorAssignmentReturnsNotFoundProblemDetail() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "missing-edit-contributor", 4, true));

        mockMvc.perform(put("/api/admin/contents/{contentId}/contributors/{assignmentId}",
                        content.contentId(), 999L)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"role\":\"AUTHOR\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("content_contributor_assignment_not_found"));
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
