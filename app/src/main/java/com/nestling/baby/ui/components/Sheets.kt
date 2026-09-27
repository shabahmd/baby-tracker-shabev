package com.nestling.baby.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nestling.baby.R
import com.nestling.baby.domain.DiaperKind
import com.nestling.baby.domain.EventType
import com.nestling.baby.domain.Side
import com.nestling.baby.domain.VolumeUnit
import com.nestling.baby.ui.TestTags
import com.nestling.baby.ui.theme.Spacing
import com.nestling.baby.ui.theme.tabular
import com.nestling.baby.util.TimeFormat

/** The FAB sheet: the same three buttons, as large tonal cards. Never a list of chips. */
@Composable
fun QuickLogSheet(
    onDismiss: () -> Unit,
    onEvent: (EventType) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag(TestTags.QUICK_LOG_SHEET),
    ) {
        SheetBody {
            SheetTitle(
                title = stringResource(R.string.quick_log_title),
                subtitle = stringResource(R.string.quick_log_subtitle),
            )
            QuickLogRow(
                modifier = Modifier.padding(top = Spacing.card),
                buttonHeight = 124.dp,
                onEvent = onEvent,
            )
        }
    }
}

/** Diaper: one more tap and it is saved. Wet / Dirty / Both, nothing else to decide. */
@Composable
fun DiaperSheet(
    onDismiss: () -> Unit,
    onPick: (DiaperKind) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag(TestTags.DIAPER_SHEET),
    ) {
        SheetBody {
            SheetTitle(title = stringResource(R.string.diaper_title))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.card),
                horizontalArrangement = Arrangement.spacedBy(Spacing.betweenRows + 4.dp),
            ) {
                DiaperKind.entries.forEach { kind ->
                    val label = stringResource(kind.labelRes)
                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPick(kind)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(104.dp)
                            .semantics { contentDescription = label }
                            .testTag(TestTags.DIAPER_OPTION + kind.name),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.card),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                painter = painterResource(EventType.DIAPER.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = Spacing.betweenRows),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Amount + side for a bottle. One segmented row for the side, one stepper for the
 * amount. The event is already saved before this sheet opens; this only refines it.
 */
@Composable
fun AmountSheet(
    initialAmountMl: Int,
    initialSide: Side?,
    unit: VolumeUnit,
    onDismiss: () -> Unit,
    onSave: (Int, Side?) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var amount by remember { mutableIntStateOf(initialAmountMl.coerceIn(MIN_ML, MAX_ML)) }
    var side by remember { mutableStateOf(initialSide) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag(TestTags.AMOUNT_SHEET),
    ) {
        SheetBody {
            SheetTitle(title = stringResource(R.string.amount_sheet_title))

            Text(
                text = stringResource(R.string.side_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.card, bottom = Spacing.betweenRows),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Side.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = side == option,
                        onClick = { side = if (side == option) null else option },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Side.entries.size),
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text(stringResource(option.labelRes)) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.amount_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.section, bottom = Spacing.betweenRows),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.card),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val decrease = stringResource(R.string.cd_amount_decrease)
                val increase = stringResource(R.string.cd_amount_increase)
                FilledTonalIconButton(
                    onClick = { amount = (amount - STEP_ML).coerceAtLeast(MIN_ML) },
                    modifier = Modifier
                        .size(Spacing.minTouchTarget)
                        .semantics { contentDescription = decrease },
                ) {
                    Text(text = "−", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = TimeFormat.amount(amount, unit),
                    style = MaterialTheme.typography.headlineSmall.tabular(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                FilledTonalIconButton(
                    onClick = { amount = (amount + STEP_ML).coerceAtMost(MAX_ML) },
                    modifier = Modifier
                        .size(Spacing.minTouchTarget)
                        .semantics { contentDescription = increase },
                ) {
                    Text(text = "+", style = MaterialTheme.typography.titleLarge)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.section),
                horizontalArrangement = Arrangement.spacedBy(Spacing.betweenRows),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.heightIn(min = Spacing.minTouchTarget),
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
                Button(
                    onClick = { onSave(amount, side) },
                    modifier = Modifier
                        .weight(1f)
                        .height(Spacing.minTouchTarget),
                ) {
                    Text(
                        text = stringResource(R.string.action_done),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetBody(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen)
            .padding(bottom = Spacing.section)
            .navigationBarsPadding(),
    ) {
        content()
    }
}

@Composable
private fun SheetTitle(title: String, subtitle: String? = null) {
    Text(text = title, style = MaterialTheme.typography.titleLarge)
    if (subtitle != null) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private const val MIN_ML = 10
private const val MAX_ML = 400
private const val STEP_ML = 10
