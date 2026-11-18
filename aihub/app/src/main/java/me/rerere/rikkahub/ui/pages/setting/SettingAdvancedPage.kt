package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.components.dots.Icon
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.nav.OneUITopAppBar
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.pages.setting.components.SettingGroupItem
import me.rerere.rikkahub.ui.pages.setting.components.SettingsGroup

/** The rarely changed chat settings, kept together so the main settings list stays short. */
@Composable
fun SettingAdvancedPage() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val itemPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)

    Scaffold(
        topBar = {
            OneUITopAppBar(
                title = stringResource(R.string.setting_page_advanced),
                scrollBehavior = scrollBehavior,
                navigationIcon = { BackButton() },
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        ) {
            item {
                SettingsGroup(title = stringResource(R.string.setting_page_advanced)) {
                    SettingGroupItem(
                        title = stringResource(R.string.setting_fonts_title),
                        subtitle = stringResource(R.string.setting_fonts_app_font_desc),
                        icon = { Icon(Icons.Rounded.Tune, null, modifier = Modifier.size(20.dp)) },
                        contentPadding = itemPadding,
                        onClick = { navController.navigate(Screen.SettingFonts) }
                    )
                    SettingGroupItem(
                        title = stringResource(R.string.setting_ui_customization_title),
                        subtitle = stringResource(R.string.setting_display_ui_customization_desc),
                        icon = { Icon(Icons.Rounded.Brush, null, modifier = Modifier.size(20.dp)) },
                        contentPadding = itemPadding,
                        onClick = { navController.navigate(Screen.SettingUICustomization) }
                    )
                    SettingGroupItem(
                        title = stringResource(R.string.setting_page_search_service),
                        icon = { Icon(Icons.Rounded.Public, null, modifier = Modifier.size(20.dp)) },
                        contentPadding = itemPadding,
                        onClick = { navController.navigate(Screen.SettingSearch) }
                    )
                }
            }
        }
    }
}
