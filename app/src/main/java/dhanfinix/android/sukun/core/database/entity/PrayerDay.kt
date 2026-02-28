package dhanfinix.android.sukun.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_day")
data class PrayerDay(
    @PrimaryKey val id: String, // "YYYY-MM-DD_lat_lng_method"
    val date: String, // YYYY-MM-DD
    val latitude: Double,
    val longitude: Double,
    val methodId: Int,
    val timingsJson: String // Serialized Map<PrayerName, String>
)
