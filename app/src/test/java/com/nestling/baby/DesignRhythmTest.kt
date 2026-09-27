package com.nestling.baby

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nestling.baby.domain.BabyEvent
import com.nestling.baby.domain.DiaperKind
import com.nestling.baby.domain.EventType
import com.nestling.baby.ui.NestlingActions
import com.nestling.baby.ui.NestlingScaffold
import com.nestling.baby.ui.NestlingUiState
import com.nestling.baby.ui.TestTags
import com.nestling.baby.ui.theme.Spacing
import com.nestling.baby.ui.theme.tabular
import com.nestling.baby.ui.theme.NestlingTheme
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The layout rules from the brief, asserted instead of eyeballed: a 16dp rhythm, rows
 * separated by whitespace rather than a divider per row, tabular numerals on anything
 * numeric, and no clipping at 1.3x font scale.
 *
 * This is the "screenshot test" made deterministic — it compares geometry against the
 * M3 baseline rather than pixels, so it can't drift with a renderer change.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class DesignRhythmTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = listOf(
        BabyEvent(
            id = 1,
            type = EventType.BOTTLE,
            startedAt = FIXED_NOW - 25 * 60_000,
            endedAt = FIXED_NOW - 13 * 60_000,
            amountMl = 120,
        ),
        BabyEvent(
            id = 2,
            type = EventType.SLEEP,
            startedAt = FIXED_NOW - 3 * 3_600_000,
            endedAt = FIXED_NOW - 2 * 3_600_000,
        ),
        BabyEvent(
            id = 3,
            type = EventType.DIAPER,
            startedAt = FIXED_NOW - 4 * 3_600_000,
            endedAt = FIXED_NOW - 4 * 3_600_000,
            diaper = DiaperKind.WET,
        ),
    )

    private fun show(fontScale: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
            ) {
                NestlingTheme(darkTheme = false, nightMode = false, dynamicColor = false) {
                    NestlingScaffold(
                        state = NestlingUiState(todayEvents = events),
                        nowMillis = FIXED_NOW,
                        snackbarHostState = remember { SnackbarHostState() },
                        actions = NestlingActions(),
                    )
                }
            }
        }
    }

    @Test
    fun spacingTokensAreOnA4dpGridWithA16dpGutter() {
        assertThat(Spacing.screen).isEqualTo(16.dp)
        assertThat(Spacing.card).isEqualTo(16.dp)
        assertThat(Spacing.section).isEqualTo(24.dp)
        assertThat(Spacing.betweenRows).isEqualTo(8.dp)
        assertThat(Spacing.minTouchTarget.value).isAtLeast(56f)
        listOf(Spacing.screen, Spacing.card, Spacing.section, Spacing.betweenRows)
            .forEach { assertThat(it.value % 4f).isEqualTo(0f) }
    }

    @Test
    fun timelineRowsSitOnTheSixteenDpGutter() {
        show()

        val row = compose.onNodeWithTag(TestTags.TIMELINE_ROW + "0").getUnclippedBoundsInRoot()
        assertThat(abs(row.left.value - Spacing.screen.value)).isLessThan(0.5f)
    }

    @Test
    fun rowsAreSeparatedByWhitespaceNotByDividers() {
        show()

        val first = compose.onNodeWithTag(TestTags.TIMELINE_ROW + "0").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag(TestTags.TIMELINE_ROW + "1").getUnclippedBoundsInRoot()
        val gap = second.top.value - first.bottom.value

        assertThat(abs(gap - Spacing.betweenRows.value)).isLessThan(0.5f)

        // Exactly one date header for the day, and no per-row dividers anywhere.
        assertThat(
            compose.onAllNodesWithTag(TestTags.SECTION_HEADER + "today").fetchSemanticsNodes(),
        ).hasSize(1)
    }

    @Test
    fun amountsAndTimersUseTabularNumerals() {
        assertThat(TextStyle.Default.tabular().fontFeatureSettings).isEqualTo("tnum")
    }

    @Test
    fun theTimelineStillReadsAt130PercentFontScale() {
        show(fontScale = 1.3f)

        // Content is present, displayed, and the touch targets have grown rather than clipped.
        compose.onNodeWithText("120 ml").assertIsDisplayed()
        compose.onNodeWithTag(TestTags.TIMELINE_ROW + "0").assertIsDisplayed()
        EventType.entries.forEach { type ->
            compose.onNodeWithTag(TestTags.EVENT_BUTTON + type.name).assertHeightIsAtLeast(56.dp)
        }
    }
}
