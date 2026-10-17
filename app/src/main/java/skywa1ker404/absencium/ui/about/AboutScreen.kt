package skywa1ker404.absencium.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ChangelogScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.NavCard
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.icons.AppIcons

const val DEVELOPER_NAME = "Skywa1ker"
const val REPOSITORY_URL = "https://github.com/skywa1ker404/Absencium"
const val WEBSITE_URL = "https://skywa1ker404.github.io/apps/absencium/"

@Destination<RootGraph>
@Composable
fun AboutScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    ScreenScaffold(title = "About", onBack = { navigator.navigateUp() }) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CookieBadge(icon = AppIcons.School, size = 72.dp)
                Column {
                    Text("Absencium", style = MaterialTheme.typography.headlineSmall)
                    if (version.isNotEmpty()) {
                        Text(
                            text = "Version $version",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "Attendance, exams and notes. Offline first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SectionTitle("Developer")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CookieBadge(icon = AppIcons.Person)
                Column {
                    Text(DEVELOPER_NAME, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Made by",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        val openLink = { url: String ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "No app can open links on this phone.", Toast.LENGTH_SHORT).show()
            }
        }
        NavCard(
            icon = AppIcons.OpenInNew,
            title = "Website",
            subtitle = "skywa1ker404.github.io/apps/absencium",
            onClick = { openLink(WEBSITE_URL) },
        )
        NavCard(
            icon = AppIcons.OpenInNew,
            title = "GitHub",
            subtitle = "github.com/skywa1ker404/Absencium",
            onClick = { openLink(REPOSITORY_URL) },
        )

        SectionTitle("What's new")
        NavCard(
            icon = AppIcons.History,
            title = "Changelog",
            subtitle = "What changed in each version",
            onClick = { navigator.navigate(ChangelogScreenDestination) },
        )
    }
}
