package dhanfinix.android.sukun.core.designsystem.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Launches a URL in a Chrome Custom Tab styled with Sukun's brand colors.
 * @param toolbarColorArgb The ARGB int of the toolbar color (use MaterialTheme.colorScheme.primary.toArgb()).
 * @param onBackgroundColorArgb The ARGB int of icon/text color (use MaterialTheme.colorScheme.onPrimary.toArgb()).
 * @param context Android context to launch the intent.
 * @param url The URL to open.
 */
fun launchSukunCustomTab(
    context: Context,
    url: String,
    toolbarColorArgb: Int,
    onBackgroundColorArgb: Int
) {
    val colorParams = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(toolbarColorArgb)
        .setNavigationBarColor(toolbarColorArgb)
        .setSecondaryToolbarColor(toolbarColorArgb)
        .build()

    CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(colorParams)
        .setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        .setShowTitle(true)
        .build()
        .launchUrl(context, Uri.parse(url))
}
