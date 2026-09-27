package com.nestling.baby.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * One 16dp rhythm, four spacing values, nothing else. Screens stay calm because there
 * is nothing to improvise with. DesignRhythmTest keeps these honest.
 */
object Spacing {
    /** Horizontal screen gutter and the vertical rhythm unit. */
    val screen = 16.dp

    /** Gap between two timeline rows — whitespace instead of a divider line. */
    val betweenRows = 8.dp

    /** Gap before a new section (date header, quick log block). */
    val section = 24.dp

    /** Inner padding of cards and sheets. */
    val card = 16.dp

    /** Minimum touch target for the three event buttons. Thumbs at 3am are not precise. */
    val minTouchTarget = 56.dp

    /** The event buttons are deliberately much bigger than the minimum. */
    val eventButtonHeight = 104.dp
}

/** Expressive shapes: generous, soft, no hard 4dp corners anywhere. */
val NestlingShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

/** Default Material 3 type scale. No custom fonts — the system font is the right font. */
val NestlingTypography = Typography()

/** Numerals that don't dance while a timer runs. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/**
 * Motion: M3 emphasized easing, and nothing over 300ms. Animation is for the sheet and
 * the FAB morph only; the timeline does not bounce, slide or fade.
 */
object Motion {
    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    const val DURATION_SHORT_MS = 180
    const val DURATION_MEDIUM_MS = 260
}
