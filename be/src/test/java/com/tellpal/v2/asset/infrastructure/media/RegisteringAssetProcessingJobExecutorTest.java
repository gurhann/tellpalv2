package com.tellpal.v2.asset.infrastructure.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingKind;
import com.tellpal.v2.asset.api.AssetProcessingPageSource;
import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.api.AssetProcessingState;
import com.tellpal.v2.asset.api.AssetProcessingTarget;
import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetStorageLocation;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.asset.api.StoryPageMediaManifest;
import com.tellpal.v2.asset.application.StoryPageMediaManifestService;
import com.tellpal.v2.asset.application.StoryPageMediaManifestStore;
import com.tellpal.v2.asset.infrastructure.storage.AssetProcessingPathBuilder;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageClientRegistry;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageFirebaseProperties;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageObjectPathBuilder;
import com.tellpal.v2.asset.infrastructure.storage.FakeFirebaseStorageAssetClient;
import com.tellpal.v2.shared.domain.LanguageCode;

class RegisteringAssetProcessingJobExecutorTest {

    private final com.tellpal.v2.asset.api.AssetRegistryApi registry = mock(com.tellpal.v2.asset.api.AssetRegistryApi.class);
    private final com.tellpal.v2.asset.api.AssetProcessingApi processingApi = mock(com.tellpal.v2.asset.api.AssetProcessingApi.class);
    private final StoryPageMediaManifestStore manifestStore = mock(StoryPageMediaManifestStore.class);
    private final FakeFirebaseStorageAssetClient fakeStorage = new FakeFirebaseStorageAssetClient(
            new AssetStorageFirebaseProperties(true, "test", "bucket", null, "test", null, null, null));
    private final AssetStorageClientRegistry storage = new AssetStorageClientRegistry(List.of(fakeStorage));
    private final PageImageRenditionProcessor renditionProcessor = mock(PageImageRenditionProcessor.class);
    private final RegisteringAssetProcessingJobExecutor executor = new RegisteringAssetProcessingJobExecutor(
            registry, processingApi, mock(ImageOptimizationAdapter.class), mock(AudioOptimizationAdapter.class),
            mock(ZipPackagingAdapter.class), storage, renditionProcessor,
            new StoryPageMediaManifestService(manifestStore),
            new AssetProcessingPathBuilder(new AssetStorageObjectPathBuilder(
                    new AssetStorageFirebaseProperties(true, "test", "bucket", null, "test", null, null, null))),
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC), 20_000_000);
    private final byte[] png = new byte[] {11, 22, 33, 44};

    @BeforeEach
    void setUp() {
        when(renditionProcessor.profileVersion()).thenReturn("unit-profile-v1");
        when(renditionProcessor.process(any(), any())).thenReturn(
                new PageImageRenditionProcessor.ProcessedPageImage(new byte[] {5, 6, 7}, 48, 24, "image/jpeg"));
        when(renditionProcessor.isDecodable(any())).thenReturn(true);
        when(registry.findByStorageLocation(any())).thenReturn(Optional.empty());
        when(registry.register(any())).thenAnswer(invocation -> {
            RegisterMediaAssetCommand command = invocation.getArgument(0);
            return asset(900L + command.objectPath().hashCode() & 0x7fffffffL, command.objectPath(),
                    command.mimeType(), command.byteSize(), command.checksumSha256());
        });
        when(manifestStore.save(any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            Long contentId = invocation.getArgument(0);
            LanguageCode language = invocation.getArgument(1);
            String revision = invocation.getArgument(2);
            String profile = invocation.getArgument(3);
            @SuppressWarnings("unchecked") List<StoryPageMediaManifestStore.Entry> entries = invocation.getArgument(4);
            Instant created = invocation.getArgument(5);
            return new StoryPageMediaManifest(1, contentId, language, revision, profile, created,
                    entries.stream().map(entry -> new com.tellpal.v2.asset.api.StoryPageMediaManifestEntry(
                            entry.pageId(), entry.pageNumber(), entry.sourceAssetId(), entry.rendition(),
                            entry.audioAssetId(), entry.sourceChecksumSha256(), entry.width(), entry.height())).toList());
        });
    }

    @Test
    void validSourcePublishesVerifiedRevisionWithoutDocker() {
        runWithSource(101L, png);

        verify(manifestStore).save(any(), any(), any(), any(), any(), any());
        verify(processingApi).complete(any());
        verify(processingApi, never()).fail(any());
    }

    @Test
    void invalidSourceFailsWithoutPublishingWithoutDocker() {
        executor.process(processingRecord(404L));

        verify(manifestStore, never()).save(any(), any(), any(), any(), any(), any());
        verify(processingApi).fail(any());
        verify(processingApi, never()).complete(any());
    }

    @Test
    void replacingSourceAssetWithIdenticalBytesProducesDifferentRevisionWithoutDocker() {
        runWithSource(101L, png);
        runWithSource(202L, png);

        ArgumentCaptor<String> revisions = ArgumentCaptor.forClass(String.class);
        verify(manifestStore, org.mockito.Mockito.times(2)).save(any(), any(), revisions.capture(), any(), any(), any());
        assertThat(revisions.getAllValues()).hasSize(2).doesNotHaveDuplicates();
    }

    private void runWithSource(long sourceId, byte[] sourceBytes) {
        String sourcePath = "original/" + sourceId + ".png";
        storage.uploadObject(com.tellpal.v2.asset.domain.StorageProvider.FIREBASE_STORAGE,
                sourcePath, "image/png", sourceBytes.length, new java.io.ByteArrayInputStream(sourceBytes));
        when(registry.findById(sourceId)).thenReturn(Optional.of(asset(sourceId, sourcePath, "image/png",
                (long) sourceBytes.length, null)));
        executor.process(processingRecord(sourceId));
    }

    private AssetProcessingRecord processingRecord(long sourceId) {
        return new AssetProcessingRecord(1L,
                AssetProcessingTarget.localization(42L, LanguageCode.TR), AssetProcessingKind.STORY_PAGE_MEDIA,
                AssetProcessingContentType.STORY, "unit-story", null, null, 1,
                List.of(new AssetProcessingPageSource(7L, 1, sourceId, null)), AssetProcessingState.PROCESSING,
                1, Instant.now(), Instant.now(), Instant.now(), null, null, null, null,
                Instant.now(), Instant.now());
    }

    private static AssetRecord asset(Long id, String path, String mime, Long size, String checksum) {
        return new AssetRecord(id, new AssetStorageLocation(AssetStorageProvider.FIREBASE_STORAGE, path),
                AssetMediaType.IMAGE, AssetKind.ORIGINAL_IMAGE, mime, size, checksum, null, null, null,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }
}
