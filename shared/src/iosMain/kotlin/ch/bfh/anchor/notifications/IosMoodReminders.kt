package ch.bfh.anchor.notifications

import androidx.compose.runtime.mutableIntStateOf
import ch.bfh.anchor.data.AnchorDatabase
import ch.bfh.anchor.model.MoodEntry
import ch.bfh.anchor.model.MoodReminderSchedule
import ch.bfh.anchor.model.MoodScale
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.settings.SettingsManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSDateComponents
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionNone
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationDefaultActionIdentifier
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

private const val CATEGORY_ID = "anchor.mood.check-in"
private const val REMINDER_ID_PREFIX = "anchor.mood.reminder."
private const val TEST_ID = "anchor.mood.test"
private const val ACTION_ID_PREFIX = "anchor.mood.score."
private const val KEY_IS_TEST = "isTest"

/**
 * Mood check-in reminders on iOS, the counterpart of Android's MoodNotifications.
 *
 * Each reminder time is a repeating daily calendar trigger, so iOS delivers them on its own with
 * no background work. Pressing and holding a notification offers the five moods as actions;
 * choosing one records it without opening the app. Tapping the notification opens the tracker.
 */
object IosMoodReminders {
    /** Bumped when a notification is tapped; the app opens the mood tracker in response. */
    val openMoodRequests = mutableIntStateOf(0)

    private val center: UNUserNotificationCenter
        get() = UNUserNotificationCenter.currentNotificationCenter()
    private val scope = MainScope()
    // The notification center only keeps a weak reference to its delegate.
    private val delegate = Delegate()
    private var database: AnchorDatabase? = null

    /**
     * Called once at launch, before the app finishes launching, so a mood chosen in a
     * notification while Anchor was closed is still delivered here.
     */
    fun install(database: AnchorDatabase, settings: SettingsManager) {
        this.database = database
        center.delegate = delegate
        // Rebuild the schedule with the current texts: they are fixed when a reminder is
        // scheduled, so this picks up a language change made since.
        if (settings.moodRemindersEnabled) {
            scope.launch {
                schedule(
                    settings.moodReminderStartMinutes,
                    settings.moodReminderEndMinutes,
                    settings.moodReminderIntervalHours
                )
            }
        }
    }

    /** Asks for permission the first time; afterwards reports the user's earlier answer. */
    internal fun whenAllowed(onComplete: (Boolean) -> Unit, onAllowed: suspend () -> Unit) {
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
            scope.launch { if (granted) onAllowed() else onComplete(false) }
        }
    }

    internal suspend fun schedule(startMinutes: Int, endMinutes: Int, intervalHours: Int) {
        cancel()
        registerCategory()
        val content = content(isTest = false)
        MoodReminderSchedule.slots(startMinutes, endMinutes, intervalHours).forEach { minutes ->
            val time = NSDateComponents().apply {
                hour = (minutes / 60).toLong()
                minute = (minutes % 60).toLong()
            }
            add(
                REMINDER_ID_PREFIX + minutes,
                content,
                UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(time, repeats = true)
            )
        }
    }

    /** Removes every reminder, whichever schedule created it. */
    internal fun cancel() {
        // Slots fall on whole and half hours, so these are all the identifiers there can be.
        val ids = (0 until 24 * 60 step 30).map { REMINDER_ID_PREFIX + it }
        center.removePendingNotificationRequestsWithIdentifiers(ids)
        center.removeDeliveredNotificationsWithIdentifiers(ids)
    }

    /** Delivers a test notification at once; [onComplete] reports whether iOS accepted it. */
    internal suspend fun sendTest(onComplete: (Boolean) -> Unit) {
        registerCategory()
        add(TEST_ID, content(isTest = true), trigger = null, onComplete = onComplete)
    }

    private suspend fun content(isTest: Boolean) = UNMutableNotificationContent().apply {
        setTitle(getString(Res.string.mood_notification_title))
        setBody(getString(Res.string.mood_notification_body_ios))
        setCategoryIdentifier(CATEGORY_ID)
        setSound(UNNotificationSound.defaultSound)
        setUserInfo(mapOf<Any?, Any?>(KEY_IS_TEST to isTest))
    }

    /** The five moods as notification actions, best first, in the current language. */
    private suspend fun registerCategory() {
        val actions = MoodScale.displayScores.map { score ->
            UNNotificationAction.actionWithIdentifier(
                ACTION_ID_PREFIX + score,
                "${MoodScale.emoji(score)} ${getString(MoodScale.labelRes(score))}",
                UNNotificationActionOptionNone // recorded in the background; the app stays closed
            )
        }
        val category = UNNotificationCategory.categoryWithIdentifier(
            CATEGORY_ID, actions, emptyList<Any>(), UNNotificationCategoryOptionNone
        )
        center.setNotificationCategories(setOf(category))
    }

    private fun add(
        id: String,
        content: UNMutableNotificationContent,
        trigger: UNNotificationTrigger?,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val request = UNNotificationRequest.requestWithIdentifier(id, content, trigger)
        center.addNotificationRequest(request) { error ->
            scope.launch { onComplete(error == null) }
        }
    }

    private fun record(response: UNNotificationResponse, completionHandler: () -> Unit) {
        val notification = response.notification
        val score = response.actionIdentifier.removePrefix(ACTION_ID_PREFIX).toIntOrNull()
        val db = database
        if (score == null || score !in MoodScale.scores || db == null) {
            completionHandler()
            return
        }
        val isTest = notification.request.content.userInfo[KEY_IS_TEST] as? Boolean ?: false
        // A repeating reminder keeps its identifier every day; the delivery time makes each
        // prompt unique, and the unique index makes a repeated answer a no-op.
        val deliveredAt = (notification.date.timeIntervalSince1970 * 1000).toLong()
        scope.launch {
            try {
                db.moodEntryDao().insert(
                    MoodEntry(
                        score = score,
                        source = if (isTest) MoodEntry.SOURCE_TEST_NOTIFICATION else MoodEntry.SOURCE_NOTIFICATION,
                        promptId = "ios-${notification.request.identifier}-$deliveredAt"
                    )
                )
                center.removeDeliveredNotificationsWithIdentifiers(listOf(notification.request.identifier))
            } finally {
                completionHandler()
            }
        }
    }

    private class Delegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
        /** Shows the notification even while Anchor is open, which the test button relies on. */
        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            willPresentNotification: UNNotification,
            withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
        ) {
            withCompletionHandler(
                UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or
                    UNNotificationPresentationOptionSound
            )
        }

        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            didReceiveNotificationResponse: UNNotificationResponse,
            withCompletionHandler: () -> Unit
        ) {
            if (didReceiveNotificationResponse.actionIdentifier == UNNotificationDefaultActionIdentifier) {
                openMoodRequests.intValue++
                withCompletionHandler()
            } else {
                record(didReceiveNotificationResponse, withCompletionHandler)
            }
        }
    }
}

/** The iOS adapter injected into the shared mood tracker; mirrors AndroidMoodReminderController. */
class IosMoodReminderController(private val settings: SettingsManager) : MoodReminderController {
    override val supported: Boolean = true

    override fun configure(
        enabled: Boolean,
        startMinutes: Int,
        endMinutes: Int,
        intervalHours: Int,
        onComplete: (Boolean) -> Unit
    ) {
        if (!enabled) {
            settings.moodRemindersEnabled = false
            IosMoodReminders.cancel()
            onComplete(true)
            return
        }
        if (startMinutes !in 0..1439 || endMinutes !in startMinutes..1439 ||
            intervalHours !in MoodReminderSchedule.intervalsInHours
        ) {
            onComplete(false)
            return
        }
        IosMoodReminders.whenAllowed(onComplete) {
            settings.moodReminderStartMinutes = startMinutes
            settings.moodReminderEndMinutes = endMinutes
            settings.moodReminderIntervalHours = intervalHours
            settings.moodRemindersEnabled = true
            IosMoodReminders.schedule(startMinutes, endMinutes, intervalHours)
            onComplete(true)
        }
    }

    override fun sendTestNotification(onComplete: (Boolean) -> Unit) {
        IosMoodReminders.whenAllowed(onComplete) { IosMoodReminders.sendTest(onComplete) }
    }
}
