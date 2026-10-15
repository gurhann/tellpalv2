package com.tellpal.imageexperiment

import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/** Experiment-only channel. Public launcher inputs never install an SDK auth session. */
class ImageExperimentActivity : FlutterActivity() {
    private val ledgerExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val memoryExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val memoryBusy = java.util.concurrent.atomic.AtomicBoolean(false)
    private var admittedExpiry = 0L
    private var comparisonAdmitted = false
    private var qualityAdmitted = false
    private var expectedQualitySession = ""
    private var expectedQualityCase = ""
    private var expectedQualityGeneration = -1L
    private val qualityLifetime = ImageExperimentQualityLifetime()
    private var qualityBinding: Map<String, Any?>? = null
    private val qualityHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val expireQuality = Runnable { clearQualityBinding() }
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
                                    admitMeasurement(value)
                                    owned.launcherOutcome.markSessionHandedOff()
                                    result.success(value)
                                }
                            }
                        }
                    }
                    "revoke" -> {
                        clearQualityBinding()
                        val arguments = call.arguments as? Map<*, *>
                        val requestedResult = if (arguments?.keys == setOf("result")) arguments["result"] as? String else null
                        bootstrap?.let { it.launcherOutcome.complete(requestedResult, it.isRevoked) }
                        ledgerOnce.set(true)
                        ledgerExecutor.shutdownNow()
                        memoryExecutor.shutdownNow()
                        bootstrap?.revoke()
                        result.success(null)
                        finish()
                    }
                    "sampleMemory" -> {
                        if (call.arguments != null || !measurementActive() || !comparisonAdmitted ||
                            !memoryBusy.compareAndSet(false, true)) result.error("MEMORY_REFUSED", null, null)
                        else memoryExecutor.execute {
                            val sample = ImageExperimentMemory.sample()
                            runOnUiThread {
                                memoryBusy.set(false)
                                if (!measurementActive()) result.error("MEMORY_REFUSED", null, null)
                                else result.success(sample)
                            }
                        }
                    }
                    "clearQualityBinding" -> {
                        if (call.arguments != null) result.success(false)
                        else { clearQualityBinding(); result.success(true) }
                    }
                    "publishQualityBinding" -> {
                        val row = call.arguments as? Map<*, *>
                        if (row == null || !measurementActive() || !qualityAdmitted || !validQualityBinding(row) ||
                            !hasWindowFocus() || resources.configuration.orientation != android.content.res.Configuration.ORIENTATION_PORTRAIT)
                            result.success(false)
                        else {
                            val root = findViewById<android.view.ViewGroup>(android.R.id.content)
                            if (root.findViewWithTag<android.view.View>("imageExperimentQualityBinding") != null ||
                                !qualityLifetime.begin(admittedExpiry)) result.success(false)
                            else {
                                val element = android.view.View(this)
                                element.tag = "imageExperimentQualityBinding"
                                element.importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
                                @Suppress("UNCHECKED_CAST")
                                val published = (row as Map<String, Any?>) + mapOf("phase" to "hold", "orientation" to "portrait",
                                    "nativeContext" to "foreground_activity", "holdDeadlineEpochMilliseconds" to qualityLifetime.deadline)
                                qualityBinding = published
                                element.contentDescription = "IMAGE_EXPERIMENT_QUALITY_BINDING " +
                                    android.util.Base64.encodeToString(org.json.JSONObject(published).toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                                root.addView(element, android.view.ViewGroup.LayoutParams(1, 1))
                                qualityHandler.postDelayed(expireQuality, maxOf(1, qualityLifetime.deadline - System.currentTimeMillis()))
                                result.success(true)
                            }
                        }
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

    private fun clearQualityBinding() {
        qualityLifetime.invalidate()
        qualityHandler.removeCallbacks(expireQuality)
        qualityBinding = null
        val root = findViewById<android.view.ViewGroup>(android.R.id.content)
        root?.findViewWithTag<android.view.View>("imageExperimentQualityBinding")?.let { root.removeView(it) }
    }
    // Called on the UI thread by the same-signed, exact-Activity instrumentation owner.
    fun readQualityBinding(): Map<String, Any?>? {
        if (!measurementActive() || !qualityLifetime.isActive() || isFinishing || isDestroyed || !hasWindowFocus() ||
            resources.configuration.orientation != android.content.res.Configuration.ORIENTATION_PORTRAIT) return null
        val root = findViewById<android.view.ViewGroup>(android.R.id.content)
        if (root.findViewWithTag<android.view.View>("imageExperimentQualityBinding") == null) return null
        return qualityBinding?.plus("nativeObservedAtEpochMilliseconds" to System.currentTimeMillis())
    }
    override fun onPause() { clearQualityBinding(); super.onPause() }

    private fun measurementActive() = bootstrap != null && bootstrap?.isRevoked == false &&
        System.currentTimeMillis() < admittedExpiry
    private fun admitMeasurement(value: Map<String, Any>) {
        try {
            if (!packageName.endsWith(".release")) return
            val envelope = ImageExperimentJson.parseObject(value.getValue("envelope") as String)
            val batch = envelope.getString("batchId")
            if (batch !in setOf("native_comparison_r1", "native_quality_r1")) return
            val manifest = ImageExperimentJson.parseObject(String(android.util.Base64.decode(
                envelope.getString("publicManifestBase64"), android.util.Base64.NO_WRAP), Charsets.UTF_8))
            val metadata = value.getValue("metadata") as Map<*, *>
            val selected = metadata["caseId"] as String
            val entries = manifest.getJSONArray("entries")
            val entry = (0 until entries.length()).map { entries.getJSONObject(it) }
                .single { it.getString("caseId") == selected }
            if (entry.getString("purposeAcceptanceReceipt") != batch) return
            admittedExpiry = envelope.getLong("expiresAt")
            comparisonAdmitted = batch == "native_comparison_r1"
            qualityAdmitted = batch == "native_quality_r1"
            fun digest(text: String) = java.security.MessageDigest.getInstance("SHA-256")
                .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            expectedQualitySession = digest(envelope.getString("sessionId"))
            expectedQualityCase = digest(selected)
            expectedQualityGeneration = envelope.getLong("generation")
        } catch (_: Exception) { comparisonAdmitted = false; qualityAdmitted = false }
    }
    private fun validQualityBinding(row: Map<*, *>): Boolean = runCatching {
        require(row.keys == setOf("caseSha256", "providerSha256", "attemptSha256", "sessionSha256", "generation",
            "observedSlotWidth", "observedSlotHeight", "observedDpr", "actualDecodeWidth", "actualDecodeHeight"))
        require(row["sessionSha256"] == expectedQualitySession && row["caseSha256"] == expectedQualityCase &&
            (row["generation"] as? Number)?.toLong() == expectedQualityGeneration)
        ImageExperimentLedger.validate(org.json.JSONObject(mapOf("schemaVersion" to 1,
            "records" to listOf(row), "droppedCount" to 0)).toString())
        require(listOf("observedSlotWidth", "observedSlotHeight", "observedDpr", "actualDecodeWidth", "actualDecodeHeight")
            .all { (row[it] as? Number)?.toDouble()?.let { number -> number > 0 } == true })
        true
    }.getOrDefault(false)

    override fun onDestroy() {
        clearQualityBinding()
        bootstrap?.revoke()
        bootstrap = null
        ledgerExecutor.shutdownNow()
        memoryExecutor.shutdownNow()
        super.onDestroy()
    }
}
