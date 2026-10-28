package app.askday.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import app.askday.data.savePhoto
import app.askday.services.Services
import kotlinx.coroutines.delay
import java.io.File

/** A photo and/or voice note someone is attaching to an answer. */
class Attachment(val photo: String? = null, val voice: String? = null, val voiceMs: Long = 0) {
    val isEmpty get() = photo == null && voice == null
}

private const val MAX_VOICE_MS = 30_000L

/** "Photo" and "Voice" buttons under an answer box, with a preview of what's attached. */
@Composable
fun AttachRow(value: Attachment, onChange: (Attachment) -> Unit) {
    val c = colors
    val context = LocalContext.current
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordFile by remember { mutableStateOf<File?>(null) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) savePhoto(context, uri, "answer-${System.currentTimeMillis()}")?.let { onChange(Attachment(it, value.voice, value.voiceMs)) }
    }

    fun stop() {
        val r = recorder ?: return
        val ms = System.currentTimeMillis() - startedAt
        runCatching { r.stop() }
        r.release()
        recorder = null
        val f = recordFile
        if (f != null && ms >= 800) onChange(Attachment(value.photo, f.absolutePath, ms)) else error = "Hold on a little longer to record."
    }

    fun start() {
        error = null
        val f = File(File(context.filesDir, "voice").apply { mkdirs() }, "voice-${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setMaxDuration(MAX_VOICE_MS.toInt())
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            recordFile = f
            startedAt = System.currentTimeMillis()
            Services.log("voice_recording_started")
        } catch (e: Exception) {
            r.release()
            error = "Couldn’t start recording."
            Services.recordError(e)
        }
    }

    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else error = "Allow the microphone to record a voice note."
    }

    LaunchedEffect(recorder) {
        while (recorder != null) {
            elapsed = System.currentTimeMillis() - startedAt
            if (elapsed >= MAX_VOICE_MS) stop()
            delay(200)
        }
    }
    DisposableEffect(Unit) { onDispose { recorder?.let { runCatching { it.stop() }; it.release() } } }

    if (value.photo != null) {
        Box {
            PhotoView(value.photo, Modifier.fillMaxWidth().heightIn(max = 220.dp))
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp).clip(CircleShape).background(Color(0x99000000))
                    .tap(label = "Remove photo") { onChange(Attachment(null, value.voice, value.voiceMs)) },
                contentAlignment = Alignment.Center,
            ) { AskIcon(Ico.Close, Color.White, 16.dp) }
        }
    }
    if (value.voice != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoicePill(value.voice, value.voiceMs)
            Txt("Remove", 13, FontWeight.Bold, c.link, Modifier.tap { onChange(Attachment(value.photo, null, 0)) })
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(Ico.Camera, if (value.photo == null) "Add photo" else "Change photo") {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        val recording = recorder != null
        Chip(if (recording) Ico.Lock else Ico.Bell, when {
            recording -> "Stop · ${elapsed / 1000}s"
            value.voice != null -> "Re-record"
            else -> "Voice note"
        }, active = recording) {
            when {
                recording -> stop()
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> start()
                else -> mic.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    ErrorText(error)
}

@Composable
private fun Chip(icon: Ico, label: String, active: Boolean = false, onClick: () -> Unit) {
    val c = colors
    Row(
        Modifier.height(40.dp).clip(CircleShape).background(if (active) c.riskBg else c.surface)
            .border(1.5.dp, if (active) c.riskLine else c.line2, CircleShape).tap(label = label, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AskIcon(icon, if (active) c.riskInk else c.ink, 16.dp)
        Txt(label, 14, FontWeight.Bold, if (active) c.riskInk else c.ink)
    }
}

/** A saved photo, cropped to fit, with rounded corners. */
@Composable
fun PhotoView(path: String?, modifier: Modifier = Modifier) {
    val image = rememberPhoto(path) ?: return
    Image(image, "Photo answer", modifier.clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
}

/** Tap to play a voice note. */
@Composable
fun VoicePill(path: String, ms: Long) {
    val c = colors
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(path) { onDispose { player?.release() } }
    val playing = player != null
    Row(
        Modifier.height(40.dp).clip(CircleShape).background(c.likedBg).tap(label = if (playing) "Stop voice note" else "Play voice note") {
            val p = player
            if (p != null) {
                p.release()
                player = null
            } else if (File(path).exists()) {
                player = MediaPlayer().apply {
                    setDataSource(path)
                    setOnCompletionListener { it.release(); player = null }
                    prepare()
                    start()
                }
            }
        }.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AskIcon(if (playing) Ico.Close else Ico.Send, c.likedInk, 16.dp, 2.2f)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            for (h in listOf(8, 14, 10, 18, 12, 16, 9, 13)) Box(Modifier.size(3.dp, h.dp).clip(CircleShape).background(c.likedInk))
        }
        Txt("${(ms / 1000).coerceAtLeast(1)}s", 13, FontWeight.Bold, c.likedInk)
    }
}

/** Shows any photo or voice note attached to an answer. */
@Composable
fun AnswerMedia(photo: String?, voice: String?, voiceMs: Long) {
    if (photo != null) PhotoView(photo, Modifier.fillMaxWidth().heightIn(max = 260.dp))
    if (voice != null) VoicePill(voice, voiceMs)
}

// ------------------------------------------------------------ share cards

/** Draws an answer as a story-sized image (1080 × 1920) and opens the share sheet. */
fun shareAnswerCard(context: Context, question: String, name: String, answer: String) {
    val w = 1080
    val h = 1920
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val bg = Paint().apply { shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), intArrayOf(0xFF0A8A80.toInt(), 0xFF0E7FA8.toInt(), 0xFF2563EB.toInt()), null, Shader.TileMode.CLAMP) }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)
    canvas.drawCircle(w - 80f, 160f, 300f, Paint().apply { color = 0x1FFFFFFF })

    val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    fun layout(text: String, size: Float, color: Int, typeface: Typeface, width: Int) =
        StaticLayout.Builder.obtain(text, 0, text.length, TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; this.typeface = typeface }, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.1f).build()

    val pad = 96f
    val brand = layout("Askday", 60f, 0xFFFFFFFF.toInt(), bold, w)
    canvas.save(); canvas.translate(pad, 140f); brand.draw(canvas); canvas.restore()
    val q = layout(question, 76f, 0xFFFFFFFF.toInt(), bold, (w - 2 * pad).toInt())
    canvas.save(); canvas.translate(pad, 420f); q.draw(canvas); canvas.restore()

    val cardTop = 420f + q.height + 90f
    val a = layout("“$answer”", 58f, 0xFF0E1A1C.toInt(), Typeface.DEFAULT, (w - 2 * pad - 120).toInt())
    val by = layout(name, 44f, 0xFF56676B.toInt(), bold, (w - 2 * pad - 120).toInt())
    val cardH = a.height + by.height + 200f
    canvas.drawRoundRect(RectF(pad, cardTop, w - pad, cardTop + cardH), 60f, 60f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
    canvas.save(); canvas.translate(pad + 60, cardTop + 70); a.draw(canvas); canvas.translate(0f, a.height + 40f); by.draw(canvas); canvas.restore()

    val footer = layout("Answer today’s question on Askday", 44f, 0xE6FFFFFF.toInt(), bold, w)
    canvas.save(); canvas.translate(pad, h - 220f); footer.draw(canvas); canvas.restore()

    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "askday-${System.currentTimeMillis()}.png")
    file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, "Share your answer"))
    Services.log("share_card")
}
