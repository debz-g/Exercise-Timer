# Exercise Timer

A small Android app for one job: beep every few seconds so you keep moving, and remind you once a day to
do it. It was built for an older family member who exercises at home alone, so the UI is large, dark and
plain, and everything works with the screen off.

No account, no network permission, no analytics. The app has exactly two things to say: "beep" while the
timer runs, and "time to exercise" once a day.

## Features

**Repeating interval timer**

- Beep every 10 s, 20 s, 30 s or 1 min, or a custom interval from 1 s to 99:59.
- One soft beep plus a matching vibration on every interval. The beep plays on the media stream, so the
  phone's volume buttons set how loud it is; the buzz is tagged as an alarm, so it still fires when the
  volume is turned down.
- Keeps repeating until Stop is pressed. The countdown ring and the button stay on screen the whole time.
- Optional **sleep timer** that ends the session by itself after 1 hour, with the remaining minutes shown
  in the UI — for when you fall asleep halfway through.
- Runs in a foreground service with a partial wake lock, so intervals keep firing with the screen off.
  The ongoing notification shows the round and has a Stop button.
- Where the platform supports it, that notification is a promoted Live Update, so the countdown can be
  seen on the lock screen without opening the app.

**Daily reminder**

- One reminder per day with an on/off switch and a time picker. Defaults to off at 08:00.
- Fires as a notification with a soft chime. Exact when the system allows it, otherwise within a few
  minutes, and it is rescheduled after a reboot, a time or timezone change, and an app update.
- A "send a test reminder" row shows the notification immediately, so it can be checked without waiting
  for the time to come around.

## Screenshots

Coming soon: the Timer and Reminder tabs.

## Build and run

Requirements: JDK 17+, Android Studio with SDK 36.1, and a device or emulator running Android 12 (API 31)
or newer. The UI uses [Obsidian](https://github.com/Redfox-OSS/obsidian), which is resolved from its public
Maven repository, already declared in `settings.gradle.kts`.

```bash
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:installDebug         # install on the connected device
./gradlew :app:testDebugUnitTest    # unit tests: countdown math, timer engine, reminder scheduling
./gradlew :app:verifyDesignRules    # fails if UI bypasses Obsidian (also runs in ./gradlew check)
./gradlew :app:assembleRelease      # R8-shrunk release APK
./gradlew :app:releaseApk -PversionCode=2 -PversionName=1.1   # release APK + copy in the project root
```

`assembleRelease` and `releaseApk` sign with `keystore.properties` and `release.jks` in the project root.
Both are gitignored, so a fresh clone falls back to the debug key. Keep the same release key across
versions — Android will not install an update signed with a different one. The version is passed on the
command line; the defaults in `app/build.gradle.kts` (1 / 1.0) are only for debug builds.

## How it is built

Single module, Kotlin and Jetpack Compose, hybrid MVI + Clean Architecture.

| Layer | Package | Contents |
|---|---|---|
| Domain | `domain/model`, `domain/usecase` | Pure Kotlin countdown and next-trigger math, one small use case per action |
| Data | `data` | `IntervalTimerEngine`, DataStore preferences, `AlarmReminderScheduler`, beep and vibration |
| Framework | `framework` | Foreground service, notifications, reminder and boot receivers |
| Presentation | `presentation` | One contract, ViewModel and screen per tab, with stateless content composables |

The timer does not count ticks: it computes progress from `SystemClock.elapsedRealtime` at start, so it
cannot drift, and a late tick can never produce a burst of beeps. It is confined to the main thread so a
cancelled session cannot publish over a new one. Those invariants are covered by
`IntervalTimerEngineTest` with virtual time.

## Privacy

The manifest requests no `INTERNET` permission. Settings and the reminder live in DataStore on the
device; there is no telemetry, crash reporting, advertising or third-party SDK beyond AndroidX, Hilt and
Obsidian.

## License

[GPL-3.0](LICENSE).
