package ch.bfh.anchor.ui.mood

import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.bfh.anchor.model.MoodEntry
import ch.bfh.anchor.model.MoodReminderSchedule
import ch.bfh.anchor.model.MoodScale
import ch.bfh.anchor.notifications.MoodReminderController
import ch.bfh.anchor.platform.formatMoodDateTime
import ch.bfh.anchor.platform.nowMillis
import ch.bfh.anchor.settings.SettingsManager
import ch.bfh.anchor.ui.adaptive.AdaptiveActionRow
import ch.bfh.anchor.ui.adaptive.AdaptiveListRow
import ch.bfh.anchor.ui.adaptive.AdaptivePicker
import ch.bfh.anchor.ui.adaptive.AdaptiveSegmentedControl
import ch.bfh.anchor.ui.adaptive.AdaptiveSwitch
import ch.bfh.anchor.ui.adaptive.AnchorCardButton
import ch.bfh.anchor.ui.adaptive.AnchorTextButton
import ch.bfh.anchor.ui.adaptive.Chevron
import ch.bfh.anchor.ui.adaptive.adaptiveSection
import ch.bfh.anchor.ui.theme.LocalIsIos
import ch.bfh.anchor.viewmodel.AnchorViewModel
import kotlin.math.roundToInt

/** Only one bottom tab can be open. The default (null) is the quick check-in landing page. */
private enum class MoodPanel { HISTORY, NOTIFICATIONS }

/** Half-hour slots of the reminder window; slot 48 is the end of the day. */
private const val LAST_SLOT = 48

private fun slotMinutes(slot: Int) = if (slot == LAST_SLOT) 1439 else slot * 30
private fun slotLabel(slot: Int) =
    if (slot == LAST_SLOT) "00:00" else MoodReminderSchedule.timeLabel(slot * 30)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MoodTrackerScreen(
    vm: AnchorViewModel,
    settingsManager: SettingsManager,
    reminderController: MoodReminderController,
    onBack: () -> Unit
) {
    val isIos = LocalIsIos.current
    val entries by vm.moods.collectAsState()
    var lastSelected by remember { mutableStateOf<Int?>(null) }
    var activePanel by remember { mutableStateOf<MoodPanel?>(null) }
    var showFullHistory by remember { mutableStateOf(false) }
    // The graph and expanded entry list share a single selected time range.
    var historyPeriodDays by remember { mutableIntStateOf(1) }
    val historyNow = remember(entries, historyPeriodDays, activePanel) { nowMillis() }
    val matchingHistory = remember(entries, historyPeriodDays, historyNow) {
        moodEntriesInPeriod(entries, historyPeriodDays, historyNow).asReversed()
    }

    var remindersEnabled by remember { mutableStateOf(settingsManager.moodRemindersEnabled) }
    var startSlot by remember { mutableIntStateOf(settingsManager.moodReminderStartMinutes / 30) }
    var endSlot by remember {
        // The midnight at the far right is the END of the day, not a second reminder.
        mutableIntStateOf(
            if (settingsManager.moodReminderEndMinutes >= 1439) LAST_SLOT
            else settingsManager.moodReminderEndMinutes / 30
        )
    }
    var intervalHours by remember { mutableIntStateOf(settingsManager.moodReminderIntervalHours) }
    var frequencyMenuExpanded by remember { mutableStateOf(false) }
    var notificationInfoExpanded by remember { mutableStateOf(false) }
    var permissionWarning by remember { mutableStateOf(false) }
    var testFeedback by remember { mutableStateOf<String?>(null) }
    // Resolved up front: they are set from click handlers, which cannot load resources.
    val testSentText = stringResource(if (isIos) Res.string.mood_test_sent_ios else Res.string.mood_test_sent)
    val whyNotificationsLabel = stringResource(Res.string.mood_why_notifications)

    val startMinutes = slotMinutes(startSlot)
    val endMinutes = slotMinutes(endSlot)
    val endLabel = slotLabel(endSlot)

    // Reads the window from state when called, not from this composition: a picker changes a
    // slot and saves in the same handler.
    fun persistAndReschedule() {
        val start = slotMinutes(startSlot)
        val end = slotMinutes(endSlot)
        settingsManager.moodReminderStartMinutes = start
        settingsManager.moodReminderEndMinutes = end
        settingsManager.moodReminderIntervalHours = intervalHours
        if (remindersEnabled) {
            reminderController.configure(true, start, end, intervalHours) { success ->
                permissionWarning = !success
            }
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        permissionWarning = false
        testFeedback = null
        reminderController.configure(enabled, startMinutes, endMinutes, intervalHours) { success ->
            if (success) remindersEnabled = enabled
            else permissionWarning = true
        }
    }

    fun sendTest() {
        testFeedback = null
        permissionWarning = false
        reminderController.sendTestNotification { success ->
            if (success) testFeedback = testSentText
            else permissionWarning = true
        }
    }

    fun closePanel() {
        activePanel = null
        showFullHistory = false
        frequencyMenuExpanded = false
        notificationInfoExpanded = false
    }

    fun togglePanel(panel: MoodPanel) {
        if (activePanel == panel) closePanel()
        else {
            activePanel = panel
            if (panel == MoodPanel.HISTORY) historyPeriodDays = 1
            showFullHistory = false
            frequencyMenuExpanded = false
            notificationInfoExpanded = false
        }
    }

    // Texts for the iOS sections, which are built outside composition.
    val iosText = IosNotificationText(
        remindersTitle = stringResource(Res.string.mood_reminders_title),
        scheduleTitle = stringResource(Res.string.mood_schedule_title),
        info = stringResource(Res.string.mood_notifications_info),
        blocked = stringResource(Res.string.mood_notifications_blocked_ios),
        scheduleHint = stringResource(
            if (remindersEnabled) Res.string.mood_local_time_hint_ios else Res.string.mood_choose_times_hint
        ),
        unavailable = stringResource(Res.string.mood_notifications_unavailable)
    )

    val backLabel = stringResource(if (activePanel == null) Res.string.common_back else Res.string.common_close)
    val onBackPressed = { if (activePanel == null) onBack() else closePanel() }

    Scaffold(
        topBar = {
            if (isIos) {
                IosBackBar(label = backLabel, showChevron = activePanel == null, onClick = onBackPressed)
            } else {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        TextButton(onClick = onBackPressed) { Text(backLabel) }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // The quick check-in stays centered on the landing page and remains visible
            // behind a panel, so tapping outside a panel returns to the same place.
            Box(
                modifier = Modifier.fillMaxSize().padding(
                    start = 20.dp, end = 20.dp,
                    bottom = if (activePanel == null) 65.dp else 0.dp
                ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    Text(
                        stringResource(Res.string.mood_question),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    MoodChoices(
                        selectedScore = lastSelected,
                        onScoreSelected = { score ->
                            vm.addMood(score) { lastSelected = score }
                        }
                    )
                    if (lastSelected != null) {
                        Text(
                            stringResource(Res.string.mood_saved),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (activePanel == null) {
                // Both sections are collapsed by default. The only controls below the
                // centered check-in are these two equal-width tabs.
                MoodPanelTabs(
                    selected = null,
                    onSelect = ::togglePanel,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                // A dismissible layer over the landing page: tapping anywhere OUTSIDE
                // the raised panel closes it without changing the selected mood.
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.26f))
                        .clickable(onClickLabel = stringResource(Res.string.mood_close_panel)) { closePanel() }
                )

                val maximumPanelHeight = (maxHeight - 80.dp).coerceAtLeast(180.dp)
                val desiredPanelHeight = when (activePanel) {
                    MoodPanel.HISTORY -> 590.dp
                    MoodPanel.NOTIFICATIONS -> 510.dp
                    null -> 275.dp
                }
                val panelHeight = minOf(maximumPanelHeight, desiredPanelHeight)

                // A bottom sheet: the open panel grows upward from the bottom edge. On iOS it
                // takes the grey grouped background of a sheet, so its sections stand out.
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth().height(panelHeight),
                    shape = RoundedCornerShape(
                        topStart = if (isIos) 28.dp else 24.dp,
                        topEnd = if (isIos) 28.dp else 24.dp
                    ),
                    color = MaterialTheme.colorScheme.run { if (isIos) background else surface },
                    tonalElevation = if (isIos) 0.dp else 5.dp,
                    shadowElevation = 7.dp
                ) {
                    Column {
                        // The very same two tab-style buttons move to the TOP when a
                        // section opens. Tap the selected tab again to close it.
                        MoodPanelTabs(
                            selected = activePanel,
                            onSelect = ::togglePanel
                        )
                        if (!isIos) HorizontalDivider()

                        // The history toggle belongs *below the graph* in the scrolling
                        // content. A stickyHeader keeps it reachable once it reaches the top.
                        // Expanding the entries preserves your position; collapsing returns
                        // to the graph, instead of leaving you deep in the old list.
                        val panelListState = remember(activePanel) { LazyListState() }
                        LaunchedEffect(activePanel, showFullHistory, historyPeriodDays) {
                            if (activePanel == MoodPanel.HISTORY && !showFullHistory) {
                                panelListState.scrollToItem(0)
                            }
                        }
                        // iOS sections bring their own side margins; other items get them here.
                        val itemInset = Modifier.padding(horizontal = if (isIos) 16.dp else 0.dp)
                        LazyColumn(
                            state = panelListState,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentPadding = PaddingValues(
                                start = if (isIos) 0.dp else 20.dp,
                                end = if (isIos) 0.dp else 20.dp,
                                top = if (isIos) 8.dp else 14.dp,
                                bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(if (isIos) 0.dp else 12.dp)
                        ) {
                            when (activePanel) {
                                MoodPanel.HISTORY -> {
                                    item {
                                        Box(itemInset) {
                                            MoodHistoryChart(
                                                entries = entries,
                                                periodDays = historyPeriodDays,
                                                now = historyNow,
                                                onPeriodDaysChange = { historyPeriodDays = it }
                                            )
                                        }
                                    }
                                    stickyHeader(key = "history_entries_toggle") {
                                        Surface(
                                            color = MaterialTheme.colorScheme.run { if (isIos) background else surface },
                                            tonalElevation = if (showFullHistory && !isIos) 2.dp else 0.dp
                                        ) {
                                            Column(itemInset) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AnchorTextButton(
                                                        onClick = { showFullHistory = !showFullHistory }
                                                    ) {
                                                        Text(
                                                            if (showFullHistory) stringResource(Res.string.mood_hide_entries)
                                                            else pluralStringResource(Res.plurals.mood_view_entries, historyPeriodDays, historyPeriodDays)
                                                        )
                                                    }
                                                }
                                                if (!isIos) {
                                                    HorizontalDivider(
                                                        color = MaterialTheme.colorScheme.outlineVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (showFullHistory) {
                                        if (matchingHistory.isEmpty()) {
                                            item {
                                                Text(
                                                    if (entries.isEmpty()) stringResource(Res.string.mood_history_empty)
                                                    else stringResource(Res.string.mood_period_empty),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = itemInset
                                                )
                                            }
                                        } else if (isIos) {
                                            adaptiveSection(items = matchingHistory, key = { it.id }, separatorInset = 56.dp) { entry ->
                                                IosMoodHistoryRow(entry)
                                            }
                                        } else {
                                            items(matchingHistory, key = { it.id }) { entry ->
                                                MoodHistoryRow(entry)
                                            }
                                        }
                                    }
                                }

                                MoodPanel.NOTIFICATIONS -> if (isIos) {
                                    iosNotificationSections(
                                        text = iosText,
                                        supported = reminderController.supported,
                                        remindersEnabled = remindersEnabled,
                                        permissionWarning = permissionWarning,
                                        testFeedback = testFeedback,
                                        startSlot = startSlot,
                                        endSlot = endSlot,
                                        intervalHours = intervalHours,
                                        onEnabledChange = ::setRemindersEnabled,
                                        onSendTest = ::sendTest,
                                        onStartChange = { slot ->
                                            startSlot = slot
                                            if (endSlot < slot) endSlot = slot
                                            persistAndReschedule()
                                        },
                                        onEndChange = { slot ->
                                            endSlot = slot
                                            persistAndReschedule()
                                        },
                                        onIntervalChange = { hours ->
                                            intervalHours = hours
                                            persistAndReschedule()
                                        }
                                    )
                                } else {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        stringResource(Res.string.mood_allow_notifications),
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Box {
                                                        IconButton(
                                                            onClick = { notificationInfoExpanded = true },
                                                            modifier = Modifier.size(40.dp).semantics {
                                                                contentDescription = whyNotificationsLabel
                                                            }
                                                        ) {
                                                            Text(
                                                                "ⓘ",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        DropdownMenu(
                                                            expanded = notificationInfoExpanded,
                                                            onDismissRequest = { notificationInfoExpanded = false },
                                                            modifier = Modifier.width(280.dp)
                                                        ) {
                                                            Text(
                                                                stringResource(Res.string.mood_notifications_info),
                                                                modifier = Modifier.padding(16.dp),
                                                                style = MaterialTheme.typography.bodyMedium
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    if (remindersEnabled) stringResource(Res.string.mood_reminders_on)
                                                    else stringResource(Res.string.mood_reminders_off),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Checkbox(
                                                enabled = reminderController.supported,
                                                checked = remindersEnabled,
                                                onCheckedChange = ::setRemindersEnabled
                                            )
                                        }
                                    }

                                    if (reminderController.supported) {
                                        // Keep the preview near its opt-in control. Avoid
                                        // offering a notification while reminders are disabled.
                                        item {
                                            OutlinedButton(
                                                onClick = ::sendTest,
                                                enabled = remindersEnabled
                                            ) {
                                                Text(stringResource(Res.string.mood_send_test))
                                            }
                                        }
                                        testFeedback?.let { feedback ->
                                            item {
                                                Text(
                                                    feedback,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        if (permissionWarning) {
                                            item {
                                                Text(
                                                    stringResource(Res.string.mood_notifications_blocked),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    // Scheduling controls are now part of the Notifications tab,
                                    // not expanded/collapsed by the permission checkbox. Changes
                                    // are saved even when reminders are turned off.
                                    if (reminderController.supported) {
                                        item {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        stringResource(Res.string.mood_start, MoodReminderSchedule.timeLabel(startMinutes)),
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                    Text(
                                                        stringResource(Res.string.mood_end, endLabel),
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                                RangeSlider(
                                                    value = startSlot.toFloat()..endSlot.toFloat(),
                                                    onValueChange = { range ->
                                                        startSlot = range.start.roundToInt().coerceIn(0, LAST_SLOT - 1)
                                                        endSlot = range.endInclusive.roundToInt()
                                                            .coerceIn(startSlot, LAST_SLOT)
                                                    },
                                                    valueRange = 0f..LAST_SLOT.toFloat(),
                                                    steps = LAST_SLOT - 1,
                                                    onValueChangeFinished = { persistAndReschedule() },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    listOf("00:00", "12:00", "00:00").forEach { label ->
                                                        Text(
                                                            label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(stringResource(Res.string.mood_repeat), style = MaterialTheme.typography.bodyMedium)
                                                    Box {
                                                        OutlinedButton(
                                                            onClick = { frequencyMenuExpanded = true }
                                                        ) {
                                                            Text(
                                                                pluralStringResource(Res.plurals.mood_every_hours, intervalHours, intervalHours) + "  ▾"
                                                            )
                                                        }
                                                        DropdownMenu(
                                                            expanded = frequencyMenuExpanded,
                                                            onDismissRequest = { frequencyMenuExpanded = false }
                                                        ) {
                                                            MoodReminderSchedule.intervalsInHours.forEach { option ->
                                                                DropdownMenuItem(
                                                                    text = {
                                                                        Text(
                                                                            pluralStringResource(Res.plurals.mood_every_hours, option, option)
                                                                        )
                                                                    },
                                                                    onClick = {
                                                                        frequencyMenuExpanded = false
                                                                        intervalHours = option
                                                                        persistAndReschedule()
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                Text(
                                                    if (remindersEnabled)
                                                        stringResource(Res.string.mood_local_time_hint)
                                                    else
                                                        stringResource(Res.string.mood_choose_times_hint),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        item {
                                            Text(
                                                stringResource(Res.string.mood_notifications_unavailable),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                null -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── iOS ──────────────────────────────────────────────────────────────────────

/** The navigation bar of a pushed iOS screen: a tinted "‹ Back" at the leading edge. */
@Composable
private fun IosBackBar(label: String, showChevron: Boolean, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .height(44.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnchorTextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
            if (showChevron) {
                // The disclosure chevron, mirrored to point back.
                Chevron(modifier = Modifier.graphicsLayer(scaleX = -1f), color = tint)
                Spacer(Modifier.width(6.dp))
            }
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
        }
    }
}

/** Texts for [iosNotificationSections], looked up beforehand because list builders cannot. */
private class IosNotificationText(
    val remindersTitle: String,
    val scheduleTitle: String,
    val info: String,
    val blocked: String,
    val scheduleHint: String,
    val unavailable: String
)

/**
 * The Notifications panel as iOS Settings would lay it out: a switch to turn reminders on,
 * then the reminder window and frequency as picker rows instead of Android's range slider.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.iosNotificationSections(
    text: IosNotificationText,
    supported: Boolean,
    remindersEnabled: Boolean,
    permissionWarning: Boolean,
    testFeedback: String?,
    startSlot: Int,
    endSlot: Int,
    intervalHours: Int,
    onEnabledChange: (Boolean) -> Unit,
    onSendTest: () -> Unit,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    onIntervalChange: (Int) -> Unit
) {
    val footer = listOfNotNull(if (permissionWarning) text.blocked else testFeedback, text.info)
        .joinToString("\n\n")
    adaptiveSection(
        header = text.remindersTitle,
        footer = if (supported) footer else text.unavailable,
        rows = buildList<@Composable () -> Unit> {
            add {
                AdaptiveListRow(
                    headline = { Text(stringResource(Res.string.mood_allow_notifications)) },
                    supporting = {
                        Text(
                            stringResource(
                                if (remindersEnabled) Res.string.mood_reminders_on else Res.string.mood_reminders_off
                            )
                        )
                    },
                    trailing = {
                        if (supported) AdaptiveSwitch(checked = remindersEnabled, onCheckedChange = onEnabledChange)
                    }
                )
            }
            if (supported && remindersEnabled) {
                add { AdaptiveActionRow(stringResource(Res.string.mood_send_test), onClick = onSendTest) }
            }
        }
    )
    if (!supported) return

    adaptiveSection(
        header = text.scheduleTitle,
        footer = text.scheduleHint,
        rows = listOf<@Composable () -> Unit>(
            {
                val slots = (0 until LAST_SLOT).toList()
                AdaptivePicker(
                    label = stringResource(Res.string.mood_start_label),
                    options = slots.map(::slotLabel),
                    selected = slotLabel(startSlot),
                    onSelect = { label -> onStartChange(slots.first { slotLabel(it) == label }) },
                    grouped = true
                )
            },
            {
                // Ends at or after the start; the last option is midnight, the end of the day.
                // A window starting at midnight cannot also end there, or "00:00" would be listed twice.
                val slots = (maxOf(startSlot, 1)..LAST_SLOT).toList()
                AdaptivePicker(
                    label = stringResource(Res.string.mood_end_label),
                    options = slots.map(::slotLabel),
                    selected = slotLabel(endSlot),
                    onSelect = { label -> onEndChange(slots.first { slotLabel(it) == label }) },
                    grouped = true
                )
            },
            {
                val labels = MoodReminderSchedule.intervalsInHours.associateWith {
                    pluralStringResource(Res.plurals.mood_every_hours, it, it)
                }
                AdaptivePicker(
                    label = stringResource(Res.string.mood_repeat),
                    options = labels.values.toList(),
                    selected = labels.getValue(intervalHours),
                    onSelect = { label -> onIntervalChange(labels.entries.first { it.value == label }.key) },
                    grouped = true
                )
            }
        )
    )
}

@Composable
private fun IosMoodHistoryRow(entry: MoodEntry) {
    AdaptiveListRow(
        leading = {
            Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                Text(MoodScale.emoji(entry.score), fontSize = 24.sp)
            }
        },
        headline = { Text(stringResource(MoodScale.labelRes(entry.score))) },
        supporting = { Text(formatMoodDateTime(entry.recordedAt, Locale.current.language)) },
        trailing = {
            Text(
                stringResource(moodSourceLabel(entry.source)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

// ── Shared ───────────────────────────────────────────────────────────────────

/**
 * Android: two filter chips. iOS: on the landing page, two cards to open a panel; inside an open
 * panel, a segmented control to switch between them.
 */
@Composable
private fun MoodPanelTabs(
    selected: MoodPanel?,
    onSelect: (MoodPanel) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = mapOf(
        MoodPanel.HISTORY to stringResource(Res.string.mood_tab_history),
        MoodPanel.NOTIFICATIONS to stringResource(Res.string.mood_tab_notifications)
    )
    val isIos = LocalIsIos.current

    if (isIos && selected != null) {
        AdaptiveSegmentedControl(
            options = labels.values.toList(),
            selectedIndex = selected.ordinal,
            onSelect = { onSelect(MoodPanel.entries[it]) },
            modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        return
    }

    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = if (isIos) 16.dp else 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MoodPanel.entries.forEach { panel ->
            if (isIos) {
                AnchorCardButton(
                    onClick = { onSelect(panel) },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        labels.getValue(panel),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 8.dp)
                    )
                }
            } else {
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = selected == panel,
                    onClick = { onSelect(panel) },
                    label = {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(labels.getValue(panel), maxLines = 1)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MoodHistoryRow(entry: MoodEntry) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(MoodScale.emoji(entry.score), fontSize = 25.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(MoodScale.labelRes(entry.score)), style = MaterialTheme.typography.bodyLarge)
                Text(
                    formatMoodDateTime(entry.recordedAt, Locale.current.language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val sourceLabel = stringResource(moodSourceLabel(entry.source))
            Text(
                sourceLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
