package dhanfinix.android.sukun.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Receiver triggered by AlarmManager at exact prayer/manual silence times.
 * Immediately enqueues a SilenceWorker or RestoreWorker.
 */
class SilenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val workManager = WorkManager.getInstance(context)

        when (action) {
            ACTION_START_SILENCE -> {
                val prayerName = intent.getStringExtra(SilenceWorker.KEY_PRAYER_NAME) ?: "Prayer"
                val durationMin = intent.getIntExtra(SilenceWorker.KEY_DURATION_MIN, 15)

                val silenceData = Data.Builder()
                    .putString(SilenceWorker.KEY_PRAYER_NAME, prayerName)
                    .putInt(SilenceWorker.KEY_DURATION_MIN, durationMin)
                    .build()

                val silenceWork = OneTimeWorkRequestBuilder<SilenceWorker>()
                    .setInputData(silenceData)
                    .addTag(SilenceScheduler.TAG_SUKUN)
                    .build()

                workManager.enqueue(silenceWork)
            }
            ACTION_STOP_SILENCE -> {
                val restoreWork = OneTimeWorkRequestBuilder<RestoreWorker>()
                    .addTag(SilenceScheduler.TAG_SUKUN)
                    .build()

                workManager.enqueue(restoreWork)
            }
        }
    }

    companion object {
        const val ACTION_START_SILENCE = "dhanfinix.android.sukun.START_SILENCE"
        const val ACTION_STOP_SILENCE = "dhanfinix.android.sukun.STOP_SILENCE"
    }
}
