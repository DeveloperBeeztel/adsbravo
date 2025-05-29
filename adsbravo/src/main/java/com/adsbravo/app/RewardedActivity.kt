package com.adsbravo.app

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import coil.load
import com.adsbravo.app.util.AdManager
import com.adsbravo.app.util.RewardManager

class RewardedActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var rewardText: TextView
    private lateinit var closeButton: FrameLayout

    private lateinit var adImage: ImageView
    private lateinit var adIcon: ImageView
    private lateinit var adTitle: TextView
    private lateinit var adText: TextView

    private var rewardGiven = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        setContentView(R.layout.activity_rewarded_ad)

        progressBar = findViewById(R.id.progressBar)
        rewardText = findViewById(R.id.rewardText)
        closeButton = findViewById(R.id.closeButton)

        adImage = findViewById(R.id.ad_image)
        adIcon = findViewById(R.id.ad_icon)
        adTitle = findViewById(R.id.ad_title)
        adText = findViewById(R.id.ad_text)

        val adData = AdManager.consumeAd()
        if (adData == null) {
            finish()
            return
        }

        // Load ad content
        adImage.load(adData.imageUrl)
        adIcon.load(adData.iconUrl)
        adTitle.text = adData.title
        adText.text = adData.text

        startCountdown()

        closeButton.setOnClickListener {
            if (rewardGiven) {
                RewardManager.onRewardObtained?.invoke()
            }
            finish()
        }

        val rootLayout = findViewById<ConstraintLayout>(R.id.rewardedRoot)
        rootLayout.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, adData.clickUrl.toUri())
            startActivity(intent)
        }

    }

    private fun startCountdown() {
        val duration = 10_000L
        val maxProgress = 100

        progressBar.max = maxProgress

        val animator = ValueAnimator.ofFloat(0f, maxProgress.toFloat())
        animator.duration = duration
        animator.interpolator = LinearInterpolator()

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            progressBar.progress = animatedValue.toInt()
        }

        animator.start()

        Handler(Looper.getMainLooper()).postDelayed({
            rewardGiven = true
            animateRewardText()
            closeButton.visibility = View.VISIBLE
        }, duration)
    }

    private fun animateRewardText() {
        rewardText.alpha = 0f
        rewardText.scaleX = 0.8f
        rewardText.scaleY = 0.8f
        rewardText.visibility = View.VISIBLE

        rewardText.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }


    override fun onDestroy() {
        RewardManager.onRewardObtained = null
        super.onDestroy()
    }
}