package com.example.medicai.utils

import android.content.Context
import android.net.Uri
import com.example.medicai.data.remote.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object AvatarUploadHelper {
    
    private const val BUCKET_NAME = "avatars"
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5 MB
    
    /**
     * Sube una imagen de avatar a Supabase Storage
     * @param context Contexto de la aplicación
     * @param imageUri URI de la imagen seleccionada
     * @param userId ID del usuario (para nombrar el archivo)
     * @return URL pública de la imagen subida o null si hubo error
     */
    suspend fun uploadAvatar(
        context: Context,
        imageUri: Uri,
        userId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Leer el archivo de la URI
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("No se pudo leer la imagen"))
            
            val bytes = inputStream.readBytes()
            inputStream.close()
            
            // 2. Validar tamaño
            if (bytes.size > MAX_FILE_SIZE) {
                return@withContext Result.failure(Exception("La imagen es muy grande. Máximo 5 MB."))
            }
            
            // 3. Generar nombre único para el archivo
            val timestamp = System.currentTimeMillis()
            val fileName = "${userId}_${timestamp}.jpg"
            
            // 4. Subir a Supabase Storage
            val bucket = SupabaseClient.client.storage.from(BUCKET_NAME)
            
            // Eliminar avatar anterior si existe
            try {
                val existingFiles = bucket.list(userId)
                existingFiles.forEach { file ->
                    bucket.delete("${userId}/${file.name}")
                }
            } catch (e: Exception) {
                // Ignorar si no hay archivos previos
            }
            
            // Subir nuevo avatar
            bucket.upload(
                path = "${userId}/${fileName}",
                data = bytes,
                upsert = true
            )
            
            // 5. Obtener URL pública
            val publicUrl = bucket.publicUrl("${userId}/${fileName}")
            
            Result.success(publicUrl)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Elimina el avatar del usuario de Supabase Storage
     */
    suspend fun deleteAvatar(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bucket = SupabaseClient.client.storage.from(BUCKET_NAME)
            
            // Listar y eliminar todos los archivos del usuario
            val files = bucket.list(userId)
            files.forEach { file ->
                bucket.delete("${userId}/${file.name}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
