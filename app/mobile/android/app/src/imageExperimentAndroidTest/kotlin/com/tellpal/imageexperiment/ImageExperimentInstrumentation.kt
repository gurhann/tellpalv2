package com.tellpal.imageexperiment

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Base64

/** Same-signed launcher reports only the ephemeral public key, never the envelope. */
class ImageExperimentInstrumentation : Instrumentation() {
    private var arguments = Bundle()
    private val refusalDiagnostics = ImageExperimentRefusalDiagnostics()
    private val creationLock = Object()
    private val started = java.util.concurrent.atomic.AtomicBoolean(false)
    private var launchMetadata: String? = null
    private var ownedActivity: ImageExperimentActivity? = null
    private var createdBootstrap: ImageExperimentBootstrap? = null
    private var creationComplete = false
    private var creationClaimed = false
    private var creationFailed = false
    private var admissionClosed = true

    override fun newActivity(cl: ClassLoader, className: String, intent: Intent): Activity =
        super.newActivity(cl, className, intent).also {
            synchronized(creationLock) {
                if (it.javaClass == ImageExperimentActivity::class.java &&
                    className == ImageExperimentActivity::class.java.name && launchMetadata != null &&
                    intent.getStringExtra("imageExperimentMetadata") == launchMetadata && ownedActivity == null) {
                    ownedActivity = it as ImageExperimentActivity
                    it.attachLauncherDiagnostics(refusalDiagnostics)
                }
            }
        }

    override fun callActivityOnCreate(activity: Activity, savedInstanceState: Bundle?) {
        synchronized(creationLock) {
            if (activity === ownedActivity) {
                if (creationClaimed) return
                creationClaimed = true
            }
        }
        var failed = true
        try {
            super.callActivityOnCreate(activity, savedInstanceState)
            failed = false
        } finally {
            val cleanup = synchronized(creationLock) {
                if (activity !== ownedActivity || creationComplete) false
                else {
                    createdBootstrap = ownedActivity?.bootstrap
                    creationFailed = failed
                    creationComplete = true
                    creationLock.notifyAll()
                    admissionClosed
                }
            }
            if (cleanup) runCatching { cleanupOwnedActivity() }
        }
    }

    private fun cleanupOwnedActivity() {
        val owned = synchronized(creationLock) { ownedActivity } ?: return
        owned.bootstrap?.revoke()
        val finishCompleted = java.util.concurrent.CountDownLatch(1)
        owned.runOnUiThread {
            try {
                if (!owned.isFinishing && !owned.isDestroyed) owned.finish()
            } finally {
                finishCompleted.countDown()
            }
        }
        // Give a responsive main thread time to finish before instrumentation terminates.
        finishCompleted.await(250, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    private fun exportLedger() {
            val ledgerFile = java.io.File(targetContext.noBackupFilesDir, "image_experiment/ledger.json")
            require(ledgerFile.canonicalFile.parentFile == java.io.File(targetContext.noBackupFilesDir, "image_experiment").canonicalFile)
            require(android.system.Os.lstat(ledgerFile.path).st_mode and android.system.OsConstants.S_IFMT == android.system.OsConstants.S_IFREG)
            require(ledgerFile.length() in 1..262_144)
            val ledger = ImageExperimentLedger.validate(ledgerFile.readText(Charsets.UTF_8))
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(ledger).joinToString("") { "%02x".format(it) }
            val chunks = (ledger.size + 3071) / 3072
            for (index in 0 until chunks) sendStatus(2, Bundle().apply {
                putString("event", "ledger-chunk")
                putString("index", index.toString())
                putString("count", chunks.toString())
                putString("ledgerSha256", digest)
                putString("payloadBase64", Base64.encodeToString(ledger.copyOfRange(index * 3072,
                    minOf((index + 1) * 3072, ledger.size)), Base64.NO_WRAP))
            })
    }

    private fun reportQualityBinding(activity: ImageExperimentActivity) {
        val completed = java.util.concurrent.CountDownLatch(1)
        var snapshot: Map<String, Any?>? = null
        activity.runOnUiThread {
            try { snapshot = activity.readQualityBinding() }
            finally { completed.countDown() }
        }
        require(completed.await(150, java.util.concurrent.TimeUnit.MILLISECONDS))
        val encoded = org.json.JSONObject.wrap(snapshot).toString().toByteArray(Charsets.UTF_8)
        require(encoded.size <= 4096)
        sendStatus(3, Bundle().apply {
            putString("event", "quality-binding")
            putString("bindingBase64", Base64.encodeToString(encoded, Base64.NO_WRAP))
        })
    }

    private fun <T> readerMain(activity: ImageExperimentActivity, deadline: Long, operation: () -> T): T {
        require(SystemClock.elapsedRealtime() < deadline)
        val completed = java.util.concurrent.CountDownLatch(1)
        val active = java.util.concurrent.atomic.AtomicBoolean(true)
        var value: T? = null
        var failure: Throwable? = null
        activity.runOnUiThread {
            try {
                require(active.get() && SystemClock.elapsedRealtime() < deadline && !activity.isFinishing && !activity.isDestroyed &&
                    activity.bootstrap?.isRevoked == false)
                value = operation()
            } catch (error: Throwable) { failure = error }
            finally { completed.countDown() }
        }
        val finished = completed.await(minOf(500, maxOf(1, deadline - SystemClock.elapsedRealtime())), java.util.concurrent.TimeUnit.MILLISECONDS)
        active.set(false)
        require(finished)
        failure?.let { throw IllegalStateException("READER_REFUSED") }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    private fun exerciseReaderBackground(activity: ImageExperimentActivity, deadline: Long) {
        val before = readerMain(activity, deadline) { activity.readReaderLifecycleForLauncher() } ?: error("READER_REFUSED")
        require(readerMain(activity, deadline) { activity.moveTaskToBack(true) })
        fun awaitLifecycle(predicate: (Map<String, Any?>) -> Boolean) {
            val limit = minOf(deadline, SystemClock.elapsedRealtime() + 3000)
            while (SystemClock.elapsedRealtime() < limit) {
                val row = readerMain(activity, deadline) { activity.readReaderLifecycleForLauncher() } ?: error("READER_REFUSED")
                if (predicate(row)) return
                Thread.sleep(20)
            }
            error("READER_REFUSED")
        }
        awaitLifecycle { it["backgrounded"] == true && (it["backgroundCount"] as Int) > (before["backgroundCount"] as Int) }
        require(deadline - SystemClock.elapsedRealtime() > 750)
        Thread.sleep(500)
        readerMain(activity, deadline) {
            activity.startActivity(Intent().setClassName(targetContext.packageName,
                "com.tellpal.imageexperiment.ImageExperimentActivity")
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        awaitLifecycle { it["backgrounded"] == false && (it["returnCount"] as Int) > (before["returnCount"] as Int) }
        require(synchronized(creationLock) { ownedActivity === activity } && !activity.isFinishing && !activity.isDestroyed)
    }

    override fun onCreate(arguments: Bundle?) {
        this.arguments = arguments ?: Bundle()
        start()
    }

    override fun onStart() {
        if (!started.compareAndSet(false, true)) return
        val invocationStart = SystemClock.elapsedRealtime()
        val result = Bundle()
        val observeRefusal: (ImageExperimentRefusalStage) -> Unit = { refusalDiagnostics.record(it) }
        var stage = ImageExperimentRefusalStage.signingIdentity
        try {
            require(context.packageManager.checkSignatures(context.packageName, targetContext.packageName) ==
                PackageManager.SIGNATURE_MATCH)
            stage = ImageExperimentRefusalStage.selfTest
            ImageExperimentHandshakeTests.run(observeRefusal)
            ImageExperimentHandshakeTests.runLedgerValidation(observeRefusal)
            stage = ImageExperimentRefusalStage.launcherMetadata
            val raw = arguments.getString("imageExperimentMetadata") ?: error("REFUSED")
            require(raw.length <= 24_000)
            val metadata = String(Base64.decode(raw, Base64.NO_WRAP), Charsets.UTF_8)
            val intent = Intent().setClassName(targetContext.packageName,
                "com.tellpal.imageexperiment.ImageExperimentActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("imageExperimentMetadata", metadata)
            stage = ImageExperimentRefusalStage.activityStart
            val launchStart = SystemClock.elapsedRealtime()
            // Reading the deadline never replaces the bootstrap's original metadata validator.
            val metadataDeadline = runCatching {
                val parsed = ImageExperimentJson.parseObject(metadata, 16_384)
                val deadline = parsed.get("deadline")
                require(deadline is Long || deadline is Int)
                parsed.getLong("deadline").also { require(it - System.currentTimeMillis() in 1..30_000) }
            }.getOrNull()
            fun remaining(): Long = minOf(30_000 - (SystemClock.elapsedRealtime() - launchStart),
                metadataDeadline?.let { it - System.currentTimeMillis() } ?: 30_000)
            synchronized(creationLock) {
                launchMetadata = metadata
                admissionClosed = false
            }
            targetContext.startActivity(intent)
            val activity = synchronized(creationLock) {
                while (!creationComplete && remaining() > 0) creationLock.wait(minOf(20, remaining()).coerceAtLeast(1))
                require(creationComplete && !creationFailed && !admissionClosed && remaining() > 0)
                ownedActivity ?: error("REFUSED")
            }
            stage = ImageExperimentRefusalStage.activityBootstrap
            val bootstrap = synchronized(creationLock) { createdBootstrap } ?: error("REFUSED")
            require(!activity.isFinishing && !activity.isDestroyed)
            stage = ImageExperimentRefusalStage.reachability
            while (!bootstrap.isReachabilityQualified && !bootstrap.isRevoked &&
                !activity.isFinishing && !activity.isDestroyed && remaining() > 0) Thread.sleep(20)
            require(bootstrap.isReachabilityQualified && !bootstrap.isRevoked &&
                !activity.isFinishing && !activity.isDestroyed && remaining() > 0)
            stage = ImageExperimentRefusalStage.pairingReport
            val parsed = ImageExperimentJson.parseObject(metadata, 16_384)
            val reader = parsed.getString("batchId") in ImageExperimentReaderPolicy.batches
            val readerPrimary = parsed.getString("batchId") in ImageExperimentReaderPolicy.primaryBatches
            synchronized(creationLock) {
                require(!admissionClosed && remaining() > 0)
                sendStatus(1, Bundle().apply {
                    putString("event", "pairing-key")
                    putString("sessionId", parsed.getString("sessionId"))
                    putString("launchNonce", parsed.getString("launchNonce"))
                    putString("publicKeySpkiBase64", bootstrap.publicKeySpkiBase64)
                })
            }
            stage = ImageExperimentRefusalStage.settlement
            val start = if (reader) invocationStart else SystemClock.elapsedRealtime()
            val nativeDeadline = start + 180_000
            var readerBackgroundCompleted = false
            var deviceFactsReported = false
            while (!bootstrap.isRevoked && !activity.isFinishing && !activity.isDestroyed &&
                SystemClock.elapsedRealtime() - start < 180_000) {
                if (arguments.getString("imageExperimentQualityCapture") == "1") reportQualityBinding(activity)
                if (reader && !deviceFactsReported) {
                    val facts = readerMain(activity, nativeDeadline) { activity.readDeviceFactsForLauncher() }
                    if (facts != null) {
                        require(facts.size in 1..4096)
                        sendStatus(4, Bundle().apply {
                            putString("event", "device-facts")
                            putString("reportBase64", Base64.encodeToString(facts, Base64.NO_WRAP))
                            putString("reportSha256", ImageExperimentBootstrap.sha256(facts))
                        })
                        deviceFactsReported = true
                    }
                }
                if (readerPrimary && !readerBackgroundCompleted && readerMain(activity, nativeDeadline) { activity.consumeReaderBackgroundRequest() }) {
                    exerciseReaderBackground(activity, nativeDeadline)
                    readerBackgroundCompleted = true
                }
                Thread.sleep(100)
            }
            synchronized(creationLock) { admissionClosed = true }
            require(!reader || SystemClock.elapsedRealtime() < nativeDeadline)
            require(!readerPrimary || readerBackgroundCompleted)
            require(bootstrap.isRevoked && bootstrap.launcherOutcome.isSettled())
            exportLedger()
            result.putString("reason", "NATIVE_LAUNCHER_SETTLED")
            finish(Activity.RESULT_OK, result)
        } catch (_: Exception) {
            ImageExperimentRefusalObservation.report(stage, observeRefusal)
            synchronized(creationLock) { admissionClosed = true; creationLock.notifyAll() }
            runCatching { cleanupOwnedActivity() }
            runCatching { exportLedger() }
            result.putString("reason", "NATIVE_LAUNCHER_REFUSED")
            runCatching { refusalDiagnostics.firstStage?.let { result.putString("diagnosticStage", it.code) } }
            finish(Activity.RESULT_CANCELED, result)
        }
    }
}
