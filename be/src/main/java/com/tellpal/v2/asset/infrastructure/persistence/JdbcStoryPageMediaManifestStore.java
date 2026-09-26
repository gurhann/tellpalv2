package com.tellpal.v2.asset.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.StoryPageMediaManifest;
import com.tellpal.v2.asset.api.StoryPageMediaManifestEntry;
import com.tellpal.v2.asset.application.StoryPageMediaManifestStore;
import com.tellpal.v2.shared.domain.LanguageCode;

@Repository
class JdbcStoryPageMediaManifestStore implements StoryPageMediaManifestStore {

    private final JdbcTemplate jdbcTemplate;
    private final AssetRegistryApi assetRegistryApi;

    JdbcStoryPageMediaManifestStore(JdbcTemplate jdbcTemplate, AssetRegistryApi assetRegistryApi) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetRegistryApi = assetRegistryApi;
    }

    @Override
    @Transactional
    public StoryPageMediaManifest save(Long contentId, LanguageCode languageCode, String revision,
            String profileVersion, List<Entry> entries, Instant createdAt) {
        Long manifestId = jdbcTemplate.query(
                "select id from story_page_media_manifests where content_id = ? and language_code = ? and revision = ?",
                rs -> rs.next() ? rs.getLong(1) : null, contentId, languageCode.value(), revision);
        if (manifestId == null) {
            manifestId = jdbcTemplate.queryForObject("""
                    insert into story_page_media_manifests
                      (content_id, language_code, revision, profile_version, schema_version, created_at)
                    values (?, ?, ?, ?, 1, ?) returning id
                    """, Long.class, contentId, languageCode.value(), revision, profileVersion, Timestamp.from(createdAt));
            for (Entry entry : entries) {
                jdbcTemplate.update("""
                        insert into story_page_media_manifest_entries
                          (manifest_id, page_id, page_number, source_asset_id, rendition_asset_id,
                           audio_asset_id, source_checksum_sha256, width, height)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, manifestId, entry.pageId(), entry.pageNumber(), entry.sourceAssetId(),
                        entry.rendition().assetId(), entry.audioAssetId(), entry.sourceChecksumSha256(),
                        entry.width(), entry.height());
            }
        }
        return findById(manifestId).orElseThrow(() -> new IllegalStateException("Published page media manifest is missing"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoryPageMediaManifest> findLatest(Long contentId, LanguageCode languageCode) {
        Long manifestId = jdbcTemplate.query(
                "select id from story_page_media_manifests where content_id = ? and language_code = ? order by created_at desc, id desc limit 1",
                rs -> rs.next() ? rs.getLong(1) : null, contentId, languageCode.value());
        return manifestId == null ? Optional.empty() : findById(manifestId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoryPageMediaManifest> findRevision(Long contentId, LanguageCode languageCode, String revision) {
        Long manifestId = jdbcTemplate.query(
                "select id from story_page_media_manifests where content_id = ? and language_code = ? and revision = ?",
                rs -> rs.next() ? rs.getLong(1) : null, contentId, languageCode.value(), revision);
        return manifestId == null ? Optional.empty() : findById(manifestId);
    }

    private Optional<StoryPageMediaManifest> findById(Long manifestId) {
        List<StoryPageMediaManifest> manifests = jdbcTemplate.query("""
                select content_id, language_code, revision, profile_version, schema_version, created_at
                from story_page_media_manifests where id = ?
                """, (rs, _rowNum) -> new StoryPageMediaManifest(
                rs.getInt("schema_version"), rs.getLong("content_id"),
                LanguageCode.from(rs.getString("language_code")), rs.getString("revision"),
                rs.getString("profile_version"), rs.getTimestamp("created_at").toInstant(),
                loadEntries(manifestId)), manifestId);
        return manifests.stream().findFirst();
    }

    private List<StoryPageMediaManifestEntry> loadEntries(Long manifestId) {
        return jdbcTemplate.query("""
                select page_id, page_number, source_asset_id, rendition_asset_id, audio_asset_id, source_checksum_sha256,
                       width, height
                from story_page_media_manifest_entries where manifest_id = ? order by page_number
                """, (rs, _rowNum) -> new StoryPageMediaManifestEntry(
                rs.getLong("page_id"), rs.getInt("page_number"), rs.getLong("source_asset_id"),
                assetRegistryApi.findById(rs.getLong("rendition_asset_id")).orElseThrow(),
                nullableLong(rs, "audio_asset_id"), rs.getString("source_checksum_sha256"),
                rs.getInt("width"), rs.getInt("height")), manifestId);
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
