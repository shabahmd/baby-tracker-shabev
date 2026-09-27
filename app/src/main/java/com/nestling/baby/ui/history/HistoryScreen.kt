package com.nestling.baby.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nestling.baby.R
import com.nestling.baby.domain.BabyEvent
import com.nestling.baby.ui.NestlingUiState
import com.nestling.baby.ui.TestTags
import com.nestling.baby.ui.components.EmptyState
import com.nestling.baby.ui.components.EventRow
import com.nestling.baby.ui.components.SectionHeader
import com.nestling.baby.ui.theme.Spacing
import com.nestling.baby.util.TimeFormat
import java.time.LocalDate

/**
 * History. A day pager and a search box. Browsing the past is reading, not syncing —
 * there is no refresh gesture because the data is already local and already current.
 */
@Composable
fun HistoryScreen(
    state: NestlingUiState,
    nowMillis: Long,
    contentPadding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onShiftDay: (Long) -> Unit,
    onJumpToToday: () -> Unit,
    onEventClick: (BabyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searching = state.searching
    val events = if (searching) state.searchResults else state.historyEvents

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
        item(key = "search") {
            SearchField(query = state.query, onQueryChange = onQueryChange)
        }

        if (!searching) {
            item(key = "pager") {
                DayPager(
                    date = state.historyDate,
                    today = state.today,
                    onShiftDay = onShiftDay,
                    onJumpToToday = onJumpToToday,
                )
            }
        }

        if (events.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(
                        if (searching) R.string.empty_search_title else R.string.empty_history_title,
                    ),
                    body = stringResource(
                        if (searching) R.string.empty_search_body else R.string.empty_history_body,
                    ),
                )
            }
        } else if (searching) {
            // Search crosses days, so each day gets its own header.
            val grouped = events.groupBy { TimeFormat.localDate(it.startedAt) }
            var index = 0
            grouped.forEach { (date, dayEvents) ->
                item(key = "header-$date") {
                    SectionHeader(text = dayLabel(date, state.today), tag = date.toString())
                }
                dayEvents.forEach { event ->
                    val rowIndex = index++
                    item(key = event.id) {
                        EventRow(
                            event = event,
                            nowMillis = nowMillis,
                            unit = state.unit,
                            index = rowIndex,
                            onClick = { onEventClick(event) },
                        )
                    }
                }
            }
        } else {
            itemsIndexed(items = events, key = { _, event -> event.id }) { index, event ->
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

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.SEARCH_FIELD),
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        placeholder = { Text(stringResource(R.string.history_search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_clear_search),
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun DayPager(
    date: LocalDate,
    today: LocalDate,
    onShiftDay: (Long) -> Unit,
    onJumpToToday: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.DAY_PAGER),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onShiftDay(-1) },
            modifier = Modifier.size(Spacing.minTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_previous_day),
            )
        }
        TextButton(
            onClick = onJumpToToday,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = dayLabel(date, today),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        IconButton(
            onClick = { onShiftDay(1) },
            enabled = date.isBefore(today),
            modifier = Modifier.size(Spacing.minTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_next_day),
            )
        }
    }
}

@Composable
private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.timeline_today)
    today.minusDays(1) -> stringResource(R.string.timeline_yesterday)
    else -> TimeFormat.day(date, today)
}

private val FAB_CLEARANCE = 96.dp
