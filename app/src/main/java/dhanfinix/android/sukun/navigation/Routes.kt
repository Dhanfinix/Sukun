package dhanfinix.android.sukun.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Splash : Route

    @Serializable
    data object Landing : Route

    @Serializable
    data object Geofencing : Route

    @Serializable
    data object LocationSilent : Route

    @Serializable
    data object Onboarding : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object About : Route
}

sealed class Screen(val route: String) {
    data object Prayer : Screen("prayer")
    data object Settings : Screen("settings")
    data object MosqueLocations : Screen("mosque_locations")
    data object Geofencing : Screen("geofencing")
    data object LocationSilent : Screen("location_silent")
}
