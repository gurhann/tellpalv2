package io.flutter.embedding.android

import android.app.Activity
import io.flutter.embedding.engine.FlutterEngine

open class FlutterActivity : Activity() {
    open fun configureFlutterEngine(flutterEngine: FlutterEngine) {}
}
