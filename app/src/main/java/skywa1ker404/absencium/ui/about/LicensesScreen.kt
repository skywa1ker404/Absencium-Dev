package skywa1ker404.absencium.ui.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.verticalScrollbar

/** The notices shipped inside the app (assets/licences, copied from docs/ by scripts/sync_licences.sh). */
private val LicenceFiles = listOf(
    "Third-party notices" to "licences/THIRD_PARTY_LICENSES.md",
    "Inter font (SIL OFL 1.1)" to "licences/licenses/Inter-OFL-1.1.txt",
    "Outfit font (SIL OFL 1.1)" to "licences/licenses/Outfit-OFL-1.1.txt",
    "Material icons (Apache 2.0)" to "licences/licenses/Material-Icons-Apache-2.0.txt",
    "Colour schemes (MIT notices)" to "licences/licenses/MIT-components.txt",
)

/** One row of the list: a heading, or a block of the notice text. */
private sealed interface Row {
    data class Heading(val text: String) : Row
    data class Body(val text: String) : Row
}

private const val MAX_BLOCK_CHARS = 2_500
private val UrlPattern = Regex("""https?://[^\s<>()\[\]]+""")

/**
 * Splits a notice into blocks at blank lines (and at line breaks inside a block longer than [MAX_BLOCK_CHARS]), so the
 * list only lays out and draws the few blocks that are on screen. The text itself is unchanged.
 */
private fun blocks(text: String): List<String> =
    text.replace("\r\n", "\n").split(Regex("\n\\s*\n")).flatMap { paragraph ->
        if (paragraph.length <= MAX_BLOCK_CHARS) {
            listOf(paragraph)
        } else {
            val parts = mutableListOf<String>()
            val current = StringBuilder()
            paragraph.lines().forEach { line ->
                if (current.isNotEmpty() && current.length + line.length > MAX_BLOCK_CHARS) {
                    parts += current.toString()
                    current.clear()
                }
                if (current.isNotEmpty()) current.append('\n')
                current.append(line)
            }
            if (current.isNotEmpty()) parts += current.toString()
            parts
        }
    }.filter { it.isNotBlank() }

/** The block's text with every web address turned into a tappable link. */
private fun linked(text: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    UrlPattern.findAll(text).forEach { match ->
        val url = match.value.trimEnd('.', ',', ';', ':')
        addLink(
            LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))),
            match.range.first,
            match.range.first + url.length,
        )
    }
}

@Destination<RootGraph>
@Composable
fun LicensesScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val rows by produceState<List<Row>>(emptyList()) {
        value = withContext(Dispatchers.IO) {
            buildList {
                LicenceFiles.forEach { (title, path) ->
                    add(Row.Heading(title))
                    val text = try {
                        context.assets.open(path).bufferedReader().use { it.readText() }
                    } catch (e: java.io.IOException) {
                        "This notice could not be loaded."
                    }
                    blocks(text).forEach { add(Row.Body(it)) }
                }
            }
        }
    }
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()

    // The page is a lazy list, not one tall scrolling column, so only the blocks on screen are laid out and drawn.
    ScreenScaffold(title = "Open-source licences", onBack = { navigator.navigateUp() }, scrollable = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            SelectionContainer(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().verticalScrollbar(listState, colors.onSurfaceVariant),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item(key = "intro") {
                        HintText("Absencium is built with open-source components. Their notices and licence texts are below.")
                    }
                    items(rows.size, key = { it }) { index ->
                        when (val row = rows[index]) {
                            is Row.Heading -> Text(
                                text = row.text,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                                modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp),
                            )
                            is Row.Body -> {
                                val annotated = remember(row.text, colors.primary) { linked(row.text, colors.primary) }
                                Text(
                                    text = annotated,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
