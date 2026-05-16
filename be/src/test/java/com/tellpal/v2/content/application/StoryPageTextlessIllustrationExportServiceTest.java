package com.tellpal.v2.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tellpal.v2.asset.api.AssetContent;
import com.tellpal.v2.asset.api.AssetContentAccessToken;
import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageLocation;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.content.application.ContentApplicationExceptions.StoryPageTextlessIllustrationsMissingException;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.StoryPage;

class StoryPageTextlessIllustrationExportServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-16T09:00:00Z");

    private final ContentRepository contentRepository = org.mockito.Mockito.mock(ContentRepository.class);
    private final AssetRegistryApi assetRegistryApi = org.mockito.Mockito.mock(AssetRegistryApi.class);
    private final StoryPageTextlessIllustrationExportService service =
            new StoryPageTextlessIllustrationExportService(
                    contentRepository,
                    assetRegistryApi,
                    new ObjectMapper().findAndRegisterModules(),
                    Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsZipWithManifestAndTextlessImages() throws Exception {
        Content content = storyContent();
        StoryPage firstPage = content.addStoryPage(null);
        StoryPage secondPage = content.addStoryPage(null);
        firstPage.updateTextlessIllustrationMediaId(101L);
        ReflectionTestUtils.setField(content, "id", 51L);
        when(contentRepository.findByIdForStoryPageAdminRead(51L)).thenReturn(Optional.of(content));
        when(assetRegistryApi.findById(101L)).thenReturn(Optional.of(assetRecord(101L)));
        when(assetRegistryApi.issueContentAccessToken(101L))
                .thenReturn(new AssetContentAccessToken("token-101", NOW.plusSeconds(300)));
        when(assetRegistryApi.openContent(101L, "token-101", null))
                .thenReturn(new AssetContent(
                        101L,
                        "source-page-1.jpg",
                        "image/jpeg",
                        10,
                        10,
                        null,
                        null,
                        new ByteArrayInputStream("image-data".getBytes())));

        StoryPageTextlessIllustrationExportService.TextlessIllustrationExport export =
                service.prepareExport(51L);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        service.writeZip(export, outputStream);

        assertThat(export.fileName()).isEqualTo("evening-garden-textless-story-pages.zip");
        assertThat(export.manifest().missingPageNumbers()).containsExactly(secondPage.getPageNumber());
        try (ZipInputStream zipInputStream = new ZipInputStream(
                new ByteArrayInputStream(outputStream.toByteArray()))) {
            assertThat(zipInputStream.getNextEntry().getName()).isEqualTo("manifest.json");
            String manifestJson = new String(zipInputStream.readAllBytes());
            assertThat(manifestJson).contains("\"contentId\" : 51");
            assertThat(manifestJson).contains("\"missingPageNumbers\" : [ 2 ]");
            assertThat(zipInputStream.getNextEntry().getName()).isEqualTo("page-001/textless.jpg");
            assertThat(new String(zipInputStream.readAllBytes())).isEqualTo("image-data");
        }
    }

    @Test
    void rejectsExportWhenNoTextlessImagesAreAvailable() {
        Content content = storyContent();
        content.addStoryPage(null);
        ReflectionTestUtils.setField(content, "id", 51L);
        when(contentRepository.findByIdForStoryPageAdminRead(51L)).thenReturn(Optional.of(content));

        assertThatThrownBy(() -> service.prepareExport(51L))
                .isInstanceOf(StoryPageTextlessIllustrationsMissingException.class);
    }

    private static Content storyContent() {
        return Content.create(ContentType.STORY, "evening-garden", 5, true);
    }

    private static AssetRecord assetRecord(Long assetId) {
        return new AssetRecord(
                assetId,
                new AssetStorageLocation(AssetStorageProvider.LOCAL_STUB, "/source/page-1.jpg"),
                AssetMediaType.IMAGE,
                AssetKind.ORIGINAL_IMAGE,
                "image/jpeg",
                10L,
                null,
                null,
                null,
                null,
                NOW,
                NOW);
    }
}
