package app.askday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The bottom bar. Every tab keeps a fixed slot, so nothing moves when you tap:
 * the selected tab gets a teal pill behind its icon and a bold label.
 */
@Composable
fun AskTabBar(route: Route) {
    val c = colors
    val tabs = listOf(
        Triple(Route.Today, "Today", Ico.Sun),
        Triple(Route.Friends, "Friends", Ico.Groups),
        Triple(Route.Groups, "Groups", Ico.Chat),
        Triple(Route.Discover, "Discover", Ico.Spark),
        Triple(Route.Me, "Me", Ico.User),
    )
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(14.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(c.bar)
                .border(1.dp, c.line, RoundedCornerShape(24.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for ((r, label, icon) in tabs) {
                val active = r == route || (r == Route.Me && route == Route.History)
                Column(
                    Modifier.weight(1f).fillMaxHeight().tap(label = label, scaleTo = 0.92f) { if (r != route) Nav.tab(r) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Box(
                        Modifier.size(52.dp, 32.dp).clip(CircleShape).then(if (active) Modifier.background(Brand.hero) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { AskIcon(icon, if (active) Color.White else c.muted, 20.dp) }
                    Txt(label, 11, if (active) FontWeight.Bold else FontWeight.Medium, if (active) c.ink else c.muted, maxLines = 1)
                }
            }
        }
    }
}
