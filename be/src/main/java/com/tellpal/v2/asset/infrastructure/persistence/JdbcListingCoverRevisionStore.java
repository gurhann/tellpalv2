package com.tellpal.v2.asset.infrastructure.persistence;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.tellpal.v2.asset.application.ListingCoverRevisionStore;

@Repository
class JdbcListingCoverRevisionStore implements ListingCoverRevisionStore {
    private final JdbcTemplate jdbc;
    JdbcListingCoverRevisionStore(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    @Override public Optional<Revision> find(long contentId,String language,long source,String profile) {
        return jdbc.query("""
            select content_id,language_code,source_asset_id,source_checksum_sha256,profile_version,revision,
                   phone_asset_id,tablet_asset_id,phone_width,phone_height,tablet_width,tablet_height
              from listing_cover_revisions where content_id=? and language_code=? and source_asset_id=? and profile_version=?
            """,(r,n)->new Revision(r.getLong(1),r.getString(2),r.getLong(3),r.getString(4),r.getString(5),r.getString(6),
                r.getLong(7),r.getLong(8),r.getInt(9),r.getInt(10),r.getInt(11),r.getInt(12)),contentId,language,source,profile).stream().findFirst();
    }
    @Override public void save(Revision r) {
        jdbc.update("""
            insert into listing_cover_revisions(content_id,language_code,source_asset_id,source_checksum_sha256,profile_version,
                revision,phone_asset_id,tablet_asset_id,phone_width,phone_height,tablet_width,tablet_height)
            values(?,?,?,?,?,?,?,?,?,?,?,?) on conflict(content_id,language_code,source_asset_id,profile_version) do nothing
            """,r.contentId(),r.language(),r.sourceAssetId(),r.sourceChecksum(),r.profile(),r.revision(),r.phoneAssetId(),r.tabletAssetId(),
            r.phoneWidth(),r.phoneHeight(),r.tabletWidth(),r.tabletHeight());
    }
}
