package com.tellpal.v2.asset;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.Optional;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.application.StoryDetailHeroQueryService;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.media.*;
import com.tellpal.v2.asset.infrastructure.storage.*;

class StoryDetailHeroQueryServiceTest {
    final AssetRegistryApi assets=mock(AssetRegistryApi.class);
    final AssetStorageClientRegistry storage=mock(AssetStorageClientRegistry.class);
    final StoryDetailHeroQueryService heroes=new StoryDetailHeroQueryService(assets,storage,new ListingCoverProfile(360,480,600,800,85,20971520,40000000),Clock.systemUTC());
    @AfterEach void close() {heroes.close();}
    @Test void signsOnlyActualDecodableMatchingSourceAndExposesDimensions() throws Exception {
        var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(700,900,BufferedImage.TYPE_INT_RGB),"png",output);
        byte[] bytes=output.toByteArray();configure(bytes,ListingCoverProcessingExecutor.sha256(bytes));
        when(storage.createSignedDownloadUrl(eq(StorageProvider.FIREBASE_STORAGE),eq("test/cover"),any())).thenReturn(new StorageSignedDownloadUrl("https://example.invalid/cover",Instant.now().plusSeconds(60)));
        var hero=heroes.find(1,"tr",5L);
        assertThat(hero).isNotNull();assertThat(hero.width()).isEqualTo(700);assertThat(hero.height()).isEqualTo(900);
        assertThat(hero.assetId()).isEqualTo(5);assertThat(hero.byteSize()).isEqualTo(bytes.length);
    }
    @Test void corruptChecksumAndMissingSourcesKeepMetadataArtworkAbsent() {
        byte[] bytes={1,2,3};configure(bytes,"a".repeat(64));assertThat(heroes.find(1,"tr",5L)).isNull();
        configure(bytes,ListingCoverProcessingExecutor.sha256(bytes));assertThat(heroes.find(1,"tr",5L)).isNull();
        assertThat(heroes.find(1,"tr",null)).isNull();verify(storage,never()).createSignedDownloadUrl(any(),any(),any());
    }
    @Test void byteAndPixelOverflowNeverSignARegisteredSource() throws Exception {
        var small=new StoryDetailHeroQueryService(assets,storage,new ListingCoverProfile(3,4,6,8,85,16,100),Clock.systemUTC());
        try {
            byte[] oversized=new byte[17]; configure(oversized,ListingCoverProcessingExecutor.sha256(oversized));
            assertThat(small.find(1,"tr",5L)).isNull();
            var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(20,20,BufferedImage.TYPE_INT_RGB),"png",output);
            byte[] image=output.toByteArray();configure(image,ListingCoverProcessingExecutor.sha256(image));
            var pixels=new StoryDetailHeroQueryService(assets,storage,new ListingCoverProfile(3,4,6,8,85,1024,100),Clock.systemUTC());
            try {assertThat(pixels.find(1,"tr",5L)).isNull();} finally {pixels.close();}
            verify(storage,never()).createSignedDownloadUrl(any(),any(),any());
        } finally {small.close();}
    }
    @Test void cancelledUnsettledStorageKeepsFourSlotsUntilActualRelease() throws Exception {
        var bounded=new StoryDetailHeroQueryService(assets,storage,new ListingCoverProfile(3,4,6,8,85,1024,100),Clock.systemUTC(),50);
        var release=new java.util.concurrent.CountDownLatch(1);
        var started=new java.util.concurrent.CountDownLatch(4);
        when(assets.findById(5L)).thenReturn(Optional.of(new AssetRecord(5L,new AssetStorageLocation(AssetStorageProvider.FIREBASE_STORAGE,"test/cover"),AssetMediaType.IMAGE,AssetKind.ORIGINAL_IMAGE,"image/png",3L,"a".repeat(64),null,null,null,Instant.now(),Instant.now())));
        when(storage.openObject(StorageProvider.FIREBASE_STORAGE,"test/cover",null)).thenAnswer(i->{
            started.countDown(); boolean done=false;
            while(!done) {try {release.await(); done=true;} catch(InterruptedException ignored) {}}
            return Optional.empty();
        });
        try (var callers=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var futures=new java.util.ArrayList<java.util.concurrent.Future<StoryDetailHeroQueryApi.Hero>>();
            for(int i=0;i<4;i++) futures.add(callers.submit(()->bounded.find(1,"tr",5L)));
            assertThat(started.await(1,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            for(var future:futures) assertThat(future.get(1,java.util.concurrent.TimeUnit.SECONDS)).isNull();
            assertThat(bounded.ownedAttemptCount()).isEqualTo(4);
            assertThat(bounded.find(1,"tr",5L)).isNull();
            verify(storage,times(4)).openObject(StorageProvider.FIREBASE_STORAGE,"test/cover",null);
            release.countDown();
            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(1)).untilAsserted(()->assertThat(bounded.ownedAttemptCount()).isZero());
            assertThat(bounded.find(1,"tr",5L)).isNull();verify(storage,times(5)).openObject(StorageProvider.FIREBASE_STORAGE,"test/cover",null);
        } finally {release.countDown();bounded.close();}
    }
    private void configure(byte[] bytes,String checksum) {
        when(assets.findById(5L)).thenReturn(Optional.of(new AssetRecord(5L,new AssetStorageLocation(AssetStorageProvider.FIREBASE_STORAGE,"test/cover"),AssetMediaType.IMAGE,AssetKind.ORIGINAL_IMAGE,"image/png",(long)bytes.length,checksum,null,null,null,Instant.now(),Instant.now())));
        when(storage.openObject(StorageProvider.FIREBASE_STORAGE,"test/cover",null)).thenAnswer(i->Optional.of(new StorageObjectContent("image/png",bytes.length,bytes.length,null,null,new ByteArrayInputStream(bytes))));
    }
}
