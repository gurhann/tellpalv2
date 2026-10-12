package com.tellpal.imageexperiment.host

import android.app.Activity
import android.os.Bundle
import com.tellpal.imageexperiment.*
import java.io.File
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject

object LauncherHarness {
    private fun launch(root: File, metadata: String? = HostedFixture.metadata().toString(), configure: () -> Unit = {}) {
        HostRuntime.reset(root)
        configure()
        val arguments = Bundle()
        if (metadata != null) arguments.putString("imageExperimentMetadata", HostedFixture.encode(metadata.toByteArray()))
        val launcher = ImageExperimentInstrumentation()
        launcher.onCreate(arguments)
        launcher.onStart()
    }

    private fun refused(stage: ImageExperimentRefusalStage?, expectedStatusCount: Int? = 0) {
        HarnessAssertions.equal(HostRuntime.resultCode, Activity.RESULT_CANCELED, "unchanged-refused-result")
        val result = HostRuntime.result!!
        HarnessAssertions.equal(result.getString("reason"), "NATIVE_LAUNCHER_REFUSED", "unchanged-generic-reason")
        HarnessAssertions.equal(result.getString("diagnosticStage"), stage?.code, "first-specific-stage-propagation")
        HarnessAssertions.equal(result.keySet(), if (stage == null) setOf("reason") else setOf("reason", "diagnosticStage"), "bounded-result-keys")
        expectedStatusCount?.let { HarnessAssertions.equal(HostRuntime.statuses.size, it, "pairing-count") }
        HostRuntime.activity?.let { activity ->
            HarnessAssertions.truth(activity.isFinishing && activity.finishCount == 1, "owned-activity-finished-once")
            activity.bootstrap?.let { bootstrap ->
                HarnessAssertions.truth(bootstrap.isRevoked && !bootstrap.launcherOutcome.isSettled(), "refusal-revoked-unsettled")
                val key = ImageExperimentBootstrap::class.java.getDeclaredField("key").apply { isAccessible = true }
                HarnessAssertions.equal(key.get(bootstrap), null, "refusal-key-cleared")
            }
        }
        HarnessAssertions.truth(HostRuntime.requests.all { it.disconnectCount > 0 }, "all-owned-connections-disconnected")
    }

    fun propagation(root: File) {
        HarnessAssertions.case("constructor-refusal-reaches-instrumentation") {
            launch(File(root, "private-propagation"), "{}")
            refused(ImageExperimentRefusalStage.metadata)
        }
    }

    fun run(root: File, expiredCertificate: File) {
        val early = listOf(
            Triple("signing-identity", ImageExperimentRefusalStage.signingIdentity, { HostRuntime.signatureResult = -1 }),
            Triple("real-self-test-vector", ImageExperimentRefusalStage.selfTestVector, { HostRuntime.failSharedVector = true }),
            Triple("activity-start", ImageExperimentRefusalStage.activityStart, { HostRuntime.failActivityLaunch = true }))
        for ((name, stage, configure) in early) HarnessAssertions.case("launcher-$name") {
            launch(File(root, name), configure = configure); refused(stage)
        }
        HarnessAssertions.case("launcher-missing-metadata") {
            launch(File(root, "missing"), null); refused(ImageExperimentRefusalStage.launcherMetadata)
        }
        HarnessAssertions.case("launcher-invalid-metadata") {
            launch(File(root, "invalid"), "{}"); refused(ImageExperimentRefusalStage.metadata)
        }
        HarnessAssertions.case("launcher-certificate-import-refusal") {
            val metadata = HostedFixture.metadata().apply {
                val bytes = byteArrayOf(1, 2, 3)
                put("brokerCertificateDerBase64", HostedFixture.encode(bytes))
                put("brokerCertificateSha256", ImageExperimentBootstrap.sha256(bytes))
            }
            launch(File(root, "certificate"), metadata.toString()); refused(ImageExperimentRefusalStage.certificate)
        }
        HarnessAssertions.case("launcher-real-expired-certificate-trust-context-refusal") {
            val bytes = expiredCertificate.readBytes()
            val certificate = java.security.cert.CertificateFactory.getInstance("X.509")
                .generateCertificate(java.io.ByteArrayInputStream(bytes)) as java.security.cert.X509Certificate
            HarnessAssertions.truth(certificate.notAfter.time < System.currentTimeMillis(), "real-certificate-expired")
            HarnessAssertions.truth(runCatching { certificate.checkValidity() }.exceptionOrNull() is
                java.security.cert.CertificateExpiredException, "real-certificate-validity-refuses-expiry")
            val metadata = HostedFixture.metadata().apply {
                put("brokerCertificateDerBase64", HostedFixture.encode(bytes))
                put("brokerCertificateSha256", ImageExperimentBootstrap.sha256(bytes))
            }
            launch(File(root, "expired-certificate"), metadata.toString())
            refused(ImageExperimentRefusalStage.trustContext)
            HarnessAssertions.equal(HostRuntime.activity!!.bootstrap, null, "expired-constructor-to-null-preserved")
            HarnessAssertions.equal(HostRuntime.activity!!.intent.getStringExtra("imageExperimentMetadata"), null, "expired-metadata-removed")
            HarnessAssertions.equal(HostRuntime.requests.size, 0, "expired-trust-context-no-requests")
        }
        val network = listOf("probe" to ImageExperimentRefusalStage.probe,
            "backend-probe" to ImageExperimentRefusalStage.backendProbe,
            "bootstrap/paired" to ImageExperimentRefusalStage.pairing,
            "bootstrap/challenge" to ImageExperimentRefusalStage.challenge,
            "bootstrap/claim" to ImageExperimentRefusalStage.claim)
        for ((path, stage) in network) HarnessAssertions.case("launcher-network-${stage.code}") {
            launch(File(root, stage.code)) { HostRuntime.networkFailure = "/experiment/v1/$path" }
            refused(stage, null)
            HarnessAssertions.equal(HostRuntime.requests.last().url.path, "/experiment/v1/$path", "failure-no-retry")
        }
        HarnessAssertions.case("launcher-original-challenge-validator") {
            launch(File(root, "challenge-validator")) { HostRuntime.networkFailure = "invalid-challenge" }
            refused(ImageExperimentRefusalStage.challenge, null)
        }
        HarnessAssertions.case("launcher-original-envelope-validator") {
            launch(File(root, "envelope-validator")) { HostRuntime.networkFailure = "invalid-envelope" }
            refused(ImageExperimentRefusalStage.envelopeFields, null)
        }
        for ((failure, stage) in listOf("invalid-envelope-time" to ImageExperimentRefusalStage.envelopeTimeToken,
            "invalid-envelope-manifest" to ImageExperimentRefusalStage.envelopeManifest)) {
            HarnessAssertions.case("launcher-$failure") {
                launch(File(root, failure)) { HostRuntime.networkFailure = failure }
                refused(stage, null)
            }
        }
        HarnessAssertions.case("launcher-original-positive-settlement-required") {
            launch(File(root, "settlement")) { HostRuntime.outcome = "refused" }
            refused(ImageExperimentRefusalStage.settlement, 1)
        }
        HarnessAssertions.case("launcher-dart-first-survives-later-native-settlement") {
            launch(File(root, "dart-first")) { HostRuntime.dartReport = "DART_MODE"; HostRuntime.outcome = "refused" }
            refused(ImageExperimentRefusalStage.dartMode, 1)
        }
        HarnessAssertions.case("launcher-ledger-required-for-original-settlement") {
            launch(File(root, "ledger-required")) { HostRuntime.ledger = "{}" }
            refused(ImageExperimentRefusalStage.settlement, 1)
        }
        HarnessAssertions.case("launcher-broken-diagnostic-export-preserves-refusal-cleanup") {
            launch(File(root, "broken-export")) { HostRuntime.networkFailure = "/experiment/v1/probe"; HostRuntime.throwDiagnosticExport = true }
            refused(null)
        }
        HarnessAssertions.case("launcher-success-is-silent-and-keeps-original-wire-contract") {
            launch(File(root, "success")) { HostRuntime.dartReport = "DART_MODE" }
            HarnessAssertions.equal(HostRuntime.resultCode, Activity.RESULT_OK, "unchanged-positive-result")
            HarnessAssertions.equal(HostRuntime.result!!.getString("reason"), "NATIVE_LAUNCHER_SETTLED", "unchanged-positive-reason")
            HarnessAssertions.equal(HostRuntime.result!!.keySet(), setOf("reason"), "success-no-diagnostic-field")
            HarnessAssertions.equal(HostRuntime.statuses.size, 1, "one-pairing-key")
            val pairing = HostRuntime.statuses.single()
            HarnessAssertions.equal(pairing.first, 1, "original-pairing-status-code")
            HarnessAssertions.equal(pairing.second.keySet(), setOf("event", "sessionId", "launchNonce", "publicKeySpkiBase64"), "original-pairing-fields")
            HarnessAssertions.equal(pairing.second.getString("event"), "pairing-key", "original-pairing-event")
            HarnessAssertions.truth(HostRuntime.activity!!.bootstrap!!.isRevoked && HostRuntime.activity!!.bootstrap!!.launcherOutcome.isSettled(), "original-explicit-settlement")
            HarnessAssertions.equal(HostRuntime.requests.map { it.requestMethod }, listOf("GET", "GET", "GET", "POST", "POST"), "original-request-order")
            for (request in HostRuntime.requests) {
                HarnessAssertions.truth(!request.instanceFollowRedirects && !request.useCaches && request.connectTimeout in 1..10_000 &&
                    request.readTimeout in 1..10_000 && request.disconnectCount > 0, "original-request-guards")
                HarnessAssertions.truth(request.hostnameVerifier === HttpsURLConnection.getDefaultHostnameVerifier(), "platform-hostname-verifier-retained")
                HarnessAssertions.equal(request.getRequestProperty("X-Experiment-Session"), "synthetic_session", "original-session-header")
                HarnessAssertions.equal(request.getRequestProperty("X-Experiment-Launch-Nonce"), "1".repeat(64), "original-nonce-header")
            }
            val proof = JSONObject(HostRuntime.requests.last().output.toString("UTF-8"))
            HarnessAssertions.equal(proof.keys().asSequence().toSet(), setOf("scope", "batchId", "sessionId", "launchNonce",
                "publicKeySpkiBase64", "challenge", "signatureBase64"), "original-proof-fields")
            val decoder = java.util.Base64.getDecoder()
            val publicDer = decoder.decode(proof.getString("publicKeySpkiBase64"))
            val publicKey = java.security.KeyFactory.getInstance("EC")
                .generatePublic(java.security.spec.X509EncodedKeySpec(publicDer))
            val payload = ImageExperimentBootstrap.signedPayload(proof.getString("scope"), proof.getString("batchId"),
                proof.getString("sessionId"), proof.getString("launchNonce"), proof.getString("challenge"), publicDer)
            val verified = java.security.Signature.getInstance("SHA256withECDSA").apply { initVerify(publicKey); update(payload) }
                .verify(decoder.decode(proof.getString("signatureBase64")))
            HarnessAssertions.truth(verified, "actual-bootstrap-proof-verifies-real-jca")
        }
    }
}
