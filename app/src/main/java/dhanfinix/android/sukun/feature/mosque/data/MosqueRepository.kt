package dhanfinix.android.sukun.feature.mosque.data

import android.content.Context
import android.location.Location
import android.util.Log
import dhanfinix.android.sukun.core.database.SukunDatabase
import dhanfinix.android.sukun.core.database.entity.SilentZone
import dhanfinix.android.sukun.core.database.entity.SilentZoneSource
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.core.location.GeofenceManager
import dhanfinix.android.sukun.core.network.ApiClient
import dhanfinix.android.sukun.feature.mosque.data.model.OverpassElement
import kotlinx.coroutines.flow.first

class MosqueRepository(private val context: Context) {
    private val overpassApi = ApiClient.overpassApi
    private val database = SukunDatabase.getDatabase(context)
    private val silentZoneDao = database.silentZoneDao()
    private val userPrefs = UserPreferences(context)
    private val geofenceManager = GeofenceManager(context)

    /**
     * Fetches nearby mosques and saves them to the database if they don't already exist.
     * Uses a 1km threshold to avoid redundant network calls.
     */
    suspend fun fetchAndSaveMosques(latitude: Double, longitude: Double, force: Boolean = false) {
        if (!force) {
            val lastLat = userPrefs.lastFetchLat.first()
            val lastLng = userPrefs.lastFetchLng.first()
            if (lastLat != null && lastLng != null) {
                val results = FloatArray(1)
                Location.distanceBetween(latitude, longitude, lastLat, lastLng, results)
                if (results[0] < 1000f) {
                    Log.d("MosqueRepository", "Skipping fetch: moved less than 1km since last fetch")
                    return
                }
            }
        }

        val mosques = getNearbyMosques(latitude, longitude)
        mosques.forEach { mosque ->
            val existing = silentZoneDao.getSilentZoneByExternalId(mosque.externalId!!)
            if (existing == null) {
                val id = silentZoneDao.insertSilentZone(mosque)
                val savedZone = mosque.copy(id = id)
                // Register geofence for the newly discovered mosque
                geofenceManager.addGeofence(savedZone)
            } else if (!existing.isEnabled) {
               // Optional: maybe update existing details? 
                // For now just keep existing.
            }
        }
        userPrefs.setLastFetchLocation(latitude, longitude)
    }

    suspend fun getNearbyMosques(latitude: Double, longitude: Double, radiusMeters: Int = 500): List<SilentZone> {
        val query = """
            [out:json];
            (
              node["amenity"="mosque"](around:$radiusMeters,$latitude,$longitude);
              way["amenity"="mosque"](around:$radiusMeters,$latitude,$longitude);
              relation["amenity"="mosque"](around:$radiusMeters,$latitude,$longitude);
              node["religion"="muslim"](around:$radiusMeters,$latitude,$longitude);
              way["religion"="muslim"](around:$radiusMeters,$latitude,$longitude);
              relation["religion"="muslim"](around:$radiusMeters,$latitude,$longitude);
            );
            out center;
        """.trimIndent()

        return try {
            val response = overpassApi.query(query)
            Log.d("MosqueRepository", "Found ${response.elements.size} mosques nearby")
            response.elements.map { element ->
                val lat = element.lat ?: element.center?.lat ?: 0.0
                val lon = element.lon ?: element.center?.lon ?: 0.0
                val name = element.tags?.get("name") ?: "Mosque"
                
                SilentZone(
                    name = name,
                    latitude = lat,
                    longitude = lon,
                    radius = calculateRadius(element),
                    source = SilentZoneSource.AUTO_MOSQUE,
                    externalId = "${element.type}/${element.id}",
                    isEnabled = true
                )
            }
        } catch (e: Exception) {
            Log.e("MosqueRepository", "Error fetching mosques: ${e.message}")
            emptyList()
        }
    }

    private fun calculateRadius(element: OverpassElement): Float {
        val bounds = element.bounds
        if (bounds != null) {
            val results = FloatArray(1)
            Location.distanceBetween(bounds.minlat, bounds.minlon, bounds.maxlat, bounds.maxlon, results)
            val diag = results[0]
            // Use half of diagonal as radius, clamped
            return (diag / 2f).coerceIn(20f, 100f)
        }
        
        return 20f 
    }
}
