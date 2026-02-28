package dhanfinix.android.sukun.worker

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dhanfinix.android.sukun.core.notification.NotificationHelper
import dhanfinix.android.sukun.core.datastore.UserPreferences
import kotlinx.coroutines.flow.first

/**
 * Worker triggered at adhan + duration.
 * 1. Restores previously saved volume levels
 * 2. Disables DND / restores ringer mode
 * 3. Cancels the countdown notification
 */
class RestoreWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val userPrefs = UserPreferences(applicationContext)

        // 1. Restore exact system ringer mode / interruption filter
        val savedRingerMode = userPrefs.savedRingerMode.first()
        val savedFilter = userPrefs.savedInterruptionFilter.first()
        
        val notifManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
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
            // Log or ignore if permission was revoked since silence started
        }

        // 2. ONLY THEN restore saved volumes
        val (savedMedia, savedRing, savedNotif) = userPrefs.savedVolumes.first()
        val savedAlarm = userPrefs.savedAlarmVol.first()
        try {
            if (savedMedia >= 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedMedia, 0)
            }
            if (savedRing >= 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, savedRing, 0)
            }
            if (savedNotif >= 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, savedNotif, 0)
            }
            if (savedAlarm >= 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, savedAlarm, 0)
            }
        } catch (e: SecurityException) {
            // Log or ignore if permission was revoked since silence started
        }

        // 3. Clear silence metadata
        userPrefs.clearSilenceState()

        // 4. Cancel notification
        NotificationHelper.cancelNotification(applicationContext)

        return Result.success()
    }
}
