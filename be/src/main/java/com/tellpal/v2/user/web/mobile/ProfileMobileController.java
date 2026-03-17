package com.tellpal.v2.user.web.mobile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tellpal.v2.user.application.UserProfileCommands.UpdateUserProfileCommand;
import com.tellpal.v2.user.application.UserProfileService;

@RestController
@RequestMapping("/api/profiles")
public class ProfileMobileController {

    private final AuthenticatedMobileUserResolver authenticatedMobileUserResolver;
    private final UserProfileService userProfileService;

    public ProfileMobileController(
            AuthenticatedMobileUserResolver authenticatedMobileUserResolver,
            UserProfileService userProfileService) {
        this.authenticatedMobileUserResolver = authenticatedMobileUserResolver;
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public List<MobileUserProfileResponse> listProfiles(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        Long userId = authenticatedMobileUserResolver.resolveCurrentUser(authorizationHeader).userId();
        return userProfileService.listProfiles(userId).stream()
                .map(MobileUserProfileResponse::from)
                .toList();
    }

    @GetMapping("/{profileId}")
    public MobileUserProfileResponse getProfile(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long profileId) {
        Long userId = authenticatedMobileUserResolver.resolveCurrentUser(authorizationHeader).userId();
        return MobileUserProfileResponse.from(userProfileService.getProfile(userId, profileId));
    }

    @PutMapping("/{profileId}")
    public MobileUserProfileResponse updateProfile(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long profileId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        Long userId = authenticatedMobileUserResolver.resolveCurrentUser(authorizationHeader).userId();
        return MobileUserProfileResponse.from(userProfileService.updateProfile(request.toCommand(userId, profileId)));
    }
}

record UpdateUserProfileRequest(
        String displayName,
        @NotBlank(message = "ageRange is required")
        String ageRange,
        @Positive(message = "avatarMediaId must be positive")
        Long avatarMediaId,
        @NotNull(message = "favoriteGenres is required")
        List<@NotBlank(message = "favoriteGenres entries must not be blank") String> favoriteGenres,
        @NotNull(message = "mainPurposes is required")
        List<@NotBlank(message = "mainPurposes entries must not be blank") String> mainPurposes,
        @NotNull(message = "primary is required")
        Boolean primary) {

    UpdateUserProfileCommand toCommand(Long userId, Long profileId) {
        return new UpdateUserProfileCommand(
                userId,
                profileId,
                displayName,
                ageRange,
                avatarMediaId,
                favoriteGenres,
                mainPurposes,
                primary);
    }
}
