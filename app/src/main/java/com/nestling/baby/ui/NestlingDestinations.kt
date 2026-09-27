package com.nestling.baby.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nestling.baby.R

/**
 * The entire navigation graph. Two destinations.
 *
 * There is no login route, no onboarding route, no paywall route, and there never will
 * be — competitors' accounts are the reason this app exists. NavigationGraphTest asserts
 * it.
 */
enum class NestlingDestination(
    val route: String,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    TODAY("today", R.string.nav_today, R.drawable.ic_today),
    HISTORY("history", R.string.nav_history, R.drawable.ic_clock),
    ;

    companion object {
        val routes: List<String> = entries.map { it.route }

        fun fromRoute(route: String?): NestlingDestination =
            entries.firstOrNull { it.route == route } ?: TODAY
    }
}
