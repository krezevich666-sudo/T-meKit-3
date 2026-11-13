package lv.tamekit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Company::class, Customer::class, Material::class, StockMovement::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun company(): CompanyDao
    abstract fun customers(): CustomerDao
    abstract fun materials(): MaterialDao
    abstract fun movements(): MovementDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tamekit.db")
                // 1. iterācijas dati netiek pārnesti (lietotne vēl netiek lietota darbā).
                // Pirms reālas lietošanas šeit jāliek īstas migrācijas.
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
