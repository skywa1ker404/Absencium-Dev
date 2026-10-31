package skywa1ker404.absencium.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import skywa1ker404.absencium.ui.AiProvidersViewModel
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.SectionTitle
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
    val modelsViewModel = containerViewModel { AiModelsViewModel(it) }
    val modelsState by modelsViewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

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
        AppTextField(
            value = keyInput,
            onValueChange = { keyInput = it; error = null; testResult = null },
            label = { Text(if (keyTail != null) "Replace key" else if (provider.keyRequired) "API key" else "API key (optional)") },
            supportingText = {
                Text(
                    if (keyTail != null) "A key ending in $keyTail is saved, encrypted on this phone. It is kept for ${provider.label} when you switch."
                    else "Encrypted with a key held in the Android Keystore and never written to the debug log.",
                )
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        AppTextField(
            value = model,
            onValueChange = { model = it; error = null; testResult = null },
            label = { Text("Model") },
            supportingText = { Text("Exactly as the provider lists it. Fetch the list below to pick one.") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
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
        ModelPickerCard(
            provider = provider,
            state = modelsState,
            selectedId = model.trim(),
            onFetch = {
                val url = baseUrl.trim().trimEnd('/')
                error = when {
                    !(url.startsWith("https://") || url.startsWith("http://")) -> "The API URL must start with https:// or http://"
                    provider.keyRequired && keyInput.isBlank() && keyTail == null -> "Enter the API key first."
                    else -> null
                }
                if (error == null) {
                    viewModel.withKey(provider, keyInput) {
                        keyTail = viewModel.keyHint(provider)
                        keyInput = ""
                        modelsViewModel.load(connection())
                    }
                }
            },
            onPick = { model = it; error = null; testResult = null },
        )
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
                    val found = problem()
                    if (found != null) {
                        error = found
                    } else {
                        testing = true
                        testResult = null
                        viewModel.withKey(provider, keyInput) {
                            keyTail = viewModel.keyHint(provider)
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
                    val found = problem()
                    if (found != null) {
                        error = found
                    } else {
                        saving = true
                        val c = connection()
                        viewModel.save(provider, c.baseUrl, c.model, c.style, keyInput, use = true) { failure ->
                            saving = false
                            if (failure == null) onDone() else error = failure
                        }
                    }
                },
            )
        }
    }
    if (editing) {
        PillButton(
            text = "Remove ${provider.label}",
            tonal = true,
            onClick = { viewModel.remove(provider); onDone() },
            modifier = Modifier.padding(top = 4.dp),
        )
        HintText("Removing forgets its saved key, URL and model from this phone.")
    }
}
