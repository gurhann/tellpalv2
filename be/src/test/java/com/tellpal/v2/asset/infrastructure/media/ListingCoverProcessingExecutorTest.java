package com.tellpal.v2.asset.infrastructure.media;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.api.AssetProcessingCommands.*;
import com.tellpal.v2.asset.application.LibraryCoverPreparationService;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.storage.*;
import com.tellpal.v2.shared.domain.LanguageCode;

class ListingCoverProcessingExecutorTest {
    private final AssetRegistryApi assets=mock(AssetRegistryApi.class);
    private final AssetProcessingApi jobs=mock(AssetProcessingApi.class);
    private final AssetStorageClientRegistry storage=mock(AssetStorageClientRegistry.class);
    private final AssetProcessingPathBuilder paths=mock(AssetProcessingPathBuilder.class);
    private final LibraryCoverPreparationService publication=mock(LibraryCoverPreparationService.class);
    private final Map<String,byte[]> objects=new HashMap<>();
    private AssetProcessingRecord job;
    private ListingCoverProcessingExecutor executor;
    private String failureMode;
    @BeforeEach void setUp() throws Exception {
        var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(600,900,BufferedImage.TYPE_INT_RGB),"png",bytes);
        objects.put("source",bytes.toByteArray());
        when(assets.findById(1L)).thenReturn(Optional.of(record(1L,"source",AssetKind.ORIGINAL_IMAGE,"image/png",bytes.size(),ListingCoverProcessingExecutor.sha256(bytes.toByteArray()))));
        when(assets.findByStorageLocation(any())).thenReturn(Optional.empty());
        when(assets.register(any())).thenAnswer(call->{RegisterMediaAssetCommand c=call.getArgument(0);return record(c.kind()==AssetKind.THUMBNAIL_PHONE?2L:3L,c.objectPath(),c.kind(),c.mimeType(),c.byteSize().intValue(),c.checksumSha256());});
        job=mock(AssetProcessingRecord.class);when(job.processingId()).thenReturn(9L);when(job.attemptCount()).thenReturn(2);when(job.leaseExpiresAt()).thenReturn(Instant.now().plusSeconds(300));when(job.coverSourceAssetId()).thenReturn(1L);when(job.contentId()).thenReturn(1L);when(job.languageCode()).thenReturn(LanguageCode.TR);when(job.target()).thenReturn(AssetProcessingTarget.localization(1L,LanguageCode.TR));when(job.kind()).thenReturn(AssetProcessingKind.LISTING_COVER);when(job.externalKey()).thenReturn("story");when(job.listingProfileVersion()).thenReturn(new ListingCoverProfile(360,480,600,800,85,20971520,40000000).version());
        when(paths.processedRoot(anyString(),anyString(),any(AssetProcessingTarget.class))).thenReturn("/local/story/");
        doAnswer(call->{String path=call.getArgument(1);if("second-upload".equals(failureMode)&&path.endsWith("tablet.jpg"))throw new IllegalStateException("storage unavailable");InputStream input=call.getArgument(4);objects.put(path,input.readAllBytes());return null;}).when(storage).uploadObject(any(),anyString(),anyString(),anyLong(),any());
        when(storage.findObjectMetadata(any(),anyString())).thenAnswer(call->{String path=call.getArgument(1);byte[] data=objects.get(path);if(data==null)return Optional.empty();return Optional.of(new StorageObjectMetadata(path.equals("source")?"image/png":"mime".equals(failureMode)?"image/png":"image/jpeg",(long)data.length));});
        when(storage.openObject(any(),anyString(),isNull())).thenAnswer(call->{String path=call.getArgument(1);byte[] data=objects.get(path);if(data==null)return Optional.empty();if("checksum".equals(failureMode)&&path.endsWith("tablet.jpg")){data=data.clone();data[data.length-3]^=1;}return Optional.of(new StorageObjectContent("image/jpeg",data.length,data.length,null,null,new ByteArrayInputStream(data)));});
        executor=new ListingCoverProcessingExecutor(assets,jobs,storage,paths,publication);
    }
    @Test void secondUploadFailureNeverPublishesHalfReadyManifestAndCarriesCapturedAttempt() {
        failureMode="second-upload";executor.process(job);verifyNoInteractions(publication);assertCapturedFailure();
    }
    @Test void storedMimeAndChecksumMismatchRejectReadyPublication() {
        for(String mode:List.of("mime","checksum")){failureMode=mode;executor.process(job);}
        verifyNoInteractions(publication);verify(jobs,times(2)).fail(any());
    }
    @Test void generatedOnlyRegistryMetadataNeverMakesArtworkReady() {
        when(assets.findByStorageLocation(any())).thenReturn(Optional.of(record(2L,"generated",AssetKind.THUMBNAIL_PHONE,"image/jpeg",0,null)));
        executor.process(job);verifyNoInteractions(publication);assertCapturedFailure();
    }
    @Test void rejectedCompletionCatchFailureStillUsesExpiredOwnershipAndCannotFailReplacement() {
        doThrow(new IllegalStateException("completion rejected")).when(publication).publish(eq(job),any());
        when(jobs.fail(any())).thenThrow(new StaleListingCoverAttemptException());
        assertThatCode(()->executor.process(job)).doesNotThrowAnyException();assertCapturedFailure();
    }
    @Test void explicitStalePublicationDoesNotEnterFailureCallback() {
        doThrow(new StaleListingCoverAttemptException()).when(publication).publish(eq(job),any());
        executor.process(job);verifyNoInteractions(jobs);
    }
    private void assertCapturedFailure(){var captor=ArgumentCaptor.forClass(FailAssetProcessingCommand.class);verify(jobs).fail(captor.capture());assertThat(captor.getValue().attempt()).isEqualTo(ListingCoverAttempt.captured(job));}
    private AssetRecord record(long id,String path,AssetKind kind,String mime,int bytes,String checksum){return new AssetRecord(id,new AssetStorageLocation(AssetStorageProvider.FIREBASE_STORAGE,path),AssetMediaType.IMAGE,kind,mime,(long)bytes,checksum,null,null,null,Instant.now(),Instant.now());}
}
