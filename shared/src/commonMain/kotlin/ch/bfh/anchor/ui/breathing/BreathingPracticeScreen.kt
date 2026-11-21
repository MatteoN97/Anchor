package ch.bfh.anchor.ui.breathing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.BreathingPattern
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.ui.adaptive.AnchorButton
import ch.bfh.anchor.ui.adaptive.AnchorCardButton
import androidx.compose.foundation.shape.CircleShape
import ch.bfh.anchor.ui.components.CloseButton
import ch.bfh.anchor.ui.components.calmBackdrop
import ch.bfh.anchor.ui.components.calmCardColor
import ch.bfh.anchor.ui.adaptive.AnchorTextButton
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private enum class PracticeStep { Choose, Running, Finished }

/** Full-screen breathing practice, reachable from Home outside of a crisis. */
@Composable
fun BreathingPracticeScreen(onDismiss: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(PracticeStep.Choose) }
    var pattern by rememberSaveable { mutableStateOf(BreathingPattern.Calm) }
    // Bumped by "A few more" so the exercise restarts from scratch.
    var round by rememberSaveable { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().calmBackdrop(strength = 0.8f, reach = 0.6f)) {
        Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
            when (step) {
                PracticeStep.Choose -> PatternChooser(
                    selected = pattern,
                    onSelect = { pattern = it },
                    onStart = { step = PracticeStep.Running }
                )

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
private fun PatternChooser(
    selected: BreathingPattern,
    onSelect: (BreathingPattern) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.breathing_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(Res.string.breathing_choose_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BreathingPattern.entries.forEach { pattern ->
                val isSelected = pattern == selected
                AnchorCardButton(
                    selected = isSelected,
                    onClick = { onSelect(pattern) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else calmCardColor(),
                    shape = MaterialTheme.shapes.large,
                    border = if (isSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else null
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(pattern.nameRes),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = pluralStringResource(
                                    Res.plurals.breathing_breaths_count,
                                    pattern.practiceCycles,
                                    pattern.practiceCycles
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = stringResource(pattern.descriptionRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnchorButton(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape
        ) {
            Text(
                text = stringResource(Res.string.breathing_start),
                style = MaterialTheme.typography.titleMedium
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
