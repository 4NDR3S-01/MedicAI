package com.example.medicai.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException

/**
 * Gestor centralizado de preferencias de usuario con encriptación
 * Proporciona acceso síncrono a preferencias para uso en BroadcastReceivers y otros componentes
 * ✅ Usa EncryptedSharedPreferences para proteger datos sensibles
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
     * Si falla la creación (por ejemplo, en dispositivos sin soporte), usa SharedPreferences normal como fallback
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return try {
            // Crear MasterKey para encriptación
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            // Crear EncryptedSharedPreferences
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: IOException) {
            // Fallback a SharedPreferences normal si hay error (dispositivos muy antiguos)
            Log.w("UserPreferencesManager", "⚠️ No se pudo crear EncryptedSharedPreferences, usando SharedPreferences normal: ${e.message}")
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            // Cualquier otro error, usar fallback
            Log.e("UserPreferencesManager", "❌ Error al crear EncryptedSharedPreferences: ${e.message}", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    /**
     * Guardar ID del usuario actual
     */
    fun saveUserId(context: Context, userId: String) {
        getPrefs(context)
            .edit()
            .putString(KEY_USER_ID, userId)
            .apply()
        Log.d("UserPreferencesManager", "✅ User ID guardado: $userId")
    }
    
    /**
     * Obtener ID del usuario actual
     */
    fun getUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_ID, null)
    }
    
    /**
     * Limpiar ID del usuario (al hacer logout)
     */
    fun clearUserId(context: Context) {
        getPrefs(context)
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
        getPrefs(context)
            .edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, prefs.enabled)
            .putInt(KEY_REMINDER_MINUTES, prefs.reminderMinutes)
            .putBoolean(KEY_SOUND_ENABLED, prefs.soundEnabled)
            .putBoolean(KEY_VIBRATION_ENABLED, prefs.vibrationEnabled)
            .apply()
        
        Log.d("UserPreferencesManager", """
            ✅ Preferencias guardadas:
            - Notificaciones: ${prefs.enabled}
            - Recordatorio: ${prefs.reminderMinutes} min
            - Sonido: ${prefs.soundEnabled}
            - Vibración: ${prefs.vibrationEnabled}
        """.trimIndent())
    }
    
    /**
     * Guardar solo notifications_enabled y reminder_minutes (para sync con Supabase)
     */
    fun saveNotificationSettings(
        context: Context,
        enabled: Boolean,
        reminderMinutes: Int
    ) {
        getPrefs(context)
            .edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            .putInt(KEY_REMINDER_MINUTES, reminderMinutes)
            .apply()
        
        Log.d("UserPreferencesManager", "✅ Configuración de notificaciones: enabled=$enabled, minutes=$reminderMinutes")
    }
    
    /**
     * Guardar solo sound_enabled
     */
    fun saveSoundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context)
            .edit()
            .putBoolean(KEY_SOUND_ENABLED, enabled)
            .apply()
        Log.d("UserPreferencesManager", "🔊 Sonido: $enabled")
    }
    
    /**
     * Guardar solo vibration_enabled
     */
    fun saveVibrationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context)
            .edit()
            .putBoolean(KEY_VIBRATION_ENABLED, enabled)
            .apply()
        Log.d("UserPreferencesManager", "📳 Vibración: $enabled")
    }
    
    /**
     * Obtener todas las preferencias de notificación
     */
    fun getNotificationPreferences(context: Context): NotificationPreferences {
        val prefs = getPrefs(context)
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
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }
    
    /**
     * Obtener minutos de anticipación para recordatorios
     */
    fun getReminderMinutes(context: Context): Int {
        return getPrefs(context).getInt(KEY_REMINDER_MINUTES, 15)
    }
    
    /**
     * Verificar si el sonido está habilitado
     */
    fun isSoundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SOUND_ENABLED, true)
    }
    
    /**
     * Verificar si la vibración está habilitada
     */
    fun isVibrationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VIBRATION_ENABLED, true)
    }
    
    /**
     * Limpiar todas las preferencias (al hacer logout)
     */
    fun clearAll(context: Context) {
        getPrefs(context)
            .edit()
            .clear()
            .apply()
        Log.d("UserPreferencesManager", "🗑️ Todas las preferencias eliminadas")
    }
}
