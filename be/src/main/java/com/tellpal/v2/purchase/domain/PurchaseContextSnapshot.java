package com.tellpal.v2.purchase.domain;

import com.tellpal.v2.shared.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "v2_purchase_context_snapshots")
public class PurchaseContextSnapshot extends BaseEntity {

    @Column(name = "purchase_event_id", nullable = false, unique = true)
    private Long purchaseEventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "attribution_window_seconds")
    private Integer attributionWindowSeconds;

    @Column(name = "attributed_app_event_id")
    private UUID attributedAppEventId;

    @Column(name = "attributed_content_id")
    private Long attributedContentId;

    @Column(name = "profile_snapshot", columnDefinition = "jsonb")
    private String profileSnapshot;

    protected PurchaseContextSnapshot() {
    }

    public PurchaseContextSnapshot(Long purchaseEventId, Long userId) {
        this.purchaseEventId = purchaseEventId;
        this.userId = userId;
    }

    public Long getPurchaseEventId() { return purchaseEventId; }
    public void setPurchaseEventId(Long purchaseEventId) { this.purchaseEventId = purchaseEventId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }

    public Integer getAttributionWindowSeconds() { return attributionWindowSeconds; }
    public void setAttributionWindowSeconds(Integer attributionWindowSeconds) { this.attributionWindowSeconds = attributionWindowSeconds; }

    public UUID getAttributedAppEventId() { return attributedAppEventId; }
    public void setAttributedAppEventId(UUID attributedAppEventId) { this.attributedAppEventId = attributedAppEventId; }

    public Long getAttributedContentId() { return attributedContentId; }
    public void setAttributedContentId(Long attributedContentId) { this.attributedContentId = attributedContentId; }

    public String getProfileSnapshot() { return profileSnapshot; }
    public void setProfileSnapshot(String profileSnapshot) { this.profileSnapshot = profileSnapshot; }
}
