package android.os

object SystemClock {
    fun elapsedRealtime(): Long = System.nanoTime() / 1_000_000 + com.tellpal.imageexperiment.host.HostRuntime.elapsedOffset
}
