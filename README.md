# Askday

**One question. Everyone. Answer to see.**

Askday is a daily question app. Every day a question drops, you answer it, and only then do you see what your friends and groups said. Streaks are shared: they live or die together.

- **Design canvas:** https://claude.ai/artifact/ALsAxkbrZiCkEvJ6GXxoAN (every screen, in light and dark)
- **Android app:** `android/` (Kotlin + Jetpack Compose)
- **iOS app:** coming next (Swift + SwiftUI), in `ios/`
- **Backend for the online version:** `later-online/` (Firebase)

---

## Where things stand

| | Status |
|---|---|
| **Version 2: groups, streaks, freezes, badges, reactions, game, recap, themes** | Built in the Android app (works offline, with sample friends) |
| **Ocean color palette** (teal into blue, warm coral and sun accents) | Built in the Android app and the design |
| **Phone login, Firebase Analytics, Crashlytics, Remote Config, AdMob ads** | Built in the Android app (switches on once `google-services.json` is added) |
| **Version 3: onboarding, global moment, friends, Asks, Discover, Memories, Askday+** | Designed (14 screens on the canvas), not built yet |
| **Online version** (real accounts and real friends) | Backend written in `later-online/`, not connected yet |
| **iOS app** | Not started |

Version 3 needs the online version. Friends, feeds, Discover and big groups only make sense with real accounts on a server.

---

## Features

### In the Android app now (version 2)

- **Phone number login** with an SMS code (Firebase Authentication), then a name and profile photo.
- **Groups** with 4 sample friends. The first question arrives right away, then one a day at 9 AM, 7 PM or 10 PM.
- **Answer to unlock:** you see others' answers only after answering, once everyone's in or 2 hours after the drop.
- **Live countdown** to unlock, and "streak at risk" warnings.
- **Shared streaks:** the group's streak grows when everyone answers.
- **Streak freezes:** every 7 days of streak earns one, up to 2. If someone misses a day, a freeze saves the streak automatically.
- **8 badges** with full-screen celebrations: First answer, Early bird, Night owl, One week strong (7 days), Thirty days, Century club (100 days), Crowd favorite and Mind reader.
- **Reactions:** love, haha, fire and wow. The most-reacted answer gets a "Most liked" badge.
- **Replies and group chat** under each day's answers.
- **Nudges:** remind a friend who hasn't answered.
- **Who said it?** A mini-game: guess which friend wrote each answer.
- **Weekly recap:** a story-style summary of the group's week.
- **History** of every question, with the top answer.
- **Light, Dark or Phone theme** (Profile › Appearance, or the moon/sun button on Today).
- **Notifications** scheduled on the phone: the daily drop, "Everyone's in," and a reminder 30 minutes before unlock.
- **Demo shortcut:** long-press the group name on a question screen to get the next question immediately.

### Designed for version 3

**Onboarding, inspired by BeReal**
1. **Hook:** today's live global question, a countdown, and blurred answers from around the world.
2. **Answer first:** write your first answer before creating an account. Text, photo or voice.
3. **Make it yours:** phone number with a verification code, name, username, and birthday (13+ age check; the birthday is never shown).
4. **Find friends:** contacts sync, your QR code, an invite link, and friend suggestions.
5. **Notifications and vibe:** a clear reason for the notification prompt ("One ping a day. That's it."), then choose question packs.
6. **Reveal:** land on today's answers, with your streak already started.

**The Askday moment.** One global question for everyone at the same moment, at a different time each day, with a window to answer "on time." Late answers are allowed but marked late.

**Friends.** Usernames, friend requests, contacts and suggestions. A daily feed of friends' answers to the global question (photo answers included), with reactions, comments, and a share button that turns an answer into a story card.

**Asks: member questions in groups.**
- Any member can post an Ask (1 a day free, 5 with Askday+).
- Answer types: text, poll, this or that, a 1–10 slider, pick a member, and photo (Askday+).
- Asks appear as a swipeable card stack (answer or skip), and results unlock after you answer.
- Asks expire after 24 hours.
- The asker is told "23 people answered your Ask." Notifications come as a digest, never one per Ask.
- The top Ask can be promoted to become tomorrow's group question, credited to its author. The "Question master" badge comes from this.
- Admins can set limits, pause Asks or approve them, and anyone can report one.

**Bigger groups.** Up to 50 members (200 with Askday+). In big groups the streak survives if 80% answer, so one inactive member can't break it for everyone. Group leaderboards, and a rotating "Question master."

**Discover.** The day's most-loved public answers worldwide (opt-in), trending Asks, and communities such as a college, a city or a shared interest.

**Memories.** A calendar of every answer you've given, "1 year ago today" throwbacks, and a shareable Year in Askday every December.

**Askday+.** The subscription, described under Revenue below.

**Safety.** Report, block, a word filter, moderation for Discover, private-by-default answers, and extra protections for under-18s.

---

## Login, Firebase and ads

### Phone number login (the only login)

People sign in with their mobile number and a 6-digit code. **Firebase Authentication sends the SMS**, and on many Android phones the code fills in by itself. "Send a new code" unlocks after 30 seconds. The first sign-in then asks for a name and photo.

For now, groups and answers are still saved on the phone. Phone login identifies the person, ready for the online version. Signing in with a different number on the same phone clears the previous person's data, and so does logging out.

### Set up Firebase (once)

1. Create a project at console.firebase.google.com.
2. **Add an Android app** with the package name `app.askday.android`.
3. **Add both fingerprints** of the app's signing key (Project settings › Your apps › Add fingerprint):
   - SHA-1: `AB:7C:C9:53:7D:75:B4:12:E0:0C:A5:49:17:9C:21:14:68:C0:99:A3`
   - SHA-256: `D5:E2:6A:C1:C7:FE:04:BB:4A:43:E9:67:C5:27:37:EC:C4:2D:13:45:92:B0:B5:BC:22:63:F7:F2:EE:BE:87:0C`
4. **Download `google-services.json`** and put it in `android/app/`. Firebase switches on automatically at the next build. Without the file the app still builds, and phone login shows a "needs Firebase" message.
5. **Authentication › Sign-in method:** enable **Phone**.
6. **Upgrade to the Blaze plan.** Phone verification is charged per SMS.
7. **Protect against SMS fraud:** under Authentication › Settings › SMS region policy, allow only the countries you serve (for example India). Turn on **App Check** when you can.
8. **Test without real SMS:** add test numbers under Authentication › Sign-in method › Phone, for example `+91 99999 00000` with code `123456`.

The app is signed with a fixed test key, `android/app/askday-release.jks` (password `askday-test-key`), so these fingerprints stay the same on every build. That key is fine for testing and sharing. **For the Play Store, create your own key, keep it private, and add its fingerprints to Firebase too.**

### Firebase Analytics

These events are logged automatically: `login`, `sign_up`, `group_created`, `answer_submitted`, `reaction`, `reply_posted`, `chat_posted`, `nudge_sent`, `game_completed`, `recap_viewed`, `theme_changed`, `logout` and `account_deleted`. Analytics is linked to the account ID, never to the phone number.

### Crashlytics

Crashes are reported automatically once Firebase is set up, along with unexpected errors the app catches. Reports are linked to the account ID.

### Remote Config: switching ads on and off

In Firebase › Remote Config, create the parameter **`ads_enabled`** (Boolean).

- `false` (the default, also used before Firebase is set up): no ads anywhere.
- `true`: ads appear for everyone, without a new app release. The app checks for changes about once an hour.

### Google Mobile Ads (AdMob)

When `ads_enabled` is on, a banner labeled "Sponsored" appears below the groups on **Today** and at the end of **History**. Google's consent form is shown first in regions that require it, such as Europe and the UK.

The app ships with **Google's official test IDs**, which show sample ads safely. To show real ads:

1. Create an app and a banner ad unit at admob.google.com.
2. Put your **app ID** in `android/gradle.properties`:
   ```
   askday.admobAppId=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
   ```
3. Put your **banner ad unit ID** in `android/app/src/main/java/app/askday/services/Services.kt` (`AdIds.BANNER`).

Never tap your own real ads; AdMob can suspend accounts for it. Keep the test IDs while developing.

## Design

All screens are on the design canvas, each with a light/dark switch in its Tweaks panel.

- **Palette:** ocean teal into blue (`#0A8A80` → `#0E7FA8` → `#2563EB`) for the main moments, a warm sun gradient (`#FFB020` → `#FF7A59`) for rewards and Askday+, coral for "at risk," and orange for the streak flame.
- **Light theme:** a soft mint-white background (`#F5FAF9`) with white cards and near-black buttons.
- **Dark theme:** deep sea green-black (`#061214`) with light buttons.
- **Type:** Bricolage Grotesque for headlines, DM Sans for everything else.
- **Tab bar:** the active tab is a teal gradient pill with its name, and every tab responds when pressed.

---

## Revenue model

The plan is to grow first and earn later. Monetization switches on once people come back day after day.

1. **Askday+ subscription (main revenue).** For example ₹99 a month or ₹799 a year in India (about $3.99 a month elsewhere), with a 7-day free trial. It includes unlimited streak freezes and streak repair, 5 Asks a day and photo Asks, groups of up to 200, full Memories and Year in Askday, and themes, app icons and profile flair. Google Play and the App Store keep 15% of subscription revenue for most small developers.
2. **One-time purchases.** Streak repairs, freezes, question packs, and gifts for friends.
3. **Askday for Teams (B2B).** A daily icebreaker for companies and remote teams, paid per employee per month, with Slack and Microsoft Teams integrations.
4. **Sponsored questions (at scale).** A clearly labeled global question from a brand, used sparingly.
5. **Printed memories.** A yearly book of a family group's answers, sold as a gift.

**Rough illustration:** 50,000 monthly users with 3% subscribing at ₹99 a month is about ₹1.5 lakh a month before store fees. Real results depend on retention, so measure that first.

---

## Roadmap

1. **Go online:** accounts, friends, the global Askday moment, the friends feed and invite links, built on `later-online/`.
2. **Build version 3 onboarding and Asks.**
3. **Prove retention:** launch to a few colleges or friend circles and track day-1, day-7 and day-30 return rates before charging anything.
4. **Growth loops:** share cards, home screen widget, contacts sync and Memories.
5. **Launch Askday+.**
6. **Launch Askday for Teams.**
7. **iOS app** in Swift and SwiftUI, with the same design and engine.

---

## Project layout

```
askday/
├── android/                      Android app (Kotlin + Jetpack Compose)
│   ├── app/src/main/java/app/askday/
│   │   ├── data/                 Models, the rules engine, store, sample content, badges, photos
│   │   ├── notify/               Notifications scheduled on the phone
│   │   ├── services/             Phone login, analytics, crash reports, remote config, ads
│   │   ├── ui/                   Theme, icons, components and every screen
│   │   └── MainActivity.kt
│   ├── app/src/main/res/         Fonts, launcher and notification icons, theme
│   └── licenses/                 Font licenses (SIL Open Font License)
├── later-online/                 Firebase backend for the online version
└── .github/workflows/            Builds the Android APK on GitHub
```

**How the Android code fits together**
- `data/Models.kt` is everything the app saves, as one JSON file on the phone.
- `data/Engine.kt` holds all the rules: daily drops, unlocking, streaks, freezes, badges, nudges, reactions and the sample friends. It's plain Kotlin with no Android code, and it has been compiled and tested on its own (20 checks, including a simulated 10 days with missed days).
- `data/Store.kt` keeps the data in memory, saves every change, and lets the screens update automatically.
- `ui/` contains the screens. `Theme.kt` has both color sets, and `Icons.kt` draws the design's line icons.

---

## Building the Android APK

### On GitHub (nothing to install)

Push this project to your repository. The **Build Android APK** workflow runs whenever anything in `android/` changes. When it shows a green tick in the **Actions** tab, open the run and download **askday-apk**. Inside is `app-release.apk`, which installs on Android 8.0 or newer.

If you're replacing the earlier Expo project in the same repository, remove its files first:

```bash
git rm -r -q app .github/workflows/build-apk.yml
cp -R ~/Downloads/askday/. .
git add -A && git commit -m "Switch to native Android" && git push
```

### On your computer

Open the `android` folder in Android Studio and press Run, or:

```bash
cd android
./gradlew assembleRelease
```

The APK lands in `android/app/build/outputs/apk/release/app-release.apk`. It's signed with a test key, which is fine for sharing with friends. Publishing on the Play Store needs your own signing key.

**Note:** the Android screens haven't been compiled yet, because the environment they were written in can't reach Google's Android libraries. The first build may report a few errors to fix.
