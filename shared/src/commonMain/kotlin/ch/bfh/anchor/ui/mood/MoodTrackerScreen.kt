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
import ch.bfh.anchor.viewmodel.AnchorViewModel
import kotlin.math.roundToInt

/** Only one bottom tab can be open. The default (null) is the quick check-in landing page. */
private enum class MoodPanel { HISTORY, NOTIFICATIONS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MoodTrackerScreen(
    vm: AnchorViewModel,
    settingsManager: SettingsManager,
    reminderController: MoodReminderController,
    onBack: () -> Unit
) {
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
            if (settingsManager.moodReminderEndMinutes >= 1439) 48
            else settingsManager.moodReminderEndMinutes / 30
        )
    }
    var intervalHours by remember { mutableIntStateOf(settingsManager.moodReminderIntervalHours) }
    var frequencyMenuExpanded by remember { mutableStateOf(false) }
    var notificationInfoExpanded by remember { mutableStateOf(false) }
    var permissionWarning by remember { mutableStateOf(false) }
    var testFeedback by remember { mutableStateOf<String?>(null) }
    // Resolved up front: they are set from click handlers, which cannot load resources.
    val testSentText = stringResource(Res.string.mood_test_sent)
    val whyNotificationsLabel = stringResource(Res.string.mood_why_notifications)

    val startMinutes = startSlot * 30
    val endMinutes = if (endSlot == 48) 1439 else endSlot * 30
    val endLabel = if (endSlot == 48) "00:00" else MoodReminderSchedule.timeLabel(endMinutes)

    fun persistAndReschedule() {
        settingsManager.moodReminderStartMinutes = startMinutes
        settingsManager.moodReminderEndMinutes = endMinutes
        settingsManager.moodReminderIntervalHours = intervalHours
        if (remindersEnabled) {
            reminderController.configure(true, startMinutes, endMinutes, intervalHours) { success ->
                permissionWarning = !success
            }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    TextButton(onClick = { if (activePanel == null) onBack() else closePanel() }) {
                        Text(stringResource(if (activePanel == null) Res.string.common_back else Res.string.common_close))
                    }
                }
            )
        }
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

                // A bottom sheet: the open panel grows upward from the bottom edge.
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth().height(panelHeight),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 5.dp,
                    shadowElevation = 7.dp
                ) {
                    Column {
                        // The very same two tab-style buttons move to the TOP when a
                        // section opens. Tap the selected tab again to close it.
                        MoodPanelTabs(
                            selected = activePanel,
                            onSelect = ::togglePanel
                        )
                        HorizontalDivider()

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
                        LazyColumn(
                            state = panelListState,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentPadding = PaddingValues(
                                start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when (activePanel) {
                                MoodPanel.HISTORY -> {
                                    item {
                                        MoodHistoryChart(
                                            entries = entries,
                                            periodDays = historyPeriodDays,
                                            now = historyNow,
                                            onPeriodDaysChange = { historyPeriodDays = it }
                                        )
                                    }
                                    stickyHeader(key = "history_entries_toggle") {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            tonalElevation = if (showFullHistory) 2.dp else 0.dp
                                        ) {
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TextButton(
                                                        onClick = { showFullHistory = !showFullHistory }
                                                    ) {
                                                        Text(
                                                            if (showFullHistory) stringResource(Res.string.mood_hide_entries)
                                                            else pluralStringResource(Res.plurals.mood_view_entries, historyPeriodDays, historyPeriodDays)
                                                        )
                                                    }
                                                }
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outlineVariant
                                                )
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
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            items(matchingHistory, key = { it.id }) { entry ->
                                                MoodHistoryRow(entry)
                                            }
                                        }
                                    }
                                }

                                MoodPanel.NOTIFICATIONS -> {
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
                                                onCheckedChange = { enabled ->
                                                    permissionWarning = false
                                                    testFeedback = null
                                                    reminderController.configure(
                                                        enabled, startMinutes, endMinutes, intervalHours
                                                    ) { success ->
                                                        if (success) remindersEnabled = enabled
                                                        else permissionWarning = true
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    if (reminderController.supported) {
                                        // Keep the preview near its opt-in control. Avoid
                                        // offering a notification while reminders are disabled.
                                        item {
                                            OutlinedButton(
                                                onClick = {
                                                    testFeedback = null
                                                    permissionWarning = false
                                                    reminderController.sendTestNotification { success ->
                                                        if (success) {
                                                            testFeedback = testSentText
                                                        } else {
                                                            permissionWarning = true
                                                        }
                                                    }
                                                },
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
                                                        startSlot = range.start.roundToInt().coerceIn(0, 47)
                                                        endSlot = range.endInclusive.roundToInt()
                                                            .coerceIn(startSlot, 48)
                                                    },
                                                    valueRange = 0f..48f,
                                                    steps = 47,
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
                                    } else if (!reminderController.supported) {
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

@Composable
private fun MoodPanelTabs(
    selected: MoodPanel?,
    onSelect: (MoodPanel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            modifier = Modifier.weight(1f),
            selected = selected == MoodPanel.HISTORY,
            onClick = { onSelect(MoodPanel.HISTORY) },
            label = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.mood_tab_history), maxLines = 1)
                }
            }
        )
        FilterChip(
            modifier = Modifier.weight(1f),
            selected = selected == MoodPanel.NOTIFICATIONS,
            onClick = { onSelect(MoodPanel.NOTIFICATIONS) },
            label = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.mood_tab_notifications), maxLines = 1)
                }
            }
        )
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
