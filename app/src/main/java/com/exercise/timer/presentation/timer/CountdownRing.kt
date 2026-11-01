package com.exercise.timer.presentation.timer

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.exercise.timer.R
import com.exercise.timer.core.format.formatCountdown
import com.exercise.timer.core.format.formatInterval
import dev.redfox.obsidian.effects.breathingGlow
import dev.redfox.obsidian.motion.ObMotion
import dev.redfox.obsidian.motion.RollingText
import dev.redfox.obsidian.theme.ObSpacing
import dev.redfox.obsidian.theme.ObsidianTheme

/**
 * The hero: a ring that drains smoothly over each interval, with the seconds left in the middle.
 * The ring is redrawn every frame from the interval's end time (draw phase only, no recomposition);
 * the digits change once a second. It pulses on every beep.
 */
@Composable
fun CountdownRing(
    intervalSeconds: Int,
    session: TimerUiState.Session,
    modifier: Modifier = Modifier,
) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    val running = session as? TimerUiState.Session.Running

    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(running != null) {
        while (running != null) withFrameMillis { now = SystemClock.elapsedRealtime() }
    }

    val pulse = remember { Animatable(1f) }
    LaunchedEffect(running?.round) {
        if (running != null && running.round > 1) {
            pulse.snapTo(1.06f)
            pulse.animateTo(1f, ObMotion.bouncy())
        }
    }

    val digits = formatCountdown(running?.remainingSeconds ?: intervalSeconds.toLong())
    val shownSeconds = running?.remainingSeconds ?: intervalSeconds.toLong()
    val unit = if (shownSeconds < 60 && (running?.intervalSeconds ?: intervalSeconds) < 60) {
        stringResource(R.string.unit_seconds)
    } else {
        stringResource(R.string.unit_minutes)
    }
    val description = if (running != null) {
        stringResource(R.string.cd_countdown_running, running.remainingSeconds, running.round)
    } else {
        stringResource(R.string.cd_countdown_idle, formatInterval(LocalContext.current, intervalSeconds))
    }

    Box(
        modifier
            .size(244.dp)
            .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        // The disc glows only while a session is on.
        Box(
            Modifier
                .size(190.dp)
                .then(if (running != null) Modifier.breathingGlow(colors.accent, CircleShape) else Modifier)
                .clip(CircleShape)
                .background(colors.card)
        )
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = colors.tile,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke),
            )
            val fraction = if (running == null) {
                1f
            } else {
                val total = running.intervalSeconds * 1_000f
                ((running.roundEndsAt - now) / total).coerceIn(0f, 1f)
            }
            drawArc(
                color = colors.accent,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (running != null) {
                    stringResource(R.string.round_label, running.round).uppercase()
                } else {
                    stringResource(R.string.every_label).uppercase()
                },
                style = type.caption,
                color = if (running != null) colors.accentLight else colors.textSecondary,
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(ObSpacing.XS)) {
                RollingText(
                    text = digits,
                    style = type.numericLarge.copy(fontSize = 80.sp, lineHeight = 88.sp, textAlign = TextAlign.Center),
                    color = colors.textPrimary,
                    rollUp = if (running != null) false else null,
                )
            }
            Text(text = unit, style = type.unit, color = colors.textSecondary)
        }
    }
}
