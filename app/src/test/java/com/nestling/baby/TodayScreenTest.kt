package com.nestling.baby

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nestling.baby.domain.ActiveTimer
import com.nestling.baby.domain.BabyEvent
import com.nestling.baby.domain.DiaperKind
import com.nestling.baby.domain.EventType
import com.nestling.baby.ui.NestlingActions
import com.nestling.baby.ui.NestlingScaffold
import com.nestling.baby.ui.NestlingUiState
import com.nestling.baby.ui.TestTags
import com.nestling.baby.ui.theme.NestlingTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The one-handed test, as a test: every event type is loggable in two taps or fewer
 * from the Today screen, the empty state says something useful, and nothing in the app
 * asks anybody to sign in.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class TodayScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var capturedNav: NavHostController? = null

    private fun show(
        state: NestlingUiState = NestlingUiState(),
        actions: NestlingActions = NestlingActions(),
    ) {
        compose.setContent {
            val nav = rememberNavController().also { capturedNav = it }
            NestlingTheme(darkTheme = false, nightMode = false, dynamicColor = false) {
                NestlingScaffold(
                    state = state,
                    nowMillis = FIXED_NOW,
                    snackbarHostState = remember { SnackbarHostState() },
                    actions = actions,
                    navController = nav,
                )
            }
        }
    }

    @Test
    fun emptyStateTellsTheParentWhatToDoNext() {
        show()

        compose.onNodeWithTag(TestTags.EMPTY_STATE).assertIsDisplayed()
        compose.onNodeWithText("No events yet").assertIsDisplayed()
        compose.onNodeWithText("Tap + to log the first feeding", substring = true).assertIsDisplayed()
    }

    @Test
    fun theThreeEventButtonsAreOnTheTodayScreenItself() {
        show()

        EventType.entries.forEach { type ->
            compose.onNodeWithTag(TestTags.EVENT_BUTTON + type.name).assertIsDisplayed()
        }
    }

    @Test
    fun bottleIsOneTapToStart() {
        var started: EventType? = null
        show(actions = NestlingActions(onToggleTimer = { started = it }))

        compose.onNodeWithTag(TestTags.EVENT_BUTTON + EventType.BOTTLE.name).performClick()

        assertThat(started).isEqualTo(EventType.BOTTLE)
    }

    @Test
    fun aRunningTimerIsStoppedWithTheSecondTap() {
        var stopped = false
        show(
            state = NestlingUiState(
                activeTimer = ActiveTimer(
                    type = EventType.BOTTLE,
                    startedAt = FIXED_NOW - 8 * 60_000,
                    lastTickAt = FIXED_NOW,
                ),
            ),
            actions = NestlingActions(onStopTimer = { stopped = true }),
        )

        compose.onNodeWithTag(TestTags.LIVE_TIMER).assertIsDisplayed()
        compose.onNodeWithText("08:00").assertIsDisplayed()
        compose.onNodeWithTag(TestTags.STOP_BUTTON).performClick()

        assertThat(stopped).isTrue()
    }

    @Test
    fun diaperIsExactlyTwoTapsFromToday() {
        var logged: DiaperKind? = null
        show(actions = NestlingActions(onLogDiaper = { logged = it }))

        // tap 1: the diaper button on the Today screen
        compose.onNodeWithTag(TestTags.EVENT_BUTTON + EventType.DIAPER.name).performClick()
        compose.onNodeWithTag(TestTags.DIAPER_SHEET).assertIsDisplayed()

        // tap 2: the type
        compose.onNodeWithTag(TestTags.DIAPER_OPTION + DiaperKind.WET.name).performClick()

        assertThat(logged).isEqualTo(DiaperKind.WET)
    }

    @Test
    fun eventButtonsClearTheMinimumTouchTarget() {
        show()

        EventType.entries.forEach { type ->
            compose.onNodeWithTag(TestTags.EVENT_BUTTON + type.name)
                .assertHeightIsAtLeast(56.dp)
        }
    }

    @Test
    fun nothingAnywhereAsksYouToSignIn() {
        show(state = NestlingUiState(todayEvents = listOf(sampleBottle())))

        listOf(
            "sign in", "log in", "login", "sign up", "create account", "password",
            "email", "subscribe", "upgrade", "free trial",
        ).forEach { phrase ->
            val matches = compose
                .onAllNodesWithText(phrase, substring = true, ignoreCase = true)
                .fetchSemanticsNodes()
            assertThat(matches).isEmpty()
        }
    }

    @Test
    fun theNavGraphItselfContainsOnlyTodayAndHistory() {
        show()
        compose.waitForIdle()

        val routes = capturedNav!!.graph.mapNotNull { it.route }
        assertThat(routes).containsExactly("today", "history")
    }

    private fun sampleBottle() = BabyEvent(
        id = 1,
        type = EventType.BOTTLE,
        startedAt = FIXED_NOW - 25 * 60_000,
        endedAt = FIXED_NOW - 12 * 60_000,
        amountMl = 120,
    )
}
