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
        var admitted = store.find(selectedAssetId);
        if (admitted.isPresent()) {
            var row = store.require(selectedAssetId, false);
            ReadingPreparationCapacity.requireParentMemory(profile);
            if (((Number) row.get("byte_size")).longValue() > profile.maxSourceBytes()
                    || Math.multiplyExact(((Number) row.get("width")).longValue(), ((Number) row.get("height")).longValue()) > profile.maxPixels())
                throw new IllegalArgumentException("ADMITTED_SOURCE_RESOURCE_LIMIT");
            if (com.tellpal.v2.asset.infrastructure.media.reading.ReadingPreservation.enabled(profile)
                    && !Set.of("image/jpeg", "image/png").contains(row.get("mime_type")))
                throw new IllegalArgumentException("SOURCE_PNG_OR_JPEG_REQUIRED");
            return new Admission((UUID) row.get("id"), selectedAssetId, ((Number) row.get("epoch")).longValue());
        }
        try(var capacity = ReadingPreparationCapacity.acquire()) {
            ReadingPreparationCapacity.requireParentMemory(profile);
            var deadline = ReadingDeadline.start(profile.revisionMillis());
            var asset = assets.findById(selectedAssetId).orElseThrow(() -> new IllegalArgumentException("SOURCE_ASSET_MISSING"));
            if (asset.getKind() != MediaKind.ORIGINAL_IMAGE) throw new IllegalArgumentException("ORIGINAL_IMAGE_REQUIRED");
            var original = storage.observe(asset.getProvider(), asset.getObjectPath(), deadline.remainingMillis(profile.sourceMillis()));
            byte[] bytes = storage.read(asset.getProvider(), original, profile.maxSourceBytes(), deadline.remainingMillis(profile.sourceMillis()));
            var decoded = ReadingImageInspector.decode(bytes, profile);
            if (!original.mime().equals(decoded.mime())) throw new IllegalArgumentException("SOURCE_STORAGE_MIME_MISMATCH");
            if (com.tellpal.v2.asset.infrastructure.media.reading.ReadingPreservation.enabled(profile) && decoded.mime().equals("image/webp")) throw new IllegalArgumentException("SOURCE_PNG_OR_JPEG_REQUIRED");
            UUID id = UUID.randomUUID();
            String path = prefix+"/reading/admitted/"+id;
            store.plan(id, selectedAssetId, path, asset.getProvider());
            var stored = storage.create(asset.getProvider(), path, decoded.mime(), bytes, profile.maxSourceBytes(), deadline.remainingMillis(profile.sourceMillis()));
            var persisted = storage.read(asset.getProvider(), stored, profile.maxSourceBytes(), deadline.remainingMillis(profile.sourceMillis()));
            var verified = ReadingImageInspector.decode(persisted, profile);
            String sha = ReadingIdentity.sha(bytes);
            if (!ReadingIdentity.sha(persisted).equals(sha) || !verified.mime().equals(decoded.mime())) throw new IllegalStateException("ADMISSION_VERIFICATION_FAILED");
            if (com.tellpal.v2.asset.infrastructure.media.reading.ReadingPreservation.enabled(profile) && !java.util.Arrays.equals(bytes, persisted)) throw new IllegalStateException("ADMISSION_BYTE_MISMATCH");
            deadline.check();
            try {
                return store.complete(id, asset.getProvider(), stored, sha, verified.image().getWidth(), verified.image().getHeight());
            } finally {
                decoded.image().flush();
                verified.image().flush();
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("ADMISSION_VERIFICATION_FAILED", e);
        }
    }

    @Override
    public boolean guard(List<Admission> admissions) {
        requireEnabled();
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("ADMISSION_GUARD_TRANSACTION_REQUIRED");
        for (var expected : admissions.stream().sorted(Comparator.comparingLong(Admission::assetId)).toList()) {
            try {
                var row = store.require(expected.assetId(), true);
                if (!expected.id().equals(row.get("id")) || expected.epoch()!=((Number) row.get("epoch")).longValue()) return false;
            } catch (com.tellpal.v2.asset.domain.reading.ReadingSourceNotAdmittedException withdrawn) { return false; }
        }
        return true;
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
