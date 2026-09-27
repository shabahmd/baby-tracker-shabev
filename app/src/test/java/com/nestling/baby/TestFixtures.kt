package com.nestling.baby

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nestling.baby.data.NestlingRepository
import com.nestling.baby.data.SettingsStore
import com.nestling.baby.data.local.NestlingDatabase
import com.nestling.baby.domain.AppSettings
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** 2026-03-14 20:00 UTC — a fixed "now" so every assertion is deterministic. */
const val FIXED_NOW = 1_773_518_400_000L

val TEST_ZONE: ZoneId = ZoneId.of("UTC")

fun appContext(): Context = ApplicationProvider.getApplicationContext()

fun inMemoryDatabase(): NestlingDatabase =
    Room.inMemoryDatabaseBuilder(appContext(), NestlingDatabase::class.java)
        .allowMainThreadQueries()
        .build()

fun onDiskDatabase(name: String): NestlingDatabase =
    Room.databaseBuilder(appContext(), NestlingDatabase::class.java, name)
        .allowMainThreadQueries()
        .build()

fun repositoryFor(database: NestlingDatabase, clock: () -> Long): NestlingRepository =
    NestlingRepository(
        eventDao = database.eventDao(),
        timerDao = database.timerDao(),
        clock = clock,
        zone = { TEST_ZONE },
    )

/** In-memory settings so ViewModel tests never touch DataStore's file IO. */
class FakeSettingsStore(initial: AppSettings = AppSettings()) : SettingsStore {
    private val flow = MutableStateFlow(initial)

    override val settings = flow

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        flow.update(transform)
    }

    val current: AppSettings get() = flow.value
}
