package com.adsbravo.app.util

import android.content.Context
import android.util.Log
import com.adsbravo.app.model.AdData
import com.adsbravo.app.model.AdsConfig
import com.adsbravo.app.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import androidx.core.content.edit

object AdManager {
    private var currentAd: AdData? = null
    private var isLoading = false
    private var config: AdsConfig? = null
    private lateinit var appContext: Context

    fun initialize(context: Context, config: AdsConfig) {
        this.config = config
        this.appContext = context.applicationContext
        fetchAd()
    }

    fun consumeAd(): AdData? {
        val ad = currentAd
        currentAd = null // Liberar para que se cargue otro luego
        fetchAd()
        return ad
    }

    fun clear() {
        currentAd = null
    }

    private fun fetchAd() {
        if (isLoading || currentAd != null) return

        config?.let { cfg ->
            isLoading = true

            val token = cfg.token
            val lang = appContext.resources.configuration.locales[0].language
            val rawUserAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"
            val userAgent = URLEncoder.encode(rawUserAgent, StandardCharsets.UTF_8.toString())
            val sourceId = cfg.sourceId
            val referer = appContext.packageName
            val secret = cfg.secret
            val uid = getOrCreateUid(appContext)
            val subAge = System.currentTimeMillis() / 1000

            val finalUrl = "http://dspfeed.adsbravo.com/$token" + "/no-ip?" +
                    "lang=$lang&ua=$userAgent&sourceid=$sourceId" +
                    "&referer=$referer&secret=$secret&uid=$uid&subage=$subAge"

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitInstance.adService.getAd(finalUrl)
                    if (response.isSuccessful) {
                        currentAd = response.body()
                        Log.d("AdManager", "Ad cargado: $currentAd")
                    } else {
                        Log.e("AdManager", "Error: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("AdManager", "Excepción: ${e.message}")
                } finally {
                    isLoading = false
                }
            }
        } ?: Log.e("AdManager", "Config no inicializada")
    }

    private fun getOrCreateUid(context: Context): String {
        val prefs = context.getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        val existingUid = prefs.getString("uid", null)
        return existingUid ?: UUID.randomUUID().toString().also {
            prefs.edit { putString("uid", it) }
        }
    }
}