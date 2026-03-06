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
import dhanfinix.android.sukun.R
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
import dhanfinix.android.sukun.core.utils.localizeDigits

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
            is PrayerEvent.DurationSelected -> setDuration(event.prayer, event.minutes)
            is PrayerEvent.OffsetSelected -> setOffset(event.prayer, event.offsetMinutes)
            is PrayerEvent.UniformDurationToggled -> setUniformDuration(event.isUniform)
            is PrayerEvent.AllDurationsSelected -> setAllDurations(event.minutes)
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
                _uiState.update { state -> state.copy(errorMessage = null) }
            }
            is PrayerEvent.SnackbarMessageConsumed -> {
                _uiState.update { state -> state.copy(snackbarMessage = null) }
            }
        }
    }

    private fun loadPrayerTimes(minDelay: Long = 0L) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoading = true, errorMessage = null) }
            val startTime = System.currentTimeMillis()

            try {
                // Offload DataStore reads to IO
                val latVal = withContext(Dispatchers.IO) { userPrefs.latitude.first() }
                val lngVal = withContext(Dispatchers.IO) { userPrefs.longitude.first() }
                val locNameVal = withContext(Dispatchers.IO) { userPrefs.locationName.first() }
                val methodVal = withContext(Dispatchers.IO) { userPrefs.calculationMethod.first() }
                val durationsMapVal = withContext(Dispatchers.IO) { userPrefs.prayerDurations.first() }
                val offsetsMapVal = withContext(Dispatchers.IO) { userPrefs.prayerOffsets.first() }
                val isUniformVal = withContext(Dispatchers.IO) { userPrefs.isDurationUniform.first() }
                val enabledMapVal = withContext(Dispatchers.IO) { userPrefs.isPrayerEnabled.first() }

                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)
                
                val deferredToday = async { prayerRepo.getPrayerTimes(today, latVal, lngVal, methodVal, offsetsMapVal) }
                val deferredTomorrow = async { prayerRepo.getPrayerTimes(tomorrow, latVal, lngVal, methodVal, offsetsMapVal) }

                val resultToday = deferredToday.await()
                val resultTomorrow = deferredTomorrow.await()

                if (resultToday.isSuccess && resultTomorrow.isSuccess) {
                    val timesMapToday = resultToday.getOrThrow()
                    val timesMapTomorrow = resultTomorrow.getOrThrow()
                    
                    val isTodayFriday = today.dayOfWeek == java.time.DayOfWeek.FRIDAY
                    val isTomorrowFriday = tomorrow.dayOfWeek == java.time.DayOfWeek.FRIDAY
                    
                    val prayersToday = PrayerName.entries.filter { 
                        if (isTodayFriday) it != PrayerName.DHUHR else it != PrayerName.JUMUAH 
                    }.map { name ->
                        PrayerInfo(
                            name = name,
                            time = timesMapToday[name] ?: "--:--",
                            isEnabled = enabledMapVal[name] ?: true
                        )
                    }
                    
                    val allPrayersToday = PrayerName.entries.map { name ->
                        PrayerInfo(
                            name = name,
                            time = timesMapToday[name] ?: "--:--",
                            isEnabled = enabledMapVal[name] ?: true
                        )
                    }
                    
                    val prayersTomorrow = PrayerName.entries.filter { 
                        if (isTomorrowFriday) it != PrayerName.DHUHR else it != PrayerName.JUMUAH 
                    }.map { name ->
                        PrayerInfo(
                            name = name,
                            time = timesMapTomorrow[name] ?: "--:--",
                            isEnabled = enabledMapVal[name] ?: true
                        )
                    }
                    
                    val finalLocName = locNameVal ?: if (latVal == -6.2088 && lngVal == 106.8456) {
                        getApplication<Application>().getString(R.string.jakarta)
                    } else {
                        withContext(Dispatchers.IO) { reverseGeocode(latVal, lngVal) } ?: getApplication<Application>().getString(R.string.unknown_location)
                    }
                    
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < minDelay) delay(minDelay - elapsed)

                    _uiState.update { state ->
                        state.copy(
                            prayers = prayersToday,
                            allPrayers = allPrayersToday,
                            prayerDurations = durationsMapVal,
                            prayerOffsets = offsetsMapVal,
                            isDurationUniform = isUniformVal,
                            latitude = latVal.toString(),
                            longitude = lngVal.toString(),
                            locationName = finalLocName,
                            method = methodVal,
                            isLoading = false,
                            isDetectingLocation = false
                        )
                    }
                    
                    scheduleWorkers(prayersToday, prayersTomorrow, durationsMapVal)
                } else {
                    val error = resultToday.exceptionOrNull() ?: resultTomorrow.exceptionOrNull()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = error?.localizedMessage ?: getApplication<Application>().getString(R.string.err_load_prayer_times)
                        )
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = getApplication<Application>().getString(R.string.err_unexpected, e.localizedMessage ?: "")
                        )
                    }
                }
            } finally {
                if (loadJob?.isActive == true) {
                    _uiState.update { state -> state.copy(isLoading = false) }
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
            
            val resId = if (!currentEnabled) R.string.silence_enabled_format else R.string.silence_disabled_format
            val prayerNameStr = getApplication<Application>().getString(prayer.nameRes)
            
            _uiState.update { state -> state.copy(
                prayers = updatedPrayers,
                snackbarMessage = getApplication<Application>().getString(resId, prayerNameStr)
            ) }
            
            // Re-fetch tomorrow for scheduling
            val lat = userPrefs.latitude.first()
            val lng = userPrefs.longitude.first()
            val method = userPrefs.calculationMethod.first()
            val tomorrow = LocalDate.now().plusDays(1)
            val timesMapTomorrow = prayerRepo.getPrayerTimes(tomorrow, lat, lng, method, currentState.prayerOffsets).getOrNull() ?: emptyMap()
            
            val isTomorrowFriday = tomorrow.dayOfWeek == java.time.DayOfWeek.FRIDAY
            val prayersTomorrow = PrayerName.entries.filter { 
                if (isTomorrowFriday) it != PrayerName.DHUHR else it != PrayerName.JUMUAH 
            }.map { name ->
                PrayerInfo(
                    name = name,
                    time = timesMapTomorrow[name] ?: "--:--",
                    isEnabled = if (name == prayer) !currentEnabled else (currentState.prayers.find { it.name == name }?.isEnabled ?: true)
                )
            }
            
            scheduleWorkers(updatedPrayers, prayersTomorrow, currentState.prayerDurations)
        }
    }

    private fun setDuration(prayer: PrayerName, minutes: Int) {
        viewModelScope.launch {
            userPrefs.setPrayerDuration(prayer, minutes)
            val updatedDurations = _uiState.value.prayerDurations + (prayer to minutes)
            _uiState.update { state -> state.copy(prayerDurations = updatedDurations) }
            
            val lat = userPrefs.latitude.first()
            val lng = userPrefs.longitude.first()
            val method = userPrefs.calculationMethod.first()
            val tomorrow = LocalDate.now().plusDays(1)
            val timesMapTomorrow = prayerRepo.getPrayerTimes(tomorrow, lat, lng, method, _uiState.value.prayerOffsets).getOrNull() ?: emptyMap()
            
            val isTomorrowFriday = tomorrow.dayOfWeek == java.time.DayOfWeek.FRIDAY
            val prayersTomorrow = PrayerName.entries.filter { 
                if (isTomorrowFriday) it != PrayerName.DHUHR else it != PrayerName.JUMUAH 
            }.map { name ->
                PrayerInfo(
                    name = name,
                    time = timesMapTomorrow[name] ?: "--:--",
                    isEnabled = _uiState.value.prayers.find { it.name == name }?.isEnabled ?: true
                )
            }
            
            scheduleWorkers(_uiState.value.prayers, prayersTomorrow, updatedDurations)
        }
    }

    private fun setOffset(prayer: PrayerName, offset: Int) {
        viewModelScope.launch {
            userPrefs.setPrayerOffset(prayer, offset)
            val updatedOffsets = _uiState.value.prayerOffsets + (prayer to offset)
            
            val lat = userPrefs.latitude.first()
            val lng = userPrefs.longitude.first()
            val method = userPrefs.calculationMethod.first()
            
            val today = LocalDate.now()
            val timesMapToday = prayerRepo.getPrayerTimes(today, lat, lng, method, updatedOffsets).getOrNull() ?: emptyMap()
            val isTodayFriday = today.dayOfWeek == java.time.DayOfWeek.FRIDAY
            
            val updatedPrayersToday = PrayerName.entries.filter { 
                if (isTodayFriday) it != PrayerName.DHUHR else it != PrayerName.JUMUAH 
            }.map { name ->
                PrayerInfo(
                    name = name,
                    time = timesMapToday[name] ?: "--:--",
                    isEnabled = _uiState.value.prayers.find { it.name == name }?.isEnabled ?: true
                )
            }
            
            val updatedAllPrayersToday = PrayerName.entries.map { name ->
                PrayerInfo(
                    name = name,
                    time = timesMapToday[name] ?: "--:--",
                    isEnabled = _uiState.value.prayers.find { it.name == name }?.isEnabled ?: true
                )
            }
            
            _uiState.update { it.copy(prayerOffsets = updatedOffsets, prayers = updatedPrayersToday, allPrayers = updatedAllPrayersToday) }
            
            val tomorrow = today.plusDays(1)
            val timesMapTomorrow = prayerRepo.getPrayerTimes(tomorrow, lat, lng, method, updatedOffsets).getOrNull() ?: emptyMap()
            
            val isTomorrowFriday = tomorrow.dayOfWeek == java.time.DayOfWeek.FRIDAY
            val prayersTomorrow = PrayerName.entries.filter { 
                if (isTomorrowFriday) it != PrayerName.DHUHR else it != PrayerName.JUMUAH 
            }.map { name ->
                PrayerInfo(
                    name = name,
                    time = timesMapTomorrow[name] ?: "--:--",
                    isEnabled = _uiState.value.prayers.find { it.name == name }?.isEnabled ?: true
                )
            }
            
            scheduleWorkers(updatedPrayersToday, prayersTomorrow, _uiState.value.prayerDurations)
        }
    }

    private fun setUniformDuration(isUniform: Boolean) {
        viewModelScope.launch {
            userPrefs.setDurationUniform(isUniform)
            _uiState.update { state -> state.copy(isDurationUniform = isUniform) }
            // If toggled to true, maybe we should sync them all to the first one?
            // The UI will just use the Fajr value as the main slider.
            // But we don't necessarily overwrite the DB until they move the slider.
            // Oh wait, Súkun's previous design was a single duration. If the user turns ON uniform,
            // we probably should overwrite everything to match immediately, or we can just wait for slider changes.
            // A safer bet is just setting the state, and let `setAllDurations` do the datastore updates.
        }
    }

    private fun setAllDurations(minutes: Int) {
        viewModelScope.launch {
            userPrefs.setAllPrayerDurations(minutes)
            
            val updatedMap = PrayerName.entries.associateWith { minutes }
            _uiState.update { state -> state.copy(prayerDurations = updatedMap) }
            
            val lat = userPrefs.latitude.first()
            val lng = userPrefs.longitude.first()
            val method = userPrefs.calculationMethod.first()
            val tomorrow = LocalDate.now().plusDays(1)
            val timesMapTomorrow = prayerRepo.getPrayerTimes(tomorrow, lat, lng, method).getOrNull() ?: emptyMap()
            
            val isTomorrowFriday = tomorrow.dayOfWeek == java.time.DayOfWeek.FRIDAY
            val prayersTomorrow = PrayerName.entries.filter { 
                if (isTomorrowFriday) it != PrayerName.DHUHR else it != PrayerName.JUMUAH 
            }.map { name ->
                PrayerInfo(
                    name = name,
                    time = timesMapTomorrow[name] ?: "--:--",
                    isEnabled = _uiState.value.prayers.find { it.name == name }?.isEnabled ?: true
                )
            }
            
            scheduleWorkers(_uiState.value.prayers, prayersTomorrow, updatedMap)
        }
    }

    private suspend fun updateLocationSync(latStr: String, lngStr: String) {
        val lat = latStr.toDoubleOrNull() ?: return
        val lng = lngStr.toDoubleOrNull() ?: return
        
        val name = withContext(Dispatchers.IO) { reverseGeocode(lat, lng) }
        userPrefs.setLocation(lat, lng, name ?: "Unknown Location")
        _uiState.update { state -> state.copy(isDetectingLocation = false, errorMessage = null) }
    }

    @RequiresPermission(
        anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"]
    )
    private suspend fun detectLocationSync() {
        val nm = getApplication<Application>().checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (nm != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            _uiState.update { state -> state.copy(isDetectingLocation = false, errorMessage = getApplication<Application>().getString(R.string.err_permission_denied)) }
            return
        }

        _uiState.update { state -> state.copy(isDetectingLocation = true, errorMessage = null) }

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
                _uiState.update { state -> state.copy(isDetectingLocation = false, errorMessage = getApplication<Application>().getString(R.string.err_location_not_found_gps)) }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            _uiState.update { state -> state.copy(isDetectingLocation = false, errorMessage = getApplication<Application>().getString(R.string.err_location_timeout)) }
        } catch (e: SecurityException) {
            _uiState.update { state -> state.copy(isDetectingLocation = false, errorMessage = getApplication<Application>().getString(R.string.err_permission_denied)) }
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException) {
                _uiState.update { state -> state.copy(isDetectingLocation = false, errorMessage = getApplication<Application>().getString(R.string.err_search_location)) }
            }
        }
    }

    private fun searchLocation(query: String) {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isDetectingLocation = true, errorMessage = null) }
            try {
                val addresses = withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(getApplication())
                    geocoder.getFromLocationName(query, 1)
                }
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val locationName = address.locality ?: address.subAdminArea ?: address.adminArea ?: query
                    userPrefs.setLocation(address.latitude, address.longitude, locationName)
                    _uiState.update { state -> state.copy(isDetectingLocation = false) }
                    loadPrayerTimes()
                } else {
                    _uiState.update { state -> state.copy(isDetectingLocation = false, isLoading = false, errorMessage = getApplication<Application>().getString(R.string.err_location_not_found)) }
                }
            } catch (e: Exception) {
                _uiState.update { state -> state.copy(isDetectingLocation = false, isLoading = false, errorMessage = getApplication<Application>().getString(R.string.err_search_location)) }
            }
        }
    }

    private fun fetchSuggestions(query: String) {
        searchJob?.cancel()
        if (query.length < 3) {
            _uiState.update { state -> state.copy(locationSuggestions = emptyList(), isSearchingSuggestions = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)
            _uiState.update { state -> state.copy(isSearchingSuggestions = true) }
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
            _uiState.update { state -> state.copy(locationSuggestions = emptyList()) }
            loadPrayerTimes()
        }
    }

    private fun clearSuggestions() {
        searchJob?.cancel()
        _uiState.update { state -> state.copy(locationSuggestions = emptyList(), isSearchingSuggestions = false) }
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

    private fun scheduleWorkers(prayersToday: List<PrayerInfo>, prayersTomorrow: List<PrayerInfo>, durations: Map<PrayerName, Int>) {
        silenceScheduler.scheduleAll(prayersToday, prayersTomorrow, durations, _uiState.value.prayerOffsets)
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

                // Compute Hijri Date with Day of Week
                val uLocale = android.icu.util.ULocale.forLocale(java.util.Locale.getDefault()).setKeywordValue("calendar", "islamic")
                val islamicCalendar = android.icu.util.IslamicCalendar(uLocale)
                val hijriFormatter = android.icu.text.SimpleDateFormat("EEEE, d MMMM yyyy", uLocale)
                val hijriDateStr = hijriFormatter.format(islamicCalendar.time)

                _uiState.update { state ->
                    state.copy(
                        currentTime = currentTimeStr.localizeDigits(),
                        currentDate = hijriDateStr.localizeDigits(),
                        nextPrayer = nextPrayerInfo?.first?.name,
                        nextPrayerCountdown = countdownStr.localizeDigits()
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
