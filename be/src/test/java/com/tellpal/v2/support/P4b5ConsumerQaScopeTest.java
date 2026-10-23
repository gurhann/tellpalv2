package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** İki sabit scope ve önceki borç bağları gerçek ortamı açmadan sınanır. */
class P4b5ConsumerQaScopeTest {
    @TempDir Path directory;
    @Test void threeFixedScopesPreserveDefaultAndExactBackendArguments() throws Exception {
        assertThat(P4b5ConsumerQaScope.values()).hasSize(4);
        assertThat(P4b5ConsumerQaScope.select(null)).isSameAs(P4b5ConsumerQaScope.P4B5);
        assertThatThrownBy(()->P4b5ConsumerQaScope.select("unknown")).hasMessage("UNKNOWN_QA_SCOPE");
        for(var scope:P4b5ConsumerQaScope.values()) {
            assertThat(P4b5ConsumerQaScope.select(scope.measurementId)).isSameAs(scope);
            assertThat(com.tellpal.v2.asset.domain.reading.ReadingIdentity.sha(P4b5ConsumerQaLedgerTest.proposal(scope))).isEqualTo(scope.proposalSha256);
            P4b5ConsumerQaLedger.validateApproval(scope,P4b5ConsumerQaLedgerTest.approval(scope));
            var other=scope==P4b5ConsumerQaScope.P4B5?P4b5ConsumerQaScope.P4B5_RETRY:P4b5ConsumerQaScope.P4B5;
            assertThatThrownBy(()->P4b5ConsumerQaLedger.validateApproval(scope,P4b5ConsumerQaLedgerTest.approval(other))).hasMessage("APPROVAL_REQUIRED");
        }
        var state=directory.resolve("backend");
        assertThat(P4b5ConsumerQaScope.P4B5.arguments(state)).containsExactly("--execute",state.toString());
        assertThat(P4b5ConsumerQaScope.P4B5_RETRY.arguments(state)).containsExactly("--scope",P4b5ConsumerQaScope.P4B5_RETRY.measurementId,"--execute",state.toString());
        var output=new ByteArrayOutputStream();
        assertThat(P4b5ConsumerQaSetup.run(new String[]{"--scope","unknown","--execute",state.toString()},new PrintStream(output))).isEqualTo(2);
        assertThat(state).doesNotExist();
    }
    @Test void retryLedgerKeepsPriorDebtAndProofShaThenEnforcesUnchangedQuota() throws Exception {
        var scope=P4b5ConsumerQaScope.P4B5_RETRY;
        try(var ledger=P4b5ConsumerQaLedgerTest.ledger(scope,directory.resolve("ledger"))) {
            ledger.importLoginReservation();ledger.openWindow(directory.resolve("window"));
            ledger.request("manifest",new Object[]{"synthetic",94L,"tr"});ledger.verify();
            for(int i=0;i<4;i++)P4b5ConsumerQaLedgerTest.media(ledger,i);
            for(int i=0;i<12;i++){ledger.request("status",new Object[]{"synthetic",P4b5ConsumerQaLedgerTest.identity()});ledger.verify();}
            assertThat(ledger.deliveryOperations()).isEqualTo(25);assertThat(ledger.reservedBytes()).isEqualTo(13170884);
            assertThatThrownBy(()->ledger.request("status",new Object[]{"synthetic",P4b5ConsumerQaLedgerTest.identity()})).hasMessage("STATUS_BUDGET");
            assertThat(P4b5ConsumerQaLedgerTest.JSON.readTree(Files.readAllBytes(directory.resolve("window"))).path("measurementId").asText()).isEqualTo(scope.measurementId);
        }
        assertThat(Files.readString(directory.resolve("ledger"))).contains("OPEN_PENDING "+scope.measurementId+" "+scope.proposalSha256,
                "PRIOR_ATTEMPT "+com.tellpal.v2.asset.domain.reading.ReadingIdentity.sha(P4b5ConsumerQaLedgerTest.priorProof())," 39 44742970\n")
                .endsWith("CLOSED 4 13170884 22 1 12 4\n");
    }
    @Test void retryPriorProofMetadataScopeAndDebtMutationsRefuseBeforeLedgerCreation() throws Exception {
        var scope=P4b5ConsumerQaScope.P4B5_RETRY;var json=P4b5ConsumerQaLedgerTest.JSON;
        byte[] proposal=P4b5ConsumerQaLedgerTest.proposal(scope);var proof=(ObjectNode)json.readTree(P4b5ConsumerQaLedgerTest.priorProof());
        scope.validatePriorProof(proposal,proof);
        for(String mutation:List.of("scope","prior-debt","files","boolean-type","old-ledger")) {
            var invalid=proof.deepCopy();
            switch(mutation) {
                case "scope" -> invalid.put("measurementId",P4b5ConsumerQaScope.P4B5.measurementId);
                case "prior-debt" -> ((ObjectNode)invalid.path("priorConsumption")).put("deliveryOperations",0);
                case "files" -> ((ObjectNode)invalid.path("previousAttemptEvidence").path("files")).remove("terminal.private.json");
                case "boolean-type" -> invalid.put("actualStagesExecuted",0);
                case "old-ledger" -> invalid.put("oldBackendLedgerExists",true);
            }
            assertThatThrownBy(()->new P4b5ConsumerQaLedger(scope,directory.resolve("ledger"),proposal,P4b5ConsumerQaLedgerTest.fixture("terminal-ledger.txt"),P4b5ConsumerQaLedgerTest.fixture("retained-ready.json"),P4b5ConsumerQaLedgerTest.approval(scope),java.time.Clock.systemUTC(),()->0,false,json.writeValueAsBytes(invalid))).as(mutation).isInstanceOf(IllegalStateException.class);
            assertThat(directory.resolve("ledger")).doesNotExist();
        }
    }
    @Test void pythonBinaryControllerImportClosureAndCommandPrefixMustMatchReviewedRun() throws Exception {
        var scope=P4b5ConsumerQaScope.P4B5_RETRY;var json=P4b5ConsumerQaLedgerTest.JSON;
        Path repo=Files.createDirectory(directory.resolve("source")).toRealPath(),helpers=Files.createDirectories(repo.resolve("tools/image-optimization"));
        Path binary=directory.toRealPath().resolve("python");Files.writeString(binary,"synthetic-python");
        var files=json.createObjectNode();
        for(String name:List.of("p4b5_consumer_qa.py","p4b5_qa_scope.py","p4_consumer_qa.py","p4_resource_observation.py","p4_reading_comparison.py","native_test_login.py","native_entry_config.py")) {
            Path file=helpers.resolve(name);Files.writeString(file,"synthetic-"+name);files.put(file.toString(),com.tellpal.v2.asset.domain.reading.ReadingIdentity.sha(Files.readAllBytes(file)));
        }
        var python=(ObjectNode)json.valueToTree(scope.identity());python.put("pythonExecutable",binary.toString()).put("pythonBinary",binary.toString()).put("pythonBinarySha256",com.tellpal.v2.asset.domain.reading.ReadingIdentity.sha(Files.readAllBytes(binary)));
        python.put("controller",helpers.resolve("p4b5_consumer_qa.py").toString()).put("controllerSha256",files.path(helpers.resolve("p4b5_consumer_qa.py").toString()).asText());python.set("files",files);
        var run=json.createObjectNode();run.set("pythonCommandPrefix",json.valueToTree(List.of(binary.toString(),python.path("controller").asText(),"--scope",scope.measurementId,"--proposal-sha256",scope.proposalSha256)));
        P4b5ConsumerQaSetup.validatePythonBinding(scope,repo,run,python);
        for(String mutation:List.of("binary","controller","import","missing-import","prefix")) {
            var changed=python.deepCopy();var changedRun=run.deepCopy();
            switch(mutation) {
                case "binary" -> changed.put("pythonBinarySha256","b".repeat(64));
                case "controller" -> changed.put("controllerSha256","b".repeat(64));
                case "import" -> ((ObjectNode)changed.path("files")).put(helpers.resolve("p4b5_qa_scope.py").toString(),"b".repeat(64));
                case "missing-import" -> ((ObjectNode)changed.path("files")).remove(helpers.resolve("native_test_login.py").toString());
                case "prefix" -> changedRun.putArray("pythonCommandPrefix").add("wrong");
            }
            assertThatThrownBy(()->P4b5ConsumerQaSetup.validatePythonBinding(scope,repo,changedRun,changed)).as(mutation).isInstanceOf(Exception.class);
        }
    }

}
