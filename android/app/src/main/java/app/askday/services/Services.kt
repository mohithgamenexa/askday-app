package app.askday.services

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

/** Ad unit IDs. These are Google's official test IDs: replace them with your own from the AdMob console. */
object AdIds {
    const val BANNER = AdUnits.BANNER
}

/**
 * Firebase Analytics, Crashlytics, Remote Config and Google Mobile Ads in one place.
 * Everything here quietly does nothing until google-services.json has been added to the app.
 */
object Services {
    private lateinit var app: Context
    private val adsStarted = AtomicBoolean(false)

    /** True once the Firebase project file is in the app. */
    var firebaseReady = false
        private set

    private val _adsEnabled = MutableStateFlow(false)
    /** Remote Config switch "ads_enabled". Off unless you turn it on in the Firebase console. */
    val adsEnabled: StateFlow<Boolean> = _adsEnabled

    private val _adsReady = MutableStateFlow(false)
    /** True once consent allows ads and the Mobile Ads SDK has started. */
    val adsReady: StateFlow<Boolean> = _adsReady

    fun init(context: Context) {
        app = context.applicationContext
        firebaseReady = FirebaseApp.getApps(app).isNotEmpty()
        if (!firebaseReady) return
        val rc = FirebaseRemoteConfig.getInstance()
        rc.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(3600).build())
        rc.setDefaultsAsync(mapOf<String, Any>("ads_enabled" to false) + Ads.DEFAULTS)
        rc.fetchAndActivate().addOnCompleteListener { _adsEnabled.value = rc.getBoolean("ads_enabled") }
    }

    // ------------------------------------------------------------ analytics

    /** Logs an event to Firebase Analytics, for example log("answer_submitted", "group_size" to 5). */
    fun log(event: String, vararg params: Pair<String, Any>) {
        if (!firebaseReady) return
        val bundle = Bundle()
        for ((k, v) in params) when (v) {
            is Int -> bundle.putLong(k, v.toLong())
            is Long -> bundle.putLong(k, v)
            is Double -> bundle.putDouble(k, v)
            is Boolean -> bundle.putString(k, v.toString())
            else -> bundle.putString(k, v.toString())
        }
        FirebaseAnalytics.getInstance(app).logEvent(event, bundle)
    }

    /** Links analytics and crash reports to the signed-in person (never their phone number). */
    fun setUser(uid: String?) {
        if (!firebaseReady) return
        FirebaseAnalytics.getInstance(app).setUserId(uid)
        FirebaseCrashlytics.getInstance().setUserId(uid ?: "")
    }

    fun recordError(t: Throwable) {
        if (firebaseReady) FirebaseCrashlytics.getInstance().recordException(t)
    }

    // ------------------------------------------------------------------ ads

    /**
     * Shows Google's consent form where the law requires it (for example in Europe), then starts the ads SDK.
     * Call from the activity when ads are switched on.
     */
    fun startAds(activity: Activity) {
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        val begin = {
            if (consent.canRequestAds() && adsStarted.compareAndSet(false, true)) {
                MobileAds.initialize(activity) {
                    _adsReady.value = true
                    // Have a full-screen and a rewarded ad ready for when they're needed.
                    Ads.preloadInterstitial(activity)
                    Ads.preloadRewarded(activity)
                }
            }
        }
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { begin() } },
            { begin() },
        )
        begin() // Consent from an earlier session may already allow ads.
    }
}
