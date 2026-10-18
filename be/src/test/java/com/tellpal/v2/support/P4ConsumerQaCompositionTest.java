package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.storage.*;
import com.tellpal.v2.user.application.*;
import com.tellpal.v2.content.api.ReadingImageComparisonApi;

/** Gerçek Spring tüketicilerinde auth/HTTP/storage sarmalarının atlanamadığını sınar. */
class P4ConsumerQaCompositionTest {
    @TempDir Path directory;
    @Test void consumersReserveBeforeDelegatesAndDeadlineClonesShareBudget() throws Exception {
        var raw=mock(AssetStorageClient.class);var verifier=mock(FirebaseTokenVerifier.class);
        when(raw.provider()).thenReturn(StorageProvider.FIREBASE_STORAGE);when(raw.withReadingDeadline(anyLong())).thenReturn(raw);
        var api=mock(ReadingImageComparisonApi.class);
        var auth=new com.tellpal.v2.user.infrastructure.firebase.FirebaseAuthenticationProperties(false,"synthetic",null,true);
        try(var ledger=new P4ConsumerQaLedger(directory.resolve("ledger"),P4ConsumerQaLedgerTest.fixture("proposal.json"),P4ConsumerQaLedgerTest.fixture("terminal-ledger.txt"),
                P4ConsumerQaLedgerTest.fixture("retained-ready.json"),P4ConsumerQaLedgerTest.approval(),Clock.systemUTC(),()->0,false);
            var context=new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("local");
            P4ConsumerQaSetup.boundedInitializer(ledger).initialize(context);
            context.registerBean("storage",AssetStorageClient.class,()->raw);context.registerBean("verifier",FirebaseTokenVerifier.class,()->verifier);
            context.registerBean(ReadingImageComparisonTestService.class,()->new ReadingImageComparisonTestService(context.getBean(FirebaseTokenVerifier.class),auth,api));
            context.refresh();clearInvocations(raw,verifier);
            var storage=context.getBean(AssetStorageClient.class);var service=context.getBean(ReadingImageComparisonTestService.class);
            assertThatThrownBy(()->service.manifest("Bearer synthetic",94,"tr")).hasMessage("PENDING_EXPIRED_CLOSED_OR_CLOCK_ROLLBACK");
            verifyNoInteractions(verifier,api);
            ledger.openWindow(directory.resolve("window"));
            for(int i=0;i<4;i++) {
                ledger.request("read",new Object[]{"synthetic",P4ConsumerQaLedgerTest.identity(),P4ConsumerQaLedgerTest.selection(i)});
                var object=P4ConsumerQaLedgerTest.object(i);
                when(raw.openVersion(object)).thenAnswer(call->{assertThat(Files.readString(directory.resolve("ledger"))).contains("READ_RESERVED");return new ByteArrayInputStream(new byte[0]);});
                storage.withReadingDeadline(1000).openVersion(object).close();
            }
            assertThatThrownBy(()->storage.openVersion(P4ConsumerQaLedgerTest.object(0))).hasMessage("READ_BUDGET");
            verify(raw,times(4)).openVersion(any());
            assertThatThrownBy(()->storage.observeVersion("anything")).hasMessage("CONSUMER_QA_STORAGE_FORBIDDEN");
            assertThatThrownBy(()->storage.createImmutable("anything","image/png",new byte[1])).hasMessage("CONSUMER_QA_STORAGE_FORBIDDEN");
            assertThatThrownBy(()->storage.openObject("anything",null)).hasMessage("CONSUMER_QA_STORAGE_FORBIDDEN");
            verify(raw,never()).observeVersion(any());verify(raw,never()).createImmutable(any(),any(),any());
        }
    }
    @Test void realSpringServicePersistsAuthBeforeDelegateAndExpiredWindowNeverCallsIt() throws Exception {
        var clock=mock(Clock.class);when(clock.millis()).thenReturn(2000000000000L);
        var verifier=mock(FirebaseTokenVerifier.class);var api=mock(ReadingImageComparisonApi.class);
        var auth=new com.tellpal.v2.user.infrastructure.firebase.FirebaseAuthenticationProperties(false,"synthetic",null,true);
        try(var ledger=new P4ConsumerQaLedger(directory.resolve("auth-ledger"),P4ConsumerQaLedgerTest.fixture("proposal.json"),P4ConsumerQaLedgerTest.fixture("terminal-ledger.txt"),
                P4ConsumerQaLedgerTest.fixture("retained-ready.json"),P4ConsumerQaLedgerTest.approval(),clock,()->0,false);
            var context=new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("local");P4ConsumerQaSetup.boundedInitializer(ledger).initialize(context);
            context.registerBean("verifier",FirebaseTokenVerifier.class,()->verifier);
            context.registerBean(ReadingImageComparisonTestService.class,()->new ReadingImageComparisonTestService(context.getBean(FirebaseTokenVerifier.class),auth,api));
            context.refresh();
            var service=context.getBean(ReadingImageComparisonTestService.class);
            when(verifier.verify("synthetic")).thenAnswer(call->{
                assertThat(Files.readString(directory.resolve("auth-ledger"))).contains("AUTH_RESERVED 21\n");
                return new VerifiedFirebaseToken(P4ConsumerQaLedger.UID);
            });
            ledger.openWindow(directory.resolve("auth-window"));
            service.manifest("Bearer synthetic",94,"tr");
            verify(verifier).verify("synthetic");verify(api).manifest(94,"tr");
            when(clock.millis()).thenReturn(2000000180000L);
            assertThatThrownBy(()->service.status("Bearer synthetic",P4ConsumerQaLedgerTest.identity())).hasMessage("PENDING_EXPIRED_CLOSED_OR_CLOCK_ROLLBACK");
            assertThatThrownBy(()->context.getBean(FirebaseTokenVerifier.class).verify("synthetic")).hasMessage("PENDING_EXPIRED_CLOSED_OR_CLOCK_ROLLBACK");
            verify(verifier,times(1)).verify("synthetic");
        }
    }

    @Test void springVerifierBudgetIsSharedWithServiceAndCannotBypassFsync() throws Exception {
        var verifier=mock(FirebaseTokenVerifier.class);var api=mock(ReadingImageComparisonApi.class);
        var auth=new com.tellpal.v2.user.infrastructure.firebase.FirebaseAuthenticationProperties(false,"synthetic",null,true);
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        try(var ledger=new P4ConsumerQaLedger(directory.resolve("budget-ledger"),P4ConsumerQaLedgerTest.fixture("proposal.json"),P4ConsumerQaLedgerTest.fixture("terminal-ledger.txt"),
                P4ConsumerQaLedgerTest.fixture("retained-ready.json"),P4ConsumerQaLedgerTest.approval(),Clock.systemUTC(),()->0,true);
            var context=new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("local");P4ConsumerQaSetup.boundedInitializer(ledger).initialize(context);
            context.registerBean("verifier",FirebaseTokenVerifier.class,()->verifier);
            context.registerBean(ReadingImageComparisonTestService.class,()->new ReadingImageComparisonTestService(context.getBean(FirebaseTokenVerifier.class),auth,api));
            context.refresh();
            var service=context.getBean(ReadingImageComparisonTestService.class);
            var wrapped=context.getBean(FirebaseTokenVerifier.class);
            when(verifier.verify("synthetic")).thenAnswer(call->{
                int total=21+calls.incrementAndGet();
                assertThat(Files.readString(directory.resolve("budget-ledger"))).contains("AUTH_RESERVED "+total+"\n");
                return new VerifiedFirebaseToken(P4ConsumerQaLedger.UID);
            });
            ledger.openWindow(directory.resolve("budget-window"));
            // Gerçek hizmet yolu ilk rezervasyonu tüketir; Spring verifier aynı kalan payı paylaşır.
            service.manifest("Bearer synthetic",94,"tr");
            for(int i=0;i<26;i++)wrapped.verify("synthetic");
            assertThat(calls.get()).isEqualTo(27);
            assertThatThrownBy(()->service.status("Bearer synthetic",P4ConsumerQaLedgerTest.identity())).hasMessage("AUTH_BUDGET");
            assertThatThrownBy(()->wrapped.verify("synthetic")).hasMessage("AUTH_BUDGET");
            verify(verifier,times(27)).verify("synthetic");
            verify(api,never()).status(any());
        }
    }

}
