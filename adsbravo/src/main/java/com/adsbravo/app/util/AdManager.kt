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
import com.adsbravo.app.model.AdKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AdManager {
    private val adsMap = mutableMapOf<AdKey, AdData?>()
    private val loadingMap = mutableMapOf<AdKey, Boolean>()
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
        val key = AdKey(adType, sourceId)

        if (adsMap[key] != null || loadingMap[key] == true) return

        val ad = loadAdFromPrefs(key)
        if (ad != null) {
            adsMap[key] = ad
            Log.d("AdManager", "Ad [$adType:$sourceId] cargado desde prefs")
            return
        }

        fetchAd(key)
    }

    fun consumeAd(adType: AdType, sourceId: String): AdData? {
        val key = AdKey(adType, sourceId)
        val ad = adsMap[key] ?: loadAdFromPrefs(key)
        adsMap[key] = null
        clearAdFromPrefs(key)
        return ad
    }

    fun clear(adType: AdType, sourceId: String) {
        val key = AdKey(adType, sourceId)
        adsMap[key] = null
        clearAdFromPrefs(key)
    }

    private fun fetchAd(key: AdKey) {
        val (adType, sourceId) = key
        config?.let { cfg ->
            loadingMap[key] = true

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
                            adsMap[key] = ad
                            saveAdToPrefs(key, ad)
                            Log.d("AdManager", "Ad [$adType:$sourceId] cargado y guardado")
                        }
                    } else {
                        Log.e("AdManager", "Error cargando [$adType:$sourceId]: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("AdManager", "Excepción cargando [$adType:$sourceId]: ${e.message}")
                } finally {
                    loadingMap[key] = false
                }
            }
        }
    }

    private fun saveAdToPrefs(key: AdKey, ad: AdData) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString("ad_${key.type.name}_${key.sourceId}", gson.toJson(ad))
            putLong("ad_${key.type.name}_${key.sourceId}_timestamp", System.currentTimeMillis())
        }
    }

    private fun loadAdFromPrefs(key: AdKey): AdData? {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString("ad_${key.type.name}_${key.sourceId}", null)
        val timestamp = prefs.getLong("ad_${key.type.name}_${key.sourceId}_timestamp", 0L)

        val age = System.currentTimeMillis() - timestamp
        if (json != null) {
            if (age <= MAX_AD_AGE_MS) {
                return gson.fromJson(json, object : TypeToken<AdData>() {}.type)
            } else {
                Log.d("AdManager", "Ad caducado [$key], eliminado")
                clearAdFromPrefs(key)
            }
        }
        return null
    }

    private fun clearAdFromPrefs(key: AdKey) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove("ad_${key.type.name}_${key.sourceId}")
            remove("ad_${key.type.name}_${key.sourceId}_timestamp")
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