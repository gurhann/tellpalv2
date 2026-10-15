package android.util

import com.tellpal.imageexperiment.host.HostRuntime

object Base64 {
    const val NO_WRAP = 2
    fun decode(value: String, flags: Int): ByteArray {
        if (HostRuntime.failSharedVector && value.startsWith("MFkwEwYHKoZIzj0")) throw HostRuntime.failure
        return java.util.Base64.getDecoder().decode(value)
    }
    fun encodeToString(value: ByteArray, flags: Int): String {
        if (HostRuntime.failQualityEncoding && value.toString(Charsets.UTF_8) == "null") throw HostRuntime.failure
        return java.util.Base64.getEncoder().encodeToString(value)
    }
}
