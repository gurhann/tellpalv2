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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/profiles")
@Tag(name = "Mobile Profiles", description = "Authenticated mobile profile lookup and update endpoints.")
@SecurityRequirement(name = "mobileBearerAuth")
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
    @Operation(summary = "List user profiles", description = "Returns profiles that belong to the authenticated mobile user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profiles returned"),
            @ApiResponse(responseCode = "400", description = "Profile request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Firebase bearer token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<MobileUserProfileResponse> listProfiles(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        Long userId = authenticatedMobileUserResolver.resolveCurrentUser(authorizationHeader).userId();
        return userProfileService.listProfiles(userId).stream()
                .map(MobileUserProfileResponse::from)
                .toList();
    }

    @GetMapping("/{profileId}")
    @Operation(summary = "Get one profile", description = "Returns one profile owned by the authenticated mobile user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "400", description = "Profile request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Firebase bearer token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Profile was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public MobileUserProfileResponse getProfile(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long profileId) {
        Long userId = authenticatedMobileUserResolver.resolveCurrentUser(authorizationHeader).userId();
        return MobileUserProfileResponse.from(userProfileService.getProfile(userId, profileId));
    }

    @PutMapping("/{profileId}")
    @Operation(summary = "Update one profile", description = "Updates one profile that belongs to the authenticated mobile user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Profile update is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Firebase bearer token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Profile was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
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
