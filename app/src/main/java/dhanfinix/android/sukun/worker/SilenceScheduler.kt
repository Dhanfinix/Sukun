package dhanfinix.android.sukun.worker

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.feature.prayer.data.PrayerRepository
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerInfo
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Schedules silence using AlarmManager for exact timing:
 * - SilenceWorker via SilenceReceiver at the exact adhan time
 * - RestoreWorker via SilenceReceiver at adhan + user-selected duration
 */
class SilenceScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedules the NEXT occurrence of each enabled prayer across a rolling 24-hour window.
     */
    suspend fun scheduleAll(
        prayersToday: List<PrayerInfo>,
        prayersTomorrow: List<PrayerInfo>,
        durations: Map<PrayerName, Int>,
        offsets: Map<PrayerName, Int> = emptyMap(),
        reminderEnabled: Boolean = false,
        reminderMinutes: Int = 10
    ) {
        cancelAll()

        val now = LocalTime.now()

        // For each prayer, we decide whether to schedule today's instance or tomorrow's.
        prayersToday.forEach { todayPrayer ->
            if (!todayPrayer.isEnabled) return@forEach

            val todayTime = parseTime(todayPrayer.time) ?: return@forEach
            val duration = durations[todayPrayer.name] ?: 15
            val restoreTime = todayTime.plusMinutes(duration.toLong())
            
            if (todayTime.isAfter(now)) {
                // Today's adhan is still in the future
                scheduleSinglePrayer(todayPrayer, duration, LocalDate.now(), reminderEnabled, reminderMinutes)
            } else if (now.isBefore(restoreTime) || (restoreTime.isBefore(todayTime) && now.isAfter(todayTime))) {
                // Adhan passed, but we are currently inside the silence window.
                // We MUST schedule the restore alarm so it comes back to normal!
                scheduleRestoreOnly(todayPrayer, duration, LocalDate.now())

                // Then also schedule tomorrow's full sequence
                val tomorrowPrayer = prayersTomorrow.find { it.name == todayPrayer.name }
                if (tomorrowPrayer?.isEnabled == true) {
                    scheduleSinglePrayer(tomorrowPrayer, durations[tomorrowPrayer.name] ?: 15, LocalDate.now().plusDays(1), reminderEnabled, reminderMinutes)
                }
            } else {
                // Today's adhan passed, schedule tomorrow's instead
                val tomorrowPrayer = prayersTomorrow.find { it.name == todayPrayer.name }
                if (tomorrowPrayer?.isEnabled == true) {
                    scheduleSinglePrayer(tomorrowPrayer, durations[tomorrowPrayer.name] ?: 15, LocalDate.now().plusDays(1), reminderEnabled, reminderMinutes)
                }
            }
        }

        // Update Widgets (Suspend for reliability)
        dhanfinix.android.sukun.feature.widget.PrayerWidget.updateAll(context)
        dhanfinix.android.sukun.feature.widget.SilenceWidget.updateAll(context)
    }

    private fun scheduleRestoreOnly(prayer: PrayerInfo, durationMin: Int, date: LocalDate) {
        val prayerTime = parseTime(prayer.time) ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, prayerTime.hour)
            set(Calendar.MINUTE, prayerTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val requestCode = prayer.name.ordinal

        val restoreTimeMs = calendar.timeInMillis + (durationMin * 60 * 1000L)
        if (restoreTimeMs > System.currentTimeMillis()) {
            val pendingRestore = PendingIntent.getBroadcast(
                context,
                requestCode + 100, // Offset to avoid collision
                getRestoreIntent(),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleExactAlarmSafely(restoreTimeMs, pendingRestore)
        }
    }

    fun scheduleManual(durationMin: Int) {
        // Cancel any prior manual restore alarm before scheduling a new one
        cancelManualRestoreAlarm()

        // For manual, we start silence IMMEDIATELY and schedule restore in the future
        val startIntent = Intent(context, SilenceReceiver::class.java).apply {
            action = SilenceReceiver.ACTION_START_SILENCE
            putExtra(SilenceReceiver.KEY_PRAYER_NAME, R.string.label_manual)
            putExtra(SilenceReceiver.KEY_DURATION_MIN, durationMin)
        }
        context.sendBroadcast(startIntent)

        val restoreTimeMs = System.currentTimeMillis() + (durationMin * 60 * 1000L)
        val pendingRestore = getPendingIntent(SilenceReceiver.ACTION_STOP_SILENCE, REQUEST_CODE_MANUAL_RESTORE)

        scheduleExactAlarmSafely(restoreTimeMs, pendingRestore)
    }

    fun stopSilence() {
        // Only cancel the manual restore alarm — do NOT touch prayer-scheduled alarms!
        cancelManualRestoreAlarm()
        // Immediately restore volumes
        val stopIntent = Intent(context, SilenceReceiver::class.java).apply {
            action = SilenceReceiver.ACTION_STOP_SILENCE
        }
        context.sendBroadcast(stopIntent)
    }

    /**
     * Extends an already-active silence by rescheduling the restore alarm to [newEndTimeMs].
     * Does NOT restart the silence — volumes stay muted by the already-running session.
     */
    suspend fun extendManual(newEndTimeMs: Long) {
        // Cancel the old restore alarm first
        cancelManualRestoreAlarm()
        // Schedule new restore alarm at the extended time
        val pendingRestore = getPendingIntent(SilenceReceiver.ACTION_STOP_SILENCE, REQUEST_CODE_MANUAL_RESTORE)
        scheduleExactAlarmSafely(newEndTimeMs, pendingRestore)
        // Update widgets to reflect new countdown (Suspend for reliability)
        dhanfinix.android.sukun.feature.widget.PrayerWidget.updateAll(context)
        dhanfinix.android.sukun.feature.widget.SilenceWidget.updateAll(context)
    }

    fun scheduleNotificationRefresh(delayMs: Long = NOTIFICATION_REFRESH_INTERVAL_MS) {
        val refreshTimeMs = System.currentTimeMillis() + delayMs
        val pendingRefresh = getPendingIntent(SilenceReceiver.ACTION_REFRESH_SILENCE_NOTIFICATION, REQUEST_CODE_NOTIFICATION_REFRESH)
        scheduleExactAlarmSafely(refreshTimeMs, pendingRefresh)
    }

    fun cancelNotificationRefresh() {
        alarmManager.cancel(getPendingIntent(SilenceReceiver.ACTION_REFRESH_SILENCE_NOTIFICATION, REQUEST_CODE_NOTIFICATION_REFRESH))
    }

    fun cancelManualRestoreAlarm() {
        // Cancel the dedicated request code for manual restore
        alarmManager.cancel(getPendingIntent(SilenceReceiver.ACTION_STOP_SILENCE, REQUEST_CODE_MANUAL_RESTORE))
    }

    fun cancelPrayerRestore(prayer: PrayerName) {
        val requestCode = prayer.ordinal + 100
        alarmManager.cancel(getPendingIntent(SilenceReceiver.ACTION_STOP_SILENCE, requestCode))
    }

    fun cancelAll() {
        // Cancel specific prayer scheduled intents
        dhanfinix.android.sukun.feature.prayer.data.model.PrayerName.entries.forEach { prayerName ->
            val pendingStart = getPendingIntent(SilenceReceiver.ACTION_START_SILENCE, prayerName.ordinal)
            val pendingRestore = getPendingIntent(SilenceReceiver.ACTION_STOP_SILENCE, prayerName.ordinal + 100)
            val pendingReminder = getPendingIntent(SilenceReceiver.ACTION_SHOW_REMINDER, prayerName.ordinal + 200)
            alarmManager.cancel(pendingStart)
            alarmManager.cancel(pendingRestore)
            alarmManager.cancel(pendingReminder)
        }
    }

    private fun scheduleSinglePrayer(
        prayer: PrayerInfo,
        durationMin: Int,
        date: LocalDate,
        reminderEnabled: Boolean = false,
        reminderMinutes: Int = 10
    ) {
        val prayerTime = parseTime(prayer.time) ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, prayerTime.hour)
            set(Calendar.MINUTE, prayerTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val requestCode = prayer.name.ordinal

        // 1. Schedule Reminder
        if (reminderEnabled && reminderMinutes > 0) {
            val reminderTimeMs = calendar.timeInMillis - (reminderMinutes * 60 * 1000L)
            if (reminderTimeMs > System.currentTimeMillis()) {
                val reminderIntent = Intent(context, SilenceReceiver::class.java).apply {
                    action = SilenceReceiver.ACTION_SHOW_REMINDER
                    putExtra(SilenceReceiver.KEY_PRAYER_NAME, prayer.name.nameRes)
                    putExtra(SilenceReceiver.KEY_REMINDER_MINUTES, reminderMinutes)
                }
                val pendingReminder = PendingIntent.getBroadcast(
                    context,
                    requestCode + 200, // Offset to avoid collision
                    reminderIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                // Use INEXACT alarm for reminder so it doesn't consume the exact alarm Doze quota!
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMs, pendingReminder)
            }
        }

        // 2. Schedule Silence (Only if DND permission is granted)
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notifManager.isNotificationPolicyAccessGranted) {
            val startIntent = Intent(context, SilenceReceiver::class.java).apply {
                action = SilenceReceiver.ACTION_START_SILENCE
                putExtra(SilenceReceiver.KEY_PRAYER_NAME, prayer.name.nameRes)
                putExtra(SilenceReceiver.KEY_DURATION_MIN, durationMin)
            }
            
            val pendingStart = PendingIntent.getBroadcast(
                context,
                requestCode,
                startIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleExactAlarmSafely(calendar.timeInMillis, pendingStart)

            // 3. Schedule Restore (Adhan + Duration)
            val restoreTimeMs = calendar.timeInMillis + (durationMin * 60 * 1000L)
            val pendingRestore = PendingIntent.getBroadcast(
                context,
                requestCode + 100, // Offset to avoid collision
                getRestoreIntent(),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleExactAlarmSafely(restoreTimeMs, pendingRestore)
        }
    }

    private fun getPendingIntent(action: String, requestCode: Int = 0): PendingIntent {
        val intent = Intent(context, SilenceReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getRestoreIntent(): Intent {
        return Intent(context, SilenceReceiver::class.java).apply {
            action = SilenceReceiver.ACTION_STOP_SILENCE
        }
    }

    fun scheduleMidnightReset() {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 5)
            set(Calendar.MILLISECOND, 0)
        }

        val intent = Intent(context, MidnightReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999, // Unique ID for midnight
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarmSafely(calendar.timeInMillis, pendingIntent)
    }

    private fun scheduleExactAlarmSafely(timeMs: Long, pendingIntent: PendingIntent) {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
        } catch (e: SecurityException) {
            // Fallback to inexact alarm if the exact permission was revoked
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
        }
    }

    private fun parseTime(timeStr: String): LocalTime? {
        return try {
            val parts = timeStr.split(":")
            LocalTime.of(parts[0].trim().toInt(), parts[1].trim().toInt())
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val TAG_SUKUN = "sukun_silence"
        // Dedicated request code for manual silence restore alarm (distinct from prayer codes 0..N+100)
        const val REQUEST_CODE_MANUAL_RESTORE = 9000
        const val REQUEST_CODE_NOTIFICATION_REFRESH = 9001
        const val NOTIFICATION_REFRESH_INTERVAL_MS = 15_000L
    }
}
