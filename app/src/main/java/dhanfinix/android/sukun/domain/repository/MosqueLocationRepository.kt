package dhanfinix.android.sukun.domain.repository

import dhanfinix.android.sukun.core.database.entity.MosqueLocation
import kotlinx.coroutines.flow.Flow

interface MosqueLocationRepository {
    fun getAllLocations(): Flow<List<MosqueLocation>>
    fun getLocationsByType(type: String): Flow<List<MosqueLocation>>
    suspend fun addLocation(location: MosqueLocation): Long
    suspend fun removeLocation(id: Long)
    suspend fun fetchGlobalMosques(latitude: Double, longitude: Double, radiusKm: Double): List<MosqueLocation>
    suspend fun clearGlobalMosques()
}
