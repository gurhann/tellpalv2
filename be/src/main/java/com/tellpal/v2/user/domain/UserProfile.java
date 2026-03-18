package com.tellpal.v2.user.domain;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/**
 * User-owned profile entity carrying personalization and audience metadata.
 */
@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseJpaEntity {

    private static final String DEFAULT_AGE_RANGE = "UNKNOWN";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser appUser;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(name = "age_range", nullable = false, length = 40)
    private String ageRange;

    @Column(name = "avatar_media_id")
    private Long avatarMediaId;

    @Column(name = "favorite_genres", nullable = false, columnDefinition = "text[]")
    private String[] favoriteGenres = new String[0];

    @Column(name = "main_purposes", nullable = false, columnDefinition = "text[]")
    private String[] mainPurposes = new String[0];

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    protected UserProfile() {
    }

    UserProfile(
            AppUser appUser,
            String displayName,
            String ageRange,
            Long avatarMediaId,
            String[] favoriteGenres,
            String[] mainPurposes,
            boolean primary) {
        this.appUser = requireAppUser(appUser);
        update(displayName, ageRange, avatarMediaId, favoriteGenres, mainPurposes, primary);
    }

    static UserProfile createDefaultPrimary(AppUser appUser) {
        return new UserProfile(appUser, null, DEFAULT_AGE_RANGE, null, new String[0], new String[0], true);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAgeRange() {
        return ageRange;
    }

    public Long getAvatarMediaId() {
        return avatarMediaId;
    }

    public List<String> getFavoriteGenres() {
        return toImmutableList(favoriteGenres);
    }

    public List<String> getMainPurposes() {
        return toImmutableList(mainPurposes);
    }

    public boolean isPrimary() {
        return primary;
    }

    /**
     * Replaces mutable profile fields and primary-profile state.
     */
    public void update(
            String displayName,
            String ageRange,
            Long avatarMediaId,
            String[] favoriteGenres,
            String[] mainPurposes,
            boolean primary) {
        this.displayName = normalizeOptionalText(displayName);
        this.ageRange = requireText(ageRange, "Profile age range must not be blank");
        this.avatarMediaId = normalizePositiveId(avatarMediaId, "Avatar media ID must be positive");
        this.favoriteGenres = normalizeValues(favoriteGenres, "Favorite genres must not contain blank values");
        this.mainPurposes = normalizeValues(mainPurposes, "Main purposes must not contain blank values");
        this.primary = primary;
    }

    void markPrimary(boolean primary) {
        this.primary = primary;
    }

    private static AppUser requireAppUser(AppUser appUser) {
        if (appUser == null) {
            throw new IllegalArgumentException("App user must not be null");
        }
        return appUser;
    }

    private static String requireText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Long normalizePositiveId(Long value, String message) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String[] normalizeValues(String[] values, String message) {
        if (values == null || values.length == 0) {
            return new String[0];
        }
        LinkedHashSet<String> normalizedValues = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeOptionalText(value);
            if (normalized == null) {
                throw new IllegalArgumentException(message);
            }
            normalizedValues.add(normalized);
        }
        return normalizedValues.toArray(String[]::new);
    }

    private static List<String> toImmutableList(String[] values) {
        if (values == null || values.length == 0) {
            return List.of();
        }
        return Arrays.stream(values.clone()).toList();
    }
}
