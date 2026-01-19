package com.example.medicai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.medicai.data.local.converters.ListConverters
import com.example.medicai.data.local.dao.AppointmentDao
import com.example.medicai.data.local.dao.MedicineDao
import com.example.medicai.data.local.dao.UserProfileDao
import com.example.medicai.data.local.entity.AppointmentEntity
import com.example.medicai.data.local.entity.MedicineEntity
import com.example.medicai.data.local.entity.UserProfileEntity

/**
 * Base de datos Room para MedicAI
 * Proporciona persistencia local para cache offline y sincronización con Supabase
 */
@Database(
    entities = [
        MedicineEntity::class,
        AppointmentEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(ListConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun medicineDao(): MedicineDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun userProfileDao(): UserProfileDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private const val DATABASE_NAME = "medicai_database"
        
        /**
         * Obtener instancia singleton de la base de datos
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // En producción, usar migraciones adecuadas
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Limpiar toda la base de datos (útil al cerrar sesión)
         */
        suspend fun clearAllTables(context: Context) {
            getInstance(context).clearAllTables()
        }
    }
}
