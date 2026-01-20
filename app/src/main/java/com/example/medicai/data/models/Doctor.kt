package com.example.medicai.data.models

import kotlinx.serialization.Serializable

/**
 * Modelo de Médico
 */
@Serializable
data class Doctor(
    val id: String = "",
    val user_id: String,
    val doctor_name: String,
    val specialty: String,
    val created_at: String? = null,
    val updated_at: String? = null
)

/**
 * DTO para upsert de médicos (sin id, usa user_id+doctor_name como clave)
 */
@Serializable
data class DoctorUpsert(
    val user_id: String,
    val doctor_name: String,
    val specialty: String
)
