package com.adsbravo.app

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ProcessLifecycleOwner
import com.adsbravo.app.model.AdType
import com.adsbravo.app.model.AdsConfig
import com.adsbravo.app.util.AdManager
import com.adsbravo.app.util.AdManager.loadAdManager
import com.adsbravo.app.util.AppLifecycleObserver
import com.adsbravo.app.util.RewardManager

object Ads {
    private var config: AdsConfig? = null
    private var isFirstLaunch = true
    private var openAppCount = 0
    private var currentOpenAppActivity: OpenAppActivity? = null

    fun init(context: Context, config: AdsConfig) {
        this.config = config
        AdManager.initialize(context, config)
        loadBanner("default_banner")
        loadCollapsibleBanner("default_collapsible")
    }

    private fun loadAd(sourceId: String, adType: AdType) {
        loadAdManager(sourceId, adType)
    }

    fun loadInterstitial(sourceId: String) {
        loadAd(sourceId, AdType.INTERSTITIAL)
    }

    fun showInterstitial(context: Context, sourceId: String) {
        val intent = Intent(context, InterstitialActivity::class.java)
        intent.putExtra("source_id", sourceId)
        context.startActivity(intent)
    }

    fun loadRewarded(sourceId: String) {
        loadAd(sourceId, AdType.REWARDED)
    }

    fun showRewarded(context: Context, sourceId: String, onReward: () -> Unit) {
        RewardManager.onRewardObtained = onReward

        val intent = Intent(context, RewardedActivity::class.java)
        intent.putExtra("source_id", sourceId)
        context.startActivity(intent)
    }

    fun loadBanner(sourceId: String) {
        loadAd(sourceId, AdType.BANNER)
    }

    fun loadCollapsibleBanner(sourceId: String) {
        loadAd(sourceId, AdType.COLLAPSIBLE_BANNER)
    }

    fun initOpenApp(context: Context) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            AppLifecycleObserver {
                if (isFirstLaunch) {
                    isFirstLaunch = false
                    return@AppLifecycleObserver
                }

                openAppCount++
                if (openAppCount % 2 == 1) {
                    currentOpenAppActivity?.finish()

                    val sourceId = "open_app"
                    loadAd(sourceId, AdType.OPEN_APP)

                    val intent = Intent(context, OpenAppActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
        )
    }

    fun setOpenAppActivity(activity: OpenAppActivity?) {
        currentOpenAppActivity = activity
    }
}