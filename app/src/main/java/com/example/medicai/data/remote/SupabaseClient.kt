package com.example.medicai.data.remote

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Cliente de Supabase - Singleton
 *
 * IMPORTANTE: Reemplaza SUPABASE_URL y SUPABASE_KEY con tus credenciales reales
 * Las puedes obtener de: https://app.supabase.com/project/_/settings/api
 */
object SupabaseClient {

    // TODO: Reemplazar con tus credenciales de Supabase
    private const val SUPABASE_URL = "https://ntnvoyzjnvrnaevhqksu.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im50bnZveXpqbnZybmFldmhxa3N1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjIxODU1ODIsImV4cCI6MjA3Nzc2MTU4Mn0.oO4M3OX5qLRBXoUM7xaOwpEAHjAtUsJNGcNwnPS9eis"

    private var _client: SupabaseClient? = null

    fun initialize(context: Context) {
        if (_client == null) {
            _client = createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_KEY
            ) {
                install(Auth) {
                    // Configuración de persistencia de sesión
                    // Esto guarda la sesión automáticamente
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

