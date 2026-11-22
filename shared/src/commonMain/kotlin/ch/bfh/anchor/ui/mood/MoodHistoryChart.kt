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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.MoodEntry
import ch.bfh.anchor.model.MoodScale
import ch.bfh.anchor.platform.formatMoodDateTime
import ch.bfh.anchor.platform.localDayBounds
import ch.bfh.anchor.platform.formatTimeOfDay
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
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
    MoodEntry.SOURCE_SAMPLE -> Res.string.mood_source_sample
    else -> Res.string.mood_source_app
}

private const val HOUR_MILLIS = 3_600_000L

/** The ranges offered above the chart, shortest first. */
internal enum class MoodPeriod(val days: Int) {
    SixHours(0), Today(1), Week(7), Month(30);

    /** Laid out by clock time, with hour labels, instead of "N days ago … Now". */
    val clockAxis: Boolean get() = this == SixHours || this == Today
}

/**
 * The time span a period covers, as start (inclusive) and end (exclusive) epoch millis. Today is
 * the calendar day, midnight to midnight, so the chart can lay check-ins out by time of day; the
 * other periods roll back from [now].
 */
internal fun moodPeriodBounds(period: MoodPeriod, now: Long): Pair<Long, Long> = when (period) {
    MoodPeriod.Today -> localDayBounds(now)
    MoodPeriod.SixHours -> (now - 6 * HOUR_MILLIS) to now + 1
    else -> (now - period.days.toLong() * DAY_MILLIS) to now + 1
}

/** Use the same exact period for graph dots and the expanded history list. */
internal fun moodEntriesInPeriod(entries: List<MoodEntry>, period: MoodPeriod, now: Long): List<MoodEntry> {
    val (start, end) = moodPeriodBounds(period, now)
    return entries.filter { it.recordedAt in start until end }.sortedBy { it.recordedAt }
}

/**
 * Vertical grid lines of a clock-time axis, as (time, label or null). Today: midnight to midnight
 * every 6 hours. Six hours: every full hour, labelled every second one so the labels fit.
 */
private fun clockTicks(period: MoodPeriod, start: Long, end: Long): List<Pair<Long, String?>> =
    if (period == MoodPeriod.Today) {
        (0..24 step 6).map { hour ->
            start + hour * HOUR_MILLIS to (if (hour < 10) "0$hour" else "$hour") + ":00"
        }
    } else {
        // Full hours in local time: count from the start of the local day.
        val dayStart = localDayBounds(start).first
        val first = dayStart + ((start - dayStart + HOUR_MILLIS - 1) / HOUR_MILLIS) * HOUR_MILLIS
        generateSequence(first) { it + HOUR_MILLIS }.takeWhile { it < end }
            .mapIndexed { i, t -> t to (if (i % 2 == 0) formatTimeOfDay(t) else null) }
            .toList()
    }
private val ChartHeight = 200.dp
private val PlotMargin = 14.dp
/** Room under the plot for the hour labels of the day view. */
private val HourLabelBand = 20.dp

/** Where things go on the canvas; shared by drawing and tap handling so they cannot drift. */
private class ChartGeometry(width: Float, height: Float, margin: Float, bottomBand: Float,
                            private val start: Long, end: Long) {
    val left = margin
    val right = width - margin
    val top = margin
    val bottom = height - margin - bottomBand
    private val span = (end - start).coerceAtLeast(1L).toFloat()
    fun x(time: Long) = left + ((time - start).toFloat() / span) * (right - left)
    fun y(score: Int) = top + ((5 - score) / 4f) * (bottom - top)
    fun point(entry: MoodEntry) = Offset(x(entry.recordedAt), y(entry.score))
}

/**
 * A true timestamped trend: each dot is an actual check-in, not a daily average. The six-hour and
 * today views run across clock hours, so the ups and downs of a day read left to right.
 */
@Composable
internal fun MoodHistoryChart(
    entries: List<MoodEntry>,
    period: MoodPeriod,
    now: Long,
    onPeriodChange: (MoodPeriod) -> Unit
) {
    val visible = remember(entries, period, now) {
        moodEntriesInPeriod(entries, period, now)
    }
    val isIos = LocalIsIos.current

    val periods = MoodPeriod.entries
    val periodLabels = periods.map {
        when (it) {
            MoodPeriod.SixHours -> stringResource(Res.string.mood_period_six_hours)
            MoodPeriod.Today -> stringResource(Res.string.mood_period_today)
            else -> pluralStringResource(Res.plurals.mood_period_days, it.days, it.days)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // iOS: a segmented control across the width, as Health and Screen Time use for ranges.
        if (isIos) AdaptiveSegmentedControl(
            options = periodLabels,
            selectedIndex = periods.indexOf(period),
            onSelect = { onPeriodChange(periods[it]) }
        ) else Row(
            // Scrolls sideways rather than wrapping if a translation makes the four chips too wide.
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
        ) {
            periods.forEachIndexed { index, option ->
                FilterChip(selected = period == option, onClick = {
                    onPeriodChange(option)
                }, label = { Text(periodLabels[index]) })
            }
        }
        Text(stringResource(Res.string.mood_chart_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        MoodTimelineCard(visible = visible, period = period, now = now)
        Text(stringResource(Res.string.mood_chart_count, visible.size, entries.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * The chart itself in its card: one dot per check-in over [period] ending at [now] (clock hours
 * for six hours and today), the tapped (or latest) check-in described underneath. [visible] must
 * already be limited to the period, see [moodEntriesInPeriod].
 */
@Composable
internal fun MoodTimelineCard(
    visible: List<MoodEntry>,
    period: MoodPeriod,
    now: Long,
    chartHeight: Dp = ChartHeight
) {
    var selectedId by remember(period) { mutableStateOf<String?>(null) }
    val (start, end) = remember(period, now) { moodPeriodBounds(period, now) }
    val selected = visible.firstOrNull { it.id == selectedId } ?: visible.lastOrNull()
    val isIos = LocalIsIos.current
    val dayView = period.clockAxis
    val ticks = remember(period, start, end) { if (dayView) clockTicks(period, start, end) else emptyList() }
    val pointColor = MaterialTheme.colorScheme.primary
    // The ring around each dot matches the card it sits on.
    val backgroundColor = MaterialTheme.colorScheme.run { if (isIos) surfaceVariant else surface }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisLabelStyle = MaterialTheme.typography.labelSmall.copy(color = axisLabelColor)
    val textMeasurer = rememberTextMeasurer()
    val bottomBand = if (dayView) HourLabelBand else 0.dp
    val canvasHeight = chartHeight + bottomBand

    ChartCard(isIos) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.width(78.dp).height(canvasHeight)
                        .padding(top = PlotMargin - 8.dp, bottom = PlotMargin - 8.dp + bottomBand),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(5, 3, 1).forEach { score ->
                        Text("$score  " + stringResource(MoodScale.labelRes(score)),
                            style = MaterialTheme.typography.labelSmall,
                            color = axisLabelColor, maxLines = 1)
                    }
                }
                Canvas(
                    modifier = Modifier.weight(1f).height(canvasHeight)
                        .pointerInput(visible, start, end, dayView) {
                            detectTapGestures { tap ->
                                if (visible.isEmpty()) return@detectTapGestures
                                val g = ChartGeometry(size.width.toFloat(), size.height.toFloat(),
                                    PlotMargin.toPx(), bottomBand.toPx(), start, end)
                                val nearest = visible.minByOrNull { (g.point(it) - tap).getDistance() }
                                if (nearest != null &&
                                    (g.point(nearest) - tap).getDistance() <= 28.dp.toPx()) {
                                    selectedId = nearest.id
                                }
                            }
                        }
                ) {
                    val g = ChartGeometry(size.width, size.height,
                        PlotMargin.toPx(), bottomBand.toPx(), start, end)
                    val hairline = 1.dp.toPx()
                    for (score in 1..5) {
                        drawLine(gridColor, Offset(g.left, g.y(score)), Offset(g.right, g.y(score)),
                            strokeWidth = hairline)
                    }
                    if (dayView) {
                        // Faint vertical lines on the clock hours, labelled underneath.
                        ticks.forEach { (time, text) ->
                            val x = g.x(time).coerceAtMost(g.right)
                            drawLine(gridColor.copy(alpha = gridColor.alpha * 0.5f),
                                Offset(x, g.top), Offset(x, g.bottom), strokeWidth = hairline)
                            if (text != null) {
                                val label = textMeasurer.measure(text, axisLabelStyle)
                                val labelX = (x - label.size.width / 2f)
                                    .coerceIn(0f, size.width - label.size.width)
                                drawText(label, topLeft = Offset(labelX, g.bottom + PlotMargin.toPx() - 4.dp.toPx()))
                            }
                        }
                        // Where the day is now: nothing to the right of it can have happened yet.
                        // (In the six-hour view "now" is the right edge.)
                        if (period == MoodPeriod.Today && now in start until end) {
                            val nowX = g.x(now)
                            drawLine(selectedColor.copy(alpha = 0.55f), Offset(nowX, g.top), Offset(nowX, g.bottom),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
                        }
                    }
                    if (visible.size > 1) {
                        val line = Path().apply {
                            visible.forEachIndexed { index, entry ->
                                val p = g.point(entry)
                                if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                            }
                        }
                        // A soft wash under the line so the shape of the day reads at a glance.
                        val area = Path().apply {
                            addPath(line)
                            lineTo(g.point(visible.last()).x, g.bottom)
                            lineTo(g.point(visible.first()).x, g.bottom)
                            close()
                        }
                        drawPath(area, Brush.verticalGradient(
                            listOf(pointColor.copy(alpha = 0.22f), pointColor.copy(alpha = 0f)),
                            startY = g.top, endY = g.bottom
                        ))
                        drawPath(line, pointColor, style = Stroke(
                            width = 2.dp.toPx(), cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ))
                    }
                    visible.forEach { entry ->
                        val p = g.point(entry)
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
            if (!dayView) {
                Row(modifier = Modifier.fillMaxWidth().padding(start = 78.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(pluralStringResource(Res.plurals.mood_chart_days_ago, period.days, period.days),
                        style = MaterialTheme.typography.labelSmall, color = axisLabelColor)
                    Text(stringResource(Res.string.mood_chart_now),
                        style = MaterialTheme.typography.labelSmall, color = axisLabelColor)
                }
            }
            if (visible.isEmpty()) {
                Text(when (period) {
                        MoodPeriod.SixHours -> stringResource(Res.string.mood_chart_empty_six_hours)
                        MoodPeriod.Today -> stringResource(Res.string.mood_chart_empty_today)
                        else -> pluralStringResource(Res.plurals.mood_chart_empty_days, period.days, period.days)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
