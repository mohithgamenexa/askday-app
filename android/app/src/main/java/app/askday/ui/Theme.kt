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
    bg = Color(0xFFF7F6FB), glow = Color(0xFFE6E0FF), ink = Color(0xFF14121F), ink2 = Color(0xFF2E2A40),
    muted = Color(0xFF5E5A72), muted3 = Color(0xFF6A6680), surface = Color.White, line = Color(0xFFE6E3EF),
    line2 = Color(0xFFD6D2E4), chip = Color(0xFFF2F0F7), dash = Color(0xFFA9A4BD), iconBtn = Color.White,
    iconBtnLine = Color(0xFFE6E3EF), btnBg = Color(0xFF14121F), btnFg = Color.White, bar = Color(0xF5FFFFFF),
    inputBg = Color.White, link = Color(0xFF4B3BFF), likedBg = Color(0xFFEEEBFF), likedInk = Color(0xFF3326D6),
    flameBg = Color(0xFFFFF1E6), flameLine = Color(0xFFFFD2B0), flameInk = Color(0xFFB84A00),
    iceBg = Color(0xFFE8F4FF), iceLine = Color(0xFFBFE0FF), iceInk = Color(0xFF0B5FA8),
    riskBg = Color(0xFFFFEEF3), riskLine = Color(0xFFFFC9D8), riskInk = Color(0xFFB3134D),
    okBg = Color(0xFFE3F7EE), okInk = Color(0xFF0B6B3A), goldBg = Color(0xFFFFF6DB), goldInk = Color(0xFF7A5500),
    goldLine = Color(0xFFF2D27A), danger = Color(0xFFB42318), overlay = Color(0x8C14121F),
    streakCard = listOf(Color(0xFFFFF0E3), Color(0xFFFFE8EF)),
)

val DarkColors = AskColors(
    dark = true,
    bg = Color(0xFF0B0A14), glow = Color(0xFF2A1F5C), ink = Color(0xFFF4F2FF), ink2 = Color(0xFFE1DCEF),
    muted = Color(0xFFA39FB8), muted3 = Color(0xFF8C88A3), surface = Color(0xFF15131F), line = Color(0xFF2A2640),
    line2 = Color(0xFF3A3460), chip = Color(0xFF1E1B2C), dash = Color(0xFF5E5878), iconBtn = Color(0x0FFFFFFF),
    iconBtnLine = Color(0x14FFFFFF), btnBg = Color.White, btnFg = Color(0xFF0B0A14), bar = Color(0xF515131F),
    inputBg = Color(0xFF15131F), link = Color(0xFFA99BFF), likedBg = Color(0xFF2A2050), likedInk = Color(0xFFC6BCFF),
    flameBg = Color(0x24FF8A3D), flameLine = Color(0x59FF8A3D), flameInk = Color(0xFFFFB47A),
    iceBg = Color(0x1F60B4FF), iceLine = Color(0x5960B4FF), iceInk = Color(0xFF8CCBFF),
    riskBg = Color(0x1AFF5E8A), riskLine = Color(0x4DFF5E8A), riskInk = Color(0xFFFF9DB8),
    okBg = Color(0x1F3DDC97), okInk = Color(0xFF6FE9B4), goldBg = Color(0x24FFD36B), goldInk = Color(0xFFFFD36B),
    goldLine = Color(0x59FFD36B), danger = Color(0xFFFF8A80), overlay = Color(0x99000000),
    streakCard = listOf(Color(0x38FF8A3D), Color(0x29FF5E8A)),
)

val LocalColors = compositionLocalOf { LightColors }

/** Colors that look the same in both themes. */
object Brand {
    val accent = Color(0xFF6C5CFF)
    val violet = Color(0xFFA24DFF)
    val pink = Color(0xFFFF5E8A)
    val flame = Color(0xFFFF8A3D)
    val ok = Color(0xFF3DDC97)
    val onHeroMuted = Color(0xD1FFFFFF)
    val hero = Brush.linearGradient(listOf(accent, violet, pink), start = Offset(0f, 0f), end = Offset(900f, 900f))
    val avatar = listOf(0xFFFFD9C2, 0xFFD6E4FF, 0xFFD9F2E3, 0xFFF7D6F0, 0xFFFFF0B3, 0xFFE2DCFF).map { Color(it) }
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
