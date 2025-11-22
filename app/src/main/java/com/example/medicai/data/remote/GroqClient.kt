package com.example.medicai.data.remote

import android.util.Log
import com.example.medicai.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Cliente para la API de Groq
 * Usa el modelo llama-3.3-70b-versatile que es muy rápido y preciso
 */
object GroqClient {

    // ✅ API Key obtenida desde BuildConfig (configurada en local.properties)
    private val GROQ_API_KEY = BuildConfig.GROQ_API_KEY
    private const val GROQ_BASE_URL = "https://api.groq.com/openai/v1"

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("GroqClient", message)
                }
            }
            level = LogLevel.INFO
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
        }

        defaultRequest {
            header("Authorization", "Bearer $GROQ_API_KEY")
            header("Content-Type", "application/json")
        }
    }

    /**
     * Enviar mensaje a Groq y obtener respuesta
     */
    suspend fun sendMessage(
        userMessage: String,
        systemPrompt: String = MEDICAL_SYSTEM_PROMPT
    ): String {
        return try {
            Log.d("GroqClient", "📤 Enviando mensaje a Groq...")

            val request = GroqChatRequest(
                model = "llama-3.3-70b-versatile", // Modelo más rápido y eficiente
                messages = listOf(
                    GroqMessage(role = "system", content = systemPrompt),
                    GroqMessage(role = "user", content = userMessage)
                ),
                temperature = 0.7,
                max_tokens = 1024,
                top_p = 1.0,
                stream = false
            )

            val response = client.post("$GROQ_BASE_URL/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val groqResponse: GroqChatResponse = response.body()
            val aiMessage = groqResponse.choices.firstOrNull()?.message?.content
                ?: "Lo siento, no pude generar una respuesta."

            Log.d("GroqClient", "✅ Respuesta recibida de Groq")
            aiMessage

        } catch (e: Exception) {
            Log.e("GroqClient", "❌ Error al llamar a Groq: ${e.message}", e)
            throw Exception("Error de conexión con el asistente IA: ${e.message}")
        }
    }

    /**
     * Cerrar el cliente HTTP
     */
    fun close() {
        client.close()
    }

    // Prompt del sistema optimizado para asistente médico
    private const val MEDICAL_SYSTEM_PROMPT = """
Eres un asistente médico virtual experto y compasivo llamado MedicAI. Tu función es proporcionar información médica precisa, actualizada y basada en evidencia.

**REGLAS IMPORTANTES:**
1. Proporciona información médica precisa y basada en evidencia científica
2. Usa un lenguaje claro, amable y comprensible para el paciente
3. Incluye emojis relevantes para mejorar la experiencia (💊 🩺 ⚠️ 📊 etc.)
4. Estructura tus respuestas con secciones claras usando markdown
75. **SIEMPRE incluye al final de CADA respuesta esta advertencia:**
   "⚠️ **IMPORTANTE:** Esta información es orientativa y educativa. No reemplaza una consulta médica profesional. Ante cualquier duda o síntoma persistente, consulta con tu médico."
6. NO diagnostiques enfermedades específicas
7. NO reemplaces la consulta con un médico profesional
8. Si la pregunta es sobre síntomas graves o emergencia, recomienda buscar atención médica INMEDIATA
9. Proporciona información sobre:
   - Medicamentos (dosis, efectos secundarios, interacciones)
   - Síntomas y condiciones comunes (información general)
   - Primeros auxilios básicos
   - Prevención y estilos de vida saludables
   - Gestión de medicamentos y recordatorios

**FORMATO DE RESPUESTA:**
- Usa **negritas** para términos importantes
- Usa listas con • para puntos clave
- Incluye secciones con emojis descriptivos
- Mantén respuestas concisas pero completas (máximo 400 palabras)
- Termina SIEMPRE con la advertencia médica obligatoria
- Si es relevante, sugiere consultar al médico

**ADVERTENCIA MÉDICA OBLIGATORIA:**
Incluye AL FINAL de CADA respuesta:
"⚠️ **IMPORTANTE:** Esta información es orientativa y educativa. No reemplaza una consulta médica profesional. Ante cualquier duda o síntoma persistente, consulta con tu médico."

Responde en español de forma clara, profesional y empática.
"""
}

// Modelos de datos para Groq API
@Serializable
data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1024,
    val top_p: Double = 1.0,
    val stream: Boolean = false
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqChatResponse(
    val id: String,
    val model: String,
    val choices: List<GroqChoice>,
    val usage: GroqUsage? = null
)

@Serializable
data class GroqChoice(
    val index: Int,
    val message: GroqMessage,
    val finish_reason: String? = null
)

@Serializable
data class GroqUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

