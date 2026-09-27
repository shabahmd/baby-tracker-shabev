package com.nestling.baby.util

import com.nestling.baby.domain.VolumeUnit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Formatting helpers. Pure functions, so the "does it read right at 3am?" rules are
 * unit testable instead of a matter of opinion.
 */
object TimeFormat {

    private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    private val DAY_WITH_YEAR: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault())

    /** Big live timer: 04:21 under an hour, 1:04:21 over it. Always tabular width. */
    fun elapsed(millis: Long): String {
        val total = (millis / 1000).coerceAtLeast(0)
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /** Timeline trailing chip: 45s / 12m / 1h 05m. */
    fun duration(millis: Long): String {
        val total = (millis / 1000).coerceAtLeast(0)
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        return when {
            hours > 0 -> String.format(Locale.US, "%dh %02dm", hours, minutes)
            minutes > 0 -> "${minutes}m"
            else -> "${total}s"
        }
    }

    /** Timeline overline: "just now", "25 min ago", "3 h ago", "2 d ago". */
    fun timeAgo(eventMillis: Long, nowMillis: Long): String {
        val delta = (nowMillis - eventMillis).coerceAtLeast(0)
        val minutes = delta / 60_000
        val hours = delta / 3_600_000
        val days = delta / 86_400_000
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours h ago"
            days == 1L -> "yesterday"
            else -> "$days d ago"
        }
    }

    fun clock(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(CLOCK)

    fun day(date: LocalDate, today: LocalDate = LocalDate.now()): String =
        if (date.year == today.year) date.format(DAY) else date.format(DAY_WITH_YEAR)

    fun localDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** 120 ml, or 4.1 oz for parents who think in ounces. */
    fun amount(ml: Int, unit: VolumeUnit): String = when (unit) {
        VolumeUnit.ML -> "$ml ml"
        VolumeUnit.OZ -> {
            val oz = (ml / 29.5735).let { (it * 10).roundToInt() / 10.0 }
            val text = if (oz % 1.0 == 0.0) oz.toInt().toString() else String.format(Locale.US, "%.1f", oz)
            "$text oz"
        }
    }
}
