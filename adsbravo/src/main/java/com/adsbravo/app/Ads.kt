package com.adsbravo.app

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ProcessLifecycleOwner
import com.adsbravo.app.model.AdsConfig
import com.adsbravo.app.util.AdManager
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
    }

    fun showInterstitial(context: Context) {
        val intent = Intent(context, InterstitialActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun showRewarded(context: Context, onReward: () -> Unit) {
        RewardManager.onRewardObtained = onReward

        val intent = Intent(context, RewardedActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
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
