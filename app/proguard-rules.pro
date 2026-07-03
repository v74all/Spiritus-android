# Keep Ktor HTTP client classes
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep kotlinx.serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-dontwarn kotlinx.serialization.**

# Keep Room entities + DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.**

# Keep Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Keep ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep data models (DTOs)
-keep class com.v7lthronyx.v7lpanel.data.api.models.** { *; }
-keep class com.v7lthronyx.v7lpanel.domain.model.** { *; }
-keep class com.v7lthronyx.v7lpanel.data.db.entities.** { *; }

# Keep Application + MainActivity + Session
-keep class com.v7lthronyx.v7lpanel.V7LApplication { *; }
-keep class com.v7lthronyx.v7lpanel.MainActivity { *; }
-keep class com.v7lthronyx.v7lpanel.DeepLinkData { *; }
-keep class com.v7lthronyx.v7lpanel.SessionHolder { *; }
-keep class com.v7lthronyx.v7lpanel.service.** { *; }

# Keep VPN classes
-keep class com.v7lthronyx.v7lpanel.vpn.** { *; }

# Keep OkHttp (used by Ktor engine)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# General Android
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-dontwarn sun.misc.Unsafe
-dontwarn org.slf4j.impl.StaticLoggerBinder

# ── Privacy: strip verbose/debug/info logging from release builds ──
# Prevents the subscription UUID, server URL, and similar values from ever
# reaching logcat in production. Log.w/e are kept for crash diagnostics.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
