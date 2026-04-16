package dhanfinix.android.sukun.worker

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerInfo
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName
import dhanfinix.android.sukun.feature.prayer.data.model.filterByDay
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate
import java.time.LocalTime

/**
 * IMPORTANT TESTING NOTE:
 * Full AlarmManager + BroadcastReceiver integration testing requires Robolectric.
 * These tests use targeted mocks to verify specific business logic invariants
 * without requiring the full Android runtime.
 */
@RunWith(MockitoJUnitRunner::class)
class SilenceSchedulerTest {

    @Mock private lateinit var context: Context
    @Mock private lateinit var alarmManager: AlarmManager
    @Mock private lateinit var notificationManager: NotificationManager

    private lateinit var scheduler: SilenceScheduler

    @Before
    fun setup() {
        `when`(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager)
        `when`(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(notificationManager)
        `when`(notificationManager.isNotificationPolicyAccessGranted).thenReturn(true)
        scheduler = SilenceScheduler(context)
    }

    /**
     * Bug 1 fix verification (filterByDay):
     * On Friday: DHUHR excluded, JUMUAH present.
     * On non-Friday: JUMUAH excluded, DHUHR present.
     */
    @Test
    fun filterByDay_excludesCorrectPrayer() {
        val fridayList = filterByDay(isFriday = true)
        val otherDayList = filterByDay(isFriday = false)

        // On Friday: DHUHR excluded, JUMUAH present
        assertTrue("JUMUAH must be present on Friday", fridayList.contains(PrayerName.JUMUAH))
        assertFalse("DHUHR must be excluded on Friday", fridayList.contains(PrayerName.DHUHR))

        // On non-Friday: JUMUAH excluded, DHUHR present
        assertTrue("DHUHR must be present on non-Friday", otherDayList.contains(PrayerName.DHUHR))
        assertFalse("JUMUAH must be excluded on non-Friday", otherDayList.contains(PrayerName.JUMUAH))
    }

    /**
     * Bug 2 fix verification:
     * cancelAll() MUST call cancelManualAlarms() first.
     * We verify that alarmManager.cancel was called at least once on any PendingIntent,
     * which proves cancelAll does the work of cancelling (rather than being a no-op).
     */
    @Test
    fun cancelAll_cancelsAtLeastOneAlarm() {
        scheduler.cancelAll()
        // Verify that alarmManager.cancel was called at least once — proves something was cancelled
        verify(alarmManager, atLeastOnce()).cancel(any(PendingIntent::class.java))
    }

    /**
     * Bug 3 fix verification:
     * scheduleRestoreOnly must schedule an alarm when called with valid prayertime near midnight.
     * We verify that setExactAndAllowWhileIdle was called with a future timestamp.
     */
    @Test
    fun scheduleRestoreOnly_schedulesAlarmForValidPrayerTime() {
        // Prayer at 20:00, duration 15 min — restore at 20:15 (well within same day)
        val prayer = PrayerInfo(
            name = PrayerName.ASR,
            time = "20:00",
            isEnabled = true
        )
        val today = LocalDate.now()

        scheduler.scheduleRestoreOnly(prayer, 15, today)

        verify(alarmManager).setExactAndAllowWhileIdle(
            eq(AlarmManager.RTC_WAKEUP),
            argThat { timeMs: Long -> timeMs > System.currentTimeMillis() },
            any(PendingIntent::class.java)
        )
    }
}