package dhanfinix.android.sukun.core.network.model

import com.google.gson.annotations.SerializedName

data class FoursquareResponse(
    @SerializedName("results") val results: List<FoursquarePlace>
)

data class FoursquarePlace(
    @SerializedName("fsq_place_id") val fsqPlaceId: String,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("location") val location: FoursquareLocation?,
    @SerializedName("categories") val categories: List<FoursquareCategory>?
)

data class FoursquareCategory(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String
)

data class FoursquareLocation(
    @SerializedName("address") val address: String?,
    @SerializedName("cross_street") val crossStreet: String?,
    @SerializedName("formatted_address") val formattedAddress: String?,
    @SerializedName("locality") val locality: String?,
    @SerializedName("region") val region: String?,
    @SerializedName("country") val country: String?
)
