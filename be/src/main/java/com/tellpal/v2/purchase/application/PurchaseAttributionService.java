package com.tellpal.v2.purchase.application;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.api.ContentLookupApi;
import com.tellpal.v2.event.api.AppEventAttributionCandidate;
import com.tellpal.v2.event.api.AttributionAppEventType;
import com.tellpal.v2.event.api.EventAttributionApi;
import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.AttributedContentNotFoundException;
import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.PurchaseAttributionUserNotFoundException;
import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.PurchaseEventNotFoundException;
import com.tellpal.v2.purchase.domain.PurchaseContextSnapshot;
import com.tellpal.v2.purchase.domain.PurchaseContextSnapshotRepository;
import com.tellpal.v2.purchase.domain.PurchaseEvent;
import com.tellpal.v2.purchase.domain.PurchaseEventRepository;
import com.tellpal.v2.user.api.AppUserProfileReference;
import com.tellpal.v2.user.api.UserLookupApi;

@Service
public class PurchaseAttributionService {

    private static final int DEFAULT_ATTRIBUTION_WINDOW_SECONDS = (int) Duration.ofHours(24).toSeconds();

    private final PurchaseEventRepository purchaseEventRepository;
    private final PurchaseContextSnapshotRepository purchaseContextSnapshotRepository;
    private final EventAttributionApi eventAttributionApi;
    private final UserLookupApi userLookupApi;
    private final ContentLookupApi contentLookupApi;

    public PurchaseAttributionService(
            PurchaseEventRepository purchaseEventRepository,
            PurchaseContextSnapshotRepository purchaseContextSnapshotRepository,
            EventAttributionApi eventAttributionApi,
            UserLookupApi userLookupApi,
            ContentLookupApi contentLookupApi) {
        this.purchaseEventRepository = purchaseEventRepository;
        this.purchaseContextSnapshotRepository = purchaseContextSnapshotRepository;
        this.eventAttributionApi = eventAttributionApi;
        this.userLookupApi = userLookupApi;
        this.contentLookupApi = contentLookupApi;
    }

    @Transactional
    public PurchaseAttributionResult createSnapshot(Long purchaseEventId) {
        if (purchaseEventId == null || purchaseEventId <= 0) {
            throw new IllegalArgumentException("Purchase event ID must be positive");
        }
        Optional<PurchaseAttributionResult> existingSnapshot = purchaseContextSnapshotRepository.findByPurchaseEventId(
                        purchaseEventId)
                .map(PurchaseAttributionService::toResult);
        if (existingSnapshot.isPresent()) {
            return existingSnapshot.get();
        }

        PurchaseEvent purchaseEvent = purchaseEventRepository.findById(purchaseEventId)
                .orElseThrow(() -> new PurchaseEventNotFoundException(purchaseEventId));
        AppUserProfileReference profileReference = resolveProfile(purchaseEvent)
                .orElseThrow(() -> new PurchaseAttributionUserNotFoundException(purchaseEventId));

        Instant occurredBeforeInclusive = purchaseEvent.getOccurredAt();
        Instant occurredAfterInclusive = occurredBeforeInclusive.minusSeconds(DEFAULT_ATTRIBUTION_WINDOW_SECONDS);
        AppEventAttributionCandidate attributedEvent = selectCandidate(eventAttributionApi.findAttributionCandidates(
                profileReference.profileId(),
                occurredAfterInclusive,
                occurredBeforeInclusive));
        Long attributedContentId = resolveAttributedContentId(attributedEvent);

        PurchaseContextSnapshot snapshot = PurchaseContextSnapshot.capture(
                purchaseEventId,
                profileReference.userId(),
                profileReference.profileId(),
                DEFAULT_ATTRIBUTION_WINDOW_SECONDS,
                attributedEvent == null ? null : attributedEvent.eventId(),
                attributedContentId,
                toProfileSnapshot(profileReference));
        return toResult(purchaseContextSnapshotRepository.save(snapshot));
    }

    private Optional<AppUserProfileReference> resolveProfile(PurchaseEvent purchaseEvent) {
        if (purchaseEvent.getAppUserId() != null) {
            Optional<AppUserProfileReference> byUserId = userLookupApi.findPrimaryProfileByUserId(
                    purchaseEvent.getAppUserId());
            if (byUserId.isPresent()) {
                return byUserId;
            }
        }
        return findByFirebaseUidCandidates(
                purchaseEvent.getSourceAppUserId(),
                purchaseEvent.getOriginalAppUserId(),
                purchaseEvent.getAliasAppUserIds());
    }

    private Optional<AppUserProfileReference> findByFirebaseUidCandidates(
            String sourceAppUserId,
            String originalAppUserId,
            List<String> aliasAppUserIds) {
        for (String candidate : mergeCandidates(sourceAppUserId, originalAppUserId, aliasAppUserIds)) {
            Optional<AppUserProfileReference> reference = userLookupApi.findPrimaryProfileByFirebaseUid(candidate);
            if (reference.isPresent()) {
                return reference;
            }
        }
        return Optional.empty();
    }

    private static List<String> mergeCandidates(String sourceAppUserId, String originalAppUserId, List<String> aliases) {
        java.util.ArrayList<String> candidates = new java.util.ArrayList<>();
        addCandidate(candidates, sourceAppUserId);
        addCandidate(candidates, originalAppUserId);
        aliases.forEach(alias -> addCandidate(candidates, alias));
        return List.copyOf(candidates);
    }

    private static void addCandidate(List<String> candidates, String candidate) {
        if (candidate == null || candidate.isBlank() || candidates.contains(candidate)) {
            return;
        }
        candidates.add(candidate.trim());
    }

    private static AppEventAttributionCandidate selectCandidate(List<AppEventAttributionCandidate> candidates) {
        Optional<AppEventAttributionCandidate> lockedContentClick = candidates.stream()
                .filter(candidate -> candidate.eventType() == AttributionAppEventType.LOCKED_CONTENT_CLICKED)
                .findFirst();
        if (lockedContentClick.isPresent()) {
            return lockedContentClick.get();
        }
        return candidates.stream()
                .filter(candidate -> candidate.eventType() == AttributionAppEventType.PAYWALL_SHOWN)
                .findFirst()
                .orElse(null);
    }

    private Long resolveAttributedContentId(AppEventAttributionCandidate attributedEvent) {
        if (attributedEvent == null || attributedEvent.contentId() == null) {
            return null;
        }
        return contentLookupApi.findById(attributedEvent.contentId())
                .map(content -> content.contentId())
                .orElseThrow(() -> new AttributedContentNotFoundException(attributedEvent.contentId()));
    }

    private static Map<String, Object> toProfileSnapshot(AppUserProfileReference profileReference) {
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("firebaseUid", profileReference.firebaseUid());
        snapshot.put("allowMarketing", profileReference.allowMarketing());
        snapshot.put("displayName", profileReference.displayName());
        snapshot.put("ageRange", profileReference.ageRange());
        snapshot.put("avatarMediaId", profileReference.avatarMediaId());
        snapshot.put("favoriteGenres", profileReference.favoriteGenres());
        snapshot.put("mainPurposes", profileReference.mainPurposes());
        return snapshot;
    }

    private static PurchaseAttributionResult toResult(PurchaseContextSnapshot snapshot) {
        return new PurchaseAttributionResult(
                snapshot.getId(),
                snapshot.getPurchaseEventId(),
                snapshot.getAppUserId(),
                snapshot.getProfileId(),
                snapshot.getAttributionWindowSeconds(),
                snapshot.getAttributedAppEventId(),
                snapshot.getAttributedContentId(),
                snapshot.getProfileSnapshot(),
                snapshot.getCreatedAt());
    }
}
