package ch.bfh.anchor.ui.settings
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
import ch.bfh.anchor.settings.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsManager: SettingsManager) {
    var userName by remember { mutableStateOf(settingsManager.userName) }
    var isEditing by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(settingsManager.userName) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.settings_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.settings_profile_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (isEditing) {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text(stringResource(Res.string.settings_name_label)) },
                    placeholder = { Text(stringResource(Res.string.settings_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            tempName = userName
                            isEditing = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.common_cancel))
                    }
                    Button(
                        onClick = {
                            settingsManager.userName = tempName.trim()
                            userName = tempName.trim()
                            isEditing = false
                        },
                        enabled = tempName.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.common_save))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.settings_name_field_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (userName.isNotBlank()) userName else stringResource(Res.string.settings_name_not_set),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    IconButton(onClick = {
                        tempName = userName
                        isEditing = true
                    }) {
                        Icon(
                            AnchorIcons.Edit,
                            contentDescription = stringResource(Res.string.settings_edit_name),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = stringResource(Res.string.settings_name_helper_updated),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}