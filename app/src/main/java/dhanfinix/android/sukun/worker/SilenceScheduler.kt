package dhanfinix.android.sukun.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerInfo
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

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
    fun scheduleAll(
        prayersToday: List<PrayerInfo>,
        prayersTomorrow: List<PrayerInfo>,
        durationMin: Int
    ) {
        cancelAll()

        val now = LocalTime.now()

        // For each prayer, we decide whether to schedule today's instance or tomorrow's.
        prayersToday.forEach { todayPrayer ->
            if (!todayPrayer.isEnabled) return@forEach

            val todayTime = parseTime(todayPrayer.time) ?: return@forEach
            
            if (todayTime.isAfter(now)) {
                // Today's adhan is still in the future
                scheduleSinglePrayer(todayPrayer, durationMin, LocalDate.now())
            } else {
                // Today's adhan passed, schedule tomorrow's instead
                val tomorrowPrayer = prayersTomorrow.find { it.name == todayPrayer.name }
                if (tomorrowPrayer?.isEnabled == true) {
                    scheduleSinglePrayer(tomorrowPrayer, durationMin, LocalDate.now().plusDays(1))
                }
            }
        }
        
        scheduleMidnightReset()
    }

    fun scheduleManual(durationMin: Int) {
        // Cancel any prior manual restore alarm before scheduling a new one
        cancelManualAlarms()

        // For manual, we start silence IMMEDIATELY and schedule restore in the future
        val startIntent = Intent(context, SilenceReceiver::class.java).apply {
            action = SilenceReceiver.ACTION_START_SILENCE
            putExtra(SilenceWorker.KEY_PRAYER_NAME, "Manual")
            putExtra(SilenceWorker.KEY_DURATION_MIN, durationMin)
        }
        context.sendBroadcast(startIntent)

        val restoreTimeMs = System.currentTimeMillis() + (durationMin * 60 * 1000L)
        val pendingRestore = getPendingIntent(SilenceReceiver.ACTION_STOP_SILENCE, REQUEST_CODE_MANUAL_RESTORE)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            restoreTimeMs,
            pendingRestore
        )
    }

    fun stopSilence() {
        // Only cancel the manual restore alarm — do NOT touch prayer-scheduled alarms!
        cancelManualAlarms()
        // Immediately restore volumes
        val stopIntent = Intent(context, SilenceReceiver::class.java).apply {
            action = SilenceReceiver.ACTION_STOP_SILENCE
        }
        context.sendBroadcast(stopIntent)
    }

    private fun cancelManualAlarms() {
        workManager.cancelAllWorkByTag(SilenceScheduler.TAG_SUKUN)
        // requestCode 0 was the old default for manual alarms
        alarmManager.cancel(getPendingIntent(SilenceReceiver.ACTION_START_SILENCE, 0))
        alarmManager.cancel(getPendingIntent(SilenceReceiver.ACTION_STOP_SILENCE, 0))
        // New dedicated request code for manual restore
        alarmManager.cancel(getPendingIntent(SilenceReceiver.ACTION_STOP_SILENCE, REQUEST_CODE_MANUAL_RESTORE))
    }

    fun cancelAll() {
        workManager.cancelAllWorkByTag(TAG_SUKUN)
        
        // Cancel default/manual intents (requestCode = 0)
        alarmManager.cancel(getPendingIntent(SilenceReceiver.ACTION_START_SILENCE, 0))
        alarmManager.cancel(getPendingIntent(SilenceReceiver.ACTION_STOP_SILENCE, 0))

        // Cancel specific prayer scheduled intents
        dhanfinix.android.sukun.feature.prayer.data.model.PrayerName.entries.forEach { prayerName ->
            val pendingStart = getPendingIntent(SilenceReceiver.ACTION_START_SILENCE, prayerName.ordinal)
            val pendingRestore = getPendingIntent(SilenceReceiver.ACTION_STOP_SILENCE, prayerName.ordinal + 100)
            alarmManager.cancel(pendingStart)
            alarmManager.cancel(pendingRestore)
        }
    }

    private fun scheduleSinglePrayer(prayer: PrayerInfo, durationMin: Int, date: LocalDate) {
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

        val startIntent = Intent(context, SilenceReceiver::class.java).apply {
            action = SilenceReceiver.ACTION_START_SILENCE
            putExtra(SilenceWorker.KEY_PRAYER_NAME, prayer.name.displayName)
            putExtra(SilenceWorker.KEY_DURATION_MIN, durationMin)
        }
        
        // We use different request codes if we wanted multiple concurrent alarms, 
        // but since we only have one "next" silence at a time in the logic usually,
        // or we just want the unique prayer. Let's use prayer name hash for uniqueness.
        val requestCode = prayer.name.ordinal

        val pendingStart = PendingIntent.getBroadcast(
            context,
            requestCode,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingStart
        )

        // Schedule Restore (Adhan + Duration)
        val restoreTimeMs = calendar.timeInMillis + (durationMin * 60 * 1000L)
        val pendingRestore = PendingIntent.getBroadcast(
            context,
            requestCode + 100, // Offset to avoid collision
            getRestoreIntent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            restoreTimeMs,
            pendingRestore
        )
    }

    private fun getPendingIntent(action: String, requestCode: Int = 0): PendingIntent {
        val intent = Intent(context, SilenceReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getRestoreIntent(): Intent {
        return Intent(context, SilenceReceiver::class.java).apply {
            action = SilenceReceiver.ACTION_STOP_SILENCE
        }
    }

    private fun scheduleMidnightReset() {
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
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
    }
}
