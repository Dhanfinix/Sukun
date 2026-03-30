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
     * Fetches nearby mosques and maintains a sliding window of the N closest ones as geofences.
     *
     * Strategy (solves the dense-area problem):
     * 1. Fetch all mosques within [radiusMeters] of current position.
     * 2. Sort by distance and take only the nearest [MAX_ACTIVE_MOSQUE_GEOFENCES].
     * 3. Persist all of them to DB (for UI list display).
     * 4. Remove geofences for previously-registered mosques no longer in the top-N.
     * 5. Add geofences only for new top-N mosques.
     * 6. Remove DB entries for mosques >10km away (stale cleanup).
     *
     * This guarantees geofence count is always ≤ MAX_ACTIVE_MOSQUE_GEOFENCES,
     * regardless of how many mosques exist in the area (e.g., central Jakarta).
     */
    suspend fun fetchAndSaveMosques(latitude: Double, longitude: Double, force: Boolean = false) {
        if (!force) {
            val lastLat = userPrefs.lastFetchLat.first()
            val lastLng = userPrefs.lastFetchLng.first()
            // Only skip if we have a previous fetch location AND we haven't moved much
            if (lastLat != null && lastLng != null) {
                val results = FloatArray(1)
                Location.distanceBetween(latitude, longitude, lastLat, lastLng, results)
                if (results[0] < 200f) {
                    Log.d("MosqueRepository", "Skipping fetch: moved less than 200m since last fetch")
                    return
                }
            }
            // If lastLat/lastLng are null → first fetch ever → always proceed
        }

        val freshMosques = getNearbyMosques(latitude, longitude)

        // Sort by distance, keep only the nearest N for geofencing
        val nearestMosques = freshMosques
            .map { mosque ->
                val dist = FloatArray(1)
                Location.distanceBetween(latitude, longitude, mosque.latitude, mosque.longitude, dist)
                mosque to dist[0]
            }
            .sortedBy { it.second }
            .take(MAX_ACTIVE_MOSQUE_GEOFENCES)
            .map { it.first }

        val nearestExternalIds = nearestMosques.mapNotNull { it.externalId }.toSet()

        // Step 1: Persist all fresh mosques to DB (for the UI list)
        freshMosques.forEach { mosque ->
            val existing = silentZoneDao.getSilentZoneByExternalId(mosque.externalId!!)
            if (existing == null) {
                silentZoneDao.insertSilentZone(mosque)
            }
        }

        // Step 2: Get all currently-registered auto-mosque geofences
        val allAutoMosques = silentZoneDao.getAutoMosqueZones()

        // Step 3: Remove geofences for mosques that fell OUT of the nearest-N window
        allAutoMosques.forEach { existing ->
            if (existing.externalId != null && existing.externalId !in nearestExternalIds && existing.isEnabled) {
                geofenceManager.removeGeofence(existing.id)
                Log.d("MosqueRepository", "Removed geofence for out-of-window mosque: ${existing.name}")
            }
        }

        // Step 4: Stale cleanup — remove DB entries for mosques >10km away
        allAutoMosques.forEach { existing ->
            val dist = FloatArray(1)
            Location.distanceBetween(latitude, longitude, existing.latitude, existing.longitude, dist)
            if (dist[0] > 10_000f) {
                silentZoneDao.deleteSilentZone(existing)
                geofenceManager.removeGeofence(existing.id)
                Log.d("MosqueRepository", "Removed stale mosque: ${existing.name} (${dist[0].toInt()}m away)")
            }
        }

        // Step 5: Build the final set of mosques to geofence:
        // - Pinned mosques ALWAYS get a geofence (user intent overrides auto-selection)
        // - Remaining slots → nearest non-pinned mosques by distance
        val allAutoMosques2 = silentZoneDao.getAutoMosqueZones() // re-fetch after stale cleanup
        val pinnedMosques = allAutoMosques2.filter { it.isUserPinned && it.isEnabled }
        val remainingSlots = (MAX_ACTIVE_MOSQUE_GEOFENCES - pinnedMosques.size).coerceAtLeast(0)

        val nearestNonPinned = nearestMosques
            .mapNotNull { mosque ->
                val existing = silentZoneDao.getSilentZoneByExternalId(mosque.externalId ?: return@mapNotNull null)
                existing?.takeIf { !it.isUserPinned && it.isEnabled }
            }
            .take(remainingSlots)

        val shouldBeActive: Set<Long> = (pinnedMosques + nearestNonPinned).map { it.id }.toSet()

        // Remove geofences for mosques no longer in the active set
        allAutoMosques2.forEach { existing ->
            if (existing.id !in shouldBeActive && existing.hasActiveGeofence) {
                geofenceManager.removeGeofence(existing.id)
                silentZoneDao.updateGeofenceActive(existing.id, false)
                Log.d("MosqueRepository", "Deactivated geofence: ${existing.name}")
            }
        }

        // Register geofences for the active set
        allAutoMosques2.forEach { existing ->
            if (existing.id in shouldBeActive && existing.isEnabled) {
                geofenceManager.addGeofence(existing)
                silentZoneDao.updateGeofenceActive(existing.id, true)
                Log.d("MosqueRepository", "Activated geofence: ${existing.name} (pinned=${existing.isUserPinned})")
            }
        }

        userPrefs.setLastFetchLocation(latitude, longitude)
    }

    /**
     * Toggle user pin for a mosque. Re-syncs geofences immediately.
     * Returns false if pinning would exceed the cap.
     */
    suspend fun toggleUserPin(zone: SilentZone, maxSlots: Int = MAX_ACTIVE_MOSQUE_GEOFENCES): Boolean {
        val pinned = !zone.isUserPinned
        if (pinned) {
            // Check if there's room
            val currentPinned = silentZoneDao.getPinnedAutoMosques().size
            if (currentPinned >= maxSlots) return false
        }
        silentZoneDao.updateUserPinned(zone.id, pinned)
        if (!pinned && zone.hasActiveGeofence) {
            // If unpinning, the geofence may be replaced by nearest — handle in next fetch cycle
            // For now remove it immediately; sliding window will re-activate if still nearest
            geofenceManager.removeGeofence(zone.id)
            silentZoneDao.updateGeofenceActive(zone.id, false)
        } else if (pinned) {
            // Immediately activate geofence for newly pinned mosque
            val updated = zone.copy(isUserPinned = true)
            geofenceManager.addGeofence(updated)
            silentZoneDao.updateGeofenceActive(zone.id, true)
        }
        return true
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
            return (diag / 2f).coerceIn(20f, 100f)
        }
        return 20f
    }

    companion object {
        /**
         * Maximum number of mosque geofences kept ACTIVE (registered with the OS) at any time.
         * Only the nearest N mosques from the last fetch are active geofences.
         * All mosques are still stored in the DB for display purposes.
         * Android hard limit is 100 total (all geofences, all apps). We leave ample room.
         */
        private const val MAX_ACTIVE_MOSQUE_GEOFENCES = 10
    }
}
