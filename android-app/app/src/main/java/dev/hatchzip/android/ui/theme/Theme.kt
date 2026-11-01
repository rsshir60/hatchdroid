package dev.hatchzip.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val HatchDarkColorScheme = darkColorScheme(
    primary = HatchPinkAccent,
    onPrimary = Color.Black,
    primaryContainer = HatchPinkContainer,
    onPrimaryContainer = HatchPinkOnContainer,
    secondary = ArchivePurple,
    onSecondary = Color.White,
    secondaryContainer = ArchivePurpleContainer,
    onSecondaryContainer = ArchivePurpleText,
    tertiary = EmeraldGreen,
    onTertiary = Color.Black,
    tertiaryContainer = EmeraldGreenContainer,
    onTertiaryContainer = EmeraldGreenText,
    background = HatchBackground,
    onBackground = TextPrimary,
    surface = HatchSurface,
    onSurface = TextPrimary,
    surfaceVariant = HatchSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = HatchOutline,
    outlineVariant = HatchDivider,
    error = CoralRed,
    onError = Color.Black,
    errorContainer = CoralRedContainer,
    onErrorContainer = CoralRedText
)

@Composable
fun HatchZipTheme(
    darkTheme: Boolean = true, // Default to deep dark visual identity
    dynamicColor: Boolean = false, // Keep locked brand colors by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> HatchDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
