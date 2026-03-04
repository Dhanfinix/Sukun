package dhanfinix.android.sukun.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import dhanfinix.android.sukun.MainActivity
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.worker.SilenceReceiver

/**
 * Manages the "Sukun Active" notification channel and countdown notification.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "sukun_silence_channel"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showSilenceNotification(context: Context, prayerName: String, endTimeMs: Long) {
        createChannel(context)

        // Tap notification → open app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Stop Mute
        val stopIntent = Intent(context, SilenceReceiver::class.java).apply {
            action = SilenceReceiver.ACTION_STOP_SILENCE
        }
        val stopPending = PendingIntent.getBroadcast(
            context, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingMs = endTimeMs - System.currentTimeMillis()
        val chronometerBase = android.os.SystemClock.elapsedRealtime() + remainingMs

        val formattedTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(endTimeMs))
        val contentText = "$prayerName • ${context.getString(R.string.silence_ends_at, formattedTime)}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.silence_active))
            .setContentText(contentText)
            .setWhen(chronometerBase)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openAppPending)
            .addAction(R.drawable.ic_notification, context.getString(R.string.stop_silence), stopPending)
            // Safety net: auto-dismiss the notification when silence ends,
            // in case RestoreWorker is delayed by Doze/battery saver.
            // This prevents the Chronometer from going into negative values.
            .setTimeoutAfter(remainingMs)

        val notification = builder.build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}
