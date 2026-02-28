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

            val result = prayerRepo.getPrayerTimes(LocalDate.now(), lat, lng, method)
            result.onSuccess { timesMap ->
                val prayers = PrayerName.entries.map { name ->
                    PrayerInfo(
                        name = name,
                        time = timesMap[name] ?: "--:--",
                        isEnabled = enabledMap[name] ?: true
                    )
                }
                scheduler.scheduleAll(prayers, duration)
            }
        }
    }
}
