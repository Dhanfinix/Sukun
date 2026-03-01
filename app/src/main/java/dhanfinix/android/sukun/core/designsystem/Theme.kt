package dhanfinix.android.sukun.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import dhanfinix.android.sukun.core.datastore.AppTheme

private val DarkColorScheme = darkColorScheme(
    // Primary — peach/terracotta pastel
    primary = Clay80,
    onPrimary = Color(0xFF5A1900),
    primaryContainer = Color(0xFF7A3016),
    onPrimaryContainer = Color(0xFFFFDBCE),
    // Secondary — dusty blush
    secondary = Blush80,
    onSecondary = Color(0xFF462824),
    secondaryContainer = Color(0xFF5F3F3A),
    onSecondaryContainer = Color(0xFFFFDBD4),
    // Tertiary — warm ochre
    tertiary = Olive80,
    onTertiary = Color(0xFF373102),
    tertiaryContainer = Color(0xFF4E461A),
    onTertiaryContainer = Color(0xFFF3E2A9),
    // Error
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // Background / Surface
    background = SukunSurfaceDark,
    onBackground = Color(0xFFF0DDD6),
    surface = SukunSurfaceDark,
    onSurface = Color(0xFFF0DDD6),
    surfaceVariant = Color(0xFF534340),
    onSurfaceVariant = Color(0xFFD8C2BE),
    // Surface containers
    surfaceContainerLowest = Color(0xFF150E0C),
    surfaceContainerLow = Color(0xFF221917),
    surfaceContainer = Color(0xFF26201E),
    surfaceContainerHigh = Color(0xFF312725),
    surfaceContainerHighest = Color(0xFF3C3230),
    // Outline
    outline = Color(0xFFA08C89),
    outlineVariant = Color(0xFF534340),
    // Inverse
    inverseSurface = Color(0xFFF0DDD6),
    inverseOnSurface = Color(0xFF382E2B),
    inversePrimary = Clay40,
    scrim = Color(0xFF000000),
)

private val LightColorScheme = lightColorScheme(
    // Primary — muted terracotta
    primary = Clay40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCE),
    onPrimaryContainer = Color(0xFF320F00),
    // Secondary — dusty blush/mocha
    secondary = Blush40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBD4),
    onSecondaryContainer = Color(0xFF2C1511),
    // Tertiary — warm olive/khaki
    tertiary = Olive40,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E2A9),
    onTertiaryContainer = Color(0xFF221B00),
    // Error
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    // Background / Surface
    background = SukunSurfaceLight,
    onBackground = Color(0xFF221917),
    surface = SukunSurfaceLight,
    onSurface = Color(0xFF221917),
    surfaceVariant = Color(0xFFF5DED8),
    onSurfaceVariant = Color(0xFF534340),
    // Surface containers (warm tinted tiers)
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0EB),
    surfaceContainer = Color(0xFFFCE9E3),
    surfaceContainerHigh = Color(0xFFF6E3DC),
    surfaceContainerHighest = Color(0xFFF0DDD6),
    // Outline
    outline = Color(0xFF857370),
    outlineVariant = Color(0xFFD8C2BE),
    // Inverse
    inverseSurface = Color(0xFF382E2B),
    inverseOnSurface = Color(0xFFFFF0EB),
    inversePrimary = Clay80,
    scrim = Color(0xFF000000),
)

@Composable
fun SukunTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isAppInDarkTheme = when (appTheme) {
        AppTheme.SYSTEM -> darkTheme
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isAppInDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isAppInDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isAppInDarkTheme
                isAppearanceLightNavigationBars = !isAppInDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}