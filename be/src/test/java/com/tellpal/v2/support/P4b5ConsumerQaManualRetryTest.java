package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tellpal.v2.asset.domain.reading.ReadingIdentity;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tam sentetik artifact kapısı; tarihsel dosya sınırında sabit hash fixture'ı kullanır. */
class P4b5ConsumerQaManualRetryTest {
    @TempDir Path directory;
    static final P4b5ConsumerQaScope SCOPE=P4b5ConsumerQaScope.P4B5_MANUAL_RETRY;

    @Test void threeClosedProofsAndUnrefundedLifecycleMutationsRefuse() throws Exception {
        var json=P4b5ConsumerQaLedgerTest.JSON;
        byte[] proposal=P4b5ConsumerQaLedgerTest.proposal(SCOPE);
        var proof=(ObjectNode)json.readTree(P4b5ConsumerQaLedgerTest.priorProof(SCOPE));
        SCOPE.validatePriorProof(proposal,proof);
        assertThat(proof.path("previousAttemptsEvidence").size()).isEqualTo(3);
        for(int index=0;index<3;index++)for(String field:List.of("measurementId","privateRoot","proposalSha256","runBindingSha256","files","qaInitialStartsConsumed","postAuditOwnedProcessProcAbsent")) {
            var invalid=proof.deepCopy();((ObjectNode)invalid.path("previousAttemptsEvidence").get(index)).put(field,0);
            assertThatThrownBy(()->SCOPE.validatePriorProof(proposal,invalid)).as(index+"/"+field).hasMessage("PRIOR_ATTEMPT_PROOF_CHANGED");
        }
        try(var ledger=P4b5ConsumerQaLedgerTest.ledger(SCOPE,directory.resolve("ledger"))) {
            ledger.importLoginReservation();ledger.openWindow(directory.resolve("window"));
            ledger.request("manifest",new Object[]{"synthetic",94L,"tr"});ledger.verify();
            for(int i=0;i<4;i++)P4b5ConsumerQaLedgerTest.media(ledger,i);
            for(int i=0;i<12;i++){ledger.request("status",new Object[]{"synthetic",P4b5ConsumerQaLedgerTest.identity()});ledger.verify();}
            assertThat(ledger.deliveryOperations()).isEqualTo(25);
            assertThat(ledger.reservedBytes()).isEqualTo(13170884);
            assertThatThrownBy(()->P4b5ConsumerQaLedgerTest.media(ledger,0)).hasMessage("MEDIA_BUDGET");
        }
    }

    @Test void completeNewScopeArtifactPassesFinalGateAndIdentityMutantsRefuseWithoutRealStartup() throws Exception {
        for(String mutation:List.of("valid","sourceDirectory","entry","package","prefix","import","approval-missing","approval-extra","approval-old","proof-audit","proof-start")) {
            var f=new P4b5ConsumerQaSetupTest.ReviewedFixture(directory.resolve(mutation),SCOPE);
            var json=f.json;
            Path proposal=f.repo.resolve(SCOPE.proposal);Files.createDirectories(proposal.getParent());Files.write(proposal,P4b5ConsumerQaLedgerTest.proposal(SCOPE));
            var proof=(ObjectNode)json.readTree(P4b5ConsumerQaLedgerTest.priorProof(SCOPE));
            if(mutation.equals("proof-audit"))((ObjectNode)proof.path("previousAttemptsEvidence").get(2).path("files")).remove("receipts/runtime-post-cleanup-audit-0001.private.json");
            if(mutation.equals("proof-start"))((ObjectNode)proof.path("previousAttemptsEvidence").get(2)).put("qaInitialStartsConsumed",0);
            f.write("receipts/prior-attempt-proof-0001.private.json",proof);
            Path scripts=Files.createDirectories(f.repo.resolve("tools/image-optimization"));
            var files=json.createObjectNode();
            for(String name:List.of("p4b5_manual_consumer_qa.py","p4b5_consumer_qa.py","p4b5_qa_scope.py","p4_consumer_qa.py","p4_resource_observation.py","p4_reading_comparison.py","native_test_login.py","native_entry_config.py")) {
                Path path=scripts.resolve(name);Files.writeString(path,"synthetic-"+name);files.put(path.toString(),f.sha(path));
            }
            Path account=f.repo.resolve("tools/mobile-test-account.json");Files.writeString(account,"synthetic-account");files.put(account.toString(),f.sha(account));
            Path binary=f.root.resolve("python");Files.writeString(binary,"synthetic-python");
            var python=f.node();python.put("pythonExecutable",binary.toString()).put("pythonBinary",binary.toString()).put("pythonBinarySha256",f.sha(binary));
            Path controller=scripts.resolve("p4b5_manual_consumer_qa.py");python.put("controller",controller.toString()).put("controllerSha256",f.sha(controller));python.set("files",files);
            if(mutation.equals("import")) {
                Path old=Files.createDirectories(f.root.resolve("old-source")).resolve("p4b5_consumer_qa.py");Files.writeString(old,"old-owned-import");files.put(old.toString(),f.sha(old));
            }
            f.write("receipts/python-binding-0001.private.json",python);
            for(String key:List.of("priorAttemptProof","pythonBinding")) {
                String name=key.equals("priorAttemptProof")?"prior-attempt-proof-0001.private.json":"python-binding-0001.private.json";
                f.run.put(key+"File",name).put(key+"Sha256",f.sha(f.root.resolve("receipts").resolve(name)));
            }
            f.run.set("pythonCommandPrefix",json.valueToTree(List.of(binary.toString(),controller.toString(),"--scope",mutation.equals("prefix")?P4b5ConsumerQaScope.P4B5_MANUAL.measurementId:SCOPE.measurementId,"--proposal-sha256",SCOPE.proposalSha256)));
            if(mutation.equals("sourceDirectory")) {
                f.source.put("sourceDirectory","source");f.replace("receipts/source-binding-0001.private.json",f.source);
                f.run.put("sourceBindingSha256",f.sha(f.root.resolve("receipts/source-binding-0001.private.json")));
                f.input.set("sourceBindingSha256",f.run.path("sourceBindingSha256"));
            }
            Path buildPath=f.root.resolve("android/builds/build-0001/build.private.json");var build=(ObjectNode)json.readTree(Files.readAllBytes(buildPath));
            if(mutation.equals("entry"))build.put("entry",P4b5ConsumerQaScope.P4B5_MANUAL.entry);
            if(mutation.equals("package"))build.put("package",P4b5ConsumerQaScope.P4B5_MANUAL.packageName());
            Files.write(buildPath,json.writeValueAsBytes(build));
            f.replace("receipts/run-binding-0001.private.json",f.run);
            String runSha=f.sha(f.root.resolve("receipts/run-binding-0001.private.json"));f.input.put("runBindingSha256",runSha);
            f.replace("backend-start.claim.private.json",f.node().put("runBindingSha256",runSha).put("state","RESERVED_NO_AUTOMATIC_RETRY"));
            for(int n=1;n<=3;n++)f.replace("reviews/pre-run-0001-0"+n+".private.json",f.node().put("sourceBindingSha256",f.run.path("sourceBindingSha256").asText()).put("runBindingSha256",runSha).put("status","PASS").put("unresolvedFindings",0).put("reviewerId","synthetic-"+n));
            var approval=f.node().put("decision","APPROVE_ONE_REAL_MANUAL_ATTEMPT").put("runBindingSha256",runSha).put("apkSha256",f.selected.path("apkSha256").asText());
            if(mutation.equals("approval-extra"))approval.put("note","human explanation");
            if(mutation.equals("approval-old"))approval.put("measurementId",P4b5ConsumerQaScope.P4B5_MANUAL.measurementId);
            if(!mutation.equals("approval-missing"))f.write("receipts/execution-approval-01.private.json",approval);
            var historical=new HashMap<Path,byte[]>();var expectedHashes=new HashMap<String,String>();
            for(var previous:json.readTree(P4b5ConsumerQaLedgerTest.priorProof(SCOPE)).path("previousAttemptsEvidence")) {
                var entries=previous.path("files").fields();while(entries.hasNext()) {
                    var entry=entries.next();Path path=Path.of(previous.path("privateRoot").asText()).resolve(entry.getKey());
                    String sentinel="synthetic-history:"+path;historical.put(path,sentinel.getBytes(StandardCharsets.UTF_8));expectedHashes.put(sentinel,entry.getValue().asText());
                }
            }
            // Yalnız eski özel dosya sınırı sentetiktir; son artifact kapısı ve diğer hash'ler gerçektir.
            try(var reads=mockStatic(P4b5ConsumerQaSetup.class,CALLS_REAL_METHODS);var hashes=mockStatic(ReadingIdentity.class,CALLS_REAL_METHODS)) {
                reads.when(()->P4b5ConsumerQaSetup.privateBytes(any(Path.class),anyInt())).thenAnswer(call->{
                    Path path=call.getArgument(0);return historical.containsKey(path)?historical.get(path):call.callRealMethod();
                });
                hashes.when(()->ReadingIdentity.sha(any(byte[].class))).thenAnswer(call->{
                    String data=new String((byte[])call.getArgument(0),StandardCharsets.UTF_8);return expectedHashes.containsKey(data)?expectedHashes.get(data):call.callRealMethod();
                });
                if(mutation.equals("valid"))assertThat(P4b5ConsumerQaSetup.validateReviewedRuntime(SCOPE,f.root,f.repo,f.input,f.actualEntries,f.javaBinary,1073741824L).path("apkSha256").asText()).isEqualTo(f.selected.path("apkSha256").asText());
                else assertThatThrownBy(()->P4b5ConsumerQaSetup.validateReviewedRuntime(SCOPE,f.root,f.repo,f.input,f.actualEntries,f.javaBinary,1073741824L)).as(mutation).isInstanceOf(Exception.class);
            }
            assertThat(f.root.resolve("backend/startup.claim.private.json")).doesNotExist();
            assertThat(f.root.resolve("install.claim.private.json")).doesNotExist();
        }
    }
}
