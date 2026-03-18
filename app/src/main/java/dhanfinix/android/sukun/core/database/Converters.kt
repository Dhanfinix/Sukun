package dhanfinix.android.sukun.core.database

import androidx.room.TypeConverter
import dhanfinix.android.sukun.core.database.entity.SilentZoneSource

class Converters {
    @TypeConverter
    fun fromSource(source: SilentZoneSource): String {
        return source.name
    }

    @TypeConverter
    fun toSource(value: String): SilentZoneSource {
        return try {
            SilentZoneSource.valueOf(value)
        } catch (e: Exception) {
            SilentZoneSource.MANUAL
        }
    }
}
