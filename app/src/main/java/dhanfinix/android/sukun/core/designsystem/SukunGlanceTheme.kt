package dhanfinix.android.sukun.core.designsystem

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.LocalContext

/**
 * A Glance-specific theme for Sukun widgets that mirrors the app's Material 3 theme.
 * Supports dynamic color (Material You) on Android 12+ and falls back to the 
 * custom Sukun palette otherwise.
 */
@Composable
fun SukunGlanceTheme(
    useDynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorProviders = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ColorProviders(
            light = dynamicLightColorScheme(context),
            dark = dynamicDarkColorScheme(context)
        )
    } else {
        ColorProviders(
            light = lightColorScheme(
                primary = WarmBrown40,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFF3EAD8),
                onPrimaryContainer = Color(0xFF231108),
                secondary = Taupe40,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFD8CFC8),
                onSecondaryContainer = Color(0xFF2C2219),
                tertiary = Khaki40,
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFF4EDBA),
                onTertiaryContainer = Color(0xFF221F02),
                background = SukunSurfaceLight,
                onBackground = Color(0xFF1C1611),
                surface = SukunSurfaceLight,
                onSurface = Color(0xFF1C1611),
                surfaceVariant = Color(0xFFEDE5DA),
                onSurfaceVariant = Color(0xFF514741),
                error = Color(0xFFBA1A1A),
                onError = Color.White,
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002),
                outline = Color(0xFF837470)
            ),
            dark = darkColorScheme(
                primary = WarmBrown80,
                onPrimary = Color(0xFF3C2211),
                primaryContainer = Color(0xFF553722),
                onPrimaryContainer = Color(0xFFF3EAD8),
                secondary = Taupe80,
                onSecondary = Color(0xFF44362D),
                secondaryContainer = Color(0xFF5C4E44),
                onSecondaryContainer = Color(0xFFD8CFC8),
                tertiary = Khaki80,
                onTertiary = Color(0xFF38340B),
                tertiaryContainer = Color(0xFF504C21),
                onTertiaryContainer = Color(0xFFF4EDBA),
                background = SukunSurfaceDark,
                onBackground = Color(0xFFE4DDD7),
                surface = SukunSurfaceDark,
                onSurface = Color(0xFFE4DDD7),
                surfaceVariant = Color(0xFF514741),
                onSurfaceVariant = Color(0xFFCFC8C1),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF690005),
                errorContainer = Color(0xFF93000A),
                onErrorContainer = Color(0xFFFFDAD6),
                outline = Color(0xFF9C8D87)
            )
        )
    }

    GlanceTheme(
        colors = colorProviders,
        content = content
    )
}
