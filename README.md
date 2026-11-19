# DoneBuzz for Android

The Android version of [DoneBuzz](https://github.com/zxy981123/DoneBuzz), a gentle, game-like to-do list for people with ADHD. Chinese and English.

Fully native: Kotlin, Jetpack Compose, Room, DataStore and Glance.

## Features

- **To-do list with a pixel cat** that walks toward the finish flag as you tick things off, with a coin, confetti, a chime and a haptic pattern built from the phone's own haptic primitives.
- **Finished items stay gold for the day** and can be un-ticked. The **Done** tab groups days as 今天 / 昨天 / 前天 with the day's total time.
- **Forgiving streaks**: one rest day doesn't break it.
- **Focus timer**: tap Start on a task with a time estimate. The countdown is an **Android 16 Live Update** that shows as a chip in the status bar and on the lock screen. When the time is up you get a notification.
- **Home screen widget** (Glance): the cat, your streak, today's progress and, when wide, the next three tasks with a + that opens the add sheet.
- **Voice input**, on-device when the phone supports it.
- **Daily reminders** with a different gentle message each weekday.
- **What's New** sheet after updates. It's also in Settings.
- Android 12+ splash screen, edge-to-edge, predictive back and a themed (monochrome) icon.

## Build

Requires Android Studio (or JDK 21) and the Android SDK with platform 37.

```bash
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # unit tests
./gradlew installDebug         # install on a running emulator or device
```

## Releasing

When a release has news worth showing, bump `WhatsNew.CURRENT_RELEASE` in `logic/Small.kt` and update `Strings.whatsNewItems`.
