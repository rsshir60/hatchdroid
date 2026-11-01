# 1. Keep JNA core classes and native dispatch methods
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** {
    public *;
}
-keepclassmembers class * extends com.sun.jna.Structure {
    public *;
}
-keepclassmembers class * extends com.sun.jna.Callback {
    public *;
}
-dontwarn java.awt.**

# 2. Keep UniFFI generated bindings & Rust progress callbacks
-keep class uniffi.** { *; }
-keep class dev.hatchzip.android.** { *; }
-keep interface uniffi.** { *; }
