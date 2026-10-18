package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.*;
import java.util.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import com.tellpal.v2.content.api.ReadingImageComparisonApi;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi;

class P4ConsumerQaSetupTest {
    @TempDir java.nio.file.Path directory;

    @Test void realCredentialGateImportsLoginBeforeOpeningWindowAndPreservesFullVerifierCeiling() throws Exception {
        directory=directory.toRealPath();
        P4LocalComparisonSetup.privateJson(directory.resolve("login.reservation.private.json"),Map.of(
                "proposalSha256",P4ConsumerQaLedger.PROPOSAL_SHA,"priorLedgerSha256",P4ConsumerQaLedger.TERMINAL_SHA,
                "qaSignInInvocationsTotal",3,"firebaseVerifierReservationsTotal",21));
        P4LocalComparisonSetup.privateJson(directory.resolve("qa-sign-in.ready.json"),Map.of(
                "proposalSha256",P4ConsumerQaLedger.PROPOSAL_SHA,"runId",P4ConsumerQaLedger.RUN,"qaUid",P4ConsumerQaLedger.UID,
                "expiresAtEpochMillis",System.currentTimeMillis()+3600000L,"qaSignInInvocationsTotal",3,"firebaseVerifierReservationsTotal",21));
        var path=directory.resolve("credential-ledger.log");
        try(var ledger=new P4ConsumerQaLedger(path,P4ConsumerQaLedgerTest.fixture("proposal.json"),
                P4ConsumerQaLedgerTest.fixture("terminal-ledger.txt"),P4ConsumerQaLedgerTest.fixture("retained-ready.json"),
                P4ConsumerQaLedgerTest.approval(),java.time.Clock.systemUTC(),()->0,false)) {
            P4ConsumerQaSetup.credentialGate(directory,ledger);
            assertThat(java.nio.file.Files.readAllLines(path)).containsExactly(
                    "OPEN_PENDING "+P4ConsumerQaLedger.PROPOSAL_SHA+" "+P4ConsumerQaLedger.TERMINAL_SHA+" 11068",
                    "PRIOR_DEBT 14 74711347 13 68374490 47 243641782 8 20 2 69 91 25 31572086",
                    "LOGIN_RESERVED 3 1 1 1 21");
            ledger.openWindow(directory.resolve("credential-window.json"));
            ledger.verify();
            assertThat(java.nio.file.Files.readAllLines(path)).contains("AUTH_RESERVED 22").doesNotContain("AUTH_RESERVED 21");
            for(int i=0;i<26;i++)ledger.verify();
            byte[] beforeLimit=java.nio.file.Files.readAllBytes(path);
            assertThatThrownBy(ledger::verify).hasMessage("AUTH_BUDGET");
            assertThat(java.nio.file.Files.readAllBytes(path)).isEqualTo(beforeLimit);
            assertThat(java.nio.file.Files.readAllLines(path).stream().filter(line->line.startsWith("AUTH_RESERVED ")).toList())
                    .hasSize(27).startsWith("AUTH_RESERVED 22").endsWith("AUTH_RESERVED 48");
        }
        assertThat(java.nio.file.Files.readAllLines(path)).endsWith("CLOSED 0 0 28 0 0 0");
    }

    @Test void actualContextCallerCarriesUsernameIntoBootEnvironmentBeforeAnyBeanOrSdk() throws Exception {
        var config=directory.resolve("application-p4.properties");
        java.nio.file.Files.writeString(config,"""
                spring.datasource.username=tellpal
                server.address=127.0.0.1
                server.port=0
                tellpal.asset.processing.poller.enabled=false
                tellpal.asset.storage.firebase.project-id=synthetic
                tellpal.asset.storage.firebase.bucket-name=synthetic
                tellpal.asset.storage.firebase.credentials-path=/synthetic/not-read.json
                tellpal.security.firebase.project-id=synthetic
                tellpal.security.firebase.credentials-path=/synthetic/not-read.json
                tellpal.security.firebase.stub-tokens-enabled=false
                tellpal.security.firebase.check-revoked=true
                tellpal.asset.storage.firebase.fake-client-enabled=false
                tellpal.security.admin.bootstrap.username=
                tellpal.security.admin.bootstrap.password=
                """+"tellpal.asset.storage.firebase.path-prefix="+P4LocalComparisonLedger.PREFIX+"\n");
        var json=P4ConsumerQaLedgerTest.JSON;
        var inputs=json.valueToTree(Map.of("cloneUrl","jdbc:postgresql://127.0.0.1:1/synthetic",
                "sdk",Map.of("projectId","synthetic","bucket","synthetic","credentialsPath","/synthetic/not-read.json")));
        var captured=new java.util.concurrent.atomic.AtomicReference<String>();
        var commandLine=new java.util.concurrent.atomic.AtomicReference<Object>();
        var contextCreated=new java.util.concurrent.atomic.AtomicBoolean();
        org.springframework.boot.SpringApplicationHook hook=app->new org.springframework.boot.SpringApplicationRunListener() {
            @Override public void environmentPrepared(org.springframework.boot.bootstrap.ConfigurableBootstrapContext bootstrap,
                    org.springframework.core.env.ConfigurableEnvironment environment) {
                captured.set(environment.getProperty("spring.datasource.username"));
                commandLine.set(environment.getPropertySources().get("commandLineArgs").getProperty("spring.datasource.username"));
                throw new IllegalStateException("TEST_STOP_BEFORE_CONTEXT_DB_SDK");
            }
            @Override public void contextPrepared(org.springframework.context.ConfigurableApplicationContext context) {contextCreated.set(true);}
        };
        String previous=System.getProperty("spring.datasource.username");
        try {
            System.setProperty("spring.datasource.username","tellpal");
            assertThatThrownBy(()->org.springframework.boot.SpringApplication.withHook(hook,(Runnable)()->{
                P4ConsumerQaSetup.context(directory,directory,inputs,json.createObjectNode(),null,"gurhankucuk");
            })).hasMessage("TEST_STOP_BEFORE_CONTEXT_DB_SDK");
        } finally {
            if(previous==null)System.clearProperty("spring.datasource.username");else System.setProperty("spring.datasource.username",previous);
        }
        assertThat(captured.get()).isEqualTo("gurhankucuk");
        assertThat(commandLine.get()).isEqualTo("gurhankucuk");assertThat(contextCreated.get()).isFalse();
    }

    @Test void actualRunNeverWritesAnotherStartupOwnersFailureAndStillRecordsItsOwnFailure() throws Exception {
        for(String mode:List.of("own-first-failure","same-process-replay","foreign-process-claim")) {
            var state=java.nio.file.Files.createDirectory(directory.resolve(mode));
            var input=state.resolve("inputs.private.json");
            P4LocalComparisonSetup.privateJson(input,Map.of("localDbUsername","gurhankucuk"));
            byte[] reservation="synthetic-reservation".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            var claim=state.resolve("startup.claim.private.json");
            if(mode.equals("same-process-replay"))P4ConsumerQaSetup.claimStartup(state,"a".repeat(64),reservation);
            if(mode.equals("foreign-process-claim"))P4LocalComparisonSetup.privateJson(claim,Map.of("pid",999999,"owner","synthetic-other"));
            byte[] original=java.nio.file.Files.exists(claim)?java.nio.file.Files.readAllBytes(claim):null;
            java.nio.file.Files.writeString(state.resolve("ledger.log"),"ACTIVE_OTHER_LEDGER_UNCHANGED\n");
            var output=new PrintStream(new ByteArrayOutputStream());String[] args={"--execute",state.toString()};
            // Yalnız sabit özel yol/hash ön kapısı yalıtılır; gerçek run/catch ve CREATE_NEW claim çalışır.
            try(var methods=mockStatic(P4ConsumerQaSetup.class)) {
                methods.when(()->P4ConsumerQaSetup.run(args,output)).thenCallRealMethod();
                methods.when(()->P4ConsumerQaSetup.privateBytes(input,65536)).thenCallRealMethod();
                methods.when(()->P4ConsumerQaSetup.localDbUsername(any(com.fasterxml.jackson.databind.JsonNode.class))).thenCallRealMethod();
                methods.when(()->P4ConsumerQaSetup.claimStartup(state,"a".repeat(64),reservation)).thenCallRealMethod();
                methods.when(()->P4ConsumerQaSetup.recoveryGate(state)).thenAnswer(call->{
                    P4ConsumerQaSetup.claimStartup(state,"a".repeat(64),reservation);return null;
                });
                assertThat(P4ConsumerQaSetup.run(args,output)).isEqualTo(2);
                methods.verify(()->P4ConsumerQaSetup.context(any(),any(),any(),any(),any(),anyString()),never());
            }
            assertThat(java.nio.file.Files.exists(state.resolve("failure.json"))).isEqualTo(mode.equals("own-first-failure"));
            if(original!=null)assertThat(java.nio.file.Files.readAllBytes(claim)).isEqualTo(original);
            assertThat(java.nio.file.Files.readString(state.resolve("ledger.log"))).isEqualTo("ACTIVE_OTHER_LEDGER_UNCHANGED\n");
        }
    }

    @Test void jvmClaimIsExclusiveAndPreservesFirstClaimOnReplay() throws Exception {
        byte[] reservation=P4ConsumerQaLedgerTest.JSON.writeValueAsBytes(P4ConsumerQaSetup.startupReservation("a".repeat(64),"BACKEND_START"));
        P4ConsumerQaSetup.claimStartup(directory,"a".repeat(64),reservation);
        var path=directory.resolve("startup.claim.private.json");byte[] first=java.nio.file.Files.readAllBytes(path);
        var receipt=P4ConsumerQaLedgerTest.JSON.readTree(first);
        assertThat(receipt.path("startupReservationSha256").asText()).isEqualTo(com.tellpal.v2.asset.domain.reading.ReadingIdentity.sha(reservation));
        assertThat(receipt.path("pid").longValue()).isEqualTo(ProcessHandle.current().pid());
        assertThatThrownBy(()->P4ConsumerQaSetup.claimStartup(directory,"a".repeat(64),reservation)).isInstanceOf(java.nio.file.FileAlreadyExistsException.class);
        assertThat(java.nio.file.Files.readAllBytes(path)).isEqualTo(first);
        assertThatThrownBy(()->P4ConsumerQaSetup.privateDirectory(directory)).hasMessage("PRIVATE_DIRECTORY_REQUIRED");
    }
    @Test void explicitLocalUserIsRequiredBeforeAnySpringOrSdkComposition() throws Exception {
        var json=P4ConsumerQaLedgerTest.JSON;
        for(String value:List.of("{}","{\"localDbUsername\":\"\"}","{\"localDbUsername\":\"tellpal\"}",
                "{\"localDbUsername\":\" gurhankucuk\"}","{\"localDbUsername\":true}")) {
            assertThatThrownBy(()->P4ConsumerQaSetup.localDbUsername(json.readTree(value))).hasMessage("EXPLICIT_LOCAL_DB_USER_REQUIRED");
        }
        assertThat(P4ConsumerQaSetup.localDbUsername(json.readTree("{\"localDbUsername\":\"gurhankucuk\"}"))).isEqualTo("gurhankucuk");
        assertThatThrownBy(()->P4ConsumerQaSetup.context(null,null,null,null,null,"tellpal")).hasMessage("EXPLICIT_LOCAL_DB_USER_REQUIRED");
    }

    @Test void commandLineDatasourceUserOverridesApplicationDefaultAndEnvironment() {
        var environment=new org.springframework.core.env.StandardEnvironment();
        environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("oldDefaults",Map.of("spring.datasource.username","tellpal")));
        environment.getPropertySources().addFirst(new org.springframework.core.env.SimpleCommandLinePropertySource(
                P4ConsumerQaSetup.datasourceUsernameArgument("gurhankucuk")));
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("gurhankucuk");
        assertThatThrownBy(()->P4ConsumerQaSetup.datasourceUsernameArgument("tellpal")).hasMessage("EXPLICIT_LOCAL_DB_USER_REQUIRED");
    }

    @Test void recoveryAllowsOnlyClosedZeroSpendLedgerWithoutWindowOrExtraReservation() {
        String zero="OPEN_PENDING "+P4ConsumerQaLedger.PROPOSAL_SHA+" "+P4ConsumerQaLedger.TERMINAL_SHA+" 11068\n"
                +"PRIOR_DEBT 14 74711347 13 68374490 47 243641782 8 20 2 69 91 25 31572086\nCLOSED 0 0 0 0 0 0\n";
        P4ConsumerQaSetup.validateRecoveryLedger(zero.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        for(String changed:List.of(zero.replace("CLOSED 0","CLOSED 1"),zero+"WINDOW_STARTED 1 180001\n",
                zero.replace("CLOSED 0 0 0 0 0 0\n",""),zero.replace("31572086","0"),zero+"AUTH_RESERVED 21\n")) {
            assertThatThrownBy(()->P4ConsumerQaSetup.validateRecoveryLedger(changed.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    .hasMessage("RECOVERY_PRIOR_SPEND_OR_WINDOW");
        }
    }

    @Test void recoveryApprovalAndJvmReservationKeepExactIncidentAndSeparateStartupKinds() throws Exception {
        var approval=P4ConsumerQaSetup.recoveryApproval();String sha="a".repeat(64);
        var reservation=P4ConsumerQaSetup.startupReservation(sha,"BACKEND_START");
        assertThat(approval).isEqualTo(P4ConsumerQaLedgerTest.JSON.readTree(
                java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/test/resources/p4/consumer-recovery-approval.json"))));
        P4ConsumerQaSetup.validateRecoveryAuthorization(approval,reservation,sha);
        for(String key:List.of("decision","localDbUsername","evidenceHashes","apkSha256","additionalQaStarts",
                "qaStartsTotalCeiling","additionalBackendStarts","backendStartsTotalCeiling","measurementCeilingsChanged")) {
            ObjectNode changed=approval.deepCopy();changed.put(key,"changed");
            assertThatThrownBy(()->P4ConsumerQaSetup.validateRecoveryAuthorization(changed,reservation,sha))
                    .hasMessage("LOCAL_RUNTIME_RECOVERY_APPROVAL_REQUIRED");
        }
        assertThatThrownBy(()->P4ConsumerQaSetup.validateRecoveryAuthorization(approval,P4ConsumerQaSetup.startupReservation(sha,"QA_START"),sha))
                .hasMessage("RECOVERY_RESERVATION_CHANGED");
        assertThatThrownBy(()->P4ConsumerQaSetup.validateRecoveryAuthorization(approval,reservation,"b".repeat(64)))
                .hasMessage("RECOVERY_RESERVATION_CHANGED");
    }
    @Test void localGateKeepsItsOriginalFiveMinutesAndNeverStartsMediaWindow() {
        long wall=2000000000000L,mono=1000000000L;
        assertThat(P4ConsumerQaSetup.localGateOpen(wall,mono,wall,mono)).isTrue();
        assertThat(P4ConsumerQaSetup.localGateOpen(wall,mono,wall+299999,mono+299999000000L)).isTrue();
        assertThat(P4ConsumerQaSetup.localGateOpen(wall,mono,wall+300000,mono+299999000000L)).isFalse();
        assertThat(P4ConsumerQaSetup.localGateOpen(wall,mono,wall+1,mono+300000000000L)).isFalse();
        assertThat(P4ConsumerQaSetup.localGateOpen(wall,mono,wall-1,mono+1)).isFalse();
        assertThat(P4ConsumerQaSetup.localGateOpen(wall,mono,wall+1,mono-1)).isFalse();
    }
    @Test void dryRunHasNoSideEffectsAndOtherModesAreClosed() {
        var output=new ByteArrayOutputStream();
        assertThat(P4ConsumerQaSetup.run(new String[]{},new PrintStream(output))).isZero();
        assertThat(output.toString()).contains("ZERO_FILE_CREDENTIAL_DB_NETWORK_IO");
        assertThat(P4ConsumerQaSetup.run(new String[]{"--seed"},new PrintStream(output))).isEqualTo(2);
    }
    @Test void realWireIntegersDoNotMistakeLongContentIdForChangedLive() throws Exception {
        var json=P4ConsumerQaLedgerTest.JSON;var expected=json.readTree(P4ConsumerQaLedgerTest.fixture("retained-ready.json"));
        var admission=new ReadingImageComparisonApi.Admission(json.treeToValue(expected.path("identity"),ReadingImageComparisonApi.Identity.class),
                json.treeToValue(expected.path("descriptor"),ReadingImageDeliveryApi.Descriptor.class),180000);
        P4ConsumerQaSetup.validateAdmission(expected,admission);
        var changed=expected.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)changed.path("identity")).put("liveGeneration",5);
        assertThatThrownBy(()->P4ConsumerQaSetup.validateAdmission(changed,admission)).hasMessage("RETAINED_LIVE_CHANGED");
    }
    @Test void exactExistingV37RunDebtIsReadWithoutResetOrNewRun() {
        var jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForObject("select current_user",String.class)).thenReturn("gurhankucuk");
        when(jdbc.queryForObject("select current_database()",String.class)).thenReturn("tellpal_p4a2_cb5fe81a7ada4cb5adce6ddde1381d09");
        when(jdbc.queryForObject("select max(version::integer) from flyway_schema_history where success",Integer.class)).thenReturn(37);
        String query="select operations,reserved_bytes,max_operations,max_bytes from reading_comparison_budgets where run_id=?";
        when(jdbc.queryForMap(query,P4ConsumerQaLedger.RUN)).thenReturn(Map.of("operations",25L,"reserved_bytes",31572086L,"max_operations",64L,"max_bytes",67108864L));
        P4ConsumerQaSetup.validateDatabase(jdbc);
        when(jdbc.queryForMap(query,P4ConsumerQaLedger.RUN)).thenReturn(Map.of("operations",24L,"reserved_bytes",31572086L,"max_operations",64L,"max_bytes",67108864L));
        assertThatThrownBy(()->P4ConsumerQaSetup.validateDatabase(jdbc)).hasMessage("V37_PRIOR_DEBT_CHANGED");
        when(jdbc.queryForObject("select current_user",String.class)).thenReturn("tellpal");
        assertThatThrownBy(()->P4ConsumerQaSetup.validateDatabase(jdbc)).hasMessage("LOCAL_DB_USER_CHANGED");
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
}
