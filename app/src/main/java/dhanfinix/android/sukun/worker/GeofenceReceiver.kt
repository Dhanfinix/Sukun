package dhanfinix.android.sukun.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dhanfinix.android.sukun.core.database.SukunDatabase
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.core.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            val errorCode = geofencingEvent?.errorCode ?: -1
            Log.e("GeofenceReceiver", "GeofencingEvent error: $errorCode")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()

        if (triggeringGeofences.isEmpty()) return

        val database = SukunDatabase.getDatabase(context)
        val silentZoneDao = database.silentZoneDao()
        val userPrefs = UserPreferences(context)

        CoroutineScope(Dispatchers.IO).launch {
            for (geofence in triggeringGeofences) {
                val zoneId = geofence.requestId.toLongOrNull() ?: continue
                val zone = silentZoneDao.getSilentZoneById(zoneId) ?: continue

                when (transitionType) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        Log.d("GeofenceReceiver", "Entered geofence: ${zone.name}")
                        userPrefs.setActiveSilentZoneId(zone.id)
                        
                        if (zone.isAutoSilent) {
                            val durationToUse = zone.silenceDuration ?: 30
                            val silenceIntent = Intent(context, SilenceReceiver::class.java).apply {
                                action = SilenceReceiver.ACTION_START_SILENCE
                                putExtra(SilenceReceiver.KEY_PRAYER_NAME_STRING, "Location: ${zone.name}")
                                putExtra(SilenceReceiver.KEY_DURATION_MIN, durationToUse)
                                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                            }
                            context.sendBroadcast(silenceIntent)
                            NotificationHelper.showGeofenceStatusNotification(context, zone.name, true, true, zone.silenceDuration)
                        } else {
                            NotificationHelper.showGeofenceStatusNotification(context, zone.name, true, false, zone.silenceDuration)
                        }
                    }
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        Log.d("GeofenceReceiver", "Exited geofence: ${zone.name}")
                        
                        val currentActiveId = userPrefs.activeSilentZoneId.first()
                        if (currentActiveId == zone.id) {
                            Log.d("GeofenceReceiver", "Exiting active zone ${zone.name}, stopping silence")
                            val stopIntent = Intent(context, SilenceReceiver::class.java).apply {
                                action = SilenceReceiver.ACTION_STOP_SILENCE
                                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                            }
                            context.sendBroadcast(stopIntent)
                        } else {
                            Log.d("GeofenceReceiver", "Exit ignored: active zone id $currentActiveId != ${zone.id}")
                        }
                        
                        userPrefs.setActiveSilentZoneId(null)
                        NotificationHelper.showGeofenceStatusNotification(context, zone.name, false)
                    }
                }
            }
        }
    }
}
