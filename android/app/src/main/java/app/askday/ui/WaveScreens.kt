package app.askday.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.MediaRecorder
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import app.askday.data.AskType
import app.askday.data.DB
import app.askday.data.DEMO_QUESTIONS
import app.askday.data.GroupRec
import app.askday.data.Social
import app.askday.data.Waves
import app.askday.data.act
import app.askday.data.qkey
import app.askday.data.savePhoto
import app.askday.services.Services
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DAY_MS = 86_400_000L
private fun shortDate(ms: Long) = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ms))

@Composable
private fun Header(title: String, sub: String) {
    val c = colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BackBtn { Nav.pop() }
        Column {
            Txt(title, 17, FontWeight.Bold)
            Txt(sub, 13, color = c.muted)
        }
    }
}

// =============================================================== friends

/** A friend's page: friendship insights, compatibility, duo, birthday and thank-you notes. */
@Composable
fun FriendProfileScreen(uid: String) {
    val c = colors
    val db = LocalDB.current
    val p = db.person(uid)
    val f = Waves.friendship(db, uid)
    val score = Waves.compatibility(db, uid)
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Header(p?.name ?: "Friend", "Friendship")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Avatar(uid, p?.name, p?.photo, 72.dp)
                Column(Modifier.weight(1f)) {
                    Display(p?.name ?: "Friend", 28)
                    Txt("Birthday ${Waves.birthdayOf(db, uid) ?: "unknown"}", 14, color = c.muted)
                }
            }
            GradientBox(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Label("Friendship score", Brand.onHeroMuted)
                    Display("$score", 48, Color.White)
                    Txt(if (score >= 70) "You two are in sync." else if (score >= 40) "Getting closer every day." else "Answer more together to grow this.", 14, color = Color.White.copy(alpha = 0.92f))
                }
            }
            AskCard(gap = 10.dp) {
                for ((label, value) in listOf(
                    "Polls you agreed on" to "${f.agreed} of ${f.sharedPolls}",
                    "Their reactions to you" to "${f.theirReactions}",
                    "Your reactions to them" to "${f.myReactions}",
                    "Groups together" to "${f.groupsTogether}",
                )) {
                    Row { Txt(label, 15, modifier = Modifier.weight(1f)); Txt(value, 15, FontWeight.Bold, tabular = true) }
                }
            }
            AskButton("Start a duo with ${p?.name ?: "them"}", style = BtnStyle.Hero) {
                var id: String? = null
                act { id = Waves.startDuo(it, uid, System.currentTimeMillis()) }
                id?.let { Nav.push(Route.Duo(it)) }
                Services.log("duo_started")
            }
            Label("Thank-you notes")
            AskField(note, { note = it.take(200) }, "Say thanks for something…", textSize = 15)
            ErrorText(error)
            if (note.isNotBlank()) AskButton("Send note") {
                error = act { Waves.sendNote(it, uid, note, System.currentTimeMillis()) }
                if (error == null) { note = ""; Services.log("thanks_note") }
            }
            for (n in db.notes.filter { it.from == uid || it.to == uid }.sortedByDescending { it.at }) {
                AskCard(gap = 4.dp, padding = PaddingValues(14.dp)) {
                    Txt(if (n.from == db.me) "You → ${p?.name}" else "${p?.name} → you", 12, FontWeight.Bold, c.muted)
                    Txt(n.text, 15, color = c.ink2)
                }
            }
        }
    }
}

/** Duos and your friends, shown on the Friends tab. */
@Composable
fun DuosAndFriends() {
    val c = colors
    val db = LocalDB.current
    val friends = db.people.keys.filter(app.askday.data.Engine::isSample)
    if (friends.isEmpty()) return
    Label("Duos")
    if (db.duos.isEmpty()) Txt("Start a daily question with just one friend. Tap a friend below.", 14, color = c.muted)
    for (duo in db.duos) {
        val p = db.person(duo.friend)
        AskCard(Modifier.fillMaxWidth().tap(scaleTo = 0.98f) { Nav.push(Route.Duo(duo.id)) }, padding = PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(duo.friend, p?.name, p?.photo, 40.dp)
                Column(Modifier.weight(1f)) {
                    Txt("You & ${p?.name}", 15, FontWeight.Bold)
                    Txt("${Waves.compatibility(db, duo.friend)}% in sync", 13, color = c.muted)
                }
                StreakChip(duo.streak, compact = true)
            }
        }
    }
    Label("Your friends")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (f in friends) {
            val p = db.person(f)
            Column(Modifier.tap(label = p?.name) { Nav.push(Route.Friend(f)) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Avatar(f, p?.name, p?.photo, 52.dp)
                Txt(p?.name ?: "", 12, FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DuoScreen(duoId: String) {
    val c = colors
    val db = LocalDB.current
    val now = rememberNow(5_000)
    val duo = db.duos.firstOrNull { it.id == duoId }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(duoId) { act { Waves.duoToday(it, duoId, System.currentTimeMillis()) } }
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (duo == null) { BackBtn { Nav.pop() }; return@Column }
            val p = db.person(duo.friend)
            Header("You & ${p?.name}", "${duo.streak}-day duo streak · ${Waves.compatibility(db, duo.friend)}% in sync")
            val today = duo.days[Social.dayKey(now)] ?: return@Column
            GradientBox(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Label("Today’s duo question", Brand.onHeroMuted)
                    Display(DEMO_QUESTIONS[today.qIndex].text, 28, Color.White)
                }
            }
            if (today.mine == null) {
                AskField(text, { text = it.take(280) }, "Your answer…", singleLine = false, minHeight = 96.dp, radius = 18.dp, focusBorder = true, textSize = 16)
                ErrorText(error)
                AskButton("Answer", style = BtnStyle.Hero, enabled = text.isNotBlank()) {
                    error = act { Waves.answerDuo(it, duoId, text, System.currentTimeMillis()) }
                    if (error == null) Services.log("duo_answered")
                }
            } else {
                AskCard(gap = 6.dp) { Txt("You", 13, FontWeight.Bold, c.muted); Txt(today.mine ?: "", 16, color = c.ink2) }
                AskCard(gap = 6.dp) {
                    Txt(p?.name ?: "Friend", 13, FontWeight.Bold, c.muted)
                    Txt(today.theirs ?: "Waiting for ${p?.name} to answer…", 16, color = if (today.theirs == null) c.muted else c.ink2)
                }
            }
        }
    }
}

// =================================================================== play

/** Compliment poll, plans, event, the group's own reactions and the album, on the group's Play tab. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayExtras(g: GroupRec) {
    val c = colors
    val db = LocalDB.current
    val now = rememberNow(30_000)
    val context = LocalContext.current
    LaunchedEffect(g.id, Social.dayKey(now)) { act { Waves.complimentPoll(it, g.id, System.currentTimeMillis()) } }
    val poll = db.compliments[g.id]?.firstOrNull { it.day == Social.dayKey(now) }
    val me = db.me ?: ""

    // Compliment poll
    if (poll != null) {
        AskCard(gap = 10.dp, border = c.goldLine) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AskIcon(Ico.Heart, c.goldInk, 18.dp, filled = true)
                Label("Compliment poll · anonymous")
            }
            Display(poll.question, 22)
            if (!poll.votes.containsKey(me)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (m in g.memberIds.filter { it != me }) {
                        val p = db.person(m)
                        Row(
                            Modifier.clip(CircleShape).background(c.surface).border(1.5.dp, c.line2, CircleShape)
                                .tap(label = p?.name) { act { Waves.voteCompliment(it, g.id, poll.id, m) }; Services.log("compliment_vote") }
                                .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) { Avatar(m, p?.name, p?.photo, 28.dp); Txt(p?.name ?: "", 14, FontWeight.Bold) }
                    }
                }
            } else {
                val picked = Waves.pickedMe(db, poll)
                Txt(if (picked.isEmpty()) "Nobody picked you yet today. Check back later." else "${picked.size} ${if (picked.size == 1) "person" else "people"} picked you!", 16, FontWeight.Bold, c.goldInk)
                for (h in poll.hintsSeen) Txt("Hint: their name starts with “${db.person(h)?.name?.firstOrNull() ?: '?'}”", 14, color = c.muted)
                if (picked.any { it !in poll.hintsSeen }) AskButton("Reveal a hint", style = BtnStyle.Soft) { act { Waves.revealHint(it, g.id, poll.id) } }
            }
        }
    }

    // Event mode
    val event = Waves.activeEvent(db, g.id, now)
    var eventName by remember { mutableStateOf("") }
    val eventPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) savePhoto(context, uri, "event-${System.currentTimeMillis()}")?.let { path -> act { it.events[g.id]?.photos?.add(path) } }
    }
    AskCard(gap = 10.dp) {
        Label(if (event != null) "Event today" else "Event mode")
        if (event == null) {
            Txt("A trip, a wedding, a fest? Start a one-day event and collect everyone’s photos in one place.", 14, color = c.muted, lineHeight = 20.sp)
            AskField(eventName, { eventName = it.take(40) }, "Goa trip", textSize = 15)
            if (eventName.isNotBlank()) AskButton("Start event") { act { Waves.startEvent(it, g.id, eventName, System.currentTimeMillis()) }; Services.log("event_started") }
        } else {
            Display(event.name, 24)
            Txt("${event.photos.size} photos so far", 13, color = c.muted)
            AskButton("Add a photo", style = BtnStyle.Soft, icon = Ico.Camera) { eventPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        }
    }

    // Plans
    var planTitle by remember { mutableStateOf("") }
    val planOptions = remember { mutableStateListOf("", "") }
    var planError by remember { mutableStateOf<String?>(null) }
    AskCard(gap = 10.dp) {
        Label("Plans")
        for (plan in db.plans[g.id].orEmpty().sortedByDescending { it.createdAt }) {
            Txt(plan.title, 16, FontWeight.Bold)
            plan.options.forEachIndexed { i, o ->
                val n = plan.votes.values.count { it == i }
                val mine = plan.votes[me] == i
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (mine) c.likedBg else c.chip).tap { act { Waves.votePlan(it, g.id, plan.id, i) } }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt(o, 14, FontWeight.Bold, if (mine) c.likedInk else c.ink, modifier = Modifier.weight(1f))
                    Txt("$n going", 13, FontWeight.Bold, c.muted, tabular = true)
                }
            }
            val planPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) savePhoto(context, uri, "plan-${System.currentTimeMillis()}")?.let { path -> act { Waves.addPlanPhoto(it, g.id, plan.id, path) } }
            }
            Txt("Add meetup photo (${plan.photos.size})", 13, FontWeight.Bold, c.link, Modifier.tap { planPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        }
        AskField(planTitle, { planTitle = it.take(80) }, "Make a plan: Movie night?", textSize = 15)
        if (planTitle.isNotBlank()) {
            planOptions.forEachIndexed { i, o -> AskField(o, { planOptions[i] = it.take(30) }, if (i == 0) "Fri 8 PM" else "Sat 7 PM", textSize = 14) }
            ErrorText(planError)
            AskButton("Share plan") {
                planError = act { Waves.createPlan(it, g.id, planTitle, planOptions.toList(), System.currentTimeMillis()) }
                if (planError == null) { planTitle = ""; planOptions[0] = ""; planOptions[1] = ""; Services.log("plan_created") }
            }
        }
    }

    // Group reactions
    var reaction by remember { mutableStateOf("") }
    var reactionError by remember { mutableStateOf<String?>(null) }
    AskCard(gap = 10.dp) {
        Label("Your group’s own reactions")
        val list = db.customReactions[g.id].orEmpty()
        if (list.isEmpty()) Txt("Add up to 3 inside-joke reactions. They appear under every answer in this group.", 14, color = c.muted, lineHeight = 20.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { for (r in list) Pill(r, c.goldBg, c.goldInk) }
        if (list.size < 3) {
            AskField(reaction, { reaction = it.take(16) }, "Iconic", textSize = 15)
            ErrorText(reactionError)
            if (reaction.isNotBlank()) AskButton("Add reaction", style = BtnStyle.Soft) {
                reactionError = act { Waves.addCustomReaction(it, g.id, reaction) }
                if (reactionError == null) reaction = ""
            }
        }
    }

    AskButton("Group album · ${Waves.album(db, g.id).size} photos", style = BtnStyle.Outline, icon = Ico.Camera) { Nav.push(Route.Album(g.id)) }
}

@Composable
fun AlbumScreen(groupId: String) {
    val db = LocalDB.current
    val photos = Waves.album(db, groupId)
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Header("Album", "${db.groups[groupId]?.name ?: ""} · ${photos.size} photos")
            if (photos.isEmpty()) Txt("Photos from answers, plans and events collect here.", 15, color = colors.muted)
            for (row in photos.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((path, _) in row) Box(Modifier.weight(1f).aspectRatio(1f)) { PhotoView(path, Modifier.fillMaxSize()) }
                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Practice groups: watch a rewarded ad to earn a streak freeze (once a day, up to 2 saved). */
@Composable
fun RewardedFreezeCard(g: GroupRec) {
    val c = colors
    val db = LocalDB.current
    val activity = LocalContext.current as? android.app.Activity
    val today = Social.dayKey(System.currentTimeMillis())
    if (g.cloud || g.freezes >= 2 || db.adFreezeDay[g.id] == today || !app.askday.services.Ads.rewardedOn) return
    LaunchedEffect(Unit) { activity?.let { app.askday.services.Ads.preloadRewarded(it) } }
    AskCard(gap = 8.dp, border = c.iceLine) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AskIcon(Ico.Snowflake, c.iceInk, 18.dp)
            Txt("Earn a streak freeze", 15, FontWeight.Bold)
        }
        Txt("Watch a short ad and your group gets a freeze that protects the streak if someone misses a day.", 13, color = c.muted, lineHeight = 18.sp)
        AskButton("Watch an ad", style = BtnStyle.Soft) {
            app.askday.services.Ads.showRewarded(activity) {
                act { d -> d.groups[g.id]?.let { it.freezes = minOf(2, it.freezes + 1) }; d.adFreezeDay[g.id] = today }
            }
        }
    }
}

/** The group goal for this month, shown on the Board tab. */
@Composable
fun GroupGoalCard(g: GroupRec) {
    val c = colors
    val db = LocalDB.current
    val (done, target) = Waves.groupGoal(db, g.id, System.currentTimeMillis())
    AskCard(gap = 8.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Label("Group goal this month") }
            Txt("$done / $target answers", 13, FontWeight.Bold, c.muted, tabular = true)
        }
        Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(c.chip)) {
            Box(Modifier.fillMaxWidth((done.toFloat() / target).coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(Brand.hero))
        }
        Txt(if (done >= target) "Goal reached! Everyone earns the month’s badge." else "Answer together to unlock this month’s group badge.", 13, color = c.muted)
    }
}

// ============================================================ reactions

/** The group's own reactions plus a 3-second voice reaction, under an answer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExtraReactions(groupId: String, questionId: String, answerId: String) {
    val c = colors
    val db = LocalDB.current
    val context = LocalContext.current
    val key = qkey(groupId, questionId) + "/" + answerId
    val custom = db.customReactions[groupId].orEmpty()
    val votes = db.customVotes[key].orEmpty()
    val voices = db.voiceReactions[key].orEmpty()
    var recording by remember { mutableStateOf(false) }
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(recording) {
        if (!recording) return@LaunchedEffect
        val f = File(File(context.filesDir, "voice").apply { mkdirs() }, "react-${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        val ok = runCatching {
            r.setAudioSource(MediaRecorder.AudioSource.MIC); r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); r.setOutputFile(f.absolutePath); r.prepare(); r.start()
            delay(3000); r.stop()
        }.isSuccess
        r.release()
        if (ok) { act { Waves.setVoiceReaction(it, key, f.absolutePath) }; Services.log("voice_reaction") }
        recording = false
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (label in custom) {
            val n = votes.values.count { it == label }
            val on = votes[db.me] == label
            Box(
                Modifier.height(30.dp).clip(CircleShape).background(if (on) c.goldBg else c.chip).tap(label = label) { act { Waves.toggleCustomVote(it, key, label) } }.padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Txt(if (n > 0) "$label $n" else label, 12, FontWeight.Bold, if (on) c.goldInk else c.muted) }
        }
        Row(
            Modifier.height(30.dp).clip(CircleShape).background(if (recording) c.riskBg else c.chip).tap(enabled = !recording, label = "Voice reaction") {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) recording = true
                else mic.launch(Manifest.permission.RECORD_AUDIO)
            }.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AskIcon(Ico.Bell, if (recording) c.riskInk else c.muted, 13.dp)
            Txt(if (recording) "Recording 3s…" else "Voice", 12, FontWeight.Bold, if (recording) c.riskInk else c.muted)
        }
        for ((uid, path) in voices) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt(if (uid == db.me) "You" else db.person(uid)?.name ?: "", 11, FontWeight.Bold, c.muted)
                VoicePill(path, 3000)
            }
        }
    }
}

// ================================================================ music

/** Shows an answer's text, or a song card for "Song: … by …" answers. */
@Composable
fun AnswerText(text: String) {
    val c = colors
    if (text.isEmpty()) return
    if (text.startsWith("Song: ")) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.likedBg).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GradientBox(Modifier.size(44.dp), RoundedCornerShape(12.dp)) { AskIcon(Ico.Spark, Color.White, 20.dp, modifier = Modifier.align(Alignment.Center)) }
            Column(Modifier.weight(1f)) {
                Label("Song")
                Txt(text.removePrefix("Song: "), 15, FontWeight.Bold, c.likedInk)
            }
        }
    } else Txt(text, 15, color = c.ink2, lineHeight = 22.sp)
}

/** "Answer with a song": title and artist become the answer text. */
@Composable
fun SongPicker(onPick: (String) -> Unit) {
    val c = colors
    var open by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    if (!open) {
        Txt("Answer with a song instead", 13, FontWeight.Bold, c.link, Modifier.tap { open = true })
        return
    }
    AskField(title, { title = it.take(60) }, "Song title", textSize = 15)
    AskField(artist, { artist = it.take(60) }, "Artist", textSize = 15)
    if (title.isNotBlank()) AskButton("Use this song", style = BtnStyle.Soft) {
        onPick("Song: ${title.trim()}" + if (artist.isNotBlank()) " by ${artist.trim()}" else "")
        open = false
        Services.log("song_answer")
    }
}

// ==================================================================== me

/** Level, season, status and links to the memory features, on the Me tab. */
@Composable
fun MeExtras() {
    val c = colors
    val db = LocalDB.current
    val now = System.currentTimeMillis()
    val xp = Waves.xp(db)
    val season = Waves.season(now)
    var status by remember { mutableStateOf(db.status ?: "") }
    AskCard(gap = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Brand.hero), contentAlignment = Alignment.Center) { Display("${Waves.level(xp)}", 22, Color.White) }
            Column(Modifier.weight(1f)) {
                Txt("Level ${Waves.level(xp)}", 16, FontWeight.Bold)
                Txt("$xp XP · ${100 - xp % 100} to the next level", 13, color = c.muted)
            }
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(c.chip)) {
            Box(Modifier.fillMaxWidth(Waves.levelProgress(xp)).fillMaxHeight().clip(CircleShape).background(Brand.hero))
        }
    }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brand.sun).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Label("This month’s season", Color.White.copy(alpha = 0.9f))
            Txt(season.name, 17, FontWeight.Bold, Color.White)
            Txt("${season.blurb} Featured pack: ${season.pack}.", 13, color = Color.White.copy(alpha = 0.92f))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Txt("Your status", 14, FontWeight.Bold)
        AskField(status, { status = it.take(60); val v = status; act { d -> d.status = v.ifBlank { null } } }, "Currently… studying for exams", textSize = 15)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Txt("Show when I’m online", 15, FontWeight.Medium)
            Txt("Friends see “Online now” or when you were last active.", 12, color = c.muted)
        }
        AskToggle(db.showOnline, "Show when I’m online") { on ->
            act { it.showOnline = on }
            app.askday.services.FriendsCloud.setShowOnline(on)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MeLink("Insights", Ico.Spark, Route.Insights, Modifier.weight(1f))
        MeLink("Time capsule", Ico.Clock, Route.Capsule, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MeLink("Year in Askday", Ico.Story, Route.Year, Modifier.weight(1f))
        val context = LocalContext.current
        Row(
            Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(16.dp))
                .tap { exportBook(context, db) }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) { AskIcon(Ico.Send, Brand.accent, 18.dp); Txt("Make a book", 15, FontWeight.Bold) }
    }
}

@Composable
private fun MeLink(label: String, icon: Ico, route: Route, modifier: Modifier) {
    val c = colors
    Row(
        modifier.height(52.dp).clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(16.dp)).tap { Nav.push(route) }.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { AskIcon(icon, Brand.accent, 18.dp); Txt(label, 15, FontWeight.Bold) }
}

/** A small banner for this month's season, on Today. */
@Composable
fun SeasonBanner() {
    val c = colors
    val s = Waves.season(System.currentTimeMillis())
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.goldBg).border(1.dp, c.goldLine, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AskIcon(Ico.Crown, c.goldInk, 18.dp)
        Txt("Season: ${s.name} · ${s.blurb}", 13, FontWeight.Bold, c.goldInk, modifier = Modifier.weight(1f))
    }
}

// ========================================================= wave 4 screens

@Composable
fun InsightsScreen() {
    val c = colors
    val db = LocalDB.current
    val i = Waves.weeklyInsights(db, System.currentTimeMillis())
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Header("Your week", "Private to you · worked out on your phone")
            GradientBox(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label("This week", Brand.onHeroMuted)
                    Display("${i.answers} answers", 34, Color.White)
                    Txt(
                        buildString {
                            i.topMood?.let { append("Mostly ${it.label.lowercase()} moods. ") }
                            i.topWord?.let { append("You mentioned “$it” ${i.topWordCount} times. ") }
                            i.topReactor?.let { append("${db.person(it)?.name ?: "A friend"} reacted to you the most.") }
                            if (isEmpty()) append("Answer a few questions this week and your insights appear here.")
                        },
                        15, color = Color.White.copy(alpha = 0.94f), lineHeight = 22.sp,
                    )
                }
            }
            AskCard(gap = 10.dp) {
                for ((label, value) in listOf("Mood check-ins" to "${i.moodDays} of 7 days", "Challenges done" to "${i.challenges}", "Photos of the day" to "${i.photos}")) {
                    Row { Txt(label, 15, modifier = Modifier.weight(1f)); Txt(value, 15, FontWeight.Bold, tabular = true) }
                }
            }
            Txt("AI-written insights arrive with the online version.", 13, color = c.muted)
        }
    }
}

@Composable
fun CapsuleScreen() {
    val c = colors
    val db = LocalDB.current
    val now = rememberNow(60_000)
    var text by remember { mutableStateOf("") }
    var attach by remember { mutableStateOf(Attachment()) }
    var days by remember { mutableIntStateOf(30) }
    var error by remember { mutableStateOf<String?>(null) }
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Header("Time capsule", "A message to your future self")
            AskField(text, { text = it.take(500) }, "Dear future me…", singleLine = false, minHeight = 120.dp, radius = 18.dp, focusBorder = true, textSize = 16)
            AttachRow(attach) { attach = it }
            Segmented(listOf(Triple(30, "1 month", null), Triple(182, "6 months", null), Triple(365, "1 year", null)), days) { days = it }
            ErrorText(error)
            AskButton("Seal the capsule", style = BtnStyle.Hero, enabled = text.isNotBlank() || attach.voice != null) {
                error = act { Waves.createCapsule(it, text, attach.voice, days, System.currentTimeMillis()) }
                if (error == null) { text = ""; attach = Attachment(); Services.log("capsule_sealed", "days" to days) }
            }
            for (cap in db.capsules.sortedBy { it.opensAt }) {
                val open = now >= cap.opensAt
                AskCard(gap = 6.dp, border = if (open) c.goldLine else c.line) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AskIcon(if (open) Ico.Spark else Ico.Lock, if (open) c.goldInk else c.muted, 16.dp)
                        Txt(if (open) "Opened · written ${shortDate(cap.createdAt)}" else "Opens ${shortDate(cap.opensAt)}", 13, FontWeight.Bold, if (open) c.goldInk else c.muted)
                    }
                    if (open) {
                        if (cap.text.isNotEmpty()) Txt(cap.text, 16, color = c.ink2, lineHeight = 22.sp)
                        cap.voice?.let { VoicePill(it, 0) }
                    } else Txt("Sealed. No peeking.", 15, color = c.muted)
                }
            }
        }
    }
}

/** Year in Askday, story style. */
@Composable
fun YearScreen() {
    val db = LocalDB.current
    val y = Waves.yearStats(db, System.currentTimeMillis())
    var i by remember { mutableIntStateOf(0) }
    val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    val slides = listOf(
        Triple("Your $year in Askday", "${y.answers}", "answers this year."),
        Triple("Longest streak", "${y.bestStreak}", "days in a row with your people."),
        Triple("Photos of the day", "${y.photos}", "moments captured."),
        Triple("Asks you posted", "${y.asks}", "questions to your groups."),
        Triple("Your most loved answer", "“${y.topAnswer ?: "Keep answering"}”", "the one everyone reacted to."),
        Triple("Your mood", y.topMood?.label ?: "—", "was your most common mood. Same time next year?"),
    )
    LaunchedEffect(i) { delay(5000); if (i + 1 < slides.size) i++ else Nav.pop() }
    val breakActivity = LocalContext.current as? android.app.Activity
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { app.askday.services.Ads.atBreak(breakActivity) } }
    Box(Modifier.fillMaxSize().background(Brand.hero).clickable(remember { MutableInteractionSource() }, null) { if (i + 1 < slides.size) i++ else Nav.pop() }) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { for (n in slides.indices) Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(if (n <= i) Color.White else Color.White.copy(alpha = 0.35f))) }
            Box(Modifier.weight(1f))
            val (label, big, rest) = slides[i]
            Label(label, Brand.onHeroMuted)
            Display(big, if (big.length > 12) 34 else 80, Color.White)
            Txt(rest, 18, color = Color.White.copy(alpha = 0.92f))
            Box(Modifier.weight(1f))
            Txt("Tap to continue", 13, color = Brand.onHeroMuted, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Lays out your year of answers and photos as a PDF book and opens the share sheet. */
fun exportBook(context: Context, d: DB) {
    val me = d.me ?: return
    val pdf = PdfDocument()
    val w = 595
    val h = 842
    val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f; isFakeBoldText = true; color = 0xFF0E1A1C.toInt() }
    val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; color = 0xFF24363A.toInt() }
    val small = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = 0xFF56676B.toInt() }
    fun layout(t: String, p: TextPaint) = StaticLayout.Builder.obtain(t, 0, t.length, p, w - 96).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

    var pageNo = 0
    var page = pdf.startPage(PdfDocument.PageInfo.Builder(w, h, ++pageNo).create())
    var y = 60f
    val cover = layout("${d.profile?.name ?: "My"} · A year on Askday", TextPaint(title).apply { textSize = 30f })
    page.canvas.save(); page.canvas.translate(48f, 300f); cover.draw(page.canvas); page.canvas.restore()
    pdf.finishPage(page)

    val entries = mutableListOf<Triple<String, String, String?>>()
    for ((day, text) in d.momentLog.toSortedMap()) entries += Triple(d.momentQuestions[day] ?: "Askday moment · $day", text, d.photos[day]?.back)
    for ((key, list) in d.answers) for (a in list) if (a.uid == me) {
        val (gid, qid) = key.split("/").let { it[0] to it.getOrElse(1) { "" } }
        entries += Triple(d.questions[gid]?.get(qid)?.text ?: "Question", a.text.ifEmpty { "(photo or voice answer)" }, a.photo)
    }
    page = pdf.startPage(PdfDocument.PageInfo.Builder(w, h, ++pageNo).create())
    for ((q, a, photo) in entries) {
        val ql = layout(q, title.apply { textSize = 16f })
        val al = layout(a, body)
        val need = ql.height + al.height + 40 + if (photo != null) 220 else 0
        if (y + need > h - 48) {
            pdf.finishPage(page)
            page = pdf.startPage(PdfDocument.PageInfo.Builder(w, h, ++pageNo).create())
            y = 60f
        }
        page.canvas.save(); page.canvas.translate(48f, y); ql.draw(page.canvas); page.canvas.translate(0f, ql.height + 8f); al.draw(page.canvas); page.canvas.restore()
        y += ql.height + al.height + 24
        if (photo != null) runCatching {
            val bmp = BitmapFactory.decodeFile(photo, BitmapFactory.Options().apply { inSampleSize = 8 })
            if (bmp != null) {
                val scale = minOf(200f / bmp.height, (w - 96f) / bmp.width)
                page.canvas.drawBitmap(bmp, null, android.graphics.RectF(48f, y, 48f + bmp.width * scale, y + bmp.height * scale), null)
                y += bmp.height * scale + 16
            }
        }
    }
    page.canvas.drawText("Made with Askday", 48f, h - 30f, small)
    pdf.finishPage(page)
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "askday-book.pdf")
    file.outputStream().use { pdf.writeTo(it) }
    pdf.close()
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Share your Askday book"))
    Services.log("book_exported", "entries" to entries.size)
}

/** Ask helper suggestions under the question box. */
@Composable
fun AskHelperHints(question: String, type: AskType, options: List<String>) {
    val c = colors
    val hints = Waves.askHelper(question, type, options)
    if (hints.isEmpty()) return
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.likedBg).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AskIcon(Ico.Spark, c.likedInk, 14.dp)
            Txt("Ask helper", 12, FontWeight.Bold, c.likedInk)
        }
        for (h in hints) Txt(h, 13, color = c.likedInk, lineHeight = 18.sp)
    }
}

/** On a member's birthday, a card in the group with a shareable birthday card. */
@Composable
fun BirthdayCard(g: GroupRec) {
    val db = LocalDB.current
    val context = LocalContext.current
    val who = Waves.birthdayInGroup(db, g.id, System.currentTimeMillis()) ?: return
    val name = if (who == db.me) "you" else db.person(who)?.name ?: "a friend"
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brand.sun).tap {
            shareAnswerCard(context, "Happy birthday${if (who == db.me) "" else ", $name"}!", g.name, "From all of us at ${g.name}")
        }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AskIcon(Ico.Spark, Color.White, 20.dp)
        Column(Modifier.weight(1f)) {
            Txt("It’s ${if (who == db.me) "your" else "$name’s"} birthday!", 15, FontWeight.Bold, Color.White)
            Txt("Tap to share a birthday card", 13, color = Color.White.copy(alpha = 0.92f))
        }
    }
}
