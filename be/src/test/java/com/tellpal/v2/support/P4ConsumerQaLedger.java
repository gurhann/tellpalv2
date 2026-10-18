package com.tellpal.v2.support;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.util.*;
import java.util.function.LongSupplier;
import com.fasterxml.jackson.databind.*;
import com.tellpal.v2.asset.domain.reading.ReadingIdentity;
import com.tellpal.v2.asset.infrastructure.storage.VersionedStorageObject;

/** Son terminal tüketimi taşıyan tek kullanımlık, çağrı öncesi fsync makbuzu. */
final class P4ConsumerQaLedger implements AutoCloseable {
    static final String PROPOSAL_SHA="079bfbe37c238bfcfc5cc3bc84f5ef420f5f7577b05b868dcd5622f73e267bf6";
    static final String TERMINAL_SHA="96dee3fb9e251a7438103f851dec78a09372d858e284e4967440a60de61c5fd1";
    static final String READY_SHA="5cd354c3210b3a6af017bc807287c3d6f80e6ed81bcae19946da1730dd1d44b7";
    static final String RUN=P4LocalComparisonLedger.RUN;
    static final String UID="jLRypTmoBrhhX21HsDJojurcKTC3";
    static final int[] ORDER={1,2,3,1};
    private final FileChannel channel;
    private final Clock clock;
    private final LongSupplier nanos;
    private final JsonNode expected;
    private long wallStart, monoStart, lastWall, lastMono;
    private int reads, auth, media, manifests, statuses;
    private long readBytes;
    private boolean opened, failed, closed;

    P4ConsumerQaLedger(Path path,byte[] proposal,byte[] terminal,byte[] ready,JsonNode approval,
            Clock clock,LongSupplier nanos,boolean loginReserved) throws IOException {
        require(ReadingIdentity.sha(proposal).equals(PROPOSAL_SHA),"PROPOSAL_CHANGED");
        require(terminal.length==11068 && ReadingIdentity.sha(terminal).equals(TERMINAL_SHA),"TERMINAL_CHANGED");
        require(ReadingIdentity.sha(ready).equals(READY_SHA),"READY_CHANGED");
        validateApproval(approval);
        this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);
        this.expected=new ObjectMapper().readTree(ready);
        channel=FileChannel.open(path,Set.of(StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try {
            append("OPEN_PENDING "+PROPOSAL_SHA+" "+TERMINAL_SHA+" 11068");
            append("PRIOR_DEBT 14 74711347 13 68374490 47 243641782 8 20 2 69 91 25 31572086");
            if(loginReserved){auth=1;append("LOGIN_RESERVED 3 1 1 1 21");}
        } catch(RuntimeException failure){channel.close();throw failure;}
    }

    static void validateApproval(JsonNode approval) {
        var keys=new HashSet<String>();approval.fieldNames().forEachRemaining(keys::add);
        require(keys.equals(Set.of("proposalSha256","decision","runId","qaUid","contentId","windowMillis","newProductionAllowed"))
                && approval.path("decision").asText().equals("A")
                && approval.path("proposalSha256").asText().equals(PROPOSAL_SHA)
                && approval.path("runId").asText().equals(RUN) && approval.path("qaUid").asText().equals(UID)
                && exact(approval.path("contentId"),94) && exact(approval.path("windowMillis"),180000)
                && approval.path("newProductionAllowed").isBoolean() && !approval.path("newProductionAllowed").booleanValue(),"APPROVAL_REQUIRED");
    }

    static boolean exact(JsonNode value,long number) {
        return value.isIntegralNumber() && value.bigIntegerValue().equals(java.math.BigInteger.valueOf(number));
    }

    synchronized void openWindow(Path window) throws IOException {
        require(!opened && !failed && !closed,"WINDOW_ALREADY_USED");
        wallStart=lastWall=clock.millis();monoStart=lastMono=nanos.getAsLong();
        try {
            P4LocalComparisonSetup.privateJson(window,Map.of("proposalSha256",PROPOSAL_SHA,"runId",RUN,
                    "startedAtEpochMillis",wallStart,"deadlineEpochMillis",Math.addExact(wallStart,180000),"windowMillis",180000));
            append("WINDOW_STARTED "+wallStart+" "+Math.addExact(wallStart,180000));opened=true;
        } catch(Exception failure){failed=true;if(failure instanceof IOException io)throw io;throw new IllegalStateException("WINDOW_FAILED",failure);}
    }

    synchronized void request(String method,Object[] args) {
        active();
        switch(method) {
            case "manifest" -> {require(manifests==0 && exactId(args[1],94) && args[2].equals("tr"),"MANIFEST_SCOPE_OR_REPLAY");manifests++;}
            case "status" -> {identity(args[1]);require(statuses<18,"STATUS_BUDGET");statuses++;}
            case "read" -> {
                identity(args[1]);require(media<4,"MEDIA_BUDGET");
                var selection=(com.tellpal.v2.asset.api.ReadingImageDeliveryApi.Selection)args[2];
                var entry=expected.path("descriptor").path("images").get(ORDER[media]);
                require(!selection.canonicalSource() && selection.role().name().equals(entry.path("source").path("binding").path("role").asText())
                        && selection.pageNumber()==entry.path("source").path("binding").path("pageNumber").intValue()
                        && selection.renditionKey().equals(entry.path("renditions").get(0).path("renditionKey").asText()),"MEDIA_SCOPE_OR_ORDER");
                media++;
            }
            default -> throw new IllegalStateException("HTTP_FORBIDDEN");
        }
        require(manifests+statuses+media<=23,"HTTP_BUDGET");
        append("HTTP_RESERVED "+method+" "+manifests+" "+statuses+" "+media);
    }

    private boolean exactId(Object value,long id){return value instanceof Long number && number==id;}

    private void identity(Object value) {
        try {var json=new ObjectMapper();require(json.readTree(json.writeValueAsBytes(value)).equals(expected.path("identity")),"LIVE_IDENTITY_CHANGED");}
        catch(IOException failure){throw new IllegalStateException("WIRE_FAILED",failure);}
    }

    synchronized void read(VersionedStorageObject object) {
        active();require(reads<4 && reads<media,"READ_BUDGET");
        var raster=expected.path("descriptor").path("images").get(ORDER[reads]).path("renditions").get(0).path("raster");
        require(object.path().equals(raster.path("objectKey").asText()) && object.version().equals(raster.path("generation").asText())
                && object.mime().equals(raster.path("mime").asText()) && exact(raster.path("byteSize"),object.bytes())
                && object.bytes()==3292721 && readBytes+object.bytes()<=13170884,"PINNED_SCOPE_OR_BYTES");
        reads++;readBytes+=object.bytes();append("READ_RESERVED "+(47+reads)+" "+(243641782+readBytes));
    }

    synchronized void importLoginReservation() {
        require(!opened && !failed && !closed && auth==0,"LOGIN_RESERVATION_REPLAY");
        auth=1;append("LOGIN_RESERVED 3 1 1 1 21");
    }

    synchronized void verify() {
        active();require(auth<28,"AUTH_BUDGET");auth++;append("AUTH_RESERVED "+(20+auth));
    }

    synchronized long remainingMillis() {
        if(!opened || failed || closed)return 0;
        long wall=clock.millis(),mono=nanos.getAsLong();
        if(wall<lastWall || mono<lastMono){failed=true;return 0;}
        lastWall=wall;lastMono=mono;
        return Math.max(0,Math.min(180000-(wall-wallStart),180000-(mono-monoStart)/1000000));
    }

    private void active(){require(remainingMillis()>0,"PENDING_EXPIRED_CLOSED_OR_CLOCK_ROLLBACK");}
    synchronized long started(){active();return wallStart;}
    synchronized long deadline(){active();return Math.addExact(wallStart,180000);}

    private void append(String line) {
        require(!failed && !closed,"LEDGER_CLOSED_OR_FAILED");
        try {
            var data=ByteBuffer.wrap((line+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            while(data.hasRemaining())channel.write(data);channel.force(true);
        } catch(IOException failure){failed=true;throw new IllegalStateException("LEDGER_WRITE_FAILED",failure);}
    }
    static void require(boolean value,String code){if(!value)throw new IllegalStateException(code);}
    @Override public synchronized void close() throws IOException {
        if(closed)return;
        try {if(!failed)append("CLOSED "+reads+" "+readBytes+" "+auth+" "+manifests+" "+statuses+" "+media);}
        finally {closed=true;channel.close();}
    }
}
