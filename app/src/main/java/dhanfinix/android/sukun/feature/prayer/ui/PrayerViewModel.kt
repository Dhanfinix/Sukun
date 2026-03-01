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
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
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

    private var loadJob: Job? = null

    init {
        // Simple startup flow: 
        // 1. Check if we need to auto-detect (fresh install)
        // 2. If yes, try to detect (with timeout)
        // 3. Load prayer times using whatever coordinates we have (GPS or fallback)
        viewModelScope.launch {
            val latVal = userPrefs.latitude.first()
            val lngVal = userPrefs.longitude.first()
            val locNameVal = userPrefs.locationName.first()

            if (latVal == -6.2088 && lngVal == 106.8456 && locNameVal == null) {
                val nm = getApplication<Application>().checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                if (nm == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // We have permission, try a quick detection before loading
                    detectLocationSync()
                }
            }
            
            // Now load the data (either with newly detected GPS, or fallback Jakarta)
            loadPrayerTimes()
            startClockTicker()
        }
    }

    fun onEvent(event: PrayerEvent) {
        when (event) {
            is PrayerEvent.TogglePrayer -> togglePrayer(event.prayer)
            is PrayerEvent.DurationSelected -> setDuration(event.minutes)
            is PrayerEvent.LocationUpdated -> {
                viewModelScope.launch {
                    updateLocationSync(event.latitude, event.longitude)
                    loadPrayerTimes()
                }
            }
            is PrayerEvent.MethodChanged -> changeMethod(event.methodId)
            is PrayerEvent.RefreshTimes -> {
                viewModelScope.launch {
                    prayerRepo.clearCache()
                    loadPrayerTimes(minDelay = 600L)
                }
            }
            is PrayerEvent.DetectLocation -> {
                viewModelScope.launch {
                    detectLocationSync()
                    loadPrayerTimes()
                }
            }
            is PrayerEvent.SearchLocation -> searchLocation(event.query)
            is PrayerEvent.SearchQueryChanged -> fetchSuggestions(event.query)
            is PrayerEvent.SuggestionSelected -> selectSuggestion(event.suggestion)
            is PrayerEvent.ClearSuggestions -> clearSuggestions()
            is PrayerEvent.ErrorMessageConsumed -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun loadPrayerTimes(minDelay: Long = 0L) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val startTime = System.currentTimeMillis()

            try {
                // Offload DataStore reads to IO
                val latVal = withContext(Dispatchers.IO) { userPrefs.latitude.first() }
                val lngVal = withContext(Dispatchers.IO) { userPrefs.longitude.first() }
                val locNameVal = withContext(Dispatchers.IO) { userPrefs.locationName.first() }
                val methodVal = withContext(Dispatchers.IO) { userPrefs.calculationMethod.first() }
                val durationVal = withContext(Dispatchers.IO) { userPrefs.silenceDurationMin.first() }
                val enabledMapVal = withContext(Dispatchers.IO) { userPrefs.isPrayerEnabled.first() }

                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)
                
                val deferredToday = async { prayerRepo.getPrayerTimes(today, latVal, lngVal, methodVal) }
                val deferredTomorrow = async { prayerRepo.getPrayerTimes(tomorrow, latVal, lngVal, methodVal) }

                val resultToday = deferredToday.await()
                val resultTomorrow = deferredTomorrow.await()

                if (resultToday.isSuccess && resultTomorrow.isSuccess) {
                    val timesMapToday = resultToday.getOrThrow()
                    val timesMapTomorrow = resultTomorrow.getOrThrow()
                    
                    val prayersToday = PrayerName.entries.map { name ->
                        PrayerInfo(
                            name = name,
                            time = timesMapToday[name] ?: "--:--",
                            isEnabled = enabledMapVal[name] ?: true
                        )
                    }
                    
                    val prayersTomorrow = PrayerName.entries.map { name ->
                        PrayerInfo(
                            name = name,
                            time = timesMapTomorrow[name] ?: "--:--",
                            isEnabled = enabledMapVal[name] ?: true
                        )
                    }
                    
                    val finalLocName = locNameVal ?: if (latVal == -6.2088 && lngVal == 106.8456) {
                        "Jakarta"
                    } else {
                        withContext(Dispatchers.IO) { reverseGeocode(latVal, lngVal) } ?: "Unknown Location"
                    }
                    
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < minDelay) delay(minDelay - elapsed)

                    _uiState.update { 
                        it.copy(
                            prayers = prayersToday,
                            silenceDurationMin = durationVal,
                            latitude = latVal.toString(),
                            longitude = lngVal.toString(),
                            locationName = finalLocName,
                            method = methodVal,
                            isLoading = false,
                            isDetectingLocation = false
                        )
                    }
                    
                    scheduleWorkers(prayersToday, prayersTomorrow, durationVal)
                } else {
                    val error = resultToday.exceptionOrNull() ?: resultTomorrow.exceptionOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error?.localizedMessage ?: "Failed to load prayer times"
                        )
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Unexpected error: ${e.localizedMessage}"
                        )
                    }
                }
            } finally {
                if (loadJob?.isActive == true) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
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
            
            // Re-fetch tomorrow for scheduling
            val lat = userPrefs.latitude.first()
            val lng = userPrefs.longitude.first()
            val method = userPrefs.calculationMethod.first()
            val tomorrow = LocalDate.now().plusDays(1)
            val timesMapTomorrow = prayerRepo.getPrayerTimes(tomorrow, lat, lng, method).getOrNull() ?: emptyMap()
            
            val prayersTomorrow = PrayerName.entries.map { name ->
                PrayerInfo(
                    name = name,
                    time = timesMapTomorrow[name] ?: "--:--",
                    isEnabled = if (name == prayer) !currentEnabled else (currentState.prayers.find { it.name == name }?.isEnabled ?: true)
                )
            }
            
            scheduleWorkers(updatedPrayers, prayersTomorrow, currentState.silenceDurationMin)
        }
    }

    private fun setDuration(minutes: Int) {
        viewModelScope.launch {
            userPrefs.setSilenceDuration(minutes)
            _uiState.update { it.copy(silenceDurationMin = minutes) }
            
            val lat = userPrefs.latitude.first()
            val lng = userPrefs.longitude.first()
            val method = userPrefs.calculationMethod.first()
            val tomorrow = LocalDate.now().plusDays(1)
            val timesMapTomorrow = prayerRepo.getPrayerTimes(tomorrow, lat, lng, method).getOrNull() ?: emptyMap()
            
            val prayersTomorrow = PrayerName.entries.map { name ->
                PrayerInfo(
                    name = name,
                    time = timesMapTomorrow[name] ?: "--:--",
                    isEnabled = _uiState.value.prayers.find { it.name == name }?.isEnabled ?: true
                )
            }
            
            scheduleWorkers(_uiState.value.prayers, prayersTomorrow, minutes)
        }
    }

    private suspend fun updateLocationSync(latStr: String, lngStr: String) {
        val lat = latStr.toDoubleOrNull() ?: return
        val lng = lngStr.toDoubleOrNull() ?: return
        
        val name = withContext(Dispatchers.IO) { reverseGeocode(lat, lng) }
        userPrefs.setLocation(lat, lng, name ?: "Unknown Location")
        
        _uiState.update { it.copy(isDetectingLocation = false, errorMessage = null) }
    }

    @RequiresPermission(
        anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"]
    )
    private suspend fun detectLocationSync() {
        val nm = getApplication<Application>().checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (nm != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            _uiState.update { it.copy(isDetectingLocation = false, errorMessage = "Location permission denied") }
            return
        }

        _uiState.update { it.copy(isDetectingLocation = true, errorMessage = null) }

        try {
            // Quick 5-second timeout for first-run or explicit refresh
            var location: Location? = null
            try {
                location = kotlinx.coroutines.withTimeout(5000L) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Ignore timeout here, try fallback
            }

            // Fallback to last known location if current location is null (or timed out)
            if (location == null) {
                location = fusedLocationClient.lastLocation.await()
            }

            if (location != null) {
                updateLocationSync(location.latitude.toString(), location.longitude.toString())
            } else {
                _uiState.update { it.copy(isDetectingLocation = false, errorMessage = "Location not found via GPS") }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            _uiState.update { it.copy(isDetectingLocation = false, errorMessage = "Location detection timed out") }
        } catch (e: SecurityException) {
            _uiState.update { it.copy(isDetectingLocation = false, errorMessage = "Location permission denied") }
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException) {
                _uiState.update { it.copy(isDetectingLocation = false, errorMessage = "Failed to detect location") }
            }
        }
    }

    private fun searchLocation(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetectingLocation = true, errorMessage = null) }
            try {
                val addresses = withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(getApplication())
                    geocoder.getFromLocationName(query, 1)
                }
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
                val addresses = withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(getApplication())
                    geocoder.getFromLocationName(query, 5)
                }
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

    private fun scheduleWorkers(prayersToday: List<PrayerInfo>, prayersTomorrow: List<PrayerInfo>, durationMin: Int) {
        silenceScheduler.scheduleAll(prayersToday, prayersTomorrow, durationMin)
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
                    String.format("-%02d:%02d:%02d", hours, minutes, seconds)
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
