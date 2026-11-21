package ch.bfh.anchor.ui.crisis

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.StringResource
import kotlinx.coroutines.delay

enum class BreathPhase(val labelRes: StringResource, val duration: Long) {
    Inhale(Res.string.crisis_breath_phase_inhale, 4000),
    Hold(Res.string.crisis_breath_phase_hold, 4000),
    Exhale(Res.string.crisis_breath_phase_exhale, 4000)
}
@Composable
fun BreathingScreen(onFinished: () -> Unit) {
    var phase by remember { mutableStateOf(BreathPhase.Inhale) }
    var cyclesLeft by remember { mutableIntStateOf(3) }
    val scale = remember { Animatable(1f) }



    LaunchedEffect(Unit) {
        while (cyclesLeft > 0) {
            // Inhale
            phase = BreathPhase.Inhale
            scale.animateTo(1.4f, animationSpec = tween(4000, easing = EaseInOut))

            // Hold
            phase = BreathPhase.Hold
            delay(4000)

            // Exhale
            phase = BreathPhase.Exhale
            scale.animateTo(1f, animationSpec = tween(4000, easing = EaseInOut))

            cyclesLeft--
        }
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(Res.string.crisis_breathe_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(scale.value)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(scale.value)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        CircleShape
                    )
            )
            Text(
                text = stringResource(phase.labelRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = pluralStringResource(
                Res.plurals.crisis_breaths_remaining,
                cyclesLeft,
                cyclesLeft
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onFinished,
            modifier = Modifier.padding(bottom = 40.dp)
        ) {
            Text(
                stringResource(Res.string.common_skip),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}