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
-dontwarn kotlinx.coroutines.**