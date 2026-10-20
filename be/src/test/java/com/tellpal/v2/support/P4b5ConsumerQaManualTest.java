package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** İki eski denemenin değişmez kanıtı ve aynı bütçe kimliği korunur. */
class P4b5ConsumerQaManualTest {
    @TempDir Path directory;
    @Test void manualScopeRequiresBothClosedAttemptsAndExactBooleanProof() throws Exception {
        var scope=P4b5ConsumerQaScope.P4B5_MANUAL;
        byte[] proposal=P4b5ConsumerQaLedgerTest.proposal(scope);
        var proof=(ObjectNode)P4b5ConsumerQaLedgerTest.JSON.readTree(P4b5ConsumerQaLedgerTest.priorProof(scope));
        scope.validatePriorProof(proposal,proof);
        assertThat(scope.identity().get("budgetRunId")).isEqualTo(P4b5ConsumerQaLedger.RUN);
        assertThat(proof.path("previousAttemptsEvidence").size()).isEqualTo(2);
        for(String field:List.of("previousAttemptsEvidence","historicalEvidence","priorConsumption","previousAttemptVerifiedByFileHashes","currentDatabaseQueried")) {
            var invalid=proof.deepCopy();invalid.put(field,0);
            assertThatThrownBy(()->scope.validatePriorProof(proposal,invalid)).hasMessage("PRIOR_ATTEMPT_PROOF_CHANGED");
        }
        for(int index=0;index<2;index++) {
            var invalid=proof.deepCopy();((ObjectNode)invalid.path("previousAttemptsEvidence").get(index)).put("terminalStatus","OPEN");
            assertThatThrownBy(()->scope.validatePriorProof(proposal,invalid)).hasMessage("PRIOR_ATTEMPT_PROOF_CHANGED");
        }
    }
    @Test void manualLedgerRetainsDebtAndFifthMediaCannotConsumeMore() throws Exception {
        var scope=P4b5ConsumerQaScope.P4B5_MANUAL;
        try(var ledger=P4b5ConsumerQaLedgerTest.ledger(scope,directory.resolve("ledger"))) {
            ledger.importLoginReservation();ledger.openWindow(directory.resolve("window"));
            ledger.request("manifest",new Object[]{"synthetic",94L,"tr"});ledger.verify();
            for(int index=0;index<4;index++)P4b5ConsumerQaLedgerTest.media(ledger,index);
            assertThat(ledger.completeMeasurement()).isTrue();
            assertThat(ledger.deliveryOperations()).isEqualTo(13);
            assertThat(ledger.reservedBytes()).isEqualTo(13170884);
            assertThatThrownBy(()->P4b5ConsumerQaLedgerTest.media(ledger,0)).hasMessage("MEDIA_BUDGET");
            assertThat(ledger.deliveryOperations()).isEqualTo(13);
        }
    }

    @Test void realApprovalGateBindsReviewedSourceRunAndApkBeforeStartupClaim() throws Exception {
        var scope=P4b5ConsumerQaScope.P4B5_MANUAL;
        for(String mutation:List.of("valid","decision","scope","run","apk")) {
            var fixture=new P4b5ConsumerQaSetupTest.ReviewedFixture(directory.resolve(mutation),scope);
            var approval=fixture.node();approval.put("decision","APPROVE_ONE_REAL_MANUAL_ATTEMPT")
                .put("runBindingSha256",fixture.input.path("runBindingSha256").asText()).put("apkSha256",fixture.selected.path("apkSha256").asText());
            switch(mutation) {
                case "decision" -> approval.put("decision","LOCAL_IMPLEMENTATION_ONLY");
                case "scope" -> approval.put("measurementId","old");
                case "run" -> approval.put("runBindingSha256","a".repeat(64));
                case "apk" -> approval.put("apkSha256","a".repeat(64));
                default -> {}
            }
            fixture.write("receipts/execution-approval-01.private.json",approval);
            if(mutation.equals("valid")) {
                P4b5ConsumerQaSetup.validateExecutionApproval(scope,fixture.root,fixture.input.path("runBindingSha256").asText(),fixture.selected.path("apkSha256").asText());
                // Geçerli artifact zinciri onay kapısını geçer; eksik prior proof hâlâ reddedilir.
                assertThatThrownBy(()->P4b5ConsumerQaSetup.validateReviewedRuntime(scope,fixture.root,fixture.repo,fixture.input,fixture.actualEntries,fixture.javaBinary,1073741824L)).hasMessage("NUMBERED_RECEIPT_REQUIRED");
            } else {
                assertThatThrownBy(()->P4b5ConsumerQaSetup.validateReviewedRuntime(scope,fixture.root,fixture.repo,fixture.input,fixture.actualEntries,fixture.javaBinary,1073741824L)).hasMessage("SEPARATE_REAL_EXECUTION_APPROVAL_REQUIRED");
            }
            assertThat(java.nio.file.Files.exists(fixture.root.resolve("backend/startup.claim.private.json"))).isFalse();
            assertThat(java.nio.file.Files.exists(fixture.root.resolve("install.claim.private.json"))).isFalse();
        }
    }
}
