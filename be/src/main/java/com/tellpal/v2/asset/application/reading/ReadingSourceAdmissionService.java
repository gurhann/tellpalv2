package com.tellpal.v2.asset.application.reading;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.domain.*;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageInspector;
import com.tellpal.v2.asset.infrastructure.persistence.reading.JdbcReadingSourceAdmissionStore;
import com.tellpal.v2.asset.infrastructure.storage.*;

/** Admission copies exact observed bytes to a fresh backend-owned ID; existing originals remain untouched. */
@Service
public class ReadingSourceAdmissionService implements ReadingSourceAdmissionApi {
    private final MediaAssetRepository assets;
    private final ReadingObjectStorage storage;
    private final JdbcReadingSourceAdmissionStore store;
    private final String prefix;
    private final Environment environment;
    private final boolean enabled;
    public ReadingSourceAdmissionService(
            MediaAssetRepository assets,
            ReadingObjectStorage storage,
            JdbcReadingSourceAdmissionStore store,
            AssetStorageFirebaseProperties properties,
            Environment environment,
            @Value("${tellpal.asset.reading.test-engine-enabled:false}") boolean enabled) {
        this.assets = assets;
        this.storage = storage;
        this.store = store;
        this.prefix = properties.pathPrefix();
        this.environment = environment;
        this.enabled = enabled;
    }

    private void requireEnabled() {
        var profiles = Set.of(environment.getActiveProfiles());
        if (!enabled || (!profiles.contains("local") && !profiles.contains("test")) || profiles.stream().anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production")) || prefix == null || !prefix.matches("(local|test)(/[A-Za-z0-9_-]+)*")) throw new IllegalStateException("READING_ADMISSION_DISABLED");
    }

    @Override
    public Admission admit(long selectedAssetId, ReadingImagePreparationApi.TestProfile profile) {
        requireEnabled();
        ReadingPreparationCapacity.requireNoTransaction();
        try(var capacity = ReadingPreparationCapacity.acquire()) {
            ReadingPreparationCapacity.requireParentMemory(profile);
            var deadline = ReadingDeadline.start(profile.revisionMillis());
            var asset = assets.findById(selectedAssetId).orElseThrow(() -> new IllegalArgumentException("SOURCE_ASSET_MISSING"));
            if (asset.getKind() != MediaKind.ORIGINAL_IMAGE) throw new IllegalArgumentException("ORIGINAL_IMAGE_REQUIRED");
            var original = storage.observe(asset.getProvider(), asset.getObjectPath(), deadline.remainingMillis(profile.sourceMillis()));
            byte[] bytes = storage.read(asset.getProvider(), original, profile.maxSourceBytes(), deadline.remainingMillis(profile.sourceMillis()));
            var decoded = ReadingImageInspector.decode(bytes, profile);
            if (!original.mime().equals(decoded.mime())) throw new IllegalArgumentException("SOURCE_STORAGE_MIME_MISMATCH");
            UUID id = UUID.randomUUID();
            String path = prefix+"/reading/admitted/"+id;
            store.plan(id, selectedAssetId, path, asset.getProvider());
            var stored = storage.create(asset.getProvider(), path, decoded.mime(), bytes, profile.maxSourceBytes(), deadline.remainingMillis(profile.sourceMillis()));
            var persisted = storage.read(asset.getProvider(), stored, profile.maxSourceBytes(), deadline.remainingMillis(profile.sourceMillis()));
            var verified = ReadingImageInspector.decode(persisted, profile);
            String sha = ReadingIdentity.sha(bytes);
            if (!ReadingIdentity.sha(persisted).equals(sha) || !verified.mime().equals(decoded.mime())) throw new IllegalStateException("ADMISSION_VERIFICATION_FAILED");
            deadline.check();
            return store.complete(id, asset.getProvider(), stored, sha, verified.image().getWidth(), verified.image().getHeight());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("ADMISSION_VERIFICATION_FAILED", e);
        }
    }

    @Override
    public void withdraw(UUID id) {
        requireEnabled();
        store.withdraw(id);
    }

    /** Returns immutable pinned authority; registry metadata cannot supply this evidence. */
    public Map<String, Object> require(long id, boolean lock) {
        return store.require(id, lock);
    }

    public VersionedStorageObject object(Map<String, Object> row) {
        return new VersionedStorageObject((String)row.get("object_path"), (String)row.get("object_version"), (String)row.get("mime_type"), ((Number)row.get("byte_size")).longValue());
    }
}
