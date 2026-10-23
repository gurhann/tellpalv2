package com.tellpal.v2.support;

import java.nio.file.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.tellpal.v2.asset.domain.reading.ReadingIdentity;

/** Ölçüm kökü ve kimliği yalnız sabit derleme sabitinden seçilir. */
enum P4b5ConsumerQaScope {
    P4B5("p4b5-7981ad25-c851-4ed6-af89-2d0fbfffd0f8",
            "e49eb8b706143f09c4fbf1738e2254f54e491d104954baf31e546a4497ec5833",
            "/Volumes/MAC_EK/tellpal-qa-private/p4b5-consumer-measurement-20261019-d80884d",
            "be/docs/evidence/p4b5-consumer-measurement/bounded-run-proposal-2026-10-19.json",
            "test_driver/reading_p4b5_qa_entry.dart"),
    P4B5_RETRY("p4b5-retry-afde2e82-70d1-430a-9094-d4532d5cd798",
            "06810424cfde11b22151aa4c719de448f5e4a1fa02d767c11765cac9627a2606",
            "/Volumes/MAC_EK/tellpal-qa-private/p4b5-retry-measurement-20261019-e11df87",
            "be/docs/evidence/p4b5-retry-measurement/bounded-run-proposal-2026-10-19.json",
            "test_driver/reading_p4b5_retry_qa_entry.dart"),
    P4B5_MANUAL("p4b5-manual-afa6ba59-d0af-493b-b4c3-91b8024b94a4",
            "f45fb1ac17bc8227af3fae5b284efd614409d590d57749fcff49e942a6e3ee92",
            "/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-consumer-measurement-20261020-be80ce1",
            "be/docs/evidence/p4b5-manual-consumer-measurement/bounded-run-proposal-2026-10-20.json",
            "test_driver/reading_consumer_manual_entry.dart"),
    P4B5_MANUAL_RETRY("p4b5-manual-retry-af5cbc33-ee49-41ef-aa04-986cb3af292f",
            "a3aab71cd8ae4685888c6d7d8004332bfc5fd9ff6d7ef91decb34e44ecd65fc6",
            "/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-retry-measurement-20261020-3ec09d1",
            "be/docs/evidence/p4b5-manual-retry-measurement/bounded-run-proposal-2026-10-20.json",
            "test_driver/reading_consumer_manual_retry_entry.dart");

    final String measurementId,proposalSha256,proposal,entry;
    final Path privateRoot;
    P4b5ConsumerQaScope(String measurementId,String proposalSha256,String privateRoot,String proposal,String entry) {
        this.measurementId=measurementId;this.proposalSha256=proposalSha256;this.privateRoot=Path.of(privateRoot);
        this.proposal=proposal;this.entry=entry;
    }
    static P4b5ConsumerQaScope select(String value) {
        if(value==null)return P4B5;
        return Arrays.stream(values()).filter(scope->scope.measurementId.equals(value)).findFirst()
                .orElseThrow(()->new IllegalStateException("UNKNOWN_QA_SCOPE"));
    }
    boolean manual() { return this==P4B5_MANUAL || this==P4B5_MANUAL_RETRY; }
    String packageName() {
        return this==P4B5_MANUAL_RETRY ? "com.tellpal.tellpal_mobile_poc.readingmanualretryqa"
                : this==P4B5_MANUAL ? "com.tellpal.tellpal_mobile_poc.readingmanualqa"
                : "com.tellpal.tellpal_mobile_poc.readingconsumerqa";
    }
    List<String> arguments(Path state) {
        return this==P4B5 ? List.of("--execute",state.toString()) : List.of("--scope",measurementId,"--execute",state.toString());
    }
    Map<String,String> identity() {
        return Map.of("measurementId",measurementId,"proposalSha256",proposalSha256,"budgetRunId",P4LocalComparisonLedger.RUN);
    }
    void validate(JsonNode value) {
        identity().forEach((key,expected)->P4b5ConsumerQaLedger.require(value.path(key).isTextual()
                && value.path(key).textValue().equals(expected),"REVIEWED_SCOPE_CHANGED"));
    }
    void validatePriorProof(byte[] proposalBytes,JsonNode proof) throws Exception {
        P4b5ConsumerQaLedger.require(this!=P4B5 && ReadingIdentity.sha(proposalBytes).equals(proposalSha256),"PRIOR_ATTEMPT_PROPOSAL_CHANGED");
        validate(proof);
        if(manual()) {
            var proposal=new ObjectMapper().readTree(proposalBytes);
            var expected=new ObjectMapper().valueToTree(identity());
            for(String key:List.of("previousAttemptsEvidence","historicalEvidence","priorConsumption"))((com.fasterxml.jackson.databind.node.ObjectNode)expected).set(key,proposal.path(key));
            ((com.fasterxml.jackson.databind.node.ObjectNode)expected).put("previousAttemptVerifiedByFileHashes",true).put("currentDatabaseQueried",false).put("oldBackendLedgerExists",false).put("actualStagesExecuted",false);
            P4b5ConsumerQaLedger.require(proof.equals(expected) && proof.path("previousAttemptsEvidence").size()==(this==P4B5_MANUAL_RETRY?3:2),"PRIOR_ATTEMPT_PROOF_CHANGED");
            return;
        }
        var previous=new ObjectMapper().readTree(proposalBytes).path("previousAttemptEvidence");
        var fields=new HashSet<String>();proof.fieldNames().forEachRemaining(fields::add);
        var proposal=new ObjectMapper().readTree(proposalBytes);
        P4b5ConsumerQaLedger.require(fields.equals(Set.of("measurementId","proposalSha256","budgetRunId","previousAttemptEvidence","historicalEvidence","priorConsumption","previousAttemptVerifiedByFileHashes","currentDatabaseQueried","oldBackendLedgerExists","actualStagesExecuted"))
                && previous.isObject() && previous.path("files").size()==12 && proof.path("previousAttemptEvidence").equals(previous)
                && proof.path("historicalEvidence").equals(proposal.path("historicalEvidence")) && proof.path("priorConsumption").equals(proposal.path("priorConsumption"))
                && proof.path("previousAttemptVerifiedByFileHashes").isBoolean() && proof.path("previousAttemptVerifiedByFileHashes").booleanValue()
                && java.util.stream.Stream.of("currentDatabaseQueried","oldBackendLedgerExists","actualStagesExecuted").allMatch(key->proof.path(key).isBoolean() && !proof.path(key).booleanValue()),"PRIOR_ATTEMPT_PROOF_CHANGED");
    }
    void validatePriorFiles(byte[] proposalBytes,JsonNode proof) throws Exception {
        validatePriorProof(proposalBytes,proof);
        if(manual()) {
            int index=0;
            for(var previous:proof.path("previousAttemptsEvidence")) {
                var previousScope=List.of(P4B5,P4B5_RETRY,P4B5_MANUAL).get(index);
                var root=previousScope.privateRoot;
                P4b5ConsumerQaLedger.require(previous.path("measurementId").asText().equals(previousScope.measurementId)
                        && previous.path("proposalSha256").asText().equals(previousScope.proposalSha256)
                        && previous.path("terminalStatus").asText().equals("FAILED_NO_RETRY")
                        && previous.path("mediaRequestsConsumed").asInt(-1)==0
                        && previous.path("additionalDeliveryOperations").asInt(-1)==0
                        && previous.path("additionalDeliveryReservedBytes").asInt(-1)==0,"PRIOR_ATTEMPT_SCOPE_CHANGED");
                P4b5ConsumerQaLedger.require(previous.path("privateRoot").asText().equals(root.toString()) && previous.path("files").size()==List.of(12,6,32).get(index++),"PRIOR_ATTEMPT_ROOT_CHANGED");
                var files=previous.path("files").fields();
                while(files.hasNext()) {
                    var file=files.next();Path path=Path.of(file.getKey());
                    P4b5ConsumerQaLedger.require(!path.isAbsolute() && path.normalize().equals(path) && !path.startsWith("..")
                        && ReadingIdentity.sha(P4b5ConsumerQaSetup.privateBytes(root.resolve(path),4194304)).equals(file.getValue().asText()),"PRIOR_ATTEMPT_FILE_CHANGED");
                }
            }
            return;
        }
        var previous=proof.path("previousAttemptEvidence");
        P4b5ConsumerQaLedger.require(previous.path("privateRoot").asText().equals(P4B5.privateRoot.toString()),"PRIOR_ATTEMPT_ROOT_CHANGED");
        var files=previous.path("files").fields();
        while(files.hasNext()) {
            var file=files.next();Path path=Path.of(file.getKey());
            P4b5ConsumerQaLedger.require(!path.isAbsolute() && path.normalize().equals(path) && !path.startsWith("..")
                    && ReadingIdentity.sha(P4b5ConsumerQaSetup.privateBytes(P4B5.privateRoot.resolve(path),4194304)).equals(file.getValue().asText()),"PRIOR_ATTEMPT_FILE_CHANGED");
        }
    }
}
