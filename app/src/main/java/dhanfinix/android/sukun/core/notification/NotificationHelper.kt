package dhanfinix.android.sukun.core.notification

import android.app.NotificationChannel
import android.content.res.Configuration
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import dhanfinix.android.sukun.MainActivity
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.worker.SilenceReceiver
import dhanfinix.android.sukun.core.datastore.TimeFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dhanfinix.android.sukun.core.utils.formatTime
import dhanfinix.android.sukun.core.utils.withIsolate

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
        startTimeMs: Long,
        endTimeMs: Long,
        timeFormat: TimeFormat = TimeFormat.AUTO
    ) {
        val locale = if (context.resources.configuration.locales[0].language == "ar") {
             Locale("ar")
        } else Locale.getDefault()

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)

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

        // Human-readable end time, e.g. "05:30 PM"
        val formattedEndTime = SimpleDateFormat("HH:mm", Locale.US).format(Date(endTimeMs))
            .formatTime(context, timeFormat)
            
        val endTimeDisplay = if (formattedEndTime.session != null) {
            "${formattedEndTime.time} ${formattedEndTime.session}".withIsolate()
        } else {
            formattedEndTime.time.withIsolate()
        }
        
        val endsAtText = context.getString(R.string.notif_ends_at, endTimeDisplay)

        // ── Compact (collapsed) view ──────────────────────────────────────────
        val compactView = RemoteViews(localizedContext.packageName, R.layout.notification_sukun_countdown)
        compactView.setTextViewText(R.id.notification_text, prayerName)
        compactView.setChronometer(R.id.notification_chronometer, chronometerBase, "%s", true)
        compactView.setChronometerCountDown(R.id.notification_chronometer, true)

        // ── Expanded (big content) view ───────────────────────────────────────
        val expandedView = RemoteViews(localizedContext.packageName, R.layout.notification_sukun_expanded)
        expandedView.setTextViewText(R.id.notif_expanded_prayer, prayerName)
        expandedView.setChronometer(R.id.notif_expanded_chronometer, chronometerBase, "%s", true)
        expandedView.setChronometerCountDown(R.id.notif_expanded_chronometer, true)
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
            .setTimeoutAfter(remainingMs)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}