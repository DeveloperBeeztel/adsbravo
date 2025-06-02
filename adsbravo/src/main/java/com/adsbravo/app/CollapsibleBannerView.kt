package com.adsbravo.app

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import coil.load
import com.adsbravo.app.model.AdType
import com.adsbravo.app.util.AdManager

class CollapsibleBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    private val bannerContainer: View
    private val expandedContent: View
    private val collapsedContent: View
    private val minimizeButton: View
    private val bannerImage: ImageView
    private val bannerTitle: TextView
    private val bannerText: TextView
    private val bannerIcon: ImageView
    private val adTitle: TextView
    private val adText: TextView
    private val adIcon: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 40_000L // 40 segundos

    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadAd()
            handler.postDelayed(this, refreshInterval)
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.collapsible_banner_view, this, true)

        expandedContent = findViewById(R.id.expandedContent)
        collapsedContent = findViewById(R.id.collapsedContent)
        minimizeButton = findViewById(R.id.minimizeButtonContainer)

        bannerContainer = findViewById(R.id.bannerContainer)
        bannerImage = findViewById(R.id.bannerImage)
        bannerTitle = findViewById(R.id.bannerTitle)
        bannerText = findViewById(R.id.bannerText)
        bannerIcon = findViewById(R.id.bannerIcon)

        adTitle = findViewById(R.id.adTitle)
        adText = findViewById(R.id.adText)
        adIcon = findViewById(R.id.adIcon)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Comienza el ciclo de actualización
        refreshRunnable.run()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun loadAd() {
        val adData = AdManager.consumeAd(AdType.COLLAPSIBLE_BANNER) ?: return

        // Expanded content
        bannerImage.load(adData.imageUrl)
        bannerIcon.load(adData.iconUrl)
        bannerTitle.text = adData.title
        bannerText.text = adData.text

        // Collapsed content
        adTitle.text = adData.title
        adText.text = adData.text
        adIcon.load(adData.iconUrl)

        setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, adData.clickUrl.toUri())
            context.startActivity(intent)
        }

        minimizeButton.setOnClickListener {
            collapseBanner()
        }

        Ads.loadCollapsibleBanner("")
    }

    private fun collapseBanner() {
        val targetHeight = 80.dpToPx(context)
        val initialParams = bannerContainer.layoutParams

        val animator = ValueAnimator.ofInt(bannerContainer.height, targetHeight)
        animator.duration = 300
        animator.addUpdateListener { valueAnimator ->
            val newHeight = valueAnimator.animatedValue as Int
            bannerContainer.layoutParams = initialParams.apply {
                height = newHeight
                width = LayoutParams.MATCH_PARENT
            }
            bannerContainer.requestLayout()
        }

        animator.start()

        expandedContent.visibility = GONE
        collapsedContent.visibility = VISIBLE
        minimizeButton.visibility = GONE
    }

    private fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
}
