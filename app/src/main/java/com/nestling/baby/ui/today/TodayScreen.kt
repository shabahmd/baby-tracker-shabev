package com.nestling.baby.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nestling.baby.R
import com.nestling.baby.domain.BabyEvent
import com.nestling.baby.domain.EventType
import com.nestling.baby.ui.NestlingUiState
import com.nestling.baby.ui.TestTags
import com.nestling.baby.ui.components.EmptyState
import com.nestling.baby.ui.components.EventRow
import com.nestling.baby.ui.components.LiveTimerCard
import com.nestling.baby.ui.components.QuickLogRow
import com.nestling.baby.ui.components.SectionHeader
import com.nestling.baby.ui.theme.Spacing

/**
 * Today. The three buttons (or the running timer) sit at the top, everything logged
 * today sits under them, newest first. Nothing else competes for attention.
 */
@Composable
fun TodayScreen(
    state: NestlingUiState,
    nowMillis: Long,
    contentPadding: PaddingValues,
    onEvent: (EventType) -> Unit,
    onStopTimer: () -> Unit,
    onDiscardTimer: () -> Unit,
    onEventClick: (BabyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(TestTags.TIMELINE),
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = contentPadding.calculateTopPadding() + Spacing.betweenRows,
            bottom = contentPadding.calculateBottomPadding() + FAB_CLEARANCE,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.betweenRows),
    ) {
        item(key = "quick-log") {
            val timer = state.activeTimer
            if (timer != null) {
                LiveTimerCard(
                    timer = timer,
                    nowMillis = nowMillis,
                    onStop = onStopTimer,
                    onDiscard = onDiscardTimer,
                )
            } else {
                QuickLogRow(onEvent = onEvent)
            }
        }

        if (state.todayEvents.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.empty_today_title),
                    body = stringResource(R.string.empty_today_body),
                )
            }
        } else {
            item(key = "header-today") {
                SectionHeader(text = stringResource(R.string.timeline_today), tag = "today")
            }
            itemsIndexed(
                items = state.todayEvents,
                key = { _, event -> event.id },
            ) { index, event ->
                EventRow(
                    event = event,
                    nowMillis = nowMillis,
                    unit = state.unit,
                    index = index,
                    onClick = { onEventClick(event) },
                )
            }
        }
    }
}

private val FAB_CLEARANCE = 96.dp
