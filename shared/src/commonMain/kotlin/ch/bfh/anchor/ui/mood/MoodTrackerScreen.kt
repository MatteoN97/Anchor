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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.bfh.anchor.model.MoodCheckIn
import ch.bfh.anchor.model.MoodCheckInResult
import ch.bfh.anchor.model.MoodEntry
import kotlinx.coroutines.delay
import ch.bfh.anchor.model.MoodScale
import ch.bfh.anchor.platform.formatMoodDateTime
import ch.bfh.anchor.platform.nowMillis
import ch.bfh.anchor.ui.adaptive.AdaptiveListRow
import ch.bfh.anchor.ui.adaptive.AnchorCardButton
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/** The panel that can be opened over the check-in. The default (null) is the landing page. */
private enum class MoodPanel { HISTORY }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MoodTrackerScreen(
    vm: AnchorViewModel,
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
    val today = remember(entries, clock) { moodEntriesInPeriod(entries, 1, clock) }
    var activePanel by remember { mutableStateOf<MoodPanel?>(null) }
    var showFullHistory by remember { mutableStateOf(false) }
    // The graph and expanded entry list share a single selected time range.
    var historyPeriodDays by remember { mutableIntStateOf(1) }
    val historyNow = remember(entries, historyPeriodDays, activePanel) { nowMillis() }
    val matchingHistory = remember(entries, historyPeriodDays, historyNow) {
        moodEntriesInPeriod(entries, historyPeriodDays, historyNow).asReversed()
    }

    // How far the open panel has been pulled down, in px. 0 = fully open.
    val scope = rememberCoroutineScope()
    var panelOffset by remember { mutableFloatStateOf(0f) }
    var panelHeightPx by remember { mutableFloatStateOf(0f) }

    fun closePanel() {
        activePanel = null
        showFullHistory = false
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
            if (panel == MoodPanel.HISTORY) historyPeriodDays = 1
            showFullHistory = false
        }
    }

    val backLabel = stringResource(if (activePanel == null) Res.string.common_back else Res.string.common_close)
    val onBackPressed = { if (activePanel == null) onBack() else dismissPanel() }

    // System back (Android gesture, iOS edge swipe) closes an open panel before leaving the screen.
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = activePanel != null,
        onBackCompleted = { dismissPanel() }
    )

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
            // Landing page: today's chart on top, the check-in under it, so a tap shows up as a
            // new dot right away. It stays visible behind a panel, so tapping outside a panel
            // returns to the same place.
            Box(
                modifier = Modifier.fillMaxSize().padding(
                    bottom = if (activePanel == null) 65.dp else 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(Res.string.mood_period_today),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MoodTimelineCard(visible = today, periodDays = 1, now = clock, chartHeight = 170.dp)
                    }
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
                        // A result from an earlier hour no longer describes the current check-in.
                        CheckInStatus(entry = entry, result = result?.takeIf { it.entry.id == entry?.id }, now = clock)
                    }
                }
            }

            if (activePanel == null) {
                // The history is collapsed by default; below the centered check-in there is
                // only the button that opens it. Reminders are set up in Settings.
                MoodPanelTabs(
                    selected = null,
                    onSelect = ::togglePanel,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                // A dismissible layer over the landing page: tapping anywhere OUTSIDE
                // the raised panel closes it without changing the selected mood.
                val maxOffset = constraints.maxHeight.toFloat()
                // Slide up when the panel opens.
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
                val panelHeight = minOf(maximumPanelHeight, 590.dp)

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
                        // The grabber and the tab are the sheet's handle: drag them down to
                        // close. The same tab-style button moves to the TOP when the panel
                        // opens; tap it again to close it.
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
                            MoodPanelTabs(
                                selected = activePanel,
                                onSelect = ::togglePanel
                            )
                        }
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
                                                            else if (historyPeriodDays == 1) stringResource(Res.string.mood_view_entries_today)
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

/**
 * Android: a filter chip per panel. iOS: on the landing page, a card to open a panel; inside an
 * open panel, its title.
 */
@Composable
private fun MoodPanelTabs(
    selected: MoodPanel?,
    onSelect: (MoodPanel) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = mapOf(MoodPanel.HISTORY to stringResource(Res.string.mood_tab_history))
    val isIos = LocalIsIos.current

    if (isIos && selected != null) {
        Text(
            labels.getValue(selected),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
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
