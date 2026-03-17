package com.tellpal.v2.user.domain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

@Entity
@Table(name = "app_users")
public class AppUser extends BaseJpaEntity {

    @Column(name = "firebase_uid", nullable = false, length = 191)
    private String firebaseUid;

    @Column(name = "is_allow_marketing", nullable = false)
    private boolean allowMarketing;

    @OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserProfile> profiles = new LinkedHashSet<>();

    protected AppUser() {
    }

    private AppUser(String firebaseUid, boolean allowMarketing) {
        this.firebaseUid = requireText(firebaseUid, "Firebase UID must not be blank");
        this.allowMarketing = allowMarketing;
    }

    public static AppUser create(String firebaseUid, boolean allowMarketing) {
        AppUser appUser = new AppUser(firebaseUid, allowMarketing);
        appUser.profiles.add(UserProfile.createDefaultPrimary(appUser));
        return appUser;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public boolean isAllowMarketing() {
        return allowMarketing;
    }

    public Set<UserProfile> getProfiles() {
        return Collections.unmodifiableSet(profiles);
    }

    public Optional<UserProfile> findProfile(Long profileId) {
        Long requiredProfileId = requirePositiveId(profileId, "Profile ID must be positive");
        return profiles.stream()
                .filter(profile -> requiredProfileId.equals(profile.getId()))
                .findFirst();
    }

    public Optional<UserProfile> primaryProfile() {
        return profiles.stream()
                .filter(UserProfile::isPrimary)
                .findFirst();
    }

    public void updateMarketingConsent(boolean allowMarketing) {
        this.allowMarketing = allowMarketing;
    }

    public UserProfile addProfile(
            String displayName,
            String ageRange,
            Long avatarMediaId,
            String[] favoriteGenres,
            String[] mainPurposes,
            boolean primary) {
        UserProfile profile = new UserProfile(
                this,
                displayName,
                ageRange,
                avatarMediaId,
                favoriteGenres,
                mainPurposes,
                primary);
        if (primary) {
            unsetPrimaryProfilesExcept(profile);
        }
        profiles.add(profile);
        return profile;
    }

    public UserProfile updateProfile(
            Long profileId,
            String displayName,
            String ageRange,
            Long avatarMediaId,
            String[] favoriteGenres,
            String[] mainPurposes,
            boolean primary) {
        UserProfile profile = findProfile(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user"));
        if (primary) {
            unsetPrimaryProfilesExcept(profile);
        }
        profile.update(displayName, ageRange, avatarMediaId, favoriteGenres, mainPurposes, primary);
        return profile;
    }

    private void unsetPrimaryProfilesExcept(UserProfile selectedProfile) {
        profiles.stream()
                .filter(profile -> profile != selectedProfile)
                .forEach(profile -> profile.markPrimary(false));
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
