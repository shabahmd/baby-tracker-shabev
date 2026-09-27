package com.nestling.baby

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nestling.baby.data.local.NestlingDatabase
import com.nestling.baby.domain.EventType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Acceptance criterion: kill the process mid-timer, relaunch, and the running timer
 * comes back with the correct start time.
 *
 * "Process death" here is the honest version of it: the database is closed without any
 * shutdown hook running, every in-memory object is thrown away, and a brand new
 * repository is built from a brand new database handle — exactly what happens when
 * Android kills a backgrounded app at 3am.
 */
@RunWith(AndroidJUnit4::class)
class TimerRestoreTest {

    private val dbName = "nestling-process-death-test.db"
    private var now = FIXED_NOW
    private var database: NestlingDatabase? = null

    @Before
    fun setUp() {
        appContext().deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        database?.close()
        appContext().deleteDatabase(dbName)
    }

    @Test
    fun runningTimerIsRestoredWithTheCorrectStartTimeAfterProcessDeath() = runTest {
        val startedAt = now

        // --- first process: start a sleep timer and let it tick twice ---
        val first = onDiskDatabase(dbName).also { database = it }
        val repositoryBefore = repositoryFor(first) { now }
        repositoryBefore.startTimer(EventType.SLEEP)
        now += 60_000
        repositoryBefore.tick()
        now += 60_000
        repositoryBefore.tick()

        // --- the process dies mid-feed. No stop, no save, no goodbye. ---
        first.close()

        // --- relaunch ---
        val second = onDiskDatabase(dbName).also { database = it }
        val repositoryAfter = repositoryFor(second) { now }
        val restored = repositoryAfter.currentTimer()

        assertThat(restored).isNotNull()
        assertThat(restored!!.type).isEqualTo(EventType.SLEEP)
        assertThat(restored.startedAt).isEqualTo(startedAt)
        assertThat(restored.lastTickAt).isEqualTo(startedAt + 120_000)
        assertThat(restored.elapsedMillis(now)).isEqualTo(120_000)

        // And stopping it after the relaunch saves the whole feed, not just the part
        // that happened after the restart.
        now += 30_000
        repositoryAfter.stopTimer()

        val events = repositoryAfter.allEventsAscending()
        assertThat(events).hasSize(1)
        assertThat(events.first().startedAt).isEqualTo(startedAt)
        assertThat(events.first().durationMillis).isEqualTo(150_000)
        assertThat(repositoryAfter.currentTimer()).isNull()
    }

    @Test
    fun tickIsPersistedOnEveryMinuteBoundary() = runTest {
        val database = inMemoryDatabase().also { this@TimerRestoreTest.database = it }
        val repository = repositoryFor(database) { now }

        repository.startTimer(EventType.BOTTLE)
        assertThat(repository.currentTimer()?.lastTickAt).isEqualTo(FIXED_NOW)

        repeat(5) {
            now += 60_000
            repository.tick()
            assertThat(repository.currentTimer()?.lastTickAt).isEqualTo(now)
            // the start time never moves
            assertThat(repository.currentTimer()?.startedAt).isEqualTo(FIXED_NOW)
        }
    }

    @Test
    fun stoppingIsAtomic_eventExistsAndTimerIsGone() = runTest {
        val database = inMemoryDatabase().also { this@TimerRestoreTest.database = it }
        val repository = repositoryFor(database) { now }

        repository.startTimer(EventType.BOTTLE)
        now += 12 * 60_000
        val id = repository.stopTimer(amountMl = 120)

        assertThat(id).isNotNull()
        assertThat(repository.currentTimer()).isNull()
        val event = repository.eventById(id!!)
        assertThat(event).isNotNull()
        assertThat(event!!.amountMl).isEqualTo(120)
        assertThat(event.durationMillis).isEqualTo(12 * 60_000L)
    }

    @Test
    fun discardingATimerSavesNothing() = runTest {
        val database = inMemoryDatabase().also { this@TimerRestoreTest.database = it }
        val repository = repositoryFor(database) { now }

        repository.startTimer(EventType.SLEEP)
        now += 5 * 60_000
        repository.discardTimer()

        assertThat(repository.currentTimer()).isNull()
        assertThat(repository.eventCount()).isEqualTo(0)
    }
}
