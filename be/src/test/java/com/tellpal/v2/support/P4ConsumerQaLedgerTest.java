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
class P4ConsumerQaLedgerTest {
    @TempDir Path directory;
    static final ObjectMapper JSON=new ObjectMapper();
    static byte[] fixture(String name) throws Exception {
        try(var input=P4ConsumerQaLedgerTest.class.getResourceAsStream("/p4/consumer-"+name)){return input.readAllBytes();}
    }
    static ObjectNode approval(){return JSON.valueToTree(Map.of("proposalSha256",P4ConsumerQaLedger.PROPOSAL_SHA,"decision","A",
            "runId",P4ConsumerQaLedger.RUN,"qaUid",P4ConsumerQaLedger.UID,"contentId",94,"windowMillis",180000,"newProductionAllowed",false));}
    static ReadingImageComparisonApi.Identity identity() throws Exception {return JSON.treeToValue(JSON.readTree(fixture("retained-ready.json")).path("identity"),ReadingImageComparisonApi.Identity.class);}
    static ReadingImageDeliveryApi.Selection selection(int order) throws Exception {
        var image=JSON.readTree(fixture("retained-ready.json")).path("descriptor").path("images").get(P4ConsumerQaLedger.ORDER[order]);
        return new ReadingImageDeliveryApi.Selection(Role.valueOf(image.path("source").path("binding").path("role").asText()),
                image.path("source").path("binding").path("pageNumber").intValue(),image.path("renditions").get(0).path("renditionKey").asText(),false);
    }
    static VersionedStorageObject object(int order) throws Exception {
        var image=JSON.readTree(fixture("retained-ready.json")).path("descriptor").path("images").get(P4ConsumerQaLedger.ORDER[order]).path("renditions").get(0).path("raster");
        return new VersionedStorageObject(image.path("objectKey").asText(),image.path("generation").asText(),image.path("mime").asText(),image.path("byteSize").longValue());
    }
    P4ConsumerQaLedger ledger(Clock clock,AtomicLong nanos) throws Exception {
        return new P4ConsumerQaLedger(directory.resolve("ledger"),fixture("proposal.json"),fixture("terminal-ledger.txt"),fixture("retained-ready.json"),approval(),clock,nanos::get,false);
    }
    @Test void pendingDoesNotAuthorizeAndFourPinnedReservationsCarryFullPriorDebt() throws Exception {
        var clock=Clock.fixed(java.time.Instant.ofEpochMilli(2000000000000L),java.time.ZoneOffset.UTC);
        try(var ledger=ledger(clock,new AtomicLong())) {
            assertThatThrownBy(ledger::verify).hasMessage("PENDING_EXPIRED_CLOSED_OR_CLOCK_ROLLBACK");
            ledger.importLoginReservation();ledger.openWindow(directory.resolve("window"));
            ledger.request("manifest",new Object[]{"synthetic",94L,"tr"});
            assertThatThrownBy(()->ledger.request("manifest",new Object[]{"synthetic",94L,"tr"})).hasMessage("MANIFEST_SCOPE_OR_REPLAY");
            for(int i=0;i<4;i++){ledger.request("read",new Object[]{"synthetic",identity(),selection(i)});ledger.read(object(i));}
            assertThatThrownBy(()->ledger.read(object(0))).hasMessage("READ_BUDGET");
            for(int i=0;i<27;i++)ledger.verify();
            assertThatThrownBy(ledger::verify).hasMessage("AUTH_BUDGET");
            assertThat(Files.readString(directory.resolve("ledger"))).contains("PRIOR_DEBT 14 74711347 13 68374490 47 243641782 8 20 2 69 91 25 31572086","READ_RESERVED 51 256812666","AUTH_RESERVED 48");
            assertThatThrownBy(()->ledger.openWindow(directory.resolve("other"))).hasMessage("WINDOW_ALREADY_USED");
        }
        assertThatThrownBy(()->ledger(clock,new AtomicLong())).isInstanceOf(FileAlreadyExistsException.class);
    }
    @Test void badShaApprovalWrongIntegerTypeAndOverflowRefuseBeforeCreatingLedger() throws Exception {
        var clock=Clock.systemUTC();
        for(var wrong:List.of(approval().put("windowMillis","180000"),approval().put("contentId",4294967390L),approval().put("newProductionAllowed",true),approval().put("extra",true)))
            assertThatThrownBy(()->new P4ConsumerQaLedger(directory.resolve("ledger"),fixture("proposal.json"),fixture("terminal-ledger.txt"),fixture("retained-ready.json"),wrong,clock,()->0,false)).hasMessage("APPROVAL_REQUIRED");
        var changed=fixture("terminal-ledger.txt");changed[0]++;
        assertThatThrownBy(()->new P4ConsumerQaLedger(directory.resolve("ledger"),fixture("proposal.json"),changed,fixture("retained-ready.json"),approval(),clock,()->0,false)).hasMessage("TERMINAL_CHANGED");
        assertThat(directory.resolve("ledger")).doesNotExist();
    }
    @Test void rollbackAndEitherExactDeadlineCloseIo() throws Exception {
        var clock=mock(Clock.class);when(clock.millis()).thenReturn(2000000000000L);var nanos=new AtomicLong();
        try(var ledger=ledger(clock,nanos)){
            ledger.openWindow(directory.resolve("window"));
            when(clock.millis()).thenReturn(2000000001000L);nanos.set(1000000000L);ledger.verify();
            when(clock.millis()).thenReturn(2000000000999L);
            assertThatThrownBy(ledger::verify).hasMessage("PENDING_EXPIRED_CLOSED_OR_CLOCK_ROLLBACK");
        }
    }
    @Test void monotonicDeadlineClosesEvenWhenWallHasNotExpired() throws Exception {
        var nanos=new AtomicLong();
        try(var ledger=ledger(Clock.systemUTC(),nanos)){
            ledger.openWindow(directory.resolve("window"));nanos.set(180000000000L);
            assertThat(ledger.remainingMillis()).isZero();
            assertThatThrownBy(ledger::verify).hasMessage("PENDING_EXPIRED_CLOSED_OR_CLOCK_ROLLBACK");
        }
    }
    @Test void wrongOrderGenerationAndCanonicalNeverSpendAStorageCall() throws Exception {
        try(var ledger=ledger(Clock.systemUTC(),new AtomicLong())){
            ledger.openWindow(directory.resolve("window"));
            assertThatThrownBy(()->ledger.request("read",new Object[]{"synthetic",identity(),selection(1)})).hasMessage("MEDIA_SCOPE_OR_ORDER");
            ledger.request("read",new Object[]{"synthetic",identity(),selection(0)});
            var object=object(0);
            assertThatThrownBy(()->ledger.read(new VersionedStorageObject(object.path(),"wrong",object.mime(),object.bytes()))).hasMessage("PINNED_SCOPE_OR_BYTES");
            ledger.read(object);
        }
    }
}
