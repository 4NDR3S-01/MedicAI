package com.example.medicai.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Receiver para reiniciar alarmas después del reinicio del dispositivo
 * Utiliza WorkManager para ejecutar la reprogramación en background
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.d("BootReceiver", "📱 Dispositivo reiniciado. Iniciando reprogramación de alarmas...")

            // ✅ Usar WorkManager para reprogramar alarmas en background
            try {
                val workRequest = OneTimeWorkRequestBuilder<AlarmReschedulerWorker>()
                    .build()
                
                WorkManager.getInstance(context).enqueue(workRequest)
                Log.d("BootReceiver", "✅ Worker de reprogramación encolado correctamente")
                
            } catch (e: Exception) {
                Log.e("BootReceiver", "❌ Error encolando worker de reprogramación: ${e.message}", e)
            }
        }
    }
}

