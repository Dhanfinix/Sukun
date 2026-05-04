package dhanfinix.android.sukun.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes Glance refreshes so different widget providers do not race each other.
 */
object WidgetUpdateCoordinator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    fun refreshAllAsync(context: Context) {
        scope.launch {
            refreshAll(context)
        }
    }

    fun refreshPrayerAsync(context: Context) {
        scope.launch {
            refreshPrayer(context)
        }
    }

    fun refreshSilenceAsync(context: Context) {
        scope.launch {
            refreshSilence(context)
        }
    }

    suspend fun refreshAll(context: Context) {
        mutex.withLock {
            PrayerWidget().updateAll(context)
            SilenceWidget().updateAll(context)
        }
    }

    suspend fun refreshPrayer(context: Context) {
        mutex.withLock {
            PrayerWidget().updateAll(context)
        }
    }

    suspend fun refreshSilence(context: Context) {
        mutex.withLock {
            SilenceWidget().updateAll(context)
        }
    }
}
