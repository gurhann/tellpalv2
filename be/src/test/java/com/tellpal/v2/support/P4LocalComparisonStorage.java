package com.tellpal.v2.support;

import java.io.InputStream;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.Optional;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.storage.*;

/** Yalnız QA kompozisyonu: tüm deadline kopyaları tek makbuzu paylaşır. */
final class P4LocalComparisonStorage implements AssetStorageClient {
    private final AssetStorageClient delegate;
    private final P4LocalComparisonLedger ledger;

    P4LocalComparisonStorage(AssetStorageClient delegate, P4LocalComparisonLedger ledger) {
        this.delegate = delegate;
        this.ledger = ledger;
    }
    @Override public StorageProvider provider() { return delegate.provider(); }
    @Override public AssetStorageClient withReadingDeadline(long millis) {
        return new P4LocalComparisonStorage(instrumentConflict(delegate.withReadingDeadline(millis)), ledger);
    }
    @Override public Optional<VersionedStorageObject> observeVersion(String path) {
        ledger.observe(path);
        return delegate.observeVersion(path);
    }
    @Override public InputStream openVersion(VersionedStorageObject object) {
        ledger.read(object.path(), object.bytes());
        return delegate.openVersion(object);
    }
    @Override public VersionedStorageObject createImmutable(String path, String mime, byte[] bytes) {
        ledger.create(path, bytes.length);
        return delegate.createImmutable(path, mime, bytes);
    }
    @Override public StorageSignedDownloadUrl createSignedDownloadUrl(String path, Instant issued) {
        throw new IllegalStateException("QA_SIGNED_URL_FORBIDDEN");
    }
    @Override public void uploadObject(String path, String mime, long bytes, InputStream content) {
        throw new IllegalStateException("QA_LEGACY_UPLOAD_FORBIDDEN");
    }
    @Override public Optional<StorageObjectMetadata> findObjectMetadata(String path) {
        throw new IllegalStateException("QA_LEGACY_METADATA_FORBIDDEN");
    }
    @Override public Optional<StorageObjectContent> openObject(String path, StorageObjectRange range) {
        throw new IllegalStateException("QA_LEGACY_READ_FORBIDDEN");
    }

    /** SDK 412'nin kendi metadata gözlemini I/O'dan önce sayar; üretim sınıfı değişmez. */
    private AssetStorageClient instrumentConflict(AssetStorageClient client) {
        if (!client.getClass().getName().equals("com.tellpal.v2.asset.infrastructure.storage.FirebaseStorageAssetClient")) return client;
        try {
            Class<?> type = client.getClass();
            var storageField = type.getDeclaredField("storage");
            var credentialField = type.getDeclaredField("serviceAccountCredentials");
            var propertyField = type.getDeclaredField("properties");
            storageField.setAccessible(true); credentialField.setAccessible(true); propertyField.setAccessible(true);
            Storage sdk = (Storage) storageField.get(client);
            Storage tracked = (Storage) Proxy.newProxyInstance(Storage.class.getClassLoader(), new Class<?>[]{Storage.class},
                    (proxy, method, args) -> {
                        try { return method.invoke(sdk, args); }
                        catch (InvocationTargetException failure) {
                            if (failure.getCause() instanceof StorageException e && e.getCode() == 412) {
                                P4LocalComparisonLedger.require(method.getName().equals("create") && args[0] instanceof com.google.cloud.storage.BlobInfo, "SDK_CONFLICT_SURFACE_CHANGED");
                                ledger.conflict(((com.google.cloud.storage.BlobInfo) args[0]).getName());
                            }
                            throw failure.getCause();
                        }
                    });
            var constructor = type.getDeclaredConstructor(Storage.class, com.google.auth.oauth2.ServiceAccountCredentials.class,
                    AssetStorageFirebaseProperties.class);
            constructor.setAccessible(true);
            return (AssetStorageClient) constructor.newInstance(tracked, credentialField.get(client), propertyField.get(client));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("SDK_TEST_DELEGATE_UNAVAILABLE", e);
        }
    }
}
