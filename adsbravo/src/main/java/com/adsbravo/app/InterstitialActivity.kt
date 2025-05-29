package com.adsbravo.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import coil.load
import androidx.core.net.toUri
import com.adsbravo.app.util.AdManager

class InterstitialActivity : AppCompatActivity() {

    private lateinit var closeButton: FrameLayout
    private lateinit var adImage: ImageView
    private lateinit var adIcon: ImageView
    private lateinit var adTitle: TextView
    private lateinit var adText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        setContentView(R.layout.activity_interstitial_ad)

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

        val interstitialRoot = findViewById<ConstraintLayout>(R.id.interstitialRoot)
        interstitialRoot.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, adData.clickUrl.toUri())
            startActivity(intent)
        }

        // Load ad content
        adImage.load(adData.imageUrl)
        adIcon.load(adData.iconUrl)
        adTitle.text = adData.title
        adText.text = adData.text

        // Mostrar el botón de cerrar después de 5 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            val closeButton = findViewById<FrameLayout>(R.id.closeButton)
            val params = closeButton.layoutParams as ConstraintLayout.LayoutParams

            val marginInDp = 8
            val marginPx = (marginInDp * resources.displayMetrics.density).toInt()

            val showLeft = (0..1).random() == 0

            if (showLeft) {
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.marginStart = marginPx
                params.endToEnd = -1
            } else {
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                params.marginEnd = marginPx
                params.startToStart = -1
            }

            closeButton.layoutParams = params
            closeButton.visibility = FrameLayout.VISIBLE
        }, 5000)

        // Cerrar la actividad al presionar la X
        closeButton.setOnClickListener {
            finish()
        }
    }
}