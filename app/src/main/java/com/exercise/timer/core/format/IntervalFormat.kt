package com.exercise.timer.core.format

import android.content.Context
import com.exercise.timer.R

/** "10 seconds", "2 minutes", "1 min 30 sec": how an interval reads in sentences and the notification. */
fun formatInterval(context: Context, seconds: Int): String {
    val res = context.resources
    val minutes = seconds / 60
    val rest = seconds % 60
    return when {
        minutes == 0 -> res.getQuantityString(R.plurals.seconds, rest, rest)
        rest == 0 -> res.getQuantityString(R.plurals.minutes, minutes, minutes)
        else -> context.getString(R.string.interval_min_sec, minutes, rest)
    }
}

/** The big countdown digits: "9" under a minute, "1:05" from a minute up. */
fun formatCountdown(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes == 0L) seconds.toString() else "%d:%02d".format(minutes, seconds)
}
