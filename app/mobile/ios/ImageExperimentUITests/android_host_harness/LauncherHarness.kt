package com.tellpal.imageexperiment.host

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.tellpal.imageexperiment.*
import java.io.File
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject

object LauncherHarness {
    private fun launch(root: File, metadata: String? = HostedFixture.metadata().toString(), configure: () -> Unit = {}): ImageExperimentInstrumentation {
        HostRuntime.reset(root)
        configure()
        val arguments = Bundle()
        if (HostRuntime.qualityCapture) arguments.putString("imageExperimentQualityCapture", "1")
        if (metadata != null) arguments.putString("imageExperimentMetadata", HostedFixture.encode(metadata.toByteArray()))
        val launcher = ImageExperimentInstrumentation()
        launcher.onCreate(arguments)
        launcher.onStart()
        HostRuntime.onMain {}
        return launcher
    }

    private fun refused(stage: ImageExperimentRefusalStage?, expectedStatusCount: Int? = 0,
        substage: ImageExperimentQualityStage? = null) {
        HarnessAssertions.equal(HostRuntime.resultCode, Activity.RESULT_CANCELED, "unchanged-refused-result")
        HarnessAssertions.equal(HostRuntime.terminalCount, 1, "one-terminal-refusal")
        val result = HostRuntime.result!!
        HarnessAssertions.equal(result.getString("reason"), "NATIVE_LAUNCHER_REFUSED", "unchanged-generic-reason")
        HarnessAssertions.equal(result.getString("diagnosticStage"), stage?.code, "first-specific-stage-propagation")
        val keys = if (stage == null) setOf("reason") else setOf("reason", "diagnosticStage")
        HarnessAssertions.equal(result.keySet(), if (substage == null) keys else keys + "diagnosticSubstage", "bounded-result-keys")
        HarnessAssertions.equal(result.getString("diagnosticSubstage"), substage?.code, "quality-first-substage")
        expectedStatusCount?.let { HarnessAssertions.equal(HostRuntime.statuses.size, it, "pairing-count") }
        HostRuntime.activity?.let { activity ->
            HarnessAssertions.truth(if (activity.isDestroyed) activity.finishCount == 0
                else activity.isFinishing && activity.finishCount == 1, "owned-activity-finished-once")
            activity.bootstrap?.let { bootstrap ->
                HarnessAssertions.truth(bootstrap.isRevoked && (!bootstrap.launcherOutcome.isSettled() ||
                    substage == ImageExperimentQualityStage.ledgerExport), "refusal-revoked-unsettled")
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
            HarnessAssertions.equal(HostRuntime.requests.map { it.url.path }.distinct(), listOf("/experiment/v1/probe",
                "/experiment/v1/backend-probe", "/experiment/v1/bootstrap/paired", "/experiment/v1/bootstrap/challenge",
                "/experiment/v1/bootstrap/claim"), "original-request-order")
            HarnessAssertions.truth(HostRuntime.requests.all { it.requestMethod ==
                if (it.url.path.endsWith("challenge") || it.url.path.endsWith("claim")) "POST" else "GET" }, "original-request-methods")
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
        lifecycle(root)
        qualityDiagnostics(root)
        readerQualityWaits(root)
        readerQualityLauncher(root)
    }

    fun readerQualityLauncher(root: File) {
        for (batch in listOf("reader_quality_full_r1", "reader_quality_100_r1", "reader_quality_075_r1")) {
            HarnessAssertions.case("reader-quality-launcher-$batch-responsive200ms-settles") {
                HostRuntime.reset(File(root, "launcher-$batch"))
                HostRuntime.readerFixture = true; HostRuntime.readerBatchId = batch
                HostRuntime.qualityCapture = true; HostRuntime.offMainUiDelay = 200
                HostRuntime.sessionCompletionDelayMillis = 800
                val arguments = Bundle().apply {
                    putString("imageExperimentQualityCapture", "1")
                    putString("imageExperimentMetadata", HostedFixture.encode(HostedFixture.metadata().toString().toByteArray()))
                }
                val launcher = ImageExperimentInstrumentation()
                launcher.onCreate(arguments); launcher.onStart(); HostRuntime.onMain {}
                HarnessAssertions.equal(HostRuntime.resultCode, Activity.RESULT_OK,
                    "reader-quality-launcher-success-${HostRuntime.result?.getString("diagnosticStage")}-${HostRuntime.result?.getString("diagnosticSubstage")}")
                HarnessAssertions.equal(HostRuntime.terminalCount, 1, "reader-quality-launcher-one-terminal")
                HarnessAssertions.truth(HostRuntime.qualityStatuses.isNotEmpty(), "reader-quality-launcher-publishes-binding")
                HarnessAssertions.equal(HostRuntime.deviceFactsStatusCount, 1, "reader-quality-launcher-observes-device-facts")
                HarnessAssertions.truth(HostRuntime.activity!!.bootstrap!!.isRevoked &&
                    HostRuntime.activity!!.bootstrap!!.launcherOutcome.isSettled(), "reader-quality-launcher-authenticated-settlement")
            }
        }
    }

    private fun qualityDiagnostics(root: File) {
        for ((name, stage, configure) in listOf(
            Triple("wait-timeout", ImageExperimentQualityStage.uiWaitTimeout, {
                HostRuntime.suppressSessionCompletion = true; HostRuntime.offMainUiDelay = 200 }),
            Triple("encoding", ImageExperimentQualityStage.statusEncoding, {
                HostRuntime.suppressSessionCompletion = true; HostRuntime.failQualityEncoding = true }),
            Triple("status-send", ImageExperimentQualityStage.statusSend, {
                HostRuntime.suppressSessionCompletion = true; HostRuntime.failQualityStatus = true }),
            Triple("settlement", ImageExperimentQualityStage.authenticatedSettlement, { HostRuntime.outcome = "refused" }),
            Triple("ledger", ImageExperimentQualityStage.ledgerExport, { HostRuntime.corruptLedgerAfterRevoke = true }))) {
            HarnessAssertions.case("quality-actual-instrumentation-$name") {
                launch(File(root, "quality-$name")) { HostRuntime.qualityCapture = true; configure() }
                refused(ImageExperimentRefusalStage.settlement, 1, stage)
                HarnessAssertions.truth(HostRuntime.result!!.keySet().all { key ->
                    HostRuntime.result!!.getString(key)?.contains(HostRuntime.failure.message!!) != true }, "quality-no-raw-failure")
                if (name == "wait-timeout") HarnessAssertions.equal(HostRuntime.qualityStatuses.size, 0, "quality-timeout-no-status")
            }
        }
        HarnessAssertions.case("quality-actual-interrupted-wait-is-not-timeout") {
            HostRuntime.reset(File(root, "quality-interrupted"))
            val launcher = ImageExperimentInstrumentation()
            val diagnostics = ImageExperimentInstrumentation::class.java.getDeclaredField("qualityDiagnostics")
                .apply { isAccessible = true }.get(launcher) as ImageExperimentQualityDiagnostics
            diagnostics.enabled = true
            val method = ImageExperimentInstrumentation::class.java.getDeclaredMethod("reportQualityBinding", ImageExperimentActivity::class.java)
                .apply { isAccessible = true }
            val activity = ImageExperimentActivity()
            Thread.currentThread().interrupt()
            val failure = try { runCatching { method.invoke(launcher, activity) }.exceptionOrNull() }
                finally { Thread.interrupted() }
            HostRuntime.onMain {}
            HarnessAssertions.truth((failure as? java.lang.reflect.InvocationTargetException)?.cause is InterruptedException,
                "quality-original-interruption-rethrown")
            HarnessAssertions.equal(diagnostics.firstStage, ImageExperimentQualityStage.uiWait, "quality-interruption-distinct-from-timeout")
        }
        HarnessAssertions.case("quality-first-substage-and-original-dart-cause-survive-cleanup") {
            val diagnostics = ImageExperimentQualityDiagnostics().apply { enabled = true }
            val original = HostRuntime.failure
            for (stage in listOf(ImageExperimentQualityStage.statusSend, ImageExperimentQualityStage.loopBody,
                ImageExperimentQualityStage.loopExit, ImageExperimentQualityStage.nativeDeadline,
                ImageExperimentQualityStage.primaryBackground, ImageExperimentQualityStage.bootstrapRevocation,
                ImageExperimentQualityStage.authenticatedSettlement, ImageExperimentQualityStage.ledgerExport)) {
                val failure = runCatching { diagnostics.run(stage) { throw original } }.exceptionOrNull()
                HarnessAssertions.truth(failure === original, "quality-original-exception-identity")
                HarnessAssertions.equal(diagnostics.firstStage, ImageExperimentQualityStage.statusSend, "quality-first-cause-not-cleanup")
            }
            launch(File(root, "quality-dart-first")) {
                HostRuntime.qualityCapture = true; HostRuntime.dartReport = "DART_MODE"; HostRuntime.outcome = "refused"
            }
            refused(ImageExperimentRefusalStage.dartMode, 1, ImageExperimentQualityStage.authenticatedSettlement)
        }
        HarnessAssertions.case("quality-each-finite-substage-and-disabled-observer-preserve-failure") {
            val codes = ImageExperimentQualityStage.entries.map { it.code }
            HarnessAssertions.truth(codes.size == 12 && codes.toSet().size == 12 &&
                codes.all { it.length <= 64 && it.matches(Regex("NATIVE_QUALITY_[A-Z_]+")) }, "quality-finite-code-inventory")
            for (stage in ImageExperimentQualityStage.entries) {
                val diagnostics = ImageExperimentQualityDiagnostics().apply { enabled = true }
                val failure = runCatching { diagnostics.run(stage) { throw HostRuntime.failure } }.exceptionOrNull()
                HarnessAssertions.truth(failure === HostRuntime.failure, "quality-each-stage-original-exception")
                HarnessAssertions.equal(diagnostics.firstStage, stage, "quality-each-stage-recorded")
                val disabled = ImageExperimentQualityDiagnostics()
                runCatching { disabled.run(stage) { throw HostRuntime.failure } }
                HarnessAssertions.equal(disabled.firstStage, null, "quality-disabled-silent")
            }
        }
        HarnessAssertions.case("quality-success-keeps-original-wire-and-null-marker") {
            launch(File(root, "quality-success")) { HostRuntime.qualityCapture = true }
            HarnessAssertions.equal(HostRuntime.resultCode, Activity.RESULT_OK, "quality-success-original-result")
            HarnessAssertions.equal(HostRuntime.result!!.keySet(), setOf("reason"), "quality-success-no-diagnostic")
            HarnessAssertions.truth(HostRuntime.qualityStatuses.isNotEmpty(), "quality-original-status-emitted")
        }
    }

    private fun readerQualityWaits(root: File) {
        fun setup(name: String): Pair<ImageExperimentInstrumentation, ImageExperimentActivity> {
            HostRuntime.reset(File(root, name))
            val launcher = ImageExperimentInstrumentation()
            val activity = ImageExperimentActivity()
            HostRuntime.activity = activity
            HostRuntime.onMain {
                activity.intent = Intent().putExtra("imageExperimentMetadata", HostedFixture.metadata().toString())
                activity.onCreate(null)
            }
            val diagnostics = ImageExperimentInstrumentation::class.java.getDeclaredField("qualityDiagnostics")
                .apply { isAccessible = true }.get(launcher) as ImageExperimentQualityDiagnostics
            diagnostics.enabled = true
            return launcher to activity
        }
        val report = ImageExperimentInstrumentation::class.java.getDeclaredMethod("reportReaderQualityBinding",
            ImageExperimentActivity::class.java, Long::class.javaPrimitiveType).apply { isAccessible = true }
        val mainRead = ImageExperimentInstrumentation::class.java.declaredMethods.single { it.name == "readerMain" }
            .apply { isAccessible = true }
        HarnessAssertions.case("reader-quality-responsive200ms-keeps-legacy150ms-separate") {
            val (launcher, activity) = setup("reader-responsive200")
            HostRuntime.offMainUiDelay = 200
            val start = System.nanoTime()
            report.invoke(launcher, activity, android.os.SystemClock.elapsedRealtime() + 5_000)
            HarnessAssertions.truth((System.nanoTime() - start) / 1_000_000 in 150..1_000, "reader-responsive-ui-wait-range")
            HarnessAssertions.equal(HostRuntime.qualityStatuses.size, 1, "reader-responsive-publishes-once")
            // The registered legacy quality-actual-instrumentation-wait-timeout case
            // uses the same200ms boundary and must still refuse its unchanged150ms wait.
        }
        for ((name, deadlineMillis, interrupt, retire) in listOf(
                listOf("timeout500", 5_000L, false, false),
                listOf("absolute120", 120L, false, false),
                listOf("interrupted", 5_000L, true, false),
                listOf("retired", 5_000L, false, true))) {
            HarnessAssertions.case("reader-quality-blocked-$name-excludes-late-read-and-publication") {
                val (launcher, activity) = setup("reader-$name")
                val entered = java.util.concurrent.CountDownLatch(1)
                val release = java.util.concurrent.CountDownLatch(1)
                HostRuntime.main.submit { entered.countDown(); require(release.await(3, java.util.concurrent.TimeUnit.SECONDS)) }
                HarnessAssertions.truth(entered.await(1, java.util.concurrent.TimeUnit.SECONDS), "reader-main-blocked-before-snapshot")
                val reads = java.util.concurrent.atomic.AtomicInteger()
                val timeouts = java.util.concurrent.atomic.AtomicInteger()
                val retired = java.util.concurrent.atomic.AtomicInteger()
                val failures = java.util.concurrent.CopyOnWriteArrayList<Throwable>()
                val done = java.util.concurrent.CountDownLatch(2)
                val deadline = android.os.SystemClock.elapsedRealtime() + (deadlineMillis as Long)
                fun worker(action: () -> Unit) = Thread {
                    if (interrupt as Boolean) Thread.currentThread().interrupt()
                    try { action() }
                    catch (failure: java.lang.reflect.InvocationTargetException) { failures += failure.cause!! }
                    finally { Thread.interrupted(); done.countDown() }
                }.also { it.start() }
                val start = System.nanoTime()
                val reportWorker = worker { report.invoke(launcher, activity, deadline) }
                val readWorker = worker {
                    mainRead.invoke(launcher, activity, deadline,
                        { timeouts.incrementAndGet(); Unit }, { retired.incrementAndGet(); null }, { reads.incrementAndGet(); null })
                }
                try {
                    if (retire as Boolean) {
                        Thread.sleep(25)
                        val bootstrap = activity.bootstrap!!
                        bootstrap.launcherOutcome.markSessionHandedOff()
                        bootstrap.launcherOutcome.markLedgerStored()
                        bootstrap.launcherOutcome.complete("settled", false)
                        bootstrap.revoke()
                        release.countDown()
                    }
                    HarnessAssertions.truth(done.await(1_200, java.util.concurrent.TimeUnit.MILLISECONDS), "reader-snapshot-failure-bounded")
                    val elapsed = (System.nanoTime() - start) / 1_000_000
                    if (name == "timeout500") HarnessAssertions.truth(elapsed in 450..1_100, "reader-exact500ms-wait-bound")
                    if (name == "absolute120") HarnessAssertions.truth(elapsed < 450, "reader-absolute-deadline-before500ms")
                    HarnessAssertions.equal(HostRuntime.qualityStatuses.size, 0, "reader-refused-or-retired-no-publication")
                    if (retire as Boolean) {
                        HarnessAssertions.truth(failures.isEmpty(), "reader-normal-retirement-is-not-refusal")
                        HarnessAssertions.truth(activity.bootstrap!!.isRevoked && activity.bootstrap!!.launcherOutcome.isSettled(),
                            "reader-original-authenticated-settlement-gate-preserved")
                        HarnessAssertions.equal(retired.get(), 1, "reader-retired-read-returned-null")
                    } else {
                        HarnessAssertions.equal(failures.size, 2, "reader-both-owned-waits-refused")
                        HarnessAssertions.truth(failures.all { if (interrupt as Boolean) it is InterruptedException
                            else it is IllegalArgumentException }, "reader-timeout-vs-interrupt-original-failure")
                        HarnessAssertions.equal(timeouts.get(), if (interrupt as Boolean) 0 else 1, "reader-timeout-only-after-false-await")
                    }
                } finally {
                    release.countDown(); reportWorker.join(1_000); readWorker.join(1_000); HostRuntime.onMain {}
                }
                HarnessAssertions.equal(reads.get(), 0, "reader-cancelled-or-retired-callback-never-read")
                HarnessAssertions.equal(HostRuntime.qualityStatuses.size, 0, "reader-late-callback-never-published")
                val diagnostics = ImageExperimentInstrumentation::class.java.getDeclaredField("qualityDiagnostics")
                    .apply { isAccessible = true }.get(launcher) as ImageExperimentQualityDiagnostics
                HarnessAssertions.equal(diagnostics.firstStage, if (retire as Boolean) null else
                    if (interrupt as Boolean) ImageExperimentQualityStage.uiWait else ImageExperimentQualityStage.uiWaitTimeout,
                    "reader-finite-timeout-interrupt-retirement-diagnostic")
            }
        }
    }

    fun startup(root: File) {
        HarnessAssertions.case("launcher-startup-dependency-reports-before-first-draw") {
            val metadata = HostedFixture.metadata().put("deadline", System.currentTimeMillis() + 5_000)
            launch(File(root, "startup-dependency"), metadata.toString())
            println("STARTUP_REPORT_COUNT ${HostRuntime.statuses.size}")
            HarnessAssertions.equal(HostRuntime.statuses.size, 1, "startup-cycle-one-pairing-report")
            HarnessAssertions.equal(HostRuntime.resultCode, Activity.RESULT_OK, "startup-cycle-explicit-success")
            HarnessAssertions.equal(HostRuntime.terminalCount, 1, "startup-cycle-one-terminal")
            HarnessAssertions.equal(HostRuntime.events.toList(), listOf("status", "pairing", "handoff", "startup-ready", "settlement"),
                "status-pairing-handoff-startup-settlement-order")
            val status = HostRuntime.statuses.single()
            HarnessAssertions.equal(status.first, 1, "startup-original-status-code")
            HarnessAssertions.equal(status.second.keySet(), setOf("event", "sessionId", "launchNonce", "publicKeySpkiBase64"),
                "startup-original-status-fields")
            HarnessAssertions.equal(status.second.getString("sessionId"), metadata.getString("sessionId"), "startup-exact-session")
            HarnessAssertions.equal(status.second.getString("launchNonce"), metadata.getString("launchNonce"), "startup-exact-nonce")
            HarnessAssertions.equal(status.second.getString("publicKeySpkiBase64"), HostRuntime.activity!!.bootstrap!!.publicKeySpkiBase64,
                "startup-exact-public-key")
        }
    }

    private fun lifecycle(root: File) {
        startup(root)
        responsiveMain(root)
        blockedMain(root)
        for ((name, flag) in listOf("constructor-failure" to { HostRuntime.failActivityConstructor = true },
            "on-create-failure" to { HostRuntime.failActivityCreate = true },
            "early-destroy" to { HostRuntime.destroyDuringCreate = true })) {
            HarnessAssertions.case("launcher-lifecycle-$name") {
                val metadata = HostedFixture.metadata().put("deadline", System.currentTimeMillis() + 500)
                launch(File(root, name), metadata.toString(), flag)
                refused(if (name == "early-destroy") ImageExperimentRefusalStage.activityBootstrap else ImageExperimentRefusalStage.activityStart)
            }
        }
        HarnessAssertions.case("launcher-no-callback-absolute-deadline") {
            val metadata = HostedFixture.metadata().put("deadline", System.currentTimeMillis() + 500)
            val start = System.nanoTime()
            launch(File(root, "no-callback"), metadata.toString()) { HostRuntime.deferCreation = true }
            refused(ImageExperimentRefusalStage.activityStart)
            HarnessAssertions.truth((System.nanoTime() - start) / 1_000_000 in 1..2_000, "creation-absolute-deadline-bounded")
            HarnessAssertions.equal(HostRuntime.activity, null, "no-callback-no-adoption")
        }
        HarnessAssertions.case("launcher-lifecycle-missing-bootstrap-metadata") {
            launch(File(root, "missing-bootstrap")) {
                HostRuntime.beforeCreate = { _, _, intent -> intent.removeExtra("imageExperimentMetadata") }
            }
            refused(ImageExperimentRefusalStage.metadata)
            HarnessAssertions.equal(HostRuntime.activity!!.bootstrap, null, "missing-bootstrap-no-admission")
        }
        HarnessAssertions.case("launcher-invalid-deadline-keeps-bootstrap-diagnostic") {
            val metadata = HostedFixture.metadata().put("deadline", 0)
            launch(File(root, "invalid-deadline"), metadata.toString())
            refused(ImageExperimentRefusalStage.metadata)
        }
        HarnessAssertions.case("launcher-creation-and-reachability-share-launch-bound") {
            launch(File(root, "shared-launch-bound")) {
                HostRuntime.beforeCreate = { _, _, _ -> HostRuntime.elapsedOffset += 29_000 }
                HostRuntime.backendProbeClockAdvance = 2_000
            }
            refused(ImageExperimentRefusalStage.reachability)
            HarnessAssertions.equal(HostRuntime.elapsedOffset, 31_000L, "creation-plus-probe-exceeds-shared-bound")
        }
        for (afterInstantiation in listOf(false, true)) {
            HarnessAssertions.case("launcher-late-owned-callback-after-closure-$afterInstantiation") {
                val launcher = launch(File(root, "late-$afterInstantiation")) {
                    HostRuntime.deferCreation = !afterInstantiation
                    HostRuntime.deferAfterInstantiation = afterInstantiation
                    HostRuntime.advanceClockOnLaunch = 30_000
                }
                HarnessAssertions.equal(HostRuntime.resultCode, Activity.RESULT_CANCELED, "late-owner-initial-refusal")
                HostRuntime.releaseCreation()
                refused(ImageExperimentRefusalStage.activityStart)
                val owner = HostRuntime.activity!!
                HarnessAssertions.truth(owner.bootstrap!!.isRevoked, "late-owner-bootstrap-revoked")
                HarnessAssertions.equal(HostRuntime.requests.size, 0, "late-owner-no-network")
                HostRuntime.onMain { launcher.callActivityOnCreate(owner, null) }
                launcher.onStart()
                HarnessAssertions.equal(owner.finishCount, 1, "late-duplicate-no-extra-finish")
                HarnessAssertions.equal(HostRuntime.terminalCount, 1, "late-duplicate-no-extra-terminal")
            }
        }
        HarnessAssertions.case("launcher-foreign-and-second-instances-stay-unowned") {
            val foreign = mutableListOf<Activity>()
            val launcher = launch(File(root, "foreign")) {
                HostRuntime.beforeInstantiate = { instrumentation, intent ->
                    val raw = intent.getStringExtra("imageExperimentMetadata")!!
                    for ((className, value) in listOf("android.app.Activity" to raw,
                        ImageExperimentActivity::class.java.name to "{}")) {
                        val otherIntent = Intent().setClassName(HostRuntime.APP_ID, className).putExtra("imageExperimentMetadata", value)
                        val other = HostRuntime.instantiate(instrumentation, otherIntent)
                        foreign += other
                        instrumentation.callActivityOnCreate(other, null)
                        HarnessAssertions.truth(!other.isFinishing && !other.isDestroyed && other.finishCount == 0, "foreign-lifecycle-untouched")
                        if (other is ImageExperimentActivity) {
                            val field = ImageExperimentInstrumentation::class.java.getDeclaredField("refusalDiagnostics").apply { isAccessible = true }
                            HarnessAssertions.truth(other.refusalDiagnostics !== field.get(instrumentation), "foreign-keeper-unbound")
                        }
                    }
                }
                HostRuntime.beforeCreate = { instrumentation, owner, intent ->
                    val otherIntent = Intent().setClassName(HostRuntime.APP_ID, ImageExperimentActivity::class.java.name)
                        .putExtra("imageExperimentMetadata", intent.getStringExtra("imageExperimentMetadata")!!)
                    val other = HostRuntime.instantiate(instrumentation, otherIntent) as ImageExperimentActivity
                    foreign += other
                    instrumentation.callActivityOnCreate(other, null)
                    HarnessAssertions.truth(other.refusalDiagnostics !== (owner as ImageExperimentActivity).refusalDiagnostics,
                        "second-instance-never-replaces-owner")
                }
                HostRuntime.afterCreate = { instrumentation, owner, _ ->
                    val bootstrap = (owner as ImageExperimentActivity).bootstrap
                    instrumentation.callActivityOnCreate(owner, null)
                    HarnessAssertions.truth(owner.bootstrap === bootstrap, "repeated-callback-inert")
                }
            }
            HarnessAssertions.equal(HostRuntime.resultCode, Activity.RESULT_OK, "foreign-channel-cannot-replace-owner")
            HarnessAssertions.equal(HostRuntime.statuses.size, 1, "foreign-no-extra-status")
            launcher.onStart()
            HarnessAssertions.equal(HostRuntime.terminalCount, 1, "duplicate-start-one-terminal")
            for (other in foreign) {
                HarnessAssertions.truth(!other.isFinishing && !other.isDestroyed && other.finishCount == 0, "foreign-cleanup-untouched")
                if (other is ImageExperimentActivity) other.bootstrap?.let {
                    HarnessAssertions.truth(!it.isRevoked, "foreign-bootstrap-not-revoked")
                }
            }
        }
    }

    fun responsiveMain(root: File) {
        HarnessAssertions.case("launcher-responsive-main-finish-ack-before-terminal") {
            launch(File(root, "responsive-finish-ack")) {
                HostRuntime.networkFailure = "/experiment/v1/probe"
                HostRuntime.offMainUiDelay = 80
            }
            refused(ImageExperimentRefusalStage.probe)
            println("FINISH_ACK_ORDER ${HostRuntime.cleanupEvents.joinToString(",")}")
            HarnessAssertions.equal(HostRuntime.cleanupEvents.toList(), listOf("activity-finish", "terminal"),
                "responsive-main-finish-before-terminal")
            HarnessAssertions.truth(HostRuntime.finishOwners.single() === HostRuntime.activity, "finish-ack-owned-only")
        }
    }

    fun blockedMain(root: File) {
        HarnessAssertions.case("launcher-blocked-main-deadline-refuses-before-release") {
            HostRuntime.reset(File(root, "blocked-main"))
            val entered = java.util.concurrent.CountDownLatch(1)
            val release = java.util.concurrent.CountDownLatch(1)
            HostRuntime.createEntered = entered
            HostRuntime.createRelease = release
            val foreign = java.util.concurrent.atomic.AtomicReference<Activity?>()
            HostRuntime.beforeCreate = { launcher, _, _ ->
                val intent = Intent().setClassName(HostRuntime.APP_ID, "android.app.Activity")
                val other = HostRuntime.instantiate(launcher, intent)
                launcher.callActivityOnCreate(other, null)
                foreign.set(other)
            }
            val metadata = HostedFixture.metadata().put("deadline", System.currentTimeMillis() + 500)
            val launcher = ImageExperimentInstrumentation()
            launcher.onCreate(Bundle().apply {
                putString("imageExperimentMetadata", HostedFixture.encode(metadata.toString().toByteArray()))
            })
            val failure = java.util.concurrent.atomic.AtomicReference<Throwable?>()
            val worker = Thread({
                try { launcher.onStart() } catch (caught: Throwable) { failure.set(caught) }
            }, "controlled-launcher-blocked-main")
            worker.start()
            try {
                HarnessAssertions.truth(entered.await(2, java.util.concurrent.TimeUnit.SECONDS), "blocked-main-entered-owned-create")
                val terminalBeforeRelease = HostRuntime.terminalReady.await(1500, java.util.concurrent.TimeUnit.MILLISECONDS)
                println("BLOCKED_MAIN_TERMINAL_COUNT ${HostRuntime.terminalCount}")
                HarnessAssertions.truth(terminalBeforeRelease && HostRuntime.terminalCount == 1 &&
                    HostRuntime.resultCode == Activity.RESULT_CANCELED, "blocked-main-refusal-before-release")
                HarnessAssertions.equal(HostRuntime.statuses.size, 0, "blocked-main-zero-pairing-reports")
                HarnessAssertions.equal(HostRuntime.activity!!.finishCount, 0, "blocked-main-finish-still-queued")
                HarnessAssertions.equal(release.count, 1L, "blocked-main-refusal-precedes-release")
            } finally {
                release.countDown()
                worker.join(3_000)
            }
            HarnessAssertions.truth(!worker.isAlive && failure.get() == null, "blocked-main-launcher-completed")
            HostRuntime.onMain {}
            refused(ImageExperimentRefusalStage.activityStart)
            val owner = HostRuntime.activity!!
            HarnessAssertions.truth(owner.bootstrap!!.isRevoked, "blocked-main-late-bootstrap-revoked")
            HarnessAssertions.equal(owner.finishCount, 1, "blocked-main-owned-finish-once")
            HarnessAssertions.truth(HostRuntime.finishOwners.single() === owner, "blocked-main-finish-owned-only")
            HarnessAssertions.truth(!foreign.get()!!.isFinishing && !foreign.get()!!.isDestroyed &&
                foreign.get()!!.finishCount == 0, "blocked-main-foreign-untouched")
            HarnessAssertions.equal(HostRuntime.requests.size, 0, "blocked-main-no-late-admission")
            launcher.onStart()
            HarnessAssertions.equal(HostRuntime.terminalCount, 1, "blocked-main-one-terminal-after-release")
        }
    }
}
