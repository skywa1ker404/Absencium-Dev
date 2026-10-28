package skywa1ker404.absencium.ui.components

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import skywa1ker404.absencium.ui.theme.LocalGlassEnabled

private fun crossWindowBlur(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        runCatching { context.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true }.getOrDefault(false)

/** Asks the system to blur the app behind this dialog's window (Android 12 and later, where the phone allows it). */
@Composable
private fun BlurBehindDialog(radiusDp: Int) {
    val view = LocalView.current
    val density = LocalDensity.current
    SideEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val params = window.attributes
            params.blurBehindRadius = (radiusDp * density.density).toInt()
            window.attributes = params
        }
    }
}

/**
 * The app's dialog, used in place of the stock `AlertDialog` everywhere (same parameters). Large rounded corners and a thin edge.
 * With Liquid Glass and frosted overlays it is see-through in the theme's colour and, where the phone supports it, the app behind
 * it is blurred by the system; otherwise it is a plain, nearly opaque surface so the text stays readable.
 */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    /** Callers may set their own corner size; the default is the app's large dialog shape. */
    shape: Shape = RoundedCornerShape(32.dp),
    containerColor: Color = Color.Unspecified,
    titleContentColor: Color = Color.Unspecified,
    textContentColor: Color = Color.Unspecified,
    properties: DialogProperties = DialogProperties(),
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val frosted = LocalGlassEnabled.current && LocalFrostedOverlays.current
    val blur = frosted && remember { crossWindowBlur(context) }
    val container = if (containerColor != Color.Unspecified) containerColor else when {
        blur -> colors.surfaceContainerHigh.copy(alpha = 0.66f)
        frosted -> colors.surfaceContainerHigh.copy(alpha = 0.94f)
        else -> colors.surfaceContainerHigh
    }
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier, properties = properties) {
        if (blur) BlurBehindDialog(radiusDp = 24)
        Surface(
            shape = shape,
            color = container,
            tonalElevation = 0.dp,
            border = BorderStroke(0.6.dp, Color.White.copy(alpha = if (frosted) 0.18f else 0.08f)),
            modifier = Modifier.widthIn(min = 280.dp, max = 560.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (icon != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { icon() }
                if (title != null) {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                        CompositionLocalProvider(LocalContentColor provides (if (titleContentColor != Color.Unspecified) titleContentColor else colors.onSurface)) { title() }
                    }
                }
                if (text != null) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        CompositionLocalProvider(LocalContentColor provides (if (textContentColor != Color.Unspecified) textContentColor else colors.onSurfaceVariant)) { text() }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (dismissButton != null) {
                        dismissButton()
                        Spacer(Modifier.width(8.dp))
                    }
                    confirmButton()
                }
            }
        }
    }
}
