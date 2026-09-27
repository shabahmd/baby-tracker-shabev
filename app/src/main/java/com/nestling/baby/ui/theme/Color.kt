package com.nestling.baby.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Fallback palette for devices without dynamic colour (pre-Android 12). Calm sage +
 * warm clay: it should look like a nursery, not a dashboard.
 */
private val Sage = Color(0xFF3F6B5B)
private val SageContainer = Color(0xFFC3EAD6)
private val Clay = Color(0xFF7A5A4C)
private val ClayContainer = Color(0xFFF6DDD0)
private val Dusk = Color(0xFF456173)
private val DuskContainer = Color(0xFFCCE6F6)

val LightColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = SageContainer,
    onPrimaryContainer = Color(0xFF07261A),
    secondary = Clay,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = ClayContainer,
    onSecondaryContainer = Color(0xFF2D1710),
    tertiary = Dusk,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = DuskContainer,
    onTertiaryContainer = Color(0xFF071E29),
    background = Color(0xFFFBF8F6),
    onBackground = Color(0xFF1A1C1A),
    surface = Color(0xFFFBF8F6),
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFE2E6DF),
    onSurfaceVariant = Color(0xFF424A43),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F2EE),
    surfaceContainer = Color(0xFFF0ECE7),
    surfaceContainerHigh = Color(0xFFEAE6E1),
    surfaceContainerHighest = Color(0xFFE4E0DB),
    outline = Color(0xFF727A72),
    outlineVariant = Color(0xFFC2CAC1),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFA7CEBB),
    onPrimary = Color(0xFF10372A),
    primaryContainer = Color(0xFF284E3F),
    onPrimaryContainer = Color(0xFFC3EAD6),
    secondary = Color(0xFFE3BFAE),
    onSecondary = Color(0xFF442A20),
    secondaryContainer = Color(0xFF5D4035),
    onSecondaryContainer = Color(0xFFF6DDD0),
    tertiary = Color(0xFFA9CBE0),
    onTertiary = Color(0xFF0E3344),
    tertiaryContainer = Color(0xFF2C4A5B),
    onTertiaryContainer = Color(0xFFCCE6F6),
    background = Color(0xFF12140F),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF12140F),
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF424A43),
    onSurfaceVariant = Color(0xFFC2CAC1),
    surfaceContainerLowest = Color(0xFF0D0F0B),
    surfaceContainerLow = Color(0xFF1A1C17),
    surfaceContainer = Color(0xFF1E201B),
    surfaceContainerHigh = Color(0xFF282B25),
    surfaceContainerHighest = Color(0xFF333630),
    outline = Color(0xFF8C948B),
    outlineVariant = Color(0xFF424A43),
)

/**
 * True-black night mode. AMOLED black so the screen emits as little light as possible,
 * dim warm accents instead of blue, and text kept above 4.5:1 against pure black.
 *
 * Contrast (WCAG, against #000000):
 *   onSurface     #E6DCD4 -> 14.6:1
 *   onSurfaceVar. #B7A9A0 -> 8.6:1
 *   primary       #F09A80 -> 8.9:1
 */
val NightColors = darkColorScheme(
    primary = Color(0xFFF09A80),
    onPrimary = Color(0xFF1F0A04),
    primaryContainer = Color(0xFF3A1A10),
    onPrimaryContainer = Color(0xFFFFD9CB),
    secondary = Color(0xFFD9A98F),
    onSecondary = Color(0xFF1E0F07),
    secondaryContainer = Color(0xFF32180E),
    onSecondaryContainer = Color(0xFFF4D6C4),
    tertiary = Color(0xFFCFA98C),
    onTertiary = Color(0xFF1C1109),
    tertiaryContainer = Color(0xFF2C1B10),
    onTertiaryContainer = Color(0xFFEFD8C4),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6DCD4),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE6DCD4),
    surfaceVariant = Color(0xFF241E1A),
    onSurfaceVariant = Color(0xFFB7A9A0),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0807),
    surfaceContainer = Color(0xFF12100E),
    surfaceContainerHigh = Color(0xFF1A1715),
    surfaceContainerHighest = Color(0xFF221E1B),
    outline = Color(0xFF7A6E67),
    outlineVariant = Color(0xFF3A322E),
    error = Color(0xFFF2857A),
    onError = Color(0xFF1F0603),
    scrim = Color(0xFF000000),
)
