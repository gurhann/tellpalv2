package com.tellpal.imageexperiment

import android.content.Context
import android.system.Os
import android.system.OsConstants
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Re-encodes an independently validated whitelist into this application's private container. */
object ImageExperimentLedger {
    private val hashes = setOf("caseSha256", "providerSha256", "attemptSha256", "sessionSha256", "sourceSha256", "referenceSha256", "settingsSha256", "scopeSha256")
    private val integers = setOf("generation", "monotonicMicroseconds", "requestCount", "retryCount", "bodyBytes",
        "sourceWidth", "sourceHeight", "requiredWidth", "requiredHeight", "decodeWidth", "decodeHeight",
        "actualDecodeWidth", "actualDecodeHeight", "frameNumber", "dartHeapBytes", "dartExternalBytes",
        "decodedLruBytes", "decodedLiveCount", "decodedPendingCount", "processBytes", "nativeBytes",
        "graphicsBytes", "diskBytes", "transferBytes", "firstRasterizedTargetFrame", "observerOverheadMicroseconds", "memoryBytes", "nativeReadMicroseconds", "nativeResultCode", "nativeResultCount", "sampleStartMicroseconds", "sampleEndMicroseconds", "sampleCadenceMicroseconds")
    private val doubles = setOf("logicalSlotWidth", "logicalSlotHeight", "dpr", "alignmentX", "alignmentY", "observedSlotWidth", "observedSlotHeight", "observedDpr")
    private val booleans = setOf("encodedVerified", "sourceShortage", "physicalPresentationVerified")
    private val strings = mapOf(
        "phase" to setOf("idle", "baseline", "load", "hold", "releasing", "post_release", "complete"),
        "memoryCounter" to setOf("ios_phys_footprint", "android_total_pss", "unavailable"),
        "memoryReason" to setOf("none", "mach_result", "mach_count", "missing_counter", "sample_failed", "session_inactive", "sample_timeout"),
        "thermalState" to setOf("nominal", "fair", "serious", "critical", "unavailable"),
        "clockDomain" to setOf("dart_monotonic"), "clockUnit" to setOf("microseconds"),
        "fit" to setOf("contain", "cover"), "role" to setOf("isolated_raster_fixture"),
        "event" to setOf("request", "body", "encoded_verified", "decoded", "draw", "failure", "revoke", "cache_state",
            "status", "status_request", "status_body", "ownership", "phase", "memory_sample", "release_complete", "layout", "dispatch"),
        "ownership" to setOf("owned", "settling", "settled", "released", "unavailable"),
        "reason" to setOf("none", "invalid_fixture", "session_inactive", "status_unavailable", "status_revoked",
            "missing_counter", "unverified_visibility", "frame_unavailable", "timing_unavailable", "clock_unmapped",
            "retained_layer_ambiguity", "route_departed", "stale_attempt", "internal_settlement_unavailable",
            "export_capacity", "pin_refused", "transport_refused", "decode_refused", "cleanup_unavailable", "phase_budget_refused", "load_timeout", "layout_unavailable"),
        "counterReason" to setOf("missing_counter", "unverified_visibility", "frame_unavailable", "timing_unavailable",
            "clock_unmapped", "retained_layer_ambiguity", "internal_settlement_unavailable"),
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
                    field in booleans -> value is Boolean
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
