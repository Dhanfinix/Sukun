package dhanfinix.android.sukun.navigation

import kotlinx.serialization.Serializable

@Serializable
data class WebPageRoute(val pageUrl: String, val pageTitle: String) : Route
