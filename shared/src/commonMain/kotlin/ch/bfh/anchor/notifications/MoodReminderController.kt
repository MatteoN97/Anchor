package ch.bfh.anchor.notifications

/** Scheduling and permission handling stay native; shared Compose uses this contract. */
interface MoodReminderController {
    val supported: Boolean

    /** onComplete(true) means the schedule is enabled/disabled successfully. */
    fun configure(
        enabled: Boolean,
        startMinutes: Int,
        endMinutes: Int,
        intervalHours: Int,
        onComplete: (Boolean) -> Unit
    )

    /** Sends a real notification immediately; does not require scheduled reminders to be enabled. */
    fun sendTestNotification(onComplete: (Boolean) -> Unit)
}

/** In-app tracking works on iOS; notifications still require native iOS implementation. */
object NoMoodReminderController : MoodReminderController {
    override val supported = false
    override fun configure(
        enabled: Boolean,
        startMinutes: Int,
        endMinutes: Int,
        intervalHours: Int,
        onComplete: (Boolean) -> Unit
    ) { onComplete(!enabled) }

    override fun sendTestNotification(onComplete: (Boolean) -> Unit) { onComplete(false) }
}
