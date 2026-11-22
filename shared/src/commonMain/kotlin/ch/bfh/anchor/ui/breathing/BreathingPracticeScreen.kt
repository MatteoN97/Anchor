package ch.bfh.anchor.ui.breathing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.BreathingPattern
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.ui.adaptive.AnchorButton
import androidx.compose.foundation.shape.CircleShape
import ch.bfh.anchor.ui.components.CloseButton
import ch.bfh.anchor.ui.components.calmBackdrop
import ch.bfh.anchor.ui.adaptive.AnchorTextButton
import org.jetbrains.compose.resources.stringResource

private enum class PracticeStep { Running, Finished }

/**
 * Full-screen breathing practice, reachable from Home outside of a crisis. It starts right away
 * with [pattern], the one chosen in Settings, which the crisis flow uses too.
 */
@Composable
fun BreathingPracticeScreen(pattern: BreathingPattern, onDismiss: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(PracticeStep.Running) }
    // Bumped by "A few more" so the exercise restarts from scratch.
    var round by rememberSaveable { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().calmBackdrop(strength = 0.8f, reach = 0.6f)) {
        Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
            when (step) {
                PracticeStep.Running -> key(round) {
                    BreathingExercise(
                        pattern = pattern,
                        cycles = pattern.practiceCycles,
                        title = stringResource(pattern.nameRes),
                        onComplete = { step = PracticeStep.Finished }
                    )
                }

                PracticeStep.Finished -> FinishedContent(
                    onDone = onDismiss,
                    onMore = {
                        round++
                        step = PracticeStep.Running
                    }
                )
            }

            CloseButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun FinishedContent(onDone: () -> Unit, onMore: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.breathing_finished_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.breathing_finished_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))
        AnchorButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape
        ) {
            Text(stringResource(Res.string.common_done), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        AnchorTextButton(onClick = onMore) {
            Text(stringResource(Res.string.breathing_more))
        }
    }
}
