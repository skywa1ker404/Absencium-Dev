package com.trajectory.vault.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.trajectory.vault.ui.theme.LocalHazeState
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.blur.*
import kotlinx.coroutines.delay

/**
 * A card that is frosted glass when the Liquid Glass engine is active (it blurs the themed
 * backdrop behind it) and a regular tonal surface otherwise.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hazeState = LocalHazeState.current
    val shape = MaterialTheme.shapes.large
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val springSize = Modifier.animateContentSize(
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
    )

    if (hazeState != null) {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val tint = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.35f)
        val fallback = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
        val style = remember(tint, fallback) {
            HazeBlurStyle {
                blurRadius(24.dp)
                noiseFactor(0.1f)
                colorEffects(listOf(HazeColorEffect.tint(tint)))
                fallbackColorEffect(HazeColorEffect.tint(fallback))
            }
        }
        Column(
            modifier = modifier
                .clip(shape)
                .hazeBlur(input = HazeInput.Backdrop(hazeState), style = style)
                .border(1.dp, Color.White.copy(alpha = if (isDark) 0.18f else 0.45f), shape)
                .then(clickModifier)
                .then(springSize),
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Surface(
            modifier = modifier.then(springSize),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(modifier = Modifier.then(clickModifier).padding(16.dp), content = content)
        }
    }
}

/** A themed screen with a transparent top bar and a scrolling column of content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.semantics { contentDescription = "Back" },
                        ) {
                            Text("←", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp, start = 4.dp),
    )
}

/** A selectable card with a radio button. A disabled option is greyed out and explains why. */
@Composable
fun OptionCard(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        onClick = if (enabled) onClick else null,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private val AccentPresets = listOf(
    Color(0xFF6750A4),
    Color(0xFF1E6FD9),
    Color(0xFF00897B),
    Color(0xFF43A047),
    Color(0xFFF57C00),
    Color(0xFFD32F2F),
    Color(0xFFD81B60),
)

/**
 * Preset swatches plus an optional free-form color picker. Dragging in the picker is
 * debounced so the setting is written once the finger settles, not on every frame.
 */
@Composable
fun AccentColorPicker(
    selected: Color,
    onSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf<Color?>(null) }
    // Fixed starting color for the picker. Passing it explicitly also selects the `Color`
    // overload of ClassicColorPicker (the call is ambiguous without a `color` argument).
    val pickerStartColor = remember { selected }

    LaunchedEffect(draft) {
        val pending = draft ?: return@LaunchedEffect
        delay(250)
        onSelected(pending)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AccentPresets.forEach { preset ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(preset)
                        .border(
                            width = if (preset == selected) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                        )
                        .clickable { onSelected(preset) }
                        .semantics { contentDescription = "Accent color" },
                )
            }
        }
        TextButton(onClick = { showPicker = !showPicker }) {
            Text(if (showPicker) "Hide custom picker" else "Custom color…")
        }
        if (showPicker) {
            ClassicColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                color = pickerStartColor,
                showAlphaBar = false,
                onColorChanged = { color: HsvColor -> draft = color.toColor() },
            )
        }
    }
}
