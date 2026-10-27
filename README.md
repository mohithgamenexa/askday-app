# Askday (native)

One question a day, answered with your friends. Answers unlock once the whole group is in.

The app is now **native**: Kotlin with Jetpack Compose for Android, and Swift with SwiftUI for iOS. The iOS app is coming next and will go in `ios/`. Everything runs on the phone, with no server or accounts, and sample friends stand in for real people.

```
askday/
├── android/                    Android app (Kotlin + Jetpack Compose)
│   └── app/src/main/java/app/askday/
│       ├── data/               Saved data, the rules engine, sample content, badges
│       ├── notify/             Notifications scheduled on the phone
│       └── ui/                 Theme, icons, components and every screen
├── later-online/               Firebase backend for a future online version (not used now)
└── .github/workflows/          Builds the Android APK on GitHub
```

## Get the Android APK

### On GitHub (nothing to install)

Push this project to your GitHub repository. The **Build Android APK** workflow runs automatically whenever anything in `android/` changes.

When it shows a green tick in the **Actions** tab, open the run and download **askday-apk**. Inside is `app-release.apk`; install it on any Android phone (8.0 or newer).

The first build takes roughly 6–10 minutes. Later builds reuse a cache and are usually faster.

### On your computer

Open the `android` folder in **Android Studio** and press Run, or build from a terminal:

```bash
cd android
./gradlew assembleRelease
```

The APK is at `android/app/build/outputs/apk/release/app-release.apk`. It's signed with a test key, which is fine for sharing with friends. The Play Store needs your own signing key.

## What's in the app

- **Guest login** that lives on the phone, then a name and photo.
- **Groups** with 4 sample friends. The first question arrives right away, then one a day at 9 AM, 7 PM or 10 PM.
- **Live countdown** to unlock, and "streak at risk" warnings.
- **Answers unlock** when everyone's in, or 2 hours after the drop.
- **Streaks and streak freezes:** every 7 days of streak earns a freeze (up to 2). If someone misses a day, a freeze saves the streak.
- **8 badges** with full-screen celebrations: First answer, Early bird, Night owl, 7/30/100-day streaks, Crowd favorite and Mind reader.
- **Reactions** (love, haha, fire, wow), replies and group chat. The top answer gets "Most liked."
- **Who said it?** Guess which friend wrote each answer.
- **Weekly recap** in story style.
- **Light, Dark or Phone** theme (Profile › Appearance, or the moon/sun button on Today).
- **Notifications** for the daily drop, "Everyone's in," and a reminder 30 minutes before unlock.

**Demo shortcut:** on a question screen, long-press the group name to get the next question now instead of waiting until tomorrow.

**Logging out** erases everything on the phone, since guest accounts live only there.

## How the code fits together

- `data/Models.kt` is everything the app saves, as one JSON file on the phone.
- `data/Engine.kt` holds all the rules: daily drops, unlocking, streaks, freezes, badges and the sample friends. It's plain Kotlin with no Android code, so it can be tested on its own.
- `data/Store.kt` keeps the data in memory, saves every change, and lets the screens update automatically.
- `ui/` contains the screens. `Theme.kt` has both color sets, and `Icons.kt` draws the same line icons as the design.

## Size and speed

The release APK is built with code shrinking and resource shrinking, and has no JavaScript engine or cross-platform runtime. It should be a small fraction of the earlier Expo build.

## Going online later

`later-online/` has a complete Firebase backend (daily drops by time zone, unlocking, streaks, nudges, push notifications and security rules). Connecting it would mean replacing `Store.kt` and the engine calls with Firebase calls; the screens read from the same data and wouldn't need to change much.
