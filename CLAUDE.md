# CLAUDE.md

Guidance for Claude Code (and anyone else) working on this repository.

## What this is

Exercise Timer: a small Android app made for one user, an older family member (Dad). It does two things:

- **Repeating interval timer.** Pick 10 s, 20 s, 30 s, 1 min or a custom time (up to 99:59). Press Start
  and it counts down, beeps and vibrates, then starts again, until Stop. An optional **sleep timer** ends
  the session after 1 hour, because he sometimes falls asleep while exercising. It keeps running with the
  screen off and shows an ongoing notification with a Stop button.
- **Daily reminder.** One time a day with an on/off switch. It fires as a notification with a soft chime
  and survives reboots and time changes.

Keep the UI simple and large: big countdown, one primary Start/Stop button that's always visible, and
plain wording.

## Commands

```bash
./gradlew :app:assembleDebug             # debug APK
./gradlew :app:testDebugUnitTest         # unit tests (domain math, timer engine, reminder scheduling)
./gradlew :app:verifyDesignRules         # fails on code that bypasses Obsidian (also runs in `check`)
./gradlew :app:installDebug              # install on the connected phone
./gradlew :app:assembleRelease           # R8-shrunk, release-signed APK -> app/build/outputs/apk/release/
./gradlew :app:releaseApk -PversionCode=2 -PversionName=1.1   # release APK copied to the root for sharing
```

adb lives in `~/Library/Android/sdk/platform-tools` (not on PATH by default).

## Architecture: MVI + Clean Architecture, single module

Package `com.exercise.timer`:

| Package | What |
|---|---|
| `domain/model` | Pure Kotlin: `TimerConfig`, `TimerStatus`, `IntervalProgress` (countdown math), `ReminderSettings` (next-trigger math) |
| `domain/repository`, `domain/reminder` | Interfaces only (`TimerRepository`, `ReminderScheduler`, `ReminderNotifier`, …) |
| `domain/usecase` | One small use case per action; ViewModels only talk to these |
| `data` | Implementations: `IntervalTimerEngine` (the timer), DataStore repositories, `AlarmReminderScheduler`, `ToneVibrationAlert` |
| `framework` | Android components: `TimerService` (foreground service), `Notifications`, reminder/boot receivers |
| `presentation` | MVI screens: `*Contract.kt` (State / Intent / Effect), `*ViewModel.kt`, `*Screen.kt` |
| `core/mvi` | `MviViewModel` base: `state: StateFlow`, `onIntent()`, one-off `effects` |
| `di` | Hilt modules; `@ApplicationScope` is a main-thread scope |

MVI rules: a screen collects `state`, sends `Intent`s through `onIntent`, and handles each `Effect` once
(snackbars, permission prompts, opening Settings). Composables stay stateless (`TimerContent`,
`ReminderContent`); platform calls such as permission checks happen in the `*Screen` wrapper and come back
in as intents.

### Timer engine invariants (`data/timer/IntervalTimerEngine.kt`)

- Progress is computed from the start time on the monotonic clock (`SystemClock.elapsedRealtime`), never by
  counting ticks, so the timer doesn't drift. If several intervals pass during one late tick, it beeps once.
- It's confined to the main thread (`@ApplicationScope`), so a cancelled session can never publish over a
  new one. Don't move it to `Dispatchers.Default`.
- It sets `Running` before starting `TimerService`; the service stops itself as soon as it sees `Idle`.
- `TimerService` holds a partial wake lock (12 h safety cap) so beeps keep coming with the screen off.
- The engine is tested with virtual time in `IntervalTimerEngineTest`. Run it after any change here.

### Interval signal (`data/timer/ToneVibrationAlert.kt`, `SoftBeep.kt`)

- One soft 660 Hz sine beep, synthesised in pure Kotlin and tested in `SoftBeepTest`. Keep it a single beep:
  repeated beeps were found harsh.
- The beep uses `USAGE_MEDIA` on purpose so the volume buttons control it. The vibration stays `USAGE_ALARM`
  because Samsung mutes media vibration in silent mode; it's the only signal left when the volume is down.

## Design system: Obsidian

The UI uses [Obsidian](https://github.com/Redfox-OSS/obsidian) (`dev.redfox:obsidian`), accent `Emerald`,
neutrals `Dark`. Its `docs/GUIDE.md` is the reference.

- Colours and type come from `ObsidianTheme.colors` / `ObsidianTheme.typography`, never `MaterialTheme`.
- Use `Modifier.pressable { }`, never `clickable` or ripples. No hard-coded `Color(0x…)`.
  `verifyDesignRules` enforces all three.
- Prefer Obsidian components (`TabPage`, `ObCard`, `ObListRow`, `ChoicePill`, `PrimaryButton`, `ObSheet`, …).
  The one exception is Material 3's `TimePicker` in `ReminderTimeSheet`, pinned to Obsidian colours,
  because Obsidian has no clock picker.
- Anything interactive animates on an `ObMotion` spring.

### Toolchain is pinned to Obsidian's

AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.02.01, compile SDK 36.1, min SDK 31 (the blur needs Android 12).
Don't upgrade these on their own. KSP must be 2.3.x: 2.2.10-2.0.2 fails with AGP 9's built-in Kotlin.

## Notifications and Live Updates

- **A channel's settings can't change after it's created.** To change sound or importance, create a new
  channel ID and add the old one to `OLD_CHANNELS` so it's deleted. That's why the current IDs are
  `timer_live` and `reminder_chime`.
- **Live Update (promoted notification) recipe**, matching the user's Alarmzy app: an IMPORTANCE_HIGH
  channel with no sound, `setRequestPromotedOngoing(true)`, a `ProgressStyle`, a title, ongoing, not
  colorized, public visibility. Don't call `setSilent(true)`: Samsung files silent notifications in a
  separate section.
- **Samsung One UI 9 (Android 17) doesn't show third-party Live Updates in the Now Bar.** Android marks the
  notification `PROMOTED_ONGOING`, but Samsung's SystemUI keeps `isPromotedState=false` and appears to
  check a package allowlist. It worked on One UI 8.5. Don't spend more time on app-side fixes for this.
  Samsung's private `android.ongoingActivityNoti.*` extras were tried and also end up hidden.
- The Timer screen shows a "Show on lock screen" card when `canPostPromotedNotifications()` is false.
- The reminder uses `USE_EXACT_ALARM` (fine for sideloading; Google Play only allows it for alarm-type apps).

## Testing on a real phone

The development phone is a Samsung S23 (One UI 9, Android 17), connected over USB.

- Install with `./gradlew :app:installDebug`, then check with screenshots (`adb exec-out screencap -p`) and
  `dumpsys` (`notification --noredact`, `activity services com.exercise.timer`, `alarm`, `vibrator_manager`).
- The owner sometimes uses the phone during a test. Before treating unexpected taps or state as a bug,
  check `logcat` for `InputDispatcher` touches.
- The phone has a PIN. Never enter it; ask the owner to unlock.
- If adb restarts with "Address already in use", wait for the port to clear (TIME_WAIT) and retry.

## Release

- R8 is on for release (`isMinifyEnabled`, `isShrinkResources`). Libraries bring their own keep rules;
  `app/proguard-rules.pro` only keeps line numbers. The mapping file is in
  `app/build/outputs/mapping/release/`.
- Release signing reads the gitignored `keystore.properties` (+ `release.jks`) and falls back to the debug
  key when they're missing. Never commit either file. The same key must sign every version, or an update
  can't be installed over Dad's copy.
- APKs are gitignored; a shareable copy is placed at the project root as `ExerciseTimer-<version>.apk`.
- The version comes from the command line, not the build file:
  `./gradlew :app:releaseApk -PversionCode=<n> -PversionName=<x.y>` builds the signed release APK and
  copies it to the root as `ExerciseTimer-<x.y>.apk`. The defaults in `app/build.gradle.kts` (1 / 1.0) are
  for debug builds only; never edit them to ship.
- When asked for a release APK, reply with that command (next `versionCode` bumped, new `versionName`)
  instead of running the build yourself — the owner runs it.
