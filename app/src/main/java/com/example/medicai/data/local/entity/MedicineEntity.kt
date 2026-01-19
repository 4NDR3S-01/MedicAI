package com.example.medicai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.medicai.data.local.converters.ListConverters
import com.example.medicai.data.models.Medicine

/**
 * Entidad Room para Medicamentos
 * Mapea la tabla 'medicines' en la base de datos local
 */
@Entity(tableName = "medicines")
@TypeConverters(ListConverters::class)
data class MedicineEntity(
    @PrimaryKey
    val id: String,
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
    val updated_at: String? = null,
    val is_synced: Boolean = false // Indica si está sincronizado con Supabase
)

/**
 * Convertir de Entity a modelo de dominio
 */
fun MedicineEntity.toMedicine(): Medicine {
    return Medicine(
        id = id,
        user_id = user_id,
        name = name,
        dosage = dosage,
        frequency = frequency,
        times = times,
        start_date = start_date,
        end_date = end_date,
        notes = notes,
        active = active,
        created_at = created_at,
        updated_at = updated_at
    )
}

/**
 * Convertir de modelo de dominio a Entity
 */
fun Medicine.toEntity(isSynced: Boolean = true): MedicineEntity {
    return MedicineEntity(
        id = id,
        user_id = user_id,
        name = name,
        dosage = dosage,
        frequency = frequency,
        times = times,
        start_date = start_date,
        end_date = end_date,
        notes = notes,
        active = active,
        created_at = created_at,
        updated_at = updated_at,
        is_synced = isSynced
    )
}
