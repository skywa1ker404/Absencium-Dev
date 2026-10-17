package skywa1ker404.absencium.ui.about

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle

/** The notices shipped inside the app (assets/licences, copied from docs/ by scripts/sync_licences.sh). */
private val LicenceFiles = listOf(
    "Third-party notices" to "licences/THIRD_PARTY_LICENSES.md",
    "Inter font (SIL OFL 1.1)" to "licences/licenses/Inter-OFL-1.1.txt",
    "Outfit font (SIL OFL 1.1)" to "licences/licenses/Outfit-OFL-1.1.txt",
    "Material icons (Apache 2.0)" to "licences/licenses/Material-Icons-Apache-2.0.txt",
    "Colour schemes (MIT notices)" to "licences/licenses/MIT-components.txt",
)

@Destination<RootGraph>
@Composable
fun LicensesScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val texts by produceState<List<Pair<String, String>>>(emptyList()) {
        value = withContext(Dispatchers.IO) {
            LicenceFiles.map { (title, path) ->
                title to try {
                    context.assets.open(path).bufferedReader().use { it.readText() }
                } catch (e: java.io.IOException) {
                    "This notice could not be loaded."
                }
            }
        }
    }
    ScreenScaffold(title = "Open-source licences", onBack = { navigator.navigateUp() }) {
        HintText("Absencium is built with open-source components. Their notices and licence texts are below.")
        texts.forEach { (title, text) ->
            SectionTitle(title)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
