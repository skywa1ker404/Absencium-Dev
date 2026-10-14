package com.absencium.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.absencium.app.ui.theme.LocalQuality

/**
 * The card every popup of the app is built on (Add Entry, Import holidays, Import exam timetable).
 *
 * - The window fills the screen and the card hangs from a fixed top edge, so when it grows it only grows downwards.
 *   In a centred window every size change also moved the top edge, which read as a jump.
 * - The card owns the one size animation: a firm spring that settles without a visible bounce. Everything inside only
 *   fades and changes size at once ([LocalHostAnimatesSize]); two competing size animations make the layout stutter.
 * - Frosted controls look different on every frame while the card resizes, so inside it controls use plain fills.
 */
@Composable
fun PopupShell(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                shape = RoundedCornerShape(34.dp),
                color = colors.surfaceContainerHigh,
                modifier = Modifier
                    .padding(top = 56.dp)
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .animateContentSize(q.spring(dampingRatio = 0.92f, stiffness = Spring.StiffnessMediumLow))
                    .heightIn(max = 680.dp),
            ) {
                CompositionLocalProvider(LocalCardBackdrop provides null, LocalHostAnimatesSize provides true) {
                    Box(modifier = Modifier.verticalScroll(rememberScrollState())) { content() }
                }
            }
        }
    }
}
