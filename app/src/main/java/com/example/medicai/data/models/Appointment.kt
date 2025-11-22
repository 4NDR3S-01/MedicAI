package com.example.medicai.data.models

import kotlinx.serialization.Serializable

/**
 * Modelo de Cita Médica
 */
@Serializable
data class Appointment(
    val id: String = "",
    val user_id: String,
    val doctor_name: String,
    val specialty: String,
    val date: String,
    val time: String,
    val location: String,
    val notes: String? = null,
    val status: String = "scheduled",
    val reminder_sent: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null
)

/**
 * DTO para crear/actualizar cita
 */
@Serializable
data class AppointmentRequest(
    val user_id: String,
    val doctor_name: String,
    val specialty: String,
    val date: String,
    val time: String,
    val location: String,
    val notes: String? = null,
    val status: String = "scheduled"
)

