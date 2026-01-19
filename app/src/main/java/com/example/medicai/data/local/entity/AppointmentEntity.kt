package com.example.medicai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.medicai.data.models.Appointment

/**
 * Entidad Room para Citas Médicas
 * Mapea la tabla 'appointments' en la base de datos local
 */
@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey
    val id: String,
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
    val updated_at: String? = null,
    val is_synced: Boolean = false // Indica si está sincronizado con Supabase
)

/**
 * Convertir de Entity a modelo de dominio
 */
fun AppointmentEntity.toAppointment(): Appointment {
    return Appointment(
        id = id,
        user_id = user_id,
        doctor_name = doctor_name,
        specialty = specialty,
        date = date,
        time = time,
        location = location,
        notes = notes,
        status = status,
        reminder_sent = reminder_sent,
        created_at = created_at,
        updated_at = updated_at
    )
}

/**
 * Convertir de modelo de dominio a Entity
 */
fun Appointment.toEntity(isSynced: Boolean = true): AppointmentEntity {
    return AppointmentEntity(
        id = id,
        user_id = user_id,
        doctor_name = doctor_name,
        specialty = specialty,
        date = date,
        time = time,
        location = location,
        notes = notes,
        status = status,
        reminder_sent = reminder_sent,
        created_at = created_at,
        updated_at = updated_at,
        is_synced = isSynced
    )
}
