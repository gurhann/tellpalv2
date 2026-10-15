package android.content

import android.content.pm.PackageManager
import com.tellpal.imageexperiment.host.HostRuntime
import java.io.File
import android.app.Instrumentation
import android.content.Intent

open class Context(private val instrumentation: Instrumentation? = null) {
    open val packageName: String get() = HostRuntime.applicationId
    val packageManager = PackageManager()
    val noBackupFilesDir: File get() = HostRuntime.privateRoot
    fun getSystemService(name: String): Any? = null
    fun startActivity(intent: Intent) = HostRuntime.startActivity(instrumentation!!, intent)
    companion object { const val ACTIVITY_SERVICE = "activity" }
}
