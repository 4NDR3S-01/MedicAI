# 🔧 CÓDIGO DE CORRECCIONES - MedicAI

Este documento contiene el código específico para implementar las correcciones críticas identificadas en la revisión.

---

## 1. 🔴 CRÍTICO: Mover Claves API a BuildConfig

### Paso 1: Actualizar `build.gradle.kts`

```kotlin
// En app/build.gradle.kts, dentro de android.defaultConfig
defaultConfig {
    applicationId = "com.example.medicai"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    
    // ✅ AGREGAR: Claves de Supabase desde local.properties
    buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL") ?: ""}\"")
    buildConfigField("String", "SUPABASE_KEY", "\"${localProperties.getProperty("SUPABASE_KEY") ?: ""}\"")
    
    // ✅ Ya existe para Groq (verificar que esté así)
    buildConfigField("String", "GROQ_API_KEY", "\"${localProperties.getProperty("GROQ_API_KEY") ?: ""}\"")
    
    // ... resto de la configuración
}
```

### Paso 2: Actualizar `local.properties`

```properties
# Agregar estas líneas a local.properties (NO versionar en Git)
SUPABASE_URL=https://ntnvoyzjnvrnaevhqksu.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im50bnZveXpqbnZybmFldmhxa3N1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjIxODU1ODIsImV4cCI6MjA3Nzc2MTU4Mn0.oO4M3OX5qLRBXoUM7xaOwpEAHjAtUsJNGcNwnPS9eis
GROQ_API_KEY=tu_groq_api_key_aqui
```

### Paso 3: Actualizar `SupabaseClient.kt`

```kotlin
package com.example.medicai.data.remote

import android.content.Context
import com.example.medicai.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Cliente de Supabase - Singleton
 * ✅ SEGURO: Claves leídas desde BuildConfig (local.properties)
 */
object SupabaseClient {

    // ✅ Leer desde BuildConfig en lugar de constantes hardcodeadas
    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private val SUPABASE_KEY = BuildConfig.SUPABASE_KEY

    private var _client: SupabaseClient? = null

    fun initialize(context: Context) {
        if (_client == null) {
            // ✅ Validar que las claves no estén vacías
            require(SUPABASE_URL.isNotBlank()) { "SUPABASE_URL no configurada en local.properties" }
            require(SUPABASE_KEY.isNotBlank()) { "SUPABASE_KEY no configurada en local.properties" }
            
            _client = createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_KEY
            ) {
                install(Auth) {
                    autoLoadFromStorage = true
                    autoSaveToStorage = true
                    alwaysAutoRefresh = true
                }
                install(Postgrest)
                install(Realtime)
                install(Storage)
            }
        }
    }

    val client: SupabaseClient
        get() = _client ?: throw IllegalStateException(
            "SupabaseClient no ha sido inicializado. Llama a initialize(context) primero."
        )
}
```

---

## 2. 🔴 CRÍTICO: Habilitar ProGuard

### Actualizar `build.gradle.kts`

```kotlin
buildTypes {
    release {
        // ✅ HABILITAR minificación y ofuscación
        isMinifyEnabled = true
        isShrinkResources = true  // ✅ Reducir tamaño eliminando recursos no usados
        
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        
        // ✅ Agregar signing config si no existe
        // signingConfig = signingConfigs.getByName("release")
    }
    debug {
        isMinifyEnabled = false
        applicationIdSuffix = ".debug"
        versionNameSuffix = "-debug"
    }
}
```

### Actualizar `proguard-rules.pro`

```proguard
# Add project specific ProGuard rules here.

# ✅ Mantener información de línea para debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ✅ Supabase - Mantener clases necesarias
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ✅ Ktor Client
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class kotlinx.coroutines.** { *; }

# ✅ Kotlinx Serialization
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

# ✅ Mantener modelos de datos para serialización
-keep class com.example.medicai.data.models.** { *; }

# ✅ Mantener BuildConfig
-keep class com.example.medicai.BuildConfig { *; }

# ✅ Mantener clases de notificaciones
-keep class com.example.medicai.notifications.** { *; }

# ✅ Mantener Application class
-keep class com.example.medicai.MedicAIApplication { *; }

# ✅ Mantener ViewModels (opcional, para debugging)
-keep class com.example.medicai.viewmodel.** { *; }

# ✅ Mantener Repositories (opcional)
-keep class com.example.medicai.data.repository.** { *; }

# ✅ Coil (carga de imágenes)
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# ✅ Markdown renderer
-keep class dev.jeziellago.compose.markdowntext.** { *; }

# ✅ WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# ✅ DataStore
-keep class androidx.datastore.** { *; }

# ✅ Google Play Services Location
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.location.**
```

---

## 3. 🟡 IMPORTANTE: Migrar a EncryptedSharedPreferences

### Actualizar `build.gradle.kts` (dependencias)

```kotlin
dependencies {
    // ... otras dependencias
    
    // ✅ Agregar para EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // ... resto de dependencias
}
```

### Actualizar `UserPreferencesManager.kt`

```kotlin
package com.example.medicai.data.local

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Gestor centralizado de preferencias de usuario
 * ✅ SEGURO: Usa EncryptedSharedPreferences para datos sensibles
 */
object UserPreferencesManager {
    private const val PREFS_NAME = "medicai_user_prefs"
    
    // Keys
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_REMINDER_MINUTES = "reminder_minutes"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    
    /**
     * Modelo de preferencias de notificación
     */
    data class NotificationPreferences(
        val enabled: Boolean = true,
        val reminderMinutes: Int = 15,
        val soundEnabled: Boolean = true,
        val vibrationEnabled: Boolean = true
    )
    
    /**
     * Obtener EncryptedSharedPreferences
     * ✅ SEGURO: Datos encriptados automáticamente
     */
    private fun getEncryptedPrefs(context: Context): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }
    
    /**
     * Guardar ID del usuario actual
     * ✅ Datos encriptados
     */
    fun saveUserId(context: Context, userId: String) {
        getEncryptedPrefs(context)
            .edit()
            .putString(KEY_USER_ID, userId)
            .apply()
        Log.d("UserPreferencesManager", "✅ User ID guardado (encriptado): ${userId.take(8)}...")
    }
    
    /**
     * Obtener ID del usuario actual
     */
    fun getUserId(context: Context): String? {
        return getEncryptedPrefs(context).getString(KEY_USER_ID, null)
    }
    
    /**
     * Limpiar ID del usuario (al hacer logout)
     */
    fun clearUserId(context: Context) {
        getEncryptedPrefs(context)
            .edit()
            .remove(KEY_USER_ID)
            .apply()
        Log.d("UserPreferencesManager", "🗑️ User ID eliminado")
    }
    
    /**
     * Guardar todas las preferencias de notificación
     */
    fun saveNotificationPreferences(
        context: Context,
        prefs: NotificationPreferences
    ) {
        getEncryptedPrefs(context)
            .edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, prefs.enabled)
            .putInt(KEY_REMINDER_MINUTES, prefs.reminderMinutes)
            .putBoolean(KEY_SOUND_ENABLED, prefs.soundEnabled)
            .putBoolean(KEY_VIBRATION_ENABLED, prefs.vibrationEnabled)
            .apply()
        
        Log.d("UserPreferencesManager", """
            ✅ Preferencias guardadas (encriptadas):
            - Notificaciones: ${prefs.enabled}
            - Recordatorio: ${prefs.reminderMinutes} min
            - Sonido: ${prefs.soundEnabled}
            - Vibración: ${prefs.vibrationEnabled}
        """.trimIndent())
    }
    
    /**
     * Guardar solo notifications_enabled y reminder_minutes
     */
    fun saveNotificationSettings(
        context: Context,
        enabled: Boolean,
        reminderMinutes: Int
    ) {
        getEncryptedPrefs(context)
            .edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            .putInt(KEY_REMINDER_MINUTES, reminderMinutes)
            .apply()
        
        Log.d("UserPreferencesManager", "✅ Configuración de notificaciones (encriptada): enabled=$enabled, minutes=$reminderMinutes")
    }
    
    /**
     * Guardar solo sound_enabled
     */
    fun saveSoundEnabled(context: Context, enabled: Boolean) {
        getEncryptedPrefs(context)
            .edit()
            .putBoolean(KEY_SOUND_ENABLED, enabled)
            .apply()
        Log.d("UserPreferencesManager", "🔊 Sonido: $enabled")
    }
    
    /**
     * Guardar solo vibration_enabled
     */
    fun saveVibrationEnabled(context: Context, enabled: Boolean) {
        getEncryptedPrefs(context)
            .edit()
            .putBoolean(KEY_VIBRATION_ENABLED, enabled)
            .apply()
        Log.d("UserPreferencesManager", "📳 Vibración: $enabled")
    }
    
    /**
     * Obtener todas las preferencias de notificación
     */
    fun getNotificationPreferences(context: Context): NotificationPreferences {
        val prefs = getEncryptedPrefs(context)
        return NotificationPreferences(
            enabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true),
            reminderMinutes = prefs.getInt(KEY_REMINDER_MINUTES, 15),
            soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        )
    }
    
    /**
     * Verificar si las notificaciones están habilitadas
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }
    
    /**
     * Obtener minutos de anticipación para recordatorios
     */
    fun getReminderMinutes(context: Context): Int {
        return getEncryptedPrefs(context).getInt(KEY_REMINDER_MINUTES, 15)
    }
    
    /**
     * Verificar si el sonido está habilitado
     */
    fun isSoundEnabled(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean(KEY_SOUND_ENABLED, true)
    }
    
    /**
     * Verificar si la vibración está habilitada
     */
    fun isVibrationEnabled(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean(KEY_VIBRATION_ENABLED, true)
    }
    
    /**
     * Limpiar todas las preferencias (al hacer logout)
     */
    fun clearAll(context: Context) {
        getEncryptedPrefs(context)
            .edit()
            .clear()
            .apply()
        Log.d("UserPreferencesManager", "🗑️ Todas las preferencias eliminadas")
    }
}
```

---

## 4. 🟡 IMPORTANTE: Mejorar Validaciones

### Actualizar `ValidationUtils.kt`

```kotlin
package com.example.medicai.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidades y constantes compartidas para validación de formularios
 * ✅ MEJORADO: Validaciones más robustas
 */
object ValidationUtils {

    /**
     * Regex mejorado para validar formato de email
     * ✅ Valida dominio con extensión
     */
    val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    )

    /**
     * Longitud mínima de contraseña
     */
    const val MIN_PASSWORD_LENGTH = 8

    /**
     * Longitud máxima de contraseña (seguridad)
     */
    const val MAX_PASSWORD_LENGTH = 128

    /**
     * Longitud mínima de teléfono (dígitos)
     */
    const val MIN_PHONE_DIGITS = 9

    /**
     * Longitud máxima de teléfono
     */
    const val MAX_PHONE_DIGITS = 15

    /**
     * Valida si un email tiene formato correcto
     * ✅ Mejorado: Valida dominio real
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        return EMAIL_REGEX.matches(email.trim())
    }

    /**
     * Valida si una contraseña cumple los requisitos mínimos
     * ✅ Mejorado: Verifica longitud máxima también
     */
    fun isValidPassword(password: String): Boolean {
        return password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH
    }

    /**
     * Valida si una contraseña es fuerte (opcional, para futuras mejoras)
     */
    fun isStrongPassword(password: String): Boolean {
        if (!isValidPassword(password)) return false
        
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        
        // Requiere al menos 3 de 4 criterios
        return listOf(hasUpperCase, hasLowerCase, hasDigit, hasSpecialChar)
            .count { it } >= 3
    }

    /**
     * Valida si un número de teléfono tiene suficientes dígitos
     * ✅ Mejorado: Valida rango de longitud
     */
    fun isValidPhone(phone: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        return digits.length in MIN_PHONE_DIGITS..MAX_PHONE_DIGITS
    }

    /**
     * Valida teléfono internacional (con código de país)
     */
    fun isValidPhoneInternational(phone: String): Boolean {
        val cleaned = phone.replace(Regex("[^+0-9]"), "")
        return cleaned.startsWith("+") && 
               cleaned.length in (MIN_PHONE_DIGITS + 1)..(MAX_PHONE_DIGITS + 1)
    }

    /**
     * Verifica que dos contraseñas coincidan
     */
    fun passwordsMatch(password: String, confirm: String): Boolean {
        return password == confirm
    }

    /**
     * Valida si una fecha es futura (para citas)
     * ✅ NUEVO
     */
    fun isFutureDate(date: String, dateFormat: String = "yyyy-MM-dd"): Boolean {
        return try {
            val format = SimpleDateFormat(dateFormat, Locale.getDefault())
            format.isLenient = false
            val inputDate = format.parse(date) ?: return false
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            inputDate.after(today)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Valida formato de hora (HH:mm)
     * ✅ NUEVO
     */
    fun isValidTime(time: String): Boolean {
        return try {
            val parts = time.split(":")
            if (parts.size != 2) return false
            
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            
            hour in 0..23 && minute in 0..59
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Valida que un nombre no esté vacío y tenga longitud razonable
     * ✅ NUEVO
     */
    fun isValidName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.isNotBlank() && trimmed.length >= 2 && trimmed.length <= 100
    }
}
```

---

## 5. 🟡 IMPORTANTE: Agregar Manejo Offline Básico

### Crear `NetworkMonitor.kt`

```kotlin
package com.example.medicai.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Monitor de conectividad de red
 * ✅ Detecta cambios en la conexión a internet
 */
object NetworkMonitor {
    
    /**
     * Verificar si hay conexión a internet disponible
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as ConnectivityManager
        
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Observar cambios en la conectividad
     * ✅ Flow que emite true/false según estado de conexión
     */
    fun observeNetworkStatus(context: Context): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as ConnectivityManager
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            
            override fun onLost(network: Network) {
                trySend(false)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                ) && networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                trySend(hasInternet)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        // Emitir estado inicial
        trySend(isNetworkAvailable(context))
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}
```

### Actualizar Repository para usar NetworkMonitor

```kotlin
// Ejemplo en MedicineRepository.kt
suspend fun getMedicines(userId: String): Result<List<Medicine>> {
    return try {
        // ✅ Verificar conexión antes de hacer llamada
        val context = MedicAIApplication.getInstance()
        if (!NetworkMonitor.isNetworkAvailable(context)) {
            return Result.Error("Sin conexión a internet. Por favor verifica tu conexión.")
        }
        
        // ... resto del código existente
    } catch (e: java.io.IOException) {
        Result.Error("Error de conexión: ${e.message}")
    } catch (e: Exception) {
        Result.Error("Error inesperado: ${e.message}")
    }
}
```

---

## 6. 🟢 MEJORA: Optimizar Compose con derivedStateOf

### Ejemplo en `HomeScreen.kt`

```kotlin
// ❌ ANTES (línea 582-597)
val nextDose = remember(medicine.times, currentTime) {
    medicine.times
        .map { timeStr ->
            val parts = timeStr.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            hour * 60 + minute
        }
        .filter { it >= currentTime }
        .minOrNull() ?: // ...
}

// ✅ DESPUÉS (optimizado)
val nextDose by remember {
    derivedStateOf {
        val currentTimeMinutes = remember {
            val cal = java.util.Calendar.getInstance()
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = cal.get(java.util.Calendar.MINUTE)
            hour * 60 + minute
        }
        
        medicine.times
            .mapNotNull { timeStr ->
                val parts = timeStr.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                hour * 60 + minute
            }
            .filter { it >= currentTimeMinutes }
            .minOrNull() ?: medicine.times.firstOrNull()?.let { timeStr ->
                val parts = timeStr.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                hour * 60 + minute
            } ?: 0
    }
}
```

---

## 📝 NOTAS IMPORTANTES

1. **local.properties**: Asegúrate de que este archivo esté en `.gitignore` y nunca se suba a Git
2. **ProGuard**: Prueba el build release después de habilitar ProGuard para verificar que no rompe nada
3. **EncryptedSharedPreferences**: Los datos existentes en SharedPreferences no se migran automáticamente, considera una migración manual si es necesario
4. **Testing**: Después de cada cambio, prueba exhaustivamente la funcionalidad afectada

---

## ✅ CHECKLIST DE IMPLEMENTACIÓN

- [ ] Actualizar `build.gradle.kts` con BuildConfig para Supabase
- [ ] Agregar claves a `local.properties`
- [ ] Actualizar `SupabaseClient.kt`
- [ ] Habilitar ProGuard en `build.gradle.kts`
- [ ] Actualizar `proguard-rules.pro`
- [ ] Agregar dependencia de `security-crypto`
- [ ] Migrar `UserPreferencesManager.kt` a EncryptedSharedPreferences
- [ ] Actualizar `ValidationUtils.kt` con validaciones mejoradas
- [ ] Crear `NetworkMonitor.kt`
- [ ] Actualizar repositorios para usar NetworkMonitor
- [ ] Probar build release
- [ ] Probar funcionalidad completa

---

**Tiempo estimado de implementación:** 4-6 horas
