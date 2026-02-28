package dhanfinix.android.sukun.feature.volume

/**
 * Immutable UI state for the Volume Screen.
 * This is the single source of truth rendered by the composable.
 */
data class VolumeUiState(
    val mediaVolume: Float = 0f,
    val maxMedia: Int = 15,
    val ringVolume: Float = 0f,
    val maxRing: Int = 15,
    val notificationVolume: Float = 0f,
    val maxNotif: Int = 15,
    val alarmVolume: Float = 0f,
    val maxAlarm: Int = 15,
    val hasDndPermission: Boolean = true,
    val isNotifLinked: Boolean = true, // Default to true
    val isSystemLinked: Boolean = false, // True if system enforces it
    val isSukunActive: Boolean = false,
    val sukunEndTime: Long = 0L,
    val sukunLabel: String? = null,
    val hasExactAlarmPermission: Boolean = true,
    val isIgnoringBatteryOptimizations: Boolean = true,
    val silenceMode: dhanfinix.android.sukun.core.datastore.SilenceMode = dhanfinix.android.sukun.core.datastore.SilenceMode.DND
)

/**
 * Sealed hierarchy of user-initiated events on the Volume Screen.
 * Composables emit these; the ViewModel processes them.
 */
sealed class VolumeEvent {
    data class MediaChanged(val volume: Float) : VolumeEvent()
    data class RingChanged(val volume: Float) : VolumeEvent()
    data class NotificationChanged(val volume: Float) : VolumeEvent()
    data class AlarmChanged(val volume: Float) : VolumeEvent()
    data object NotifLinkToggled : VolumeEvent()
    data object Refresh : VolumeEvent()
    data class StartManualSilence(val durationMin: Int) : VolumeEvent()
    data object StopSilence : VolumeEvent()
    data object OnResume : VolumeEvent()
    data object RequestExactAlarm : VolumeEvent()
    data object RequestIgnoreBattery : VolumeEvent()
    data class SilenceModeChanged(val mode: dhanfinix.android.sukun.core.datastore.SilenceMode) : VolumeEvent()
}
