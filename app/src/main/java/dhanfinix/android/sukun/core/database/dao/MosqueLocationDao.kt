package dhanfinix.android.sukun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dhanfinix.android.sukun.core.database.entity.MosqueLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface MosqueLocationDao {
    @Query("SELECT * FROM mosque_location WHERE type = :type")
    fun getLocationsByType(type: String): Flow<List<MosqueLocation>>

    @Query("SELECT * FROM mosque_location")
    fun getAllLocations(): Flow<List<MosqueLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: MosqueLocation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<MosqueLocation>)

    @Query("DELETE FROM mosque_location WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM mosque_location WHERE type = :type")
    suspend fun deleteAllByType(type: String)

    @Query("DELETE FROM mosque_location")
    suspend fun clearAll()
}
