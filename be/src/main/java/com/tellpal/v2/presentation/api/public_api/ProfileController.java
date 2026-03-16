package com.tellpal.v2.presentation.api.public_api;

import com.tellpal.v2.presentation.dto.user.CreateProfileRequest;
import com.tellpal.v2.presentation.dto.user.UpdateProfileRequest;
import com.tellpal.v2.presentation.dto.user.UserProfileResponse;
import com.tellpal.v2.presentation.dto.user.UserResponse;
import com.tellpal.v2.user.application.UserApplicationService;
import com.tellpal.v2.user.domain.AppUser;
import com.tellpal.v2.user.domain.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProfileController {

    private final UserApplicationService userApplicationService;

    public ProfileController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<UserResponse> register() {
        String firebaseUid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AppUser user = userApplicationService.registerOrGetUser(firebaseUid);
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(user));
    }

    @GetMapping("/api/auth/me")
    public ResponseEntity<UserResponse> me() {
        String firebaseUid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AppUser user = userApplicationService.getUserByFirebaseUid(firebaseUid);
        return ResponseEntity.ok(toUserResponse(user));
    }

    @GetMapping("/api/profiles")
    public ResponseEntity<List<UserProfileResponse>> listProfiles() {
        String firebaseUid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AppUser user = userApplicationService.getUserByFirebaseUid(firebaseUid);
        List<UserProfileResponse> profiles = userApplicationService.listProfiles(user.getId())
                .stream()
                .map(this::toUserProfileResponse)
                .toList();
        return ResponseEntity.ok(profiles);
    }

    @PostMapping("/api/profiles")
    public ResponseEntity<UserProfileResponse> createProfile(@RequestBody CreateProfileRequest request) {
        String firebaseUid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AppUser user = userApplicationService.getUserByFirebaseUid(firebaseUid);
        UserProfile profile = userApplicationService.createProfile(user.getId(), request.name(), request.ageRange());
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserProfileResponse(profile));
    }

    @GetMapping("/api/profiles/{id}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable Long id) {
        UserProfile profile = userApplicationService.getProfile(id);
        return ResponseEntity.ok(toUserProfileResponse(profile));
    }

    @PutMapping("/api/profiles/{id}")
    public ResponseEntity<UserProfileResponse> updateProfile(@PathVariable Long id,
                                                             @RequestBody UpdateProfileRequest request) {
        UserProfile profile = userApplicationService.updateProfile(
                id,
                request.name(),
                request.ageRange(),
                request.avatarMediaId(),
                request.favoriteGenres(),
                request.mainPurposes()
        );
        return ResponseEntity.ok(toUserProfileResponse(profile));
    }

    private UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getFirebaseUid(), user.isAllowMarketing());
    }

    private UserProfileResponse toUserProfileResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getName(),
                profile.getAgeRange(),
                profile.getAvatarMediaId(),
                profile.getFavoriteGenres(),
                profile.getMainPurposes(),
                profile.isPrimary()
        );
    }
}
