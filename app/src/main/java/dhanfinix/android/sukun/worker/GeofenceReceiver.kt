package dhanfinix.android.sukun.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.core.notification.NotificationHelper
import dhanfinix.android.sukun.core.utils.VolumeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // GeofencingEvent.fromIntent can return null in some cases; guard against that.
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofenceReceiver: received null GeofencingEvent")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        // Get the geofences that were triggered (safe-call; may be null)
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()
        triggeringGeofences.forEach { geofence ->
            Log.d(TAG, "Geofence triggered: ${geofence.requestId}")
        }

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.d(TAG, "Entered geofence, initiating silence")
                // Start silence for mosque location
                CoroutineScope(Dispatchers.IO).launch {
                    val userPrefs = UserPreferences(context)
                    val isAutoSilent = userPrefs.isMosqueAutoSilent.first()
                    val durationMin = userPrefs.mosqueSilentDuration.first()

                    if (isAutoSilent) {
                        // Automatically trigger silence
                        dhanfinix.android.sukun.worker.SilenceScheduler(context).scheduleManual(durationMin)
                    } else {
                        // Ask user via notification
                        dhanfinix.android.sukun.core.notification.NotificationHelper.showMosqueEntryNotification(context, durationMin)
                    }
                }
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d(TAG, "Exited geofence, restoring volume")
                // Only cancel the notification if it was showing.
                // If it was auto-silenced, the SilenceScheduler's end timer handles restoration.
                dhanfinix.android.sukun.core.notification.NotificationHelper.cancelMosqueEntryNotification(context)
            }
            else -> {
                Log.d(TAG, "Unknown geofence transition: $geofenceTransition")
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
