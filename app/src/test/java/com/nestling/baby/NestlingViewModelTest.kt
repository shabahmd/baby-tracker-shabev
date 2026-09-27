package com.nestling.baby

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nestling.baby.data.NestlingRepository
import com.nestling.baby.data.local.NestlingDatabase
import com.nestling.baby.domain.AppSettings
import com.nestling.baby.domain.DiaperKind
import com.nestling.baby.domain.EventType
import com.nestling.baby.domain.NightSuggestion
import com.nestling.baby.domain.Side
import com.nestling.baby.domain.VolumeUnit
import com.nestling.baby.platform.PlatformEffects
import com.nestling.baby.ui.NestlingViewModel
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NestlingViewModelTest {

    private var now = FIXED_NOW
    private lateinit var database: NestlingDatabase
    private lateinit var settings: FakeSettingsStore
    private lateinit var repository: NestlingRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = inMemoryDatabase()
        settings = FakeSettingsStore()
        repository = repositoryFor(database) { now }
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(initialSettings: AppSettings = AppSettings()): NestlingViewModel {
        settings = FakeSettingsStore(initialSettings)
        return NestlingViewModel(
            repository = repositoryFor(database) { now },
            settingsStore = settings,
            effects = PlatformEffects.None,
            clock = { now },
            zone = { ZoneId.of("UTC") },
        )
    }

    /** Room writes finish on their own executor; wait for the outcome, not for a tick. */
    private suspend fun awaitUntil(timeoutMillis: Long = 5_000, condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default) {
            val deadline = System.currentTimeMillis() + timeoutMillis
            while (System.currentTimeMillis() < deadline) {
                if (condition()) return@withContext
                delay(10)
            }
            throw AssertionError("Condition was still false after ${timeoutMillis}ms")
        }
    }

    @Test
    fun tappingBottleStartsATimerAndTappingItAgainSavesTheFeed() = runTest {
        val vm = viewModel()

        vm.toggleTimer(EventType.BOTTLE)
        awaitUntil { repository.currentTimer()?.type == EventType.BOTTLE }

        now += 9 * 60_000
        vm.toggleTimer(EventType.BOTTLE)
        awaitUntil { repository.currentTimer() == null && repository.eventCount() == 1 }

        val events = repository.allEventsAscending()
        assertThat(events.first().type).isEqualTo(EventType.BOTTLE)
        assertThat(events.first().durationMillis).isEqualTo(9 * 60_000L)
        // Stop reuses the last amount, so logging a bottle really is two taps.
        assertThat(events.first().amountMl).isEqualTo(AppSettings().lastAmountMl)
    }

    @Test
    fun switchingTypeMidTimerSavesTheFirstOneRatherThanLosingIt() = runTest {
        val vm = viewModel()

        vm.toggleTimer(EventType.SLEEP)
        awaitUntil { repository.currentTimer()?.type == EventType.SLEEP }

        now += 20 * 60_000
        vm.toggleTimer(EventType.BOTTLE)
        awaitUntil { repository.currentTimer()?.type == EventType.BOTTLE }

        val events = repository.allEventsAscending()
        assertThat(events).hasSize(1)
        assertThat(events.first().type).isEqualTo(EventType.SLEEP)
        assertThat(events.first().durationMillis).isEqualTo(20 * 60_000L)
    }

    @Test
    fun diaperIsSavedInstantly() = runTest {
        val vm = viewModel()

        vm.logDiaper(DiaperKind.BOTH)
        awaitUntil { repository.eventCount() == 1 }

        val event = repository.allEventsAscending().first()
        assertThat(event.type).isEqualTo(EventType.DIAPER)
        assertThat(event.diaper).isEqualTo(DiaperKind.BOTH)
        assertThat(event.durationMillis).isEqualTo(0L)
    }

    @Test
    fun editingTheAmountRemembersItForNextTime() = runTest {
        val vm = viewModel()

        vm.toggleTimer(EventType.BOTTLE)
        awaitUntil { repository.currentTimer() != null }
        now += 5 * 60_000
        vm.stopTimer()
        awaitUntil { repository.eventCount() == 1 }

        val id = repository.allEventsAscending().first().id
        vm.saveAmount(id, 165, Side.RIGHT)
        awaitUntil { repository.eventById(id)?.amountMl == 165 }

        assertThat(repository.eventById(id)?.side).isEqualTo(Side.RIGHT)
        assertThat(settings.current.lastAmountMl).isEqualTo(165)
        assertThat(settings.current.lastSide).isEqualTo(Side.RIGHT)
    }

    @Test
    fun deletingAnEventRemovesItCompletely() = runTest {
        val vm = viewModel()

        vm.logDiaper(DiaperKind.WET)
        awaitUntil { repository.eventCount() == 1 }
        val id = repository.allEventsAscending().first().id

        vm.deleteEvent(id)
        awaitUntil { repository.eventCount() == 0 }
    }

    @Test
    fun historyNeverPagesIntoTheFuture() = runTest {
        val vm = viewModel()
        val today = LocalDate.ofEpochDay(FIXED_NOW / 86_400_000L)

        vm.shiftDay(1)
        assertThat(vm.state.value.historyDate).isEqualTo(today)

        vm.shiftDay(-1)
        assertThat(vm.state.value.historyDate).isEqualTo(today.minusDays(1))
    }

    @Test
    fun settingsRoundTrip() = runTest {
        val vm = viewModel()

        vm.setNightMode(true)
        vm.setUnit(VolumeUnit.OZ)
        vm.setBabyName("  Ada  ")

        assertThat(settings.current.nightMode).isTrue()
        assertThat(settings.current.unit).isEqualTo(VolumeUnit.OZ)
        assertThat(settings.current.babyName).isEqualTo("Ada")
    }

    @Test
    fun searchFindsEventsByTypeAmountSideAndDiaperKind() = runTest {
        repository.startTimer(EventType.BOTTLE)
        now += 10 * 60_000
        repository.stopTimer(amountMl = 140, side = Side.LEFT)
        repository.logDiaper(DiaperKind.DIRTY)

        assertThat(repository.search("bottle").first()).hasSize(1)
        assertThat(repository.search("140").first()).hasSize(1)
        assertThat(repository.search("dirty").first()).hasSize(1)
        assertThat(repository.search("left").first()).hasSize(1)
        assertThat(repository.search("nothing-matches-this").first()).isEmpty()
    }

    @Test
    fun nightModeIsOfferedLateAtNightAndOnlyOnce() {
        assertThat(
            NightSuggestion.shouldSuggest(
                hourOfDay = 23,
                nightModeEnabled = false,
                suppressedOn = "",
                today = "2026-03-14",
            ),
        ).isTrue()
        assertThat(
            NightSuggestion.shouldSuggest(
                hourOfDay = 3,
                nightModeEnabled = false,
                suppressedOn = "",
                today = "2026-03-14",
            ),
        ).isTrue()
        assertThat(
            NightSuggestion.shouldSuggest(
                hourOfDay = 14,
                nightModeEnabled = false,
                suppressedOn = "",
                today = "2026-03-14",
            ),
        ).isFalse()
        assertThat(
            NightSuggestion.shouldSuggest(
                hourOfDay = 23,
                nightModeEnabled = true,
                suppressedOn = "",
                today = "2026-03-14",
            ),
        ).isFalse()
        assertThat(
            NightSuggestion.shouldSuggest(
                hourOfDay = 23,
                nightModeEnabled = false,
                suppressedOn = "2026-03-14",
                today = "2026-03-14",
            ),
        ).isFalse()
    }
}
