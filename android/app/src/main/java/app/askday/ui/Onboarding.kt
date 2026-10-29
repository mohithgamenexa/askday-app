package app.askday.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.askday.data.DEMO_QUESTIONS
import app.askday.data.Social
import app.askday.data.act
import app.askday.services.Services

/** Starts a guest account and continues onboarding. */
@Composable
fun GuestLink(modifier: Modifier = Modifier) {
    val c = colors
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Txt(
            if (busy) "Starting…" else "Continue as guest",
            14, FontWeight.Bold, c.muted,
            Modifier.fillMaxWidth().tap(enabled = !busy, label = "Continue as guest") {
                busy = true
                error = null
                app.askday.services.PhoneAuth.signInAsGuest(
                    onSignedIn = { uid ->
                        busy = false
                        Services.log("login", "method" to "guest")
                        act { app.askday.data.Engine.signIn(it, uid, "") }
                        app.askday.services.Cloud.afterSignIn(uid)
                    },
                    onError = { busy = false; error = it },
                )
            },
            align = TextAlign.Center,
        )
        ErrorText(error)
    }
}

/** The thin progress bar along the top of onboarding. */
@Composable
fun OnboardingProgress(step: Int, total: Int = 5) {
    val c = colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until total) {
            Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).then(if (i < step) Modifier.background(Brand.hero) else Modifier.background(c.line2)))
        }
    }
}

// --------------------------------------------------------- 1 · the hook

/** Today's live global question, a countdown and blurred answers: answer to see. */
@Composable
fun HookScreen(onAnswer: () -> Unit, onHaveAccount: () -> Unit) {
    val c = colors
    val db = LocalDB.current
    val now = rememberNow()
    val m = db.moment
    val question = m?.text ?: DEMO_QUESTIONS[Social.momentIndexFor(Social.dayKey(now))].text
    val previews = listOf(Triple("PR", "Priya · Mumbai", "My chai was the perfect temperature, finally."), Triple("JM", "Jonas · Berlin", "A stranger held the lift door for me."), Triple("AO", "Amaka · Lagos", "My little sister learned to ride a bike."))
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientBox(Modifier.size(40.dp), RoundedCornerShape(13.dp)) { Display("?", 24, Color.White, Modifier.align(Alignment.Center)) }
                Display("Askday", 24, modifier = Modifier.weight(1f))
                Row(Modifier.height(30.dp).clip(CircleShape).background(c.riskBg).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Brand.pink))
                    Txt("LIVE", 13, FontWeight.Bold, c.riskInk)
                }
            }
            GradientBox(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Label("Today’s Askday · everyone, everywhere", Brand.onHeroMuted)
                    Display(question, 30, Color.White)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Txt("Time left to answer on time", 12, FontWeight.Medium, Brand.onHeroMuted)
                            Display(formatCountdown(maxOf(0L, (m?.onTimeUntil ?: (now + 5 * 60_000L)) - now)), 28, Color.White)
                        }
                        Row(Modifier.height(32.dp).clip(CircleShape).background(Color(0x47061214)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AskIcon(Ico.Lock, Color.White, 14.dp, 2.2f)
                            Txt("Answers hidden", 13, FontWeight.Bold, Color.White)
                        }
                    }
                }
            }
            AskCard(gap = 10.dp) {
                previews.forEachIndexed { i, (ini, who, text) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Avatar("p$i", who, size = 32.dp, label = ini, bg = Brand.avatar[i])
                        Column {
                            Txt(who, 13, FontWeight.Bold)
                            Txt(text, 14, color = c.ink2, modifier = Modifier.blur(5.dp))
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Display("One question. Everyone. Answer to see.", 26)
                AskButton("Answer today’s question", style = BtnStyle.Hero, onClick = onAnswer)
                Txt("I already have an account", 14, FontWeight.Bold, c.link, Modifier.fillMaxWidth().tap(onClick = onHaveAccount), align = TextAlign.Center)
                GuestLink()
            }
        }
    }
}

// ------------------------------------------------------ 2 · answer first

/** Write your first answer before creating an account. */
@Composable
fun AnswerFirstScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val c = colors
    val db = LocalDB.current
    val now = rememberNow(60_000)
    var text by rememberSaveable { mutableStateOf(db.pendingAnswer ?: "") }
    val question = db.moment?.text ?: DEMO_QUESTIONS[Social.momentIndexFor(Social.dayKey(now))].text
    AskScreen {
        Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            BackBtn(onBack)
            OnboardingProgress(1)
            Label("Step 1 · Your first answer")
            Display(question, 30)
            AskField(text, { text = it.take(280) }, "Type your answer…", singleLine = false, minHeight = 120.dp, radius = 20.dp, focusBorder = true, textSize = 17)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.likedBg).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AskIcon(Ico.Lock, c.likedInk, 22.dp)
                Txt("Nobody sees it yet. Save your answer to unlock what your friends said today.", 14, color = c.likedInk, lineHeight = 20.sp, modifier = Modifier.weight(1f))
            }
            Box(Modifier.weight(1f))
            AskButton("Save my answer", style = BtnStyle.Hero, enabled = text.isNotBlank()) {
                act { Social.setPendingAnswer(it, text) }
                Services.log("onboarding_answer_first")
                onSaved()
            }
        }
    }
}

// ----------------------------------------------- 4 · username and birthday

@Composable
fun DetailsScreen() {
    val c = colors
    var username by rememberSaveable { mutableStateOf("") }
    var birthday by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AskScreen {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            OnboardingProgress(3)
            Label("Step 3 · Make it yours")
            Display("Pick a username", 32)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt("Username", 14, FontWeight.Bold)
                AskField(username, { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '.' || ch == '_' }.take(20) }, "aarav.k", focusBorder = true)
                Txt("Friends find you with @${username.ifBlank { "username" }}.", 13, color = c.muted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt("Birthday", 14, FontWeight.Bold)
                AskField(
                    birthday,
                    { raw ->
                        val digits = raw.filter(Char::isDigit).take(8)
                        birthday = buildString {
                            digits.forEachIndexed { i, ch -> if (i == 2 || i == 4) append(" / "); append(ch) }
                        }
                    },
                    "DD / MM / YYYY",
                    keyboard = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Txt("You need to be 13 or older. Your birthday is never shown on your profile.", 13, color = c.muted, lineHeight = 18.sp)
            }
            Box(Modifier.height(12.dp))
            ErrorText(error)
            AskButton("Continue", enabled = username.length >= 3 && birthday.length >= 14) {
                error = act { Social.saveDetails(it, username, birthday.replace(" ", "")) }
            }
        }
    }
}

// ------------------------------------------------------ 5 · find friends

@Composable
fun FindFriendsScreen(onNext: () -> Unit) {
    val c = colors
    val db = LocalDB.current
    val context = LocalContext.current
    val handle = db.profile?.username ?: "me"
    val share = { via: String ->
        Services.log("invite_shared", "via" to via)
        val text = "Join me on Askday: one question a day, answered together. Add me: @$handle"
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Invite friends"))
    }
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            OnboardingProgress(4)
            Label("Step 4 · Your people")
            Display("Askday is better with friends", 32)
            Txt("Invite your people from your contacts, or share your username.", 15, color = c.muted, lineHeight = 22.sp)
            ContactsList(compact = true)
            AskCard(gap = 14.dp) {
                for ((label, icon, via) in listOf(Triple("Share on WhatsApp or anywhere", Ico.WhatsApp, "share"), Triple("Send your username", Ico.Chat, "username"))) {
                    Row(Modifier.fillMaxWidth().tap { share(via) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(c.chip), contentAlignment = Alignment.Center) { AskIcon(icon, c.ink, 20.dp) }
                        Txt(label, 15, FontWeight.Bold, modifier = Modifier.weight(1f))
                        AskIcon(Ico.Chevron, c.muted, 16.dp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(c.chip), contentAlignment = Alignment.Center) { AskIcon(Ico.User, c.ink, 20.dp) }
                    Column(Modifier.weight(1f)) {
                        Txt("Your username", 13, color = c.muted)
                        Txt("@$handle", 16, FontWeight.Bold)
                    }
                }
            }
            Txt("You’ll also join two starter groups with sample members, so there’s something to answer right away.", 13, color = c.muted, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Box(Modifier.height(8.dp))
            AskButton("Continue", onClick = onNext)
        }
    }
}

// ---------------------------------------- 6 · notifications and your vibe

private val PACKS = listOf("Funny", "Deep talk", "Throwback", "Hot takes", "Family", "Spicy", "Campus", "Work")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VibeScreen(askNotifications: () -> Unit) {
    val c = colors
    val picked = remember { mutableStateListOf("Funny", "Deep talk", "Hot takes") }
    var asked by remember { mutableStateOf(false) }
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            OnboardingProgress(5)
            Label("Step 5 · Never miss the moment")
            AskCard(gap = 10.dp, padding = androidx.compose.foundation.layout.PaddingValues(22.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientBox(Modifier.size(64.dp), RoundedCornerShape(22.dp)) { AskIcon(Ico.Bell, Color.White, 30.dp, modifier = Modifier.align(Alignment.Center)) }
                    Display("One ping a day. That’s it.", 24, align = TextAlign.Center)
                    Txt("The Askday question drops at a different time every day. We’ll tell you the moment it does, and when friends answer your Asks.", 14, color = c.muted, align = TextAlign.Center, lineHeight = 20.sp)
                    AskButton(if (asked) "Notifications on" else "Turn on", enabled = !asked) {
                        asked = true
                        askNotifications()
                    }
                }
            }
            Label("Pick your vibe")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (p in PACKS) {
                    val on = p in picked
                    Box(
                        Modifier.height(44.dp).clip(CircleShape).background(if (on) c.likedBg else c.surface)
                            .border(1.5.dp, if (on) Brand.accent else c.line2, CircleShape)
                            .tap(label = p) { if (on) picked.remove(p) else picked.add(p) }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Txt(p, 14, FontWeight.Bold, if (on) c.likedInk else c.ink) }
                }
            }
            Txt("This shapes your group questions. Change it any time.", 13, color = c.muted)
            Box(Modifier.height(8.dp))
            AskButton("Reveal today’s answers", style = BtnStyle.Hero) {
                act { Social.finishOnboarding(it, picked.toList(), System.currentTimeMillis()) }
                Services.log("onboarding_complete", "packs" to picked.size)
                Nav.tab(Route.Friends)
            }
        }
    }
}
