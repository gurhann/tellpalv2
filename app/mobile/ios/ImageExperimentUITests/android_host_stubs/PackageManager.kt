package android.content.pm

import com.tellpal.imageexperiment.host.HostRuntime

class PackageManager {
    fun checkSignatures(first: String, second: String): Int = HostRuntime.signatureResult
    companion object { const val SIGNATURE_MATCH = 0 }
}
