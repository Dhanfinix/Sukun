package dhanfinix.android.sukun.feature.prayer.data.model

import androidx.compose.runtime.Stable

/**
 * Represents the five daily prayers with their display names.
 */
@Stable
enum class PrayerName(val displayName: String) {
    FAJR("Fajr"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    MAGHRIB("Maghrib"),
    ISHA("Isha")
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
