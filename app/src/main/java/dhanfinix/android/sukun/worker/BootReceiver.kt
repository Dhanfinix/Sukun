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
 * Re-schedules all pending Sukun silence workers after device reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val userPrefs = UserPreferences(context)
        val prayerRepo = PrayerRepository(context)
        val scheduler = SilenceScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            val lat = userPrefs.latitude.first()
            val lng = userPrefs.longitude.first()
            val method = userPrefs.calculationMethod.first()
            val duration = userPrefs.silenceDurationMin.first()
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
                
                scheduler.scheduleAll(prayersToday, prayersTomorrow, duration)
            }
        }
    }
}
