package com.tridente.nootiz.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NootizColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = PureWhite,
    secondary = SkyBlueCard,
    onSecondary = TextPrimaryNavy,
    tertiary = LimeAccent,
    onTertiary = TextPrimaryNavy,
    background = OffWhiteBackground,
    onBackground = TextPrimaryNavy,
    surface = OffWhiteBackground,
    onSurface = TextPrimaryNavy,
    surfaceVariant = IceBlueCard,
    onSurfaceVariant = TextSecondarySlate
)

@Composable
fun NootizTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NootizColorScheme,
        typography = Typography,
        content = content
    )
}
