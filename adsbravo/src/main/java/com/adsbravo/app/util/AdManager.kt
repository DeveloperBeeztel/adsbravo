package com.adsbravo.app.util

import android.content.Context
import android.util.Log
import com.adsbravo.app.model.AdData
import com.adsbravo.app.model.AdType
import com.adsbravo.app.model.AdsConfig
import com.adsbravo.app.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AdManager {
    private val adsMap = mutableMapOf<AdType, AdData?>()
    private val loadingMap = mutableMapOf<AdType, Boolean>()
    private lateinit var appContext: Context
    private var config: AdsConfig? = null
    private val gson = Gson()
    private const val PREFS_NAME = "ads_prefs"
    private const val MAX_AD_AGE_MS = 12 * 60 * 60 * 1000L // 12 horas

    fun initialize(context: Context, config: AdsConfig) {
        this.config = config
        this.appContext = context.applicationContext
    }

    fun loadAdManager(sourceId: String, adType: AdType) {
        // Si ya hay uno cargado en memoria, no cargamos
        if (adsMap[adType] != null || loadingMap[adType] == true) return

        // Intentar cargar desde prefs
        val ad = loadAdFromPrefs(adType)
        if (ad != null) {
            adsMap[adType] = ad
            Log.d("AdManager", "Ad [$adType] cargado desde prefs")
            return
        }

        // Si no hay, cargar desde red
        fetchAd(adType, sourceId)
    }

    fun consumeAd(adType: AdType): AdData? {
        val ad = adsMap[adType] ?: loadAdFromPrefs(adType)
        adsMap[adType] = null
        clearAdFromPrefs(adType)
        return ad
    }

    fun clear(adType: AdType) {
        adsMap[adType] = null
        clearAdFromPrefs(adType)
    }

    private fun fetchAd(adType: AdType, sourceId: String) {
        config?.let { cfg ->
            loadingMap[adType] = true

            val token = cfg.token
            val lang = appContext.resources.configuration.locales[0].language
            val userAgent = URLEncoder.encode(
                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36",
                StandardCharsets.UTF_8.toString()
            )
            val referer = appContext.packageName
            val uid = getOrCreateUid(appContext)
            val subAge = System.currentTimeMillis() / 1000
            val secret = cfg.secret
            val fullSourceId = "${adType.sourceId}: $sourceId"

            val url = "http://dspfeed.adsbravo.com/$token/no-ip?" +
                    "lang=$lang&ua=$userAgent&sourceid=${URLEncoder.encode(fullSourceId, StandardCharsets.UTF_8.toString())}" +
                    "&referer=$referer&secret=$secret&uid=$uid&subage=$subAge"

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitInstance.adService.getAd(url)
                    if (response.isSuccessful) {
                        val ad = response.body()
                        if (ad != null) {
                            adsMap[adType] = ad
                            saveAdToPrefs(adType, ad)
                            Log.d("AdManager", "Ad [$adType] cargado y guardado")
                        }
                    } else {
                        Log.e("AdManager", "Error cargando [$adType]: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("AdManager", "Excepción cargando [$adType]: ${e.message}")
                } finally {
                    loadingMap[adType] = false
                }
            }
        }
    }

    private fun saveAdToPrefs(adType: AdType, ad: AdData) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString("ad_${adType.name}", gson.toJson(ad))
            putLong("ad_${adType.name}_timestamp", System.currentTimeMillis())
        }
    }

    private fun loadAdFromPrefs(adType: AdType): AdData? {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString("ad_${adType.name}", null)
        val timestamp = prefs.getLong("ad_${adType.name}_timestamp", 0L)
        if (json != null && System.currentTimeMillis() - timestamp <= MAX_AD_AGE_MS) {
            return gson.fromJson(json, object : TypeToken<AdData>() {}.type)
        }
        return null
    }

    private fun clearAdFromPrefs(adType: AdType) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove("ad_${adType.name}")
            remove("ad_${adType.name}_timestamp")
        }
    }

    private fun getOrCreateUid(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingUid = prefs.getString("uid", null)
        return existingUid ?: UUID.randomUUID().toString().also {
            prefs.edit { putString("uid", it) }
        }
    }

    private fun logStoredAds() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        AdType.entries.forEach { adType ->
            val json = prefs.getString("ad_${adType.name}", null)

            if (json != null) {
                Log.d("AdManager", "Ad en prefs [$adType]: $json")
            } else {
                Log.d("AdManager", "No hay ad guardado para [$adType]")
            }
        }
        Log.d("AdManager", "------------------")
    }
}