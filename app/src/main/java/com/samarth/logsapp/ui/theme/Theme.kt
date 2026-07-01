package com.samarth.logsapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NotesDarkColorScheme = darkColorScheme(
    background = BgColor,
    surface = PanelColor,
    onBackground = TextColor,
    onSurface = TextColor,
    primary = AccentColor,
    onPrimary = BgColor,
    secondary = AccentDimColor,
    error = ErrorColor
)

@Composable
fun LogsAppTheme(
    // The app is always dark-themed by design (matches the web journal),
    // regardless of system theme — kept as a param in case that changes later.
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NotesDarkColorScheme,
        typography = NotesTypography,
        content = content
    )
}
