package com.nestling.baby.domain

/**
 * The only three things Nestling logs. Adding a fourth is a v2 conversation.
 */
enum class EventType {
    BOTTLE,
    SLEEP,
    DIAPER,
    ;

    val isTimed: Boolean get() = this != DIAPER

    companion object {
        fun fromStorage(value: String?): EventType =
            entries.firstOrNull { it.name == value } ?: BOTTLE
    }
}

enum class Side {
    LEFT,
    RIGHT,
    BOTH,
    ;

    companion object {
        fun fromStorage(value: String?): Side? = entries.firstOrNull { it.name == value }
    }
}

enum class DiaperKind {
    WET,
    DIRTY,
    BOTH,
    ;

    companion object {
        fun fromStorage(value: String?): DiaperKind? = entries.firstOrNull { it.name == value }
    }
}

enum class VolumeUnit {
    ML,
    OZ,
    ;

    companion object {
        fun fromStorage(value: String?): VolumeUnit = entries.firstOrNull { it.name == value } ?: ML
    }
}

/**
 * A single logged event. [startedAt]/[endedAt] are epoch milliseconds — the wall clock the
 * parent saw, never a server timestamp.
 */
data class BabyEvent(
    val id: Long = 0L,
    val type: EventType,
    val startedAt: Long,
    val endedAt: Long? = null,
    val side: Side? = null,
    val amountMl: Int? = null,
    val diaper: DiaperKind? = null,
    val note: String? = null,
) {
    val durationMillis: Long?
        get() = endedAt?.let { (it - startedAt).coerceAtLeast(0L) }
}

/**
 * A timer that is running right now. Persisted in Room on start and on every minute
 * boundary, so a process death can never turn a 40 minute feed into a lost one.
 */
data class ActiveTimer(
    val type: EventType,
    val startedAt: Long,
    val lastTickAt: Long,
    val side: Side? = null,
) {
    fun elapsedMillis(now: Long): Long = (now - startedAt).coerceAtLeast(0L)
}

/** User settings. All local, all optional, none of them an account. */
data class AppSettings(
    val babyName: String = "Baby",
    val nightMode: Boolean = false,
    val dynamicColor: Boolean = true,
    val unit: VolumeUnit = VolumeUnit.ML,
    val lastAmountMl: Int = 120,
    val lastSide: Side? = null,
    val nightPromptSuppressedOn: String = "",
)
