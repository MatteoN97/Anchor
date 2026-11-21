package ch.bfh.anchor.ui.crisis

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.HelpContact
import ch.bfh.anchor.platform.rememberPhoneDialer
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val checkInOptions: List<Triple<Int, String, StringResource>> = listOf(
    Triple(1, "😊", Res.string.crisis_checkin_great),
    Triple(2, "🙂", Res.string.crisis_checkin_better),
    Triple(3, "😐", Res.string.crisis_checkin_okay),
    Triple(4, "😔", Res.string.crisis_checkin_rough),
    Triple(5, "😢", Res.string.crisis_checkin_not_great)
)

@Composable
fun CrisisCheckInScreen(
    crisisLine: HelpContact,
    trustedContact: HelpContact?,
    onFinished: (Int) -> Unit,
    onNeedMoreHelp: () -> Unit
) {
    var selectedRating by remember { mutableStateOf<Int?>(null) }
    var showNeedHelp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        if (showNeedHelp) {
            NeedMoreHelpContent(
                crisisLine = crisisLine,
                trustedContact = trustedContact,
                onGoAgain = onNeedMoreHelp,
                onDone = { onFinished(selectedRating ?: 5) }
            )
        } else {
            CheckInContent(
                selectedRating = selectedRating,
                onRatingSelected = { selectedRating = it },
                onDone = {
                    val rating = selectedRating ?: 3
                    if (rating >= 4) showNeedHelp = true
                    else onFinished(rating)
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CheckInContent(
    selectedRating: Int?,
    onRatingSelected: (Int) -> Unit,
    onDone: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.crisis_checkin_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(Res.string.crisis_checkin_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            checkInOptions.forEach { (rating, emoji, labelRes) ->
                val selected = selectedRating == rating
                Surface(
                    onClick = { onRatingSelected(rating) },
                    modifier = Modifier.weight(1f),
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = emoji, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Button(
            onClick = onDone,
            enabled = selectedRating != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = stringResource(Res.string.common_done),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun NeedMoreHelpContent(
    crisisLine: HelpContact,
    trustedContact: HelpContact?,
    onGoAgain: () -> Unit,
    onDone: () -> Unit
) {
    val dialPhoneNumber = rememberPhoneDialer()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.crisis_help_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(Res.string.crisis_help_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Go again
            Surface(
                onClick = onGoAgain,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.crisis_help_again_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(Res.string.crisis_help_again_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            trustedContact?.let { contact ->
                CallCard(
                    title = if (contact.name.isNotBlank()) {
                        stringResource(Res.string.crisis_call_trusted_title, contact.name)
                    } else {
                        stringResource(Res.string.crisis_call_trusted_fallback)
                    },
                    subtitle = contact.number,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = { dialPhoneNumber(contact.number) }
                )
            }

            CallCard(
                title = stringResource(Res.string.crisis_call_line_title),
                subtitle = if (crisisLine.name.isNotBlank()) "${crisisLine.name} — ${crisisLine.number}"
                else crisisLine.number,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                onClick = { dialPhoneNumber(crisisLine.number) }
            )

            TextButton(
                onClick = onDone,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = stringResource(Res.string.crisis_help_done),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CallCard(
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}
