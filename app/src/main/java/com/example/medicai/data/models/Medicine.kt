package com.example.medicai.data.models

import kotlinx.serialization.Serializable

/**
 * Modelo de Medicamento
 */
@Serializable
data class Medicine(
    val id: String = "",
    val user_id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val times: List<String>,
    val start_date: String,
    val end_date: String? = null,
    val notes: String? = null,
    val active: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

/**
 * DTO para crear/actualizar medicamento
 */
@Serializable
data class MedicineRequest(
    val user_id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val times: List<String>,
    val start_date: String,
    val end_date: String? = null,
    val notes: String? = null,
    val active: Boolean = true
)

/**
 * DTO para upsert de medicamentos (incluye id)
 */
@Serializable
data class MedicineUpsert(
    val id: String,
    val user_id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val times: List<String>,
    val start_date: String,
    val end_date: String?,
    val notes: String?,
    val active: Boolean
)

