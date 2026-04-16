package dhanfinix.android.sukun.feature.prayer.data.model

import androidx.compose.runtime.Stable
import dhanfinix.android.sukun.R

/**
 * Represents the five daily prayers with their display names.
 */
@Stable
enum class PrayerName(val nameRes: Int) {
    FAJR(R.string.prayer_fajr),
    DHUHR(R.string.prayer_dhuhr),
    JUMUAH(R.string.prayer_jumuah),
    ASR(R.string.prayer_asr),
    MAGHRIB(R.string.prayer_maghrib),
    ISHA(R.string.prayer_isha);

    companion object {
        /**
         * Returns the prayers appropriate for a given day.
         * On Fridays, DHUHR is replaced by JUMUAH (DHUHR filtered out).
         * On other days, JUMUAH is filtered out.
         */
        fun filterByDay(isFriday: Boolean): List<PrayerName> = entries.filter {
            if (isFriday) it != DHUHR else it != JUMUAH
        }
    }
}

/**
 * Top-level alias so tests (and any external caller) can call filterByDay(dayOfWeek)
 * without going through the companion object.
 */
fun filterByDay(isFriday: Boolean): List<PrayerName> = PrayerName.filterByDay(isFriday)

/**
 * Holds info about a single prayer for display.
 */
@Stable
data class PrayerInfo(
    val name: PrayerName,
    val time: String, // "HH:mm" format
    val isEnabled: Boolean,
    val formattedTime: dhanfinix.android.sukun.core.utils.FormattedTime? = null
)
@Stable
data class LocationSuggestion(
    val name: String,
    val latitude: Double,
    val longitude: Double
)
