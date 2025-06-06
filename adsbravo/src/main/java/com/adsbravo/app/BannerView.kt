package com.adsbravo.app

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.net.toUri
import coil.load
import com.adsbravo.app.model.AdType
import com.adsbravo.app.util.AdManager

class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val adIcon: ImageView
    private val adTitle: TextView
    private val adText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 40_000L // 40 segundos

    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadAd()
            handler.postDelayed(this, refreshInterval)
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.banner_view, this, true)
        orientation = HORIZONTAL

        adIcon = findViewById(R.id.adIcon)
        adTitle = findViewById(R.id.adTitle)
        adText = findViewById(R.id.adText)
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

    fun loadAd() {
        val adData = AdManager.consumeAd(AdType.BANNER, "default_banner") ?: return

        adIcon.load(adData.iconUrl)
        adTitle.text = adData.title
        adText.text = adData.text

        setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, adData.clickUrl.toUri())
            context.startActivity(intent)
        }

        Ads.loadBanner("default_banner")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeightPx = 80.dpToPx(context)
        val heightSpec = MeasureSpec.makeMeasureSpec(desiredHeightPx, MeasureSpec.EXACTLY)

        super.onMeasure(widthMeasureSpec, heightSpec)
    }

    private fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
}