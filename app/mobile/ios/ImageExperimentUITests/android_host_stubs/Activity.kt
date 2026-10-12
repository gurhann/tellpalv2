package android.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tellpal.imageexperiment.host.HostRuntime

open class Activity : Context() {
    var intent = Intent()
    @Volatile var isFinishing = false
    @Volatile var isDestroyed = false
    var finishCount = 0
    open fun onCreate(savedInstanceState: Bundle?) {}
    open fun onDestroy() { isDestroyed = true }
    open fun finish() { finishCount++; isFinishing = true }
    fun runOnUiThread(action: () -> Unit) = HostRuntime.onMain(action)
    companion object { const val RESULT_OK = -1; const val RESULT_CANCELED = 0 }
}
