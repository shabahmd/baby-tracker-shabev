package com.nestling.baby.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nestling.baby.domain.EventType
import com.nestling.baby.ui.TestTags
import com.nestling.baby.ui.theme.Spacing

/**
 * The three buttons the whole product is built around. Large tonal cards, 104dp tall
 * (the 56dp minimum touch target is the floor, not the goal), icon over label, one
 * haptic tick on press.
 */
@Composable
fun EventButtonCard(
    type: EventType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val description = stringResource(type.startDescriptionRes)
    val container = type.containerColor()
    val onContainer = type.onContainerColor()

    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .heightIn(min = Spacing.eventButtonHeight)
            .sizeIn(minHeight = Spacing.minTouchTarget, minWidth = Spacing.minTouchTarget)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .testTag(TestTags.EVENT_BUTTON + type.name),
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = onContainer,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.card),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(type.iconRes),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
            )
            Text(
                text = stringResource(type.labelRes),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.betweenRows),
            )
        }
    }
}

/** Bottle · Sleep · Diaper, always in the same order, always in reach of one thumb. */
@Composable
fun QuickLogRow(
    modifier: Modifier = Modifier,
    buttonHeight: androidx.compose.ui.unit.Dp = Spacing.eventButtonHeight,
    onEvent: (EventType) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.QUICK_LOG_ROW),
        horizontalArrangement = Arrangement.spacedBy(Spacing.betweenRows + 4.dp),
    ) {
        EventType.entries.forEach { type ->
            EventButtonCard(
                type = type,
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight),
                onClick = { onEvent(type) },
            )
        }
    }
}
