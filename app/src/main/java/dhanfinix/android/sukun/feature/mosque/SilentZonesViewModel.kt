package dhanfinix.android.sukun.feature.mosque

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dhanfinix.android.sukun.core.database.SukunDatabase
import dhanfinix.android.sukun.core.database.entity.SilentZone
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.core.location.GeofenceManager
import dhanfinix.android.sukun.worker.SilenceReceiver
import dhanfinix.android.sukun.core.notification.NotificationHelper
import android.content.Intent
import android.location.Location
import android.location.Geocoder
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.annotation.SuppressLint
import dhanfinix.android.sukun.R

data class SilentZonesUiState(
    val zones: List<SilentZone> = emptyList(),
    val userLat: Double? = null,
    val userLng: Double? = null,
    val isLocationFresh: Boolean = false,
    val isLoading: Boolean = false,
    val isFineLocationGranted: Boolean = false,
    val isBackgroundLocationGranted: Boolean = false,
    val isDndAccessGranted: Boolean = false,
    val activeZoneId: Long? = null,
    val errorMessage: String? = null
)

data class MapSuggestion(
    val title: String,
    val subtitle: String?,
    val latitude: Double,
    val longitude: Double
)

class SilentZonesViewModel(private val application: Application) : AndroidViewModel(application) {
    private val database = SukunDatabase.getDatabase(application)
    private val silentZoneDao = database.silentZoneDao()
    private val userPrefs = UserPreferences(application)
    private val geofenceManager = GeofenceManager(application)
    private val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                _uiState.update { it.copy(userLat = location.latitude, userLng = location.longitude, isLocationFresh = true) }
                // Persist to userPrefs for other features
                viewModelScope.launch {
                    userPrefs.setLocation(location.latitude, location.longitude)
                }
            }
        }
    }

    private val _uiState = MutableStateFlow(SilentZonesUiState())
    val uiState: StateFlow<SilentZonesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            silentZoneDao.getAllSilentZones().collect { zones ->
                _uiState.update { it.copy(zones = zones) }
                // Re-sync all enabled geofences if needed
                reSyncGeofences(zones)
            }
        }
        viewModelScope.launch {
            val lat = userPrefs.latitude.first()
            val lng = userPrefs.longitude.first()
            _uiState.update { it.copy(userLat = lat, userLng = lng) }
            startLocationUpdates()
        }
        viewModelScope.launch {
            userPrefs.activeSilentZoneId.collect { id ->
                _uiState.update { it.copy(activeZoneId = id) }
            }
        }
        refreshPermissions()
    }

    fun refreshPermissions() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(application, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(application, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Automatic on older versions
        }

        val dndGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }

        _uiState.update { it.copy(
            isFineLocationGranted = fineLocationGranted,
            isBackgroundLocationGranted = backgroundLocationGranted,
            isDndAccessGranted = dndGranted
        ) }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(application, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(application, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).apply {
                setMinUpdateIntervalMillis(2000L)
            }.build()

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            _uiState.update { it.copy(isLoading = true) }
        } else {
            _uiState.update { it.copy(isLocationFresh = true) }
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _uiState.update { it.copy(isLoading = false) }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }

    private fun reSyncGeofences(zones: List<SilentZone>) {
        val enabledZones = zones.filter { it.isEnabled }
        if (enabledZones.isEmpty()) {
            geofenceManager.removeAllGeofences()
        } else {
            // Batch update for reliability and precision
            geofenceManager.addAllGeofences(enabledZones)
        }
    }

    private fun triggerZoneManually(zone: SilentZone) {
        viewModelScope.launch {
            userPrefs.setActiveSilentZoneId(zone.id)
            
            if (zone.isAutoSilent) {
                val durationToUse = zone.silenceDuration ?: 30 // Default 30 min to match receiver
                val silenceIntent = Intent(application, SilenceReceiver::class.java).apply {
                    action = SilenceReceiver.ACTION_START_SILENCE
                    putExtra(SilenceReceiver.KEY_PRAYER_NAME_STRING, application.getString(R.string.label_location_prefix, zone.name))
                    putExtra(SilenceReceiver.KEY_DURATION_MIN, durationToUse)
                    addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                }
                application.sendBroadcast(silenceIntent)
                NotificationHelper.showGeofenceStatusNotification(application, zone.name, true, true, zone.silenceDuration)
            } else {
                NotificationHelper.showGeofenceStatusNotification(application, zone.name, true, false, zone.silenceDuration)
            }
        }
    }

    private fun isUserInsideZone(zone: SilentZone): Boolean {
        val userLat = _uiState.value.userLat ?: return false
        val userLng = _uiState.value.userLng ?: return false
        
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLng, zone.latitude, zone.longitude, results)
        return results[0] <= zone.radius
    }

    fun addSilentZone(name: String, lat: Double, lng: Double, radius: Float, isAutoSilent: Boolean = true, silenceDuration: Int? = null) {
        viewModelScope.launch {
            val zone = SilentZone(
                name = name, 
                latitude = lat, 
                longitude = lng, 
                radius = radius,
                isAutoSilent = isAutoSilent,
                silenceDuration = silenceDuration
            )
            val id = silentZoneDao.insertSilentZone(zone)
            val savedZone = zone.copy(id = id)
            geofenceManager.addGeofence(savedZone)
            
            // Immediate check: if user is already inside the new zone, trigger it!
            if (isUserInsideZone(savedZone)) {
                triggerZoneManually(savedZone)
            }
        }
    }

    fun deleteSilentZone(zone: SilentZone) {
        viewModelScope.launch {
            silentZoneDao.deleteSilentZone(zone)
            geofenceManager.removeGeofence(zone.id)
            
            // If this was the active zone, stop silence
            if (_uiState.value.activeZoneId == zone.id) {
                userPrefs.setActiveSilentZoneId(null)
                val stopIntent = Intent(application, SilenceReceiver::class.java).apply {
                    action = SilenceReceiver.ACTION_STOP_SILENCE
                    addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                }
                application.sendBroadcast(stopIntent)
                NotificationHelper.showGeofenceStatusNotification(application, zone.name, false)
            }
        }
    }

    fun toggleSilentZone(zone: SilentZone) {
        viewModelScope.launch {
            val newStatus = !zone.isEnabled
            silentZoneDao.updateEnabledStatus(zone.id, newStatus)
            if (newStatus) {
                val updatedZone = zone.copy(isEnabled = newStatus)
                geofenceManager.addGeofence(updatedZone)
                
                // Immediate check if user is already inside
                if (isUserInsideZone(updatedZone)) {
                    triggerZoneManually(updatedZone)
                }
            } else {
                geofenceManager.removeGeofence(zone.id)
                // If this was active, stop it
                if (_uiState.value.activeZoneId == zone.id) {
                    userPrefs.setActiveSilentZoneId(null)
                    val stopIntent = Intent(application, SilenceReceiver::class.java).apply {
                        action = SilenceReceiver.ACTION_STOP_SILENCE
                        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    }
                    application.sendBroadcast(stopIntent)
                    NotificationHelper.showGeofenceStatusNotification(application, zone.name, false)
                }
            }
        }
    }

    fun toggleAutoSilent(zone: SilentZone) {
        viewModelScope.launch {
            val newStatus = !zone.isAutoSilent
            silentZoneDao.updateAutoSilentStatus(zone.id, newStatus)
        }
    }

    fun searchLocation(
        query: String,
        onResult: (Double, Double) -> Unit,
        onError: (String) -> Unit
    ) {
        if (query.isBlank()) {
            onError(application.getString(R.string.err_enter_location_search))
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    withContext(Dispatchers.Main) {
                        onError(application.getString(R.string.err_geocoder_unavailable_device))
                    }
                    return@launch
                }
                val results = geocode(query, 1)
                val first = results?.firstOrNull()
                if (first != null) {
                    withContext(Dispatchers.Main) {
                        onResult(first.latitude, first.longitude)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError(application.getString(R.string.err_location_not_found_search))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(application.getString(R.string.err_search_failed))
                }
            }
        }
    }

    fun searchLocationSuggestions(
        query: String,
        onResult: (List<MapSuggestion>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (query.isBlank()) {
            onResult(emptyList())
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    withContext(Dispatchers.Main) { onError(application.getString(R.string.err_geocoder_unavailable)) }
                    return@launch
                }
                val results = geocode(query, 3).orEmpty()
                val suggestions = results.mapNotNull { address ->
                    val full = address.getAddressLine(0)
                    val line1 = address.getAddressLine(1)
                    val feature = address.featureName
                    val locality = address.locality
                    val admin = address.adminArea
                    val subLocality = address.subLocality
                    val thoroughfare = address.thoroughfare
                    val subThoroughfare = address.subThoroughfare
                    val candidates = listOfNotNull(
                        feature,
                        locality,
                        admin,
                        subLocality,
                        thoroughfare,
                        subThoroughfare,
                        full,
                        line1
                    )
                    val bestNamed = candidates.firstOrNull()
                    Log.d(
                        "SilentZoneSearch",
                        "Geocoder candidates for \"$query\": ${candidates.joinToString(" | ")}"
                    )
                    if (bestNamed == null) {
                        null
                    } else {
                        val subtitle = if (full != null && full != bestNamed) full else line1
                        MapSuggestion(bestNamed, subtitle, address.latitude, address.longitude)
                    }
                }
                withContext(Dispatchers.Main) { onResult(suggestions) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(application.getString(R.string.err_search_failed)) }
            }
        }
    }

    private suspend fun geocode(query: String, maxResults: Int): List<android.location.Address>? {
        val geocoder = Geocoder(application)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocationName(
                    query,
                    maxResults,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            cont.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            cont.resume(emptyList())
                        }
                    }
                )
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(query, maxResults)
        }
    }

    fun updateSilentZone(zone: SilentZone) {
        viewModelScope.launch {
            silentZoneDao.updateSilentZone(zone)
            if (zone.isEnabled) {
                geofenceManager.addGeofence(zone)
                // If it's the active zone, we might need to update its status, but the 
                // receiver handles the silence end time when it next triggers.
                // For now, re-adding the geofence to the system is the priority.
            } else {
                geofenceManager.removeGeofence(zone.id)
            }
        }
    }
}
