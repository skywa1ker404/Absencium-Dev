package skywa1ker404.absencium.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** A soft tint for a card and the colours that read on it. All of them come from the active theme, never from fixed values. */
@Immutable
class Hue(val container: Color, val onContainer: Color, val accent: Color)

/**
 * One of five theme-derived tints, picked by [key] so the same thing (a topic, a kind of material) always gets the same one.
 * Reference images 03, 09 and 12 tint each card by hue; here the hue follows the theme (Default, Monet, presets, Liquid Glass).
 */
@Composable
fun hueFor(key: Long): Hue {
    val c = MaterialTheme.colorScheme
    return when (((key % 5) + 5).toInt() % 5) {
        0 -> Hue(c.primaryContainer, c.onPrimaryContainer, c.primary)
        1 -> Hue(c.secondaryContainer, c.onSecondaryContainer, c.secondary)
        2 -> Hue(c.tertiaryContainer, c.onTertiaryContainer, c.tertiary)
        3 -> Hue(
            lerp(c.primaryContainer, c.tertiaryContainer, 0.5f),
            lerp(c.onPrimaryContainer, c.onTertiaryContainer, 0.5f),
            lerp(c.primary, c.tertiary, 0.5f),
        )
        else -> Hue(
            lerp(c.secondaryContainer, c.tertiaryContainer, 0.5f),
            lerp(c.onSecondaryContainer, c.onTertiaryContainer, 0.5f),
            lerp(c.secondary, c.tertiary, 0.5f),
        )
    }
}
