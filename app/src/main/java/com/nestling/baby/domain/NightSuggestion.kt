package com.nestling.baby.domain

/**
 * "It's late, want the dim red theme?" — offered once per day, after 10pm or before 5am,
 * and never if night mode is already on. Pure so the rule is testable without a clock.
 */
object NightSuggestion {

    const val FROM_HOUR = 22
    const val UNTIL_HOUR = 5

    fun shouldSuggest(
        hourOfDay: Int,
        nightModeEnabled: Boolean,
        suppressedOn: String,
        today: String,
    ): Boolean {
        if (nightModeEnabled) return false
        if (suppressedOn == today) return false
        return hourOfDay >= FROM_HOUR || hourOfDay < UNTIL_HOUR
    }
}
