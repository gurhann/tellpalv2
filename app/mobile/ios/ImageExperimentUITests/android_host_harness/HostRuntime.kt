package com.tellpal.imageexperiment.host

import android.app.Activity
import android.os.Bundle
import com.tellpal.imageexperiment.ImageExperimentActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Controlled Android ownership and Flutter boundaries; production code owns all validation. */
object HostRuntime {
    const val APP_ID = "com.tellpal.tellpal_mobile_poc.imageexperiment.profile"
    val failure = IllegalStateException("PRIVATE_RAW_SENTINEL_DO_NOT_EXPORT")
    val main = Executors.newSingleThreadExecutor { Thread(it, "controlled-android-main") }
    lateinit var privateRoot: File
    var activity: ImageExperimentActivity? = null
    var handler: ((MethodCall, MethodChannel.Result) -> Unit)? = null
    var resultCode: Int? = null
    var result: Bundle? = null
    val statuses = java.util.concurrent.CopyOnWriteArrayList<Pair<Int, Bundle>>()
    val requests = java.util.concurrent.CopyOnWriteArrayList<HostedConnection>()
    var signatureResult = 0
    var failSharedVector = false
    var failActivityLaunch = false
    var throwDiagnosticExport = false
    var networkFailure: String? = null
    var dartReport: Any? = null
    var outcome = "settled"
    var ledger = VALID_LEDGER
    var sessionError: String? = null
    const val VALID_LEDGER = """{"schemaVersion":1,"records":[{"event":"status_request","observerOverheadMicroseconds":2,"dpr":2.0,"fit":"contain","firstRasterizedTargetFrame":null}],"droppedCount":0}"""

    fun reset(root: File) {
        activity?.let { onMain { it.onDestroy() } }
        privateRoot = root.apply { mkdirs() }
        activity = null; handler = null; resultCode = null; result = null
        statuses.clear(); requests.clear()
        signatureResult = 0; failSharedVector = false; failActivityLaunch = false
        throwDiagnosticExport = false; networkFailure = null; dartReport = null
        outcome = "settled"; ledger = VALID_LEDGER; sessionError = null
    }

    fun onMain(action: () -> Unit) {
        if (Thread.currentThread().name == "controlled-android-main") action()
        else main.submit(action).get(5, TimeUnit.SECONDS)
    }

    fun invoke(method: String, arguments: Any?): CapturedResult {
        val captured = CapturedResult()
        onMain { handler!!(MethodCall(method, arguments), captured) }
        return captured
    }

    fun startSession() {
        handler!!(MethodCall("takeSession", null), object : MethodChannel.Result {
            override fun success(result: Any?) {
                require(result is Map<*, *> && result.keys == setOf("envelope", "metadata"))
                Thread({
                    Thread.sleep(120)
                    if (dartReport != null) invoke("reportDiagnostic", dartReport)
                    val stored = invoke("storeLedger", ledger)
                    require(stored.await() is Boolean)
                    invoke("revoke", mapOf("result" to outcome)).await()
                }, "controlled-dart-client").start()
            }
            override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                sessionError = errorCode
            }
            override fun notImplemented() = error("Unexpected method")
        })
    }

    fun close() {
        activity?.let { onMain { it.onDestroy() } }
        main.shutdownNow()
    }
}

class CapturedResult : MethodChannel.Result {
    private val done = CountDownLatch(1)
    var value: Any? = null
    var error: String? = null
    var unimplemented = false
    override fun success(result: Any?) { value = result; done.countDown() }
    override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
        require(errorMessage == null && errorDetails == null)
        error = errorCode; done.countDown()
    }
    override fun notImplemented() { unimplemented = true; done.countDown() }
    fun await(): Any? { require(done.await(5, TimeUnit.SECONDS)); return value }
}
