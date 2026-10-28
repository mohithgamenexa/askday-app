package app.askday.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.askday.data.Answer
import app.askday.data.Comment
import app.askday.data.DropSlot
import app.askday.data.Engine
import app.askday.data.GroupRec
import app.askday.data.QuestionRec
import app.askday.data.Reaction
import app.askday.data.act
import app.askday.data.qkey
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun clock(ms: Long) = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
private fun dayHeader(ms: Long) = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(ms))

// ------------------------------------------------------------- new group

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewGroupScreen() {
    val c = colors
    var mode by rememberSaveable { mutableStateOf("create") }
    var name by rememberSaveable { mutableStateOf("") }
    var slot by rememberSaveable { mutableStateOf(DropSlot.evening) }
    var code by rememberSaveable { mutableStateOf("") }
    var created by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val haptics = rememberHaptics()
    val db = LocalDB.current
    val invite = created?.let { db.groups[it]?.inviteCode }

    AskScreen {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackBtn { Nav.pop() }
                Display(if (mode == "create") "New group" else "Join a group", 26)
            }
            Segmented(listOf(Triple("create", "Create", null), Triple("join", "Join with code", null)), mode) { if (created == null) mode = it }

            if (mode == "join") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Txt("Invite code", 14, FontWeight.Bold)
                    AskField(code, { code = it.filter(Char::isLetterOrDigit).uppercase().take(10) }, "7KQ2XM")
                    Txt("It’s the last part of the invite link your friend sent you.", 13, color = c.muted)
                }
                ErrorText(error)
                AskButton("Join group", enabled = code.length >= 4) {
                    error = "Joining with a code needs the online version of Askday. For now, create a group and sample friends will join you."
                }
                return@Column
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt("Group name", 14, FontWeight.Bold)
                AskField(name, { if (created == null) name = it.take(40) }, "College Crew", focusBorder = true)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Txt("When should the question drop?", 14, FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (s in DropSlot.entries) {
                        val active = s == slot
                        Column(
                            Modifier
                                .weight(1f)
                                .height(58.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (active) c.likedBg else c.surface)
                                .border(if (active) 2.dp else 1.dp, if (active) Brand.accent else c.line, RoundedCornerShape(16.dp))
                                .tap(label = "${s.label}, ${s.time}") { if (created == null) slot = s },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Txt(s.label, 14, FontWeight.Bold)
                            Txt(s.time, 12, color = if (active) c.ink2 else c.muted)
                        }
                    }
                }
            }

            AskCard(gap = 14.dp) {
                Txt("Invite friends", 14, FontWeight.Bold)
                Txt(
                    if (created != null) "Real invites work once Askday is online. For now, sample friends have joined your group."
                    else "Your invite code appears here once the group is created.",
                    13, color = c.muted, lineHeight = 18.sp,
                )
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(c.bg).padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt("askday.app/j/${invite ?: "······"}", 15, FontWeight.Medium, if (invite != null) c.ink else c.muted, Modifier.weight(1f), maxLines = 1)
                    Box(Modifier.height(36.dp).clip(RoundedCornerShape(10.dp)).background(c.btnBg.copy(alpha = 0.45f)).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Txt("Copy", 13, FontWeight.Bold, c.btnFg)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((label, icon) in listOf("WhatsApp" to Ico.WhatsApp, "Instagram" to Ico.Instagram, "Messages" to Ico.Chat, "More" to Ico.More)) {
                        Column(
                            Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(12.dp)).background(c.bg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                        ) {
                            AskIcon(icon, c.ink.copy(alpha = 0.5f), 22.dp)
                            Txt(label, 12, FontWeight.Medium, c.ink.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            ErrorText(error)
            if (created != null) {
                AskButton("Go to today’s question") {
                    val id = created!!
                    Nav.pop()
                    Nav.push(Route.Group(id))
                }
            } else {
                AskButton("Create group", enabled = name.isNotBlank()) {
                    var id: String? = null
                    error = act { id = Engine.createGroup(it, name, slot, System.currentTimeMillis()) }
                    if (id != null) {
                        haptics(true)
                        app.askday.services.Services.log("group_created", "drop_slot" to slot.name)
                        created = id
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------- group

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupScreen(groupId: String, questionId: String?) {
    val c = colors
    val db = LocalDB.current
    val g = db.groups[groupId]
    AskScreen {
        if (g == null) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BackBtn { Nav.pop() }
                Display("This group isn’t available", 26)
                Txt("You may have left it.", 15, color = c.muted)
            }
            return@AskScreen
        }
        val qid = questionId ?: g.currentId
        val q = qid?.let { db.questions[groupId]?.get(it) }
        val me = db.me ?: ""
        val myAnswer = q?.let { db.answers[qkey(groupId, it.id)]?.firstOrNull { a -> a.uid == me } }
        val canSeeAll = myAnswer != null && q?.unlocked == true
        val isCurrent = q != null && q.id == g.currentId

        LaunchedEffect(canSeeAll, isCurrent, q?.answeredBy?.size) {
            if (canSeeAll && isCurrent && q != null) act { Engine.markSeen(it, groupId, q.id, q.answeredBy.size) }
        }

        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackBtn { Nav.pop() }
            // Demo shortcut: long-press the group name to get the next question now.
            Column(
                Modifier.weight(1f).combinedClickable(onClick = {}, onLongClick = {
                    act { Engine.dropNextNow(it, groupId, System.currentTimeMillis()) }
                    Nav.pop()
                    Nav.push(Route.Group(groupId))
                }),
            ) {
                Txt(g.name, 17, FontWeight.Bold, maxLines = 1)
                Txt(if (q != null) "Day ${q.day} · ${dayHeader(q.dropAt)}" else "Your first question is on its way", 13, color = c.muted)
            }
            FreezeChip(g.freezes)
            StreakChip(g.streak)
        }
        if (q == null) return@AskScreen
        if (canSeeAll) AnswersView(g, q, me) else QuestionView(g, q, myAnswer, me, isCurrent)
    }
}

// -------------------------------------------------------------- question

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionView(g: GroupRec, q: QuestionRec, myAnswer: Answer?, me: String, canAnswer: Boolean) {
    val c = colors
    val db = LocalDB.current
    val now = rememberNow()
    val haptics = rememberHaptics()
    var text by rememberSaveable(q.id) { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val nudged = remember { mutableStateMapOf<String, Boolean>() }
    var nudgeTarget by remember { mutableStateOf<String?>(null) }

    val answered = q.answeredBy
    val missing = g.memberIds.filter { it !in answered }
    val nudgeable = missing.filter { it != me }
    val target = nudgeTarget?.takeIf { it in nudgeable } ?: nudgeable.firstOrNull()
    val ordered = g.memberIds.sortedBy { if (it in answered) 0 else 1 }
    val atRisk = canAnswer && !q.unlocked && g.streak > 0 && missing.isNotEmpty()
    val hidden = answered.count { it != me }

    val rise = remember(q.id) { Animatable(30f) }
    LaunchedEffect(q.id) { rise.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (atRisk) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.riskBg).border(1.dp, c.riskLine, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(8.dp).shadow(6.dp, CircleShape, ambientColor = Brand.pink, spotColor = Brand.pink).clip(CircleShape).background(Brand.pink))
                Column(Modifier.weight(1f)) {
                    Txt("${g.streak}-day streak at risk.", 14, FontWeight.Bold, c.riskInk)
                    Txt(if (g.freezes > 0) "A streak freeze will save it if someone misses." else "Everyone has to answer before unlock.", 14, color = c.ink2)
                }
                if (g.freezes > 0) AskIcon(Ico.Snowflake, c.iceInk, 16.dp)
            }
        }

        GradientBox(Modifier.fillMaxWidth().graphicsLayer { translationY = rise.value }) {
            Box(Modifier.offset(x = 230.dp, y = (-40).dp).size(180.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)))
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Label(if (canAnswer) "Today’s question" else "Day ${q.day}", Brand.onHeroMuted)
                Display(q.text, 33, Color.White)
                if (!q.unlocked) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Txt("Answers unlock in", 12, FontWeight.Medium, Brand.onHeroMuted)
                            Display(formatCountdown(q.unlockAt - now), 30, Color.White)
                        }
                        if (hidden > 0) {
                            Row(
                                Modifier.height(32.dp).clip(CircleShape).background(Color(0x470B0A14)).padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                AskIcon(Ico.Lock, Color.White, 14.dp, 2.2f)
                                Txt("$hidden answer${if (hidden == 1) "" else "s"} hidden", 13, FontWeight.Bold, Color.White)
                            }
                        }
                    }
                }
            }
        }

        AskCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("${answered.size} of ${g.memberIds.size} are in", 14, FontWeight.Bold, modifier = Modifier.weight(1f))
                if (canAnswer && target != null && !q.unlocked) {
                    Row(
                        Modifier.tap(enabled = nudged[target] != true) {
                            val err = act { Engine.nudge(it, g.id, target, System.currentTimeMillis()) }
                            if (err == null) {
                                haptics(true)
                                nudged[target] = true
                                app.askday.services.Services.log("nudge_sent")
                            } else error = err
                        }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        AskIcon(Ico.Bell, c.link, 15.dp)
                        Txt(if (nudged[target] == true) "Nudged" else "Nudge ${db.person(target)?.name?.substringBefore(" ") ?: "them"}", 13, FontWeight.Bold, c.link)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                g.memberIds.forEachIndexed { i, _ ->
                    Box(
                        Modifier.weight(1f).height(6.dp).clip(CircleShape)
                            .then(if (i < answered.size) Modifier.background(Brand.hero) else Modifier.background(c.line)),
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (m in ordered.take(8)) {
                    val p = db.person(m)
                    val done = m in answered
                    val selectable = !done && m != me
                    Box(if (selectable) Modifier.tap(label = "Choose ${p?.name} to nudge", haptic = false) { nudgeTarget = m } else Modifier) {
                        Avatar(m, p?.name, p?.photo, 34.dp, done = done, dashed = !done, highlight = (!done && m == me) || (selectable && m == target), label = if (!done && m == me) "You" else null)
                    }
                }
            }
        }

        when {
            myAnswer != null -> AskCard(gap = 8.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AskIcon(Ico.Lock, c.link, 14.dp)
                    Txt("Locked in", 13, FontWeight.Bold, c.link)
                }
                Txt(myAnswer.text, 16, color = c.ink2, lineHeight = 23.sp)
            }
            canAnswer -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt("Your answer", 14, FontWeight.Bold)
                AskField(text, { text = it.take(280) }, "Be honest. Nobody sees it until they answer too.", singleLine = false, minHeight = 100.dp, radius = 18.dp, focusBorder = true, textSize = 16)
                if (text.length > 240) Txt("${280 - text.length} characters left", 12, color = c.muted, align = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }
            else -> AskCard { Txt("You didn’t answer this one, so its answers stay locked.", 15, color = c.muted) }
        }

        ErrorText(error)
        if (canAnswer && myAnswer == null) {
            AskButton("Lock in my answer", icon = Ico.Lock, enabled = text.isNotBlank()) {
                error = act { Engine.submitAnswer(it, g.id, q.id, me, text, System.currentTimeMillis()) }
                if (error == null) {
                    haptics(true)
                    app.askday.services.Services.log("answer_submitted", "day" to q.day, "length" to text.trim().length)
                }
            }
        } else if (myAnswer != null && !q.unlocked) {
            Txt(
                (if (missing.size == 1) "Waiting on 1 more person." else "Waiting on ${missing.size} more people.") + " We’ll tell you the moment answers unlock.",
                14, color = c.muted, align = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// --------------------------------------------------------------- answers

@Composable
private fun ReactionButton(kind: Reaction, count: Int, mine: Boolean, onClick: () -> Unit) {
    val c = colors
    val pop = remember { Animatable(1f) }
    var bump by remember { mutableStateOf(0) }
    LaunchedEffect(bump) {
        if (bump > 0) {
            pop.animateTo(1.4f, spring(stiffness = 900f))
            pop.animateTo(1f, spring(dampingRatio = 0.4f))
        }
    }
    val ink = if (mine) c.likedInk else c.muted
    val icon = when (kind) {
        Reaction.love -> Ico.Heart
        Reaction.haha -> Ico.Laugh
        Reaction.fire -> Ico.Flame
        Reaction.wow -> Ico.Wow
    }
    Row(
        Modifier.height(32.dp).clip(CircleShape).background(if (mine) c.likedBg else c.chip)
            .tap(label = "${kind.name}, $count", haptic = !mine) {
                if (!mine) bump++
                onClick()
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        AskIcon(icon, ink, 15.dp, filled = mine && kind == Reaction.love, modifier = Modifier.scale(pop.value))
        if (count > 0) Txt("$count", 13, FontWeight.Bold, ink, tabular = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswersView(g: GroupRec, q: QuestionRec, me: String) {
    val c = colors
    val db = LocalDB.current
    val key = qkey(g.id, q.id)
    val answers = db.answers[key].orEmpty().sortedBy { it.createdAt }
    val comments = db.comments[key].orEmpty()
    val replies = comments.filter { it.answerId != null }.groupBy { it.answerId!! }
    val chat = comments.filter { it.answerId == null }
    val open = remember { mutableStateMapOf<String, Boolean>() }
    var draft by rememberSaveable { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<Answer?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val game = db.games[key]
    val nameOf = { id: String -> if (id == me) "You" else db.person(id)?.name ?: "Friend" }
    val friendAnswers = answers.count { it.uid != me && Engine.isSample(it.uid) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (q.allAnswered && q.id == g.currentId && g.streak > 0) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .background(androidx.compose.ui.graphics.Brush.linearGradient(c.streakCard))
                        .border(1.dp, c.flameLine, RoundedCornerShape(22.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        Modifier.size(56.dp).shadow(16.dp, RoundedCornerShape(18.dp), ambientColor = Brand.flame, spotColor = Brand.flame)
                            .clip(RoundedCornerShape(18.dp)).background(Brand.flame.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { AskIcon(Ico.Flame, Brand.flame, 30.dp, filled = true) }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Display("${g.streak}", 30)
                            Txt("day streak", 14, FontWeight.Bold, c.flameInk, Modifier.padding(bottom = 4.dp))
                        }
                        Txt(if (g.streak >= g.bestStreak) "Everyone’s in. Your longest run yet." else "Everyone’s in. The streak lives.", 13, color = c.ink2)
                    }
                }
            } else {
                val (bg, fg, label) = when {
                    q.frozen -> Triple(c.iceBg, c.iceInk, "Freeze used · streak saved")
                    q.allAnswered -> Triple(c.okBg, c.okInk, "Everyone answered")
                    else -> Triple(c.chip, c.muted, "${q.answeredBy.size} of ${g.memberIds.size} answered")
                }
                Row(Modifier.height(28.dp).clip(CircleShape).background(bg).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (q.frozen) AskIcon(Ico.Snowflake, fg, 13.dp) else if (q.allAnswered) AskIcon(Ico.Check, fg, 14.dp, 2.4f)
                    Txt(label, 12, FontWeight.Bold, fg)
                }
            }

            Display(q.text, 25)

            if (friendAnswers >= 2) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(18.dp))
                        .tap(scaleTo = 0.98f) { Nav.push(Route.Game(g.id, q.id)) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GradientBox(Modifier.size(40.dp), RoundedCornerShape(13.dp)) { AskIcon(Ico.Game, Color.White, 18.dp, modifier = Modifier.align(Alignment.Center)) }
                    Column(Modifier.weight(1f)) {
                        Txt("Who said it?", 15, FontWeight.Bold)
                        Txt(if (game != null) "Your best: ${game.score} of ${game.total}. Play again?" else "Guess who wrote each answer.", 13, color = c.muted)
                    }
                }
            }

            answers.forEachIndexed { i, a ->
                RevealIn(i) { AnswerCard(g, q, a, me, replies[a.id].orEmpty(), open[a.id] == true, nameOf, onOpen = { open[a.id] = true }, onReply = { replyTo = a }, onError = { error = it }) }
            }

            if (chat.isNotEmpty()) {
                Txt("Group chat", 14, FontWeight.Bold, c.muted)
                for (m in chat) ChatBubble(m, m.uid == me, nameOf)
            }
            ErrorText(error)
        }

        Column(Modifier.fillMaxWidth().background(c.bar).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            replyTo?.let { r ->
                Row {
                    Txt("Replying to ${nameOf(r.uid)}", 13, color = c.muted, modifier = Modifier.weight(1f))
                    Txt("Cancel", 13, FontWeight.Bold, c.link, Modifier.tap { replyTo = null })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AskField(draft, { draft = it.take(280) }, replyTo?.let { "Reply to ${nameOf(it.uid)}…" } ?: "Say something to the group…", Modifier.weight(1f), radius = 24.dp, textSize = 15)
                GradientBox(
                    Modifier.size(48.dp).tap(enabled = draft.isNotBlank(), label = "Send") {
                        error = act { Engine.postComment(it, g.id, q.id, me, draft, replyTo?.id, System.currentTimeMillis()) }
                        app.askday.services.Services.log(if (replyTo != null) "reply_posted" else "chat_posted")
                        replyTo?.let { open[it.id] = true }
                        draft = ""
                        replyTo = null
                    },
                    CircleShape,
                ) { AskIcon(Ico.Send, Color.White, 20.dp, 2.2f, modifier = Modifier.align(Alignment.Center)) }
            }
        }
    }
}

/** Answers rise into place one after another the first time they appear. */
@Composable
private fun RevealIn(index: Int, content: @Composable () -> Unit) {
    val y = remember { Animatable(24f) }
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(120L + index * 110L)
        coroutineScope {
            launch { y.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
            launch { a.animateTo(1f) }
        }
    }
    Box(Modifier.graphicsLayer { translationY = y.value; alpha = a.value }) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerCard(
    g: GroupRec,
    q: QuestionRec,
    a: Answer,
    me: String,
    replies: List<Comment>,
    expanded: Boolean,
    nameOf: (String) -> String,
    onOpen: () -> Unit,
    onReply: () -> Unit,
    onError: (String) -> Unit,
) {
    val c = colors
    val db = LocalDB.current
    val p = db.person(a.uid)
    val top = q.topAnswerId == a.id
    AskCard(border = if (top) c.goldLine else c.line, padding = PaddingValues(14.dp), gap = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(a.uid, p?.name, p?.photo, 34.dp)
            Txt(nameOf(a.uid), 14, FontWeight.Bold, modifier = Modifier.weight(1f))
            if (top) {
                Box(Modifier.height(22.dp).clip(CircleShape).background(c.goldBg).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                    Txt("MOST LIKED", 11, FontWeight.Bold, c.goldInk, letterSpacing = 0.4.sp)
                }
            }
            Txt(clock(a.createdAt), 12, color = c.muted3)
        }
        Txt(a.text, 15, color = c.ink2, lineHeight = 22.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (k in Reaction.entries) {
                ReactionButton(k, a.reactions.values.count { it == k }, a.reactions[me] == k) {
                    act { Engine.toggleReaction(it, g.id, q.id, a.id, me, k) }?.let(onError)
                    app.askday.services.Services.log("reaction", "kind" to k.name)
                }
            }
            Row(
                Modifier.height(32.dp).clip(CircleShape).background(c.chip).tap { if (replies.isNotEmpty() && !expanded) onOpen() else onReply() }.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                AskIcon(Ico.Chat, c.muted, 14.dp)
                Txt(if (replies.isNotEmpty() && !expanded) "${replies.size}" else "Reply", 13, FontWeight.Bold, c.muted)
            }
        }
        if (expanded && replies.isNotEmpty()) {
            Row {
                Box(Modifier.width(2.dp).height((replies.size * 44).dp).background(c.line))
                Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (r in replies) {
                        Txt(nameOf(r.uid), 13, FontWeight.Bold)
                        Txt(r.text, 14, color = c.ink2, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(m: Comment, mine: Boolean, nameOf: (String) -> String) {
    val c = colors
    val db = LocalDB.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        if (!mine) {
            Avatar(m.uid, db.person(m.uid)?.name, db.person(m.uid)?.photo, 28.dp)
            Box(Modifier.width(8.dp))
        }
        Column(
            Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(18.dp))
                .then(if (mine) Modifier.background(Brand.hero) else Modifier.background(c.surface).border(1.dp, c.line, RoundedCornerShape(18.dp)))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (!mine) Txt(nameOf(m.uid), 12, FontWeight.Bold, c.muted)
            Txt(m.text, 15, color = if (mine) Color.White else c.ink, lineHeight = 21.sp)
        }
    }
}
