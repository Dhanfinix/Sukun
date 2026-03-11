package dhanfinix.android.sukun.domain.usecase

import dhanfinix.android.sukun.core.database.entity.MosqueLocation
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.core.location.GeofenceManager
import dhanfinix.android.sukun.domain.repository.MosqueLocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class StartLocationSilenceUseCase(
    private val repository: MosqueLocationRepository,
    private val geofenceManager: GeofenceManager,
    private val userPreferences: UserPreferences
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            val locations = repository.getAllLocations().first()
            val radius = userPreferences.locationSilenceRadius.first().toFloat()
            geofenceManager.addGeofences(locations, radius)
            userPreferences.setLocationSilenceEnabled(true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class StopLocationSilenceUseCase(
    private val geofenceManager: GeofenceManager,
    private val userPreferences: UserPreferences
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            geofenceManager.removeGeofences()
            userPreferences.setLocationSilenceEnabled(false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class AddCustomMosqueUseCase(
    private val repository: MosqueLocationRepository
) {
    suspend operator fun invoke(location: MosqueLocation): Result<Long> {
        return try {
            if (location.type != "CUSTOM") {
                return Result.failure(IllegalArgumentException("Location must be of type CUSTOM"))
            }
            val id = repository.addLocation(location)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class FetchGlobalMosquesUseCase(
    private val repository: MosqueLocationRepository,
    private val userPreferences: UserPreferences
) {
    suspend operator fun invoke(latitude: Double, longitude: Double, radiusKm: Double): Result<List<MosqueLocation>> {
        return try {
            // Clear old global mosques first
            repository.clearGlobalMosques()
            val mosques = repository.fetchGlobalMosques(latitude, longitude, radiusKm)
            userPreferences.setGlobalMosquesLoaded(true)
            Result.success(mosques)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetMosqueLocationsUseCase(
    private val repository: MosqueLocationRepository
) {
    operator fun invoke(): Flow<List<MosqueLocation>> {
        return repository.getAllLocations()
    }
}

class RemoveMosqueLocationUseCase(
    private val repository: MosqueLocationRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return try {
            repository.removeLocation(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
