package dhanfinix.android.sukun.worker

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dhanfinix.android.sukun.core.notification.NotificationHelper
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.core.datastore.SilenceMode
import kotlinx.coroutines.flow.first

/**
 * Worker triggered at the exact prayer (adhan) time.
 * 1. Saves current volume levels to DataStore
 * 2. Enables DND or sets ringer to silent
 * 3. Shows the countdown notification
 */
class SilenceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prayerName = inputData.getString(KEY_PRAYER_NAME) ?: "Prayer"
        val durationMin = inputData.getInt(KEY_DURATION_MIN, 15)

        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val userPrefs = UserPreferences(applicationContext)

        val notifManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Save current volumes & system modes
        val currentMedia = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val currentRing = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val currentNotif = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        val currentAlarm = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        
        val currentRingerMode = audioManager.ringerMode
        val currentFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notifManager.currentInterruptionFilter
        } else {
            -1
        }
        
        userPrefs.saveAllVolumes(currentMedia, currentRing, currentNotif, currentAlarm, currentRingerMode, currentFilter)

        // Save silence metadata
        val endTime = System.currentTimeMillis() + (durationMin * 60 * 1000L)
        userPrefs.setSilenceMetadata(endTime, prayerName)

        val silenceMode = userPrefs.silenceMode.first()

        // 2. Enable selected Silence Mode
        when (silenceMode) {
            SilenceMode.SILENT -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }
            SilenceMode.VIBRATE -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
        }

        // 3. Set volumes to 0 for maximum silence
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
            
            if (silenceMode != SilenceMode.VIBRATE) {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            }
        } catch (e: SecurityException) {
            // Log or ignore
        }

        // 4. Show notification
        NotificationHelper.showSilenceNotification(applicationContext, prayerName, endTime)

        return Result.success()
    }

    companion object {
        const val KEY_PRAYER_NAME = "prayer_name"
        const val KEY_DURATION_MIN = "duration_min"
    }
}
