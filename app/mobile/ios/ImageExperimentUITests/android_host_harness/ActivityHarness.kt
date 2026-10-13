package com.tellpal.imageexperiment.host

import android.content.Intent
import com.tellpal.imageexperiment.*
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.File

object ActivityHarness {
    private fun create(root: File, metadata: String): ImageExperimentActivity {
        HostRuntime.reset(root)
        val activity = ImageExperimentActivity()
        HostRuntime.activity = activity
        HostRuntime.onMain {
            activity.intent = Intent().putExtra("imageExperimentMetadata", metadata)
            activity.onCreate(null)
            activity.configureFlutterEngine(FlutterEngine())
        }
        return activity
    }

    fun run(root: File, dartProducer: File) {
        HarnessAssertions.case("actual-dart-producer-all-47-codes-accepted-by-activity-channel") {
            val declaration = Regex("enum\\s+ImageExperimentFixtureStage\\s*\\{([\\s\\S]*?)\\n\\}")
                .find(dartProducer.readText())?.groupValues?.get(1)
            HarnessAssertions.truth(declaration != null, "independent-dart-producer-enum-present")
            val codes = Regex("\\(\\s*'([^']*)'\\s*\\)").findAll(declaration!!.substringBefore(';'))
                .map { it.groupValues[1] }.toList()
            HarnessAssertions.equal(codes.size, 47, "independent-dart-producer-count")
            HarnessAssertions.equal(codes.toSet().size, 47, "independent-dart-producer-distinct")
            for ((index, code) in codes.withIndex()) {
                val activity = create(File(root, "dart-producer-$index"), HostedFixture.metadata().toString())
                HarnessAssertions.equal(HostRuntime.invoke("reportDiagnostic", code).await(), true, "actual-dart-producer-channel-accepts-code")
                HarnessAssertions.equal(activity.refusalDiagnostics.firstStage?.code, code, "actual-dart-producer-code-retained")
                val laterCode = codes[(index + 1) % codes.size]
                HarnessAssertions.equal(HostRuntime.invoke("reportDiagnostic", laterCode).await(), true, "actual-dart-producer-later-code-accepted")
                HarnessAssertions.equal(activity.refusalDiagnostics.firstStage?.code, code, "actual-dart-producer-later-cannot-replace")
            }
        }
        HarnessAssertions.case("actual-activity-strict-channel-and-session-neutrality") {
            val activity = create(File(root, "activity-channel"), HostedFixture.metadata().toString())
            val bootstrap = activity.bootstrap!!
            val key = bootstrap.publicKeySpkiBase64
            val invalid = listOf(null, 7, true, mapOf("stage" to "DART_MODE"), "", "UNKNOWN",
                "NATIVE_PROBE", "DART_MODE\n", "DART_MODE\r", "DART_MODE\u0000", "x".repeat(65), HostRuntime.failure.message)
            for (value in invalid) HarnessAssertions.equal(HostRuntime.invoke("reportDiagnostic", value).await(), false, "channel-invalid-report")
            HarnessAssertions.equal(activity.refusalDiagnostics.firstStage, null, "channel-invalid-silent")
            HarnessAssertions.equal(HostRuntime.invoke("reportDiagnostic", "DART_MODE").await(), true, "channel-known-report")
            HarnessAssertions.equal(HostRuntime.invoke("reportDiagnostic", "DART_CONFIGURATION").await(), true, "channel-later-known")
            HarnessAssertions.equal(activity.refusalDiagnostics.firstStage, ImageExperimentRefusalStage.dartMode, "channel-first-only")
            HarnessAssertions.truth(!bootstrap.isRevoked && !bootstrap.isReachabilityQualified && !bootstrap.launcherOutcome.isSettled(), "diagnostic-no-admission")
            HarnessAssertions.equal(bootstrap.publicKeySpkiBase64, key, "diagnostic-no-key-change")
            HarnessAssertions.equal(HostRuntime.requests.size, 0, "diagnostic-no-requests")
            HarnessAssertions.equal(activity.intent.getStringExtra("imageExperimentMetadata"), null, "metadata-removed")
            HarnessAssertions.truth(HostRuntime.invoke("unknownMethod", null).apply { await() }.unimplemented, "unknown-method")
        }
        HarnessAssertions.case("actual-activity-first-native-beats-dart-and-null-bootstrap") {
            val activity = create(File(root, "activity-refused"), "{}")
            HarnessAssertions.equal(activity.bootstrap, null, "constructor-to-null-preserved")
            HarnessAssertions.equal(activity.refusalDiagnostics.firstStage, ImageExperimentRefusalStage.metadata, "native-inner-stage-first")
            HarnessAssertions.equal(HostRuntime.invoke("reportDiagnostic", "DART_MODE").await(), true, "late-valid-report")
            HarnessAssertions.equal(activity.refusalDiagnostics.firstStage, ImageExperimentRefusalStage.metadata, "native-stage-survives")
            val session = HostRuntime.invoke("takeSession", null).apply { await() }
            HarnessAssertions.equal(session.error, "NATIVE_BOOTSTRAP_REFUSED", "null-bootstrap-original-error")
        }
        HarnessAssertions.case("actual-activity-broken-report-output-does-not-change-session") {
            val activity = create(File(root, "activity-broken-output"), HostedFixture.metadata().toString())
            val bootstrap = activity.bootstrap!!
            val key = bootstrap.publicKeySpkiBase64
            HostRuntime.onMain {
                HostRuntime.handler!!(MethodCall("reportDiagnostic", "DART_MODE"), object : MethodChannel.Result {
                    override fun success(result: Any?) {
                        HarnessAssertions.equal(result, true, "broken-output-fixed-boolean")
                        throw HostRuntime.failure
                    }
                    override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) = error("Unexpected result")
                    override fun notImplemented() = error("Unexpected result")
                })
            }
            HarnessAssertions.equal(activity.refusalDiagnostics.firstStage, ImageExperimentRefusalStage.dartMode, "broken-output-fixed-stage")
            HarnessAssertions.truth(!bootstrap.isRevoked && !bootstrap.isReachabilityQualified && !bootstrap.launcherOutcome.isSettled(), "broken-output-no-admission")
            HarnessAssertions.equal(bootstrap.publicKeySpkiBase64, key, "broken-output-no-key-change")
            HarnessAssertions.equal(HostRuntime.requests.size, 0, "broken-output-no-requests")
            HarnessAssertions.truth(!activity.isFinishing && activity.finishCount == 0, "broken-output-no-teardown")
        }
        HarnessAssertions.case("actual-activity-ledger-and-teardown-once") {
            val activity = create(File(root, "activity-ledger"), HostedFixture.metadata().toString())
            val bootstrap = activity.bootstrap!!
            HarnessAssertions.equal(HostRuntime.invoke("storeLedger", HostRuntime.VALID_LEDGER).await(), true, "actual-ledger-stored")
            HarnessAssertions.equal(HostRuntime.invoke("storeLedger", HostRuntime.VALID_LEDGER).await(), false, "ledger-once-only")
            HostRuntime.invoke("revoke", mapOf("result" to "settled", "extra" to "SYNTHETIC")).await()
            HarnessAssertions.truth(bootstrap.isRevoked && !bootstrap.launcherOutcome.isSettled(), "malformed-result-refuses-settlement")
            HarnessAssertions.truth(activity.isFinishing && activity.finishCount == 1, "owned-finish-once")
            HostRuntime.onMain { activity.onDestroy() }
            HarnessAssertions.equal(activity.bootstrap, null, "destroy-clears-bootstrap")
            HarnessAssertions.truth(activity.isDestroyed, "destroy-calls-super")
        }
        HarnessAssertions.case("new-activity-owned-binding-before-constructor") {
            HostRuntime.reset(File(root, "activity-binding"))
            val launcher = ImageExperimentInstrumentation()
            val field = ImageExperimentInstrumentation::class.java.getDeclaredField("refusalDiagnostics").apply { isAccessible = true }
            HostRuntime.beforeCreate = { _, activity, _ ->
                HarnessAssertions.truth((activity as ImageExperimentActivity).refusalDiagnostics === field.get(launcher), "keeper-shared-before-onCreate")
            }
            launcher.onCreate(android.os.Bundle().apply { putString("imageExperimentMetadata", HostedFixture.encode("{}".toByteArray())) })
            launcher.onStart()
            HarnessAssertions.equal((field.get(launcher) as ImageExperimentRefusalDiagnostics).firstStage,
                ImageExperimentRefusalStage.metadata, "bound-constructor-refusal")
        }
    }
}
