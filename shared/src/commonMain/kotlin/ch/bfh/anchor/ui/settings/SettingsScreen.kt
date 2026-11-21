package ch.bfh.anchor.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.BreathingPattern
import ch.bfh.anchor.model.CrisisLines
import ch.bfh.anchor.model.HelpContact
import ch.bfh.anchor.platform.rememberDeviceRegion
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.settings.SettingsManager
import ch.bfh.anchor.ui.adaptive.AdaptiveActionRow
import ch.bfh.anchor.ui.adaptive.AdaptiveListRow
import ch.bfh.anchor.ui.adaptive.AdaptivePageScaffold
import ch.bfh.anchor.ui.adaptive.AdaptiveSheet
import ch.bfh.anchor.ui.adaptive.AdaptiveTextField
import ch.bfh.anchor.ui.adaptive.Checkmark
import ch.bfh.anchor.ui.adaptive.adaptiveSection
import ch.bfh.anchor.ui.adaptive.largeTitle
import ch.bfh.anchor.ui.icons.AnchorIcons
import ch.bfh.anchor.ui.theme.LocalIsIos
import org.jetbrains.compose.resources.stringResource

/** Which edit sheet is open. */
private enum class EditTarget { Name, CrisisLine, TrustedContact }

@Composable
fun SettingsScreen(settingsManager: SettingsManager) {
    val userName by settingsManager.userName.collectAsState()
    var crisisPattern by remember { mutableStateOf(settingsManager.crisisBreathingPattern) }
    var customLine by remember { mutableStateOf(settingsManager.customCrisisLine) }
    var trusted by remember { mutableStateOf(settingsManager.trustedContact) }
    var editing by remember { mutableStateOf<EditTarget?>(null) }

    val isIos = LocalIsIos.current
    val listState = rememberLazyListState()
    val region = rememberDeviceRegion()
    val regionalLine = CrisisLines.forRegion(region)
    val effectiveLine = customLine ?: (regionalLine ?: CrisisLines.fallback).contact

    // Resolved here: the LazyColumn builder below is not a composable scope.
    val title = stringResource(Res.string.settings_title)
    val profileTitle = stringResource(Res.string.settings_profile_title)
    val nameHelper = stringResource(Res.string.settings_name_helper_updated)
    val nameLabel = stringResource(Res.string.settings_name_field_label)
    val notSet = stringResource(Res.string.settings_name_not_set)
    val breathingTitle = stringResource(Res.string.settings_crisis_breathing_label)
    val breathingHelper = stringResource(Res.string.settings_crisis_breathing_helper)
    val contactsTitle = stringResource(Res.string.settings_contacts_title)
    val contactsHelper = stringResource(Res.string.settings_contacts_helper)
    val crisisLineLabel = stringResource(Res.string.settings_crisis_line_label)
    val trustedLabel = stringResource(Res.string.settings_trusted_label)
    val trustedNotSet = stringResource(Res.string.settings_trusted_not_set)
    val useAutomatic = stringResource(Res.string.settings_crisis_line_reset)
    val remove = stringResource(Res.string.settings_contact_remove)
    val lineSource = when {
        customLine != null -> stringResource(Res.string.settings_crisis_line_custom)
        regionalLine != null -> stringResource(Res.string.settings_crisis_line_auto, regionalLine.countryName)
        else -> stringResource(Res.string.settings_crisis_line_auto_fallback, CrisisLines.fallback.countryName)
    }
    val patternNames = BreathingPattern.entries.associateWith { stringResource(it.nameRes) }
    val patternDescriptions = BreathingPattern.entries.associateWith { stringResource(it.descriptionRes) }

    AdaptivePageScaffold(title = title, listState = listState) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            largeTitle(title)

            adaptiveSection(
                header = profileTitle,
                footer = nameHelper,
                rows = listOf<@Composable () -> Unit>({
                    ValueRow(
                        label = nameLabel,
                        value = userName.ifBlank { notSet },
                        editLabel = stringResource(Res.string.settings_edit_name),
                        onClick = { editing = EditTarget.Name }
                    )
                })
            )

            adaptiveSection(
                items = BreathingPattern.entries,
                header = breathingTitle,
                footer = breathingHelper
            ) { pattern ->
                PatternRow(
                    name = patternNames.getValue(pattern),
                    description = patternDescriptions.getValue(pattern),
                    selected = pattern == crisisPattern,
                    onSelect = {
                        settingsManager.crisisBreathingPattern = pattern
                        crisisPattern = pattern
                    }
                )
            }

            adaptiveSection(
                header = contactsTitle,
                footer = contactsHelper,
                rows = buildList<@Composable () -> Unit> {
                    add {
                        ContactRow(
                            label = crisisLineLabel,
                            contact = effectiveLine,
                            emptyText = "",
                            caption = lineSource,
                            onClick = { editing = EditTarget.CrisisLine }
                        )
                    }
                    if (customLine != null) {
                        add {
                            AdaptiveActionRow(useAutomatic, onClick = {
                                settingsManager.customCrisisLine = null
                                customLine = null
                            })
                        }
                    }
                    add {
                        ContactRow(
                            label = trustedLabel,
                            contact = trusted,
                            emptyText = trustedNotSet,
                            caption = null,
                            onClick = { editing = EditTarget.TrustedContact }
                        )
                    }
                    if (trusted != null) {
                        add {
                            AdaptiveActionRow(remove, color = MaterialTheme.colorScheme.error, onClick = {
                                settingsManager.trustedContact = null
                                trusted = null
                            })
                        }
                    }
                }
            )

            item { Spacer(Modifier.height(if (isIos) 8.dp else 16.dp)) }
        }
    }

    when (editing) {
        EditTarget.Name -> NameSheet(
            initial = userName,
            onDismiss = { editing = null },
            onSave = {
                settingsManager.setUserName(it)
                editing = null
            }
        )
        EditTarget.CrisisLine -> ContactSheet(
            title = crisisLineLabel,
            initial = customLine ?: effectiveLine,
            namePlaceholder = stringResource(Res.string.settings_crisis_line_name_placeholder),
            onDismiss = { editing = null },
            onSave = {
                settingsManager.customCrisisLine = it
                customLine = settingsManager.customCrisisLine
                editing = null
            }
        )
        EditTarget.TrustedContact -> ContactSheet(
            title = trustedLabel,
            initial = trusted,
            namePlaceholder = stringResource(Res.string.settings_trusted_name_placeholder),
            onDismiss = { editing = null },
            onSave = {
                settingsManager.trustedContact = it
                trusted = settingsManager.trustedContact
                editing = null
            }
        )
        null -> Unit
    }
}

/** iOS: "Name ............ Matteo >". Android: label over value, with an edit button. */
@Composable
private fun ValueRow(label: String, value: String, editLabel: String, onClick: () -> Unit) {
    val isIos = LocalIsIos.current
    AdaptiveListRow(
        headline = { Text(label) },
        supporting = if (isIos) null else ({ Text(value) }),
        trailing = {
            if (isIos) {
                Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Icon(AnchorIcons.Edit, contentDescription = editLabel, tint = MaterialTheme.colorScheme.primary)
            }
        },
        onClick = onClick,
        showChevron = true
    )
}

/** iOS: a checkmark on the chosen row. Android: a leading radio button. */
@Composable
private fun PatternRow(name: String, description: String, selected: Boolean, onSelect: () -> Unit) {
    val isIos = LocalIsIos.current
    AdaptiveListRow(
        headline = { Text(name) },
        supporting = { Text(description) },
        leading = if (isIos) null else ({ RadioButton(selected = selected, onClick = null) }),
        trailing = if (isIos) ({
            Box(Modifier.width(20.dp)) {
                if (selected) Checkmark(MaterialTheme.colorScheme.primary, size = 16.dp, strokeWidth = 2.2.dp)
            }
        }) else null,
        modifier = Modifier.selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
    )
}

@Composable
private fun ContactRow(
    label: String,
    contact: HelpContact?,
    emptyText: String,
    caption: String?,
    onClick: () -> Unit
) {
    AdaptiveListRow(
        headline = { Text(label) },
        supporting = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                when {
                    contact == null -> Text(emptyText)
                    contact.name.isNotBlank() -> Text("${contact.name} — ${contact.number}")
                    else -> Text(contact.number)
                }
                if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall)
            }
        },
        onClick = onClick,
        showChevron = true
    )
}

@Composable
private fun NameSheet(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AdaptiveSheet(
        title = stringResource(Res.string.settings_name_label),
        cancelLabel = stringResource(Res.string.common_cancel),
        confirmLabel = stringResource(Res.string.common_save),
        confirmEnabled = name.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onSave(name) }
    ) {
        AdaptiveTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(Res.string.settings_name_label),
            placeholder = stringResource(Res.string.settings_name_placeholder),
            singleLine = true,
            showLabel = false
        )
    }
}

@Composable
private fun ContactSheet(
    title: String,
    initial: HelpContact?,
    namePlaceholder: String,
    onDismiss: () -> Unit,
    onSave: (HelpContact) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var number by remember { mutableStateOf(initial?.number.orEmpty()) }
    AdaptiveSheet(
        title = title,
        cancelLabel = stringResource(Res.string.common_cancel),
        confirmLabel = stringResource(Res.string.common_save),
        confirmEnabled = number.any { it.isDigit() },
        onDismiss = onDismiss,
        onConfirm = { onSave(HelpContact(name = name.trim(), number = number.trim())) }
    ) {
        AdaptiveTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(Res.string.settings_contact_name),
            placeholder = namePlaceholder,
            singleLine = true
        )
        AdaptiveTextField(
            value = number,
            onValueChange = { number = it },
            label = stringResource(Res.string.settings_contact_number),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
    }
}
