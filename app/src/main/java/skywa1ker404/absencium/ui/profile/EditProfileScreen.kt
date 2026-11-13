package skywa1ker404.absencium.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.AppMenu
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.settings.rememberSettingsViewModel
import skywa1ker404.absencium.ui.vault.entry.ImageTools
import skywa1ker404.absencium.ui.vault.entry.PhotoCropDialog
import java.io.File

private val COURSES = listOf(
    "B.Pharm", "D.Pharm", "Pharm.D", "M.Pharm", "MBBS", "BDS", "B.Sc", "M.Sc", "B.Tech", "B.E.", "M.Tech", "BCA", "MCA",
    "B.Com", "M.Com", "BA", "MA", "BBA", "MBA", "LLB", "Diploma",
)
private const val OTHER = "Other"
private val YEARS = (1..6).map { "Year $it" } + (1..12).map { "Semester $it" }

/** Course and year are kept as one entry ("B.Pharm, Year 2") so the AI never sees them as two unrelated facts. */
private fun splitCourseYear(joined: String): Pair<String, String> {
    val i = joined.lastIndexOf(", ")
    if (i < 0) return joined to ""
    val year = joined.substring(i + 2)
    return if (year in YEARS) joined.substring(0, i) to year else joined to ""
}

/** The profile as its own page: picture (with the app's crop tool), name, course and year as two pickers saved as one entry, and college. */
@Destination<RootGraph>
@Composable
fun EditProfileScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val settingsViewModel = rememberSettingsViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val loaded = settings

    var name by remember(loaded.displayName) { mutableStateOf(loaded.displayName) }
    val initial = remember(loaded.profileCourse, loaded.profileYear) {
        // An older profile kept the year apart; join it the first time the page opens.
        val (c, y) = splitCourseYear(loaded.profileCourse)
        c to y.ifBlank { loaded.profileYear }
    }
    var course by remember(initial) { mutableStateOf(initial.first) }
    var year by remember(initial) { mutableStateOf(initial.second) }
    var college by remember(loaded.profileCollege) { mutableStateOf(loaded.profileCollege) }
    var cropPath by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val temp = File(context.cacheDir, "profile-pick.jpg")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input -> temp.outputStream().use { input.copyTo(it) } }
                cropPath = temp.absolutePath
            }
        }
    }

    ScreenScaffold(title = "Edit profile", onBack = { navigator.navigateUp() }, largeTitle = true) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AvatarPicture(fileName = loaded.profileAvatar, initials = name, size = 112)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    text = if (loaded.profileAvatar.isBlank()) "Add picture" else "Change picture",
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    tonal = true,
                    compact = true,
                )
                if (loaded.profileAvatar.isNotBlank()) {
                    PillButton(text = "Remove", onClick = {
                        File(context.filesDir, loaded.profileAvatar).delete()
                        settingsViewModel.setProfileAvatar("")
                    }, tonal = true, compact = true)
                }
            }
        }
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true,
                    shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
                )
                var custom by remember(course) { mutableStateOf(course.isNotBlank() && course !in COURSES) }
                PickerField(
                    label = "Course",
                    value = if (custom) OTHER else course,
                    options = COURSES + OTHER,
                    onPick = {
                        if (it == OTHER) { custom = true; course = "" } else { custom = false; course = it }
                    },
                )
                if (custom) {
                    AppTextField(
                        value = course, onValueChange = { course = it }, label = { Text("Your course") }, singleLine = true,
                        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
                    )
                }
                PickerField(label = "Year or semester", value = year, options = YEARS, onPick = { year = it })
                AppTextField(
                    value = college, onValueChange = { college = it }, label = { Text("College") }, singleLine = true,
                    shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
                )
                HintText("Course and year are saved together as one line, and stay on this phone.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                    PillButton(text = "Save", onClick = {
                        settingsViewModel.setDisplayName(name.trim())
                        val joined = listOf(course.trim(), year.trim()).filter { it.isNotBlank() }.joinToString(", ")
                        settingsViewModel.setProfileDetails(joined, "", college)
                        navigator.navigateUp()
                    })
                    PillButton(text = "Cancel", tonal = true, onClick = { navigator.navigateUp() })
                }
            }
        }
    }

    cropPath?.let { path ->
        PhotoCropDialog(
            path = path,
            newFile = { File(context.filesDir, "profile-${System.currentTimeMillis()}.jpg") },
            onCancel = { cropPath = null },
            onDone = { file ->
                if (loaded.profileAvatar.isNotBlank()) File(context.filesDir, loaded.profileAvatar).delete()
                settingsViewModel.setProfileAvatar(file.name)
                File(path).delete()
                cropPath = null
            },
        )
    }
}

/** A read-only field that opens a menu of [options]. */
@Composable
private fun PickerField(label: String, value: String, options: List<String>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value.ifBlank { "Choose" }, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                }
                Icon(AppIcons.ExpandMore, contentDescription = null)
            }
        }
        AppMenu(expanded = open, onDismiss = { open = false }, solid = true) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { open = false; onPick(option) })
            }
        }
    }
}

/** The profile picture, or the initials (a person icon when there is no name) on the primary container. */
@Composable
fun AvatarPicture(fileName: String, initials: String, size: Int, modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = CircleShape) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val bitmap by produceState<ImageBitmap?>(null, fileName) {
        value = if (fileName.isBlank()) null else withContext(Dispatchers.IO) {
            ImageTools.decode(File(context.filesDir, fileName).absolutePath, size * 3)?.asImageBitmap()
        }
    }
    Box(
        modifier = modifier.size(size.dp).clip(shape).background(colors.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(image, contentDescription = "Profile picture", contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp))
        } else {
            val letters = initials.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
            if (letters.isEmpty()) {
                Icon(AppIcons.Person, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size((size / 2.5).dp))
            } else {
                Text(letters, style = MaterialTheme.typography.headlineMedium, color = colors.onPrimaryContainer)
            }
        }
    }
}
