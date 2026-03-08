package dhanfinix.android.sukun.core.utils

import dhanfinix.android.sukun.core.datastore.NumeralSystem
import dhanfinix.android.sukun.core.datastore.TimeFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.text.format.DateFormat
import android.content.Context

/**
 * Utility to localize digits (0-9) to Eastern Arabic numerals (٠-٩)
 * if the current locale is Arabic.
 */
fun String.localizeDigits(numeralSystem: NumeralSystem = NumeralSystem.AUTO): String {
    val locale = Locale.getDefault()
    val isArabic = locale.language == "ar"

    // Fix: We need BiDi isolation for Arabic locale even if we don't localize digits
    // to prevent signs (-, :) from flipping to the wrong side.
    if (!isArabic) return this

    val shouldLocalize = when (numeralSystem) {
        NumeralSystem.AUTO -> isArabic
        NumeralSystem.EASTERN -> isArabic // Only localize if in Arabic locale
        NumeralSystem.WESTERN -> false
    }

    val digits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val converted = if (shouldLocalize) {
        this.map { char ->
            if (char in '0'..'9') {
                digits[char - '0']
            } else {
                char
            }
        }.joinToString("")
    } else {
        this
    }

    // Wrap in Left-to-Right Isolate (\u2066) and Pop Directional Isolate (\u2069)
    // to ensure signs like '-' stay on the left as a prefix even in RTL containers.
    return "\u2066$converted\u2069"
}

/**
 * Convenience for Int to localized String
 */
fun Int.toLocalizedPath(numeralSystem: NumeralSystem = NumeralSystem.AUTO): String = 
    this.toString().localizeDigits(numeralSystem)

/**
 * Formats a time string (HH:mm or HH:mm:ss) according to user preference.
 */
fun String.formatTime(
    context: Context,
    timeFormat: TimeFormat = TimeFormat.AUTO
): String {
    if (this.isBlank() || this == "--:--") return this
    
    val is24Hour = when (timeFormat) {
        TimeFormat.H12 -> false
        TimeFormat.H24 -> true
        TimeFormat.AUTO -> DateFormat.is24HourFormat(context)
    }

    return try {
        val hasSeconds = this.count { it == ':' } == 2
        val inputFormatter = if (hasSeconds) DateTimeFormatter.ofPattern("HH:mm:ss") else DateTimeFormatter.ofPattern("HH:mm")
        val time = LocalTime.parse(this, inputFormatter)
        
        val outputPattern = if (is24Hour) {
            if (hasSeconds) "HH:mm:ss" else "HH:mm"
        } else {
            if (hasSeconds) "hh:mm:ss a" else "hh:mm a"
        }
        
        // Use Locale.US for the formatting to keep numeric digits as '0-9' 
        // before they are later localized by localizeDigits()
        time.format(DateTimeFormatter.ofPattern(outputPattern, Locale.US))
    } catch (e: Exception) {
        this
    }
}
