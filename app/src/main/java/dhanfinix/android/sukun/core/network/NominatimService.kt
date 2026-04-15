package dhanfinix.android.sukun.core.network

import dhanfinix.android.sukun.feature.mosque.data.model.NominatimResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NominatimService {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 5,
        @Query("accept-language") language: String = "en",
        @Header("User-Agent") userAgent: String = "Sukun-Android-App"
    ): List<NominatimResponse>
}
