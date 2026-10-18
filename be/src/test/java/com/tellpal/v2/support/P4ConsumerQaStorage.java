package com.tellpal.v2.support;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.storage.*;

/** SDK transport'u korunur; yalnız dört sıralı immutable rendition okuması açıktır. */
final class P4ConsumerQaStorage implements AssetStorageClient {
    private final AssetStorageClient delegate;
    private final P4ConsumerQaLedger ledger;
    P4ConsumerQaStorage(AssetStorageClient delegate,P4ConsumerQaLedger ledger){this.delegate=delegate;this.ledger=ledger;}
    @Override public StorageProvider provider(){return delegate.provider();}
    @Override public AssetStorageClient withReadingDeadline(long millis){return new P4ConsumerQaStorage(delegate.withReadingDeadline(millis),ledger);}
    @Override public InputStream openVersion(VersionedStorageObject object){ledger.read(object);return delegate.openVersion(object);}
    private IllegalStateException forbidden(){return new IllegalStateException("CONSUMER_QA_STORAGE_FORBIDDEN");}
    @Override public Optional<VersionedStorageObject> observeVersion(String path){throw forbidden();}
    @Override public VersionedStorageObject createImmutable(String path,String mime,byte[] bytes){throw forbidden();}
    @Override public StorageSignedDownloadUrl createSignedDownloadUrl(String path,Instant issued){throw forbidden();}
    @Override public void uploadObject(String path,String mime,long bytes,InputStream content){throw forbidden();}
    @Override public Optional<StorageObjectMetadata> findObjectMetadata(String path){throw forbidden();}
    @Override public Optional<StorageObjectContent> openObject(String path,StorageObjectRange range){throw forbidden();}
}
