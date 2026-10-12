package com.tellpal.imageexperiment

import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/** Experiment-only channel. Public launcher inputs never install an SDK auth session. */
class ImageExperimentActivity : FlutterActivity() {
    private val ledgerExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val ledgerOnce = java.util.concurrent.atomic.AtomicBoolean(false)
    var refusalDiagnostics = ImageExperimentRefusalDiagnostics()
        private set
    var bootstrap: ImageExperimentBootstrap? = null
        private set

    // Instrumentation binds only its newly created owned Activity before onCreate.
    fun attachLauncherDiagnostics(diagnostics: ImageExperimentRefusalDiagnostics) {
        refusalDiagnostics = diagnostics
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        bootstrap = try {
            ImageExperimentRefusalObservation.run(ImageExperimentRefusalStage.activityBootstrap,
                { refusalDiagnostics.record(it) }) {
                ImageExperimentBootstrap(intent.getStringExtra("imageExperimentMetadata"), packageName,
                    observeRefusal = { refusalDiagnostics.record(it) })
            }
        } catch (_: Exception) {
            null
        }
        intent.removeExtra("imageExperimentMetadata")
        super.onCreate(savedInstanceState)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "tellpal/image_experiment")
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "reportDiagnostic" -> {
                        val accepted = refusalDiagnostics.reportDart(call.arguments)
                        runCatching { result.success(accepted) }
                    }
                    "takeSession" -> {
                        val owned = bootstrap
                        if (owned == null) result.error("NATIVE_BOOTSTRAP_REFUSED", null, null)
                        else owned.takeSession { value ->
                            runOnUiThread {
                                if (value == null) result.error("NATIVE_BOOTSTRAP_REFUSED", null, null)
                                else {
                                    owned.launcherOutcome.markSessionHandedOff()
                                    result.success(value)
                                }
                            }
                        }
                    }
                    "revoke" -> {
                        val arguments = call.arguments as? Map<*, *>
                        val requestedResult = if (arguments?.keys == setOf("result")) arguments["result"] as? String else null
                        bootstrap?.let { it.launcherOutcome.complete(requestedResult, it.isRevoked) }
                        ledgerOnce.set(true)
                        ledgerExecutor.shutdownNow()
                        bootstrap?.revoke()
                        result.success(null)
                        finish()
                    }
                    "storeLedger" -> {
                        val raw = call.arguments as? String
                        if (raw == null || !ledgerOnce.compareAndSet(false, true)) result.success(false)
                        else ledgerExecutor.execute {
                            val stored = ImageExperimentLedger.store(this, raw)
                            runOnUiThread {
                                if (stored) bootstrap?.launcherOutcome?.markLedgerStored()
                                result.success(stored)
                            }
                        }
                    }
                    else -> result.notImplemented()
                }
            }
    }

    override fun onDestroy() {
        bootstrap?.revoke()
        bootstrap = null
        ledgerExecutor.shutdownNow()
        super.onDestroy()
    }
}
