package dhanfinix.android.sukun.worker

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.datastore.SilenceMode
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.core.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receiver triggered by AlarmManager at exact prayer/manual silence times.
 * Uses goAsync() to perform fast SharedPreferences/AudioManager operations safely
 * without being delayed by WorkManager or Doze mode.
 */
class SilenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (action == ACTION_START_SILENCE) {
                    val prayerResId = intent.getIntExtra(KEY_PRAYER_NAME, R.string.app_name)
                    val prayerName = context.getString(prayerResId)
                    val durationMin = intent.getIntExtra(KEY_DURATION_MIN, 15)
                    handleStartSilence(context, prayerName, durationMin)
                } 
                else if (action == ACTION_STOP_SILENCE) {
                    handleStopSilence(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleStartSilence(context: Context, prayerName: String, durationMin: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Prevent execution if permission was revoked after the alarm was scheduled
        if (!notifManager.isNotificationPolicyAccessGranted) {
            Log.w(TAG, "SilenceReceiver aborted: DND permission is missing for $prayerName")
            return
        }

        val userPrefs = UserPreferences(context)

        val silenceMode = userPrefs.silenceMode.first()

        // 1. Save current volumes & system modes
        val currentMedia = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val currentRing = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val currentNotif = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        val currentAlarm = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val currentRingerMode = audioManager.ringerMode
        val currentFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) notifManager.currentInterruptionFilter else -1

        // BUG 6 FIX: Check if Sukun is already active. If it is, DO NOT overwrite the saved volumes.
        val isAlreadyActive = userPrefs.silenceEndTime.first() > System.currentTimeMillis()
        if (!isAlreadyActive) {
            userPrefs.saveAllVolumes(currentMedia, currentRing, currentNotif, currentAlarm, currentRingerMode, currentFilter)
        }

        // 2. Mute streams FIRST. 
        // Why? On many Android versions, setting stream volume to 0 automatically triggers 
        // the OS to switch the ringer mode to Silent. We must do this before setting our desired mode.
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
            
            if (silenceMode != SilenceMode.VIBRATE) {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission to mute streams", e)
        }

        // 3. Apply chosen Silence Mode LAST to ensure it overrides any OS-level side effects.
        try {
            when (silenceMode) {
                SilenceMode.SILENT -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }
                SilenceMode.VIBRATE -> {
                    // Turn off DND FIRST
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                    // Set ringer mode to VIBRATE LAST
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Not allowed to change Do Not Disturb state", e)
        }

        // 4. Update UI/metadata
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationMin * 60 * 1000L)
        userPrefs.setSilenceMetadata(startTime, endTime, prayerName)

        // 5. Show ongoing notification with working chronometer
        NotificationHelper.showSilenceNotification(context, prayerName, startTime, endTime)
    }

    private suspend fun handleStopSilence(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val userPrefs = UserPreferences(context)

        // 1. Restore exact system ringer mode / interruption filter
        val savedRingerMode = userPrefs.savedRingerMode.first()
        val savedFilter = userPrefs.savedInterruptionFilter.first()
        
        try {
            if (notifManager.isNotificationPolicyAccessGranted) {
                if (savedFilter >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    notifManager.setInterruptionFilter(savedFilter)
                } else {
                    notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }
            
            if (savedRingerMode >= 0) {
                audioManager.ringerMode = savedRingerMode
            } else {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission to restore ringer mode", e)
        }

        // 2. Restore saved volumes
        val (savedMedia, savedRing, savedNotif) = userPrefs.savedVolumes.first()
        val savedAlarm = userPrefs.savedAlarmVol.first()
        try {
            if (savedMedia >= 0) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedMedia, 0)
            if (savedRing >= 0) audioManager.setStreamVolume(AudioManager.STREAM_RING, savedRing, 0)
            if (savedNotif >= 0) audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, savedNotif, 0)
            if (savedAlarm >= 0) audioManager.setStreamVolume(AudioManager.STREAM_ALARM, savedAlarm, 0)
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission to restore volumes", e)
        }

        // 3. Clear silence metadata
        userPrefs.clearSilenceState()

        // 4. Cancel notification
        NotificationHelper.cancelNotification(context)
    }

    companion object {
        const val ACTION_START_SILENCE = "dhanfinix.android.sukun.START_SILENCE"
        const val ACTION_STOP_SILENCE = "dhanfinix.android.sukun.STOP_SILENCE"
        const val KEY_PRAYER_NAME = "prayer_name"
        const val KEY_DURATION_MIN = "duration_min"
        private const val TAG = "SilenceReceiver"
    }
}
