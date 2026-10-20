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
final class P4b5ConsumerQaLedger implements AutoCloseable {
    static final String PROPOSAL_SHA="e49eb8b706143f09c4fbf1738e2254f54e491d104954baf31e546a4497ec5833";
    static final String TERMINAL_SHA="ba0920e32691d3f9bab292f7119a4d9a51e9f853e2fb79cbce03db5df6ad6250";
    static final String READY_SHA="5cd354c3210b3a6af017bc807287c3d6f80e6ed81bcae19946da1730dd1d44b7";
    static final String MEASUREMENT="p4b5-7981ad25-c851-4ed6-af89-2d0fbfffd0f8";
    static final String RUN=P4LocalComparisonLedger.RUN;
    static final String UID="jLRypTmoBrhhX21HsDJojurcKTC3";
    static final int[] ORDER={1,2,3,1};
    private final P4b5ConsumerQaScope scope;
    private final FileChannel channel;
    private final Clock clock;
    private final LongSupplier nanos;
    private final JsonNode expected;
    private long wallStart, monoStart, lastWall, lastMono;
    private int reads, auth, media, manifests, statuses, delivery, inFlight;
    private long readBytes;
    private boolean opened, failed, closed;

    P4b5ConsumerQaLedger(Path path,byte[] proposal,byte[] terminal,byte[] ready,JsonNode approval,
            Clock clock,LongSupplier nanos,boolean loginReserved) throws IOException {
        this(P4b5ConsumerQaScope.P4B5,path,proposal,terminal,ready,approval,clock,nanos,loginReserved,null);
    }

    P4b5ConsumerQaLedger(P4b5ConsumerQaScope scope,Path path,byte[] proposal,byte[] terminal,byte[] ready,JsonNode approval,
            Clock clock,LongSupplier nanos,boolean loginReserved,byte[] priorProof) throws IOException {
        this.scope=Objects.requireNonNull(scope);
        require(ReadingIdentity.sha(proposal).equals(scope.proposalSha256),"PROPOSAL_CHANGED");
        if(scope!=P4b5ConsumerQaScope.P4B5) {
            require(priorProof!=null,"PRIOR_ATTEMPT_PROOF_REQUIRED");
            try {scope.validatePriorProof(proposal,new ObjectMapper().readTree(priorProof));}
            catch(IOException failure){throw failure;}
            catch(Exception failure){throw new IllegalStateException("PRIOR_ATTEMPT_PROOF_CHANGED",failure);}
        }
        require(terminal.length==752 && ReadingIdentity.sha(terminal).equals(TERMINAL_SHA),"TERMINAL_CHANGED");
        require(ReadingIdentity.sha(ready).equals(READY_SHA),"READY_CHANGED");
        validateApproval(scope,approval);
        this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);
        this.expected=new ObjectMapper().readTree(ready);
        channel=FileChannel.open(path,Set.of(StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try {
            append("OPEN_PENDING "+scope.measurementId+" "+scope.proposalSha256+" "+TERMINAL_SHA+" 752");
            if(priorProof!=null)append("PRIOR_ATTEMPT "+ReadingIdentity.sha(priorProof));
            append("PRIOR_DEBT 14 74711347 13 68374490 51 256812666 8 31 3 73 107 39 44742970");
            if(loginReserved){auth=1;append("LOGIN_RESERVED 4 1 1 1 32");}
        } catch(RuntimeException failure){channel.close();throw failure;}
    }

    P4b5ConsumerQaScope scope(){return scope;}

    static void validateApproval(JsonNode approval) {validateApproval(P4b5ConsumerQaScope.P4B5,approval);}

    static void validateApproval(P4b5ConsumerQaScope scope,JsonNode approval) {
        var keys=new HashSet<String>();approval.fieldNames().forEachRemaining(keys::add);
        require(keys.equals(Set.of("proposalSha256","decision","budgetRunId","measurementId","qaUid","contentId","windowMillis","newProductionAllowed"))
                && approval.path("decision").asText().equals("A")
                && approval.path("proposalSha256").asText().equals(scope.proposalSha256)
                && approval.path("measurementId").asText().equals(scope.measurementId)
                && approval.path("budgetRunId").asText().equals(RUN) && approval.path("qaUid").asText().equals(UID)
                && exact(approval.path("contentId"),94) && exact(approval.path("windowMillis"),120000)
                && approval.path("newProductionAllowed").isBoolean() && !approval.path("newProductionAllowed").booleanValue(),"APPROVAL_REQUIRED");
    }

    static boolean exact(JsonNode value,long number) {
        return value.isIntegralNumber() && value.bigIntegerValue().equals(java.math.BigInteger.valueOf(number));
    }

    synchronized void openWindow(Path window) throws IOException {
        require(!opened && !failed && !closed,"WINDOW_ALREADY_USED");
        wallStart=lastWall=clock.millis();monoStart=lastMono=nanos.getAsLong();
        try {
            P4LocalComparisonSetup.privateJson(window,Map.of("proposalSha256",scope.proposalSha256,"budgetRunId",RUN,"measurementId",scope.measurementId,
                    "startedAtEpochMillis",wallStart,"deadlineEpochMillis",Math.addExact(wallStart,120000),"windowMillis",120000));
            append("WINDOW_STARTED "+wallStart+" "+Math.addExact(wallStart,120000));opened=true;
        } catch(Exception failure){failed=true;if(failure instanceof IOException io)throw io;throw new IllegalStateException("WINDOW_FAILED",failure);}
    }

    synchronized void request(String method,Object[] args) {
        active();
        switch(method) {
            case "manifest" -> {require(manifests==0 && exactId(args[1],94) && args[2].equals("tr"),"MANIFEST_SCOPE_OR_REPLAY");manifests++;}
            case "status" -> {identity(args[1]);require(statuses<12,"STATUS_BUDGET");statuses++;}
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
        require(manifests+statuses+media<=17,"HTTP_BUDGET");
        append("HTTP_RESERVED "+method+" "+manifests+" "+statuses+" "+media);
    }

    private boolean exactId(Object value,long id){return value instanceof Long number && number==id;}

    private void identity(Object value) {
        try {var json=new ObjectMapper();require(json.readTree(json.writeValueAsBytes(value)).equals(expected.path("identity")),"LIVE_IDENTITY_CHANGED");}
        catch(IOException failure){throw new IllegalStateException("WIRE_FAILED",failure);}
    }

    synchronized void read(VersionedStorageObject object) {
        active();require(reads<4 && reads<media && auth>=manifests+statuses+reads*2+2,"READ_BUDGET_OR_AUTH_ORDER");
        var raster=expected.path("descriptor").path("images").get(ORDER[reads]).path("renditions").get(0).path("raster");
        require(object.path().equals(raster.path("objectKey").asText()) && object.version().equals(raster.path("generation").asText())
                && object.mime().equals(raster.path("mime").asText()) && exact(raster.path("byteSize"),object.bytes())
                && object.bytes()==3292721 && readBytes+object.bytes()<=13170884,"PINNED_SCOPE_OR_BYTES");
        reserveDelivery();reads++;readBytes+=object.bytes();append("READ_RESERVED "+(51+reads)+" "+(256812666+readBytes));
    }

    synchronized void importLoginReservation() {
        require(!opened && !failed && !closed && auth==0,"LOGIN_RESERVATION_REPLAY");
        auth=1;append("LOGIN_RESERVED 4 1 1 1 32");
    }

    synchronized void verify() {
        active();require(auth<22 && auth < 1+manifests+statuses+media*2,"AUTH_BUDGET_OR_REQUEST_ORDER");reserveDelivery();auth++;append("AUTH_RESERVED "+(31+auth));
    }

    private void reserveDelivery() {
        require(delivery<25,"ADDITIONAL_DELIVERY_QUOTA");delivery++;
        append("DELIVERY_RESERVED "+delivery+" "+(39+delivery));
    }

    synchronized void fail(String code) {
        if(!failed && !closed)append("FAILED_NO_RETRY "+code);
        failed=true;
    }

    synchronized void enterRequest(String method,Object[] args){request(method,args);inFlight++;}
    synchronized void exitRequest(){require(inFlight>0,"REQUEST_DRAIN_UNDERFLOW");inFlight--;}
    synchronized boolean isClosed(){return closed;}
    synchronized boolean completeMeasurement(){return reads==4 && manifests==1 && media==4 && auth==1+manifests+statuses+2*media && delivery==auth-1+reads && readBytes==13170884;}
    synchronized int reads(){return reads;}
    synchronized int manifests(){return manifests;}
    synchronized int media(){return media;}
    synchronized String terminalLine(){return (failed?"FAILED_CLOSED ":"CLOSED ")+reads+" "+readBytes+" "+auth+" "+manifests+" "+statuses+" "+media;}
    synchronized boolean isFailed(){return failed;}
    synchronized int deliveryOperations(){return delivery;}
    synchronized long reservedBytes(){return readBytes;}

    synchronized long remainingMillis() {
        if(!opened || failed || closed)return 0;
        long wall=clock.millis(),mono=nanos.getAsLong();
        if(wall<lastWall || mono<lastMono){failed=true;return 0;}
        lastWall=wall;lastMono=mono;
        return Math.max(0,Math.min(120000-(wall-wallStart),120000-(mono-monoStart)/1000000));
    }

    private void active(){require(remainingMillis()>0,"PENDING_EXPIRED_CLOSED_OR_CLOCK_ROLLBACK");}
    synchronized long started(){active();return wallStart;}
    synchronized long deadline(){active();return Math.addExact(wallStart,120000);}

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
        try {
            if(inFlight!=0)failed=true;
            boolean terminalFailure=failed;failed=false;
            try {append((terminalFailure?"FAILED_CLOSED ":"CLOSED ")+reads+" "+readBytes+" "+auth+" "+manifests+" "+statuses+" "+media);}
            finally {failed=terminalFailure;}
        }
        finally {closed=true;channel.close();}
    }
}
