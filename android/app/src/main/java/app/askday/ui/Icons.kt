package app.askday.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// The same line icons as the design, drawn from SVG path data on a 24×24 grid.

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r},${cy}a$r,$r 0 1,0 ${2 * r},0a$r,$r 0 1,0 ${-2 * r},0"
private fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r},${y}h${w - 2 * r}a$r,$r 0 0 1 $r,$r" + "v${h - 2 * r}a$r,$r 0 0 1 -$r,$r" + "h${-(w - 2 * r)}a$r,$r 0 0 1 -$r,-$r" + "v${-(h - 2 * r)}a$r,$r 0 0 1 $r,-${r}z"

enum class Ico(vararg val paths: String) {
    Back("M15 18l-6-6 6-6"),
    Chevron("M9 6l6 6-6 6"),
    Plus("M12 5v14M5 12h14"),
    Flame("M12 3c1 3.5 5 5.5 5 10a5 5 0 0 1-10 0c0-2 1-3.5 2-4.5 0 2 1 3 2 3 0-3-1-5.5 1-8.5z"),
    Lock(rrect(5f, 11f, 14f, 10f, 2f), "M8 11V7a4 4 0 0 1 8 0v4"),
    Camera("M4 8h3l2-3h6l2 3h3v11H4z", circle(12f, 13f, 3.5f)),
    Check("M5 12l5 5 9-10"),
    Heart("M12 21s-7.5-4.6-9.5-9.3C1 8 3.4 4.5 7 4.5c2 0 3.6 1.1 5 3 1.4-1.9 3-3 5-3 3.6 0 6 3.5 4.5 7.2C19.5 16.4 12 21 12 21z"),
    Chat("M4 5h16v11H9l-5 4z"),
    Bell("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10 21h4"),
    Send("M5 12h14M13 6l6 6-6 6"),
    Groups(circle(9f, 8f, 3.5f), "M2.5 20a6.5 6.5 0 0 1 13 0", "M16 4.5a3.5 3.5 0 0 1 0 7M18 14.5a6.5 6.5 0 0 1 3.5 5.5"),
    Clock(circle(12f, 12f, 9f), "M12 7v5l3 2"),
    User(circle(12f, 8f, 4f), "M4 21a8 8 0 0 1 16 0"),
    Snowflake("M12 2v20M4.9 7l14.2 10M4.9 17L19.1 7", "M9 4l3 2 3-2M9 20l3-2 3 2M3.5 10.2l3.4-.4.9-3.3M20.5 13.8l-3.4.4-.9 3.3M3.5 13.8l3.4.4.9 3.3M20.5 10.2l-3.4-.4-.9-3.3"),
    Sun(circle(12f, 12f, 4f), "M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"),
    Moon("M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z"),
    PhoneTheme(rrect(6f, 2f, 12f, 20f, 3f), "M12 6v12"),
    Spark("M12 3l2.2 6.8L21 12l-6.8 2.2L12 21l-2.2-6.8L3 12l6.8-2.2z"),
    Crown("M3 8l4.5 4L12 5l4.5 7L21 8l-2 11H5z"),
    Brain("M9 4a3 3 0 0 0-3 3 3 3 0 0 0-2 5 3 3 0 0 0 2 5 3 3 0 0 0 6 1V5a2 2 0 0 0-3-1z", "M15 4a3 3 0 0 1 3 3 3 3 0 0 1 2 5 3 3 0 0 1-2 5 3 3 0 0 1-6 1"),
    Laugh(circle(12f, 12f, 9f), "M7.5 13.5h9a4.5 4.5 0 0 1-9 0zM8.5 9.5l1.5-1 1.5 1M12.5 9.5l1.5-1 1.5 1"),
    Wow(circle(12f, 12f, 9f), circle(9f, 9.5f, 0.6f), circle(15f, 9.5f, 0.6f), circle(12f, 15f, 2.2f)),
    Close("M6 6l12 12M18 6L6 18"),
    Game(rrect(2.5f, 7f, 19f, 11f, 5f), "M7 11v3M5.5 12.5h3M15.5 11.5h.01M18 13.5h.01"),
    Story(rrect(5f, 3f, 14f, 18f, 3f), "M10 9.5v5l4-2.5z"),
    WhatsApp("M21 12a8.5 8.5 0 0 1-12.5 7.5L3 21l1.5-5A8.5 8.5 0 1 1 21 12z"),
    Instagram(rrect(3f, 3f, 18f, 18f, 5f), circle(12f, 12f, 4f)),
    More(circle(5f, 12f, 1.5f), circle(12f, 12f, 1.5f), circle(19f, 12f, 1.5f)),
}

private val parsed = HashMap<Ico, List<Path>>()
private fun pathsFor(icon: Ico) = parsed.getOrPut(icon) { icon.paths.map { PathParser().parsePathString(it).toPath() } }

@Composable
fun AskIcon(icon: Ico, color: Color, size: Dp = 20.dp, strokeWidth: Float = 2f, filled: Boolean = false, modifier: Modifier = Modifier) {
    val paths = pathsFor(icon)
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 24f
        scale(k, k, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            for (p in paths) {
                if (filled) drawPath(p, color, style = Fill)
                drawPath(p, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
