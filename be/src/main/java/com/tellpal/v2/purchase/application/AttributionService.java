package com.tellpal.v2.purchase.application;

import com.tellpal.v2.purchase.domain.PurchaseContextSnapshot;
import com.tellpal.v2.purchase.domain.PurchaseContextSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AttributionService {

    private static final int ATTRIBUTION_WINDOW_SECONDS = 86400;
    private static final String LOCKED_CONTENT_CLICKED = "LOCKED_CONTENT_CLICKED";
    private static final String PAYWALL_SHOWN = "PAYWALL_SHOWN";

    private final PurchaseContextSnapshotRepository snapshotRepository;

    public AttributionService(PurchaseContextSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    public PurchaseContextSnapshot createSnapshot(
            Long purchaseEventId,
            Long userId,
            Long profileId,
            OffsetDateTime purchaseOccurredAt,
            List<AppEventCandidate> recentAppEvents,
            Long attributedContentId,
            String profileSnapshotJson) {

        Optional<PurchaseContextSnapshot> existing = snapshotRepository.findByPurchaseEventId(purchaseEventId);
        if (existing.isPresent()) {
            return existing.get();
        }

        PurchaseContextSnapshot snapshot = new PurchaseContextSnapshot(purchaseEventId, userId);
        snapshot.setProfileId(profileId);
        snapshot.setAttributionWindowSeconds(ATTRIBUTION_WINDOW_SECONDS);
        snapshot.setAttributedContentId(attributedContentId);
        snapshot.setProfileSnapshot(profileSnapshotJson);

        UUID attributedEventId = resolveAttributedEventId(recentAppEvents, purchaseOccurredAt);
        snapshot.setAttributedAppEventId(attributedEventId);

        return snapshotRepository.save(snapshot);
    }

    private UUID resolveAttributedEventId(List<AppEventCandidate> candidates, OffsetDateTime purchaseOccurredAt) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        OffsetDateTime windowStart = purchaseOccurredAt.minusSeconds(ATTRIBUTION_WINDOW_SECONDS);
        List<AppEventCandidate> inWindow = filterWithinWindow(candidates, windowStart, purchaseOccurredAt);

        return findByEventType(inWindow, LOCKED_CONTENT_CLICKED)
                .or(() -> findByEventType(inWindow, PAYWALL_SHOWN))
                .map(AppEventCandidate::eventId)
                .orElse(null);
    }

    private List<AppEventCandidate> filterWithinWindow(
            List<AppEventCandidate> candidates,
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd) {
        return candidates.stream()
                .filter(e -> !e.occurredAt().isBefore(windowStart) && !e.occurredAt().isAfter(windowEnd))
                .toList();
    }

    private Optional<AppEventCandidate> findByEventType(List<AppEventCandidate> candidates, String eventType) {
        return candidates.stream()
                .filter(e -> eventType.equals(e.eventType()))
                .findFirst();
    }

    public record AppEventCandidate(UUID eventId, String eventType, OffsetDateTime occurredAt) {}
}
