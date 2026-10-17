package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.time.Clock;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageClient;
import com.tellpal.v2.asset.infrastructure.storage.VersionedStorageObject;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.user.application.FirebaseTokenVerifier;

/** Yeni saat fixture OPEN'ından bağımsızdır; süre ve önceki tüketim asla sıfırlanmaz. */
class P4ApprovedWindowLedgerTest {
    @TempDir Path directory;
    static final long START=2000000000000L;
    static final String EXISTING=P4LocalComparisonLedger.PREFIX+"/existing.png";
    final ObjectMapper json=new ObjectMapper();
    Path source() throws Exception {
        Path parent=directory.toRealPath().resolve(P4LocalComparisonLedger.RUN);
        Files.createDirectories(parent.resolve("mobile-window-180"));Files.createDirectories(parent.resolve("mobile-continuation"));
        Path source=parent.resolve("ledger.log");
        try(var input=getClass().getResourceAsStream("/p4/existing-comparison-ledger.txt")) {Files.write(source,input.readAllBytes());}
        return source;
    }
    Path child(Path source) {return source.resolveSibling("mobile-window-180").resolve("ledger.log");}
    Clock clock() {var clock=mock(Clock.class);when(clock.millis()).thenReturn(START);return clock;}

    @Test void gatesDoNotStartWindowAndIndependentExact180SecondBoundaryClosesIo() throws Exception {
        Path source=source(),child=child(source),window=child.resolveSibling("window.json");byte[] original=Files.readAllBytes(source);
        var clock=clock();
        assertThatThrownBy(()->P4LocalComparisonLedger.resumeMedia(source,source.resolveSibling("mobile-continuation").resolve("ledger.log"),clock)).hasMessage("RUN_WINDOW_OR_FAILED");
        try(var ledger=P4LocalComparisonLedger.resumeApprovedWindow(source,child,clock,()->0)) {
            assertThat(window).doesNotExist();assertThat(ledger.remainingMillis()).isZero();
            assertThatThrownBy(()->ledger.read(EXISTING,1)).hasMessage("RUN_WINDOW_OR_FAILED");
            assertThatThrownBy(ledger::verify).hasMessage("RUN_WINDOW_OR_FAILED");
            when(clock.millis()).thenReturn(START+300000);
            ledger.openApprovedWindow();
            var receipt=json.readTree(Files.readAllBytes(window));
            long expectedStart=START+300000;
            assertThat(receipt.path("startedAtEpochMillis").asLong()).isEqualTo(expectedStart);
            assertThat(receipt.path("deadlineEpochMillis").asLong()).isEqualTo(expectedStart+180000);
            assertThat(receipt.path("windowMillis").asLong()).isEqualTo(180000);
            assertThat(receipt.path("additionalLoginVerifications").asInt()).isEqualTo(1);
            assertThat(receipt.path("priorLedgerSha256").asText()).isEqualTo(P4LocalComparisonLedger.CONTINUATION_SHA);
            assertThat(ledger.windowStartedAtEpochMillis()).isEqualTo(expectedStart);
            assertThat(ledger.windowDeadlineEpochMillis()).isEqualTo(expectedStart+180000);
            assertThat(ledger.remainingMillis()).isEqualTo(180000);
            when(clock.millis()).thenReturn(expectedStart+179999);
            ledger.read(EXISTING,1);ledger.verify();
            when(clock.millis()).thenReturn(expectedStart+180000);
            assertThat(ledger.remainingMillis()).isZero();
            assertThatThrownBy(()->ledger.read(EXISTING,1)).hasMessage("RUN_WINDOW_OR_FAILED");
            assertThatThrownBy(ledger::verify).hasMessage("RUN_WINDOW_OR_FAILED");
            assertThatThrownBy(ledger::openApprovedWindow).hasMessage("APPROVED_WINDOW_ALREADY_USED");
        }
        assertThat(Files.readAllBytes(source)).isEqualTo(original);
        assertThat(Files.readAllBytes(child)).startsWith(original);
        assertThat(Files.readString(child)).contains("READ_RESERVED 42 215362418 ","AUTH_RESERVED 5\n","AUTH_RESERVED 6\n");
        assertThatThrownBy(()->P4LocalComparisonLedger.resumeApprovedWindow(source,child,clock,()->0)).isInstanceOf(FileAlreadyExistsException.class);
    }

    @Test void previousReadAndExtraLoginDebtsRemainInsideGlobalCeilings() throws Exception {
        Path source=source(),child=child(source);
        try(var ledger=P4LocalComparisonLedger.resumeApprovedWindow(source,child,clock(),()->0)) {
            ledger.openApprovedWindow();
            for(int i=0;i<23;i++)ledger.read(EXISTING,1);
            assertThatThrownBy(()->ledger.read(EXISTING,1)).hasMessage("READ_BUDGET");
            for(int i=0;i<59;i++)ledger.verify();
            assertThatThrownBy(ledger::verify).hasMessage("AUTH_BUDGET");
            assertThat(Files.readString(child)).contains("READ_RESERVED 64 215362440 ","AUTH_RESERVED 5\n","AUTH_RESERVED 64\n");
            assertThatThrownBy(()->ledger.create(EXISTING,1)).hasMessage("MEDIA_CONTINUATION_READ_AUTH_ONLY");
            assertThatThrownBy(()->ledger.observe(EXISTING)).hasMessage("MEDIA_CONTINUATION_READ_AUTH_ONLY");
            assertThatThrownBy(()->ledger.once("RUN_EXACT_JOB")).hasMessage("MEDIA_CONTINUATION_READ_AUTH_ONLY");
            assertThatThrownBy(()->ledger.scope(94)).hasMessage("MEDIA_CONTINUATION_READ_AUTH_ONLY");
            assertThatThrownBy(()->ledger.phase(5)).hasMessage("MEDIA_CONTINUATION_READ_AUTH_ONLY");
        }
    }

    @Test void pendingSpringWrappersRefuseReadAndAuthBeforeRawDelegates() throws Exception {
        Path source=source(),child=child(source);
        var rawStorage=mock(AssetStorageClient.class);var rawVerifier=mock(FirebaseTokenVerifier.class);
        when(rawStorage.provider()).thenReturn(StorageProvider.FIREBASE_STORAGE);
        try(var ledger=P4LocalComparisonLedger.resumeApprovedWindow(source,child,clock(),()->0);
            var context=new AnnotationConfigApplicationContext()) {
            P4LocalComparisonSetup.boundedInitializer(ledger).initialize(context);
            context.registerBean("firebaseAssetStorageClient",AssetStorageClient.class,()->rawStorage);
            context.registerBean("firebaseTokenVerifier",FirebaseTokenVerifier.class,()->rawVerifier);
            context.refresh();
            assertThat(context.getBean(AssetStorageClient.class)).isInstanceOf(P4LocalComparisonStorage.class);
            clearInvocations(rawStorage,rawVerifier);
            var object=new VersionedStorageObject(EXISTING,"1","image/png",1);
            assertThatThrownBy(()->context.getBean(AssetStorageClient.class).openVersion(object)).hasMessage("RUN_WINDOW_OR_FAILED");
            assertThatThrownBy(()->context.getBean(FirebaseTokenVerifier.class).verify("synthetic")).hasMessage("RUN_WINDOW_OR_FAILED");
            verifyNoInteractions(rawStorage,rawVerifier);
            assertThat(Files.readAllBytes(child)).isEqualTo(Files.readAllBytes(source));
            assertThat(child.resolveSibling("window.json")).doesNotExist();
        }
    }

    @Test void freshCredentialReceiptMustBindExactlySecondLoginAndFifthVerifierReservation() throws Exception {
        Path child=directory.resolve("credential");Files.createDirectory(child);
        ObjectNode ready=json.valueToTree(Map.of("runId",P4LocalComparisonLedger.RUN,"planSha256",P4LocalComparisonLedger.PLAN_SHA,
                "qaUid","jLRypTmoBrhhX21HsDJojurcKTC3","expiresAtEpochMillis",System.currentTimeMillis()+3600000,
                "qaSignInInvocationsTotal",2,"firebaseVerifierReservationsTotal",5));
        P4LocalComparisonSetup.privateJson(child.resolve("qa-sign-in.ready.json"),ready);
        ObjectNode inputs=json.createObjectNode().put("credentialReceiptSha256",com.tellpal.v2.asset.domain.reading.ReadingIdentity.sha(Files.readAllBytes(child.resolve("qa-sign-in.ready.json"))));
        P4ExistingComparisonSetup.credentialGate(child,inputs,true);
        for(ObjectNode wrong:new ObjectNode[]{ready.deepCopy().put("qaSignInInvocationsTotal",3),ready.deepCopy().put("firebaseVerifierReservationsTotal",4),ready.deepCopy().put("firebaseVerifierReservationsTotal","5"),
                ready.deepCopy().put("qaSignInInvocationsTotal",4294967298L),ready.deepCopy().put("firebaseVerifierReservationsTotal",4294967301L)}) {
            Files.write(child.resolve("qa-sign-in.ready.json"),json.writeValueAsBytes(wrong));
            inputs.put("credentialReceiptSha256",com.tellpal.v2.asset.domain.reading.ReadingIdentity.sha(Files.readAllBytes(child.resolve("qa-sign-in.ready.json"))));
            assertThatThrownBy(()->P4ExistingComparisonSetup.credentialGate(child,inputs,true)).hasMessage("ADDITIONAL_LOGIN_RESERVATION_REQUIRED");
        }
        Files.write(child.resolve("qa-sign-in.ready.json"),json.writeValueAsBytes(ready.deepCopy().put("expiresAtEpochMillis",new java.math.BigInteger("18446746073709551616"))));
        inputs.put("credentialReceiptSha256",com.tellpal.v2.asset.domain.reading.ReadingIdentity.sha(Files.readAllBytes(child.resolve("qa-sign-in.ready.json"))));
        assertThatThrownBy(()->P4ExistingComparisonSetup.credentialGate(child,inputs,true)).hasMessage("REAL_QA_CREDENTIAL_READY_REQUIRED");
    }

    @Test void exclusiveWindowFailureDoesNotReplaceReceiptOrAllowRestart() throws Exception {
        Path source=source(),child=child(source),window=child.resolveSibling("window.json");
        Files.writeString(window,"existing-exclusive-window",StandardOpenOption.CREATE_NEW);
        try(var ledger=P4LocalComparisonLedger.resumeApprovedWindow(source,child,clock(),()->0)) {
            assertThatThrownBy(ledger::openApprovedWindow).isInstanceOf(FileAlreadyExistsException.class);
            assertThatThrownBy(ledger::openApprovedWindow).hasMessage("APPROVED_WINDOW_ALREADY_USED");
            assertThatThrownBy(ledger::verify).hasMessage("RUN_WINDOW_OR_FAILED");
        }
        assertThat(Files.readString(window)).isEqualTo("existing-exclusive-window");
        assertThatThrownBy(()->P4LocalComparisonLedger.resumeApprovedWindow(source,child,clock(),()->0)).isInstanceOf(FileAlreadyExistsException.class);
    }

    @Test void monotonicDeadlineCannotBeExtendedByWallClockRollback() throws Exception {
        Path source=source(),child=child(source);var clock=clock();var nanos=new java.util.concurrent.atomic.AtomicLong(7000000000L);
        try(var ledger=P4LocalComparisonLedger.resumeApprovedWindow(source,child,clock,nanos::get)) {
            ledger.openApprovedWindow();
            when(clock.millis()).thenReturn(START+170000);nanos.addAndGet(170000000000L);
            assertThat(ledger.remainingMillis()).isEqualTo(10000);
            when(clock.millis()).thenReturn(START+1000);nanos.addAndGet(9999999999L);
            ledger.read(EXISTING,1);ledger.verify();
            nanos.incrementAndGet();
            assertThat(ledger.remainingMillis()).isZero();
            assertThatThrownBy(()->ledger.read(EXISTING,1)).hasMessage("RUN_WINDOW_OR_FAILED");
            assertThatThrownBy(ledger::verify).hasMessage("RUN_WINDOW_OR_FAILED");
        }
    }

    @Test void realReadyConsumerKeepsWindowClosedUntilLiveAndSecondCredentialGatePass() throws Exception {
        Path source=source(),childLedger=child(source),child=childLedger.getParent();
        var identity=new com.tellpal.v2.content.api.ReadingImageComparisonApi.Identity(94,"tr",1,"revision","digest",java.util.UUID.randomUUID());
        var descriptor=new com.tellpal.v2.asset.api.ReadingImageDeliveryApi.Descriptor(java.util.List.of(),Map.of());
        var admitted=new com.tellpal.v2.content.api.ReadingImageComparisonApi.Admission(identity,descriptor,180000);
        ObjectNode expected=json.createObjectNode().set("identity",json.readTree(json.writeValueAsBytes(identity)));
        expected.set("descriptor",json.readTree(json.writeValueAsBytes(descriptor)));
        ObjectNode ready=json.valueToTree(Map.of("runId",P4LocalComparisonLedger.RUN,"planSha256",P4LocalComparisonLedger.PLAN_SHA,
                "qaUid","jLRypTmoBrhhX21HsDJojurcKTC3","expiresAtEpochMillis",System.currentTimeMillis()+3600000,
                "qaSignInInvocationsTotal",2,"firebaseVerifierReservationsTotal",5));
        P4LocalComparisonSetup.privateJson(child.resolve("qa-sign-in.ready.json"),ready);
        ObjectNode inputs=json.createObjectNode().put("credentialReceiptSha256",com.tellpal.v2.asset.domain.reading.ReadingIdentity.sha(Files.readAllBytes(child.resolve("qa-sign-in.ready.json"))));
        P4ExistingComparisonSetup.credentialGate(child,inputs,true);
        try(var ledger=P4LocalComparisonLedger.resumeApprovedWindow(source,childLedger,clock(),()->0)) {
            ObjectNode changed=expected.deepCopy();changed.withObject("identity").put("revision","changed");
            assertThatThrownBy(()->P4ExistingComparisonSetup.prepareReady(child,inputs,changed,admitted,ledger,true,12345,456)).hasMessage("EXISTING_LIVE_CHANGED");
            assertThat(child.resolve("window.json")).doesNotExist();assertThat(child.resolve("ready.json")).doesNotExist();
            Files.write(child.resolve("qa-sign-in.ready.json"),json.writeValueAsBytes(ready.deepCopy().put("firebaseVerifierReservationsTotal",4)));
            assertThatThrownBy(()->P4ExistingComparisonSetup.prepareReady(child,inputs,expected,admitted,ledger,true,12345,456)).hasMessage("CREDENTIAL_RECEIPT_CHANGED");
            assertThat(child.resolve("window.json")).doesNotExist();assertThat(child.resolve("ready.json")).doesNotExist();
            Files.write(child.resolve("qa-sign-in.ready.json"),json.writeValueAsBytes(ready));
            inputs.put("credentialReceiptSha256",com.tellpal.v2.asset.domain.reading.ReadingIdentity.sha(Files.readAllBytes(child.resolve("qa-sign-in.ready.json"))));
            P4ExistingComparisonSetup.prepareReady(child,inputs,expected,admitted,ledger,true,12345,456);
            var window=json.readTree(Files.readAllBytes(child.resolve("window.json")));var result=json.readTree(Files.readAllBytes(child.resolve("ready.json")));
            assertThat(result.path("windowStartedAtEpochMillis")).isEqualTo(window.path("startedAtEpochMillis"));
            assertThat(result.path("windowDeadlineEpochMillis")).isEqualTo(window.path("deadlineEpochMillis"));
            assertThat(result.path("identity")).isEqualTo(expected.path("identity"));assertThat(result.path("descriptor")).isEqualTo(expected.path("descriptor"));
            assertThat(result.path("loopbackPort").asInt()).isEqualTo(12345);assertThat(result.path("pid").asLong()).isEqualTo(456);
        }
    }

    @Test void approvedModeRequiresExactWindowAndSingleAdditionalLoginWithoutProduction() {
        ObjectNode approval=json.valueToTree(Map.of("runId",P4LocalComparisonLedger.RUN,"planSha256",P4LocalComparisonLedger.PLAN_SHA,
                "contentId",94,"newProductionAllowed",false,"priorLedgerSha256",P4LocalComparisonLedger.CONTINUATION_SHA,
                "windowMillis",180000,"additionalLoginVerifications",1));
        P4ExistingComparisonSetup.validateApproval(approval,true);
        assertThatThrownBy(()->P4ExistingComparisonSetup.validateApproval(approval,false)).hasMessage("CONTINUATION_APPROVAL");
        for(ObjectNode wrong:new ObjectNode[]{approval.deepCopy().put("windowMillis",180001),approval.deepCopy().put("additionalLoginVerifications",2),approval.deepCopy().put("additionalLoginVerifications","1")})
            assertThatThrownBy(()->P4ExistingComparisonSetup.validateApproval(wrong,true)).hasMessage("APPROVED_WINDOW_LIMITS");
        for(ObjectNode wrong:new ObjectNode[]{approval.deepCopy().put("newProductionAllowed",true),approval.deepCopy().put("contentId",95),approval.deepCopy().put("unapproved",true)})
            assertThatThrownBy(()->P4ExistingComparisonSetup.validateApproval(wrong,true)).hasMessage("CONTINUATION_APPROVAL");
        assertThatThrownBy(()->P4ExistingComparisonSetup.validateApproval(approval.deepCopy().put("additionalLoginVerifications",4294967297L),true)).hasMessage("APPROVED_WINDOW_LIMITS");
        assertThatThrownBy(()->P4ExistingComparisonSetup.validateApproval(approval.deepCopy().put("windowMillis",new java.math.BigInteger("18446744073709731616")),true)).hasMessage("APPROVED_WINDOW_LIMITS");
        assertThatThrownBy(()->P4ExistingComparisonSetup.validateApproval(approval.deepCopy().put("contentId",new java.math.BigInteger("18446744073709551710")),true)).hasMessage("CONTINUATION_APPROVAL");
    }
}
