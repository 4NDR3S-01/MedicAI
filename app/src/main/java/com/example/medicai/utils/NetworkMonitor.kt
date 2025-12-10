package com.example.medicai.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

/**
 * Utilidad para monitorear el estado de la conexión a internet
 */
object NetworkMonitor {
    
    /**
     * Verificar si hay conexión a internet disponible
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * Verificar si hay conexión a internet y loguear el estado
     */
    fun checkNetworkAvailability(context: Context): Boolean {
        val isAvailable = isNetworkAvailable(context)
        Log.d("NetworkMonitor", if (isAvailable) "✅ Conexión a internet disponible" else "❌ Sin conexión a internet")
        return isAvailable
    }
}
