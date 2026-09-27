package com.nestling.baby.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.nestling.baby.R
import com.nestling.baby.domain.DiaperKind
import com.nestling.baby.domain.EventType
import com.nestling.baby.domain.Side

@get:DrawableRes
val EventType.iconRes: Int
    get() = when (this) {
        EventType.BOTTLE -> R.drawable.ic_bottle
        EventType.SLEEP -> R.drawable.ic_sleep
        EventType.DIAPER -> R.drawable.ic_diaper
    }

@get:StringRes
val EventType.labelRes: Int
    get() = when (this) {
        EventType.BOTTLE -> R.string.event_bottle
        EventType.SLEEP -> R.string.event_sleep
        EventType.DIAPER -> R.string.event_diaper
    }

@get:StringRes
val EventType.startDescriptionRes: Int
    get() = when (this) {
        EventType.BOTTLE -> R.string.cd_log_bottle
        EventType.SLEEP -> R.string.cd_log_sleep
        EventType.DIAPER -> R.string.cd_log_diaper
    }

@get:StringRes
val EventType.stopDescriptionRes: Int
    get() = when (this) {
        EventType.SLEEP -> R.string.cd_stop_sleep
        else -> R.string.cd_stop_bottle
    }

@get:StringRes
val DiaperKind.labelRes: Int
    get() = when (this) {
        DiaperKind.WET -> R.string.diaper_wet
        DiaperKind.DIRTY -> R.string.diaper_dirty
        DiaperKind.BOTH -> R.string.diaper_both
    }

@get:StringRes
val Side.labelRes: Int
    get() = when (this) {
        Side.LEFT -> R.string.side_left
        Side.RIGHT -> R.string.side_right
        Side.BOTH -> R.string.side_both
    }

/** One tonal colour per event type so the three buttons are distinguishable half asleep. */
@Composable
@ReadOnlyComposable
fun EventType.containerColor(): Color = when (this) {
    EventType.BOTTLE -> MaterialTheme.colorScheme.primaryContainer
    EventType.SLEEP -> MaterialTheme.colorScheme.tertiaryContainer
    EventType.DIAPER -> MaterialTheme.colorScheme.secondaryContainer
}

@Composable
@ReadOnlyComposable
fun EventType.onContainerColor(): Color = when (this) {
    EventType.BOTTLE -> MaterialTheme.colorScheme.onPrimaryContainer
    EventType.SLEEP -> MaterialTheme.colorScheme.onTertiaryContainer
    EventType.DIAPER -> MaterialTheme.colorScheme.onSecondaryContainer
}
