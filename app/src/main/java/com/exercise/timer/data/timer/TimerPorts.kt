package com.exercise.timer.data.timer

/** Monotonic milliseconds (`SystemClock.elapsedRealtime` in the app; virtual time in tests). */
fun interface ElapsedClock {
    fun now(): Long
}

/** The end-of-interval signal: a beep and a buzz. */
fun interface IntervalAlert {
    fun play()
}

/** Keeps the process alive and visible (a foreground service) while a session runs. */
fun interface TimerKeepAlive {
    fun sessionStarted()
}
