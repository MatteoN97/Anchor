package ch.bfh.anchor.notifications

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import ch.bfh.anchor.MainActivity
import ch.bfh.anchor.android.R
import ch.bfh.anchor.data.createDatabase
import ch.bfh.anchor.model.MoodEntry
import ch.bfh.anchor.model.MoodReminderSchedule
import ch.bfh.anchor.settings.SettingsManager
import ch.bfh.anchor.settings.createSettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

private const val CHANNEL_ID = "mood_check_ins"
private const val NOTIFICATION_ID = 8921
private const val ACTION_REMIND = "ch.bfh.anchor.MOOD_REMINDER"
private const val ACTION_RECORD = "ch.bfh.anchor.MOOD_RECORD"
private const val EXTRA_SCORE = "score"
private const val EXTRA_PROMPT = "prompt"
private const val EXTRA_IS_TEST = "is_test"
const val EXTRA_OPEN_MOOD = "open_mood_tracker"

/** Inexact, one-shot alarms: after each delivery, schedule the next daily window slot. */
internal object MoodNotifications {
    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 501,
        Intent(context, MoodNotificationReceiver::class.java).setAction(ACTION_REMIND),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun schedule(context: Context, settings: SettingsManager) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = alarmIntent(context)
        alarms.cancel(pending)
        if (!settings.moodRemindersEnabled) return

        // Recompute from the local clock each time, so tomorrow's window begins at the
        // configured time even over DST and timezone changes. Both endpoints are inclusive.
        val now = System.currentTimeMillis()
        val slots = MoodReminderSchedule.slots(
            settings.moodReminderStartMinutes,
            settings.moodReminderEndMinutes,
            settings.moodReminderIntervalHours
        )
        var nextTime: Long? = null
        for (daysAhead in 0..2) {
            for (minuteOfDay in slots) {
                val candidate = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.DAY_OF_YEAR, daysAhead)
                    set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
                    set(Calendar.MINUTE, minuteOfDay % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                if (candidate > now) {
                    nextTime = candidate
                    break
                }
            }
            if (nextTime != null) break
        }
        // Inexact alarms deliberately avoid special exact-alarm permissions.
        nextTime?.let { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it, pending) }
    }

    /** Avoid quiet-hour alerts; allow 30 minutes of leeway for an inexact final slot. */
    fun withinReminderWindow(settings: SettingsManager): Boolean {
        val now = Calendar.getInstance()
        val minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val latest = (settings.moodReminderEndMinutes + 30).coerceAtMost(1439)
        return minuteOfDay in settings.moodReminderStartMinutes..latest
    }

    fun cancel(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.cancel(alarmIntent(context))
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(context: Context): NotificationManager {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, "Mood check-in reminders", NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Optional mood check-in reminders"
                setShowBadge(false)
            })
        }
        return manager
    }

    fun isAllowed(context: Context): Boolean {
        val manager = ensureChannel(context)
        return manager.areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 26 ||
                manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE) &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED)
    }

    /** Returns false if notifications are blocked. A test creates the same real one-tap prompt. */
    fun show(context: Context, isTest: Boolean = false): Boolean {
        if (!isAllowed(context)) return false
        val manager = ensureChannel(context)

        val promptId = "android-${UUID.randomUUID()}"
        val choiceIds = intArrayOf(R.id.mood_1, R.id.mood_2, R.id.mood_3, R.id.mood_4, R.id.mood_5)
        val layout = RemoteViews(context.packageName, R.layout.mood_notification_expanded)
        val choiceActions = mutableMapOf<Int, PendingIntent>()
        choiceIds.forEachIndexed { index, id ->
            val score = index + 1
            val intent = Intent(context, MoodNotificationReceiver::class.java).apply {
                action = ACTION_RECORD
                // Distinct identity per button and notification, even after process restarts.
                data = Uri.parse("anchor://mood/$promptId/$score")
                putExtra(EXTRA_SCORE, score)
                putExtra(EXTRA_PROMPT, promptId)
                putExtra(EXTRA_IS_TEST, isTest)
            }
            val click = PendingIntent.getBroadcast(
                context, score, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            layout.setOnClickPendingIntent(id, click)
            choiceActions[score] = click
        }

        val openApp = PendingIntent.getActivity(
            context, 502,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_MOOD, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }
        val publicVersion = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }.setSmallIcon(R.drawable.ic_mood_notification)
            .setContentTitle("Anchor reminder")
            .build()

        val notification = builder
            .setSmallIcon(R.drawable.ic_mood_notification)
            .setContentTitle("A quick check-in")
            .setContentText("How are you feeling? Expand to choose a mood.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setStyle(Notification.DecoratedCustomViewStyle())
            .setCustomBigContentView(layout)
            .setCategory(Notification.CATEGORY_REMINDER)
            // Standard actions remain usable if an OEM limits custom expanded layouts.
            .addAction(Notification.Action.Builder(R.drawable.ic_mood_notification, "5 · Great", choiceActions.getValue(5)).build())
            .addAction(Notification.Action.Builder(R.drawable.ic_mood_notification, "3 · Okay", choiceActions.getValue(3)).build())
            .addAction(Notification.Action.Builder(R.drawable.ic_mood_notification, "1 · Not great", choiceActions.getValue(1)).build())
            .build()

        return try {
            // Re-post a fresh notification for each test as well as each scheduled reminder.
            manager.cancel(NOTIFICATION_ID)
            manager.notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            // Permission may be revoked between permission check and notification delivery.
            false
        }
    }

    fun dismiss(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIFICATION_ID)
    }
}

/** Android-facing adapter injected into the shared Compose tracker. */
class AndroidMoodReminderController(
    private val activity: Activity,
    private val settings: SettingsManager,
    private val askPermission: ((Boolean) -> Unit) -> Unit
) : MoodReminderController {
    override val supported: Boolean = true

    private fun whenNotificationAllowed(onComplete: (Boolean) -> Unit, onAllowed: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            askPermission { granted ->
                if (granted && MoodNotifications.isAllowed(activity)) onAllowed()
                else onComplete(false)
            }
        } else if (MoodNotifications.isAllowed(activity)) {
            onAllowed()
        } else {
            onComplete(false)
        }
    }

    override fun configure(
        enabled: Boolean,
        startMinutes: Int,
        endMinutes: Int,
        intervalHours: Int,
        onComplete: (Boolean) -> Unit
    ) {
        if (!enabled) {
            settings.moodRemindersEnabled = false
            MoodNotifications.cancel(activity)
            onComplete(true)
            return
        }
        if (startMinutes !in 0..1439 || endMinutes !in startMinutes..1439 ||
            intervalHours !in MoodReminderSchedule.intervalsInHours) {
            onComplete(false)
            return
        }
        whenNotificationAllowed(onComplete) {
            settings.moodReminderStartMinutes = startMinutes
            settings.moodReminderEndMinutes = endMinutes
            settings.moodReminderIntervalHours = intervalHours
            settings.moodRemindersEnabled = true
            MoodNotifications.schedule(activity, settings)
            onComplete(true)
        }
    }

    override fun sendTestNotification(onComplete: (Boolean) -> Unit) {
        whenNotificationAllowed(onComplete) {
            onComplete(MoodNotifications.show(activity, isTest = true))
        }
    }
}

/** PendingIntent targets this receiver: the UI is not launched for an actual score selection. */
class MoodNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REMIND -> {
                val settings = createSettingsManager(context)
                if (settings.moodRemindersEnabled) {
                    if (MoodNotifications.withinReminderWindow(settings)) {
                        MoodNotifications.show(context)
                    }
                    MoodNotifications.schedule(context, settings)
                }
            }
            ACTION_RECORD -> {
                val score = intent.getIntExtra(EXTRA_SCORE, -1)
                val promptId = intent.getStringExtra(EXTRA_PROMPT)
                if (score !in 1..5 || promptId.isNullOrBlank()) return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = createDatabase(context.applicationContext)
                        // Unique index on promptId + INSERT OR IGNORE guarantees one entry.
                        db.moodEntryDao().insert(MoodEntry(
                            score = score,
                            source = if (intent.getBooleanExtra(EXTRA_IS_TEST, false))
                                MoodEntry.SOURCE_TEST_NOTIFICATION else MoodEntry.SOURCE_NOTIFICATION,
                            promptId = promptId
                        ))
                        MoodNotifications.dismiss(context)
                    } catch (error: Exception) {
                        Log.e("AnchorMood", "Could not save notification check-in", error)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}

/** Device reboot, timezone change, and manual clock change can invalidate scheduled alarms. */
class MoodRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            MoodNotifications.schedule(context, createSettingsManager(context))
        }
    }
}
