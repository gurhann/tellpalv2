package com.tellpal.p5d;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import io.flutter.plugin.common.MethodChannel;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** VM service kullanmadan seri komut ve ekran görüntüsü toplar. */
public final class P5dInstrumentation extends Instrumentation {
    private String runNonce;
    private int presentedCases = 0;
    private final java.util.List<Map<String, Object>> captures = new java.util.ArrayList<>();

    @Override
    public void onCreate(Bundle arguments) { super.onCreate(arguments); runNonce = arguments.getString("p5dRunNonce"); start(); }

    private Object command(String method, Object arguments) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> value = new AtomicReference<>();
        AtomicReference<String> failure = new AtomicReference<>();
        runOnMainSync(() -> P5dActivity.current.channel.invokeMethod(method, arguments, new MethodChannel.Result() {
            public void success(Object result) { value.set(result); latch.countDown(); }
            public void error(String code, String message, Object details) { failure.set(code + ":" + message + ":" + String.valueOf(details)); latch.countDown(); }
            public void notImplemented() { failure.set("not_implemented"); latch.countDown(); }
        }));
        if (!latch.await(28, TimeUnit.SECONDS)) throw new IllegalStateException("case_watchdog");
        if (failure.get() != null) throw new IllegalStateException(failure.get());
        return value.get();
    }

    private void progress(String stage) {
        Bundle event = new Bundle(); event.putString("p5dProgress", stage); sendStatus(0, event);
    }

    private android.view.View flutterView(android.view.View view) {
        if (view instanceof io.flutter.embedding.android.FlutterView) return view;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                android.view.View found = flutterView(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private android.view.SurfaceView flutterSurface(android.view.View view) {
        if (view instanceof android.view.SurfaceView) return (android.view.SurfaceView) view;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                android.view.SurfaceView found = flutterSurface(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Flutter yüzeyi fiziksel boyutunda kopyalanır; UiAutomation bağlantısı açılmaz. */
    private Bitmap surfaceCopy(android.view.SurfaceView surface) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(surface.getWidth(), surface.getHeight(), Bitmap.Config.ARGB_8888);
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger code = new java.util.concurrent.atomic.AtomicInteger(-1);
        android.view.PixelCopy.request(surface, bitmap, result -> { code.set(result); latch.countDown(); },
            new android.os.Handler(android.os.Looper.getMainLooper()));
        if (!latch.await(2, TimeUnit.SECONDS)) { bitmap.recycle(); throw new IllegalStateException("pixel_copy_timeout"); }
        if (code.get() != android.view.PixelCopy.SUCCESS) {
            bitmap.recycle();
            throw new IllegalStateException("pixel_copy_failed:" + code.get());
        }
        return bitmap;
    }

    private Bitmap presented(Map<String, Object> proof, int index, long epoch, boolean crop) throws Exception {
        if (proof == null || ((Number) proof.get("index")).intValue() != index ||
            ((Number) proof.get("epoch")).longValue() != epoch || !Boolean.valueOf(crop).equals(proof.get("crop"))) {
            throw new IllegalStateException("stale_or_wrong_mode_proof");
        }
        int[] origin = new int[2];
        AtomicReference<android.view.SurfaceView> target = new AtomicReference<>();
        AtomicReference<String> geometryFailure = new AtomicReference<>();
        runOnMainSync(() -> {
            android.view.View flutter = flutterView(P5dActivity.current.getWindow().getDecorView());
            android.view.SurfaceView surface = flutterSurface(flutter);
            if (surface == null || surface.getWidth() <= 0 || surface.getHeight() <= 0 ||
                surface.getHolder().getSurfaceFrame().width() != surface.getWidth() ||
                surface.getHolder().getSurfaceFrame().height() != surface.getHeight()) {
                geometryFailure.set("surface_physical_geometry_refused"); return;
            }
            int[] flutterOrigin = new int[2], surfaceOrigin = new int[2];
            flutter.getLocationOnScreen(flutterOrigin); surface.getLocationOnScreen(surfaceOrigin);
            origin[0] = flutterOrigin[0] - surfaceOrigin[0]; origin[1] = flutterOrigin[1] - surfaceOrigin[1];
            target.set(surface);
        });
        if (geometryFailure.get() != null) throw new IllegalStateException(geometryFailure.get());
        int left = origin[0] + ((Number) proof.get("markerX")).intValue();
        int top = origin[1] + ((Number) proof.get("markerY")).intValue();
        int cell = ((Number) proof.get("markerCellPixels")).intValue();
        java.util.List<?> colors = (java.util.List<?>) proof.get("markerColors");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        do {
            Bitmap bitmap = surfaceCopy(target.get());
            boolean matches = bitmap != null && left >= 0 && top >= 0 &&
                left + cell * 2 <= bitmap.getWidth() && top + cell * 2 <= bitmap.getHeight();
            if (matches) {
                for (int y = 0; y < cell * 2 && matches; y++) {
                    for (int x = 0; x < cell * 2; x++) {
                        int expected = ((Number) colors.get(y / cell * 2 + x / cell)).intValue();
                        if ((bitmap.getPixel(left + x, top + y) & 0xffffff) != expected) { matches = false; break; }
                    }
                }
            }
            if (matches) {
                proof.put("nativeMarkerLeft", left); proof.put("nativeMarkerTop", top);
                proof.put("nativePresentationVerified", true);
                proof.put("nativeCaptureSource", "FlutterSurfaceViewPixelCopy");
                proof.put("nativeSurfaceWidth", bitmap.getWidth()); proof.put("nativeSurfaceHeight", bitmap.getHeight());
                proof.put("nativeFlutterOffsetX", origin[0]); proof.put("nativeFlutterOffsetY", origin[1]);
                return bitmap;
            }
            if (bitmap != null) bitmap.recycle();
            Thread.sleep(16);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("presentation_proof_timeout");
    }

    private void screenshot(File folder, String name, Bitmap bitmap) throws Exception {
        try (FileOutputStream stream = new FileOutputStream(new File(folder, name))) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new IllegalStateException("screenshot_failed");
        } finally { bitmap.recycle(); }
    }

    private void persist(Map<String, Object> status) throws Exception {
        status.put("runNonce", runNonce);
        status.put("presentedCases", presentedCases);
        status.put("captures", captures);
        status.put("pid", android.os.Process.myPid());
        status.put("api", android.os.Build.VERSION.SDK_INT);
        status.put("abi", android.os.Build.SUPPORTED_ABIS[0]);
        status.put("rendererConfiguration", "Skia; EnableImpeller=false");
        try (FileOutputStream stream = new FileOutputStream(new File(P5dActivity.current.outputDirectory, "runtime.json"))) {
            stream.write(new JSONObject(status).toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onStart() {
        Bundle result = new Bundle();
        try {
            Intent intent = new Intent();
            intent.setClassName(getTargetContext(), "com.tellpal.p5d.P5dActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("p5dRunNonce", runNonce);
            startActivitySync(intent);
            progress("initializing");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(28);
            Map<String, Object> status;
            do {
                Thread.sleep(100);
                try { status = (Map<String, Object>) command("status", null); }
                catch (IllegalStateException error) {
                    if (!"not_implemented".equals(error.getMessage())) throw error;
                    status = new HashMap<>();
                }
                if (status.get("error") != null) throw new IllegalStateException("preflight_failed");
            } while (!"ready".equals(status.get("status")) && System.nanoTime() < deadline);
            if (!"ready".equals(status.get("status"))) throw new IllegalStateException("initialization_watchdog");
            File output = P5dActivity.current.outputDirectory;
            for (int index = 0; index < 12; index++) {
                for (int sample = 0; sample < 4; sample++) {
                    progress("case_" + index + "_sample_" + sample);
                    Map<String, Object> args = new HashMap<>(); args.put("index", index); args.put("sample", sample);
                    status = (Map<String, Object>) command("show", args);
                    if (!"loaded".equals(status.get("status"))) throw new IllegalStateException("case_refused");
                    persist(status);
                    Map<String, Object> slotProof = (Map<String, Object>) status.get("frameProof");
                    long epoch = ((Number) slotProof.get("epoch")).longValue();
                    Bitmap slot = presented(slotProof, index, epoch, false);
                    if (sample == 3) {
                        screenshot(output, "case-" + index + "-slot.png", slot);
                        Map<String, Object> cropProof = (Map<String, Object>) command("crop", true);
                        Map<String, Object> roi = (Map<String, Object>) cropProof.get("roi");
                        if (!roi.get("sourceWidth").equals(roi.get("destinationPhysicalWidth")) ||
                            !roi.get("sourceHeight").equals(roi.get("destinationPhysicalHeight")) ||
                            ((Number) roi.get("sourceX")).intValue() != new int[]{400, 440, 240, 240}[index / 3] ||
                            ((Number) roi.get("sourceY")).intValue() != new int[]{760, 2160, 1410, 240}[index / 3]) throw new IllegalStateException("crop_geometry_refused");
                        screenshot(output, "case-" + index + "-crop-1to1.png", presented(cropProof, index, epoch, true));
                        Map<String, Object> capture = new HashMap<>();
                        capture.put("index", index); capture.put("slot", slotProof); capture.put("crop", cropProof);
                        captures.add(capture);
                        presentedCases++;
                    } else { slot.recycle(); }
                    command("release", null);
                }
            }
            status = (Map<String, Object>) command("stop", null);
            persist(status);
            result.putString("p5dResult", "PASS");
            result.putString("outputDirectory", output.getAbsolutePath());
            finish(Activity.RESULT_OK, result);
        } catch (Exception error) {
            try { persist((Map<String, Object>) command("stop", null)); } catch (Exception ignored) { }
            result.putString("p5dResult", "FAIL");
            result.putString("reason", error.getClass().getSimpleName() + ":" + error.getMessage());
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}
