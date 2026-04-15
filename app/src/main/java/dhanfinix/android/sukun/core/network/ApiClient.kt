package dhanfinix.android.sukun.core.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://api.aladhan.com/"
    private const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/"
    private const val OVERPASS_BASE_URL = "https://overpass-api.de/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val userAgentInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestWithUserAgent = original.newBuilder()
            .header("User-Agent", "SukunApp/1.1.5 (Android)") // Required by Overpass API to prevent 403 Forbidden
            .build()
        chain.proceed(requestWithUserAgent)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(userAgentInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: AladhanService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AladhanService::class.java)
    }

    val nominatimApi: NominatimService by lazy {
        Retrofit.Builder()
            .baseUrl(NOMINATIM_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimService::class.java)
    }

    val overpassApi: OverpassService by lazy {
        Retrofit.Builder()
            .baseUrl(OVERPASS_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassService::class.java)
    }
}
