package app.askday.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.askday.data.Engine
import app.askday.data.GroupRec
import app.askday.data.ThemeChoice
import app.askday.data.act
import app.askday.data.savePhoto
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- welcome

@Composable
private fun PreviewCard(initials: String, bg: Color, name: String, text: String?, angle: Float) {
    val c = colors
    Row(
        Modifier
            .width(258.dp)
            .rotate(angle)
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Avatar(name, name, size = 32.dp, label = initials, bg = bg)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Txt(name, 13, FontWeight.Bold)
            if (text != null) Txt(text, 14, color = c.ink2, lineHeight = 19.sp)
            else {
                Box(Modifier.fillMaxWidth(0.9f).height(10.dp).clip(CircleShape).background(c.line2))
                Box(Modifier.fillMaxWidth(0.6f).height(10.dp).clip(CircleShape).background(c.line2))
            }
        }
    }
}

/** Slides up into place a moment after the screen opens. */
@Composable
private fun DropIn(delayMs: Long, content: @Composable () -> Unit) {
    val y = remember { Animatable(40f) }
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        coroutineScope {
            launch { y.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 260f)) }
            launch { a.animateTo(1f) }
        }
    }
    Box(Modifier.graphicsLayer { translationY = y.value; alpha = a.value }) { content() }
}

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    val c = colors
    AskScreen {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientBox(Modifier.size(40.dp), RoundedCornerShape(13.dp)) {
                    Display("?", 24, Color.White, Modifier.align(Alignment.Center))
                }
                Display("Askday", 24)
            }
            Box(Modifier.fillMaxWidth().height(250.dp)) {
                Box(Modifier.align(Alignment.TopStart)) { DropIn(120) { PreviewCard("PR", Color(0xFFFFD9C2), "Priya", "A single sock. Not even a pair.", -5f) } }
                Box(Modifier.align(Alignment.TopEnd).offset(y = 86.dp)) { DropIn(340) { PreviewCard("SM", Color(0xFFD6E4FF), "Sam", null, 4f) } }
                Box(Modifier.offset(x = 20.dp, y = 186.dp)) {
                    DropIn(650) {
                        Row(
                            Modifier.height(46.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(c.btnBg).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AskIcon(Ico.Lock, c.btnFg, 16.dp, 2.2f)
                            Txt("Answer to unlock 4 more", 14, FontWeight.Bold, c.btnFg)
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Display("One question.\nEvery day.", 44)
                    Display("Don’t break it.", 44, if (c.dark) Color(0xFFFFB020) else Color(0xFFE5602F))
                }
                Txt(
                    "Answer with your friends before the clock runs out. Answers unlock only when everyone’s in, and your streak lives or dies together.",
                    16, color = c.muted, lineHeight = 24.sp,
                )
            }
            Box(Modifier.weight(1f))
            AskButton("Continue with phone number", icon = Ico.Lock) { onContinue() }
            Txt("By continuing you agree to our Terms and Privacy Policy.", 12, color = c.muted3, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ------------------------------------------------------------ phone login

/** Phone number, then the 6-digit code Firebase sends by SMS. */
@Composable
fun PhoneLoginScreen(onBack: () -> Unit) {
    val c = colors
    val activity = LocalContext.current as android.app.Activity
    val haptics = rememberHaptics()
    var number by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var step by rememberSaveable { mutableStateOf("number") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resendIn by remember { mutableStateOf(0) }
    LaunchedEffect(resendIn) { if (resendIn > 0) { delay(1000); resendIn-- } }

    val full = "+91" + number.filter(Char::isDigit)
    val signedIn = { uid: String, phone: String ->
        busy = false
        haptics(true)
        app.askday.services.Services.log("login", "method" to "phone")
        act { Engine.signIn(it, uid, phone) }
        Unit
    }
    val send = { resend: Boolean ->
        error = null
        busy = true
        app.askday.services.PhoneAuth.sendCode(activity, full, resend,
            onCodeSent = { busy = false; step = "code"; resendIn = 30 },
            onSignedIn = signedIn,
            onError = { busy = false; error = it })
    }

    AskScreen {
        Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
            BackBtn { if (step == "code") { step = "number"; code = "" } else onBack() }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Display(if (step == "number") "What’s your number?" else "Enter the code", 34)
                Txt(
                    if (step == "number") "We’ll text you a 6-digit code. Your number is never shown to anyone."
                    else "We sent it to +91 ${number.trim()}. On many phones it fills in by itself.",
                    15, color = c.muted, lineHeight = 22.sp,
                )
            }
            if (step == "number") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.width(76.dp).height(56.dp).clip(RoundedCornerShape(16.dp)).background(c.inputBg).border(1.5.dp, c.line2, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Txt("+91", 17, FontWeight.Bold) }
                    AskField(
                        number, { number = it.filter { ch -> ch.isDigit() || ch == ' ' }.take(12) }, "98765 43210", Modifier.weight(1f), focusBorder = true,
                        keyboard = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                    )
                }
            } else {
                AskField(
                    code, { code = it.filter(Char::isDigit).take(6) }, "6-digit code", focusBorder = true, textSize = 22,
                    keyboard = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                )
                Txt(
                    if (resendIn > 0) "Send a new code in ${resendIn}s" else "Send a new code",
                    14, FontWeight.Bold, if (resendIn > 0) c.muted else c.link,
                    Modifier.tap(enabled = resendIn == 0 && !busy) { send(true) },
                )
            }
            Box(Modifier.weight(1f))
            ErrorText(error)
            if (step == "number") {
                AskButton(if (busy) "Sending…" else "Send code", enabled = !busy && number.count(Char::isDigit) == 10) { send(false) }
            } else {
                AskButton(if (busy) "Checking…" else "Verify", enabled = !busy && code.length == 6) {
                    error = null
                    busy = true
                    app.askday.services.PhoneAuth.verifyCode(code, signedIn) { busy = false; error = it }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ setup

@Composable
fun SetupScreen(isNew: Boolean) {
    val c = colors
    val db = LocalDB.current
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(db.profile?.name ?: "") }
    var photo by rememberSaveable { mutableStateOf(db.profile?.photo) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photo = savePhoto(context, uri, "avatar-${System.currentTimeMillis()}") ?: photo
    }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    AskScreen {
        Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Box(Modifier.height(44.dp)) { if (!isNew) BackBtn { Nav.pop() } }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Display(if (isNew) "Let your friends know it’s you" else "Edit your profile", 34)
                Txt("Your name and photo show next to your answers in every group.", 15, color = c.muted, lineHeight = 22.sp)
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(132.dp)) {
                    Box(Modifier.size(132.dp).clip(CircleShape).background(Brand.hero).padding(4.dp)) {
                        Avatar(db.me ?: "me", name.ifBlank { "?" }, photo, 124.dp)
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(44.dp).clip(CircleShape).background(c.bg).padding(3.dp).clip(CircleShape).background(c.btnBg)
                            .tap(label = "Add photo") { pick() },
                        contentAlignment = Alignment.Center,
                    ) { AskIcon(Ico.Camera, c.btnFg, 20.dp) }
                }
                Txt(if (photo != null) "Change photo" else "Add a photo", 14, FontWeight.Bold, c.link, Modifier.tap { pick() })
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt("Display name", 14, FontWeight.Bold)
                AskField(name, { name = it.take(30) }, "Your name")
                Txt("You can change this any time in Profile.", 13, color = c.muted)
            }
            Box(Modifier.weight(1f))
            ErrorText(error)
            AskButton(if (isNew) "Continue" else "Save changes", enabled = name.isNotBlank()) {
                error = act { Engine.saveProfile(it, name.trim(), photo, System.currentTimeMillis()) }
                if (error == null && isNew) app.askday.services.Services.log("sign_up")
                if (error == null && !isNew) Nav.pop()
            }
        }
    }
}

// ------------------------------------------------------------------ today

private class Status(val label: String, val bg: Color, val fg: Color, val urgent: Boolean = false, val risk: Boolean = false, val needsAnswer: Boolean = false)

@Composable
private fun statusFor(g: GroupRec, now: Long): Status {
    val c = colors
    val db = LocalDB.current
    val me = db.me ?: ""
    val q = db.current(g) ?: return Status("Starting soon", c.chip, c.muted)
    val answered = me in q.answeredBy
    val waiting = maxOf(g.memberIds.size - q.answeredBy.size, 0)
    if (!answered && !q.unlocked) return Status("Answer · ${formatLeft(q.unlockAt - now)} left", c.btnBg, c.btnFg, urgent = true, needsAnswer = true)
    if (!answered) return Status("Answer to see ${q.answeredBy.size}", c.btnBg, c.btnFg, needsAnswer = true)
    if (!q.unlocked) {
        return if (g.streak > 0) Status("Streak at risk · $waiting out", c.riskBg, c.riskInk, risk = true)
        else Status("Waiting on $waiting", c.chip, c.muted)
    }
    val seen = db.seen[g.id]
    val fresh = q.answeredBy.size - (if (seen?.questionId == q.id) seen.count else 1)
    return if (fresh > 0) Status("$fresh new answer${if (fresh == 1) "" else "s"}", c.okBg, c.okInk) else Status("All caught up", c.chip, c.muted)
}

@Composable
private fun StatTile(value: String, label: String, color: Color, modifier: Modifier) {
    val c = colors
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(18.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Display(value, 24, color, maxLines = 1)
        Txt(label, 12, color = c.muted)
    }
}

@Composable
fun TodayScreen() {
    val c = colors
    val db = LocalDB.current
    val now = rememberNow(15_000)
    val groups = db.groupsSorted()
    val statuses = groups.map { statusFor(it, now) }
    val toAnswer = statuses.count { it.needsAnswer }
    val nextUnlock = groups.mapNotNull { g -> db.current(g)?.takeIf { !it.unlocked }?.unlockAt ?: g.nextDropAt }.filter { it > now }.minOrNull()
    val recapGroup = groups.firstOrNull()?.takeIf { g -> (db.questions[g.id]?.values?.count { it.dropAt >= now - 7 * 86_400_000L } ?: 0) >= 3 }

    AskScreen(bottomBar = false) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Label(SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(now)))
                    Display("Today", 36)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconBtn(if (c.dark) Ico.Sun else Ico.Moon, if (c.dark) "Switch to light mode" else "Switch to dark mode") {
                        act { it.theme = if (c.dark) ThemeChoice.light else ThemeChoice.dark }
                        app.askday.services.Services.log("theme_changed", "theme" to if (c.dark) "light" else "dark")
                    }
                    IconBtn(Ico.Plus, "Create a group", solid = true) { Nav.push(Route.NewGroup) }
                }
            }

            MomentCard { Nav.tab(Route.Friends) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("${db.profile?.stats?.bestStreak ?: 0}", "Best streak", c.flameInk, Modifier.weight(1f))
                StatTile("${groups.sumOf { it.freezes }}", "Freezes", c.iceInk, Modifier.weight(1f))
                StatTile("${app.askday.data.Social.waitingAsksTotal(db, now)}", "Asks waiting", c.ink, Modifier.weight(1f))
            }
            val quickLeft = app.askday.data.Social.quickQuestions(db, now).count { db.quick[app.askday.data.Social.quickKey(now, it)] == null }
            if (quickLeft > 0) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brand.sun).tap(scaleTo = 0.98f) { Nav.tab(Route.Discover) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AskIcon(Ico.Spark, Color.White, 22.dp)
                    Column(Modifier.weight(1f)) {
                        Txt("$quickLeft quick questions today", 15, FontWeight.Bold, Color.White)
                        Txt("Answer to see what people around the world said", 13, color = Color.White.copy(alpha = 0.92f))
                    }
                }
            }
            if (groups.isNotEmpty()) Label("Your groups · ${toAnswer} to answer · next unlock ${nextUnlock?.let { shortLeft(it - now) } ?: "—"}")

            if (recapGroup != null) {
                GradientBox(Modifier.fillMaxWidth().tap { Nav.push(Route.Recap(recapGroup.id)) }, RoundedCornerShape(22.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                            AskIcon(Ico.Story, Color.White, 22.dp)
                        }
                        Column(Modifier.weight(1f)) {
                            Txt("Your week in ${recapGroup.name}", 15, FontWeight.Bold, Color.White)
                            Txt("Tap to watch your weekly recap.", 13, color = Brand.onHeroMuted)
                        }
                    }
                }
            }

            if (groups.isEmpty()) {
                AskCard(padding = androidx.compose.foundation.layout.PaddingValues(24.dp), gap = 14.dp) {
                    Display("Start a group with your people", 26)
                    Txt("Create a group and your first question arrives right away. Sample friends join so you can try everything.", 15, color = c.muted, lineHeight = 22.sp)
                    AskButton("Create a group", style = BtnStyle.Hero) { Nav.push(Route.NewGroup) }
                }
            }

            groups.forEachIndexed { i, g ->
                val s = statuses[i]
                val q = db.current(g)
                AskCard(
                    Modifier.fillMaxWidth().tap(label = "${g.name}, ${s.label}", scaleTo = 0.98f) { Nav.push(Route.Group(g.id)) },
                    border = if (s.risk) c.riskLine else c.line,
                    glow = s.urgent,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Txt(g.name, 17, FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                        if (g.freezes > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                AskIcon(Ico.Snowflake, c.iceInk, 14.dp)
                                Txt("${g.freezes}", 13, FontWeight.Bold, c.iceInk)
                            }
                        }
                        StreakChip(g.streak, compact = true)
                    }
                    if (s.urgent && q != null) Txt(q.text, 15, color = c.ink2, lineHeight = 21.sp, maxLines = 2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarStack(g.memberIds.map { Triple(it, db.person(it)?.name, db.person(it)?.photo) })
                        Box(Modifier.weight(1f))
                        Pill(s.label, s.bg, s.fg)
                    }
                }
            }
            if (groups.isNotEmpty()) AdBanner()
        }
    }
}

// ---------------------------------------------------------------- tab bar

@Composable
fun TabBar(route: Route) {
    val c = colors
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(14.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(c.bar)
                .border(1.dp, c.line, RoundedCornerShape(24.dp)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tabs = listOf(Triple(Route.Today, "Today", Ico.Sun), Triple(Route.Friends, "Friends", Ico.Groups), Triple(Route.Groups, "Groups", Ico.Chat), Triple(Route.Discover, "Discover", Ico.Spark), Triple(Route.Me, "Me", Ico.User))
            for ((r, label, icon) in tabs) {
                val active = r == route || (r == Route.Me && route == Route.History)
                Row(
                    Modifier
                        .height(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .then(if (active) Modifier.background(Brand.hero) else Modifier.background(Color.Transparent))
                        .tap(label = label, scaleTo = 0.9f) { if (r != route) Nav.tab(r) }
                        .padding(horizontal = if (active) 14.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AskIcon(icon, if (active) Color.White else c.muted, 21.dp)
                    if (active) Txt(label, 14, FontWeight.Bold, Color.White)
                }
            }
        }
    }
}

