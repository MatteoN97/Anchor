package ch.bfh.anchor.ui.mood

import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.bfh.anchor.model.MoodScale
import ch.bfh.anchor.ui.components.calmCardColor
import ch.bfh.anchor.ui.components.fade
import kotlinx.coroutines.launch

/**
 * The same five faces, labels and order everywhere Anchor asks for a mood.
 *
 * Every tap answers with a small bounce and a light haptic, including a second tap on the face
 * that is already selected: the tracker stores each tap as a new check-in, so a tap that changed
 * nothing on screen looked like it had been ignored.
 */
@Composable
fun MoodChoices(
    selectedScore: Int?,
    onScoreSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MoodScale.displayScores.forEach { score ->
            val selected = selectedScore == score
            val scale = remember { Animatable(1f) }
            val container by animateColorAsState(
                if (selected) colors.primary else calmCardColor(),
                animationSpec = tween(180)
            )
            Surface(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    scope.launch {
                        scale.animateTo(0.88f, tween(70))
                        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                    onScoreSelected(score)
                },
                modifier = Modifier.weight(1f).graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
                color = container,
                shape = MaterialTheme.shapes.medium,
                border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant.fade(0.6f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(MoodScale.emoji(score), fontSize = 24.sp)
                    Text(
                        stringResource(MoodScale.labelRes(score)),
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        color = if (selected) colors.onPrimary else colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}
