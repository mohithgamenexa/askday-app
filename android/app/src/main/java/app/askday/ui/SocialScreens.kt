package app.askday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.askday.data.Reaction
import app.askday.data.Social
import app.askday.data.act
import app.askday.services.Services
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun clockTime(ms: Long) = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))

// ----------------------------------------------------------- the moment

/** The global Askday moment card, used on Today. */
@Composable
fun MomentCard(onOpen: () -> Unit) {
    val db = LocalDB.current
    val now = rememberNow()
    val m = db.moment ?: return
    val me = db.me
    val answered = m.answers.any { it.uid == me }
    val isToday = m.dayKey == Social.dayKey(now)
    val nextDrop = Social.momentDropFor(now + if (now >= Social.momentDropFor(now)) 86_400_000L else 0L)
    GradientBox(Modifier.fillMaxWidth().tap(scaleTo = 0.98f, label = "Today’s Askday", onClick = onOpen)) {
        Box(Modifier.padding(start = 230.dp).size(170.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)))
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Label("The Askday moment", Brand.onHeroMuted) }
                if (answered) {
                    Row(Modifier.height(26.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        AskIcon(Ico.Check, Color.White, 13.dp, 2.6f)
                        Txt(if (m.answers.first { it.uid == me }.late) "Answered late" else "Answered on time", 12, FontWeight.Bold, Color.White)
                    }
                }
            }
            Display(m.text, 24, Color.White)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val friends = m.answers.count { it.uid != me }
                Txt(
                    when {
                        !isToday -> "Next Askday drops at ${clockTime(nextDrop)}"
                        answered -> "$friends friends answered · see them all"
                        else -> "$friends friends answered · answer to see"
                    },
                    13, color = Color.White.copy(alpha = 0.92f), modifier = Modifier.weight(1f),
                )
                Display(formatCountdown(if (isToday && now < m.onTimeUntil) m.onTimeUntil - now else maxOf(0, nextDrop - now)), 20, Color.White)
            }
        }
    }
}

// --------------------------------------------------------------- Friends

@Composable
fun FriendsScreen() {
    val c = colors
    val db = LocalDB.current
    val haptics = rememberHaptics()
    val m = db.moment
    val me = db.me ?: ""
    val mine = m?.answers?.firstOrNull { it.uid == me }
    var text by rememberSaveable { mutableStateOf("") }
    var attach by remember { mutableStateOf(Attachment()) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    AskScreen(bottomBar = false) {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Display("Friends", 32, modifier = Modifier.weight(1f))
                Row(
                    Modifier.height(40.dp).clip(RoundedCornerShape(14.dp)).background(c.btnBg).tap { Nav.push(Route.Contacts) }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AskIcon(Ico.Plus, c.btnFg, 16.dp, 2.4f)
                    Txt("Invite contacts", 13, FontWeight.Bold, c.btnFg)
                }
            }
            if (m == null) {
                Txt("Today’s question is on its way.", 15, color = c.muted)
                return@Column
            }
            AskCard(padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.width(6.dp).height(44.dp).clip(CircleShape).background(Brand.hero))
                    Column {
                        Label("Today’s Askday")
                        Txt(m.text, 15, FontWeight.Bold, lineHeight = 21.sp)
                    }
                }
            }
            if (mine == null) {
                AskField(text, { text = it.take(280) }, "Your answer…", singleLine = false, minHeight = 96.dp, radius = 18.dp, focusBorder = true, textSize = 16)
                AttachRow(attach) { attach = it }
                ErrorText(error)
                AskButton("Answer to see your friends", style = BtnStyle.Hero, icon = Ico.Lock, enabled = text.isNotBlank() || !attach.isEmpty) {
                    error = act { Social.answerMoment(it, text, System.currentTimeMillis(), attach.photo, attach.voice, attach.voiceMs) }
                    if (error == null) {
                        haptics(true)
                        Services.log("moment_answered")
                    }
                }
            }
            val ordered = m.answers.sortedByDescending { it.at }.sortedByDescending { it.uid == me }
            for (a in ordered) {
                val p = db.person(a.uid)
                val locked = mine == null && a.uid != me
                AskCard(gap = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Avatar(a.uid, p?.name, p?.photo, 36.dp)
                        Column(Modifier.weight(1f)) {
                            Txt(if (a.uid == me) "You" else p?.name ?: "Friend", 14, FontWeight.Bold)
                            Txt(clockTime(a.at) + if (a.late) " · Late" else " · On time", 12, color = if (a.late) c.riskInk else c.muted3)
                        }
                    }
                    if (locked) Txt("Answer today’s question to see this.", 15, color = c.ink2, modifier = Modifier.blur(6.dp))
                    else {
                        if (a.text.isNotEmpty()) Txt(a.text, 15, color = c.ink2, lineHeight = 21.sp)
                        AnswerMedia(a.photo, a.voice, a.voiceMs)
                    }
                    if (!locked) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.height(30.dp).clip(CircleShape).background(c.chip).tap(label = "Share as a story card") {
                                shareAnswerCard(context, m.text, if (a.uid == me) (db.profile?.name ?: "Me") else p?.name ?: "A friend", a.text.ifEmpty { "A photo or voice answer" })
                            }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { AskIcon(Ico.Send, c.muted, 14.dp) }
                            for (k in Reaction.entries) {
                                val n = a.reactions.values.count { it == k }
                                val on = a.reactions[me] == k
                                val icon = when (k) { Reaction.love -> Ico.Heart; Reaction.haha -> Ico.Laugh; Reaction.fire -> Ico.Flame; Reaction.wow -> Ico.Wow }
                                Row(
                                    Modifier.height(30.dp).clip(CircleShape).background(if (on) c.likedBg else c.chip).tap(label = k.name) { act { Social.reactMoment(it, a.uid, k) } }.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    AskIcon(icon, if (on) c.likedInk else c.muted, 14.dp, filled = on && k == Reaction.love)
                                    if (n > 0) Txt("$n", 13, FontWeight.Bold, if (on) c.likedInk else c.muted, tabular = true)
                                }
                            }
                        }
                    }
                }
            }
            if (m.answers.none { it.uid != me }) Txt("Your friends haven’t answered yet. Check back soon.", 14, color = c.muted, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ---------------------------------------------------------------- Groups

@Composable
fun GroupsTabScreen() {
    val c = colors
    val db = LocalDB.current
    val now = rememberNow(15_000)
    AskScreen(bottomBar = false) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Display("Groups", 32, modifier = Modifier.weight(1f))
                IconBtn(Ico.Plus, "Create a group", solid = true) { Nav.push(Route.NewGroup) }
            }
            val groups = db.groupsSorted()
            if (groups.isEmpty()) {
                AskCard(padding = PaddingValues(24.dp), gap = 14.dp) {
                    Display("Start a group with your people", 26)
                    Txt("Groups get their own daily question, Asks from members, and a shared streak.", 15, color = c.muted, lineHeight = 22.sp)
                    AskButton("Create a group", style = BtnStyle.Hero) { Nav.push(Route.NewGroup) }
                }
            }
            for (g in groups) {
                val waiting = Social.waitingAsks(db, g.id, now).size
                AskCard(Modifier.fillMaxWidth().tap(scaleTo = 0.98f, label = g.name) { Nav.push(Route.Group(g.id, tab = if (waiting > 0) "asks" else null)) }, glow = waiting > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Txt("${g.name} · ${g.memberIds.size}", 17, FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                        StreakChip(g.streak, compact = true)
                    }
                    if (g.starter) Txt("Starter group · sample members", 12, FontWeight.Bold, c.muted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarStack(g.memberIds.map { Triple(it, db.person(it)?.name, db.person(it)?.photo) })
                        Box(Modifier.weight(1f))
                        if (waiting > 0) Pill("$waiting Asks · answer now", c.btnBg, c.btnFg) else Pill("All caught up", c.chip, c.muted)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------- Discover

private class PublicAnswer(val initials: String, val name: String, val place: String, val text: String, val loves: String)

private val PUBLIC = listOf(
    PublicAnswer("LM", "Lucía", "Madrid", "My grandma finally learned to send voice notes. She sent me 14.", "8.2k"),
    PublicAnswer("TK", "Takumi", "Osaka", "The train was late, so I saw the sunrise from the platform.", "5.9k"),
    PublicAnswer("NA", "Nisha", "Pune", "My dog learned to open the fridge. I’m proud and scared.", "4.1k"),
    PublicAnswer("OB", "Ola", "Accra", "A stranger paid for my coffee and left before I could thank them.", "3.7k"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen() {
    val c = colors
    AskScreen(bottomBar = false) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Display("Discover", 32)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.likedBg).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AskIcon(Ico.Spark, c.likedInk, 18.dp)
                Txt("A preview. Discover goes live with real answers from around the world in the online version.", 13, color = c.likedInk, lineHeight = 18.sp, modifier = Modifier.weight(1f))
            }
            QuickQuestions()
            Label("Most loved answers worldwide")
            PUBLIC.forEachIndexed { i, a ->
                AskCard(gap = 8.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Avatar("pub$i", a.name, size = 30.dp, label = a.initials, bg = Brand.avatar[(i + 2) % Brand.avatar.size])
                        Txt("${a.name} · ${a.place}", 13, FontWeight.Bold, modifier = Modifier.weight(1f))
                        AskIcon(Ico.Heart, c.muted, 14.dp)
                        Txt(a.loves, 13, FontWeight.Bold, c.muted)
                    }
                    Txt(a.text, 15, color = c.ink2, lineHeight = 21.sp)
                }
            }
            Label("Trending Asks")
            AskCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AskIcon(Ico.Spark, Brand.accent, 22.dp)
                    Column(Modifier.weight(1f)) {
                        Txt("“What’s a movie everyone loves that you hated?”", 15, FontWeight.Bold)
                        Txt("From the community", 13, color = c.muted)
                    }
                }
            }
            Label("Communities")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (name in listOf("Your college", "Your city", "Cricket fans", "Movie buffs", "Foodies")) {
                    Box(Modifier.height(40.dp).clip(CircleShape).background(c.surface).border(1.5.dp, c.line2, CircleShape).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Txt(name, 14, FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------- Memories

@Composable
fun MemoriesScreen() {
    val c = colors
    val db = LocalDB.current
    val days = Social.answeredDays(db)
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstWeekday = Calendar.getInstance().apply { set(year, month, 1) }.get(Calendar.DAY_OF_WEEK) - 1
    val yearAgo = "%04d%02d%02d".format(year - 1, month + 1, today)
    val throwback = db.momentLog[yearAgo]?.let { (db.momentQuestions[yearAgo] ?: "A year ago") to it }
    val latest = db.momentLog.keys.maxOrNull()?.let { (db.momentQuestions[it] ?: "Your latest answer") to db.momentLog[it]!! }
    val monthKey = "%04d%02d".format(year, month + 1)
    val countThisMonth = days.count { it.startsWith(monthKey) }

    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackBtn { Nav.pop() }
                Column {
                    Txt("Memories", 17, FontWeight.Bold)
                    Txt("Every answer you’ve given", 13, color = c.muted)
                }
            }
            val (q, a) = throwback ?: latest ?: ("Your first memory is waiting" to "Answer today’s Askday and it lands here.")
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brand.sun).padding(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label(if (throwback != null) "1 year ago today" else "Latest memory", Color.White.copy(alpha = 0.9f))
                    Display("“$q”", 22, Color.White)
                    Txt(if (throwback != null || latest != null) "You said: $a" else a, 15, color = Color.White)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Label(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)) }
                Txt("$countThisMonth of $today days", 13, FontWeight.Bold, c.muted, tabular = true)
            }
            val cells = firstWeekday + daysInMonth
            for (week in 0 until (cells + 6) / 7) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (dow in 0 until 7) {
                        val day = week * 7 + dow - firstWeekday + 1
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (day in 1..daysInMonth) {
                                val answered = "$monthKey%02d".format(day) in days
                                Box(
                                    Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).then(
                                        when {
                                            day == today && answered -> Modifier.background(Brand.hero)
                                            answered -> Modifier.background(c.likedBg)
                                            else -> Modifier.background(c.chip)
                                        },
                                    ),
                                    contentAlignment = Alignment.Center,
                                ) { Txt("$day", 12, FontWeight.Bold, if (day == today && answered) Color.White else if (answered) c.likedInk else c.muted3) }
                            }
                        }
                    }
                }
            }
            AskCard(padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientBox(Modifier.size(44.dp), RoundedCornerShape(14.dp)) { AskIcon(Ico.Spark, Color.White, 22.dp, modifier = Modifier.align(Alignment.Center)) }
                    Column(Modifier.weight(1f)) {
                        Txt("Your Year in Askday", 15, FontWeight.Bold)
                        Txt("Arrives in December. Built to share.", 13, color = c.muted)
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------- Askday+

@Composable
fun PlusScreen() {
    val c = colors
    var notice by remember { mutableStateOf(false) }
    val perks = listOf(Ico.Snowflake to "Unlimited streak freezes, plus streak repair", Ico.Plus to "5 Asks a day, and photo Asks", Ico.Groups to "Bigger groups, up to 200", Ico.Clock to "Full Memories and Year in Askday", Ico.Sun to "Themes, app icons and profile flair")
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBtn(Ico.Close, "Close", size = 44.dp) { Nav.pop() }
            GradientBox(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Display("Askday+", 34, Color.White)
                    Txt("Never lose a streak. Ask more. Remember everything.", 15, color = Color.White.copy(alpha = 0.92f))
                }
            }
            AskCard(gap = 12.dp) {
                for ((icon, text) in perks) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AskIcon(icon, Brand.accent, 20.dp)
                        Txt(text, 15, FontWeight.Medium)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(c.likedBg).border(2.dp, Brand.accent, RoundedCornerShape(20.dp)).padding(14.dp)) {
                    Txt("Yearly · save 33%", 14, FontWeight.Bold, c.likedInk)
                    Display("₹799", 24)
                    Txt("₹67 / month", 12, color = c.muted)
                }
                Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(20.dp)).padding(14.dp)) {
                    Txt("Monthly", 14, FontWeight.Bold)
                    Display("₹99", 24)
                    Txt("per month", 12, color = c.muted)
                }
            }
            if (notice) Txt("Askday+ isn’t on sale yet. It arrives with the online version.", 14, FontWeight.Medium, c.likedInk, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            AskButton("Start 7-day free trial", style = BtnStyle.Hero) {
                notice = true
                Services.log("plus_tapped")
            }
            Txt("Prices are examples and may change.", 12, color = c.muted3, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ------------------------------------------------------- quick questions

/** Five global questions a day: answer any of them to see what people around the world said. */
@Composable
fun QuickQuestions() {
    val c = colors
    val db = LocalDB.current
    val now = rememberNow(60_000)
    val open = remember { androidx.compose.runtime.mutableStateMapOf<Int, String>() }
    Label("Quick questions · new every day")
    for (qi in Social.quickQuestions(db, now)) {
        val mine = db.quick[Social.quickKey(now, qi)]
        AskCard(gap = 10.dp) {
            Txt(app.askday.data.DEMO_QUESTIONS[qi].text, 16, FontWeight.Bold, lineHeight = 21.sp)
            if (mine == null) {
                val draft = open[qi] ?: ""
                AskField(draft, { open[qi] = it.take(200) }, "Your answer…", textSize = 15)
                if (draft.isNotBlank()) AskButton("Answer to see the world", style = BtnStyle.Hero) {
                    act { Social.answerQuick(it, qi, draft, System.currentTimeMillis()) }
                    Services.log("quick_answered")
                }
            } else {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.likedBg).padding(12.dp)) {
                    Txt("You: $mine", 14, FontWeight.Bold, c.likedInk)
                }
                for (a in Social.quickAnswers(qi, now)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Avatar("w" + a.name, a.name, size = 28.dp)
                        Column(Modifier.weight(1f)) {
                            Txt("${a.name} · ${a.place}", 12, FontWeight.Bold, c.muted)
                            Txt(a.text, 14, color = c.ink2, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}
