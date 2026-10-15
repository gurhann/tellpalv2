package android.os

object Build {
    const val MODEL = "synthetic-model"
    val SUPPORTED_ABIS = arrayOf("armeabi-v7a")
    const val FINGERPRINT = "synthetic-generic-emulator"
    const val HARDWARE = "synthetic"
    const val PRODUCT = "synthetic"
    object VERSION { const val SDK_INT = 30 }
}
object Debug {
    class MemoryInfo { val totalPss = 1024 }
    fun getMemoryInfo(info: MemoryInfo) {}
}
