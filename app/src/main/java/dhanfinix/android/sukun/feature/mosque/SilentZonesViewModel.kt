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
import com.google.android.gms.tasks.CancellationTokenSource
import dhanfinix.android.sukun.core.network.ApiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import android.annotation.SuppressLint
import android.widget.Toast
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.database.entity.SilentZoneSource

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
    val isLocationSilenceEnabled: Boolean = true,
    val isAutoMosqueSilenceEnabled: Boolean = true,
    val isAggressiveLocationEnabled: Boolean = false,
    val errorMessage: String? = null,
    /** Number of currently OS-registered geofences for auto-mosques */
    val activeGeofenceCount: Int = 0,
    /** Maximum allowed active mosque geofences */
    val maxGeofenceSlots: Int = 10
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
    private val mosqueRepository = dhanfinix.android.sukun.feature.mosque.data.MosqueRepository(application)
    private var singleFixJob: kotlinx.coroutines.Job? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                _uiState.update { it.copy(userLat = location.latitude, userLng = location.longitude, isLocationFresh = true, isLoading = false) }
                // Persist to userPrefs so foreground service distance checks are based on latest position
                viewModelScope.launch {
                    userPrefs.setLocation(location.latitude, location.longitude)
                    // Always fetch mosques when we get fresh location on map open (once)
                    if (_uiState.value.isAutoMosqueSilenceEnabled) {
                        mosqueRepository.fetchAndSaveMosques(location.latitude, location.longitude)
                    }
                }
            }
        }
    }

    private val _uiState = MutableStateFlow(SilentZonesUiState())
    val uiState: StateFlow<SilentZonesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                silentZoneDao.getAllSilentZones(),
                userPrefs.isAutoMosqueSilenceEnabled
            ) { zones, autoMosqueEnabled ->
                _uiState.update { it.copy(zones = zones) }
                // Re-sync geofences, filtering out mosques if disabled
                val effectiveZones = zones.filter { 
                    if (it.source == SilentZoneSource.AUTO_MOSQUE) autoMosqueEnabled else true
                }
                reSyncGeofences(effectiveZones)
            }.collect()
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
        viewModelScope.launch {
            var lastEnabled: Boolean? = null
            userPrefs.isLocationSilenceEnabled.collect { enabled ->
                _uiState.update { it.copy(isLocationSilenceEnabled = enabled) }
                if (enabled && lastEnabled == false) {
                    // Re-evaluate current location to see if we should trigger any zone
                    checkAndTriggerActiveZone()
                }
                lastEnabled = enabled
            }
        }
        viewModelScope.launch {
            var lastAutoMosque: Boolean? = null
            userPrefs.isAutoMosqueSilenceEnabled.collect { enabled ->
                _uiState.update { it.copy(isAutoMosqueSilenceEnabled = enabled) }
                // Only act if the value actually changed (not on initial emission)
                if (lastAutoMosque != null && lastAutoMosque != enabled) {
                    if (enabled) {
                        val isAggressive = _uiState.value.isAggressiveLocationEnabled
                        geofenceManager.requestBackgroundLocationUpdates(isAggressive)
                        _uiState.value.userLat?.let { lat ->
                            _uiState.value.userLng?.let { lng ->
                                viewModelScope.launch {
                                    mosqueRepository.fetchAndSaveMosques(lat, lng)
                                }
                            }
                        }
                    } else {
                        geofenceManager.removeBackgroundLocationUpdates()
                    }
                } else if (lastAutoMosque == null && enabled) {
                    // First emit: just start the appropriate location tracking without restarting service
                    val isAggressive = _uiState.value.isAggressiveLocationEnabled
                    if (!isAggressive) {
                        // Only start standard background updates; foreground service is started by
                        // GeofenceForegroundService itself on reboot / user toggle
                        geofenceManager.requestBackgroundLocationUpdates(false)
                    }
                    // Do NOT startForeground here — it's managed by the aggressive toggle
                }
                lastAutoMosque = enabled
            }
        }
        viewModelScope.launch {
            var lastAggressive: Boolean? = null
            userPrefs.isAggressiveLocationEnabled.collect { enabled ->
                _uiState.update { it.copy(isAggressiveLocationEnabled = enabled) }
                // Only restart location tracking when aggressive setting actually changes
                if (lastAggressive != null && lastAggressive != enabled) {
                    if (_uiState.value.isAutoMosqueSilenceEnabled) {
                        geofenceManager.requestBackgroundLocationUpdates(enabled)
                    }
                }
                lastAggressive = enabled
            }
        }
        refreshPermissions()

        // Observe active geofence count live from DB
        viewModelScope.launch {
            silentZoneDao.getActiveGeofenceCount().collect { count ->
                _uiState.update { it.copy(activeGeofenceCount = count) }
            }
        }
    }

    // Removed fetchNearbyMosques as it is now handled by MosqueRepository.fetchAndSaveMosques

    fun refreshMosques() {
        viewModelScope.launch {
            val lat = _uiState.value.userLat ?: return@launch
            val lng = _uiState.value.userLng ?: return@launch

            _uiState.update { it.copy(isLoading = true) }
            
            // Fix: No longer deleting all auto mosques. Just force a fresh fetch.
            mosqueRepository.fetchAndSaveMosques(lat, lng, force = true)
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun toggleMosquePinned(zone: SilentZone) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = mosqueRepository.toggleUserPin(zone, _uiState.value.maxGeofenceSlots)
            if (!success) {
                val max = _uiState.value.maxGeofenceSlots
                _uiState.update {
                    it.copy(errorMessage = "geofence_cap:$max")
                }
            }
        }
    }

    private fun checkAndTriggerActiveZone() {
        val lat = _uiState.value.userLat ?: return
        val lng = _uiState.value.userLng ?: return
        val zones = _uiState.value.zones
        
        val activeZone = zones.find { zone ->
            if (!zone.isEnabled) return@find false
            val results = FloatArray(1)
            Location.distanceBetween(lat, lng, zone.latitude, zone.longitude, results)
            results[0] <= zone.radius
        }

        activeZone?.let { triggerZoneManually(it) }
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
            _uiState.update { it.copy(isLocationFresh = false, isLoading = true) }
            // Immediate fix: get last known location first
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { loc ->
                    _uiState.update { it.copy(userLat = loc.latitude, userLng = loc.longitude, isLocationFresh = true, isLoading = false) }
                }
            }

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).apply {
                setMinUpdateIntervalMillis(1000L)
            }.build()

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            // Safety timeout so the UI doesn't spin forever if GPS is unavailable
            viewModelScope.launch {
                delay(6000)
                _uiState.update { state ->
                    if (state.isLocationFresh) state else state.copy(isLoading = false)
                }
            }
        } else {
            _uiState.update { it.copy(isLocationFresh = false, isLoading = false) }
        }
    }

    /**
     * Attempts a single high-accuracy location fix for quick recentering.
     * Falls back to last known location and reports failure via [onFailure] when unavailable.
     */
    @SuppressLint("MissingPermission")
    fun requestSingleLocationFix(onFailure: (() -> Unit)? = null) {
        if (ContextCompat.checkSelfPermission(application, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(application, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            onFailure?.invoke()
            return
        }

        singleFixJob?.cancel()
        singleFixJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isLocationFresh = false) }
            val freshLocation = withTimeoutOrNull(5000L) {
                try {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
                } catch (e: Exception) {
                    null
                }
            }

            if (freshLocation != null) {
                _uiState.update { it.copy(
                    userLat = freshLocation.latitude,
                    userLng = freshLocation.longitude,
                    isLocationFresh = true,
                    isLoading = false
                ) }
                userPrefs.setLocation(freshLocation.latitude, freshLocation.longitude)
            } else {
                val lastKnown = try { fusedLocationClient.lastLocation.await() } catch (_: Exception) { null }
                if (lastKnown != null) {
                    _uiState.update { it.copy(
                        userLat = lastKnown.latitude,
                        userLng = lastKnown.longitude,
                        isLocationFresh = false,
                        isLoading = false
                    ) }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    onFailure?.invoke()
                }
            }
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _uiState.update { it.copy(isLoading = false) }
    }

    fun setLocationSilenceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setLocationSilenceEnabled(enabled)
        }
    }

    fun setAutoMosqueSilenceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setAutoMosqueSilenceEnabled(enabled)
        }
    }

    fun setAggressiveLocationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setAggressiveLocationEnabled(enabled)
        }
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
            val isEnabled = userPrefs.isLocationSilenceEnabled.first()
            userPrefs.setActiveSilentZoneId(zone.id)
            
            if (!isEnabled) {
                Toast.makeText(application, R.string.status_location_silence_disabled, Toast.LENGTH_SHORT).show()
                return@launch
            }
            
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
        viewModelScope.launch {
            try {
                val results = ApiClient.nominatimApi.search(query, limit = 1)
                val first = results.firstOrNull()
                if (first != null) {
                    onResult(first.lat.toDouble(), first.lon.toDouble())
                } else {
                    // Fallback to Geocoder
                    searchLocationWithGeocoder(query, onResult, onError)
                }
            } catch (e: Exception) {
                // Fallback to Geocoder
                searchLocationWithGeocoder(query, onResult, onError)
            }
        }
    }

    private fun searchLocationWithGeocoder(
        query: String,
        onResult: (Double, Double) -> Unit,
        onError: (String) -> Unit
    ) {
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
        viewModelScope.launch {
            try {
                val results = ApiClient.nominatimApi.search(query, limit = 5)
                if (results.isNotEmpty()) {
                    val suggestions = results.map { res ->
                        val title = res.displayName.split(",").firstOrNull() ?: res.displayName
                        val subtitle = res.displayName.substringAfter(",").trim().ifEmpty { null }
                        MapSuggestion(
                            title = title,
                            subtitle = subtitle,
                            latitude = res.lat.toDouble(),
                            longitude = res.lon.toDouble()
                        )
                    }
                    onResult(suggestions)
                } else {
                    searchLocationSuggestionsWithGeocoder(query, onResult, onError)
                }
            } catch (e: Exception) {
                searchLocationSuggestionsWithGeocoder(query, onResult, onError)
            }
        }
    }

    private fun searchLocationSuggestionsWithGeocoder(
        query: String,
        onResult: (List<MapSuggestion>) -> Unit,
        onError: (String) -> Unit
    ) {
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
