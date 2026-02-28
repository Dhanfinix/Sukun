package dhanfinix.android.sukun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dhanfinix.android.sukun.core.database.entity.PrayerDay

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayer_day WHERE id = :id")
    suspend fun getPrayerDayById(id: String): PrayerDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<PrayerDay>)

    @Query("DELETE FROM prayer_day WHERE date < :beforeDate")
    suspend fun clearOldData(beforeDate: String)

    @Query("DELETE FROM prayer_day")
    suspend fun clearAll()
}
