package dhanfinix.android.sukun.feature.mosque

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dhanfinix.android.sukun.core.database.SukunDatabase
import dhanfinix.android.sukun.core.database.entity.MosqueLocation
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.data.repository.MosqueLocationRepositoryImpl
import dhanfinix.android.sukun.domain.usecase.AddCustomMosqueUseCase
import dhanfinix.android.sukun.domain.usecase.FetchGlobalMosquesUseCase
import dhanfinix.android.sukun.domain.usecase.GetMosqueLocationsUseCase
import dhanfinix.android.sukun.domain.usecase.RemoveMosqueLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MosqueLocationsUiState(
    val mosques: List<MosqueLocation> = emptyList(),
    val userLat: Double? = null,
    val userLng: Double? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class MosqueLocationsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = SukunDatabase.getDatabase(application).mosqueLocationDao()
    private val repository = MosqueLocationRepositoryImpl(dao)
    private val userPrefs = UserPreferences(application)

    private val getMosquesUseCase = GetMosqueLocationsUseCase(repository)
    private val fetchGlobalUseCase = FetchGlobalMosquesUseCase(repository, userPrefs)
    private val addCustomUseCase = AddCustomMosqueUseCase(repository)
    private val removeUseCase = RemoveMosqueLocationUseCase(repository)

    private val _uiState = MutableStateFlow(MosqueLocationsUiState())
    val uiState: StateFlow<MosqueLocationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMosquesUseCase().collect { mosques ->
                _uiState.update { it.copy(mosques = mosques) }
            }
        }
        viewModelScope.launch {
            val lat = userPrefs.latitude.first()
            val lng = userPrefs.longitude.first()
            _uiState.update { it.copy(userLat = lat, userLng = lng) }
            
            // Auto-load mosques near the cached location on startup
            fetchGlobalMosques(lat, lng)
        }
    }

    fun fetchGlobalMosques(latitude: Double? = null, longitude: Double? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val targetLat = latitude ?: userPrefs.latitude.first()
            val targetLng = longitude ?: userPrefs.longitude.first()
            
            val result = fetchGlobalUseCase(targetLat, targetLng, radiusKm = 1.0)
            _uiState.update { state ->
                if (result.isSuccess) {
                    val count = result.getOrNull()?.size ?: 0
                    state.copy(isLoading = false, successMessage = "Found $count mosque(s) nearby")
                } else {
                    state.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to fetch mosques"
                    )
                }
            }
        }
    }

    fun addCustomMosque(name: String, latitude: Double, longitude: Double, address: String) {
        viewModelScope.launch {
            val location = MosqueLocation(
                name = name.trim(),
                latitude = latitude,
                longitude = longitude,
                type = "CUSTOM",
                address = address.trim().ifBlank { null }
            )
            val result = addCustomUseCase(location)
            _uiState.update { state ->
                if (result.isSuccess) {
                    state.copy(successMessage = "Mosque \"${location.name}\" added")
                } else {
                    state.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to add mosque")
                }
            }
        }
    }

    fun deleteLocation(id: Long) {
        viewModelScope.launch {
            val result = removeUseCase(id)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = "Failed to delete mosque") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
