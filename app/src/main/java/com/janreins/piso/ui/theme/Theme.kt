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
    outline = OutlineLight,
    error = ExpenseRed,
    errorContainer = ExpenseContainer,
    onErrorContainer = OnExpenseContainer
)

@Composable
fun PisoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false by default to showcase Piso brand teal
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
