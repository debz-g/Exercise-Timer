package com.exercise.timer.presentation.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.exercise.timer.R
import com.exercise.timer.core.format.formatInterval
import com.exercise.timer.domain.model.TimerConfig
import dev.redfox.obsidian.components.ObSheet
import dev.redfox.obsidian.components.ObTextField
import dev.redfox.obsidian.components.PrimaryButton
import dev.redfox.obsidian.theme.ObSpacing
import dev.redfox.obsidian.theme.ScreenGutter

/** Minutes and seconds for an interval that isn't one of the presets. */
@Composable
fun CustomIntervalSheet(
    initialSeconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var minutes by rememberSaveable { mutableStateOf((initialSeconds / 60).takeIf { it > 0 }?.toString().orEmpty()) }
    var seconds by rememberSaveable { mutableStateOf((initialSeconds % 60).takeIf { it > 0 }?.toString().orEmpty()) }
    val total = (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
    val valid = total in TimerConfig.MIN_INTERVAL_SECONDS..TimerConfig.MAX_INTERVAL_SECONDS &&
        (seconds.toIntOrNull() ?: 0) < 60
    val number = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)

    ObSheet(onDismiss = onDismiss, title = stringResource(R.string.custom_title)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
            horizontalArrangement = Arrangement.spacedBy(ObSpacing.M),
        ) {
            ObTextField(
                value = minutes,
                onValueChange = { minutes = it.filter(Char::isDigit).take(2) },
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.custom_minutes),
                placeholder = "0",
                keyboardOptions = number,
            )
            ObTextField(
                value = seconds,
                onValueChange = { seconds = it.filter(Char::isDigit).take(2) },
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.custom_seconds),
                placeholder = "0",
                isError = (seconds.toIntOrNull() ?: 0) >= 60,
                supportingText = if ((seconds.toIntOrNull() ?: 0) >= 60) stringResource(R.string.custom_seconds_error) else null,
                keyboardOptions = number.copy(imeAction = ImeAction.Done),
            )
        }
        PrimaryButton(
            text = if (valid) {
                stringResource(R.string.custom_confirm, formatInterval(LocalContext.current, total))
            } else {
                stringResource(R.string.custom_confirm_empty)
            },
            onClick = { onConfirm(total) },
            enabled = valid,
            modifier = Modifier.padding(horizontal = ScreenGutter, vertical = ObSpacing.L),
        )
    }
}
