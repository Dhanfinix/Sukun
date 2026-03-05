package dhanfinix.android.sukun.feature.prayer.ui

import androidx.compose.runtime.Stable
import dhanfinix.android.sukun.feature.prayer.data.model.LocationSuggestion
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerInfo
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName

/**
 * UI state for the Prayer Screen (UDF).
 */
@Stable
data class PrayerUiState(
    val prayers: List<PrayerInfo> = emptyList(),
    val allPrayers: List<PrayerInfo> = emptyList(),
    val prayerDurations: Map<PrayerName, Int> = emptyMap(),
    val prayerOffsets: Map<PrayerName, Int> = emptyMap(),
    val isDurationUniform: Boolean = false,
    val currentDate: String = "",
    val latitude: String = "0.0",
    val longitude: String = "0.0",
    val locationName: String? = null,
    val method: Int = 20,
    val isDetectingLocation: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val currentTime: String = "--:--:--",
    val nextPrayer: PrayerName? = null,
    val nextPrayerCountdown: String = "--:--:--",
    val locationSuggestions: List<LocationSuggestion> = emptyList(),
    val isSearchingSuggestions: Boolean = false
)

/**
 * Sealed event hierarchy for user actions on the Prayer Screen (UDF).
 */
sealed class PrayerEvent {
    data class TogglePrayer(val prayer: PrayerName) : PrayerEvent()
    data class DurationSelected(val prayer: PrayerName, val minutes: Int) : PrayerEvent()
    data class OffsetSelected(val prayer: PrayerName, val offsetMinutes: Int) : PrayerEvent()
    data class UniformDurationToggled(val isUniform: Boolean) : PrayerEvent()
    data class AllDurationsSelected(val minutes: Int) : PrayerEvent()
    data class LocationUpdated(val latitude: String, val longitude: String) : PrayerEvent()
    data class MethodChanged(val methodId: Int) : PrayerEvent()
    data object RefreshTimes : PrayerEvent()
    data object DetectLocation : PrayerEvent()
    data class SearchLocation(val query: String) : PrayerEvent()
    data class SearchQueryChanged(val query: String) : PrayerEvent()
    data class SuggestionSelected(val suggestion: LocationSuggestion) : PrayerEvent()
    data object ClearSuggestions : PrayerEvent()
    data object ErrorMessageConsumed : PrayerEvent()
    data object SnackbarMessageConsumed : PrayerEvent()
}
