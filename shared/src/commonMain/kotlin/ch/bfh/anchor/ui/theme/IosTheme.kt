package ch.bfh.anchor.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.bfh.anchor.platform.isIosHost
import kotlinx.coroutines.launch

/** Whether to render the iOS flavour of the UI. Provided once in [AnchorTheme]. */
val LocalIsIos = staticCompositionLocalOf { isIosHost }

/**
 * iOS-structured colours (UIKit's grouped-table layering) carrying Anchor's calm palette. The
 * mapping, so screens can stay platform-agnostic:
 *
 * - background / surface         → systemGroupedBackground (the page behind sections), warmed
 * - surfaceVariant / containers  → secondarySystemGroupedBackground (the cells and cards)
 * - secondaryContainer           → tertiarySystemFill (segmented control track, chips)
 * - onSurfaceVariant             → secondaryLabel
 * - outlineVariant               → separator
 * - tertiary                     → the switch "on" colour (sage instead of system green)
 */
internal object IosColors {
    val SageLight = Color(0xFF4F7A68)
    val SageDark = Color(0xFF8CC3A8)
    val SwitchLight = Color(0xFF5B9B7E)
    val SwitchDark = Color(0xFF4F9476)
    val RedLight = Color(0xFFB4473A)
    val RedDark = Color(0xFFE0675A)
    val Gray = Color(0xFF8E8E93)
}

internal val IosLightColorScheme: ColorScheme = lightColorScheme(
    primary = IosColors.SageLight,
    onPrimary = Color.White,
    primaryContainer = AnchorPalette.Sage90,
    onPrimaryContainer = AnchorPalette.Sage10,
    secondary = AnchorPalette.Lavender50,
    onSecondary = Color.White,
    secondaryContainer = Color(0x1F767680),
    onSecondaryContainer = Color.Black,
    tertiary = IosColors.SwitchLight,
    onTertiary = Color.White,
    tertiaryContainer = AnchorPalette.Peach90,
    onTertiaryContainer = AnchorPalette.Peach20,
    background = Color(0xFFF4F1EB),
    onBackground = AnchorPalette.Ink,
    surface = Color(0xFFF4F1EB),
    onSurface = AnchorPalette.Ink,
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0x993C3C43),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F1EB),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    outline = Color(0xFFC6C6C8),
    outlineVariant = Color(0x4A3C3C43),
    error = IosColors.RedLight,
    onError = Color.White,
    errorContainer = AnchorPalette.Terra90,
    onErrorContainer = AnchorPalette.Terra20,
    scrim = Color(0x66000000)
)

internal val IosDarkColorScheme: ColorScheme = darkColorScheme(
    primary = IosColors.SageDark,
    onPrimary = Color(0xFF0E3526),
    primaryContainer = AnchorPalette.Sage30,
    onPrimaryContainer = Color(0xFFCDEBDC),
    secondary = AnchorPalette.Lavender80,
    onSecondary = AnchorPalette.Lavender20,
    secondaryContainer = Color(0x3D767680),
    onSecondaryContainer = Color.White,
    tertiary = IosColors.SwitchDark,
    onTertiary = Color.White,
    tertiaryContainer = AnchorPalette.Peach30,
    onTertiaryContainer = AnchorPalette.Peach90,
    background = Color(0xFF0B0D0B),
    onBackground = Color.White,
    surface = Color(0xFF0B0D0B),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1B1E1B),
    onSurfaceVariant = Color(0x99EBEBF5),
    surfaceContainerLowest = Color(0xFF1B1E1B),
    surfaceContainerLow = Color(0xFF1B1E1B),
    surfaceContainer = Color(0xFF262A26),
    surfaceContainerHigh = Color(0xFF262A26),
    surfaceContainerHighest = Color(0xFF5F635F), // selected segment in dark mode
    outline = Color(0xFF383A38),
    outlineVariant = Color(0x99545458),
    error = IosColors.RedDark,
    onError = Color.White,
    errorContainer = AnchorPalette.Terra30,
    onErrorContainer = Color(0xFFFFDAD4),
    scrim = Color(0x99000000)
)

private fun ios(size: Int, lineHeight: Int, tracking: Double, weight: FontWeight = FontWeight.Normal) =
    TextStyle(
        fontFamily = FontFamily.Default, // San Francisco on iOS
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = tracking.sp
    )

/**
 * Apple's default Dynamic Type sizes, line heights and SF Pro tracking, assigned to the Material
 * slots by role. Material's own letter spacing (+0.5 on body text) is what made the iOS build look
 * loosely spaced and "Android-like" even though it was already drawing San Francisco.
 */
internal val IosTypography = Typography(
    displayLarge = ios(34, 41, 0.40, FontWeight.Bold),      // Large Title
    displayMedium = ios(34, 41, 0.40, FontWeight.Bold),
    displaySmall = ios(34, 41, 0.40, FontWeight.Bold),
    headlineLarge = ios(34, 41, 0.40, FontWeight.Bold),
    headlineMedium = ios(28, 34, 0.38, FontWeight.Bold),    // Title 1
    headlineSmall = ios(22, 28, -0.26, FontWeight.Bold),    // Title 2
    titleLarge = ios(20, 25, -0.45, FontWeight.SemiBold),   // Title 3
    titleMedium = ios(17, 22, -0.43, FontWeight.SemiBold),  // Headline
    titleSmall = ios(15, 20, -0.23, FontWeight.SemiBold),   // Subheadline, emphasised
    bodyLarge = ios(17, 22, -0.43),                         // Body
    bodyMedium = ios(15, 20, -0.23),                        // Subheadline
    bodySmall = ios(13, 18, -0.08),                         // Footnote
    labelLarge = ios(17, 22, -0.43, FontWeight.SemiBold),   // Button titles
    labelMedium = ios(13, 18, -0.08),                       // Footnote
    labelSmall = ios(11, 13, 0.06)                          // Caption 2
)

/** Continuous-looking corners at iOS radii. Buttons stay capsules (Material's default). */
internal val IosShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp), // menus
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(IosMetrics.CardCorner),
    large = RoundedCornerShape(IosMetrics.CardCorner),
    extraLarge = RoundedCornerShape(28.dp)
)

internal object IosMetrics {
    /** Corner radius of inset-grouped sections and cards. */
    val CardCorner = 20.dp
    val RowHorizontalPadding = 16.dp
    val RowMinHeight = 44.dp
    /** Separators start at the text, not the cell edge. */
    val SeparatorInset = 16.dp
    val Hairline = 0.5.dp
}

/**
 * The iOS pressed state for custom tappable rows: a faint wash over the cell while the finger is
 * down, instead of Material's ripple spreading from the touch point.
 */
internal class IosHighlightIndication(private val color: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        HighlightNode(interactionSource, color)

    override fun equals(other: Any?) = other is IosHighlightIndication && other.color == color
    override fun hashCode() = color.hashCode()

    private class HighlightNode(
        private val interactionSource: InteractionSource,
        private val color: Color
    ) : Modifier.Node(), DrawModifierNode {
        private var pressed = false

        override fun onAttach() {
            coroutineScope.launch {
                var presses = 0
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> presses++
                        is PressInteraction.Release, is PressInteraction.Cancel -> presses--
                    }
                    val now = presses > 0
                    if (now != pressed) {
                        pressed = now
                        invalidateDraw()
                    }
                }
            }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            if (pressed) drawRect(color)
        }
    }
}
