package dhanfinix.android.sukun.feature.prayer.data.model

import com.google.gson.annotations.SerializedName

data class AladhanResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: AladhanData
)

data class AladhanCalendarResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<AladhanData>
)

data class AladhanData(
    @SerializedName("timings") val timings: PrayerTimings,
    @SerializedName("date") val date: AladhanDate,
    @SerializedName("meta") val meta: AladhanMeta
)

data class PrayerTimings(
    @SerializedName("Fajr") val fajr: String,
    @SerializedName("Sunrise") val sunrise: String,
    @SerializedName("Dhuhr") val dhuhr: String,
    @SerializedName("Asr") val asr: String,
    @SerializedName("Sunset") val sunset: String,
    @SerializedName("Maghrib") val maghrib: String,
    @SerializedName("Isha") val isha: String,
    @SerializedName("Imsak") val imsak: String,
    @SerializedName("Midnight") val midnight: String,
    @SerializedName("Firstthird") val firstthird: String,
    @SerializedName("Lastthird") val lastthird: String
)

data class AladhanDate(
    @SerializedName("readable") val readable: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("gregorian") val gregorian: GregorianDate
)

data class GregorianDate(
    @SerializedName("date") val date: String, // DD-MM-YYYY
    @SerializedName("format") val format: String,
    @SerializedName("day") val day: String,
    @SerializedName("month") val month: GregorianMonth,
    @SerializedName("year") val year: String
)

data class GregorianMonth(
    @SerializedName("number") val number: Int,
    @SerializedName("en") val en: String
)

data class AladhanMeta(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("method") val method: MethodInfo
)

data class MethodInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)
