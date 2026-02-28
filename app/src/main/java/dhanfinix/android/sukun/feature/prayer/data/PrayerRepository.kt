package dhanfinix.android.sukun.feature.prayer.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dhanfinix.android.sukun.core.database.SukunDatabase
import dhanfinix.android.sukun.core.database.entity.PrayerDay
import dhanfinix.android.sukun.core.network.ApiClient
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repository that manages prayer times with Room caching and monthly fetching.
 *
 * Flow:
 *  1. Check Room for today's data
 *  2. If missing → fetch the full month from Aladhan Calendar API
 *  3. Store the full month in Room
 *  4. Return today's times
 *
 * Cache is invalidated when:
 *  - Location changes (manual or GPS)
 *  - User pulls to refresh
 *  - Month rolls over (midnight tick)
 */
class PrayerRepository(context: Context) {

    private val api = ApiClient.api
    private val prayerDao = SukunDatabase.getDatabase(context).prayerDao()
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Get prayer times for a specific date.
     * Fetches a full month if data isn't cached yet.
     */
    suspend fun getPrayerTimes(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        method: Int = 20
    ): Result<Map<PrayerName, String>> {
        val dateStr = date.format(dateFormatter)
        val id = buildId(dateStr, latitude, longitude, method)

        // 1. Try Room cache
        val cached = prayerDao.getPrayerDayById(id)
        if (cached != null) {
            return Result.success(deserializeTimings(cached.timingsJson))
        }

        // 2. Fetch full month from API
        return try {
            val response = api.getCalendar(date.year, date.monthValue, latitude, longitude, method)
            if (response.code == 200) {
                val prayerDays = response.data.map { dayData ->
                    // API date format: "DD-MM-YYYY" → convert to "YYYY-MM-DD"
                    val apiDate = dayData.date.gregorian.date
                    val parts = apiDate.split("-")
                    val formattedDate = "${parts[2]}-${parts[1]}-${parts[0]}"

                    val timingsMap = mapOf(
                        PrayerName.FAJR to cleanTime(dayData.timings.fajr),
                        PrayerName.DHUHR to cleanTime(dayData.timings.dhuhr),
                        PrayerName.ASR to cleanTime(dayData.timings.asr),
                        PrayerName.MAGHRIB to cleanTime(dayData.timings.maghrib),
                        PrayerName.ISHA to cleanTime(dayData.timings.isha)
                    )

                    PrayerDay(
                        id = buildId(formattedDate, latitude, longitude, method),
                        date = formattedDate,
                        latitude = latitude,
                        longitude = longitude,
                        methodId = method,
                        timingsJson = serializeTimings(timingsMap)
                    )
                }

                // 3. Store in Room
                prayerDao.insertAll(prayerDays)

                // 4. Return today's data
                val newCached = prayerDao.getPrayerDayById(id)
                if (newCached != null) {
                    Result.success(deserializeTimings(newCached.timingsJson))
                } else {
                    Result.failure(Exception("Date $dateStr not found in API response"))
                }
            } else {
                Result.failure(Exception("API error: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Force re-fetch from network by clearing Room cache.
     */
    suspend fun clearCache() {
        prayerDao.clearAll()
    }

    private fun buildId(date: String, lat: Double, lng: Double, method: Int): String {
        return "${date}_${lat}_${lng}_$method"
    }

    private fun serializeTimings(timings: Map<PrayerName, String>): String {
        return gson.toJson(timings)
    }

    private fun deserializeTimings(json: String): Map<PrayerName, String> {
        val type = object : TypeToken<Map<PrayerName, String>>() {}.type
        return gson.fromJson(json, type)
    }

    /**
     * Aladhan sometimes returns times with timezone offsets like "05:30 (WIB)".
     * This strips anything after the time.
     */
    private fun cleanTime(raw: String): String {
        return raw.trim().split(" ").first()
    }
}
