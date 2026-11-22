package ch.bfh.anchor.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.MoodReminderSchedule
import ch.bfh.anchor.notifications.MoodReminderController
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.settings.SettingsManager
import ch.bfh.anchor.ui.adaptive.AdaptiveActionRow
import ch.bfh.anchor.ui.adaptive.AdaptiveListRow
import ch.bfh.anchor.ui.adaptive.AdaptivePicker
import ch.bfh.anchor.ui.adaptive.AdaptiveSwitch
import ch.bfh.anchor.ui.adaptive.adaptiveSection
import ch.bfh.anchor.ui.theme.LocalIsIos
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Half-hour slots of the reminder window; slot 48 is the end of the day. */
private const val LAST_SLOT = 48

private fun slotMinutes(slot: Int) = if (slot == LAST_SLOT) 1439 else slot * 30
private fun slotLabel(slot: Int) =
    if (slot == LAST_SLOT) "00:00" else MoodReminderSchedule.timeLabel(slot * 30)

/** The mood reminder settings and what the last action reported, kept for the Settings screen. */
class MoodReminderState(
    private val settings: SettingsManager,
    private val controller: MoodReminderController,
    private val testSentText: String
) {
    val supported get() = controller.supported
    var enabled by mutableStateOf(settings.moodRemindersEnabled)
        private set
    var startSlot by mutableIntStateOf(settings.moodReminderStartMinutes / 30)
        private set
    // The midnight at the end of the list is the END of the day, not a second reminder.
    var endSlot by mutableIntStateOf(
        if (settings.moodReminderEndMinutes >= 1439) LAST_SLOT else settings.moodReminderEndMinutes / 30
    )
        private set
    var intervalHours by mutableIntStateOf(settings.moodReminderIntervalHours)
        private set
    var permissionWarning by mutableStateOf(false)
        private set
    var testFeedback by mutableStateOf<String?>(null)
        private set

    fun changeEnabled(value: Boolean) {
        permissionWarning = false
        testFeedback = null
        controller.configure(value, slotMinutes(startSlot), slotMinutes(endSlot), intervalHours) { success ->
            if (success) enabled = value else permissionWarning = true
        }
    }

    fun sendTest() {
        testFeedback = null
        permissionWarning = false
        controller.sendTestNotification { success ->
            if (success) testFeedback = testSentText else permissionWarning = true
        }
    }

    fun setStart(slot: Int) {
        startSlot = slot
        if (endSlot < slot) endSlot = slot
        save()
    }

    fun setEnd(slot: Int) {
        endSlot = slot
        save()
    }

    fun setInterval(hours: Int) {
        intervalHours = hours
        save()
    }

    /** The schedule is kept while reminders are off, and applied at once while they are on. */
    private fun save() {
        val start = slotMinutes(startSlot)
        val end = slotMinutes(endSlot)
        settings.moodReminderStartMinutes = start
        settings.moodReminderEndMinutes = end
        settings.moodReminderIntervalHours = intervalHours
        if (enabled) controller.configure(true, start, end, intervalHours) { success -> permissionWarning = !success }
    }
}

@Composable
fun rememberMoodReminderState(settings: SettingsManager, controller: MoodReminderController): MoodReminderState {
    val testSent = stringResource(if (LocalIsIos.current) Res.string.mood_test_sent_ios else Res.string.mood_test_sent)
    return remember(settings, controller) { MoodReminderState(settings, controller, testSent) }
}

/** Texts for [moodReminderSection], looked up beforehand because list builders cannot. */
class MoodReminderTexts(val title: String, val footer: String)

@Composable
fun moodReminderTexts(state: MoodReminderState): MoodReminderTexts {
    val isIos = LocalIsIos.current
    val footer = if (!state.supported) {
        stringResource(Res.string.mood_notifications_unavailable)
    } else {
        listOfNotNull(
            if (state.permissionWarning) {
                stringResource(if (isIos) Res.string.mood_notifications_blocked_ios else Res.string.mood_notifications_blocked)
            } else state.testFeedback,
            if (state.enabled) {
                stringResource(if (isIos) Res.string.mood_local_time_hint_ios else Res.string.mood_local_time_hint)
            } else stringResource(Res.string.mood_choose_times_hint),
            stringResource(Res.string.mood_notifications_info)
        ).joinToString("\n\n")
    }
    return MoodReminderTexts(stringResource(Res.string.settings_mood_reminders_title), footer)
}

/**
 * Mood check-in reminders: a switch to turn them on, a test notification, and the daily window
 * and frequency as pickers.
 */
fun LazyListScope.moodReminderSection(state: MoodReminderState, texts: MoodReminderTexts, showHeader: Boolean = true) {
    adaptiveSection(
        header = texts.title.takeIf { showHeader },
        footer = texts.footer,
        rows = buildList<@Composable () -> Unit> {
            add {
                AdaptiveListRow(
                    headline = { Text(stringResource(Res.string.mood_allow_notifications)) },
                    supporting = {
                        Text(stringResource(if (state.enabled) Res.string.mood_reminders_on else Res.string.mood_reminders_off))
                    },
                    trailing = {
                        if (state.supported) AdaptiveSwitch(checked = state.enabled, onCheckedChange = state::changeEnabled)
                    },
                    modifier = if (state.supported) {
                        Modifier.toggleable(value = state.enabled, role = Role.Switch, onValueChange = state::changeEnabled)
                    } else Modifier
                )
            }
            if (!state.supported) return@buildList
            if (state.enabled) {
                add { AdaptiveActionRow(stringResource(Res.string.mood_send_test), onClick = state::sendTest) }
            }
            add {
                val slots = (0 until LAST_SLOT).toList()
                SchedulePicker(
                    label = stringResource(Res.string.mood_start_label),
                    options = slots.map(::slotLabel),
                    selected = slotLabel(state.startSlot),
                    onSelect = { label -> state.setStart(slots.first { slotLabel(it) == label }) }
                )
            }
            add {
                // Ends at or after the start; the last option is midnight, the end of the day.
                // A window starting at midnight cannot also end there, or "00:00" would be listed twice.
                val slots = (maxOf(state.startSlot, 1)..LAST_SLOT).toList()
                SchedulePicker(
                    label = stringResource(Res.string.mood_end_label),
                    options = slots.map(::slotLabel),
                    selected = slotLabel(state.endSlot),
                    onSelect = { label -> state.setEnd(slots.first { slotLabel(it) == label }) }
                )
            }
            add {
                val labels = MoodReminderSchedule.intervalsInHours.associateWith {
                    pluralStringResource(Res.plurals.mood_every_hours, it, it)
                }
                SchedulePicker(
                    label = stringResource(Res.string.mood_repeat),
                    options = labels.values.toList(),
                    selected = labels.getValue(state.intervalHours),
                    onSelect = { label -> state.setInterval(labels.entries.first { it.value == label }.key) }
                )
            }
        }
    )
}

/** iOS: a picker row of the grouped section. Android: a dropdown field with the list's margins. */
@Composable
private fun SchedulePicker(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    val isIos = LocalIsIos.current
    AdaptivePicker(
        label = label,
        options = options,
        selected = selected,
        onSelect = onSelect,
        grouped = true,
        modifier = if (isIos) Modifier else Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    )
}
