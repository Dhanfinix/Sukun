package dhanfinix.android.sukun.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationResult
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.feature.mosque.data.MosqueRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!LocationResult.hasResult(intent)) return

        val locationResult = LocationResult.extractResult(intent) ?: return
        val lastLocation = locationResult.lastLocation ?: return

        val userPrefs = UserPreferences(context)
        val mosqueRepository = MosqueRepository(context)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isEnabled = userPrefs.isAutoMosqueSilenceEnabled.first()
                if (!isEnabled) {
                    Log.d("LocationReceiver", "Auto-mosque discovery disabled, ignoring update")
                    return@launch
                }

                Log.d("LocationReceiver", "Background location received: ${lastLocation.latitude}, ${lastLocation.longitude}")
                mosqueRepository.fetchAndSaveMosques(lastLocation.latitude, lastLocation.longitude)
            } catch (e: Exception) {
                Log.e("LocationReceiver", "Error processing background location", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
