package dhanfinix.android.sukun.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dhanfinix.android.sukun.core.database.dao.PrayerDao
import dhanfinix.android.sukun.core.database.dao.SilentZoneDao
import dhanfinix.android.sukun.core.database.entity.PrayerDay
import dhanfinix.android.sukun.core.database.entity.SilentZone

@Database(entities = [PrayerDay::class, SilentZone::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class SukunDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao
    abstract fun silentZoneDao(): SilentZoneDao

    companion object {
        @Volatile
        private var INSTANCE: SukunDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE silent_zones ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'")
                db.execSQL("ALTER TABLE silent_zones ADD COLUMN externalId TEXT")
            }
        }

        fun getDatabase(context: Context): SukunDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SukunDatabase::class.java,
                    "sukun_database"
                )
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
