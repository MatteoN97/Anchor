package ch.bfh.anchor.ui.crisis

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import ch.bfh.anchor.ui.icons.AnchorIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.stringResource
import ch.bfh.anchor.model.Feeling
import ch.bfh.anchor.viewmodel.AnchorViewModel

enum class CrisisStep { FeelingPicker, Breathing, Suggestions, CheckIn }

@Composable
fun CrisisScreen(
    onDismiss: () -> Unit,
    vm: AnchorViewModel
) {
    var step by remember { mutableStateOf(CrisisStep.FeelingPicker) }
    var selectedFeeling by remember { mutableStateOf<Feeling?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (step) {
                CrisisStep.FeelingPicker -> FeelingPickerScreen(
                    onSelected = { feeling ->
                        selectedFeeling = feeling
                        step = if (feeling.skipBreathing) CrisisStep.Suggestions
                        else CrisisStep.Breathing
                    }
                )

                CrisisStep.Breathing -> BreathingScreen(
                    onFinished = { step = CrisisStep.Suggestions }
                )

                CrisisStep.Suggestions -> SuggestedToolsScreen(
                    feeling = selectedFeeling ?: Feeling.ANXIOUS,
                    viewModel = vm,
                    onFinished = { step = CrisisStep.CheckIn },
                    onToolAdded = { tool -> vm.addTool(tool.name, tool.category, tool.description) }
                )

                CrisisStep.CheckIn -> CrisisCheckInScreen(
                    onFinished = { rating ->
                        vm.addCrisisCheckIn(selectedFeeling ?: Feeling.ANXIOUS, rating)
                        onDismiss()
                    },
                    onNeedMoreHelp = { step = CrisisStep.FeelingPicker }
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        imageVector = AnchorIcons.Close,
                        contentDescription = stringResource(Res.string.common_dismiss),
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
