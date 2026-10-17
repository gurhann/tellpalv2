package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.storage.*;
import com.tellpal.v2.user.application.*;

/** Gerçek initializer ve DI tüketicileri; raw adapter'lar yalnız bellek fake'idir. */
class P4LocalComparisonCompositionTest {
    @TempDir Path directory;
    record AuthConsumer(FirebaseTokenVerifier verifier) {
        VerifiedFirebaseToken verify() {return verifier.verify("synthetic");}
    }
    @Test void actualSpringConsumersSharePersistentReservationsAndCannotReachRawAdaptersAfterCeiling() throws Exception {
        var raw=mock(AssetStorageClient.class);
        when(raw.provider()).thenReturn(StorageProvider.FIREBASE_STORAGE);
        when(raw.withReadingDeadline(anyLong())).thenReturn(raw);
        var verifier=mock(FirebaseTokenVerifier.class);
        when(verifier.verify("synthetic")).thenReturn(new VerifiedFirebaseToken("synthetic-uid"));
        Path receipt=directory.resolve("ledger");
        try(var ledger=new P4LocalComparisonLedger(receipt,Clock.systemUTC(),1);
            var context=new AnnotationConfigApplicationContext()) {
            P4LocalComparisonSetup.boundedInitializer(ledger).initialize(context);
            context.registerBean("firebaseAssetStorageClient",AssetStorageClient.class,()->raw);
            context.registerBean("firebaseTokenVerifier",FirebaseTokenVerifier.class,()->verifier);
            context.registerBean(AssetStorageClientRegistry.class,()->new AssetStorageClientRegistry(
                    List.copyOf(context.getBeansOfType(AssetStorageClient.class).values())));
            context.registerBean(ReadingObjectStorage.class);
            context.registerBean(AuthConsumer.class,()->new AuthConsumer(context.getBean(FirebaseTokenVerifier.class)));
            context.refresh();
            assertThat(context.getBean(AssetStorageClient.class)).isInstanceOf(P4LocalComparisonStorage.class);
            ledger.phase(0);
            var object=new VersionedStorageObject(P4LocalComparisonLedger.PREFIX+"/test.png","1","image/png",1);
            when(raw.openVersion(object)).thenAnswer(call->{
                assertThat(Files.readString(receipt)).contains("READ_RESERVED");
                return new ByteArrayInputStream(new byte[]{1});
            });
            var consumer=context.getBean(ReadingObjectStorage.class);
            for(int i=0;i<3;i++)assertThat(consumer.read(StorageProvider.FIREBASE_STORAGE,object,1,1000)).containsExactly((byte)1);
            assertThatThrownBy(()->consumer.read(StorageProvider.FIREBASE_STORAGE,object,1,1000)).hasMessage("READ_BUDGET");
            verify(raw,times(3)).openVersion(object);
            for(int i=1;i<=5;i++)ledger.phase(i);
            var auth=context.getBean(AuthConsumer.class);
            for(int i=0;i<63;i++)assertThat(auth.verify().firebaseUid()).isEqualTo("synthetic-uid");
            assertThatThrownBy(auth::verify).hasMessage("AUTH_BUDGET");
            verify(verifier,times(63)).verify("synthetic");
            assertThat(Files.readString(receipt)).contains("AUTH_RESERVED 64");
        }
    }
}
