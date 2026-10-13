package io.flutter.plugin.common

import com.tellpal.imageexperiment.host.HostRuntime

data class MethodCall(val method: String, val arguments: Any?)
class MethodChannel(private val messenger: Any, name: String) {
    interface Result {
        fun success(result: Any?)
        fun error(errorCode: String, errorMessage: String?, errorDetails: Any?)
        fun notImplemented()
    }
    fun setMethodCallHandler(handler: (MethodCall, Result) -> Unit) { HostRuntime.setHandler(messenger, handler) }
}
