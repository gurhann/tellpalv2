package com.tellpal.v2.event.web.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest
@AutoConfigureMockMvc
class EventMobileIntegrationTest extends PostgresIntegrationTestBase {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ContentManagementService contentManagementService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    app_events,
                    content_events,
                    content_free_access,
                    category_contents,
                    category_localizations,
                    categories,
                    content_contributors,
                    contributors,
                    story_page_localizations,
                    story_pages,
                    content_localizations,
                    contents,
                    user_profiles,
                    app_users
                restart identity cascade
                """);
    }

    @Test
    void duplicateContentEventIdReturnsDuplicateReceiptWithoutSecondInsert() throws Exception {
        Long contentId = createContent("event-story-duplicate-id");
        UUID eventId = UUID.fromString("a4ec1db2-1111-2222-3333-444455556666");

        postContentEvent(eventId, contentId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECORDED"));

        postContentEvent(eventId, contentId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE_EVENT_ID"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from content_events", Integer.class)).isEqualTo(1);
    }

    @Test
    void duplicateLegacyEventKeyReturnsLegacyDuplicateReceipt() throws Exception {
        Long contentId = createContent("event-story-legacy-key");
        String legacyEventKey = "firebase-legacy-41";

        postContentEvent(
                UUID.fromString("b4ec1db2-1111-2222-3333-444455556666"),
                contentId,
                legacyEventKey)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECORDED"));

        postContentEvent(
                UUID.fromString("c4ec1db2-1111-2222-3333-444455556666"),
                contentId,
                legacyEventKey)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE_LEGACY_EVENT_KEY"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from content_events", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select legacy_event_key from content_events",
                String.class)).isEqualTo(legacyEventKey);
    }

    @Test
    void batchEndpointProcessesMixedEventsAndReportsDuplicateCounts() throws Exception {
        Long contentId = createContent("event-story-batch");
        UUID duplicateAppEventId = UUID.fromString("d4ec1db2-1111-2222-3333-444455556666");

        mockMvc.perform(post("/api/events/app")
                        .header("Authorization", "Bearer stub:event-user")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventId": "%s",
                                  "eventType": "APP_OPENED",
                                  "occurredAt": "2026-03-17T11:00:00Z"
                                }
                                """.formatted(duplicateAppEventId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECORDED"));

        MvcResult result = mockMvc.perform(post("/api/events/batch")
                        .header("Authorization", "Bearer stub:event-user")
                        .contentType("application/json")
                        .content("""
                                {
                                  "contentEvents": [
                                    {
                                      "eventId": "e4ec1db2-1111-2222-3333-444455556666",
                                      "contentId": %d,
                                      "languageCode": "tr",
                                      "eventType": "COMPLETE",
                                      "occurredAt": "2026-03-17T11:01:00Z"
                                    }
                                  ],
                                  "appEvents": [
                                    {
                                      "eventId": "%s",
                                      "eventType": "APP_OPENED",
                                      "occurredAt": "2026-03-17T11:01:05Z"
                                    }
                                  ]
                                }
                                """.formatted(contentId, duplicateAppEventId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordedCount").value(1))
                .andExpect(jsonPath("$.duplicateCount").value(1))
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsByteArray());

        assertThat(payload.get("receipts")).hasSize(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from content_events", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from app_events", Integer.class)).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.ResultActions postContentEvent(
            UUID eventId,
            Long contentId,
            String legacyEventKey) throws Exception {
        String legacyField = legacyEventKey == null ? "" : """
                ,
                "legacyEventKey": "%s"
                """.formatted(legacyEventKey);
        return mockMvc.perform(post("/api/events/content")
                .header("Authorization", "Bearer stub:event-user")
                .contentType("application/json")
                .content("""
                        {
                          "eventId": "%s",
                          "contentId": %d,
                          "languageCode": "tr",
                          "eventType": "EXIT",
                          "occurredAt": "2026-03-17T11:00:00Z",
                          "leftPage": 4
                          %s
                        }
                        """.formatted(eventId, contentId, legacyField)));
    }

    private Long createContent(String externalKey) {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, externalKey, 5, true));
        return content.contentId();
    }
}
