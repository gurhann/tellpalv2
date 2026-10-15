package android.os

/** Configurable OS boundary only; production sampling and conversion remain unchanged. */
object Debug {
    var pss: Int = 0
    var fail: Boolean = false
    var reads: Int = 0
    class MemoryInfo { var totalPss: Int = 0 }
    fun getMemoryInfo(info: MemoryInfo) {
        reads++
        if (fail) throw IllegalStateException("synthetic reader failure")
        info.totalPss = pss
    }
}

object SystemClock {
    var nanos: Long = 0
    fun elapsedRealtimeNanos(): Long {
        val result = nanos
        nanos += 7000L
        return result
    }
}
