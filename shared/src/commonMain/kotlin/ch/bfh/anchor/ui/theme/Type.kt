package ch.bfh.anchor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun style(size: Int, lineHeight: Int, tracking: Double, weight: FontWeight = FontWeight.Normal) =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = tracking.sp
    )

/**
 * Android type scale. Material's roles and sizes, but with heavier, slightly tightened headings
 * and roomier body text: the screens are mostly short headings over a little reading, and the
 * journal is long-form writing.
 */
val Typography = Typography(
    displayLarge = style(52, 60, -0.5, FontWeight.SemiBold),
    displayMedium = style(42, 50, -0.4, FontWeight.SemiBold),
    displaySmall = style(34, 42, -0.3, FontWeight.SemiBold),
    headlineLarge = style(30, 38, -0.3, FontWeight.SemiBold),
    headlineMedium = style(26, 34, -0.2, FontWeight.SemiBold),
    headlineSmall = style(22, 30, -0.1, FontWeight.SemiBold),
    titleLarge = style(20, 28, 0.0, FontWeight.SemiBold),
    titleMedium = style(16, 24, 0.1, FontWeight.SemiBold),
    titleSmall = style(14, 20, 0.1, FontWeight.SemiBold),
    bodyLarge = style(16, 26, 0.15),
    bodyMedium = style(14, 21, 0.2),
    bodySmall = style(12, 17, 0.3),
    labelLarge = style(14, 20, 0.1, FontWeight.SemiBold),
    labelMedium = style(12, 16, 0.4, FontWeight.Medium),
    labelSmall = style(11, 16, 0.5, FontWeight.Medium)
)

/** Soft, generous corners: cards read as pebbles rather than tiles. */
val AnchorShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)
