package dhanfinix.android.sukun.feature.mosque.data.model

import com.google.gson.annotations.SerializedName

data class OverpassResponse(
    val elements: List<OverpassElement>
)

data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double?,
    val lon: Double?,
    val center: OverpassCenter?,
    val tags: Map<String, String>?,
    val nodes: List<Long>?,
    val bounds: OverpassBounds?
)

data class OverpassCenter(
    val lat: Double,
    val lon: Double
)

data class OverpassBounds(
    val minlat: Double,
    val minlon: Double,
    val maxlat: Double,
    val maxlon: Double
)
