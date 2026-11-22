package ch.bfh.anchor.ui.breathing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.BreathPhase
import ch.bfh.anchor.model.BreathingPattern
import ch.bfh.anchor.platform.KeepScreenOn
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.breaths_remaining
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val MinScale = 1f
private const val MaxScale = 1.4f

/**
 * Guides the user through [cycles] rounds of [pattern], then calls [onComplete].
 * Used both inside the crisis flow and for standalone practice.
 */
@Composable
fun BreathingExercise(
    pattern: BreathingPattern,
    cycles: Int,
    title: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    bottomAction: @Composable () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val currentOnComplete by rememberUpdatedState(onComplete)

    var phase by remember(pattern) { mutableStateOf(pattern.steps.first().phase) }
    var cyclesLeft by remember(pattern, cycles) { mutableIntStateOf(cycles) }
    val scale = remember { Animatable(MinScale) }
    val phaseProgress = remember { Animatable(0f) }

    KeepScreenOn()

    LaunchedEffect(pattern, cycles) {
        scale.snapTo(MinScale)
        repeat(cycles) {
            for (step in pattern.steps) {
                phase = step.phase
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                phaseProgress.snapTo(0f)
                coroutineScope {
                    launch {
                        phaseProgress.animateTo(
                            1f,
                            tween(step.durationMs, easing = LinearEasing)
                        )
                    }
                    when (step.phase) {
                        BreathPhase.Inhale ->
                            scale.animateTo(MaxScale, tween(step.durationMs, easing = EaseInOut))
                        BreathPhase.Exhale ->
                            scale.animateTo(MinScale, tween(step.durationMs, easing = EaseInOut))
                        BreathPhase.HoldIn, BreathPhase.HoldOut ->
                            delay(step.durationMs.toLong())
                    }
                }
            }
            cyclesLeft--
        }
        currentOnComplete()
    }

    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(300.dp)
        ) {
            // Progress through the current phase, so holds don't look frozen.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                drawArc(
                    color = primary.copy(alpha = 0.12f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )
                drawArc(
                    color = primary.copy(alpha = 0.6f),
                    startAngle = -90f,
                    sweepAngle = 360f * phaseProgress.value,
                    useCenter = false,
                    style = stroke
                )
            }
            val inner = MaterialTheme.colorScheme.primaryContainer
            // iOS secondaryContainer is a translucent control fill; use the palette's lavender there.
            val innerEnd = MaterialTheme.colorScheme.secondaryContainer.takeIf { it.alpha > 0.99f }
                ?: MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f).compositeOver(inner)
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .scale(scale.value)
                    .background(
                        Brush.radialGradient(listOf(primary.copy(alpha = 0.16f), primary.copy(alpha = 0.02f))),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(scale.value)
                    .background(
                        Brush.linearGradient(listOf(inner, innerEnd)),
                        CircleShape
                    )
            )
            Text(
                text = stringResource(phase.labelRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = pluralStringResource(Res.plurals.breaths_remaining, cyclesLeft, cyclesLeft),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(bottom = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            bottomAction()
        }
    }
}
