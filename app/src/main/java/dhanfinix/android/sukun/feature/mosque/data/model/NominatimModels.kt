package dhanfinix.android.sukun.feature.mosque.data.model

import com.google.gson.annotations.SerializedName

data class NominatimResponse(
    @SerializedName("place_id") val placeId: Long,
    @SerializedName("licence") val licence: String,
    @SerializedName("osm_type") val osmType: String,
    @SerializedName("osm_id") val osmId: Long,
    @SerializedName("boundingbox") val boundingBox: List<String>,
    @SerializedName("lat") val lat: String,
    @SerializedName("lon") val lon: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("class") val clazz: String,
    @SerializedName("type") val type: String,
    @SerializedName("importance") val importance: Double,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("address") val address: NominatimAddress? = null
)

data class NominatimAddress(
    @SerializedName("house_number") val houseNumber: String? = null,
    @SerializedName("road") val road: String? = null,
    @SerializedName("neighbourhood") val neighbourhood: String? = null,
    @SerializedName("suburb") val suburb: String? = null,
    @SerializedName("city_district") val cityDistrict: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("county") val county: String? = null,
    @SerializedName("state_district") val stateDistrict: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("postcode") val postcode: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("country_code") val countryCode: String? = null,
    @SerializedName("village") val village: String? = null,
    @SerializedName("town") val town: String? = null
)
