# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================
# Supabase
# ============================================
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**
-keep class io.github.jan.tennert.supabase.** { *; }
-dontwarn io.github.jan.tennert.supabase.**

# ============================================
# Ktor Client
# ============================================
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keepclassmembers class io.ktor.** { *; }

# ============================================
# Kotlinx Serialization
# ============================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *;
}
-keep,includedescriptorclasses class com.example.medicai.data.models.**$$serializer { *; }
-keepclassmembers class com.example.medicai.data.models.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.medicai.data.models.** {
    <init>(...);
}

# ============================================
# Modelos de datos de la aplicación
# ============================================
-keep class com.example.medicai.data.models.** { *; }
-keep class com.example.medicai.data.remote.** { *; }

# ============================================
# BuildConfig
# ============================================
-keepclassmembers class com.example.medicai.BuildConfig {
    public static final *;
}

# ============================================
# Coroutines
# ============================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============================================
# Jetpack Compose
# ============================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keepclassmembers class androidx.compose.** { *; }

# Compose Runtime
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.runtime.**

# Compose UI
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.ui.**

# ============================================
# Navigation Compose
# ============================================
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**
-keepclassmembers class androidx.navigation.** { *; }

# ============================================
# ViewModel y Lifecycle
# ============================================
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**
-keepclassmembers class androidx.lifecycle.** { *; }

# ViewModels específicos de la app
-keep class com.example.medicai.viewmodel.** { *; }

# ============================================
# Notifications y AlarmManager
# ============================================
-keep class com.example.medicai.notifications.** { *; }
-keep class * extends android.content.BroadcastReceiver {
    public <init>(...);
}

# ============================================
# Sensores
# ============================================
-keep class com.example.medicai.sensors.** { *; }

# ============================================
# Material 3
# ============================================
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.material3.**

# ============================================
# Repositorios
# ============================================
-keep class com.example.medicai.data.repository.** { *; }

# ============================================
# AppLogger (Logging)
# ============================================
-keep class com.example.medicai.utils.AppLogger { *; }