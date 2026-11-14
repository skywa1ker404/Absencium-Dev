package skywa1ker404.absencium.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import skywa1ker404.absencium.data.settings.AvatarShape

/** The chosen avatar shape and whether a ring and background are drawn behind avatars (Appearance > Avatars). */
data class AvatarStyle(val shape: AvatarShape = AvatarShape.LEAF8, val frame: Boolean = true)

val LocalAvatarStyle = compositionLocalOf { AvatarStyle() }

/** The Compose shape of an [AvatarShape]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AvatarShape.toComposeShape(): Shape =
    when (this) {
        AvatarShape.LEAF8 -> MaterialShapes.Clover8Leaf.toShape()
        AvatarShape.CIRCLE -> CircleShape
        AvatarShape.LEAF4 -> MaterialShapes.Clover4Leaf.toShape()
        AvatarShape.GHOST -> MaterialShapes.Ghostish.toShape()
        AvatarShape.NONE -> RectangleShape
    }
