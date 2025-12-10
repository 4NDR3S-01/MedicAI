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
 * 
 * Las credenciales se obtienen desde BuildConfig (configuradas en local.properties)
 * Esto evita exponer las claves API en el código fuente.
 */
object SupabaseClient {

    // ✅ Credenciales obtenidas desde BuildConfig (configuradas en local.properties)
    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private val SUPABASE_KEY = BuildConfig.SUPABASE_KEY

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

