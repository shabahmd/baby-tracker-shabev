package com.nestling.baby.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * The running timer lives in the database, not in memory. Starting a timer is a write;
 * so is every minute boundary. Stopping is a single transaction that inserts the event
 * and clears the timer — you can never end up with both, or neither.
 */
@Dao
abstract class TimerDao {

    @Query("SELECT * FROM active_timer WHERE id = ${ActiveTimerEntity.SINGLETON_ID}")
    abstract fun observe(): Flow<ActiveTimerEntity?>

    @Query("SELECT * FROM active_timer WHERE id = ${ActiveTimerEntity.SINGLETON_ID}")
    abstract suspend fun current(): ActiveTimerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(timer: ActiveTimerEntity)

    @Query("UPDATE active_timer SET lastTickAt = :now WHERE id = ${ActiveTimerEntity.SINGLETON_ID}")
    abstract suspend fun touch(now: Long)

    @Query("DELETE FROM active_timer")
    abstract suspend fun clear()

    @Insert
    abstract suspend fun insertEvent(event: EventEntity): Long

    @Transaction
    open suspend fun start(timer: ActiveTimerEntity) = upsert(timer)

    @Transaction
    open suspend fun tick(now: Long) = touch(now)

    /** Insert the finished event and clear the running timer, atomically. */
    @Transaction
    open suspend fun stopAndLog(event: EventEntity): Long {
        val id = insertEvent(event)
        clear()
        return id
    }

    @Transaction
    open suspend fun discard() = clear()
}
