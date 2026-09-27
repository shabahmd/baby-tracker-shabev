package com.nestling.baby

import com.google.common.truth.Truth.assertThat
import com.nestling.baby.ui.NestlingDestination
import org.junit.Test

/**
 * Acceptance criterion: no login screen exists anywhere in the nav graph.
 *
 * Competitors gate a 3am feed log behind an account. Nestling has two destinations and
 * neither of them can ever be an auth wall — if someone adds one, this fails.
 */
class NavigationGraphTest {

    private val forbidden = listOf(
        "login", "log_in", "signin", "sign_in", "signup", "sign_up", "register",
        "auth", "oauth", "account", "profile", "password", "paywall", "subscribe",
        "subscription", "premium", "pro", "upgrade", "onboarding", "welcome",
    )

    @Test
    fun theGraphIsExactlyTodayAndHistory() {
        assertThat(NestlingDestination.routes).containsExactly("today", "history").inOrder()
    }

    @Test
    fun noDestinationLooksLikeAnAuthOrPaywallScreen() {
        NestlingDestination.entries.forEach { destination ->
            val route = destination.route.lowercase()
            forbidden.forEach { word ->
                assertThat(route).doesNotContain(word)
            }
        }
    }

    @Test
    fun unknownRoutesFallBackToTodayRatherThanAnything() {
        assertThat(NestlingDestination.fromRoute(null)).isEqualTo(NestlingDestination.TODAY)
        assertThat(NestlingDestination.fromRoute("login")).isEqualTo(NestlingDestination.TODAY)
    }
}
