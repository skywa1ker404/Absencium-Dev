package skywa1ker404.absencium.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalQuality

/**
 * True inside a container that already animates its own size (an [AppCard], or the Add Entry popup card).
 *
 * Two size animations running on the same change fight each other and the layout visibly stutters, so exactly one
 * owner animates: when the host does, content inside it only fades in and out and changes size at once, and the host's
 * spring carries the motion. Anywhere else, [Reveal] and [InlineDropdown] animate their own size.
 */
val LocalHostAnimatesSize = staticCompositionLocalOf { false }

/** Marks [content] as living inside a container that animates its own size. */
@Composable
fun HostAnimatesSize(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHostAnimatesSize provides true, content = content)
}

/**
 * The app's expand and collapse: content fades in and its container grows with the Material 3 Expressive spring, the same
 * motion as the "Did you collect today's notes?" list. Use it instead of a bare AnimatedVisibility so every
 * expansion in the app moves the same way.
 */
@Composable
fun Reveal(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val q = LocalQuality.current
    if (LocalHostAnimatesSize.current) {
        AnimatedVisibility(visible = visible, modifier = modifier, enter = fadeIn(), exit = fadeOut()) { content() }
    } else {
        AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = fadeIn() + expandVertically(q.spring(dampingRatio = Spring.DampingRatioLowBouncy)),
            exit = fadeOut() + shrinkVertically(q.spring(dampingRatio = Spring.DampingRatioNoBouncy)),
        ) { content() }
    }
}

/** The chevron that turns over when something expands, with the same bouncy spring everywhere. */
@Composable
fun ExpandChevron(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    description: String? = null,
    tint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
    val q = LocalQuality.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chevron",
    )
    Icon(
        imageVector = AppIcons.ExpandMore,
        contentDescription = description,
        tint = if (tint == androidx.compose.ui.graphics.Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else tint,
        modifier = modifier.graphicsLayer { rotationZ = rotation },
    )
}

/**
 * The text field used everywhere in the app, so every field has the same shape, colours and focus look.
 * It takes the same parameters as Material's OutlinedTextField for the ones the app uses.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = FieldShape,
) {
    val colors = MaterialTheme.colorScheme
    val fieldFill = if (skywa1ker404.absencium.ui.theme.LocalGlassEnabled.current) colors.surfaceContainerLow.copy(alpha = 0.45f) else colors.surfaceContainerLow
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outlineVariant,
            focusedContainerColor = fieldFill,
            unfocusedContainerColor = fieldFill,
            disabledContainerColor = fieldFill,
        ),
    )
}

/** The same field for text whose selection the caller controls (the note editor's formatting toolbar moves the selection). */
@Composable
fun AppTextField(
    value: androidx.compose.ui.text.input.TextFieldValue,
    onValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    shape: Shape = FieldShape,
) {
    val colors = MaterialTheme.colorScheme
    val fieldFill = if (skywa1ker404.absencium.ui.theme.LocalGlassEnabled.current) colors.surfaceContainerLow.copy(alpha = 0.45f) else colors.surfaceContainerLow
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyLarge,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        minLines = minLines,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outlineVariant,
            focusedContainerColor = fieldFill,
            unfocusedContainerColor = fieldFill,
        ),
    )
}

/** The one corner shape of every text field and dropdown. */
val FieldShape: Shape = RoundedCornerShape(20.dp)

/** One choice of an [InlineDropdown]. */
data class DropdownOption<K>(val key: K, val label: String, val supporting: String? = null)

/**
 * The app's dropdown: a field that opens its list right below it, inside the screen, instead of a floating menu.
 * It looks and moves like the "Did you collect today's notes?" list: a bouncy chevron and a springy expansion.
 */
@Composable
fun <K> InlineDropdown(
    options: List<DropdownOption<K>>,
    selected: K?,
    onSelect: (K) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Choose",
    label: String? = null,
    enabled: Boolean = true,
    maxListHeight: androidx.compose.ui.unit.Dp = 240.dp,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    var open by rememberSaveable { mutableStateOf(false) }
    val chosen = options.firstOrNull { it.key == selected }
    val ownsSize = !LocalHostAnimatesSize.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(colors.surfaceContainerHighest)
            .then(if (ownsSize) Modifier.animateContentSize(q.spring(dampingRatio = Spring.DampingRatioLowBouncy)) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled && options.isNotEmpty(),
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.foundation.LocalIndication.current,
                ) { open = !open }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (label != null) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Text(
                    text = chosen?.label ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (chosen == null) colors.onSurfaceVariant else colors.onSurface,
                )
            }
            ExpandChevron(expanded = open, description = if (open) "Close list" else "Open list")
        }
        if (open) {
            Column(modifier = Modifier.heightIn(max = maxListHeight).verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option.key)
                                open = false
                            }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (option.key == selected) colors.primary else colors.onSurface,
                        )
                        if (option.supporting != null) {
                            Text(option.supporting, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
