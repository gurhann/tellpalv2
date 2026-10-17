package com.tellpal.v2.support;

import java.io.PrintStream;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role;
import com.tellpal.v2.asset.domain.reading.ReadingProfiles;
import com.tellpal.v2.content.api.ReadingImageComparisonApi;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/** Yalnız mevcut #94 LIVE'ını eski bütçeyle sunar; yeni pencere ayrı açık onay ister. */
public final class P4ExistingComparisonSetup {
    private static final ObjectMapper JSON=new ObjectMapper();
    private P4ExistingComparisonSetup() { }
    public static void main(String[] args) {System.exit(run(args,System.out));}
    static int run(String[] args,PrintStream output) {
        if(args.length==0 || Arrays.equals(args,new String[]{"--dry-run"})) {
            output.println("DRY_RUN_ZERO_FILE_CREDENTIAL_DB_NETWORK_IO_EXISTING_ONLY");return 0;
        }
        if(args.length!=2 || !args[0].equals("--execute")) {output.println("REFUSED_ARGUMENTS");return 2;}
        try {
            Path child=Path.of(args[1]).toAbsolutePath().normalize();
            boolean approvedWindow=child.getFileName().toString().equals("mobile-window-180");
            String childName=approvedWindow?"mobile-window-180":"mobile-continuation";
            P4LocalComparisonSetup.require(child.equals(Path.of("var",P4LocalComparisonLedger.RUN,childName).toAbsolutePath().normalize())
                    && child.toRealPath().equals(child)
                    && child.getFileName().toString().equals(childName)
                    && child.getParent().getFileName().toString().equals(P4LocalComparisonLedger.RUN)
                    && Files.getPosixFilePermissions(child).equals(PosixFilePermissions.fromString("rwx------")),"CONTINUATION_DIRECTORY");
            var approval=JSON.readTree(Files.readAllBytes(child.resolve("approval.json")));
            validateApproval(approval,approvedWindow);
            var inputs=JSON.readTree(Files.readAllBytes(child.resolve("inputs.json")));
            var plan=P4LocalComparisonSetup.validateInputs(inputs);
            credentialGate(child,inputs,approvedWindow);
            byte[] prior=Files.readAllBytes(child.resolve("prior-ready.json"));
            P4LocalComparisonSetup.require(Arrays.equals(prior,Files.readAllBytes(child.getParent().resolve("ready.json"))),"PRIOR_READY_CHANGED");
            var expected=JSON.readTree(prior);
            P4LocalComparisonSetup.require(exactNumber(expected.path("contentId"),94)
                    && expected.path("runId").asText().equals(P4LocalComparisonLedger.RUN)
                    && expected.path("planSha256").asText().equals(P4LocalComparisonLedger.PLAN_SHA),"PRIOR_SCOPE_CHANGED");
            try(var ledger=approvedWindow
                    ?P4LocalComparisonLedger.resumeApprovedWindow(child.getParent().resolve("ledger.log"),child.resolve("ledger.log"),Clock.systemUTC())
                    :P4LocalComparisonLedger.resumeMedia(child.getParent().resolve("ledger.log"),child.resolve("ledger.log"),Clock.systemUTC());
                var context=P4LocalComparisonSetup.context(child,inputs,plan,ledger,94,false)) {
                var jdbc=context.getBean(JdbcTemplate.class);
                P4LocalComparisonSetup.require(jdbc.queryForObject("select current_database()",String.class).equals(plan.path("database").path("name").asText())
                        && jdbc.queryForObject("select max(version::integer) from flyway_schema_history where success",Integer.class)==37,"CONTINUATION_CLONE_REQUIRED");
                var profile=P4LocalComparisonSetup.profile(plan);
                for(var role:Role.values())P4LocalComparisonSetup.require(ReadingProfiles.capture(role,profile).fingerprint().equals(plan.path("runtimePreflight").path("roleFingerprints").path(role.name()).asText()),"RUNTIME_FINGERPRINT_CHANGED");
                P4LocalComparisonSetup.isolation(context,94);
                var admitted=context.getBean(ReadingImageComparisonApi.class).manifest(94,"tr");
                prepareReady(child,inputs,expected,admitted,ledger,approvedWindow,
                        ((WebServerApplicationContext)context).getWebServer().getPort(),ProcessHandle.current().pid());
                while(ledger.remainingMillis()>1000 && !Files.exists(child.resolve("close.request")))Thread.sleep(250);
                if(ledger.remainingMillis()>0)ledger.record("CLOSED","MEDIA_CONTINUATION_RELEASED");
            }
            return 0;
        } catch(Exception failure) {output.println("REFUSED_EXISTING_MEDIA_CONTINUATION_NO_RETRY_NO_EXTENSION");return 2;}
    }
    /** Gerçek giriş ve test aynı LIVE/credential kapılarından sonra tek pencereyi açar. */
    static void prepareReady(Path child,JsonNode inputs,JsonNode expected,ReadingImageComparisonApi.Admission admitted,
            P4LocalComparisonLedger ledger,boolean approvedWindow,int port,long pid) throws Exception {
        var identity=JSON.readTree(JSON.writeValueAsBytes(admitted.identity()));
        var descriptor=JSON.readTree(JSON.writeValueAsBytes(admitted.descriptor()));
        P4LocalComparisonSetup.require(identity.equals(expected.path("identity")) && descriptor.equals(expected.path("descriptor")),"EXISTING_LIVE_CHANGED");
        var result=new TreeMap<String,Object>();result.put("runId",P4LocalComparisonLedger.RUN);result.put("planSha256",P4LocalComparisonLedger.PLAN_SHA);
        result.put("contentId",94);result.put("identity",admitted.identity());result.put("descriptor",admitted.descriptor());
        result.put("loopbackPort",port);result.put("pid",pid);
        if(approvedWindow) {
            credentialGate(child,inputs,true);
            ledger.openApprovedWindow();
            result.put("windowStartedAtEpochMillis",ledger.windowStartedAtEpochMillis());
            result.put("windowDeadlineEpochMillis",ledger.windowDeadlineEpochMillis());
        }
        ledger.record("MEDIA_CONTINUATION_READY","94");
        P4LocalComparisonSetup.privateJson(child.resolve("ready.json"),result);
    }
    static boolean exactNumber(JsonNode value,long expected) {
        return value.isIntegralNumber() && value.bigIntegerValue().equals(java.math.BigInteger.valueOf(expected));
    }
    static void credentialGate(Path child,JsonNode inputs,boolean approvedWindow) throws Exception {
        P4LocalComparisonSetup.credentialGate(child,inputs);
        if(approvedWindow) {
            var ready=JSON.readTree(Files.readAllBytes(child.resolve("qa-sign-in.ready.json")));
            P4LocalComparisonSetup.require(exactNumber(ready.path("qaSignInInvocationsTotal"),2)
                    && exactNumber(ready.path("firebaseVerifierReservationsTotal"),5),"ADDITIONAL_LOGIN_RESERVATION_REQUIRED");
        }
    }
    static void validateApproval(JsonNode approval,boolean approvedWindow) {
        var expected=new HashSet<>(Set.of("runId","planSha256","contentId","newProductionAllowed","priorLedgerSha256"));
        if(approvedWindow)expected.addAll(Set.of("windowMillis","additionalLoginVerifications"));
        var keys=new HashSet<String>();approval.fieldNames().forEachRemaining(keys::add);
        P4LocalComparisonSetup.require(keys.equals(expected)
                && approval.path("runId").asText().equals(P4LocalComparisonLedger.RUN)
                && approval.path("planSha256").asText().equals(P4LocalComparisonLedger.PLAN_SHA)
                && exactNumber(approval.path("contentId"),94)
                && approval.path("newProductionAllowed").isBoolean() && !approval.path("newProductionAllowed").asBoolean()
                && approval.path("priorLedgerSha256").asText().equals(P4LocalComparisonLedger.CONTINUATION_SHA),"CONTINUATION_APPROVAL");
        if(approvedWindow)P4LocalComparisonSetup.require(exactNumber(approval.path("windowMillis"),180000)
                && exactNumber(approval.path("additionalLoginVerifications"),1),"APPROVED_WINDOW_LIMITS");
    }

}
