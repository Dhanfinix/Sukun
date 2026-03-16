package dhanfinix.android.sukun.core.database.dao

import androidx.room.*
import dhanfinix.android.sukun.core.database.entity.SilentZone
import kotlinx.coroutines.flow.Flow

@Dao
interface SilentZoneDao {
    @Query("SELECT * FROM silent_zones")
    fun getAllSilentZones(): Flow<List<SilentZone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSilentZone(silentZone: SilentZone): Long

    @Delete
    suspend fun deleteSilentZone(silentZone: SilentZone)

    @Query("UPDATE silent_zones SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateEnabledStatus(id: Long, isEnabled: Boolean)

    @Query("UPDATE silent_zones SET isAutoSilent = :isAutoSilent WHERE id = :id")
    suspend fun updateAutoSilentStatus(id: Long, isAutoSilent: Boolean)

    @Update
    suspend fun updateSilentZone(silentZone: SilentZone)

    @Query("SELECT * FROM silent_zones WHERE id = :id")
    suspend fun getSilentZoneById(id: Long): SilentZone?

    @Query("SELECT * FROM silent_zones WHERE isEnabled = 1")
    suspend fun getEnabledSilentZones(): List<SilentZone>
}
