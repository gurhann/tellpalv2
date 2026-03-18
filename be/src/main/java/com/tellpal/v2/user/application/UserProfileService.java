package com.tellpal.v2.user.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.user.application.UserApplicationExceptions.AppUserNotFoundException;
import com.tellpal.v2.user.application.UserApplicationExceptions.UserProfileNotFoundException;
import com.tellpal.v2.user.application.UserProfileCommands.UpdateUserProfileCommand;
import com.tellpal.v2.user.application.UserProfileResults.UserProfileRecord;
import com.tellpal.v2.user.domain.AppUser;
import com.tellpal.v2.user.domain.AppUserRepository;
import com.tellpal.v2.user.domain.UserProfile;

/**
 * Application service for reading and updating user profile data.
 */
@Service
public class UserProfileService {

    private final AppUserRepository appUserRepository;
    private final UserAssetReferenceValidator assetReferenceValidator;

    public UserProfileService(
            AppUserRepository appUserRepository,
            UserAssetReferenceValidator assetReferenceValidator) {
        this.appUserRepository = appUserRepository;
        this.assetReferenceValidator = assetReferenceValidator;
    }

    /**
     * Lists all profiles owned by a user.
     */
    @Transactional(readOnly = true)
    public List<UserProfileRecord> listProfiles(Long userId) {
        AppUser appUser = loadAppUser(userId);
        return appUser.getProfiles().stream()
                .map(profile -> UserProfileMapper.toRecord(userId, profile))
                .toList();
    }

    /**
     * Returns one profile owned by a user.
     */
    @Transactional(readOnly = true)
    public UserProfileRecord getProfile(Long userId, Long profileId) {
        AppUser appUser = loadAppUser(userId);
        UserProfile profile = appUser.findProfile(profileId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId, profileId));
        return UserProfileMapper.toRecord(userId, profile);
    }

    /**
     * Updates profile content and optionally changes which profile is primary.
     */
    @Transactional
    public UserProfileRecord updateProfile(UpdateUserProfileCommand command) {
        AppUser appUser = loadAppUser(command.userId());
        if (appUser.findProfile(command.profileId()).isEmpty()) {
            throw new UserProfileNotFoundException(command.userId(), command.profileId());
        }
        assetReferenceValidator.requireImageAsset(command.avatarMediaId(), "avatarMediaId");
        UserProfile profile = appUser.updateProfile(
                command.profileId(),
                command.displayName(),
                command.ageRange(),
                command.avatarMediaId(),
                command.favoriteGenres().toArray(String[]::new),
                command.mainPurposes().toArray(String[]::new),
                command.primary());
        AppUser persistedUser = appUserRepository.save(appUser);
        UserProfile persistedProfile = persistedUser.findProfile(profile.getId())
                .orElseThrow(() -> new UserProfileNotFoundException(command.userId(), command.profileId()));
        return UserProfileMapper.toRecord(command.userId(), persistedProfile);
    }

    private AppUser loadAppUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new AppUserNotFoundException(userId));
    }
}
