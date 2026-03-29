package com.tellpal.v2.content.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import com.tellpal.v2.content.api.AdminContentLocalizationView;
import com.tellpal.v2.content.api.AdminContentQueryApi;
import com.tellpal.v2.content.api.AdminContentView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentNotFoundException;
import com.tellpal.v2.content.application.ContentManagementResults.ContentLocalizationRecord;
import com.tellpal.v2.content.application.ContentManagementResults.StoryPageRecord;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.application.ContentPublicationService;
import com.tellpal.v2.content.application.StoryPageManagementService;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiExceptionHandler;
import com.tellpal.v2.shared.web.admin.AdminAuthenticationFacade;
import com.tellpal.v2.shared.web.admin.AdminProblemDetailsFactory;

@WebMvcTest({ContentAdminController.class, StoryPageAdminController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({ContentAdminExceptionHandler.class, AdminApiExceptionHandler.class, AdminProblemDetailsFactory.class})
class ContentAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentManagementService contentManagementService;

    @MockitoBean
    private AdminContentQueryApi adminContentQueryApi;

    @MockitoBean
    private StoryPageManagementService storyPageManagementService;

    @MockitoBean
    private ContentPublicationService contentPublicationService;

    @MockitoBean
    private Clock clock;

    @MockitoBean
    private AdminAuthenticationFacade adminAuthenticationFacade;

    @Test
    void createContentReturnsCreatedResponse() throws Exception {
        when(contentManagementService.createContent(any())).thenReturn(new ContentReference(
                51L,
                ContentApiType.STORY,
                "moonlight-story",
                true,
                5,
                0));

        mockMvc.perform(post("/api/admin/contents")
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "STORY",
                                  "externalKey": "moonlight-story",
                                  "ageRange": 5,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/admin/contents/51"))
                .andExpect(jsonPath("$.contentId").value(51))
                .andExpect(jsonPath("$.type").value("STORY"))
                .andExpect(jsonPath("$.externalKey").value("moonlight-story"))
                .andExpect(jsonPath("$.pageCount").value(0));
    }

    @Test
    void listContentsReturnsMetadataAndLocalizationSnapshots() throws Exception {
        when(adminContentQueryApi.listContents()).thenReturn(List.of(new AdminContentView(
                51L,
                ContentApiType.STORY,
                "moonlight-story",
                false,
                5,
                2,
                List.of(new AdminContentLocalizationView(
                        51L,
                        LanguageCode.TR,
                        "Ay Isigi",
                        "Gece masali",
                        null,
                        11L,
                        null,
                        8,
                        "PUBLISHED",
                        "COMPLETED",
                        Instant.parse("2026-03-17T09:00:00Z"),
                        true)))));

        mockMvc.perform(get("/api/admin/contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contentId").value(51))
                .andExpect(jsonPath("$[0].active").value(false))
                .andExpect(jsonPath("$[0].localizations[0].languageCode").value("tr"))
                .andExpect(jsonPath("$[0].localizations[0].processingStatus").value("COMPLETED"));
    }

    @Test
    void getMissingContentReturnsNotFoundProblemDetails() throws Exception {
        when(adminContentQueryApi.findContent(88L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/contents/88"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Content not found"))
                .andExpect(jsonPath("$.errorCode").value("content_not_found"));
    }

    @Test
    void createLocalizationReturnsCreatedResponse() throws Exception {
        when(contentManagementService.createLocalization(any())).thenReturn(new ContentLocalizationRecord(
                51L,
                LanguageCode.TR,
                "Ay Isigi",
                "Gece masali",
                null,
                11L,
                null,
                8,
                LocalizationStatus.PUBLISHED,
                ProcessingStatus.PENDING,
                Instant.parse("2026-03-17T09:00:00Z"),
                false));

        mockMvc.perform(post("/api/admin/contents/51/localizations/tr")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Ay Isigi",
                                  "description": "Gece masali",
                                  "coverMediaId": 11,
                                  "durationMinutes": 8,
                                  "status": "PUBLISHED",
                                  "processingStatus": "PENDING",
                                  "publishedAt": "2026-03-17T09:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/admin/contents/51/localizations/tr"))
                .andExpect(jsonPath("$.contentId").value(51))
                .andExpect(jsonPath("$.languageCode").value("tr"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.visibleToMobile").value(false));
    }

    @Test
    void addStoryPageReturnsCreatedResponse() throws Exception {
        when(storyPageManagementService.addStoryPage(any())).thenReturn(new StoryPageRecord(51L, 1, 88L, 0));

        mockMvc.perform(post("/api/admin/contents/51/story-pages")
                        .contentType("application/json")
                        .content("""
                                {
                                  "pageNumber": 1,
                                  "illustrationMediaId": 88
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/admin/contents/51/story-pages/1"))
                .andExpect(jsonPath("$.contentId").value(51))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.illustrationMediaId").value(88));
    }

    @Test
    void missingContentBecomesNotFoundProblemDetails() throws Exception {
        when(contentManagementService.updateContent(any())).thenThrow(new ContentNotFoundException(51L));

        mockMvc.perform(put("/api/admin/contents/51")
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "missing-content",
                                  "ageRange": 4,
                                  "active": true
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Content not found"))
                .andExpect(jsonPath("$.errorCode").value("content_not_found"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/admin/contents/51"));
    }

    @Test
    void storyPageStateConflictBecomesConflictProblemDetails() throws Exception {
        when(storyPageManagementService.addStoryPage(any()))
                .thenThrow(new IllegalStateException("Story pages can only be managed for STORY content"));

        mockMvc.perform(post("/api/admin/contents/91/story-pages")
                        .contentType("application/json")
                        .content("""
                                {
                                  "pageNumber": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Content state conflict"))
                .andExpect(jsonPath("$.errorCode").value("content_state_conflict"));
    }

    @Test
    void processingPatchReturnsUpdatedLocalization() throws Exception {
        when(contentManagementService.markLocalizationProcessingStatus(any())).thenReturn(new ContentLocalizationRecord(
                51L,
                LanguageCode.EN,
                "Moonlight",
                "Calming audio",
                "Breathe slowly.",
                21L,
                34L,
                9,
                LocalizationStatus.PUBLISHED,
                ProcessingStatus.COMPLETED,
                Instant.parse("2026-03-17T09:00:00Z"),
                true));

        mockMvc.perform(patch("/api/admin/contents/51/localizations/en/processing-status")
                        .contentType("application/json")
                        .content("""
                                {
                                  "processingStatus": "COMPLETED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCode").value("en"))
                .andExpect(jsonPath("$.processingStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.visibleToMobile").value(true));
    }

    @Test
    void publishLocalizationReturnsUpdatedLocalization() throws Exception {
        when(contentPublicationService.publishLocalization(any())).thenReturn(new ContentLocalizationRecord(
                51L,
                LanguageCode.TR,
                "Ay Isigi",
                "Gece masali",
                null,
                11L,
                null,
                8,
                LocalizationStatus.PUBLISHED,
                ProcessingStatus.PENDING,
                Instant.parse("2026-03-17T09:00:00Z"),
                false));

        mockMvc.perform(post("/api/admin/contents/51/localizations/tr/publish")
                        .contentType("application/json")
                        .content("""
                                {
                                  "publishedAt": "2026-03-17T09:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCode").value("tr"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").value("2026-03-17T09:00:00Z"));
    }

    @Test
    void publishLocalizationWithoutBodyUsesClockInstant() throws Exception {
        Instant publishedAt = Instant.parse("2026-03-17T10:15:00Z");
        when(clock.instant()).thenReturn(publishedAt);
        when(contentPublicationService.publishLocalization(any())).thenReturn(new ContentLocalizationRecord(
                51L,
                LanguageCode.TR,
                "Ay Isigi",
                "Gece masali",
                null,
                11L,
                null,
                8,
                LocalizationStatus.PUBLISHED,
                ProcessingStatus.PENDING,
                publishedAt,
                false));

        mockMvc.perform(post("/api/admin/contents/51/localizations/tr/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedAt").value("2026-03-17T10:15:00Z"));

        verify(contentPublicationService).publishLocalization(argThat(command ->
                command.contentId().equals(51L)
                        && command.languageCode() == LanguageCode.TR
                        && command.publishedAt().equals(publishedAt)));
    }

    @Test
    void archiveLocalizationReturnsUpdatedLocalization() throws Exception {
        when(contentPublicationService.archiveLocalization(any())).thenReturn(new ContentLocalizationRecord(
                51L,
                LanguageCode.TR,
                "Ay Isigi",
                "Gece masali",
                null,
                11L,
                null,
                8,
                LocalizationStatus.ARCHIVED,
                ProcessingStatus.COMPLETED,
                Instant.parse("2026-03-17T09:00:00Z"),
                false));

        mockMvc.perform(post("/api/admin/contents/51/localizations/tr/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCode").value("tr"))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }
}
