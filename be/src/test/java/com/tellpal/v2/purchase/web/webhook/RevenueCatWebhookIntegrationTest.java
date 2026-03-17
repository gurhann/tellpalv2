package com.tellpal.v2.purchase.web.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.event.api.EventTrackingApi;
import com.tellpal.v2.event.api.EventTrackingCommands.RecordAppEventCommand;
import com.tellpal.v2.event.domain.AppEventType;
import com.tellpal.v2.purchase.api.PurchaseCatalogApi;
import com.tellpal.v2.purchase.api.PurchaseCatalogCommands.CreateSubscriptionProductCommand;
import com.tellpal.v2.purchase.domain.BillingPeriodUnit;
import com.tellpal.v2.purchase.domain.SubscriptionProductType;
import com.tellpal.v2.support.PostgresIntegrationTestBase;
import com.tellpal.v2.user.domain.AppUser;
import com.tellpal.v2.user.domain.AppUserRepository;

@SpringBootTest(properties = "tellpal.purchase.revenuecat.authorization-header=Bearer rc-secret")
@AutoConfigureMockMvc
class RevenueCatWebhookIntegrationTest extends PostgresIntegrationTestBase {

    private static final String AUTHORIZATION_HEADER = "Bearer rc-secret";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PurchaseCatalogApi purchaseCatalogApi;

    @Autowired
    private EventTrackingApi eventTrackingApi;

    @Autowired
    private ContentManagementService contentManagementService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    purchase_context_snapshots,
                    purchase_events,
                    subscription_products,
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
    void invalidAuthorizationHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/webhooks/revenuecat")
                        .header("Authorization", "Bearer wrong-secret")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(samplePayload(
                                "rc-event-auth-fail",
                                "unknown-user",
                                "PLAY_STORE",
                                "monthly-premium",
                                Instant.parse("2026-03-17T12:00:00Z")))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("revenuecat_authorization_failed"));
    }

    @Test
    void duplicateRevenueCatEventIdReturnsDuplicateStatusWithoutSecondInsert() throws Exception {
        createUser("purchase-user-1");
        registerProduct("PLAY_STORE", "monthly-premium");
        byte[] payload = objectMapper.writeValueAsBytes(samplePayload(
                "rc-event-duplicate",
                "purchase-user-1",
                "PLAY_STORE",
                "monthly-premium",
                Instant.parse("2026-03-17T12:00:00Z")));

        mockMvc.perform(post("/api/webhooks/revenuecat")
                        .header("Authorization", AUTHORIZATION_HEADER)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECORDED"));

        mockMvc.perform(post("/api/webhooks/revenuecat")
                        .header("Authorization", AUTHORIZATION_HEADER)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE_EVENT_ID"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from purchase_events", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from purchase_context_snapshots", Integer.class)).isEqualTo(1);
    }

    @Test
    void unknownLookupValueReturnsUnprocessableEntity() throws Exception {
        createUser("purchase-user-lookup");

        mockMvc.perform(post("/api/webhooks/revenuecat")
                        .header("Authorization", AUTHORIZATION_HEADER)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(samplePayload(
                                "rc-event-unknown-store",
                                "purchase-user-lookup",
                                "NOT_A_STORE",
                                "monthly-premium",
                                Instant.parse("2026-03-17T12:00:00Z")))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("purchase_lookup_invalid"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from purchase_events", Integer.class)).isZero();
    }

    @Test
    void lockedContentClickTakesPrecedenceOverNewerPaywallShown() throws Exception {
        AppUser user = createUser("purchase-user-attribution");
        Long profileId = primaryProfileId(user.getId());
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "premium-story", 6, true));
        registerProduct("PLAY_STORE", "monthly-premium");

        UUID lockedClickEventId = UUID.fromString("2a3d6d35-0101-4201-9001-111111111111");
        UUID paywallEventId = UUID.fromString("2a3d6d35-0101-4201-9001-222222222222");
        eventTrackingApi.recordAppEvent(new RecordAppEventCommand(
                lockedClickEventId,
                profileId,
                AppEventType.LOCKED_CONTENT_CLICKED,
                content.contentId(),
                Instant.parse("2026-03-17T10:00:00Z"),
                Map.of("source", "integration"),
                null));
        eventTrackingApi.recordAppEvent(new RecordAppEventCommand(
                paywallEventId,
                profileId,
                AppEventType.PAYWALL_SHOWN,
                null,
                Instant.parse("2026-03-17T11:00:00Z"),
                Map.of("source", "integration"),
                null));

        mockMvc.perform(post("/api/webhooks/revenuecat")
                        .header("Authorization", AUTHORIZATION_HEADER)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(samplePayload(
                                "rc-event-attribution",
                                "purchase-user-attribution",
                                "PLAY_STORE",
                                "monthly-premium",
                                Instant.parse("2026-03-17T12:00:00Z")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECORDED"));

        assertThat(jdbcTemplate.queryForObject(
                "select attributed_app_event_id from purchase_context_snapshots",
                UUID.class)).isEqualTo(lockedClickEventId);
        assertThat(jdbcTemplate.queryForObject(
                "select attributed_content_id from purchase_context_snapshots",
                Long.class)).isEqualTo(content.contentId());
        assertThat(jdbcTemplate.queryForObject(
                "select app_user_id from purchase_events",
                Long.class)).isEqualTo(user.getId());
    }

    private AppUser createUser(String firebaseUid) {
        return appUserRepository.save(AppUser.create(firebaseUid, false));
    }

    private Long primaryProfileId(Long userId) {
        return jdbcTemplate.queryForObject(
                "select id from user_profiles where app_user_id = ? and is_primary = true",
                Long.class,
                userId);
    }

    private void registerProduct(String storeCode, String productId) {
        purchaseCatalogApi.createProduct(new CreateSubscriptionProductCommand(
                storeCode,
                productId,
                SubscriptionProductType.SUBSCRIPTION,
                BillingPeriodUnit.MONTH,
                1,
                List.of("premium")));
    }

    private Map<String, Object> samplePayload(
            String revenueCatEventId,
            String appUserId,
            String storeCode,
            String productId,
            Instant occurredAt) {
        long epochMillis = occurredAt.toEpochMilli();
        LinkedHashMap<String, Object> event = new LinkedHashMap<>();
        event.put("id", revenueCatEventId);
        event.put("type", "INITIAL_PURCHASE");
        event.put("app_user_id", appUserId);
        event.put("aliases", List.of(appUserId));
        event.put("original_app_user_id", appUserId);
        event.put("store", storeCode);
        event.put("product_id", productId);
        event.put("entitlement_id", "premium");
        event.put("entitlement_ids", List.of("premium"));
        event.put("period_type", "NORMAL");
        event.put("environment", "PRODUCTION");
        event.put("currency", "USD");
        event.put("price", 9.99);
        event.put("price_in_purchased_currency", 9.99);
        event.put("transaction_id", "txn-" + revenueCatEventId);
        event.put("country_code", "US");
        event.put("event_timestamp_ms", epochMillis);
        event.put("purchased_at_ms", epochMillis);
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("api_version", "1.0");
        payload.put("event", event);
        return payload;
    }
}
