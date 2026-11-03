package id.mts.rth.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = EmeraldTeal,
    onPrimary = White,
    primaryContainer = MintLembut,
    onPrimaryContainer = TealGelap,
    secondary = SkyBlue,
    onSecondary = White,
    secondaryContainer = SkyLembut,
    onSecondaryContainer = SkyGelap,
    background = SlateBackground,
    onBackground = SlateDark,
    surface = White,
    onSurface = SlateDark,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = SlateOutline
)

@Composable
fun SimrthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Tetap menggunakan tema Emerald Madrasah yang konsisten di semua perangkat HP guru
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
