package ch.bfh.anchor.model

/** Local clock slots for each day. Both ends of the window are inclusive. */
object MoodReminderSchedule {
    val intervalsInHours: List<Int> = listOf(1, 2, 3, 4, 6, 8, 12)

    fun slots(startMinutes: Int, endMinutes: Int, everyHours: Int): List<Int> {
        require(startMinutes in 0..1439) { "Start must be within the day" }
        require(endMinutes in startMinutes..1439) { "End must be at or after start" }
        require(everyHours in intervalsInHours) { "Unsupported reminder interval" }
        return (startMinutes..endMinutes step (everyHours * 60)).toList()
    }

    fun timeLabel(minutes: Int): String {
        require(minutes in 0..1439)
        val h = (minutes / 60).toString().padStart(2, '0')
        val m = (minutes % 60).toString().padStart(2, '0')
        return "$h:$m"
    }
}
