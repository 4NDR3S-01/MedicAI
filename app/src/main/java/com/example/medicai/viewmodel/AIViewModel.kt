package com.example.medicai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicai.data.models.ChatMessage
import com.example.medicai.data.remote.GroqClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel para el Asistente IA con integración de Groq API
 */
class AIViewModel : ViewModel() {

    // Estado de mensajes
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Estado de error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val sessionId = UUID.randomUUID().toString()


    /**
     * Enviar mensaje del usuario y obtener respuesta de la IA
     */
    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) {
            Log.w("AIViewModel", "⚠️ Mensaje vacío, no se envía")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Agregar mensaje del usuario
                val userChatMessage = ChatMessage(
                    role = "user",
                    content = userMessage.trim(),
                    session_id = sessionId
                )
                _messages.value = _messages.value + userChatMessage

                Log.d("AIViewModel", "📤 Enviando mensaje: $userMessage")
                Log.d("AIViewModel", "📊 Total mensajes en conversación: ${_messages.value.size}")

                // Construir historial conversacional para mantener contexto
                val conversationHistory = _messages.value.map { msg ->
                    com.example.medicai.data.remote.GroqMessage(
                        role = msg.role,
                        content = msg.content
                    )
                }

                Log.d("AIViewModel", "🔄 Enviando ${conversationHistory.size} mensajes de contexto a la IA")

                // Llamar a la API de Groq con contexto completo
                val aiResponse = GroqClient.sendMessageWithContext(conversationHistory)

                // Agregar respuesta de la IA
                val aiChatMessage = ChatMessage(
                    role = "assistant",
                    content = aiResponse,
                    session_id = sessionId
                )
                _messages.value = _messages.value + aiChatMessage

                Log.d("AIViewModel", "✅ Respuesta recibida de Groq")
                Log.d("AIViewModel", "📊 Total mensajes: ${_messages.value.size}")

            } catch (e: Exception) {
                Log.e("AIViewModel", "❌ Error: ${e.message}", e)
                _error.value = "Error al comunicarse con el asistente: ${e.message}"

                // Agregar mensaje de error amigable
                val errorMessage = ChatMessage(
                    role = "assistant",
                    content = "❌ Lo siento, hubo un problema al procesar tu mensaje.\n\n" +
                            "**Posibles causas:**\n" +
                            "• Problema de conexión a internet\n" +
                            "• Servicio temporalmente no disponible\n\n" +
                            "Por favor, intenta de nuevo en un momento.",
                    session_id = sessionId
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Limpiar historial de chat
     */
    fun clearChat() {
        _messages.value = emptyList()
        Log.d("AIViewModel", "🗑️ Chat limpiado")
    }

    /**
     * Limpiar mensaje de error
     */
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        GroqClient.close()
    }
}

