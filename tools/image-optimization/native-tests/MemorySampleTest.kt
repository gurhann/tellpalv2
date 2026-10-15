package com.tellpal.imageexperiment

import android.os.Debug
import android.os.SystemClock

fun main() {
    val cases = listOf(
        Triple(12345, 12641280L, "none"),
        Triple(0, 0L, "none"),
        Triple(Int.MAX_VALUE, 2199023254528L, "none"),
        Triple(-1, null, "missing_counter")
    )
    for ((pss, expectedBytes, reason) in cases) {
        Debug.pss = pss
        Debug.fail = false
        val result = ImageExperimentMemory.sample()
        check(result["memoryBytes"] == expectedBytes) { "bytes for $pss: $result" }
        check(result["memoryReason"] == reason)
        check(result["memoryCounter"] == "android_total_pss")
        check(result["nativeReadMicroseconds"] == 7L)
        check(result["nativeResultCode"] == null && result["nativeResultCount"] == null)
        check(result["thermalState"] == "unavailable")
    }
    Debug.fail = true
    val failed = ImageExperimentMemory.sample()
    check(failed["memoryBytes"] == null && failed["memoryReason"] == "sample_failed")
    check(failed["nativeReadMicroseconds"] == 7L)
    check(Debug.reads == 5 && SystemClock.nanos == 70000L)
    println("PASS: actual ImageExperimentMemory.sample; 5 cases; 5 OS reads; no skips")
}
