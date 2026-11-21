package ch.bfh.anchor.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.common_dismiss
import ch.bfh.anchor.ui.adaptive.AnchorIconButton
import ch.bfh.anchor.ui.icons.AnchorIcons
import org.jetbrains.compose.resources.stringResource

/**
 * Building blocks of the calm look, shared by Home, the crisis flow, breathing and the note
 * editor. Everything reads MaterialTheme, so it follows light/dark, iOS and Material You.
 */

/**
 * A soft wash of [tint] at the top of the page fading into the background, drawn behind the
 * content. It gives full-screen pages depth without a hard header edge.
 */
@Composable
fun Modifier.calmBackdrop(
    tint: Color = MaterialTheme.colorScheme.primaryContainer,
    strength: Float = 0.75f,
    reach: Float = 0.55f
): Modifier {
    val background = MaterialTheme.colorScheme.background
    return drawBehind {
        drawRect(background)
        drawRect(
            Brush.verticalGradient(
                0f to tint.copy(alpha = strength),
                reach to tint.copy(alpha = 0f),
                endY = size.height
            )
        )
    }
}

/** An icon centred in a tinted circle: the leading mark of cards and tiles. */
@Composable
fun IconBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
    }
}

/** The round "X" at the top of full-screen flows. */
@Composable
fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnchorIconButton(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AnchorIcons.Close,
                contentDescription = stringResource(Res.string.common_dismiss),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Where the user is in a short multi-step flow: one dot per step, the current one stretched into
 * a pill. [description] is read by screen readers instead of the dots.
 */
@Composable
fun StepDots(count: Int, current: Int, description: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val active = index == current
            val width by animateDpAsState(if (active) 22.dp else 7.dp)
            val color by animateColorAsState(
                when {
                    active -> MaterialTheme.colorScheme.primary
                    index < current -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                }
            )
            Box(
                Modifier
                    .height(7.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * The fill for a raised card on the page background: white on the warm light page, a lifted grey
 * in dark mode, the white cell on iOS.
 */
@Composable
fun calmCardColor(): Color {
    val colors = MaterialTheme.colorScheme
    return when {
        ch.bfh.anchor.ui.theme.LocalIsIos.current -> colors.surfaceVariant
        colors.background.luminance() < 0.5f -> colors.surfaceContainer
        else -> colors.surfaceContainerLowest
    }
}
