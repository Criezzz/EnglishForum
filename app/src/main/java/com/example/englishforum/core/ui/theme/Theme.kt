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
    return if (isDarkTheme) {
        val primary = seedColor.shiftLightness(0.75f)
        val secondary = seedColor.shiftHue(20f).shiftLightness(0.65f)
        val tertiary = seedColor.shiftHue(-35f).shiftLightness(0.7f)
        val neutral = if (useAmoled) Color.Black else seedColor.shiftSaturation(0.15f).shiftLightness(0.18f)
        val elevatedSurface = if (useAmoled) Color(0xFF101010) else neutral.shiftLightness(1.1f)

        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = seedColor.shiftLightness(0.45f),
            onPrimaryContainer = Color.White,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondary.shiftLightness(0.55f),
            onSecondaryContainer = Color.White,
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = tertiary.shiftLightness(0.5f),
            onTertiaryContainer = Color.White,
            background = if (useAmoled) Color.Black else neutral,
            onBackground = Color.White,
            surface = if (useAmoled) Color.Black else neutral,
            surfaceVariant = if (useAmoled) Color(0xFF1C1C1C) else elevatedSurface,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFDADADA),
            outline = Color(0xFF8F8F8F)
        )
    } else {
        val primary = seedColor.shiftLightness(1.05f)
        val secondary = seedColor.shiftHue(20f).shiftLightness(1.15f)
        val tertiary = seedColor.shiftHue(-25f).shiftLightness(1.1f)
        val surface = seedColor.shiftSaturation(0.12f).shiftLightness(1.35f)

        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.shiftLightness(1.25f),
            onPrimaryContainer = Color(0xFF1B1B1F),
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondary.shiftLightness(1.2f),
            onSecondaryContainer = Color(0xFF1B1B1F),
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = tertiary.shiftLightness(1.2f),
            onTertiaryContainer = Color(0xFF1B1B1F),
            background = surface,
            onBackground = Color(0xFF1B1B1F),
            surface = surface,
            surfaceVariant = surface.shiftLightness(0.95f),
            onSurface = Color(0xFF1C1B1F),
            onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFF79747E)
        )
    }
}

private fun Color.shiftLightness(factor: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[2] = (hsl[2] * factor).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.shiftSaturation(factor: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[1] = (hsl[1] * factor).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.shiftHue(delta: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[0] = (hsl[0] + delta) % 360f
    if (hsl[0] < 0f) hsl[0] += 360f
    return Color(ColorUtils.HSLToColor(hsl))
}
