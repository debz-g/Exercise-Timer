package com.exercise.timer.presentation.reminder

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exercise.timer.R
import com.exercise.timer.presentation.app.LocalSnackbars
import dev.redfox.obsidian.components.ObCard
import dev.redfox.obsidian.components.ObDivider
import dev.redfox.obsidian.components.ObListRow
import dev.redfox.obsidian.components.ObSwitch
import dev.redfox.obsidian.components.PillButton
import dev.redfox.obsidian.components.SectionLabel
import dev.redfox.obsidian.components.TabPage
import dev.redfox.obsidian.effects.glassSource
import dev.redfox.obsidian.motion.RevealColumn
import dev.redfox.obsidian.motion.RollingText
import dev.redfox.obsidian.theme.ObSpacing
import dev.redfox.obsidian.theme.ObsidianTheme
import dev.redfox.obsidian.theme.ScreenGutter
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ReminderScreen(viewModel: ReminderViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbars = LocalSnackbars.current
    val notifications = NotificationManagerCompat.from(context)

    // The permission can change in Settings while we're away, so re-check on every resume.
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(ReminderIntent.NotificationPermissionChecked(notifications.areNotificationsEnabled()))
        onPauseOrDispose {}
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onIntent(ReminderIntent.NotificationPermissionChecked(granted))
        // Asked too often before: Android won't show the prompt, so open the app's notification settings.
        if (!granted) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    val savedMessage = stringResource(R.string.reminder_saved)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ReminderEffect.RequestNotificationPermission -> permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                is ReminderEffect.ReminderSaved -> snackbars.show(savedMessage.format(effect.time.formatted()))
            }
        }
    }

    ReminderContent(state, viewModel::onIntent)
}

@Composable
fun ReminderContent(state: ReminderUiState, onIntent: (ReminderIntent) -> Unit) {
    val colors = ObsidianTheme.colors
    TabPage(title = stringResource(R.string.tab_reminder)) { padding ->
        RevealColumn(
            Modifier
                .glassSource()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = ScreenGutter),
        ) {
            if (!state.notificationsAllowed) {
                ObCard(Modifier.fillMaxWidth().padding(bottom = ObSpacing.L), contentPadding = PaddingValues(ObSpacing.L)) {
                    ObListRow(
                        title = stringResource(R.string.notifications_off_title),
                        subtitle = stringResource(R.string.notifications_off_subtitle),
                        icon = R.drawable.ic_notifications,
                        iconTint = colors.warning,
                        trailing = {
                            PillButton(
                                text = stringResource(R.string.action_allow),
                                onClick = { onIntent(ReminderIntent.AllowNotifications) },
                                contentColor = colors.accentLight,
                                containerColor = colors.accentContainerStrong,
                            )
                        },
                    )
                }
            }

            SectionLabel(stringResource(R.string.section_daily).uppercase(), Modifier.padding(bottom = ObSpacing.S))
            ObCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(ObSpacing.L)) {
                ObListRow(
                    title = stringResource(R.string.reminder_title),
                    subtitle = stringResource(if (state.enabled) R.string.reminder_on else R.string.reminder_off),
                    icon = R.drawable.ic_notifications_active,
                    titleStyleLarge = true,
                    onClick = { onIntent(ReminderIntent.SetEnabled(!state.enabled)) },
                    trailing = {
                        ObSwitch(checked = state.enabled, onCheckedChange = { onIntent(ReminderIntent.SetEnabled(it)) })
                    },
                )
                ObDivider(Modifier.padding(vertical = ObSpacing.M))
                ObListRow(
                    title = stringResource(R.string.reminder_time),
                    subtitle = stringResource(R.string.reminder_time_subtitle),
                    icon = R.drawable.ic_schedule,
                    titleStyleLarge = true,
                    onClick = { onIntent(ReminderIntent.OpenTimePicker) },
                    trailing = {
                        RollingText(
                            text = state.time.formatted(),
                            style = ObsidianTheme.typography.numericSmall,
                            color = if (state.enabled) colors.accentLight else colors.textSecondary,
                        )
                    },
                )
            }

            SectionLabel(
                stringResource(R.string.section_check).uppercase(),
                Modifier.padding(top = ObSpacing.XL, bottom = ObSpacing.S),
            )
            ObCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(ObSpacing.L)) {
                ObListRow(
                    title = stringResource(R.string.test_reminder),
                    subtitle = stringResource(R.string.test_reminder_subtitle),
                    icon = R.drawable.ic_fitness_center,
                    onClick = { onIntent(ReminderIntent.SendTestReminder) },
                )
            }
            Text(
                text = stringResource(R.string.reminder_footer),
                style = ObsidianTheme.typography.body,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = ObSpacing.L, start = ObSpacing.XS, end = ObSpacing.XS),
            )
        }
    }

    if (state.timeSheetOpen) {
        ReminderTimeSheet(
            initial = state.time,
            onConfirm = { onIntent(ReminderIntent.PickTime(it)) },
            onDismiss = { onIntent(ReminderIntent.DismissTimePicker) },
        )
    }
}

internal fun LocalTime.formatted(): String = format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
