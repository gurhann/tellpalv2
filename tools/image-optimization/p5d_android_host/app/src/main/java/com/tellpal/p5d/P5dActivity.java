package com.tellpal.p5d;

import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;
import java.util.HashMap;
import java.util.Map;

/** Plugin kaydı yapılmayan ayrı çevrimdışı host. */
public final class P5dActivity extends FlutterActivity {
    public static volatile P5dActivity current;
    public MethodChannel channel;
    public java.io.File outputDirectory;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine engine) {
        current = this;
        String nonce = getIntent().getStringExtra("p5dRunNonce");
        if (nonce == null || !nonce.matches("[a-f0-9]{32}")) throw new IllegalStateException("run_nonce_refused");
        outputDirectory = new java.io.File(getExternalFilesDir(null), nonce);
        if (!outputDirectory.mkdir()) throw new IllegalStateException("run_output_exists");
        channel = new MethodChannel(engine.getDartExecutor().getBinaryMessenger(), "tellpal.p5d.offline");
        channel.setMethodCallHandler((call, result) -> {
            if (!call.method.equals("environment")) { result.notImplemented(); return; }
            Map<String, Object> values = new HashMap<>();
            values.put("packageId", getPackageName());
            values.put("internetPermission", checkSelfPermission("android.permission.INTERNET") == PackageManager.PERMISSION_GRANTED);
            values.put("outputDirectory", outputDirectory.getAbsolutePath());
            result.success(values);
        });
    }

    @Override
    protected void onDestroy() {
        if (current == this) current = null;
        super.onDestroy();
    }
}
