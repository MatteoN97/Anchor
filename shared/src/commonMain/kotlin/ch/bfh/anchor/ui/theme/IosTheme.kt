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
 * iOS system colours (UIKit's dynamic colours, resolved for light and dark) mapped onto the
 * Material slots the screens already read. The mapping, so screens can stay platform-agnostic:
 *
 * - background / surface         → systemGroupedBackground (the grey page behind sections)
 * - surfaceVariant / containers  → secondarySystemGroupedBackground (the white cells and cards)
 * - secondaryContainer           → tertiarySystemFill (segmented control track, chips)
 * - onSurfaceVariant             → secondaryLabel
 * - outlineVariant               → separator
 */
internal object IosColors {
    val IndigoLight = Color(0xFF5856D6)
    val IndigoDark = Color(0xFF5E5CE6)
    val RedLight = Color(0xFFFF3B30)
    val RedDark = Color(0xFFFF453A)
    val GreenLight = Color(0xFF34C759)
    val GreenDark = Color(0xFF30D158)
    val Gray = Color(0xFF8E8E93)
}

internal val IosLightColorScheme: ColorScheme = lightColorScheme(
    primary = IosColors.IndigoLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E6FA),
    onPrimaryContainer = IosColors.IndigoLight,
    secondary = IosColors.Gray,
    onSecondary = Color.White,
    secondaryContainer = Color(0x1F767680),
    onSecondaryContainer = Color.Black,
    tertiary = IosColors.GreenLight,
    onTertiary = Color.White,
    background = Color(0xFFF2F2F7),
    onBackground = Color.Black,
    surface = Color(0xFFF2F2F7),
    onSurface = Color.Black,
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0x993C3C43),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2F2F7),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    outline = Color(0xFFC6C6C8),
    outlineVariant = Color(0x4A3C3C43),
    error = IosColors.RedLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E3),
    onErrorContainer = IosColors.RedLight,
    scrim = Color(0x66000000)
)

internal val IosDarkColorScheme: ColorScheme = darkColorScheme(
    primary = IosColors.IndigoDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A2A55),
    onPrimaryContainer = Color(0xFFB4B3FF),
    secondary = IosColors.Gray,
    onSecondary = Color.White,
    secondaryContainer = Color(0x3D767680),
    onSecondaryContainer = Color.White,
    tertiary = IosColors.GreenDark,
    onTertiary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0x99EBEBF5),
    surfaceContainerLowest = Color(0xFF1C1C1E),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF2C2C2E),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF636366), // selected segment in dark mode
    outline = Color(0xFF38383A),
    outlineVariant = Color(0x99545458),
    error = IosColors.RedDark,
    onError = Color.White,
    errorContainer = Color(0xFF3A1513),
    onErrorContainer = IosColors.RedDark,
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
