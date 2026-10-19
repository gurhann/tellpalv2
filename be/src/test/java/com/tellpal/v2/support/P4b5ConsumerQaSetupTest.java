package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

/** Gerçek ortamı açmadan setup kimliği ve borç kapılarını sınar. */
class P4b5ConsumerQaSetupTest {
    @TempDir Path directory;
    @Test void exactSeed39AndFinal25DeltaNeverMutateDatabase() {
        var jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForObject("select current_user",String.class)).thenReturn("gurhankucuk");
        when(jdbc.queryForObject("select current_database()",String.class)).thenReturn("tellpal_p4a2_cb5fe81a7ada4cb5adce6ddde1381d09");
        when(jdbc.queryForObject("select max(version::integer) from flyway_schema_history where success",Integer.class)).thenReturn(37);
        String query="select operations,reserved_bytes,max_operations,max_bytes from reading_comparison_budgets where run_id=?";
        when(jdbc.queryForMap(query,P4b5ConsumerQaLedger.RUN)).thenReturn(Map.of("operations",39L,"reserved_bytes",44742970L,"max_operations",64L,"max_bytes",67108864L));
        P4b5ConsumerQaSetup.validateDatabase(jdbc);
        when(jdbc.queryForMap(query,P4b5ConsumerQaLedger.RUN)).thenReturn(Map.of("operations",64L,"reserved_bytes",57913854L,"max_operations",64L,"max_bytes",67108864L));
        P4b5ConsumerQaSetup.validateFinalDatabase(jdbc,25,13170884);
        assertThatThrownBy(()->P4b5ConsumerQaSetup.validateFinalDatabase(jdbc,24,13170884)).hasMessage("V37_RECONCILIATION_FAILED");
        assertThatThrownBy(()->P4b5ConsumerQaSetup.validateDatabase(jdbc)).hasMessage("V37_PRIOR_DEBT_CHANGED");
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void dryRunUsernameAndOriginalFiveMinuteGateAreExact() {
        var output=new ByteArrayOutputStream();assertThat(P4b5ConsumerQaSetup.run(new String[]{},new PrintStream(output))).isZero();
        assertThat(output.toString()).contains("ZERO_FILE_CREDENTIAL_DB_NETWORK_IO_P4B5");
        for(Object user:List.of("tellpal","",true,1))assertThatThrownBy(()->P4b5ConsumerQaSetup.localDbUsername(P4b5ConsumerQaLedgerTest.JSON.valueToTree(Map.of("localDbUsername",user)))).hasMessage("EXPLICIT_LOCAL_DB_USER_REQUIRED");
        assertThat(P4b5ConsumerQaSetup.localGateOpen(1,1,300000,299999000001L)).isTrue();
        assertThat(P4b5ConsumerQaSetup.localGateOpen(1,1,300001,299999000001L)).isFalse();
        assertThat(P4b5ConsumerQaSetup.localGateOpen(1,1,0,2)).isFalse();
        assertThatThrownBy(()->P4b5ConsumerQaSetup.privateDirectory(directory)).hasMessage("PRIVATE_DIRECTORY_REQUIRED");
    }
    @Test void localPreparationClockRollbackIsTerminalWithinOriginalWindow() {
        var gate=new P4b5ConsumerQaSetup.LocalGate(1000,1000);
        assertThat(gate.open(2000,2000)).isTrue();
        assertThat(gate.open(1999,3000)).isFalse();
        assertThat(gate.open(3000,4000)).isFalse();
    }
    @Test void sourceAndClasspathDriftRefuseBeforeAnyComposition() throws Exception {
        directory=directory.toRealPath();var source=directory.resolve("source.txt");Files.writeString(source,"synthetic");
        var sha=com.tellpal.v2.asset.domain.reading.ReadingIdentity.sha(Files.readAllBytes(source));
        var input=P4b5ConsumerQaLedgerTest.JSON.valueToTree(Map.of("sourceBindings",Map.of("source.txt",sha),"classpathBindings",Map.of(source.toString(),sha)));
        P4b5ConsumerQaSetup.validateBindings(directory,input);
        Files.writeString(source,"changed");assertThatThrownBy(()->P4b5ConsumerQaSetup.validateBindings(directory,input)).hasMessage("SOURCE_DRIFT");
    }
    static final class ReviewedFixture {
        final Path root,repo,classes,javaBinary;final com.fasterxml.jackson.databind.node.ObjectNode input,run,source,classpath,selected;
        List<Path> actualEntries;
        final com.fasterxml.jackson.databind.ObjectMapper json=P4b5ConsumerQaLedgerTest.JSON;
        ReviewedFixture(Path parent) throws Exception {
            Files.createDirectories(parent);root=parent.toRealPath();repo=Files.createDirectories(root.resolve("source"));
            Files.createDirectories(root.resolve("receipts"));Files.createDirectories(root.resolve("reviews"));
            classes=Files.createDirectories(root.resolve("classes"));actualEntries=List.of(classes);Files.writeString(classes.resolve("One.class"),"synthetic-class");
            javaBinary=root.resolve("java");Files.writeString(javaBinary,"synthetic-java25");
            Files.writeString(repo.resolve("one.java"),"synthetic-source");
            source=node();source.set("files",json.valueToTree(Map.of("one.java",sha(repo.resolve("one.java")))));write("receipts/source-binding-0001.private.json",source);
            classpath=node();classpath.set("entries",json.valueToTree(List.of(classes.toString())));classpath.set("files",json.valueToTree(Map.of(classes.resolve("One.class").toString(),sha(classes.resolve("One.class")))));classpath.put("javaBinarySha256",sha(javaBinary));write("receipts/classpath-binding-0001.private.json",classpath);
            var buildRoot=Files.createDirectories(root.resolve("android/builds/build-0001"));Files.writeString(buildRoot.resolve("consumer.apk"),"synthetic-apk");
            var build=node();build.put("apkSha256",sha(buildRoot.resolve("consumer.apk")));build.put("sourceBindingSha256",sha(root.resolve("receipts/source-binding-0001.private.json")));build.put("entry","test_driver/reading_p4b5_qa_entry.dart");build.put("package","com.tellpal.tellpal_mobile_poc.readingconsumerqa");build.put("mode","debug");write("android/builds/build-0001/build.private.json",build);
            selected=node();selected.put("buildName","build-0001");selected.set("apkSha256",build.path("apkSha256"));write("receipts/selected-build-0001.private.json",selected);
            run=node();for(String name:List.of("sourceBinding","classpathBinding","selectedBuild")) {
                String file=Map.of("sourceBinding","source-binding-0001.private.json","classpathBinding","classpath-binding-0001.private.json","selectedBuild","selected-build-0001.private.json").get(name);
                run.put(name+"File",file);run.put(name+"Sha256",sha(root.resolve("receipts").resolve(file)));
            }
            write("receipts/run-binding-0001.private.json",run);
            String runSha=sha(root.resolve("receipts/run-binding-0001.private.json"));
            var claim=node();claim.put("runBindingSha256",runSha);claim.put("state","RESERVED_NO_AUTOMATIC_RETRY");write("backend-start.claim.private.json",claim);
            for(int number=1;number<=3;number++) {
                var review=node();review.put("sourceBindingSha256",run.path("sourceBindingSha256").asText());review.put("runBindingSha256",runSha);review.put("status","PASS");review.put("unresolvedFindings",0);review.put("reviewerId","reviewer-"+number);
                write("reviews/pre-run-0001-0"+number+".private.json",review);
            }
            input=json.createObjectNode();input.put("runBindingFile","run-binding-0001.private.json");input.put("runBindingSha256",runSha);
            input.set("sourceBindingFile",run.path("sourceBindingFile"));input.set("sourceBindingSha256",run.path("sourceBindingSha256"));input.set("sourceBindings",source.path("files"));input.set("classpathEntries",classpath.path("entries"));input.set("classpathBindings",classpath.path("files"));
        }
        com.fasterxml.jackson.databind.node.ObjectNode node(){var n=json.createObjectNode();n.put("proposalSha256",P4b5ConsumerQaLedger.PROPOSAL_SHA);n.put("measurementId",P4b5ConsumerQaLedger.MEASUREMENT);n.put("budgetRunId",P4b5ConsumerQaLedger.RUN);return n;}
        String sha(Path path) throws Exception {return com.tellpal.v2.asset.domain.reading.ReadingIdentity.sha(Files.readAllBytes(path));}
        void write(String name,Object value) throws Exception {P4LocalComparisonSetup.privateJson(root.resolve(name),value);}
        void replace(String name,Object value) throws Exception {Files.write(root.resolve(name),json.writeValueAsBytes(value));}
        com.fasterxml.jackson.databind.JsonNode validate() throws Exception {return P4b5ConsumerQaSetup.validateReviewedRuntime(root,repo,input,actualEntries,javaBinary,1073741824L);}
    }

    @Test void reviewedRunSourceClasspathJavaApkAndThreePassesBindBeforeAnyRuntimeIo() throws Exception {
        var f=new ReviewedFixture(directory.resolve("valid"));var links=f.validate();
        assertThat(links.path("runBindingSha256").asText()).isEqualTo(f.input.path("runBindingSha256").asText());
        assertThat(links.path("apkSha256").asText()).isEqualTo(f.selected.path("apkSha256").asText());
        assertThatThrownBy(()->P4b5ConsumerQaSetup.validateReviewedRuntime(f.root,f.repo,f.input,List.of(f.classes),f.javaBinary,536870912L)).hasMessage("SERVING_HEAP_CHANGED");
    }

    @Test void matchingForgedClasspathInputAndAllApprovedChainDriftsAreRejected() throws Exception {
        for(String mutation:List.of("matching-input","run","source","source-receipt","missing-class","extra-class","java","claim","apk","review","cleanup")) {
            var f=new ReviewedFixture(directory.resolve(mutation));
            switch(mutation) {
                case "matching-input" -> {var other=Files.createDirectory(f.root.resolve("other"));f.actualEntries=List.of(other);Files.writeString(other.resolve("Other.class"),"other");f.input.set("classpathEntries",f.json.valueToTree(List.of(other.toString())));f.input.set("classpathBindings",f.json.valueToTree(Map.of(other.resolve("Other.class").toString(),f.sha(other.resolve("Other.class")))));}
                case "run" -> f.input.put("runBindingSha256","b".repeat(64));
                case "source" -> f.input.put("sourceBindingSha256","b".repeat(64));
                case "source-receipt" -> f.replace("receipts/source-binding-0001.private.json",f.source.put("measurementId","old-run"));
                case "missing-class" -> Files.delete(f.classes.resolve("One.class"));
                case "extra-class" -> Files.writeString(f.classes.resolve("Extra.class"),"unexpected");
                case "java" -> Files.writeString(f.javaBinary,"different-java");
                case "claim" -> f.replace("backend-start.claim.private.json",f.node().put("runBindingSha256","b".repeat(64)));
                case "apk" -> Files.writeString(f.root.resolve("android/builds/build-0001/consumer.apk"),"different-apk");
                case "review" -> f.replace("reviews/pre-run-0001-03.private.json",f.node().put("status","PASS").put("runBindingSha256","old-run"));
                case "cleanup" -> Files.writeString(f.root.resolve("cleanup.claim.private.json"),"closed");
                default -> throw new AssertionError(mutation);
            }
            assertThatThrownBy(f::validate).as(mutation).isInstanceOf(Exception.class);
        }
    }

    @Test void finalOutcomeRequiresClosedHealthyLedgerAndPostContextDatabaseReconciliation() throws Exception {
        Path state=directory.toRealPath();var jdbc=mock(JdbcTemplate.class);
        String query="select operations,reserved_bytes,max_operations,max_bytes from reading_comparison_budgets where run_id=?";
        when(jdbc.queryForMap(query,P4b5ConsumerQaLedger.RUN)).thenReturn(Map.of("operations",52L,"reserved_bytes",57913854L,"max_operations",64L,"max_bytes",67108864L));
        var ledger=P4b5ConsumerQaLedgerTest.ledger(P4b5ConsumerQaScope.P4B5,state.resolve("ledger.log"));
        ledger.importLoginReservation();ledger.openWindow(state.resolve("window"));ledger.request("manifest",new Object[]{"synthetic",94L,"tr"});ledger.verify();
        for(int i=0;i<4;i++)P4b5ConsumerQaLedgerTest.media(ledger,i);
        var links=P4b5ConsumerQaLedgerTest.JSON.createObjectNode();
        assertThatThrownBy(()->P4b5ConsumerQaSetup.successfulOutcome(state,jdbc,ledger,true,links)).hasMessage("BACKEND_NOT_SUCCESSFULLY_DRAINED");
        ledger.close();var result=P4b5ConsumerQaSetup.successfulOutcome(state,jdbc,ledger,true,links);
        assertThat(result).containsEntry("outcome","SUCCESS").containsEntry("drainVerified",true);
        when(jdbc.queryForMap(query,P4b5ConsumerQaLedger.RUN)).thenReturn(Map.of("operations",53L,"reserved_bytes",57913854L,"max_operations",64L,"max_bytes",67108864L));
        assertThatThrownBy(()->P4b5ConsumerQaSetup.successfulOutcome(state,jdbc,ledger,true,links)).hasMessage("V37_RECONCILIATION_FAILED");
        ledger.fail("SYNTHETIC_LATE_FAILURE");
        assertThatThrownBy(()->P4b5ConsumerQaSetup.successfulOutcome(state,jdbc,ledger,true,links)).hasMessage("BACKEND_NOT_SUCCESSFULLY_DRAINED");
    }

    final class CredentialFixture implements AutoCloseable {
        final Path state;final P4b5ConsumerQaLedger ledger;
        final com.fasterxml.jackson.databind.node.ObjectNode receipt,reservation,claims;
        CredentialFixture(String name) throws Exception {this(name,P4b5ConsumerQaScope.P4B5);}
        CredentialFixture(String name,P4b5ConsumerQaScope scope) throws Exception {
            state=Files.createDirectory(directory.resolve(name)).toRealPath();
            ledger=P4b5ConsumerQaLedgerTest.ledger(scope,state.resolve("ledger.log"));
            var json=P4b5ConsumerQaLedgerTest.JSON;
            reservation=json.valueToTree(Map.of("proposalSha256",scope.proposalSha256,"measurementId",scope.measurementId,"budgetRunId",P4b5ConsumerQaLedger.RUN,
                    "qaSignInInvocationsTotal",4,"firebaseVerifierReservationsTotal",32,"accountLookupReserved",1,"tokenExchangeReserved",1,"loginVerifierReserved",1));
            long now=System.currentTimeMillis()/1000;
            claims=json.valueToTree(Map.of("aud","tellpal-ee0dd","iss","https://securetoken.google.com/tellpal-ee0dd","sub",P4b5ConsumerQaLedger.UID,"user_id",P4b5ConsumerQaLedger.UID,"iat",now-10,"exp",now+600));
            receipt=reservation.deepCopy();receipt.put("qaUid",P4b5ConsumerQaLedger.UID);receipt.put("tokenFile",state.resolve("id-token.private").toString());receipt.put("expiresAtEpochMillis",(now+600)*1000);
            save();
        }
        void save() throws Exception {
            var json=P4b5ConsumerQaLedgerTest.JSON;
            for(var entry:Map.of("qa-sign-in.ready.json",receipt,"login.reservation.private.json",reservation).entrySet()) {
                Path file=state.resolve(entry.getKey());
                if(Files.exists(file))Files.write(file,json.writeValueAsBytes(entry.getValue()));
                else P4LocalComparisonSetup.privateJson(file,entry.getValue());
            }
            String payload=Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(claims));
            Path token=state.resolve("id-token.private");Files.writeString(token,"eyJhbGciOiJSUzI1NiJ9."+payload+".c3ludGhldGlj",StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
            Files.setPosixFilePermissions(token,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        }
        void open(java.util.List<String> sequence) throws Exception {
            P4b5ConsumerQaSetup.openCredentialWindow(state,ledger,new TreeMap<>(),()->sequence.add("DB_THEN_ADMISSION"),()->{sequence.add("LOCAL_GATE");return true;});
        }
        @Override public void close() throws Exception {ledger.close();}
    }

    @Test void actualCredentialWindowRejectsMissingWrongScopeReservationUidTokenAndExpiryBeforeLoginImport() throws Exception {
        for(var scope:P4b5ConsumerQaScope.values())for(String mode:List.of("missing-login","measurement","missing-reservation","reservation-measurement","reservation-total","reservation-missing","reservation-wrong","uid","missing-token","token-path","token-format","token-header","token-signature","token-aud","token-iss","token-sub","token-user","token-expired","token-exp-type","token-future","receipt-expired","receipt-exp-type","receipt-token-exp-drift")) {
            try(var fixture=new CredentialFixture(scope.name()+mode,scope)) {
                switch(mode) {
                    case "measurement" -> fixture.receipt.put("measurementId","wrong");
                    case "reservation-measurement" -> fixture.reservation.put("measurementId","wrong");
                    case "reservation-total" -> fixture.reservation.put("firebaseVerifierReservationsTotal",31);
                    case "reservation-missing" -> fixture.reservation.remove("accountLookupReserved");
                    case "reservation-wrong" -> fixture.reservation.put("tokenExchangeReserved",true);
                    case "uid" -> fixture.receipt.put("qaUid","wrong");
                    case "token-path" -> fixture.receipt.put("tokenFile",fixture.state.resolve("other-token").toString());
                    case "token-aud" -> fixture.claims.put("aud","wrong");
                    case "token-iss" -> fixture.claims.put("iss","wrong");
                    case "token-sub" -> fixture.claims.put("sub","wrong");
                    case "token-user" -> fixture.claims.put("user_id","wrong");
                    case "token-expired" -> fixture.claims.put("exp",1);
                    case "token-exp-type" -> fixture.claims.put("exp","9999999999");
                    case "token-future" -> fixture.claims.put("iat",9999999999L);
                    case "receipt-expired" -> fixture.receipt.put("expiresAtEpochMillis",1);
                    case "receipt-exp-type" -> fixture.receipt.put("expiresAtEpochMillis",true);
                    case "receipt-token-exp-drift" -> fixture.receipt.put("expiresAtEpochMillis",fixture.receipt.path("expiresAtEpochMillis").longValue()+1000);
                }
                fixture.save();
                if(mode.equals("missing-login"))Files.delete(fixture.state.resolve("qa-sign-in.ready.json"));
                if(mode.equals("missing-reservation"))Files.delete(fixture.state.resolve("login.reservation.private.json"));
                if(mode.equals("missing-token"))Files.delete(fixture.state.resolve("id-token.private"));
                if(mode.equals("token-format"))Files.writeString(fixture.state.resolve("id-token.private"),"synthetic-invalid-token");
                if(mode.equals("token-header"))Files.writeString(fixture.state.resolve("id-token.private"),"a."+Files.readString(fixture.state.resolve("id-token.private")).split("\\.")[1]+".c2ln");
                if(mode.equals("token-signature"))Files.writeString(fixture.state.resolve("id-token.private"),Files.readString(fixture.state.resolve("id-token.private")).replace(".c3ludGhldGlj",".a"));
                var sequence=new ArrayList<String>();
                assertThatThrownBy(()->fixture.open(sequence)).as(mode).isInstanceOf(Exception.class);
                assertThat(sequence).as(mode).isEmpty();
                assertThat(Files.exists(fixture.state.resolve("window.json"))).as(mode).isFalse();
                assertThat(Files.exists(fixture.state.resolve("ready.json"))).as(mode).isFalse();
                assertThat(Files.readString(fixture.state.resolve("ledger.log"))).as(mode).doesNotContain("LOGIN_RESERVED","WINDOW_STARTED");
            }
        }
    }

    @Test void actualCredentialWindowValidEvidenceImportsOnceThenPreservesDatabaseAdmissionAndLocalGateOrder() throws Exception {
        for(var scope:P4b5ConsumerQaScope.values())try(var fixture=new CredentialFixture(scope.name()+"valid-login",scope)) {
            var sequence=new ArrayList<String>();fixture.open(sequence);
            assertThat(sequence).containsExactly("DB_THEN_ADMISSION","LOCAL_GATE");
            assertThat(Files.readString(fixture.state.resolve("ledger.log"))).containsOnlyOnce("LOGIN_RESERVED 4 1 1 1 32").containsOnlyOnce("WINDOW_STARTED");
            assertThat(Files.exists(fixture.state.resolve("window.json"))).isTrue();
            assertThat(P4b5ConsumerQaLedgerTest.JSON.readTree(Files.readAllBytes(fixture.state.resolve("ready.json"))).path("windowDeadlineEpochMillis").longValue()-fixture.ledger.started()).isEqualTo(120000);
            byte[] window=Files.readAllBytes(fixture.state.resolve("window.json")),ready=Files.readAllBytes(fixture.state.resolve("ready.json"));
            assertThatThrownBy(()->fixture.open(sequence)).hasMessage("LOGIN_RESERVATION_REPLAY");
            assertThat(sequence).containsExactly("DB_THEN_ADMISSION","LOCAL_GATE");
            assertThat(Files.readAllBytes(fixture.state.resolve("window.json"))).isEqualTo(window);
            assertThat(Files.readAllBytes(fixture.state.resolve("ready.json"))).isEqualTo(ready);
        }
    }

    @Test void retryCredentialGateRejectsLegacyProposalAndMeasurementBeforeLoginImport() throws Exception {
        try(var fixture=new CredentialFixture("cross-scope",P4b5ConsumerQaScope.P4B5_RETRY)) {
            fixture.receipt.put("proposalSha256",P4b5ConsumerQaScope.P4B5.proposalSha256).put("measurementId",P4b5ConsumerQaScope.P4B5.measurementId);fixture.save();
            assertThatThrownBy(()->fixture.open(new ArrayList<>())).hasMessage("LOGIN_SCOPE_OR_RESERVATION");
            assertThat(Files.readString(fixture.state.resolve("ledger.log"))).doesNotContain("LOGIN_RESERVED","WINDOW_STARTED");
        }
    }

}
