package dhanfinix.android.sukun.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dhanfinix.android.sukun.core.database.SukunDatabase
import dhanfinix.android.sukun.core.database.entity.SilentZoneSource
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.core.notification.NotificationHelper
import dhanfinix.android.sukun.R
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

        val pendingResult = goAsync() // Essential for background reliability
        val database = SukunDatabase.getDatabase(context)
        val silentZoneDao = database.silentZoneDao()
        val userPrefs = UserPreferences(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isLocationSilenceEnabled = userPrefs.isLocationSilenceEnabled.first()
                val isAutoMosqueSilenceEnabled = userPrefs.isAutoMosqueSilenceEnabled.first()

                for (geofence in triggeringGeofences) {
                    val zoneId = geofence.requestId.toLongOrNull() ?: continue
                    val zone = silentZoneDao.getSilentZoneById(zoneId) ?: continue

                    when (transitionType) {
                        Geofence.GEOFENCE_TRANSITION_ENTER, Geofence.GEOFENCE_TRANSITION_DWELL -> {
                            Log.d("GeofenceReceiver", "Entered/Dwell geofence: ${zone.name} (type: $transitionType)")

                            // IMPROVE-2: Skip if zone is disabled in DB (geofence may lag behind DB state)
                            if (!zone.isEnabled) {
                                Log.d("GeofenceReceiver", "Zone ${zone.name} is disabled in DB, skipping.")
                                continue
                            }

                            userPrefs.setActiveSilentZoneId(zone.id)

                            if (!isLocationSilenceEnabled) {
                                Log.d("GeofenceReceiver", "Master toggle OFF, skipping silence action but ID is saved.")
                                continue
                            }

                            if (zone.source == SilentZoneSource.AUTO_MOSQUE && !isAutoMosqueSilenceEnabled) {
                                Log.d("GeofenceReceiver", "Auto-Mosque toggle OFF, skipping silence action for detected mosque.")
                                continue
                            }

                            if (zone.isAutoSilent) {
                                val durationToUse = zone.silenceDuration ?: 30
                                // BUG-2 FIX: Use localized string instead of hardcoded English prefix
                                val zoneName = context.getString(R.string.label_location_prefix, zone.name)
                                val silenceIntent = Intent(context, SilenceReceiver::class.java).apply {
                                    action = SilenceReceiver.ACTION_START_SILENCE
                                    putExtra(SilenceReceiver.KEY_PRAYER_NAME_STRING, zoneName)
                                    putExtra(SilenceReceiver.KEY_DURATION_MIN, durationToUse)
                                    addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                                }
                                context.sendBroadcast(silenceIntent)

                                // IMPROVE-4: Safety alarm for "until exit" (silenceDuration == null)
                                // If the EXIT event is missed (Doze mode), silence would be stuck forever.
                                // Schedule a 2-hour safety stop that gets cancelled if EXIT fires first.
                                if (zone.silenceDuration == null) {
                                    scheduleSafetyStopAlarm(context, zone.id)
                                }

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
                                // Cancel the safety alarm since we got the real EXIT
                                cancelSafetyStopAlarm(context, zone.id)
                                val stopIntent = Intent(context, SilenceReceiver::class.java).apply {
                                    action = SilenceReceiver.ACTION_STOP_SILENCE
                                    addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                                }
                                context.sendBroadcast(stopIntent)
                                // BUG-1 FIX: Only clear activeSilentZoneId when the exiting zone IS the active zone
                                userPrefs.setActiveSilentZoneId(null)
                            } else {
                                Log.d("GeofenceReceiver", "Exit ignored: active zone id $currentActiveId != ${zone.id}")
                            }

                            NotificationHelper.showGeofenceStatusNotification(context, zone.name, false)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val SAFETY_ALARM_REQUEST_BASE = 90000 // Unique base for safety alarm request codes
        private const val SAFETY_ALARM_DURATION_MS = 2 * 60 * 60 * 1000L // 2 hours

        fun scheduleSafetyStopAlarm(context: Context, zoneId: Long) {
            val intent = Intent(context, SilenceReceiver::class.java).apply {
                action = SilenceReceiver.ACTION_STOP_SILENCE
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                (SAFETY_ALARM_REQUEST_BASE + zoneId).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAt = System.currentTimeMillis() + SAFETY_ALARM_DURATION_MS
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } catch (e: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
            Log.d("GeofenceReceiver", "Safety stop alarm scheduled for zone $zoneId at $triggerAt")
        }

        fun cancelSafetyStopAlarm(context: Context, zoneId: Long) {
            val intent = Intent(context, SilenceReceiver::class.java).apply {
                action = SilenceReceiver.ACTION_STOP_SILENCE
            }
            val pi = PendingIntent.getBroadcast(
                context,
                (SAFETY_ALARM_REQUEST_BASE + zoneId).toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(pi)
                Log.d("GeofenceReceiver", "Safety stop alarm cancelled for zone $zoneId")
            }
        }
    }
}
