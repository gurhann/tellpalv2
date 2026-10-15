package com.tellpal.imageexperiment

import android.content.Context
import android.system.Os
import android.system.OsConstants
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Re-encodes an independently validated whitelist into this application's private container. */
object ImageExperimentLedger {
    private val hashes = setOf("caseSha256", "providerSha256", "attemptSha256", "sessionSha256", "sourceSha256", "referenceSha256", "settingsSha256", "scopeSha256", "modelSha256", "abisSha256", "deviceFactsSha256")
    private val integers = setOf("generation", "monotonicMicroseconds", "requestCount", "retryCount", "bodyBytes", "sourceWidth", "sourceHeight", "requiredWidth", "requiredHeight", "decodeWidth", "decodeHeight", "actualDecodeWidth", "actualDecodeHeight", "frameNumber", "dartHeapBytes", "dartExternalBytes", "decodedLruBytes", "decodedLiveCount", "decodedPendingCount", "processBytes", "nativeBytes", "graphicsBytes", "diskBytes", "transferBytes", "firstRasterizedTargetFrame", "observerOverheadMicroseconds", "memoryBytes", "nativeReadMicroseconds", "nativeResultCode", "nativeResultCount", "sampleStartMicroseconds", "sampleEndMicroseconds", "sampleCadenceMicroseconds", "pageIndex", "selectionGeneration", "decodedKeepAliveCount", "frameCount", "frameBuildTotalMicroseconds", "frameRasterTotalMicroseconds", "frameBuildMaxMicroseconds", "frameRasterMaxMicroseconds", "apiLevel", "memTotalBytes", "memoryClassMiB", "backgroundCount", "returnCount", "backgroundEpochMilliseconds", "returnedEpochMilliseconds")
    private val doubles = setOf("logicalSlotWidth", "logicalSlotHeight", "dpr", "alignmentX", "alignmentY", "observedSlotWidth", "observedSlotHeight", "observedDpr", "textScaleFactor", "paddingLeft", "paddingTop", "paddingRight", "paddingBottom", "viewInsetLeft", "viewInsetTop", "viewInsetRight", "viewInsetBottom")
    private val booleans = setOf("encodedVerified", "sourceShortage", "physicalPresentationVerified", "lowRamDevice", "isPhysicalDevice", "backgrounded")
    private val strings = mapOf(
        "phase" to setOf("idle", "baseline", "load", "hold", "releasing", "post_release", "complete", "reader", "natural_retention", "owned_eviction"),
        "memoryCounter" to setOf("ios_phys_footprint", "android_total_pss", "unavailable"),
        "memoryReason" to setOf("none", "mach_result", "mach_count", "missing_counter", "sample_failed", "session_inactive", "sample_timeout"),
        "thermalState" to setOf("nominal", "fair", "serious", "critical", "unavailable"),
        "clockDomain" to setOf("dart_monotonic"),
        "clockUnit" to setOf("microseconds"),
        "fit" to setOf("contain", "cover"),
        "orientation" to setOf("portrait", "landscape"),
        "platform" to setOf("ios", "android"),
        "deviceReason" to setOf("none", "unsupported", "read_failed", "invalid_value", "unavailable"),
        "qemu" to setOf("0", "1", "unavailable"),
        "role" to setOf("isolated_raster_fixture"),
        "event" to setOf("request", "body", "encoded_verified", "decoded", "draw", "failure", "revoke", "cache_state", "status", "status_request", "status_body", "ownership", "phase", "memory_sample", "release_complete", "layout", "dispatch", "reader_geometry", "reader_selection", "page_paint", "frame_aggregate", "device_facts", "lifecycle"),
        "ownership" to setOf("owned", "settling", "settled", "released", "unavailable"),
        "reason" to setOf("none", "invalid_fixture", "session_inactive", "status_unavailable", "status_revoked", "missing_counter", "unverified_visibility", "frame_unavailable", "timing_unavailable", "clock_unmapped", "retained_layer_ambiguity", "route_departed", "stale_attempt", "internal_settlement_unavailable", "export_capacity", "pin_refused", "transport_refused", "decode_refused", "cleanup_unavailable", "phase_budget_refused", "load_timeout", "layout_unavailable", "lifecycle_unavailable", "device_facts_unavailable", "reader_incomplete"),
        "counterReason" to setOf("missing_counter", "unverified_visibility", "frame_unavailable", "timing_unavailable", "clock_unmapped", "retained_layer_ambiguity", "internal_settlement_unavailable"),
        "cacheState" to setOf("namespace_keys_absent", "network-cold", "disk-warm/decoded-cold", "decoded-warm", "unavailable"))

    fun validate(raw: String): ByteArray {
        require(raw.toByteArray(Charsets.UTF_8).size <= 262_144)
        val root = ImageExperimentJson.parseObject(raw, 262_144)
        require(root.keys().asSequence().toSet() == setOf("schemaVersion", "records", "droppedCount"))
        require(root.get("schemaVersion") == 1 && isInteger(root.get("droppedCount")) && root.getLong("droppedCount") >= 0)
        val records = root.get("records") as? JSONArray ?: error("REFUSED")
        require(records.length() <= 512)
        for (index in 0 until records.length()) {
            val row = records.get(index) as? JSONObject ?: error("REFUSED")
            for (field in row.keys().asSequence()) {
                val value = row.get(field)
                require(when {
                    field in hashes -> value is String && Regex("[a-f0-9]{64}").matches(value)
                    field in integers -> value == JSONObject.NULL ||
                        (isInteger(value) && (value as Number).toLong() >= if (field == "frameNumber") -1 else 0)
                    field in doubles -> value is Number && value.toDouble().isFinite()
                    field in booleans -> value is Boolean ||
                        (field in setOf("lowRamDevice", "isPhysicalDevice") && value == JSONObject.NULL)
                    field in strings -> value is String && value in strings.getValue(field)
                    else -> false
                })
            }
        }
        // Never persist original JSON: duplicate-key shadowed or unvalidated string bytes cannot survive.
        return root.toString().toByteArray(Charsets.UTF_8).also { require(it.size <= 262_144) }
    }

    private fun isInteger(value: Any) = value is Int || value is Long

    fun store(context: Context, raw: String): Boolean {
        var temporary: File? = null
        val started = android.os.SystemClock.elapsedRealtime()
        fun checkDeadline() = require(!Thread.currentThread().isInterrupted &&
            android.os.SystemClock.elapsedRealtime() - started < 2000)
        return try {
            checkDeadline()
            val bytes = validate(raw)
            checkDeadline()
            val root = context.noBackupFilesDir.canonicalFile
            val directory = File(root, "image_experiment")
            require(directory.mkdirs() || directory.isDirectory)
            require(directory.canonicalFile.parentFile == root &&
                Os.lstat(directory.path).st_mode and OsConstants.S_IFMT == OsConstants.S_IFDIR)
            Os.chmod(directory.path, 0x1c0) // 0700
            val destination = File(directory, "ledger.json")
            if (destination.exists()) require(Os.lstat(destination.path).st_mode and OsConstants.S_IFMT == OsConstants.S_IFREG)
            temporary = File.createTempFile("ledger-", ".tmp", directory)
            Os.chmod(temporary.path, 0x180) // 0600
            temporary.outputStream().use { output ->
                for (offset in bytes.indices step 4096) {
                    checkDeadline()
                    output.write(bytes, offset, minOf(4096, bytes.size - offset))
                }
            }
            checkDeadline()
            require(temporary.renameTo(destination))
            true
        } catch (_: Exception) { false }
        finally { temporary?.delete() }
    }
}

/** Self PSS is a distinct OS accounting view, never an iOS footprint equivalent. */
object ImageExperimentMemory {
    fun sample(): Map<String, Any?> {
        val start = android.os.SystemClock.elapsedRealtimeNanos()
        var bytes: Long? = null
        var reason = "sample_failed"
        try {
            val info = android.os.Debug.MemoryInfo()
            android.os.Debug.getMemoryInfo(info)
            val pss = info.totalPss
            if (pss >= 0) { bytes = Math.multiplyExact(pss.toLong(), 1024L); reason = "none" }
            else reason = "missing_counter"
        } catch (_: Exception) { }
        return mapOf("memoryBytes" to bytes, "memoryCounter" to "android_total_pss",
            "memoryReason" to reason, "nativeReadMicroseconds" to
                (android.os.SystemClock.elapsedRealtimeNanos() - start) / 1000L,
            "nativeResultCode" to null, "nativeResultCount" to null, "thermalState" to "unavailable")
    }
}

/** A single native hold lease. Cleared/expired evidence cannot be republished for this attempt. */
class ImageExperimentQualityLifetime(private val now: () -> Long = System::currentTimeMillis) {
    private var started = false
    var deadline: Long = 0L
        private set
    fun begin(sessionExpiresAt: Long): Boolean {
        val current = now()
        if (started || sessionExpiresAt <= current) return false
        val limit = runCatching { Math.addExact(current, 10_000L) }.getOrNull() ?: return false
        started = true
        deadline = minOf(limit, sessionExpiresAt)
        return isActive()
    }
    fun isActive(): Boolean = deadline > now()
    fun invalidate() { started = true; deadline = 0L }
}

/** Reader permissions are finite and never widen a legacy single-case session. */
object ImageExperimentReaderPolicy {
    val primaryBatches = setOf("reader_resolution_a_r1", "reader_resolution_b_r1", "reader_resolution_c_r1")
    val qualityBatches = setOf("reader_quality_full_r1", "reader_quality_100_r1", "reader_quality_075_r1")
    val batches = primaryBatches + qualityBatches
    fun accepts(batch: String, selected: String, entries: JSONArray): Boolean = runCatching {
        val count = if (batch in qualityBatches) 2 else 6
        require(batch in batches && selected == "reader_page_1" && entries.length() == count)
        for (index in 0 until count) {
            val entry = entries.getJSONObject(index)
            require(entry.getString("caseId") == "reader_page_${index + 1}" &&
                entry.getString("mime") == "image/png" && entry.getString("purposeAcceptanceReceipt") == batch)
        }
        true
    }.getOrDefault(false)
}

/** Counts actual OS lifecycle callbacks, including an interrupted return as unavailable. */
class ImageExperimentReaderLifecycle(private val now: () -> Long = System::currentTimeMillis) {
    private var enabled = false
    private var backgroundCount = 0
    private var returnCount = 0
    private var backgrounded = false
    private var backgroundAt: Long? = null
    private var returnedAt: Long? = null
    fun enable() { enabled = true }
    fun background() {
        if (!enabled || backgrounded) return
        backgrounded = true; backgroundCount++; backgroundAt = now(); returnedAt = null
    }
    fun foreground() {
        if (!enabled || !backgrounded) return
        backgrounded = false; returnCount++; returnedAt = now()
    }
    fun snapshot(): Map<String, Any?> = mapOf("schemaVersion" to 1, "backgroundCount" to backgroundCount,
        "returnCount" to returnCount, "backgrounded" to backgrounded,
        "backgroundEpochMilliseconds" to backgroundAt, "returnedEpochMilliseconds" to returnedAt)
}

/** At most two reader bindings share one twenty-second quality deadline. */
class ImageExperimentReaderQualityLifetime(private val now: () -> Long = System::currentTimeMillis) {
    private var globalDeadline = 0L
    private var publications = 0
    private var invalidated = false
    var deadline = 0L
        private set
    fun begin(sessionExpiresAt: Long): Boolean = runCatching {
        val current = now()
        require(!invalidated && publications < 2 && sessionExpiresAt > current)
        val total = if (globalDeadline == 0L) Math.addExact(current, 20_000L) else globalDeadline
        val selected = minOf(total, Math.addExact(current, 10_000L), sessionExpiresAt)
        require(selected > current)
        globalDeadline = total; deadline = selected; publications++
        true
    }.getOrDefault(false)
    fun isActive() = !invalidated && deadline > now()
    fun release() { deadline = 0L }
    fun invalidate() { invalidated = true; deadline = 0L }
}

/** Model and RAM facts are OS observations; catalog and physical ownership remain separate gates. */
object ImageExperimentDeviceFacts {
    fun validatedBytes(value: Map<String, Any?>): ByteArray {
        val nullable = setOf("model", "apiLevel", "abis", "memTotalBytes", "lowRamDevice", "memoryClassMiB", "isPhysicalDevice", "qemu")
        require(value.keys == nullable + setOf("schemaVersion", "platform", "unavailableReasons"))
        require(value["schemaVersion"] == 1 && value["platform"] == "android")
        val model = value["model"]
        require(model == null || model is String && model.length in 1..128 && model.none { it.isISOControl() })
        val abis = value["abis"]
        require(abis == null || abis is List<*> && abis.size in 1..8 && abis.all { it is String && Regex("[A-Za-z0-9_-]{1,32}").matches(it) })
        for (key in setOf("apiLevel", "memTotalBytes", "memoryClassMiB")) {
            val item = value[key]
            require(item == null || (item is Int || item is Long) && (item as Number).toLong() > 0)
        }
        for (key in setOf("lowRamDevice", "isPhysicalDevice")) require(value[key] == null || value[key] is Boolean)
        require(value["qemu"] == null || value["qemu"] in setOf("0", "1"))
        val reasons = value["unavailableReasons"] as? Map<*, *> ?: error("FACTS_REFUSED")
        require(reasons.keys == nullable.filter { value[it] == null }.toSet() &&
            reasons.values.all { it in setOf("unsupported", "read_failed", "invalid_value") })
        return JSONObject(value).toString().toByteArray(Charsets.UTF_8).also { require(it.size <= 4096) }
    }

    fun memTotalBytes(raw: String): Long? = runCatching {
        require(raw.toByteArray(Charsets.US_ASCII).size <= 8192)
        val matches = Regex("(?m)^MemTotal:[ \\t]+([0-9]+)[ \\t]+kB[ \\t]*$").findAll(raw).toList()
        require(matches.size == 1)
        Math.multiplyExact(matches.single().groupValues[1].toLong(), 1024L).also { require(it > 0) }
    }.getOrNull()

    fun read(context: Context): Map<String, Any?> {
        val reasons = mutableMapOf<String, String>()
        fun <T> observe(field: String, operation: () -> T?): T? = try {
            operation().also { if (it == null) reasons[field] = "invalid_value" }
        } catch (_: Exception) { reasons[field] = "read_failed"; null }
        val model = observe("model") { android.os.Build.MODEL.takeIf { it.length in 1..128 && it.none { c -> c.isISOControl() } } }
        val api = observe("apiLevel") { android.os.Build.VERSION.SDK_INT.takeIf { it > 0 } }
        val abis = observe("abis") { android.os.Build.SUPPORTED_ABIS.toList().takeIf {
            it.size in 1..8 && it.all { abi -> Regex("[A-Za-z0-9_-]{1,32}").matches(abi) } } }
        val memTotal = observe("memTotalBytes") {
            val started = android.os.SystemClock.elapsedRealtime()
            val bytes = File("/proc/meminfo").inputStream().use { input ->
                val buffer = ByteArray(8193)
                var count = 0
                while (count < buffer.size) {
                    val read = input.read(buffer, count, buffer.size - count)
                    if (read <= 0) break
                    count += read
                }
                require(count <= 8192)
                buffer.copyOf(count)
            }
            require(android.os.SystemClock.elapsedRealtime() - started < 250)
            memTotalBytes(String(bytes, Charsets.US_ASCII))
        }
        val manager = runCatching { context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager }.getOrNull()
        val lowRam = observe("lowRamDevice") { manager?.isLowRamDevice }
        val memoryClass = observe("memoryClassMiB") { manager?.memoryClass?.takeIf { it > 0 } }
        val qemu = observe("qemu") {
            val properties = Class.forName("android.os.SystemProperties")
            (properties.getMethod("get", String::class.java).invoke(null, "ro.kernel.qemu") as? String)
                ?.takeIf { it == "0" || it == "1" }
        }
        val emulator = observe("isPhysicalDevice") {
            val values = listOf(android.os.Build.FINGERPRINT, android.os.Build.MODEL,
                android.os.Build.HARDWARE, android.os.Build.PRODUCT)
            require(values.all { it.isNotBlank() })
            if (qemu == "1" || values.any { value -> listOf("generic", "emulator", "sdk_gphone", "google_sdk", "goldfish", "ranchu")
                .any { value.contains(it, ignoreCase = true) } }
            ) false else null
        }
        if (emulator == null) reasons["isPhysicalDevice"] = "unsupported"
        return mapOf("schemaVersion" to 1, "platform" to "android", "model" to model, "apiLevel" to api,
            "abis" to abis, "memTotalBytes" to memTotal, "lowRamDevice" to lowRam,
            "memoryClassMiB" to memoryClass, "isPhysicalDevice" to emulator, "qemu" to qemu, "unavailableReasons" to reasons)
    }
}
