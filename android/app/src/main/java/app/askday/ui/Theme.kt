package app.askday.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.askday.R

/** The two color sets, matching the design. */
data class AskColors(
    val dark: Boolean,
    val bg: Color,
    val glow: Color,
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val muted3: Color,
    val surface: Color,
    val line: Color,
    val line2: Color,
    val chip: Color,
    val dash: Color,
    val iconBtn: Color,
    val iconBtnLine: Color,
    val btnBg: Color,
    val btnFg: Color,
    val bar: Color,
    val inputBg: Color,
    val link: Color,
    val likedBg: Color,
    val likedInk: Color,
    val flameBg: Color,
    val flameLine: Color,
    val flameInk: Color,
    val iceBg: Color,
    val iceLine: Color,
    val iceInk: Color,
    val riskBg: Color,
    val riskLine: Color,
    val riskInk: Color,
    val okBg: Color,
    val okInk: Color,
    val goldBg: Color,
    val goldInk: Color,
    val goldLine: Color,
    val danger: Color,
    val overlay: Color,
    val streakCard: List<Color>,
)

val LightColors = AskColors(
    dark = false,
    bg = Color(0xFFF5FAF9), glow = Color(0xFFCDEFEA), ink = Color(0xFF0E1A1C), ink2 = Color(0xFF24363A),
    muted = Color(0xFF56676B), muted3 = Color(0xFF66777A), surface = Color.White, line = Color(0xFFE1ECEA),
    line2 = Color(0xFFCFDEDB), chip = Color(0xFFEEF5F4), dash = Color(0xFF9DB3B0), iconBtn = Color.White,
    iconBtnLine = Color(0xFFE1ECEA), btnBg = Color(0xFF0E1A1C), btnFg = Color.White, bar = Color(0xF5FFFFFF),
    inputBg = Color.White, link = Color(0xFF0A7C73), likedBg = Color(0xFFDDF6F1), likedInk = Color(0xFF08655E),
    flameBg = Color(0xFFFFF1E6), flameLine = Color(0xFFFFD2B0), flameInk = Color(0xFFB84A00),
    iceBg = Color(0xFFE8F4FF), iceLine = Color(0xFFBFE0FF), iceInk = Color(0xFF0B5FA8),
    riskBg = Color(0xFFFFEFEC), riskLine = Color(0xFFFFCFC7), riskInk = Color(0xFFB4261A),
    okBg = Color(0xFFE3F7EE), okInk = Color(0xFF0B6B3A), goldBg = Color(0xFFFFF6DB), goldInk = Color(0xFF7A5500),
    goldLine = Color(0xFFF2D27A), danger = Color(0xFFB42318), overlay = Color(0x8C0E1A1C),
    streakCard = listOf(Color(0xFFFFF0E3), Color(0xFFFFE8EF)),
)

val DarkColors = AskColors(
    dark = true,
    bg = Color(0xFF061214), glow = Color(0xFF0B3B38), ink = Color(0xFFEFFAF8), ink2 = Color(0xFFD3E6E3),
    muted = Color(0xFF93A9A6), muted3 = Color(0xFF7F9592), surface = Color(0xFF0E1C1E), line = Color(0xFF1C3134),
    line2 = Color(0xFF29464A), chip = Color(0xFF132628), dash = Color(0xFF4E6B6E), iconBtn = Color(0x0FFFFFFF),
    iconBtnLine = Color(0x14FFFFFF), btnBg = Color(0xFFEFFAF8), btnFg = Color(0xFF061214), bar = Color(0xF50E1C1E),
    inputBg = Color(0xFF0E1C1E), link = Color(0xFF5EEAD4), likedBg = Color(0xFF0F3B37), likedInk = Color(0xFF7FF0DE),
    flameBg = Color(0x24FF8A3D), flameLine = Color(0x59FF8A3D), flameInk = Color(0xFFFFB47A),
    iceBg = Color(0x1F60B4FF), iceLine = Color(0x5960B4FF), iceInk = Color(0xFF8CCBFF),
    riskBg = Color(0x1FFF6B5B), riskLine = Color(0x59FF6B5B), riskInk = Color(0xFFFF9C90),
    okBg = Color(0x1F3DDC97), okInk = Color(0xFF6FE9B4), goldBg = Color(0x24FFD36B), goldInk = Color(0xFFFFD36B),
    goldLine = Color(0x59FFD36B), danger = Color(0xFFFF8A80), overlay = Color(0x99000000),
    streakCard = listOf(Color(0x38FF8A3D), Color(0x29FF5E8A)),
)

val LocalColors = compositionLocalOf { LightColors }

/** Colors that look the same in both themes. */
object Brand {
    // Ocean: teal into blue, with a warm coral for the stakes.
    val accent = Color(0xFF0A8A80)
    val violet = Color(0xFF0E7FA8)
    val pink = Color(0xFFFF6B5B)
    val blue = Color(0xFF2563EB)
    val sun = Brush.linearGradient(listOf(Color(0xFFFFB020), Color(0xFFFF7A59)))
    val flame = Color(0xFFFF8A3D)
    val ok = Color(0xFF3DDC97)
    val onHeroMuted = Color(0xD1FFFFFF)
    val hero = Brush.linearGradient(listOf(accent, violet, blue), start = Offset(0f, 0f), end = Offset(900f, 900f))
    val avatar = listOf(0xFFFFD9C2, 0xFFD6E4FF, 0xFFD9F2E3, 0xFFF7D6F0, 0xFFFFF0B3, 0xFFCFEFEA).map { Color(it) }
    val avatarInk = Color(0xFF1B1830)
}

/** Bundled variable fonts: Bricolage Grotesque for headlines, DM Sans for everything else. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
object AskFonts {
    val display = FontFamily(
        Font(R.font.bricolage, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    )
    val body = FontFamily(
        Font(R.font.dmsans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.dmsans, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(R.font.dmsans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    )
}

val colors: AskColors
    @Composable get() = LocalColors.current
