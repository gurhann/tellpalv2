package com.tellpal.v2.content.application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tellpal.v2.asset.api.AssetContent;
import com.tellpal.v2.asset.api.AssetContentAccessToken;
import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.content.application.ContentApplicationExceptions.AssetMediaTypeMismatchException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.AssetReferenceNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.StoryPageTextlessIllustrationsMissingException;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.content.domain.StoryPage;

@Service
public class StoryPageTextlessIllustrationExportService {

    private static final Pattern UNSAFE_FILE_NAME_CHARACTERS = Pattern.compile("[^a-zA-Z0-9._-]+");

    private final ContentRepository contentRepository;
    private final AssetRegistryApi assetRegistryApi;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public StoryPageTextlessIllustrationExportService(
            ContentRepository contentRepository,
            AssetRegistryApi assetRegistryApi,
            ObjectMapper objectMapper,
            Clock clock) {
        this.contentRepository = contentRepository;
        this.assetRegistryApi = assetRegistryApi;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Creates a stable export plan for textless story-page illustrations.
     */
    @Transactional(readOnly = true)
    public TextlessIllustrationExport prepareExport(Long contentId) {
        Content content = loadStoryContent(contentId);
        TextlessIllustrationExportCover cover = content.getTextlessCoverMediaId() == null
                ? null
                : toExportCover(content.getTextlessCoverMediaId());
        List<StoryPage> orderedPages = content.getStoryPages().stream()
                .sorted(Comparator.comparingInt(StoryPage::getPageNumber))
                .toList();
        List<TextlessIllustrationExportPage> includedPages = orderedPages.stream()
                .filter(storyPage -> storyPage.getTextlessIllustrationMediaId() != null)
                .map(this::toExportPage)
                .toList();
        if (cover == null && includedPages.isEmpty()) {
            throw new StoryPageTextlessIllustrationsMissingException(requireContentId(content));
        }
        List<Integer> missingPages = orderedPages.stream()
                .filter(storyPage -> storyPage.getTextlessIllustrationMediaId() == null)
                .map(StoryPage::getPageNumber)
                .toList();
        TextlessIllustrationManifest manifest = new TextlessIllustrationManifest(
                requireContentId(content),
                content.getExternalKey(),
                Instant.now(clock),
                orderedPages.size(),
                cover,
                includedPages,
                missingPages);
        return new TextlessIllustrationExport(
                exportFileName(content.getExternalKey()),
                manifest,
                cover,
                includedPages);
    }

    /**
     * Writes the export as a zip file with a manifest and one folder per included page.
     */
    public void writeZip(TextlessIllustrationExport export, OutputStream outputStream) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            writeManifest(export, zipOutputStream);
            if (export.cover() != null) {
                writeCoverFile(export.cover(), zipOutputStream);
            }
            for (TextlessIllustrationExportPage page : export.pages()) {
                writePageFile(page, zipOutputStream);
            }
        }
    }

    private void writeManifest(TextlessIllustrationExport export, ZipOutputStream zipOutputStream) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry("manifest.json"));
        zipOutputStream.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(export.manifest()));
        zipOutputStream.closeEntry();
    }

    private void writePageFile(TextlessIllustrationExportPage page, ZipOutputStream zipOutputStream) throws IOException {
        AssetContentAccessToken token = assetRegistryApi.issueContentAccessToken(page.assetId());
        AssetContent content = assetRegistryApi.openContent(page.assetId(), token.token(), null);
        try (InputStream inputStream = content.content()) {
            zipOutputStream.putNextEntry(new ZipEntry(page.fileName()));
            inputStream.transferTo(zipOutputStream);
            zipOutputStream.closeEntry();
        }
    }

    private void writeCoverFile(TextlessIllustrationExportCover cover, ZipOutputStream zipOutputStream) throws IOException {
        AssetContentAccessToken token = assetRegistryApi.issueContentAccessToken(cover.assetId());
        AssetContent content = assetRegistryApi.openContent(cover.assetId(), token.token(), null);
        try (InputStream inputStream = content.content()) {
            zipOutputStream.putNextEntry(new ZipEntry(cover.fileName()));
            inputStream.transferTo(zipOutputStream);
            zipOutputStream.closeEntry();
        }
    }

    private TextlessIllustrationExportCover toExportCover(Long assetId) {
        AssetRecord asset = requireTextlessImageAsset("textlessCoverMediaId", assetId);
        String fileName = "cover/textless%s".formatted(
                extension(asset.storageLocation().objectPath(), asset.mimeType()));
        return new TextlessIllustrationExportCover(
                asset.assetId(),
                asset.storageLocation().objectPath(),
                asset.mimeType(),
                fileName);
    }

    private TextlessIllustrationExportPage toExportPage(StoryPage storyPage) {
        Long assetId = storyPage.getTextlessIllustrationMediaId();
        AssetRecord asset = requireTextlessImageAsset("textlessIllustrationMediaId", assetId);
        String fileName = "page-%03d/textless%s".formatted(
                storyPage.getPageNumber(),
                extension(asset.storageLocation().objectPath(), asset.mimeType()));
        return new TextlessIllustrationExportPage(
                storyPage.getPageNumber(),
                asset.assetId(),
                asset.storageLocation().objectPath(),
                asset.mimeType(),
                fileName);
    }

    private AssetRecord requireTextlessImageAsset(String fieldName, Long assetId) {
        AssetRecord asset = assetRegistryApi.findById(assetId)
                .orElseThrow(() -> new AssetReferenceNotFoundException(fieldName, assetId));
        if (asset.mediaType() != AssetMediaType.IMAGE) {
            throw new AssetMediaTypeMismatchException(
                    fieldName,
                    assetId,
                    AssetMediaType.IMAGE,
                    asset.mediaType());
        }
        return asset;
    }

    private Content loadStoryContent(Long contentId) {
        Content content = contentRepository.findByIdForStoryPageAdminRead(requireContentId(contentId))
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        if (!content.getType().supportsStoryPages()) {
            throw new IllegalStateException("Textless story-page illustrations can only be exported for STORY content");
        }
        return content;
    }

    private static String exportFileName(String externalKey) {
        String safeExternalKey = UNSAFE_FILE_NAME_CHARACTERS.matcher(externalKey).replaceAll("-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "")
                .toLowerCase(Locale.ROOT);
        if (safeExternalKey.isBlank()) {
            safeExternalKey = "story";
        }
        return safeExternalKey + "-story-source-images.zip";
    }

    private static String extension(String objectPath, String mimeType) {
        int lastSlash = objectPath.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? objectPath.substring(lastSlash + 1) : objectPath;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
        }
        if ("image/png".equalsIgnoreCase(mimeType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(mimeType)) {
            return ".webp";
        }
        return ".jpg";
    }

    private static Long requireContentId(Content content) {
        Long contentId = content.getId();
        if (contentId == null || contentId <= 0) {
            throw new IllegalStateException("Content must be persisted before story-page illustration export");
        }
        return contentId;
    }

    private static Long requireContentId(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        return contentId;
    }

    public record TextlessIllustrationExport(
            String fileName,
            TextlessIllustrationManifest manifest,
            TextlessIllustrationExportCover cover,
            List<TextlessIllustrationExportPage> pages) {

        public TextlessIllustrationExport(
                String fileName,
                TextlessIllustrationManifest manifest,
                List<TextlessIllustrationExportPage> pages) {
            this(fileName, manifest, null, pages);
        }
    }

    public record TextlessIllustrationManifest(
            Long contentId,
            String externalKey,
            Instant exportedAt,
            int totalPageCount,
            TextlessIllustrationExportCover textlessCover,
            List<TextlessIllustrationExportPage> includedPages,
            List<Integer> missingPageNumbers) {

        public TextlessIllustrationManifest(
                Long contentId,
                String externalKey,
                Instant exportedAt,
                int totalPageCount,
                List<TextlessIllustrationExportPage> includedPages,
                List<Integer> missingPageNumbers) {
            this(contentId, externalKey, exportedAt, totalPageCount, null, includedPages, missingPageNumbers);
        }
    }

    public record TextlessIllustrationExportCover(
            Long assetId,
            String objectPath,
            String mimeType,
            String fileName) {
    }

    public record TextlessIllustrationExportPage(
            int pageNumber,
            Long assetId,
            String objectPath,
            String mimeType,
            String fileName) {
    }
}
