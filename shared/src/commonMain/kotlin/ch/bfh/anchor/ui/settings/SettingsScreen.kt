package ch.bfh.anchor.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.AppLanguage
import ch.bfh.anchor.model.BreathingPattern
import ch.bfh.anchor.model.CrisisLines
import ch.bfh.anchor.model.HelpContact
import ch.bfh.anchor.model.effectiveCrisisLine
import ch.bfh.anchor.notifications.MoodReminderController
import ch.bfh.anchor.notifications.NoMoodReminderController
import ch.bfh.anchor.platform.rememberDeviceRegion
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.settings.SettingsManager
import ch.bfh.anchor.ui.adaptive.AdaptiveActionRow
import ch.bfh.anchor.ui.adaptive.AdaptiveListRow
import ch.bfh.anchor.ui.adaptive.AdaptivePageScaffold
import ch.bfh.anchor.ui.adaptive.AdaptiveSheet
import ch.bfh.anchor.ui.adaptive.AdaptiveSwitch
import ch.bfh.anchor.ui.adaptive.AdaptiveTextField
import ch.bfh.anchor.ui.adaptive.AnchorCardButton
import ch.bfh.anchor.ui.adaptive.Checkmark
import ch.bfh.anchor.ui.adaptive.PageBack
import ch.bfh.anchor.ui.adaptive.adaptiveSection
import ch.bfh.anchor.ui.adaptive.largeTitle
import ch.bfh.anchor.ui.icons.AnchorIcons
import ch.bfh.anchor.ui.theme.LocalIsIos
import ch.bfh.anchor.ui.theme.ThemeMode
import ch.bfh.anchor.ui.theme.dynamicColorAvailable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A settings topic with a page of its own, opened from the main Settings list as in the iOS
 * Settings app. [route] is its part of the navigation route.
 */
enum class SettingsPage(val route: String, val titleRes: StringResource) {
    Language("language", Res.string.settings_language_title),
    Appearance("appearance", Res.string.settings_appearance_title),
    MoodReminders("reminders", Res.string.settings_mood_reminders_title),
    Breathing("breathing", Res.string.settings_crisis_breathing_label),
    Contacts("contacts", Res.string.settings_contacts_title),
    SampleData("samples", Res.string.settings_sample_title);

    companion object {
        fun fromRoute(route: String?) = entries.firstOrNull { it.route == route }
    }
}

/** The main Settings list: the name, then one row per topic with its current value. */
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    reminderController: MoodReminderController = NoMoodReminderController,
    onOpenPage: (SettingsPage) -> Unit
) {
    val language by settingsManager.appLanguage.collectAsState()
    val userName by settingsManager.userName.collectAsState()
    val themeMode by settingsManager.themeMode.collectAsState()
    var editingName by remember { mutableStateOf(false) }
    // Read when this page is shown: it leaves composition while a topic page is open, so these
    // are fresh again on the way back.
    val remindersOn = remember { reminderController.supported && settingsManager.moodRemindersEnabled }
    val pattern = remember { settingsManager.crisisBreathingPattern }

    val listState = rememberLazyListState()
    // Resolved here: the LazyColumn builder below is not a composable scope.
    val title = stringResource(Res.string.settings_title)
    val nameLabel = stringResource(Res.string.settings_name_field_label)
    val nameHelper = stringResource(Res.string.settings_name_helper_updated)
    val notSet = stringResource(Res.string.settings_name_not_set)
    val titles = SettingsPage.entries.associateWith { stringResource(it.titleRes) }
    val values = mapOf(
        SettingsPage.Language to
            if (language == AppLanguage.System) stringResource(Res.string.settings_language_system) else language.nativeName,
        SettingsPage.Appearance to stringResource(themeLabelRes(themeMode)),
        SettingsPage.MoodReminders to stringResource(if (remindersOn) Res.string.settings_value_on else Res.string.settings_value_off),
        SettingsPage.Breathing to stringResource(pattern.nameRes)
    )

    fun androidx.compose.foundation.lazy.LazyListScope.topics(pages: List<SettingsPage>) {
        adaptiveSection(items = pages, separatorInset = TopicSeparatorInset) { page ->
            TopicRow(
                icon = page.icon,
                iconColor = page.iconColor,
                title = titles.getValue(page),
                value = values[page],
                onClick = { onOpenPage(page) }
            )
        }
    }

    AdaptivePageScaffold(title = title, listState = listState) { padding ->
        LazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            largeTitle(title)
            adaptiveSection(
                footer = nameHelper,
                separatorInset = TopicSeparatorInset,
                rows = listOf<@Composable () -> Unit>({
                    TopicRow(
                        icon = AnchorIcons.Person,
                        iconColor = Color(0xFF8E8E93),
                        title = nameLabel,
                        value = userName.ifBlank { notSet },
                        onClick = { editingName = true }
                    )
                })
            )
            topics(listOf(SettingsPage.Language, SettingsPage.Appearance))
            topics(listOf(SettingsPage.MoodReminders, SettingsPage.Breathing, SettingsPage.Contacts))
            topics(listOf(SettingsPage.SampleData))
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (editingName) {
        NameSheet(
            initial = userName,
            onDismiss = { editingName = false },
            onSave = {
                settingsManager.setUserName(it)
                editingName = false
            }
        )
    }
}

/** One topic's page, with a way back to the main Settings list. */
@Composable
fun SettingsPageScreen(
    page: SettingsPage,
    settingsManager: SettingsManager,
    reminderController: MoodReminderController = NoMoodReminderController,
    onLanguageChange: (AppLanguage) -> Unit = settingsManager::setAppLanguage,
    onAddSampleMoods: () -> Unit = {},
    onRemoveSampleMoods: () -> Unit = {},
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    val title = stringResource(page.titleRes)
    val back = PageBack(stringResource(Res.string.settings_title), onBack)
    // Sheets of the Contacts page: they sit outside the list.
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    val contacts = rememberContactsState(settingsManager)

    // Every section is resolved here: the LazyColumn builder below is not a composable scope.
    val content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = when (page) {
        SettingsPage.Language -> languageContent(settingsManager, onLanguageChange)
        SettingsPage.Appearance -> appearanceContent(settingsManager)
        SettingsPage.MoodReminders -> {
            val state = rememberMoodReminderState(settingsManager, reminderController)
            val texts = moodReminderTexts(state)
            ({ moodReminderSection(state, texts, showHeader = false) })
        }
        SettingsPage.Breathing -> breathingContent(settingsManager)
        SettingsPage.Contacts -> contactsContent(contacts) { editing = it }
        SettingsPage.SampleData -> sampleContent(onAddSampleMoods, onRemoveSampleMoods)
    }

    AdaptivePageScaffold(title = title, listState = listState, back = back) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + if (LocalIsIos.current) 8.dp else 0.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }

    when (editing) {
        EditTarget.CrisisLine -> ContactSheet(
            title = stringResource(Res.string.settings_crisis_line_label),
            initial = contacts.customLine ?: contacts.effectiveLine,
            namePlaceholder = stringResource(Res.string.settings_crisis_line_name_placeholder),
            onDismiss = { editing = null },
            onSave = {
                contacts.saveCustomLine(it)
                editing = null
            }
        )
        EditTarget.TrustedContact -> ContactSheet(
            title = stringResource(Res.string.settings_trusted_label),
            initial = contacts.trusted,
            namePlaceholder = stringResource(Res.string.settings_trusted_name_placeholder),
            onDismiss = { editing = null },
            onSave = {
                contacts.saveTrusted(it)
                editing = null
            }
        )
        null -> Unit
    }
}

// ── Topic pages ──────────────────────────────────────────────────────────────

@Composable
private fun languageContent(
    settingsManager: SettingsManager,
    onLanguageChange: (AppLanguage) -> Unit
): androidx.compose.foundation.lazy.LazyListScope.() -> Unit {
    val language by settingsManager.appLanguage.collectAsState()
    val helper = stringResource(Res.string.settings_language_helper)
    val names = AppLanguage.entries.associateWith {
        if (it == AppLanguage.System) stringResource(Res.string.settings_language_system) else it.nativeName
    }
    return {
        adaptiveSection(items = AppLanguage.entries, footer = helper) { option ->
            ChoiceRow(
                name = names.getValue(option),
                description = null,
                selected = option == language,
                onSelect = { if (option != language) onLanguageChange(option) }
            )
        }
    }
}

@Composable
private fun appearanceContent(settingsManager: SettingsManager): androidx.compose.foundation.lazy.LazyListScope.() -> Unit {
    val isIos = LocalIsIos.current
    val themeMode by settingsManager.themeMode.collectAsState()
    val dynamicColor by settingsManager.dynamicColor.collectAsState()
    val themeLabels = ThemeMode.entries.associateWith { stringResource(themeLabelRes(it)) }
    val dynamicTitle = stringResource(Res.string.settings_dynamic_color)
    val dynamicSubtitle = stringResource(Res.string.settings_dynamic_color_subtitle)
    return {
        adaptiveSection(
            rows = buildList<@Composable () -> Unit> {
                add { ThemePicker(selected = themeMode, labels = themeLabels, onSelect = settingsManager::setThemeMode) }
                // Material You exists on Android 12+ only, and the iOS look ignores it.
                if (dynamicColorAvailable && !isIos) {
                    add {
                        AdaptiveListRow(
                            headline = { Text(dynamicTitle) },
                            supporting = { Text(dynamicSubtitle) },
                            trailing = {
                                AdaptiveSwitch(checked = dynamicColor, onCheckedChange = settingsManager::setDynamicColor)
                            },
                            modifier = Modifier.toggleable(
                                value = dynamicColor,
                                role = Role.Switch,
                                onValueChange = settingsManager::setDynamicColor
                            )
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun breathingContent(settingsManager: SettingsManager): androidx.compose.foundation.lazy.LazyListScope.() -> Unit {
    var crisisPattern by remember { mutableStateOf(settingsManager.crisisBreathingPattern) }
    val helper = stringResource(Res.string.settings_crisis_breathing_helper)
    val names = BreathingPattern.entries.associateWith { stringResource(it.nameRes) }
    val descriptions = BreathingPattern.entries.associateWith { stringResource(it.descriptionRes) }
    return {
        adaptiveSection(items = BreathingPattern.entries, footer = helper) { pattern ->
            ChoiceRow(
                name = names.getValue(pattern),
                description = descriptions.getValue(pattern),
                selected = pattern == crisisPattern,
                onSelect = {
                    settingsManager.crisisBreathingPattern = pattern
                    crisisPattern = pattern
                }
            )
        }
    }
}

/** Which contact sheet is open. */
private enum class EditTarget { CrisisLine, TrustedContact }

/** The crisis line and trusted contact, as saved and as shown. */
private class ContactsState(private val settings: SettingsManager, val region: String?) {
    var customLine by mutableStateOf(settings.customCrisisLine)
        private set
    var trusted by mutableStateOf(settings.trustedContact)
        private set
    val regionalLine get() = CrisisLines.forRegion(region)
    val effectiveLine get() = effectiveCrisisLine(customLine, region)

    fun saveCustomLine(contact: HelpContact?) {
        settings.customCrisisLine = contact
        customLine = settings.customCrisisLine
    }

    fun saveTrusted(contact: HelpContact?) {
        settings.trustedContact = contact
        trusted = settings.trustedContact
    }
}

@Composable
private fun rememberContactsState(settings: SettingsManager): ContactsState {
    val region = rememberDeviceRegion()
    return remember(settings, region) { ContactsState(settings, region) }
}

@Composable
private fun contactsContent(
    contacts: ContactsState,
    onEdit: (EditTarget) -> Unit
): androidx.compose.foundation.lazy.LazyListScope.() -> Unit {
    val helper = stringResource(Res.string.settings_contacts_helper)
    val crisisLineLabel = stringResource(Res.string.settings_crisis_line_label)
    val trustedLabel = stringResource(Res.string.settings_trusted_label)
    val trustedNotSet = stringResource(Res.string.settings_trusted_not_set)
    val useAutomatic = stringResource(Res.string.settings_crisis_line_reset)
    val remove = stringResource(Res.string.settings_contact_remove)
    val regionalLine = contacts.regionalLine
    val lineSource = when {
        contacts.customLine != null -> stringResource(Res.string.settings_crisis_line_custom)
        regionalLine != null -> stringResource(Res.string.settings_crisis_line_auto, stringResource(regionalLine.countryName))
        else -> stringResource(Res.string.settings_crisis_line_auto_fallback, stringResource(CrisisLines.fallback.countryName))
    }
    return {
        adaptiveSection(
            footer = helper,
            rows = buildList<@Composable () -> Unit> {
                add {
                    ContactRow(
                        label = crisisLineLabel,
                        contact = contacts.effectiveLine,
                        emptyText = "",
                        caption = lineSource,
                        onClick = { onEdit(EditTarget.CrisisLine) }
                    )
                }
                if (contacts.customLine != null) {
                    add { AdaptiveActionRow(useAutomatic, onClick = { contacts.saveCustomLine(null) }) }
                }
                add {
                    ContactRow(
                        label = trustedLabel,
                        contact = contacts.trusted,
                        emptyText = trustedNotSet,
                        caption = null,
                        onClick = { onEdit(EditTarget.TrustedContact) }
                    )
                }
                if (contacts.trusted != null) {
                    add {
                        AdaptiveActionRow(remove, color = MaterialTheme.colorScheme.error, onClick = { contacts.saveTrusted(null) })
                    }
                }
            }
        )
    }
}

/** For demos and testing the mood chart; every sample is marked and removable. */
@Composable
private fun sampleContent(onAdd: () -> Unit, onRemove: () -> Unit): androidx.compose.foundation.lazy.LazyListScope.() -> Unit {
    val footer = stringResource(Res.string.settings_sample_footer)
    val add = stringResource(Res.string.settings_sample_add)
    val remove = stringResource(Res.string.settings_sample_remove)
    return {
        adaptiveSection(
            footer = footer,
            rows = listOf<@Composable () -> Unit>(
                { AdaptiveActionRow(add, onClick = onAdd) },
                { AdaptiveActionRow(remove, color = MaterialTheme.colorScheme.error, onClick = onRemove) }
            )
        )
    }
}

// ── Rows ─────────────────────────────────────────────────────────────────────

/** Where a separator starts in the main list: under the title, past the icon. */
private val TopicSeparatorInset = 58.dp

private val SettingsPage.icon: ImageVector
    get() = when (this) {
        SettingsPage.Language -> AnchorIcons.Language
        SettingsPage.Appearance -> AnchorIcons.Palette
        SettingsPage.MoodReminders -> AnchorIcons.Notifications
        SettingsPage.Breathing -> AnchorIcons.Air
        SettingsPage.Contacts -> AnchorIcons.Call
        SettingsPage.SampleData -> AnchorIcons.Insights
    }

/** The colour of the icon tile on iOS, as in the Settings app. */
private val SettingsPage.iconColor: Color
    get() = when (this) {
        SettingsPage.Language -> Color(0xFF007AFF)
        SettingsPage.Appearance -> Color(0xFF5856D6)
        SettingsPage.MoodReminders -> Color(0xFFFF3B30)
        SettingsPage.Breathing -> Color(0xFF32ADE6)
        SettingsPage.Contacts -> Color(0xFF34C759)
        SettingsPage.SampleData -> Color(0xFFFF9500)
    }

private fun themeLabelRes(mode: ThemeMode) = when (mode) {
    ThemeMode.System -> Res.string.settings_theme_system
    ThemeMode.Light -> Res.string.settings_theme_light
    ThemeMode.Dark -> Res.string.settings_theme_dark
}

/**
 * iOS: a white icon on a coloured tile, the title, the current value in grey and a chevron.
 * Android: a tinted icon, the title with the value under it.
 */
@Composable
private fun TopicRow(icon: ImageVector, iconColor: Color, title: String, value: String?, onClick: () -> Unit) {
    val isIos = LocalIsIos.current
    AdaptiveListRow(
        leading = {
            if (isIos) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(iconColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
            } else {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        },
        headline = { Text(title) },
        supporting = if (isIos || value == null) null else ({ Text(value) }),
        trailing = if (isIos && value != null) ({
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp)
            )
        }) else null,
        onClick = onClick,
        showChevron = true
    )
}

/** One option of a single choice. iOS: a checkmark on the chosen row. Android: a leading radio button. */
@Composable
private fun ChoiceRow(name: String, description: String?, selected: Boolean, onSelect: () -> Unit) {
    val isIos = LocalIsIos.current
    AdaptiveListRow(
        headline = { Text(name) },
        supporting = description?.let { { Text(it) } },
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

/** System / Light / Dark as three tiles, each with its icon; the chosen one fills with sage. */
@Composable
private fun ThemePicker(selected: ThemeMode, labels: Map<ThemeMode, String>, onSelect: (ThemeMode) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            AnchorCardButton(
                onClick = { onSelect(mode) },
                selected = isSelected,
                modifier = Modifier.weight(1f),
                color = if (isSelected) colors.primaryContainer else colors.onSurface.copy(alpha = 0.05f),
                contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                shape = MaterialTheme.shapes.small,
                border = if (isSelected) BorderStroke(1.5.dp, colors.primary) else null
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = when (mode) {
                            ThemeMode.System -> AnchorIcons.Palette
                            ThemeMode.Light -> AnchorIcons.LightMode
                            ThemeMode.Dark -> AnchorIcons.DarkMode
                        },
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(labels.getValue(mode), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
