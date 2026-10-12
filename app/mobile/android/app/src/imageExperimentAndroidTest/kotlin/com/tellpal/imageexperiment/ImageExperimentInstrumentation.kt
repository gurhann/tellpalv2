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

    override fun newActivity(cl: ClassLoader, className: String, intent: Intent): Activity =
        super.newActivity(cl, className, intent).also {
            if (it is ImageExperimentActivity) it.attachLauncherDiagnostics(refusalDiagnostics)
        }

    override fun onCreate(arguments: Bundle?) {
        this.arguments = arguments ?: Bundle()
        start()
    }

    override fun onStart() {
        var activity: ImageExperimentActivity? = null
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
            activity = startActivitySync(intent) as ImageExperimentActivity
            stage = ImageExperimentRefusalStage.activityBootstrap
            val bootstrap = activity.bootstrap ?: error("REFUSED")
            stage = ImageExperimentRefusalStage.reachability
            val pairingStart = SystemClock.elapsedRealtime()
            while (!bootstrap.isReachabilityQualified && !bootstrap.isRevoked &&
                SystemClock.elapsedRealtime() - pairingStart < 30_000) Thread.sleep(20)
            require(bootstrap.isReachabilityQualified && !bootstrap.isRevoked)
            stage = ImageExperimentRefusalStage.pairingReport
            val parsed = ImageExperimentJson.parseObject(metadata, 16_384)
            sendStatus(1, Bundle().apply {
                putString("event", "pairing-key")
                putString("sessionId", parsed.getString("sessionId"))
                putString("launchNonce", parsed.getString("launchNonce"))
                putString("publicKeySpkiBase64", bootstrap.publicKeySpkiBase64)
            })
            stage = ImageExperimentRefusalStage.settlement
            val start = SystemClock.elapsedRealtime()
            while (!bootstrap.isRevoked && !activity.isFinishing && !activity.isDestroyed &&
                SystemClock.elapsedRealtime() - start < 180_000) {
                Thread.sleep(100)
            }
            require(bootstrap.isRevoked && bootstrap.launcherOutcome.isSettled())
            result.putString("reason", "NATIVE_LAUNCHER_SETTLED")
            finish(Activity.RESULT_OK, result)
        } catch (_: Exception) {
            ImageExperimentRefusalObservation.report(stage, observeRefusal)
            activity?.bootstrap?.revoke()
            activity?.let { owned ->
                runCatching { runOnMainSync { if (!owned.isFinishing && !owned.isDestroyed) owned.finish() } }
            }
            result.putString("reason", "NATIVE_LAUNCHER_REFUSED")
            runCatching { refusalDiagnostics.firstStage?.let { result.putString("diagnosticStage", it.code) } }
            finish(Activity.RESULT_CANCELED, result)
        }
    }
}
