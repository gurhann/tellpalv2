package android.content

class Intent {
    var className: String = ""
    var packageName: String = ""
    var flags: Int = 0
    private val extras = mutableMapOf<String, String>()
    fun setClassName(packageName: String, className: String) = apply { this.packageName = packageName; this.className = className }
    fun addFlags(flags: Int) = apply { this.flags = this.flags or flags }
    fun putExtra(key: String, value: String) = apply { extras[key] = value }
    fun getStringExtra(key: String): String? = extras[key]
    fun removeExtra(key: String) { extras.remove(key) }
    companion object { const val FLAG_ACTIVITY_NEW_TASK = 0x10000000 }
}
