package com.tellpal.v2.asset.infrastructure.storage;

import java.io.*;
import org.springframework.stereotype.Component;
import com.tellpal.v2.asset.application.reading.ReadingPreparationCapacity;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.domain.reading.ReadingIdentity;
import com.tellpal.v2.asset.domain.reading.ReadingDeadline;

/** Bounded generation-pinned reads and create-only equality-checked writes; never legacy overwrite. */
@Component
public class ReadingObjectStorage {
    private final AssetStorageClientRegistry clients;
    public ReadingObjectStorage(AssetStorageClientRegistry clients) { this.clients=clients; }
    public VersionedStorageObject observe(StorageProvider provider,String path) { return observe(provider,path,1000); }
    public VersionedStorageObject observe(StorageProvider provider,String path,long millis) {
        ReadingPreparationCapacity.requireNoTransaction();
        var deadline=ReadingDeadline.start(millis);
        var observed=clients.clientFor(provider).withReadingDeadline(deadline.remainingMillis(millis)).observeVersion(path)
                .orElseThrow(() -> new IllegalStateException("STORAGE_OBJECT_MISSING"));
        deadline.check();return observed;
    }
    public byte[] read(StorageProvider provider,VersionedStorageObject object,long limit) throws IOException { return read(provider,object,limit,1000); }
    public byte[] read(StorageProvider provider,VersionedStorageObject object,long limit,long millis) throws IOException {
        ReadingPreparationCapacity.requireNoTransaction();
        if(object.bytes()>limit || object.bytes()<=0 || limit>Integer.MAX_VALUE-8L) throw new IOException("STORAGE_BYTE_LIMIT");
        var deadline=ReadingDeadline.start(millis);
        try(var input=clients.clientFor(provider).withReadingDeadline(deadline.remainingMillis(millis)).openVersion(object)) {
            var captured=new ByteArrayOutputStream(); byte[] buffer=new byte[8192]; int count;
            while((count=input.read(buffer,0,(int)Math.min(buffer.length,object.bytes()+1-captured.size())))!=-1) {
                deadline.check();
                captured.write(buffer,0,count);
                if(captured.size()>object.bytes()) throw new IOException("STORAGE_SIZE_MISMATCH");
            }
            byte[] bytes=captured.toByteArray();
            if(bytes.length!=object.bytes()) throw new IOException("STORAGE_SIZE_MISMATCH");
            deadline.check();
            return bytes;
        }
    }
    public VersionedStorageObject create(StorageProvider provider,String path,String mime,byte[] bytes,long limit) throws IOException { return create(provider,path,mime,bytes,limit,1000); }
    public VersionedStorageObject create(StorageProvider provider,String path,String mime,byte[] bytes,long limit,long millis) throws IOException {
        ReadingPreparationCapacity.requireNoTransaction();
        var deadline=ReadingDeadline.start(millis);
        var stored=clients.clientFor(provider).withReadingDeadline(deadline.remainingMillis(millis)).createImmutable(path,mime,bytes);
        if(!mime.equals(stored.mime()) || stored.bytes()!=bytes.length || !ReadingIdentity.sha(read(provider,stored,limit,deadline.remainingMillis(millis))).equals(ReadingIdentity.sha(bytes)))
            throw new IOException("IMMUTABLE_OBJECT_CONFLICT");
        deadline.check();return stored;
    }
}
