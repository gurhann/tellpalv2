# Same-signed instrumentation binds the target process bootstrap API by class name.
-keep class com.tellpal.imageexperiment.** { *; }

# Separately packaged instrumentation calls the target APK's shared stdlib.
# Target-only R8 reachability cannot see those calls or their inherited APIs.
-keep class kotlin.** { *; }
