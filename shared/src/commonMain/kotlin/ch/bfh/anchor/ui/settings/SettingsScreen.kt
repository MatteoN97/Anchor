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
import ch.bfh.anchor.model.BreathingPattern
import ch.bfh.anchor.model.CrisisLines
import ch.bfh.anchor.model.HelpContact
import ch.bfh.anchor.platform.rememberDeviceRegion
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsManager: SettingsManager) {
    val userName by settingsManager.userName.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(userName) }
    var crisisPattern by remember { mutableStateOf(settingsManager.crisisBreathingPattern) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.settings_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
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
                            settingsManager.setUserName(tempName)
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.settings_crisis_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.selectableGroup()) {
                Text(
                    text = stringResource(Res.string.settings_crisis_breathing_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BreathingPattern.entries.forEach { pattern ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = pattern == crisisPattern,
                                onClick = {
                                    settingsManager.crisisBreathingPattern = pattern
                                    crisisPattern = pattern
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = pattern == crisisPattern, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(pattern.nameRes),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(pattern.descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(Res.string.settings_crisis_breathing_helper),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            HelpContactsSection(settingsManager)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HelpContactsSection(settingsManager: SettingsManager) {
    val region = rememberDeviceRegion()
    val regionalLine = CrisisLines.forRegion(region)
    var customLine by remember { mutableStateOf(settingsManager.customCrisisLine) }
    var trusted by remember { mutableStateOf(settingsManager.trustedContact) }

    Text(
        text = stringResource(Res.string.settings_contacts_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )

    val custom = customLine
    ContactSetting(
        label = stringResource(Res.string.settings_crisis_line_label),
        current = custom ?: (regionalLine ?: CrisisLines.fallback).contact,
        caption = when {
            custom != null -> stringResource(Res.string.settings_crisis_line_custom)
            regionalLine != null ->
                stringResource(Res.string.settings_crisis_line_auto, regionalLine.countryName)
            else -> stringResource(
                Res.string.settings_crisis_line_auto_fallback,
                CrisisLines.fallback.countryName
            )
        },
        emptyText = "",
        namePlaceholder = stringResource(Res.string.settings_crisis_line_name_placeholder),
        clearLabel = if (custom != null) stringResource(Res.string.settings_crisis_line_reset) else null,
        onSave = {
            settingsManager.customCrisisLine = it
            customLine = settingsManager.customCrisisLine
        },
        onClear = {
            settingsManager.customCrisisLine = null
            customLine = null
        }
    )

    ContactSetting(
        label = stringResource(Res.string.settings_trusted_label),
        current = trusted,
        caption = null,
        emptyText = stringResource(Res.string.settings_trusted_not_set),
        namePlaceholder = stringResource(Res.string.settings_trusted_name_placeholder),
        clearLabel = if (trusted != null) stringResource(Res.string.settings_contact_remove) else null,
        onSave = {
            settingsManager.trustedContact = it
            trusted = settingsManager.trustedContact
        },
        onClear = {
            settingsManager.trustedContact = null
            trusted = null
        }
    )

    Text(
        text = stringResource(Res.string.settings_contacts_helper),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Shows one contact with an edit button; editing swaps in name and number fields. */
@Composable
private fun ContactSetting(
    label: String,
    current: HelpContact?,
    caption: String?,
    emptyText: String,
    namePlaceholder: String,
    clearLabel: String?,
    onSave: (HelpContact) -> Unit,
    onClear: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isEditing) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.settings_contact_name)) },
                placeholder = { Text(namePlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                label = { Text(stringResource(Res.string.settings_contact_number)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = { isEditing = false }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.common_cancel))
                }
                Button(
                    onClick = {
                        onSave(HelpContact(name = name.trim(), number = number.trim()))
                        isEditing = false
                    },
                    enabled = number.any { it.isDigit() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.common_save))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (current != null) {
                        if (current.name.isNotBlank()) {
                            Text(text = current.name, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            text = current.number,
                            style = if (current.name.isNotBlank()) MaterialTheme.typography.bodyMedium
                            else MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Text(
                            text = emptyText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (caption != null) {
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = {
                    name = current?.name.orEmpty()
                    number = current?.number.orEmpty()
                    isEditing = true
                }) {
                    Icon(
                        AnchorIcons.Edit,
                        contentDescription = stringResource(Res.string.settings_contact_edit, label),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (clearLabel != null) {
                TextButton(onClick = onClear) { Text(clearLabel) }
            }
        }
    }
}
