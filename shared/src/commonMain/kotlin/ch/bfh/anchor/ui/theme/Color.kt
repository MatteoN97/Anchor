package ch.bfh.anchor.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Anchor's own palette: "soft and calm". A sage green to lead, a muted lavender beside it, a warm
 * peach for highlights and a terracotta for anything urgent, on warm off-white (light) or a
 * green-tinted near-black (dark). Every text/background pair below meets WCAG AA.
 *
 * Material You is opt-in (Settings → Appearance); without it, Android uses these schemes and iOS
 * the iOS-structured schemes in IosTheme.kt, which take their accents from here.
 */
internal object AnchorPalette {
    // Sage
    val Sage10 = Color(0xFF0E2A1F)
    val Sage30 = Color(0xFF2B4D3E)
    val Sage40 = Color(0xFF3F6A57)
    val Sage50 = Color(0xFF4F7A68)
    val Sage80 = Color(0xFF9ECBB5)
    val Sage90 = Color(0xFFD3E8DC)
    val Sage95 = Color(0xFFE7F3EC)

    // Lavender
    val Lavender20 = Color(0xFF24214A)
    val Lavender30 = Color(0xFF3E3A66)
    val Lavender50 = Color(0xFF6A66A0)
    val Lavender80 = Color(0xFFC7C2F0)
    val Lavender90 = Color(0xFFE6E3F8)

    // Peach
    val Peach20 = Color(0xFF3A1A08)
    val Peach30 = Color(0xFF5A3320)
    val Peach50 = Color(0xFF9A5B3A)
    val Peach80 = Color(0xFFF0B898)
    val Peach90 = Color(0xFFFBE3D4)

    // Terracotta, for SOS and errors: warm, but still unmistakably "this one matters".
    val Terra20 = Color(0xFF4A120B)
    val Terra30 = Color(0xFF6B2A21)
    val Terra50 = Color(0xFFB4473A)
    val Terra80 = Color(0xFFF2A196)
    val Terra90 = Color(0xFFFADDD7)

    // Warm neutrals (light)
    val Cream = Color(0xFFF7F4EE)
    val Sand1 = Color(0xFFF2EEE6)
    val Sand2 = Color(0xFFEDE8DF)
    val Sand3 = Color(0xFFE7E2D8)
    val Sand4 = Color(0xFFE1DBD0)
    val Stone = Color(0xFF5E5A52)
    val Ink = Color(0xFF1F1D1A)

    // Green-tinted neutrals (dark)
    val Night0 = Color(0xFF0F110F)
    val Night1 = Color(0xFF141614)
    val Night2 = Color(0xFF1A1D1A)
    val Night3 = Color(0xFF1F221F)
    val Night4 = Color(0xFF292C29)
    val Night5 = Color(0xFF333733)
    val Mist = Color(0xFFE5E2DC)
    val Fog = Color(0xFFBFBAB0)
}

internal val AnchorLightColorScheme: ColorScheme = with(AnchorPalette) {
    lightColorScheme(
        primary = Sage50,
        onPrimary = Color.White,
        primaryContainer = Sage90,
        onPrimaryContainer = Sage10,
        inversePrimary = Sage80,
        secondary = Lavender50,
        onSecondary = Color.White,
        secondaryContainer = Lavender90,
        onSecondaryContainer = Lavender20,
        tertiary = Peach50,
        onTertiary = Color.White,
        tertiaryContainer = Peach90,
        onTertiaryContainer = Peach20,
        background = Cream,
        onBackground = Ink,
        surface = Cream,
        onSurface = Ink,
        surfaceVariant = Sand2,
        onSurfaceVariant = Stone,
        surfaceTint = Sage50,
        surfaceBright = Color.White,
        surfaceDim = Sand4,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Sand1,
        surfaceContainer = Sand2,
        surfaceContainerHigh = Sand3,
        surfaceContainerHighest = Sand4,
        inverseSurface = Color(0xFF33312D),
        inverseOnSurface = Sand1,
        outline = Color(0xFF8B867C),
        outlineVariant = Color(0xFFD6D0C4),
        error = Terra50,
        onError = Color.White,
        errorContainer = Terra90,
        onErrorContainer = Terra20,
        scrim = Color(0xFF000000)
    )
}

internal val AnchorDarkColorScheme: ColorScheme = with(AnchorPalette) {
    darkColorScheme(
        primary = Sage80,
        onPrimary = Color(0xFF0E3526),
        primaryContainer = Sage30,
        onPrimaryContainer = Color(0xFFCDEBDC),
        inversePrimary = Sage50,
        secondary = Lavender80,
        onSecondary = Lavender20,
        secondaryContainer = Lavender30,
        onSecondaryContainer = Color(0xFFE6E3FF),
        tertiary = Peach80,
        onTertiary = Peach20,
        tertiaryContainer = Peach30,
        onTertiaryContainer = Peach90,
        background = Night1,
        onBackground = Mist,
        surface = Night1,
        onSurface = Mist,
        surfaceVariant = Night4,
        onSurfaceVariant = Fog,
        surfaceTint = Sage80,
        surfaceBright = Night5,
        surfaceDim = Night0,
        surfaceContainerLowest = Night0,
        surfaceContainerLow = Night2,
        surfaceContainer = Night3,
        surfaceContainerHigh = Night4,
        surfaceContainerHighest = Night5,
        inverseSurface = Mist,
        inverseOnSurface = Night3,
        outline = Color(0xFF8A877F),
        outlineVariant = Color(0xFF3F433F),
        error = Terra80,
        onError = Terra20,
        errorContainer = Terra30,
        onErrorContainer = Color(0xFFFFDAD4),
        scrim = Color(0xFF000000)
    )
}
