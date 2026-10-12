package android.os

import com.tellpal.imageexperiment.host.HostRuntime

class Bundle {
    private val values = linkedMapOf<String, Any?>()
    fun putString(key: String, value: String?) {
        if (key == "diagnosticStage" && HostRuntime.throwDiagnosticExport) throw HostRuntime.failure
        values[key] = value
    }
    fun getString(key: String): String? = values[key] as? String
    fun keySet(): Set<String> = values.keys
}
