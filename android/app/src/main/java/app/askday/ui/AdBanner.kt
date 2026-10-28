package app.askday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.askday.services.AdIds
import app.askday.services.Services
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * A banner ad in a card labeled "Sponsored". Shows only when the Remote Config switch
 * "ads_enabled" is on and the person's consent allows ads.
 */
@Composable
fun AdBanner() {
    val enabled by Services.adsEnabled.collectAsStateWithLifecycle()
    val ready by Services.adsReady.collectAsStateWithLifecycle()
    if (!enabled || !ready) return
    val c = colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(22.dp)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Label("Sponsored")
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdIds.BANNER
                    loadAd(AdRequest.Builder().build())
                }
            },
        )
    }
}
