package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role;
import com.tellpal.v2.asset.infrastructure.storage.VersionedStorageObject;
import com.tellpal.v2.content.api.ReadingImageComparisonApi;

/** Gerçek eski makbuz fixture'ı ile reset, sıralama ve iki saat kapılarını sınar. */
class P4b5ConsumerQaLedgerTest {
    static final String TERMINAL="""
            OPEN_PENDING 079bfbe37c238bfcfc5cc3bc84f5ef420f5f7577b05b868dcd5622f73e267bf6 96dee3fb9e251a7438103f851dec78a09372d858e284e4967440a60de61c5fd1 11068
            PRIOR_DEBT 14 74711347 13 68374490 47 243641782 8 20 2 69 91 25 31572086
            LOGIN_RESERVED 3 1 1 1 21
            WINDOW_STARTED 1792343619415 1792343799415
            HTTP_RESERVED manifest 1 0 0
            AUTH_RESERVED 22
            HTTP_RESERVED read 1 0 1
            AUTH_RESERVED 23
            READ_RESERVED 48 246934503
            AUTH_RESERVED 24
            HTTP_RESERVED read 1 0 2
            AUTH_RESERVED 25
            READ_RESERVED 49 250227224
            AUTH_RESERVED 26
            HTTP_RESERVED read 1 0 3
            AUTH_RESERVED 27
            READ_RESERVED 50 253519945
            HTTP_RESERVED status 1 1 3
            AUTH_RESERVED 28
            AUTH_RESERVED 29
            HTTP_RESERVED read 1 1 4
            AUTH_RESERVED 30
            READ_RESERVED 51 256812666
            AUTH_RESERVED 31
            CLOSED 4 13170884 11 1 1 4
            """;
    @TempDir Path directory;
    static final ObjectMapper JSON=new ObjectMapper();
    static byte[] fixture(String name) throws Exception {
        if(name.equals("proposal.json"))return Files.readAllBytes(Path.of("docs/evidence/p4b5-consumer-measurement/bounded-run-proposal-2026-10-19.json"));
        if(name.equals("terminal-ledger.txt"))return TERMINAL.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try(var input=P4b5ConsumerQaLedgerTest.class.getResourceAsStream("/p4/consumer-"+name)){return input.readAllBytes();}
    }
    static ObjectNode approval(){return JSON.valueToTree(Map.of("proposalSha256",P4b5ConsumerQaLedger.PROPOSAL_SHA,"decision","A",
            "budgetRunId",P4b5ConsumerQaLedger.RUN,"measurementId",P4b5ConsumerQaLedger.MEASUREMENT,"qaUid",P4b5ConsumerQaLedger.UID,"contentId",94,"windowMillis",120000,"newProductionAllowed",false));}
    static byte[] proposal(P4b5ConsumerQaScope scope) throws Exception {return Files.readAllBytes(Path.of("..").resolve(scope.proposal));}
    static byte[] priorProof() throws Exception {return priorProof(P4b5ConsumerQaScope.P4B5_RETRY);}
    static byte[] priorProof(P4b5ConsumerQaScope scope) throws Exception {
        var proposal=JSON.readTree(proposal(scope));
        var proof=JSON.valueToTree(scope.identity());
        for(String key:List.of(scope==P4b5ConsumerQaScope.P4B5_MANUAL?"previousAttemptsEvidence":"previousAttemptEvidence","historicalEvidence","priorConsumption"))((ObjectNode)proof).set(key,proposal.path(key));
        ((ObjectNode)proof).put("previousAttemptVerifiedByFileHashes",true).put("currentDatabaseQueried",false).put("oldBackendLedgerExists",false).put("actualStagesExecuted",false);
        return JSON.writeValueAsBytes(proof);
    }
    static ObjectNode approval(P4b5ConsumerQaScope scope){var value=approval();value.put("proposalSha256",scope.proposalSha256).put("measurementId",scope.measurementId);return value;}
    static P4b5ConsumerQaLedger ledger(P4b5ConsumerQaScope scope,Path path) throws Exception {
        return new P4b5ConsumerQaLedger(scope,path,proposal(scope),fixture("terminal-ledger.txt"),fixture("retained-ready.json"),approval(scope),Clock.systemUTC(),()->0,false,
                scope!=P4b5ConsumerQaScope.P4B5 ? priorProof(scope) : null);
    }
    static ReadingImageComparisonApi.Identity identity() throws Exception {return JSON.treeToValue(JSON.readTree(fixture("retained-ready.json")).path("identity"),ReadingImageComparisonApi.Identity.class);}
    static ReadingImageDeliveryApi.Selection selection(int order) throws Exception {
        var image=JSON.readTree(fixture("retained-ready.json")).path("descriptor").path("images").get(P4b5ConsumerQaLedger.ORDER[order]);
        return new ReadingImageDeliveryApi.Selection(Role.valueOf(image.path("source").path("binding").path("role").asText()),
                image.path("source").path("binding").path("pageNumber").intValue(),image.path("renditions").get(0).path("renditionKey").asText(),false);
    }
    static VersionedStorageObject object(int order) throws Exception {
        var image=JSON.readTree(fixture("retained-ready.json")).path("descriptor").path("images").get(P4b5ConsumerQaLedger.ORDER[order]).path("renditions").get(0).path("raster");
        return new VersionedStorageObject(image.path("objectKey").asText(),image.path("generation").asText(),image.path("mime").asText(),image.path("byteSize").longValue());
    }
    P4b5ConsumerQaLedger ledger(Clock clock,AtomicLong nanos) throws Exception {
        return new P4b5ConsumerQaLedger(directory.resolve("ledger"),fixture("proposal.json"),fixture("terminal-ledger.txt"),fixture("retained-ready.json"),approval(),clock,nanos::get,false);
    }
    static void media(P4b5ConsumerQaLedger ledger,int index) throws Exception {
        ledger.request("read",new Object[]{"synthetic",identity(),selection(index)});ledger.verify();ledger.read(object(index));ledger.verify();
    }
    @Test void fullQuotaPreservesSeed39AndFsyncsFourPinnedReadsAnd22Verifiers() throws Exception {
        try(var ledger=ledger(Clock.systemUTC(),new AtomicLong())) {
            ledger.importLoginReservation();ledger.openWindow(directory.resolve("window"));
            ledger.request("manifest",new Object[]{"synthetic",94L,"tr"});ledger.verify();
            for(int i=0;i<4;i++)media(ledger,i);
            for(int i=0;i<12;i++){ledger.request("status",new Object[]{"synthetic",identity()});ledger.verify();}
            assertThat(ledger.deliveryOperations()).isEqualTo(25);assertThat(ledger.reservedBytes()).isEqualTo(13170884);
            var quota=P4b5ConsumerQaLedger.class.getDeclaredMethod("reserveDelivery");quota.setAccessible(true);
            assertThatThrownBy(()->quota.invoke(ledger)).isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                    .hasCauseInstanceOf(IllegalStateException.class).cause().hasMessage("ADDITIONAL_DELIVERY_QUOTA");
            String text=Files.readString(directory.resolve("ledger"));
            assertThat(text).contains(" 39 44742970\n","DELIVERY_RESERVED 25 64\n","READ_RESERVED 55 269983550\n","AUTH_RESERVED 53\n");
            assertThatThrownBy(()->ledger.request("status",new Object[]{"synthetic",identity()})).hasMessage("STATUS_BUDGET");
            assertThatThrownBy(()->ledger.read(object(0))).hasMessage("READ_BUDGET_OR_AUTH_ORDER");
            assertThatThrownBy(ledger::verify).hasMessage("AUTH_BUDGET_OR_REQUEST_ORDER");
        }
        assertThat(Files.readString(directory.resolve("ledger"))).endsWith("CLOSED 4 13170884 22 1 12 4\n");
        assertThatThrownBy(()->ledger(Clock.systemUTC(),new AtomicLong())).isInstanceOf(FileAlreadyExistsException.class);
    }
    @Test void exactApprovalAndAllInputProofHashesRefuseBeforeFileCreation() throws Exception {
        for(var wrong:List.of(approval().put("windowMillis","120000"),approval().put("contentId",true),approval().put("measurementId",P4b5ConsumerQaLedger.RUN),approval().put("extra",true)))
            assertThatThrownBy(()->new P4b5ConsumerQaLedger(directory.resolve("ledger"),fixture("proposal.json"),fixture("terminal-ledger.txt"),fixture("retained-ready.json"),wrong,Clock.systemUTC(),()->0,false)).hasMessage("APPROVAL_REQUIRED");
        for(String field:List.of("proposal.json","terminal-ledger.txt","retained-ready.json")) {
            byte[] proposal=fixture("proposal.json"),terminal=fixture("terminal-ledger.txt"),ready=fixture("retained-ready.json");
            (field.equals("proposal.json")?proposal:field.equals("terminal-ledger.txt")?terminal:ready)[0]++;
            assertThatThrownBy(()->new P4b5ConsumerQaLedger(directory.resolve("ledger"),proposal,terminal,ready,approval(),Clock.systemUTC(),()->0,false)).isInstanceOf(IllegalStateException.class);
        }
        assertThat(directory.resolve("ledger")).doesNotExist();
    }
    @Test void pendingRollbackDeadlineReplayAndAuthReadOrderAreClosed() throws Exception {
        var clock=mock(Clock.class);when(clock.millis()).thenReturn(2000000000000L);var nanos=new AtomicLong();
        try(var ledger=ledger(clock,nanos)) {
            assertThatThrownBy(ledger::verify).hasMessage("PENDING_EXPIRED_CLOSED_OR_CLOCK_ROLLBACK");
            ledger.importLoginReservation();ledger.openWindow(directory.resolve("window"));
            assertThatThrownBy(ledger::verify).hasMessage("AUTH_BUDGET_OR_REQUEST_ORDER");
            ledger.request("manifest",new Object[]{"synthetic",94L,"tr"});ledger.verify();
            assertThatThrownBy(()->ledger.request("manifest",new Object[]{"synthetic",94L,"tr"})).hasMessage("MANIFEST_SCOPE_OR_REPLAY");
            ledger.request("read",new Object[]{"synthetic",identity(),selection(0)});
            assertThatThrownBy(()->ledger.read(object(0))).hasMessage("READ_BUDGET_OR_AUTH_ORDER");
            ledger.verify();ledger.read(object(0));ledger.verify();
            when(clock.millis()).thenReturn(2000000001000L);nanos.set(1000000000L);assertThat(ledger.remainingMillis()).isPositive();
            when(clock.millis()).thenReturn(2000000000999L);assertThat(ledger.remainingMillis()).isZero();
            assertThatThrownBy(ledger::verify).hasMessage("PENDING_EXPIRED_CLOSED_OR_CLOCK_ROLLBACK");
        }
    }
    @Test void exactMonotonicDeadlineStopsWhileWallHasTimeRemaining() throws Exception {
        var nanos=new AtomicLong();try(var ledger=ledger(Clock.systemUTC(),nanos)) {
            ledger.importLoginReservation();ledger.openWindow(directory.resolve("window"));nanos.set(120000000000L);
            assertThat(ledger.remainingMillis()).isZero();assertThatThrownBy(ledger::verify).isInstanceOf(IllegalStateException.class);
        }
    }
}
