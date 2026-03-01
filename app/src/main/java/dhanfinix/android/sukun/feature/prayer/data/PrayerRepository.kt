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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository that manages prayer times with Room caching and monthly fetching.
 */
class PrayerRepository(context: Context) {

    private val api = ApiClient.api
    private val prayerDao = SukunDatabase.getDatabase(context).prayerDao()
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val fetchMutex = Mutex()

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
        val roundedLat = Math.round(latitude * 10000.0) / 10000.0
        val roundedLng = Math.round(longitude * 10000.0) / 10000.0
        
        val dateStr = date.format(dateFormatter)
        val id = buildId(dateStr, roundedLat, roundedLng, method)

        // 1. Try Room cache (outside lock for speed)
        val cached = prayerDao.getPrayerDayById(id)
        if (cached != null) {
            return Result.success(deserializeTimings(cached.timingsJson))
        }

        // 2. Lock to prevent concurrent network fetches for the same month
        return fetchMutex.withLock {
            // Re-check cache inside lock
            val reCached = prayerDao.getPrayerDayById(id)
            if (reCached != null) {
                return@withLock Result.success(deserializeTimings(reCached.timingsJson))
            }

            try {
                val response = api.getCalendar(date.year, date.monthValue, roundedLat, roundedLng, method)
                if (response.code == 200) {
                    val prayerDays = response.data.map { dayData ->
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
                            id = buildId(formattedDate, roundedLat, roundedLng, method),
                            date = formattedDate,
                            latitude = roundedLat,
                            longitude = roundedLng,
                            methodId = method,
                            timingsJson = serializeTimings(timingsMap)
                        )
                    }

                    prayerDao.insertAll(prayerDays)

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
