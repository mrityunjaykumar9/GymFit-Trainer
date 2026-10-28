package com.freegymtrainer.app.domain.nutrition

/** Intermittent-fasting eating windows: [fastHours] without food, [eatHours] to eat. */
enum class FastingProtocol(val label: String, val fastHours: Int, val eatHours: Int) {
    OFF("Off", 0, 24),
    P12_12("12:12", 12, 12),
    P14_10("14:10", 14, 10),
    P16_8("16:8", 16, 8),
    P18_6("18:6", 18, 6);

    companion object {
        fun fromName(name: String?): FastingProtocol = values().firstOrNull { it.name == name } ?: OFF
    }
}

enum class FastingPhase { EATING, FASTING }

data class FastingStatus(
    val phase: FastingPhase,
    /** Minutes until the current phase ends. */
    val minutesLeft: Int,
    /** 0..1 progress through the current phase. */
    val progress: Float,
    val windowStartMinute: Int,
    val windowEndMinute: Int
)

object Fasting {
    private const val DAY = 24 * 60

    /**
     * Where the person is in today's eating window. The window opens at [startMinuteOfDay] (minutes
     * after midnight) and lasts the protocol's eating hours, wrapping past midnight when needed.
     */
    fun status(protocol: FastingProtocol, startMinuteOfDay: Int, nowMinuteOfDay: Int): FastingStatus? {
        if (protocol == FastingProtocol.OFF) return null
        val eat = protocol.eatHours * 60
        val start = startMinuteOfDay.coerceIn(0, DAY - 1)
        val end = (start + eat) % DAY
        // minutes since the window opened, 0..1439
        val sinceOpen = (nowMinuteOfDay - start + DAY) % DAY
        return if (sinceOpen < eat) {
            FastingStatus(FastingPhase.EATING, eat - sinceOpen, sinceOpen.toFloat() / eat, start, end)
        } else {
            val fastLen = DAY - eat
            val intoFast = sinceOpen - eat
            FastingStatus(FastingPhase.FASTING, fastLen - intoFast, intoFast.toFloat() / fastLen, start, end)
        }
    }

    fun formatClock(minuteOfDay: Int): String {
        val m = ((minuteOfDay % DAY) + DAY) % DAY
        val h24 = m / 60
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        return "%d:%02d %s".format(h12, m % 60, if (h24 < 12) "AM" else "PM")
    }

    fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}
