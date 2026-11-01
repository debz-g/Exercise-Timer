package com.exercise.timer.presentation.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.exercise.timer.R
import com.exercise.timer.presentation.reminder.ReminderScreen
import com.exercise.timer.presentation.timer.TimerScreen
import dev.chrisbanes.haze.rememberHazeState
import dev.redfox.obsidian.components.ObSnackbarHost
import dev.redfox.obsidian.components.ObSnackbarState
import dev.redfox.obsidian.effects.LocalHazeState
import dev.redfox.obsidian.motion.ObTransitions
import dev.redfox.obsidian.motion.RevealScope
import dev.redfox.obsidian.navigation.FloatingNavBar
import dev.redfox.obsidian.navigation.FloatingNavItem
import dev.redfox.obsidian.navigation.NavIconMotion
import dev.redfox.obsidian.navigation.floatingNavClearance
import dev.redfox.obsidian.theme.ObsidianAccent
import dev.redfox.obsidian.theme.ObsidianNeutrals
import dev.redfox.obsidian.theme.ObsidianTheme

/** Snackbars for the whole app, shown above the nav bar. */
val LocalSnackbars = staticCompositionLocalOf { ObSnackbarState() }

private enum class Tab(val icon: Int, val label: Int, val motion: NavIconMotion) {
    Timer(R.drawable.ic_timer, R.string.tab_timer, NavIconMotion.Pop),
    Reminder(R.drawable.ic_notifications, R.string.tab_reminder, NavIconMotion.Shake),
}

/** Two tabs, no pushed screens: the timer and the daily reminder. */
@Composable
fun ExerciseTimerApp() {
    ObsidianTheme(accent = ObsidianAccent.Emerald, neutrals = ObsidianNeutrals.Dark) {
        val hazeState = rememberHazeState()
        val snackbars = remember { ObSnackbarState() }
        var tab by rememberSaveable { mutableStateOf(Tab.Timer) }

        AskForNotificationsOnce()

        CompositionLocalProvider(LocalHazeState provides hazeState, LocalSnackbars provides snackbars) {
            Box(Modifier.fillMaxSize().background(ObsidianTheme.colors.background)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { ObTransitions.tab(forward = targetState.ordinal > initialState.ordinal) },
                    label = "tab",
                ) { current ->
                    RevealScope {
                        when (current) {
                            Tab.Timer -> TimerScreen()
                            Tab.Reminder -> ReminderScreen()
                        }
                    }
                }
                FloatingNavBar(
                    items = Tab.entries.map { FloatingNavItem(it.icon, stringResource(it.label), it.motion) },
                    selectedIndex = tab.ordinal,
                    onSelect = { tab = Tab.entries[it] },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                ObSnackbarHost(snackbars, Modifier.align(Alignment.BottomCenter), bottomPadding = floatingNavClearance())
            }
        }
    }
}

/** The timer's notification (with its Stop button) and the reminder both need this on Android 13+. */
@Composable
private fun AskForNotificationsOnce() {
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted && !asked) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
