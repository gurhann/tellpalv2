package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.tellpal.v2.asset.infrastructure.storage.*;

class P4LocalComparisonLedgerTest {
    @TempDir Path directory;
    static final String PATH=P4LocalComparisonLedger.PREFIX+"/one.png";
    @Test void defaultDoesNotRequireAnyConfigurationOrFiles() {
        var output=new java.io.ByteArrayOutputStream();
        assertThat(P4LocalComparisonSetup.run(new String[0],new java.io.PrintStream(output))).isZero();
        assertThat(output.toString()).contains("ZERO_FILE_CREDENTIAL_DB_NETWORK_IO");
    }
    @Test void deadlineCloneSharesBudgetAndReservesBeforeDelegateEvenWhenItFails() throws Exception {
        Path receipt=directory.resolve("ledger");
        var raw=mock(AssetStorageClient.class);
        when(raw.withReadingDeadline(anyLong())).thenReturn(raw);
        when(raw.openVersion(any())).thenThrow(new IllegalStateException("transport"));
        var object=new VersionedStorageObject(PATH,"1","image/png",100);
        try(var ledger=new P4LocalComparisonLedger(receipt,Clock.systemUTC())) {
            ledger.phase(0);var storage=new P4LocalComparisonStorage(raw,ledger);
            for(int i=0;i<3;i++) assertThatThrownBy(()->storage.withReadingDeadline(10).openVersion(object)).hasMessage("transport");
            assertThatThrownBy(()->storage.withReadingDeadline(10).openVersion(object)).hasMessage("READ_BUDGET");
            verify(raw,times(3)).openVersion(object);
            assertThat(Files.readString(receipt)).contains("READ_RESERVED 3 300");
        }
        assertThatThrownBy(()->new P4LocalComparisonLedger(receipt,Clock.systemUTC())).isInstanceOf(FileAlreadyExistsException.class);
    }
    @Test void invalidScopePhaseAndExpiredWindowRefuseBeforeStorage() throws Exception {
        var time=mock(Clock.class);when(time.millis()).thenReturn(1L);var raw=mock(AssetStorageClient.class);
        try(var ledger=new P4LocalComparisonLedger(directory.resolve("ledger"),time)) {
            var storage=new P4LocalComparisonStorage(raw,ledger);
            assertThatThrownBy(()->storage.openVersion(new VersionedStorageObject("prod/image","1","image/png",10))).hasMessage("STORAGE_SCOPE");
            assertThatThrownBy(()->ledger.phase(1)).hasMessage("PHASE_REPLAY_OR_ORDER");
            ledger.phase(0);when(time.millis()).thenReturn(900001L);
            assertThatThrownBy(()->storage.createImmutable(PATH,"image/png",new byte[10])).hasMessage("RUN_WINDOW_OR_FAILED");
            verifyNoInteractions(raw);
        }
    }
    @Test void concurrentCallsCannotPassThreeReadPhaseAndFailuresNeverRefund() throws Exception {
        try(var ledger=new P4LocalComparisonLedger(directory.resolve("ledger"),Clock.systemUTC());var pool=Executors.newFixedThreadPool(8)) {
            ledger.phase(0);var tasks=new java.util.ArrayList<Future<Boolean>>();
            for(int i=0;i<8;i++)tasks.add(pool.submit(()->{try{ledger.read(PATH,1);return true;}catch(IllegalStateException e){return false;}}));
            int allowed=0;for(var task:tasks)if(task.get())allowed++;assertThat(allowed).isEqualTo(3);
        }
    }
    @Test void exactlyOneKnownDuplicateConflictConsumesObservationButOtherConflictsFail() throws Exception {
        try(var ledger=new P4LocalComparisonLedger(directory.resolve("ledger"),Clock.systemUTC())) {
            ledger.phase(0);ledger.phase(1);ledger.phase(2);ledger.create(PATH,10);
            assertThatThrownBy(()->ledger.conflict(PATH)).hasMessage("UNEXPECTED_CONFLICT");
            ledger.create(PATH,10);ledger.conflict(PATH);
            assertThatThrownBy(()->ledger.conflict(PATH)).hasMessage("UNEXPECTED_CONFLICT");
        }
    }
    @Test void loginVerificationConsumesOneOfTheSixtyFourTotalVerifications() throws Exception {
        try(var ledger=new P4LocalComparisonLedger(directory.resolve("auth-ledger"),Clock.systemUTC(),1)) {
            for(int i=0;i<=5;i++)ledger.phase(i);
            for(int i=0;i<63;i++)ledger.verify();
            assertThatThrownBy(ledger::verify).hasMessage("AUTH_BUDGET");
        }
    }
    @Test void actualSdkConflictObservationIsReservedAndUnexpectedConflictNeverReachesMetadataIo() throws Exception {
        var sdk=mock(com.google.cloud.storage.Storage.class);
        var blob=mock(com.google.cloud.storage.Blob.class);
        when(blob.getGeneration()).thenReturn(1L);when(blob.getSize()).thenReturn(10L);when(blob.getContentType()).thenReturn("image/png");
        when(sdk.create(any(com.google.cloud.storage.BlobInfo.class),any(byte[].class),any(com.google.cloud.storage.Storage.BlobTargetOption[].class)))
                .thenReturn(blob).thenThrow(new com.google.cloud.storage.StorageException(412,"exists"));
        when(sdk.get(any(com.google.cloud.storage.BlobId.class))).thenReturn(blob);
        Class<?> type=Class.forName("com.tellpal.v2.asset.infrastructure.storage.FirebaseStorageAssetClient");
        var ctor=type.getDeclaredConstructor(com.google.cloud.storage.Storage.class,com.google.auth.oauth2.ServiceAccountCredentials.class,AssetStorageFirebaseProperties.class);
        ctor.setAccessible(true);
        var raw=(AssetStorageClient)ctor.newInstance(sdk,null,new AssetStorageFirebaseProperties(false,"synthetic","synthetic","unused",P4LocalComparisonLedger.PREFIX,null,null,null));
        var instrument=P4LocalComparisonStorage.class.getDeclaredMethod("instrumentConflict",AssetStorageClient.class);instrument.setAccessible(true);
        try(var ledger=new P4LocalComparisonLedger(directory.resolve("sdk-ledger"),Clock.systemUTC())) {
            for(int i=0;i<=2;i++)ledger.phase(i);
            var wrapper=new P4LocalComparisonStorage(raw,ledger);
            var tracked=new P4LocalComparisonStorage((AssetStorageClient)instrument.invoke(wrapper,raw),ledger);
            tracked.createImmutable(PATH,"image/png",new byte[10]);
            tracked.createImmutable(PATH,"image/png",new byte[10]);
            verify(sdk,times(1)).get(any(com.google.cloud.storage.BlobId.class));
            assertThat(Files.readString(directory.resolve("sdk-ledger"))).contains("OBSERVE_RESERVED 1");
        }
        reset(sdk);
        when(sdk.create(any(com.google.cloud.storage.BlobInfo.class),any(byte[].class),any(com.google.cloud.storage.Storage.BlobTargetOption[].class)))
                .thenThrow(new com.google.cloud.storage.StorageException(412,"exists"));
        try(var ledger=new P4LocalComparisonLedger(directory.resolve("unexpected-ledger"),Clock.systemUTC())) {
            ledger.phase(0);var wrapper=new P4LocalComparisonStorage(raw,ledger);
            var tracked=new P4LocalComparisonStorage((AssetStorageClient)instrument.invoke(wrapper,raw),ledger);
            assertThatThrownBy(()->tracked.createImmutable(PATH,"image/png",new byte[10])).hasMessage("UNEXPECTED_CONFLICT");
            verify(sdk,never()).get(any(com.google.cloud.storage.BlobId.class));
        }
    }
    @Test void repeatedCommandsAndExcludedIdsAreRejected() throws Exception {
        try(var ledger=new P4LocalComparisonLedger(directory.resolve("ledger"),Clock.systemUTC())) {
            for(long id:new long[]{71,93,112})assertThatThrownBy(()->ledger.scope(id)).hasMessage("FIXTURE_SCOPE");
            ledger.once("PUBLISH_ONCE");assertThatThrownBy(()->ledger.once("PUBLISH_ONCE")).hasMessage("ACTION_REPLAY");
        }
    }
}
