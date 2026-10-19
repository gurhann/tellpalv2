package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.*;
import java.nio.file.*;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.storage.*;
import com.tellpal.v2.user.application.*;
import com.tellpal.v2.content.api.ReadingImageComparisonApi;

/** Gerçek Spring proxy, verifier ve SDK sarmasını sıralı olarak çalıştırır. */
class P4b5ConsumerQaCompositionTest {
    @TempDir Path directory;
    @Test void springServiceReservesBeforeAuthAndReadFailureRemainsTerminal() throws Exception {
        var raw=mock(AssetStorageClient.class);when(raw.provider()).thenReturn(StorageProvider.FIREBASE_STORAGE);
        when(raw.withReadingDeadline(anyLong())).thenReturn(raw);
        var verifier=mock(FirebaseTokenVerifier.class);var api=mock(ReadingImageComparisonApi.class);
        var auth=new com.tellpal.v2.user.infrastructure.firebase.FirebaseAuthenticationProperties(false,"synthetic",null,true);
        try(var ledger=new P4b5ConsumerQaLedger(directory.resolve("ledger"),P4b5ConsumerQaLedgerTest.fixture("proposal.json"),P4b5ConsumerQaLedgerTest.fixture("terminal-ledger.txt"),
                P4b5ConsumerQaLedgerTest.fixture("retained-ready.json"),P4b5ConsumerQaLedgerTest.approval(),Clock.systemUTC(),()->0,false);
            var context=new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("local");P4b5ConsumerQaSetup.boundedInitializer(ledger).initialize(context);
            context.registerBean("storage",AssetStorageClient.class,()->raw);context.registerBean("verifier",FirebaseTokenVerifier.class,()->verifier);
            context.registerBean(ReadingImageComparisonTestService.class,()->new ReadingImageComparisonTestService(context.getBean(FirebaseTokenVerifier.class),auth,api));context.refresh();
            when(verifier.verify("synthetic")).thenAnswer(call->{assertThat(Files.readString(directory.resolve("ledger"))).contains("AUTH_RESERVED 33");return new VerifiedFirebaseToken(P4b5ConsumerQaLedger.UID);});
            ledger.importLoginReservation();ledger.openWindow(directory.resolve("window"));
            context.getBean(ReadingImageComparisonTestService.class).manifest("Bearer synthetic",94,"tr");
            verify(verifier).verify("synthetic");verify(api).manifest(94,"tr");
            ledger.request("read",new Object[]{"synthetic",P4b5ConsumerQaLedgerTest.identity(),P4b5ConsumerQaLedgerTest.selection(0)});ledger.verify();
            var object=P4b5ConsumerQaLedgerTest.object(0);
            when(raw.openVersion(object)).thenAnswer(call->{assertThat(Files.readString(directory.resolve("ledger"))).contains("READ_RESERVED 52 260105387");throw new IllegalStateException("SYNTHETIC_IO_FAILURE");});
            assertThatThrownBy(()->context.getBean(AssetStorageClient.class).withReadingDeadline(1000).openVersion(object)).hasMessage("SYNTHETIC_IO_FAILURE");
            assertThat(ledger.reservedBytes()).isEqualTo(3292721);assertThat(ledger.remainingMillis()).isZero();
            assertThatThrownBy(()->context.getBean(AssetStorageClient.class).observeVersion("anything")).hasMessage("CONSUMER_QA_STORAGE_FORBIDDEN");
            verify(raw,never()).observeVersion(any());assertThatThrownBy(ledger::verify).hasMessage("PENDING_EXPIRED_CLOSED_OR_CLOCK_ROLLBACK");
        }
    }
}
