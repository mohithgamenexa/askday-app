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
| **Phase A: new onboarding, global Askday moment, Friends feed, Asks, Memories, Askday+ screen, Discover preview, 5-tab bar** | Built in the Android app (with sample friends) |
| **Extras: question packs, question master, Ask of the day → tomorrow’s question, big groups (80% rule), leaderboards, photo and voice answers, share cards, home screen widget, Question master badge** | Built in the Android app |
| **Wave 1: photo of the day, mood, daily challenge, battles, Draw it, Guess the group, group trivia** | Built in the Android app |
| **Waves 2–4: friendships and memories; progress and expression; smart features and keepsakes** | Built in the Android app |
| **Phase B: real friends across phones, contact matching, live Discover, payments** | Needs the online version |
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

### Also in the Android app

- **Question packs:** the packs you pick (Funny, Deep talk, Throwback and more) decide which bank questions your groups get. Change them in Profile.
- **Question master:** each day one member writes tomorrow’s group question. On your turn, a card appears under the day’s answers.
- **Ask of the day → tomorrow’s question:** if your Ask is the group’s most answered one, it becomes the next group question, credited to you.
- **Group sizes:** small (5), medium (9) or big (13). Groups of 10 or more keep their streak when 80% answer.
- **Leaderboards** in each group: most consistent, funniest (most "haha" reactions) and early bird, over the last 30 days.
- **Photo and voice answers** for the Askday moment and group questions (voice notes up to 30 seconds).
- **Share cards:** turn any answer into a story-sized image for Instagram or WhatsApp.
- **Home screen widget:** today’s Askday question and whether you’ve answered. Long-press your home screen › Widgets › Askday.
- **Question master badge:** 10 of your Asks answered by the whole group.
- **Starter groups:** after onboarding everyone joins *Askday Lounge* (13 members) and *Late Night Talks* (5), labeled as starter groups with sample members, so the app is never empty.
- **Quick questions:** five global questions a day in Discover (and a card on Today). Answer one to see what people around the world said.
- **Daily rituals (Today › Daily):**
  - *Photo of the day:* a daily photo prompt. Take the photo with the back camera, then add a selfie, BeReal-style.
  - *Mood check-in:* one tap, with a bar showing how your friends feel today.
  - *Today's challenge:* a small daily challenge with its own streak.
  - *Today's battle:* a global this-or-that vote, with results revealed at 9 PM.
  - *Draw it:* a daily doodle prompt, drawn with your finger and saved as an image.
- **Wave 2: friendships and memories**
  - *Compliment polls (group › Play):* a positive, anonymous daily poll ("Who has the best laugh?"). See how many picked you, and reveal one hint at a time.
  - *Duos (Friends tab):* a daily question with just one friend, with its own streak.
  - *Friendship insights:* tap a friend on the Friends tab to see your friendship score, polls you agreed on, and reactions both ways.
  - *Thank-you notes:* send a note from a friend's page.
  - *Birthday mode:* on a member's birthday, the group's question is about them, and a card in the group shares a birthday card.
  - *Group albums (group › Play › Group album):* every photo from answers, plans and events.
  - *Time capsule (Me):* a message or voice note that opens in 1 month, 6 months or a year, with a notification.
  - *Year in Askday (Me):* a story-style recap of your year.
- **Wave 3: progress, expression and meeting up**
  - *Levels and XP (Me):* points for everything you do, 100 per level.
  - *Seasons:* a monthly theme with a featured pack, on Today and Me.
  - *Group goals (group › Board):* answer together to hit the month's target.
  - *Plans (group › Play):* propose a plan with times; friends vote; add meetup photos after.
  - *Event mode (group › Play):* a one-day event that collects everyone's photos.
  - *Music answers:* answer a group question with a song.
  - *Profile vibes:* a "currently…" status on Me.
  - *Custom reactions (group › Play):* up to 3 of a group's own reactions under every answer.
  - *Voice reactions:* a 3-second voice reaction under any group answer.
- **Wave 4: smart features and keepsakes**
  - *Weekly insights (Me › Insights):* your moods, most-used word and top reactor this week, worked out on the phone.
  - *Smarter questions:* packs your group reacts to most are picked first.
  - *Ask helper:* suggestions while you write an Ask.
  - *Make a book (Me):* exports your answers and photos as a PDF to share or print.
- **Guess the group:** when voting on a poll Ask, guess the group's top pick. Right guesses score on the group's *Play* tab ("Knows the group best").
- **Group trivia (group › Play):** questions about what your friends answered on earlier days.
- **5-minute moment:** the Askday moment must be answered within 5 minutes of the drop to count as on time. Later answers are allowed and marked late.
- **Catch up on the last 7 days:** pick an earlier day on the Friends tab to answer past moments, or tap a past group question in History. Late answers don't change streaks.
- **Contacts:** in onboarding and from Friends › Invite contacts. See and search your contacts and invite anyone by SMS or WhatsApp. Contacts are read on the phone only and never uploaded. Seeing which contacts already use Askday, and who is online, comes with the online version.


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

### Phone number or guest login

People sign in with their mobile number and a 6-digit code, or tap **Continue as guest** (on the first screen or the phone screen). Guests use Firebase anonymous sign-in: a real account ID with no phone number. **Turn on Anonymous** in Firebase › Authentication › Sign-in method for guest login to work. **Firebase Authentication sends the SMS**, and on many Android phones the code fills in by itself. "Send a new code" unlocks after 30 seconds. The first sign-in then asks for a name and photo.

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

Ads only appear when the Remote Config switch **`ads_enabled`** is on, and Google's consent form is shown first where the law requires it (for example Europe and the UK). Each format also has its own switch, all on by default:

| Format | Switch | Where it appears |
|---|---|---|
| Banner | `ads_banner` | Bottom of Today, History and Discover |
| Native | `ads_native` | End of the Friends feed (once there are 3+ answers), and in Discover between sections, styled as a card labeled "Sponsored" |
| Interstitial | `ads_interstitial` | Only at natural breaks: after a Who said it? result, after group trivia, and after closing the weekly recap or Year in Askday. Never while answering or during onboarding. |
| Rewarded | `ads_rewarded` | "Watch an ad for 1 more Ask" when today's Asks run out, and "Earn a streak freeze" in practice groups (once a day per group, up to 2 saved) |

Interstitials are capped by `interstitial_cooldown_minutes` (default 3) and `interstitial_daily_cap` (default 3). Change any of these in Remote Config without an app update.

The app uses **Google's official test IDs**, which always show safe sample ads:

| Format | Test ID |
|---|---|
| App ID | `ca-app-pub-3940256099942544~3347511713` |
| Banner | `ca-app-pub-3940256099942544/6300978111` |
| Interstitial | `ca-app-pub-3940256099942544/1033173712` |
| Rewarded | `ca-app-pub-3940256099942544/5224354917` |
| Native | `ca-app-pub-3940256099942544/2247696110` |

**To switch to real ads later:**
1. In admob.google.com, create the app and one ad unit of each type.
2. Put your **app ID** in `android/gradle.properties` as `askday.admobAppId=ca-app-pub-…~…`.
3. Put your four **ad unit IDs** in `android/app/src/main/java/app/askday/services/Ads.kt` (`AdUnits`).

Never tap your own real ads; AdMob can suspend accounts for it. Keep the test IDs while developing.

Rewarded streak freezes are only offered in practice groups. In real groups, freezes are kept by the server, and rewarding them needs AdMob's server-side verification, which can be added later.

## Designed for version 3

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

### Phone number or guest login

People sign in with their mobile number and a 6-digit code, or tap **Continue as guest** (on the first screen or the phone screen). Guests use Firebase anonymous sign-in: a real account ID with no phone number. **Turn on Anonymous** in Firebase › Authentication › Sign-in method for guest login to work. **Firebase Authentication sends the SMS**, and on many Android phones the code fills in by itself. "Send a new code" unlocks after 30 seconds. The first sign-in then asks for a name and photo.

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

---

## Phase B: the online version

Phase B moves Askday onto your Firebase project (`ask-day`) so real people share groups, answers and friends across phones. The phone keeps its own copy for speed and offline use and syncs with the server.

### Decisions
- **Sample friends** appear only in the starter rooms, labeled "Sample", and only until a room has real activity that day.
- **Starter groups** become real public rooms everyone joins (*Askday Lounge*, *Late Night Talks*). Public rooms have no shared streak; answers unlock once you answer, and the most loved show first.
- **Discover** shows public answers only when the author turns on *Share publicly* for that answer (off by default).

### Stages
| Stage | What it does | Status |
|---|---|---|
| **B1 · Accounts** | Profiles on the server, restore on a new phone, unique usernames, guests can add a phone number, delete removes server data | **Built** |
| **B2 · Friends** | Friend requests, username search, contacts on Askday (hashed numbers), online status with a privacy switch | **Built** |
| **B3 · Real groups** | Invite links, real members, server-side drops, unlock, streaks, freezes, reactions, replies, nudges | **Built** |
| B4 · Global | Real Askday moment, Friends feed, Quick questions, battle results, public rooms, Discover | Next |
| B5 · Everything shared | Asks, compliment polls, Duos, plans, events, albums, photo of the day | Planned |
| B6 · Push and safety | Push notifications, report and block, word filter, moderation, AI insights | Planned |

### What's on the server (B1)
- `users/{uid}`: the public profile (name, username, photo, status, level). Anyone signed in can read it; only you can write it.
- `users/{uid}/private/me`: phone number, birthday, packs and settings. Only you can read or write it.
- `usernames/{name}`: makes usernames unique.
- Storage `avatars/{uid}.jpg`: profile photos (images under 5 MB).

The rules are in `server/firestore.rules` and `server/storage.rules`.

### Set up (once)
1. **Firestore:** Firebase console › Build › Firestore Database › Create database › *Production mode* › location **asia-south1 (Mumbai)**.
2. **Storage:** Build › Storage › Get started (same location).
3. **Let GitHub deploy the server:** Project settings › Service accounts › **Generate new private key** (downloads a JSON file). Then in GitHub: repository › Settings › Secrets and variables › Actions › **New repository secret**, name it `FIREBASE_SERVICE_ACCOUNT`, and paste the whole file's contents.
4. Push the `server/` folder. The **Deploy server** workflow publishes the rules. If it reports a permissions error, give that service account the **Firebase Rules Admin** role in Google Cloud › IAM.

Keep the service account file private, and never commit it to the repository.

### What's on the server (B2)
- `friendRequests/{from_to}`: pending requests. Only the two people involved can see one.
- `users/{uid}/friends/{fid}`: accepted friends, written for both people when a request is accepted.
- `phoneHashes/{hash}`: hashed phone numbers of signed-in accounts. Only Cloud Functions can read or write this.
- Realtime Database `status/{uid}`: online, offline or hidden, plus when you were last seen.
- Cloud Functions (in `server/functions`, region asia-south1):
  - `registerPhone` registers your own verified number (from your sign-in), so friends can find you.
  - `matchContacts` takes hashed contact numbers and returns which belong to Askday accounts.
  - `unregisterPhone` removes your number from matching when you delete your account.

Contact numbers are hashed on the phone as SHA-256 of `askday-contacts-v1:` plus the number in international format (for example `+919876543210`). Raw numbers never leave the phone.

### Set up for B2 (once)
1. **Realtime Database:** Firebase console › Build › Realtime Database › Create database › location **asia-southeast1 (Singapore)** › *Start in locked mode*.
2. **Download `google-services.json` again** (Project settings › Your apps › Android app) and replace the one in `android/app/`. The new file includes your Realtime Database address. Without it, online status stays off, but everything else works.
3. Push `server/`. The **Deploy server** workflow now also deploys the Cloud Functions and the Realtime Database rules. The first Functions deploy can take a few minutes, because Google turns on the Cloud Functions, Cloud Build and Artifact Registry services for the project.
4. If the deploy reports a permissions error, open Google Cloud console › IAM, find the `firebase-adminsdk` service account, and add the roles **Cloud Functions Admin**, **Service Account User** and **Artifact Registry Administrator**.

### What's on the server (B3)
- `groups/{gid}`: a real group (name, members, invite code, drop time, day, streak, freezes, next drop). Only members can read it; only Cloud Functions write it.
- `groups/{gid}/questions/{qid}`: each day's question, who's answered, unlocked or not. Members can read; only Cloud Functions write.
- `groups/{gid}/questions/{qid}/answers/{uid}`: your answer, with an optional photo and voice note. You can always read yours; everyone else's only after you've answered and the question has unlocked. You can only change your own reaction.
- `…/comments/{cid}`: replies and group chat, after unlock.
- `invites/{code}`: invite code → group (Cloud Functions only).
- Storage `answers/{gid}/{qid}/{uid}.jpg|.m4a`: photo and voice answers, visible to group members only.
- Cloud Functions:
  - `createGroup`, `joinGroup`, `leaveGroup`.
  - `onAnswer` counts answers, unlocks and grows the streak (a freeze every 7 days, up to 2).
  - `clock` runs every 10 minutes: it drops due questions at 9 AM, 7 PM or 10 PM India time and unlocks questions after 2 hours. Before each drop it settles the previous day: a saved freeze protects the streak, otherwise it resets.
  - `nudge` and `setNextQuestion` (the question master).
- Hosting `ask-day.web.app`: `/j/CODE` invite pages, plus `/.well-known/assetlinks.json` so Android opens invite links straight in the app.

### In the app (B3)
- **Groups › New group › Real group** creates a group on the server with an invite link to share (WhatsApp, Instagram, Messages, Copy). **Practice group** works as before, with sample friends.
- **Join with code**, or tap an invite link. If you're not signed in yet, Askday joins you right after sign-in.
- Real groups appear in the same screens as before, with the same look. Answering, reactions, replies, nudges and the question master all go through the server.

### Set up for B3 (once)
1. Upload `server/`. **Deploy server** now also publishes the website (Hosting) and the database index the clock needs. The index can take a few minutes to build after the first deploy.
2. **Storage rules read Firestore** (to check group membership). If Storage › Rules shows a banner asking for permission to read Firestore, click to allow it.
3. The `clock` function needs **Cloud Scheduler**; Firebase turns it on during the first deploy. If the deploy log asks for it, enable "Cloud Scheduler API" in Google Cloud console.
