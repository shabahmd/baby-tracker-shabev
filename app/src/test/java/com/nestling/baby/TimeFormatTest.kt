package com.nestling.baby

import com.google.common.truth.Truth.assertThat
import com.nestling.baby.domain.VolumeUnit
import com.nestling.baby.util.TimeFormat
import org.junit.Test

/** The words and numbers a parent reads at 3am. Small, but worth pinning down. */
class TimeFormatTest {

    @Test
    fun elapsedIsAlwaysTheSameWidthUnderAnHour() {
        assertThat(TimeFormat.elapsed(0)).isEqualTo("00:00")
        assertThat(TimeFormat.elapsed(9_000)).isEqualTo("00:09")
        assertThat(TimeFormat.elapsed(65_000)).isEqualTo("01:05")
        assertThat(TimeFormat.elapsed(59 * 60_000 + 59_000)).isEqualTo("59:59")
        assertThat(TimeFormat.elapsed(3_600_000)).isEqualTo("1:00:00")
        assertThat(TimeFormat.elapsed(3_725_000)).isEqualTo("1:02:05")
        assertThat(TimeFormat.elapsed(-5_000)).isEqualTo("00:00")
    }

    @Test
    fun durationReadsLikeAHumanWroteIt() {
        assertThat(TimeFormat.duration(45_000)).isEqualTo("45s")
        assertThat(TimeFormat.duration(12 * 60_000)).isEqualTo("12m")
        assertThat(TimeFormat.duration(65 * 60_000)).isEqualTo("1h 05m")
    }

    @Test
    fun timeAgoMatchesTheTimelineOverline() {
        val now = FIXED_NOW
        assertThat(TimeFormat.timeAgo(now, now)).isEqualTo("just now")
        assertThat(TimeFormat.timeAgo(now - 25 * 60_000, now)).isEqualTo("25 min ago")
        assertThat(TimeFormat.timeAgo(now - 3 * 3_600_000, now)).isEqualTo("3 h ago")
        assertThat(TimeFormat.timeAgo(now - 26 * 3_600_000, now)).isEqualTo("yesterday")
        assertThat(TimeFormat.timeAgo(now - 3 * 86_400_000L, now)).isEqualTo("3 d ago")
    }

    @Test
    fun amountsConvertToOuncesWithoutNoise() {
        assertThat(TimeFormat.amount(120, VolumeUnit.ML)).isEqualTo("120 ml")
        assertThat(TimeFormat.amount(120, VolumeUnit.OZ)).isEqualTo("4.1 oz")
        assertThat(TimeFormat.amount(0, VolumeUnit.OZ)).isEqualTo("0 oz")
    }
}
