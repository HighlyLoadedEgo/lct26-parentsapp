package ru.nksk.parentsapp.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ParentsColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandSurface,
    secondary = BrandPrimaryDark,
    background = BrandSurface,
    surface = BrandSurface,
    surfaceVariant = BrandSurfaceVariant,
    onBackground = BrandPrimaryDark,
    onSurface = BrandPrimaryDark,
    onSurfaceVariant = BrandOnSurfaceMuted,
)

@Composable
fun ParentsAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ParentsColorScheme,
        typography = Typography,
        content = content,
    )
}
