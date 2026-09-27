package com.nestling.baby

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.nestling.baby.di.Graph

class NestlingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Open the database eagerly-ish: the first tap must not wait on disk.
        Graph.repository(this)
        // Inherit the user's wallpaper palette wherever the platform supports it.
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
