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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages the "Sukun Active" notification channel and countdown notification.
 *
 * Uses two RemoteViews layouts:
 *  - [R.layout.notification_sukun_countdown]: compact (collapsed) — one-liner + chronometer
 *  - [R.layout.notification_sukun_expanded]: big content (expanded) — large timer, end time, stop btn
 */
object NotificationHelper {

    private const val CHANNEL_ID = "sukun_silence_channel"
    const val NOTIFICATION_ID = 1001

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

    fun showSilenceNotification(
        context: Context,
        prayerName: String,
        startTimeMs: Long,   // when silence began — needed to compute progress
        endTimeMs: Long
    ) {
        createChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, SilenceReceiver::class.java).apply {
            action = SilenceReceiver.ACTION_STOP_SILENCE
        }
        val stopPending = PendingIntent.getBroadcast(
            context, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Chronometer base (elapsed realtime domain)
        val remainingMs = endTimeMs - System.currentTimeMillis()
        val chronometerBase = android.os.SystemClock.elapsedRealtime() + remainingMs

        // Human-readable end time, e.g. "14:53"
        val endTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(endTimeMs))
        val endsAtText = context.getString(R.string.notif_ends_at, endTimeFormatted)

        // ── Compact (collapsed) view ──────────────────────────────────────────
        val compactView = RemoteViews(context.packageName, R.layout.notification_sukun_countdown)
        compactView.setTextViewText(R.id.notification_text, prayerName)
        compactView.setChronometer(R.id.notification_chronometer, chronometerBase, "%s", true)

        // ── Expanded (big content) view ───────────────────────────────────────
        val expandedView = RemoteViews(context.packageName, R.layout.notification_sukun_expanded)
        expandedView.setTextViewText(R.id.notif_expanded_prayer, prayerName)
        expandedView.setChronometer(R.id.notif_expanded_chronometer, chronometerBase, "%s", true)
        expandedView.setTextViewText(R.id.notif_expanded_end_time, endsAtText)
        expandedView.setOnClickPendingIntent(R.id.notif_expanded_stop, stopPending)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(compactView)
            .setCustomBigContentView(expandedView)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openAppPending)
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.stop_silence),
                stopPending
            )
            .setTimeoutAfter(remainingMs)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}