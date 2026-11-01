package com.exercise.timer.presentation.reminder

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.exercise.timer.R
import dev.redfox.obsidian.components.ObSheet
import dev.redfox.obsidian.components.PrimaryButton
import dev.redfox.obsidian.theme.ObSpacing
import dev.redfox.obsidian.theme.ObsidianTheme
import dev.redfox.obsidian.theme.ScreenGutter
import java.time.LocalTime

/**
 * Obsidian has no clock picker, so this uses Material 3's; Obsidian installs a matching M3 theme,
 * and the colours below pin it to Obsidian's tokens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimeSheet(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ObsidianTheme.colors
    val picker = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    ObSheet(onDismiss = onDismiss, title = stringResource(R.string.reminder_time)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = ScreenGutter), contentAlignment = Alignment.Center) {
            TimePicker(
                state = picker,
                colors = TimePickerDefaults.colors(
                    clockDialColor = colors.tile,
                    clockDialSelectedContentColor = colors.onAccent,
                    clockDialUnselectedContentColor = colors.textPrimary,
                    selectorColor = colors.accent,
                    containerColor = colors.card,
                    periodSelectorBorderColor = colors.borderStrong,
                    periodSelectorSelectedContainerColor = colors.accentContainerStrong,
                    periodSelectorUnselectedContainerColor = colors.cardInner,
                    periodSelectorSelectedContentColor = colors.accentLight,
                    periodSelectorUnselectedContentColor = colors.textSecondary,
                    timeSelectorSelectedContainerColor = colors.accentContainerStrong,
                    timeSelectorUnselectedContainerColor = colors.tile,
                    timeSelectorSelectedContentColor = colors.accentLight,
                    timeSelectorUnselectedContentColor = colors.textPrimary,
                ),
            )
        }
        PrimaryButton(
            text = stringResource(R.string.action_save),
            onClick = { onConfirm(LocalTime.of(picker.hour, picker.minute)) },
            modifier = Modifier.padding(horizontal = ScreenGutter, vertical = ObSpacing.L),
        )
    }
}
