package dhanfinix.android.sukun.core.network

import dhanfinix.android.sukun.feature.prayer.data.model.AladhanCalendarResponse
import dhanfinix.android.sukun.feature.prayer.data.model.AladhanResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AladhanService {

    @GET("v1/timings/{timestamp}")
    suspend fun getTimings(
        @Path("timestamp") timestamp: Long,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 20
    ): AladhanResponse

    @GET("v1/calendar/{year}/{month}")
    suspend fun getCalendar(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 20
    ): AladhanCalendarResponse
}
