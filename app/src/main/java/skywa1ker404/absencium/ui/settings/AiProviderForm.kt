package skywa1ker404.absencium.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.absencium.data.ai.AiConnection
import skywa1ker404.absencium.data.ai.AiProvider
import skywa1ker404.absencium.data.ai.ApiStyle
import skywa1ker404.absencium.data.ai.providerForKey
import skywa1ker404.absencium.ui.AiProvidersViewModel
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.components.SettingRow
import skywa1ker404.absencium.ui.icons.AppIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import skywa1ker404.absencium.ui.containerViewModel

/**
 * Sets up one provider: which service, its API URL, the model, and its own API key. The key is encrypted in the Android Keystore and
 * stays saved for this provider when the student switches to another one. [initial] is the provider being edited, or null when adding
 * (then the provider can be chosen). Used by the provider screen and by onboarding.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiProviderForm(
    initial: AiProvider?,
    viewModel: AiProvidersViewModel,
    onDone: () -> Unit,
    saveLabel: String = "Save and use",
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val editing = initial != null
    val saved = initial != null && rows.any { it.provider == initial }
    var provider by rememberSaveable { mutableStateOf(initial ?: AiProvider.entries.firstOrNull { p -> rows.none { it.provider == p } } ?: AiProvider.OPENAI) }
    val start = remember(provider) { viewModel.profileFor(provider) }
    var baseUrl by rememberSaveable(provider) { mutableStateOf(start.baseUrl) }
    var model by rememberSaveable(provider) { mutableStateOf(start.model) }
    var style by rememberSaveable(provider) { mutableStateOf(start.style) }
    // Not rememberSaveable: the key must not be written into saved instance state.
    var keyInput by remember(provider) { mutableStateOf("") }
    var keyTail by remember(provider) { mutableStateOf(viewModel.keyHint(provider)) }
    var error by remember(provider) { mutableStateOf<String?>(null) }
    var testing by remember(provider) { mutableStateOf(false) }
    var testResult by remember(provider) { mutableStateOf<Pair<Boolean, String>?>(null) }
    var saving by remember(provider) { mutableStateOf(false) }
    // The API URL and style are filled in for every known provider; only a custom one needs them, so they sit under Advanced.
    var advanced by rememberSaveable(provider) { mutableStateOf(provider == AiProvider.CUSTOM) }
    val advancedTurn by animateFloatAsState(if (advanced) 180f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "advancedArrow")
    val modelsViewModel = containerViewModel { AiModelsViewModel(it) }
    val modelsState by modelsViewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    /** A typed key becomes the first key of a provider; once one is saved, typed keys are added with Add key instead. */
    fun firstKey(): String = if (keyTail == null) keyInput else ""

    fun connection() = AiConnection(
        provider,
        if (provider == AiProvider.CUSTOM) style else provider.style,
        baseUrl.trim().trimEnd('/'),
        model.trim(),
    )

    fun problem(): String? {
        val url = baseUrl.trim().trimEnd('/')
        return when {
            !(url.startsWith("https://") || url.startsWith("http://")) -> "The API URL must start with https:// or http://"
            url.substringAfter("://").isBlank() -> "Enter the full API URL."
            model.isBlank() -> "Enter the model name, or fetch the list and pick one."
            provider.keyRequired && keyInput.isBlank() && keyTail == null -> "Enter the API key for ${provider.label}."
            else -> null
        }
    }

    if (!editing) {
        SectionTitle("Provider")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AiProvider.entries.forEach { option ->
                ExpressiveChip(
                    selected = provider == option,
                    onClick = { provider = option; modelsViewModel.reset() },
                    label = { Text(option.label, maxLines = 1, softWrap = false) },
                    shape = CircleShape,
                )
            }
        }
    }

    SectionTitle(if (editing) provider.label else "Connection")
    AppCard(modifier = Modifier.fillMaxWidth()) {
        var tails by remember(provider) { mutableStateOf(viewModel.keyTails(provider)) }
        var rests by remember(provider) { mutableStateOf(viewModel.keyResting(provider)) }
        fun refreshKeys() {
            tails = viewModel.keyTails(provider)
            rests = viewModel.keyResting(provider)
            keyTail = tails.firstOrNull()
        }
        /** A key typed but not added yet, while one is already saved, is added now so it is not lost. */
        fun commitExtra() {
            if (tails.isNotEmpty() && keyInput.isNotBlank() && viewModel.addKey(provider, keyInput)) { keyInput = ""; refreshKeys() }
        }
        if (tails.isNotEmpty()) {
            Text("API keys", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            tails.forEachIndexed { i, tail ->
                SettingRow(
                    title = "Key ending in $tail",
                    subtitle = (if (i == 0) "In use. Tried first." else "Tap to use this key instead.") + (if (rests.getOrNull(i) == true) " Resting after a limit." else ""),
                    onClick = { viewModel.useKey(provider, i); refreshKeys() },
                    leading = { RadioButton(selected = i == 0, onClick = null) },
                    trailing = {
                        IconButton(onClick = { viewModel.removeKey(provider, i); refreshKeys() }) {
                            Icon(AppIcons.Delete, contentDescription = "Remove key ending in $tail", tint = colors.onSurfaceVariant)
                        }
                    },
                )
            }
            if (tails.size > 1) HintText("When the key in use reaches its limit, the next one is tried by itself; when all have, another provider with a key takes over.")
        }
        AppTextField(
            value = keyInput,
            onValueChange = { keyInput = it; error = null; testResult = null },
            label = { Text(if (tails.isNotEmpty()) "Add another key" else if (provider.keyRequired) "API key" else "API key (optional)") },
            supportingText = { Text("Encrypted with a key held in the Android Keystore and never written to the debug log. Every key stays saved for ${provider.label} when you switch.") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        val detected = providerForKey(keyInput)
        if (detected != null && detected != provider) {
            Text(
                "This looks like a ${detected.label} key, not a ${provider.label} one.",
                style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(top = 4.dp),
            )
            if (!editing) {
                PillButton(
                    text = "Use ${detected.label}",
                    onClick = { provider = detected; modelsViewModel.reset() },
                    tonal = true, compact = true, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        if (tails.isNotEmpty() && keyInput.isNotBlank()) {
            PillButton(
                text = "Add key",
                onClick = {
                    if (viewModel.addKey(provider, keyInput)) { keyInput = ""; refreshKeys() } else error = "That key is already saved, or there is no room for more."
                },
                tonal = true, compact = true, modifier = Modifier.padding(top = 4.dp),
            )
        }
        AppTextField(
            value = model,
            onValueChange = { model = it; error = null; testResult = null },
            label = { Text("Model") },
            supportingText = { Text("Exactly as the provider lists it. Fetch the list below to pick one.") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        ModelPickerCard(
            provider = provider,
            state = modelsState,
            selectedId = model.trim(),
            onFetch = {
                val url = baseUrl.trim().trimEnd('/')
                if (!(url.startsWith("https://") || url.startsWith("http://"))) advanced = true
                error = when {
                    !(url.startsWith("https://") || url.startsWith("http://")) -> "The API URL must start with https:// or http://"
                    provider.keyRequired && keyInput.isBlank() && keyTail == null -> "Enter the API key first."
                    else -> null
                }
                if (error == null) {
                    viewModel.withKey(provider, firstKey()) {
                        refreshKeys()
                        keyInput = ""
                        modelsViewModel.load(connection())
                    }
                }
            },
            onPick = { model = it; error = null; testResult = null },
        )
        SettingRow(
            title = "Advanced",
            subtitle = if (provider == AiProvider.CUSTOM) "API URL and API style" else "API URL",
            onClick = { advanced = !advanced },
            modifier = Modifier.padding(top = 8.dp),
            trailing = { Icon(AppIcons.ExpandMore, contentDescription = if (advanced) "Hide advanced options" else "Show advanced options", modifier = Modifier.rotate(advancedTurn)) },
        )
        AnimatedVisibility(
            visible = advanced,
            enter = expandVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeOut(),
        ) {
            Column {
                AppTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it; error = null; testResult = null },
                    label = { Text("API URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (baseUrl.trim().startsWith("http://")) {
                    HintText("Plain http is only meant for a server on your own network.", Modifier.padding(top = 4.dp))
                }
                if (provider == AiProvider.CUSTOM) {
                    Text("API style", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        ApiStyle.entries.forEach { option ->
                            ExpressiveChip(
                                selected = style == option,
                                onClick = { style = option; testResult = null },
                                label = { Text(option.label, maxLines = 1, softWrap = false) },
                                shape = CircleShape,
                            )
                        }
                    }
                }
            }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(top = 8.dp)) }
        testResult?.let { (ok, text) ->
            Text(text, style = MaterialTheme.typography.bodySmall, color = if (ok) colors.primary else colors.error, modifier = Modifier.padding(top = 8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            PillButton(
                text = "Test connection",
                tonal = true,
                loading = testing,
                onClick = {
                    commitExtra()
                    val found = problem()
                    if (found != null) {
                        error = found
                        if (found.contains("API URL")) advanced = true
                    } else {
                        testing = true
                        testResult = null
                        viewModel.withKey(provider, firstKey()) {
                            refreshKeys()
                            keyInput = ""
                            viewModel.test(connection()) { ok, text ->
                                testing = false
                                testResult = ok to text
                            }
                        }
                    }
                },
            )
            PillButton(
                text = saveLabel,
                loading = saving,
                onClick = {
                    commitExtra()
                    val found = problem()
                    if (found != null) {
                        error = found
                        if (found.contains("API URL")) advanced = true
                    } else {
                        saving = true
                        val c = connection()
                        viewModel.save(provider, c.baseUrl, c.model, c.style, firstKey(), use = true) { failure ->
                            saving = false
                            if (failure == null) onDone() else error = failure
                        }
                    }
                },
            )
        }
    }
    if (saved) {
        PillButton(
            text = "Remove ${provider.label}",
            tonal = true,
            onClick = { viewModel.remove(provider, onDone) },
            modifier = Modifier.padding(top = 4.dp),
        )
        HintText("Removing forgets its saved key, URL and model from this phone.")
    }
}
