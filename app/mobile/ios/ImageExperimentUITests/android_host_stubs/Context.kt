package android.content

import android.content.pm.PackageManager
import com.tellpal.imageexperiment.host.HostRuntime
import java.io.File
import android.app.Instrumentation
import android.content.Intent

open class Context(private val instrumentation: Instrumentation? = null) {
    open val packageName: String = HostRuntime.APP_ID
    val packageManager = PackageManager()
    val noBackupFilesDir: File get() = HostRuntime.privateRoot
    fun startActivity(intent: Intent) = HostRuntime.startActivity(instrumentation!!, intent)
}
