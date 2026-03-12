package dhanfinix.android.sukun.core.utils

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log

class VolumeController(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun setSilentMode(enabled: Boolean) {
        try {
            if (enabled) {
                // Check for Do Not Disturb permission if needed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
                    Log.w("VolumeController", "Missing Do Not Disturb permission")
                    return
                }
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                Log.d("VolumeController", "Ringer mode set to SILENT")
            } else {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                Log.d("VolumeController", "Ringer mode set to NORMAL")
            }
        } catch (e: Exception) {
            Log.e("VolumeController", "Failed to set ringer mode", e)
        }
    }

    fun isSilentMode(): Boolean {
        return audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
    }
}
