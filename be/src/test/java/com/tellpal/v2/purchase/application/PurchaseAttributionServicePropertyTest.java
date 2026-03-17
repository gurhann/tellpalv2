package com.tellpal.v2.purchase.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.api.ContentLookupApi;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.event.api.AppEventAttributionCandidate;
import com.tellpal.v2.event.api.AttributionAppEventType;
import com.tellpal.v2.event.api.EventAttributionApi;
import com.tellpal.v2.purchase.domain.PurchaseContextSnapshot;
import com.tellpal.v2.purchase.domain.PurchaseContextSnapshotRepository;
import com.tellpal.v2.purchase.domain.PurchaseEvent;
import com.tellpal.v2.purchase.domain.PurchaseEventRepository;
import com.tellpal.v2.purchase.domain.PurchaseSource;
import com.tellpal.v2.user.api.AppUserProfileReference;
import com.tellpal.v2.user.api.AppUserReference;
import com.tellpal.v2.user.api.UserLookupApi;

class PurchaseAttributionServicePropertyTest {

    private static final Instant PURCHASE_TIME = Instant.parse("2026-03-17T12:00:00Z");

    @Property(tries = 80)
    void lockedContentClickWinsEvenWhenPaywallShownIsMoreRecent(
            @ForAll @IntRange(min = 2, max = 24) int lockedHoursBeforePurchase,
            @ForAll @IntRange(min = 1, max = 1) int paywallHoursBeforePurchase) {
        UUID lockedClickEventId = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
        UUID paywallEventId = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
        PurchaseAttributionService service = newService(List.of(
                candidate(
                        paywallEventId,
                        AttributionAppEventType.PAYWALL_SHOWN,
                        null,
                        PURCHASE_TIME.minusSeconds(paywallHoursBeforePurchase * 3600L)),
                candidate(
                        lockedClickEventId,
                        AttributionAppEventType.LOCKED_CONTENT_CLICKED,
                        900L,
                        PURCHASE_TIME.minusSeconds(lockedHoursBeforePurchase * 3600L))));

        PurchaseAttributionResult result = service.createSnapshot(1L);

        assertThat(result.attributedAppEventId()).isEqualTo(lockedClickEventId);
        assertThat(result.attributedContentId()).isEqualTo(900L);
    }

    @Property(tries = 80)
    void latestPaywallIsChosenWhenThereIsNoLockedContentClick(
            @ForAll("sortedPaywallOffsets") List<Integer> paywallHoursBeforePurchase) {
        List<AppEventAttributionCandidate> candidates = paywallHoursBeforePurchase.stream()
                .map(hours -> candidate(
                        UUID.nameUUIDFromBytes(("paywall-" + hours).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        AttributionAppEventType.PAYWALL_SHOWN,
                        null,
                        PURCHASE_TIME.minusSeconds(hours * 3600L)))
                .toList();
        PurchaseAttributionService service = newService(candidates);

        PurchaseAttributionResult result = service.createSnapshot(1L);

        Integer latestOffset = paywallHoursBeforePurchase.getFirst();
        UUID expectedEventId = UUID.nameUUIDFromBytes(("paywall-" + latestOffset)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(result.attributedAppEventId()).isEqualTo(expectedEventId);
        assertThat(result.attributedContentId()).isNull();
    }

    @Provide
    Arbitrary<List<Integer>> sortedPaywallOffsets() {
        return Arbitraries.integers()
                .between(1, 24)
                .list()
                .ofMinSize(1)
                .ofMaxSize(8)
                .uniqueElements()
                .map(values -> values.stream().sorted().toList());
    }

    private PurchaseAttributionService newService(List<AppEventAttributionCandidate> candidates) {
        PurchaseEventRepository purchaseEventRepository = new InMemoryPurchaseEventRepository(samplePurchaseEvent());
        PurchaseContextSnapshotRepository snapshotRepository = new InMemoryPurchaseContextSnapshotRepository();
        EventAttributionApi eventAttributionApi = (profileId, occurredAfterInclusive, occurredBeforeInclusive) -> candidates;
        UserLookupApi userLookupApi = new UserLookupApi() {
            @Override
            public Optional<AppUserReference> findByFirebaseUid(String firebaseUid) {
                return Optional.empty();
            }

            @Override
            public Optional<AppUserProfileReference> findPrimaryProfileByUserId(Long userId) {
                return Optional.of(new AppUserProfileReference(
                        41L,
                        91L,
                        "purchase-user",
                        false,
                        "Test User",
                        "UNKNOWN",
                        null,
                        List.of("story"),
                        List.of("sleep")));
            }

            @Override
            public Optional<AppUserProfileReference> findPrimaryProfileByFirebaseUid(String firebaseUid) {
                return Optional.empty();
            }
        };
        ContentLookupApi contentLookupApi = new ContentLookupApi() {
            @Override
            public Optional<ContentReference> findById(Long contentId) {
                return Optional.of(new ContentReference(contentId, ContentApiType.STORY, "content-" + contentId, true, 6, 5));
            }

            @Override
            public Optional<ContentReference> findByExternalKey(String externalKey) {
                return Optional.empty();
            }
        };
        return new PurchaseAttributionService(
                purchaseEventRepository,
                snapshotRepository,
                eventAttributionApi,
                userLookupApi,
                contentLookupApi);
    }

    private static PurchaseEvent samplePurchaseEvent() {
        return PurchaseEvent.record(
                41L,
                "purchase-user",
                "purchase-user",
                List.of("purchase-user"),
                PURCHASE_TIME,
                PURCHASE_TIME,
                PurchaseSource.CLIENT,
                "INITIAL_PURCHASE",
                "monthly-premium",
                null,
                "premium",
                List.of("premium"),
                "PLAY_STORE",
                "USD",
                null,
                null,
                null,
                null,
                null,
                Map.of("source", "property-test"),
                PURCHASE_TIME,
                null,
                null,
                null,
                "NORMAL",
                "PRODUCTION",
                null,
                null,
                null,
                "txn-property",
                null,
                null,
                null,
                "US",
                null,
                null,
                null,
                null,
                null);
    }

    private static AppEventAttributionCandidate candidate(
            UUID eventId,
            AttributionAppEventType eventType,
            Long contentId,
            Instant occurredAt) {
        return new AppEventAttributionCandidate(eventId, 91L, eventType, contentId, occurredAt);
    }

    private static final class InMemoryPurchaseEventRepository implements PurchaseEventRepository {

        private final PurchaseEvent purchaseEvent;

        private InMemoryPurchaseEventRepository(PurchaseEvent purchaseEvent) {
            this.purchaseEvent = purchaseEvent;
        }

        @Override
        public Optional<PurchaseEvent> findById(Long purchaseEventId) {
            return Optional.of(purchaseEvent);
        }

        @Override
        public Optional<PurchaseEvent> findByRevenuecatEventId(String revenuecatEventId) {
            return Optional.empty();
        }

        @Override
        public PurchaseEvent save(PurchaseEvent purchaseEvent) {
            return purchaseEvent;
        }
    }

    private static final class InMemoryPurchaseContextSnapshotRepository implements PurchaseContextSnapshotRepository {

        private PurchaseContextSnapshot snapshot;

        @Override
        public Optional<PurchaseContextSnapshot> findByPurchaseEventId(Long purchaseEventId) {
            return Optional.ofNullable(snapshot);
        }

        @Override
        public boolean existsByPurchaseEventId(Long purchaseEventId) {
            return snapshot != null;
        }

        @Override
        public PurchaseContextSnapshot save(PurchaseContextSnapshot purchaseContextSnapshot) {
            this.snapshot = purchaseContextSnapshot;
            return purchaseContextSnapshot;
        }
    }
}
