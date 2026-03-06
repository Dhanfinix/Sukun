package dhanfinix.android.sukun.core.utils

import java.util.Locale

/**
 * Utility to localize digits (0-9) to Eastern Arabic numerals (٠-٩)
 * if the current locale is Arabic.
 */
fun String.localizeDigits(): String {
    val locale = Locale.getDefault()
    if (locale.language != "ar") return this

    val digits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return this.map { char ->
        if (char in '0'..'9') {
            digits[char - '0']
        } else {
            char
        }
    }.joinToString("")
}

/**
 * Convenience for Int to localized String
 */
fun Int.toLocalizedPath(): String = this.toString().localizeDigits()
