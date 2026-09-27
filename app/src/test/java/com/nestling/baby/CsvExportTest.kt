package com.nestling.baby

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nestling.baby.data.CsvExporter
import com.nestling.baby.data.local.NestlingDatabase
import com.nestling.baby.domain.DiaperKind
import com.nestling.baby.domain.EventType
import com.nestling.baby.domain.Side
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Acceptance criterion: the exported CSV contains every event ever logged. Count match,
 * id match, and the file on disk matches the string we generated.
 */
@RunWith(AndroidJUnit4::class)
class CsvExportTest {

    private var now = FIXED_NOW
    private val database: NestlingDatabase = inMemoryDatabase()
    private val repository = repositoryFor(database) { now }

    @After
    fun tearDown() = database.close()

    @Test
    fun csvContainsEveryEventEverLogged() = runTest {
        val logged = 250
        repeat(logged) { index ->
            when (index % 3) {
                0 -> {
                    repository.startTimer(EventType.BOTTLE)
                    now += 11 * 60_000
                    repository.stopTimer(amountMl = 90 + index % 60, side = Side.entries[index % 3])
                }

                1 -> {
                    repository.startTimer(EventType.SLEEP)
                    now += 47 * 60_000
                    repository.stopTimer()
                }

                else -> repository.logDiaper(DiaperKind.entries[index % 3])
            }
            now += 60_000
        }

        val all = repository.allEventsAscending()
        assertThat(all).hasSize(logged)
        assertThat(repository.eventCount()).isEqualTo(logged)

        val csv = CsvExporter.toCsv(all, TEST_ZONE)
        val lines = csv.trim().lines()

        assertThat(lines.first()).isEqualTo(CsvExporter.HEADER)
        assertThat(lines.size - 1).isEqualTo(repository.eventCount())

        val exportedIds = lines.drop(1).map { it.substringBefore(',').toLong() }
        assertThat(exportedIds).containsExactlyElementsIn(all.map { it.id })

        // ...and the same is true of the file that actually gets shared.
        val file = CsvExporter.write(appContext(), csv)
        val fileLines = file.readText().trim().lines()
        assertThat(fileLines.size - 1).isEqualTo(logged)
        assertThat(fileLines.first()).isEqualTo(CsvExporter.HEADER)
    }

    @Test
    fun csvKeepsAmountsSidesAndDiaperKinds() = runTest {
        repository.startTimer(EventType.BOTTLE)
        now += 10 * 60_000
        repository.stopTimer(amountMl = 150, side = Side.LEFT)
        repository.logDiaper(DiaperKind.DIRTY)

        val csv = CsvExporter.toCsv(repository.allEventsAscending(), TEST_ZONE)
        val rows = csv.trim().lines().drop(1)

        assertThat(rows).hasSize(2)
        assertThat(rows[0]).contains("bottle")
        assertThat(rows[0]).contains("left")
        assertThat(rows[0]).contains("150")
        assertThat(rows[0]).contains("600") // duration in seconds
        assertThat(rows[1]).contains("diaper")
        assertThat(rows[1]).contains("dirty")
    }

    @Test
    fun notesWithCommasDoNotBreakTheColumns() {
        val event = com.nestling.baby.domain.BabyEvent(
            id = 7,
            type = EventType.DIAPER,
            startedAt = FIXED_NOW,
            endedAt = FIXED_NOW,
            diaper = DiaperKind.WET,
            note = "big one, at grandma's \"house\"",
        )
        val row = CsvExporter.toCsv(listOf(event), TEST_ZONE).trim().lines()[1]

        // 8 separators outside the quoted note = 9 columns, same as the header.
        val outsideQuotes = row.fold(0 to false) { (count, inQuotes), char ->
            when {
                char == '"' -> count to !inQuotes
                char == ',' && !inQuotes -> count + 1 to inQuotes
                else -> count to inQuotes
            }
        }.first
        assertThat(outsideQuotes).isEqualTo(CsvExporter.HEADER.count { it == ',' })
    }
}
