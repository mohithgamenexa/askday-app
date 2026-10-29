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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.askday.data.Ask
import app.askday.data.AskResponse
import app.askday.data.AskType
import app.askday.data.GroupRec
import app.askday.data.Social
import app.askday.data.act
import app.askday.services.Services

private fun typeIcon(t: AskType) = when (t) {
    AskType.text -> Ico.Chat
    AskType.poll -> Ico.Groups
    AskType.thisOrThat -> Ico.Send
    AskType.slider -> Ico.Spark
    AskType.member -> Ico.User
}

private fun hoursLeft(a: Ask, now: Long): String {
    val h = maxOf(0L, (a.expiresAt - now) / 3_600_000L)
    return if (h >= 1) "${h}h left" else "under 1h left"
}

// ---------------------------------------------------- the Asks card stack

/** The group's waiting Asks as a stack of cards: answer or skip, one at a time. */
@Composable
fun AsksView(g: GroupRec) {
    val c = colors
    val db = LocalDB.current
    val now = rememberNow(10_000)
    val haptics = rememberHaptics()
    val me = db.me ?: ""
    val waiting = Social.waitingAsks(db, g.id, now)
    val mine = db.asks[g.id].orEmpty().filter { it.uid == me && now < it.expiresAt }.sortedByDescending { it.createdAt }
    val top = waiting.firstOrNull()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (top == null) {
            AskCard(padding = PaddingValues(22.dp), gap = 10.dp) {
                Display("You’re all caught up", 24)
                Txt("New Asks from the group show up here. Ask something yourself to get things going.", 15, color = c.muted, lineHeight = 22.sp)
            }
        } else {
            Box(Modifier.fillMaxWidth()) {
                if (waiting.size > 2) AskCard(Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(y = 26.dp).graphicsLayer { alpha = 0.5f }) { Box(Modifier.height(40.dp)) }
                if (waiting.size > 1) AskCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp).offset(y = 13.dp).graphicsLayer { alpha = 0.8f }) { Box(Modifier.height(40.dp)) }
                AskCardFace(g, top, onDone = { haptics(false) })
            }
            Box(Modifier.height(18.dp))
            Txt("${waiting.size} waiting", 13, FontWeight.Bold, c.muted)
        }

        AskButton("Ask the group", style = BtnStyle.Hero, icon = Ico.Plus) { Nav.push(Route.NewAsk(g.id)) }
        Txt("${Social.asksLeftToday(db, g.id, now)} of ${Social.FREE_ASKS_PER_DAY} Asks left today · Askday+ gets 5", 13, color = c.muted)

        if (mine.isNotEmpty()) {
            Label("Your Asks")
            for (a in mine) {
                AskCard(Modifier.fillMaxWidth().tap(scaleTo = 0.98f) { Nav.push(Route.AskResults(g.id, a.id)) }, gap = 6.dp) {
                    Txt(a.question, 15, FontWeight.Bold)
                    Txt("${a.responses.size} of ${g.memberIds.size - 1} answered · ${hoursLeft(a, now)}", 13, color = c.muted)
                }
            }
        }
    }
}

/** The front card: the question and the controls for its answer type. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AskCardFace(g: GroupRec, a: Ask, onDone: () -> Unit) {
    val c = colors
    val db = LocalDB.current
    val now = rememberNow(30_000)
    val author = db.person(a.uid)
    var option by remember(a.id) { mutableStateOf<Int?>(null) }
    var member by remember(a.id) { mutableStateOf<String?>(null) }
    var value by remember(a.id) { mutableFloatStateOf(5f) }
    var text by rememberSaveable(a.id) { mutableStateOf("") }
    var error by remember(a.id) { mutableStateOf<String?>(null) }
    val ready = when (a.type) {
        AskType.poll, AskType.thisOrThat -> option != null
        AskType.member -> member != null
        AskType.slider -> true
        AskType.text -> text.isNotBlank()
    }
    val submit = {
        val r = AskResponse(System.currentTimeMillis(), text = text.takeIf { a.type == AskType.text }, option = option, value = if (a.type == AskType.slider) value.toInt() else null, member = member)
        error = act { Social.respondAsk(it, g.id, a.id, r) }
        if (error == null) {
            onDone()
            Services.log("ask_answered", "type" to a.type.name)
            Nav.push(Route.AskResults(g.id, a.id))
        }
    }

    AskCard(Modifier.fillMaxWidth(), gap = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(a.uid, author?.name, author?.photo, 34.dp)
            Column(Modifier.weight(1f)) {
                Txt("${author?.name ?: "Someone"} asks", 14, FontWeight.Bold)
                Txt("${a.type.label} · ${a.responses.size} answered · ${hoursLeft(a, now)}", 12, color = c.muted3)
            }
            Row(Modifier.height(26.dp).clip(CircleShape).background(c.likedBg).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AskIcon(typeIcon(a.type), c.likedInk, 13.dp, 2.4f)
                Txt(a.type.label, 12, FontWeight.Bold, c.likedInk)
            }
        }
        Display(a.question, 26)
        when (a.type) {
            AskType.poll, AskType.thisOrThat -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                a.options.forEachIndexed { i, o ->
                    val on = option == i
                    Row(
                        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(if (on) c.likedBg else c.surface)
                            .border(if (on) 2.dp else 1.5.dp, if (on) Brand.accent else c.line2, RoundedCornerShape(16.dp))
                            .tap(label = o) { option = i }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Txt(o, 15, FontWeight.Bold, if (on) c.likedInk else c.ink, modifier = Modifier.weight(1f))
                        Box(Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (on) Brand.accent else c.line2, CircleShape).padding(4.dp).clip(CircleShape).background(if (on) Brand.accent else Color.Transparent))
                    }
                }
            }
            AskType.slider -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Display("${value.toInt()}", 44, Brand.accent)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (n in 1..10) {
                        val on = n <= value.toInt()
                        Box(
                            Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(10.dp))
                                .then(if (on) Modifier.background(Brand.hero) else Modifier.background(c.chip))
                                .tap(label = "$n", haptic = false) { value = n.toFloat() },
                            contentAlignment = Alignment.Center,
                        ) { Txt("$n", 12, FontWeight.Bold, if (on) Color.White else c.muted) }
                    }
                }
            }
            AskType.member -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (m in g.memberIds.filter { it != db.me }) {
                    val p = db.person(m)
                    val on = member == m
                    Row(
                        Modifier.clip(CircleShape).background(if (on) c.likedBg else c.surface).border(1.5.dp, if (on) Brand.accent else c.line2, CircleShape)
                            .tap(label = p?.name) { member = m }.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Avatar(m, p?.name, p?.photo, 28.dp)
                        Txt(p?.name ?: "Friend", 14, FontWeight.Bold, if (on) c.likedInk else c.ink)
                    }
                }
            }
            AskType.text -> AskField(text, { text = it.take(200) }, "Your answer…", singleLine = false, minHeight = 90.dp, radius = 16.dp, focusBorder = true, textSize = 16)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AskIcon(Ico.Lock, c.muted, 14.dp)
            Txt("Results unlock after you answer", 13, color = c.muted)
        }
        ErrorText(error)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AskButton("Skip", Modifier.weight(1f), style = BtnStyle.Outline) {
                act { Social.skipAsk(it, g.id, a.id) }
                Services.log("ask_skipped")
            }
            AskButton(if (a.type == AskType.poll || a.type == AskType.thisOrThat) "Vote" else "Answer", Modifier.weight(2f), style = BtnStyle.Hero, enabled = ready) { submit() }
        }
    }
}

// --------------------------------------------------------------- new Ask

@Composable
fun NewAskScreen(groupId: String) {
    val c = colors
    val db = LocalDB.current
    val g = db.groups[groupId]
    var type by rememberSaveable { mutableStateOf(AskType.poll) }
    var question by rememberSaveable { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "", "") }
    var error by remember { mutableStateOf<String?>(null) }
    val now = rememberNow(60_000)

    AskScreen {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackBtn { Nav.pop() }
                Column {
                    Txt("New Ask", 17, FontWeight.Bold)
                    Txt("${g?.name ?: ""} · ${g?.memberIds?.size ?: 0} members", 13, color = c.muted)
                }
            }
            Label("Answer type")
            for (row in AskType.entries.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (t in row) {
                        val on = t == type
                        Column(
                            Modifier.weight(1f).height(76.dp).clip(RoundedCornerShape(18.dp)).background(if (on) c.likedBg else c.surface)
                                .border(if (on) 2.dp else 1.dp, if (on) Brand.accent else c.line, RoundedCornerShape(18.dp))
                                .tap(label = t.label) {
                                    type = t
                                    if (t == AskType.thisOrThat) while (options.size > 2) options.removeAt(options.lastIndex)
                                    else while (options.size < 3) options.add("")
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                        ) {
                            AskIcon(typeIcon(t), if (on) c.likedInk else c.ink, 20.dp)
                            Txt(t.label, 13, FontWeight.Bold, if (on) c.likedInk else c.ink)
                        }
                    }
                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt("Your question", 14, FontWeight.Bold)
                AskField(question, { question = it.take(140) }, if (type == AskType.member) "Who’s most likely to…" else "Ask the group anything", focusBorder = true)
            }
            if (type == AskType.poll || type == AskType.thisOrThat) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Txt(if (type == AskType.thisOrThat) "The two choices" else "Options", 14, FontWeight.Bold)
                    options.forEachIndexed { i, o -> AskField(o, { options[i] = it.take(40) }, "Option ${i + 1}", textSize = 15) }
                    if (type == AskType.poll && options.size < 4) {
                        Row(
                            Modifier.fillMaxWidth().height(44.dp).clip(CircleShape).border(1.5.dp, c.line2, CircleShape).tap { options.add("") },
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AskIcon(Ico.Plus, c.ink, 16.dp)
                            Txt("Add option", 14, FontWeight.Bold)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AskIcon(Ico.Spark, Color(0xFFFFB020), 15.dp)
                Txt("${Social.asksLeftToday(db, groupId, now)} Asks left today · Askday+ gets 5 a day", 13, color = c.muted)
            }
            ErrorText(error)
            AskButton("Ask the group", style = BtnStyle.Hero, enabled = question.isNotBlank()) {
                var id: String? = null
                error = act { id = Social.createAsk(it, groupId, type, question, options.toList(), System.currentTimeMillis()) }
                id?.let {
                    Services.log("ask_created", "type" to type.name)
                    Nav.pop()
                    Nav.push(Route.AskResults(groupId, it))
                }
            }
        }
    }
}

// ----------------------------------------------------------- Ask results

@Composable
fun AskResultsScreen(groupId: String, askId: String) {
    val c = colors
    val db = LocalDB.current
    val now = rememberNow(5_000)
    val g = db.groups[groupId]
    val a = db.asks[groupId]?.firstOrNull { it.id == askId }
    val me = db.me ?: ""
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackBtn { Nav.pop() }
                Column {
                    Txt(if (a?.uid == me) "Your Ask" else "Ask results", 17, FontWeight.Bold)
                    Txt("${g?.name ?: ""}${if (a != null) " · " + hoursLeft(a, now) else ""}", 13, color = c.muted)
                }
            }
            if (a == null || g == null) {
                Txt("This Ask has expired.", 15, color = c.muted)
                return@Column
            }
            val total = a.responses.size
            if (a.uid == me) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.likedBg).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Brand.sun), contentAlignment = Alignment.Center) { AskIcon(Ico.Bell, Color.White, 22.dp) }
                    Column(Modifier.weight(1f)) {
                        Txt(if (total == 0) "Waiting for answers…" else "$total ${if (total == 1) "person" else "people"} answered your Ask", 15, FontWeight.Bold, c.likedInk)
                        Txt(if (Social.askOfTheDay(db, groupId, now)?.id == a.id) "It’s the most answered Ask in the group today." else "Answers keep coming in for 24 hours.", 13, color = c.likedInk)
                    }
                }
            }
            Display(a.question, 26)
            AskCard(gap = 14.dp) {
                when (a.type) {
                    AskType.poll, AskType.thisOrThat -> {
                        val counts = a.options.indices.map { i -> a.responses.values.count { it.option == i } }
                        val myPick = a.responses[me]?.option
                        a.options.forEachIndexed { i, o -> ResultBar(o + if (myPick == i) " · you" else "", counts[i], total, counts[i] == counts.maxOrNull() && counts[i] > 0) }
                    }
                    AskType.member -> {
                        val tally = a.responses.values.mapNotNull { it.member }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
                        if (tally.isEmpty()) Txt("No votes yet.", 14, color = c.muted)
                        tally.forEachIndexed { i, (m, n) -> ResultBar(if (m == me) "You" else db.person(m)?.name ?: "Friend", n, total, i == 0) }
                    }
                    AskType.slider -> {
                        val values = a.responses.values.mapNotNull { it.value }
                        val avg = if (values.isEmpty()) 0.0 else values.average()
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Display("%.1f".format(avg), 44, Brand.accent)
                            Txt("average from $total", 14, color = c.muted, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (n in 1..10) {
                                val k = values.count { it == n }
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.fillMaxWidth().height((8 + k * 18).dp).clip(RoundedCornerShape(6.dp)).then(if (k > 0) Modifier.background(Brand.hero) else Modifier.background(c.chip)))
                                    Txt("$n", 11, FontWeight.Bold, c.muted)
                                }
                            }
                        }
                    }
                    AskType.text -> {
                        if (a.responses.isEmpty()) Txt("No answers yet.", 14, color = c.muted)
                        for ((uid, r) in a.responses) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Avatar(uid, db.person(uid)?.name, db.person(uid)?.photo, 30.dp)
                                Column {
                                    Txt(if (uid == me) "You" else db.person(uid)?.name ?: "Friend", 13, FontWeight.Bold)
                                    Txt(r.text ?: "", 15, color = c.ink2, lineHeight = 21.sp)
                                }
                            }
                        }
                    }
                }
            }
            if (Social.askOfTheDay(db, groupId, now)?.id == a.id) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.goldBg).border(1.dp, c.goldLine, RoundedCornerShape(18.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AskIcon(Ico.Crown, c.goldInk, 20.dp)
                    Txt("Ask of the day: the most answered Ask in ${g.name} right now.", 14, FontWeight.Bold, c.goldInk, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ResultBar(label: String, count: Int, total: Int, top: Boolean) {
    val c = colors
    val pct = if (total == 0) 0 else Math.round(count * 100f / total)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Txt(label, 14, FontWeight.Bold, modifier = Modifier.weight(1f))
            Txt("$pct% · $count", 14, FontWeight.Bold, c.muted, tabular = true)
        }
        Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(c.chip)) {
            Box(Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().clip(CircleShape).then(if (top) Modifier.background(Brand.hero) else Modifier.background(c.line2)))
        }
    }
}
