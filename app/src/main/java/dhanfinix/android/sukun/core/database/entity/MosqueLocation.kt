package dhanfinix.android.sukun.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mosque_location")
data class MosqueLocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val address: String? = null // Optional for custom or global details
)
