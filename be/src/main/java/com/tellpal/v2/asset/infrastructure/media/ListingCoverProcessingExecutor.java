package com.tellpal.v2.asset.infrastructure.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.api.AssetProcessingCommands.FailAssetProcessingCommand;
import com.tellpal.v2.asset.application.LibraryCoverPreparationService;
import com.tellpal.v2.asset.application.ListingCoverRevisionStore.Revision;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.storage.*;

/** Produces and verifies real phone/tablet bytes; stale callbacks never escape through the legacy catch. */
@Component
public class ListingCoverProcessingExecutor {
    private final AssetRegistryApi assets;
    private final AssetProcessingApi jobs;
    private final AssetStorageClientRegistry storage;
    private final AssetProcessingPathBuilder paths;
    private final LibraryCoverPreparationService preparation;
    public ListingCoverProcessingExecutor(AssetRegistryApi assets,AssetProcessingApi jobs,AssetStorageClientRegistry storage,
            AssetProcessingPathBuilder paths,LibraryCoverPreparationService preparation) {
        this.assets=assets;this.jobs=jobs;this.storage=storage;this.paths=paths;this.preparation=preparation;
    }
    public void process(AssetProcessingRecord job) {
        var attempt=ListingCoverAttempt.captured(job);
        try {
            var profile=ListingCoverProfile.parse(job.listingProfileVersion());
            var source=assets.findById(job.coverSourceAssetId()).orElseThrow(()->new IllegalArgumentException("Cover source is missing"));
            if(source.mediaType()!=AssetMediaType.IMAGE) throw new IllegalArgumentException("Cover source must be an image");
            var provider=StorageProvider.valueOf(source.storageLocation().provider().name());
            var path=source.storageLocation().objectPath();
            var metadata=storage.findObjectMetadata(provider,path).orElseThrow(()->new IllegalArgumentException("Cover source bytes are missing"));
            if(metadata.byteSize()==null || metadata.byteSize()<=0 || metadata.byteSize()>profile.maxSourceBytes()
                    || !java.util.Objects.equals(source.mimeType(),metadata.mimeType())) throw new IllegalArgumentException("Cover source metadata is invalid");
            byte[] bytes=read(provider,path,profile.maxSourceBytes());
            if(bytes.length!=metadata.byteSize()) throw new IllegalArgumentException("Cover source byte size differs");
            String checksum=sha256(bytes);
            if(source.checksumSha256()!=null && !checksum.equals(source.checksumSha256())) throw new IllegalArgumentException("Cover source checksum differs");
            String revision=sha256((job.contentId()+"|"+job.languageCode()+"|"+source.assetId()+"|"+checksum+"|"+profile.version()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var phone=new ListingCoverRenditionProcessor(profile.phoneWidth(),profile.phoneHeight(),profile.maxPixels(),profile.quality()).process(bytes,source.mimeType());
            var tablet=new ListingCoverRenditionProcessor(profile.tabletWidth(),profile.tabletHeight(),profile.maxPixels(),profile.quality()).process(bytes,source.mimeType());
            String root=paths.processedRoot("STORY",job.externalKey(),job.target())+"listing-covers/"+revision+"/";
            long phoneId=upload(provider,root+"phone.jpg",AssetKind.THUMBNAIL_PHONE,phone);
            long tabletId=upload(provider,root+"tablet.jpg",AssetKind.THUMBNAIL_TABLET,tablet);
            preparation.publish(job,new Revision(job.contentId(),job.languageCode().value(),source.assetId(),checksum,profile.version(),revision,
                    phoneId,tabletId,phone.width(),phone.height(),tablet.width(),tablet.height()));
        } catch(StaleListingCoverAttemptException ignored) {
            // Expired workers intentionally have no failure transition against their replacement.
        } catch(RuntimeException e) {
            try {jobs.fail(new FailAssetProcessingCommand(job.target(),job.kind(),"LISTING_COVER_FAILED",
                    "Listing image preparation failed",attempt));}
            catch(StaleListingCoverAttemptException ignored) { /* Replacement attempt remains untouched. */ }
        }
    }
    private byte[] read(StorageProvider provider,String path,int limit) {
        var object=storage.openObject(provider,path,null).orElseThrow(()->new IllegalArgumentException("Image bytes unavailable"));
        try(var input=object.content()) {byte[] bytes=input.readNBytes(limit+1);if(bytes.length==0 || bytes.length>limit) throw new IllegalArgumentException("Image byte limit exceeded");return bytes;}
        catch(IOException e) {throw new IllegalArgumentException("Image read failed",e);}
    }
    private long upload(StorageProvider provider,String path,AssetKind kind,ListingCoverRenditionProcessor.ProcessedPageImage image) {
        storage.uploadObject(provider,path,image.mimeType(),image.bytes().length,new ByteArrayInputStream(image.bytes()));
        var metadata=storage.findObjectMetadata(provider,path).orElseThrow(()->new IllegalStateException("Stored thumbnail missing"));
        if(!image.mimeType().equals(metadata.mimeType()) || metadata.byteSize()==null || metadata.byteSize()!=image.bytes().length)
            throw new IllegalStateException("Stored thumbnail metadata differs");
        byte[] actual=read(provider,path,image.bytes().length);
        if(!sha256(actual).equals(sha256(image.bytes()))) throw new IllegalStateException("Stored thumbnail checksum differs");
        try {var decoded=javax.imageio.ImageIO.read(new ByteArrayInputStream(actual));
            if(decoded==null || decoded.getWidth()!=image.width() || decoded.getHeight()!=image.height()) throw new IllegalStateException("Stored thumbnail dimensions differ");
        } catch(IOException e) {throw new IllegalStateException("Stored thumbnail decoding failed",e);}
        var location=new AssetStorageLocation(AssetStorageProvider.valueOf(provider.name()),path);
        var record=assets.findByStorageLocation(location).orElseGet(()->assets.register(new RegisterMediaAssetCommand(location.provider(),path,kind,
                "image/jpeg",(long)actual.length,sha256(actual))));
        if(record.kind()!=kind || !"image/jpeg".equals(record.mimeType()) || record.byteSize()==null || record.byteSize()!=actual.length
                || !sha256(actual).equals(record.checksumSha256())) throw new IllegalStateException("Generated-only thumbnail registry rejected");
        return record.assetId();
    }
    public static String sha256(byte[] bytes) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
