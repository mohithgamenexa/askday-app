package app.askday.services

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

/**
 * Ad unit IDs. These are Google's official TEST IDs, which always show safe sample ads.
 * Replace them with your own from the AdMob console before publishing.
 */
object AdUnits {
    const val BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
}

/**
 * Every ad format, each with its own Remote Config switch (all need "ads_enabled" on too):
 * ads_banner, ads_native, ads_interstitial, ads_rewarded,
 * plus interstitial_cooldown_minutes (default 3) and interstitial_daily_cap (default 3).
 */
object Ads {
    val DEFAULTS = mapOf<String, Any>(
        "ads_banner" to true, "ads_native" to true, "ads_interstitial" to true, "ads_rewarded" to true,
        "interstitial_cooldown_minutes" to 3L, "interstitial_daily_cap" to 3L,
    )

    private fun rc(key: String) = if (Services.firebaseReady) FirebaseRemoteConfig.getInstance().getBoolean(key) else false
    private val live get() = Services.adsEnabled.value && Services.adsReady.value

    val bannerOn get() = live && rc("ads_banner")
    val nativeOn get() = live && rc("ads_native")
    val interstitialOn get() = live && rc("ads_interstitial")
    val rewardedOn get() = live && rc("ads_rewarded")

    // ------------------------------------------------------------ interstitial

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var lastShown = 0L
    private var shownToday = 0
    private var shownDay = ""

    fun preloadInterstitial(context: Context) {
        if (!interstitialOn || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(context.applicationContext, AdUnits.INTERSTITIAL, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; loadingInterstitial = false }
            override fun onAdFailedToLoad(error: LoadAdError) { loadingInterstitial = false }
        })
    }

    /**
     * Shows a full-screen ad at a natural break (after a game result, after closing a recap),
     * but no more than once every few minutes and a few times a day.
     */
    fun atBreak(activity: Activity?) {
        activity ?: return
        if (!interstitialOn) return
        val now = System.currentTimeMillis()
        val day = app.askday.data.Social.dayKey(now)
        if (day != shownDay) { shownDay = day; shownToday = 0 }
        val cooldown = FirebaseRemoteConfig.getInstance().getLong("interstitial_cooldown_minutes").coerceAtLeast(1) * 60_000
        val cap = FirebaseRemoteConfig.getInstance().getLong("interstitial_daily_cap")
        val ad = interstitial
        if (ad == null) { preloadInterstitial(activity); return }
        if (now - lastShown < cooldown || shownToday >= cap) return
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { preloadInterstitial(activity) }
        }
        lastShown = now
        shownToday++
        ad.show(activity)
        Services.log("ad_interstitial_shown")
    }

    // ---------------------------------------------------------------- rewarded

    private var rewarded: RewardedAd? = null
    private var loadingRewarded = false

    fun preloadRewarded(context: Context) {
        if (!rewardedOn || rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(context.applicationContext, AdUnits.REWARDED, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; loadingRewarded = false }
            override fun onAdFailedToLoad(error: LoadAdError) { loadingRewarded = false }
        })
    }

    val rewardedReady get() = rewardedOn && rewarded != null

    /** Shows a rewarded ad; [onReward] runs only if the person watched it through. */
    fun showRewarded(activity: Activity?, onReward: () -> Unit) {
        activity ?: return
        val ad = rewarded
        if (ad == null) {
            preloadRewarded(activity)
            return
        }
        rewarded = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { preloadRewarded(activity) }
        }
        ad.show(activity) {
            Services.log("ad_reward_earned")
            onReward()
        }
    }

    // ------------------------------------------------------------------ native

    /** Loads one native ad; [onLoaded] gets it (or null). Destroy it when it leaves the screen. */
    fun loadNative(context: Context, onLoaded: (NativeAd?) -> Unit) {
        if (!nativeOn) return onLoaded(null)
        AdLoader.Builder(context, AdUnits.NATIVE)
            .forNativeAd { onLoaded(it) }
            .withAdListener(object : AdListener() { override fun onAdFailedToLoad(error: LoadAdError) { onLoaded(null) } })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    /** Builds the native ad's layout in code: icon, headline, body, image and a button. */
    fun nativeView(context: Context, ad: NativeAd, ink: Int, muted: Int, buttonBg: Int, buttonFg: Int): NativeAdView {
        fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics).toInt()
        val root = NativeAdView(context)
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val top = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val icon = ImageView(context).apply { layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) } }
        ad.icon?.drawable?.let { icon.setImageDrawable(it) } ?: run { icon.layoutParams = LinearLayout.LayoutParams(0, 0) }
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        val headline = TextView(context).apply { text = ad.headline; setTextColor(ink); textSize = 15f; setTypeface(typeface, Typeface.BOLD); maxLines = 2 }
        val advertiser = TextView(context).apply { text = ad.advertiser ?: ""; setTextColor(muted); textSize = 12f; maxLines = 1 }
        texts.addView(headline); texts.addView(advertiser)
        top.addView(icon); top.addView(texts)
        val body = TextView(context).apply { text = ad.body ?: ""; setTextColor(muted); textSize = 14f; maxLines = 3; setPadding(0, dp(8), 0, dp(8)) }
        val media = MediaView(context).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(160)) }
        val cta = Button(context).apply {
            text = ad.callToAction ?: "Learn more"; isAllCaps = false; setTextColor(buttonFg); setBackgroundColor(buttonBg)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply { topMargin = dp(10) }
        }
        column.addView(top); column.addView(body); column.addView(media); column.addView(cta)
        root.addView(column)
        root.headlineView = headline
        root.bodyView = body
        root.iconView = icon
        root.advertiserView = advertiser
        root.callToActionView = cta
        root.mediaView = media
        root.setNativeAd(ad)
        return root
    }
}
