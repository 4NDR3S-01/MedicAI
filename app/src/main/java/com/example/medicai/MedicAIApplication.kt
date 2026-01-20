package com.example.medicai

import android.app.Application
import com.example.medicai.data.remote.SupabaseClient
import com.example.medicai.data.repository.AppointmentRepository
import com.example.medicai.data.repository.MedicineRepository
import com.example.medicai.data.local.UserPreferencesManager
import com.example.medicai.utils.NetworkMonitor
import com.example.medicai.notifications.MedicAINotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Clase Application personalizada para MedicAI
 * Se ejecuta cuando la app se inicia
 * ✅ Implementa singleton para acceso global al contexto de la aplicación
 */
class MedicAIApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private lateinit var instance: MedicAIApplication
        
        /**
         * Obtener instancia de la aplicación
         * ✅ Permite acceso al contexto desde cualquier parte de la app
         */
        fun getInstance(): MedicAIApplication {
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Inicializar Supabase con contexto de la aplicación
        // Esto permite que guarde la sesión automáticamente
        SupabaseClient.initialize(this)

        // Crear canales de notificación
        MedicAINotificationManager.createNotificationChannels(this)

        // Sincronizar pendientes cuando vuelva el internet
        appScope.launch {
            NetworkMonitor.observeNetworkAvailability(this@MedicAIApplication).collect { isOnline ->
                if (isOnline) {
                    val userId = UserPreferencesManager.getUserId(this@MedicAIApplication)
                    if (userId != null) {
                        AppointmentRepository().getAppointments(userId)
                        MedicineRepository().getMedicines(userId)
                    }
                }
            }
        }
    }
}
