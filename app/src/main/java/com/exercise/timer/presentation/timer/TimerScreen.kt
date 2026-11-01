package com.exercise.timer.presentation.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exercise.timer.R
import com.exercise.timer.core.format.formatInterval
import com.exercise.timer.presentation.app.LocalSnackbars
import dev.redfox.obsidian.components.ChoicePill
import dev.redfox.obsidian.components.ObCard
import dev.redfox.obsidian.components.ObListRow
import dev.redfox.obsidian.components.ObSwitch
import dev.redfox.obsidian.components.PillButton
import dev.redfox.obsidian.components.PrimaryButton
import dev.redfox.obsidian.components.SectionLabel
import dev.redfox.obsidian.components.SwitchSize
import dev.redfox.obsidian.components.TabPage
import dev.redfox.obsidian.effects.glassSource
import dev.redfox.obsidian.motion.CollapseExit
import dev.redfox.obsidian.motion.ExpandEnter
import dev.redfox.obsidian.motion.revealOnEnter
import dev.redfox.obsidian.theme.ObSpacing
import dev.redfox.obsidian.theme.ObsidianTheme
import dev.redfox.obsidian.theme.ScreenGutter

/** Wires the ViewModel to the stateless [TimerContent]: state down, intents up, effects once. */
@Composable
fun TimerScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbars = LocalSnackbars.current
    val context = LocalContext.current
    val sleepEnded = stringResource(R.string.sleep_timer_ended)

    // Can be switched in Settings while we're away, so re-check on every resume.
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(TimerIntent.LiveUpdatesChecked(canPostLiveUpdates(context)))
        onPauseOrDispose {}
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TimerEffect.SleepTimerEndedSession -> snackbars.show(sleepEnded)
                TimerEffect.OpenLiveUpdateSettings -> openLiveUpdateSettings(context)
            }
        }
    }
    TimerContent(state, viewModel::onIntent)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerContent(state: TimerUiState, onIntent: (TimerIntent) -> Unit) {
    val colors = ObsidianTheme.colors
    val running = state.session as? TimerUiState.Session.Running

    TabPage(title = stringResource(R.string.tab_timer)) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .glassSource()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    // Room to scroll the last card clear of the pinned button.
                    .padding(start = ScreenGutter, end = ScreenGutter, bottom = StartButtonClearance),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedVisibility(visible = !state.liveUpdatesAllowed, enter = ExpandEnter, exit = CollapseExit) {
                    ObCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = ObSpacing.S),
                        contentPadding = PaddingValues(ObSpacing.L),
                        borderColor = colors.accentBorder,
                    ) {
                        ObListRow(
                            title = stringResource(R.string.live_updates_title),
                            subtitle = stringResource(R.string.live_updates_subtitle),
                            icon = R.drawable.ic_notifications_active,
                            iconTint = colors.accentLight,
                            onClick = { onIntent(TimerIntent.AllowLiveUpdates) },
                            trailing = {
                                PillButton(
                                    text = stringResource(R.string.action_turn_on),
                                    onClick = { onIntent(TimerIntent.AllowLiveUpdates) },
                                    contentColor = colors.accentLight,
                                    containerColor = colors.accentContainerStrong,
                                )
                            },
                        )
                    }
                }
                CountdownRing(
                    intervalSeconds = state.config.intervalSeconds,
                    session = state.session,
                    modifier = Modifier.revealOnEnter(0).padding(vertical = ObSpacing.S),
                )
                Text(
                    text = if (running != null) {
                        stringResource(R.string.running_caption, formatInterval(LocalContext.current, running.intervalSeconds))
                    } else {
                        stringResource(R.string.idle_caption)
                    },
                    style = ObsidianTheme.typography.body,
                    color = colors.textSecondary,
                    modifier = Modifier.revealOnEnter(1).padding(bottom = ObSpacing.L),
                )

                AnimatedVisibility(visible = running == null, enter = ExpandEnter, exit = CollapseExit) {
                    IntervalPicker(state, onIntent, Modifier.revealOnEnter(2))
                }

                ObCard(
                    modifier = Modifier.fillMaxWidth().revealOnEnter(3).padding(top = ObSpacing.L),
                    contentPadding = PaddingValues(ObSpacing.L),
                ) {
                    ObListRow(
                        title = stringResource(R.string.sleep_timer),
                        icon = R.drawable.ic_bedtime,
                        subtitle = when (val left = running?.sleepMinutesLeft) {
                            null -> stringResource(R.string.sleep_timer_subtitle)
                            else -> pluralStringResource(R.plurals.sleep_timer_left, left, left)
                        },
                        onClick = { onIntent(TimerIntent.SetSleepTimer(!state.config.sleepTimerEnabled)) },
                        trailing = {
                            ObSwitch(
                                checked = state.config.sleepTimerEnabled,
                                onCheckedChange = { onIntent(TimerIntent.SetSleepTimer(it)) },
                                size = SwitchSize.Large,
                            )
                        },
                    )
                }
            }

            // Start/Stop stays on screen above the nav bar: it's the one control that always matters.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(colors.background.copy(alpha = 0f), colors.background)))
                    .padding(top = ObSpacing.XL, bottom = padding.calculateBottomPadding())
                    .padding(horizontal = ScreenGutter),
            ) {
                PrimaryButton(
                    text = stringResource(if (running != null) R.string.action_stop else R.string.action_start),
                    icon = if (running != null) R.drawable.ic_stop else R.drawable.ic_play_arrow,
                    onClick = { onIntent(if (running != null) TimerIntent.Stop else TimerIntent.Start) },
                    modifier = Modifier.revealOnEnter(4),
                )
            }
        }
    }

    if (state.customSheetOpen) {
        CustomIntervalSheet(
            initialSeconds = state.config.intervalSeconds,
            onConfirm = { onIntent(TimerIntent.ConfirmCustomInterval(it)) },
            onDismiss = { onIntent(TimerIntent.DismissCustomInterval) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntervalPicker(state: TimerUiState, onIntent: (TimerIntent) -> Unit, modifier: Modifier = Modifier) {
    val selected = state.config.intervalSeconds
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ObSpacing.S)) {
        SectionLabel(stringResource(R.string.section_interval).uppercase())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ObSpacing.S), verticalArrangement = Arrangement.spacedBy(ObSpacing.S)) {
            IntervalPresets.all.forEach { seconds ->
                ChoicePill(
                    text = if (seconds < 60) {
                        pluralStringResource(R.plurals.seconds_short, seconds, seconds)
                    } else {
                        pluralStringResource(R.plurals.minutes_short, seconds / 60, seconds / 60)
                    },
                    selected = selected == seconds,
                    onClick = { onIntent(TimerIntent.SelectInterval(seconds)) },
                )
            }
            val custom = state.customSeconds
            ChoicePill(
                text = if (custom != null) {
                    stringResource(R.string.custom_selected, formatInterval(LocalContext.current, custom))
                } else {
                    stringResource(R.string.custom)
                },
                selected = custom != null,
                onClick = { onIntent(TimerIntent.OpenCustomInterval) },
            )
        }
    }
}

/** The pinned Start/Stop button's height plus its top fade. */
private val StartButtonClearance = 96.dp

/** Live Updates arrived in Android 16; before that there's nothing to switch on. */
private fun canPostLiveUpdates(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA ||
        context.getSystemService(NotificationManager::class.java).canPostPromotedNotifications()

private fun openLiveUpdateSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // Some phones don't have the dedicated page; the app's notification settings hold the same switch.
    runCatching { context.startActivity(intent) }.onFailure {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
