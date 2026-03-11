package dhanfinix.android.sukun.core.network

import dhanfinix.android.sukun.core.network.model.FoursquareResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface FoursquareService {
    @GET("places/search")
    suspend fun searchPlaces(
        @Query("ll") latLng: String,      // "latitude,longitude"
        @Query("radius") radius: Int,     // in meters
//        @Query("query") query: String = "Masjid Al-I'tisham",     // in meters
        @Query("fsq_category_ids") categories: String = "4bf58dd8d48988d138941735", // 4bf58dd8d48988d138941735 = Mosque BSON, 12098 = Mosque numeric
        @Query("fields") fields: String = "fsq_place_id,name,location,latitude,longitude,categories",
        @Query("sort") sort: String = "DISTANCE",
        @Query("limit") limit: Int = 100
    ): FoursquareResponse
}

object FoursquareApiClient {
    private const val BASE_URL = "https://places-api.foursquare.com/"
    
    // Replace this with a secure BuildConfig key in production
    private const val API_KEY = "YUNBJOON4H4ZAXFADUMHOVBMBFM1AOKXADRC3IYCR1DVHU3Y"

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $API_KEY")
            .header("X-Places-Api-Version", "2025-06-17")
            .header("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val api: FoursquareService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FoursquareService::class.java)
    }
}
