package dhanfinix.android.sukun.core.utils

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log

import dhanfinix.android.sukun.core.datastore.SilenceMode
import dhanfinix.android.sukun.core.datastore.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Small utility that centralizes applying silence modes and restoring volume.
 *
 * NOTE:
 * - These functions are suspend so callers can call them from coroutines and safely
 *   interact with DataStore (UserPreferences) which exposes suspend APIs.
 * - The implementation is defensive: it checks DND permission before attempting to change
 *   interruption filters, and wraps platform calls in try/catch to avoid crashing the receiver.
 */
object VolumeController {
    private const val TAG = "VolumeController"

    /**
     * Apply the requested [mode] (SILENT or VIBRATE) to the device and save current volumes
     * into DataStore so they can be restored later by [restoreVolume].
     *
     * @param context application context
     * @param mode desired silence mode
     * @param label optional label to describe why silence was applied (persisted by caller separately)
     */
    suspend fun applySilenceMode(context: Context, mode: SilenceMode, label: String? = null) {
        withContext(Dispatchers.IO) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Snapshot current volumes and modes
                val currentMedia = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val currentRing = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                val currentNotif = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                val currentAlarm = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                val currentRingerMode = audioManager.ringerMode
                val currentFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) notifManager.currentInterruptionFilter else -1

                // Persist current state to DataStore
                val prefs = UserPreferences(context)
                prefs.saveAllVolumes(
                    currentMedia,
                    currentRing,
                    currentNotif,
                    currentAlarm,
                    currentRingerMode,
                    currentFilter
                )

                // Mute streams first to avoid OS side-effects (some OEMs change ringer mode)
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
                    if (mode != SilenceMode.VIBRATE) {
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                    }
                } catch (se: SecurityException) {
                    Log.w(TAG, "No permission to set stream volumes: ${se.message}")
                }

                // Apply ringer mode / DND as final step
                try {
                    when (mode) {
                        SilenceMode.SILENT -> {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                        }
                        SilenceMode.VIBRATE -> {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                        }
                    }
                } catch (se: SecurityException) {
                    Log.w(TAG, "Not allowed to change ringer mode: ${se.message}")
                }

            } catch (t: Throwable) {
                Log.e(TAG, "applySilenceMode failed: ${t.message}", t)
                throw t
            }
        }
    }

    /**
     * Restore previously saved volumes and system modes from DataStore.
     * If saved data is not present, this attempts a safe best-effort restore (no-ops).
     */
    suspend fun restoreVolume(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val prefs = UserPreferences(context)

                // Read saved volumes and system modes
                val (savedMedia, savedRing, savedNotif) = prefs.savedVolumes.first()
                val savedAlarm = prefs.savedAlarmVol.first()
                val savedRingerMode = prefs.savedRingerMode.first()
                val savedFilter = prefs.savedInterruptionFilter.first()

                // Restore interruption filter / DND first (if permission granted)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notifManager.isNotificationPolicyAccessGranted) {
                        if (savedFilter >= 0) {
                            notifManager.setInterruptionFilter(savedFilter)
                        } else {
                            notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        }
                    }
                } catch (se: SecurityException) {
                    Log.w(TAG, "Cannot set interruption filter: ${se.message}")
                }

                // Restore ringer mode
                try {
                    if (savedRingerMode >= 0) {
                        audioManager.ringerMode = savedRingerMode
                    } else {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    }
                } catch (se: SecurityException) {
                    Log.w(TAG, "Cannot set ringer mode: ${se.message}")
                }

                // Restore stream volumes
                try {
                    if (savedMedia >= 0) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedMedia, 0)
                    if (savedRing >= 0) audioManager.setStreamVolume(AudioManager.STREAM_RING, savedRing, 0)
                    if (savedNotif >= 0) audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, savedNotif, 0)
                    if (savedAlarm >= 0) audioManager.setStreamVolume(AudioManager.STREAM_ALARM, savedAlarm, 0)
                } catch (se: SecurityException) {
                    Log.w(TAG, "Cannot restore stream volumes: ${se.message}")
                }

                // Clear saved metadata in DataStore so we don't restore repeatedly
                prefs.clearSilenceState()

            } catch (t: Throwable) {
                Log.e(TAG, "restoreVolume failed: ${t.message}", t)
                throw t
            }
        }
    }
}
