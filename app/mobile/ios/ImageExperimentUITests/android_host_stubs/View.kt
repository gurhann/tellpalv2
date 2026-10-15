package android.view

open class View(context: android.content.Context) {
    var tag: Any? = null
    var contentDescription: CharSequence? = null
    var importantForAccessibility = 0
    companion object { const val IMPORTANT_FOR_ACCESSIBILITY_YES = 1 }
}
class ViewGroup(context: android.content.Context) : View(context) {
    class LayoutParams(val width: Int, val height: Int)
    private val children = mutableListOf<View>()
    fun addView(view: View, params: LayoutParams) { children += view }
    fun removeView(view: View) { children -= view }
    @Suppress("UNCHECKED_CAST")
    fun <T : View> findViewWithTag(tag: Any): T? = children.firstOrNull { it.tag == tag } as T?
}
