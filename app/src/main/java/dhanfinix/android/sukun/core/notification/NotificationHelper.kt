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
    private const val REMINDER_CHANNEL_ID = "sukun_reminder_channel"
    private const val GEOFENCE_CHANNEL_ID = "sukun_geofence_channel"

    const val ID_ACTIVE_STATUS = 1001
    const val ID_ALERT_EVENT = 1002

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
        
        val endsAtLabel = localizedContext.getString(R.string.notif_ends_at, endTimeDisplay)

        // ── Compact (collapsed) view ──────────────────────────────────────────
        val compactView = RemoteViews(localizedContext.packageName, R.layout.notification_sukun_countdown)
        compactView.setTextViewText(R.id.notification_title, localizedContext.getString(R.string.notif_title))
        compactView.setTextViewText(R.id.notification_text, prayerName)
        compactView.setChronometer(R.id.notification_chronometer, chronometerBase, "%s", true)
        compactView.setChronometerCountDown(R.id.notification_chronometer, true)

        // ── Expanded (big content) view ───────────────────────────────────────
        val expandedView = RemoteViews(localizedContext.packageName, R.layout.notification_sukun_expanded)
        expandedView.setTextViewText(R.id.notif_expanded_prayer, prayerName)
        expandedView.setTextViewText(R.id.notif_expanded_prayer, prayerName)
        expandedView.setTextViewText(R.id.notif_expanded_status, localizedContext.getString(R.string.silence_active))
        expandedView.setChronometer(R.id.notif_expanded_chronometer, chronometerBase, "%s", true)
        expandedView.setChronometerCountDown(R.id.notif_expanded_chronometer, true)
        expandedView.setTextViewText(R.id.notif_expanded_end_time, endsAtLabel)
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
        manager.notify(ID_ACTIVE_STATUS, builder.build())
        // When active silence starts, clear any transient alerts (like "Entered Area" or "Reminder")
        manager.cancel(ID_ALERT_EVENT)
    }

    fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(ID_ACTIVE_STATUS)
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context, 1, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.reminder_title_context, prayerName.withIsolate())
        val minutesStr = java.lang.String.format(java.util.Locale.US, "%d", minutesBefore).withIsolate()
        val content = context.getString(R.string.reminder_msg_context, prayerName.withIsolate(), minutesStr)

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppPending)
            .addAction(0, context.getString(R.string.btn_prepare), openAppPending)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(ID_ALERT_EVENT, builder.build())
    }

    fun showGeofenceStatusNotification(
        context: Context, 
        zoneName: String, 
        isEntering: Boolean,
        isAutoSilent: Boolean = true,
        silenceDuration: Int? = null
    ) {
        // Optimization: If it's auto-silent, the "Active Silence" notification (ID 1001) 
        // will show up immediately. We don't need a separate "Entered" alert (ID 1002).
        if (isEntering && isAutoSilent) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (manager.getNotificationChannel(GEOFENCE_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                GEOFENCE_CHANNEL_ID, 
                context.getString(R.string.geofence_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val title = if (isEntering) context.getString(R.string.notif_geofence_entered, zoneName) 
                    else context.getString(R.string.notif_geofence_left, zoneName)
        val content = if (isEntering) context.getString(R.string.notif_geofence_enter_action)
                      else context.getString(R.string.notif_volume_restored)

        val builder = NotificationCompat.Builder(context, GEOFENCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (isEntering) {
            val silenceIntent = Intent(context, SilenceReceiver::class.java).apply {
                action = SilenceReceiver.ACTION_START_SILENCE
                putExtra(SilenceReceiver.KEY_PRAYER_NAME_STRING, context.getString(R.string.label_location_prefix, zoneName))
                putExtra(SilenceReceiver.KEY_DURATION_MIN, silenceDuration ?: 720) 
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }
            val silencePending = PendingIntent.getBroadcast(
                context, 3001, silenceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_notification, context.getString(R.string.action_silence_now), silencePending)
            builder.setContentIntent(silencePending)
        }

        manager.notify(ID_ALERT_EVENT, builder.build())
    }
}