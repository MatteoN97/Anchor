package ch.bfh.anchor.ui.mood

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
                        Text(if (activePanel == null) "Back" else "Close")
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
                        "How are you feeling?",
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
                            "Saved. That's all you need to do.",
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
                        .clickable(onClickLabel = "Close panel") { closePanel() }
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
                                                            if (showFullHistory) "Hide entries"
                                                            else "View entries · $historyPeriodDays ${if (historyPeriodDays == 1) "day" else "days"}"
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
                                                    if (entries.isEmpty()) "Your history will appear after your first check-in."
                                                    else "No check-ins in this period.",
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
                                                        "Allow notifications",
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Box {
                                                        IconButton(
                                                            onClick = { notificationInfoExpanded = true },
                                                            modifier = Modifier.size(40.dp).semantics {
                                                                contentDescription = "Why allow notifications?"
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
                                                                "Reminders are completely optional. They can gently prompt you to check in during the hours you choose. You can leave them off and simply open this page whenever you want to record how you feel.",
                                                                modifier = Modifier.padding(16.dp),
                                                                style = MaterialTheme.typography.bodyMedium
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    if (remindersEnabled) "Reminders are on"
                                                    else "Off — check in here whenever you like",
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
                                                            testFeedback = "Test sent. Tap a mood in the notification, then open Mood over time."
                                                        } else {
                                                            permissionWarning = true
                                                        }
                                                    }
                                                },
                                                enabled = remindersEnabled
                                            ) {
                                                Text("Send test notification")
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
                                                    "Notifications are blocked. Allow Anchor notifications in Android settings.",
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
                                                        "Start  ${MoodReminderSchedule.timeLabel(startMinutes)}",
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                    Text(
                                                        "End  $endLabel",
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
                                                    Text("Repeat", style = MaterialTheme.typography.bodyMedium)
                                                    Box {
                                                        OutlinedButton(
                                                            onClick = { frequencyMenuExpanded = true }
                                                        ) {
                                                            Text(
                                                                "Every $intervalHours ${if (intervalHours == 1) "hour" else "hours"}  ▾"
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
                                                                            "Every $option ${if (option == 1) "hour" else "hours"}"
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
                                                        "Uses your phone's local time. Android may delay reminders."
                                                    else
                                                        "Choose your preferred times here; turn on notifications whenever you're ready.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else if (!reminderController.supported) {
                                        item {
                                            Text(
                                                "Scheduled notifications aren't available on this device yet.",
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
                    Text("Mood over time", maxLines = 1)
                }
            }
        )
        FilterChip(
            modifier = Modifier.weight(1f),
            selected = selected == MoodPanel.NOTIFICATIONS,
            onClick = { onSelect(MoodPanel.NOTIFICATIONS) },
            label = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Notifications", maxLines = 1)
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
                Text(MoodScale.label(entry.score), style = MaterialTheme.typography.bodyLarge)
                Text(
                    formatMoodDateTime(entry.recordedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val sourceLabel = when (entry.source) {
                MoodEntry.SOURCE_TEST_NOTIFICATION -> "Test"
                MoodEntry.SOURCE_NOTIFICATION -> "Reminder"
                MoodEntry.SOURCE_CRISIS -> "After support"
                else -> "In app"
            }
            Text(
                sourceLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
