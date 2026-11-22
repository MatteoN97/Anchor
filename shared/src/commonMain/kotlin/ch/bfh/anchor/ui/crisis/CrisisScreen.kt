package ch.bfh.anchor.ui.crisis

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.stringResource
import ch.bfh.anchor.model.BreathingPattern
import ch.bfh.anchor.model.Feeling
import ch.bfh.anchor.model.HelpContact
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import ch.bfh.anchor.ui.components.CloseButton
import ch.bfh.anchor.ui.components.StepDots
import ch.bfh.anchor.ui.components.calmBackdrop
import ch.bfh.anchor.ui.adaptive.AnchorTextButton
import ch.bfh.anchor.ui.breathing.BreathingExercise
import ch.bfh.anchor.viewmodel.AnchorViewModel

enum class CrisisStep { FeelingPicker, Breathing, Suggestions, CheckIn }

@Composable
fun CrisisScreen(
    onDismiss: () -> Unit,
    vm: AnchorViewModel,
    breathingPattern: BreathingPattern,
    crisisLine: HelpContact,
    trustedContact: HelpContact?
) {
    // Saveable, so rotating the phone mid-crisis does not send the user back to the first step.
    var step by rememberSaveable { mutableStateOf(CrisisStep.FeelingPicker) }
    var selectedFeeling by rememberSaveable { mutableStateOf<Feeling?>(null) }

    // Breathing is skipped for some feelings, so the flow has three or four steps.
    val steps = if (selectedFeeling?.skipBreathing == true) {
        listOf(CrisisStep.FeelingPicker, CrisisStep.Suggestions, CrisisStep.CheckIn)
    } else CrisisStep.entries
    val stepIndex = steps.indexOf(step).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .calmBackdrop(strength = 0.8f, reach = 0.6f)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepDots(
                    count = steps.size,
                    current = stepIndex,
                    description = stringResource(Res.string.crisis_step_progress, stepIndex + 1, steps.size)
                )
                Spacer(Modifier.weight(1f))
                CloseButton(onClick = onDismiss)
            }

            // Steps fade and drift in rather than snapping, which reads as calmer.
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(tween(350, delayMillis = 90)) + slideInVertically(tween(350)) { it / 12 }) togetherWith
                        fadeOut(tween(150))
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "crisis-step"
            ) { current ->
                Box(Modifier.fillMaxSize()) {
                    when (current) {
                        CrisisStep.FeelingPicker -> FeelingPickerScreen(
                            onSelected = { feeling ->
                                selectedFeeling = feeling
                                step = if (feeling.skipBreathing) CrisisStep.Suggestions
                                else CrisisStep.Breathing
                            }
                        )

                        CrisisStep.Breathing -> BreathingExercise(
                            pattern = breathingPattern,
                            cycles = 3,
                            title = stringResource(Res.string.crisis_breathe_title),
                            onComplete = { step = CrisisStep.Suggestions },
                            bottomAction = {
                                AnchorTextButton(onClick = { step = CrisisStep.Suggestions }) {
                                    Text(
                                        stringResource(Res.string.common_skip),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )

                        CrisisStep.Suggestions -> SuggestedToolsScreen(
                            feeling = selectedFeeling ?: Feeling.ANXIOUS,
                            viewModel = vm,
                            onFinished = { step = CrisisStep.CheckIn },
                            onToolAdded = { tool -> vm.addTool(tool.name, tool.category, tool.description) }
                        )

                        CrisisStep.CheckIn -> CrisisCheckInScreen(
                            crisisLine = crisisLine,
                            trustedContact = trustedContact,
                            onRated = { rating ->
                                vm.addCrisisCheckIn(selectedFeeling ?: Feeling.ANXIOUS, rating)
                            },
                            onClose = onDismiss,
                            onNeedMoreHelp = { step = CrisisStep.FeelingPicker }
                        )
                    }
                }
            }

            // Always within reach, whatever step the flow is on. Its own strip rather than an
            // overlay, so it never covers a step's Continue, Skip or Done button.
            CallForHelpButton(
                crisisLine = crisisLine,
                trustedContact = trustedContact,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 16.dp)
            )
        }
    }
}
