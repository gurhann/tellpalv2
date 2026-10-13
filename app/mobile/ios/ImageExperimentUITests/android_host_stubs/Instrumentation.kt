package android.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tellpal.imageexperiment.host.HostRuntime

open class Instrumentation {
    val context = Context()
    val targetContext = Context(this)
    open fun onCreate(arguments: Bundle?) {}
    open fun onStart() {}
    fun start() {}
    open fun newActivity(cl: ClassLoader, className: String, intent: Intent): Activity =
        cl.loadClass(className).getDeclaredConstructor().newInstance() as Activity
    open fun callActivityOnCreate(activity: Activity, savedInstanceState: Bundle?) {
        if (HostRuntime.failActivityCreate) throw HostRuntime.failure
        activity.onCreate(savedInstanceState)
        if (HostRuntime.destroyDuringCreate) activity.onDestroy()
    }
    fun startActivitySync(intent: Intent): Activity = HostRuntime.startActivitySync(this, intent)
    fun runOnMainSync(action: () -> Unit) = HostRuntime.onMain(action)
    fun sendStatus(code: Int, result: Bundle) = HostRuntime.reportStatus(code, result)
    fun finish(code: Int, result: Bundle) = HostRuntime.recordTerminal(code, result)
}
