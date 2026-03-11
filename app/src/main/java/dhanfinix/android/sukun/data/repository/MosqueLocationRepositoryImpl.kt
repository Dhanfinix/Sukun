package dhanfinix.android.sukun.data.repository

import dhanfinix.android.sukun.core.database.dao.MosqueLocationDao
import dhanfinix.android.sukun.core.database.entity.MosqueLocation
import dhanfinix.android.sukun.core.network.FoursquareApiClient
import dhanfinix.android.sukun.core.network.model.FoursquareResponse
import dhanfinix.android.sukun.domain.repository.MosqueLocationRepository
import kotlinx.coroutines.flow.Flow

class MosqueLocationRepositoryImpl(
    private val dao: MosqueLocationDao
) : MosqueLocationRepository {

    override fun getAllLocations(): Flow<List<MosqueLocation>> = dao.getAllLocations()

    override fun getLocationsByType(type: String): Flow<List<MosqueLocation>> = dao.getLocationsByType(type)

    override suspend fun addLocation(location: MosqueLocation): Long = dao.insert(location)

    override suspend fun removeLocation(id: Long) = dao.deleteById(id)

    override suspend fun fetchGlobalMosques(latitude: Double, longitude: Double, radiusKm: Double): List<MosqueLocation> {
        val radiusMeters = (radiusKm * 1000).toInt()
        val llString = "$latitude,$longitude"

        val mosques = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                dhanfinix.android.sukun.core.network.FoursquareApiClient.api
                    .searchPlaces(latLng = llString, radius = radiusMeters, limit = 50)
                    .results.mapNotNull { place ->
                        val lat = place.latitude
                        val lon = place.longitude
                        if (lat == null || lon == null) return@mapNotNull null
                        
                        MosqueLocation(
                            name = place.name,
                            latitude = lat,
                            longitude = lon,
                            type = "GLOBAL",
                            address = place.location?.address ?: place.location?.formattedAddress ?: place.location?.crossStreet
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e("MosqueLocationRepo", "Error fetching global mosques from Foursquare", e)
                emptyList()
            }
        }

        // Persist fetched global mosques locally
        dao.deleteAllByType("GLOBAL")
        dao.insertAll(mosques)
        return mosques
    }

    override suspend fun clearGlobalMosques() {
        dao.deleteAllByType("GLOBAL")
    }
}
