package dhanfinix.android.sukun.core.network

import dhanfinix.android.sukun.feature.mosque.data.model.OverpassResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassService {
    @GET("api/interpreter")
    suspend fun query(@Query("data") data: String): OverpassResponse
}
