# Askday (local demo version)

One question a day, answered with your friends. Answers unlock once the whole group is in.

This version runs **entirely on the phone**. There's no server, no Firebase and no accounts: everything is saved on the device. Sample friends join each group and answer, so you can try the full experience on your own.

```
askday/
├── app/            The app: Android, iOS and web from one codebase
│   ├── src/app/    Screens (each file is a screen)
│   ├── src/lib/    The on-device database, the rules engine, notifications
│   └── src/components/
└── later-online/   Firebase backend for the future online version (not used now)
```

## Try it in your browser (quickest)

You need **Node.js 22 or newer**.

```bash
cd app
npm install
npx expo start --web
```

## Get the Android APK without Android Studio

Both options build the APK in the cloud and give you a file to download. Neither needs Android Studio.

### Option A: GitHub (nothing to install)

1. Create a free account at github.com and click **New repository**. Give it any name, choose **Private**, and create it.
2. On the new repository's page, click **uploading an existing file**. Drag in everything inside the `askday` folder: `app`, `later-online`, `.github` and `README.md`. Then click **Commit changes**.
3. Open the **Actions** tab. A build called "Build Android APK" starts by itself and takes about 15–20 minutes.
4. When it shows a green tick, click it. Under **Artifacts**, download **askday-apk**. It's a zip containing `app-release.apk`.

Every time you upload changed files, a fresh APK is built automatically.

If the build doesn't start, the `.github` folder probably didn't upload: some computers hide folders whose names start with a dot. In that case click **Add file › Create new file**, type `.github/workflows/build-apk.yml` as the name, paste in the contents of that file from this project, and commit.

### Option B: Expo's cloud

You need **Node.js 22 or newer** on your computer and a free account at expo.dev.

```bash
cd app
npm install
npx eas-cli@latest login
npx eas-cli@latest build --platform android --profile preview
```

Answer **yes** when it offers to create the project and to generate a signing key. After 10–20 minutes it prints a download link for the APK. You can open that link straight on your Android phone.

## Build the Android APK on your computer

If you prefer to build it yourself, no accounts are needed.

1. Install **Android Studio** (free). Open it once and let it finish installing the Android SDK.
2. Make sure your terminal can find the SDK. On Windows, set an environment variable `ANDROID_HOME` to `C:\Users\<you>\AppData\Local\Android\Sdk`. On Mac, add this to your shell profile:
   ```bash
   export ANDROID_HOME=$HOME/Library/Android/sdk
   ```
3. Build:
   ```bash
   cd app
   npm install
   npx expo run:android --variant release
   ```
   The first build downloads Android build tools and takes 10–20 minutes. Later builds are much faster. If an Android phone is plugged in with USB debugging on (or an emulator is running), the app also installs on it automatically.
4. Your APK is at:
   ```
   app/android/app/build/outputs/apk/release/app-release.apk
   ```
   Copy it to any Android phone and open it to install. Android asks you to allow installing apps from that source the first time.

This APK is signed with a test key, which is fine for sharing with friends. The Play Store needs your own signing key; set that up when you publish.

## How the demo works

- **Guest login.** "Continue as guest" creates an account on this phone. Pick a name and photo.
- **Groups.** When you create a group, 4 sample friends join it (Priya, Sam, Rohan and others).
- **The daily question.** A new group gets its first question right away. After that, one drops each day at the time you chose: 9 AM, 7 PM or 10 PM.
- **Sample friends answer.** Some answer within a couple of minutes of the drop, and the rest answer over the next minute or two after you do. Their answers are written for each question, so the reveal makes sense.
- **Unlocking and streaks.** Answers unlock once everyone's in, or 2 hours after the drop. The streak grows when everyone answers and resets when someone misses a day.
- **Reactions.** After answers unlock, friends like your answer, reply to it, and sometimes write back in the group chat.
- **Nudges work.** Nudge a friend who hasn't answered and they'll answer within a few seconds.
- **Notifications** are scheduled on the phone itself: the daily drop, "Everyone's in," and a reminder 30 minutes before unlock if you haven't answered.

**Demo shortcut:** on a group's question screen, **long-press the group name** at the top to get the next question immediately instead of waiting until tomorrow. Useful for showing the app to people.

**Logging out** erases everything on that phone, since guest accounts live only there. The app warns you first.

### What needs the online version

Real friends on their own phones, joining by invite code or link, and keeping your account when you switch phones all need a server. In this version the invite card shows a code but sharing is turned off, and "Join with code" explains that it needs the online version.

The demo has 30 questions with written sample answers; groups cycle through them in a shuffled order.

## Going online later

`later-online/` contains a complete Firebase backend built earlier: daily drops by time zone, unlocking, streaks, nudges, push notifications and security rules that stop anyone from seeing answers before they've answered. Moving to it means replacing `app/src/lib/store.ts` and `app/src/lib/engine.ts` with Firebase calls; the screens use the hooks in `app/src/lib/data.ts` and won't need to change much.

## Useful commands

```bash
cd app
npx tsc --noEmit                         # check the code for errors
npx expo start --web                     # run in the browser
npx expo run:android --variant release   # build and install the APK
npx expo install --fix                   # fix package versions after upgrading Expo
```
