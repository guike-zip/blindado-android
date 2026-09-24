package io.blindado.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = BlindadoGreen,
    onPrimary = BackgroundDark,
    secondary = BlindadoGreenDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
)

private val LightColors = lightColorScheme(
    primary = BlindadoGreenDark,
    onPrimary = SurfaceLight,
    secondary = BlindadoGreen,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
)

/** Tema Material 3 do Blindado — respeita claro/escuro do sistema (Constituição, Princípio VI). */
@Composable
fun BlindadoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = BlindadoTypography,
        content = content,
    )
}
