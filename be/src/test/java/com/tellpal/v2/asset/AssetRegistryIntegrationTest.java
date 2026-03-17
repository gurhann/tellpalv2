package com.tellpal.v2.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.RefreshMediaAssetDownloadUrlCommand;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest
class AssetRegistryIntegrationTest extends PostgresIntegrationTestBase {

    private static final String SAMPLE_CHECKSUM =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Autowired
    private AssetRegistryApi assetRegistryApi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("truncate table media_assets restart identity cascade");
    }

    @Test
    void registerPersistsAssetAndAllowsNullableMetadata() {
        AssetRecord asset = assetRegistryApi.register(new RegisterMediaAssetCommand(
                AssetStorageProvider.LOCAL_STUB,
                "/content/story/moonlight/tr/original/cover.jpg",
                AssetKind.ORIGINAL_IMAGE,
                null,
                null,
                null));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        select provider, object_path, media_type, kind, mime_type, byte_size, checksum_sha256, created_at, updated_at
                        from media_assets
                        where id = ?
                        """,
                asset.assetId());

        assertThat(asset.mediaType()).isEqualTo(AssetMediaType.IMAGE);
        assertThat(asset.createdAt()).isNotNull();
        assertThat(asset.updatedAt()).isNotNull();
        assertThat(row.get("provider")).isEqualTo("LOCAL_STUB");
        assertThat(row.get("object_path")).isEqualTo("/content/story/moonlight/tr/original/cover.jpg");
        assertThat(row.get("media_type")).isEqualTo("IMAGE");
        assertThat(row.get("kind")).isEqualTo("ORIGINAL_IMAGE");
        assertThat(row.get("mime_type")).isNull();
        assertThat(row.get("byte_size")).isNull();
        assertThat(row.get("checksum_sha256")).isNull();
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();
    }

    @Test
    void refreshDownloadUrlCacheStoresSignedUrlAndExpiryTimestamps() {
        AssetRecord registered = assetRegistryApi.register(new RegisterMediaAssetCommand(
                AssetStorageProvider.LOCAL_STUB,
                "/content/story/moonlight/tr/processed/cover-thumb.jpg",
                AssetKind.THUMBNAIL_PHONE,
                "image/jpeg",
                2048L,
                SAMPLE_CHECKSUM));

        AssetRecord refreshed = assetRegistryApi.refreshDownloadUrlCache(
                new RefreshMediaAssetDownloadUrlCommand(registered.assetId()));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        select cached_download_url, download_url_cached_at, download_url_expires_at
                        from media_assets
                        where id = ?
                        """,
                registered.assetId());

        assertThat(refreshed.cachedDownloadUrl()).startsWith("http://localhost:8080/_stub/assets/");
        assertThat(refreshed.downloadUrlCachedAt()).isNotNull();
        assertThat(refreshed.downloadUrlExpiresAt()).isAfter(refreshed.downloadUrlCachedAt());
        assertThat(row.get("cached_download_url")).isEqualTo(refreshed.cachedDownloadUrl());
        assertThat(row.get("download_url_cached_at")).isNotNull();
        assertThat(row.get("download_url_expires_at")).isNotNull();
    }

    @Test
    void databaseRejectsDuplicateProviderAndObjectPath() {
        assetRegistryApi.register(new RegisterMediaAssetCommand(
                AssetStorageProvider.FIREBASE_STORAGE,
                "/content/story/moonlight/tr/original/audio.mp3",
                AssetKind.ORIGINAL_AUDIO,
                "audio/mpeg",
                4096L,
                SAMPLE_CHECKSUM));

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                        insert into media_assets (
                            provider,
                            object_path,
                            media_type,
                            kind,
                            created_at,
                            updated_at
                        ) values (?, ?, ?, ?, now(), now())
                        """,
                "FIREBASE_STORAGE",
                "/content/story/moonlight/tr/original/audio.mp3",
                "AUDIO",
                "ORIGINAL_AUDIO"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void listRecentReturnsNewestAssetsFirstAndHonorsLimit() throws Exception {
        AssetRecord first = assetRegistryApi.register(new RegisterMediaAssetCommand(
                AssetStorageProvider.LOCAL_STUB,
                "/assets/first.jpg",
                AssetKind.ORIGINAL_IMAGE,
                "image/jpeg",
                100L,
                SAMPLE_CHECKSUM));
        Thread.sleep(10L);
        AssetRecord second = assetRegistryApi.register(new RegisterMediaAssetCommand(
                AssetStorageProvider.LOCAL_STUB,
                "/assets/second.jpg",
                AssetKind.ORIGINAL_IMAGE,
                "image/jpeg",
                200L,
                SAMPLE_CHECKSUM));
        Thread.sleep(10L);
        AssetRecord third = assetRegistryApi.register(new RegisterMediaAssetCommand(
                AssetStorageProvider.LOCAL_STUB,
                "/assets/third.jpg",
                AssetKind.ORIGINAL_IMAGE,
                "image/jpeg",
                300L,
                SAMPLE_CHECKSUM));

        assertThat(assetRegistryApi.listRecent(2))
                .extracting(AssetRecord::assetId)
                .containsExactly(third.assetId(), second.assetId());
        assertThat(assetRegistryApi.listRecent(5))
                .extracting(AssetRecord::assetId)
                .containsExactly(third.assetId(), second.assetId(), first.assetId());
    }
}
