package com.tellpal.v2.user.application;

import com.tellpal.v2.user.domain.AppUser;
import com.tellpal.v2.user.domain.UserProfile;
import com.tellpal.v2.user.domain.UserProfileRepository;
import com.tellpal.v2.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserApplicationService(UserRepository userRepository,
                                  UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public AppUser registerOrGetUser(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    AppUser newUser = userRepository.save(new AppUser(firebaseUid));
                    UserProfile primaryProfile = new UserProfile(newUser.getId(), true);
                    userProfileRepository.save(primaryProfile);
                    return newUser;
                });
    }

    @Transactional(readOnly = true)
    public AppUser getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    public AppUser getUserByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new UserNotFoundException(firebaseUid));
    }

    @Transactional(readOnly = true)
    public List<UserProfile> listProfiles(Long userId) {
        return userProfileRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(Long profileId) {
        return userProfileRepository.findById(profileId)
                .orElseThrow(() -> new UserProfileNotFoundException(profileId));
    }

    public UserProfile updateProfile(Long profileId, String name, Integer ageRange,
                                     Long avatarMediaId, List<String> favoriteGenres,
                                     List<String> mainPurposes) {
        UserProfile profile = getProfile(profileId);
        if (name != null) {
            profile.setName(name);
        }
        if (ageRange != null) {
            profile.setAgeRange(ageRange);
        }
        if (avatarMediaId != null) {
            profile.setAvatarMediaId(avatarMediaId);
        }
        if (favoriteGenres != null) {
            profile.setFavoriteGenres(favoriteGenres);
        }
        if (mainPurposes != null) {
            profile.setMainPurposes(mainPurposes);
        }
        return userProfileRepository.save(profile);
    }

    public UserProfile createProfile(Long userId, String name, Integer ageRange) {
        UserProfile profile = new UserProfile(userId, false);
        profile.setName(name);
        profile.setAgeRange(ageRange);
        return userProfileRepository.save(profile);
    }
}
