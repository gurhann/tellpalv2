package com.tellpal.v2.user.web.mobile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingPageSource;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.RefreshMediaAssetDownloadUrlCommand;
import com.tellpal.v2.asset.api.StoryPageMediaManifest;
import com.tellpal.v2.asset.api.StoryPageMediaManifestApi;
import com.tellpal.v2.user.api.UserAuthenticationException;
import com.tellpal.v2.user.api.UserResolutionApi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Firebase-authenticated, allowlisted endpoint for the protected story media playback experiment. */
@RestController
@RequestMapping("/api/test/story-page-media")
@Tag(name = "Test Story Media", description = "Protected experimental story page media delivery.")
public class TestStoryPageMediaController {

    private final UserResolutionApi userResolutionApi;
    private final StoryPageMediaManifestApi manifestApi;
    private final AssetProcessingApi assetProcessingApi;
    private final AssetRegistryApi assetRegistryApi;
    private final Set<String> allowedFirebaseUids;

    public TestStoryPageMediaController(
            UserResolutionApi userResolutionApi,
            StoryPageMediaManifestApi manifestApi,
            AssetProcessingApi assetProcessingApi,
            AssetRegistryApi assetRegistryApi,
            @Value("${tellpal.asset.story-page-media.test.allowed-firebase-uids:}") String allowlist) {
        this.userResolutionApi = userResolutionApi;
        this.manifestApi = manifestApi;
        this.assetProcessingApi = assetProcessingApi;
        this.assetRegistryApi = assetRegistryApi;
        this.allowedFirebaseUids = Arrays.stream(allowlist.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    @GetMapping("/{contentId}")
    @Operation(summary = "Get protected story page media manifest", description = "Returns one verified manifest and short-lived image and source-audio URLs to allowlisted Firebase users.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verified manifest returned"),
            @ApiResponse(responseCode = "401", description = "Firebase authentication is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Firebase user is not allowlisted for the experiment"),
            @ApiResponse(responseCode = "404", description = "No verified manifest exists")
    })
    @SecurityRequirement(name = "mobileBearerAuth")
    public TestStoryPageMediaManifestResponse getManifest(
            @PathVariable Long contentId,
            @RequestParam("lang") String languageCode,
            @RequestParam(name = "revision", required = false) String revision,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String idToken = extractBearerToken(authorizationHeader);
        String firebaseUid;
        try {
            firebaseUid = userResolutionApi.resolveOrCreateByIdToken(idToken).firebaseUid();
        } catch (UserAuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firebase authentication is invalid");
        }
        if (!allowedFirebaseUids.contains(firebaseUid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not enabled for story media testing");
        }
        StoryPageMediaManifest manifest = (revision == null || revision.isBlank()
                ? manifestApi.findLatestByLanguageValue(contentId, languageCode)
                : manifestApi.findRevisionByLanguageValue(contentId, languageCode, revision))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Verified story media manifest was not found"));
        if (revision == null || revision.isBlank()) {
            var processing = assetProcessingApi.findStoryPageMediaSnapshot(contentId, languageCode)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current story page sources were not found"));
            boolean matchesCurrent = manifestMatches(manifest.pages(), processing.pageSources());
            boolean matchesFollowup = processing.pageSourceFollowup().isEmpty()
                    || manifestMatches(manifest.pages(), processing.pageSourceFollowup());
            if (!matchesCurrent || !matchesFollowup) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Current story page media is not ready");
            }
        }
        return TestStoryPageMediaManifestResponse.from(manifest, assetRegistryApi);
    }

    private static boolean manifestMatches(List<com.tellpal.v2.asset.api.StoryPageMediaManifestEntry> pages,
            List<AssetProcessingPageSource> sources) {
        return pages.size() == sources.size()
                && java.util.stream.IntStream.range(0, sources.size()).allMatch(index -> {
                    AssetProcessingPageSource source = sources.get(index);
                    var page = pages.get(index);
                    return source.pageId().equals(page.pageId()) && source.pageNumber() == page.pageNumber()
                            && java.util.Objects.equals(source.illustrationAssetId(), page.sourceAssetId())
                            && java.util.Objects.equals(source.audioAssetId(), page.audioAssetId());
                });
    }

    private static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")
                || authorizationHeader.substring("Bearer ".length()).isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firebase bearer token is required");
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}
