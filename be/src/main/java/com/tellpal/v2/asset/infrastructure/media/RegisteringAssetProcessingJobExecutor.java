package com.tellpal.v2.asset.infrastructure.media;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingCommands.CompleteAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.FailAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.api.AssetProcessingKind;
import com.tellpal.v2.asset.api.AssetProcessingPageSource;
import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageLocation;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.asset.api.StoryPageMediaManifest;
import com.tellpal.v2.asset.application.StoryPageMediaManifestService;
import com.tellpal.v2.asset.application.StoryPageMediaManifestStore;
import com.tellpal.v2.asset.infrastructure.processing.AssetProcessingJobExecutor;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageClientRegistry;
import com.tellpal.v2.asset.infrastructure.storage.AssetProcessingPathBuilder;
import com.tellpal.v2.asset.infrastructure.storage.StorageObjectContent;
import com.tellpal.v2.asset.infrastructure.storage.StorageObjectMetadata;
import com.tellpal.v2.asset.domain.StorageProvider;

@Component
class RegisteringAssetProcessingJobExecutor implements AssetProcessingJobExecutor {

    private final AssetRegistryApi assetRegistryApi;
    private final AssetProcessingApi assetProcessingApi;
    private final ImageOptimizationAdapter imageOptimizationAdapter;
    private final AudioOptimizationAdapter audioOptimizationAdapter;
    private final ZipPackagingAdapter zipPackagingAdapter;
    private final AssetStorageClientRegistry storageClientRegistry;
    private final PageImageRenditionProcessor pageImageRenditionProcessor;
    private final StoryPageMediaManifestService storyPageMediaManifestService;
    private final AssetProcessingPathBuilder pathBuilder;
    private final Clock clock;
    private final int maxSourceBytes;
    private ListingCoverProcessingExecutor listingCovers;

    @org.springframework.beans.factory.annotation.Autowired
    RegisteringAssetProcessingJobExecutor(AssetRegistryApi assetRegistryApi,AssetProcessingApi assetProcessingApi,
            ImageOptimizationAdapter images,AudioOptimizationAdapter audio,ZipPackagingAdapter zip,
            AssetStorageClientRegistry storage,PageImageRenditionProcessor pages,StoryPageMediaManifestService manifests,
            AssetProcessingPathBuilder paths,Clock clock,@Value("${tellpal.asset.story-page-media.max-source-bytes:20971520}") int maxBytes,
            ListingCoverProcessingExecutor listingCovers) {
        this(assetRegistryApi,assetProcessingApi,images,audio,zip,storage,pages,manifests,paths,clock,maxBytes);
        this.listingCovers=listingCovers;
    }

    RegisteringAssetProcessingJobExecutor(
            AssetRegistryApi assetRegistryApi,
            AssetProcessingApi assetProcessingApi,
            ImageOptimizationAdapter imageOptimizationAdapter,
            AudioOptimizationAdapter audioOptimizationAdapter,
            ZipPackagingAdapter zipPackagingAdapter,
            AssetStorageClientRegistry storageClientRegistry,
            PageImageRenditionProcessor pageImageRenditionProcessor,
            StoryPageMediaManifestService storyPageMediaManifestService,
            AssetProcessingPathBuilder pathBuilder,
            Clock clock,
            @Value("${tellpal.asset.story-page-media.max-source-bytes:20971520}") int maxSourceBytes) {
        this.assetRegistryApi = assetRegistryApi;
        this.assetProcessingApi = assetProcessingApi;
        this.imageOptimizationAdapter = imageOptimizationAdapter;
        this.audioOptimizationAdapter = audioOptimizationAdapter;
        this.zipPackagingAdapter = zipPackagingAdapter;
        this.storageClientRegistry = storageClientRegistry;
        this.pageImageRenditionProcessor = pageImageRenditionProcessor;
        this.storyPageMediaManifestService = storyPageMediaManifestService;
        this.pathBuilder = pathBuilder;
        this.clock = clock;
        if (maxSourceBytes <= 0) throw new IllegalArgumentException("Page image source byte limit must be positive");
        this.maxSourceBytes = maxSourceBytes;
    }

    @Override
    public void process(AssetProcessingRecord assetProcessingRecord) {
        if(assetProcessingRecord.kind()==AssetProcessingKind.LISTING_COVER) {
            listingCovers.process(assetProcessingRecord);return;
        }
        try {
            if (assetProcessingRecord.kind() == AssetProcessingKind.STORY_PAGE_MEDIA) {
                processStoryPageMedia(assetProcessingRecord);
                return;
            }
            AssetStorageProvider targetProvider = resolveTargetProvider(assetProcessingRecord);
            validateSourceAssets(assetProcessingRecord);

            List<GeneratedAssetPlan> plans = new ArrayList<>();
            if (assetProcessingRecord.kind() != AssetProcessingKind.STORY_NARRATION) {
                plans.addAll(imageOptimizationAdapter.generateCoverVariants(assetProcessingRecord));
            }
            audioOptimizationAdapter.generateOptimizedAudio(assetProcessingRecord).ifPresent(plans::add);
            if (assetProcessingRecord.kind() != AssetProcessingKind.STORY_NARRATION) {
                plans.addAll(zipPackagingAdapter.generatePackages(assetProcessingRecord));
            }

            for (GeneratedAssetPlan plan : plans) {
                registerOrReuse(targetProvider, plan);
            }

            assetProcessingApi.complete(new CompleteAssetProcessingCommand(
                    assetProcessingRecord.target(), assetProcessingRecord.kind()));
        } catch (RuntimeException exception) {
            assetProcessingApi.fail(new FailAssetProcessingCommand(
                    assetProcessingRecord.target(), assetProcessingRecord.kind(),
                    "PROCESSING_FAILED",
                    exception.getMessage()));
        }
    }

    private void processStoryPageMedia(AssetProcessingRecord processing) {
        try {
            if (processing.pageSources().isEmpty()) {
                throw new IllegalArgumentException("Story page source snapshot is empty");
            }
            // First pass retains only identity and checksum data so memory stays bounded to one
            // source image rather than the full story's transformed renditions.
            List<PageFingerprint> pageFingerprints = processing.pageSources().stream()
                    .map(this::fingerprintPage)
                    .toList();
            String profileVersion = pageImageRenditionProcessor.profileVersion();
            String revisionInput = profileVersion + "|" + pageFingerprints.stream()
                    .map(page -> page.source().pageId() + ":" + page.source().pageNumber() + ":"
                            + page.source().illustrationAssetId() + ":" + page.sourceChecksum() + ":"
                            + page.source().audioAssetId())
                    .collect(Collectors.joining("|"));
            String revision = sha256(revisionInput.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StorageProvider provider = pageFingerprints.stream()
                    .map(page -> page.provider())
                    .distinct()
                    .reduce((left, right) -> {
                        throw new IllegalArgumentException("A story page revision cannot span storage providers");
                    }).orElseThrow();
            List<StoryPageMediaManifestStore.Entry> entries = new ArrayList<>();
            for (PageFingerprint page : pageFingerprints) {
                PageImageRenditionProcessor.ProcessedPageImage processed = transformPage(page);
                String objectPath = pageMediaPath(processing, revision, page.source());
                storageClientRegistry.uploadObject(provider, objectPath, processed.mimeType(),
                        processed.bytes().length, new ByteArrayInputStream(processed.bytes()));
                verifyStoredImage(provider, objectPath, processed.bytes(), processed.mimeType());
                AssetRecord rendition = assetRegistryApi.findByStorageLocation(new AssetStorageLocation(
                                AssetStorageProvider.valueOf(provider.name()), objectPath))
                        .orElseGet(() -> assetRegistryApi.register(new RegisterMediaAssetCommand(
                                AssetStorageProvider.valueOf(provider.name()), objectPath,
                                AssetKind.STORY_PAGE_RENDITION, processed.mimeType(),
                                (long) processed.bytes().length, sha256(processed.bytes()))));
                entries.add(new StoryPageMediaManifestStore.Entry(
                        page.source().pageId(), page.source().pageNumber(), page.source().illustrationAssetId(),
                        rendition, page.source().audioAssetId(), page.sourceChecksum(),
                        processed.width(), processed.height()));
            }
            StoryPageMediaManifest manifest = storyPageMediaManifestService.publish(
                    processing.contentId(), processing.languageCode(), revision, profileVersion, entries, Instant.now(clock));
            if (manifest.pages().size() != processing.pageSources().size()) {
                throw new IllegalStateException("Published story page media manifest is incomplete");
            }
            assetProcessingApi.complete(new CompleteAssetProcessingCommand(processing.target(), processing.kind()));
        } catch (RuntimeException exception) {
            assetProcessingApi.fail(new FailAssetProcessingCommand(
                    processing.target(), processing.kind(), "PAGE_MEDIA_PROCESSING_FAILED", exception.getMessage()));
        }
    }

    private PageFingerprint fingerprintPage(AssetProcessingPageSource source) {
        SourceBytes sourceBytes = readSource(source);
        return new PageFingerprint(source, sourceBytes.sourceAsset(), sourceBytes.provider(), sourceBytes.checksum(),
                sourceBytes.mimeType());
    }

    private PageImageRenditionProcessor.ProcessedPageImage transformPage(PageFingerprint fingerprint) {
        SourceBytes sourceBytes = readSource(fingerprint.source());
        if (!sourceBytes.checksum().equals(fingerprint.sourceChecksum())
                || !sourceBytes.sourceAsset().assetId().equals(fingerprint.sourceAsset().assetId())) {
            throw new IllegalArgumentException("Page illustration source changed during revision processing");
        }
        return pageImageRenditionProcessor.process(sourceBytes.bytes(), sourceBytes.mimeType());
    }

    private SourceBytes readSource(AssetProcessingPageSource source) {
        if (source.illustrationAssetId() == null) {
            throw new IllegalArgumentException("Localized page illustration is missing for page " + source.pageNumber());
        }
        AssetRecord sourceAsset = assetRegistryApi.findById(source.illustrationAssetId())
                .orElseThrow(() -> new IllegalArgumentException("Page illustration source is not registered"));
        if (sourceAsset.mediaType() != AssetMediaType.IMAGE) {
            throw new IllegalArgumentException("Page illustration source must be an image");
        }
        StorageProvider provider = StorageProvider.valueOf(sourceAsset.storageLocation().provider().name());
        String objectPath = sourceAsset.storageLocation().objectPath();
        StorageObjectMetadata metadata = storageClientRegistry.findObjectMetadata(provider, objectPath)
                .orElseThrow(() -> new IllegalArgumentException("Page illustration source object is missing"));
        if (metadata.byteSize() == null || metadata.byteSize() <= 0 || metadata.byteSize() > maxSourceBytes) {
            throw new IllegalArgumentException("Page illustration source exceeds the configured byte limit");
        }
        if (metadata.mimeType() == null || (sourceAsset.mimeType() != null
                && !sourceAsset.mimeType().equalsIgnoreCase(metadata.mimeType()))) {
            throw new IllegalArgumentException("Page illustration storage MIME type does not match its registry record");
        }
        StorageObjectContent object = storageClientRegistry.openObject(provider, objectPath, null)
                .orElseThrow(() -> new IllegalArgumentException("Page illustration source bytes are unavailable"));
        byte[] bytes;
        try (var input = object.content()) {
            bytes = input.readNBytes(maxSourceBytes + 1);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Page illustration source could not be read", exception);
        }
        if (bytes.length == 0 || bytes.length > maxSourceBytes || bytes.length != metadata.byteSize()) {
            throw new IllegalArgumentException("Page illustration source byte size is invalid");
        }
        return new SourceBytes(sourceAsset, provider, sha256(bytes), metadata.mimeType(), bytes);
    }

    private void verifyStoredImage(StorageProvider provider, String objectPath, byte[] expected, String mimeType) {
        StorageObjectMetadata metadata = storageClientRegistry.findObjectMetadata(provider, objectPath)
                .orElseThrow(() -> new IllegalStateException("Uploaded page rendition is missing"));
        if (!mimeType.equalsIgnoreCase(metadata.mimeType()) || metadata.byteSize() == null
                || metadata.byteSize() != expected.length) {
            throw new IllegalStateException("Uploaded page rendition metadata does not match generated bytes");
        }
        StorageObjectContent stored = storageClientRegistry.openObject(provider, objectPath, null)
                .orElseThrow(() -> new IllegalStateException("Uploaded page rendition bytes are unavailable"));
        try (var input = stored.content()) {
            byte[] actual = input.readNBytes(Math.max(expected.length + 1, 2));
            if (actual.length != expected.length || !sha256(actual).equals(sha256(expected))
                    || !pageImageRenditionProcessor.isDecodable(actual)) {
                throw new IllegalStateException("Uploaded page rendition failed byte or image verification");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Uploaded page rendition could not be verified", exception);
        }
    }

    private String pageMediaPath(AssetProcessingRecord processing, String revision, AssetProcessingPageSource source) {
        String root = pathBuilder.processedRoot(processing.contentType().name(), processing.externalKey(), processing.target());
        return root + "revisions/" + revision + "/pages/" + source.pageId() + ".jpg";
    }

    private record PageFingerprint(AssetProcessingPageSource source, AssetRecord sourceAsset,
            StorageProvider provider, String sourceChecksum, String mimeType) {
    }

    private record SourceBytes(AssetRecord sourceAsset, StorageProvider provider, String checksum,
            String mimeType, byte[] bytes) {
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void validateSourceAssets(AssetProcessingRecord assetProcessingRecord) {
        if (assetProcessingRecord.kind() == AssetProcessingKind.STORY_NARRATION) {
            requireSourceAsset(assetProcessingRecord.audioSourceAssetId(), AssetMediaType.AUDIO, "audioSourceAssetId");
            return;
        }
        requireSourceAsset(assetProcessingRecord.coverSourceAssetId(), AssetMediaType.IMAGE, "coverSourceAssetId");
        if (assetProcessingRecord.contentType() != AssetProcessingContentType.STORY) {
            requireSourceAsset(assetProcessingRecord.audioSourceAssetId(), AssetMediaType.AUDIO, "audioSourceAssetId");
        }
    }

    private AssetRecord requireSourceAsset(Long assetId, AssetMediaType expectedMediaType, String fieldName) {
        if (assetId == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        AssetRecord assetRecord = assetRegistryApi.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Source asset not found: " + assetId));
        if (assetRecord.mediaType() != expectedMediaType) {
            throw new IllegalArgumentException("Source asset " + assetId + " must be " + expectedMediaType);
        }
        return assetRecord;
    }

    private AssetStorageProvider resolveTargetProvider(AssetProcessingRecord assetProcessingRecord) {
        return findSourceAsset(assetProcessingRecord.coverSourceAssetId())
                .or(() -> findSourceAsset(assetProcessingRecord.audioSourceAssetId()))
                .map(record -> record.storageLocation().provider())
                .orElse(AssetStorageProvider.FIREBASE_STORAGE);
    }

    private Optional<AssetRecord> findSourceAsset(Long assetId) {
        if (assetId == null) {
            return Optional.empty();
        }
        return assetRegistryApi.findById(assetId);
    }

    private AssetRecord registerOrReuse(AssetStorageProvider provider, GeneratedAssetPlan plan) {
        AssetStorageLocation storageLocation = new AssetStorageLocation(provider, plan.objectPath());
        return assetRegistryApi.findByStorageLocation(storageLocation)
                .orElseGet(() -> assetRegistryApi.register(new RegisterMediaAssetCommand(
                        provider,
                        plan.objectPath(),
                        plan.kind(),
                        plan.mimeType(),
                        null,
                        null)));
    }
}
