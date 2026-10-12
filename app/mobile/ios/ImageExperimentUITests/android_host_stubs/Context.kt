package android.content

import android.content.pm.PackageManager
import com.tellpal.imageexperiment.host.HostRuntime
import java.io.File

open class Context {
    open val packageName: String = HostRuntime.APP_ID
    val packageManager = PackageManager()
    val noBackupFilesDir: File get() = HostRuntime.privateRoot
}
