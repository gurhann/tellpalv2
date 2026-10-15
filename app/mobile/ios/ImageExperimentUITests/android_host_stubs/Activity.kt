package android.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tellpal.imageexperiment.host.HostRuntime

open class Activity : Context() {
    var intent = Intent()
    @Volatile var isFinishing = false
    @Volatile var isDestroyed = false
    @Volatile var finishCount = 0
    open fun onCreate(savedInstanceState: Bundle?) {}
    open fun onPause() {}
    open fun onStop() {}
    open fun onResume() {}
    open fun onDestroy() { isDestroyed = true }
    open fun finish() { finishCount++; isFinishing = true; HostRuntime.recordActivityFinish(this) }
    fun runOnUiThread(action: () -> Unit) = HostRuntime.runOnUiThread(action)
    val resources = android.content.res.Resources()
    private val content = android.view.ViewGroup(this)
    fun hasWindowFocus() = !isFinishing && !isDestroyed
    @Suppress("UNCHECKED_CAST")
    fun <T : android.view.View> findViewById(id: Int): T = content as T
    fun moveTaskToBack(nonRoot: Boolean): Boolean { onPause(); onStop(); return true }
    companion object { const val RESULT_OK = -1; const val RESULT_CANCELED = 0 }
}
