package dhanfinix.android.sukun.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dhanfinix.android.sukun.core.database.dao.PrayerDao
import dhanfinix.android.sukun.core.database.dao.SilentZoneDao
import dhanfinix.android.sukun.core.database.entity.PrayerDay
import dhanfinix.android.sukun.core.database.entity.SilentZone

@Database(entities = [PrayerDay::class, SilentZone::class], version = 4, exportSchema = false)
abstract class SukunDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao
    abstract fun silentZoneDao(): SilentZoneDao

    companion object {
        @Volatile
        private var INSTANCE: SukunDatabase? = null

        fun getDatabase(context: Context): SukunDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SukunDatabase::class.java,
                    "sukun_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
