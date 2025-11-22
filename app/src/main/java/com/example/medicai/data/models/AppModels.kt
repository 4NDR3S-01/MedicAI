package com.example.medicai.data.models

import kotlinx.serialization.Serializable


/**
 * Modelo de recordatorio de medicamento
 */
@Serializable
data class MedicineReminder(
    val id: String? = null,
    val medicine_id: String,
    val user_id: String,
    val scheduled_time: String,
    val taken: Boolean = false,
    val taken_at: String? = null,
    val skipped: Boolean = false,
    val created_at: String? = null
)

/**
 * Modelo de conversación con IA
 */
@Serializable
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val user_id: String = "",
    val role: String, // "user" o "assistant"
    val content: String,
    val timestamp: String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
    val session_id: String? = null
) {
    val isUser: Boolean
        get() = role == "user"

    val text: String
        get() = content
}

/**
 * Request para consulta a IA
 */
data class AIQueryRequest(
    val query: String,
    val context: String? = null
)

/**
 * Response de IA
 */
data class AIQueryResponse(
    val response: String,
    val confidence: Float? = null,
    val sources: List<String>? = null
)

