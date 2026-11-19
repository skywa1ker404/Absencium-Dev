package me.rerere.rikkahub.ui.components.ui

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.rerere.rikkahub.ui.hooks.HapticPattern
import me.rerere.rikkahub.ui.hooks.rememberPremiumHaptics

/**
 * A Material 3 Switch wrapped with premium haptic feedback.
 * 
 * This component provides consistent haptic feedback across the app
 * when users toggle switches, following the "fidget toy" philosophy.
 * 
 * Uses [HapticPattern.Pop] for tactile feedback on state change.
 */
@Composable
fun HapticSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors()
) {
    val haptics = rememberPremiumHaptics()
    
    val change: (Boolean) -> Unit = { newValue ->
        haptics.perform(HapticPattern.Pop)
        onCheckedChange?.invoke(newValue)
    }
    val slot = me.rerere.rikkahub.ui.theme.HubStyle.switch
    if (slot != null) {
        // Inside Absencium the switch is the app's, so Pixelated and Dot matrix reach the chat (the app's switch takes no custom colours).
        slot(checked, change, modifier, enabled)
        return
    }
    Switch(
        checked = checked,
        onCheckedChange = change,
        modifier = modifier,
        thumbContent = thumbContent,
        enabled = enabled,
        colors = colors
    )
}
