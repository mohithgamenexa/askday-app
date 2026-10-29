package app.askday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.askday.services.CloudUser
import app.askday.services.FriendsCloud
import app.askday.services.Services
import kotlinx.coroutines.delay

/** A real Askday person in a list, with an online dot and an action button. */
@Composable
fun PersonRow(u: CloudUser, subtitle: String?, online: Boolean = false, action: @Composable () -> Unit) {
    val c = colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box {
            Avatar(u.uid, u.name, null, 42.dp)
            if (online) Box(Modifier.align(Alignment.BottomEnd).size(13.dp).clip(CircleShape).background(c.surface).padding(2.dp).clip(CircleShape).background(Brand.ok))
        }
        Column(Modifier.weight(1f)) {
            Txt(u.name.ifBlank { "Askday user" }, 15, FontWeight.Bold, maxLines = 1)
            Txt(subtitle ?: (u.username?.let { "@$it" } ?: ""), 13, color = if (online) c.okInk else c.muted, maxLines = 1)
        }
        action()
    }
}

@Composable
fun SmallButton(label: String, solid: Boolean = true, onClick: () -> Unit) {
    val c = colors
    Box(
        Modifier.height(32.dp).clip(CircleShape).background(if (solid) c.btnBg else c.chip).tap(label = label, onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Txt(label, 13, FontWeight.Bold, if (solid) c.btnFg else c.ink) }
}

/** Friend requests and real friends on the Friends tab. */
@Composable
fun RealFriendsSection() {
    if (!Services.firebaseReady) return
    val c = colors
    val friends by FriendsCloud.friends.collectAsStateWithLifecycle()
    val incoming by FriendsCloud.incoming.collectAsStateWithLifecycle()
    val presence by FriendsCloud.presence.collectAsStateWithLifecycle()
    val now = rememberNow(30_000)
    if (incoming.isNotEmpty()) {
        Label("Friend requests · ${incoming.size}")
        AskCard(gap = 14.dp) {
            for (u in incoming) PersonRow(u, "wants to be friends") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallButton("Accept") { FriendsCloud.accept(u.uid) }
                    SmallButton("Ignore", solid = false) { FriendsCloud.decline(u.uid) }
                }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { Label(if (friends.isEmpty()) "Real friends" else "Real friends · ${friends.size}") }
        Txt("Add friends", 13, FontWeight.Bold, c.link, Modifier.tap { Nav.push(Route.AddFriends) })
    }
    if (friends.isEmpty()) {
        AskCard(gap = 10.dp) {
            Txt("Find your people on Askday", 15, FontWeight.Bold)
            Txt("Search by username, or see which of your contacts are already here.", 13, color = c.muted)
            AskButton("Add friends", style = BtnStyle.Soft, icon = Ico.Plus) { Nav.push(Route.AddFriends) }
        }
    } else {
        val sorted = friends.sortedByDescending { presence[it.uid]?.online == true }
        AskCard(gap = 14.dp) {
            for (u in sorted) {
                val p = presence[u.uid]
                PersonRow(u, FriendsCloud.presenceLabel(p, now) ?: u.username?.let { "@$it" }, online = p?.online == true) {}
            }
        }
    }
}

/** Search by username and see contacts on Askday. */
@Composable
fun AddFriendsScreen() {
    val c = colors
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CloudUser>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val friends by FriendsCloud.friends.collectAsStateWithLifecycle()
    val outgoing by FriendsCloud.outgoing.collectAsStateWithLifecycle()
    val incoming by FriendsCloud.incoming.collectAsStateWithLifecycle()
    LaunchedEffect(query) {
        if (query.trim().length < 2) { results = emptyList(); return@LaunchedEffect }
        delay(350) // wait until they stop typing
        searching = true
        FriendsCloud.search(query) { results = it; searching = false }
    }
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackBtn { Nav.pop() }
                Column {
                    Txt("Add friends", 17, FontWeight.Bold)
                    Txt("Search, or find people from your contacts", 13, color = c.muted)
                }
            }
            AskField(query, { query = it.take(20) }, "Search usernames, like aarav.k", focusBorder = true)
            if (searching) Txt("Searching…", 13, color = c.muted)
            if (results.isNotEmpty()) AskCard(gap = 14.dp) {
                for (u in results) PersonRow(u, null) { FriendAction(u.uid, friends.any { it.uid == u.uid }, u.uid in outgoing, incoming.any { it.uid == u.uid }) }
            } else if (query.trim().length >= 2 && !searching) Txt("No one found with that username.", 14, color = c.muted)
            Label("From your contacts")
            ContactsList()
        }
    }
}

/** Add / Requested / Accept / Friends, depending on where you stand with someone. */
@Composable
fun FriendAction(uid: String, isFriend: Boolean, requested: Boolean, theyAsked: Boolean) {
    val c = colors
    when {
        isFriend -> Pill("Friends", c.okBg, c.okInk)
        theyAsked -> SmallButton("Accept") { FriendsCloud.accept(uid) }
        requested -> SmallButton("Requested", solid = false) { FriendsCloud.cancel(uid) }
        else -> SmallButton("Add") { FriendsCloud.sendRequest(uid) }
    }
}

/** Shown in the contacts list: your contacts who already use Askday. */
@Composable
fun ContactMatches(numbers: List<String>, nameOf: (String) -> String) {
    val c = colors
    if (!Services.firebaseReady) return
    var matches by remember { mutableStateOf<Map<String, CloudUser>?>(null) }
    val friends by FriendsCloud.friends.collectAsStateWithLifecycle()
    val outgoing by FriendsCloud.outgoing.collectAsStateWithLifecycle()
    val incoming by FriendsCloud.incoming.collectAsStateWithLifecycle()
    val presence by FriendsCloud.presence.collectAsStateWithLifecycle()
    LaunchedEffect(numbers.size) { FriendsCloud.matchContacts(numbers) { matches = it } }
    AskCard(gap = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AskIcon(Ico.Spark, Brand.accent, 18.dp)
            Txt(when (val m = matches) { null -> "Checking who’s on Askday…"; else -> if (m.isEmpty()) "None of your contacts are on Askday yet" else "${m.size} of your contacts are on Askday" }, 15, FontWeight.Bold)
        }
        Txt("Only scrambled versions of the numbers are checked. Your contacts never leave your phone.", 12, color = c.muted)
        for ((number, u) in matches.orEmpty()) {
            PersonRow(u.copy(name = nameOf(number).ifBlank { u.name }), FriendsCloud.presenceLabel(presence[u.uid]) ?: u.username?.let { "@$it" }, online = presence[u.uid]?.online == true) {
                FriendAction(u.uid, friends.any { it.uid == u.uid }, u.uid in outgoing, incoming.any { it.uid == u.uid })
            }
        }
    }
}
