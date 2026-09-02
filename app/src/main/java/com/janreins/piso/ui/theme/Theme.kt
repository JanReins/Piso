package com.janreins.piso.ui.theme

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
import com.janreins.piso.data.local.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = BrandSage,
    onPrimary = Color(0xFF062016),
    primaryContainer = BrandPine,
    onPrimaryContainer = Color(0xFFD8EADF),
    secondary = BrandSageSoft,
    onSecondary = Color(0xFF062016),
    tertiary = BrandAccent,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = Color(0xFFE1E3DF),
    onSurface = Color(0xFFE1E3DF),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = OutlineDark,
    error = ExpenseRed,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandForest,
    onPrimary = Color.White,
    primaryContainer = BrandContainerLight,
    onPrimaryContainer = OnBrandContainerLight,
    secondary = BrandAccent,
    onSecondary = Color.White,
    tertiary = BrandPine,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A),
    onSurfaceVariant = Color(0xFF49544E),
    outline = OutlineLight,
    error = ExpenseRed,
    errorContainer = ExpenseContainer,
    onErrorContainer = OnExpenseContainer
)

@Composable
fun PisoTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

