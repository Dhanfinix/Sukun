package dhanfinix.android.sukun.feature.volume

import kotlin.math.roundToInt
import android.app.Application
import android.media.AudioManager
import android.widget.Toast
import android.app.NotificationManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dhanfinix.android.sukun.core.datastore.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import dhanfinix.android.sukun.worker.SilenceScheduler
import dhanfinix.android.sukun.core.reliability.ReliabilityManager
import kotlinx.coroutines.flow.first

/**
 * ViewModel for the Volume Dashboard.
 * Processes [VolumeEvent]s and emits [VolumeUiState] via StateFlow (UDF).
 */
class VolumeViewModel(application: Application) : AndroidViewModel(application) {

    private val audioManager =
        application.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager

    private val userPrefs = UserPreferences(application)
    private val silenceScheduler = SilenceScheduler(application)
    private val reliabilityManager = ReliabilityManager(application)

    private val _uiState = MutableStateFlow(VolumeUiState())
    val uiState: StateFlow<VolumeUiState> = _uiState.asStateFlow()

    init {
        loadCurrentVolumes()
        observeSettings()
        observeSilenceState()
    }

    /**
     * Single entry point for all user events — the UDF event handler.
     */
    fun onEvent(event: VolumeEvent) {
        when (event) {
            is VolumeEvent.MediaChanged -> setMediaVolume(event.volume)
            is VolumeEvent.RingChanged -> setRingVolume(event.volume)
            is VolumeEvent.NotificationChanged -> setNotificationVolume(event.volume)
            is VolumeEvent.AlarmChanged -> setAlarmVolume(event.volume)
            is VolumeEvent.NotifLinkToggled -> toggleNotifLink()
            is VolumeEvent.Refresh, is VolumeEvent.OnResume -> loadCurrentVolumes()
            is VolumeEvent.StartManualSilence -> startManualSilenceOrConfirm(event.durationMin)
            is VolumeEvent.StopSilence -> stopSilence()
            is VolumeEvent.RequestExactAlarm -> reliabilityManager.openExactAlarmSettings()
            is VolumeEvent.RequestIgnoreBattery -> reliabilityManager.requestIgnoreBatteryOptimizations()
            is VolumeEvent.SilenceModeChanged -> setSilenceMode(event.mode)
            is VolumeEvent.ConfirmOverwrite -> confirmOverwrite()
            is VolumeEvent.DismissOverwrite -> _uiState.update { it.copy(pendingOverwriteDurationMin = null) }
        }
    }

    private fun loadCurrentVolumes() {
        val mediaMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val ringMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val notifMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        val alarmMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

        val mediaCurrent = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val ringCurrent = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val notifCurrent = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        val alarmCurrent = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        val isSystemLinked = checkSystemVolumeLinking()

        _uiState.update {
            it.copy(
                mediaVolume = toPercent(mediaCurrent, mediaMax),
                maxMedia = mediaMax,
                ringVolume = toPercent(ringCurrent, ringMax),
                maxRing = ringMax,
                notificationVolume = toPercent(notifCurrent, notifMax),
                maxNotif = notifMax,
                alarmVolume = toPercent(alarmCurrent, alarmMax),
                maxAlarm = alarmMax,
                hasDndPermission = hasDndPermission(),
                isSystemLinked = isSystemLinked,
                // If system enforces it, we force our internal toggle to true as well
                isNotifLinked = if (isSystemLinked) true else it.isNotifLinked,
                hasExactAlarmPermission = reliabilityManager.isExactAlarmPermissionGranted(),
                isIgnoringBatteryOptimizations = reliabilityManager.isIgnoringBatteryOptimizations()
            )
        }
    }

    private fun checkSystemVolumeLinking(): Boolean {
        return try {
            // Android 14 (API 34) officially decoupled them.
            // For older versions or specific OEMs, we check the hidden "notifications_use_ring_volume" flag.
            val defaultValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) 0 else 1
            
            val result = Settings.System.getInt(
                getApplication<Application>().contentResolver,
                "notifications_use_ring_volume",
                defaultValue
            )
            result == 1
        } catch (e: Exception) {
            // Fallback: Android 14+ is likely unlinked, older is likely linked
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        }
    }

    private fun toPercent(current: Int, max: Int): Float =
        if (max > 0) (current.toFloat() / max * 100f) else 0f

    private fun fromPercent(percent: Float, max: Int): Int =
        ((percent / 100f) * max).roundToInt().coerceIn(0, max)

    private fun setMediaVolume(percent: Float) {
        try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentLevel = fromPercent(percent, max)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentLevel, 0)
            _uiState.update { it.copy(mediaVolume = toPercent(currentLevel, max)) }
        } catch (e: SecurityException) {
            // Log or ignore if permission missing
        }
    }

    private fun setRingVolume(percent: Float) {
        if (!hasDndPermission()) {
            showPermissionToast()
            return
        }
        
        try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val currentLevel = fromPercent(percent, max)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, currentLevel, 0)

            // Sync with Notification if linked
            if (_uiState.value.isNotifLinked) {
                val notifMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
                val notifLevel = fromPercent(percent, notifMax)
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, notifLevel, 0)
                _uiState.update { it.copy(notificationVolume = toPercent(notifLevel, notifMax)) }
            }

            _uiState.update { it.copy(ringVolume = toPercent(currentLevel, max)) }
        } catch (e: SecurityException) {
            // Log or ignore
        }
    }

    private fun setNotificationVolume(percent: Float) {
        if (!hasDndPermission()) {
            showPermissionToast()
            return
        }

        try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            val currentLevel = fromPercent(percent, max)
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, currentLevel, 0)
            
            // Sync with Ring if linked
            if (_uiState.value.isNotifLinked) {
                val ringMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                val ringLevel = fromPercent(percent, ringMax)
                audioManager.setStreamVolume(AudioManager.STREAM_RING, ringLevel, 0)
                _uiState.update { it.copy(ringVolume = toPercent(ringLevel, ringMax)) }
            }

            _uiState.update { it.copy(notificationVolume = toPercent(currentLevel, max)) }
        } catch (e: SecurityException) {
            // Log or ignore
        }
    }

    private fun setAlarmVolume(percent: Float) {
        try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val currentLevel = fromPercent(percent, max)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, currentLevel, 0)
            _uiState.update { it.copy(alarmVolume = toPercent(currentLevel, max)) }
        } catch (e: SecurityException) {
            // Log or ignore
        }
    }

    private fun hasDndPermission(): Boolean {
        val nm = getApplication<Application>().getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    private fun showPermissionToast() {
        Toast.makeText(
            getApplication(),
            "Please grant Do Not Disturb access to change this volume",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun toggleNotifLink() {
        viewModelScope.launch {
            val newValue = !_uiState.value.isNotifLinked
            userPrefs.setNotifLinked(newValue)
            _uiState.update { it.copy(isNotifLinked = newValue) }
            
            // If just linked, sync them immediately
            if (newValue) {
                setRingVolume(_uiState.value.ringVolume)
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            userPrefs.isNotifLinked.collect { linked ->
                _uiState.update { it.copy(isNotifLinked = linked) }
            }
        }
        viewModelScope.launch {
            userPrefs.silenceMode.collect { mode ->
                _uiState.update { it.copy(silenceMode = mode) }
            }
        }
    }

    private fun observeSilenceState() {
        viewModelScope.launch {
            userPrefs.silenceEndTime.collect { endTime ->
                updateSilenceState(endTime)
            }
        }
        viewModelScope.launch {
            userPrefs.silenceLabel.collect { label ->
                _uiState.update { it.copy(sukunLabel = label) }
            }
        }
    }

    // The UI handles its own second-precision ticker now based on sukunEndTime.

    private var wasSukunActive = false

    private fun updateSilenceState(endTime: Long) {
        val now = System.currentTimeMillis()
        val isActive = endTime > now
        
        _uiState.update { 
            it.copy(
                isSukunActive = isActive,
                sukunEndTime = if (isActive) endTime else 0L
            )
        }

        // If silence just ended (e.g. from notification while app is foreground),
        // refresh the volume sliders to reflect the restored system volumes.
        if (wasSukunActive && !isActive) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(500) // Give RestoreWorker a moment to apply volumes
                loadCurrentVolumes()
            }
        }
        wasSukunActive = isActive
    }

    private fun startManualSilenceOrConfirm(durationMin: Int) {
        if (_uiState.value.isSukunActive) {
            // A silence is already running — ask the user to confirm before overwriting
            _uiState.update { it.copy(pendingOverwriteDurationMin = durationMin) }
        } else {
            startManualSilence(durationMin)
        }
    }

    private fun confirmOverwrite() {
        val pending = _uiState.value.pendingOverwriteDurationMin ?: return
        _uiState.update { it.copy(pendingOverwriteDurationMin = null) }
        stopSilence()
        startManualSilence(pending)
    }

    private fun startManualSilence(durationMin: Int) {
        silenceScheduler.scheduleManual(durationMin)
        // Opt: immediate UI update before Worker starts
        val endTime = System.currentTimeMillis() + (durationMin * 60 * 1000L)
        updateSilenceState(endTime)
        _uiState.update { it.copy(sukunLabel = "Manual") }
        
        // Refresh sliders after a tiny delay to allow SilenceWorker to fire and set volumes to 0
        viewModelScope.launch {
            delay(500)
            loadCurrentVolumes()
        }
    }

    private fun stopSilence() {
        silenceScheduler.stopSilence()
        // DO NOT clear prefs here. Let the RestoreWorker (called by scheduler) clear them.
        // We just update UI immediately for responsiveness.
        _uiState.update { 
            it.copy(
                isSukunActive = false,
                sukunEndTime = 0L,
                sukunLabel = null
            )
        }
        
        viewModelScope.launch {
            delay(500) 
            loadCurrentVolumes()
        }
    }

    private fun setSilenceMode(mode: dhanfinix.android.sukun.core.datastore.SilenceMode) {
        viewModelScope.launch {
            userPrefs.setSilenceMode(mode)
        }
    }
}
