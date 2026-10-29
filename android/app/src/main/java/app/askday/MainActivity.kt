package app.askday

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.askday.data.Engine
import app.askday.data.Store
import app.askday.notify.Notifier
import app.askday.services.Services
import app.askday.ui.AskdayApp
import app.askday.ui.Nav
import app.askday.ui.Route
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val askPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Services.init(applicationContext)
        Store.init(applicationContext)
        Notifier.createChannel(applicationContext)
        Store.onChange = {
            Notifier.reschedule(applicationContext, it)
            app.askday.notify.AskdayWidget.update(applicationContext, it)
            app.askday.services.Cloud.syncProfile(it)
        }

        // Keep the clock running while the app is open: drops, friends' answers, unlocks.
        lifecycleScope.launch {
            while (true) {
                if (Store.ready.value) Store.update {
                    val now = System.currentTimeMillis()
                    val a = Engine.tick(it, now)
                    val b = app.askday.data.Social.tick(it, now)
                    a || b
                }
                delay(3000)
            }
        }
        // Ads start only when the Remote Config switch "ads_enabled" is on.
        lifecycleScope.launch {
            Services.adsEnabled.collect { on -> if (on) Services.startAds(this@MainActivity) }
        }
        openFromNotification(intent)

        setContent {
            val db by Store.db.collectAsStateWithLifecycle()
            val dark = db.theme == app.askday.data.ThemeChoice.dark ||
                (db.theme == app.askday.data.ThemeChoice.system && (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES)
            LaunchedEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            // Notifications are asked for during onboarding, with a reason, rather than at launch.
            AskdayApp(askNotifications = {
                if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            })
        }
    }

    // Online status: active while Askday is on screen, away when it isn't.
    override fun onStart() {
        super.onStart()
        app.askday.services.FriendsCloud.setActive(true)
    }

    override fun onStop() {
        super.onStop()
        app.askday.services.FriendsCloud.setActive(false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFromNotification(intent)
    }

    private fun openFromNotification(intent: Intent?) {
        val groupId = intent?.getStringExtra("groupId") ?: return
        Nav.tab(Route.Today)
        if (groupId.isNotEmpty()) Nav.push(Route.Group(groupId))
    }
}
