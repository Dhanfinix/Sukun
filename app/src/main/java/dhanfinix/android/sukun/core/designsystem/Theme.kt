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
    // Primary — sandy tan pastel
    primary = WarmBrown80,
    onPrimary = Color(0xFF3C2211),
    primaryContainer = Color(0xFF553722),
    onPrimaryContainer = Color(0xFFF3EAD8),
    // Secondary — neutral taupe
    secondary = Taupe80,
    onSecondary = Color(0xFF44362D),
    secondaryContainer = Color(0xFF5C4E44),
    onSecondaryContainer = Color(0xFFD8CFC8),
    // Tertiary — warm khaki
    tertiary = Khaki80,
    onTertiary = Color(0xFF38340B),
    tertiaryContainer = Color(0xFF504C21),
    onTertiaryContainer = Color(0xFFF4EDBA),
    // Error
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // Background / Surface
    background = SukunSurfaceDark,
    onBackground = Color(0xFFE4DDD7),
    surface = SukunSurfaceDark,
    onSurface = Color(0xFFE4DDD7),
    surfaceVariant = Color(0xFF514741),
    onSurfaceVariant = Color(0xFFCFC8C1),
    // Surface containers
    surfaceContainerLowest = Color(0xFF16110C),
    surfaceContainerLow = Color(0xFF231813),
    surfaceContainer = Color(0xFF271C17),
    surfaceContainerHigh = Color(0xFF322721),
    surfaceContainerHighest = Color(0xFF3D312B),
    // Outline
    outline = Color(0xFF9C8D87),
    outlineVariant = Color(0xFF514741),
    // Inverse
    inverseSurface = Color(0xFFE4DDD7),
    inverseOnSurface = Color(0xFF332A24),
    inversePrimary = WarmBrown40,
    scrim = Color(0xFF000000),
)

private val LightColorScheme = lightColorScheme(
    // Primary — warm coffee brown
    primary = WarmBrown40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3EAD8),  // exact #F3EAD8 from palette
    onPrimaryContainer = Color(0xFF231108),
    // Secondary — neutral taupe
    secondary = Taupe40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8CFC8),
    onSecondaryContainer = Color(0xFF2C2219),
    // Tertiary — warm khaki/sand
    tertiary = Khaki40,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF4EDBA),
    onTertiaryContainer = Color(0xFF221F02),
    // Error
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    // Background / Surface
    background = SukunSurfaceLight,
    onBackground = Color(0xFF1C1611),
    surface = SukunSurfaceLight,
    onSurface = Color(0xFF1C1611),
    surfaceVariant = Color(0xFFEDE5DA),
    onSurfaceVariant = Color(0xFF514741),
    // Surface containers — warm beige tiers
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F1EB),
    surfaceContainer = Color(0xFFF0EAE3),
    surfaceContainerHigh = Color(0xFFEAE4DD),
    surfaceContainerHighest = Color(0xFFE4DDD7),
    // Outline
    outline = Color(0xFF837470),
    outlineVariant = Color(0xFFCFC8C1),
    // Inverse
    inverseSurface = Color(0xFF332A24),
    inverseOnSurface = Color(0xFFF7EDE6),
    inversePrimary = WarmBrown80,
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