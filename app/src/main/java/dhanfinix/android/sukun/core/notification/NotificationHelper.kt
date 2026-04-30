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

    private const val CHANNEL_ID = "sukun_silence_channel_v2"
    private const val REMINDER_CHANNEL_ID = "sukun_reminder_channel"
    private const val PROGRESS_MAX = 1000
    private const val EXPANDED_REBIND_INTERVAL_MS = 15_000L
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val reminderChannel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            context.getString(R.string.prayer_reminder),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.reminder_desc)
        }
        manager.createNotificationChannel(reminderChannel)
    }

    fun showSilenceNotification(
        context: Context,
        prayerName: String,
        startTimeMs: Long,
        endTimeMs: Long,
        timeFormat: TimeFormat = TimeFormat.AUTO,
        extendMinutes: Int = 5
    ) {
        val locale = if (context.resources.configuration.locales[0].language == "ar") {
             Locale("ar")
        } else Locale.getDefault()

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)

        createChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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

        val extendIntent = Intent(context, SilenceReceiver::class.java).apply {
            action = SilenceReceiver.ACTION_EXTEND_SILENCE
        }
        val extendPending = PendingIntent.getBroadcast(
            context, 10, extendIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nowMs = System.currentTimeMillis()

        // Chronometer base (elapsed realtime domain)
        val remainingMs = endTimeMs - nowMs
        val chronometerBase = android.os.SystemClock.elapsedRealtime() + remainingMs
        val totalMs = (endTimeMs - startTimeMs).coerceAtLeast(1L)
        val elapsedMs = (nowMs - startTimeMs).coerceIn(0L, totalMs)
        val progress = ((elapsedMs * PROGRESS_MAX) / totalMs).toInt()

        // Human-readable end time, e.g. "05:30 PM"
        val formattedEndTime = SimpleDateFormat("HH:mm", Locale.US).format(Date(endTimeMs))
            .formatTime(context, timeFormat)
            
        val endTimeDisplay = if (formattedEndTime.session != null) {
            "${formattedEndTime.time} ${formattedEndTime.session}".withIsolate()
        } else {
            formattedEndTime.time.withIsolate()
        }
        
        val endsAtLabel = localizedContext.getString(R.string.notif_ends_at, endTimeDisplay)
        val expandedLayout = if ((nowMs / EXPANDED_REBIND_INTERVAL_MS) % 2L == 0L) {
            R.layout.notification_sukun_expanded
        } else {
            R.layout.notification_sukun_expanded_alt
        }

        // ── Compact (collapsed) view ──────────────────────────────────────────
        val compactView = RemoteViews(localizedContext.packageName, R.layout.notification_sukun_countdown)
        compactView.setTextViewText(R.id.notification_title, localizedContext.getString(R.string.notif_title))
        compactView.setTextViewText(R.id.notification_text, prayerName)
        compactView.setChronometer(R.id.notification_chronometer, chronometerBase, "%s", true)
        compactView.setChronometerCountDown(R.id.notification_chronometer, true)

        // ── Expanded (big content) view ───────────────────────────────────────
        val expandedView = RemoteViews(localizedContext.packageName, expandedLayout)
        expandedView.setTextViewText(R.id.notif_expanded_prayer, prayerName)
        expandedView.setTextViewText(R.id.notif_expanded_status, localizedContext.getString(R.string.silence_active))
        expandedView.setChronometer(R.id.notif_expanded_chronometer, chronometerBase, "%s", true)
        expandedView.setChronometerCountDown(R.id.notif_expanded_chronometer, true)
        expandedView.setProgressBar(R.id.notif_expanded_progress, PROGRESS_MAX, progress, false)
        expandedView.setTextViewText(R.id.notif_expanded_end_time, endsAtLabel)
        expandedView.setOnClickPendingIntent(R.id.notif_expanded_stop, stopPending)
        expandedView.setTextViewText(R.id.notif_expanded_extend, "+${extendMinutes}m")
        expandedView.setOnClickPendingIntent(R.id.notif_expanded_extend, extendPending)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(compactView)
            .setCustomBigContentView(expandedView)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openAppPending)
            .setTimeoutAfter(remainingMs)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    fun showRestoredNotification(context: Context, prayerName: String) {
        val locale = if (context.resources.configuration.locales[0].language == "ar") {
            Locale("ar")
        } else Locale.getDefault()

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)

        createChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context, 2, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = localizedContext.getString(R.string.notif_restored_title)
        val content = localizedContext.getString(R.string.notif_restored_msg, prayerName.withIsolate())

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(openAppPending)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }

    fun showReminderNotification(context: Context, prayerName: String, minutesBefore: Int) {
        val locale = if (context.resources.configuration.locales[0].language == "ar") {
            Locale("ar")
        } else Locale.getDefault()

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)

        createChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context, 1, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.reminder_title_context, prayerName.withIsolate())
        val minutesStr = java.lang.String.format(Locale.US, "%d", minutesBefore).withIsolate()
        val content = context.getString(R.string.reminder_msg_context, prayerName.withIsolate(), minutesStr)

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppPending)
            .setTimeoutAfter(minutesBefore * 60 * 1000L)
            .addAction(0, context.getString(R.string.btn_prepare), openAppPending)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }
}
