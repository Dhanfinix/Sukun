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
        method: Int = 20,
        offsets: Map<PrayerName, Int> = emptyMap()
    ): Result<Map<PrayerName, String>> {
        val roundedLat = Math.round(latitude * 10000.0) / 10000.0
        val roundedLng = Math.round(longitude * 10000.0) / 10000.0
        
        val dateStr = date.format(dateFormatter)
        val id = buildId(dateStr, roundedLat, roundedLng, method)

        // 1. Try Room cache (outside lock for speed)
        val cached = prayerDao.getPrayerDayById(id)
        if (cached != null) {
            val timings = deserializeTimings(cached.timingsJson)
            return Result.success(applyOffsets(timings, offsets))
        }

        // 2. Lock to prevent concurrent network fetches for the same month
        return fetchMutex.withLock {
            // Re-check cache inside lock
            val reCached = prayerDao.getPrayerDayById(id)
            if (reCached != null) {
                val timings = deserializeTimings(reCached.timingsJson)
                return@withLock Result.success(applyOffsets(timings, offsets))
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
                            PrayerName.JUMUAH to cleanTime(dayData.timings.dhuhr), // Same time as Dhuhr
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
                        val timings = deserializeTimings(newCached.timingsJson)
                        Result.success(applyOffsets(timings, offsets))
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

    private fun applyOffsets(timings: Map<PrayerName, String>, offsets: Map<PrayerName, Int>): Map<PrayerName, String> {
        if (offsets.isEmpty()) return timings
        
        return timings.mapValues { (prayer, timeStr) ->
            val offsetMins = offsets[prayer] ?: 0
            if (offsetMins == 0 || timeStr == "--:--") return@mapValues timeStr
            
            try {
                val parts = timeStr.split(":")
                var h = parts[0].toInt()
                var m = parts[1].toInt()
                
                m += offsetMins
                while (m < 0) {
                    m += 60
                    h -= 1
                }
                while (m >= 60) {
                    m -= 60
                    h += 1
                }
                h = (h % 24 + 24) % 24
                
                String.format("%02d:%02d", h, m)
            } catch (e: Exception) {
                timeStr
            }
        }
    }
}
