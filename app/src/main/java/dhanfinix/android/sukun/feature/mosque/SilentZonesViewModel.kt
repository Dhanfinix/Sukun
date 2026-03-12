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
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.annotation.SuppressLint

data class SilentZonesUiState(
    val zones: List<SilentZone> = emptyList(),
    val userLat: Double? = null,
    val userLng: Double? = null,
    val isLocationFresh: Boolean = false,
    val isLoading: Boolean = false,
    val isBackgroundLocationGranted: Boolean = false,
    val isDndAccessGranted: Boolean = false,
    val activeZoneId: Long? = null,
    val errorMessage: String? = null
)

class SilentZonesViewModel(private val application: Application) : AndroidViewModel(application) {
    private val database = SukunDatabase.getDatabase(application)
    private val silentZoneDao = database.silentZoneDao()
    private val userPrefs = UserPreferences(application)
    private val geofenceManager = GeofenceManager(application)
    private val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
            fetchCurrentLocation()
        }
        viewModelScope.launch {
            userPrefs.activeSilentZoneId.collect { id ->
                _uiState.update { it.copy(activeZoneId = id) }
            }
        }
        refreshPermissions()
    }

    fun refreshPermissions() {
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
            isBackgroundLocationGranted = backgroundLocationGranted,
            isDndAccessGranted = dndGranted
        ) }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(application, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(application, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    _uiState.update { it.copy(userLat = location.latitude, userLng = location.longitude, isLocationFresh = true) }
                } else {
                    _uiState.update { it.copy(isLocationFresh = true) }
                }
            }.addOnFailureListener {
                _uiState.update { it.copy(isLocationFresh = true) }
            }
        } else {
            _uiState.update { it.copy(isLocationFresh = true) }
        }
    }

    private fun reSyncGeofences(zones: List<SilentZone>) {
        // Simple strategy: remove all and add enabled ones
        // In a real app, we'd only update changes
        geofenceManager.removeAllGeofences()
        zones.filter { it.isEnabled }.forEach { zone ->
            geofenceManager.addGeofence(zone)
        }
    }

    private fun triggerZoneManually(zone: SilentZone) {
        viewModelScope.launch {
            userPrefs.setActiveSilentZoneId(zone.id)
            
            if (zone.isAutoSilent) {
                val durationToUse = zone.silenceDuration ?: 720
                val silenceIntent = Intent(application, SilenceReceiver::class.java).apply {
                    action = SilenceReceiver.ACTION_START_SILENCE
                    putExtra(SilenceReceiver.KEY_PRAYER_NAME_STRING, "Location: ${zone.name}")
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
