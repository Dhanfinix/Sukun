package dhanfinix.android.sukun.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "silent_zones")
data class SilentZone(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float = 100f, // in meters
    val isEnabled: Boolean = true,
    val isAutoSilent: Boolean = true,
    val silenceDuration: Int? = null // in minutes, null means "Until Exit"
)
