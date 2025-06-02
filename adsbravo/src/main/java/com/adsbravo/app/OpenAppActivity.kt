package com.adsbravo.app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import coil.load
import com.adsbravo.app.model.AdType
import com.adsbravo.app.util.AdManager

class OpenAppActivity : AppCompatActivity() {
    private lateinit var closeContainer: LinearLayout
    private lateinit var adImage: ImageView
    private lateinit var adIcon: ImageView
    private lateinit var adTitle: TextView
    private lateinit var adText: TextView

    private lateinit var adAppName: TextView
    private lateinit var adAppIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        Ads.setOpenAppActivity(this)
        setContentView(R.layout.activity_open_app_ad)

        closeContainer = findViewById(R.id.close_container)

        adImage = findViewById(R.id.ad_image)
        adIcon = findViewById(R.id.ad_icon)
        adTitle = findViewById(R.id.ad_title)
        adText = findViewById(R.id.ad_text)

        adAppName = findViewById(R.id.app_name)
        adAppIcon = findViewById(R.id.app_icon)

        val adData = AdManager.consumeAd(AdType.OPEN_APP)
        if (adData == null) {
            finish()
            return
        }

        val mainContainer = findViewById<LinearLayout>(R.id.main_container)
        mainContainer.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, adData.clickUrl.toUri())
            startActivity(intent)
        }

        // Load ad content
        adImage.load(adData.imageUrl)
        adIcon.load(adData.iconUrl)
        adTitle.text = adData.title
        adText.text = adData.text

        val packageManager = this.packageManager
        val packageName = this.packageName
        val appIcon = packageManager.getApplicationIcon(packageName)
        val appName = this.applicationInfo.loadLabel(packageManager).toString()

        adAppIcon.load(appIcon)
        adAppName.text = appName


        closeContainer.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Ads.setOpenAppActivity(null)
    }
}