package com.tellpal.imageexperiment.host

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import com.tellpal.imageexperiment.ImageExperimentActivity
import com.tellpal.imageexperiment.ImageExperimentBootstrap
import com.tellpal.imageexperiment.ImageExperimentLedger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Controlled lifecycle and per-engine channels; production code owns all validation. */
object HostRuntime {
    const val APP_ID = "com.tellpal.tellpal_mobile_poc.imageexperiment.profile"
    var readerFixture = false
    val applicationId: String get() = if (readerFixture) APP_ID.removeSuffix(".profile") + ".release" else APP_ID
    val failure = IllegalStateException("PRIVATE_RAW_SENTINEL_DO_NOT_EXPORT")
    val main = Executors.newSingleThreadExecutor { Thread(it, "controlled-android-main") }
    lateinit var privateRoot: File
    @Volatile var activity: ImageExperimentActivity? = null
    private val messengerOwners = java.util.IdentityHashMap<Any, Activity>()
    private val handlers = java.util.IdentityHashMap<Activity, (MethodCall, MethodChannel.Result) -> Unit>()
    private val instances = mutableListOf<Activity>()
    val handler: ((MethodCall, MethodChannel.Result) -> Unit)? get() = handlers[activity]
    @Volatile var resultCode: Int? = null
    @Volatile var result: Bundle? = null
    @Volatile var terminalCount = 0
    var terminalReady = CountDownLatch(1)
    var createEntered: CountDownLatch? = null
    var createRelease: CountDownLatch? = null
    var offMainUiDelay = 0L
    val cleanupEvents = java.util.concurrent.CopyOnWriteArrayList<String>()
    val finishOwners = java.util.concurrent.CopyOnWriteArrayList<Activity>()
    val statuses = java.util.concurrent.CopyOnWriteArrayList<Pair<Int, Bundle>>()
    val requests = java.util.concurrent.CopyOnWriteArrayList<HostedConnection>()
    val events = java.util.concurrent.CopyOnWriteArrayList<String>()
    @Volatile var pairedPublicKey: String? = null
    @Volatile var elapsedOffset = 0L
    private var startupReady = CountDownLatch(1)
    var launchIntent: Intent? = null
    private var launchMetadata: String? = null
    var pendingCreation: (() -> Unit)? = null
    var beforeInstantiate: ((Instrumentation, Intent) -> Unit)? = null
    var beforeCreate: ((Instrumentation, Activity, Intent) -> Unit)? = null
    var afterCreate: ((Instrumentation, Activity, Intent) -> Unit)? = null
    var deferCreation = false
    var deferAfterInstantiation = false
    var advanceClockOnLaunch = 0L
    var signatureResult = 0
    var failSharedVector = false
    var failActivityLaunch = false
    var failActivityConstructor = false
    var failActivityCreate = false
    var destroyDuringCreate = false
    var throwDiagnosticExport = false
    var networkFailure: String? = null
    var backendProbeClockAdvance = 0L
    var dartReport: Any? = null
    var outcome = "settled"
    var ledger = VALID_LEDGER
    @Volatile var sessionError: String? = null
    const val VALID_LEDGER = """{"schemaVersion":1,"records":[{"event":"status_request","observerOverheadMicroseconds":2,"dpr":2.0,"fit":"contain","firstRasterizedTargetFrame":null}],"droppedCount":0}"""

    fun reset(root: File) {
        onMain {
            instances.toList().forEach { if (!it.isDestroyed) it.onDestroy() }
            activity?.let { if (!it.isDestroyed) it.onDestroy() }
            instances.clear(); messengerOwners.clear(); handlers.clear()
        }
        privateRoot = root.apply { mkdirs() }
        activity = null; resultCode = null; result = null; terminalCount = 0; readerFixture = false
        terminalReady = CountDownLatch(1); createEntered = null; createRelease = null; offMainUiDelay = 0
        cleanupEvents.clear(); finishOwners.clear()
        statuses.clear(); requests.clear(); events.clear(); pairedPublicKey = null
        startupReady = CountDownLatch(1); launchIntent = null; launchMetadata = null; pendingCreation = null
        beforeInstantiate = null; beforeCreate = null; afterCreate = null; deferCreation = false; deferAfterInstantiation = false
        elapsedOffset = 0; advanceClockOnLaunch = 0
        signatureResult = 0; failSharedVector = false; failActivityLaunch = false
        failActivityConstructor = false; failActivityCreate = false; destroyDuringCreate = false
        throwDiagnosticExport = false; networkFailure = null; backendProbeClockAdvance = 0; dartReport = null
        outcome = "settled"; ledger = VALID_LEDGER; sessionError = null
    }

    fun onMain(action: () -> Unit) {
        if (Thread.currentThread().name == "controlled-android-main") action()
        else main.submit(action).get(5, TimeUnit.SECONDS)
    }

    fun runOnUiThread(action: () -> Unit) {
        if (Thread.currentThread().name == "controlled-android-main") action()
        else main.submit {
            if (offMainUiDelay != 0L) Thread.sleep(offMainUiDelay)
            action()
        }
    }

    fun awaitActivityCreate(owner: Activity) {
        if (owner !== activity) return
        val release = createRelease ?: return
        createEntered!!.countDown()
        check(release.await(5, TimeUnit.SECONDS)) { "HARNESS_ASSERTION blocked-main-fixture-release-bound" }
    }

    fun recordActivityFinish(owner: Activity) {
        finishOwners += owner
        cleanupEvents += "activity-finish"
    }

    fun recordTerminal(code: Int, result: Bundle) {
        this.result = result; resultCode = code; terminalCount++
        cleanupEvents += "terminal"
        terminalReady.countDown()
    }

    fun bindMessenger(owner: Activity, messenger: Any) { messengerOwners[messenger] = owner }
    fun setHandler(messenger: Any, value: (MethodCall, MethodChannel.Result) -> Unit) {
        handlers[messengerOwners[messenger] ?: error("Unowned engine")] = value
    }
    fun invoke(method: String, arguments: Any?, owner: Activity = activity!!): CapturedResult {
        val captured = CapturedResult()
        onMain {
            handlers[owner]!!(MethodCall(method, arguments), captured)
            if (method == "revoke") events += "settlement"
        }
        return captured
    }

    fun instantiate(launcher: Instrumentation, intent: Intent): Activity {
        if (failActivityConstructor) throw failure
        val created = launcher.newActivity(javaClass.classLoader, intent.className, intent)
        instances += created
        created.intent = intent
        return created
    }

    fun startActivity(launcher: Instrumentation, intent: Intent) {
        if (failActivityLaunch) throw failure
        launchIntent = intent
        launchMetadata = intent.getStringExtra("imageExperimentMetadata")
        val create = {
            beforeInstantiate?.invoke(launcher, intent)
            val owned = instantiate(launcher, intent)
            activity = owned as ImageExperimentActivity
            beforeCreate?.invoke(launcher, owned, intent)
            val complete = {
                launcher.callActivityOnCreate(owned, null)
                afterCreate?.invoke(launcher, owned, intent)
                if (!owned.isFinishing && !owned.isDestroyed) startSession(owned)
            }
            if (deferAfterInstantiation) pendingCreation = complete else complete()
        }
        if (deferCreation) pendingCreation = create
        else main.submit { runCatching { create() } }
        if (advanceClockOnLaunch != 0L) elapsedOffset += advanceClockOnLaunch
    }

    fun startActivitySync(launcher: Instrumentation, intent: Intent): Activity {
        startActivity(launcher, intent)
        // API 33 synchronous startup waits for Flutter's UI/idle gate.
        check(startupReady.await(7, TimeUnit.SECONDS)) { "HARNESS_ASSERTION synchronous-startup-callback-bound" }
        return activity!!
    }

    fun releaseCreation() {
        val pending = pendingCreation ?: error("No pending lifecycle")
        pendingCreation = null
        onMain { pending() }
    }

    fun reportStatus(code: Int, result: Bundle) {
        val owner = activity ?: error("No owned activity")
        val bootstrap = owner.bootstrap ?: error("No owned bootstrap")
        if (code == 2) {
            require(result.keySet() == setOf("event", "index", "count", "ledgerSha256", "payloadBase64") &&
                result.getString("event") == "ledger-chunk" && bootstrap.isRevoked)
            val file = java.io.File(privateRoot, "image_experiment/ledger.json")
            val bytes = ImageExperimentLedger.validate(file.readText())
            val index = result.getString("index")!!.toInt()
            val count = result.getString("count")!!.toInt()
            require(count == (bytes.size + 3071) / 3072 && index in 0 until count)
            require(result.getString("ledgerSha256") == ImageExperimentBootstrap.sha256(bytes))
            val payload = java.util.Base64.getDecoder().decode(result.getString("payloadBase64"))
            require(payload.contentEquals(bytes.copyOfRange(index * 3072, minOf((index + 1) * 3072, bytes.size))))
            return
        }
        val metadata = org.json.JSONObject(launchMetadata!!)
        require(code == 1 && result.keySet() == setOf("event", "sessionId", "launchNonce", "publicKeySpkiBase64"))
        require(result.getString("event") == "pairing-key" && result.getString("sessionId") == metadata.getString("sessionId") &&
            result.getString("launchNonce") == metadata.getString("launchNonce") &&
            result.getString("publicKeySpkiBase64") == bootstrap.publicKeySpkiBase64 && bootstrap.isReachabilityQualified && !bootstrap.isRevoked)
        require(statuses.isEmpty())
        statuses += code to result
        events += "status"
        pairedPublicKey = result.getString("publicKeySpkiBase64")
    }

    fun startSession(owner: ImageExperimentActivity) {
        handlers[owner]!!(MethodCall("takeSession", null), object : MethodChannel.Result {
            override fun success(result: Any?) {
                require(result is Map<*, *> && result.keys == setOf("envelope", "metadata"))
                events += "handoff"; events += "startup-ready"; startupReady.countDown()
                Thread({
                    Thread.sleep(120)
                    if (dartReport != null) invoke("reportDiagnostic", dartReport, owner)
                    val stored = invoke("storeLedger", ledger, owner)
                    require(stored.await() is Boolean)
                    invoke("revoke", mapOf("result" to outcome), owner).await()
                }, "controlled-dart-client").start()
            }
            override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                sessionError = errorCode
                events += "startup-ready"; startupReady.countDown()
            }
            override fun notImplemented() = error("Unexpected method")
        })
    }

    fun close() {
        onMain {
            instances.toList().forEach { if (!it.isDestroyed) it.onDestroy() }
            activity?.let { if (!it.isDestroyed) it.onDestroy() }
        }
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
