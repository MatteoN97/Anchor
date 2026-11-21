package ch.bfh.anchor.ui.adaptive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import ch.bfh.anchor.ui.theme.LocalIsIos

/** UIKit buttons fade while held and spring back on release. */
@Composable
internal fun Modifier.pressDim(source: MutableInteractionSource, pressedAlpha: Float = 0.4f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed) pressedAlpha else 1f,
        animationSpec = tween(durationMillis = if (pressed) 0 else 250)
    )
    return graphicsLayer { this.alpha = alpha }
}

/**
 * The prominent, filled button. A capsule on iOS that dims while pressed; a Material button with
 * [shape] on Android. Leave the colours unspecified for the primary colour.
 */
@Composable
fun AnchorButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    shape: Shape = ButtonDefaults.shape,
    content: @Composable RowScope.() -> Unit
) {
    val isIos = LocalIsIos.current
    val source = remember { MutableInteractionSource() }
    val colors = ButtonDefaults.buttonColors(
        containerColor = containerColor.takeOrElse { MaterialTheme.colorScheme.primary },
        contentColor = contentColor.takeOrElse { MaterialTheme.colorScheme.onPrimary }
    )
    Button(
        onClick = onClick,
        modifier = if (isIos) modifier.pressDim(source, pressedAlpha = 0.7f) else modifier,
        enabled = enabled,
        shape = if (isIos) CircleShape else shape,
        colors = colors,
        interactionSource = source,
        content = content
    )
}

/** A text-only button: a plain tinted label on iOS, Material's TextButton on Android. */
@Composable
fun AnchorTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    emphasized: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val isIos = LocalIsIos.current
    val source = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        modifier = if (isIos) modifier.pressDim(source, pressedAlpha = 0.3f) else modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = source
    ) {
        if (isIos) {
            // iOS bar buttons are Body-sized, regular weight; the confirming one is semibold.
            ProvideTextStyle(
                MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal
                )
            ) { content() }
        } else {
            content()
        }
    }
}

@Composable
fun AnchorIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val isIos = LocalIsIos.current
    val source = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        modifier = if (isIos) modifier.pressDim(source, pressedAlpha = 0.3f) else modifier,
        enabled = enabled,
        interactionSource = source,
        content = content
    )
}

/**
 * A tappable card. On iOS it greys over while pressed, like a table cell; on Android it ripples.
 */
@Composable
fun AnchorCardButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = Color.Unspecified,
    shape: Shape = MaterialTheme.shapes.medium,
    border: BorderStroke? = null,
    selected: Boolean? = null,
    content: @Composable () -> Unit
) {
    val isIos = LocalIsIos.current
    val source = remember { MutableInteractionSource() }
    val resolvedContentColor = contentColor.takeOrElse { MaterialTheme.colorScheme.onSurface }
    val surfaceModifier = if (isIos) {
        modifier.clip(shape).indication(source, LocalIndication.current)
    } else {
        modifier
    }
    if (selected != null) {
        Surface(
            selected = selected,
            onClick = onClick,
            modifier = surfaceModifier,
            shape = shape,
            color = color,
            contentColor = resolvedContentColor,
            border = border,
            interactionSource = source,
            content = content
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = surfaceModifier,
            shape = shape,
            color = color,
            contentColor = resolvedContentColor,
            border = border,
            interactionSource = source,
            content = content
        )
    }
}

private inline fun Color.takeOrElse(block: () -> Color): Color =
    if (this == Color.Unspecified) block() else this
