package com.tellpal.v2.event.web.mobile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tellpal.v2.event.api.EventTrackingApi;
import com.tellpal.v2.event.api.EventTrackingResults.EventBatchIngestResult;
import com.tellpal.v2.event.api.EventTrackingResults.EventIngestReceipt;
import com.tellpal.v2.event.api.EventTrackingResults.EventIngestStatus;
import com.tellpal.v2.event.api.EventTrackingResults.EventStream;
import com.tellpal.v2.event.application.EventApplicationExceptions.ReferencedContentNotFoundException;
import com.tellpal.v2.user.api.AuthenticatedAppUser;
import com.tellpal.v2.user.api.UserAuthenticationException;
import com.tellpal.v2.user.api.UserResolutionApi;
import com.tellpal.v2.shared.web.admin.AdminApiExceptionHandler;
import com.tellpal.v2.shared.web.admin.AdminAuthenticationFacade;
import com.tellpal.v2.shared.web.admin.AdminProblemDetailsFactory;

@WebMvcTest(EventMobileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        AuthenticatedMobileEventUserResolver.class,
        EventMobileExceptionHandler.class,
        AdminApiExceptionHandler.class,
        AdminProblemDetailsFactory.class
})
class EventMobileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventTrackingApi eventTrackingApi;

    @MockitoBean
    private UserResolutionApi userResolutionApi;

    @MockitoBean
    private AdminAuthenticationFacade adminAuthenticationFacade;

    @Test
    void recordContentEventReturnsReceipt() throws Exception {
        UUID eventId = UUID.fromString("1b2c3d4e-1111-2222-3333-444455556666");
        when(userResolutionApi.resolveOrCreateByIdToken("stub:event-user"))
                .thenReturn(new AuthenticatedAppUser(51L, 510L, "firebase-51", false));
        when(eventTrackingApi.recordContentEvent(any())).thenReturn(new EventIngestReceipt(
                eventId,
                EventStream.CONTENT,
                EventIngestStatus.RECORDED,
                Instant.parse("2026-03-17T11:00:00Z")));

        mockMvc.perform(post("/api/events/content")
                        .header("Authorization", "Bearer stub:event-user")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventId": "1b2c3d4e-1111-2222-3333-444455556666",
                                  "contentId": 91,
                                  "languageCode": "tr",
                                  "eventType": "START",
                                  "occurredAt": "2026-03-17T10:59:30Z",
                                  "sessionId": "9a9a9a9a-1111-2222-3333-444444444444",
                                  "engagementSeconds": 12,
                                  "metadata": {
                                    "platform": "ios"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.stream").value("CONTENT"))
                .andExpect(jsonPath("$.status").value("RECORDED"))
                .andExpect(jsonPath("$.ingestedAt").value("2026-03-17T11:00:00Z"));

        verify(eventTrackingApi).recordContentEvent(org.mockito.ArgumentMatchers.argThat(command ->
                command.profileId().equals(510L)
                        && command.contentId().equals(91L)
                        && command.languageCode().value().equals("tr")
                        && command.metadata().equals(Map.of("platform", "ios"))));
    }

    @Test
    void recordAppEventReturnsBadRequestForUnknownContent() throws Exception {
        when(userResolutionApi.resolveOrCreateByIdToken("stub:event-user"))
                .thenReturn(new AuthenticatedAppUser(51L, 510L, "firebase-51", false));
        when(eventTrackingApi.recordAppEvent(any()))
                .thenThrow(new ReferencedContentNotFoundException(91L));

        mockMvc.perform(post("/api/events/app")
                        .header("Authorization", "Bearer stub:event-user")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventId": "2b2c3d4e-1111-2222-3333-444455556666",
                                  "eventType": "LOCKED_CONTENT_CLICKED",
                                  "contentId": 91,
                                  "occurredAt": "2026-03-17T10:59:30Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid content reference"))
                .andExpect(jsonPath("$.errorCode").value("content_not_found"));
    }

    @Test
    void batchEndpointReturnsAggregateCounts() throws Exception {
        when(userResolutionApi.resolveOrCreateByIdToken("stub:event-user"))
                .thenReturn(new AuthenticatedAppUser(51L, 510L, "firebase-51", false));
        when(eventTrackingApi.recordBatchEvents(any())).thenReturn(new EventBatchIngestResult(List.of(
                new EventIngestReceipt(
                        UUID.fromString("3b2c3d4e-1111-2222-3333-444455556666"),
                        EventStream.CONTENT,
                        EventIngestStatus.RECORDED,
                        Instant.parse("2026-03-17T11:01:00Z")),
                new EventIngestReceipt(
                        UUID.fromString("4b2c3d4e-1111-2222-3333-444455556666"),
                        EventStream.APP,
                        EventIngestStatus.DUPLICATE_EVENT_ID,
                        Instant.parse("2026-03-17T11:01:05Z")))));

        mockMvc.perform(post("/api/events/batch")
                        .header("Authorization", "Bearer stub:event-user")
                        .contentType("application/json")
                        .content("""
                                {
                                  "contentEvents": [
                                    {
                                      "eventId": "3b2c3d4e-1111-2222-3333-444455556666",
                                      "contentId": 91,
                                      "languageCode": "en",
                                      "eventType": "COMPLETE",
                                      "occurredAt": "2026-03-17T11:00:00Z"
                                    }
                                  ],
                                  "appEvents": [
                                    {
                                      "eventId": "4b2c3d4e-1111-2222-3333-444455556666",
                                      "eventType": "APP_OPENED",
                                      "occurredAt": "2026-03-17T11:00:05Z"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordedCount").value(1))
                .andExpect(jsonPath("$.duplicateCount").value(1))
                .andExpect(jsonPath("$.receipts.length()").value(2))
                .andExpect(jsonPath("$.receipts[1].status").value("DUPLICATE_EVENT_ID"));
    }

    @Test
    void invalidFirebaseTokenReturnsUnauthorizedProblemDetails() throws Exception {
        when(userResolutionApi.resolveOrCreateByIdToken(eq("stub:expired-token")))
                .thenThrow(new UserAuthenticationException("Token expired"));

        mockMvc.perform(post("/api/events/app")
                        .header("Authorization", "Bearer stub:expired-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventId": "5b2c3d4e-1111-2222-3333-444455556666",
                                  "eventType": "APP_OPENED",
                                  "occurredAt": "2026-03-17T11:00:05Z"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Firebase token"))
                .andExpect(jsonPath("$.errorCode").value("firebase_auth_error"));
    }
}
