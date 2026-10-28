package app.askday.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.askday.data.DB
import app.askday.data.GroupRec
import app.askday.data.Profile
import app.askday.data.QuestionRec
import app.askday.data.Store
import app.askday.data.ThemeChoice
import app.askday.data.Engine
import app.askday.data.act
import androidx.compose.runtime.setValue

sealed interface Route {
    data object Today : Route
    data object Friends : Route
    data object Groups : Route
    data object Discover : Route
    data object History : Route
    data object Me : Route
    data object Memories : Route
    data object Plus : Route
    data class NewAsk(val groupId: String) : Route
    data class AskResults(val groupId: String, val askId: String) : Route
    data object EditProfile : Route
    data object NewGroup : Route
    data object ManageGroups : Route
    data class Group(val id: String, val q: String? = null, val tab: String? = null) : Route
    data class Game(val id: String, val q: String) : Route
    data class Recap(val id: String) : Route
}

/** A simple screen stack. The three tabs sit at the bottom of it. */
object Nav {
    val stack = mutableStateListOf<Route>(Route.Today)
    fun push(r: Route) { stack.add(r) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun tab(r: Route) { stack.clear(); stack.add(r) }
}

val LocalDB = androidx.compose.runtime.staticCompositionLocalOf { DB() }

// Small helpers the screens share.
fun DB.groupsSorted(): List<GroupRec> = groups.values.filter { g -> me != null && me in g.memberIds }.sortedByDescending { it.lastActivityAt }
fun DB.current(g: GroupRec): QuestionRec? = g.currentId?.let { questions[g.id]?.get(it) }
fun DB.person(id: String): Profile? = if (profile?.id == id) profile else people[id]

@Composable
fun AskdayApp(askNotifications: () -> Unit = {}) {
    val db by Store.db.collectAsStateWithLifecycle()
    val ready by Store.ready.collectAsStateWithLifecycle()
    val dark = when (db.theme) {
        ThemeChoice.dark -> true
        ThemeChoice.system -> isSystemInDarkTheme()
        ThemeChoice.light -> false
    }
    val c = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalColors provides c, LocalDB provides db) {
        Box(Modifier.fillMaxSize().background(c.bg)) {
            if (!ready) return@Box
            // Signed in means: Firebase knows this phone number, and the saved data belongs to it.
            val uid = app.askday.services.PhoneAuth.currentUid
            androidx.compose.runtime.LaunchedEffect(uid, db.me) {
                if (uid != null && db.me != uid) act { Engine.signIn(it, uid, app.askday.services.PhoneAuth.currentPhone ?: "") }
                app.askday.services.Services.setUser(uid)
            }
            // Before sign-in: hook → answer first → phone. After: name and photo → username and birthday → friends → vibe.
            var step by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("hook") }
            when {
                uid == null || db.me != uid -> when (step) {
                    "answer" -> {
                        BackHandler { step = "hook" }
                        AnswerFirstScreen(onBack = { step = "hook" }, onSaved = { step = "phone" })
                    }
                    "phone" -> {
                        BackHandler { step = "hook" }
                        PhoneLoginScreen(onBack = { step = "hook" })
                    }
                    else -> HookScreen(onAnswer = { step = "answer" }, onHaveAccount = { step = "phone" })
                }
                db.profile == null -> SetupScreen(isNew = true)
                db.profile?.username == null -> DetailsScreen()
                !db.onboarded -> if (step == "vibe") VibeScreen(askNotifications) else FindFriendsScreen(onNext = { step = "vibe" })
                db.profile == null -> SetupScreen(isNew = true)
                else -> {
                    val route = Nav.stack.last()
                    BackHandler(enabled = Nav.stack.size > 1) { Nav.pop() }
                    AnimatedContent(route, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "nav") { r ->
                        when (r) {
                            Route.Today -> TodayScreen()
                            Route.Friends -> FriendsScreen()
                            Route.Groups -> GroupsTabScreen()
                            Route.Discover -> DiscoverScreen()
                            Route.Memories -> MemoriesScreen()
                            Route.Plus -> PlusScreen()
                            is Route.NewAsk -> NewAskScreen(r.groupId)
                            is Route.AskResults -> AskResultsScreen(r.groupId, r.askId)
                            Route.History -> HistoryScreen()
                            Route.Me -> ProfileScreen()
                            Route.EditProfile -> SetupScreen(isNew = false)
                            Route.NewGroup -> NewGroupScreen()
                            Route.ManageGroups -> ManageGroupsScreen()
                            is Route.Group -> GroupScreen(r.id, r.q, r.tab)
                            is Route.Game -> GameScreen(r.id, r.q)
                            is Route.Recap -> RecapScreen(r.id)
                        }
                    }
                    if (route in listOf(Route.Today, Route.Friends, Route.Groups, Route.Discover, Route.Me, Route.History)) TabBar(route)
                    CelebrationOverlay()
                }
            }
        }
    }
}
