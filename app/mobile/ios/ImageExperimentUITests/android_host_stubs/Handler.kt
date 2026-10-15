package android.os

class Looper { companion object { fun getMainLooper() = Looper() } }
class Handler(looper: Looper) {
    private val pending = mutableMapOf<Runnable, java.util.concurrent.ScheduledFuture<*>>()
    fun postDelayed(operation: Runnable, delay: Long): Boolean {
        synchronized(pending) { pending[operation] = executor.schedule({
            com.tellpal.imageexperiment.host.HostRuntime.runOnUiThread { operation.run() }
        }, delay, java.util.concurrent.TimeUnit.MILLISECONDS) }
        return true
    }
    fun removeCallbacks(operation: Runnable) { synchronized(pending) { pending.remove(operation)?.cancel(false) } }
    companion object {
        private val executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "synthetic-handler").apply { isDaemon = true }
        }
    }
}
