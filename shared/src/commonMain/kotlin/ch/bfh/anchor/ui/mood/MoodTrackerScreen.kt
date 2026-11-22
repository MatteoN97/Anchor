package ch.bfh.anchor.ui.mood

import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import ch.bfh.anchor.ui.adaptive.AnchorTextButton
import ch.bfh.anchor.ui.adaptive.Chevron
import ch.bfh.anchor.ui.adaptive.adaptiveSection
import ch.bfh.anchor.ui.theme.LocalIsIos
import ch.bfh.anchor.viewmodel.AnchorViewModel
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.rememberLazyListState
import ch.bfh.anchor.platform.formatTimeOfDay

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
    var showFullHistory by remember { mutableStateOf(false) }
    // The graph and expanded entry list share a single selected time range.
    var historyPeriod by remember { mutableStateOf(MoodPeriod.Today) }
    val matchingHistory = remember(entries, historyPeriod, clock) {
        moodEntriesInPeriod(entries, historyPeriod, clock).asReversed()
    }

    // Reminders are set up in Settings.
    val backLabel = stringResource(Res.string.common_back)

    Scaffold(
        topBar = {
            if (isIos) {
                IosBackBar(label = backLabel, onClick = onBack)
            } else {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text(backLabel) }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // The mood chart (6 hours / Today / 7 / 30 days) on top, the check-in under it,
            // so a tap shows up as a new dot right away, then the entries of the chosen period.
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
        }
    }
}

// ── iOS ──────────────────────────────────────────────────────────────────────

/** The navigation bar of a pushed iOS screen: a tinted "‹ Back" at the leading edge. */
@Composable
private fun IosBackBar(label: String, onClick: () -> Unit) {
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
            // The disclosure chevron, mirrored to point back.
            Chevron(modifier = Modifier.graphicsLayer(scaleX = -1f), color = tint)
            Spacer(Modifier.width(6.dp))
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
