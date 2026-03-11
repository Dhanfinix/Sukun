package dhanfinix.android.sukun.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import dhanfinix.android.sukun.core.database.dao.MosqueLocationDao
import dhanfinix.android.sukun.core.database.dao.PrayerDao
import dhanfinix.android.sukun.core.database.entity.MosqueLocation

import dhanfinix.android.sukun.core.database.entity.PrayerDay

@Database(entities = [PrayerDay::class, MosqueLocation::class], version = 2, exportSchema = true)
abstract class SukunDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao
    abstract fun mosqueLocationDao(): MosqueLocationDao

    companion object {
        @Volatile
        private var INSTANCE: SukunDatabase? = null

        fun getDatabase(context: Context): SukunDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SukunDatabase::class.java,
                    "sukun_database"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }

    }
}
