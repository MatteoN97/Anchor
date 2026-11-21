package ch.bfh.anchor.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import ch.bfh.anchor.ui.theme.AnchorPalette
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.platform.currentHour
import ch.bfh.anchor.platform.daysSince
import ch.bfh.anchor.platform.formatLongDay
import ch.bfh.anchor.platform.nowMillis
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.settings.SettingsManager
import ch.bfh.anchor.ui.adaptive.AnchorButton
import ch.bfh.anchor.ui.adaptive.AnchorCardButton
import ch.bfh.anchor.ui.components.IconBadge
import ch.bfh.anchor.ui.components.calmBackdrop
import ch.bfh.anchor.ui.components.calmCardColor
import ch.bfh.anchor.ui.icons.AnchorIcons
import ch.bfh.anchor.ui.theme.AnchorTheme
import ch.bfh.anchor.viewmodel.AnchorViewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen(
    vm: AnchorViewModel,
    settingsManager: SettingsManager,
    onSosClick: () -> Unit,
    onBreathingClick: () -> Unit,
    onMoodClick: () -> Unit
) {
    val userName by settingsManager.userName.collectAsState()

    val tools by vm.tools.collectAsState()
    val notes by vm.notes.collectAsState()
    val crisisCheckIns by vm.crisisCheckIns.collectAsState()

    val lastCrisisDays: Int? = remember(crisisCheckIns) {
        crisisCheckIns.maxByOrNull { it.date }?.let { daysSince(it.date) }
    }
    val lastCrisisText: String? = when (lastCrisisDays) {
        null -> null
        0 -> stringResource(Res.string.home_last_crisis_today)
        1 -> stringResource(Res.string.home_last_crisis_yesterday)
        else -> pluralStringResource(
            Res.plurals.home_last_crisis_days,
            lastCrisisDays,
            lastCrisisDays
        )
    }

    HomeScreenContent(
        greeting = greeting(userName),
        dateLine = remember { formatLongDay(nowMillis()) },
        toolsCount = tools.size,
        notesCount = notes.size,
        crisisCount = crisisCheckIns.size,
        lastCrisisText = lastCrisisText,
        onSosClick = onSosClick,
        onBreathingClick = onBreathingClick,
        onMoodClick = onMoodClick
    )
}

/** "Good evening, Matteo", or without the name when none is set. */
@Composable
private fun greeting(userName: String): String {
    val hour = remember { currentHour() }
    val named = userName.isNotBlank()
    return when (hour) {
        in 5..11 -> if (named) stringResource(Res.string.home_greeting_morning_name, userName)
        else stringResource(Res.string.home_greeting_morning)
        in 12..17 -> if (named) stringResource(Res.string.home_greeting_afternoon_name, userName)
        else stringResource(Res.string.home_greeting_afternoon)
        else -> if (named) stringResource(Res.string.home_greeting_evening_name, userName)
        else stringResource(Res.string.home_greeting_evening)
    }
}

@Composable
private fun HomeScreenContent(
    greeting: String,
    dateLine: String,
    toolsCount: Int,
    notesCount: Int,
    crisisCount: Int,
    lastCrisisText: String?,
    onSosClick: () -> Unit,
    onBreathingClick: () -> Unit,
    onMoodClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .calmBackdrop(strength = 0.9f, reach = 0.5f)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))

        // Header
        Text(
            text = dateLine.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = colors.primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineLarge,
            color = colors.onBackground,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.home_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        // Your space
        SectionLabel(stringResource(Res.string.home_section_space))
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                icon = AnchorIcons.Favorite,
                value = toolsCount,
                label = stringResource(Res.string.home_stat_tools),
                tint = colors.primary,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            StatTile(
                icon = AnchorIcons.SelfImprovement,
                value = crisisCount,
                label = stringResource(Res.string.home_stat_sessions),
                tint = colors.secondary,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            StatTile(
                icon = AnchorIcons.EditNote,
                value = notesCount,
                label = stringResource(Res.string.home_stat_notes),
                tint = colors.tertiary,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        if (lastCrisisText != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = lastCrisisText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        // For right now
        SectionLabel(stringResource(Res.string.home_section_now))
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                icon = AnchorIcons.Air,
                title = stringResource(Res.string.home_breathing_title),
                subtitle = stringResource(Res.string.home_breathing_subtitle),
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                onClick = onBreathingClick,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            ActionTile(
                icon = AnchorIcons.Insights,
                title = stringResource(Res.string.home_mood_title),
                subtitle = stringResource(Res.string.home_mood_subtitle),
                containerColor = colors.secondaryContainer.takeIfOpaque()
                    ?: if (colors.background.luminance() > 0.5f) AnchorPalette.Lavender90 else AnchorPalette.Lavender30,
                contentColor = if (colors.secondaryContainer.takeIfOpaque() != null) colors.onSecondaryContainer
                else colors.onBackground,
                onClick = onMoodClick,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        Spacer(Modifier.height(28.dp))

        SosCard(onSosClick = onSosClick)

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * On iOS secondaryContainer is a translucent system fill meant for controls; a tile needs a solid
 * colour, so fall back in that case.
 */
private fun Color.takeIfOpaque(): Color? = takeIf { it.alpha >= 0.99f }

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = 4.dp, bottom = 10.dp)
            .semantics { heading() }
    )
}

@Composable
private fun StatTile(
    icon: ImageVector,
    value: Int,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = calmCardColor(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconBadge(
                icon = icon,
                containerColor = tint.copy(alpha = 0.14f),
                contentColor = tint,
                size = 34.dp,
                iconSize = 18.dp
            )
            Column {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnchorCardButton(
        onClick = onClick,
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(
                icon = icon,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                contentColor = contentColor
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = contentColor)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.75f)
                )
            }
        }
    }
}

/**
 * The way into the crisis flow. Warm rather than alarming, but the strongest colour on the page
 * and a full-width target, so it is found without looking for it.
 */
@Composable
private fun SosCard(onSosClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.errorContainer,
        contentColor = colors.onErrorContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = AnchorIcons.SelfImprovement,
                    containerColor = colors.error.copy(alpha = 0.14f),
                    contentColor = colors.onErrorContainer
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.home_overwhelmed),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(Res.string.home_sos_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }
            AnchorButton(
                onClick = onSosClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                containerColor = colors.error,
                contentColor = colors.onError,
                shape = CircleShape
            ) {
                Text(
                    text = stringResource(Res.string.home_sos_button),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AnchorTheme {
        HomeScreenContent(
            greeting = "Good evening, Matteo",
            dateLine = "Saturday, 21 November",
            toolsCount = 4,
            notesCount = 12,
            crisisCount = 2,
            lastCrisisText = "Last crisis session: 3 days ago",
            onSosClick = {},
            onBreathingClick = {},
            onMoodClick = {}
        )
    }
}
