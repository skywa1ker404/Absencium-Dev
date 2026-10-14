package com.absencium.app.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** An icon on a scalloped "cookie" badge, one of the Material 3 Expressive shapes. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CookieBadge(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialShapes.Cookie12Sided.toShape())
            .background(colors.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.onPrimaryContainer,
            modifier = Modifier.size(size / 2),
        )
    }
}
