package dhanfinix.android.sukun.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.feature.prayer.data.PrayerRepository
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerInfo
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Triggered at midnight to refresh today's alarms.
 */
class MidnightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val userPrefs = UserPreferences(context)
        val prayerRepo = PrayerRepository(context)
        val scheduler = SilenceScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            val lat = userPrefs.latitude.first()
            val lng = userPrefs.longitude.first()
            val method = userPrefs.calculationMethod.first()
            val duration = userPrefs.silenceDurationMin.first()
            val enabledMap = userPrefs.isPrayerEnabled.first()

            // Fetching today's times (which are now available since it's past 00:00)
            val result = prayerRepo.getPrayerTimes(LocalDate.now(), lat, lng, method)
            result.onSuccess { timesMap ->
                val prayers = PrayerName.entries.map { name ->
                    PrayerInfo(
                        name = name,
                        time = timesMap[name] ?: "--:--",
                        isEnabled = enabledMap[name] ?: true
                    )
                }
                // Re-schedule for the whole day
                scheduler.scheduleAll(prayers, duration)
                
                // Set the next midnight alarm
                // (Optional: SilenceScheduler could handle this, but for simplicity let's keep it here or inside scheduleAll)
            }
        }
    }
}
