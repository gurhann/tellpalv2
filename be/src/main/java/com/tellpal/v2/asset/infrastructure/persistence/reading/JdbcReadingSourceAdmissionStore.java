package com.tellpal.v2.asset.infrastructure.persistence.reading;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.tellpal.v2.asset.domain.*;
import com.tellpal.v2.asset.infrastructure.storage.VersionedStorageObject;
import com.tellpal.v2.asset.api.ReadingSourceAdmissionApi.Admission;

/** Plans durable ownership before I/O and commits new asset/admission evidence together afterward. */
@Repository
public class JdbcReadingSourceAdmissionStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final MediaAssetRepository assets;
    public JdbcReadingSourceAdmissionStore(JdbcTemplate jdbc, PlatformTransactionManager manager, MediaAssetRepository assets) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(manager);
        this.assets = assets;
    }

    public void plan(UUID id, long selected, String path, StorageProvider provider) {
        tx.executeWithoutResult(s -> jdbc.update("""
                insert into reading_source_admissions(id,selected_asset_id,object_path,provider)
                values(?,?,?,?)
                """, id, selected, path, provider.name()));
    }

    public Admission complete(UUID id, StorageProvider provider, VersionedStorageObject object, String sha, int width, int height) {
        return tx.execute(s -> {
            var row = jdbc.queryForMap("""
                select * from reading_source_admissions
                where id=? for update
                """, id);
            if (!row.get("state").equals("PLANNED") || !row.get("object_path").equals(object.path())) throw new IllegalStateException("ADMISSION_OWNERSHIP_LOST");
            var asset = MediaAsset.register(provider, object.path(), MediaKind.ORIGINAL_IMAGE);
            asset.updateMetadata(object.mime(), object.bytes(), sha);
            asset = assets.save(asset);
            jdbc.update("""
                update reading_source_admissions
                set asset_id=?,object_version=?,mime_type=?,byte_size=?,sha256=?,width=?,height=?,state='ADMITTED'
                where id=?
                """, asset.getId(), object.version(), object.mime(), object.bytes(), sha, width, height, id);
            return new Admission(id, asset.getId(), 1);
        });
    }

    public Map<String, Object> require(long assetId, boolean lock) {
        var rows = jdbc.queryForList("""
                select * from reading_source_admissions
                where asset_id=?
                """+(lock?" for update":""), assetId);
        if (rows.size() != 1 || !rows.getFirst().get("state").equals("ADMITTED")) throw new com.tellpal.v2.asset.domain.reading.ReadingSourceNotAdmittedException();
        return rows.getFirst();
    }

    public void withdraw(UUID id) {
        tx.executeWithoutResult(s -> jdbc.update("""
                update reading_source_admissions
                set state='WITHDRAWN',epoch=epoch+1
                where id=?
                and state<>'WITHDRAWN'
                """, id));
    }
}
