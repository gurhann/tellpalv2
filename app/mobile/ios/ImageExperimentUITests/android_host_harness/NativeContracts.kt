package com.tellpal.imageexperiment.host

import com.tellpal.imageexperiment.*
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

object NativeContracts {
    fun run(root: File) {
        HarnessAssertions.case("real-jca-json-ledger-self-tests") {
            HostRuntime.reset(File(root, "self-tests"))
            val observed = mutableListOf<ImageExperimentRefusalStage>()
            ImageExperimentHandshakeTests.run { observed += it }
            ImageExperimentHandshakeTests.runLedgerValidation { observed += it }
            ImageExperimentHandshakeTests.run()
            ImageExperimentHandshakeTests.runLedgerValidation()
            HarnessAssertions.equal(observed.size, 0, "successful-self-tests-silent")
            HarnessAssertions.truth(ImageExperimentLedger.store(android.content.Context(), HostRuntime.VALID_LEDGER), "ledger-store")
            val stored = File(HostRuntime.privateRoot, "image_experiment/ledger.json")
            HarnessAssertions.truth(stored.readText().contains("status_request"), "ledger-whitelist-stored")
        }
        HarnessAssertions.case("observer-preserves-exact-original-exception") {
            val original = HostRuntime.failure
            val seen = mutableListOf<ImageExperimentRefusalStage>()
            val failure = runCatching {
                ImageExperimentRefusalObservation.run(ImageExperimentRefusalStage.probe, { seen += it; throw AssertionError("observer") }) {
                    throw original
                }
            }.exceptionOrNull()
            HarnessAssertions.truth(failure === original, "exception-identity")
            HarnessAssertions.equal(seen, listOf(ImageExperimentRefusalStage.probe), "one-passive-observation")
            ImageExperimentRefusalObservation.report(ImageExperimentRefusalStage.probe) { throw AssertionError("observer") }
            HarnessAssertions.equal(ImageExperimentRefusalObservation.run(ImageExperimentRefusalStage.probe) { 37 }, 37, "no-observer-return")
        }
        HarnessAssertions.case("first-only-concurrent-keeper") {
            val keeper = ImageExperimentRefusalDiagnostics()
            val ready = CountDownLatch(24)
            val go = CountDownLatch(1)
            val winners = AtomicInteger()
            val threads = List(24) { index -> Thread {
                ready.countDown(); go.await()
                if (keeper.record(ImageExperimentRefusalStage.entries[index])) winners.incrementAndGet()
            } }
            threads.forEach { it.start() }; ready.await(); go.countDown()
            threads.forEach { it.join(1000); check(!it.isAlive) }
            HarnessAssertions.equal(winners.get(), 1, "one-concurrent-winner")
            val retained = keeper.firstStage
            for (stage in ImageExperimentRefusalStage.entries) HarnessAssertions.truth(!keeper.record(stage), "later-native-refused")
            HarnessAssertions.equal(keeper.firstStage, retained, "first-stage-retained")
        }
        HarnessAssertions.case("bounded-known-dart-report-only") {
            val invalid = listOf(null, 1, true, emptyMap<String, String>(), "", "unknown", "NATIVE_METADATA",
                "DART_MODE\r", "DART_MODE\n", "DART_MODE\u0000", "x".repeat(65), HostRuntime.failure.message)
            for (value in invalid) {
                val keeper = ImageExperimentRefusalDiagnostics()
                HarnessAssertions.equal(keeper.reportDart(value), false, "invalid-report-false")
                HarnessAssertions.equal(keeper.firstStage, null, "invalid-report-silent")
            }
            for (stage in ImageExperimentRefusalStage.entries.filter { it.code.startsWith("DART_") }) {
                val keeper = ImageExperimentRefusalDiagnostics()
                HarnessAssertions.truth(stage.code.length <= 64 && Regex("[A-Z0-9_]+").matches(stage.code), "fixed-code")
                HarnessAssertions.truth(keeper.reportDart(stage.code), "known-report")
                HarnessAssertions.equal(keeper.firstStage, stage, "known-retained")
                HarnessAssertions.truth(keeper.reportDart("DART_MODE"), "later-valid-report-accepted")
                HarnessAssertions.equal(keeper.firstStage, stage, "later-dart-does-not-replace")
            }
        }
        HarnessAssertions.case("real-constructor-noop-and-broken-observer") {
            HostRuntime.reset(File(root, "constructors"))
            val normal = ImageExperimentBootstrap(HostedFixture.metadata().toString(), HostRuntime.APP_ID)
            val observed = mutableListOf<ImageExperimentRefusalStage>()
            val watched = ImageExperimentBootstrap(HostedFixture.metadata().toString(), HostRuntime.APP_ID) { observed += it }
            HarnessAssertions.equal(observed.size, 0, "constructor-success-silent")
            HarnessAssertions.truth(normal.publicKeySpkiBase64.isNotEmpty() && watched.publicKeySpkiBase64.isNotEmpty(), "real-key-generation")
            normal.revoke(); watched.revoke()
            val failure = runCatching {
                ImageExperimentBootstrap("{}", HostRuntime.APP_ID) { observed += it; throw AssertionError("observer") }
            }.exceptionOrNull()
            HarnessAssertions.truth(failure is IllegalArgumentException, "constructor-original-refusal")
            HarnessAssertions.equal(observed.last(), ImageExperimentRefusalStage.metadata, "constructor-specific-stage")
        }
        HarnessAssertions.case("self-test-failure-observer-cannot-replace") {
            HostRuntime.failSharedVector = true
            val failure = runCatching { ImageExperimentHandshakeTests.run { throw AssertionError("observer") } }.exceptionOrNull()
            HostRuntime.failSharedVector = false
            HarnessAssertions.truth(failure === HostRuntime.failure, "self-test-exception-identity")
        }
        HarnessAssertions.case("async-dispatch-refusal-and-once-only-gate") {
            val observed = mutableListOf<ImageExperimentRefusalStage>()
            val bootstrap = ImageExperimentBootstrap(HostedFixture.metadata().toString(), HostRuntime.APP_ID) { observed += it }
            val executor = ImageExperimentBootstrap::class.java.getDeclaredField("executor").apply { isAccessible = true }
                .get(bootstrap) as java.util.concurrent.ExecutorService
            executor.shutdownNow()
            var callbacks = 0
            bootstrap.takeSession { HarnessAssertions.equal(it, null, "rejected-dispatch-null"); callbacks++ }
            bootstrap.takeSession { HarnessAssertions.equal(it, null, "duplicate-take-null"); callbacks++ }
            HarnessAssertions.equal(observed.first(), ImageExperimentRefusalStage.dispatch, "dispatch-stage")
            HarnessAssertions.equal(observed.last(), ImageExperimentRefusalStage.handoff, "duplicate-original-gate-stage")
            HarnessAssertions.equal(callbacks, 2, "each-original-callback-once")
            HarnessAssertions.equal(HostRuntime.requests.size, 0, "no-request-after-dispatch-failure")
            bootstrap.revoke(); bootstrap.revoke()
            HarnessAssertions.truth(bootstrap.isRevoked, "once-only-revoked")
        }
        HarnessAssertions.case("async-broken-observer-preserves-cleanup-request-and-callback-count") {
            HostRuntime.reset(File(root, "async-observer"))
            HostRuntime.networkFailure = "/experiment/v1/probe"
            val observed = java.util.concurrent.CopyOnWriteArrayList<ImageExperimentRefusalStage>()
            val bootstrap = ImageExperimentBootstrap(HostedFixture.metadata().toString(), HostRuntime.APP_ID) {
                observed += it; throw AssertionError("observer")
            }
            val completed = CountDownLatch(1)
            val callbackCount = AtomicInteger()
            bootstrap.takeSession {
                HarnessAssertions.equal(it, null, "async-refusal-original-null")
                callbackCount.incrementAndGet(); completed.countDown()
            }
            HarnessAssertions.truth(completed.await(5, java.util.concurrent.TimeUnit.SECONDS), "async-refusal-callback-completes")
            HarnessAssertions.truth(bootstrap.isRevoked && !bootstrap.launcherOutcome.isSettled(), "async-observer-original-revocation")
            HarnessAssertions.equal(callbackCount.get(), 1, "async-observer-one-callback")
            HarnessAssertions.equal(observed.toList(), listOf(ImageExperimentRefusalStage.probe), "async-observer-specific-stage")
            HarnessAssertions.equal(HostRuntime.requests.size, 1, "async-observer-one-original-request")
            HarnessAssertions.truth(HostRuntime.requests.single().disconnectCount > 0, "async-observer-original-disconnect")
            val key = ImageExperimentBootstrap::class.java.getDeclaredField("key").apply { isAccessible = true }
            HarnessAssertions.equal(key.get(bootstrap), null, "async-observer-key-cleared")
            bootstrap.revoke()
            HarnessAssertions.equal(callbackCount.get(), 1, "async-observer-once-only-revoke")
        }
    }
}
