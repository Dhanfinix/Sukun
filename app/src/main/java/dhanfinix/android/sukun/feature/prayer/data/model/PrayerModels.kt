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
    ISHA(R.string.prayer_isha)
}

/**
 * Holds info about a single prayer for display.
 */
@Stable
data class PrayerInfo(
    val name: PrayerName,
    val time: String, // "HH:mm" format
    val isEnabled: Boolean
)
@Stable
data class LocationSuggestion(
    val name: String,
    val latitude: Double,
    val longitude: Double
)
