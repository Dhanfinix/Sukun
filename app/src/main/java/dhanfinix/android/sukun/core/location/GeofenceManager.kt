package dhanfinix.android.sukun.core.location

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dhanfinix.android.sukun.core.database.entity.MosqueLocation
import dhanfinix.android.sukun.worker.GeofenceReceiver
import kotlinx.coroutines.tasks.await

/**
 * Manager responsible for registering and removing geofences for mosque locations.
 *
 * Usage notes:
 * - Ensure the app has the appropriate location permissions before calling [addGeofences].
 * - The caller should handle background location permission on Android 10+ if persistent geofences are required.
 * - This class performs asynchronous operations and exposes suspend functions to integrate with coroutines.
 */
class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    /**
     * Add geofences for the provided locations using the given radius (in meters).
     * Existing geofences will not be removed by this call. Use [removeGeofences] to clear.
     */
    suspend fun addGeofences(locations: List<MosqueLocation>, radiusMeters: Float) {
        if (locations.isEmpty()) return

        // Build Geofence objects
        val geofences = locations.map { loc ->
            Geofence.Builder()
                .setRequestId("mosque_${loc.id}") // unique id per entry
                .setCircularRegion(loc.latitude, loc.longitude, radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        val pendingIntent = getGeofencePendingIntent()

        try {
            geofencingClient.addGeofences(request, pendingIntent).await()
            Log.d(TAG, "Added ${geofences.size} geofences")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to add geofences: ${t.message}", t)
            throw t
        }
    }

    /**
     * Remove all geofences previously registered with the same PendingIntent.
     */
    suspend fun removeGeofences() {
        val pendingIntent = getGeofencePendingIntent()
        try {
            geofencingClient.removeGeofences(pendingIntent).await()
            Log.d(TAG, "Removed geofences")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to remove geofences: ${t.message}", t)
            throw t
        }
    }

    /**
     * Creates (or reuses) the PendingIntent that will be delivered to [GeofenceReceiver]
     * when geofence transitions occur.
     */
    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context.applicationContext, GeofenceReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context.applicationContext, GEOFENCE_PENDING_INTENT_REQUEST_CODE, intent, flags)
    }

    companion object {
        private const val TAG = "GeofenceManager"
        private const val GEOFENCE_PENDING_INTENT_REQUEST_CODE = 0
    }
}
