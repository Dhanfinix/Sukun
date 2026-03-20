package dhanfinix.android.sukun.core.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import dhanfinix.android.sukun.core.database.entity.SilentZone
import dhanfinix.android.sukun.worker.GeofenceReceiver

class GeofenceManager(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private val locationPendingIntent: PendingIntent by lazy {
        val intent = Intent(context, dhanfinix.android.sukun.worker.LocationReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            1, // Unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun requestBackgroundLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 
            30 * 60 * 1000L // 30 minutes
        ).apply {
            setMinUpdateDistanceMeters(500f) // Match the 500m mosque fetch radius for full coverage overlap
        }.build()

        try {
            fusedLocationClient.removeLocationUpdates(locationPendingIntent)
            fusedLocationClient.requestLocationUpdates(locationRequest, locationPendingIntent)
            Log.d("GeofenceManager", "Background location updates requested")
        } catch (e: Exception) {
            Log.e("GeofenceManager", "Failed to request background location updates", e)
        }
    }

    fun removeBackgroundLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationPendingIntent)
            Log.d("GeofenceManager", "Background location updates removed")
        } catch (e: Exception) {
            Log.e("GeofenceManager", "Failed to remove background location updates", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(silentZone: SilentZone, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val geofence = Geofence.Builder()
            .setRequestId(silentZone.id.toString())
            .setCircularRegion(
                silentZone.latitude,
                silentZone.longitude,
                silentZone.radius
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(10000) // 10 seconds dwell time for faster trigger
            .setNotificationResponsiveness(0) // 0 for best-effort immediate responsiveness
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER) // Detection if user is already inside
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceManager", "Geofence added: ${silentZone.name}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceManager", "Failed to add geofence: ${silentZone.name}", e)
                onFailure(e)
            }
    }

    @SuppressLint("MissingPermission")
    fun addAllGeofences(zones: List<SilentZone>, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        if (zones.isEmpty()) return

        val geofences = zones.map { silentZone ->
            Geofence.Builder()
                .setRequestId(silentZone.id.toString())
                .setCircularRegion(
                    silentZone.latitude,
                    silentZone.longitude,
                    silentZone.radius
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(10000)
                .setNotificationResponsiveness(0)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceManager", "Batch geofences added: ${zones.size} zones")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceManager", "Failed to add batch geofences", e)
                onFailure(e)
            }
    }

    fun removeGeofence(id: Long, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        geofencingClient.removeGeofences(listOf(id.toString()))
            .addOnSuccessListener {
                Log.d("GeofenceManager", "Geofence removed: $id")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceManager", "Failed to remove geofence: $id", e)
                onFailure(e)
            }
    }

    fun removeAllGeofences(onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceManager", "All geofences removed")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceManager", "Failed to remove all geofences", e)
                onFailure(e)
            }
    }
}
