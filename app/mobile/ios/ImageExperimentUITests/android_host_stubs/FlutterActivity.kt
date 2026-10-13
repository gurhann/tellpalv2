package io.flutter.embedding.android

import android.app.Activity
import io.flutter.embedding.engine.FlutterEngine
import android.os.Bundle
import com.tellpal.imageexperiment.host.HostRuntime

open class FlutterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HostRuntime.awaitActivityCreate(this)
        configureFlutterEngine(FlutterEngine())
    }
    open fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        HostRuntime.bindMessenger(this, flutterEngine.dartExecutor.binaryMessenger)
    }
}
