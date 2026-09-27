package com.nestling.baby.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nestling.baby.domain.ActiveTimer
import com.nestling.baby.domain.BabyEvent
import com.nestling.baby.domain.DiaperKind
import com.nestling.baby.domain.EventType
import com.nestling.baby.domain.Side

@Entity(
    tableName = "events",
    indices = [Index("startedAt"), Index("type")],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val startedAt: Long,
    val endedAt: Long?,
    val side: String?,
    val amountMl: Int?,
    val diaper: String?,
    val note: String?,
    val createdAt: Long,
)

@Entity(tableName = "active_timer")
data class ActiveTimerEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val type: String,
    val startedAt: Long,
    val lastTickAt: Long,
    val side: String?,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

fun EventEntity.toDomain(): BabyEvent = BabyEvent(
    id = id,
    type = EventType.fromStorage(type),
    startedAt = startedAt,
    endedAt = endedAt,
    side = Side.fromStorage(side),
    amountMl = amountMl,
    diaper = DiaperKind.fromStorage(diaper),
    note = note,
)

fun BabyEvent.toEntity(createdAt: Long): EventEntity = EventEntity(
    id = id,
    type = type.name,
    startedAt = startedAt,
    endedAt = endedAt,
    side = side?.name,
    amountMl = amountMl,
    diaper = diaper?.name,
    note = note,
    createdAt = createdAt,
)

fun ActiveTimerEntity.toDomain(): ActiveTimer = ActiveTimer(
    type = EventType.fromStorage(type),
    startedAt = startedAt,
    lastTickAt = lastTickAt,
    side = Side.fromStorage(side),
)
