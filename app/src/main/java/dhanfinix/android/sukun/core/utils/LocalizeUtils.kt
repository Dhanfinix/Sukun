package dhanfinix.android.sukun.core.utils

import dhanfinix.android.sukun.core.datastore.TimeFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.text.format.DateFormat
import android.content.Context

/**
 * Wraps a string in LTR isolate characters to ensure it's rendered correctly in RTL contexts.
 * Useful for times, countdowns, and numbers.
 */
fun String.withIsolate(): String = "\u2066$this\u2069"

/**
 * Holds formatted time parts for UI display.
 */
data class FormattedTime(
    val time: String,      // e.g. "05:30"
    val session: String? = null // e.g. "AM", "PM", or null for 24h
)

/**
 * Formats a time string (HH:mm or HH:mm:ss) according to user preference.
 */
fun String.formatTime(
    context: Context,
    timeFormat: TimeFormat = TimeFormat.AUTO
): FormattedTime {
    if (this.isBlank() || this == "--:--") return FormattedTime(this)
    
    val is24Hour = when (timeFormat) {
        TimeFormat.H12 -> false
        TimeFormat.H24 -> true
        TimeFormat.AUTO -> DateFormat.is24HourFormat(context)
    }

    return try {
        val cleanInput = this.substringBefore(" ").trim()
        val hasSeconds = cleanInput.count { it == ':' } == 2
        val inputFormatter = if (hasSeconds) DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US) else DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        val time = LocalTime.parse(cleanInput, inputFormatter)
        
        if (is24Hour) {
            val pattern = if (hasSeconds) "HH:mm:ss" else "HH:mm"
            FormattedTime(time.format(DateTimeFormatter.ofPattern(pattern, Locale.US)))
        } else {
            val timePattern = if (hasSeconds) "hh:mm:ss" else "hh:mm"
            val amPmPattern = "a"
            
            val formattedTime = time.format(DateTimeFormatter.ofPattern(timePattern, Locale.US))
            // Force AM/PM to be Latin uppercase for modern look
            val session = time.format(DateTimeFormatter.ofPattern(amPmPattern, Locale.US)).uppercase()
            
            FormattedTime(formattedTime, session)
        }
    } catch (e: Exception) {
        FormattedTime(this)
    }
}
