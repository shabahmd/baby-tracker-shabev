package com.nestling.baby.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EventEntity::class, ActiveTimerEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class NestlingDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    abstract fun timerDao(): TimerDao

    companion object {
        const val NAME = "nestling.db"

        fun create(context: Context): NestlingDatabase =
            Room.databaseBuilder(context.applicationContext, NestlingDatabase::class.java, NAME)
                // No fallbackToDestructiveMigration. Ever. Losing a parent's log is the
                // one bug this app exists to avoid; a failed migration must be loud.
                .build()
    }
}
