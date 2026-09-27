package com.nestling.baby.di

import android.content.Context
import com.nestling.baby.data.DataStoreSettings
import com.nestling.baby.data.NestlingRepository
import com.nestling.baby.data.SettingsStore
import com.nestling.baby.data.local.NestlingDatabase

/**
 * A 200 line app does not need a DI framework. It needs one place where the database
 * is created exactly once — including when the process is started by the widget
 * receiver or the timer service rather than by the launcher.
 */
object Graph {

    @Volatile
    private var database: NestlingDatabase? = null

    @Volatile
    private var repositoryRef: NestlingRepository? = null

    @Volatile
    private var settingsRef: SettingsStore? = null

    fun repository(context: Context): NestlingRepository =
        repositoryRef ?: synchronized(this) {
            repositoryRef ?: NestlingRepository(
                eventDao = database(context).eventDao(),
                timerDao = database(context).timerDao(),
            ).also { repositoryRef = it }
        }

    fun settings(context: Context): SettingsStore =
        settingsRef ?: synchronized(this) {
            settingsRef ?: DataStoreSettings(context.applicationContext).also { settingsRef = it }
        }

    private fun database(context: Context): NestlingDatabase =
        database ?: synchronized(this) {
            database ?: NestlingDatabase.create(context).also { database = it }
        }

    /** Test seam: lets a unit test swap in an in-memory database. */
    fun overrideForTesting(repository: NestlingRepository?, settings: SettingsStore?) {
        synchronized(this) {
            repositoryRef = repository
            settingsRef = settings
        }
    }
}
