package com.example.englishforum.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.example.englishforum.core.model.DEFAULT_SEED_COLOR
import com.example.englishforum.core.model.ThemeOption

private val DefaultSeedColor = Color(DEFAULT_SEED_COLOR.toInt())

@Composable
fun EnglishForumTheme(
    themeOption: ThemeOption = ThemeOption.FOLLOW_SYSTEM,
    useDynamicColor: Boolean = false,
    seedColor: Color = DefaultSeedColor,
    useAmoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeOption) {
        ThemeOption.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        ThemeOption.LIGHT -> false
        ThemeOption.DARK -> true
    }

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val baseScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            
            // Apply AMOLED mode to dynamic color scheme if enabled
            if (useAmoled && darkTheme) {
                baseScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF1C1C1C),
                    surfaceContainer = Color(0xFF101010),
                    surfaceContainerLow = Color(0xFF0A0A0A),
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerHigh = Color(0xFF1F1F1F),
                    surfaceContainerHighest = Color(0xFF2A2A2A)
                )
            } else {
                baseScheme
            }
        }

        else -> createSeedColorScheme(
            seedColor = seedColor,
            isDarkTheme = darkTheme,
            useAmoled = useAmoled
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun createSeedColorScheme(
    seedColor: Color,
    isDarkTheme: Boolean,
    useAmoled: Boolean
): ColorScheme {
    // Convert to HSL for proper tone generation
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seedColor.toArgb(), hsl)
    val hue = hsl[0]
    
    return if (isDarkTheme) {
        // Material 3 tonal palette approach
        val primary = colorFromHslTone(hue, 0.48f, 0.80f)  // 80 tone
        val onPrimary = colorFromHslTone(hue, 0.48f, 0.20f)  // 20 tone
        val primaryContainer = colorFromHslTone(hue, 0.48f, 0.30f)  // 30 tone
        val onPrimaryContainer = colorFromHslTone(hue, 0.48f, 0.90f)  // 90 tone
        
        val secondary = colorFromHslTone(hue + 20f, 0.36f, 0.80f)
        val onSecondary = colorFromHslTone(hue + 20f, 0.36f, 0.20f)
        val secondaryContainer = colorFromHslTone(hue + 20f, 0.36f, 0.30f)
        val onSecondaryContainer = colorFromHslTone(hue + 20f, 0.36f, 0.90f)
        
        val tertiary = colorFromHslTone(hue - 35f, 0.40f, 0.80f)
        val onTertiary = colorFromHslTone(hue - 35f, 0.40f, 0.20f)
        val tertiaryContainer = colorFromHslTone(hue - 35f, 0.40f, 0.30f)
        val onTertiaryContainer = colorFromHslTone(hue - 35f, 0.40f, 0.90f)
        
        val error = Color(0xFFFFB4AB)
        val onError = Color(0xFF690005)
        val errorContainer = Color(0xFF93000A)
        val onErrorContainer = Color(0xFFFFDAD6)
        
        val background = if (useAmoled) Color.Black else colorFromHslTone(hue, 0.06f, 0.10f)
        val onBackground = colorFromHslTone(hue, 0.06f, 0.90f)
        
        val surface = if (useAmoled) Color.Black else colorFromHslTone(hue, 0.06f, 0.10f)
        val onSurface = colorFromHslTone(hue, 0.06f, 0.90f)
        val surfaceVariant = if (useAmoled) Color(0xFF1C1C1C) else colorFromHslTone(hue, 0.12f, 0.20f)
        val onSurfaceVariant = colorFromHslTone(hue, 0.12f, 0.80f)
        
        val outline = colorFromHslTone(hue, 0.12f, 0.60f)
        val outlineVariant = colorFromHslTone(hue, 0.12f, 0.30f)
        
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainer = if (useAmoled) Color(0xFF101010) else colorFromHslTone(hue, 0.06f, 0.12f),
            surfaceContainerHigh = if (useAmoled) Color(0xFF1F1F1F) else colorFromHslTone(hue, 0.06f, 0.17f),
            surfaceContainerHighest = if (useAmoled) Color(0xFF2A2A2A) else colorFromHslTone(hue, 0.06f, 0.22f),
            surfaceContainerLow = if (useAmoled) Color(0xFF0A0A0A) else colorFromHslTone(hue, 0.06f, 0.08f),
            surfaceContainerLowest = if (useAmoled) Color.Black else colorFromHslTone(hue, 0.06f, 0.04f),
            outline = outline,
            outlineVariant = outlineVariant,
            inverseSurface = colorFromHslTone(hue, 0.06f, 0.90f),
            inverseOnSurface = colorFromHslTone(hue, 0.06f, 0.20f),
            inversePrimary = colorFromHslTone(hue, 0.48f, 0.40f)
        )
    } else {
        // Light theme with Material 3 tones
        val primary = colorFromHslTone(hue, 0.48f, 0.40f)  // 40 tone
        val onPrimary = Color.White
        val primaryContainer = colorFromHslTone(hue, 0.48f, 0.90f)  // 90 tone
        val onPrimaryContainer = colorFromHslTone(hue, 0.48f, 0.10f)  // 10 tone
        
        val secondary = colorFromHslTone(hue + 20f, 0.36f, 0.40f)
        val onSecondary = Color.White
        val secondaryContainer = colorFromHslTone(hue + 20f, 0.36f, 0.90f)
        val onSecondaryContainer = colorFromHslTone(hue + 20f, 0.36f, 0.10f)
        
        val tertiary = colorFromHslTone(hue - 35f, 0.40f, 0.40f)
        val onTertiary = Color.White
        val tertiaryContainer = colorFromHslTone(hue - 35f, 0.40f, 0.90f)
        val onTertiaryContainer = colorFromHslTone(hue - 35f, 0.40f, 0.10f)
        
        val error = Color(0xFFBA1A1A)
        val onError = Color.White
        val errorContainer = Color(0xFFFFDAD6)
        val onErrorContainer = Color(0xFF410002)
        
        val background = colorFromHslTone(hue, 0.06f, 0.98f)
        val onBackground = colorFromHslTone(hue, 0.06f, 0.10f)
        
        val surface = colorFromHslTone(hue, 0.06f, 0.98f)
        val onSurface = colorFromHslTone(hue, 0.06f, 0.10f)
        val surfaceVariant = colorFromHslTone(hue, 0.12f, 0.90f)
        val onSurfaceVariant = colorFromHslTone(hue, 0.12f, 0.30f)
        
        val outline = colorFromHslTone(hue, 0.12f, 0.50f)
        val outlineVariant = colorFromHslTone(hue, 0.12f, 0.80f)
        
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainer = colorFromHslTone(hue, 0.06f, 0.94f),
            surfaceContainerHigh = colorFromHslTone(hue, 0.06f, 0.92f),
            surfaceContainerHighest = colorFromHslTone(hue, 0.06f, 0.90f),
            surfaceContainerLow = colorFromHslTone(hue, 0.06f, 0.96f),
            surfaceContainerLowest = Color.White,
            outline = outline,
            outlineVariant = outlineVariant,
            inverseSurface = colorFromHslTone(hue, 0.06f, 0.20f),
            inverseOnSurface = colorFromHslTone(hue, 0.06f, 0.95f),
            inversePrimary = colorFromHslTone(hue, 0.48f, 0.80f)
        )
    }
}

// Helper function to create colors using Material 3's tonal palette approach
private fun colorFromHslTone(hue: Float, saturation: Float, lightness: Float): Color {
    val normalizedHue = ((hue % 360f) + 360f) % 360f
    return Color(ColorUtils.HSLToColor(floatArrayOf(normalizedHue, saturation, lightness)))
}

