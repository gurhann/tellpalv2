package android.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tellpal.imageexperiment.host.HostRuntime
import com.tellpal.imageexperiment.ImageExperimentActivity
import io.flutter.embedding.engine.FlutterEngine

open class Instrumentation {
    val context = Context()
    val targetContext = Context()
    open fun onCreate(arguments: Bundle?) {}
    open fun onStart() {}
    fun start() {}
    open fun newActivity(cl: ClassLoader, className: String, intent: Intent): Activity =
        cl.loadClass(className).getDeclaredConstructor().newInstance() as Activity
    fun startActivitySync(intent: Intent): Activity {
        if (HostRuntime.failActivityLaunch) throw HostRuntime.failure
        var created: ImageExperimentActivity? = null
        HostRuntime.onMain {
            val owned = newActivity(javaClass.classLoader, intent.className, intent) as ImageExperimentActivity
            HostRuntime.activity = owned
            owned.intent = intent
            owned.onCreate(null)
            owned.configureFlutterEngine(FlutterEngine())
            created = owned
            HostRuntime.startSession()
        }
        return created!!
    }
    fun runOnMainSync(action: () -> Unit) = HostRuntime.onMain(action)
    fun sendStatus(code: Int, result: Bundle) { HostRuntime.statuses += code to result }
    fun finish(code: Int, result: Bundle) { HostRuntime.resultCode = code; HostRuntime.result = result }
}
