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
import ch.bfh.anchor.model.MoodCheckIn
import ch.bfh.anchor.model.MoodCheckInResult
import ch.bfh.anchor.model.MoodEntry
import kotlinx.coroutines.delay
import ch.bfh.anchor.model.MoodReminderSchedule
import ch.bfh.anchor.model.MoodScale
import ch.bfh.anchor.notifications.MoodReminderController
import ch.bfh.anchor.platform.formatMoodDateTime
import ch.bfh.anchor.platform.nowMillis
import ch.bfh.anchor.settings.SettingsManager
import ch.bfh.anchor.ui.adaptive.AdaptiveActionRow
import ch.bfh.anchor.ui.adaptive.AdaptiveListRow
import ch.bfh.anchor.ui.adaptive.AdaptivePicker
import ch.bfh.anchor.ui.adaptive.AdaptiveSwitch
import ch.bfh.anchor.ui.adaptive.AnchorTextButton
import ch.bfh.anchor.ui.adaptive.Chevron
import ch.bfh.anchor.ui.adaptive.adaptiveSection
import ch.bfh.anchor.ui.theme.LocalIsIos
import ch.bfh.anchor.viewmodel.AnchorViewModel
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ch.bfh.anchor.platform.formatTimeOfDay
import kotlinx.coroutines.launch

/** The sheet over the landing page; the history and check-in live on the page itself. */
private enum class MoodPanel { NOTIFICATIONS }

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
    // The clock the hourly limit is judged by. Ticks while a check-in is blocking, so the faces
    // unlock on their own when the hour is over.
    var clock by remember { mutableLongStateOf(nowMillis()) }
    // What the last tap did, and how many taps so far: the message animates on every tap, also
    // when two taps give the same result.
    var lastResult by remember { mutableStateOf<MoodCheckInResult?>(null) }
    var taps by remember { mutableIntStateOf(0) }
    // The result arrives a moment before the database flow does; use it meanwhile.
    val blocking = MoodCheckIn.blocking(entries, clock)
        ?: lastResult?.entry?.takeIf { clock < MoodCheckIn.nextAllowedAt(it) }
    // Ticks every 30 s (the "now" line of today's chart moves) and exactly when a blocking
    // check-in expires, so the faces unlock on time.
    LaunchedEffect(blocking?.id) {
        while (true) {
            val left = blocking?.let { MoodCheckIn.nextAllowedAt(it) - nowMillis() }
            delay(if (left != null && left > 0) minOf(left, 30_000L) else 30_000L)
            clock = nowMillis()
        }
    }
    var activePanel by remember { mutableStateOf<MoodPanel?>(null) }
    var showFullHistory by remember { mutableStateOf(false) }
    // The graph and expanded entry list share a single selected time range.
    var historyPeriod by remember { mutableStateOf(MoodPeriod.Today) }
    val matchingHistory = remember(entries, historyPeriod, clock) {
        moodEntriesInPeriod(entries, historyPeriod, clock).asReversed()
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

    // How far the open panel has been pulled down, in px. 0 = fully open.
    val scope = rememberCoroutineScope()
    var panelOffset by remember { mutableFloatStateOf(0f) }
    var panelHeightPx by remember { mutableFloatStateOf(0f) }

    fun closePanel() {
        activePanel = null
        frequencyMenuExpanded = false
        notificationInfoExpanded = false
    }

    /** Slides the panel down out of sight, then closes it. */
    fun dismissPanel(velocity: Float = 0f) {
        if (activePanel == null) return
        scope.launch {
            animate(panelOffset, panelHeightPx.coerceAtLeast(panelOffset + 1f), velocity, tween(200)) { value, _ ->
                panelOffset = value
            }
            closePanel()
        }
    }

    /** After a drag: close if pulled far or flung down, otherwise spring back open. */
    suspend fun settlePanel(velocity: Float) {
        if (panelOffset <= 0f) return
        if (panelOffset > panelHeightPx * 0.3f || velocity > 1200f) dismissPanel(velocity)
        else animate(panelOffset, 0f, velocity, spring()) { value, _ -> panelOffset = value }
    }

    // Lets the panel content scroll as usual and hands the rest of a downward drag to the panel:
    // at the top of the list, pulling down moves the whole sheet, as a system sheet does.
    val panelScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && panelOffset > 0f) {
                    val used = maxOf(available.y, -panelOffset)
                    panelOffset += used
                    return Offset(0f, used)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && source == NestedScrollSource.UserInput) {
                    panelOffset += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (panelOffset <= 0f) return Velocity.Zero
                settlePanel(available.y)
                return available
            }
        }
    }

    fun togglePanel(panel: MoodPanel) {
        if (activePanel == panel) dismissPanel()
        else {
            activePanel = panel
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
    val onBackPressed = { if (activePanel == null) onBack() else dismissPanel() }
    val notificationsLabel = stringResource(Res.string.mood_tab_notifications)

    // System back (Android gesture, iOS edge swipe) closes an open panel before leaving the screen.
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = activePanel != null,
        onBackCompleted = { dismissPanel() }
    )

    Scaffold(
        topBar = {
            if (isIos) {
                IosBackBar(label = backLabel, showChevron = activePanel == null, onClick = onBackPressed) {
                    if (activePanel == null) {
                        AnchorTextButton(onClick = { togglePanel(MoodPanel.NOTIFICATIONS) }) {
                            Text(notificationsLabel, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        TextButton(onClick = onBackPressed) { Text(backLabel) }
                    },
                    actions = {
                        if (activePanel == null) {
                            TextButton(onClick = { togglePanel(MoodPanel.NOTIFICATIONS) }) { Text(notificationsLabel) }
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Landing page: the mood chart (Today / 7 / 30 days) on top, the check-in under it,
            // so a tap shows up as a new dot right away, then the entries of the chosen period.
            // It stays visible behind the notifications sheet.
            // iOS sections bring their own side margins; other items get them here.
            val itemInset = Modifier.padding(horizontal = if (isIos) 16.dp else 20.dp)
            val landingState = rememberLazyListState()
            LazyColumn(
                state = landingState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                item(key = "chart") {
                    Box(itemInset) {
                        MoodHistoryChart(
                            entries = entries,
                            period = historyPeriod,
                            now = clock,
                            onPeriodChange = { historyPeriod = it }
                        )
                    }
                }
                item(key = "check_in") {
                    Column(
                        modifier = itemInset.fillMaxWidth().padding(top = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            stringResource(Res.string.mood_question),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        MoodChoices(
                            selectedScore = blocking?.score,
                            onScoreSelected = { score ->
                                vm.recordMood(score) { result ->
                                    clock = nowMillis()
                                    lastResult = result
                                    taps++
                                }
                            }
                        )
                        // Space reserved up front so the faces do not jump when the text changes.
                        AnimatedContent(
                            targetState = Triple(blocking, lastResult, taps),
                            contentKey = { (entry, _, tapCount) -> "${entry?.id}-$tapCount" },
                            modifier = Modifier.heightIn(min = 76.dp),
                            transitionSpec = {
                                (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 4 })
                                    .togetherWith(fadeOut(tween(120)))
                            }
                        ) { (entry, result, _) ->
                            // A result from an earlier window no longer describes the current check-in.
                            CheckInStatus(entry = entry, result = result?.takeIf { it.entry.id == entry?.id }, now = clock)
                        }
                    }
                }
                item(key = "entries_toggle") {
                    Column(itemInset) {
                        if (!isIos) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            AnchorTextButton(onClick = { showFullHistory = !showFullHistory }) {
                                Text(
                                    if (showFullHistory) stringResource(Res.string.mood_hide_entries)
                                    else when (historyPeriod) {
                                        MoodPeriod.SixHours -> stringResource(Res.string.mood_view_entries_six_hours)
                                        MoodPeriod.Today -> stringResource(Res.string.mood_view_entries_today)
                                        else -> pluralStringResource(Res.plurals.mood_view_entries, historyPeriod.days, historyPeriod.days)
                                    }
                                )
                            }
                        }
                    }
                }
                if (showFullHistory) {
                    if (matchingHistory.isEmpty()) {
                        item(key = "entries_empty") {
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
                            Box(itemInset) { MoodHistoryRow(entry) }
                        }
                    }
                }
            }

            if (activePanel != null) {
                // A dismissible layer over the landing page: tapping anywhere OUTSIDE
                // the raised panel closes it without changing the selected mood.
                val maxOffset = constraints.maxHeight.toFloat()
                // Slide up when a panel opens (not when switching between the two panels).
                LaunchedEffect(Unit) {
                    panelOffset = maxOffset
                    animate(maxOffset, 0f, animationSpec = spring(stiffness = 500f, dampingRatio = 0.9f)) { value, _ ->
                        panelOffset = value
                    }
                }
                val openFraction = if (panelHeightPx > 0f)
                    (1f - panelOffset / panelHeightPx).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.26f * openFraction))
                        .clickable(onClickLabel = stringResource(Res.string.mood_close_panel)) { dismissPanel() }
                )

                val maximumPanelHeight = (maxHeight - 80.dp).coerceAtLeast(180.dp)
                val panelHeight = minOf(maximumPanelHeight, 510.dp)

                // A bottom sheet: the open panel grows upward from the bottom edge. On iOS it
                // takes the grey grouped background of a sheet, so its sections stand out.
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .offset { IntOffset(0, panelOffset.roundToInt()) }
                        .onSizeChanged { panelHeightPx = it.height.toFloat() }
                        .fillMaxWidth().height(panelHeight),
                    shape = RoundedCornerShape(
                        topStart = if (isIos) 28.dp else 24.dp,
                        topEnd = if (isIos) 28.dp else 24.dp
                    ),
                    color = MaterialTheme.colorScheme.run { if (isIos) background else surface },
                    tonalElevation = if (isIos) 0.dp else 5.dp,
                    shadowElevation = 7.dp
                ) {
                    Column(Modifier.nestedScroll(panelScroll)) {
                        // The grabber and the title are the sheet's handle: drag them down to close.
                        Column(
                            Modifier.draggable(
                                state = rememberDraggableState { delta ->
                                    panelOffset = (panelOffset + delta).coerceAtLeast(0f)
                                },
                                orientation = Orientation.Vertical,
                                onDragStopped = { velocity -> settlePanel(velocity) }
                            )
                        ) {
                            SheetGrabber()
                            Text(
                                notificationsLabel,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp)
                            )
                        }
                        if (!isIos) HorizontalDivider()

                        val panelListState = remember(activePanel) { LazyListState() }
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
private fun IosBackBar(
    label: String,
    showChevron: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
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
        Spacer(Modifier.weight(1f))
        trailing()
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
 * Under the faces: before a check-in, the once-an-hour rule; after one, when it was saved,
 * when the next one is possible (with a thin bar filling up towards it) and that a tap on
 * another face corrects it.
 */
@Composable
private fun CheckInStatus(entry: MoodEntry?, result: MoodCheckInResult?, now: Long) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (entry == null) {
            Text(
                stringResource(Res.string.mood_check_in_rule),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            return@Column
        }
        val savedAt = formatTimeOfDay(entry.recordedAt)
        val headline = when (result) {
            is MoodCheckInResult.Saved -> stringResource(Res.string.mood_saved_at, savedAt)
            is MoodCheckInResult.Updated -> stringResource(Res.string.mood_updated, stringResource(MoodScale.labelRes(entry.score)))
            is MoodCheckInResult.Unchanged -> stringResource(Res.string.mood_already_saved, savedAt)
            null -> stringResource(Res.string.mood_checked_in_at, savedAt)
        }
        Text(headline, style = MaterialTheme.typography.bodyMedium, color = colors.primary, textAlign = TextAlign.Center)
        val next = MoodCheckIn.nextAllowedAt(entry)
        LinearProgressIndicator(
            progress = { ((now - entry.recordedAt).toFloat() / MoodCheckIn.INTERVAL_MILLIS).coerceIn(0.02f, 1f) },
            modifier = Modifier.width(140.dp).height(4.dp),
            color = colors.primary,
            trackColor = colors.outlineVariant.copy(alpha = 0.5f),
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Text(
            stringResource(Res.string.mood_next_check_in, formatTimeOfDay(next)),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** The pill at the top of a sheet that says it can be dragged. */
@Composable
private fun SheetGrabber() {
    val isIos = LocalIsIos.current
    Box(Modifier.fillMaxWidth().padding(top = if (isIos) 6.dp else 10.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = if (isIos) 36.dp else 32.dp, height = if (isIos) 5.dp else 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.run { if (isIos) outline else onSurfaceVariant.copy(alpha = 0.4f) })
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
