package com.nestling.baby.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nestling.baby.R
import com.nestling.baby.domain.BabyEvent
import com.nestling.baby.domain.EventType
import com.nestling.baby.domain.VolumeUnit
import com.nestling.baby.ui.TestTags
import com.nestling.baby.ui.theme.Spacing
import com.nestling.baby.ui.theme.tabular
import com.nestling.baby.util.TimeFormat

/**
 * One event. M3 ListItem: leading icon, overline time-ago, headline "02:14 · Bottle
 * 120 ml", trailing duration chip. No divider — rows are separated by whitespace and a
 * soft container, never by a line.
 */
@Composable
fun EventRow(
    event: BabyEvent,
    nowMillis: Long,
    unit: VolumeUnit,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val amountText = event.amountMl?.takeIf { event.type == EventType.BOTTLE }
        ?.let { TimeFormat.amount(it, unit) }
    val durationText = event.durationMillis
        ?.takeIf { event.type.isTimed && it > 0 }
        ?.let { TimeFormat.duration(it) }
    val detail = when (event.type) {
        EventType.DIAPER -> event.diaper?.let { stringResource(it.labelRes) }
        else -> event.side?.let { stringResource(it.labelRes) }
    }
    val supporting: (@Composable () -> Unit)? = detail?.let { text ->
        {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val trailing: (@Composable () -> Unit)? = durationText?.let { text -> { DurationChip(text) } }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.TIMELINE_ROW + index),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            overlineContent = {
                Text(
                    text = TimeFormat.timeAgo(event.startedAt, nowMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            headlineContent = {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = TimeFormat.clock(event.startedAt),
                        style = MaterialTheme.typography.titleLarge.tabular(),
                    )
                    Text(
                        text = " · " + stringResource(event.type.labelRes),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (amountText != null) {
                        Text(
                            text = amountText,
                            style = MaterialTheme.typography.headlineSmall.tabular(),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            },
            supportingContent = supporting,
            leadingContent = { EventAvatar(event.type) },
            trailingContent = trailing,
        )
    }
}

@Composable
private fun EventAvatar(type: EventType) {
    Surface(
        shape = CircleShape,
        color = type.containerColor(),
        contentColor = type.onContainerColor(),
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(type.iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun DurationChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.tabular(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/** Date divider: a label and air, not a rule. */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    tag: String = text,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(start = 4.dp, top = Spacing.betweenRows, bottom = Spacing.betweenRows)
            .testTag(TestTags.SECTION_HEADER + tag),
    )
}

/**
 * A real empty state. An illustration, a sentence that tells you what to do next, and
 * the promise that matters: it stays on this phone.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen, vertical = Spacing.section)
            .testTag(TestTags.EMPTY_STATE),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.betweenRows),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_empty_night),
            contentDescription = stringResource(R.string.cd_empty_illustration),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            modifier = Modifier.size(132.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
