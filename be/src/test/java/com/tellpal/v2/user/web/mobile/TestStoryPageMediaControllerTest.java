package com.tellpal.v2.user.web.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingKind;
import com.tellpal.v2.asset.api.AssetProcessingPageSource;
import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.api.AssetProcessingState;
import com.tellpal.v2.asset.api.AssetProcessingTarget;
import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetStorageLocation;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.StoryPageMediaManifest;
import com.tellpal.v2.asset.api.StoryPageMediaManifestApi;
import com.tellpal.v2.asset.api.StoryPageMediaManifestEntry;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.user.api.AuthenticatedAppUser;
import com.tellpal.v2.user.api.UserResolutionApi;

class TestStoryPageMediaControllerTest {

    private final UserResolutionApi users = mock(UserResolutionApi.class);
    private final StoryPageMediaManifestApi manifests = mock(StoryPageMediaManifestApi.class);
    private final AssetProcessingApi processing = mock(AssetProcessingApi.class);
    private final AssetRegistryApi assets = mock(AssetRegistryApi.class);
    private final TestStoryPageMediaController controller = new TestStoryPageMediaController(
            users, manifests, processing, assets, "allowed-firebase-uid");
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final String revision = "a".repeat(64);
    private final StoryPageMediaManifest manifest = new StoryPageMediaManifest(1, 5L, LanguageCode.TR, revision,
            "jpeg-v1", now, List.of(
                    new StoryPageMediaManifestEntry(12L, 1, 101L, asset(31L, "old-first-url", now), null,
                            "1".repeat(64), 40, 20),
                    new StoryPageMediaManifestEntry(11L, 2, 102L, asset(32L, "old-second-url", now), 81L,
                            "2".repeat(64), 20, 40)));
    private final List<AssetProcessingPageSource> currentSources = List.of(
            new AssetProcessingPageSource(12L, 1, 101L, null),
            new AssetProcessingPageSource(11L, 2, 102L, 81L));

    @Test
    void allowlistedUserGetsOrderedCurrentManifestWithRefreshedImageUrls() {
        givenCurrentState(currentSources, List.of());

        TestStoryPageMediaManifestResponse response = controller.getManifest(5L, "tr", null, "Bearer valid-token");

        assertThat(response.pages()).extracting(TestStoryPageMediaManifestResponse.Entry::pageId)
                .containsExactly(12L, 11L);
        assertThat(response.pages()).extracting(page -> page.illustration().downloadUrl())
                .containsExactly("https://fresh.test/31", "https://fresh.test/32");
        assertThat(response.pages().get(1).audioAssetId()).isEqualTo(81L);
        verify(assets).refreshDownloadUrlCache(new com.tellpal.v2.asset.api.RefreshMediaAssetDownloadUrlCommand(31L));
        verify(assets).refreshDownloadUrlCache(new com.tellpal.v2.asset.api.RefreshMediaAssetDownloadUrlCommand(32L));
    }

    @Test
    void latestManifestIsDeniedWhenCurrentSourceSnapshotChanged() {
        givenCurrentState(List.of(new AssetProcessingPageSource(12L, 1, 199L, null), currentSources.get(1)), List.of());

        assertThatThrownBy(() -> controller.getManifest(5L, "tr", null, "Bearer valid-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Current story page media is not ready");
    }

    @Test
    void latestManifestIsDeniedWhileDifferentFollowupSnapshotIsPending() {
        givenCurrentState(currentSources,
                List.of(new AssetProcessingPageSource(12L, 1, 199L, null), currentSources.get(1)));

        assertThatThrownBy(() -> controller.getManifest(5L, "tr", null, "Bearer valid-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Current story page media is not ready");
    }

    private void givenCurrentState(List<AssetProcessingPageSource> active, List<AssetProcessingPageSource> followup) {
        when(users.resolveOrCreateByIdToken("valid-token"))
                .thenReturn(new AuthenticatedAppUser(1L, 1L, "allowed-firebase-uid", false));
        when(manifests.findLatestByLanguageValue(5L, "tr")).thenReturn(Optional.of(manifest));
        when(processing.findStoryPageMediaSnapshot(5L, "tr")).thenReturn(Optional.of(new AssetProcessingRecord(
                        1L, AssetProcessingTarget.localization(5L, LanguageCode.TR), AssetProcessingKind.STORY_PAGE_MEDIA,
                        AssetProcessingContentType.STORY, "test-story", null, null, active.size(), active, followup,
                        AssetProcessingState.PROCESSING, 1, now, now, now, null, null, null, null, now, now)));
        when(assets.refreshDownloadUrlCache(any())).thenAnswer(invocation -> {
            var command = invocation.<com.tellpal.v2.asset.api.RefreshMediaAssetDownloadUrlCommand>getArgument(0);
            return asset(command.assetId(), "https://fresh.test/" + command.assetId(), now.plusSeconds(60));
        });
    }

    private static AssetRecord asset(Long id, String url, Instant expiry) {
        return new AssetRecord(id, new AssetStorageLocation(AssetStorageProvider.FIREBASE_STORAGE, "rendition/" + id),
                AssetMediaType.IMAGE, AssetKind.STORY_PAGE_RENDITION, "image/jpeg", 100L, "f".repeat(64),
                url, expiry, expiry, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }
}
