package app.askday.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.askday.data.BADGES
import app.askday.data.BadgeIcon
import app.askday.data.CelebrationKind
import app.askday.data.DB
import app.askday.data.Engine
import app.askday.data.NotifPrefs
import app.askday.data.QuestionRec
import app.askday.data.Store
import app.askday.data.ThemeChoice
import app.askday.data.act
import app.askday.data.badgeById
import app.askday.data.qkey
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --------------------------------------------------------------- history

private fun DB.topAnswer(groupId: String, q: QuestionRec): app.askday.data.Answer? {
    val me = me ?: return null
    if (!q.unlocked || me !in q.answeredBy) return null
    return answers[qkey(groupId, q.id)]?.firstOrNull { it.id == q.topAnswerId }
}

@Composable
fun HistoryScreen() {
    val c = colors
    val db = LocalDB.current
    val groups = db.groupsSorted()
    var selected by remember { mutableStateOf<String?>(null) }
    val g = groups.firstOrNull { it.id == selected } ?: groups.firstOrNull()
    val me = db.me ?: ""
    val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

    AskScreen(bottomBar = false) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Display("History", 36, modifier = Modifier.weight(1f))
                if (g != null) {
                    Row(
                        Modifier.height(40.dp).clip(RoundedCornerShape(14.dp)).background(c.btnBg).tap { Nav.push(Route.Recap(g.id)) }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AskIcon(Ico.Story, c.btnFg, 16.dp)
                        Txt("Weekly recap", 13, FontWeight.Bold, c.btnFg)
                    }
                }
            }
            if (groups.isEmpty()) Txt("Create a group and its questions will collect here.", 15, color = c.muted)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (x in groups) {
                    val active = x.id == g?.id
                    Box(
                        Modifier.height(38.dp).clip(CircleShape).background(if (active) c.btnBg else c.surface)
                            .then(if (active) Modifier else Modifier.border(1.dp, c.line, CircleShape))
                            .tap { selected = x.id }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Txt(x.name, 13, if (active) FontWeight.Bold else FontWeight.Medium, if (active) c.btnFg else c.ink) }
                }
            }
            if (g != null) {
                for (q in db.questions[g.id]?.values.orEmpty().sortedByDescending { it.day }) {
                    val top = db.topAnswer(g.id, q)
                    val day = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(q.dropAt))
                    val label = (if (day == today) "Today" else SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(q.dropAt))) + " · Day ${q.day}"
                    AskCard(Modifier.fillMaxWidth().tap(scaleTo = 0.98f) { Nav.push(Route.Group(g.id, q.id)) }, border = if (q.frozen) c.iceLine else c.line, gap = 8.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { Label(label) }
                            if (q.frozen) AskIcon(Ico.Snowflake, c.iceInk, 13.dp)
                            Txt(
                                if (q.frozen) " Freeze used" else "${q.answeredBy.size}/${maxOf(g.memberIds.size, q.answeredBy.size)} answered",
                                12, FontWeight.Bold, if (q.frozen) c.iceInk else if (q.allAnswered) c.okInk else c.muted,
                            )
                        }
                        Txt(q.text, 16, FontWeight.Bold, lineHeight = 21.sp)
                        when {
                            top != null -> Txt("Most liked · ${if (top.uid == me) "You" else db.person(top.uid)?.name}: “${top.text}”", 14, color = c.muted, lineHeight = 20.sp, maxLines = 2)
                            me !in q.answeredBy && System.currentTimeMillis() - q.dropAt <= 7 * 86_400_000L -> Txt("Tap to answer late", 14, FontWeight.Bold, c.link)
                            me !in q.answeredBy -> Txt("You didn’t answer this one.", 14, color = c.muted3)
                        }
                    }
                }
                AdBanner()
            }
        }
    }
}

// --------------------------------------------------------------- profile

@Composable
fun BadgeGlyph(icon: BadgeIcon, color: Color, size: Int) {
    val ico = when (icon) {
        BadgeIcon.spark -> Ico.Spark
        BadgeIcon.sun -> Ico.Sun
        BadgeIcon.moon -> Ico.Moon
        BadgeIcon.flame -> Ico.Flame
        BadgeIcon.crown -> Ico.Crown
        BadgeIcon.brain -> Ico.Brain
    }
    AskIcon(ico, color, size.dp)
}

@Composable
private fun SettingsRow(last: Boolean = false, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val c = colors
    Column {
        Row(
            Modifier.fillMaxWidth().height(56.dp).then(if (onClick != null) Modifier.tap(scaleTo = 0.99f, onClick = onClick) else Modifier).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen() {
    val c = colors
    val db = LocalDB.current
    val p = db.profile ?: return
    var confirm by remember { mutableStateOf<String?>(null) }

    AskScreen(bottomBar = false) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.clip(CircleShape).background(Brand.hero).padding(3.dp)) { Avatar(p.id, p.name, p.photo, 70.dp) }
                Column(Modifier.weight(1f)) {
                    Display(p.name, 28, maxLines = 1)
                    Txt((p.username?.let { "@$it · " } ?: "") + (if (app.askday.services.PhoneAuth.isGuest) "Guest · " else "") + "Joined " + SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(p.createdAt)), 14, color = c.muted)
                }
                Box(
                    Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(c.surface).border(1.5.dp, c.line2, RoundedCornerShape(12.dp))
                        .tap { Nav.push(Route.EditProfile) }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Txt("Edit", 13, FontWeight.Bold) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((value, label, tint) in listOf(
                    Triple(p.stats.bestStreak, "Best streak", c.flameInk),
                    Triple(p.stats.answers, "Answers", c.ink),
                    Triple(db.groupsSorted().size, "Groups", c.ink),
                )) {
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(18.dp)).padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Display("$value", 26, tint)
                        Txt(label, 12, color = c.muted)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brand.sun).tap(scaleTo = 0.98f) { Nav.push(Route.Plus) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Txt("Try Askday+ free", 16, FontWeight.Bold, Color.White)
                    Txt("Unlimited freezes, more Asks, full Memories", 13, color = Color.White.copy(alpha = 0.92f))
                }
                AskIcon(Ico.Send, Color.White, 20.dp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((label, icon, route) in listOf(Triple("Memories", Ico.Clock, Route.Memories), Triple("History", Ico.Story, Route.History))) {
                    Row(
                        Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(16.dp))
                            .tap { Nav.push(route) }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AskIcon(icon, Brand.accent, 18.dp)
                        Txt(label, 15, FontWeight.Bold)
                    }
                }
            }
            if (app.askday.services.PhoneAuth.isGuest) {
                AskCard(gap = 10.dp, border = c.goldLine) {
                    Txt("You’re using a guest account", 15, FontWeight.Bold)
                    Txt("Add your phone number to keep your account if you change phones. Everything stays as it is.", 13, color = c.muted, lineHeight = 18.sp)
                    AskButton("Add phone number", style = BtnStyle.Soft) { Nav.push(Route.LinkPhone) }
                }
            }
            MeExtras()
            db.status?.let { Txt("“$it”", 15, FontWeight.Medium, c.ink2) }
            Row {
                Box(Modifier.weight(1f)) { Label("Badges") }
                Txt("${BADGES.count { it.id in p.badges }} of ${BADGES.size}", 13, FontWeight.Bold, c.muted, tabular = true)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 4) {
                for (b in BADGES) {
                    val has = b.id in p.badges
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(18.dp))
                            .graphicsLayer { alpha = if (has) 1f else 0.55f }.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(if (has) Color(b.tint) else c.chip), contentAlignment = Alignment.Center) {
                            BadgeGlyph(b.icon, if (has) Color.White else c.muted, 22)
                        }
                        Txt(b.title, 11, FontWeight.Bold, align = TextAlign.Center, maxLines = 2)
                    }
                }
            }

            Label("Question packs")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (pack in app.askday.data.PACK_NAMES) {
                    val on = pack in p.packs
                    Box(
                        Modifier.height(40.dp).clip(CircleShape).background(if (on) c.likedBg else c.surface).border(1.5.dp, if (on) Brand.accent else c.line2, CircleShape)
                            .tap(label = pack) { act { d -> d.profile?.packs?.let { if (pack in it) it.remove(pack) else it.add(pack) } } }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Txt(pack, 14, FontWeight.Bold, if (on) c.likedInk else c.ink) }
                }
            }
            Txt("Group questions come from the packs you pick.", 13, color = c.muted)

            Label("Appearance")
            Segmented(
                listOf(Triple(ThemeChoice.light, "Light", Ico.Sun), Triple(ThemeChoice.dark, "Dark", Ico.Moon), Triple(ThemeChoice.system, "Phone", Ico.PhoneTheme)),
                db.theme,
            ) { choice ->
                act { it.theme = choice }
                app.askday.services.Services.log("theme_changed", "theme" to choice.name)
            }
            Txt("“Phone” follows your phone’s light or dark setting.", 13, color = c.muted)

            Label("Notifications")
            AskCard(padding = PaddingValues(0.dp), gap = 0.dp) {
                val rows = listOf<Triple<String, (NotifPrefs) -> Boolean, (NotifPrefs, Boolean) -> Unit>>(
                    Triple("Daily question alert", { it.daily }, { n, v -> n.daily = v }),
                    Triple("Answers unlocked", { it.unlocked }, { n, v -> n.unlocked = v }),
                    Triple("Nudges from friends", { it.nudges }, { n, v -> n.nudges = v }),
                    Triple("Streak reminder", { it.reminders }, { n, v -> n.reminders = v }),
                )
                rows.forEachIndexed { i, (label, get, set) ->
                    SettingsRow(last = i == rows.lastIndex) {
                        Txt(label, 15, FontWeight.Medium, modifier = Modifier.weight(1f))
                        AskToggle(get(p.notif), label) { v -> act { d -> d.profile?.notif?.let { set(it, v) } } }
                    }
                }
            }

            Label("Account")
            AskCard(padding = PaddingValues(0.dp), gap = 0.dp) {
                SettingsRow(onClick = { Nav.push(Route.ManageGroups) }) {
                    Txt("Manage groups", 15, FontWeight.Medium, modifier = Modifier.weight(1f))
                    AskIcon(Ico.Chevron, c.muted, 16.dp)
                }
                SettingsRow(onClick = { confirm = "logout" }) { Txt("Log out", 15, FontWeight.Medium) }
                SettingsRow(last = true, onClick = { confirm = "delete" }) { Txt("Delete account", 15, FontWeight.Bold, c.danger) }
            }
        }
    }

    confirm?.let { kind ->
        ConfirmDialog(
            title = if (kind == "logout") "Log out?" else "Delete your account?",
            body = if (kind == "logout") "Your groups and answers are saved on this phone only for now, so logging out erases them. Signing in again starts fresh."
            else "This erases your groups, answers and streaks from this phone. It can’t be undone.",
            action = if (kind == "logout") "Log out" else "Delete account",
            onCancel = { confirm = null },
        ) {
            confirm = null
            app.askday.services.Services.log(if (kind == "logout") "logout" else "account_deleted")
            Nav.tab(Route.Today)
            if (kind != "logout") app.askday.services.Cloud.deleteAccount(Store.db.value)
            app.askday.services.PhoneAuth.signOut()
            Store.wipe()
        }
    }
}

/** A themed confirmation card over a dimmed screen. */
@Composable
fun ConfirmDialog(title: String, body: String, action: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val c = colors
    Box(
        Modifier.fillMaxSize().background(c.overlay).clickable(remember { MutableInteractionSource() }, null) { onCancel() }.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 380.dp).clip(RoundedCornerShape(28.dp)).background(c.surface).clickable(remember { MutableInteractionSource() }, null) {}.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Display(title, 24)
            Txt(body, 15, color = c.muted, lineHeight = 22.sp)
            AskButton(action, onClick = onConfirm)
            AskButton("Cancel", style = BtnStyle.Soft, onClick = onCancel)
        }
    }
}

// ---------------------------------------------------------- manage groups

@Composable
fun ManageGroupsScreen() {
    val c = colors
    val db = LocalDB.current
    var leaving by remember { mutableStateOf<String?>(null) }
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackBtn { Nav.pop() }
                Display("Manage groups", 26)
            }
            val groups = db.groupsSorted()
            if (groups.isEmpty()) Txt("You’re not in any groups yet.", 15, color = c.muted)
            for (g in groups) {
                AskCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(g.name, 17, FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                        AvatarStack(g.memberIds.map { Triple(it, db.person(it)?.name, db.person(it)?.photo) })
                    }
                    Txt("${g.memberIds.size} members, including sample friends", 13, color = c.muted)
                    AskButton("Leave group", style = BtnStyle.Outline) { leaving = g.id }
                }
            }
        }
    }
    leaving?.let { id ->
        ConfirmDialog("Leave ${db.groups[id]?.name}?", "This deletes the group and its answers from this phone.", "Leave group", { leaving = null }) {
            if (db.groups[id]?.cloud == true) app.askday.services.GroupsCloud.leaveGroup(id) else act { Engine.leaveGroup(it, id) }
            leaving = null
        }
    }
}

// ----------------------------------------------------------- who said it

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameScreen(groupId: String, questionId: String) {
    val c = colors
    val db = LocalDB.current
    val haptics = rememberHaptics()
    val g = db.groups[groupId]
    val q = db.questions[groupId]?.get(questionId)
    val me = db.me ?: ""
    // Shuffle once so the order doesn't give it away.
    val rounds = remember(questionId) { db.answers[qkey(groupId, questionId)].orEmpty().filter { it.uid != me }.shuffled() }
    var index by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }

    AskScreen {
        if (g == null || q == null || rounds.isEmpty()) {
            Column(Modifier.padding(24.dp)) { BackBtn { Nav.pop() } }
            return@AskScreen
        }
        if (done) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                GradientBox(Modifier.size(96.dp), RoundedCornerShape(32.dp)) { AskIcon(Ico.Brain, Color.White, 46.dp, modifier = Modifier.align(Alignment.Center)) }
                Display("$score/${rounds.size}", 56)
                Display(if (score == rounds.size) "Mind reader!" else if (score * 2 >= rounds.size) "You know your people." else "Plot twist, huh?", 28, align = TextAlign.Center)
                Txt(if (score == rounds.size) "A perfect score. That’s a badge." else "Come back tomorrow for another round.", 15, color = c.muted, align = TextAlign.Center)
                Box(Modifier.height(12.dp))
                val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
                AskButton("Back to answers") { Nav.pop(); app.askday.services.Ads.atBreak(activity) }
            }
            return@AskScreen
        }
        val round = rounds[index]
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackBtn { Nav.pop() }
                Column {
                    Txt("Who said it?", 17, FontWeight.Bold)
                    Txt("Round ${index + 1} of ${rounds.size} · Score $score", 13, color = c.muted, tabular = true)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rounds.indices.forEach { i ->
                    Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).then(if (i <= index) Modifier.background(Brand.hero) else Modifier.background(c.line)))
                }
            }
            Label(q.text)
            AskCard(padding = PaddingValues(22.dp)) { Display("“${round.text}”", 26) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                for (m in g.memberIds.filter { it != me }) {
                    val p = db.person(m)
                    val right = picked != null && m == round.uid
                    val wrong = picked == m && m != round.uid
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                            .background(if (right) c.okBg else if (wrong) c.riskBg else c.surface)
                            .border(1.5.dp, if (right) Brand.ok else if (wrong) Brand.pink else c.line, RoundedCornerShape(18.dp))
                            .tap(enabled = picked == null, label = p?.name) {
                                picked = m
                                if (m == round.uid) score++
                                haptics(m != round.uid)
                            }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Avatar(m, p?.name, p?.photo, 34.dp)
                        Txt(p?.name ?: "", 15, FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                        if (right) AskIcon(Ico.Check, c.okInk, 14.dp, 2.4f)
                        if (wrong) AskIcon(Ico.Close, c.riskInk, 16.dp)
                    }
                }
            }
            if (picked != null) {
                Txt(
                    if (picked == round.uid) "Correct!" else "Nope, that was ${db.person(round.uid)?.name}.",
                    15, FontWeight.Bold, if (picked == round.uid) c.okInk else c.riskInk, align = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                AskButton(if (index + 1 < rounds.size) "Next answer" else "See my score") {
                    if (index + 1 < rounds.size) {
                        index++
                        picked = null
                    } else {
                        act { Engine.saveGameScore(it, groupId, questionId, score, rounds.size, System.currentTimeMillis()) }
                        app.askday.services.Services.log("game_completed", "score" to score, "total" to rounds.size)
                        done = true
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------- weekly recap

private val STORY_BACKGROUNDS = listOf(
    listOf(Color(0xFF0A8A80), Color(0xFF0E7FA8), Color(0xFF2563EB)),
    listOf(Color(0xFFFF8A3D), Color(0xFFFF5E8A)),
    listOf(Color(0xFF0B3B38), Color(0xFF0A8A80)),
    listOf(Color(0xFF2563EB), Color(0xFF0EA5A4)),
    listOf(Color(0xFF0F766E), Color(0xFF2563EB)),
)

private class RecapData(
    val days: Int, val answered: Int, val everyoneIn: Int, val frozen: Int, val streak: Int, val freezes: Int,
    val received: Int, val top: app.askday.data.Answer?, val topQuestion: String?, val favorite: Pair<String, Int>?,
)

private fun DB.recap(groupId: String): RecapData? {
    val g = groups[groupId] ?: return null
    val me = me ?: return null
    val since = System.currentTimeMillis() - 7 * 86_400_000L
    val qs = questions[groupId]?.values.orEmpty().filter { it.dropAt >= since }
    if (qs.isEmpty()) return null
    var top: app.askday.data.Answer? = null
    var topQ: String? = null
    val byPerson = HashMap<String, Int>()
    var received = 0
    for (q in qs) for (a in answers[qkey(groupId, q.id)].orEmpty()) {
        val n = a.reactions.size
        byPerson[a.uid] = (byPerson[a.uid] ?: 0) + n
        if (a.uid == me) received += n
        if (n > 0 && (top == null || n > top.reactions.size)) { top = a; topQ = q.text }
    }
    val fav = byPerson.entries.maxByOrNull { it.value }?.takeIf { it.value > 0 }
    return RecapData(qs.size, qs.count { me in it.answeredBy }, qs.count { it.allAnswered }, qs.count { it.frozen }, g.streak, g.freezes, received, top, topQ, fav?.let { it.key to it.value })
}

@Composable
fun RecapScreen(groupId: String) {
    val db = LocalDB.current
    val r = db.recap(groupId)
    val name = db.groups[groupId]?.name ?: ""
    val me = db.me
    var i by remember { mutableIntStateOf(0) }
    val nameOf = { id: String -> if (id == me) "You" else db.person(id)?.name ?: "A friend" }

    val slideCount = if (r == null) 0 else 5
    val breakActivity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { app.askday.services.Ads.atBreak(breakActivity) } }
    LaunchedEffect(groupId) { app.askday.services.Services.log("recap_viewed") }
    @Composable
    fun Slide(n: Int) {
        if (r == null) return
        when (n) {
        0 -> {
            Label("Your week in $name", Brand.onHeroMuted)
            Display("${r.days}", 96, Color.White)
            Display("questions in 7 days.", 32, Color.White)
            Txt("You answered ${r.answered} of them.", 17, color = Brand.onHeroMuted)
        }
        1 -> {
            AskIcon(Ico.Flame, Color.White, 56.dp, filled = true)
            Display("${r.streak}", 96, Color.White)
            Display("day streak.", 32, Color.White)
            Txt("Everyone was in on ${r.everyoneIn} ${if (r.everyoneIn == 1) "day" else "days"}." + (if (r.frozen > 0) " A freeze saved ${r.frozen} ${if (r.frozen == 1) "day" else "days"}." else ""), 17, color = Brand.onHeroMuted)
            if (r.freezes > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AskIcon(Ico.Snowflake, Color.White, 18.dp)
                Txt("${r.freezes} freeze${if (r.freezes == 1) "" else "s"} saved for later", 15, FontWeight.Bold, Color.White)
            }
        }
        2 -> {
            Label("Answer of the week", Brand.onHeroMuted)
            val top = r.top
            if (top == null) Display("No reactions yet this week. Go show some love.", 34, Color.White)
            else {
                Txt(r.topQuestion ?: "", 16, color = Brand.onHeroMuted)
                Display("“${top.text}”", 36, Color.White)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(top.uid, nameOf(top.uid), db.person(top.uid)?.photo, 36.dp)
                    Txt("${nameOf(top.uid)} · ${top.reactions.size} reactions", 16, FontWeight.Bold, Color.White)
                }
            }
        }
        3 -> {
            AskIcon(Ico.Crown, Color.White, 56.dp)
            val fav = r.favorite
            if (fav == null) Display("The crown is up for grabs.", 34, Color.White)
            else {
                Label("Crowd favorite", Brand.onHeroMuted)
                Display(nameOf(fav.first), 44, Color.White)
                Txt("${fav.second} reactions on their answers this week.", 17, color = Brand.onHeroMuted)
            }
        }
        4 -> {
            AskIcon(Ico.Heart, Color.White, 56.dp, filled = true)
            Label("Your week", Brand.onHeroMuted)
            Display("${r.received}", 96, Color.White)
            Display("reactions on your answers.", 32, Color.White)
            Txt("Same time next week. Don’t break the streak.", 17, color = Brand.onHeroMuted)
        }
        }
    }

    LaunchedEffect(i, slideCount) {
        if (slideCount == 0) return@LaunchedEffect
        delay(5000)
        if (i + 1 < slideCount) i++ else Nav.pop()
    }
    val rise = remember(i) { Animatable(30f) }
    LaunchedEffect(i) { rise.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }

    Box(Modifier.fillMaxSize().background(Brush.linearGradient(STORY_BACKGROUNDS[i % STORY_BACKGROUNDS.size]))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (n in 0 until slideCount) Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(if (n <= i) Color.White else Color.White.copy(alpha = 0.35f)))
            }
            if (slideCount == 0) {
                Display("Not enough this week for a recap yet.", 28, Color.White, Modifier.padding(28.dp), TextAlign.Center)
            } else {
                Column(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp).graphicsLayer { translationY = rise.value },
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) { Slide(i) }
                Txt("Tap to continue", 13, color = Brand.onHeroMuted, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
            }
        }
        Row(Modifier.fillMaxSize().padding(top = 120.dp)) {
            Box(Modifier.weight(1f).fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { if (i > 0) i-- })
            Box(Modifier.weight(2f).fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { if (i + 1 < slideCount) i++ else Nav.pop() })
        }
        Box(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 24.dp, end = 12.dp).size(44.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)).tap(label = "Close recap") { Nav.pop() },
            contentAlignment = Alignment.Center,
        ) { AskIcon(Ico.Close, Color.White, 20.dp, 2.2f) }
    }
}

// ----------------------------------------------------------- celebration

/** Full-screen moment for badges, streak milestones and earned freezes. */
@Composable
fun CelebrationOverlay() {
    val c = colors
    val db = LocalDB.current
    val cel = db.celebrations.firstOrNull() ?: return
    val haptics = rememberHaptics()
    val pop = remember(cel.id) { Animatable(0.6f) }
    LaunchedEffect(cel.id) {
        haptics(true)
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
    }
    val badge = badgeById(cel.badge)
    val tint = when (cel.kind) {
        CelebrationKind.freeze -> Color(0xFF3B9CFF)
        CelebrationKind.streak -> Brand.flame
        CelebrationKind.badge -> badge?.let { Color(it.tint) } ?: Brand.accent
    }
    val title = when (cel.kind) {
        CelebrationKind.streak -> "${cel.streak}-day streak!"
        CelebrationKind.freeze -> "Streak freeze earned"
        CelebrationKind.badge -> badge?.title ?: "New badge"
    }
    val body = when (cel.kind) {
        CelebrationKind.streak -> "${cel.groupName} hasn’t missed a day in ${cel.streak} days. Legendary."
        CelebrationKind.freeze -> "${cel.streak} days strong in ${cel.groupName}. If someone misses a day, this freeze saves the streak."
        CelebrationKind.badge -> badge?.description ?: ""
    }
    Box(Modifier.fillMaxSize().background(c.overlay).clickable(remember { MutableInteractionSource() }, null) {}.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 380.dp).scale(pop.value).shadow(30.dp, RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp)).background(c.surface).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(112.dp).shadow(20.dp, RoundedCornerShape(38.dp), ambientColor = tint, spotColor = tint).clip(RoundedCornerShape(38.dp)).background(tint), contentAlignment = Alignment.Center) {
                when (cel.kind) {
                    CelebrationKind.freeze -> AskIcon(Ico.Snowflake, Color.White, 56.dp)
                    CelebrationKind.streak -> Display("${cel.streak}", 48, Color.White)
                    CelebrationKind.badge -> BadgeGlyph(badge?.icon ?: BadgeIcon.spark, Color.White, 56)
                }
            }
            Label(if (cel.kind == CelebrationKind.badge) "Badge unlocked" else if (cel.kind == CelebrationKind.streak) "Milestone" else "Reward")
            Display(title, 30, align = TextAlign.Center)
            Txt(body, 15, color = c.muted, align = TextAlign.Center, lineHeight = 22.sp)
            Box(Modifier.width(1.dp).height(4.dp))
            AskButton("Nice", style = BtnStyle.Hero) { act { Engine.dismissCelebration(it, cel.id) } }
        }
    }
}
