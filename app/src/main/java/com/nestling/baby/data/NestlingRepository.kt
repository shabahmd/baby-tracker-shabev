package com.nestling.baby.data

import com.nestling.baby.data.local.ActiveTimerEntity
import com.nestling.baby.data.local.EventDao
import com.nestling.baby.data.local.EventEntity
import com.nestling.baby.data.local.TimerDao
import com.nestling.baby.data.local.toDomain
import com.nestling.baby.domain.ActiveTimer
import com.nestling.baby.domain.BabyEvent
import com.nestling.baby.domain.DiaperKind
import com.nestling.baby.domain.EventType
import com.nestling.baby.domain.Side
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The single source of truth. Reads are Flows straight off Room, so the UI is always
 * fresh — which is exactly why pull-to-refresh does not exist in this app.
 */
class NestlingRepository(
    private val eventDao: EventDao,
    private val timerDao: TimerDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {

    fun observeDay(date: LocalDate): Flow<List<BabyEvent>> {
        val (from, until) = dayBounds(date)
        return eventDao.observeBetween(from, until).map { rows -> rows.map { it.toDomain() } }
    }

    fun observeAll(): Flow<List<BabyEvent>> =
        eventDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun search(query: String): Flow<List<BabyEvent>> {
        val pattern = "%" + query.trim().lowercase() + "%"
        return eventDao.search(pattern).map { rows -> rows.map { it.toDomain() } }
    }

    fun observeActiveTimer(): Flow<ActiveTimer?> =
        timerDao.observe().map { it?.toDomain() }

    suspend fun currentTimer(): ActiveTimer? = timerDao.current()?.toDomain()

    /** Start a live timer. Written to disk immediately: this is the 3am crash test. */
    suspend fun startTimer(type: EventType, side: Side? = null): ActiveTimer {
        val now = clock()
        val entity = ActiveTimerEntity(
            type = type.name,
            startedAt = now,
            lastTickAt = now,
            side = side?.name,
        )
        timerDao.start(entity)
        return entity.toDomain()
    }

    /** Persist the fact that the timer is still running. Called on every minute boundary. */
    suspend fun tick() {
        timerDao.tick(clock())
    }

    /**
     * Stop the running timer and store it as an event, in one transaction.
     * Returns the new event id, or null if nothing was running.
     */
    suspend fun stopTimer(amountMl: Int? = null, side: Side? = null): Long? {
        val active = timerDao.current() ?: return null
        val type = EventType.fromStorage(active.type)
        val now = clock().coerceAtLeast(active.startedAt)
        val event = EventEntity(
            type = active.type,
            startedAt = active.startedAt,
            endedAt = now,
            side = (side ?: Side.fromStorage(active.side))?.name,
            amountMl = amountMl?.takeIf { type == EventType.BOTTLE },
            diaper = null,
            note = null,
            createdAt = now,
        )
        return timerDao.stopAndLog(event)
    }

    suspend fun discardTimer() {
        timerDao.discard()
    }

    /** Diapers are instant: one tap for the type, saved, done. */
    suspend fun logDiaper(kind: DiaperKind): Long {
        val now = clock()
        return eventDao.log(
            EventEntity(
                type = EventType.DIAPER.name,
                startedAt = now,
                endedAt = now,
                side = null,
                amountMl = null,
                diaper = kind.name,
                note = null,
                createdAt = now,
            ),
        )
    }

    /** Used by tests and by the widget/service path that logs an already-finished event. */
    suspend fun logEvent(event: BabyEvent): Long {
        val now = clock()
        return eventDao.log(
            EventEntity(
                id = 0L,
                type = event.type.name,
                startedAt = event.startedAt,
                endedAt = event.endedAt,
                side = event.side?.name,
                amountMl = event.amountMl,
                diaper = event.diaper?.name,
                note = event.note,
                createdAt = now,
            ),
        )
    }

    suspend fun updateEvent(
        id: Long,
        amountMl: Int? = null,
        side: Side? = null,
        diaper: DiaperKind? = null,
    ) {
        val existing = eventDao.byId(id) ?: return
        eventDao.edit(
            existing.copy(
                amountMl = amountMl ?: existing.amountMl,
                side = side?.name ?: existing.side,
                diaper = diaper?.name ?: existing.diaper,
            ),
        )
    }

    suspend fun deleteEvent(id: Long) {
        eventDao.remove(id)
    }

    suspend fun eventById(id: Long): BabyEvent? = eventDao.byId(id)?.toDomain()

    /** Oldest first — the order a pediatrician reads a CSV in. */
    suspend fun allEventsAscending(): List<BabyEvent> = eventDao.allAscending().map { it.toDomain() }

    suspend fun eventCount(): Int = eventDao.count()

    private fun dayBounds(date: LocalDate): Pair<Long, Long> {
        val z = zone()
        val from = date.atStartOfDay(z).toInstant().toEpochMilli()
        val until = date.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli()
        return from to until
    }
}
