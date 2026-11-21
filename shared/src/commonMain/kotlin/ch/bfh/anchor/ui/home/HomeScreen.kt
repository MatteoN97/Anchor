package ch.bfh.anchor.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import ch.bfh.anchor.platform.daysSince
import ch.bfh.anchor.settings.SettingsManager
import ch.bfh.anchor.ui.theme.AnchorTheme
import ch.bfh.anchor.viewmodel.AnchorViewModel

@Composable
fun HomeScreen(
    vm: AnchorViewModel,
    settingsManager: SettingsManager,
    onSosClick: () -> Unit
) {
    val userName by settingsManager.userName.collectAsState()

    val tools by vm.tools.collectAsState()
    val notes by vm.notes.collectAsState()
    val crisisCheckIns by vm.crisisCheckIns.collectAsState()

    val lastCrisisDays: Int? = remember(crisisCheckIns) {
        crisisCheckIns.maxByOrNull { it.date }?.let { daysSince(it.date) }
    }
    val lastCrisisText: String? = when (lastCrisisDays) {
        null -> null
        0 -> stringResource(Res.string.home_last_crisis_today)
        1 -> stringResource(Res.string.home_last_crisis_yesterday)
        else -> pluralStringResource(
            Res.plurals.home_last_crisis_days,
            lastCrisisDays,
            lastCrisisDays
        )
    }

    HomeScreenContent(
        userName = userName,
        toolsCount = tools.size,
        notesCount = notes.size,
        crisisCount = crisisCheckIns.size,
        lastCrisisText = lastCrisisText,
        onSosClick = onSosClick
    )
}

@Composable
private fun HomeScreenContent(
    userName: String,
    toolsCount: Int,
    notesCount: Int,
    crisisCount: Int,
    lastCrisisText: String?,
    onSosClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (userName.isNotBlank()) {
                        stringResource(Res.string.home_greeting_with_name, userName)
                    } else {
                        stringResource(Res.string.home_tagline)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = stringResource(Res.string.home_stat_tools),
                    value = toolsCount,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(Res.string.home_stat_sessions),
                    value = crisisCount,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(Res.string.home_stat_notes),
                    value = notesCount,
                    modifier = Modifier.weight(1f)
                )
            }

            if (lastCrisisText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = lastCrisisText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.home_overwhelmed),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onSosClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = stringResource(Res.string.home_sos_button),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AnchorTheme {
        HomeScreenContent(
            userName = "Matteo",
            toolsCount = 4,
            notesCount = 12,
            crisisCount = 2,
            lastCrisisText = "Last crisis session: 3 days ago",
            onSosClick = {}
        )
    }
}
