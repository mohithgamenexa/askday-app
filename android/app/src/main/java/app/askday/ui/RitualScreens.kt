package app.askday.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.askday.data.GroupRec
import app.askday.data.Mood
import app.askday.data.Rituals
import app.askday.data.Social
import app.askday.data.act
import app.askday.data.savePhoto
import app.askday.services.Services
import java.io.File
import java.util.Calendar

// ------------------------------------------------------ the Daily section

/** Today's rituals, shown as cards on the Today screen. */
@Composable
fun DailyRituals() {
    val c = colors
    val db = LocalDB.current
    val now = rememberNow(30_000)
    val day = Social.dayKey(now)
    val haptics = rememberHaptics()
    Label("Daily")

    // Photo of the day
    val photo = db.photos[day]
    AskCard(Modifier.fillMaxWidth().tap(scaleTo = 0.98f, label = "Photo of the day") { Nav.push(Route.Photo) }, gap = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientBox(Modifier.size(44.dp), RoundedCornerShape(14.dp)) { AskIcon(Ico.Camera, Color.White, 22.dp, modifier = Modifier.align(Alignment.Center)) }
            Column(Modifier.weight(1f)) {
                Label("Photo of the day")
                Txt(Rituals.photoPrompt(day), 15, FontWeight.Bold)
            }
            if (photo != null) AskIcon(Ico.Check, c.okInk, 18.dp, 2.4f) else AskIcon(Ico.Chevron, c.muted, 16.dp)
        }
        if (photo != null) PhotoView(photo.back, Modifier.fillMaxWidth().height(140.dp))
    }

    // Mood check-in
    val mood = db.moods[day]
    AskCard(gap = 10.dp) {
        Label("How are you feeling?")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (m in Mood.entries) {
                val on = mood == m.name
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (on) Color(m.color).copy(alpha = 0.18f) else c.chip)
                        .border(if (on) 2.dp else 0.dp, if (on) Color(m.color) else Color.Transparent, RoundedCornerShape(14.dp))
                        .tap(label = m.label) {
                            act { Rituals.setMood(it, m, System.currentTimeMillis()) }
                            Services.log("mood_set", "mood" to m.name)
                        }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(18.dp).clip(CircleShape).background(Color(m.color)))
                    Txt(m.label, 11, FontWeight.Bold, if (on) c.ink else c.muted)
                }
            }
        }
        if (mood != null) {
            val g = Rituals.groupMoods(db, now)
            val total = g.values.sum().coerceAtLeast(1)
            Txt("Your friends today", 12, FontWeight.Bold, c.muted)
            Row(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)) {
                for ((m, n) in g) if (n > 0) Box(Modifier.weight(n.toFloat() / total).fillMaxSize().background(Color(m.color)))
            }
        }
    }

    // Daily challenge
    val done = db.challenges[day] != null
    AskCard(gap = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Label("Today’s challenge") }
            Txt("${Rituals.challengeStreak(db, now)}-day streak", 12, FontWeight.Bold, c.flameInk)
        }
        Txt(Rituals.challenge(day), 16, FontWeight.Bold, lineHeight = 21.sp)
        if (done) Pill("Done today", c.okBg, c.okInk)
        else AskButton("I did it", style = BtnStyle.Soft) {
            act { Rituals.completeChallenge(it, System.currentTimeMillis()) }
            haptics(true)
            Services.log("challenge_done")
        }
    }

    // This-or-that battle
    val battle = Rituals.battle(day)
    val vote = db.battles[day]
    val revealed = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= Rituals.BATTLE_REVEAL_HOUR
    AskCard(gap = 10.dp) {
        Label("Today’s battle · everyone")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((i, side) in listOf(battle.left, battle.right).withIndex()) {
                val on = vote == i
                val pct = if (i == 0) Rituals.battleLeftPercent(day) else 100 - Rituals.battleLeftPercent(day)
                Column(
                    Modifier.weight(1f).height(72.dp).clip(RoundedCornerShape(16.dp))
                        .then(if (on) Modifier.background(Brand.hero) else Modifier.background(c.chip))
                        .tap(enabled = vote == null, label = side) {
                            act { Rituals.voteBattle(it, i, System.currentTimeMillis()) }
                            haptics(false)
                            Services.log("battle_vote")
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    Txt(side, 16, FontWeight.Bold, if (on) Color.White else c.ink)
                    if (vote != null && revealed) Txt("$pct%", 13, FontWeight.Bold, if (on) Color.White else c.muted, tabular = true)
                }
            }
        }
        Txt(if (vote == null) "Pick a side. Results reveal at 9 PM." else if (revealed) "Results are in." else "Voted. Results reveal at 9 PM.", 13, color = c.muted)
    }

    // Draw it
    val doodle = db.doodles[day]
    AskCard(Modifier.fillMaxWidth().tap(scaleTo = 0.98f, label = "Draw it") { Nav.push(Route.Doodle) }, gap = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Brand.sun), contentAlignment = Alignment.Center) { AskIcon(Ico.Spark, Color.White, 22.dp) }
            Column(Modifier.weight(1f)) {
                Label("Draw it")
                Txt(Rituals.doodlePrompt(day), 15, FontWeight.Bold)
            }
            if (doodle != null) AskIcon(Ico.Check, c.okInk, 18.dp, 2.4f) else AskIcon(Ico.Chevron, c.muted, 16.dp)
        }
        if (doodle != null) PhotoView(doodle, Modifier.fillMaxWidth().height(140.dp))
    }
}

// ------------------------------------------------------- photo of the day

/** Take today's photo with the back camera, then add a front-camera shot of you. */
@Composable
fun PhotoOfDayScreen() {
    val c = colors
    val db = LocalDB.current
    val context = LocalContext.current
    val now = rememberNow(60_000)
    val day = Social.dayKey(now)
    val saved = db.photos[day]
    var back by remember { mutableStateOf(saved?.back) }
    var front by remember { mutableStateOf(saved?.front) }
    var pending by remember { mutableStateOf<Pair<String, File>?>(null) }

    fun newFile(): Pair<Uri, File> {
        val f = File(File(context.filesDir, "photos").apply { mkdirs() }, "daily-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, context.packageName + ".files", f) to f
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val p = pending
        if (ok && p != null) { if (p.first == "back") back = p.second.absolutePath else front = p.second.absolutePath }
        pending = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) savePhoto(context, uri, "daily-${System.currentTimeMillis()}")?.let { if (back == null) back = it else front = it }
    }
    val shoot = { which: String ->
        val (uri, f) = newFile()
        pending = which to f
        camera.launch(uri)
    }

    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackBtn { Nav.pop() }
                Column {
                    Txt("Photo of the day", 17, FontWeight.Bold)
                    Txt("Your view, then your face", 13, color = c.muted)
                }
            }
            Display(Rituals.photoPrompt(day), 28)
            Box(Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(24.dp)).background(c.chip), contentAlignment = Alignment.Center) {
                val b = back
                if (b == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AskIcon(Ico.Camera, c.muted, 36.dp)
                        Txt("1 · Take the photo", 15, FontWeight.Bold, c.muted)
                    }
                } else {
                    PhotoView(b, Modifier.fillMaxSize())
                    front?.let { f ->
                        Box(Modifier.align(Alignment.TopStart).padding(12.dp).size(110.dp, 140.dp).clip(RoundedCornerShape(16.dp)).border(3.dp, Color.White, RoundedCornerShape(16.dp))) {
                            PhotoView(f, Modifier.fillMaxSize())
                        }
                    }
                }
            }
            if (back == null) {
                AskButton("Take the photo", style = BtnStyle.Hero, icon = Ico.Camera) { shoot("back") }
                AskButton("Choose from gallery", style = BtnStyle.Outline) { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            } else {
                if (front == null) AskButton("2 · Add a selfie (switch to the front camera)", style = BtnStyle.Outline, icon = Ico.User) { shoot("front") }
                AskButton(if (saved == null) "Share with friends" else "Update today’s photo", style = BtnStyle.Hero) {
                    act { Rituals.savePhoto(it, back!!, front, System.currentTimeMillis()) }
                    Services.log("photo_of_day", "selfie" to (front != null))
                    Nav.pop()
                }
                Txt("Retake", 14, FontWeight.Bold, c.link, Modifier.fillMaxWidth().tap { back = null; front = null }, align = TextAlign.Center)
            }
            Label("Friends today")
            Txt("Your friends’ photos appear here once they’re on Askday.", 14, color = c.muted)
        }
    }
}

// ---------------------------------------------------------------- draw it

private class Stroke1(val color: Color, val width: Float) { val points = mutableStateListOf<Offset>() }

@Composable
fun DoodleScreen() {
    val c = colors
    val context = LocalContext.current
    val now = rememberNow(60_000)
    val day = Social.dayKey(now)
    val strokes = remember { mutableStateListOf<Stroke1>() }
    val palette = listOf(c.ink, Color(0xFF0A8A80), Color(0xFF2563EB), Color(0xFFFF6B5B), Color(0xFFFFB020), Color(0xFF10B981))
    var color by remember { mutableStateOf(palette[0]) }
    var canvasWidth by remember { mutableIntStateOf(0) }
    AskScreen {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackBtn { Nav.pop() }
                Column(Modifier.weight(1f)) {
                    Txt("Draw it", 17, FontWeight.Bold)
                    Txt(Rituals.doodlePrompt(day), 13, color = c.muted)
                }
                Txt("Undo", 14, FontWeight.Bold, c.link, Modifier.tap { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) })
            }
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)).background(Color.White).border(1.dp, c.line, RoundedCornerShape(24.dp))) {
                Canvas(
                    Modifier.fillMaxSize().pointerInput(color) {
                        canvasWidth = this.size.width
                        detectDragGestures(
                            onDragStart = { p -> strokes.add(Stroke1(color, 10f).also { it.points.add(p) }) },
                            onDrag = { change, _ -> strokes.lastOrNull()?.points?.add(change.position) },
                        )
                    },
                ) {
                    for (s in strokes) {
                        val path = androidx.compose.ui.graphics.Path()
                        s.points.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
                        drawPath(path, s.color, style = Stroke(width = s.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (p in palette) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(p).border(if (p == color) 3.dp else 0.dp, Brand.accent, CircleShape).tap(label = "Colour") { color = p })
                }
            }
            AskButton("Save doodle", style = BtnStyle.Hero, enabled = strokes.isNotEmpty()) {
                // Draw the strokes into an image file.
                val w = 1080
                val scale = if (canvasWidth > 0) w.toFloat() / canvasWidth else 1f
                val bmp = Bitmap.createBitmap(w, (w * 1.2f).toInt(), Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                for (s in strokes) {
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        this.color = s.color.toArgb(); strokeWidth = s.width * scale; style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND; strokeJoin = android.graphics.Paint.Join.ROUND
                    }
                    val path = android.graphics.Path()
                    s.points.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x * scale, p.y * scale) else path.lineTo(p.x * scale, p.y * scale) }
                    canvas.drawPath(path, paint)
                }
                val f = File(File(context.filesDir, "photos").apply { mkdirs() }, "doodle-${System.currentTimeMillis()}.png")
                f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                act { Rituals.saveDoodle(it, f.absolutePath, System.currentTimeMillis()) }
                Services.log("doodle_saved")
                Nav.pop()
            }
        }
    }
}

// -------------------------------------------------------------- group play

/** The Play tab inside a group: group trivia, and who knows the group best. */
@Composable
fun PlayView(g: GroupRec) {
    val c = colors
    val db = LocalDB.current
    val trivia = Rituals.trivia(db, g.id)
    val guesses = Rituals.guessScores(db, g.id)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AskCard(gap = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientBox(Modifier.size(44.dp), RoundedCornerShape(14.dp)) { AskIcon(Ico.Brain, Color.White, 22.dp, modifier = Modifier.align(Alignment.Center)) }
                Column(Modifier.weight(1f)) {
                    Txt("Group trivia", 16, FontWeight.Bold)
                    Txt(if (trivia.isEmpty()) "Answer a few days of questions to unlock trivia about your group." else "${trivia.size} questions about what your group said. Best: ${db.trivia[g.id] ?: 0}", 13, color = c.muted, lineHeight = 18.sp)
                }
            }
            if (trivia.isNotEmpty()) AskButton("Play trivia", style = BtnStyle.Hero) { Nav.push(Route.Trivia(g.id)) }
        }
        AskCard(gap = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AskIcon(Ico.Crown, c.goldInk, 18.dp)
                Txt("Knows the group best", 15, FontWeight.Bold)
            }
            Txt("Guess the group’s top pick when you vote on a poll. Right guesses score here.", 13, color = c.muted, lineHeight = 18.sp)
            if (guesses.isEmpty()) Txt("No correct guesses yet.", 14, color = c.muted)
            guesses.take(5).forEachIndexed { i, (uid, n) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Txt("${i + 1}", 14, FontWeight.Bold, if (i == 0) c.goldInk else c.muted, tabular = true)
                    Avatar(uid, db.person(uid)?.name, db.person(uid)?.photo, 30.dp)
                    Txt(if (uid == db.me) "You" else db.person(uid)?.name ?: "Friend", 15, FontWeight.Bold, modifier = Modifier.weight(1f))
                    Txt("$n right", 13, FontWeight.Bold, c.muted, tabular = true)
                }
            }
        }
    }
}

@Composable
fun TriviaScreen(groupId: String) {
    val c = colors
    val db = LocalDB.current
    val haptics = rememberHaptics()
    val questions = remember(groupId) { Rituals.trivia(db, groupId) }
    var index by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackBtn { Nav.pop() }
                Column {
                    Txt("Group trivia", 17, FontWeight.Bold)
                    Txt(if (index < questions.size) "Question ${index + 1} of ${questions.size} · Score $score" else "Finished", 13, color = c.muted, tabular = true)
                }
            }
            if (index >= questions.size) {
                Display("$score/${questions.size}", 56, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Txt(if (score == questions.size) "You really listen to your friends." else "Pay more attention to the group chat.", 16, color = c.muted, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                AskButton("Done") { Nav.pop() }
                return@Column
            }
            val q = questions[index]
            AskCard(padding = PaddingValues(22.dp)) { Display(q.prompt, 24) }
            q.options.forEachIndexed { i, o ->
                val right = picked != null && i == q.correct
                val wrong = picked == i && i != q.correct
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (right) c.okBg else if (wrong) c.riskBg else c.surface)
                        .border(1.5.dp, if (right) Brand.ok else if (wrong) Brand.pink else c.line2, RoundedCornerShape(16.dp))
                        .tap(enabled = picked == null) {
                            picked = i
                            if (i == q.correct) score++
                            haptics(i != q.correct)
                        }.padding(16.dp),
                ) { Txt("“$o”", 15, FontWeight.Medium) }
            }
            if (picked != null) AskButton(if (index + 1 < questions.size) "Next" else "See my score") {
                if (index + 1 >= questions.size) {
                    act { Rituals.saveTriviaScore(it, groupId, score) }
                    Services.log("trivia_done", "score" to score)
                }
                index++
                picked = null
            }
        }
    }
}
