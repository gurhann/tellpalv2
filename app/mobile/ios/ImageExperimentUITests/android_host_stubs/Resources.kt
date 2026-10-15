package android.content.res

class Resources { val configuration = Configuration() }
class Configuration {
    var orientation = ORIENTATION_PORTRAIT
    companion object { const val ORIENTATION_PORTRAIT = 1 }
}
