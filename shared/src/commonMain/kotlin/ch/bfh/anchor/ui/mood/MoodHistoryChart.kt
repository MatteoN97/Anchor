package ch.bfh.anchor.ui.mood

import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.ui.text.intl.Locale
import org.jetbrains.compose.resources.StringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.MoodEntry
import ch.bfh.anchor.model.MoodScale
import ch.bfh.anchor.platform.formatMoodDateTime
import ch.bfh.anchor.ui.adaptive.AdaptiveSegmentedControl
import ch.bfh.anchor.ui.theme.IosMetrics
import ch.bfh.anchor.ui.theme.LocalIsIos
import androidx.compose.foundation.shape.RoundedCornerShape

private const val DAY_MILLIS = 86_400_000L

/** Where a mood entry came from, as shown next to it in the history. */
internal fun moodSourceLabel(source: String): StringResource = when (source) {
    MoodEntry.SOURCE_TEST_NOTIFICATION -> Res.string.mood_source_test
    MoodEntry.SOURCE_NOTIFICATION -> Res.string.mood_source_reminder
    MoodEntry.SOURCE_CRISIS -> Res.string.mood_source_crisis
    else -> Res.string.mood_source_app
}

/** Use the same exact rolling period for graph dots and the expanded history list. */
internal fun moodEntriesInPeriod(entries: List<MoodEntry>, periodDays: Int, now: Long): List<MoodEntry> {
    val start = now - periodDays.toLong() * DAY_MILLIS
    return entries.filter { it.recordedAt in start..now }.sortedBy { it.recordedAt }
}

/** A true timestamped trend: each dot is an actual check-in, not a daily average. */
@Composable
internal fun MoodHistoryChart(
    entries: List<MoodEntry>,
    periodDays: Int,
    now: Long,
    onPeriodDaysChange: (Int) -> Unit
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val start = now - periodDays.toLong() * DAY_MILLIS
    val visible = remember(entries, periodDays, now) {
        moodEntriesInPeriod(entries, periodDays, now)
    }
    val selected = visible.firstOrNull { it.id == selectedId } ?: visible.lastOrNull()
    val isIos = LocalIsIos.current
    val pointColor = MaterialTheme.colorScheme.primary
    // The ring around each dot matches the card it sits on.
    val backgroundColor = MaterialTheme.colorScheme.run { if (isIos) surfaceVariant else surface }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val selectedColor = MaterialTheme.colorScheme.tertiary

    val periods = listOf(1, 7, 30)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // iOS: a segmented control across the width, as Health and Screen Time use for ranges.
        if (isIos) AdaptiveSegmentedControl(
            options = periods.map { pluralStringResource(Res.plurals.mood_period_days, it, it) },
            selectedIndex = periods.indexOf(periodDays).coerceAtLeast(0),
            onSelect = { selectedId = null; onPeriodDaysChange(periods[it]) }
        ) else Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            FilterChip(selected = periodDays == 1, onClick = {
                selectedId = null; onPeriodDaysChange(1)
            }, label = { Text(pluralStringResource(Res.plurals.mood_period_days, 1, 1)) })
            Spacer(Modifier.width(6.dp))
            FilterChip(selected = periodDays == 7, onClick = {
                selectedId = null; onPeriodDaysChange(7)
            }, label = { Text(pluralStringResource(Res.plurals.mood_period_days, 7, 7)) })
            Spacer(Modifier.width(6.dp))
            FilterChip(selected = periodDays == 30, onClick = {
                selectedId = null; onPeriodDaysChange(30)
            }, label = { Text(pluralStringResource(Res.plurals.mood_period_days, 30, 30)) })
        }
        Text(stringResource(Res.string.mood_chart_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        ChartCard(isIos) {
            Column(modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.width(78.dp).height(200.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("5  " + stringResource(MoodScale.labelRes(5)), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("3  " + stringResource(MoodScale.labelRes(3)), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("1  " + stringResource(MoodScale.labelRes(1)), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Canvas(
                        modifier = Modifier.weight(1f).height(200.dp)
                            .pointerInput(visible, start, now) {
                                detectTapGestures { tap ->
                                    if (visible.isEmpty()) return@detectTapGestures
                                    val margin = 14.dp.toPx()
                                    val plotW = (size.width.toFloat() - margin * 2).coerceAtLeast(1f)
                                    val plotH = (size.height.toFloat() - margin * 2).coerceAtLeast(1f)
                                    val timeSpan = (now - start).coerceAtLeast(1L).toFloat()
                                    fun location(entry: MoodEntry): Offset = Offset(
                                        margin + ((entry.recordedAt - start).toFloat() / timeSpan) * plotW,
                                        margin + ((5 - entry.score) / 4f) * plotH
                                    )
                                    val nearest = visible.minByOrNull { (location(it) - tap).getDistance() }
                                    if (nearest != null &&
                                        (location(nearest) - tap).getDistance() <= 28.dp.toPx()) {
                                        selectedId = nearest.id
                                    }
                                }
                            }
                    ) {
                        val margin = 14.dp.toPx()
                        val plotW = (size.width - margin * 2).coerceAtLeast(1f)
                        val plotH = (size.height - margin * 2).coerceAtLeast(1f)
                        val timeSpan = (now - start).coerceAtLeast(1L).toFloat()
                        fun location(entry: MoodEntry): Offset = Offset(
                            margin + ((entry.recordedAt - start).toFloat() / timeSpan) * plotW,
                            margin + ((5 - entry.score) / 4f) * plotH
                        )
                        for (score in 1..5) {
                            val y = margin + ((5 - score) / 4f) * plotH
                            drawLine(gridColor, Offset(margin, y), Offset(size.width - margin, y),
                                strokeWidth = 1.dp.toPx())
                        }
                        if (visible.size > 1) {
                            val path = Path().apply {
                                visible.forEachIndexed { index, entry ->
                                    val p = location(entry)
                                    if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                }
                            }
                            drawPath(path, pointColor, style = Stroke(
                                width = 2.dp.toPx(), cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            ))
                        }
                        visible.forEach { entry ->
                            val p = location(entry)
                            val isSelected = entry.id == selected?.id
                            if (isSelected) {
                                drawCircle(selectedColor.copy(alpha = 0.2f),
                                    radius = 10.dp.toPx(), center = p)
                            }
                            drawCircle(backgroundColor, radius = 6.dp.toPx(), center = p)
                            drawCircle(if (isSelected) selectedColor else pointColor,
                                radius = if (isSelected) 5.5.dp.toPx() else 4.dp.toPx(), center = p)
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(start = 78.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (periodDays == 1) stringResource(Res.string.mood_chart_24h_ago)
                         else pluralStringResource(Res.plurals.mood_chart_days_ago, periodDays, periodDays),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(Res.string.mood_chart_now), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (visible.isEmpty()) {
                    Text(if (periodDays == 1) stringResource(Res.string.mood_chart_empty_24h)
                         else pluralStringResource(Res.plurals.mood_chart_empty_days, periodDays, periodDays),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    HorizontalDivider()
                    selected?.let { entry ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(MoodScale.emoji(entry.score), style = MaterialTheme.typography.headlineSmall)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(MoodScale.labelRes(entry.score)), style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text(formatMoodDateTime(entry.recordedAt, Locale.current.language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val sourceLabel = stringResource(moodSourceLabel(entry.source))
                            Text(sourceLabel, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Text(stringResource(Res.string.mood_chart_count, visible.size, entries.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Android: an outlined card. iOS: a plain white cell, like a grouped section. */
@Composable
private fun ChartCard(isIos: Boolean, content: @Composable ColumnScope.() -> Unit) {
    if (isIos) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(IosMetrics.CardCorner),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            content = content
        )
    } else {
        OutlinedCard(modifier = Modifier.fillMaxWidth(), content = content)
    }
}
