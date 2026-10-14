package com.absencium.app.ui.about

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.absencium.app.ui.components.PillButton
import com.absencium.app.ui.components.StepIndicator
import com.absencium.app.ui.icons.AppIcons
import com.absencium.app.ui.theme.LocalQuality
import kotlinx.coroutines.launch

private data class TourPage(val icon: ImageVector, val title: String, val body: String)

/** What the app does today. Keep each page true to what is in the app. */
private val Pages = listOf(
    TourPage(
        AppIcons.School,
        "Welcome to Absencium",
        "One place for your attendance, exams and notes. Everything is stored on your phone and works offline.",
    ),
    TourPage(
        AppIcons.CalendarMonth,
        "Attendance that adds up",
        "Mark each lecture present or absent. Absencium shows your percentage per subject and how many lectures you can still skip.",
    ),
    TourPage(
        AppIcons.Schedule,
        "Exams, holidays and timetable",
        "Add exams and holidays, even a whole festival break in one go. Saturday and Sunday can be holidays (a setting), and you can edit anything you saved.",
    ),
    TourPage(
        AppIcons.Folder,
        "A vault for your notes",
        "Keep notes and guides by subject. When you miss a class, the attendance card reminds you to collect that day's notes.",
    ),
    TourPage(
        AppIcons.Palette,
        "Make it yours, make it smooth",
        "Pick Monet, Liquid Glass, the Default Theme or a preset, and Performance or Quality mode to suit your phone. Performance mode turns down blur and animation; nothing is taken away.",
    ),
)

/** A short tour of what the app is for, opened from Settings. */
@Destination<RootGraph>
@Composable
fun IntroTourScreen(navigator: DestinationsNavigator) {
    IntroTourContent(onFinish = { navigator.navigateUp() })
}

/**
 * The tour itself. On the first launch it plays before the setup wizard and [onFinish] moves on to it;
 * from Settings, [onFinish] just goes back.
 */
@Composable
fun IntroTourContent(onFinish: () -> Unit) {
    val q = LocalQuality.current
    val pagerState = rememberPagerState(pageCount = { Pages.size })
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage
    val last = page == Pages.lastIndex

    BackHandler(enabled = page > 0) { scope.launch { pagerState.animateScrollToPage(page - 1) } }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text(if (last) "Close" else "Skip") }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val item = Pages[index]
            // The badge springs in each time its page becomes the current one.
            val pop = remember { Animatable(0.6f) }
            LaunchedEffect(page == index) {
                if (page == index) {
                    pop.snapTo(0.6f)
                    pop.animateTo(1f, q.spring(dampingRatio = 0.55f, stiffness = 300f))
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
                    CookieBadge(icon = item.icon, size = 160.dp)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(item.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepIndicator(current = page, total = Pages.size)
            PillButton(
                text = if (last) "Done" else "Next",
                onClick = {
                    if (last) onFinish() else scope.launch { pagerState.animateScrollToPage(page + 1) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
