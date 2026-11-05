package skywa1ker404.absencium.ui.aihub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.ui.AbsenciumAi

/**
 * The old AI hub is gone; Absencium AI is the embedded chat. Screens that still navigate here (and old links) land in the chat, and this
 * page is left again at once so Back returns to where the student came from.
 */
@Destination<RootGraph>
@Composable
fun AiHubScreen(navigator: DestinationsNavigator, chatId: Long? = null) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AbsenciumAi.open(context)
        navigator.navigateUp()
    }
}
