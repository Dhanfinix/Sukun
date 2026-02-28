package dhanfinix.android.sukun.feature.prayer.ui

import android.app.Application
import android.location.Geocoder
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dhanfinix.android.sukun.feature.prayer.data.model.LocationSuggestion
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerInfo
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName
import dhanfinix.android.sukun.feature.prayer.data.PrayerRepository
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.worker.SilenceScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * ViewModel for Prayer Settings.
 * Now uses a persistent Monthly Calendar repository.
 */
class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prayerRepo = PrayerRepository(application)
    private val userPrefs = UserPreferences(application)
    private val silenceScheduler = SilenceScheduler(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    init {
        loadPrayerTimes()
        startClockTicker()
    }

    fun onEvent(event: PrayerEvent) {
        when (event) {
            is PrayerEvent.TogglePrayer -> togglePrayer(event.prayer)
            is PrayerEvent.DurationSelected -> setDuration(event.minutes)
            is PrayerEvent.LocationUpdated -> updateLocation(event.latitude, event.longitude)
            is PrayerEvent.MethodChanged -> changeMethod(event.methodId)
            is PrayerEvent.RefreshTimes -> loadPrayerTimes()
            is PrayerEvent.DetectLocation -> detectLocation()
            is PrayerEvent.SearchLocation -> searchLocation(event.query)
            is PrayerEvent.SearchQueryChanged -> fetchSuggestions(event.query)
            is PrayerEvent.SuggestionSelected -> selectSuggestion(event.suggestion)
            is PrayerEvent.ClearSuggestions -> clearSuggestions()
            is PrayerEvent.ErrorMessageConsumed -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun loadPrayerTimes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val lat = userPrefs.latitude.first()
            val lng = userPrefs.longitude.first()
            val locName = userPrefs.locationName.first()
            val method = userPrefs.calculationMethod.first()
            val duration = userPrefs.silenceDurationMin.first()
            val enabledMap = userPrefs.isPrayerEnabled.first()

            val result = prayerRepo.getPrayerTimes(LocalDate.now(), lat, lng, method)

            result.fold(
                onSuccess = { timesMap ->
                    val prayers = PrayerName.entries.map { name ->
                        PrayerInfo(
                            name = name,
                            time = timesMap[name] ?: "--:--",
                            isEnabled = enabledMap[name] ?: true
                        )
                    }
                    val finalLocName = locName ?: reverseGeocode(lat, lng) ?: "Jakarta"
                    
                    _uiState.update { 
                        it.copy(
                            prayers = prayers,
                            silenceDurationMin = duration,
                            latitude = lat.toString(),
                            longitude = lng.toString(),
                            locationName = finalLocName,
                            method = method,
                            isLoading = false
                        )
                    }
                    scheduleWorkers(prayers, duration)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load prayer times"
                        )
                    }
                }
            )
        }
    }

    private fun togglePrayer(prayer: PrayerName) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val currentEnabled = currentState.prayers
                .firstOrNull { it.name == prayer }?.isEnabled ?: return@launch
            
            userPrefs.setPrayerEnabled(prayer, !currentEnabled)

            val updatedPrayers = currentState.prayers.map {
                if (it.name == prayer) it.copy(isEnabled = !currentEnabled) else it
            }
            _uiState.update { it.copy(prayers = updatedPrayers) }
            scheduleWorkers(updatedPrayers, currentState.silenceDurationMin)
        }
    }

    private fun setDuration(minutes: Int) {
        viewModelScope.launch {
            userPrefs.setSilenceDuration(minutes)
            _uiState.update { it.copy(silenceDurationMin = minutes) }
            scheduleWorkers(_uiState.value.prayers, minutes)
        }
    }

    private fun updateLocation(latStr: String, lngStr: String) {
        viewModelScope.launch {
            val lat = latStr.toDoubleOrNull() ?: return@launch
            val lng = lngStr.toDoubleOrNull() ?: return@launch
            
            val name = reverseGeocode(lat, lng)
            userPrefs.setLocation(lat, lng, name ?: "Unknown Location")
            
            // Set flag only for the immediate detect -> load flow
            _uiState.update { it.copy(isDetectingLocation = false, errorMessage = null) }
            loadPrayerTimes() 
        }
    }

    @RequiresPermission(
        anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"]
    )
    private fun detectLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetectingLocation = true, errorMessage = null) }

            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            updateLocation(location.latitude.toString(), location.longitude.toString())
                        } else {
                            _uiState.update { it.copy(isDetectingLocation = false, isLoading = false, errorMessage = "Location not found") }
                        }
                    }
                    .addOnFailureListener {
                        _uiState.update { it.copy(isDetectingLocation = false, isLoading = false, errorMessage = "Failed to detect location") }
                    }
            } catch (e: SecurityException) {
                 _uiState.update { it.copy(isDetectingLocation = false, isLoading = false, errorMessage = "Location permission denied") }
            }
        }
    }

    private fun searchLocation(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetectingLocation = true, errorMessage = null) }
            try {
                val geocoder = Geocoder(getApplication())
                val addresses = geocoder.getFromLocationName(query, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val locationName = address.locality ?: address.subAdminArea ?: address.adminArea ?: query
                    userPrefs.setLocation(address.latitude, address.longitude, locationName)
                    _uiState.update { it.copy(isDetectingLocation = false) }
                    loadPrayerTimes()
                } else {
                    _uiState.update { it.copy(isDetectingLocation = false, isLoading = false, errorMessage = "Location not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDetectingLocation = false, isLoading = false, errorMessage = "Error searching location") }
            }
        }
    }

    private fun fetchSuggestions(query: String) {
        searchJob?.cancel()
        if (query.length < 3) {
            _uiState.update { it.copy(locationSuggestions = emptyList(), isSearchingSuggestions = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(isSearchingSuggestions = true) }
            try {
                val geocoder = Geocoder(getApplication())
                val addresses = geocoder.getFromLocationName(query, 5)
                val suggestions = addresses?.map { address ->
                    val name = listOfNotNull(
                        address.locality,
                        address.subAdminArea,
                        address.adminArea,
                        address.countryName
                    ).distinct().joinToString(", ")
                    LocationSuggestion(name, address.latitude, address.longitude)
                } ?: emptyList()
                _uiState.update { it.copy(locationSuggestions = suggestions, isSearchingSuggestions = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchingSuggestions = false) }
            }
        }
    }

    private fun selectSuggestion(suggestion: LocationSuggestion) {
        viewModelScope.launch {
            userPrefs.setLocation(suggestion.latitude, suggestion.longitude, suggestion.name)
            _uiState.update { it.copy(locationSuggestions = emptyList()) }
            loadPrayerTimes()
        }
    }

    private fun clearSuggestions() {
        searchJob?.cancel()
        _uiState.update { it.copy(locationSuggestions = emptyList(), isSearchingSuggestions = false) }
    }

    private fun reverseGeocode(lat: Double, lng: Double): String? {
        return try {
            val geocoder = Geocoder(getApplication())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.locality ?: address.subAdminArea ?: address.adminArea ?: address.countryName
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun changeMethod(methodId: Int) {
        viewModelScope.launch {
            userPrefs.setCalculationMethod(methodId)
            loadPrayerTimes() 
        }
    }

    private fun scheduleWorkers(prayers: List<PrayerInfo>, durationMin: Int) {
        silenceScheduler.scheduleAll(prayers, durationMin)
    }

    private fun startClockTicker() {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        viewModelScope.launch {
            var lastDate = LocalDate.now()
            while (true) {
                val now = LocalTime.now()
                val today = LocalDate.now()
                
                // Detection of Day Rollover
                if (today.isAfter(lastDate)) {
                    lastDate = today
                    loadPrayerTimes()
                }

                val currentTimeStr = now.format(timeFormatter)
                
                val nextPrayerInfo = calculateNextPrayer(now, _uiState.value.prayers)
                val countdownStr = nextPrayerInfo?.let { (prayer, remaining) ->
                    val hours = remaining / 3600
                    val minutes = (remaining % 3600) / 60
                    val seconds = remaining % 60
                    String.format("%02d:%02d:%02d", hours, minutes, seconds)
                } ?: "--:--:--"

                _uiState.update { 
                    it.copy(
                        currentTime = currentTimeStr,
                        nextPrayerName = nextPrayerInfo?.first?.name?.displayName,
                        nextPrayerCountdown = countdownStr
                    )
                }
                delay(1000)
            }
        }
    }

    private fun calculateNextPrayer(now: LocalTime, prayers: List<PrayerInfo>): Pair<PrayerInfo, Long>? {
        if (prayers.isEmpty()) return null
        
        val sortedPrayers = prayers
            .mapNotNull { prayer ->
                try {
                    val timeParts = prayer.time.split(":")
                    if (timeParts.size != 2) return@mapNotNull null
                    val prayerTime = LocalTime.of(timeParts[0].toInt(), timeParts[1].toInt())
                    prayer to prayerTime
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.second }

        val next = sortedPrayers.firstOrNull { it.second.isAfter(now) }
        
        return if (next != null) {
            val secondsUntil = now.until(next.second, ChronoUnit.SECONDS)
            next.first to secondsUntil
        } else {
            // Tomorrow's Fajr logic
            // Since we have persistent data now, we COULD theoretically get exactly tomorrow's Fajr from Room.
            // But for the TICKER specifically, staying within today's relative Fajr (usually 1min diff) 
            // is okay until the next day loads.
            
            val fajr = sortedPrayers.firstOrNull { it.first.name == PrayerName.FAJR } ?: return null
            val secondsUntilEndOfDay = now.until(LocalTime.MAX, ChronoUnit.SECONDS) + 1
            val secondsInNextDay = LocalTime.MIN.until(fajr.second, ChronoUnit.SECONDS)
            fajr.first to (secondsUntilEndOfDay + secondsInNextDay)
        }
    }
}
