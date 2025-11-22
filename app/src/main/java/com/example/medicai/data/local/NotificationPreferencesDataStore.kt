package com.example.medicai.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationPrefsDataStore by preferencesDataStore(name = "notification_prefs")

object NotificationPreferencesDataStore {
    private val SOUND_KEY = booleanPreferencesKey("sound_enabled")
    private val VIBRATION_KEY = booleanPreferencesKey("vibration_enabled")

    fun soundEnabledFlow(context: Context): Flow<Boolean> =
        context.notificationPrefsDataStore.data.map { prefs: Preferences -> prefs[SOUND_KEY] ?: true }

    suspend fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.notificationPrefsDataStore.edit { prefs ->
            prefs[SOUND_KEY] = enabled
        }
    }

    fun vibrationEnabledFlow(context: Context): Flow<Boolean> =
        context.notificationPrefsDataStore.data.map { prefs: Preferences -> prefs[VIBRATION_KEY] ?: true }

    suspend fun setVibrationEnabled(context: Context, enabled: Boolean) {
        context.notificationPrefsDataStore.edit { prefs ->
            prefs[VIBRATION_KEY] = enabled
        }
    }
}
