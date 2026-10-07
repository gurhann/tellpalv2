package com.tellpal.v2.asset.application;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.media.ListingCoverProfile;
import com.tellpal.v2.asset.infrastructure.media.ListingCoverProcessingExecutor;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageClientRegistry;

/** Validates actual current-source bytes within a bounded, query-only artwork envelope. */
@Service
public class StoryDetailHeroQueryService implements StoryDetailHeroQueryApi {
    private final AssetRegistryApi assets;
    private final AssetStorageClientRegistry storage;
    private final ListingCoverProfile profile;
    private final Clock clock;
    private final long waitMillis;
    private final Semaphore attempts=new Semaphore(4);
    private final ExecutorService workers=Executors.newVirtualThreadPerTaskExecutor();
    public StoryDetailHeroQueryService(AssetRegistryApi assets,AssetStorageClientRegistry storage,
            ListingCoverProfile profile,Clock clock) {
        this(assets,storage,profile,clock,8000);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public StoryDetailHeroQueryService(AssetRegistryApi assets,AssetStorageClientRegistry storage,ListingCoverProfile profile,Clock clock,
            @org.springframework.beans.factory.annotation.Value("${tellpal.library.detail-hero-wait-ms:8000}") long waitMillis) {
        if(waitMillis<1 || waitMillis>8000) throw new IllegalArgumentException("Invalid detail hero wait");
        this.assets=assets; this.storage=storage; this.profile=profile; this.clock=clock; this.waitMillis=waitMillis;
    }
    @Override public Hero find(long contentId,String language,Long source) {
        if(source==null || !attempts.tryAcquire()) return null;
        var future=workers.submit(() -> {
            try {return verify(source);} finally {attempts.release();}
        });
        try {return future.get(waitMillis,TimeUnit.MILLISECONDS);}
        catch(InterruptedException e) {Thread.currentThread().interrupt(); future.cancel(true); return null;}
        catch(Exception e) {future.cancel(true); return null;}
    }
    private Hero verify(long source) throws Exception {
        var asset=assets.findById(source).orElse(null);
        if(asset==null || asset.kind()!=AssetKind.ORIGINAL_IMAGE || asset.mediaType()!=AssetMediaType.IMAGE
            || !("image/png".equals(asset.mimeType()) || "image/jpeg".equals(asset.mimeType()))
            || asset.byteSize()==null || asset.byteSize()<=0 || asset.byteSize()>profile.maxSourceBytes()
            || asset.checksumSha256()==null || !asset.checksumSha256().matches("[a-f0-9]{64}")) return null;
        var provider=StorageProvider.valueOf(asset.storageLocation().provider().name());
        String path=asset.storageLocation().objectPath();
        var object=storage.openObject(provider,path,null).orElse(null);
        if(object==null) return null;
        byte[] bytes;
        try(var input=object.content()) {
            if(object.byteSize()!=asset.byteSize() || !object.mimeType().equals(asset.mimeType())) return null;
            bytes=input.readNBytes(profile.maxSourceBytes()+1);
        }
        if(Thread.currentThread().isInterrupted() || bytes.length!=asset.byteSize()
            || !ListingCoverProcessingExecutor.sha256(bytes).equals(asset.checksumSha256())) return null;
        int width,height;
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) return null;
            var reader=readers.next();
            try {
                reader.setInput(input,true,true);
                String format=reader.getFormatName();
                if(!("image/png".equals(asset.mimeType()) && "png".equalsIgnoreCase(format))
                    && !("image/jpeg".equals(asset.mimeType()) && ("jpeg".equalsIgnoreCase(format)||"jpg".equalsIgnoreCase(format)))) return null;
                width=reader.getWidth(0); height=reader.getHeight(0);
                if(width<=0 || height<=0 || (long)width*height>profile.maxPixels()) return null;
                // Verify decoding without allocating the full 40-million-pixel source.
                int sample=Math.max(1,(int)Math.ceil(Math.max(width/1600d,height/2048d)));
                var parameters=reader.getDefaultReadParam(); parameters.setSourceSubsampling(sample,sample,0,0);
                var decoded=reader.read(0,parameters);
                if(decoded==null) return null;
                decoded.flush();
            } finally {reader.dispose();}
        }
        if(Thread.currentThread().isInterrupted()) return null;
        var signed=storage.createSignedDownloadUrl(provider,path,Instant.now(clock));
        return new Hero(source,asset.checksumSha256(),asset.mimeType(),bytes.length,signed.url(),signed.expiresAt(),width,height);
    }
    public int ownedAttemptCount() {return 4-attempts.availablePermits();}
    @PreDestroy public void close() {workers.shutdownNow();}
}
