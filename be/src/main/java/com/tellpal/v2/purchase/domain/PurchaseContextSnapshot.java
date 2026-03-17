package com.tellpal.v2.purchase.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "purchase_context_snapshots")
@Immutable
public class PurchaseContextSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_event_id", nullable = false, updatable = false)
    private Long purchaseEventId;

    @Column(name = "app_user_id", nullable = false, updatable = false)
    private Long appUserId;

    @Column(name = "profile_id", updatable = false)
    private Long profileId;

    @Column(name = "attribution_window_seconds", nullable = false, updatable = false)
    private Integer attributionWindowSeconds;

    @Column(name = "attributed_app_event_id", updatable = false)
    private UUID attributedAppEventId;

    @Column(name = "attributed_content_id", updatable = false)
    private Long attributedContentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_snapshot", nullable = false, columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> profileSnapshot = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PurchaseContextSnapshot() {
    }

    private PurchaseContextSnapshot(
            Long purchaseEventId,
            Long appUserId,
            Long profileId,
            Integer attributionWindowSeconds,
            UUID attributedAppEventId,
            Long attributedContentId,
            Map<String, Object> profileSnapshot) {
        this.purchaseEventId = PurchaseDomainValidator.requirePositiveId(
                purchaseEventId,
                "Purchase event ID must be positive");
        this.appUserId = PurchaseDomainValidator.requirePositiveId(appUserId, "App user ID must be positive");
        this.profileId = PurchaseDomainValidator.normalizePositiveId(profileId, "Profile ID must be positive");
        this.attributionWindowSeconds = PurchaseDomainValidator.requirePositiveInteger(
                attributionWindowSeconds,
                "Attribution window seconds must be positive");
        this.attributedAppEventId = attributedAppEventId;
        this.attributedContentId = PurchaseDomainValidator.normalizePositiveId(
                attributedContentId,
                "Attributed content ID must be positive");
        this.profileSnapshot = PurchaseDomainValidator.copyJsonMap(profileSnapshot);
    }

    public static PurchaseContextSnapshot capture(
            Long purchaseEventId,
            Long appUserId,
            Long profileId,
            Integer attributionWindowSeconds,
            UUID attributedAppEventId,
            Long attributedContentId,
            Map<String, Object> profileSnapshot) {
        return new PurchaseContextSnapshot(
                purchaseEventId,
                appUserId,
                profileId,
                attributionWindowSeconds,
                attributedAppEventId,
                attributedContentId,
                profileSnapshot);
    }

    public Long getId() {
        return id;
    }

    public Long getPurchaseEventId() {
        return purchaseEventId;
    }

    public Long getAppUserId() {
        return appUserId;
    }

    public Long getProfileId() {
        return profileId;
    }

    public Integer getAttributionWindowSeconds() {
        return attributionWindowSeconds;
    }

    public UUID getAttributedAppEventId() {
        return attributedAppEventId;
    }

    public Long getAttributedContentId() {
        return attributedContentId;
    }

    public Map<String, Object> getProfileSnapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(profileSnapshot));
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
