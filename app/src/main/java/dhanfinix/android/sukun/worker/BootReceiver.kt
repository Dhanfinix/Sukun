package dhanfinix.android.sukun.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.core.notification.NotificationHelper
import dhanfinix.android.sukun.feature.prayer.data.PrayerRepository
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerInfo
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Re-schedules all pending Sukun silence workers after device reboot.
 * Also recovers any active silences that were interrupted by the reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val userPrefs = UserPreferences(context)
        val prayerRepo = PrayerRepository(context)
        val scheduler = SilenceScheduler(context)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Bug 7 Fix: Mid-Silence Reboot Amnesia
                val endTime = userPrefs.silenceEndTime.first()
                if (endTime > System.currentTimeMillis()) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val restoreIntent = Intent(context, SilenceReceiver::class.java).apply {
                        action = SilenceReceiver.ACTION_STOP_SILENCE
                    }
                    val pendingRestore = PendingIntent.getBroadcast(
                        context,
                        SilenceScheduler.REQUEST_CODE_MANUAL_RESTORE, 
                        restoreIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    try {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, pendingRestore)
                    } catch (e: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, pendingRestore)
                    }
                    
                    val label = userPrefs.silenceLabel.first() ?: "Prayer"
                    val startTime = userPrefs.silenceStartTime.first().takeIf { it > 0L }
                        ?: (endTime - 30 * 60 * 1000L) // Fallback: assume 30 min silence if no start stored
                    NotificationHelper.showSilenceNotification(context, label, startTime, endTime)
                }

                val lat = userPrefs.latitude.first()
                val lng = userPrefs.longitude.first()
                val method = userPrefs.calculationMethod.first()
                val durations = userPrefs.prayerDurations.first()
                val enabledMap = userPrefs.isPrayerEnabled.first()

                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)
                
                val resultToday = prayerRepo.getPrayerTimes(today, lat, lng, method)
                val resultTomorrow = prayerRepo.getPrayerTimes(tomorrow, lat, lng, method)

                if (resultToday.isSuccess && resultTomorrow.isSuccess) {
                    val timesMapToday = resultToday.getOrThrow()
                    val timesMapTomorrow = resultTomorrow.getOrThrow()
                    
                    val prayersToday = PrayerName.entries.map { name ->
                        PrayerInfo(
                            name = name,
                            time = timesMapToday[name] ?: "--:--",
                            isEnabled = enabledMap[name] ?: true
                        )
                    }
                    
                    val prayersTomorrow = PrayerName.entries.map { name ->
                        PrayerInfo(
                            name = name,
                            time = timesMapTomorrow[name] ?: "--:--",
                            isEnabled = enabledMap[name] ?: true
                        )
                    }
                    
                    scheduler.scheduleAll(prayersToday, prayersTomorrow, durations)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
