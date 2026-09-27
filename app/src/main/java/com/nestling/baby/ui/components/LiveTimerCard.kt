package com.nestling.baby.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nestling.baby.R
import com.nestling.baby.domain.ActiveTimer
import com.nestling.baby.domain.EventType
import com.nestling.baby.ui.TestTags
import com.nestling.baby.ui.theme.Spacing
import com.nestling.baby.ui.theme.tabular
import com.nestling.baby.util.TimeFormat

/**
 * The running timer. Big tabular-numeral clock so it reads from across a dark room, one
 * unmissable STOP button, and a quiet Discard for the times you tapped the wrong thing.
 *
 * The elapsed time is derived from the start time stored in Room — rotation, app switch
 * and process death all land on the same number.
 */
@Composable
fun LiveTimerCard(
    timer: ActiveTimer,
    nowMillis: Long,
    modifier: Modifier = Modifier,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val title = when (timer.type) {
        EventType.SLEEP -> stringResource(R.string.timer_running_sleep)
        else -> stringResource(R.string.timer_running_bottle)
    }
    val stopDescription = stringResource(timer.type.stopDescriptionRes)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.LIVE_TIMER),
        shape = MaterialTheme.shapes.large,
        color = timer.type.containerColor(),
        contentColor = timer.type.onContainerColor(),
    ) {
        Column(modifier = Modifier.padding(Spacing.card)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(timer.type.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = Spacing.betweenRows),
                )
            }
            Text(
                text = TimeFormat.elapsed(timer.elapsedMillis(nowMillis)),
                style = MaterialTheme.typography.displayMedium.tabular(),
                modifier = Modifier.padding(top = Spacing.betweenRows),
            )
            Text(
                text = stringResource(R.string.timer_started_at, TimeFormat.clock(timer.startedAt)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.card),
                horizontalArrangement = Arrangement.spacedBy(Spacing.betweenRows),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStop()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(Spacing.minTouchTarget)
                        .semantics { contentDescription = stopDescription }
                        .testTag(TestTags.STOP_BUTTON),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.timer_stop),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                val discardDescription = stringResource(R.string.cd_discard_timer)
                TextButton(
                    onClick = onDiscard,
                    modifier = Modifier
                        .heightIn(min = Spacing.minTouchTarget)
                        .semantics { contentDescription = discardDescription },
                ) {
                    Text(stringResource(R.string.timer_discard))
                }
            }
        }
    }
}
