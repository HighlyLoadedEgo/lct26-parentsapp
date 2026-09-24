package ru.nksk.parentsapp.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ParentsColorScheme = lightColorScheme(
    primary = Lime,
    onPrimary = Ink,
    primaryContainer = LimeContainer,
    onPrimaryContainer = Ink,
    secondary = LavenderSoft,
    onSecondary = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = ChipGray,
    onSurfaceVariant = MutedInk,
)

@Composable
fun ParentsAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ParentsColorScheme,
        typography = Typography,
        content = content,
    )
}
