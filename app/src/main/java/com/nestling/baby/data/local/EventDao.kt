package com.nestling.baby.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Every write goes through a @Transaction method. Room commits or it doesn't — there is
 * no half-written feed.
 */
@Dao
abstract class EventDao {

    @Query("SELECT * FROM events WHERE startedAt >= :from AND startedAt < :until ORDER BY startedAt DESC, id DESC")
    abstract fun observeBetween(from: Long, until: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY startedAt DESC, id DESC")
    abstract fun observeAll(): Flow<List<EventEntity>>

    @Query(
        """
        SELECT * FROM events
        WHERE lower(type) LIKE :pattern
           OR lower(COALESCE(side, '')) LIKE :pattern
           OR lower(COALESCE(diaper, '')) LIKE :pattern
           OR lower(COALESCE(note, '')) LIKE :pattern
           OR CAST(COALESCE(amountMl, '') AS TEXT) LIKE :pattern
        ORDER BY startedAt DESC, id DESC
        """,
    )
    abstract fun search(pattern: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY startedAt ASC, id ASC")
    abstract suspend fun allAscending(): List<EventEntity>

    @Query("SELECT COUNT(*) FROM events")
    abstract suspend fun count(): Int

    @Query("SELECT * FROM events WHERE id = :id")
    abstract suspend fun byId(id: Long): EventEntity?

    @Query("DELETE FROM events WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Insert
    abstract suspend fun insert(event: EventEntity): Long

    @Update
    abstract suspend fun updateRow(event: EventEntity)

    @Transaction
    open suspend fun log(event: EventEntity): Long = insert(event)

    @Transaction
    open suspend fun edit(event: EventEntity) = updateRow(event)

    @Transaction
    open suspend fun remove(id: Long) = deleteById(id)
}
