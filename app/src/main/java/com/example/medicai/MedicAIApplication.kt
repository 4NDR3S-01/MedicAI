package com.example.medicai

import android.app.Application
import com.example.medicai.data.remote.SupabaseClient
import com.example.medicai.notifications.MedicAINotificationManager

/**
 * Clase Application personalizada para MedicAI
 * Se ejecuta cuando la app se inicia
 * ✅ Implementa singleton para acceso global al contexto de la aplicación
 */
class MedicAIApplication : Application() {

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
    }
}
