package app.askday.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ------------------------------------------------------------------- text

@Composable
fun Txt(
    text: String,
    size: Int = 15,
    weight: FontWeight = FontWeight.Normal,
    color: Color = colors.ink,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    tabular: Boolean = false,
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            fontFamily = AskFonts.body,
            fontWeight = weight,
            fontSize = size.sp,
            color = color,
            textAlign = align ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            letterSpacing = letterSpacing,
            fontFeatureSettings = if (tabular) "tnum" else null,
        ),
    )
}

@Composable
fun Display(text: String, size: Int = 32, color: Color = colors.ink, modifier: Modifier = Modifier, align: TextAlign? = null, maxLines: Int = Int.MAX_VALUE) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            fontFamily = AskFonts.display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = size.sp,
            lineHeight = (size * 1.06f).sp,
            letterSpacing = (-0.03).em,
            color = color,
            textAlign = align ?: TextAlign.Unspecified,
            fontFeatureSettings = "tnum",
        ),
    )
}

@Composable
fun Label(text: String, color: Color = colors.muted) =
    Txt(text.uppercase(), size = 12, weight = FontWeight.Bold, color = color, letterSpacing = 1.2.sp)

// ----------------------------------------------------------------- layout

/** A full screen with the soft glow at the top, kept clear of the status and navigation bars. */
@Composable
fun AskScreen(bottomBar: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val c = colors
    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(c.glow, c.bg),
                        center = Offset(size.width / 2, -size.height * 0.1f),
                        radius = size.height * 0.55f,
                    ),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .then(if (bottomBar) Modifier.navigationBarsPadding() else Modifier),
            content = content,
        )
    }
}

@Composable
fun AskCard(
    modifier: Modifier = Modifier,
    border: Color = colors.line,
    glow: Boolean = false,
    padding: PaddingValues = PaddingValues(16.dp),
    gap: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .then(if (glow) Modifier.shadow(18.dp, shape, ambientColor = Brand.accent, spotColor = Brand.accent) else Modifier)
            .clip(shape)
            .background(colors.surface)
            .then(
                if (glow) Modifier.border(BorderStroke(1.5.dp, Brand.hero), shape)
                else Modifier.border(1.dp, border, shape),
            )
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

// -------------------------------------------------------------- pressable

/** Springs down slightly when touched, with a light haptic tap. */
@Composable
fun Modifier.tap(enabled: Boolean = true, label: String? = null, haptic: Boolean = true, scaleTo: Float = 0.97f, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) scaleTo else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "tap")
    val feedback = LocalHapticFeedback.current
    return this
        .scale(scale)
        .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button) {
            if (haptic) feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

@Composable
fun rememberHaptics(): (strong: Boolean) -> Unit {
    val feedback = LocalHapticFeedback.current
    return { strong -> feedback.performHapticFeedback(if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove) }
}

enum class BtnStyle { Primary, Hero, Outline, Soft }

@Composable
fun AskButton(
    label: String,
    modifier: Modifier = Modifier,
    style: BtnStyle = BtnStyle.Primary,
    icon: Ico? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = colors
    val shape = RoundedCornerShape(18.dp)
    val fg = when (style) {
        BtnStyle.Primary -> c.btnFg
        BtnStyle.Hero -> Color.White
        else -> c.ink
    }
    Row(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .then(if (style == BtnStyle.Hero) Modifier.shadow(16.dp, shape, ambientColor = Brand.violet, spotColor = Brand.violet) else Modifier)
            .clip(shape)
            .then(
                when (style) {
                    BtnStyle.Primary -> Modifier.background(c.btnBg)
                    BtnStyle.Hero -> Modifier.background(Brand.hero)
                    BtnStyle.Outline -> Modifier.background(c.surface).border(1.5.dp, c.line2, shape)
                    BtnStyle.Soft -> Modifier.background(c.chip)
                },
            )
            .then(if (enabled) Modifier else Modifier.background(c.bg.copy(alpha = 0.45f)))
            .tap(enabled = enabled, label = label, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) AskIcon(icon, fg, 18.dp, 2.2f)
        Txt(label, 16, FontWeight.Bold, fg)
    }
}

@Composable
fun IconBtn(icon: Ico, label: String, solid: Boolean = false, size: Dp = 46.dp, onClick: () -> Unit) {
    val c = colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(if (solid) c.btnBg else c.iconBtn)
            .then(if (solid) Modifier else Modifier.border(1.dp, c.iconBtnLine, shape))
            .tap(label = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AskIcon(icon, if (solid) c.btnFg else c.ink, 22.dp, if (icon == Ico.Plus) 2.4f else 2f)
    }
}

@Composable
fun BackBtn(onClick: () -> Unit) = IconBtn(Ico.Back, "Back", size = 44.dp, onClick = onClick)

@Composable
fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(bg).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Txt(text, 13, FontWeight.Bold, fg, maxLines = 1)
    }
}

/** A row of options where one is chosen, like Light / Dark / Phone. */
@Composable
fun <T> Segmented(options: List<Triple<T, String, Ico?>>, value: T, onChange: (T) -> Unit) {
    val c = colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.chip).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for ((id, label, icon) in options) {
            val active = id == value
            val fg = if (active) c.ink else c.muted
            Row(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .then(if (active) Modifier.shadow(4.dp, RoundedCornerShape(12.dp)) else Modifier)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) c.surface else Color.Transparent)
                    .tap(label = label) { onChange(id) },
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) AskIcon(icon, fg, 16.dp)
                Txt(label, 14, if (active) FontWeight.Bold else FontWeight.Medium, fg)
            }
        }
    }
}

// ------------------------------------------------------------------ inputs

@Composable
fun AskField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: Dp = 56.dp,
    radius: Dp = 16.dp,
    focusBorder: Boolean = false,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    textSize: Int = 17,
) {
    val c = colors
    val shape = RoundedCornerShape(radius)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = singleLine,
        keyboardOptions = keyboard,
        cursorBrush = SolidColor(Brand.accent),
        textStyle = TextStyle(fontFamily = AskFonts.body, fontSize = textSize.sp, color = c.ink, lineHeight = (textSize * 1.4f).sp),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.inputBg)
            .border(1.5.dp, if (focusBorder && value.isNotEmpty()) Brand.accent else c.line2, shape)
            .semantics { contentDescription = placeholder },
        decorationBox = { inner ->
            Box(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp).then(if (singleLine) Modifier else Modifier.heightIn(min = minHeight - 28.dp)),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty()) Txt(placeholder, textSize, color = c.muted3)
                inner()
            }
        },
    )
}

@Composable
fun AskToggle(value: Boolean, label: String, onChange: (Boolean) -> Unit) {
    val c = colors
    val x by animateDpAsState(if (value) 20.dp else 0.dp, tween(180), label = "toggle")
    Box(
        Modifier
            .size(50.dp, 30.dp)
            .clip(CircleShape)
            .background(if (value) Brand.hero else SolidColor(c.line2))
            .tap(label = label) { onChange(!value) }
            .padding(3.dp),
    ) {
        Box(Modifier.offset(x = x).size(24.dp).clip(CircleShape).background(Color.White))
    }
}

// ------------------------------------------------------------------ avatars

fun initials(name: String?): String {
    val words = name.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "${words[0][0]}${words[1][0]}".uppercase()
    }
}

fun avatarColor(id: String): Color {
    var h = 0
    for (ch in id) h = h * 31 + ch.code
    return Brand.avatar[Math.floorMod(h, Brand.avatar.size)]
}

@Composable
fun rememberPhoto(path: String?): ImageBitmap? = remember(path) {
    path?.let { p ->
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(p, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 1080 || bounds.outHeight / (sample * 2) >= 1440) sample *= 2
            BitmapFactory.decodeFile(p, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
        }.getOrNull()
    }
}

@Composable
fun Avatar(
    id: String,
    name: String?,
    photo: String? = null,
    size: Dp = 32.dp,
    done: Boolean = false,
    dashed: Boolean = false,
    highlight: Boolean = false,
    label: String? = null,
    bg: Color? = null,
    ring: Boolean = false,
) {
    val c = colors
    val image = rememberPhoto(photo)
    Box(Modifier.size(size).semantics { contentDescription = name ?: "" }) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .then(
                    if (dashed) Modifier.drawBehind {
                        drawCircle(
                            if (highlight) c.link else c.dash,
                            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))),
                            radius = this.size.minDimension / 2 - 1.dp.toPx(),
                        )
                    } else Modifier.background(bg ?: avatarColor(id)),
                )
                .then(if (ring) Modifier.border(2.dp, c.surface, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null && photo.startsWith("http") && !dashed) {
                // Real people's photos come from the server.
                coil.compose.AsyncImage(photo, null, Modifier.size(size), contentScale = ContentScale.Crop)
            } else if (image != null && !dashed) {
                Image(image, null, Modifier.size(size), contentScale = ContentScale.Crop)
            } else {
                Txt(
                    label ?: initials(name),
                    maxOf(10, (size.value * 0.34f).toInt()),
                    FontWeight.Bold,
                    if (dashed) (if (highlight) c.link else c.muted) else Brand.avatarInk,
                )
            }
        }
        if (done) {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(14.dp).clip(CircleShape).background(c.surface).padding(2.dp)
                    .clip(CircleShape).background(Brand.ok),
            )
        }
    }
}

@Composable
fun AvatarStack(people: List<Triple<String, String?, String?>>, max: Int = 3, size: Dp = 28.dp) {
    val c = colors
    Row {
        people.take(max).forEachIndexed { i, (id, name, photo) ->
            Box(Modifier.offset(x = (-8 * i).dp)) { Avatar(id, name, photo, size, ring = true) }
        }
        val extra = people.size - max
        if (extra > 0) {
            Box(
                Modifier.offset(x = (-8 * max).dp).size(size).clip(CircleShape).background(c.line).border(2.dp, c.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Txt("+$extra", 10, FontWeight.Bold) }
        }
    }
}

// -------------------------------------------------------------------- bits

/** The flame. Pulses gently while the streak is alive. */
@Composable
fun StreakChip(streak: Int, compact: Boolean = false) {
    val c = colors
    val pulse by rememberInfiniteTransition(label = "flame").animateFloat(1f, if (streak > 0) 1.12f else 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
    val ink = if (streak > 0) c.flameInk else c.muted
    if (compact) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.semantics { contentDescription = "$streak-day streak" }) {
            AskIcon(Ico.Flame, if (streak > 0) Brand.flame else c.muted, 15.dp, filled = streak > 0)
            Txt("$streak", 14, FontWeight.Bold, ink, tabular = true)
        }
        return
    }
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .height(36.dp)
            .then(if (streak > 0) Modifier.shadow(10.dp, shape, ambientColor = Brand.flame, spotColor = Brand.flame) else Modifier)
            .clip(shape)
            .background(c.flameBg)
            .border(1.dp, c.flameLine, shape)
            .padding(horizontal = 12.dp)
            .semantics { contentDescription = "$streak-day streak" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        AskIcon(Ico.Flame, Brand.flame, 16.dp, filled = true, modifier = Modifier.scale(pulse))
        Txt("$streak", 15, FontWeight.Bold, ink, tabular = true)
    }
}

@Composable
fun FreezeChip(count: Int) {
    if (count <= 0) return
    val c = colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.height(36.dp).clip(shape).background(c.iceBg).border(1.dp, c.iceLine, shape).padding(horizontal = 10.dp)
            .semantics { contentDescription = "$count streak freeze${if (count == 1) "" else "s"} saved" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AskIcon(Ico.Snowflake, c.iceInk, 15.dp)
        Txt("$count", 14, FontWeight.Bold, c.iceInk)
    }
}

@Composable
fun ErrorText(message: String?) {
    if (message != null) Txt(message, 14, FontWeight.Medium, colors.danger)
}

@Composable
fun GradientBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(28.dp), content: @Composable BoxScope.() -> Unit) {
    Box(modifier.shadow(24.dp, shape, ambientColor = Brand.violet, spotColor = Brand.violet).clip(shape).background(Brand.hero), content = content)
}

@Composable
fun Gap(h: Dp) = Box(Modifier.height(h))

@Composable
fun RowScope.Spacer1() = Box(Modifier.weight(1f))

/** The current time, updated every second (or as often as you ask). */
@Composable
fun rememberNow(everyMs: Long = 1000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(everyMs) {
        while (true) {
            delay(everyMs)
            now = System.currentTimeMillis()
        }
    }
    return now
}

fun formatCountdown(ms: Long): String {
    val s = maxOf(0L, ms / 1000)
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

fun formatLeft(ms: Long): String {
    val m = maxOf(0L, Math.round(ms / 60000.0))
    return if (m < 60) "${m}m" else "${m / 60}h %02dm".format(m % 60)
}

fun shortLeft(ms: Long): String {
    val m = maxOf(0L, Math.round(ms / 60000.0))
    if (m < 60) return "${m}m"
    val h = m / 60
    return if (h >= 10) "${h}h" else "${h}h %02dm".format(m % 60)
}
